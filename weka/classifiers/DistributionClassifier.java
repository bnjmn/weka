/*
 *    DistributionClassifier.java
 *    Copyright (C) 1999 Eibe Frank, Len Trigg
 *
 */

package weka.classifiers;

import weka.core.*;

/** 
 * Abstract classification model that produces (for each test instance)
 * an estimate of the membership in each class 
 * (ie. a probability distribution).
 *
 * @author   Eibe Frank (trigg@cs.waikato.ac.nz)
 * @author   Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public abstract class DistributionClassifier extends Classifier {

  /**
   * Predicts the class memberships for a given instance. If
   * an instance is unclassified, the returned array elements
   * must be all zero. If the class is numeric, the array
   * must consist of only one element, which contains the
   * predicted value.
   *
   * @param instance the instance to be classified
   * @return an array containing the estimated membership 
   * probabilities of the test instance in each class (this 
   * should sum to at most 1)
   * @exception Exception if distribution could not be 
   * computed successfully
   */
  public abstract double[] distributionForInstance(Instance instance) 
       throws Exception;

  /**
   * Classifies the given test instance. The instance has to belong to a
   * dataset when it's being classified.
   *
   * @param instance the instance to be classified
   * @return the predicted most likely class for the instance or 
   * Instance.missingValue() if no prediction is made
   * @exception Exception if an error occurred during the prediction
   */
  public double classifyInstance(Instance instance) throws Exception {

    double [] dist = distributionForInstance(instance);
    if (dist == null) {
      throw new Exception("Null distribution predicted");
    }
    switch (instance.classAttribute().type()) {
    case Attribute.NOMINAL:
      double max = 0;
      int maxIndex = 0;
      
      for (int i = 0; i < dist.length; i++) {
	if (dist[i] > max) {
	  maxIndex = i;
	  max = dist[i];
	}
      }
      if (max > 0) {
	return maxIndex;
      } else {
	return Instance.missingValue();
      }
    case Attribute.NUMERIC:
      return dist[0];
    default:
      return Instance.missingValue();
    }
  }
}
