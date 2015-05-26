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
 *    WekaFoldBasedClassifierHadoopReducer
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;

import weka.classifiers.Classifier;
import weka.core.Utils;

/**
 * Reducer implementation for the model-building phase of the evaluation job.
 * Subclasses WekaClassifierHadoopReducer in order to reduce keyed on fold
 * number.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaFoldBasedClassifierHadoopReducer extends
  WekaClassifierHadoopReducer {

  @Override
  public void reduce(Text key, Iterable<BytesWritable> values, Context context)
    throws IOException {

    Configuration conf = context.getConfiguration();

    String outputDestination = conf.get(CLASSIFIER_WRITE_PATH);

    if (outputDestination == null || outputDestination.length() == 0) {
      throw new IOException("No destination given for aggregated classifier");
    }

    // need to prepend the fold number so that the evaluation phase
    // (or the next iteration of incremental learning) can load
    // the appropriate aggregated classifier for the fold being
    // considered
    String foldString = key.toString();
    foldString = foldString.substring(foldString.lastIndexOf("_") + 1,
      foldString.length());
    int fold = -1;
    try {
      fold = Integer.parseInt(foldString.trim());
    } catch (NumberFormatException n) {
      throw new IOException(n);
    }

    String modelNameOnly = outputDestination.substring(
      outputDestination.lastIndexOf("/") + 1, outputDestination.length());
    outputDestination = outputDestination.substring(0,
      outputDestination.lastIndexOf("/") + 1);
    outputDestination += ("" + fold + "_" + modelNameOnly);

    String minTrainingFrac = conf.get(MIN_TRAINING_FRACTION);
    if (minTrainingFrac != null && minTrainingFrac.length() > 0) {
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
    } catch (Exception ex) {
      throw new IOException(ex);
    }

    try {
      Classifier aggregated = m_task.aggregate(classifiersToAgg,
        numTrainingInstancesPerClassifier, forceVote);
      writeClassifierToDestination(aggregated, outputDestination, conf);

      int numAggregated = classifiersToAgg.size();
      classifiersToAgg = null;
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

      Text outkey = new Text();
      outkey.set("Summary for fold number " + fold + ":\n");
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
        outkey.set("Aggregated model for fold number " + fold + ":\n");
        outVal.set(aggregated.toString());
        context.write(outkey, outVal);
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }
}
