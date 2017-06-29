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
 *    MLlibLinearRegressionSGD.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mllib;

import distributed.core.DistributedJobConfig;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.GeneralizedLinearModel;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.regression.LassoWithSGD;
import org.apache.spark.mllib.regression.LinearRegressionWithSGD;
import org.apache.spark.mllib.regression.RidgeRegressionWithSGD;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;
import weka.filters.StreamableFilter;
import weka.knowledgeflow.SingleThreadedExecution;

import java.util.List;

/**
 * Weka wrapper classifier for the MLlib linear regression scheme
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@SingleThreadedExecution
public class MLlibLinearRegressionSGD extends MLlibClassifier {
  private static final long serialVersionUID = -6870703150482123262L;

  /** The learned model */
  protected GeneralizedLinearModel m_model;

  /** Default regularization to use */
  protected Regularization m_regularization = Regularization.None;

  /** Default number of iterations to perform */
  protected int m_numIterations = 100;

  /** Default learning rate */
  protected double m_stepSize = 1e-6;

  /** Default mini-batch fraction */
  protected double m_miniBatchFraction = 1.0;

  /** Default regularization penalty */
  protected double m_regularizationParam = 0.01;

  /**
   * Constructor
   */
  public MLlibLinearRegressionSGD() {
    m_numDecimalPlaces = 4;
  }

  /**
   * About information
   *
   * @return about info
   */
  public String globalInfo() {
    return "Spark MLlib SGD linear regression.";
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
    result.enable(Capabilities.Capability.NUMERIC_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Set the type of regularization to use
   *
   * @param reg the type of regularization to use
   */
  @OptionMetadata(displayName = "Regularization",
    description = "Regularization to use: None (OLS), L1 (Lasso), L2 (ridge). "
      + "Default = None.", commandLineParamName = "regularization",
    commandLineParamSynopsis = "-regularization <None|L1|L2>",
    displayOrder = 11)
  public void setRegularization(Regularization reg) {
    m_regularization = reg;
  }

  /**
   * Get the type of regularization to use
   *
   * @return the type of regularization
   */
  public Regularization getRegularization() {
    return m_regularization;
  }

  /**
   * Set the number of iterations to perform
   *
   * @param iterations the number of iterations
   */
  @OptionMetadata(displayName = "Number of iterations",
    description = "Number of "
      + "iterations (epochs) to perform. Default = 100.",
    commandLineParamName = "I", commandLineParamSynopsis = "-I <integer>",
    displayOrder = 12)
  public void setNumIterations(int iterations) {
    m_numIterations = iterations;
  }

  /**
   * Get the number of iterations to perform
   *
   * @return the number of iterations
   */
  public int getNumIterations() {
    return m_numIterations;
  }

  /**
   * Set the step size/learning rate
   *
   * @param stepSize step size
   */
  @OptionMetadata(displayName = "Step size",
    description = "Step size (learning " + "rate). Default = 1e-6.",
    commandLineParamName = "L", commandLineParamSynopsis = "-L <double>",
    displayOrder = 13)
  public void setStepSize(double stepSize) {
    m_stepSize = stepSize;
  }

  /**
   * Get the step size/learning rate
   *
   * @return step size
   */
  public double getStepSize() {
    return m_stepSize;
  }

  /**
   * Set the mini batch fraction
   *
   * @param miniBatchFraction the mini batch fraction
   */
  @OptionMetadata(
    displayName = "Mini batch fraction",
    description = "Fraction "
      + "of the data to use at each iteration for computing a stochastic gradient "
      + "(Default = 1.0)", commandLineParamName = "mini-batch",
    commandLineParamSynopsis = "-mini-batch <double>", displayOrder = 14)
  public
    void setMiniBatchFraction(double miniBatchFraction) {
    m_miniBatchFraction = miniBatchFraction;
  }

  /**
   * Get the mini batch fraction
   *
   * @return the mini batch fraction
   */
  public double getMiniBatchFraction() {
    return m_miniBatchFraction;
  }

  /**
   * Set the regularization parameter
   *
   * @param regularizationParameter the regularization parameter
   */
  @OptionMetadata(displayName = "Regularization parameter",
    description = "The value of the regularization parameter (default = 0.01)",
    commandLineParamName = "reg-param",
    commandLineParamSynopsis = "-reg-param <double>", displayOrder = 15)
  public void setRegularizationParameter(double regularizationParameter) {
    m_regularizationParam = regularizationParameter;
  }

  /**
   * Get the regularization parameter
   *
   * @return the regularization parameter
   */
  public double getRegularizationParameter() {
    return m_regularizationParam;
  }

  /**
   * Set options
   *
   * @param opts an array of options to set
   * @throws Exception if a problem occurs
   */
  @Override
  public void setOptions(String[] opts) throws Exception {
    String reg = Utils.getOption("regularization", opts);
    if (!DistributedJobConfig.isEmpty(reg)) {
      Regularization toSet = Regularization.None;
      for (Regularization i : Regularization.values()) {
        if (i.toString().equalsIgnoreCase(reg)) {
          toSet = i;
          break;
        }
      }
      setRegularization(toSet);
    }

    super.setOptions(opts);
  }

  /**
   * Learn the underlying MLlib model.
   *
   * @param wekaData an RDD of {@code Instance} objects
   * @param headerWithSummary
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
    if (!m_classAtt.isNumeric()) {
      throw new DistributedWekaException("Class attribute must be numeric");
    }

    if (m_regularization == Regularization.None) {
      m_model =
        LinearRegressionWithSGD.train(mlLibData.rdd(), m_numIterations,
          m_stepSize, m_miniBatchFraction);
    } else if (m_regularization == Regularization.L1) {
      m_model =
        LassoWithSGD.train(mlLibData.rdd(), m_numIterations, m_stepSize,
          m_regularizationParam, m_miniBatchFraction);
    } else {
      m_model =
        RidgeRegressionWithSGD.train(mlLibData.rdd(), m_numIterations,
          m_stepSize, m_regularizationParam, m_miniBatchFraction);
    }
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
    if (m_model == null) {
      throw new DistributedWekaException("No model built yet!");
    }
    return m_model.predict(test);
  }

  /**
   * Get a textual description of the model
   *
   * @return a textual description of the model
   */
  public String toString() {
    if (m_model == null) {
      return "No model built yet";
    }

    StringBuilder b = new StringBuilder();
    b.append(m_model.toString()).append("\n\n");
    Instances data = m_datasetMaker.getTransformedHeader();
    b.append(data.classAttribute().name()).append(" =").append("\n\n");
    double[] coeffs = m_model.weights().toArray();
    double intercept = m_model.intercept();
    int index = 0;
    for (int i = 0; i < data.numAttributes(); i++) {
      if (i != data.classIndex()) {
        b.append(Utils.doubleToString(coeffs[index], 12, getNumDecimalPlaces()))
          .append(" * ");
        b.append(data.attribute(i).name()).append("\n");
        index++;
      }
    }
    b.append(Utils.doubleToString(intercept, 12, getNumDecimalPlaces()))
      .append("\n");

    return b.toString();
  }

  @Override
  public void run(Object toRun, final String[] options) {
    runClassifier((MLlibLinearRegressionSGD) toRun, options);
  }

  public static void main(String[] args) {
    MLlibLinearRegressionSGD lr = new MLlibLinearRegressionSGD();
    lr.run(lr, args);
  }

  public enum Regularization {
    None, L1, L2;
  }
}
