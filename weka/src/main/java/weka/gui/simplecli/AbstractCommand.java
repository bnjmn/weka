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
 * AbstractCommand.java
 * Copyright (C) 2018 University of Waikato, Hamilton, NZ
 */

package weka.gui.simplecli;

import weka.core.PluginManager;
import weka.core.Utils;
import weka.gui.SimpleCLIPanel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ancestor for command.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public abstract class AbstractCommand
  implements Serializable, Comparable<AbstractCommand> {

  /** the owner. */
  protected SimpleCLIPanel m_Owner;

  /** the available commands. */
  protected static List<AbstractCommand> m_Commands;

  /**
   * Sets the owner.
   *
   * @param value	the owner
   */
  public void setOwner(SimpleCLIPanel value) {
    m_Owner = value;
  }

  /**
   * Returns the owner.
   *
   * @return		the owner
   */
  public SimpleCLIPanel getOwner() {
    return m_Owner;
  }

  /**
   * Returns the name of the command.
   *
   * @return		the name
   */
  public abstract String getName();

  /**
   * Returns the help string (no indentation).
   *
   * @return		the help
   */
  public abstract String getHelp();

  /**
   * Returns the one-liner help string for the parameters.
   *
   * @return		the help, empty if none available
   */
  public abstract String getParameterHelp();

  /**
   * Executes the command with the given parameters.
   *
   * @param params 	the parameters for the command
   * @throws Exception	if command fails
   */
  protected abstract void doExecute(String[] params) throws Exception;

  /**
   * Expands the variables in the string.
   *
   * @param s		the string to expand
   * @return		the expanded string
   */
  protected String expandVars(String s) throws Exception {
    String	result;
    int		pos;
    int		lastPos;
    int		endPos;
    String	var;

    result = s;
    pos    = -1;
    while (true) {
      lastPos = pos;
      pos     = result.indexOf("${", lastPos);
      if (pos == -1) {
	break;
      }
      if (lastPos == pos) {
	throw new Exception("Failed to expand variables in string: " + s);
      }
      endPos = result.indexOf("}", pos);
      if (endPos == -1) {
	throw new Exception("Failed to expand variables in string: " + s);
      }
      var = result.substring(pos + 2, endPos);
      if (var.startsWith("env.")) {
        var = var.substring(4);
        if (System.getenv(var) != null) {
	  result = result.substring(0, pos) + System.getenv(var) + result.substring(endPos + 1);
	}
	else {
	  throw new Exception("Unknown environment variable: " + var);
	}
      }
      else {
	if (m_Owner.getVariables().containsKey(var)) {
	  result = result.substring(0, pos) + m_Owner.getVariables().get(var) + result.substring(endPos + 1);
	}
	else {
	  throw new Exception("Unknown variable: " + var);
	}
      }
    }

    return result;
  }

  /**
   * Executes the command with the given parameters.
   * Expands any variables in the parameters.
   *
   * @param params 	the parameters for the command
   * @throws Exception	if command fails
   */
  public void execute(String[] params) throws Exception {
    int		i;

    if (m_Owner == null)
      throw new Exception("No SimpleCLI owner set!");

    for (i = 0; i < params.length; i++)
      params[i] = expandVars(params[i]);

    doExecute(params);

    m_Owner = null;
  }

  /**
   * Performs comparison just on the name.
   *
   * @param o		the other command to compare with
   * @return		less than, equal to, or greater than 0
   * @see		#getName()
   */
  @Override
  public int compareTo(AbstractCommand o) {
    return getName().compareTo(o.getName());
  }

  /**
   * Returns true if the object is a command with the same name.
   *
   * @param obj		the other object to compare with
   * @return		true if the same
   */
  @Override
  public boolean equals(Object obj) {
    return (obj instanceof AbstractCommand) && (compareTo((AbstractCommand) obj) == 0);
  }

  /**
   * Returns all available commands.
   *
   * @return		the commands
   */
  public static synchronized List<AbstractCommand> getCommands() {
    List<String> 		classes;
    List<AbstractCommand>	cmds;
    AbstractCommand		cmd;

    if (m_Commands == null) {
      // get commands
      classes = PluginManager.getPluginNamesOfTypeList(AbstractCommand.class.getName());
      cmds    = new ArrayList<>();
      for (String cls: classes) {
	try {
	  cmd = (AbstractCommand) Utils.forName(AbstractCommand.class, cls, new String[0]);
	  cmds.add(cmd);
	}
	catch (Exception e) {
	  System.err.println("Failed to instantiate SimpleCLI command: " + cls);
	  e.printStackTrace();
	}
      }
      Collections.sort(cmds);
      m_Commands = cmds;
    }
    return m_Commands;
  }

  /**
   * Locates the command for the given name.
   *
   * @param name	the command to look for
   * @return		the command, null if not found
   */
  public static AbstractCommand getCommand(String name) {
    AbstractCommand	result;

    result = null;
    for (AbstractCommand c: getCommands()) {
      if (c.getName().equals(name)) {
	result = c;
	break;
      }
    }

    return result;
  }
}
