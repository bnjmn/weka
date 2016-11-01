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
 *    GraphicalEnvironmentCommandHandler.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.WekaException;

/**
 * Interface for graphical command handlers
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface GraphicalEnvironmentCommandHandler {

  /**
   * Attempt to perform a graphical command (if supported) in the current
   * graphical environment
   *
   * @param commandName the name of the command to execute
   * @param commandArgs the optional arguments
   * @return the result of performing the command, or null if the command does
   *         not return a result
   * @throws WekaException if a problem occurs
   */
  <T> T performCommand(String commandName, Object... commandArgs)
    throws WekaException;
}
