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
 *    MLlibClassifierEvaluationSparkJob
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.storage.StorageLevel;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import weka.classifiers.evaluation.AggregateableEvaluation;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.mllib.MLlibClassifier;
import weka.classifiers.mllib.MLlibDecisionTree;
import weka.core.Attribute;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.core.WekaPackageClassLoaderManager;
import weka.core.converters.CSVSaver;
import weka.core.converters.Saver;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.WekaClassifierEvaluationMapTask;
import weka.distributed.WekaClassifierEvaluationReduceTask;
import weka.filters.Filter;
import weka.filters.StreamableFilter;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.InstancesProducer;
import weka.gui.beans.TextProducer;

/**
 * Spark job for running an evaluation of an MLlib classifier or regressor.
 * Invokes up to four separate jobs (passes over the data). 1) Header creation,
 * 2) optional random shuffling of the data, 3) model construction and 4) model
 * evaluation. Can perform evaluation on the training data, on a separate test
 * set, or via cross-validation. In the case of the later, the folds created by
 * this job are guaranteed to be the same as used by the
 * WekaClassifierEvaluationSparkJob as long as the same number of initial input
 * partitions, number of folds and random seed are used.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class MLlibClassifierEvaluationSparkJob extends SparkJob implements
  CommandlineRunnable, TextProducer, InstancesProducer {

  private static final long serialVersionUID = 6112545908412128486L;

  /** Subdirectory under the output directory for storing evaluation results */
  protected static final String OUTPUT_SUBDIR = "eval";

  /**
   * Optional user-supplied subdirectory of [output_dir]/eval in which to store
   * results
   */
  protected String m_optionalOutputSubDir = "";

  /** The MLlib classifier to use (default = decision tree) */
  protected MLlibClassifier m_classifier = new MLlibDecisionTree();

  /**
   * Fraction of predictions to retain in order to compute auc/auprc.
   * Predictions are not retained if this is unspecified or the fraction is set
   * {@code <= 0}
   */
  protected String m_predFrac = "";

  /** Total number of folds (default = 1; i.e. evaluate on the training data) */
  protected String m_totalFolds = "1";

  /** Random seed for shuffling */
  protected String m_randomSeed = "1";

  /** Class index or name */
  protected String m_classIndex = "";

  /**
   * Spec for a data source to use for obtaining a test set (if not doing
   * cross-validation or test on training). Command line use only
   */
  protected String m_separateTestSetDataSource = "";

  /** Actual test set data source instance (command line use only) */
  protected DataSource m_testSetDS;

  /** Knowledge Flow use only */
  protected boolean m_evalOnTestSetIfPresent;

  /** True if the data is to be randomly shuffled and stratified first */
  protected boolean m_randomizeAndStratify;

  /** ARFF job */
  protected ArffHeaderSparkJob m_arffHeaderJob = new ArffHeaderSparkJob();

  /** Randomize and stratify job */
  protected RandomizedDataSparkJob m_randomizeSparkJob =
    new RandomizedDataSparkJob();

  /** Textual evaluation results if job successful */
  protected String m_textEvalResults;

  /** Instances version of the evaluation results */
  protected Instances m_evalResults;

  /** Optional preprocessing filters */
  protected List<StreamableFilter> m_preprocessors = new ArrayList<>();

  /**
   * Constructor
   */
  public MLlibClassifierEvaluationSparkJob() {
    super("MLlib classifier evaluation job",
      "Evaluates an MLlib classifier using data splits, for a given"
        + " random seed, that are the same "
        + "as those generated by the WekaClassifierEvaluationSparkJob");
  }

  /**
   * Get an optional subdirectory of [output-dir]/eval in which to store results
   *
   * @return an optional subdirectory in the output directory for results
   */
  public String getOutputSubdir() {
    return m_optionalOutputSubDir;
  }

  /**
   * Set an optional subdirectory of [output-dir]/eval in which to store results
   *
   * @param subdir an optional subdirectory in the output directory for results
   */
  @OptionMetadata(displayName = "Output subdirectory",
    description = "An optional subdirectory of <output-dir>/eval in which to "
      + "store the results ", displayOrder = 1)
  public void setOutputSubdir(String subdir) {
    m_optionalOutputSubDir = subdir;
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
  @OptionMetadata(
    displayName = "Sampling fraction for computing AUC",
    description = "The percentage of all predictions (randomly sampled) to retain "
      + "for computing AUC and AUPRC. If not specified, then these metrics are not"
      + "computed and no predictions are kept. Use this option to keep the number "
      + "of predictions retained under control when computing AUC/PRC.",
    displayOrder = 2)
  public
    void setSampleFractionForAUC(String f) {
    m_predFrac = f;
  }

  /**
   * Get the specification of a data source to use for obtaining a separate test
   * set. Either this or the total number of folds should be specified (but not
   * both). Command line mode only.
   *
   * @return the specification of a data source
   */
  public String getSeparateTestSetDataSource() {
    return m_separateTestSetDataSource;
  }

  /**
   * Get the specification of a data source to use for obtaining a separate test
   * set. Either this or the total number of folds should be specified (but not
   * both). Command line mode only.
   *
   * @param spec the name of a data source to use
   */
  @ProgrammaticProperty
  public void setSeparateTestSetDataSource(String spec) {
    m_separateTestSetDataSource = spec;
  }

  /**
   * Set whether to evaluate on a test set (if present in the dataset manager).
   * Knowledge Flow mode only
   *
   * @param evaluateOnTestSetIfPresent true to evaluate on a test set if
   *          available
   */
  @OptionMetadata(displayName = "Evaluate on separate test set if present",
    description = "Evaluate on a separate test dataset (if present) rather "
      + "than perform cross-validation on the training data.", displayOrder = 3)
  public
    void setEvaluateOnTestSetIfPresent(boolean evaluateOnTestSetIfPresent) {
    m_evalOnTestSetIfPresent = evaluateOnTestSetIfPresent;
  }

  /**
   * Get whether to evaluate on a test set (if present in the dataset manager).
   * Knowledge Flow mode only
   *
   * @return true to evaluate on a test set if available
   */
  public boolean getEvaluateOnTestSetIfPresent() {
    return m_evalOnTestSetIfPresent;
  }

  /**
   * Set the total number of folds to use
   *
   * @param totalFolds the total number of folds for the cross-validation
   */
  @OptionMetadata(displayName = "Number of folds", description = "Number of "
    + "folds to use when cross-validating", displayOrder = 4)
  public void setTotalFolds(String totalFolds) {
    m_totalFolds = totalFolds;
  }

  /**
   * Get the total number of folds to use
   *
   * @return the total number of folds for the cross-validation
   */
  public String getTotalFolds() {
    return m_totalFolds;
  }

  /**
   * Set the random seed to use
   *
   * @param seed the random seed to use
   */
  @ProgrammaticProperty
  public void setSeed(String seed) {
    m_randomSeed = seed;
  }

  /**
   * Get the random seed to use
   *
   * @return the random seed to use
   */
  public String getSeed() {
    return m_randomSeed;
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
  @OptionMetadata(displayName = "Class attribute/index", description = "The "
    + "name or 1-based index of the class attribute", displayOrder = 6)
  public void setClassAttribute(String c) {
    m_classIndex = c;
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
   * Set the MLlib classifier/regressor to evaluate
   *
   * @param classifier the classifier ro regressor to evaluate
   */
  @OptionMetadata(displayName = "MLlib classifier/regressor",
    description = "The MLlib classifier or regressor to evaluate",
    displayOrder = 0)
  public void setClassifier(MLlibClassifier classifier) {
    m_classifier = classifier;
  }

  /**
   * Get the MLlib classifier/regressor to evaluate
   *
   * @return the classifier ro regressor to evaluate
   */
  public MLlibClassifier getClassifier() {
    return m_classifier;
  }

  public void setPreprocessingFilters(StreamableFilter[] toUse) {
    m_preprocessors.addAll(Arrays.asList(toUse));
  }

  public StreamableFilter[] getPreprocessingFilters() {
    return m_preprocessors
      .toArray(new StreamableFilter[m_preprocessors.size()]);
  }

  /**
   * Get an enumeration of command-line options
   *
   * @return an enumeration of command-line options
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option("", "", 0,
      "\nOptions specific to model building and evaluation:"));

    result.add(new Option("\tMLlib classifier to build and evaluate", "W", 1,
      "-W <MLlib classifier and options>"));

    result
      .add(new Option(
        "\tSpecify a filter to pre-process the data "
          + "with.\n\tThe filter must be a StreamableFilter, meaning that the output"
          + "\n\tformat produced by the filter must be able to be determined"
          + "\n\tdirectly from the input data format. This option may be supplied"
          + "\n\tmultiple times in order to apply more than one filter.",
        "filter", 1, "-filter <filter name and options>"));

    result.add(new Option(
      "\tSeparate data source for loading a test set. Set either this or\n\t"
        + "folds for a cross-validation (note that setting folds\n\t"
        + "to 1 will perform testing on training)", "test-set-data-source", 1,
      "-test-set-data-source <spec>"));

    result.add(new Option("\tNumber of folds for cross-validation. Set either "
      + "this or -test-set-data-source for a separate test set", "folds", 1,
      "-folds <integer>"));

    result.add(new Option(
      "\tCompute AUC and AUPRC. Note that this requires individual\n\t"
        + "predictions to be retained - specify a fraction of\n\t"
        + "predictions to sample (e.g. 0.5) in order to save resources.",
      "auc", 1, "-auc <fraction of predictions to sample>"));
    result.add(new Option("\tOptional sub-directory of <output-dir>/eval "
      + "in which to store results.", "output-subdir", 1,
      "-output-subdir <directory name>"));

    result.add(new Option("\tClass index (1-based) or class attribute name "
      + "(default = last attribute).", "class", 1, "-class <index or name>"));

    result.add(new Option(
      "\tCreate data splits with the order of the input instances\n\t"
        + "shuffled randomly. Also stratifies the data if the class\n\t"
        + "is nominal. Works in conjunction with -num-splits; can\n\t"
        + "alternatively use -num-instances-per-slice.", "randomize", 0,
      "-randomize"));

    result.add(new Option("", "", 0,
      "\nOptions specific to data randomization/stratification:"));
    RandomizedDataSparkJob tempRJob = new RandomizedDataSparkJob();
    Enumeration<Option> randOpts = tempRJob.listOptions();
    while (randOpts.hasMoreElements()) {
      result.add(randOpts.nextElement());
    }

    return result.elements();
  }

  protected String getFilterSpec(StreamableFilter f) {
    return f.getClass().getName()
      + (f instanceof OptionHandler ? " "
      + Utils.joinOptions(((OptionHandler) f).getOptions()) : "");
  }

  protected String[] getJobOptionsOnly() {
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

    for (StreamableFilter f : m_preprocessors) {
      options.add("-filter");
      options.add(getFilterSpec(f));
    }

    if (!DistributedJobConfig.isEmpty(getSeparateTestSetDataSource())) {
      options.add("-test-set-data-source");
      options.add(getSeparateTestSetDataSource());
    }

    if (!DistributedJobConfig.isEmpty(getSampleFractionForAUC())) {
      options.add("-auc");
      options.add(getSampleFractionForAUC());
    }

    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      options.add("-class");
      options.add(getClassAttribute());
    }

    if (!DistributedJobConfig.isEmpty(getTotalFolds())) {
      options.add("-folds");
      options.add(getTotalFolds());
    }

    if (!DistributedJobConfig.isEmpty(getOutputSubdir())) {
      options.add("-output-subdir");
      options.add(getOutputSubdir());
    }

    if (getEvaluateOnTestSetIfPresent()) {
      options.add("-evaluate-on-test");
    }

    if (getRandomizeAndStratify()) {
      options.add("-randomize");

      options.addAll(Arrays.asList(m_randomizeSparkJob.getOptions()));
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Get an array of the current option settings
   *
   * @return an array of the current option settings
   */
  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    for (String opt : getJobOptionsOnly()) {
      options.add(opt);
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Set command-line options
   *
   * @param options the options and values to set
   * @throws Exception if a problem occurs
   */
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

    String separateTestSet = Utils.getOption("test-set-data-source", options);
    setSeparateTestSetDataSource(separateTestSet);

    String auc = Utils.getOption("auc", options);
    setSampleFractionForAUC(auc);

    String outputSubDir = Utils.getOption("output-subdir", options);
    setOutputSubdir(outputSubDir);
    setEvaluateOnTestSetIfPresent(Utils.getFlag("evaluate-on-test", options));

    String className = Utils.getOption("class", options);
    setClassAttribute(className);

    String foldsS = Utils.getOption("folds", options);
    if (foldsS.length() > 0) {
      setTotalFolds(foldsS);
    }

    setRandomizeAndStratify(Utils.getFlag("randomize", options));

    String seedS = Utils.getOption("seed", options);
    if (seedS.length() > 0) {
      setSeed(seedS);
    }

    // copy the options at this point so that we can set
    // general hadoop configuration for the ARFF header
    // job
    String[] optionsCopy = options.clone();

    super.setOptions(options);

    // Set options for the stratify config (if necessary)
    m_randomizeSparkJob.setOptions(optionsCopy.clone());
    if (seedS.length() > 0) {
      m_randomizeSparkJob.setRandomSeed(seedS);
    }

    // options for the ARFF header job
    m_arffHeaderJob.setOptions(optionsCopy);
  }

  /**
   * Create cross-validation folds
   * 
   * @param dataset the dataset (RDD of instances) to operate on
   * @param headerNoSummary the header of the dataset (without summary
   *          attributes)
   * @param numFolds the number of folds to create
   * @param seed the random seed to use
   * @return an RDD of {@code FoldMaker} objects. These can be used to obtain
   *         partial training and testing folds for the RDD partition that they
   *         were built on
   * @throws DistributedWekaException if a problem occurs
   */
  protected JavaRDD<FoldMaker> createFolds(JavaRDD<Instance> dataset,
    final Instances headerNoSummary, final int numFolds, final int seed)
    throws DistributedWekaException {

    int origNumPartitions = dataset.getNumPartitions();
    JavaRDD<FoldMaker> foldMap =
      dataset
        .mapPartitions(new FlatMapFunction<Iterator<Instance>, FoldMaker>() {
          private static final long serialVersionUID = -2193086909367962482L;

          protected List<FoldMaker> m_partialFoldsForPartition =
            new ArrayList<FoldMaker>();

          @Override
          public Iterable<FoldMaker> call(Iterator<Instance> instanceIterator)
            throws Exception {

            if (instanceIterator.hasNext()) {
              FoldMaker foldMaker =
                new FoldMaker(headerNoSummary, numFolds, seed);

              while (instanceIterator.hasNext()) {
                foldMaker.processInstance(instanceIterator.next());
              }

              foldMaker.finalizeTask();
              m_partialFoldsForPartition.add(foldMaker);
            }

            return m_partialFoldsForPartition;
          }
        });

    if (getDebug()) {
      logMessage("[MLlib evaluation] Number of partitions in original data: "
        + dataset.getNumPartitions());
      logMessage("[MLlib evaluation] Number of partitions in foldMap: "
        + foldMap.getNumPartitions());
    }
    if (foldMap.getNumPartitions() < origNumPartitions) {
      foldMap = foldMap.repartition(origNumPartitions);
    }

    return foldMap.persist(StorageLevel.MEMORY_AND_DISK());
  }

  /**
   * Get an RDD of the instances comprising a training fold
   *
   * @param folds the RDD of {@code FoldMakers}
   * @param foldNumber the 1-based number of the fold to get
   * @return an RDD containing all the training instances that belong to the
   *         fold
   * @throws DistributedWekaException if a problem occurs
   */
  protected JavaRDD<Instance> getTrainingFold(JavaRDD<FoldMaker> folds,
    final int foldNumber) throws DistributedWekaException {

    JavaRDD<Instance> trainingFold =
      folds.mapPartitions(new FlatMapFunction<Iterator<FoldMaker>, Instance>() {
        private static final long serialVersionUID = -529804293727416419L;

        protected List<Instance> m_trainingInstancesForFold =
          new ArrayList<Instance>();

        @Override
        public Iterable<Instance> call(Iterator<FoldMaker> foldMakerIterator)
          throws DistributedWekaException {
          // we should be processing just one FoldMaker per partition/call

          if (foldMakerIterator.hasNext()) {
            int count = 0;
            while (foldMakerIterator.hasNext()) {
              if (count > 1) {
                throw new DistributedWekaException(
                  "We seem to have more than one "
                    + "FoldMaker in this partition!");
              }
              FoldMaker current = foldMakerIterator.next();
              Instances training = current.getTrainingFold(foldNumber);
              m_trainingInstancesForFold = new ArrayList<>(training.size());
              for (int i = 0; i < training.numInstances(); i++) {
                m_trainingInstancesForFold.add(training.get(i));
              }

              count++;
            }
          }

          return m_trainingInstancesForFold;
        }
      }).persist(getCachingStrategy().getStorageLevel());
    // materialize
    trainingFold.count();

    return trainingFold;
  }

  /**
   * Get an RDD of the instances comprising a test fold
   *
   * @param folds the RDD of {@code FoldMakers}
   * @param foldNumber the 1-based number of the fold to get
   * @return an RDD containing all the training instances that belong to the
   *         fold
   * @throws DistributedWekaException if a problem occurs
   */
  protected JavaRDD<Instance> getTestFold(JavaRDD<FoldMaker> folds,
    final int foldNumber) throws DistributedWekaException {

    JavaRDD<Instance> testFold =
      folds.mapPartitions(new FlatMapFunction<Iterator<FoldMaker>, Instance>() {

        private static final long serialVersionUID = -6559772554897005039L;

        protected List<Instance> m_testInstancesForFold =
          new ArrayList<Instance>();

        @Override
        public Iterable<Instance> call(Iterator<FoldMaker> foldMakerIterator)
          throws DistributedWekaException {

          // we should be processing just one FoldMaker per partition/call

          if (foldMakerIterator.hasNext()) {
            int count = 0;
            while (foldMakerIterator.hasNext()) {
              if (count > 1) {
                throw new DistributedWekaException(
                  "We seem to have more than one "
                    + "FoldMaker in this partition!");
              }
              FoldMaker current = foldMakerIterator.next();
              Instances test = current.getTestFold(foldNumber);
              m_testInstancesForFold = new ArrayList<>(test.size());
              for (int i = 0; i < test.numInstances(); i++) {
                m_testInstancesForFold.add(test.get(i));
              }

              count++;
            }
          }

          return m_testInstancesForFold;
        }
      }).persist(getCachingStrategy().getStorageLevel());
    // materialize
    testFold.count();

    return testFold;
  }

  /**
   * Build the MLlib classifier/regressor models for the training folds
   * 
   * @param dataset the dataset to operate on
   * @param folds the number of cross-validation folds
   * @param seed the random seed to use
   * @param headerWithSummary the header of the dataset (with summary
   *          attributes)
   * @return an array of MLlibClassifier objects - each element is a model for a
   *         training fold
   * @throws DistributedWekaException if a problem occurs
   */
  protected MLlibClassifier[]
    phaseOneBuildClassifiers(JavaRDD<Instance> dataset, int folds, int seed,
      Instances headerWithSummary) throws DistributedWekaException {

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
    headerNoSummary.setClassIndex(headerWithSummary.classIndex());

    MLlibClassifier[] classifiers = new MLlibClassifier[folds];
    JavaRDD<FoldMaker> foldRDD =
      createFolds(dataset, headerNoSummary, folds, seed);

    logMessage("[MLlib evaluation] Phase 1 - building fold classifiers");
    try {
      for (int i = 1; i <= folds; i++) {
        logMessage("[MLlib evaluation] Getting training fold " + i);
        JavaRDD<Instance> training = getTrainingFold(foldRDD, i);
        logMessage("[MLlib evaluation] Building model for fold " + i);
        MLlibClassifier toTrain = m_classifier.getClass().newInstance();
        toTrain.setOptions(m_classifier.getOptions());
        toTrain.buildClassifier(training, headerWithSummary, m_preprocessors,
          getCachingStrategy());
        classifiers[i - 1] = toTrain;
        training.unpersist();
      }
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }

    foldRDD.unpersist(); // no longer needed

    return classifiers;
  }

  /**
   * Evaluate MLlib classifiers/regressors on test folds
   * 
   * @param dataset the dataset to operate on
   * @param headerWithSummary the header of the dataset (with summary
   *          attributes)
   * @param classifiers an array of MLlibClassifiers
   * @param folds the number of folds
   * @param seed the random seed to use
   * @return an aggregated {@code Evaluation} object containing the overall
   *         evaluation for the MLlib scheme
   * @throws DistributedWekaException if a problem occurs
   */
  protected Evaluation phaseTwoEvaluateClassifiers(JavaRDD<Instance> dataset,
    final Instances headerWithSummary, final MLlibClassifier[] classifiers,
    final int folds, final int seed) throws DistributedWekaException {

    final Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
    headerNoSummary.setClassIndex(headerWithSummary.classIndex());

    // an sample size > 0 indicates that we will be retaining
    // predictions in order to compute auc/auprc
    String predFracS = getSampleFractionForAUC();
    double predFrac = 0;
    if (!DistributedJobConfig.isEmpty(predFracS)) {
      try {
        predFrac = Double.parseDouble(predFracS);
      } catch (NumberFormatException ex) {
        logMessage("Warning: unable to parse the fraction of predictions to retain: "
          + predFracS);
      }
    }
    final double fpredFrac = predFrac;

    final Attribute classAtt = headerNoSummary.classAttribute();
    String classAttSummaryName =
      CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX + classAtt.name();
    Attribute summaryClassAtt =
      headerWithSummary.attribute(classAttSummaryName);
    if (summaryClassAtt == null) {
      throw new DistributedWekaException(
        "Evaluate "
          + "classifiers: was unable to find the summary metadata attribute for the "
          + "class attribute in the header");
    }

    double priorsCount = 0;
    double[] priors =
      new double[classAtt.isNominal() ? classAtt.numValues() : 1];
    if (classAtt.isNominal()) {
      for (int i = 0; i < classAtt.numValues(); i++) {
        String label = classAtt.value(i);
        String labelWithCount =
          summaryClassAtt.value(i).replace(label + "_", "").trim();

        try {
          priors[i] = Double.parseDouble(labelWithCount);
        } catch (NumberFormatException n) {
          throw new DistributedWekaException(n);
        }
      }

      priorsCount = classAtt.numValues();
    } else {

      double count =
        ArffSummaryNumericMetric.COUNT.valueFromAttribute(summaryClassAtt);
      double sum =
        ArffSummaryNumericMetric.SUM.valueFromAttribute(summaryClassAtt);

      priors[0] = sum;
      priorsCount = count;
    }
    final double[] fpriors = priors;
    final double fpriorsCount = priorsCount;

    logMessage("[MLlib evaluation] Evaluating classifiers on " + "test folds");

    JavaRDD<Evaluation> mapFolds =
      dataset
        .mapPartitions(new FlatMapFunction<Iterator<Instance>, Evaluation>() {
          private static final long serialVersionUID = 8765894253849522324L;

          protected List<Evaluation> m_evaluationForPartition =
            new ArrayList<Evaluation>();

          @Override
          public Iterable<Evaluation> call(Iterator<Instance> instanceIterator)
            throws Exception {

            WekaClassifierEvaluationMapTask[] evalTasks =
              new WekaClassifierEvaluationMapTask[folds];
            for (int i = 0; i < folds; i++) {
              evalTasks[i] = new WekaClassifierEvaluationMapTask();
              evalTasks[i].setClassifier(classifiers[i]);
              evalTasks[i].setTotalNumFolds(folds);
              evalTasks[i].setFoldNumber(i + 1);
              evalTasks[i].setup(headerNoSummary, fpriors, fpriorsCount, seed,
                fpredFrac);
            }

            while (instanceIterator.hasNext()) {
              Instance current = instanceIterator.next();
              for (WekaClassifierEvaluationMapTask t : evalTasks) {
                t.processInstance(current);
              }
            }

            AggregateableEvaluation agg = null;
            // finalize
            for (int i = 0; i < folds; i++) {
              evalTasks[i].finalizeTask();
              Evaluation eval = evalTasks[i].getEvaluation();

              // save memory
              evalTasks[i] = null;
              classifiers[i] = null;

              if (agg == null) {
                agg = new AggregateableEvaluation(eval);
              }
              agg.aggregate(eval);
            }

            if (agg != null) {
              m_evaluationForPartition.add(agg);
            }

            return m_evaluationForPartition;
          }
        });

    // reduce locally
    logMessage("[MLlibClassifierEvaluation] Reducing partial evaluations");

    try {
      List<Evaluation> evals = mapFolds.collect();
      AggregateableEvaluation aggEval =
        new AggregateableEvaluation(evals.get(0));
      for (Evaluation e : evals) {
        aggEval.aggregate(e);
      }

      return aggEval;
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }
  }

  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    m_currentContext = sparkContext;
    boolean success;
    setJobStatus(JobStatus.RUNNING);

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    // Make sure that we save out to a subdirectory of the output
    // directory
    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);
    if (!DistributedJobConfig.isEmpty(m_optionalOutputSubDir)) {
      outputPath =
        addSubdirToPath(outputPath,
          environmentSubstitute(m_optionalOutputSubDir));
    }

    int totalFolds = 1;
    if (!DistributedJobConfig.isEmpty(getTotalFolds())) {
      totalFolds = Integer.parseInt(environmentSubstitute(getTotalFolds()));
    }

    if (totalFolds < 1) {
      throw new DistributedWekaException("Total folds can't be less than 1!");
    }

    if (totalFolds > 1
      && !DistributedJobConfig.isEmpty(getSeparateTestSetDataSource())) {
      throw new DistributedWekaException(
        "Total folds is > 1 and a separate test set "
          + "has been specified - can only perform one or the other out "
          + "of a cross-validation or separate test set evaluation");
    }

    int seed = 1;
    if (!DistributedJobConfig.isEmpty(getSeed())) {
      seed = Integer.parseInt(environmentSubstitute(getSeed()));
    }

    JavaRDD<Instance> dataSet = null;
    Instances headerWithSummary = null;
    if (getDataset(TRAINING_DATA) != null) {
      dataSet = getDataset(TRAINING_DATA).getRDD();
      headerWithSummary = getDataset(TRAINING_DATA).getHeaderWithSummary();
      logMessage("[MLlib evaluation] RDD<Instance> dataset provided: "
        + dataSet.partitions().size() + " partitions.");
      logMessage("[MLlib evaluation] Current caching strategy: "
        + getCachingStrategy());
    }

    if (dataSet == null) {
      logMessage("[MLlib evaluation] Invoking ARFF job...");
      m_arffHeaderJob.setEnvironment(m_env);
      m_arffHeaderJob.setLog(getLog());
      m_arffHeaderJob.setStatusMessagePrefix(m_statusMessagePrefix);
      success = m_arffHeaderJob.runJobWithContext(sparkContext);
      setCachingStrategy(m_arffHeaderJob.getCachingStrategy());

      if (!success) {
        setJobStatus(JobStatus.FAILED);
        statusMessage("Unable to continue - creating the ARFF header failed!");
        logMessage("[MLlib evaluation] Unable to continue - creating the ARFF header failed!");
        return false;
      }

      Dataset d = m_arffHeaderJob.getDataset(TRAINING_DATA);
      headerWithSummary = d.getHeaderWithSummary();
      dataSet = d.getRDD();
      setDataset(TRAINING_DATA, d);
      logMessage("[MLlib evaluation] Fetching RDD<Instance> dataset "
        + "from ARFF job: " + dataSet.partitions().size() + " partitions.");
    }

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
    String classAtt = "";
    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      classAtt = environmentSubstitute(getClassAttribute());
    }
    try {
      WekaClassifierSparkJob.setClassIndex(classAtt, headerNoSummary, true);
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }

    if (getRandomizeAndStratify()) {
      m_randomizeSparkJob.setEnvironment(m_env);
      m_randomizeSparkJob.setDefaultToLastAttIfClassNotSpecified(true);
      m_randomizeSparkJob.setStatusMessagePrefix(m_statusMessagePrefix);
      m_randomizeSparkJob.setLog(getLog());
      m_randomizeSparkJob.setCachingStrategy(getCachingStrategy());
      m_randomizeSparkJob.setDataset(TRAINING_DATA, new Dataset(dataSet,
        headerWithSummary));
      m_randomizeSparkJob.setRandomSeed(getSeed());

      if (!m_randomizeSparkJob.runJobWithContext(sparkContext)) {
        statusMessage("Unable to continue - randomization/stratification of input data failed!");
        logMessage("[MLlib evaluation] Unable to continue - randomization/stratification of input data failed!");
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

    // clean the output directory
    SparkJob.deleteDirectory(outputPath);

    // set class index on the header with summary
    headerWithSummary.setClassIndex(headerNoSummary.classIndex());
    MLlibClassifier[] foldClassifiers =
      phaseOneBuildClassifiers(dataSet, totalFolds, seed, headerWithSummary);

    // separate test set?
    Dataset testDataset =
      getEvaluateOnTestSetIfPresent() ? getDataset(TEST_DATA) : null;
    JavaRDD<Instance> testDatasetRDD =
      testDataset != null ? testDataset.getRDD() : null;
    if (testDatasetRDD != null) {
      logMessage("[MLlib evaluation] RDD<Instance> test dataset provided: "
        + testDatasetRDD.partitions().size() + " partitions.");
    } else {
      logMessage("[MLlib evaluation] No separate test set provided - using training data");
    }

    // the following handles command-line execution only with
    // respect to a separate test set datasource
    if (testDatasetRDD == null && m_testSetDS != null) {
      // dataset.unpersist();

      m_testSetDS.setEnvironment(m_env);
      m_testSetDS.setLog(getLog());
      m_testSetDS.setStatusMessagePrefix(m_statusMessagePrefix);
      m_testSetDS.applyConfigProperties(sparkContext);
      // make sure it creates a test set (rather than picking up the training
      // data type)!!
      m_testSetDS.setDatasetType(new SelectedTag(DataSource.DATASET_TYPE.TEST
        .ordinal(), DataSource.TAGS_SELECTION));
      m_testSetDS.runJobWithContext(sparkContext);

      CSVToARFFHeaderMapTask rowHelper = new CSVToARFFHeaderMapTask();
      try {
        rowHelper.setOptions(Utils.splitOptions(m_arffHeaderJob
          .getCsvToArffTaskOptions()));
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
      Dataset testD = m_testSetDS.getDataset(Dataset.TEST_DATA);
      testD.setHeaderWithSummary(headerWithSummary);
      testD.materializeRDD(m_testSetDS.getCachingStrategy(), rowHelper, false);

      // transfer the dataset over to this job's dataset manager
      m_datasetManager.addAll(m_testSetDS.getDatasetIterator());
      testDatasetRDD = m_datasetManager.getDataset(Dataset.TEST_DATA).getRDD();
    }

    Evaluation results =
      phaseTwoEvaluateClassifiers(testDatasetRDD != null ? testDatasetRDD
        : dataSet, headerWithSummary, foldClassifiers, totalFolds, seed);

    storeAndWriteEvalResults(results, headerNoSummary, totalFolds,
      testDatasetRDD != null, getSeed(), outputPath);

    return true;
  }

  /**
   * Stores the results and writes them to the output path
   *
   * @param aggregated the final Evaluation object
   * @param headerNoSummary the header of the training data without summary
   *          attributes
   * @param totalFolds the total number of folds used in the evaluation
   * @param testSetPresent true if a separate test set was used
   * @param seed the random number seed used for shuffling and fold creation
   * @param outputPath the output path
   * @throws IOException if a problem occurs
   */
  protected void storeAndWriteEvalResults(Evaluation aggregated,
    Instances headerNoSummary, int totalFolds, boolean testSetPresent,
    String seed, String outputPath) throws IOException {

    StringBuilder buff = new StringBuilder();
    String info = "Summary - ";
    if (testSetPresent) {
      info += "separate test set";
    } else if (totalFolds == 1) {
      info += "test on training";
    } else {
      info +=
        totalFolds + " fold cross-validation (seed=" + seed + "): "
          + m_classifier.getClass().getCanonicalName() + " "
          + Utils.joinOptions(m_classifier.getOptions())
          + "\n(note: relative measures might be slightly "
          + "pessimistic due to the mean/mode of the target being computed on "
          + "all the data rather than on training folds)";
    }
    info += ":\n";
    if (aggregated.predictions() != null) {
      info +=
        "Number of predictions retained for computing AUC/AUPRC: "
          + aggregated.predictions().size() + "\n";
    }
    buff.append(info).append("\n\n");
    buff.append(aggregated.toSummaryString()).append("\n");
    if (headerNoSummary.classAttribute().isNominal()) {
      try {
        buff.append(aggregated.toClassDetailsString()).append("\n");
        buff.append(aggregated.toMatrixString()).append("\n");
      } catch (Exception ex) {
        logMessage(ex);
        throw new IOException(ex);
      }
    }

    String evalOutputPath =
      outputPath
        + (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
        + "evaluation.txt";
    m_textEvalResults = buff.toString();
    PrintWriter writer = null;
    try {
      writer = openTextFileForWrite(evalOutputPath);
      writer.println(m_textEvalResults);
    } finally {
      if (writer != null) {
        writer.flush();
        writer.close();
        writer = null;
      }
    }

    OutputStream stream = null;
    try {
      Instances asInstances =
        WekaClassifierEvaluationReduceTask
          .evaluationResultsToInstances(aggregated);
      m_evalResults = asInstances;

      String arffOutputPath =
        outputPath
          + (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
          + "evaluation.arff";
      writer = openTextFileForWrite(arffOutputPath);
      writer.println(asInstances.toString());

      String csvOutputPath =
        outputPath
          + (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
          + "evaluation.csv";
      stream = openFileForWrite(csvOutputPath);
      CSVSaver saver = new CSVSaver();
      saver.setRetrieval(Saver.BATCH);
      saver.setInstances(asInstances);
      saver.setDestination(stream);
      saver.writeBatch();
    } catch (Exception ex) {
      logMessage(ex);
      throw new IOException(ex);
    } finally {
      if (writer != null) {
        writer.flush();
        writer.close();
      }
      if (stream != null) {
        stream.flush();
        stream.close();
      }
    }
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {
    if (!(toRun instanceof MLlibClassifierEvaluationSparkJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a MLLibClassifierEvaluationSparkJob!");
    }

    try {
      MLlibClassifierEvaluationSparkJob wcsj =
        (MLlibClassifierEvaluationSparkJob) toRun;
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
  public Instances getInstances() {
    return m_evalResults;
  }

  @Override
  public String getText() {
    return m_textEvalResults;
  }

  public static void main(String[] args) {
    MLlibClassifierEvaluationSparkJob wcsj =
      new MLlibClassifierEvaluationSparkJob();
    wcsj.run(wcsj, args);
  }

  /**
   * Helper class for obtaining instances for a given training or test fold in a
   * cross-validation
   */
  protected static class FoldMaker implements Serializable {

    private static final long serialVersionUID = 6595943145229245724L;

    protected Instances m_header;
    protected int m_seed = 1;
    protected int m_totalFolds;
    protected boolean m_isFinalized;
    protected Random m_r;

    public FoldMaker(Instances header, int totalFolds, int randomSeed) {
      m_header = header;
      m_seed = randomSeed;
      m_totalFolds = totalFolds;
    }

    public Instances getTestFold(int foldNumber)
      throws DistributedWekaException {
      if (m_totalFolds > 1 && foldNumber >= 1) {
        return m_header.testCV(m_totalFolds, foldNumber);
      }

      throw new DistributedWekaException(
        "Unable to provide a test fold for fold number " + foldNumber
          + " out of " + m_totalFolds + " folds");
    }

    public Instances getTrainingFold(int foldNumber)
      throws DistributedWekaException {
      if (m_totalFolds > 1 && foldNumber >= 1) {
        return m_header.trainCV(m_totalFolds, foldNumber - 1, m_r);
      }

      throw new DistributedWekaException(
        "Unable to provide a training fold for fold number " + foldNumber
          + " out of " + m_totalFolds + " folds");
    }

    public void processInstance(Instance current) {
      // TODO implement reservoir sampling as an option
      m_header.add(current);
    }

    public void finalizeTask() {
      m_header.compactify();
      m_r = new Random(m_seed);
      m_header.randomize(m_r);
      if (m_header.classIndex() >= 0 && m_header.classAttribute().isNominal()
        && m_totalFolds > 1) {
        m_header.stratify(m_totalFolds);
      }
      m_isFinalized = true;
    }
  }
}
