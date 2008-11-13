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
 *    NaiveBayesSimple.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.bayes;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for building and using a simple Naive Bayes classifier.
 * Numeric attributes are modelled by a normal distribution. For more
 * information, see<p>
 *
 * Richard Duda and Peter Hart (1973).<i>Pattern
 * Classification and Scene Analysis</i>. Wiley, New York.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.13.2.2 $ 
*/
public class NaiveBayesSimple extends Classifier {

  /** All the counts for nominal attributes. */
  protected double [][][] m_Counts;
  
  /** The means for numeric attributes. */
  protected double [][] m_Means;

  /** The standard deviations for numeric attributes. */
  protected double [][] m_Devs;

  /** The prior probabilities of the classes. */
  protected double [] m_Priors;

  /** The instances used for training. */
  protected Instances m_Instances;

  /** Constant for normal distribution. */
  protected static double NORM_CONST = Math.sqrt(2 * Math.PI);

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for building and using a simple Naive Bayes classifier."
      +"Numeric attributes are modelled by a normal distribution. For more "
      +"information, see\n\n"
      +"Richard Duda and Peter Hart (1973). Pattern "
      +"Classification and Scene Analysis. Wiley, New York.";
  }

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
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }
    if (instances.classAttribute().isNumeric()) {
      throw new UnsupportedClassTypeException("Naive Bayes: Class is numeric!");
    }
    
    m_Instances = new Instances(instances, 0);
    
    // Reserve space
    m_Counts = new double[instances.numClasses()]
      [instances.numAttributes() - 1][0];
    m_Means = new double[instances.numClasses()]
      [instances.numAttributes() - 1];
    m_Devs = new double[instances.numClasses()]
      [instances.numAttributes() - 1];
    m_Priors = new double[instances.numClasses()];
    Enumeration enu = instances.enumerateAttributes();
    while (enu.hasMoreElements()) {
      Attribute attribute = (Attribute) enu.nextElement();
      if (attribute.isNominal()) {
	for (int j = 0; j < instances.numClasses(); j++) {
	  m_Counts[j][attIndex] = new double[attribute.numValues()];
	}
      } else {
	for (int j = 0; j < instances.numClasses(); j++) {
	  m_Counts[j][attIndex] = new double[1];
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
	      m_Counts[(int)instance.classValue()][attIndex]
		[(int)instance.value(attribute)]++;
	    } else {
	      m_Means[(int)instance.classValue()][attIndex] +=
		instance.value(attribute);
	      m_Counts[(int)instance.classValue()][attIndex][0]++;
	    }
	  }
	  attIndex++;
	}
	m_Priors[(int)instance.classValue()]++;
      }
    }
    
    // Compute means
    Enumeration enumAtts = instances.enumerateAttributes();
    attIndex = 0;
    while (enumAtts.hasMoreElements()) {
      Attribute attribute = (Attribute) enumAtts.nextElement();
      if (attribute.isNumeric()) {
	for (int j = 0; j < instances.numClasses(); j++) {
	  if (m_Counts[j][attIndex][0] < 2) {
	    throw new Exception("attribute " + attribute.name() +
				": less than two values for class " +
				instances.classAttribute().value(j));
	  }
	  m_Means[j][attIndex] /= m_Counts[j][attIndex][0];
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
	      m_Devs[(int)instance.classValue()][attIndex] +=
		(m_Means[(int)instance.classValue()][attIndex]-
		 instance.value(attribute))*
		(m_Means[(int)instance.classValue()][attIndex]-
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
	  if (m_Devs[j][attIndex] <= 0) {
	    throw new Exception("attribute " + attribute.name() +
				": standard deviation is 0 for class " +
				instances.classAttribute().value(j));
	  }
	  else {
	    m_Devs[j][attIndex] /= m_Counts[j][attIndex][0] - 1;
	    m_Devs[j][attIndex] = Math.sqrt(m_Devs[j][attIndex]);
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
	  sum = Utils.sum(m_Counts[j][attIndex]);
	  for (int i = 0; i < attribute.numValues(); i++) {
	    m_Counts[j][attIndex][i] =
	      (m_Counts[j][attIndex][i] + 1) 
	      / (sum + (double)attribute.numValues());
	  }
	}
      }
      attIndex++;
    }
    
    // Normalize priors
    sum = Utils.sum(m_Priors);
    for (int j = 0; j < instances.numClasses(); j++)
      m_Priors[j] = (m_Priors[j] + 1) 
	/ (sum + (double)instances.numClasses());
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
	    probs[j] *= m_Counts[j][attIndex][(int)instance.value(attribute)];
	  } else {
	    probs[j] *= normalDens(instance.value(attribute),
				   m_Means[j][attIndex],
				   m_Devs[j][attIndex]);}
	}
	attIndex++;
      }
      probs[j] *= m_Priors[j];
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

    if (m_Instances == null) {
      return "Naive Bayes (simple): No model built yet.";
    }
    try {
      StringBuffer text = new StringBuffer("Naive Bayes (simple)");
      int attIndex;
      
      for (int i = 0; i < m_Instances.numClasses(); i++) {
	text.append("\n\nClass " + m_Instances.classAttribute().value(i) 
		    + ": P(C) = " 
		    + Utils.doubleToString(m_Priors[i], 10, 8)
		    + "\n\n");
	Enumeration enumAtts = m_Instances.enumerateAttributes();
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
			  doubleToString(m_Counts[i][attIndex][j], 10, 8)
			  + "\t");
	  } else {
	    text.append("Mean: " + Utils.
			doubleToString(m_Means[i][attIndex], 10, 8) + "\t");
	    text.append("Standard Deviation: " 
			+ Utils.doubleToString(m_Devs[i][attIndex], 10, 8));
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

  /**
   * Density function of normal distribution.
   */
  protected double normalDens(double x, double mean, double stdDev) {
    
    double diff = x - mean;
    
    return (1 / (NORM_CONST * stdDev)) 
      * Math.exp(-(diff * diff / (2 * stdDev * stdDev)));
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
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


