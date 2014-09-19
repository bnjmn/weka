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
 *    KMeansHadoopReducer
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

import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import weka.distributed.KMeansMapTask;
import weka.distributed.KMeansReduceTask;
import distributed.core.DistributedJobConfig;

/**
 * Hadoop reducer for a k-means iteration
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class KMeansHadoopReducer extends
  Reducer<Text, BytesWritable, Text, Text> {

  /**
   * The key in the configuration that the write path for k-means centroids is
   * associated with
   */
  public static final String KMEANS_WRITE_PATH =
    "*weka.distributed.kmeans_write_path";

  /** File prefix for serialized reducers */
  public static final String KMEANS_REDUCE_FILE_PREFIX = "reduce_run";

  /** The output destination to write centroids to */
  protected String m_outputDestination;

  /** The iteration being performed */
  protected int m_iterationNumber;

  @Override
  public void setup(Context context) throws IOException {
    Configuration conf = context.getConfiguration();
    m_outputDestination = conf.get(KMEANS_WRITE_PATH);
    if (DistributedJobConfig.isEmpty(m_outputDestination)) {
      throw new IOException("No output path for centroids supplied!");
    }

    String taskOptsS =
      conf
        .get(KMeansHadoopMapper.KMEANS_MAP_TASK_OPTIONS);

    try {
      if (!DistributedJobConfig.isEmpty(taskOptsS)) {
        String[] taskOpts = Utils.splitOptions(taskOptsS);

        String iterationNum = Utils.getOption("iteration", taskOpts);
        if (DistributedJobConfig.isEmpty(iterationNum)) {
          throw new IOException(
            "Unable to continue without knowing the current iteration number!");
        }
        try {
          m_iterationNumber = Integer.parseInt(iterationNum);
        } catch (NumberFormatException ex) {
          throw new IOException(ex);
        }
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

    Instances transformedHeaderNoSummary = null;
    List<List<Instances>> partialClusterSummariesForRun =
      new ArrayList<List<Instances>>();

    try {
      for (BytesWritable b : values) {
        byte[] bytes = b.getBytes();
        KMeansMapTask current = deserialize(bytes);
        if (transformedHeaderNoSummary == null) {
          transformedHeaderNoSummary = current.getTransformedHeader();
        }

        partialClusterSummariesForRun.add(current.getCentroidStats());
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }

    if (transformedHeaderNoSummary != null
      && partialClusterSummariesForRun.size() > 0) {
      KMeansReduceTask reducer = new KMeansReduceTask();

      try {
        reducer =
          reducer.reduceClusters(runNum, m_iterationNumber,
            transformedHeaderNoSummary, partialClusterSummariesForRun);

        writeReduceTaskToDestination(reducer, m_outputDestination, runNum,
          context.getConfiguration());

        System.err.println("Wrote reducer for run: " + runNum
          + ". Total within clust err: "
          + reducer.getTotalWithinClustersError());
      } catch (DistributedWekaException e) {
        throw new IOException(e);
      }
    } else {
      if (transformedHeaderNoSummary == null) {
        throw new IOException(
          "Was unable to get the transformed header from the KMeansMapTasks!");
      }
      if (partialClusterSummariesForRun.size() == 0) {
        throw new IOException("There were no custer summaries to aggregate!");
      }
    }
  }

  protected static void writeReduceTaskToDestination(KMeansReduceTask toSave,
    String outputPath, int runNum, Configuration conf) throws IOException {

    ObjectOutputStream oos = null;
    try {
      Path pt = new Path(outputPath + "/" + KMEANS_REDUCE_FILE_PREFIX + runNum);
      FileSystem fs = FileSystem.get(conf);
      if (fs.exists(pt)) {
        fs.delete(pt, true);
      }

      FSDataOutputStream fout = fs.create(pt);
      BufferedOutputStream bos = new BufferedOutputStream(fout);
      oos = new ObjectOutputStream(bos);
      oos.writeObject(toSave);
    } finally {
      if (oos != null) {
        oos.flush();
        oos.close();
      }
    }
  }

  protected static KMeansMapTask deserialize(byte[] bytes) throws IOException,
    ClassNotFoundException {
    ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
    ObjectInputStream p = null;
    Object toReturn = null;

    try {
      p = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
        istream)));

      toReturn = p.readObject();
      if (!(toReturn instanceof KMeansMapTask)) {
        throw new IOException(
          "Object deserialized was not a KMeansMapTask object!");
      }
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return (KMeansMapTask) toReturn;
  }
}
