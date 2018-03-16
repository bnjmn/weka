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
 * Cls.java
 * Copyright (C) 2018 University of Waikato, Hamilton, NZ
 */

package weka.gui.simplecli;

/**
 * Clears the output area.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class Cls
  extends AbstractCommand {

  /**
   * Returns the name of the command.
   *
   * @return		the name
   */
  @Override
  public String getName() {
    return "cls";
  }

  /**
   * Returns the help string (no indentation).
   *
   * @return		the help
   */
  @Override
  public String getHelp() {
    return "Clears the output area.";
  }

  /**
   * Returns the one-liner help string for the parameters.
   *
   * @return		the help, empty if none available
   */
  public String getParameterHelp() {
    return "";
  }

  /**
   * Executes the command with the given parameters.
   *
   * @param params 	the parameters for the command
   * @throws Exception	if command fails
   */
  @Override
  protected void doExecute(String[] params) throws Exception {
    // Clear the text area
    m_Owner.getOutputArea().setText("");
  }
}
