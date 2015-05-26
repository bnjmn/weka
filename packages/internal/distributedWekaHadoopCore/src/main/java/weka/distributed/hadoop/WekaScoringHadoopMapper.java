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
 *    WekaScoringHadoopMapper
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.WekaScoringMapTask;
import distributed.core.DistributedJobConfig;

/**
 * Mapper implementation for the WekaScoringHadoop job
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaScoringHadoopMapper extends
  Mapper<LongWritable, Text, LongWritable, Text> {

  /**
   * The key in the Configuration that the options for this task are associated
   * with
   */
  public static final String SCORING_MAP_TASK_OPTIONS =
    "*weka.distributed.scoring_map_task_opts";

  /** The underlying general Weka scoring map task */
  protected WekaScoringMapTask m_task = null;

  /** Header of the data */
  protected Instances m_scoringDataHeader;

  /** Helper ARFF header map task for parsing CSV with */
  protected CSVToARFFHeaderMapTask m_rowHelper = null;

  /** The columns to output in the scored data */
  protected Range m_colsToOutput;

  protected int[] m_selectedIndices;

  protected boolean m_rangeInitialized;

  /** apparently it is good practice to re-use these */
  protected Text m_outputText = new Text();

  /** Holds parsed rows for batch prediction */
  protected List<Object> m_parsedBatch = new ArrayList<Object>();

  /**
   * Helper method to load the model to score with from the distributed cache
   * 
   * @param filename the name of the model file
   * @return a list containing the model and the header of the data it was built
   *         with
   * @throws IOException if a problem occurs
   */
  protected static List<Object> loadModel(String filename) throws IOException {
    File f = new File(filename);

    if (!f.exists()) {
      throw new IOException("The classifier model file '" + filename
        + "' does not seem to exist in the distributed cache!");
    }

    ObjectInputStream ois = null;
    List<Object> result = new ArrayList<Object>();
    try {
      ois =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));

      try {
        Object model = ois.readObject();
        result.add(model);
      } catch (ClassNotFoundException ex) {
        throw new IOException(ex);
      }
      Instances header = null;
      try {
        header = (Instances) ois.readObject();
        result.add(header);
      } catch (Exception ex) {
        // we won't complain if there is no header in the stream
      }

      ois.close();
      ois = null;
    } finally {
      if (ois != null) {
        ois.close();
      }
    }

    return result;
  }

  @Override
  public void setup(Context context) throws IOException {

    m_rangeInitialized = false;
    m_task = new WekaScoringMapTask();
    m_rowHelper = new CSVToARFFHeaderMapTask();

    Configuration conf = context.getConfiguration();
    String taskOptsS = conf.get(SCORING_MAP_TASK_OPTIONS);
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

        // load the header for the data and set the class index
        m_scoringDataHeader =
          CSVToARFFHeaderReduceTask.stripSummaryAtts(WekaClassifierHadoopMapper
            .loadTrainingHeader(arffHeaderFileName));

        // WekaClassifierHadoopMapper.setClassIndex(taskOpts,
        // m_scoringDataHeader,
        // true);

        // load the model to use
        String modelFileName = Utils.getOption("model-file-name", taskOpts);

        if (DistributedJobConfig.isEmpty(modelFileName)) {
          throw new IOException("No model file to load has been specified");
        }

        List<Object> loadedModel = loadModel(modelFileName);
        Object model = loadedModel.get(0);
        Instances modelHeader =
          loadedModel.size() == 2 ? (Instances) loadedModel.get(1) : null;

        if (modelHeader == null) {
          throw new IOException(
            "No header included in serialized model file - can't continue.");
        }

        m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
          .instanceHeaderToAttributeNameList(modelHeader));

        m_task.setModel(model, modelHeader, m_scoringDataHeader);

        String attRange = Utils.getOption("columns-to-output", taskOpts);
        if (!DistributedJobConfig.isEmpty(attRange)
          && !attRange.equals("first-last")) {
          m_colsToOutput = new Range();
          m_colsToOutput.setRanges(attRange);

          m_colsToOutput.setUpper(m_scoringDataHeader.numAttributes() - 1);
          m_selectedIndices = m_colsToOutput.getSelection();
        }
      } else {
        throw new IOException(
          "Can't continue without access to the ARFF header file");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Concatenates an input row and the predictions produced for it by the
   * classifier
   * 
   * @param parsed the parsed CSV input values
   * @param row the row as a string (used for speed when we are outputting all
   *          the input columns rather than a selection)
   * @param preds the predictions from the classifier
   * @param predictionLabels the labels of the class values (if the class is
   *          nominal)
   * @return an input row plus predictions
   */
  protected String concatenateRowAndPreds(String[] parsed, String row,
    double[] preds, List<String> predictionLabels) {
    StringBuilder b = new StringBuilder();

    if (m_colsToOutput == null) {
      b.append(row);
    } else {
      boolean first = true;
      for (int i = 0; i < m_selectedIndices.length; i++) {
        if (!first) {
          b.append(",");
        } else {
          first = false;
        }

        if (!DistributedJobConfig.isEmpty(parsed[m_selectedIndices[i]])) {
          b.append(parsed[m_selectedIndices[i]]);
        }
      }
    }

    for (int i = 0; i < preds.length; i++) {

      b.append(",")
        .append(predictionLabels != null ? predictionLabels.get(i) + ":" : "")
        .append("" + preds[i]);

    }

    return b.toString();
  }

  @Override
  public void map(LongWritable key, Text value, Context context)
    throws IOException {
    String row = value.toString();

    if (row != null) {
      String[] parsed = m_rowHelper.parseRowOnly(row);

      if (parsed.length != m_scoringDataHeader.numAttributes()) {
        throw new IOException(
          "Parsed a row that contains a different number of values than "
            + "there are attributes in the ARFF header for the incoming data: "
            + row);
      }

      try {
        // specify true for the incremental classifier argument and
        // false for the force batch learning. This ensures that the
        // values of String attributes do not accumulate in memory.
        Instance toProcess =
          WekaClassifierHadoopMapper.makeInstance(m_rowHelper,
            m_scoringDataHeader, true, false, parsed);

        if (m_task.isBatchPredictor() && !m_task.modelIsUsingStringAttributes()) {
          if (m_colsToOutput == null) {
            m_parsedBatch.add(row);
          } else {
            m_parsedBatch.add(parsed);
          }

          double[][] preds = m_task.processInstanceBatchPredictor(toProcess);
          if (preds != null) {
            // output the batch
            List<String> labels = m_task.getPredictionLabels();
            for (int i = 0; i < preds.length; i++) {
              String tempRow =
                m_colsToOutput != null ? null : m_parsedBatch.get(i).toString();
              String[] tempParsedRow =
                tempRow == null ? (String[]) m_parsedBatch.get(i) : null;
              String newVal =
                concatenateRowAndPreds(tempParsedRow, tempRow, preds[i], labels);

              m_outputText.set(newVal);

              // don't need the key
              context.write(null, m_outputText);
            }

            m_parsedBatch.clear();
          }
        } else {
          double[] preds = m_task.processInstance(toProcess);
          List<String> labels = m_task.getPredictionLabels();
          String newVal = concatenateRowAndPreds(parsed, row, preds, labels);

          // Text nv = new Text();
          // nv.set(newVal);
          m_outputText.set(newVal);

          // don't need the key
          context.write(null, m_outputText);
        }
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
  }

  @Override
  public void cleanup(Context context) throws IOException {

    if (m_task.isBatchPredictor()) {
      try {
        double[][] preds = m_task.finalizeBatchPrediction();
        if (preds != null) {
          List<String> labels = m_task.getPredictionLabels();
          for (int i = 0; i < preds.length; i++) {
            String tempRow =
              m_colsToOutput != null ? null : m_parsedBatch.get(i).toString();
            String[] tempParsedRow =
              tempRow == null ? (String[]) m_parsedBatch.get(i) : null;
            String newVal =
              concatenateRowAndPreds(tempParsedRow, tempRow, preds[i], labels);

            m_outputText.set(newVal);

            // don't need the key
            context.write(null, m_outputText);
          }

          m_parsedBatch.clear();
        }

      } catch (Exception ex) {
        throw new IOException(ex);
      }
    }

    String missingMismatch = m_task.getMissingMismatchAttributeInfo();

    if (!DistributedJobConfig.isEmpty(missingMismatch)) {
      System.err
        .println("There were some missing or type mismatches for the "
          + "attributes that the model was expecting to be in the incoming data:\n\n");
      System.err.println(missingMismatch);
    }
  }
}
