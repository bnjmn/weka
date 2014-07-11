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
 *    IterativeClassifierOptimizer.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.IterativeClassifier;
import weka.classifiers.RandomizableClassifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.EvaluationMetricHelper;
import weka.classifiers.evaluation.ThresholdProducingMetric;
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

/**
 * Chooses the best number of iterations for an IterativeClassifier such as
 * LogitBoost using cross-validation.
 * 
 <!-- globalinfo-start -->
 * Optimizes the number of iterations of the given iterative classifier using cross-validation.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -A
 *  If set, average estimate is used rather than one estimate from pooled predictions.
 * </pre>
 * 
 * <pre> -L &lt;num&gt;
 *  The number of iterations to look ahead for to find a better optimum.
 *  (default 50)</pre>
 * 
 * <pre> -F &lt;num&gt;
 *  Number of folds for cross-validation.
 *  (default 10)</pre>
 * 
 * <pre> -R &lt;num&gt;
 *  Number of runs for cross-validation.
 *  (default 1)</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.meta.LogitBoost)</pre>
 * 
 * <pre> -metric &lt;name&gt;
 *  Evaluation metric to optimise (default rmse). Available metrics:
 *  correct,incorrect,kappa,total cost,average cost,kb relative,kb information,
 *  correlation,complexity 0,complexity scheme,complexity improvement,
 *  mae,rmse,rae,rrse,coverage,region size,tp rate,fp rate,precision,recall,
 *  f-measure,mcc,roc area,prc area</pre>
 * 
 * <pre> -class-value-index &lt;0-based index&gt;
 *  Class value index to optimise. Ignored for all but information-retrieval
 *  type metrics (such as roc area). If unspecified (or a negative value is supplied),
 *  and an information-retrieval metric is specified, then the class-weighted average
 *  metric used. (default -1)</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.meta.LogitBoost:
 * </pre>
 * 
 * <pre> -Q
 *  Use resampling instead of reweighting for boosting.</pre>
 * 
 * <pre> -P &lt;percent&gt;
 *  Percentage of weight mass to base training on.
 *  (default 100, reduce to around 90 speed up)</pre>
 * 
 * <pre> -L &lt;num&gt;
 *  Threshold on the improvement of the likelihood.
 *  (default -Double.MAX_VALUE)</pre>
 * 
 * <pre> -H &lt;num&gt;
 *  Shrinkage parameter.
 *  (default 1)</pre>
 * 
 * <pre> -Z &lt;num&gt;
 *  Z max threshold for responses.
 *  (default 3)</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -I &lt;num&gt;
 *  Number of iterations.
 *  (default 10)</pre>
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
 <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 10141 $
 */
public class IterativeClassifierOptimizer extends RandomizableClassifier {

  /** for serialization */
  private static final long serialVersionUID = -3665485256313525864L;

  /** The base classifier to use */
  protected IterativeClassifier m_IterativeClassifier = new LogitBoost();

  /** The number of folds for the cross-validation. */
  protected int m_NumFolds = 10;

  /** The number of runs for the cross-validation. */
  protected int m_NumRuns = 1;

  /** Whether to use average. */
  protected boolean m_UseAverage = false;

  /** The number of iterations to look ahead for to find a better optimum. */
  protected int m_lookAheadIterations = 50;

  public static Tag[] TAGS_EVAL;

  static {
    List<String> evalNames = EvaluationMetricHelper.getAllMetricNames();
    TAGS_EVAL = new Tag[evalNames.size()];
    for (int i = 0; i < evalNames.size(); i++) {
      TAGS_EVAL[i] = new Tag(i, evalNames.get(i), evalNames.get(i), false);
    }
  }

  /** The evaluation metric to use */
  protected String m_evalMetric = "rmse";

  /**
   * The class value index to use with information retrieval type metrics. < 0
   * indicates to use the class weighted average version of the metric".
   */
  protected int m_classValueIndex = -1;

  /** 
   * The thresholds to be used for classification, if the metric implements
   * ThresholdProducingMetric.
   */
  protected double[] m_thresholds = null;

  /**
   * The best value found for the criterion to be optimized.
   */
  protected double m_bestResult = Double.MAX_VALUE;

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {

    return "Optimizes the number of iterations of the given iterative "
      + "classifier using cross-validation.";
  }

  /**
   * String describing default classifier.
   */
  protected String defaultIterativeClassifierString() {

    return "weka.classifiers.meta.LogitBoost";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String useAverageTipText() {
    return "If true, average estimates are used instead of one estimate from pooled predictions.";
  }

  /**
   * Get the value of UseAverage.
   * 
   * @return Value of UseAverage.
   */
  public boolean getUseAverage() {

    return m_UseAverage;
  }

  /**
   * Set the value of UseAverage.
   * 
   * @param newUseAverage Value to assign to UseAverage.
   */
  public void setUseAverage(boolean newUseAverage) {

    m_UseAverage = newUseAverage;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numRunsTipText() {
    return "Number of runs for cross-validation.";
  }

  /**
   * Get the value of NumRuns.
   * 
   * @return Value of NumRuns.
   */
  public int getNumRuns() {

    return m_NumRuns;
  }

  /**
   * Set the value of NumRuns.
   * 
   * @param newNumRuns Value to assign to NumRuns.
   */
  public void setNumRuns(int newNumRuns) {

    m_NumRuns = newNumRuns;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numFoldsTipText() {
    return "Number of folds for cross-validation.";
  }

  /**
   * Get the value of NumFolds.
   * 
   * @return Value of NumFolds.
   */
  public int getNumFolds() {

    return m_NumFolds;
  }

  /**
   * Set the value of NumFolds.
   * 
   * @param newNumFolds Value to assign to NumFolds.
   */
  public void setNumFolds(int newNumFolds) {

    m_NumFolds = newNumFolds;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String lookAheadIterationsTipText() {
    return "The number of iterations to look ahead for to find a better optimum.";
  }

  /**
   * Get the value of LookAheadIterations.
   * 
   * @return Value of LookAheadIterations.
   */
  public int getLookAheadIterations() {

    return m_lookAheadIterations;
  }

  /**
   * Set the value of LookAheadIterations.
   * 
   * @param newLookAheadIterations Value to assign to LookAheadIterations.
   */
  public void setLookAheadIterations(int newLookAheadIterations) {

    m_lookAheadIterations = newLookAheadIterations;
  }

  /**
   * Builds the classifier.
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

    if (m_IterativeClassifier == null) {
      throw new Exception("A base classifier has not been specified!");
    }

    // Can classifier handle the data?
    getCapabilities().testWithFail(data);

    // Need to shuffle the data
    Random randomInstance = new Random(m_Seed);

    // Save reference to original data
    Instances origData = data;

    // Remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    if (data.numInstances() < m_NumFolds) {
      System.err.println("WARNING: reducing number of folds to number of instances in " +
                         "IterativeClassifierOptimizer");
      m_NumFolds = data.numInstances();
    }

    // Initialize datasets and classifiers
    Instances[][] trainingSets = new Instances[m_NumRuns][m_NumFolds];
    Instances[][] testSets = new Instances[m_NumRuns][m_NumFolds];
    IterativeClassifier[][] classifiers =
      new IterativeClassifier[m_NumRuns][m_NumFolds];
    for (int j = 0; j < m_NumRuns; j++) {
      data.randomize(randomInstance);
      if (data.classAttribute().isNominal()) {
        data.stratify(m_NumFolds);
      }
      for (int i = 0; i < m_NumFolds; i++) {
        trainingSets[j][i] = data.trainCV(m_NumFolds, i, randomInstance);
        testSets[j][i] = data.testCV(m_NumFolds, i);
        classifiers[j][i] =
          (IterativeClassifier) AbstractClassifier.makeCopy(m_IterativeClassifier);
        classifiers[j][i].initializeClassifier(trainingSets[j][i]);
      }
    }

    // Perform evaluation
    EvaluationMetricHelper helper = null;
    Evaluation eval = null;
    boolean maximise = true;
    int numIts = 0;
    int bestIts = 0;
    m_bestResult = Double.MAX_VALUE;
    m_thresholds = null;
    int numberOfIterationsSinceMinimum = -1;
    while (true) {
      double result = 0;
      double[] tempThresholds = null;

      // Shall we use the average score obtained from the folds or not?
      if (!m_UseAverage) {
        eval = new Evaluation(data);
        if (helper == null) {
          helper = new EvaluationMetricHelper(eval);
          maximise = helper.metricIsMaximisable(m_evalMetric);
          if (maximise) {
            m_bestResult = Double.MIN_VALUE;
          }
        } else {
          helper.setEvaluation(eval);
        }
      }
      for (int r = 0; r < m_NumRuns; r++) {
        for (int i = 0; i < m_NumFolds; i++) {

          // Shall we use the average score obtained from the folds or not?
          if (m_UseAverage) {
            eval = new Evaluation(trainingSets[r][i]);
            if (helper == null) {
              helper = new EvaluationMetricHelper(eval);
              maximise = helper.metricIsMaximisable(m_evalMetric);
              if (maximise) {
                m_bestResult = Double.MIN_VALUE;
              }
            } else {
              helper.setEvaluation(eval);
            }
          }
          eval.evaluateModel(classifiers[r][i], testSets[r][i]);

          // Shall we use the average score obtained from the folds or not?
          if (m_UseAverage) {
            result +=
              getClassValueIndex() >= 0 ? 
              helper.getNamedMetric(m_evalMetric,
                                    getClassValueIndex()) : helper.getNamedMetric(m_evalMetric);
            double[] thresholds = helper.getNamedMetricThresholds(m_evalMetric);
            
            // Add thresholds (if applicable) so that we can compute average thresholds later
            if (thresholds != null) {
              if (tempThresholds == null) {
                tempThresholds = new double[data.numClasses()];
              }
              for (int j = 0; j < thresholds.length; j++) {
                tempThresholds[j] += thresholds[j];
              }
            }
          }
        }
      }
      
      // Shall we use the average score obtained from the folds or not?
      if (!m_UseAverage) {
        result =
          getClassValueIndex() >= 0 ? 
          helper.getNamedMetric(m_evalMetric,
                                getClassValueIndex()) : helper.getNamedMetric(m_evalMetric);
        tempThresholds = helper.getNamedMetricThresholds(m_evalMetric);
      } else {
        result /= (double)(m_NumFolds * m_NumRuns);

        // Compute average thresholds if applicable
        if (tempThresholds != null) {
          for (int j = 0; j < tempThresholds.length; j++) {
            tempThresholds[j] /= (double) (m_NumRuns * m_NumFolds);
          }
        }
      }
      if (m_Debug) {
        System.err.println("Iteration: " + numIts + " " + "Measure: " + result);
        if (tempThresholds != null) {
          System.err.print("Thresholds:");
          for (int j = 0; j < tempThresholds.length; j++) {
            System.err.print(" " + tempThresholds[j]);
          }
          System.err.println();
        }
      }

      double delta = maximise ? m_bestResult - result : result - m_bestResult;

      // Is there an improvement?
      if (delta < 0) {
        m_bestResult = result;
        bestIts = numIts;
        m_thresholds = tempThresholds;
        numberOfIterationsSinceMinimum = -1;
      }
      numberOfIterationsSinceMinimum++;
      numIts++;

      if (numberOfIterationsSinceMinimum >= m_lookAheadIterations) {
        break;
      }
        
      // Update classifiers for next round
      for (int r = 0; r < m_NumRuns; r++) {
        for (int i = 0; i < m_NumFolds; i++) {
          if (!classifiers[r][i].next()) {
            if (m_Debug) {
              System.err.println("Classifier failed to iterate in cross-validation.");
            }
            break; // Break out if one classifier fails to iterate
          }
        }
      }
    }
    classifiers = null;
    trainingSets = null;
    testSets = null;
    data = null;

    // Build classifieer based on identified number of iterations
    m_IterativeClassifier.initializeClassifier(origData);
    int i = 0;
    while (i++ < bestIts && m_IterativeClassifier.next()) {
    }
    ;
    m_IterativeClassifier.done();
  }

  /**
   * Returns the class distribution for an instance.
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {

    // Does the metric produce thresholds that need to be applied?
    if (m_thresholds != null) {
      double[] dist = m_IterativeClassifier.distributionForInstance(inst);
      double[] newDist = new double[dist.length];
      for (int i = 0; i < dist.length; i++) {
        if (dist[i] >= m_thresholds[i]) {
          newDist[i] = 1.0;
        }
      }
      Utils.normalize(newDist); // Could have multiple 1.0 entries
      return newDist;
    } else {
      return m_IterativeClassifier.distributionForInstance(inst);
    }
  }

  /**
   * Returns a string describing the classifier.
   */
  @Override
  public String toString() {

    if (m_IterativeClassifier == null) {
      return "No classifier built yet.";
    } else {
      StringBuffer sb = new StringBuffer();
      sb.append("Best value found: " + m_bestResult + "\n\n");
      if (m_thresholds != null) {
        sb.append("Thresholds found: ");
        for (int i = 0; i < m_thresholds.length; i++) {
          sb.append(m_thresholds[i] + " ");
        }
      }
      sb.append("\n\n");
      sb.append(m_IterativeClassifier.toString());
      return sb.toString();
    }
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(4);

    newVector.addElement(new Option("\tIf set, average estimate is used rather "
                                    + "than one estimate from pooled predictions.\n", "A", 0, "-A"));
    newVector.addElement(new Option("\t" + lookAheadIterationsTipText() + "\n"
      + "\t(default 50)", "L", 1, "-L <num>"));
    newVector.addElement(new Option("\tNumber of folds for cross-validation.\n"
      + "\t(default 10)", "F", 1, "-F <num>"));
    newVector.addElement(new Option("\tNumber of runs for cross-validation.\n"
      + "\t(default 1)", "R", 1, "-R <num>"));

    newVector
      .addElement(new Option("\tFull name of base classifier.\n"
        + "\t(default: " + defaultIterativeClassifierString() + ")", "W", 1,
        "-W"));

    List<String> metrics = EvaluationMetricHelper.getAllMetricNames();
    StringBuilder b = new StringBuilder();
    int length = 0;
    for (String m : metrics) {
      b.append(m.toLowerCase()).append(",");

      length += m.length();
      if (length >= 60) {
        b.append("\n\t");
        length = 0;
      }
    }
    newVector.addElement(new Option(
      "\tEvaluation metric to optimise (default rmse). Available metrics:\n\t"
        + b.substring(0, b.length() - 1), "metric", 1, "-metric <name>"));

    newVector
      .addElement(new Option(
        "\tClass value index to optimise. Ignored for all but information-retrieval\n\t"
          + "type metrics (such as roc area). If unspecified (or a negative value is supplied),\n\t"
          + "and an information-retrieval metric is specified, then the class-weighted average\n\t"
          + "metric used. (default -1)", "class-value-index", 1,
        "-class-value-index <0-based index>"));

    newVector.addAll(Collections.list(super.listOptions()));

    newVector.addElement(new Option("", "", 0,
      "\nOptions specific to classifier "
        + m_IterativeClassifier.getClass().getName() + ":"));
    newVector.addAll(Collections.list(((OptionHandler) m_IterativeClassifier)
      .listOptions()));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:
   * <p>
   * 
   * -W classname <br>
   * Specify the full class name of the base learner.
   * <p>
   * 
   * Options after -- are passed to the designated classifier.
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    super.setOptions(options);

    setUseAverage(Utils.getFlag('A', options));

    String lookAheadIterations = Utils.getOption('L', options);
    if (lookAheadIterations.length() != 0) {
      setLookAheadIterations(Integer.parseInt(lookAheadIterations));
    } else {
      setLookAheadIterations(50);
    }

    String numFolds = Utils.getOption('F', options);
    if (numFolds.length() != 0) {
      setNumFolds(Integer.parseInt(numFolds));
    } else {
      setNumFolds(10);
    }

    String numRuns = Utils.getOption('R', options);
    if (numRuns.length() != 0) {
      setNumRuns(Integer.parseInt(numRuns));
    } else {
      setNumRuns(1);
    }

    String evalMetric = Utils.getOption("metric", options);
    if (evalMetric.length() > 0) {
      boolean found = false;
      for (int i = 0; i < TAGS_EVAL.length; i++) {
        if (TAGS_EVAL[i].getIDStr().equalsIgnoreCase(evalMetric)) {
          setEvaluationMetric(new SelectedTag(i, TAGS_EVAL));
          found = true;
          break;
        }
      }

      if (!found) {
        throw new Exception("Unknown evaluation metric: " + evalMetric);
      }
    }

    String classValIndex = Utils.getOption("class-value-index", options);
    if (classValIndex.length() > 0) {
      setClassValueIndex(Integer.parseInt(classValIndex));
    } else {
      setClassValueIndex(-1);
    }

    String classifierName = Utils.getOption('W', options);

    if (classifierName.length() > 0) {
      setIterativeClassifier(getIterativeClassifier(classifierName,
        Utils.partitionOptions(options)));
    } else {
      setIterativeClassifier(getIterativeClassifier(
        defaultIterativeClassifierString(), Utils.partitionOptions(options)));
    }
  }

  /**
   * Get classifier for string.
   * 
   * @return a classifier
   * @throws exception if a problem occurs
   */
  protected IterativeClassifier getIterativeClassifier(String name,
    String[] options) throws Exception {

    Classifier c = AbstractClassifier.forName(name, options);
    if (c instanceof IterativeClassifier) {
      return (IterativeClassifier) c;
    } else {
      throw new IllegalArgumentException(name
        + " is not an IterativeClassifier.");
    }
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    if (getUseAverage()) {
      options.add("-A");
    }

    options.add("-W");
    options.add(getIterativeClassifier().getClass().getName());

    options.add("-L");
    options.add("" + getLookAheadIterations());

    options.add("-F");
    options.add("" + getNumFolds());
    options.add("-R");
    options.add("" + getNumRuns());

    options.add("-metric");
    options.add(getEvaluationMetric().getSelectedTag().getIDStr());

    if (getClassValueIndex() >= 0) {
      options.add("-class-value-index");
      options.add("" + getClassValueIndex());
    }

    Collections.addAll(options, super.getOptions());

    String[] classifierOptions =
      ((OptionHandler) m_IterativeClassifier).getOptions();
    if (classifierOptions.length > 0) {
      options.add("--");
      Collections.addAll(options, classifierOptions);
    }

    return options.toArray(new String[0]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String evaluationMetricTipText() {
    return "The evaluation metric to use";
  }

  /**
   * Set the evaluation metric to use
   * 
   * @param metric the metric to use
   */
  public void setEvaluationMetric(SelectedTag metric) {
    if (metric.getTags() == TAGS_EVAL) {
      m_evalMetric = metric.getSelectedTag().getIDStr();
    }
  }

  /**
   * Get the evaluation metric to use
   * 
   * @return the evaluation metric to use
   */
  public SelectedTag getEvaluationMetric() {
    for (int i = 0; i < TAGS_EVAL.length; i++) {
      if (TAGS_EVAL[i].getIDStr().equalsIgnoreCase(m_evalMetric)) {
        return new SelectedTag(i, TAGS_EVAL);
      }
    }

    // if we get here then it could be because a plugin
    // metric is no longer available. Default to rmse
    return new SelectedTag(12, TAGS_EVAL);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String classValueIndexTipText() {
    return "The class value index to use with information retrieval type metrics. A value < 0"
      + " indicates to use the class weighted average version of the metric.";
  }

  /**
   * Set the class value index to use
   * 
   * @param i the class value index to use
   */
  public void setClassValueIndex(int i) {
    m_classValueIndex = i;
  }

  /**
   * Get the class value index to use
   * 
   * @return the class value index to use
   */
  public int getClassValueIndex() {
    return m_classValueIndex;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String iterativeClassifierTipText() {
    return "The iterative classifier to be optimized.";
  }

  /**
   * Returns default capabilities of the base classifier.
   * 
   * @return the capabilities of the base classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    if (getIterativeClassifier() != null) {
      result = getIterativeClassifier().getCapabilities();
    } else {
      result = new Capabilities(this);
      result.disableAll();
    }

    // set dependencies
    for (Capability cap : Capability.values()) {
      result.enableDependency(cap);
    }

    result.setOwner(this);

    return result;
  }

  /**
   * Set the base learner.
   * 
   * @param newIterativeClassifier the classifier to use.
   */
  public void
    setIterativeClassifier(IterativeClassifier newIterativeClassifier) {

    m_IterativeClassifier = newIterativeClassifier;
  }

  /**
   * Get the classifier used as the base learner.
   * 
   * @return the classifier used as the classifier
   */
  public IterativeClassifier getIterativeClassifier() {

    return m_IterativeClassifier;
  }

  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   * 
   * @return the classifier string
   */
  protected String getIterativeClassifierSpec() {

    IterativeClassifier c = getIterativeClassifier();
    return c.getClass().getName() + " "
      + Utils.joinOptions(((OptionHandler) c).getOptions());
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10649 $");
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv the options
   */
  public static void main(String[] argv) {
    runClassifier(new IterativeClassifierOptimizer(), argv);
  }
}

