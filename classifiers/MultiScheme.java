/*
 *    MultiScheme.java
 *    Copyright (C) 1999 Len Trigg
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
 * Class for selecting a classifier from among several using cross 
 * validation on the training data.<p>
 *
 * Valid options from the command line are:<p>
 *
 * -D <br>
 * Turn on debugging output.<p>
 *
 * -S seed <br>
 * Random number seed (default 1).<p>
 *
 * -B learnerstring <br>
 * Learnerstring should contain the full class name of a scheme
 * included for selection followed by options to the learner
 * (required, option should be used once for each learner).<p>
 *
 * -X num_folds <br>
 * Use cross validation error as the basis for learner selection.
 * (default 0, is to use error on the training data instead)<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class MultiScheme extends Classifier implements OptionHandler {

  // ===================
  // Protected variables
  // ===================

  /** The classifier that had the best performance on training data. */
  protected Classifier m_Classifier;
 
  /** The list of classifier class names */
  protected FastVector m_ClassifierNames;

  /** The list of options for each classifier */
  protected FastVector m_ClassifierOptions;

  /** The index into the vector for the selected scheme (and options) */
  protected int m_ClassifierIndex;

  /**
   * Number of folds to use for cross validation (0 means use training
   * error for selection)
   */
  protected int m_NumXValFolds;

  /** Debugging mode, gives extra output if true */
  protected boolean m_Debug;

  /** Random number seed */
  protected int m_Seed = 1;

  // ===============
  // Public methods.
  // ===============

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option(
	      "\tTurn on debugging output.",
	      "D", 0, "-D"));
    newVector.addElement(new Option(
	      "\tFull class name of learner to include, followed\n"
	      + "\tby scheme options. May be specified multiple times,\n"
	      + "\trequired at least twice.\n"
	      + "\teg: \"weka.classifiers.NaiveBayes -D\"",
	      "B", 1, "-B <learner specification>"));
    newVector.addElement(new Option(
	      "\tSets the random number seed (default 1).",
	      "S", 1, "-S <random number seed>"));
    newVector.addElement(new Option(
	      "\tUse cross validation for model selection using the\n"
	      + "\tgiven number of folds. (default 0, is to\n"
	      + "\tuse training error)",
	      "X", 1, "-X <number of folds>"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Turn on debugging output.<p>
   *
   * -S seed <br>
   * Random number seed (default 1).<p>
   *
   * -B learnerstring <br>
   * Learnerstring should contain the full class name of a scheme
   * included for selection followed by options to the learner
   * (required, option should be used once for each learner).<p>
   *
   * -X num_folds <br>
   * Use cross validation error as the basis for learner selection.
   * (default 0, is to use error on the training data instead)<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setDebug(Utils.getFlag('D', options));
    
    String numFoldsString = Utils.getOption('X', options);
    if (numFoldsString.length() != 0) {
      setNumFolds(Integer.parseInt(numFoldsString));
    } else {
      setNumFolds(0);
    }
    
    String randomString = Utils.getOption('S', options);
    if (randomString.length() != 0) {
      setSeed(Integer.parseInt(randomString));
    } else {
      setSeed(1);
    }

    // Iterate through the schemes
    m_ClassifierNames = null;
    m_ClassifierOptions = null;
    while (true) {
      String learnerString = Utils.getOption('B', options);
      if (learnerString.length() == 0) {
	break;
      }
      addLearner(learnerString);
    }
    if ((m_ClassifierNames == null) || (m_ClassifierNames.size() <= 1)) {
      throw new Exception("At least two learners must be specified"
			  + " with the -B option.");
    }
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [5];
    int current = 0;

    if (m_ClassifierNames != null) {
      options = new String [m_ClassifierNames.size() * 2 + 5];
      for (int i = 0; i < m_ClassifierNames.size(); i++) {
	options[current++] = "-B"; options[current++] = "" + getLearner(i);
      }
    }
    if (getNumFolds() > 1) {
      options[current++] = "-X"; options[current++] = "" + getNumFolds();
    }
    options[current++] = "-S"; options[current++] = "" + getSeed();
    if (getDebug()) {
      options[current++] = "-D";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Add a learner to the set of learners.
   *
   * @param learnerString a string consisting of the class name of a classifier
   * followed by any required learner options
   * @exception Exception if the learner class name is not valid or the 
   * classifier does not accept the supplied options
   */
  public void addLearner(String learnerString) throws Exception {

    // Split the learner String into classname and options
    learnerString = learnerString.trim();
    int breakLoc = learnerString.indexOf(' ');
    String learnerName = learnerString;
    String learnerOptions = "";
    if (breakLoc != -1) {
      learnerName = learnerString.substring(0, breakLoc);
      learnerOptions = learnerString.substring(breakLoc).trim();
    }
    Classifier tempClassifier;
    try {
      tempClassifier = (Classifier)Class.forName(learnerName).newInstance();
    } catch (Exception ex) {
      throw new Exception("Can't find Classifier with class name: "
			  + learnerName);
    }
    if (tempClassifier instanceof OptionHandler) {
      String [] options = Utils.splitOptions(learnerOptions);
      ((OptionHandler)tempClassifier).setOptions(options);
    }
    // Everything good, so add the learner to the set.
    if (m_ClassifierNames == null) {
      m_ClassifierNames = new FastVector();
      m_ClassifierOptions = new FastVector();
    }
    m_ClassifierNames.addElement(learnerName);
    m_ClassifierOptions.addElement(learnerOptions);
 }

  /**
   * Gets the learner string, which contains the class name of
   * the learner and any options to the learner
   *
   * @param index the index of the learner string to retrieve, starting from
   * 0.
   * @return the learner string, or the empty string if no learner
   * has been assigned (or the index given is out of range).
   */
  public String getLearner(int index) {
    
    if ((m_ClassifierNames == null) 
	|| (m_ClassifierNames.size() < index)) {
      return "";
    }
    if ((m_ClassifierOptions == null) 
	|| (m_ClassifierOptions.size() < index)
	|| ((String)m_ClassifierOptions.elementAt(index)).equals("")) {
      return (String)m_ClassifierNames.elementAt(index);
    }
    return (String)m_ClassifierNames.elementAt(index) 
      + " " + (String)m_ClassifierOptions.elementAt(index);
  }

  /**
   * Sets the seed for random number generation.
   *
   * @param seed the random number seed
   */
  public void setSeed(int seed) {
    
    m_Seed = seed;;
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
   * Gets the number of folds for cross-validation. A number less
   * than 2 specifies using training error rather than cross-validation.
   *
   * @return the number of folds for cross-validation
   */
  public int getNumFolds() {

    return m_NumXValFolds;
  }

  /**
   * Sets the number of folds for cross-validation. A number less
   * than 2 specifies using training error rather than cross-validation.
   *
   * @param numFolds the number of folds for cross-validation
   */
  public void setNumFolds(int numFolds) {
    
    m_NumXValFolds = numFolds;
  }

  /**
   * Set debugging mode
   *
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {

    m_Debug = debug;
  }

  /**
   * Get whether debugging is turned on
   *
   * @return true if debugging output is on
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Buildclassifier selects a classifier from the set of classifiers
   * by minimising error on the training data.
   *
   * @param data the training data to be used for generating the
   * boosted classifier.
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    if (m_ClassifierNames == null) {
      throw new Exception("No base learners have been set!");
    }
    Instances newData = new Instances(data);
    newData.deleteWithMissingClass();
    newData.randomize(new Random(m_Seed));
    if (newData.classAttribute().isNominal() && (m_NumXValFolds > 1))
      newData.stratify(m_NumXValFolds);
    Instances train = newData;               // train on all data by default
    Instances test = newData;               // test on training data by default
    Classifier bestClassifier = null;
    int bestIndex = -1;
    double bestPerformance = Double.NaN;
    int numClassifiers = m_ClassifierNames.size();
    for (int i = 0; i < numClassifiers; i++) {
      // Instantiate the classifier
      String learnerName = (String)m_ClassifierNames.elementAt(i);
      Classifier currentClassifier = (Classifier)Class.forName(learnerName)
	.newInstance();
      if (currentClassifier instanceof OptionHandler) {
	String [] options = Utils.splitOptions((String)m_ClassifierOptions
					 .elementAt(i));
	((OptionHandler)currentClassifier).setOptions(options);
      }

      Evaluation evaluation;
      if (m_NumXValFolds > 1) {
	evaluation = new Evaluation(newData);
	for (int j = 0; j < m_NumXValFolds; j++) {
	  train = newData.trainCV(m_NumXValFolds, j);
	  test = newData.testCV(m_NumXValFolds, j);
	  currentClassifier.buildClassifier(train);
	  evaluation.setPriors(train);
	  evaluation.evaluateModel(currentClassifier, test);
	}
      } else {
	currentClassifier.buildClassifier(train);
	evaluation = new Evaluation(train);
	evaluation.evaluateModel(currentClassifier, test);
      }

      double error = evaluation.errorRate();
      if (m_Debug) {
	System.err.println("Error rate: " + Utils.doubleToString(error, 6, 4)
			   + " for classifier " + learnerName);
      }

      if ((i == 0) || (error < bestPerformance)) {
	bestClassifier = currentClassifier;
	bestPerformance = error;
	bestIndex = i;
      }
    }
    m_ClassifierIndex = bestIndex;
    m_Classifier = bestClassifier;
    if (m_NumXValFolds > 1) {
      m_Classifier.buildClassifier(newData);
    }
  }

  /**
   * Classifies a given instance using the selected classifier.
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double classifyInstance(Instance instance) throws Exception {

    return m_Classifier.classifyInstance(instance);
  }

  /**
   * Output a representation of this classifier
   */
  public String toString() {

    if ((m_ClassifierNames == null) || (m_ClassifierNames.size() == 0)) {
      return "MultiScheme: No schemes entered for selection";
    }

    String result = "MultiScheme selection using";
    if (m_NumXValFolds > 1) {
      result += " cross validation error";
    } else {
      result += " error on training data";
    }
    result += " from the following:\n";
    for (int i = 0; i < m_ClassifierNames.size(); i++) {
      result += '\t' + (String)m_ClassifierNames.elementAt(i)
      + ' ' + (String)m_ClassifierOptions.elementAt(i) + '\n';
    }

    if (m_Classifier == null) {
      return result + "No scheme selected yet";
    }
    result += "Selected scheme: "
      + (String)m_ClassifierNames.elementAt(m_ClassifierIndex)
      + ' ' 
      + (String)m_ClassifierOptions.elementAt(m_ClassifierIndex)
      + "\n\n"
      + m_Classifier.toString();
    return result;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new MultiScheme(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

}
