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
 *    ReplaceMissingValues.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */


package weka.filters.unsupervised.attribute;

import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.UnsupervisedFilter;

/** 
 <!-- globalinfo-start -->
 * Replaces all missing values for nominal and numeric attributes in a dataset with the modes and means from the training data.
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
 * @version $Revision: 1.7 $
 */
public class ReplaceMissingValues 
  extends PotentialClassIgnorer
  implements UnsupervisedFilter {

  /** for serialization */
  static final long serialVersionUID = 8349568310991609867L;
  
  /** The modes and means */
  private double[] m_ModesAndMeans = null;

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Replaces all missing values for nominal and numeric attributes in a "
      + "dataset with the modes and means from the training data.";
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
    if (m_ModesAndMeans == null) {
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

    if (m_ModesAndMeans == null) {
      // Compute modes and means
      double sumOfWeights =  getInputFormat().sumOfWeights();
      double[][] counts = new double[getInputFormat().numAttributes()][];
      for (int i = 0; i < getInputFormat().numAttributes(); i++) {
	if (getInputFormat().attribute(i).isNominal()) {
	  counts[i] = new double[getInputFormat().attribute(i).numValues()];
	  counts[i][0] = sumOfWeights;
	}
      }
      double[] sums = new double[getInputFormat().numAttributes()];
      for (int i = 0; i < sums.length; i++) {
	sums[i] = sumOfWeights;
      }
      double[] results = new double[getInputFormat().numAttributes()];
      for (int j = 0; j < getInputFormat().numInstances(); j++) {
	Instance inst = getInputFormat().instance(j);
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
      m_ModesAndMeans = new double[getInputFormat().numAttributes()];
      for (int i = 0; i < getInputFormat().numAttributes(); i++) {
	if (getInputFormat().attribute(i).isNominal()) {
	  m_ModesAndMeans[i] = (double)Utils.maxIndex(counts[i]);
	} else if (getInputFormat().attribute(i).isNumeric()) {
	  if (Utils.gr(sums[i], 0)) {
	    m_ModesAndMeans[i] = results[i] / sums[i];
	  }
	}
      }

      // Convert pending input instances
      for(int i = 0; i < getInputFormat().numInstances(); i++) {
	convertInstance(getInputFormat().instance(i));
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
      double []vals = new double[instance.numValues()];
      int []indices = new int[instance.numValues()];
      int num = 0;
      for (int j = 0; j < instance.numValues(); j++) {
	if (instance.isMissingSparse(j) &&
	    (getInputFormat().classIndex() != instance.index(j)) &&
	    (instance.attributeSparse(j).isNominal() ||
	     instance.attributeSparse(j).isNumeric())) {
	  if (m_ModesAndMeans[instance.index(j)] != 0.0) {
	    vals[num] = m_ModesAndMeans[instance.index(j)];
	    indices[num] = instance.index(j);
	    num++;
	  } 
	} else {
	  vals[num] = instance.valueSparse(j);
	  indices[num] = instance.index(j);
	  num++;
	}
      } 
      if (num == instance.numValues()) {
	inst = new SparseInstance(instance.weight(), vals, indices,
                                  instance.numAttributes());
      } else {
	double []tempVals = new double[num];
	int []tempInd = new int[num];
	System.arraycopy(vals, 0, tempVals, 0, num);
	System.arraycopy(indices, 0, tempInd, 0, num);
	inst = new SparseInstance(instance.weight(), tempVals, tempInd,
                                  instance.numAttributes());
      }
    } else {
      double []vals = new double[getInputFormat().numAttributes()];
      for (int j = 0; j < instance.numAttributes(); j++) {
	if (instance.isMissing(j) &&
	    (getInputFormat().classIndex() != j) &&
	    (getInputFormat().attribute(j).isNominal() ||
	     getInputFormat().attribute(j).isNumeric())) {
	  vals[j] = m_ModesAndMeans[j]; 
	} else {
	  vals[j] = instance.value(j);
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
    runFilter(new ReplaceMissingValues(), argv);
  }
}
