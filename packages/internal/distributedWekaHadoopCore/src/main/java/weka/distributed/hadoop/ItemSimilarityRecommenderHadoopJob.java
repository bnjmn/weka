package weka.distributed.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;

import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Option;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import weka.recommender.ItemSimilarityRecommender;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.hadoop.HDFSUtils;
import distributed.hadoop.MapReduceJobConfig;

public class ItemSimilarityRecommenderHadoopJob extends HadoopJob implements
  CommandlineRunnable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 2088471831291153184L;

  protected static final String OUTPUT_SUBDIR = "/recommender";

  /** ARFF job */
  protected ArffHeaderHadoopJob m_arffHeaderJob = new ArffHeaderHadoopJob();

  /** Holds the options to the header task */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /** Holds the options to the similarity map task */
  protected String m_recommenderMapTaskOpts = "";

  /** Configuration for the denormalize phase */
  protected MapReduceJobConfig m_denormalizeConfig = new MapReduceJobConfig();

  /**
   * Constructor
   */
  public ItemSimilarityRecommenderHadoopJob() {
    super("Item similarity recommender job",
      "Build an similarity-based item recommender");

    m_mrConfig.setMapperClass(RecommenderSimilarityRowHadoopMapper.class
      .getName());
    // m_mrConfig.setCombinerClass(RecommenderSimilarityHadoopCombiner.class
    // .getName());
    m_mrConfig.setReducerClass(RecommenderSimilarityRowHadoopReducer.class
      .getName());
    m_mrConfig.setMapOutputKeyClass(LongWritable.class.getName());
    m_mrConfig.setOutputKeyClass(LongWritable.class.getName());
    m_mrConfig.setOutputValueClass(BytesWritable.class.getName());
  }

  /**
   * Help information
   * 
   * @return help information for this job
   */
  public String globalInfo() {
    return "Builds a similarity-based item recommender";
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
   * Get the options to the header job
   * 
   * @return options to the header job
   */
  public String getCSVMapTaskOptions() {
    return m_wekaCsvToArffMapTaskOpts;
  }

  public void setRecommenderMapTaskOptions(String opts) {
    m_recommenderMapTaskOpts = opts;
  }

  public String getRecommenderMapTaskOptions() {
    return m_recommenderMapTaskOpts;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    ItemSimilarityRecommender tempRecommenderTask = new ItemSimilarityRecommender();
    Enumeration<Option> rOpts = tempRecommenderTask.listOptions();
    while (rOpts.hasMoreElements()) {
      result.add(rOpts.nextElement());
    }

    result.add(new Option("", "", 0,
      "\nOptions specific to ARFF training header creation:"));

    ArffHeaderHadoopJob tempArffJob = new ArffHeaderHadoopJob();
    Enumeration<Option> arffOpts = tempArffJob.listOptions();
    while (arffOpts.hasMoreElements()) {
      result.add(arffOpts.nextElement());
    }

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    // copy the options at this point so that we can set
    // general hadoop configuration for the ARFF header
    // job
    String[] optionsCopy = options.clone();

    super.setOptions(options);

    // set options for the denormalize config
    m_denormalizeConfig.setOptions(optionsCopy.clone());

    m_arffHeaderJob.setOptions(optionsCopy);
    String optsToCSVTask = Utils.joinOptions(m_arffHeaderJob.getOptions());
    if (!DistributedJobConfig.isEmpty(optsToCSVTask)) {
      setCSVMapTaskOptions(optsToCSVTask);
    }

    // options to the similarity map task
    ItemSimilarityRecommender simTemp = new ItemSimilarityRecommender();
    simTemp.setOptions(options);
    String optsToRecommenderTask = Utils.joinOptions(simTemp.getOptions());

    if (!DistributedJobConfig.isEmpty(optsToRecommenderTask)) {
      setRecommenderMapTaskOptions(optsToRecommenderTask);
    }
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

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

    if (!DistributedJobConfig.isEmpty(getRecommenderMapTaskOptions())) {
      try {
        String[] mapOpts = Utils.splitOptions(getRecommenderMapTaskOptions());

        for (String s : mapOpts) {
          options.add(s);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    return options.toArray(new String[options.size()]);
  }

  protected void stageDenormalizeArtifacts(Configuration conf)
    throws IOException {

    // push item means and counts and denormalized header to the
    // distributed cache

    String denormalizePath = m_denormalizeConfig.getOutputPath();
    String countsPath = denormalizePath + "/"
      + DenormalizeHadoopReducer.AUX_ITEM_STATS_SUBDIR_NAME + "/"
      + DenormalizeHadoopReducer.ITEM_COUNTS_FILE_NAME;
    String meansPath = denormalizePath + "/"
      + DenormalizeHadoopReducer.AUX_ITEM_STATS_SUBDIR_NAME + "/"
      + DenormalizeHadoopReducer.ITEM_MEANS_FILE_NAME;
    String denormalizedHeaderHeaderPath = denormalizePath + "/"
      + DenormalizeHadoopReducer.AUX_ITEM_STATS_SUBDIR_NAME + "/"
      + DenormalizeHadoopReducer.DENORMALIZED_HEADER_FILE_NAME;

    List<String> paths = new ArrayList<String>();
    paths.add(countsPath);
    paths.add(meansPath);
    paths.add(denormalizedHeaderHeaderPath);

    try {
      HDFSUtils.addFilesToDistributedCache(m_mrConfig.getHDFSConfig(), conf,
        paths, m_env);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  protected boolean initializeAndRunArffJob() throws DistributedWekaException {
    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

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

  protected boolean initializeAndRunDenormalizeJob()
    throws DistributedWekaException, IOException {

    m_denormalizeConfig
      .setMapperClass(weka.distributed.hadoop.DenormalizeHadoopMapper.class
        .getName());
    m_denormalizeConfig
      .setReducerClass(weka.distributed.hadoop.DenormalizeHadoopReducer.class
        .getName());
    m_denormalizeConfig.setMapOutputKeyClass(LongWritable.class.getName());
    m_denormalizeConfig.setMapOutputValueClass(Text.class.getName());

    Configuration conf = new Configuration();

    String pathToHeader = environmentSubstitute(m_arffHeaderJob
      .getAggregatedHeaderPath());
    String fileNameOnly = pathToHeader.substring(
      pathToHeader.lastIndexOf("/") + 1, pathToHeader.length());

    HDFSUtils.addFileToDistributedCache(m_denormalizeConfig.getHDFSConfig(),
      conf, pathToHeader, m_env);

    StringBuilder opts = new StringBuilder();
    opts.append("-arff-header ").append(fileNameOnly).append(" ");

    // denormalize specific options - just the ARFF header is necessary
    // as this contains min and max item IDs in the summary attributes
    m_denormalizeConfig.setUserSuppliedProperty(
      DenormalizeHadoopMapper.DENORMALIZE_MAP_TASK_OPTIONS, opts.toString());

    // CSV options
    m_denormalizeConfig.setUserSuppliedProperty(
      CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS,
      environmentSubstitute(getCSVMapTaskOptions()));

    // set output path
    String outputPath = m_mrConfig.getOutputPath();
    outputPath += "/denormalized";
    outputPath = environmentSubstitute(outputPath);
    m_denormalizeConfig.setOutputPath(outputPath);

    // configure the location to save denormalized ARFF header,
    // item counts and item means
    String auxItemStatsWritePath = outputPath + "/"
      + DenormalizeHadoopReducer.AUX_ITEM_STATS_SUBDIR_NAME;
    m_denormalizeConfig
      .setUserSuppliedProperty(
        DenormalizeHadoopReducer.AUX_ITEM_STATS_WRITE_PATH,
        auxItemStatsWritePath);

    m_denormalizeConfig.setNumberOfReducers("1");

    installWekaLibrariesInHDFS(conf);

    Job job = null;
    try {
      job = m_denormalizeConfig.configureForHadoop(
        "Denormalize <user,item,rating> job", conf, m_env);
    } catch (ClassNotFoundException ex) {
      throw new DistributedWekaException(ex);
    }

    // run the job
    m_denormalizeConfig.deleteOutputDirectory(job, m_env);
    statusMessage("Submitting denormalize <user, item, rating> data job");
    logMessage("Submitting denormalize <user, item, rating> data job");

    boolean success = runJob(job);

    if (!success) {
      statusMessage("Denormalize data job failed - check logs on Hadoop");
      logMessage("Denormalize data job failed - check logs on Hadoop");
    }

    return success;
  }

  @Override
  public boolean runJob() throws DistributedWekaException {
    boolean success = true;
    try {
      setJobStatus(JobStatus.RUNNING);

      if (!initializeAndRunArffJob()) {
        return false;
      }

      if (!initializeAndRunDenormalizeJob()) {
        return false;
      }

      // set output sub directory
      String outputPath = m_mrConfig.getOutputPath();
      outputPath = environmentSubstitute(outputPath);
      outputPath += OUTPUT_SUBDIR;
      m_mrConfig.setOutputPath(outputPath);

      // Reducer will write serialized model and matrix here.
      m_mrConfig.setUserSuppliedProperty(
        RecommenderSimilarityHadoopReducer.SIMILARITY_WRITE_PATH, outputPath);

      // make sure that the input to this job is the output from the
      // denormalize job
      m_mrConfig.setInputPaths(m_denormalizeConfig.getOutputPath());

      // stage denormalize artifacts
      Configuration conf = new Configuration();
      stageDenormalizeArtifacts(conf);

      m_mrConfig.setUserSuppliedProperty(
        RecommenderSimilarityHadoopMapper.SIMILARITY_MAP_TASK_OPTIONS,
        getRecommenderMapTaskOptions());

      // controls how many spill files will be merged into one file
      // at one time. Higher value means more files processed and fewer
      // passes (default was 10).
      // m_mrConfig.setUserSuppliedProperty("io.sort.factor", "100");

      // percentage of sort memory used to keep track of record tracking meta
      // data. Spilling to disk happens when this is full ("record full = true"
      // in logs)
      // m_mrConfig.setUserSuppliedProperty("io.sort.record.percent", "0.15");

      // our mappers use very little memory - up the portion of
      // the default map JVM heap (200mb if it hasn't been changed) to
      // allow more sorting in memory
      // m_mrConfig.setUserSuppliedProperty("io.sort.mb", "175");

      // install weka libraries and user-selected packages to HDFS
      // and to the distributed cache/classpath for the job
      installWekaLibrariesInHDFS(conf);

      Job job = null;
      try {
        job = m_mrConfig.configureForHadoop(getJobName(), conf, m_env);
      } catch (ClassNotFoundException e) {
        throw new DistributedWekaException(e);
      }

      cleanOutputDirectory(job);
      statusMessage("Submitting " + getJobName());
      logMessage("Submitting " + getJobName());

      success = runJob(job);

      if (!success) {
        setJobStatus(JobStatus.FAILED);
        statusMessage(getJobName() + " failed - check logs on Hadoop");
        logMessage(getJobName() + " failed - check logs on Hadoop");
      } else {
        setJobStatus(JobStatus.FINISHED);
      }
    } catch (Exception ex) {
      setJobStatus(JobStatus.FAILED);
      throw new DistributedWekaException(ex);
    }

    return success;
  }

  @Override
  public void run(Object toRun, String[] options) {
    if (!(toRun instanceof ItemSimilarityRecommenderHadoopJob)) {
      throw new IllegalArgumentException(
        "Object is not an ItemSimilarityHadoopJob!");
    }

    try {
      ItemSimilarityRecommenderHadoopJob ishj = (ItemSimilarityRecommenderHadoopJob) toRun;

      if (Utils.getFlag('h', options)) {
        String help = DistributedJob.makeOptionsStr(ishj);
        System.err.println(help);
        System.exit(1);
      }

      ishj.setOptions(options);
      ishj.runJob();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static void main(String[] args) {
    ItemSimilarityRecommenderHadoopJob ishj = new ItemSimilarityRecommenderHadoopJob();

    ishj.run(ishj, args);
  }
}
