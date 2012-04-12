/*
 *    KernelDensity.java
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
 * Class for building and using a kernel density classifier.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class KernelDensity extends DistributionClassifier {

  // =================
  // Private variables
  // =================

  /** The number of instances in each class (null if class numeric). */
  private double [] theCounts;
  
  /** The instances used for "training". */
  private Instances theInstances;

  /** The minimum values for numeric attributes. */
  private double [] minArray;

  /** The maximum values for numeric attributes. */
  private double [] maxArray;
 
  /** Constant */
  private static double CO = Math.sqrt(2 * Math.PI);

  // ===============
  // Public methods.
  // ===============

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
      throw new Exception("Class attribute has to be nominal!");
    }
    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    theInstances = instances;
    minArray = new double [theInstances.numAttributes()];
    maxArray = new double [theInstances.numAttributes()];
    for (int i = 0; i < theInstances.numAttributes(); i++) {
      minArray[i] = maxArray[i] = Double.NaN;
    }
    theCounts = new double[theInstances.numClasses()];
    for (int i = 0; i < theInstances.numInstances(); i++) {
      Instance inst = theInstances.instance(i);
      if (!inst.classIsMissing()) {
	theCounts[(int) inst.classValue()] += inst.weight();
      }
      updateMinMax(inst);
    }
  }

  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be classified
   * @return index of the predicted class
   * @exception Exception if instance can't be classified
   */
  public double classifyInstance(Instance instance) throws Exception {

    return Utils.maxIndex(distributionForInstance(instance));
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if the probabilities can't be computed
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    
    double[] probs = new double[theInstances.numClasses()];
    double prob, sum;

    sum = Math.sqrt(Utils.sum(theCounts));
    updateMinMax(instance);
    for (int i = 0; i < theInstances.numInstances(); i++) {
      Instance inst = theInstances.instance(i);
      if (!inst.classIsMissing()) {
	prob = 1;
	for (int j = 0; j < theInstances.numAttributes(); j++) {
	  if (j != theInstances.classIndex()) {
	    prob *= normalKernel(distance(instance, inst, j) * sum) * sum;  
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

  // ===============
  // Private methods
  // ===============

  /**
   * Calculates the distance between two instances according to one attribute
   *
   * @param test the first instance
   * @param train the second instance
   * @return the distance between the two given instances
   */          

  private double distance(Instance first, Instance second, int i) {
    
    double diff, distance = 0;

    if (theInstances.attribute(i).isNominal()) {
      
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

    if (Double.isNaN(minArray[i]) || Utils.eq(maxArray[i],minArray[i])) {
      return 0;
    } else {
      return (x - minArray[i]) / (maxArray[i] - minArray[i]);
    }
  }

  /**
   * Updates the minimum and maximum values for all the attributes
   * based on a new instance.
   *
   * @param instance the new instance
   */
  private void updateMinMax(Instance instance) {
    
    for (int j = 0; j < theInstances.numAttributes(); j++) {
      if ((theInstances.attribute(j).isNumeric()) 
	  && (!instance.isMissing(j))) {
	if (Double.isNaN(minArray[j])) {
	  minArray[j] = instance.value(j);
	  maxArray[j] = instance.value(j);
	} else {
	  if (instance.value(j) < minArray[j]) {
	    minArray[j] = instance.value(j);
	  } else {
	    if (instance.value(j) > maxArray[j]) {
	      maxArray[j] = instance.value(j);
	    }
	  }
	}
      }
    }
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
      System.out.println(Evaluation.evaluateModel(new KernelDensity(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}




