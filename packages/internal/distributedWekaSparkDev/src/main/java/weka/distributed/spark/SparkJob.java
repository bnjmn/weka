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

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.spark.SparkJobConfig;
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
import org.apache.log4j.WriterAppender;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import weka.classifiers.mllib.MLlibSparkContextManager;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.WekaException;
import weka.core.WekaPackageManager;
import weka.distributed.DistributedWekaException;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Abstract base class for Spark jobs.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 12253 $
 */
public abstract class SparkJob extends DistributedJob implements OptionHandler {

  /** The key for a training RDD */
  public static final String TRAINING_DATA = AbstractDataset.TRAINING_DATA;

  /** The key for a test RDD */
  public static final String TEST_DATA = AbstractDataset.TEST_DATA;

  /** For serialization */
  private static final long serialVersionUID = -7408381585962151487L;

  // TODO this can probably be transient too
  /** Connection configuration for this job */
  protected SparkJobConfig m_sjConfig = new SparkJobConfig();

  /** Output debugging info */
  protected boolean m_debug;

  /** Holds the spark context currently in use */
  protected transient JavaSparkContext m_currentContext;

  /** Holds datasets in play */
  protected transient DatasetManager m_datasetManager = new DatasetManager();

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
   * @param subdirName the name of the subdirectory to add
   * @return the parent path with the sudirectory appended.
   */
  public static String addSubdirToPath(String parent, String subdirName) {

    return SparkUtils.addSubdirToPath(parent, subdirName);
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
    return SparkUtils.getFSConfigurationForPath(path, pathOnly);
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
    return SparkUtils.resolveLocalOrOtherFileSystemPath(original);
  }

  /**
   * Delete a directory (and all contents).
   *
   * @param path the path to the directory to delete
   * @throws IOException if the path is not a directory or a problem occurs
   */
  public static void deleteDirectory(String path) throws IOException {
    SparkUtils.deleteDirectory(path);
  }

  /**
   * Opens the named file for reading on either the local file system or HDFS.
   * HDFS files should use the form {@code "hdfs://host:port/<path>"}
   *
   * @param file the file to open for reading on either the local or HDFS file
   *          system
   * @return an InputStream for the file
   * @throws IOException if a problem occurs
   */
  public static InputStream openFileForRead(String file) throws IOException {
    return SparkUtils.openFileForRead(file);
  }

  /**
   * Open the named file for writing to on either the local file system or HDFS.
   * HDFS files should use the form {@code "hdfs://host:port/<path>"}. Note
   * that, on the local file system, the directory path must exist. Under HDFS,
   * the path is created automatically.
   *
   * @param file the file to write to
   * @return an OutputStream for writing to the file
   * @throws IOException if a problem occurs
   */
  public static OutputStream openFileForWrite(String file) throws IOException {
    return SparkUtils.openFileForWrite(file);
  }

  /**
   * Open the named file as a text file for writing to on either the local file
   * system or any other protocol specific file system supported by Hadoop.
   * Protocol files should use the form "{@code protocol://host:port/<path>}."
   * Note that, on the local file system, the directory path must exist.
   *
   * @param file the file to write to
   * @return an PrintWriter for writing to the file
   * @throws IOException if a problem occurs
   */
  public static PrintWriter openTextFileForWrite(String file)
    throws IOException {
    return SparkUtils.openTextFileForWrite(file);
  }

  /**
   * Check that the named file exists on either the local file system or HDFS.
   *
   * @param file the file to check
   * @return true if the file exists on the local file system or in HDFS
   * @throws IOException if a problem occurs
   */
  public static boolean checkFileExists(String file) throws IOException {
    return SparkUtils.checkFileExists(file);
  }

  /**
   * Get the size in bytes of a file/directory
   *
   * @param path the path to the file/directory
   * @return the size in bytes
   * @throws IOException if a problem occurs
   */
  public static long getSizeInBytesOfPath(String path) throws IOException {
    return SparkUtils.getSizeInBytesOfPath(path);
  }

  /**
   * Adds necessary Weka libraries to the supplied SparkContext
   *
   * @param context the context to add dependencies to
   * @throws WekaException if a problem occurs
   */
  protected void addWekaLibrariesToSparkContext(JavaSparkContext context)
    throws WekaException {
    m_sjConfig.addWekaLibrariesToSparkContext(context, this);
  }

  /**
   * Create a SparkContext for this job. Configures the context with required
   * libraries, additional weka packages etc.
   *
   * @param conf the configuration for the job
   * @throws WekaException if a problem occurs
   * @return a SparkContext for the job
   */
  public JavaSparkContext createSparkContextForJob(SparkJobConfig conf)
    throws WekaException {

    // check for windows
    String osType = System.getProperty("os.name");
    if (osType != null && osType.toLowerCase().contains("windows")) {
      // make sure that winutils.exe can be found so that Hadoop filesystem
      // stuff works properly - but only if HADOOP_HOME and hadoop.home.dir are
      // not already set
      if (System.getProperty("hadoop.home.dir") == null
        && System.getenv("HADOOP_HOME") == null
        && System.getenv("hadoop_home") == null) {
        System.setProperty("hadoop.home.dir",
          WekaPackageManager.PACKAGES_DIR.toString() + File.separator
            + "distributedWekaSparkDev" + File.separator + "windows");
      }
    }

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
    options.addAll(Arrays.asList(baseOptions));

    String[] configOpts = m_sjConfig.getOptions();
    options.addAll(Arrays.asList(configOpts));

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
    return SparkUtils.initSparkLogAppender(this);
  }

  /**
   * Remove the supplied appender from Spark's logging. This should be called at
   * the end of a job
   *
   * @param appender the appender to remove
   */
  public void removeSparkLogAppender(WriterAppender appender) {
    SparkUtils.removeSparkLogAppender(appender);
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
    return SparkUtils.determineClusterMemory(sparkContext);
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
   *          be {@code <= 0} to indicate that the default should be used)
   * @param enforceMaxPartitions if true then any max partitions specified by
   *          the user will be enforced (this might trigger a shuffle operation)
   * @return a {@code JavaRDD<Instance>} dataset
   * @throws IOException if a problem occurs
   */
  public JavaRDD<Instance> loadInstanceObjectFile(String path,
    JavaSparkContext sparkContext, CachingStrategy strategy, int minPartitions,
    boolean enforceMaxPartitions) throws IOException {
    String resolvedPath = SparkJob.resolveLocalOrOtherFileSystemPath(path);

    JavaRDD<Instance> dataset;

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
   * Process an {@code RDD<String>} into an {@code RDD<Instance>}
   *
   * @param input the {@code RDD<String>} input
   * @param headerNoSummary the header of the data without summary attributes
   * @param csvParseOptions the options for the CSV parser
   * @param strategy the optional caching strategy to use
   * @param enforceMaxPartitions if true then any max partitions specified by
   *          the user will be enforced (this might trigger a shuffle operation)
   * @return a {@code JavaRDD<Instance>} dataset
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
   *          be {@code <= 0} to indicate that the default should be used)
   * @param enforceMaxPartitions if true then any max partitions specified by
   *          the user will be enforced (this might trigger a shuffle operation)
   * @return a {@code JavaRDD<Instance>} dataset
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
   *          be {@code <= 0} to indicate that the default should be used)
   * @param enforceMaxPartitions if true then any max partitions specified by
   *          the user will be enforced (this might trigger a shuffle operation)
   * @return an {@code JavaRDD<Instance>} dataset
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
    m_datasetManager.setDataset(key, dataset);
  }

  /**
   * Return a named dataset, or null if the name is unknown.
   *
   * @param key the name of the dataset to get
   * @return the named dataset or null
   */
  public Dataset getDataset(String key) {
    return m_datasetManager.getDataset(key);
  }

  /**
   * Return an iterator over the named datasets for this job
   *
   * @return an iterator over the datasets for this job
   */
  public Iterator<Map.Entry<String, Dataset>> getDatasetIterator() {
    return m_datasetManager.getDatasetIterator();
  }

  /**
   * Get a collection of datasets for this job
   *
   * @return a collection of datasets
   */
  public Collection<Dataset> getDatasets() {
    return m_datasetManager.getDatasets();
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
    WriterAppender logAppender = MLlibSparkContextManager.getWriterAppender();
    if (logAppender == null) {
      logAppender = initSparkLogAppender();
    }
    if (context == null) {
      synchronized (MLlibSparkContextManager.class) {
        m_currentContext = MLlibSparkContextManager.getSparkContext();

        if (m_currentContext == null) {
          m_currentContext = createSparkContextForJob(m_sjConfig);
          MLlibSparkContextManager.setSparkContext(m_currentContext, logAppender);
        }
      }
    }
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
    /*
     * setCachingStrategy(new CachingStrategy(m_sjConfig.getInputFile(),
     * m_sjConfig.getAvailableClusterMemory() (m_sjConfig.getMemoryFraction() >
     * 0 && m_sjConfig.getMemoryFraction() <= 1 ? m_sjConfig
     * .getMemoryFraction() : 0.6), m_sjConfig.getInMemoryDataOverheadFactor(),
     * sparkContext));
     */

    // Use a default caching strategy here - FileDataSource overrides
    // to set based on file size
    setCachingStrategy(new CachingStrategy());

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
      // Let the shutdown hook handle terminating the spark context
      // shutdownJob(logAppender);
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
}
