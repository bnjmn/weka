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
 *    CorrelationMatrixSparkStep
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import distributed.core.DistributedJobConfig;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.awt.Image;
import java.util.List;

/**
 * Knowledge Flow step for the correlation matrix/PCA job
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "CorrelationMatrixSparkJob", category = "Spark",
  toolTipText = "Computes a correlation/covariance matrix for numeric data "
    + "in Spark. The data can include a class attribute, which "
    + "can be part of the correlation analysis if it is numeric "
    + "or ignored if it is nominal. The user can optionally have "
    + "the job perform a PCA analysis using the computed "
    + "correlation/covariance matrix as input. Note that this "
    + "is done outside of Spark on the client machine as a "
    + "postprocessing step, so is suitable for data that does not "
    + "conatain a large number of columns. The PCA analysis will "
    + "be written back into the output directory, along "
    + "with a serialized PCA filter that can be used for preprocessing "
    + "data in the WekaClassfierSpark job.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "CorrelationMatrixSparkJob.gif")
public class CorrelationMatrixSparkJob extends AbstractSparkJob {

  private static final long serialVersionUID = 8919207015338745542L;

  public CorrelationMatrixSparkJob() {
    super();
    m_job = new weka.distributed.spark.CorrelationMatrixSparkJob();
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = super.getOutgoingConnectionTypes();
    if (getStepManager().numIncomingConnections() > 0) {
      result.add(StepManager.CON_TEXT);
      result.add(StepManager.CON_IMAGE);
    }

    return result;
  }

  @Override
  protected void notifyJobOutputConnections() throws WekaException {
    if (((weka.distributed.spark.CorrelationMatrixSparkJob) m_job)
      .getRunPCA()) {
      String pcaText =
        ((weka.distributed.spark.CorrelationMatrixSparkJob) m_runningJob)
          .getText();

      if (!DistributedJobConfig.isEmpty(pcaText)) {
        Data outText = new Data(StepManager.CON_TEXT, pcaText);
        outText.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
          "Spark - PCA analysis");
        getStepManager().outputData(outText);
      }
    }

    Image heatmap =
      ((weka.distributed.spark.CorrelationMatrixSparkJob) m_runningJob)
        .getImage();
    if (heatmap != null) {
      Data outImage = new Data(StepManager.CON_IMAGE, heatmap);
      outImage.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
        "Spark - correlation heat map");
      getStepManager().outputData(outImage);
    }
  }
}
