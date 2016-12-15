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
 *    WriteDataToResult.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Environment;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.JobEnvironment;
import weka.knowledgeflow.StepManager;

import java.util.Arrays;
import java.util.List;

/**
 * Step that stores incoming non-incremental data in the job environment
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "WriteDataToResult",
  category = "Flow",
  toolTipText = "Write incoming non-incremental data to the results store in the "
    + "job environment", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "WriteDataToResult.gif")
public class WriteDataToResult extends BaseStep {
  private static final long serialVersionUID = -1932252461151862615L;

  @Override
  public void stepInit() throws WekaException {
    Environment env =
      getStepManager().getExecutionEnvironment().getEnvironmentVariables();

    if (!(env instanceof JobEnvironment)) {
      JobEnvironment jobEnvironment = new JobEnvironment(env);
      getStepManager().getExecutionEnvironment().setEnvironmentVariables(
        jobEnvironment);
    }
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    JobEnvironment jobEnvironment =
      (JobEnvironment) getStepManager().getExecutionEnvironment()
        .getEnvironmentVariables();
    getStepManager().logDetailed(
      "Storing " + data.getConnectionName() + " in " + "result");
    jobEnvironment.addToResult(data);
    getStepManager().finished();
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_TESTSET, StepManager.CON_BATCH_CLASSIFIER,
      StepManager.CON_BATCH_CLUSTERER, StepManager.CON_BATCH_ASSOCIATOR,
      StepManager.CON_TEXT, StepManager.CON_IMAGE);
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return null;
  }
}
