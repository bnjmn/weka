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
 *    CanopyClustererSparkJob
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.storage.StorageLevel;

import weka.clusterers.Canopy;
import weka.clusterers.Clusterer;
import weka.clusterers.FilteredClusterer;
import weka.clusterers.InstanceWithCanopyAssignments;
import weka.core.*;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.CanopyMapTask;
import weka.distributed.CanopyReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.clusterers.CanopyAssigner;
import weka.distributed.clusterers.CanopyBuilder;
import weka.filters.Filter;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;

/**
 * Spark job for training a Canopy clusterer
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CanopyClustererSparkJob extends SparkJob implements
  CommandlineRunnable {

  /**
   * The subdirectory of the output directory that this job saves its results to
   */
  protected static final String OUTPUT_SUBDIR = "canopy";
  /** For serialization */
  private static final long serialVersionUID = 7905426811312121221L;
  /** Default name for the model */
  protected String m_modelName = "outputModel.model";

  protected String m_wekaCsvToArffMapTaskOpts = "";
  protected String m_canopyMapTaskOpts = "";

  protected ArffHeaderSparkJob m_arffHeaderJob = new ArffHeaderSparkJob();

  /** True if the data is to be randomly shuffled and stratified first */
  protected boolean m_randomizeAndStratify;

  /** Options for the randomize/stratify job */
  protected String m_randomizeJobOpts = "";

  /** Randomize and stratify job */
  protected RandomizedDataSparkJob m_randomizeSparkJob =
    new RandomizedDataSparkJob();

  protected String m_aggregationT1 = "" + Canopy.DEFAULT_T1;
  protected String m_aggregationT2 = "-0.5";

  /** Whether to assign the canopies to the training data */
  protected boolean m_assignCanopiesToTrainingData;

  /** This can be either a Canopy clusterer or a FilteredClusterer */
  protected Clusterer m_finalClusterer;

  /** The full path to the final model (local or HDFS file system) */
  protected String m_pathToAggregatedCanopy = "";

  /** Maximum number of clusters to emerge from the reduce phase */
  protected String m_maxNumClustersReducePhase = "2";

  /** Holds the new RDD of instances with canopy assignments */
  protected JavaRDD<InstanceWithCanopyAssignments> m_canopiesAssigned;

  /**
   * Constructor
   */
  public CanopyClustererSparkJob() {
    super("Canopy clusterer builder job", "Build a canopy clusterer");
  }

  public static void main(String[] args) {
    CanopyClustererSparkJob ccsj = new CanopyClustererSparkJob();
    ccsj.run(ccsj, args);
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option(
      "\tCreate data splits with the order of the input instances\n\t"
        + "shuffled randomly. Also stratifies the data if the class\n\t"
        + "is nominal. Works in conjunction with -min-slices; can\n\t"
        + "alternatively use -num-instances-per-slice.", "randomize", 0,
      "-randomize"));

    result.add(new Option("\tName of output model file. Model will be\n\t"
      + "written to output-path/canopy/model/<model name>", "model-file-name",
      1, "-model-file-name <model-name>"));

    CanopyMapTask tempCanopy = new CanopyMapTask();
    Enumeration<Option> canopyOpts = tempCanopy.listOptions();
    while (canopyOpts.hasMoreElements()) {
      result.add(canopyOpts.nextElement());
    }

    result.add(new Option("\tMaximum number of canopies to result from\n\t"
      + "the reduce phase (default = 2)", "-max-reduce-canopies", 1,
      "-max-reduce-canopies <num>"));

    result.add(new Option("\tThe T2 distance to use in the reduce phase. A\n\t"
      + "value < 0 is taken as a positive multiplier for the standard\n\t"
      + "deviation-based T2 heuristic (default = -0.5)", "t2-reduce", 1,
      "-t2-reduce <num>"));

    result.add(new Option("\tThe T1 distance to use in the reduce phase. A\n\t"
      + "value < 0 is taken as a positive multiplier for T2 (default = -1.5)",
      "t1-reduce", 1, "-t1-reduce <num>"));

    result
      .add(new Option(
        "\tAssign canopies to each training instance after clustering is complete",
        "assign-canopies", 0, "-assign-canopies"));

    RandomizedDataSparkJob tempRJob = new RandomizedDataSparkJob();
    Enumeration<Option> randOpts = tempRJob.listOptions();
    while (randOpts.hasMoreElements()) {
      result.add(randOpts.nextElement());
    }

    return result.elements();
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    options.add("-model-file-name");
    options.add(getModelFileName());

    if (getRandomizeAndStratify()) {
      options.add("-randomize");
    }

    options.add("-max-reduce-canopies");
    options.add(getMaxNumCanopiesReducePhase());

    options.add("-t2-reduce");
    options.add(getT2ReducePhase());

    options.add("-t1-reduce");
    options.add(getT1ReducePhase());

    if (getAssignCanopiesToTrainingData()) {
      options.add("-assign-canopies");
    }

    if (!DistributedJobConfig.isEmpty(getCanopyMapTaskOptions())) {
      try {
        String[] canopyOpts = Utils.splitOptions(getCanopyMapTaskOptions());

        for (String s : canopyOpts) {
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

  @Override
  public void setOptions(String[] options) throws Exception {

    String modelFileName = Utils.getOption("model-file-name", options);
    if (!DistributedJobConfig.isEmpty(modelFileName)) {
      setModelFileName(modelFileName);
    }

    setRandomizeAndStratify(Utils.getFlag("randomize", options));

    String temp = Utils.getOption("max-reduce-canopies", options);
    if (!DistributedJobConfig.isEmpty(temp)) {
      setMaxNumCanopiesReducePhase(temp);
    }

    temp = Utils.getOption("t2-reduce", options);
    if (!DistributedJobConfig.isEmpty(temp)) {
      setT2ReducePhase(temp);
    }

    temp = Utils.getOption("t1-reduce", options);
    if (!DistributedJobConfig.isEmpty(temp)) {
      setT1ReducePhase(temp);
    }

    setAssignCanopiesToTrainingData(Utils.getFlag("assign-canopies", options));

    CanopyMapTask tempCanopy = new CanopyMapTask();
    tempCanopy.setOptions(options);
    String canopyOpts = Utils.joinOptions(tempCanopy.getOptions());
    if (!DistributedJobConfig.isEmpty(canopyOpts)) {
      setCanopyMapTaskOptions(canopyOpts);
    }

    // copy the options at this point so that we can set
    // general hadoop configuration for the ARFF header
    // job
    String[] optionsCopy = options.clone();

    super.setOptions(options);

    // Set options for the stratify config (if necessary)
    m_randomizeSparkJob.setOptions(optionsCopy.clone());

    String optsToRandomize =
      Utils.joinOptions(m_randomizeSparkJob.getOptions());
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

  public String getCanopyMapTaskOptions() {
    return m_canopyMapTaskOpts;
  }

  public void setCanopyMapTaskOptions(String opts) {
    m_canopyMapTaskOpts = opts;
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
   * Get whether to randomize (and stratify) the input data or not
   * 
   * @return true if the input data is to be randomized and stratified
   */
  public boolean getRandomizeAndStratify() {
    return m_randomizeAndStratify;
  }

  /**
   * Set whether to randomize (and stratify) the input data or not
   *
   * @param r true if the input data is to be randomized and stratified
   */
  public void setRandomizeAndStratify(boolean r) {
    m_randomizeAndStratify = r;
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
   * Get the name only for the model file
   * 
   * @return the name only (not full path) that the model should be saved to
   */
  public String getModelFileName() {
    return m_modelName;
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
   * Tip text for this property
   *
   * @return tip text for this property
   */
  public String maxNumCanopiesReducePhaseTipText() {
    return "The maximum number of canopies to form in the reduce phase";
  }

  /**
   * Get the maximum number of canopies to form in the reduce phase
   *
   * @return the maximum number of canopies to form in the reduce phase
   */
  public String getMaxNumCanopiesReducePhase() {
    return m_maxNumClustersReducePhase;
  }

  /**
   * Set the maximum number of canopies to form in the reduce phase
   *
   * @param max the maximum number of canopies to form in the reduce phase
   */
  public void setMaxNumCanopiesReducePhase(String max) {
    m_maxNumClustersReducePhase = max;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String t1ReducePhaseTipText() {
    return "The T1 distance to use in the reduce phase. Values < 0 are taken "
      + "as a positive multiplier for the T2 distance";
  }

  /**
   * Get the T1 distance to use in the reduce phase
   *
   * @return the T1 distance to use in the reduce phase
   */
  public String getT1ReducePhase() {
    return m_aggregationT1;
  }

  /**
   * Set the T1 distance to use in the reduce phase
   *
   * @param t1 the T1 distance to use in the reduce phase
   */
  public void setT1ReducePhase(String t1) {
    m_aggregationT1 = t1;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String t2ReducePhaseTipText() {
    return "The T2 distance to use in the reduce phase. Values < 0 are taken "
      + "as a positive multiplier for the standard deviation-based heuristic T2 distance";
  }

  /**
   * Get the T2 distance to use in the reduce phase
   *
   * @return the T2 distance to use in the reduce phase
   */
  public String getT2ReducePhase() {
    return m_aggregationT2;
  }

  /**
   * Set the T2 distance to use in the reduce phase
   *
   * @param t2 the T2 distance to use in the reduce phase
   */
  public void setT2ReducePhase(String t2) {
    m_aggregationT2 = t2;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String assignCanopiesToTrainingDataTipText() {
    return "Assign canopies to each training instance once clustering is complete";
  }

  /**
   * Get whether to assign canopies to the training data
   *
   * @return true if the canopies found are to be assigned to the training data
   *         (thus creating an new RDD)
   */
  public boolean getAssignCanopiesToTrainingData() {
    return m_assignCanopiesToTrainingData;
  }

  /**
   * Set whether to assign canopies to the training data
   *
   * @param assign true if the canopies found are to be assigned to the training
   *          data (thus creating an new RDD)
   */
  public void setAssignCanopiesToTrainingData(boolean assign) {
    m_assignCanopiesToTrainingData = assign;
  }

  protected CanopyMapTask getConfiguredMapTask()
    throws DistributedWekaException {
    CanopyMapTask task = new CanopyMapTask();

    if (!DistributedJobConfig.isEmpty(getCanopyMapTaskOptions())) {
      try {
        System.err.println(getCanopyMapTaskOptions());
        task.setOptions(Utils
          .splitOptions(environmentSubstitute(getCanopyMapTaskOptions())));
      } catch (Exception e) {
        logMessage(e);
        throw new DistributedWekaException(e);
      }
    }

    return task;
  }

  /**
   * The reduce phase of distributed canopy clustering
   *
   * @param canopies the list of individual canopy clusteres learned in the map
   *          phase
   * @return a single canopy cluster that encapsulates the final set of canopies
   */
  protected Clusterer reduceCanopies(List<Clusterer> canopies,
    Instances headerWithSummary) throws DistributedWekaException {

    int numCanopies =
      Integer.parseInt(environmentSubstitute(getMaxNumCanopiesReducePhase()));
    double aggT1 = Double.parseDouble(environmentSubstitute(m_aggregationT1));
    double aggT2 = Double.parseDouble(environmentSubstitute(m_aggregationT2));

    CanopyReduceTask task = new CanopyReduceTask();
    task.setMaxFinalNumCanopies(numCanopies);
    task.setAggregationT1(aggT1);
    task.setAggregationT2(aggT2);

    Clusterer result = task.reduceCanopies(canopies, headerWithSummary);
    System.out.println(result);
    return result;
  }

  /**
   * Assigns canopy membership to the instances in a dataset. Creates a new
   * RDD[InstanceWithCanopyAssignments].
   *
   * @param dataset the dataset to process
   * @param headerNoSummary the header of the data (sans summary attributes)
   * @throws Exception if a problem occurs
   */
  protected void assignCanopiesToDataset(JavaRDD<Instance> dataset,
    final Instances headerNoSummary) throws Exception {

    Canopy canopy =
      (Canopy) ((m_finalClusterer instanceof Canopy) ? m_finalClusterer
        : (Canopy) ((FilteredClusterer) m_finalClusterer).getClusterer());

    Filter preprocess =
      (m_finalClusterer instanceof FilteredClusterer) ? ((FilteredClusterer) m_finalClusterer)
        .getFilter() : null;

    final CanopyAssigner canopyAssigner =
      new CanopyAssigner(headerNoSummary, getCSVMapTaskOptions(), canopy,
        preprocess);

    JavaRDD<InstanceWithCanopyAssignments> canopiesAssignedMapResults =
      dataset
        .mapPartitions(
          new FlatMapFunction<Iterator<Instance>, InstanceWithCanopyAssignments>() {

            /**
             * For serialization
             */
            private static final long serialVersionUID = -1457310019397486011L;
            protected List<InstanceWithCanopyAssignments> m_results =
              new ArrayList<InstanceWithCanopyAssignments>();

            @Override
            public Iterable<InstanceWithCanopyAssignments> call(
              Iterator<Instance> split) throws IOException,
              DistributedWekaException {

              while (split.hasNext()) {
                Instance current = split.next();

                InstanceWithCanopyAssignments holder =
                  canopyAssigner.process(current);

                m_results.add(holder);
              }

              return m_results;
            }
          }, true);

    m_canopiesAssigned =
      canopiesAssignedMapResults
        .persist(getCachingStrategy().getStorageLevel());
  }

  protected void buildClusterer(JavaRDD<Instance> dataset,
    final Instances headerWithSummary) throws Exception {

    final Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    final CanopyMapTask canopyMapTask = getConfiguredMapTask();
    final CanopyBuilder canopyBuilder =
      new CanopyBuilder(headerWithSummary, headerNoSummary, canopyMapTask,
        getCanopyMapTaskOptions());

    // map phase
    JavaRDD<Clusterer> canopyMap =
      dataset
        .mapPartitions(new FlatMapFunction<Iterator<Instance>, Clusterer>() {

          /** for serialization */
          private static final long serialVersionUID = -8219560148988983518L;

          // protected Instances m_header = new Instances(headerNoSummary, 0);
          protected List<Clusterer> m_results = new ArrayList<Clusterer>();

          // protected CSVToARFFHeaderMapTask m_rowHelper;

          @Override
          public Iterable<Clusterer> call(Iterator<Instance> split)
            throws IOException, DistributedWekaException {

            while (split.hasNext()) {
              Instance currentI = split.next();
              canopyBuilder.process(currentI);
            }

            canopyBuilder.finishedInput();
            m_results.add(canopyBuilder.getFinalizedClusterer());

            return m_results;
          }
        });

    // reduce
    List<Clusterer> canopies = canopyMap.collect();
    canopyMap.unpersist();
    canopyMap = null;
    m_finalClusterer = reduceCanopies(canopies, headerWithSummary);

    if (getAssignCanopiesToTrainingData()) {
      assignCanopiesToDataset(dataset, headerNoSummary);
    }

    dataset.unpersist();
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    m_currentContext = sparkContext;
    setJobStatus(JobStatus.RUNNING);
    boolean success = true;

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    JavaRDD<Instance> dataSet = null;
    Instances headerWithSummary = null;
    if (getDataset(TRAINING_DATA) != null) {
      dataSet = ((Dataset<Instance>) getDataset(TRAINING_DATA)).getDataset();
      headerWithSummary = getDataset(TRAINING_DATA).getHeaderWithSummary();
      logMessage("RDD<Instance> dataset provided: "
        + dataSet.partitions().size() + " partitions.");
    }

    if (dataSet == null && headerWithSummary == null) {
      logMessage("Invoking ARFF Job...");
      m_arffHeaderJob.setEnvironment(m_env);
      m_arffHeaderJob.setLog(getLog());
      m_arffHeaderJob.setStatusMessagePrefix(m_statusMessagePrefix);
      m_arffHeaderJob.setCachingStrategy(getCachingStrategy());

      // header job necessary?
      success = m_arffHeaderJob.runJobWithContext(sparkContext);

      if (!success) {
        setJobStatus(JobStatus.FAILED);
        statusMessage("Unable to continue - creating the ARFF header failed!");
        logMessage("Unable to continue - creating the ARFF header failed!");
        return false;
      }

      Dataset<Instance> d = (Dataset<Instance>) m_arffHeaderJob.getDataset(TRAINING_DATA);
      headerWithSummary = d.getHeaderWithSummary();
      dataSet = d.getDataset();
      setDataset(TRAINING_DATA, new Dataset<Instance>(dataSet, headerWithSummary));
      logMessage("Fetching RDD<Instance> dataset from ARFF job: "
        + dataSet.partitions().size() + " partitions.");
    }

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    // Make sure that we save out to a subdirectory of the output
    // directory
    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);
    String outputModel = addSubdirToPath(outputPath, "model");
    String outputCanopyAssignments =
      addSubdirToPath(outputPath, "canopyAssignments");

    // clean the output directory
    SparkJob.deleteDirectory(outputPath);

    // serialized input is assumed to already be randomized...
    if (getRandomizeAndStratify() /* && !getSerializedInput() */) {
      m_randomizeSparkJob.setDefaultToLastAttIfClassNotSpecified(false);
      m_randomizeSparkJob.setEnvironment(m_env);
      m_randomizeSparkJob.setLog(getLog());
      m_randomizeSparkJob.setStatusMessagePrefix(m_statusMessagePrefix);
      m_randomizeSparkJob.setCachingStrategy(getCachingStrategy());
      m_randomizeSparkJob.setDataset(TRAINING_DATA, new Dataset<Instance>(dataSet,
        headerWithSummary));

      if (!m_randomizeSparkJob.runJobWithContext(sparkContext)) {
        statusMessage("Unable to continue - randomization/stratification of input data failed!");
        logMessage("Unable to continue - randomization/stratification of input data failed!");
        return false;
      }

      logMessage("Runing Canopy job...");

      Dataset<Instance> d = (Dataset<Instance>) m_randomizeSparkJob.getDataset(TRAINING_DATA);
      dataSet = d.getDataset();
      headerWithSummary = d.getHeaderWithSummary();
      setDataset(TRAINING_DATA, new Dataset<Instance>(dataSet, headerWithSummary));
    }

    try {
      buildClusterer(dataSet, headerWithSummary);
      // m_dataSet = randomized;
    } catch (Exception e) {
      logMessage(e);
      throw new DistributedWekaException(e);
    }

    outputModel +=
      (outputModel.toLowerCase().startsWith("://") ? "/" : File.separator)
        + getModelFileName();

    if (m_finalClusterer != null) {
      WekaClassifierSparkJob.writeModelToDestination(m_finalClusterer,
        headerNoSummary, outputModel);
    }

    if (m_canopiesAssigned != null) {
      // save as object file
      m_canopiesAssigned.saveAsTextFile(outputCanopyAssignments);
    }

    setJobStatus(JobStatus.FINISHED);
    return true;
  }

  public JavaRDD<InstanceWithCanopyAssignments> getDataWithCanopiesAssigned()
    throws DistributedWekaException {
    if (m_canopiesAssigned == null) {
      throw new DistributedWekaException(
        "No data with canopies assigned is available. Did you turn"
          + "on the option to assign canopies to the data?");
    }
    return m_canopiesAssigned.persist(StorageLevel.MEMORY_AND_DISK());
  }

  protected void writeCanopyAssignments(String outputPath,
    JavaPairRDD<Integer, ?> assignments) throws IOException {
    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(outputPath, pathOnly);

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);
    p = p.makeQualified(fs);

    assignments.saveAsNewAPIHadoopFile(pathOnly[0], NullWritable.class,
      Text.class, SparkJob.NoKeyTextOutputFormat.class, conf);
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {
    if (!(toRun instanceof CanopyClustererSparkJob)) {
      throw new IllegalArgumentException(
        "Object to run is not an CanopyClustererSparkJob!");
    }

    try {
      CanopyClustererSparkJob wcsj = (CanopyClustererSparkJob) toRun;
      if (Utils.getFlag('h', options)) {
        String help = DistributedJob.makeOptionsStr(wcsj);
        System.err.println(help);
        System.exit(1);
      }

      wcsj.setOptions(options);
      wcsj.runJob();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
