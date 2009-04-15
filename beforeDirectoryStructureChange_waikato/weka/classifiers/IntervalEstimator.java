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
 *    IntervalEstimator.java
 *    Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers;

import weka.core.Instance;

/** 
 * Interface for classifiers that can output confidence intervals
 *
 * @author Kurt Driessens (kurtd@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface IntervalEstimator {

  /**
   * Returns an N*2 array, where N is the number of possible classes, that estimate 
   * the boundaries for the confidence interval with a confidence level specified by
   * the second parameter. Every row of the returned array gives the probability estimates 
   * for a single class.  In the case of numeric predictions, a single confidance interval 
   * will be returned.
   *
   * @param inst the instance to make the prediction for.
   * @param confidenceLevel the percentage of cases that the interval should cover.
   * @return an array of confidance intervals (one for each class)
   * @exception Exception if the intervals can't be computed
   */
  double[][] predictInterval(Instance inst, double confidenceLevel) throws Exception;
}

