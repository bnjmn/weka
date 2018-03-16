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
 * Help.java
 * Copyright (C) 2018 University of Waikato, Hamilton, NZ
 */

package weka.gui.simplecli;

import java.util.ArrayList;
import java.util.List;

/**
 * Outputs help for a command or for all.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class Help
  extends AbstractCommand {

  /**
   * Returns the name of the command.
   *
   * @return		the name
   */
  @Override
  public String getName() {
    return "help";
  }

  /**
   * Returns the help string (no indentation).
   *
   * @return		the help
   */
  @Override
  public String getHelp() {
    return "Outputs the help for the specified command or, if omitted,\n"
      + "for all commands.";
  }

  /**
   * Returns the one-liner help string for the parameters.
   *
   * @return		the help, empty if none available
   */
  public String getParameterHelp() {
    return "[command1] [command2] [...]";
  }

  /**
   * Executes the command with the given parameters.
   *
   * @param params 	the parameters for the command
   * @throws Exception	if command fails
   */
  @Override
  protected void doExecute(String[] params) throws Exception {
    List<AbstractCommand>	cmds;
    AbstractCommand		cmd;
    List<AbstractCommand>	help;
    boolean			all;

    all  = false;
    cmds = getCommands();

    // specific command?
    help = new ArrayList<>();
    for (String param: params) {
      cmd = getCommand(param);
      if (cmd != null) {
	help.add(cmd);
	break;
      }
      else {
        throw new Exception("Unknown command: " + param);
      }
    }
    // all?
    if (help.isEmpty()) {
      all = true;
      help.addAll(cmds);
    }

    for (AbstractCommand c: help) {
      System.out.println(c.getName() + (c.getParameterHelp().isEmpty() ? "" : " " + c.getParameterHelp()));
      String[] lines = c.getHelp().split("\n");
      for (String line: lines)
	System.out.println("\t" + line);
      System.out.println();
    }

    // additional information
    if (all) {
      System.out.println("\nNotes:");
      System.out.println("- Variables can be used anywhere using '${<name>}' with '<name>'");
      System.out.println("  being the name of the variable.");
      System.out.println("- Environment variables can be used with '${env.<name>}', ");
      System.out.println("  e.g., '${env.PATH}' to retrieve the PATH variable.");
    }
  }
}
