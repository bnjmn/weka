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
 *    MLlibSVM.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mllib;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.classification.SVMModel;
import org.apache.spark.mllib.classification.SVMWithSGD;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.optimization.L1Updater;
import org.apache.spark.mllib.regression.LabeledPoint;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;
import weka.filters.StreamableFilter;
import weka.knowledgeflow.SingleThreadedExecution;

import java.util.List;

/**
 * Weka wrapper for the MLlib SVM classifier
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@SingleThreadedExecution
public class MLlibSVM extends MLlibClassifier {

  private static final long serialVersionUID = -6639185524841381694L;

  /**
   * Enum for the penalty type
   */
  public enum Penalty {
    L1, L2;
  }

  /** Holds the learned model */
  protected SVMModel m_svmModel;

  /** The number of iterations of SGD to run */
  protected int m_numIterations = 100;

  /** The step size/learning rate */
  protected double m_stepSize = 1.0;

  /** Regularization parameter */
  protected double m_regParam = 0.01;

  /** The mini batch fraction */
  protected double m_miniBatchFraction = 1.0;

  /** L2 is the default in Spark SVM */
  protected Penalty m_penalty = Penalty.L2;

  /**
   * About information
   *
   * @return the about info
   */
  public String globalInfo() {
    return "Spark MLlib binary class linear SVM wrapper classifier.";
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capabilities.Capability.MISSING_VALUES);

    // class
    result.enable(Capabilities.Capability.BINARY_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Set the number of SGD iterations to run
   *
   * @param numIterations the number of SGD iterations to run
   */
  @OptionMetadata(displayName = "Number of iterations", description = "Number "
    + "of iterations to perform (default = 100)", displayOrder = 1,
    commandLineParamName = "iterations",
    commandLineParamSynopsis = "-iterations <integer>")
  public void setNumIterations(int numIterations) {
    m_numIterations = numIterations;
  }

  /**
   * Get the number of SGD iterations to run
   *
   * @return the number of SGD iterations to run
   */
  public int getNumIterations() {
    return m_numIterations;
  }

  /**
   * Set the step size/learning rate
   *
   * @param stepSize the step size
   */
  @OptionMetadata(displayName = "Step size", description = "The step size to "
    + "use", displayOrder = 2, commandLineParamName = "step-size",
    commandLineParamSynopsis = "-step-size <number>")
  public void setStepSize(double stepSize) {
    m_stepSize = stepSize;
  }

  /**
   * Get the step size/learning rate
   *
   * @return the step size
   */
  public double getStepSize() {
    return m_stepSize;
  }

  /**
   * Set the regularization type
   *
   * @param p the regularization type
   */
  @OptionMetadata(displayName = "Penalty", description = "Update penalty "
    + "(default = L2)", displayOrder = 3, commandLineParamName = "penalty",
    commandLineParamSynopsis = "-penalty <L1 | L2>")
  public void setPenalty(Penalty p) {
    m_penalty = p;
  }

  /**
   * Get the regularization type
   *
   * @return the regularization type
   */
  public Penalty getPenalty() {
    return m_penalty;
  }

  /**
   * Set the regularization parameter value
   *
   * @param regParam the regularization parameter value
   */
  @OptionMetadata(displayName = "Regularization parameter",
    description = "The regularization value (defualt = 0.01)",
    displayOrder = 4, commandLineParamName = "reg-param",
    commandLineParamSynopsis = "-reg-param <number>")
  public void setRegularizationParameter(double regParam) {
    m_regParam = regParam;
  }

  /**
   * Get the regularization parameter value
   *
   * @return the regularization parameter value
   */
  public double getRegularizationParameter() {
    return m_regParam;
  }

  /**
   * Set the mini batch fraction to use
   *
   * @param miniBatchFraction the mini batch fraction
   */
  @OptionMetadata(displayName = "Mini batch fraction",
    description = "Fraction of data to use in mini batches (default = 1.0)",
    displayOrder = 5, commandLineParamName = "mini-batch",
    commandLineParamSynopsis = "-mini-batch <number>")
  public void setMiniBatchFraction(double miniBatchFraction) {
    m_miniBatchFraction = miniBatchFraction;
  }

  /**
   * Get the mini batch fraction to use
   *
   * @return the mini batch fraction
   */
  public double getMiniBatchFraction() {
    return m_miniBatchFraction;
  }

  /**
   * Learn the underlying SVM model.
   *
   * @param wekaData an RDD of {@code Instance} objects
   * @param headerWithSummary header (with summary attributes) for the data
   * @param preprocessors streamable filters for preprocessing the data
   * @param strategy the header of the data (with summary attributes)
   * @throws DistributedWekaException if a problem occurs
   */
  @Override
  public void learnModel(JavaRDD<Instance> wekaData,
    Instances headerWithSummary, List<StreamableFilter> preprocessors,
    CachingStrategy strategy) throws DistributedWekaException {

    Filter preconstructed = loadPreconstructedFilterIfNecessary();

    JavaRDD<LabeledPoint> mlLibData =
      m_datasetMaker.labeledPointRDDFirstBatch(headerWithSummary, wekaData,
        !getDontReplaceMissingValues(), (PreconstructedFilter) preconstructed,
        preprocessors, Integer.MAX_VALUE, false, strategy);

    m_classAtt = headerWithSummary.attribute(m_datasetMaker.getClassIndex());

    if (!m_classAtt.isNominal() && m_classAtt.numValues() != 2) {
      throw new DistributedWekaException("Class attribute must be binary!");
    }

    SVMWithSGD svmSGD = new SVMWithSGD();
    svmSGD.optimizer().setNumIterations(m_numIterations);
    svmSGD.optimizer().setRegParam(m_regParam);
    svmSGD.optimizer().setStepSize(m_stepSize);
    svmSGD.optimizer().setMiniBatchFraction(m_miniBatchFraction);
    if (m_penalty != Penalty.L2) {
      svmSGD.optimizer().setUpdater(new L1Updater());
    }
    m_svmModel = svmSGD.run(mlLibData.rdd());
  }

  /**
   * Predict a test point using the learned model
   *
   * @param test a {@code Vector} containing the test point
   * @return the predicted value (index of class for classification, or actual
   *         value for regression)
   * @throws DistributedWekaException if a problem occurs
   */
  @Override
  public double predict(Vector test) throws DistributedWekaException {
    if (m_svmModel == null) {
      throw new DistributedWekaException("No model built yet!");
    }

    return m_svmModel.predict(test);
  }

  /**
   * Get a textual description of the model
   *
   * @return a textual description of the model
   */
  public String toString() {
    if (m_svmModel == null) {
      return "No model built yet";
    }
    return m_svmModel.toString();
  }

  @Override
  public void run(Object toRun, String[] options) {
    runClassifier((MLlibSVM) toRun, options);
  }

  public static void main(String[] args) {
    MLlibSVM svm = new MLlibSVM();
    svm.run(svm, args);
  }
}
