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
 *    MLlibClassifierSparkJob
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import weka.classifiers.Classifier;
import weka.classifiers.mllib.MLlibClassifier;
import weka.classifiers.mllib.MLlibDecisionTree;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.WekaPackageClassLoaderManager;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.filters.Filter;
import weka.filters.StreamableFilter;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.ClassifierProducer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import static weka.distributed.spark.WekaClassifierSparkJob.setClassIndex;
import static weka.distributed.spark.WekaClassifierSparkJob.writeModelToDestination;

/**
 * Job for executing a MLLib classifier on a Spark cluster
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class MLlibClassifierSparkJob extends SparkJob implements
  CommandlineRunnable, ClassifierProducer {

  private static final long serialVersionUID = 5478047699354073003L;

  /**
   * The subdirectory of the output directory that this job saves its results to
   */
  protected static final String OUTPUT_SUBDIR = "model";

  protected MLlibClassifier m_classifier = new MLlibDecisionTree();

  /** Path to a pre-constructed filter to use (if any) */
  protected String m_pathToPreconstructedFilter = "";

  /** True to suppress the output of the serialized model to a file */
  protected boolean m_dontWriteClassifierToOutputDir;

  /** Class index or name */
  protected String m_classIndex = "";

  /** Holds the options to the header task */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /** ARFF job */
  protected ArffHeaderSparkJob m_arffHeaderJob = new ArffHeaderSparkJob();

  /** True if the data is to be randomly shuffled and stratified first */
  protected boolean m_randomizeAndStratify;

  /** Options for the randomize/stratify job */
  protected String m_randomizeJobOpts = "";

  /** Default name for the model */
  protected String m_modelName = "outputModel.model";

  /** Randomize and stratify job */
  protected RandomizedDataSparkJob m_randomizeSparkJob =
    new RandomizedDataSparkJob();

  /** The header used to build the classifier */
  protected Instances m_classifierHeader;

  /** Optional preprocessing filters */
  protected List<StreamableFilter> m_preprocessors = new ArrayList<>();

  public MLlibClassifierSparkJob() {
    super("MLLibClassifier", "Builds an MLlibClassifier model");
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option("\tMLlib classifier to build", "W", 1,
      "-W <MLlib classifier and options>"));

    result.add(new Option(
      "\tCreate data splits with the order of the input instances\n\t"
        + "shuffled randomly. Also stratifies the data if the class\n\t"
        + "is nominal. Works in conjunction with -num-splits; can\n\t"
        + "alternatively use -num-instances-per-slice.", "randomize", 0,
      "-randomize"));

    result.add(new Option("\tName of output model file. Model will be\n\t"
      + "written to output-path/model/<model name>", "model-file-name", 1,
      "-model-file-name <model-name>"));

    result.add(new Option("\tClass index (1-based) or class attribute name "
      + "(default = last attribute).", "class", 1, "-class <index or name>"));

    result
      .add(new Option(
        "\tSpecify a filter to pre-process the data "
          + "with.\n\tThe filter must be a StreamableFilter, meaning that the output"
          + "\n\tformat produced by the filter must be able to be determined"
          + "\n\tdirectly from the input data format. This option may be supplied"
          + "\n\tmultiple times in order to apply more than one filter.",
        "filter", 1, "-filter <filter name and options>"));

    result.add(new Option("", "", 0,
      "\nOptions specific to data randomization/stratification:"));
    RandomizedDataSparkJob tempRJob = new RandomizedDataSparkJob();
    Enumeration<Option> randOpts = tempRJob.listOptions();
    while (randOpts.hasMoreElements()) {
      result.add(randOpts.nextElement());
    }

    // TODO list base mllib classifier options

    return result.elements();
  }

  protected String getFilterSpec(StreamableFilter f) {
    return f.getClass().getName()
      + (f instanceof OptionHandler ? " "
        + Utils.joinOptions(((OptionHandler) f).getOptions()) : "");
  }

  public String[] getJobOptionsOnly() {
    List<String> options = new ArrayList<String>();

    options.add("-W");
    String name = getClassifier().getClass().getCanonicalName() + " ";
    String[] opts = ((OptionHandler) getClassifier()).getOptions();
    for (int i = 0; i < opts.length; i++) {
      if (opts[i].equalsIgnoreCase("-spark-opts")) {
        opts[i] = "";
        opts[i + 1] = "";
        break;
      }
    }
    name += Utils.joinOptions(opts);
    options.add(name);

    options.add("-model-file-name");
    options.add(getModelFileName());

    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      options.add("-class");
      options.add(getClassAttribute());
    }

    for (StreamableFilter f : m_preprocessors) {
      options.add("-filter");
      options.add(getFilterSpec(f));
    }

    if (getRandomizeAndStratify()) {
      options.add("-randomize");

      options.addAll(Arrays.asList(m_randomizeSparkJob.getOptions()));
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    for (String opt : getJobOptionsOnly()) {
      options.add(opt);
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    String mllibString = Utils.getOption("W", options);
    if (mllibString.length() > 0) {
      String[] mlopts = Utils.splitOptions(mllibString);
      String mlalg = mlopts[0];
      mlopts[0] = "";
      m_classifier =
        (MLlibClassifier) Utils.forName(MLlibClassifier.class, mlalg, mlopts);
    }

    String modelFileName = Utils.getOption("model-file-name", options);
    if (!DistributedJobConfig.isEmpty(modelFileName)) {
      setModelFileName(modelFileName);
    }

    String className = Utils.getOption("class", options);
    setClassAttribute(className);

    setRandomizeAndStratify(Utils.getFlag("randomize", options));

    // copy the options at this point so that we can set
    // general hadoop configuration for the ARFF header
    // job
    String[] optionsCopy = options.clone();

    super.setOptions(options);

    m_preprocessors.clear();
    while (true) {
      String filterSpec = Utils.getOption("filter", options);
      if (DistributedJobConfig.isEmpty(filterSpec)) {
        break;
      }

      String[] spec = Utils.splitOptions(filterSpec);
      if (spec.length == 0) {
        throw new IllegalArgumentException(
          "Invalid filter specification string: " + filterSpec);
      }
      String filterClass = spec[0];
      spec[0] = "";
      Filter f =
        (Filter) Utils.forName(Filter.class, filterClass, spec);
      if (!(f instanceof StreamableFilter)) {
        throw new IllegalArgumentException("Filter '" + filterClass
          + "' is not a" + " StreamableFilter");
      }

      m_preprocessors.add((StreamableFilter) f);
    }

    // Set options for the stratify config (if necessary)
    m_randomizeSparkJob.setOptions(optionsCopy.clone());

    // options for the ARFF header job
    m_arffHeaderJob.setOptions(optionsCopy);
  }

  public void setPreprocessingFilters(StreamableFilter[] toUse) {
    m_preprocessors.addAll(Arrays.asList(toUse));
  }

  public StreamableFilter[] getPreprocessingFilters() {
    return m_preprocessors
      .toArray(new StreamableFilter[m_preprocessors.size()]);
  }

  public void setClassifier(MLlibClassifier classifier) {
    m_classifier = classifier;
  }

  @Override
  public Classifier getClassifier() {
    return m_classifier;
  }

  /**
   * Set whether to suppress the output of the serialized mode to a file
   *
   * @param d true to suppress the output of the model to the filesystem
   */
  public void setDontWriteClassifierToOutputDir(boolean d) {
    m_dontWriteClassifierToOutputDir = d;
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
  @ProgrammaticProperty
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
  @ProgrammaticProperty
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
  @ProgrammaticProperty
  public void setWriteRandomlyShuffledSplitsToOutput(boolean write) {
    m_randomizeSparkJob.setWriteSplitsToOutput(write);
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

  @Override
  public Instances getTrainingHeader() {
    return m_classifierHeader;
  }

  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {
    m_currentContext = sparkContext;

    setJobStatus(JobStatus.RUNNING);
    boolean success;

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    JavaRDD<Instance> dataSet = null;
    Instances headerWithSummary = null;
    if (getDataset(TRAINING_DATA) != null
      && getDataset(TRAINING_DATA).getRDD() != null) {
      dataSet = (JavaRDD<Instance>) getDataset(TRAINING_DATA).getRDD();
      headerWithSummary = getDataset(TRAINING_DATA).getHeaderWithSummary();
      logMessage("RDD<Instance> dataset provided: "
        + dataSet.partitions().size() + " partitions.");
      logMessage("Current caching strategy: " + getCachingStrategy());
    }

    if (dataSet == null) {
      logMessage("Invoking ARFF Job...");
      m_arffHeaderJob.setEnvironment(m_env);
      m_arffHeaderJob.setLog(getLog());
      m_arffHeaderJob.setStatusMessagePrefix(m_statusMessagePrefix);
      // m_arffHeaderJob.setCachingStrategy(getCachingStrategy());

      // header job necessary?
      success = m_arffHeaderJob.runJobWithContext(sparkContext);
      setCachingStrategy(m_arffHeaderJob.getCachingStrategy());

      if (!success) {
        setJobStatus(JobStatus.FAILED);
        statusMessage("Unable to continue - creating the ARFF header failed!");
        logMessage("Unable to continue - creating the ARFF header failed!");
        return false;
      }

      Dataset d = m_arffHeaderJob.getDataset(TRAINING_DATA);
      headerWithSummary = d.getHeaderWithSummary();
      dataSet = d.getRDD();
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
      headerWithSummary.setClassIndex(headerNoSummary.classIndex());
    } catch (Exception ex) {
      logMessage(ex);
      throw new DistributedWekaException(ex);
    }
    m_classifierHeader = headerNoSummary;

    // Make sure that we save out to a subdirectory of the output
    // directory
    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    if (!m_dontWriteClassifierToOutputDir) {
      outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);
    }

    if (getRandomizeAndStratify() /* && !m_sjConfig.getSerializedInput() */) {
      m_randomizeSparkJob.setEnvironment(m_env);
      m_randomizeSparkJob.setDefaultToLastAttIfClassNotSpecified(true);
      m_randomizeSparkJob.setStatusMessagePrefix(m_statusMessagePrefix);
      m_randomizeSparkJob.setLog(getLog());
      m_randomizeSparkJob.setCachingStrategy(getCachingStrategy());
      m_randomizeSparkJob.setDataset(TRAINING_DATA, new Dataset(dataSet,
        headerWithSummary));

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
      dataSet = (JavaRDD<weka.core.Instance>) d.getRDD();
      headerWithSummary = d.getHeaderWithSummary();
      setDataset(TRAINING_DATA, new Dataset(dataSet, headerWithSummary));
    }

    // Now we can actually build the MLlib model
    m_classifier.buildClassifier(dataSet, headerWithSummary, m_preprocessors,
      getCachingStrategy());

    if (getDebug()) {
      System.out.println(m_classifier.toString());
    }

    // write the classifier to the output directory
    if (!m_dontWriteClassifierToOutputDir) {
      outputPath +=
        (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
          + getModelFileName();
      writeModelToDestination(m_classifier, headerNoSummary, outputPath);
    }

    setJobStatus(JobStatus.FINISHED);

    return true;
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

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {
    if (!(toRun instanceof MLlibClassifierSparkJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a MLLibClassifierSparkJob!");
    }

    try {
      MLlibClassifierSparkJob wcsj = (MLlibClassifierSparkJob) toRun;
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

  public static void main(String[] args) {
    MLlibClassifierSparkJob wcsj = new MLlibClassifierSparkJob();
    wcsj.run(wcsj, args);
  }
}
