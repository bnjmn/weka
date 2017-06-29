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
 *    MLlibDecisionTree.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mllib;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.DecisionTree;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;

import distributed.core.DistributedJobConfig;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.distributed.spark.mllib.util.MLlibDatasetMaker;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;
import weka.filters.StreamableFilter;
import weka.knowledgeflow.SingleThreadedExecution;

import java.util.List;

/**
 * Weka classifier wrapper for the MLlib DecisionTree classifier
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@SingleThreadedExecution
public class MLlibDecisionTree extends MLlibClassifier {
  private static final long serialVersionUID = 5965047909851348129L;

  /**
   * Maximum number of nominal labels, above which an attribute will be treated
   * as numeric
   */
  protected int m_maxCategoricalValues =
    MLlibDatasetMaker.DEFAULT_MAX_NOMINAL_LABELS;

  /** Default maximum depth of the tree */
  protected int m_maxDepth = 5;

  /** Default maximum number of bins */
  protected int m_maxBins = 32;

  /** Default impurity metric */
  protected Impurity m_impurity = Impurity.Entropy;

  /** Holds the learned decision tree */
  protected DecisionTreeModel m_treeModel;

  /**
   * About information
   *
   * @return the about info for this scheme
   */
  public String globalInfo() {
    return "Spark MLlib decision tree wrapper classifier.";
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
    result.enable(Capabilities.Capability.NUMERIC_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Set the impurity metric to use
   *
   * @param impurity the impurity metric to use
   */
  @OptionMetadata(displayName = "Impurity",
    description = "Impurity measure to use (default = Entropy)",
    commandLineParamName = "impurity",
    commandLineParamSynopsis = "-impurity <impurity>", displayOrder = 10)
  public void setImpurity(Impurity impurity) {
    m_impurity = impurity;
  }

  /**
   * Get the impurity metric to use
   *
   * @return the impurity metric to use
   */
  public Impurity getImpurity() {
    return m_impurity;
  }

  /**
   * Set the maximum tree depth
   *
   * @param maxDepth the maximum depth
   */
  @OptionMetadata(displayName = "Maximum tree depth",
    description = "Maximum depth of the tree (default = 5)",
    commandLineParamName = "depth", commandLineParamSynopsis = "-depth <int>",
    displayOrder = 11)
  public void setMaxDepth(int maxDepth) {
    m_maxDepth = maxDepth;
  }

  /**
   * Get the maximum tree depth
   *
   * @return the maximum depth
   */
  public int getMaxDepth() {
    return m_maxDepth;
  }

  /**
   * Set the maximum number of bins to use when splitting attributes
   *
   * @param maxBins the maximum number of bins to use
   */
  @OptionMetadata(displayName = "Maximum number of bins to use",
    description = "Maximum number of bins to use when splitting attributes "
      + "(default = 32)", commandLineParamName = "max-bins",
    commandLineParamSynopsis = "-max-bins <int>", displayOrder = 12)
  public void setMaxBins(int maxBins) {
    m_maxBins = maxBins;
  }

  /**
   * Get the maximum number of bins to use when splitting attributes
   *
   * @return the maximum number of bins to use
   */
  public int getMaxBins() {
    return m_maxBins;
  }

  /**
   * Set the maximum number of categories for a nominal attribute, above which
   * it will be treated as numeric
   *
   * @param maxValues the maximum number of categories that a nominal attribute
   *          can have
   */
  @OptionMetadata(
    displayName = "Max categories for nominal attributes",
    description = "Maximum number of labels for nominal attributes, above which"
      + " nominal attributes will be treated as numeric (default = "
      + MLlibDatasetMaker.DEFAULT_MAX_NOMINAL_LABELS + ")",
    commandLineParamName = "max-labels",
    commandLineParamSynopsis = "-max-labels <integer>", displayOrder = 13)
  public
    void setMaxNominalValues(int maxValues) {
    m_maxCategoricalValues = maxValues;
  }

  /**
   * Get the maximum number of categories for a nominal attribute, above which
   * it will be treated as numeric
   *
   * @return the maximum number of categories that a nominal attribute can have
   */
  public int getMaxNominalValues() {
    return m_maxCategoricalValues;
  }

  /**
   * Set options
   *
   * @param opts an array of option settings
   * @throws Exception if a problem occurs
   */
  @Override
  public void setOptions(String[] opts) throws Exception {
    String impurity = Utils.getOption("impurity", opts);
    if (!DistributedJobConfig.isEmpty(impurity)) {
      Impurity toSet = Impurity.Entropy;
      for (Impurity i : Impurity.values()) {
        if (i.toString().equalsIgnoreCase(impurity)) {
          toSet = i;
          break;
        }
      }
      setImpurity(toSet);
    }

    super.setOptions(opts);
  }

  /**
   * Learn the underlying decision tree model.
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
        preprocessors, getMaxNominalValues(), true, strategy);

    m_classAtt = headerWithSummary.attribute(m_datasetMaker.getClassIndex());

    if (m_classAtt.isNumeric()
      && m_impurity != MLlibDecisionTree.Impurity.Variance) {
      throw new DistributedWekaException(
        "Impurity must be set to Variance for " + "regression problems");
    } else if (m_classAtt.isNominal()
      && m_impurity == MLlibDecisionTree.Impurity.Variance) {
      throw new DistributedWekaException(
        "Impurity can't be set to Variance for classification problems");
    }

    // TODO switch to using
    // org.apache.spark.mllib.tree.configuration.Strategy.defaultStrategy()
    // for configuring tree building options - supports more options than the
    // static trainClassifier() methods

    m_treeModel =
      m_classAtt.isNominal() ? DecisionTree.trainClassifier(mlLibData,
        m_classAtt.numValues(), m_datasetMaker.getCategoricalFeaturesMap(),
        m_impurity.toString().toLowerCase(), m_maxDepth, m_maxBins)
        : DecisionTree.trainRegressor(mlLibData, m_datasetMaker
          .getCategoricalFeaturesMap(), m_impurity.toString().toLowerCase(),
          m_maxDepth, m_maxBins);
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
    if (m_treeModel == null) {
      throw new DistributedWekaException("No model built yet!");
    }

    return m_treeModel.predict(test);
  }

  /**
   * Get a textual description of the classifier
   *
   * @return a textual description of the classifier
   */
  public String toString() {
    if (m_treeModel == null) {
      return "No model built yet!";
    }

    return m_treeModel.toDebugString();
  }

  /**
   * Main method
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    MLlibDecisionTree dt = new MLlibDecisionTree();
    dt.run(dt, args);
  }

  @Override
  public void run(Object toRun, final String[] options) {
    runClassifier((MLlibDecisionTree) toRun, options);
  }

  /**
   * Enum for impurity settings
   */
  public enum Impurity {
    Entropy, Gini, Variance;
  }
}
