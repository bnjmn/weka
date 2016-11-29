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
 *    QDA.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.functions;

import weka.classifiers.AbstractClassifier;

import weka.core.*;
import weka.core.Capabilities.Capability;

import weka.estimators.MultivariateGaussianEstimator;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.RemoveUseless;

import java.util.Collections;
import java.util.Enumeration;

/**
 * <!-- globalinfo-start -->
 * Generates a QDA. The covariance matrices are estimated using maximum likelihood from the per-class data.
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start -->
 * Valid options are: <p/>
 *
 * <pre> -R
 *  The ridge parameter.
 *  (default is 1e-6)</pre>
 *
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 *
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 *
 * <!-- options-end -->
 *
 * @author Eibe Frank, University of Waikato
 * @version $Revision: 10382 $
 */
public class QDA extends AbstractClassifier implements WeightedInstancesHandler {
  
  /** for serialization */
  static final long serialVersionUID = -9113383498193689291L;

  /** Holds header of training date */
  protected Instances m_Data;
  
  /** The per-class estimators */
  protected MultivariateGaussianEstimator[] m_Estimators;

  /** The logs of the prior probabilities */
  protected double[] m_LogPriors;

  /** Ridge parameter */
  protected double m_Ridge = 1e-6;

  /** Rmeove useless filter */
  protected RemoveUseless m_RemoveUseless;

  /**
   * Global info for this classifier.
   */
  public String globalInfo() {
    return "Generates a QDA model. The covariance matrices are estimated using maximum likelihood from the per-class data.";
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String ridgeTipText() {
    return "The value of the ridge parameter.";
  }

  /**
   * Get the value of Ridge.
   *
   * @return Value of Ridge.
   */
  public double getRidge() {

    return m_Ridge;
  }

  /**
   * Set the value of Ridge.
   *
   * @param newRidge Value to assign to Ridge.
   */
  public void setRidge(double newRidge) {

    m_Ridge = newRidge;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {

    java.util.Vector<Option> newVector = new java.util.Vector<Option>(7);

    newVector.addElement(new Option(
            "\tThe ridge parameter.\n"+
                    "\t(default is 1e-6)",
            "R", 0, "-R"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   * <!-- options-start -->
   * Valid options are: <p/>
   *
   * <pre> -R
   *  The ridge parameter.
   *  (default is 1e-6)</pre>
   *
   * <pre> -output-debug-info
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   *
   * <pre> -do-not-check-capabilities
   *  If set, classifier capabilities are not checked before classifier is built
   *  (use with caution).</pre>
   *
   * <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String ridgeString = Utils.getOption('R', options);
    if (ridgeString.length() != 0) {
      setRidge(Double.parseDouble(ridgeString));
    } else {
      setRidge(1e-6);
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of IBk.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions() {

    java.util.Vector<String> options = new java.util.Vector<String>();
    options.add("-R"); options.add("" + getRidge());

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);
    
    return result;
  }

  /**
   * Builds the classifier.
   */
  public void buildClassifier(Instances insts) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(insts);

    // Remove constant attributes
    m_RemoveUseless = new RemoveUseless();
    m_RemoveUseless.setInputFormat(insts);
    insts = Filter.useFilter(insts, m_RemoveUseless);
    insts.deleteWithMissingClass();

    // Establish class counts, etc.
    int[] counts = new int[insts.numClasses()];
    double[] sumOfWeightsPerClass = new double[insts.numClasses()];
    for (int i = 0; i < insts.numInstances(); i++) {
      Instance inst = insts.instance(i);
      int classIndex = (int) inst.classValue();
      counts[classIndex]++;
      sumOfWeightsPerClass[classIndex] += inst.weight();
    }

    // Collect relevant data into array
    double[][][] data = new double[insts.numClasses()][][];
    double[][] weights = new double[insts.numClasses()][];
    for (int i = 0; i < insts.numClasses(); i++) {
      data[i] = new double[counts[i]][insts.numAttributes() - 1];
      weights[i] = new double[counts[i]];
    }
    int[] currentCount = new int[insts.numClasses()];
    for (int i = 0; i < insts.numInstances(); i++) {
      Instance inst = insts.instance(i);
      int classIndex = (int) inst.classValue();
      weights[classIndex][currentCount[classIndex]] = inst.weight();
      int index = 0;
      double[] row = data[classIndex][currentCount[classIndex]++];
      for (int j = 0; j < inst.numAttributes(); j++) {
        if (j != insts.classIndex()) {
          row[index++] = inst.value(j);
        }
      }
    }

    // Establish estimator for each class
    m_Estimators = new MultivariateGaussianEstimator[insts.numClasses()];
    for (int i = 0; i < insts.numClasses(); i++) {
      if (sumOfWeightsPerClass[i] > 0) {
        m_Estimators[i] = new MultivariateGaussianEstimator();
        m_Estimators[i].setRidge(getRidge());
        m_Estimators[i].estimate(data[i], weights[i]);
      }
    }

    // Establish prior probabilities for each class
    m_LogPriors = new double[insts.numClasses()];
    double sumOfWeights = Utils.sum(sumOfWeightsPerClass);
    for (int i = 0; i < insts.numClasses(); i++) {
      if (sumOfWeightsPerClass[i] > 0) {
        m_LogPriors[i] = Math.log(sumOfWeightsPerClass[i]) - Math.log(sumOfWeights);
      }
    }

    // Store header only
    m_Data = new Instances(insts, 0);
  }   
    
  /**
   * Output class probabilities using Bayes' rule.
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
    
    // Filter instance
    m_RemoveUseless.input(inst);
    inst = m_RemoveUseless.output();
    
    // Convert instance to array
    double[] values = new double[inst.numAttributes() - 1];
    int index = 0;
    for (int i = 0; i < m_Data.numAttributes(); i++) {
      if (i != m_Data.classIndex()) {
        values[index++] = inst.value(i);
      }
    }
    double[] posteriorProbs = new double[m_Data.numClasses()];
    for (int i = 0; i < m_Data.numClasses(); i++) {
      if (m_Estimators[i] != null) {
        posteriorProbs[i] = m_Estimators[i].logDensity(values) + m_LogPriors[i];
      } else {
        posteriorProbs[i] = -Double.MAX_VALUE;
      }
    }
    posteriorProbs = Utils.logs2probs(posteriorProbs);
    return posteriorProbs;
  }

  /**
   * Produces textual description of the classifier.
   * @return the textual description
   */
  public String toString() {

    if (m_LogPriors == null) {
      return "No model has been built yet.";
    }
    StringBuffer result = new StringBuffer();
    result.append("QDA model (multivariate Gaussian for each class)\n\n");

    for (int i = 0; i < m_Data.numClasses(); i++) {
      if (m_Estimators[i] != null) {
        result.append("Estimates for class " + m_Data.classAttribute().value(i) + "\n\n");
        result.append("Natural logarithm of class prior probability: " +
                Utils.doubleToString(m_LogPriors[i], getNumDecimalPlaces()) + "\n");
        result.append("Class prior probability: " +
                Utils.doubleToString(Math.exp(m_LogPriors[i]), getNumDecimalPlaces()) + "\n\n");
        result.append("Multivariate Gaussian estimator:\n\n" + m_Estimators[i] + "\n");
      }
    }
    return result.toString();
  }  


  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
    public String getRevision() {
    return RevisionUtils.extract("$Revision: 10382 $");
  }
  
  /**
   * Generates an QDA classifier.
   * 
   * @param argv the options
   */
  public static void main(String [] argv){  
    runClassifier(new QDA(), argv);
  }
}

