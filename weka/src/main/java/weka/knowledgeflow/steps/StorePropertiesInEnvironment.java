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
 *    StorePropertiesInEnvironment.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

/**
 * Stores property values specified in incoming instances in the flow
 * environment.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "StorePropertiesInEnvironment",
  category = "Flow",
  toolTipText = "Store property settings for a particular algorithm-based step "
    + "(eg Classifier, Clusterer etc) in the flow environment. When connected "
    + "to a downstream Job step, the sub-flow executed by the Job can use a "
    + "SetPropertiesFromEnvironment step to access the stored properties and "
    + "set them on the underlying scheme in an algorithm-based step. Each property "
    + "is configured by specifying the attribute in the incoming instance to obtain "
    + "its value from, the target scheme-based step (in the sub-flow) that will "
    + "receive it, the property name/path to set on the target step and a default "
    + "property value (optional) to use if the value is missing in the incoming "
    + "instance. If the property/path field is left blank, then it is assumed that "
    + "the value is actually a scheme + options spec in command-line form; otherwise, "
    + "the value is set by processing the property path - e.g. if our target step "
    + "to receive property settings was Bagging (itself with default settings), and "
    + "the property path to set was 'classifier.maxDepth', then the classifier property "
    + "of Bagging would yield a REPTree base classifier and the maxDepth property of "
    + "REPTree would be set. Note that the SetPropertiesFromEnvironment step will "
    + "process property settings in the order that they are defined by this step. This "
    + "means that it is possible to set the entire base learner for a Classifier step"
    + "with one property setting and then drill down to a particular option in the "
    + "base learner using a second property setting.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "StorePropertiesInEnvironment.gif")
public class StorePropertiesInEnvironment extends BaseStep {
  private static final long serialVersionUID = -1526289154505863542L;

  /** Separators for internal variable specification */
  public static final String SEP1 = "@@vv@@";
  public static final String SEP2 = "@a@a";

  /**
   * Map of properties to set based on the values of attributes in incoming
   * instances. Keyed by attribute name/index. List contains target step name,
   * property path (can be empty string to indicate a command line spec for a
   * complete base-scheme config), default property value. If an incoming
   * attribute value is missing, and no default property value is available, an
   * exception will be generated.
   */
  protected Map<String, List<String>> m_propsToSetFromIncomingInstances =
    new LinkedHashMap<>();

  /** True if the structure has been checked */
  protected boolean m_structureCheckComplete;

  /**
   * OK if there is at least one specified attribute in the incoming instance
   * structure
   */
  protected boolean m_structureOK;

  /** Internal string-based representation of property configs */
  protected String m_internalRep = "";

  protected boolean m_raiseErrorWhenValueMissing;

  @ProgrammaticProperty
  public void setPropsInternalRep(String rep) {
    m_internalRep = rep;
  }

  public String getPropsInternalRep() {
    return m_internalRep;
  }

  @Override
  public void stepInit() throws WekaException {
    m_structureCheckComplete = false;
    m_structureOK = false;
    m_propsToSetFromIncomingInstances = internalDynamicToMap(m_internalRep);

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

    if (getStepManager().numIncomingConnections() > 0
      && m_propsToSetFromIncomingInstances.size() == 0) {
      getStepManager().logWarning(
        "Incoming data detected, but no properties to "
          + "set from incoming instances have been defined.");
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
    Map<String, Map<String, String>> props = new HashMap<>();

    for (Map.Entry<String, List<String>> e : m_propsToSetFromIncomingInstances
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
        String stepName = environmentSubstitute(e.getValue().get(0));
        String propToSet = environmentSubstitute(e.getValue().get(1));
        String val = environmentSubstitute(e.getValue().get(2));

        if (inst.isMissing(index)) {
          if (val.length() == 0 && m_raiseErrorWhenValueMissing) {
            throw new WekaException("Value of attribute '"
              + inst.attribute(index).name()
              + "' was missing in current instance and no default value has "
              + "been specified");
          }
        } else {
          val = inst.stringValue(index);
        }
        Map<String, String> propsForStep = props.get(stepName);
        if (propsForStep == null) {
          propsForStep = new LinkedHashMap<>();
          props.put(stepName, propsForStep);
        }
        propsForStep.put(propToSet, val);
        getStepManager().logDebug(
          "Storing property '" + propToSet + "' for step " + "'" + stepName
            + "' with value '" + val + "'");
      }
    }

    JobEnvironment env =
      (JobEnvironment) getStepManager().getExecutionEnvironment()
        .getEnvironmentVariables();
    env.addToStepProperties(props);

    if (existingEnv != null) {
      Map<String, Map<String, String>> existingProps =
        existingEnv
          .getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_PROPERTIES);
      if (existingProps != null) {
        props.putAll(existingProps);
      }
    }
    Data output = new Data(StepManager.CON_ENVIRONMENT);
    output.setPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_PROPERTIES,
      props);
    if (existingEnv != null) {
      output.setPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_VARIABLES,
        existingEnv
          .getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_VARIABLES));
    }
    output.setPayloadElement(StepManager.CON_AUX_DATA_INSTANCE, inst);
    output.setPayloadElement(StepManager.CON_AUX_DATA_IS_INCREMENTAL, true);
    getStepManager().outputData(output);
  }

  protected void checkStructure(Instances structure) {
    List<String> notFoundInIncoming = new ArrayList<>();
    for (String attName : m_propsToSetFromIncomingInstances.keySet()) {
      if (structure.attribute(attName) == null) {
        notFoundInIncoming.add(attName);
      } else {
        m_structureOK = true;
      }
    }

    if (notFoundInIncoming.size() == m_propsToSetFromIncomingInstances.size()) {
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

  @Override
  public List<String> getIncomingConnectionTypes() {

    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET,
        StepManager.CON_INSTANCE, StepManager.CON_ENVIRONMENT);
    }

    return new ArrayList<>();
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    if (getStepManager().numIncomingConnections() != 0) {
      return Arrays.asList(StepManager.CON_ENVIRONMENT);
    }

    return new ArrayList<>();
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
    return "weka.gui.knowledgeflow.steps.StorePropertiesInEnvironmentStepEditorDialog";
  }

  public static Map<String, List<String>> internalDynamicToMap(
    String internalRep) {
    Map<String, List<String>> propsToSet = new LinkedHashMap<>();
    if (internalRep != null && internalRep.length() > 0) {
      String[] parts = internalRep.split(SEP1);
      for (String p : parts) {
        String[] attVal = p.split(SEP2);
        if (attVal.length == 4) {
          String attName = attVal[0].trim();
          String stepName = attVal[1].trim();
          String propName = attVal[2].trim();
          String defVal = attVal[3].trim();

          if (attName.length() > 0 && stepName.length() > 0) {
            List<String> stepAndDefL = new ArrayList<>();
            stepAndDefL.add(stepName);
            stepAndDefL.add(propName);
            stepAndDefL.add(defVal);
            propsToSet.put(attName, stepAndDefL);
          }
        }
      }
    }

    return propsToSet;
  }
}
