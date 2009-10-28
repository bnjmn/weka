/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    ClassifierAttributeEval.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.attributeSelection;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.rules.OneR;
import weka.core.Capabilities;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

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
 * 
 * <pre> -S &lt;seed&gt;
 *  Random number seed for cross validation.
 *  (default = 1)</pre>
 * 
 * <pre> -F &lt;folds&gt;
 *  Number of folds for cross validation.
 *  (default = 10)</pre>
 * 
 * <pre> -D
 *  Use training data for evaluation rather than cross validaton.</pre>
 * 
 * <pre> -B &lt;classname + options&gt;
 *  Classifier to use.
 *  (default = OneR)</pre>
 * 
 <!-- options-end -->
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class ClassifierAttributeEval
  extends ASEvaluation
  implements AttributeEvaluator, OptionHandler {
  
  /** for serialization. */
  private static final long serialVersionUID = 2442390690522602284L;

  /** The training instances. */
  protected Instances m_trainInstances;

  /** Random number seed. */
  protected int m_randomSeed;

  /** Number of folds for cross validation. */
  protected int m_folds;

  /** Use training data to evaluate merit rather than x-val. */
  protected boolean m_evalUsingTrainingData;

  /** The classifier to use for evaluating the attribute. */
  protected Classifier m_Classifier;

  /**
   * Constructor.
   */
  public ClassifierAttributeEval () {
    resetOptions();
  }
  
  /**
   * Returns a string describing this attribute evaluator.
   * 
   * @return 		a description of the evaluator suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "ClassifierAttributeEval :\n\nEvaluates the worth of an attribute by "
      +"using a user-specified classifier.\n";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return 		an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();

    result.addElement(new Option(
        "\tRandom number seed for cross validation.\n"
        + "\t(default = 1)",
        "S", 1, "-S <seed>"));

    result.addElement(new Option(
        "\tNumber of folds for cross validation.\n"
        + "\t(default = 10)",
        "F", 1, "-F <folds>"));

    result.addElement(new Option(
        "\tUse training data for evaluation rather than cross validaton.",
        "D", 0, "-D"));

    result.addElement(new Option(
        "\tClassifier to use.\n"
        + "\t(default = OneR)",
        "B", 1, "-B <classname + options>"));

    return result.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -S &lt;seed&gt;
   *  Random number seed for cross validation.
   *  (default = 1)</pre>
   * 
   * <pre> -F &lt;folds&gt;
   *  Number of folds for cross validation.
   *  (default = 10)</pre>
   * 
   * <pre> -D
   *  Use training data for evaluation rather than cross validaton.</pre>
   * 
   * <pre> -B &lt;classname + options&gt;
   *  Classifier to use.
   *  (default = OneR)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String [] options) throws Exception {
    String 	tmpStr;
    String[]	tmpOptions;
    
    tmpStr = Utils.getOption('S', options);
    if (tmpStr.length() != 0)
      setSeed(Integer.parseInt(tmpStr));
    
    tmpStr = Utils.getOption('F', options);
    if (tmpStr.length() != 0)
      setFolds(Integer.parseInt(tmpStr));

    tmpStr = Utils.getOption('B', options);
    if (tmpStr.length() != 0) {
      tmpOptions    = Utils.splitOptions(tmpStr);
      tmpStr        = tmpOptions[0];
      tmpOptions[0] = "";
      setClassifier((Classifier) Utils.forName(Classifier.class, tmpStr, tmpOptions));
    }
    
    setEvalUsingTrainingData(Utils.getFlag('D', options));
    Utils.checkForRemainingOptions(options);
  }

  /**
   * returns the current setup.
   * 
   * @return the options of the current setup
   */
  public String[] getOptions() {
    Vector<String>	result;
    
    result = new Vector<String>();
    
    if (getEvalUsingTrainingData())
      result.add("-D");
    
    result.add("-S");
    result.add("" + getSeed());
    
    result.add("-F");
    result.add("" + getFolds());
    
    result.add("-B");
    result.add(
	new String(
	    m_Classifier.getClass().getName() + " " 
	    + Utils.joinOptions(((OptionHandler)m_Classifier).getOptions())).trim());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Set the random number seed for cross validation.
   *
   * @param value 	the seed to use
   */
  public void setSeed(int value) {
    m_randomSeed = value;
  }

  /**
   * Get the random number seed.
   *
   * @return 		an <code>int</code> value
   */
  public int getSeed() {
    return m_randomSeed;
  }

  /**
   * Returns a string for this option suitable for display in the gui
   * as a tip text.
   *
   * @return 		a string describing this option
   */
  public String seedTipText() {
    return "Set the seed for use in cross validation.";
  }

  /**
   * Set the number of folds to use for cross validation.
   *
   * @param value 	the number of folds
   */
  public void setFolds(int value) {
    m_folds = value;
    if (m_folds < 2)
      m_folds = 2;
  }
   
  /**
   * Get the number of folds used for cross validation.
   *
   * @return 		the number of folds
   */
  public int getFolds() {
    return m_folds;
  }

  /**
   * Returns a string for this option suitable for display in the gui
   * as a tip text.
   *
   * @return 		a string describing this option
   */
  public String foldsTipText() {
    return "Set the number of folds for cross validation.";
  }

  /**
   * Use the training data to evaluate attributes rather than cross validation.
   *
   * @param value 	true if training data is to be used for evaluation
   */
  public void setEvalUsingTrainingData(boolean value) {
    m_evalUsingTrainingData = value;
  }

  /**
   * Returns true if the training data is to be used for evaluation.
   *
   * @return 		true if training data is to be used for evaluation
   */
  public boolean getEvalUsingTrainingData() {
    return m_evalUsingTrainingData;
  }

  /**
   * Returns a string for this option suitable for display in the gui
   * as a tip text.
   *
   * @return 		a string describing this option
   */
  public String evalUsingTrainingDataTipText() {
    return "Use the training data to evaluate attributes rather than "
      + "cross validation.";
  }

  /**
   * Set the classifier to use for evaluating the attribute.
   *
   * @param value	the classifier to use
   */
  public void setClassifier(Classifier value) {
    m_Classifier = value;
  }

  /**
   * Returns the classifier to use for evaluating the attribute.
   *
   * @return 		the classifier in use
   */
  public Classifier getClassifier() {
    return m_Classifier;
  }

  /**
   * Returns a string for this option suitable for display in the gui
   * as a tip text.
   *
   * @return 		a string describing this option
   */
  public String classifierTipText() {
    return "The classifier to use for evaluating the attribute.";
  }

  /**
   * Returns the capabilities of this evaluator.
   *
   * @return            the capabilities of this evaluator
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities 	result;
    
    if (m_Classifier != null) {
      result = m_Classifier.getCapabilities();
      result.setOwner(this);
    }
    else {
      result = super.getCapabilities();
      result.disableAll();
    }
    
    return result;
  }

  /**
   * Initializes a ClassifierAttribute attribute evaluator.
   *
   * @param data 	set of instances serving as training data 
   * @throws Exception 	if the evaluator has not been generated successfully
   */
  public void buildEvaluator (Instances data) throws Exception {
    // can evaluator handle data?
    getCapabilities().testWithFail(data);

    m_trainInstances = data;
  }


  /**
   * Resets to defaults.
   */
  protected void resetOptions () {
    m_trainInstances        = null;
    m_randomSeed            = 1;
    m_folds                 = 10;
    m_evalUsingTrainingData = false;
    m_Classifier            = new OneR();
  }


  /**
   * Evaluates an individual attribute by measuring the amount
   * of information gained about the class given the attribute.
   *
   * @param attribute 	the index of the attribute to be evaluated
   * @return		the evaluation
   * @throws Exception 	if the attribute could not be evaluated
   */
  public double evaluateAttribute(int attribute) throws Exception {
    int[] 	featArray; 
    double 	errorRate;
    Evaluation 	eval;
    Remove 	delTransform;
    Instances 	train;
    Classifier 	cls;

    // create tmp dataset
    featArray    = new int[2]; // feat + class
    delTransform = new Remove();
    delTransform.setInvertSelection(true);
    train        = new Instances(m_trainInstances);
    featArray[0] = attribute;
    featArray[1] = train.classIndex();
    delTransform.setAttributeIndicesArray(featArray);
    delTransform.setInputFormat(train);
    train = Filter.useFilter(train, delTransform);
    
    // evaluate classifier
    eval = new Evaluation(train);
    cls  = AbstractClassifier.makeCopy(m_Classifier);
    if (m_evalUsingTrainingData) {
      cls.buildClassifier(train);
      eval.evaluateModel(cls, train);
    }
    else {
      eval.crossValidateModel(cls, train, m_folds, new Random(m_randomSeed));
    }
    errorRate = eval.errorRate();
    
    return (1 - errorRate)*100.0;
  }

  /**
   * Return a description of the evaluator.
   * 
   * @return 		description as a string
   */
  public String toString () {
    StringBuffer text = new StringBuffer();

    if (m_trainInstances == null) {
      text.append("\tClassifier feature evaluator has not been built yet");
    }
    else {
      text.append("\tClassifier feature evaluator.\n\n");
      text.append("\tUsing ");
      if (m_evalUsingTrainingData)
        text.append("training data for evaluation of attributes.\n");
      else
        text.append(getFolds()+ " fold cross validation for evaluating attributes.\n");
      text.append("\tClassifier in use: " + m_Classifier.getClass().getName() + " " + Utils.joinOptions(((OptionHandler)m_Classifier).getOptions()));
    }
    text.append("\n");
    
    return text.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for executing this class.
   *
   * @param args 	the options
   */
  public static void main (String[] args) {
    runEvaluator(new ClassifierAttributeEval(), args);
  }
}
