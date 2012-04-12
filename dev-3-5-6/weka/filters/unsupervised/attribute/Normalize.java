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
 *    Normalize.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.Capabilities.Capability;
import weka.filters.UnsupervisedFilter;

/** 
 <!-- globalinfo-start -->
 * Normalizes all numeric values in the given dataset (apart from the class attribute, if set). The resulting values are in [0,1] for the data used to compute the normalization intervals.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -unset-class-temporarily
 *  Unsets the class index temporarily before the filter is
 *  applied to the data.
 *  (default: no)</pre>
 * 
 <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.8 $
 */
public class Normalize 
  extends PotentialClassIgnorer 
  implements UnsupervisedFilter {
  
  /** for serialization */
  static final long serialVersionUID = -8158531150984362898L;

  /** The minimum values for numeric attributes. */
  private double [] m_MinArray;
  
  /** The maximum values for numeric attributes. */
  private double [] m_MaxArray;

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Normalizes all numeric values in the given dataset (apart from the "
      + "class attribute, if set). The resulting values "
      + "are in [0,1] for the data used to compute the normalization intervals. ";
  }

  /** 
   * Returns the Capabilities of this filter.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enableAllAttributes();
    result.enable(Capability.MISSING_VALUES);
    
    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);
    
    return result;
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the input format can't be set 
   * successfully
   */
  public boolean setInputFormat(Instances instanceInfo) 
       throws Exception {

    super.setInputFormat(instanceInfo);
    setOutputFormat(instanceInfo);
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
   * @throws IllegalStateException if no input format has been set.
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (m_MinArray == null) {
      bufferInput(instance);
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
   * @throws IllegalStateException if no input structure has been defined
   */
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_MinArray == null) {
      Instances input = getInputFormat();
      // Compute minimums and maximums
      m_MinArray = new double[input.numAttributes()];
      m_MaxArray = new double[input.numAttributes()];
      for (int i = 0; i < input.numAttributes(); i++) {
	m_MinArray[i] = Double.NaN;
      }
      for (int j = 0; j < input.numInstances(); j++) {
	double[] value = input.instance(j).toDoubleArray();
	for (int i = 0; i < input.numAttributes(); i++) {
	  if (input.attribute(i).isNumeric() &&
	      (input.classIndex() != i)) {
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
      for(int i = 0; i < input.numInstances(); i++) {
	convertInstance(input.instance(i));
      }
    } 
    // Free memory
    flushInput();

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Convert a single instance over. The converted instance is 
   * added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstance(Instance instance) {
  
    Instance inst = null;
    if (instance instanceof SparseInstance) {
      double[] newVals = new double[instance.numAttributes()];
      int[] newIndices = new int[instance.numAttributes()];
      double[] vals = instance.toDoubleArray();
      int ind = 0;
      for (int j = 0; j < instance.numAttributes(); j++) {
	double value;
	if (instance.attribute(j).isNumeric() &&
	    (!Instance.isMissingValue(vals[j])) &&
	    (getInputFormat().classIndex() != j)) {
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
      inst = new SparseInstance(instance.weight(), tempVals, tempInd,
                                instance.numAttributes());
    } else {
      double[] vals = instance.toDoubleArray();
      for (int j = 0; j < getInputFormat().numAttributes(); j++) {
	if (instance.attribute(j).isNumeric() &&
	    (!Instance.isMissingValue(vals[j])) &&
	    (getInputFormat().classIndex() != j)) {
	  if (Double.isNaN(m_MinArray[j]) ||
	      (m_MaxArray[j] == m_MinArray[j])) {
	    vals[j] = 0;
	  } else {
	    vals[j] = (vals[j] - m_MinArray[j]) / 
	      (m_MaxArray[j] - m_MinArray[j]);
	  }
	}
      }	
      inst = new Instance(instance.weight(), vals);
    }
    inst.setDataset(instance.dataset());
    push(inst);
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {
    runFilter(new Normalize(), argv);
  }
}
