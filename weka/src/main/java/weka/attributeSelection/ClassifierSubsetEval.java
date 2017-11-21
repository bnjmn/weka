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
 *    ClassifierSubsetEval.java
 *    Copyright (C) 2000 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.attributeSelection;

import java.io.File;
import java.util.BitSet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.AbstractEvaluationMetric;
import weka.classifiers.evaluation.InformationRetrievalEvaluationMetric;
import weka.classifiers.rules.ZeroR;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 <!-- globalinfo-start -->
 * Classifier subset evaluator:<br>
 * <br>
 * Evaluates attribute subsets on training data or a seperate hold out testing set. Uses a classifier to estimate the 'merit' of a set of attributes.
 * <br><br>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -B &lt;classifier&gt;
 *  class name of the classifier to use for accuracy estimation.
 *  Place any classifier options LAST on the command line
 *  following a "--". eg.:
 *   -B weka.classifiers.bayes.NaiveBayes ... -- -K
 *  (default: weka.classifiers.rules.ZeroR)</pre>
 * 
 * <pre> -T
 *  Use the training data to estimate accuracy.</pre>
 * 
 * <pre> -H &lt;filename&gt;
 *  Name of the hold out/test set to 
 *  estimate accuracy on.</pre>
 * 
 * <pre> -percentage-split
 *  Perform a percentage split on the training data.
 *  Use in conjunction with -T.</pre>
 * 
 * <pre> -P
 *  Split percentage to use (default = 90).</pre>
 * 
 * <pre> -S
 *  Random seed for percentage split (default = 1).</pre>
 * 
 * <pre> -E &lt;DEFAULT|ACC|RMSE|MAE|F-MEAS|AUC|AUPRC|CORR-COEFF&gt;
 *  Performance evaluation measure to use for selecting attributes.
 *  (Default = default: accuracy for discrete class and rmse for numeric class)</pre>
 * 
 * <pre> -IRclass &lt;label | index&gt;
 *  Optional class value (label or 1-based index) to use in conjunction with
 *  IR statistics (f-meas, auc or auprc). Omitting this option will use
 *  the class-weighted average.</pre>
 * 
 * <pre> 
 * Options specific to scheme weka.classifiers.rules.ZeroR:
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
 * <pre> -batch-size
 *  The desired batch size for batch prediction  (default 100).</pre>
 * 
 <!-- options-end -->
 * 
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 10332 $
 */
public class ClassifierSubsetEval extends HoldOutSubsetEvaluator implements
  OptionHandler, ErrorBasedMeritEvaluator {

  /** for serialization */
  static final long serialVersionUID = 7532217899385278710L;

  /** training instances */
  private Instances m_trainingInstances;

  /** class index */
  private int m_classIndex;

  /** number of attributes in the training data */
  private int m_numAttribs;

  /** number of training instances */
  // private int m_numInstances; NOT USED

  /** holds the template classifier to use for error estimates */
  private Classifier m_ClassifierTemplate = new ZeroR();

  /**
   * Holds the classifier used when evaluating single hold-out instances - this
   * is used by RaceSearch and the trained classifier may need to persist
   * between calls to that particular method.
   */
  private Classifier m_Classifier = new ZeroR();

  /** the file that containts hold out/test instances */
  private File m_holdOutFile = new File("Click to set hold out or "
    + "test instances");

  /** the instances to test on */
  private Instances m_holdOutInstances;

  /** evaluate on training data rather than separate hold out/test set */
  private boolean m_useTraining = true;

  /** Whether to hold out a percentage of the training data */
  protected boolean m_usePercentageSplit;

  /** Seed for randomizing prior to splitting training data */
  protected int m_seed = 1;

  /** The split to use if doing a percentage split */
  protected String m_splitPercent = "90";

  public static final int EVAL_DEFAULT = 1;
  public static final int EVAL_ACCURACY = 2;
  public static final int EVAL_RMSE = 3;
  public static final int EVAL_MAE = 4;
  public static final int EVAL_FMEASURE = 5;
  public static final int EVAL_AUC = 6;
  public static final int EVAL_AUPRC = 7;
  public static final int EVAL_CORRELATION = 8;
  public static final int EVAL_PLUGIN = 9;

  protected static List<AbstractEvaluationMetric> PLUGIN_METRICS =
    AbstractEvaluationMetric.getPluginMetrics();

  /** Holds all tags for metrics */
  public static final Tag[] TAGS_EVALUATION;

  static {
    int totalPluginCount = 0;
    if (PLUGIN_METRICS != null) {
      for (AbstractEvaluationMetric m : PLUGIN_METRICS) {
        totalPluginCount += m.getStatisticNames().size();
      }
    }

    TAGS_EVALUATION = new Tag[8 + totalPluginCount];
    TAGS_EVALUATION[0] =
      new Tag(EVAL_DEFAULT, "default",
        "Default: accuracy (discrete class); RMSE (numeric class)");
    TAGS_EVALUATION[1] =
      new Tag(EVAL_ACCURACY, "acc", "Accuracy (discrete class only)");
    TAGS_EVALUATION[2] =
      new Tag(EVAL_RMSE, "rmse",
        "RMSE (of the class probabilities for discrete class)");
    TAGS_EVALUATION[3] =
      new Tag(EVAL_MAE, "mae",
        "MAE (of the class probabilities for discrete class)");
    TAGS_EVALUATION[4] =
      new Tag(EVAL_FMEASURE, "f-meas", "F-measure (discrete class only)");
    TAGS_EVALUATION[5] =
      new Tag(EVAL_AUC, "auc",
        "AUC (area under the ROC curve - discrete class only)");
    TAGS_EVALUATION[6] =
      new Tag(EVAL_AUPRC, "auprc",
        "AUPRC (area under the precision-recall curve - discrete class only)");
    TAGS_EVALUATION[7] =
      new Tag(EVAL_CORRELATION, "corr-coeff",
        "Correlation coefficient - numeric class only");

    if (PLUGIN_METRICS != null) {
      int index = 8;
      for (AbstractEvaluationMetric m : PLUGIN_METRICS) {
        for (String stat : m.getStatisticNames()) {
          TAGS_EVALUATION[index++] =
            new WrapperSubsetEval.PluginTag(index + 1, m, stat);
        }
      }
    }
  }

  /** The evaluation measure to use */
  protected Tag m_evaluationMeasure = TAGS_EVALUATION[0];

  /**
   * If >= 0, and an IR metric is being used, then evaluate with respect to this
   * class value (0-based index)
   */
  protected int m_IRClassVal = -1;

  /** User supplied option for IR class value (either name or 1-based index) */
  protected String m_IRClassValS = "";

  /**
   * Returns a string describing this attribute evaluator
   * 
   * @return a description of the evaluator suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Classifier subset evaluator:\n\nEvaluates attribute subsets on training data or a seperate "
      + "hold out testing set. Uses a classifier to estimate the 'merit' of a set of attributes.";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   **/
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> newVector = new Vector<Option>(8);

    newVector.addElement(new Option(
      "\tclass name of the classifier to use for accuracy estimation.\n"
        + "\tPlace any classifier options LAST on the command line\n"
        + "\tfollowing a \"--\". eg.:\n"
        + "\t\t-B weka.classifiers.bayes.NaiveBayes ... -- -K\n"
        + "\t(default: weka.classifiers.rules.ZeroR)", "B", 1,
      "-B <classifier>"));

    newVector.addElement(new Option("\tUse the training data to estimate"
      + " accuracy.", "T", 0, "-T"));

    newVector.addElement(new Option("\tName of the hold out/test set to "
      + "\n\testimate accuracy on.", "H", 1, "-H <filename>"));

    newVector.addElement(new Option("\tPerform a percentage split on the "
      + "training data.\n\tUse in conjunction with -T.", "percentage-split", 0,
      "-percentage-split"));

    newVector.addElement(new Option(
      "\tSplit percentage to use (default = 90).", "P", 1, "-P"));
    newVector.addElement(new Option(
      "\tRandom seed for percentage split (default = 1).", "S", 1, "-S"));

    newVector.addElement(new Option(
      "\tPerformance evaluation measure to use for selecting attributes.\n"
        + "\t(Default = default: accuracy for discrete class and rmse for "
        + "numeric class)", "E", 1, "-E " + Tag.toOptionList(TAGS_EVALUATION)));

    newVector
      .addElement(new Option(
        "\tOptional class value (label or 1-based index) to use in conjunction with\n"
          + "\tIR statistics (f-meas, auc or auprc). Omitting this option will use\n"
          + "\tthe class-weighted average.", "IRclass", 1,
        "-IRclass <label | index>"));

    if ((m_ClassifierTemplate != null)
      && (m_ClassifierTemplate instanceof OptionHandler)) {

      newVector.addElement(new Option("", "", 0, "\nOptions specific to "
        + "scheme " + m_ClassifierTemplate.getClass().getName() + ":"));

      newVector.addAll(Collections.list(((OptionHandler) m_ClassifierTemplate)
        .listOptions()));
    }

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p>
   * 
   * <pre> -B &lt;classifier&gt;
   *  class name of the classifier to use for accuracy estimation.
   *  Place any classifier options LAST on the command line
   *  following a "--". eg.:
   *   -B weka.classifiers.bayes.NaiveBayes ... -- -K
   *  (default: weka.classifiers.rules.ZeroR)</pre>
   * 
   * <pre> -T
   *  Use the training data to estimate accuracy.</pre>
   * 
   * <pre> -H &lt;filename&gt;
   *  Name of the hold out/test set to 
   *  estimate accuracy on.</pre>
   * 
   * <pre> -percentage-split
   *  Perform a percentage split on the training data.
   *  Use in conjunction with -T.</pre>
   * 
   * <pre> -P
   *  Split percentage to use (default = 90).</pre>
   * 
   * <pre> -S
   *  Random seed for percentage split (default = 1).</pre>
   * 
   * <pre> -E &lt;DEFAULT|ACC|RMSE|MAE|F-MEAS|AUC|AUPRC|CORR-COEFF&gt;
   *  Performance evaluation measure to use for selecting attributes.
   *  (Default = default: accuracy for discrete class and rmse for numeric class)</pre>
   * 
   * <pre> -IRclass &lt;label | index&gt;
   *  Optional class value (label or 1-based index) to use in conjunction with
   *  IR statistics (f-meas, auc or auprc). Omitting this option will use
   *  the class-weighted average.</pre>
   * 
   * <pre> 
   * Options specific to scheme weka.classifiers.rules.ZeroR:
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
   * <pre> -batch-size
   *  The desired batch size for batch prediction  (default 100).</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String optionString;
    resetOptions();

    optionString = Utils.getOption('B', options);
    if (optionString.length() == 0) {
      optionString = ZeroR.class.getName();
    }
    setClassifier(AbstractClassifier.forName(optionString,
      Utils.partitionOptions(options)));

    optionString = Utils.getOption('H', options);
    if (optionString.length() != 0) {
      setHoldOutFile(new File(optionString));
    }

    setUsePercentageSplit(Utils.getFlag("percentage-split", options));

    optionString = Utils.getOption('P', options);
    if (optionString.length() > 0) {
      setSplitPercent(optionString);
    }

    setUseTraining(Utils.getFlag('T', options));

    optionString = Utils.getOption('E', options);
    if (optionString.length() != 0) {
      for (Tag t : TAGS_EVALUATION) {
        if (t.getIDStr().equalsIgnoreCase(optionString)) {
          setEvaluationMeasure(new SelectedTag(t.getIDStr(), TAGS_EVALUATION));
          break;
        }
      }
    }

    optionString = Utils.getOption("IRClass", options);
    if (optionString.length() > 0) {
      setIRClassValue(optionString);
    }

    optionString = Utils.getOption("S", options);
    if (optionString.length() > 0) {
      setSeed(Integer.parseInt(optionString));
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String seedTipText() {
    return "The random seed to use for randomizing the training data "
      + "prior to performing a percentage split";
  }

  /**
   * Set the random seed used to randomize the data before performing a
   * percentage split
   * 
   * @param s the seed to use
   */
  public void setSeed(int s) {
    m_seed = s;
  }

  /**
   * Get the random seed used to randomize the data before performing a
   * percentage split
   * 
   * @return the seed to use
   */
  public int getSeed() {
    return m_seed;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String usePercentageSplitTipText() {
    return "Evaluate using a percentage split on the training data";
  }

  /**
   * Set whether to perform a percentage split on the training data for
   * evaluation
   * 
   * @param p true if a percentage split is to be performed
   */
  public void setUsePercentageSplit(boolean p) {
    m_usePercentageSplit = p;
  }

  /**
   * Get whether to perform a percentage split on the training data for
   * evaluation
   * 
   * @return true if a percentage split is to be performed
   */
  public boolean getUsePercentageSplit() {
    return m_usePercentageSplit;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String splitPercentTipText() {
    return "The percentage split to use";
  }

  /**
   * Set the split percentage to use
   * 
   * @param sp the split percentage to use
   */
  public void setSplitPercent(String sp) {
    m_splitPercent = sp;
  }

  /**
   * Get the split percentage to use
   * 
   * @return the split percentage to use
   */
  public String getSplitPercent() {
    return m_splitPercent;
  }

  /**
   * Set the class value (label or index) to use with IR metric evaluation of
   * subsets. Leaving this unset will result in the class weighted average for
   * the IR metric being used.
   * 
   * @param val the class label or 1-based index of the class label to use when
   *          evaluating subsets with an IR metric
   */
  public void setIRClassValue(String val) {
    m_IRClassValS = val;
  }

  /**
   * Get the class value (label or index) to use with IR metric evaluation of
   * subsets. Leaving this unset will result in the class weighted average for
   * the IR metric being used.
   * 
   * @return the class label or 1-based index of the class label to use when
   *         evaluating subsets with an IR metric
   */
  public String getIRClassValue() {
    return m_IRClassValS;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String IRClassValueTipText() {
    return "The class label, or 1-based index of the class label, to use "
      + "when evaluating subsets with an IR metric (such as f-measure "
      + "or AUC. Leaving this unset will result in the class frequency "
      + "weighted average of the metric being used.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String evaluationMeasureTipText() {
    return "The measure used to evaluate the performance of attribute combinations.";
  }

  /**
   * Gets the currently set performance evaluation measure used for selecting
   * attributes for the decision table
   * 
   * @return the performance evaluation measure
   */
  public SelectedTag getEvaluationMeasure() {
    return new SelectedTag(m_evaluationMeasure.getIDStr(), TAGS_EVALUATION);
  }

  /**
   * Sets the performance evaluation measure to use for selecting attributes for
   * the decision table
   * 
   * @param newMethod the new performance evaluation metric to use
   */
  public void setEvaluationMeasure(SelectedTag newMethod) {
    if (newMethod.getTags() == TAGS_EVALUATION) {
      m_evaluationMeasure = newMethod.getSelectedTag();
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String classifierTipText() {
    return "Classifier to use for estimating the accuracy of subsets";
  }

  /**
   * Set the classifier to use for accuracy estimation
   * 
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {
    m_ClassifierTemplate = newClassifier;
    m_Classifier = newClassifier;
  }

  /**
   * Get the classifier used as the base learner.
   * 
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {
    return m_ClassifierTemplate;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String holdOutFileTipText() {
    return "File containing hold out/test instances.";
  }

  /**
   * Gets the file that holds hold out/test instances.
   * 
   * @return File that contains hold out instances
   */
  public File getHoldOutFile() {
    return m_holdOutFile;
  }

  /**
   * Set the file that contains hold out/test instances
   * 
   * @param h the hold out file
   */
  public void setHoldOutFile(File h) {
    m_holdOutFile = h;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String useTrainingTipText() {
    return "Use training data instead of hold out/test instances.";
  }

  /**
   * Get if training data is to be used instead of hold out/test data
   * 
   * @return true if training data is to be used instead of hold out data
   */
  public boolean getUseTraining() {
    return m_useTraining;
  }

  /**
   * Set if training data is to be used instead of hold out/test data
   * 
   * @param t true if training data is to be used instead of hold out data
   */
  public void setUseTraining(boolean t) {
    m_useTraining = t;
  }

  /**
   * Gets the current settings of ClassifierSubsetEval
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    if (getClassifier() != null) {
      options.add("-B");
      options.add(getClassifier().getClass().getName());
    }

    if (getUseTraining()) {
      options.add("-T");
    }
    options.add("-H");
    options.add(getHoldOutFile().getPath());

    if (getUsePercentageSplit()) {
      options.add("-percentage-split");
      options.add("-P");
      options.add(m_splitPercent);
      options.add("-S");
      options.add("" + getSeed());
    }

    options.add("-E");
    options.add(m_evaluationMeasure.getIDStr());

    if (m_IRClassValS != null && m_IRClassValS.length() > 0) {
      options.add("-IRClass");
      options.add(m_IRClassValS);
    }

    if ((m_ClassifierTemplate != null)
      && (m_ClassifierTemplate instanceof OptionHandler)) {
      String[] classifierOptions =
        ((OptionHandler) m_ClassifierTemplate).getOptions();
      if (classifierOptions.length > 0) {
        options.add("--");
        Collections.addAll(options, classifierOptions);
      }
    }

    return options.toArray(new String[0]);
  }

  /**
   * Returns the capabilities of this evaluator.
   * 
   * @return the capabilities of this evaluator
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    if (getClassifier() == null) {
      result = super.getCapabilities();
      result.disableAll();
    } else {
      result = getClassifier().getCapabilities();
    }

    // set dependencies
    for (Capability cap : Capability.values()) {
      result.enableDependency(cap);
    }

    return result;
  }

  /**
   * Generates a attribute evaluator. Has to initialize all fields of the
   * evaluator that are not being set via options.
   * 
   * @param data set of instances serving as training data
   * @throws Exception if the evaluator has not been generated successfully
   */
  @Override
  public void buildEvaluator(Instances data) throws Exception {

    // can evaluator handle data?
    getCapabilities().testWithFail(data);

    m_trainingInstances = new Instances(data);
    m_classIndex = m_trainingInstances.classIndex();
    m_numAttribs = m_trainingInstances.numAttributes();
    // m_numInstances = m_trainingInstances.numInstances(); NOT USED

    // load the testing data
    if (!m_useTraining
      && (!getHoldOutFile().getPath().startsWith("Click to set"))) {
      java.io.Reader r =
        new java.io.BufferedReader(new java.io.FileReader(getHoldOutFile()
          .getPath()));
      m_holdOutInstances = new Instances(r);
      m_holdOutInstances.setClassIndex(m_trainingInstances.classIndex());
      if (m_trainingInstances.equalHeaders(m_holdOutInstances) == false) {
        throw new Exception("Hold out/test set is not compatable with "
          + "training data.\n"
          + m_trainingInstances.equalHeadersMsg(m_holdOutInstances));
      }
    } else if (m_usePercentageSplit) {
      int splitPercentage = 90; // default
      try {
        splitPercentage = Integer.parseInt(m_splitPercent);
      } catch (NumberFormatException n) {
      }

      m_trainingInstances.randomize(new Random(m_seed));
      int trainSize =
        Math.round(m_trainingInstances.numInstances() * splitPercentage / 100);
      int testSize = m_trainingInstances.numInstances() - trainSize;

      m_holdOutInstances =
        new Instances(m_trainingInstances, trainSize, testSize);
      m_trainingInstances = new Instances(m_trainingInstances, 0, trainSize);
    }

    if (m_IRClassValS != null && m_IRClassValS.length() > 0) {
      // try to parse as a number first
      try {
        m_IRClassVal = Integer.parseInt(m_IRClassValS);
        // make zero-based
        m_IRClassVal--;
      } catch (NumberFormatException e) {
        // now try as a named class label
        m_IRClassVal =
          m_trainingInstances.classAttribute().indexOfValue(m_IRClassValS);
      }
    }
  }

  /**
   * Evaluates a subset of attributes
   * 
   * @param subset a bitset representing the attribute subset to be evaluated
   * @return the error rate
   * @throws Exception if the subset could not be evaluated
   */
  @Override
  public double evaluateSubset(BitSet subset) throws Exception {
    int i, j;
    double evalMetric = 0;
    int numAttributes = 0;
    Instances trainCopy = null;
    Instances testCopy = null;
    String[] cOpts = null;
    Evaluation evaluation = null;
    if (m_ClassifierTemplate instanceof OptionHandler) {
      cOpts = ((OptionHandler) m_ClassifierTemplate).getOptions();
    }
    Classifier classifier =
      AbstractClassifier.forName(m_ClassifierTemplate.getClass().getName(),
        cOpts);

    Remove delTransform = new Remove();
    delTransform.setInvertSelection(true);
    // copy the training instances
    trainCopy = new Instances(m_trainingInstances);

    if (!m_useTraining) {
      if (m_holdOutInstances == null) {
        throw new Exception("Must specify a set of hold out/test instances "
          + "with -H");
      }
      // copy the test instances
      testCopy = new Instances(m_holdOutInstances);
    } else if (m_usePercentageSplit) {
      testCopy = new Instances(m_holdOutInstances);
    }

    // count attributes set in the BitSet
    for (i = 0; i < m_numAttribs; i++) {
      if (subset.get(i)) {
        numAttributes++;
      }
    }

    // set up an array of attribute indexes for the filter (+1 for the class)
    int[] featArray = new int[numAttributes + 1];

    for (i = 0, j = 0; i < m_numAttribs; i++) {
      if (subset.get(i)) {
        featArray[j++] = i;
      }
    }

    featArray[j] = m_classIndex;
    delTransform.setAttributeIndicesArray(featArray);
    delTransform.setInputFormat(trainCopy);
    trainCopy = Filter.useFilter(trainCopy, delTransform);
    if (!m_useTraining || m_usePercentageSplit) {
      testCopy = Filter.useFilter(testCopy, delTransform);
    }

    // build the classifier
    classifier.buildClassifier(trainCopy);

    evaluation = new Evaluation(trainCopy);
    if (!m_useTraining || m_usePercentageSplit) {
      evaluation.evaluateModel(classifier, testCopy);
    } else {
      evaluation.evaluateModel(classifier, trainCopy);
    }

    String metricName = null;
    String statName = null;
    AbstractEvaluationMetric pluginMetric = null;

    switch (m_evaluationMeasure.getID()) {
    case EVAL_DEFAULT:
      evalMetric = evaluation.errorRate();
      break;
    case EVAL_ACCURACY:
      evalMetric = evaluation.errorRate();
      break;
    case EVAL_RMSE:
      evalMetric = evaluation.rootMeanSquaredError();
      break;
    case EVAL_MAE:
      evalMetric = evaluation.meanAbsoluteError();
      break;
    case EVAL_FMEASURE:
      if (m_IRClassVal < 0) {
        evalMetric = evaluation.weightedFMeasure();
      } else {
        evalMetric = evaluation.fMeasure(m_IRClassVal);
      }
      break;
    case EVAL_AUC:
      if (m_IRClassVal < 0) {
        evalMetric = evaluation.weightedAreaUnderROC();
      } else {
        evalMetric = evaluation.areaUnderROC(m_IRClassVal);
      }
      break;
    case EVAL_AUPRC:
      if (m_IRClassVal < 0) {
        evalMetric = evaluation.weightedAreaUnderPRC();
      } else {
        evalMetric = evaluation.areaUnderPRC(m_IRClassVal);
      }
    case EVAL_CORRELATION:
      evalMetric = evaluation.correlationCoefficient();
      break;
    default:
      if (m_evaluationMeasure.getID() >= EVAL_PLUGIN) {
        metricName =
          ((WrapperSubsetEval.PluginTag) m_evaluationMeasure).getMetricName();
        statName =
          ((WrapperSubsetEval.PluginTag) m_evaluationMeasure)
            .getStatisticName();
        statName =
          ((WrapperSubsetEval.PluginTag) m_evaluationMeasure)
            .getStatisticName();
        pluginMetric = evaluation.getPluginMetric(metricName);
        if (pluginMetric == null) {
          throw new Exception("Metric  " + metricName + " does not seem to be "
            + "available");
        }
      }

      if (pluginMetric instanceof InformationRetrievalEvaluationMetric) {
        if (m_IRClassVal < 0) {
          evalMetric =
            ((InformationRetrievalEvaluationMetric) pluginMetric)
              .getClassWeightedAverageStatistic(statName);
        } else {
          evalMetric =
            ((InformationRetrievalEvaluationMetric) pluginMetric).getStatistic(
              statName, m_IRClassVal);
        }
      } else {
        evalMetric = pluginMetric.getStatistic(statName);
      }
      break;
    }

    switch (m_evaluationMeasure.getID()) {
    case EVAL_DEFAULT:
    case EVAL_ACCURACY:
    case EVAL_RMSE:
    case EVAL_MAE:
      if (m_trainingInstances.classAttribute().isNominal()
        && (m_evaluationMeasure.getID() == EVAL_DEFAULT || m_evaluationMeasure
          .getID() == EVAL_ACCURACY)) {
        evalMetric = 1 - evalMetric;
      } else {
        evalMetric = -evalMetric; // maximize
      }
      break;
    default:
      if (pluginMetric != null
        && !pluginMetric.statisticIsMaximisable(statName)) {
        evalMetric = -evalMetric; // maximize
      }
    }

    return evalMetric;
  }

  /**
   * Evaluates a subset of attributes with respect to a set of instances.
   * Calling this function overrides any test/hold out instances set from
   * setHoldOutFile.
   * 
   * @param subset a bitset representing the attribute subset to be evaluated
   * @param holdOut a set of instances (possibly separate and distinct from
   *          those use to build/train the evaluator) with which to evaluate the
   *          merit of the subset
   * @return the "merit" of the subset on the holdOut data
   * @throws Exception if the subset cannot be evaluated
   */
  public double evaluateSubset(BitSet subset, Instances holdOut)
    throws Exception {
    int i, j;
    double evalMetric = 0;
    int numAttributes = 0;
    Instances trainCopy = null;
    Instances testCopy = null;
    String[] cOpts = null;
    Evaluation evaluation = null;
    if (m_ClassifierTemplate instanceof OptionHandler) {
      cOpts = ((OptionHandler) m_ClassifierTemplate).getOptions();
    }
    Classifier classifier =
      AbstractClassifier.forName(m_ClassifierTemplate.getClass().getName(),
        cOpts);

    if (m_trainingInstances.equalHeaders(holdOut) == false) {
      throw new Exception("evaluateSubset : Incompatable instance types.\n"
        + m_trainingInstances.equalHeadersMsg(holdOut));
    }

    Remove delTransform = new Remove();
    delTransform.setInvertSelection(true);
    // copy the training instances
    trainCopy = new Instances(m_trainingInstances);

    testCopy = new Instances(holdOut);

    // count attributes set in the BitSet
    for (i = 0; i < m_numAttribs; i++) {
      if (subset.get(i)) {
        numAttributes++;
      }
    }

    // set up an array of attribute indexes for the filter (+1 for the class)
    int[] featArray = new int[numAttributes + 1];

    for (i = 0, j = 0; i < m_numAttribs; i++) {
      if (subset.get(i)) {
        featArray[j++] = i;
      }
    }

    featArray[j] = m_classIndex;
    delTransform.setAttributeIndicesArray(featArray);
    delTransform.setInputFormat(trainCopy);
    trainCopy = Filter.useFilter(trainCopy, delTransform);
    testCopy = Filter.useFilter(testCopy, delTransform);

    // build the classifier
    classifier.buildClassifier(trainCopy);

    evaluation = new Evaluation(trainCopy);
    evaluation.evaluateModel(classifier, testCopy);

    String metricName = null;
    String statName = null;
    AbstractEvaluationMetric pluginMetric = null;

    switch (m_evaluationMeasure.getID()) {
    case EVAL_DEFAULT:
      evalMetric = evaluation.errorRate();
      break;
    case EVAL_ACCURACY:
      evalMetric = evaluation.errorRate();
      break;
    case EVAL_RMSE:
      evalMetric = evaluation.rootMeanSquaredError();
      break;
    case EVAL_MAE:
      evalMetric = evaluation.meanAbsoluteError();
      break;
    case EVAL_FMEASURE:
      if (m_IRClassVal < 0) {
        evalMetric = evaluation.weightedFMeasure();
      } else {
        evalMetric = evaluation.fMeasure(m_IRClassVal);
      }
      break;
    case EVAL_AUC:
      if (m_IRClassVal < 0) {
        evalMetric = evaluation.weightedAreaUnderROC();
      } else {
        evalMetric = evaluation.areaUnderROC(m_IRClassVal);
      }
      break;
    case EVAL_AUPRC:
      if (m_IRClassVal < 0) {
        evalMetric = evaluation.weightedAreaUnderPRC();
      } else {
        evalMetric = evaluation.areaUnderPRC(m_IRClassVal);
      }
    case EVAL_CORRELATION:
      evalMetric = evaluation.correlationCoefficient();
      break;
    default:
      if (m_evaluationMeasure.getID() >= EVAL_PLUGIN) {
        metricName =
          ((WrapperSubsetEval.PluginTag) m_evaluationMeasure).getMetricName();
        statName =
          ((WrapperSubsetEval.PluginTag) m_evaluationMeasure)
            .getStatisticName();
        statName =
          ((WrapperSubsetEval.PluginTag) m_evaluationMeasure)
            .getStatisticName();
        pluginMetric = evaluation.getPluginMetric(metricName);
        if (pluginMetric == null) {
          throw new Exception("Metric  " + metricName + " does not seem to be "
            + "available");
        }
      }

      if (pluginMetric instanceof InformationRetrievalEvaluationMetric) {
        if (m_IRClassVal < 0) {
          evalMetric =
            ((InformationRetrievalEvaluationMetric) pluginMetric)
              .getClassWeightedAverageStatistic(statName);
        } else {
          evalMetric =
            ((InformationRetrievalEvaluationMetric) pluginMetric).getStatistic(
              statName, m_IRClassVal);
        }
      } else {
        evalMetric = pluginMetric.getStatistic(statName);
      }
      break;
    }

    switch (m_evaluationMeasure.getID()) {
    case EVAL_DEFAULT:
    case EVAL_ACCURACY:
    case EVAL_RMSE:
    case EVAL_MAE:
      if (m_trainingInstances.classAttribute().isNominal()
        && (m_evaluationMeasure.getID() == EVAL_DEFAULT || m_evaluationMeasure
          .getID() == EVAL_ACCURACY)) {
        evalMetric = 1 - evalMetric;
      } else {
        evalMetric = -evalMetric; // maximize
      }
      break;
    default:
      if (pluginMetric != null
        && !pluginMetric.statisticIsMaximisable(statName)) {
        evalMetric = -evalMetric; // maximize
      }
    }

    return evalMetric;
  }

  /**
   * Evaluates a subset of attributes with respect to a single instance. Calling
   * this function overides any hold out/test instances set through
   * setHoldOutFile.
   * 
   * @param subset a bitset representing the attribute subset to be evaluated
   * @param holdOut a single instance (possibly not one of those used to
   *          build/train the evaluator) with which to evaluate the merit of the
   *          subset
   * @param retrain true if the classifier should be retrained with respect to
   *          the new subset before testing on the holdOut instance.
   * @return the "merit" of the subset on the holdOut instance
   * @throws Exception if the subset cannot be evaluated
   */
  @Override
  public double
    evaluateSubset(BitSet subset, Instance holdOut, boolean retrain)
      throws Exception {

    if (m_evaluationMeasure.getID() != EVAL_DEFAULT) {
      throw new Exception(
        "Can only use default evaluation measure in the method");
    }
    int i, j;
    double error;
    int numAttributes = 0;
    Instances trainCopy = null;
    Instance testCopy = null;
    if (m_trainingInstances.equalHeaders(holdOut.dataset()) == false) {
      throw new Exception("evaluateSubset : Incompatable instance types.\n"
        + m_trainingInstances.equalHeadersMsg(holdOut.dataset()));
    }

    Remove delTransform = new Remove();
    delTransform.setInvertSelection(true);
    // copy the training instances
    trainCopy = new Instances(m_trainingInstances);

    testCopy = (Instance) holdOut.copy();

    // count attributes set in the BitSet
    for (i = 0; i < m_numAttribs; i++) {
      if (subset.get(i)) {
        numAttributes++;
      }
    }

    // set up an array of attribute indexes for the filter (+1 for the class)
    int[] featArray = new int[numAttributes + 1];

    for (i = 0, j = 0; i < m_numAttribs; i++) {
      if (subset.get(i)) {
        featArray[j++] = i;
      }
    }
    featArray[j] = m_classIndex;
    delTransform.setAttributeIndicesArray(featArray);
    delTransform.setInputFormat(trainCopy);

    if (retrain) {
      trainCopy = Filter.useFilter(trainCopy, delTransform);
      // build the classifier
      m_Classifier.buildClassifier(trainCopy);
    }

    delTransform.input(testCopy);
    testCopy = delTransform.output();

    double pred;
    double[] distrib;
    distrib = m_Classifier.distributionForInstance(testCopy);
    if (m_trainingInstances.classAttribute().isNominal()) {
      pred = distrib[(int) testCopy.classValue()];
    } else {
      pred = distrib[0];
    }

    if (m_trainingInstances.classAttribute().isNominal()) {
      error = 1.0 - pred;
    } else {
      error = testCopy.classValue() - pred;
    }

    // return the negative of the error as search methods need to
    // maximize something
    return -error;
  }

  /**
   * Returns a string describing classifierSubsetEval
   * 
   * @return the description as a string
   */
  @Override
  public String toString() {
    StringBuffer text = new StringBuffer();

    if (m_trainingInstances == null) {
      text.append("\tClassifier subset evaluator has not been built yet\n");
    } else {
      text.append("\tClassifier Subset Evaluator\n");
      text.append("\tLearning scheme: " + getClassifier().getClass().getName()
        + "\n");
      text.append("\tScheme options: ");
      String[] classifierOptions = new String[0];

      if (m_ClassifierTemplate instanceof OptionHandler) {
        classifierOptions = ((OptionHandler) m_ClassifierTemplate).getOptions();

        for (String classifierOption : classifierOptions) {
          text.append(classifierOption + " ");
        }
      }

      text.append("\n");
      text.append("\tHold out/test set: ");
      if (!m_useTraining) {
        if (getHoldOutFile().getPath().startsWith("Click to set")) {
          text.append("none\n");
        } else {
          text.append(getHoldOutFile().getPath() + '\n');
        }
      } else {
        if (m_usePercentageSplit) {
          text.append("Percentage split: " + m_splitPercent + "\n");
        } else {
          text.append("Training data\n");
        }
      }

      String IRClassL = "";
      if (m_IRClassVal >= 0) {
        IRClassL =
          "(class value: "
            + m_trainingInstances.classAttribute().value(m_IRClassVal) + ")";
      }
      switch (m_evaluationMeasure.getID()) {
      case EVAL_DEFAULT:
      case EVAL_ACCURACY:
        if (m_trainingInstances.attribute(m_classIndex).isNumeric()) {
          text.append("\tSubset evaluation: RMSE\n");
        } else {
          text.append("\tSubset evaluation: classification error\n");
        }
        break;
      case EVAL_RMSE:
        if (m_trainingInstances.attribute(m_classIndex).isNumeric()) {
          text.append("\tSubset evaluation: RMSE\n");
        } else {
          text.append("\tSubset evaluation: RMSE (probability estimates)\n");
        }
        break;
      case EVAL_MAE:
        if (m_trainingInstances.attribute(m_classIndex).isNumeric()) {
          text.append("\tSubset evaluation: MAE\n");
        } else {
          text.append("\tSubset evaluation: MAE (probability estimates)\n");
        }
        break;
      case EVAL_FMEASURE:
        text.append("\tSubset evaluation: F-measure "
          + (m_IRClassVal >= 0 ? IRClassL : "") + "\n");
        break;
      case EVAL_AUC:
        text.append("\tSubset evaluation: area under the ROC curve "
          + (m_IRClassVal >= 0 ? IRClassL : "") + "\n");
        break;
      case EVAL_AUPRC:
        text.append("\tSubset evalation: area under the precision-recal curve "
          + (m_IRClassVal >= 0 ? IRClassL : "") + "\n");
        break;
      case EVAL_CORRELATION:
        text.append("\tSubset evaluation: correlation coefficient\n");
        break;
      default:
        text
          .append("\tSubset evaluation: " + m_evaluationMeasure.getReadable());
        if (((WrapperSubsetEval.PluginTag) m_evaluationMeasure).getMetric() instanceof InformationRetrievalEvaluationMetric) {
          text.append(" " + (m_IRClassVal > 0 ? IRClassL : ""));
        }
        text.append("\n");
        break;
      }
    }
    return text.toString();
  }

  /**
   * reset to defaults
   */
  protected void resetOptions() {
    m_trainingInstances = null;
    m_ClassifierTemplate = new ZeroR();
    m_holdOutFile = new File("Click to set hold out or test instances");
    m_holdOutInstances = null;
    m_useTraining = false;
    m_splitPercent = "90";
    m_usePercentageSplit = false;
    m_evaluationMeasure = TAGS_EVALUATION[0];
    m_IRClassVal = -1;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10332 $");
  }

  /**
   * Main method for testing this class.
   * 
   * @param args the options
   */
  public static void main(String[] args) {
    runEvaluator(new ClassifierSubsetEval(), args);
  }
}

