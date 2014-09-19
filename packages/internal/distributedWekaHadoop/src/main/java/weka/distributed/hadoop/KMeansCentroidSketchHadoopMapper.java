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
 *    KMeansCentroidSketchHadoopMapper
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import weka.clusterers.CentroidSketch;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.KMeansMapTask;
import distributed.core.DistributedJobConfig;

/**
 * Hadoop mapper for the k-means|| initialization procedure. Uses weighted
 * reservoir sampling to maintain a random selection of the instances with the
 * greatest distance from the current set of centroid candidates.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class KMeansCentroidSketchHadoopMapper extends
  Mapper<LongWritable, Text, Text, BytesWritable> {

  /**
   * The key in the Configuration that the options for this task are associated
   * with
   */
  public static final String CENTROID_SKETCH_MAP_TASK_OPTIONS =
    "*weka.distributed.centroid_sketch_map_task_opts";

  /** File prefix for serialized sketch files */
  public static final String SKETCH_FILE_PREFIX = "sketch_run";

  /** The underlying centroid sketch tasks - one for each run */
  protected CentroidSketch[] m_tasks;

  /** Use a configured KMeansMapTask to apply user-specified filters to the data */
  protected KMeansMapTask m_forFilteringOnly;

  /** Helper Weka CSV map task - used simply for parsing CSV entering the map */
  protected CSVToARFFHeaderMapTask m_rowHelper;

  /** The ARFF header of the data */
  protected Instances m_trainingHeader;

  /** The number of runs of k-means being performed */
  protected int m_numRuns = 1;

  /** True if this is the first iteration of the k-means|| initialization */
  protected boolean m_isFirstIteration;

  /**
   * Helper method for loading serialized centroid sketches from the distributed
   * cache
   * 
   * @param prefix the filename prefix for serialized centroid sketches
   * @param numRuns the number of runs (and hence the number of sketches) that
   *          we're expecting
   * @return an array of CentroidSketch objects (one for each run)
   * @throws Exception if a problem occurs
   */
  protected static CentroidSketch[] loadSketchesFromRunFiles(String prefix,
    int numRuns) throws Exception {
    CentroidSketch[] sketches = new CentroidSketch[numRuns];

    for (int i = 0; i < numRuns; i++) {
      File f = new File(prefix + i);

      if (!f.exists()) {
        throw new IOException("The centroid sketch file '" + f.toString()
          + "' does not seem to exist in the distributed cache!");
      }

      ObjectInputStream ois = null;

      try {
        ois =
          new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));

        CentroidSketch s = (CentroidSketch) ois.readObject();
        sketches[i] = s;
      } finally {
        if (ois != null) {
          ois.close();
        }
      }
    }

    for (CentroidSketch sketche : sketches) {
      System.err.println("Starting sketch - num instances in sketch: "
        + sketche.getCurrentSketch().numInstances());
    }

    return sketches;
  }

  @Override
  public void setup(Context context) throws IOException {
    m_rowHelper = new CSVToARFFHeaderMapTask();

    Configuration conf = context.getConfiguration();
    String taskOptsS = conf.get(CENTROID_SKETCH_MAP_TASK_OPTIONS);
    String csvOptsS =
      conf.get(CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS);

    try {
      if (!DistributedJobConfig.isEmpty(csvOptsS)) {
        String[] csvOpts = Utils.splitOptions(csvOptsS);
        m_rowHelper.setOptions(csvOpts);
      }

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
        m_trainingHeader =
          CSVToARFFHeaderReduceTask
            .stripSummaryAtts(headerWithSummary);

        m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
          .instanceHeaderToAttributeNameList(m_trainingHeader));

        // num runs
        String numRuns = Utils.getOption("num-runs", taskOpts);
        if (!DistributedJobConfig.isEmpty(numRuns)) {
          try {
            m_numRuns = Integer.parseInt(numRuns);
          } catch (NumberFormatException ex) {
            throw new IOException(
              "Unable to parse number of runs from -num-runs option");
          }
        } else {
          throw new IOException(
            "Unable to continue without knowing how many runs are being performed!");
        }

        // first iteration?
        m_isFirstIteration = Utils.getFlag("first-iteration", taskOpts);

        // load centroid sketches from the distributed cache
        // m_tasks = loadSketches("centroidSketches.ser");
        m_tasks = loadSketchesFromRunFiles(SKETCH_FILE_PREFIX, m_numRuns);

        // init a KMeansMapTask to use for data filtering
        m_forFilteringOnly = new KMeansMapTask();
        try {
          m_forFilteringOnly.setOptions(taskOpts);

          m_forFilteringOnly.init(headerWithSummary);
        } catch (Exception ex) {
          throw new IOException(ex);
        }
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  protected void processRow(String row) throws IOException {
    if (row != null) {
      String[] parsed = m_rowHelper.parseRowOnly(row);

      if (parsed.length != m_trainingHeader.numAttributes()) {
        throw new IOException(
          "Parsed a row that contains a different number of values than "
            + "there are attributes in the training ARFF header: " + row);
      }

      try {
        Instance toProcess =
          m_rowHelper.makeInstance(m_trainingHeader, true, parsed);

        // make sure it goes through any filters first!
        toProcess = m_forFilteringOnly.applyFilters(toProcess);

        for (int k = 0; k < m_numRuns; k++) {
          m_tasks[k].process(toProcess, m_isFirstIteration);
        }

      } catch (Exception ex) {
        throw new IOException(ex);
      }
    }
  }

  @Override
  public void map(LongWritable key, Text value, Context context)
    throws IOException {
    if (value != null) {
      processRow(value.toString());
    }
  }

  protected static byte[] sketchToBytes(CentroidSketch sketch)
    throws IOException {
    ObjectOutputStream p = null;
    byte[] bytes = null;

    try {
      ByteArrayOutputStream ostream = new ByteArrayOutputStream();
      OutputStream os = ostream;

      p =
        new ObjectOutputStream(new BufferedOutputStream(
          new GZIPOutputStream(os)));
      p.writeObject(sketch);
      p.flush();
      p.close();

      bytes = ostream.toByteArray();

      p = null;
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return bytes;
  }

  @Override
  public void cleanup(Context context) throws IOException, InterruptedException {
    // emit serialized sketch tasks with run number as key
    for (int i = 0; i < m_tasks.length; i++) {
      System.err.println("Number of instances in sketch: "
        + m_tasks[i].getCurrentSketch().numInstances());
      System.err.println("Number of instances in reservoir: "
        + m_tasks[i].getReservoirSample().getSample().size());
      byte[] bytes = sketchToBytes(m_tasks[i]);
      String runNum = "run" + i;
      Text key = new Text();
      key.set(runNum);
      BytesWritable value = new BytesWritable();
      value.set(bytes, 0, bytes.length);
      context.write(key, value);
    }
  }
}
