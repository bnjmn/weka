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
 *    MLlibRandomForest.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mllib;

import distributed.core.DistributedJobConfig;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.RandomForest;
import org.apache.spark.mllib.tree.model.RandomForestModel;
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

import java.util.List;

/**
 * Weka classifier wrapper for the MLlib Random Forest classifier
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class MLlibRandomForest extends MLlibClassifier {

  private static final long serialVersionUID = 7442869917230270564L;

  /** The number of trees to learn */
  protected int m_numTrees = 10;

  /**
   * The strategy to use for determining how many randomly selected attributes
   * to select from when splitting
   */
  protected String m_featureSubsetStrategy = "auto";

  /** Seed for the random number generator */
  protected int m_seed = 1;

  /**
   * Maximum number of nominal labels, above which an attribute will be treated
   * as numeric
   */
  protected int m_maxCategoricalValues =
    MLlibDatasetMaker.DEFAULT_MAX_NOMINAL_LABELS;

  /** Maximum depth to grow the trees */
  protected int m_maxDepth = 5;

  /** Maximum number of bins */
  protected int m_maxBins = 32;

  /** Whether to print all the individual trees to the output or not */
  protected boolean m_printIndividualTrees;

  /** Impurity metric to use */
  protected MLlibDecisionTree.Impurity m_impurity =
    MLlibDecisionTree.Impurity.Entropy;

  /** The learned model */
  protected RandomForestModel m_randomForestModel;

  /**
   * About information
   *
   * @return the about info for this scheme
   */
  public String globalInfo() {
    return "Spark MLlib random forest wrapper classifier.";
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
   * Set the number of trees to learn
   *
   * @param numTrees the number of trees to learn
   */
  @OptionMetadata(displayName = "Number of trees",
    description = "Number of trees to build",
    commandLineParamName = "num-trees",
    commandLineParamSynopsis = "-num-trees <integer>", displayOrder = 10)
  public void setNumTrees(int numTrees) {
    m_numTrees = numTrees;
  }

  /**
   * Get the number of trees to learn
   *
   * @return the number of trees to learn
   */
  public int getNumTrees() {
    return m_numTrees;
  }

  /**
   * Set the strategy used to choose the number attributes to select from when
   * splitting
   *
   * @param strategy the strategy to use ('auto' lets the algorithm decide)
   */
  @OptionMetadata(displayName = "Feature subset strategy",
    description = "Number of features to use as candidates for splitting at "
      + "each node. Specify as a fraction of the total number of features; "
      + "'auto' lets the algorithm decide.", commandLineParamName = "strategy",
    commandLineParamSynopsis = "-strategy <strategy>", displayOrder = 11)
  public void setFeatureSubsetStrategy(String strategy) {
    m_featureSubsetStrategy = strategy;
  }

  /**
   * Get the strategy used to choose the number attributes to select from when
   * splitting
   *
   * @return the strategy to use ('auto' lets the algorithm decide)
   */
  public String getFeatureSubsetStrategy() {
    return m_featureSubsetStrategy;
  }

  /**
   * Set the random seed
   *
   * @param seed get the random seed
   */
  @OptionMetadata(displayName = "Random seed",
    description = "Random seed to use", commandLineParamName = "seed",
    commandLineParamSynopsis = "-seed <integer>", displayOrder = 12)
  public void setSeed(int seed) {
    m_seed = seed;
  }

  /**
   * Get the random seed
   *
   * @return the random seed
   */
  public int getSeed() {
    return m_seed;
  }

  /**
   * Set the impurity metric to use
   *
   * @param impurity the impurity metric to use
   */
  @OptionMetadata(displayName = "Impurity",
    description = "Impurity measure to use (default = Entropy)",
    commandLineParamName = "impurity",
    commandLineParamSynopsis = "-impurity <impurity>", displayOrder = 13)
  public void setImpurity(MLlibDecisionTree.Impurity impurity) {
    m_impurity = impurity;
  }

  /**
   * Get the impurity metric to use
   *
   * @return the impurity metric to use
   */
  public MLlibDecisionTree.Impurity getImpurity() {
    return m_impurity;
  }

  /**
   * Set the maximum depth for each tree
   *
   * @param maxDepth the maximum depth grow trees to
   */
  @OptionMetadata(displayName = "Maximum tree depth",
    description = "Maximum depth of the tree (default = 5)",
    commandLineParamName = "depth", commandLineParamSynopsis = "-depth <int>",
    displayOrder = 14)
  public void setMaxDepth(int maxDepth) {
    m_maxDepth = maxDepth;
  }

  /**
   * Get the maximum depth for each tree
   *
   * @return the maximum depth grow trees to
   */
  public int getMaxDepth() {
    return m_maxDepth;
  }

  /**
   * Set the maximum number of bins to use when splitting numeric attributes
   *
   * @param maxBins the maximum number of bins to use
   */
  @OptionMetadata(displayName = "Maximum number of bins to use",
    description = "Maximum number of bins to use when splitting attributes "
      + "(default = 32)", commandLineParamName = "max-bins",
    commandLineParamSynopsis = "-max-bins <int>", displayOrder = 15)
  public void setMaxBins(int maxBins) {
    m_maxBins = maxBins;
  }

  /**
   * Get the maximum number of bins to use when splitting numeric attributes
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
  @OptionMetadata(displayName = "Max categories for nominal attributes",
    description = "Maximum number of labels for nominal attributes, above "
      + "which nominal attributes will be treated as numeric (default = "
      + MLlibDatasetMaker.DEFAULT_MAX_NOMINAL_LABELS + ")",
    commandLineParamName = "max-labels",
    commandLineParamSynopsis = "-max-labels <integer>", displayOrder = 16)
  public void setMaxNominalValues(int maxValues) {
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
   * Set whether to print all the trees out to the output or not
   *
   * @param print true to print out all the trees
   */
  @OptionMetadata(displayName = "Print the individual trees in the output",
    description = "Whether to output the textual description of all the base "
      + "tree models", commandLineParamName = "print",
    commandLineParamSynopsis = "-print", commandLineParamIsFlag = true,
    displayOrder = 17)
  public void setPrintTrees(boolean print) {
    m_printIndividualTrees = print;
  }

  /**
   * Get whether to print all the trees out to the output or not
   *
   * @return true to print out all the trees
   */
  public boolean getPrintTrees() {
    return m_printIndividualTrees;
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
      MLlibDecisionTree.Impurity toSet = MLlibDecisionTree.Impurity.Entropy;
      for (MLlibDecisionTree.Impurity i : MLlibDecisionTree.Impurity.values()) {
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
   * Learn the underlying random forest model.
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

    m_randomForestModel =
      m_classAtt.isNominal() ? RandomForest.trainClassifier(mlLibData,
        m_classAtt.numValues(), m_datasetMaker.getCategoricalFeaturesMap(),
        m_numTrees, m_featureSubsetStrategy, m_impurity.toString()
          .toLowerCase(), m_maxDepth, m_maxBins, m_seed) : RandomForest
        .trainRegressor(mlLibData, m_datasetMaker.getCategoricalFeaturesMap(),
          m_numTrees, m_featureSubsetStrategy, m_impurity.toString()
            .toLowerCase(), m_maxDepth, m_maxBins, m_seed);
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
    if (m_randomForestModel == null) {
      throw new DistributedWekaException("No model built yet!");
    }
    return m_randomForestModel.predict(test);
  }

  /**
   * Get a textual description of the model
   *
   * @return a textual description of the model
   */
  public String toString() {
    if (m_randomForestModel == null) {
      return "No model built yet!";
    }

    return m_printIndividualTrees ? m_randomForestModel.toDebugString()
      : m_randomForestModel.toString();
  }

  public void run(Object toRun, final String[] options) {
    runClassifier((MLlibRandomForest) toRun, options);
  }

  public static void main(String[] args) {
    MLlibRandomForest rf = new MLlibRandomForest();
    rf.run(rf, args);
  }
}
