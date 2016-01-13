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
 *    LoggingLevel.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

/**
 * Enum for different logging levels
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public enum LoggingLevel {
  NONE("None"), LOW("Low"), BASIC("Basic"), DETAILED("Detailed"), DEBUGGING(
    "Debugging"), WARNING("WARNING"), ERROR("ERROR");

  /** The name of this logging level */
  private final String m_name;

  /**
   * Constructor
   *
   * @param name the name of this logging level
   */
  LoggingLevel(String name) {
    m_name = name;
  }

  /**
   * Return LoggingLevel given its name as a string
   *
   * @param s the string containing the name of the logging level
   * @return the LoggingLevel (or LoggingLevel.BASIC if the string could
   * not be matched)
   */
  public static LoggingLevel stringToLevel(String s) {
    LoggingLevel ret = LoggingLevel.BASIC;
    for (LoggingLevel l : LoggingLevel.values()) {
      if (l.toString().equals(s)) {
        ret = l;
      }
    }
    return ret;
  }

  /**
   * String representation
   *
   * @return the string representation of this logging level
   */
  @Override
  public String toString() {
    return m_name;
  }
}
