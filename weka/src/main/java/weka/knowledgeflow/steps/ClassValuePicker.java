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
 *    ClassValuePicker.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.SwapValues;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Step that allows the selection of the class label that is to be considered as
 * the "positive" class when computing threshold curves.
 *
 * @author Mark Hall
 */
@KFStep(name = "ClassValuePicker", category = "Evaluation",
  toolTipText = "Designate which class value is considered the \"positive\" "
    + "class value (useful for ROC analysis)",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "ClassValuePicker.gif")
public class ClassValuePicker extends BaseStep {

  /** For serialization */
  private static final long serialVersionUID = 8558445535347028472L;

  /**
   * User specified class label, label index or special identifier (e.g
   * "first"/"last")
   */
  protected String m_classValueS = "/first";

  /** Class label after environment variables have been resolved */
  protected String m_classValue = "/first";

  /** True if the class is set in the incoming data */
  protected boolean m_classIsSet;

  /** True if the class is set and is nominal */
  protected boolean m_classIsNominal;

  /**
   * Set the class value considered to be the "positive" class value.
   * 
   * @param value the class value index to use
   */
  @OptionMetadata(displayName = "Class value",
    description = "The class value to consider as the 'positive' class",
    displayOrder = 1)
  public void setClassValue(String value) {
    m_classValueS = value;
  }

  /**
   * Gets the class value considered to be the "positive" class value.
   * 
   * @return the class value index
   */
  public String getClassValue() {
    return m_classValueS;
  }

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  @Override
  public void stepInit() throws WekaException {
    m_classIsSet = true;
    m_classIsNominal = true;

    m_classValue = getStepManager().environmentSubstitute(m_classValueS).trim();
    if (m_classValue.length() == 0) {
      throw new WekaException("No class label specified as the positive class!");
    }
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the payload to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    Instances dataSet =
      (Instances) data.getPayloadElement(data.getConnectionName());
    if (dataSet == null) {
      throw new WekaException("Data should not be null!");
    }

    if (dataSet.classAttribute() == null) {
      getStepManager().logWarning("No class attribute set in the data");
      m_classIsSet = false;
    }

    if (m_classIsSet && dataSet.classAttribute().isNumeric()) {
      getStepManager().logWarning("Class is numeric");
      m_classIsNominal = false;
    }

    Instances newDataSet = dataSet;
    if (m_classIsSet && m_classIsNominal) {
      newDataSet = assignClassValue(dataSet);
    }

    Data newData = new Data(data.getConnectionName());
    newData.setPayloadElement(data.getConnectionName(), newDataSet);
    getStepManager().outputData(newData);

    getStepManager().finished();
  }

  /**
   * Set the class value to be considered the 'positive' class
   *
   * @param dataSet the dataset to assign the class value for
   * @return the altered dataset
   * @throws WekaException if a problem occurs
   */
  protected Instances assignClassValue(Instances dataSet) throws WekaException {

    Attribute classAtt = dataSet.classAttribute();
    int classValueIndex = classAtt.indexOfValue(m_classValue);

    if (classValueIndex == -1) {
      if (m_classValue.equalsIgnoreCase("last")
        || m_classValue.equalsIgnoreCase("/last")) {
        classValueIndex = classAtt.numValues() - 1;
      } else if (m_classValue.equalsIgnoreCase("first")
        || m_classValue.equalsIgnoreCase("/first")) {
        classValueIndex = 0;
      } else {
        // try to parse as a number
        String clV = m_classValue;
        if (m_classValue.startsWith("/") && m_classValue.length() > 1) {
          clV = clV.substring(1);
        }
        try {
          classValueIndex = Integer.parseInt(clV);
          classValueIndex--; // zero-based
        } catch (NumberFormatException ex) {
        }
      }
    }
    if (classValueIndex < 0 || classValueIndex > classAtt.numValues() - 1) {
      throw new WekaException("Class label/index '" + m_classValue
        + "' is unknown or out of range!");
    }

    if (classValueIndex != 0) {
      try {
        SwapValues sv = new SwapValues();
        sv.setAttributeIndex("" + (dataSet.classIndex() + 1));
        sv.setFirstValueIndex("first");
        sv.setSecondValueIndex("" + (classValueIndex + 1));
        sv.setInputFormat(dataSet);
        Instances newDataSet = Filter.useFilter(dataSet, sv);
        newDataSet.setRelationName(dataSet.relationName());

        getStepManager().logBasic(
          "New class value: " + newDataSet.classAttribute().value(0));
        return newDataSet;
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }

    return dataSet;
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
    if (getStepManager().numIncomingConnections() > 0) {
      return new ArrayList<String>();
    }
    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_TESTSET);
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
    List<String> result = new ArrayList<String>();

    if (getStepManager().numIncomingConnectionsOfType(StepManager.CON_DATASET) > 0) {
      result.add(StepManager.CON_DATASET);
    } else if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_TRAININGSET) > 0) {
      result.add(StepManager.CON_TRAININGSET);
    } else if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_TESTSET) > 0) {
      result.add(StepManager.CON_TESTSET);
    }

    return result;
  }

  /**
   * If possible, get the output structure for the named connection type as a
   * header-only set of instances. Can return null if the specified connection
   * type is not representable as Instances or cannot be determined at present.
   *
   * @param connectionName the name of the connection type to get the output
   *          structure for
   * @return the output structure as a header-only Instances object
   * @throws WekaException if a problem occurs
   */
  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    m_classValue = getStepManager().environmentSubstitute(m_classValueS).trim();
    if (!(connectionName.equals(StepManager.CON_DATASET)
      || connectionName.equals(StepManager.CON_TRAININGSET)
      || connectionName.equals(StepManager.CON_TESTSET) || connectionName
        .equals(StepManager.CON_INSTANCE))
      || getStepManager().numIncomingConnections() == 0) {
      return null;
    }

    // our output structure is the same as whatever kind of input we are getting
    Instances strucForDatasetCon =
      getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_DATASET);
    if (strucForDatasetCon != null) {
      // assignClass(strucForDatasetCon);
      return strucForDatasetCon;
    }

    Instances strucForTestsetCon =
      getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_TESTSET);
    if (strucForTestsetCon != null) {
      // assignClass(strucForTestsetCon);
      return strucForTestsetCon;
    }

    Instances strucForTrainingCon =
      getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_TRAININGSET);
    if (strucForTrainingCon != null) {
      // assignClass(strucForTrainingCon);
      return strucForTrainingCon;
    }

    Instances strucForInstanceCon =
      getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_INSTANCE);
    if (strucForInstanceCon != null) {
      // assignClass(strucForInstanceCon);
      return strucForInstanceCon;
    }

    return null;
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
    return "weka.gui.knowledgeflow.steps.ClassValuePickerStepEditorDialog";
  }
}
