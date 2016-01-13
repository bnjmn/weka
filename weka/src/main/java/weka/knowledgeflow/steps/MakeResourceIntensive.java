package weka.knowledgeflow.steps;

import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "MakeResourceIntensive", category = "Flow",
  toolTipText = "<html>Makes downstream connected steps resource intensive (or not).<br>"
    + " This shifts "
    + "processing of such steps between the main step executor<br>"
    + "service and the high resource executor service or vice versa.</html>",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DiamondPlain.gif")
public class MakeResourceIntensive extends BaseStep {

  private static final long serialVersionUID = -5670771681991035130L;

  /** True if downstream steps are to be made resource intensive */
  protected boolean m_setAsResourceIntensive = true;

  /**
   * Set whether downstream steps are to be made resource intensive or not
   * 
   * @param resourceIntensive true if the downstream connected steps are to be
   *          made resource intensive
   */
  @OptionMetadata(displayName = "Make downstream step(s) high resource",
    description = "<html>Makes downstream connected "
      + "steps resource intensive (or not)<br>This shifts processing of such steps "
      + "between the main step executor service and the high resource executor "
      + "service or vice versa.</html>")
  public void setMakeResourceIntensive(boolean resourceIntensive) {
    m_setAsResourceIntensive = resourceIntensive;
  }

  /**
   * Get whether downstream steps are to be made resource intensive
   * 
   * @return true if downstream connected steps are to be made resource
   *         intensive
   */
  public boolean getMakeResourceIntensive() {
    return m_setAsResourceIntensive;
  }

  @Override
  public void stepInit() throws WekaException {

  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_TESTSET, StepManager.CON_BATCH_CLASSIFIER,
      StepManager.CON_BATCH_CLUSTERER, StepManager.CON_BATCH_ASSOCIATOR);
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    Set<String> inConnTypes =
      getStepManager().getIncomingConnections().keySet();
    return new ArrayList<String>(inConnTypes);
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    String connType = data.getConnectionName();
    List<StepManager> connected =
      getStepManager().getOutgoingConnectedStepsOfConnectionType(connType);

    for (StepManager m : connected) {
      getStepManager().logDetailed("Setting " + m.getName()
        + " as resource intensive: " + m_setAsResourceIntensive);
      ((StepManagerImpl) m)
        .setStepIsResourceIntensive(m_setAsResourceIntensive);
    }
    getStepManager().outputData(data);
  }
}
