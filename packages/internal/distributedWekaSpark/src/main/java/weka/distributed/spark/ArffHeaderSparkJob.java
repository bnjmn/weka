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
 * @version $Revision$
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

  /** The summary stats */
  protected String m_summaryStats = "";

  /** Comma-separated list of attribute names */
  protected String m_attributeNames = "";

  /** Location of a names file to load attribute names from - file:// or hdfs:// */
  protected String m_attributeNamesFile = "";

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
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result
      .add(new Option(
        "\tPath to header file to use. Set this if you have\n\t"
          + "run previous jobs that have generated a header already. Setting this\n\t"
          + "prevents this job from running.", "existing-header", 1,
        "-existing-header <path>"));
    result
      .add(new Option("\tComma separated list of attribute names to use.\n\t"
        + "Use either this option, -names-file or neither (in which case\n\t"
        + "attribute names will be generated).", "A", 1, "-A <attribute names>"));
    result.add(new Option(
      "\tLocation of a names file to source attribute names\n\t"
        + "from. Can exist locally or in HDFS. "
        + "Use either this option, -A or neither (in which case\n\t"
        + "attribute names will be generated).", "names-file", 1,
      "-names-file <path to file>"));
    result
      .add(new Option(
        "\tFile name for output ARFF header. Note that this is a name only\n\t"
          + "and not a path. This file will be created in the output directory\n\t"
          + "specified by the -output-path option. (default is a "
          + "randomly generated name)", "header-file-name", 1,
        "-header-file-name <name>"));

    CSVToARFFHeaderMapTask tempTask = new CSVToARFFHeaderMapTask();
    Enumeration<Option> mtOpts = tempTask.listOptions();
    while (mtOpts.hasMoreElements()) {
      result.addElement(mtOpts.nextElement());
    }

    result.add(new Option("", "", 0,
      "\nGeneral Spark job configuration options:"));

    Enumeration<Option> superOpts = super.listOptions();

    while (superOpts.hasMoreElements()) {
      result.addElement(superOpts.nextElement());
    }

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

    if (!DistributedJobConfig.isEmpty(getAttributeNames())) {
      options.add("-A");
      options.add(getAttributeNames());
    }

    if (!DistributedJobConfig.isEmpty(getAttributeNamesFile())) {
      options.add("-names-file");
      options.add(getAttributeNamesFile());
    }

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

    if (!DistributedJobConfig.isEmpty(getAttributeNames())) {
      options.add("-A");
      options.add(getAttributeNames());
    }

    if (!DistributedJobConfig.isEmpty(getAttributeNamesFile())) {
      options.add("-names-file");
      options.add(getAttributeNamesFile());
    }

    if (!DistributedJobConfig.isEmpty(getOutputHeaderFileName())) {
      options.add("-header-file-name");
      options.add(getOutputHeaderFileName());
    }

    String[] superOpts = super.getOptions();
    options.addAll(Arrays.asList(superOpts));

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

    String attNames = Utils.getOption('A', options);
    setAttributeNames(attNames);

    String namesFile = Utils.getOption("names-file", options);
    setAttributeNamesFile(namesFile);

    String outputName = Utils.getOption("header-file-name", options);
    setOutputHeaderFileName(outputName);

    super.setOptions(options);

    // any options to pass on to the underlying Weka csv to arff map task?
    CSVToARFFHeaderMapTask tempMap = new CSVToARFFHeaderMapTask();
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
  public String attributeNamesTipText() {
    return "A comma-separated list of attribute names for the data";
  }

  /**
   * Get a comma-separated list of attribute names to use when generating the
   * ARFF header. Use this or a names file to specify attribute names to use.
   * Otherwise, names are generated automatically
   *
   * @return the names of the attributes
   */
  public String getAttributeNames() {
    return m_attributeNames;
  }

  /**
   * Set a comma-separated list of attribute names to use when generating the
   * ARFF header. Use this or a names file to specify attribute names to use.
   * Otherwise, names are generated automatically
   *
   * @param names the names of the attributes
   */
  public void setAttributeNames(String names) {
    m_attributeNames = names;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String attributeNamesFileTipText() {
    return "Path to the file to load attribute names from";
  }

  /**
   * Get the path to an file containing attribute names to use. Names should be
   * listed one per line in the file. The file may reside on the local file
   * system or in HDFS. Use either this option or setAttributeNames to specify
   * names for the attributes when creating the ARFF header. If left unset then
   * names will be generated automatically.
   *
   * @return the path to a names file to use
   */
  public String getAttributeNamesFile() {
    return m_attributeNamesFile;
  }

  /**
   * Set the path to an file containing attribute names to use. Names should be
   * listed one per line in the file. The file may reside on the local file
   * system or in HDFS. Use either this option or setAttributeNames to specify
   * names for the attributes when creating the ARFF header. If left unset then
   * names will be generated automatically.
   *
   * @param namesfile the path to a names file to use
   */
  public void setAttributeNamesFile(String namesfile) {
    m_attributeNamesFile = namesfile;
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
   * Resolves any user-supplied attribute names, in order to pass to the
   * underlying CSVToARFF job. Looks for attribute names supplied via the
   * command line option -A or via a "names" file (which can exist on the local
   * file system or in HDFS). The format of a names file is one attribute
   * (column) name per line.
   *
   * @return a list of attribute names
   * @throws IOException if a problem occurs
   */
  protected List<String> resolveAttributeNames() throws IOException {
    List<String> result = new ArrayList<String>();

    if (!DistributedJobConfig.isEmpty(getAttributeNames())) {
      String[] n = environmentSubstitute(getAttributeNames()).split(",");
      for (String nn : n) {
        nn = nn.trim();
        if (nn.length() > 0) {
          result.add(nn);
        }
      }
    } else if (!DistributedJobConfig.isEmpty(getAttributeNamesFile())) {
      String namesFile = environmentSubstitute(getAttributeNamesFile());

      InputStream in = openFileForRead(namesFile);
      InputStreamReader isr = new InputStreamReader(in);
      BufferedReader br = new BufferedReader(isr);

      try {
        String line = "";
        while ((line = br.readLine()) != null) {
          result.add(line.trim());
        }
      } finally {
        br.close();
      }
    }

    if (result.size() == 0) {
      result = null;
    }

    return result;
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

  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    m_currentContext = sparkContext;
    setJobStatus(JobStatus.RUNNING);

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }
    String inputFile = environmentSubstitute(m_sjConfig.getInputFile());
    logMessage("[ARFF header job] input:" + inputFile);
    int minSlices = 1;
    if (!DistributedJobConfig.isEmpty(m_sjConfig.getMinInputSlices())) {
      try {
        minSlices =
          Integer
            .parseInt(environmentSubstitute(m_sjConfig.getMinInputSlices()));
      } catch (NumberFormatException e) {
      }
    }

    // handle existing header file
    if (handleExistingHeaderFile()) {
      // No need to run the job! The user-supplied existing header file
      // does exist on the file system.

      m_pathToAggregatedHeader =
        environmentSubstitute(getPathToExistingHeader());

      // load the header file
      m_finalHeader = loadArffHeaderFromPath(m_pathToAggregatedHeader);
      JavaRDD<Instance> dataSet =
        loadInput(inputFile,
          CSVToARFFHeaderReduceTask.stripSummaryAtts(m_finalHeader),
          getCsvToArffTaskOptions(), sparkContext, getCachingStrategy(),
          minSlices, true);

      setDataset(TRAINING_DATA, new Dataset<Instance>(dataSet, m_finalHeader));

      // done!
      return true;
    }

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

    final List<String> attributeNames = resolveAttributeNames();

    String absoluteInputFile = resolveLocalOrOtherFileSystemPath(inputFile);

    boolean computeQuartiles;
    try {
      computeQuartiles =
        Utils.getFlag("compute-quartiles",
          Utils.splitOptions(getCsvToArffTaskOptions()));
    } catch (Exception ex) {
      logMessage(ex);
      throw new DistributedWekaException(ex);
    }

    JavaRDD<String> input = sparkContext.textFile(absoluteInputFile, minSlices);
    input.persist(getCachingStrategy().getStorageLevel());

    logMessage("[ARFF header job] Number of partitions in input: "
      + input.partitions().size());

    JavaRDD<CSVToARFFHeaderMapTask> mapResults =
      input.mapPartitions(
        new FlatMapFunction<Iterator<String>, CSVToARFFHeaderMapTask>() {

          /** For serialization */
          private static final long serialVersionUID = -6675756865309324148L;
          protected CSVToARFFHeaderMapTask m_innerTask;
          List<CSVToARFFHeaderMapTask> m_list =
            new ArrayList<CSVToARFFHeaderMapTask>();

          @Override
          public Iterable<CSVToARFFHeaderMapTask> call(Iterator<String> split)
            throws IOException, DistributedWekaException {

            if (m_innerTask == null) {
              m_innerTask = new CSVToARFFHeaderMapTask();
              try {
                m_innerTask.setOptions(Utils
                  .splitOptions(getCsvToArffTaskOptions()));

              } catch (Exception e) {
                throw new DistributedWekaException(e);
              }
            }

            int count = 0;
            while (split.hasNext()) {
              m_innerTask.processRow(split.next(), attributeNames);
              count++;
            }

            if (getDebug()) {
              logMessage("[ArffHeaderSparkJob] Number of rows processed: "
                + count);
            }

            m_innerTask.serializeAllQuantileEstimators();
            m_list.add(m_innerTask);

            return m_list;
          }
        }).persist(getCachingStrategy().getStorageLevel());

    if (mapResults.partitions().size() >= m_parallelReduceThreshold) {
      // parallel partial reduce (combine)

      // probably need to force materialization
      // of mapResults RDD before coalesce. Otherwise I think it might force
      // coalesce on the original input RDD!!
      mapResults.count();

      // int numPartitionsForCombine =
      // mapResults.partitions().size() / m_parallelReduceMinPartitionSize;
      mapResults = mapResults.coalesce(100, true);

      logMessage("[ARFF header job] Coalescing partial header RDD down to "
        + mapResults.partitions().size() + " partitions");

      mapResults = combinePartials(mapResults);
    }

    logMessage("[ARFF header job] Number of partitions in partial header RDD: "
      + mapResults.partitions().size());

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

    m_finalHeader =
      computeQuartiles ? CSVToARFFHeaderReduceTask
        .aggregateHeadersAndQuartiles(headerPortionsDataHolder)
        : CSVToARFFHeaderReduceTask.aggregate(headerPortionsInstances);
    System.out.println(m_finalHeader);

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
    m_summaryStats =
      StatsFormatter
        .formatStats(m_finalHeader, computeQuartiles, decimalPlaces);

    // save the input for other jobs to use...
    JavaRDD<Instance> dataSet =
      stringRDDToInstanceRDD(input,
        CSVToARFFHeaderReduceTask.stripSummaryAtts(m_finalHeader),
        getCsvToArffTaskOptions(), getCachingStrategy(), true);

    setDataset(TRAINING_DATA, new Dataset<Instance>(dataSet, m_finalHeader));

    writeHeaderToOutput();
    if (computeQuartiles) {
      writeChartsToOutput(outputPath);
    }
    setJobStatus(JobStatus.FINISHED);

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

    JavaRDD<CSVToARFFHeaderMapTask> result =
      toCombine
        .mapPartitions(new FlatMapFunction<Iterator<CSVToARFFHeaderMapTask>, CSVToARFFHeaderMapTask>() {

          /** For serialization */
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

  @Override
  public String getText() {
    return m_summaryStats;
  }
}
