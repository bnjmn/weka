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
 *    SetVariables.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

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

/**
 * Step that can be used to set the values of environment variables for the flow
 * being executed. Can be useful when testing flows that use environment
 * variables (that would typically have values set appropriately at runtime in a
 * production setting). This step is special in the sense the the Knowledge Flow
 * checks for it and invokes its stepInit() method (thus setting variables)
 * before initializing all other steps in the flow. This step does not accept
 * any connections, nor does it produce any data. Just placing one in your flow
 * is sufficient to ensure that variables get initialized.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "SetVariables", category = "Flow",
  toolTipText = "Assign default values for variables if they are not already set",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DiamondPlain.gif")
public class SetVariables extends BaseStep {

  protected static final String SEP1 = "@@**@@";
  protected static final String SEP2 = "@*@*";

  private static final long serialVersionUID = 8042350408846800738L;

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
