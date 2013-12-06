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
 *    RealAdaBoost.java
 *    Copyright (C) 1999, 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.Classifier;
import weka.classifiers.RandomizableIteratedSingleClassifierEnhancer;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Randomizable;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

/**
 * <!-- globalinfo-start --> Class for boosting a 2-class classifier using the
 * Real Adaboost method.<br/>
 * <br/>
 * For more information, see<br/>
 * <br/>
 * J. Friedman, T. Hastie, R. Tibshirani (2000). Additive Logistic Regression: a
 * Statistical View of Boosting. Annals of Statistics. 95(2):337-407.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;article{Friedman2000,
 *    author = {J. Friedman and T. Hastie and R. Tibshirani},
 *    journal = {Annals of Statistics},
 *    number = {2},
 *    pages = {337-407},
 *    title = {Additive Logistic Regression: a Statistical View of Boosting},
 *    volume = {95},
 *    year = {2000}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -P &lt;num&gt;
 *  Percentage of weight mass to base training on.
 *  (default 100, reduce to around 90 speed up)
 * </pre>
 * 
 * <pre>
 * -Q
 *  Use resampling for boosting.
 * </pre>
 * 
 * <pre>
 * -H &lt;num&gt;
 *  Shrinkage parameter.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -I &lt;num&gt;
 *  Number of iterations.
 *  (default 10)
 * </pre>
 * 
 * <pre>
 * -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.DecisionStump)
 * </pre>
 * 
 * <pre>
 * Options specific to classifier weka.classifiers.trees.DecisionStump:
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * Options after -- are passed to the designated classifier.
 * <p>
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class RealAdaBoost extends RandomizableIteratedSingleClassifierEnhancer
  implements WeightedInstancesHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -7378109809933197974L;

  /** The number of successfully generated base classifiers. */
  protected int m_NumIterationsPerformed;

  /** Weight Threshold. The percentage of weight mass used in training */
  protected int m_WeightThreshold = 100;

  /** The value of the shrinkage parameter */
  protected double m_Shrinkage = 1;

  /** Use boosting with reweighting? */
  protected boolean m_UseResampling;

  /** a ZeroR model in case no model can be built from the data */
  protected Classifier m_ZeroR;

  /** Sum of weights on training data */
  protected double m_SumOfWeights;

  /**
   * Constructor.
   */
  public RealAdaBoost() {

    m_Classifier = new weka.classifiers.trees.DecisionStump();
  }

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {

    return "Class for boosting a 2-class classifier using the Real Adaboost method.\n\n"
      + "For more information, see\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.ARTICLE);
    result
      .setValue(Field.AUTHOR, "J. Friedman and T. Hastie and R. Tibshirani");
    result.setValue(Field.TITLE,
      "Additive Logistic Regression: a Statistical View of Boosting");
    result.setValue(Field.JOURNAL, "Annals of Statistics");
    result.setValue(Field.VOLUME, "95");
    result.setValue(Field.NUMBER, "2");
    result.setValue(Field.PAGES, "337-407");
    result.setValue(Field.YEAR, "2000");

    return result;
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  @Override
  protected String defaultClassifierString() {

    return "weka.classifiers.trees.DecisionStump";
  }

  /**
   * Select only instances with weights that contribute to the specified
   * quantile of the weight distribution
   * 
   * @param data the input instances
   * @param quantile the specified quantile eg 0.9 to select 90% of the weight
   *          mass
   * @return the selected instances
   */
  protected Instances selectWeightQuantile(Instances data, double quantile) {

    int numInstances = data.numInstances();
    Instances trainData = new Instances(data, numInstances);
    double[] weights = new double[numInstances];

    double sumOfWeights = 0;
    for (int i = 0; i < numInstances; i++) {
      weights[i] = data.instance(i).weight();
      sumOfWeights += weights[i];
    }
    double weightMassToSelect = sumOfWeights * quantile;
    int[] sortedIndices = Utils.sort(weights);

    // Select the instances
    sumOfWeights = 0;
    for (int i = numInstances - 1; i >= 0; i--) {
      Instance instance = (Instance) data.instance(sortedIndices[i]).copy();
      trainData.add(instance);
      sumOfWeights += weights[sortedIndices[i]];
      if ((sumOfWeights > weightMassToSelect) && (i > 0)
        && (weights[sortedIndices[i]] != weights[sortedIndices[i - 1]])) {
        break;
      }
    }
    if (m_Debug) {
      System.err.println("Selected " + trainData.numInstances() + " out of "
        + numInstances);
    }
    return trainData;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>();

    newVector.addElement(new Option(
      "\tPercentage of weight mass to base training on.\n"
        + "\t(default 100, reduce to around 90 speed up)", "P", 1, "-P <num>"));

    newVector.addElement(new Option("\tUse resampling for boosting.", "Q", 0,
      "-Q"));

    newVector.addElement(new Option("\tShrinkage parameter.\n"
      + "\t(default 1)", "H", 1, "-H <num>"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -P &lt;num&gt;
   *  Percentage of weight mass to base training on.
   *  (default 100, reduce to around 90 speed up)
   * </pre>
   * 
   * <pre>
   * -Q
   *  Use resampling for boosting.
   * </pre>
   * 
   * <pre>
   * -H &lt;num&gt;
   *  Shrinkage parameter.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -I &lt;num&gt;
   *  Number of iterations.
   *  (default 10)
   * </pre>
   * 
   * <pre>
   * -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.DecisionStump)
   * </pre>
   * 
   * <pre>
   * Options specific to classifier weka.classifiers.trees.DecisionStump:
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * Options after -- are passed to the designated classifier.
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String thresholdString = Utils.getOption('P', options);
    if (thresholdString.length() != 0) {
      setWeightThreshold(Integer.parseInt(thresholdString));
    } else {
      setWeightThreshold(100);
    }

    String shrinkageString = Utils.getOption('H', options);
    if (shrinkageString.length() != 0) {
      setShrinkage(new Double(shrinkageString).doubleValue());
    } else {
      setShrinkage(1.0);
    }

    setUseResampling(Utils.getFlag('Q', options));

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    if (getUseResampling()) {
      result.add("-Q");
    }

    result.add("-P");
    result.add("" + getWeightThreshold());

    result.add("-H");
    result.add("" + getShrinkage());

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String shrinkageTipText() {
    return "Shrinkage parameter (use small value like 0.1 to reduce "
      + "overfitting).";
  }

  /**
   * Get the value of Shrinkage.
   * 
   * @return Value of Shrinkage.
   */
  public double getShrinkage() {

    return m_Shrinkage;
  }

  /**
   * Set the value of Shrinkage.
   * 
   * @param newShrinkage Value to assign to Shrinkage.
   */
  public void setShrinkage(double newShrinkage) {

    m_Shrinkage = newShrinkage;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String weightThresholdTipText() {
    return "Weight threshold for weight pruning.";
  }

  /**
   * Set weight threshold
   * 
   * @param threshold the percentage of weight mass used for training
   */
  public void setWeightThreshold(int threshold) {

    m_WeightThreshold = threshold;
  }

  /**
   * Get the degree of weight thresholding
   * 
   * @return the percentage of weight mass used for training
   */
  public int getWeightThreshold() {

    return m_WeightThreshold;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String useResamplingTipText() {
    return "Whether resampling is used instead of reweighting.";
  }

  /**
   * Set resampling mode
   * 
   * @param r true if resampling should be done
   */
  public void setUseResampling(boolean r) {

    m_UseResampling = r;
  }

  /**
   * Get whether resampling is turned on
   * 
   * @return true if resampling output is on
   */
  public boolean getUseResampling() {

    return m_UseResampling;
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    if (super.getCapabilities().handles(Capability.BINARY_CLASS)) {
      result.enable(Capability.BINARY_CLASS);
    }

    return result;
  }

  /**
   * Boosting method.
   * 
   * @param data the training data to be used for generating the boosted
   *          classifier.
   * @throws Exception if the classifier could not be built successfully
   */

  @Override
  public void buildClassifier(Instances data) throws Exception {

    super.buildClassifier(data);

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    m_SumOfWeights = data.sumOfWeights();

    if ((!m_UseResampling)
      && (m_Classifier instanceof WeightedInstancesHandler)) {
      buildClassifierWithWeights(data);
    } else {
      buildClassifierUsingResampling(data);
    }
  }

  /**
   * Boosting method. Boosts using resampling
   * 
   * @param data the training data to be used for generating the boosted
   *          classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  protected void buildClassifierUsingResampling(Instances data)
    throws Exception {

    Instances trainData, sample, training, trainingWeightsNotNormalized;
    int numInstances = data.numInstances();
    Random randomInstance = new Random(m_Seed);
    double minLoss = Double.MAX_VALUE;

    // Create a copy of the data so that when the weights are diddled
    // with it doesn't mess up the weights for anyone else
    trainingWeightsNotNormalized = new Instances(data, 0, numInstances);

    // Do boostrap iterations
    for (m_NumIterationsPerformed = -1; m_NumIterationsPerformed < m_Classifiers.length; m_NumIterationsPerformed++) {
      if (m_Debug) {
        System.err.println("Training classifier "
          + (m_NumIterationsPerformed + 1));
      }

      training = new Instances(trainingWeightsNotNormalized);
      normalizeWeights(training, 1.0);

      // Select instances to train the classifier on
      if (m_WeightThreshold < 100) {
        trainData = selectWeightQuantile(training,
          (double) m_WeightThreshold / 100);
      } else {
        trainData = new Instances(training);
      }

      // Resample
      double[] weights = new double[trainData.numInstances()];
      for (int i = 0; i < weights.length; i++) {
        weights[i] = trainData.instance(i).weight();
      }

      sample = trainData.resampleWithWeights(randomInstance, weights);

      // Build classifier
      if (m_NumIterationsPerformed == -1) {
        m_ZeroR = new weka.classifiers.rules.ZeroR();
        m_ZeroR.buildClassifier(data);
      } else {
        m_Classifiers[m_NumIterationsPerformed].buildClassifier(sample);
      }

      // Update instance weights
      setWeights(trainingWeightsNotNormalized, m_NumIterationsPerformed);

      // Has progress been made?
      double loss = 0;
      for (Instance inst : trainingWeightsNotNormalized) {
        loss += Math.log(inst.weight());
      }
      if (m_Debug) {
        System.err.println("Current loss on log scale: " + loss);
      }
      if ((m_NumIterationsPerformed > -1) && (loss > minLoss)) {
        if (m_Debug) {
          System.err.println("Loss has increased: bailing out.");
        }
        break;
      }
      minLoss = loss;
    }
  }

  /**
   * Sets the weights for the next iteration.
   * 
   * @param training the training instances
   * @throws Exception if something goes wrong
   */
  protected void setWeights(Instances training, int iteration) throws Exception {

    for (Instance instance : training) {
      double reweight = 1;
      double prob = 1, shrinkage = m_Shrinkage;

      if (iteration == -1) {
        prob = m_ZeroR.distributionForInstance(instance)[0];
        shrinkage = 1.0;
      } else {
        prob = m_Classifiers[iteration].distributionForInstance(instance)[0];

        // Make sure that probabilities are never 0 or 1 using ad-hoc smoothing
        prob = (m_SumOfWeights * prob + 1) / (m_SumOfWeights + 2);
      }

      if (instance.classValue() == 1) {
        reweight = shrinkage * 0.5 * (Math.log(prob) - Math.log(1 - prob));
      } else {
        reweight = shrinkage * 0.5 * (Math.log(1 - prob) - Math.log(prob));
      }
      instance.setWeight(instance.weight() * Math.exp(reweight));
    }
  }

  /**
   * Normalize the weights for the next iteration.
   * 
   * @param training the training instances
   * @throws Exception if something goes wrong
   */
  protected void normalizeWeights(Instances training, double oldSumOfWeights)
    throws Exception {

    // Renormalize weights
    double newSumOfWeights = training.sumOfWeights();
    for (Instance instance : training) {
      instance.setWeight(instance.weight() * oldSumOfWeights / newSumOfWeights);
    }
  }

  /**
   * Boosting method. Boosts any classifier that can handle weighted instances.
   * 
   * @param data the training data to be used for generating the boosted
   *          classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  protected void buildClassifierWithWeights(Instances data) throws Exception {

    Instances trainData, training, trainingWeightsNotNormalized;
    int numInstances = data.numInstances();
    Random randomInstance = new Random(m_Seed);
    double minLoss = Double.MAX_VALUE;

    // Create a copy of the data so that when the weights are diddled
    // with it doesn't mess up the weights for anyone else
    trainingWeightsNotNormalized = new Instances(data, 0, numInstances);

    // Do boostrap iterations
    for (m_NumIterationsPerformed = -1; m_NumIterationsPerformed < m_Classifiers.length; m_NumIterationsPerformed++) {
      if (m_Debug) {
        System.err.println("Training classifier "
          + (m_NumIterationsPerformed + 1));
      }

      training = new Instances(trainingWeightsNotNormalized);
      normalizeWeights(training, m_SumOfWeights);

      // Select instances to train the classifier on
      if (m_WeightThreshold < 100) {
        trainData = selectWeightQuantile(training,
          (double) m_WeightThreshold / 100);
      } else {
        trainData = new Instances(training, 0, numInstances);
      }

      // Build classifier
      if (m_NumIterationsPerformed == -1) {
        m_ZeroR = new weka.classifiers.rules.ZeroR();
        m_ZeroR.buildClassifier(data);
      } else {
        if (m_Classifiers[m_NumIterationsPerformed] instanceof Randomizable) {
          ((Randomizable) m_Classifiers[m_NumIterationsPerformed])
            .setSeed(randomInstance.nextInt());
        }
        m_Classifiers[m_NumIterationsPerformed].buildClassifier(trainData);
      }

      // Update instance weights
      setWeights(trainingWeightsNotNormalized, m_NumIterationsPerformed);

      // Has progress been made?
      double loss = 0;
      for (Instance inst : trainingWeightsNotNormalized) {
        loss += Math.log(inst.weight());
      }
      if (m_Debug) {
        System.err.println("Current loss on log scale: " + loss);
      }
      if ((m_NumIterationsPerformed > -1) && (loss > minLoss)) {
        if (m_Debug) {
          System.err.println("Loss has increased: bailing out.");
        }
        break;
      }
      minLoss = loss;
    }
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   * 
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if instance could not be classified successfully
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {

    double[] sums = new double[instance.numClasses()];
    for (int i = -1; i < m_NumIterationsPerformed; i++) {
      double prob = 1, shrinkage = m_Shrinkage;
      if (i == -1) {
        prob = m_ZeroR.distributionForInstance(instance)[0];
        shrinkage = 1.0;
      } else {
        prob = m_Classifiers[i].distributionForInstance(instance)[0];

        // Make sure that probabilities are never 0 or 1 using ad-hoc smoothing
        prob = (m_SumOfWeights * prob + 1) / (m_SumOfWeights + 2);
      }
      sums[0] += shrinkage * 0.5 * (Math.log(prob) - Math.log(1 - prob));
    }
    sums[1] = -sums[0];
    return Utils.logs2probs(sums);
  }

  /**
   * Returns description of the boosted classifier.
   * 
   * @return description of the boosted classifier as a string
   */
  @Override
  public String toString() {

    StringBuffer text = new StringBuffer();

    if (m_ZeroR == null) {
      text.append("No model built yet.\n\n");
    } else {
      text.append("RealAdaBoost: Base classifiers: \n\n");
      text.append(m_ZeroR.toString() + "\n\n");
      for (int i = 0; i < m_NumIterationsPerformed; i++) {
        text.append(m_Classifiers[i].toString() + "\n\n");
      }
      text.append("Number of performed Iterations: " + m_NumIterationsPerformed
        + "\n");
    }

    return text.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv the options
   */
  public static void main(String[] argv) {
    runClassifier(new RealAdaBoost(), argv);
  }
}
