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
 *    KMeansClustererHadoopJob
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.hadoop.AbstractHadoopJobConfig;
import distributed.hadoop.HDFSConfig;
import distributed.hadoop.HDFSUtils;
import distributed.hadoop.MapReduceJobConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import weka.clusterers.CentroidSketch;
import weka.clusterers.ClusterUtils;
import weka.clusterers.Clusterer;
import weka.clusterers.PreconstructedFilteredClusterer;
import weka.clusterers.PreconstructedKMeans;
import weka.clusterers.SimpleKMeans;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.Option;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.KMeansMapTask;
import weka.distributed.KMeansReduceTask;
import weka.filters.Filter;
import weka.gui.beans.ClustererProducer;
import weka.gui.beans.TextProducer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * Hadoop job for building a k-means clusterer usining either random centroid or
 * k-means|| initialization.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class KMeansClustererHadoopJob extends HadoopJob implements
  CommandlineRunnable, TextProducer, ClustererProducer {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -4063045814370310397L;

  /**
   * The subdirectory of the output directory that this job saves its results to
   */
  protected static final String OUTPUT_SUBDIR = "/kmeans";

  /** Name of the output model */
  protected String m_modelName = "outputModel.model";

  /** ARFF job */
  protected ArffHeaderHadoopJob m_arffHeaderJob = new ArffHeaderHadoopJob();

  /** Randomized data chunk job */
  protected RandomizedDataChunkHadoopJob m_randomizeJob =
    new RandomizedDataChunkHadoopJob();

  /** Full path to the final model in HDFS */
  protected String m_hdfsPathToAggregatedClusterer = "";

  /** Number of nodes in the user's Hadoop cluster. Default = 1 */
  protected String m_numNodesAvailable = "1";

  /**
   * True if the job to create randomly shuffled (and stratified) data chunks is
   * to be run before building the model
   */
  protected boolean m_randomize;

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

  /** The final clusterer produced by this job */
  protected Clusterer m_finalClusterer;

  /** Maximum number of iterations to run */
  protected String m_numIterations = "20";

  /** Perform 10 runs of k-means in parallel */
  protected String m_numRuns = "1";

  /** Number of clusters to find */
  protected String m_numClusters = "2";

  /**
   * The random seed to use with data randomization and k-means|| initialization
   */
  protected String m_randomSeed = "1";

  /** Number of iterations for the k-means|| initialization */
  protected String m_kMeansParallelInitSteps = "5";

  /** Close enough to have converged? */
  protected double m_convergenceTolerance = 1e-4;

  /** Options for the randomize/stratify job */
  protected String m_randomizeJobOpts = "";

  /** Options for the k-means map task */
  protected String m_kMeansMapTaskOpts = "";

  /** Options for the ARFF job */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /**
   * Holds priming data for distance function (if k-means|| initialization is
   * run)
   */
  protected Instances m_distanceFunctionPrimingData;

  /** Initialize with the k-means parallel routine? */
  protected boolean m_initializeWithRandomCenters;

  /**
   * Whether to display standard deviations of centroids in textual output of
   * final model
   */
  protected boolean m_displayStdDevs;

  /**
   * Constructor
   */
  public KMeansClustererHadoopJob() {
    super("KMeans clusterer builder job",
      "Build a k-means clusterer with either standard "
        + "initialization or k-means|| initialization");

    m_mrConfig.setMapperClass(KMeansHadoopMapper.class.getName());
    m_mrConfig.setReducerClass(KMeansHadoopReducer.class.getName());
  }

  /**
   * Help information
   * 
   * @return the help information for this job
   */
  public String globalInfo() {
    return "Learns a k-means clustering using either standard random initialization "
      + "or k-means|| initialization";
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String convergenceToleranceTipText() {
    return "Tollerance for convergence";
  }

  /**
   * Set the convergence tolerance
   * 
   * @param tol the convergence tolerance
   */
  public void setConvergenceTolerance(double tol) {
    m_convergenceTolerance = tol;
  }

  /**
   * Get the convergence tolerance
   * 
   * @return the convergence tolerance
   */
  public double getConvergenceTolerance() {
    return m_convergenceTolerance;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String initWithRandomCentroidsTipText() {
    return "Initialize with randomly selected centroids rather than use the "
      + "k-means|| initialization procedure";
  }

  /**
   * Set whether to initialize with randomly selected centroids rather than
   * using the k-means|| initialization procedure.
   * 
   * @param init true if randomly selected initial centroids are to be used
   */
  public void setInitWithRandomCentroids(boolean init) {
    m_initializeWithRandomCenters = init;
  }

  /**
   * Get whether to initialize with randomly selected centroids rather than
   * using the k-means|| initialization procedure.
   * 
   * @return true if randomly selected initial centroids are to be used
   */
  public boolean getInitWithRandomCentroids() {
    return m_initializeWithRandomCenters;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String numNodesInClusterTipText() {
    return "The number of nodes in the Hadoop cluster - "
      + "used when determining the number of reducers to run";
  }

  /**
   * Set the number of nodes in the Hadoop cluster
   * 
   * @param n the number of nodes in the Hadoop cluster
   */
  public void setNumNodesInCluster(String n) {
    m_numNodesAvailable = n;
  }

  /**
   * Get the number of nodes in the Hadoop cluster
   * 
   * @return the number of nodes in the Hadoop cluster
   */
  public String getNumNodesInCluster() {
    return m_numNodesAvailable;
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String randomlyShuffleDataNumChunksTipText() {
    return "The number of randomly shuffled data chunks to create. Use in "
      + "conjunction with createRandomizedDataChunks";
  }

  /**
   * Set the number of randomly shuffled data chunks to create. Use in
   * conjunction with createRandomizedDataChunks.
   * 
   * @param chunks the number of chunks to create.
   */
  public void setRandomlyShuffleDataNumChunks(String chunks) {
    m_numDataChunks = chunks;
  }

  /**
   * Get the number of randomly shuffled data chunks to create. Use in
   * conjunction with createRandomizedDataChunks.
   * 
   * @return the number of chunks to create.
   */
  public String getRandomlyShuffleDataNumChunks() {
    return m_numDataChunks;
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String modelFileNameTipText() {
    return "The name only (not full path) that the model should be saved to in the output directory";
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
   * @return the tip text for this property
   */
  public String randomlyShuffleDataTipText() {
    return "Randomly shuffle the order of the input data";
  }

  /**
   * Set whether to randomly shuffle the order of the instances in the input
   * data before clustering
   * 
   * @param r true if the data should be randomly shuffled
   */
  public void setRandomlyShuffleData(boolean r) {
    m_randomize = r;
  }

  /**
   * Get whether to randomly shuffle the order of the instances in the input
   * data before clustering
   * 
   * @return true if the data should be randomly shuffled
   */
  public boolean getRandomlyShuffleData() {
    return m_randomize;
  }

  /**
   * Tip text for this property.
   * 
   * @return the tip text for this property
   */
  public String numClustersTipText() {
    return "The number of clusters to find";
  }

  /**
   * Set the number of clusters to find
   * 
   * @param numClusters the number of clusters to find
   */
  public void setNumClusters(String numClusters) {
    m_numClusters = numClusters;
  }

  /**
   * Get the number of clusters to find
   * 
   * @return the number of clusters to find
   */
  public String getNumClusters() {
    return m_numClusters;
  }

  /**
   * Tip text for this property.
   * 
   * @return the tip text for this property
   */
  public String numRunsTipText() {
    return "The number of k-means runs to perform in parallel (best run is selected as final model)";
  }

  /**
   * Set the number of k-means runs to perform in parallel
   * 
   * @param numRuns the number of k-means runs to perform in parallel
   */
  public void setNumRuns(String numRuns) {
    m_numRuns = numRuns;
  }

  /**
   * Get the number of k-means runs to perform in parallel
   * 
   * @return the number of k-means runs to perform in parallel
   */
  public String getNumRuns() {
    return m_numRuns;
  }

  /**
   * Tip text for this property.
   * 
   * @return the tip text for this property
   */
  public String numIterationsTipText() {
    return "The maximum number of k-means iterations to perform";
  }

  /**
   * Set the maximum number of k-means iterations to perform
   * 
   * @param numIts the maximum number of iterations to perform
   */
  public void setNumIterations(String numIts) {
    m_numIterations = numIts;
  }

  /**
   * Get the maximum number of k-means iterations to perform
   * 
   * @return the maximum number of iterations to perform
   */
  public String getNumIterations() {
    return m_numIterations;
  }

  /**
   * Tip text for this property.
   * 
   * @return the tip text for this property
   */
  public String randomSeedTipText() {
    return "Seed for random number generation";
  }

  /**
   * Set the seed for random number generation
   * 
   * @param seed the seed for the random number generator
   */
  public void setRandomSeed(String seed) {
    m_randomSeed = seed;
  }

  /**
   * Get the seed for random number generation
   * 
   * @return the seed for the random number generator
   */
  public String getRandomSeed() {
    return m_randomSeed;
  }

  protected void setKMeansMapTaskOpts(String opts) {
    m_kMeansMapTaskOpts = opts;
  }

  protected String getKMeansMapTaskOpts() {
    return m_kMeansMapTaskOpts;
  }

  /**
   * Tip text for this property.
   * 
   * @return the tip text for this property
   */
  public String kMeansParallelInitStepsTipText() {
    return "The number of iterations of the k-means|| initialization routine to perform. "
      + "Only applies if initializeFlow using random centroids has not been turned on.";
  }

  /**
   * Set the number of iterations of the k-means|| initialization routine to
   * perform
   * 
   * @param steps the number of iterations of the k-means|| init routine to
   *          perform
   */
  public void setKMeansParallelInitSteps(String steps) {
    m_kMeansParallelInitSteps = steps;
  }

  /**
   * Get the number of iterations of the k-means|| initialization routine to
   * perform
   * 
   * @return the number of iterations of the k-means|| init routine to perform
   */
  public String getKMeansParallelInitSteps() {
    return m_kMeansParallelInitSteps;
  }

  /**
   * Set the options for the randomize/stratify task
   * 
   * @param opts the options for the randomize task
   */
  public void setRandomizeJobOptions(String opts) {
    m_randomizeJobOpts = opts;
  }

  /**
   * Get the options for the randomize/stratify task
   * 
   * @return the options for the randomize task
   */
  public String getRandomizedJobOptions() {
    return m_randomizeJobOpts;
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
   * Set the options to the header job
   * 
   * @param opts options to the header job
   */
  public void setCSVMapTaskOptions(String opts) {
    m_wekaCsvToArffMapTaskOpts = opts;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String displayCentroidStdDevsTipText() {
    return "Display centroid standard deviations in textual output of model";
  }

  /**
   * Set whether to display the standard deviations of centroids in textual
   * output of the model
   * 
   * @param d true if standard deviations are to be displayed
   */
  public void setDisplayCentroidStdDevs(boolean d) {
    m_displayStdDevs = d;
  }

  /**
   * Get whether to display the standard deviations of centroids in textual
   * output of the model
   * 
   * @return true if standard deviations are to be displayed
   */
  public boolean getDisplayCentroidStdDevs() {
    return m_displayStdDevs;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option(
      "\tCreate data splits with the order of the input instances\n\t"
        + "shuffled randomly. Use in conjuction with -num-chunks.",
      "randomize", 0,
      "-randomize"));
    result.add(new Option(
      "\tNumber of randomized data chunks. Use in conjunction with\n\t"
        + "-randomized-chunks", "num-chunks", 1, "-num-chunks <integer>"));

    result.add(new Option("\tName of output model file. Model will be\n\t"
      + "written to output-path/k-means/model/<model name>", "model-file-name",
      1, "-model-file-name <model-name>"));

    result.add(new Option("\tNumber of clusters to find (default = 2)",
      "num-clusters", 1, "-num-clusters <integer>"));

    result.add(new Option("\tMax number of k-means iterations (default = 20)",
      "num-iterations", 1, "-num-iterations <integer>"));

    result.add(new Option(
      "\tNumber of separately initialized runs of k-means to\n\t"
        + "perform in parallel (default = 1)", "num-runs", 1,
      "-num-runs <integer>"));

    result.add(new Option("\tTolerance for convergence (default = 1e-4)",
      "tolerance", 1, "-tolerance <double>"));

    result.add(new Option(
      "\tInitialize with randomly selected centroids instead\n\t"
        + "of running k-means|| initialization.", "init-random", 0,
      "-init-random"));

    result.add(new Option("\tDisplay std. deviations for centroids", "V", 0,
      "-V"));

    result.add(new Option("\tRandom seed (default 1).", "seed", 1,
      "-seed <integer>"));

    result.add(new Option(
      "\tNumber of nodes available in cluster (default = 1).", "num-nodes", 1,
      "-num-nodes"));

    KMeansMapTask tempMapTask = new KMeansMapTask();
    Enumeration<Option> mapOpts = tempMapTask.listOptions();
    while (mapOpts.hasMoreElements()) {
      result.add(mapOpts.nextElement());
    }

    RandomizedDataChunkHadoopJob tempRJob = new RandomizedDataChunkHadoopJob();
    Enumeration<Option> randOpts = tempRJob.listOptions();
    while (randOpts.hasMoreElements()) {
      result.add(randOpts.nextElement());
    }

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    String modelFileName = Utils.getOption("model-file-name", options);
    if (!DistributedJobConfig.isEmpty(modelFileName)) {
      setModelFileName(modelFileName);
    }

    setRandomlyShuffleData(Utils.getFlag("randomize", options));

    setInitWithRandomCentroids(Utils.getFlag("init-random", options));

    String numDataChunks = Utils.getOption("num-chunks", options);
    setRandomlyShuffleDataNumChunks(numDataChunks);

    String temp = Utils.getOption("num-clusters", options);
    if (!DistributedJobConfig.isEmpty(temp)) {
      setNumClusters(temp);
    }
    temp = Utils.getOption("num-iterations", options);
    if (!DistributedJobConfig.isEmpty(temp)) {
      setNumIterations(temp);
    }
    temp = Utils.getOption("num-runs", options);
    if (!DistributedJobConfig.isEmpty(temp)) {
      setNumRuns(temp);
    }
    temp = Utils.getOption("seed", options);
    if (!DistributedJobConfig.isEmpty(temp)) {
      setRandomSeed(temp);
    }
    temp = Utils.getOption("tolerance", options);
    if (!DistributedJobConfig.isEmpty(temp)) {
      setConvergenceTolerance(Double.parseDouble(temp));
    }

    String numNodes = Utils.getOption("num-nodes", options);
    setNumNodesInCluster(numNodes);
    setDisplayCentroidStdDevs(Utils.getFlag('V', options));

    KMeansMapTask tempKTask = new KMeansMapTask();
    tempKTask.setOptions(options);
    String mapOpts = Utils.joinOptions(tempKTask.getOptions());
    // if (!DistributedJobConfig.isEmpty(mapOpts)) {
    setKMeansMapTaskOpts(mapOpts);
    // }

    String[] optionsCopy = options.clone();

    super.setOptions(options);

    // options for the randomize job
    m_randomizeJob.setOptions(optionsCopy.clone());
    String optsToRandomize =
      Utils.joinOptions(m_randomizeJob.getOptions());
    if (!DistributedJobConfig.isEmpty(optsToRandomize)) {
      setRandomizeJobOptions(optsToRandomize);
    }

    // options for the ARFF header job
    m_arffHeaderJob.setOptions(optionsCopy);
    String optsToCSVTask = Utils.joinOptions(m_arffHeaderJob.getOptions());
    if (!DistributedJobConfig.isEmpty(optsToCSVTask)) {
      setCSVMapTaskOptions(optsToCSVTask);
    }
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

    if (getRandomlyShuffleData()) {
      options.add("-randomize");

      if (!DistributedJobConfig.isEmpty(getRandomlyShuffleDataNumChunks())) {
        options.add("-num-chunks");
        options.add(getRandomlyShuffleDataNumChunks());
      }
    }

    if (getInitWithRandomCentroids()) {
      options.add("-init-random");
    }

    if (getDisplayCentroidStdDevs()) {
      options.add("-V");
    }

    options.add("-num-clusters");
    options.add(getNumClusters());
    options.add("-num-iterations");
    options.add(getNumIterations());
    options.add("-num-runs");
    options.add(getNumRuns());
    options.add("-seed");
    options.add(getRandomSeed());
    options.add("-tolerance");
    options.add("" + getConvergenceTolerance());

    if (!DistributedJobConfig.isEmpty(getNumNodesInCluster())) {
      options.add("-num-nodes");
      options.add(getNumNodesInCluster());
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public String[] getOptions() {

    List<String> options = new ArrayList<String>();
    for (String opt : getJobOptionsOnly()) {
      options.add(opt);
    }

    if (!DistributedJobConfig.isEmpty(getKMeansMapTaskOpts())) {
      try {
        String[] kMeansOpts = Utils.splitOptions(getKMeansMapTaskOpts());
        for (String s : kMeansOpts) {
          options.add(s);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
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

    if (!DistributedJobConfig.isEmpty(getRandomizedJobOptions())) {
      try {
        String[] csvOpts = Utils.splitOptions(getRandomizedJobOptions());

        for (String s : csvOpts) {
          options.add(s);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return options.toArray(new String[options.size()]);
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

    if (!getRandomlyShuffleData()) {
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

    if (!DistributedJobConfig.isEmpty(getRandomSeed())) {
      m_randomizeJob.setRandomSeed(environmentSubstitute(getRandomSeed()));
    }

    m_randomizeJob
      .setNumRandomizedDataChunks(getRandomlyShuffleDataNumChunks());

    // make sure that the class attribute does not get set by default!
    m_randomizeJob.setDontDefaultToLastAttIfClassNotSpecified(true);

    if (!m_randomizeJob.runJob()) {
      statusMessage("Unable to continue - randomized data chunk job failed!");
      logMessage("Unable to continue - randomized data chunk job failed!");
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

    return true;
  }

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

    return true;
  }

  /**
   * Stage intermediate sketches by moving them from the output directory to a
   * staging directory and then add them to the distributed cache ready for the
   * next iteration
   * 
   * @param conf the configuration to use
   * @param outputPath the path in HDFS where the intermediate sketches were
   *          dumped by the previous iteration
   * @param numRuns the number of runs
   * @throws IOException if a problem occurs
   */
  protected void stageIntermediateSketches(Configuration conf,
    String outputPath, int numRuns)
    throws IOException {

    for (int i = 0; i < numRuns; i++) {
      String fullOutputPath =
        outputPath + "/" + KMeansCentroidSketchHadoopMapper.SKETCH_FILE_PREFIX
          + i;
      String stagingPath =
        HDFSUtils.WEKA_TEMP_DISTRIBUTED_CACHE_FILES
          + KMeansCentroidSketchHadoopMapper.SKETCH_FILE_PREFIX + i;
      HDFSUtils.moveInHDFS(fullOutputPath, stagingPath,
        m_mrConfig.getHDFSConfig(), m_env);
      HDFSUtils.addFileToDistributedCache(m_mrConfig.getHDFSConfig(), conf,
        stagingPath, m_env);
    }
  }

  /**
   * Stage the ARFF header and add to the distributed cache
   * 
   * @param pathToHeader path to the header in HDFS
   * @param hdfsConfig the HDFS config to use
   * @param conf the configuration to use
   * @return just the filename part of the ARFF header path
   * @throws IOException if a problem occurs
   */
  protected String stageArffHeader(String pathToHeader, HDFSConfig hdfsConfig,
    Configuration conf)
    throws IOException {
    HDFSUtils.addFileToDistributedCache(hdfsConfig, conf,
      pathToHeader, m_env);
    String fileNameOnly =
      pathToHeader.substring(pathToHeader.lastIndexOf("/") + 1,
        pathToHeader.length());

    return fileNameOnly;
  }

  /**
   * Reads the k-means centroid sketches from the output directory.
   * 
   * @param conf the configuration to use
   * @param outputPath the output path where the centroids were written
   * @param numRuns the number of runs being performed
   * @return an array of centroid sketches
   * @throws IOException if a problem occurs
   */
  protected CentroidSketch[] getSketchesFromHDFS(Configuration conf,
    String outputPath, int numRuns) throws IOException {

    CentroidSketch[] results = new CentroidSketch[numRuns];
    FileSystem fs = FileSystem.get(conf);
    for (int i = 0; i < numRuns; i++) {
      String fullOutputPath =
        outputPath + "/" + KMeansCentroidSketchHadoopMapper.SKETCH_FILE_PREFIX
          + i;
      Path p = new Path(fullOutputPath);
      InputStream is = fs.open(p);
      BufferedInputStream bis = new BufferedInputStream(is);
      ObjectInputStream ois = new ObjectInputStream(bis);

      try {
        CentroidSketch sketchForRun = (CentroidSketch) ois.readObject();
        results[i] = sketchForRun;
      } catch (ClassNotFoundException ex) {
        throw new IOException(ex);
      } finally {
        is.close();
      }
    }

    return results;
  }

  /**
   * Reads the k-means reduce results from the output directory
   * 
   * @param conf the configuration to use
   * @param outputPath the output path where the centroids were written
   * @param numRuns the number of runs being performed
   * @param converged an array indicating which runs have converged already
   *          (thus we don't excpect a result in the output directory for them)
   * @return an array of KMeansReduceTask objects
   * @throws IOException if a problem occurs
   */
  protected KMeansReduceTask[] getKMeansReducesFromHDFS(Configuration conf,
    String outputPath, int numRuns, boolean[] converged) throws IOException {

    KMeansReduceTask[] results = new KMeansReduceTask[numRuns];
    FileSystem fs = FileSystem.get(conf);
    for (int i = 0; i < numRuns; i++) {
      if (!converged[i]) {
        String fullOutputPath =
          outputPath + "/" + KMeansHadoopReducer.KMEANS_REDUCE_FILE_PREFIX
            + i;
        Path p = new Path(fullOutputPath);
        InputStream is = fs.open(p);
        BufferedInputStream bis = new BufferedInputStream(is);
        ObjectInputStream ois = new ObjectInputStream(bis);

        try {
          KMeansReduceTask reduceForRun = (KMeansReduceTask) ois.readObject();
          results[i] = reduceForRun;
        } catch (ClassNotFoundException ex) {
          throw new IOException(ex);
        } finally {
          is.close();
        }
      }
    }

    return results;
  }

  /**
   * Perform one iteration of the k-means algorithm
   * 
   * @param numRuns number of runs of k-means being performed in parallel
   * @param iterationNum the current iteration number
   * @param conf the configuration object to use
   * @param kMeansConfig the MapReduceJobConfig for the k-means iteration
   * @param baseMapTasks an array of KMeansMapTasks to use as the start points
   *          of the iteration
   * @param arffHeaderFileName the name of the arff header to use
   * @param jobName the job name
   * @throws DistributedWekaException if a problem occurs
   * @throws IOException if a problem occurs
   */
  protected void performKMeansIteration(int numRuns, int iterationNum,
    Configuration conf, MapReduceJobConfig kMeansConfig,
    KMeansMapTask[] baseMapTasks, String arffHeaderFileName, String jobName)
    throws DistributedWekaException, IOException {

    HDFSConfig hdfsConfig = kMeansConfig.getHDFSConfig();

    for (int i = 0; i < numRuns; i++) {
      // stage each and add to distributed cache
      HDFSUtils.serializeObjectToDistributedCache(baseMapTasks[i],
        hdfsConfig,
        conf, KMeansHadoopMapper.KMEANS_MAP_FILE_PREFIX + i, m_env);
    }

    kMeansConfig.setMapperClass(KMeansHadoopMapper.class.getName());
    kMeansConfig.setReducerClass(KMeansHadoopReducer.class.getName());
    kMeansConfig.setUserSuppliedProperty(
      KMeansHadoopMapper.KMEANS_MAP_TASK_OPTIONS,
      environmentSubstitute(getKMeansMapTaskOpts()) + " -arff-header "
        + arffHeaderFileName + " -num-runs " + numRuns + " -iteration "
        + iterationNum);
    kMeansConfig.setUserSuppliedProperty(
      KMeansHadoopReducer.KMEANS_WRITE_PATH,
      kMeansConfig.getOutputPath());

    Job job = null;
    try {
      job = kMeansConfig.configureForHadoop(jobName, conf, m_env);
    } catch (ClassNotFoundException e) {
      throw new DistributedWekaException(e);
    }

    cleanOutputDirectory(job);
    statusMessage("Submitting k-means pass: " + (iterationNum + 1));
    logMessage("Submitting k-means pass: " + (iterationNum + 1));

    if (!runJob(job)) {
      throw new DistributedWekaException("k-means iteration: "
        + (iterationNum + 1) + " failed "
        + "- check logs on Hadoop");
    }
  }

  /**
   * Make the final PreconstructedKMeans clusterer to wrap the centroids and
   * stats found during map-reduce.
   * 
   * @param best the best result from the runs of k-means that were performed in
   *          parallel
   * @param preprocess any pre-processing filters applied
   * @param initialStartingPoints the initial starting centroids
   * @param finalNumIterations the final number of iterations performed
   * @return a final clusterer object
   * @throws DistributedWekaException if a problem occurs
   */
  protected Clusterer makeFinalClusterer(KMeansReduceTask best,
    Filter preprocess, Instances initialStartingPoints, int finalNumIterations)
    throws DistributedWekaException {

    Clusterer finalClusterer = null;
    PreconstructedKMeans finalKMeans = new PreconstructedKMeans();
    // global priming data for the distance function (this will be in
    // the transformed space if we're using preprocessing filters)
    Instances globalPrimingData = best.getGlobalDistanceFunctionPrimingData();
    NormalizableDistance dist = new EuclideanDistance();
    dist.setInstances(globalPrimingData);
    finalKMeans.setClusterCentroids(best.getCentroidsForRun());
    finalKMeans.setFinalNumberOfIterations(finalNumIterations + 1);
    if (initialStartingPoints != null) {
      finalKMeans.setInitialStartingPoints(initialStartingPoints);
    }
    try {
      finalKMeans.setDistanceFunction(dist);
      finalKMeans.setClusterStats(best.getAggregatedCentroidSummaries());
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }

    if (!getInitWithRandomCentroids()) {
      finalKMeans.setInitializationMethod(new SelectedTag(
        SimpleKMeans.KMEANS_PLUS_PLUS, SimpleKMeans.TAGS_SELECTION));
    }

    finalKMeans.setDisplayStdDevs(getDisplayCentroidStdDevs());

    finalClusterer = finalKMeans;

    if (preprocess != null) {
      PreconstructedFilteredClusterer fc =
        new PreconstructedFilteredClusterer();
      fc.setFilter(preprocess);
      fc.setClusterer(finalKMeans);
      finalClusterer = fc;
    }

    return finalClusterer;
  }

  /**
   * Perform all k-means iterations
   * 
   * @param numRuns the number of runs of k-means to perform in parallel
   * @param conf the configuration object to use
   * @param startPoints the initial starting centroids
   * @throws DistributedWekaException if a problem occurs
   * @throws IOException if a problem occurs
   */
  protected void runKMeansIterations(int numRuns, Configuration conf,
    List<Instances> startPoints) throws DistributedWekaException, IOException {

    // make a copy of the start points
    List<Instances> initializationPoints = new ArrayList<Instances>();
    for (Instances i : startPoints) {
      initializationPoints.add(i);
    }

    int numIterations = 20;
    String numIterationsS = getNumIterations();
    if (!DistributedJobConfig.isEmpty(numIterationsS)) {
      try {
        numIterations = Integer.parseInt(environmentSubstitute(numIterationsS));
      } catch (NumberFormatException ex) {
        // ignore
      }
    }

    KMeansReduceTask bestResult = null;
    int bestRunNum = -1;
    int finalNumIterations = -1;

    Instances headerWithSummary = m_arffHeaderJob.getFinalHeader();
    // add the aggregated ARFF header to the distributed cache
    String arffHeaderFileName =
      environmentSubstitute(m_arffHeaderJob.getAggregatedHeaderPath());
    arffHeaderFileName =
      stageArffHeader(arffHeaderFileName, m_mrConfig.getHDFSConfig(),
        conf);

    KMeansMapTask[] mapTasks = new KMeansMapTask[numRuns];
    for (int i = 0; i < numRuns; i++) {
      try {
        mapTasks[i] = new KMeansMapTask();
        mapTasks[i].setOptions(Utils
          .splitOptions(environmentSubstitute(getKMeansMapTaskOpts())));

        mapTasks[i].init(headerWithSummary);
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }

      mapTasks[i].setCentroids(startPoints.get(i));
      if (m_distanceFunctionPrimingData != null) {
        mapTasks[i].setDummyDistancePrimingData(m_distanceFunctionPrimingData);
      }
    }

    boolean[] converged = new boolean[numRuns];
    int numConverged = 0;

    for (int i = 0; i < numIterations; i++) {
      String jobName =
        "k-means - iteration "
          + (i + 1)
          + " :"
          + environmentSubstitute(Utils.joinOptions(getJobOptionsOnly()))
          + " " + environmentSubstitute(getKMeansMapTaskOpts())
          + " -arff-header "
          + arffHeaderFileName;

      performKMeansIteration(numRuns, i, conf, m_mrConfig, mapTasks,
        arffHeaderFileName, jobName);

      // fetch reduce results from HDFS and check convergence...
      KMeansReduceTask[] statsForIteration =
        getKMeansReducesFromHDFS(conf, m_mrConfig.getOutputPath(),
          numRuns, converged);

      for (int j = 0; j < numRuns; j++) {
        if (i == 0 && m_distanceFunctionPrimingData == null) {
          // just finished the first iteration, so we'll have global mins/maxes
          // available for the (potentially) filtered training data
          mapTasks[j].setDummyDistancePrimingData(statsForIteration[j]
            .getGlobalDistanceFunctionPrimingData());

          logDebug("Setting dummy distance priming data:\n"
            + statsForIteration[j].getGlobalDistanceFunctionPrimingData());
        }

        if (!converged[j]) {
          Instances newCentersForRun =
            statsForIteration[j].getCentroidsForRun();

          logDebug("Centers for run " + j + " iteration: " + (i + 1) + "\n"
            + newCentersForRun);
          logDebug("Total within cluster error for run " + j + ": "
            + statsForIteration[j].getTotalWithinClustersError());

          if (i < numIterations - 1) {
            // check for convergence - if we dropped a centroid (because it
            // became
            // empty) then we'll check for convergence in the next iteration
            if (newCentersForRun.numInstances() == startPoints.get(j)
              .numInstances()) {
              boolean changed = false;
              double totalDist = 0;
              for (int k = 0; k < newCentersForRun.numInstances(); k++) {
                double dist =
                  mapTasks[j].distance(newCentersForRun.instance(k),
                    startPoints
                      .get(j).instance(k));
                logDebug("Run " + j + " cluster " + k
                  + " convergence distance: "
                  + dist);
                totalDist += dist;

                if (dist > m_convergenceTolerance) {
                  changed = true;
                  if (i < 2) {
                    break;
                  }
                }
              }

              if (!changed) {
                logMessage("Run: " + j + " converged in " + (i + 1)
                  + " iterations. Total within cluster error: "
                  + statsForIteration[j].getTotalWithinClustersError());
                // List<Instances> centroidSummaries =
                // statsForIteration[j].getAggregatedCentroidSummaries();
                // if (true) {
                // for (Instances sum : centroidSummaries) {
                // System.err.println(sum);
                // }
                // }
                converged[j] = true;
                numConverged++;

                if (bestResult == null) {
                  bestResult = statsForIteration[j];
                  bestRunNum = j;
                  finalNumIterations = bestResult.getIterationNumber();
                } else {
                  if (statsForIteration[j].getTotalWithinClustersError() < bestResult
                    .getTotalWithinClustersError()) {
                    bestResult = statsForIteration[j];
                    bestRunNum = j;
                    finalNumIterations = bestResult.getIterationNumber();
                  }
                }
              } else if (i > 2 && bestResult != null) {
                // try to stop slowly converging runs - that will probably
                // never beat the current best - from dragging the job out
                double remainingIts = numIterations - i;

                // TODO should probably keep a running average of the
                // improvement in squared error per run
                double projectedImprovement = remainingIts * totalDist;
                double currentSqErr =
                  statsForIteration[j].getTotalWithinClustersError();
                if ((bestResult.getTotalWithinClustersError() + m_convergenceTolerance) < (currentSqErr - projectedImprovement)) {
                  // doesn't look like this run will catch up to the current
                  // best...
                  logDebug("Aborting run " + j
                    + " as its current within clust. error (" + currentSqErr
                    + ") "
                    + "is unlikely to beat the current best run ("
                    + bestResult.getTotalWithinClustersError() + ") within "
                    + remainingIts + " iterations");
                  converged[j] = true;
                  numConverged++;
                }
              }
            }
          }

          // update start-points with new centers
          startPoints.set(j, newCentersForRun);
          mapTasks[j].setCentroids(newCentersForRun);
        }
      }

      // check for convergence of all *all* runs and break
      if (numConverged == numRuns || i == numIterations - 1) {
        for (int j = 0; j < numRuns; j++) {
          if (statsForIteration[j] != null) {
            if (bestResult == null) {
              bestResult = statsForIteration[j];
              bestRunNum = j;
              finalNumIterations = bestResult.getIterationNumber();
            } else {
              if (statsForIteration[j].getTotalWithinClustersError() < bestResult
                .getTotalWithinClustersError()) {
                bestResult = statsForIteration[j];
                bestRunNum = j;
                finalNumIterations = bestResult.getIterationNumber();
              }
            }
          }
        }
        break;
      }
    }

    m_finalClusterer =
      makeFinalClusterer(bestResult, mapTasks[0].getPreprocessingFilters(),
        initializationPoints.get(bestRunNum), finalNumIterations);

    logMessage(m_finalClusterer.toString());
    writeFinalClustererToHDFS(conf,
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary));
  }

  /**
   * Writes the final clusterer as a serialized model to the output directory in
   * HDFS.
   * 
   * @param conf the configuration object to use
   * @param headerNoSummary the header of the training data
   * @throws IOException if a problem occurs
   */
  protected void writeFinalClustererToHDFS(Configuration conf,
    Instances headerNoSummary)
    throws IOException {
    if (m_finalClusterer != null) {
      statusMessage("Writing k-means model to job output directory...");
      logMessage("Writing k-means model to job output directory");

      String outputDir = m_mrConfig.getOutputPath();
      ObjectOutputStream oos = null;
      try {
        FileSystem fs = FileSystem.get(conf);

        Path p = new Path(outputDir + "/" + getModelFileName());
        FSDataOutputStream dos = fs.create(p);

        oos = new ObjectOutputStream(new BufferedOutputStream(dos));
        oos.writeObject(m_finalClusterer);
      } finally {
        if (oos != null) {
          oos.flush();
          oos.close();
        }
      }

      if (headerNoSummary != null) {
        // now write the header
        statusMessage("Writing ARFF header to job output directory...");
        logMessage("Writing ARFF header to job output directory");
        String p = outputDir + "/"
          + getModelFileName().replace(".model", "").replace(".MODEL", "")
          + "_arffHeader.arff";
        CSVToArffHeaderHadoopReducer.writeHeaderToDestination(headerNoSummary,
          p, conf);
      }
    }
  }

  /**
   * Run the k-means|| initialization job
   * 
   * @param numRuns the number of runs
   * @param numClusters the number of clusters
   * @return a list of Instances objects
   * @throws DistributedWekaException
   * @throws IOException if a problem occurs
   * @throws DistributedWekaException if a problem occurs
   */
  protected List<Instances> initializeWithKMeansParallel(int numRuns,
    int numClusters) throws DistributedWekaException, IOException {

    int numSteps = 2;
    if (!DistributedJobConfig.isEmpty(getKMeansParallelInitSteps())) {
      try {
        numSteps =
          Integer.parseInt(environmentSubstitute(getKMeansParallelInitSteps()));
      } catch (NumberFormatException ex) {
        // don't fuss
      }
    }

    int randomSeed = 1;
    if (!DistributedJobConfig.isEmpty(getRandomSeed())) {
      try {
        randomSeed = Integer.parseInt(environmentSubstitute(getRandomSeed()));
      } catch (NumberFormatException ex) {
        // don't fuss
      }
    }

    Instances headerWithSummary = m_arffHeaderJob.getFinalHeader();

    // Step 1: start with 1 randomly chosen point for each run
    List<Instances> randomSingleCenters =
      initializeWithRandomCenters(numRuns, 1);

    // Create a single KMeansMap task (just for data filtering purposes)
    KMeansMapTask forFilteringOnly = new KMeansMapTask();

    try {
      forFilteringOnly.setOptions(Utils
        .splitOptions(environmentSubstitute(getKMeansMapTaskOpts())));

      // initialize sketches
      forFilteringOnly.init(headerWithSummary);
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }

    // initial distance function to use with the sketches
    NormalizableDistance distanceFunc = forFilteringOnly.getDistanceFunction();

    // Create iteration 0 CentroidSketches, serialize and place in
    // the distributed cache
    CentroidSketch[] initialSketches = new CentroidSketch[numRuns];
    for (int i = 0; i < numRuns; i++) {
      try {
        Instances transformedStartSketch = randomSingleCenters.get(i);
        // forFilteringOnly.applyFilters(randomSingleCenters.get(i));

        initialSketches[i] =
          new CentroidSketch(transformedStartSketch, distanceFunc,
            2 * numClusters, randomSeed + i);
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    // serialize sketches into tmp distributed cache staging location in
    // HDFS
    HDFSConfig hdfsConfig = m_mrConfig.getHDFSConfig();
    Configuration conf = new Configuration();
    try {
      for (int i = 0; i < numRuns; i++) {
        HDFSUtils.serializeObjectToDistributedCache(initialSketches[i],
          hdfsConfig,
          conf, "sketch_run" + i, m_env);
      }
    } catch (IOException e) {
      throw new DistributedWekaException(e);
    }

    MapReduceJobConfig kMeansParallelConfig = new MapReduceJobConfig();

    // set the base connection-based options
    try {
      kMeansParallelConfig.setOptions(getOptions());
    } catch (Exception e1) {
      throw new DistributedWekaException(e1);
    }
    kMeansParallelConfig
      .setMapperClass(KMeansCentroidSketchHadoopMapper.class
        .getName());
    kMeansParallelConfig
      .setReducerClass(KMeansCentroidSketchHadoopReducer.class.getName());

    kMeansParallelConfig.setNumberOfReducers(m_mrConfig.getNumberOfReducers());

    // set the input path in case we are using randomized chunks
    kMeansParallelConfig.setInputPaths(m_mrConfig.getInputPaths());

    // save the sketches into a subdirectory
    kMeansParallelConfig.setOutputPath(m_mrConfig.getOutputPath() + "/sketch");
    kMeansParallelConfig.setUserSuppliedProperty(
      KMeansCentroidSketchHadoopReducer.SKETCH_WRITE_PATH,
      kMeansParallelConfig.getOutputPath());

    String arffHeaderFileName =
      environmentSubstitute(m_arffHeaderJob.getAggregatedHeaderPath());
    arffHeaderFileName =
      stageArffHeader(arffHeaderFileName, kMeansParallelConfig.getHDFSConfig(),
        conf);

    kMeansParallelConfig.setUserSuppliedProperty(
      KMeansCentroidSketchHadoopMapper.CENTROID_SKETCH_MAP_TASK_OPTIONS,
      environmentSubstitute(Utils.joinOptions(getJobOptionsOnly()))
        + " " + environmentSubstitute(getKMeansMapTaskOpts())
        + " -first-iteration -arff-header " + arffHeaderFileName);

    kMeansParallelConfig.setUserSuppliedProperty(
      CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS,
      environmentSubstitute(getCSVMapTaskOptions()));

    addWekaLibrariesToClasspath(conf);

    // run the sketch iterations
    Job job = null;
    for (int i = 0; i < numSteps; i++) {
      if (i == 1) {
        // no longer the first iteration
        kMeansParallelConfig.setUserSuppliedProperty(
          KMeansCentroidSketchHadoopMapper.CENTROID_SKETCH_MAP_TASK_OPTIONS,
          environmentSubstitute(Utils.joinOptions(getJobOptionsOnly()))
            + " " + environmentSubstitute(getKMeansMapTaskOpts())
            + " -arff-header "
            + arffHeaderFileName);
      }

      String jobName = "k-means|| initialization job - iteration: "
        + (i + 1)
        + " "
        + kMeansParallelConfig.getUserSuppliedProperty(
          KMeansCentroidSketchHadoopMapper.CENTROID_SKETCH_MAP_TASK_OPTIONS);

      try {
        job = kMeansParallelConfig.configureForHadoop(jobName, conf, m_env);
      } catch (ClassNotFoundException e) {
        throw new DistributedWekaException(e);
      }

      cleanOutputDirectory(job);

      statusMessage("Submitting iteration (" + (i + 1)
        + ") of job: k-means|| initialization");
      logMessage("Submitting iteration (" + (i + 1)
        + ") of job: k-means|| initialization");

      if (!runJob(job)) {
        throw new DistributedWekaException("k-means|| initialization failed "
          + "- check logs on Hadoop");
      }

      if (i < numSteps - 1) {
        // now need to move output sketches to staging ready for next
        // iteration
        statusMessage("Staging intermediate centroid sketches ready for "
          + "iteration " + (i + 2));
        logMessage("Staging intermediate centroid sketches ready for "
          + "iteration " + (i + 2));
        stageIntermediateSketches(conf, kMeansParallelConfig.getOutputPath(),
          numRuns);
      }
    }

    CentroidSketch[] finalSketches =
      getSketchesFromHDFS(conf, kMeansParallelConfig.getOutputPath(), numRuns);
    Instances globalPrimingData =
      finalSketches[0].getDistanceFunction().getInstances();
    if (globalPrimingData.numInstances() != 2) {
      throw new DistributedWekaException(
        "Was expecting a two instance (global priming data)"
          + " dataset to be set in the distance function in each sketch!");
    }

    // Configure some KMeans map tasks ready to be used to assign training
    // instances to sketch candidate centers
    KMeansMapTask[] onePassMaps = new KMeansMapTask[numRuns];
    for (int i = 0; i < numRuns; i++) {
      try {
        onePassMaps[i] = new KMeansMapTask();
        onePassMaps[i].setOptions(Utils
          .splitOptions(environmentSubstitute(getKMeansMapTaskOpts())));

        onePassMaps[i].init(headerWithSummary);
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }

      onePassMaps[i].setCentroids(finalSketches[i].getCurrentSketch());
      onePassMaps[i].setDummyDistancePrimingData(globalPrimingData);
    }

    String jobName =
      "k-means|| initialization job - computing sketch membership: "
        + kMeansParallelConfig.getUserSuppliedProperty(
          KMeansCentroidSketchHadoopMapper.CENTROID_SKETCH_MAP_TASK_OPTIONS);

    performKMeansIteration(numRuns, 0, conf, kMeansParallelConfig, onePassMaps,
      arffHeaderFileName, jobName);

    // now retrieve the KMeansReduceTasks that hold the clustering stats
    // for the centroid sketch centers and compute the final centers!
    boolean[] converged = new boolean[numRuns];
    KMeansReduceTask[] statsForSketches =
      getKMeansReducesFromHDFS(conf, kMeansParallelConfig.getOutputPath(),
        numRuns, converged);
    List<Instances> finalStartingPointsForRuns =
      ClusterUtils.weightSketchesAndClusterToFinalStartPoints(numRuns,
        numClusters, finalSketches, statsForSketches, getDebug());

    logDebug("Final starting points for run: 0\n"
      + finalStartingPointsForRuns.get(0));

    m_distanceFunctionPrimingData = globalPrimingData;
    logDebug("Distance function priming data:\n"
      + m_distanceFunctionPrimingData);

    return finalStartingPointsForRuns;
  }

  /**
   * If the data has been randomly shuffled into n chunks then this does select
   * randomly chosen centers. If the data hasn't been randomly shuffled then
   * rows are read sequentially from the first data file in the input directory
   * 
   * @param numRuns the number of runs of k-means
   * @param numClusters the number of clusters
   * @return a list of centers (as Instances objects)
   * @throws DistributedWekaException if a problem occurs
   */
  protected List<Instances> initializeWithRandomCenters(int numRuns,
    int numClusters) throws DistributedWekaException {

    String csvConfig = getCSVMapTaskOptions();
    CSVToARFFHeaderMapTask csvTask = new CSVToARFFHeaderMapTask();
    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(m_arffHeaderJob
        .getFinalHeader());
    Configuration conf = new Configuration();
    m_mrConfig.getHDFSConfig().configureForHadoop(conf, m_env);

    List<Instance> candidateList = new ArrayList<Instance>();
    int numRowsToGet = 2 * numRuns * numClusters;
    boolean ok = false;

    try {
      csvTask.setOptions(Utils.splitOptions(csvConfig));
      csvTask.initParserOnly(CSVToARFFHeaderMapTask
        .instanceHeaderToAttributeNameList(headerNoSummary));
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }
    if (getRandomlyShuffleData()) {
      String randomizedOutputPath =
        m_randomizeJob.getRandomizedChunkOutputPath();
      try {
        FileSystem fs = FileSystem.get(conf);
        // FileStatus[] contents = fs.listStatus(new
        // Path(randomizedOutputPath));

        int chunkNum = 0;

        while (!ok) {
          Path chunk =
            new Path(randomizedOutputPath + "/chunk" + chunkNum + "-r-00000");
          if (!fs.exists(chunk)) {
            if (chunkNum == 0) {
              // something bad has happened - there doesn't seem to be any
              // chunk files
              throw new DistributedWekaException(
                "Unable to find any chunk files in the "
                  + "randomize job's output directory: " + randomizedOutputPath);
            }
            break; // run out of chunks
          }
          FSDataInputStream di = fs.open(chunk);
          BufferedReader br = null;
          try {
            br = new BufferedReader(new InputStreamReader(di));

            // get a few more than we need in order to avoid
            // duplicates (hopefully)
            int count = 0;
            String line = null;
            while ((line = br.readLine()) != null && count < numRowsToGet) {
              String[] parsed = csvTask.parseRowOnly(line);
              Instance inst =
                csvTask.makeInstance(headerNoSummary, false, parsed, false);
              candidateList.add(inst);
              count++;
            }

            if (count == numRowsToGet) {
              ok = true;
            } else {
              chunkNum++;
            }
            br.close();
            br = null;
          } catch (Exception ex) {
            throw new DistributedWekaException(ex);
          } finally {
            if (br != null) {
              br.close();
            }
          }
        }
      } catch (IOException ex) {
        throw new DistributedWekaException(ex);
      }
    } else {
      String inS = m_mrConfig.getInputPaths();
      String[] inputPaths = inS.split(",");
      BufferedReader br = null;
      try {
        FileSystem fs = FileSystem.get(conf);
        int count = 0;
        for (String inPath : inputPaths) {
          FileStatus[] contents = fs.listStatus(new Path(inPath));
          for (FileStatus s : contents) {
            String nameOnly = s.getPath().toString();
            nameOnly =
              nameOnly.substring(nameOnly.lastIndexOf("/") + 1,
                nameOnly.length());
            if (!nameOnly.startsWith(".") && !nameOnly.startsWith("_")
              && fs.isFile(s.getPath())) {
              FSDataInputStream di = fs.open(s.getPath());

              br = new BufferedReader(new InputStreamReader(di));
              String line = null;
              while ((line = br.readLine()) != null && count < numRowsToGet) {
                String[] parsed = csvTask.parseRowOnly(line);
                Instance inst =
                  csvTask.makeInstance(headerNoSummary, false, parsed, false);
                candidateList.add(inst);
                count++;
              }

              if (count == numRowsToGet) {
                ok = true;
                break;
              }
              br.close();
              br = null;
            }
          }
        }
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      } finally {
        if (br != null) {
          try {
            br.close();
          } catch (IOException e) {
            throw new DistributedWekaException(e);
          }
        }
      }
    }

    if (candidateList.size() < numRuns * numClusters) {
      throw new DistributedWekaException(
        "Was unable to obtain enough initial start points "
          + "for " + numRuns + " runs with " + numClusters
          + " start points each.");
    }

    // make sure that start points and header have been through any filters
    KMeansMapTask forFilteringOnly = new KMeansMapTask();
    try {
      forFilteringOnly.setOptions(Utils
        .splitOptions(environmentSubstitute(getKMeansMapTaskOpts())));

      // initialize sketches
      forFilteringOnly.init(m_arffHeaderJob.getFinalHeader());

      for (int i = 0; i < candidateList.size(); i++) {
        Instance filtered = forFilteringOnly.applyFilters(candidateList.get(i));
        candidateList.set(i, filtered);
      }

      headerNoSummary = forFilteringOnly.applyFilters(headerNoSummary);
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }

    List<Instances> startPoints =
      KMeansMapTask.assignStartPointsFromList(numRuns, numClusters,
        candidateList, headerNoSummary);

    return startPoints;
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

        if (!initializeAndRunRandomizeDataJob(m_arffHeaderJob.getFinalHeader())) {
          return false;
        }

        String outputPath = m_mrConfig.getOutputPath();
        outputPath += OUTPUT_SUBDIR;
        outputPath = environmentSubstitute(outputPath);
        m_mrConfig.setOutputPath(outputPath);

        // reducer will write the aggregated classifier to here
        outputPath += "/" + environmentSubstitute(getModelFileName());
        m_hdfsPathToAggregatedClusterer = outputPath;

        Configuration conf = new Configuration();

        // Need these for row parsing via open-csv
        m_mrConfig.setUserSuppliedProperty(
          CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS,
          environmentSubstitute(getCSVMapTaskOptions()));

        try {
          installWekaLibrariesInHDFS(conf);
        } catch (IOException ex) {
          setJobStatus(JobStatus.FAILED);
          throw new DistributedWekaException(ex);
        }

        int nRuns = 1;
        String numRuns = getNumRuns();
        if (!DistributedJobConfig.isEmpty(numRuns)) {
          try {
            nRuns = Integer.parseInt(environmentSubstitute(getNumRuns()));
          } catch (NumberFormatException e) {
          }
        }
        int numNodes = 1;
        String nNodes = getNumNodesInCluster();
        if (!DistributedJobConfig.isEmpty(nNodes)) {
          try {
            numNodes = Integer.parseInt(nNodes);
          } catch (NumberFormatException e) {
          }
        }

        String taskMaxKey =
          AbstractHadoopJobConfig.isHadoop2() ? MapReduceJobConfig.HADOOP2_TASKTRACKER_REDUCE_TASKS_MAXIMUM
            : MapReduceJobConfig.HADOOP_TASKTRACKER_REDUCE_TASKS_MAXIMUM;
        String reduceTasksMaxPerNode = conf.get(taskMaxKey);
        int reduceMax = 2;

        // allow our configuration to override the defaults for the cluster
        String userMaxOverride =
          m_mrConfig
            .getUserSuppliedProperty(
              MapReduceJobConfig.HADOOP_TASKTRACKER_REDUCE_TASKS_MAXIMUM);
        if (DistributedJobConfig.isEmpty(userMaxOverride)) {
          // try the Hadoop 2 version
          userMaxOverride =
            m_mrConfig
              .getUserSuppliedProperty(MapReduceJobConfig.HADOOP2_TASKTRACKER_REDUCE_TASKS_MAXIMUM);
        }
        if (!DistributedJobConfig.isEmpty(userMaxOverride)) {
          reduceTasksMaxPerNode = environmentSubstitute(userMaxOverride);
        }

        if (!DistributedJobConfig.isEmpty(reduceTasksMaxPerNode)) {
          reduceMax =
            Integer.parseInt(environmentSubstitute(reduceTasksMaxPerNode));
        }
        int numReducers = Math.min(nRuns, reduceMax * numNodes);
        if (numReducers > 1) {
          logMessage("Setting number of reducers for clustering job to: "
            + numReducers);

          m_mrConfig.setNumberOfReducers("" + numReducers);
        }

        int numClusters = 2;
        if (!DistributedJobConfig.isEmpty(getNumClusters())) {
          String nCl = environmentSubstitute(getNumClusters());
          try {
            numClusters = Integer.parseInt(nCl);
          } catch (NumberFormatException e) {
          }
        }

        // k-means!! initialization for k-means
        if (!m_initializeWithRandomCenters) {
          List<Instances> startPoints =
            initializeWithKMeansParallel(nRuns, numClusters);
          runKMeansIterations(nRuns, conf, startPoints);
        } else {
          // random initialization for k-means
          List<Instances> startPoints =
            initializeWithRandomCenters(nRuns, numClusters);

          logDebug("Randomly selected starting points for run 0\n"
            + startPoints.get(0).toString());
          runKMeansIterations(nRuns, conf, startPoints);
        }

      } catch (IOException e) {
        throw new DistributedWekaException(e);
      }
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    return success;
  }

  @Override
  public Clusterer getClusterer() {
    return m_finalClusterer;
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

  @Override
  public String getText() {
    return m_finalClusterer != null ? m_finalClusterer.toString()
      : "Clusterer not built yet!";
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

  /**
   * Main method for executing this job from the command line
   * 
   * @param args arguments to the job
   */
  public static void main(String[] args) {

    KMeansClustererHadoopJob job = new KMeansClustererHadoopJob();
    job.run(job, args);
  }

  @Override
  public void run(Object toRun, String[] args) {

    if (!(toRun instanceof KMeansClustererHadoopJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a CorrelationMatrixHadoopJob!");
    }

    try {
      KMeansClustererHadoopJob job = (KMeansClustererHadoopJob) toRun;

      if (Utils.getFlag('h', args)) {
        String help = DistributedJob.makeOptionsStr(job);
        System.err.println(help);
        System.exit(1);
      }

      job.setOptions(args);
      job.runJob();

      // if (!DistributedJobConfig.isEmpty(getText())) {
      // System.out.println(getText());
      // }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
