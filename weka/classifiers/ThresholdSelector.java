/*
 *    ThresholdSelector.java
 *    Copyright (C) 1999 Eibe Frank
 *
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
package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for selecting a threshold on a probability output by a
 * distribution classifier. The treshold is set so that a given
 * performance measure is optimized. Currently this is the
 * F-measure. Performance is measured on the training data, a hold-out
 * set or using cross-validation.<p>
 *
 * Valid options are:<p>
 *
 * -C integer <br>
 * The class for which treshold is determined. Based on
 * this designated class the problem is treated as a two-class
 * problem (default 0). <p>
 *
 * -D <br>
 * Turn on debugging output.<p>
 *
 * -W classname <br>
 * Specify the full class name of the base classifier. <p>
 *
 * -X num <br> 
 * Number of folds used for cross validation. If just a
 * hold-out set is used, this determines the size of the hold-out set
 * (default 10).<p>
 *
 * -S seed <br>
 * Random number seed (default 1).<p>
 *
 * -E integer <br>
 * Sets the evaluation mode. Use 0 for evaluation using cross-validation,
 * 1 for evaluation using hold-out set, and 2 for evaluation on the
 * training data (default 0).<p>
 *
 * Options after -- are passed to the designated sub-classifier. <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ 
 */
public class ThresholdSelector extends Classifier 
  implements OptionHandler {

  /** The evaluation modes */
  public final static int CROSS_VALIDATION = 0;
  public final static int TUNING_DATA = 1;
  public final static int TRAINING_DATA = 2;

  /** The generated base classifier */
  protected DistributionClassifier m_Classifier = 
    new weka.classifiers.ZeroR();

  /** The threshold that lead to the best performance */
  protected double m_BestThreshold = -Double.MAX_VALUE;

  /** The best value that has been observed */
  protected double m_BestValue = - Double.MAX_VALUE;
  
  /** The number of folds used in cross-validation */
  protected int m_NumFolds = 10;

  /** Random number seed */
  protected int m_Seed = 1;

  /** Designated class value */
  protected int m_DesignatedClass = 0;

  /** Debugging mode, gives extra output if true */
  protected boolean m_Debug;

  /** The data used for training */
  protected Instances m_Data;

  /** The data used for evaluation */
  protected Instances m_EvalData;

  /** The evaluation mode */
  protected int m_Mode = ThresholdSelector.CROSS_VALIDATION;

  /**
   * Collects the predicted probabilities.
   */
  protected double[] collectProbabilities() throws Exception {

    double[] probs = new double[m_EvalData.numInstances()];
    
    if ((m_Mode == ThresholdSelector.TRAINING_DATA) ||
	(m_Mode == ThresholdSelector.TUNING_DATA)) {
      m_Classifier.buildClassifier(m_Data);
      for (int i = 0; i < m_EvalData.numInstances(); i++) {
	Instance inst = m_EvalData.instance(i);
	probs[i] = 
	  m_Classifier.distributionForInstance(inst)[m_DesignatedClass];
      }
      return probs;
    } else {
      int index = 0;
      for (int j = 0; j < m_NumFolds; j++) {
	Instances train = m_EvalData.trainCV(m_NumFolds, j);
	m_Classifier.buildClassifier(train);
	Instances test = m_EvalData.testCV(m_NumFolds, j);
	for (int i = 0; i < test.numInstances(); i++) {
	  Instance inst = test.instance(i);
	  probs[index++] = 
	    m_Classifier.distributionForInstance(inst)[m_DesignatedClass];
	}
      }
      return probs;
    }
  }

  /**
   * Finds the best threshold.
   */
  protected void findThreshold() throws Exception {

    double[] probabilities = collectProbabilities();
    int[] sortedIndices = Utils.sortUnsafe(probabilities);

    double[][] confusionMatrix = new double[2][2];
    m_BestThreshold = -Double.MAX_VALUE;
    for (int i = 0; i < sortedIndices.length; i++) {
      Instance inst = m_EvalData.instance(sortedIndices[i]);
      if ((int) inst.classValue() == m_DesignatedClass) {
	confusionMatrix[0][0] += inst.weight();
      } else {
	confusionMatrix[1][0] += inst.weight();
      }
    }
    m_BestValue = computeValue(confusionMatrix);
    for (int i = 0; i < sortedIndices.length; i++) {
      Instance inst = m_EvalData.instance(sortedIndices[i]);
      if ((int) inst.classValue() == m_DesignatedClass) {
	confusionMatrix[0][0] -= inst.weight();
	confusionMatrix[0][1] += inst.weight();
      } else {
	confusionMatrix[1][0] -= inst.weight();
	confusionMatrix[1][1] += inst.weight();
      }
      if (m_Debug) {
	for (int j = 0; j < confusionMatrix.length; j++) {
	  for (int k = 0; k < confusionMatrix[j].length; k++) 
	    System.err.print(confusionMatrix[j][k] + " ");
	  System.err.println();
	}
	System.err.println();
      }
      if ((i + 1) == sortedIndices.length) {
	double curr = computeValue(confusionMatrix);
	if (m_Debug) {
	  System.err.println(probabilities[sortedIndices[i]] + " " + curr);
	}
	if (curr > m_BestValue) {
	  m_BestValue = curr;
	  m_BestThreshold = probabilities[sortedIndices[i]];
	}
      } else {
	double prob1 = probabilities[sortedIndices[i]];
	double prob2 = probabilities[sortedIndices[i + 1]];
	if (prob2 > prob1) {
	  double curr = computeValue(confusionMatrix);
	  if (m_Debug) {
	    System.err.println(prob1 + " " + curr);
	  }
	  if (curr > m_BestValue) {
	    m_BestValue = curr;
	    m_BestThreshold = prob1;
	  }
	}
      }
    }
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(6);

    newVector.addElement(new Option(
	      "\tThe class for which treshold is determined. Based on\n" +
	      "\tthis designated class the problem is treated as a two-class problem\n" +
	      "\t(default 0).\n",
	      "C", 1, "-C <integer>"));
    newVector.addElement(new Option(
	      "\tTurn on debugging output.",
	      "D", 0, "-D"));
    newVector.addElement(new Option(
	      "\tFull name of classifier to perform parameter selection on.\n"
	      + "\teg: weka.classifiers.NaiveBayes",
	      "W", 1, "-W <classifier class name>"));
    newVector.addElement(new Option(
	      "\tNumber of folds used for cross validation. If just a\n" +
	      "\thold-out set is used, this determines the size of the hold-out set\n" +
	      "\t(default 10).",
	      "X", 1, "-X <number of folds>"));
    newVector.addElement(new Option(
	      "\tSets the random number seed (default 1).",
	      "S", 1, "-S <random number seed>"));
    newVector.addElement(new Option(
	      "\tSets the evaluation mode. Use 0 for\n" +
	      "\tevaluation using cross-validation,\n" +
	      "\t1 for evaluation using hold-out set,\n" +
	      "\tand 2 for evaluation on the\n" +
	      "\ttraining data (default 0).",
	      "E", 1, "-E <integer>"));

    if ((m_Classifier != null) &&
	(m_Classifier instanceof OptionHandler)) {
      newVector.addElement(new Option("",
	        "", 0,
		"\nOptions specific to sub-classifier "
	        + m_Classifier.getClass().getName()
		+ ":\n(use -- to signal start of sub-classifier options)"));
      Enumeration enum = ((OptionHandler)m_Classifier).listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -C integer <br>
   * The class for which treshold is determined. Based on
   * this designated class the problem is treated as a two-class
   * problem (default 0).<p>
   *
   * -D <br>
   * Turn on debugging output.<p>
   *
   * -W classname <br>
   * Specify the full class name of classifier to perform cross-validation
   * selection on.<p>
   *
   * -X num <br> 
   * Number of folds used for cross validation. If just a
   * hold-out set is used, this determines the size of the hold-out set
   * (default 10).<p>
   *
   * -S seed <br>
   * Random number seed (default 1).<p>
   *
   * -E integer <br>
   * Sets the evaluation mode. Use 0 for evaluation using cross-validation,
   * 1 for evaluation using hold-out set, and 2 for evaluation on the
   * training data (default 0).<p>
   *
   * Options after -- are passed to the designated sub-classifier. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    setDebug(Utils.getFlag('D', options));

    String classString = Utils.getOption('C', options);
    if (classString.length() != 0) {
      m_DesignatedClass = Integer.parseInt(classString);
    } else {
      m_DesignatedClass = 0;
    }

    String foldsString = Utils.getOption('X', options);
    if (foldsString.length() != 0) {
      setNumFolds(Integer.parseInt(foldsString));
    } else {
      setNumFolds(10);
    }

    String randomString = Utils.getOption('S', options);
    if (randomString.length() != 0) {
      setSeed(Integer.parseInt(randomString));
    } else {
      setSeed(1);
    }

    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }

    String modeString = Utils.getOption('E', options);
    if (modeString.length() != 0) {
      m_Mode = Integer.parseInt(modeString);
    } else {
      m_Mode = 0;
    }

    setClassifier((DistributionClassifier)Classifier.
		  forName(classifierName,
			  Utils.partitionOptions(options)));
    if (!(m_Classifier instanceof OptionHandler)) {
      throw new Exception("Base classifier must accept options");
    }
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) && 
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }

    int current = 0;
    String [] options = new String [classifierOptions.length + 12];

    options[current++] = "-C"; options[current++] = "" + getClass();
    if (getDebug()) {
      options[current++] = "-D";
    }
    options[current++] = "-X"; options[current++] = "" + getNumFolds();
    options[current++] = "-S"; options[current++] = "" + getSeed();

    if (getClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getClassifier().getClass().getName();
    }
    options[current++] = "-E"; options[current++] = "" + getEvaluationMode();
    options[current++] = "--";

    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    Instances data;

    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    if (instances.numClasses() != 2) {
      throw new Exception("Only works for two-class datasets!");
    }
    instances = new Instances(instances);
    instances.deleteWithMissingClass();
    if (instances.numInstances() == 0) {
      throw new Exception("No training instances without missing class.");
    }
    if (instances.numInstances() < m_NumFolds) {
      throw new Exception("Number of training instances smaller than number of folds.");
    }
    switch (m_Mode) {
    case CROSS_VALIDATION: 
      data = new Instances(instances);
      data.randomize(new Random(m_Seed));
      data.stratify(m_NumFolds);
      m_Data = data;
      m_EvalData = data;
      findThreshold();
      m_Classifier.buildClassifier(instances);
      break;
    case TUNING_DATA:
      data = new Instances(instances);
      data.randomize(new Random(m_Seed));
      data.stratify(m_NumFolds);
      m_Data = instances.trainCV(m_NumFolds, 0);
      m_EvalData = instances.testCV(m_NumFolds, 0);
      findThreshold();
      m_Classifier.buildClassifier(instances);
      break;
    default:
      m_Data = instances;
      m_EvalData = instances;
      findThreshold();
    }
    m_Data = null; m_EvalData = null;
  }


  /**
   * Predicts the class value for the given test instance.
   *
   * @param instance the instance to be classified
   * @return the predicted class value
   * @exception Exception if an error occurred during the prediction
   */
  public double classifyInstance(Instance instance) throws Exception {
    
    double prob = 
      m_Classifier.distributionForInstance(instance)[m_DesignatedClass];
    if (prob > m_BestThreshold) {
      return m_DesignatedClass;
    } else {
      return (int) 1 - m_DesignatedClass;
    }
  }

  /**
   * Computes the value of the optimization criterion for the
   * given confusion matrix.
   */
  public double computeValue(double[][] table) {

    double precision = 0, recall = 0, fMeasure = 0;

    double predictedPos = (table[0][0] + table[1][0]);
    double truePos = (table[0][0] + table[0][1]);
    
    if (Utils.gr(predictedPos, 0)) {
      precision = table[0][0] / predictedPos;
    } 
    if (Utils.gr(truePos, 0)) {
      recall = table[0][0] / truePos;
    }
    if (Utils.gr(precision + recall, 0)) {
      fMeasure = (2 * precision * recall) / (precision + recall);
    }

    return fMeasure;
  }

  /**
   * Sets the evaluation mode.
   *
   * @param mode the evaluation mode
   */
  public void setEvaluationMode(int mode) {
    
    m_Mode = mode;
  }

  /**
   * Gets the evaluation mode.
   * 
   * @return the evaluation mode
   */
  public int getEvaluationMode() {

    return m_Mode;
  }
  
  /**
   * Sets the seed for random number generation.
   *
   * @param seed the random number seed
   */
  public void setSeed(int seed) {
    
    m_Seed = seed;
  }

  /**
   * Gets the random number seed.
   * 
   * @return the random number seed
   */
  public int getSeed() {

    return m_Seed;
  }

  /**
   * Sets debugging mode
   *
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {

    m_Debug = debug;
  }

  /**
   * Gets whether debugging is turned on
   *
   * @return true if debugging output is on
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Get the number of folds used for cross-validation.
   *
   * @return the number of folds used for cross-validation.
   */
  public int getNumFolds() {
    
    return m_NumFolds;
  }
  
  /**
   * Set the number of folds used for cross-validation.
   *
   * @param newNumFolds the number of folds used for cross-validation.
   */
  public void setNumFolds(int newNumFolds) {
    
    m_NumFolds = newNumFolds;
  }

  /**
   * Set the classifier for boosting. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(DistributionClassifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the classifier used as the classifier
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }

 
  /**
   * Returns description of the cross-validated classifier.
   *
   * @return description of the cross-validated classifier as a string
   */
  public String toString() {

    if (m_BestValue == -Double.MAX_VALUE)
      return "ThresholdSelector: No model built yet.";

    String result = "Threshold Selector.\n"
    + "Classifier: " + m_Classifier.getClass().getName() + "\n";

    result += "Index of designated class: " + m_DesignatedClass + "\n";

    result += "Evaluation mode: ";
    switch (m_Mode) {
    case ThresholdSelector.CROSS_VALIDATION:
      result += m_NumFolds + "-fold cross-validation";
      break;
    case ThresholdSelector.TUNING_DATA:
      result += "tuning on 1/" + m_NumFolds + " of the data";
      break;
    default:
      result += "tuning on the training data";
    }
    result += "\n";

    result += "Threshold: " + m_BestThreshold + "\n";
    result += "Best value: " + m_BestValue + "\n";

    result += m_Classifier.toString();
    return result;
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new ThresholdSelector(), 
						  argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}


  
