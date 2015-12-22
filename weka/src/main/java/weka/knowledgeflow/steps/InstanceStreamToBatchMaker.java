package weka.knowledgeflow.steps;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Step that converts an incoming instance stream to a batch dataset
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "InstanceStreamToBatchMaker", category = "Flow",
  toolTipText = "Converts an incoming instance stream into a batch dataset",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "InstanceStreamToBatchMaker.gif")
public class InstanceStreamToBatchMaker extends BaseStep {

  private static final long serialVersionUID = 5461324282251111320L;
  /** True if we've been reset */
  protected boolean m_isReset;

  protected Instances m_structure;
  protected List<Instance> m_batch;

  @Override
  public void stepInit() throws WekaException {
    m_batch = new ArrayList<Instance>();
    m_isReset = true;
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_INSTANCE);
    }
    return null;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    if (getStepManager().numIncomingConnections() > 0) {
      return Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET);
    }

    return null;
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    if (m_isReset) {
      m_isReset = false;
      if (data.getPrimaryPayload() == null) {
        throw new WekaException("We didn't receive any instances!");
      }
      getStepManager().logDetailed("Collecting instances...");
      Instance temp = data.getPrimaryPayload();
      m_structure = new Instances(temp.dataset(), 0);
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
      return;
    }

    if (!getStepManager().isStreamFinished(data)) {
      getStepManager().throughputUpdateStart();
      Instance inst = data.getPrimaryPayload();
      m_batch.add(inst);
      getStepManager().throughputUpdateEnd();
    } else {
      // output batch
      Instances toOutput = new Instances(m_structure, m_batch.size());
      for (Instance i : m_batch) {
        toOutput.add(i);
      }
      toOutput.compactify();

      // save memory
      m_batch.clear();
      getStepManager().logBasic(
        "Emitting a batch of " + toOutput.numInstances() + " instances.");
      List<String> outCons =
        new ArrayList<String>(getStepManager().getOutgoingConnections()
          .keySet());
      Data out = new Data(outCons.get(0), toOutput);
      out.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
      out.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
      if (!isStopRequested()) {
        getStepManager().outputData(out);
        getStepManager().finished();
      } else {
        getStepManager().interrupted();
      }
    }
  }

  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    if (getStepManager().numIncomingConnections() > 0) {
      // we don't alter the structure of the incoming data
      return getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_INSTANCE);
    }

    return null;
  }
}
