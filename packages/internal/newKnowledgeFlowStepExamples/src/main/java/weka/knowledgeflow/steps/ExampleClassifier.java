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
 *    ExampleClassifier.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple paired processing example that trains and evaluates a classifier.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 * @see PairedDataHelper
 * @see weka.knowledgeflow.steps.PairedDataHelper.PairedProcessor
 */
@KFStep(name = "ExampleClassifier", category = "Examples",
  toolTipText = "A simple " + "custom classifier processing example",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DefaultClassifier.gif")
public class ExampleClassifier extends BaseStep implements
  PairedDataHelper.PairedProcessor<weka.classifiers.Classifier> {

  /** For serialization */
  private static final long serialVersionUID = -1032628770114748181L;

  /** The classifier to train */
  protected weka.classifiers.Classifier m_classifier = new NaiveBayes();

  /** Handles train test pair routing and synchronization for us */
  protected transient PairedDataHelper<weka.classifiers.Classifier> m_trainTestHelper;

  /**
   * Set the classifier to train/eval
   * 
   * @param classifier the classifier to use
   */
  @OptionMetadata(displayName = "The classifier to use", description = "The "
    + "classifier to train")
  public void setClassifier(weka.classifiers.Classifier classifier) {
    m_classifier = classifier;
  }

  /**
   * Get the classifier to train/eval
   *
   * @return the classifier to use
   */
  public weka.classifiers.Classifier getClassifier() {
    return m_classifier;
  }

  /**
   * Initialize the step
   * 
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {

    // create and initialize our train/test pair helper
    m_trainTestHelper =
      new PairedDataHelper<weka.classifiers.Classifier>(
        this,
        this,
        StepManager.CON_TRAININGSET,
        getStepManager().numIncomingConnectionsOfType(StepManager.CON_TESTSET) > 0 ? StepManager.CON_TESTSET
          : null);
  }

  /**
   * Incoming connections we can accept.
   * 
   * @return the list of incoming connections that can be made given our current
   *         state
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    // we can accept a training connection, as long as we don't already
    // have one connected
    if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_TRAININGSET) == 0) {
      result.add(StepManager.CON_TRAININGSET);
    }

    // we can accept a test connection, as long as we don't already have one
    // connected and we have a training connection
    if (getStepManager().numIncomingConnectionsOfType(StepManager.CON_TESTSET) == 0
      && getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_TRAININGSET) > 0) {
      result.add(StepManager.CON_TESTSET);
    }

    return result;
  }

  /**
   * Outgoing connections we can produce
   *
   * @return the list of outgoing connections we can produce given our current state
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {

    // we will produce some textual results as long as we
    // have at least an incoming training connection
    return getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_TRAININGSET) > 0 ? Arrays.asList(StepManager.CON_TEXT)
      : new ArrayList<String>();
  }

  /**
   * Process some incoming data (either train or test sets)
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {

    // let our helper sort out train/test and synchronization issues
    m_trainTestHelper.process(data);
  }

  /**
   * Process training data
   *
   * @param setNum the set number of this piece of training data
   * @param maxSetNum the maximum set number in the batch
   * @param data the data object containing the training data
   * @param helper the PairedDataHelper to use
   * @return a trained classifier
   * @throws WekaException if a problem occurs
   */
  @Override
  public weka.classifiers.Classifier processPrimary(Integer setNum,
    Integer maxSetNum, Data data,
    PairedDataHelper<weka.classifiers.Classifier> helper) throws WekaException {
    try {
      // make a copy of our template classifier to train
      weka.classifiers.Classifier classifierToTrain =
        AbstractClassifier.makeCopy(m_classifier);

      // get the training instances from the Data object
      Instances trainData = data.getPrimaryPayload();

      // initialize and store an evaluation object to use when we receive the
      // corresponding test set
      Evaluation evaluation = new Evaluation(trainData);
      helper.addIndexedValueToNamedStore("initializedEvalObjects", setNum,
        evaluation);

      // train the classifier
      getStepManager().logBasic(
        "Training classifier: " + m_classifier.getClass().getCanonicalName()
          + " on set " + setNum + " of " + maxSetNum);
      classifierToTrain.buildClassifier(trainData);

      // output the textual description of the model
      Data textOut =
        new Data(StepManager.CON_TEXT, classifierToTrain.toString());
      textOut.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
        m_classifier.getClass().getCanonicalName());
      textOut.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
      textOut
        .setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, maxSetNum);
      getStepManager().outputData(textOut);

      return classifierToTrain;
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Process test data
   *
   * @param setNum the set number of this piece of test data
   * @param maxSetNum the maximum set number in the batch
   * @param data the data object containing the test data
   * @param helper the PairedDataHelper to use
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processSecondary(Integer setNum, Integer maxSetNum, Data data,
    PairedDataHelper<weka.classifiers.Classifier> helper) throws WekaException {
    Instances testData = data.getPrimaryPayload();

    // get the trained classifier for this set number
    weka.classifiers.Classifier classifier =
      helper.getIndexedPrimaryResult(setNum);

    // get the initialized Evaluation object to use
    Evaluation eval =
      helper.getIndexedValueFromNamedStore("initializedEvalObjects", setNum);

    try {
      // evaluate the classifier
      getStepManager().logBasic(
        "Evaluating classifier: " + m_classifier.getClass().getCanonicalName()
          + " on set " + setNum + " of " + maxSetNum);
      eval.evaluateModel(classifier, testData);

      // output the evaluation results
      String results = eval.toSummaryString();
      Data textData = new Data(StepManager.CON_TEXT, results);
      textData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
        m_classifier.getClass().getCanonicalName() + " - eval");
      textData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
      textData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
        maxSetNum);
      getStepManager().outputData(textData);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }
}
