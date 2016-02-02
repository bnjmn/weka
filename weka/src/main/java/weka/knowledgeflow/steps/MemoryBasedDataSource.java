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
 *    MemoryBasedDataSource.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.Arrays;
import java.util.List;

/**
 * Simple start step that stores a set of instances and outputs it in a
 * dataSet connection. Gets used programmatically when the setInstances()
 * method is invoked on the MainKFPerspective in order to create a new
 * Flow containing this step.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "MemoryDataSource", category = "DataSources",
  toolTipText = "Memory-based data", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "DefaultDataSource.gif")
public class MemoryBasedDataSource extends BaseStep {
  private static final long serialVersionUID = -1901014330145130275L;

  /** The data that will be output from this step */
  protected Instances m_instances;

  /**
   * Set the data to output from this step
   *
   * @param instances
   */
  public void setInstances(Instances instances) {
    m_instances = instances;
  }

  /**
   * Get the data to output from this step
   *
   * @return
   */
  public Instances getInstances() {
    return m_instances;
  }

  /**
   * Initialize the step
   *
   * @throws WekaException if the data to output has not been set yet
   */
  @Override
  public void stepInit() throws WekaException {
    if (m_instances == null) {
      throw new WekaException(
        "Has not been initialized with a set of instances");
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
    return null;
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
    return Arrays.asList(StepManager.CON_DATASET);
  }

  /**
   * Start processing
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void start() throws WekaException {
    getStepManager().processing();
    Data output = new Data(StepManager.CON_DATASET, m_instances);
    getStepManager().outputData(output);
    getStepManager().finished();
  }
}
