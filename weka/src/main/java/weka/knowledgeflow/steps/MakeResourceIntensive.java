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
 *    MakeResourceIntensive.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * A Step that makes downstream steps that are directly connected to this step
 * resource intensive (or not). This overrides whatever the downstream step may
 * (or may not) have declared in it's {@code KFStep} class annotation with
 * regards to whether it is resource intensive (cpu or memory). The Knowledge
 * Flow execution environment uses two executor services - a primary one to
 * execute batch processing for steps; and a secondary one for executing
 * {@code StepTask}s (which are assumed to be resource intensive by default) or
 * for executing batch processing for a Step when it declares itself to be
 * resource intensive. This secondary executor service uses a limited (typically
 * {@code <= num cpu cores}) number of threads. Steps that involve potentially
 * intensive (cpu/memory) processing should declare themselves resource
 * intensive so that less taxing steps (and the UI) get cpu cycles. E.g. the
 * Classifier Step is resource intensive so that processing cross-validation
 * folds in parallel for a large data set or computationally intensive
 * classifier does not blow out memory or bog the system down.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "MakeResourceIntensive",
  category = "Flow",
  toolTipText = "Makes downstream connected steps resource intensive (or not)."
    + " This shifts "
    + "processing of such steps between the main step executor<br>"
    + "service and the high resource executor service or vice versa.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DiamondPlain.gif")
public class MakeResourceIntensive extends BaseStep {

  private static final long serialVersionUID = -5670771681991035130L;

  /** True if downstream steps are to be made resource intensive */
  protected boolean m_setAsResourceIntensive = true;

  /**
   * Set whether downstream steps are to be made resource intensive or not
   * 
   * @param resourceIntensive true if the downstream connected steps are to be
   *          made resource intensive
   */
  @OptionMetadata(
    displayName = "Make downstream step(s) high resource",
    description = "<html>Makes downstream connected "
      + "steps resource intensive (or not)<br>This shifts processing of such steps "
      + "between the main step executor service and the high resource executor "
      + "service or vice versa.</html>")
  public
    void setMakeResourceIntensive(boolean resourceIntensive) {
    m_setAsResourceIntensive = resourceIntensive;
  }

  /**
   * Get whether downstream steps are to be made resource intensive
   * 
   * @return true if downstream connected steps are to be made resource
   *         intensive
   */
  public boolean getMakeResourceIntensive() {
    return m_setAsResourceIntensive;
  }

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {

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
    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_TESTSET, StepManager.CON_BATCH_CLASSIFIER,
      StepManager.CON_BATCH_CLUSTERER, StepManager.CON_BATCH_ASSOCIATOR);
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
    Set<String> inConnTypes =
      getStepManager().getIncomingConnections().keySet();
    return new ArrayList<String>(inConnTypes);
  }

  /**
   * Process incoming data
   *
   * @param data the data to process
   * @throws WekaException
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    String connType = data.getConnectionName();
    List<StepManager> connected =
      getStepManager().getOutgoingConnectedStepsOfConnectionType(connType);

    for (StepManager m : connected) {
      getStepManager().logDetailed(
        "Setting " + m.getName() + " as resource intensive: "
          + m_setAsResourceIntensive);
      ((StepManagerImpl) m)
        .setStepIsResourceIntensive(m_setAsResourceIntensive);
    }
    getStepManager().outputData(data);
  }
}
