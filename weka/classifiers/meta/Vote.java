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
 *    Vote.java
 *    Copyright (C) 2000 Alexander K. Seewald
 *
 */

package weka.classifiers.meta;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.Evaluation;
import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;

/**
 * Class for combining classifiers using unweighted average of
 * probability estimates (classification) or numeric predictions 
 * (regression).
 *
 * Valid options from the command line are:<p>
 *
 * -B classifierstring <br>
 * Classifierstring should contain the full class name of a scheme
 * included for selection followed by options to the classifier
 * (required, option should be used once for each classifier).<p>
 *
 * @author Alexander K. Seewald (alex@seewald.at)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */

public class Vote extends Classifier implements OptionHandler {

  /** The list of classifiers */
  protected Classifier [] m_Classifiers = {
    new weka.classifiers.rules.ZeroR()
  };
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Class for combining classifiers using unweighted average of "
      + "probability estimates (classification) or numeric predictions "
      + "(regression).";
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
	      "\tFull class name of classifier to include, followed\n"
	      + "\tby scheme options. May be specified multiple times,\n"
	      + "\trequired at least twice.\n"
	      + "\teg: \"weka.classifiers.NaiveBayes -K\"",
	      "B", 1, "-B <classifier specification>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -B classifierstring <br>
   * Classifierstring should contain the full class name of a scheme
   * included for selection followed by options to the classifier
   * (required, option should be used once for each classifier).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

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

    String [] options = new String [0];
    int current = 0;

    if (m_Classifiers.length != 0) {
      options = new String [m_Classifiers.length * 2];
      for (int i = 0; i < m_Classifiers.length; i++) {
	options[current++] = "-B";
	options[current++] = "" + getClassifierSpec(i);
      }
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classifiersTipText() {
    return "The base classifiers to be used.";
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
   * Buildclassifier selects a classifier from the set of classifiers
   * by minimising error on the training data.
   *
   * @param data the training data to be used for generating the
   * boosted classifier.
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    if (m_Classifiers.length == 0) {
      throw new Exception("Vote: No base classifiers have been set!");
    }

    // Check for non-nominal classes
    if (!data.classAttribute().isNominal()) {
      throw new UnsupportedClassTypeException("Vote: Nominal class, please.");
    }

    Instances newData = new Instances(data);
    newData.deleteWithMissingClass();

    for (int i = 0; i < m_Classifiers.length; i++) {
      getClassifier(i).buildClassifier(data);
    }
  }

  /**
   * Classifies a given instance using the selected classifier.
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    double[] probs = getClassifier(0).distributionForInstance(instance);
    for (int i = 1; i < m_Classifiers.length; i++) {
      double[] dist = getClassifier(i).distributionForInstance(instance);
      for (int j = 0; j < dist.length; j++) {
	probs[j] += dist[j];
      }
    }
    for (int j = 0; j < probs.length; j++) {
      probs[j] /= (double)m_Classifiers.length;
    }
    return probs;
  }

  /**
   * Output a representation of this classifier
   */
  public String toString() {

    if (m_Classifiers == null) {
      return "Vote: No model built yet.";
    }

    String result = "Vote combines";
    result += " the probability distributions of these base learners:\n";
    for (int i = 0; i < m_Classifiers.length; i++) {
      result += '\t' + getClassifierSpec(i) + '\n';
    }

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
      System.out.println(Evaluation.evaluateModel(new Vote(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

}
