/*
 *    Stacking.java
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
 * Implements stacking. For more information, see<p>
 *
 * David H. Wolpert (1992). <i>Stacked
 * generalization</i>. Neural Networks, 5:241-259, Pergamon Press. <p>
 *
 * Valid options are:<p>
 *
 * -X num_folds <br>
 * The number of folds for the cross-validation (default 10).<p>
 *
 * -S seed <br>
 * Random number seed (default 1).<p>
 *
 * -B learnerstring <br>
 * Learnerstring should contain the full class name of a base scheme
 * followed by options to the learner.
 * (required, option should be used once for each learner).<p>
 *
 * -M learnerstring <br>
 * Learnerstring for the meta learner. Same format as for base learners.
 * (required) <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $ */
public class Stacking extends Classifier implements OptionHandler {

  /** The meta classifier. */
  protected Classifier m_MetaClassifier = null;

  /** The name of the meta classifier. */
  protected String m_MetaClassifierName = null;

  /** The options for the meta classifier. */
  protected String m_MetaClassifierOptions = null;

  /** The base classifiers. */
  protected FastVector m_BaseClassifiers = null;
 
  /** The list of base classifier class names */
  protected FastVector m_BaseClassifierNames = null;

  /** The list of options for the base classifiers */
  protected FastVector m_BaseClassifierOptions = null;

  /** Format for meta data */
  protected Instances m_MetaFormat = null;

  /** Format for base data */
  protected Instances m_BaseFormat = null;

  /** Set the number of folds for the cross-validation */
  protected int m_NumFolds = 10;

  /** Random number seed */
  protected int m_Seed = 1;

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);
    newVector.addElement(new Option(
	      "\tFull class name of base learners to include, followed "
	      + "by scheme options\n"
	      + "\t(may be specified multiple times).\n"
	      + "\teg: \"weka.classifiers.NaiveBayes -K\"",
	      "B", 1, "-B"));
    newVector.addElement(new Option(
	      "\tFull name of meta learner, followed by options.",
	      "M", 0, "-M"));
    newVector.addElement(new Option(
	      "\tSets the number of cross-validation folds.",
	      "X", 1, "-X"));
    newVector.addElement(new Option(
	      "\tSets the random number seed.",
	      "S", 1, "-S"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -X num_folds <br>
   * The number of folds for the cross-validation (default 10).<p>
   *
   * -S seed <br>
   * Random number seed (default 1).<p>
   *
   * -B learnerstring <br>
   * Learnerstring should contain the full class name of a base scheme
   * followed by options to the learner.
   * (required, option should be used once for each learner).<p>
   *
   * -M learnerstring <br>
   * Learnerstring for the meta learner. Same format as for base learners.
   * (required) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String numFoldsString = Utils.getOption('X', options);
    if (numFoldsString.length() != 0) {
      setNumFolds(Integer.parseInt(numFoldsString));
    } else {
      setNumFolds(10);
    }
    String randomString = Utils.getOption('S', options);
    if (randomString.length() != 0) {
      setSeed(Integer.parseInt(randomString));
    } else {
      setSeed(1);
    }

    // Iterate through the schemes
    m_BaseClassifierNames = null;
    m_BaseClassifierOptions = null;
    while (true) {
      String learnerString = Utils.getOption('B', options);
      if (learnerString.length() == 0) {
	break;
      }
      addLearner(learnerString);
    }
    if ((m_BaseClassifierNames == null) 
	|| (m_BaseClassifierNames.size() == 0)) {
      throw new Exception("At least one base learner must be specified"
			  + " with the -B option.");
    }
   String  metaLearnerString = Utils.getOption('M', options);
    if (metaLearnerString.length() == 0) {
      throw new Exception("Meta learner has to be provided.");
    }
    addMetaLearner(metaLearnerString);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options;
    int current = 0;

    if (m_BaseClassifierNames != null) {
      options = new String [m_BaseClassifierNames.size() * 2 + 6];
      for (int i = 0; i < m_BaseClassifierNames.size(); i++) {
	options[current++] = "-B"; options[current++] = "" + getLearner(i);
      }
    } else {
      options = new String[6];
    }
    options[current++] = "-X"; options[current++] = "" + getNumFolds();
    options[current++] = "-S"; options[current++] = "" + getSeed();
    if (!getMetaLearner().equals("")) {
      options[current++] = "-M"; options[current++] = getMetaLearner();
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
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
   * Gets the number of folds for the cross-validation.
   *
   * @return the number of folds for the cross-validation
   */
  public int getNumFolds() {

    return m_NumFolds;
  }

  /**
   * Sets the number of folds for the cross-validation.
   *
   * @param numFolds the number of folds for the cross-validation
   * @exception Exception if parameter illegal
   */
  public void setNumFolds(int numFolds) throws Exception {
    
    if (numFolds < 0) {
      throw new Exception("Stacking: Number of cross-validation " +
			  "folds must be positive.");
    }
    m_NumFolds = numFolds;
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
    if (m_BaseClassifierNames == null) {
      m_BaseClassifierNames = new FastVector();
      m_BaseClassifierOptions = new FastVector();
    }
    m_BaseClassifierNames.addElement(learnerName);
    m_BaseClassifierOptions.addElement(learnerOptions);
 }

  /**
   * Gets the learner string, which contains the class name of
   * the learner and any options to the learner
   *
   * @param index the learner string to retrieve
   * @return the learner string, or the empty string if no learner
   * has been assigned (or the index given is out of range).
   */
  public String getLearner(int index) {
    
    if ((m_BaseClassifierNames == null) 
	|| (m_BaseClassifierNames.size() < index)) {
      return "";
    }
    if ((m_BaseClassifierOptions == null) 
	|| (m_BaseClassifierOptions.size() < index)
	|| ((String)m_BaseClassifierOptions.elementAt(index)).equals("")) {
      return (String)m_BaseClassifierNames.elementAt(index);
    }
    return (String)m_BaseClassifierNames.elementAt(index) 
      + " " + (String)m_BaseClassifierOptions.elementAt(index);
  }


  /**
   * Adds meta learner
   *
   * @param learnerString a string consisting of the class name of a classifier
   * followed by any required learner options
   * @exception Exception if the learner class name is not valid or the 
   * classifier does not accept the supplied options
   */
  public void addMetaLearner(String learnerString) throws Exception {

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

    // Everything good, so add meta learner
    m_MetaClassifierName = learnerName;
    m_MetaClassifierOptions = learnerOptions;
  }
  
  /**
   * Gets the meta learner string, which contains the class name of
   * the meta learner and any options to the meta learner
   *
   * @return the meta learner string, or the empty string if no meta learner
   * has been assigned.
   */
  public String getMetaLearner() {
    
    if (m_MetaClassifierName == null) {
      return "";
    }
    if ((m_MetaClassifierOptions == null) 
	|| ((String)m_MetaClassifierOptions).equals("")) {
      return m_MetaClassifierName;
    }
    return m_MetaClassifierName + " " + m_MetaClassifierOptions;
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

    if (m_BaseClassifierNames == null) {
      throw new Exception("No base learners have been set");
    }
    if (m_MetaClassifierName == null) {
      throw new Exception("No meta learner has been set");
    }
    if (!(data.classAttribute().isNominal() ||
	  data.classAttribute().isNumeric())) {
      throw new Exception("Class attribute has to be nominal or numeric!");
    }
    Instances newData = new Instances(data);
    m_BaseFormat = new Instances(data, 0);
    newData.deleteWithMissingClass();
    newData.randomize(new Random(m_Seed));
    if (newData.classAttribute().isNominal())
      newData.stratify(m_NumFolds);
    int numClassifiers = m_BaseClassifierNames.size();
    m_BaseClassifiers = new FastVector(m_BaseClassifierNames.size());
    for (int i = 0; i < numClassifiers; i++) {

      // Instantiate the base classifiers
      String learnerName = (String) m_BaseClassifierNames.elementAt(i);
      Classifier currentClassifier = (Classifier)Class.forName(learnerName)
	.newInstance();
      if (currentClassifier instanceof OptionHandler) {
	String [] options = Utils.splitOptions((String)m_BaseClassifierOptions
					 .elementAt(i));
	((OptionHandler)currentClassifier).setOptions(options);
      }
      m_BaseClassifiers.addElement(currentClassifier);
    }

    // Create meta data
    Instances metaData = metaFormat(newData);
    m_MetaFormat = new Instances(metaData, 0);
    for (int j = 0; j < m_NumFolds; j++) {
      Instances train = newData.trainCV(m_NumFolds, j);

      // Build base classifiers
      Enumeration enum = m_BaseClassifiers.elements();
      while (enum.hasMoreElements()) {
	Classifier currentClassifier = (Classifier) enum.nextElement();
	currentClassifier.buildClassifier(train);
      }

      // Classify test instances and add to meta data
      Instances test = newData.testCV(m_NumFolds, j);
      for (int i = 0; i < test.numInstances(); i++) {
	metaData.add(metaInstance(test.instance(i)));
      }
    }

    // Rebuilt all the base classifiers on the full training data
    for (int i = 0; i < numClassifiers; i++) {
      Classifier currentClassifier = (Classifier) m_BaseClassifiers.elementAt(i);
      currentClassifier.buildClassifier(newData);
    }
   

    // Instantiate meta classifier
    m_MetaClassifier = (Classifier)Class.forName(m_MetaClassifierName).newInstance();
    if (m_MetaClassifier instanceof OptionHandler) {
      String[] options = Utils.splitOptions(m_MetaClassifierOptions);
      ((OptionHandler)m_MetaClassifier).setOptions(options);
    }

    // Build meta classifier
    m_MetaClassifier.buildClassifier(metaData);
  }

  /**
   * Classifies a given instance using the stacked classifier.
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double classifyInstance(Instance instance) throws Exception {

    return m_MetaClassifier.classifyInstance(metaInstance(instance));
  }

  /**
   * Output a representation of this classifier
   */
  public String toString() {

    if ((m_BaseClassifierNames == null) || (m_BaseClassifierNames.size() == 0)) {
      return "Stacking: No base schemes entered";
    }
    if ((m_MetaClassifierName == null))
      return "Stacking: No meta scheme selected";

    String result = "Stacking\n\nBase classifiers\n\n";
    for (int i = 0; i < m_BaseClassifiers.size(); i++) {
      Classifier currentClassifier = (Classifier) m_BaseClassifiers.elementAt(i);
      result += currentClassifier.toString() +"\n\n";
    }
   
    result += "\n\nMeta classifier\n\n";
    result += m_MetaClassifier.toString();

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
      System.out.println(Evaluation.evaluateModel(new Stacking(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  /**
   * Makes the format for the level-1 data.
   *
   * @param instances the level-0 format
   * @return the format for the meta data
   */
  protected Instances metaFormat(Instances instances) throws Exception {

    FastVector attributes = new FastVector();
    Instances metaFormat;
    Attribute attribute;
    int i = 0;

    for (int k = 0; k < m_BaseClassifiers.size(); k++) {
      Classifier classifier = (Classifier) m_BaseClassifiers.elementAt(k);
      String name = (String) m_BaseClassifierNames.elementAt(k);
      if (m_BaseFormat.classAttribute().isNumeric()) {
	attributes.addElement(new Attribute(name));
      } else {
	if (classifier instanceof DistributionClassifier) {
	  for (int j = 0; j < m_BaseFormat.classAttribute().numValues(); j++) {
	    attributes.addElement(new Attribute(name + ":" + 
					 m_BaseFormat.classAttribute().value(j)));
	  }
	} else {
	  FastVector values = new FastVector();
	  for (int j = 0; j < m_BaseFormat.classAttribute().numValues(); j++) {
	    values.addElement(m_BaseFormat.classAttribute().value(j));
	  }
	  attributes.addElement(new Attribute(name, values));
	}
      }
    }
    attributes.addElement(m_BaseFormat.classAttribute());
    metaFormat = new Instances("Meta format", attributes, 0);
    metaFormat.setClassIndex(metaFormat.numAttributes() - 1);
    return metaFormat;
  }

  /**
   * Makes a level-1 instance from the given instance.
   * 
   * @param instance the instance to be transformed
   * @return the level-1 instance
   */
  protected Instance metaInstance(Instance instance) throws Exception {

    double[] values = new double[m_MetaFormat.numAttributes()];
    Instance metaInstance;
    int i = 0;

    Enumeration enum = m_BaseClassifiers.elements();
    while (enum.hasMoreElements()) {
      Classifier classifier = (Classifier) enum.nextElement();
      if (m_BaseFormat.classAttribute().isNumeric()) {
	values[i++] = classifier.classifyInstance(instance);
      } else {
	if (classifier instanceof DistributionClassifier) {
	  double[] dist = ((DistributionClassifier)classifier).
	    distributionForInstance(instance);
	  for (int j = 0; j < dist.length; j++) {
	    values[i++] = dist[j];
	  }
	} else {
	  values[i++] = classifier.classifyInstance(instance);
	}
      }
    }
    values[i] = instance.classValue();
    metaInstance = new Instance(1, values);
    metaInstance.setDataset(m_MetaFormat);
    return metaInstance;
  }
}


