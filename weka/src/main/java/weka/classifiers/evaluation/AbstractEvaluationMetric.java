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
 *    AbstractEvaluationMetric.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.evaluation;

import weka.core.PluginManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Abstract base class for pluggable classification/regression evaluation
 * metrics.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class AbstractEvaluationMetric implements Serializable {

  /** For serialization */
  private static final long serialVersionUID = -924507718482386887L;

  /**
   * Gets a list of freshly instantiated concrete implementations of available
   * plugin metrics or null if there are no plugin metrics available
   * 
   * @return a list of plugin metrics or null if there are no plugin metrics
   */
  public synchronized static ArrayList<AbstractEvaluationMetric> getPluginMetrics() {
    ArrayList<AbstractEvaluationMetric> pluginMetricsList = null;
    Set<String> pluginMetrics =
      PluginManager.getPluginNamesOfType(AbstractEvaluationMetric.class
        .getName());
    if (pluginMetrics != null) {
      pluginMetricsList = new ArrayList<AbstractEvaluationMetric>();

      for (String metric : pluginMetrics) {
        try {
          Object impl =
            PluginManager.getPluginInstance(
              AbstractEvaluationMetric.class.getName(), metric);
          if (impl instanceof AbstractEvaluationMetric) {
            pluginMetricsList.add((AbstractEvaluationMetric) impl);
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }
    return pluginMetricsList;
  }

  /**
   * Exception for subclasses to throw if asked for a statistic that is not part
   * of their implementation
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  public class UnknownStatisticException extends IllegalArgumentException {

    /** For serialization */
    private static final long serialVersionUID = -8787045492227999839L;

    /**
     * Constructs a new UnknownStatisticsException
     * 
     * @param message the exception's message
     */
    public UnknownStatisticException(String message) {
      super(message);
    }
  }

  /**
   * Base evaluation object for subclasses to access for statistics. IMPORTANT:
   * subclasses should treat this object as read-only
   */
  protected Evaluation m_baseEvaluation;

  /**
   * Set the base evaluation object to use. IMPORTANT: subclasses should treat
   * this object as read-only.
   * 
   * @param eval
   */
  public void setBaseEvaluation(Evaluation eval) {
    m_baseEvaluation = eval;
  }

  /**
   * Return true if this evaluation metric can be computed when the class is
   * nominal
   * 
   * @return true if this evaluation metric can be computed when the class is
   *         nominal
   */
  public abstract boolean appliesToNominalClass();

  /**
   * Return true if this evaluation metric can be computed when the class is
   * numeric
   * 
   * @return true if this evaluation metric can be computed when the class is
   *         numeric
   */
  public abstract boolean appliesToNumericClass();

  /**
   * Get the name of this metric
   * 
   * @return the name of this metric
   */
  public abstract String getMetricName();

  /**
   * Get a short description of this metric (algorithm, forumulas etc.).
   * 
   * @return a short description of this metric
   */
  public abstract String getMetricDescription();

  /**
   * Get a list of the names of the statistics that this metrics computes. E.g.
   * an information theoretic evaluation measure might compute total number of
   * bits as well as average bits/instance
   * 
   * @return the names of the statistics that this metric computes
   */
  public abstract List<String> getStatisticNames();

  /**
   * Get the value of the named statistic
   * 
   * @param statName the name of the statistic to compute the value for
   * @return the computed statistic or Utils.missingValue() if the statistic
   *         can't be computed for some reason
   */
  public abstract double getStatistic(String statName);

  /**
   * True if the optimum value of the named metric is a maximum value; false if
   * the optimim value is a minimum value. Subclasses should override this
   * method to suit their statistic(s)
   * 
   * @return true (default implementation)
   */
  public boolean statisticIsMaximisable(String statName) {
    return true;
  }
}
