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
 * -B classifierstring <br>
 * Classifierstring should contain the full class name of a base scheme
 * followed by options to the classifier.
 * (required, option should be used once for each classifier).<p>
 *
 * -M classifierstring <br>
 * Classifierstring for the meta classifier. Same format as for base
 * classifiers. (required) <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.9 $ 
 */
public class Stacking extends Classifier implements OptionHandler {

  /** The meta classifier. */
  protected Classifier m_MetaClassifier = new weka.classifiers.ZeroR();

  /** The base classifiers. */
  protected Classifier [] m_BaseClassifiers = new Classifier [0];
 
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
	      "\tFull class name of base classifiers to include, followed "
	      + "by scheme options\n"
	      + "\t(may be specified multiple times).\n"
	      + "\teg: \"weka.classifiers.NaiveBayes -K\"",
	      "B", 1, "-B <scheme specification>"));
    newVector.addElement(new Option(
	      "\tFull name of meta classifier, followed by options.",
	      "M", 0, "-M <scheme specification>"));
    newVector.addElement(new Option(
	      "\tSets the number of cross-validation folds.",
	      "X", 1, "-X <number of folds>"));
    newVector.addElement(new Option(
	      "\tSets the random number seed.",
	      "S", 1, "-S <random number seed>"));

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
   * -B classifierstring <br>
   * Classifierstring should contain the full class name of a base scheme
   * followed by options to the classifier.
   * (required, option should be used once for each classifier).<p>
   *
   * -M classifierstring <br>
   * Classifierstring for the meta classifier. Same format as for base
   * classifiers. (required) <p>
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
    if (classifiers.size() == 0) {
      throw new Exception("At least one base classifier must be specified"
			  + " with the -B option.");
    } else {
      Classifier [] classifiersArray = new Classifier [classifiers.size()];
      for (int i = 0; i < classifiersArray.length; i++) {
	classifiersArray[i] = (Classifier) classifiers.elementAt(i);
      }
      setBaseClassifiers(classifiersArray);
    }

    String classifierString = Utils.getOption('M', options);
    String [] classifierSpec = Utils.splitOptions(classifierString);
    if (classifierSpec.length == 0) {
      throw new Exception("Meta classifier has to be provided.");
    }
    String classifierName = classifierSpec[0];
    classifierSpec[0] = "";
    setMetaClassifier(Classifier.forName(classifierName, classifierSpec));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String[6];
    int current = 0;

    if (m_BaseClassifiers.length != 0) {
      options = new String [m_BaseClassifiers.length * 2 + 6];
      for (int i = 0; i < m_BaseClassifiers.length; i++) {
	options[current++] = "-B";
	options[current++] = "" + getBaseClassifierSpec(i);
      }
    }
    options[current++] = "-X"; options[current++] = "" + getNumFolds();
    options[current++] = "-S"; options[current++] = "" + getSeed();
    if (getMetaClassifier() != null) {
      options[current++] = "-M";
      options[current++] = getClassifierSpec(getMetaClassifier());
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
   * Sets the list of possible classifers to choose from.
   *
   * @param classifiers an array of classifiers with all options set.
   */
  public void setBaseClassifiers(Classifier [] classifiers) {

    m_BaseClassifiers = classifiers;
  }

  /**
   * Gets the list of possible classifers to choose from.
   *
   * @return the array of Classifiers
   */
  public Classifier [] getBaseClassifiers() {

    return m_BaseClassifiers;
  }

  /**
   * Gets the specific classifier from the set of base classifiers.
   *
   * @param index the index of the classifier to retrieve
   * @return the classifier
   */
  public Classifier getBaseClassifier(int index) {
    
    return m_BaseClassifiers[index];
  }


  /**
   * Adds meta classifier
   *
   * @param classifier the classifier with all options set.
   */
  public void setMetaClassifier(Classifier classifier) {

    m_MetaClassifier = classifier;
  }
  
  /**
   * Gets the meta classifier.
   *
   * @return the meta classifier
   */
  public Classifier getMetaClassifier() {
    
    return m_MetaClassifier;
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

    if (m_BaseClassifiers.length == 0) {
      throw new Exception("No base classifiers have been set");
    }
    if (m_MetaClassifier == null) {
      throw new Exception("No meta classifier has been set");
    }
    if (!(data.classAttribute().isNominal() ||
	  data.classAttribute().isNumeric())) {
      throw new Exception("Class attribute has to be nominal or numeric!");
    }
    Instances newData = new Instances(data);
    m_BaseFormat = new Instances(data, 0);
    newData.deleteWithMissingClass();
    if (newData.numInstances() == 0) {
      throw new Exception("No training instances without missing class!");
    }
    newData.randomize(new Random(m_Seed));
    if (newData.classAttribute().isNominal())
      newData.stratify(m_NumFolds);
    int numClassifiers = m_BaseClassifiers.length;
    // Create meta data
    Instances metaData = metaFormat(newData);
    m_MetaFormat = new Instances(metaData, 0);
    for (int j = 0; j < m_NumFolds; j++) {
      Instances train = newData.trainCV(m_NumFolds, j);

      // Build base classifiers
      for (int i = 0; i < m_BaseClassifiers.length; i++) {
	getBaseClassifier(i).buildClassifier(train);
      }

      // Classify test instances and add to meta data
      Instances test = newData.testCV(m_NumFolds, j);
      for (int i = 0; i < test.numInstances(); i++) {
	metaData.add(metaInstance(test.instance(i)));
      }
    }

    // Rebuilt all the base classifiers on the full training data
    for (int i = 0; i < numClassifiers; i++) {
      getBaseClassifier(i).buildClassifier(newData);
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

    if (m_BaseClassifiers.length == 0) {
      return "Stacking: No base schemes entered";
    }
    if ((m_MetaClassifier == null))
      return "Stacking: No meta scheme selected";

    String result = "Stacking\n\nBase classifiers\n\n";
    for (int i = 0; i < m_BaseClassifiers.length; i++) {
      result += getBaseClassifier(i).toString() +"\n\n";
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

    for (int k = 0; k < m_BaseClassifiers.length; k++) {
      Classifier classifier = (Classifier) getBaseClassifier(k);
      String name = classifier.getClass().getName();
      if (m_BaseFormat.classAttribute().isNumeric()) {
	attributes.addElement(new Attribute(name));
      } else {
	if (classifier instanceof DistributionClassifier) {
	  for (int j = 0; j < m_BaseFormat.classAttribute().numValues(); j++) {
	    attributes.addElement(new Attribute(name + ":" + 
						m_BaseFormat
						.classAttribute().value(j)));
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
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @param index the index of the classifier string to retrieve, starting from
   * 0.
   * @return the classifier string, or the empty string if no classifier
   * has been assigned (or the index given is out of range).
   */
  protected String getBaseClassifierSpec(int index) {
    
    if (m_BaseClassifiers.length < index) {
      return "";
    }
    return getClassifierSpec(getBaseClassifier(index));
  }
  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @param c the classifier
   * @return the classifier specification string.
   */
  protected String getClassifierSpec(Classifier c) {
    
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
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
    for (int k = 0; k < m_BaseClassifiers.length; k++) {
      Classifier classifier = getBaseClassifier(k);
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


