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
 * @version $Revision: 1.5 $
 */
public interface Estimator extends Serializable {

  /**
   * Add a new data value to the current estimator.
   *
   * @param data the new data value 
   * @param weight the weight assigned to the data value 
   */
  void addValue(double data, double weight);

  /**
   * Get a probability estimate for a value.
   *
   * @param data the value to estimate the probability of
   * @return the estimated probability of the supplied value
   */
  double getProbability(double data);
}








