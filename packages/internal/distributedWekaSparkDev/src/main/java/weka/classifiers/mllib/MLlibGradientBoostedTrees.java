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
 *    MLlibGradientBoostedTrees.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mllib;

import distributed.core.DistributedJobConfig;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.GradientBoostedTrees;
import org.apache.spark.mllib.tree.configuration.BoostingStrategy;
import org.apache.spark.mllib.tree.model.GradientBoostedTreesModel;
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
import java.util.Map;

/**
 * Weka wrapper classifier for MLlib gradient boosted trees
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@SingleThreadedExecution
public class MLlibGradientBoostedTrees extends MLlibClassifier {
  private static final long serialVersionUID = -531311160289328886L;

  /** The learned model */
  protected GradientBoostedTreesModel m_gradientBoostedTreeModel;

  /** Default number of iterations to perform */
  protected int m_numIterations = 10;

  /** Default max depth for trees */
  protected int m_maxDepth = 5;

  /** Default max bins when splitting */
  protected int m_maxBins = 32;

  /** Default max categorical values */
  protected int m_maxCategoricalValues =
    MLlibDatasetMaker.DEFAULT_MAX_NOMINAL_LABELS;

  /** Default impurity metric to use */
  protected MLlibDecisionTree.Impurity m_impurity =
    MLlibDecisionTree.Impurity.Entropy;

  /** True to print out all the base trees */
  protected boolean m_printAllModels;

  /**
   * About info
   *
   * @return the about info
   */
  public String globalInfo() {
    return "Spark MLlib gradient boosted trees wrapper classifier.";
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
    result.enable(Capabilities.Capability.NUMERIC_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Set the number of iterations to perform
   *
   * @param iter the number of iterations to perform
   */
  @OptionMetadata(displayName = "Number of iterations",
    description = "Number of" + "iterations to perform",
    commandLineParamName = "I", commandLineParamSynopsis = "-I <integer>",
    displayOrder = 12)
  public void setNumIterations(int iter) {
    m_numIterations = iter;
  }

  /**
   * Get the number of iterations to perform
   *
   * @return the number of iterations to perform
   */
  public int getNumIterations() {
    return m_numIterations;
  }

  /**
   * Set the impurity metric to use
   *
   * @param impurity the impurity metric
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
   * @return the impurity metric
   */
  public MLlibDecisionTree.Impurity getImpurity() {
    return m_impurity;
  }

  /**
   * Set the maximum tree depth
   *
   * @param maxDepth the maximum tree depth
   */
  @OptionMetadata(displayName = "Maximum tree depth",
    description = "Maximum depth of the tree (default = 5)",
    commandLineParamName = "depth", commandLineParamSynopsis = "-depth <int>",
    displayOrder = 14)
  public void setMaxDepth(int maxDepth) {
    m_maxDepth = maxDepth;
  }

  /**
   * Get the maximum tree depth
   *
   * @return the maximum tree depth
   */
  public int getMaxDepth() {
    return m_maxDepth;
  }

  /**
   * Set the maximum number of bins to use during splitting
   *
   * @param maxBins the maximum number of bins
   */
  @OptionMetadata(displayName = "Maximum number of bins to use",
    description = "Maximum number of bins to use when splitting attributes "
      + "(default = 32)", commandLineParamName = "max-bins",
    commandLineParamSynopsis = "-max-bins <int>", displayOrder = 15)
  public void setMaxBins(int maxBins) {
    m_maxBins = maxBins;
  }

  /**
   * Get the maximum number of bins to use during splitting
   *
   * @return the maximum number of bins
   */
  public int getMaxBins() {
    return m_maxBins;
  }

  /**
   * Set the maximum number of categorical values a nominal attribute can have,
   * above which it gets treated as numeric
   *
   * @param maxValues the maximum number of categorical values
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
   * Get the maximum number of categorical values a nominal attribute can have,
   * above which it gets treated as numeric
   *
   * @return the maximum number of categorical values
   */
  public int getMaxNominalValues() {
    return m_maxCategoricalValues;
  }

  /**
   * Set whether to print all the base trees or not
   *
   * @param print true to print all the trees
   */
  @OptionMetadata(displayName = "Print the individual trees in the output",
    description = "Whether to output the textual description of all the base "
      + "tree models", commandLineParamName = "print",
    commandLineParamSynopsis = "-print", commandLineParamIsFlag = true,
    displayOrder = 17)
  public void setPrintTrees(boolean print) {
    m_printAllModels = print;
  }

  /**
   * Get whether to print all the base trees or not
   *
   * @return true to print all the trees
   */
  public boolean getPrintTrees() {
    return m_printAllModels;
  }

  /**
   * Set options
   *
   * @param opts an array of options to set
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
   * Learn the underlying MLlib model.
   *
   * @param wekaData an RDD of {@code Instance} objects
   * @param headerWithSummary header (including summary attributes) for the data
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

    Map<Integer, Integer> categoricalMap =
      m_datasetMaker.getCategoricalFeaturesMap();
    m_classAtt = headerWithSummary.attribute(m_datasetMaker.getClassIndex());

    if (m_classAtt.isNumeric()
      && m_impurity != MLlibDecisionTree.Impurity.Variance) {
      throw new DistributedWekaException(
        "Impurity must be set to Variance for " + "regression problems");
    } else if (m_classAtt.isNominal()) {
      if (m_impurity == MLlibDecisionTree.Impurity.Variance) {
        throw new DistributedWekaException(
          "Impurity can't be set to Variance for classification problems");
      }

      if (m_classAtt.numValues() != 2) {
        throw new DistributedWekaException("Class attribute must be binary");
      }
    }

    BoostingStrategy boostingStrategy =
      BoostingStrategy.defaultParams(m_classAtt.isNominal() ? "Classification"
        : "Regression");

    boostingStrategy.setNumIterations(m_numIterations);
    boostingStrategy.getTreeStrategy().setMaxDepth(m_maxDepth);
    boostingStrategy.getTreeStrategy().setCategoricalFeaturesInfo(
      m_datasetMaker.getCategoricalFeaturesMap());

    if (m_classAtt.isNominal()) {
      boostingStrategy.getTreeStrategy().setNumClasses(m_classAtt.numValues());
      if (m_impurity == MLlibDecisionTree.Impurity.Entropy) {
        boostingStrategy.getTreeStrategy().setImpurity(
          org.apache.spark.mllib.tree.impurity.Entropy.instance());
      } else {
        boostingStrategy.getTreeStrategy().setImpurity(
          org.apache.spark.mllib.tree.impurity.Gini.instance());
      }
    }

    m_gradientBoostedTreeModel =
      GradientBoostedTrees.train(mlLibData, boostingStrategy);
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
    if (m_gradientBoostedTreeModel == null) {
      throw new DistributedWekaException("No model built yet!");
    }

    return m_gradientBoostedTreeModel.predict(test);
  }

  /**
   * Get a textual description of the model
   *
   * @return a textual description of the model
   */
  public String toString() {
    if (m_gradientBoostedTreeModel == null) {
      return "No model built yet!";
    }

    return m_printAllModels ? m_gradientBoostedTreeModel.toDebugString()
      : m_gradientBoostedTreeModel.toString();
  }

  public static void main(String[] args) {
    MLlibGradientBoostedTrees gbt = new MLlibGradientBoostedTrees();
    gbt.run(gbt, args);
  }

  @Override
  public void run(Object toRun, final String[] options) {
    runClassifier((MLlibGradientBoostedTrees) toRun, options);
  }
}
