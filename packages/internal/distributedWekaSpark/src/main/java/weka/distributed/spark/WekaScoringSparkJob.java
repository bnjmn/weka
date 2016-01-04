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
 *    WekaScoringSparkJob
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.broadcast.Broadcast;
import weka.core.Attribute;
import weka.core.CommandlineRunnable;
import weka.core.DenseInstance;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Range;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.WekaScoringMapTask;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

//import org.apache.spark.streaming.api.java.JavaDStream;
// import org.apache.spark.streaming.api.java.JavaStreamingContext;

/**
 * Spark job for scoring new data using an existing Weka classifier or clusterer
 * model.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaScoringSparkJob extends SparkJob implements
  CommandlineRunnable {

  /** The subdirectory of the output directory to save the job outputs to */
  protected static final String OUTPUT_SUBDIR = "/scoring";

  /** For serialization */
  private static final long serialVersionUID = 5083591225675824700L;

  /** The columns from the original input data to output in the scoring results */
  protected String m_colsRange = "first-last";

  /** The path (HDFS or local) to the model to use for scoring */
  protected String m_modelPath = "";

  /** ARFF header job (if necessary) */
  protected ArffHeaderSparkJob m_arffHeaderJob = new ArffHeaderSparkJob();

  /** Options to the ARFF header job */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /** Encapsulates the loaded/provided model */
  protected WekaScoringMapTask m_scoringTask;

  /** The header of the dataset to be scored */
  protected Instances m_headerNoSummary;

  /** The header of the dataset used to train the model */
  protected Instances m_modelHeader;

  /** The format of the scored data */
  protected Instances m_scoredOutputFormat;

  /**
   * The columns of the incoming data to keep in the output (if null, then will
   * keep all columns)
   */
  protected int[] m_selectedIndices;

  /**
   * A threshold to apply to the value of a prediction - predicted instances
   * below the threshold do not make it into the output. Takes the format:
   * {@code [label|index]:<double>}. The label or index is the name of a class label (or
   * zero-based index of the label) respectively. The label or index can be
   * omitted entirely in the case of a numeric target; in the case of a nominal
   * target, the first label (index 0) is assumed.
   * 
   * If this option is unspecified, then no threshold is applied.
   */
  protected String m_classPredictionThreshold = "";

  /**
   * Constructor
   */
  public WekaScoringSparkJob() {
    super("Scoring job", "Score data with a Weka model");
  }

  /**
   * Helper method to load the model to score with
   *
   * @param modelPath the name of the model file
   * @return a list containing the model and the header of the data it was built
   *         with
   * @throws IOException if a problem occurs
   */
  public static List<Object> loadModel(String modelPath) throws IOException {

    InputStream is = openFileForRead(modelPath);

    ObjectInputStream ois = null;
    List<Object> result = new ArrayList<Object>();
    try {
      ois = new ObjectInputStream(new BufferedInputStream(is));

      try {
        Object model = ois.readObject();
        result.add(model);
      } catch (ClassNotFoundException ex) {
        throw new IOException(ex);
      }
      Instances header = null;
      try {
        header = (Instances) ois.readObject();
        result.add(header);
      } catch (Exception ex) {
        // we won't complain if there is no header in the stream
      }

      ois.close();
      ois = null;
    } finally {
      if (ois != null) {
        ois.close();
      }
    }

    return result;
  }

  public static void main(String[] args) {
    WekaScoringSparkJob wsj = new WekaScoringSparkJob();
    wsj.run(wsj, args);
  }

  /**
   * Set the model to use when scoring and intitalizes the output format
   *
   * @param model the model to use (supports classifiers and clusterers)
   * @param modelHeader the header of the data used to train the model
   * @param dataHeader the header of the incoming data (sans any summary
   *          attributes)
   * @throws DistributedWekaException if a problem occurs
   */
  public void
    setModel(Object model, Instances modelHeader, Instances dataHeader)
      throws DistributedWekaException {

    logMessage("[WekaScoringSparkJob] constructing scoring task for "
      + model.getClass().getName());
    m_scoringTask = new WekaScoringMapTask();
    m_scoringTask.setModel(model, modelHeader, dataHeader);
    m_headerNoSummary = dataHeader;
    m_modelHeader = modelHeader;

    // now setup the format of the output RDD<Instances>
    String colsToOutput =
      environmentSubstitute(getColumnsToOutputInScoredData());
    if (!DistributedJobConfig.isEmpty(colsToOutput)
      && !colsToOutput.equalsIgnoreCase("first-last")) {
      Range r = new Range();
      r.setRanges(colsToOutput);
      r.setUpper(m_headerNoSummary.numAttributes() - 1);
      m_selectedIndices = r.getSelection();
    }

    logMessage("[WekaScoringSparkJob] settup up output format of scored data");
    List<String> predictionLabels = m_scoringTask.getPredictionLabels();
    ArrayList<Attribute> outAtts = new ArrayList<Attribute>();
    if (m_selectedIndices == null) {
      for (int i = 0; i < m_headerNoSummary.numAttributes(); i++) {
        outAtts.add((Attribute) m_headerNoSummary.attribute(i).copy());
      }
    } else {
      for (int i : m_selectedIndices) {
        outAtts.add((Attribute) m_headerNoSummary.attribute(i).copy());
      }
    }

    for (int i = 0; i < predictionLabels.size(); i++) {
      outAtts.add(new Attribute("pred_" + predictionLabels.get(i)));
    }

    m_scoredOutputFormat = new Instances("ScoredData", outAtts, 0);
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
   * Tip text for this property.
   *
   * @return the tip text for this property.
   */
  public String classPredictionThresholdTipText() {
    return "A threshold to apply to the value of a prediction - predicted instances "
      + "below the threshold do not make it into the output. Takes the format: "
      + "[label|index]:<double>. The label or index is the name of a class label (or "
      + "zero-based index of the label) respectively. The label or index can be "
      + "omitted entirely in the case of a numeric target; in the case of a nominal "
      + "target, the first label (index 0) is assumed. If this option is unspecified, "
      + "then no threshold is applied.";
  }

  /**
   * Get the prediction threshold to apply - predicted instances below the
   * threshold do not make it into the output. Takes the format:
   * {@code [label|index]:<double>}. The label or index is the name of a class label (or
   * zero-based index of the label) respectively. The label or index can be
   * omitted entirely in the case of a numeric target; in the case of a nominal
   * target, the first label (index 0) is assumed.
   *
   * If this option is unspecified, then no threshold is applied.
   *
   * @return the prediction threshold to apply
   */
  public String getClassPredictionThreshold() {
    return m_classPredictionThreshold;
  }

  /**
   * Set the prediction threshold to apply - predicted instances below the
   * threshold do not make it into the output. Takes the format:
   * {@code [label|index]:<double>}. The label or index is the name of a class label (or
   * zero-based index of the label) respectively. The label or index can be
   * omitted entirely in the case of a numeric target; in the case of a nominal
   * target, the first label (index 0) is assumed.
   *
   * If this option is unspecified, then no threshold is applied.
   *
   * @param thresh the prediction threshold to apply
   */
  public void setClassPredictionThreshold(String thresh) {
    m_classPredictionThreshold = thresh;
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
   * Get the path (HDFS or local) to the model to use for scoring.
   *
   * @return the path to the model to use for scoring
   */
  public String getModelPath() {
    return m_modelPath;
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
   * Get the columns to output (as a comma-separated list of indexes) in the
   * scored data. 'first' and 'last' may be used as well (e.g. 1,2,10-last).
   *
   * @return the columns to output in the scored data
   */
  public String getColumnsToOutputInScoredData() {
    return m_colsRange;
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
   * Get the options for the ARFF header job
   *
   * @return the options for the ARFF header job
   */
  public String getCSVMapTaskOptions() {
    return m_wekaCsvToArffMapTaskOpts;
  }

  /**
   * Set the options for the ARFF header job
   *
   * @param opts options for the ARFF header job
   */
  public void setCSVMapTaskOptions(String opts) {
    m_wekaCsvToArffMapTaskOpts = opts;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option(
      "\tPath to model file to use for scoring (can be \n\t"
        + "local or in HDFS)", "model-file", 1,
      "-model-file <path to model file>"));

    result.add(new Option(
      "\tColumns to output in the scored data. Specify as\n\t"
        + "a range, e.g. 1,4,5,10-last (default = first-last).",
      "columns-to-output", 1, "-columns-to-output <cols>"));

    result
      .add(new Option(
        "\tThreshold to apply to the predicted scores. Predicted instances"
          + "\n\tbelow the threshold do not make it into the output. Takes the format: "
          + "\n\t[label|index]:<double>. The label or index is the name of a class label (or "
          + "\n\tzero-based index of the label) respectively. The label or index can be "
          + "\n\tomitted entirely in the case of a numeric target; in the case of a nominal "
          + "\n\ttarget, the first label (index 0) is assumed. If this option is unspecified, "
          + "\n\tthen no threshold is applied.", "threshold", 1,
        "-threshold [name | index]:<double>"));

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
    for (String s : getJobOptionsOnly()) {
      options.add(s);
    }

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

  @Override
  public void setOptions(String[] options) throws Exception {
    String modelPath = Utils.getOption("model-file", options);
    setModelPath(modelPath);

    String range = Utils.getOption("columns-to-output", options);
    setColumnsToOutputInScoredData(range);

    String threshold = Utils.getOption("threshold", options);
    setClassPredictionThreshold(threshold);

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

  public String[] getJobOptionsOnly() {
    List<String> options = new ArrayList<String>();

    if (!DistributedJobConfig.isEmpty(getModelPath())) {
      options.add("-model-file");
      options.add(getModelPath());
    }

    if (!DistributedJobConfig.isEmpty(getColumnsToOutputInScoredData())) {
      options.add("-columns-to-output");
      options.add(getColumnsToOutputInScoredData());
    }

    if (!DistributedJobConfig.isEmpty(getClassPredictionThreshold())) {
      options.add("-threshold");
      options.add(getClassPredictionThreshold());
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Create and configure a ScoringFlatMapFunction using the current option
   * settings and model.
   *
   * @param sparkContext the context to use
   * @return a configured ScoringFlatMapFunction
   * @throws DistributedWekaException
   */
  protected ScoringFlatMapFunction createScoringFunction(
    JavaSparkContext sparkContext) throws DistributedWekaException {
    int threshIndex = -1;
    double thresh = 0;
    if (!DistributedJobConfig.isEmpty(getClassPredictionThreshold())) {
      String t = environmentSubstitute(getClassPredictionThreshold());
      String[] parts = t.split(":");
      threshIndex = 0;
      if (parts.length == 2) {
        boolean set = false;
        if (m_scoringTask.modelIsAClassifier()) {
          Attribute classAtt = m_modelHeader.classAttribute();

          // try a named label first
          if (classAtt.indexOfValue(parts[0]) >= 0) {
            set = true;
            threshIndex = classAtt.indexOfValue(parts[0]);
          }
        }

        if (!set) {
          // must be a cluster or user has specified an index...
          try {
            threshIndex = Integer.parseInt(parts[0]);
          } catch (NumberFormatException ex) {
            throw new DistributedWekaException(ex);
          }
        }
      }

      String threshToParse = parts.length == 2 ? parts[1] : parts[0];
      try {
        thresh = Double.parseDouble(threshToParse);
      } catch (NumberFormatException ex) {
        throw new DistributedWekaException(ex);
      }
    }

    // model could be large, so we'll broadcast it to the nodes
    final Broadcast<WekaScoringMapTask> broadcastModel =
      sparkContext.broadcast(m_scoringTask);

    ScoringFlatMapFunction scoring =
      new ScoringFlatMapFunction(broadcastModel, m_scoredOutputFormat,
        threshIndex, thresh, m_selectedIndices);

    return scoring;
  }

  /**
   * Score the supplied batch dataset using the model
   *
   * @param dataset the dataset to score
   * @param sparkContext the spark context to use
   * @return the scored data as a new RDD
   * @throws Exception if a problem occurs
   */
  protected JavaRDD<Instance> scoreDataBatch(JavaRDD<Instance> dataset,
    JavaSparkContext sparkContext) throws Exception {

    if (m_scoringTask == null) {
      throw new DistributedWekaException(
        "No model has been set for scoring with");
    }

    ScoringFlatMapFunction scoring = createScoringFunction(sparkContext);

    JavaRDD<Instance> scoredDataset = dataset.mapPartitions(scoring);

    return scoredDataset;
  }

  /*
   * Score the supplied streaming dataset using the model
   *
   * @param streamingContext the streaming context to use
   * @param streamingData the streaming RDD
   * @return the scored data as a new JavaDStream
   * @throws DistributedWekaException if a problem occurs
   *
  public JavaDStream<Instance> scoreDataStreaming(
    JavaStreamingContext streamingContext, JavaDStream<Instance> streamingData)
    throws DistributedWekaException {

    if (m_scoringTask == null) {
      throw new DistributedWekaException(
        "No model has been set for scoring with");
    }

    ScoringFlatMapFunction scoring =
      createScoringFunction(streamingContext.sparkContext());
    JavaDStream<Instance> scoredData = streamingData.mapPartitions(scoring);

    return scoredData;
  } */

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
      dataSet = ((Dataset<Instance>) getDataset(TRAINING_DATA)).getDataset();
      headerWithSummary = getDataset(TRAINING_DATA).getHeaderWithSummary();
      logMessage("RDD<Instance> dataset provided: "
        + dataSet.partitions().size() + " partitions.");
    }

    // header job necessary?
    if (dataSet == null && headerWithSummary == null) {
      logMessage("Invoking ARFF Job...");
      m_arffHeaderJob.setEnvironment(m_env);
      m_arffHeaderJob.setLog(getLog());
      m_arffHeaderJob.setStatusMessagePrefix(m_statusMessagePrefix);
      m_arffHeaderJob.setCachingStrategy(getCachingStrategy());

      success = m_arffHeaderJob.runJobWithContext(sparkContext);

      if (!success) {
        setJobStatus(JobStatus.FAILED);
        statusMessage("[WekaScoringSparkJob] Unable to continue - creating the ARFF header failed!");
        logMessage("[WekaScoringSparkJob] Unable to continue - creating the ARFF header failed!");
        return false;
      }

      Dataset<Instance> d =
        (Dataset<Instance>) m_arffHeaderJob.getDataset(TRAINING_DATA);
      headerWithSummary = d.getHeaderWithSummary();
      dataSet = d.getDataset();
      setDataset(TRAINING_DATA, new Dataset<Instance>(dataSet,
        headerWithSummary));
      logMessage("Fetching RDD<Instance> dataset from ARFF job: "
        + dataSet.partitions().size() + " partitions.");
    }

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);

    // clean the output directory
    SparkJob.deleteDirectory(outputPath);

    if (DistributedJobConfig.isEmpty(getModelPath())) {
      throw new DistributedWekaException("No model to score with supplied!");
    }

    List<Object> modelAndHeader =
      loadModel(environmentSubstitute(getModelPath()));
    if (modelAndHeader.size() != 2) {
      throw new DistributedWekaException(
        "Can't continue unless there is both the "
          + "model and the ARFF header of the data it was trained with available!");
    }

    setModel(modelAndHeader.get(0), (Instances) modelAndHeader.get(1),
      headerNoSummary);

    try {
      JavaRDD<Instance> scoredData =
        scoreDataBatch(dataSet, sparkContext).persist(
          getCachingStrategy().getStorageLevel());

      logMessage("[WekaScoringSparkJob] writing scored data to " + outputPath);

      // write scored data...
      writeScoredData(outputPath, scoredData);
    } catch (Exception ex) {
      logMessage(ex);
      throw new DistributedWekaException(ex);
    }

    setJobStatus(JobStatus.FINISHED);
    return true;
  }

  /**
   * Write the supplied data to the output directory
   *
   * @param outputPath the path to write to
   * @param scoredData the data to write
   * @throws IOException if a problem occurs
   */
  protected void
    writeScoredData(String outputPath, JavaRDD<Instance> scoredData)
      throws IOException {
    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(outputPath, pathOnly);

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);
    p = p.makeQualified(fs);

    String finalOutputDir = "";
    if (!p.toString().toLowerCase().startsWith("file:/")) {
      finalOutputDir = p.toString();
    } else {
      finalOutputDir = pathOnly[0];
    }

    scoredData.saveAsTextFile(finalOutputDir);

    // write the header of the scored data out to the output directory in both
    // ARFF and CSV format
    String outArff = outputPath + "/scoredHeader.arff";
    String outCSV = outputPath + "/scoredHeader.csv";

    PrintWriter writer = null;
    try {
      writer = openTextFileForWrite(outArff);
      writer.println(m_scoredOutputFormat);
      writer.flush();
      writer.close();

      writer = openTextFileForWrite(outCSV);
      StringBuilder b = new StringBuilder();
      for (int i = 0; i < m_scoredOutputFormat.numAttributes(); i++) {
        b.append(m_scoredOutputFormat.attribute(i).name()).append(",");
      }
      b.setLength(b.length() - 1);
      writer.println(b.toString());
      writer.flush();
      writer.close();
    } finally {
      if (writer != null) {
        writer.close();
      }
    }

  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {

    if (!(toRun instanceof WekaScoringSparkJob)) {
      throw new IllegalArgumentException(
        "Object to run is not a RandomizedDataSparkJob");
    }

    try {
      WekaScoringSparkJob wsj = (WekaScoringSparkJob) toRun;
      if (Utils.getFlag('h', options)) {
        String help = DistributedJob.makeOptionsStr(wsj);
        System.err.println(help);
        System.exit(1);
      }

      wsj.setOptions(options);
      wsj.runJob();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * FlatMapFunction for scoring RDD partitions.
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  protected static class ScoringFlatMapFunction implements
    FlatMapFunction<Iterator<Instance>, Instance> {

    /** For serialization */
    private static final long serialVersionUID = -8714068105548643581L;

    /**
     * Provide access to the model in the case that it has been broadcast to the
     * nodes
     */
    protected Broadcast<WekaScoringMapTask> m_broadcastModel;

    /** Holds the model in the case that broadcasting is not being used */
    protected WekaScoringMapTask m_task;

    protected Instances m_scoredOutputFormat;
    protected int m_threshIndex = -1;
    protected double m_thresh;
    protected int[] m_selectedIndices;

    /** Holds input instances when the model is a BatchPredictor */
    protected List<Instance> m_scored = new ArrayList<Instance>();

    /**
     * Constructor to use when the model is being broadcast to the nodes
     *
     * @param broadcastModel for obtaining the model
     * @param scoredOutputFormat the output format of the scored data
     * @param threshIndex index of the class label to apply the probability
     *          threshold to - -1 for no thresholding
     * @param thresh the threshold to use (if threshIndex >= 0)
     * @param selectedIndices indices of input attributes to include in the
     *          scored output (null to include all)
     */
    public ScoringFlatMapFunction(
      final Broadcast<WekaScoringMapTask> broadcastModel,
      final Instances scoredOutputFormat, final int threshIndex,
      final double thresh, final int[] selectedIndices) {
      m_broadcastModel = broadcastModel;
      m_scoredOutputFormat = scoredOutputFormat;
      m_threshIndex = threshIndex;
      m_thresh = thresh;
      m_selectedIndices = selectedIndices;
    }

    /**
     * Constructor to use when the model is being broadcast to the nodes. Does not
     * apply a threshold to the probability of a given class label; includes all
     * input attributes in the output.
     *
     * @param scoredOutputFormat the output format of the scored data
     * @param broadcastModel for obtaining the model
     */
    public ScoringFlatMapFunction(Instances scoredOutputFormat,
      final Broadcast<WekaScoringMapTask> broadcastModel) {
      this(broadcastModel, scoredOutputFormat, -1, -1, null);
    }

    /**
     * Constructor to use when the model is not being broadcast. Does not apply
     * a threshold to the probability of a given class label; includes all input
     * attributes in the output
     *
     * @param scoredOutputFormat the output format of the scored data
     * @param task the scoring task to use
     */
    public ScoringFlatMapFunction(Instances scoredOutputFormat,
      WekaScoringMapTask task) {
      m_scoredOutputFormat = scoredOutputFormat;
      m_task = task;
    }

    /**
     * Contruct an output instance given an input one and the model's prediction
     *
     * @param input the input instance
     * @param preds predictions from the model for the input instance
     * @return an output instance
     */
    protected Instance constructOutputInstance(Instance input, double[] preds) {

      double[] vals = new double[m_scoredOutputFormat.numAttributes()];
      if (m_selectedIndices != null) {
        for (int i = 0; i < m_selectedIndices.length; i++) {
          vals[i] = input.value(m_selectedIndices[i]);
        }

        for (int i = m_selectedIndices.length; i < m_scoredOutputFormat
          .numAttributes(); i++) {
          vals[i] = preds[i - m_selectedIndices.length];
        }
      } else {
        for (int i = 0; i < input.numAttributes(); i++) {
          vals[i] = input.value(i);
        }

        for (int i = input.numAttributes(); i < m_scoredOutputFormat
          .numAttributes(); i++) {
          vals[i] = preds[i - input.numAttributes()];
        }
      }

      Instance scoredI =
        input instanceof SparseInstance ? new SparseInstance(input.weight(),
          vals) : new DenseInstance(input.weight(), vals);

      scoredI.setDataset(m_scoredOutputFormat);

      return scoredI;
    }

    @Override
    public Iterable<Instance> call(Iterator<Instance> partition)
      throws Exception {

      WekaScoringMapTask task =
        m_broadcastModel != null ? m_broadcastModel.value() : m_task;
      List<Instance> forBatchPredictors = new ArrayList<Instance>();

      while (partition.hasNext()) {
        Instance toScore = partition.next();

        if (task.isBatchPredictor()) {
          forBatchPredictors.add(toScore);

          double[][] preds = task.processInstanceBatchPredictor(toScore);
          if (preds != null) {
            // hit batch size, now assemble output

            for (int i = 0; i < preds.length; i++) {
              // skip any that don't meet the threshold (if set)
              if (m_threshIndex >= 0) {
                if (preds[i][m_threshIndex] < m_thresh) {
                  continue;
                }
              }
              Instance scored =
                constructOutputInstance(forBatchPredictors.get(i), preds[i]);
              m_scored.add(scored);
            }

            forBatchPredictors.clear();
          }
        } else {
          double[] preds = task.processInstance(toScore);

          if (m_threshIndex < 0 || (preds[m_threshIndex] >= m_thresh)) {
            Instance scored = constructOutputInstance(toScore, preds);
            m_scored.add(scored);
          }
        }
      }

      // flush any remaining batch (if using a batch predictor)
      if (task.isBatchPredictor()) {
        double[][] preds = task.finalizeBatchPrediction();
        if (preds != null) {
          for (int i = 0; i < preds.length; i++) {
            // skip any that don't meet the threshold (if set)
            if (m_threshIndex >= 0) {
              if (preds[i][m_threshIndex] < m_thresh) {
                continue;
              }
            }

            Instance scored =
              constructOutputInstance(forBatchPredictors.get(i), preds[i]);
            m_scored.add(scored);
          }

          forBatchPredictors.clear();
        }
      }

      return m_scored;
    }
  }
}
