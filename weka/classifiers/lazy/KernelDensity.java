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
 *    KernelDensity.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.lazy;

import weka.classifiers.Classifier;
import weka.classifiers.DistributionClassifier;
import weka.classifiers.Evaluation;
import java.io.*;
import java.util.*;
import weka.core.*;


/**
 * Class for building and using a very simple kernel density classifier.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.8 $
 */
public class KernelDensity extends DistributionClassifier {

  /** The number of instances in each class (null if class numeric). */
  private double [] m_Counts;
  
  /** The instances used for "training". */
  private Instances m_Instances;

  /** The minimum values for numeric attributes. */
  private double [] m_MinArray;

  /** The maximum values for numeric attributes. */
  private double [] m_MaxArray;
 
  /** Constant */
  private static double CO = Math.sqrt(2 * Math.PI);

  /**
   * Returns value for normal kernel
   *
   * @param x the argument to the kernel function
   * @return the value for a normal kernel
   */
  private double normalKernel(double x) {

    return Math.exp(-(x * x) / 2) / CO;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    if (!instances.classAttribute().isNominal()) {
      throw new UnsupportedClassTypeException("Class attribute has to be nominal!");
    }
    if (instances.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }
    m_Instances = instances;
    m_MinArray = new double [m_Instances.numAttributes()];
    m_MaxArray = new double [m_Instances.numAttributes()];
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      m_MinArray[i] = m_MaxArray[i] = Double.NaN;
    }
    m_Counts = new double[m_Instances.numClasses()];
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      Instance inst = m_Instances.instance(i);
      if (!inst.classIsMissing()) {
	m_Counts[(int) inst.classValue()] += inst.weight();
      }
      updateMinMax(inst);
    }
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if the probabilities can't be computed
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    
    double[] probs = new double[m_Instances.numClasses()];
    double prob, sum, temp;
    double lowerBound = Math.pow(Double.MIN_VALUE, 1.0 / 
				 (instance.numAttributes() - 1.0)); 

    sum = Math.sqrt(Utils.sum(m_Counts));
    updateMinMax(instance);
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      Instance inst = m_Instances.instance(i);
      if (!inst.classIsMissing()) {
	prob = 1;
	for (int j = 0; j < m_Instances.numAttributes(); j++) {
	  if (j != m_Instances.classIndex()) {
	    temp = normalKernel(distance(instance, inst, j) * sum) * sum;
	    if (temp < lowerBound) {
	      prob *= lowerBound;
	    } else {
	      prob *= temp;
	    }
	  }
	}
	probs[(int) inst.classValue()] += prob;
      }
    }
    Utils.normalize(probs);

    return probs;
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {

    return "Kernel Density Estimator";
  }

  /**
   * Calculates the distance between two instances according to one attribute
   *
   * @param test the first instance
   * @param train the second instance
   * @return the distance between the two given instances
   */
  private double distance(Instance first, Instance second, int i) {
    
    double diff, distance = 0;

    if (m_Instances.attribute(i).isNominal()) {
      
      // If attribute is nominal
      if (first.isMissing(i) || second.isMissing(i) ||
	  ((int)first.value(i) != (int)second.value(i))) {
	distance += 1;
      }
    } else {
      
      // If attribute is numeric
      if (first.isMissing(i) || second.isMissing(i)) {
	if (first.isMissing(i) && second.isMissing(i)) {
	  diff = 1;
	} else {
	  if (second.isMissing(i)) {
	    diff = norm(first.value(i), i);
	  } else {
	    diff = norm(second.value(i), i);
	  }
	  if (diff < 0.5) {
	    diff = 1.0 - diff;
	  }
	}
      } else {
	diff = norm(first.value(i), i) - norm(second.value(i), i);
      }
      distance += diff;
    }
      
    return distance;
  }
    
  /**
   * Normalizes a given value of a numeric attribute.
   *
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  private double norm(double x, int i) {

    if (Double.isNaN(m_MinArray[i]) || Utils.eq(m_MaxArray[i],m_MinArray[i])) {
      return 0;
    } else {
      return (x - m_MinArray[i]) / (m_MaxArray[i] - m_MinArray[i]);
    }
  }

  /**
   * Updates the minimum and maximum values for all the attributes
   * based on a new instance.
   *
   * @param instance the new instance
   */
  private void updateMinMax(Instance instance) {
    
    for (int j = 0; j < m_Instances.numAttributes(); j++) {
      if ((m_Instances.attribute(j).isNumeric()) 
	  && (!instance.isMissing(j))) {
	if (Double.isNaN(m_MinArray[j])) {
	  m_MinArray[j] = instance.value(j);
	  m_MaxArray[j] = instance.value(j);
	} else {
	  if (instance.value(j) < m_MinArray[j]) {
	    m_MinArray[j] = instance.value(j);
	  } else {
	    if (instance.value(j) > m_MaxArray[j]) {
	      m_MaxArray[j] = instance.value(j);
	    }
	  }
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
      System.out.println(Evaluation.evaluateModel(new KernelDensity(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}




