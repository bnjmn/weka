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
 * Exit.java
 * Copyright (C) 2018 University of Waikato, Hamilton, NZ
 */

package weka.gui.simplecli;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowEvent;

/**
 * Closes the Simple CLI window.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class Exit
  extends AbstractCommand {

  /**
   * Returns the name of the command.
   *
   * @return		the name
   */
  @Override
  public String getName() {
    return "exit";
  }

  /**
   * Returns the help string (no indentation).
   *
   * @return		the help
   */
  @Override
  public String getHelp() {
    return "Exits the SimpleCLI program.";
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
    // Shut down
    // determine parent
    Container parent = m_Owner.getParent();
    Container frame = null;
    boolean finished = false;
    while (!finished) {
      if ((parent instanceof JFrame) || (parent instanceof Frame)
        || (parent instanceof JInternalFrame)) {
        frame = parent;
        finished = true;
      }

      if (!finished) {
        parent = parent.getParent();
        finished = (parent == null);
      }
    }
    // fire the frame close event
    if (frame != null) {
      if (frame instanceof JInternalFrame) {
        ((JInternalFrame) frame).doDefaultCloseAction();
      }
      else {
        ((Window) frame).dispatchEvent(new WindowEvent((Window) frame,
          WindowEvent.WINDOW_CLOSING));
      }
    }
  }
}
