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
 *    KMeansClustererSparkJob
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.storage.StorageLevel;
import scala.Tuple2;
import weka.clusterers.CentroidSketch;
import weka.clusterers.Clusterer;
import weka.clusterers.PreconstructedFilteredClusterer;
import weka.clusterers.PreconstructedKMeans;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NominalStats;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.KMeansMapTask;
import weka.distributed.KMeansReduceTask;
import weka.filters.Filter;
import weka.gui.beans.ClustererProducer;
import weka.gui.beans.TextProducer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Spark job for training a k-means clusterer with or without the k-means++
 * (kmeans||) initialization procedure
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 12253 $
 */
public class KMeansClustererSparkJob extends SparkJob implements
  CommandlineRunnable, TextProducer, ClustererProducer, OptionHandler {

  public static final String K_MEANS_MODEL = "k-means-model";

  /**
   * The subdirectory of the output directory that this job saves its results to
   */
  protected static final String OUTPUT_SUBDIR = "kmeans";

  /** For serialization */
  private static final long serialVersionUID = -7983704737099141085L;

  /** Name of the output model */
  protected String m_modelName = "outputModel.model";

  /** ARFF header job */
  protected ArffHeaderSparkJob m_arffHeaderJob = new ArffHeaderSparkJob();

  /** Options for the ARFF header job */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /**
   * True if the data is to be randomly shuffled
   */
  protected boolean m_randomize;

  /** Options for the randomize/stratify job */
  protected String m_randomizeJobOpts = "";

  /** Options for the k-means map task */
  protected String m_kMeansMapTaskOpts = "";

  /** Randomize and stratify job */
  protected RandomizedDataSparkJob m_randomizeSparkJob =
    new RandomizedDataSparkJob();

  /** Maximum number of iterations to run */
  protected String m_numIterations = "20";

  /** Perform 10 runs of k-means in parallel */
  protected String m_numRuns = "1";

  /** Number of clusters to find */
  protected String m_numClusters = "2";

  /** Seed for the random number generator */
  protected String m_randomSeed = "1";

  /** Number of iterations for the k-means|| initialization */
  protected String m_kMeansParallelInitSteps = "5";

  /** Close enough to have converged? */
  protected double m_convergenceTolerance = 1e-4;

  /**
   * Whether to display standard deviations of centroids in textual output of
   * final model
   */
  protected boolean m_displayStdDevs;

  /**
   * Holds priming data for distance function (if k-means|| initialization is
   * run)
   */
  protected Instances m_distanceFunctionPrimingData;

  /** Initialize with the k-means parallel routine? */
  protected boolean m_initializeWithRandomCenters;

  /** Holds the final clustering model */
  protected Clusterer m_finalClusterer;

  /** The header (sans summary attributes) used to train the clusterer with */
  protected Instances m_trainingHeader;

  public KMeansClustererSparkJob() {
    super("k-means clusterer job", "Build a k-means clusterer");
  }

  public static void main(String[] args) {
    KMeansClustererSparkJob kcsj = new KMeansClustererSparkJob();
    kcsj.run(kcsj, args);
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
  public String initWithRandomCentroidsTipText() {
    return "Initialize with randomly selected centroids rather than use the "
      + "k-means|| initialization procedure";
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
   * Set whether to initialize with randomly selected centroids rather than
   * using the k-means|| initialization procedure.
   *
   * @param init true if randomly selected initial centroids are to be used
   */
  public void setInitWithRandomCentroids(boolean init) {
    m_initializeWithRandomCenters = init;
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
   * Get the convergence tolerance
   * 
   * @return the convergence tolerance
   */
  public double getConvergenceTolerance() {
    return m_convergenceTolerance;
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
   * @return the tip text for this property
   */
  public String randomlyShuffleDataTipText() {
    return "Randomly shuffle the order of the input data";
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
   * Set whether to randomly shuffle the order of the instances in the input
   * data before clustering
   *
   * @param r true if the data should be randomly shuffled
   */
  public void setRandomlyShuffleData(boolean r) {
    m_randomize = r;
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
   * Get the number of clusters to find
   * 
   * @return the number of clusters to find
   */
  public String getNumClusters() {
    return m_numClusters;
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
   * Tip text for this property.
   *
   * @return the tip text for this property
   */
  public String numRunsTipText() {
    return "The number of k-means runs to perform in parallel (best run is selected as final model)";
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
   * Set the number of k-means runs to perform in parallel
   *
   * @param numRuns the number of k-means runs to perform in parallel
   */
  public void setNumRuns(String numRuns) {
    m_numRuns = numRuns;
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
   * Get the maximum number of k-means iterations to perform
   * 
   * @return the maximum number of iterations to perform
   */
  public String getNumIterations() {
    return m_numIterations;
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
   * Tip text for this property.
   *
   * @return the tip text for this property
   */
  public String randomSeedTipText() {
    return "Seed for random number generation";
  }

  /**
   * Get the seed for random number generation
   * 
   * @return the seed for the random number generator
   */
  public String getRandomSeed() {
    return m_randomSeed;
  }

  /**
   * Set the seed for random number generation
   *
   * @param seed the seed for the random number generator
   */
  public void setRandomSeed(String seed) {
    m_randomSeed = seed;
  }

  public String getKMeansMapTaskOpts() {
    return m_kMeansMapTaskOpts;
  }

  public void setKMeansMapTaskOpts(String opts) {
    m_kMeansMapTaskOpts = opts;
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
   * Get the number of iterations of the k-means|| initialization routine to
   * perform
   * 
   * @return the number of iterations of the k-means|| init routine to perform
   */
  public String getKMeansParallelInitSteps() {
    return m_kMeansParallelInitSteps;
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
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String displayCentroidStdDevsTipText() {
    return "Display centroid standard deviations in textual output of model";
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

    result.add(new Option(
      "\tThe number of k-means|| initialization iterations to perform\n\t"
        + "if initializing with k-means||. (default = 5).", "init-kmeans-its",
      0, "-init-kmeans-its"));

    result.add(new Option("\tDisplay std. deviations for centroids", "V", 0,
      "-V"));

    result.add(new Option("\tRandom seed (default 1).", "seed", 1,
      "-seed <integer>"));

    KMeansMapTask tempMapTask = new KMeansMapTask();
    Enumeration<Option> mapOpts = tempMapTask.listOptions();
    while (mapOpts.hasMoreElements()) {
      result.add(mapOpts.nextElement());
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
    options.add("-init-kmeans-its");
    options.add(getKMeansParallelInitSteps());
    options.add("-seed");
    options.add(getRandomSeed());
    options.add("-tolerance");
    options.add("" + getConvergenceTolerance());

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

  @Override
  public void setOptions(String[] options) throws Exception {
    String modelFileName = Utils.getOption("model-file-name", options);
    if (!DistributedJobConfig.isEmpty(modelFileName)) {
      setModelFileName(modelFileName);
    }

    setRandomlyShuffleData(Utils.getFlag("randomize", options));

    setInitWithRandomCentroids(Utils.getFlag("init-random", options));

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
    temp = Utils.getOption("init-kmeans-its", options);
    if (!DistributedJobConfig.isEmpty(temp)) {
      setKMeansParallelInitSteps(temp);
    }

    temp = Utils.getOption("seed", options);
    if (!DistributedJobConfig.isEmpty(temp)) {
      setRandomSeed(temp);
    }
    temp = Utils.getOption("tolerance", options);
    if (!DistributedJobConfig.isEmpty(temp)) {
      setConvergenceTolerance(Double.parseDouble(temp));
    }

    setDisplayCentroidStdDevs(Utils.getFlag('V', options));

    KMeansMapTask tempKTask = new KMeansMapTask();
    tempKTask.setOptions(options);
    String mapOpts = Utils.joinOptions(tempKTask.getOptions());
    setKMeansMapTaskOpts(mapOpts);

    String[] optionsCopy = options.clone();

    super.setOptions(options);

    // options for the randomize job
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

  /**
   * Perform an iteration of k-means
   *
   * @param dataset the dataset to operate on
   * @param mapTasks the underlying map tasks to use - one for each separate run
   *          of k-means that we're doing in parallel
   * @param converged array indicating which runs have converged
   * @param iterationNum the iteration number that we're up to
   * @param transformedHeaderNoSummary the header of the training data (sans
   *          summary attributes)
   * @return a list of KMeansReduceTasks encapsulating the results of the
   *         iteration for each active run of k-means
   * @throws DistributedWekaException if a problem occurs
   */
  protected List<Tuple2<Integer, KMeansReduceTask>> performKMeansIteration(
    JavaRDD<Instance> dataset, final KMeansMapTask[] mapTasks,
    final boolean[] converged, final int iterationNum,
    final Instances transformedHeaderNoSummary) throws DistributedWekaException {

    final int numRuns = mapTasks.length;

    // keyed by run, a list of partial centroid summary instances
    // - one Instances object for each centroid (may be null if a
    // given centroid did not get any instances assigned to it)
    JavaPairRDD<Integer, List<Instances>> mapRuns =
      dataset
        .mapPartitionsToPair(
          new PairFlatMapFunction<Iterator<Instance>, Integer, List<Instances>>() {

            /**
             * For serialization
             */
            private static final long serialVersionUID = 6063661312796545915L;

            protected List<Tuple2<Integer, List<Instances>>> m_centroidStatsForRuns =
              new ArrayList<Tuple2<Integer, List<Instances>>>();

            @Override
            public Iterable<Tuple2<Integer, List<Instances>>> call(
              Iterator<Instance> split) throws DistributedWekaException {

              while (split.hasNext()) {
                Instance current = split.next();

                for (int k = 0; k < numRuns; k++) {
                  if (!converged[k]) {
                    mapTasks[k].processInstance(current);
                  }
                }
              }

              for (int k = 0; k < numRuns; k++) {
                if (!converged[k]) {
                  List<Instances> centroidStatsForRun =
                    mapTasks[k].getCentroidStats();
                  m_centroidStatsForRuns
                    .add(new Tuple2<Integer, List<Instances>>(k,
                      centroidStatsForRun));
                }
              }

              return m_centroidStatsForRuns;
            }
          }).sortByKey().partitionBy(new IntegerKeyPartitioner(numRuns))
        .persist(StorageLevel.MEMORY_AND_DISK());

    mapRuns.count();

    // Reduce. Need to aggregate all the cluster stats
    // for each run. Do we repartition into numRuns partitions and then
    // run another mapPartitions phase? With our custom partitioner this
    // should guarantee that a partition only contains the lists of instances
    // for one run. Can't use partitionByKey because CSVReduce is not
    // associative, and needs to see the whole list of summary instances
    // objects for one run, cluster# (need to run a separate reduce for
    // each cluster centroid within each run anyway). Then update the
    // final error for each centroid in each run and the total error
    // (sum of errors over centroids for a run)
    JavaPairRDD<Integer, KMeansReduceTask> reducedByRun =
      mapRuns
        .mapPartitionsToPair(new PairFlatMapFunction<Iterator<Tuple2<Integer, List<Instances>>>, Integer, KMeansReduceTask>() {

          /**
           * For serialization
           */
          private static final long serialVersionUID = -747645603149767637L;

          protected List<Tuple2<Integer, KMeansReduceTask>> m_resultsForRun =
            new ArrayList<Tuple2<Integer, KMeansReduceTask>>();

          @Override
          public Iterable<Tuple2<Integer, KMeansReduceTask>> call(
            Iterator<Tuple2<Integer, List<Instances>>> split)
            throws DistributedWekaException {

            List<List<Instances>> partialsForRun =
              new ArrayList<List<Instances>>();
            int runNumber = -1;

            while (split.hasNext()) {
              Tuple2<Integer, List<Instances>> partial = split.next();
              if (runNumber < 0) {
                runNumber = partial._1().intValue();
              } else {
                if (partial._1().intValue() != runNumber) {
                  throw new DistributedWekaException("[k-means] reduce phase: "
                    + "was not expecting the run number to change within a "
                    + "partition!");
                }
              }

              partialsForRun.add(partial._2());
            }

            KMeansReduceTask reducer = new KMeansReduceTask();

            // size might be zero if we are operating on a partition for a
            // run that has already converged (in which case there will be no
            // data in this partition)...
            if (partialsForRun.size() > 0) {
              reducer.reduceClusters(runNumber, iterationNum,
                transformedHeaderNoSummary, partialsForRun);
              m_resultsForRun.add(new Tuple2<Integer, KMeansReduceTask>(
                runNumber, reducer));
            }

            return m_resultsForRun;
          }
        });

    List<Tuple2<Integer, KMeansReduceTask>> runResults = reducedByRun.collect();
    mapRuns.unpersist();
    reducedByRun.unpersist();

    return runResults;
  }

  /**
   * Performs the k-means iterations for all runs in parallel
   *
   * @param dataset the dataset to find clusters on
   * @param headerWithSummary the header of the training data (including summary
   *          attributes)
   * @param numIterations the maximum number of iterations to perform
   * @param numRuns the number of separate runs of k-means to perform in
   *          parallel. The run with the smallest within cluster error becomes
   *          the final model
   * @param numClusters the number of clusters to find
   * @return the final clusterer
   * @throws IOException if a problem occurs
   * @throws DistributedWekaException if a problem occurs
   */
  protected Clusterer buildClusterer(JavaRDD<Instance> dataset,
    Instances headerWithSummary, int numIterations, final int numRuns,
    final int numClusters) throws IOException, DistributedWekaException {

    final Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
    m_trainingHeader = headerNoSummary;

    Instances tmpTrans = null;
    // one configured task per run
    final KMeansMapTask[] mapTasks = new KMeansMapTask[numRuns];
    for (int i = 0; i < numRuns; i++) {
      mapTasks[i] = new KMeansMapTask();

      try {
        mapTasks[i].setOptions(Utils.splitOptions(getKMeansMapTaskOpts()));
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
      tmpTrans = mapTasks[i].init(headerWithSummary);
    }

    // header sans summary attributes after it has been through
    // any filters that the user has specified. This format
    // is needed by the KMeansReduceTask so that it can obtain
    // correct indexes for nominal values
    final Instances transformedHeaderNoSummary = tmpTrans;

    // initial centers
    List<Instances> centers = null;
    if (getInitWithRandomCentroids()) {
      centers =
        initializeWithRandomCenters(dataset, headerWithSummary, numRuns,
          numClusters);
    } else {
      centers =
        initializeWithKMeansParallel(dataset, headerWithSummary, numRuns,
          numClusters);
    }
    if (getDebug()) {
      for (int i = 0; i < numRuns; i++) {
        logMessage("[k-means] k-means"
          + (getInitWithRandomCentroids() ? "" : "||") + " start points Run "
          + i + ":\n" + centers.get(i));
      }
    }

    final boolean[] converged = new boolean[numRuns];
    double[] convergenceSqErr = new double[numRuns];
    int[] numItsPerformed = new int[numRuns];
    int numConverged = 0;

    // make a copy of the initial starting points
    List<Instances> initialCenters = new ArrayList<Instances>(centers);

    List<Tuple2<Integer, KMeansReduceTask>> runResults;
    KMeansReduceTask bestResult = null;
    int bestRunNum = -1;
    int finalNumIterations = -1;
    for (int i = 0; i < numIterations; i++) {

      final int iterationNum = i;

      // initialize each run's map task with it's respective current
      // cluster centers
      for (int j = 0; j < numRuns; j++) {
        mapTasks[j].setCentroids(centers.get(j));
      }

      // perform an k-means iteration
      runResults =
        performKMeansIteration(dataset, mapTasks, converged, iterationNum,
          transformedHeaderNoSummary);

      // create new center list
      for (Tuple2<Integer, KMeansReduceTask> r : runResults) {
        int run = r._1().intValue();

        if (converged[run]) {
          continue;
        }

        KMeansReduceTask runRes = r._2();

        // if we've just finished the first iteration
        if (i == 0) {
          mapTasks[run].setDummyDistancePrimingData(runRes
            .getGlobalDistanceFunctionPrimingData());
        }

        Instances newCentersForRun = runRes.getCentroidsForRun();

        if (getDebug()) {
          logMessage("[k-means] centers for run " + run + " iteration: "
            + (i + 1) + "\n" + newCentersForRun);
          logMessage("[k-means] Total within cluster error: "
            + runRes.getTotalWithinClustersError() + "\n");
        }

        if (i < numIterations - 1) {
          // check for convergence - if we dropped a centroid (because it became
          // empty) then we'll check for convergence in the next iteration
          if (newCentersForRun.numInstances() == centers.get(run)
            .numInstances()) {
            boolean changed = false;
            double totalDist = 0;
            for (int k = 0; k < newCentersForRun.numInstances(); k++) {
              double dist =
                mapTasks[run].distance(newCentersForRun.instance(k), centers
                  .get(run).instance(k));
              if (m_debug) {
                logMessage("[k-means] Run " + run + " convergence distance: "
                  + dist);
              }
              totalDist += dist;
              if (dist > m_convergenceTolerance) {
                changed = true;
              }
            }

            if (!changed) {
              logMessage("[k-means] Run: " + run + " converged in " + (i + 1)
                + " iterations.");
              List<Instances> centroidSummaries =
                runRes.getAggregatedCentroidSummaries();
              if (m_debug) {
                for (Instances sum : centroidSummaries) {
                  System.err.println(sum);
                }
              }
              converged[run] = true;
              convergenceSqErr[run] = runRes.getTotalWithinClustersError();
              numItsPerformed[run] = i + 1;
              numConverged++;

              if (bestResult == null) {
                bestResult = runRes;
                bestRunNum = run;
                finalNumIterations = bestResult.getIterationNumber();
              } else {
                if (runRes.getTotalWithinClustersError() < bestResult
                  .getTotalWithinClustersError()) {
                  bestResult = runRes;
                  bestRunNum = run;
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
              double currentSqErr = runRes.getTotalWithinClustersError();
              if ((bestResult.getTotalWithinClustersError() + m_convergenceTolerance) < (currentSqErr - projectedImprovement)) {
                if (getDebug()) {
                  logMessage("[k-means] aborting run " + run
                    + " as its current within clust. error (" + currentSqErr
                    + ") " + "is unlikely to beat the current best run ("
                    + bestResult.getTotalWithinClustersError() + ") within "
                    + remainingIts + " iterations");
                }

                converged[run] = true;
                convergenceSqErr[run] = currentSqErr;
                numItsPerformed[run] = -(i + 1);
                numConverged++;
              }
            }
          }
        }

        centers.set(run, newCentersForRun);
      }

      // check for convergence of *all* remaining runs and break
      if (numConverged == numRuns || i == numIterations - 1) {
        // scan for best
        for (Tuple2<Integer, KMeansReduceTask> r : runResults) {
          int run = r._1().intValue();
          KMeansReduceTask runRes = r._2();

          if (bestResult == null) {
            bestResult = runRes;
            bestRunNum = run;
            finalNumIterations = bestResult.getIterationNumber();
          } else {
            if (runRes.getTotalWithinClustersError() < bestResult
              .getTotalWithinClustersError()) {
              bestResult = runRes;
              bestRunNum = run;
              finalNumIterations = bestResult.getIterationNumber();
            }
          }
        }
        break;
      }
    }

    Clusterer finalClusterer =
      makeFinalClusterer(bestResult, mapTasks[0].getPreprocessingFilters(),
        initialCenters.get(bestRunNum), finalNumIterations);
    System.err.println(finalClusterer);

    if (numRuns > 1) {
      for (int i = 0; i < numRuns; i++) {
        System.err.println("Run "
          + i
          + ""
          + (numItsPerformed[i] < 0 ? " halted after " + -numItsPerformed[i]
            : " converged after " + numItsPerformed[i])
          + " iterations. Within cluster sum of sq. err: "
          + convergenceSqErr[i]);
      }
    }

    return finalClusterer;
  }

  /**
   * Write a clusterer to the output directory
   *
   * @param finalClusterer the cluster to write
   * @param header the header of the data (sans summary attributes) used to
   *          train the clusterer
   * @param outputPath the output path to write to
   * @throws IOException if a problem occurs
   */
  protected void writeClustererToDestination(Clusterer finalClusterer,
    Instances header, String outputPath) throws IOException {
    OutputStream os = openFileForWrite(outputPath);
    ObjectOutputStream oos = null;

    try {
      BufferedOutputStream bos = new BufferedOutputStream(os);
      oos = new ObjectOutputStream(bos);

      oos.writeObject(finalClusterer);
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

  /**
   * Constructs the final clusterer object once iteration has completed. This
   * will encapsulate the results from the best run of k-means.
   *
   * @param best the results of the best run of k-means
   * @param preprocess any preprocessing filter(s) in play
   * @param initialStartingPoints the initial starting points for the best run
   *          of k-means
   * @param finalNumIterations the final number of iterations executed by the
   *          best run of k-means
   * @return the final clusterer object
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
    finalKMeans.setDisplayStdDevs(getDisplayCentroidStdDevs());
    if (initialStartingPoints != null) {
      finalKMeans.setInitialStartingPoints(initialStartingPoints);
    }
    try {
      finalKMeans.setDistanceFunction(dist);
      finalKMeans.setClusterStats(best.getAggregatedCentroidSummaries());
    } catch (Exception e) {
      logMessage(e);
      throw new DistributedWekaException(e);
    }

    if (!getInitWithRandomCentroids()) {
      finalKMeans.setInitializationMethod(new SelectedTag(
        SimpleKMeans.KMEANS_PLUS_PLUS, SimpleKMeans.TAGS_SELECTION));
    }

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
   * Perform the k-means|| initialization process
   *
   * @param dataset the dataset to operate on
   * @param headerWithSummary the header of the data, with summary attributes
   * @param numRuns the number of separate runs of k-means to be performed in
   *          parallel
   * @param numClusters the number of clusters to generate
   * @return a list of Instances objects, where each Instances object contains
   *         the starting points for one run of k-means
   * @throws IOException if a problem occurs
   * @throws DistributedWekaException if a problem occurs
   */
  protected List<Instances> initializeWithKMeansParallel(
    JavaRDD<Instance> dataset, Instances headerWithSummary, final int numRuns,
    int numClusters) throws IOException, DistributedWekaException {

    int numSteps =
      Integer.parseInt(environmentSubstitute(getKMeansParallelInitSteps()));

    // random seed option
    int randomSeed = 1;
    if (!DistributedJobConfig.isEmpty(getRandomSeed())) {
      try {
        randomSeed = Integer.parseInt(environmentSubstitute(getRandomSeed()));
      } catch (NumberFormatException ex) {
        // don't fuss
      }
    }

    // 1) start with 1 randomly chosen point for each run
    // 2) run sketch for x iterations (aggregating reservoirs for each
    // run at the end of each iteration (i.e. reservoirs for run 1
    // on each split of the data, reservoirs for run 2, etc.)
    // 3) Get final sketch for each run
    // 4) Weight each point in each sketch by the number of points
    // in the data that cluster to it
    // 5) Run local KMeans on data weighted data to obtain final k
    // starting centers

    // Step 1: start with 1 randomly chosen point for each run
    List<Instances> randomSingleCenters =
      initializeWithRandomCenters(dataset, headerWithSummary, numRuns, 1);

    // Step 2: run sketch for x iterations (aggregating reservoirs for each
    // run at the end of each iteration (i.e. reservoirs for run 1
    // on each split of the data, reservoirs for run 2, etc.)
    Instances tmpTrans = null;
    // one configured task per run (we'll use this for an initial distance
    // function and for step 4 where we need to cluster all the points to
    // get cluster sizes
    final KMeansMapTask[] mapTasks = new KMeansMapTask[numRuns];
    for (int i = 0; i < numRuns; i++) {
      mapTasks[i] = new KMeansMapTask();

      try {
        mapTasks[i].setOptions(Utils.splitOptions(getKMeansMapTaskOpts()));
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
      tmpTrans = mapTasks[i].init(headerWithSummary);
    }

    // transformed header (has passed through filters)
    final Instances transformedHeaderNoSummary = tmpTrans;

    NormalizableDistance distanceFunc = mapTasks[0].getDistanceFunction();
    final CentroidSketch[] sketches = new CentroidSketch[numRuns];
    // initialize sketches
    for (int i = 0; i < numRuns; i++) {
      try {
        // apply any filters
        Instances transformedStartSketch = randomSingleCenters.get(i);
        // mapTasks[0].applyFilters(randomSingleCenters.get(i));

        sketches[i] =
          new CentroidSketch(transformedStartSketch, distanceFunc,
            2 * numClusters, randomSeed + i);
      } catch (Exception ex) {
        logMessage(ex);
        throw new DistributedWekaException(ex);
      }
    }

    // this is used when processing instances in partitions to
    // ensure that each instance from the data set gets
    // filtered appropriately
    final KMeansMapTask forFilteringOnly = mapTasks[0];

    for (int i = 0; i < numSteps; i++) {
      logMessage("[k-means] Running iteration " + (i + 1)
        + " of k-means|| initialization procedure.");
      final int iterationNum = i;

      // keyed by run, a list of partial sketches
      // - one CentroidSketch object for each run in each partition
      JavaPairRDD<Integer, CentroidSketch> mapRuns =
        dataset
          .mapPartitionsToPair(
            new PairFlatMapFunction<Iterator<Instance>, Integer, CentroidSketch>() {

              /**
               * For serialization
               */
              private static final long serialVersionUID = 6063661312796545915L;

              protected List<Tuple2<Integer, CentroidSketch>> m_centroidSketchesForRuns =
                new ArrayList<Tuple2<Integer, CentroidSketch>>();

              @Override
              public Iterable<Tuple2<Integer, CentroidSketch>> call(
                Iterator<Instance> split) throws DistributedWekaException {

                while (split.hasNext()) {
                  Instance current = split.next();
                  try {
                    // make sure it goes through any filters first!
                    current = forFilteringOnly.applyFilters(current);
                  } catch (Exception ex) {
                    throw new DistributedWekaException(ex);
                  }

                  for (int k = 0; k < numRuns; k++) {
                    sketches[k].process(current, iterationNum == 0);
                  }
                }

                for (int k = 0; k < numRuns; k++) {
                  m_centroidSketchesForRuns
                    .add(new Tuple2<Integer, CentroidSketch>(k, sketches[k]));

                }

                return m_centroidSketchesForRuns;
              }
            }).sortByKey().partitionBy(new IntegerKeyPartitioner(numRuns))
          .persist(StorageLevel.MEMORY_AND_DISK());

      mapRuns.count();

      // Each partion of mapRuns now contains partials for just one run.
      // Here we aggregate the partials per run
      JavaPairRDD<Integer, CentroidSketch> reducedByRun =
        mapRuns
          .mapPartitionsToPair(new PairFlatMapFunction<Iterator<Tuple2<Integer, CentroidSketch>>, Integer, CentroidSketch>() {

            /** For serialization */
            private static final long serialVersionUID = 7689178383188695493L;

            protected List<Tuple2<Integer, CentroidSketch>> m_resultsForRun =
              new ArrayList<Tuple2<Integer, CentroidSketch>>();

            @Override
            public Iterable<Tuple2<Integer, CentroidSketch>> call(
              Iterator<Tuple2<Integer, CentroidSketch>> split)
              throws DistributedWekaException {

              int runNumber = -1;
              CentroidSketch initial = null;
              List<NormalizableDistance> distsForRun =
                new ArrayList<NormalizableDistance>();

              while (split.hasNext()) {
                Tuple2<Integer, CentroidSketch> partial = split.next();
                if (runNumber < 0) {
                  runNumber = partial._1().intValue();
                } else {
                  if (partial._1().intValue() != runNumber) {
                    throw new DistributedWekaException(
                      "[k-means] k-means|| initialization: "
                        + "was not expecting the run number to change within "
                        + "a partition!");
                  }
                }

                if (initial == null) {
                  initial = partial._2();
                } else {
                  try {
                    initial.aggregateReservoir(partial._2()
                      .getReservoirSample());
                  } catch (Exception e) {
                    throw new DistributedWekaException(e);
                  }
                }

                // get all the distance functions and
                // compute priming data that has global
                // min and maxes.
                if (iterationNum == 0) {
                  // only need to determine global distance function
                  // priming data once (i.e. in the first iteration of
                  // the k-means|| process)
                  distsForRun.add(partial._2().getDistanceFunction());
                }
              }

              // update the distance function with global numeric
              // attribute ranges
              if (distsForRun.size() > 0) {
                Instances distancePrimingData =
                  KMeansReduceTask
                    .computeDistancePrimingDataFromDistanceFunctions(
                      distsForRun, transformedHeaderNoSummary);
                initial.getDistanceFunction().setInstances(distancePrimingData);
              }

              m_resultsForRun.add(new Tuple2<Integer, CentroidSketch>(
                runNumber, initial));

              return m_resultsForRun;
            }
          });

      List<Tuple2<Integer, CentroidSketch>> runResults = reducedByRun.collect();
      mapRuns.unpersist();
      mapRuns = null;

      for (Tuple2<Integer, CentroidSketch> r : runResults) {
        int runNum = r._1().intValue();
        sketches[runNum] = r._2();

        // add the current contents of the reservoir to the sketch
        // for each run
        try {
          sketches[runNum].addReservoirToCurrentSketch();

          if (m_debug) {
            logMessage("[k-means] Iteration: " + i
              + " - number of instances in sketch: "
              + sketches[runNum].getCurrentSketch().numInstances() + "\n"
              + sketches[runNum].getCurrentSketch());
          }
        } catch (Exception ex) {
          logMessage(ex);
          throw new DistributedWekaException(ex);
        }
      }
      reducedByRun.unpersist();
    }

    // perform and aggregate clustering using the final sketch results
    // so that we can find out how many points are assigned to
    // each instance in the sketch.
    Instances globalPriming = sketches[0].getDistanceFunction().getInstances();
    if (globalPriming.numInstances() != 2) {
      logMessage("[k-means] Error: as expecting a two instance "
        + "(global priming data) dataset to be set in the distance function "
        + "in each sketch!");
      throw new DistributedWekaException(
        "Was expecting a two instance (global priming data)"
          + " dataset to be set in the distance function in each sketch!");
    }
    for (int i = 0; i < numRuns; i++) {
      // set sketches as centers for map tasks
      // in preparation for clustering (so that we can)
      // find out how many training points get assigned to
      // each center

      mapTasks[i].setCentroids(sketches[i].getCurrentSketch());
      mapTasks[i].setDummyDistancePrimingData(globalPriming);
    }

    // 3 & 4) Get final sketch for each run and weight each point in
    // the sketch by the number of training instances that cluster to it
    List<Tuple2<Integer, KMeansReduceTask>> clusterAssignments =
      performKMeansIteration(dataset, mapTasks, new boolean[numRuns], 1,
        transformedHeaderNoSummary);

    List<Instances> finalStartPointsForRuns = new ArrayList<Instances>();
    for (int i = 0; i < numRuns; i++) {
      int rN = clusterAssignments.get(i)._1().intValue();
      List<Instances> centroidSummaries =
        clusterAssignments.get(i)._2().getAggregatedCentroidSummaries();

      Instances sketchForRun = sketches[i].getCurrentSketch();

      // empty clusters shouldn't be a problem - in
      // one iteration each sketch member should at minimum
      // have itself assigned (i.e. count >= 1). NOTE: The only exception
      // could occur if the sketch contains duplicate instances. However,
      // this shouldn't happen within a single WeightedReservoirSampling
      // as candidate instances with weight 0 (i.e. distance 0 to the sketch
      // in this case) are never added to the sketch.
      if (centroidSummaries.size() != sketchForRun.numInstances()) {
        logMessage("[k-means] Error: was expecting as "
          + "many summary headers as \n"
          + "there are center candidates in the sketch for run " + rN);
        throw new DistributedWekaException(
          "Was expecting as many summary headers as "
            + "there are center candidates in the sketch for run " + rN);
      }

      for (int j = 0; j < sketchForRun.numInstances(); j++) {
        Instance centerCandidate = sketchForRun.instance(j);
        Instances centerStats = centroidSummaries.get(j);
        double weightForCandidate = -1.0;
        // now grab the first summary attribute and get count
        for (int k = 0; k < sketchForRun.numAttributes(); k++) {

          if (sketchForRun.attribute(k).isNumeric()) {
            Attribute statsAtt =
              centerStats
                .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
                  + sketchForRun.attribute(k).name());
            weightForCandidate =
              ArffSummaryNumericMetric.COUNT.valueFromAttribute(statsAtt)
                + ArffSummaryNumericMetric.MISSING.valueFromAttribute(statsAtt);
            break;
          } else if (sketchForRun.attribute(k).isNominal()) {
            Attribute statsAtt =
              centerStats
                .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
                  + sketchForRun.attribute(k).name());
            NominalStats ns = NominalStats.attributeToStats(statsAtt);
            weightForCandidate = 0;
            for (String s : ns.getLabels()) {
              weightForCandidate += ns.getCount(s);
            }
            weightForCandidate += ns.getNumMissing();
          }
        }

        if (weightForCandidate < 0) {
          logMessage("[k-means] Error: unable to compute the "
            + "number of training instances " + "assigned to sketch member "
            + j + " in run " + i);
          throw new DistributedWekaException(
            "Unable to compute the number of training instances "
              + "assigned to sketch member " + j + " in run " + i);
        }

        // finally - set the weight
        centerCandidate.setWeight(weightForCandidate);
      }

      if (m_debug) {
        logMessage("Final weighted sketch (run " + i
          + ") prior to local KMeans:\n" + sketchForRun);
      }

      // now run standard k-means on the weighted sketch to
      // (hopefully) get the requested number of start points
      SimpleKMeans localKMeans = new SimpleKMeans();
      try {
        localKMeans.setNumClusters(numClusters);
        localKMeans.setInitializationMethod(new SelectedTag(
          SimpleKMeans.KMEANS_PLUS_PLUS, SimpleKMeans.TAGS_SELECTION));
        localKMeans.buildClusterer(sketchForRun);
        finalStartPointsForRuns.add(localKMeans.getClusterCentroids());
      } catch (Exception ex) {
        logMessage(ex);
        throw new DistributedWekaException(ex);
      }
    }

    m_distanceFunctionPrimingData = globalPriming;

    return finalStartPointsForRuns;
  }

  /**
   * Initialize by randomly selecting instances from the dataset
   *
   * @param dataset the dataset to operate on
   * @param headerWithSummary the header of the data, with summary attributes
   * @param numRuns the number of runs of k-means to perform in parallel
   * @param numClusters the number of clusters to find
   * @return a list of Instances objects, where each Instances object contains
   *         the randomly selected start points for one run of k-means
   * @throws IOException if a problem occurs
   * @throws DistributedWekaException if a problem occurs
   */
  protected List<Instances> initializeWithRandomCenters(
    JavaRDD<Instance> dataset, Instances headerWithSummary, int numRuns,
    int numClusters) throws IOException, DistributedWekaException {

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    // sample all runs worth of initial centers in one hit
    // take twice as many as needed in case there are duplicates
    int seed = 1;
    if (!DistributedJobConfig.isEmpty(getRandomSeed())) {
      try {
        seed = Integer.parseInt(environmentSubstitute(getRandomSeed()));
      } catch (NumberFormatException e) {
        // don't complain
      }
    }

    // oversample for > 1 cluster per run, so that we have some options if there
    // are duplicates in the list. numClusters == 1 will be used when seeding
    // the k-means|| initialization process
    int oversampleFactor = numClusters > 1 ? 2 : 1;
    List<Instance> centerList =
      dataset.takeSample(true, oversampleFactor * numRuns * numClusters, seed);
    List<Instance> centerListSettable = new ArrayList<>(centerList);

    // make sure that start points and header have been through any filters
    KMeansMapTask forFilteringOnly = new KMeansMapTask();
    try {
      forFilteringOnly.setOptions(Utils
        .splitOptions(environmentSubstitute(getKMeansMapTaskOpts())));

      // initialize sketches
      headerNoSummary = forFilteringOnly.init(headerWithSummary);

      for (int i = 0; i < centerListSettable.size(); i++) {
        Instance filtered =
          forFilteringOnly.applyFilters(centerListSettable.get(i));
        centerListSettable.set(i, filtered);
      }

    } catch (Exception ex) {
      logMessage(ex);
      throw new DistributedWekaException(ex);
    }

    List<Instances> centreCandidates =
      KMeansMapTask.assignStartPointsFromList(numRuns, numClusters,
        centerListSettable, headerNoSummary);

    return centreCandidates;
  }

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
      dataSet = (JavaRDD<Instance>) getDataset(TRAINING_DATA).getRDD();
      headerWithSummary = getDataset(TRAINING_DATA).getHeaderWithSummary();
      logMessage("[k-means] RDD<Instance> dataset provided: "
        + dataSet.partitions().size() + " partitions.");
      logMessage("Current caching strategy: " + getCachingStrategy());
    }

    if (dataSet == null && headerWithSummary == null) {
      logMessage("[k-means] invoking ARFF Job...");
      m_arffHeaderJob.setEnvironment(m_env);
      m_arffHeaderJob.setLog(getLog());
      m_arffHeaderJob.setStatusMessagePrefix(m_statusMessagePrefix);

      // header job necessary?
      success = m_arffHeaderJob.runJobWithContext(sparkContext);
      setCachingStrategy(m_arffHeaderJob.getCachingStrategy());
      if (!success) {
        setJobStatus(JobStatus.FAILED);
        statusMessage("Unable to continue - creating the ARFF header failed!");
        logMessage("[k-means] unable to continue - creating the ARFF header failed!");
        return false;
      }

      Dataset d = m_arffHeaderJob.getDataset(TRAINING_DATA);
      headerWithSummary = d.getHeaderWithSummary();
      dataSet = d.getRDD();
      setDataset(TRAINING_DATA, new Dataset(dataSet, headerWithSummary));
    }

    int numClusters = 2;
    if (!DistributedJobConfig.isEmpty(getNumClusters())) {
      try {
        numClusters = Integer.parseInt(environmentSubstitute(getNumClusters()));
      } catch (NumberFormatException e) {
        // ignore
      }
    }

    int numRuns = 1;
    if (!DistributedJobConfig.isEmpty(getNumRuns())) {
      try {
        numRuns = Integer.parseInt(environmentSubstitute(getNumRuns()));
      } catch (NumberFormatException e) {
        // ignore
      }
    }

    int numIterations = 20;
    if (!DistributedJobConfig.isEmpty(getNumIterations())) {
      try {
        numIterations =
          Integer.parseInt(environmentSubstitute(getNumIterations()));
      } catch (NumberFormatException e) {
        // ignore
      }
    }

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    // Make sure that we save out to a subdirectory of the output
    // directory
    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);

    Clusterer finalClusterer = null;
    // serialized input is assumed to already be randomized...
    if (getRandomlyShuffleData() /* && !getSerializedInput() */) {
      m_randomizeSparkJob.setDefaultToLastAttIfClassNotSpecified(false);
      m_randomizeSparkJob.setEnvironment(m_env);
      m_randomizeSparkJob.setLog(getLog());
      m_randomizeSparkJob.setStatusMessagePrefix(m_statusMessagePrefix);
      m_randomizeSparkJob.setCachingStrategy(getCachingStrategy());
      m_randomizeSparkJob.setDataset(TRAINING_DATA, new Dataset(dataSet,
        headerWithSummary));

      if (!m_randomizeSparkJob.runJobWithContext(sparkContext)) {
        statusMessage("Unable to continue - random shuffling of "
          + "input data failed!");
        logMessage("[k-means] ynable to continue - random shuffling of input "
          + "data failed!");
        return false;
      }

      Dataset d = m_randomizeSparkJob.getDataset(TRAINING_DATA);
      dataSet = d.getRDD();
      headerWithSummary = d.getHeaderWithSummary();
      setDataset(TRAINING_DATA, new Dataset(dataSet, headerWithSummary));
      // m_dataSet = randomized;
    }

    m_finalClusterer =
      buildClusterer(dataSet, headerWithSummary, numIterations, numRuns,
        numClusters);
    // pass on the model (in case EM clustering is being executed downstream
    // from us)
    getDataset(TRAINING_DATA).setAdditionalDataElement(K_MEANS_MODEL,
      m_finalClusterer);

    outputPath +=
      (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
        + getModelFileName();
    writeClustererToDestination(m_finalClusterer, headerNoSummary, outputPath);

    setJobStatus(JobStatus.FINISHED);
    return success;
  }

  @Override
  public Clusterer getClusterer() {
    return m_finalClusterer;
  }

  @Override
  public Instances getTrainingHeader() {
    return m_trainingHeader;
  }

  @Override
  public String getText() {
    return m_finalClusterer != null ? m_finalClusterer.toString()
      : "Clusterer not built yet!";
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {
    if (!(toRun instanceof KMeansClustererSparkJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a KMeansClustererSparkJob!");
    }

    try {
      KMeansClustererSparkJob kcsj = (KMeansClustererSparkJob) toRun;
      if (Utils.getFlag('h', options)) {
        String help = DistributedJob.makeOptionsStr(kcsj);
        System.err.println(help);
        System.exit(1);
      }

      kcsj.setOptions(options);
      kcsj.runJob();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
