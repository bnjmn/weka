/*
 *    NominalPrediction.java
 *    Copyright (C) 2000 Intelligenesis Corp.
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
package weka.classifiers.evaluation;

/**
 * Encapsulates an evaluatable nominal prediction: the predicted probability
 * distribution plus the actual class value.
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.1 $
 */
public class NominalPrediction {

  /** The predicted probabilities */
  private double [] m_Distribution;

  /** The actual class value */
  private int m_Actual;

  /** The weight assigned to this prediction */
  private double m_Weight = 1;

  public NominalPrediction(int actual, double [] distribution) {

    this(actual, distribution, 1);
  }

  public NominalPrediction(int actual, double [] distribution, double weight) {

    m_Actual = actual;
    m_Distribution = distribution;
    m_Weight = weight;
  }

  /** Gets the predicted probabilities */
  public double [] distribution() { return m_Distribution; }

  /** Gets the actual class value */
  public int actual() { return m_Actual; }

  /** Gets the weight assigned to this prediction */
  public double weight() { return m_Weight; }

  /**
   * Determine the predicted class (doesn't detect multiple 
   * classifications).
   *
   * @return the predicted class value, or -1 if no prediction was made.
   */
  public int predicted() {
    int predictedClass = -1;
    double bestProb = 0.0;
    for(int i = 0; i < m_Distribution.length; i++) {
      if (m_Distribution[i] > bestProb) {
        predictedClass = i;
        bestProb = m_Distribution[i];
      }
    }
    return predictedClass;
  }

  /**
   * Calculates the prediction margin. This is defined as the difference
   * between the probability predicted for the actual class and the highest
   * predicted probability of the other classes.
   *
   * @return the margin for this prediction.
   */
  public double margin() {

    double probActual = m_Distribution[m_Actual];
    double probNext = 0;
    for(int i = 0; i < m_Distribution.length; i++)
      if ((i != m_Actual) &&
	  (m_Distribution[i] > probNext))
	probNext = m_Distribution[i];

    return probActual - probNext;
  }
}

