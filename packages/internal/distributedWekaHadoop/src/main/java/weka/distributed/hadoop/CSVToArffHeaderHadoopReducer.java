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
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import weka.core.Instances;
import weka.distributed.CSVToARFFHeaderReduceTask;

/**
 * Reducer implementation for the ArffHeaderHadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CSVToArffHeaderHadoopReducer extends
  Reducer<Text, BytesWritable, Text, Text> {

  /** Key for the property that holds the write path for the output header file */
  public static String CSV_TO_ARFF_HEADER_WRITE_PATH = "*weka.distributed.csv_to_arff_header_write_path";

  /** The underlying general Weka CSV reduce task */
  protected CSVToARFFHeaderReduceTask m_task = null;

  @Override
  public void setup(Context context) throws IOException {
    m_task = new CSVToARFFHeaderReduceTask();
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

    int counter = 0;
    try {
      for (BytesWritable b : values) {
        byte[] bytes = b.getBytes();

        Instances aHeader = deserialize(bytes);
        headersToAgg.add(aHeader);
        counter++;
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }

    try {
      Instances aggregated = m_task.aggregate(headersToAgg);
      writeHeaderToDestination(aggregated, outputDestination, conf);

      Text outkey = new Text();
      outkey.set("AKey");
      Text outval = new Text();
      outval.set("Num headers aggregated " + counter);
      context.write(outkey, outval);

    } catch (Exception e) {
      throw new IOException(e);
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
      if (!outputDestination.startsWith("hdfs://")) {
        outputDestination = constructHDFSURI(outputDestination, conf);
      }

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
   * Helper to construct a HDFS URI
   * 
   * @param path the path to write to
   * @param conf the Configuration for the job
   * @return a HDFS URI
   */
  protected static String constructHDFSURI(String path, Configuration conf) {
    String hostPort = conf.get("fs.default.name");

    if (!hostPort.endsWith("/") && !path.endsWith("/")) {
      hostPort += "/";
    }

    hostPort += path;

    return hostPort;
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
  protected Instances deserialize(byte[] bytes) throws IOException,
    ClassNotFoundException {
    ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
    ObjectInputStream p = null;
    Object toReturn = null;
    try {
      p = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
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
}
