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
 *    RandomizedChunkHadoopReducer
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import weka.core.Attribute;
import weka.core.ChartUtils.NumericAttributeBinData;
import weka.core.Instances;
import weka.core.QuantileCalculator;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderMapTask.ArffSummaryNumericMetric;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import distributed.core.DistributedJobConfig;

/**
 * Hadoop Reducer implementation for the job that creates randomly shuffled (and
 * stratified if the class is nominal) data chunks. Keyed by data chunk number,
 * it deals instances out per class in a round-robin fashion amongst the output
 * files.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class RandomizedDataChunkHadoopReducer extends
  Reducer<Text, Text, Text, Text> {

  /**
   * The key in the Configuration that the options for this task are associated
   * with
   */
  public static String NUM_DATA_CHUNKS =
    "*weka.distributed.num_randomized_data_chunks";

  /** The output files */
  protected MultipleOutputs<Text, Text> m_mos;

  /** The header of the data */
  protected Instances m_trainingHeader;

  /** The header with summary attributes */
  protected Instances m_trainingHeaderWithSummary;

  /** For calculating quartiles */
  protected QuantileCalculator m_quartiles;

  /** Used when computing quartiles (sub task for updating summary attributes) */
  protected CSVToARFFHeaderMapTask m_rowHelper;

  /**
   * Full path to the header - used when we write back the header updated with
   * quartiles
   */
  protected String m_arffHeaderFullPath = "";

  /** The total number of data chunks */
  protected int m_numberOfDataChunks;

  /** Whether we should compute quartiles/histograms */
  protected boolean m_computeQuartiles;

  /**
   * Whether we should generate charts (only applies when quartiles are being
   * generated)
   */
  protected boolean m_generateCharts;

  /** Only compute quartiles - don't write data chunks */
  protected boolean m_computeQuartilesOnly;

  /**
   * Used to build histograms for numeric attributes (sub task for producing
   * attribute charts)
   */
  protected Map<Integer, NumericAttributeBinData> m_numericHistogramData;

  /**
   * A buffer for minority-class instances. Only comes into affect in the case
   * where there are fewer instances of a class than there are requested data
   * chunks. In this case we will sample randomly from our buffer in order to
   * ensure that each data chunk gets at least one instance of a particular
   * class
   */
  protected List<List<String>> m_classInstancesBuffer;

  /** Keeps track of how many instances per class we've seen */
  protected int[] m_countsPerClass;

  /**
   * Used for oversampling minority classes to ensure that each data chunk gets
   * one instance of each minority class
   */
  protected Random m_random = new Random(42);

  /** Holds output values */
  protected Text m_outVal = new Text();

  @Override
  public void setup(Context context) throws IOException {
    m_mos = new MultipleOutputs<Text, Text>(context);

    Configuration conf = context.getConfiguration();

    String taskOptsS = conf.get(NUM_DATA_CHUNKS);
    String randomizeMapOpts =
      conf
        .get(RandomizedDataChunkHadoopMapper.RANDOMIZED_DATA_CHUNK_MAP_TASK_OPTIONS);
    if (taskOptsS == null || DistributedJobConfig.isEmpty(taskOptsS)) {
      throw new IOException(
        "Number of output files/data chunks not available!!");
    }

    try {
      if (!DistributedJobConfig.isEmpty(randomizeMapOpts)) {
        String[] taskOpts = Utils.splitOptions(randomizeMapOpts);

        // name of the training ARFF header file
        String arffHeaderFileName = Utils.getOption("arff-header", taskOpts);
        if (DistributedJobConfig.isEmpty(arffHeaderFileName)) {
          throw new IOException(
            "Can't continue without the name of the ARFF header file!");
        }
        m_trainingHeaderWithSummary =
          WekaClassifierHadoopMapper.loadTrainingHeader(arffHeaderFileName);
        m_trainingHeader =
          CSVToARFFHeaderReduceTask
            .stripSummaryAtts(m_trainingHeaderWithSummary);

        m_arffHeaderFullPath =
          Utils.getOption("arff-header-full-path", taskOpts);

        m_computeQuartiles = Utils.getFlag("compute-quartiles", taskOpts);
        m_computeQuartilesOnly = Utils.getFlag("quartiles-only", taskOpts);
        m_generateCharts = Utils.getFlag("charts", taskOpts);

        try {
          m_numberOfDataChunks = Integer.parseInt(taskOptsS);
          // m_instanceBuffer = new ArrayList<String>(m_numberOfDataChunks);
        } catch (NumberFormatException e) {
          throw new Exception(e);
        }

        // scan for numeric atts and whether quartiles have been
        // computed yet
        m_computeQuartiles =
          (m_computeQuartiles || m_computeQuartilesOnly)
            && !DistributedJobConfig.isEmpty(m_arffHeaderFullPath);
        if (m_computeQuartiles) {
          for (int i = 0; i < m_trainingHeader.numAttributes(); i++) {
            if (m_trainingHeader.attribute(i).isNumeric()) {
              Attribute summary =
                m_trainingHeaderWithSummary
                  .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
                    + m_trainingHeader.attribute(i).name());

              if (summary != null) {
                if (!Utils
                  .isMissingValue(ArffSummaryNumericMetric.FIRSTQUARTILE
                    .valueFromAttribute(summary))) {
                  m_computeQuartiles = false;
                  break;
                }
              }
            }
          }
        }

        if (m_computeQuartiles) {
          m_quartiles =
            new QuantileCalculator(m_trainingHeader, new double[] { 0.25, 0.5,
              0.75 });

          m_numericHistogramData =
            new HashMap<Integer, NumericAttributeBinData>();

          // setup numeric att bin classes
          for (int i = 0; i < m_trainingHeader.numAttributes(); i++) {
            if (m_trainingHeader.attribute(i).isNumeric()) {
              Attribute summary =
                m_trainingHeaderWithSummary
                  .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
                    + m_trainingHeader.attribute(i).name());

              if (summary == null) {
                throw new DistributedWekaException(
                  "Can't find summary attribute for att: "
                    + m_trainingHeader.attribute(i).name());
              }

              m_numericHistogramData.put(i, new NumericAttributeBinData(
                m_trainingHeader.attribute(i).name(), summary));
            }
          }
          m_quartiles.setHistogramMap(m_numericHistogramData);
        }

        WekaClassifierHadoopMapper.setClassIndex(taskOpts, m_trainingHeader,
          true);

        if (m_computeQuartiles) {
          // set up a row helper. Simply for parsing CSV values so that we can
          // do quantile updates
          m_rowHelper = new CSVToARFFHeaderMapTask();
          // taskOptsS =
          // conf.get(WekaClassifierHadoopMapper.CLASSIFIER_MAP_TASK_OPTIONS);
          String csvOptsS =
            conf
              .get(CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS);
          if (!DistributedJobConfig.isEmpty(csvOptsS)) {
            String[] csvOpts = Utils.splitOptions(csvOptsS);
            m_rowHelper.setOptions(csvOpts);
          }
          // WekaClassifierHadoopMapper.setClassIndex(taskOpts,
          // m_trainingHeader,
          // true);
          m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
            .instanceHeaderToAttributeNameList(m_trainingHeader));
        }
      } else {
        throw new Exception(
          "Can't continue without the name of the ARFF header file!");
      }

      int numClasses = 1;
      if (m_trainingHeader.classIndex() >= 0
        && m_trainingHeader.classAttribute().isNominal()) {
        numClasses = m_trainingHeader.classAttribute().numValues();

        // only need the instances buffer if the class is nominal
        m_classInstancesBuffer = new ArrayList<List<String>>();
        for (int i = 0; i < numClasses; i++) {
          m_classInstancesBuffer.add(new ArrayList<String>());
        }
      }
      m_countsPerClass = new int[numClasses];

    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  protected void randomizeAndStratify(Iterable<Text> values)
    throws InterruptedException, IOException {

    for (Text t : values) {
      String row = t.toString();
      String[] parts = row.split("@:@");
      String inst = parts[0];

      if (m_quartiles != null) {
        // do quartile updates
        String[] parsed = m_rowHelper.parseRowOnly(inst);
        try {
          m_quartiles.update(parsed, m_rowHelper.getMissingValue());
        } catch (Exception ex) {
          throw new IOException(ex);
        }
      }
      // } else if (m_numericHistogramData != null) {
      // String[] parsed = m_rowHelper.parseRowOnly(inst);
      // updateHistogramData(parsed, m_rowHelper.getMissingValue());
      // }

      if (!m_computeQuartilesOnly) {
        int classVal = Integer.parseInt(parts[1]);

        int chunk = m_countsPerClass[classVal] % m_numberOfDataChunks;
        String name = "chunk" + chunk;
        m_outVal.set(inst);
        m_mos.write(name, null, m_outVal);

        // add to the minority class buffers. If we've seen
        // at least m_numberOfDataChunks instances for this class then
        // each data chunk will have at least one instance of this class
        if (m_countsPerClass[classVal] < m_numberOfDataChunks) {
          m_classInstancesBuffer.get(classVal).add(inst);
        }

        m_countsPerClass[classVal]++;
      }
    }
  }

  protected void randomize(Iterable<Text> values) throws InterruptedException,
    IOException {
    // instances have already been shuffled over the chunks by
    // the map task, so we just deal the ones for this particular
    // key out evenly over the output files

    for (Text t : values) {
      if (m_quartiles != null) {
        // do quartile updates
        String row = t.toString();
        String[] parsed = m_rowHelper.parseRowOnly(row);
        try {
          m_quartiles.update(parsed, m_rowHelper.getMissingValue());
        } catch (Exception ex) {
          throw new IOException(ex);
        }
      }
      // } else if (m_numericHistogramData != null) {
      // String row = t.toString();
      // String[] parsed = m_rowHelper.parseRowOnly(row);
      // updateHistogramData(parsed, m_rowHelper.getMissingValue());
      // }

      if (!m_computeQuartilesOnly) {
        int chunk = m_countsPerClass[0] % m_numberOfDataChunks;
        String name = "chunk" + chunk;
        m_mos.write(name, null, t);

        m_countsPerClass[0]++;
      }
    }
  }

  @Override
  public void reduce(Text key, Iterable<Text> values, Context context)
    throws IOException, InterruptedException {
    if (m_trainingHeader.classAttribute().isNumeric()) {
      randomize(values);
    } else {
      randomizeAndStratify(values);
    }
  }

  @Override
  public void cleanup(Context context) throws IOException, InterruptedException {
    // Here is where we will oversample minority classes if necessary
    // in order to ensure that each class is represented in each data
    // chunk

    if (!m_computeQuartilesOnly) {
      for (int i = 0; i < m_countsPerClass.length; i++) {

        // make sure we skip empty classes by checking for > 0
        if (m_countsPerClass[i] > 0
          && m_countsPerClass[i] < m_numberOfDataChunks) {
          while (m_countsPerClass[i] < m_numberOfDataChunks) {

            // choose randomly from the instances we've seen for class index i
            int instIndex =
              m_random.nextInt(m_classInstancesBuffer.get(i).size());
            m_outVal.set(m_classInstancesBuffer.get(i).get(instIndex));
            String name = "chunk" + m_countsPerClass[i];
            m_mos.write(name, null, m_outVal);

            m_countsPerClass[i]++;
          }
        }
      }

      m_mos.close();
    }

    // any quantile computation piggybacked on this run?
    if (m_quartiles != null) {
      try {
        Instances updatedHeader =
          CSVToARFFHeaderReduceTask
            .updateSummaryAttsWithQuartilesAndHistograms(
              m_trainingHeaderWithSummary, m_quartiles, m_numericHistogramData);

        CSVToArffHeaderHadoopReducer.writeHeaderToDestination(updatedHeader,
          m_arffHeaderFullPath, context.getConfiguration());

        m_trainingHeaderWithSummary = updatedHeader;
      } catch (DistributedWekaException e) {
        throw new IOException(e);
      }
    }

    // charts
    if (m_generateCharts) {
      Configuration conf = context.getConfiguration();
      String outputPath =
        m_arffHeaderFullPath
          .substring(0, m_arffHeaderFullPath.lastIndexOf("/"));
      if (m_numericHistogramData != null) {
        CSVToArffHeaderHadoopReducer
          .writeAttributeChartsIfNecessary(m_trainingHeaderWithSummary,
            m_numericHistogramData, outputPath, conf);
      } else {
        try {
          boolean containsNumeric =
            CSVToARFFHeaderReduceTask
              .headerContainsNumericAttributes(m_trainingHeaderWithSummary);
          boolean generate =
            !containsNumeric
              || (containsNumeric && CSVToARFFHeaderReduceTask
                .headerContainsQuartiles(m_trainingHeaderWithSummary));

          if (generate) {
            CSVToArffHeaderHadoopReducer.writeAttributeChartsIfNecessary(
              m_trainingHeaderWithSummary, outputPath, conf);
          }
        } catch (DistributedWekaException e) {
          throw new IOException(e);
        }
      }
    }
  }
}
