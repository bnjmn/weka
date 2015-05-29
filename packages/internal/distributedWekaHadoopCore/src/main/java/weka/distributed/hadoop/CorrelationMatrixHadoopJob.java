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
 *    CorrelationMatrixHadoopJob
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.awt.Image;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.CorrelationMatrixMapTask;
import weka.distributed.CorrelationMatrixRowReduceTask;
import weka.distributed.DistributedWekaException;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.PreConstructedPCA;
import weka.filters.unsupervised.attribute.Remove;
import weka.gui.beans.ImageProducer;
import weka.gui.beans.TextProducer;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.hadoop.AbstractHadoopJobConfig;
import distributed.hadoop.HDFSUtils;
import distributed.hadoop.MapReduceJobConfig;

/**
 * Hadoop job for constructing a correlation matrix. Has an option to use the
 * matrix constructed for performing (a non-distributed) PCA analysis.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CorrelationMatrixHadoopJob extends HadoopJob implements
  TextProducer, ImageProducer, CommandlineRunnable {

  /** For serialization */
  private static final long serialVersionUID = 7319464898913984018L;

  /** Subdirectory of the output directory for storing results to */
  public static final String OUTPUT_SUBDIR = "/correlation";

  /** ARFF header job to run first (if necessary) */
  protected ArffHeaderHadoopJob m_arffHeaderJob = new ArffHeaderHadoopJob();

  /** Stores options to the ARFF header map task */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /** Stores options to the correlation map task */
  protected String m_correlationMapTaskOpts = "";

  /** Class index or name */
  protected String m_classIndex = "";

  /** Number of nodes in the user's Hadoop cluster. Default = 1 */
  protected String m_numNodesAvailable = "1";

  /**
   * Whether to run the Arff header job. An earlier job may have already created
   * an Arff header that is usable for this job.
   */
  protected boolean m_runArffJob = true;

  /** Whether to run a PCA analysis after the job completes */
  protected boolean m_runPCA;

  /** Holds the textual PCA summary string (if PCA is run) */
  protected String m_pcaSummary = "";

  /**
   * Other jobs/tasks can retrieve the matrix from us (if we were successful)
   */
  protected weka.core.matrix.Matrix m_finalMatrix;

  /** Holds the heatmap image of the correlation matrix */
  protected Image m_correlationHeatMap;

  /**
   * Constructor
   */
  public CorrelationMatrixHadoopJob() {
    super("Correlation matrix job",
      "Compute a correlation or covariance matrix");

    m_mrConfig.setMapperClass(CorrelationMatrixHadoopMapper.class.getName());
    m_mrConfig.setReducerClass(CorrelationMatrixRowHadoopReducer.class
      .getName());
  }

  /**
   * Textual help info for this job
   * 
   * @return help info
   */
  public String globalInfo() {
    return "Computes a correlation or covariance matrix. Can "
      + "optionally run a (non-distributed) principal "
      + "components analysis using the correlation matrix " + "as input.";
  }

  /**
   * Set the options for the csv map tasks
   * 
   * @param opts options for the csv map taksk
   */
  public void setCSVMapTaskOptions(String opts) {
    m_wekaCsvToArffMapTaskOpts = opts;
  }

  /**
   * Get the options for the csv map tasks
   * 
   * @return options for the csv map taksk
   */
  public String getCSVMapTaskOptions() {
    return m_wekaCsvToArffMapTaskOpts;
  }

  /**
   * Set options for the correlation map tasks
   * 
   * @param opts options for the correlation map tasks
   */
  public void setCorrelationMapTaskOptions(String opts) {
    m_correlationMapTaskOpts = opts;
  }

  /**
   * Get options for the correlation map tasks
   * 
   * @return options for the correlation map tasks
   */
  public String getCorrelationMapTaskOptions() {
    return m_correlationMapTaskOpts;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String numNodesInClusterTipText() {
    return "The number of nodes in the Hadoop cluster - "
      + "used when determining the number of reducers to run";
  }

  /**
   * Set the number of nodes in the Hadoop cluster
   * 
   * @param n the number of nodes in the Hadoop cluster
   */
  public void setNumNodesInCluster(String n) {
    m_numNodesAvailable = n;
  }

  /**
   * Get the number of nodes in the Hadoop cluster
   * 
   * @return the number of nodes in the Hadoop cluster
   */
  public String getNumNodesInCluster() {
    return m_numNodesAvailable;
  }

  /**
   * Get the matrix generated by this job, or null if the job has not been run
   * yet.
   * 
   * @return the matrix generated by this job or null if the job has not been
   *         run yet.
   */
  public weka.core.matrix.Matrix getMatrix() {
    return m_finalMatrix;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option("\tClass index (1-based) or class attribute name "
      + "(default = no class set).", "class", 1, "-class <index or name>"));

    result.add(new Option(
      "\tNumber of nodes available in cluster (default = 1).", "num-nodes", 1,
      "-num-nodes"));

    result.add(new Option(
      "\tRun PCA analysis and build a PCA filter when job completes.", "pca",
      0, "-pca"));

    CorrelationMatrixMapTask tempTask = new CorrelationMatrixMapTask();
    Enumeration<Option> tOpts = tempTask.listOptions();
    while (tOpts.hasMoreElements()) {
      result.add(tOpts.nextElement());
    }

    result.add(new Option("", "", 0,
      "\nOptions specific to ARFF header creation:"));

    ArffHeaderHadoopJob tempArffJob = new ArffHeaderHadoopJob();
    Enumeration<Option> arffOpts = tempArffJob.listOptions();
    while (arffOpts.hasMoreElements()) {
      result.add(arffOpts.nextElement());
    }

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    setRunPCA(Utils.getFlag("pca", options));

    String numNodes = Utils.getOption("num-nodes", options);
    setNumNodesInCluster(numNodes);

    String className = Utils.getOption("class", options);
    setClassAttribute(className);

    String[] optionsCopy = options.clone();

    // set general hadoop connection/config opts for our job
    super.setOptions(options);

    // options for the ARFF header job
    String sArffOpts = Utils.joinOptions(optionsCopy);
    if (!sArffOpts.contains("-summary-stats")) {
      // make sure we generate summary stats!!
      sArffOpts += " -summary-stats";
      optionsCopy = Utils.splitOptions(sArffOpts);
    }
    m_arffHeaderJob.setOptions(optionsCopy);

    String optsToCSVTask = Utils.joinOptions(m_arffHeaderJob.getOptions());
    if (!DistributedJobConfig.isEmpty(optsToCSVTask)) {
      setCSVMapTaskOptions(optsToCSVTask);
    }

    // options to the Correlation task
    CorrelationMatrixMapTask correlationTemp = new CorrelationMatrixMapTask();
    correlationTemp.setOptions(options);
    String optsToCorrTask = Utils.joinOptions(correlationTemp.getOptions());
    if (!DistributedJobConfig.isEmpty(optsToCorrTask)) {
      setCorrelationMapTaskOptions(optsToCorrTask);
    }
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      options.add("-class");
      options.add(getClassAttribute());
    }

    if (getRunPCA()) {
      options.add("-pca");
    }

    if (!DistributedJobConfig.isEmpty(getNumNodesInCluster())) {
      options.add("-num-nodes");
      options.add(getNumNodesInCluster());
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

    if (!DistributedJobConfig.isEmpty(getCorrelationMapTaskOptions())) {
      try {
        String[] corrOpts = Utils.splitOptions(getCorrelationMapTaskOptions());

        for (String s : corrOpts) {
          options.add(s);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Get just the job options
   * 
   * @return the job options
   */
  public String[] getJobOptionsOnly() {
    List<String> options = new ArrayList<String>();

    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      options.add("-class");
      options.add(getClassAttribute());
    }

    if (getRunPCA()) {
      options.add("-pca");
    }

    if (!DistributedJobConfig.isEmpty(getNumNodesInCluster())) {
      options.add("-nom-nodes");
      options.add(getNumNodesInCluster());
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String classAttributeTipText() {
    return "The name or index of the class attribute";
  }

  /**
   * Set the name or index of the class attribute.
   * 
   * @param c the name or index of the class attribute
   */
  public void setClassAttribute(String c) {
    m_classIndex = c;
  }

  /**
   * Get the name or index of the class attribute.
   * 
   * @return name or index of the class attribute
   */
  public String getClassAttribute() {
    return m_classIndex;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String runPCATipText() {
    return "Run a PCA analysis as a pos-processing step.";
  }

  /**
   * Set whether to run a PCA analysis (using the generated correlation matrix
   * as inpu) as a post-processing step
   * 
   * @param runPCA true if PCA should be run after the correlation job finishes
   */
  public void setRunPCA(boolean runPCA) {
    m_runPCA = runPCA;
  }

  /**
   * Get whether to run a PCA analysis (using the generated correlation matrix
   * as inpu) as a post-processing step
   * 
   * @return true if PCA should be run after the correlation job finishes
   */
  public boolean getRunPCA() {
    return m_runPCA;
  }

  /**
   * Run the ARFF job (if necessary)
   * 
   * @return true if the ARFF job succeeded
   * @throws DistributedWekaException if a problem occurs
   */
  protected boolean initializeAndRunArffJob() throws DistributedWekaException {

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    // Run the ARFF header job first
    if (m_runArffJob) {
      m_arffHeaderJob.setEnvironment(m_env);
      m_arffHeaderJob.setLog(getLog());
      m_arffHeaderJob.setStatusMessagePrefix(m_statusMessagePrefix);
      if (!m_arffHeaderJob.runJob()) {
        statusMessage("Unable to continue - creating the ARFF header failed!");
        logMessage("Unable to continue - creating the ARFF header failed!");
        return false;
      }
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
    m_finalMatrix = null;
    boolean success = true;
    setJobStatus(JobStatus.RUNNING);

    // arff job first
    if (!initializeAndRunArffJob()) {
      return false;
    }

    // check that all non-class attributes are numeric (for now).
    Instances headerI = m_arffHeaderJob.getFinalHeader();
    headerI = CSVToARFFHeaderReduceTask.stripSummaryAtts(headerI);
    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      String sClass = environmentSubstitute(getClassAttribute());
      try {
        WekaClassifierHadoopMapper.setClassIndex(sClass, headerI, false);
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());

      // add the aggregated ARFF header to the distributed cache
      String pathToHeader =
        environmentSubstitute(m_arffHeaderJob.getAggregatedHeaderPath());
      Configuration conf = new Configuration();

      try {
        HDFSUtils.addFileToDistributedCache(m_mrConfig.getHDFSConfig(), conf,
          pathToHeader, m_env);
      } catch (IOException e) {
        throw new DistributedWekaException(e);
      }

      String fileNameOnly =
        pathToHeader.substring(pathToHeader.lastIndexOf("/") + 1,
          pathToHeader.length());

      StringBuilder correlationMapOptions = new StringBuilder();

      correlationMapOptions.append("-arff-header").append(" ")
        .append(fileNameOnly).append(" ");

      if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
        correlationMapOptions.append("-class").append(" ")
          .append(environmentSubstitute(getClassAttribute())).append(" ");
      }

      if (!DistributedJobConfig.isEmpty(getCorrelationMapTaskOptions())) {
        correlationMapOptions
          .append(environmentSubstitute(getCorrelationMapTaskOptions()));
      }

      m_mrConfig.setUserSuppliedProperty(
        CorrelationMatrixHadoopMapper.CORRELATION_MATRIX_MAP_TASK_OPTIONS,
        environmentSubstitute(correlationMapOptions.toString()));

      // Need these for row parsing via open-csv
      m_mrConfig.setUserSuppliedProperty(
        CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS,
        environmentSubstitute(getCSVMapTaskOptions()));

      setJobName(getJobName() + " " + correlationMapOptions.toString());

      try {
        installWekaLibrariesInHDFS(conf);
      } catch (IOException ex) {
        setJobStatus(JobStatus.FAILED);
        throw new DistributedWekaException(ex);
      }

      Job job = null;
      try {
        // set the number of reducers equal to Math.min(numMatrixRows,
        // (reduceMax * numNodesInCluster)
        int numNodesAvail = 1;
        String numNodesInCluster =
          environmentSubstitute(getNumNodesInCluster());
        if (!DistributedJobConfig.isEmpty(numNodesInCluster)) {
          try {
            numNodesAvail = Integer.parseInt(numNodesInCluster);
          } catch (NumberFormatException n) {
            logMessage("WARNING: unable to parse the number of available nodes - setting to 1");
          }
        }
        String taskMaxKey =
          AbstractHadoopJobConfig.isHadoop2() ? MapReduceJobConfig.HADOOP2_TASKTRACKER_REDUCE_TASKS_MAXIMUM
            : MapReduceJobConfig.HADOOP_TASKTRACKER_REDUCE_TASKS_MAXIMUM;
        String reduceTasksMaxPerNode = conf.get(taskMaxKey);
        int reduceMax = 2;

        // allow our configuration to override the defaults for the cluster
        String userMaxOverride =
          m_mrConfig
            .getUserSuppliedProperty(MapReduceJobConfig.HADOOP_TASKTRACKER_REDUCE_TASKS_MAXIMUM);
        if (DistributedJobConfig.isEmpty(userMaxOverride)) {
          // try the Hadoop 2 version
          userMaxOverride =
            m_mrConfig
              .getUserSuppliedProperty(MapReduceJobConfig.HADOOP2_TASKTRACKER_REDUCE_TASKS_MAXIMUM);
        }
        if (!DistributedJobConfig.isEmpty(userMaxOverride)) {
          reduceTasksMaxPerNode = environmentSubstitute(userMaxOverride);
        }

        // num rows in matrix is equal to num attributes in the arff
        // header file (possibly -1 if the class gets ignored)
        int classAdjust = -1;
        if (getCorrelationMapTaskOptions().contains("-keep-class")) {
          classAdjust = 0;
        }

        // The header generated by the ARFF job
        Instances header = m_arffHeaderJob.getFinalHeader();
        header = CSVToARFFHeaderReduceTask.stripSummaryAtts(header);
        int rowsInMatrix = header.numAttributes() + classAdjust;

        if (!DistributedJobConfig.isEmpty(reduceTasksMaxPerNode)) {
          reduceMax =
            Integer.parseInt(environmentSubstitute(reduceTasksMaxPerNode));
        }

        int numReducers = Math.min(rowsInMatrix, reduceMax * numNodesAvail);

        logMessage("Setting number of reducers for correlation job to: "
          + numReducers);
        m_mrConfig.setNumberOfReducers("" + numReducers);

        job = m_mrConfig.configureForHadoop(getJobName(), conf, m_env);

        cleanOutputDirectory(job);
      } catch (ClassNotFoundException ex) {
        setJobStatus(JobStatus.FAILED);
        throw new DistributedWekaException(ex);
      } catch (IOException ex) {
        setJobStatus(JobStatus.FAILED);
        throw new DistributedWekaException(ex);
      }

      statusMessage("Submitting job: " + getJobName());
      logMessage("Submitting job: " + getJobName());

      success = runJob(job);

      if (!success) {
        statusMessage("Correlation matrix job failed - check logs on Hadoop");
        logMessage("Correlation matrix job failed - check logs on Hadoop");
        setJobStatus(JobStatus.FAILED);
        return false; // can't continue
      }

      // now we need to read the part-r-xxxxx files out of the output
      // directory, construct a final Matrix and write it back to the
      // output directory
      finalMatrix(conf, fileNameOnly);

      setJobStatus(success ? JobStatus.FINISHED : JobStatus.FAILED);
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    return success;
  }

  /**
   * Construct the final matrix by reading all the part files and using
   * weka.core.Matrix to write back to HDFS
   * 
   * @param conf the Configuration for the correlation job
   * @param arffFileName the name of the ARFF header for the data that the
   *          correlation/covariance matrix was computed from
   * @throws DistributedWekaException if a problem occurs
   */
  protected void finalMatrix(Configuration conf, String arffFileName)
    throws DistributedWekaException {
    statusMessage("Constructing final matrix...");
    logMessage("Constructing final matrix...");
    Map<Integer, double[]> rows = new TreeMap<Integer, double[]>();
    try {
      FileSystem fs = FileSystem.get(conf);
      String outputDir = m_mrConfig.getOutputPath();

      FileStatus[] contents = fs.listStatus(new Path(outputDir));

      int maxRowNum = 0;
      for (FileStatus s : contents) {
        String nameOnly = s.getPath().toString();
        nameOnly =
          nameOnly.substring(nameOnly.lastIndexOf("/") + 1, nameOnly.length());
        if (nameOnly.startsWith("part-r-")) {
          FSDataInputStream di = fs.open(s.getPath());

          BufferedReader br = null;
          try {
            br = new BufferedReader(new InputStreamReader(di));
            String line = null;
            while ((line = br.readLine()) != null) {
              String[] keyRest = line.split("\t");
              if (keyRest.length != 2) {
                throw new DistributedWekaException(
                  "Was expecting a key and correlation entries on this line: "
                    + line);
              }

              int rowNum = Integer.parseInt(keyRest[0].trim());
              if (rowNum > maxRowNum) {
                maxRowNum = rowNum;
              }
              double[] aRow = new double[rowNum + 1];
              String[] corrEntries = keyRest[1].split(" ");
              if (corrEntries.length != rowNum + 1) {
                throw new DistributedWekaException(
                  "Wrong number of values for correlation row: " + rowNum
                    + ". Was " + "expecting " + (rowNum + 1) + " but got "
                    + corrEntries.length);
              }

              for (int i = 0; i < rowNum + 1; i++) {
                aRow[i] = Double.parseDouble(corrEntries[i]);
              }

              rows.put(new Integer(rowNum), aRow);
            }

            br.close();
            br = null;
          } finally {
            if (br != null) {
              br.close();
            }
          }
        }
      }

      if (maxRowNum + 1 > rows.size()) {
        throw new DistributedWekaException(
          "Matrix incomplete! Max row number seen in part files: " + maxRowNum
            + ". Number of rows read from part files: " + rows.size());
      }

      double[][] m = new double[rows.size()][rows.size()];
      for (Map.Entry<Integer, double[]> e : rows.entrySet()) {
        int i = e.getKey();
        double[] js = e.getValue();

        for (int j = 0; j < js.length; j++) {
          m[i][j] = js[j];
          m[j][i] = js[j];
        }
      }

      m_finalMatrix = new weka.core.matrix.Matrix(m);

      statusMessage("Writing correlation matrix back to HDFS: " + outputDir);
      logMessage("Writing correlation matrix back to HDFS: " + outputDir);

      // write a textual matrix back into the output directory in HDFS
      Path p = new Path(outputDir + "/" + arffFileName + "_matrix.txt");
      FSDataOutputStream dos = fs.create(p, true);
      BufferedWriter bw = null;
      try {
        bw = new BufferedWriter(new OutputStreamWriter(dos));
        m_finalMatrix.write(bw);
        bw.close();
        bw = null;
      } finally {
        if (bw != null) {
          bw.close();
        }
      }

      // now write it back in tuple format
      statusMessage("Writing correlation matrix, in tuple format, back to HDFS: "
        + outputDir);
      logMessage("Writing correlation matrix, in tuple format, back to HDFS: "
        + outputDir);

      p = new Path(outputDir + "/" + arffFileName + "_matrix_tuple.txt");
      dos = fs.create(p, true);
      PrintWriter pw = null;
      try {
        bw = new BufferedWriter(new OutputStreamWriter(dos));
        pw = new PrintWriter(bw);
        for (Map.Entry<Integer, double[]> e : rows.entrySet()) {
          int i = e.getKey();
          double[] js = e.getValue();

          for (int j = 0; j < js.length; j++) {
            pw.println("" + i + "," + j + "," + js[j]);
          }
        }
      } finally {
        if (pw != null) {
          pw.close();
        }
      }

      // write out the names of the attributes that are in the correlation
      // matrix
      List<String> rowAttNames =
        writeCorrelationMatrixRowColumnLabels(conf, arffFileName);
      m_correlationHeatMap =
        CorrelationMatrixRowReduceTask.getHeatMapForMatrix(m_finalMatrix,
          rowAttNames);

      p = new Path(outputDir + "/" + arffFileName + "_heatmap.png");
      dos = fs.create(p, true);
      try {
        CorrelationMatrixRowReduceTask.writeHeatMapImage(m_correlationHeatMap,
          dos);
      } finally {
        if (dos != null) {
          dos.close();
        }
      }

    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }

    if (getRunPCA()) {
      runPCA(conf, arffFileName);
    }
  }

  /**
   * Write the names of the attributes that made it into the correlation matrix
   * (i.e all numeric attributes, and the class (if set) if the user has opted
   * to keep it as part of the analysis) into the output directory for the job
   * 
   * @param conf the Configuration to use
   * @param arffFileName the name of the header file
   * @returns a list of the row attribute names
   * @throws DistributedWekaException if a problem occurs
   */
  protected List<String> writeCorrelationMatrixRowColumnLabels(
    Configuration conf, String arffFileName) throws DistributedWekaException {

    String outputDir = m_mrConfig.getOutputPath();
    Instances header = m_arffHeaderJob.getFinalHeader();

    boolean keepClass = false;
    if (!DistributedJobConfig.isEmpty(getCorrelationMapTaskOptions())) {
      try {
        String[] opts = Utils.splitOptions(getCorrelationMapTaskOptions());

        CorrelationMatrixMapTask temp = new CorrelationMatrixMapTask();
        temp.setOptions(opts);

        keepClass = temp.getKeepClassAttributeIfSet();
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(header);
    String classAtt = getClassAttribute();
    if (!DistributedJobConfig.isEmpty(classAtt)) {
      classAtt = environmentSubstitute(classAtt);
      try {
        WekaClassifierHadoopMapper.setClassIndex(classAtt, headerNoSummary,
          false);
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }

    StringBuilder rem = new StringBuilder();
    if (headerNoSummary.classIndex() >= 0 && !keepClass) {
      rem.append("" + (headerNoSummary.classIndex() + 1)).append(",");
    }

    // remove all nominal attributes
    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      if (!headerNoSummary.attribute(i).isNumeric()) {
        rem.append("" + (i + 1)).append(",");
      }
    }
    if (rem.length() > 0) {
      Remove remove = new Remove();
      rem.deleteCharAt(rem.length() - 1); // remove the trailing ,
      String attIndices = rem.toString();
      remove.setAttributeIndices(attIndices);
      remove.setInvertSelection(false);

      try {
        remove.setInputFormat(headerNoSummary);

        headerNoSummary = Filter.useFilter(headerNoSummary, remove);
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    List<String> rowLabels = new ArrayList<String>();
    try {
      FileSystem fs = FileSystem.get(conf);
      Path p = new Path(outputDir + "/" + arffFileName + "_matrix_labels.txt");
      FSDataOutputStream dos = fs.create(p, true);
      BufferedWriter bw = null;
      try {
        bw = new BufferedWriter(new OutputStreamWriter(dos));
        for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
          bw.write(headerNoSummary.attribute(i).name() + "\n");
          rowLabels.add(headerNoSummary.attribute(i).name());
        }
        bw.flush();
        bw.close();
        bw = null;
      } finally {
        if (bw != null) {
          bw.close();
        }
      }
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }

    return rowLabels;
  }

  /**
   * Execute a PCA analysis using the correlation/covariance matrix output by
   * the job. Also builds and serializes a PreconstructedPCA filter back into
   * the HDFS output directory for the job
   * 
   * @param conf the Configuration of the correlation job
   * @param arffFileName the name of the ARFF header for the data that the
   *          correlation/covariance matrix was generated from
   * @throws DistributedWekaException if a problem occurs
   */
  protected void runPCA(Configuration conf, String arffFileName)
    throws DistributedWekaException {

    String outputDir = m_mrConfig.getOutputPath();

    Instances header = m_arffHeaderJob.getFinalHeader();
    boolean isCov = false;
    boolean keepClass = false;
    if (!DistributedJobConfig.isEmpty(getCorrelationMapTaskOptions())) {
      try {
        String[] opts = Utils.splitOptions(getCorrelationMapTaskOptions());

        CorrelationMatrixMapTask temp = new CorrelationMatrixMapTask();
        temp.setOptions(opts);

        isCov = temp.getCovariance();
        keepClass = temp.getKeepClassAttributeIfSet();
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    if (!DistributedJobConfig.isEmpty(getClassAttribute())) {
      String sClass = environmentSubstitute(getClassAttribute());
      try {
        Instances tempHeaderSansSummary =
          CSVToARFFHeaderReduceTask.stripSummaryAtts(header);
        WekaClassifierHadoopMapper.setClassIndex(sClass, tempHeaderSansSummary,
          false);
        header.setClassIndex(tempHeaderSansSummary.classIndex());
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    try {
      PreConstructedPCA pca =
        new PreConstructedPCA(header, m_finalMatrix, keepClass, isCov);

      // this triggers the computation of the PCA analysis
      pca.setInputFormat(CSVToARFFHeaderReduceTask.stripSummaryAtts(header));

      m_pcaSummary = pca.toString();

      // write the textual summary back into HDFS
      FileSystem fs = FileSystem.get(conf);
      statusMessage("Writing PCA summary to HDFS: " + outputDir);
      logMessage("Writing PCA summary to HDFS: " + outputDir);

      Path p = new Path(outputDir + "/" + arffFileName + "_pca_summary.txt");
      FSDataOutputStream dos = fs.create(p, true);
      BufferedWriter bw = null;
      try {
        bw = new BufferedWriter(new OutputStreamWriter(dos));
        bw.write(m_pcaSummary);
        bw.flush();
        bw.close();
        bw = null;
      } finally {
        if (bw != null) {
          bw.close();
        }
      }

      // write the serialized PreConstructedPCA filter to HDFS
      statusMessage("Writing serialized PCA filter to HDFS: " + outputDir);
      logMessage("Writing serialized PCA filter to HDFS: " + outputDir);

      p = new Path(outputDir + "/" + arffFileName + "_pca_filter.ser");
      dos = fs.create(p, true);
      ObjectOutputStream os = null;
      try {
        os = new ObjectOutputStream(new BufferedOutputStream(dos));
        os.writeObject(pca);
        os.flush();
        os.close();
        os = null;
      } finally {
        if (os != null) {
          os.close();
        }
      }
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }
  }

  @Override
  public String getText() {
    return m_pcaSummary;
  }

  @Override
  public Image getImage() {
    return m_correlationHeatMap;
  }

  /**
   * Main method for executing this job from the command line
   * 
   * @param args arguments to the job
   */
  public static void main(String[] args) {

    CorrelationMatrixHadoopJob job = new CorrelationMatrixHadoopJob();
    job.run(job, args);
  }

  @Override
  public void run(Object toRun, String[] args) {

    if (!(toRun instanceof CorrelationMatrixHadoopJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a CorrelationMatrixHadoopJob!");
    }

    try {
      CorrelationMatrixHadoopJob job = (CorrelationMatrixHadoopJob) toRun;

      if (Utils.getFlag('h', args)) {
        String help = DistributedJob.makeOptionsStr(job);
        System.err.println(help);
        System.exit(1);
      }

      job.setOptions(args);
      job.runJob();

      if (!DistributedJobConfig.isEmpty(getText())) {
        System.out.println(getText());
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
