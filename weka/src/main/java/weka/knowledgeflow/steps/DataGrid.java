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
 *    DataGrid.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializedObject;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.StreamThroughput;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A step that allows the user to define instances to output
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "DataGrid", category = "DataSources",
  toolTipText = "Specify a grid of data to turn into instances",
  iconPath = StepVisual.BASE_ICON_PATH + "ArffLoader.gif")
public class DataGrid extends BaseStep {

  private static final long serialVersionUID = 1318159328875458847L;

  /** The instances to output (as a string) */
  protected String m_data = "";

  /** Reusable data object for streaming output */
  protected Data m_incrementalData;

  /** For overall stream throughput measuring */
  protected StreamThroughput m_flowThroughput;

  /**
   * Set the data to be output by this {@code DataGrid} in textual ARFF format.
   *
   * @param data the data to be output in textual ARFF format
   */
  @ProgrammaticProperty
  public void setData(String data) {
    m_data = data;
  }

  /**
   * Get the data to be output by this {@code DataGrid} in textual ARFF format
   *
   * @return the data to be output in textual ARFF format
   */
  public String getData() {
    return m_data;
  }

  /**
   * Initialize the step;
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {
      m_incrementalData = new Data(StepManager.CON_INSTANCE);
    } else {
      m_incrementalData = null;
      m_flowThroughput = null;
    }
  }

  /**
   * Start processing
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void start() throws WekaException {
    if (getStepManager().numOutgoingConnections() > 0) {
      if (m_data.length() == 0) {
        getStepManager().logWarning("No data to output!");
      } else {
        try {
          // make instances
          String data = environmentSubstitute(m_data);
          Instances toOutput = new Instances(new StringReader(data));
          if (getStepManager().numOutgoingConnectionsOfType(
            StepManager.CON_DATASET) > 0) {
            getStepManager().processing();
            Data batch = new Data(StepManager.CON_DATASET, toOutput);
            getStepManager().outputData(batch);
            getStepManager().finished();
          } else {
            // streaming case
            String stm =
              getName() + "$" + hashCode() + 99
                + "| overall flow throughput -|";
            m_flowThroughput =
              new StreamThroughput(stm, "Starting flow...",
                ((StepManagerImpl) getStepManager()).getLog());
            Instances structure = toOutput.stringFreeStructure();
            Instances structureCopy = null;
            Instances currentStructure = structure;
            boolean containsStrings = toOutput.checkForStringAttributes();
            if (containsStrings) {
              structureCopy =
                (Instances) (new SerializedObject(structure).getObject());
            }

            if (isStopRequested()) {
              getStepManager().interrupted();
              return;
            }

            for (int i = 0; i < toOutput.numInstances(); i++) {
              if (isStopRequested()) {
                break;
              }
              Instance nextInst = toOutput.instance(i);
              m_flowThroughput.updateStart();
              getStepManager().throughputUpdateStart();
              if (containsStrings) {
                if (currentStructure == structure) {
                  currentStructure = structureCopy;
                } else {
                  currentStructure = structure;
                }

                for (int j = 0; j < toOutput.numAttributes(); j++) {
                  if (toOutput.attribute(j).isString()) {
                    if (!nextInst.isMissing(j)) {
                      currentStructure.attribute(j).setStringValue(
                        nextInst.stringValue(j));
                      nextInst.setValue(j, 0);
                    }
                  }
                }
              }

              nextInst.setDataset(currentStructure);
              m_incrementalData.setPayloadElement(StepManager.CON_INSTANCE,
                nextInst);
              getStepManager().throughputUpdateEnd();
              getStepManager().outputData(m_incrementalData);
              m_flowThroughput.updateEnd(((StepManagerImpl) getStepManager())
                .getLog());
            }

            if (isStopRequested()) {
              ((StepManagerImpl) getStepManager()).getLog().statusMessage(
                stm + "remove");
              getStepManager().interrupted();
              return;
            }
            m_flowThroughput.finished(((StepManagerImpl) getStepManager())
              .getLog());

            // signal end of input
            m_incrementalData.clearPayload();
            getStepManager().throughputFinished(m_incrementalData);
          }
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
      }
    } else {
      getStepManager().logWarning("No connected outputs");
    }
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
    if (getStepManager().isStepBusy()) {
      return null;
    }

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_DATASET) == 0
      && getStepManager()
        .numOutgoingConnectionsOfType(StepManager.CON_INSTANCE) == 0) {
      return null;
    }

    try {
      return new Instances(new StringReader(m_data)).stringFreeStructure();
    } catch (IOException e) {
      throw new WekaException(e);
    }
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    return null;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numOutgoingConnections() == 0) {
      result.add(StepManager.CON_DATASET);
      result.add(StepManager.CON_INSTANCE);
    } else if (getStepManager().numOutgoingConnectionsOfType(
      StepManager.CON_DATASET) > 0) {
      result.add(StepManager.CON_DATASET);
    } else {
      result.add(StepManager.CON_INSTANCE);
    }

    return result;
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
    return "weka.gui.knowledgeflow.steps.DataGridStepEditorDialog";
  }
}
