package weka.knowledgeflow.steps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import weka.core.Environment;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.gui.knowledgeflow.StepVisual;

@KFStep(
  name = "SetVariables",
  category = "Flow",
  toolTipText = "Assign default values for variables if they are not already set",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DiamondPlain.gif")
public class SetVariables extends BaseStep {

  protected static final String SEP1 = "@@**@@";
  protected static final String SEP2 = "@*@*";

  protected String m_internalRep = "";
  protected Map<String, String> m_varsToSet =
    new LinkedHashMap<String, String>();

  @ProgrammaticProperty
  public void setVarsInternalRep(String rep) {
    m_varsToSet.clear();
    m_internalRep = rep;
  }

  public String getVarsInternalRep() {
    return m_internalRep;
  }

  @Override
  public void stepInit() throws WekaException {
    if (m_internalRep == null || m_internalRep.length() == 0) {
      return;
    }
    String[] parts = m_internalRep.split(SEP1);
    for (String p : parts) {
      String[] keyVal = p.trim().split(SEP2);
      if (keyVal.length == 2) {
        m_varsToSet.put(keyVal[0].trim(), keyVal[1]);
      }
    }

    Environment currentEnv =
      getStepManager().getExecutionEnvironment().getEnvironmentVariables();
    if (currentEnv == null) {
      throw new WekaException(
        "The execution environment doesn't seem to have any support for variables");
    }

    for (Map.Entry<String, String> e : m_varsToSet.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();

      if (currentEnv.getVariableValue(key) == null) {
        currentEnv.addVariable(key, value);
      }
    }
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    return new ArrayList<String>();
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return new ArrayList<String>();
  }
}
