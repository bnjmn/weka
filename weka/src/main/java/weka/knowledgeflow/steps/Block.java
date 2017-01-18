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
 *    Block.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * A step that waits for a specified step to finish processing before allowing
 * incoming data to proceed downstream.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "Block", category = "Flow",
  toolTipText = "Block until a specific step has finished procesing",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DiamondPlain.gif")
public class Block extends BaseStep {

  private static final long serialVersionUID = 3204082191908877620L;

  /** The name of the step to wait for */
  protected String m_stepToWaitFor = "";

  /** The {@code StepManager} of the step to wait for */
  protected transient StepManager m_smForStep;

  /**
   * Set the step to wait for
   *
   * @param stepToWaitFor the step to wait for
   */
  @OptionMetadata(displayName = "Wait until this step has completed",
    description = "This step will prevent data from passing downstream until "
      + "the specified step has finished processing")
  public void setStepToWaitFor(String stepToWaitFor) {
    m_stepToWaitFor = stepToWaitFor;
  }

  /**
   * Get the step to wait for
   *
   * @return the step to wait for
   */
  public String getStepToWaitFor() {
    return m_stepToWaitFor;
  }

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    if (m_stepToWaitFor == null || m_stepToWaitFor.length() == 0) {
      getStepManager().logWarning(
        "No step to wait for specified - will not block");
    }

    m_smForStep =
      getStepManager().findStepInFlow(environmentSubstitute(m_stepToWaitFor));

    if (m_smForStep == getStepManager()) {
      // don't block on our self!!
      throw new WekaException("Blocking on oneself will cause deadlock!");
    }

    if (m_smForStep == null) {
      throw new WekaException("Step '" + environmentSubstitute(m_stepToWaitFor)
        + "' does not seem " + "to exist in the flow!");
    }
  }

  /**
   * Process incoming data
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    if (m_smForStep == null) {
      // just pass data through
      getStepManager().outputData(data);
    } else {
      getStepManager().processing();
      getStepManager().logBasic(
        "Waiting for step '" + environmentSubstitute(m_stepToWaitFor) + "'");
      getStepManager().statusMessage(
        "Waiting for step '" + environmentSubstitute(m_stepToWaitFor) + "'");
      while (!m_smForStep.isStepFinished()) {
        if (isStopRequested()) {
          break;
        }
        try {
          Thread.sleep(300);
        } catch (InterruptedException e) {
          getStepManager().interrupted();
          return;
        }
      }
      getStepManager().logBasic("Releasing data");
      getStepManager().statusMessage("Releasing data");

    }

    if (isStopRequested()) {
      getStepManager().interrupted();
    } else {
      getStepManager().outputData(data);
      getStepManager().finished();
    }
  }

  /**
   * Get a list of incoming connection types that this step can accept at this
   * time
   *
   * @return a list of incoming connection types
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_INSTANCE, StepManager.CON_TESTSET,
      StepManager.CON_BATCH_CLASSIFIER, StepManager.CON_BATCH_CLUSTERER,
      StepManager.CON_BATCH_ASSOCIATOR, StepManager.CON_TEXT);
  }

  /**
   * Get a list of outgoing connection types that this step can produce at this
   * time
   *
   * @return a list of outgoing connection types
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    Set<String> inConnTypes =
      getStepManager().getIncomingConnections().keySet();
    return new ArrayList<String>(inConnTypes);
  }

  /**
   * Get the fully qualified class name of the custom editor for this step
   *
   * @return the class name of the custom editor
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.BlockStepEditorDialog";
  }
}
