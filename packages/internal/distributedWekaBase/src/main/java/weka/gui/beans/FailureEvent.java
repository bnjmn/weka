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
 *    FailureEvent
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Failure event for Hadoop KF steps
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class FailureEvent extends EventObject {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -4222891746127324609L;

  /** Holds the reason for the failure - usually the message from an Exception */
  protected String m_failureReason = "";

  /** Any additional stuff that needs to be passed on */
  protected Map<String, Object> m_payload = new HashMap<String, Object>();

  /**
   * Constructor
   * 
   * @param source the source KF step generating this event
   * @param reasonDetailed the reason for the failure (usually the message from
   *          an Exception)
   */
  public FailureEvent(Object source, String reasonDetailed) {
    super(source);

    m_failureReason = reasonDetailed;
  }

  /**
   * Get the failure reason
   * 
   * @return the failure reason
   */
  public String getFailureInfo() {
    return m_failureReason;
  }

  /**
   * Set a payload element
   * 
   * @param key the key for the element
   * @param value the value of the element
   */
  public <T> void setPayloadElement(String key, T value) {
    m_payload.put(key, value);
  }

  /**
   * Get a payload element
   * 
   * @param key the key of the element to get
   * @return the value of the element (or null if it is not set)
   */
  public <T> T getPayloadElement(String key) {
    return (T) m_payload.get(key);
  }
}
