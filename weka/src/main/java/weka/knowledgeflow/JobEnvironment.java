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
 *    JobEnvironment.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.Environment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extended Environment with support for storing results and property values to
 * be set at a later date on the base schemes of WekaAlgorithmWrapper steps. The
 * latter is useful in the case where variables are not supported for properties
 * in a base scheme.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class JobEnvironment extends Environment {

  /** Map of non-incremental result data */
  protected Map<String, LinkedHashSet<Data>> m_resultData = new ConcurrentHashMap<>();

  /**
   * Map of properties for various base schemes owned by scheme-specific
   * (WekaAlgorithmWrapper) steps. Outer map is keyed by step name, inner map by
   * property name
   */
  protected Map<String, Map<String, String>> m_stepProperties =
    new ConcurrentHashMap<>();

  /**
   * Constructor
   */
  public JobEnvironment() {
    super();
  }

  /**
   * Construct a JobEnvironment by copying the contents of a standard
   * Environment
   * 
   * @param env the Environment to copy into this JobEnvironment
   */
  public JobEnvironment(Environment env) {
    super(env);

    if (env instanceof JobEnvironment) {
      m_stepProperties.putAll(((JobEnvironment) env).m_stepProperties);
    }
  }

  /**
   * Add a non-incremental data object to the result
   * 
   * @param data the data to add
   */
  public void addToResult(Data data) {
    if (!data.isIncremental()) {
      LinkedHashSet<Data> dataList = m_resultData.get(data.getConnectionName());
      if (dataList == null) {
        dataList = new LinkedHashSet<>();
        m_resultData.put(data.getConnectionName(), dataList);
      }
      dataList.add(data);
    }
  }

  /**
   * Add all the results from the supplied map to this environment's results
   *
   * @param otherResults the results to add
   */
  public void addAllResults(Map<String, LinkedHashSet<Data>> otherResults) {
    for (Map.Entry<String, LinkedHashSet<Data>> e : otherResults.entrySet()) {
      if (!m_resultData.containsKey(e.getKey())) {
        m_resultData.put(e.getKey(), e.getValue());
      } else {
        LinkedHashSet<Data> toAddTo = m_resultData.get(e.getKey());
        toAddTo.addAll(e.getValue());
      }
    }
  }

  /**
   * Get a list of any result data objects of the supplied connection type
   * 
   * @param connName the name of the connection to get result data objects for
   * @return a list of result data objects for the specified connection type, or
   *         null if none exist
   */
  public LinkedHashSet<Data> getResultDataOfType(String connName) {
    LinkedHashSet<Data> results = m_resultData.remove(connName);
    return results;
  }

  /**
   * Returns true if the results contain data of a particular connection type
   *
   * @param connName the name of the connection to check for data
   * @return true if the results contain data of the supplied connection type
   */
  public boolean hasResultDataOfType(String connName) {
    return m_resultData.containsKey(connName);
  }

  /**
   * Get a map of all the result data objects
   * 
   * @return a map of all result data
   */
  public Map<String, LinkedHashSet<Data>> getResultData() {
    return m_resultData;
  }

  public void clearResultData() {
    m_resultData.clear();
  }

  /**
   * Get the step properties for a named step
   * 
   * @param stepName the name of the step to get properties for
   * @return a map of properties for the named step
   */
  public Map<String, String> getStepProperties(String stepName) {
    return m_stepProperties.get(stepName);
  }

  /**
   * Clear all step properties
   */
  public void clearStepProperties() {
    m_stepProperties.clear();
  }

  /**
   * Add the supplied map of step properties. The map contains properties for
   * various base schemes owned by scheme-specific (WekaAlgorithmWrapper) steps.
   * Outer map is keyed by step name, inner map by property name.
   * 
   * @param propsToAdd properties to add
   */
  public void addToStepProperties(Map<String, Map<String, String>> propsToAdd) {
    m_stepProperties.putAll(propsToAdd);
  }
}
