/*
 *    DecisionStump.java
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
 * Class for building and using a decision stump.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class DecisionStump extends DistributionClassifier 
  implements WeightedInstancesHandler {

  /** The attribute used for classification. */
  private int m_AttIndex;

  /** The split point (index respectively). */
  private double m_SplitPoint;

  /** The distribution of class values or the means in each subset. */
  private double[][] m_Distribution;

  /** The instances used for training. */
  private Instances theInstances;

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {
    
    double bestVal = Double.MAX_VALUE, currVal;
    double bestPoint = -Double.MAX_VALUE, sum;
    int bestAtt = -1, numClasses;

    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }

    double[][] bestDist = new double[3][instances.numClasses()];

    theInstances = new Instances(instances);
    theInstances.deleteWithMissingClass();
    if (theInstances.classAttribute().isNominal()) {
      numClasses = theInstances.numClasses();
    } else {
      numClasses = 1;
    }

    // For each attribute
    for (int i = 0; i < theInstances.numAttributes(); i++) {
      if (i != theInstances.classIndex()) {
	
	// Reserve space for distribution.
	m_Distribution = new double[3][numClasses];

	// Compute value of criterion for best split on attribute
	if (theInstances.attribute(i).isNominal()) {
	  currVal = findSplitNominal(i);
	} else {
	  currVal = findSplitNumeric(i);
	}
	if (Utils.sm(currVal, bestVal)) {
	  bestVal = currVal;
	  bestAtt = i;
	  bestPoint = m_SplitPoint;
	  for (int j = 0; j < 3; j++) {
	    System.arraycopy(m_Distribution[j], 0, bestDist[j], 0, 
			     numClasses);
	  }
	}
      }
    }
    
    // Set attribute, split point and distribution.
    m_AttIndex = bestAtt;
    m_SplitPoint = bestPoint;
    m_Distribution = bestDist;
    if (theInstances.classAttribute().isNominal()) {
      for (int i = 0; i < m_Distribution.length; i++) {
	Utils.normalize(m_Distribution[i]);
      }
    }
    
    // Save memory
    theInstances = new Instances(theInstances, 0);
  }

  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be classified
   * @return index of the predicted class
   * @exception Exception if the instance can't be classified
   */
  public double classifyInstance(Instance instance) throws Exception {

    if (theInstances.classAttribute().isNominal()) {
      return Utils.maxIndex(distributionForInstance(instance));
    } else {
      return m_Distribution[whichSubset(instance)][0];
    }
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if distribution can't be computed
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    return m_Distribution[whichSubset(instance)];
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString(){
    
    try {
      StringBuffer text = new StringBuffer();
      
      text.append("Decision Stump\n\n");
      text.append("Classifications\n\n");
      Attribute att = theInstances.attribute(m_AttIndex);
      if (att.isNominal()) {
	text.append(att.name() + " = " + att.value((int)m_SplitPoint) + 
		    " : ");
	text.append(printClass(m_Distribution[0]));
	text.append(att.name() + " != " + att.value((int)m_SplitPoint) + 
		    " : ");
	text.append(printClass(m_Distribution[1]));
      } else {
	text.append(att.name() + " <= " + m_SplitPoint + " : ");
	text.append(printClass(m_Distribution[0]));
	text.append(att.name() + " > " + m_SplitPoint + " : ");
	text.append(printClass(m_Distribution[1]));
      }
      text.append(att.name() + " is missing : ");
      text.append(printClass(m_Distribution[2]));

      if (theInstances.classAttribute().isNominal()) {
	text.append("\nClass distributions\n\n");
	if (att.isNominal()) {
	  text.append(att.name() + " = " + att.value((int)m_SplitPoint) + 
		      "\n");
	  text.append(printDist(m_Distribution[0]));
	  text.append(att.name() + " != " + att.value((int)m_SplitPoint) + 
		      "\n");
	  text.append(printDist(m_Distribution[1]));
	} else {
	  text.append(att.name() + " <= " + m_SplitPoint + "\n");
	  text.append(printDist(m_Distribution[0]));
	  text.append(att.name() + " > " + m_SplitPoint + "\n");
	  text.append(printDist(m_Distribution[1]));
	}
	text.append(att.name() + " is missing\n");
	text.append(printDist(m_Distribution[2]));
      }

      return text.toString();
    } catch (Exception e) {
      return "Can't print decision stump classifier!";
    }
  }

  /** 
   * Prints a class distribution.
   *
   * @param dist the class distribution to print
   * @return the distribution as a string
   * @exception Exception if distribution can't be printed
   */
  private String printDist(double[] dist) throws Exception {

    StringBuffer text = new StringBuffer();
    
    if (theInstances.classAttribute().isNominal()) {
      for (int i = 0; i < theInstances.numClasses(); i++) {
	text.append(theInstances.classAttribute().value(i) + "\t");
      }
      text.append("\n");
      for (int i = 0; i < theInstances.numClasses(); i++) {
	text.append(dist[i] + "\t");
      }
      text.append("\n");
    }
    
    return text.toString();
  }

  /** 
   * Prints a classification.
   *
   * @param dist the class distribution
   * @return the classificationn as a string
   * @exception Exception if the classification can't be printed
   */
  private String printClass(double[] dist) throws Exception {

    StringBuffer text = new StringBuffer();
    
    if (theInstances.classAttribute().isNominal()) {
      text.append(theInstances.classAttribute().value(Utils.maxIndex(dist)));
    } else {
      text.append(dist[0]);
    }
    
    return text.toString() + "\n";
  }

  /**
   * Finds best split for nominal attribute and returns value.
   *
   * @param index attribute index
   * @return value of criterion for the best split
   * @exception Exception if something goes wrong
   */
  private double findSplitNominal(int index) throws Exception {

    if (theInstances.classAttribute().isNominal()) {
      return findSplitNominalNominal(index);
    } else {
      return findSplitNominalNumeric(index);
    }
  }

  /**
   * Finds best split for nominal attribute and nominal class
   * and returns value.
   *
   * @param index attribute index
   * @return value of criterion for the best split
   * @exception Exception if something goes wrong
   */
  private double findSplitNominalNominal(int index) throws Exception {

    double bestVal = Double.MAX_VALUE, currVal;
    double[][] counts = new double[theInstances.attribute(index).numValues() 
				  + 1][theInstances.numClasses()];
    double[] sumCounts = new double[theInstances.numClasses()];
    double[][] bestDist = new double[3][theInstances.numClasses()];
    int numMissing = 0;

    // Compute counts for all the values
    for (int i = 0; i < theInstances.numInstances(); i++) {
      Instance inst = theInstances.instance(i);
      if (inst.isMissing(index)) {
	numMissing++;
	counts[theInstances.attribute(index).numValues()]
	  [(int)inst.classValue()] += inst.weight();
      } else {
	counts[(int)inst.value(index)][(int)inst.classValue()] += inst
	  .weight();
      }
    }

    // Compute sum of counts
    for (int i = 0; i < theInstances.attribute(index).numValues() + 1; i++) {
      for (int j = 0; j < theInstances.numClasses(); j++) {
	sumCounts[j] += counts[i][j];
      }
    }
    
    // Make split counts for each possible split and evaluate
    System.arraycopy(counts[theInstances.attribute(index).numValues()], 0,
		     m_Distribution[2], 0, theInstances.numClasses());
    for (int i = 0; i < theInstances.attribute(index).numValues(); i++) {
      for (int j = 0; j < theInstances.numClasses(); j++) {
	m_Distribution[0][j] = counts[i][j];
	m_Distribution[1][j] = sumCounts[j] - counts[i][j];
      }
      currVal = ContingencyTables.entropyConditionedOnRows(m_Distribution);
      if (Utils.sm(currVal, bestVal)) {
	bestVal = currVal;
	m_SplitPoint = (double)i;
	for (int j = 0; j < 3; j++) {
	  System.arraycopy(m_Distribution[j], 0, bestDist[j], 0, 
			   theInstances.numClasses());
	}
      }
    }

    // No missing values in training data.
    if (numMissing == 0) {
      System.arraycopy(sumCounts, 0, bestDist[2], 0, 
		       theInstances.numClasses());
    }
   
    m_Distribution = bestDist;
    return bestVal;
  }

  /**
   * Finds best split for nominal attribute and numeric class
   * and returns value.
   *
   * @param index attribute index
   * @return value of criterion for the best split
   * @exception Exception if something goes wrong
   */
  private double findSplitNominalNumeric(int index) throws Exception {

    double bestVal = Double.MAX_VALUE, currVal;
    double[] sumsSquaresPerValue = 
      new double[theInstances.attribute(index).numValues()], 
      sumsPerValue = new double[theInstances.attribute(index).numValues()], 
      weightsPerValue = new double[theInstances.attribute(index).numValues()];
    double totalSumSquares = 0, totalSum = 0, totalSumOfWeights = 0;
    double[] sumsSquares = new double[3], sumOfWeights = new double[3];
    double[][] bestDist = new double[3][1];

    // Compute counts for all the values
    for (int i = 0; i < theInstances.numInstances(); i++) {
      Instance inst = theInstances.instance(i);
      if (inst.isMissing(index)) {
	m_Distribution[2][0] += inst.classValue() * inst.weight();
	sumsSquares[2] += inst.classValue() * inst.classValue() 
	  * inst.weight();
	sumOfWeights[2] += inst.weight();
      } else {
	weightsPerValue[(int)inst.value(index)] += inst.weight();
	sumsPerValue[(int)inst.value(index)] += inst.classValue() 
	  * inst.weight();
	sumsSquaresPerValue[(int)inst.value(index)] += 
	  inst.classValue() * inst.classValue() * inst.weight();
      }
    }

    // Compute sum of counts without missing ones
    for (int i = 0; i < theInstances.attribute(index).numValues(); i++) {
      totalSumOfWeights += weightsPerValue[i];
      totalSumSquares += sumsSquaresPerValue[i];
      totalSum += sumsPerValue[i];
    }
    
    // Make split counts for each possible split and evaluate
    for (int i = 0; i < theInstances.attribute(index).numValues(); i++) {
      
      m_Distribution[0][0] = sumsPerValue[i];
      sumsSquares[0] = sumsSquaresPerValue[i];
      sumOfWeights[0] = weightsPerValue[i];
      m_Distribution[1][0] = totalSum - sumsPerValue[i];
      sumsSquares[1] = totalSumSquares - sumsSquaresPerValue[i];
      sumOfWeights[1] = totalSumOfWeights - weightsPerValue[i];

      currVal = variance(m_Distribution, sumsSquares, sumOfWeights);
      
      if (Utils.sm(currVal, bestVal)) {
	bestVal = currVal;
	m_SplitPoint = (double)i;
	for (int j = 0; j < 3; j++) {
	  if (!Utils.eq(sumOfWeights[j], 0)) {
	    bestDist[j][0] = m_Distribution[j][0] / sumOfWeights[j];
	  } else {
	    bestDist[j][0] = totalSum / totalSumOfWeights;
	  }
	}
      }
    }

    m_Distribution = bestDist;
    return bestVal;
  }

  /**
   * Finds best split for numeric attribute and returns value.
   *
   * @param index attribute index
   * @return value of criterion for the best split
   * @exception Exception if something goes wrong
   */
  private double findSplitNumeric(int index) throws Exception {

    if (theInstances.classAttribute().isNominal()) {
      return findSplitNumericNominal(index);
    } else {
      return findSplitNumericNumeric(index);
    }
  }

  /**
   * Finds best split for numeric attribute and nominal class
   * and returns value.
   *
   * @param index attribute index
   * @return value of criterion for the best split
   * @exception Exception if something goes wrong
   */
  private double findSplitNumericNominal(int index) throws Exception {

    double bestVal = Double.MAX_VALUE, currVal, currCutPoint;
    int numMissing = 0;
    double[] sum = new double[theInstances.numClasses()];
    double[][] bestDist = new double[3][theInstances.numClasses()];

    // Compute counts for all the values
    for (int i = 0; i < theInstances.numInstances(); i++) {
      Instance inst = theInstances.instance(i);
      if (!inst.isMissing(index)) {
	m_Distribution[1][(int)inst.classValue()] += inst.weight();
      } else {
	m_Distribution[2][(int)inst.classValue()] += inst.weight();
	numMissing++;
      }
    }
    System.arraycopy(m_Distribution[1], 0, sum, 0, theInstances.numClasses());

    // Sort instances
    theInstances.sort(index);
    
    // Make split counts for each possible split and evaluate
    for (int i = 0; i < theInstances.numInstances() - (numMissing + 1); i++) {
      Instance inst = theInstances.instance(i);
      Instance instPlusOne = theInstances.instance(i + 1);
      m_Distribution[0][(int)inst.classValue()] += inst.weight();
      m_Distribution[1][(int)inst.classValue()] -= inst.weight();
      if (Utils.sm(inst.value(index), instPlusOne.value(index))) {
	currCutPoint = (inst.value(index) + instPlusOne.value(index)) / 2.0;
	currVal = ContingencyTables.entropyConditionedOnRows(m_Distribution);
	if (Utils.sm(currVal, bestVal)) {
	  m_SplitPoint = currCutPoint;
	  bestVal = currVal;
	  for (int j = 0; j < 3; j++) {
	    System.arraycopy(m_Distribution[j], 0, bestDist[j], 0, 
			     theInstances.numClasses());
	  }
	}
      }
    }

    // No missing values in training data.
    if (numMissing == 0) {
      System.arraycopy(sum, 0, bestDist[2], 0, theInstances.numClasses());
    }
 
    m_Distribution = bestDist;
    return bestVal;
  }

  /**
   * Finds best split for numeric attribute and numeric class
   * and returns value.
   *
   * @param index attribute index
   * @return value of criterion for the best split
   * @exception Exception if something goes wrong
   */
  private double findSplitNumericNumeric(int index) throws Exception {

    double bestVal = Double.MAX_VALUE, currVal, currCutPoint;
    int numMissing = 0;
    double[] sumsSquares = new double[3], sumOfWeights = new double[3];
    double[][] bestDist = new double[3][1];
    double totalSum = 0, totalSumOfWeights = 0;

    // Compute counts for all the values
    for (int i = 0; i < theInstances.numInstances(); i++) {
      Instance inst = theInstances.instance(i);
      if (!inst.isMissing(index)) {
	m_Distribution[1][0] += inst.classValue() * inst.weight();
	sumsSquares[1] += inst.classValue() * inst.classValue() 
	  * inst.weight();
	sumOfWeights[1] += inst.weight();
      } else {
	m_Distribution[2][0] += inst.classValue() * inst.weight();
	sumsSquares[2] += inst.classValue() * inst.classValue() 
	  * inst.weight();
	sumOfWeights[2] += inst.weight();
	numMissing++;
      }
      totalSumOfWeights += inst.weight();
      totalSum += inst.classValue() * inst.weight();
    }

    // Sort instances
    theInstances.sort(index);
    
    // Make split counts for each possible split and evaluate
    for (int i = 0; i < theInstances.numInstances() - (numMissing + 1); i++) {
      Instance inst = theInstances.instance(i);
      Instance instPlusOne = theInstances.instance(i + 1);
      m_Distribution[0][0] += inst.classValue() * inst.weight();
      sumsSquares[0] += inst.classValue() * inst.classValue() * inst.weight();
      sumOfWeights[0] += inst.weight();
      m_Distribution[1][0] -= inst.classValue() * inst.weight();
      sumsSquares[1] -= inst.classValue() * inst.classValue() * inst.weight();
      sumOfWeights[1] -= inst.weight();
      if (Utils.sm(inst.value(index), instPlusOne.value(index))) {
	currCutPoint = (inst.value(index) + instPlusOne.value(index)) / 2.0;
	currVal = variance(m_Distribution, sumsSquares, sumOfWeights);
	if (Utils.sm(currVal, bestVal)) {
	  m_SplitPoint = currCutPoint;
	  bestVal = currVal;
	  for (int j = 0; j < 3; j++) {
	    if (!Utils.eq(sumOfWeights[j], 0)) {
	      bestDist[j][0] = m_Distribution[j][0] / sumOfWeights[j];
	    } else {
	      bestDist[j][0] = totalSum / totalSumOfWeights;
	    }
	  }
	}
      }
    }

    m_Distribution = bestDist;
    return bestVal;
  }

  /**
   * Computes variance for subsets.
   */
  private double variance(double[][] s,double[] sS,double[] sumOfWeights) {

    double var = 0;

    for (int i = 0; i < s.length; i++) {
      if (Utils.gr(sumOfWeights[i], 0)) {
	var += sS[i] - ((s[i][0] * s[i][0]) / (double) sumOfWeights[i]);
      }
    }
    
    return var;
  }

  /**
   * Returns the subset an instance falls into.
   */
  private int whichSubset(Instance instance) throws Exception {

    if (instance.isMissing(m_AttIndex)) {
      return 2;
    } else if (instance.attribute(m_AttIndex).isNominal()) {
      if ((int)instance.value(m_AttIndex) == m_SplitPoint) {
	return 0;
      } else {
	return 1;
      }
    } else {
      if (Utils.smOrEq(instance.value(m_AttIndex), m_SplitPoint)) {
	return 0;
      } else {
	return 1;
      }
    }
  }
 
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    Classifier scheme;

    try {
      scheme = new DecisionStump();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}


