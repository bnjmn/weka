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
 *    ClassAssigner.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Attribute;
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
 * Knowledge Flow step for assigning a class attribute in incoming data
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "ClassAssigner", category = "Evaluation",
  toolTipText = "Designate which column is to be considered the class column "
    + "in incoming data.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "ClassAssigner.gif")
public class ClassAssigner extends BaseStep {

  private static final long serialVersionUID = -4269063233834866140L;

  /** Holds resoved class column/index */
  protected String m_classColumnS = "/last";

  /** Holds user-specified class column/index */
  protected String m_classCol = "/last";

  /** True if the class has already been assigned */
  protected boolean m_classAssigned;

  /** True if processing an instance stream */
  protected boolean m_isInstanceStream;

  /** Counter used for streams */
  protected int m_streamCount;

  /**
   * Set the class column to use
   *
   * @param col the class column to use
   */
  public void setClassColumn(String col) {
    m_classColumnS = col;
  }

  /**
   * Get the class column to use
   *
   * @return the class column to use
   */
  public String getClassColumn() {
    return m_classColumnS;
  }

  /**
   * Initialize the step prior to execution
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    if (m_classColumnS == null || m_classColumnS.length() == 0) {
      throw new WekaException("No class column specified!");
    }

    m_classCol = getStepManager().environmentSubstitute(m_classColumnS).trim();
    m_classAssigned = false;
    m_isInstanceStream = false;
    m_streamCount = 0;
  }

  /**
   * Process incoming data
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    Object payload = data.getPayloadElement(data.getConnectionName());
    if (!m_classAssigned) {
      if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
        m_isInstanceStream = true;
        Instance inst = (Instance) payload;
        if (inst != null) {
          assignClass(inst.dataset());
        }
      } else {
        getStepManager().processing();
        if (payload == null) {
          throw new WekaException("Incoming data is null!");
        }
        payload = new Instances((Instances)payload);
        assignClass((Instances) payload);
        data = new Data(data.getConnectionName(), payload);
      }
      m_streamCount++;
      m_classAssigned = m_streamCount == 2;
    }

    if (isStopRequested()) {
      if (!m_isInstanceStream) {
        getStepManager().interrupted();
      }
      return;
    }

    if (m_isInstanceStream) {
      if (!getStepManager().isStreamFinished(data)) {
        getStepManager().throughputUpdateStart();
      } else {
        getStepManager().throughputFinished(new Data(data.getConnectionName()));
        return;
      }
      getStepManager().throughputUpdateEnd();
    }

    getStepManager().outputData(data.getConnectionName(), data);

    if (!m_isInstanceStream || payload == null) {
      if (!isStopRequested()) {
        getStepManager().finished();
      } else {
        getStepManager().interrupted();
      }
    }
  }

  /**
   * Assign the class to a set of instances
   *
   * @param dataSet the instances to assign the class to
   * @throws WekaException if a problem occurs
   */
  protected void assignClass(Instances dataSet) throws WekaException {
    Attribute classAtt = dataSet.attribute(m_classCol);
    boolean assigned = false;
    if (classAtt != null) {
      dataSet.setClass(classAtt);
      assigned = true;
    } else {
      if (m_classCol.equalsIgnoreCase("last")
        || m_classCol.equalsIgnoreCase("/last")) {
        dataSet.setClassIndex(dataSet.numAttributes() - 1);
        assigned = true;
      } else if (m_classCol.equalsIgnoreCase("first")
        || m_classCol.equalsIgnoreCase("/first")) {
        dataSet.setClassIndex(0);
        assigned = true;
      } else {
        // try parsing as an index
        try {
          int classIndex = Integer.parseInt(m_classCol);
          classIndex--;
          if (classIndex >= 0 && classIndex < dataSet.numAttributes()) {
            dataSet.setClassIndex(classIndex);
            assigned = true;
          }
        } catch (NumberFormatException ex) {
        }
      }
    }

    if (!assigned) {
      throw new WekaException(
        "Unable to assign '" + m_classCol + "' as the class.");
    }
    getStepManager()
      .logBasic("Assigned '" + dataSet.classAttribute().name() + "' as class.");
  }

  /**
   * Get the incoming connections that this step can accept at this time
   *
   * @return a list of incoming connection types that can be accepted
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
        StepManager.CON_TESTSET, StepManager.CON_INSTANCE);
    }

    return new ArrayList<String>();
  }

  /**
   * Get the outgoing connection types that this step can produce at this time
   *
   * @return a list of outgoing connections that can be produced
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {
      result.add(StepManager.CON_INSTANCE);
    } else if (getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_DATASET) > 0) {
      result.add(StepManager.CON_DATASET);
    } else if (getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_TRAININGSET) > 0) {
      result.add(StepManager.CON_TRAININGSET);
    } else if (getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_TESTSET) > 0) {
      result.add(StepManager.CON_TESTSET);
    }

    return result;
  }

  /**
   * Return the structure of data output by this step for a given incoming
   * connection type
   *
   * @param connectionName the incoming connection type
   * @return the structure (header-only instances) of the output
   * @throws WekaException if a problem occurs
   */
  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    m_classCol = getStepManager().environmentSubstitute(m_classColumnS).trim();
    if (!(connectionName.equals(StepManager.CON_DATASET)
      || connectionName.equals(StepManager.CON_TRAININGSET)
      || connectionName.equals(StepManager.CON_TESTSET)
      || connectionName.equals(StepManager.CON_INSTANCE))
      || getStepManager().numIncomingConnections() == 0) {
      return null;
    }

    // our output structure is the same as whatever kind of input we are getting
    Instances strucForDatasetCon = getStepManager()
      .getIncomingStructureForConnectionType(StepManager.CON_DATASET);
    if (strucForDatasetCon != null) {
      assignClass(strucForDatasetCon);
      return strucForDatasetCon;
    }

    Instances strucForTestsetCon = getStepManager()
      .getIncomingStructureForConnectionType(StepManager.CON_TESTSET);
    if (strucForTestsetCon != null) {
      assignClass(strucForTestsetCon);
      return strucForTestsetCon;
    }

    Instances strucForTrainingCon = getStepManager()
      .getIncomingStructureForConnectionType(StepManager.CON_TRAININGSET);
    if (strucForTrainingCon != null) {
      assignClass(strucForTrainingCon);
      return strucForTrainingCon;
    }

    Instances strucForInstanceCon = getStepManager()
      .getIncomingStructureForConnectionType(StepManager.CON_INSTANCE);
    if (strucForInstanceCon != null) {
      assignClass(strucForInstanceCon);
      return strucForInstanceCon;
    }

    return null;
  }

  /**
   * Get the custom editor for this step
   *
   * @return the fully qualified class name of the clustom editor for this
   * step
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.ClassAssignerStepEditorDialog";
  }
}
