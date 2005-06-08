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
 *    OneRAttributeEval.java
 *    Copyright (C) 1999 Mark Hall
 *
 */

package  weka.attributeSelection;

import  java.util.*;
import  weka.core.*;
import  weka.classifiers.*;
import  weka.filters.unsupervised.attribute.Remove;
import  weka.filters.Filter;

/** 
 * Class for Evaluating attributes individually by using the OneR
 * classifier. <p>
 *
 * -S <seed> <br>
 * Set the seed for cross validation (default = 1). <p>
 *
 * -F <folds> <br>
 * Set the number of folds for cross validation (default = 10). <p>
 * 
 * -B <minimum bucket size> <br>
 * Set the minimum number of objects per bucket (passed on to
 * OneR, default = 6). <p>
 *
 * -D <br>
 * Use the training data to evaluate attributes rather than cross validation. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.15 $
 */
public class OneRAttributeEval
  extends AttributeEvaluator implements OptionHandler
{

  /** The training instances */
  private Instances m_trainInstances;

  /** The class index */
  private int m_classIndex;

  /** The number of attributes */
  private int m_numAttribs;

  /** The number of instances */
  private int m_numInstances;

  /** Random number seed */
  private int m_randomSeed;

  /** Number of folds for cross validation */
  private int m_folds;

  /** Use training data to evaluate merit rather than x-val */
  private boolean m_evalUsingTrainingData;

  /** Passed on to OneR */
  private int m_minBucketSize;


  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "OneRAttributeEval :\n\nEvaluates the worth of an attribute by "
      +"using the OneR classifier.\n";
  }

  /**
   * Returns a string for this option suitable for display in the gui
   * as a tip text
   *
   * @return a string describing this option
   */
  public String seedTipText() {
    return "Set the seed for use in cross validation.";
  }

  /**
   * Set the random number seed for cross validation
   *
   * @param seed the seed to use
   */
  public void setSeed(int seed) {
    m_randomSeed = seed;
  }

  /**
   * Get the random number seed
   *
   * @return an <code>int</code> value
   */
  public int getSeed() {
    return m_randomSeed;
  }

  /**
   * Returns a string for this option suitable for display in the gui
   * as a tip text
   *
   * @return a string describing this option
   */
  public String foldsTipText() {
    return "Set the number of folds for cross validation.";
  }

  /**
   * Set the number of folds to use for cross validation
   *
   * @param folds the number of folds
   */
  public void setFolds(int folds) {
    m_folds = folds;
    if (m_folds < 2) {
      m_folds = 2;
    }
  }
   
  /**
   * Get the number of folds used for cross validation
   *
   * @return the number of folds
   */
  public int getFolds() {
    return m_folds;
  }

  /**
   * Returns a string for this option suitable for display in the gui
   * as a tip text
   *
   * @return a string describing this option
   */
  public String evalUsingTrainingDataTipText() {
    return "Use the training data to evaluate attributes rather than "
      + "cross validation.";
  }

  /**
   * Use the training data to evaluate attributes rather than cross validation
   *
   * @param e true if training data is to be used for evaluation
   */
  public void setEvalUsingTrainingData(boolean e) {
    m_evalUsingTrainingData = e;
  }

  /**
   * Returns a string for this option suitable for display in the gui
   * as a tip text
   *
   * @return a string describing this option
   */
  public String minimumBucketSizeTipText() {
    return "The minimum number of objects in a bucket "
      + "(passed to OneR).";
  }

  /**
   * Set the minumum bucket size used by OneR
   *
   * @param minB the minimum bucket size to use
   */
  public void setMinimumBucketSize(int minB) {
    m_minBucketSize = minB;
  }

  /**
   * Get the minimum bucket size used by oneR
   *
   * @return the minimum bucket size used
   */
  public int getMinimumBucketSize() {
    return m_minBucketSize;
  }

  /**
   * Returns true if the training data is to be used for evaluation
   *
   * @return true if training data is to be used for evaluation
   */
  public boolean getEvalUsingTrainingData() {
    return m_evalUsingTrainingData;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option(
			 "\tRandom number seed for cross validation (default = 1)",
			 "S", 1, "-S <seed>"));

    newVector.addElement(new Option(
			 "\tNumber of folds for cross validation (default = 10)",
			 "F", 1, "-F <folds>"));

    newVector.addElement(new Option(
		   "\tUse training data for evaluation rather than cross validaton",
		   "D", 0, "-D"));

    newVector.addElement(new Option(
			 "\tMinimum number of objects in a bucket (passed on to "
			 +"OneR, default = 6)",
			 "B", 1, "-B <minimum bucket size>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -S <seed> <br>
   * Set the seed for cross validation (default = 1). <p>
   *
   * -F <folds> <br>
   * Set the number of folds for cross validation (default = 10). <p>
   * 
   * -B <minimum bucket size> <br>
   * Set the minimum number of objects per bucket (passed on to
   * OneR, default = 6). <p>
   *
   * -D <br>
   * Use the training data to evaluate attributes rather than cross validation. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String [] options) throws Exception {
    String temp = Utils.getOption('S', options);

    if (temp.length() != 0) {
      setSeed(Integer.parseInt(temp));
    }
    
    temp = Utils.getOption('F', options);
    if (temp.length() != 0) {
      setFolds(Integer.parseInt(temp));
    }

    temp = Utils.getOption('B', options);
    if (temp.length() != 0) {
      setMinimumBucketSize(Integer.parseInt(temp));
    }
    
    setEvalUsingTrainingData(Utils.getFlag('D', options));
    Utils.checkForRemainingOptions(options);
  }

  public String []  getOptions() {
    String [] options = new String [7];
    int current = 0;
    
    if (getEvalUsingTrainingData()) {
      options[current++] = "-D";
    }
    
    options[current++] = "-S";
    options[current++] = "" + getSeed();
    options[current++] = "-F";
    options[current++] = "" + getFolds();
    options[current++] = "-B";
    options[current++] = "" + getMinimumBucketSize();

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Constructor
   */
  public OneRAttributeEval () {
    resetOptions();
  }


  /**
   * Initializes a OneRAttribute attribute evaluator.
   * Discretizes all attributes that are numeric.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator (Instances data)
    throws Exception {
    m_trainInstances = data;

    if (m_trainInstances.checkForStringAttributes()) {
      throw  new UnsupportedAttributeTypeException("Can't handle string attributes!");
    }

    m_classIndex = m_trainInstances.classIndex();
    m_numAttribs = m_trainInstances.numAttributes();
    m_numInstances = m_trainInstances.numInstances();

    if (m_trainInstances.attribute(m_classIndex).isNumeric()) {
      throw  new Exception("Class must be nominal!");
    }
  }


  /**
   * rests to defaults.
   */
  protected void resetOptions () {
    m_trainInstances = null;
    m_randomSeed = 1;
    m_folds = 10;
    m_evalUsingTrainingData = false;
    m_minBucketSize = 6; // default used by OneR
  }


  /**
   * evaluates an individual attribute by measuring the amount
   * of information gained about the class given the attribute.
   *
   * @param attribute the index of the attribute to be evaluated
   * @exception Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute (int attribute)
    throws Exception {
    int[] featArray = new int[2]; // feat + class
    double errorRate;
    Evaluation o_Evaluation;
    Remove delTransform = new Remove();
    delTransform.setInvertSelection(true);
    // copy the instances
    Instances trainCopy = new Instances(m_trainInstances);
    featArray[0] = attribute;
    featArray[1] = trainCopy.classIndex();
    delTransform.setAttributeIndicesArray(featArray);
    delTransform.setInputFormat(trainCopy);
    trainCopy = Filter.useFilter(trainCopy, delTransform);
    o_Evaluation = new Evaluation(trainCopy);
    String [] oneROpts = { "-B", ""+getMinimumBucketSize()};
    Classifier oneR = Classifier.forName("weka.classifiers.rules.OneR", oneROpts);
    if (m_evalUsingTrainingData) {
      oneR.buildClassifier(trainCopy);
      o_Evaluation.evaluateModel(oneR, trainCopy);
    } else {
      /*      o_Evaluation.crossValidateModel("weka.classifiers.rules.OneR", 
	      trainCopy, 10, 
	      null, new Random(m_randomSeed)); */
      o_Evaluation.crossValidateModel(oneR, trainCopy, m_folds, new Random(m_randomSeed));
    }
    errorRate = o_Evaluation.errorRate();
    return  (1 - errorRate)*100.0;
  }


  /**
   * Return a description of the evaluator
   * @return description as a string
   */
  public String toString () {
    StringBuffer text = new StringBuffer();

    if (m_trainInstances == null) {
      text.append("\tOneR feature evaluator has not been built yet");
    }
    else {
      text.append("\tOneR feature evaluator.\n\n");
      text.append("\tUsing ");
      if (m_evalUsingTrainingData) {
	text.append("training data for evaluation of attributes.");
      } else {
	text.append(""+getFolds()+" fold cross validation for evaluating "
		    +"attributes.");
      }
      text.append("\n\tMinimum bucket size for OneR: "
		  +getMinimumBucketSize());
    }

    text.append("\n");
    return  text.toString();
  }


  // ============
  // Test method.
  // ============
  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {
    try {
      System.out.println(AttributeSelection.
			 SelectAttributes(new OneRAttributeEval(), args));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

}

