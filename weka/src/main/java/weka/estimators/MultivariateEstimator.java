/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    MultivariateEstimator.java
 *    Copyright (C) 2013 University of Waikato
 */

package weka.estimators;

/**
 * Interface to Multivariate Distribution Estimation
 * 
 * @author Uday Kamath, PhD candidate George Mason University
 * @version $Revision$
 */
public interface MultivariateEstimator {

  /**
   * Fits the value to the density estimator.
   * 
   * @param value the value to add
   * @param weight the weight of the value
   */
  void estimate(double[][] value, double[] weight);

  /**
   * Returns the natural logarithm of the density estimate at the given point.
   * 
   * @param value the value at which to evaluate
   * @return the natural logarithm of the density estimate at the given value
   */
  double logDensity(double[] value);

}
