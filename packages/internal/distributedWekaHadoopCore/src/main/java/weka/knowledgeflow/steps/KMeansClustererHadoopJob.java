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
 *    KMeansClustererHadoopJob
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.List;

/**
 * Knowledge flow step for the k-means|| Hadoop job
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "KMeansClustererHadoopJob", category = "Hadoop",
  toolTipText = "Learns a k-means++ clusterer in Hadoop using either standard "
    + "random initialization or k-means|| initialization.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "KMeansClustererHadoopJob.gif")
public class KMeansClustererHadoopJob extends AbstractHadoopJob {

  private static final long serialVersionUID = 1150217938097090061L;

  public KMeansClustererHadoopJob() {
    super();
    m_job = new weka.distributed.hadoop.KMeansClustererHadoopJob();
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = super.getOutgoingConnectionTypes();
      result.add(StepManager.CON_TEXT);
      result.add(StepManager.CON_BATCH_CLUSTERER);

    return result;
  }

  @Override
  protected void notifyJobOutputConnections() throws WekaException {
    weka.clusterers.Clusterer finalClusterer =
      ((weka.distributed.hadoop.KMeansClustererHadoopJob) m_runningJob)
        .getClusterer();
    Instances modelHeader =
      ((weka.distributed.hadoop.KMeansClustererHadoopJob) m_runningJob)
        .getTrainingHeader();

    if (modelHeader == null) {
      getStepManager()
        .logWarning("No training header available for the model!");
    }

    if (finalClusterer != null) {
      if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_TEXT) > 0) {
        String textual = finalClusterer.toString();

        String title = "Hadoop: ";
        String clustererSpec = finalClusterer.getClass().getName();
        clustererSpec +=
          " "
            + Utils.joinOptions(((OptionHandler) finalClusterer).getOptions());
        title += clustererSpec;

        Data textData = new Data(StepManager.CON_TEXT, textual);
        textData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE, title);
        getStepManager().outputData(textData);
      }

      if (modelHeader != null
        && getStepManager().numOutgoingConnectionsOfType(
          StepManager.CON_BATCH_CLUSTERER) > 0) {
        Data batchClusterer =
          new Data(StepManager.CON_BATCH_CLUSTERER, finalClusterer);
        batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_TRAININGSET,
          modelHeader);
        batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
        batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
          1);
        batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_LABEL,
          getName());
        getStepManager().outputData(batchClusterer);
      }
    }
  }
}
