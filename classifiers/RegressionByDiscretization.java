/*
 *    RegressionByDiscretization.java
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
import weka.estimators.*;
import weka.filters.*;

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
 * -W classname <br>
 * Specify the full class name of a weak learner as the basis for 
 * regression (required).<p>
 *
 * -B num <br>
 * The number of bins the class attribute will be discretized into.
 * (default 10) <p>
 *
 * -O <br>
 * Optimize number of bins (values up to and including the -B option will
 * be considered). (default no debugging output) <p>
 *
 * Any options after -- will be passed to the sub-learner. <p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version 1.0 - 17 Dec 1998 - Initial version (Len)
 */
public class RegressionByDiscretization extends Classifier 
  implements OptionHandler, WeightedInstancesHandler {

  // =================
  // Protected variables
  // =================

  /** The subclassifier. */
  protected DistributionClassifier m_Classifier;
  
  /** The discretization filter. */
  protected DiscretizeFilter m_Discretizer;

  /** The number of classes in the Discretized training data. */
  protected int m_NumBins = 10;

  /** The mean values for each Discretized class interval. */
  protected double [] m_ClassMeans;

  /** Whether debugging output will be printed */
  protected boolean b_Debug;

  /** Whether the Discretizer will optimise the number of bins */
  protected boolean b_OptimizeBins;


  // ===============
  // Public methods.
  // ===============

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    if (instances.numClasses() < 0) {
      throw new Exception ("Dataset has no class attribute");
    }
    
    if (m_Classifier instanceof WeightedInstancesHandler) {
      instances = new Instances(instances);
    } else {
      double[] weights = new double[instances.numInstances()];
      for (int i = 0; i < weights.length; i++) {
	weights[i] = instances.instance(i).weight();
      }
      instances = instances.resampleWithWeights(new Random(42), weights);
    }
    instances.deleteWithMissingClass();

    // Discretize the training data
    m_Discretizer = new DiscretizeFilter();
    m_Discretizer.setBins(m_NumBins);
    if (b_OptimizeBins) {
      m_Discretizer.setOptimizeBinning(true);
    }
    m_Discretizer.setUseMDL(false);
    m_Discretizer.setAttributeIndices(""+ (instances.classIndex() + 1));
    m_Discretizer.inputFormat(instances);
    Instances newTrain = Filter.useFilter(instances, m_Discretizer);
    int numClasses = newTrain.numClasses();

    // Calculate the mean value for each bin of the new class attribute
    m_ClassMeans = new double [numClasses];
    int [] classCounts = new int [numClasses];
    for (int i = 0; i < instances.numInstances(); i++) {
      int classVal = (int) newTrain.instance(i).classValue();
      classCounts[classVal]++;
      m_ClassMeans[classVal] += instances.instance(i).classValue();
    }

    for (int i = 0; i < numClasses; i++) {
      if (classCounts[i] > 0) {
	m_ClassMeans[i] /= classCounts[i];
      }
    }

    if (b_Debug) {
      System.out.println("Boundaries    Bin Mean");
      System.out.println("======================");
      System.out.println("-infinity");
      double [] cutPoints = m_Discretizer.getCutPoints(instances.classIndex());
      if (cutPoints != null) {
	for (int i = 0; i < cutPoints.length; i++) {
	  System.out.println("              " + m_ClassMeans[i]);
	  System.out.println("" + cutPoints[i]);
	}
      }
      System.out.println("              " 
			 + m_ClassMeans[m_ClassMeans.length - 1]);
      System.out.println("infinity");
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
  public double classifyInstance(Instance instance) 
       throws Exception {  

    // Discretize the test instance
    if (m_Discretizer.numPendingOutput() > 0) {
      throw new Exception("DiscretizeFilter output queue not empty");
    }

    if (m_Discretizer.input(instance)) {

      Instance newInstance = m_Discretizer.output();
      double [] probs = m_Classifier.distributionForInstance(newInstance);
      
      double prediction = 0, probSum = 0;
      for (int j = 0; j < probs.length; j++) {
	prediction += probs[j] * m_ClassMeans[j];
	probSum += probs[j];
      }
      
      return prediction / probSum;
      
    } else {
      throw new Exception("DiscretizeFilter didn't make the test instance"
			  + " immediately available");
    }
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3);
    newVector.addElement(new Option("\tProduce debugging output."
				    + "\t(default no debugging output)",
				    "D", 0,"-D"));
    newVector.addElement(new Option("\tNumber of bins the class attribute will"
				    + " be discretized into.\n"
				    + "\t(default 10)",
				    "B", 1,"-B"));
    newVector.addElement(new Option("\tOptimize number of bins (values"
				    + " up to and including the -B option will"
				    + " be considered)\n"
				    + "\t(default no debugging output)",
				    "O", 0,"-O"));
    newVector.addElement(new Option("\tFull class name of sub-classifier to"
				    + " use for the regression.\n"
				    + "\teg: weka.classifiers.NaiveBayes",
				    "W", 1,"-W"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Produce debugging output. <p>
   *
   * -W classname <br>
   * Specify the full class name of a weak learner as the basis for 
   * regression (required).<p>
   *
   * -B num <br>
   * The number of bins the class attribute will be discretized into.
   * (default 10) <p>
   *
   * -O <br>
   * Optimize number of bins (values up to and including the -B option will
   * be considered). (default no debugging output) <p>
   *
   * Any options after -- will be passed to the sub-learner. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String binString = Utils.getOption('B', options);
    if (binString.length() != 0) {
      setNumBins(Integer.parseInt(binString));
    } else {
      setNumBins(10);
    }

    setDebug(Utils.getFlag('D', options));

    setOptimizeBins(Utils.getFlag('O', options));

    String learnerString = Utils.getOption('W', options);
    if (learnerString.length() == 0) {
      throw new Exception("A learner must be specified with the -W option.");
    }
    setLearner(learnerString);

    if ((m_Classifier != null) && 
	(m_Classifier instanceof OptionHandler)) {
      String [] classifierOptions = Utils.partitionOptions(options);
      ((OptionHandler)m_Classifier).setOptions(classifierOptions);
      Utils.checkForRemainingOptions(classifierOptions);
    }
      
    /*    Utils.checkForRemainingOptions(options); */
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
    String [] options = new String [classifierOptions.length + 14];
    int current = 0;
    if (getDebug()) {
      options[current++] = "-D";
    }
    if (getOptimizeBins()) {
      options[current++] = "-O";
    }
    options[current++] = "-B"; options[current++] = "" + getNumBins();
    if (getLearner() != null) {
      options[current++] = "-W"; options[current++] = getLearner();
    }
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
   * Sets whether the discretizer optimizes the number of bins
   *
   * @param optimize true if the discretizer should optimize the number of bins
   */
  public void setOptimizeBins(boolean optimize) {

    b_OptimizeBins = optimize;
  }

  /**
   * Gets whether the discretizer optimizes the number of bins
   *
   * @return true if the discretizer should optimize the number of bins
   */
  public boolean getOptimizeBins() {

    return b_OptimizeBins;
  }

  /**
   * Sets whether debugging output will be printed
   *
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {

    b_Debug = debug;
  }

  /**
   * Gets whether debugging output will be printed
   *
   * @return true if debug output should be printed
   */
  public boolean getDebug() {

    return b_Debug;
  }

  /**
   * Sets the number of bins the class attribute will be discretized into.
   *
   * @param numBins the number of bins to use
   */
  public void setNumBins(int numBins) {

    m_NumBins = numBins;
  }

  /**
   * Gets the number of bins the class attribute will be discretized into.
   *
   * @return the number of bins to use
   */
  public int getNumBins() {

    return m_NumBins;
  }


  /**
   * Gets the name of the learner
   *
   * @return the full class name of the learner
   */
  public String getLearner() {

    if (m_Classifier == null) {
      return null;
    }
    return m_Classifier.getClass().getName();
  }

  /**
   * Set the base learner for regression. 
   * @param learnerName the full class name of the learner to use
   * @exception Exception if learnerName is not a valid class name
   */
  public void setLearner(String learnerName) throws Exception {

    m_Classifier = null;
    try {
      m_Classifier = (DistributionClassifier)Class.forName(learnerName).
      newInstance();
    } catch (Exception ex) {
      throw new Exception("Can't find DistributionClassifier with class name: "
			  + learnerName);
    }
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    int attIndex;

    text.append("Regression by discretization\n");
    if (m_Classifier == null) {
      text.append("No subclassifier assigned");
    } else {
      text.append("\nClass attribute discretized into " 
		  + m_ClassMeans.length + " values\n");

      text.append("\nSubclassifier: " + m_Classifier.getClass().getName() 
		  + "\n\n");
      text.append(m_Classifier.toString());
    }
    return text.toString();
  }
 

  // ============
  // Test method.
  // ============

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(
			 new RegressionByDiscretization(), argv));
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println(ex.getMessage());
    }
  }
}



