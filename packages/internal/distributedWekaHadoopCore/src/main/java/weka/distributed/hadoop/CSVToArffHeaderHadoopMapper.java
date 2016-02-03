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
 *    CSVToARFFHeaderHadoopMapper
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderMapTask.HeaderAndQuantileDataHolder;
import weka.distributed.DistributedWekaException;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Mapper implementation for the ArffHeaderHadoop job
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CSVToArffHeaderHadoopMapper extends
  Mapper<LongWritable, Text, Text, BytesWritable> {

  /**
   * The key in the Configuration that the options for this task are associated
   * with
   */
  public static String CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS =
    "*weka.distributed.csv_to_arff_header_map_task_opts";

  /** The underlying general Weka CSV map task */
  protected CSVToARFFHeaderMapTask m_task;

  /**
   * Holds the list of attribute names to use for the columns in the incoming
   * data
   */
  protected List<String> m_attNames;

  /**
   * Any error that occurs during mapping. Hadoop does not seem to report these
   * exceptions when thrown by the map() function (although they do stop the
   * task attempt immediately), instead, the cleanup() method gets called (which
   * might result in other exceptions getting reported and making the root cause
   * more tricky to establish)
   */
  protected IOException m_fatalMappingError;

  /** Whether to estimate quantiles or not */
  protected boolean m_estimateQuantiles;

  /**
   * Read attribute names (one per line) from the supplied Reader
   * 
   * @param br the BufferedReader to read from
   * @return a list of attribute names
   * @throws IOException if a problem occurs
   */
  public static List<String> readNames(BufferedReader br) throws IOException {

    List<String> names = new ArrayList<String>();
    try {
      String line = "";
      while ((line = br.readLine()) != null) {
        names.add(line.trim());
      }
    } finally {
      if (br != null) {
        br.close();
      }
    }

    return names;
  }

  /**
   * Handles obtaining the attribute names to use from a file. Assumes that the
   * file is in the distributed cache
   * 
   * @param filename the name of the names file (as a filename only).
   * @throws IOException if a problem occurs
   */
  protected void setupAttNamesFromFile(String filename) throws IOException {
    m_attNames = new ArrayList<String>();

    File f = new File(filename);

    if (!f.exists()) {
      throw new IOException("Names file '" + filename + "' does not seem "
        + "to exist in the distributed cache! Absolute path: "
        + f.getAbsolutePath());
    }

    BufferedReader br = null;
    br = new BufferedReader(new FileReader(filename));
    m_attNames = readNames(br);
  }

  @Override
  public void setup(Context context) throws IOException {
    // initialize and configure
    m_task = new CSVToARFFHeaderMapTask();

    Configuration conf = context.getConfiguration();

    // String numMaps = conf.get("mapred.task.id");
    // int mapNum = HadoopJob.getMapNumber(numMaps);
    // System.err.println("Map number: " + mapNum);

    // property starts with a * to prevent it from being shown in
    // the user supplied properties table in the GUI
    String taskOpts = conf.get(CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS);
    if (taskOpts != null && taskOpts.length() > 0) {
      try {
        String[] options = Utils.splitOptions(taskOpts);

        // user supplied via an option?
        String attNames = Utils.getOption('A', options);
        if (attNames != null && attNames.length() > 0) {
          String[] split = attNames.split(",");
          m_attNames = new ArrayList<String>();
          for (String n : split) {
            n = n.trim();
            if (n.length() > 0) {
              m_attNames.add(n);
            }
          }
        } else {
          // user attribute names in a hdfs file?
          String attNamesFile = Utils.getOption("names-file", options);
          if (attNamesFile != null && attNamesFile.length() > 0) {
            // load this from the distributed cache
            setupAttNamesFromFile(attNamesFile);
          }
        }

        // pass on remaining options to map task
        m_task.setOptions(options);

        m_estimateQuantiles = m_task.getComputeQuartilesAsPartOfSummaryStats();
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
  }

  @Override
  public void map(LongWritable key, Text value, Context context)
    throws IOException {
    if (value != null) {
      try {
        m_task.processRow(value.toString(), m_attNames);
      } catch (Exception ex) {
        m_fatalMappingError = new IOException(ex);
        throw new IOException(ex);
      }
    }
  }

  @Override
  public void cleanup(Context context) throws IOException, InterruptedException {

    if (m_fatalMappingError != null) {
      throw m_fatalMappingError;
    }

    HeaderAndQuantileDataHolder holder = null;
    Instances header = null;
    if (!m_estimateQuantiles) {
      header = m_task.getHeader();
    } else {
      try {
        holder = m_task.getHeaderAndQuantileEstimators();
      } catch (DistributedWekaException ex) {
        throw new IOException(ex);
      }
    }

    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    OutputStream os = ostream;
    ObjectOutputStream p;

    p = new ObjectOutputStream(new BufferedOutputStream(
      new GZIPOutputStream(os)));
    p.writeObject(header != null ? header : holder);
    p.flush();
    p.close();

    byte[] bytes = ostream.toByteArray();
    // make sure all headers go to the same reducer
    String contantKey = "header";

    Text key = new Text();
    key.set(contantKey);
    BytesWritable value = new BytesWritable();
    value.set(bytes, 0, bytes.length);
    context.write(key, value); // write the header
  }
}
