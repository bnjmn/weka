/*
 *    ReplaceMissingValuesFilter.java
 *    Copyright (C) 1999 Eibe Frank
 *
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
 * @version $Revision: 1.7 $
 */
public class ReplaceMissingValuesFilter extends Filter {

  /** The modes and means */
  private double[] m_ModesAndMeans = null;

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
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
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
      double sumOfWeights =  m_InputFormat.sumOfWeights();
      double[][] counts = new double[m_InputFormat.numAttributes()][];
      for (int i = 0; i < m_InputFormat.numAttributes(); i++) {
	if (m_InputFormat.attribute(i).isNominal()) {
	  counts[i] = new double[m_InputFormat.attribute(i).numValues()];
	  counts[i][0] = sumOfWeights;
	}
      }
      double[] sums = new double[m_InputFormat.numAttributes()];
      for (int i = 0; i < sums.length; i++) {
	sums[i] = sumOfWeights;
      }
      double[] results = new double[m_InputFormat.numAttributes()];
      for (int j = 0; j < m_InputFormat.numInstances(); j++) {
	Instance inst = m_InputFormat.instance(j);
	for (int i = 0; i < inst.numValues(); i++) {
	  if (!inst.isMissingSparse(i)) {
	    double value = inst.valueSparse(i);
	    if (inst.attributeSparse(i).isNominal()) {
	      counts[inst.index(i)][(int)value] += inst.weight();
	      counts[inst.index(i)][0] -= inst.weight();
	    } else if (inst.attributeSparse(i).isNumeric()) {
	      results[inst.index(i)] += inst.weight() * inst.valueSparse(i);
	    }
	  } else {
	    if (inst.attributeSparse(i).isNominal()) {
	      counts[inst.index(i)][0] -= inst.weight();
	    } else if (inst.attributeSparse(i).isNumeric()) {
	      sums[inst.index(i)] -= inst.weight();
	    }
	  }
	}
      }
      m_ModesAndMeans = new double[m_InputFormat.numAttributes()];
      for (int i = 0; i < m_InputFormat.numAttributes(); i++) {
	if (m_InputFormat.attribute(i).isNominal()) {
	  m_ModesAndMeans[i] = (double)Utils.maxIndex(counts[i]);
	} else if (m_InputFormat.attribute(i).isNumeric()) {
	  if (Utils.gr(sums[i], 0)) {
	    m_ModesAndMeans[i] = results[i] / sums[i];
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
      double[] newVals = new double[m_InputFormat.numAttributes()];
      for(int j = 0; j < instance.numAttributes(); j++) {
	if (instance.isMissing(j) &&
	    (m_InputFormat.attribute(j).isNominal() ||
	     m_InputFormat.attribute(j).isNumeric())) {
	  newVals[j] = m_ModesAndMeans[j]; 
	} else {
	  newVals[j] = instance.value(j);
	}
      } 
      push(new Instance(instance.weight(), newVals));
    } else {
      double[] newVals = new double[instance.numValues()];
      int[] newIndices = new int[instance.numValues()];
      int num = 0;
      for(int j = 0; j < instance.numValues(); j++) {
	if (instance.isMissingSparse(j) &&
	    (instance.attributeSparse(j).isNominal() ||
	     instance.attributeSparse(j).isNumeric())) {
	  if (m_ModesAndMeans[instance.index(j)] != 0.0) {
	    newVals[num] = m_ModesAndMeans[instance.index(j)];
	    newIndices[num] = instance.index(j);
	    num++;
	  } 
	} else {
	  newVals[num] = instance.valueSparse(j);
	  newIndices[num] = instance.index(j);
	  num++;
	}
      } 
      if (num == instance.numValues()) {
	push(new SparseInstance(instance.weight(), newVals, newIndices,
			      instance.numAttributes()));
      } else {
	double[] tempVals = new double[num];
	int[] tempInd = new int[num];
	System.arraycopy(newVals, 0, tempVals, 0, num);
	System.arraycopy(newIndices, 0, tempInd, 0, num);
	push(new SparseInstance(instance.weight(), tempVals, tempInd,
				instance.numAttributes()));
      }
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
 	Filter.batchFilterFile(new ReplaceMissingValuesFilter(), argv);
      } else {
	Filter.filterFile(new ReplaceMissingValuesFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








