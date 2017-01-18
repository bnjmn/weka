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
 *    StepManager.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.Instances;
import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.Logger;
import weka.knowledgeflow.steps.Step;

import java.util.List;
import java.util.Map;

/**
 * Client public interface for the StepManager. Step implementations should only
 * use this interface
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public interface StepManager {

  // standard connection types
  public static final String CON_DATASET = "dataSet";
  public static final String CON_INSTANCE = "instance";
  public static final String CON_TRAININGSET = "trainingSet";
  public static final String CON_TESTSET = "testSet";
  public static final String CON_BATCH_CLASSIFIER = "batchClassifier";
  public static final String CON_INCREMENTAL_CLASSIFIER =
    "incrementalClassifier";
  public static final String CON_INCREMENTAL_CLUSTERER = "incrementalClusterer";
  public static final String CON_BATCH_CLUSTERER = "batchClusterer";
  public static final String CON_BATCH_ASSOCIATOR = "batchAssociator";
  public static final String CON_VISUALIZABLE_ERROR = "visualizableError";
  public static final String CON_THRESHOLD_DATA = "thresholdData";
  public static final String CON_TEXT = "text";
  public static final String CON_IMAGE = "image";
  public static final String CON_GRAPH = "graph";
  public static final String CON_CHART = "chart";
  public static final String CON_INFO = "info";
  public static final String CON_ENVIRONMENT = "environment";
  public static final String CON_JOB_SUCCESS = "jobSuccess";
  public static final String CON_JOB_FAILURE = "jobFailure";

  // auxiliary information for various connections
  public static final String CON_AUX_DATA_SET_NUM = "aux_set_num";
  public static final String CON_AUX_DATA_MAX_SET_NUM = "aux_max_set_num";
  public static final String CON_AUX_DATA_TEST_INSTANCE = "aux_testInstance";
  public static final String CON_AUX_DATA_TESTSET = "aux_testsSet";
  public static final String CON_AUX_DATA_TRAININGSET = "aux_trainingSet";
  public static final String CON_AUX_DATA_INSTANCE = "aux_instance";
  public static final String CON_AUX_DATA_TEXT_TITLE = "aux_textTitle";
  public static final String CON_AUX_DATA_LABEL = "aux_label";
  public static final String CON_AUX_DATA_CLASS_ATTRIBUTE = "class_attribute";
  public static final String CON_AUX_DATA_GRAPH_TITLE = "graph_title";
  public static final String CON_AUX_DATA_GRAPH_TYPE = "graph_type";
  public static final String CON_AUX_DATA_CHART_MAX = "chart_max";
  public static final String CON_AUX_DATA_CHART_MIN = "chart_min";
  public static final String CON_AUX_DATA_CHART_DATA_POINT = "chart_data_point";
  public static final String CON_AUX_DATA_CHART_LEGEND = "chart_legend";
  public static final String CON_AUX_DATA_ENVIRONMENT_VARIABLES = "env_variables";
  public static final String CON_AUX_DATA_ENVIRONMENT_PROPERTIES = "env_properties";
  public static final String CON_AUX_DATA_ENVIRONMENT_RESULTS = "env_results";
  public static final String CON_AUX_DATA_BATCH_ASSOCIATION_RULES =
    "batch_association_rules";
  public static final String CON_AUX_DATA_INCREMENTAL_STREAM_END =
    "incremental_stream_end";
  public static final String CON_AUX_DATA_IS_INCREMENTAL = "incremental_stream";

  /**
   * Get the name of the step managed by this StepManager
   * 
   * @return the name of the managed step
   */
  String getName();

  /**
   * Get the actual step managed by this step manager
   *
   * @return the Step managed by this step manager
   */
  Step getManagedStep();

  /**
   * Get the executing environment. This contains information such as whether
   * the flow is running in headless environment, what environment variables are
   * available and methods to execute units of work in parallel.
   * 
   * @return the execution environment
   */
  ExecutionEnvironment getExecutionEnvironment();

  /**
   * Get the knowledge flow settings
   *
   * @return the knowledge flow settings
   */
  Settings getSettings();

  /**
   * Get the number of steps that are connected with incoming connections
   * 
   * @return the number of incoming connections
   */
  int numIncomingConnections();

  /**
   * Get the number of steps that are connected with outgoing connections
   *
   * @return the number of outgoing connections
   */
  int numOutgoingConnections();

  /**
   * Get the number of steps that are connected with the given incoming
   * connection type
   * 
   * @param connectionName the type of the incoming connection
   * @return the number of steps connected with the specified incoming
   *         connection type
   */
  int numIncomingConnectionsOfType(String connectionName);

  /**
   * Get the number of steps that are connected with the given outgoing
   * connection type
   * 
   * @param connectionName the type of the outgoing connection
   * @return the number of steps connected with the specified outgoing
   *         connection type
   */
  int numOutgoingConnectionsOfType(String connectionName);

  /**
   * Get a list of steps that are the source of incoming connections of the
   * given type
   * 
   * @param connectionName the name of the incoming connection to get a list of
   *          steps for
   * @return a list of steps that are the source of incoming connections of the
   *         given type
   */
  List<StepManager> getIncomingConnectedStepsOfConnectionType(
    String connectionName);

  /**
   * Get the named step that is connected with an incoming connection.
   * 
   * @param stepName the name of the step to get
   * @return the step connected with an incoming connection or null if the named
   *         step is not connected
   */
  StepManager getIncomingConnectedStepWithName(String stepName);

  /**
   * Get a named step connected to this step with an outgoing connection
   *
   * @param stepName the name of the step to look for
   * @return the connected step
   */
  StepManager getOutgoingConnectedStepWithName(String stepName);

  /**
   * Get a list of downstream steps connected to this step with the given
   * connection type.
   * 
   * @param connectionName the name of the outgoing connection
   * @return a list of downstream steps connected to this one with the named
   *         connection type
   */
  List<StepManager> getOutgoingConnectedStepsOfConnectionType(
    String connectionName);

  /**
   * Get a Map of all incoming connections. Map is keyed by connection type;
   * values are lists of steps
   * 
   * @return a Map of incoming connections
   */
  Map<String, List<StepManager>> getIncomingConnections();

  /**
   * Get a Map of all outgoing connections. Map is keyed by connection type;
   * values are lists of steps
   * 
   * @return a Map of outgoing connections
   */
  Map<String, List<StepManager>> getOutgoingConnections();

  /**
   * Output data to all steps connected with the supplied outgoing connection
   * type. Populates the source and connection name in the supplied Data object
   * for the client
   * 
   * @param outgoingConnectionName the type of the outgoing connection to send
   *          data to
   * @param data a single Data object to send
   * @throws WekaException if a problem occurs
   */
  void outputData(String outgoingConnectionName, Data data)
    throws WekaException;

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
  void outputData(Data... data) throws WekaException;

  /**
   * Output a single Data object to the named step with the supplied outgoing
   * connection type
   * 
   * @param outgoingConnectionName the name of the outgoing connection
   * @param stepName the name of the step to send the data to
   * @param data the data to send
   * @throws WekaException if a problem occurs
   */
  void outputData(String outgoingConnectionName, String stepName, Data data)
    throws WekaException;

  /**
   * Attempt to retrieve the structure (as a header-only set of instances) for
   * the named incoming connection type. Assumes that there is only one step
   * connected with the supplied incoming connection type.
   * 
   * @param connectionName the type of the incoming connection to get the
   *          structure for
   * @return the structure of the data for the specified incoming connection, or
   *         null if the structure can't be determined (or represented as an
   *         Instances object)
   * @throws WekaException if a problem occurs
   */
  Instances getIncomingStructureForConnectionType(String connectionName)
    throws WekaException;

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
  Instances getIncomingStructureFromStep(StepManager sourceStep,
    String connectionName) throws WekaException;

  /**
   * Returns true if, at this time, the step managed by this step manager is
   * currently busy with processing
   *
   * @return true if the step managed by this step manager is busy
   */
  boolean isStepBusy();

  /**
   * Return true if a stop has been requested by the runtime environment
   *
   * @return true if a stop has been requested
   */
  boolean isStopRequested();

  /**
   * Return true if the current step is finished.
   *
   * @return true if the current step is finished
   */
  boolean isStepFinished();

  /**
   * Step implementations processing batch data should call this to indicate
   * that they have started some processing. Calling this should set the busy
   * flag to true.
   */
  void processing();

  /**
   * Step implementations processing batch data should call this to indicate
   * that they have finished all processing. Calling this should set the busy
   * flag to false.
   */
  void finished();

  /**
   * Step implementations processing batch data should call this as soon as they
   * have finished processing after a stop has been requested. Calling this
   * should set the busy flag to false.
   */
  void interrupted();

  /**
   * Returns true if this data object marks the end of an incremental stream.
   * Note - does not check that the data object is actually an incremental one
   * of some sort! Just checks to see if the CON_AUX_DATA_INCREMENTAL_STREAM_END
   * flag is set to true or not;
   *
   * @param data the data element to check
   * @return true if the data element is flagged as end of stream
   */
  boolean isStreamFinished(Data data);

  /**
   * Start a throughput measurement. Should only be used by steps that are
   * processing instance streams. Call just before performing a unit of work for
   * an incoming instance.
   */
  void throughputUpdateStart();

  /**
   * End a throughput measurement. Should only be used by steps that are
   * processing instance streams. Call just after finishing a unit of work for
   * an incoming instance
   */
  void throughputUpdateEnd();

  /**
   * Signal that throughput measurement has finished. Should only be used by
   * steps that are emitting incremental data. Call as the completion of an
   * data stream.
   *
   * @param data one or more Data events (with appropriate connection type set)
   *          to pass on to downstream connected steps. These are used to carry
   *          any final data and to inform the downstream step(s) that the
   *          stream has ended
   * @throws WekaException if a problem occurs
   */
  void throughputFinished(Data... data) throws WekaException;

  /**
   * Log a message at the "low" level
   * 
   * @param message the message to log
   */
  void logLow(String message);

  /**
   * Log a message at the "basic" level
   * 
   * @param message the message to log
   */
  void logBasic(String message);

  /**
   * Log a message at the "detailed" level
   * 
   * @param message the message to log
   */
  void logDetailed(String message);

  /**
   * Log a message at the "debug" level
   * 
   * @param message the message to log
   */
  void logDebug(String message);

  /**
   * Log a warning message. Always makes it into the log regardless of what
   * logging level the user has specified.
   * 
   * @param message the message to log
   */
  void logWarning(String message);

  /**
   * Log an error message. Always makes it into the log regardless of what
   * logging level the user has specified. Causes all flow execution to halt.
   * Prints an exception to the log if supplied.
   * 
   * @param message the message to log
   * @param cause the optional Throwable to log
   */
  void logError(String message, Throwable cause);

  /**
   * Write a message to the log at the given logging level
   * 
   * @param message the message to write
   * @param level the level for the message
   */
  void log(String message, LoggingLevel level);

  /**
   * Write a status message
   * 
   * @param message the message
   */
  void statusMessage(String message);

  /**
   * Get the log
   *
   * @return the log object
   */
  Logger getLog();

  /**
   * Get the currently set logging level
   *
   * @return the currently set logging level
   */
  LoggingLevel getLoggingLevel();

  /**
   * Substitute all known environment variables in the given string
   * 
   * @param source the source string
   * @return the source string with all known variables resolved
   */
  String environmentSubstitute(String source);

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
  Step getInfoStep(Class stepClass) throws WekaException;

  /**
   * Returns a reference to the step being managed if it has one or more
   * outgoing CON_INFO connections.
   *
   * @return the step being managed if outgoing CON_INFO connections are present
   * @throws WekaException if there are no outgoing CON_INFO connections
   */
  Step getInfoStep() throws WekaException;

  /**
   * Finds a named step in the current flow. Returns null if the named step is
   * not present in the flow
   *
   * @param stepNameToFind the name of the step to find
   * @return the StepManager of the named step, or null if the step does not
   *         exist in the current flow.
   */
  StepManager findStepInFlow(String stepNameToFind);

  /**
   * Returns true if the step managed by this step manager has been marked as
   * being resource (cpu/memory) intensive.
   * 
   * @return true if the managed step is resource intensive
   */
  boolean stepIsResourceIntensive();

  /**
   * Mark the step managed by this step manager as resource intensive
   * 
   * @param isResourceIntensive true if the step managed by this step manager is
   *          resource intensive
   */
  void setStepIsResourceIntensive(boolean isResourceIntensive);

  /**
   * Marked the step managed by this step manager as one that must run
   * single-threaded. I.e. in an executor service with one worker thread, thus
   * effectively preventing more than one copy of the step from executing at any
   * one point in time
   *
   * @param mustRunSingleThreaded true if the managed step must run
   *          single-threaded
   */
  void setStepMustRunSingleThreaded(boolean mustRunSingleThreaded);

  /**
   * Returns true if the step managed by this step manager has been marked as
   * one that must run single-threaded. I.e. in an executor service with one
   * worker thread, thus effectively preventing more than one copy of the step
   * from executing at any one point in time
   *
   * @param mustRunSingleThreaded true if the managed step must run
   *          single-threaded
   */
  boolean getStepMustRunSingleThreaded();
}
