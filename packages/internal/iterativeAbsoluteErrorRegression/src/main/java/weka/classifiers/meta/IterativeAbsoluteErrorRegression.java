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
 *    IterativeAbsoluteErrorRegression.java
 *    Copyright (C) 2015-16 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Random;

/**
 * <!-- globalinfo-start -->
 * Iteratively fits a regression model by attempting to minimize absolute error, using a base learner that minimizes weighted squared error.<br>
 * <br>
 * Weights are bounded from below by 1.0 / Utils.SMALL.<br>
 * <br>
 * Resamples data based on weights if base learner is not a WeightedInstancesHandler.<br>
 * <br>
 * For more information see:<br>
 * <br>
 * E. J. Schlossmacher (1973). An Iterative Technique for Absolute Deviations Curve Fitting. Journal of the American Statistical Association. 68(344).
 * <br><br>
 * <!-- globalinfo-end -->
 *
 * <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Schlossmacher1973,
 *    author = {E. J. Schlossmacher},
 *    journal = {Journal of the American Statistical Association},
 *    number = {344},
 *    title = {An Iterative Technique for Absolute Deviations Curve Fitting},
 *    volume = {68},
 *    year = {1973}
 * }
 * </pre>
 * <br><br>
 * <!-- technical-bibtex-end -->
 *
 * <!-- options-start -->
 * Valid options are: <br><br>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.DecisionStump)</pre>
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
 * Options specific to classifier weka.classifiers.trees.DecisionStump:
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
 * <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 11192 $
 */
public class IterativeAbsoluteErrorRegression extends RandomizableSingleClassifierEnhancer
        implements TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -2368837579670527151L;

  /** The number of iterations that have been performed **/
  protected int m_NumIterations = -1;

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Iteratively fits a regression model by attempting to minimize absolute error, using"
            + "a base learner that minimizes weighted squared error.\n\n"
            + "Weights are bounded from below by 1.0 / Utils.SMALL.\n\n"
            + "Resamples data based on weights if base learner is not a WeightedInstancesHandler.\n\n"
      +"For more information see:\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   *
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "E. J. Schlossmacher");
    result.setValue(Field.YEAR, "1973");
    result.setValue(Field.TITLE, "An Iterative Technique for Absolute Deviations Curve Fitting");
    result.setValue(Field.JOURNAL, "Journal of the American Statistical Association");
    result.setValue(Field.VOLUME, "68");
    result.setValue(Field.NUMBER, "344");

    return result;
  }

  /**
   * Default constructor specifying DecisionStump as the base classifier
   */
  public IterativeAbsoluteErrorRegression() {

    this(new weka.classifiers.trees.DecisionStump());
  }

  /**
   * Constructor which takes base classifier as argument.
   *
   * @param classifier the base classifier to use
   */
  public IterativeAbsoluteErrorRegression(Classifier classifier) {

    m_Classifier = classifier;
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.trees.DecisionStump";
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    
    return result;
  }

  /**
   * Method used to build the classifier.
   */
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class and make new Instance objects
    data = new Instances(data);
    data.deleteWithMissingClass();

    // Set all weights to one initially
    for (Instance inst : data) {
      inst.setWeight(1.0);
    }

    // Build initial classifier, collect predictions and compute initial error
    Classifier classifier = AbstractClassifier.makeCopy(getClassifier());
    classifier.buildClassifier(data);
    double[] residuals = new double[data.numInstances()];
    double oldError = 0;
    if ((classifier instanceof BatchPredictor) &&
            ((BatchPredictor)classifier).implementsMoreEfficientBatchPrediction()) {
      double[][] p = ((BatchPredictor) classifier).distributionsForInstances(data);
      for (int i = 0; i < residuals.length; i++) {
        residuals[i] = Math.abs(p[i][0] - data.instance(i).classValue());
        oldError += residuals[i];
      }
    } else {
      for (int i = 0; i < residuals.length; i++) {
        residuals[i] = Math.abs(classifier.classifyInstance(data.instance(i)) - data.instance(i).classValue());
        oldError += residuals[i];
      }
    }

    // Calculate absolute error for mean predictor
    double mean = data.meanOrMode(data.classIndex());
    double AEforMean = 0;
    for (int i = 0; i < data.numInstances(); i++) {
      AEforMean += Math.abs(mean - data.instance(i).classValue());
    }

    // Compute relative absolute error
    oldError /= AEforMean;

    if (getDebug()) {
      System.err.println("Initial relative absolute error: " + oldError);
    }

    // Get random number generator in case we need one
    Random random = data.getRandomNumberGenerator(getSeed());

    // Loop until convergence
    double newError = 0;
    Classifier savedClassifier = classifier;
    m_NumIterations = 1;
    while (true) {

      // Establish weights based on residuals, using Schlossmacher's method
      double sumOfWeights = 0;
      for (int i = 0; i < residuals.length; i++) {
        double weight = (Math.abs(residuals[i]) > Utils.SMALL) ? 1.0 / Math.abs(residuals[i]) : 1.0 / Utils.SMALL;
        data.instance(i).setWeight(weight);
        sumOfWeights += weight;
      }

      // Rescale weights so that their sum is equal to the number of instances
      for (int i = 0; i < residuals.length; i++) {
        data.instance(i).setWeight((double) data.numInstances() * data.instance(i).weight() / sumOfWeights);
      }

      // Build the classifier
      classifier = AbstractClassifier.makeCopy(getClassifier());
      if (!(classifier instanceof WeightedInstancesHandler)) {
        classifier.buildClassifier(data.resampleWithWeights(random));
      } else {
        classifier.buildClassifier(data);
      }

      // Collect the new residuals
      newError = 0;
      if ((classifier instanceof BatchPredictor) &&
              ((BatchPredictor)classifier).implementsMoreEfficientBatchPrediction()) {
        double[][] p = ((BatchPredictor) classifier).distributionsForInstances(data);
        for (int i = 0; i < residuals.length; i++) {
          residuals[i] = Math.abs(p[i][0] - data.instance(i).classValue());
          newError += residuals[i];
        }
      } else {
        for (int i = 0; i < residuals.length; i++) {
          residuals[i] = Math.abs(classifier.classifyInstance(data.instance(i)) - data.instance(i).classValue());
          newError += residuals[i];
        }
      }


      // Compute relative absolute error
      newError /= AEforMean;

      if (getDebug()) {
        System.err.println("Relative absolute error in iteration " + m_NumIterations + ": " + newError);
      }

      // Terminate if necessary
      if (oldError <= newError + Utils.SMALL) {
        break;
      }

      // Save information
      oldError = newError;
      savedClassifier = classifier;
      m_NumIterations++;
    }
    m_Classifier = savedClassifier;
  }

  /**
   * Classify an instance.
   *
   * @param inst the instance to predict
   * @return a prediction for the instance
   * @throws Exception if an error occurs
   */
  public double classifyInstance(Instance inst) throws Exception {

    return getClassifier().classifyInstance(inst);
  }

  /**
   * Returns true if the base classifier implements BatchPredictor and is able
   * to generate batch predictions efficiently
   *
   * @return true if the base classifier can generate batch predictions
   *         efficiently
   */
  public boolean implementsMoreEfficientBatchPrediction() {
    if (!(getClassifier() instanceof BatchPredictor)) {
      return super.implementsMoreEfficientBatchPrediction();
    }

    return ((BatchPredictor) getClassifier())
            .implementsMoreEfficientBatchPrediction();
  }

  /**
   * Returns the distributions for a set of instances. Calls
   * base classifier's distributionForInstance() if it does not support
   * distributionForInstances().
   *
   * @param insts the instances to compute the distribution for
   * @return the class distributions for the given instances
   * @throws Exception if the distribution can't be computed successfully
   */
  @Override
  public double[][] distributionsForInstances(Instances insts) throws Exception {

    if (getClassifier() instanceof BatchPredictor) {
      return ((weka.core.BatchPredictor) getClassifier()).distributionsForInstances(insts);
    }
    return super.distributionsForInstances(insts);
  }

  /**
   * Returns textual description of the classifier.
   *
   * @return a description of the classifier as a string
   */
  public String toString() {

    if (m_NumIterations == -1) {
      return "Classifier hasn't been built yet!";
    }
    StringBuffer text = new StringBuffer();
    text.append("Iterative Absolute Error Regression (" + m_NumIterations + ")\n\n");
    text.append("Final model\n\n" + getClassifier());
    return text.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11192 $");
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {
    runClassifier(new IterativeAbsoluteErrorRegression(), argv);
  }
}

