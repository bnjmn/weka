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

import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.Logger;
import weka.core.PluginManager;
import weka.gui.knowledgeflow.GraphicalEnvironmentCommandHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

  /**
   * The handler (if any) for application-level commands in a graphical
   * environment
   */
  protected GraphicalEnvironmentCommandHandler m_graphicalEnvCommandHandler;

  /** The environment variables to use */
  protected transient Environment m_envVars = Environment.getSystemWide();

  /** The knowledge flow settings to use */
  protected transient Settings m_settings;

  /**
   * This is the main executor service, used by the execution environment to
   * invoke processIncoming() on a step when batch data is passed to it - i.e. a
   * step receiving data will execute in its own thread. This is not used,
   * however, when streaming data is processed, as it usually the case that
   * processing is lightweight in this case and the gains of running a separate
   * thread for each piece of data probably do not outweigh the overheads
   * involved.
   */
  protected transient ExecutorService m_executorService;

  /**
   * An executor service that steps can use to do work in parallel (if desired)
   * by using {@code StepTask instances}. This executor service is intended for
   * high cpu load tasks, and the number of threads used by it should be kept <=
   * number of physical CPU cores
   */
  protected transient ExecutorService m_clientExecutorService;

  /**
   * An executor service with a single worker thread. This is used to execute
   * steps that are marked with the {@code SingleThreadedExecution annotation},
   * which effectively limits one object of the type in question to be executing
   * in the KnowledgeFlow/JVM at any one time.
   */
  protected transient ExecutorService m_singleThreadService;

  /** The log */
  protected transient Logger m_log;

  /** Log handler to wrap log in */
  protected transient LogManager m_logHandler;

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
   * Get the environment for performing commands at the application-level in a
   * graphical environment.
   *
   * @return the graphical environment command handler, or null if running
   *         headless
   */
  @Override
  public GraphicalEnvironmentCommandHandler
    getGraphicalEnvironmentCommandHandler() {

    return m_graphicalEnvCommandHandler;
  }

  /**
   * Set the environment for performing commands at the application-level in a
   * graphical environment.
   *
   * @handler the handler to use
   */
  @Override
  public void setGraphicalEnvironmentCommandHandler(
    GraphicalEnvironmentCommandHandler handler) {
    m_graphicalEnvCommandHandler = handler;
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

    m_logHandler.setLoggingLevel(m_settings.getSetting(
      KFDefaults.MAIN_PERSPECTIVE_ID, KFDefaults.LOGGING_LEVEL_KEY,
      KFDefaults.LOGGING_LEVEL));
  }

  @Override
  public Settings getSettings() {
    if (m_settings == null) {
      m_settings = new Settings("weka", KFDefaults.APP_ID);
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
    if (m_logHandler == null) {
      m_logHandler = new LogManager(m_log);
      m_logHandler.m_statusMessagePrefix =
        "BaseExecutionEnvironment$" + hashCode() + "|";
    }
    m_logHandler.setLog(m_log);
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
    String taskType = "";
    if (stepTask.getMustRunSingleThreaded()) {
      taskType = " (single threaded)";
    } else if (stepTask.isResourceIntensive()) {
      taskType = " (resource intensive)";
    }
    m_logHandler.logDebug("Submitting " + stepTask.toString() + taskType);
    if (stepTask.getMustRunSingleThreaded()) {
      return m_singleThreadService.submit(stepTask);
    }
    if (stepTask.isResourceIntensive()) {
      return m_clientExecutorService.submit(stepTask);
    }
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
   * Gets a new instance of the default flow executor suitable for use with this
   * execution environment
   *
   * @return a new instance of the default flow executor suitable for use with
   *         this execution environment
   */
  public FlowExecutor getDefaultFlowExecutor() {
    return new FlowRunner();
  }

  /**
   * Get the executor that will actually be responsible for running the flow.
   * This is not guaranteed to be available from this execution environment
   * until the flow is actually running (or at least initialized)
   * 
   * @return the executor that will be running the flow
   */
  public FlowExecutor getFlowExecutor() {
    return m_flowExecutor;
  }

  /**
   * Set the executor that will actually be responsible for running the flow.
   * This is not guaranteed to be available from this execution environment
   * until the flow is actually running (or at least initialized)
   * 
   * @param executor the executor that will be running the flow
   */
  public void setFlowExecutor(FlowExecutor executor) {
    m_flowExecutor = executor;
  }

  /**
   * Start the main step executor service and the high cpu load executor service
   * for clients to use to execute {@code StepTask} instances in this execution
   * environment. Client steps are free to use this service or to just do their
   * processing locally within their own code (in which case the main step
   * executor service is used).
   * 
   * @param numThreadsMain the number of threads to use (level of parallelism).
   *          <= 0 indicates no limit on parallelism
   * @param numThreadsHighLoad the number of threads to use for the high cpu
   *          load executor service (executes {@code StepTask} instances)
   */
  protected void startClientExecutionService(int numThreadsMain,
    int numThreadsHighLoad) {
    if (m_executorService != null) {
      m_executorService.shutdownNow();
    }

    m_logHandler
      .logDebug("Requested number of threads for main step executor: "
        + numThreadsMain);
    m_logHandler
      .logDebug("Requested number of threads for high load executor: "
        + (numThreadsHighLoad > 0 ? numThreadsHighLoad : Runtime.getRuntime()
          .availableProcessors()));
    m_executorService =
      numThreadsMain > 0 ? Executors.newFixedThreadPool(numThreadsMain)
        : Executors.newCachedThreadPool();

    m_clientExecutorService =
      numThreadsHighLoad > 0 ? Executors.newFixedThreadPool(numThreadsHighLoad)
        : Executors.newFixedThreadPool(Runtime.getRuntime()
          .availableProcessors());

    m_singleThreadService = Executors.newSingleThreadExecutor();
  }

  /**
   * Stop the client executor service
   */
  protected void stopClientExecutionService() {
    if (m_executorService != null) {
      m_executorService.shutdown();
      try {
        // try to avoid a situation where a step might not have received
        // data yet (and will be launching tasks on the client executor service
        // when it does), all preceding steps have finished, and the shutdown
        // monitor has polled and found all steps not busy. In this case,
        // both executor services will be shutdown. The main one will have
        // the job of executing the step in question (and will already have
        // a runnable for this in its queue, so this will get executed).
        // However,
        // the client executor service will (potentially) have an empty queue
        // as the step has not launched a task on it yet. This can lead to
        // a situation where the step tries to launch a task when the
        // client executor service has been shutdown. Blocking here at the
        // main executor service should avoid this situation.
        m_executorService.awaitTermination(5L, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    if (m_clientExecutorService != null) {
      m_clientExecutorService.shutdown();
    }

    if (m_singleThreadService != null) {
      m_singleThreadService.shutdown();
    }
  }

  /**
   * Launches a Step (via the startStep() method) in either than standard step
   * executor service or the resource intensive executor service. Does not check
   * that the step is actually a start point.
   *
   * @param startPoint the step to launch as a start point
   * @throws WekaException if a problem occurs
   */
  protected void launchStartPoint(final StepManagerImpl startPoint)
    throws WekaException {

    String taskType =
      startPoint.getStepMustRunSingleThreaded() ? " (single-threaded)"
        : (startPoint.stepIsResourceIntensive() ? " (resource intensive)" : "");
    m_logHandler.logDebug("Submitting " + startPoint.getName() + taskType);

    if (startPoint.getStepMustRunSingleThreaded()) {
      StepTask<Void> singleThreaded = new StepTask<Void>(null) {
        private static final long serialVersionUID = -4008646793585608806L;

        @Override
        public void process() throws Exception {
          startPoint.startStep();
        }
      };
      singleThreaded.setMustRunSingleThreaded(true);
      submitTask(singleThreaded);
    } else if (startPoint.stepIsResourceIntensive()) {
      submitTask(new StepTask<Void>(null) {

        /** For serialization */
        private static final long serialVersionUID = -5466021103296024455L;

        @Override
        public void process() throws Exception {
          startPoint.startStep();
        }
      });
    } else {
      m_executorService.submit(new Runnable() {
        @Override
        public void run() {
          startPoint.startStep();
        }
      });
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
        String taskType =
          step.getStepMustRunSingleThreaded() ? " (single-threaded)" : (step
            .stepIsResourceIntensive() ? " (resource intensive)" : "");
        m_logHandler.logDebug("Submitting " + step.getName() + taskType);
        if (step.getStepMustRunSingleThreaded()) {
          m_singleThreadService.submit(new Runnable() {
            @Override
            public void run() {
              for (Data d : data) {
                step.processIncoming(d);
              }
            }
          });
        } else if (step.stepIsResourceIntensive()) {
          m_clientExecutorService.submit(new Runnable() {
            @Override
            public void run() {
              for (Data d : data) {
                step.processIncoming(d);
              }
            }
          });
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

  /**
   * Get default settings for the base execution environment
   *
   * @return the default settings
   */
  @Override
  public Defaults getDefaultSettings() {
    return new BaseExecutionEnvironmentDefaults();
  }

  /**
   * Defaults for the base execution environment
   */
  public static class BaseExecutionEnvironmentDefaults extends Defaults {

    public static final Settings.SettingKey STEP_EXECUTOR_SERVICE_NUM_THREADS_KEY =
      new Settings.SettingKey(KFDefaults.APP_ID + ".stepExecutorNumThreads",
        "Number of threads to use in the main step executor service", "");
    public static final int STEP_EXECUTOR_SERVICE_NUM_THREADS = 50;

    public static final Settings.SettingKey RESOURCE_INTENSIVE_EXECUTOR_SERVICE_NUM_THREADS_KEY =
      new Settings.SettingKey(KFDefaults.APP_ID
        + ".highResourceExecutorNumThreads",
        "Number of threads to use in the resource intensive executor service",
        "<html>This executor service is used for executing StepTasks and<br>"
          + "Steps that are marked as resource intensive. 0 = use as many<br>"
          + "threads as there are cpu processors.</html>");

    /** Default (0) means use as many threads as there are cpu processors */
    public static final int RESOURCE_INTENSIVE_EXECUTOR_SERVICE_NUM_THREADS = 0;
    private static final long serialVersionUID = -3386792058002464330L;

    public BaseExecutionEnvironmentDefaults() {
      super(KFDefaults.APP_ID);

      m_defaults.put(STEP_EXECUTOR_SERVICE_NUM_THREADS_KEY,
        STEP_EXECUTOR_SERVICE_NUM_THREADS);
      m_defaults.put(RESOURCE_INTENSIVE_EXECUTOR_SERVICE_NUM_THREADS_KEY,
        RESOURCE_INTENSIVE_EXECUTOR_SERVICE_NUM_THREADS);
    }
  }
}
