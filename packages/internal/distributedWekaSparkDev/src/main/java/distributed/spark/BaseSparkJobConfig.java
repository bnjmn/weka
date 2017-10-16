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
 *    BaseSparkJobConfig
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package distributed.spark;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import weka.core.ClassloaderUtil;
import weka.core.Option;
import weka.core.Utils;
import weka.core.WekaException;
import weka.core.WekaPackageClassLoaderManager;
import weka.core.WekaPackageManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * Basic options common to batch and streaming spark jobs
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class BaseSparkJobConfig extends DistributedJobConfig {

  // The following are libraries needed to run distributed Weka for Spark
  // tasks in the Spark environment (i.e. needed on the classpath for workers).

  public static final String PACKAGE_NAME = "distributedWekaSparkDev";

  /** The path to the distributedWekaSpark.jar */
  public static final String DISTRIBUTED_WEKA_SPARK_JAR =
    WekaPackageManager.PACKAGES_DIR.toString() + File.separator + PACKAGE_NAME
      + File.separator + "distributedWekaSparkDev.jar";

  /** The path to the distributedWekaBase.jar */
  public static final String DISTRIBUTED_WEKA_BASE_JAR =
    WekaPackageManager.PACKAGES_DIR.toString() + File.separator
      + "distributedWekaBase" + File.separator + "distributedWekaBase.jar";

  /** The path to the opencsv.jar */
  public static final String OPEN_CSV_JAR = WekaPackageManager.PACKAGES_DIR
    .toString()
    + File.separator
    + "distributedWekaBase"
    + File.separator
    + "lib" + File.separator + "opencsv-2.3.jar";

  /** The path to the jfreechart jar */
  public static final String JFREECHART_JAR = WekaPackageManager.PACKAGES_DIR
    .toString()
    + File.separator
    + "distributedWekaBase"
    + File.separator
    + "lib" + File.separator + "jfreechart-1.0.13.jar";

  /** The path to the jcommon jar */
  public static final String JCOMMON_JAR = WekaPackageManager.PACKAGES_DIR
    .toString()
    + File.separator
    + "distributedWekaBase"
    + File.separator
    + "lib" + File.separator + "jcommon-1.0.16.jar";

  /** The path to the colt.jar */
  public static final String COLT_JAR = WekaPackageManager.PACKAGES_DIR
    .toString()
    + File.separator
    + "distributedWekaBase"
    + File.separator
    + "lib" + File.separator + "colt-1.2.0.jar";

  /** The path to the la4j.jar */
  public static final String LA4J_JAR = WekaPackageManager.PACKAGES_DIR
    .toString()
    + File.separator
    + "distributedWekaBase"
    + File.separator
    + "lib" + File.separator + "la4j-0.4.5.jar";

  /** The path to the t-digest.jar */
  public static final String TDIGEST_JAR = WekaPackageManager.PACKAGES_DIR
    .toString()
    + File.separator
    + "distributedWekaBase"
    + File.separator
    + "lib" + File.separator + "t-digest-3.1.jar";

  public static final String WEKA_SPARK_PROPS = "wekaSpark.props";
  protected static List<String> s_runtimeLibraries = new ArrayList<>();

  static {
    try {
      // main distrubuted Weka spark jar + jars from distributed Weka base
      s_runtimeLibraries.addAll(Arrays.asList(DISTRIBUTED_WEKA_SPARK_JAR,
        DISTRIBUTED_WEKA_BASE_JAR, OPEN_CSV_JAR, JFREECHART_JAR, JCOMMON_JAR,
        COLT_JAR, LA4J_JAR, TDIGEST_JAR));
      File propsFile =
        new File(WekaPackageManager.PACKAGES_DIR.toString() + File.separator
          + PACKAGE_NAME + File.separator + WEKA_SPARK_PROPS);
      Properties sparkProps = new Properties();
      sparkProps.load(new FileInputStream(propsFile));

      String runtimeLibs = sparkProps.getProperty("sparkRuntimeLibs", "");
      String[] libs = runtimeLibs.split(",");

      // add in additional spark-related libraries that are not typically part
      // of the spark assembly jar (e.g. CSV & Avro data frame stuff from
      // DataBricks)
      for (String lib : libs) {
        s_runtimeLibraries.add(WekaPackageManager.PACKAGES_DIR.toString()
          + File.separator + PACKAGE_NAME + File.separator + "lib"
          + File.separator + lib.trim());
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static final String MASTER_HOST = "sparkMasterHost";
  public static final String MASTER_PORT = "sparkMasterPort";
  public static final String SPARK_HOME_DIR = "sparkHomeDir";
  public static final String DEFAULT_SPARK_MASTER_PORT = "7077";
  public static final String DEFAULT_MESOS_MASTER_PORT = "5050";
  public static final String DEFAULT_HDFS_PORT = "9000";
  public static final String HADOOP_FS_DEFAULT_NAME = "fs.default.name";

  private static final long serialVersionUID = -2355686495513829382L;

  protected List<String> m_additonalJobLibraries = new ArrayList<>();

  protected static String WEKA_JAR_FILE_NAME = System.getProperty(
    "weka.jar.filename", "weka.jar");

  /**
   * A default path to a weka.jar file. If the classpath contains a weka.jar
   * file (rather than a directory of weka classes) when Weka is started then
   * this path will be populated automatically by scanning for weka.jar in the
   * classpath.
   */
  protected static String DEFAULT_WEKA_JAR_PATH = System
    .getProperty("user.home") + File.separator + WEKA_JAR_FILE_NAME;

  // Attempt to locate the weka.jar in the classpath and set a
  // the default path to it
  static {
    try {
      ClassLoader cl = ClassloaderUtil.class.getClassLoader();
      if (cl instanceof URLClassLoader) {
        URL[] urls = ((URLClassLoader) cl).getURLs();

        for (URL u : urls) {
          if (u.toString().endsWith(WEKA_JAR_FILE_NAME)) {
            File f = new File(u.toURI());
            DEFAULT_WEKA_JAR_PATH = f.toString();
            break;
          }
        }
      }
    } catch (Exception ex) {

    }
  }

  public void addAdditionalJobLibrary(String library) {
    m_additonalJobLibraries.add(library);
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();

    options.addAll(Collections.list(super.listOptions()));

    options.add(new Option("\tThe host that the master is running on "
      + "(include the protocol in the case of spark or mesos clusters\n\t("
      + "eg \"spark://\", \"mesos://\"); can also use \"local[*]\" to run\n\t"
      + "locally using all the CPU cores on your machine.", "master", 1,
      "-master <host>"));

    options.add(new Option("\tThe port that the master is running on", "port",
      1, "-port <num>"));

    options.add(new Option(
      "\tThe root directory of the spark installation on the slave nodes",
      "spark-home", 1, "-spark-home <path>"));

    options.addElement(new Option("\tPath to the weka.jar file", "weka-jar", 1,
      "-weka-jar <path to weka.jar>"));

    options.addElement(new Option("\tAdditional Weka packages to use.",
      "weka-packages", 1,
      "-weka-packages <comma-separated list of package names>"));

    options
      .addElement(new Option(
        "\tTotal available cluster memory (in Gb). Used in"
          + "in automaticaly determining a caching strategy.\n\t"
          + "Default = -1 (unknown - i.e. just use default strategy of MEMORY_AND_DISK)",
        "cluster-mem", 1, "-cluster-mem <Gb>"));

    options.addElement(new Option("\tFraction of Java heap to use for Spark's "
      + "memory cache.\n\tDefault = 0.6.", "mem-fraction", 1,
      "-mem-fraction <fraction>"));

    options.addElement(new Option(
      "\tIn memory data overhead factor (as a multiple of\n\t"
        + "on-disk size). Used when determining a caching strategy.\n\t"
        + "Default = 3.", "overhead", 1, "-overhead <number>"));

    return options.elements();
  }

  @Override
  public String[] getOptions() {
    ArrayList<String> opts = new ArrayList<String>();
    for (String s : super.getOptions()) {
      opts.add(s);
    }

    if (!DistributedJobConfig.isEmpty(getMasterHost())) {
      opts.add("-master");
      opts.add(getMasterHost());
    }

    if (!DistributedJobConfig.isEmpty(getMasterPort())) {
      opts.add("-port");
      opts.add(getMasterPort());
    }

    if (!DistributedJobConfig.isEmpty(getSparkHomeDirectory())) {
      opts.add("-spark-home");
      opts.add(getSparkHomeDirectory());
    }

    if (!DistributedJobConfig.isEmpty(getPathToWekaJar())) {
      opts.add("-weka-jar");
      opts.add(getPathToWekaJar());
    }

    if (!DistributedJobConfig.isEmpty(getWekaPackages())) {
      opts.add("-weka-packages");
      opts.add(getWekaPackages());
    }

    opts.add("-cluster-mem");
    opts.add("" + getAvailableClusterMemory());

    opts.add("-overhead");
    opts.add("" + getInMemoryDataOverheadFactor());

    opts.add("-mem-fraction");
    opts.add("" + getMemoryFraction());

    return opts.toArray(new String[opts.size()]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    setMasterHost(Utils.getOption("master", options));
    setMasterPort(Utils.getOption("port", options));
    setSparkHomeDirectory(Utils.getOption("spark-home", options));

    String wekaPath = Utils.getOption("weka-jar", options);
    if (!DistributedJobConfig.isEmpty(wekaPath)) {
      setPathToWekaJar(wekaPath);
    }

    String additionalPackages = Utils.getOption("weka-packages", options);
    setWekaPackages(additionalPackages);

    String tmp = Utils.getOption("cluster-mem", options);
    if (!DistributedJobConfig.isEmpty(tmp)) {
      setAvailableClusterMemory(Double.parseDouble(tmp));
    }

    tmp = Utils.getOption("overhead", options);
    if (!DistributedJobConfig.isEmpty(tmp)) {
      setInMemoryDataOverheadFactor(Double.parseDouble(tmp));
    }

    tmp = Utils.getOption("mem-fraction", options);
    if (!DistributedJobConfig.isEmpty(tmp)) {
      setMemoryFraction(Double.parseDouble(tmp));
    }

    super.setOptions(options);
  }

  /** Holds the path to the weka.jar */
  protected String m_pathToWekaJar = DEFAULT_WEKA_JAR_PATH;

  /** Packages (names) to include in the job */
  protected String m_wekaPackages = "";

  /**
   * The available memory in the cluster (in Gb). Used by the caching strategy
   * selector. -1 indicates to use the default caching strategy
   * (MEMORY_AND_DISK)
   */
  protected double m_availableClusterMemory = -1;

  /**
   * The overhead (as a multiple of the original on-disk data size) that data
   * occupies in object form in memory. Default is pretty much worse case for
   * string data
   */
  protected double m_objectOverhead = 3;

  /** Fraction of Java heap to use for Spark's memory cache */
  protected double m_memFraction = 0.6;

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String masterHostTipText() {
    return "The host that the master is running on (include the protocol = eg \"spark://\" "
      + "or \"mesos://\"); YARN execution should be specified with "
      + "\"yarn-client\"; can also run locally by specifying \"local[*]\".";
  }

  /**
   * Get the host for the master node. This can be spark://..., mesos:// or
   * yarn-client.
   *
   * @return the host of the master
   */
  public String getMasterHost() {
    return getProperty(MASTER_HOST);
  }

  /**
   * Set the host for the master node. This can be spark://..., mesos:// or
   * yarn-client.
   *
   * @param host the host of the master
   */
  public void setMasterHost(String host) {
    setProperty(MASTER_HOST, host);
  }

  /**
   * Tool tip text for this property
   *
   * @return the tool tip text for this property
   */
  public String masterPortTipText() {
    return "The port that the master is running on";
  }

  /**
   * Get the port for the master node. If not specified, then the default will
   * be used for whichever type of cluster is specified by the master host
   * setting. Note that port is not needed for yarn-client mode.
   *
   * @return the port for the master node
   */
  public String getMasterPort() {
    return getProperty(MASTER_PORT);
  }

  /**
   * Set the port for the master node. If not specified, then the default will
   * be used for whichever type of cluster is specified by the master host
   * setting. Note that port is not needed for yarn-client mode.
   *
   * @param port the port for the master node
   */
  public void setMasterPort(String port) {
    setProperty(MASTER_PORT, port);
  }

  /**
   * Tool tip text for this property
   *
   * @return the tool tip text for this property
   */
  public String sparkHomeDirectoryTipText() {
    return "The root directory of the spark installation on the slave nodes";
  }

  /**
   * Get the root directory of the spark installation on the slave nodes
   *
   * @return the root directory of the spark installation on the slave nodes
   */
  public String getSparkHomeDirectory() {
    return getProperty(SPARK_HOME_DIR);
  }

  /**
   * Set the root directory of the spark installation on the slave nodes
   *
   * @param sparkHome the root directory of the spark installation on the slave
   *          nodes
   */
  public void setSparkHomeDirectory(String sparkHome) {
    setProperty(SPARK_HOME_DIR, sparkHome);
  }

  /**
   * Tip text for this property.
   *
   * @return the tip text for this property.
   */
  public String availableClusterMemoryTipText() {
    return "The total memory available to the cluster in Gb (-1 = unknown). Used in"
      + " the automatic determination of a caching strategy to use.";
  }

  /**
   * Get the total available cluster memory.
   *
   * @return the total available cluster memory in Gb
   */
  public double getAvailableClusterMemory() {
    return m_availableClusterMemory;
  }

  /**
   * Set the total available cluster memory.
   *
   * @param m the total available cluster memory in Gb
   */
  public void setAvailableClusterMemory(double m) {
    m_availableClusterMemory = m;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String inMemoryDataOverheadFactorTipText() {
    return "Overhead of data in memory as a multiple of the on-disk size";
  }

  /**
   * Get the overhead factor for data in memory. This is a multiple of the
   * on-disk size of the input data.
   *
   * @return the overhead factory for data in memory.
   */
  public double getInMemoryDataOverheadFactor() {
    return m_objectOverhead;
  }

  /**
   * Set the overhead factor for data in memory. This is a multiple of the
   * on-disk size of the input data.
   *
   * @param f the overhead factory for data in memory.
   */
  public void setInMemoryDataOverheadFactor(double f) {
    m_objectOverhead = f;
  }

  /**
   * Tip text for this property.
   *
   * @return the tip text for this property
   */
  public String memoryFractionTipText() {
    return "The fraction of Java heap to use for Spark's memory cache.";
  }

  /**
   * Get the fraction of Java heap to use for Spark's memory cache.
   *
   * @return the fraction to use
   */
  public double getMemoryFraction() {
    return m_memFraction;
  }

  /**
   * Set the fraction of Java heap to use for Spark's memory cache.
   *
   * @param f the fraction to use
   */
  public void setMemoryFraction(double f) {
    m_memFraction = f;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String pathToWekaJarTipText() {
    return "The path to the weka jar file. This will be included as"
      + "a library for the spark job";
  }

  /**
   * Get the path to the weka.jar file. Will be populated automatically if the
   * classpath contains a weka.jar. The weka.jar is included as a library for
   * the spark job.
   *
   * @return the path to the weka.jar.
   */
  public String getPathToWekaJar() {
    return m_pathToWekaJar;
  }

  /**
   * Set the path to the weka.jar file. Will be populated automatically if the
   * classpath contains a weka.jar. The weka.jar is included as a library for
   * the spark job
   *
   * @param path the path to the weka.jar.
   */
  public void setPathToWekaJar(String path) {
    m_pathToWekaJar = path;
  }

  /**
   * Tip text for this property.
   *
   * @return the tip text for this property.
   */
  public String wekaPackagesTipText() {
    return "A list of comma separated weka package names to use with the job. "
      + "Any jar files in the main package directory and the lib "
      + "directory of each package will be included as libraries for the spark"
      + "job.";
  }

  /**
   * Get a comma separated list of the names of additional weka packages to use
   * with the job. Any jar files in the main package directory and the lib
   * directory of the package will be included as a library for the spark job.
   *
   * @return a comma separated list of weka packages to use with the job
   */
  public String getWekaPackages() {
    return m_wekaPackages;
  }

  /**
   * Set a comma separated list of the names of additional weka packages to use
   * with the job. Any jar files in the main package directory and the lib
   * directory of the package will be included as a library for the spark job.
   *
   * @param packages a comma separated list of weka packages to use with the job
   */
  public void setWekaPackages(String packages) {
    m_wekaPackages = packages;
  }

  /**
   * Attempt to get a default port based on the master url
   *
   * @return a default port to use. Returns null if no master has been set
   */
  public String getDefaultPortForMaster() {
    if (DistributedJobConfig.isEmpty(getMasterHost())) {
      return null;
    }

    if (getMasterHost().toLowerCase().startsWith("local")) {
      // no port necessary
      return "";
    }

    if (getMasterHost().toLowerCase().startsWith("spark")) {
      return DEFAULT_SPARK_MASTER_PORT;
    }

    if (getMasterHost().toLowerCase().startsWith("mesos")) {
      return DEFAULT_MESOS_MASTER_PORT;
    }

    // YARN case
    return "";
  }

  protected String getMasterURL() {
    // rely on spark configuration to fill in settings in Config object
    if (DistributedJobConfig.isEmpty(getMasterHost())) {
      return "";
    }

    if (getMasterHost().toLowerCase().startsWith("local")) {
      return getMasterHost();
    }

    String masterHost = getMasterHost();

    // yarn config info:
    // https://spark.apache.org/docs/1.1.0/running-on-yarn.html
    String masterURL = masterHost + (masterHost.startsWith("yarn") ? "" : ":");

    if (!masterHost.startsWith("yarn")) {
      if (DistributedJobConfig.isEmpty(getMasterPort())) {
        masterURL += getDefaultPortForMaster();
      } else {
        masterURL += getMasterPort();
      }
    } else {
      // look for a ://... after yarn-client or yarn-cluster. If present
      // take this as the setting for yarn.resourcemanager.hostname; otherwise
      // assume the user will manually set various yarn.resourcemanager.*
      // properties as user properties.
      // http://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/ClusterSetup.html
      // TODO this currently does not work, but might in the future. See notes
      // in getBaseSparkContext()
      if (masterURL.indexOf("://") > 0) {
        String resourceManagerHost =
          masterURL.substring(masterURL.indexOf("://") + 3, masterURL.length());
        setUserSuppliedProperty("yarn.resourcemanager.hostname",
          resourceManagerHost);

        // chop out just the yarn-client/yarn-cluster part
        masterURL = masterURL.substring(0, masterURL.indexOf("://"));
      }
    }

    return masterURL;
  }

  /**
   * Gets a configured SparkContext
   *
   * @param jobName the job name to set on the context
   * @return a configured SparkContext
   */
  public JavaSparkContext getBaseSparkContext(String jobName) {

    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    SparkConf sConf = null;
    JavaSparkContext context = null;
    try {
      System.err.println("Creating spark context with cl "
        + this.getClass().getClassLoader());
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());
      sConf = new SparkConf();

      if (!DistributedJobConfig.isEmpty(getMasterURL())) {
        sConf.setMaster(getMasterURL());
      }
      sConf.setAppName(jobName);
      if (!DistributedJobConfig.isEmpty(getSparkHomeDirectory())) {
        sConf.setSparkHome(getSparkHomeDirectory());
      }

      boolean hasYarn = false;
      // set user-supplied properties
      for (String prop : getUserSuppliedPropertyNames()) {
        if (prop.startsWith("yarn.")) {
          hasYarn = true;
        }
        sConf.set(prop, getUserSuppliedProperty(prop));
      }

      context = new JavaSparkContext(sConf);
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    // TODO Currently no way of setting yarn-specific props. It is too
    // late by the time new SparkContext() is done - Spark is already
    // talking to the resource manager. Spark 1.3 might have a solution:
    // https://issues.apache.org/jira/browse/SPARK-4924
    /*
     * if (hasYarn) { for (String prop : getUserSuppliedPropertyNames()) { if
     * (prop.startsWith("yarn.")) { System.err.println(
     * "[SparkJobConfig] setting '" + prop + "=" + getUserSuppliedProperty(prop)
     * + "' on Hadoop Configuration"); context.hadoopConfiguration() .set(prop,
     * getUserSuppliedProperty(prop)); } } }
     */

    return context;
  }

  private void writeJarFromStream(InputStream inStream, File destination)
    throws IOException {

    BufferedInputStream bis = null;
    BufferedOutputStream bos = null;
    try {
      bis = new BufferedInputStream(inStream);
      bos = new BufferedOutputStream(new FileOutputStream(destination));
      copyStreams(bis, bos);
    } finally {
      if (bos != null) {
        bos.flush();
        bos.close();
      }
      if (bis != null) {
        bis.close();
      }
    }
  }

  private static void copyStreams(InputStream input, OutputStream output)
    throws IOException {
    int count;
    byte data[] = new byte[1024];
    while ((count = input.read(data, 0, 1024)) != -1) {
      output.write(data, 0, count);
    }
  }

  /**
   * Adds necessary Weka libraries to the supplied SparkContext
   *
   * @param context the context to add dependencies to
   * @param job the job that is using the context
   * @throws WekaException if a problem occurs
   */
  public void addWekaLibrariesToSparkContext(JavaSparkContext context,
    DistributedJob job) throws WekaException {

    File wekaJarFile = new File(getPathToWekaJar());
    if (!wekaJarFile.exists()) {
      throw new WekaException("The path to the weka jar file '"
        + wekaJarFile.toString() + "' does not seem to exist!");
    }

    context.addJar(getPathToWekaJar());

    // now extract mtj-related jars from the weka.jar if necessary
    File mtjTmpDir =
      new File(WekaPackageManager.PACKAGES_DIR.toString() + File.separator
        + PACKAGE_NAME + File.separator + "mtj");
    if (!mtjTmpDir.exists()) {
      if (!mtjTmpDir.mkdirs()) {
        throw new WekaException("Was unable to create a directory ("
          + mtjTmpDir.toString() + ") in order to extract and store required "
          + "mtj jar files");
      }
    }
    File mtjFile = new File(mtjTmpDir.toString() + File.separator + "mtj.jar");
    File coreFile =
      new File(mtjTmpDir.toString() + File.separator + "core.jar");
    File arpackFile =
      new File(mtjTmpDir.toString() + File.separator
        + "arpack_combined_all.jar");
    if (!mtjFile.exists() || !coreFile.exists() || !arpackFile.exists()) {
      // extract from weka.jar
      ClassLoader coreClassLoader =
        WekaPackageClassLoaderManager.getWekaPackageClassLoaderManager()
          .getClass().getClassLoader();
      InputStream mtjCoreInputStream =
        coreClassLoader.getResourceAsStream("core.jar");
      InputStream arpackAllInputStream =
        coreClassLoader.getResourceAsStream("arpack_combined_all.jar");
      InputStream mtjInputStream =
        coreClassLoader.getResourceAsStream("mtj.jar");

      try {
        writeJarFromStream(mtjInputStream, mtjFile);
        writeJarFromStream(mtjCoreInputStream, coreFile);
        writeJarFromStream(arpackAllInputStream, arpackFile);
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }

    context.addJar(mtjFile.toString());
    context.addJar(coreFile.toString());
    context.addJar(arpackFile.toString());

    for (String jar : s_runtimeLibraries) {
      if (new File(jar).exists()) {
        context.addJar(jar);
      } else {
        System.err.println("WARNING: runtime lib '" + jar + "' does not seem "
          + "to exist on disk - skipping");
      }
    }

    // Other dependencies and user selected packages
    if (!DistributedJobConfig.isEmpty(getWekaPackages())) {
      setUserSuppliedProperty(DistributedJob.WEKA_ADDITIONAL_PACKAGES_KEY,
        getWekaPackages());
      List<String> wekaPackageNames = job.getAdditionalWekaPackageNames(this);
      if (wekaPackageNames.size() > 0) {
        addWekaPackageLibrariesToContext(wekaPackageNames, context);
      }
    }
  }

  private void addWekaPackageLibrariesToContext(List<String> packageNames,
    JavaSparkContext context) {

    // first determine all the jars necessary
    File packagesDir = WekaPackageManager.PACKAGES_DIR;
    List<String> installLibraries = new ArrayList<String>();
    for (String packageDir : packageNames) {
      // package dir
      File current =
        new File(packagesDir.toString() + File.separator + packageDir);

      if (current.exists() && current.isDirectory()) {
        File[] contents = current.listFiles();
        if (contents != null) {
          for (File f : contents) {
            if (f.isFile() && f.toString().toLowerCase().endsWith(".jar")) {
              installLibraries.add(f.toString());
            }
          }
        }
      }

      // lib dir
      File libDir = new File(current.toString() + File.separator + "lib");
      if (libDir.exists() && libDir.isDirectory()) {
        File[] libContents = libDir.listFiles();
        if (libContents != null) {
          for (File f : libContents) {
            if (f.isFile() && f.toString().toLowerCase().endsWith(".jar")) {
              installLibraries.add(f.toString());
            }
          }
        }
      }
    }

    for (String jar : installLibraries) {
      context.addJar(jar);
    }

    // additional job-specified libraries
    for (String additional : m_additonalJobLibraries) {
      context.addJar(additional);
    }
  }
}
