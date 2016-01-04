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
 *    WekaClassifierSparkJob
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Attribute;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.core.WekaException;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.WekaClassifierMapTask;
import weka.distributed.WekaClassifierReduceTask;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;
import weka.gui.beans.ClassifierProducer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Job for building a classifier in Spark.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierSparkJob extends SparkJob implements
  CommandlineRunnable, ClassifierProducer {

  /**
   * The subdirectory of the output directory that this job saves its results to
   */
  protected static final String OUTPUT_SUBDIR = "model";
  /** For serialization */
  private static final long serialVersionUID = 2057009350486703814L;
  /** Default name for the model */
  protected String m_modelName = "outputModel.model";

  /** Path to a pre-constructed filter to use (if any) */
  protected String m_pathToPreconstructedFilter = "";

  /** Class index or name */
  protected String m_classIndex = "";

  /**
   * Number of iterations over the data to perform - {@code > 1} only makes sense for
   * iterative aggregateable classifiers such as SGD
   */
  protected int m_numIterations = 1;

  /** Holds the options to the map task */
  protected String m_wekaClassifierMapTaskOpts = "";

  /** Holds the options to the header task */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /** ARFF job */
  protected ArffHeaderSparkJob m_arffHeaderJob = new ArffHeaderSparkJob();

  /** The full path to the final model (local or HDFS file system) */
  protected String m_pathToAggregatedClassifier = "";

  /** True if the data is to be randomly shuffled and stratified first */
  protected boolean m_randomizeAndStratify;

  /** Options for the randomize/stratify job */
  protected String m_randomizeJobOpts = "";

  /** Randomize and stratify job */
  protected RandomizedDataSparkJob m_randomizeSparkJob =
    new RandomizedDataSparkJob();

  /** The final classifier produced by this job */
  protected Classifier m_finalClassifier;

  /** The header used to build the classifier */
  protected Instances m_classifierHeader;

  public WekaClassifierSparkJob() {
    super("Weka classifier builder job", "Build an aggregated Weka classifier");
  }

  protected static void configureClassifierMapTask(WekaClassifierMapTask task,
    Classifier intermediateClassifier, String options, int iteration,
    PreconstructedFilter preconstructedFilter, int numSplits) throws Exception {

    if (!DistributedJobConfig.isEmpty(options)) {
      task.setOptions(Utils.splitOptions(options));
    }

    if (intermediateClassifier != null && iteration > 0) {
      // continue training
      task.setClassifier(intermediateClassifier);
      task.setContinueTrainingUpdateableClassifier(true);
    }

    if (preconstructedFilter != null) {
      task.addPreconstructedFilterToUse(preconstructedFilter);
    }

  }

  protected static Instance parseInstance(String row,
    CSVToARFFHeaderMapTask rowHelper, Instances headerNoSummary,
    WekaClassifierMapTask classifierTask) throws IOException {

    boolean setStringVals =
      (classifierTask.getClassifier() instanceof UpdateableClassifier && !classifierTask
        .getForceBatchLearningForUpdateableClassifiers());

    return SparkJob.parseInstance(row, rowHelper, headerNoSummary,
      setStringVals);
  }

  /**
   * Helper method for setting the class index in the supplied Instances object
   *
   * @param classNameOrIndex name or index of the class attribute (may be the
   *          special 'first' or 'last' strings)
   * @param data the data to set the class index in
   * @param defaultToLast true if the data should have the last attribute set to
   *          the class if no class name/index is supplied
   * @throws Exception if a problem occurs
   */
  public static void setClassIndex(String classNameOrIndex, Instances data,
    boolean defaultToLast) throws Exception {
    if (!DistributedJobConfig.isEmpty(classNameOrIndex)) {
      // try as a 1-based index first
      try {
        int classIndex = Integer.parseInt(classNameOrIndex);
        classIndex--;
        data.setClassIndex(classIndex);
      } catch (NumberFormatException e) {
        // named attribute?
        Attribute classAttribute = data.attribute(classNameOrIndex.trim());
        if (classAttribute != null) {
          data.setClass(classAttribute);
        } else if (classNameOrIndex.toLowerCase().equals("first")) {
          data.setClassIndex(0);
        } else if (classNameOrIndex.toLowerCase().equals("last")) {
          data.setClassIndex(data.numAttributes() - 1);
        } else {
          throw new Exception("Can't find class attribute: " + classNameOrIndex
            + " in ARFF header!");
        }
      }
    } else if (defaultToLast) {
      // nothing specified? Default to last attribute
      data.setClassIndex(data.numAttributes() - 1);
    }
  }

  /**
   * Utility routine to write a Weka model to a destination path
   *
   * @param model the model to write
   * @param header the header of the training data used to train the model
   * @param outputPath the path to write to
   * @throws IOException if a problem occurs
   */
  public static void writeModelToDestination(Object model, Instances header,
    String outputPath) throws IOException {
    OutputStream os = openFileForWrite(outputPath);
    ObjectOutputStream oos = null;

    try {
      BufferedOutputStream bos = new BufferedOutputStream(os);
      oos = new ObjectOutputStream(bos);

      oos.writeObject(model);
      if (header != null) {
        oos.writeObject(header);
      }
    } finally {
      if (oos != null) {
        oos.flush();
        oos.close();
      }
    }
  }

  public static void main(String[] args) {
    WekaClassifierSparkJob wcsj = new WekaClassifierSparkJob();
    wcsj.run(wcsj, args);
  }

  /**
   * Load a PreconstructedFilter from local or HDFS filesystems
   *
   * @param filterPath the path to the filter
   * @return the loaded filter
   * @throws IOException if an IO problem occurs
   * @throws ClassNotFoundException if the filter class is not found
   * @throws WekaException if the loaded filter is not a PreconstrucedFilter
   */
  protected static PreconstructedFilter loadPreconstructedFilter(
    String filterPath) throws IOException, ClassNotFoundException,
    WekaException {

    InputStream is = SparkJob.openFileForRead(filterPath);
    ObjectInputStream ois = null;
    PreconstructedFilter finalFilter = null;
    try {
      ois = new ObjectInputStream(new BufferedInputStream(is));
      Filter filter = (Filter) ois.readObject();

      if (!(filter instanceof PreconstructedFilter)) {
        throw new WekaException("The filter in the file '" + filterPath
          + "' is " + "not a PreconstructedFilter");
      }
      finalFilter = (PreconstructedFilter) filter;
    } finally {
      if (ois != null) {
        ois.close();
      }
    }
    return finalFilter;
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
      + "written to output-path/model/<model name>", "model-file-name", 1,
      "-model-file-name <model-name>"));
    result.add(new Option(
      "\tNumber of iterations over the data (default = 1).\n\t"
        + "More than 1 iteration only makes sense for classifiers such\n\t"
        + "as SGD and SGDText", "num-iterations", 1, "-num-iterations <num>"));
    result.add(new Option("\tClass index (1-based) or class attribute name "
      + "(default = last attribute).", "class", 1, "-class <index or name>"));
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
      "\nOptions specific to data randomization/stratification:"));
    RandomizedDataSparkJob tempRJob = new RandomizedDataSparkJob();
    Enumeration<Option> randOpts = tempRJob.listOptions();
    while (randOpts.hasMoreElements()) {
      result.add(randOpts.nextElement());
    }

    return result.elements();
  }

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

    if (getRandomizeAndStratify()) {
      options.add("-randomize");
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
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    for (String opt : getJobOptionsOnly()) {
      options.add(opt);
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

    String filterPath = Utils.getOption("preconstructed-filter", options);
    setPathToPreconstructedFilter(filterPath);

    setRandomizeAndStratify(Utils.getFlag("randomize", options));

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

    // options to the classifier map task
    WekaClassifierMapTask classifierTemp = new WekaClassifierMapTask();
    classifierTemp.setOptions(options);
    String optsToClassifierTask =
      Utils.joinOptions(classifierTemp.getOptions());
    if (!DistributedJobConfig.isEmpty(optsToClassifierTask)) {
      setClassifierMapTaskOptions(optsToClassifierTask);
    }
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
   * @return the tip texdt for this property
   */
  public String randomizeAndStratifyTipText() {
    return "Randomly shuffle (and stratify if class is nominal) the input data";
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
   * @return the tip text for this property
   */
  public String numRandomlyShuffledSplitsTipText() {
    return "The number of randomly shuffled splits to create (if randomly shuffling the data)";
  }

  /**
   * Get the number of randomly shuffled splits to make (if randomly shuffling
   * the data)
   *
   * @return the number of randomly shuffled splits to make
   */
  public String getNumRandomlyShuffledSplits() {
    return m_randomizeSparkJob.getNumRandomlyShuffledSplits();
  }

  /**
   * Set the number of randomly shuffled splits to make (if randomly shuffling
   * the data)
   *
   * @param s the number of randomly shuffled splits to make
   */
  public void setNumRandomlyShuffledSplits(String s) {
    m_randomizeSparkJob.setNumRandomlyShuffledSplits(s);
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String writeRandomlyShuffledSplitsToOutput() {
    return "Output splits from the random shuffling process to the file system";
  }

  /**
   * Get whether the randomly shuffled data job should output its splits to the
   * file system
   *
   * @return true if the random shuffle job should output splits to disk
   */
  public boolean getWriteRandomlyShuffledSplitsToOutput() {
    return m_randomizeSparkJob.getWriteSplitsToOutput();
  }

  /**
   * Set whether the randomly shuffled data job should output its splits to the
   * file system
   *
   * @param write true if the random shuffle job should output splits to disk
   */
  public void setWriteRandomlyShuffledSplitsToOutput(boolean write) {
    m_randomizeSparkJob.setWriteSplitsToOutput(write);
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
   * Get the options for the classifier task
   *
   * @return options for the classifier task
   */
  public String getClassifierMapTaskOptions() {
    return m_wekaClassifierMapTaskOpts;
  }

  /**
   * Set the options for the classifier task
   *
   * @param opts options for the classifier task
   */
  public void setClassifierMapTaskOptions(String opts) {
    m_wekaClassifierMapTaskOpts = opts;
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
  public String classAttributeTipText() {
    return "The name or index of the class attribute. 'first' and "
      + "'last' may also be used.";
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
   * Set the name or index of the class attribute ("first" and "last" can also
   * be used)
   *
   * @param c the name or index of the class attribute
   */
  public void setClassAttribute(String c) {
    m_classIndex = c;
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
   * Get the number of iterations (passes over the data) to run in the model
   * building phase. {@code > 1} only makes sense for incremental classifiers such as
   * SGD that converge on a solution.
   *
   * @return the number of iterations to run
   */
  public int getNumIterations() {
    return m_numIterations;
  }

  /**
   * Set the number of iterations (passes over the data) to run in the model
   * building phase. {@code > 1} only makes sense for incremental classifiers such as
   * SGD that converge on a solution.
   *
   * @param i the number of iterations to run
   */
  public void setNumIterations(int i) {
    m_numIterations = i;
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
   * Tip text for this property
   *
   * @return tip text for this property
   */
  public String pathToPreconstructedFilterTipText() {
    return "The path to a (optional) pre-constructed filter to use. The filter "
      + "may reside in HDFS or on the local file system.";
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
   * Set the path to a pre-constructed filter to use to pre-process the data
   * entering each map. This path may be inside or outside of HDFS.
   *
   * @param path the path to a pre-constructed filter to use
   */
  public void setPathToPreconstructedFilter(String path) {
    m_pathToPreconstructedFilter = path;
  }

  protected void buildClassifier(JavaRDD<Instance> dataset,
    final Instances headerNoSummary, PreconstructedFilter preconstructedFilter)
    throws Exception {
    Classifier intermediateClassifier = null;
    logMessage("[Weka classifier spark job] Running classifier job");

    // just use headerNoSummary for class index
    final int classIndex = headerNoSummary.classIndex();
    final int numPartitions = dataset.partitions().size();

    for (int i = 0; i < m_numIterations; i++) {

      final WekaClassifierMapTask classifierTask = new WekaClassifierMapTask();
      configureClassifierMapTask(classifierTask, intermediateClassifier,
        environmentSubstitute(getClassifierMapTaskOptions()), i,
        preconstructedFilter, dataset.partitions().size());

      JavaRDD<Classifier> classifierMap =
        dataset
          .mapPartitions(new FlatMapFunction<Iterator<Instance>, Classifier>() {

            /** For serialization */
            private static final long serialVersionUID = 2454528046593536500L;

            protected Instances m_header;

            /** Holds the trained classifier for this partition */
            protected List<Classifier> m_results = new ArrayList<Classifier>();

            @Override
            public Iterable<Classifier> call(Iterator<Instance> split)
              throws IOException, DistributedWekaException {

              Environment env = new Environment();
              env.addVariable(WekaClassifierMapTask.TOTAL_NUMBER_OF_MAPS, ""
                + numPartitions);
              classifierTask.setEnvironment(env);

              Instance current = split.next();
              if (current == null) {
                throw new IOException("No data in this partition!!");
              }
              m_header = current.dataset();
              m_header.setClassIndex(classIndex);
              classifierTask.setup(m_header);
              classifierTask.processInstance(current);

              int count = 0;
              while (split.hasNext()) {
                current = split.next();
                classifierTask.processInstance(current);
                count++;
              }

              System.out
                .println("Number of rows processed by classifier map task: "
                  + count);

              // finalize
              classifierTask.finalizeTask();
              m_results.add(classifierTask.getClassifier());

              return m_results;
            }
          });

      // reduce!!!
      List<Classifier> classifiers = classifierMap.collect();
      WekaClassifierReduceTask reduceTask = new WekaClassifierReduceTask();
      intermediateClassifier =
        reduceTask.aggregate(classifiers, null,
          classifierTask.getForceVotedEnsembleCreation());

    }

    m_finalClassifier = intermediateClassifier;
    // System.err.println(m_finalClassifier);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    m_currentContext = sparkContext;
    boolean success;
    setJobStatus(JobStatus.RUNNING);

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    JavaRDD<Instance> dataSet = null;
    Instances headerWithSummary = null;
    if (getDataset(TRAINING_DATA) != null) {
      dataSet = (JavaRDD<Instance>) getDataset(TRAINING_DATA).getDataset();
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

      Dataset d = m_arffHeaderJob.getDataset(TRAINING_DATA);
      headerWithSummary = d.getHeaderWithSummary();
      dataSet = d.getDataset();
      setDataset(TRAINING_DATA, d);
      logMessage("Fetching RDD<Instance> dataset from ARFF job: "
        + dataSet.partitions().size() + " partitions.");
    }

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    String classAtt = "";
    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      classAtt = environmentSubstitute(getClassAttribute());
    }
    try {
      setClassIndex(classAtt, headerNoSummary, true);
    } catch (Exception ex) {
      logMessage(ex);
      throw new DistributedWekaException(ex);
    }
    m_classifierHeader = headerNoSummary;

    // TODO - handle loading a pre-constructed filter
    PreconstructedFilter preconstructedFilter = null;
    if (!DistributedJobConfig.isEmpty(getPathToPreconstructedFilter())) {
      String filterPath =
        environmentSubstitute(getPathToPreconstructedFilter());
      try {
        preconstructedFilter = loadPreconstructedFilter(filterPath);
        logMessage("[WekaClassifierSparkJob] Loaded preconstructed filter: "
          + preconstructedFilter.getClass().toString());
      } catch (Exception ex) {
        logMessage(ex);
        throw new DistributedWekaException(ex);
      }
    }

    // Make sure that we save out to a subdirectory of the output
    // directory
    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);

    if (getRandomizeAndStratify() /* && !m_sjConfig.getSerializedInput() */) {
      m_randomizeSparkJob.setEnvironment(m_env);
      m_randomizeSparkJob.setDefaultToLastAttIfClassNotSpecified(true);
      m_randomizeSparkJob.setStatusMessagePrefix(m_statusMessagePrefix);
      m_randomizeSparkJob.setLog(getLog());
      m_randomizeSparkJob.setCachingStrategy(getCachingStrategy());
      m_randomizeSparkJob.setDataset(TRAINING_DATA, new Dataset<Instance>(dataSet,
        headerWithSummary));

      // make sure the random seed gets in there from the setting in the
      // underlying
      // classifier map task
      try {
        String[] classifierOpts =
          Utils.splitOptions(getClassifierMapTaskOptions());
        String seedS = Utils.getOption("seed", classifierOpts);
        if (!DistributedJobConfig.isEmpty(seedS)) {
          seedS = environmentSubstitute(seedS);
          m_randomizeSparkJob.setRandomSeed(seedS);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      if (!m_randomizeSparkJob.runJobWithContext(sparkContext)) {
        statusMessage("Unable to continue - randomization/stratification of input data failed!");
        logMessage("Unable to continue - randomization/stratification of input data failed!");
        return false;
      }

      // training data after execution of randomize job is the randomly shuffled
      // RDD
      Dataset d = m_randomizeSparkJob.getDataset(TRAINING_DATA);
      // dataSet =
      // m_randomizeSparkJob.getRandomizedStratifiedRDD();
      dataSet = (JavaRDD<weka.core.Instance>) d.getDataset();
      headerWithSummary = d.getHeaderWithSummary();
      setDataset(TRAINING_DATA, new Dataset<Instance>(dataSet, headerWithSummary));
    }

    try {
      buildClassifier(dataSet, headerNoSummary, preconstructedFilter);
      // dataset.unpersist();
      // dataset = null;
    } catch (Exception e) {
      logMessage(e);
      throw new DistributedWekaException(e);
    }

    // write the classifier to the output directory
    outputPath +=
      (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
        + getModelFileName();
    writeModelToDestination(m_finalClassifier, headerNoSummary, outputPath);

    setJobStatus(JobStatus.FINISHED);
    return true;
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {
    if (!(toRun instanceof WekaClassifierSparkJob)) {
      throw new IllegalArgumentException(
        "Object to run is not an WekaClassifierSparkJob!");
    }

    try {
      WekaClassifierSparkJob wcsj = (WekaClassifierSparkJob) toRun;
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

  @Override
  public Classifier getClassifier() {
    return m_finalClassifier;
  }

  @Override
  public Instances getTrainingHeader() {
    return m_classifierHeader;
  }
}
