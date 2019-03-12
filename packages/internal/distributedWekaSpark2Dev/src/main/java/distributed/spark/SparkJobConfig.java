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
 *    SparkJobConfig
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package distributed.spark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Option;
import weka.core.Utils;
import distributed.core.DistributedJobConfig;

/**
 * Basic connection options and settings for a batch Spark job.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class SparkJobConfig extends BaseSparkJobConfig {

  /** Path to input file to process by this job (use hdfs:// for a file in hdfs) */
  protected String m_inputFile = "";

  /**
   * True if the input is an RDD saved as a SequenceFile containing serialized
   * Instance objects
   */
  protected boolean m_serializedInput;

  /**
   * The minimum number of splits to make from the input file. This is based on
   * the block size in HDFS (64mb), so you can't have fewer slices than blocks
   */
  protected String m_minInputSlices = "1";

  protected String m_maxInputSlices = "";

  /** Output directory path for the job */
  protected String m_outputDir = "";

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();
    options.addAll(Collections.list(super.listOptions()));

    /* options.addElement(new Option(
      "\tInput file/directory to process (may be local or " + "in HDFS)",
      "-input-file", 1, "-input-file <path to file>")); */

    options
      .addElement(new Option(
        "\tMinimum number of input slices (partitions) to create from "
          + "the input file (default = 1)", "min-slices", 1,
        "-min-slices <num>"));
    options.addElement(new Option(
      "\tMaximum number of input slices (partitions) to create from "
        + "the input file (default: no maximum).\n\t"
        + "Note that using this option may trigger a shuffle.", "max-slices",
      1, "-max-slices <num>"));

    options.addElement(new Option("\tOutput directory for the job",
      "output-dir", 1, "-output-dir <path>"));

    return options.elements();
  }

  @Override
  public String[] getOptions() {

    ArrayList<String> opts = new ArrayList<String>();

    /* if (!DistributedJobConfig.isEmpty(getInputFile())) {
      opts.add("-input-file");
      opts.add(getInputFile());
    } */

    if (!DistributedJobConfig.isEmpty(getMinInputSlices())) {
      opts.add("-min-slices");
      opts.add(getMinInputSlices());
    }

    if (!DistributedJobConfig.isEmpty(getMaxInputSlices())) {
      opts.add("-max-slices");
      opts.add(getMaxInputSlices());
    }

    if (!DistributedJobConfig.isEmpty(getOutputDir())) {
      opts.add("-output-dir");
      opts.add(getOutputDir());
    }

    for (String s : super.getOptions()) {
      opts.add(s);
    }

    return opts.toArray(new String[opts.size()]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    // setInputFile(Utils.getOption("input-file", options));
    setMinInputSlices(Utils.getOption("min-slices", options));
    setMaxInputSlices(Utils.getOption("max-slices", options));
    setOutputDir(Utils.getOption("output-dir", options));

    super.setOptions(options);
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String minInputSlicesTipText() {
    return "The minimum number of splits to create for the input file";
  }

  /**
   * Get the minimum number of input splits to create from the input file. By
   * default there is 1 split per 64Mb block (HDFS block size), so there can't
   * be fewer splits than there are blocks in the file. However, there can be
   * more splits.
   *
   * @return the number of splits to create
   */
  public String getMinInputSlices() {
    return m_minInputSlices;
  }

  /**
   * Set the minimum number of input splits (partitions) to create from the
   * input file. By default there is 1 split per 64Mb block (HDFS block size),
   * so there can't be fewer splits than there are blocks in the file. However,
   * there can be more splits.
   *
   * @param slices the number of splits to create
   */
  public void setMinInputSlices(String slices) {
    m_minInputSlices = slices;
  }

  /**
   * 
   * @return the tip text for this property
   */
  public String maxInputSlicesTipText() {
    return "The maximum number of splits/partitions to create "
      + "for the input file.";
  }

  /**
   * Get the maximum number of input splits (partitions) to create from the
   * input file. If this is fewer than the natural number of partitions (i.e. as
   * determined by the HDFS block size) then this will trigger a coalesce.
   *
   * @return the maximum number of splits/partitions to create from an input
   *         file.
   */
  public String getMaxInputSlices() {
    return m_maxInputSlices;
  }

  /**
   * Set the maximum number of input splits (partitions) to create from the
   * input file. If this is fewer than the natural number of partitions (i.e. as
   * determined by the HDFS block size) then this will trigger a coalesce.
   *
   * @param max the maximum number of splits/partitions to create from an input
   *          file.
   */
  public void setMaxInputSlices(String max) {
    m_maxInputSlices = max;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String outputDirTipText() {
    return "The output directory for this job";
  }

  /**
   * Get the output directory for this job
   *
   * @return the output directory for this job
   */
  public String getOutputDir() {
    return m_outputDir;
  }

  /**
   * Set the output directory for this job
   *
   * @param dir the output directory for this job
   */
  public void setOutputDir(String dir) {
    m_outputDir = dir;
  }

}
