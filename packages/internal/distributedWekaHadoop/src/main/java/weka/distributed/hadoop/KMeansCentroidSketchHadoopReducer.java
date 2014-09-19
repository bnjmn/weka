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
 *    KMeansCentroidSketchHadoopReducer
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import weka.clusterers.CentroidSketch;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.Utils;
import weka.distributed.KMeansMapTask;
import weka.distributed.KMeansReduceTask;
import distributed.core.DistributedJobConfig;

/**
 * Hadoop reducer for the k-means|| initialization procedure. Aggregates
 * centroid candidates from the map phase.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class KMeansCentroidSketchHadoopReducer extends
  Reducer<Text, BytesWritable, Text, Text> {

  /**
   * The key in the configuration that the write path for centroid sketches is
   * associated with
   */
  public static final String SKETCH_WRITE_PATH =
    "*weka.distributed.centroid_sketch_write_path";

  /** The same as the output directory for the job */
  protected String m_outputDestination;

  /**
   * True if this is a reduce for the first iteration of the k-means||
   * initialization
   */
  protected boolean m_isFirstIteration;

  /**
   * The header of the training data after it has been through any filters
   * specified for the k-means job
   */
  protected Instances m_transformedHeaderNoSummary;

  @Override
  public void setup(Context context) throws IOException {
    Configuration conf = context.getConfiguration();
    m_outputDestination = conf.get(SKETCH_WRITE_PATH);
    if (DistributedJobConfig.isEmpty(m_outputDestination)) {
      throw new IOException("No output path for centroid sketches supplied!");
    }

    String taskOptsS =
      conf
        .get(KMeansCentroidSketchHadoopMapper.CENTROID_SKETCH_MAP_TASK_OPTIONS);

    // determine if this is the first iteration:
    // if so then we need to deal with the distance function
    // with respect to global priming data
    try {
      if (!DistributedJobConfig.isEmpty(taskOptsS)) {
        String[] taskOpts = Utils.splitOptions(taskOptsS);

        // name of the training ARFF header file
        String arffHeaderFileName = Utils.getOption("arff-header", taskOpts);
        if (DistributedJobConfig.isEmpty(arffHeaderFileName)) {
          throw new IOException(
            "Can't continue without the name of the ARFF header file!");
        }
        Instances headerWithSummary =
          WekaClassifierHadoopMapper.loadTrainingHeader(arffHeaderFileName);

        // first iteration?
        m_isFirstIteration = Utils.getFlag("first-iteration", taskOpts);

        KMeansMapTask forFilteringOnly = new KMeansMapTask();
        forFilteringOnly.setOptions(taskOpts);

        // gets us the header (sans summary attributes) after it has passed
        // through any filters that the user may have specified (including the
        // replace missing values filter)
        m_transformedHeaderNoSummary = forFilteringOnly.init(headerWithSummary);
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void reduce(Text key, Iterable<BytesWritable> values, Context context)
    throws IOException {

    int runNum = 0;
    String rS = key.toString();
    rS = rS.replace("run", "");
    try {
      runNum = Integer.parseInt(rS);
    } catch (NumberFormatException ex) {
      throw new IOException(ex);
    }
    CentroidSketch initial = null;

    List<NormalizableDistance> distsForRun =
      new ArrayList<NormalizableDistance>();
    try {
      for (BytesWritable b : values) {
        byte[] bytes = b.getBytes();

        CentroidSketch current = deserialize(bytes);
        if (initial == null) {
          initial = current;
        } else {
          initial.aggregateReservoir(current.getReservoirSample());
        }

        if (m_isFirstIteration) {
          distsForRun.add(current.getDistanceFunction());
        }
      }

      // add the reservoir to the current sketch
      initial.addReservoirToCurrentSketch();

      // update the distance function with global numeric
      // attribute ranges
      if (m_isFirstIteration) {
        Instances distancePrimingData =
          KMeansReduceTask
            .computeDistancePrimingDataFromDistanceFunctions(
              distsForRun,
              m_transformedHeaderNoSummary);

        initial.getDistanceFunction().setInstances(distancePrimingData);
      }

      // save the sketch out
      writeSketchToDestination(initial, m_outputDestination, runNum,
        context.getConfiguration());

      System.err.println("Number of instances in sketch for run " + runNum
        + ": " + initial.getCurrentSketch().numInstances());
      Text outKey = new Text();
      outKey.set("Summary:\n");
      Text outVal = new Text();
      outVal.set("Number of instances in sketch for run " + runNum + ": "
        + initial.getCurrentSketch().numInstances());

      context.write(outKey, outVal);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Writes the sketch for the given run number out to the destination as
   * "sketch_run#.ser".
   * 
   * @param sketch the sketch to save
   * @param outputDestination the output directory in HDFS
   * @param runNumber the run number of the sketch
   * @param conf the configuration to use
   * @throws IOException if a problem occurs
   */
  protected static void writeSketchToDestination(CentroidSketch sketch,
    String outputDestination, int runNumber, Configuration conf)
    throws IOException {
    ObjectOutputStream oos = null;
    try {

      Path pt = new Path(outputDestination + "/sketch_run" + runNumber);
      FileSystem fs = FileSystem.get(conf);
      if (fs.exists(pt)) {
        // remove the file
        fs.delete(pt, true);
      }

      FSDataOutputStream fout = fs.create(pt);
      BufferedOutputStream bos = new BufferedOutputStream(fout);
      oos = new ObjectOutputStream(bos);
      oos.writeObject(sketch);
    } finally {
      if (oos != null) {
        oos.flush();
        oos.close();
      }
    }
  }

  /**
   * Deserializes a CentroidSketch from an array of bytes
   * 
   * @param bytes the bytes to deserialize from
   * @return the deserialized CentroidSketch
   * @throws IOException if a problem occurs
   * @throws ClassNotFoundException if a class can't be found
   */
  protected static CentroidSketch deserialize(byte[] bytes) throws IOException,
    ClassNotFoundException {

    ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
    ObjectInputStream p = null;
    Object toReturn = null;

    try {
      p = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
        istream)));

      toReturn = p.readObject();
      if (!(toReturn instanceof CentroidSketch)) {
        throw new IOException(
          "Object deserialized was not a CentroidSketch object!");
      }
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return (CentroidSketch) toReturn;
  }
}
