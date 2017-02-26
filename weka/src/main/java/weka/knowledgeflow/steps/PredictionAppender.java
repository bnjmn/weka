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
 *    PredictionAppender.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.classifiers.UpdateableClassifier;
import weka.classifiers.misc.InputMappedClassifier;
import weka.clusterers.DensityBasedClusterer;
import weka.core.*;
import weka.filters.unsupervised.attribute.Add;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Step that can produce data with predictions appended from batch or
 * incremental classifiers and clusterers
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "PredictionAppender",
  category = "Evaluation",
  toolTipText = "Append predictions from classifiers or clusterers to incoming data ",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "PredictionAppender.gif")
public class PredictionAppender extends BaseStep {

  private static final long serialVersionUID = 3558618759400903936L;

  /** True if probabilities are to be appended */
  protected boolean m_appendProbabilities;

  /** Holds structure of streaming output */
  protected Instances m_streamingOutputStructure;

  /** Re-usable Data object for streaming output */
  protected Data m_instanceData = new Data(StepManager.CON_INSTANCE);

  /** Keep track of indexes of string attributes in the streaming case */
  protected List<Integer> m_stringAttIndexes;

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    m_streamingOutputStructure = null;
  }

  /**
   * Get the incoming connection types that this step accepts
   *
   * @return a list of acceptable incoming connection types
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays
        .asList(StepManager.CON_BATCH_CLASSIFIER,
          StepManager.CON_INCREMENTAL_CLASSIFIER,
          StepManager.CON_BATCH_CLUSTERER);
    }

    return new ArrayList<String>();
  }

  /**
   * Get a list of outgoing connection types that this step can produce at this
   * time
   *
   * @return a list of outgoing connection types
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_BATCH_CLASSIFIER) > 0
      || getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_BATCH_CLUSTERER) > 0) {
      result.add(StepManager.CON_TRAININGSET);
      result.add(StepManager.CON_TESTSET);
    } else if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_INCREMENTAL_CLASSIFIER) > 0) {
      result.add(StepManager.CON_INSTANCE);
    }

    return result;
  }

  /**
   * Process incoming data
   *
   * @param data the Data object to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    Instances trainingData =
      (Instances) data.getPayloadElement(StepManager.CON_AUX_DATA_TRAININGSET);
    Instances testData =
      (Instances) data.getPayloadElement(StepManager.CON_AUX_DATA_TESTSET);
    Instance streamInstance =
      (Instance) data.getPayloadElement(StepManager.CON_AUX_DATA_TEST_INSTANCE);
    if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_BATCH_CLASSIFIER) > 0) {
      processBatchClassifierCase(data, trainingData, testData);
    } else if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_INCREMENTAL_CLASSIFIER) > 0) {
      processIncrementalClassifier(data, streamInstance);
    } else if (getStepManager().numIncomingConnectionsOfType(
      StepManagerImpl.CON_BATCH_CLUSTERER) > 0) {
      processBatchClustererCase(data, trainingData, testData);
    }
  }

  /**
   * Process an incremental classifier
   *
   * @param data the Data object to process
   * @param inst the instance to process
   * @throws WekaException if a problem occurs
   */
  protected void processIncrementalClassifier(Data data, Instance inst)
    throws WekaException {
    if (isStopRequested()) {
      return;
    }

    if (getStepManager().isStreamFinished(data)) {
      // done
      // notify downstream steps of end of stream
      Data d = new Data(StepManager.CON_INSTANCE);
      getStepManager().throughputFinished(d);
      return;
    }

    getStepManager().throughputUpdateStart();
    boolean labelOrNumeric =
      !m_appendProbabilities || inst.classAttribute().isNumeric();
    weka.classifiers.Classifier classifier =
      (weka.classifiers.Classifier) data
        .getPayloadElement(StepManager.CON_INCREMENTAL_CLASSIFIER);
    if (m_streamingOutputStructure == null) {
      // start of stream
      if (classifier == null) {
        throw new WekaException("No classifier in incoming data object!");
      }
      if (!(classifier instanceof UpdateableClassifier)) {
        throw new WekaException("Classifier in data object is not "
          + "an UpdateableClassifier!");
      }
      m_stringAttIndexes = new ArrayList<Integer>();
      for (int i = 0; i < inst.numAttributes(); i++) {
        if (inst.attribute(i).isString()) {
          m_stringAttIndexes.add(i);
        }
      }
      try {
        m_streamingOutputStructure =
          makeOutputDataClassifier(inst.dataset(), classifier, !labelOrNumeric,
            "_with_predictions");
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }

    double[] instanceVals =
      new double[m_streamingOutputStructure.numAttributes()];
    Instance newInstance = null;
    for (int i = 0; i < inst.numAttributes(); i++) {
      instanceVals[i] = inst.value(i);
    }
    if (!m_appendProbabilities || inst.classAttribute().isNumeric()) {
      try {
        double predClass = classifier.classifyInstance(inst);
        instanceVals[instanceVals.length - 1] = predClass;
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    } else if (m_appendProbabilities) {
      try {
        double[] preds = classifier.distributionForInstance(inst);
        int index = 0;
        for (int i = instanceVals.length - inst.classAttribute().numValues(); i < instanceVals.length; i++) {
          instanceVals[i] = preds[index++];
        }
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }

    Instance newInst = new DenseInstance(inst.weight(), instanceVals);
    newInst.setDataset(m_streamingOutputStructure);
    // check for string attributes
    if (m_stringAttIndexes != null) {
      for (int i = 0; i < m_stringAttIndexes.size(); i++) {
        int index = m_stringAttIndexes.get(i);
        m_streamingOutputStructure.attribute(index).setStringValue(
          inst.stringValue(index));
      }
    }

    m_instanceData.setPayloadElement(StepManagerImpl.CON_INSTANCE, newInst);
    if (isStopRequested()) {
      return;
    }
    getStepManager().throughputUpdateEnd();
    getStepManager().outputData(m_instanceData.getConnectionName(),
      m_instanceData);
  }

  /**
   * Process a batch classifier
   *
   * @param data the Data object to process
   * @param trainingData the training data (can be null)
   * @param testData the test data (can be null)
   * @throws WekaException if a problem occurs
   */
  protected void processBatchClustererCase(Data data, Instances trainingData,
    Instances testData) throws WekaException {

    if (isStopRequested()) {
      getStepManager().interrupted();
      return;
    }

    weka.clusterers.Clusterer clusterer =
      (weka.clusterers.Clusterer) data
        .getPayloadElement(StepManager.CON_BATCH_CLUSTERER);
    int setNum =
      (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM);
    int maxSetNum =
      (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM);
    String relationNameModifier = "_set_" + setNum + "_of_" + maxSetNum;

    if (m_appendProbabilities && !(clusterer instanceof DensityBasedClusterer)) {
      throw new WekaException(
        "Only DensityBasedClusterers can append probabilities.");
    }

    try {
      getStepManager().processing();
      boolean clusterLabel =
        !m_appendProbabilities || !(clusterer instanceof DensityBasedClusterer);
      Instances newTrainInstances =
        trainingData != null ? makeOutputDataClusterer(trainingData, clusterer,
          !clusterLabel, relationNameModifier) : null;
      Instances newTestInstances =
        testData != null ? makeOutputDataClusterer(testData, clusterer,
          !clusterLabel, relationNameModifier) : null;

      if (newTrainInstances != null
        && getStepManager().numOutgoingConnectionsOfType(
          StepManager.CON_TRAININGSET) > 0) {
        for (int i = 0; i < newTrainInstances.numInstances(); i++) {
          if (clusterLabel) {
            predictLabelClusterer(clusterer, newTrainInstances.instance(i),
              trainingData.instance(i));
          } else {
            predictProbabilitiesClusterer((DensityBasedClusterer) clusterer,
              newTrainInstances.instance(i), trainingData.instance(i));
          }
        }

        if (isStopRequested()) {
          getStepManager().interrupted();
          return;
        }
        Data outTrain = new Data(StepManager.CON_TRAININGSET);
        outTrain.setPayloadElement(StepManager.CON_TRAININGSET,
          newTrainInstances);
        outTrain.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
        outTrain.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
          maxSetNum);
        getStepManager().outputData(outTrain);
      }

      if (newTestInstances != null
        && (getStepManager().numOutgoingConnectionsOfType(
          StepManager.CON_TESTSET) > 0 || getStepManager()
          .numOutgoingConnectionsOfType(StepManager.CON_DATASET) > 0)) {

        for (int i = 0; i < newTestInstances.numInstances(); i++) {
          if (clusterLabel) {
            predictLabelClusterer(clusterer, newTestInstances.instance(i),
              testData.instance(i));
          } else {
            predictProbabilitiesClusterer((DensityBasedClusterer) clusterer,
              newTestInstances.instance(i), testData.instance(i));
          }
        }

        if (isStopRequested()) {
          getStepManager().interrupted();
          return;
        }
        if (getStepManager().numOutgoingConnectionsOfType(
          StepManager.CON_TESTSET) > 0) {
          Data outTest = new Data(StepManager.CON_TESTSET);
          outTest.setPayloadElement(StepManager.CON_TESTSET, newTestInstances);
          outTest.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
          outTest.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
            maxSetNum);
          getStepManager().outputData(outTest);
        }
        if (getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_DATASET) > 0) {
          Data outData = new Data(StepManager.CON_DATASET);
          outData.setPayloadElement(StepManager.CON_DATASET, newTestInstances);
          outData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
          outData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
            maxSetNum);
          getStepManager().outputData(outData);
        }
      }
      getStepManager().finished();
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Process a batch classifier
   *
   * @param data the Data object to process
   * @param trainingData the training data (can be null)
   * @param testData the test data (can be null)
   * @throws WekaException if a problem occurs
   */
  protected void processBatchClassifierCase(Data data, Instances trainingData,
    Instances testData) throws WekaException {

    if (isStopRequested()) {
      getStepManager().interrupted();
      return;
    }

    weka.classifiers.Classifier classifier =
      (weka.classifiers.Classifier) data
        .getPayloadElement(StepManager.CON_BATCH_CLASSIFIER);
    int setNum =
      (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM);
    int maxSetNum =
      (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM);
    String relationNameModifier = "_set_" + setNum + "_of_" + maxSetNum;
    boolean classNumeric =
      trainingData != null ? trainingData.classAttribute().isNumeric()
        : testData.classAttribute().isNumeric();

    boolean labelOrNumeric = !m_appendProbabilities || classNumeric;
    try {
      getStepManager().processing();
      Instances newTrainInstances =
        trainingData != null ? makeOutputDataClassifier(trainingData,
          classifier, !labelOrNumeric, relationNameModifier) : null;
      Instances newTestInstances =
        testData != null ? makeOutputDataClassifier(testData, classifier,
          !labelOrNumeric, relationNameModifier) : null;
      if (newTrainInstances != null
        && getStepManager().numOutgoingConnectionsOfType(
          StepManager.CON_TRAININGSET) > 0) {
        for (int i = 0; i < newTrainInstances.numInstances(); i++) {
          if (labelOrNumeric) {
            predictLabelClassifier(classifier, newTrainInstances.instance(i),
              trainingData.instance(i));
          } else {
            predictProbabilitiesClassifier(classifier,
              newTrainInstances.instance(i), trainingData.instance(i));
          }
        }
        if (isStopRequested()) {
          getStepManager().interrupted();
          return;
        }
        Data outTrain = new Data(StepManager.CON_TRAININGSET);
        outTrain.setPayloadElement(StepManager.CON_TRAININGSET,
          newTrainInstances);
        outTrain.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
        outTrain.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
          maxSetNum);
        getStepManager().outputData(outTrain);
      }
      if (newTestInstances != null
        && (getStepManager().numOutgoingConnectionsOfType(
          StepManager.CON_TESTSET) > 0 || getStepManager()
          .numOutgoingConnectionsOfType(StepManager.CON_DATASET) > 0)) {
        for (int i = 0; i < newTestInstances.numInstances(); i++) {
          if (labelOrNumeric) {
            predictLabelClassifier(classifier, newTestInstances.instance(i),
              testData.instance(i));
          } else {
            predictProbabilitiesClassifier(classifier,
              newTestInstances.instance(i), testData.instance(i));
          }
        }
        if (isStopRequested()) {
          getStepManager().interrupted();
          return;
        }
        if (getStepManager().numOutgoingConnectionsOfType(
          StepManager.CON_TESTSET) > 0) {
          Data outTest = new Data(StepManager.CON_TESTSET);
          outTest.setPayloadElement(StepManager.CON_TESTSET, newTestInstances);
          outTest.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
          outTest.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
            maxSetNum);
          getStepManager().outputData(outTest);
        }
        if (getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_DATASET) > 0) {
          Data outData = new Data(StepManager.CON_DATASET);
          outData.setPayloadElement(StepManager.CON_DATASET, newTestInstances);
          outData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
          outData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
            maxSetNum);
          getStepManager().outputData(outData);
        }
      }
      getStepManager().finished();
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Add a cluster label to an instance using a clusterer
   *
   * @param clusterer the clusterer to use
   * @param inst the instance to append a prediction to
   * @param instOrig the original instance
   * @throws WekaException if a problem occurs
   */
  protected void predictLabelClusterer(weka.clusterers.Clusterer clusterer,
    Instance inst, Instance instOrig) throws WekaException {
    try {
      int cluster = clusterer.clusterInstance(instOrig);
      inst.setValue(inst.numAttributes() - 1, (double) cluster);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Add a distribution over cluster labels to an instance using a
   * DensityBasedClusterer
   *
   * @param clusterer the clusterer to use
   * @param inst the instance to append a prediction to
   * @param instOrig the original instance
   * @throws WekaException if a problem occurs
   */
  protected void predictProbabilitiesClusterer(DensityBasedClusterer clusterer,
    Instance inst, Instance instOrig) throws WekaException {
    try {
      double[] preds = clusterer.distributionForInstance(instOrig);
      for (int i = 0; i < preds.length; i++) {
        inst.setValue(inst.numAttributes() - preds.length + i, preds[i]);
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Add a label to an instance using a classifier
   *
   * @param classifier the classifier to use
   * @param inst the instance to append prediction to
   * @param instOrig the original instance
   * @throws WekaException if a problem occurs
   */
  protected void predictLabelClassifier(weka.classifiers.Classifier classifier,
    Instance inst, Instance instOrig) throws WekaException {

    try {
      double pred = classifier.classifyInstance(instOrig);
      inst.setValue(inst.numAttributes() - 1, pred);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Add a distribution over class labels to an instance using a classifier
   *
   * @param classifier the classifier to use
   * @param inst the instance to append prediction to
   * @param instOrig the original instance
   * @throws WekaException if a problem occurs
   */
  protected void predictProbabilitiesClassifier(
    weka.classifiers.Classifier classifier, Instance inst, Instance instOrig)
    throws WekaException {
    try {
      double[] preds = classifier.distributionForInstance(instOrig);
      for (int i = 0; i < preds.length; i++) {
        inst.setValue(inst.numAttributes() - preds.length + i, preds[i]);
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Make an output dataset for a clusterer. Either a single attribute is added
   * for holding cluster labels, or a series of attributes are added in order to
   * hold predicted cluster distribution
   *
   * @param inputData the incoming data
   * @param clusterer the clusterer
   * @param distribution true if a distribution over cluster labels will be
   *          predicted
   * @param relationNameModifier modifier to add to the incoming relation name
   * @return the output dataset
   * @throws Exception if a problem occurs
   */
  protected Instances makeOutputDataClusterer(Instances inputData,
    weka.clusterers.Clusterer clusterer, boolean distribution,
    String relationNameModifier) throws Exception {

    String clustererName = clusterer.getClass().getName();
    clustererName =
      clustererName.substring(clustererName.lastIndexOf('.') + 1,
        clustererName.length());
    Instances newData = new Instances(inputData);

    if (distribution) {
      for (int i = 0; i < clusterer.numberOfClusters(); i++) {
        Add addF = new Add();
        addF.setAttributeIndex("last");
        addF.setAttributeName("prob_cluster" + i);
        addF.setInputFormat(newData);
        newData = weka.filters.Filter.useFilter(newData, addF);
      }
    } else {
      Add addF = new Add();
      addF.setAttributeIndex("last");
      addF.setAttributeName("assigned_cluster: " + clustererName);
      String clusterLabels = "0";
      for (int i = 1; i <= clusterer.numberOfClusters() - 1; i++) {
        clusterLabels += "," + i;
      }
      addF.setNominalLabels(clusterLabels);
      addF.setInputFormat(newData);
      newData = weka.filters.Filter.useFilter(newData, addF);
    }

    newData.setRelationName(inputData.relationName() + relationNameModifier);
    return newData;
  }

  /**
   * Make an output dataset for a classifier. Either a single attribute is added
   * for holding class labels, or a series of attributes are added in order to
   * hold predicted class distribution
   *
   * @param inputData the incoming data
   * @param classifier the classifier
   * @param distribution true if a distribution over class labels will be
   *          predicted
   * @param relationNameModifier modifier to add to the incoming relation name
   * @return the output dataset
   * @throws Exception if a problem occurs
   */
  protected Instances makeOutputDataClassifier(Instances inputData,
    weka.classifiers.Classifier classifier, boolean distribution,
    String relationNameModifier) throws Exception {

    // get class attribute from InputMappedClassifier (if necessary)
    Attribute classAttribute = inputData.classAttribute();
    if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
      classAttribute = ((InputMappedClassifier) classifier).getModelHeader(new Instances(inputData, 0)).classAttribute();
    }

    String classifierName = classifier.getClass().getName();
    classifierName =
      classifierName.substring(classifierName.lastIndexOf('.') + 1,
        classifierName.length());
    Instances newData = new Instances(inputData);

    if (distribution) {
      for (int i = 0; i < classAttribute.numValues(); i++) {
        Add addF = new Add();
        addF.setAttributeIndex("last");
        addF.setAttributeName(classifierName + "_prob_"
          + classAttribute.value(i));
        addF.setInputFormat(newData);
        newData = weka.filters.Filter.useFilter(newData, addF);
      }
    } else {
      Add addF = new Add();
      addF.setAttributeIndex("last");
      addF.setAttributeName("class_predicted_by: " + classifierName);
      if (classAttribute.isNominal()) {
        String classLabels = classAttribute.value(0);
        for (int i = 1; i < classAttribute.numValues(); i++) {
          classLabels += "," + classAttribute.value(i);
        }
        addF.setNominalLabels(classLabels);
      }
      addF.setInputFormat(inputData);
      newData = weka.filters.Filter.useFilter(inputData, addF);
    }

    newData.setRelationName(inputData.relationName() + relationNameModifier);
    return newData;
  }

  /**
   * Set whether to append probability distributions rather than predicted
   * classes
   *
   * @param append true to append probability distributions
   */
  public void setAppendProbabilities(boolean append) {
    m_appendProbabilities = append;
  }

  /**
   * Get whether to append probability distributions rather than predicted
   * classes
   *
   * @return true if probability distributions are to be appended
   */
  @OptionMetadata(displayName = "Append probabilities",
    description = "Append probabilities")
  public boolean getAppendProbabilities() {
    return m_appendProbabilities;
  }
}
