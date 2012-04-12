/*
 *    ReplaceMissingValuesFilter.java
 *    Copyright (C) 1999 Eibe Frank
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
 * Replaces all missing values for nominal and numeric attributes in a 
 * dataset with the modes and means from the training data.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version 1.0
 */
public class ReplaceMissingValuesFilter extends Filter {

  // =================
  // Private variables
  // =================

  /** The modes and means */
  private double[] m_ModesAndMeans = null;

  // ==============
  // Public methods
  // ==============

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the input format can't be set 
   * successfully
   */
  public boolean inputFormat(Instances instanceInfo) 
       throws Exception {

    m_InputFormat = new Instances(instanceInfo, 0);
    setOutputFormat(m_InputFormat);
    b_NewBatch = true;
    m_ModesAndMeans = null;
    return true;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception Exception if the input instance was not of the 
   * correct format or if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (b_NewBatch) {
      resetQueue();
      b_NewBatch = false;
    }
    if (m_ModesAndMeans == null) {
      m_InputFormat.add(instance);
      return false;
    } else {
      convertInstance(instance);
      return true;
    }
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @exception Exception if no input structure has been defined
   */

  public boolean batchFinished() throws Exception {

    Instance current;

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_ModesAndMeans == null) {
   
      // Compute modes and means
      m_ModesAndMeans = new double[m_InputFormat.numAttributes()];
      for (int i = 0; i < m_InputFormat.numAttributes(); i++) {
	if (m_InputFormat.attribute(i).isNominal() ||
            m_InputFormat.attribute(i).isNumeric()) {
          m_ModesAndMeans[i] = m_InputFormat.meanOrMode(i);
        } 
      }

      // Convert pending input instances

      for(int i = 0; i < m_InputFormat.numInstances(); i++) {
	current = m_InputFormat.instance(i);
	convertInstance(current);
      }
    } 

    b_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  // ===============
  // Private methods
  // ===============

  /**
   * Convert a single instance over. The converted instance is 
   * added to the end of the output queue.
   *
   * @param instance the instance to convert
   */

  private void convertInstance(Instance instance) throws Exception {
  
    Instance newInstance = new Instance(instance);

    for(int j = 0; j < m_InputFormat.numAttributes(); j++) 
      if (instance.isMissing(j) &&
          (m_InputFormat.attribute(j).isNominal() ||
           m_InputFormat.attribute(j).isNumeric())) {
        newInstance.setValue(j, m_ModesAndMeans[j]); 
      }
    push(newInstance);
  }

  // ============
  // Test method.
  // ============

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new ReplaceMissingValuesFilter(),argv);
      } else {
	Filter.filterFile(new ReplaceMissingValuesFilter(),argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








