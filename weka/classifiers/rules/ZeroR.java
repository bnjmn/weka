/*
 *    ZeroR.java
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
 * Class for building and using a 0R classifier. Predicts the mean
 * (for a numeric class) or the mode (for a nominal class).
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class ZeroR extends DistributionClassifier 
  implements WeightedInstancesHandler {

  // =================
  // Private variables
  // =================

  /** The class value 0R predicts. */
  private double theClassValue;

  /** The number of instances in each class (null if class numeric). */
  private double [] theCounts;
  
  /** The instances used for "training". */
  private Instances theInstances;

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

    theInstances = instances;
    theClassValue = 0;
    switch (instances.classAttribute().type()) {
    case Attribute.NUMERIC:
      theCounts = null;
      break;
    case Attribute.NOMINAL:
      theCounts = new double [instances.numClasses()];
      for (int i = 0; i < theCounts.length; i++) {
	theCounts[i] = 1;
      }
      break;
    default:
      throw new Exception("ZeroR can only handle nominal and numeric class"
			  + " attributes.");
    }
    Enumeration enum = instances.enumerateInstances();
    while (enum.hasMoreElements()) {
      Instance instance = (Instance) enum.nextElement();
      if (!instance.classIsMissing()) {
	if (instances.classAttribute().isNominal()) {
	  theCounts[(int)instance.classValue()] += instance.weight();
	} else {
	  theClassValue += instance.weight() * instance.classValue();
	}
      }
    }
    if (instances.classAttribute().isNumeric()) {
      theClassValue /= instances.sumOfWeights();
    } else {
      theClassValue = Utils.maxIndex(theCounts);
      Utils.normalize(theCounts);
    }
  }

  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be classified
   * @return index of the predicted class
   */
  public double classifyInstance(Instance instance) {

    return theClassValue;
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if class is numeric
   */
  public double [] distributionForInstance(Instance instance) 
       throws Exception {
	 
    if (theCounts == null) {
      double[] result = new double[1];
      result[0] = theClassValue;
      return result;
    } else {
      return (double []) theCounts.clone();
    }
  }
  
  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {

    try { 
      if (theCounts == null) {
	return "ZeroR predicts class value: " + theClassValue;
      } else {
	return "ZeroR predicts class value: " +
	  theInstances.classAttribute().value((int) theClassValue);
      }
    } catch (Exception e) {
      return "Can't print classifier!";
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
      System.out.println(Evaluation.evaluateModel(new ZeroR(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}


