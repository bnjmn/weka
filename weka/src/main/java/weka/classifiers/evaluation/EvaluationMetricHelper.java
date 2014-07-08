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
 *    EvaluationMetricHelper.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper routines for extracting metric values from built-in and plugin
 * evaluation metrics.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class EvaluationMetricHelper {

  /** The Evaluation object to extract built-in and plugin metrics from */
  protected Evaluation m_eval;

  /** A lookup for built-in metrics */
  protected Map<String, Integer> m_builtin = new HashMap<String, Integer>();

  /** A lookup for plugin metrics */
  protected Map<String, AbstractEvaluationMetric> m_pluginMetrics =
    new HashMap<String, AbstractEvaluationMetric>();

  /**
   * Construct a new EvaluationMetricHelper
   * 
   * @param eval the Evaluation object to use
   */
  public EvaluationMetricHelper(Evaluation eval) {
    for (int i = 0; i < Evaluation.BUILT_IN_EVAL_METRICS.length; i++) {
      m_builtin.put(Evaluation.BUILT_IN_EVAL_METRICS[i].toLowerCase(), i);
    }

    setEvaluation(eval);
  }

  /**
   * Sets the Evaluation object to use
   * 
   * @param eval the Evaluation object to use
   */
  public void setEvaluation(Evaluation eval) {
    m_eval = eval;
    initializeWithPluginMetrics();
  }

  /**
   * Initializes the plugin lookup
   */
  protected void initializeWithPluginMetrics() {
    m_pluginMetrics.clear();
    List<AbstractEvaluationMetric> pluginMetrics = m_eval.getPluginMetrics();
    if (pluginMetrics != null && pluginMetrics.size() > 0) {
      for (AbstractEvaluationMetric m : pluginMetrics) {
        List<String> statNames = m.getStatisticNames();
        for (String s : statNames) {
          m_pluginMetrics.put(s.toLowerCase(), m);
        }
      }
    }
  }

  /**
   * Get a list of built-in metric names
   * 
   * @return a list of built-in metric names
   */
  public static List<String> getBuiltInMetricNames() {
    List<String> builtIn = new ArrayList<String>();
    builtIn.addAll(Arrays.asList(Evaluation.BUILT_IN_EVAL_METRICS));

    return builtIn;
  }

  /**
   * Get a list of plugin metric names
   * 
   * @return a list of plugin metric names
   */
  public static List<String> getPluginMetricNames() {
    List<String> pluginNames = new ArrayList<String>();
    List<AbstractEvaluationMetric> pluginMetrics =
      AbstractEvaluationMetric.getPluginMetrics();

    if (pluginMetrics != null) {
      for (AbstractEvaluationMetric m : pluginMetrics) {
        List<String> statNames = m.getStatisticNames();
        for (String s : statNames) {
          pluginNames.add(s.toLowerCase());
        }
      }
    }

    return pluginNames;
  }

  /**
   * Get a list of all available evaluation metric names
   * 
   * @return a list of all available evaluation metric names
   */
  public static List<String> getAllMetricNames() {
    List<String> metrics = getBuiltInMetricNames();
    metrics.addAll(getPluginMetricNames());

    return metrics;
  }

  /**
   * Returns true if the specified built-in metric is maximisable
   * 
   * @param metricIndex the index of metric
   * @return true if the metric in question is optimum at a maximal value
   * @throws Exception if the metric is not a known built-in metric
   */
  protected boolean builtInMetricIsMaximisable(int metricIndex)
    throws Exception {
    switch (metricIndex) {
    case 0:
      // correct
      return true;
    case 1:
      // incorrect
      return false;
    case 2:
      // kappa
      return true;
    case 3:
      // total cost
      return false;
    case 4:
      // avg cost
      return false;
    case 5:
      // KB relative info
      return false;
    case 6:
      // KB info
      return false;
    case 7:
      // correlation
      return true;
    case 8:
      // SF prior entropy
      return false;
    case 9:
      // SF scheme entropy
      return false;
    case 10:
      // SF entropy gain
      return true;
    case 11:
      // MAE
      return false;
    case 12:
      // RMSE
      return false;
    case 13:
      // RAE
      return false;
    case 14:
      // RRSE
      return false;
    case 15:
      // coverage of cases by predicted regions
      return true;
    case 16:
      // size of predicted regions
      return false;
    case 17:
      // TPR
      return true;
    case 18:
      // FPR
      return false;
    case 19:
      // precision
      return true;
    case 20:
      // recall
      return true;
    case 21:
      // f-measure
      return true;
    case 22:
      // Matthews correlation
      return true;
    case 23:
      // AUC
      return true;
    case 24:
      // AUPRC
      return true;
    }

    throw new Exception("Unknown built-in metric");
  }

  /**
   * Gets the value of a built-in metric
   * 
   * @param metricIndex the index of the metric
   * @param classValIndex the optional class value index
   * @return the value of the metric
   * @throws Exception if the metric is not a known built-in metric
   */
  protected double getBuiltinMetricValue(int metricIndex, int... classValIndex)
    throws Exception {

    boolean hasValIndex = classValIndex != null && classValIndex.length == 1;

    switch (metricIndex) {
    case 0:
      return m_eval.correct();
    case 1:
      return m_eval.incorrect();
    case 2:
      return m_eval.kappa();
    case 3:
      return m_eval.totalCost();
    case 4:
      return m_eval.avgCost();
    case 5:
      return m_eval.KBRelativeInformation();
    case 6:
      return m_eval.KBInformation();
    case 7:
      return m_eval.correlationCoefficient();
    case 8:
      return m_eval.SFPriorEntropy();
    case 9:
      return m_eval.SFSchemeEntropy();
    case 10:
      return m_eval.SFEntropyGain();
    case 11:
      return m_eval.meanAbsoluteError();
    case 12:
      return m_eval.rootMeanSquaredError();
    case 13:
      return m_eval.relativeAbsoluteError();
    case 14:
      return m_eval.rootRelativeSquaredError();
    case 15:
      return m_eval.coverageOfTestCasesByPredictedRegions();
    case 16:
      return m_eval.sizeOfPredictedRegions();
    case 17:
      return hasValIndex ? m_eval.truePositiveRate(classValIndex[0]) : m_eval
        .weightedTruePositiveRate();
    case 18:
      return hasValIndex ? m_eval.falsePositiveRate(classValIndex[0]) : m_eval
        .weightedFalsePositiveRate();
    case 19:
      return hasValIndex ? m_eval.precision(classValIndex[0]) : m_eval
        .weightedPrecision();
    case 20:
      return hasValIndex ? m_eval.recall(classValIndex[0]) : m_eval
        .weightedRecall();
    case 21:
      return hasValIndex ? m_eval.fMeasure(classValIndex[0]) : m_eval
        .weightedFMeasure();
    case 22:
      return hasValIndex ? m_eval
        .matthewsCorrelationCoefficient(classValIndex[0]) : m_eval
        .weightedMatthewsCorrelation();
    case 23:
      return hasValIndex ? m_eval.areaUnderROC(classValIndex[0]) : m_eval
        .weightedAreaUnderROC();
    case 24:
      return hasValIndex ? m_eval.areaUnderPRC(classValIndex[0]) : m_eval
        .weightedAreaUnderPRC();
    }

    throw new Exception("Unknown built-in metric");
  }

  /**
   * Get the value of a plugin metric
   * 
   * @param m the metric to get the value from
   * @param statName the name of the statistic to get the value of
   * @param classValIndex the optional class value index
   * @return the value of the metric
   * @throws Exception if a problem occurs
   */
  protected double getPluginMetricValue(AbstractEvaluationMetric m,
    String statName, int... classValIndex) throws Exception {

    boolean hasValIndex = classValIndex != null && classValIndex.length == 1;

    if (m instanceof InformationRetrievalEvaluationMetric) {
      return hasValIndex ? ((InformationRetrievalEvaluationMetric) m)
        .getStatistic(statName, classValIndex[0])
        : ((InformationRetrievalEvaluationMetric) m)
          .getClassWeightedAverageStatistic(statName);
    }

    return m.getStatistic(statName);
  }

  /**
   * Returns true if the named statistic is maximisable
   * 
   * @param m the metric to check
   * @param statName the name of the statistic to check
   * @return true if the metric in question is optimum at a maximal value
   */
  protected boolean pluginMetricIsMaximisable(AbstractEvaluationMetric m,
    String statName) {
    return m.statisticIsMaximisable(statName);
  }

  /**
   * Gets the value of a named metric. For information retrieval metrics if a
   * class value index is not supplied then the class weighted variant is
   * returned.
   * 
   * @param statName the name of the metric/statistic to get
   * @param classValIndex the optional class value index
   * @return the value of the metric
   * @throws Exception if the metric/stat is unknown or a problem occurs
   */
  public double getNamedMetric(String statName, int... classValIndex)
    throws Exception {

    if (classValIndex != null && classValIndex.length > 1) {
      throw new IllegalArgumentException(
        "Only one class value index should be supplied");
    }

    Integer builtinIndex = m_builtin.get(statName.toLowerCase());
    if (builtinIndex != null) {
      return getBuiltinMetricValue(builtinIndex.intValue(), classValIndex);
    } else {
      AbstractEvaluationMetric m = m_pluginMetrics.get(statName.toLowerCase());
      if (m == null) {
        throw new Exception("Unknown evaluation metric: " + statName);
      }
      return getPluginMetricValue(m, statName, classValIndex);
    }
  }

  /**
   * Gets the thresholds produced by the metric, if the metric implements
   * ThresholdProducingMetric.
   * 
   * @param statName the name of the metric/statistic to get
   * @return the thresholds, null if metric does not produce any
   * @throws Exception if the metric/stat is unknown or a problem occurs
   */
  public double[] getNamedMetricThresholds(String statName)
    throws Exception {

    Integer builtinIndex = m_builtin.get(statName.toLowerCase());
    if (builtinIndex != null) {
      return null; // built-in metrics don not produce thresholds
    } else {
      AbstractEvaluationMetric m = m_pluginMetrics.get(statName.toLowerCase());
      if (m == null) {
        throw new Exception("Unknown evaluation metric: " + statName);
      }
      if (m instanceof ThresholdProducingMetric) {
        return ((ThresholdProducingMetric)m).getThresholds();
      } else {
        return null;
      }
    }
  }

  /**
   * Returns true if the named metric is maximisable
   * 
   * @param statName the name of the metric/statistic to check
   * @return true if the metric in question is optimum at a maximal value
   * @throws Exception if a problem occurs
   */
  public boolean metricIsMaximisable(String statName) throws Exception {
    Integer builtinIndex = m_builtin.get(statName.toLowerCase());
    if (builtinIndex != null) {
      return builtInMetricIsMaximisable(builtinIndex.intValue());
    } else {
      AbstractEvaluationMetric m = m_pluginMetrics.get(statName.toLowerCase());
      if (m == null) {
        throw new Exception("Unknown evaluation metric: " + statName);
      }
      return pluginMetricIsMaximisable(m, statName);
    }
  }
}
