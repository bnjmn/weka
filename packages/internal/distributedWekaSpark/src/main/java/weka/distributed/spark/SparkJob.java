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
 *    SparkJob
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import java.io.*;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;
import weka.core.*;
import weka.distributed.DistributedWekaException;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.spark.SparkJobConfig;

/**
 * Abstract base class for Spark jobs.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class SparkJob extends DistributedJob implements OptionHandler {

  /** The path to the distributedWekaSpark.jar */
  public static final String DISTRIBUTED_WEKA_SPARK_JAR =
    WekaPackageManager.PACKAGES_DIR.toString() + File.separator
      + "distributedWekaSpark" + File.separator + "distributedWekaSpark.jar";

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

  /** The key for a training RDD */
  public static final String TRAINING_DATA = "trainingData";

  /** The key for a test RDD */
  public static final String TEST_DATA = "testData";

  /** For serialization */
  private static final long serialVersionUID = -7408381585962151487L;

  // TODO this can probably be transient too
  /** Connection configuration for this job */
  protected SparkJobConfig m_sjConfig = new SparkJobConfig();

  /** Output debugging info */
  protected boolean m_debug;

  /** Holds the spark context currently in use */
  protected transient JavaSparkContext m_currentContext;

  /** Holds dataset(s) in play */
  protected transient Map<String, Dataset> m_datasets =
    new HashMap<String, Dataset>();

  /** The RDD caching strategy to use */
  protected CachingStrategy m_cachingStrategy;

  /**
   * Constructor.
   * 
   * @param jobName the name of this job
   * @param jobDescription the description of this job
   */
  public SparkJob(String jobName, String jobDescription) {
    super(jobName, jobDescription);
  }

  /**
   * Adds a subdirectory to a parent path. Handles local and hdfs:// files
   *
   * @param parent the parent (may include the hdfs://host:port part
   * @param subdirName
   * @return the parent path with the sudirectory appended.
   */
  public static String addSubdirToPath(String parent, String subdirName) {

    String result =
      parent.toLowerCase().startsWith("hdfs://") ? (parent + "/" + subdirName)
        : parent + File.separator + subdirName;

    return result;
  }

  /**
   * Returns a Configuration object configured with the name node and port
   * present in the supplied path (hdfs://host:port/path). Also returns the
   * path-only part of the URI. Note that absolute paths will require an extra
   * /. E.g. hdfs://host:port//users/fred/input. Also handles local files system
   * paths if no protocol is supplied - e.g. bob/george for a relative path
   * (relative to the current working directory) or /bob/george for an absolute
   * path.
   *
   * @param path the URI or local path from which to configure
   * @param pathOnly will hold the path-only part of the URI
   * @return a Configuration object
   */
  public static Configuration getFSConfigurationForPath(String path,
    String[] pathOnly) {
    if (!path.toLowerCase().contains("://")) {
      // assume local file system
      Configuration conf = new Configuration();
      conf.set(SparkJobConfig.HADOOP_FS_DEFAULT_NAME, "file:/");

      // convert path to absolute if necessary
      File f = new File(path);
      if (!f.isAbsolute()) {
        path = f.getAbsolutePath();
      }

      pathOnly[0] = path;

      return conf;
    }

    Configuration conf = new Configuration();
    // extract protocol,host and port part (hdfs://host:port//)
    String portAndHost = path.substring(0, path.lastIndexOf(":"));
    String portPlusRemainder =
      path.substring(path.lastIndexOf(":") + 1, path.length());
    String port =
      portPlusRemainder.substring(0, portPlusRemainder.indexOf("/"));
    String filePath =
      portPlusRemainder.substring(portPlusRemainder.indexOf("/") + 1,
        portPlusRemainder.length());

    conf.set(SparkJobConfig.HADOOP_FS_DEFAULT_NAME, portAndHost + ":" + port);
    if (pathOnly != null && pathOnly.length > 0) {
      pathOnly[0] = filePath;
    }

    return conf;
  }

  /**
   * Takes an input path and returns a fully qualified absolute one. Handles
   * local and non-local file systems. Original path can be a relative one. In
   * the case of a local file system, the relative path (relative to the working
   * directory) is made into an absolute one. In the case of a file system such
   * as HDFS, an absolute path will require an additional '/' - E.g.
   * hdfs://host:port//users/fred/input - otherwise it will be treated as
   * relative (to the user's home directory in HDFS). In either case, the
   * returned path will be an absolute one.
   *
   * @param original original path (either relative or absolute) on a file
   *          system
   * @return absolute path
   * @throws IOException if a problem occurs
   */
  public static String resolveLocalOrOtherFileSystemPath(String original)
    throws IOException {

    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(original, pathOnly);
    String result = "";

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);
    p = p.makeQualified(fs);

    if (!p.toString().toLowerCase().startsWith("file:/")) {
      result = p.toString();
    } else {
      result = pathOnly[0];
    }

    return result;
  }

  /**
   * Delete a directory (and all contents).
   *
   * @param path the path to the directory to delete
   * @throws IOException if the path is not a directory or a problem occurs
   */
  public static void deleteDirectory(String path) throws IOException {
    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(path, pathOnly);

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);

    if (fs.isFile(p)) {
      throw new IOException("The path '" + pathOnly[0]
        + "' is not a directory!");
    }

    fs.delete(p, true);
  }

  /**
   * Opens the named file for reading on either the local file system or HDFS.
   * HDFS files should use the form "hdfs://host:port/<path>"
   *
   * @param file the file to open for reading on either the local or HDFS file
   *          system
   * @return an InputStream for the file
   * @throws IOException if a problem occurs
   */
  public static InputStream openFileForRead(String file) throws IOException {

    InputStream result = null;
    if (file.toLowerCase().indexOf("://") > 0) {
      String[] pathOnly = new String[1];
      Configuration conf = getFSConfigurationForPath(file, pathOnly);

      FileSystem fs = FileSystem.get(conf);
      Path path = new Path(pathOnly[0]);
      result = fs.open(path);
    } else {
      // local file
      result = new FileInputStream(file);
    }

    return result;
  }

  /**
   * Open the named file for writing to on either the local file system or HDFS.
   * HDFS files should use the form "hdfs://host:port/<path>". Note that, on the
   * local file system, the directory path must exist. Under HDFS, the path is
   * created automatically.
   *
   * @param file the file to write to
   * @return an OutputStream for writing to the file
   * @throws IOException if a problem occurs
   */
  public static OutputStream openFileForWrite(String file) throws IOException {
    OutputStream result = null;

    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(file, pathOnly);

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);
    result = fs.create(p);

    return result;
  }

  /**
   * Open the named file as a text file for writing to on either the local file
   * system or any other protocol specific file system supported by Hadoop.
   * Protocol files should use the form "protocol://host:port/<path>." Note
   * that, on the local file system, the directory path must exist.
   *
   * @param file the file to write to
   * @return an PrintWriter for writing to the file
   * @throws IOException if a problem occurs
   */
  public static PrintWriter openTextFileForWrite(String file)
    throws IOException {
    PrintWriter result = null;
    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(file, pathOnly);

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);

    OutputStream fout = fs.create(p);

    result = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fout)));

    return result;
  }

  /**
   * Check that the named file exists on either the local file system or HDFS.
   *
   * @param file the file to check
   * @return true if the file exists on the local file system or in HDFS
   * @throws IOException if a problem occurs
   */
  public static boolean checkFileExists(String file) throws IOException {
    if (file.toLowerCase().indexOf("://") > 0) {
      String[] pathOnly = new String[1];
      Configuration conf = getFSConfigurationForPath(file, pathOnly);

      FileSystem fs = FileSystem.get(conf);
      Path path = new Path(pathOnly[0]);

      return fs.exists(path) && fs.isFile(path);
    } else {
      File f = new File(file);
      return f.exists() && f.isFile();
    }
  }

  /**
   * Get the size in bytes of a file/directory
   *
   * @param path the path to the file/directory
   * @return the size in bytes
   * @throws IOException if a problem occurs
   */
  public static long getSizeInBytesOfPath(String path) throws IOException {
    String[] pathHolder = new String[1];
    Configuration conf = getFSConfigurationForPath(path, pathHolder);
    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathHolder[0]);
    return fs.getContentSummary(p).getLength();
  }

  /**
   * Adds necessary Weka libraries to the supplied SparkContext
   *
   * @param context the context to add dependencies to
   */
  protected void addWekaLibrariesToSparkContext(JavaSparkContext context) {
    context.addJar(m_sjConfig.getPathToWekaJar());
    context.addJar(DISTRIBUTED_WEKA_BASE_JAR);
    context.addJar(DISTRIBUTED_WEKA_SPARK_JAR);
    context.addJar(OPEN_CSV_JAR);
    context.addJar(JFREECHART_JAR);
    context.addJar(JCOMMON_JAR);
    context.addJar(LA4J_JAR);
    context.addJar(COLT_JAR);

    // Other dependencies and user selected packages
    List<String> wekaPackageNames = getAdditionalWekaPackageNames(m_sjConfig);
    if (wekaPackageNames.size() > 0) {
      addWekaPackageLibrariesToContext(wekaPackageNames, context);
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
        for (File f : contents) {
          if (f.isFile() && f.toString().toLowerCase().endsWith(".jar")) {
            installLibraries.add(f.toString());
          }
        }
      }

      // lib dir
      File libDir = new File(current.toString() + File.separator + "lib");
      if (libDir.exists() && libDir.isDirectory()) {
        File[] libContents = libDir.listFiles();
        for (File f : libContents) {
          if (f.isFile() && f.toString().toLowerCase().endsWith(".jar")) {
            installLibraries.add(f.toString());
          }
        }
      }
    }

    for (String jar : installLibraries) {
      context.addJar(jar);
    }
  }

  /**
   * Create a SparkContext for this job. Configures the context with required
   * libraries, additional weka packages etc.
   *
   * @param conf the configuration for the job
   * @return a SparkContext for the job
   */
  public JavaSparkContext createSparkContextForJob(SparkJobConfig conf) {

    JavaSparkContext context = conf.getBaseSparkContext(getJobName());
    addWekaLibrariesToSparkContext(context);

    return context;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();
    Enumeration<Option> confOpts = m_sjConfig.listOptions();

    while (confOpts.hasMoreElements()) {
      options.addElement(confOpts.nextElement());
    }

    options.addElement(new Option("\tOutput debugging info.", "debug", 1,
      "-debug"));

    return options.elements();
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    String[] baseOptions = getBaseOptionsOnly();
    for (String b : baseOptions) {
      options.add(b);
    }

    String[] configOpts = m_sjConfig.getOptions();
    for (String o : configOpts) {
      options.add(o);
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    m_sjConfig.setOptions(options);

    setDebug(Utils.getFlag("debug", options));
  }

  /**
   * Return the base options only (not the subclasses options or the options
   * specific to the configuration)
   *
   * @return just the base options
   */
  public String[] getBaseOptionsOnly() {
    List<String> options = new ArrayList<String>();

    if (getDebug()) {
      options.add("-debug");
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Tip text for this property.
   *
   * @return the tip text for this property.
   */
  public String debugTipText() {
    return "Output additional debugging info";
  }

  /**
   * Get whether to output debug info.
   *
   * @return true if debug info is to be output.
   */
  public boolean getDebug() {
    return m_debug;
  }

  /**
   * Set whether to output debug info.
   *
   * @param d true if debug info is to be output.
   */
  public void setDebug(boolean d) {
    m_debug = d;
  }

  /**
   * Initialize and return an appender for hooking into Spark's log4j logging
   * and directing it to Weka's log
   *
   * @return an appender
   */
  public WriterAppender initSparkLogAppender() {
    WekaLoggingPrintWriter sparkToWeka = new WekaLoggingPrintWriter();

    WriterAppender appender =
      new WriterAppender(new SimpleLayout(), sparkToWeka);

    Logger sparkLog = Logger.getLogger("org.apache.spark");
    if (sparkLog != null) {
      sparkLog.addAppender(appender);

      return appender;
    }

    return null;
  }

  /**
   * Remove the supplied appender from Spark's logging. This should be called at
   * the end of a job
   *
   * @param appender the appender to remove
   */
  public void removeSparkLogAppender(WriterAppender appender) {
    if (appender != null) {
      Logger sparkLog = Logger.getLogger("org.apache.spark");
      if (sparkLog != null) {
        sparkLog.removeAppender(appender);
      }
    }
  }

  /**
   * Attempts to determine the available cluster memory (in bytes).
   *
   * TODO not sure if this actually works reliably
   *
   * @param sparkContext the spark context to use
   * @return the total available cluster memory.
   */
  protected long determineClusterMemory(JavaSparkContext sparkContext) {
    scala.collection.Map<String, Tuple2<Object, Object>> memoryMap =
      JavaSparkContext.toSparkContext(sparkContext).getExecutorMemoryStatus();

    long totalMem = 0L;
    Map<String, Tuple2<Object, Object>> jMap =
      scala.collection.JavaConversions.asJavaMap(memoryMap);
    for (Tuple2<Object, Object> v : jMap.values()) {
      totalMem += ((Number) v._2()).longValue();
    }

    return totalMem;
  }

  /**
   * Get the caching strategy to use for this job
   *
   * @return the caching strategy to use for this job
   */
  public CachingStrategy getCachingStrategy() {
    return m_cachingStrategy;
  }

  /**
   * Set the caching strategy to use for this job
   *
   * @param cs the caching strategy to use for this job
   */
  public void setCachingStrategy(CachingStrategy cs) {
    m_cachingStrategy = cs;
  }

  /**
   * Load a file/directory of serialized instances (as stored in Spark object
   * file format).
   *
   * @param path the path to the file or directory to load
   * @param sparkContext the context to use
   * @param strategy the optional caching strategy to use
   * @param minPartitions the minimum number of partitions/slices to create (may
   *          be <= 0 to indicate that the default should be used)
   * @param enforceMaxPartitions if true then any max partitions specified by
   *          the user will be enforced (this might trigger a shuffle operation)
   * @return a JavaRDD<Instance> dataset
   * @throws IOException if a problem occurs
   */
  public JavaRDD<Instance> loadInstanceObjectFile(String path,
    JavaSparkContext sparkContext, CachingStrategy strategy, int minPartitions,
    boolean enforceMaxPartitions) throws IOException {
    String resolvedPath = SparkJob.resolveLocalOrOtherFileSystemPath(path);

    JavaRDD<Instance> dataset = null;

    if (minPartitions <= 0) {
      dataset = sparkContext.objectFile(resolvedPath);
    } else {
      dataset = sparkContext.objectFile(resolvedPath, minPartitions);
    }

    if (enforceMaxPartitions
      && !DistributedJobConfig.isEmpty(m_sjConfig.getMaxInputSlices())) {
      try {
        int maxSlices =
          Integer
            .parseInt(environmentSubstitute(m_sjConfig.getMaxInputSlices()));
        if (dataset.partitions().size() > maxSlices) {
          logMessage("[SparkJob] Coalescing to " + maxSlices + " input splits.");
          dataset = dataset.coalesce(maxSlices, true);
        }
      } catch (NumberFormatException e) {
      }
    }

    if (strategy != null) {
      dataset.persist(strategy.getStorageLevel());
    }

    return dataset;
  }

  /**
   * Process an RDD<String> into an RDD<Instance>
   *
   * @param input the RDD<String> input
   * @param headerNoSummary the header of the data without summary attributes
   * @param csvParseOptions the options for the CSV parser
   * @param strategy the optional caching strategy to use
   * @param enforceMaxPartitions if true then any max partitions specified by
   *          the user will be enforced (this might trigger a shuffle operation)
   * @return a JavaRDD<Instance> dataset
   */
  public JavaRDD<Instance> stringRDDToInstanceRDD(JavaRDD<String> input,
    Instances headerNoSummary, String csvParseOptions,
    CachingStrategy strategy, boolean enforceMaxPartitions) {
    CSVToInstanceFlatMapFunction instanceFunction =
      new CSVToInstanceFlatMapFunction(headerNoSummary, csvParseOptions);

    // , true here because we preserve partitions at this point
    JavaRDD<Instance> dataset = input.mapPartitions(instanceFunction, true);

    input.unpersist();
    input = null;

    if (enforceMaxPartitions
      && !DistributedJobConfig.isEmpty(m_sjConfig.getMaxInputSlices())) {
      try {
        int maxSlices =
          Integer
            .parseInt(environmentSubstitute(m_sjConfig.getMaxInputSlices()));
        if (dataset.partitions().size() > maxSlices) {
          logMessage("[SparkJob] Coalescing to " + maxSlices + " input splits.");
          dataset = dataset.coalesce(maxSlices, true);
        }
      } catch (NumberFormatException e) {
      }
    }

    if (strategy != null) {
      dataset.persist(strategy.getStorageLevel());
    }

    return dataset;
  }

  /**
   * Load a file/directory containing instances in CSV format.
   *
   * @param path the path to the file or directory to load
   * @param headerNoSummary the header to use (sans summary attributes)
   * @param csvParseOptions options to the CSV parser
   * @param sparkContext the context to use
   * @param strategy the optional caching strategy to use
   * @param minPartitions the minimum number of partitions/slices to create (may
   *          be <= 0 to indicate that the default should be used) * @param
   * @param enforceMaxPartitions if true then any max partitions specified by
   *          the user will be enforced (this might trigger a shuffle operation)
   * @return a JavaRDD<Instance> dataset
   * @throws IOException if a problem occurs
   */
  public JavaRDD<Instance> loadCSVFile(String path, Instances headerNoSummary,
    String csvParseOptions, JavaSparkContext sparkContext,
    CachingStrategy strategy, int minPartitions, boolean enforceMaxPartitions)
    throws IOException {
    String resolvedPath = SparkJob.resolveLocalOrOtherFileSystemPath(path);

    JavaRDD<String> input =
      minPartitions <= 0 ? sparkContext.textFile(resolvedPath) : sparkContext
        .textFile(resolvedPath, minPartitions);

    if (strategy != null) {
      input.persist(strategy.getStorageLevel());
    }

    return stringRDDToInstanceRDD(input, headerNoSummary, csvParseOptions,
      strategy, enforceMaxPartitions);
  }

  /**
   * Load an input file/directory. Assumes CSV input unless -serialize has been
   * set.
   *
   * @param inputPath the path to the file or directory to load
   * @param headerNoSummary the header of the data (sans summary attributes)
   * @param csvParseOptions options to the CSV parser (used if source is CSV)
   * @param sparkContext the context to use
   * @param strategy the caching strategy to use
   * @param minPartitions the minimum number of partitions/slices to create (may
   *          be <=0 to indicate that the default should be used)
   * @param enforceMaxPartitions if true then any max partitions specified by
   *          the user will be enforced (this might trigger a shuffle operation)
   * @return an JavaRDD<Instance> dataset
   * @throws IOException if a problem occurs
   */
  public JavaRDD<Instance> loadInput(String inputPath,
    Instances headerNoSummary, String csvParseOptions,
    JavaSparkContext sparkContext, CachingStrategy strategy, int minPartitions,
    boolean enforceMaxPartitions) throws IOException {

    /*
     * if (getSerializedInput()) { return loadInstanceObjectFile(inputPath,
     * sparkContext, strategy, minPartitions, enforceMaxPartitions); }
     */

    return loadCSVFile(inputPath, headerNoSummary, csvParseOptions,
      sparkContext, strategy, minPartitions, enforceMaxPartitions);
  }

  /**
   * Get the SparkJobConfig object for this job
   *
   * @return the SparkJobConfig object
   */
  public SparkJobConfig getSparkJobConfig() {
    return m_sjConfig;
  }

  /**
   * Get the current spark context in use by this (and potentially other) jobs.
   *
   * @return the current spark context (or null if there isn't one)
   */
  public JavaSparkContext getSparkContext() {
    return m_currentContext;
  }

  /**
   * Set a dataset for this job to potentially make use of
   *
   * @param key the name of the dataset
   * @param dataset the dataset itself
   */
  public void setDataset(String key, Dataset dataset) {
    m_datasets.put(key, dataset);
  }

  /**
   * Return a named dataset, or null if the name is unknown.
   *
   * @param key the name of the dataset to get
   * @return the named dataset or null
   */
  public Dataset getDataset(String key) {
    return m_datasets.get(key);
  }

  /**
   * Return an iterator over the named datasets for this job
   *
   * @return an iterator over the datasets for this job
   */
  public Iterator<Map.Entry<String, Dataset>> getDatasets() {
    return m_datasets.entrySet().iterator();
  }

  /**
   * Initialize logging and set or create a context to use. This method should
   * be used in the case where multiple jobs are going to be executed within the
   * same JVM and thus need to share one spark context. It configures the
   * supplied/created context with a caching strategy and installs a log
   * appender in the spark logger. This method should be called just once, on
   * the first job that is run. The spark context (for use in subsequent jobs)
   * can be obtained after this by calling getSparkContext() on the first job.
   *
   * @param context the context to use (or null to create a new context)
   * @return the logAppender attached to the spark logger
   * @throws Exception if a problem occurs
   */
  public WriterAppender initJob(JavaSparkContext context) throws Exception {
    WriterAppender logAppender = initSparkLogAppender();
    m_currentContext =
      context != null ? context : createSparkContextForJob(m_sjConfig);
    applyConfigProperties(m_currentContext);

    return logAppender;
  }

  /**
   * Shuts down the context in use by this job and removes the supplied log
   * appender object (if any) from the spark logger. This method should be used
   * in the case where multiple jobs are going to be executed within the same
   * JVM and thus need to share one spark context. It should be called after the
   * last job has completed executing
   *
   * @param logAppender the log appender to remove from the spark logger. May be
   *          null.
   */
  public void shutdownJob(WriterAppender logAppender) {
    if (m_currentContext != null) {
      m_currentContext.stop();
    }
    m_currentContext = null;
    if (logAppender != null) {
      removeSparkLogAppender(logAppender);
    }
  }

  /**
   * Clients to implement
   *
   * @param sparkContext the context to use
   * @return true if the job was successful
   * @throws IOException if a IO problem occurs
   * @throws DistributedWekaException if any other problem occurs
   */
  public abstract boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException;

  protected void applyConfigProperties(JavaSparkContext sparkContext)
    throws DistributedWekaException {
    setCachingStrategy(new CachingStrategy(m_sjConfig.getInputFile(),
      m_sjConfig.getAvailableClusterMemory()
        * (m_sjConfig.getMemoryFraction() > 0
          && m_sjConfig.getMemoryFraction() <= 1 ? m_sjConfig
          .getMemoryFraction() : 0.6),
      m_sjConfig.getInMemoryDataOverheadFactor(), sparkContext));

    logMessage("Using caching strategy: "
      + getCachingStrategy().getStorageLevel().description());

    if (m_sjConfig.getMemoryFraction() > 0
      && m_sjConfig.getMemoryFraction() <= 1) {
      sparkContext.getConf().set("spark.storage.memoryFraction",
        "" + m_sjConfig.getMemoryFraction());
    }
  }

  @Override
  public boolean runJob() throws DistributedWekaException {
    WriterAppender logAppender = null;
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    boolean success = true;
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());

      logAppender = initJob(null);
      success = runJobWithContext(m_currentContext);
    } catch (Exception ex) {
      setJobStatus(JobStatus.FAILED);
      logMessage("A problem occurred while executing the job.", ex);
      throw new DistributedWekaException(ex);
    } finally {
      shutdownJob(logAppender);
      Thread.currentThread().setContextClassLoader(orig);
      if (getJobStatus() != JobStatus.FAILED) {
        setJobStatus(JobStatus.FINISHED);
      }
    }

    return success;
  }

  /**
   * Subclass of TextOutputFormat that does not write the key. We need this as
   * PairRDDFunctions.saveAsNewAPIHadoopFile() does not wrap its key/values in
   * org.apache.hadoop.io.* classes, so specifying a NullWritable for the key
   * does not suppress the key.
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   *
   * @param <K> Key type
   * @param <V> Value type
   */
  public static class NoKeyTextOutputFormat<K, V> extends
    TextOutputFormat<K, V> {

    @Override
    public RecordWriter<K, V> getRecordWriter(TaskAttemptContext job)
      throws IOException, InterruptedException {
      Configuration conf = job.getConfiguration();
      boolean isCompressed = getCompressOutput(job);
      String keyValueSeparator = "\t";
      CompressionCodec codec = null;
      String extension = "";
      if (isCompressed) {
        Class<? extends CompressionCodec> codecClass =
          getOutputCompressorClass(job, GzipCodec.class);
        codec = ReflectionUtils.newInstance(codecClass, conf);
        extension = codec.getDefaultExtension();
      }
      Path file = getDefaultWorkFile(job, extension);
      FileSystem fs = file.getFileSystem(conf);
      if (!isCompressed) {
        FSDataOutputStream fileOut = fs.create(file, false);
        return new NoKeyLineRecordWriter<K, V>(fileOut, keyValueSeparator);
      } else {
        FSDataOutputStream fileOut = fs.create(file, false);
        return new NoKeyLineRecordWriter<K, V>(new DataOutputStream(
          codec.createOutputStream(fileOut)), keyValueSeparator);
      }
    }

    protected static class NoKeyLineRecordWriter<K, V> extends
      LineRecordWriter<K, V> {

      public NoKeyLineRecordWriter(DataOutputStream out,
        String keyValueSeparator) {
        super(out, keyValueSeparator);
      }

      @Override
      public synchronized void write(K key, V value) throws IOException {
        super.write(null, value);
      }
    }
  }

  /**
   * Used for hooking into Spark's log4j logging via a WriterAppender
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected class WekaLoggingPrintWriter extends PrintWriter {

    public WekaLoggingPrintWriter() {
      super(System.out);
    }

    @Override
    public void println(String string) {
      if (m_log != null) {
        if (string.endsWith("\n")) {
          string = string.substring(0, string.length() - 1);
        }
        m_log.logMessage(string);
      }
    }

    @Override
    public void println(Object obj) {
      println(obj.toString());
    }

    @Override
    public void write(String string) {
      println(string);
    }

    @Override
    public void print(String string) {
      println(string);
    }

    @Override
    public void print(Object obj) {
      print(obj.toString());
    }
  }
}
