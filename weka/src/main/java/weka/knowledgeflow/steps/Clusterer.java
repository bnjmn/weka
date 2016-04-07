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

import weka.core.Drawable;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.FilePropertyMetadata;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Step that wraps a Weka clusterer. Handles trainingSet and testSet incoming
 * connections
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "Clusterer", category = "Clusterers",
  toolTipText = "Weka clusterer wrapper", iconPath = "",
  resourceIntensive = true)
public class Clusterer extends WekaAlgorithmWrapper implements
  PairedDataHelper.PairedProcessor<weka.clusterers.Clusterer> {

  private static final long serialVersionUID = 3275754421525338036L;
  /**
   * The template for constructing concrete instances of the clusterer to train
   */
  protected weka.clusterers.Clusterer m_clustererTemplate;

  /** Holds a trained/loaded clusterer */
  protected weka.clusterers.Clusterer m_trainedClusterer;

  /** Header used to train the clusterer */
  protected Instances m_trainedClustererHeader;

  /** Handles train test pair routing and synchronization for us */
  protected transient PairedDataHelper<weka.clusterers.Clusterer> m_trainTestHelper;

  /**
   * Optional file to load a pre-trained model to score with (batch, or to score
   * and update (incremental) in the case of testSet only (batch) or instance
   * (incremental) connections
   */
  protected File m_loadModelFileName = new File("");

  /** True if the step has just been reset */
  protected boolean m_isReset;

  /** Re-usable Data object for incrementalClusterer output */
  protected Data m_incrementalData;

  /** True if there is a single incoming "instance" connection */
  protected boolean m_streaming;

  /**
   * Get the clusterer to train
   *
   * @return the clusterer to train
   */
  public weka.clusterers.Clusterer getClusterer() {
    return (weka.clusterers.Clusterer) getWrappedAlgorithm();
  }

  /**
   * Set the clusterer to train
   *
   * @param clusterer the clusterer to train
   */
  @ProgrammaticProperty
  public void setClusterer(weka.clusterers.Clusterer clusterer) {
    setWrappedAlgorithm(clusterer);
  }

  /**
   * Get the name of the clusterer to load at execution time. This only applies
   * in the case where the only incoming connection is a test set connection
   * (batch mode) or an instance connection (incremental prediction mode).
   *
   * @return the name of the file to load the model from
   */
  public File getLoadClustererFileName() {
    return m_loadModelFileName;
  }

  /**
   * Set the name of the clusterer to load at execution time. This only applies
   * in the case where the only incoming connection is a test set connection
   * (batch mode) or an instance connection (incremental prediction mode).
   *
   * @param filename the name of the file to load the model from
   */
  @OptionMetadata(
    displayName = "Clusterer model to load",
    description = "Optional "
      + "path to a clusterer to load at execution time (only applies when using "
      + "testSet connections)")
  @FilePropertyMetadata(fileChooserDialogType = KFGUIConsts.OPEN_DIALOG,
    directoriesOnly = false)
  public void setLoadClustererFileName(File filename) {
    m_loadModelFileName = filename;
  }

  /**
   * Get the class of the wrapped algorithm
   *
   * @return the class of the wrapped algorithm
   */
  @Override
  public Class getWrappedAlgorithmClass() {
    return weka.clusterers.Clusterer.class;
  }

  /**
   * Set the wrapped algorithm
   *
   * @param algo the algorithm to wrap
   */
  @Override
  public void setWrappedAlgorithm(Object algo) {
    super.setWrappedAlgorithm(algo);
    m_defaultIconPath = StepVisual.BASE_ICON_PATH + "DefaultClusterer.gif";
  }

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    if (!(getWrappedAlgorithm() instanceof weka.clusterers.Clusterer)) {
      throw new WekaException("Incorrect type of algorithm");
    }

    try {
      m_clustererTemplate =
        weka.clusterers.AbstractClusterer
          .makeCopy((weka.clusterers.Clusterer) getWrappedAlgorithm());

      if (m_clustererTemplate instanceof EnvironmentHandler) {
        ((EnvironmentHandler) m_clustererTemplate)
          .setEnvironment(getStepManager().getExecutionEnvironment()
            .getEnvironmentVariables());
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    // create and initialize our train/test pair helper if necessary
    if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_TRAININGSET) > 0) {
      m_trainTestHelper =
        new PairedDataHelper<weka.clusterers.Clusterer>(
          this,
          this,
          StepManager.CON_TRAININGSET,
          getStepManager()
            .numIncomingConnectionsOfType(StepManager.CON_TESTSET) > 0 ? StepManager.CON_TESTSET
            : null);
    }

    m_isReset = true;
    m_streaming = false;
    m_incrementalData = new Data(StepManager.CON_INCREMENTAL_CLUSTERER);

    if (getLoadClustererFileName() != null
      && getLoadClustererFileName().toString().length() > 0
      && getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_TRAININGSET) == 0) {
      String resolvedFileName =
        getStepManager().environmentSubstitute(
          getLoadClustererFileName().toString());
      try {
        loadModel(resolvedFileName);
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }
  }

  /**
   * Process an incoming data object
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    try {
      if (m_isReset) {
        m_isReset = false;
        getStepManager().processing();
        Instances incomingStructure = null;
        if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
          incomingStructure =
            ((Instance) data.getPayloadElement(StepManager.CON_INSTANCE))
              .dataset();
        } else {
          incomingStructure =
            (Instances) data.getPayloadElement(data.getConnectionName());
        }

        if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
          m_streaming = true;
          if (m_trainedClusterer == null) {
            m_trainedClusterer =
              weka.clusterers.AbstractClusterer.makeCopy(m_clustererTemplate);
            if (m_trainedClusterer instanceof EnvironmentHandler) {
              ((EnvironmentHandler) m_trainedClusterer)
                .setEnvironment(getStepManager().getExecutionEnvironment()
                  .getEnvironmentVariables());
            }
            // TODO - support incremental training at some point?
          }
        } else if (data.getConnectionName().equals(StepManager.CON_TRAININGSET)) {
          m_trainedClustererHeader = incomingStructure;
        }

        if (m_trainedClustererHeader != null
          && !incomingStructure.equalHeaders(m_trainedClustererHeader)) {
          throw new WekaException("Structure of incoming data does not match "
            + "that of the trained clusterer");
        }
      }

      if (m_streaming) {
        // TODO processStreaming()
      } else if (m_trainTestHelper != null) {
        // train test pairs
        m_trainTestHelper.process(data);
      } else {
        processOnlyTestSet(data);
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Process a Data object in the case where we only have an incoming testSet
   * connection
   *
   * @param data the Data object to process
   * @throws WekaException if a problem occurs
   */
  protected void processOnlyTestSet(Data data) throws WekaException {
    try {
      weka.clusterers.Clusterer tempToTest =
        weka.clusterers.AbstractClusterer.makeCopy(m_trainedClusterer);
      Data batchClusterer =
        new Data(StepManager.CON_BATCH_CLUSTERER, tempToTest);
      batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_TESTSET,
        data.getPayloadElement(StepManager.CON_AUX_DATA_TESTSET));
      batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM,
        data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1));
      batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
        data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1));
      getStepManager().outputData(batchClusterer);
      if (isStopRequested()) {
        getStepManager().interrupted();
      } else {
        getStepManager().finished();
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Get a list of connection types that could be made to this Step at this
   * point in time
   *
   * @return a list of incoming connection types that could be made at this time
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    int numTraining =
      getStepManager()
        .numIncomingConnectionsOfType(StepManager.CON_TRAININGSET);
    int numTesting =
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_TESTSET);

    int numInstance =
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_INSTANCE);

    if (numTraining == 0) {
      result.add(StepManager.CON_TRAININGSET);
    }

    if (numTesting == 0) {
      result.add(StepManager.CON_TESTSET);
    }

    // streaming prediction only
    if (numTraining == 0 && numTesting == 0) {
      result.add(StepManager.CON_INSTANCE);
    }

    return result;
  }

  /**
   * Get a list of outgoing connections that could be made from this step at
   * this point in time
   *
   * @return a list of outgoing connections that could be made at this point in
   *         time
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    int numTraining =
      getStepManager()
        .numIncomingConnectionsOfType(StepManager.CON_TRAININGSET);
    int numTesting =
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_TESTSET);

    List<String> result = new ArrayList<String>();
    if (numTraining > 0 || numTesting > 0) {
      result.add(StepManager.CON_BATCH_CLUSTERER);
    }

    result.add(StepManager.CON_TEXT);

    if (getClusterer() instanceof Drawable && numTraining > 0) {
      result.add(StepManager.CON_GRAPH);
    }

    // info connection - downstream steps can get our wrapped clusterer
    // for information (configuration) purposes
    result.add(StepManager.CON_INFO);

    return result;
  }

  /**
   * Load a pre-trained model from the supplied path
   *
   * @param filePath the path to load the model from
   * @throws Exception if a problem occurs
   */
  protected void loadModel(String filePath) throws Exception {
    ObjectInputStream is = null;
    try {
      is =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(
          new File(filePath))));

      m_trainedClusterer = (weka.clusterers.Clusterer) is.readObject();

      // try and read the header
      try {
        m_trainedClustererHeader = (Instances) is.readObject();
      } catch (Exception ex) {
        getStepManager().logWarning(
          "Model file '" + filePath
            + "' does not seem to contain an Instances header");
      }
    } finally {
      if (is != null) {
        is.close();
      }
    }
  }

  /**
   * Output a Data object containing a dot graph, if the model is Drawable and
   * we have downstream steps receiving graph connections.
   *
   * @param clusterer the clusterer to generate the graph from
   * @param setNum the set number of the data used to generate the graph
   * @throws WekaException if a problem occurs
   */
  protected void
    outputGraphData(weka.clusterers.Clusterer clusterer, int setNum)
      throws WekaException {
    if (clusterer instanceof Drawable) {
      if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_GRAPH) == 0) {
        return;
      }

      try {
        String graphString = ((Drawable) clusterer).graph();
        int graphType = ((Drawable) clusterer).graphType();
        String grphTitle = clusterer.getClass().getCanonicalName();
        grphTitle =
          grphTitle.substring(grphTitle.lastIndexOf('.') + 1,
            grphTitle.length());
        grphTitle =
          "Set " + setNum + " (" + m_trainedClustererHeader.relationName()
            + ") " + grphTitle;
        Data graphData = new Data(StepManager.CON_GRAPH);
        graphData.setPayloadElement(StepManager.CON_GRAPH, graphString);
        graphData.setPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TITLE,
          grphTitle);
        graphData.setPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TYPE,
          graphType);
        getStepManager().outputData(graphData);
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }
  }

  /**
   * Output a Data object containing a textual description of a model to any
   * outgoing text connections
   *
   * @param clusterer the clusterer to get the textual description of
   * @param setNum the set number of the training data
   * @throws WekaException if a problem occurs
   */
  protected void
    outputTextData(weka.clusterers.Clusterer clusterer, int setNum)
      throws WekaException {

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_TEXT) == 0) {
      return;
    }

    Data textData = new Data(StepManager.CON_TEXT);

    String modelString = clusterer.toString();
    String titleString = clusterer.getClass().getName();

    titleString =
      titleString.substring(titleString.lastIndexOf('.') + 1,
        titleString.length());
    modelString =
      "=== Clusterer model ===\n\n" + "Scheme:   " + titleString + "\n"
        + "Relation: " + m_trainedClustererHeader.relationName() + "\n\n"
        + modelString;
    titleString = "Model: " + titleString;

    textData.setPayloadElement(StepManager.CON_TEXT, modelString);
    textData
      .setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE, titleString);

    if (setNum != -1) {
      textData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
    }

    getStepManager().outputData(textData);
  }

  /**
   * Output batch clusterer data to downstream steps
   *
   * @param clusterer the clusterer to outpit
   * @param setNum the set number of the current dataset
   * @param maxSetNum the maximum set number
   * @param trainingSplit the training data
   * @param testSplit the test data, or null if there is no test data
   * @throws WekaException if a problem occurs
   */
  protected void outputBatchClusterer(weka.clusterers.Clusterer clusterer,
    int setNum, int maxSetNum, Instances trainingSplit, Instances testSplit)
    throws WekaException {
    Data batchClusterer = new Data(StepManager.CON_BATCH_CLUSTERER, clusterer);
    batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_TRAININGSET,
      trainingSplit);
    if (testSplit != null) {
      batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_TESTSET,
        testSplit);
    }
    batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
    batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
      maxSetNum);
    batchClusterer.setPayloadElement(StepManager.CON_AUX_DATA_LABEL, getName());
    getStepManager().outputData(batchClusterer);
  }

  /**
   * Process a training split (primary data handled by the PairedDataHelper)
   *
   * @param setNum the number of this split/fold
   * @param maxSetNum the maximum number of splits/folds in the group
   * @param data the actual split/fold data
   * @param helper the PairedDataHelper managing the paired data
   * @return a Classifier trained on this training split
   * @throws WekaException if a problem occurs
   */
  @Override
  public weka.clusterers.Clusterer processPrimary(Integer setNum,
    Integer maxSetNum, Data data,
    PairedDataHelper<weka.clusterers.Clusterer> helper) throws WekaException {
    Instances trainingData = data.getPrimaryPayload();
    try {
      weka.clusterers.Clusterer clusterer =
        weka.clusterers.AbstractClusterer.makeCopy(m_clustererTemplate);

      String clustererDesc = clusterer.getClass().getCanonicalName();
      clustererDesc =
        clustererDesc.substring(clustererDesc.lastIndexOf('.') + 1);
      if (clusterer instanceof OptionHandler) {
        String optsString =
          Utils.joinOptions(((OptionHandler) clusterer).getOptions());
        clustererDesc += " " + optsString;
      }

      if (clusterer instanceof EnvironmentHandler) {
        ((EnvironmentHandler) clusterer).setEnvironment(getStepManager()
          .getExecutionEnvironment().getEnvironmentVariables());
      }

      // retain the training data
      helper
        .addIndexedValueToNamedStore("trainingSplits", setNum, trainingData);

      if (!isStopRequested()) {
        getStepManager().logBasic(
          "Building " + clustererDesc + " on " + trainingData.relationName()
            + " for fold/set " + setNum + " out of " + maxSetNum);

        if (maxSetNum == 1) {
          // single train/test split - makes sense to retain this trained
          // classifier
          m_trainedClusterer = clusterer;
        }

        clusterer.buildClusterer(trainingData);

        getStepManager().logDetailed(
          "Finished building " + clustererDesc + "on "
            + trainingData.relationName() + " for fold/set " + setNum
            + " out of " + maxSetNum);

        outputTextData(clusterer, setNum);
        outputGraphData(clusterer, setNum);

        if (getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_TESTSET) == 0) {
          // output a butch clusterer for just the trained model
          outputBatchClusterer(clusterer, setNum, maxSetNum, trainingData, null);
        }
      }
      return clusterer;
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Process a test split/fold (secondary data handled by PairedDataHelper)
   *
   * @param setNum the set number of this split/fold
   * @param maxSetNum the maximum number of splits/folds in the group
   * @param data the actual split/fold data
   * @param helper the PairedDataHelper managing the paried data
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processSecondary(Integer setNum, Integer maxSetNum, Data data,
    PairedDataHelper<weka.clusterers.Clusterer> helper) throws WekaException {

    // trained clusterer for this set number
    weka.clusterers.Clusterer clusterer =
      helper.getIndexedPrimaryResult(setNum);

    // test data
    Instances testSplit = data.getPrimaryPayload();

    // paired training data
    Instances trainingSplit =
      helper.getIndexedValueFromNamedStore("trainingSplits", setNum);

    getStepManager().logBasic(
      "Dispatching model for set " + setNum + " out of " + maxSetNum
        + " to output");

    outputBatchClusterer(clusterer, setNum, maxSetNum, trainingSplit, testSplit);
  }
}
