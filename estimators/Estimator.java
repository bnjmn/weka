/*
 *    Estimator.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.estimators;

import java.util.*;
import java.io.*;

/** 
 * Interface for probability estimators. Example code: <p>
 *
 * <code> <pre>
 *   // Create a discrete estimator that takes values 0 to 9
 *   DiscreteEstimator newEst = new DiscreteEstimator(10, true);
 *
 *   // Create 50 random integers first predicting the probability of the
 *   // value, then adding the value to the estimator
 *   Random r = new Random(seed);
 *   for(int i = 0; i < 50; i++) {
 *     current = Math.abs(r.nextInt() % 10);
 *     System.out.println(newEst);
 *     System.out.println("Prediction for " + current 
 *                        + " = " + newEst.getProbability(current));
 *     newEst.addValue(current, 1);
 *   }
 * </pre> </code>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public interface Estimator extends Serializable {

  /**
   * Add a new data value to the current estimator.
   *
   * @param data the new data value 
   * @param weight the weight assigned to the data value 
   */
  public void addValue(double data, double weight);

  /**
   * Get a probability estimate for a value.
   *
   * @param data the value to estimate the probability of
   * @return the estimated probability of the supplied value
   */
  public double getProbability(double data);
}








