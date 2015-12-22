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
 *    BaseExecutionEnvironment.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.Environment;
import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.Logger;
import weka.gui.beans.PluginManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Base class for execution environments
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class BaseExecutionEnvironment implements ExecutionEnvironment {

  /** Description of the default execution environment */
  public static final String DESCRIPTION = "Default execution environment";

  static {
    // register the default execution environment
    PluginManager.addPlugin(BaseExecutionEnvironment.class.getCanonicalName(),
      DESCRIPTION, BaseExecutionEnvironment.class.getCanonicalName());
  }

  /**
   * The FlowExecutor that will be running the flow. This is not guaranteed to
   * be available from this class until the flow is initialized
   */
  protected FlowExecutor m_flowExecutor;

  /** Whether the execution environment is headless or not */
  protected boolean m_headless;

  /** The environment variables to use */
  protected transient Environment m_envVars = Environment.getSystemWide();

  /** The knowledge flow settings to use */
  protected transient Settings m_settings;

  /**
   * An executor service that steps can use to do work in parallel (if desired).
   * This is also used by the execution environment to invoke processIncoming()
   * on a step when batch data is passed to it - i.e. a step receiving data will
   * execute in its own thread. This is not used, however, when streaming data
   * is processed, as it usually the case that processing is lightweight in this
   * case and the gains of running a separate thread for each piece of data
   * probably do not outweigh the overheads involved.
   */
  protected transient ExecutorService m_executorService;

  /** The log */
  protected transient Logger m_log;

  /** The level to log at */
  protected LoggingLevel m_loggingLevel = LoggingLevel.BASIC;

  /**
   * Get a description of this execution environment
   *
   * @return a description of this execution environemtn
   */
  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  /**
   * Get whether this execution environment is headless
   *
   * @return true if this execution environment is headless
   */
  @Override
  public boolean isHeadless() {
    return m_headless;
  }

  /**
   * Set whether this execution environment is headless
   * 
   * @param headless true if the execution environment is headless
   */
  @Override
  public void setHeadless(boolean headless) {
    m_headless = headless;
  }

  /**
   * Get environment variables for this execution environment
   * 
   * @return the environment variables for this execution environment
   */
  @Override
  public Environment getEnvironmentVariables() {
    return m_envVars;
  }

  /**
   * Set environment variables for this execution environment
   * 
   * @param env the environment variables to use
   */
  @Override
  public void setEnvironmentVariables(Environment env) {
    m_envVars = env;
  }

  @Override
  public void setSettings(Settings settings) {
    m_settings = settings;
  }

  @Override
  public Settings getSettings() {
    if (m_settings == null) {
      m_settings = new Settings("weka", KFDefaults.APP_ID);
      try {
        m_settings.loadSettings();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return m_settings;
  }

  /**
   * Get the log in use
   * 
   * @return the log in use
   */
  @Override
  public Logger getLog() {
    return m_log;
  }

  /**
   * Set the log to use
   * 
   * @param log the log to use
   */
  @Override
  public void setLog(Logger log) {
    m_log = log;
  }

  /**
   * Get the logging level in use
   *
   * @return the logging level in use
   */
  @Override
  public LoggingLevel getLoggingLevel() {
    return m_loggingLevel;
  }

  /**
   * Set the logging level to use
   *
   * @param level the logging level to use
   */
  @Override
  public void setLoggingLevel(LoggingLevel level) {
    m_loggingLevel = level;
  }

  /**
   * Submit a task to be run by the execution environment. The default execution
   * environment uses an ExecutorService to run tasks in parallel. Client steps
   * are free to use this service or to just do their processing locally within
   * their own code.
   * 
   * @param stepTask the StepTask encapsulating the code to be run
   * @return the Future holding the status and result when complete
   */
  @Override
  public <T> Future<ExecutionResult<T>> submitTask(StepTask<T> stepTask)
    throws WekaException {

    return m_executorService.submit(stepTask);
  }

  /**
   * The main point at which to request stop processing of a flow. This will
   * request the FlowExecutor to stop and then shutdown the executor service
   */
  @Override
  public void stopProcessing() {
    if (getFlowExecutor() != null) {
      getFlowExecutor().stopProcessing();
    }
    if (m_executorService != null) {
      m_executorService.shutdownNow();
      m_executorService = null;
    }
  }

  /**
   * Get the executor that will actually be responsible for running the flow.
   * This is not guaranteed to be available from this execution environment
   * until the flow is actually running (or at least initialized)
   * 
   * @return the executor that will be running the flow
   */
  protected FlowExecutor getFlowExecutor() {
    return m_flowExecutor;
  }

  /**
   * Set the executor that will actually be responsible for running the flow.
   * This is not guaranteed to be available from this execution environment
   * until the flow is actually running (or at least initialized)
   * 
   * @param executor the executor that will be running the flow
   */
  protected void setFlowExecutor(FlowExecutor executor) {
    m_flowExecutor = executor;
  }

  /**
   * Start the executor service for clients to use to execute tasks in this
   * execution environment. Client steps are free to use this service or to just
   * do their processing locally within their own code.
   * 
   * @param numThreads the number of threads to use (level of parallelism). <= 0
   *          indicates no limit on parallelism
   */
  protected void startClientExecutionService(int numThreads) {
    if (m_executorService != null) {
      m_executorService.shutdownNow();
    }
    m_executorService =
      numThreads > 0 ? Executors.newFixedThreadPool(numThreads)
        : Executors.newCachedThreadPool();
  }

  /**
   * Stop the client executor service
   */
  protected void stopClientExecutionService() {
    if (m_executorService != null) {
      m_executorService.shutdown();
    }
  }

  /**
   * Send the supplied data to the specified step. Base implementation just
   * calls processIncoming() on the step directly. Subclasses may opt to do
   * something different (e.g. wrap data and target step to be executed, and
   * then execute remotely).
   * 
   * @param data the data to input to the target step
   * @param step the step to receive data
   * @throws WekaException if a problem occurs
   */
  protected void sendDataToStep(final StepManagerImpl step, final Data... data)
    throws WekaException {

    if (data != null) {
      if (data.length == 1
        && (StepManagerImpl.connectionIsIncremental(data[0]))) {
        // we don't want the overhead of spinning up a thread for single
        // instance (streaming) connections.
        step.processIncoming(data[0]);
      } else {
        m_executorService.submit(new Runnable() {

          @Override
          public void run() {
            for (Data d : data) {
              step.processIncoming(d);
            }
          }
        });
      }
    }
  }
}
