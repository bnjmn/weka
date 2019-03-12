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
 *    MLlibNaiveBayes.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mllib;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;
import weka.filters.StreamableFilter;
import weka.filters.supervised.attribute.Discretize;
import weka.knowledgeflow.SingleThreadedExecution;

import java.util.ArrayList;
import java.util.List;

/**
 * Weka wrapper classifier for the MLlib naive bayes scheme
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@SingleThreadedExecution
public class MLlibNaiveBayes extends MLlibClassifier {

  private static final long serialVersionUID = -5258197565273034975L;

  /**
   * Enum for the type of NB model
   */
  public enum ModelType {
    bernoulli, multinomial;
  }

  /** The type of NB model */
  protected ModelType m_modelType = ModelType.bernoulli;

  /** The laplace correction (I think) */
  protected double m_lambda = 1.0;

  /** The learned model */
  protected NaiveBayesModel m_naiveBayesModel;

  /**
   * Supervised discretization of numeric attributes (when running in desktop
   * mode only)
   */
  protected Discretize m_discretize;

  /** Track indexes of class labels that do not have training examples */
  protected List<Integer> m_emptyClassList = new ArrayList<>();

  /**
   * About information
   *
   * @return about info
   */
  public String globalInfo() {
    return "Spark Naive Bayes wrapper classifier";
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
    result.enable(Capabilities.Capability.NOMINAL_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Set the type of Naive Bayes model to learn
   *
   * @param type the type of NB model to learn
   */
  @OptionMetadata(displayName = "Model type", description = "The type of "
    + "naive Bayes model, bernoulli or multinomial", displayOrder = 1,
    commandLineParamName = "model-type",
    commandLineParamSynopsis = "-model-type <bernoulli | multinomial>")
  public void setModelType(ModelType type) {
    m_modelType = type;
  }

  /**
   * Get the type of Naive Bayes model to learn
   *
   * @return the type of NB model to learn
   */
  public ModelType getModelType() {
    return m_modelType;
  }

  /**
   * Build the classifier from the standard Weka side of the API (i.e. normal
   * Instances objects that can be loaded into main memory). This can apply
   * supervised discretization to handle numeric attributes.
   *
   * @param training the training data
   * @throws Exception if a problem occurs
   */
  @Override
  public void buildClassifier(Instances training) throws Exception {
    // check for numeric attributes (bernoulli only)
    if (m_modelType == ModelType.bernoulli) {
      StringBuilder b = new StringBuilder();
      if (training.checkForAttributeType(Attribute.NUMERIC)) {
        for (int i = 0; i < training.numAttributes(); i++) {
          if (i != training.classIndex() && training.attribute(i).isNumeric()) {
            AttributeStats stats = training.attributeStats(i);
            b.append(i + 1).append(",");
          }
        }
      }

      if (b.length() > 0) {
        String toDiscretize = b.substring(0, b.length() - 1);
        m_discretize = new Discretize();
        m_discretize.setAttributeIndices(b.toString());
        m_discretize.setInputFormat(training);
        training = Filter.useFilter(training, m_discretize);
      }
    } else {
      for (int i = 0; i < training.numAttributes(); i++) {
        if (i != training.classIndex()) {
          if (!training.attribute(i).isNumeric()) {
            throw new Exception("MLlibNaiveBayes can only handle numeric "
              + "attributes in multinomial mode");
          }
        }
      }
    }

    // check for empty classes
    AttributeStats classStats = training.attributeStats(training.classIndex());
    for (int i = 0; i < classStats.nominalCounts.length; i++) {
      if (classStats.nominalCounts[i] == 0) {
        m_emptyClassList.add(i);
      }
    }

    super.buildClassifier(training);
  }

  /**
   * Return a probability distribution over the classes given a test instance in
   * Weka's {@code Instance} form
   *
   * @param test the test instance to predict
   * @return an array of predicted class probabilities
   * @throws Exception if a problem occurs
   */
  @Override
  public double[] distributionForInstance(Instance test) throws Exception {
    if (m_naiveBayesModel == null) {
      throw new Exception("No model built yet");
    }

    if (m_discretize != null) {
      m_discretize.input(test);
      test = m_discretize.output();
    }

    double[] preds = super.distributionForInstance(test);
    if (m_emptyClassList.size() > 0) {
      // adjust distribution to take into account empty classes
      double[] adjusted = new double[test.classAttribute().numValues()];
      int count = 0;
      for (int i = 0; i < adjusted.length; i++) {
        if (!m_emptyClassList.contains(i)) {
          adjusted[i] = preds[count];
          count++;
        }
      }
      preds = adjusted;
    }

    return preds;
  }

  /**
   * Return a probability distribution over the classes given a test instance in
   * {@code Vector} form
   *
   * @param test a {@code Vector} containing the test point
   * @return an array of predicted class probabilities
   * @throws DistributedWekaException
   */
  @Override
  public double[] distributionForVector(Vector test)
    throws DistributedWekaException {
    if (m_naiveBayesModel == null) {
      throw new DistributedWekaException("No model built yet");
    }

    Vector predictions = m_naiveBayesModel.predictProbabilities(test);
    return predictions.toArray();
  }

  /**
   * Build the classifier using an RDD of Instance objects. Assumes that any
   * numeric attributes are actually binary (i.e. only contain 1 or 0 values).
   * Any non-binary numeric attributes will need to have already been converted
   * to binary ones.
   *
   * @param wekaData
   * @param headerWithSummary
   * @param preprocessors streamable filters for preprocessing the data
   * @param strategy
   * @throws DistributedWekaException
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

    if (!m_classAtt.isNominal()) {
      throw new DistributedWekaException("Class attribute must be nominal!");
    }

    m_naiveBayesModel =
      NaiveBayes.train(mlLibData.rdd(), m_lambda, m_modelType.toString());
  }

  /**
   * Get a textual description of the model
   *
   * @return a textual description of the model
   */
  public String toString() {
    if (m_naiveBayesModel == null) {
      return "No model built yet";
    }

    return m_naiveBayesModel.toString();
  }

  @Override
  public void run(Object toRun, String[] options) {
    runClassifier((MLlibNaiveBayes) toRun, options);
  }

  public static void main(String[] args) {
    MLlibNaiveBayes nb = new MLlibNaiveBayes();

    nb.run(nb, args);
  }
}
