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
 *    CalibrationCurveGenerator.java
 *    Copyright (C) 2018 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.gui.visualize.plugins.CalibrationCurveUtils;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Knowledge Flow step that produces a set of instances containing calibration
 * curve data.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
@KFStep(name = "CalibrationCurveGenerator", category = "Evaluation",
  toolTipText = "Generates plottable calibration curve data",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "CalibrationCurveGenerator.gif")
public class CalibrationCurveGenerator extends BaseStep {

  private static final long serialVersionUID = 375178533516458499L;

  /** True to pool incoming predictions for multiple sets/folds of data */
  protected boolean m_poolSets;

  /** True if the step has been reset */
  protected boolean m_isReset;

  /** Holds the pooled data (if pooling) */
  protected Instances m_pooledData;

  /**
   * Set whether to pool the incoming predictions for separate sets/folds of
   * data when generating the calibration curve
   *
   * @param poolSets true to pool separate sets/folds of data
   */
  @OptionMetadata(
    displayName = "Pool incoming sets/folds",
    description = "Whether to pool incoming sets/folds of predicted data in "
      + "order to generate a single calibration curve (as opposed to one curve "
      + "per set/fold)", displayOrder = 1)
  public
    void setPoolSets(boolean poolSets) {
    m_poolSets = poolSets;
  }

  /**
   * Get whether to pool the incoming predictions for separate sets/folds of
   * data when generating the calibration curve
   *
   * @return true to pool separate sets/folds of data
   */
  public boolean getPoolSets() {
    return m_poolSets;
  }

  @Override
  public void stepInit() throws WekaException {
    Map<String, List<StepManager>> incoming =
      getStepManager().getIncomingConnections();

    boolean ok = false;
    PredictionAppender pa = null;
    for (String key : incoming.keySet()) {
      // there will only be one key (as only one connection is allowed)
      List<StepManager> steps = incoming.get(key);
      for (StepManager sm : steps) {
        if (sm.getManagedStep() instanceof PredictionAppender) {
          ok = true;
          pa = (PredictionAppender) sm.getManagedStep();
          break;
        }
      }
    }

    if (!ok) {
      throw new WekaException(
        "Incoming data must be from a PredictionAppender " + "step!");
    }

    // now check that output probabilities has been turned on in
    // PredictionAppender
    if (!pa.getAppendProbabilities()) {
      throw new WekaException(
        "The connected PredictionAppender must be configured "
          + "to append probability estimates. Note that CalibrationCurves can only "
          + "be generated for data with a nominal class attribute.");
    }
    m_isReset = true;
    m_pooledData = null;
  }

  @Override
  public void processIncoming(Data data) throws WekaException {

    if (isStopRequested()) {
      getStepManager().interrupted();
      return;
    }

    getStepManager().processing();

    Instances predictedInsts = data.getPrimaryPayload();
    int maxSetNum =
      (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM);
    int setNum =
      (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM);

    if (maxSetNum > 1 && getPoolSets()) {
      if (m_isReset) {
        m_pooledData = new Instances(predictedInsts);
      } else {
        m_pooledData.addAll(predictedInsts);
      }
    }

    m_isReset = false;

    if (getPoolSets() && setNum < maxSetNum) {
      getStepManager().finished();
      return;
    }

    if (getPoolSets() && maxSetNum > 1) {
      predictedInsts = m_pooledData;
    }

    if (predictedInsts.classIndex() < 0) {
      throw new WekaException("No class set in the predicted data!");
    }

    int numAttributes = predictedInsts.numAttributes();
    int numClasses = predictedInsts.classAttribute().numValues();

    // we always produce a curve for the first label. The user can
    // always choose a label by using a ClassValuePicker step
    Attribute classAtt = predictedInsts.classAttribute();
    Attribute predictedLabelProbAtt =
      predictedInsts.attribute(numAttributes - numClasses);

    ArrayList<Prediction> preds = new ArrayList<>();
    for (int i = 0; i < predictedInsts.numInstances(); i++) {
      Instance current = predictedInsts.instance(i);
      double[] dist = new double[numClasses];
      dist[0] = current.value(predictedLabelProbAtt.index());
      double actual = current.classValue();

      preds.add(new NominalPrediction(actual, dist, current.weight()));
    }

    try {
      Instances curveInsts =
        CalibrationCurveUtils
          .getCalibrationCurveAsInstances(preds, classAtt, 0);
      curveInsts.setRelationName("__" + curveInsts.relationName());

      Instance zero = curveInsts.remove(curveInsts.numInstances() - 2);
      zero.setWeight(-1);
      curveInsts.add(0, zero);
      curveInsts.lastInstance().setWeight(-1);
      Data output = new Data(StepManager.CON_DATASET, curveInsts);
      output.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
      output.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
      getStepManager().outputData(output);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    getStepManager().finished();
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    if (getStepManager().numIncomingConnections() > 0) {
      return Arrays.asList(StepManager.CON_DATASET);
    }

    return null;
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET);
    }

    return new ArrayList<String>();
  }
}
