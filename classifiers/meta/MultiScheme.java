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
 *    MultiScheme.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Evaluation;
import weka.classifiers.DistributionClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;
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
 * -B classifierstring <br>
 * Classifierstring should contain the full class name of a scheme
 * included for selection followed by options to the classifier
 * (required, option should be used once for each classifier).<p>
 *
 * -X num_folds <br>
 * Use cross validation error as the basis for classifier selection.
 * (default 0, is to use error on the training data instead)<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.12 $
 */
public class MultiScheme extends DistributionClassifier implements OptionHandler {

  /** The classifier that had the best performance on training data. */
  protected DistributionClassifier m_Classifier;
 
  /** The list of classifiers */
  protected Classifier [] m_Classifiers = {
     new weka.classifiers.rules.ZeroR()
  };

  /** The index into the vector for the selected scheme */
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

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option(
	      "\tTurn on debugging output.",
	      "D", 0, "-D"));
    newVector.addElement(new Option(
	      "\tFull class name of classifier to include, followed\n"
	      + "\tby scheme options. May be specified multiple times,\n"
	      + "\trequired at least twice.\n"
	      + "\teg: \"weka.classifiers.bayes.NaiveBayes -D\"",
	      "B", 1, "-B <classifier specification>"));
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
   * -B classifierstring <br>
   * Classifierstring should contain the full class name of a scheme
   * included for selection followed by options to the classifier
   * (required, option should be used once for each classifier).<p>
   *
   * -X num_folds <br>
   * Use cross validation error as the basis for classifier selection.
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
    FastVector classifiers = new FastVector();
    while (true) {
      String classifierString = Utils.getOption('B', options);
      if (classifierString.length() == 0) {
	break;
      }
      String [] classifierSpec = Utils.splitOptions(classifierString);
      if (classifierSpec.length == 0) {
	throw new Exception("Invalid classifier specification string");
      }
      String classifierName = classifierSpec[0];
      classifierSpec[0] = "";
      classifiers.addElement(Classifier.forName(classifierName,
						classifierSpec));
    }
    if (classifiers.size() <= 1) {
      throw new Exception("At least two classifiers must be specified"
			  + " with the -B option.");
    } else {
      Classifier [] classifiersArray = new Classifier [classifiers.size()];
      for (int i = 0; i < classifiersArray.length; i++) {
	classifiersArray[i] = (Classifier) classifiers.elementAt(i);
      }
      setClassifiers(classifiersArray);
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

    if (m_Classifiers.length != 0) {
      options = new String [m_Classifiers.length * 2 + 5];
      for (int i = 0; i < m_Classifiers.length; i++) {
	options[current++] = "-B";
	options[current++] = "" + getClassifierSpec(i);
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
   * Sets the list of possible classifers to choose from.
   *
   * @param classifiers an array of classifiers with all options set.
   */
  public void setClassifiers(Classifier [] classifiers) {

    m_Classifiers = classifiers;
  }

  /**
   * Gets the list of possible classifers to choose from.
   *
   * @return the array of Classifiers
   */
  public Classifier [] getClassifiers() {

    return m_Classifiers;
  }
  
  /**
   * Gets a single classifier from the set of available classifiers.
   *
   * @param index the index of the classifier wanted
   * @return the Classifier
   */
  public Classifier getClassifier(int index) {

    return m_Classifiers[index];
  }
  
  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @param index the index of the classifier string to retrieve, starting from
   * 0.
   * @return the classifier string, or the empty string if no classifier
   * has been assigned (or the index given is out of range).
   */
  protected String getClassifierSpec(int index) {
    
    if (m_Classifiers.length < index) {
      return "";
    }
    Classifier c = getClassifier(index);
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
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

    if (m_Classifiers.length == 0) {
      throw new Exception("No base classifiers have been set!");
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
    int numClassifiers = m_Classifiers.length;
    for (int i = 0; i < numClassifiers; i++) {
      Classifier currentClassifier = getClassifier(i);
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
			   + " for classifier "
			   + currentClassifier.getClass().getName());
      }

      if ((i == 0) || (error < bestPerformance)) {
	bestClassifier = currentClassifier;
	bestPerformance = error;
	bestIndex = i;
      }
    }
    m_ClassifierIndex = bestIndex;
    if (m_NumXValFolds > 1) {
      bestClassifier.buildClassifier(newData);
    }
    if (!(bestClassifier instanceof DistributionClassifier)) {
      m_Classifier = new DistributionMetaClassifier();
      ((DistributionMetaClassifier)m_Classifier).setClassifier(bestClassifier);
    } else {
      m_Classifier = (DistributionClassifier)bestClassifier;
    }
  }

  /**
   * Returns class probabilities.
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    return m_Classifier.distributionForInstance(instance);
  }

  /**
   * Output a representation of this classifier
   */
  public String toString() {

    if (m_Classifier == null) {
      return "MultiScheme: No model built yet.";
    }

    String result = "MultiScheme selection using";
    if (m_NumXValFolds > 1) {
      result += " cross validation error";
    } else {
      result += " error on training data";
    }
    result += " from the following:\n";
    for (int i = 0; i < m_Classifiers.length; i++) {
      result += '\t' + getClassifierSpec(i) + '\n';
    }

    result += "Selected scheme: "
      + getClassifierSpec(m_ClassifierIndex)
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
