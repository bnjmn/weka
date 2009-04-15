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
 *    NumericPrediction.java
 *    Copyright (C) 2002 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.evaluation;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

import java.io.Serializable;

/**
 * Encapsulates an evaluatable numeric prediction: the predicted class value
 * plus the actual class value.
 *
 * @author Len Trigg (len@reeltwo.com)
 * @version $Revision: 1.8.2.1 $
 */
public class NumericPrediction
  implements Prediction, Serializable, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = -4880216423674233887L;

  /** The actual class value */
  private double m_Actual = MISSING_VALUE;

  /** The predicted class value */
  private double m_Predicted = MISSING_VALUE;

  /** The weight assigned to this prediction */
  private double m_Weight = 1;

  /**
   * Creates the NumericPrediction object with a default weight of 1.0.
   *
   * @param actual the actual value, or MISSING_VALUE.
   * @param predicted the predicted value, or MISSING_VALUE.
   */
  public NumericPrediction(double actual, double predicted) {

    this(actual, predicted, 1);
  }

  /**
   * Creates the NumericPrediction object.
   *
   * @param actual the actual value, or MISSING_VALUE.
   * @param predicted the predicted value, or MISSING_VALUE.
   * @param weight the weight assigned to the prediction.
   */
  public NumericPrediction(double actual, double predicted, double weight) {

    m_Actual = actual;
    m_Predicted = predicted;
    m_Weight = weight;
  }

  /** 
   * Gets the actual class value.
   *
   * @return the actual class value, or MISSING_VALUE if no
   * prediction was made.  
   */
  public double actual() { 

    return m_Actual; 
  }

  /**
   * Gets the predicted class value.
   *
   * @return the predicted class value, or MISSING_VALUE if no
   * prediction was made.  
   */
  public double predicted() { 

    return m_Predicted; 
  }

  /** 
   * Gets the weight assigned to this prediction. This is typically the weight
   * of the test instance the prediction was made for.
   *
   * @return the weight assigned to this prediction.
   */
  public double weight() { 

    return m_Weight; 
  }

  /**
   * Calculates the prediction error. This is defined as the predicted
   * value minus the actual value.
   *
   * @return the error for this prediction, or
   * MISSING_VALUE if either the actual or predicted value
   * is missing.  
   */
  public double error() {

    if ((m_Actual == MISSING_VALUE) ||
        (m_Predicted == MISSING_VALUE)) {
      return MISSING_VALUE;
    }
    return m_Predicted - m_Actual;
  }

  /**
   * Gets a human readable representation of this prediction.
   *
   * @return a human readable representation of this prediction.
   */
  public String toString() {

    StringBuffer sb = new StringBuffer();
    sb.append("NUM: ").append(actual()).append(' ').append(predicted());
    sb.append(' ').append(weight());
    return sb.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.8.2.1 $");
  }
}
