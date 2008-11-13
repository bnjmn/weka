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
 *    Stacking.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.meta;

import weka.classifiers.*;
import weka.classifiers.rules.ZeroR;
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
 * @version $Revision: 1.23.2.1 $ 
 */
public class Stacking extends RandomizableMultipleClassifiersCombiner {

  /** The meta classifier */
  protected Classifier m_MetaClassifier = new ZeroR();
 
  /** Format for meta data */
  protected Instances m_MetaFormat = null;

  /** Format for base data */
  protected Instances m_BaseFormat = null;

  /** Set the number of folds for the cross-validation */
  protected int m_NumFolds = 10;
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Combines several classifiers using the stacking method. "
      + "Can do classification or regression. "
      + "For more information, see\n\n"
      + "David H. Wolpert (1992). \"Stacked "
      + "generalization\". Neural Networks, 5:241-259, Pergamon Press.";
  }
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(2);
    newVector.addElement(new Option(
	      metaOption(),
	      "M", 0, "-M <scheme specification>"));
    newVector.addElement(new Option(
	      "\tSets the number of cross-validation folds.",
	      "X", 1, "-X <number of folds>"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }
    return newVector.elements();
  }

  /**
   * String describing option for setting meta classifier
   */
  protected String metaOption() {

    return "\tFull name of meta classifier, followed by options.\n" +
      "\t(default: \"weka.classifiers.rules.Zero\")";
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
   * classifiers. (default: weka.classifiers.rules.ZeroR) <p>
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
    processMetaOptions(options);
    super.setOptions(options);
  }

  /**
   * Process options setting meta classifier.
   */
  protected void processMetaOptions(String[] options) throws Exception {

    String classifierString = Utils.getOption('M', options);
    String [] classifierSpec = Utils.splitOptions(classifierString);
    String classifierName;
    if (classifierSpec.length == 0) {
      classifierName = "weka.classifiers.rules.ZeroR";
    } else {
      classifierName = classifierSpec[0];
      classifierSpec[0] = "";
    }
    setMetaClassifier(Classifier.forName(classifierName, classifierSpec));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 4];

    int current = 0;
    options[current++] = "-X"; options[current++] = "" + getNumFolds();
    options[current++] = "-M";
    options[current++] = getMetaClassifier().getClass().getName() + " "
      + Utils.joinOptions(((OptionHandler)getMetaClassifier()).getOptions());

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);
    return options;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numFoldsTipText() {
    return "The number of folds used for cross-validation.";
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
      throw new IllegalArgumentException("Stacking: Number of cross-validation " +
					 "folds must be positive.");
    }
    m_NumFolds = numFolds;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String metaClassifierTipText() {
    return "The meta classifiers to be used.";
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

    if (m_MetaClassifier == null) {
      throw new IllegalArgumentException("No meta classifier has been set");
    }
    if (!(data.classAttribute().isNominal() ||
	  data.classAttribute().isNumeric())) {
      throw new UnsupportedClassTypeException("Class attribute has to be nominal " +
					      "or numeric!");
    }
    Instances newData = new Instances(data);
    m_BaseFormat = new Instances(data, 0);
    newData.deleteWithMissingClass();
    if (newData.numInstances() == 0) {
      throw new IllegalArgumentException("No training instances without missing " +
					 "class!");
    }
    Random random = new Random(m_Seed);
    newData.randomize(random);
    if (newData.classAttribute().isNominal()) {
      newData.stratify(m_NumFolds);
    }

    // Create meta level
    generateMetaLevel(newData, random);

    // Rebuilt all the base classifiers on the full training data
    for (int i = 0; i < m_Classifiers.length; i++) {
      getClassifier(i).buildClassifier(newData);
    }
  }

  /**
   * Generates the meta data
   */
  protected void generateMetaLevel(Instances newData, Random random) 
    throws Exception {

    Instances metaData = metaFormat(newData);
    m_MetaFormat = new Instances(metaData, 0);
    for (int j = 0; j < m_NumFolds; j++) {
      Instances train = newData.trainCV(m_NumFolds, j, random);

      // Build base classifiers
      for (int i = 0; i < m_Classifiers.length; i++) {
	getClassifier(i).buildClassifier(train);
      }

      // Classify test instances and add to meta data
      Instances test = newData.testCV(m_NumFolds, j);
      for (int i = 0; i < test.numInstances(); i++) {
	metaData.add(metaInstance(test.instance(i)));
      }
    }

    m_MetaClassifier.buildClassifier(metaData);
  }

  /**
   * Returns class probabilities.
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    return m_MetaClassifier.distributionForInstance(metaInstance(instance));
  }

  /**
   * Output a representation of this classifier
   */
  public String toString() {

    if (m_Classifiers.length == 0) {
      return "Stacking: No base schemes entered.";
    }
    if (m_MetaClassifier == null) {
      return "Stacking: No meta scheme selected.";
    }
    if (m_MetaFormat == null) {
      return "Stacking: No model built yet.";
    }
    String result = "Stacking\n\nBase classifiers\n\n";
    for (int i = 0; i < m_Classifiers.length; i++) {
      result += getClassifier(i).toString() +"\n\n";
    }
   
    result += "\n\nMeta classifier\n\n";
    result += m_MetaClassifier.toString();

    return result;
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

    for (int k = 0; k < m_Classifiers.length; k++) {
      Classifier classifier = (Classifier) getClassifier(k);
      String name = classifier.getClass().getName();
      if (m_BaseFormat.classAttribute().isNumeric()) {
	attributes.addElement(new Attribute(name));
      } else {
	for (int j = 0; j < m_BaseFormat.classAttribute().numValues(); j++) {
	  attributes.addElement(new Attribute(name + ":" + 
					      m_BaseFormat
					      .classAttribute().value(j)));
	}
      }
    }
    attributes.addElement(m_BaseFormat.classAttribute().copy());
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
    for (int k = 0; k < m_Classifiers.length; k++) {
      Classifier classifier = getClassifier(k);
      if (m_BaseFormat.classAttribute().isNumeric()) {
	values[i++] = classifier.classifyInstance(instance);
      } else {
	double[] dist = classifier.distributionForInstance(instance);
	for (int j = 0; j < dist.length; j++) {
	  values[i++] = dist[j];
	}
      }
    }
    values[i] = instance.classValue();
    metaInstance = new Instance(1, values);
    metaInstance.setDataset(m_MetaFormat);
    return metaInstance;
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
}









