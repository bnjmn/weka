/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    ClassBalancer.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.supervised.instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.SimpleBatchFilter;
import weka.filters.SupervisedFilter;
import weka.filters.unsupervised.attribute.Discretize;

/**
 * <!-- globalinfo-start -->
 * Reweights the instances in the data so that each class has the same total weight. The total sum of weights accross all instances will be maintained. Only the weights in the first batch of data received by this filter are changed, so it can be used with the FilteredClassifier. If the class is numeric, it is discretized using equal-width discretization to establish pseudo classes for weighting.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start -->
 * Valid options are: <p/>
 *
 * <pre> -num-intervals &lt;positive integer&gt;
 *  The number of discretization intervals to use when the class is numeric (default of weka.attribute.unsupervised.Discretize).</pre>
 *
 * <pre> -output-debug-info
 *  If set, filter is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, filter capabilities are not checked when input format is set
 *  (use with caution).</pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank
 * @version $Revision: 10215 $
 */
public class ClassBalancer extends SimpleBatchFilter implements SupervisedFilter,
        WeightedInstancesHandler, WeightedAttributesHandler {

  /** for serialization */
  static final long serialVersionUID = 6237337831221353842L;

  /** number of discretization intervals to use if the class is numeric */
  protected int m_NumIntervals = 10;

  /**
   * Gets the number of discretization intervals to use when the class is numeric.
   *
   * @return the number of discretization intervals
   */
  @OptionMetadata(
          displayName = "Number of discretization intervals",
          description = "The number of discretization intervals to use when the class is numeric.",
          displayOrder = 1,
          commandLineParamName = "num-intervals",
          commandLineParamSynopsis = "-num-intervals <int>",
          commandLineParamIsFlag = false)
  public int getNumIntervals() { return m_NumIntervals; }

  /**
   * Sets the number of discretization intervals to use.
   *
   * @param num the number of discretization intervals to use.
   */
  public void setNumIntervals(int num) { m_NumIntervals = num; }

  /**
   * Returns a string describing this filter.
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "Reweights the instances in the data so that each class has the same total "
            + "weight. The total sum of weights across all instances will be maintained. Only "
            + "the weights in the first batch of data received by this filter are changed, so "
            + "it can be used with the FilteredClassifier. If the class is numeric, the class is "
            + "discretized using equal-width discretization to establish pseudo classes for weighting.";
  }

  /**
   * Determines the output format based on the input format and returns this.
   * 
   * @param inputFormat the input format to base the output format on
   * @return the output format
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat) {
    return new Instances(inputFormat, 0);
  }

  /**
   * Returns the Capabilities of this filter.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enableAllAttributes();
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Processes the given data.
   * 
   * @param instances the data to process
   * @return the modified data
   * @throws Exception in case the processing goes wrong
   */
  @Override
  protected Instances process(Instances instances) throws Exception {

    // Only change first batch of data
    if (isFirstBatchDone()) {
      return new Instances(instances);
    }

    Instances dataToUseForMakingWeights = instances;
    if (instances.classAttribute().isNumeric()) {
      Discretize discretizer = new Discretize();
      discretizer.setBins(m_NumIntervals);
      discretizer.setIgnoreClass(true);
      int[] indices = new int[] {instances.classIndex()};
      discretizer.setAttributeIndicesArray(indices);
      discretizer.setInputFormat(instances);
      dataToUseForMakingWeights = Filter.useFilter(instances, discretizer);
    }

    // Calculate the sum of weights per class and in total
    double[] sumOfWeightsPerClass = new double[dataToUseForMakingWeights.numClasses()];
    for (int i = 0; i < dataToUseForMakingWeights.numInstances(); i++) {
      Instance inst = dataToUseForMakingWeights.instance(i);
      sumOfWeightsPerClass[(int)inst.classValue()] += inst.weight();
    }
    double sumOfWeights = Utils.sum(sumOfWeightsPerClass);

    // Copy data and rescale weights
    Instances result = new Instances(instances);
    double factor = sumOfWeights / (double)dataToUseForMakingWeights.numClasses();
    for (int i = 0; i < result.numInstances(); i++) {
       result.instance(i).setWeight(factor * result.instance(i).weight() /
                         sumOfWeightsPerClass[(int)dataToUseForMakingWeights.instance(i).classValue()]);
    }
    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10215 $");
  }

  /**
   * runs the filter with the given arguments
   * 
   * @param args the commandline arguments
   */
  public static void main(String[] args) {
    runFilter(new ClassBalancer(), args);
  }
}

