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
 *    WekaClassifierSparkJob
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
 * Knowledge Flow step for the Weka classifier Spark job
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "WekaClassifierSparkJob", category = "Spark",
  toolTipText = "Builds and an aggregated Weka classifier - produces a single "
    + "model of the same type for Aggregateable classifiers or a voted ensemble "
    + "of the base classifier if they are not Aggregateable.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "WekaClassifierSparkJob.gif")
public class WekaClassifierSparkJob extends AbstractSparkJob {

  private static final long serialVersionUID = -8688034941610875109L;

  /**
   * Constructor
   */
  public WekaClassifierSparkJob() {
    super();

    m_job = new weka.distributed.spark.WekaClassifierSparkJob();
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = super.getOutgoingConnectionTypes();
    if (getStepManager().numIncomingConnections() > 0) {
      result.add(StepManager.CON_BATCH_CLASSIFIER);
      result.add(StepManager.CON_TEXT);
    }

    return result;
  }

  @Override
  public void notifyJobOutputConnections() throws WekaException {
    weka.classifiers.Classifier finalClassifier =
      ((weka.distributed.spark.WekaClassifierSparkJob) m_runningJob)
        .getClassifier();
    Instances modelHeader =
      ((weka.distributed.spark.WekaClassifierSparkJob) m_runningJob)
        .getTrainingHeader();
    String classAtt =
      ((weka.distributed.spark.WekaClassifierSparkJob) m_runningJob)
        .getClassAttribute();
    try {
      weka.distributed.spark.WekaClassifierSparkJob.setClassIndex(classAtt,
        modelHeader, true);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    if (finalClassifier == null) {
      getStepManager().logWarning("No classifier produced!");
    }

    if (modelHeader == null) {
      getStepManager()
        .logWarning("No training header available for the model!");
    }

    if (finalClassifier != null) {
      if (getStepManager()
        .numOutgoingConnectionsOfType(StepManager.CON_TEXT) > 0) {
        String textual = finalClassifier.toString();

        String title = "Spark: ";
        String classifierSpec = finalClassifier.getClass().getName();
        if (finalClassifier instanceof OptionHandler) {
          classifierSpec += " "
            + Utils.joinOptions(((OptionHandler) finalClassifier).getOptions());
        }
        title += classifierSpec;

        Data textData = new Data(StepManager.CON_TEXT, textual);
        textData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE, title);
        getStepManager().outputData(textData);
      }

      if (modelHeader != null) {
        Data headerData = new Data(StepManager.CON_DATASET, modelHeader);
        headerData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
        headerData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
        getStepManager().outputData(headerData);
      }
    }
  }
}
