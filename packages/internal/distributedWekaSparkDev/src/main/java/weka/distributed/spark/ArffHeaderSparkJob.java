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
 *    ArffHeaderSparkJob
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import weka.core.Attribute;
import weka.core.ChartUtils;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.core.stats.NumericAttributeBinData;
import weka.core.stats.StatsFormatter;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderMapTask.HeaderAndQuantileDataHolder;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.gui.beans.InstancesProducer;
import weka.gui.beans.TextProducer;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

/**
 * A Spark job for creating a unified ARFF header (including summary "meta"
 * attributes).
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 13221 $
 */
public class ArffHeaderSparkJob extends SparkJob implements
  CommandlineRunnable, InstancesProducer, TextProducer {

  /** key for specifying a chart width to use */
  public static final String CHART_WIDTH_KEY = "weka.chart.width";

  /** key for specifying a chart height to use */
  public static final String CHART_HEIGHT_KEY = "weka.chart.height";

  /** Default width for charts */
  public static final int DEFAULT_CHART_WIDTH = 600;

  /** Default height for charts */
  public static final int DEFAULT_CHART_HEIGHT = 400;

  /** Subdirectory in the output directory for storing the ARFF header to */
  public static final String OUTPUT_SUBDIR = "arff";

  /** For serialization */
  private static final long serialVersionUID = 2620022681471920065L;

  /** The final aggregated header */
  protected Instances m_finalHeader;

  /** The summary statistics */
  protected String m_summaryStats = "";

  /** Options to the underlying csv to arff map task */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /**
   * If set, prevents this job from running. The user may have already run a job
   * that generates an ARFF header. It can be re-used by setting this property.
   */
  protected String m_pathToExistingHeader = "";

  /**
   * The name of the file (in the specified output directory) that will hold the
   * header arff file. If not specified a random number is generated as the file
   * name.
   */
  protected String m_outputHeaderFileName = "";

  /** The path to the final header file (if this job runs successfully) */
  protected String m_pathToAggregatedHeader = "";

  /**
   * If there are {@code >=} this number of partitions then we will run a
   * parallel partial reduce (combine) before running the final local reduce to
   * 1 ARFF header
   */
  protected int m_parallelReduceThreshold = 400;

  /** Holds any images generated from the summary attributes */
  protected List<BufferedImage> m_summaryCharts =
    new ArrayList<BufferedImage>();

  /** Holds the names of the attributes in the summary charts */
  protected List<String> m_summaryChartAttNames = new ArrayList<String>();

  /** The datasource to use (if running from the command line) */
  protected DataSource m_dataSource = new CSVDataSource();

  /**
   * True if any data frames converted to RDDs should be unpersisted after
   * processing
   */
  protected boolean m_unpersistDataFrames;

  /**
   * Constructor
   */
  public ArffHeaderSparkJob() {
    super("ARFF instances header job",
      "Create a consolidated ARFF header from the input data");
  }

  /**
   * Load an ARFF header from the supplied path (local or HDFS depending on path
   * spec)
   *
   * @param pathToHeader path to the header to load
   * @return the header as an Instances object
   * @throws IOException if a problem occurs
   */
  protected Instances loadArffHeaderFromPath(String pathToHeader)
    throws IOException {
    InputStream is = openFileForRead(pathToHeader);
    InputStreamReader ir = new InputStreamReader(is);
    Instances header = null;
    try {
      header = new Instances(ir);
    } finally {
      ir.close();
    }

    // check for stats summary
    if (pathToHeader.toLowerCase().endsWith(".arff")) {
      pathToHeader.replace(".arff", "").replace(".ARFF", "");
    }
    if (checkFileExists(pathToHeader)) {
      is = openFileForRead(pathToHeader);
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      try {
        StringBuilder builder = new StringBuilder();
        String line = "";
        while ((line = br.readLine()) != null) {
          builder.append(line).append("\n");
        }
        m_summaryStats = builder.toString();
      } finally {
        br.close();
      }
    }

    return header;
  }

  protected static String loadSummaryStatsFromPath(String pathToHeader)
    throws IOException {
    InputStream is = null;
    String result = "";
    // check for stats summary
    if (pathToHeader.toLowerCase().endsWith(".arff")) {
      pathToHeader.replace(".arff", "").replace(".ARFF", "");
    }
    if (checkFileExists(pathToHeader)) {
      is = openFileForRead(pathToHeader);
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      try {
        StringBuilder builder = new StringBuilder();
        String line = "";
        while ((line = br.readLine()) != null) {
          builder.append(line).append("\n");
        }
        result = builder.toString();
      } finally {
        br.close();
      }
    }

    return result;
  }

  public void setUnpersistDataFrames(boolean unpersist) {
    m_unpersistDataFrames = unpersist;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result
      .add(new Option(
        "\tPath to header file to use. Set this if you have\n\t"
          + "run previous jobs that have generated a header already. Setting this\n\t"
          + "prevents this job from running.", "existing-header", 1,
        "-existing-header <path>"));
    /*
     * result .add(new
     * Option("\tComma separated list of attribute names to use.\n\t" +
     * "Use either this option, -names-file or neither (in which case\n\t" +
     * "attribute names will be generated).", "A", 1, "-A <attribute names>"));
     * result.add(new Option(
     * "\tLocation of a names file to source attribute names\n\t" +
     * "from. Can exist locally or in HDFS. " +
     * "Use either this option, -A or neither (in which case\n\t" +
     * "attribute names will be generated).", "names-file", 1,
     * "-names-file <path to file>"));
     */
    result
      .add(new Option(
        "\tFile name for output ARFF header. Note that this is a name only\n\t"
          + "and not a path. This file will be created in the output directory\n\t"
          + "specified by the -output-path option. (default is a "
          + "randomly generated name)", "header-file-name", 1,
        "-header-file-name <name>"));

    CSVToARFFHeaderMapTask tempTask = new CSVToARFFHeaderMapTask(false, true);
    Enumeration<Option> mtOpts = tempTask.listOptions();
    while (mtOpts.hasMoreElements()) {
      result.addElement(mtOpts.nextElement());
    }

    result.add(new Option("\tFull name of data source", "data-source", 1,
      "-data-source <spec>"));

    result.add(new Option("", "", 0, "\nOptions specific to "
      + m_dataSource.getClass().getCanonicalName() + ":"));

    // CSVToARFFHeaderMapTask tempTask = new CSVToARFFHeaderMapTask(false,
    // true);
    mtOpts = m_dataSource.listOptions();
    while (mtOpts.hasMoreElements()) {
      result.addElement(mtOpts.nextElement());
    }

    /*
     * result.add(new Option("", "", 0,
     * "\nGeneral Spark job configuration options:"));
     * 
     * Enumeration<Option> superOpts = super.listOptions();
     * 
     * while (superOpts.hasMoreElements()) {
     * result.addElement(superOpts.nextElement()); }
     */

    return result.elements();
  }

  /**
   * Get the options specific to this job only.
   *
   * @return the options specific to this job only
   */
  public String[] getJobOptionsOnly() {
    List<String> options = new ArrayList<String>();

    if (!DistributedJobConfig.isEmpty(getPathToExistingHeader())) {
      options.add("-existing-header");
      options.add(getPathToExistingHeader());
    }

    /*
     * if (!DistributedJobConfig.isEmpty(getAttributeNames())) {
     * options.add("-A"); options.add(getAttributeNames()); }
     * 
     * if (!DistributedJobConfig.isEmpty(getAttributeNamesFile())) {
     * options.add("-names-file"); options.add(getAttributeNamesFile()); }
     */

    if (!DistributedJobConfig.isEmpty(getOutputHeaderFileName())) {
      options.add("-header-file-name");
      options.add(getOutputHeaderFileName());
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    // these are options to the hadoop map task (especially the -names-file)
    if (!DistributedJobConfig.isEmpty(getPathToExistingHeader())) {
      options.add("-existing-header");
      options.add(getPathToExistingHeader());
    }

    /*
     * if (!DistributedJobConfig.isEmpty(getAttributeNames())) {
     * options.add("-A"); options.add(getAttributeNames()); }
     * 
     * if (!DistributedJobConfig.isEmpty(getAttributeNamesFile())) {
     * options.add("-names-file"); options.add(getAttributeNamesFile()); }
     */

    if (!DistributedJobConfig.isEmpty(getOutputHeaderFileName())) {
      options.add("-header-file-name");
      options.add(getOutputHeaderFileName());
    }

    options.add("-data-source");
    options.add(m_dataSource.getClass().getName());
    options.addAll(Arrays.asList(m_dataSource.getOptions()));

    // String[] superOpts = super.getOptions();
    // options.addAll(Arrays.asList(superOpts));

    if (!DistributedJobConfig.isEmpty(getCsvToArffTaskOptions())) {
      try {
        String[] csvOpts = Utils.splitOptions(getCsvToArffTaskOptions());
        options.addAll(Arrays.asList(csvOpts));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    // these are options to the hadoop map task (especially the -names-file)

    String existing = Utils.getOption("existing-header", options);
    setPathToExistingHeader(existing);

    // String attNames = Utils.getOption('A', options);
    // setAttributeNames(attNames);

    // String namesFile = Utils.getOption("names-file", options);
    // setAttributeNamesFile(namesFile);

    String outputName = Utils.getOption("header-file-name", options);
    setOutputHeaderFileName(outputName);

    String[] optsCopy = options.clone();
    super.setOptions(options);

    String dsSpec = Utils.getOption("data-source", options);
    if (!DistributedJobConfig.isEmpty(dsSpec)) {
      m_dataSource =
        (DataSource) Utils.forName(DataSource.class,
          environmentSubstitute(dsSpec), null);
    }
    m_dataSource.setOptions(optsCopy);

    // any options to pass on to the underlying Weka csv to arff map task?
    CSVToARFFHeaderMapTask tempMap = new CSVToARFFHeaderMapTask(false, true);
    tempMap.setOptions(options);

    String optsToWekaMapTask = Utils.joinOptions(tempMap.getOptions());
    if (!DistributedJobConfig.isEmpty(optsToWekaMapTask)) {
      setCsvToArffTaskOptions(optsToWekaMapTask);
    }
  }

  /**
   * The tip text for this property
   *
   * @return the tip text for this property
   */
  public String pathToExistingHeaderTipText() {
    return "The path to an existing ARFF header to use (if set "
      + "this prevents the ARFF header job from running).";
  }

  /**
   * Get the path to an previously created header file to use. If set this
   * prevents the job from running
   *
   * @return the path to a previously created header
   */
  public String getPathToExistingHeader() {
    return m_pathToExistingHeader;
  }

  /**
   * Set the path to an previously created header file to use. If set this
   * prevents the job from running
   *
   * @param path the path to a previously created header
   */
  public void setPathToExistingHeader(String path) {
    m_pathToExistingHeader = path;
  }

  /**
   * The tip text for this property
   *
   * @return the tip text for this property
   */
  public String outputHeaderFileNameTipText() {
    return "The name of the header file (filename only, not path) to create in "
      + "the output directory for the job. If not specified then a name is "
      + "generated automatically.";
  }

  /**
   * Get the name of the header file to create in the output directory. If left
   * unset then a name is generated automatically.
   *
   * @return the name for the ARFF header file
   */
  public String getOutputHeaderFileName() {
    return m_outputHeaderFileName;
  }

  /**
   * Set the name of the header file to create in the output directory. If left
   * unset then a name is generated automatically.
   *
   * @param name the name for the ARFF header file
   */
  public void setOutputHeaderFileName(String name) {
    m_outputHeaderFileName = name;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String csvToArffTaskOptionsTipText() {
    return "Options to pass on to the underlying csv to arff task";
  }

  /**
   * Get the options to pass on to the underlying csv to arff task
   *
   * @return options to pass on to the csv to arff map and reduce tasks
   */
  public String getCsvToArffTaskOptions() {
    return m_wekaCsvToArffMapTaskOpts;
  }

  /**
   * Set the options to pass on to the underlying csv to arff task
   *
   * @param opts options to pass on to the csv to arff map and reduce tasks
   */
  public void setCsvToArffTaskOptions(String opts) {
    m_wekaCsvToArffMapTaskOpts = opts;
  }

  /**
   * Check to see if the user-supplied existing header file does actual exist on
   * the file system (checks in HDFS if the existing path starts with
   * "hdfs://").
   *
   * @return true if the user-supplied existing header file does actually exist
   *         on the file system
   * @throws IOException if a problem occurs
   */
  protected boolean handleExistingHeaderFile() throws IOException {
    String existingPath = environmentSubstitute(getPathToExistingHeader());

    return SparkJob.checkFileExists(existingPath);
  }

  /**
   * Write the final header out to the destination (local or HDFS depending on
   * output directory specification).
   *
   * @throws IOException if a problem occurs
   */
  protected void writeHeaderToOutput() throws IOException {

    OutputStream os = openFileForWrite(m_pathToAggregatedHeader);
    BufferedWriter br = new BufferedWriter(new OutputStreamWriter(os));
    PrintWriter pr = new PrintWriter(br);
    try {
      pr.print(getInstances());
      pr.print("\n");
      pr.flush();
    } finally {
      pr.close();
    }
  }

  /**
   * Writes attribute summary png files to the output directory (local or HDFS
   * depending on output directory specification) if user has opted to compute
   * quartiles. NOTE, does not overwrite any existing png files in the output
   * directory.
   *
   * @param outputPath the path to the output directory
   * @throws IOException if a problem occurs
   * @throws DistributedWekaException if a problem occurs
   */
  protected void writeChartsToOutput(String outputPath) throws IOException,
    DistributedWekaException {

    Instances headerWithSummary = getInstances();
    Instances headerWithoutSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
    Map<Integer, NumericAttributeBinData> numericBinStats =
      new HashMap<Integer, NumericAttributeBinData>();

    for (int i = 0; i < headerWithoutSummary.numAttributes(); i++) {
      Attribute a = headerWithoutSummary.attribute(i);
      if (a.isNumeric()) {
        Attribute summary =
          headerWithSummary
            .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
              + a.name());
        NumericAttributeBinData binData =
          new NumericAttributeBinData(a.name(), summary, -1);

        numericBinStats.put(i, binData);
      }
    }

    for (int i = 0; i < headerWithoutSummary.numAttributes(); i++) {
      if (!headerWithoutSummary.attribute(i).isNominal()
        && !headerWithoutSummary.attribute(i).isNumeric()) {
        continue;
      }
      String name = headerWithoutSummary.attribute(i).name();
      Attribute summary =
        headerWithSummary
          .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
            + name);
      if (summary == null) {
        logMessage("[ArffHeaderSparkJob] Can't find summary attribute for "
          + "attribute: " + name);
        continue;
      }

      String fileName =
        outputPath
          + (outputPath.toLowerCase().contains("://") ? "/" : File.separator)
          + name + ".png";

      int chartWidth = DEFAULT_CHART_WIDTH;
      String userWidth = m_sjConfig.getUserSuppliedProperty(CHART_WIDTH_KEY);
      if (!DistributedJobConfig.isEmpty(userWidth)) {
        try {
          chartWidth = Integer.parseInt(environmentSubstitute(userWidth));
        } catch (NumberFormatException ex) {
        }
      }
      int chartHeight = DEFAULT_CHART_HEIGHT;
      String userHeight = m_sjConfig.getUserSuppliedProperty(CHART_HEIGHT_KEY);
      if (!DistributedJobConfig.isEmpty(userHeight)) {
        try {
          chartHeight = Integer.parseInt(environmentSubstitute(userHeight));
        } catch (NumberFormatException ex) {
        }
      }
      OutputStream os = openFileForWrite(fileName);
      if (headerWithoutSummary.attribute(i).isNominal()) {
        BufferedImage chart =
          ChartUtils.createAttributeChartNominal(summary, headerWithoutSummary
            .attribute(i).name(), os, chartWidth, chartHeight);
        if (chart != null) {
          m_summaryCharts.add(chart);
          m_summaryChartAttNames.add(name);
        }
      } else {
        NumericAttributeBinData binStats =
          numericBinStats.get(headerWithoutSummary.attribute(i).index());
        if (binStats == null) {
          throw new DistributedWekaException(
            "Unable to find histogram bin data for attribute: "
              + headerWithoutSummary.attribute(i).name());
        }

        BufferedImage chart =
          ChartUtils.createAttributeChartNumeric(binStats, summary, os,
            chartWidth, chartHeight);
        if (chart != null) {
          m_summaryCharts.add(chart);
          m_summaryChartAttNames.add(name);
        }
      }
    }
  }

  /**
   * Get the summary charts (if any)
   *
   * @return a list of Image objects or null
   */
  public List<BufferedImage> getSummaryCharts() {
    return m_summaryCharts;
  }

  /**
   * Get the names of the attributes in the summary charts
   *
   * @return the names of the attributes in the summary charts
   */
  public List<String> getSummaryChartAttNames() {
    return m_summaryChartAttNames;
  }

  protected void makeFinalHeader(JavaRDD<CSVToARFFHeaderMapTask> mapResults,
    boolean computeQuartiles) throws DistributedWekaException {
    List<CSVToARFFHeaderMapTask> partialTasks = mapResults.collect();
    // logMessage("**** Number of collected results: " + partialTasks.size());
    System.err.println(partialTasks.get(0).getHeader());

    // List<Object> headerPortions = mapResults.collect();
    List<Instances> headerPortionsInstances = new ArrayList<Instances>();
    List<HeaderAndQuantileDataHolder> headerPortionsDataHolder =
      new ArrayList<HeaderAndQuantileDataHolder>();

    for (CSVToARFFHeaderMapTask o : partialTasks) {
      if (computeQuartiles) {
        o.deSerializeAllQuantileEstimators();
        headerPortionsDataHolder.add(o.getHeaderAndQuantileEstimators());
      } else {
        headerPortionsInstances.add(o.getHeader());
      }
    }

    int decimalPlaces = 2;
    try {
      String dp =
        Utils.getOption("decimal-places",
          Utils.splitOptions(getCsvToArffTaskOptions()));
      if (!DistributedJobConfig.isEmpty(dp)) {
        decimalPlaces = Integer.parseInt(dp);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    m_finalHeader =
      computeQuartiles ? CSVToARFFHeaderReduceTask
        .aggregateHeadersAndQuartiles(headerPortionsDataHolder)
        : CSVToARFFHeaderReduceTask.aggregate(headerPortionsInstances);
    System.out.println(m_finalHeader);
    m_summaryStats =
      StatsFormatter.formatStats(m_finalHeader, computeQuartiles, decimalPlaces);
  }

  /**
   * Processes one or more {@code DataFrame}s. All frames are unioned into one
   * mondo frame and then the ARFF header + summary statistics are computed from
   * it. All {@code Dataset} instances with non-null backing DataFrames in the
   * {@code DatasetManager} for this job will have the resultant header set.
   * They will also have a backing {@code RDD<Instance>} dataset created.
   *
   * TODO perhaps have an option (somehow) to prevent materializing the RDD
   * until it is needed?
   *
   * @param frames the frames to process
   * @param unpersistFrames true if any dataframes processed into RDDs of
   *          Instances should be unpersisted
   * @throws IOException if a problem occurs
   * @throws DistributedWekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected void processDataFrames(List<DataFrame> frames,
    boolean unpersistFrames) throws IOException, DistributedWekaException {

    if (frames.size() > 1) {
      logMessage("Unioning " + frames.size() + " DataFrames");
    }

    DataFrame mondoFrame = frames.get(0);
    for (int i = 1; i < frames.size(); i++) {
      mondoFrame = mondoFrame.unionAll(frames.get(i));
    }

    final List<String> attributeNames = Arrays.asList(mondoFrame.columns());

    boolean computeQuartiles;
    try {
      computeQuartiles =
        Utils.getFlag("compute-quartiles",
          Utils.splitOptions(getCsvToArffTaskOptions()));
    } catch (Exception ex) {
      logMessage(ex);
      throw new DistributedWekaException(ex);
    }

    boolean headerExists = handleExistingHeaderFile();
    if (!headerExists) {
      logMessage("Computing ARFF metadata for DataFrame(s)");

      JavaRDD<Row> input = mondoFrame.javaRDD();
      JavaRDD<CSVToARFFHeaderMapTask> mapResults =
        input.mapPartitions(
          new FlatMapFunction<Iterator<Row>, CSVToARFFHeaderMapTask>() {

            private static final long serialVersionUID = 6701325790775953348L;
            protected CSVToARFFHeaderMapTask m_innerTask;
            List<CSVToARFFHeaderMapTask> m_list =
              new ArrayList<CSVToARFFHeaderMapTask>();

            protected Object[] m_reUsableVals;

            @Override
            public Iterable<CSVToARFFHeaderMapTask> call(Iterator<Row> split)
              throws IOException, DistributedWekaException {

              if (m_innerTask == null) {
                m_innerTask = new CSVToARFFHeaderMapTask();
                m_reUsableVals = new Object[attributeNames.size()];
                try {
                  m_innerTask.setOptions(Utils
                    .splitOptions(getCsvToArffTaskOptions()));
                } catch (Exception e) {
                  throw new DistributedWekaException(e);
                }
              }

              int count = 0;
              while (split.hasNext()) {
                Row nextRow = split.next();
                for (int i = 0; i < m_reUsableVals.length; i++) {
                  m_reUsableVals[i] = nextRow.get(i);
                }
                m_innerTask.processRowValues(m_reUsableVals, attributeNames);
                count++;
              }

              if (getDebug()) {
                logMessage("[ArffHeaderSparkJob] Number of DataFrame rows "
                  + "processed: " + count);
              }
              m_innerTask.serializeAllQuantileEstimators();
              m_list.add(m_innerTask);

              return m_list;
            }
          }).persist(getCachingStrategy().getStorageLevel());

      // "combine" if necessary
      mapResults = combinePartials(mapResults);

      logMessage("[ARFF header job] Number of partitions in partial header RDD: "
        + mapResults.partitions().size());
      makeFinalHeader(mapResults, computeQuartiles);
    }

    // now need to convert all individual DataFrames to RDD<Instance>
    Iterator<Map.Entry<String, Dataset>> dataSets =
      m_datasetManager.getDatasetIterator();
    CSVToARFFHeaderMapTask rowHelper = new CSVToARFFHeaderMapTask();
    try {
      rowHelper.setOptions(Utils.splitOptions(getCsvToArffTaskOptions()));
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }

    while (dataSets.hasNext()) {
      Map.Entry<String, Dataset> nextE = dataSets.next();
      Dataset current = nextE.getValue();
      String datasetType = nextE.getKey();
      DataFrame sourceFrame = current.getDataFrame();
      if (sourceFrame != null) {
        logMessage("Converting " + datasetType + " dataset to rdd<instance>");
        JavaRDD<Instance> instanceRDD =
          Dataset
            .dataFrameToInstanceRDD(this.getCachingStrategy(), sourceFrame,
              m_finalHeader, rowHelper, false, m_unpersistDataFrames);
        // instanceRDD.count();
        current.setHeaderWithSummary(m_finalHeader);
        current.setRDD(instanceRDD);
      }
    }

    if (!headerExists) {
      String outputPath = createOutputHeaderPath();
      writeHeaderToOutput();
      if (computeQuartiles) {
        writeChartsToOutput(outputPath);
      }
    }
    setJobStatus(JobStatus.FINISHED);
  }

  /**
   * Creates the path to the output ARFF header file. Uses a randomly generated
   * number as the file name if a name has not been specified by the user.
   *
   * @return the resolved path to the <b>directory</b> that will hold the ARFF
   *         file
   */
  protected String createOutputHeaderPath() {
    // Make sure that we save out to a subdirectory of the output
    // directory
    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);
    Random r = new Random();
    String outHeadName = "" + Math.abs(r.nextInt());
    if (!DistributedJobConfig.isEmpty(getOutputHeaderFileName())) {
      outHeadName = environmentSubstitute(getOutputHeaderFileName());
    }
    if (!outHeadName.toLowerCase().endsWith(".arff")) {
      outHeadName += ".arff";
    }
    outputPath +=
      (outputPath.toLowerCase().contains("://") ? "/" : File.separator);

    m_pathToAggregatedHeader = outputPath + outHeadName;

    return outputPath;
  }

  protected void processWithDatasource(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    // Make sure that the caching strategy gets set correctly
    // by the data source (since it is not the primary job and
    // does not create the spark context)
    m_dataSource.applyConfigProperties(sparkContext);
    m_dataSource.runJobWithContext(sparkContext);

    // set our caching strategy based on whatever the data source
    // has decided on
    setCachingStrategy(m_dataSource.getCachingStrategy());

    // transfer all datasets over to this job's dataset manager
    m_datasetManager.addAll(m_dataSource.getDatasetIterator());

    List<DataFrame> dataFrames = Dataset.getAllDataFrames(m_dataSource);
    if (dataFrames.size() == 0) {
      throw new DistributedWekaException(
        "Datasource did not produce any data frames!");
    }

    processDataFrames(dataFrames, true);
  }

  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    m_finalHeader = null;
    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }
    m_currentContext = sparkContext;
    setJobStatus(JobStatus.RUNNING);

    if (handleExistingHeaderFile()) {
      m_pathToAggregatedHeader =
        environmentSubstitute(getPathToExistingHeader());

      // load the header file
      m_finalHeader = loadArffHeaderFromPath(m_pathToAggregatedHeader);
      m_summaryStats = loadSummaryStatsFromPath(m_pathToAggregatedHeader);
      if (getDebug()) {
        logMessage(m_finalHeader.toString());
      }
    }

    // Any dataframes for us to work with?
    List<DataFrame> dataFrames = Dataset.getAllDataFrames(this);
    // TODO we take all incoming dataframes, union them into one big one,
    // compute the ARFF header for it, and then configure the datasets with
    // their backing RDD<Instance> and header; Unpersist the DataFrames here
    // or later?
    if (dataFrames.size() > 0) {
      processDataFrames(dataFrames, m_unpersistDataFrames);
    } else {
      // processLegacyCSVFile(sparkContext);
      processWithDatasource(sparkContext);
    }

    return true;
  }

  /**
   * Runs a "combine" style operation on the RDD of partial CSVToArff map tasks.
   * When there are a lot of partitions, coalescing and then combining within
   * each partition should be more efficient than collecting all the partials
   * and doing all the aggregation on the driver.
   *
   * @param toCombine the coalesced RDD of CSVToArffHeaderMapTask
   * @return an RDD containing one combined CSVToArffHeaderMapTask in each
   *         partition
   * @throws DistributedWekaException if a problem occurs
   */
  protected JavaRDD<CSVToARFFHeaderMapTask> combinePartials(
    JavaRDD<CSVToARFFHeaderMapTask> toCombine) throws DistributedWekaException {

    JavaRDD<CSVToARFFHeaderMapTask> result = toCombine;

    if (toCombine.partitions().size() >= m_parallelReduceThreshold) {
      // parallel partial reduce (combine)

      // probably need to force materialization
      // of mapResults RDD before coalesce. Otherwise I think it might force
      // coalesce on the original input RDD!!
      toCombine.count();

      // int numPartitionsForCombine =
      // mapResults.partitions().size() / m_parallelReduceMinPartitionSize;
      toCombine = toCombine.coalesce(100, true);

      logMessage("[ARFF header job] Coalescing partial header RDD down to "
        + toCombine.partitions().size() + " partitions");

      result =
        toCombine
          .mapPartitions(new FlatMapFunction<Iterator<CSVToARFFHeaderMapTask>, CSVToARFFHeaderMapTask>() {

            /**
             * For serialization
             */
            private static final long serialVersionUID = 1938643314985328258L;

            List<CSVToARFFHeaderMapTask> m_combinedPartition =
              new ArrayList<CSVToARFFHeaderMapTask>();

            @Override
            public Iterable<CSVToARFFHeaderMapTask> call(
              Iterator<CSVToARFFHeaderMapTask> split)
              throws DistributedWekaException {

              List<CSVToARFFHeaderMapTask> partitionPartials =
                new ArrayList<CSVToARFFHeaderMapTask>();

              while (split.hasNext()) {
                CSVToARFFHeaderMapTask partial = split.next();
                partial.deSerializeAllQuantileEstimators();
                partitionPartials.add(partial);
              }

              CSVToARFFHeaderMapTask combined =
                CSVToARFFHeaderMapTask.combine(partitionPartials);
              combined.serializeAllQuantileEstimators();
              m_combinedPartition.add(combined);

              return m_combinedPartition;
            }

          });
    }

    return result;
  }

  /**
   * Get the final header
   *
   * @return the final header
   */
  public Instances getHeader() {
    return m_finalHeader;
  }

  /**
   * Get the final header (calls getHeader())
   *
   * @return the final header
   */
  @Override
  public Instances getInstances() {
    return getHeader();
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {

    if (!(toRun instanceof ArffHeaderSparkJob)) {
      throw new IllegalArgumentException(
        "Object to run is not an ArffHeaderSparkJob!");
    }

    try {
      ArffHeaderSparkJob ahhj = (ArffHeaderSparkJob) toRun;
      if (Utils.getFlag('h', options)) {
        String help = DistributedJob.makeOptionsStr(ahhj);
        System.err.println(help);
        System.exit(1);
      }

      ahhj.setOptions(options);
      ahhj.runJob();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Main method for executing this job
   *
   * @param args arguments to the job
   */
  public static void main(String[] args) {
    ArffHeaderSparkJob ahhj = new ArffHeaderSparkJob();
    ahhj.run(ahhj, args);
  }

  @Override
  public String getText() {
    return m_summaryStats;
  }
}
