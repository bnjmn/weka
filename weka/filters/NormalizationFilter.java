/*
 *    NormalizationFilter.java
 *    Copyright (C) 1999 Eibe Frank
 *
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
 * @version $Revision: 1.5 $
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
      for (int j = 0; j < m_InputFormat.numInstances(); j++) {
	double[] value = m_InputFormat.instance(j).toDoubleArray();
	for (int i = 0; i < m_InputFormat.numAttributes(); i++) {
	  if (m_InputFormat.attribute(i).isNumeric()) {
	    if (!Instance.isMissingValue(value[i])) {
	      if (Double.isNaN(m_MinArray[i])) {
		m_MinArray[i] = m_MaxArray[i] = value[i];
	      } else {
		if (value[i] < m_MinArray[i]) {
		  m_MinArray[i] = value[i];
		}
		if (value[i] > m_MaxArray[i]) {
		  m_MaxArray[i] = value[i];
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

      // Free memory
      m_InputFormat = new Instances(m_InputFormat, 0);
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
  
    if (!(instance instanceof SparseInstance)) {
      double[] vals = instance.toDoubleArray();
      for (int j = 0; j < m_InputFormat.numAttributes(); j++) {
	if (instance.attribute(j).isNumeric() &&
	    (!Instance.isMissingValue(vals[j]))) {
	  if (Double.isNaN(m_MinArray[j]) ||
	      (m_MaxArray[j] == m_MinArray[j])) {
	    vals[j] = 0;
	  } else {
	    vals[j] = (vals[j] - m_MinArray[j]) / 
	      (m_MaxArray[j] - m_MinArray[j]);
	  }
	}
      }	
      push(new Instance(instance.weight(), vals));
    } else {
      double[] newVals = new double[instance.numAttributes()];
      int[] newIndices = new int[instance.numAttributes()];
      double[] vals = instance.toDoubleArray();
      int ind = 0;
      for (int j = 0; j < instance.numAttributes(); j++) {
	double value;
	if (instance.attribute(j).isNumeric() &&
	    (!Instance.isMissingValue(vals[j]))) {
	  if (Double.isNaN(m_MinArray[j]) ||
	      (m_MaxArray[j] == m_MinArray[j])) {
	    value = 0;
	  } else {
	    value = (vals[j] - m_MinArray[j]) / 
	      (m_MaxArray[j] - m_MinArray[j]);
	  }
	  if (value != 0.0) {
	    newVals[ind] = value;
	    newIndices[ind] = j;
	    ind++;
	  }
	} else {
	  value = vals[j];
	  if (value != 0.0) {
	    newVals[ind] = value;
	    newIndices[ind] = j;
	    ind++;
	  }
	}
      }	
      double[] tempVals = new double[ind];
      int[] tempInd = new int[ind];
      System.arraycopy(newVals, 0, tempVals, 0, ind);
      System.arraycopy(newIndices, 0, tempInd, 0, ind);
      push(new SparseInstance(instance.weight(), tempVals, tempInd,
			      instance.numAttributes()));
    }
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








