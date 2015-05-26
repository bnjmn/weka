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
 *    WekaClassifierHadoopJob
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

import weka.classifiers.Classifier;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.WekaClassifierMapTask;
import weka.gui.beans.ClassifierProducer;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.hadoop.HDFSUtils;
import distributed.hadoop.MapReduceJobConfig;

/**
 * Hadoop job for building a classifier or regressor.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierHadoopJob extends HadoopJob implements
  ClassifierProducer, CommandlineRunnable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 5266821649358242686L;

  /**
   * The subdirectory of the output directory that this job saves its results to
   */
  protected static final String OUTPUT_SUBDIR = "/model";

  /**
   * Number of iterations over the data (only for incremental learners such as
   * SGD)
   */
  protected int m_numIterations = 1;

  /** Default name for the model */
  protected String m_modelName = "outputModel.model";

  /** Path to a pre-constructed filter to use (if any) */
  protected String m_pathToPreconstructedFilter = "";

  /** Class index or name */
  protected String m_classIndex = "";

  /** Holds the options to the map task */
  protected String m_wekaClassifierMapTaskOpts = "";

  /** Holds the options to the header task */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /** ARFF job */
  protected ArffHeaderHadoopJob m_arffHeaderJob = new ArffHeaderHadoopJob();

  /** Randomized data chunk job */
  protected RandomizedDataChunkHadoopJob m_randomizeJob =
    new RandomizedDataChunkHadoopJob();

  /** Full path to the final model in HDFS */
  protected String m_hdfsPathToAggregatedClassifier = "";

  /**
   * Minimum fraction of the largest number of training instances processed by
   * any classifier on its split of the data to allow a classifier to be
   * aggregated. Default = 50%, (defined in WekaClassifierMapTask) so any
   * classifier that has trained on less than 50% of the number of instances
   * processed by the classifier that has seen the most data is discarded.
   */
  protected String m_minTrainingFraction = "";

  /**
   * True if the job to create randomly shuffled (and stratified) data chunks is
   * to be run before building the model
   */
  protected boolean m_createRandomizedDataChunks;

  /**
   * Number of data chunks to create (either this or numInstancesPerDataChunk
   * should be specified)
   */
  protected String m_numDataChunks = "";

  /**
   * Number of instances per data chunk (determines how many data chunks are
   * created)
   */
  protected String m_numInstancesPerDataChunk = "";

  /** The configuration for the randomized data chunk creation phase */
  protected MapReduceJobConfig m_randomizeConfig = new MapReduceJobConfig();

  /** The final classifier produced by this job */
  protected Classifier m_finalClassifier;

  /**
   * Constructor
   */
  public WekaClassifierHadoopJob() {
    super("Weka classifier builder job", "Build an aggregated Weka classifier");

    m_mrConfig.setMapperClass(WekaClassifierHadoopMapper.class.getName());
    m_mrConfig.setReducerClass(WekaClassifierHadoopReducer.class.getName());
  }

  /**
   * Help information
   * 
   * @return help information for this job
   */
  public String globalInfo() {
    return "Trains a classifier - produces a single model of the "
      + "same type for Aggregateable classifiers or a "
      + "voted ensemble of the base classifiers if they "
      + "are not directly aggregateable";
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String modelFileNameTipText() {
    return "The name only (not full path) that the model should be saved to";
  }

  /**
   * Set the name only for the model file
   * 
   * @param m the name only (not full path) that the model should be saved to
   */
  public void setModelFileName(String m) {
    m_modelName = m;
  }

  /**
   * Get the name only for the model file
   * 
   * @return the name only (not full path) that the model should be saved to
   */
  public String getModelFileName() {
    return m_modelName;
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String numIterationsTipText() {
    return "The number of iterations to run in the model building phase - >1 only "
      + "makes sense for incremental classifiers such as SGD.";
  }

  /**
   * Set the number of iterations (passes over the data) to run in the model
   * building phase. > 1 only makes sense for incremental classifiers such as
   * SGD that converge on a solution.
   * 
   * @param i the number of iterations to run
   */
  public void setNumIterations(int i) {
    m_numIterations = i;
  }

  /**
   * Get the number of iterations (passes over the data) to run in the model
   * building phase. > 1 only makes sense for incremental classifiers such as
   * SGD that converge on a solution.
   * 
   * @return the number of iterations to run
   */
  public int getNumIterations() {
    return m_numIterations;
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String classAttributeTipText() {
    return "The name or index of the class attribute. 'first' and "
      + "'last' may also be used.";
  }

  /**
   * Set the name or index of the class attribute ("first" and "last" can also
   * be used)
   * 
   * @param c the name or index of the class attribute
   */
  public void setClassAttribute(String c) {
    m_classIndex = c;
  }

  /**
   * Get the name or index of the class attribute ("first" and "last" can also
   * be used)
   * 
   * @return the name or index of the class attribute
   */
  public String getClassAttribute() {
    return m_classIndex;
  }

  /**
   * Set the options to the header job
   * 
   * @param opts options to the header job
   */
  public void setCSVMapTaskOptions(String opts) {
    m_wekaCsvToArffMapTaskOpts = opts;
  }

  /**
   * Get the options to the header job
   * 
   * @return options to the header job
   */
  public String getCSVMapTaskOptions() {
    return m_wekaCsvToArffMapTaskOpts;
  }

  /**
   * Set the options for the underlying map task
   * 
   * @param opts the options for the underlying map task
   */
  public void setClassifierMapTaskOptions(String opts) {
    m_wekaClassifierMapTaskOpts = opts;
  }

  /**
   * Get the options for the underlying map task
   * 
   * @return the options for the underlying map task
   */
  public String getClassifierMapTaskOptions() {
    return m_wekaClassifierMapTaskOpts;
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String minTrainingFractionTipText() {
    return "The fraction of training instances below which a model learned "
      + "by a map task will be discarded from the aggregation. This is "
      + "a percentage of the total number of instances seen by the map "
      + "task that has seen the mpst data. This option is useful when "
      + "not using randomly shuffled data chunks, as there may be one "
      + "input slit that contains significantly less data than all the "
      + "others, and we might want to discard the model learned on this "
      + "one.";
  }

  /**
   * Set the minimum training fraction. This is a percentage of the total number
   * of instances seen by the map task that has seen the most data. This option
   * is useful when not using randomly shuffled data chunks, as there may be one
   * input split that contains significantly less data than all the others and
   * we might want to discard the model learned on this chunk.
   * 
   * @param frac the fraction of training instances below which a model should
   *          be discarded from the aggregation
   */
  public void setMinTrainingFraction(String frac) {
    m_minTrainingFraction = frac;
  }

  /**
   * Get the minimum training fraction. This is a percentage of the total number
   * of instances seen by the map task that has seen the most data. This option
   * is useful when not using randomly shuffled data chunks, as there may be one
   * input split that contains significantly less data than all the others and
   * we might want to discard the model learned on this chunk.
   * 
   * @return the fraction of training instances below which a model should be
   *         discarded from the aggregation
   */
  public String getMinTrainingFraction() {
    return m_minTrainingFraction;
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String pathToPreconstructedFilterTipText() {
    return "The path to a (optional) pre-constructed filter to use. The filter "
      + "may reside in HDFS or on the local file system.";
  }

  /**
   * Set the path to a pre-constructed filter to use to pre-process the data
   * entering each map. This path may be inside or outside of HDFS.
   * 
   * @param path the path to a pre-constructed filter to use
   */
  public void setPathToPreconstructedFilter(String path) {
    m_pathToPreconstructedFilter = path;
  }

  /**
   * Get the path to a pre-constructed filter to use to pre-process the data
   * entering each map. This path may be inside or outside of HDFS.
   * 
   * @return the path to a pre-constructed filter to use
   */
  public String getPathToPreconstructedFilter() {
    return m_pathToPreconstructedFilter;
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String createRandomizedDataChunksTipText() {
    return "Create randomly shuffled (and stratified if class is nominal) data "
      + "chunks. This involves an extra pass (job) over the data. One of "
      + "numRandomizedDataChunks or numInstancesPerRandomizedDataChunk must "
      + "be set in conjunction with this option.";
  }

  /**
   * Set whether to create randomly shuffled (and stratified if the class is
   * nominal) data chunks via a pre-processing pass/job.
   * 
   * @param s true if randomly shuffled data chunks are to be created for input
   */
  public void setCreateRandomizedDataChunks(boolean s) {
    m_createRandomizedDataChunks = s;
  }

  /**
   * Get whether to create randomly shuffled (and stratified if the class is
   * nominal) data chunks via a pre-processing pass/job.
   * 
   * @return true if randomly shuffled data chunks are to be created for input
   */
  public boolean getCreateRandomizedDataChunks() {
    return m_createRandomizedDataChunks;
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String numRandomizedDataChunksTipText() {
    return "The number of randomly shuffled data chunks to create. Use in "
      + "conjunction with createRandomizedDataChunks";
  }

  /**
   * Set the number of randomly shuffled data chunks to create. Use in
   * conjunction with createRandomizedDataChunks.
   * 
   * @param chunks the number of chunks to create.
   */
  public void setNumRandomizedDataChunks(String chunks) {
    m_numDataChunks = chunks;
  }

  /**
   * Get the number of randomly shuffled data chunks to create. Use in
   * conjunction with createRandomizedDataChunks.
   * 
   * @return the number of chunks to create.
   */
  public String getNumRandomizedDataChunks() {
    return m_numDataChunks;
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String numInstancesPerRandomizedDataChunkTipText() {
    return "The number of instances that each randomly shuffled data chunk "
      + "should contain. Use in conjunction with createRandomizedDataChunks.";
  }

  /**
   * Set the number of instances that each randomly shuffled data chunk should
   * have. Use in conjunction with createRandomizedDataChunks.
   * 
   * @param insts the number of instances that each randomly shuffled data chunk
   *          should contain
   */
  public void setNumInstancesPerRandomizedDataChunk(String insts) {
    m_numInstancesPerDataChunk = insts;
  }

  /**
   * Get the number of instances that each randomly shuffled data chunk should
   * have. Use in conjunction with createRandomizedDataChunks.
   * 
   * @return the number of instances that each randomly shuffled data chunk
   *         should contain
   */
  public String getNumInstancesPerRandomizedDataChunk() {
    return m_numInstancesPerDataChunk;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result
      .add(new Option(
        "\tCreate data chunks with the order of the input instances\n\t"
          + "shuffled randomly. Also stratifies the data if the class\n\t"
          + "is nominal. Use in conjuction with -num-chunks or -num-instances-per-chunk",
        "randomized-chunks", 0, "-randomized-chunks"));
    result.add(new Option(
      "\tNumber of randomized data chunks. Use in conjunction with\n\t"
        + "-randomized-chunks", "num-chunks", 1, "-num-chunks <integer>"));
    result.add(new Option(
      "\tNumber of instances per randomized data chunk.\n\t"
        + "Use in conjunction with -randomized-chunks",
      "num-instances-per-chunk", 1, "-num-instances-per-chunk <integer>"));

    result.add(new Option("\tName of output model file. Model will be\n\t"
      + "written to output-path/model/<model name>", "model-file-name", 1,
      "-model-file-name <model-name>"));
    result.add(new Option(
      "\tNumber of iterations over the data (default = 1).\n\t"
        + "More than 1 iteration only makes sense for classifiers such\n\t"
        + "as SGD and SGDText", "num-iterations", 1, "-num-iterations <num>"));
    result.add(new Option("\tClass index (1-based) or class attribute name "
      + "(default = last attribute).", "class", 1, "-class <index or name>"));
    result
      .add(new Option(
        "\tMinimum allowable fraction of the largest number of training instances\n\t"
          + "processed by any classifier on its split of the data. Any classifier\n\t"
          + "that sees less than this percentage of instances on its data set\n\t"
          + "will be discarded and not form part of the final aggregation\n\t"
          + "(default = 0.5)", "aggregation-fraction", 1,
        "-aggregation-fraction <fraction>"));
    result.add(new Option(
      "\tPath to a serialized pre-constructed filter to use on the data.\n\t"
        + "The filter may reside locally or in HDFS.", "preconstructed-filter",
      1, "-preconstructed-filter <path to filter>"));

    WekaClassifierMapTask tempClassifierTask = new WekaClassifierMapTask();
    Enumeration<Option> cOpts = tempClassifierTask.listOptions();

    while (cOpts.hasMoreElements()) {
      result.add(cOpts.nextElement());
    }

    result.add(new Option("", "", 0,
      "\nOptions specific to ARFF training header creation:"));

    ArffHeaderHadoopJob tempArffJob = new ArffHeaderHadoopJob();
    Enumeration<Option> arffOpts = tempArffJob.listOptions();
    while (arffOpts.hasMoreElements()) {
      result.add(arffOpts.nextElement());
    }

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    String numIts = Utils.getOption("num-iterations", options);
    if (!DistributedJobConfig.isEmpty(numIts)) {
      setNumIterations(Integer.parseInt(numIts));
    } else {
      setNumIterations(1);
    }

    String modelFileName = Utils.getOption("model-file-name", options);
    if (!DistributedJobConfig.isEmpty(modelFileName)) {
      setModelFileName(modelFileName);
    }

    String className = Utils.getOption("class", options);
    setClassAttribute(className);

    String aggFrac = Utils.getOption("aggregation-fraction", options);
    setMinTrainingFraction(aggFrac);

    String filterPath = Utils.getOption("preconstructed-filter", options);
    setPathToPreconstructedFilter(filterPath);

    setCreateRandomizedDataChunks(Utils.getFlag("randomized-chunks", options));

    String numDataChunks = Utils.getOption("num-chunks", options);
    setNumRandomizedDataChunks(numDataChunks);

    String numInstancesPerChunk =
      Utils.getOption("num-instances-per-chunk", options);
    setNumInstancesPerRandomizedDataChunk(numInstancesPerChunk);

    // copy the options at this point so that we can set
    // general hadoop configuration for the ARFF header
    // job
    String[] optionsCopy = options.clone();

    super.setOptions(options);

    m_randomizeJob.setOptions(optionsCopy.clone());
    // // Set options for the stratify config (if necessary)
    // m_randomizeConfig.setOptions(optionsCopy.clone());

    // options for the ARFF header job
    m_arffHeaderJob.setOptions(optionsCopy);
    String optsToCSVTask = Utils.joinOptions(m_arffHeaderJob.getOptions());
    if (!DistributedJobConfig.isEmpty(optsToCSVTask)) {
      setCSVMapTaskOptions(optsToCSVTask);
    }

    // options to the classifier map task
    WekaClassifierMapTask classifierTemp = new WekaClassifierMapTask();
    classifierTemp.setOptions(options);
    String optsToClassifierTask =
      Utils.joinOptions(classifierTemp.getOptions());
    if (!DistributedJobConfig.isEmpty(optsToClassifierTask)) {
      setClassifierMapTaskOptions(optsToClassifierTask);
    }
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    options.add("-model-file-name");
    options.add(getModelFileName());

    options.add("-num-iterations");
    options.add("" + getNumIterations());

    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      options.add("-class");
      options.add(getClassAttribute());
    }

    if (!DistributedJobConfig.isEmpty(getPathToPreconstructedFilter())) {
      options.add("-preconstructed-filter");
      options.add(getPathToPreconstructedFilter());
    }

    if (!DistributedJobConfig.isEmpty(getMinTrainingFraction())) {
      options.add("-aggregation-fraction");
      options.add(getMinTrainingFraction());
    }

    if (getCreateRandomizedDataChunks()) {
      options.add("-randomized-chunks");

      if (!DistributedJobConfig.isEmpty(getNumRandomizedDataChunks())) {
        options.add("-num-chunks");
        options.add(getNumRandomizedDataChunks());
      }

      if (!DistributedJobConfig
        .isEmpty(getNumInstancesPerRandomizedDataChunk())) {
        options.add("-num-instances-per-chunk");
        options.add(getNumInstancesPerRandomizedDataChunk());
      }
    }

    if (!DistributedJobConfig.isEmpty(getCSVMapTaskOptions())) {
      try {
        String[] csvOpts = Utils.splitOptions(getCSVMapTaskOptions());

        for (String s : csvOpts) {
          options.add(s);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (!DistributedJobConfig.isEmpty(getClassifierMapTaskOptions())) {
      try {
        String[] classifierOpts =
          Utils.splitOptions(getClassifierMapTaskOptions());

        for (String s : classifierOpts) {
          options.add(s);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Get the options for this job only
   * 
   * @return the options for this job only
   */
  public String[] getJobOptionsOnly() {
    List<String> options = new ArrayList<String>();

    options.add("-model-file-name");
    options.add(getModelFileName());

    options.add("-num-iterations");
    options.add("" + getNumIterations());

    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      options.add("-class");
      options.add(getClassAttribute());
    }

    if (!DistributedJobConfig.isEmpty(getPathToPreconstructedFilter())) {
      options.add("-preconstructed-filter");
      options.add(getPathToPreconstructedFilter());
    }

    if (!DistributedJobConfig.isEmpty(getMinTrainingFraction())) {
      options.add("-aggregation-fraction");
      options.add(getMinTrainingFraction());
    }

    if (getCreateRandomizedDataChunks()) {
      options.add("-randomized-chunks");

      if (!DistributedJobConfig.isEmpty(getNumRandomizedDataChunks())) {
        options.add("-num-chunks");
        options.add(getNumRandomizedDataChunks());
      }

      if (!DistributedJobConfig
        .isEmpty(getNumInstancesPerRandomizedDataChunk())) {
        options.add("-num-instances-per-chunk");
        options.add(getNumInstancesPerRandomizedDataChunk());
      }
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Stages the intermediate classifier (when running more than one iteration)
   * in HDFS, ready to be pushed out to the nodes via the distributed cache
   * 
   * @param conf the Configuration for the job
   * @throws IOException if a problem occurs
   */
  protected void stageIntermediateClassifier(Configuration conf)
    throws IOException {

    String fullOutputPath = m_hdfsPathToAggregatedClassifier;
    String modelNameOnly = getModelFileName();

    String stagingPath =
      HDFSUtils.WEKA_TEMP_DISTRIBUTED_CACHE_FILES + modelNameOnly;

    HDFSUtils.moveInHDFS(fullOutputPath, stagingPath,
      m_mrConfig.getHDFSConfig(), m_env);

    HDFSUtils.addFileToDistributedCache(m_mrConfig.getHDFSConfig(), conf,
      stagingPath, m_env);
  }

  /**
   * Handles moving a preconstructed filter into HDFS (if not there already) and
   * then adding it to the distributed cache for the job.
   * 
   * @param conf the Configuration of the job
   * @return just the filename part of the path - this is what will be
   *         accessible from the distributed cache to clients via standard Java
   *         file IO
   * @throws IOException if a problem occurs
   */
  protected String handlePreconstructedFilter(Configuration conf)
    throws IOException {
    String filterPath = environmentSubstitute(getPathToPreconstructedFilter());

    String filenameOnly =
      HDFSUtils.addFileToDistributedCache(m_mrConfig.getHDFSConfig(), conf,
        filterPath, m_env);

    return filenameOnly;
  }

  /**
   * Initializes and runs the phase that creates randomly shuffled data chunks
   * from the input file(s).
   * 
   * @param header the header of the training data
   * @return true if the job is successful
   * @throws DistributedWekaException if a problem occurs
   * @throws IOException if a problem occurs
   */
  protected boolean initializeAndRunRandomizeDataJob(Instances header)
    throws DistributedWekaException, IOException {

    if (!getCreateRandomizedDataChunks()) {
      return true;
    }

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    logMessage("Checking to see if randomize data chunk job is needed...");
    statusMessage("Checking to see if randomize data chunk job is needed...");
    m_randomizeJob.setEnvironment(m_env);
    m_randomizeJob.setLog(getLog());
    m_randomizeJob.setStatusMessagePrefix(m_statusMessagePrefix);

    // make sure the random seed gets in there from the setting in
    // the underlying classifier map task.
    try {
      String[] classifierOpts =
        Utils.splitOptions(getClassifierMapTaskOptions());
      String seedS = Utils.getOption("seed", classifierOpts);
      if (!DistributedJobConfig.isEmpty(seedS)) {
        seedS = environmentSubstitute(seedS);
        m_randomizeJob.setRandomSeed(seedS);
      }
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    m_randomizeJob.setNumRandomizedDataChunks(getNumRandomizedDataChunks());
    m_randomizeJob
      .setNumInstancesPerRandomizedDataChunk(getNumInstancesPerRandomizedDataChunk());
    m_randomizeJob.setClassAttribute(getClassAttribute());

    if (!m_randomizeJob.runJob()) {
      statusMessage("Unable to continue - randomized data chunk job failed!");
      logMessage("Unable to continue - randomized data chunk job failed!");
      return false;
    }

    return true;
  }

  /**
   * Whether to run the Arff header job. Multiple runs of this job will only
   * need the Arff header job to be run once (on the first run).
   */
  protected boolean m_runArffJob = true;

  /**
   * Initialize and run the ARFF header creation job (if necessary).
   * 
   * @return true if the job was successful
   * @throws DistributedWekaException if a problem occurs
   * @throws IOException if a problem occurs
   */
  protected boolean initializeAndRunArffJob() throws DistributedWekaException,
    IOException {

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    // Run the ARFF header job first
    if (m_runArffJob) {
      logMessage("Executing ARFF Job....");
      statusMessage("Excecuting ARFF Job...");
      m_arffHeaderJob.setEnvironment(m_env);
      m_arffHeaderJob.setLog(getLog());
      m_arffHeaderJob.setStatusMessagePrefix(m_statusMessagePrefix);

      if (!m_arffHeaderJob.runJob()) {
        statusMessage("Unable to continue - creating the ARFF header failed!");
        logMessage("Unable to continue - creating the ARFF header failed!");
        return false;
      }
    }

    if (getCreateRandomizedDataChunks()) {
      boolean stratResult =
        initializeAndRunRandomizeDataJob(m_arffHeaderJob.getFinalHeader());

      if (!stratResult) {
        statusMessage("Unable to continue - stratification of input data failed!");
        logMessage("Unable to continue - stratification of input data failed!");
        return false;
      }

      // unset any mapredMaxSplitSize here because we
      // now have the number of maps determined by the number of
      // data chunks generated by the randomization job
      m_mrConfig.setMapredMaxSplitSize("");

      // alter the input path to point to the output
      // directory of the randomize job.
      // String randomizeOutputPath = m_randomizeConfig.getOutputPath();
      String randomizeOutputPath =
        m_randomizeJob.getRandomizedChunkOutputPath();
      m_mrConfig.setInputPaths(randomizeOutputPath);
    }

    String outputPath = m_mrConfig.getOutputPath();
    outputPath += OUTPUT_SUBDIR;
    outputPath = environmentSubstitute(outputPath);
    m_mrConfig.setOutputPath(outputPath);

    // reducer will write the aggregated classifier to here
    outputPath += "/" + environmentSubstitute(getModelFileName());
    m_mrConfig.setUserSuppliedProperty(
      WekaClassifierHadoopReducer.CLASSIFIER_WRITE_PATH, outputPath);
    m_hdfsPathToAggregatedClassifier = outputPath;

    if (!DistributedJobConfig.isEmpty(getMinTrainingFraction())) {
      try {
        Double.parseDouble(environmentSubstitute(getMinTrainingFraction()));
        m_mrConfig.setUserSuppliedProperty(
          WekaClassifierHadoopReducer.MIN_TRAINING_FRACTION,
          environmentSubstitute(getMinTrainingFraction()));
      } catch (NumberFormatException ex) {
        System.err
          .println("Warning: unable to parse aggregation-fraction value: "
            + environmentSubstitute(getMinTrainingFraction()));
      }
    }

    return true;
  }

  /**
   * Perform an iteration of the model building phase
   * 
   * @param iteration the iteration to perform
   * @param stageIntermediateClassifier true if the intermediate classifier from
   *          the last iteration should be pushed out to the nodes via the
   *          distributed cache
   * @param conf the Configuration of the job
   * @return true if the job succeeds
   * @throws IOException if a problem occurs
   * @throws DistributedWekaException if a problem occurs
   */
  protected boolean performIteration(int iteration,
    boolean stageIntermediateClassifier, Configuration conf)
    throws IOException, DistributedWekaException {
    String jobName = environmentSubstitute(getJobName());
    if (jobName.lastIndexOf(" - ") > 0) {
      jobName = jobName.substring(0, jobName.lastIndexOf(" - "));
    }

    // WekaClassifierMapTask tmpMap = new WekaClassifierMapTask();
    // String classifierPlusOptions = "";
    // try {
    // tmpMap.setOptions(Utils.splitOptions(getClassifierMapTaskOptions()));
    // Classifier c = tmpMap.getClassifier();
    // classifierPlusOptions = c.getClass().getName();
    // if (c instanceof OptionHandler) {
    // classifierPlusOptions += " "
    // + Utils.joinOptions(((OptionHandler) c).getOptions());
    // }
    // } catch (Exception ex) {
    // throw new DistributedWekaException(ex);
    // }

    // add the aggregated ARFF header to the distributed cache
    String pathToHeader =
      environmentSubstitute(m_arffHeaderJob.getAggregatedHeaderPath());

    if (conf == null) {
      conf = new Configuration();
    }

    HDFSUtils.addFileToDistributedCache(m_mrConfig.getHDFSConfig(), conf,
      pathToHeader, m_env);
    String fileNameOnly =
      pathToHeader.substring(pathToHeader.lastIndexOf("/") + 1,
        pathToHeader.length());

    List<String> classifierMapOptions = new ArrayList<String>();

    classifierMapOptions.add("-arff-header");
    classifierMapOptions.add(fileNameOnly);

    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      classifierMapOptions.add("-class");
      classifierMapOptions.add(environmentSubstitute(getClassAttribute()));
    }

    if (!DistributedJobConfig.isEmpty(getPathToPreconstructedFilter())) {
      String filterFilenameOnly = handlePreconstructedFilter(conf);

      classifierMapOptions.add("-preconstructed-filter");
      classifierMapOptions.add(filterFilenameOnly);
    }

    if (iteration > 0) {
      classifierMapOptions.add("-continue-training-updateable");

      if (stageIntermediateClassifier) {
        // Add the model from the previous iteration to the
        // distributed cache. Need to first copy it to a staging
        // directory (since it will be in our output directory
        // and will be deleted when we clean output before launching)
        stageIntermediateClassifier(conf);
      }

      classifierMapOptions.add("-model-file-name");
      classifierMapOptions.add(environmentSubstitute(getModelFileName()));
    }

    if (!DistributedJobConfig.isEmpty(getClassifierMapTaskOptions())) {
      try {
        String cmo = environmentSubstitute(getClassifierMapTaskOptions());
        String[] parts = Utils.splitOptions(cmo);
        for (String p : parts) {
          classifierMapOptions.add(p);
        }
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }
    m_mrConfig.setUserSuppliedProperty(
      WekaClassifierHadoopMapper.CLASSIFIER_MAP_TASK_OPTIONS,
      environmentSubstitute(Utils.joinOptions(classifierMapOptions
        .toArray(new String[classifierMapOptions.size()]))));

    setJobName(jobName
      + " - iteration: "
      + (iteration + 1)
      + " "
      + Utils.joinOptions(classifierMapOptions
        .toArray(new String[classifierMapOptions.size()])));

    // Need these for row parsing via open-csv
    m_mrConfig.setUserSuppliedProperty(
      CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS,
      environmentSubstitute(getCSVMapTaskOptions()));

    // install the weka libraries and any user-selected packages
    // to HDFS and add to the distributed cache/classpath for
    // the job
    if (iteration == 0) {
      installWekaLibrariesInHDFS(conf);
    } else {
      addWekaLibrariesToClasspath(conf);
      addWekaPackageLibrariesToClasspath(
        determinePackageJars(getAdditionalWekaPackageNames(m_mrConfig), true),
        conf);
    }

    Job job = null;
    try {
      job = m_mrConfig.configureForHadoop(getJobName(), conf, m_env);
    } catch (ClassNotFoundException e) {
      throw new DistributedWekaException(e);
    }

    cleanOutputDirectory(job);

    statusMessage("Submitting iteration " + (iteration + 1) + " of job: "
      + getJobName());
    logMessage("Submitting iteration " + (iteration + 1) + " of job: "
      + getJobName());

    boolean success = runJob(job);
    if (!success) {
      statusMessage("Weka classifier job failed - check logs on Hadoop");
      logMessage("Weka classifier job failed - check logs on Hadoop");
    }

    return success;
  }

  @Override
  public boolean runJob() throws DistributedWekaException {

    boolean success = true;
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());
      try {
        setJobStatus(JobStatus.RUNNING);

        if (!initializeAndRunArffJob()) {
          return false;
        }

        // complete the configuration of this job (for each iteration)
        for (int i = 0; i < m_numIterations; i++) {
          success = performIteration(i, true, null);

          if (!success) {
            statusMessage("Weka classifier job failed - check logs on Hadoop");
            logMessage("Weka classifier job failed - check logs on Hadoop");
            break;
          }
        }

        setJobStatus(success ? JobStatus.FINISHED : JobStatus.FAILED);
      } catch (Exception ex) {
        setJobStatus(JobStatus.FAILED);
        throw new DistributedWekaException(ex);
      }

      if (success) {
        // grab the final classifier out of HDFS
        Configuration conf = new Configuration();
        m_mrConfig.getHDFSConfig().configureForHadoop(conf, m_env);
        try {
          FileSystem fs = FileSystem.get(conf);
          Path p = new Path(m_hdfsPathToAggregatedClassifier);
          FSDataInputStream di = fs.open(p);
          ObjectInputStream ois = null;
          try {
            ois = new ObjectInputStream(new BufferedInputStream(di));
            Object classifier = ois.readObject();
            ois.close();
            ois = null;

            m_finalClassifier = (Classifier) classifier;
          } finally {
            if (ois != null) {
              ois.close();
            }
          }

        } catch (IOException e) {
          throw new DistributedWekaException(e);
        } catch (ClassNotFoundException e) {
          throw new DistributedWekaException(e);
        }
      }
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    return success;
  }

  @Override
  public Classifier getClassifier() {
    return m_finalClassifier;
  }

  @Override
  public void stopJob() {
    super.stopJob();

    if (m_arffHeaderJob != null) {
      m_arffHeaderJob.stopJob();
    }

    if (m_randomizeJob != null) {
      m_randomizeJob.stopJob();
    }
  }

  @Override
  public Instances getTrainingHeader() {
    if (m_arffHeaderJob != null) {
      Instances result = m_arffHeaderJob.getFinalHeader();
      if (result != null) {
        try {
          return CSVToARFFHeaderReduceTask.stripSummaryAtts(result);
        } catch (DistributedWekaException e) {
          e.printStackTrace();
        }
      }
    }

    return null;
  }

  public static void main(String[] args) {
    WekaClassifierHadoopJob wchj = new WekaClassifierHadoopJob();

    wchj.run(wchj, args);
  }

  @Override
  public void run(Object toRun, String[] args) throws IllegalArgumentException {
    if (!(toRun instanceof WekaClassifierHadoopJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a WekaClassifierHadoopJob!");
    }

    try {
      WekaClassifierHadoopJob wchj = (WekaClassifierHadoopJob) toRun;

      if (Utils.getFlag('h', args)) {
        String help = DistributedJob.makeOptionsStr(wchj);
        System.err.println(help);
        System.exit(1);
      }

      wchj.setOptions(args);
      wchj.runJob();

      System.out.println(wchj.getClassifier());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
