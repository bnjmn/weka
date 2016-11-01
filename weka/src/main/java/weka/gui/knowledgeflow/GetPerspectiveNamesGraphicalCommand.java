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
 *    GetPerspectiveNamesGraphicalCommand.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.WekaException;
import weka.gui.Perspective;
import weka.knowledgeflow.KFDefaults;

import java.util.ArrayList;
import java.util.List;

/**
 * Class implementing a command for getting the names of all visible
 * perspectives
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class GetPerspectiveNamesGraphicalCommand extends
  AbstractGraphicalCommand {

  /** Command ID */
  public static final String GET_PERSPECTIVE_NAMES_KEY = "getPerspectiveNames";

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
    return GET_PERSPECTIVE_NAMES_KEY;
  }

  /**
   * Get the description of this command
   *
   * @return the description of this command
   */
  @Override
  public String getCommandDescription() {
    return "Gets the names of of visible Knowledge Flow perspectives";
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
  public List<String> performCommand(Object... commandArgs)
    throws WekaException {
    if (m_mainPerspective == null) {
      throw new WekaException("This command cannot be applied in the "
        + "current graphical environment");
    }
    List<Perspective> perspectives =
      m_mainPerspective.getMainApplication().getPerspectiveManager()
        .getVisiblePerspectives();
    List<String> result = new ArrayList<>();
    for (Perspective p : perspectives) {
      if (!p.getPerspectiveID()
        .equalsIgnoreCase(KFDefaults.MAIN_PERSPECTIVE_ID)) {
        result.add(p.getPerspectiveTitle());
      }
    }

    return result;
  }
}
