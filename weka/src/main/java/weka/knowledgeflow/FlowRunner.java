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
 *    FlowRunner.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.PluginManager;
import weka.core.Settings;
import weka.core.Utils;
import weka.core.WekaException;
import weka.core.WekaPackageManager;
import weka.gui.Logger;
import weka.gui.knowledgeflow.KnowledgeFlowApp;
import weka.knowledgeflow.steps.Note;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * A FlowExecutor that can launch start points in a flow in parallel or
 * sequentially.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class FlowRunner implements FlowExecutor, CommandlineRunnable {

  /** The flow to execute */
  protected Flow m_flow;

  /** The execution environment */
  protected transient BaseExecutionEnvironment m_execEnv;

  /** The log to use */
  protected transient Logger m_log = new SimpleLogger();

  /** Local log handler for FlowRunner-specific logging */
  protected transient LogManager m_logHandler;

  /** The level to at which to log at */
  protected LoggingLevel m_loggingLevel = LoggingLevel.BASIC;

  /** Invoke start points sequentially? */
  protected boolean m_startSequentially;

  /** Number of worker threads to use in the step executor service */
  protected int m_numThreads =
    BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.STEP_EXECUTOR_SERVICE_NUM_THREADS;

  /**
   * Number of worker threads to use in the resource intensive executor service
   */
  protected int m_resourceIntensiveNumThreads =
    BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.RESOURCE_INTENSIVE_EXECUTOR_SERVICE_NUM_THREADS;

  /** Callback to notify when execution completes */
  protected List<ExecutionFinishedCallback> m_callbacks =
    new ArrayList<ExecutionFinishedCallback>();

  /** Gets set to true if the stopProcessing() method is called */
  protected boolean m_wasStopped;

  /**
   * Constructor
   */
  public FlowRunner() {
    Settings settings = new Settings("weka", KFDefaults.APP_ID);
    settings.applyDefaults(new KFDefaults());
    init(settings);
  }

  /**
   * Constructor
   */
  public FlowRunner(Settings settings) {
    init(settings);
  }

  protected void init(Settings settings) {
    // TODO probably need some command line options to override settings for
    // logging, execution environment etc.

    // force the base execution environment class to be loaded so that it
    // registers itself with the plugin manager
    new BaseExecutionEnvironment();
    String execName =
      settings.getSetting(KFDefaults.APP_ID,
        KnowledgeFlowApp.KnowledgeFlowGeneralDefaults.EXECUTION_ENV_KEY,
        KnowledgeFlowApp.KnowledgeFlowGeneralDefaults.EXECUTION_ENV);
    BaseExecutionEnvironment execE = null;
    try {
      execE =
        (BaseExecutionEnvironment) PluginManager.getPluginInstance(
          BaseExecutionEnvironment.class.getCanonicalName(), execName);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    if (execE != null) {
      m_execEnv = execE;
    } else {
      // default execution environment is headless
      m_execEnv = new BaseExecutionEnvironment();
    }
    m_execEnv.setHeadless(true);
    m_execEnv.setFlowExecutor(this);
    m_execEnv.setLog(m_log);
    m_execEnv.setSettings(settings);

    m_numThreads =
      settings
        .getSetting(
          KFDefaults.APP_ID,
          BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.STEP_EXECUTOR_SERVICE_NUM_THREADS_KEY,
          BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.STEP_EXECUTOR_SERVICE_NUM_THREADS);
    m_resourceIntensiveNumThreads =
      settings
        .getSetting(
          KFDefaults.APP_ID,
          BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.RESOURCE_INTENSIVE_EXECUTOR_SERVICE_NUM_THREADS_KEY,
          BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.RESOURCE_INTENSIVE_EXECUTOR_SERVICE_NUM_THREADS);
  }

  /**
   * Set the settings to use when executing the Flow
   *
   * @param settings the settings to use
   */
  @Override
  public void setSettings(Settings settings) {
    init(settings);
  }

  /**
   * Get the settings in use when executing the Flow
   *
   * @return the settings
   */
  @Override
  public Settings getSettings() {
    return m_execEnv.getSettings();
  }

  /**
   * Main method for executing the FlowRunner
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
      "Logging started");
    try {
      WekaPackageManager.loadPackages(false, true, false);
      FlowRunner fr = new FlowRunner();
      fr.run(fr, args);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Run a FlowRunner object
   *
   * @param toRun the FlowRunner object to execute
   * @param args the command line arguments
   * @throws Exception if a problem occurs
   */
  @Override
  public void run(Object toRun, String[] args) throws Exception {
    if (!(toRun instanceof FlowRunner)) {
      throw new IllegalArgumentException("Object to run is not an instance of "
        + "FlowRunner!");
    }

    if (args.length < 1) {
      System.err.println("Usage:\n\nFlowRunner <json flow file> [-s]\n\n"
        + "\tUse -s to launch start points sequentially (default launches "
        + "in parallel).");
    } else {
      Settings settings = new Settings("weka", KFDefaults.APP_ID);
      settings.loadSettings();
      settings.applyDefaults(new KFDefaults());
      FlowRunner fr = (FlowRunner) toRun;
      fr.setSettings(settings);

      String fileName = args[0];
      args[0] = "";
      fr.setLaunchStartPointsSequentially(Utils.getFlag("s", args));

      Flow flowToRun = Flow.loadFlow(new File(fileName), new SimpleLogger());

      fr.setFlow(flowToRun);
      fr.run();
      fr.waitUntilFinished();
      fr.m_logHandler.logLow("FlowRunner: Finished all flows.");
      System.exit(0);
    }
  }

  /**
   * Set a callback to notify when flow execution finishes
   *
   * @param callback the callback to notify
   */
  @Override
  public void addExecutionFinishedCallback(ExecutionFinishedCallback callback) {
    if (!m_callbacks.contains(callback)) {
      m_callbacks.add(callback);
    }
  }

  /**
   * Remove a callback
   *
   * @param callback the callback to remove
   */
  @Override
  public void
    removeExecutionFinishedCallback(ExecutionFinishedCallback callback) {
    m_callbacks.remove(callback);
  }

  /**
   * Get the flow to execute
   * 
   * @return the flow to execute
   */
  @Override
  public Flow getFlow() {
    return m_flow;
  }

  /**
   * Set the flow to execute
   *
   * @param flow the flow to execute
   */
  @Override
  public void setFlow(Flow flow) {
    m_flow = flow;
  }

  /**
   * Get the log to use
   * 
   * @return the log to use
   */
  @Override
  public Logger getLogger() {
    return m_log;
  }

  /**
   * Set the log to use
   *
   * @param logger the log to use
   */
  @Override
  public void setLogger(Logger logger) {
    m_log = logger;
    if (m_execEnv != null) {
      m_execEnv.setLog(logger);
    }
  }

  /**
   * Get the logging level to use
   * 
   * @return the logging level to use
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
    if (m_execEnv != null) {
      m_execEnv.setLoggingLevel(level);
    }
  }

  /**
   * Get whether to launch start points sequentially
   * 
   * @return true if start points are to be launched sequentially
   */
  public boolean getLaunchStartPointsSequentially() {
    return m_startSequentially;
  }

  /**
   * Set whether to launch start points sequentially
   *
   * @param s true if start points are to be launched sequentially
   */
  public void setLaunchStartPointsSequentially(boolean s) {
    m_startSequentially = s;
  }

  @Override
  public BaseExecutionEnvironment getExecutionEnvironment() {
    return m_execEnv;
  }

  /**
   * Set the execution environment to use
   *
   * @param env the execution environment to use
   */
  @Override
  public void setExecutionEnvironment(BaseExecutionEnvironment env) {
    m_execEnv = env;
  }

  /**
   * Execute the flow
   *
   * @throws WekaException if a problem occurs
   */
  public void run() throws WekaException {
    if (m_flow == null) {
      throw new WekaException("No flow to execute!");
    }

    if (m_startSequentially) {
      runSequentially();
    } else {
      runParallel();
    }
  }

  /**
   * Initialize the flow ready for execution
   *
   * @return a list of start points in the Flow
   * @throws WekaException if a problem occurs during initialization
   */
  protected List<StepManagerImpl> initializeFlow() throws WekaException {
    m_wasStopped = false;
    if (m_flow == null) {
      m_wasStopped = true;
      for (ExecutionFinishedCallback c : m_callbacks) {
        c.executionFinished();
      }

      throw new WekaException("No flow to execute!");
    }

    m_logHandler = new LogManager(m_log);
    m_logHandler.m_statusMessagePrefix = "FlowRunner$" + hashCode() + "|";
    setLoggingLevel(m_execEnv.getSettings().getSetting(
      KFDefaults.MAIN_PERSPECTIVE_ID, KFDefaults.LOGGING_LEVEL_KEY,
      LoggingLevel.BASIC, Environment.getSystemWide()));
    m_logHandler.setLoggingLevel(m_loggingLevel);

    List<StepManagerImpl> startPoints = m_flow.findPotentialStartPoints();
    if (startPoints.size() == 0) {
      m_wasStopped = true;
      m_logHandler.logError("FlowRunner: there don't appear to be any "
        + "start points to launch!", null);
      for (ExecutionFinishedCallback c : m_callbacks) {
        c.executionFinished();
      }

      return null;
    }

    m_wasStopped = false;
    m_execEnv.startClientExecutionService(m_numThreads,
      m_resourceIntensiveNumThreads);

    if (!m_flow.initFlow(this)) {
      m_wasStopped = true;
      for (ExecutionFinishedCallback c : m_callbacks) {
        c.executionFinished();
      }
      throw new WekaException(
        "Flow did not initializeFlow properly - check log.");
    }

    return startPoints;
  }

  /**
   * Run the flow by launching start points sequentially.
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void runSequentially() throws WekaException {
    List<StepManagerImpl> startPoints = initializeFlow();
    if (startPoints == null) {
      return;
    }
    runSequentially(startPoints);
  }

  /**
   * Run the flow by launching start points in parallel
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void runParallel() throws WekaException {
    List<StepManagerImpl> startPoints = initializeFlow();
    if (startPoints == null) {
      return;
    }
    runParallel(startPoints);
  }

  /**
   * Execute the flow by launching start points sequentially
   *
   * @param startPoints the list of potential start points
   * @throws WekaException if a problem occurs
   */
  protected void runSequentially(List<StepManagerImpl> startPoints)
    throws WekaException {
    m_logHandler.logDetailed("Flow runner: using execution environment - " + ""
      + m_execEnv.getDescription());

    TreeMap<Integer, StepManagerImpl> sortedStartPoints =
      new TreeMap<Integer, StepManagerImpl>();
    List<StepManagerImpl> unNumbered = new ArrayList<StepManagerImpl>();

    for (StepManagerImpl s : startPoints) {
      String stepName = s.getManagedStep().getName();
      if (stepName.startsWith("!")) {
        continue;
      }
      if (stepName.indexOf(":") > 0) {
        try {
          Integer num = Integer.parseInt(stepName.split(":")[0]);
          sortedStartPoints.put(num, s);
        } catch (NumberFormatException ex) {
          unNumbered.add(s);
        }
      } else {
        unNumbered.add(s);
      }
    }

    int biggest = 0;
    if (sortedStartPoints.size() > 0) {
      biggest = sortedStartPoints.lastKey();
    }
    for (StepManagerImpl s : unNumbered) {
      biggest++;
      sortedStartPoints.put(biggest, s);
    }

    for (final StepManagerImpl stepToStart : sortedStartPoints.values()) {
      if (stepToStart.getManagedStep() instanceof Note) {
        continue;
      }

      m_logHandler.logLow("FlowRunner: Launching start point: "
        + stepToStart.getManagedStep().getName());

      m_execEnv.launchStartPoint(stepToStart);
      /*
       * m_execEnv.submitTask(new StepTask<Void>(null) {
       * 
       * /** For serialization * private static final long serialVersionUID =
       * -5466021103296024455L;
       * 
       * @Override public void process() throws Exception {
       * stepToStart.startStep(); } });
       */
    }

    m_logHandler.logDebug("FlowRunner: Launching shutdown monitor");
    launchExecutorShutdownThread();
  }

  /**
   * Execute the flow by launching start points in parallel
   *
   * @param startPoints the list of potential start points to execute
   * @throws WekaException if a problem occurs
   */
  protected void runParallel(List<StepManagerImpl> startPoints)
    throws WekaException {

    m_logHandler.logDetailed("Flow runner: using execution environment - " + ""
      + m_execEnv.getDescription());

    for (final StepManagerImpl startP : startPoints) {
      if (startP.getManagedStep().getName().startsWith("!")
        || startP.getManagedStep() instanceof Note) {
        continue;
      }

      m_logHandler.logLow("FlowRunner: Launching start point: "
        + startP.getManagedStep().getName());

      m_execEnv.launchStartPoint(startP);

      /*
       * m_execEnv.submitTask(new StepTask<Void>(null) { /** For serialization *
       * private static final long serialVersionUID = 663985401825979869L;
       * 
       * @Override public void process() throws Exception { startP.startStep();
       * } });
       */
    }
    m_logHandler.logDebug("FlowRunner: Launching shutdown monitor");
    launchExecutorShutdownThread();
  }

  /**
   * Launch a thread to monitor the progress of the flow, and then shutdown the
   * executor service once all steps have completed.
   */
  protected void launchExecutorShutdownThread() {
    if (m_execEnv != null) {
      Thread shutdownThread = new Thread() {
        @Override
        public void run() {
          waitUntilFinished();
          m_logHandler.logDebug("FlowRunner: Shutting down executor service");
          m_execEnv.stopClientExecutionService();
          for (ExecutionFinishedCallback c : m_callbacks) {
            c.executionFinished();
          }
        }
      };
      shutdownThread.start();
    }
  }

  /**
   * Wait until all the steps are no longer busy
   */
  @Override
  public void waitUntilFinished() {
    try {
      Thread.sleep(800);
      while (true) {
        boolean busy = flowBusy();
        if (busy) {
          Thread.sleep(3000);
        } else {
          break;
        }
      }
    } catch (Exception ex) {
      m_logHandler.logDetailed("FlowRunner: Attempting to stop all steps...");
    }
  }

  /**
   * Checks to see if any step(s) are doing work
   *
   * @return true if one or more steps in the flow are busy
   */
  public boolean flowBusy() {
    boolean busy = false;
    Iterator<StepManagerImpl> iter = m_flow.iterator();
    while (iter.hasNext()) {
      StepManagerImpl s = iter.next();
      if (s.isStepBusy()) {
        m_logHandler.logDebug(s.getName() + " is still busy.");
        busy = true;
      }
    }

    return busy;
  }

  /**
   * Attempt to stop processing in all steps
   */
  @Override
  public synchronized void stopProcessing() {
    Iterator<StepManagerImpl> iter = m_flow.iterator();
    while (iter.hasNext()) {
      iter.next().stopStep();
    }
    System.err.println("Asked all steps to stop...");
    m_wasStopped = true;
  }

  /**
   * Returns true if execution was stopped via the stopProcessing() method
   *
   * @return true if execution was stopped.
   */
  @Override
  public boolean wasStopped() {
    return m_wasStopped;
  }

  @Override
  public void preExecution() throws Exception {
  }

  @Override
  public void postExecution() throws Exception {
  }

  /**
   * A simple logging implementation that writes to standard out
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class SimpleLogger implements weka.gui.Logger {

    /** The date format to use for logging */
    SimpleDateFormat m_DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void logMessage(String lm) {
      System.out.println(m_DateFormat.format(new Date()) + ": " + lm);
    }

    @Override
    public void statusMessage(String lm) {
      // don't output status messages to the console
    }
  }
}
