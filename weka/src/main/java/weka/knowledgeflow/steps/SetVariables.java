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

import weka.core.Attribute;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.JobEnvironment;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Step that can be used to set the values of environment variables for the flow
 * being executed. Can be useful when testing flows that use environment
 * variables (that would typically have values set appropriately at runtime in a
 * production setting). This step is special in the sense the the Knowledge Flow
 * checks for it and invokes its stepInit() method (thus setting variables)
 * before initializing all other steps in the flow. It can also be used to set
 * 'dynamic' variables based on the values of attributes in incoming instances.
 * Dynamic variables are not guaranteed to be available to other steps in the
 * same flow at runtime. Instead, they are meant to be used by a directly
 * connected (via 'variables' connection) 'Job' step, which will execute a
 * specified sub-flow for each 'variables' data object received.
 * 
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "SetVariables",
  category = "Flow",
  toolTipText = "Assign default values for static variables, if not already set, and "
    + "for dynamic variables. Static variables are guaranteed to be available to "
    + "all other steps at initialization as the Knowledge Flow makes sure that "
    + "SetVariables is invoked first first. Dynamic variables can have their "
    + "values set using the values of attributes from incoming instances. Dynamic "
    + "variables are *not* guaranteed to be available to other steps in the flow - "
    + "instead, they are intended for use by a directly connected 'Job' step, which "
    + "will execute a specified sub-flow for each 'variables' data object received.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "SetVariables.gif")
public class SetVariables extends BaseStep {

  /** Separators for internal variable specification */
  public static final String SEP1 = "@@vv@@";
  public static final String SEP2 = "@v@v";
  public static final String SEP3 = "@a@a";

  private static final long serialVersionUID = 8042350408846800738L;

  /** Holds static variables in internal representation */
  protected String m_internalRep = "";

  /** Holds dynamic variables in internal representation */
  protected String m_dynamicInternalRep = "";

  /** Map of variables to set with fixed values */
  protected Map<String, String> m_varsToSet =
    new LinkedHashMap<String, String>();

  /**
   * Map of variables to set based on the values of attributes in incoming
   * instances
   */
  protected Map<String, List<String>> m_varsToSetFromIncomingInstances =
    new LinkedHashMap<>();

  /**
   * OK if there is at least one specified attribute in the incoming instance
   * structure
   */
  protected boolean m_structureOK;

  /** True if the structure has been checked */
  protected boolean m_structureCheckComplete;

  /**
   * True if an exception should be raised when an attribute value being used to
   * set a variable is missing, instead of using a default value
   */
  protected boolean m_raiseErrorWhenValueMissing;

  /**
   * Set the static variables to set (in internal representation)
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

  @ProgrammaticProperty
  public void setDynamicVarsInternalRep(String rep) {
    m_dynamicInternalRep = rep;
  }

  public String getDynamicVarsInternalRep() {
    return m_dynamicInternalRep;
  }

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  @Override
  public void stepInit() throws WekaException {
    m_structureCheckComplete = false;
    m_structureOK = false;
    m_varsToSet = internalToMap(m_internalRep);
    m_varsToSetFromIncomingInstances =
      internalDynamicToMap(m_dynamicInternalRep);

    Environment currentEnv =
      getStepManager().getExecutionEnvironment().getEnvironmentVariables();
    if (currentEnv == null) {
      throw new WekaException(
        "The execution environment doesn't seem to have any support for variables");
    }

    if (!(currentEnv instanceof JobEnvironment)) {
      currentEnv = new JobEnvironment(currentEnv);
      getStepManager().getExecutionEnvironment().setEnvironmentVariables(
        currentEnv);
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

    if (getStepManager().numIncomingConnections() > 0
      && m_varsToSetFromIncomingInstances.size() == 0) {
      getStepManager().logWarning(
        "Incoming data detected, but no variables to set from incoming "
          + "instances have been defined");
    }
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    if (!m_structureCheckComplete) {
      m_structureCheckComplete = true;

      Instances structure = null;
      if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
        structure = ((Instance) data.getPrimaryPayload()).dataset();
      } else if (data.getConnectionName().equals(StepManager.CON_ENVIRONMENT)) {
        structure =
          ((Instance) data.getPayloadElement(StepManager.CON_AUX_DATA_INSTANCE))
            .dataset();
      } else {
        structure = data.getPrimaryPayload();
      }

      checkStructure(structure);
    }

    getStepManager().processing();

    if (data.getConnectionName().equals(StepManager.CON_INSTANCE)
      || data.getConnectionName().equals(StepManager.CON_ENVIRONMENT)) {
      if (isStopRequested()) {
        getStepManager().interrupted();
        return;
      }
      if (getStepManager().isStreamFinished(data)) {
        Data finished = new Data(StepManager.CON_ENVIRONMENT);
        if (data.getConnectionName().equals(StepManager.CON_ENVIRONMENT)) {
          finished
            .setPayloadElement(
              StepManager.CON_AUX_DATA_ENVIRONMENT_VARIABLES,
              data
                .getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_VARIABLES));
          finished
            .setPayloadElement(
              StepManager.CON_AUX_DATA_ENVIRONMENT_PROPERTIES,
              data
                .getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_PROPERTIES));
        }
        getStepManager().throughputFinished(finished);
        return;
      }
      Instance toProcess =
        (Instance) (data.getConnectionName().equals(StepManager.CON_INSTANCE) ? data
          .getPrimaryPayload() : data
          .getPayloadElement(StepManager.CON_AUX_DATA_INSTANCE));
      getStepManager().throughputUpdateStart();
      processInstance(toProcess,
        data.getConnectionName().equals(StepManager.CON_ENVIRONMENT) ? data
          : null);
      getStepManager().throughputUpdateEnd();
    } else {
      Instances insts = data.getPrimaryPayload();
      for (int i = 0; i < insts.numInstances(); i++) {
        if (isStopRequested()) {
          break;
        }
        processInstance(insts.instance(i), null);
        Data finished = new Data(StepManager.CON_ENVIRONMENT);
        getStepManager().throughputFinished(finished);
      }
      if (isStopRequested()) {
        getStepManager().interrupted();
      }
    }
  }

  protected void processInstance(Instance inst, Data existingEnv)
    throws WekaException {
    Map<String, String> vars = new HashMap<>();
    for (Map.Entry<String, List<String>> e : m_varsToSetFromIncomingInstances
      .entrySet()) {
      String attName = environmentSubstitute(e.getKey());
      Attribute current = inst.dataset().attribute(attName);
      int index = -1;
      if (current != null) {
        index = current.index();
      } else {
        // try as a 1-based index
        try {
          index = Integer.parseInt(attName);
          index--; // make zero-based
        } catch (NumberFormatException ex) {
          // ignore
        }
      }
      if (index != -1) {
        String varToSet = environmentSubstitute(e.getValue().get(0));
        String val = environmentSubstitute(e.getValue().get(1));

        if (inst.isMissing(index)) {
          if (m_raiseErrorWhenValueMissing) {
            throw new WekaException("Value of attribute '"
              + inst.attribute(index).name()
              + "' was missing in current instance");
          }
        } else {
          val = inst.stringValue(index);
        }
        vars.put(varToSet, val);
      }
    }

    Environment env =
      getStepManager().getExecutionEnvironment().getEnvironmentVariables();
    for (Map.Entry<String, String> e : vars.entrySet()) {
      env.addVariable(e.getKey(), e.getValue());
    }

    if (existingEnv != null) {
      Map<String, String> existingVars =
        existingEnv
          .getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_VARIABLES);
      if (existingVars != null) {
        vars.putAll(existingVars);
      }
    }

    Data output = new Data(StepManager.CON_ENVIRONMENT);
    output.setPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_VARIABLES,
      vars);

    if (existingEnv != null) {
      output.setPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_PROPERTIES,
        existingEnv
          .getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_PROPERTIES));
    }

    // make sure that each data output is in the same thread
    output.setPayloadElement(StepManager.CON_AUX_DATA_INSTANCE, inst);
    output.setPayloadElement(StepManager.CON_AUX_DATA_IS_INCREMENTAL, true);
    getStepManager().outputData(output);
  }

  protected void checkStructure(Instances structure) {
    List<String> notFoundInIncoming = new ArrayList<>();
    for (String attName : m_varsToSetFromIncomingInstances.keySet()) {
      if (structure.attribute(attName) == null) {
        notFoundInIncoming.add(attName);
      } else {
        m_structureOK = true;
      }
    }

    if (notFoundInIncoming.size() == m_varsToSetFromIncomingInstances.size()) {
      getStepManager().logWarning(
        "None of the specified attributes appear to be "
          + "in the incoming instance structure");
      return;
    }

    for (String s : notFoundInIncoming) {
      getStepManager().logWarning(
        "Attribute '" + s + "' was not found in the "
          + "incoming instance structure");
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
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET,
        StepManager.CON_INSTANCE, StepManager.CON_ENVIRONMENT);
    }

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
    if (getStepManager().numIncomingConnections() != 0) {
      return Arrays.asList(StepManager.CON_ENVIRONMENT);
    }

    return new ArrayList<String>();
  }

  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {
    if (getStepManager().numIncomingConnections() == 0
      || (!connectionName.equals(StepManager.CON_DATASET)
        && !connectionName.equals(StepManager.CON_TRAININGSET) && !connectionName
          .equals(StepManager.CON_TESTSET))
      && !connectionName.equals(StepManager.CON_INSTANCE)
      && !connectionName.equals(StepManager.CON_ENVIRONMENT)) {
      return null;
    }

    // our output structure is the same as whatever kind of input we are getting
    return getStepManager().getIncomingStructureForConnectionType(
      connectionName);
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

  public static Map<String, List<String>> internalDynamicToMap(
    String internalRep) {
    Map<String, List<String>> varsToSet = new LinkedHashMap<>();
    if (internalRep != null && internalRep.length() > 0) {
      String[] parts = internalRep.split(SEP1);
      for (String p : parts) {
        String[] attVal = p.split(SEP3);
        if (attVal.length == 2) {
          String attName = attVal[0].trim();
          String[] varDefault = attVal[1].trim().split(SEP2);
          String varName = varDefault[0].trim();
          String defaultV = "";
          if (varDefault.length == 2) {
            defaultV = varDefault[1].trim();
          }
          List<String> varAndDefL = new ArrayList<>();
          varAndDefL.add(varName);
          varAndDefL.add(defaultV);
          varsToSet.put(attName, varAndDefL);
        }
      }
    }

    return varsToSet;
  }

  /**
   * Convert a string in the internal static variable representation to a map of
   * variables + values
   *
   * @param internalRep the variables in internal represenation
   * @return a map of variables + values
   */
  public static Map<String, String> internalToMap(String internalRep) {
    Map<String, String> varsToSet = new LinkedHashMap<String, String>();
    if (internalRep != null && internalRep.length() > 0) {
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
