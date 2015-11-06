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
 *    SquaredError.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.functions.loss;

import weka.classifiers.functions.loss.LossFunction;

/**
 * <!-- globalinfo-start -->
 * Squared error for MLPRegressor and MLPClassifier:<br>
 * loss(a, b) = (a-b)^2
 * <br><br>
 * <!-- globalinfo-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 10949 $
 */
public class SquaredError implements LossFunction {

  /**
   * This will return a string describing the classifier.
   *
   * @return The string.
   */
  public String globalInfo() {

    return "Squared error for MLPRegressor and MLPClassifier:\n" +
            "loss(a, b) = (a-b)^2";
  }

  /**
   * Returns the loss.
   * @param pred predicted target value
   * @param actual actual target value
   * @return the loss
   */
  @Override
  public double loss(double pred, double actual) {
    return (pred - actual) * (pred - actual);
  }

  /**
   * The derivative of the loss with respect to the predicted value
   * @param pred predicted target value
   * @param actual actual target value
   * @return the value of the derivative
   */
  @Override
  public double derivative(double pred, double actual) {
    return pred - actual;
  }
}

