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
 *    RegressionByDiscretization.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Evaluation;
import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;
import java.io.*;
import java.util.*;

import weka.core.*;
import weka.estimators.*;
import weka.filters.unsupervised.attribute.Discretize;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.PotentialClassIgnorer;

/**
 * Class for a regression scheme that employs any distribution
 * classifier on a copy of the data that has the class attribute
 * discretized. The predicted value is the expected value of the 
 * mean class value for each discretized interval (based on the 
 * predicted probabilities for each interval).<p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Produce debugging output. <p>
 *
 * -W classifierstring <br>
 * Classifierstring should contain the full class name of a classifier
 * followed by options to the classifier.
 * (required).<p>
 *
 * -F discretizerstring <br>
 * Full name of discretizer (should be a discretizer), followed by options.<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.24 $
 */
public class RegressionByDiscretization extends Classifier 
  implements OptionHandler {

  /** The subclassifier. */
  protected Classifier m_Classifier = new ZeroR();
  
  /** The discretization filter. */
  protected PotentialClassIgnorer m_Discretizer = new Discretize("last");

  /** The mean values for each Discretized class interval. */
  protected double [] m_ClassMeans;

  /** Whether debugging output will be printed */
  protected boolean m_Debug;
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "A regression scheme that employs any "
      + "classifier on a copy of the data that has the class attribute "
      + "discretized. The predicted value is the expected value of the "
      + "mean class value for each discretized interval (based on the "
      + "predicted probabilities for each interval).";
  }

  /**
   * Default constructor.
   */
  public RegressionByDiscretization() {
    
    ((Discretize)m_Discretizer).setAttributeIndices("last");
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    if (!instances.classAttribute().isNumeric()) {
      throw new UnsupportedClassTypeException ("Class attribute has to be numeric");
    }

    // Discretize the training data
    m_Discretizer.setIgnoreClass(true);
    m_Discretizer.setInputFormat(instances);
    Instances newTrain = Filter.useFilter(instances, m_Discretizer);

    if (!newTrain.classAttribute().isNominal()) {
      throw new UnsupportedClassTypeException ("Class attribute has to be nominal " +
					       "after discretization");
    }
    int numClasses = newTrain.numClasses();

    // Calculate the mean value for each bin of the new class attribute
    m_ClassMeans = new double [numClasses];
    int [] classCounts = new int [numClasses];
    for (int i = 0; i < instances.numInstances(); i++) {
      Instance inst = newTrain.instance(i);
      if (!inst.classIsMissing()) {
	int classVal = (int) inst.classValue();
	classCounts[classVal]++;
	m_ClassMeans[classVal] += instances.instance(i).classValue();
      }
    }

    for (int i = 0; i < numClasses; i++) {
      if (classCounts[i] > 0) {
	m_ClassMeans[i] /= classCounts[i];
      }
    }

    if (m_Debug) {
      System.out.println("Bin Means");
      System.out.println("==========");
      for (int i = 0; i < m_ClassMeans.length; i++) {
	System.out.println(m_ClassMeans[i]);
      }
      System.out.println();
    }

    // Train the sub-classifier
    m_Classifier.buildClassifier(newTrain);
  }

  /**
   * Returns a predicted class for the test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class value
   * @exception Exception if the prediction couldn't be made
   */
  public double classifyInstance(Instance instance) throws Exception {  

    // Discretize the test instance
    if (m_Discretizer.numPendingOutput() > 0) {
      throw new Exception("Discretize output queue not empty");
    }

    if (m_Discretizer.input(instance)) {

      m_Discretizer.batchFinished();
      Instance newInstance = m_Discretizer.output();
      double [] probs = m_Classifier.distributionForInstance(newInstance);
      
      double prediction = 0, probSum = 0;
      for (int j = 0; j < probs.length; j++) {
	prediction += probs[j] * m_ClassMeans[j];
	probSum += probs[j];
      }

      return prediction /  probSum;
      
    } else {
      throw new Exception("Discretize didn't make the test instance"
			  + " immediately available");
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3);
    newVector.addElement(new Option("\tProduce debugging output.",
				    "D", 0,"-D"));

    newVector.addElement(new Option(
	      "\tFull class name of classifier to use, followed\n"
	      + "\tby scheme options. (required)\n"
	      + "\teg: \"weka.classifiers.bayes.NaiveBayes -D\"",
	      "W", 1, "-W <classifier specification>"));
    newVector.addElement(new Option(
	      "\tFull name of filter (should be a discretizer), followed\n"
	      + "\tby options for this filter.\n"
	      + "\teg: \"weka.filters.unsupervised.attribute.Discretize -R last\"",
	      "F", 1, "-F <discretizer specification>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Produce debugging output. <p>
   *
   * -W classifierstring <br>
   * Classifierstring should contain the full class name of a classifier
   * followed by options to the classifier.
   * (required).<p>
   *
   * -F discretizerstring <br>
   * Full name of filter (should be a discretizer), followed by options.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setDebug(Utils.getFlag('D', options));

    String classifierString = Utils.getOption('W', options);
    if (classifierString.length() == 0) {
      throw new Exception("A classifier must be specified"
			  + " with the -W option.");
    }
    String [] classifierSpec = Utils.splitOptions(classifierString);
    if (classifierSpec.length == 0) {
      throw new Exception("Invalid classifier specification string");
    }
    String classifierName = classifierSpec[0];
    classifierSpec[0] = "";
    setClassifier(Classifier.forName(classifierName, classifierSpec));
    
    // Same for discretizer
    String discretizerString = Utils.getOption('F', options);
    if (discretizerString.length() != 0) {
      String [] discretizerSpec = Utils.splitOptions(discretizerString);
      if (discretizerSpec.length == 0) {
	throw new Exception("Invalid discretizer specification string");
      }
      String discretizerName = discretizerSpec[0];
      discretizerSpec[0] = "";
      setDiscretizer((Filter)Utils.
		     forName(Filter.class, discretizerName, discretizerSpec));
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
    if (getDebug()) {
      options[current++] = "-D";
    }

    options[current++] = "-W";
    options[current++] = "" + getClassifierSpec();

    // Same for discretizer
    options[current++] = "-F";
    options[current++] = "" + getDiscretizerSpec();

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
  public String discretizerTipText() {
    return "The discretizer to use. Chosen class must extend PotentialClassIgnorer.";
  }

  /**
   * Sets the discretizer
   *
   * @param discretizer the discretizer with all options set.
   */
  public void setDiscretizer(Filter discretizer) {

    if (!(discretizer instanceof PotentialClassIgnorer)) {
      throw new IllegalArgumentException("Filter must extend PotentialClassIgnorer!");
    }
    m_Discretizer = (PotentialClassIgnorer)discretizer;
  }

  /**
   * Gets the discretizer used.
   *
   * @return the discretizer
   */
  public Filter getDiscretizer() {

    return m_Discretizer;
  }
  
  /**
   * Gets the discretizer specification string, which contains the class name of
   * the discretizer and any options to the discretizer
   *
   * @return the discretizer string.
   */
  protected String getDiscretizerSpec() {
    
    Filter c = getDiscretizer();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classifierTipText() {
    return "The classifiers to be used.";
  }
  
  /**
   * Set the classifier for boosting. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {

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
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @return the classifier string.
   */
  protected String getClassifierSpec() {
    
    Classifier c = getClassifier();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String debugTipText() {
    return "Whether debug information is output to console.";
  }

  /**
   * Get the Debug value.
   * @return the Debug value.
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Set the Debug value.
   * @param newDebug The new Debug value.
   */
  public void setDebug(boolean newDebug) {
    
    m_Debug = newDebug;
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    int attIndex;

    text.append("Regression by discretization");
    if (m_ClassMeans == null) {
      text.append(": No model built yet.");
    } else {
      text.append("\n\nClass attribute discretized into " 
		  + m_ClassMeans.length + " values\n");

      text.append("\nClassifier spec: " + getClassifierSpec() 
		  + "\n");
      text.append("\nDiscretizer spec: " + getDiscretizerSpec()
		  + "\n\n");
      text.append(m_Classifier.toString());
    }
    return text.toString();
  }
 
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(
			 new RegressionByDiscretization(), argv));
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}



