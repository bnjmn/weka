/*
 *    NaiveBayesSimple.java
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
 * Class for building and using a Naive Bayes classifier.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0 - ?? ??? 1998 - Initial version (Eibe)
 */
public class NaiveBayesSimple extends DistributionClassifier {

  // =================
  // Private variables
  // =================

  /** All the counts for nominal attributes. */
  private double [][][] theCounts;
  
  /** The means for numeric attributes. */
  private double [][] theMeans;

  /** The standard deviations for numeric attributes. */
  private double [][] theDevs;

  /** The prior probabilities of the classes. */
  private double [] thePriors;

  /** The instances used for training. */
  private Instances theInstances;

  /** Constant for normal distribution. */
  private static double normConst = Math.sqrt(2 * Math.PI);

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

    int attIndex = 0;
    double sum;
    
    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    if (instances.classAttribute().isNumeric()) {
      throw new Exception("Naive Bayes: Class is numeric!");
    }
    
    theInstances = new Instances(instances, 0, 0);
    
    // Reserve space
    
    theCounts = new double[instances.numClasses()]
      [instances.numAttributes() - 1][0];
    theMeans = new double[instances.numClasses()]
      [instances.numAttributes() - 1];
    theDevs = new double[instances.numClasses()]
      [instances.numAttributes() - 1];
    thePriors = new double[instances.numClasses()];
    Enumeration enum = instances.enumerateAttributes();
    while (enum.hasMoreElements()) {
      Attribute attribute = (Attribute) enum.nextElement();
      if (attribute.isNominal()) {
	for (int j = 0; j < instances.numClasses(); j++) {
	  theCounts[j][attIndex] = new double[attribute.numValues()];
	}
      } else {
	for (int j = 0; j < instances.numClasses(); j++) {
	  theCounts[j][attIndex] = new double[1];
	}
      }
      attIndex++;
    }
    
    // Compute counts and sums
    
    Enumeration enumInsts = instances.enumerateInstances();
    while (enumInsts.hasMoreElements()) {
      Instance instance = (Instance) enumInsts.nextElement();
      if (!instance.classIsMissing()) {
	Enumeration enumAtts = instances.enumerateAttributes();
	attIndex = 0;
	while (enumAtts.hasMoreElements()) {
	  Attribute attribute = (Attribute) enumAtts.nextElement();
	  if (!instance.isMissing(attribute)) {
	    if (attribute.isNominal()) {
	      theCounts[(int)instance.classValue()][attIndex]
		[(int)instance.value(attribute)]++;
	    } else {
	      theMeans[(int)instance.classValue()][attIndex] +=
		instance.value(attribute);
	      theCounts[(int)instance.classValue()][attIndex][0]++;
	    }
	  }
	  attIndex++;
	}
	thePriors[(int)instance.classValue()]++;
      }
    }
    
    // Compute means
    
    Enumeration enumAtts = instances.enumerateAttributes();
    attIndex = 0;
    while (enumAtts.hasMoreElements()) {
      Attribute attribute = (Attribute) enumAtts.nextElement();
      if (attribute.isNumeric()) {
	for (int j = 0; j < instances.numClasses(); j++) {
	  if (theCounts[j][attIndex][0] < 2) {
	    throw new Exception("attribute " + attribute.name() +
				": less than two values for class " +
				instances.classAttribute().value(j));
	  }
	  theMeans[j][attIndex] /= theCounts[j][attIndex][0];
	}
      }
      attIndex++;
    }    
    
    // Compute standard deviations
    
    enumInsts = instances.enumerateInstances();
    while (enumInsts.hasMoreElements()) {
      Instance instance = 
	(Instance) enumInsts.nextElement();
      if (!instance.classIsMissing()) {
	enumAtts = instances.enumerateAttributes();
	attIndex = 0;
	while (enumAtts.hasMoreElements()) {
	  Attribute attribute = (Attribute) enumAtts.nextElement();
	  if (!instance.isMissing(attribute)) {
	    if (attribute.isNumeric()) {
	      theDevs[(int)instance.classValue()][attIndex] +=
		(theMeans[(int)instance.classValue()][attIndex]-
		 instance.value(attribute))*
		(theMeans[(int)instance.classValue()][attIndex]-
		 instance.value(attribute));
	    }
	  }
	  attIndex++;
	}
      }
    }
    enumAtts = instances.enumerateAttributes();
    attIndex = 0;
    while (enumAtts.hasMoreElements()) {
      Attribute attribute = (Attribute) enumAtts.nextElement();
      if (attribute.isNumeric()) {
	for (int j = 0; j < instances.numClasses(); j++) {
	  if (theDevs[j][attIndex] <= 0) {
	    throw new Exception("attribute " + attribute.name() +
				": standard deviation is 0 for class " +
				instances.classAttribute().value(j));
	  }
	  else {
	    theDevs[j][attIndex] /= theCounts[j][attIndex][0] - 1;
	    theDevs[j][attIndex] = Math.sqrt(theDevs[j][attIndex]);
	  }
	}
      }
      attIndex++;
    } 
    
    // Normalize counts
    
    enumAtts = instances.enumerateAttributes();
    attIndex = 0;
    while (enumAtts.hasMoreElements()) {
      Attribute attribute = (Attribute) enumAtts.nextElement();
      if (attribute.isNominal()) {
	for (int j = 0; j < instances.numClasses(); j++) {
	  sum = Utils.sum(theCounts[j][attIndex]);
	  for (int i = 0; i < attribute.numValues(); i++) {
	    theCounts[j][attIndex][i] =
	      (theCounts[j][attIndex][i] + 1) 
	      / (sum + (double)attribute.numValues());
	  }
	}
      }
      attIndex++;
    }
    
    // Normalize priors
    sum = Utils.sum(thePriors);
    for (int j = 0; j < instances.numClasses(); j++)
      thePriors[j] = (thePriors[j] + 1) 
	/ (sum + (double)instances.numClasses());
  }

  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be classified
   * @return index of the predicted class
   * @exception Exception if the instance can't be classified
   */
  public double classifyInstance(Instance instance) throws Exception {

    return Utils.maxIndex(distributionForInstance(instance));
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if distribution can't be computed
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    
    double [] probs = new double[instance.numClasses()];
    int attIndex;
    
    for (int j = 0; j < instance.numClasses(); j++) {
      probs[j] = 1;
      Enumeration enumAtts = instance.enumerateAttributes();
      attIndex = 0;
      while (enumAtts.hasMoreElements()) {
	Attribute attribute = (Attribute) enumAtts.nextElement();
	if (!instance.isMissing(attribute)) {
	  if (attribute.isNominal()) {
	    probs[j] *= theCounts[j][attIndex][(int)instance.value(attribute)];
	  } else {
	    probs[j] *= normalDens(instance.value(attribute),
				   theMeans[j][attIndex],
				   theDevs[j][attIndex]);}
	}
	attIndex++;
      }
      probs[j] *= thePriors[j];
    }
    
    // Normalize probabilities
    Utils.normalize(probs);

    return probs;
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {
    
    try {
      StringBuffer text = new StringBuffer();
      int attIndex;
      
      text.append("Naive Bayes");
      
      for (int i = 0; i < theInstances.numClasses(); i++) {
	text.append("\n\nClass " + theInstances.classAttribute().value(i) 
		    + ": P(C) = " 
		    + Utils.doubleToString(thePriors[i], 10, 8)
		    + "\n\n");
	Enumeration enumAtts = theInstances.enumerateAttributes();
	attIndex = 0;
	while (enumAtts.hasMoreElements()) {
	  Attribute attribute = (Attribute) enumAtts.nextElement();
	  text.append("Attribute " + attribute.name() + "\n");
	  if (attribute.isNominal()) {
	    for (int j = 0; j < attribute.numValues(); j++) {
	      text.append(attribute.value(j) + "\t");
	    }
	    text.append("\n");
	    for (int j = 0; j < attribute.numValues(); j++)
	      text.append(Utils.
			  doubleToString(theCounts[i][attIndex][j], 10, 8)
			  + "\t");
	  } else {
	    text.append("Mean: " + Utils.
			doubleToString(theMeans[i][attIndex], 10, 8) + "\t");
	    text.append("Standard Deviation: " 
			+ Utils.doubleToString(theDevs[i][attIndex], 10, 8));
	  }
	  text.append("\n\n");
	  attIndex++;
	}
      }
      
      return text.toString();
    } catch (Exception e) {
      return "Can't print Naive Bayes classifier!";
    }
  }
 
  // ===============
  // Private methods
  // ===============

  /**
   * Density function of normal distribution.
   */
  private double normalDens(double x, double mean, double stdDev) {
    
    double diff = x - mean;
    
    return (1 / (normConst * stdDev)) 
      * Math.exp(-(diff * diff / (2 * stdDev * stdDev)));
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

    Classifier scheme;

    try {
      scheme = new NaiveBayesSimple();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}


