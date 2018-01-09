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
 *    Classifier.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.UpdateableBatchProcessor;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.misc.InputMappedClassifier;
import weka.core.Drawable;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.SerializationHelper;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.FilePropertyMetadata;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.LoggingLevel;
import weka.knowledgeflow.SingleThreadedExecution;
import weka.knowledgeflow.StepManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Step that wraps a Weka classifier. Handles instance, trainingSet and testSet
 * incoming connections. If the base classifier is Updateable, then it can be
 * optionally updated incrementall on an incoming connection of type instance.
 * Otherwise, instance connections are used for testing a classifier
 * incrementally. In the case of a single incoming testSet connection it is
 * assumed that the classifier has already been trained.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "Classifier", category = "Classifiers",
  toolTipText = "Weka classifier wrapper", iconPath = "",
  resourceIntensive = true)
public class Classifier extends WekaAlgorithmWrapper implements
  PairedDataHelper.PairedProcessor<weka.classifiers.Classifier> {

  /** For serialization */
  private static final long serialVersionUID = 8326706942962123155L;

  /**
   * The template for constructing concrete instances of the classifier to train
   */
  protected weka.classifiers.Classifier m_classifierTemplate;

  /**
   * Holds the trained classifier in the case of single train/test pairs or
   * instance stream connections
   */
  protected weka.classifiers.Classifier m_trainedClassifier;
  protected Instances m_trainedClassifierHeader;

  /**
   * Optional file to load a pre-trained model to score with (batch, or to score
   * and update (incremental) in the case of testSet only (batch) or instance
   * (incremental) connections
   */
  protected File m_loadModelFileName = new File("");

  /**
   * True if we should resent an Updateable classifier at the start of
   * processing for an incoming "instance" stream
   */
  protected boolean m_resetIncrementalClassifier;

  /**
   * True if we should update an incremental classifier when there is a incoming
   * "instance" stream
   */
  protected boolean m_updateIncrementalClassifier = true;

  /** True if we are processing streaming data */
  protected boolean m_streaming;

  /** True if the classifier in use is Updateable */
  protected boolean m_classifierIsIncremental;

  /** Handles train test pair routing and synchronization for us */
  protected transient PairedDataHelper<weka.classifiers.Classifier> m_trainTestHelper;

  /** Reusable data for incremental streaming classifiers */
  protected Data m_incrementalData = new Data(
    StepManager.CON_INCREMENTAL_CLASSIFIER);

  /** True if we've been reset */
  protected boolean m_isReset;

  @Override
  public Class getWrappedAlgorithmClass() {
    return weka.classifiers.Classifier.class;
  }

  @Override
  public void setWrappedAlgorithm(Object algo) {
    super.setWrappedAlgorithm(algo);
    m_defaultIconPath = StepVisual.BASE_ICON_PATH + "DefaultClassifier.gif";
  }

  /**
   * Get the classifier to train
   *
   * @return the classifier to train
   */
  public weka.classifiers.Classifier getClassifier() {
    return (weka.classifiers.Classifier) getWrappedAlgorithm();
  }

  /**
   * Set the classifier to train
   *
   * @param classifier the classifier to train
   */
  @ProgrammaticProperty
  public void setClassifier(weka.classifiers.Classifier classifier) {
    setWrappedAlgorithm(classifier);
  }

  @Override
  public void stepInit() throws WekaException {
    try {
      m_trainedClassifier = null;
      m_trainedClassifierHeader = null;
      m_trainTestHelper = null;
      m_incrementalData = new Data(StepManager.CON_INCREMENTAL_CLASSIFIER);
      m_classifierTemplate =
        AbstractClassifier
          .makeCopy((weka.classifiers.Classifier) getWrappedAlgorithm());

      if (m_classifierTemplate instanceof EnvironmentHandler) {
        ((EnvironmentHandler) m_classifierTemplate)
          .setEnvironment(getStepManager().getExecutionEnvironment()
            .getEnvironmentVariables());
      }

      // Check to see if the classifier is one that must run single-threaded
      Annotation a =
        m_classifierTemplate.getClass().getAnnotation(
          SingleThreadedExecution.class);
      if (a != null) {
        getStepManager().logBasic(
          getClassifier().getClass().getCanonicalName() + " "
            + "will be executed in the single threaded executor");
        getStepManager().setStepMustRunSingleThreaded(true);
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    // create and initialize our train/test pair helper if necessary
    if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_TRAININGSET) > 0) {
      m_trainTestHelper =
        new PairedDataHelper<weka.classifiers.Classifier>(
          this,
          this,
          StepManager.CON_TRAININGSET,
          getStepManager()
            .numIncomingConnectionsOfType(StepManager.CON_TESTSET) > 0 ? StepManager.CON_TESTSET
            : null);
    }

    m_isReset = true;
    m_classifierIsIncremental =
      m_classifierTemplate instanceof UpdateableClassifier;

    if (getLoadClassifierFileName() != null
      && getLoadClassifierFileName().toString().length() > 0
      && getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_TRAININGSET) == 0) {
      String resolvedFileName =
        getStepManager().environmentSubstitute(
          getLoadClassifierFileName().toString());
      try {
        getStepManager().logBasic("Loading classifier: " + resolvedFileName);
        loadModel(resolvedFileName);
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }

    if (m_trainedClassifier != null
      && getStepManager()
        .numIncomingConnectionsOfType(StepManager.CON_INSTANCE) > 0
      && !m_classifierIsIncremental) {
      getStepManager().logWarning(
        "Loaded classifier is not an incremental one "
          + "- will only be able to evaluate, and not update, on the incoming "
          + "instance stream.");
    }
  }

  /**
   * Get the name of the classifier to load at execution time. This only applies
   * in the case where the only incoming connection is a test set connection
   * (batch mode) or an instance connection (incremental mode).
   *
   * @return the name of the file to load the model from
   */
  public File getLoadClassifierFileName() {
    return m_loadModelFileName;
  }

  /**
   * Set the name of the classifier to load at execution time. This only applies
   * in the case where the only incoming connection is a test set connection
   * (batch mode) or an instance connection (incremental mode).
   *
   * @param filename the name of the file to load the model from
   */
  @OptionMetadata(
    displayName = "Classifier model to load",
    description = "Optional "
      + "Path to a classifier to load at execution time (only applies when using "
      + "testSet or instance connections)")
  @FilePropertyMetadata(fileChooserDialogType = KFGUIConsts.OPEN_DIALOG,
    directoriesOnly = false)
  public void setLoadClassifierFileName(File filename) {
    m_loadModelFileName = filename;
  }

  /**
   * Get whether to reset an incremental classifier at the start of an incoming
   * instance stream
   *
   * @return true if the classifier should be reset
   */
  public boolean getResetIncrementalClassifier() {
    return m_resetIncrementalClassifier;
  }

  /**
   * Set whether to reset an incremental classifier at the start of an incoming
   * instance stream
   *
   * @param reset true if the classifier should be reset
   */
  @OptionMetadata(
    displayName = "Reset incremental classifier",
    description = "Reset classifier (if it is incremental) at the start of the incoming "
      + "instance stream")
  public
    void setResetIncrementalClassifier(boolean reset) {
    m_resetIncrementalClassifier = reset;
  }

  /**
   * Get whether to update an incremental classifier on an incoming instance
   * stream
   *
   * @return true if an incremental classifier should be updated
   */
  public boolean getUpdateIncrementalClassifier() {
    return m_updateIncrementalClassifier;
  }

  /**
   * Set whether to update an incremental classifier on an incoming instance
   * stream
   *
   * @param update true if an incremental classifier should be updated
   */
  @OptionMetadata(
    displayName = "Update incremental classifier",
    description = " Update an incremental classifier on incoming instance stream")
  public
    void setUpdateIncrementalClassifier(boolean update) {
    m_updateIncrementalClassifier = update;
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    try {
      getStepManager().processing();
      if (m_isReset) {
        m_isReset = false;
        Instances incomingStructure = null;
        if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
          incomingStructure =
            new Instances(
              ((Instance) data.getPayloadElement(StepManager.CON_INSTANCE))
                .dataset(),
              0);
        } else {
          incomingStructure =
            (Instances) data.getPayloadElement(data.getConnectionName());
        }
        if (incomingStructure.classAttribute() == null) {
          getStepManager()
            .logWarning(
              "No class index is set in the data - using last attribute as class");
          incomingStructure
            .setClassIndex(incomingStructure.numAttributes() - 1);
        }

        if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
          m_streaming = true;
          if (m_trainedClassifier == null) {
            m_trainedClassifier =
              weka.classifiers.AbstractClassifier
                .makeCopy(m_classifierTemplate);
            getStepManager().logBasic("Initialising incremental classifier");
            m_trainedClassifier.buildClassifier(incomingStructure);
            m_trainedClassifierHeader = incomingStructure;
          } else if (m_resetIncrementalClassifier && m_classifierIsIncremental) {
            // make a copy here, just in case buildClassifier() implementations
            // do not re-initialize the classifier correctly
            m_trainedClassifier =
              weka.classifiers.AbstractClassifier
                .makeCopy(m_classifierTemplate);
            m_trainedClassifierHeader = incomingStructure;
            getStepManager().logBasic("Resetting incremental classifier");
            m_trainedClassifier.buildClassifier(m_trainedClassifierHeader);
          }

          getStepManager()
            .logBasic(
              m_updateIncrementalClassifier && m_classifierIsIncremental ? "Training incrementally"
                : "Predicting incrementally");
        } else if (data.getConnectionName().equals(StepManager.CON_TRAININGSET)) {
          m_trainedClassifierHeader = incomingStructure;
        } else if (data.getConnectionName().equals(StepManager.CON_TESTSET)
          && getStepManager().numIncomingConnectionsOfType(
            StepManager.CON_TRAININGSET) == 0
          && m_classifierTemplate instanceof InputMappedClassifier) {
          m_trainedClassifier =
            weka.classifiers.AbstractClassifier.makeCopy(m_classifierTemplate);
          // force the InputMappedClassifier to load a model (if one has been
          // configured)
          ((InputMappedClassifier) m_trainedClassifier).getModelHeader(null);
        }

        if (m_trainedClassifierHeader != null
          && !incomingStructure.equalHeaders(m_trainedClassifierHeader)) {
          if (!(m_trainedClassifier instanceof InputMappedClassifier)) {
            throw new WekaException(
              "Structure of incoming data does not match "
                + "that of the trained classifier");
          }
        }
      }

      if (m_streaming) {
        processStreaming(data);
      } else if (m_trainTestHelper != null) {
        // train test pairs
        m_trainTestHelper.process(data);
      } else {
        // test only
        processOnlyTestSet(data);
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
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
  public weka.classifiers.Classifier processPrimary(Integer setNum,
    Integer maxSetNum, Data data,
    PairedDataHelper<weka.classifiers.Classifier> helper) throws WekaException {

    Instances trainingData = data.getPrimaryPayload();
    if (m_trainedClassifierHeader == null) {
      m_trainedClassifierHeader = new Instances(trainingData, 0);
    }
    try {
      weka.classifiers.Classifier classifier =
        AbstractClassifier.makeCopy(m_classifierTemplate);

      String classifierDesc = classifier.getClass().getCanonicalName();
      classifierDesc =
        classifierDesc.substring(classifierDesc.lastIndexOf(".") + 1);
      if (classifier instanceof OptionHandler) {
        String optsString =
          Utils.joinOptions(((OptionHandler) classifier).getOptions());
        classifierDesc += " " + optsString;
      }
      if (classifier instanceof EnvironmentHandler) {
        ((EnvironmentHandler) classifier).setEnvironment(getStepManager()
          .getExecutionEnvironment().getEnvironmentVariables());
      }

      // retain the training data
      helper
        .addIndexedValueToNamedStore("trainingSplits", setNum, trainingData);
      if (!isStopRequested()) {
        getStepManager().logBasic(
          "Building " + classifierDesc + " on " + trainingData.relationName()
            + " for fold/set " + setNum + " out of " + maxSetNum);
        if (getStepManager().getLoggingLevel().ordinal() > LoggingLevel.LOW
          .ordinal()) {
          getStepManager().statusMessage(
            "Building " + classifierDesc + " on fold/set " + setNum);
        }

        if (maxSetNum == 1) {
          // single train/test split - makes sense to retain this trained
          // classifier
          m_trainedClassifier = classifier;
        }
        classifier.buildClassifier((Instances) trainingData);
        getStepManager().logDetailed(
          "Finished building " + classifierDesc + "on "
            + trainingData.relationName() + " for fold/set " + setNum
            + " out of " + maxSetNum);

        outputTextData(classifier, setNum);
        outputGraphData(classifier, setNum);

        if (getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_TESTSET) == 0) {
          // output a batch classifier for just the trained model
          Data batchClassifier =
            new Data(StepManager.CON_BATCH_CLASSIFIER, classifier);
          batchClassifier.setPayloadElement(
            StepManager.CON_AUX_DATA_TRAININGSET, trainingData);
          batchClassifier.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM,
            setNum);
          batchClassifier.setPayloadElement(
            StepManager.CON_AUX_DATA_MAX_SET_NUM, maxSetNum);
          batchClassifier.setPayloadElement(StepManager.CON_AUX_DATA_LABEL,
            getName());
          getStepManager().outputData(batchClassifier);
        }
      }
      return classifier;
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
    PairedDataHelper<weka.classifiers.Classifier> helper) throws WekaException {

    // trained classifier for this set number
    weka.classifiers.Classifier classifier =
      helper.getIndexedPrimaryResult(setNum);

    // test data
    Instances testSplit = data.getPrimaryPayload();

    if (m_trainedClassifierHeader != null
      && !testSplit.equalHeaders(m_trainedClassifierHeader)) {
      if (!(m_trainedClassifier instanceof InputMappedClassifier)) {
        throw new WekaException(
          "Structure of incoming data does not match "
            + "that of the trained classifier");
      }
    }

    // paired training data
    Instances trainingSplit =
      helper.getIndexedValueFromNamedStore("trainingSplits", setNum);

    getStepManager().logBasic(
      "Dispatching model for set " + setNum + " out of " + maxSetNum
        + " to output");

    Data batchClassifier =
      new Data(StepManager.CON_BATCH_CLASSIFIER, classifier);
    batchClassifier.setPayloadElement(StepManager.CON_AUX_DATA_TRAININGSET,
      trainingSplit);
    batchClassifier.setPayloadElement(StepManager.CON_AUX_DATA_TESTSET,
      testSplit);
    batchClassifier.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
    batchClassifier.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
      maxSetNum);
    batchClassifier
      .setPayloadElement(StepManager.CON_AUX_DATA_LABEL, getName());
    getStepManager().outputData(batchClassifier);
  }

  /**
   * Process a Data object in the case where we only have an incoming testSet
   * connection
   *
   * @param data the Data object to process
   * @throws WekaException if a problem occurs
   */
  protected void processOnlyTestSet(Data data) throws WekaException {
    // avoid any potential thread safety issues...
    try {
      weka.classifiers.Classifier tempToTest =
        weka.classifiers.AbstractClassifier.makeCopy(m_trainedClassifier);
      Data batchClassifier = new Data(StepManager.CON_BATCH_CLASSIFIER);
      batchClassifier.setPayloadElement(StepManager.CON_BATCH_CLASSIFIER,
        tempToTest);
      batchClassifier.setPayloadElement(StepManager.CON_AUX_DATA_TESTSET,
        data.getPayloadElement(StepManager.CON_TESTSET));
      batchClassifier.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM,
        data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1));
      batchClassifier.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
        data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1));
      batchClassifier.setPayloadElement(StepManager.CON_AUX_DATA_LABEL,
        getName());
      getStepManager().outputData(batchClassifier);
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
   * Process a Data object in the case of an incoming instance (streaming)
   * connection
   *
   * @param data the Data object to process
   * @throws WekaException if a problem occurs
   */
  protected void processStreaming(Data data) throws WekaException {
    if (isStopRequested()) {
      return;
    }
    Instance inst = (Instance) data.getPayloadElement(StepManager.CON_INSTANCE);
    if (getStepManager().isStreamFinished(data)) {
      // finished
      if (m_trainedClassifier instanceof UpdateableBatchProcessor) {
        try {
          ((UpdateableBatchProcessor) m_trainedClassifier).batchFinished();
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
      }

      // notify any downstream steps consuming incremental classifier
      // data that the stream has finished
      m_incrementalData.setPayloadElement(
        StepManager.CON_INCREMENTAL_CLASSIFIER, m_trainedClassifier);
      m_incrementalData.setPayloadElement(
        StepManager.CON_AUX_DATA_TEST_INSTANCE, null);
      // getStepManager().outputData(m_incrementalData);

      outputTextData(m_trainedClassifier, -1);
      outputGraphData(m_trainedClassifier, 0);

      if (!isStopRequested()) {
        getStepManager().throughputFinished(m_incrementalData);
      }
      return;
    }

    // test on the instance
    m_incrementalData.setPayloadElement(StepManager.CON_AUX_DATA_TEST_INSTANCE,
      inst);
    m_incrementalData.setPayloadElement(StepManager.CON_INCREMENTAL_CLASSIFIER,
      m_trainedClassifier);
    getStepManager().outputData(m_incrementalData.getConnectionName(),
      m_incrementalData);

    // train on the instance?
    getStepManager().throughputUpdateStart();
    if (m_classifierIsIncremental && m_updateIncrementalClassifier) {
      if (!inst.classIsMissing()) {
        try {
          ((UpdateableClassifier) m_trainedClassifier).updateClassifier(inst);
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
      }
    }
    getStepManager().throughputUpdateEnd();
  }

  /**
   * Output a Data object containing a textual description of a model to any
   * outgoing text connections
   *
   * @param classifier the classifier to get the textual description of
   * @param setNum the set number of the training data
   * @throws WekaException if a problem occurs
   */
  protected void outputTextData(weka.classifiers.Classifier classifier,
    int setNum) throws WekaException {

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_TEXT) == 0) {
      return;
    }

    Data textData = new Data(StepManager.CON_TEXT);

    String modelString = classifier.toString();
    String titleString = classifier.getClass().getName();

    titleString =
      titleString.substring(titleString.lastIndexOf('.') + 1,
        titleString.length());
    modelString =
      "=== Classifier model ===\n\n" + "Scheme:   " + titleString + "\n"
        + "Relation: " + m_trainedClassifierHeader.relationName() + "\n\n"
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
   * Output a Data object containing a dot graph, if the model is Drawable and
   * we have downstream steps receiving graph connections.
   *
   * @param classifier the classifier to generate the graph from
   * @param setNum the set number of the data used to generate the graph
   * @throws WekaException if a problem occurs
   */
  protected void outputGraphData(weka.classifiers.Classifier classifier,
    int setNum) throws WekaException {
    if (classifier instanceof Drawable) {
      if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_GRAPH) == 0) {
        return;
      }

      try {
        String graphString = ((Drawable) classifier).graph();
        int graphType = ((Drawable) classifier).graphType();
        String grphTitle = classifier.getClass().getCanonicalName();
        grphTitle =
          grphTitle.substring(grphTitle.lastIndexOf('.') + 1,
            grphTitle.length());
        grphTitle =
          "Set " + setNum + " (" + m_trainedClassifierHeader.relationName()
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

    if (numTraining == 0 && numTesting == 0) {
      result.add(StepManager.CON_INSTANCE);
    }

    if (numInstance == 0 && numTraining == 0) {
      result.add(StepManager.CON_TRAININGSET);
    }

    if (numInstance == 0 && numTesting == 0) {
      result.add(StepManager.CON_TESTSET);
    }

    if (getStepManager().numIncomingConnectionsOfType(StepManager.CON_INFO) == 0) {
      result.add(StepManager.CON_INFO);
    }

    return result;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnections() > 0) {
      int numTraining =
        getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_TRAININGSET);
      int numTesting =
        getStepManager().numIncomingConnectionsOfType(StepManager.CON_TESTSET);
      int numInstance =
        getStepManager().numIncomingConnectionsOfType(StepManager.CON_INSTANCE);

      if (numInstance > 0) {
        result.add(StepManager.CON_INCREMENTAL_CLASSIFIER);
      } else if (numTraining > 0 || numTesting > 0) {
        result.add(StepManager.CON_BATCH_CLASSIFIER);
      }

      result.add(StepManager.CON_TEXT);

      if (getClassifier() instanceof Drawable && numTraining > 0) {
        result.add(StepManager.CON_GRAPH);
      }
    }

    // info connection - downstream steps can get our wrapped classifier
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
        SerializationHelper.getObjectInputStream(new FileInputStream(new File(
          filePath)));

      m_trainedClassifier = (weka.classifiers.Classifier) is.readObject();

      if (!(m_trainedClassifier.getClass().getCanonicalName()
        .equals(getClassifier().getClass().getCanonicalName()))) {
        throw new Exception("The loaded model '"
          + m_trainedClassifier.getClass().getCanonicalName() + "' is not a '"
          + getClassifier().getClass().getCanonicalName() + "'");
      }

      // try and read the header
      try {
        m_trainedClassifierHeader = (Instances) is.readObject();
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
}
