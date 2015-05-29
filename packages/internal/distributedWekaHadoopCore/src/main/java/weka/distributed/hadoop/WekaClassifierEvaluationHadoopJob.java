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
 *    WekaClassifierEvaluationHadoopJob
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import weka.gui.beans.InstancesProducer;
import weka.gui.beans.TextProducer;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.hadoop.AbstractHadoopJobConfig;
import distributed.hadoop.HDFSUtils;
import distributed.hadoop.MapReduceJobConfig;

/**
 * Hadoop job for running an evaluation of a classifier or regressor. Invokes up
 * to four separate jobs (passes over the data). 1) Header creation, 2) optional
 * randomly shuffled data chunk creation, 3) model construction and 4) model
 * evaluation. Can perform evaluation on the training data, on a separate test
 * set, or via cross-validation. In the case of the later, models for all folds
 * are created and aggregated in one pass (job) and then evaluated on all folds
 * in a second job.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierEvaluationHadoopJob extends HadoopJob implements
  TextProducer, InstancesProducer, CommandlineRunnable {

  /** For serialization */
  private static final long serialVersionUID = -5625588954027886749L;

  protected WekaClassifierHadoopJob m_classifierJob =
    new WekaClassifierHadoopJob();

  /**
   * Use this to determine the optimal number of reducers for the reduce stage
   * of the fold-based model building stage. Want to use:
   * 
   * min(totalFolds, (numNodes * mapred.tasktracker.reduce.tasks.maximum))
   */
  protected int m_nodesAvailable = 1;

  /** Textual evaluation results if job successful */
  protected String m_textEvalResults;

  /** Instances version of the evaluation results */
  protected Instances m_evalResults;

  /**
   * Path to a separate test set (if not doing cross-validation or test on
   * training)
   */
  protected String m_separateTestSetPath = "";

  /**
   * Fraction of predictions to retain in order to compute auc/auprc.
   * Predictions are not retained if this is unspecified or the fraction is set
   * <= 0
   */
  protected String m_predFrac = "";

  /**
   * Constructor
   */
  public WekaClassifierEvaluationHadoopJob() {
    super("Weka classifier evaluation job", "Evaluates a Weka classifier");

    // replace the default mapper and reducer with the fold-based equivalents
    m_classifierJob.getMapReduceJobConfig().setMapperClass(
      WekaFoldBasedClassifierHadoopMapper.class.getName());
    m_classifierJob.getMapReduceJobConfig().setReducerClass(
      WekaFoldBasedClassifierHadoopReducer.class.getName());

    // mapper and reducer for phase 2 of this job
    m_mrConfig
      .setMapperClass(WekaFoldBasedClassifierEvaluationHadoopMapper.class
        .getName());
    m_mrConfig.setReducerClass(WekaClassifierEvaluationHadoopReducer.class
      .getName());
  }

  public static void main(String[] args) {

    WekaClassifierEvaluationHadoopJob wchej =
      new WekaClassifierEvaluationHadoopJob();
    wchej.run(wchej, args);
  }

  /**
   * Help info for this job
   *
   * @return help info for this job
   */
  public String globalInfo() {
    return "Evaluates a classifier using either the training data, "
      + "a separate test set or a cross-validation.";
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    // the classifier job has all the options we need once the default
    // mapper and reducer have been replaced with the fold-based equivalents
    result.add(new Option("", "", 0,
      "\nNote: the -fold-number option is ignored by this job."));

    result.add(new Option(
      "\tNumber of nodes available in cluster (default = 1).", "num-nodes", 1,
      "-num-nodes"));

    result.add(new Option("", "", 0,
      "\nOptions specific to model building and evaluation:"));

    result
      .add(new Option(
        "\tPath to a separate test set. Set either this or\n\t"
          + "total-folds for a cross-validation (note that settting total-folds\n\t"
          + "to 1 will perform testing on training)", "test-set-path", 1,
        "-test-set-path <path>"));
    result.add(new Option(
      "\tCompute AUC and AUPRC. Note that this requires individual\n\t"
        + "predictions to be retained - specify a fraction of\n\t"
        + "predictions to sample (e.g. 0.5) in order to save resources.",
      "auc", 1, "-auc <fraction of predictions to sample>"));

    WekaClassifierHadoopJob tempClassifierJob = new WekaClassifierHadoopJob();

    Enumeration<Option> cOpts = tempClassifierJob.listOptions();
    while (cOpts.hasMoreElements()) {
      result.add(cOpts.nextElement());
    }

    return result.elements();
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    for (String o : super.getOptions()) {
      options.add(o);
    }

    if (!DistributedJobConfig.isEmpty(getSeparateTestSetPath())) {
      options.add("-test-set-path");
      options.add(getSeparateTestSetPath());
    }

    if (!DistributedJobConfig.isEmpty(getSampleFractionForAUC())) {
      options.add("-auc");
      options.add(getSampleFractionForAUC());
    }

    options.add("-num-nodes");
    options.add("" + getNumNodesInCluster());

    String[] classifierJobOpts = m_classifierJob.getOptions();
    for (String o : classifierJobOpts) {
      options.add(o);
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    String numNodes = Utils.getOption("num-nodes", options);
    if (!DistributedJobConfig.isEmpty(numNodes)) {
      setNumNodesInCluster(Integer.parseInt(numNodes));
    }

    String separateTestSet = Utils.getOption("test-set-path", options);
    setSeparateTestSetPath(separateTestSet);

    String auc = Utils.getOption("auc", options);
    setSampleFractionForAUC(auc);

    String[] optionsCopy = options.clone();

    super.setOptions(options);

    m_classifierJob.setOptions(optionsCopy);
  }

  /**
   * Get the options pertaining to this job only
   *
   * @return the options for this job only
   */
  public String[] getJobOptionsOnly() {
    List<String> options = new ArrayList<String>();

    if (!DistributedJobConfig.isEmpty(getSeparateTestSetPath())) {
      options.add("-test-set-path");
      options.add(getSeparateTestSetPath());
    }

    if (!DistributedJobConfig.isEmpty(getSampleFractionForAUC())) {
      options.add("-auc");
      options.add(getSampleFractionForAUC());
    }

    options.add("-num-nodes");
    options.add("" + getNumNodesInCluster());

    return options.toArray(new String[options.size()]);
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String separateTestSetPathTipText() {
    return "The path (in HDFS) to a separate test set to use";
  }

  /**
   * Get the path in HDFS to the separate test set to use. Either this or the
   * total number of folds should be specified (but not both).
   * 
   * @return the path in HDFS to the separate test set to evaluate on
   */
  public String getSeparateTestSetPath() {
    return m_separateTestSetPath;
  }

  /**
   * Set the path in HDFS to the separate test set to use. Either this or the
   * total number of folds should be specified (but not both).
   *
   * @param path the path in HDFS to the separate test set to evaluate on
   */
  public void setSeparateTestSetPath(String path) {
    m_separateTestSetPath = path;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String sampleFractionForAUCTipText() {
    return "The percentage of all predictions (randomly sampled) to retain for computing AUC "
      + "and AUPRC. If not specified, then these metrics are not computed and "
      + "no predictions are kept. "
      + "Use this option to keep the number of predictions retained under "
      + "control when computing AUC/AUPRC.";
  }

  /**
   * Get the percentage of predictions to retain (via uniform random sampling)
   * for computing AUC and AUPRC. If not specified, then no predictions are
   * retained and these metrics are not computed.
   * 
   * @return the fraction (between 0 and 1) of all predictions to retain for
   *         computing AUC/AUPRC.
   */
  public String getSampleFractionForAUC() {
    return m_predFrac;
  }

  /**
   * Set the percentage of predictions to retain (via uniform random sampling)
   * for computing AUC and AUPRC. If not specified, then no predictions are
   * retained and these metrics are not computed.
   *
   * @param f the fraction (between 0 and 1) of all predictions to retain for
   *          computing AUC/AUPRC.
   */
  public void setSampleFractionForAUC(String f) {
    m_predFrac = f;
  }

  /**
   * Stages classifiers generated by the model building job ready to be
   * distributed to nodes via the distributed cache for the evaluation job
   *
   * @param numFolds the number of folds (models) generated
   * @param outputModelPath the path in HDFS that the models were saved to
   * @param conf the Configuration for the job
   * @throws Exception if a problem occurs
   */
  protected void stageClassifiersForFolds(int numFolds, String outputModelPath,
    Configuration conf) throws Exception {

    // String modelFileName = outputModelPath.substring(
    // outputModelPath.lastIndexOf("/") + 1, outputModelPath.length());
    String modelNameOnly = m_classifierJob.getModelFileName();
    String pathOnly =
      outputModelPath.substring(0, outputModelPath.lastIndexOf("/") + 1);

    for (int i = 0; i < numFolds; i++) {
      String modelNameWithFoldNumber = "" + (i + 1) + "_" + modelNameOnly;
      String outputModelPathWithFoldNumber = pathOnly + modelNameWithFoldNumber;

      String stagingPath =
        HDFSUtils.WEKA_TEMP_DISTRIBUTED_CACHE_FILES + modelNameWithFoldNumber;

      HDFSUtils.moveInHDFS(outputModelPathWithFoldNumber, stagingPath,
        m_classifierJob.m_mrConfig.getHDFSConfig(), m_env);

      HDFSUtils.addFileToDistributedCache(
        m_classifierJob.m_mrConfig.getHDFSConfig(), conf, stagingPath, m_env);
    }
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String numNodesInClusterTipText() {
    return "The number of nodes in the cluster - used for setting the "
      + "number of reducers to use when performing a cross-validation";
  }

  /**
   * Get the number of nodes in the user's Hadoop cluster. Used for setting the
   * number of reducers to use when performing a cross-validation.
   * 
   * @return the number of nodes in the cluster
   */
  public int getNumNodesInCluster() {
    return m_nodesAvailable;
  }

  /**
   * Set the number of nodes in the user's Hadoop cluster. Used for setting the
   * number of reducers to use when performing a cross-validation.
   *
   * @param n the number of nodes in the cluster
   */
  public void setNumNodesInCluster(int n) {
    m_nodesAvailable = n;
  }

  /**
   * Runs the evaluation phase/job
   *
   * @param outputModelPath the path in HDFS that the models were saved to by
   *          the model building phase
   * @return true if the job was successful
   * @throws Exception if a problem occurs
   */
  protected boolean runEvaluationPhase(String outputModelPath) throws Exception {
    String pathToHeader =
      m_classifierJob.m_arffHeaderJob.getAggregatedHeaderPath();

    Configuration conf = new Configuration();

    HDFSUtils.addFileToDistributedCache(m_mrConfig.getHDFSConfig(), conf,
      pathToHeader, m_env);
    String fileNameOnly =
      pathToHeader.substring(pathToHeader.lastIndexOf("/") + 1,
        pathToHeader.length());

    List<String> classifierMapOptions = new ArrayList<String>();
    classifierMapOptions.add("-arff-header");
    classifierMapOptions.add(fileNameOnly);

    if (!DistributedJobConfig.isEmpty(m_classifierJob.getClassAttribute())) {
      classifierMapOptions.add("-class");
      classifierMapOptions.add(environmentSubstitute(m_classifierJob
        .getClassAttribute()));
    }

    String classifierMapTaskOptions =
      m_classifierJob.getClassifierMapTaskOptions();

    classifierMapOptions.add("-model-file-name");
    classifierMapOptions.add(environmentSubstitute(m_classifierJob
      .getModelFileName()));

    if (!DistributedJobConfig.isEmpty(getSeparateTestSetPath())) {
      classifierMapOptions.add("-test-set-path");
      classifierMapOptions.add(environmentSubstitute(getSeparateTestSetPath()));
    }

    if (!DistributedJobConfig.isEmpty(getSampleFractionForAUC())) {
      String aucS = environmentSubstitute(getSampleFractionForAUC());
      double auc = 0;
      try {
        auc = Double.parseDouble(aucS);

        if (auc > 1) {
          auc /= 100.0;
        }
      } catch (NumberFormatException e) {
        throw new Exception("Unable to parse the sampling fraction for AUC: "
          + aucS);
      }

      if (auc > 0) {
        classifierMapOptions.add("-auc");
        classifierMapOptions.add("" + auc);
      }
    }

    if (!DistributedJobConfig.isEmpty(classifierMapTaskOptions)) {
      String[] parts =
        Utils.splitOptions(environmentSubstitute(classifierMapTaskOptions));
      for (String s : parts) {
        classifierMapOptions.add(s);
      }
    }

    String[] mapOpts =
      Utils.splitOptions(environmentSubstitute(classifierMapTaskOptions));
    String numFolds = Utils.getOption("total-folds", mapOpts.clone());
    int totalFolds = 1;
    if (!DistributedJobConfig.isEmpty(numFolds)) {
      totalFolds = Integer.parseInt(numFolds);
    }

    stageClassifiersForFolds(totalFolds, outputModelPath, conf);

    String jobName = environmentSubstitute(getJobName());
    if (DistributedJobConfig.isEmpty(getSeparateTestSetPath())) {
      jobName += " (" + numFolds + " folds ) ";
    } else {
      jobName += " (separate test set) ";
    }
    jobName +=
      Utils.joinOptions(classifierMapOptions
        .toArray(new String[classifierMapOptions.size()]));
    setJobName(jobName);

    String outputPath = m_mrConfig.getOutputPath();
    outputPath += "/eval";
    m_mrConfig.setOutputPath(outputPath);

    if (m_classifierJob.getCreateRandomizedDataChunks()) {
      // grab the input path(s) from the classifier job if
      // the data has been randomized into chunks in a
      // pre-processing step
      m_mrConfig.setInputPaths(m_classifierJob.m_mrConfig.getInputPaths());

      // unset any mapredMaxSplitSize here because we
      // now have the number of maps determined by the number of
      // data chunks generated by the randomization job
      if (DistributedJobConfig.isEmpty(getSeparateTestSetPath())) {
        m_mrConfig.setMapredMaxSplitSize("");
      }
    }

    // separate test set? (overrides any original input paths)
    if (!DistributedJobConfig.isEmpty(getSeparateTestSetPath())) {
      String sep = getSeparateTestSetPath();
      try {
        sep = environmentSubstitute(sep);
      } catch (Exception ex) {
      }

      m_mrConfig.setInputPaths(sep);
    }

    m_mrConfig.setUserSuppliedProperty(
      WekaClassifierHadoopMapper.CLASSIFIER_MAP_TASK_OPTIONS,
      environmentSubstitute(Utils.joinOptions(classifierMapOptions
        .toArray(new String[classifierMapOptions.size()]))));

    // Need these for row parsing via open-csv
    m_mrConfig.setUserSuppliedProperty(
      CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS,
      environmentSubstitute(m_classifierJob.getCSVMapTaskOptions()));

    // no need to install Weka libraries as this would have been done
    // by the classifier job. Just add to the classpath

    addWekaLibrariesToClasspath(conf);
    addWekaPackageLibrariesToClasspath(
      determinePackageJars(getAdditionalWekaPackageNames(m_mrConfig), true),
      conf);

    // Now setup the job
    Job job =
      m_mrConfig.configureForHadoop(environmentSubstitute(getJobName()), conf,
        m_env);

    cleanOutputDirectory(job);

    statusMessage("Submitting fold-based evaluation job: " + numFolds
      + " folds.");
    logMessage("Submitting fold-based evaluation job: " + numFolds + " folds.");

    return runJob(job);
  }

  @Override
  public boolean runJob() throws DistributedWekaException {
    boolean success = true;
    ClassLoader orig = Thread.currentThread().getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());
      if (m_env == null) {
        m_env = Environment.getSystemWide();
      }
      try {
        // number of iterations for multi-pass incremental classifiers
        int numIterations = m_classifierJob.getNumIterations();

        String classifierMapTaskOptions =
          environmentSubstitute(m_classifierJob.getClassifierMapTaskOptions());
        String[] cOpts = Utils.splitOptions(classifierMapTaskOptions);
        int totalFolds = 1;
        String numFolds = Utils.getOption("total-folds", cOpts.clone());
        if (!DistributedJobConfig.isEmpty(numFolds)) {
          totalFolds = Integer.parseInt(numFolds);
        }

        if (totalFolds > 1
          && !DistributedJobConfig.isEmpty(getSeparateTestSetPath())) {
          throw new DistributedWekaException(
            "Total folds is > 1 and a separate test set "
              + "has been specified - can only perform one or the other out "
              + "of a cross-validation or separate test set evaluation");
        }

        // optimal number of reducers for the fold-based classifier
        // building job
        Configuration conf = new Configuration();
        String taskMaxKey =
          AbstractHadoopJobConfig.isHadoop2() ? MapReduceJobConfig.HADOOP2_TASKTRACKER_REDUCE_TASKS_MAXIMUM
            : MapReduceJobConfig.HADOOP_TASKTRACKER_REDUCE_TASKS_MAXIMUM;
        String reduceTasksMaxPerNode = conf.get(taskMaxKey);

        // allow our configuration to override the defaults for the cluster
        String userMaxOverride =
          m_mrConfig
            .getUserSuppliedProperty(MapReduceJobConfig.HADOOP_TASKTRACKER_REDUCE_TASKS_MAXIMUM);
        if (DistributedJobConfig.isEmpty(userMaxOverride)) {
          // try the Hadoop 2 version
          userMaxOverride =
            m_mrConfig
              .getUserSuppliedProperty(MapReduceJobConfig.HADOOP2_TASKTRACKER_REDUCE_TASKS_MAXIMUM);
        }
        if (!DistributedJobConfig.isEmpty(userMaxOverride)) {
          reduceTasksMaxPerNode =
            environmentSubstitute(userMaxOverride);
        }

        int reduceMax = 2;
        if (!DistributedJobConfig.isEmpty(reduceTasksMaxPerNode)) {
          reduceMax =
            Integer.parseInt(environmentSubstitute(reduceTasksMaxPerNode));
        }
        int numReducers =
          Math.min(totalFolds, (reduceMax * getNumNodesInCluster()));
        logMessage("Setting num reducers per node for fold-based model building to: "
          + numReducers);
        m_classifierJob.m_mrConfig.setNumberOfReducers("" + numReducers);
        m_classifierJob.setLog(getLog());
        m_classifierJob.setStatusMessagePrefix(m_statusMessagePrefix);
        m_classifierJob.setEnvironment(m_env);

        setJobStatus(JobStatus.RUNNING);
        if (!m_classifierJob.initializeAndRunArffJob()) {
          setJobStatus(JobStatus.FAILED);
          return false;
        }

        String outputModelPath =
          environmentSubstitute(m_classifierJob.m_mrConfig
            .getUserSuppliedProperty(WekaClassifierHadoopReducer.CLASSIFIER_WRITE_PATH));
        if (DistributedJobConfig.isEmpty(outputModelPath)) {
          throw new Exception("The output model path is not set!");
        }

        for (int i = 0; i < numIterations; i++) {
          conf = new Configuration();
          if (i > 0) {
            stageClassifiersForFolds(totalFolds, outputModelPath, conf);
          }

          m_classifierJob.setEnvironment(m_env);
          if (!m_classifierJob.performIteration(i, false, conf)) {
            success = false;
            statusMessage("Unable to continue - fold-based classifier job failed. "
              + "Check Hadoop logs");
            logMessage("Unable to continue - fold-based classifier job failed. "
              + "Check Hadoop logs");
            break;
          }
        }

        if (!success) {
          setJobStatus(JobStatus.FAILED);
          return false;
        }

        // launch phase 2

        if (!runEvaluationPhase(outputModelPath)) {
          success = false;
          statusMessage("Evaluation phase failed. Check hadoop logs");
          logMessage("Evaluation phase failed. Check hadoop logs");
        }

        setJobStatus(success ? JobStatus.FINISHED : JobStatus.FAILED);
      } catch (Exception ex) {
        setJobStatus(JobStatus.FAILED);
        throw new DistributedWekaException(ex);
      }

      if (success) {
        // grab the results from HDFS
        retrieveResults();
      }
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    return success;
  }

  /**
   * Grabs the textual evaluation results out of HDFS
   *
   * @throws DistributedWekaException if a problem occurs
   */
  protected void retrieveResults() throws DistributedWekaException {
    try {
      Configuration conf = new Configuration();
      m_mrConfig.getHDFSConfig().configureForHadoop(conf, m_env);

      FileSystem fs = FileSystem.get(conf);
      String outputPath = m_mrConfig.getOutputPath() + "/part-r-00000";
      Path p = new Path(outputPath);
      DataInputStream di = fs.open(p);
      BufferedReader br = null;
      try {
        StringBuilder evalResults = new StringBuilder();
        br = new BufferedReader(new InputStreamReader(di));
        String line = "";
        while ((line = br.readLine()) != null) {
          evalResults.append(line).append("\n");
        }

        br.close();
        br = null;
        m_textEvalResults = evalResults.toString();

        // read the instances version of the summary
        outputPath = m_mrConfig.getOutputPath() + "/evaluation.arff";
        p = new Path(outputPath);
        di = fs.open(p);
        br = new BufferedReader(new InputStreamReader(di));
        m_evalResults = new Instances(br);
        br.close();
        br = null;
      } finally {
        if (br != null) {
          br.close();
        }
      }
    } catch (IOException e) {
      throw new DistributedWekaException(e);
    }
  }

  @Override
  public String getText() {
    return m_textEvalResults;
  }

  @Override
  public Instances getInstances() {
    return m_evalResults;
  }

  @Override
  public void run(Object toRun, String[] args) throws IllegalArgumentException {

    if (!(toRun instanceof WekaClassifierEvaluationHadoopJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a WekaClassifierEvaluationHadoopJob!");
    }

    try {
      WekaClassifierEvaluationHadoopJob wchej =
        (WekaClassifierEvaluationHadoopJob) toRun;

      if (Utils.getFlag('h', args)) {
        String help = DistributedJob.makeOptionsStr(wchej);
        System.err.println(help);
        System.exit(1);
      }

      wchej.setOptions(args);
      wchej.runJob();

      System.out.print(wchej.getText());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
