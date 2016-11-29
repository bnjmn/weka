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
 *    SendToPerspectiveGraphicalCommand.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.Perspective;

import java.util.List;

/**
 * Class implementing sending a set of Instances to a named perspective
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SendToPerspectiveGraphicalCommand extends AbstractGraphicalCommand {

  /** Command ID */
  public static final String SEND_TO_PERSPECTIVE_COMMAND_KEY =
    "sendToPerspective";

  /** The main KF perspective */
  protected MainKFPerspective m_mainPerspective;

  /**
   * Set the graphical environment
   *
   * @param graphicalEnvironment the graphical environment
   */
  @Override
  public void setGraphicalEnvironment(Object graphicalEnvironment) {
    super.setGraphicalEnvironment(graphicalEnvironment);

    if (graphicalEnvironment instanceof MainKFPerspective) {
      m_mainPerspective = (MainKFPerspective) graphicalEnvironment;
    }
  }

  /**
   * Get the name of the command
   *
   * @return the name of the command
   */
  @Override
  public String getCommandName() {
    return SEND_TO_PERSPECTIVE_COMMAND_KEY;
  }

  /**
   * Get the description of this command
   *
   * @return the description of this command
   */
  @Override
  public String getCommandDescription() {
    return "Send the supplied instances to the named perspective";
  }

  /**
   * Execute the command
   *
   * @param commandArgs arguments to the command
   * @return null (no return value for this command)
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object performCommand(Object... commandArgs) throws WekaException {
    if (commandArgs.length != 2 && !(commandArgs[1] instanceof Instances)) {
      throw new WekaException(
        "Was expecting two arguments: 1) perspective name, and 2) "
          + "argument of type weka.core.Instances");
    }

    Instances toSend = (Instances) commandArgs[1];
    String perspectiveName = commandArgs[0].toString();
    List<Perspective> perspectives =
      m_mainPerspective.getMainApplication().getPerspectiveManager()
        .getVisiblePerspectives();

    Perspective target = null;
    String targetID = null;
    for (Perspective p : perspectives) {
      if (p.acceptsInstances()
        && p.getPerspectiveTitle().equalsIgnoreCase(perspectiveName)) {
        targetID = p.getPerspectiveID();
        target = p;
        break;
      }
    }
    if (target == null) {
      throw new WekaException("Was unable to find requested perspective");
    }

    target.setInstances(toSend);
    m_mainPerspective.getMainApplication().getPerspectiveManager()
      .setActivePerspective(targetID);
    m_mainPerspective.getMainApplication().getPerspectiveManager()
      .setEnablePerspectiveTab(targetID, true);

    return null;
  }
}
