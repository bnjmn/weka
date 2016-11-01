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
 *    SendToPerspectiveStepEditorDialog.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.GetPerspectiveNamesGraphicalCommand;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.gui.knowledgeflow.SendToPerspectiveGraphicalCommand;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Step that can send incoming instances to a perspective. Only operates
 * in a graphical (i.e. non-headless) environment.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "SendToPerspective", category = "Flow",
  toolTipText = "Send instances to a perspective (graphical environment only)",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DiamondPlain.gif")
public class SendToPerspective extends BaseStep {

  private static final long serialVersionUID = 7322550048407408819L;

  protected String m_perspectiveName = "";

  public void setPerspectiveName(String name) {
    m_perspectiveName = name;
  }

  public String getPerspectiveName() {
    return m_perspectiveName;
  }

  @Override
  public void stepInit() throws WekaException {
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    if (getStepManager().getExecutionEnvironment().isHeadless()) {
      getStepManager().logWarning(
        "Unable to send data to perspective due to "
          + "execution in a headless environment.");
    } else {
      if (m_perspectiveName == null || m_perspectiveName.length() == 0) {
        getStepManager().logWarning("No perspective specified");
      } else {
        List<String> visiblePerspectives =
          getStepManager()
            .getExecutionEnvironment()
            .getGraphicalEnvironmentCommandHandler()
            .performCommand(
              GetPerspectiveNamesGraphicalCommand.GET_PERSPECTIVE_NAMES_KEY);
        if (!visiblePerspectives.contains(m_perspectiveName)) {
          throw new WekaException("The perspective to send to '"
            + m_perspectiveName + "' does not seem to be available");
        }

        Instances toSend = data.getPrimaryPayload();
        if (toSend != null) {
          getStepManager()
            .getExecutionEnvironment()
            .getGraphicalEnvironmentCommandHandler()
            .performCommand(
              SendToPerspectiveGraphicalCommand.SEND_TO_PERSPECTIVE_COMMAND_KEY,
              m_perspectiveName, toSend);
        }
      }
    }

    getStepManager().finished();
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<>();
    if (getStepManager().numIncomingConnections() == 0) {
      result.add(StepManager.CON_DATASET);
      result.add(StepManager.CON_TRAININGSET);
      result.add(StepManager.CON_TESTSET);
    }

    return result;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return null;
  }
}
