/*
 *    Bagging.java
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
 * Class for bagging a classifier.<p>
 *
 * Valid options are:<p>
 *
 * -W classname <br>
 * Specify the full class name of a weak learner as the basis for 
 * boosting (required).<p>
 *
 * -I num <br>
 * Set the number of bagging iterations (default 10). <p>
 *
 * -S seed <br>
 * Random number seed for resampling. <p>
 *
 * Options after -- are passed to the designated learner.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class Bagging extends DistributionClassifier 
  implements OptionHandler {

  // =================
  // Private variables
  // =================

  /** A reference to the instance format */
  protected Instances m_Training;

  /** Array for storing the generated base classifiers. */
  protected Classifier[] m_Classifiers;
  
  /** A random number generator. */
  protected Random m_Random = new Random();
  
  /** The number of iterations. */
  protected int m_NumIterations = 10;

  /** The seed for random number generation. */
  protected int m_Seed = -1;

  // ===============
  // Public methods.
  // ===============

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
	      "\tNumber of bagging iterations.\n"
	      + "\t(default 10)",
	      "I", 1, "-I"));
    newVector.addElement(new Option(
	      "\tFull name of scheme to bag.\n"
	      + "\teg: weka.classifiers.NaiveBayes",
	      "W", 1, "-W"));
    newVector.addElement(new Option("\tSeed for random number generator.",
				    "S", 1, "-S"));

    if ((m_Classifiers != null) &&
	(m_Classifiers[0] instanceof OptionHandler)) {
      newVector.
	addElement(new Option("", "", 0, "\nOptions specific to scheme "
			      + m_Classifiers[0].getClass().getName() + ":"));
      Enumeration enum = ((OptionHandler)m_Classifiers[0]).listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    }
    return newVector.elements();
  }


  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classname <br>
   * Specify the full class name of a weak learner as the basis for 
   * boosting (required).<p>
   *
   * -I num <br>
   * Set the number of bagging iterations (default 10). <p>
   *
   * -S seed <br>
   * Random number seed for resampling. <p>
   *
   * Options after -- are passed to the designated learner.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String learnerString = Utils.getOption('W', options);
    if (learnerString.length() == 0) {
      throw new Exception("A scheme must be specified with the -W option.");
    }

    String bagIterations = Utils.getOption('I', options);
    if (bagIterations.length() != 0) {
      setNumIterations(Integer.parseInt(bagIterations));
    }

    setScheme(learnerString);

    String seed = Utils.getOption('S', options);
    if (seed.length() != 0) {
      setSeed(Integer.parseInt(seed));
    }

    // Set the options for each classifier
    if ((m_Classifiers != null) &&
	(m_Classifiers[0] instanceof OptionHandler)) {
      String [] classifierOptions = Utils.partitionOptions(options);
      for(int i = 0; i < getNumIterations(); i++) {
	String [] tempOptions = (String [])classifierOptions.clone();
	((OptionHandler)m_Classifiers[i]).setOptions(tempOptions);
	Utils.checkForRemainingOptions(tempOptions);
      }
    }	  
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] classifierOptions = new String [0];
    if ((m_Classifiers != null) && 
	(m_Classifiers[0] instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifiers[0]).getOptions();
    }
    String [] options = new String [classifierOptions.length + 7];
    int current = 0;
    options[current++] = "-S"; options[current++] = "" + getSeed();
    options[current++] = "-I"; options[current++] = "" + getNumIterations();

    if (getScheme() != null) {
      options[current++] = "-W"; options[current++] = getScheme();
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
   * Sets the scheme to be bagged
   *
   * @param learnerName the full class name of the learner to bag
   * @exception Exception if learnerName is not a valid class name
   */
  public void setScheme(String learnerName) throws Exception {

    m_Classifiers = null;
    try {
      m_Classifiers = new Classifier [getNumIterations()];
      for(int i = 0; i < getNumIterations(); i++) {
	m_Classifiers[i] = (Classifier)Class.forName(learnerName)
	  .newInstance();
      }
    } catch (Exception ex) {
      throw new Exception("Can't find Classifier with class name: " +
			  learnerName);
    }
  }

  /**
   * Gets the name of the bagged learner
   *
   * @return the full class name of the bagged learner
   */
  public String getScheme() {

    if (m_Classifiers == null) {
      return null;
    }
    return m_Classifiers[0].getClass().getName();
  }

  /**
   * Sets the number of bagging iterations
   */
  public void setNumIterations(int numIterations) {

    m_NumIterations = numIterations;
  }

  /**
   * Gets the number of bagging iterations
   *
   * @return the maximum number of bagging iterations
   */
  public int getNumIterations() {
    
    return m_NumIterations;
  }

  /**
   * Set the seed for random number generation.
   *
   * @param seed the seed 
   */
  public void setSeed(int seed) {

    m_Seed = seed;
    m_Random.setSeed(seed);
  }

  /**
   * Gets the seed for the random number generations
   *
   * @return the seed for the random number generation
   */
  public int getSeed() {
    
    return m_Seed;
  }

  /**
   * Bagging method.
   *
   * @param data the training data to be used for generating the
   * bagged classifier.
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    Instances baggData;
    int i, j;

    if (m_Classifiers == null) {
      throw new Exception("A base learner has not been specified!");
    }
    m_Training = data;
    for (j = 0; j < m_Classifiers.length; j++) {
      baggData = new Instances(data, data.numInstances());
      while (baggData.numInstances() < data.numInstances()) {
	i = (int) (m_Random.nextDouble() * 
		   (double) data.numInstances());
	baggData.add(data.instance(i));
      }
      m_Classifiers[j].buildClassifier(baggData);
    }
  }
  
  /**
   * Classifies a given instance using the bagged classifier.
   *
   * @param data the test data
   * @param index the index of the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double classifyInstance(Instance instance)
       throws Exception {
      
    return (double)Utils.maxIndex(distributionForInstance(instance));
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return preedicted class probability distribution
   * @exception Exception if distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    double [] sums = new double [instance.numClasses()], newProbs; 
    
    for (int i = 0; i < m_NumIterations; i++) {
      if (m_Classifiers[i] instanceof DistributionClassifier) {
	newProbs = ((DistributionClassifier)m_Classifiers[i]).
	  distributionForInstance(instance);
	for (int j = 0; j < newProbs.length; j++)
	  sums[j] += newProbs[j];
      } else {
	sums[(int)m_Classifiers[i].classifyInstance(instance)]++;
      }
    }
    Utils.normalize(sums);
    return sums;
  }

  /**
   * Returns description of the bagged classifier.
   *
   * @return description of the bagged classifier as a string
   */
  public String toString() {
    
    StringBuffer text = new StringBuffer();
    
    text.append("All the base classifiers: \n\n");
    for (int i = 0; i < m_Classifiers.length; i++)
      text.append(m_Classifiers[i].toString() + "\n\n");
    
    return text.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {
   
    try {
      System.out.println(Evaluation.
			 evaluateModel(new Bagging(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
