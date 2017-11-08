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

/**
 * CapabilitiesUtils.java
 * Copyright (C) 2017 University of Waikato, Hamilton, NZ
 */

package weka.core;

/**
 * Helper class for Capabilities.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class CapabilitiesUtils {

  /**
   * returns a comma-separated list of all the capabilities, excluding interface-based ones.
   *
   * @param c the capabilities to get a string representation from
   * @return the string describing the capabilities
   */
  public static String listCapabilities(Capabilities c) {
    return c.listCapabilities();
  }

  /**
   * generates a string from the capabilities, suitable to add to the help
   * text.
   *
   * @param title the title for the capabilities
   * @param c the capabilities
   * @return a string describing the capabilities
   */
  public static String addCapabilities(String title, Capabilities c) {
   return c.addCapabilities(title);
  }
}
