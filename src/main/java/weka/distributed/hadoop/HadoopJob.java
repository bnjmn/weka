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
 *    HadoopJob
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.TaskCompletionEvent;
import org.apache.hadoop.mapreduce.Job;

import weka.core.ClassloaderUtil;
import weka.core.Environment;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.WekaPackageManager;
import weka.distributed.DistributedWekaException;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.hadoop.HDFSUtils;
import distributed.hadoop.MapReduceJobConfig;

/**
 * Abstract base class for Hadoop jobs. Contains routines for installing Weka
 * libraries in HDFS, running jobs and getting status information on running
 * jobs.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class HadoopJob extends DistributedJob implements OptionHandler {

  /** For serialization */
  private static final long serialVersionUID = -9026086203818342364L;

  /** The path to the distributedWekaHadoop.jar */
  public static final String DISTRIBUTED_WEKA_HADOOP_JAR = WekaPackageManager.PACKAGES_DIR
    .toString()
    + File.separator
    + "distributedWekaHadoop"
    + File.separator
    + "distributedWekaHadoop.jar";

  /** The path to the distributedWekaBase.jar */
  public static final String DISTRIBUTED_WEKA_BASE_JAR = WekaPackageManager.PACKAGES_DIR
    .toString()
    + File.separator
    + "distributedWekaBase"
    + File.separator
    + "distributedWekaBase.jar";

  /** The path to the opencsv.jar */
  public static final String OPEN_CSV_JAR = WekaPackageManager.PACKAGES_DIR
    .toString()
    + File.separator
    + "distributedWekaBase"
    + File.separator
    + "lib" + File.separator + "opencsv-2.3.jar";

  /**
   * A default path to a weka.jar file. If the classpath contains a weka.jar
   * file (rather than a directory of weka classes) when Weka is started then
   * this path will be populated automatically by scanning for weka.jar in the
   * classpath.
   */
  protected static String DEFAULT_WEKA_JAR_PATH = System
    .getProperty("user.home") + File.separator + "weka.jar";

  // Attempt to locate the weka.jar in the classpath and set a
  // the default path to it
  static {
    try {
      ClassLoader cl = ClassloaderUtil.class.getClassLoader();
      if (cl instanceof URLClassLoader) {
        URL[] urls = ((URLClassLoader) cl).getURLs();

        for (URL u : urls) {
          if (u.toString().endsWith("weka.jar")) {
            File f = new File(u.toURI());
            DEFAULT_WEKA_JAR_PATH = f.toString();
          }
        }
      }
    } catch (Exception ex) {

    }
  }

  /** Holds the path to the weka.jar */
  protected String m_pathToWekaJar = DEFAULT_WEKA_JAR_PATH;

  /** The main configuration object for this job */
  protected MapReduceJobConfig m_mrConfig = new MapReduceJobConfig();

  /** interval (seconds) between status updates for the running job */
  protected String m_loggingInterval = "10";

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();
    Enumeration<Option> confOpts = m_mrConfig.listOptions();

    options.addElement(new Option("\tPath to the weka.jar file", "weka-jar", 1,
      "-weka-jar <path to weka.jar>"));

    options.addElement(new Option("\tAdditional Weka packages to use.",
      "weka-packages", 1,
      "-weka-packages <comma-separated list of package names>"));
    options.addElement(new Option(
      "\tLogging interval in seconds (default = 15).", "logging-interval", 1,
      "-logging-interval <seconds>"));

    while (confOpts.hasMoreElements()) {
      options.addElement(confOpts.nextElement());
    }

    return options.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    String wekaPath = Utils.getOption("weka-jar", options);
    if (!DistributedJobConfig.isEmpty(wekaPath)) {
      setPathToWekaJar(wekaPath);
    }

    String additionalPackages = Utils.getOption("weka-packages", options);
    setAdditionalWekaPackages(additionalPackages);

    String logInt = Utils.getOption("logging-interval", options);
    setLoggingInterval(logInt);

    m_mrConfig.setOptions(options);
  }

  /**
   * Return the base options only (not the subclasses options or the options
   * specific to the configuration)
   * 
   * @return just the base options
   */
  public String[] getBaseOptionsOnly() {
    List<String> options = new ArrayList<String>();

    if (!DistributedJobConfig.isEmpty(getPathToWekaJar())) {
      options.add("-weka-jar");
      options.add(getPathToWekaJar());
    }

    if (!DistributedJobConfig.isEmpty(getAdditionalWekaPackages())) {
      options.add("-weka-packages");
      options.add(getAdditionalWekaPackages());
    }

    if (!DistributedJobConfig.isEmpty(getLoggingInterval())) {
      options.add("-logging-interval");
      options.add(getLoggingInterval());
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    String[] baseOptions = getBaseOptionsOnly();
    for (String b : baseOptions) {
      options.add(b);
    }

    String[] configOpts = m_mrConfig.getOptions();
    for (String o : configOpts) {
      options.add(o);
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Constructor for a HadoopJob
   * 
   * @param jobName the name of the job
   * @param jobDescription a short description of the job
   */
  public HadoopJob(String jobName, String jobDescription) {
    super(jobName, jobDescription);
  }

  /**
   * Set the main configuration to use with this job
   * 
   * @param conf the main configuration to use with this job
   */
  public void setMapReduceJobConfig(MapReduceJobConfig conf) {
    m_mrConfig = conf;
  }

  /**
   * Get the main configuration to use with this job
   * 
   * @return the main configuration to use with this job
   */
  public MapReduceJobConfig getMapReduceJobConfig() {
    return m_mrConfig;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String pathToWekaJarTipText() {
    return "The path to the weka jar file. This will get installed in"
      + "HDFS and placed into the classpath for map and reduce tasks";
  }

  /**
   * Set the path to the weka.jar file. Will be populated automatically if the
   * classpath contains a weka.jar. The weka.jar is installed in HDFS and used
   * in the classpath for map and reduce tasks.
   * 
   * @param path the path to the weka.jar.
   */
  public void setPathToWekaJar(String path) {
    m_pathToWekaJar = path;
  }

  /**
   * Get the path to the weka.jar file. Will be populated automatically if the
   * classpath contains a weka.jar. The weka.jar is installed in HDFS and used
   * in the classpath for map and reduce tasks.
   * 
   * @return the path to the weka.jar.
   */
  public String getPathToWekaJar() {
    return m_pathToWekaJar;
  }

  /**
   * Tip text for this property.
   * 
   * @return the tip text for this property.
   */
  public String additionalWekaPackagesTipText() {
    return "A list of comma separated weka package names to use with the job. "
      + "Any jar files in the main package directory and the lib "
      + "directory of each package will get installed in HDFS and "
      + "placed in the classpath of map and reduce tasks.";
  }

  /**
   * Set a comma separated list of the names of additional weka packages to use
   * with the job. Any jar files in the main package directory and the lib
   * directory of the package will get installed in HDFS and placed in the
   * classpath of map and reduce tasks
   * 
   * @param packages a comma separated list of weka packages to use with the job
   */
  public void setAdditionalWekaPackages(String packages) {
    m_mrConfig.setUserSuppliedProperty(
      DistributedJob.WEKA_ADDITIONAL_PACKAGES_KEY, packages);
  }

  /**
   * Get a comma separated list of the names of additional weka packages to use
   * with the job. Any jar files in the main package directory and the lib
   * directory of the package will get installed in HDFS and placed in the
   * classpath of map and reduce tasks
   * 
   * @return a comma separated list of weka packages to use with the job
   */
  public String getAdditionalWekaPackages() {
    return m_mrConfig
      .getUserSuppliedProperty(DistributedJob.WEKA_ADDITIONAL_PACKAGES_KEY);
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String loggingIntervalTipText() {
    return "The interval (in seconds) between output of logging information"
      + " from running jobs";
  }

  /**
   * Set the interval between output of logging information from running jobs.
   * 
   * @param li the interval (in seconds) between output of logging information
   */
  public void setLoggingInterval(String li) {
    m_loggingInterval = li;
  }

  /**
   * Get the interval between output of logging information from running jobs.
   * 
   * @return the interval (in seconds) between output of logging information
   */
  public String getLoggingInterval() {
    return m_loggingInterval;
  }

  /**
   * Installs the core weka library and the distributed weka libraries in HDFS.
   * Also adds the libraries to the classpath for map and reduce tasks by
   * populating the appropriate properties in the supplied Hadoop Configuration
   * object.
   * 
   * @param conf the Configuration object to populate
   * @throws IOException if a problem occurs
   */
  protected void installWekaLibrariesInHDFS(Configuration conf)
    throws IOException {
    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    if (m_pathToWekaJar == null
      || DistributedJobConfig.isEmpty(m_pathToWekaJar.toString())) {
      throw new IOException(
        "No path to weka.jar file provided. We need to install the "
          + "weka.jar in HDFS so that it is available to running Jobs");
    }

    statusMessage("Installing libraries in HDFS...");
    List<String> installLibraries = new ArrayList<String>();
    logMessage("Copying " + environmentSubstitute(m_pathToWekaJar) + " to HDFS");
    installLibraries.add(environmentSubstitute(m_pathToWekaJar));
    logMessage("Copying " + DISTRIBUTED_WEKA_BASE_JAR + " to HSFS");
    installLibraries.add(DISTRIBUTED_WEKA_BASE_JAR);
    logMessage("Copying " + DISTRIBUTED_WEKA_HADOOP_JAR + " to HSFS");
    installLibraries.add(DISTRIBUTED_WEKA_HADOOP_JAR);
    logMessage("Copying " + OPEN_CSV_JAR + " to HDFS");
    installLibraries.add(OPEN_CSV_JAR);

    HDFSUtils.copyFilesToWekaHDFSInstallationDirectory(installLibraries,
      m_mrConfig.getHDFSConfig(), m_env, true);

    addWekaLibrariesToClasspath(conf);

    installWekaPackageLibrariesInHDFS(
      getAdditionalWekaPackageNames(m_mrConfig), conf);
  }

  /**
   * Install the jar files for a list of named weka packages in HDFS and add
   * them to the classpath for map and reduce tasks
   * 
   * @param packageNames a list of weka packages to install the jar files for
   * @param conf the Hadoop configuration to set the classpath for map and
   *          reduce tasks
   * @throws IOException if a problem occurs
   */
  private void installWekaPackageLibrariesInHDFS(List<String> packageNames,
    Configuration conf) throws IOException {
    if (packageNames == null || packageNames.size() == 0) {
      return;
    }

    File packagesDir = WekaPackageManager.PACKAGES_DIR;
    List<String> installLibraries = new ArrayList<String>();
    for (String packageDir : packageNames) {

      // package dir
      File current = new File(packagesDir.toString() + File.separator
        + packageDir);

      if (current.exists() && current.isDirectory()) {
        File[] contents = current.listFiles();
        for (File f : contents) {
          if (f.isFile() && f.toString().toLowerCase().endsWith(".jar")) {
            logMessage("Copying package '" + packageDir + "': " + f.getName()
              + " to HDFS");
            installLibraries.add(f.toString());
          }
        }

        // lib dir
        File libDir = new File(current.toString() + File.separator + "lib");
        if (libDir.exists() && libDir.isDirectory()) {
          File[] libContents = libDir.listFiles();
          for (File f : libContents) {
            if (f.isFile() && f.toString().toLowerCase().endsWith(".jar")) {
              logMessage("Copying package '" + packageDir + "': " + f.getName()
                + " to HDFS");
              installLibraries.add(f.toString());
            }
          }
        }
      }
    }

    HDFSUtils.copyFilesToWekaHDFSInstallationDirectory(installLibraries,
      m_mrConfig.getHDFSConfig(), m_env, true);

    addWekaPackageLibrariesToClasspath(installLibraries, conf);
  }

  /**
   * Adds the core weka and distributed weka jar files to the classpath for map
   * and reduce tasks
   * 
   * @param conf the Configuration object to populate
   * @throws IOException if a problem occurs
   */
  protected void addWekaLibrariesToClasspath(Configuration conf)
    throws IOException {
    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    statusMessage("Adding Weka libraries to the distributed cache and classpath "
      + "for the job");
    List<String> cacheFiles = new ArrayList<String>();
    cacheFiles.add(new File(m_pathToWekaJar).getName());
    cacheFiles.add(new File(DISTRIBUTED_WEKA_BASE_JAR).getName());
    cacheFiles.add(new File(DISTRIBUTED_WEKA_HADOOP_JAR).getName());
    cacheFiles.add(new File(OPEN_CSV_JAR).getName());

    HDFSUtils.addWekaInstalledFilesToClasspath(m_mrConfig.getHDFSConfig(),
      conf, cacheFiles, m_env);
  }

  /**
   * @param packageJars a list of paths to jar files from packages to add to the
   *          classpath
   * @param conf the Hadoop Configuration to populate
   * @throws IOException if a problem occurs
   */
  private void addWekaPackageLibrariesToClasspath(List<String> packageJars,
    Configuration conf) throws IOException {

    if (packageJars == null || packageJars.size() == 0) {
      return;
    }

    List<String> cacheFiles = new ArrayList<String>();
    statusMessage("Adding Weka package libraries to the distributed cache and classpath");
    for (String jar : packageJars) {
      cacheFiles.add(new File(jar).getName());
    }

    HDFSUtils.addWekaInstalledFilesToClasspath(m_mrConfig.getHDFSConfig(),
      conf, cacheFiles, m_env);
  }

  /**
   * Deletes the output directory for a job
   * 
   * @param job the Job object to delete the output directory for
   * @throws IOException if a problem occurs
   */
  public void cleanOutputDirectory(Job job) throws IOException {
    if (DistributedJobConfig.isEmpty(m_mrConfig.getOutputPath())) {
      throw new IOException("No output directory set!");
    }

    m_mrConfig.deleteOutputDirectory(job, m_env);
  }

  /**
   * Runs the supplied job
   * 
   * @param job the job to run
   * @return true if the job was successful
   * @throws DistributedWekaException if a problem occurs
   */
  protected boolean runJob(Job job) throws DistributedWekaException {
    try {
      m_stopRunningJob = false;
      if (DistributedJobConfig.isEmpty(getLoggingInterval())) {
        m_loggingInterval = "10";
      }
      int logInterval = Integer.parseInt(m_loggingInterval);
      System.out.println("Setting logging interval to " + logInterval);
      job.submit();

      try {
        int taskCompletionEventIndex = 0;
        while (!m_stopRunningJob && !job.isComplete()) {
          if (logInterval >= 1) {
            printJobStatus(job);
            taskCompletionEventIndex += logTaskMessages(job,
              taskCompletionEventIndex);

            Thread.sleep(logInterval * 1000);
          } else {
            Thread.sleep(60000);
          }
        }
      } catch (InterruptedException ie) {
        logMessage(ie.getMessage());
        m_stopRunningJob = true;
      }

      if (m_stopRunningJob && !job.isComplete()) {
        job.killJob();
      }
      m_stopRunningJob = false;

      return job.isSuccessful();
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }
  }

  /**
   * Print status information for the supplied (running) job
   * 
   * @param job the job to print status info for
   * @throws IOException if a problem occurs
   */
  protected void printJobStatus(Job job) throws IOException {
    float setupPercent = job.setupProgress() * 100f;
    float mapPercent = job.mapProgress() * 100f;
    float reducePercent = job.reduceProgress() * 100f;

    String info = getJobName() + " Setup: " + setupPercent + " Map: "
      + mapPercent + " Reduce: " + reducePercent;

    statusMessage(info);
    logMessage(info);
  }

  /**
   * Output task messages for the currently running job
   * 
   * @param job the job to output messages for
   * @param startIndex the index to start outputting messages from
   * @return the index of the last message output
   * @throws IOException if a problem occurs
   */
  protected int logTaskMessages(Job job, int startIndex) throws IOException {
    TaskCompletionEvent[] tcEvents = job.getTaskCompletionEvents(startIndex);

    StringBuilder taskMessages = new StringBuilder();
    for (int i = 0; i < tcEvents.length; i++) {
      taskMessages.append(tcEvents[i].toString()).append("\n");
    }

    logMessage(taskMessages.toString());

    return tcEvents.length;
  }

  /**
   * Extract the number of a map/reduce attempt from the supplied taskID string.
   * 
   * @param taskID the taskID string
   * @param prefix the prefix identifying the type of task (i.e. mapper or
   *          reducer)
   * @return the task number
   */
  public static int getMapReduceNumber(String taskID, String prefix) {
    if (taskID.indexOf(prefix) < 0) {
      return -1; // not what was expected
    }

    String lastPart = taskID
      .substring(taskID.indexOf(prefix) + prefix.length());
    String theNumber = lastPart.substring(0, lastPart.indexOf("_"));

    return Integer.parseInt(theNumber);
  }

  /**
   * Get the number of the map attempt from the supplied task ID string
   * 
   * @param taskID the task ID string
   * @return the number of the map attempt
   */
  public static int getMapNumber(String taskID) {
    return getMapReduceNumber(taskID, "_m_");
  }

  /**
   * Get the number of the reduce attempt from the supplied task ID string
   * 
   * @param taskID the task ID string
   * @return the number of the reduce attempt
   */
  public static int getReduceNumber(String taskID) {
    return getMapReduceNumber(taskID, "_r");
  }
}
