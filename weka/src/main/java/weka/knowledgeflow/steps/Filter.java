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
 *    Filter.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.WekaException;
import weka.filters.StreamableFilter;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

/**
 * Step that wraps a Weka filter. Handles dataSet, trainingSet, testSet and
 * instance connections.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "Filter", category = "Filters",
  toolTipText = "Weka filter wrapper", iconPath = "")
public class Filter extends WekaAlgorithmWrapper {
  private static final long serialVersionUID = 6857031910153224479L;

  /** Template filter */
  protected weka.filters.Filter m_filterTemplate;

  /** Used when processing streaming data */
  protected weka.filters.Filter m_streamingFilter;

  /** True if we've been reset */
  protected boolean m_isReset;

  /** True if we're streaming */
  protected boolean m_streaming;

  /** True if string attributes are present in streaming case */
  protected boolean m_stringAttsPresent;

  /** Map of filters that have processed the first batch */
  protected Map<Integer, weka.filters.Filter> m_filterMap =
    new HashMap<Integer, weka.filters.Filter>();

  /** Map of waiting test sets when batch filtering */
  protected Map<Integer, Instances> m_waitingTestData =
    new HashMap<Integer, Instances>();

  /** Data object to reuse when processing incrementally */
  protected Data m_incrementalData;

  /** Keeps track of the number of train/test batches processed */
  protected AtomicInteger m_setCount;

  @Override
  public Class getWrappedAlgorithmClass() {
    return weka.filters.Filter.class;
  }

  @Override
  public void setWrappedAlgorithm(Object algo) {
    super.setWrappedAlgorithm(algo);
    m_defaultIconPath = StepVisual.BASE_ICON_PATH + "DefaultFilter.gif";
  }

  @ProgrammaticProperty
  public void setFilter(weka.filters.Filter filter) {
    setWrappedAlgorithm(filter);
  }

  public weka.filters.Filter getFilter() {
    return (weka.filters.Filter) getWrappedAlgorithm();
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    int numDataset =
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_DATASET);
    int numTraining =
      getStepManager()
        .numIncomingConnectionsOfType(StepManager.CON_TRAININGSET);
    int numTesting =
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_TESTSET);
    int numInstance =
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_INSTANCE);

    if (numDataset == 0 && numTraining == 0 && numTesting == 0
      && getFilter() instanceof StreamableFilter) {
      result.add(StepManager.CON_INSTANCE);
    }

    if (numInstance == 0 && numDataset == 0 && numTraining == 0) {
      result.add(StepManager.CON_DATASET);
      result.add(StepManager.CON_TRAININGSET);
    }

    if (numInstance == 0 && numTesting == 0) {
      result.add(StepManager.CON_TESTSET);
    }

    return result;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    int numDataset =
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_DATASET);
    int numTraining =
      getStepManager()
        .numIncomingConnectionsOfType(StepManager.CON_TRAININGSET);
    int numTesting =
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_TESTSET);
    int numInstance =
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_INSTANCE);

    if (numInstance > 0) {
      result.add(StepManager.CON_INSTANCE);
    }

    if (numDataset > 0) {
      result.add(StepManager.CON_DATASET);
    }

    if (numTraining > 0) {
      result.add(StepManager.CON_TRAININGSET);
    }

    if (numTesting > 0) {
      result.add(StepManager.CON_TESTSET);
    }

    // info connection - downstream steps can get our wrapped filter
    // for information (configuration) purposes
    result.add(StepManager.CON_INFO);

    return result;
  }

  @Override
  public void stepInit() throws WekaException {
    if (!(getWrappedAlgorithm() instanceof weka.filters.Filter)) {
      throw new WekaException("Incorrect type of algorithm");
    }

    try {
      m_filterTemplate = weka.filters.Filter.makeCopy(getFilter());

      if (m_filterTemplate instanceof EnvironmentHandler) {
        ((EnvironmentHandler) m_filterTemplate).setEnvironment(getStepManager()
          .getExecutionEnvironment().getEnvironmentVariables());
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    m_incrementalData = new Data(StepManager.CON_INSTANCE);
    m_filterMap.clear();
    m_waitingTestData.clear();
    m_streaming = false;
    m_stringAttsPresent = false;
    m_isReset = true;
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    Integer setNum = data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM);
    Integer maxSetNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM);

    if (m_isReset) {
      m_isReset = false;
      m_setCount = new AtomicInteger(maxSetNum != null ? maxSetNum : 1);
      getStepManager().processing();
      if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
        Instances incomingStructure =
          ((Instance) data.getPayloadElement(StepManager.CON_INSTANCE))
            .dataset();
        m_streaming = true;
        getStepManager().logBasic("Initializing streaming filter");
        try {
          m_streamingFilter = weka.filters.Filter.makeCopy(m_filterTemplate);
          m_streamingFilter.setInputFormat(incomingStructure);
          m_stringAttsPresent =
            m_streamingFilter.getOutputFormat().checkForStringAttributes();
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
      }
    }

    // we are NOT necessarily guaranteed to get tran/test pairs in the order
    // of train followed by test (especially if they come from different
    // sources). Output from trainTestSplitMaker and XValMaker are guaranteed
    // to be in order though
    if (m_streaming) {
      if (getStepManager().isStreamFinished(data)) {
        checkPendingStreaming();

        m_incrementalData.clearPayload();
        getStepManager().throughputFinished(m_incrementalData);
      } else {
        processStreaming(data);
      }
    } else if (data.getConnectionName().equals(StepManager.CON_TRAININGSET)
      || data.getConnectionName().equals(StepManager.CON_DATASET)) {
      Instances d = data.getPrimaryPayload();
      processFirstBatch(d, data.getConnectionName(), setNum, maxSetNum);
    } else {
      // if there are just test set connections, then process them as first
      // batches. Otherwise, process them as subsequent batches
      Instances d = data.getPrimaryPayload();
      if (getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_TRAININGSET) == 0
        && getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_DATASET) == 0) {
        processFirstBatch(d, data.getConnectionName(), setNum, maxSetNum);
      } else {
        processSubsequentBatch(d, data.getConnectionName(), setNum, maxSetNum);
      }
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
    } else if (!m_streaming) {
      if (m_setCount.get() == 0) {
        getStepManager().finished();

        // save memory
        m_waitingTestData.clear();
        m_filterMap.clear();
      }
    }
  }

  protected void processFirstBatch(Instances batch, String conType,
    Integer setNum, Integer maxSetNum) throws WekaException {

    try {
      weka.filters.Filter filterToUse =
        weka.filters.Filter.makeCopy(m_filterTemplate);
      if (!isStopRequested()) {
        filterToUse.setInputFormat(batch);
        String message = "Filtering " + conType + " (" + batch.relationName();
        if (setNum != null && maxSetNum != null) {
          message += ", set " + setNum + " of " + maxSetNum;
        }
        message += ")";
        getStepManager().statusMessage(message);
        getStepManager().logBasic(message);
        processBatch(batch, conType, filterToUse, setNum, maxSetNum);

        if (setNum != null) {
          m_filterMap.put(setNum, filterToUse);
        } else {
          m_filterMap.put(-1, filterToUse);
        }

        Instances waitingTest = m_waitingTestData.get(setNum);
        if (waitingTest != null) {
          processSubsequentBatch(waitingTest, StepManager.CON_TESTSET, setNum,
            maxSetNum);
        } else if (getStepManager().numIncomingConnections() == 1) {
          m_setCount.decrementAndGet();
        }
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  protected synchronized void processSubsequentBatch(Instances batch,
    String conType, Integer setNum, Integer maxSetNum) throws WekaException {

    Integer sN = setNum != null ? setNum : -1;
    weka.filters.Filter filterToUse = m_filterMap.get(sN);
    if (filterToUse == null) {
      // we've received the test set first...
      m_waitingTestData.put(setNum, batch);
      return;
    }

    if (!isStopRequested()) {
      String message = "Filtering " + conType + " (" + batch.relationName();
      if (setNum != null && maxSetNum != null) {
        message += ", set " + setNum + " of " + maxSetNum;
      }
      message += ") - batch mode";
      getStepManager().statusMessage(message);
      getStepManager().logBasic(message);
      processBatch(batch, conType, filterToUse, setNum, maxSetNum);
    }

    m_setCount.decrementAndGet();
  }

  protected void processBatch(Instances batch, String conType,
    weka.filters.Filter filterToUse, Integer setNum, Integer maxSetNum)
    throws WekaException {
    try {
      Instances filtered = weka.filters.Filter.useFilter(batch, filterToUse);
      String title = conType + ": " + filtered.relationName();
      Data output = new Data(conType, filtered);
      if (setNum != null && maxSetNum != null) {
        output.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
        output.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
          maxSetNum);
        output.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE, title);
      }
      getStepManager().outputData(output);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  protected void processStreaming(Data data) throws WekaException {
    Instance toFilter = data.getPrimaryPayload();
    getStepManager().throughputUpdateStart();
    try {
      if (m_streamingFilter.input(toFilter)) {
        Instance filteredI = m_streamingFilter.output();
        if (m_stringAttsPresent) {
          for (int i = 0; i < filteredI.numAttributes(); i++) {
            if (filteredI.dataset().attribute(i).isString()
              && !filteredI.isMissing(i)) {
              String val = filteredI.stringValue(i);
              filteredI.dataset().attribute(i).setStringValue(val);
              filteredI.setValue(i, 0);
            }
          }
        }
        m_incrementalData
          .setPayloadElement(StepManager.CON_INSTANCE, filteredI);
        if (!isStopRequested()) {
          getStepManager().outputData(m_incrementalData);
        }
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
    getStepManager().throughputUpdateEnd();
  }

  protected void checkPendingStreaming() throws WekaException {
    try {
      m_streamingFilter.batchFinished();
      Instances structureCopy =
        m_streamingFilter.getOutputFormat().stringFreeStructure();
      while (m_streamingFilter.numPendingOutput() > 0) {
        getStepManager().throughputUpdateStart();
        Instance filteredI = m_streamingFilter.output();
        if (m_stringAttsPresent) {
          for (int i = 0; i < filteredI.numAttributes(); i++) {
            String val = filteredI.stringValue(i);
            structureCopy.attribute(i).setStringValue(val);
            filteredI.setValue(i, 0);
          }
          filteredI.setDataset(structureCopy);
        }
        m_incrementalData
          .setPayloadElement(StepManager.CON_INSTANCE, filteredI);
        if (!isStopRequested()) {
          getStepManager().outputData(m_incrementalData);
        }
        getStepManager().throughputUpdateEnd();
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    Instances incomingStructure = null;
    String incomingConType = null;
    if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_TRAININGSET) > 0) {
      incomingConType = StepManager.CON_TRAININGSET;
    } else if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_TESTSET) > 0) {
      incomingConType = StepManager.CON_TESTSET;
    } else if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_DATASET) > 0) {
      incomingConType = StepManager.CON_DATASET;
    } else if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_INSTANCE) > 0) {
      incomingConType = StepManager.CON_INSTANCE;
    }

    if (incomingConType != null) {
      incomingStructure =
        getStepManager().getIncomingStructureForConnectionType(incomingConType);
    }

    if (incomingStructure != null) {
      try {
        weka.filters.Filter tempFilter =
          weka.filters.Filter.makeCopy(m_filterTemplate);
        if (tempFilter.setInputFormat(incomingStructure)) {
          return tempFilter.getOutputFormat();
        }
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }

    return null;
  }
}
