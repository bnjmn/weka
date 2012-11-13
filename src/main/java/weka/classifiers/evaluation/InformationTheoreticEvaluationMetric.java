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
 *    InformationTheoreticEvaluationMetric.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.evaluation;

import weka.classifiers.ConditionalDensityEstimator;
import weka.core.Instance;

/**
 * Primarily a marker interface for information theoretic evaluation metrics to
 * implement. Allows the command line interface to display these metrics or not
 * based on user-supplied options
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface InformationTheoreticEvaluationMetric {

  /**
   * Updates the statistics about a classifiers performance for the current test
   * instance. Gets called when the class is nominal. Implementers need only
   * implement this method if it is not possible to compute their statistics
   * from what is stored in the base Evaluation object.
   * 
   * @param predictedDistribution the probabilities assigned to each class
   * @param instance the instance to be classified
   * @throws Exception if the class of the instance is not set
   */
  void updateStatsForClassifier(double[] predictedDistribution,
      Instance instance) throws Exception;

  /**
   * Updates the statistics about a predictors performance for the current test
   * instance. Gets called when the class is numeric. Implementers need only
   * implement this method if it is not possible to compute their statistics
   * from what is stored in the base Evaluation object.
   * 
   * @param predictedValue the numeric value the classifier predicts
   * @param instance the instance to be classified
   * @throws Exception if the class of the instance is not set
   */
  void updateStatsForPredictor(double predictedValue, Instance instance)
      throws Exception;

  /**
   * Updates stats for conditional density estimator based on current test
   * instance. Gets called when the class is numeric and the classifier is a
   * ConditionalDensityEstimators. Implementers need only implement this method
   * if it is not possible to compute their statistics from what is stored in
   * the base Evaluation object.
   * 
   * @param classifier the conditional density estimator
   * @param classMissing the instance for which density is to be computed,
   *          without a class value
   * @param classValue the class value of this instance
   * @throws Exception if density could not be computed successfully
   */
  void updateStatsForConditionalDensityEstimator(
      ConditionalDensityEstimator classifier, Instance classMissing,
      double classValue) throws Exception;

  /**
   * Return a formatted string (suitable for displaying in console or GUI
   * output) containing all the statistics that this metric computes.
   * 
   * @return a formatted string containing all the computed statistics
   */
  String toSummaryString();
}
