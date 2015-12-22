package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@KFStep(name = "TestSetMaker", category = "Evaluation",
  toolTipText = "Make an incoming dataSet or trainingSet into a testSet",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "TestSetMaker.gif")
public class TestSetMaker extends BaseStep {

  private static final long serialVersionUID = 6384920860783839811L;

  @Override
  public void stepInit() {
    // nothing to do
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    String incomingConnName = data.getConnectionName();
    Instances insts = (Instances) data.getPayloadElement(incomingConnName);
    if (insts == null) {
      throw new WekaException("Incoming instances should not be null!");
    }

    getStepManager().logBasic(
      "Creating a test set for relation " + insts.relationName());
    Data newData = new Data();
    newData.setPayloadElement(StepManager.CON_TESTSET, insts);
    newData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
    newData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
    if (!isStopRequested()) {
      getStepManager().outputData(StepManager.CON_TESTSET, newData);
    }
    getStepManager().finished();
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays
        .asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET);
    }

    return new ArrayList<String>();
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    if (getStepManager().numIncomingConnections() > 0) {
      return Arrays.asList(StepManager.CON_TESTSET);
    }
    return new ArrayList<String>();
  }

  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {
    if (!connectionName.equals(StepManager.CON_TESTSET)
      || getStepManager().numIncomingConnections() == 0) {
      return null;
    }

    Instances strucForDatasetCon =
      getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_DATASET);
    if (strucForDatasetCon != null) {
      return strucForDatasetCon;
    }

    Instances strucForTrainingSetCon =
      getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_TRAININGSET);
    if (strucForTrainingSetCon != null) {
      return strucForTrainingSetCon;
    }

    return null;
  }
}
