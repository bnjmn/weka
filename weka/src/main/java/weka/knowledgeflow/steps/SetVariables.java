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

import weka.core.Environment;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.KFGUIConsts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

  /** Separators for internal variable specification */
  public static final String SEP1 = "@@vv@@";
  public static final String SEP2 = "@v@v";

  private static final long serialVersionUID = 8042350408846800738L;

  /** Holds variables in internal representation */
  protected String m_internalRep = "";

  /** Map of variables to set */
  protected Map<String, String> m_varsToSet =
    new LinkedHashMap<String, String>();

  /**
   * Set the variables to set (in internal representation)
   *
   * @param rep the variables to set
   */
  @ProgrammaticProperty
  public void setVarsInternalRep(String rep) {
    m_internalRep = rep;
  }

  /**
   * Get the variables to set (in internal representation)
   *
   * @return the variables to set
   */
  public String getVarsInternalRep() {
    return m_internalRep;
  }

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  @Override
  public void stepInit() throws WekaException {

    m_varsToSet = internalToMap(m_internalRep);

    Environment currentEnv =
      getStepManager().getExecutionEnvironment().getEnvironmentVariables();
    if (currentEnv == null) {
      throw new WekaException(
        "The execution environment doesn't seem to have any support for variables");
    }

    for (Map.Entry<String, String> e : m_varsToSet.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();

      if (key != null && key.length() > 0 && value != null
        && currentEnv.getVariableValue(key) == null) {
        getStepManager()
          .logDetailed("Setting variable: " + key + " = " + value);
        currentEnv.addVariable(key, value);
      }
    }
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
    return new ArrayList<String>();
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
    return new ArrayList<String>();
  }

  /**
   * Return the fully qualified name of a custom editor component (JComponent)
   * to use for editing the properties of the step. This method can return null,
   * in which case the system will dynamically generate an editor using the
   * GenericObjectEditor
   *
   * @return the fully qualified name of a step editor component
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.SetVariablesStepEditorDialog";
  }

  /**
   * Convert a string in the internal variable representation to a map
   * of variables + values
   *
   * @param internalRep the variables in internal represenation
   * @return a map of variables + values
   */
  public static Map<String, String> internalToMap(String internalRep) {
    Map<String, String> varsToSet = new LinkedHashMap<String, String>();
    if (internalRep != null || internalRep.length() > 0) {
      String[] parts = internalRep.split(SEP1);
      for (String p : parts) {
        String[] keyVal = p.trim().split(SEP2);
        if (keyVal.length == 2) {
          varsToSet.put(keyVal[0].trim(), keyVal[1]);
        }
      }
    }

    return varsToSet;
  }
}
