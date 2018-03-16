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
 * Set.java
 * Copyright (C) 2018 University of Waikato, Hamilton, NZ
 */

package weka.gui.simplecli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sets a variable.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class Set
  extends AbstractCommand {

  /**
   * Returns the name of the command.
   *
   * @return		the name
   */
  @Override
  public String getName() {
    return "set";
  }

  /**
   * Returns the help string (no indentation).
   *
   * @return		the help
   */
  @Override
  public String getHelp() {
    return "Sets a variable.\n"
      + "If no key=value pair is given all current variables are listed.";
  }

  /**
   * Returns the one-liner help string for the parameters.
   *
   * @return		the help, empty if none available
   */
  public String getParameterHelp() {
    return "[name=value]";
  }

  /**
   * Executes the command with the given parameters.
   *
   * @param params 	the parameters for the command
   * @throws Exception	if command fails
   */
  @Override
  protected void doExecute(String[] params) throws Exception {
    String  		name;
    String		value;
    List<String> 	names;

    if (params.length == 0) {
      names = new ArrayList<>(m_Owner.getVariables().keySet());
      if (names.size() == 0) {
	System.out.println("No variables stored!");
      }
      else {
	Collections.sort(names);
	for (String n: names)
	  System.out.println(n + "=" + m_Owner.getVariables().get(n));
      }
      return;
    }

    if (params.length != 1)
      throw new Exception("Expected exactly one argument: name=value");
    if (!params[0].contains("="))
      throw new Exception("Expected format 'name=value', encountered: " + params[0]);

    name  = params[0].substring(0, params[0].indexOf("="));
    value = params[0].substring(params[0].indexOf("=") + 1);

    m_Owner.getVariables().put(name, value);
  }
}
