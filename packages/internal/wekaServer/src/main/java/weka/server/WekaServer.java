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
 *    WekaServer.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.Password;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.LogHandler;
import weka.core.WekaPackageManager;
import weka.experiment.Task;
import weka.experiment.TaskStatusInfo;
import weka.gui.Logger;
import weka.server.WekaTaskMap.WekaTaskEntry;
import weka.server.logging.ServerLogger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * The main server program that launches tasks (either locally or on registered
 * remote slaves).
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaServer implements CommandlineRunnable {

  /** Server root directory */
  public static final String SERVER_ROOT_DIRECTORY =
    WekaPackageManager.WEKA_HOME.toString() + File.separator + "server"
      + File.separator;

  /** Subdirectory for task persistence */
  public static final String TASK_PERSISTENCE_DIRECTORY = SERVER_ROOT_DIRECTORY
    + "tasks" + File.separator;

  /** Subdirectory for temp */
  public static final String TASK_TEMP_DIRECTORY = SERVER_ROOT_DIRECTORY
    + "tmp" + File.separator;

  static {
    File serverDir = new File(SERVER_ROOT_DIRECTORY);
    if (!serverDir.exists()) {
      if (!serverDir.mkdir()) {
        System.err.println("[WekaServer] Unable to create main "
          + "server directory (" + SERVER_ROOT_DIRECTORY + ")");
      }
    }

    File taskDir = new File(TASK_PERSISTENCE_DIRECTORY);
    if (!taskDir.exists()) {
      if (!taskDir.mkdir()) {
        System.err.println("[WekaServer] Unable to create task "
          + "persistence directory (" + TASK_PERSISTENCE_DIRECTORY + ")");
      }
    }

    File tmpDir = new File(TASK_TEMP_DIRECTORY);
    if (!tmpDir.exists()) {
      if (!tmpDir.mkdir()) {
        System.err.println("[WekaServer] Unable to create task "
          + "temp directory (" + TASK_TEMP_DIRECTORY + ")");
      }
    }
  }

  /** Default port for the server */
  public static final int PORT = 8085;

  /** Tasks on the server */
  protected WekaTaskMap m_taskMap = new WekaTaskMap();

  /** For running tasks */
  protected ThreadPoolExecutor m_executorPool;

  /** The Jetty web server instance */
  protected Server m_jettyServer;

  /** Hostname the server is running on */
  protected String m_hostname;

  /** Port the server is listening on */
  protected int m_port = PORT; // default port

  /** Username for authentication */
  protected String m_username;

  /** Password for authentication */
  protected String m_password;

  /** Run as a daemon */
  protected boolean m_daemon = false; // main method exits and server runs as a
                                      // daemon if true

  /** maximum number of task that will run concurrently */
  protected int m_numExecutionSlots = 2;

  /**
   * How long will each task take compared to the fastest server in the cluster.
   * Default value of 1.0 means that this server is as fast as the fastest. If
   * the fastest server runs at 2GHz, and this one runs at 1.5GHz, set the load
   * adjust to 2/1.5.
   */
  protected double m_loadAdjust = 1.0;

  /** default stale time (1 hour) for unscheduled finished/failed tasks */
  protected long m_staleTime = 3600000;

  /** host:port of master server (if any) to register with) */
  protected String m_master = null;

  /** Map of slaves registered with us */
  protected Map<String, String> m_slaves = new HashMap<String, String>();

  /**
   * Provides singleton access to the Apache commons HTTP connection manager.
   */
  public static class ConnectionManager {

    // singleton
    private static ConnectionManager s_connectionManager;
    private final MultiThreadedHttpConnectionManager m_manager;

    private ConnectionManager() {
      m_manager = new MultiThreadedHttpConnectionManager();
      m_manager.getParams().setDefaultMaxConnectionsPerHost(100);
      m_manager.getParams().setMaxTotalConnections(200);
    }

    public static ConnectionManager getSingleton() {
      if (s_connectionManager == null) {
        s_connectionManager = new ConnectionManager();
      }
      return s_connectionManager;
    }

    public static void addCredentials(HttpClient client, String username,
      String password) {
      if (username != null && username.length() > 0 && password != null
        && password.length() > 0) {
        try {
          username = Environment.getSystemWide().substitute(username);
        } catch (Exception ex) {
        }
        try {
          password = Environment.getSystemWide().substitute(password);
        } catch (Exception ex) {
        }
        // TODO handle encrypted password
        Credentials creds = new UsernamePasswordCredentials(username, password);
        client.getState().setCredentials(AuthScope.ANY, creds);
        client.getParams().setAuthenticationPreemptive(true);
      }
    }

    public void shutdown() {
      s_connectionManager.shutdown();
    }

    public HttpClient createHttpClient() {
      return new HttpClient(m_manager);
    }
  }

  /**
   * Static utility method for serializing a task
   * 
   * @param toSerialize the task to serialize
   * @return an array of bytes
   * @throws Exception if a problem occurs
   */
  public static byte[] serializeTask(Task toSerialize) throws Exception {
    byte[] taskAsBytes = null;

    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    OutputStream os = ostream;
    ObjectOutputStream p;
    p =
      new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(os)));

    p.writeObject(toSerialize);
    p.flush();
    p.close(); // used to be ostream.close() !
    taskAsBytes = ostream.toByteArray();

    return taskAsBytes;
  }

  /**
   * Constructor
   */
  public WekaServer() {
      // Force loading of plugin evaluation metrics. For some reason (probably
      // to do with classloading from static blocks) deserialized Explorer tasks
      // seem to result in linkage errors when plugin metric classes get loaded
      // by the same classloader twice. This doesn't seem to affect plugin classifiers
      // and other schemes.
      weka.classifiers.evaluation.Evaluation.getAllEvaluationMetricNames();
  }

  /**
   * Constructor
   * 
   * @param hostname hostname to run on
   * @param port port to listen on
   * @param daemon true if running as a daemon
   * @param executionSlots the maximum number of tasks to execute in parallel
   */
  public WekaServer(String hostname, int port, boolean daemon,
    int executionSlots) {
    m_hostname = hostname;
    m_port = port;
    m_daemon = daemon;
    m_numExecutionSlots = executionSlots;
  }

  /**
   * Starts the Jetty server
   * 
   * @throws Exception if a problem occurs
   */
  protected void startJettyServer() throws Exception {
    // load any persisted scheduled tasks
    loadTasks();

    if (m_jettyServer != null) {
      throw new Exception("Server is already running. Stop it first.");
    }

    if (m_hostname == null) {
      throw new Exception("No hostname has been specified!!");
    }

    weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
      "Logging started");

    m_jettyServer = new Server();

    String wekaServerPasswordPath =
      WekaPackageManager.WEKA_HOME.toString() + File.separator + "server"
        + File.separator + "weka.pwd";
    File wekaServerPasswordFile = new File(wekaServerPasswordPath);
    boolean useAuth = wekaServerPasswordFile.exists();

    SecurityHandler securityHandler = null;
    if (useAuth) {
      System.out.println("[WekaServer] installing security handler");
      Constraint constraint = new Constraint();
      constraint.setName(Constraint.__BASIC_AUTH);
      constraint.setRoles(new String[] { Constraint.ANY_ROLE });
      constraint.setAuthenticate(true);

      ConstraintMapping constraintMapping = new ConstraintMapping();
      constraintMapping.setConstraint(constraint);
      constraintMapping.setPathSpec("/*");

      securityHandler = new SecurityHandler();
      securityHandler.setUserRealm(new HashUserRealm("WekaServer",
        wekaServerPasswordFile.toString()));
      securityHandler
        .setConstraintMappings(new ConstraintMapping[] { constraintMapping });

      BufferedReader br = null;
      try {
        br = new BufferedReader(new FileReader(wekaServerPasswordFile));
        String line = null;
        while ((line = br.readLine()) != null) {
          // not a comment character, then assume its the data
          if (!line.startsWith("#")) {
            String[] parts = line.split(":");
            if (parts.length > 3 || parts.length < 2) {
              continue;
            }
            m_username = parts[0].trim();
            m_password = parts[1].trim();
            if (parts.length == 3 && parts[1].trim().startsWith("OBF")) {
              m_password = m_password + ":" + parts[2];
              String deObbs = Password.deobfuscate(m_password);
              m_password = deObbs;
            }
            break;
          }
        }
      } catch (Exception ex) {
        System.err.println("[WekaServer} Error reading password file: "
          + ex.getMessage());
      } finally {
        if (br != null) {
          br.close();
        }
      }
    }

    // Servlets
    ContextHandlerCollection contexts = new ContextHandlerCollection();

    // Root context
    Context root =
      new Context(contexts, RootServlet.CONTEXT_PATH, Context.SESSIONS);
    RootServlet rootServlet = new RootServlet(m_taskMap, this);
    root.addServlet(new ServletHolder(rootServlet), "/*");

    // Execute task
    Context executeTask =
      new Context(contexts, ExecuteTaskServlet.CONTEXT_PATH, Context.SESSIONS);
    executeTask.addServlet(new ServletHolder(new ExecuteTaskServlet(m_taskMap,
      this)), "/*");

    // Task status
    Context taskStatus =
      new Context(contexts, GetTaskStatusServlet.CONTEXT_PATH, Context.SESSIONS);
    taskStatus.addServlet(new ServletHolder(new GetTaskStatusServlet(m_taskMap,
      this)), "/*");

    // Purge task
    Context purgeTask =
      new Context(contexts, PurgeTaskServlet.CONTEXT_PATH, Context.SESSIONS);
    purgeTask.addServlet(new ServletHolder(
      new PurgeTaskServlet(m_taskMap, this)), "/*");

    // Add slave
    Context addSlave =
      new Context(contexts, AddSlaveServlet.CONTEXT_PATH, Context.SESSIONS);
    addSlave.addServlet(
      new ServletHolder(new AddSlaveServlet(m_taskMap, this)), "/*");

    // Server load factor
    Context loadFactor =
      new Context(contexts, GetServerLoadServlet.CONTEXT_PATH, Context.SESSIONS);
    loadFactor.addServlet(new ServletHolder(new GetServerLoadServlet(m_taskMap,
      this)), "/*");

    // Set task status (from slave)
    Context setTaskStatus =
      new Context(contexts, SetTaskStatusServlet.CONTEXT_PATH, Context.SESSIONS);
    setTaskStatus.addServlet(new ServletHolder(new SetTaskStatusServlet(
      m_taskMap, this)), "/*");

    // Set last executon for task (from slave)
    Context setLastExecution =
      new Context(contexts, SetLastExecutionServlet.CONTEXT_PATH,
        Context.SESSIONS);
    setLastExecution.addServlet(new ServletHolder(new SetLastExecutionServlet(
      m_taskMap, this)), "/*");

    // Get task list servlet
    Context getTaskList =
      new Context(contexts, GetTaskListServlet.CONTEXT_PATH, Context.SESSIONS);
    getTaskList.addServlet(new ServletHolder(new GetTaskListServlet(m_taskMap,
      this)), "/*");

    // Get task result servlet
    Context getTaskResult =
      new Context(contexts, GetTaskResultServlet.CONTEXT_PATH, Context.SESSIONS);
    getTaskResult.addServlet(new ServletHolder(new GetTaskResultServlet(
      m_taskMap, this)), "/*");

    // Get schedule servlet
    Context getSchedule =
      new Context(contexts, GetScheduleServlet.CONTEXT_PATH, Context.SESSIONS);
    getSchedule.addServlet(new ServletHolder(new GetScheduleServlet(m_taskMap,
      this)), "/*");

    /* // static test servlet
    Context testStatic =
      new Context(contexts, StaticTestServlet.CONTEXT_PATH, Context.SESSIONS);
    testStatic.addServlet(new ServletHolder(new StaticTestServlet(m_taskMap,
      this)), "/*"); */

    m_jettyServer.setHandlers((securityHandler != null) ? new Handler[] {
      securityHandler, contexts } : new Handler[] { contexts });

    // start execution

    SocketConnector connector = new SocketConnector();
    connector.setPort(m_port);
    connector.setHost(m_hostname);
    connector.setName("WekaServer@" + m_hostname);

    m_jettyServer.setConnectors(new Connector[] { connector });

    m_jettyServer.start();
    startExecutorPool();

    // start a purge thread that purges stale tasks
    Thread purgeThread = new Thread() {
      @Override
      public void run() {
        while (true) {
          purgeTasks(m_staleTime);
          try {
            Thread.sleep(m_staleTime);
          } catch (InterruptedException ie) {
          }
        }
      }
    };

    if (m_staleTime > 0) {
      System.out.println("[WekaServer] Starting purge thread.");
      purgeThread.setPriority(Thread.MIN_PRIORITY);
      purgeThread.setDaemon(m_daemon);
      purgeThread.start();
    } else {
      System.out.println("[WekaServer] Purge thread disabled.");
    }

    // start a thread for executing scheduled tasks
    Thread scheduleChecker = new Thread() {
      GregorianCalendar m_cal = new GregorianCalendar();

      @Override
      public void run() {
        while (true) {
          List<WekaTaskEntry> tasks = m_taskMap.getTaskList();
          for (WekaTaskEntry t : tasks) {
            NamedTask task = m_taskMap.getTask(t);
            if (task instanceof Scheduled
              && task.getTaskStatus().getExecutionStatus() != TaskStatusInfo.PROCESSING) {
              // Date lastExecution = m_taskMap.getExecutionTime(t);
              Date lastExecution = t.getLastExecution();
              Schedule schedule = ((Scheduled) task).getSchedule();
              boolean runIt = false;
              try {
                runIt = schedule.execute(lastExecution);
              } catch (Exception ex) {
                System.err
                  .println("[WekaServer] There is a problem with scheduled task "
                    + t.toString() + "\n\n" + ex.getMessage());
              }
              if (runIt) {
                System.out.println("[WekaServer] Starting scheduled task "
                  + t.toString());
                executeTask(t);
              }
            }
          }

          try {
            // check every 60 seconds
            // wait enough seconds to be on the minute
            Date now = new Date();
            m_cal.setTime(now);
            int seconds = (60 - m_cal.get(Calendar.SECOND));
            Thread.sleep(seconds * 1000);
          } catch (InterruptedException ie) {
          }
        }
      }
    };

    System.out.println("[WekaServer] Starting schedule checker.");
    scheduleChecker.setPriority(Thread.MIN_PRIORITY);
    scheduleChecker.setDaemon(m_daemon);
    scheduleChecker.start();

    // Register with a master server?
    if (m_master != null && m_master.length() > 0
      && m_master.lastIndexOf(":") > 0) {
      registerWithMaster();
    }

    if (!m_daemon) {
      m_jettyServer.join();
    }
  }

  /**
   * Register this server instance with a master server
   * 
   * @throws Exception if a problem occurs
   */
  protected void registerWithMaster() throws Exception {
    System.out.println("[WekaServer] Registering as a slave with '" + m_master
      + "'");

    InputStream is = null;
    PostMethod post = null;
    try {

      String url = "http://" + m_master;
      url = url.replace(" ", "%20");
      url += AddSlaveServlet.CONTEXT_PATH;
      url += "/?slave=" + m_hostname + ":" + m_port;
      url += "&client=Y";

      post = new PostMethod(url);
      post.setDoAuthentication(true);
      post.addRequestHeader(new Header("Content-Type", "text/plain"));

      // Get HTTP client
      HttpClient client = ConnectionManager.getSingleton().createHttpClient();
      ConnectionManager.addCredentials(client, m_username, m_password);

      // Execute request
      int result = client.executeMethod(post);
      System.out
        .println("[WekaServer] Response from master server : " + result);
      if (result == 401) {
        System.err.println("[WekaServer] Unable to register with master"
          + " - authentication required.\n");
      } else {

        // the response
        is = post.getResponseBodyAsStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        Object response = ois.readObject();
        if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
          System.err.println("[WekaServer] A problem occurred while "
            + "registering with master server : \n" + "\t"
            + response.toString());
        } else {
          System.out.println("[WekaServer] " + response.toString());
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (is != null) {
        is.close();
      }

      if (post != null) {
        post.releaseConnection();
      }
    }
  }

  /**
   * Stop the server
   */
  public void stopServer() {
    try {
      if (m_executorPool != null) {
        m_executorPool.shutdown();
      }

      if (m_jettyServer != null) {
        m_jettyServer.stop();

        weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
          m_jettyServer.getConnectors()[0].getName() + " stopped.");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Start the executor pool
   */
  protected void startExecutorPool() {
    if (m_executorPool != null) {
      m_executorPool.shutdownNow();
    }

    m_executorPool =
      new ThreadPoolExecutor(m_numExecutionSlots, m_numExecutionSlots, 120,
        TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
  }

  /**
   * Excecute a task
   * 
   * @param entry the task to execute
   */
  protected synchronized void executeTask(final WekaTaskEntry entry) {

    final NamedTask task = m_taskMap.getTask(entry);
    if (task == null) {
      System.err
        .println("[WekaServer] Asked to execute an non-existent task! ("
          + entry.toString() + ")");
      return;
    }

    String hostToUse = chooseExecutionHost();

    if (task instanceof LogHandler) {
      Logger log = ((LogHandler) task).getLog();
      log.logMessage("Starting task \"" + entry.toString() + "\" (" + hostToUse
        + ")");
    }

    entry.setServer(hostToUse);
    if (hostToUse.equals(m_hostname + ":" + m_port)) {
      Runnable toRun = new Runnable() {
        @Override
        public void run() {

          Date startTime = new Date();
          GregorianCalendar cal = new GregorianCalendar();
          cal.setTime(startTime);

          // We only use resolution down to the minute level
          cal.set(Calendar.SECOND, 0);
          cal.set(Calendar.MILLISECOND, 0);
          // m_taskMap.setExecutionTime(entry, startTime);
          entry.setLastExecution(cal.getTime());
          if (entry.getCameFromMaster()) {
            // Talk back to the master - tell it the execution time,
            // and that the task is now processing
            sendExecutionTimeToMaster(entry);
            sendTaskStatusInfoToMaster(entry, TaskStatusInfo.PROCESSING);
          }

          // ask the task to load any resources (if necessary)
          task.loadResources();
          task.execute();

          // save this task so that we have the last execution
          // time recorded
          // if (task instanceof Scheduled) {
          persistTask(entry, task);

          // save memory (if possible)
          task.persistResources();

          // }

          if (entry.getCameFromMaster()) {
            // Talk back to the master - pass on the actual final execution
            // status
            sendTaskStatusInfoToMaster(entry, task.getTaskStatus()
              .getExecutionStatus());
          }
        }
      };

      if (entry.getCameFromMaster()) {
        // Talk back to the master - tell it that this
        // task is pending (WekaTaskMap.WekaTaskEntry.PENDING)
        sendTaskStatusInfoToMaster(entry, WekaTaskMap.WekaTaskEntry.PENDING);
      }
      m_executorPool.execute(toRun);
    } else {
      if (!executeTaskRemote(entry, task, hostToUse)) {
        // failed to hand off to slave for some reason
        System.err.println("[WekaServer] Failed to hand task '"
          + entry.toString() + "' to slave server ('" + hostToUse + ")");
        System.out.println("[WekaServer] removing '" + hostToUse + "' from "
          + "list of slaves.");
        m_slaves.remove(hostToUse);
        System.out.println("[WekaServer] Re-trying execution of task '"
          + entry.toString() + "'");
        executeTask(entry);
      }
    }
  }

  /**
   * Execute a task on a slave server
   * 
   * @param entry the task entry for the task
   * @param task the task to execute
   * @param slave the slave to execute on
   * @return true if the task is sent to the slave successfully
   */
  protected synchronized boolean executeTaskRemote(WekaTaskEntry entry,
    NamedTask task, String slave) {

    InputStream is = null;
    PostMethod post = null;
    boolean success = true;
    NamedTask originalTask = task;

    if (task instanceof Scheduled) {
      // scheduling checks are run on this local server. So wrap this
      // scheduled task up in an unscheduled instance for this single
      // execution on the slave
      NamedTask oneTime = new WekaTaskMap.NamedClassDelegator(task);
      task = oneTime;
    }

    try {
      // make sure that the task loads any resources it needs
      // before we pass it on
      task.loadResources();

      byte[] serializedTask = WekaServer.serializeTask(task);
      String url = "http://" + slave;
      url = url.replace(" ", "%20");
      url += ExecuteTaskServlet.CONTEXT_PATH;
      url += "/?client=Y&master=Y";
      post = new PostMethod(url);
      RequestEntity entity = new ByteArrayRequestEntity(serializedTask);
      post.setRequestEntity(entity);

      post.setDoAuthentication(true);
      post.addRequestHeader(new Header("Content-Type",
        "application/octect-stream"));

      // Get HTTP client
      HttpClient client = ConnectionManager.getSingleton().createHttpClient();
      ConnectionManager.addCredentials(client, m_username, m_password);

      // Execute request
      int result = client.executeMethod(post);
      System.out.println("[WekaServer] Executing task on slave server : "
        + slave);
      System.out.println("[WekaServer] Sending " + serializedTask.length
        + " bytes...");
      System.out.println("[WekaServer] Response from slave : " + result);

      if (result == 401) {
        System.err.println("[WekaServer] Unable to send task to "
          + "slave server (" + slave + ") - authentication required.\n");
        success = false;
      } else {
        is = post.getResponseBodyAsStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        // System.out.println("Number of bytes in response " + ois.available());
        Object response = ois.readObject();

        if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
          System.err
            .println("[WekaServer] A problem occurred at the slave sever : \n"
              + "\t" + response.toString());
        }
        entry.setRemoteID(response.toString());
        entry.setServer(slave);

        success = true;

        // this will not necessarily capture the execution time of
        // this task as it might get queued at the slave...
        if (originalTask != null) {
          persistTask(entry, originalTask);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      success = false;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      if (post != null) {
        post.releaseConnection();
      }

      // save memory (if possible)
      task.persistResources();
    }

    return success;
  }

  /**
   * Choose a slave (or us if no slave is available) to execute on
   * 
   * @return the name of the server to execute on
   */
  protected String chooseExecutionHost() {
    // start with the local server and load
    String host = m_hostname + ":" + m_port;
    double minLoad = getServerLoad();
    // double minLoad = 2.0;

    // run locally if we have some capacity (load < 1)
    if (m_slaves.size() > 0 && minLoad >= 1.0) {
      for (String slave : m_slaves.keySet()) {
        double load = RootServlet.getSlaveLoad(this, slave);
        System.out
          .println("[WekaServer] load of slave : " + slave + " " + load);
        if (load >= 0 && load < minLoad) {
          minLoad = load;
          host = slave;
        }
      }
    }
    return host;
  }

  /**
   * Container for a task
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class TaskHolder implements Serializable {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 154246714518322803L;
    protected WekaTaskEntry m_taskEntry;
    protected NamedTask m_task;

    public TaskHolder(WekaTaskEntry entry, NamedTask task) {
      m_taskEntry = entry;
      m_task = task;
    }

    public WekaTaskEntry getTaskEntry() {
      return m_taskEntry;
    }

    public NamedTask getTask() {
      return m_task;
    }
  }

  /**
   * Persist a task to disk
   * 
   * @param entry the task entry for the task to persist
   * @param task the task to persist
   */
  protected void persistTask(WekaTaskEntry entry, NamedTask task) {
    try {
      if (!checkPersistenceSubDir()) {
        return;
      }

      TaskHolder holder = new TaskHolder(entry, task);
      File persist =
        new File(TASK_PERSISTENCE_DIRECTORY + m_hostname + "_" + m_port
          + File.separator + entry.toString());
      ObjectOutputStream oos =
        new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
          persist)));
      oos.writeObject(holder);
      oos.flush();
      oos.close();
      oos = null;
    } catch (IOException ex) {
      ex.printStackTrace();
      System.err.println("[WekaServer] Problem persisting task: "
        + entry.toString());
    }
  }

  /**
   * Get a temp file
   * 
   * @return a temp file
   * @throws Exception if a problem occurs
   */
  public static File getTempFile() throws Exception {
    File tmpDir = new File(TASK_TEMP_DIRECTORY);

    if (!tmpDir.exists()) {
      if (!tmpDir.mkdir()) {
        throw new Exception("Unable to create task temp directory!");
      }
    }

    File tmpFile =
      new File(TASK_TEMP_DIRECTORY + UUID.randomUUID().toString().trim());

    return tmpFile;
  }

  /**
   * Send the last time of execution of the supplied task to the master server
   * 
   * @param entry the task entry for the task in question
   */
  protected void sendExecutionTimeToMaster(final WekaTaskEntry entry) {

    Thread t = new Thread() {
      @Override
      public void run() {

        PostMethod post = null;
        InputStream is = null;
        try {
          String url = "http://" + m_master;
          url = url.replace(" ", "%20");
          url += SetLastExecutionServlet.CONTEXT_PATH;
          url += "/?name=" + entry.toString();
          SimpleDateFormat sdf =
            new SimpleDateFormat(SetLastExecutionServlet.DATE_FORMAT);
          String formattedDate = sdf.format(entry.getLastExecution());
          url += "&lastExecution=" + formattedDate;

          post = new PostMethod(url);
          post.setDoAuthentication(true);
          post.addRequestHeader(new Header("Content-Type", "text/plain"));

          // Get HTTP client
          HttpClient client =
            ConnectionManager.getSingleton().createHttpClient();
          ConnectionManager.addCredentials(client, m_username, m_password);

          // Execute request
          int result = client.executeMethod(post);
          // System.out.println("[WekaServer] Response from master server : " +
          // result);
          if (result == 401) {
            System.err
              .println("[WekaServer] Unable to send task last execution time back "
                + "to master - authentication required.\n");
          } else {

            // the response
            is = post.getResponseBodyAsStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            Object response = ois.readObject();
            if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
              System.err
                .println("[WekaServer] A problem occurred while "
                  + "trying to send task last execution timeback to master server : \n"
                  + "\t" + response.toString());
            } else {
              // System.out.println("[WekaServer] " + response.toString());
            }
          }
        } catch (Exception ex) {
          System.err
            .println("[WekaServer] A problem occurred while "
              + "trying to send task last execution time back to master server : \n"
              + ex.getMessage());
          ex.printStackTrace();
        } finally {
          if (is != null) {
            try {
              is.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }

          if (post != null) {
            post.releaseConnection();
          }
        }
      }
    };

    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }

  /**
   * Send status info for the supplied task back to the master server
   * 
   * @param entry the task entry for the task in question
   * @param taskStatus the status to send
   */
  protected void sendTaskStatusInfoToMaster(final WekaTaskEntry entry,
    final int taskStatus) {

    Thread t = new Thread() {
      @Override
      public void run() {
        PostMethod post = null;
        InputStream is = null;
        try {
          String url = "http://" + m_master;
          url = url.replace(" ", "%20");
          url += SetTaskStatusServlet.CONTEXT_PATH;
          url += "/?name=" + entry.toString();
          url += "&status=" + taskStatus;

          post = new PostMethod(url);
          post.setDoAuthentication(true);
          post.addRequestHeader(new Header("Content-Type", "text/plain"));

          // Get HTTP client
          HttpClient client =
            ConnectionManager.getSingleton().createHttpClient();
          ConnectionManager.addCredentials(client, m_username, m_password);

          // Execute request
          int result = client.executeMethod(post);
          // System.out.println("[WekaServer] SendTaskStatus: Response from master server : "
          // + result);
          if (result == 401) {
            System.err
              .println("[WekaServer] Unable to send task status back to master"
                + " - authentication required.\n");
          } else {

            // the response
            is = post.getResponseBodyAsStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            Object response = ois.readObject();
            if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
              System.err.println("[WekaServer] A problem occurred while "
                + "trying to send task status back to master server : \n"
                + "\t" + response.toString());
            } else {
              // System.out.println("[WekaServer] " + response.toString());
            }
          }
        } catch (Exception ex) {
          System.err.println("[WekaServer] A problem occurred while "
            + "trying to send task status back to master server : \n"
            + ex.getMessage());
          ex.printStackTrace();
        } finally {
          if (is != null) {
            try {
              is.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }

          if (post != null) {
            post.releaseConnection();
          }
        }
      }
    };

    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }

  /**
   * Purge any old tasks
   * 
   * @param purgeInterval the interval in milliseconds beyond which a finished
   *          task should be purged
   */
  protected void purgeTasks(long purgeInterval) {
    Date now = new Date();
    long nowMilli = now.getTime();
    boolean doPurge = false;

    List<WekaTaskEntry> taskList = m_taskMap.getTaskList();
    for (WekaTaskEntry t : taskList) {
      NamedTask task = m_taskMap.getTask(t);
      doPurge = false;
      if (!(task instanceof Scheduled)) {
        // Date lastExecuted = getExecutionTime(t);
        Date lastExecuted = t.getLastExecution();
        if (lastExecuted != null) {
          if (task.getTaskStatus().getExecutionStatus() == TaskStatusInfo.PROCESSING) {
            // don't purge executing tasks!!
            continue;
          }
          long milli = lastExecuted.getTime();

          // leave tasks that were sent to us from another server for twice as
          // long in
          // order to give the master a chance to tell us to purge them
          long pI =
            (t.getCameFromMaster() ? (purgeInterval * 2) : purgeInterval);

          if (nowMilli - milli > pI) {
            doPurge = true;
          }
        }
      } else {
        Date lastExecuted = t.getLastExecution();
        Date nextExecution =
          ((Scheduled) task).getSchedule().nextExecution(lastExecuted);
        if (nextExecution == null && lastExecuted != null) {
          long milli = lastExecuted.getTime();

          // leave tasks that were sent to us from another server for twice as
          // long in
          // order to give the master a chance to tell us to purge them
          long pI =
            (t.getCameFromMaster() ? (purgeInterval * 2) : purgeInterval);

          if (nowMilli - milli > pI) {
            doPurge = true;
          }
        }
      }

      if (doPurge) {
        PostMethod post = null;
        InputStream is = null;

        try {
          String url = "http://" + getHostname() + ":" + getPort();
          url = url.replace(" ", "%20");
          url += PurgeTaskServlet.CONTEXT_PATH;
	  url += "/?name=" + URLEncoder.encode(t.toString(), "UTF-8");
          url += "&client=Y";

          post = new PostMethod(url);
          post.setDoAuthentication(true);
          post.addRequestHeader(new Header("Content-Type", "text/plain"));

          // Get HTTP client
          HttpClient client =
            ConnectionManager.getSingleton().createHttpClient();
          ConnectionManager.addCredentials(client, m_username, m_password);

          // Execute request
          int result = client.executeMethod(post);
          // System.out.println("[WekaServer] Response from master server : " +
          // result);
          if (result == 401) {
            System.err.println("[WekaServer] Unable to purge task"
              + " - authentication required.\n");
          } else {
            // the response
            is = post.getResponseBodyAsStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            Object response = ois.readObject();
            if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
              System.err.println("[WekaServer] A problem occurred while "
                + "trying to purge task (" + t.toString() + "): \n" + "\t"
                + response.toString());
            } else {
              System.out.println("[WekaServer] purged task: " + t.toString());
            }
          }
        } catch (Exception ex) {
          System.err
            .println("[WekaServer] A problem occurred while "
              + "trying to purge task (" + t.toString() + "): "
              + ex.getMessage());
          ex.printStackTrace();
        } finally {
          if (is != null) {
            try {
              is.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }

          if (post != null) {
            post.releaseConnection();
          }
        }
      }
    }
  }

  /**
   * Cleanup a given task's persistence directory
   * 
   * @param entry the task entry for the task to cleanup
   */
  protected void cleanupTask(WekaTaskEntry entry) {

    if (!checkPersistenceSubDir()) {
      return;
    }

    // try and remove the persisted task
    File persist =
      new File(TASK_PERSISTENCE_DIRECTORY + m_hostname + "_" + m_port
        + File.separator + entry.toString());

    if (persist.exists()) {
      if (!persist.delete()) {
        persist.deleteOnExit();
      }
    }
  }

  /**
   * Load any persisted tasks
   */
  private void loadTasks() {
    try {
      if (!checkPersistenceSubDir()) {
        return;
      }
      File persistDir =
        new File(TASK_PERSISTENCE_DIRECTORY + m_hostname + "_" + m_port
          + File.separator);
      File[] contents = persistDir.listFiles();
      /*
       * System.out.println("Checking persisted tasks...");
       * System.out.println("Directory contains " + contents.length + " files");
       */
      for (File f : contents) {
        System.out.println(f.toString());
        ObjectInputStream ois =
          new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));
        Object holder = ois.readObject();
        ois.close();
        ois = null;

        if (holder instanceof TaskHolder) {
          WekaTaskEntry entry = ((TaskHolder) holder).getTaskEntry();
          NamedTask task = ((TaskHolder) holder).getTask();

          // set the originating server to this WekaServer instance so that the
          // logging object
          // can create the appropriate logging subdirectory (if necessary)
          entry.setOriginatingServer(getHostname() + ":" + getPort());

          // set a log (since logs are transient)
          if (task instanceof LogHandler) {
            ServerLogger sl = new ServerLogger(entry);
            ((LogHandler) task).setLog(sl);

            // try and load any historical log entries
            sl.loadLog();

            // ask the task to persist any resources
            task.persistResources();

            m_taskMap.addTask(entry, task);

            if (!(task instanceof Scheduled)) {
              if (entry.getLastExecution() == null) {
                // was not executed previously, so execute now
                executeTask(entry);
              }
            }
          }
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Check and create the persistence subdirectory if necessary
   * 
   * @return true if the directory exists (or was created successfully)
   */
  private boolean checkPersistenceSubDir() {
    File persistSubD =
      new File(TASK_PERSISTENCE_DIRECTORY + m_hostname + "_" + m_port);
    if (!persistSubD.exists()) {
      if (!persistSubD.mkdir()) {
        System.err
          .println("[WekaServer] Can't create task persistence subdirectory ("
            + persistSubD.toString() + ")");
        return false;
      }
    }
    return true;
  }

  /**
   * Get the number of running tasks
   * 
   * @return the number of running tasks
   */
  public int numRunningTasks() {
    if (m_executorPool == null) {
      return -1;
    }

    return m_executorPool.getActiveCount();
  }

  /**
   * Get the number of queued tasks
   * 
   * @return the number of queued tasks
   */
  public int numQueuedTasks() {
    if (m_executorPool == null) {
      return -1;
    }

    return m_executorPool.getQueue().size();
  }

  /**
   * Get the load on this server (num running + num queued) * load adjust / num
   * execution slots
   * 
   * @return the load on this server
   */
  public double getServerLoad() {
    return (((double) numRunningTasks() + (double) numQueuedTasks())
      * m_loadAdjust / m_numExecutionSlots);
  }

  /**
   * Get the hostname for this server
   * 
   * @return the hostname
   */
  public String getHostname() {
    return m_hostname;
  }

  /**
   * Set the hostname for this server
   * 
   * @param hostname the hostname
   */
  public void setHostname(String hostname) {
    m_hostname = hostname;
  }

  /**
   * Get the port for this server
   * 
   * @return the port
   */
  public int getPort() {
    return m_port;
  }

  /**
   * Set the port for this server
   * 
   * @param port the port
   */
  public void setPort(int port) {
    m_port = port;
  }

  /**
   * Get the username for authentication
   * 
   * @return the username
   */
  protected String getUsername() {
    return m_username;
  }

  /**
   * Get the password for authentication
   * 
   * @return the password
   */
  protected String getPassword() {
    return m_password;
  }

  /**
   * Get the embedded Jetty server
   * 
   * @return the embedded Jetty server
   */
  public Server getServer() {
    return m_jettyServer;
  }

  /**
   * Set whether this server should run as a daemon
   * 
   * @param daemon true if the server should run as a daemon
   */
  public void setDaemon(boolean daemon) {
    m_daemon = daemon;
  }

  /**
   * Get whether this server should run as a daemon
   * 
   * @return true if running as a daemon
   */
  public boolean getDaemon() {
    return m_daemon;
  }

  /**
   * Set the number of parallel execution slots to use
   * 
   * @param slots the number of slots to use
   */
  public void setNumExecutionSlots(int slots) {
    m_numExecutionSlots = slots;
  }

  /**
   * Get the number of parallel execution slots to use
   * 
   * @return the number of slots to use
   */
  public int getNumExecutionSlots() {
    return m_numExecutionSlots;
  }

  /**
   * Set the stale task time (in milliseconds)
   * 
   * @param interval the stale task time
   */
  public void setStaleTaskTime(long interval) {
    m_staleTime = interval;
  }

  /**
   * Get the stale task time (in milliseconds)
   * 
   * @return the stale task time
   */
  public long getStaleTaskTime() {
    return m_staleTime;
  }

  /**
   * Set the load adjust for this server
   * 
   * @param adjust the load adjust factor
   */
  public void setLoadAdjust(double adjust) {
    m_loadAdjust = adjust;
  }

  /**
   * Get the load adjust for this server
   * 
   * @return the load adjust factor
   */
  public double getLoadAdjust() {
    return m_loadAdjust;
  }

  /**
   * Set the master server (if this server is a slave)
   * 
   * @param master the master server
   */
  public void setMaster(String master) {
    m_master = master;
  }

  /**
   * Get the master server (if this server is a slave)
   * 
   * @return the master server
   */
  public String getMaster() {
    return m_master;
  }

  /**
   * Add a slave server
   * 
   * @param slave the slave to add
   */
  protected void addSlave(String slave) {
    m_slaves.put(slave, slave);
  }

  /**
   * Remove a slave server
   * 
   * @param slave the slave to add
   * @return true if the slave was successfully removed
   */
  protected boolean removeSlave(String slave) {
    String removed = m_slaves.remove(slave);

    return (removed != null);
  }

  /**
   * Get the slaves that have reported to this server
   * 
   * @return a set of slaves
   */
  protected Set<String> getSlaves() {
    return m_slaves.keySet();
  }

  /**
   * Create a command line usage string
   * 
   * @return a command line usage string
   */
  public static String commandLineUsage() {
    return "Usage: WekaServer [-host <hostname>] [-port <port>] "
      + "[-slots <numSlots>] [-load-adjust <value>] [-daemon] "
      + "[-master <master:port>] [-staleTime <milliseconds>]";
  }

  @Override
  public void preExecution() throws Exception {

  }

  @Override
  public void run(Object toRun, String[] args) throws IllegalArgumentException {
    if (!(toRun instanceof WekaServer)) {
      throw new IllegalArgumentException(
        "Supplied object is not a WekaServer!!");
    }
    WekaServer server = (WekaServer) toRun;

    try {
      if (args.length > 0
        && (args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("-help"))) {
        System.out.println(WekaServer.commandLineUsage());
        System.exit(0);
      }

      String hostname = null;
      String port = null;
      boolean daemon = false;
      int numSlots = -1;
      double loadAdjust = 0.0;
      String master = null;
      long purgeInterval = 0;

      // process options
      for (int i = 0; i < args.length; i++) {
        if (args[i].equalsIgnoreCase("-host")) {
          if (++i == args.length) {
            System.out.println(WekaServer.commandLineUsage());
            System.exit(1);
          }
          hostname = args[i];
        } else if (args[i].equalsIgnoreCase("-port")) {
          if (++i == args.length) {
            System.out.println(WekaServer.commandLineUsage());
            System.exit(1);
          }
          port = args[i];
        } else if (args[i].equalsIgnoreCase("-slots")) {
          if (++i == args.length) {
            System.out.println(WekaServer.commandLineUsage());
            System.exit(1);
          }
          numSlots = Integer.parseInt(args[i]);
        } else if (args[i].equalsIgnoreCase("-load-adjust")) {
          if (++i == args.length) {
            System.out.println(WekaServer.commandLineUsage());
            System.exit(1);
          }
          loadAdjust = Double.parseDouble(args[i]);
        } else if (args[i].equalsIgnoreCase("-master")) {
          if (++i == args.length) {
            System.out.println(WekaServer.commandLineUsage());
            System.exit(1);
          }
          master = args[i];
        } else if (args[i].equalsIgnoreCase("-staleTime")) {
          if (++i == args.length) {
            System.out.println(WekaServer.commandLineUsage());
            System.exit(1);
          }
          purgeInterval = Long.parseLong(args[i]);
        } else if (args[i].equalsIgnoreCase("-daemon")) {
          daemon = true;
        } else {
          System.out.println(WekaServer.commandLineUsage());
          System.exit(1);
        }
      }

      InetAddress localhost = null;
      if (hostname == null) {
        // try to get the hostname
        try {
          localhost = InetAddress.getLocalHost();
          if (localhost != null) {
            System.out.println("Host name: " + localhost.getHostName());
            hostname = localhost.getHostName();
          } else {
            hostname = "localhost";
          }
        } catch (Exception ex) {
        }
      }

      if (port == null) {
        port = "" + WekaServer.PORT;
      }

      server.setHostname(hostname);
      server.setPort(Integer.parseInt(port));
      if (numSlots > 0) {
        server.setNumExecutionSlots(numSlots);
      }
      if (loadAdjust > 0) {
        server.setLoadAdjust(loadAdjust);
      }
      if (purgeInterval != 0) {
        server.setStaleTaskTime(purgeInterval);
      }
      server.setDaemon(daemon);
      server.setMaster(master);

      server.startJettyServer();

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public void postExecution() throws Exception {

  }

  /**
   * Main method for launching the server
   * 
   * @param args command line arguments
   */
  public static void main(String[] args) {
    try {
      WekaServer server = new WekaServer();
      server.run(server, args);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
    }
  }
}
