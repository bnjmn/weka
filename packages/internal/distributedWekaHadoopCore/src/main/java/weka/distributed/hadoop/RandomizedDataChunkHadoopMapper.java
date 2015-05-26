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
 *    RandomizedChunkHadoopMapper
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import distributed.core.DistributedJobConfig;

/**
 * Hadoop Mapper implementation for the job that creates randomly shuffled (and
 * stratified if the class is nominal) data chunks. Distributes rows by randomly
 * assigning them a key from the range 1 - num chunks.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class RandomizedDataChunkHadoopMapper extends
  Mapper<LongWritable, Text, Text, Text> {

  /**
   * The key in the Configuration that the options for this task are associated
   * with
   */
  public static String RANDOMIZED_DATA_CHUNK_MAP_TASK_OPTIONS =
    "*weka.distributed.randomized_data_chunks_map_task_opts";

  /** Helper for parsing CSV */
  protected CSVToARFFHeaderMapTask m_rowHelper = null;

  /** The header of the training data */
  protected Instances m_trainingHeader;

  /** The number of chunks to generate */
  protected int m_numChunks;

  /** For randomly shuffling the data */
  protected Random m_random;

  /** Output key values */
  protected Text m_outKey = new Text();

  /** Output values */
  protected Text m_outValue = new Text();

  @Override
  public void setup(Context context) throws IOException {
    m_rowHelper = new CSVToARFFHeaderMapTask();

    Configuration conf = context.getConfiguration();
    String taskOptsS = conf.get(RANDOMIZED_DATA_CHUNK_MAP_TASK_OPTIONS);
    String numChunksS =
      conf.get(RandomizedDataChunkHadoopReducer.NUM_DATA_CHUNKS);
    String csvOptsS =
      conf.get(CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS);

    try {
      if (!DistributedJobConfig.isEmpty(csvOptsS)) {
        String[] csvOpts = Utils.splitOptions(csvOptsS);
        m_rowHelper.setOptions(csvOpts);
      }
      // m_rowHelper.initParserOnly();

      if (!DistributedJobConfig.isEmpty(taskOptsS)) {
        String[] taskOpts = Utils.splitOptions(taskOptsS);

        // name of the training ARFF header file
        String arffHeaderFileName = Utils.getOption("arff-header", taskOpts);
        if (DistributedJobConfig.isEmpty(arffHeaderFileName)) {
          throw new IOException(
            "Can't continue without the name of the ARFF header file!");
        }
        m_trainingHeader =
          CSVToARFFHeaderReduceTask.stripSummaryAtts(WekaClassifierHadoopMapper
            .loadTrainingHeader(arffHeaderFileName));

        WekaClassifierHadoopMapper.setClassIndex(taskOpts, m_trainingHeader,
          !Utils.getFlag("dont-default-class-to-last", taskOpts));

        m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
          .instanceHeaderToAttributeNameList(m_trainingHeader));

        if (DistributedJobConfig.isEmpty(numChunksS)) {
          throw new Exception("Can't continue because the number of "
            + "data chunks is not defined!");
        }

        try {
          m_numChunks = Integer.parseInt(numChunksS);
        } catch (NumberFormatException ex) {
          throw new Exception(ex);
        }

        String seedS = Utils.getOption("seed", taskOpts);
        if (!DistributedJobConfig.isEmpty(seedS)) {
          m_random = new Random(Long.parseLong(seedS));
        } else {
          m_random = new Random(1L);
        }

        // throw away the first few random numbers because
        // they are less random
        for (int i = 0; i < 20; i++) {
          m_random.nextInt();
        }
      } else {
        throw new IOException(
          "Can't continue without the name of the ARFF header file!");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void map(LongWritable key, Text value, Context context)
    throws IOException {

    if (value != null) {
      processRow(value.toString(), context);
    }
  }

  protected double getClassValueIndexFromRow(String[] parsed)
    throws DistributedWekaException {
    int classIndex = m_trainingHeader.classIndex();

    if (classIndex < 0) {
      throw new DistributedWekaException("No class index is set!");
    }

    String classVal = parsed[classIndex].trim();
    double classValIndex = Utils.missingValue();
    if (!DistributedJobConfig.isEmpty(classVal.trim())
      && !classVal.equals(m_rowHelper.getMissingValue())) {

      classValIndex = m_trainingHeader.classAttribute().indexOfValue(classVal);
      if (classValIndex < 0) {
        // try the default vals in row helper
        String defaultClassValue = m_rowHelper.getDefaultValue(classIndex);

        if (defaultClassValue == null) {
          throw new DistributedWekaException(
            "Class value '"
              + classVal
              + "' does not seem to be defined in the header and there is no default value to use!");
        }

        classValIndex =
          m_trainingHeader.classAttribute().indexOfValue(defaultClassValue);
      }
    }

    return classValIndex;
  }

  /**
   * Process a row of data
   * 
   * @param row the row to process
   * @param context the context of the job
   * @throws IOException if a problem occurs
   */
  protected void processRow(String row, Context context) throws IOException {
    if (row != null) {
      // scatter the instances randomly over the chunks
      int chunkNum = m_random.nextInt(m_numChunks);
      m_outKey.set("chunk" + chunkNum);

      if (m_trainingHeader.classIndex() < 0
        || m_trainingHeader.classAttribute().isNumeric()) {
        // no parsing necessary
        m_outValue.set(row);
      } else {
        // class is nominal
        String[] parsed = m_rowHelper.parseRowOnly(row);

        if (parsed.length != m_trainingHeader.numAttributes()) {
          throw new IOException(
            "Parsed a row that contains a different number of values than "
              + "there are attributes in the training ARFF header: " + row);
        }

        try {
          double classValIndex = getClassValueIndexFromRow(parsed);
          if (!Utils.isMissingValue(classValIndex)) {
            m_outValue.set(row + "@:@" + (int) classValIndex);
          } else {
            // don't output if the class is missing
            // System.err.println("Missing class value in row: " + row);
            return;
          }
        } catch (Exception ex) {
          throw new IOException(ex);
        }
      }

      try {
        context.write(m_outKey, m_outValue);
      } catch (InterruptedException ex) {
        throw new IOException(ex);
      }
    }
  }
}
