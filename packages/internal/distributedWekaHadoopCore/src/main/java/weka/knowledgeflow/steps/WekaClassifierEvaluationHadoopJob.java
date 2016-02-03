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
 *    WekaClassifierEvaluationHadoopStep
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import distributed.core.DistributedJobConfig;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.List;

/**
 * Knowledge Flow step for the classifier evaluation Spark job
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "WekaClassifierEvaluationHadoopJob", category = "Hadoop",
  toolTipText = "Builds and evaluates an aggregated classifier "
    + "via cross-validation in Spark", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "WekaClassifierEvaluationHadoopJob.gif")
public class WekaClassifierEvaluationHadoopJob extends AbstractHadoopJob {

  private static final long serialVersionUID = -5581130186524164522L;

  /**
   * Constructor
   */
  public WekaClassifierEvaluationHadoopJob() {
    super();

    m_job = new weka.distributed.hadoop.WekaClassifierEvaluationHadoopJob();
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = super.getOutgoingConnectionTypes();
      result.add(StepManager.CON_DATASET);
      result.add(StepManager.CON_TEXT);

    return result;
  }

  @Override
  public void notifyJobOutputConnections() throws WekaException {
    String evalText =
      ((weka.distributed.hadoop.WekaClassifierEvaluationHadoopJob) m_runningJob)
        .getText();

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_TEXT) > 0) {
      if (!DistributedJobConfig.isEmpty(evalText)) {

        Data textData = new Data(StepManager.CON_TEXT, evalText);
        textData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
          "Hadoop - evaluation result");

        getStepManager().outputData(textData);
      } else {
        getStepManager().logWarning("No evaluation results produced!");
      }
    }

    Instances evalInstances =
      ((weka.distributed.hadoop.WekaClassifierEvaluationHadoopJob) m_runningJob)
        .getInstances();

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_DATASET) > 0) {
      if (evalInstances != null) {
        Data evalData = new Data(StepManager.CON_DATASET, evalInstances);
        evalData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
        evalData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);

        getStepManager().outputData(evalData);
      } else {
        getStepManager().logWarning("No evaluation results produced!");
      }
    }
  }
}
