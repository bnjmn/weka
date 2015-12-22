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

/**
 * WeightedInstancesHandlerWrapper.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.classifiers.meta;

import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.ResampleUtils;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Generic wrapper around any classifier to enable weighted instances support.<br>
 * Uses resampling with weights if the base classifier is not implementing the weka.core.WeightedInstancesHandler interface and there are instance weights other 1.0 present. By default, the training data is passed through to the base classifier if it can handle instance weights. However, it is possible to force the use of resampling with weights as well.
 * <br><br>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -force-resample-with-weights
 *  Forces resampling of weights, regardless of whether
 *  base classifier handles instance weights</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.rules.ZeroR)</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 * <pre> -num-decimal-places
 *  The number of decimal places for the output of numbers in the model (default 2).</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.rules.ZeroR:
 * </pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 * <pre> -num-decimal-places
 *  The number of decimal places for the output of numbers in the model (default 2).</pre>
 * 
 <!-- options-end -->
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class WeightedInstancesHandlerWrapper
  extends RandomizableSingleClassifierEnhancer
  implements WeightedInstancesHandler {

  private static final long serialVersionUID = 2980789213434466135L;

  /** command-line option for resampling with weights. */
  public static final String FORCE_RESAMPLE_WITH_WEIGHTS = "force-resample-with-weights";

  /** whether to force resampling with weights. */
  protected boolean m_ForceResampleWithWeights = false;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return
      "Generic wrapper around any classifier to enable weighted instances support.\n"
      + "Uses resampling with weights if the base classifier is not implementing "
      + "the " + WeightedInstancesHandler.class.getName() + " interface and there "
      + "are instance weights other than 1.0 present. By default, "
      + "the training data is passed through to the base classifier if it can handle "
      + "instance weights. However, it is possible to force the use of resampling "
      + "with weights as well.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {
    Vector<Option> 	result;

    result = new Vector<Option>();

    result.addElement(new Option(
      "\tForces resampling of weights, regardless of whether\n"
	+ "\tbase classifier handles instance weights",
      FORCE_RESAMPLE_WITH_WEIGHTS, 0, "-" + FORCE_RESAMPLE_WITH_WEIGHTS));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    setForceResampleWithWeights(Utils.getFlag(FORCE_RESAMPLE_WITH_WEIGHTS, options));
    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return 		an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    List<String> 	result;

    result = new ArrayList<String>();

    if (getForceResampleWithWeights())
      result.add("-" + FORCE_RESAMPLE_WITH_WEIGHTS);

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Gets the size of each subSpace, as a percentage of the training set size.
   *
   * @return 		the subSpace size, as a percentage.
   */
  public boolean getForceResampleWithWeights() {
    return m_ForceResampleWithWeights;
  }

  /**
   * Sets the size of each subSpace, as a percentage of the training set size.
   *
   * @param value 	the subSpace size, as a percentage.
   */
  public void setForceResampleWithWeights(boolean value) {
    m_ForceResampleWithWeights = value;
  }

  /**
   * Returns the tip text for this property
   *
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String forceResampleWithWeightsTipText() {
    return
      "If enabled, forces the data to be resampled with weights, regardless "
	+ "of whether the base classifier can handle instance weights.";
  }

  /**
   * builds the classifier.
   *
   * @param data 	the training data to be used for generating the
   * 			classifier.
   * @throws Exception 	if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    boolean resample = getForceResampleWithWeights()
      || (!(m_Classifier instanceof WeightedInstancesHandler) && ResampleUtils.hasInstanceWeights(data));

    if (resample) {
      if (getDebug())
	System.err.println(getClass().getName() + ": resampling training data");
      data = data.resampleWithWeights(new Random(m_Seed));
    }

    m_Classifier.buildClassifier(data);
  }

  /**
   * Calculates the class membership probabilities for the given test
   * instance.
   *
   * @param instance 	the instance to be classified
   * @return 		preedicted class probability distribution
   * @throws Exception 	if distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    return m_Classifier.distributionForInstance(instance);
  }

  /**
   * Classifies the given test instance.
   *
   * @param instance 	the instance to be classified
   * @return 		the predicted most likely class for the instance or
   *         		Utils.missingValue() if no prediction is made
   * @throws Exception 	if an error occurred during the prediction
   */
  @Override
  public double classifyInstance(Instance instance) throws Exception {
    return m_Classifier.classifyInstance(instance);
  }

  /**
   * Returns a string description of the model.
   *
   * @return		the model
   */
  public String toString() {
    StringBuilder	result;

    result = new StringBuilder();
    result.append(getClass().getSimpleName()).append("\n");
    result.append(getClass().getSimpleName().replaceAll(".", "=")).append("\n\n");
    result.append("Force resample with weights: " + getForceResampleWithWeights() + "\n");
    result.append("Base classifier:\n");
    result.append("- command-line: " + Utils.toCommandLine(m_Classifier) + "\n");
    result.append("- handles instance weights: " + (m_Classifier instanceof WeightedInstancesHandler) + "\n\n");
    result.append(m_Classifier.toString());

    return result.toString();
  }

  /**
   * Returns the revision string.
   *
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for testing this class.
   *
   * @param args 	the options
   */
  public static void main(String[] args) {
    runClassifier(new RandomSubSpace(), args);
  }
}
