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
 *    LossFunction.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.functions.loss;

import java.io.Serializable;

/**
 * Interface implemented by loss functions for MLPRegressor and MLPClassifier.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 10949 $
 */
public interface LossFunction extends Serializable {

  /**
   * Returns the loss.
   * @param pred predicted target value
   * @param actual actual target value
   * @return the loss
   */
  double loss(double pred, double actual);

  /**
   * The derivative of the loss with respect to the predicted value
   * @param pred predicted target value
   * @param actual actual target value
   * @return the value of the derivative
   */
  double derivative(double pred, double actual);
}
