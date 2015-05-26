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
 *    WekaScoringHadoopJob
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;

import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.hadoop.HDFSUtils;

/**
 * Hadoop job for scoring new data using an existing Weka classifier or
 * clusterer.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaScoringHadoopJob extends HadoopJob implements
  CommandlineRunnable {

  /** The subdirectory of the output directory to save the job outputs to */
  protected static final String OUTPUT_SUBDIR = "/scoring";

  /** For serialization */
  private static final long serialVersionUID = 2899919003194014468L;

  /** The columns from the original input data to output in the scoring results */
  protected String m_colsRange = "first-last";

  /** The path (HDFS or local) to the model to use for scoring */
  protected String m_modelPath = "";

  /** ARFF header job (if necessary) */
  protected ArffHeaderHadoopJob m_arffHeaderJob = new ArffHeaderHadoopJob();

  /** Options to the ARFF header job */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /**
   * Constructor
   */
  public WekaScoringHadoopJob() {
    super("Scoring job", "Score data with a Weka model");

    m_mrConfig.setMapperClass(WekaScoringHadoopMapper.class.getName());
    // turn off the reducer phase as we only need mappers

    m_mrConfig.setMapOutputKeyClass(LongWritable.class.getName());
    m_mrConfig.setMapOutputValueClass(Text.class.getName());
  }

  /**
   * Help information for this job
   * 
   * @return help information for this job
   */
  public String globalInfo() {
    return "Score new data using a previously trained model.";
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String modelPathTipText() {
    return "The path (HDFS or local) to the model to use for scoring";
  }

  /**
   * Set the path (HDFS or local) to the model to use for scoring.
   * 
   * @param modelPath the path to the model to use for scoring
   */
  public void setModelPath(String modelPath) {
    m_modelPath = modelPath;
  }

  /**
   * Get the path (HDFS or local) to the model to use for scoring.
   * 
   * @return the path to the model to use for scoring
   */
  public String getModelPath() {
    return m_modelPath;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String columnsToOutputInScoredDataTipText() {
    return "The columns to output (as a comma-separated list of indexes) in "
      + "the scored data. 'first' and 'last' may be used as well ("
      + "e.g. 1,2,10-last)";
  }

  /**
   * Set the columns to output (as a comma-separated list of indexes) in the
   * scored data. 'first' and 'last' may be used as well (e.g. 1,2,10-last).
   * 
   * @param cols the columns to output in the scored data
   */
  public void setColumnsToOutputInScoredData(String cols) {
    m_colsRange = cols;
  }

  /**
   * Get the columns to output (as a comma-separated list of indexes) in the
   * scored data. 'first' and 'last' may be used as well (e.g. 1,2,10-last).
   * 
   * @return the columns to output in the scored data
   */
  public String getColumnsToOutputInScoredData() {
    return m_colsRange;
  }

  /**
   * Set the options for the ARFF header job
   * 
   * @param opts options for the ARFF header job
   */
  public void setCSVMapTaskOptions(String opts) {
    m_wekaCsvToArffMapTaskOpts = opts;
  }

  /**
   * Get the options for the ARFF header job
   * 
   * @return the options for the ARFF header job
   */
  public String getCSVMapTaskOptions() {
    return m_wekaCsvToArffMapTaskOpts;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option(
      "\tPath to model file to use for scoring (can be \n\t"
        + "local or in HDFS", "model-file", 1,
      "-model-file <path to model file>"));

    result.add(new Option(
      "\tColumns to output in the scored data. Specify as\n\t"
        + "a range, e.g. 1,4,5,10-last (default = first-last).",
      "columns-to-output", 1, "-columns-to-output"));

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
    String modelPath = Utils.getOption("model-file", options);
    if (!DistributedJobConfig.isEmpty(modelPath)) {
      setModelPath(modelPath);
    }

    String range = Utils.getOption("columns-to-output", options);
    if (!DistributedJobConfig.isEmpty(range)) {
      setColumnsToOutputInScoredData(range);
    }

    // copy the options at this point so that we can set
    // general hadoop configuration for the ARFF header
    // job
    String[] optionsCopy = options.clone();

    super.setOptions(options);

    m_arffHeaderJob.setOptions(optionsCopy);

    String optsToCSVTask = Utils.joinOptions(m_arffHeaderJob.getOptions());
    if (!DistributedJobConfig.isEmpty(optsToCSVTask)) {
      setCSVMapTaskOptions(optsToCSVTask);
    }
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    if (!DistributedJobConfig.isEmpty(getModelPath())) {
      options.add("-model-file");
      options.add(getModelPath());
    }

    options.add("-columns-to-output");
    options.add(getColumnsToOutputInScoredData());

    if (!DistributedJobConfig.isEmpty(getCSVMapTaskOptions())) {
      try {
        String[] csvOpts = Utils.splitOptions(getCSVMapTaskOptions());

        for (String s : csvOpts) {
          options.add(s);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    return options.toArray(new String[options.size()]);
  }

  public String[] getJobOptionsOnly() {
    List<String> options = new ArrayList<String>();

    if (!DistributedJobConfig.isEmpty(getModelPath())) {
      options.add("-model-file");
      options.add(getModelPath());
    }

    options.add("-columns-to-output");
    options.add(getColumnsToOutputInScoredData());

    return options.toArray(new String[options.size()]);
  }

  /**
   * Loads the user-supplied model and sets the job name based on the classifier
   * and its options.
   * 
   * @param is InputStream to read the model from
   * @throws IOException if a problem occurs
   */
  protected void loadClassifierAndSetJobName(InputStream is) throws IOException {
    ObjectInputStream ois = null;

    try {
      ois = new ObjectInputStream(new BufferedInputStream(is));

      Object model = ois.readObject();

      String className = model.getClass().toString();
      String options = "";
      if (model instanceof OptionHandler) {
        options = " " + Utils.joinOptions(((OptionHandler) model).getOptions());
      }

      setJobName("Scoring job: " + className + options);
    } catch (Exception ex) {
      throw new IOException(ex);
    } finally {
      if (ois != null) {
        ois.close();
      }
    }
  }

  /**
   * Handles loading the user-supplied model from either HDFS or the local file
   * system
   * 
   * @param conf the Configuration for the job
   * @return the file name only part of the path to the model
   * @throws IOException if a problem occurs
   */
  protected String handleModelFile(Configuration conf) throws IOException {
    String modelPath = getModelPath();

    try {
      modelPath = environmentSubstitute(modelPath);
    } catch (Exception ex) {
    }

    if (modelPath.startsWith("hdfs://")) {
      modelPath = modelPath.replace("hdfs://", "");

      // strip the host and port (if provided)
      modelPath = modelPath.substring(modelPath.indexOf("/"));
    }

    String modelNameOnly = null;
    String hdfsPath = "";
    File f = new File(modelPath);
    boolean success = false;
    if (f.exists()) {
      // local model file
      if (modelPath.lastIndexOf(File.separator) >= 0) {
        modelNameOnly =
          modelPath.substring(modelPath.lastIndexOf(File.separator)
            + File.separator.length(), modelPath.length());
      } else {
        modelNameOnly = modelPath;
      }

      hdfsPath = HDFSUtils.WEKA_TEMP_DISTRIBUTED_CACHE_FILES + modelNameOnly;
      logMessage("Copying local model file (" + modelPath + ") to HDFS.");
      HDFSUtils.copyToHDFS(modelPath, hdfsPath, m_mrConfig.getHDFSConfig(),
        m_env, true);

      loadClassifierAndSetJobName(new FileInputStream(f));
      success = true;
    } else {
      // hdfs source
      hdfsPath = modelPath;

      modelNameOnly =
        modelPath.substring(modelPath.lastIndexOf("/") + 1, modelPath.length());

      Configuration tempConf = new Configuration();
      m_mrConfig.getHDFSConfig().configureForHadoop(tempConf, m_env);
      FileSystem fs = FileSystem.get(tempConf);
      Path p = new Path(hdfsPath);
      FSDataInputStream di = fs.open(p);

      loadClassifierAndSetJobName(di);
      success = true;
    }

    if (!success) {
      throw new IOException("Unable to locate model file: " + modelPath
        + " on " + "the local file system or in HDFS.");
    }

    System.err.println("Adding " + hdfsPath + " to the distributed cache.");
    HDFSUtils.addFileToDistributedCache(m_mrConfig.getHDFSConfig(), conf,
      hdfsPath, m_env);

    return modelNameOnly;
  }

  /**
   * Initializes and executes the ARFF header job if necessary
   * 
   * @return true if the job succeeds
   * @throws DistributedWekaException if a problem occurs
   */
  protected boolean initializeAndRunArffJob() throws DistributedWekaException {

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }
    m_arffHeaderJob.setEnvironment(m_env);
    if (!m_arffHeaderJob.runJob()) {
      statusMessage("Unable to continue - creating the ARFF header failed!");
      logMessage("Unable to continue - creating the ARFF header failed!");
      return false;
    }

    // configure our output subdirectory
    String outputPath = m_mrConfig.getOutputPath();
    outputPath += OUTPUT_SUBDIR;
    outputPath = environmentSubstitute(outputPath);
    m_mrConfig.setOutputPath(outputPath);

    return true;
  }

  @Override
  public boolean runJob() throws DistributedWekaException {

    if (DistributedJobConfig.isEmpty(getModelPath())) {
      throw new DistributedWekaException(
        "No model file specified - can't continue");
    }

    boolean success = false;
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());
      setJobStatus(JobStatus.RUNNING);

      if (!initializeAndRunArffJob()) {
        return false;
      }

      // set zero reducers - map only job
      m_mrConfig.setNumberOfReducers("0");

      String pathToHeader =
        environmentSubstitute(m_arffHeaderJob.getAggregatedHeaderPath());
      Configuration conf = new Configuration();

      try {
        // add the arff header to the distributed cache
        HDFSUtils.addFileToDistributedCache(m_mrConfig.getHDFSConfig(), conf,
          pathToHeader, m_env);

        String modelNameOnly = handleModelFile(conf);
        String arffNameOnly =
          pathToHeader.substring(pathToHeader.lastIndexOf("/") + 1,
            pathToHeader.length());
        String colRange =
          environmentSubstitute(getColumnsToOutputInScoredData());

        String mapOptions =
          "-arff-header "
            + arffNameOnly
            + " -model-file-name "
            + modelNameOnly
            + (!DistributedJobConfig.isEmpty(colRange) ? " -columns-to-output "
              + colRange : "");

        m_mrConfig.setUserSuppliedProperty(
          WekaScoringHadoopMapper.SCORING_MAP_TASK_OPTIONS, mapOptions);

        // Need these for row parsing via open-csv
        m_mrConfig.setUserSuppliedProperty(
          CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS,
          environmentSubstitute(getCSVMapTaskOptions()));

        installWekaLibrariesInHDFS(conf);

        Job job = null;
        job = m_mrConfig.configureForHadoop(getJobName(), conf, m_env);

        cleanOutputDirectory(job);

        statusMessage("Submitting scoring job: " + getJobName());
        logMessage("Submitting scoring job: " + getJobName());

        success = runJob(job);
      } catch (IOException ex) {
        throw new DistributedWekaException(ex);
      } catch (ClassNotFoundException e) {
        throw new DistributedWekaException(e);
      }
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    return success;
  }

  public static void main(String[] args) {

    WekaScoringHadoopJob wsj = new WekaScoringHadoopJob();
    wsj.run(wsj, args);
  }

  @Override
  public void run(Object toRun, String[] args) throws IllegalArgumentException {

    if (!(toRun instanceof WekaScoringHadoopJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a WekaScoringHadoopJob");
    }

    try {
      WekaScoringHadoopJob wsj = (WekaScoringHadoopJob) toRun;

      if (Utils.getFlag('h', args)) {
        String help = DistributedJob.makeOptionsStr(wsj);
        System.err.println(help);
        System.exit(1);
      }

      wsj.setOptions(args);
      wsj.runJob();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
