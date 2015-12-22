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

@KFStep(name = "CrossValidationFoldMaker", category = "Evaluation",
  toolTipText = "A Step that creates stratified cross-validation folds from incoming data",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "CrossValidationFoldMaker.gif")
public class CrossValidationFoldMaker extends BaseStep {

  private static final long serialVersionUID = 6090713408437825355L;

  protected boolean m_preserveOrder;

  protected String m_numFoldsS = "10";
  protected String m_seedS = "1";

  protected int m_numFolds = 10;
  protected long m_seed = 1L;

  @OptionMetadata(displayName = "Number of folds",
    description = "THe number of folds to create", displayOrder = 0)
  public void setNumFolds(String folds) {
    m_numFoldsS = folds;
  }

  public String getNumFolds() {
    return m_numFoldsS;
  }

  public String preserveOrderTipText() {
    return "Preserve the order of the training data "
      + "(rather than randomly shuffling) before creating folds";
  }

  @OptionMetadata(displayName = "Preserve instances order",
    description = "Preserve the order of instances rather than randomly shuffling",
    displayOrder = 1)
  public void setPreserveOrder(boolean preserve) {
    m_preserveOrder = preserve;
  }

  public boolean getPreserveOrder() {
    return m_preserveOrder;
  }

  @OptionMetadata(displayName = "Random seed",
    description = "The random seed to use for shuffling", displayOrder = 3)
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

    String folds = getStepManager().environmentSubstitute(getNumFolds());
    try {
      m_numFolds = Integer.parseInt(folds);
    } catch (NumberFormatException e) {
      getStepManager()
        .logWarning("Unable to parse number of folds value: " + folds);
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
    dataSet = new Instances(dataSet);
    getStepManager().logBasic("Creating cross-validation folds");
    getStepManager().statusMessage("Creating cross-validation folds");

    Random random = new Random(m_seed);
    if (!getPreserveOrder()) {
      dataSet.randomize(random);
    }
    if (dataSet.classIndex() >= 0
      && dataSet.attribute(dataSet.classIndex()).isNominal()
      && !getPreserveOrder()) {
      getStepManager().logBasic("Stratifying data");
      dataSet.stratify(m_numFolds);
    }

    for (int i = 0; i < m_numFolds; i++) {
      if (isStopRequested()) {
        break;
      }
      Instances train =
        (!m_preserveOrder) ? dataSet.trainCV(m_numFolds, i, random)
          : dataSet.trainCV(m_numFolds, i);
      Instances test = dataSet.testCV(m_numFolds, i);

      Data trainData = new Data(StepManager.CON_TRAININGSET);
      trainData.setPayloadElement(StepManager.CON_TRAININGSET, train);
      trainData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, i + 1);
      trainData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
        m_numFolds);
      Data testData = new Data(StepManager.CON_TESTSET);
      testData.setPayloadElement(StepManager.CON_TESTSET, test);
      testData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, i + 1);
      testData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
        m_numFolds);
      if (!isStopRequested()) {
        getStepManager().outputData(trainData, testData);
      }
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
    if (strucForTrainingCon != null) {
      return strucForTrainingCon;
    }

    return null;
  }
}
