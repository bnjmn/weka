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
 * Java.java
 * Copyright (C) 2018 University of Waikato, Hamilton, NZ
 */

package weka.gui.simplecli;

import weka.core.OptionHandler;
import weka.gui.SimpleCLIPanel.ClassRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets a variable.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class Java
  extends AbstractCommand {

  /**
   * Returns the name of the command.
   *
   * @return		the name
   */
  @Override
  public String getName() {
    return "java";
  }

  /**
   * Returns the help string (no indentation).
   *
   * @return		the help
   */
  @Override
  public String getHelp() {
    return "Lists the capabilities of the specified class.\n"
      + "If the class is a " + OptionHandler.class.getName() + " then\n"
      + "trailing options after the classname will be\n"
      + "set as well.\n";
  }

  /**
   * Returns the one-liner help string for the parameters.
   *
   * @return		the help, empty if none available
   */
  public String getParameterHelp() {
    return "<classname> <args>";
  }

  /**
   * Executes the command with the given parameters.
   *
   * @param params 	the parameters for the command
   * @throws Exception	if command fails
   */
  @Override
  protected void doExecute(String[] params) throws Exception {
    // Execute the main method of a class
    try {
      if (params.length == 0) {
        throw new Exception("No class name given");
      }
      String className = params[0];
      params[0] = "";
      if (m_Owner.isBusy()) {
        throw new Exception("An object is already running, use \"kill\""
          + " to stop it.");
      }
      Class<?> theClass = Class.forName(className);

      // some classes expect a fixed order of the args, i.e., they don't
      // use Utils.getOption(...) => create new array without first two
      // empty strings (former "java" and "<classname>")
      List<String> argv = new ArrayList<>();
      for (int i = 1; i < params.length; i++) {
        argv.add(params[i]);
      }

      m_Owner.startThread(new ClassRunner(m_Owner, theClass, argv.toArray(new String[argv.size()])));
    }
    catch (Exception e) {
      System.out.println(e);
    }
  }
}
