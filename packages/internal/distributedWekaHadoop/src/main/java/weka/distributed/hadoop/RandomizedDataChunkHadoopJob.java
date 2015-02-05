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
 *    RandomizedDataChunkHadoopJob
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import weka.core.Attribute;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NominalStats;
import weka.core.stats.NumericStats;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.hadoop.HDFSUtils;

/**
 * Job for creating randomly shuffled (and stratified if a nominal class is set)
 * data chunks.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class RandomizedDataChunkHadoopJob extends HadoopJob implements
  CommandlineRunnable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 3559718941696900951L;

  /**
   * The subdirectory of the output directory that this job saves its results to
   */
  protected static final String OUTPUT_SUBDIR = "/randomized";

  /** Holds the options to the header task */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /** ARFF job */
  protected ArffHeaderHadoopJob m_arffHeaderJob = new ArffHeaderHadoopJob();

  /**
   * Number of data chunks to create (either this or numInstancesPerDataChunk
   * should be specified)
   */
  protected String m_numDataChunks = "";

  /**
   * Number of instances per data chunk (determines how many data chunks are
   * created)
   */
  protected String m_numInstancesPerDataChunk = "";

  /** Class index or name */
  protected String m_classIndex = "";

  /** Random seed for shuffling the data */
  protected String m_randomSeed = "1";

  /**
   * If true then we won't default to setting the last attribute as the class if
   * it isn't already set
   */
  protected boolean m_dontDefaultToLastAttIfClassNotSet;

  /**
   * True if the output directory should be deleted first (doing so will force
   * this job to run in the case where there are already chunk files present in
   * the output directory)
   */
  protected boolean m_cleanOutputDir;

  public RandomizedDataChunkHadoopJob() {
    super("Randomly shuffled (and stratified) data chunk job",
      "Create a set of input files where the rows are randomly "
        + "shuffled (and stratified if the class is set and nominal). One of "
        + "numRandomizedDataChunks or numInstancesPerRandomizedDataChunk must "
        + "be set in conjunction with this option.");

    m_mrConfig.setMapperClass(RandomizedDataChunkHadoopMapper.class.getName());
    m_mrConfig
      .setReducerClass(RandomizedDataChunkHadoopReducer.class.getName());
    m_mrConfig.setMapOutputValueClass(Text.class.getName());
  }

  /**
   * Help information
   * 
   * @return help information for this job
   */
  public String globalInfo() {
    return "Produces randomized and stratified data chunks";
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

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String numRandomizedDataChunksTipText() {
    return "The number of randomly shuffled data chunks to create. Use in "
      + "conjunction with createRandomizedDataChunks";
  }

  /**
   * Non-command line option to allow clients to turn off the default behavior
   * of defaulting to setting the last attribute as the class if not explicitly
   * specified.
   * 
   * @param d true if the class is not to be set to the last attribute if the
   *          user has not specifically specified a class
   */
  public void setDontDefaultToLastAttIfClassNotSpecified(boolean d) {
    m_dontDefaultToLastAttIfClassNotSet = d;
  }

  /**
   * Non-command line option to allow clients to turn off the default behavior
   * of defaulting to setting the last attribute as the class if not explicitly
   * specified.
   * 
   * @return true if the class is not to be set to the last attribute if the
   *         user has not specifically specified a class
   */
  public boolean getDontDefaultToLastAttIfClassNotSpecified() {
    return m_dontDefaultToLastAttIfClassNotSet;
  }

  /**
   * Set the number of randomly shuffled data chunks to create. Use in
   * conjunction with createRandomizedDataChunks.
   * 
   * @param chunks the number of chunks to create.
   */
  public void setNumRandomizedDataChunks(String chunks) {
    m_numDataChunks = chunks;
  }

  /**
   * Get the number of randomly shuffled data chunks to create. Use in
   * conjunction with createRandomizedDataChunks.
   * 
   * @return the number of chunks to create.
   */
  public String getNumRandomizedDataChunks() {
    return m_numDataChunks;
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String numInstancesPerRandomizedDataChunkTipText() {
    return "The number of instances that each randomly shuffled data chunk "
      + "should contain. Use in conjunction with createRandomizedDataChunks.";
  }

  /**
   * Set the number of instances that each randomly shuffled data chunk should
   * have. Use in conjunction with createRandomizedDataChunks.
   * 
   * @param insts the number of instances that each randomly shuffled data chunk
   *          should contain
   */
  public void setNumInstancesPerRandomizedDataChunk(String insts) {
    m_numInstancesPerDataChunk = insts;
  }

  /**
   * Get the number of instances that each randomly shuffled data chunk should
   * have. Use in conjunction with createRandomizedDataChunks.
   * 
   * @return the number of instances that each randomly shuffled data chunk
   *         should contain
   */
  public String getNumInstancesPerRandomizedDataChunk() {
    return m_numInstancesPerDataChunk;
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
   * Set the name or index of the class attribute ("first" and "last" can also
   * be used)
   * 
   * @param c the name or index of the class attribute
   */
  public void setClassAttribute(String c) {
    m_classIndex = c;
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
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String randomSeedTipText() {
    return "The random seed to use when randomly shuffling the data";
  }

  /**
   * Set the random seed for shuffling the data
   * 
   * @param seed the random seed to use
   */
  public void setRandomSeed(String seed) {
    m_randomSeed = seed;
  }

  /**
   * Get the random seed for shuffling the data
   * 
   * @return the random seed to use
   */
  public String getRandomSeed() {
    return m_randomSeed;
  }

  /**
   * Tip text for this property
   * 
   * @return tip text for this property
   */
  public String cleanOutputDirectoryTipText() {
    return "Set to true to delete any existing output directory and force this job to run";
  }

  /**
   * Set whether to blow away the output directory before running. If an output
   * directory exists (and is populated with chunk files) then deleting this
   * prior to running will force the job to run.
   * 
   * @param clean true if the output directory should be deleted before first
   *          (thus forcing the job to run if there was a populated output
   *          directory already).
   */
  public void setCleanOutputDirectory(boolean clean) {
    m_cleanOutputDir = clean;
  }

  /**
   * Get whether to blow away the output directory before running. If an output
   * directory exists (and is populated with chunk files) then deleting this
   * prior to running will force the job to run.
   * 
   * @return true if the output directory should be deleted before first (thus
   *         forcing the job to run if there was a populated output directory
   *         already).
   */
  public boolean getCleanOutputDirectory() {
    return m_cleanOutputDir;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result
      .add(new Option(
        "\tClean output directory. Forces the job to run "
          + "in the case where the output directory\n\talready exists and is populated "
          + "with chunk files", "clean", 0, "-clean"));
    result.add(new Option(
      "\tNumber of randomized data chunks. Use in conjunction with\n\t"
        + "-randomized-chunks", "num-chunks", 1, "-num-chunks <integer>"));
    result.add(new Option(
      "\tNumber of instances per randomized data chunk.\n\t"
        + "Use in conjunction with -randomized-chunks",
      "num-instances-per-chunk", 1, "-num-instances-per-chunk <integer>"));

    result.add(new Option("\tClass index (1-based) or class attribute name "
      + "(default = last attribute).", "class", 1, "-class <index or name>"));

    result.add(new Option("\tRandom seed (default = 1)", "seed", 1,
      "-seed <integer>"));

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
    String numDataChunks = Utils.getOption("num-chunks", options);
    setNumRandomizedDataChunks(numDataChunks);

    String numInstancesPerChunk =
      Utils.getOption("num-instances-per-chunk", options);
    setNumInstancesPerRandomizedDataChunk(numInstancesPerChunk);

    String className = Utils.getOption("class", options);
    setClassAttribute(className);

    String seed = Utils.getOption("seed", options);
    setRandomSeed(seed);

    setCleanOutputDirectory(Utils.getFlag("clean", options));

    // copy the options at this point so that we can set
    // general hadoop configuration for the ARFF header
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

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    if (getCleanOutputDirectory()) {
      options.add("-clean");
    }

    if (!DistributedJobConfig.isEmpty(getNumRandomizedDataChunks())) {
      options.add("-num-chunks");
      options.add(getNumRandomizedDataChunks());
    }

    if (!DistributedJobConfig.isEmpty(getNumInstancesPerRandomizedDataChunk())) {
      options.add("-num-instances-per-chunk");
      options.add(getNumInstancesPerRandomizedDataChunk());
    }

    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      options.add("-class");
      options.add(getClassAttribute());
    }

    if (!DistributedJobConfig.isEmpty(getRandomSeed())) {
      options.add("-seed");
      options.add(getRandomSeed());
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

  /**
   * Get the options for this job only
   * 
   * @return the options for this job only
   */
  public String[] getJobOptionsOnly() {
    List<String> options = new ArrayList<String>();

    if (getCleanOutputDirectory()) {
      options.add("-clean");
    }

    if (!DistributedJobConfig.isEmpty(getNumRandomizedDataChunks())) {
      options.add("-num-chunks");
      options.add(getNumRandomizedDataChunks());
    }

    if (!DistributedJobConfig.isEmpty(getNumInstancesPerRandomizedDataChunk())) {
      options.add("-num-instances-per-chunk");
      options.add(getNumInstancesPerRandomizedDataChunk());
    }

    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      options.add("-class");
      options.add(getClassAttribute());
    }

    if (!DistributedJobConfig.isEmpty(getRandomSeed())) {
      options.add("-seed");
      options.add(getRandomSeed());
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Initialize and run the ARFF header creation job (if necessary).
   * 
   * @return true if the job was successful
   * @throws DistributedWekaException if a problem occurs
   * @throws IOException if a problem occurs
   */
  protected boolean initializeAndRunArffJob() throws DistributedWekaException,
    IOException {

    if (m_arffHeaderJob.getFinalHeader() != null) {
      return true;
    }

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    // Run the ARFF header job first
    logMessage("Checking to see if ARFF job is needed....");
    statusMessage("Checking to see if ARFF job is needed...");
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

  @Override
  public boolean runJob() throws DistributedWekaException {

    boolean success = true;

    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());

      try {
        if (!m_cleanOutputDir) {
          // check to see if there are files in the output directory. If so,
          // assume that we don't need to run;
          String outputDir = m_mrConfig.getOutputPath() + OUTPUT_SUBDIR;
          Configuration conf = new Configuration();
          m_mrConfig.getHDFSConfig().configureForHadoop(conf, m_env);
          FileSystem fs = FileSystem.get(conf);
          Path p = new Path(outputDir + "/chunk0-r-00000");
          if (fs.exists(p)) {
            if (m_log != null) {
              statusMessage("Output directory is populated with chunk files - no need to execute");
              logMessage("Output directory is populated with chunk files - no need to execute");
            } else {
              System.err
                .println("Output directory is populated with chunk files - no need to execute");
            }
            return true;
          }
        }

        setJobStatus(JobStatus.RUNNING);

        m_arffHeaderJob.setGenerateCharts(false);
        if (!initializeAndRunArffJob()) {
          return false;
        }

        Instances header = m_arffHeaderJob.getFinalHeader();

        Instances headerNoSummary =
          CSVToARFFHeaderReduceTask.stripSummaryAtts(header);
        try {
          WekaClassifierHadoopMapper.setClassIndex(getClassAttribute(),
            headerNoSummary, !m_dontDefaultToLastAttIfClassNotSet);
        } catch (Exception e) {
          throw new DistributedWekaException(e);
        }

        // a summary attribute for getting the total number of instances
        Attribute summaryAttOrig = null;
        for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
          if (headerNoSummary.attribute(i).isNumeric()
            || headerNoSummary.attribute(i).isNominal()) {
            summaryAttOrig = headerNoSummary.attribute(i);
            break;
          }
        }

        String summaryName = summaryAttOrig.name();
        Attribute summaryAtt =
          header.attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
            + summaryName);

        if (summaryAtt == null) {
          throw new DistributedWekaException(
            "Was unable to find the summary attribute for " + "attribute: "
              + summaryName);
        }

        int totalNumInstances = 0;

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

        Configuration conf = new Configuration();

        // set randomize/stratify properties
        // add the aggregated ARFF header to the distributed cache
        String pathToHeader =
          environmentSubstitute(m_arffHeaderJob.getAggregatedHeaderPath());

        HDFSUtils.addFileToDistributedCache(m_mrConfig.getHDFSConfig(), conf,
          pathToHeader, m_env);
        String fileNameOnly =
          pathToHeader.substring(pathToHeader.lastIndexOf("/") + 1,
            pathToHeader.length());

        List<String> randomizeMapOptions = new ArrayList<String>();
        randomizeMapOptions.add("-arff-header");
        randomizeMapOptions.add(fileNameOnly);

        if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
          randomizeMapOptions.add("-class");
          randomizeMapOptions.add(environmentSubstitute(getClassAttribute()));
        }

        if (!DistributedJobConfig.isEmpty(getRandomSeed())) {
          randomizeMapOptions.add("-seed");
          randomizeMapOptions.add(environmentSubstitute(getRandomSeed()));
        }

        if (m_dontDefaultToLastAttIfClassNotSet) {
          randomizeMapOptions.add("-dont-default-class-to-last");
        }

        m_mrConfig
          .setUserSuppliedProperty(
            RandomizedDataChunkHadoopMapper.RANDOMIZED_DATA_CHUNK_MAP_TASK_OPTIONS,
            environmentSubstitute(Utils.joinOptions(randomizeMapOptions
              .toArray(new String[randomizeMapOptions.size()]))));

        // Need these for row parsing via open-csv
        m_mrConfig.setUserSuppliedProperty(
          CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS,
          environmentSubstitute(getCSVMapTaskOptions()));

        int numChunks = 0;
        if (DistributedJobConfig.isEmpty(getNumRandomizedDataChunks())
          && DistributedJobConfig
            .isEmpty(getNumInstancesPerRandomizedDataChunk())) {
          throw new DistributedWekaException("Must specify either the number "
            + "of chunks to create or the number of instances per chunk");
        }

        if (!DistributedJobConfig.isEmpty(getNumRandomizedDataChunks())) {
          try {
            numChunks =
              Integer
                .parseInt(environmentSubstitute(getNumRandomizedDataChunks()));
          } catch (NumberFormatException ex) {
            throw new DistributedWekaException(ex);
          }
        } else {
          int numInsts = 0;
          try {
            numInsts =
              Integer
                .parseInt(environmentSubstitute(getNumInstancesPerRandomizedDataChunk()));
          } catch (NumberFormatException ex) {
            throw new DistributedWekaException(ex);
          }

          if (numInsts <= 0) {
            throw new DistributedWekaException(
              "Number of instances per chunk must be > 0");
          }

          if (numInsts > totalNumInstances) {
            throw new DistributedWekaException(
              "Can't have more instances per chunk than "
                + "there are instances in the dataset!");
          }

          double nc = (double) totalNumInstances / numInsts;
          nc = Math.ceil(nc);

          numChunks = (int) nc;
        }

        if (numChunks <= 1) {
          throw new DistributedWekaException(
            "Can't randomize because number of data chunks <= 1");
        }

        m_mrConfig.setUserSuppliedProperty(
          RandomizedDataChunkHadoopReducer.NUM_DATA_CHUNKS, "" + numChunks);

        // set output path
        String outputPath = m_mrConfig.getOutputPath();
        outputPath += OUTPUT_SUBDIR;
        outputPath = environmentSubstitute(outputPath);
        m_mrConfig.setOutputPath(outputPath);

        // set number of reducers to 1 (otherwise we'll get more
        // chunks than we want!
        m_mrConfig.setNumberOfReducers("1");

        installWekaLibrariesInHDFS(conf);

        Job job = null;
        try {
          job =
            m_mrConfig
              .configureForHadoop(
                "Create randomly shuffled input data chunk job - num chunks: "
                  + numChunks, conf, m_env);
        } catch (ClassNotFoundException e) {
          throw new DistributedWekaException(e);
        }

        // setup multiple outputs
        for (int i = 0; i < numChunks; i++) {
          MultipleOutputs.addNamedOutput(job, "chunk" + i,
            TextOutputFormat.class, Text.class, Text.class);
        }

        // run the job!
        m_mrConfig.deleteOutputDirectory(job, m_env);

        statusMessage("Submitting randomized data chunk job ");
        logMessage("Submitting randomized data chunk job ");

        success = runJob(job);

        setJobStatus(success ? JobStatus.FINISHED : JobStatus.FAILED);

        if (!success) {
          statusMessage("Create randomly shuffled input data chunk job failed - check logs on Hadoop");
          logMessage("Create randomly shuffled input data chunk job job failed - check logs on Hadoop");
        } else {
          // need to tidy up in the output directory - for some reason
          // there seems to be a spurious part-r-00000 with size 0 created
          String toDelete = outputPath + "/part-r-00000";
          HDFSUtils.deleteFile(m_mrConfig.getHDFSConfig(), conf, toDelete,
            m_env);
        }

      } catch (Exception ex) {
        setJobStatus(JobStatus.FAILED);
        throw new DistributedWekaException(ex);
      }
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    return success;
  }

  /**
   * Get the path to the output directory for this job
   * 
   * @return the path to the output directory for this job
   */
  public String getRandomizedChunkOutputPath() {
    return m_mrConfig.getOutputPath()
      + (m_mrConfig.getOutputPath().endsWith(OUTPUT_SUBDIR) ? ""
        : OUTPUT_SUBDIR);
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {
    if (!(toRun instanceof RandomizedDataChunkHadoopJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a RandomizedDataChunkHadoopJob!");
    }

    try {
      RandomizedDataChunkHadoopJob rdchj = (RandomizedDataChunkHadoopJob) toRun;

      if (Utils.getFlag('h', options)) {
        String help = DistributedJob.makeOptionsStr(rdchj);
        System.err.println(help);
        System.exit(1);
      }

      rdchj.setOptions(options);
      rdchj.runJob();

    } catch (Exception ex) {
      ex.printStackTrace();
    }

  }

  public static void main(String[] args) {
    RandomizedDataChunkHadoopJob rdchj = new RandomizedDataChunkHadoopJob();

    rdchj.run(rdchj, args);
  }
}
