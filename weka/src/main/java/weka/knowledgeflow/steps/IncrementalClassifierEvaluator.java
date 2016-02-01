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
 *    IncrementalClassifierEvaluator.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.classifiers.evaluation.Evaluation;
import weka.core.Instance;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Step that evaluates incremental classifiers and produces strip chart data
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "IncrementalClassifierEvaluator",
  category = "Evaluation",
  toolTipText = "Evaluate the performance of incrementally training classifiers",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "IncrementalClassifierEvaluator.gif")
public class IncrementalClassifierEvaluator extends BaseStep {

  private static final long serialVersionUID = -5951569492213633100L;

  /** Legend information */
  protected List<String> m_dataLegend;

  /** Actual data point values */
  protected double[] m_dataPoint;

  /** Re-usable chart data */
  protected Data m_chartData = new Data(StepManager.CON_CHART);

  protected double m_min = Double.MAX_VALUE;
  protected double m_max = Double.MIN_VALUE;

  /** how often (in milliseconds) to report throughput to the log */
  protected int m_statusFrequency = 2000;

  /** Count of instances seen */
  protected int m_instanceCount;

  // output info retrieval and auc stats for each class (if class is nominal)
  protected boolean m_outputInfoRetrievalStats;

  /** Main eval object */
  protected Evaluation m_eval;

  /**
   * window size for computing performance metrics - 0 means no window, i.e
   * don't "forget" performance on any instances
   */
  protected int m_windowSize;

  /** Evaluation object for window */
  protected Evaluation m_windowEval;

  /** Window instances */
  protected LinkedList<Instance> m_window;

  /** Window predictions */
  protected LinkedList<double[]> m_windowedPreds;

  /** True if rest */
  protected boolean m_reset;

  /** Holds the name of the classifier being used */
  protected String m_classifierName;

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  @Override
  public void stepInit() throws WekaException {
    m_instanceCount = 0;
    m_dataPoint = new double[1];
    m_dataLegend = new ArrayList<String>();
    if (m_windowSize > 0) {
      m_window = new LinkedList<Instance>();
      m_windowedPreds = new LinkedList<double[]>();
      getStepManager().logBasic(
        "Chart output using windowed " + "evaluation over " + m_windowSize
          + " instances");
    }
    m_reset = true;
  }

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
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_INCREMENTAL_CLASSIFIER);
    }

    return new ArrayList<String>();
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
    if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_INCREMENTAL_CLASSIFIER) > 0) {
      result.add(StepManager.CON_TEXT);
      result.add(StepManager.CON_CHART);
    }

    return result;
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the payload to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    if (isStopRequested()) {
      return;
    }

    if (getStepManager().isStreamFinished(data)) {
      // done
      // notify downstream steps of end of stream
      Data d = new Data(StepManager.CON_CHART);
      getStepManager().throughputFinished(d);

      // save memory if using windowed evaluation
      m_windowEval = null;
      m_window = null;
      m_windowedPreds = null;

      if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_TEXT) > 0) {
        try {
          String textTitle = m_classifierName;
          String results =
            "=== Performance information ===\n\n" + "Scheme:   " + textTitle
              + "\n" + "Relation: " + m_eval.getHeader().relationName()
              + "\n\n" + m_eval.toSummaryString();
          if (m_eval.getHeader().classIndex() >= 0
            && m_eval.getHeader().classAttribute().isNominal()
            && (m_outputInfoRetrievalStats)) {
            results += "\n" + m_eval.toClassDetailsString();
          }
          if (m_eval.getHeader().classIndex() >= 0
            && m_eval.getHeader().classAttribute().isNominal()) {
            results += "\n" + m_eval.toMatrixString();
          }
          textTitle = "Results: " + textTitle;
          Data textData = new Data(StepManager.CON_TEXT);
          textData.setPayloadElement(StepManager.CON_TEXT, results);
          textData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
            textTitle);
          getStepManager().outputData(textData);
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
      }

      return;
    }

    weka.classifiers.Classifier classifier =
      (weka.classifiers.Classifier) data
        .getPayloadElement(StepManager.CON_INCREMENTAL_CLASSIFIER);
    Instance instance =
      (Instance) data.getPayloadElement(StepManager.CON_AUX_DATA_TEST_INSTANCE);

    try {
      if (m_reset) {
        m_reset = false;
        m_classifierName = classifier.getClass().getName();
        m_classifierName =
          m_classifierName.substring(m_classifierName.lastIndexOf(".") + 1,
            m_classifierName.length());
        m_eval = new Evaluation(instance.dataset());
        m_eval.useNoPriors();
        if (m_windowSize > 0) {
          m_windowEval = new Evaluation(instance.dataset());
          m_windowEval.useNoPriors();
        }

        if (instance.classAttribute().isNominal()) {
          if (!instance.classIsMissing()) {
            m_dataPoint = new double[3];
            m_dataLegend.add("Accuracy");
            m_dataLegend.add("RMSE (prob)");
            m_dataLegend.add("Kappa");
          } else {
            m_dataPoint = new double[1];
            m_dataLegend.add("Confidence");
          }
        } else {
          m_dataPoint = new double[1];
          if (instance.classIsMissing()) {
            m_dataLegend.add("Prediction");
          } else {
            m_dataLegend.add("RMSE");
          }
        }
      }

      getStepManager().throughputUpdateStart();
      m_instanceCount++;
      double[] dist = classifier.distributionForInstance(instance);
      double pred = 0;
      if (!instance.classIsMissing()) {
        if (m_outputInfoRetrievalStats) {
          m_eval.evaluateModelOnceAndRecordPrediction(dist, instance);
        } else {
          m_eval.evaluateModelOnce(dist, instance);
        }

        if (m_windowSize > 0) {
          m_windowEval.evaluateModelOnce(dist, instance);
          m_window.addFirst(instance);
          m_windowedPreds.addFirst(dist);

          if (m_instanceCount > m_windowSize) {
            // forget the oldest prediction
            Instance oldest = m_window.removeLast();
            double[] oldDist = m_windowedPreds.removeLast();

            oldest.setWeight(-oldest.weight());
            m_windowEval.evaluateModelOnce(oldDist, oldest);
            oldest.setWeight(-oldest.weight());
          }
        }
      } else {
        pred = classifier.classifyInstance(instance);
      }
      if (instance.classIndex() >= 0) {
        // need to check that the class is not missing
        if (instance.classAttribute().isNominal()) {
          if (!instance.classIsMissing()) {
            if (m_windowSize > 0) {
              m_dataPoint[1] = m_windowEval.rootMeanSquaredError();
              m_dataPoint[2] = m_windowEval.kappa();
            } else {
              m_dataPoint[1] = m_eval.rootMeanSquaredError();
              m_dataPoint[2] = m_eval.kappa();
            }
          }
          double primaryMeasure = 0;
          if (!instance.classIsMissing()) {
            primaryMeasure =
              m_windowSize > 0 ? 1.0 - m_windowEval.errorRate() : 1.0 - m_eval
                .errorRate();
          } else {
            // record confidence as the primary measure
            // (another possibility would be entropy of
            // the distribution, or perhaps average
            // confidence)
            primaryMeasure = dist[Utils.maxIndex(dist)];
          }
          m_dataPoint[0] = primaryMeasure;
          m_chartData
            .setPayloadElement(StepManager.CON_AUX_DATA_CHART_MIN, 0.0);
          m_chartData
            .setPayloadElement(StepManager.CON_AUX_DATA_CHART_MAX, 1.0);
          m_chartData.setPayloadElement(StepManager.CON_AUX_DATA_CHART_LEGEND,
            m_dataLegend);
          m_chartData.setPayloadElement(
            StepManager.CON_AUX_DATA_CHART_DATA_POINT, m_dataPoint);
        } else {
          // numeric class
          double update;
          if (!instance.classIsMissing()) {
            update =
              m_windowSize > 0 ? m_windowEval.rootMeanSquaredError() : m_eval
                .rootMeanSquaredError();
          } else {
            update = pred;
          }
          m_dataPoint[0] = update;
          if (update > m_max) {
            m_max = update;
          }
          if (update < m_min) {
            m_min = update;
          }
          m_chartData.setPayloadElement(StepManager.CON_AUX_DATA_CHART_MIN,
            instance.classIsMissing() ? m_min : 0.0);
          m_chartData.setPayloadElement(StepManager.CON_AUX_DATA_CHART_MAX,
            m_max);
          m_chartData.setPayloadElement(StepManager.CON_AUX_DATA_CHART_LEGEND,
            m_dataLegend);
          m_chartData.setPayloadElement(
            StepManager.CON_AUX_DATA_CHART_DATA_POINT, m_dataPoint);
        }

        if (isStopRequested()) {
          return;
        }
        getStepManager().throughputUpdateEnd();
        getStepManager().outputData(m_chartData.getConnectionName(),
          m_chartData);
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Set how often progress is reported to the status bar.
   *
   * @param s report progress every s instances
   */
  public void setStatusFrequency(int s) {
    m_statusFrequency = s;
  }

  /**
   * Get how often progress is reported to the status bar.
   *
   * @return after how many instances, progress is reported to the status bar
   */
  public int getStatusFrequency() {
    return m_statusFrequency;
  }

  /**
   * Return a tip text string for this property
   *
   * @return a string for the tip text
   */
  public String statusFrequencyTipText() {
    return "How often to report progress to the status bar.";
  }

  /**
   * Set whether to output per-class information retrieval statistics (nominal
   * class only).
   *
   * @param i true if info retrieval stats are to be output
   */
  public void setOutputPerClassInfoRetrievalStats(boolean i) {
    m_outputInfoRetrievalStats = i;
  }

  /**
   * Get whether per-class information retrieval stats are to be output.
   *
   * @return true if info retrieval stats are to be output
   */
  public boolean getOutputPerClassInfoRetrievalStats() {
    return m_outputInfoRetrievalStats;
  }

  /**
   * Return a tip text string for this property
   *
   * @return a string for the tip text
   */
  public String outputPerClassInfoRetrievalStatsTipText() {
    return "Output per-class info retrieval stats. If set to true, predictions get "
      + "stored so that stats such as AUC can be computed. Note: this consumes some memory.";
  }

  /**
   * Set whether to compute evaluation for charting over a fixed sized window of
   * the most recent instances (rather than the whole stream).
   *
   * @param windowSize the size of the window to use for computing the
   *          evaluation metrics used for charting. Setting a value of zero or
   *          less specifies that no windowing is to be used.
   */
  public void setChartingEvalWindowSize(int windowSize) {
    m_windowSize = windowSize;
  }

  /**
   * Get whether to compute evaluation for charting over a fixed sized window of
   * the most recent instances (rather than the whole stream).
   *
   * @return the size of the window to use for computing the evaluation metrics
   *         used for charting. Setting a value of zero or less specifies that
   *         no windowing is to be used.
   */
  public int getChartingEvalWindowSize() {
    return m_windowSize;
  }

  /**
   * Return a tip text string for this property
   *
   * @return a string for the tip text
   */
  public String chartingEvalWindowSizeTipText() {
    return "For charting only, specify a sliding window size over which to compute "
      + "performance stats. <= 0 means eval on whole stream";
  }
}
