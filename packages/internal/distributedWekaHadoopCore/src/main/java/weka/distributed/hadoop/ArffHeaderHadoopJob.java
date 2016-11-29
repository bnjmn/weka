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
 *    ArffHeaderHadoopJob
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.gui.beans.InstancesProducer;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.hadoop.HDFSUtils;
import weka.gui.beans.TextProducer;

/**
 * A Hadoop job for creating a unified ARFF header (including summary "meta"
 * attributes).
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ArffHeaderHadoopJob extends HadoopJob implements
  InstancesProducer, TextProducer, CommandlineRunnable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -8812941574014567344L;

  /** Subdirectory of the output directory for storing results to */
  public static final String OUTPUT_SUBDIR = "/arff";

  /**
   * Constructor
   */
  public ArffHeaderHadoopJob() {
    super("ARFF instances header job",
      "Create a consolidated ARFF header for the job");

    m_mrConfig.setMapperClass(CSVToArffHeaderHadoopMapper.class.getName());
    m_mrConfig.setReducerClass(CSVToArffHeaderHadoopReducer.class.getName());
  }

  /**
   * Returns textual help information for this job
   * 
   * @return help information for this job
   */
  public String globalInfo() {
    return "Creates a unified ARFF header from input CSV files by "
      + "determining column types automatically (if not specified "
      + "by the user); it also "
      + "computes summary statistics for each attribute "
      + "that get stored in additional \"meta\" attributes. This "
      + "job creates a header (with all nominal values determined) "
      + "that gets used by other distributed jobs to ensure that "
      + " models created on data processed by separate map tasks "
      + "are consistent and can be aggregated.";
  }

  /**
   * If set, prevents this job from running. The user may have already run a job
   * that generates an ARFF header. It can be re-used by setting this property.
   */
  protected String m_pathToExistingHeader = "";

  /** Comma-separated list of attribute names */
  protected String m_attributeNames = "";

  /** Location of a names file to load attribute names from - file:// or hdfs:// */
  protected String m_attributeNamesFile = "";

  /** Whether to run the second pass to compute quartiles */
  protected boolean m_includeQuartilesInSummaryAtts;

  /** Whether to generate summary charts */
  protected boolean m_generateCharts;

  /** Options to the underlying csv to arff map task */
  protected String m_wekaCsvToArffMapTaskOpts = "";

  /** Holds the path (in HDFS) to the aggregated header file */
  protected String m_hdfsPathToAggregatedHeader = "";

  /**
   * The name of the file (in the specified output directory) that will hold the
   * header arff file. If not specified a random number is generated as the file
   * name.
   */
  protected String m_outputHeaderFileName = "";

  /** The instances header produced by the job */
  protected Instances m_finalHeader;

  /** Formatted summary stats */
  protected String m_summaryStats = "";

  /** Holds any images generated from the summary attributes */
  protected List<BufferedImage> m_summaryCharts;

  public String getAggregatedHeaderPath() {
    return m_hdfsPathToAggregatedHeader;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result
      .add(new Option(
        "\tPath to header file to use. Set this if you have\n\t"
          + "run previous jobs that have generated a header already. Setting this\n\t"
          + "prevents this job from running.", "existing-header", 1,
        "-existing-header <path to file>"));
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

    result.add(new Option(
      "\tGenerate summary charts as png files. Note that\n\t"
        + "has option no affect if quartiles are not being computed", "charts",
      0, "-charts"));

    CSVToARFFHeaderMapTask tempTask = new CSVToARFFHeaderMapTask();
    Enumeration<Option> mtOpts = tempTask.listOptions();
    while (mtOpts.hasMoreElements()) {
      result.addElement(mtOpts.nextElement());
    }

    result.add(new Option("", "", 0,
      "\nGeneral Hadoop job configuration options:"));

    Enumeration<Option> superOpts = super.listOptions();

    while (superOpts.hasMoreElements()) {
      result.addElement(superOpts.nextElement());
    }

    return result.elements();
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

    setGenerateCharts(Utils.getFlag("charts", options));

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

    if (getGenerateCharts()) {
      options.add("-charts");
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

    // if (getIncludeQuartilesInSummaryAttributes()) {
    // options.add("-quartiles");
    // }

    if (getGenerateCharts()) {
      options.add("-charts");
    }

    String[] superOpts = super.getOptions();
    for (String o : superOpts) {
      options.add(o);
    }

    if (!DistributedJobConfig.isEmpty(getCsvToArffTaskOptions())) {
      try {
        String[] csvOpts = Utils.splitOptions(getCsvToArffTaskOptions());

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
   * Get the header that was generated by the job. Returns null if the job
   * hasn't been run yet.
   * 
   * @return the header generated by the job or null if the job hasn't been run
   *         yet.
   */
  public Instances getFinalHeader() {
    return m_finalHeader;
  }

  @Override
  public Instances getInstances() {
    return getFinalHeader();
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
   * Set the path to an previously created header file to use. If set this
   * prevents the job from running
   * 
   * @param path the path to a previously created header
   */
  public void setPathToExistingHeader(String path) {
    m_pathToExistingHeader = path;
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
   * The tip text for this property
   * 
   * @return the tip text for this property
   */
  public String outputHeaderFileNameTipText() {
    return "The name of the header file to create in the output directory"
      + "for the job. If not specified then a name is generated automatically.";
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
   * Get the name of the header file to create in the output directory. If left
   * unset then a name is generated automatically.
   * 
   * @return the name for the ARFF header file
   */
  public String getOutputHeaderFileName() {
    return m_outputHeaderFileName;
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
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String attributeNamesFileTipText() {
    return "Path to the file to load attribute names from";
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
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String generateChartsTipText() {
    return "Generate summary chart png files - has no affect if quantiles are not been"
      + " computed.";
  }

  /**
   * Set whether to generate summary charts as png files in the output directory
   * (histograms, pie charts, box plots).
   * 
   * @param g true if summary charts are to be generated
   */
  public void setGenerateCharts(boolean g) {
    m_generateCharts = g;
  }

  /**
   * Get whether to generate summary charts as png files in the output directory
   * (histograms, pie charts, box plots).
   * 
   * @return true if summary charts are to be generated
   */
  public boolean getGenerateCharts() {
    return m_generateCharts;
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
   * Set the options to pass on to the underlying csv to arff task
   * 
   * @param opts options to pass on to the csv to arff map and reduce tasks
   */
  public void setCsvToArffTaskOptions(String opts) {
    m_wekaCsvToArffMapTaskOpts = opts;
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
   * Copies the names file into HDFS (if necessary) and adds it to the
   * distributed cache for the job
   * 
   * @param conf
   * @return the filename part of the path
   * @throws IOException
   */
  protected String handleNamesFile(Configuration conf) throws IOException {
    String namesFile = environmentSubstitute(getAttributeNamesFile());

    String filenameOnly =
      HDFSUtils.addFileToDistributedCache(m_mrConfig.getHDFSConfig(), conf,
        namesFile, m_env);

    return filenameOnly;
  }

  /**
   * Generates png summary attribute charts if necessary
   * 
   * @param outputPath the output path in HDFS
   * @throws DistributedWekaException if a problem occurs
   * @throws IOException if a problem occurs
   */
  protected void generateChartsIfNecessary(String outputPath)
    throws DistributedWekaException, IOException {

    if (getGenerateCharts()) {
      boolean hasNumeric =
        CSVToARFFHeaderReduceTask
          .headerContainsNumericAttributes(getFinalHeader());

      boolean generate =
        !hasNumeric
          || (hasNumeric && CSVToARFFHeaderReduceTask
            .headerContainsQuartiles(getFinalHeader()));

      if (generate) {
        // generate any charts (if necessary).
        Configuration conf = new Configuration();
        m_mrConfig.getHDFSConfig().configureForHadoop(conf, m_env);
        CSVToArffHeaderHadoopReducer.writeAttributeChartsIfNecessary(
          getFinalHeader(), outputPath, conf);

        m_summaryCharts = new ArrayList<BufferedImage>();
        FileSystem fs = FileSystem.get(conf);
        Instances header = getFinalHeader();
        header = CSVToARFFHeaderReduceTask.stripSummaryAtts(header);
        for (int i = 0; i < header.numAttributes(); i++) {
          String attName = header.attribute(i).name();
          attName = outputPath + "/" + attName + ".png";
          Path p = new Path(attName);
          if (fs.exists(p)) {
            FSDataInputStream is = fs.open(p);
            BufferedImage image = ImageIO.read(is);
            m_summaryCharts.add(image);
            is.close();
          }
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
   * Checks for the existence of an existing ARFF header file in HDFS or the
   * local file system. If local, it is copied into HDFS.
   * 
   * @throws DistributedWekaException if the file does not exist or there is a
   *           problem transfering it into HDFS
   */
  protected void handleExistingHeaderFile() throws DistributedWekaException {

    String existingPath = getPathToExistingHeader();

    try {
      existingPath = environmentSubstitute(existingPath);
    } catch (Exception ex) {
    }

    // check local file system first
    File f = new File(existingPath);
    boolean success = false;
    if (f.exists()) {
      // copy this file into HDFS
      String hdfsDest =
        HDFSUtils.WEKA_TEMP_DISTRIBUTED_CACHE_FILES + f.getName();

      try {
        HDFSUtils.copyToHDFS(existingPath, hdfsDest,
          m_mrConfig.getHDFSConfig(), m_env, true);

        m_hdfsPathToAggregatedHeader = hdfsDest;
        Configuration conf = new Configuration();
        m_mrConfig.getHDFSConfig().configureForHadoop(conf, m_env);
        getFinalHeaderFromHDFS(conf, hdfsDest);
        success = true;
      } catch (IOException e) {
        throw new DistributedWekaException(e);
      }
    } else {
      try {
        Path p = new Path(existingPath);
        Configuration conf = new Configuration();
        m_mrConfig.getHDFSConfig().configureForHadoop(conf, m_env);
        FileSystem fs = FileSystem.get(conf);
        if (fs.exists(p)) {
          m_hdfsPathToAggregatedHeader = existingPath;
          getFinalHeaderFromHDFS(conf, existingPath);
          success = true;
        }
      } catch (IOException ex) {
        throw new DistributedWekaException(ex);
      }
    }

    if (!success) {
      throw new DistributedWekaException("Was unable to find '" + existingPath
        + "' on either " + "the local file system or in HDFS");
    }
  }

  @Override
  public boolean runJob() throws DistributedWekaException {
    boolean success = true;
    m_finalHeader = null;

    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());
      if (!DistributedJobConfig.isEmpty(getPathToExistingHeader())) {
        try {
          handleExistingHeaderFile();

          // success = runQuartileJobIfNecessary();
          // if (success) {
          // Configuration conf = new Configuration();
          // m_mrConfig.getHDFSConfig().configureForHadoop(conf, m_env);
          // getFinalHeaderFromHDFS(conf, getAggregatedHeaderPath());
          // }

          // See if charts need to be generated (i.e. there are no
          // png files already in the output directory)
          try {
            String outputPath = m_mrConfig.getOutputPath();
            outputPath += OUTPUT_SUBDIR;
            generateChartsIfNecessary(outputPath);
          } catch (Exception ex) {
            logMessage("A problem occurred while checking whether charts need to "
              + "be generated (reason: " + ex.getMessage() + ")");
          }

          return success;
        } catch (DistributedWekaException ex) {
          logMessage("Unable to laod existing header file from '"
            + getPathToExistingHeader() + "' (reason: " + ex.getMessage()
            + "). Running job to create header...");
        }
      }

      setJobStatus(JobStatus.RUNNING);

      if (m_env == null) {
        m_env = Environment.getSystemWide();
      }

      // make sure that we save out to a subdirectory of the output directory
      // so that the arff header doesn't get deleted by any jobs that invoke
      // us first before themselves
      String outputPath = m_mrConfig.getOutputPath();
      outputPath += OUTPUT_SUBDIR;
      m_mrConfig.setOutputPath(outputPath);
      Random r = new Random();
      String outHeadName = "" + Math.abs(r.nextInt());
      if (!DistributedJobConfig.isEmpty(getOutputHeaderFileName())) {
        outHeadName = environmentSubstitute(getOutputHeaderFileName());
      }
      if (!outHeadName.toLowerCase().endsWith(".arff")) {
        outHeadName += ".arff";
      }
      outputPath += "/" + outHeadName;
      // m_hdfsPathToAggregatedHeader = HDFSUtils.constructHDFSURI(
      // m_mrConfig.getHDFSConfig(), outputPath, m_env);
      m_hdfsPathToAggregatedHeader = outputPath;

      // Where to write the consolidated ARFF file to
      m_mrConfig.setUserSuppliedProperty(
        CSVToArffHeaderHadoopReducer.CSV_TO_ARFF_HEADER_WRITE_PATH, outputPath);

      CSVToARFFHeaderMapTask tempMapTask = new CSVToARFFHeaderMapTask();
      if (!DistributedJobConfig.isEmpty(getCsvToArffTaskOptions())) {
        String[] tempOpts = Utils.splitOptions(getCsvToArffTaskOptions());
        tempMapTask.setOptions(tempOpts);
      }

      Configuration conf = new Configuration();

      // Options to the map task and the underlying general Weka map
      // task
      List<String> csvToArffTaskOptions = new ArrayList<String>();
      // StringBuilder csvToArffTaskOptions = new StringBuilder();
      if (!DistributedJobConfig.isEmpty(getAttributeNames())) {
        csvToArffTaskOptions.add("-A");
        csvToArffTaskOptions.add(environmentSubstitute(getAttributeNames()));
      } else if (!DistributedJobConfig.isEmpty(getAttributeNamesFile())) {
        String filenameOnly = handleNamesFile(conf);
        csvToArffTaskOptions.add("-names-file");
        csvToArffTaskOptions.add(filenameOnly);
      }

      if (!DistributedJobConfig.isEmpty(getCsvToArffTaskOptions())) {
        String[] csvTaskOpts = Utils.splitOptions(getCsvToArffTaskOptions());
        for (String s : csvTaskOpts) {
          csvToArffTaskOptions.add(s);
        }
      }

      if (csvToArffTaskOptions.size() > 0) {
        m_mrConfig.setUserSuppliedProperty(
          CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS,
          environmentSubstitute(Utils.joinOptions(csvToArffTaskOptions
            .toArray(new String[csvToArffTaskOptions.size()]))));

        setJobName(getJobName() + " " + csvToArffTaskOptions.toString());
      }

      // install the weka libraries and any user-selected packages
      // to HDFS and add to the distributed cache/classpath for
      // the job
      installWekaLibrariesInHDFS(conf);

      Job job =
        m_mrConfig.configureForHadoop(environmentSubstitute(getJobName()),
          conf, m_env);

      cleanOutputDirectory(job);

      statusMessage("Submitting job: " + getJobName());
      logMessage("Submitting job: " + getJobName());

      success = runJob(job);
      // boolean genCharts = getGenerateCharts();
      // if (success) {
      // getFinalHeaderFromHDFS(conf, outputPath);
      // // success = runQuartileJobIfNecessary();
      //
      // // restore the state of generate charts because we
      // // passed a reference to ourself to the randomize job
      // // (so it would not execute the ARFF job again) and
      // // the randomize job will have called
      // // setGenerateCharts(false) on us.
      // // setGenerateCharts(genCharts);
      // }

      if (success) {
        getFinalHeaderFromHDFS(conf, outputPath);
      }

      generateChartsIfNecessary(m_mrConfig.getOutputPath());

      setJobStatus(success ? JobStatus.FINISHED : JobStatus.FAILED);
    } catch (Exception ex) {
      setJobStatus(JobStatus.FAILED);
      throw new DistributedWekaException(ex);
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    return success;
  }

  /**
   * Read the final ARFF header file out of HDFS
   * 
   * @param conf the Configuration of the job that created the header
   * @param outputFileName the path to the header in HDFS
   * @throws DistributedWekaException if a problem occurs
   */
  protected void getFinalHeaderFromHDFS(Configuration conf,
    String outputFileName) throws DistributedWekaException {
    try {
      FileSystem fs = FileSystem.get(conf);

      Path p = new Path(outputFileName);
      FSDataInputStream is = fs.open(p);

      BufferedReader br = null;
      try {
        br = new BufferedReader(new InputStreamReader(is));

        m_finalHeader = new Instances(br);
        br.close();
        br = null;

        // now try and grab the formatted summary stats
        if (outputFileName.toLowerCase().endsWith(".arff")) {
          outputFileName = outputFileName.replace(".arff", "").replace(".ARFF", "");
        }
        outputFileName += ".summary";
        p = new Path(outputFileName);
        if (fs.exists(p)) {
          is = fs.open(p);
          br = new BufferedReader(new InputStreamReader(is));
          StringBuilder summaryBuilder = new StringBuilder();
          String line = "";
          while((line = br.readLine()) != null) {
            summaryBuilder.append(line).append("\n");
          }

          br.close();
          br = null;

          m_summaryStats = summaryBuilder.toString();
        }
      } finally {
        if (br != null) {
          br.close();
        }
      }
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }
  }

  public static void main(String[] args) {

    ArffHeaderHadoopJob ahhj = new ArffHeaderHadoopJob();
    ahhj.run(ahhj, args);
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {

    if (!(toRun instanceof ArffHeaderHadoopJob)) {
      throw new IllegalArgumentException(
        "Object to run is not an ArffHeaderHadoopJob!");
    }

    try {
      ArffHeaderHadoopJob ahhj = (ArffHeaderHadoopJob) toRun;
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

  @Override public String getText() {
    return m_summaryStats;
  }
}
