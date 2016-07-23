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
 *    Clusterer.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.clusterers.ClusterEvaluation;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A step that evaluates the performance of batch trained clusterers
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "ClustererPerformanceEvaluator", category = "Evaluation",
  toolTipText = "Evaluates batch clusterers",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "ClustererPerformanceEvaluator.gif")
public class ClustererPerformanceEvaluator extends BaseStep {

  private static final long serialVersionUID = -6337375482954345717L;

  /**
   * Get a list of incoming connection types that this step can accept. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and any existing incoming connections. E.g. a step might be able to accept
   * one (and only one) incoming batch data connection.
   *
   * @return a list of incoming connections that this step can accept given its
   *         current state
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    if (getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_BATCH_CLUSTERER) == 0) {
      result.add(StepManager.CON_BATCH_CLUSTERER);
    }
    return result;
  }

  /**
   * Get a list of outgoing connection types that this step can produce. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and the incoming connections. E.g. depending on what incoming connection is
   * present, a step might be able to produce a trainingSet output, a testSet
   * output or neither, but not both.
   *
   * @return a list of outgoing connections that this step can produce
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnections() > 0) {
      result.add(StepManager.CON_TEXT);
      // result.add(StepManager.CON_VISUALIZABLE_ERROR);
    }

    return result;
  }

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  @Override
  public void stepInit() {
    // nothing to do
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the payload to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    weka.clusterers.Clusterer clusterer = (weka.clusterers.Clusterer) data
      .getPayloadElement(StepManager.CON_BATCH_CLUSTERER);
    Instances trainData =
      (Instances) data.getPayloadElement(StepManager.CON_AUX_DATA_TRAININGSET);
    Instances testData =
      (Instances) data.getPayloadElement(StepManager.CON_AUX_DATA_TESTSET);
    Integer setNum =
      (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM);
    Integer maxSetNum =
      (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM);

    if (setNum == 1) {
      getStepManager().processing();
    }

    ClusterEvaluation eval = new ClusterEvaluation();
    eval.setClusterer(clusterer);
    // cluster evaluation is no cumulative across sets, so each
    // set is a separate evaluation

    String clusterSpec = makeClustererSpec(clusterer);
    String clusterClass = clusterer.getClass().getCanonicalName();
    clusterClass = clusterClass.substring(clusterClass.lastIndexOf('.') + 1,
      clusterClass.length());
    if (trainData != null && !isStopRequested()) {
      getStepManager().statusMessage("Evaluating (training set " + setNum
        + " of " + maxSetNum + ") " + clusterSpec);

      try {
        eval.evaluateClusterer(trainData);
      } catch (Exception ex) {
        throw new WekaException(ex);
      }

      if (!isStopRequested()) {
        String resultT = "=== Evaluation result for training instances ===\n\n"
          + "Scheme: " + clusterSpec + "\n" + "Relation: "
          + trainData.relationName() + "\n\n" + eval.clusterResultsToString();

        if (trainData.classIndex() >= 0
          && trainData.classAttribute().isNumeric()) {
          resultT +=
            "\n\nNo class-based evaluation possible. Class attribute has to be "
              + "nominal.";
        }
        Data text = new Data(StepManager.CON_TEXT, resultT);
        text.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
          clusterClass + " train (" + setNum + " of " + maxSetNum + ")");
        getStepManager().outputData(text);
      }
    }

    if (testData != null && !isStopRequested()) {
      getStepManager().statusMessage("Evaluating (test set " + setNum + " of "
        + maxSetNum + ") " + clusterSpec);

      eval = new ClusterEvaluation();
      eval.setClusterer(clusterer);
      try {
        eval.evaluateClusterer(testData);
      } catch (Exception ex) {
        throw new WekaException(ex);
      }

      if (!isStopRequested()) {
        String resultT = "=== Evaluation result for test instances ===\n\n"
          + "Scheme: " + clusterSpec + "\n" + "Relation: "
          + testData.relationName() + "\n\n" + eval.clusterResultsToString();
        if (testData.classIndex() >= 0
          && testData.classAttribute().isNumeric()) {
          resultT +=
            "\n\nNo class-based evaluation possible. Class attribute has to be "
              + "nominal.";
        }
        Data text = new Data(StepManager.CON_TEXT, resultT);
        text.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
          clusterClass + " test (" + setNum + " of " + maxSetNum + ")");
        getStepManager().outputData(text);
      }
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
    } else if (setNum.intValue() == maxSetNum.intValue()) {
      getStepManager().finished();
    }
  }

  protected String makeClustererSpec(weka.clusterers.Clusterer clusterer) {
    String clusterSpec = clusterer.getClass().getCanonicalName();
    clusterSpec = clusterSpec.substring(clusterSpec.lastIndexOf('.') + 1,
      clusterSpec.length());

    String opts = " ";
    if (clusterer instanceof OptionHandler) {
      opts = Utils.joinOptions(((OptionHandler) clusterer).getOptions());
    }

    return clusterSpec + opts;
  }
}
