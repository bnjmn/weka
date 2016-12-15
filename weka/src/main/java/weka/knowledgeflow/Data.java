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
 *    Data.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.knowledgeflow.steps.Step;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>Class for encapsulating data to be transferred between Knowledge Flow steps
 * over a particular connection type. Typical usage involves constructing a Data
 * object with a given connection name and then setting one or more pieces of
 * "payload" data to encapsulate. Usually there is a primary payload that is
 * associated with the connection name itself (there is a constructor that takes
 * both the connection name and the primary payload data). Further auxiliary
 * data items can be added via the setPayloadElement() method.</p>
 * 
 * <p>Standard connection types are defined as constants in the StepManager class.
 * There is nothing to prevent clients from using their own connection types (as
 * these are just string identifiers).</p>
 *
 * Typical usage in a client step (transferring an training instances object):<br>
 * <br>
 * 
 * <pre>
 *     Instances train = ...
 *     Data myData = new Data(StepManager.CON_TRAININGSET, train);
 *     getStepManager().outputData(myData);
 * </pre>
 * 
 * @author mhall{[at]}pentaho{[dot]}com
 * @version $Revision: $
 */
public class Data implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 239235113781041619L;

  /** The source of this data */
  protected Step m_sourceStep;

  /**
   * The connection name for this Data - determines which downstream steps will
   * receive the data.
   */
  protected String m_connectionName;

  /** The payload */
  protected Map<String, Object> m_payloadMap =
    new LinkedHashMap<String, Object>();

  /**
   * Empty constructor - no connection name; no payload
   */
  public Data() {
  }

  /**
   * Construct a Data object with just a connection name
   *
   * @param connectionName the connection name
   */
  public Data(String connectionName) {
    setConnectionName(connectionName);
  }

  /**
   * Construct a Data object with a connection name and a primary payload object
   * to associate with the connection name
   *
   * @param connectionName connection name
   * @param primaryPayload primary payload object (i.e. is keyed against the
   *          connection name)
   */
  public Data(String connectionName, Object primaryPayload) {
    this(connectionName);
    setPayloadElement(connectionName, primaryPayload);
  }

  /**
   * Set the source step of producing this Data object
   *
   * @param sourceStep the source step
   */
  public void setSourceStep(Step sourceStep) {
    m_sourceStep = sourceStep;
  }

  /**
   * Get the source step producing this Data object
   *
   * @return the source step producing the data object
   */
  public Step getSourceStep() {
    return m_sourceStep;
  }

  /**
   * Set the connection name for this Data object
   *
   * @param name the name of the connection
   */
  public void setConnectionName(String name) {
    m_connectionName = name;
    if (StepManagerImpl.connectionIsIncremental(this)) {
      setPayloadElement(StepManager.CON_AUX_DATA_IS_INCREMENTAL, true);
    }
  }

  /**
   * Get the connection name associated with this Data object
   *
   * @return the connection name
   */
  public String getConnectionName() {
    return m_connectionName;
  }

  /**
   * Get the primary payload of this data object (i.e. the data associated with
   * the connection name)
   *
   * @param <T> the type of the primary payload
   * @return the primary payload data (or null if there is no data associated
   *         with the connection)
   */
  @SuppressWarnings("unchecked")
  public <T> T getPrimaryPayload() {
    return (T) m_payloadMap.get(m_connectionName);
  }

  /**
   * Get a payload element from this Data object. Standard payload element names
   * (keys) can be found as constants defined in the StepManager class.
   * 
   * @param name the name of the payload element to get
   * @return the named payload element, or null if it does not exist in this
   *         Data object
   */
  @SuppressWarnings("unchecked")
  public <T> T getPayloadElement(String name) {
    return (T) m_payloadMap.get(name);
  }

  /**
   * Get a payload element from this Data object. Standard payload element names
   * (keys) can be found as constants defined in the StepManager class.
   *
   * @param name the name of the payload element to get
   * @param defaultValue a default value if the named element does not exist
   * @param <T> the type of the payload element to get
   * @return the payload element, or the supplied default value.
   */
  public <T> T getPayloadElement(String name, T defaultValue) {
    Object result = getPayloadElement(name);
    if (result == null) {
      return defaultValue;
    }

    return (T) result;
  }

  /**
   * Set a payload element to encapsulate in this Data object. Standard payload
   * element names (keys) can be found as constants defined in the StepManager
   * class.
   * 
   * @param name the name of the payload element to encapsulate
   * @param value the value of the payload element
   */
  public void setPayloadElement(String name, Object value) {
    m_payloadMap.put(name, value);
  }

  /**
   * Clear all payload elements from this Data object
   */
  public void clearPayload() {
    m_payloadMap.clear();
  }

  /**
   * Return true if the connection specified for this data object is incremental
   *
   * @return true if the connection for this data object is incremental
   */
  public boolean isIncremental() {
    return StepManagerImpl.connectionIsIncremental(this);
  }
}
