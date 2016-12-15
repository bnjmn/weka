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
 *    GetDataFromResult.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.JobEnvironment;
import weka.knowledgeflow.StepManager;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Step that outputs data stored in the job environment
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "GetDataFromResult", category = "Flow",
  toolTipText = "Output data stored in the job environment",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "GetDataFromResult.gif")
public class GetDataFromResult extends BaseStep {

  private static final long serialVersionUID = 7447845310997458636L;

  @Override
  public void stepInit() throws WekaException {

  }

  @Override
  public void start() throws WekaException {
    if (getStepManager().numIncomingConnections() == 0
      && getStepManager().getExecutionEnvironment().getEnvironmentVariables() instanceof JobEnvironment) {
      JobEnvironment jobEnvironment =
        (JobEnvironment) getStepManager().getExecutionEnvironment()
          .getEnvironmentVariables();
      outputDataFromResult(jobEnvironment.getResultData());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void processIncoming(Data data) throws WekaException {
    outputDataFromResult((Map<String, LinkedHashSet<Data>>) data
      .getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_RESULTS));
  }

  protected void outputDataFromResult(Map<String, LinkedHashSet<Data>> results)
    throws WekaException {
    if (results != null && results.size() > 0) {
      getStepManager().processing();
      Set<String> outConns = getStepManager().getOutgoingConnections().keySet();
      for (String conn : outConns) {
        LinkedHashSet<Data> connData = results.get(conn);
        if (connData != null) {
          for (Data d : connData) {
            getStepManager().outputData(d);
          }
        } else {
          getStepManager().logBasic(
            "No results of type '" + conn + "' in job " + "environment");
        }
      }
    } else {
      getStepManager().logBasic("No results to output from job environment");
    }
    getStepManager().finished();

  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_JOB_SUCCESS);
    }

    return null;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_TESTSET, StepManager.CON_BATCH_CLASSIFIER,
      StepManager.CON_BATCH_CLUSTERER, StepManager.CON_BATCH_ASSOCIATOR,
      StepManager.CON_TEXT, StepManager.CON_IMAGE);
  }
}
