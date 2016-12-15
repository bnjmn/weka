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
 *    WriteToWekaLog.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.LoggingLevel;
import weka.knowledgeflow.StepManager;

/**
 * Step that takes incoming data and writes it to the Weka log
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "WriteToWekaLog", category = "Flow",
  toolTipText = "Write data to the log", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "WriteWekaLog.gif")
public class WriteWekaLog extends BaseStep {

  private static final long serialVersionUID = -2306717547200779711L;

  /** How often to write incremental data to the log */
  protected String m_incrementalWriteFrequency = "1000";

  /** Resolved frequency */
  protected int m_incrFreq = 1000;

  /** Count of how many incremental data points have been seen so far */
  protected int m_incrCount;

  /** True if the step has been reset */
  protected boolean m_isReset;

  /** True if the input is incremental */
  protected boolean m_inputIsIncremental;

  /** Level to log at */
  protected LoggingLevel m_logLevel = LoggingLevel.BASIC;

  /**
   * Set the logging level to use
   *
   * @param level the level to use
   */
  @OptionMetadata(displayName = "Logging level", description = "The level at "
    + "which to write log messages", displayOrder = 1)
  public void setLoggingLevel(LoggingLevel level) {
    m_logLevel = level;
  }

  /**
   * Get the logging level to use
   *
   * @return the level to use
   */
  public LoggingLevel getLoggingLevel() {
    return m_logLevel;
  }

  /**
   * Set how frequently to write an incremental data point to the log
   *
   * @param frequency the frequency (in data points) to write to the log
   */
  @OptionMetadata(displayName = "Incremental logging frequency",
    description = "How often to write an incremental/streaming data point "
      + "to the log", displayOrder = 2)
  public void setIncrementalLoggingFrequency(String frequency) {
    m_incrementalWriteFrequency = frequency;
  }

  /**
   * Get how frequently to write an incremental data point to the log
   *
   * @return the frequency (in data points) to write to the log
   */
  public String getIncrementalLoggingFrequency() {
    return m_incrementalWriteFrequency;
  }

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    m_isReset = true;
    m_incrCount = 0;
    m_inputIsIncremental = false;
    String resolvedFreq =
      getStepManager().environmentSubstitute(m_incrementalWriteFrequency);
    if (resolvedFreq.length() > 0) {
      try {
        m_incrFreq = Integer.parseInt(m_incrementalWriteFrequency);
      } catch (NumberFormatException ex) {
        getStepManager().logWarning(
          "Unable to parse incremental write frequency " + "setting "
            + resolvedFreq);
      }
    }
  }

  /**
   * Process an incoming piece of data
   *
   * @param data the payload to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    if (m_isReset) {
      m_isReset = false;
      m_inputIsIncremental =
        data.getPayloadElement(StepManager.CON_AUX_DATA_IS_INCREMENTAL, false);
    }

    if (m_inputIsIncremental) {
      processStreaming(data);

      if (isStopRequested()) {
        getStepManager().interrupted();
      }
    } else {
      getStepManager().processing();
      processBatch(data);
      if (isStopRequested()) {
        getStepManager().interrupted();
      } else {
        getStepManager().finished();
      }
    }
  }

  /**
   * Process a streaming data point
   *
   * @param data the data
   * @throws WekaException if a problem occurs
   */
  protected void processStreaming(Data data) throws WekaException {
    Object payload = data.getPrimaryPayload();
    if (m_incrCount % m_incrFreq == 0 && payload != null) {
      getStepManager().log(payload.toString(), m_logLevel);
    }
    m_incrCount++;
  }

  /**
   * Process a batch data point
   *
   * @param data the data
   * @throws WekaException if a problem occurs
   */
  protected void processBatch(Data data) throws WekaException {
    Object payload = data.getPrimaryPayload();
    if (payload != null) {
      getStepManager().log(payload.toString(), m_logLevel);
    }
  }

  /**
   * Get a list of acceptable incoming connection types (at this point in time)
   *
   * @return a list of legal incoming connection types to accept
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<>();
    if (getStepManager().numIncomingConnections() == 0) {
      result.add(StepManager.CON_INSTANCE);
    }

    if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_INSTANCE) == 0) {
      result.addAll(Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET,
        StepManager.CON_TEXT, StepManager.CON_BATCH_ASSOCIATOR,
        StepManager.CON_BATCH_CLASSIFIER, StepManager.CON_BATCH_CLUSTERER));
    }
    return result;
  }

  /**
   * Get currently generatable outgoing connection types
   *
   * @return a list of outgoing connection types
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    Map<String, List<StepManager>> incoming =
      getStepManager().getIncomingConnections();
    return new ArrayList<>(incoming.keySet());
  }
}
