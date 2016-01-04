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
 *    RandomizedDataSparkJob
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;
import weka.core.Attribute;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NominalStats;
import weka.core.stats.NumericStats;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * Job for creating randomly shuffled (and stratified if a nominal class is set)
 * RDDs. Can also persist the shuffled splits as either binary object files or
 * CSV.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class RandomizedDataSparkJob extends SparkJob implements
  CommandlineRunnable {

  /** The name of the subdir in the output directory for this job */
  public static final String OUTPUT_SUBDIR = "randomized";

  /** For serialization */
  private static final long serialVersionUID = -8150037090117009707L;

  /** ARFF job */
  protected ArffHeaderSparkJob m_arffHeaderJob = new ArffHeaderSparkJob();

  /** Holds the options to the header task */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /** No output to the file system by default */
  protected boolean m_writeRandomizedDataToOutput;

  /** The number of randomly shuffled splits to create */
  protected String m_numSplits = "10";

  /** random number seed option */
  protected String m_seed = "1";

  /** class attribute (for stratification) */
  protected String m_classAttribute = "";

  /**
   * If true then assume the last attribute is the class (when
   * not explicitly set)
   */
  protected boolean m_defaultToLastAttIfClassNotSpecified;

  /**
   * Holds the randomized and stratified RDD resulting from this job if the
   * input data does not contain string or relational attributes
   */
  protected JavaRDD<Instance> m_sortedByFold;

  /** Holds the final path for the randomly shuffled data chunks */
  protected String m_finalOutputDirectory = "";

  // TODO make an option to set the number slices based on the requested number
  // of instances per slice (perhaps this should be a general SparkJob option?).
  // if this gets used then the client will need to get the final number of
  // slices from us (so that subsequent jobs are in sync with the randomized
  // partitions created by this job)
  /**
   * Number of instances per data slice (determines how many data slices are
   * created)
   */
  protected String m_numInstancesPerInputSlice = "";

  /**
   * True if the output directory should be deleted first (doing so will force
   * this job to run in the case where there are already chunk files present in
   * the output directory)
   */
  // TODO - might revisit sometime if we can work out
  // how to force the number of partitions in an RDD
  // to be equal to the number files read from a directory.
  // Otherwise, our carefully shuffled and stratified splits
  // are likely to get split up further
  // protected boolean m_cleanOutputDir;

  /**
   * Whether to save randomly shuffled/stratified splits in text rather than
   * serialized object form
   */
  protected boolean m_saveTextualSplits;

  /**
   * Constructor
   */
  public RandomizedDataSparkJob() {
    super("Randomized and stratified split job",
      "Randomize and stratifify the input data");
  }

  public static void main(String[] args) {
    RandomizedDataSparkJob rdj = new RandomizedDataSparkJob();
    rdj.run(rdj, args);
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    /*
     * result .add(new Option(
     * "\tClean output directory. Forces the job to run " +
     * "in the case where the output directory\n\talready exists and is populated "
     * + "with chunk files", "clean", 0, "-clean"));
     */

    result.add(new Option(
      "\tNumber of shuffled folds/splits to create (default = 10)",
      "num-splits", 1, "-num-splits <integer>"));

    result.add(new Option("\tClass index (1-based) or class attribute name "
      + "(default = last attribute).", "class", 1, "-class <index or name>"));

    result.add(new Option("\tRandom seed (default = 1)", "seed", 1,
      "-seed <integer>"));

    result.add(new Option(
      "\tOutput randomly shuffled/stratified split files to "
        + "\n\tthe output directory.", "output", 0, "-output"));

    result.add(new Option(
      "\tPersist split files as text (CSV) instead of binary."
        + "\n\tHas no affect if -output has been not been specified.",
      "output-csv", 0, "-output-csv"));

    result.add(new Option("", "", 0,
      "\nOptions specific to ARFF training header creation:"));

    ArffHeaderSparkJob tempArffJob = new ArffHeaderSparkJob();
    Enumeration<Option> arffOpts = tempArffJob.listOptions();
    while (arffOpts.hasMoreElements()) {
      result.add(arffOpts.nextElement());
    }

    return result.elements();
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

    return options.toArray(new String[options.size()]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    String numInstancesPerSplit =
      Utils.getOption("num-instances-per-slice", options);
    setNumInstancesPerShuffledSplit(numInstancesPerSplit);

    setNumRandomlyShuffledSplits(Utils.getOption("num-splits", options));

    String className = Utils.getOption("class", options);
    setClassAttribute(className);

    String seed = Utils.getOption("seed", options);
    setRandomSeed(seed);

    // setCleanOutputDirectory(Utils.getFlag("clean", options));

    setWriteSplitsToOutput(Utils.getFlag("output", options));
    setPersistSplitFilesAsCSV(Utils.getFlag("output-csv", options));

    // copy the options at this point so that we can set
    // general configuration for the ARFF header
    // job
    String[] optionsCopy = options.clone();

    super.setOptions(options);

    // options for the ARFF header job
    m_arffHeaderJob.setOptions(optionsCopy);
    String optsToCSVTask = Utils.joinOptions(m_arffHeaderJob.getOptions());
    if (!DistributedJobConfig.isEmpty(optsToCSVTask)) {
      setCSVMapTaskOptions(optsToCSVTask);
    }
  }

  public String[] getJobOptionsOnly() {
    List<String> options = new ArrayList<String>();

    /*
     * if (getCleanOutputDirectory()) { options.add("-clean"); }
     */

    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      options.add("-class");
      options.add(getClassAttribute());
    }

    if (!DistributedJobConfig.isEmpty(getRandomSeed())) {
      options.add("-seed");
      options.add(getRandomSeed());
    }

    if (!DistributedJobConfig.isEmpty(getNumRandomlyShuffledSplits())) {
      options.add("-num-splits");
      options.add(getNumRandomlyShuffledSplits());
    }

    if (!DistributedJobConfig.isEmpty(getNumInstancesPerShuffledSplit())) {
      options.add("-num-instances-per-slice");
      options.add(getNumInstancesPerShuffledSplit());
    }

    if (getWriteSplitsToOutput()) {
      options.add("-output");
    }

    if (getPersistSplitFilesAsCSV()) {
      options.add("-output-csv");
    }

    return options.toArray(new String[options.size()]);
  }

  public boolean getDontDefaultToLastAttIfClassNotSpecified() {
    return m_defaultToLastAttIfClassNotSpecified;
  }

  public void setDefaultToLastAttIfClassNotSpecified( boolean d ) {
    m_defaultToLastAttIfClassNotSpecified = d;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String numRandomlyShuffledSplitsTipText() {
    return "The number of randomly shuffled splits to create";
  }

  /**
   * Get the number of randmly shuffled splits to make (if randomly shuffling
   * the data)
   *
   * @return the number of randomly shuffled splits to make
   */
  public String getNumRandomlyShuffledSplits() {
    return m_numSplits;
  }

  /**
   * Set the number of randomly shuffled splits to make (if randomly shuffling
   * the data)
   *
   * @param s the number of randomly shuffled splits to make
   */
  public void setNumRandomlyShuffledSplits(String s) {
    m_numSplits = s;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String writeSplitsToOutputTipText() {
    return "Output shuffled/stratified split files?";
  }

  /**
   * Get whether to output randomly shuffled split files
   * 
   * @return true if splits are to be output
   */
  public boolean getWriteSplitsToOutput() {
    return m_writeRandomizedDataToOutput;
  }

  /**
   * Set whether to output randomly shuffled split files
   *
   * @param d true if splits are to be output
   */
  public void setWriteSplitsToOutput(boolean d) {
    m_writeRandomizedDataToOutput = d;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String persistSplitFilesAsCSV() {
    return "Persist randomly shuffled split files in textual CSV form instead of binary";
  }

  /**
   * Get whether to persist randomly shuffled data chunks as CSV files rather
   * than serialized objects
   * 
   * @return true if data chunks should be persisted as CSV rather than object
   *         files
   */
  public boolean getPersistSplitFilesAsCSV() {
    return m_saveTextualSplits;
  }

  /**
   * Set whether to persist randomly shuffled data chunks as CSV files rather
   * than serialized objects
   *
   * @param s true if data chunks should be persisted as CSV rather than object
   *          files
   */
  public void setPersistSplitFilesAsCSV(boolean s) {
    m_saveTextualSplits = s;
  }

  /**
   * Tip text for this property
   *
   * @return tip text for this property
   */
  public String numInstancesPerShuffledSplitTipText() {
    return "The number of instances that each randomly shuffled data split (partition) "
      + "should contain. If not specified then the number of instances per chunk"
      + "is determined by the min-slices setting";
  }

  /**
   * Get the number of instances that each randomly shuffled data chunk should
   * have.
   *
   * @return the number of instances that each randomly shuffled data chunk
   *         should contain
   */
  public String getNumInstancesPerShuffledSplit() {
    return m_numInstancesPerInputSlice;
  }

  /**
   * Set the number of instances that each randomly shuffled data chunk should
   * have.
   *
   * @param insts the number of instances that each randomly shuffled data chunk
   *          should contain
   */
  public void setNumInstancesPerShuffledSplit(String insts) {
    m_numInstancesPerInputSlice = insts;
  }

  /**
   * Tip text for this property
   *
   * @return tip text for this property
   */
  /*
   * public String cleanOutputDirectoryTipText() { return
   * "Set to true to delete any existing output directory and force this job to run"
   * ; }
   */

  /**
   * Get whether to blow away the output directory before running. If an output
   * directory exists (and is populated with chunk files) then deleting this
   * prior to running will force the job to run.
   * 
   * @return true if the output directory should be deleted before first (thus
   *         forcing the job to run if there was a populated output directory
   *         already).
   */
  /*
   * public boolean getCleanOutputDirectory() { return m_cleanOutputDir; }
   */

  /**
   * Set whether to blow away the output directory before running. If an output
   * directory exists (and is populated with chunk files) then deleting this
   * prior to running will force the job to run.
   *
   * @param clean true if the output directory should be deleted before first
   *          (thus forcing the job to run if there was a populated output
   *          directory already).
   */
  /*
   * public void setCleanOutputDirectory(boolean clean) { m_cleanOutputDir =
   * clean; }
   */

  /**
   * Tip text for this property
   *
   * @return tip text for this property
   */
  public String randomSeedTipText() {
    return "The random seed to use when randomly shuffling the data";
  }

  /**
   * Get the random seed for shuffling the data
   * 
   * @return the random seed to use
   */
  public String getRandomSeed() {
    return m_seed;
  }

  /**
   * Set the random seed for shuffling the data
   *
   * @param seed the random seed to use
   */
  public void setRandomSeed(String seed) {
    m_seed = seed;
  }

  /**
   * Tip text for this property
   *
   * @return tip text for this property
   */
  public String classAttributeTipText() {
    return "The name or index of the class attribute. 'first' and "
      + "'last' may also be used. If set, and the class is nominal, "
      + "then output chunk files will be stratified.";
  }

  /**
   * Get the name or index of the class attribute ("first" and "last" can also
   * be used)
   * 
   * @return the name or index of the class attribute
   */
  public String getClassAttribute() {
    return m_classAttribute;
  }

  /**
   * Set the name or index of the class attribute ("first" and "last" can also
   * be used)
   *
   * @param c the name or index of the class attribute
   */
  public void setClassAttribute(String c) {
    m_classAttribute = c;
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
   * Perform the randomization (and stratification) in the case where the input
   * data does not contain string or relational attributes. In this case, our
   * final RDD can contain instances objects, which will avoid further parsing
   * in subsequent jobs.
   *
   * @param input
   * @param outputPath
   * @param numFoldSlices
   * @param random
   * @param headerWithSummary
   * @param classIndex the classIndex to use
   * @throws IOException
   * @throws DistributedWekaException
   */
  protected void performRandomShuffle(JavaRDD<Instance> input,
    String outputPath, final int numFoldSlices, final Random random,
    final Instances headerWithSummary, int classIndex) throws IOException,
    DistributedWekaException {

    final Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
    headerNoSummary.setClassIndex(classIndex);

    boolean containsStringOrRelational =
      headerNoSummary.checkForStringAttributes()
        || headerNoSummary.checkForAttributeType(Attribute.RELATIONAL);

    final PhaseOneRandomization phaseOne =
      new PhaseOneRandomization(headerNoSummary, getCSVMapTaskOptions(),
        containsStringOrRelational, random, numFoldSlices);

    // Phase 1 - randomly shuffle the data
    logMessage("[Randomly shuffle data] Starting phase 1...");
    JavaPairRDD<Integer, Object> mapResults =
      input.mapPartitionsToPair(
        new PairFlatMapFunction<Iterator<Instance>, Integer, Object>() {

          /** For serialization */
          private static final long serialVersionUID = -5351850875358513817L;

          protected List<Tuple2<Integer, Object>> m_randomizedRows =
            new ArrayList<Tuple2<Integer, Object>>();

          // protected CSVToARFFHeaderMapTask m_rowHelper;

          @Override
          public Iterable<Tuple2<Integer, Object>>
            call(Iterator<Instance> split) throws IOException,
              DistributedWekaException {

            while (split.hasNext()) {
              Instance row = split.next();

              Tuple2<Integer, Object> processed = phaseOne.process(row);
              m_randomizedRows.add(processed);
            }

            // System.err.println("****** Number in partition: " + m_count);
            return m_randomizedRows;
          }
        }).persist(getCachingStrategy().getStorageLevel());

    // Now sort into ascending order of random assignment number
    JavaPairRDD<Integer, Object> sortedByAssignment =
      mapResults.sortByKey(true)
        .partitionBy(new IntegerKeyPartitioner(numFoldSlices))
        .persist(getCachingStrategy().getStorageLevel());
    sortedByAssignment.count();

    // discard mapResults
    mapResults.unpersist();
    mapResults = null;

    // List<Tuple2<Integer, Object>> tmpData = sortedByAssignment.collect();
    // for (Tuple2<Integer, Object> row : tmpData) {
    // ((Instance) row._2()).setDataset(headerNoSummary);
    // System.err.println(row._1() + ": " + row._2().toString());
    // }

    if (headerNoSummary.classIndex() < 0
      || headerNoSummary.classAttribute().isNumeric()) {

      // No need for the second phase of dealing classes out to splits
      // if there is no class or a numeric class
      // m_sortedByFold = sortedByAssignment;
      // , true here because we preserve the partitions from sortedByAssignment
      JavaRDD<Instance> finalDataSet =
        sortedByAssignment.mapPartitions(
          new FlatMapFunction<Iterator<Tuple2<Integer, Object>>, Instance>() {

            /**
             * For serialization
             */
            private static final long serialVersionUID = -4129157509045217459L;
            List<Instance> m_list = new ArrayList<Instance>();

            @Override
            public Iterable<Instance> call(
              Iterator<Tuple2<Integer, Object>> split) {
              while (split.hasNext()) {

                // make sure each instance has a reference to the header
                Instance nextI = (Instance) split.next()._2();
                nextI.setDataset(headerNoSummary);
                m_list.add(nextI);
              }

              return m_list;
            }
          }, true).persist(getCachingStrategy().getStorageLevel());

      finalDataSet.count(); // materialize this RDD

      logMessage("[Randomly shuffle data] Unpersisting sorted phase 1 RDD");
      sortedByAssignment.unpersist();
      sortedByAssignment = null;

      m_sortedByFold = finalDataSet;

    } else {
      // phase 2 - deal classes out to splits + oversample minority classes
      final PhaseTwoStratification phaseTwo =
        new PhaseTwoStratification(headerNoSummary, numFoldSlices, false);

      logMessage("[Randomly shuffle data] Starting phase 2 (deal to folds/stratification)...");
      JavaPairRDD<Integer, Object> dealtToFolds =
        sortedByAssignment
          .mapPartitionsToPair(
            new PairFlatMapFunction<Iterator<Tuple2<Integer, Object>>, Integer, Object>() {

              /**
               * For serialization
               */
              private static final long serialVersionUID =
                -5903374381393577497L;

              protected List<Tuple2<Integer, Object>> m_dealtRows =
                new ArrayList<Tuple2<Integer, Object>>();

              @Override
              public Iterable<Tuple2<Integer, Object>> call(
                Iterator<Tuple2<Integer, Object>> split) {

                while (split.hasNext()) {
                  Tuple2<Integer, Object> current = split.next();
                  Tuple2<Integer, Object> result = phaseTwo.process(current._2);

                  m_dealtRows.add(result);
                }

                phaseTwo.checkForMinorityClassCases(m_dealtRows);

                return m_dealtRows;
              }
            }).persist(getCachingStrategy().getStorageLevel());

      // discard sortedByAssignment

      logMessage("[Randomly shuffle data] Repartitioning phase 2 RDD according to fold number");
      JavaPairRDD<Integer, Object> tmpSortedByFold =
        dealtToFolds.sortByKey()
          .partitionBy(new IntegerKeyPartitioner(numFoldSlices))
          .persist(getCachingStrategy().getStorageLevel());

      tmpSortedByFold.count();

      sortedByAssignment.unpersist();
      sortedByAssignment = null;
      dealtToFolds.unpersist();
      dealtToFolds = null;

      // writeRandomizedSplits(outputPath, dealtToFolds);

      // List<Tuple2<Integer, Object>> tmpData = dealtToFolds.collect();
      // for (Tuple2<Integer, Object> row : tmpData) {
      // ((Instance) row._2()).setDataset(headerNoSummary);
      // System.err.println(row._1() + ": " + row._2().toString());
      // }

      // m_sortedByFold = dealtToFolds.sortByKey(true);
      logMessage("[Randomly shuffle data] Creating and persisting final dataset (RDD<Instance>)...");
      JavaRDD<Instance> finalDataSet = null;

      if (!containsStringOrRelational) {
        finalDataSet =
          tmpSortedByFold.mapPartitions(
            new FlatMapFunction<Iterator<Tuple2<Integer, Object>>, Instance>() {

              /**
               * For serialization
               */
              private static final long serialVersionUID = 5425826829981136102L;
              List<Instance> m_list = new ArrayList<Instance>();

              @Override
              public Iterable<Instance> call(
                Iterator<Tuple2<Integer, Object>> split) {
                while (split.hasNext()) {

                  // make sure that each instance has access to the header
                  Instance nextI = (Instance) split.next()._2();
                  nextI.setDataset(headerNoSummary);
                  m_list.add(nextI);
                }

                return m_list;
              }
            }, true).persist(getCachingStrategy().getStorageLevel());
      } else {
        CSVToInstancePairFlatMapFunction instanceFunction =
          new CSVToInstancePairFlatMapFunction(headerNoSummary,
            getCSVMapTaskOptions());

        // , true here because we preserve the partitions from tmpSortedByFold
        finalDataSet =
          tmpSortedByFold.mapPartitions(instanceFunction, true).persist(
            getCachingStrategy().getStorageLevel());
      }

      logMessage("[Randomly shuffle data] forcing materialization of final shuffled data");
      finalDataSet.count();

      logMessage("[Randomly shuffle data] Unpersisting intermediate RDDs");

      tmpSortedByFold.unpersist();
      tmpSortedByFold = null;

      m_sortedByFold = finalDataSet;
      logMessage("[Randomly shuffle data] Finished shuffling/stratifying RDD. Number of partitions: "
        + m_sortedByFold.partitions().size());
    }

    setDataset(TRAINING_DATA, new Dataset<Instance>(m_sortedByFold, headerWithSummary));

    if (m_writeRandomizedDataToOutput) {
      writeRandomizedSplits(outputPath, m_sortedByFold);
    }
  }

  // TODO we might remove this entire mechanism
  protected void loadShuffledDataFiles(String outputDir,
    JavaSparkContext sparkContext, Instances headerNoSummary, int minSlices)
    throws IOException, DistributedWekaException {

    // update caching strategy based on shuffled data...
    CachingStrategy newStrategy =
      new CachingStrategy(outputDir, m_sjConfig.getAvailableClusterMemory(),
        m_sjConfig.getInMemoryDataOverheadFactor(), sparkContext);
    setCachingStrategy(newStrategy);
    logMessage("[Randomly shuffle data] Setting caching strategy to: "
      + getCachingStrategy().getStorageLevel().description());

    if (getPersistSplitFilesAsCSV()) {
      m_sortedByFold =
        loadCSVFile(outputDir, headerNoSummary, getCSVMapTaskOptions(),
          sparkContext, getCachingStrategy(), -1, true);
    } else {
      m_sortedByFold =
        loadInstanceObjectFile(outputDir, sparkContext, getCachingStrategy(),
          minSlices, true);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    m_currentContext = sparkContext;
    setJobStatus(JobStatus.RUNNING);
    boolean success = false;

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    // Make sure that we save out to a subdirectory of the output
    // directory
    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);

    JavaRDD<Instance> inputData = null;
    Instances headerWithSummary = null;
    if (getDataset(TRAINING_DATA) != null) {
      inputData = (JavaRDD<Instance>)getDataset(TRAINING_DATA).getDataset();
      headerWithSummary = getDataset(TRAINING_DATA).getHeaderWithSummary();
      logMessage("RDD<Instance> dataset provided: "
        + inputData.partitions().size() + " partitions.");
    }

    if (inputData == null && headerWithSummary == null) {
      logMessage("[Randomly shuffle data] Invoking ARFF Job...");
      m_arffHeaderJob.setEnvironment(m_env);
      m_arffHeaderJob.setLog(getLog());
      m_arffHeaderJob.setStatusMessagePrefix(m_statusMessagePrefix);
      m_arffHeaderJob.setCachingStrategy(getCachingStrategy());

      // header job necessary?
      success = m_arffHeaderJob.runJobWithContext(sparkContext);

      if (!success) {
        setJobStatus(JobStatus.FAILED);
        statusMessage("Unable to continue - creating the ARFF header failed!");
        logMessage("[Randomly shuffle data] Unable to continue - creating the ARFF header failed!");
        return false;
      }

      Dataset d = m_arffHeaderJob.getDataset(TRAINING_DATA);

      headerWithSummary = d.getHeaderWithSummary();
      inputData = (JavaRDD<Instance>)d.getDataset();
      logMessage("Fetching RDD<Instance> dataset from ARFF job: "
        + inputData.partitions().size() + " partitions.");
    }

    /*
     * // TODO revisit at some stage... Current assumption: if you // have
     * output from this job as serialized instances then you // are happy with
     * the shuffling and will not want to re-shuffle if
     * (m_sjConfig.getSerializedInput()) { throw new DistributedWekaException(
     * "Randomly shuffling serialized Instance " +
     * "input is not supported yet."); }
     */

    // clean the output directory
    SparkJob.deleteDirectory(outputPath);

    int seed = 1;
    if (!DistributedJobConfig.isEmpty(getRandomSeed())) {
      seed = Integer.parseInt(environmentSubstitute(getRandomSeed()));
    }
    final Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    try {
      WekaClassifierSparkJob.setClassIndex(
        environmentSubstitute(m_classAttribute), headerNoSummary,
        m_defaultToLastAttIfClassNotSpecified );

    } catch (Exception e) {
      logMessage(e);
      throw new DistributedWekaException(e);
    }

    // find summary attribute for class (if set, otherwise just use the first
    // numeric on nominal attribute). We're using this simply to find out
    // the total number of instances in the dataset
    String className = null;
    if (headerNoSummary.classIndex() >= 0) {
      className = headerNoSummary.classAttribute().name();
    } else {
      for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
        if (headerNoSummary.attribute(i).isNumeric()
          || headerNoSummary.attribute(i).isNominal()) {
          className = headerNoSummary.attribute(i).name();
          break;
        }
      }
    }
    Attribute summaryClassAtt =
      headerWithSummary
        .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
          + className);
    if (summaryClassAtt == null) {
      throw new DistributedWekaException(
        "Was unable to find the summary attribute for " + "the class: "
          + className);
    }

    int totalNumInstances = 0;
    int numFoldSlices = 10;
    // summary attribute for getting the total number of instances
    Attribute summaryAttOrig = null;
    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      if (headerNoSummary.attribute(i).isNumeric()
        || headerNoSummary.attribute(i).isNominal()) {
        summaryAttOrig = headerNoSummary.attribute(i);
        break;
      }
    }

    if (summaryAttOrig == null) {
      throw new DistributedWekaException("Was unable to find a summary attribute for "
        + "deterimining the total number of instances in the dataset");
    }

    String summaryName = summaryAttOrig.name();
    Attribute summaryAtt =
      headerWithSummary
        .attribute(
          CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX + summaryName);
    if (summaryAtt == null) {
      logMessage("[RandomizedDataSparkJob] Was unable to find the summary "
        + "attribute for attribute: " + summaryName);
      throw new DistributedWekaException("Was unable to find the summary "
        + "attribute for attribute: " + summaryName);
    }

    if (summaryAttOrig.isNominal()) {
      NominalStats stats = NominalStats.attributeToStats(summaryAtt);
      for (String label : stats.getLabels()) {
        totalNumInstances += stats.getCount(label);
      }
    } else {
      NumericStats stats = NumericStats.attributeToStats(summaryAtt);
      totalNumInstances =
        (int) stats.getStats()[ArffSummaryNumericMetric.COUNT.ordinal()];
    }

    if (DistributedJobConfig.isEmpty(getNumRandomlyShuffledSplits())
      && DistributedJobConfig.isEmpty(getNumInstancesPerShuffledSplit())) {
      logMessage("[RandomizedDataSparkJob] Must specify either the number of "
        + "splits or the number of instances per split");
      throw new DistributedWekaException("Must specify either the number of "
        + "splits or the number of instances per split");
    }

    if (!DistributedJobConfig.isEmpty(getNumRandomlyShuffledSplits())) {
      numFoldSlices =
        Integer.parseInt(environmentSubstitute(getNumRandomlyShuffledSplits()));
    } else {
      int numInsts = 0;
      try {
        numInsts =
          Integer
            .parseInt(environmentSubstitute(getNumInstancesPerShuffledSplit()));
      } catch (NumberFormatException ex) {
        throw new DistributedWekaException(ex);
      }

      if (numInsts <= 0) {
        throw new DistributedWekaException(
          "Number of instances per split must " + "be > 0");
      }

      if (numInsts > totalNumInstances) {
        throw new DistributedWekaException("Can't have more instances per split "
          + "than there are instances in the dataset!");
      }
      double nc = (double) totalNumInstances / numInsts;
      nc = Math.ceil(nc);
      numFoldSlices = (int) nc;
    }
    logMessage("[Randomly shuffle data] creating " + numFoldSlices + " splits.");

    if (headerNoSummary.attribute(className).isNominal()) {
      NominalStats stats = NominalStats.attributeToStats(summaryClassAtt);
      for (String label : stats.getLabels()) {
        totalNumInstances += stats.getCount(label);
      }
    } else {
      NumericStats stats = NumericStats.attributeToStats(summaryClassAtt);
      totalNumInstances =
        (int) stats.getStats()[ArffSummaryNumericMetric.COUNT.ordinal()];
    }

    logMessage("[Randomly shuffle data] Num slices = " + numFoldSlices);

    final Random random = new Random(seed);
    for (int i = 0; i < 20; i++) {
      random.nextInt();
    }

    performRandomShuffle(inputData, outputPath, numFoldSlices, random,
      headerWithSummary, headerNoSummary.classIndex());

    setJobStatus(JobStatus.FINISHED);

    return true;
  }

  /**
   * Writes the randomized splits out to the output directory. Spark ensures
   * that each key goes to a separate part-xxxxx file in the output.
   *
   * @param outputPath the path to write to
   * @param sortedByFold the RDD containing the sorted dataset keyed by fold
   * @throws IOException if a problem occurs
   */
  protected void writeRandomizedSplits(String outputPath,
    JavaRDD<Instance> sortedByFold) throws IOException {
    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(outputPath, pathOnly);

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);
    p = p.makeQualified(fs);

    if (!p.toString().toLowerCase().startsWith("file:/")) {
      m_finalOutputDirectory = p.toString();
    } else {
      m_finalOutputDirectory = pathOnly[0];
    }

    if (m_saveTextualSplits) {
      sortedByFold.saveAsTextFile(m_finalOutputDirectory);
    } else {
      sortedByFold.saveAsObjectFile(m_finalOutputDirectory);
    }
    // sortedByFold.saveAsNewAPIHadoopFile(pathOnly[0], NullWritable.class,
    // Text.class, SparkJob.NoKeyTextOutputFormat.class, conf);

  }

  protected void writeRandomizedSplits(String outputPath,
    JavaPairRDD<Integer, Object> sortedByFold) throws IOException {
    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(outputPath, pathOnly);

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);
    p = p.makeQualified(fs);

    if (!p.toString().toLowerCase().startsWith("file:/")) {
      m_finalOutputDirectory = p.toString();
    } else {
      m_finalOutputDirectory = pathOnly[0];
    }

    // sortedByFold.saveAsNewAPIHadoopFile(pathOnly[0], NullWritable.class,
    // Text.class, SparkJob.NoKeyTextOutputFormat.class, conf);
    //
    sortedByFold.saveAsNewAPIHadoopFile(pathOnly[0], IntWritable.class,
      Text.class, TextOutputFormat.class, conf);
  }

  /**
   * Get the randomized (and potentially stratified) RDD resulting from this job
   *
   * @return the randomly shuffled (and stratified) RDD
   */
  public JavaRDD<Instance> getRandomizedStratifiedRDD() {
    // return m_sortedByFold.persist(StorageLevel.MEMORY_AND_DISK());
    return m_sortedByFold;
  }

  public String getFinalOutputDirectory() {

    // TODO adjust this with respect to protocol?? Problem
    // here because RDD.input() doesn't have a version where
    // we can pass in a path + Configuration (thus allowing us
    // to set up relative (to user's home) paths)

    return m_finalOutputDirectory;
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {

    if (!(toRun instanceof RandomizedDataSparkJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a RandomizedDataSparkJob");
    }

    try {
      RandomizedDataSparkJob rdj = (RandomizedDataSparkJob) toRun;
      if (Utils.getFlag('h', options)) {
        String help = DistributedJob.makeOptionsStr(rdj);
        System.err.println(help);
        System.exit(1);
      }

      rdj.setOptions(options);
      rdj.runJob();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Inner class for phase one of the two phase random shuffle and stratify
   * process. Phase one performs the shuffling by assigning each instance to a
   * randomly selected chunk. RDD can then be sorted on chunk number (key) and
   * the IntegerKeyPartitioner used to re-partition according to key.
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class PhaseOneRandomization implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = 3355852963548447505L;

    /** The header of the data (sans summary attributes) */
    protected Instances m_headerNoSummary;

    /** Used for parsing CSV */
    protected CSVToARFFHeaderMapTask m_rowHelper;

    /** Options to the CSV parser */
    protected String m_csvMapTaskOpts = "";

    /** For random number generation */
    protected Random m_random;

    /** Whether to represent instances as strings or not */
    protected boolean m_instancesAsStrings;

    /** Number of folds (splits) of the data */
    protected int m_numFoldSlices;

    /**
     * Constructor
     *
     * @param headerNoSummary the header of the data (sans summary attributes)
     * @param csvMapTaskOpts options for the CSV parser
     * @param instancesAsStrings true if instances are to be kept as string
     *          objects
     * @param random for random number generation
     * @param numFoldSlices number of splits
     */
    public PhaseOneRandomization(final Instances headerNoSummary,
      final String csvMapTaskOpts, boolean instancesAsStrings,
      final Random random, final int numFoldSlices) {
      m_headerNoSummary = headerNoSummary;
      m_csvMapTaskOpts = csvMapTaskOpts;
      m_instancesAsStrings = instancesAsStrings;
      m_random = random;
      m_numFoldSlices = numFoldSlices;
    }

    /**
     * Process a row/instance
     *
     * @param toProcess the row to process
     * @return either a key,string tuple or a key,instance tuple
     * @throws DistributedWekaException if a problem occurs
     * @throws IOException if a problem occurs
     */
    public Tuple2<Integer, Object> process(String toProcess)
      throws DistributedWekaException, IOException {
      if (m_rowHelper == null) {
        m_rowHelper = new CSVToARFFHeaderMapTask();
        try {
          m_rowHelper.setOptions(Utils.splitOptions(m_csvMapTaskOpts));
          m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
            .instanceHeaderToAttributeNameList(m_headerNoSummary));
        } catch (Exception e) {
          throw new DistributedWekaException(e);
        }
      }

      Instance currentI =
        DistributedJob.parseInstance(toProcess, m_rowHelper, m_headerNoSummary,
          m_instancesAsStrings);

      return process(currentI);
    }

    public Tuple2<Integer, Object> process(Instance currentI) {
      int chunkNum = 0;
      if (currentI.classIndex() < 0
        || !currentI.isMissing(currentI.classIndex())) {
        chunkNum = m_random.nextInt(m_numFoldSlices);
      }

      Object instToAdd = null;
      if (m_instancesAsStrings) {
        instToAdd = currentI.toString();

        if (m_headerNoSummary.classIndex() >= 0
          && m_headerNoSummary.classAttribute().isNominal()) {
          instToAdd =
            (instToAdd.toString() + "@:@" + (int) currentI.classValue());
        }
      } else {
        // set the reference to the dataset to null here so that
        // we prevent many copies of the header being created when
        // Spark serializes stuff when we sort
        currentI.setDataset(null);
        instToAdd = currentI;
      }

      Tuple2<Integer, Object> result =
        new Tuple2<Integer, Object>(chunkNum, instToAdd);

      return result;
    }
  }

  /**
   * Inner class for phase two of the random shuffle and stratify process. This
   * implements the stratification process if there is a class set in the data
   * and it is nominal. In all other cases only phase one is needed.
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class PhaseTwoStratification implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = 6969249334933544891L;

    /** Header of the data (sans summary attributes) */
    protected Instances m_headerNoSummary;

    /**
     * True if instances are to remain in string form (rather than Instance
     * object form)
     */
    protected boolean m_instancesAsStrings;

    /** Number of slices/folds/chunks */
    protected int m_numFoldSlices;

    /** Keeps track of the number of instances seen in each class */
    protected int[] m_countsPerClass;

    /** Maintains a buffer for instances from minority classes */
    protected List<List<Object>> m_classInstancesBuffer;

    /** For randomization */
    protected Random m_random = new Random(42);

    public PhaseTwoStratification(Instances headerNoSummary, int numFoldSlices,
      boolean instancesAsStrings) {
      m_headerNoSummary = headerNoSummary;
      m_instancesAsStrings = instancesAsStrings;
      m_numFoldSlices = numFoldSlices;

      m_countsPerClass =
        new int[headerNoSummary.classIndex() < 0
          || headerNoSummary.classAttribute().isNumeric() ? 1 : headerNoSummary
          .classAttribute().numValues()];

      if (headerNoSummary.classIndex() >= 0
        && headerNoSummary.classAttribute().isNominal()) {
        m_classInstancesBuffer = new ArrayList<List<Object>>();
        int numClasses = headerNoSummary.classAttribute().numValues();
        for (int i = 0; i < numClasses; i++) {
          m_classInstancesBuffer.add(new ArrayList<Object>());
        }
      }
    }

    protected int assignToFold(Object toProcess, int classIndex) {
      int fold = m_countsPerClass[classIndex] % m_numFoldSlices;

      m_countsPerClass[classIndex]++;

      // add to the minority class buffers. If we've seen
      // at least m_numberOfDataChunks instances for this class then
      // each data chunk will have at least one instance of this class
      if (m_countsPerClass[classIndex] < m_numFoldSlices) {
        m_classInstancesBuffer.get(classIndex).add(toProcess);
      }
      return fold;
    }

    public Tuple2<Integer, Object> process(Object toProcess) {

      int classVal = 0;
      Object inst = null;
      if (m_instancesAsStrings) {
        inst = toProcess.toString();

        if (m_headerNoSummary.classIndex() >= 0
          && m_headerNoSummary.classAttribute().isNominal()) {
          String[] parts = inst.toString().split("@:@");
          inst = parts[0];
          classVal = Integer.parseInt(parts[1]);
        }
      } else {
        inst = toProcess;

        if (m_headerNoSummary.classIndex() >= 0
          && m_headerNoSummary.classAttribute().isNominal()) {
          classVal =
            (int) ((Instance) inst).value(m_headerNoSummary.classIndex());
          // classVal = (int) ((Instance) inst).classValue();
        }
      }

      int fold = assignToFold(inst, classVal);

      return new Tuple2<Integer, Object>(fold, inst);
    }

    public void
      checkForMinorityClassCases(List<Tuple2<Integer, Object>> toAddTo) {
      // folds that have not received minority class instances?
      for (int i = 0; i < m_countsPerClass.length; i++) {
        if (m_countsPerClass[i] < m_numFoldSlices && m_countsPerClass[i] > 0) {
          while (m_countsPerClass[i] < m_numFoldSlices) {
            int instIndex =
              m_random.nextInt(m_classInstancesBuffer.get(i).size());

            toAddTo.add(new Tuple2<Integer, Object>(m_countsPerClass[i],
              m_classInstancesBuffer.get(i).get(instIndex)));

            m_countsPerClass[i]++;
          }
        }
      }
    }
  }
}
