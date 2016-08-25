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
 *    CSVToARFFHeaderHadoopReducer
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import weka.core.Attribute;
import weka.core.ChartUtils;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.stats.NumericAttributeBinData;
import weka.core.stats.StatsFormatter;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderMapTask.HeaderAndQuantileDataHolder;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import distributed.core.DistributedJobConfig;

/**
 * Reducer implementation for the ArffHeaderHadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CSVToArffHeaderHadoopReducer extends
  Reducer<Text, BytesWritable, Text, Text> {

  /** key for specifying a chart width to use */
  public static final String CHART_WIDTH_KEY = "weka.chart.width";

  /** key for specifying a chart height to use */
  public static final String CHART_HEIGHT_KEY = "weka.chart.height";

  /** Default width for charts */
  public static final int DEFAULT_CHART_WIDTH = 600;

  /** Default height for charts */
  public static final int DEFAULT_CHART_HEIGHT = 400;

  /** Key for the property that holds the write path for the output header file */
  public static String CSV_TO_ARFF_HEADER_WRITE_PATH =
    "*weka.distributed.csv_to_arff_header_write_path";

  /** The underlying general Weka CSV reduce task */
  protected CSVToARFFHeaderReduceTask m_task = null;

  /** Whether quantiles are being estimated */
  protected boolean m_estimateQuantiles;

  protected int m_decimalPlaces = 2;

  @Override
  public void setup(Context context) throws IOException {
    m_task = new CSVToARFFHeaderReduceTask();

    Configuration conf = context.getConfiguration();
    String taskOpts =
      conf.get(CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS);
    if (taskOpts != null && taskOpts.length() > 0) {
      try {
        String[] options = Utils.splitOptions(taskOpts);

        m_estimateQuantiles = Utils.getFlag("compute-quartiles", options);

        String decimalPlaces = Utils.getOption("decimal-places", options);
        if (!DistributedJobConfig.isEmpty(decimalPlaces)) {
          try {
            m_decimalPlaces = Integer.parseInt(decimalPlaces);
          } catch (NumberFormatException e) {
            // quietly ignore
          }
        }

      } catch (Exception ex) {
        throw new IOException(ex);
      }
    }
  }

  @Override
  public void reduce(Text key, Iterable<BytesWritable> values, Context context)
    throws IOException {
    Configuration conf = context.getConfiguration();
    String outputDestination = conf.get(CSV_TO_ARFF_HEADER_WRITE_PATH);

    if (outputDestination == null || outputDestination.length() == 0) {
      throw new IOException("No destination given for aggregated ARFF header");
    }

    List<Instances> headersToAgg = new ArrayList<Instances>();
    List<HeaderAndQuantileDataHolder> holdersToAgg =
      new ArrayList<HeaderAndQuantileDataHolder>();

    int counter = 0;
    try {
      for (BytesWritable b : values) {
        byte[] bytes = b.getBytes();
        if (m_estimateQuantiles) {
          HeaderAndQuantileDataHolder holder = deserializeHolder(bytes);
          holdersToAgg.add(holder);
        } else {
          Instances aHeader = deserializeHeader(bytes);
          headersToAgg.add(aHeader);
        }
        counter++;
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }

    try {
      Instances aggregated =
        m_estimateQuantiles ? m_task.aggregateHeadersAndQuartiles(holdersToAgg)
          : m_task.aggregate(headersToAgg);
      writeHeaderToDestination(aggregated, outputDestination, conf);
      writeSummaryStatsStringToDestination(aggregated, outputDestination,
        m_estimateQuantiles, m_decimalPlaces, conf);

      Text outkey = new Text();
      outkey.set("AKey");
      Text outval = new Text();
      outval.set("Num headers aggregated " + counter);
      context.write(outkey, outval);

    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public static void writeSummaryStatsStringToDestination(
    Instances headerWithSummary, String outputFile, boolean quantiles,
    int decimalPlaces, Configuration conf) throws IOException,
    DistributedWekaException {
    String summaryStats =
      StatsFormatter.formatStats(headerWithSummary, quantiles, decimalPlaces);

    if (outputFile.toLowerCase().endsWith(".arff")) {
      outputFile = outputFile.replace(".arff", "").replace(".ARFF", "");
    }
    outputFile += ".summary";

    PrintWriter pr = null;
    try {
      Path pt = new Path(outputFile);
      FileSystem fs = FileSystem.get(conf);
      if (fs.exists(pt)) {
        // remove the file
        fs.delete(pt, true);
      }

      FSDataOutputStream fout = fs.create(pt);
      OutputStreamWriter osr = new OutputStreamWriter(fout);
      BufferedWriter br = new BufferedWriter(osr);
      pr = new PrintWriter(br);

      pr.print(summaryStats);
      pr.println();
      pr.flush();
    } finally {
      if (pr != null) {
        pr.close();
      }
    }
  }

  /**
   * Writes the Instances header to the destination file in HDFS
   * 
   * @param insts the Instances header to write
   * @param outputDestination the destination in HDFS
   * @param conf the Configuration object for the job
   * @throws IOException if a problem occurs
   */
  protected static void writeHeaderToDestination(Instances insts,
    String outputDestination, Configuration conf) throws IOException {
    PrintWriter pr = null;
    try {
      Path pt = new Path(outputDestination);
      FileSystem fs = FileSystem.get(conf);
      if (fs.exists(pt)) {
        // remove the file
        fs.delete(pt, true);
      }

      FSDataOutputStream fout = fs.create(pt);
      OutputStreamWriter osr = new OutputStreamWriter(fout);
      BufferedWriter br = new BufferedWriter(osr);
      pr = new PrintWriter(br);
      pr.print(insts.toString());
      pr.print("\n");
      pr.flush();
    } finally {
      if (pr != null) {
        pr.close();
      }
    }
  }

  /**
   * Returns true if there is at least one file in the output directory that
   * matches an attribute name and ends with ".png"
   * 
   * @param headerWithSummary the header of the arff data (with summary
   *          attributes)
   * @param outputDir the output directory to check
   * @param conf the Configuration to use
   * @return true if at least one attribute summary chart file exists int he
   *         output directory
   * @throws IOException if a problem occurs
   */
  protected static boolean attributeChartsExist(Instances headerWithSummary,
    String outputDir, Configuration conf) throws IOException {
    FileSystem fs = FileSystem.get(conf);

    try {
      Instances headerWithoutSummary =
        CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

      for (int i = 0; i < headerWithoutSummary.numAttributes(); i++) {
        if (!headerWithoutSummary.attribute(i).isNominal()
          && !headerWithoutSummary.attribute(i).isNumeric()) {
          continue;
        }
        String name = headerWithoutSummary.attribute(i).name();

        String fileName = name + ".png";
        Path p = new Path(outputDir + "/" + fileName);

        if (fs.exists(p)) {
          return true;
        }
      }
    } catch (DistributedWekaException e) {
      throw new IOException(e);
    }

    return false;
  }

  /**
   * Write out attribute summary charts to the output directory in HDFS if
   * necessary
   * 
   * @param headerWithSummary the header of the data with summary attributes
   * @param outputDir the output directory to write to
   * @param conf the Configuration to use to obtain a file system and optional
   *          chart dimensions
   * @throws IOException if a problem occurs
   */
  protected static void writeAttributeChartsIfNecessary(
    Instances headerWithSummary, String outputDir, Configuration conf)
    throws IOException {

    try {
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

      writeAttributeChartsIfNecessary(headerWithSummary, numericBinStats,
        outputDir, conf);
    } catch (DistributedWekaException e) {
      throw new IOException(e);
    }
  }

  /**
   * Write out attribute summary charts to the output directory in HDFS if
   * necessary
   * 
   * @param headerWithSummary the header of the data with summary attributes
   * @param numericBinStats a map of numeric histogram bin data, keyed by
   *          attribute index
   * @param outputDir the output directory to write to
   * @param conf the Configuration to use to obtain a file system and optional
   *          chart dimensions
   * @throws IOException if a problem occurs
   */
  protected static void writeAttributeChartsIfNecessary(
    Instances headerWithSummary,
    Map<Integer, NumericAttributeBinData> numericBinStats, String outputDir,
    Configuration conf) throws IOException {
    FileSystem fs = FileSystem.get(conf);
    try {
      Instances headerWithoutSummary =
        CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

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
          System.err
            .println("[WriteAttributeCharts] Can't find summary attribute "
              + "for attribute: " + name);
          continue;
        }

        String fileName = name + ".png";
        Path p = new Path(outputDir + "/" + fileName);
        if (fs.exists(p)) {
          continue;
        }

        int chartWidth = DEFAULT_CHART_WIDTH;
        String userWidth = conf.get(CHART_WIDTH_KEY);
        if (!DistributedJobConfig.isEmpty(userWidth)) {
          try {
            chartWidth = Integer.parseInt(userWidth);
          } catch (NumberFormatException e) {
          }
        }
        int chartHeight = DEFAULT_CHART_HEIGHT;
        String userHeight = conf.get(CHART_HEIGHT_KEY);
        if (!DistributedJobConfig.isEmpty(userHeight)) {
          try {
            chartHeight = Integer.parseInt(userHeight);
          } catch (NumberFormatException e) {
          }
        }

        FSDataOutputStream dos = fs.create(p, true);
        if (headerWithoutSummary.attribute(i).isNominal()) {
          ChartUtils.createAttributeChartNominal(summary, headerWithoutSummary
            .attribute(i).name(), dos, chartWidth, chartHeight);
        } else {
          NumericAttributeBinData binStats =
            numericBinStats.get(headerWithoutSummary.attribute(i).index());
          if (binStats == null) {
            throw new DistributedWekaException(
              "Unable to find histogram bin data for attribute: "
                + headerWithoutSummary.attribute(i).name());
          }

          ChartUtils.createAttributeChartNumeric(binStats, summary, dos,
            chartWidth, chartHeight);
        }
      }
    } catch (DistributedWekaException e) {
      throw new IOException(e);
    }
  }

  /**
   * Helper method to decompress a serialized Instances object
   * 
   * @param bytes an array of bytes containing the compressed serialized
   *          Instances object
   * @return an Instances object
   * @throws IOException if a problem occurs
   * @throws ClassNotFoundException if a class can't be loaded
   */
  protected Instances deserializeHeader(byte[] bytes) throws IOException,
    ClassNotFoundException {
    ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
    ObjectInputStream p = null;
    Object toReturn = null;
    try {
      p =
        new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
          istream)));

      toReturn = p.readObject();
      if (!(toReturn instanceof Instances)) {
        throw new IOException(
          "Object deserialized was not an Instances object!");
      }
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return (Instances) toReturn;
  }

  protected HeaderAndQuantileDataHolder deserializeHolder(byte[] bytes)
    throws IOException, ClassNotFoundException {

    ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
    ObjectInputStream p = null;
    Object toReturn = null;
    try {
      p =
        new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
          istream)));

      toReturn = p.readObject();

      if (!(toReturn instanceof HeaderAndQuantileDataHolder)) {
        throw new IOException(
          "Object deserialized was not an HeaderAndQuantileDataHolder object!");
      }
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return (HeaderAndQuantileDataHolder) toReturn;
  }
}
