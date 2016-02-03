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
 *    MapReduceJobConfig.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package distributed.hadoop;

import distributed.core.DistributedJobConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import weka.core.Environment;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * The main job configuration used by Weka Hadoop jobs
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class MapReduceJobConfig extends AbstractHadoopJobConfig implements
  OptionHandler {

  /** Internal key for the number of mappers to use */
  public static final String NUM_MAPPERS = "numMappers";

  /** Internal key for the number of reducers to use */
  public static final String NUM_REDUCERS = "numReducers";

  /**
   * Internal key for the maximum number of mappers that will run on a node
   * concurrently
   */
  public static final String TASK_TRACKER_MAP_MAXIMUM = "taskTrackerMaxMappers";

  /** Internal key for the name of the mapper class */
  public static final String MAPPER_CLASS = "mapperClass";

  /** Internal key for the name of the reducer class */
  public static final String REDUCER_CLASS = "reducerClass";

  /** Internal key for the name of the combiner class */
  public static final String COMBINER_CLASS = "combinerClass";

  /** Internal key for the name of the input format class to use */
  public static final String INPUT_FORMAT_CLASS = "inputFormatClass";

  /** Internal key for the name of the output format class to use */
  public static final String OUTPUT_FORMAT_CLASS = "outputFormatClass";

  /** Internal key for the name of the map output key class to use */
  public static final String MAP_OUTPUT_KEY_CLASS = "mapOutputKeyClass";

  /** Internal key for the name of the map output value class to use */
  public static final String MAP_OUTPUT_VALUE_CLASS = "mapOutputValueClass";

  /** Internal key for the name of the (job/reducer) output key to use */
  public static final String OUTPUT_KEY_CLASS = "outputKeyClass";

  /** Internal key for the name of the (job/reducer) output value to use */
  public static final String OUTPUT_VALUE_CLASS = "outputValueClass";

  /** Internal key for the input path(s) to use for the job */
  public static final String INPUT_PATHS = "inputPaths";

  /** Internal key for the output path to use for the job */
  public static final String OUTPUT_PATH = "outputPath";

  /** Internal key for the maximum block size (for splitting data) to use */
  public static final String MAPRED_MAX_SPLIT_SIZE = "mapredMaxSplitSize";

  /** Internal key for the Hadoop property for the job tracker host */
  public static final String HADOOP_JOB_TRACKER_HOST = "mapred.job.tracker";

  /** Internal key for the Hadoop property for the yarn resource manager address */
  public static final String YARN_RESOURCE_MANAGER_ADDRESS =
    "yarn.resourcemanager.address";

  /**
   * Internal key for the Hadoop property for the yarn resource manager
   * scheduler address. Weka will use yarn.resource.manager.address:8030 to set
   * this. If this is not appropriate for a particular cluster it can be
   * overridden using -user-prop arguments to jobs, or by placing the cluster
   * configuration directory in the classpath when running jobs.
   */
  public static final String YARN_RESOURCE_MANAGER_SCHEDULER_ADDRESS =
    "yarn.resourcemanager.scheduler.address";

  /** Internal key for the Hadoop 1 property for the maximum block size */
  public static final String HADOOP_MAPRED_MAX_SPLIT_SIZE =
    "mapred.max.split.size";

  /** Internal key for the Haddop 2 property for the maximum block size */
  public static final String HADOOP2_MAPRED_MAX_SPLIT_SIZE =
    "mapreduce.input.fileinputformat.split.maxsize";

  /**
   * Internal key for the Hadoop 1 property for the maximum number of number of
   * reducers to run per node
   */
  public static final String HADOOP_TASKTRACKER_REDUCE_TASKS_MAXIMUM =
    "mapred.tasktracker.reduce.tasks.maximum";

  /**
   * Internal key for the Hadoop 2 property for the maximum number of number of
   * reducers to run per node
   */
  public static final String HADOOP2_TASKTRACKER_REDUCE_TASKS_MAXIMUM =
    "mapreduce.tasktracker.reduce.tasks.maximum";

  /** For serialization */
  private static final long serialVersionUID = -1721850598954532369L;

  /** HDFSConfig for HDFS properties */
  protected HDFSConfig m_hdfsConfig = new HDFSConfig();

  /**
   * Constructor - sets defaults
   */
  public MapReduceJobConfig() {

    // defaults
    if (AbstractHadoopJobConfig.isHadoop2()) {
      // default port for resource manager
      setJobTrackerPort(AbstractHadoopJobConfig.DEFAULT_PORT_YARN);
    } else {
      setJobTrackerPort(AbstractHadoopJobConfig.DEFAULT_PORT);
    }
    setJobTrackerHost("localhost");
    getHDFSConfig().setHDFSHost("localhost");
    getHDFSConfig().setHDFSPort("8020");

    // some defaults for Weka style jobs. Mappers tend to create
    // some sort of object (classifier, instances etc.) to be
    // aggregated in the Reducer. Reducers will proably write
    // the aggregated object via serialization to a custom
    // location in HDFS. Output from the job is probably just
    // some status information
    setProperty(INPUT_FORMAT_CLASS, TextInputFormat.class.getName());
    setProperty(OUTPUT_FORMAT_CLASS, TextOutputFormat.class.getName());

    setProperty(MAP_OUTPUT_KEY_CLASS, Text.class.getName());
    setProperty(MAP_OUTPUT_VALUE_CLASS, BytesWritable.class.getName());

    setProperty(OUTPUT_KEY_CLASS, Text.class.getName());
    setProperty(OUTPUT_VALUE_CLASS, Text.class.getName());
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    // HDFS options
    Enumeration<Option> hdfsOpts = m_hdfsConfig.listOptions();
    while (hdfsOpts.hasMoreElements()) {
      Option o = hdfsOpts.nextElement();
      result.add(o);
    }

    result.addElement(new Option(
      "\tJob tracker/resource manager hostname. (default: localhost)",
      "jobtracker-host", 1, "-jobtracker-host <hostname>"));
    result.addElement(new Option(
      "\tJob tracker/resource manager port. (default 8021/8032)",
      "jobtracker-port", 1, "-jobtracker-port <port number>"));
    result.addElement(new Option("\tThe number of maps (hint to MR).",
      "num-maps", 1, "-num-maps <integer>"));
    result.addElement(new Option("\tMaximum number of map tasks to run "
      + "concurrently per node.", "map-maximum", 1, "-map-maximum"));
    result.addElement(new Option("\tNumber of reducers to use.",
      "num-reducers", 1, "-num-reducers"));
    result.addElement(new Option("\tInput path(s) in HDFS (comma-separated)",
      "input-paths", 1, "-input-paths"));
    result.addElement(new Option("\tOutput path for the job in HDFS.",
      "output-path", 1, "-output-path"));
    result.addElement(new Option(
      "\tMaximum split size (in bytes) for each mapper to " + "process.",
      "max-split-size", 1, "-max-split-size"));

    Enumeration<Option> superOpts = super.listOptions();
    while (superOpts.hasMoreElements()) {
      result.add(superOpts.nextElement());
    }

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    super.setOptions(options);

    setJobTrackerHost(Utils.getOption("jobtracker-host", options));
    setJobTrackerPort(Utils.getOption("jobtracker-port", options));
    setNumberOfMaps(Utils.getOption("num-maps", options));
    setTaskTrackerMapTasksMaximum(Utils.getOption("map-maximum", options));
    setNumberOfReducers(Utils.getOption("num-reducers", options));
    setInputPaths(Utils.getOption("input-paths", options));
    setOutputPath(Utils.getOption("output-path", options));
    setMapredMaxSplitSize(Utils.getOption("max-split-size", options));

    m_hdfsConfig.setOptions(options);
  }

  @Override
  public String[] getOptions() {
    List<String> opts = new ArrayList<String>();

    String[] hdfsOpts = m_hdfsConfig.getOptions();
    for (String o : hdfsOpts) {
      opts.add(o);
    }

    opts.add("-jobtracker-host");
    opts.add(getJobTrackerHost());

    if (!DistributedJobConfig.isEmpty(getJobTrackerPort())) {
      opts.add("-jobtracker-port");
      opts.add(getJobTrackerPort());
    }

    if (!DistributedJobConfig.isEmpty(getNumberOfMaps())) {
      opts.add("-num-maps");
      opts.add(getNumberOfMaps());
    }

    if (!DistributedJobConfig.isEmpty(getTaskTrackerMapTasksMaximum())) {
      opts.add("-map-maximum");
      opts.add(getTaskTrackerMapTasksMaximum());
    }

    if (!DistributedJobConfig.isEmpty(getNumberOfReducers())) {
      opts.add("-num-reducers");
      opts.add(getNumberOfReducers());
    }

    if (!DistributedJobConfig.isEmpty(getInputPaths())) {
      opts.add("-input-paths");
      opts.add(getInputPaths());
    }

    if (!DistributedJobConfig.isEmpty(getOutputPath())) {
      opts.add("-output-path");
      opts.add(getOutputPath());
    }

    if (!DistributedJobConfig.isEmpty(getMapredMaxSplitSize())) {
      opts.add("-max-split-size");
      opts.add(getMapredMaxSplitSize());
    }

    String[] superOpts = super.getOptions();
    for (String s : superOpts) {
      opts.add(s);
    }

    return opts.toArray(new String[opts.size()]);
  }

  /**
   * Set the HDFSConfig to use
   * 
   * @param config the HDFSConfig to use
   */
  public void setHDFSConfig(HDFSConfig config) {
    m_hdfsConfig = config;
  }

  /**
   * Get the HDFSConfig to use
   * 
   * @return the HDFSConfig to use
   */
  public HDFSConfig getHDFSConfig() {
    return m_hdfsConfig;
  }

  /**
   * Get the tool tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String HDFSHostTipText() {
    return "The HDFS (name node) host to use";
  }

  /**
   * Set the HDFSHost (name node)
   * 
   * @param host the HDFS host
   */
  public void setHDFSHost(String host) {
    m_hdfsConfig.setHDFSHost(host);
  }

  /**
   * Get the HDFS host (name node)
   * 
   * @return the HDFS host
   */
  public String getHDFSHost() {
    return m_hdfsConfig.getHDFSHost();
  }

  /**
   * Get the tool tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String HDFSPortTipText() {
    return "The HDFS (name node) port";
  }

  /**
   * Set the HDFS port
   * 
   * @param port the HDFS port
   */
  public void setHDFSPort(String port) {
    m_hdfsConfig.setHDFSPort(port);
  }

  /**
   * Get the HDFS port
   * 
   * @return the HDFS port
   */
  public String getHDFSPort() {
    return m_hdfsConfig.getHDFSPort();
  }

  /**
   * Get the tool tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String numberOfMapsTipText() {
    return "The number of maps to use. This is just a hint to the underlying Hadoop"
      + " framework for how many maps to use. Using setMapredMaxSplitSize(), which"
      + " sets the Hadoop property mapred.max.split.size, gives greater control over"
      + " how many maps will be run (and thus how much data each map processes).";
  }

  /**
   * Set the number of maps to use. This is just a hint to the underlying Hadoop
   * framework for how many maps to use. Using setMapredMaxSplitSize(), which
   * sets the Hadoop property mapred.max.split.size, gives greater control over
   * how many maps will be run (and thus how much data each map processes).
   * 
   * @param nM the number of maps to use
   */
  public void setNumberOfMaps(String nM) {
    setProperty(NUM_MAPPERS, nM);
  }

  /**
   * Get the number of maps to use. This is just a hint to the underlying Hadoop
   * framework for how many maps to use. Using setMapredMaxSplitSize(), which
   * sets the Hadoop property mapred.max.split.size, gives greater control over
   * how many maps will be run (and thus how much data each map processes).
   * 
   * @return the number of maps to use
   */
  public String getNumberOfMaps() {
    return getProperty(NUM_MAPPERS);
  }

  /**
   * Get the tool tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String taskTrackerMapTasksMaximumTipText() {
    return "The maximum number of map tasks to run concurrently by a task tracker (node)";
  }

  /**
   * Set the maximum number of map tasks to run concurrently by a task tracker
   * (node). The cluster setting for this will be used if not specified here
   * 
   * @param mmt the maximum number of map tasks to run concurrently by a task
   *          tracker
   */
  public void setTaskTrackerMapTasksMaximum(String mmt) {
    setProperty(TASK_TRACKER_MAP_MAXIMUM, mmt);
  }

  /**
   * Get the maximum number of map tasks to run concurrently by a task tracker
   * (node). The cluster setting for this will be used if not specified here
   * 
   * @return the maximum number of map tasks to run concurrently by a task
   *         tracker
   */
  public String getTaskTrackerMapTasksMaximum() {
    return getProperty(TASK_TRACKER_MAP_MAXIMUM);
  }

  /**
   * Get the tool tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String numberOfReducersTipText() {
    return "The number of reducers to use. Weka jobs set this property automatically.";
  }

  /**
   * Set the number of reducers to use. Weka jobs set this property
   * automatically
   * 
   * @param nR the number of reducers to use.
   */
  public void setNumberOfReducers(String nR) {
    setProperty(NUM_REDUCERS, nR);
  }

  /**
   * Get the number of reducers to use. Weka jobs set this property
   * automatically
   * 
   * @return the number of reducers to use.
   */
  public String getNumberOfReducers() {
    return getProperty(NUM_REDUCERS);
  }

  /**
   * Set the mapper class name to use. Weka jobs configure this automatically.
   * 
   * @param mapperClass the mapper class name
   */
  public void setMapperClass(String mapperClass) {
    setProperty(MAPPER_CLASS, mapperClass);
  }

  /**
   * Get the mapper class name to use. Weka jobs configure this automatically.
   * 
   * @return the mapper class name
   */
  public String getMapperClass() {
    return getProperty(MAPPER_CLASS);
  }

  /**
   * Set the name of the reducer class to use. Weka jobs set this automatically.
   * 
   * @param reducerClass the name of the reducer class
   */
  public void setReducerClass(String reducerClass) {
    setProperty(REDUCER_CLASS, reducerClass);
  }

  /**
   * Get the name of the reducer class to use. Weka jobs set this automatically.
   * 
   * @return the name of the reducer class
   */
  public String getReducerClass() {
    return getProperty(REDUCER_CLASS);
  }

  /**
   * Set the name of the reducer class (if any) to use. Weka jobs may set this
   * automatically
   * 
   * @param combinerClass the name of the combiner class to use
   */
  public void setCombinerClass(String combinerClass) {
    setProperty(COMBINER_CLASS, combinerClass);
  }

  /**
   * Get the name of the reducer class (if any) to use. Weka jobs may set this
   * automatically
   * 
   * @return the name of the combiner class to use
   */
  public String getCombinerClass() {
    return getProperty(COMBINER_CLASS);
  }

  /**
   * Set the name of the input format class to use. Weka jobs set this
   * automatically.
   * 
   * @param inputFormatClass the name of the input format class to use
   */
  public void setInputFormatClass(String inputFormatClass) {
    setProperty(INPUT_FORMAT_CLASS, inputFormatClass);
  }

  /**
   * Get the name of the input format class to use. Weka jobs set this
   * automatically.
   * 
   * @return the name of the input format class to use
   */
  public String getInputFormatClass() {
    return getProperty(INPUT_FORMAT_CLASS);
  }

  /**
   * Set the name of the output format class to use. Weka jobs set this
   * automatically.
   * 
   * @param outputFormatClass the name of the output format class to use.
   */
  public void setOutputFormatClass(String outputFormatClass) {
    setProperty(OUTPUT_FORMAT_CLASS, outputFormatClass);
  }

  /**
   * Get the name of the output format class to use. Weka jobs set this
   * automatically.
   * 
   * @return the name of the output format class to use.
   */
  public String getOutputFormatClass() {
    return getProperty(OUTPUT_FORMAT_CLASS);
  }

  /**
   * Set the name of the map output key class to use. Weka jobs set this
   * automatically.
   * 
   * @param mapOutputKeyClass the name of the map output key class
   */
  public void setMapOutputKeyClass(String mapOutputKeyClass) {
    setProperty(MAP_OUTPUT_KEY_CLASS, mapOutputKeyClass);
  }

  /**
   * Get the name of the map output key class to use. Weka jobs set this
   * automatically.
   * 
   * @return the name of the map output key class
   */
  public String getMapOutputKeyClass() {
    return getProperty(MAP_OUTPUT_KEY_CLASS);
  }

  /**
   * Set the name of the map output value class to use. Weka jobs set this
   * automatically.
   * 
   * @param mapOutputValueClass the name of the map output value class
   */
  public void setMapOutputValueClass(String mapOutputValueClass) {
    setProperty(MAP_OUTPUT_VALUE_CLASS, mapOutputValueClass);
  }

  /**
   * Get the name of the map output value class to use. Weka jobs set this
   * automatically.
   * 
   * @return the name of the map output value class
   */
  public String getMapOutputValueClass() {
    return getProperty(MAP_OUTPUT_VALUE_CLASS);
  }

  /**
   * Set the name of the (reducer) output key class to use
   * 
   * @param outputKeyClass the name of the output key class to use
   */
  public void setOutputKeyClass(String outputKeyClass) {
    setProperty(OUTPUT_KEY_CLASS, outputKeyClass);
  }

  /**
   * Get the name of the (reducer) output key class to use
   * 
   * @return the name of the output key class to use
   */
  public String getOutputKeyClass() {
    return getProperty(OUTPUT_KEY_CLASS);
  }

  /**
   * Set the name of the (reducer) output value class to use
   * 
   * @param outputValueClass the name of the output value class to use
   */
  public void setOutputValueClass(String outputValueClass) {
    setProperty(OUTPUT_VALUE_CLASS, outputValueClass);
  }

  /**
   * Get the name of the (reducer) output value class to use
   * 
   * @return the name of the output value class to use
   */
  public String getOutputValueClass() {
    return getProperty(OUTPUT_VALUE_CLASS);
  }

  /**
   * Get the tip text for this property
   * 
   * @return the tip text for this property
   */
  public String inputPathsTipText() {
    return "The path to the directory in HDFS that contains the input files";
  }

  /**
   * Set the input path(s) to use
   * 
   * @param inputPaths the input paths to use
   */
  public void setInputPaths(String inputPaths) {
    setProperty(INPUT_PATHS, inputPaths);
  }

  /**
   * Get the input path(s) to use
   * 
   * @return the input paths to use
   */
  public String getInputPaths() {
    return getProperty(INPUT_PATHS);
  }

  /**
   * Get the tip text for this property
   * 
   * @return the tip text for this property
   */
  public String outputPathTipText() {
    return "The path in HDFS to the output directory";
  }

  /**
   * Set the output path to use
   * 
   * @param outputPath the output path to use
   */
  public void setOutputPath(String outputPath) {
    setProperty(OUTPUT_PATH, outputPath);
  }

  /**
   * Get the output path to use
   * 
   * @return the output path to use
   */
  public String getOutputPath() {
    return getProperty(OUTPUT_PATH);
  }

  /**
   * Set the maximum split size (in bytes). This can be used to control the
   * number of maps that run. The default block size of 64Mb may be too large
   * for some batch learning tasks depending on data characteristics, choice of
   * learning algorithm and available RAM on the node.
   * 
   * @param maxSize the maximum split size (in bytes)
   */
  public void setMapredMaxSplitSize(String maxSize) {
    setProperty(MAPRED_MAX_SPLIT_SIZE, maxSize);
  }

  /**
   * Get the maximum split size (in bytes). This can be used to control the
   * number of maps that run. The default block size of 64Mb may be too large
   * for some batch learning tasks depending on data characteristics, choice of
   * learning algorithm and available RAM on the node.
   * 
   * @return the maximum split size (in bytes)
   */
  public String getMapredMaxSplitSize() {
    return getProperty(MAPRED_MAX_SPLIT_SIZE);
  }

  /**
   * Substitute environment variables in a given string
   * 
   * @param s the string to substitute in
   * @param env environment variables
   * @return the string with variables replaced
   */
  protected static String environmentSubstitute(String s, Environment env) {
    if (env != null) {
      try {
        s = env.substitute(s);
      } catch (Exception ex) {
      }
    }

    return s;
  }

  /**
   * Apply the settings encapsulated in this config and return a Job object
   * ready for execution.
   * 
   * @param jobName the name of the job
   * @param conf the Configuration object that will be wrapped in the Job
   * @param env environment variables
   * @return a configured Job object
   * @throws IOException if a problem occurs
   * @throws ClassNotFoundException if various classes are not found
   */
  public Job configureForHadoop(String jobName, Configuration conf,
    Environment env) throws IOException, ClassNotFoundException {

    String jobTrackerPort = getJobTrackerPort();
    if (DistributedJobConfig.isEmpty(jobTrackerPort)) {
      jobTrackerPort =
        AbstractHadoopJobConfig.isHadoop2() ? AbstractHadoopJobConfig.DEFAULT_PORT_YARN
          : AbstractHadoopJobConfig.DEFAULT_PORT;
    }
    String jobTracker = getJobTrackerHost() + ":" + jobTrackerPort;
    if (DistributedJobConfig.isEmpty(jobTracker)) {
      System.err.println("No "
        + (AbstractHadoopJobConfig.isHadoop2() ? "resource manager "
          : "JobTracker ") + "set - running locally...");
    } else {
      jobTracker = environmentSubstitute(jobTracker, env);
      if (AbstractHadoopJobConfig.isHadoop2()) {
        conf.set(YARN_RESOURCE_MANAGER_ADDRESS, jobTracker);
        conf.set(YARN_RESOURCE_MANAGER_SCHEDULER_ADDRESS,
          environmentSubstitute(getJobTrackerHost(), env) + ":8030");
      } else {
        conf.set(HADOOP_JOB_TRACKER_HOST, jobTracker);
      }
    }
    System.err.println("Using "
      + (AbstractHadoopJobConfig.isHadoop2() ? "resource manager: "
        : "jobtracker: ") + jobTracker);

    if (AbstractHadoopJobConfig.isHadoop2()) {
      // a few other properties needed to run against Yarn
      conf.set("yarn.nodemanager.aux-services", "mapreduce_shuffle");
      conf.set("mapreduce.framework.name", "yarn");
    }

    if (!DistributedJobConfig.isEmpty(getMapredMaxSplitSize())) {
      conf.set(
        AbstractHadoopJobConfig.isHadoop2() ? HADOOP2_MAPRED_MAX_SPLIT_SIZE
          : HADOOP_MAPRED_MAX_SPLIT_SIZE, getMapredMaxSplitSize());
    }

    // Do any user supplied properties here before creating the Job
    for (Map.Entry<String, String> e : m_additionalUserSuppliedProperties
      .entrySet()) {
      conf.set(e.getKey(), e.getValue());
    }

    m_hdfsConfig.configureForHadoop(conf, env);
    Job job = new Job(conf, jobName);

    String numMappers = getNumberOfMaps();
    if (!DistributedJobConfig.isEmpty(numMappers)) {
      numMappers = environmentSubstitute(numMappers, env);
      ((JobConf) job.getConfiguration()).setNumMapTasks(Integer
        .parseInt(numMappers));
    }

    // The number of map tasks that will be run simultaneously by a task tracker
    String maxConcurrentMapTasks = getTaskTrackerMapTasksMaximum();
    if (!DistributedJobConfig.isEmpty(maxConcurrentMapTasks)) {
      ((JobConf) job.getConfiguration()).set(
        "mapred.tasktracker.map.tasks.maximum", maxConcurrentMapTasks);
    }

    String numReducers = getNumberOfReducers();
    if (!DistributedJobConfig.isEmpty(numReducers)) {
      numReducers = environmentSubstitute(numReducers, env);
      job.setNumReduceTasks(Integer.parseInt(numReducers));

      if (Integer.parseInt(numReducers) == 0) {
        System.err
          .println("Warning - no reducer class set. Configuring for a map only job");
      }
    } else {
      job.setNumReduceTasks(1);
    }
    String mapperClass = getMapperClass();
    if (DistributedJobConfig.isEmpty(mapperClass)) {
      throw new IOException("No mapper class specified!");
    }
    mapperClass = environmentSubstitute(mapperClass, env);

    @SuppressWarnings("unchecked")
    Class<? extends Mapper> mc =
      (Class<? extends Mapper>) Class.forName(mapperClass);

    job.setMapperClass(mc);

    String reducerClass = getReducerClass();
    if (DistributedJobConfig.isEmpty(reducerClass)
      && Integer.parseInt(numReducers) > 0) {
      throw new IOException("No reducer class specified!");
    } else if (job.getNumReduceTasks() > 0) {
      reducerClass = environmentSubstitute(reducerClass, env);

      @SuppressWarnings("unchecked")
      Class<? extends Reducer> rc =
        (Class<? extends Reducer>) Class.forName(reducerClass);

      job.setReducerClass(rc);
    }

    String combinerClass = getCombinerClass();
    if (!DistributedJobConfig.isEmpty(combinerClass)) {
      combinerClass = environmentSubstitute(combinerClass, env);

      @SuppressWarnings("unchecked")
      Class<? extends Reducer> cc =
        (Class<? extends Reducer>) Class.forName(combinerClass);

      job.setCombinerClass(cc);
    }

    String inputFormatClass = getInputFormatClass();
    if (DistributedJobConfig.isEmpty(inputFormatClass)) {
      throw new IOException("No input format class specified");
    }
    inputFormatClass = environmentSubstitute(inputFormatClass, env);

    @SuppressWarnings("unchecked")
    Class<? extends InputFormat> ifc =
      (Class<? extends InputFormat>) Class.forName(inputFormatClass);

    job.setInputFormatClass(ifc);

    String outputFormatClass = getOutputFormatClass();
    if (DistributedJobConfig.isEmpty(outputFormatClass)) {
      throw new IOException("No output format class specified");
    }
    outputFormatClass = environmentSubstitute(outputFormatClass, env);

    @SuppressWarnings("unchecked")
    Class<? extends OutputFormat> ofc =
      (Class<? extends OutputFormat>) Class.forName(outputFormatClass);
    job.setOutputFormatClass(ofc);

    String mapOutputKeyClass = getMapOutputKeyClass();
    if (DistributedJobConfig.isEmpty(mapOutputKeyClass)) {
      throw new IOException("No map output key class defined");
    }
    mapOutputKeyClass = environmentSubstitute(mapOutputKeyClass, env);
    Class mokc = Class.forName(mapOutputKeyClass);
    job.setMapOutputKeyClass(mokc);

    String mapOutputValueClass = getMapOutputValueClass();
    if (DistributedJobConfig.isEmpty(mapOutputValueClass)) {
      throw new IOException("No map output value class defined");
    }
    mapOutputValueClass = environmentSubstitute(mapOutputValueClass, env);
    Class movc = Class.forName(mapOutputValueClass);
    job.setMapOutputValueClass(movc);

    String outputKeyClass = getOutputKeyClass();
    if (DistributedJobConfig.isEmpty(outputKeyClass)) {
      throw new IOException("No output key class defined");
    }
    outputKeyClass = environmentSubstitute(outputKeyClass, env);
    Class okc = Class.forName(outputKeyClass);
    job.setOutputKeyClass(okc);

    String outputValueClass = getOutputValueClass();
    if (DistributedJobConfig.isEmpty(outputValueClass)) {
      throw new IOException("No output value class defined");
    }
    outputValueClass = environmentSubstitute(outputValueClass, env);
    Class ovc = Class.forName(outputValueClass);
    job.setOutputValueClass(ovc);

    String inputPaths = getInputPaths();
    // don't complain if there aren't any as inputs such as HBASE
    // require other properties to be set
    if (!DistributedJobConfig.isEmpty(inputPaths)) {
      inputPaths = environmentSubstitute(inputPaths, env);
      FileInputFormat.setInputPaths(job, inputPaths);
    }

    String outputPath = getOutputPath();
    if (DistributedJobConfig.isEmpty(outputPath)) {
      throw new IOException("No output path specified");
    }
    outputPath = environmentSubstitute(outputPath, env);
    FileOutputFormat.setOutputPath(job, new Path(outputPath));

    return job;
  }

  /**
   * Clean the output directory specified for the supplied job
   * 
   * @param job the job to clean the output directory for
   * @param env environment variables
   * @throws IOException if a problem occurs
   */
  public void deleteOutputDirectory(Job job, Environment env)
    throws IOException {
    Configuration conf = job.getConfiguration();
    String outputDir = getOutputPath();

    if (DistributedJobConfig.isEmpty(outputDir)) {
      throw new IOException("Can't delete output path - no path defined!");
    }

    HDFSUtils.deleteDirectory(getHDFSConfig(), conf, outputDir, env);
  }
}
