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
 *    ApproximateAbsoluteError.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.functions.loss;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import java.util.Enumeration;
import java.util.Vector;

/**
 * <!-- globalinfo-start -->
 * Approximate absolute error for MLPRegressor and MLPClassifier:<br>
 * loss(a, b) = sqrt((a-b)^2+epsilon)
 * <br><br>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -E &lt;double&gt;
 *  Epsilon to be added (default: 0.01).</pre>
 * 
 * <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 10949 $
 */
public class ApproximateAbsoluteError implements LossFunction, OptionHandler {

  /** The value of the epsilon parameter */
  protected double m_Epsilon = 0.01;

  /**
   * This will return a string describing the classifier.
   *
   * @return The string.
   */
  public String globalInfo() {

    return "Approximate absolute error for MLPRegressor and MLPClassifier:\n" +
            "loss(a, b) = sqrt((a-b)^2+epsilon)";
  }

  /**
   * Returns the loss.
   * @param pred predicted target value
   * @param actual actual target value
   * @return the loss
   */
  @Override
  public double loss(double pred, double actual) {
    double diff = pred - actual;
    return Math.sqrt(diff * diff + m_Epsilon);
  }

  /**
   * The derivative of the loss with respect to the predicted value
   * @param pred predicted target value
   * @param actual actual target value
   * @return the value of the derivative
   */
  @Override
  public double derivative(double pred, double actual) {
    double diff = pred - actual;
    return diff / Math.sqrt(diff * diff + m_Epsilon);
  }

  /**
   * @return a string to describe the option
   */
  public String epsilonTipText() {

    return "The epsilon parameter for the approximate absolute error.";
  }

  /**
   * Returns the value of the epsilon parameter.
   * @return the value of the epsilon parameter.
   */
  public double getEpsilon() {
    return m_Epsilon;
  }

  /**
   * Sets the value of the epsilon parameter.
   * @param epsilon the value of the epsilon parameter.
   */
  public void setEpsilon(double epsilon) {
    this.m_Epsilon = epsilon;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(1);

    newVector.addElement(new Option(
            "\tEpsilon to be added (default: 0.01).", "E", 1, "-E <double>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   *
   * <!-- options-start -->
   * Valid options are: <p>
   * 
   * <pre> -E &lt;double&gt;
   *  Epsilon to be added (default: 0.01).</pre>
   * 
   * <!-- options-end -->
  * <p>
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String epsilon = Utils.getOption('E', options);
    if (epsilon.length() != 0) {
      setEpsilon(Double.parseDouble(epsilon));
    } else {
      setEpsilon(0.01);
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the loss function.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-E");
    options.add("" + getEpsilon());

    return options.toArray(new String[0]);
  }

}

