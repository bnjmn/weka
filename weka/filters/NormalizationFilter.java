/*
 *    NormalizationFilter.java
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
 * Normalizes all numeric values in the given dataset. The resulting
 * values are in [0,1] for the data used to compute the normalization
 * intervals.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.2 $
 */
public class NormalizationFilter extends Filter {

  /** The minimum values for numeric attributes. */
  private double [] m_MinArray;
  
  /** The maximum values for numeric attributes. */
  private double [] m_MaxArray;

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
    m_NewBatch = true;
    m_MinArray = m_MaxArray = null;
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
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (m_MinArray == null) {
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
    if (m_MinArray == null) {
   
      // Compute minimums and maximums
      m_MinArray = new double[m_InputFormat.numAttributes()];
      m_MaxArray = new double[m_InputFormat.numAttributes()];
      for (int i = 0; i < m_InputFormat.numAttributes(); i++) {
	m_MinArray[i] = Double.NaN;
      }
     for (int i = 0; i < m_InputFormat.numAttributes(); i++) {
	if (m_InputFormat.attribute(i).isNumeric()) {
	  for (int j = 0; j < m_InputFormat.numInstances(); j++) {
	    if (!m_InputFormat.instance(j).isMissing(i)) {
	      double value = m_InputFormat.instance(j).value(i);
	      if (Double.isNaN(m_MinArray[i])) {
		m_MinArray[i] = m_MaxArray[i] = value;
	      } else {
		if (value < m_MinArray[i]) {
		  m_MinArray[i] = value;
		}
		if (value > m_MaxArray[i]) {
		  m_MaxArray[i] = value;
		}
	      }
	    }
	  }
	} 
      }

      // Convert pending input instances
      for(int i = 0; i < m_InputFormat.numInstances(); i++) {
	current = m_InputFormat.instance(i);
	convertInstance(current);
      }
    } 

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Convert a single instance over. The converted instance is 
   * added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstance(Instance instance) throws Exception {
  
    double [] newVals = instance.toDoubleArray();
    for (int j = 0; j < m_InputFormat.numAttributes(); j++) {
      if (instance.attribute(j).isNumeric() &&
	  (!instance.isMissing(j))) {
	if (Double.isNaN(m_MinArray[j]) ||
	    (m_MaxArray[j] == m_MinArray[j])) {
	  newVals[j] = 0;
	} else {
	  newVals[j] = (instance.value(j) - m_MinArray[j]) / 
            (m_MaxArray[j] - m_MinArray[j]);
	}
      }
    }	
    push(new Instance(instance.weight(), newVals));
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new NormalizationFilter(), argv);
      } else {
	Filter.filterFile(new NormalizationFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








