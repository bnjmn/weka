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
 *    Defaults.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for providing a set of default settings for an application.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class Defaults implements Serializable {

  /** For serialization */
  private static final long serialVersionUID = 1061521489520308096L;

  /** Identifier for this set of defaults */
  protected String m_defaultsID = "";

  /** Maintains the list of default settings */
  protected Map<Settings.SettingKey, Object> m_defaults =
    new LinkedHashMap<Settings.SettingKey, Object>();

  /**
   * Construct a new empty Defaults
   *
   * @param ID the ID for this set of defaults
   */
  public Defaults(String ID) {
    setID(ID);
  }

  /**
   * Construct a new Defaults
   *
   * @param ID the ID for this set of defaults
   * @param defaults the default settings to use
   */
  public Defaults(String ID, Map<Settings.SettingKey, Object> defaults) {
    this(ID);
    m_defaults = defaults;
  }

  /**
   * Get the ID of this set of defaults
   *
   * @return the ID of this set of defaults
   */
  public String getID() {
    return m_defaultsID;
  }

  /**
   * Set the ID for this set of defaults
   *
   * @param ID the ID to use
   */
  public void setID(String ID) {
    m_defaultsID = ID;
  }

  /**
   * Get the map of default settings
   *
   * @return the map of default settings
   */
  public Map<Settings.SettingKey, Object> getDefaults() {
    return m_defaults;
  }

  /**
   * Add the supplied defaults to this one. Note that the added defaults now
   * come under the ID of this set of defaults.
   *
   * @param toAdd the defaults to add
   */
  public void add(Defaults toAdd) {
    m_defaults.putAll(toAdd.getDefaults());
  }
}
