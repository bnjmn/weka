package weka.knowledgeflow.steps;

import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.StepManager;

import java.util.Arrays;
import java.util.List;

@KFStep(name = "Dummy", category = "Misc",
  toolTipText = "A step that is the equivalent of /dev/null",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DiamondPlain.gif")
public class Dummy extends BaseStep {

  private static final long serialVersionUID = -5822675617001689385L;

  @Override
  public void stepInit() throws WekaException {
    // nothing to do
  }

  @Override
  public List<String> getIncomingConnectionTypes() {

    // TODO add all connection types here...
    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_TESTSET, StepManager.CON_INSTANCE);
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return null;
  }
}
