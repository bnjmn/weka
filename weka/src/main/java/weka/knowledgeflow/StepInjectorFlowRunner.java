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
 *    StepInjectorFlowRunner
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.WekaException;
import weka.knowledgeflow.steps.Step;

import java.util.List;

/**
 * A flow runner that runs a flow by injecting data into a target step
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class StepInjectorFlowRunner extends FlowRunner {

  /** True if the flow has been reset */
  protected boolean m_reset = true;

  /** True if data is streaming */
  protected boolean m_streaming;

  /**
   * Rest the runner
   */
  public void reset() {
    m_reset = true;
    m_streaming = false;
  }

  /**
   * Inject data into the flow
   *
   * @param toInject the data to inject
   * @param callback a {@code ExecutionFinishedCallback} to notify when
   *          execution completes
   * @param target the target {@code Step} to inject to
   * @throws WekaException if a problem occurs
   */
  public void injectWithExecutionFinishedCallback(final Data toInject,
    ExecutionFinishedCallback callback, final Step target) throws WekaException {

    if (StepManagerImpl.connectionIsIncremental(toInject)) {
      throw new WekaException(
        "Only batch data can be injected via this method.");
    }

    addExecutionFinishedCallback(callback);

    String connName = toInject.getConnectionName();
    List<String> accceptableInputs = target.getIncomingConnectionTypes();
    if (!accceptableInputs.contains(connName)) {
      throw new WekaException("Step '" + target.getName() + "' can't accept a "
        + connName + " input at present!");
    }

    initializeFlow();
    m_execEnv.submitTask(new StepTask<Void>(null) {
      /** For serialization */
      private static final long serialVersionUID = 663985401825979869L;

      @Override
      public void process() throws Exception {
        target.processIncoming(toInject);
      }
    });
    m_logHandler.logDebug("StepInjectorFlowRunner: Launching shutdown monitor");
    launchExecutorShutdownThread();
  }

  /**
   * Find a step in the flow
   * 
   * @param stepName the name of the Step to find
   * @param stepClass the class of the step to find
   * @return the named step
   * @throws WekaException if the named step is not in the flow or the found
   *           step is not of the supplied class
   */
  public Step findStep(String stepName, Class stepClass) throws WekaException {
    if (m_flow == null) {
      throw new WekaException("No flow set!");
    }

    StepManagerImpl manager = m_flow.findStep(stepName);
    if (manager == null) {
      throw new WekaException("Step '" + stepName + "' does not seem "
        + "to be part of the flow!");
    }

    Step target = manager.getManagedStep();
    if (target.getClass() != stepClass) {
      throw new WekaException("Step '" + stepName + "' is not an instance of "
        + stepClass.getCanonicalName());
    }

    if (target.getIncomingConnectionTypes() == null
      || target.getIncomingConnectionTypes().size() == 0) {
      throw new WekaException("Step '" + stepName
        + "' cannot process any incoming data!");
    }

    return target;
  }

  /**
   * Inject streaming data into the target step in the flow
   *
   * @param toInject a streaming {@code Data} object to inject
   * @param target the target step to inject to
   * @param lastData true if this is the last piece of data in the stream
   * @throws WekaException if a problem occurs
   */
  public void injectStreaming(Data toInject, Step target, boolean lastData)
    throws WekaException {
    if (m_reset) {
      if (m_streaming) {
        m_execEnv.stopClientExecutionService();
      }

      String connName = toInject.getConnectionName();
      List<String> accceptableInputs = target.getIncomingConnectionTypes();
      if (!accceptableInputs.contains(connName)) {
        throw new WekaException("Step '" + target.getName()
          + "' can't accept a " + connName + " input at present!");
      }

      initializeFlow();
      toInject.setPayloadElement(
        StepManager.CON_AUX_DATA_INCREMENTAL_STREAM_END, false);
      m_streaming = true;
      m_reset = false;
    }

    if (lastData) {
      toInject.setPayloadElement(
        StepManager.CON_AUX_DATA_INCREMENTAL_STREAM_END, true);
    }

    target.processIncoming(toInject);
    if (lastData) {
      m_logHandler
        .logDebug("StepInjectorFlowRunner: Shutting down executor service");
      m_execEnv.stopClientExecutionService();
      reset();
    }
  }
}
