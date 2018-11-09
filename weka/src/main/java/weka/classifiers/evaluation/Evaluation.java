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
 *    Evaluation.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.evaluation;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.ConditionalDensityEstimator;
import weka.classifiers.CostMatrix;
import weka.classifiers.IntervalEstimator;
import weka.classifiers.IterativeClassifier;
import weka.classifiers.Sourcable;
import weka.classifiers.UpdateableBatchProcessor;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.evaluation.output.prediction.AbstractOutput;
import weka.classifiers.evaluation.output.prediction.PlainText;
import weka.classifiers.misc.InputMappedClassifier;
import weka.classifiers.pmml.consumer.PMMLClassifier;
import weka.classifiers.xml.XMLClassifier;
import weka.core.*;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.pmml.PMMLFactory;
import weka.core.pmml.PMMLModel;
import weka.core.xml.KOML;
import weka.core.xml.XMLOptions;
import weka.core.xml.XMLSerialization;
import weka.estimators.UnivariateKernelEstimator;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class for evaluating machine learning models.
 * <p/>
 *
 * -------------------------------------------------------------------
 * <p/>
 *
 * General options when evaluating a learning scheme from the command-line:
 * <p/>
 *
 * -t filename <br/>
 * Name of the file with the training data. (required)
 * <p/>
 *
 * -T filename <br/>
 * Name of the file with the test data. If missing a cross-validation is
 * performed.
 * <p/>
 *
 * -c index <br/>
 * Index of the class attribute (1, 2, ...; default: last).
 * <p/>
 *
 * -x number <br/>
 * The number of folds for the cross-validation (default: 10).
 * <p/>
 *
 * -no-cv <br/>
 * No cross validation. If no test file is provided, no evaluation is done.
 * <p/>
 *
 * -split-percentage percentage <br/>
 * Sets the percentage for the train/test set split, e.g., 66.
 * <p/>
 *
 * -preserve-order <br/>
 * Preserves the order in the percentage split instead of randomizing the data
 * first with the seed value ('-s').
 * <p/>
 *
 * -s seed <br/>
 * Random number seed for the cross-validation and percentage split (default:
 * 1).
 * <p/>
 *
 * -m filename <br/>
 * The name of a file containing a cost matrix.
 * <p/>
 *
 * -disable list <br/>
 * A comma separated list of metric names not to include in the output.
 * <p/>
 *
 * -l filename <br/>
 * Loads classifier from the given file. In case the filename ends with ".xml",
 * a PMML file is loaded or, if that fails, options are loaded from XML.
 * <p/>
 *
 * -d filename <br/>
 * Saves classifier built from the training data into the given file. In case
 * the filename ends with ".xml" the options are saved XML, not the model.
 * <p/>
 *
 * -v <br/>
 * Outputs no statistics for the training data.
 * <p/>
 *
 * -o <br/>
 * Outputs statistics only, not the classifier.
 * <p/>
 *
 * -output-models-for-training-splits <br/>
 * Output models for training splits if cross-validation or percentage-split evaluation is used.
 * <p/>
 *
 * -do-not-output-per-class-statistics <br/>
 * Do not output statistics per class.
 * <p/>
 *
 * -k <br/>
 * Outputs information-theoretic statistics.
 * <p/>
 *
 * -classifications
 * "weka.classifiers.evaluation.output.prediction.AbstractOutput + options" <br/>
 * Uses the specified class for generating the classification output. E.g.:
 * weka.classifiers.evaluation.output.prediction.PlainText or :
 * weka.classifiers.evaluation.output.prediction.CSV
 *
 * -p range <br/>
 * Outputs predictions for test instances (or the train instances if no test
 * instances provided and -no-cv is used), along with the attributes in the
 * specified range (and nothing else). Use '-p 0' if no attributes are desired.
 * <p/>
 * Deprecated: use "-classifications ..." instead.
 * <p/>
 *
 * -distribution <br/>
 * Outputs the distribution instead of only the prediction in conjunction with
 * the '-p' option (only nominal classes).
 * <p/>
 * Deprecated: use "-classifications ..." instead.
 * <p/>
 *
 * -no-predictions <br/>
 * Turns off the collection of predictions in order to conserve memory.
 * <p/>
 *
 * -r <br/>
 * Outputs cumulative margin distribution (and nothing else).
 * <p/>
 *
 * -g <br/>
 * Only for classifiers that implement "Graphable." Outputs the graph
 * representation of the classifier (and nothing else).
 * <p/>
 *
 * -xml filename | xml-string <br/>
 * Retrieves the options from the XML-data instead of the command line.
 * <p/>
 *
 * -threshold-file file <br/>
 * The file to save the threshold data to. The format is determined by the
 * extensions, e.g., '.arff' for ARFF format or '.csv' for CSV.
 * <p/>
 *
 * -threshold-label label <br/>
 * The class label to determine the threshold data for (default is the first
 * label)
 * <p/>
 *
 * -------------------------------------------------------------------
 * <p/>
 *
 * Example usage as the main of a classifier (called FunkyClassifier):
 * <code> <pre>
 * public static void main(String [] args) {
 *   runClassifier(new FunkyClassifier(), args);
 * }
 * </pre> </code>
 * <p/>
 *
 * ------------------------------------------------------------------
 * <p/>
 *
 * Example usage from within an application: <code> <pre>
 * Instances trainInstances = ... instances got from somewhere
 * Instances testInstances = ... instances got from somewhere
 * Classifier scheme = ... scheme got from somewhere
 *
 * Evaluation evaluation = new Evaluation(trainInstances);
 * evaluation.evaluateModel(scheme, testInstances);
 * System.out.println(evaluation.toSummaryString());
 * </pre> </code>
 *
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class Evaluation implements Summarizable, RevisionHandler, Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -7010314486866816271L;

  /**
   * The number of classes.
   */
  protected int m_NumClasses;

  /**
   * The number of folds for a cross-validation.
   */
  protected int m_NumFolds;

  /**
   * The weight of all incorrectly classified instances.
   */
  protected double m_Incorrect;

  /**
   * The weight of all correctly classified instances.
   */
  protected double m_Correct;

  /**
   * The weight of all unclassified instances.
   */
  protected double m_Unclassified;

  /*** The weight of all instances that had no class assigned to them. */
  protected double m_MissingClass;

  /**
   * The weight of all instances that had a class assigned to them.
   */
  protected double m_WithClass;

  /**
   * Array for storing the confusion matrix.
   */
  protected double[][] m_ConfusionMatrix;

  /**
   * The names of the classes.
   */
  protected String[] m_ClassNames;

  /**
   * Is the class nominal or numeric?
   */
  protected boolean m_ClassIsNominal;

  /**
   * The prior probabilities of the classes.
   */
  protected double[] m_ClassPriors;

  /**
   * The sum of counts for priors.
   */
  protected double m_ClassPriorsSum;

  /**
   * The cost matrix (if given).
   */
  protected CostMatrix m_CostMatrix;

  /**
   * The total cost of predictions (includes instance weights).
   */
  protected double m_TotalCost;

  /**
   * Sum of errors.
   */
  protected double m_SumErr;

  /**
   * Sum of absolute errors.
   */
  protected double m_SumAbsErr;

  /**
   * Sum of squared errors.
   */
  protected double m_SumSqrErr;

  /**
   * Sum of class values.
   */
  protected double m_SumClass;

  /**
   * Sum of squared class values.
   */
  protected double m_SumSqrClass;

  /*** Sum of predicted values. */
  protected double m_SumPredicted;

  /**
   * Sum of squared predicted values.
   */
  protected double m_SumSqrPredicted;

  /**
   * Sum of predicted * class values.
   */
  protected double m_SumClassPredicted;

  /**
   * Sum of absolute errors of the prior.
   */
  protected double m_SumPriorAbsErr;

  /**
   * Sum of absolute errors of the prior.
   */
  protected double m_SumPriorSqrErr;

  /**
   * Total Kononenko & Bratko Information.
   */
  protected double m_SumKBInfo;

  /*** Resolution of the margin histogram. */
  protected static int k_MarginResolution = 500;

  /**
   * Cumulative margin distribution.
   */
  protected double m_MarginCounts[];

  /**
   * Number of non-missing class training instances seen.
   */
  protected int m_NumTrainClassVals;

  /**
   * Array containing all numeric training class values seen.
   */
  protected double[] m_TrainClassVals;

  /**
   * Array containing all numeric training class weights.
   */
  protected double[] m_TrainClassWeights;

  /**
   * Numeric class estimator for prior.
   */
  protected UnivariateKernelEstimator m_PriorEstimator;

  /**
   * Whether complexity statistics are available.
   */
  protected boolean m_ComplexityStatisticsAvailable = true;

  /**
   * The minimum probability accepted from an estimator to avoid taking log(0)
   * in Sf calculations.
   */
  protected static final double MIN_SF_PROB = Double.MIN_VALUE;

  /**
   * Total entropy of prior predictions.
   */
  protected double m_SumPriorEntropy;

  /**
   * Total entropy of scheme predictions.
   */
  protected double m_SumSchemeEntropy;

  /**
   * Whether coverage statistics are available.
   */
  protected boolean m_CoverageStatisticsAvailable = true;

  /**
   * The confidence level used for coverage statistics.
   */
  protected double m_ConfLevel = 0.95;

  /**
   * Total size of predicted regions at the given confidence level.
   */
  protected double m_TotalSizeOfRegions;

  /**
   * Total coverage of test cases at the given confidence level.
   */
  protected double m_TotalCoverage;

  /**
   * Minimum target value.
   */
  protected double m_MinTarget;

  /**
   * Maximum target value.
   */
  protected double m_MaxTarget;

  /**
   * The list of predictions that have been generated (for computing AUC).
   */
  protected ArrayList<Prediction> m_Predictions;

  /**
   * enables/disables the use of priors, e.g., if no training set is present in
   * case of de-serialized schemes.
   */
  protected boolean m_NoPriors = false;

  /**
   * The header of the training set.
   */
  protected Instances m_Header;

  /**
   * whether to discard predictions (and save memory).
   */
  protected boolean m_DiscardPredictions;

  /**
   * Holds plugin evaluation metrics
   */
  protected List<AbstractEvaluationMetric> m_pluginMetrics;

  /**
   * The list of metrics to display in the output
   */
  protected List<String> m_metricsToDisplay = new ArrayList<String>();

  public static final String[] BUILT_IN_EVAL_METRICS = {"Correct",
          "Incorrect", "Kappa", "Total cost", "Average cost", "KB relative",
          "KB information", "Correlation", "Complexity 0", "Complexity scheme",
          "Complexity improvement", "MAE", "RMSE", "RAE", "RRSE", "Coverage",
          "Region size", "TP rate", "FP rate", "Precision", "Recall", "F-measure",
          "MCC", "ROC area", "PRC area"};

  /**
   * Utility method to get a list of the names of all built-in and plugin
   * evaluation metrics
   *
   * @return the complete list of available evaluation metrics
   */
  public static List<String> getAllEvaluationMetricNames() {
    List<String> allEvals = new ArrayList<String>();

    for (String s : Evaluation.BUILT_IN_EVAL_METRICS) {
      allEvals.add(s);
    }
    final List<AbstractEvaluationMetric> pluginMetrics =
            AbstractEvaluationMetric.getPluginMetrics();

    if (pluginMetrics != null) {
      for (AbstractEvaluationMetric m : pluginMetrics) {
        if (m instanceof InformationRetrievalEvaluationMetric) {
          List<String> statNames = m.getStatisticNames();
          for (String s : statNames) {
            allEvals.add(s);
          }
        } else {
          allEvals.add(m.getMetricName());
        }
      }
    }

    return allEvals;
  }

  /**
   * Initializes all the counters for the evaluation. Use
   * <code>useNoPriors()</code> if the dataset is the test set and you can't
   * initialize with the priors from the training set via
   * <code>setPriors(Instances)</code>.
   *
   * @param data set of training instances, to get some header information and
   *             prior class distribution information
   * @throws Exception if the class is not defined
   * @see #useNoPriors()
   * @see #setPriors(Instances)
   */
  public Evaluation(Instances data) throws Exception {

    this(data, null);
  }

  /**
   * Initializes all the counters for the evaluation and also takes a cost
   * matrix as parameter. Use <code>useNoPriors()</code> if the dataset is the
   * test set and you can't initialize with the priors from the training set via
   * <code>setPriors(Instances)</code>.
   *
   * @param data       set of training instances, to get some header information and
   *                   prior class distribution information
   * @param costMatrix the cost matrix---if null, default costs will be used
   * @throws Exception if cost matrix is not compatible with data, the class is
   *                   not defined or the class is numeric
   * @see #useNoPriors()
   * @see #setPriors(Instances)
   */
  public Evaluation(Instances data, CostMatrix costMatrix) throws Exception {

    m_Header = new Instances(data, 0);
    m_NumClasses = data.numClasses();
    m_NumFolds = 1;
    m_ClassIsNominal = data.classAttribute().isNominal();

    if (m_ClassIsNominal) {
      m_ConfusionMatrix = new double[m_NumClasses][m_NumClasses];
      m_ClassNames = new String[m_NumClasses];
      for (int i = 0; i < m_NumClasses; i++) {
        m_ClassNames[i] = data.classAttribute().value(i);
      }
    }
    m_CostMatrix = costMatrix;
    if (m_CostMatrix != null) {
      if (!m_ClassIsNominal) {
        throw new Exception("Class has to be nominal if cost matrix given!");
      }
      if (m_CostMatrix.size() != m_NumClasses) {
        throw new Exception("Cost matrix not compatible with data!");
      }
    }
    m_ClassPriors = new double[m_NumClasses];
    setPriors(data);
    m_MarginCounts = new double[k_MarginResolution + 1];

    for (String s : BUILT_IN_EVAL_METRICS) {
      if (!s.equalsIgnoreCase("Coverage") && !s.equalsIgnoreCase("Region size")) {
        m_metricsToDisplay.add(s.toLowerCase());
      }
    }

    m_pluginMetrics = AbstractEvaluationMetric.getPluginMetrics();
    if (m_pluginMetrics != null) {
      for (AbstractEvaluationMetric m : m_pluginMetrics) {
        m.setBaseEvaluation(this);
        if (m instanceof InformationRetrievalEvaluationMetric) {
          List<String> statNames = m.getStatisticNames();
          for (String s : statNames) {
            m_metricsToDisplay.add(s.toLowerCase());
          }
        } else {
          m_metricsToDisplay.add(m.getMetricName().toLowerCase());
        }
      }
    }
  }

  /**
   * Returns the header of the underlying dataset.
   *
   * @return the header information
   */
  public Instances getHeader() {
    return m_Header;
  }

  /**
   * Sets whether to discard predictions, ie, not storing them for future
   * reference via predictions() method in order to conserve memory.
   *
   * @param value true if to discard the predictions
   * @see #predictions()
   */
  public void setDiscardPredictions(boolean value) {
    m_DiscardPredictions = value;
    if (m_DiscardPredictions) {
      m_Predictions = null;
    }
  }

  /**
   * Returns whether predictions are not recorded at all, in order to conserve
   * memory.
   *
   * @return true if predictions are not recorded
   * @see #predictions()
   */
  public boolean getDiscardPredictions() {
    return m_DiscardPredictions;
  }

  /**
   * Returns the list of plugin metrics in use (or null if there are none)
   *
   * @return the list of plugin metrics
   */
  public List<AbstractEvaluationMetric> getPluginMetrics() {
    return m_pluginMetrics;
  }

  /**
   * Set a list of the names of metrics to have appear in the output. The
   * default is to display all built in metrics and plugin metrics that haven't
   * been globally disabled.
   *
   * @param display a list of metric names to have appear in the output
   */
  public void setMetricsToDisplay(List<String> display) {
    // make sure all metric names are lower case for matching
    m_metricsToDisplay.clear();
    for (String s : display) {
      m_metricsToDisplay.add(s.trim().toLowerCase());
    }
  }

  /**
   * Get a list of the names of metrics to have appear in the output The default
   * is to display all built in metrics and plugin metrics that haven't been
   * globally disabled.
   *
   * @return a list of metric names to have appear in the output
   */
  public List<String> getMetricsToDisplay() {
    return m_metricsToDisplay;
  }

  /**
   * Toggle the output of the metrics specified in the supplied list.
   *
   * @param metricsToToggle a list of metrics to toggle
   */
  public void toggleEvalMetrics(List<String> metricsToToggle) {
    for (String s : metricsToToggle) {
      if (m_metricsToDisplay.contains(s.toLowerCase())) {
        m_metricsToDisplay.remove(s.toLowerCase());
      } else {
        m_metricsToDisplay.add(s.toLowerCase());
      }
    }
  }

  /**
   * Get the named plugin evaluation metric
   *
   * @param name the name of the metric (as returned by
   *             AbstractEvaluationMetric.getName()) or the fully qualified class
   *             name of the metric to find
   * @return the metric or null if the metric is not in the list of plugin
   * metrics
   */
  public AbstractEvaluationMetric getPluginMetric(String name) {
    AbstractEvaluationMetric match = null;

    if (m_pluginMetrics != null) {
      for (AbstractEvaluationMetric m : m_pluginMetrics) {
        if (m.getMetricName().equals(name)
                || m.getClass().getName().equals(name)) {
          match = m;
          break;
        }
      }
    }

    return match;
  }

  /**
   * Returns the area under ROC for those predictions that have been collected
   * in the evaluateClassifier(Classifier, Instances) method. Returns
   * Utils.missingValue() if the area is not available.
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the area under the ROC curve or not a number
   */
  public double areaUnderROC(int classIndex) {

    // Check if any predictions have been collected
    if (m_Predictions == null) {
      return Utils.missingValue();
    } else {
      ThresholdCurve tc = new ThresholdCurve();
      Instances result = tc.getCurve(m_Predictions, classIndex);
      return ThresholdCurve.getROCArea(result);
    }
  }

  /**
   * Calculates the weighted (by class size) AUC.
   *
   * @return the weighted AUC.
   */
  public double weightedAreaUnderROC() {
    double[] classCounts = new double[m_NumClasses];
    double classCountSum = 0;

    for (int i = 0; i < m_NumClasses; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        classCounts[i] += m_ConfusionMatrix[i][j];
      }
      classCountSum += classCounts[i];
    }

    double aucTotal = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      double temp = areaUnderROC(i);
      if (classCounts[i] > 0) { // If temp is NaN, we want the sum to also be NaN if count > 0
        aucTotal += (temp * classCounts[i]);
      }
    }

    return aucTotal / classCountSum;
  }

  /**
   * Returns the area under precision-recall curve (AUPRC) for those predictions
   * that have been collected in the evaluateClassifier(Classifier, Instances)
   * method. Returns Utils.missingValue() if the area is not available.
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the area under the precision-recall curve or not a number
   */
  public double areaUnderPRC(int classIndex) {
    // Check if any predictions have been collected
    if (m_Predictions == null) {
      return Utils.missingValue();
    } else {
      ThresholdCurve tc = new ThresholdCurve();
      Instances result = tc.getCurve(m_Predictions, classIndex);
      return ThresholdCurve.getPRCArea(result);
    }
  }

  /**
   * Calculates the weighted (by class size) AUPRC.
   *
   * @return the weighted AUPRC.
   */
  public double weightedAreaUnderPRC() {
    double[] classCounts = new double[m_NumClasses];
    double classCountSum = 0;

    for (int i = 0; i < m_NumClasses; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        classCounts[i] += m_ConfusionMatrix[i][j];
      }
      classCountSum += classCounts[i];
    }

    double auprcTotal = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      double temp = areaUnderPRC(i);
      if (classCounts[i] > 0) { // If temp is NaN, we want the sum to also be NaN if count > 0
        auprcTotal += (temp * classCounts[i]);
      }
    }

    return auprcTotal / classCountSum;
  }

  /**
   * Returns a copy of the confusion matrix.
   *
   * @return a copy of the confusion matrix as a two-dimensional array
   */
  public double[][] confusionMatrix() {

    double[][] newMatrix = new double[m_ConfusionMatrix.length][0];

    for (int i = 0; i < m_ConfusionMatrix.length; i++) {
      newMatrix[i] = new double[m_ConfusionMatrix[i].length];
      System.arraycopy(m_ConfusionMatrix[i], 0, newMatrix[i], 0,
              m_ConfusionMatrix[i].length);
    }
    return newMatrix;
  }

  /**
   * Performs a (stratified if class is nominal) cross-validation for a
   * classifier on a set of instances. Now performs a deep copy of the
   * classifier before each call to buildClassifier() (just in case the
   * classifier is not initialized properly).
   *
   * @param classifier             the classifier with any options set.
   * @param data                   the data on which the cross-validation is to be performed
   * @param numFolds               the number of folds for the cross-validation
   * @param random                 random number generator for randomization
   * @throws Exception if a classifier could not be generated successfully or
   *                   the class is not defined
   */
  public void crossValidateModel(Classifier classifier, Instances data, int numFolds, Random random)
          throws Exception {
    crossValidateModel(classifier, data, numFolds, random, new Object[0]);
  }

  /**
   * Performs a (stratified if class is nominal) cross-validation for a
   * classifier on a set of instances. Performs a deep copy of the
   * classifier before each call to buildClassifier() (just in case the
   * classifier is not initialized properly).
   *
   * @param classifier             the classifier with any options set.
   * @param data                   the data on which the cross-validation is to be performed
   * @param numFolds               the number of folds for the cross-validation
   * @param random                 random number generator for randomization
   * @param forPrinting varargs parameter that, if supplied, is
   *                               expected to hold a
   *                               weka.classifiers.evaluation.output.prediction.AbstractOutput
   *                               object or a StringBuffer for model output
   * @throws Exception if a classifier could not be generated successfully or
   *                   the class is not defined
   */
  public void crossValidateModel(Classifier classifier, Instances data,
                                 int numFolds, Random random, Object... forPrinting)
          throws Exception {

    // Make a copy of the data we can reorder
    data = new Instances(data);
    data.randomize(random);
    if (data.classAttribute().isNominal()) {
      data.stratify(numFolds);
    }

    // We assume that the first element is a
    // weka.classifiers.evaluation.output.prediction.AbstractOutput object
    AbstractOutput classificationOutput = null;
    if (forPrinting.length > 0 && forPrinting[0] instanceof AbstractOutput) {
      // print the header first
      classificationOutput = (AbstractOutput) forPrinting[0];
      classificationOutput.setHeader(data);
      classificationOutput.printHeader();
    }

    // Do the folds
    for (int i = 0; i < numFolds; i++) {
      Instances train = data.trainCV(numFolds, i, random);
      setPriors(train);
      Classifier copiedClassifier = AbstractClassifier.makeCopy(classifier);
      copiedClassifier.buildClassifier(train);
      if (classificationOutput == null && forPrinting.length > 0) {
        ((StringBuffer)forPrinting[0]).append("\n=== Classifier model (training fold " + (i + 1) +") ===\n\n" +
                copiedClassifier);
      }
      Instances test = data.testCV(numFolds, i);
      if (classificationOutput != null){
        evaluateModel(copiedClassifier, test, forPrinting);
      } else {
        evaluateModel(copiedClassifier, test);
      }
    }
    m_NumFolds = numFolds;

    if (classificationOutput != null) {
      classificationOutput.printFooter();
    }
  }

  /**
   * Performs a (stratified if class is nominal) cross-validation for a
   * classifier on a set of instances.
   *
   * @param classifierString a string naming the class of the classifier
   * @param data             the data on which the cross-validation is to be performed
   * @param numFolds         the number of folds for the cross-validation
   * @param options          the options to the classifier. Any options
   * @param random           the random number generator for randomizing the data accepted
   *                         by the classifier will be removed from this array.
   * @throws Exception if a classifier could not be generated successfully or
   *                   the class is not defined
   */
  public void crossValidateModel(String classifierString, Instances data,
                                 int numFolds, String[] options, Random random) throws Exception {

    crossValidateModel(AbstractClassifier.forName(classifierString, options),
            data, numFolds, random);
  }

  /**
   * Evaluates a classifier with the options given in an array of strings.
   * <p/>
   * <p>
   * Valid options are:
   * <p/>
   * <p>
   * -t filename <br/>
   * Name of the file with the training data. (required)
   * <p/>
   * <p>
   * -T filename <br/>
   * Name of the file with the test data. If missing a cross-validation is
   * performed.
   * <p/>
   * <p>
   * -c index <br/>
   * Index of the class attribute (1, 2, ...; default: last).
   * <p/>
   * <p>
   * -x number <br/>
   * The number of folds for the cross-validation (default: 10).
   * <p/>
   * <p>
   * -no-cv <br/>
   * No cross validation. If no test file is provided, no evaluation is done.
   * <p/>
   * <p>
   * -split-percentage percentage <br/>
   * Sets the percentage for the train/test set split, e.g., 66.
   * <p/>
   * <p>
   * -preserve-order <br/>
   * Preserves the order in the percentage split instead of randomizing the data
   * first with the seed value ('-s').
   * <p/>
   * <p>
   * -s seed <br/>
   * Random number seed for the cross-validation and percentage split (default:
   * 1).
   * <p/>
   * <p>
   * -m filename <br/>
   * The name of a file containing a cost matrix.
   * <p/>
   * <p>
   * -l filename <br/>
   * Loads classifier from the given file. In case the filename ends with
   * ".xml",a PMML file is loaded or, if that fails, options are loaded from
   * XML.
   * <p/>
   * <p>
   * -d filename <br/>
   * Saves classifier built from the training data into the given file. In case
   * the filename ends with ".xml" the options are saved XML, not the model.
   * <p/>
   * <p>
   * -v <br/>
   * Outputs no statistics for the training data.
   * <p/>
   * <p>
   * -o <br/>
   * Outputs statistics only, not the classifier.
   * <p/>
   * <p>
   * -output-models-for-training-splits <br/>
   * Output models for training splits if cross-validation or percentage-split evaluation is used.
   * <p/>
   * <p>
   * -do-not-output-per-class-statistics <br/>
   * Do not output statistics per class.
   * <p/>
   * <p>
   * -k <br/>
   * Outputs information-theoretic statistics.
   * <p/>
   * <p>
   * -classifications
   * "weka.classifiers.evaluation.output.prediction.AbstractOutput + options" <br/>
   * Uses the specified class for generating the classification output. E.g.:
   * weka.classifiers.evaluation.output.prediction.PlainText or :
   * weka.classifiers.evaluation.output.prediction.CSV
   * <p>
   * -p range <br/>
   * Outputs predictions for test instances (or the train instances if no test
   * instances provided and -no-cv is used), along with the attributes in the
   * specified range (and nothing else). Use '-p 0' if no attributes are
   * desired.
   * <p/>
   * Deprecated: use "-classifications ..." instead.
   * <p/>
   * <p>
   * -distribution <br/>
   * Outputs the distribution instead of only the prediction in conjunction with
   * the '-p' option (only nominal classes).
   * <p/>
   * Deprecated: use "-classifications ..." instead.
   * <p/>
   * <p>
   * -no-predictions <br/>
   * Turns off the collection of predictions in order to conserve memory.
   * <p/>
   * <p>
   * -r <br/>
   * Outputs cumulative margin distribution (and nothing else).
   * <p/>
   * <p>
   * -g <br/>
   * Only for classifiers that implement "Graphable." Outputs the graph
   * representation of the classifier (and nothing else).
   * <p/>
   * <p>
   * -xml filename | xml-string <br/>
   * Retrieves the options from the XML-data instead of the command line.
   * <p/>
   * <p>
   * -threshold-file file <br/>
   * The file to save the threshold data to. The format is determined by the
   * extensions, e.g., '.arff' for ARFF format or '.csv' for CSV.
   * <p/>
   * <p>
   * -threshold-label label <br/>
   * The class label to determine the threshold data for (default is the first
   * label)
   * <p/>
   *
   * @param classifierString class of machine learning classifier as a string
   * @param options          the array of string containing the options
   * @return a string describing the results
   * @throws Exception if model could not be evaluated successfully
   */
  public static String evaluateModel(String classifierString, String[] options)
          throws Exception {

    Classifier classifier;

    // Create classifier
    try {
      classifier = AbstractClassifier.forName(classifierString, null);
    } catch (Exception e) {
      throw new Exception("Can't find class with name " + classifierString
              + '.');
    }
    return evaluateModel(classifier, options);
  }

  /**
   * A test method for this class. Just extracts the first command line argument
   * as a classifier class name and calls evaluateModel.
   *
   * @param args an array of command line arguments, the first of which must be
   *             the class name of a classifier.
   */
  public static void main(String[] args) {

    try {
      if (args.length == 0) {
        throw new Exception("The first argument must be the class name of a classifier");
      }
      String classifier = args[0];
      args[0] = "";
      System.out.println(evaluateModel(classifier, args));
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }

  /**
   * Tries to get the classifier from the provided model file
   *
   * @param modelFileName the name of the model file
   * @param template      the template header to compare the saved data header to
   * @return the classifier
   */
  protected static Classifier getModelFromFile(String modelFileName, Instances template) throws Exception {

    Classifier classifier = null;

    // Do we have a model file or options in XML?
    if (modelFileName.endsWith(".xml")) {

      // try to load file as PMML first
      try {
        PMMLModel pmmlModel = PMMLFactory.getPMMLModel(modelFileName);
        if (pmmlModel instanceof PMMLClassifier) {
          classifier = ((PMMLClassifier) pmmlModel);
        }
      } catch (Exception ex) {
        throw new IllegalArgumentException("Failed to read model XML file " + modelFileName);
      }
    } else {

      // Try to load (gzipped) serialized Java objects or KOML
      InputStream is = new FileInputStream(modelFileName);
      if (modelFileName.endsWith(".gz")) {
        is = new GZIPInputStream(is);
      }
      if (!modelFileName.endsWith(".koml")) {
        ObjectInputStream objectInputStream = SerializationHelper.getObjectInputStream(is);
        classifier = (Classifier) objectInputStream.readObject();
        // try and read a header (if present)
        Instances savedStructure = null;
        try {
          savedStructure = (Instances) objectInputStream.readObject();
        } catch (Exception ex) {
          // don't make a fuss
        }
        if (savedStructure != null) {
          // test for compatibility with template
          if (!(classifier instanceof InputMappedClassifier) && !template.equalHeaders(savedStructure)) {
            throw new Exception("training and test set are not compatible\n" + template.equalHeadersMsg(savedStructure));
          }
        }
        objectInputStream.close();
      } else if (KOML.isPresent()) {
        BufferedInputStream xmlInputStream = new BufferedInputStream(is);
        classifier = (Classifier) KOML.read(xmlInputStream);
        xmlInputStream.close();
      } else {
        throw new WekaException("KOML library is not present");
      }
    }

    if (classifier == null) {
      throw new IllegalArgumentException("Failed to classifier from model file " + modelFileName);
    }

    return classifier;
  }

  /**
   * Saves the given classifier, along with the template Instances object (if appropriate) to the given file.
   *
   * @param classifier           the classifier
   * @param template             the template
   * @param objectOutputFileName the file name
   */
  protected static void saveClassifier(Classifier classifier, Instances template, String objectOutputFileName)
      throws Exception {

    File f = new File(objectOutputFileName).getAbsoluteFile();
    OutputStream os = new FileOutputStream(f);
    if (!(objectOutputFileName.endsWith(".xml") || (objectOutputFileName.endsWith(".koml") && KOML.isPresent()))) {
      if (objectOutputFileName.endsWith(".gz")) {
        os = new GZIPOutputStream(os);
      }
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);
      objectOutputStream.writeObject(classifier);
      if (template != null) {
        objectOutputStream.writeObject(template);
      }
      objectOutputStream.flush();
      objectOutputStream.close();
    } else {
      BufferedOutputStream xmlOutputStream = new BufferedOutputStream(os);
      if (objectOutputFileName.endsWith(".xml")) {
        XMLSerialization xmlSerial = new XMLClassifier();
        xmlSerial.write(xmlOutputStream, classifier);
      } else
        // whether KOML is present has already been checked
        // if not present -> ".koml" is interpreted as binary - see above
        if (objectOutputFileName.endsWith(".koml")) {
          KOML.write(xmlOutputStream, classifier);
        }
      xmlOutputStream.close();
    }
  }

  /**
   * Evaluates a classifier with the options given in an array of strings.
   * <p/>
   *
   * Valid options are:
   * <p/>
   *
   * -t name of training file <br/>
   * Name of the file with the training data. (required)
   * <p/>
   *
   * -T name of test file <br/>
   * Name of the file with the test data. If missing a cross-validation is
   * performed.
   * <p/>
   *
   * -c class index <br/>
   * Index of the class attribute (1, 2, ...; default: last).
   * <p/>
   *
   * -x number of folds <br/>
   * The number of folds for the cross-validation (default: 10).
   * <p/>
   *
   * -no-cv <br/>
   * No cross validation. If no test file is provided, no evaluation is done.
   * <p/>
   *
   * -split-percentage percentage <br/>
   * Sets the percentage for the train/test set split, e.g., 66.
   * <p/>
   *
   * -preserve-order <br/>
   * Preserves the order in the percentage split instead of randomizing the data
   * first with the seed value ('-s').
   * <p/>
   *
   * -s seed <br/>
   * Random number seed for the cross-validation and percentage split (default:
   * 1).
   * <p/>
   *
   * -m file with cost matrix <br/>
   * The name of a file containing a cost matrix.
   * <p/>
   *
   * -l filename <br/>
   * Loads classifier from the given file. In case the filename ends with
   * ".xml",a PMML file is loaded or, if that fails, options are loaded from
   * XML.
   * <p/>
   *
   * -d filename <br/>
   * Saves classifier built from the training data into the given file. In case
   * the filename ends with ".xml" the options are saved XML, not the model.
   * <p/>
   *
   * -v <br/>
   * Outputs no statistics for the training data.
   * <p/>
   *
   * -o <br/>
   * Outputs statistics only, not the classifier.
   * <p/>
   *
   * -output-models-for-training-splits <br/>
   * Output models for training splits if cross-validation or percentage-split evaluation is used.
   * <p/>
   *
   * -do-not-output-per-class-statistics <br/>
   * Do not output statistics per class.
   * <p/>
   *
   * -k <br/>
   * Outputs information-theoretic statistics.
   * <p/>
   *
   * -classifications
   * "weka.classifiers.evaluation.output.prediction.AbstractOutput + options" <br/>
   * Uses the specified class for generating the classification output. E.g.:
   * weka.classifiers.evaluation.output.prediction.PlainText or :
   * weka.classifiers.evaluation.output.prediction.CSV
   *
   * -p range <br/>
   * Outputs predictions for test instances (or the train instances if no test
   * instances provided and -no-cv is used), along with the attributes in the
   * specified range (and nothing else). Use '-p 0' if no attributes are
   * desired.
   * <p/>
   * Deprecated: use "-classifications ..." instead.
   * <p/>
   *
   * -distribution <br/>
   * Outputs the distribution instead of only the prediction in conjunction with
   * the '-p' option (only nominal classes).
   * <p/>
   * Deprecated: use "-classifications ..." instead.
   * <p/>
   *
   * -no-predictions <br/>
   * Turns off the collection of predictions in order to conserve memory.
   * <p/>
   *
   * -r <br/>
   * Outputs cumulative margin distribution (and nothing else).
   * <p/>
   *
   * -g <br/>
   * Only for classifiers that implement "Graphable." Outputs the graph
   * representation of the classifier (and nothing else).
   * <p/>
   *
   * -xml filename | xml-string <br/>
   * Retrieves the options from the XML-data instead of the command line.
   * <p/>
   *
   * @param classifier machine learning classifier
   * @param options the array of string containing the options
   * @throws Exception if model could not be evaluated successfully
   * @return a string describing the results
   */
  public static String evaluateModel(Classifier classifier, String[] options) throws Exception {

    StringBuffer schemeOptionsText = null;
    long trainTimeStart = 0, trainTimeElapsed = 0, testTimeStart = 0, testTimeElapsed = 0;

    // help requested?
    if (Utils.getFlag("h", options) || Utils.getFlag("help", options)) {

      // global info requested as well?
      boolean globalInfo = Utils.getFlag("synopsis", options) || Utils.getFlag("info", options);

      throw new Exception("\nHelp requested." + makeOptionString(classifier, globalInfo));
    }

    // do we get the input from XML instead of normal command-line parameters?
    try {
      String xml = Utils.getOption("xml", options);
      if (!xml.equals("")) {
        options = new XMLOptions(xml).toArray(); // All other options are ignored
      }
    } catch (Exception ex) {
      throw new Exception("\nWeka exception: " + ex.getMessage() + makeOptionString(classifier, false));
    }


    // Store settings for (almost all) general options
    boolean noCrossValidation = Utils.getFlag("no-cv", options);
    String classIndexString = Utils.getOption('c', options);
    String trainFileName = Utils.getOption('t', options);
    String objectInputFileName = Utils.getOption('l', options);
    String objectOutputFileName = Utils.getOption('d', options);
    String testFileName = Utils.getOption('T', options);
    String foldsString = Utils.getOption('x', options);
    String seedString = Utils.getOption('s', options);
    boolean outputModelsForTrainingSplits = Utils.getFlag("output-models-for-training-splits", options);
    boolean classStatistics = !Utils.getFlag("do-not-output-per-class-statistics", options);
    boolean noOutput = Utils.getFlag('o', options);
    boolean trainStatistics = !Utils.getFlag('v', options);
    boolean printComplexityStatistics = Utils.getFlag('k', options);
    boolean printMargins = Utils.getFlag('r', options);
    boolean printGraph = Utils.getFlag('g', options);
    String sourceClass = Utils.getOption('z', options);
    boolean printSource = (sourceClass.length() != 0);
    String thresholdFile = Utils.getOption("threshold-file", options);
    String thresholdLabel = Utils.getOption("threshold-label", options);
    boolean forceBatchTraining = Utils.getFlag("force-batch-training", options);
    String classifications = Utils.getOption("classifications", options);
    String classificationsOld = Utils.getOption("p", options);
    String splitPercentageString = Utils.getOption("split-percentage", options);
    boolean preserveOrder = Utils.getFlag("preserve-order", options);
    boolean discardPredictions = Utils.getFlag("no-predictions", options);
    String metricsToToggle = Utils.getOption("toggle", options);
    boolean continueIteratingIterative = Utils.getFlag("continue-iterating", options);
    boolean cleanUpIterative = Utils.getFlag("clean-up", options);

    // Some other variables that we might set later.
    CostMatrix costMatrix = null;
    double splitPercentage = -1;
    int classIndex = -1, actualClassIndex = -1;
    int seed = 1, folds = 10;
    Instances train = null, test = null, template = null;
    AbstractOutput classificationOutput = null;
    List<String> toggleList = new ArrayList<String>();
    int labelIndex = 0;

    // We need to output help if something goes wrong with the option settings
    try {
      if (metricsToToggle.length() > 0) {
        String[] parts = metricsToToggle.split(",");
        for (String p : parts) {
          toggleList.add(p.trim().toLowerCase());
        }
      }

      // Read potential .xml model file that may hold scheme-specific options
      if ((objectInputFileName.length() != 0) && (objectInputFileName.endsWith(".xml"))) {
        try { // Try to load scheme-specific options as XMLClassifier
          OptionHandler cl = (OptionHandler) new XMLClassifier().read(objectInputFileName);
          options = Stream.concat(Arrays.stream(cl.getOptions()), Arrays.stream(options)).toArray(String[]::new);
          objectInputFileName = ""; // We have not actually read a built model, only some options
        } catch (Exception ex) {
        }
      }

      // Basic checking for global parameter settings
      if (trainFileName.length() == 0) {
        if (objectInputFileName.length() == 0) {
          throw new IllegalArgumentException("No training file and no object input file given.");
        }
        if (testFileName.length() == 0) {
          throw new IllegalArgumentException("No training file and no test file given.");
        }
      } else if ((objectInputFileName.length() != 0)
              && ((!(classifier instanceof UpdateableClassifier) && !(classifier instanceof IterativeClassifier) || forceBatchTraining) || (testFileName.length() == 0))) {
        throw new IllegalArgumentException("Classifier not incremental or batch training forced, or no test file provided: can't use model file.");
      }
      if ((objectInputFileName.length() != 0) && ((splitPercentageString.length() != 0) || (foldsString.length() != 0))) {
        throw new IllegalArgumentException("Cannot perform percentage split or cross-validation when model provided.");
      }
      if (splitPercentageString.length() != 0) {
        if (foldsString.length() != 0) {
          throw new IllegalArgumentException("Percentage split cannot be used in conjunction with cross-validation ('-x').");
        }
        splitPercentage = Double.parseDouble(splitPercentageString);
        if ((splitPercentage <= 0) || (splitPercentage >= 100)) {
          throw new IllegalArgumentException("Split percentage needs to be >0 and <100.");
        }
      }
      if ((preserveOrder) && (splitPercentage == -1)) {
        throw new IllegalArgumentException("Split percentage is missing.");
      }
      if (discardPredictions && (classifications.length() > 0 || classificationsOld.length() > 0)) {
        throw new IllegalArgumentException("Cannot both discard and output predictions!");
      }
      if (thresholdFile.length() > 0 && (classifications.length() > 0 || classificationsOld.length() > 0)) {
        throw new IllegalArgumentException("Cannot output predictions and also write threshold file!");
      }
      if (thresholdFile.length() > 0 && (!trainStatistics && noCrossValidation && splitPercentageString.length() <= 0 && testFileName.length() <= 0)) {
        throw new IllegalArgumentException("Can only write a threshold file when performance statistics are computed!");
      }
      if (printMargins && (!trainStatistics && noCrossValidation && splitPercentageString.length() <= 0 && testFileName.length() <= 0)) {
        throw new IllegalArgumentException("Can only print margins when performance statistics are computed!");
      }
      if ((trainFileName.length() == 0) && (printComplexityStatistics)) { // if no training file given, we don't have any priors
        throw new IllegalArgumentException("Cannot print complexity statistics without training file!");
      }
      if (printGraph && !(classifier instanceof Drawable)) {
        throw new IllegalArgumentException("Can only print graph if classifier implements Drawable interface!");
      }
      if (printSource && !(classifier instanceof Sourcable)) {
        throw new IllegalArgumentException("Can only print source if classifier implements Sourcable interface!");
      }
      if (printGraph && !(trainFileName.length() > 0) && !(objectInputFileName.length() > 0)) {
        throw new IllegalArgumentException("Can only print graph if training file or model file is provided!");
      }
      if (printSource && !(trainFileName.length() > 0) && !(objectInputFileName.length() > 0)) {
        throw new IllegalArgumentException("Can only print source if training file or model file is provided!");
      }
      if (objectInputFileName.length() > 0 && (trainFileName.length() > 0) &&
              (!(classifier instanceof UpdateableClassifier) && !(classifier instanceof IterativeClassifier) || forceBatchTraining)) {
        throw new IllegalArgumentException("Can't use batch training when updating/continue iterating with an existing classifier!");
      }
      if (noCrossValidation && testFileName.length() != 0) {
        throw new IllegalArgumentException("Attempt to turn off cross-validation when explicit test file is provided!");
      }
      if (splitPercentageString.length() > 0 && testFileName.length() != 0) {
        throw new IllegalArgumentException("Cannot perform percentage split when explicit test file is provided!");
      }
      if ((thresholdFile.length() != 0) && discardPredictions) {
        throw new IllegalArgumentException("Can only output to threshold file when predictions are not discarded!");
      }
      if (outputModelsForTrainingSplits && (testFileName.length() > 0 ||
              ((splitPercentageString.length() == 0) && noCrossValidation))) {
        throw new IllegalArgumentException("Can only output models for training splits if cross-validation or " +
              "percentage split evaluation is performed!");
      }

      // Set seed, number of folds, and class index if required
      if (seedString.length() != 0) {
        seed = Integer.parseInt(seedString);
      }
      if (foldsString.length() != 0) {
        folds = Integer.parseInt(foldsString);
      }
      if (classIndexString.length() != 0) {
        if (classIndexString.equals("first")) {
          classIndex = 1;
        } else if (classIndexString.equals("last")) {
          classIndex = -1;
        } else {
          classIndex = Integer.parseInt(classIndexString);
        }
      }

      // Try to open training and/or test file
      if (testFileName.length() != 0) {
        try {
          template = test = new DataSource(testFileName).getStructure();
          if (classIndex != -1) {
            test.setClassIndex(classIndex - 1);
          } else {
            if ((test.classIndex() == -1) || (classIndexString.length() != 0)) {
              test.setClassIndex(test.numAttributes() - 1);
            }
          }
          actualClassIndex = test.classIndex();
        } catch(Exception e){
          throw new Exception("Can't open file " + testFileName + '.');
        }
      }
      if (trainFileName.length() != 0) {
        try {
          template = train = new DataSource(trainFileName).getStructure();
          if (classIndex != -1) {
            train.setClassIndex(classIndex - 1);
          } else {
            if ((train.classIndex() == -1) || (classIndexString.length() != 0)) {
              train.setClassIndex(train.numAttributes() - 1);
            }
          }
          actualClassIndex = train.classIndex();
        } catch (Exception e) {
          throw new Exception("Can't open file " + trainFileName + '.');
        }
      }

      // Need to check whether train and test file are compatible
      if (!(classifier instanceof weka.classifiers.misc.InputMappedClassifier)) {
        if ((test != null) && (train != null) && !test.equalHeaders(train)) {
          throw new IllegalArgumentException("Train and test file not compatible!\n" + test.equalHeadersMsg(train));
        }
      }

      // Need to check whether output of threshold file is possible if desired by user
      if ((thresholdFile.length() != 0) && !template.classAttribute().isNominal()) {
        throw new IllegalArgumentException("Can only output to threshold file when class attribute is nominal!");
      }

      // Need to check whether output of margins is possible if desired by user
      if (printMargins && !template.classAttribute().isNominal()) {
        throw new IllegalArgumentException("Can only print margins when class is nominal!");
      }

      // Read model file if appropriate (which may just hold scheme-specific options, and not a built model)
      if (objectInputFileName.length() != 0) {
        Classifier backedUpClassifier = classifier;
        if (objectInputFileName.endsWith(".xml")) {
          try { // Try to load scheme-specific options as XMLClassifier
            OptionHandler cl = (OptionHandler) new XMLClassifier().read(objectInputFileName);
            options = Stream.concat(Arrays.stream(cl.getOptions()), Arrays.stream(options)).toArray(String[]::new);
            objectInputFileName = ""; // We have not actually read a built model, only some options
          } catch (IllegalArgumentException ex) {
            classifier = getModelFromFile(objectInputFileName, template);
          }
        } else {
          classifier = getModelFromFile(objectInputFileName, template);
        }
        if (!classifier.getClass().equals(backedUpClassifier.getClass())) {
          throw new IllegalArgumentException("Loaded classifier is " + classifier.getClass().getCanonicalName() +
                  ", not " + backedUpClassifier.getClass().getCanonicalName() + "!");
        }

        // set options if IterativeClassifier && continue iterating
        if (continueIteratingIterative && classifier instanceof IterativeClassifier &&
          classifier instanceof OptionHandler) {

          if (train == null) {
            throw new IllegalArgumentException("IterativeClassifiers require training "
              + "data to continue iterating");
          }

          ((OptionHandler) classifier).setOptions(options);
          ((IterativeClassifier) classifier).setResume(!cleanUpIterative);
        }
      }

      // Check for cost matrix
      costMatrix = handleCostOption(Utils.getOption('m', options), template.numClasses());

      // Need to check whether use of cost matrix is possible if desired by user
      if ((costMatrix != null) && !template.classAttribute().isNominal()) {
        throw new IllegalArgumentException("Can only use cost matrix when class attribute is nominal!");
      }

      // Determine if predictions are to be output
      if (classifications.length() > 0) {
        classificationOutput = AbstractOutput.fromCommandline(classifications);
        if (classificationOutput == null) {
          throw new IllegalArgumentException("Failed to instantiate class for classification output: " + classifications);
        }
        classificationOutput.setHeader(template);
      } else if (classificationsOld.length() > 0) {
        // backwards compatible with old "-p range" and "-distribution" options
        classificationOutput = new PlainText();
        classificationOutput.setHeader(template);
        if (!classificationsOld.equals("0")) {
          classificationOutput.setAttributes(classificationsOld);
        }
        classificationOutput.setOutputDistribution(Utils.getFlag("distribution", options));
      } else {
        if (Utils.getFlag("distribution", options)) { // -distribution flag needs -p option
          throw new Exception("Cannot print distribution without '-p' option!");
        }
      }

      if (thresholdLabel.length() != 0) {
        labelIndex = template.classAttribute().indexOfValue(thresholdLabel);
      }
      if (labelIndex == -1) {
        throw new IllegalArgumentException("Class label '" + thresholdLabel + "' is unknown!");
      }

      // If a model file is given, we shouldn't process scheme-specific options
      // (with the exception of IterativeClassifiers)
      if (objectInputFileName.length() == 0) {
        if (classifier instanceof OptionHandler) {
          for (String option : options) {
            if (option.length() != 0) {
              if (schemeOptionsText == null) {
                schemeOptionsText = new StringBuffer();
              }
              if (option.indexOf(' ') != -1) {
                schemeOptionsText.append('"' + option + "\" ");
              } else {
                schemeOptionsText.append(option + " ");
              }
            }
          }
          ((OptionHandler) classifier).setOptions(options);
        }
      }
      Utils.checkForRemainingOptions(options);
    } catch (Exception ex) {
      throw new Exception("\nWeka exception: " + ex.getMessage() + makeOptionString(classifier, false));
    }

    // Build classifier on full training set if necessary
    Classifier classifierBackup = null;
    if (objectInputFileName.length() == 0) {
      classifierBackup = AbstractClassifier.makeCopy(classifier); // Back up configured classifier
    }
    if (trainFileName.length() > 0) {
      if (!noOutput || trainStatistics || printGraph || printSource || objectOutputFileName.length() > 0 ||
              (testFileName.length() > 0) || (classificationOutput != null && noCrossValidation && splitPercentage == -1)) {
        if ((classifier instanceof UpdateableClassifier) && !forceBatchTraining) { // Build classifier incrementally
          trainTimeStart = System.currentTimeMillis();
          DataSource trainSource = new DataSource(trainFileName);
          trainSource.getStructure(); // Need to advance in the file to get to the data
          if (objectInputFileName.length() <= 0) { // Only need to initialize classifier if we haven't loaded one
            classifier.buildClassifier(new Instances(train, 0));
          }
          while (trainSource.hasMoreElements(train)) {
            ((UpdateableClassifier) classifier).updateClassifier(trainSource.nextElement(train));
          }
          if (classifier instanceof UpdateableBatchProcessor) {
            ((UpdateableBatchProcessor) classifier).batchFinished();
          }
          trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;
        } else if (classifier instanceof IterativeClassifier && continueIteratingIterative) {
          IterativeClassifier iClassifier = (IterativeClassifier)classifier;
          Instances tempTrain = new DataSource(trainFileName).getDataSet(actualClassIndex);
          iClassifier.initializeClassifier(tempTrain);
          while (iClassifier.next()){
          }
          iClassifier.done();
        } else { // Build classifier in one go
          Instances tempTrain = new DataSource(trainFileName).getDataSet(actualClassIndex);
          if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
            Instances mappedClassifierDataset = ((weka.classifiers.misc.InputMappedClassifier) classifier)
                    .getModelHeader(new Instances(template, 0));
            if (!mappedClassifierDataset.equalHeaders(tempTrain)) {
              for (int zz = 0; zz < tempTrain.numInstances(); zz++) {
                Instance mapped = ((weka.classifiers.misc.InputMappedClassifier) classifier)
                        .constructMappedInstance(tempTrain.instance(zz));
                mappedClassifierDataset.add(mapped);
              }
              tempTrain = mappedClassifierDataset;
            }
          }
          trainTimeStart = System.currentTimeMillis();
          classifier.buildClassifier(tempTrain);
          trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;
        }
      }
    }

    // If classifier is drawable output string describing graph
    if (printGraph) {
      return ((Drawable) classifier).graph();
    }

    // Output the classifier as equivalent source
    if (printSource) {
      return wekaStaticWrapper((Sourcable) classifier, sourceClass);
    }

    // Save classifier if appropriate
    if (objectOutputFileName.length() > 0) {
      saveClassifier(classifier, template, objectOutputFileName);
    }

    // Output model
    StringBuffer text = new StringBuffer();
    if (!(noOutput || printMargins || classificationOutput != null)) {
      if (classifier instanceof OptionHandler) {
        if (schemeOptionsText != null) {
          text.append("\nOptions: " + schemeOptionsText + "\n");
        }
      }
      text.append("\n=== Classifier model (full training set) ===\n\n" + classifier.toString() + "\n");
      text.append("\nTime taken to build model: " + Utils.doubleToString(trainTimeElapsed / 1000.0, 2) + " seconds\n");
    }

    // Stop here if no output of performance statistics or predictions is required and no threshold data is required
    if (!trainStatistics && noCrossValidation && splitPercentage != -1 && testFileName.length() <= 0 &&
            classificationOutput == null) {
      if (noOutput) {
        return "";
      } else {
        return text.toString();
      }
    }

    if (!printMargins && (costMatrix != null)) {
      text.append("\n=== Evaluation Cost Matrix ===\n\n");
      text.append(costMatrix.toString());
    }

    // Do we need a mapped classifier header?
    Instances mappedClassifierHeader = null;
    if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
      mappedClassifierHeader = ((weka.classifiers.misc.InputMappedClassifier) classifier)
              .getModelHeader(new Instances(template, 0));
    }

    // Do we just want to output predictions?
    if (classificationOutput != null) {

      // ===============================================
      // Code path for when predictions are to be output
      // ===============================================

      // Set up appropriate header for input mapped classifier
      if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
        classificationOutput.setHeader(mappedClassifierHeader);
      }

      // Set up buffer
      StringBuffer predsBuff = new StringBuffer();
      classificationOutput.setBuffer(predsBuff);
      if (testFileName.length() > 0) { // CASE 1: SEPARATE TEST SET
        predsBuff.append("\n=== Predictions on test data ===\n\n");
        classificationOutput.print(classifier, new DataSource(testFileName));
      } else if (splitPercentage > 0) { // CASE 2: PERCENTAGE SPLIT
        Instances tmpInst = new DataSource(trainFileName).getDataSet(actualClassIndex);
        if (!preserveOrder) {
          tmpInst.randomize(new Random(seed));
        }
        int trainSize = (int) Math.round(tmpInst.numInstances() * splitPercentage / 100);
        int testSize = tmpInst.numInstances() - trainSize;
        Instances trainInst = new Instances(tmpInst, 0, trainSize);
        classifier = AbstractClassifier.makeCopy(classifierBackup);
        classifier.buildClassifier(trainInst);
        trainInst = null;
        Instances testInst = new Instances(tmpInst, trainSize, testSize);
        predsBuff.append("\n=== Predictions on test split ===\n\n");
        classificationOutput.print(classifier, testInst);
      } else if (!noCrossValidation) { // CASE 3: CROSS-VALIDATION
        Random random = new Random(seed);
        Evaluation testingEvaluation = new Evaluation(new Instances(template, 0), costMatrix);
        if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
          testingEvaluation = new Evaluation(new Instances(mappedClassifierHeader, 0), costMatrix);
        }
        testingEvaluation.toggleEvalMetrics(toggleList);
        classifier = AbstractClassifier.makeCopy(classifierBackup);
        predsBuff.append("\n=== Predictions under cross-validation ===\n\n");
        testingEvaluation.crossValidateModel(classifier, new DataSource(trainFileName).getDataSet(actualClassIndex), folds, random,
                classificationOutput);
      } else {
        predsBuff.append("\n=== Predictions on training data ===\n\n");
        classificationOutput.print(classifier, new DataSource(trainFileName));
      }
      text.append("\n" + predsBuff);
    } else {

      // ================================================
      // Code path for when performance is to be computed
      // ================================================

      Evaluation testingEvaluation = new Evaluation(new Instances(template, 0), costMatrix);
      if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
        testingEvaluation = new Evaluation(new Instances(mappedClassifierHeader, 0), costMatrix);
      }
      testingEvaluation.setDiscardPredictions(discardPredictions);
      testingEvaluation.toggleEvalMetrics(toggleList);

      // CASE 1: SEPARATE TEST SET
      if (testFileName.length() > 0) {

        // Evaluation on the training data required?
        if (train != null && trainStatistics && !printMargins) {
          Evaluation trainingEvaluation = new Evaluation(new Instances(template, 0), costMatrix);
          if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
            trainingEvaluation = new Evaluation(new Instances(mappedClassifierHeader, 0), costMatrix);
          }
          trainingEvaluation.setDiscardPredictions(discardPredictions);
          trainingEvaluation.toggleEvalMetrics(toggleList);
          trainingEvaluation.setPriors(train);
          testingEvaluation.setPriors(train);
          DataSource trainSource = new DataSource(trainFileName);
          trainSource.getStructure(); // We already know the structure but need to advance to the data section
          while (trainSource.hasMoreElements(train)) {
            Instance trainInst = trainSource.nextElement(train);
            trainingEvaluation.updatePriors(trainInst);
            testingEvaluation.updatePriors(trainInst);
          }
          if ((classifier instanceof BatchPredictor) && (((BatchPredictor) classifier).implementsMoreEfficientBatchPrediction())) {
            testTimeStart = System.currentTimeMillis();
            trainingEvaluation.evaluateModel(classifier, new DataSource(trainFileName).getDataSet(actualClassIndex));
            testTimeElapsed = System.currentTimeMillis() - testTimeStart;
          } else {
            trainSource = new DataSource(trainFileName);
            trainSource.getStructure(); // We already know the structure but need to advance to the data section
            testTimeStart = System.currentTimeMillis();
            while (trainSource.hasMoreElements(train)) {
              trainingEvaluation.evaluateModelOnceAndRecordPrediction(classifier, trainSource.nextElement(train));
            }
            testTimeElapsed = System.currentTimeMillis() - testTimeStart;
          }
          text.append("\nTime taken to test model on training data: ");
          text.append(Utils.doubleToString(testTimeElapsed / 1000.0, 2) + " seconds");
          text.append(trainingEvaluation.toSummaryString("\n\n=== Error on training data ===\n", printComplexityStatistics));
          if (template.classAttribute().isNominal()) {
            if (classStatistics) {
              text.append("\n\n" + trainingEvaluation.toClassDetailsString());
            }
            text.append("\n\n" + trainingEvaluation.toMatrixString());
          }
        }

        // Evaluate on test data
        if (train == null) {
          testingEvaluation.useNoPriors();
        }
        if (classifier instanceof BatchPredictor && ((BatchPredictor) classifier).implementsMoreEfficientBatchPrediction()) {
          testTimeStart = System.currentTimeMillis();
          testingEvaluation.evaluateModel(classifier, new DataSource(testFileName).getDataSet(test.classIndex()));
          testTimeElapsed = System.currentTimeMillis() - testTimeStart;
        } else {
          DataSource testSource = new DataSource(testFileName);
          testSource.getStructure(); // We already know the structure but need to advance to the data section
          testTimeStart = System.currentTimeMillis();
          while (testSource.hasMoreElements(test)) {
            testingEvaluation.evaluateModelOnceAndRecordPrediction(classifier, testSource.nextElement(test));
          }
          testTimeElapsed = System.currentTimeMillis() - testTimeStart;
        }
        if (printMargins) {
          return testingEvaluation.toCumulativeMarginDistributionString();
        }
        text.append("\nTime taken to test model on test data: ");
        text.append(Utils.doubleToString(testTimeElapsed / 1000.0, 2) + " seconds");
        text.append(testingEvaluation.toSummaryString("\n\n=== Error on test data ===\n", printComplexityStatistics));
        if (template.classAttribute().isNominal()) {
          if (classStatistics) {
            text.append("\n\n" + testingEvaluation.toClassDetailsString());
          }
          text.append("\n\n" + testingEvaluation.toMatrixString());
        }
      } else if (splitPercentage > 0) { // CASE 2: PERCENTAGE SPLIT

        // Evaluation on the training data required?
        if (train != null && trainStatistics && !printMargins) {
          Evaluation trainingEvaluation = new Evaluation(new Instances(template, 0), costMatrix);
          if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
            trainingEvaluation = new Evaluation(new Instances(mappedClassifierHeader, 0), costMatrix);
          }
          trainingEvaluation.setDiscardPredictions(discardPredictions);
          trainingEvaluation.toggleEvalMetrics(toggleList);
          DataSource trainSource = new DataSource(trainFileName);
          trainSource.getStructure(); // We already know the structure but need to advance to the data section
          trainingEvaluation.setPriors(train);
          while (trainSource.hasMoreElements(train)) {
            trainingEvaluation.updatePriors(trainSource.nextElement(train));
          }
          if ((classifier instanceof BatchPredictor) && (((BatchPredictor) classifier).implementsMoreEfficientBatchPrediction())) {
            testTimeStart = System.currentTimeMillis();
            trainingEvaluation.evaluateModel(classifier, new DataSource(trainFileName).getDataSet(actualClassIndex));
            testTimeElapsed = System.currentTimeMillis() - testTimeStart;
          } else {
            trainSource = new DataSource(trainFileName);
            trainSource.getStructure(); // We already know the structure but need to advance to the data section
            testTimeStart = System.currentTimeMillis();
            while (trainSource.hasMoreElements(train)) {
              trainingEvaluation.evaluateModelOnceAndRecordPrediction(classifier, trainSource.nextElement(train));
            }
            testTimeElapsed = System.currentTimeMillis() - testTimeStart;
          }
          text.append("\nTime taken to test model on training data: ");
          text.append(Utils.doubleToString(testTimeElapsed / 1000.0, 2) + " seconds");
          text.append(trainingEvaluation.toSummaryString("\n\n=== Error on training data ===\n", printComplexityStatistics));
          if (template.classAttribute().isNominal()) {
            if (classStatistics) {
              text.append("\n\n" + trainingEvaluation.toClassDetailsString());
            }
            text.append("\n\n" + trainingEvaluation.toMatrixString());
          }
        }

        Instances tmpInst = new DataSource(trainFileName).getDataSet(actualClassIndex);
        if (!preserveOrder) {
          tmpInst.randomize(new Random(seed));
        }
        int trainSize = (int) Math.round(tmpInst.numInstances() * splitPercentage / 100);
        int testSize = tmpInst.numInstances() - trainSize;
        Instances trainInst = new Instances(tmpInst, 0, trainSize);
        classifier = AbstractClassifier.makeCopy(classifierBackup);
        classifier.buildClassifier(trainInst);
        if (outputModelsForTrainingSplits) {
          text.append("\n=== Classifier model (training split) ===\n\n" + classifier.toString() + "\n");
        }
        testingEvaluation.setPriors(trainInst);
        trainInst = null;
        Instances testInst = new Instances(tmpInst, trainSize, testSize);
        testTimeStart = System.currentTimeMillis();
        testingEvaluation.evaluateModel(classifier, testInst);
        testTimeElapsed = System.currentTimeMillis() - testTimeStart;
        if (printMargins) {
          return testingEvaluation.toCumulativeMarginDistributionString();
        }
        text.append("\nTime taken to test model on test split: ");
        text.append(Utils.doubleToString(testTimeElapsed / 1000.0, 2) + " seconds");
        text.append(testingEvaluation.toSummaryString("\n\n=== Error on test split ===\n", printComplexityStatistics));
        if (template.classAttribute().isNominal()) {
          if (classStatistics) {
            text.append("\n\n" + testingEvaluation.toClassDetailsString());
          }
          text.append("\n\n" + testingEvaluation.toMatrixString());
        }
      } else if (!noCrossValidation) { // CASE 3: CROSS-VALIDATION

        // Evaluation on the training data required?
        if (train != null && trainStatistics && !printMargins) {
          Evaluation trainingEvaluation = new Evaluation(new Instances(template, 0), costMatrix);
          if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
            trainingEvaluation = new Evaluation(new Instances(mappedClassifierHeader, 0), costMatrix);
          }
          trainingEvaluation.setDiscardPredictions(discardPredictions);
          trainingEvaluation.toggleEvalMetrics(toggleList);
          DataSource trainSource = new DataSource(trainFileName);
          trainSource.getStructure(); // We already know the structure but need to advance to the data section
          trainingEvaluation.setPriors(train);
          while (trainSource.hasMoreElements(train)) {
            trainingEvaluation.updatePriors(trainSource.nextElement(train));
          }
          if ((classifier instanceof BatchPredictor) && (((BatchPredictor) classifier).implementsMoreEfficientBatchPrediction())) {
            testTimeStart = System.currentTimeMillis();
            trainingEvaluation.evaluateModel(classifier, new DataSource(trainFileName).getDataSet(actualClassIndex));
            testTimeElapsed = System.currentTimeMillis() - testTimeStart;
          } else {
            trainSource = new DataSource(trainFileName);
            trainSource.getStructure(); // We already know the structure but need to advance to the data section
            testTimeStart = System.currentTimeMillis();
            while (trainSource.hasMoreElements(train)) {
              trainingEvaluation.evaluateModelOnceAndRecordPrediction(classifier, trainSource.nextElement(train));
            }
            testTimeElapsed = System.currentTimeMillis() - testTimeStart;
          }
          text.append("\nTime taken to test model on training data: ");
          text.append(Utils.doubleToString(testTimeElapsed / 1000.0, 2) + " seconds");
          text.append(trainingEvaluation.toSummaryString("\n\n=== Error on training data ===\n", printComplexityStatistics));
          if (template.classAttribute().isNominal()) {
            if (classStatistics) {
              text.append("\n\n" + trainingEvaluation.toClassDetailsString());
            }
            text.append("\n\n" + trainingEvaluation.toMatrixString());
          }
        }

        Random random = new Random(seed);
        // use untrained (!) classifier for cross-validation
        classifier = AbstractClassifier.makeCopy(classifierBackup);
        testTimeStart = System.currentTimeMillis();
        if (!outputModelsForTrainingSplits) {
          testingEvaluation.crossValidateModel(classifier, new DataSource(trainFileName).getDataSet(actualClassIndex),
                  folds, random);
        } else {
          testingEvaluation.crossValidateModel(classifier, new DataSource(trainFileName).getDataSet(actualClassIndex),
                  folds, random, text);
        }
        testTimeElapsed = System.currentTimeMillis() - testTimeStart;
        if (printMargins) {
          return testingEvaluation.toCumulativeMarginDistributionString();
        }
        text.append("\nTime taken to perform cross-validation: ");
        text.append(Utils.doubleToString(testTimeElapsed / 1000.0, 2) + " seconds");
        if (template.classAttribute().isNumeric()) {
          text.append("\n\n\n" + testingEvaluation.toSummaryString("=== Cross-validation ===\n",
                  printComplexityStatistics));
        } else {
          text.append("\n\n\n" + testingEvaluation.toSummaryString("=== Stratified "
                  + "cross-validation ===\n", printComplexityStatistics));
        }
        if (template.classAttribute().isNominal()) {
          if (classStatistics) {
            text.append("\n\n" + testingEvaluation.toClassDetailsString());
          }
          text.append("\n\n" + testingEvaluation.toMatrixString());
        }
      } else if (trainStatistics) { // CASE 4: Only evaluate on the training set

        // Evaluation on the training data required?
        if (train != null) {
          Evaluation trainingEvaluation = new Evaluation(new Instances(template, 0), costMatrix);
          if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
            trainingEvaluation = new Evaluation(new Instances(mappedClassifierHeader, 0), costMatrix);
          }
          trainingEvaluation.setDiscardPredictions(discardPredictions);
          trainingEvaluation.toggleEvalMetrics(toggleList);
          DataSource trainSource = new DataSource(trainFileName);
          trainSource.getStructure(); // We already know the structure but need to advance to the data section
          trainingEvaluation.setPriors(train);
          while (trainSource.hasMoreElements(train)) {
            trainingEvaluation.updatePriors(trainSource.nextElement(train));
          }
          if ((classifier instanceof BatchPredictor) && (((BatchPredictor) classifier).implementsMoreEfficientBatchPrediction())) {
            testTimeStart = System.currentTimeMillis();
            trainingEvaluation.evaluateModel(classifier, new DataSource(trainFileName).getDataSet(actualClassIndex));
            testTimeElapsed = System.currentTimeMillis() - testTimeStart;
          } else {
            trainSource = new DataSource(trainFileName);
            trainSource.getStructure(); // We already know the structure but need to advance to the data section
            testTimeStart = System.currentTimeMillis();
            while (trainSource.hasMoreElements(train)) {
              trainingEvaluation.evaluateModelOnceAndRecordPrediction(classifier, trainSource.nextElement(train));
            }
            testTimeElapsed = System.currentTimeMillis() - testTimeStart;
          }
          if (printMargins) {
            return trainingEvaluation.toCumulativeMarginDistributionString();
          }
          text.append("\nTime taken to test model on training data: ");
          text.append(Utils.doubleToString(testTimeElapsed / 1000.0, 2) + " seconds");
          text.append(trainingEvaluation.toSummaryString("\n\n=== Error on training data ===\n", printComplexityStatistics));
          if (template.classAttribute().isNominal()) {
            if (classStatistics) {
              text.append("\n\n" + trainingEvaluation.toClassDetailsString());
            }
            text.append("\n\n" + trainingEvaluation.toMatrixString());
          }
          testingEvaluation = trainingEvaluation;
        }
      }

      // Output threshold file
      if (thresholdFile.length() != 0) {
        ThresholdCurve tc = new ThresholdCurve();
        Instances result = tc.getCurve(testingEvaluation.predictions(), labelIndex);
        DataSink.write(thresholdFile, result);
      }
    }

    return text.toString();
  }

  /**
   * Attempts to load a cost matrix.
   *
   * @param costFileName the filename of the cost matrix
   * @param numClasses the number of classes that should be in the cost matrix
   *          (only used if the cost file is in old format).
   * @return a <code>CostMatrix</code> value, or null if costFileName is empty
   * @throws Exception if an error occurs.
   */
  protected static CostMatrix handleCostOption(String costFileName,
    int numClasses) throws Exception {

    if ((costFileName != null) && (costFileName.length() != 0)) {

      Reader costReader = null;
      try {
        costReader = new BufferedReader(new FileReader(costFileName));
      } catch (Exception e) {
        throw new Exception("Can't open file " + e.getMessage() + '.');
      }
      try {
        // First try as a proper cost matrix format
        return new CostMatrix(costReader);
      } catch (Exception ex) {
        try {
          // Now try as the poxy old format :-)
          // System.err.println("Attempting to read old format cost file");
          try {
            costReader.close(); // Close the old one
            costReader = new BufferedReader(new FileReader(costFileName));
          } catch (Exception e) {
            throw new Exception("Can't open file " + e.getMessage() + '.');
          }
          CostMatrix costMatrix = new CostMatrix(numClasses);
          // System.err.println("Created default cost matrix");
          costMatrix.readOldFormat(costReader);
          return costMatrix;
          // System.err.println("Read old format");
        } catch (Exception e2) {
          // re-throw the original exception
          // System.err.println("Re-throwing original exception");
          throw ex;
        }
      }
    } else {
      return null;
    }
  }

  /**
   * Evaluates the classifier on a given set of instances. Note that the data
   * must have exactly the same format (e.g. order of attributes) as the data
   * used to train the classifier! Otherwise the results will generally be
   * meaningless.
   *
   * @param classifier machine learning classifier
   * @param data set of test instances for evaluation
   * @param forPredictionsPrinting varargs parameter that, if supplied, is
   *          expected to hold a
   *          weka.classifiers.evaluation.output.prediction.AbstractOutput
   *          object
   * @return the predictions
   * @throws Exception if model could not be evaluated successfully
   */
  public double[] evaluateModel(Classifier classifier, Instances data,
    Object... forPredictionsPrinting) throws Exception {
    // for predictions printing
    AbstractOutput classificationOutput = null;

    double predictions[] = new double[data.numInstances()];

    if (forPredictionsPrinting.length > 0) {
      classificationOutput = (AbstractOutput) forPredictionsPrinting[0];
    }

    if (classifier instanceof BatchPredictor
      && ((BatchPredictor) classifier).implementsMoreEfficientBatchPrediction()) {
      // make a copy and set the class to missing
      Instances dataPred = new Instances(data);
      for (int i = 0; i < data.numInstances(); i++) {
        dataPred.instance(i).setClassMissing();
      }
      double[][] preds =
        ((BatchPredictor) classifier).distributionsForInstances(dataPred);
      for (int i = 0; i < data.numInstances(); i++) {
        double[] p = preds[i];

        predictions[i] = evaluationForSingleInstance(p, data.instance(i), true);

        if (classificationOutput != null) {
          classificationOutput.printClassification(p, data.instance(i), i);
        }
      }
    } else {
      // Need to be able to collect predictions if appropriate (for AUC)

      for (int i = 0; i < data.numInstances(); i++) {
        predictions[i] =
          evaluateModelOnceAndRecordPrediction(classifier, data.instance(i));
        if (classificationOutput != null) {
          classificationOutput.printClassification(classifier,
            data.instance(i), i);
        }
      }
    }

    return predictions;
  }

  /**
   * Evaluates the supplied distribution on a single instance.
   *
   * @param dist the supplied distribution
   * @param instance the test instance to be classified
   * @param storePredictions whether to store predictions for nominal classifier
   * @return the prediction
   * @throws Exception if model could not be evaluated successfully
   */
  public double evaluationForSingleInstance(double[] dist, Instance instance,
    boolean storePredictions) throws Exception {

    double pred;

    if (m_ClassIsNominal) {
      pred = Utils.maxIndex(dist);
      if (dist[(int) pred] <= 0) {
        pred = Utils.missingValue();
      }
      updateStatsForClassifier(dist, instance);
      if (storePredictions && !m_DiscardPredictions) {
        if (m_Predictions == null) {
          m_Predictions = new ArrayList<Prediction>();
        }
        m_Predictions.add(new NominalPrediction(instance.classValue(), dist,
          instance.weight()));
      }
    } else {
      pred = dist[0];
      updateStatsForPredictor(pred, instance);
      if (storePredictions && !m_DiscardPredictions) {
        if (m_Predictions == null) {
          m_Predictions = new ArrayList<Prediction>();
        }
        m_Predictions.add(new NumericPrediction(instance.classValue(), pred,
          instance.weight()));
      }
    }

    return pred;
  }

  /**
   * Evaluates the classifier on a single instance and records the prediction.
   *
   * @param classifier machine learning classifier
   * @param instance the test instance to be classified
   * @param storePredictions whether to store predictions for nominal classifier
   * @return the prediction made by the clasifier
   * @throws Exception if model could not be evaluated successfully or the data
   *           contains string attributes
   */
  protected double evaluationForSingleInstance(Classifier classifier,
    Instance instance, boolean storePredictions) throws Exception {

    Instance classMissing = (Instance) instance.copy();
    classMissing.setDataset(instance.dataset());

    if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
      instance = (Instance) instance.copy();
      instance =
        ((weka.classifiers.misc.InputMappedClassifier) classifier)
          .constructMappedInstance(instance);
      // System.out.println("Mapped instance " + instance);
      int mappedClass =
        ((weka.classifiers.misc.InputMappedClassifier) classifier)
          .getMappedClassIndex();
      classMissing.setMissing(mappedClass);
    } else {
      classMissing.setClassMissing();
    }

    // System.out.println("instance (to predict)" + classMissing);
    double pred =
      evaluationForSingleInstance(
        classifier.distributionForInstance(classMissing), instance,
        storePredictions);

    // We don't need to do the following if the class is nominal because in that
    // case
    // entropy and coverage statistics are always computed.
    if (!m_ClassIsNominal) {
      if (!instance.classIsMissing() && !Utils.isMissingValue(pred)) {
        if (classifier instanceof IntervalEstimator) {
          updateStatsForIntervalEstimator((IntervalEstimator) classifier,
            classMissing, instance.classValue());
        } else {
          m_CoverageStatisticsAvailable = false;
        }
        if (classifier instanceof ConditionalDensityEstimator) {
          updateStatsForConditionalDensityEstimator(
            (ConditionalDensityEstimator) classifier, classMissing,
            instance.classValue());
        } else {
          m_ComplexityStatisticsAvailable = false;
        }
      }
    }
    return pred;
  }

  /**
   * Evaluates the classifier on a single instance and records the prediction.
   *
   * @param classifier machine learning classifier
   * @param instance the test instance to be classified
   * @return the prediction made by the clasifier
   * @throws Exception if model could not be evaluated successfully or the data
   *           contains string attributes
   */
  public double evaluateModelOnceAndRecordPrediction(Classifier classifier,
    Instance instance) throws Exception {

    return evaluationForSingleInstance(classifier, instance, true);
  }

  /**
   * Evaluates the classifier on a single instance.
   *
   * @param classifier machine learning classifier
   * @param instance the test instance to be classified
   * @return the prediction made by the clasifier
   * @throws Exception if model could not be evaluated successfully or the data
   *           contains string attributes
   */
  public double evaluateModelOnce(Classifier classifier, Instance instance)
    throws Exception {

    return evaluationForSingleInstance(classifier, instance, false);
  }

  /**
   * Evaluates the supplied distribution on a single instance.
   *
   * @param dist the supplied distribution
   * @param instance the test instance to be classified
   * @return the prediction
   * @throws Exception if model could not be evaluated successfully
   */
  public double evaluateModelOnce(double[] dist, Instance instance)
    throws Exception {

    return evaluationForSingleInstance(dist, instance, false);
  }

  /**
   * Evaluates the supplied distribution on a single instance.
   *
   * @param dist the supplied distribution
   * @param instance the test instance to be classified
   * @return the prediction
   * @throws Exception if model could not be evaluated successfully
   */
  public double evaluateModelOnceAndRecordPrediction(double[] dist,
    Instance instance) throws Exception {

    return evaluationForSingleInstance(dist, instance, true);
  }

  /**
   * Evaluates the supplied prediction on a single instance.
   *
   * @param prediction the supplied prediction
   * @param instance the test instance to be classified
   * @throws Exception if model could not be evaluated successfully
   */
  public void evaluateModelOnce(double prediction, Instance instance)
    throws Exception {

    evaluateModelOnce(makeDistribution(prediction), instance);
  }

  /**
   * Returns the predictions that have been collected.
   *
   * @return a reference to the FastVector containing the predictions that have
   *         been collected. This should be null if no predictions have been
   *         collected.
   */
  public ArrayList<Prediction> predictions() {
    if (m_DiscardPredictions) {
      return null;
    } else {
      return m_Predictions;
    }
  }

  /**
   * Wraps a static classifier in enough source to test using the weka class
   * libraries.
   *
   * @param classifier a Sourcable Classifier
   * @param className the name to give to the source code class
   * @return the source for a static classifier that can be tested with weka
   *         libraries.
   * @throws Exception if code-generation fails
   */
  public static String
    wekaStaticWrapper(Sourcable classifier, String className) throws Exception {

    StringBuffer result = new StringBuffer();
    String staticClassifier = classifier.toSource(className);

    result.append("// Generated with Weka " + Version.VERSION + "\n");
    result.append("//\n");
    result
      .append("// This code is public domain and comes with no warranty.\n");
    result.append("//\n");
    result.append("// Timestamp: " + new Date() + "\n");
    result.append("\n");
    result.append("package weka.classifiers;\n");
    result.append("\n");
    result.append("import weka.core.Attribute;\n");
    result.append("import weka.core.Capabilities;\n");
    result.append("import weka.core.Capabilities.Capability;\n");
    result.append("import weka.core.Instance;\n");
    result.append("import weka.core.Instances;\n");
    result.append("import weka.core.RevisionUtils;\n");
    result
      .append("import weka.classifiers.Classifier;\nimport weka.classifiers.AbstractClassifier;\n");
    result.append("\n");
    result.append("public class WekaWrapper\n");
    result.append("  extends AbstractClassifier {\n");

    // globalInfo
    result.append("\n");
    result.append("  /**\n");
    result.append("   * Returns only the toString() method.\n");
    result.append("   *\n");
    result.append("   * @return a string describing the classifier\n");
    result.append("   */\n");
    result.append("  public String globalInfo() {\n");
    result.append("    return toString();\n");
    result.append("  }\n");

    // getCapabilities
    result.append("\n");
    result.append("  /**\n");
    result.append("   * Returns the capabilities of this classifier.\n");
    result.append("   *\n");
    result.append("   * @return the capabilities\n");
    result.append("   */\n");
    result.append("  public Capabilities getCapabilities() {\n");
    result.append(((Classifier) classifier).getCapabilities().toSource(
      "result", 4));
    result.append("    return result;\n");
    result.append("  }\n");

    // buildClassifier
    result.append("\n");
    result.append("  /**\n");
    result.append("   * only checks the data against its capabilities.\n");
    result.append("   *\n");
    result.append("   * @param i the training data\n");
    result.append("   */\n");
    result
      .append("  public void buildClassifier(Instances i) throws Exception {\n");
    result.append("    // can classifier handle the data?\n");
    result.append("    getCapabilities().testWithFail(i);\n");
    result.append("  }\n");

    // classifyInstance
    result.append("\n");
    result.append("  /**\n");
    result.append("   * Classifies the given instance.\n");
    result.append("   *\n");
    result.append("   * @param i the instance to classify\n");
    result.append("   * @return the classification result\n");
    result.append("   */\n");
    result
      .append("  public double classifyInstance(Instance i) throws Exception {\n");
    result.append("    Object[] s = new Object[i.numAttributes()];\n");
    result.append("    \n");
    result.append("    for (int j = 0; j < s.length; j++) {\n");
    result.append("      if (!i.isMissing(j)) {\n");
    result.append("        if (i.attribute(j).isNominal())\n");
    result.append("          s[j] = new String(i.stringValue(j));\n");
    result.append("        else if (i.attribute(j).isNumeric())\n");
    result.append("          s[j] = new Double(i.value(j));\n");
    result.append("      }\n");
    result.append("    }\n");
    result.append("    \n");
    result.append("    // set class value to missing\n");
    result.append("    s[i.classIndex()] = null;\n");
    result.append("    \n");
    result.append("    return " + className + ".classify(s);\n");
    result.append("  }\n");

    // getRevision
    result.append("\n");
    result.append("  /**\n");
    result.append("   * Returns the revision string.\n");
    result.append("   * \n");
    result.append("   * @return        the revision\n");
    result.append("   */\n");
    result.append("  public String getRevision() {\n");
    result.append("    return RevisionUtils.extract(\"1.0\");\n");
    result.append("  }\n");

    // toString
    result.append("\n");
    result.append("  /**\n");
    result
      .append("   * Returns only the classnames and what classifier it is based on.\n");
    result.append("   *\n");
    result.append("   * @return a short description\n");
    result.append("   */\n");
    result.append("  public String toString() {\n");
    result.append("    return \"Auto-generated classifier wrapper, based on "
      + classifier.getClass().getName() + " (generated with Weka "
      + Version.VERSION + ").\\n" + "\" + this.getClass().getName() + \"/"
      + className + "\";\n");
    result.append("  }\n");

    // main
    result.append("\n");
    result.append("  /**\n");
    result.append("   * Runs the classfier from commandline.\n");
    result.append("   *\n");
    result.append("   * @param args the commandline arguments\n");
    result.append("   */\n");
    result.append("  public static void main(String args[]) {\n");
    result.append("    runClassifier(new WekaWrapper(), args);\n");
    result.append("  }\n");
    result.append("}\n");

    // actual classifier code
    result.append("\n");
    result.append(staticClassifier);

    return result.toString();
  }

  /**
   * Gets the number of test instances that had a known class value (actually
   * the sum of the weights of test instances with known class value).
   *
   * @return the number of test instances with known class
   */
  public final double numInstances() {

    return m_WithClass;
  }

  /**
   * Gets the coverage of the test cases by the predicted regions at the
   * confidence level specified when evaluation was performed.
   *
   * @return the coverage of the test cases by the predicted regions
   */
  public final double coverageOfTestCasesByPredictedRegions() {

    if (!m_CoverageStatisticsAvailable) {
      return Double.NaN;
    }

    return 100 * m_TotalCoverage / m_WithClass;
  }

  /**
   * Gets the average size of the predicted regions, relative to the range of
   * the target in the training data, at the confidence level specified when
   * evaluation was performed.
   *
   * @return the average size of the predicted regions
   */
  public final double sizeOfPredictedRegions() {

    if (m_NoPriors || !m_CoverageStatisticsAvailable) {
      return Double.NaN;
    }

    return 100 * m_TotalSizeOfRegions / m_WithClass;
  }

  /**
   * Gets the weight of the instances that had a non-missing class value
   *
   * @return the weight of the instances that had a non-missing class value
   */
  public final double withClass() {
    return m_WithClass;
  }

  /**
   * Gets the weight of the instances that had missing class values
   *
   * @return the weight of the instances that had missing class values
   */
  public final double missingClass() {
    return m_MissingClass;
  }

  /**
   * Gets the number of instances incorrectly classified (that is, for which an
   * incorrect prediction was made). (Actually the sum of the weights of these
   * instances)
   *
   * @return the number of incorrectly classified instances
   */
  public final double incorrect() {

    return m_Incorrect;
  }

  /**
   * Gets the percentage of instances incorrectly classified (that is, for which
   * an incorrect prediction was made).
   *
   * @return the percent of incorrectly classified instances (between 0 and 100)
   */
  public final double pctIncorrect() {

    return 100 * m_Incorrect / m_WithClass;
  }

  /**
   * Gets the total cost, that is, the cost of each prediction times the weight
   * of the instance, summed over all instances.
   *
   * @return the total cost
   */
  public final double totalCost() {

    return m_TotalCost;
  }

  /**
   * Gets the average cost, that is, total cost of misclassifications (incorrect
   * plus unclassified) over the total number of instances.
   *
   * @return the average cost.
   */
  public final double avgCost() {

    return m_TotalCost / m_WithClass;
  }

  /**
   * Gets the number of instances correctly classified (that is, for which a
   * correct prediction was made). (Actually the sum of the weights of these
   * instances)
   *
   * @return the number of correctly classified instances
   */
  public final double correct() {

    return m_Correct;
  }

  /**
   * Gets the percentage of instances correctly classified (that is, for which a
   * correct prediction was made).
   *
   * @return the percent of correctly classified instances (between 0 and 100)
   */
  public final double pctCorrect() {

    return 100 * m_Correct / m_WithClass;
  }

  /**
   * Gets the number of instances not classified (that is, for which no
   * prediction was made by the classifier). (Actually the sum of the weights of
   * these instances)
   *
   * @return the number of unclassified instances
   */
  public final double unclassified() {

    return m_Unclassified;
  }

  /**
   * Gets the percentage of instances not classified (that is, for which no
   * prediction was made by the classifier).
   *
   * @return the percent of unclassified instances (between 0 and 100)
   */
  public final double pctUnclassified() {

    return 100 * m_Unclassified / m_WithClass;
  }

  /**
   * Returns the estimated error rate or the root mean squared error (if the
   * class is numeric). If a cost matrix was given this error rate gives the
   * average cost.
   *
   * @return the estimated error rate (between 0 and 1, or between 0 and maximum
   *         cost)
   */
  public final double errorRate() {

    if (!m_ClassIsNominal) {
      return Math.sqrt(m_SumSqrErr / (m_WithClass - m_Unclassified));
    }
    if (m_CostMatrix == null) {
      return m_Incorrect / m_WithClass;
    } else {
      return avgCost();
    }
  }

  /**
   * Returns value of kappa statistic if class is nominal.
   *
   * @return the value of the kappa statistic
   */
  public final double kappa() {

    double[] sumRows = new double[m_ConfusionMatrix.length];
    double[] sumColumns = new double[m_ConfusionMatrix.length];
    double sumOfWeights = 0;
    for (int i = 0; i < m_ConfusionMatrix.length; i++) {
      for (int j = 0; j < m_ConfusionMatrix.length; j++) {
        sumRows[i] += m_ConfusionMatrix[i][j];
        sumColumns[j] += m_ConfusionMatrix[i][j];
        sumOfWeights += m_ConfusionMatrix[i][j];
      }
    }
    double correct = 0, chanceAgreement = 0;
    for (int i = 0; i < m_ConfusionMatrix.length; i++) {
      chanceAgreement += (sumRows[i] * sumColumns[i]);
      correct += m_ConfusionMatrix[i][i];
    }
    chanceAgreement /= (sumOfWeights * sumOfWeights);
    correct /= sumOfWeights;

    if (chanceAgreement < 1) {
      return (correct - chanceAgreement) / (1 - chanceAgreement);
    } else {
      return 1;
    }
  }

  /**
   * Returns the correlation coefficient if the class is numeric.
   *
   * @return the correlation coefficient
   * @throws Exception if class is not numeric
   */
  public final double correlationCoefficient() throws Exception {

    if (m_ClassIsNominal) {
      throw new Exception("Can't compute correlation coefficient: "
        + "class is nominal!");
    }

    double correlation = 0;
    double varActual =
      m_SumSqrClass - m_SumClass * m_SumClass / (m_WithClass - m_Unclassified);
    double varPredicted =
      m_SumSqrPredicted - m_SumPredicted * m_SumPredicted
        / (m_WithClass - m_Unclassified);
    double varProd =
      m_SumClassPredicted - m_SumClass * m_SumPredicted
        / (m_WithClass - m_Unclassified);

    if (varActual * varPredicted <= 0) {
      correlation = 0.0;
    } else {
      correlation = varProd / Math.sqrt(varActual * varPredicted);
    }

    return correlation;
  }

  /**
   * Returns the mean absolute error. Refers to the error of the predicted
   * values for numeric classes, and the error of the predicted probability
   * distribution for nominal classes.
   *
   * @return the mean absolute error
   */
  public final double meanAbsoluteError() {

    return m_SumAbsErr / (m_WithClass - m_Unclassified);
  }

  /**
   * Returns the mean absolute error of the prior.
   *
   * @return the mean absolute error
   */
  public final double meanPriorAbsoluteError() {

    if (m_NoPriors) {
      return Double.NaN;
    }

    return m_SumPriorAbsErr / m_WithClass;
  }

  /**
   * Returns the relative absolute error.
   *
   * @return the relative absolute error
   * @throws Exception if it can't be computed
   */
  public final double relativeAbsoluteError() throws Exception {

    if (m_NoPriors) {
      return Double.NaN;
    }

    return 100 * meanAbsoluteError() / meanPriorAbsoluteError();
  }

  /**
   * Returns the root mean squared error.
   *
   * @return the root mean squared error
   */
  public final double rootMeanSquaredError() {

    return Math.sqrt(m_SumSqrErr / (m_WithClass - m_Unclassified));
  }

  /**
   * Returns the root mean prior squared error.
   *
   * @return the root mean prior squared error
   */
  public final double rootMeanPriorSquaredError() {

    if (m_NoPriors) {
      return Double.NaN;
    }

    return Math.sqrt(m_SumPriorSqrErr / m_WithClass);
  }

  /**
   * Returns the root relative squared error if the class is numeric.
   *
   * @return the root relative squared error
   */
  public final double rootRelativeSquaredError() {

    if (m_NoPriors) {
      return Double.NaN;
    }

    return 100.0 * rootMeanSquaredError() / rootMeanPriorSquaredError();
  }

  /**
   * Returns the mean base-2 log loss wrt the null model. Just calls
   * SFMeanPriorEntropy.
   *
   * @return the null model entropy per instance
   */
  public final double priorEntropy() {

    // The previous version of this method calculated
    // mean base-2 log loss for the null model wrt the
    // instances passed into setPriors(). Now, this method will
    // return the loss for the null model wrt to the test
    // instances (whatever they are).
    return SFMeanPriorEntropy();
  }

  /**
   * Return the total Kononenko & Bratko Information score in bits.
   *
   * @return the K&B information score
   * @throws Exception if the class is not nominal
   */
  public final double KBInformation() throws Exception {

    if (!m_ClassIsNominal) {
      throw new Exception("Can't compute K&B Info score: " + "class numeric!");
    }

    if (m_NoPriors) {
      return Double.NaN;
    }

    return m_SumKBInfo;
  }

  /**
   * Return the Kononenko & Bratko Information score in bits per instance.
   *
   * @return the K&B information score
   * @throws Exception if the class is not nominal
   */
  public final double KBMeanInformation() throws Exception {

    if (!m_ClassIsNominal) {
      throw new Exception("Can't compute K&B Info score: class numeric!");
    }

    if (m_NoPriors) {
      return Double.NaN;
    }

    return m_SumKBInfo / (m_WithClass - m_Unclassified);
  }

  /**
   * Return the Kononenko & Bratko Relative Information score.
   * Differs slightly from the expression used in KB's paper
   * because it uses the mean log-loss of the TEST instances
   * wrt to the null model for normalization.
   *
   * @return the K&B relative information score
   * @throws Exception if the class is not nominal
   */
  public final double KBRelativeInformation() throws Exception {

    if (!m_ClassIsNominal) {
      throw new Exception("Can't compute K&B Info score: " + "class numeric!");
    }

    if (m_NoPriors) {
      return Double.NaN;
    }

    return 100.0 * KBMeanInformation() / priorEntropy();
  }

  /**
   * Returns the base-2 log loss wrt the null model.
   *
   * @return the total null model entropy
   */
  public final double SFPriorEntropy() {

    if (m_NoPriors || !m_ComplexityStatisticsAvailable) {
      return Double.NaN;
    }

    return m_SumPriorEntropy;
  }

  /**
   * Returns the mean base-2 log loss wrt the null model.
   *
   * @return the null model entropy per instance
   */
  public final double SFMeanPriorEntropy() {

    if (m_NoPriors || !m_ComplexityStatisticsAvailable) {
      return Double.NaN;
    }

    return m_SumPriorEntropy / m_WithClass;
  }

  /**
   * Returns the base-2 log loss wrt the scheme.
   *
   * @return the total scheme entropy
   */
  public final double SFSchemeEntropy() {

    if (!m_ComplexityStatisticsAvailable) {
      return Double.NaN;
    }

    return m_SumSchemeEntropy;
  }

  /**
   * Returns the mean base-2 log loss wrt the scheme.
   *
   * @return the scheme entropy per instance
   */
  public final double SFMeanSchemeEntropy() {

    if (!m_ComplexityStatisticsAvailable) {
      return Double.NaN;
    }

    return m_SumSchemeEntropy / (m_WithClass - m_Unclassified);
  }

  /**
   * Returns the difference in base-2 log loss between null model and scheme.
   *
   * @return the total "SF score"
   */
  public final double SFEntropyGain() {

    if (m_NoPriors || !m_ComplexityStatisticsAvailable) {
      return Double.NaN;
    }

    return m_SumPriorEntropy - m_SumSchemeEntropy;
  }

  /**
   * Returns the mean difference in base-2 log loss between null model and scheme.
   *
   * @return the "SF score" per instance
   */
  public final double SFMeanEntropyGain() {

    if (m_NoPriors || !m_ComplexityStatisticsAvailable) {
      return Double.NaN;
    }

    return (m_SumPriorEntropy - m_SumSchemeEntropy)
      / (m_WithClass - m_Unclassified);
  }

  /**
   * Output the cumulative margin distribution as a string suitable for input
   * for gnuplot or similar package.
   *
   * @return the cumulative margin distribution
   * @throws Exception if the class attribute is nominal
   */
  public String toCumulativeMarginDistributionString() throws Exception {

    if (!m_ClassIsNominal) {
      throw new Exception("Class must be nominal for margin distributions");
    }
    String result = "";
    double cumulativeCount = 0;
    double margin;
    for (int i = 0; i <= k_MarginResolution; i++) {
      if (m_MarginCounts[i] != 0) {
        cumulativeCount += m_MarginCounts[i];
        margin = i * 2.0 / k_MarginResolution - 1.0;
        result =
          result + Utils.doubleToString(margin, 7, 3) + ' '
            + Utils.doubleToString(cumulativeCount * 100 / m_WithClass, 7, 3)
            + '\n';
      } else if (i == 0) {
        result =
          Utils.doubleToString(-1.0, 7, 3) + ' '
            + Utils.doubleToString(0, 7, 3) + '\n';
      }
    }
    return result;
  }

  /**
   * Calls toSummaryString() with no title and no complexity stats.
   *
   * @return a summary description of the classifier evaluation
   */
  @Override
  public String toSummaryString() {

    return toSummaryString("", false);
  }

  /**
   * Calls toSummaryString() with a default title.
   *
   * @param printComplexityStatistics if true, complexity statistics are
   *          returned as well
   * @return the summary string
   */
  public String toSummaryString(boolean printComplexityStatistics) {

    return toSummaryString("=== Summary ===\n", printComplexityStatistics);
  }

  /**
   * Outputs the performance statistics in summary form. Lists number (and
   * percentage) of instances classified correctly, incorrectly and
   * unclassified. Outputs the total number of instances classified, and the
   * number of instances (if any) that had no class value provided.
   *
   * @param title the title for the statistics
   * @param printComplexityStatistics if true, complexity statistics are
   *          returned as well
   * @return the summary as a String
   */
  public String
    toSummaryString(String title, boolean printComplexityStatistics) {

    StringBuffer text = new StringBuffer();

    if (printComplexityStatistics && m_NoPriors) {
      printComplexityStatistics = false;
      System.err
        .println("Priors disabled, cannot print complexity statistics!");
    }

    text.append(title + "\n");
    try {
      if (m_WithClass > 0) {
        if (m_ClassIsNominal) {
          boolean displayCorrect = m_metricsToDisplay.contains("correct");
          boolean displayIncorrect = m_metricsToDisplay.contains("incorrect");
          boolean displayKappa = m_metricsToDisplay.contains("kappa");
          boolean displayTotalCost = m_metricsToDisplay.contains("total cost");
          boolean displayAverageCost =
            m_metricsToDisplay.contains("average cost");

          if (displayCorrect) {
            text.append("Correctly Classified Instances     ");
            text.append(Utils.doubleToString(correct(), 12, 4) + "     "
              + Utils.doubleToString(pctCorrect(), 12, 4) + " %\n");
          }
          if (displayIncorrect) {
            text.append("Incorrectly Classified Instances   ");
            text.append(Utils.doubleToString(incorrect(), 12, 4) + "     "
              + Utils.doubleToString(pctIncorrect(), 12, 4) + " %\n");
          }
          if (displayKappa) {
            text.append("Kappa statistic                    ");
            text.append(Utils.doubleToString(kappa(), 12, 4) + "\n");
          }

          if (m_CostMatrix != null) {
            if (displayTotalCost) {
              text.append("Total Cost                         ");
              text.append(Utils.doubleToString(totalCost(), 12, 4) + "\n");
            }
            if (displayAverageCost) {
              text.append("Average Cost                       ");
              text.append(Utils.doubleToString(avgCost(), 12, 4) + "\n");
            }
          }
          if (printComplexityStatistics) {
            boolean displayKBRelative =
              m_metricsToDisplay.contains("kb relative");
            boolean displayKBInfo =
              m_metricsToDisplay.contains("kb information");
            if (displayKBRelative) {
              text.append("K&B Relative Info Score            ");
              text.append(Utils.doubleToString(KBRelativeInformation(), 12, 4)
                + " %\n");
            }
            if (displayKBInfo) {
              text.append("K&B Information Score              ");
              text.append(Utils.doubleToString(KBInformation(), 12, 4)
                + " bits");
              text.append(Utils.doubleToString(KBMeanInformation(), 12, 4)
                + " bits/instance\n");
            }
          }

          if (m_pluginMetrics != null) {
            for (AbstractEvaluationMetric m : m_pluginMetrics) {
              if (m instanceof StandardEvaluationMetric
                && m.appliesToNominalClass() && !m.appliesToNumericClass()) {
                String metricName = m.getMetricName().toLowerCase();
                boolean display = m_metricsToDisplay.contains(metricName);
                // For the GUI and the command line StandardEvaluationMetrics
                // are an "all or nothing" jobby (because we need the user to
                // supply how they should be displayed and formatted via the
                // toSummaryString() method
                if (display) {
                  String formattedS =
                    ((StandardEvaluationMetric) m).toSummaryString();
                  text.append(formattedS);
                }
              }
            }
          }
        } else {
          boolean displayCorrelation =
            m_metricsToDisplay.contains("correlation");
          if (displayCorrelation) {
            text.append("Correlation coefficient            ");
            text.append(Utils.doubleToString(correlationCoefficient(), 12, 4)
              + "\n");
          }

          if (m_pluginMetrics != null) {
            for (AbstractEvaluationMetric m : m_pluginMetrics) {
              if (m instanceof StandardEvaluationMetric
                && !m.appliesToNominalClass() && m.appliesToNumericClass()) {
                String metricName = m.getMetricName().toLowerCase();
                boolean display = m_metricsToDisplay.contains(metricName);

                if (display) {
                  String formattedS =
                    ((StandardEvaluationMetric) m).toSummaryString();
                  text.append(formattedS);
                }
              }
            }
          }
        }
        if (printComplexityStatistics && m_ComplexityStatisticsAvailable) {
          boolean displayComplexityOrder0 =
            m_metricsToDisplay.contains("complexity 0");
          boolean displayComplexityScheme =
            m_metricsToDisplay.contains("complexity scheme");
          boolean displayComplexityImprovement =
            m_metricsToDisplay.contains("complexity improvement");
          if (displayComplexityOrder0) {
            text.append("Class complexity | order 0         ");
            text
              .append(Utils.doubleToString(SFPriorEntropy(), 12, 4) + " bits");
            text.append(Utils.doubleToString(SFMeanPriorEntropy(), 12, 4)
              + " bits/instance\n");
          }
          if (displayComplexityScheme) {
            text.append("Class complexity | scheme          ");
            text.append(Utils.doubleToString(SFSchemeEntropy(), 12, 4)
              + " bits");
            text.append(Utils.doubleToString(SFMeanSchemeEntropy(), 12, 4)
              + " bits/instance\n");
          }
          if (displayComplexityImprovement) {
            text.append("Complexity improvement     (Sf)    ");
            text.append(Utils.doubleToString(SFEntropyGain(), 12, 4) + " bits");
            text.append(Utils.doubleToString(SFMeanEntropyGain(), 12, 4)
              + " bits/instance\n");
          }
        }

        if (printComplexityStatistics && m_pluginMetrics != null) {
          for (AbstractEvaluationMetric m : m_pluginMetrics) {
            if (m instanceof InformationTheoreticEvaluationMetric) {
              if ((m_ClassIsNominal && m.appliesToNominalClass())
                || (!m_ClassIsNominal && m.appliesToNumericClass())) {
                String metricName = m.getMetricName().toLowerCase();
                boolean display = m_metricsToDisplay.contains(metricName);
                List<String> statNames = m.getStatisticNames();
                for (String s : statNames) {
                  display =
                    (display && m_metricsToDisplay.contains(s.toLowerCase()));
                }
                if (display) {
                  String formattedS =
                    ((InformationTheoreticEvaluationMetric) m)
                      .toSummaryString();
                  text.append(formattedS);
                }
              }
            }
          }
        }

        boolean displayMAE = m_metricsToDisplay.contains("mae");
        boolean displayRMSE = m_metricsToDisplay.contains("rmse");
        boolean displayRAE = m_metricsToDisplay.contains("rae");
        boolean displayRRSE = m_metricsToDisplay.contains("rrse");

        if (displayMAE) {
          text.append("Mean absolute error                ");
          text.append(Utils.doubleToString(meanAbsoluteError(), 12, 4) + "\n");
        }
        if (displayRMSE) {
          text.append("Root mean squared error            ");
          text.append(Utils.doubleToString(rootMeanSquaredError(), 12, 4)
            + "\n");
        }
        if (!m_NoPriors) {
          if (displayRAE) {
            text.append("Relative absolute error            ");
            text.append(Utils.doubleToString(relativeAbsoluteError(), 12, 4)
              + " %\n");
          }
          if (displayRRSE) {
            text.append("Root relative squared error        ");
            text.append(Utils.doubleToString(rootRelativeSquaredError(), 12, 4)
              + " %\n");
          }
        }
        if (m_pluginMetrics != null) {
          for (AbstractEvaluationMetric m : m_pluginMetrics) {
            if (m instanceof StandardEvaluationMetric
              && m.appliesToNominalClass() && m.appliesToNumericClass()) {
              String metricName = m.getMetricName().toLowerCase();
              boolean display = m_metricsToDisplay.contains(metricName);
              List<String> statNames = m.getStatisticNames();
              for (String s : statNames) {
                display =
                  (display && m_metricsToDisplay.contains(s.toLowerCase()));
              }
              if (display) {
                String formattedS =
                  ((StandardEvaluationMetric) m).toSummaryString();
                text.append(formattedS);
              }
            }
          }
        }

        if (m_CoverageStatisticsAvailable) {
          boolean displayCoverage = m_metricsToDisplay.contains("coverage");
          boolean displayRegionSize =
            m_metricsToDisplay.contains("region size");

          if (displayCoverage) {
            text.append("Coverage of cases ("
              + Utils.doubleToString(m_ConfLevel, 4, 2) + " level)     ");
            text.append(Utils.doubleToString(
              coverageOfTestCasesByPredictedRegions(), 12, 4) + " %\n");
          }
          if (!m_NoPriors) {
            if (displayRegionSize) {
              text.append("Mean rel. region size ("
                + Utils.doubleToString(m_ConfLevel, 4, 2) + " level) ");
              text.append(Utils.doubleToString(sizeOfPredictedRegions(), 12, 4)
                + " %\n");
            }
          }
        }
      }
      if (Utils.gr(unclassified(), 0)) {
        text.append("UnClassified Instances             ");
        text.append(Utils.doubleToString(unclassified(), 12, 4) + "     "
          + Utils.doubleToString(pctUnclassified(), 12, 4) + " %\n");
      }
      text.append("Total Number of Instances          ");
      text.append(Utils.doubleToString(m_WithClass, 12, 4) + "\n");
      if (m_MissingClass > 0) {
        text.append("Ignored Class Unknown Instances            ");
        text.append(Utils.doubleToString(m_MissingClass, 12, 4) + "\n");
      }
    } catch (Exception ex) {
      // Should never occur since the class is known to be nominal
      // here
      System.err.println("Arggh - Must be a bug in Evaluation class");
      ex.printStackTrace();
    }

    return text.toString();
  }

  /**
   * Calls toMatrixString() with a default title.
   *
   * @return the confusion matrix as a string
   * @throws Exception if the class is numeric
   */
  public String toMatrixString() throws Exception {

    return toMatrixString("=== Confusion Matrix ===\n");
  }

  /**
   * Outputs the performance statistics as a classification confusion matrix.
   * For each class value, shows the distribution of predicted class values.
   *
   * @param title the title for the confusion matrix
   * @return the confusion matrix as a String
   * @throws Exception if the class is numeric
   */
  public String toMatrixString(String title) throws Exception {

    StringBuffer text = new StringBuffer();
    char[] IDChars =
      { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
    int IDWidth;
    boolean fractional = false;

    if (!m_ClassIsNominal) {
      throw new Exception("Evaluation: No confusion matrix possible!");
    }

    // Find the maximum value in the matrix
    // and check for fractional display requirement
    double maxval = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        double current = m_ConfusionMatrix[i][j];
        if (current < 0) {
          current *= -10;
        }
        if (current > maxval) {
          maxval = current;
        }
        double fract = current - Math.rint(current);
        if (!fractional && ((Math.log(fract) / Math.log(10)) >= -2)) {
          fractional = true;
        }
      }
    }

    IDWidth =
      1 + Math.max(
        (int) (Math.log(maxval) / Math.log(10) + (fractional ? 3 : 0)),
        (int) (Math.log(m_NumClasses) / Math.log(IDChars.length)));
    text.append(title).append("\n");
    for (int i = 0; i < m_NumClasses; i++) {
      if (fractional) {
        text.append(" ").append(num2ShortID(i, IDChars, IDWidth - 3))
          .append("   ");
      } else {
        text.append(" ").append(num2ShortID(i, IDChars, IDWidth));
      }
    }
    text.append("   <-- classified as\n");
    for (int i = 0; i < m_NumClasses; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        text.append(" ").append(
          Utils.doubleToString(m_ConfusionMatrix[i][j], IDWidth,
            (fractional ? 2 : 0)));
      }
      text.append(" | ").append(num2ShortID(i, IDChars, IDWidth)).append(" = ")
        .append(m_ClassNames[i]).append("\n");
    }
    return text.toString();
  }

  /**
   * Generates a breakdown of the accuracy for each class (with default title),
   * incorporating various information-retrieval statistics, such as true/false
   * positive rate, precision/recall/F-Measure. Should be useful for ROC curves,
   * recall/precision curves.
   *
   * @return the statistics presented as a string
   * @throws Exception if class is not nominal
   */
  public String toClassDetailsString() throws Exception {

    return toClassDetailsString("=== Detailed Accuracy By Class ===\n");
  }

  /**
   * Generates a breakdown of the accuracy for each class, incorporating various
   * information-retrieval statistics, such as true/false positive rate,
   * precision/recall/F-Measure. Should be useful for ROC curves,
   * recall/precision curves.
   *
   * @param title the title to prepend the stats string with
   * @return the statistics presented as a string
   * @throws Exception if class is not nominal
   */
  public String toClassDetailsString(String title) throws Exception {

    if (!m_ClassIsNominal) {
      throw new Exception("Evaluation: No per class statistics possible!");
    }

    boolean displayTP = m_metricsToDisplay.contains("tp rate");
    boolean displayFP = m_metricsToDisplay.contains("fp rate");
    boolean displayP = m_metricsToDisplay.contains("precision");
    boolean displayR = m_metricsToDisplay.contains("recall");
    boolean displayFM = m_metricsToDisplay.contains("f-measure");
    boolean displayMCC = m_metricsToDisplay.contains("mcc");
    boolean displayROC = m_metricsToDisplay.contains("roc area");
    boolean displayPRC = m_metricsToDisplay.contains("prc area");

    StringBuffer text =
      new StringBuffer(title + "\n                 "
        + (displayTP ? "TP Rate  " : "") + (displayFP ? "FP Rate  " : "")
        + (displayP ? "Precision  " : "") + (displayR ? "Recall   " : "")
        + (displayFM ? "F-Measure  " : "") + (displayMCC ? "MCC      " : "")
        + (displayROC ? "ROC Area  " : "") + (displayPRC ? "PRC Area  " : ""));

    if (m_pluginMetrics != null && m_pluginMetrics.size() > 0) {
      for (AbstractEvaluationMetric m : m_pluginMetrics) {
        if (m instanceof InformationRetrievalEvaluationMetric
          && m.appliesToNominalClass()) {
          String metricName = m.getMetricName().toLowerCase();
          if (m_metricsToDisplay.contains(metricName)) {
            List<String> statNames = m.getStatisticNames();
            for (String name : statNames) {
              if (m_metricsToDisplay.contains(name.toLowerCase())) {
                if (name.length() < 7) {
                  name = Utils.padRight(name, 7);
                }
                text.append(name).append("  ");
              }
            }
          }
        }
      }
    }

    text.append("Class\n");
    for (int i = 0; i < m_NumClasses; i++) {
      text.append("                 ");
      if (displayTP) {
        double tpr = truePositiveRate(i);
        if (Utils.isMissingValue(tpr)) {
          text.append("?        ");
        } else {
          text.append(String.format("%-9.3f", tpr));
        }
      }
      if (displayFP) {
        double fpr = falsePositiveRate(i);
        if (Utils.isMissingValue(fpr)) {
          text.append("?        ");
        } else {
          text.append(String.format("%-9.3f", fpr));
        }
      }
      if (displayP) {
        double p = precision(i);
        if (Utils.isMissingValue(p)) {
          text.append("?          ");
        } else {
          text.append(String.format("%-11.3f", precision(i)));
        }
      }
      if (displayR) {
        double r = recall(i);
        if (Utils.isMissingValue(r)) {
          text.append("?        ");
        } else {
          text.append(String.format("%-9.3f", recall(i)));
        }
      }
      if (displayFM) {
        double fm = fMeasure(i);
        if (Utils.isMissingValue(fm)) {
          text.append("?          ");
        } else {
          text.append(String.format("%-11.3f", fMeasure(i)));
        }
      }
      if (displayMCC) {
        double mat = matthewsCorrelationCoefficient(i);
        if (Utils.isMissingValue(mat)) {
          text.append("?        ");
        } else {
          text.append(String
            .format("%-9.3f", matthewsCorrelationCoefficient(i)));
        }
      }
      if (displayROC) {
        double rocVal = areaUnderROC(i);
        if (Utils.isMissingValue(rocVal)) {
          text.append("?         ");
        } else {
          text.append(String.format("%-10.3f", rocVal));
        }
      }
      if (displayPRC) {
        double prcVal = areaUnderPRC(i);
        if (Utils.isMissingValue(prcVal)) {
          text.append("?         ");
        } else {
          text.append(String.format("%-10.3f", prcVal));
        }
      }

      if (m_pluginMetrics != null && m_pluginMetrics.size() > 0) {
        for (AbstractEvaluationMetric m : m_pluginMetrics) {
          if (m instanceof InformationRetrievalEvaluationMetric
            && m.appliesToNominalClass()) {
            String metricName = m.getMetricName().toLowerCase();
            if (m_metricsToDisplay.contains(metricName)) {
              List<String> statNames = m.getStatisticNames();
              for (String name : statNames) {
                if (m_metricsToDisplay.contains(name.toLowerCase())) {
                  double stat =
                    ((InformationRetrievalEvaluationMetric) m).getStatistic(
                      name, i);
                  if (name.length() < 7) {
                    name = Utils.padRight(name, 7);
                  }
                  if (Utils.isMissingValue(stat)) {
                    Utils.padRight("?", name.length());
                  } else {
                    text.append(
                      String.format("%-" + name.length() + ".3f", stat))
                      .append("  ");
                  }
                }
              }
            }
          }
        }
      }

      text.append(m_ClassNames[i]).append('\n');
    }

    text.append("Weighted Avg.    ");
    if (displayTP) {
      double wtpr = weightedTruePositiveRate();
      if (Utils.isMissingValue(wtpr)) {
        text.append("?        ");
      } else {
        text.append(String.format("%-9.3f", wtpr));
      }
    }
    if (displayFP) {
      double wfpr = weightedFalsePositiveRate();
      if (Utils.isMissingValue(wfpr)) {
        text.append("?        ");
      } else {
        text.append(String.format("%-9.3f", wfpr));
      }
    }
    if (displayP) {
      double wp = weightedPrecision();
      if (Utils.isMissingValue(wp)) {
        text.append("?          ");
      } else {
        text.append(String.format("%-11.3f", wp));
      }
    }
    if (displayR) {
      double wr = weightedRecall();
      if (Utils.isMissingValue(wr)) {
        text.append("?        ");
      } else {
        text.append(String.format("%-9.3f", wr));
      }
    }
    if (displayFM) {
      double wf = weightedFMeasure();
      if (Utils.isMissingValue(wf)) {
        text.append("?          ");
      } else {
        text.append(String.format("%-11.3f", wf));
      }
    }
    if (displayMCC) {
      double wmc = weightedMatthewsCorrelation();
      if (Utils.isMissingValue(wmc)) {
        text.append("?        ");
      } else {
        text.append(String.format("%-9.3f", wmc));
      }
    }
    if (displayROC) {
      double wroc = weightedAreaUnderROC();
      if (Utils.isMissingValue(wroc)) {
        text.append("?         ");
      } else {
        text.append(String.format("%-10.3f", wroc));
      }
    }
    if (displayPRC) {
      double wprc = weightedAreaUnderPRC();
      if (Utils.isMissingValue(wprc)) {
        text.append("?         ");
      } else {
        text.append(String.format("%-10.3f", wprc));
      }
    }

    if (m_pluginMetrics != null && m_pluginMetrics.size() > 0) {
      for (AbstractEvaluationMetric m : m_pluginMetrics) {
        if (m instanceof InformationRetrievalEvaluationMetric
          && m.appliesToNominalClass()) {
          String metricName = m.getMetricName().toLowerCase();
          if (m_metricsToDisplay.contains(metricName)) {
            List<String> statNames = m.getStatisticNames();
            for (String name : statNames) {
              if (m_metricsToDisplay.contains(name.toLowerCase())) {
                double stat =
                  ((InformationRetrievalEvaluationMetric) m)
                    .getClassWeightedAverageStatistic(name);
                if (name.length() < 7) {
                  name = Utils.padRight(name, 7);
                }
                if (Utils.isMissingValue(stat)) {
                  Utils.padRight("?", name.length());
                } else {
                  text
                    .append(String.format("%-" + name.length() + ".3f", stat))
                    .append("  ");
                }
              }
            }
          }
        }
      }
    }

    text.append("\n");

    return text.toString();
  }

  /**
   * Calculate the number of true positives with respect to a particular class.
   * This is defined as
   * <p/>
   *
   * <pre>
   * correctly classified positives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double numTruePositives(int classIndex) {

    double correct = 0;
    for (int j = 0; j < m_NumClasses; j++) {
      if (j == classIndex) {
        correct += m_ConfusionMatrix[classIndex][j];
      }
    }
    return correct;
  }

  /**
   * Calculate the true positive rate with respect to a particular class. This
   * is defined as
   * <p/>
   *
   * <pre>
   * correctly classified positives
   * ------------------------------
   *       total positives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double truePositiveRate(int classIndex) {

    double correct = 0, total = 0;
    for (int j = 0; j < m_NumClasses; j++) {
      if (j == classIndex) {
        correct += m_ConfusionMatrix[classIndex][j];
      }
      total += m_ConfusionMatrix[classIndex][j];
    }
    return correct / total;
  }

  /**
   * Calculates the weighted (by class size) true positive rate.
   *
   * @return the weighted true positive rate.
   */
  public double weightedTruePositiveRate() {
    double[] classCounts = new double[m_NumClasses];
    double classCountSum = 0;

    for (int i = 0; i < m_NumClasses; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        classCounts[i] += m_ConfusionMatrix[i][j];
      }
      classCountSum += classCounts[i];
    }

    double truePosTotal = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      double temp = truePositiveRate(i);
      if (classCounts[i] > 0) { // If temp is NaN, we want the sum to also be NaN if count > 0
        truePosTotal += (temp * classCounts[i]);
      }
    }

    return truePosTotal / classCountSum;
  }

  /**
   * Calculate the number of true negatives with respect to a particular class.
   * This is defined as
   * <p/>
   *
   * <pre>
   * correctly classified negatives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double numTrueNegatives(int classIndex) {

    double correct = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i != classIndex) {
        for (int j = 0; j < m_NumClasses; j++) {
          if (j != classIndex) {
            correct += m_ConfusionMatrix[i][j];
          }
        }
      }
    }
    return correct;
  }

  /**
   * Calculate the true negative rate with respect to a particular class. This
   * is defined as
   * <p/>
   *
   * <pre>
   * correctly classified negatives
   * ------------------------------
   *       total negatives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double trueNegativeRate(int classIndex) {

    double correct = 0, total = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i != classIndex) {
        for (int j = 0; j < m_NumClasses; j++) {
          if (j != classIndex) {
            correct += m_ConfusionMatrix[i][j];
          }
          total += m_ConfusionMatrix[i][j];
        }
      }
    }
    return correct / total;
  }

  /**
   * Calculates the weighted (by class size) true negative rate.
   *
   * @return the weighted true negative rate.
   */
  public double weightedTrueNegativeRate() {
    double[] classCounts = new double[m_NumClasses];
    double classCountSum = 0;

    for (int i = 0; i < m_NumClasses; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        classCounts[i] += m_ConfusionMatrix[i][j];
      }
      classCountSum += classCounts[i];
    }

    double trueNegTotal = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      double temp = trueNegativeRate(i);
      if (classCounts[i] > 0) { // If temp is NaN, we want the sum to also be NaN if count > 0
        trueNegTotal += (temp * classCounts[i]);
      }
    }

    return trueNegTotal / classCountSum;
  }

  /**
   * Calculate number of false positives with respect to a particular class.
   * This is defined as
   * <p/>
   *
   * <pre>
   * incorrectly classified negatives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double numFalsePositives(int classIndex) {

    double incorrect = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i != classIndex) {
        for (int j = 0; j < m_NumClasses; j++) {
          if (j == classIndex) {
            incorrect += m_ConfusionMatrix[i][j];
          }
        }
      }
    }
    return incorrect;
  }

  /**
   * Calculate the false positive rate with respect to a particular class. This
   * is defined as
   * <p/>
   *
   * <pre>
   * incorrectly classified negatives
   * --------------------------------
   *        total negatives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double falsePositiveRate(int classIndex) {

    double incorrect = 0, total = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i != classIndex) {
        for (int j = 0; j < m_NumClasses; j++) {
          if (j == classIndex) {
            incorrect += m_ConfusionMatrix[i][j];
          }
          total += m_ConfusionMatrix[i][j];
        }
      }
    }
    return incorrect / total;
  }

  /**
   * Calculates the weighted (by class size) false positive rate.
   *
   * @return the weighted false positive rate.
   */
  public double weightedFalsePositiveRate() {
    double[] classCounts = new double[m_NumClasses];
    double classCountSum = 0;

    for (int i = 0; i < m_NumClasses; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        classCounts[i] += m_ConfusionMatrix[i][j];
      }
      classCountSum += classCounts[i];
    }

    double falsePosTotal = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      double temp = falsePositiveRate(i);
      if (classCounts[i] > 0) { // If temp is NaN, we want the sum to also be NaN if count > 0
        falsePosTotal += (temp * classCounts[i]);
      }
    }

    return falsePosTotal / classCountSum;
  }

  /**
   * Calculate number of false negatives with respect to a particular class.
   * This is defined as
   * <p/>
   *
   * <pre>
   * incorrectly classified positives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double numFalseNegatives(int classIndex) {

    double incorrect = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i == classIndex) {
        for (int j = 0; j < m_NumClasses; j++) {
          if (j != classIndex) {
            incorrect += m_ConfusionMatrix[i][j];
          }
        }
      }
    }
    return incorrect;
  }

  /**
   * Calculate the false negative rate with respect to a particular class. This
   * is defined as
   * <p/>
   *
   * <pre>
   * incorrectly classified positives
   * --------------------------------
   *        total positives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double falseNegativeRate(int classIndex) {

    double incorrect = 0, total = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i == classIndex) {
        for (int j = 0; j < m_NumClasses; j++) {
          if (j != classIndex) {
            incorrect += m_ConfusionMatrix[i][j];
          }
          total += m_ConfusionMatrix[i][j];
        }
      }
    }
    return incorrect / total;
  }

  /**
   * Calculates the weighted (by class size) false negative rate.
   *
   * @return the weighted false negative rate.
   */
  public double weightedFalseNegativeRate() {
    double[] classCounts = new double[m_NumClasses];
    double classCountSum = 0;

    for (int i = 0; i < m_NumClasses; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        classCounts[i] += m_ConfusionMatrix[i][j];
      }
      classCountSum += classCounts[i];
    }

    double falseNegTotal = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      double temp = falseNegativeRate(i);
      if (classCounts[i] > 0) { // If temp is NaN, we want the sum to also be NaN if count > 0
        falseNegTotal += (temp * classCounts[i]);
      }
    }

    return falseNegTotal / classCountSum;
  }

  /**
   * Calculates the matthews correlation coefficient (sometimes called phi
   * coefficient) for the supplied class
   *
   * @param classIndex the index of the class to compute the matthews
   *          correlation coefficient for
   *
   * @return the mathews correlation coefficient
   */
  public double matthewsCorrelationCoefficient(int classIndex) {
    double numTP = numTruePositives(classIndex);
    double numTN = numTrueNegatives(classIndex);
    double numFP = numFalsePositives(classIndex);
    double numFN = numFalseNegatives(classIndex);
    double n = (numTP * numTN) - (numFP * numFN);
    double d =
      (numTP + numFP) * (numTP + numFN) * (numTN + numFP) * (numTN + numFN);
    d = Math.sqrt(d);

    return n / d;
  }

  /**
   * Calculates the weighted (by class size) matthews correlation coefficient.
   *
   * @return the weighted matthews correlation coefficient.
   */
  public double weightedMatthewsCorrelation() {
    double[] classCounts = new double[m_NumClasses];
    double classCountSum = 0;

    for (int i = 0; i < m_NumClasses; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        classCounts[i] += m_ConfusionMatrix[i][j];
      }
      classCountSum += classCounts[i];
    }

    double mccTotal = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      double temp = matthewsCorrelationCoefficient(i);
      if (classCounts[i] > 0) { // If temp is NaN, we want the sum to also be NaN if count > 0
        mccTotal += (temp * classCounts[i]);
      }
    }

    return mccTotal / classCountSum;
  }

  /**
   * Calculate the recall with respect to a particular class. This is defined as
   * <p/>
   *
   * <pre>
   * correctly classified positives
   * ------------------------------
   *       total positives
   * </pre>
   * <p/>
   * (Which is also the same as the truePositiveRate.)
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the recall
   */
  public double recall(int classIndex) {

    return truePositiveRate(classIndex);
  }

  /**
   * Calculates the weighted (by class size) recall.
   *
   * @return the weighted recall.
   */
  public double weightedRecall() {
    return weightedTruePositiveRate();
  }

  /**
   * Calculate the precision with respect to a particular class. This is defined
   * as
   * <p/>
   *
   * <pre>
   * correctly classified positives
   * ------------------------------
   *  total predicted as positive
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the precision
   */
  public double precision(int classIndex) {

    double correct = 0, total = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i == classIndex) {
        correct += m_ConfusionMatrix[i][classIndex];
      }
      total += m_ConfusionMatrix[i][classIndex];
    }
    return correct / total;
  }

  /**
   * Calculates the weighted (by class size) precision.
   *
   * @return the weighted precision.
   */
  public double weightedPrecision() {
    double[] classCounts = new double[m_NumClasses];
    double classCountSum = 0;

    for (int i = 0; i < m_NumClasses; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        classCounts[i] += m_ConfusionMatrix[i][j];
      }
      classCountSum += classCounts[i];
    }

    double precisionTotal = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      double temp = precision(i);
      if (classCounts[i] > 0) { // If temp is NaN, we want the sum to also be NaN if count > 0
        precisionTotal += (temp * classCounts[i]);
      }
    }

    return precisionTotal / classCountSum;
  }

  /**
   * Calculate the F-Measure with respect to a particular class. This is defined
   * as
   * <p/>
   *
   * <pre>
   * 2 * recall * precision
   * ----------------------
   *   recall + precision
   * </pre>
   *
   * Returns zero when both precision and recall are zero
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the F-Measure
   */
  public double fMeasure(int classIndex) {

    double precision = precision(classIndex);
    double recall = recall(classIndex);
    if ((precision == 0) && (recall == 0)) {
      return 0;
    }
    return 2 * precision * recall / (precision + recall);
  }

  /**
   * Calculates the macro weighted (by class size) average F-Measure.
   *
   * @return the weighted F-Measure.
   */
  public double weightedFMeasure() {
    double[] classCounts = new double[m_NumClasses];
    double classCountSum = 0;

    for (int i = 0; i < m_NumClasses; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        classCounts[i] += m_ConfusionMatrix[i][j];
      }
      classCountSum += classCounts[i];
    }

    double fMeasureTotal = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      double temp = fMeasure(i);
      if (classCounts[i] > 0) { // If temp is NaN, we want the sum to also be NaN if count > 0
        fMeasureTotal += (temp * classCounts[i]);
      }
    }

    return fMeasureTotal / classCountSum;
  }

  /**
   * Unweighted macro-averaged F-measure. If some classes not present in the
   * test set, they're just skipped (since recall is undefined there anyway) .
   *
   * @return unweighted macro-averaged F-measure.
   * */
  public double unweightedMacroFmeasure() {
    weka.experiment.Stats rr = new weka.experiment.Stats();
    for (int c = 0; c < m_NumClasses; c++) {
      // skip if no testing positive cases of this class
      if (numTruePositives(c) + numFalseNegatives(c) > 0) {
        rr.add(fMeasure(c));
      }
    }
    rr.calculateDerived();
    return rr.mean;
  }

  /**
   * Unweighted micro-averaged F-measure. If some classes not present in the
   * test set, they have no effect.
   *
   * Note: if the test set is *single-label*, then this is the same as accuracy.
   *
   * @return unweighted micro-averaged F-measure.
   */
  public double unweightedMicroFmeasure() {
    double tp = 0;
    double fn = 0;
    double fp = 0;
    for (int c = 0; c < m_NumClasses; c++) {
      tp += numTruePositives(c);
      fn += numFalseNegatives(c);
      fp += numFalsePositives(c);
    }
    return 2 * tp / (2 * tp + fn + fp);
  }

  /**
   * Sets the class prior probabilities.
   *
   * @param train the training instances used to determine the prior
   *          probabilities
   * @throws Exception if the class attribute of the instances is not set
   */
  public void setPriors(Instances train) throws Exception {

    m_NoPriors = false;

    if (!m_ClassIsNominal) {

      m_NumTrainClassVals = 0;
      m_TrainClassVals = null;
      m_TrainClassWeights = null;
      m_PriorEstimator = null;

      m_MinTarget = Double.MAX_VALUE;
      m_MaxTarget = -Double.MAX_VALUE;

      for (int i = 0; i < train.numInstances(); i++) {
        Instance currentInst = train.instance(i);
        if (!currentInst.classIsMissing()) {
          addNumericTrainClass(currentInst.classValue(), currentInst.weight());
        }
      }

      m_ClassPriors[0] = m_ClassPriorsSum = 0;
      for (int i = 0; i < train.numInstances(); i++) {
        if (!train.instance(i).classIsMissing()) {
          m_ClassPriors[0] +=
            train.instance(i).classValue() * train.instance(i).weight();
          m_ClassPriorsSum += train.instance(i).weight();
        }
      }

    } else {
      for (int i = 0; i < m_NumClasses; i++) {
        m_ClassPriors[i] = 1;
      }
      m_ClassPriorsSum = m_NumClasses;
      for (int i = 0; i < train.numInstances(); i++) {
        if (!train.instance(i).classIsMissing()) {
          m_ClassPriors[(int) train.instance(i).classValue()] +=
            train.instance(i).weight();
          m_ClassPriorsSum += train.instance(i).weight();
        }
      }
      m_MaxTarget = m_NumClasses;
      m_MinTarget = 0;
    }
  }

  /**
   * Get the current weighted class counts.
   *
   * @return the weighted class counts
   */
  public double[] getClassPriors() {
    return m_ClassPriors;
  }

  /**
   * Updates the class prior probabilities or the mean respectively (when
   * incrementally training).
   *
   * @param instance the new training instance seen
   * @throws Exception if the class of the instance is not set
   */
  public void updatePriors(Instance instance) throws Exception {
    if (!instance.classIsMissing()) {
      if (!m_ClassIsNominal) {
        addNumericTrainClass(instance.classValue(), instance.weight());
        m_ClassPriors[0] += instance.classValue() * instance.weight();
        m_ClassPriorsSum += instance.weight();
      } else {
        m_ClassPriors[(int) instance.classValue()] += instance.weight();
        m_ClassPriorsSum += instance.weight();
      }
    }
  }

  /**
   * disables the use of priors, e.g., in case of de-serialized schemes that
   * have no access to the original training set, but are evaluated on a set
   * set.
   */
  public void useNoPriors() {
    m_NoPriors = true;
  }

  /**
   * Tests whether the current evaluation object is equal to another evaluation
   * object.
   *
   * @param obj the object to compare against
   * @return true if the two objects are equal
   */
  @Override
  public boolean equals(Object obj) {

    if ((obj == null) || !(obj.getClass().equals(this.getClass()))) {
      return false;
    }
    Evaluation cmp = (Evaluation) obj;
    if (m_ClassIsNominal != cmp.m_ClassIsNominal) {
      return false;
    }
    if (m_NumClasses != cmp.m_NumClasses) {
      return false;
    }

    if (m_Incorrect != cmp.m_Incorrect) {
      return false;
    }
    if (m_Correct != cmp.m_Correct) {
      return false;
    }
    if (m_Unclassified != cmp.m_Unclassified) {
      return false;
    }
    if (m_MissingClass != cmp.m_MissingClass) {
      return false;
    }
    if (m_WithClass != cmp.m_WithClass) {
      return false;
    }

    if (m_SumErr != cmp.m_SumErr) {
      return false;
    }
    if (m_SumAbsErr != cmp.m_SumAbsErr) {
      return false;
    }
    if (m_SumSqrErr != cmp.m_SumSqrErr) {
      return false;
    }
    if (m_SumClass != cmp.m_SumClass) {
      return false;
    }
    if (m_SumSqrClass != cmp.m_SumSqrClass) {
      return false;
    }
    if (m_SumPredicted != cmp.m_SumPredicted) {
      return false;
    }
    if (m_SumSqrPredicted != cmp.m_SumSqrPredicted) {
      return false;
    }
    if (m_SumClassPredicted != cmp.m_SumClassPredicted) {
      return false;
    }

    if (m_ClassIsNominal) {
      for (int i = 0; i < m_NumClasses; i++) {
        for (int j = 0; j < m_NumClasses; j++) {
          if (m_ConfusionMatrix[i][j] != cmp.m_ConfusionMatrix[i][j]) {
            return false;
          }
        }
      }
    }

    return true;
  }

  /**
   * Make up the help string giving all the command line options.
   *
   * @param classifier the classifier to include options for
   * @param globalInfo include the global information string for the classifier
   *          (if available).
   * @return a string detailing the valid command line options
   */
  protected static String makeOptionString(Classifier classifier,
    boolean globalInfo) {

    StringBuffer optionsText = new StringBuffer("");

    // General options
    optionsText.append("\n\nGeneral options:\n\n");
    optionsText.append("-h or -help\n");
    optionsText.append("\tOutput help information.\n");
    optionsText.append("-synopsis or -info\n");
    optionsText.append("\tOutput synopsis for classifier (use in conjunction "
      + " with -h)\n");
    optionsText.append("-t <name of training file>\n");
    optionsText.append("\tSets training file.\n");
    optionsText.append("-T <name of test file>\n");
    optionsText
      .append("\tSets test file. If missing, a cross-validation will be performed\n");
    optionsText.append("\ton the training data.\n");
    optionsText.append("-c <class index>\n");
    optionsText.append("\tSets index of class attribute (default: last).\n");
    optionsText.append("-x <number of folds>\n");
    optionsText
      .append("\tSets number of folds for cross-validation (default: 10).\n");
    optionsText.append("-no-cv\n");
    optionsText.append("\tDo not perform any cross validation.\n");
    optionsText.append("-force-batch-training\n");
    optionsText
      .append("\tAlways train classifier in batch mode, never incrementally.\n");
    optionsText.append("-split-percentage <percentage>\n");
    optionsText
      .append("\tSets the percentage for the train/test set split, e.g., 66.\n");
    optionsText.append("-preserve-order\n");
    optionsText.append("\tPreserves the order in the percentage split.\n");
    optionsText.append("-s <random number seed>\n");
    optionsText
      .append("\tSets random number seed for cross-validation or percentage split\n");
    optionsText.append("\t(default: 1).\n");
    optionsText.append("-m <name of file with cost matrix>\n");
    optionsText.append("\tSets file with cost matrix.\n");
    optionsText.append("-continue-iterating\n");
    optionsText.append("\tContinue training an IterativeClassifier model that has\n\t"
      + "been loaded via -l.\n");
    optionsText.append("-clean-up\n");
    optionsText.append("\tReduce storage size of an loaded IterativeClassifier\n" +
    "\tafter iterating. This effectively 'freezes' the model, and no further\n"
      + "\titeration is then possible.\n");
    optionsText
      .append("-toggle <comma-separated list of evaluation metric names>\n");
    optionsText
      .append("\tComma separated list of metric names to toggle in the output.\n\t"
        + "All metrics are output by default with the exception of 'Coverage' and "
        + "'Region size'.\n\t");
    optionsText.append("Available metrics:\n\t");
    List<String> metricsToDisplay =
      new ArrayList<String>(Arrays.asList(BUILT_IN_EVAL_METRICS));

    List<AbstractEvaluationMetric> pluginMetrics =
      AbstractEvaluationMetric.getPluginMetrics();
    if (pluginMetrics != null) {
      for (AbstractEvaluationMetric m : pluginMetrics) {
        if (m instanceof InformationRetrievalEvaluationMetric) {
          List<String> statNames = m.getStatisticNames();
          for (String s : statNames) {
            metricsToDisplay.add(s.toLowerCase());
          }
        } else {
          metricsToDisplay.add(m.getMetricName().toLowerCase());
        }
      }
    }

    int length = 0;
    for (int i = 0; i < metricsToDisplay.size(); i++) {
      optionsText.append(metricsToDisplay.get(i));
      length += metricsToDisplay.get(i).length();
      if (i != metricsToDisplay.size() - 1) {
        optionsText.append(",");
      }
      if (length >= 60) {
        optionsText.append("\n\t");
        length = 0;
      }
    }
    optionsText.append("\n");
    optionsText.append("-l <name of input file>\n");
    optionsText
      .append("\tSets model input file. In case the filename ends with '.xml',\n");
    optionsText
      .append("\ta PMML file is loaded or, if that fails, options are loaded\n");
    optionsText.append("\tfrom the XML file.\n");
    optionsText.append("-d <name of output file>\n");
    optionsText
      .append("\tSets model output file. In case the filename ends with '.xml',\n");
    optionsText
      .append("\tonly the options are saved to the XML file, not the model.\n");
    optionsText.append("-v\n");
    optionsText.append("\tOutputs no statistics for training data.\n");
    optionsText.append("-o\n");
    optionsText.append("\tOutputs statistics only, not the classifier.\n");
    optionsText.append("-output-models-for-training-splits\n");
    optionsText.append("\tOutput models for training splits if cross-validation or percentage-split evaluation is used.\n");
    optionsText.append("-do-not-output-per-class-statistics\n");
    optionsText.append("\tDo not output statistics for each class.\n");
    optionsText.append("-k\n");
    optionsText.append("\tOutputs information-theoretic statistics.\n");
    optionsText
      .append("-classifications \"weka.classifiers.evaluation.output.prediction.AbstractOutput + options\"\n");
    optionsText
      .append("\tUses the specified class for generating the classification output.\n");
    optionsText.append("\tE.g.: " + PlainText.class.getName() + "\n");
    optionsText.append("-p range\n");
    optionsText
      .append("\tOutputs predictions for test instances (or the train instances if\n");
    optionsText
      .append("\tno test instances provided and -no-cv is used), along with the \n");
    optionsText
      .append("\tattributes in the specified range (and nothing else). \n");
    optionsText.append("\tUse '-p 0' if no attributes are desired.\n");
    optionsText.append("\tDeprecated: use \"-classifications ...\" instead.\n");
    optionsText.append("-distribution\n");
    optionsText
      .append("\tOutputs the distribution instead of only the prediction\n");
    optionsText
      .append("\tin conjunction with the '-p' option (only nominal classes).\n");
    optionsText.append("\tDeprecated: use \"-classifications ...\" instead.\n");
    optionsText.append("-r\n");
    optionsText.append("\tOnly outputs cumulative margin distribution.\n");
    if (classifier instanceof Sourcable) {
      optionsText.append("-z <class name>\n");
      optionsText.append("\tOnly outputs the source representation"
        + " of the classifier,\n\tgiving it the supplied" + " name.\n");
    }
    if (classifier instanceof Drawable) {
      optionsText.append("-g\n");
      optionsText.append("\tOnly outputs the graph representation"
        + " of the classifier.\n");
    }
    optionsText.append("-xml filename | xml-string\n");
    optionsText
      .append("\tRetrieves the options from the XML-data instead of the "
        + "command line.\n");
    optionsText.append("-threshold-file <file>\n");
    optionsText
      .append("\tThe file to save the threshold data to.\n"
        + "\tThe format is determined by the extensions, e.g., '.arff' for ARFF \n"
        + "\tformat or '.csv' for CSV.\n");
    optionsText.append("-threshold-label <label>\n");
    optionsText
      .append("\tThe class label to determine the threshold data for\n"
        + "\t(default is the first label)\n");
    optionsText.append("-no-predictions\n");
    optionsText
      .append("\tTurns off the collection of predictions in order to conserve memory.\n");

    // Get scheme-specific options
    if (classifier instanceof OptionHandler) {
      optionsText.append("\nOptions specific to "
        + classifier.getClass().getName() + ":\n\n");
      Enumeration<Option> enu = ((OptionHandler) classifier).listOptions();
      while (enu.hasMoreElements()) {
        Option option = enu.nextElement();
        optionsText.append(option.synopsis() + '\n');
        optionsText.append(option.description() + "\n");
      }
    }

    // Get global information (if available)
    if (globalInfo) {
      try {
        String gi = getGlobalInfo(classifier);
        optionsText.append(gi);
      } catch (Exception ex) {
        // quietly ignore
      }
    }
    return optionsText.toString();
  }

  /**
   * Return the global info (if it exists) for the supplied classifier.
   * 
   * @param classifier the classifier to get the global info for
   * @return the global info (synopsis) for the classifier
   * @throws Exception if there is a problem reflecting on the classifier
   */
  protected static String getGlobalInfo(Classifier classifier) throws Exception {
    BeanInfo bi = Introspector.getBeanInfo(classifier.getClass());
    MethodDescriptor[] methods;
    methods = bi.getMethodDescriptors();
    Object[] args = {};
    String result =
      "\nSynopsis for " + classifier.getClass().getName() + ":\n\n";

    for (MethodDescriptor method : methods) {
      String name = method.getDisplayName();
      Method meth = method.getMethod();
      if (name.equals("globalInfo")) {
        String globalInfo = (String) (meth.invoke(classifier, args));
        result += globalInfo;
        break;
      }
    }

    return result;
  }

  /**
   * Method for generating indices for the confusion matrix.
   * 
   * @param num integer to format
   * @param IDChars the characters to use
   * @param IDWidth the width of the entry
   * @return the formatted integer as a string
   */
  protected String num2ShortID(int num, char[] IDChars, int IDWidth) {

    char ID[] = new char[IDWidth];
    int i;

    for (i = IDWidth - 1; i >= 0; i--) {
      ID[i] = IDChars[num % IDChars.length];
      num = num / IDChars.length - 1;
      if (num < 0) {
        break;
      }
    }
    for (i--; i >= 0; i--) {
      ID[i] = ' ';
    }

    return new String(ID);
  }

  /**
   * Convert a single prediction into a probability distribution with all zero
   * probabilities except the predicted value which has probability 1.0.
   * 
   * @param predictedClass the index of the predicted class
   * @return the probability distribution
   */
  protected double[] makeDistribution(double predictedClass) {

    double[] result = new double[m_NumClasses];
    if (Utils.isMissingValue(predictedClass)) {
      return result;
    }
    if (m_ClassIsNominal) {
      result[(int) predictedClass] = 1.0;
    } else {
      result[0] = predictedClass;
    }
    return result;
  }

  /**
   * Updates all the statistics about a classifiers performance for the current
   * test instance.
   * 
   * @param predictedDistribution the probabilities assigned to each class
   * @param instance the instance to be classified
   * @throws Exception if the class of the instance is not set
   */
  protected void updateStatsForClassifier(double[] predictedDistribution,
    Instance instance) throws Exception {

    int actualClass = (int) instance.classValue();

    if (!instance.classIsMissing()) {
      updateMargins(predictedDistribution, actualClass, instance.weight());

      // Determine the predicted class (doesn't detect multiple
      // classifications)
      int predictedClass = -1;
      double bestProb = 0.0;
      for (int i = 0; i < m_NumClasses; i++) {
        if (predictedDistribution[i] > bestProb) {
          predictedClass = i;
          bestProb = predictedDistribution[i];
        }
      }

      m_WithClass += instance.weight();

      // Determine misclassification cost
      if (m_CostMatrix != null) {
        if (predictedClass < 0) {
          // For missing predictions, we assume the worst possible cost.
          // This is pretty harsh.
          // Perhaps we could take the negative of the cost of a correct
          // prediction (-m_CostMatrix.getElement(actualClass,actualClass)),
          // although often this will be zero
          m_TotalCost +=
            instance.weight() * m_CostMatrix.getMaxCost(actualClass, instance);
        } else {
          m_TotalCost +=
            instance.weight()
              * m_CostMatrix.getElement(actualClass, predictedClass, instance);
        }
      }

      // Update counts when no class was predicted
      if (predictedClass < 0) {
        m_Unclassified += instance.weight();
        return;
      }

      double predictedProb =
        Math.max(MIN_SF_PROB, predictedDistribution[actualClass]);
      double priorProb =
        Math.max(MIN_SF_PROB, m_ClassPriors[actualClass] / m_ClassPriorsSum);
      if (predictedProb >= priorProb) {
        m_SumKBInfo +=
          (Utils.log2(predictedProb) - Utils.log2(priorProb))
            * instance.weight();
      } else {
        m_SumKBInfo -=
          (Utils.log2(1.0 - predictedProb) - Utils.log2(1.0 - priorProb))
            * instance.weight();
      }

      m_SumSchemeEntropy -= Utils.log2(predictedProb) * instance.weight();
      m_SumPriorEntropy -= Utils.log2(priorProb) * instance.weight();

      updateNumericScores(predictedDistribution,
        makeDistribution(instance.classValue()), instance.weight());

      // Update coverage stats
      int[] indices = Utils.stableSort(predictedDistribution);
      double sum = 0, sizeOfRegions = 0;
      for (int i = predictedDistribution.length - 1; i >= 0; i--) {
        if (sum >= m_ConfLevel) {
          break;
        }
        sum += predictedDistribution[indices[i]];
        sizeOfRegions++;
        if (actualClass == indices[i]) {
          m_TotalCoverage += instance.weight();
        }
      }
      m_TotalSizeOfRegions += instance.weight() * sizeOfRegions / (m_MaxTarget - m_MinTarget);

      // Update other stats
      m_ConfusionMatrix[actualClass][predictedClass] += instance.weight();
      if (predictedClass != actualClass) {
        m_Incorrect += instance.weight();
      } else {
        m_Correct += instance.weight();
      }
    } else {
      m_MissingClass += instance.weight();
    }

    if (m_pluginMetrics != null) {
      for (AbstractEvaluationMetric m : m_pluginMetrics) {
        if (m instanceof StandardEvaluationMetric) {
          ((StandardEvaluationMetric) m).updateStatsForClassifier(
            predictedDistribution, instance);
        } else if (m instanceof InformationRetrievalEvaluationMetric) {
          ((InformationRetrievalEvaluationMetric) m).updateStatsForClassifier(
            predictedDistribution, instance);
        } else if (m instanceof InformationTheoreticEvaluationMetric) {
          ((InformationTheoreticEvaluationMetric) m).updateStatsForClassifier(
            predictedDistribution, instance);
        }
      }
    }
  }

  /**
   * Updates stats for interval estimator based on current test instance.
   * 
   * @param classifier the interval estimator
   * @param classMissing the instance for which the intervals are computed,
   *          without a class value
   * @param classValue the class value of this instance
   * @throws Exception if intervals could not be computed successfully
   */
  protected void updateStatsForIntervalEstimator(IntervalEstimator classifier,
    Instance classMissing, double classValue) throws Exception {

    double[][] preds = classifier.predictIntervals(classMissing, m_ConfLevel);
    if (m_Predictions != null) {
      ((NumericPrediction) m_Predictions.get(m_Predictions.size() - 1))
        .setPredictionIntervals(preds);
    }
    for (double[] pred : preds) {
      m_TotalSizeOfRegions += classMissing.weight() * (pred[1] - pred[0]) / (m_MaxTarget - m_MinTarget);
    }
    for (double[] pred : preds) {
      if ((pred[1] >= classValue) && (pred[0] <= classValue)) {
        m_TotalCoverage += classMissing.weight();
        break;
      }
    }

    if (m_pluginMetrics != null) {
      for (AbstractEvaluationMetric m : m_pluginMetrics) {
        if (m instanceof IntervalBasedEvaluationMetric) {
          ((IntervalBasedEvaluationMetric) m).updateStatsForIntervalEstimator(
            classifier, classMissing, classValue);
        }
      }
    }
  }

  /**
   * Updates stats for conditional density estimator based on current test
   * instance.
   * 
   * @param classifier the conditional density estimator
   * @param classMissing the instance for which density is to be computed,
   *          without a class value
   * @param classValue the class value of this instance
   * @throws Exception if density could not be computed successfully
   */
  protected void updateStatsForConditionalDensityEstimator(
    ConditionalDensityEstimator classifier, Instance classMissing,
    double classValue) throws Exception {

    if (m_PriorEstimator == null) {
      setNumericPriorsFromBuffer();
    }
    m_SumSchemeEntropy -=
      classifier.logDensity(classMissing, classValue) * classMissing.weight()
        / Utils.log2;
    m_SumPriorEntropy -=
      m_PriorEstimator.logDensity(classValue) * classMissing.weight()
        / Utils.log2;
  }

  /**
   * Updates all the statistics about a predictors performance for the current
   * test instance.
   * 
   * @param predictedValue the numeric value the classifier predicts
   * @param instance the instance to be classified
   * @throws Exception if the class of the instance is not set
   */
  protected void updateStatsForPredictor(double predictedValue,
    Instance instance) throws Exception {

    if (!instance.classIsMissing()) {

      // Update stats
      m_WithClass += instance.weight();
      if (Utils.isMissingValue(predictedValue)) {
        m_Unclassified += instance.weight();
        return;
      }
      m_SumClass += instance.weight() * instance.classValue();
      m_SumSqrClass +=
        instance.weight() * instance.classValue() * instance.classValue();
      m_SumClassPredicted +=
        instance.weight() * instance.classValue() * predictedValue;
      m_SumPredicted += instance.weight() * predictedValue;
      m_SumSqrPredicted += instance.weight() * predictedValue * predictedValue;

      updateNumericScores(makeDistribution(predictedValue),
        makeDistribution(instance.classValue()), instance.weight());

    } else {
      m_MissingClass += instance.weight();
    }

    if (m_pluginMetrics != null) {
      for (AbstractEvaluationMetric m : m_pluginMetrics) {
        if (m instanceof StandardEvaluationMetric) {
          ((StandardEvaluationMetric) m).updateStatsForPredictor(
            predictedValue, instance);
        } else if (m instanceof InformationTheoreticEvaluationMetric) {
          ((InformationTheoreticEvaluationMetric) m).updateStatsForPredictor(
            predictedValue, instance);
        }
      }
    }
  }

  /**
   * Update the cumulative record of classification margins.
   * 
   * @param predictedDistribution the probability distribution predicted for the
   *          current instance
   * @param actualClass the index of the actual instance class
   * @param weight the weight assigned to the instance
   */
  protected void updateMargins(double[] predictedDistribution, int actualClass,
    double weight) {

    double probActual = predictedDistribution[actualClass];
    double probNext = 0;

    for (int i = 0; i < m_NumClasses; i++) {
      if ((i != actualClass) && (predictedDistribution[i] > probNext)) {
        probNext = predictedDistribution[i];
      }
    }

    double margin = probActual - probNext;
    int bin = (int) ((margin + 1.0) / 2.0 * k_MarginResolution);
    m_MarginCounts[bin] += weight;
  }

  /**
   * Update the numeric accuracy measures. For numeric classes, the accuracy is
   * between the actual and predicted class values. For nominal classes, the
   * accuracy is between the actual and predicted class probabilities.
   * 
   * @param predicted the predicted values
   * @param actual the actual value
   * @param weight the weight associated with this prediction
   */
  protected void updateNumericScores(double[] predicted, double[] actual,
    double weight) {

    double diff;
    double sumErr = 0, sumAbsErr = 0, sumSqrErr = 0;
    double sumPriorAbsErr = 0, sumPriorSqrErr = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      diff = predicted[i] - actual[i];
      sumErr += diff;
      sumAbsErr += Math.abs(diff);
      sumSqrErr += diff * diff;
      diff = (m_ClassPriors[i] / m_ClassPriorsSum) - actual[i];
      sumPriorAbsErr += Math.abs(diff);
      sumPriorSqrErr += diff * diff;
    }
    m_SumErr += weight * sumErr / m_NumClasses;
    m_SumAbsErr += weight * sumAbsErr / m_NumClasses;
    m_SumSqrErr += weight * sumSqrErr / m_NumClasses;
    m_SumPriorAbsErr += weight * sumPriorAbsErr / m_NumClasses;
    m_SumPriorSqrErr += weight * sumPriorSqrErr / m_NumClasses;
  }

  /**
   * Adds a numeric (non-missing) training class value and weight to the buffer
   * of stored values. Also updates minimum and maximum target value.
   * 
   * @param classValue the class value
   * @param weight the instance weight
   */
  protected void addNumericTrainClass(double classValue, double weight) {

    // Update minimum and maximum target value
    if (classValue > m_MaxTarget) {
      m_MaxTarget = classValue;
    }
    if (classValue < m_MinTarget) {
      m_MinTarget = classValue;
    }

    // Update buffer
    if (m_TrainClassVals == null) {
      m_TrainClassVals = new double[100];
      m_TrainClassWeights = new double[100];
    }
    if (m_NumTrainClassVals == m_TrainClassVals.length) {
      double[] temp = new double[m_TrainClassVals.length * 2];
      System.arraycopy(m_TrainClassVals, 0, temp, 0, m_TrainClassVals.length);
      m_TrainClassVals = temp;

      temp = new double[m_TrainClassWeights.length * 2];
      System.arraycopy(m_TrainClassWeights, 0, temp, 0,
        m_TrainClassWeights.length);
      m_TrainClassWeights = temp;
    }
    m_TrainClassVals[m_NumTrainClassVals] = classValue;
    m_TrainClassWeights[m_NumTrainClassVals] = weight;
    m_NumTrainClassVals++;
  }

  /**
   * Sets up the priors for numeric class attributes from the training class
   * values that have been seen so far.
   */
  protected void setNumericPriorsFromBuffer() {

    m_PriorEstimator = new UnivariateKernelEstimator();
    for (int i = 0; i < m_NumTrainClassVals; i++) {
      m_PriorEstimator.addValue(m_TrainClassVals[i], m_TrainClassWeights[i]);
    }
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
}
