/*
 *    ObsfucateFilter.java
 *    Copyright (C) 2000 Intelligenesis Corp.
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package weka.filters;

import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * A simple instance filter that renames the relation, all attribute names
 * and all nominal (and string) attribute values. For exchanging sensitive
 * datasets. Currently doesn't like string attributes.
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.1 $
 */
public class ObsfucateFilter extends Filter {

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "An instance filter that obsfucates all strings in the data";
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   */
  public boolean inputFormat(Instances instanceInfo) {

    m_InputFormat = new Instances(instanceInfo, 0);
    
    // Make the obsfucated header
    FastVector v = new FastVector();
    for (int i = 0; i < instanceInfo.numAttributes(); i++) {
      Attribute oldAtt = instanceInfo.attribute(i);
      Attribute newAtt = null;
      switch (oldAtt.type()) {
      case Attribute.NUMERIC:
        newAtt = new Attribute("A" + (i + 1));
        break;
      case Attribute.NOMINAL:
        FastVector vals = new FastVector();
        for (int j = 0; j < oldAtt.numValues(); j++) {
          vals.addElement("V" + (j + 1));
        }
        newAtt = new Attribute("A" + (i + 1), vals);
        break;
      case Attribute.STRING:
      default:
        newAtt = (Attribute) oldAtt.copy();
        System.err.println("Not converting attribute: " + oldAtt.name());
        break;
      }
      v.addElement(newAtt);
    }
    Instances newHeader = new Instances("R", v, 10);
    setOutputFormat(newHeader);
    m_NewBatch = true;
    return true;
  }


  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception Exception if the input instance was not of the correct 
   * format or if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    double [] newVals = instance.toDoubleArray();
    push(new Instance(instance.weight(), newVals));
    return true;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {
    
    try {
      if (Utils.getFlag('b', argv)) {
	Filter.batchFilterFile(new ObsfucateFilter(), argv);
      } else {
	Filter.filterFile(new ObsfucateFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








