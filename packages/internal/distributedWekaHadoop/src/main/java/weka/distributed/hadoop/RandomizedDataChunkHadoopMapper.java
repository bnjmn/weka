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

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
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
  public static String RANDOMIZED_DATA_CHUNK_MAP_TASK_OPTIONS = "*weka.distributed.randomized_data_chunks_map_task_opts";

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
    String numChunksS = conf
      .get(RandomizedDataChunkHadoopReducer.NUM_DATA_CHUNKS);
    String csvOptsS = conf
      .get(CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS);

    try {
      if (!DistributedJobConfig.isEmpty(csvOptsS)) {
        String[] csvOpts = Utils.splitOptions(csvOptsS);
        m_rowHelper.setOptions(csvOpts);
      }
      m_rowHelper.initParserOnly();

      if (!DistributedJobConfig.isEmpty(taskOptsS)) {
        String[] taskOpts = Utils.splitOptions(taskOptsS);

        // name of the training ARFF header file
        String arffHeaderFileName = Utils.getOption("arff-header", taskOpts);
        if (DistributedJobConfig.isEmpty(arffHeaderFileName)) {
          throw new IOException(
            "Can't continue without the name of the ARFF header file!");
        }
        m_trainingHeader = CSVToARFFHeaderReduceTask
          .stripSummaryAtts(WekaClassifierHadoopMapper
            .loadTrainingHeader(arffHeaderFileName));

        WekaClassifierHadoopMapper.setClassIndex(taskOpts, m_trainingHeader,
          true);

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

        // if (!m_trainingHeader.classAttribute().isNominal()) {
        // throw new Exception(
        // "Can only perform stratification if the class is nominal!!");
        // }
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

  protected void processRow(String row, Context context) throws IOException {
    if (row != null) {
      String[] parsed = m_rowHelper.parseRowOnly(row);

      if (parsed.length != m_trainingHeader.numAttributes()) {
        throw new IOException(
          "Parsed a row that contains a different number of values than "
            + "there are attributes in the training ARFF header: " + row);
      }
      try {
        Instance toProcess = WekaClassifierHadoopMapper.makeInstance(
          m_rowHelper, m_trainingHeader, true, false, parsed);

        if (!toProcess.isMissing(toProcess.classIndex())) {

          // key is the class label
          // m_outKey.set(toProcess.stringValue(toProcess.classIndex()));

          // scatter the instances randomly over the chunks
          int chunkNum = m_random.nextInt(m_numChunks);
          m_outKey.set("chunk" + chunkNum);

          if (m_trainingHeader.classAttribute().isNominal()) {

            // append the index of the class value if class is nominal -
            // this gives the reducer quick access to it without having
            // to parse the entire CSV row into an instance again
            m_outValue.set(toProcess.toString() + "@:@"
              + (int) toProcess.classValue());
          } else {
            // The main function here is just to randomize the order
            // of the instances in the chunks
            m_outValue.set(toProcess.toString());
          }

          context.write(m_outKey, m_outValue);
        }
      } catch (Exception ex) {
        throw new IOException(ex);
      }
    }
  }
}
