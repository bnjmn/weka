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
 * Capabilities.java
 * Copyright (C) 2018 University of Waikato, Hamilton, NZ
 */

package weka.gui.simplecli;

import weka.core.CapabilitiesHandler;
import weka.core.OptionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Outputs the capabilities of the specified class.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class Capabilities
  extends AbstractCommand {

  /**
   * Returns the name of the command.
   *
   * @return		the name
   */
  @Override
  public String getName() {
    return "capabilities";
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
    try {
      Object obj = Class.forName(params[0]).newInstance();
      if (obj instanceof CapabilitiesHandler) {
        if (obj instanceof OptionHandler) {
          List<String> args = new ArrayList<>();
          for (int i = 1; i < params.length; i++) {
            args.add(params[i]);
          }
          ((OptionHandler) obj).setOptions(args.toArray(new String[args.size()]));
        }
        weka.core.Capabilities caps = ((CapabilitiesHandler) obj).getCapabilities();
        System.out.println(caps.toString().replace("[", "\n").replace("]", "\n"));
      } else {
        System.out.println("'" + params[0] + "' is not a "
          + CapabilitiesHandler.class.getName() + "!");
      }
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}
