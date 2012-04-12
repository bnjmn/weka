/*
 *    ConditionalEstimator.java
 *    Copyright (C) 1999 Len Trigg
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
package weka.estimators;

import java.util.*;

/** 
 * Interface for probability estimators.
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version 1.0
 */

public interface ConditionalEstimator {

  // ===============
  // Public methods.
  // ===============
       
  /**
   * Add a new data value to the current estimator.
   * @param data the new data value 
   * @param given the new value that data is conditional upon 
   * @param weight the weight assigned to the data value 
   */
  void addValue(double data, double given, double weight);

  /**
   * Get a probability estimator for a value
   * @param given the new value that data is conditional upon 
   * @return the estimator for the supplied value given the condition
   */
  Estimator getEstimator(double given);

  /**
   * Get a probability for a value conditional on another value
   * @param data the value to estimate the probability of
   * @param given the new value that data is conditional upon 
   * @return the estimator for the supplied value given the condition
   */
  double getProbability(double data, double given);

}








