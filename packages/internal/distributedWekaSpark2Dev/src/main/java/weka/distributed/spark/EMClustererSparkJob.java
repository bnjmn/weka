package weka.distributed.spark;

import distributed.core.DistributedJobConfig;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import weka.clusterers.Clusterer;
import weka.clusterers.PreconstructedFilteredClusterer;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.InstanceWithWeightsHolder;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.EMapTask;
import weka.distributed.MMapTask;
import weka.gui.beans.ClustererProducer;
import weka.gui.beans.TextProducer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class EMClustererSparkJob extends SparkJob implements
  CommandlineRunnable, TextProducer, ClustererProducer {

  public static final String TRAINING_DATA_WITH_WEIGHTS = "em_trainingData";

  /**
   * The subdirectory of the output directory that this job saves its results to
   */
  protected static final String OUTPUT_SUBDIR = "em";

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

  /**
   * The number of k-means runs to perform in order to select a starting point
   * for EM
   */
  protected int m_numKMeansRuns = 1;

  /** Number of clusters to find */
  protected String m_numClusters = "2";

  /** Seed for the random number generator */
  protected String m_randomSeed = "1";

  /** Maximum number of EM iterations to perform */
  protected int m_maxIterations = 20;

  /** Use a pre-saved k-means model for initialization? */
  protected String m_pathToKMeansModel = "";

  /**
   * Minimum improvement in log likelihood needed in order to continue iterating
   */
  protected double m_minLogLikelihoodImprovement = 1e-6;

  /** Holds the final clustering model */
  protected Clusterer m_finalClusterer;

  /** The header (sans summary attributes) used to train the clusterer with */
  protected Instances m_trainingHeader;

  /** Randomize and stratify job */
  protected RandomizedDataSparkJob m_randomizeSparkJob =
    new RandomizedDataSparkJob();

  protected KMeansClustererSparkJob m_kMeansClustererSparkJob =
    new KMeansClustererSparkJob();

  public EMClustererSparkJob() {
    super("EM clusterer job", "Build an expectation maximisation clusterer");
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

  @Override
  public Clusterer getClusterer() {
    return null;
  }

  @Override
  public Instances getTrainingHeader() {
    return null;
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {

  }

  protected void iterateEM(JavaRDD<InstanceWithWeightsHolder> dataset,
    Clusterer kMeans, Instances headerNoSummary)
    throws DistributedWekaException {
    MMapTask mTask = new MMapTask();
    mTask.setCurrentModelFromKMeans(kMeans, headerNoSummary);
    int numClusters = -1;
    try {
      numClusters = kMeans.numberOfClusters();
    } catch (Exception ex) {

    }
    final EMapTask eTask = new EMapTask();
    eTask.initFromM(mTask);

    MMapTask prevModel = null;
    MMapTask finalModel = null;
    for (int i = 0; i < m_maxIterations; i++) {
      // E step - compute cluster membership
      dataset =
        dataset
          .mapPartitions(
            new FlatMapFunction<Iterator<InstanceWithWeightsHolder>, InstanceWithWeightsHolder>() {
              protected List<InstanceWithWeightsHolder> m_results =
                new ArrayList<InstanceWithWeightsHolder>();

              @Override
              public Iterator<InstanceWithWeightsHolder> call(
                Iterator<InstanceWithWeightsHolder> split) throws Exception {

                while (split.hasNext()) {
                  m_results.add(eTask.processInstance(split.next()));
                }

                return m_results.iterator();
              }
            }).persist(getCachingStrategy().getStorageLevel());
      dataset.count(); // force materialization

      // M step - estimate new parameters and compute log likelihood of current
      // model
      mTask.initNewModel(headerNoSummary, numClusters);
      final MMapTask mTask_f = mTask;
      JavaRDD<MMapTask> newParams =
        dataset
          .mapPartitions(new FlatMapFunction<Iterator<InstanceWithWeightsHolder>, MMapTask>() {

            protected List<MMapTask> m_partialParams =
              new ArrayList<MMapTask>();

            @Override
            public Iterator<MMapTask> call(
              Iterator<InstanceWithWeightsHolder> split) throws Exception {

              while (split.hasNext()) {
                mTask_f.processInstance(split.next());
              }

              m_partialParams.add(mTask_f);
              return m_partialParams.iterator();
            }
          });

      List<MMapTask> partials = newParams.collect();
      MMapTask aggregatedNewModel = mTask.aggregateNewModels(partials);

      // compute the total log likelihood of the current model
      mTask.aggregateLogLikelihoodCurrentModel(partials);
      if (i > 0) {
        if (mTask.getLogLikelihood() - prevModel.getLogLikelihood() < m_minLogLikelihoodImprovement) {
          finalModel = prevModel;
          break;
        } else {
          finalModel = mTask;
        }
      }
      prevModel = mTask.copy();
      mTask = aggregatedNewModel;
    }
  }

  /**
   * Just produces an an initial RDD[InstanceWithWeightsHolder] dataset from a
   * RDD[Instance] one, ready for EM to iterate over.
   * 
   * @param input the training RDD[Instance] dataset
   * @return an RDD[InstanceWithWeights] dataset suitable for EM to iterate over
   */
  protected JavaRDD<InstanceWithWeightsHolder> produceInitialWeightedData(
    JavaRDD<Instance> input) {

    JavaRDD<InstanceWithWeightsHolder> training =
      input.map(new Function<Instance, InstanceWithWeightsHolder>() {
        @Override
        public InstanceWithWeightsHolder call(Instance instance)
          throws Exception {
          return new InstanceWithWeightsHolder(instance);
        }
      }).persist(getCachingStrategy().getStorageLevel());

    // materialize
    training.count();

    return training;
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

    int numClusters = 2;
    if (!DistributedJobConfig.isEmpty(getNumClusters())) {
      try {
        numClusters = Integer.parseInt(environmentSubstitute(getNumClusters()));
      } catch (NumberFormatException e) {
        // ignore
      }
    }

    JavaRDD<Instance> dataSet = null;
    Instances headerWithSummary = null;

    // first see if we have a dataset and k-means available already
    WDDataset d = getDataset(TRAINING_DATA);
    Clusterer finalKMeans = null;
    if (d != null) {
      dataSet = d.getRDD();
      headerWithSummary =
        getDataset(TRAINING_DATA_WITH_WEIGHTS).getHeaderWithSummary();
      logMessage("[EM] RDD<InstanceWithWeightsHolder> dataset provided: "
        + dataSet.partitions().size() + " partitions.");

      finalKMeans =
        (Clusterer) d
          .getAdditionalDataElement(KMeansClustererSparkJob.K_MEANS_MODEL);
    }

    // if no k-means available and we're not going to load one then run
    // k-means
    if (finalKMeans == null && m_pathToKMeansModel.length() == 0) {
      // need to run the k-means job
      logMessage("[EM] initializing with k-means...");
      m_kMeansClustererSparkJob.setEnvironment(m_env);
      m_kMeansClustererSparkJob.setLog(getLog());
      m_kMeansClustererSparkJob.setStatusMessagePrefix(m_statusMessagePrefix);
      m_kMeansClustererSparkJob.setCachingStrategy(getCachingStrategy());
      m_kMeansClustererSparkJob.setNumClusters("" + numClusters);
      /*
       * m_kMeansClustererSparkJob.setRandomlyShuffleData(getRandomlyShuffleData(
       * )); if (getRandomlyShuffleData()) { //
       * m_kMeansClustererSparkJob.setRandomizeJobOptions(); }
       */

      success = m_kMeansClustererSparkJob.runJobWithContext(sparkContext);
      if (!success) {
        statusMessage("Unable to continue - k-means initialization process failed!");
        logMessage("[EM] k-means initialization processes failed - can't "
          + "continue");
        setJobStatus(JobStatus.FAILED);
        return false;
      }
      d = m_kMeansClustererSparkJob.getDataset(TRAINING_DATA);

      dataSet = d.getRDD();
      headerWithSummary = d.getHeaderWithSummary();
      finalKMeans = m_kMeansClustererSparkJob.getClusterer();
    }

    // if still no dataset, then we must be going to load a k-means model.
    // Therefore, we need to run the ARFF job and potentially the random
    // shuffle job
    if (dataSet == null) {
      // TODO
      // we need to run the arff header job and possibly the randomize job

      logMessage("[k-means] invoking ARFF Job...");
      m_arffHeaderJob.setEnvironment(m_env);
      m_arffHeaderJob.setLog(getLog());
      m_arffHeaderJob.setStatusMessagePrefix(m_statusMessagePrefix);
      m_arffHeaderJob.setCachingStrategy(getCachingStrategy());

      // header job necessary?
      success = m_arffHeaderJob.runJobWithContext(sparkContext);
      if (!success) {
        setJobStatus(JobStatus.FAILED);
        statusMessage("Unable to continue - creating the ARFF header failed!");
        logMessage("[EM] unable to continue - creating the ARFF header failed!");
        return false;
      }

      d = m_arffHeaderJob.getDataset(TRAINING_DATA);
      headerWithSummary = d.getHeaderWithSummary();
      dataSet = d.getRDD();
      setDataset(TRAINING_DATA, new WDDataset(dataSet, headerWithSummary));

      if (getRandomlyShuffleData()) {
        m_randomizeSparkJob.setDefaultToLastAttIfClassNotSpecified(false);
        m_randomizeSparkJob.setEnvironment(m_env);
        m_randomizeSparkJob.setLog(getLog());
        m_randomizeSparkJob.setStatusMessagePrefix(m_statusMessagePrefix);
        m_randomizeSparkJob.setCachingStrategy(getCachingStrategy());
        m_randomizeSparkJob.setDataset(TRAINING_DATA, new WDDataset(dataSet,
          headerWithSummary));

        if (!m_randomizeSparkJob.runJobWithContext(sparkContext)) {
          statusMessage("Unable to continue - random shuffling of "
            + "input data failed!");
          logMessage("[EM] unable to continue - random shuffling of input "
            + "data failed!");
          return false;
        }

        d = m_randomizeSparkJob.getDataset(TRAINING_DATA);
        dataSet = d.getRDD();
        headerWithSummary = d.getHeaderWithSummary();
        setDataset(TRAINING_DATA, new WDDataset(dataSet, headerWithSummary));
      }
    }

    // load a k-means model and do a header consistency check
    if (finalKMeans == null && m_pathToKMeansModel.length() > 0) {
      InputStream in = openFileForRead(m_pathToKMeansModel);
      try {
        Object[] modelAndHeader = SerializationHelper.readAll(in);
        if (modelAndHeader.length < 2) {
          setJobStatus(JobStatus.FAILED);
          statusMessage("Unable to continue - serialized k-means model file "
            + "does not contain a training header!");
          logMessage("[EM] Unable to continue - serialized k-means model file "
            + "does not contain a training header!");
          return false;
        }
        if (!(modelAndHeader[0] instanceof Clusterer)) {
          throw new DistributedWekaException("Loaded model is not a clusterer!");
        }
        finalKMeans = (Clusterer) modelAndHeader[0];
        Instances modelHeader = (Instances) modelAndHeader[1];
        Instances headerNoSummary =
          CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
        String consistency = headerNoSummary.equalHeadersMsg(modelHeader);
        if (consistency != null) {
          throw new DistributedWekaException(consistency);
        }
      } catch (Exception ex) {
        setJobStatus(JobStatus.FAILED);
        throw new DistributedWekaException(ex);
      }
    }

    int kMeansNumClusters = -1;
    try {
      kMeansNumClusters = finalKMeans.numberOfClusters();
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }
    // check how many clusters k-means has been run with...
    logMessage("[EM] Using k-means result for initialization");
    if (numClusters != kMeansNumClusters) {
      logMessage("[EM] NOTE: k-means finished with " + kMeansNumClusters
        + " clusters. This differs "
        + "from the requested number of clusters: " + numClusters);
    }

    Instances headerNoSummary;
    if (!(finalKMeans instanceof PreconstructedFilteredClusterer)) {
      headerNoSummary =
        CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
    } else {
      headerNoSummary =
        ((PreconstructedFilteredClusterer) finalKMeans).getFilter()
          .getOutputFormat();
    }

    iterateEM(produceInitialWeightedData(dataSet), finalKMeans, headerNoSummary);

    // TODO produce final EM clusterer and save

    setJobStatus(JobStatus.FINISHED);
    return true;
  }

  @Override
  public String getText() {
    return null;
  }
}
