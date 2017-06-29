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
 *    WekaClassifierEvaluationSparkJob
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.storage.StorageLevel;
import scala.Tuple2;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.AggregateableEvaluation;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Attribute;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.core.converters.CSVSaver;
import weka.core.converters.Saver;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.WekaClassifierEvaluationMapTask;
import weka.distributed.WekaClassifierEvaluationReduceTask;
import weka.distributed.WekaClassifierMapTask;
import weka.distributed.WekaClassifierReduceTask;
import weka.filters.PreconstructedFilter;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.InstancesProducer;
import weka.gui.beans.TextProducer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Spark job for running an evaluation of a classifier or a regressor. Invokes
 * up to four separate jobs (passes over the data). 1) Header creation, 2)
 * optional randomly shuffled data chunk creation, 3) model construction and 4)
 * model evaluation. Can perform evaluation on the training data, on a separate
 * test set, or via cross-validation. In the case of the later, models for all
 * folds are created and aggregated in one pass (job) and then evaluated on all
 * folds in a second job.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 12253 $
 */
public class WekaClassifierEvaluationSparkJob extends SparkJob implements
  TextProducer, InstancesProducer, CommandlineRunnable {

  /**
   * The subdirectory of the output directory that this job saves its results to
   */
  protected static final String OUTPUT_SUBDIR = "eval";

  /** For serialization */
  private static final long serialVersionUID = 8099932783095825201L;

  /** Classifier job (used just for option parsing) */
  protected WekaClassifierSparkJob m_classifierJob =
    new WekaClassifierSparkJob();

  /** Textual evaluation results if job successful */
  protected String m_textEvalResults;

  /** Instances version of the evaluation results */
  protected Instances m_evalResults;

  /**
   * Spec for a data source to use for obtaining a test set (if not doing
   * cross-validation or test on training). Command line use only
   */
  protected String m_separateTestSetDataSource = "";

  /** Actual test set data source instance (command line use only) */
  protected DataSource m_testSetDS;

  /** Knowledge Flow use only */
  protected boolean m_evalOnTestSetIfPresent;

  /**
   * Fraction of predictions to retain in order to compute auc/auprc.
   * Predictions are not retained if this is unspecified or the fraction is set
   * {@code <= 0}
   */
  protected String m_predFrac = "";

  /**
   * Optional user-supplied subdirectory of [output_dir]/eval in which to store
   * results
   */
  protected String m_optionalOutputSubDir = "";

  /**
   * Constructor
   */
  public WekaClassifierEvaluationSparkJob() {
    super("Weka classifier evaluation job", "Evaluates a Weka classifier");
  }

  public static void main(String[] args) {
    WekaClassifierEvaluationSparkJob wcesj =
      new WekaClassifierEvaluationSparkJob();
    wcesj.run(wcesj, args);
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

    result.add(new Option("", "", 0,
      "\nOptions specific to model building and evaluation:"));

    result
      .add(new Option(
        "\tSeparate data source for loading a test set. Set either this or\n\t"
          + "total-folds for a cross-validation (note that setting total-folds\n\t"
          + "to 1 will perform testing on training)", "test-set-data-source",
        1, "-test-set-data-source <spec>"));
    result.add(new Option(
      "\tCompute AUC and AUPRC. Note that this requires individual\n\t"
        + "predictions to be retained - specify a fraction of\n\t"
        + "predictions to sample (e.g. 0.5) in order to save resources.",
      "auc", 1, "-auc <fraction of predictions to sample>"));
    result.add(new Option("\tOptional sub-directory of <output-dir>/eval "
      + "in which to store results.", "output-subdir", 1,
      "-output-subdir <directory name>"));

    WekaClassifierSparkJob tempClassifierJob = new WekaClassifierSparkJob();

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

    if (!DistributedJobConfig.isEmpty(getSeparateTestSetDataSource())) {
      options.add("-test-set-data-source");
      options.add(getSeparateTestSetDataSource());
    }

    if (!DistributedJobConfig.isEmpty(getSampleFractionForAUC())) {
      options.add("-auc");
      options.add(getSampleFractionForAUC());
    }

    if (!DistributedJobConfig.isEmpty(getOutputSubdir())) {
      options.add("-output-subdir");
      options.add(getOutputSubdir());
    }

    if (getEvaluateOnTestSetIfPresent()) {
      options.add("-evaluate-on-test");
    }

    String[] classifierJobOpts = m_classifierJob.getOptions();
    for (String o : classifierJobOpts) {
      options.add(o);
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    m_testSetDS = null;
    String separateTestSet = Utils.getOption("test-set-data-source", options);
    setSeparateTestSetDataSource(separateTestSet);

    String auc = Utils.getOption("auc", options);
    setSampleFractionForAUC(auc);

    String outputSubDir = Utils.getOption("output-subdir", options);
    setOutputSubdir(outputSubDir);
    setEvaluateOnTestSetIfPresent(Utils.getFlag("evaluate-on-test", options));

    String[] optionsCopy = options.clone();

    super.setOptions(options);

    m_classifierJob.setOptions(optionsCopy.clone());
    if (!DistributedJobConfig.isEmpty(getSeparateTestSetDataSource())) {
      m_testSetDS =
        (DataSource) Utils.forName(DataSource.class,
          environmentSubstitute(getSeparateTestSetDataSource()), null);
      m_testSetDS.setOptions(optionsCopy.clone());
    }
  }

  /**
   * Get the options pertaining to this job only
   * 
   * @return the options for this job only
   */
  public String[] getJobOptionsOnly() {
    List<String> options = new ArrayList<String>();

    if (!DistributedJobConfig.isEmpty(getSeparateTestSetDataSource())) {
      options.add("-test-set-data-source");
      options.add(getSeparateTestSetDataSource());
    }

    if (!DistributedJobConfig.isEmpty(getSampleFractionForAUC())) {
      options.add("-auc");
      options.add(getSampleFractionForAUC());
    }

    if (!DistributedJobConfig.isEmpty(getOutputSubdir())) {
      options.add("-output-subdir");
      options.add(getOutputSubdir());
    }

    if (getEvaluateOnTestSetIfPresent()) {
      options.add("-evaluate-on-test");
    }

    return options.toArray(new String[options.size()]);
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
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String evaluateOnTestSetIfPresentTipText() {
    return "Evaluate on a test dataset (if present) rather than perform "
      + "cross-validation on the training data.";
  }

  /**
   * Set whether to evaluate on a test set (if present in the dataset manager).
   * Knowledge Flow mode only
   *
   * @param evaluateOnTestSetIfPresent true to evaluate on a test set if
   *          available
   */
  public void setEvaluateOnTestSetIfPresent(boolean evaluateOnTestSetIfPresent) {
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
   * Tool tip text for this property
   *
   * @return the tool tip text for this property
   */
  public String outputSubdirTipText() {
    return "An optional subdirectory of <output-dir>/eval in which to store the "
      + "results";
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
  public void setOutputSubdir(String subdir) {
    m_optionalOutputSubDir = subdir;
  }

  protected Classifier[] phaseOneBuildClassifiers(JavaRDD<Instance> dataset,
    final Instances headerNoSummary,
    final PreconstructedFilter preconstructedFilter) throws Exception {

    int totalFolds = 1;
    final String classifierMapTaskOptions =
      environmentSubstitute(m_classifierJob.getClassifierMapTaskOptions());
    String[] cOpts = Utils.splitOptions(classifierMapTaskOptions);
    String numFolds = Utils.getOption("total-folds", cOpts.clone());
    final boolean forceVote = Utils.getFlag("force-vote", cOpts.clone());
    if (!DistributedJobConfig.isEmpty(numFolds)) {
      totalFolds = Integer.parseInt(numFolds);
    }
    final int tFolds = totalFolds;

    final Classifier[] foldClassifiers = new Classifier[totalFolds];

    // just use headerNoSummary for class index
    final int classIndex = headerNoSummary.classIndex();
    final int numPartitions = dataset.partitions().size();

    int numIterations = m_classifierJob.getNumIterations();

    final int numSplits = dataset.partitions().size();

    for (int i = 0; i < numIterations; i++) {
      final int iterationNum = i;
      logMessage("[WekaClassifierEvaluation] Phase 1 (map), iteration "
        + (i + 1));

      JavaPairRDD<Integer, Classifier> mapFolds =
        dataset
          .mapPartitionsToPair(new PairFlatMapFunction<Iterator<Instance>, Integer, Classifier>() {

            /** For serialization */
            private static final long serialVersionUID = -1906414304952140395L;

            protected Instances m_header;

            /** Holds results */
            protected List<Tuple2<Integer, Classifier>> m_classifiersForFolds =
              new ArrayList<Tuple2<Integer, Classifier>>();

            @Override
            public Iterable<Tuple2<Integer, Classifier>> call(
              Iterator<Instance> split) throws IOException,
              DistributedWekaException {

              Instance current = split.next();
              if (current == null) {
                throw new IOException("No data in this partition!!");
              }

              m_header = current.dataset();
              m_header.setClassIndex(classIndex);
              // WekaClassifierMapTask tempTask = new WekaClassifierMapTask();
              // try {
              // WekaClassifierSparkJob.configureClassifierMapTask(tempTask,
              // null, classifierMapTaskOptions, iterationNum,
              // preconstructedFilter, numSplits);
              // } catch (Exception ex) {
              // throw new DistributedWekaException(ex);
              // }
              //
              // boolean isUpdateableClassifier = tempTask.getClassifier()
              // instanceof UpdateableClassifier;
              // boolean forceBatchForUpdateable =
              // tempTask.getForceBatchLearningForUpdateableClassifiers();

              WekaClassifierMapTask[] tasks = new WekaClassifierMapTask[tFolds];
              for (int j = 0; j < tFolds; j++) {
                try {
                  tasks[j] = new WekaClassifierMapTask();
                  WekaClassifierSparkJob.configureClassifierMapTask(tasks[j],
                    foldClassifiers[j], classifierMapTaskOptions, iterationNum,
                    preconstructedFilter, numSplits);

                  // set fold number and total folds
                  tasks[j].setFoldNumber(j + 1);
                  tasks[j].setTotalNumFolds(tFolds);
                  Environment env = new Environment();
                  env.addVariable(WekaClassifierMapTask.TOTAL_NUMBER_OF_MAPS,
                    "" + numPartitions);
                  tasks[j].setEnvironment(env);
                } catch (Exception ex) {
                  logMessage(ex);
                  throw new DistributedWekaException(ex);
                }

                // initialize
                tasks[j].setup(headerNoSummary);
              }

              while (split.hasNext()) {
                current = split.next();

                for (int j = 0; j < tFolds; j++) {
                  tasks[j].processInstance(current);
                }
              }

              for (int j = 0; j < tFolds; j++) {
                tasks[j].finalizeTask();
                m_classifiersForFolds.add(new Tuple2<Integer, Classifier>(j,
                  tasks[j].getClassifier()));
              }

              return m_classifiersForFolds;
            }
          });
      mapFolds = mapFolds.persist(StorageLevel.MEMORY_AND_DISK());
      JavaPairRDD<Integer, Classifier> mapFoldsSorted = mapFolds.sortByKey();// .persist(StorageLevel.MEMORY_AND_DISK());
      mapFoldsSorted =
        mapFoldsSorted.partitionBy(new IntegerKeyPartitioner(totalFolds))
          .persist(StorageLevel.MEMORY_AND_DISK());

      // memory and disk here for fast access and to avoid
      // recomputing partial classifiers if all partial classifiers
      // can't fit in memory

      // reduce fold models
      logMessage("[WekaClassifierEvaluation] Phase 1 (reduce), iteration "
        + (i + 1));
      JavaPairRDD<Integer, Classifier> reducedByFold =
        mapFoldsSorted
          .mapPartitionsToPair(new PairFlatMapFunction<Iterator<Tuple2<Integer, Classifier>>, Integer, Classifier>() {

            /** For serialization */
            private static final long serialVersionUID = 2481672301097842496L;

            /** Holds reduced classifier for one fold (partition) */
            protected List<Tuple2<Integer, Classifier>> m_reducedForFold =
              new ArrayList<Tuple2<Integer, Classifier>>();

            @Override
            public Iterable<Tuple2<Integer, Classifier>> call(
              Iterator<Tuple2<Integer, Classifier>> split)
              throws DistributedWekaException {

              int foldNum = -1;

              List<Classifier> classifiers = new ArrayList<Classifier>();
              while (split.hasNext()) {
                Tuple2<Integer, Classifier> partial = split.next();
                if (foldNum < 0) {
                  foldNum = partial._1().intValue();
                } else {
                  if (partial._1().intValue() != foldNum) {
                    throw new DistributedWekaException(
                      "[WekaClassifierEvaluation] build "
                        + "classifiers reduce phase: was not expecting fold number "
                        + "to change within a partition!");
                  }
                }
                classifiers.add(partial._2());
              }

              WekaClassifierReduceTask reduceTask =
                new WekaClassifierReduceTask();
              Classifier intermediateClassifier =
                reduceTask.aggregate(classifiers, null, forceVote);

              m_reducedForFold.add(new Tuple2<Integer, Classifier>(foldNum,
                intermediateClassifier));

              return m_reducedForFold;
            }
          });

      List<Tuple2<Integer, Classifier>> aggregated = reducedByFold.collect();
      for (Tuple2<Integer, Classifier> t : aggregated) {
        foldClassifiers[t._1()] = t._2();
      }

      mapFolds.unpersist();
      mapFoldsSorted.unpersist();
      reducedByFold.unpersist();
    }

    return foldClassifiers;
  }

  protected Evaluation phaseTwoEvaluateClassifiers(JavaRDD<Instance> dataset,
    final Instances headerWithSummary, final Instances headerNoSummary,
    final Classifier[] foldClassifiers) throws Exception {

    int totalFolds = 1;
    final String classifierMapTaskOptions =
      environmentSubstitute(m_classifierJob.getClassifierMapTaskOptions());
    String[] cOpts = Utils.splitOptions(classifierMapTaskOptions);
    String numFolds = Utils.getOption("total-folds", cOpts);
    if (!DistributedJobConfig.isEmpty(numFolds)) {
      totalFolds = Integer.parseInt(numFolds);
    }
    final int tFolds = totalFolds;
    final boolean forceBatch = Utils.getFlag("force-batch", cOpts);
    String sSeed = Utils.getOption("seed", cOpts);
    long seed = 1L;
    if (!DistributedJobConfig.isEmpty(sSeed)) {
      try {
        sSeed = m_env.substitute(sSeed);
      } catch (Exception ex) {
      }

      try {
        seed = Long.parseLong(sSeed);
      } catch (NumberFormatException ex) {
      }
    }
    final long fseed = seed;

    // an sample size > 0 indicates that we will be retaining
    // predictions in order to compute auc/auprc
    String predFracS = getSampleFractionForAUC();
    double predFrac = 0;
    if (!DistributedJobConfig.isEmpty(predFracS)) {
      try {
        predFrac = Double.parseDouble(predFracS);
      } catch (NumberFormatException ex) {
        System.err
          .println("Unable to parse the fraction of predictions to retain: "
            + predFracS);
      }
    }
    final double fpredFrac = predFrac;

    Attribute classAtt = headerNoSummary.classAttribute();
    String classAttSummaryName =
      CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX + classAtt.name();
    Attribute summaryClassAtt =
      headerWithSummary.attribute(classAttSummaryName);
    if (summaryClassAtt == null) {
      throw new DistributedWekaException(
        "[WekaClassifierEvaluation] evaluate "
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
          throw new Exception(n);
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

    // map phase
    logMessage("[WekaClassifierEvaluation] Phase 2 (map)");
    JavaRDD<Evaluation> mapFolds =
      dataset
        .mapPartitions(new FlatMapFunction<Iterator<Instance>, Evaluation>() {

          /** For serialization */
          private static final long serialVersionUID = 5800617408839460876L;

          protected List<Evaluation> m_evaluationForPartition =
            new ArrayList<Evaluation>();

          @Override
          public Iterable<Evaluation> call(Iterator<Instance> split)
            throws IOException, DistributedWekaException {

            // setup base tasks
            WekaClassifierEvaluationMapTask[] evalTasks =
              new WekaClassifierEvaluationMapTask[tFolds];
            for (int i = 0; i < tFolds; i++) {
              evalTasks[i] = new WekaClassifierEvaluationMapTask();
              evalTasks[i].setClassifier(foldClassifiers[i]);
              evalTasks[i].setTotalNumFolds(tFolds);
              evalTasks[i].setFoldNumber(i + 1);
              evalTasks[i].setBatchTrainedIncremental(forceBatch);
              try {
                evalTasks[i].setup(headerNoSummary, fpriors, fpriorsCount,
                  fseed, fpredFrac);
              } catch (Exception ex) {
                throw new DistributedWekaException(ex);
              }
            }

            try {
              while (split.hasNext()) {
                Instance current = split.next();
                for (WekaClassifierEvaluationMapTask t : evalTasks) {
                  t.processInstance(current);
                }
              }

              AggregateableEvaluation agg = null;
              // finalize
              for (int i = 0; i < tFolds; i++) {
                evalTasks[i].finalizeTask();
                Evaluation eval = evalTasks[i].getEvaluation();

                // save memory
                evalTasks[i] = null;
                foldClassifiers[i] = null;

                if (agg == null) {
                  agg = new AggregateableEvaluation(eval);
                }
                agg.aggregate(eval);
              }

              if (agg != null) {
                m_evaluationForPartition.add(agg);
              }

            } catch (Exception ex) {
              throw new DistributedWekaException(ex);
            }

            return m_evaluationForPartition;
          }
        });

    // reduce locally
    logMessage("[WekaClassifierEvaluation] Phase 2 (reduce)");
    List<Evaluation> evals = mapFolds.collect();
    AggregateableEvaluation aggEval = new AggregateableEvaluation(evals.get(0));
    for (Evaluation e : evals) {
      aggEval.aggregate(e);
    }

    return aggEval;
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

    try {
      // Make sure that we save out to a subdirectory of the output
      // directory
      String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
      outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);
      if (!DistributedJobConfig.isEmpty(m_optionalOutputSubDir)) {
        outputPath =
          addSubdirToPath(outputPath,
            environmentSubstitute(m_optionalOutputSubDir));
      }

      String classifierMapTaskOptions =
        environmentSubstitute(m_classifierJob.getClassifierMapTaskOptions());
      String[] cOpts = Utils.splitOptions(classifierMapTaskOptions);
      int totalFolds = 1;
      String numFolds = Utils.getOption("total-folds", cOpts.clone());
      if (!DistributedJobConfig.isEmpty(numFolds)) {
        totalFolds = Integer.parseInt(numFolds);
      }

      if (totalFolds > 1
        && !DistributedJobConfig.isEmpty(getSeparateTestSetDataSource())) {
        throw new DistributedWekaException(
          "Total folds is > 1 and a separate test set "
            + "has been specified - can only perform one or the other out "
            + "of a cross-validation or separate test set evaluation");
      }

      String seed = Utils.getOption("seed", cOpts);

      if (totalFolds < 1) {
        throw new DistributedWekaException("Total folds can't be less than 1!");
      }

      JavaRDD<Instance> dataSet = null;
      Instances headerWithSummary = null;
      if (getDataset(TRAINING_DATA) != null) {
        dataSet = getDataset(TRAINING_DATA).getRDD();
        headerWithSummary = getDataset(TRAINING_DATA).getHeaderWithSummary();
        logMessage("RDD<Instance> dataset provided: "
          + dataSet.partitions().size() + " partitions.");
        logMessage("Current caching strategy: " + getCachingStrategy());
      }

      if (dataSet == null) {
        // Run the ARFF job if necessary
        logMessage("Invoking ARFF Job...");
        m_classifierJob.m_arffHeaderJob.setEnvironment(m_env);
        m_classifierJob.m_arffHeaderJob.setLog(getLog());
        m_classifierJob.m_arffHeaderJob
          .setStatusMessagePrefix(m_statusMessagePrefix);
        success =
          m_classifierJob.m_arffHeaderJob.runJobWithContext(sparkContext);
        setCachingStrategy(m_classifierJob.m_arffHeaderJob.getCachingStrategy());

        if (!success) {
          setJobStatus(JobStatus.FAILED);
          statusMessage("Unable to continue - creating the ARFF header failed!");
          logMessage("Unable to continue - creating the ARFF header failed!");
          return false;
        }

        Dataset d = m_classifierJob.m_arffHeaderJob.getDataset(TRAINING_DATA);
        headerWithSummary = d.getHeaderWithSummary();
        dataSet = d.getRDD();
        setDataset(TRAINING_DATA, d);
        logMessage("Fetching RDD<Instance> dataset from ARFF job: "
          + dataSet.partitions().size() + " partitions.");
      }

      Instances headerNoSummary =
        CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
      String classAtt = "";
      if (!DistributedJobConfig.isEmpty(m_classifierJob.getClassAttribute())) {
        classAtt = environmentSubstitute(m_classifierJob.getClassAttribute());
      }
      WekaClassifierSparkJob.setClassIndex(classAtt, headerNoSummary, true);

      if (m_classifierJob.getRandomizeAndStratify()
      /* && !m_classifierJob.getSerializedInput() */) {
        m_classifierJob.m_randomizeSparkJob.setEnvironment(m_env);
        m_classifierJob.m_randomizeSparkJob
          .setDefaultToLastAttIfClassNotSpecified(true);
        m_classifierJob.m_randomizeSparkJob
          .setStatusMessagePrefix(m_statusMessagePrefix);
        m_classifierJob.m_randomizeSparkJob.setLog(getLog());
        m_classifierJob.m_randomizeSparkJob
          .setCachingStrategy(getCachingStrategy());
        m_classifierJob.m_randomizeSparkJob.setDataset(TRAINING_DATA,
          new Dataset(dataSet, headerWithSummary));

        // make sure the random seed gets in there from the setting in the
        // underlying
        // classifier map task
        try {
          String[] classifierOpts =
            Utils.splitOptions(classifierMapTaskOptions);
          String seedS = Utils.getOption("seed", classifierOpts);
          if (!DistributedJobConfig.isEmpty(seedS)) {
            seedS = environmentSubstitute(seedS);
            m_classifierJob.m_randomizeSparkJob.setRandomSeed(seedS);
          }
        } catch (Exception ex) {
          logMessage(ex);
          ex.printStackTrace();
        }

        if (!m_classifierJob.m_randomizeSparkJob
          .runJobWithContext(sparkContext)) {
          statusMessage("Unable to continue - randomization/stratification of input data failed!");
          logMessage("Unable to continue - randomization/stratification of input data failed!");
          return false;
        }

        Dataset d =
          m_classifierJob.m_randomizeSparkJob.getDataset(TRAINING_DATA);
        dataSet = d.getRDD();
        headerWithSummary = d.getHeaderWithSummary();
        setDataset(TRAINING_DATA, d);
      }
      // clean the output directory
      SparkJob.deleteDirectory(outputPath);

      // TODO preconstructed filters (third argument)
      Classifier[] foldClassifiers =
        phaseOneBuildClassifiers(dataSet, headerNoSummary, null);

      // separate test set?
      Dataset testDataset =
        getEvaluateOnTestSetIfPresent() ? getDataset(TEST_DATA) : null;
      JavaRDD<Instance> testDatasetRDD =
        testDataset != null ? testDataset.getRDD() : null;
      if (testDatasetRDD != null) {
        logMessage("RDD<Instance> test dataset provided: "
          + testDatasetRDD.partitions().size() + " partitions.");
      } else {
        logMessage("No separate test set provided - using training data");
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
          rowHelper.setOptions(Utils
            .splitOptions(m_classifierJob.m_arffHeaderJob
              .getCsvToArffTaskOptions()));
        } catch (Exception ex) {
          throw new DistributedWekaException(ex);
        }
        Dataset testD = m_testSetDS.getDataset(Dataset.TEST_DATA);
        testD.setHeaderWithSummary(headerWithSummary);
        testD
          .materializeRDD(m_testSetDS.getCachingStrategy(), rowHelper, false);

        // transfer the dataset over to this job's dataset manager
        m_datasetManager.addAll(m_testSetDS.getDatasetIterator());
        testDatasetRDD =
          m_datasetManager.getDataset(Dataset.TEST_DATA).getRDD();
      }

      Evaluation results =
        phaseTwoEvaluateClassifiers(testDatasetRDD != null ? testDatasetRDD
          : dataSet, headerWithSummary, headerNoSummary, foldClassifiers);

      storeAndWriteEvalResults(results, headerNoSummary, totalFolds,
        testDatasetRDD != null, seed, outputPath);

    } catch (Exception ex) {
      logMessage(ex);
      throw new DistributedWekaException(ex);
    }
    setJobStatus(JobStatus.FINISHED);

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
          + m_classifierJob.getClassifierMapTaskOptions()
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

      String arffOutputPath =
        outputPath
          + (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
          + "evaluation.arff";
      writer = openTextFileForWrite(arffOutputPath);
      writer.println(asInstances.toString());
      m_evalResults = asInstances;

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
  public Instances getInstances() {
    return m_evalResults;
  }

  @Override
  public String getText() {
    return m_textEvalResults;
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {
    if (!(toRun instanceof WekaClassifierEvaluationSparkJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a WekaClassifierEvaluationSparkJob!");
    }

    try {
      WekaClassifierEvaluationSparkJob job =
        (WekaClassifierEvaluationSparkJob) toRun;
      if (Utils.getFlag('h', options)) {
        String help = DistributedJob.makeOptionsStr(job);
        System.err.println(help);
        System.exit(1);
      }

      job.setOptions(options);
      job.runJob();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
