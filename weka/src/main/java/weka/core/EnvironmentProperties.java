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
 * EnvironmentProperties.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.util.Properties;
import java.util.Set;

/**
 * Extends Properties to allow the value of a system property (if set) to
 * override that which has been loaded/set. This allows the user to override
 * from the command line any property that is loaded into a property file via
 * weka.core.Utils.readProperties().
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class EnvironmentProperties extends Properties {

  protected transient Environment m_env = Environment.getSystemWide();

  public EnvironmentProperties() {
    super();
  }

  public EnvironmentProperties(Properties props) {
    super(props);
  }

  @Override
  public String getProperty(String key) {
    // allow system-wide properties to override
    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }
    String result = m_env.getVariableValue(key);

    if (result == null) {
      result = super.getProperty(key);
    }

    return result;
  }
}
