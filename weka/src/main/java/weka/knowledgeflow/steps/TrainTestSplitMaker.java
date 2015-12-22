package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@KFStep(name = "TrainTestSplitMaker", category = "Evaluation",
  toolTipText = "Create a random train/test split",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "TrainTestSplitMaker.gif")
public class TrainTestSplitMaker extends BaseStep {

  private static final long serialVersionUID = 7685026723199727685L;

  protected String m_trainPercentageS = "66";

  protected String m_seedS = "1";

  protected double m_trainPercentage = 66.0;

  protected long m_seed = 1L;

  @Override
  public String globalInfo() {
    return "A step that randomly splits incoming data into a training and test set";
  }

  /**
   * Tip text info for this property
   * 
   * @return a <code>String</code> value
   */
  public String trainPercentTipText() {
    return "The percentage of data to go into the training set";
  }

  public void setTrainPercent(String percent) {
    m_trainPercentageS = percent;
  }

  public String getTrainPercent() {
    return m_trainPercentageS;
  }

  /**
   * Tip text for this property
   * 
   * @return a <code>String</code> value
   */
  public String seedTipText() {
    return "The randomization seed";
  }

  public void setSeed(String seed) {
    m_seedS = seed;
  }

  public String getSeed() {
    return m_seedS;
  }

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
      getStepManager()
        .logWarning("Unable to parse train percentage value: " + tP);
    }
  }

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
    dataSet.randomize(new Random(m_seed));
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

  @Override
  public List<String> getIncomingConnectionTypes() {
    if (getStepManager().numIncomingConnections() > 0) {
      return new ArrayList<String>();
    }

    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_TESTSET);
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return getStepManager().numIncomingConnections() > 0
      ? Arrays.asList(StepManager.CON_TRAININGSET, StepManager.CON_TESTSET)
      : new ArrayList<String>();
  }

  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    // we produce training and testset connections
    if ((!connectionName.equals(StepManager.CON_TRAININGSET)
      && !connectionName.equals(StepManager.CON_TESTSET))
      || getStepManager().numIncomingConnections() == 0) {
      return null;
    }

    // our output structure is the same as whatever kind of input we are getting
    Instances strucForDatasetCon = getStepManager()
      .getIncomingStructureForConnectionType(StepManager.CON_DATASET);
    if (strucForDatasetCon != null) {
      return strucForDatasetCon;
    }

    Instances strucForTestsetCon = getStepManager()
      .getIncomingStructureForConnectionType(StepManager.CON_TESTSET);
    if (strucForTestsetCon != null) {
      return strucForTestsetCon;
    }

    Instances strucForTrainingCon = getStepManager()
      .getIncomingStructureForConnectionType(StepManager.CON_TRAININGSET);
    if (strucForTestsetCon != null) {
      return strucForTrainingCon;
    }

    return null;
  }
}
