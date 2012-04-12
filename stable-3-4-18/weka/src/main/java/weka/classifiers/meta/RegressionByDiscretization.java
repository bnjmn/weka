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

import weka.classifiers.*;
import weka.classifiers.rules.ZeroR;
import java.io.*;
import java.util.*;

import weka.core.*;
import weka.estimators.*;
import weka.filters.unsupervised.attribute.Discretize;
import weka.filters.Filter;

/**
 * Class for a regression scheme that employs any distribution
 * classifier on a copy of the data that has the class attribute (equal-width)
 * discretized. The predicted value is the expected value of the 
 * mean class value for each discretized interval (based on the 
 * predicted probabilities for each interval).<p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Produce debugging output. <p>
 *
 * -B <int> <br>
 * Number of bins for equal-width discretization (default 10).<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.30.2.1 $
 */
public class RegressionByDiscretization extends SingleClassifierEnhancer {
  
  /** The discretization filter. */
  protected Discretize m_Discretizer = new Discretize();

  /** The number of discretization intervals. */
  protected int m_NumBins = 10;

  /** The mean values for each Discretized class interval. */
  protected double [] m_ClassMeans;
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "A regression scheme that employs any "
      + "classifier on a copy of the data that has the class attribute (equal-width) "
      + "discretized. The predicted value is the expected value of the "
      + "mean class value for each discretized interval (based on the "
      + "predicted probabilities for each interval).";
  }

  /**
   * String describing default classifier.
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.trees.J48";
  }

  /**
   * Default constructor.
   */
  public RegressionByDiscretization() {

    m_Classifier = new weka.classifiers.trees.J48();
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
    m_Discretizer.setAttributeIndices("" + (instances.classIndex() + 1));
    m_Discretizer.setBins(getNumBins());
    m_Discretizer.setInputFormat(instances);
    Instances newTrain = Filter.useFilter(instances, m_Discretizer);

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

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
	      "\tNumber of bins for equal-width discretization\n"
	      + "\t(default 10).\n",
	      "B", 1, "-B <int>"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }

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
   * followed by options to the classifier
   * (default: weka.classifiers.rules.ZeroR).<p>
   *
   * -B <int> <br>
   * Number of bins for equal-width discretization (default 10).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String binsString = Utils.getOption('B', options);
    if (binsString.length() != 0) {
      setNumBins(Integer.parseInt(binsString));
    } else {
      setNumBins(10);
    }

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 2];
    int current = 0;

    options[current++] = "-B";
    options[current++] = "" + getNumBins();

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);

    return options;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numBinsTipText() {

    return "Number of bins for discretization.";
  }

  /**
   * Gets the number of bins numeric attributes will be divided into
   *
   * @return the number of bins.
   */
  public int getNumBins() {

    return m_NumBins;
  }

  /**
   * Sets the number of bins to divide each selected numeric attribute into
   *
   * @param numBins the number of bins
   */
  public void setNumBins(int numBins) {

    m_NumBins = numBins;
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



