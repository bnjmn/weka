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
 *    CSVToArffHeaderHadoopJob
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.distributed.hadoop.ArffHeaderHadoopJob;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Knowledge Flow step for the CSVToARFFHadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "CSVToARFFHeaderHadoopJob", category = "Hadoop",
  toolTipText = "Makes a unified ARFF header for a dataset",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "ARFFHeaderHadoopJob.gif")
public class CSVToARFFHadoopJob extends AbstractHadoopJob {

  private static final long serialVersionUID = -8034610787488919825L;

  public CSVToARFFHadoopJob() {
    m_job = new ArffHeaderHadoopJob();
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
  protected void notifyJobOutputConnections() throws WekaException {
    Instances finalHeader =
      ((ArffHeaderHadoopJob) m_runningJob).getFinalHeader();
    String summaryStats = ((ArffHeaderHadoopJob)m_runningJob).getText();

    if (finalHeader != null) {
      Data outputData = new Data(StepManager.CON_DATASET, finalHeader);
      outputData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
      outputData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
      getStepManager().outputData(outputData);
    }

    if (summaryStats != null) {
      Data outputData = new Data(StepManager.CON_TEXT, summaryStats);
      outputData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE, "summary stats");
      getStepManager().outputData(outputData);
    }

    List<BufferedImage> charts =
      ((weka.distributed.hadoop.ArffHeaderHadoopJob) m_runningJob)
        .getSummaryCharts();
    if (charts != null && charts.size() > 0) {
      for (BufferedImage i : charts) {
        Data imageData = new Data(StepManager.CON_IMAGE, i);
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
