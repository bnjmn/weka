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
 *    TrainTestSplitMaker.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A step that creates a random train/test split from an incoming data set.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "TrainTestSplitMaker",
  category = "Evaluation",
  toolTipText = "A step that randomly splits incoming data into a training and test set",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "TrainTestSplitMaker.gif")
public class TrainTestSplitMaker extends BaseStep {

  private static final long serialVersionUID = 7685026723199727685L;

  /** Default split percentage */
  protected String m_trainPercentageS = "66";

  /** Default seed for the random number generator */
  protected String m_seedS = "1";

  /** Resolved percentage */
  protected double m_trainPercentage = 66.0;

  /**
   * Whether to preserve the order of the data before making the split, rather
   * than randomly shuffling
   */
  protected boolean m_preserveOrder;

  /** Resolved seed */
  protected long m_seed = 1L;

  /**
   * Set the training percentage
   *
   * @param percent the training percentage
   */
  @OptionMetadata(displayName = "Training percentage",
    description = "The percentage of data to go into the training set",
    displayOrder = 1)
  public void setTrainPercent(String percent) {
    m_trainPercentageS = percent;
  }

  /**
   * Get the training percentage
   *
   * @return the training percentage
   */
  public String getTrainPercent() {
    return m_trainPercentageS;
  }

  /**
   * Set the random seed to use
   *
   * @param seed the random seed to use
   */
  @OptionMetadata(displayName = "Random seed",
    description = "The random seed to use when shuffling the data",
    displayOrder = 2)
  public void setSeed(String seed) {
    m_seedS = seed;
  }

  /**
   * Get the random seed to use
   *
   * @return the random seed to use
   */
  public String getSeed() {
    return m_seedS;
  }

  /**
   * Set whether to preserve the order of the instances or not
   *
   * @param preserve true to preserve the order rather than randomly shuffling
   *          first
   */
  @OptionMetadata(
    displayName = "Preserve instance order",
    description = "Preserve the order of the instances rather than randomly shuffling",
    displayOrder = 3)
  public
    void setPreserveOrder(boolean preserve) {
    m_preserveOrder = preserve;
  }

  /**
   * Get whether to preserve the order of the instances or not
   *
   * @return true to preserve the order rather than randomly shuffling first
   */
  public boolean getPreserveOrder() {
    return m_preserveOrder;
  }

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    String seed = getStepManager().environmentSubstitute(getSeed());
    try {
      m_seed = Long.parseLong(seed);
    } catch (NumberFormatException ex) {
      getStepManager().logWarning("Unable to parse seed value: " + seed);
    }

    String tP = getStepManager().environmentSubstitute(getTrainPercent());
    try {
      m_trainPercentage = Double.parseDouble(tP);
    } catch (NumberFormatException ex) {
      getStepManager().logWarning(
        "Unable to parse train percentage value: " + tP);
    }
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    String incomingConnName = data.getConnectionName();
    Instances dataSet = (Instances) data.getPayloadElement(incomingConnName);
    if (dataSet == null) {
      throw new WekaException("Incoming instances should not be null!");
    }

    getStepManager().logBasic("Creating train/test split");
    getStepManager().statusMessage("Creating train/test split");

    if (!getPreserveOrder()) {
      dataSet.randomize(new Random(m_seed));
    }
    int trainSize =
      (int) Math.round(dataSet.numInstances() * m_trainPercentage / 100);
    int testSize = dataSet.numInstances() - trainSize;

    Instances train = new Instances(dataSet, 0, trainSize);
    Instances test = new Instances(dataSet, trainSize, testSize);

    Data trainData = new Data(StepManager.CON_TRAININGSET);
    trainData.setPayloadElement(StepManager.CON_TRAININGSET, train);
    trainData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
    trainData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
    Data testData = new Data(StepManager.CON_TESTSET);
    testData.setPayloadElement(StepManager.CON_TESTSET, test);
    testData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
    testData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);

    if (!isStopRequested()) {
      getStepManager().outputData(trainData, testData);
    }

    getStepManager().finished();
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
    if (getStepManager().numIncomingConnections() > 0) {
      return new ArrayList<String>();
    }

    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_TESTSET);
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
    return getStepManager().numIncomingConnections() > 0 ? Arrays.asList(
      StepManager.CON_TRAININGSET, StepManager.CON_TESTSET)
      : new ArrayList<String>();
  }

  /**
   * If possible, get the output structure for the named connection type as a
   * header-only set of instances. Can return null if the specified connection
   * type is not representable as Instances or cannot be determined at present.
   *
   * @param connectionName the name of the connection type to get the output
   *          structure for
   * @return the output structure as a header-only Instances object
   * @throws WekaException if a problem occurs
   */
  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    // we produce training and testset connections
    if ((!connectionName.equals(StepManager.CON_TRAININGSET) && !connectionName
      .equals(StepManager.CON_TESTSET))
      || getStepManager().numIncomingConnections() == 0) {
      return null;
    }

    // our output structure is the same as whatever kind of input we are getting
    Instances strucForDatasetCon =
      getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_DATASET);
    if (strucForDatasetCon != null) {
      return strucForDatasetCon;
    }

    Instances strucForTestsetCon =
      getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_TESTSET);
    if (strucForTestsetCon != null) {
      return strucForTestsetCon;
    }

    Instances strucForTrainingCon =
      getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_TRAININGSET);
    if (strucForTrainingCon != null) {
      return strucForTrainingCon;
    }

    return null;
  }
}
