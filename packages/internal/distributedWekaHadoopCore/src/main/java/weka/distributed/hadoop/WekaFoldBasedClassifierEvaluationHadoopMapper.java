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
 *    WekaFoldBasedClassifierEvaluationHadoopMapper
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.evaluation.AggregateableEvaluation;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Attribute;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.WekaClassifierEvaluationMapTask;
import weka.distributed.WekaClassifierMapTask;
import distributed.core.DistributedJobConfig;

/**
 * Hadoop mapper implementation for the evaluation job. Loads one or more
 * classifiers from the distributed cache. Incoming data is used for evaluation
 * as is if the total number of folds = 1; otherwise the correct test fold is
 * created by the underlying WekaClassifierEvaluationMapTask for each of the
 * folds to be evaluated.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaFoldBasedClassifierEvaluationHadoopMapper extends
  Mapper<LongWritable, Text, Text, BytesWritable> {

  // loads one or more classifiers (if total folds > 1 then names are prefixed
  // by
  // fold#_); otherwise the original output model file name is loaded.
  // Incoming data is used for evaluation as is if total folds = 1; otherwise
  // correct test fold is created by the underlying
  // WekaClassiiferEvaluationMapTask

  /** Used for CSV parsing */
  protected CSVToARFFHeaderMapTask m_rowHelper;

  /** The header of the data being processed */
  protected Instances m_trainingHeader;

  /** Underlying general Weka map tasks - one for each fold */
  protected WekaClassifierEvaluationMapTask[] m_tasks;

  /** Total number of folds - default use all the data */
  protected int m_totalFolds = 1;

  /** Holds the original model file name */
  protected String m_originalModelFileName = "";

  /** True if batch learning has been forced for incremental learners */
  protected boolean m_forceBatch = false;

  /** True if the classifier was incremental */
  protected boolean m_classifierIsUpdateable = false;

  /** Environment variables */
  protected Environment m_env = Environment.getSystemWide();

  @Override
  public void setup(Context context) throws IOException {
    m_rowHelper = new CSVToARFFHeaderMapTask();

    Configuration conf = context.getConfiguration();
    String taskOptsS = conf
      .get(WekaClassifierHadoopMapper.CLASSIFIER_MAP_TASK_OPTIONS);
    String csvOptsS = conf
      .get(CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS);

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
        Instances headerWithSummaryAtts = WekaClassifierHadoopMapper
          .loadTrainingHeader(arffHeaderFileName);
        m_trainingHeader = CSVToARFFHeaderReduceTask
          .stripSummaryAtts(headerWithSummaryAtts);

        WekaClassifierHadoopMapper.setClassIndex(taskOpts, m_trainingHeader,
          true);

        m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
          .instanceHeaderToAttributeNameList(m_trainingHeader));

        Attribute classAtt = m_trainingHeader.classAttribute();
        // now look for the summary stats att with this name so that we can set
        // up priors for evaluation properly. NOTE, that the relative evaluation
        // measures will probably be slightly pessimistic due to the fact that
        // we are using priors computed on the entire data set here, rather than
        // computed from each training fold.
        String classAttSummaryName = CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
          + classAtt.name();
        Attribute summaryClassAtt = headerWithSummaryAtts
          .attribute(classAttSummaryName);

        if (summaryClassAtt == null) {
          throw new Exception(
            "WekaClassifierEvaluationHadoopReducer - was unable to find "
              + "the summary meta data attribute for the class attribute in the header");
        }

        double priorsCount = 0;
        double[] priors = new double[classAtt.isNominal() ? classAtt
          .numValues() : 1];
        if (classAtt.isNominal()) {
          for (int i = 0; i < classAtt.numValues(); i++) {
            String label = classAtt.value(i);
            String labelWithCount = summaryClassAtt.value(i)
              .replace(label + "_", "").trim();

            try {
              priors[i] = Double.parseDouble(labelWithCount);
            } catch (NumberFormatException n) {
              throw new Exception(n);
            }
          }

          priorsCount = classAtt.numValues();
        } else {

          double count = ArffSummaryNumericMetric.COUNT
            .valueFromAttribute(summaryClassAtt);
          double sum = ArffSummaryNumericMetric.SUM
            .valueFromAttribute(summaryClassAtt);

          priors[0] = sum;
          priorsCount = count;
        }

        // now grab fold-related opts to pass to the underlying task and
        // sort out loading of the classifiers
        String totalFoldsS = Utils.getOption("total-folds", taskOpts);
        m_totalFolds = 1; // default - use all the data
        m_totalFolds = Integer.parseInt(totalFoldsS);
        m_forceBatch = Utils.getFlag("force-batch", taskOpts);
        String sSeed = Utils.getOption("seed", taskOpts);
        long seed = 1L; // default

        if (!DistributedJobConfig.isEmpty(sSeed)) {
          try {
            sSeed = m_env.substitute(sSeed);
          } catch (Exception ex) {
          }

          try {
            seed = Long.parseLong(sSeed);
          } catch (NumberFormatException ex) {
          }
        }

        // an sample size > 0 indicates that we will be retaining
        // predictions in order to compute auc/auprc
        String predFracS = Utils.getOption("auc", taskOpts);
        double predFrac = 0;
        if (!DistributedJobConfig.isEmpty(predFracS)) {
          try {
            predFrac = Double.parseDouble(predFracS);
          } catch (NumberFormatException ex) {
            System.err
              .println("Unable to parse the fraction of predictions to retain: "
                + predFracS);
          }
        }

        m_originalModelFileName = Utils.getOption("model-file-name", taskOpts);
        if (DistributedJobConfig.isEmpty(m_originalModelFileName)) {
          throw new IOException(
            "Fold-based evaluation con't proceed as no model "
              + "filename to load has been specified");
        }

        // determine if the underlying classifier was updateable or
        // not
        WekaClassifierMapTask temp = new WekaClassifierMapTask();
        temp.setOptions(taskOpts.clone());
        Classifier tempClassifier = temp.getClassifier();
        m_classifierIsUpdateable = (tempClassifier instanceof UpdateableClassifier);

        m_tasks = new WekaClassifierEvaluationMapTask[m_totalFolds];
        for (int i = 0; i < m_totalFolds; i++) {
          m_tasks[i] = new WekaClassifierEvaluationMapTask();

          // If the classifier is updateable and was not batch trained
          // then we have to load them all apriori so that the incremental
          // fold selection can operate. Otherwise we will defer loading
          // until the cleanup() phase where we can evaluate them one
          // at a time and save memory
          if (!m_forceBatch && m_classifierIsUpdateable) {
            String modelToLoad = "" + (i + 1) + "_" + m_originalModelFileName;
            Classifier foldModel = WekaClassifierHadoopMapper
              .loadClassifier(modelToLoad);
            m_tasks[i].setClassifier(foldModel);
          }
          m_tasks[i].setTotalNumFolds(m_totalFolds);
          m_tasks[i].setFoldNumber(i + 1);
          m_tasks[i].setBatchTrainedIncremental(m_forceBatch);
          m_tasks[i].setup(m_trainingHeader, priors, priorsCount, seed,
            predFrac);
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
      String row = value.toString();
      String[] parsed = m_rowHelper.parseRowOnly(row);

      if (parsed.length != m_trainingHeader.numAttributes()) {
        throw new IOException(
          "Parsed a row that contains a different number of values than "
            + "there are attributes in the training ARFF header: " + row);
      }

      try {
        Instance toProcess = WekaClassifierHadoopMapper.makeInstance(
          m_rowHelper, m_trainingHeader, m_classifierIsUpdateable,
          m_forceBatch, parsed);

        for (int i = 0; i < m_totalFolds; i++) {
          m_tasks[i].processInstance(toProcess);
        }
      } catch (Exception ex) {
        throw new IOException(ex);
      }
    }
  }

  /**
   * Serializes and compresses an Evaluation object to an array of bytes
   * 
   * @param eval the Evaluation object to serialize
   * @return an array of bytes
   * @throws IOException if a problem occurs
   */
  protected static byte[] evalToBytes(Evaluation eval) throws IOException {
    ObjectOutputStream p = null;
    byte[] bytes = null;

    try {
      ByteArrayOutputStream ostream = new ByteArrayOutputStream();
      OutputStream os = ostream;

      p = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(
        os)));
      p.writeObject(eval);
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
  public void cleanup(Context context) throws IOException {
    try {
      // aggregate the stats over all folds in this chunk
      AggregateableEvaluation agg = null;
      for (int i = 0; i < m_totalFolds; i++) {
        if (!m_classifierIsUpdateable || m_forceBatch) {
          String modelToLoad = "" + (i + 1) + "_" + m_originalModelFileName;
          Classifier foldModel = WekaClassifierHadoopMapper
            .loadClassifier(modelToLoad);
          m_tasks[i].setClassifier(foldModel);
        }

        m_tasks[i].finalizeTask();
        Evaluation eval = m_tasks[i].getEvaluation();

        // save memory
        m_tasks[i] = null;

        if (agg == null) {
          agg = new AggregateableEvaluation(eval);
        }
        agg.aggregate(eval);
      }

      if (agg != null) {
        byte[] bytes = evalToBytes(agg);
        String constantKey = "evaluation";
        Text key = new Text();
        key.set(constantKey);

        BytesWritable value = new BytesWritable();
        value.set(bytes, 0, bytes.length);

        context.write(key, value);
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }
}
