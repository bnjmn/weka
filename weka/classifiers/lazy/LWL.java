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
 *    LWL.java
 *    Copyright (C) 1999, 2002, 2003 Len Trigg, Eibe Frank
 *
 */

package weka.classifiers.lazy;

import weka.classifiers.DistributionClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.meta.DistributionMetaClassifier;
import weka.classifiers.UpdateableClassifier;
import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Locally-weighted learning. Uses an instance-based algorithm to
 * assign instance weights which are then used by a specified
 * WeightedInstancesHandler.  A good choice for classification is
 * NaiveBayes. LinearRegression is suitable for regression problems.
 * For more information, see<p>
 *
 * Eibe Frank, Mark Hall, and Bernhard Pfahringer (2003). Locally
 * Weighted Naive Bayes. Working Paper 04/03, Department of Computer
 * Science, University of Waikato.
 *
 * Atkeson, C., A. Moore, and S. Schaal (1996) <i>Locally weighted
 * learning</i>
 * <a href="ftp://ftp.cc.gatech.edu/pub/people/cga/air1.ps.gz">download 
 * postscript</a>. <p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Produce debugging output. <p>
 *
 * -K num <br>
 * Set the number of neighbours used for setting kernel bandwidth.
 * (default all) <p>
 *
 * -W num <br>
 * Set the weighting kernel shape to use. 1 = Inverse, 2 = Gaussian.
 * (default 0 = Linear) <p>
 *
 * -B classname <br>
 * Specify the full class name of a base classifier (which needs
 * to be a WeightedInstancesHandler) (required).<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.7 $ */
public class LWL extends DistributionClassifier 
  implements OptionHandler, UpdateableClassifier, 
  WeightedInstancesHandler {

  // To maintain the same version number after fixing bug
  static final long serialVersionUID = 7858523588073370243L;
  
  /** The training instances used for classification. */
  protected Instances m_Train;

  /** The minimum values for numeric attributes. */
  protected double [] m_Min;

  /** The maximum values for numeric attributes. */
  protected double [] m_Max;
    
  /** True if debugging output should be printed */
  protected boolean m_Debug;

  /** The number of neighbours used to select the kernel bandwidth */
  protected int m_kNN = -1;

  /** The weighting kernel method currently selected */
  protected int m_WeightKernel = LINEAR;

  /** True if m_kNN should be set to all instances */
  protected boolean m_UseAllK = true;

  /** The available kernel weighting methods */
  protected static final int LINEAR  = 0;
  protected static final int INVERSE = 1;
  protected static final int GAUSS   = 2;

  /** The classifier object */
  protected DistributionClassifier m_classifier = new DecisionStump();

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(4);
    newVector.addElement(new Option(
				    "\tClassifier to wrap. (required)\n",
				    "B", 1,"-B <classifier name>"));

    newVector.addElement(new Option("\tProduce debugging output.\n"
				    + "\t(default no debugging output)",
				    "D", 0, "-D"));
    newVector.addElement(new Option("\tSet the number of neighbors used to set"
				    + " the kernel bandwidth.\n"
				    + "\t(default all)",
				    "K", 1, "-K <number of neighbours>"));
    newVector.addElement(new Option("\tSet the weighting kernel shape to use."
				    + " 1 = Inverse, 2 = Gaussian.\n"
				    + "\t(default 0 = Linear)",
				    "W", 1,"-W <number of weighting method>"));
    
    if ((m_classifier != null) &&
	(m_classifier instanceof OptionHandler)) {
      newVector.addElement(new Option(
	     "",
	     "", 0, "\nOptions specific to classifier "
	     + m_classifier.getClass().getName() + ":"));
      Enumeration enum = ((OptionHandler)m_classifier).listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    }

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Produce debugging output. <p>
   *
   * -K num <br>
   * Set the number of neighbours used for setting kernel bandwidth.
   * (default all) <p>
   *
   * -W num <br>
   * Set the weighting kernel shape to use. 1 = Inverse, 2 = Gaussian.
   * (default 0 = Linear) <p>
   *
   * -B classname <br>
   * Specify the full class name of a base classifier (which needs
   * to be a WeightedInstancesHandler) (required).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String knnString = Utils.getOption('K', options);
    if (knnString.length() != 0) {
      setKNN(Integer.parseInt(knnString));
    } else {
      setKNN(0);
    }

    String weightString = Utils.getOption('W', options);
    if (weightString.length() != 0) {
      setWeightingKernel(Integer.parseInt(weightString));
    } else {
      setWeightingKernel(LINEAR);
    }

    String classifierName = Utils.getOption('B', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -B option.");
    }
    setClassifier(Classifier.forName(classifierName,
				     Utils.partitionOptions(options)));

    setDebug(Utils.getFlag('D', options));
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] classifierOptions = new String [0];
    if ((m_classifier != null) && 
	(m_classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_classifier).getOptions();
    }
    String [] options = new String [classifierOptions.length + 8];
    int current = 0;

    if (getClassifier() != null) {
      options[current++] = "-B";
      options[current++] = getClassifier().getClass().getName();
    }
  
    if (getDebug()) {
      options[current++] = "-D";
    }
    options[current++] = "-W"; options[current++] = "" + getWeightingKernel();
    if (!m_UseAllK) {
      options[current++] = "-K"; options[current++] = "" + getKNN();
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
   * Set the classifier for locally weighted learning. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {

    if (newClassifier instanceof WeightedInstancesHandler) {
      if (newClassifier instanceof DistributionClassifier) {
	m_classifier = (DistributionClassifier)newClassifier;
      } else {
	m_classifier = new DistributionMetaClassifier();
	((DistributionMetaClassifier)m_classifier).setClassifier(newClassifier);
      }
    } else {
      throw new IllegalArgumentException("Classifier must be a WeightedInstancesHandler!");
    }
  }

  /**
   * Get the classifier used as the classifier
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {

    return m_classifier;
  }


  /**
   * Sets whether debugging output should be produced
   *
   * @param debug true if debugging output should be printed
   */
  public void setDebug(boolean debug) {

    m_Debug = debug;
  }
  /**
   * SGts whether debugging output should be produced
   *
   * @return true if debugging output should be printed
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Sets the number of neighbours used for kernel bandwidth setting.
   * The bandwidth is taken as the distance to the kth neighbour.
   *
   * @param knn the number of neighbours included inside the kernel
   * bandwidth, or 0 to specify using all neighbors.
   */
  public void setKNN(int knn) {

    m_kNN = knn;
    if (knn <= 0) {
      m_kNN = 0;
      m_UseAllK = true;
    } else {
      m_UseAllK = false;
    }
  }

  /**
   * Gets the number of neighbours used for kernel bandwidth setting.
   * The bandwidth is taken as the distance to the kth neighbour.
   *
   * @return the number of neighbours included inside the kernel
   * bandwidth, or 0 for all neighbours
   */
  public int getKNN() {

    return m_kNN;
  }

  /**
   * Sets the kernel weighting method to use. Must be one of LINEAR,
   * INVERSE, or GAUSS, other values are ignored.
   *
   * @param kernel the new kernel method to use. Must be one of LINEAR,
   * INVERSE, or GAUSS
   */
  public void setWeightingKernel(int kernel) {

    if ((kernel != LINEAR)
	&& (kernel != INVERSE)
	&& (kernel != GAUSS)) {
      return;
    }
    m_WeightKernel = kernel;
  }

  /**
   * Gets the kernel weighting method to use.
   *
   * @return the new kernel method to use. Will be one of LINEAR,
   * INVERSE, or GAUSS
   */
  public int getWeightingKernel() {

    return m_WeightKernel;
  }

  /**
   * Gets an attributes minimum observed value
   *
   * @param index the index of the attribute
   * @return the minimum observed value
   */
  protected double getAttributeMin(int index) {

    return m_Min[index];
  }

  /**
   * Gets an attributes maximum observed value
   *
   * @param index the index of the attribute
   * @return the maximum observed value
   */
  protected double getAttributeMax(int index) {

    return m_Max[index];
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    if (instances.classIndex() < 0) {
      throw new Exception("No class attribute assigned to instances");
    }

    if (instances.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }

    // Throw away training instances with missing class
    m_Train = new Instances(instances, 0, instances.numInstances());
    m_Train.deleteWithMissingClass();

    // Calculate the minimum and maximum values
    m_Min = new double [m_Train.numAttributes()];
    m_Max = new double [m_Train.numAttributes()];
    for (int i = 0; i < m_Train.numAttributes(); i++) {
      m_Min[i] = m_Max[i] = Double.NaN;
    }
    for (int i = 0; i < m_Train.numInstances(); i++) {
      updateMinMax(m_Train.instance(i));
    }
  }

  /**
   * Adds the supplied instance to the training set
   *
   * @param instance the instance to add
   * @exception Exception if instance could not be incorporated
   * successfully
   */
  public void updateClassifier(Instance instance) throws Exception {

    if (m_Train.equalHeaders(instance.dataset()) == false) {
      throw new Exception("Incompatible instance types");
    }
    if (!instance.classIsMissing()) {
      updateMinMax(instance);
      m_Train.add(instance);
    }
  }
  
  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return preedicted class probability distribution
   * @exception Exception if distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    if (m_Train.numInstances() == 0) {
      throw new Exception("No training instances!");
    }

    updateMinMax(instance);

    // Get the distances to each training instance
    double [] distance = new double [m_Train.numInstances()];
    for (int i = 0; i < m_Train.numInstances(); i++) {
      distance[i] = distance(instance, m_Train.instance(i));
    }
    int [] sortKey = Utils.sort(distance);

    if (m_Debug) {
      System.out.println("Instance Distances");
      for (int i = 0; i < distance.length; i++) {
	System.out.println("" + distance[sortKey[i]]);
      }
    }

    // Determine the bandwidth
    int k = sortKey.length - 1;
    if (!m_UseAllK && (m_kNN < k)) {
      k = m_kNN;
    }
    double bandwidth = distance[sortKey[k]];

    // Check for bandwidth zero
    if (bandwidth <= 0) {
      for (int i = k + 1; i < sortKey.length; i++) {
	if (distance[sortKey[i]] > bandwidth) {
	  bandwidth = distance[sortKey[i]];
	  break;
	}
      }
      if (bandwidth <= 0) {
	throw new Exception("All training instances coincide with test instance!");
      }
    }

    // Rescale the distances by the bandwidth
    for (int i = 0; i < distance.length; i++) {
      distance[i] = distance[i] / bandwidth;
    }

    // Pass the distances through a weighting kernel
    for (int i = 0; i < distance.length; i++) {
      switch (m_WeightKernel) {
      case LINEAR:
	distance[i] = Math.max(1.0001 - distance[i], 0);
	break;
      case INVERSE:
	distance[i] = 1.0 / (1.0 + distance[i]);
	break;
      case GAUSS:
	distance[i] = Math.exp(-distance[i] * distance[i]);
	break;
      }
    }

    if (m_Debug) {
      System.out.println("Instance Weights");
      for (int i = 0; i < distance.length; i++) {
	System.out.println("" + distance[i]);
      }
    }

    // Set the weights on a copy of the training data
    Instances weightedTrain = new Instances(m_Train, 0);
    double sumOfWeights = 0, newSumOfWeights = 0;
    for (int i = 0; i < distance.length; i++) {
      double weight = distance[sortKey[i]];
      if (weight < 1e-20) {
	break;
      }
      Instance newInst = (Instance) m_Train.instance(sortKey[i]).copy();
      sumOfWeights += newInst.weight();
      newSumOfWeights += newInst.weight() * weight;
      newInst.setWeight(newInst.weight() * weight);
      weightedTrain.add(newInst);
    }
    if (m_Debug) {
      System.out.println("Kept " + weightedTrain.numInstances() + " out of "
			 + m_Train.numInstances() + " instances");
    }
    
    // Rescale weights
    for (int i = 0; i < weightedTrain.numInstances(); i++) {
      Instance newInst = weightedTrain.instance(i);
      newInst.setWeight(newInst.weight() * sumOfWeights / newSumOfWeights);
    }

    // Create a weighted classifier
    m_classifier.buildClassifier(weightedTrain);

    if (m_Debug) {
      System.out.println("Classifying test instance: " + instance);
      System.out.println("Built base classifier:\n" 
			 + m_classifier.toString());
    }

    // Return the classifier's predictions
    return m_classifier.distributionForInstance(instance);
  }
 
  /**
   * Returns a description of this classifier.
   *
   * @return a description of this classifier as a string.
   */
  public String toString() {

    if (m_Train == null) {
      return "Locally weighted learning: No model built yet.";
    }
    String result = "Locally weighted learning\n"
      + "===========================\n";

    result += "Using classifier: " + m_classifier.getClass().getName() + "\n";

    switch (m_WeightKernel) {
    case LINEAR:
      result += "Using linear weighting kernels\n";
      break;
    case INVERSE:
      result += "Using inverse-distance weighting kernels\n";
      break;
    case GAUSS:
      result += "Using gaussian weighting kernels\n";
      break;
    }
    result += "Using " + (m_UseAllK ? "all" : "" + m_kNN) + " neighbours";
    return result;
  }

  /**
   * Calculates the distance between two instances
   *
   * @param test the first instance
   * @param train the second instance
   * @return the distance between the two given instances, between 0 and 1
   */          
  private double distance(Instance first, Instance second) {  

    double distance = 0;
    int firstI, secondI;

    for (int p1 = 0, p2 = 0; 
	 p1 < first.numValues() || p2 < second.numValues();) {
      if (p1 >= first.numValues()) {
	firstI = m_Train.numAttributes();
      } else {
	firstI = first.index(p1); 
      }
      if (p2 >= second.numValues()) {
	secondI = m_Train.numAttributes();
      } else {
	secondI = second.index(p2);
      }
      if (firstI == m_Train.classIndex()) {
	p1++; continue;
      } 
      if (secondI == m_Train.classIndex()) {
	p2++; continue;
      } 
      double diff;
      if (firstI == secondI) {
	diff = difference(firstI, 
			  first.valueSparse(p1),
			  second.valueSparse(p2));
	p1++; p2++;
      } else if (firstI > secondI) {
	diff = difference(secondI, 
			  0, second.valueSparse(p2));
	p2++;
      } else {
	diff = difference(firstI, 
			  first.valueSparse(p1), 0);
	p1++;
      }
      distance += diff * diff;
    }
    distance = Math.sqrt(distance);
    return distance;
  }
   
  /**
   * Computes the difference between two given attribute
   * values.
   */
  private double difference(int index, double val1, double val2) {

    switch (m_Train.attribute(index).type()) {
    case Attribute.NOMINAL:
      
      // If attribute is nominal
      if (Instance.isMissingValue(val1) || 
	  Instance.isMissingValue(val2) ||
	  ((int)val1 != (int)val2)) {
	return 1;
      } else {
	return 0;
      }
    case Attribute.NUMERIC:
      // If attribute is numeric
      if (Instance.isMissingValue(val1) || 
	  Instance.isMissingValue(val2)) {
	if (Instance.isMissingValue(val1) && 
	    Instance.isMissingValue(val2)) {
	  return 1;
	} else {
	  double diff;
	  if (Instance.isMissingValue(val2)) {
	    diff = norm(val1, index);
	  } else {
	    diff = norm(val2, index);
	  }
	  if (diff < 0.5) {
	    diff = 1.0 - diff;
	  }
	  return diff;
	}
      } else {
	return norm(val1, index) - norm(val2, index);
      }
    default:
      return 0;
    }
  }

  /**
   * Normalizes a given value of a numeric attribute.
   *
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  private double norm(double x,int i) {

    if (Double.isNaN(m_Min[i]) || Utils.eq(m_Max[i], m_Min[i])) {
      return 0;
    } else {
      return (x - m_Min[i]) / (m_Max[i] - m_Min[i]);
    }
  }
                      
  /**
   * Updates the minimum and maximum values for all the attributes
   * based on a new instance.
   *
   * @param instance the new instance
   */
  private void updateMinMax(Instance instance) {  

    for (int j = 0; j < m_Train.numAttributes(); j++) {
      if (!instance.isMissing(j)) {
	if (Double.isNaN(m_Min[j])) {
	  m_Min[j] = instance.value(j);
	  m_Max[j] = instance.value(j);
	} else if (instance.value(j) < m_Min[j]) {
	  m_Min[j] = instance.value(j);
	} else if (instance.value(j) > m_Max[j]) {
	  m_Max[j] = instance.value(j);
	}
      }
    }
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(
	    new LWL(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}





