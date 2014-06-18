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
 *    DistributedJobConfig.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package distributed.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Base class for different types of distributed configurations.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class DistributedJobConfig implements OptionHandler,
  Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -1508983502442626306L;

  /**
   * Holds the named configuration properties that concrete implementations
   * using a particular distributed system would need in order to configure
   * connections, job parameters etc.
   */
  protected Map<String, String> m_configProperties =
    new HashMap<String, String>();

  /**
   * Holds any additional user-supplied properties. I.e. properties beyond those
   * that the implementation may allow the user to set via a GUI or pre-defined
   * command line interface. These will be expected to be keyed by property
   * names actually expected by the underlying distributed system (rather than
   * more user friendly names exposed by a GUI or command line interface), or
   * any custom properties that the Mapper/Reducer code may need.
   */
  protected Map<String, String> m_additionalUserSuppliedProperties =
    new HashMap<String, String>();

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option(
      "\tUser supplied property-value pair (format propName=propValue).\n\t"
        + "May be supplied multiple times to set more than one property.",
      "user-prop", 1, "-user-prop <property=value>"));

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    m_additionalUserSuppliedProperties.clear();

    while (true) {
      String userProp = Utils.getOption("user-prop", options);
      if (DistributedJobConfig.isEmpty(userProp)) {
        break;
      }
      String[] parts = userProp.split("=");
      if (parts.length != 2) {
        throw new Exception("Illegal user property-value specification: "
          + userProp);
      }

      setUserSuppliedProperty(parts[0].trim(), parts[1].trim());
    }
  }

  @Override
  public String[] getOptions() {
    List<String> opts = new ArrayList<String>();

    List<String> userProps = getUserSuppliedPropertyNames();
    for (String p : userProps) {
      String propVal = p + "=";
      String v = getUserSuppliedProperty(p);
      if (!DistributedJobConfig.isEmpty(v)) {
        propVal += v;
        opts.add("-user-prop");
        opts.add(propVal);
      }
    }

    return opts.toArray(new String[opts.size()]);
  }

  /**
   * Set a configuration property
   * 
   * @param propName property name
   * @param propValue property value
   */
  public void setProperty(String propName, String propValue) {

    if (propValue == null) {
      propValue = "";
    }

    m_configProperties.put(propName, propValue);
  }

  /**
   * Get a configuration property.
   * 
   * @param propName property name
   * @return the value of the property or the empty string if the property is
   *         not set
   */
  public String getProperty(String propName) {
    String r = m_configProperties.get(propName);

    return r == null ? "" : r;
  }

  /**
   * Get a list of all the properties that have values.
   * 
   * @return a list of the set properties.
   */
  public List<String> getPropertyNames() {
    List<String> names = new ArrayList<String>();

    for (String s : m_configProperties.keySet()) {
      names.add(s);
    }

    return names;
  }

  /**
   * Set a user-supplied property
   * 
   * @param propName property name
   * @param propValue property value
   */
  public void setUserSuppliedProperty(String propName, String propValue) {
    if (propValue == null) {
      propValue = "";
    }
    m_additionalUserSuppliedProperties.put(propName, propValue);
  }

  /**
   * Get a user-supplied property
   * 
   * @param propName property name
   * @return the value of the named property or the empty string if it is not
   *         set.
   */
  public String getUserSuppliedProperty(String propName) {
    String r = m_additionalUserSuppliedProperties.get(propName);

    return r == null ? "" : r;
  }

  /**
   * Get a list of the user-supplied property names
   * 
   * @return a list of the user-supplied properties that have values
   */
  public List<String> getUserSuppliedPropertyNames() {
    List<String> names = new ArrayList<String>();

    for (String s : m_additionalUserSuppliedProperties.keySet()) {
      names.add(s);
    }

    return names;
  }

  /**
   * Get the full map of user-supplied properties
   * 
   * @return the map of user supplied properties
   */
  public Map<String, String> getUserSuppliedProperties() {
    return m_additionalUserSuppliedProperties;
  }

  /**
   * Clear the map of user-supplied properties
   */
  public void clearUserSuppliedProperties() {
    m_additionalUserSuppliedProperties.clear();
  }

  /**
   * Utility method that returns true if the supplied string is null or the
   * empty string
   * 
   * @param s the string to check
   * @return true if the string is null or empty.
   */
  public static boolean isEmpty(String s) {
    return s == null || s.length() == 0;
  }
}
