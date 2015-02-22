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
 *    SparkJobConfig
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package distributed.spark;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

import weka.core.ClassloaderUtil;
import weka.core.Option;
import weka.core.Utils;
import distributed.core.DistributedJobConfig;

/**
 * Basic connection options and settings for a Spark job.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class SparkJobConfig extends DistributedJobConfig {

  public static final String MASTER_HOST = "sparkMasterHost";
  public static final String MASTER_PORT = "sparkMasterPort";
  public static final String SPARK_HOME_DIR = "sparkHomeDir";
  public static final String DEFAULT_SPARK_MASTER_PORT = "7077";
  public static final String DEFAULT_MESOS_MASTER_PORT = "5050";
  public static final String DEFAULT_HDFS_PORT = "9000";
  public static final String HADOOP_FS_DEFAULT_NAME = "fs.default.name";

  /** For serialization */
  private static final long serialVersionUID = 4347165096270398328L;

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

  /** Packages (names) to include in the job */
  protected String m_wekaPackages = "";

  /** Path to input file to process by this job (use hdfs:// for a file in hdfs) */
  protected String m_inputFile = "";

  /**
   * True if the input is an RDD saved as a SequenceFile containing serialized
   * Instance objects
   */
  protected boolean m_serializedInput;

  /**
   * The minimum number of splits to make from the input file. This is based on
   * the block size in HDFS (64mb), so you can't have fewer slices than blocks
   */
  protected String m_minInputSlices = "1";

  protected String m_maxInputSlices = "";

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

  /** Output directory path for the job */
  protected String m_outputDir = "";

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();

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

    options.addElement(new Option(
      "\tInput file/directory to process (may be local or " + "in HDFS)",
      "-input-file", 1, "-input-file <path to file>"));

    options
      .addElement(new Option(
        "\tMinimum number of input slices (partitions) to create from "
          + "the input file (default = 1)", "min-slices", 1,
        "-min-slices <num>"));
    options.addElement(new Option(
      "\tMaximum number of input slices (partitions) to create from "
        + "the input file (default: no maximum).\n\t"
        + "Note that using this option may trigger a shuffle.", "max-slices",
      1, "-max-slices <num>"));

    options.addElement(new Option("\tOutput directory for the job",
      "output-dir", 1, "-output-dir <path>"));

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

    options.addAll(Collections.list(super.listOptions()));

    return options.elements();
  }

  @Override
  public String[] getOptions() {

    ArrayList<String> opts = new ArrayList<String>();

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

    if (!DistributedJobConfig.isEmpty(getInputFile())) {
      opts.add("-input-file");
      opts.add(getInputFile());
    }

    if (!DistributedJobConfig.isEmpty(getMinInputSlices())) {
      opts.add("-min-slices");
      opts.add(getMinInputSlices());
    }

    if (!DistributedJobConfig.isEmpty(getMaxInputSlices())) {
      opts.add("-max-slices");
      opts.add(getMaxInputSlices());
    }

    if (!DistributedJobConfig.isEmpty(getOutputDir())) {
      opts.add("-output-dir");
      opts.add(getOutputDir());
    }

    opts.add("-cluster-mem");
    opts.add("" + getAvailableClusterMemory());

    opts.add("-overhead");
    opts.add("" + getInMemoryDataOverheadFactor());

    opts.add("-mem-fraction");
    opts.add("" + getMemoryFraction());

    for (String s : super.getOptions()) {
      opts.add(s);
    }

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

    setInputFile(Utils.getOption("input-file", options));
    setMinInputSlices(Utils.getOption("min-slices", options));
    setMaxInputSlices(Utils.getOption("max-slices", options));
    setOutputDir(Utils.getOption("output-dir", options));

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
  public String inputFileTipText() {
    return "The path to the file to process by this job. This "
      + "can be on the local file system or in HDFS (use "
      + "hdfs:// to specify a file in HDFS).";
  }

  /**
   * Get the path to the file to process by this job. This can be on the local
   * file system or in HDFS. Use hdfs:// to specify a file in HDFS.
   *
   * @return the path to the file to process by the job
   */
  public String getInputFile() {
    return m_inputFile;
  }

  /**
   * Set the path to the file to process by this job. This can be on the local
   * file system or in HDFS. Use hdfs:// to specify a file in HDFS.
   *
   * @param filePath the path to the file to process by the job
   */
  public void setInputFile(String filePath) {
    m_inputFile = filePath;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String minInputSlicesTipText() {
    return "The minimum number of splits to create for the input file";
  }

  /**
   * Get the minimum number of input splits to create from the input file. By
   * default there is 1 split per 64Mb block (HDFS block size), so there can't
   * be fewer splits than there are blocks in the file. However, there can be
   * more splits.
   *
   * @return the number of splits to create
   */
  public String getMinInputSlices() {
    return m_minInputSlices;
  }

  /**
   * Set the minimum number of input splits (partitions) to create from the
   * input file. By default there is 1 split per 64Mb block (HDFS block size),
   * so there can't be fewer splits than there are blocks in the file. However,
   * there can be more splits.
   *
   * @param slices the number of splits to create
   */
  public void setMinInputSlices(String slices) {
    m_minInputSlices = slices;
  }

  /**
   * 
   * @return the tip text for this property
   */
  public String maxInputSlicesTipText() {
    return "The maximum number of splits/partitions to create "
      + "for the input file.";
  }

  /**
   * Get the maximum number of input splits (partitions) to create from the
   * input file. If this is fewer than the natural number of partitions (i.e. as
   * determined by the HDFS block size) then this will trigger a coalesce.
   *
   * @return the maximum number of splits/partitions to create from an input
   *         file.
   */
  public String getMaxInputSlices() {
    return m_maxInputSlices;
  }

  /**
   * Set the maximum number of input splits (partitions) to create from the
   * input file. If this is fewer than the natural number of partitions (i.e. as
   * determined by the HDFS block size) then this will trigger a coalesce.
   *
   * @param max the maximum number of splits/partitions to create from an input
   *          file.
   */
  public void setMaxInputSlices(String max) {
    m_maxInputSlices = max;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String outputDirTipText() {
    return "The output directory for this job";
  }

  /**
   * Get the output directory for this job
   *
   * @return the output directory for this job
   */
  public String getOutputDir() {
    return m_outputDir;
  }

  /**
   * Set the output directory for this job
   *
   * @param dir the output directory for this job
   */
  public void setOutputDir(String dir) {
    m_outputDir = dir;
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

    SparkConf sConf = new SparkConf();
    sConf.setMaster(getMasterURL());
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

    JavaSparkContext context = new JavaSparkContext(sConf);

    // TODO Currently no way of setting yarn-specific props. It is too
    // late by the time new SparkContext() is done - Spark is already
    // talking to the resource manager. Spark 1.3 might have a solution:
    // https://issues.apache.org/jira/browse/SPARK-4924
    /*
     * if (hasYarn) { for (String prop : getUserSuppliedPropertyNames()) { if
     * (prop.startsWith("yarn.")) {
     * System.err.println("[SparkJobConfig] setting '" + prop + "=" +
     * getUserSuppliedProperty(prop) + "' on Hadoop Configuration");
     * context.hadoopConfiguration() .set(prop, getUserSuppliedProperty(prop));
     * } } }
     */

    return context;
  }
}
