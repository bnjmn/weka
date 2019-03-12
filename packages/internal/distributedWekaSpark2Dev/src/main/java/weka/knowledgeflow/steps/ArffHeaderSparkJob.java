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
 *    ArffHeaderSparkStep
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Knowledge Flow step for the ArffHeader Spark Job
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "ArffHeaderSparkJob", category = "Spark",
  toolTipText = "Makes a unified ARFF header for a dataset ",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "ARFFHeaderSparkJob.gif")
public class ArffHeaderSparkJob extends AbstractSparkJob {

  private static final long serialVersionUID = 4091267140368960764L;

  public ArffHeaderSparkJob() {
    super();

    m_job = new weka.distributed.spark.ArffHeaderSparkJob();
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnections() == 0) {
      result.add(CON_SUCCESS);
      result.add(CON_FAILURE);
      result.add(AbstractDataSource.CON_DATA);
    }
    return result;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = super.getOutgoingConnectionTypes();
    result.add(StepManager.CON_DATASET);
    result.add(StepManager.CON_IMAGE);
    result.add(StepManager.CON_TEXT);

    return result;
  }

  @Override
  public void notifyJobOutputConnections() throws WekaException {
    Instances finalHeader =
      ((weka.distributed.spark.ArffHeaderSparkJob) m_runningJob).getHeader();
    String summaryStats =
      ((weka.distributed.spark.ArffHeaderSparkJob) m_runningJob).getText();
    if (finalHeader != null) {
      Data outputData = new Data(StepManager.CON_DATASET, finalHeader);
      outputData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
      outputData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
      getStepManager().outputData(outputData);
    }

    if (summaryStats != null) {
      Data outputData = new Data(StepManager.CON_TEXT, summaryStats);
      outputData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
        "summary stats");
      getStepManager().outputData(outputData);
    }

    List<BufferedImage> charts =
      ((weka.distributed.spark.ArffHeaderSparkJob) m_runningJob)
        .getSummaryCharts();
    List<String> attNames =
      ((weka.distributed.spark.ArffHeaderSparkJob) m_runningJob)
        .getSummaryChartAttNames();

    if (charts != null && charts.size() > 0) {
      for (int i = 0; i < charts.size(); i++) {
        BufferedImage image = charts.get(i);
        Data imageData = new Data(StepManager.CON_IMAGE, image);
        imageData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
          attNames.get(i));
        getStepManager().outputData(imageData);
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          // ignore
        }
      }
    }
  }
}
