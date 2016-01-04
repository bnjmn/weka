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
 *    KettleInject.java
 *    Copyright (C) 2008-2015 Pentaho Corporation
 */

package weka.knowledgeflow.steps;

import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point into a KnowledgeFlow for data from a Kettle transform
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "KettleInject", category = "DataSources",
  toolTipText = "Bridge to accept input from Kettle/PDI. Use this step "
    + "when designing Knowledge Flow processes that will be executed by "
    + "the Pentaho Data Integration Knowledge Flow plugin step",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "KettleInject.gif")
public class KettleInject extends BaseStep {

  private static final long serialVersionUID = -3290687280752790168L;

  @Override
  public void stepInit() throws WekaException {
    if (getStepManager().numOutgoingConnections() == 0) {
      throw new WekaException("No outgoing connections!");
    }
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {
      result.add(StepManager.CON_INSTANCE);
    } else if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_DATASET) > 0) {
      result.add(StepManager.CON_DATASET);
    } else if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_TRAININGSET) > 0) {
      result.add(StepManager.CON_TRAININGSET);
    } else if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_TESTSET) > 0) {
      result.add(StepManager.CON_TESTSET);
    }

    return result;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    // if no connections yet, then we can produce anything that
    // the downstream step(s) can accept
    if (getStepManager().numOutgoingConnections() == 0) {
      result.add(StepManager.CON_INSTANCE);
      result.add(StepManager.CON_DATASET);
      result.add(StepManager.CON_TRAININGSET);
      result.add(StepManager.CON_TESTSET);
    } else if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {
      result.add(StepManager.CON_INSTANCE);
    } else if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_DATASET) > 0) {
      result.add(StepManager.CON_DATASET);
    } else if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_TRAININGSET) > 0) {
      result.add(StepManager.CON_TRAININGSET);
    } else if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_TESTSET) > 0) {
      result.add(StepManager.CON_TESTSET);
    }

    return result;
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    // just pass on the data!
    getStepManager().outputData(data);
  }
}
