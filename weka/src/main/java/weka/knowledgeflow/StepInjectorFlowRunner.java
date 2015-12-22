package weka.knowledgeflow;

import weka.core.WekaException;
import weka.knowledgeflow.steps.Step;

import java.util.List;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class StepInjectorFlowRunner extends FlowRunner {

  protected boolean m_reset = true;
  protected boolean m_streaming;

  public void reset() {
    m_reset = true;
    m_streaming = false;
  }

  public void injectWithExecutionFinishedCallback(final Data toInject,
    ExecutionFinishedCallback callback, final Step target)
      throws WekaException {

    if (StepManagerImpl.connectionIsIncremental(toInject)) {
      throw new WekaException(
        "Only batch data can be injected via this method.");
    }

    setExecutionFinishedCallback(callback);

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

  public Step findStep(String stepName, Class stepClass) throws WekaException {
    if (m_flow == null) {
      throw new WekaException("No flow set!");
    }

    StepManagerImpl manager = m_flow.findStep(stepName);
    if (manager == null) {
      throw new WekaException(
        "Step '" + stepName + "' does not seem " + "to be part of the flow!");
    }

    Step target = manager.getManagedStep();
    if (target.getClass() != stepClass) {
      throw new WekaException("Step '" + stepName + "' is not an instance of "
        + stepClass.getCanonicalName());
    }

    if (target.getIncomingConnectionTypes() == null
      || target.getIncomingConnectionTypes().size() == 0) {
      throw new WekaException(
        "Step '" + stepName + "' cannot process any incoming data!");
    }

    return target;
  }

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
