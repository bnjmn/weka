package weka.knowledgeflow;

import weka.core.Environment;
import weka.core.Settings;
import weka.core.Utils;
import weka.core.WekaException;
import weka.core.WekaPackageManager;
import weka.gui.Logger;
import weka.gui.beans.PluginManager;
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
public class FlowRunner implements FlowExecutor {

  /** The flow to execute */
  protected Flow m_flow;

  /** The execution environment */
  protected transient BaseExecutionEnvironment m_execEnv =
    new BaseExecutionEnvironment();

  /** The log to use */
  protected transient Logger m_log = new SimpleLogger();

  /** Local log handler for FlowRunner-specific logging */
  protected transient LogHandler m_logHandler;

  /** The level to at which to log at */
  protected LoggingLevel m_loggingLevel = LoggingLevel.BASIC;

  /** Invoke start points sequentially? */
  protected boolean m_startSequentially;

  /** Executor service used for launching start points in parallel */
  // protected ExecutorService m_executorService;

  /** Number of worker threads to use in the step executor service */
  protected int m_numThreads =
    BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.STEP_EXECUTOR_SERVICE_NUM_THREADS;

  /**
   * Number of worker threads to use in the resource intensive executor service
   */
  protected int m_resourceIntensiveNumThreads =
    BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.RESOURCE_INTENSIVE_EXECUTOR_SERVICE_NUM_THREADS;

  /** Callback to notify when execution completes */
  protected ExecutionFinishedCallback m_callback;

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

    String execName = settings.getSetting(KFDefaults.APP_ID,
      KnowledgeFlowApp.KnowledgeFlowGeneralDefaults.EXECUTION_ENV_KEY,
      KnowledgeFlowApp.KnowledgeFlowGeneralDefaults.EXECUTION_ENV);
    BaseExecutionEnvironment execE = null;
    try {
      execE = (BaseExecutionEnvironment) PluginManager.getPluginInstance(
        BaseExecutionEnvironment.class.getCanonicalName(), execName);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    if (execE != null) {
      m_execEnv = execE;
    }
    // default execution environment is headless
    m_execEnv = new BaseExecutionEnvironment();
    m_execEnv.setHeadless(true);
    m_execEnv.setFlowExecutor(this);
    m_execEnv.setLog(m_log);
    m_execEnv.setSettings(settings);

    m_numThreads = settings.getSetting(KFDefaults.APP_ID,
            BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.STEP_EXECUTOR_SERVICE_NUM_THREADS_KEY,
            BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.STEP_EXECUTOR_SERVICE_NUM_THREADS);
    m_resourceIntensiveNumThreads = settings.getSetting(KFDefaults.APP_ID,
            BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.RESOURCE_INTENSIVE_EXECUTOR_SERVICE_NUM_THREADS_KEY,
            BaseExecutionEnvironment.BaseExecutionEnvironmentDefaults.RESOURCE_INTENSIVE_EXECUTOR_SERVICE_NUM_THREADS);
  }

  @Override
  public void setSettings(Settings settings) {
    init(settings);
  }

  @Override
  public Settings getSettings() {
    return m_execEnv.getSettings();
  }

  public static void main(String[] args) {
    weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
      "Logging started");
    if (args.length < 1) {
      System.err.println("Usage:\n\nFlowRunner <json flow file> [-s]\n\n"
        + "\tUse -s to launch start points sequentially (default launches "
        + "in parallel).");
    } else {
      try {
        WekaPackageManager.loadPackages(false, true, false);
        Settings settings = new Settings("weka", KFDefaults.APP_ID);
        settings.loadSettings();
        settings.applyDefaults(new KFDefaults());

        FlowRunner fr = new FlowRunner(settings);
        String fileName = args[0];
        args[0] = "";
        fr.setLaunchStartPointsSequentially(Utils.getFlag("s", args));

        Flow toRun = Flow.loadFlow(new File(fileName), new SimpleLogger());
        // Flow.loadFlow(new BufferedReader(new FileReader(fileName)));
        /*
         * String loggingLevel = Environment.getSystemWide().getVariableValue(
         * "weka.knowledgeflow.loggingLevel"); if (loggingLevel != null &&
         * loggingLevel.length() > 0) { LoggingLevel lev =
         * LoggingLevel.stringToLevel(loggingLevel); fr.setLoggingLevel(lev); }
         */
        fr.setFlow(toRun);
        fr.run();
        fr.waitUntilFinished();
        fr.m_logHandler.logLow("FlowRunner: Finished all flows.");
        System.exit(0);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Set a callback to notify when flow execution finishes
   *
   * @param callback the callback to notify
   */
  @Override
  public void setExecutionFinishedCallback(ExecutionFinishedCallback callback) {
    m_callback = callback;
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
  protected void run() throws WekaException {
    if (m_flow == null) {
      throw new WekaException("No flow to execute!");
    }

    m_logHandler = new LogHandler(m_log);
    m_logHandler.setLoggingLevel(m_loggingLevel);
    m_logHandler.m_statusMessagePrefix = "FlowRunner$" + hashCode() + "|";
    List<StepManagerImpl> startPoints = m_flow.findPotentialStartPoints();
    if (startPoints.size() == 0) {
      m_logHandler.logError(
        "FlowRunner: there don't appear to be any " + "start points to launch!",
        null);
      return;
    }

    if (!m_flow.initFlow(this)) {
      throw new WekaException(
        "Flow did not initializeFlow properly - check log.");
    }

    if (m_startSequentially) {
      runSequentially(startPoints);
    } else {
      runParallel(startPoints);
    }
  }

  protected List<StepManagerImpl> initializeFlow() throws WekaException {
    m_wasStopped = false;
    if (m_flow == null) {
      m_wasStopped = true;
      if (m_callback != null) {
        m_callback.executionFinished();
      }
      throw new WekaException("No flow to execute!");
    }

    m_logHandler = new LogHandler(m_log);
    m_logHandler.m_statusMessagePrefix = "FlowRunner$" + hashCode() + "|";
    setLoggingLevel(m_execEnv.getSettings().getSetting(KFDefaults.APP_ID,
      KFDefaults.LOGGING_LEVEL_KEY, LoggingLevel.BASIC,
      Environment.getSystemWide()));
    m_logHandler.setLoggingLevel(m_loggingLevel);

    List<StepManagerImpl> startPoints = m_flow.findPotentialStartPoints();
    if (startPoints.size() == 0) {
      m_wasStopped = true;
      m_logHandler.logError(
        "FlowRunner: there don't appear to be any " + "start points to launch!",
        null);
      if (m_callback != null) {
        m_callback.executionFinished();
      }
      return null;
    }

    m_wasStopped = false;
    m_execEnv.startClientExecutionService(m_numThreads,
      m_resourceIntensiveNumThreads);

    if (!m_flow.initFlow(this)) {
      m_wasStopped = true;
      if (m_callback != null) {
        m_callback.executionFinished();
      }
      throw new WekaException(
        "Flow did not initializeFlow properly - check log.");
    }

    return startPoints;
  }

  @Override
  public void runSequentially() throws WekaException {
    List<StepManagerImpl> startPoints = initializeFlow();
    if (startPoints == null) {
      return;
    }
    runSequentially(startPoints);
  }

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
      /* m_execEnv.submitTask(new StepTask<Void>(null) {

        /** For serialization *
        private static final long serialVersionUID = -5466021103296024455L;

        @Override
        public void process() throws Exception {
          stepToStart.startStep();
        }
      }); */
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

      /* m_execEnv.submitTask(new StepTask<Void>(null) {
        /** For serialization *
        private static final long serialVersionUID = 663985401825979869L;

        @Override
        public void process() throws Exception {
          startP.startStep();
        }
      }); */
    }
    m_logHandler.logDebug("FlowRunner: Launching shutdown monitor");
    launchExecutorShutdownThread();
  }

  protected void launchExecutorShutdownThread() {
    if (m_execEnv != null) {
      Thread shutdownThread = new Thread() {
        @Override
        public void run() {
          waitUntilFinished();
          m_logHandler.logDebug("FlowRunner: Shutting down executor service");
          m_execEnv.stopClientExecutionService();
          if (m_callback != null) {
            m_callback.executionFinished();
          }
        }
      };
      shutdownThread.start();
    }
  }

  /**
   * Wait until all the steps are no longer busy
   */
  private void waitUntilFinished() {
    try {
      Thread.sleep(500);
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
    System.err.println("Stopped all steps...");
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
