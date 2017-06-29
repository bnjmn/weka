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
 *    CorrelationMatrixSparkJob
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
import scala.Tuple2;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instance;
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

import java.awt.Image;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Spark job for constructing a correlation matrix. Has an option to use the
 * matrix constructed for performing a (non-distributed) PCA analysis.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 12253 $
 */
public class CorrelationMatrixSparkJob extends SparkJob implements
  TextProducer, ImageProducer, CommandlineRunnable {

  /** For serialization */
  private static final long serialVersionUID = 5807216718573114917L;

  /** The name of the subdir in the output directory for this job */
  public static final String OUTPUT_SUBDIR = "correlation";

  /** ARFF job */
  protected ArffHeaderSparkJob m_arffHeaderJob = new ArffHeaderSparkJob();

  /** Holds the options to the header task */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /** Stores options to the correlation map task */
  protected String m_correlationMapTaskOpts = "";

  /** Class index or name */
  protected String m_classIndex = "";

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

  // /**
  // * Header information that might be supplied by a parent/previous job (that
  // * has already invoked the ARFF job)
  // */
  // protected Instances m_headerWithSummary;

  /**
   * Constructor
   */
  public CorrelationMatrixSparkJob() {
    super("Correlation matrix job",
      "Compute a correlation or covariance matrix");
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
      "\tRun PCA analysis and build a PCA filter when job completes.", "pca",
      0, "-pca"));

    CorrelationMatrixMapTask tempTask = new CorrelationMatrixMapTask();
    Enumeration<Option> tOpts = tempTask.listOptions();
    while (tOpts.hasMoreElements()) {
      result.add(tOpts.nextElement());
    }

    result.add(new Option("", "", 0,
      "\nOptions specific to ARFF header creation:"));

    ArffHeaderSparkJob tempArffJob = new ArffHeaderSparkJob();
    Enumeration<Option> arffOpts = tempArffJob.listOptions();
    while (arffOpts.hasMoreElements()) {
      result.add(arffOpts.nextElement());
    }

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    setRunPCA(Utils.getFlag("pca", options));

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

    return options.toArray(new String[options.size()]);
  }

  /**
   * Build the correlation matrix and write it to the output destination
   * 
   * @param dataset the input RDD dataset to use
   * @param headerWithSummary the header of the data (with summary attributes)
   * @param outputPath the path to write results to
   * @throws Exception if a problem occurs
   */
  protected void buildMatrix(JavaRDD<Instance> dataset,
    final Instances headerWithSummary, String outputPath) throws Exception {

    String matrixMapOpts = getCorrelationMapTaskOptions();
    String[] mapOpts = null;
    if (!DistributedJobConfig.isEmpty(matrixMapOpts)) {
      mapOpts = Utils.splitOptions(environmentSubstitute(matrixMapOpts));
    }
    final String[] fMapOpts = mapOpts;

    // construct a temporary map task in order to determine how
    // many rows there will be in the matrix (after deleting any
    // nominal atts and potentially the class att)
    CorrelationMatrixMapTask tempTask = new CorrelationMatrixMapTask();
    if (fMapOpts != null) {
      tempTask.setOptions(fMapOpts.clone());
    }
    final boolean missingReplacedWithMean = !tempTask.getIgnoreMissingValues();
    final boolean covarianceInsteadOfCorrelation = tempTask.getCovariance();
    final boolean deleteClassIfSet = !tempTask.getKeepClassAttributeIfSet();

    tempTask.setup(headerWithSummary);
    final int numRowsInMatrix = tempTask.getMatrix().length;

    JavaPairRDD<Integer, MatrixRowHolder> mapToPartialRows =
      dataset
        .mapPartitionsToPair(
          new PairFlatMapFunction<Iterator<Instance>, Integer, MatrixRowHolder>() {

            /** For serialization */
            private static final long serialVersionUID = -3024936415666668127L;

            protected List<Tuple2<Integer, MatrixRowHolder>> m_partialRows =
              new ArrayList<Tuple2<Integer, MatrixRowHolder>>();

            @Override
            public Iterable<Tuple2<Integer, MatrixRowHolder>> call(
              Iterator<Instance> split) throws DistributedWekaException {

              CorrelationMatrixMapTask task = new CorrelationMatrixMapTask();
              try {
                if (fMapOpts != null) {
                  task.setOptions(fMapOpts);
                }
                task.setup(headerWithSummary);

                while (split.hasNext()) {
                  task.processInstance(split.next());
                }

                // output all the rows in this partial matrix
                double[][] partialMatrix = task.getMatrix();
                int[][] coOcc = task.getCoOccurrenceCounts();
                for (int i = 0; i < partialMatrix.length; i++) {
                  double[] row = partialMatrix[i];
                  int[] co = null;
                  if (coOcc != null) {
                    co = coOcc[i];
                  }                  MatrixRowHolder rh = new MatrixRowHolder(i, row, co);
                  m_partialRows
                    .add(new Tuple2<Integer, MatrixRowHolder>(i, rh));
                }
              } catch (Exception ex) {
                throw new DistributedWekaException(ex);
              }

              return m_partialRows;
            }
          }).sortByKey()
        .partitionBy(new IntegerKeyPartitioner(numRowsInMatrix))
        .persist(getCachingStrategy().getStorageLevel());

    JavaPairRDD<Integer, double[]> aggregatedRows =
      mapToPartialRows
        .mapPartitionsToPair(new PairFlatMapFunction<Iterator<Tuple2<Integer, MatrixRowHolder>>, Integer, double[]>() {

          /** For serialization */
          private static final long serialVersionUID = -1290972198473290092L;
          protected List<Tuple2<Integer, double[]>> result =
            new ArrayList<Tuple2<Integer, double[]>>();

          @Override
          public Iterable<Tuple2<Integer, double[]>> call(
            Iterator<Tuple2<Integer, MatrixRowHolder>> split)
            throws DistributedWekaException {

            List<double[]> partials = new ArrayList<double[]>();
            List<int[]> partialCoOcc = new ArrayList<int[]>();
            int rowNum = -1;
            while (split.hasNext()) {
              Tuple2<Integer, MatrixRowHolder> nextRow = split.next();
              if (rowNum < 0) {
                rowNum = nextRow._2().getRowNumber();
              } else {
                if (nextRow._2().getRowNumber() != rowNum) {
                  throw new DistributedWekaException(
                    "Was not expecting the matrix row number "
                      + "to change within a partition!");
                }

                partials.add(nextRow._2().getRow());
                if (!missingReplacedWithMean) {
                  partialCoOcc.add(nextRow._2().getCoOccurrencesCounts());
                }
              }
            }

            if (partials.size() > 0) {
              CorrelationMatrixRowReduceTask reducer =
                new CorrelationMatrixRowReduceTask();

              double[] aggregated =
                reducer.aggregate(rowNum, partials, partialCoOcc,
                  headerWithSummary, missingReplacedWithMean,
                  covarianceInsteadOfCorrelation, deleteClassIfSet);
              result.add(new Tuple2<Integer, double[]>(rowNum, aggregated));
            }

            return result;
          }
        });

    List<Tuple2<Integer, double[]>> reducedRows = aggregatedRows.collect();
    mapToPartialRows.unpersist();

    double[][] m = new double[reducedRows.size()][reducedRows.size()];
    for (Tuple2<Integer, double[]> row : reducedRows) {
      int i = row._1();
      double[] js = row._2();

      for (int j = 0; j < js.length; j++) {
        m[i][j] = js[j];
        m[j][i] = js[j];
      }
    }

    m_finalMatrix = new weka.core.matrix.Matrix(m);

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
    try {
      writeMatrixToOutput(outputPath, reducedRows,
        headerNoSummary, deleteClassIfSet);
    } catch (Exception ex) {
      logMessage(ex);
      throw new DistributedWekaException(ex);
    }

    if (getRunPCA()) {
      runPCA(outputPath, covarianceInsteadOfCorrelation, !deleteClassIfSet,
        headerWithSummary, headerNoSummary);
    }
  }

  /**
   * Run a local PCA analysis using the computed matrix
   * 
   * @param outputPath the path to write the serialized PCA filter and textual
   *          results to
   * @param isCov true if the matrix is a covariance one, rather than a
   *          correlation one
   * @param keepClass true if the class attribute is to be retained
   * @param header the header of the data (with summary attributes)
   * @param headerNoSummary the header of the data (sans summary attributes)
   * @throws DistributedWekaException if a problem occurs
   */
  protected void runPCA(String outputPath, boolean isCov, boolean keepClass,
    Instances header, Instances headerNoSummary)
    throws DistributedWekaException {
    header.setClassIndex(headerNoSummary.classIndex());

    try {
      PreConstructedPCA pca =
        new PreConstructedPCA(header, m_finalMatrix, keepClass, isCov);

      // this triggers the computation of the PCA analysis
      pca.setInputFormat(CSVToARFFHeaderReduceTask.stripSummaryAtts(header));

      m_pcaSummary = pca.toString();

      // write the textual summary to the output
      logMessage("Writing PCA summary to " + outputPath);
      String summaryPath =
        outputPath
          + (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
          + "pca_summary.txt";

      BufferedWriter bw = null;
      try {
        bw =
          new BufferedWriter(new OutputStreamWriter(
            openFileForWrite(summaryPath)));
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
      logMessage("Writing serialized PCA filter " + outputPath);
      String pcaPath =
        outputPath
          + (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
          + "pca_filter.ser";

      ObjectOutputStream os = null;
      try {
        os =
          new ObjectOutputStream(new BufferedOutputStream(
            openFileForWrite(pcaPath)));
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
      logMessage(ex);
      throw new DistributedWekaException(ex);
    }
  }

  protected void writeMatrixToOutput(String outputPath,
    List<Tuple2<Integer, double[]>> reducedRows, Instances headerNoSummary,
    boolean deleteClassIfSet)
    throws Exception {
    String outputPathStandard =
      outputPath
        + (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
        + "matrix.txt";

    String outputPathTuple =
      outputPath
        + (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
        + "matrix_tuple.txt";

    String outputPathNames =
      outputPath
        + (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
        + "matrix_labels.txt";

    String outputPathHeatmap =
      outputPath
        + (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
        + "matrix_heatmap.png";

    logMessage("[CorrelationMatrixJob] Writing correlation matrix to output");
    OutputStreamWriter os =
      new OutputStreamWriter(openFileForWrite(outputPathStandard));

    BufferedWriter bw = null;
    try {
      bw = new BufferedWriter(os);
      m_finalMatrix.write(bw);
      bw.close();
      bw = null;
    } finally {
      if (bw != null) {
        bw.close();
      }
    }

    // now write it out in tuple format
    logMessage("[CorrelationMatrixJob] Writing correlation matrix, in tuple format, to output");
    os = new OutputStreamWriter(openFileForWrite(outputPathTuple));
    PrintWriter pw = null;
    try {
      bw = new BufferedWriter(os);
      pw = new PrintWriter(bw);
      for (Tuple2<Integer, double[]> row : reducedRows) {
        int i = row._1();
        double[] js = row._2();

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
    String classAtt = environmentSubstitute(getClassAttribute());
    WekaClassifierSparkJob.setClassIndex(classAtt, headerNoSummary, false);

    StringBuilder rem = new StringBuilder();
    if (headerNoSummary.classIndex() >= 0 && deleteClassIfSet) {
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
        logMessage(ex);
        throw new DistributedWekaException(ex);
      }
    }

    logMessage("[CorrelationMatrixJob] Writing matrix row labels to output");
    List<String> rowLabels = new ArrayList<String>();
    try {

      os = new OutputStreamWriter(openFileForWrite(outputPathNames));
      bw = null;
      try {
        bw = new BufferedWriter(os);
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
      logMessage(ex);
      throw new DistributedWekaException(ex);
    }

    // write the heat map image to the output
    logMessage("[CorrelationMatrixJob] Writing heatmap image to output");
    m_correlationHeatMap =
      CorrelationMatrixRowReduceTask.getHeatMapForMatrix(m_finalMatrix,
        rowLabels);

    OutputStream o = null;
    try {
      o = openFileForWrite(outputPathHeatmap);
      CorrelationMatrixRowReduceTask.writeHeatMapImage(m_correlationHeatMap,
        o);
    } finally {
      if (o != null) {
        o.close();
      }
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

    JavaRDD<Instance> dataSet = null;
    Instances headerWithSummary = null;
    if (getDataset(TRAINING_DATA) != null) {
      dataSet = (JavaRDD<Instance>) getDataset(TRAINING_DATA).getRDD();
      headerWithSummary = getDataset(TRAINING_DATA).getHeaderWithSummary();
      logMessage("RDD<Instance> dataset provided: "
        + dataSet.partitions().size() + " partitions.");
      logMessage("Current caching strategy: " + getCachingStrategy());
    }

    if (dataSet == null && headerWithSummary == null) {
      logMessage("Invoking ARFF Job...");
      m_arffHeaderJob.setEnvironment(m_env);
      m_arffHeaderJob.setLog(getLog());
      m_arffHeaderJob.setStatusMessagePrefix(m_statusMessagePrefix);

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
      dataSet = d.getRDD();
      headerWithSummary = d.getHeaderWithSummary();
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
      WekaClassifierSparkJob.setClassIndex(classAtt, headerNoSummary, false);
    } catch (Exception ex) {
      logMessage(ex);
      throw new DistributedWekaException(ex);
    }

    //
    // if (dataSet == null) {
    // dataSet =
    // loadInput(inputFile, headerNoSummary, getCSVMapTaskOptions(),
    // sparkContext, getCachingStrategy(), minSlices, true);
    // }

    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);

    // clean the output directory
    SparkJob.deleteDirectory(outputPath);

    // do the actual work!
    try {
      buildMatrix(dataSet, headerWithSummary, outputPath);
    } catch (Exception e) {
      logMessage(e);
      throw new DistributedWekaException(e);
    }
    setJobStatus(JobStatus.FINISHED);

    return true;
  }

  @Override public Image getImage() {
    return m_correlationHeatMap;
  }

  @Override public String getText() {
    return m_pcaSummary;
  }

  /**
   * A container class for holding a partially computed row of the
   * correlation/covariance matrix along with the row number.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * 
   */
  protected static class MatrixRowHolder implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = -3722225020174913388L;

    /** The row number of this row in the matrix */
    protected int m_rowNumber;

    /** The row itself */
    protected double[] m_row;

    /**
     * If missings are not being replaced, then this holds the count of
     * co-occurrences
     */
    protected int[] m_coCoccurrences;

    /**
     * Construct a new MatrixRowHolder
     * 
     * @param rowNum the number of this row of the matrix
     * @param row the row itself
     * @param coOccurrences the co-occurrence counts (if missings are not
     *          replaced with means)
     */
    public MatrixRowHolder(int rowNum, double[] row, int[] coOccurrences) {
      m_rowNumber = rowNum;
      m_row = row;
      m_coCoccurrences = coOccurrences;
    }

    /**
     * Construct a new MatrixRowHolder
     * 
     * @param rowNum the number of this row of the matrix
     * @param row the row itself
     */
    public MatrixRowHolder(int rowNum, double[] row) {
      this(rowNum, row, null);
    }

    /**
     * Get the row number
     * 
     * @return the row number
     */
    public int getRowNumber() {
      return m_rowNumber;
    }

    /**
     * Get the row
     * 
     * @return the row
     */
    public double[] getRow() {
      return m_row;
    }

    /**
     * Get the co-occurrences counts, or null if missings were replaced with
     * means
     * 
     * @return the co-occurrences counts for this row, or null if missings were
     *         replaced with means
     */
    public int[] getCoOccurrencesCounts() {
      return m_coCoccurrences;
    }
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {

    if (!(toRun instanceof CorrelationMatrixSparkJob)) {
      throw new IllegalArgumentException(
        "Object to run is not an WekaClassifierSparkJob!");
    }

    try {
      CorrelationMatrixSparkJob cmsj = (CorrelationMatrixSparkJob) toRun;
      if (Utils.getFlag('h', options)) {
        String help = DistributedJob.makeOptionsStr(cmsj);
        System.err.println(help);
        System.exit(1);
      }

      cmsj.setOptions(options);
      cmsj.runJob();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static void main(String[] args) {
    CorrelationMatrixSparkJob cmsj = new CorrelationMatrixSparkJob();
    cmsj.run(cmsj, args);
  }

}
