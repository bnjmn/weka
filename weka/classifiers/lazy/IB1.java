/*
 *    IB1.java
 *    Copyright (C) 1999 Stuart Inglis,Len Trigg,Eibe Frank
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
 * IB1-type classifier. Uses a simple distance measure to find the training
 * instance closest to the given test instance, and predicts the same class
 * as this training instance. If multiple instances are
 * the same (smallest) distance to the test instance, the first one found is
 * used. See <p>
 * 
 * Aha, D., and D. Kibler (1991) "Instance-based learning algorithms",
 * <i>Machine Learning</i>, vol.6, pp. 37-66.<p>
 *
 * @author Stuart Inglis (singlis@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class IB1 extends Classifier implements UpdateableClassifier {

  /** The training instances used for classification. */
  private Instances train;

  /** The minimum values for numeric attributes. */
  private double [] minArray;

  /** The maximum values for numeric attributes. */
  private double [] maxArray;

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception{
    
    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }

    train = new Instances(instances);
    minArray = new double [train.numAttributes()];
    maxArray = new double [train.numAttributes()];
    for (int i = 0; i < train.numAttributes(); i++) {
      minArray[i] = maxArray[i] = Double.NaN;
    }
    Enumeration enum = train.enumerateInstances();
    while (enum.hasMoreElements()) {
      updateMinMax((Instance) enum.nextElement());
    }
  }

  /**
   * Updates the classifier.
   *
   * @param instance the instance to be put into the classifier
   * @exception Exception if the instance could not be included successfully
   */
  public void updateClassifier(Instance instance) throws Exception {
  
    train.add(instance);
    updateMinMax(instance);
  }

  /**
   * Classifies the given test instance.
   *
   * @param instance the instance to be classified
   * @return the predicted class for the instance 
   * @exception Exception if the instance can't be classified
   */
  public double classifyInstance(Instance instance) throws Exception {
    
    double distance, minDistance = Double.MAX_VALUE, classValue = 0;

    updateMinMax(instance);
    Enumeration enum = train.enumerateInstances();
    while (enum.hasMoreElements()) {
      Instance trainInstance = (Instance) enum.nextElement();
      if (!trainInstance.classIsMissing()) {
	distance = distance(instance, trainInstance);
	if (distance < minDistance) {
	  minDistance = distance;
	  classValue = trainInstance.classValue();
	}
      }
    }

    return classValue;
  }

  /**
   * Returns a description of this classifier.
   *
   * @return a description of this classifier as a string.
   */
  public String toString() {

    return ("IB1 classifier");
  }

  /**
   * Calculates the distance between two instances
   *
   * @param test the first instance
   * @param train the second instance
   * @return the distance between the two given instances
   */          
  private double distance(Instance first, Instance second) {
    
    double diff, distance = 0;

    for(int i = 0; i < train.numAttributes(); i++) { 
      if (i == train.classIndex()) {
	continue;
      }
      if (train.attribute(i).isNominal()) {

	// If attribute is nominal
	if (first.isMissing(i) || second.isMissing(i) ||
	    ((int)first.value(i) != (int)second.value(i))) {
	  distance += 1;
	}
      } else {
	
	// If attribute is numeric
	if (first.isMissing(i) || second.isMissing(i)){
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
	distance += diff * diff;
      }
    }
    
    return distance;
  }
    
  /**
   * Normalizes a given value of a numeric attribute.
   *
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  private double norm(double x,int i) {

    if (Double.isNaN(minArray[i]) || Utils.eq(maxArray[i], minArray[i])) {
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
    
    for (int j = 0;j < train.numAttributes(); j++) {
      if ((train.attribute(j).isNumeric()) && (!instance.isMissing(j))) {
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

  /**
   * Main method for testing this class.
   *
   * @param argv should contain command line arguments for evaluation
   * (see Evaluation).
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new IB1(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}




