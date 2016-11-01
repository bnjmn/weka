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
 *    AbstractGraphicalCommand.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.WekaException;

/**
 * Base class for a graphical command
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class AbstractGraphicalCommand {

  /** A reference to the graphical environment */
  protected Object m_graphicalEnv;

  /** Set a reference to the graphical environment */
  public void setGraphicalEnvironment(Object env) {
    m_graphicalEnv = env;
  }

  /**
   * Get the name of this command
   *
   * @return the name of this command
   */
  public abstract String getCommandName();

  /**
   * Get a description of this command
   *
   * @return a description of this command
   */
  public abstract String getCommandDescription();

  /**
   * Perform the command
   *
   * @param commandArgs arguments to the command
   * @param <T> the return type
   * @return the result, or null if the command does not return a result
   * @throws WekaException if a problem occurs
   */
  public abstract <T> T performCommand(Object... commandArgs)
    throws WekaException;
}
