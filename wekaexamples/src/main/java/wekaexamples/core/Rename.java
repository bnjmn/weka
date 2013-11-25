/*
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

/*
 *    Rename.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.core;

import java.util.Vector;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * Renames all the labels of nominal attributes to numbers, they way they
 * appear, e.g., attribute a1 has the labels "what", "so" and "ever" are renamed
 * to "0", "1" and "2".
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class Rename {

  /**
   * Takes two parameters as input: 1. input arff file 2. output arff file
   * (transformed) 3. (optional) the attribute index (1-based), otherwise all
   * attributes except class are changed Assumption: last attribute is class
   * attribute
   * 
   * @param args the commandline arguments
   * @throws Exception if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      throw new Exception("Needs at least input and output ARFF file!");
    }

    // read arff file
    Instances arff = DataSource.read(args[0]);
    arff.setClassIndex(arff.numAttributes() - 1);

    // determine attributes to modify
    Integer[] indices = null;
    if (args.length > 2) {
      indices = new Integer[1];
      indices[0] = new Integer(Integer.parseInt(args[2]) - 1);
    } else {
      Vector<Integer> v = new Vector<Integer>();
      for (int i = 0; i < arff.numAttributes() - 2; i++) {
        if (arff.attribute(i).isNominal()) {
          v.add(new Integer(i));
        }
      }
      indices = v.toArray(new Integer[v.size()]);
    }

    // rename labels of all nominal attributes
    for (Integer indice : indices) {
      int attInd = indice.intValue();
      Attribute att = arff.attribute(attInd);
      for (int n = 0; n < att.numValues(); n++) {
        arff.renameAttributeValue(att, att.value(n), "" + n);
      }
    }

    // save arff file
    DataSink.write(args[1], arff);
  }
}
