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
 *    WekaClassifierHadoopMapper
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
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

import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.WekaClassifierReduceTask;
import distributed.core.DistributedJobConfig;

public class WekaClassifierHadoopReducer extends
  Reducer<Text, BytesWritable, Text, Text> {

  /**
   * The key in the Configuration that the write path for the model is
   * associated with
   */
  public static final String CLASSIFIER_WRITE_PATH =
    "*weka.distributed.weka_classifier_write_path";

  /**
   * The key in the configuration that the minimum training fraction is
   * associated with
   */
  public static final String MIN_TRAINING_FRACTION =
    "*weka.distributed.weka_classifier_min_training_fraction";

  /** The underlying general Weka classifier reduce task */
  protected WekaClassifierReduceTask m_task = null;

  /**
   * The in the configuration to check for in order to prevent textual output of
   * the aggregated model
   */
  public static final String SUPPRESS_CLASSIFIER_TEXT_OUT =
    "weka.classifier.reducer.suppressTextOut";

  /**
   * If true, then the textual output of the aggregated classifier is not
   * written out. Use property weka.classifier.reducer.suppressTextOut=true.
   */
  protected boolean m_suppressAggregatedClassifierTextualOutput;

  @Override
  public void setup(Context context) throws IOException {
    m_task = new WekaClassifierReduceTask();
    String suppress =
      context.getConfiguration().get(SUPPRESS_CLASSIFIER_TEXT_OUT);
    if (!DistributedJobConfig.isEmpty(suppress)) {
      m_suppressAggregatedClassifierTextualOutput =
        suppress.toLowerCase().equals("true");
    }
  }

  @Override
  public void reduce(Text key, Iterable<BytesWritable> values, Context context)
    throws IOException {

    Configuration conf = context.getConfiguration();

    String outputDestination = conf.get(CLASSIFIER_WRITE_PATH);

    if (outputDestination == null || outputDestination.length() == 0) {
      throw new IOException("No destination given for aggregated classifier");
    }

    String minTrainingFrac = conf.get(MIN_TRAINING_FRACTION);
    if (!DistributedJobConfig.isEmpty(minTrainingFrac)) {
      double frac = Double.parseDouble(minTrainingFrac);
      if (frac > 1) {
        frac /= 100.0;
      }
      m_task.setMinTrainingFraction(frac);
    }

    String mapTaskOpts = conf
      .get(WekaClassifierHadoopMapper.CLASSIFIER_MAP_TASK_OPTIONS);
    boolean forceVote = false;
    try {
      forceVote = Utils.getFlag("force-vote", Utils.splitOptions(mapTaskOpts));
    } catch (Exception e) {
      throw new IOException(e);
    }

    List<Classifier> classifiersToAgg = new ArrayList<Classifier>();
    List<Integer> numTrainingInstancesPerClassifier = new ArrayList<Integer>();

    try {
      for (BytesWritable b : values) {
        byte[] bytes = b.getBytes();

        List<Object> info = deserialize(bytes);
        classifiersToAgg.add((Classifier) info.get(0));
        numTrainingInstancesPerClassifier.add((Integer) info.get(1));
      }
    } catch (ClassNotFoundException ex) {
      throw new IOException(ex);
    }

    try {
      Classifier aggregated = m_task.aggregate(classifiersToAgg,
        numTrainingInstancesPerClassifier, forceVote);

      int numAggregated = classifiersToAgg.size();
      classifiersToAgg = null; // save memory
      System.gc();
      Runtime currR = Runtime.getRuntime();
      long freeM = currR.freeMemory();
      long totalM = currR.totalMemory();
      long maxM = currR.maxMemory();
      System.err
        .println("[WekaClassifierHadoopReducer] Memory (free/total/max.) in bytes: "
          + String.format("%,d", freeM) + " / "
          + String.format("%,d", totalM) + " / "
          + String.format("%,d", maxM));

      writeClassifierToDestination(aggregated, outputDestination, conf);

      Text outkey = new Text();
      outkey.set("Summary:\n");
      Text outVal = new Text();
      StringBuffer buff = new StringBuffer();
      buff
        .append("Number of training instances processed by each classifier: ");
      for (Integer i : numTrainingInstancesPerClassifier) {
        buff.append(i).append(" ");
      }
      if (m_task.getDiscarded().size() > 0) {
        buff.append("\nThere was one classifier not aggregated because it "
          + "had seen less than " + m_task.getMinTrainingFraction() * 100.0
          + "% of amount of data (" + m_task.getDiscarded().get(0)
          + " instances) that the others had\n");
      }
      outVal.set("Number of classifiers aggregated: " + numAggregated
        + ". Final classifier is a " + aggregated.getClass().getName() + "\n"
        + buff.toString());
      context.write(outkey, outVal);

      if (!m_suppressAggregatedClassifierTextualOutput) {
        outkey.set("Aggregated model:\n");
        outVal.set(aggregated.toString());
        context.write(outkey, outVal);
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Helper routine for serializing the final aggregated model (plus header) to
   * the output directory.
   * 
   * @param classifier the classifier to serialize
   * @param outputDestination the output destination in HDFS
   * @param conf the Configuration for the job
   * @throws DistributedWekaException if a problem occurs
   * @throws IOException if a problem occurs
   */
  protected static void writeClassifierToDestination(Classifier classifier,
    String outputDestination, Configuration conf)
    throws DistributedWekaException, IOException {

    // try and write the header out with the classifier too
    String mapperTaskOpts = conf
      .get(WekaClassifierHadoopMapper.CLASSIFIER_MAP_TASK_OPTIONS);

    Instances header = null;
    try {
      String[] taskOpts = Utils.splitOptions(mapperTaskOpts);
      String arffHeaderFileName = Utils.getOption("arff-header", taskOpts);
      if (!DistributedJobConfig.isEmpty(arffHeaderFileName)) {
        header = CSVToARFFHeaderReduceTask
          .stripSummaryAtts(WekaClassifierHadoopMapper
            .loadTrainingHeader(arffHeaderFileName));
        WekaClassifierHadoopMapper.setClassIndex(taskOpts, header, true);
      } else {
        System.err
          .println("WekaClassifierHadoopReducer - unable to load training header from "
            + "the distributed cache. Will only save the classifier.");
      }
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }

    ObjectOutputStream oos = null;
    try {
      // if (!outputDestination.startsWith("hdfs://")) {
      // outputDestination = constructHDFSURI(outputDestination, conf);
      // }

      Path pt = new Path(outputDestination);
      FileSystem fs = FileSystem.get(conf);
      if (fs.exists(pt)) {
        // remove the file
        fs.delete(pt, true);
      }

      FSDataOutputStream fout = fs.create(pt);
      BufferedOutputStream bos = new BufferedOutputStream(fout);
      oos = new ObjectOutputStream(bos);
      oos.writeObject(classifier);
      if (header != null) {
        oos.writeObject(header);
      }
    } finally {
      if (oos != null) {
        oos.flush();
        oos.close();
      }
    }

    // we'll write out the header (as <model name>_arffHeader.arff)
    // to the output directory too. Users can use this header with
    // future data to ensure compatibility (rather than having to
    // wrap the saved model in an InputMappedClassifier).
    String path = outputDestination.substring(0,
      outputDestination.lastIndexOf("/"));
    String modelName = outputDestination.substring(
      outputDestination.lastIndexOf("/") + 1, outputDestination.length());
    modelName = modelName.replace(".model", "").replace(".MODEL", "");
    path += "/" + modelName + "_arffHeader.arff";
    CSVToArffHeaderHadoopReducer.writeHeaderToDestination(header, path, conf);
  }

  /**
   * Helper routine for constructing a HDFS URL
   * 
   * @param path the path to construct the URL for
   * @param conf the Configuration for the job
   * @return a HDFS URL as a string
   */
  // protected static String constructHDFSURI(String path, Configuration conf) {
  // String hostPort = conf.get("fs.default.name");
  //
  // if (!hostPort.endsWith("/") && !path.endsWith("/")) {
  // hostPort += "/";
  // }
  //
  // hostPort += path;
  //
  // return hostPort;
  // }

  /**
   * Deserializes a classifier and an integer holding the number of instances it
   * was trained on
   * 
   * @param bytes an array of bytes containing the compressed serialized
   *          classifier
   * @return a list holding the classifier and the number of instances it was
   *         trained on
   * @throws IOException if a problem occurs
   * @throws ClassNotFoundException if a class can't be loaded
   */
  protected List<Object> deserialize(byte[] bytes) throws IOException,
    ClassNotFoundException {
    ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
    ObjectInputStream p = null;
    Object toReturn = null;
    List<Object> returnVals = new ArrayList<Object>(2);
    try {
      p = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
        istream)));

      toReturn = p.readObject();
      if (!(toReturn instanceof Classifier)) {
        throw new IOException(
          "Object deserialized was not a Classifier object!");
      }

      // grab the number of training instances
      Integer numTrainingInsts = (Integer) p.readObject();
      returnVals.add(toReturn);
      returnVals.add(numTrainingInsts);
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return returnVals;
  }
}
