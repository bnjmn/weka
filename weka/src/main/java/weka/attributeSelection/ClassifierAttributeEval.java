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
 *    ClassifierAttributeEval.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.attributeSelection;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Utils;

/**
 <!-- globalinfo-start -->
 * ClassifierAttributeEval :<br/>
 * <br/>
 * Evaluates the worth of an attribute by using a user-specified classifier.<br/>
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * <pre> -L
 *  Evaluate an attribute by measuring the impact of leaving it out
 *  from the full set instead of considering its worth in isolation</pre>
 * 
 * <pre> -B &lt;base learner&gt;
 *  class name of base learner to use for  accuracy estimation.
 *  Place any classifier options LAST on the command line
 *  following a "--". eg.:
 *   -B weka.classifiers.bayes.NaiveBayes ... -- -K
 *  (default: weka.classifiers.rules.ZeroR)</pre>
 * 
 * <pre> -F &lt;num&gt;
 *  number of cross validation folds to use for estimating accuracy.
 *  (default=5)</pre>
 * 
 * <pre> -R &lt;seed&gt;
 *  Seed for cross validation accuracy testimation.
 *  (default = 1)</pre>
 * 
 * <pre> -T &lt;num&gt;
 *  threshold by which to execute another cross validation
 *  (standard deviation---expressed as a percentage of the mean).
 *  (default: 0.01 (1%))</pre>
 * 
 * <pre> -E &lt;acc | rmse | mae | f-meas | auc | auprc&gt;
 *  Performance evaluation measure to use for selecting attributes.
 *  (Default = accuracy for discrete class and rmse for numeric class)</pre>
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
 * <pre> -execution-slots &lt;integer&gt;
 *  Number of attributes to evaluate in parallel.
 *  Default = 1 (i.e. no parallelism)</pre>
 * 
 <!-- options-end -->
 * 
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 14195 $
 */
public class ClassifierAttributeEval extends ASEvaluation implements
  AttributeEvaluator, OptionHandler {

  /** for serialization. */
  private static final long serialVersionUID = 2442390690522602284L;

  /** The training instances. */
  protected Instances m_trainInstances;

  /** Holds the merit scores for each attribute */
  protected double[] m_merit;

  /** The configured underlying Wrapper instance to use for evaluation */
  protected WrapperSubsetEval m_wrapperTemplate = new WrapperSubsetEval();

  /** Holds toString() info for the wrapper */
  protected String m_wrapperSetup = "";

  /**
   * Whether to leave each attribute out in turn and evaluate rather than just
   * evaluate on each attribute
   */
  protected boolean m_leaveOneOut;

  /** Executor service for multi-threading */
  protected transient ExecutorService m_pool;

  /** The number of attributes to evaluate in parallel */
  protected int m_executionSlots = 1;

  /**
   * Constructor.
   */
  public ClassifierAttributeEval() {
    resetOptions();
  }

  /**
   * Returns a string describing this attribute evaluator.
   * 
   * @return a description of the evaluator suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "ClassifierAttributeEval :\n\nEvaluates the worth of an attribute by "
      + "using a user-specified classifier.\n";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    Enumeration<Option> wrapperOpts = m_wrapperTemplate.listOptions();
    while (wrapperOpts.hasMoreElements()) {
      result.addElement(wrapperOpts.nextElement());
    }

    result.addElement(new Option(
      "\tEvaluate an attribute by measuring the impact of leaving it out\n\t"
        + "from the full set instead of considering its worth in isolation",
      "L", 0, "-L"));

    result.addElement(new Option(
      "\tNumber of attributes to evaluate in parallel.\n\t"
        + "Default = 1 (i.e. no parallelism)", "execution-slots", 1,
      "-execution-slots <integer>"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -B &lt;base learner&gt;
   *  class name of base learner to use for  accuracy estimation.
   *  Place any classifier options LAST on the command line
   *  following a "--". eg.:
   *   -B weka.classifiers.bayes.NaiveBayes ... -- -K
   *  (default: weka.classifiers.rules.ZeroR)</pre>
   * 
   * <pre> -F &lt;num&gt;
   *  number of cross validation folds to use for estimating accuracy.
   *  (default=5)</pre>
   * 
   * <pre> -R &lt;seed&gt;
   *  Seed for cross validation accuracy testimation.
   *  (default = 1)</pre>
   * 
   * <pre> -T &lt;num&gt;
   *  threshold by which to execute another cross validation
   *  (standard deviation---expressed as a percentage of the mean).
   *  (default: 0.01 (1%))</pre>
   * 
   * <pre> -E &lt;acc | rmse | mae | f-meas | auc | auprc&gt;
   *  Performance evaluation measure to use for selecting attributes.
   *  (Default = accuracy for discrete class and rmse for numeric class)</pre>
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
   * <pre> -L
   *  Evaluate an attribute by measuring the impact of leaving it out
   *  from the full set instead of considering its worth in isolation</pre>
   * 
   * <pre> -execution-slots &lt;integer&gt;
   *  Number of attributes to evaluate in parallel.
   *  Default = 1 (i.e. no parallelism)</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    resetOptions();

    m_leaveOneOut = Utils.getFlag('L', options);
    String slots = Utils.getOption("execution-slots", options);
    if (slots.length() > 0) {
      m_executionSlots = Integer.parseInt(slots);
    }
    m_wrapperTemplate.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * returns the current setup.
   * 
   * @return the options of the current setup
   */
  @Override
  public String[] getOptions() {
    ArrayList<String> result;

    result = new ArrayList<String>();

    if (m_leaveOneOut) {
      result.add("-L");
    }

    result.add("-execution-slots");
    result.add("" + m_executionSlots);

    for (String o : m_wrapperTemplate.getOptions()) {
      result.add(o);
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String leaveOneAttributeOutTipText() {
    return "Evaluate an attribute by measuring the impact of leaving it "
      + "out from the full set instead of considering its worth in isolation.";
  }

  /**
   * Set whether to evaluate the merit of an attribute based on the impact of
   * leaving it out from the full set instead of considering its worth in
   * isolation
   * 
   * @param l true if each attribute should be evaluated by measuring the impact
   *          of leaving it out from the full set
   */
  public void setLeaveOneAttributeOut(boolean l) {
    m_leaveOneOut = l;
  }

  /**
   * Get whether to evaluate the merit of an attribute based on the impact of
   * leaving it out from the full set instead of considering its worth in
   * isolation
   * 
   * @return true if each attribute should be evaluated by measuring the impact
   *         of leaving it out from the full set
   */
  public boolean getLeaveOneAttributeOut() {
    return m_leaveOneOut;
  }

  /**
   * Tip text for this property.
   * 
   * @return the tip text for this property
   */
  public String numToEvaluateInParallelTipText() {
    return "The number of attributes to evaluate in parallel";
  }

  /**
   * Set the number of attributes to evaluate in parallel
   * 
   * @param n the number of attributes to evaluate in parallel
   */
  public void setNumToEvaluateInParallel(int n) {
    m_executionSlots = n;
  }

  /**
   * Get the number of attributes to evaluate in parallel
   * 
   * @return the number of attributes to evaluate in parallel
   */
  public int getNumToEvaluateInParallel() {
    return m_executionSlots;
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
    m_wrapperTemplate.setIRClassValue(val);
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
    return m_wrapperTemplate.getIRClassValue();
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
    return m_wrapperTemplate.getEvaluationMeasure();
  }

  /**
   * Sets the performance evaluation measure to use for selecting attributes for
   * the decision table
   * 
   * @param newMethod the new performance evaluation metric to use
   */
  public void setEvaluationMeasure(SelectedTag newMethod) {
    m_wrapperTemplate.setEvaluationMeasure(newMethod);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String thresholdTipText() {
    return m_wrapperTemplate.thresholdTipText();
  }

  /**
   * Set the value of the threshold for repeating cross validation
   * 
   * @param t the value of the threshold
   */
  public void setThreshold(double t) {
    m_wrapperTemplate.setThreshold(t);
  }

  /**
   * Get the value of the threshold
   * 
   * @return the threshold as a double
   */
  public double getThreshold() {
    return m_wrapperTemplate.getThreshold();
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String foldsTipText() {
    return m_wrapperTemplate.foldsTipText();
  }

  /**
   * Set the number of folds to use for accuracy estimation
   * 
   * @param f the number of folds
   */
  public void setFolds(int f) {
    m_wrapperTemplate.setFolds(f);
  }

  /**
   * Get the number of folds used for accuracy estimation
   * 
   * @return the number of folds
   */
  public int getFolds() {
    return m_wrapperTemplate.getFolds();
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String seedTipText() {
    return m_wrapperTemplate.seedTipText();
  }

  /**
   * Set the seed to use for cross validation
   * 
   * @param s the seed
   */
  public void setSeed(int s) {
    m_wrapperTemplate.setSeed(s);
  }

  /**
   * Get the random number seed used for cross validation
   * 
   * @return the seed
   */
  public int getSeed() {
    return m_wrapperTemplate.getSeed();
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String classifierTipText() {
    return m_wrapperTemplate.classifierTipText();
  }

  /**
   * Set the classifier to use for accuracy estimation
   * 
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {
    m_wrapperTemplate.setClassifier(newClassifier);
  }

  /**
   * Get the classifier used as the base learner.
   * 
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {
    return m_wrapperTemplate.getClassifier();
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

    result = m_wrapperTemplate.getClassifier().getCapabilities();
    result.setOwner(this);

    return result;
  }

  /**
   * Initializes a ClassifierAttribute attribute evaluator.
   * 
   * @param data set of instances serving as training data
   * @throws Exception if the evaluator has not been generated successfully
   */
  @Override
  public void buildEvaluator(final Instances data) throws Exception {
    // can evaluator handle data?
    getCapabilities().testWithFail(data);

    m_trainInstances = new Instances(data, 0);

    double baseMerit = 0;
    m_merit = new double[data.numAttributes()];
    m_pool = Executors.newFixedThreadPool(m_executionSlots);

    Set<Future<double[]>> results = new HashSet<Future<double[]>>();

    for (int i = -1; i < data.numAttributes(); i++) {
      if (i != data.classIndex()) {

        final int attIndex = i;
        Future<double[]> futureEval = m_pool.submit(new Callable<double[]>() {
          @Override
          public double[] call() throws Exception {
            double[] eval = new double[2];
            eval[0] = attIndex;
            WrapperSubsetEval evaluator = new WrapperSubsetEval();
            evaluator.setOptions(m_wrapperTemplate.getOptions());
            evaluator.buildEvaluator(data);
            if (m_wrapperSetup.length() == 0) {
              m_wrapperSetup = evaluator.toString();
            }
            BitSet b = new BitSet(data.numAttributes());
            if (m_leaveOneOut) {
              b.set(0, data.numAttributes());
              b.set(data.classIndex(), false);
            }
            if (attIndex >= 0) {
              b.set(attIndex, !m_leaveOneOut);
            }
            eval[1] = evaluator.evaluateSubset(b);

            return eval;
          }
        });

        results.add(futureEval);
      }
    }

    for (Future<double[]> f : results) {
      if (f.get()[0] != -1) {
        m_merit[(int) f.get()[0]] = f.get()[1];
      } else {
        baseMerit = f.get()[1];
      }
    }

    for (int i = 0; i < data.numAttributes(); i++) {
      m_merit[i] =
        m_leaveOneOut ? baseMerit - m_merit[i] : m_merit[i] - baseMerit;
    }

    m_pool.shutdown();
    m_trainInstances = new Instances(m_trainInstances, 0);
  }

  /**
   * Resets to defaults.
   */
  protected void resetOptions() {
    m_trainInstances = null;
    m_wrapperTemplate = new WrapperSubsetEval();
    m_wrapperSetup = "";
  }

  /**
   * Evaluates an individual attribute by measuring the amount of information
   * gained about the class given the attribute.
   * 
   * @param attribute the index of the attribute to be evaluated
   * @return the evaluation
   * @throws Exception if the attribute could not be evaluated
   */
  @Override
  public double evaluateAttribute(int attribute) throws Exception {
    return m_merit[attribute];
  }

  /**
   * Return a description of the evaluator.
   * 
   * @return description as a string
   */
  @Override
  public String toString() {
    StringBuffer text = new StringBuffer();

    if (m_trainInstances == null) {
      text.append("\tClassifier feature evaluator has not been built yet");
    } else {
      text.append("\tClassifier feature evaluator "
        + (m_leaveOneOut ? "(leave one out)" : "") + "\n\n");
      text.append("\tUsing ");

      text.append(m_wrapperSetup);
    }
    text.append("\n");

    return text.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 14195 $");
  }

  /**
   * Main method for executing this class.
   * 
   * @param args the options
   */
  public static void main(String[] args) {
    runEvaluator(new ClassifierAttributeEval(), args);
  }
}

