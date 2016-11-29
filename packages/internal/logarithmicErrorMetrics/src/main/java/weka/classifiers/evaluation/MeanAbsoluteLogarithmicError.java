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
 *    MeanAbsoluteLogarithmicError.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.evaluation;

import java.util.ArrayList;
import java.util.List;

import weka.core.Instance;
import weka.core.Utils;

/**
 *  Provides mean absolute logarithmic error for evaluating regression
 *  schemes.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 11192 $
 */
public class MeanAbsoluteLogarithmicError extends AbstractEvaluationMetric implements StandardEvaluationMetric {

  /** Sum of log errors */
  protected double m_SumLogErrors = 0;

  /** Sum of weights */
  protected double m_SumOfWeights = 0;

  /**
   * Whether metric applies to nominal class.
   * @return false
   */
  public boolean appliesToNominalClass() {
    return false;
  }

  /**
   * Whether metric applies to numeric class.
   * @return true
   */
  public boolean appliesToNumericClass() {
    return true;
  }

  /**
   * The names of the metrics.
   * @return the names of the metrics.
   */
  public String getMetricName() {
    return "MALE";
  }

  /**
   * A brief description of the metrics.
   * @return a brief description of the metrics.
   */
  public String getMetricDescription() { return "The mean absolute logarithmic error, using log(x + 1)."; }

  /**
   * Update stats for a nominal class. Does nothing because metrics are for regression only.
   * @param predictedDistribution the probabilities assigned to each class
   * @param instance the instance to be classified
   */
  public  void updateStatsForClassifier(double[] predictedDistribution, Instance instance) {
    // Do nothing
    }

  /**
   * Update stats for a numeric class.
   * @param predictedValue the value that is predicted
   * @param instance the instance to be classified
   */
  public  void updateStatsForPredictor(double predictedValue, Instance instance) {

    if (!instance.classIsMissing()) {
      if (!Utils.isMissingValue(predictedValue)) {
        double diff = (Math.log(instance.classValue() + 1) - Math.log(predictedValue + 1));
        m_SumLogErrors += instance.weight() * Math.abs(diff);
        m_SumOfWeights += instance.weight();
      }
    }
  }

  /**
   * Returns the (short) names of the statistics that are made available.
   * @return a list of short names
   */
  public List<String> getStatisticNames() {

    ArrayList<String> names = new ArrayList<String>();
    names.add("male");

    return names;
  }

  /**
   * Produces string providing textual summary of statistics.
   * @return the string produced
   */
  public String toSummaryString() {

    return "Mean absolute logarithmic error    " + Utils.doubleToString(getStatistic("male"), 12, 4) + "\n";
  }

  /**
   * Returns the value of the statistic based on the given short name.
   * @param name the short name
   * @return the value of the statistic
   */
  public double getStatistic(String name) {

    if (!name.equals("male")) {
      throw new UnknownStatisticException("Statistic " + name + " is unknown.");
    }

    return m_SumLogErrors / m_SumOfWeights;
  }

  /**
   * Whether metric is to be maximized.
   *
   * @return false
   */
  public boolean statisticIsMaximisable(String statName) {

    return false;
  }
}
