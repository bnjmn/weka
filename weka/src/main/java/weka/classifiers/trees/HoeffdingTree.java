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
 *    HoeffdingTree.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.trees.ht.ActiveHNode;
import weka.classifiers.trees.ht.GiniSplitMetric;
import weka.classifiers.trees.ht.HNode;
import weka.classifiers.trees.ht.InactiveHNode;
import weka.classifiers.trees.ht.InfoGainSplitMetric;
import weka.classifiers.trees.ht.LeafNode;
import weka.classifiers.trees.ht.LearningNode;
import weka.classifiers.trees.ht.NBNode;
import weka.classifiers.trees.ht.NBNodeAdaptive;
import weka.classifiers.trees.ht.SplitCandidate;
import weka.classifiers.trees.ht.SplitMetric;
import weka.classifiers.trees.ht.SplitNode;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

/**
 <!-- globalinfo-start --> 
 * A Hoeffding tree (VFDT) is an incremental, anytime
 * decision tree induction algorithm that is capable of learning from massive
 * data streams, assuming that the distribution generating examples does not
 * change over time. Hoeffding trees exploit the fact that a small sample can
 * often be enough to choose an optimal splitting attribute. This idea is
 * supported mathematically by the Hoeffding bound, which quantifies the number
 * of observations (in our case, examples) needed to estimate some statistics
 * within a prescribed precision (in our case, the goodness of an attribute).<br/>
 * <br/>
 * A theoretically appealing feature of Hoeffding Trees not shared by
 * otherincremental decision tree learners is that it has sound guarantees of
 * performance. Using the Hoeffding bound one can show that its output is
 * asymptotically nearly identical to that of a non-incremental learner using
 * infinitely many examples. For more information see: <br/>
 * <br/>
 * Geoff Hulten, Laurie Spencer, Pedro Domingos: Mining time-changing data
 * streams. In: ACM SIGKDD Intl. Conf. on Knowledge Discovery and Data Mining,
 * 97-106, 2001.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start --> 
 * BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{Hulten2001,
 *    author = {Geoff Hulten and Laurie Spencer and Pedro Domingos},
 *    booktitle = {ACM SIGKDD Intl. Conf. on Knowledge Discovery and Data Mining},
 *    pages = {97-106},
 *    publisher = {ACM Press},
 *    title = {Mining time-changing data streams},
 *    year = {2001}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 * 
 <!-- options-start --> 
 * Valid options are:
 * <p/>
 * 
 * <pre>
 * -L
 *  The leaf prediction strategy to use. 0 = majority class, 1 = naive Bayes, 2 = naive Bayes adaptive.
 *  (default = 0)
 * </pre>
 * 
 * <pre>
 * -S
 *  The splitting criterion to use. 0 = Gini, 1 = Info gain
 *  (default = 0)
 * </pre>
 * 
 * <pre>
 * -E
 *  The allowable error in a split decision - values closer to zero will take longer to decide
 *  (default = 1e-7)
 * </pre>
 * 
 * <pre>
 * -H
 *  Threshold below which a split will be forced to break ties
 *  (default = 0.05)
 * </pre>
 * 
 * <pre>
 * -M
 *  Minimum fraction of weight required down at least two branches for info gain splitting
 *  (default = 0.01)
 * </pre>
 * 
 * <pre>
 * -G
 *  Grace period - the number of instances a leaf should observe between split attempts
 *  (default = 200)
 * </pre>
 * 
 * <pre>
 * -N
 *  The number of instances (weight) a leaf should observe before allowing naive Bayes to make predictions (NB or NB adaptive only)
 *  (default = 0)
 * </pre>
 * 
 * <pre>
 * -P
 *  Print leaf models when using naive Bayes at the leaves.
 * </pre>
 * 
 <!-- options-end -->
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class HoeffdingTree extends AbstractClassifier implements
    UpdateableClassifier, WeightedInstancesHandler, OptionHandler,
    RevisionHandler, TechnicalInformationHandler, Drawable, Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 7117521775722396251L;

  protected Instances m_header;
  protected HNode m_root;

  /** The number of instances a leaf should observe between split attempts */
  protected double m_gracePeriod = 200;

  /**
   * The allowable error in a split decision. Values closer to zero will take
   * longer to decide
   */
  protected double m_splitConfidence = 0.0000001;

  /** Threshold below which a split will be forced to break ties */
  protected double m_hoeffdingTieThreshold = 0.05;

  /**
   * The minimum fraction of weight required down at least two branches for info
   * gain splitting
   */
  protected double m_minFracWeightForTwoBranchesGain = 0.01;

  /** The splitting metric to use */
  protected int m_selectedSplitMetric = INFO_GAIN_SPLIT;
  protected SplitMetric m_splitMetric = new InfoGainSplitMetric(
      m_minFracWeightForTwoBranchesGain);

  /** The leaf prediction strategy to use */
  protected int m_leafStrategy = LEAF_NB_ADAPTIVE;

  /**
   * The number of instances (total weight) a leaf should observe before
   * allowing naive Bayes to make predictions
   */
  protected double m_nbThreshold = 0;

  protected int m_activeLeafCount;
  protected int m_inactiveLeafCount;
  protected int m_decisionNodeCount;

  public static final int GINI_SPLIT = 0;
  public static final int INFO_GAIN_SPLIT = 1;

  public static final Tag[] TAGS_SELECTION = {
      new Tag(GINI_SPLIT, "Gini split"),
      new Tag(INFO_GAIN_SPLIT, "Info gain split") };

  public static final int LEAF_MAJ_CLASS = 0;
  public static final int LEAF_NB = 1;
  public static final int LEAF_NB_ADAPTIVE = 2;

  public static final Tag[] TAGS_SELECTION2 = {
      new Tag(LEAF_MAJ_CLASS, "Majority class"),
      new Tag(LEAF_NB, "Naive Bayes"),
      new Tag(LEAF_NB_ADAPTIVE, "Naive Bayes adaptive") };

  /**
   * Print out leaf models in the case of naive Bayes or naive Bayes adaptive
   * leaves
   */
  protected boolean m_printLeafModels;

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {
    return "A Hoeffding tree (VFDT) is an incremental, anytime decision tree induction algorithm"
        + " that is capable of learning from massive data streams, assuming that the"
        + " distribution generating examples does not change over time. Hoeffding trees"
        + " exploit the fact that a small sample can often be enough to choose an optimal"
        + " splitting attribute. This idea is supported mathematically by the Hoeffding"
        + " bound, which quantifies the number of observations (in our case, examples)"
        + " needed to estimate some statistics within a prescribed precision (in our"
        + " case, the goodness of an attribute).\n\nA theoretically appealing feature "
        + " of Hoeffding Trees not shared by otherincremental decision tree learners is that "
        + " it has sound guarantees of performance. Using the Hoeffding bound one can show that "
        + " its output is asymptotically nearly identical to that of a non-incremental learner "
        + " using infinitely many examples. For more information see: \n\n"
        + getTechnicalInformation().toString();
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

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR,
        "Geoff Hulten and Laurie Spencer and Pedro Domingos");
    result.setValue(Field.TITLE, "Mining time-changing data streams");
    result.setValue(Field.BOOKTITLE,
        "ACM SIGKDD Intl. Conf. on Knowledge Discovery and Data Mining");
    result.setValue(Field.YEAR, "2001");
    result.setValue(Field.PAGES, "97-106");
    result.setValue(Field.PUBLISHER, "ACM Press");

    return result;
  }

  protected void reset() {
    m_root = null;

    m_activeLeafCount = 0;
    m_inactiveLeafCount = 0;
    m_decisionNodeCount = 0;
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
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> newVector = new Vector<Option>();

    newVector.add(new Option("\tThe leaf prediction strategy to use. 0 = "
        + "majority class, 1 = naive Bayes, 2 = naive Bayes adaptive.\n\t"
        + "(default = 2)", "L", 1, "-L"));

    newVector.add(new Option("\tThe splitting criterion to use. 0 = "
        + "Gini, 1 = Info gain\n\t" + "(default = 1)", "S", 1, "-S"));
    newVector.add(new Option("\tThe allowable error in a split decision "
        + "- values closer to zero will take longer to decide\n\t"
        + "(default = 1e-7)", "E", 1, "-E"));
    newVector.add(new Option(
        "\tThreshold below which a split will be forced to "
            + "break ties\n\t(default = 0.05)", "H", 1, "-H"));
    newVector.add(new Option(
        "\tMinimum fraction of weight required down at least two "
            + "branches for info gain splitting\n\t(default = 0.01)", "M", 1,
        "-M"));
    newVector.add(new Option("\tGrace period - the number of instances "
        + "a leaf should observe between split attempts\n\t"
        + "(default = 200)", "G", 1, "-G"));
    newVector
        .add(new Option("\tThe number of instances (weight) a leaf "
            + "should observe before allowing naive Bayes to make "
            + "predictions (NB or NB adaptive only)\n\t(default = 0)", "N", 1,
            "-N"));
    newVector.add(new Option("\tPrint leaf models when using naive Bayes "
        + "at the leaves.", "P", 0, "-P"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   <!-- options-start --> 
   * Valid options are:
   * <p/>
   * 
   * <pre>
   * -L
   *  The leaf prediction strategy to use. 0 = majority class, 1 = naive Bayes, 2 = naive Bayes adaptive.
   *  (default = 0)
   * </pre>
   * 
   * <pre>
   * -S
   *  The splitting criterion to use. 0 = Gini, 1 = Info gain
   *  (default = 0)
   * </pre>
   * 
   * <pre>
   * -E
   *  The allowable error in a split decision - values closer to zero will take longer to decide
   *  (default = 1e-7)
   * </pre>
   * 
   * <pre>
   * -H
   *  Threshold below which a split will be forced to break ties
   *  (default = 0.05)
   * </pre>
   * 
   * <pre>
   * -M
   *  Minimum fraction of weight required down at least two branches for info gain splitting
   *  (default = 0.01)
   * </pre>
   * 
   * <pre>
   * -G
   *  Grace period - the number of instances a leaf should observe between split attempts
   *  (default = 200)
   * </pre>
   * 
   * <pre>
   * -N
   *  The number of instances (weight) a leaf should observe before allowing naive Bayes to make predictions (NB or NB adaptive only)
   *  (default = 0)
   * </pre>
   * 
   * <pre>
   * -P
   *  Print leaf models when using naive Bayes at the leaves.
   * </pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    reset();

    super.setOptions(options);

    String opt = Utils.getOption('L', options);
    if (opt.length() > 0) {
      setLeafPredictionStrategy(new SelectedTag(Integer.parseInt(opt),
          TAGS_SELECTION2));
    }

    opt = Utils.getOption('S', options);
    if (opt.length() > 0) {
      setSplitCriterion(new SelectedTag(Integer.parseInt(opt), TAGS_SELECTION));
    }

    opt = Utils.getOption('E', options);
    if (opt.length() > 0) {
      setSplitConfidence(Double.parseDouble(opt));
    }

    opt = Utils.getOption('H', options);
    if (opt.length() > 0) {
      setHoeffdingTieThreshold(Double.parseDouble(opt));
    }

    opt = Utils.getOption('M', options);
    if (opt.length() > 0) {
      setMinimumFractionOfWeightInfoGain(Double.parseDouble(opt));
    }

    opt = Utils.getOption('G', options);
    if (opt.length() > 0) {
      setGracePeriod(Double.parseDouble(opt));
    }

    opt = Utils.getOption('N', options);
    if (opt.length() > 0) {
      setNaiveBayesPredictionThreshold(Double.parseDouble(opt));
    }

    m_printLeafModels = Utils.getFlag('P', options);
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    ArrayList<String> options = new ArrayList<String>();

    options.add("-L");
    options.add("" + getLeafPredictionStrategy().getSelectedTag().getID());

    options.add("-S");
    options.add("" + getSplitCriterion().getSelectedTag().getID());

    options.add("-E");
    options.add("" + getSplitConfidence());

    options.add("-H");
    options.add("" + getHoeffdingTieThreshold());

    options.add("-M");
    options.add("" + getMinimumFractionOfWeightInfoGain());

    options.add("-G");
    options.add("" + getGracePeriod());

    options.add("-N");
    options.add("" + getNaiveBayesPredictionThreshold());

    if (m_printLeafModels) {
      options.add("-P");
    }

    return options.toArray(new String[1]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String printLeafModelsTipText() {
    return "Print leaf models (naive bayes leaves only)";
  }

  public void setPrintLeafModels(boolean p) {
    m_printLeafModels = p;
  }

  public boolean getPrintLeafModels() {
    return m_printLeafModels;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String minimumFractionOfWeightInfoGainTipText() {
    return "Minimum fraction of weight required down at least two branches "
        + "for info gain splitting.";
  }

  /**
   * Set the minimum fraction of weight required down at least two branches for
   * info gain splitting
   * 
   * @param m the minimum fraction of weight
   */
  public void setMinimumFractionOfWeightInfoGain(double m) {
    m_minFracWeightForTwoBranchesGain = m;
  }

  /**
   * Get the minimum fraction of weight required down at least two branches for
   * info gain splitting
   * 
   * @return the minimum fraction of weight
   */
  public double getMinimumFractionOfWeightInfoGain() {
    return m_minFracWeightForTwoBranchesGain;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String gracePeriodTipText() {
    return "Number of instances (or total weight of instances) a leaf "
        + "should observe between split attempts.";
  }

  /**
   * Set the number of instances (or total weight of instances) a leaf should
   * observe between split attempts
   * 
   * @param grace the grace period
   */
  public void setGracePeriod(double grace) {
    m_gracePeriod = grace;
  }

  /**
   * Get the number of instances (or total weight of instances) a leaf should
   * observe between split attempts
   * 
   * @return the grace period
   */
  public double getGracePeriod() {
    return m_gracePeriod;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String hoeffdingTieThresholdTipText() {
    return "Theshold below which a split will be forced to break ties.";
  }

  /**
   * Set the threshold below which a split will be forced to break ties
   * 
   * @param ht the threshold
   */
  public void setHoeffdingTieThreshold(double ht) {
    m_hoeffdingTieThreshold = ht;
  }

  /**
   * Get the threshold below which a split will be forced to break ties
   * 
   * @return the threshold
   */
  public double getHoeffdingTieThreshold() {
    return m_hoeffdingTieThreshold;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String splitConfidenceTipText() {
    return "The allowable error in a split decision. Values closer to zero "
        + "will take longer to decide.";
  }

  /**
   * Set the allowable error in a split decision. Values closer to zero will
   * take longer to decide.
   * 
   * @param sc the split confidence
   */
  public void setSplitConfidence(double sc) {
    m_splitConfidence = sc;
  }

  /**
   * Get the allowable error in a split decision. Values closer to zero will
   * take longer to decide.
   * 
   * @return the split confidence
   */
  public double getSplitConfidence() {
    return m_splitConfidence;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String splitCriterionTipText() {
    return "The splitting criterion to use";
  }

  /**
   * Set the split criterion to use (either Gini or info gain).
   * 
   * @param crit the criterion to use
   */
  public void setSplitCriterion(SelectedTag crit) {
    if (crit.getTags() == TAGS_SELECTION) {
      m_selectedSplitMetric = crit.getSelectedTag().getID();
    }
  }

  /**
   * Get the split criterion to use (either Gini or info gain).
   * 
   * @return the criterion to use
   */
  public SelectedTag getSplitCriterion() {
    return new SelectedTag(m_selectedSplitMetric, TAGS_SELECTION);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String leafPredictionStrategyTipText() {
    return "The leaf prediction strategy to use";
  }

  /**
   * Set the leaf prediction strategy to use (majority class, naive Bayes or
   * naive Bayes adaptive)
   * 
   * @param strat the strategy to use
   */
  public void setLeafPredictionStrategy(SelectedTag strat) {
    if (strat.getTags() == TAGS_SELECTION2) {
      m_leafStrategy = strat.getSelectedTag().getID();
    }
  }

  /**
   * Get the leaf prediction strategy to use (majority class, naive Bayes or
   * naive Bayes adaptive)
   * 
   * @return the strategy to use
   */
  public SelectedTag getLeafPredictionStrategy() {
    return new SelectedTag(m_leafStrategy, TAGS_SELECTION2);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String naiveBayesPredictionThresholdTipText() {
    return "The number of instances (weight) a leaf should observe "
        + "before allowing naive Bayes (adaptive) to make predictions";
  }

  /**
   * Set the number of instances (weight) a leaf should observe before allowing
   * naive Bayes to make predictions
   * 
   * @param n the number/weight of instances
   */
  public void setNaiveBayesPredictionThreshold(double n) {
    m_nbThreshold = n;
  }

  /**
   * Get the number of instances (weight) a leaf should observe before allowing
   * naive Bayes to make predictions
   * 
   * @return the number/weight of instances
   */
  public double getNaiveBayesPredictionThreshold() {
    return m_nbThreshold;
  }

  protected static double computeHoeffdingBound(double max, double confidence,
      double weight) {
    return Math.sqrt(((max * max) * Math.log(1.0 / confidence))
        / (2.0 * weight));
  }

  /**
   * Builds the classifier.
   * 
   * @param data the data to train with
   * @throws Exception if classifier can't be built successfully
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {
    reset();

    m_header = new Instances(data, 0);
    if (m_selectedSplitMetric == GINI_SPLIT) {
      m_splitMetric = new GiniSplitMetric();
    } else {
      m_splitMetric = new InfoGainSplitMetric(m_minFracWeightForTwoBranchesGain);
    }

    data = new Instances(data);
    data.deleteWithMissingClass();
    for (int i = 0; i < data.numInstances(); i++) {
      updateClassifier(data.instance(i));
    }

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

  }

  /**
   * Updates the classifier with the given instance.
   * 
   * @param instance the new training instance to include in the model
   * @exception Exception if the instance could not be incorporated in the
   *              model.
   */
  @Override
  public void updateClassifier(Instance inst) throws Exception {

    if (inst.classIsMissing()) {
      return;
    }

    if (m_root == null) {
      m_root = newLearningNode();
    }

    LeafNode l = m_root.leafForInstance(inst, null, null);
    HNode actualNode = l.m_theNode;
    if (actualNode == null) {
      actualNode = new ActiveHNode();
      l.m_parentNode.setChild(l.m_parentBranch, actualNode);
    }

    if (actualNode instanceof LearningNode) {
      actualNode.updateNode(inst);

      if (/* m_growthAllowed && */actualNode instanceof ActiveHNode) {
        double totalWeight = actualNode.totalWeight();
        if (totalWeight
            - ((ActiveHNode) actualNode).m_weightSeenAtLastSplitEval > m_gracePeriod) {

          // try a split
          trySplit((ActiveHNode) actualNode, l.m_parentNode, l.m_parentBranch);

          ((ActiveHNode) actualNode).m_weightSeenAtLastSplitEval = totalWeight;
        }
      }
    }
  }

  /**
   * Returns class probabilities for an instance.
   * 
   * @param instance the instance to compute the distribution for
   * @return the class probabilities
   * @throws Exception if distribution can't be computed successfully
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {

    Attribute classAtt = inst.classAttribute();
    double[] pred = new double[classAtt.numValues()];

    if (m_root != null) {
      LeafNode l = m_root.leafForInstance(inst, null, null);
      HNode actualNode = l.m_theNode;

      if (actualNode == null) {
        actualNode = l.m_parentNode;
      }

      pred = actualNode.getDistribution(inst, classAtt);

    } else {
      // all class values equally likely
      for (int i = 0; i < classAtt.numValues(); i++) {
        pred[i] = 1;
      }
      Utils.normalize(pred);
    }

    // Utils.normalize(pred);
    return pred;
  }

  /**
   * Deactivate (prevent growth) from the supplied node
   * 
   * @param toDeactivate the node to deactivate
   * @param parent the node's parent
   * @param parentBranch the branch leading to the node
   */
  protected void deactivateNode(ActiveHNode toDeactivate, SplitNode parent,
      String parentBranch) {
    HNode leaf = new InactiveHNode(toDeactivate.m_classDistribution);

    if (parent == null) {
      m_root = leaf;
    } else {
      parent.setChild(parentBranch, leaf);
    }
    m_activeLeafCount--;
    m_inactiveLeafCount++;
  }

  /**
   * Activate (allow growth) the supplied node
   * 
   * @param toActivate the node to activate
   * @param parent the node's parent
   * @param parentBranch the branch leading to the node
   */
  protected void activateNode(InactiveHNode toActivate, SplitNode parent,
      String parentBranch) {
    HNode leaf = new ActiveHNode();
    leaf.m_classDistribution = toActivate.m_classDistribution;

    if (parent == null) {
      m_root = leaf;
    } else {
      parent.setChild(parentBranch, leaf);
    }

    m_activeLeafCount++;
    m_inactiveLeafCount--;
  }

  /**
   * Try a split from the supplied node
   * 
   * @param node the node to split
   * @param parent the parent of the node
   * @param parentBranch the branch leading to the node
   * @throws Exception if a problem occurs
   */
  protected void trySplit(ActiveHNode node, SplitNode parent,
      String parentBranch) throws Exception {

    // non-pure?
    if (node.numEntriesInClassDistribution() > 1) {
      List<SplitCandidate> bestSplits = node.getPossibleSplits(m_splitMetric);
      Collections.sort(bestSplits);

      boolean doSplit = false;
      if (bestSplits.size() < 2) {
        doSplit = bestSplits.size() > 0;
      } else {
        // compute the Hoeffding bound
        double metricMax = m_splitMetric.getMetricRange(node.m_classDistribution);
        double hoeffdingBound = computeHoeffdingBound(metricMax,
            m_splitConfidence, node.totalWeight());

        SplitCandidate best = bestSplits.get(bestSplits.size() - 1);
        SplitCandidate secondBest = bestSplits.get(bestSplits.size() - 2);

        if (best.m_splitMerit - secondBest.m_splitMerit > hoeffdingBound
            || hoeffdingBound < m_hoeffdingTieThreshold) {
          doSplit = true;
        }

        // TODO - remove poor attributes stuff?
      }

      if (doSplit) {
        SplitCandidate best = bestSplits.get(bestSplits.size() - 1);

        if (best.m_splitTest == null) {
          // preprune
          deactivateNode(node, parent, parentBranch);
        } else {
          SplitNode newSplit = new SplitNode(node.m_classDistribution,
              best.m_splitTest);

          for (int i = 0; i < best.numSplits(); i++) {
            ActiveHNode newChild = newLearningNode();
            newChild.m_classDistribution = best.m_postSplitClassDistributions
                .get(i);
            newChild.m_weightSeenAtLastSplitEval = newChild.totalWeight();
            String branchName = "";
            if (m_header.attribute(best.m_splitTest.splitAttributes().get(0))
                .isNumeric()) {
              branchName = i == 0 ? "left" : "right";
            } else {
              Attribute splitAtt = m_header.attribute(best.m_splitTest
                  .splitAttributes().get(0));
              branchName = splitAtt.value(i);
            }
            newSplit.setChild(branchName, newChild);
          }

          m_activeLeafCount--;
          m_decisionNodeCount++;
          m_activeLeafCount += best.numSplits();

          if (parent == null) {
            m_root = newSplit;
          } else {
            parent.setChild(parentBranch, newSplit);
          }
        }
      }
    }
  }

  /**
   * Create a new learning node (either majority class, naive Bayes or naive
   * Bayes adaptive)
   * 
   * @return a new learning node
   * @throws Exception if a problem occurs
   */
  protected ActiveHNode newLearningNode() throws Exception {
    ActiveHNode newChild;

    if (m_leafStrategy == LEAF_MAJ_CLASS) {
      newChild = new ActiveHNode();
    } else if (m_leafStrategy == LEAF_NB) {
      newChild = new NBNode(m_header, m_nbThreshold);
    } else {
      newChild = new NBNodeAdaptive(m_header, m_nbThreshold);
    }

    return newChild;
  }

  /**
   * Return a textual description of the mode
   * 
   * @return a String describing the model
   */
  @Override
  public String toString() {
    if (m_root == null) {
      return "No model built yet!";
    }

    return m_root.toString(m_printLeafModels);
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

  public static void main(String[] args) {
    runClassifier(new HoeffdingTree(), args);
  }

  @Override
  public int graphType() {
    return Drawable.TREE;
  }

  @Override
  public String graph() throws Exception {
    if (m_root == null) {
      throw new Exception("No model built yet!");
    }
    m_root.installNodeNums(0);
    StringBuffer buff = new StringBuffer();
    buff.append("digraph HoeffdingTree {\n");
    m_root.graphTree(buff);
    buff.append("}\n");

    return buff.toString();
  }
}
