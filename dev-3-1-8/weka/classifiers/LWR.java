/*
 *    LWR.java
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

/**
 * Locally-weighted regression. Uses an instance-based algorithm to assign
 * instance weights which are then used by a linear regression model.  For
 * more information, see<p>
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
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.7 $
 */
public class LWR extends Classifier 
  implements OptionHandler, UpdateableClassifier, 
  WeightedInstancesHandler {
  
  /** The training instances used for classification. */
  protected Instances m_Train;

  /** The minimum values for numeric attributes. */
  protected double [] m_Min;

  /** The maximum values for numeric attributes. */
  protected double [] m_Max;
    
  /** True if debugging output should be printed */
  protected boolean m_Debug;

  /** The number of neighbours used to select the kernel bandwidth */
  protected int m_kNN = 5;

  /** The weighting kernel method currently selected */
  protected int m_WeightKernel = LINEAR;

  /** True if m_kNN should be set to all instances */
  protected boolean m_UseAllK = true;

  /* The available kernel weighting methods */
  protected static final int LINEAR  = 0;
  protected static final int INVERSE = 1;
  protected static final int GAUSS   = 2;

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(2);
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

    setDebug(Utils.getFlag('D', options));
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [5];
    int current = 0;

    if (getDebug()) {
      options[current++] = "-D";
    }
    options[current++] = "-W"; options[current++] = "" + getWeightingKernel();
    if (!m_UseAllK) {
      options[current++] = "-K"; options[current++] = "" + getKNN();
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
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
    if (instances.classAttribute().type() != Attribute.NUMERIC) {
      throw new Exception("Class attribute must be numeric");
    }

    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
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
   * Predicts the class value for the given test instance.
   *
   * @param instance the instance to be classified
   * @return the predicted class value
   * @exception Exception if an error occurred during the prediction
   */
  public double classifyInstance(Instance instance) throws Exception {

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
    if (bandwidth == distance[sortKey[0]]) {
      for (int i = k; i < sortKey.length; i++) {
	if (distance[sortKey[i]] > bandwidth) {
	  bandwidth = distance[sortKey[i]];
	  break;
	}
      }
      if (bandwidth == distance[sortKey[0]]) {
	bandwidth *= 10;  // Include them all
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
    for (int i = 0; i < distance.length; i++) {
      double weight = distance[sortKey[i]];
      if (weight < 1e-20) {
	break;
      }
      Instance newInst = (Instance) m_Train.instance(sortKey[i]).copy();
      newInst.setWeight(newInst.weight() * weight);
      weightedTrain.add(newInst);
    }
    if (m_Debug) {
      System.out.println("Kept " + weightedTrain.numInstances() + " out of "
			 + m_Train.numInstances() + " instances");
    }
    
    // Create a weighted linear regression
    LinearRegression weightedRegression = new LinearRegression();
    weightedRegression.buildClassifier(weightedTrain);

    if (m_Debug) {
      System.out.println("Classifying test instance: " + instance);
      System.out.println("Built regression model:\n" 
			 + weightedRegression.toString());
    }
    // Return the linear regression's prediction
    return weightedRegression.classifyInstance(instance);
  }
 
  /**
   * Returns a description of this classifier.
   *
   * @return a description of this classifier as a string.
   */
  public String toString() {

    if (m_Train == null) {
      return "Locally weighted regression: No model built yet.";
    }
    String result = "Locally weighted regression\n"
      + "===========================\n";

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

    double diff, distance = 0;
    int numAttribsUsed = 0;
    for(int i = 0; i < m_Train.numAttributes(); i++) { 
      if (i == m_Train.classIndex()) {
	continue;
      }
      switch (m_Train.attribute(i).type()) {
      case Attribute.NOMINAL:
	// If attribute is nominal
	numAttribsUsed++;
	if (first.isMissing(i) || second.isMissing(i) ||
	    ((int)first.value(i) != (int)second.value(i))) {
	  diff = 1;
	} else {
	  diff = 0;
	}
	break;
      case Attribute.NUMERIC:
	// If attribute is numeric
	numAttribsUsed++;	
	if (first.isMissing(i) || second.isMissing(i)) {
	  if (first.isMissing(i) && second.isMissing(i)) {
	    diff = 1;
	  } else {
	    if (second.isMissing(i)) {
	      diff = norm(first.value(i),i);
	    } else {
	      diff = norm(second.value(i),i);
	    }
	    if (diff < 0.5) {
	      diff = 1.0-diff;
	    }
	  }
	} else {
	  diff = norm(first.value(i),i) - norm(second.value(i),i);
	}
	break;
      default:
	diff = 0;
	break;
      }
      distance += diff * diff;
    }
    return Math.sqrt(distance / numAttribsUsed);
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
	    new LWR(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}





