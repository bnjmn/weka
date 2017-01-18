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
 *    StepManagerImpl.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.Environment;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Settings;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.Logger;
import weka.gui.beans.StreamThroughput;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.steps.KFStep;
import weka.knowledgeflow.steps.Step;
import weka.knowledgeflow.steps.WekaAlgorithmWrapper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete implementation of the StepManager interface. Has a number of
 * methods, beyond those aimed at Step implementations, that are useful for
 * applications that manipulate Steps and their connections.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class StepManagerImpl implements StepManager {

  /** The step being managed by this step manager */
  protected Step m_managedStep;

  /**
   * True if the runtime environment has requested that the managed step stop
   * processing
   */
  protected boolean m_stopRequested;

  /** True if, at the current time, the managed step is busy with processing */
  protected boolean m_stepIsBusy;

  /** True if the step is finished with processing (as far as it can tell) */
  protected boolean m_stepIsFinished;

  /**
   * Set and get arbitrary properties relating to this step/step manager. E.g. a
   * plugin execution environment might allow a step to be marked as execute
   * remotely or locally
   */
  protected Map<String, Object> m_stepProperties =
    new HashMap<String, Object>();

  /**
   * Holds the name of the class of the editor for the managed step. If
   * null/empty then the environment will dynamically generate an editor using
   * the GeneicObjectEditor
   */
  protected String m_managedStepEditor;

  /** Map of incoming connections, keyed by connection name */
  protected Map<String, List<StepManager>> m_connectedByTypeIncoming =
    new LinkedHashMap<String, List<StepManager>>();

  /** Map of outgoing connections, keyed by connection name */
  protected Map<String, List<StepManager>> m_connectedByTypeOutgoing =
    new LinkedHashMap<String, List<StepManager>>();

  /** Non-step parties interested in outgoing data */
  protected Map<String, List<StepOutputListener>> m_outputListeners =
    new LinkedHashMap<String, List<StepOutputListener>>();

  /**
   * The StepVisual for this step (non-null if existing in a GUI environment)
   */
  protected StepVisual m_stepVisual;

  /**
   * Temporary holder for the x axis visual location of this step. Populated
   * when a flow is loaded if coordinates are present in the step's JSON
   * representation. If running in an interactive graphical environment then a
   * StepVisual will be created and initialized with these values
   */
  protected int m_x = -1;

  /**
   * Temporary holder for the y axis visual location of this step. Populated
   * when a flow is loaded if coordinates are present in the step's JSON
   * representation. If running in an interactive graphical environment then a
   * StepVisual will be created and initialized with these values
   */
  protected int m_y = -1;

  /**
   * Holds the executing environment. Will be able to query to see if
   * environment is headless. Will be able to request a stop of the entire flow.
   */
  protected BaseExecutionEnvironment m_executionEnvironment;

  /** The log to use */
  protected LogManager m_log;

  /** For measuring performance of instance streams */
  protected transient StreamThroughput m_throughput;

  /**
   * Used when interrogating the managed step for what output connections it can
   * produce at present given the incoming connections to the step. Normally, a
   * step decides what it can produce on the basis of what physical incoming
   * connections are present, regardless of whether the connection may or may
   * not produce data (e.g. if there is a broken link in the graph further
   * upstream). When this flag is true, the routine adjusts the number of
   * incoming connections of a given type to account for broken upstream links.
   * This is primarily used by the graphical UI in order to change connections
   * from red to grey when rendering.
   */
  protected boolean m_adjustForGraphicalRendering;

  /** True if the managed step is a resource (cpu/memory) intensive step */
  protected boolean m_stepIsResourceIntensive;

  /**
   * True if the managed step must run single threaded - i.e. in an executor
   * service with one worker thread
   */
  protected boolean m_stepMustRunSingleThreaded;

  /**
   * Constructor
   *
   * @param step the Step to manage
   */
  public StepManagerImpl(Step step) {
    setManagedStep(step);
  }

  /**
   * Get the name of the Step being managed
   *
   * @return the name of the Step being managed
   */
  @Override
  public String getName() {
    return m_managedStep.getName();
  }

  /**
   * Get the step managed by this manager
   *
   * @return the step managed by this manager
   */
  @Override
  public Step getManagedStep() {
    return m_managedStep;
  }

  /**
   * Set the step managed by this manager
   *
   * @param step the step to manage
   */
  public void setManagedStep(Step step) {
    m_managedStep = step;
    step.setStepManager(this);
    setManagedStepEditorClass(step.getCustomEditorForStep());

    Annotation a = step.getClass().getAnnotation(KFStep.class);
    m_stepIsResourceIntensive = a != null && ((KFStep) a).resourceIntensive();

    a = step.getClass().getAnnotation(SingleThreadedExecution.class);
    m_stepMustRunSingleThreaded = a != null;
  }

  /**
   * Set whether the managed step is resource (cpu/memory) intensive or not
   *
   * @param resourceIntensive true if the managed step is resource intensive
   */
  @Override
  public void setStepIsResourceIntensive(boolean resourceIntensive) {
    m_stepIsResourceIntensive = resourceIntensive;
  }

  /**
   * Get whether the managed step is resource (cpu/memory) intensive or not
   *
   * @return true if the step is resource intensive
   */
  @Override
  public boolean stepIsResourceIntensive() {
    return m_stepIsResourceIntensive;
  }

  /**
   * Set whether the managed step must run single-threaded. I.e. in an executor
   * service with one worker thread, thus effectively preventing more than one
   * copy of the step from executing at any one point in time
   *
   * @param mustRunSingleThreaded true if the managed step must run
   *          single-threaded
   */
  @Override
  public void setStepMustRunSingleThreaded(boolean mustRunSingleThreaded) {
    m_stepMustRunSingleThreaded = mustRunSingleThreaded;
  }

  /**
   * Get whether the managed step must run single-threaded. I.e. in an executor
   * service with one worker thread, thus effectively preventing more than one
   * copy of the step from executing at any one point in time
   *
   * @return true if the managed step must run single-threaded
   */
  @Override
  public boolean getStepMustRunSingleThreaded() {
    return m_stepMustRunSingleThreaded;
  }

  /**
   * Get the step visual in use (if running in a visual environment)
   *
   * @return the step visual in use
   */
  public StepVisual getStepVisual() {
    return m_stepVisual;
  }

  /**
   * Set the step visual to use when running in a graphical environment
   *
   * @param visual the step visual to use
   */
  public void setStepVisual(StepVisual visual) {
    m_stepVisual = visual;
    if (m_x != -1 && m_y != -1) {
      m_stepVisual.setX(m_x);
      m_stepVisual.setY(m_y);
    }
  }

  /**
   * Set a property for this step
   *
   * @param name the name of the property
   * @param value the value of the property
   */
  public void setStepProperty(String name, Object value) {
    m_stepProperties.put(name, value);
  }

  /**
   * Get a named property for this step.
   *
   * @param name the name of the property to get
   * @return the value of the property or null if the property is not set
   */
  public Object getStepProperty(String name) {
    return m_stepProperties.get(name);
  }

  /**
   * Get the fully qualified name of an editor component that can be used to
   * graphically configure the step. If not supplied, then the environment will
   * dynamically generate an editor using the GenericObjectEditor.
   *
   * @return editor the editor class to use
   */
  protected String getManagedStepEditorClass() {
    return m_managedStepEditor;
  }

  /**
   * Set the fully qualified name of an editor component that can be used to
   * graphically configure the step. If not supplied, then the environment will
   * dynamically generate an editor using the GenericObjectEditor.
   *
   * @param editor the editor class to use
   */
  protected void setManagedStepEditorClass(String editor) {
    m_managedStepEditor = editor;
  }

  /**
   * Get the execution environment the managed step is running in
   *
   * @return the execution environment
   */
  @Override
  public ExecutionEnvironment getExecutionEnvironment() {
    return m_executionEnvironment;
  }

  /**
   * Get the current knowledge flow settings
   *
   * @return the current knowledge flow settings
   * @throws IllegalStateException if there is no execution environment
   *           available
   */
  @Override
  public Settings getSettings() {
    if (getExecutionEnvironment() == null) {
      throw new IllegalStateException("There is no execution environment "
        + "available!");
    }
    return getExecutionEnvironment().getSettings();
  }

  /**
   * Set the execution environment the managed step is running in
   *
   * @param env the execution environment
   * @throws WekaException if a problem occurs
   */
  protected void setExecutionEnvironment(ExecutionEnvironment env)
    throws WekaException {

    if (!(env instanceof BaseExecutionEnvironment)) {
      throw new WekaException(
        "Execution environments need to be BaseExecutionEnvironment "
          + "(or subclass thereof)");
    }

    m_executionEnvironment = (BaseExecutionEnvironment) env;
    setLog(m_executionEnvironment.getLog());
    setLoggingLevel(m_executionEnvironment.getLoggingLevel());
  }

  /**
   * Get the logging level in use
   *
   * @return the logging level in use
   */
  @Override
  public LoggingLevel getLoggingLevel() {
    return m_log != null ? m_log.getLoggingLevel() : LoggingLevel.BASIC;
  }

  /**
   * Set the logging level to use
   *
   * @param newLevel the level to use
   */
  public void setLoggingLevel(LoggingLevel newLevel) {
    if (m_log == null) {
      m_log = new LogManager(getManagedStep());
    }
    m_log.setLoggingLevel(newLevel);
  }

  /**
   * Get the log to use
   *
   * @return the log in use or null if no log has been set
   */
  @Override
  public Logger getLog() {
    return m_log != null ? m_log.getLog() : null;
  }

  /**
   * Set the log to use
   *
   * @param log the log to use
   */
  public void setLog(Logger log) {
    m_log = new LogManager(getManagedStep());

    m_log.setLog(log);
  }

  /**
   * Initialize the step being managed
   *
   * @return true if the initialization was successful
   */
  protected boolean initStep() {
    boolean initializedOK = false;
    m_stepIsBusy = false;
    m_stopRequested = false;
    m_stepIsFinished = false;
    try {
      getManagedStep().stepInit();
      // getManagedStep().init();
      initializedOK = true;
    } catch (WekaException ex) {
      logError(ex.getMessage(), ex);
    } catch (Throwable ex) {
      logError(ex.getMessage(), ex);
    }

    m_throughput = null;

    return initializedOK;
  }

  /**
   * Returns true if, at the current time, the managed step is busy with
   * processing
   *
   * @return true if the managed step is busy with processing
   */
  @Override
  public boolean isStepBusy() {
    return m_stepIsBusy;
  }

  /**
   * Return true if a stop has been requested by the runtime environment
   *
   * @return true if a stop has been requested
   */
  @Override
  public boolean isStopRequested() {
    return m_stopRequested;
  }

  /**
   * Return true if the current step is finished.
   *
   * @return true if the current step is finished
   */
  @Override
  public boolean isStepFinished() {
    return m_stepIsFinished;
  }

  /**
   * Set the status of the stop requested flag
   *
   * @param stopRequested true if a stop has been requested
   */
  public void setStopRequested(boolean stopRequested) {
    m_stopRequested = stopRequested;
  }

  /**
   * Started processing. Sets the busy flag to true.
   */
  @Override
  public void processing() {
    m_stepIsBusy = true;
  }

  /**
   * Finished all processing. Sets the busy flag to false and prints a finished
   * message to the status area of the log.
   */
  @Override
  public void finished() {
    m_stepIsBusy = false;
    m_stepIsFinished = true;
    if (!isStopRequested()) {
      statusMessage("Finished.");
    }
  }

  /**
   * Finished processing due to a stop being requested. Sets the busy flag to
   * false.
   */
  @Override
  public void interrupted() {
    m_stepIsBusy = false;
  }

  /**
   * Returns true if this data object marks the end of an incremental stream.
   * Note - does not check that the data object is actually an incremental one
   * of some sort! Just checks to see if the CON_AUX_DATA_INCREMENTAL_STREAM_END
   * flag is set to true or not;
   *
   * @param data the data element to check
   * @return true if the data element is flagged as end of stream
   */
  @Override
  public boolean isStreamFinished(Data data) {
    return data.getPayloadElement(CON_AUX_DATA_INCREMENTAL_STREAM_END, false);
  }

  /**
   * Clients can use this to record a start point for streaming throughput
   * measuring
   */
  @Override
  public void throughputUpdateStart() {
    if (m_throughput == null) {
      m_throughput = new StreamThroughput(stepStatusMessagePrefix());
    }
    processing();
    m_throughput.updateStart();
  }

  /**
   * Clients can use this to record a stop point for streaming throughput
   * measuring
   */
  @Override
  public void throughputUpdateEnd() {
    if (m_throughput != null) {
      m_throughput.updateEnd(m_log.getLog());

      if (isStopRequested()) {
        finished();
      }
    }
  }

  /**
   * Clients can use this to indicate that throughput measuring is finished
   * (i.e. the stream being processed has ended). Final throughput information
   * is printed to the log and status
   *
   * @param data one or more Data events (with appropriate connection type set)
   *          to pass on to downstream connected steps. These are used to carry
   *          any final data and to inform the downstream step(s) that the
   *          stream has ended
   * @throws WekaException if a problem occurs
   */
  @Override
  public void throughputFinished(Data... data) throws WekaException {
    finished();
    if (data.length > 0) {
      for (Data d : data) {
        d.setPayloadElement(CON_AUX_DATA_INCREMENTAL_STREAM_END, true);
      }
      outputData(data);
    }
    if (m_throughput != null) {
      m_throughput.finished(m_log.getLog());
    }
    // not actually interrupted - we just abuse this method in order to
    // set the busy flag to false
    interrupted();
  }

  private void disconnectStep(List<StepManager> connList, Step toDisconnect) {
    Iterator<StepManager> iter = connList.iterator();
    while (iter.hasNext()) {
      StepManagerImpl candidate = (StepManagerImpl) iter.next();
      if (toDisconnect == candidate.getManagedStep()) {
        iter.remove();
        break;
      }
    }
  }

  /**
   * Disconnect the supplied step under the associated connection type from both
   * the incoming and outgoing connections for the step managed by this manager.
   * Does nothing if this step does not have any connections to the supplied
   * step, or does not have connections to the supplied step of the required
   * type.
   *
   * @param toDisconnect the step to disconnect
   * @param connType the connection type to disconnect
   */
  public void disconnectStepWithConnection(Step toDisconnect, String connType) {
    // incoming first
    List<StepManager> connectedWithType =
      m_connectedByTypeIncoming.get(connType);
    if (connectedWithType != null) {
      disconnectStep(connectedWithType, toDisconnect);
      if (connectedWithType.size() == 0) {
        m_connectedByTypeIncoming.remove(connType);
      }
    }

    // outgoing
    connectedWithType = m_connectedByTypeOutgoing.get(connType);
    if (connectedWithType != null) {
      disconnectStep(connectedWithType, toDisconnect);
      if (connectedWithType.size() == 0) {
        m_connectedByTypeOutgoing.remove(connType);
      }
    }
  }

  /**
   * Remove the supplied step from connections (both incoming and outgoing of
   * all types) for the step managed by this manager. Does nothing if the this
   * step does not have any connections to the supplied step
   *
   * @param toDisconnect the step to disconnect
   */
  public void disconnectStep(Step toDisconnect) {

    // incoming first
    List<String> emptyCons = new ArrayList<String>();
    for (Map.Entry<String, List<StepManager>> e : m_connectedByTypeIncoming
      .entrySet()) {
      // for (List<StepManager> sList : m_connectedByTypeIncoming.values()) {
      List<StepManager> sList = e.getValue();
      disconnectStep(sList, toDisconnect);
      if (sList.size() == 0) {
        emptyCons.add(e.getKey());
      }
    }
    for (String conn : emptyCons) {
      m_connectedByTypeIncoming.remove(conn);
    }
    emptyCons.clear();

    // outgoing
    for (Map.Entry<String, List<StepManager>> e : m_connectedByTypeOutgoing
      .entrySet()) {
      // for (List<StepManager> sList : m_connectedByTypeOutgoing.values()) {
      List<StepManager> sList = e.getValue();
      disconnectStep(sList, toDisconnect);
      if (sList.size() == 0) {
        emptyCons.add(e.getKey());
      }
    }
    for (String conn : emptyCons) {
      m_connectedByTypeOutgoing.remove(conn);
    }
  }

  /**
   * Clear all connections to/from the step managed by this manager. Also makes
   * sure that all directly connected upstream and downstream steps remove their
   * respective outgoing and incoming connections to this step
   */
  public void clearAllConnections() {
    m_connectedByTypeIncoming.clear();
    m_connectedByTypeOutgoing.clear();
  }

  /**
   * Add an incoming connection (comprising of the type of connection and
   * associated step component) to this step of the specified type
   *
   * @param connectionName the name of the type of connection to add
   * @param step the source step component that is connecting with given
   *          connection type
   */
  public void
    addIncomingConnection(String connectionName, StepManagerImpl step) {
    List<StepManager> steps = m_connectedByTypeIncoming.get(connectionName);
    if (steps == null) {
      steps = new ArrayList<StepManager>();
      m_connectedByTypeIncoming.put(connectionName, steps);
    }
    steps.add(step);
  }

  /**
   * Remove an incoming connection to this step of the specified type
   *
   * @param connectionName the name of the type of connection to remove
   * @param step the source step component associated with the given connection
   *          type
   */
  public void removeIncomingConnection(String connectionName,
    StepManagerImpl step) {
    List<StepManager> steps = m_connectedByTypeIncoming.get(connectionName);
    steps.remove(step);
  }

  /**
   * Add an outgoing connection (comprising of the type of connection and
   * associated target step) to this step of the specified type. Connection is
   * only made if the target step will accept the connection type at this time
   *
   * @param connectionName the name of the type of connection to add
   * @param step the target step component that is receiving the given
   *          connection type it can't accept the connection at the present time
   * @return true if the connection was successful
   */
  public boolean addOutgoingConnection(String connectionName,
    StepManagerImpl step) {
    return addOutgoingConnection(connectionName, step, false);
  }

  /**
   * Add an outgoing connection (comprising of the type of connection and
   * associated target step) to this step of the specified type. Connection is
   * only made if the target step will accept the connection type at this time
   *
   * @param connectionName the name of the type of connection to add
   * @param step the target step component that is receiving the given
   *          connection type
   * @param force whether to force the connection, even if the target step says
   *          it can't accept the connection at the present time
   * @return true if the connection was successful
   */
  public boolean addOutgoingConnection(String connectionName,
    StepManagerImpl step, boolean force) {

    // if target step can accept this connection type at this time then
    // create outgoing connection on this step and incoming connection
    // on the target step
    boolean connSuccessful = false;
    List<String> targetCanAccept =
      step.getManagedStep().getIncomingConnectionTypes();
    if (targetCanAccept.contains(connectionName) || force) {
      List<StepManager> steps = m_connectedByTypeOutgoing.get(connectionName);
      if (steps == null) {
        steps = new ArrayList<StepManager>();
        m_connectedByTypeOutgoing.put(connectionName, steps);
      }
      step.addIncomingConnection(connectionName, this);
      steps.add(step);
      connSuccessful = true;
    }
    return connSuccessful;
  }

  /**
   * Remove an outgoing connection from this step of the specified type
   *
   * @param connectionName the name of the type of connection to remove
   * @param step the target step component associated with the given connection
   *          type
   */
  public void removeOutgoingConnection(String connectionName,
    StepManagerImpl step) {
    List<StepManager> steps = m_connectedByTypeOutgoing.get(connectionName);
    steps.remove(step);

    // target step now loses an incoming connection
    step.removeIncomingConnection(connectionName, this);
  }

  /**
   * Get a list of steps providing incoming connections of the specified type
   *
   * @param connectionName the type of connection being received by this step
   * @return a list of connected steps
   */
  @Override
  public List<StepManager> getIncomingConnectedStepsOfConnectionType(
    String connectionName) {
    return m_connectedByTypeIncoming.get(connectionName) != null ? m_connectedByTypeIncoming
      .get(connectionName) : new ArrayList<StepManager>();
  }

  @Override
  public List<StepManager> getOutgoingConnectedStepsOfConnectionType(
    String connectionName) {
    return m_connectedByTypeOutgoing.get(connectionName) != null ? m_connectedByTypeOutgoing
      .get(connectionName) : new ArrayList<StepManager>();
  }

  private StepManager getConnectedStepWithName(String stepName,
    Map<String, List<StepManager>> connectedSteps) {
    StepManager result = null;

    for (Map.Entry<String, List<StepManager>> e : connectedSteps.entrySet()) {
      List<StepManager> stepsOfConnType = e.getValue();
      for (StepManager s : stepsOfConnType) {
        if (((StepManagerImpl) s).getManagedStep().getName().equals(stepName)) {
          result = s;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Get a named step connected to this step with an incoming connection
   *
   * @param stepName the name of the step to look for
   * @return the connected step
   */
  @Override
  public StepManager getIncomingConnectedStepWithName(String stepName) {
    return getConnectedStepWithName(stepName, m_connectedByTypeIncoming);
  }

  /**
   * Get a named step connected to this step with an outgoing connection
   *
   * @param stepName the name of the step to look for
   * @return the connected step
   */
  @Override
  public StepManager getOutgoingConnectedStepWithName(String stepName) {
    return getConnectedStepWithName(stepName, m_connectedByTypeOutgoing);
  }

  /**
   * Get the map of downstream (outgoing connections) connected steps
   *
   * @return the map of downstream connected steps
   */
  @Override
  public Map<String, List<StepManager>> getOutgoingConnections() {
    return m_connectedByTypeOutgoing;
  }

  /**
   * Get the man of upstream (incoming connections) connected steps
   *
   * @return the map of upstream connected steps
   */
  @Override
  public Map<String, List<StepManager>> getIncomingConnections() {
    return m_connectedByTypeIncoming;
  }

  /**
   * Register non-step third party to receive data from the managed step for the
   * specified outgoing connection type. Output listeners are not serialized
   * into the JSON flow when flows are saved.
   *
   * @param listener the output listener to register
   * @param outputConnectionName the name of the connection type
   */
  public void addStepOutputListener(StepOutputListener listener,
    String outputConnectionName) {
    List<StepOutputListener> listenersForConnectionType =
      m_outputListeners.get(outputConnectionName);
    if (listenersForConnectionType == null) {
      listenersForConnectionType = new ArrayList<StepOutputListener>();
      m_outputListeners.put(outputConnectionName, listenersForConnectionType);
    }

    if (!listenersForConnectionType.contains(listener)) {
      listenersForConnectionType.add(listener);
    }
  }

  /**
   * De-register non-step third party from receiving data from the managed step
   *
   * @param listener the output listener to de-register
   * @param outputConnectionName the name of the connection type the listener is
   *          registered against
   */
  public void removeStepOutputListener(StepOutputListener listener,
    String outputConnectionName) {
    List<StepOutputListener> listenersForConnectionType =
      m_outputListeners.get(outputConnectionName);

    if (listenersForConnectionType != null) {
      listenersForConnectionType.remove(listener);
    }
  }

  /**
   * Clear all registered StepOutputListeners
   */
  public void clearAllStepOutputListeners() {
    m_outputListeners.clear();
  }

  /**
   * Clear all the StepOutputListeners that are registered to receive the
   * supplied connection type.
   * 
   * @param outputConnectionName type of the connection to clear the listeners
   *          for
   */
  public void clearStepOutputListeners(String outputConnectionName) {
    List<StepOutputListener> listenersForConnectionType =
      m_outputListeners.get(outputConnectionName);

    if (listenersForConnectionType != null) {
      listenersForConnectionType.clear();
    }
  }

  /**
   * Pass any StepOutputListeners the supplied Data object
   * 
   * @param data the data to pass on
   */
  protected void notifyOutputListeners(Data data) throws WekaException {
    List<StepOutputListener> listenersForType =
      m_outputListeners.get(data.getConnectionName());
    if (listenersForType != null) {
      for (StepOutputListener l : listenersForType) {
        if (!l.dataFromStep(data)) {
          logWarning("StepOutputListener '" + l.getClass().getCanonicalName()
            + "' " + "did not process data '" + data.getConnectionName()
            + "' successfully'");
        }
      }
    }
  }

  /**
   * Output a Data object to all downstream connected Steps that are connected
   * with the supplied connection name. Sets the connection type on the supplied
   * Data object to the supplied connection name. Also notifies any registered
   * StepOutputListeners.
   * 
   * @param outgoingConnectionName the type of the outgoing connection to send
   *          data to
   * @param data a single Data object to send
   * @throws WekaException
   */
  @Override
  public void outputData(String outgoingConnectionName, Data data)
    throws WekaException {
    if (!isStopRequested()) {
      data.setConnectionName(outgoingConnectionName);
      data.setSourceStep(m_managedStep);

      List<StepManager> toNotify =
        m_connectedByTypeOutgoing.get(outgoingConnectionName);
      if (toNotify != null) {
        for (StepManager s : toNotify) {
          if (!isStopRequested()) {
            m_executionEnvironment.sendDataToStep((StepManagerImpl) s, data);
          }
        }
      }

      notifyOutputListeners(data);
    }
  }

  /**
   * Output one or more Data objects to all relevant steps. Populates the source
   * in each Data object for the client, HOWEVER, the client must have populated
   * the connection type in each Data object to be output so that the
   * StepManager knows which connected steps to send the data to. Also notifies
   * any registered {@code StepOutputListeners}. Note that the downstream
   * step(s)' processIncoming() method is called in a separate thread for batch
   * connections. Furthermore, if multiple Data objects are supplied via the
   * varargs argument, and a target step will receive more than one of the Data
   * objects, then they will be passed on to the step in question sequentially
   * within the same thread of execution.
   *
   * @param data one or more Data objects to be sent
   * @throws WekaException if a problem occurs
   */
  @Override
  public void outputData(Data... data) throws WekaException {
    if (!isStopRequested()) {
      Map<StepManagerImpl, List<Data>> stepsToSendTo =
        new LinkedHashMap<StepManagerImpl, List<Data>>();

      for (Data d : data) {
        d.setSourceStep(m_managedStep);
        if (d.getConnectionName() == null
          || d.getConnectionName().length() == 0) {
          throw new WekaException("Data does not have a connection name set.");
        }
        List<StepManager> candidates =
          m_connectedByTypeOutgoing.get(d.getConnectionName());
        if (candidates != null) {
          for (StepManager s : candidates) {
            List<Data> toReceive = stepsToSendTo.get(s);
            if (toReceive == null) {
              toReceive = new ArrayList<Data>();
              stepsToSendTo.put((StepManagerImpl) s, toReceive);
            }
            toReceive.add(d);
          }
        }

        notifyOutputListeners(d);
      }

      for (Map.Entry<StepManagerImpl, List<Data>> e : stepsToSendTo.entrySet()) {
        if (!e.getKey().isStopRequested()) {
          m_executionEnvironment.sendDataToStep(e.getKey(), e.getValue()
            .toArray(new Data[e.getValue().size()]));
        }
      }
    }
  }

  /**
   * Outputs the supplied Data object to the named Step. Does nothing if the
   * named step is not connected immediately downstream of this Step. Sets the
   * supplied connection name on the Data object. Also notifies any
   * StepOutputListeners.
   * 
   * @param outgoingConnectionName the name of the outgoing connection
   * @param stepName the name of the step to send the data to
   * @param data the data to send
   * @throws WekaException
   */
  @Override
  public void outputData(String outgoingConnectionName, String stepName,
    Data data) throws WekaException {
    if (!isStopRequested()) {
      data.setConnectionName(outgoingConnectionName);
      data.setSourceStep(m_managedStep);

      List<StepManager> outConnsOfType =
        m_connectedByTypeOutgoing.get(outgoingConnectionName);
      StepManagerImpl namedTarget = null;
      for (StepManager c : outConnsOfType) {
        if (((StepManagerImpl) c).getManagedStep().getName().equals(stepName)) {
          namedTarget = (StepManagerImpl) c;
        }
      }

      if (namedTarget != null && !namedTarget.isStopRequested()) {
        m_executionEnvironment.sendDataToStep(namedTarget, data);
      } else {
        // TODO log an error here and stop?
      }

      notifyOutputListeners(data);
    }
  }

  /**
   * Start the managed step processing
   */
  protected void startStep() {
    try {
      getManagedStep().start();
    } catch (WekaException ex) {
      interrupted();
      logError(ex.getMessage(), ex);
    } catch (Throwable ex) {
      interrupted();
      logError(ex.getMessage(), ex);
    }
  }

  /**
   * Stop the managed step's processing
   */
  protected void stopStep() {
    m_stopRequested = true;
    getManagedStep().stop();
  }

  /**
   * Have the managed step process the supplied data object
   *
   * @param data the data for the managed step to process
   */
  protected void processIncoming(Data data) {
    try {
      getManagedStep().processIncoming(data);
    } catch (WekaException ex) {
      interrupted();
      logError(ex.getMessage(), ex);
    } catch (Throwable e) {
      interrupted();
      logError(e.getMessage(), e);
    }
  }

  /**
   * Used by the rendering routine in LayoutPanel to ensure that connections
   * downstream from a deleted connection get rendered in grey rather than red.
   * 
   * @return a list of outgoing connection types that the managed step can
   *         produce (adjusted to take into account any upstream broken
   *         connections)
   */
  public List<String> getStepOutgoingConnectionTypes() {
    m_adjustForGraphicalRendering = true;
    List<String> results = getManagedStep().getOutgoingConnectionTypes();
    m_adjustForGraphicalRendering = false;
    return results;
  }

  /**
   * Get the number of incoming connections to the managed step
   *
   * @return the number of incoming connections
   */
  @Override
  public int numIncomingConnections() {
    int size = 0;

    for (Map.Entry<String, List<StepManager>> e : m_connectedByTypeIncoming
      .entrySet()) {
      if (m_adjustForGraphicalRendering) {
        size += numIncomingConnectionsOfType(e.getKey());
      } else {
        size += e.getValue().size();
      }
    }

    return size;
  }

  /**
   * Get the number of incoming connections to the managed step of a given type
   *
   * @param connectionName the name of the connection type
   * @return the number of incoming connections of this type
   */
  @Override
  public int numIncomingConnectionsOfType(String connectionName) {
    int num = 0;
    List<StepManager> inOfType = m_connectedByTypeIncoming.get(connectionName);
    if (inOfType != null) {
      if (m_adjustForGraphicalRendering) {
        // adjust num incoming connections according
        // to what the upstream steps can produce at present
        for (StepManager connS : inOfType) {
          List<String> generatableOutputCons =
            ((StepManagerImpl) connS).getStepOutgoingConnectionTypes();
          if (generatableOutputCons.contains(connectionName)) {
            num++;
          }
        }
      } else {
        num = inOfType.size();
      }
    }

    return num;
  }

  /**
   * Get the number of outgoing connections from the managed step
   *
   * @return the number of incoming connections
   */
  @Override
  public int numOutgoingConnections() {
    int size = 0;

    for (Map.Entry<String, List<StepManager>> e : m_connectedByTypeOutgoing
      .entrySet()) {
      size += e.getValue().size() - (m_adjustForGraphicalRendering ? 1 : 0);
    }
    if (size < 0) {
      size = 0;
    }
    return size;
  }

  /**
   * Get the number of outgoing connections from the managed step of a given
   * type
   *
   * @param connectionName the name of the connection type
   * @return the number of outgoing connections of this type
   */
  @Override
  public int numOutgoingConnectionsOfType(String connectionName) {
    int num = 0;
    List<StepManager> outOfType = m_connectedByTypeOutgoing.get(connectionName);
    if (outOfType != null) {
      num = outOfType.size();
      if (m_adjustForGraphicalRendering) {
        num--;
      }
    }

    return num;
  }

  /**
   * Attempt to get the incoming structure (as a header-only set of instances)
   * for the named incoming connection type. Assumes that there is only one
   * incoming connection of the named type. If there are zero, or more than one,
   * then null is returned
   *
   * @param connectionName the name of the incoming connection to get the
   *          structure for
   * @return the structure as a header-only set of instances or null if there
   *         are zero or more than one upstream connected steps producing the
   *         named connection, or if the upstream step can't tell us the
   *         structure, or if the upstream step can't represent the structure of
   *         the connection type as a set of instances.
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public Instances getIncomingStructureForConnectionType(String connectionName)
    throws WekaException {
    if (getIncomingConnectedStepsOfConnectionType(connectionName).size() == 1) {
      return ((StepManagerImpl) getIncomingConnectedStepsOfConnectionType(
        connectionName).get(0)).getManagedStep()
        .outputStructureForConnectionType(connectionName);
    }

    return null;
  }

  /**
   * Attempt to get the incoming structure (as a header-only set of instances)
   * from the given managed step for the given connection type.
   *
   * @param sourceStep the step manager managing the source step
   * @param connectionName the name of the connection to attempt to get the
   *          structure for
   * @return the structure as a header-only set of instances, or null if the
   *         source step can't determine this at present or if it can't be
   *         represented as a set of instances.
   * @throws WekaException if a problem occurs
   */
  @Override
  public Instances getIncomingStructureFromStep(StepManager sourceStep,
    String connectionName) throws WekaException {
    return ((StepManagerImpl) sourceStep).getManagedStep()
      .outputStructureForConnectionType(connectionName);
  }

  /**
   * Log a message at the low logging level
   *
   * @param message the message to log
   */
  @Override
  public void logLow(String message) {
    if (m_log != null) {
      m_log.logLow(message);
    }
  }

  /**
   * Log a message at the basic logging level
   *
   * @param message the message to log
   */
  @Override
  public void logBasic(String message) {
    if (m_log != null) {
      m_log.logBasic(message);
    }
  }

  /**
   * Log a message at the detailed logging level
   *
   * @param message the message to log
   */
  @Override
  public void logDetailed(String message) {
    if (m_log != null) {
      m_log.logDetailed(message);
    }
  }

  /**
   * Log a message at the debugging logging level
   *
   * @param message the message to log
   */
  @Override
  public void logDebug(String message) {
    if (m_log != null) {
      m_log.logDebug(message);
    }
  }

  /**
   * Log a warning message
   *
   * @param message the message to log
   */
  @Override
  public void logWarning(String message) {
    if (m_log != null) {
      m_log.logWarning(message);
      m_log.statusMessage("WARNING: " + message);
    }
  }

  /**
   * Log an error
   *
   * @param message the message to log
   * @param cause the optional Throwable to log
   */
  @Override
  public void logError(String message, Throwable cause) {
    if (m_log != null) {
      m_log.log(message, LoggingLevel.ERROR, cause);
      m_log.statusMessage("ERROR: " + message);
    }
    if (m_executionEnvironment != null) {
      // fatal error - make sure that everything stops.
      m_executionEnvironment.stopProcessing();
    }
  }

  /**
   * Output a status message to the status area of the log
   *
   * @param message the message to output
   */
  @Override
  public void statusMessage(String message) {
    if (m_log != null) {
      m_log.statusMessage(message);
    }
  }

  /**
   * Log a message at the supplied logging level
   *
   * @param message the message to write
   * @param level the level for the message
   */
  @Override
  public void log(String message, LoggingLevel level) {
    if (m_log != null) {
      m_log.log(message, level, null);
    }
  }

  /**
   * Substitute the values of environment variables in the given string
   *
   * @param source the source string to substitute in
   * @return the source string with all known environment variables resolved
   */
  @Override
  public String environmentSubstitute(String source) {
    Environment toUse = Environment.getSystemWide(); // default system-wide

    if (getExecutionEnvironment() != null) {
      toUse = getExecutionEnvironment().getEnvironmentVariables();
    }

    String result = source;

    if (source != null) {
      try {
        result = toUse.substitute(source);
      } catch (Exception ex) {
        // ignore
      }
    }

    return result;
  }

  /**
   * Returns a reference to the step being managed if it has one or more
   * outgoing CON_INFO connections and the managed step is of the supplied class
   *
   * @param stepClass the expected class of the step
   * @return the step being managed if outgoing CON_INFO connections are present
   *         and the step is of the supplied class
   * @throws WekaException if there are no outgoing CON_INFO connections or the
   *           managed step is the wrong type
   */
  @Override
  public Step getInfoStep(Class stepClass) throws WekaException {
    Step info = getInfoStep();
    if (!(info.getClass() == stepClass)) {
      throw new WekaException("The managed step ("
        + info.getClass().getCanonicalName() + ") is not "
        + "not an instance of the required class: "
        + stepClass.getCanonicalName());
    }

    return info;
  }

  /**
   * Returns a reference to the step being managed if it has one or more
   * outgoing CON_INFO connections.
   * 
   * @return the step being managed if outgoing CON_INFO connections are present
   * @throws WekaException if there are no outgoing CON_INFO connections
   */
  @Override
  public Step getInfoStep() throws WekaException {
    if (numOutgoingConnectionsOfType(StepManager.CON_INFO) > 0) {
      return getManagedStep();
    }

    throw new WekaException("There are no outgoing info connections from "
      + "this step!");
  }

  /**
   * Finds a named step in the current flow. Returns null if the named step is
   * not present in the flow
   *
   * @param stepNameToFind the name of the step to find
   * @return the StepManager of the named step, or null if the step does not
   *         exist in the current flow.
   */
  @Override
  public StepManager findStepInFlow(String stepNameToFind) {
    Flow flow = m_executionEnvironment.getFlowExecutor().getFlow();

    return flow.findStep(stepNameToFind);
  }

  /**
   * Gets a prefix for the step managed by this manager. Used to uniquely
   * identify steps in the status area of the log
   *
   * @return a unique prefix for the step managed by this manager
   */
  public String stepStatusMessagePrefix() {
    String prefix =
      (getManagedStep() != null ? getManagedStep().getName() : "Unknown") + "$";

    prefix +=
      (getManagedStep() != null ? getManagedStep().hashCode() : 1) + "|";
    if (getManagedStep() instanceof WekaAlgorithmWrapper) {
      Object wrappedAlgo =
        ((WekaAlgorithmWrapper) getManagedStep()).getWrappedAlgorithm();
      if (wrappedAlgo instanceof OptionHandler) {
        prefix +=
          Utils.joinOptions(((OptionHandler) wrappedAlgo).getOptions()) + "|";
      }
    }

    return prefix;
  }

  /**
   * Return true if the supplied connection name is an incremental connection.
   * Several built-in connection types are considered incremental: instance,
   * incremental_classifier, and chart. Clients can indicate that their custom
   * connection/data is incremental by setting the payload element
   * "CON_AUX_DATA_IS_INCREMENTAL" to true in their Data object.
   *
   * @param conn the name of the connection to check
   * @return true if the supplied connection name is an incremental connection
   */
  protected static boolean connectionIsIncremental(Data conn) {
    return conn.getConnectionName().equalsIgnoreCase(StepManager.CON_INSTANCE)
      || conn.getConnectionName().equalsIgnoreCase(
        StepManager.CON_INCREMENTAL_CLASSIFIER)
      || conn.getConnectionName().equalsIgnoreCase(StepManager.CON_CHART)
      || conn.getPayloadElement(StepManager.CON_AUX_DATA_IS_INCREMENTAL, false);
  }
}
