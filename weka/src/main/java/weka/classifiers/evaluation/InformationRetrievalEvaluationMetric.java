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
 *    InformationRetrievalEvaluationMetric.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.evaluation;

import weka.core.Instance;

/**
 * An interface for information retrieval evaluation metrics to implement.
 * Allows the command line interface to display these metrics or not based on
 * user-supplied options. These statistics will be displayed as new columns in
 * the table of information retrieval statistics. As such, a toSummaryString()
 * formatted representation is not required.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface InformationRetrievalEvaluationMetric {

  /**
   * Updates the statistics about a classifiers performance for the current test
   * instance. Implementers need only implement this method if it is not
   * possible to compute their statistics from what is stored in the base
   * Evaluation object.
   * 
   * @param predictedDistribution the probabilities assigned to each class
   * @param instance the instance to be classified
   * @throws Exception if the class of the instance is not set
   */
  void updateStatsForClassifier(double[] predictedDistribution,
      Instance instance) throws Exception;

  /**
   * Get the value of the named statistic for the given class index.
   * 
   * If the implementing class is extending AbstractEvaluationMetric then the
   * implementation of getStatistic(String statName) should just call this
   * method with a classIndex of 0.
   * 
   * @param statName the name of the statistic to compute the value for
   * @param classIndex the class index for which to compute the statistic
   * @return the value of the named statistic for the given class index or
   *         Utils.missingValue() if the statistic can't be computed for some
   *         reason
   */
  double getStatistic(String statName, int classIndex);

  /**
   * Get the weighted (by class) average for this statistic.
   * 
   * @param statName the name of the statistic to compute
   * @return the weighted (by class) average value of the statistic or
   *         Utils.missingValue() if this can't be computed (or isn't
   *         appropriate).
   */
  double getClassWeightedAverageStatistic(String statName);
}
