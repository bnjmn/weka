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

  protected Instances m_instances;

  public void setInstances(Instances instances) {
    m_instances = instances;
  }

  public Instances getInstances() {
    return m_instances;
  }

  @Override
  public void stepInit() throws WekaException {
    if (m_instances == null) {
      throw new WekaException(
        "Has not been initialized with a set of instances");
    }
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    return null;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return Arrays.asList(StepManager.CON_DATASET);
  }

  @Override
  public void start() throws WekaException {
    getStepManager().processing();
    Data output = new Data(StepManager.CON_DATASET, m_instances);
    getStepManager().outputData(output);
    getStepManager().finished();
  }
}
