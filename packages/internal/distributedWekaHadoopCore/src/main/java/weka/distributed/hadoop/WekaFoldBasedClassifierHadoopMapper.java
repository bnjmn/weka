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
 *    WekaFoldBasedClassifierHadoopMapper
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import distributed.core.DistributedJobConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.WekaClassifierMapTask;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;

import java.io.IOException;

/**
 * Mapper implementation for the model-building phase of the evaluation job.
 * Subclasses WekaClassifierHadoopMapper in order to build multiple models - one
 * for each fold.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaFoldBasedClassifierHadoopMapper extends
  WekaClassifierHadoopMapper {

  /**
   * The underlying general Weka classifier map tasks to build the fold models
   * with
   */
  protected WekaClassifierMapTask[] m_tasks;

  /** The total number of folds - default is 1 (use all the data) */
  protected int m_totalFolds = 1;

  /** True if the classifier is incremental */
  protected boolean m_isUpdateableClassifier;

  /**
   * True if the classifier is incremental but we will force it to be batch
   * trained
   */
  protected boolean m_forceBatchForUpdateable;

  @Override
  public void setup(Context context) throws IOException {
    m_rowHelper = new CSVToARFFHeaderMapTask();

    Configuration conf = context.getConfiguration();
    String taskOptsS = conf.get(CLASSIFIER_MAP_TASK_OPTIONS);
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
        m_trainingHeader = CSVToARFFHeaderReduceTask
          .stripSummaryAtts(loadTrainingHeader(arffHeaderFileName));

        setClassIndex(taskOpts, m_trainingHeader, true);

        m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
          .instanceHeaderToAttributeNameList(m_trainingHeader));

        String totalFolds = Utils.getOption("total-folds", taskOpts);
        if (!DistributedJobConfig.isEmpty(totalFolds)) {
          m_totalFolds = Integer.parseInt(totalFolds);
        }
        // just consume the fold-number option (if its been set for some reason)
        // so that it doesn't get
        // passed into setOptions() on the task. We will set the fold
        // number on the tasks ourself
        Utils.getOption("fold-number", taskOpts);

        // multiple iterations over the training data for an updateable
        // classifier?
        boolean continueTrainingUpdateable = Utils.getFlag(
          "continue-training-updateable", taskOpts);

        // Preconstructed filter to use?
        String preconstructedFilter = Utils.getOption("preconstructed-filter",
          taskOpts);
        Filter preConstructedF = null;
        if (!DistributedJobConfig.isEmpty(preconstructedFilter)) {
          // try and load this from the distributed cache
          preConstructedF = loadPreconstructedFilter(preconstructedFilter);
        }

        // pass on the number of maps to the task
        Environment env = new Environment();
        int totalNumMaps = conf.getInt("mapred.map.tasks", 1);
        env.addVariable(WekaClassifierMapTask.TOTAL_NUMBER_OF_MAPS, ""
          + totalNumMaps);

        // copy the task options
        String[] taskOptsCopy = taskOpts.clone();

        // Make a temporary task so we can determine if the classifier is
        // updateable
        WekaClassifierMapTask tempTask = new WekaClassifierMapTask();
        tempTask.setOptions(taskOptsCopy.clone());
        m_isUpdateableClassifier =
          (tempTask.getClassifier() instanceof UpdateableClassifier);
        m_forceBatchForUpdateable = tempTask
          .getForceBatchLearningForUpdateableClassifiers();

        // tasks to work with
        m_tasks = new WekaClassifierMapTask[m_totalFolds];
        for (int i = 0; i < m_totalFolds; i++) {
          m_tasks[i] = new WekaClassifierMapTask();
          // set environment first, before setOptions so that
          // when setClassifier() is called in the map task
          // it has access to the number of mappers that will
          // run. This allows the number of base models per
          // mapper to be set appropriately for
          // IteratedSingleClassifierEnhancers
          m_tasks[i].setEnvironment(env);
          m_tasks[i].setOptions(taskOptsCopy.clone());
          m_tasks[i].setFoldNumber(i + 1);
          m_tasks[i].setTotalNumFolds(m_totalFolds);
          m_tasks[i]
            .setContinueTrainingUpdateableClassifier(continueTrainingUpdateable);

          if (preConstructedF != null) {
            m_tasks[i]
              .addPreconstructedFilterToUse((PreconstructedFilter) preConstructedF);
          }
        }

        if (continueTrainingUpdateable) {
          String classifierFileName = Utils.getOption("model-file-name",
            taskOpts);
          if (DistributedJobConfig.isEmpty(classifierFileName)) {
            throw new IOException(
              "Continued training of incremental classifier "
                + "has been specified but no file name for the classifier "
                + "to load has been specified. Can't continue.");
          }

          for (int i = 0; i < m_totalFolds; i++) {
            String foldClassifierName = "" + (i + 1) + "_" + classifierFileName;
            Classifier toUse = loadClassifier(foldClassifierName);

            // System.err
            // .println("Initial classifier for continued training on fold "
            // + (i + 1) + ":\n" + toUse.toString());

            if (!(toUse instanceof UpdateableClassifier)) {
              throw new Exception(
                "The classifier loaded is not an UpdateableClassifier (incremental)");
            }

            m_tasks[i].setClassifier(toUse);
          }
        }

        // initialize
        for (int i = 0; i < m_totalFolds; i++) {
          m_tasks[i].setup(m_trainingHeader);
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
  protected void processRow(String row) throws IOException {
    if (row != null) {
      String[] parsed = m_rowHelper.parseRowOnly(row);

      if (parsed.length != m_trainingHeader.numAttributes()) {
        throw new IOException(
          "Parsed a row that contains a different number of values than "
            + "there are attributes in the training ARFF header: " + row);
      }
      try {
        Instance toProcess = makeInstance(m_rowHelper, m_trainingHeader,
          m_isUpdateableClassifier, m_forceBatchForUpdateable, parsed);
        for (int i = 0; i < m_totalFolds; i++) {
          m_tasks[i].processInstance(toProcess);
        }
      } catch (Exception ex) {
        throw new IOException(ex);
      }
    }
  }

  @Override
  public void cleanup(Context context) throws IOException, InterruptedException {
    try {
      for (int i = 0; i < m_totalFolds; i++) {
        m_tasks[i].finalizeTask();

        // System.err.println("Model after continued training on fold " + (i +
        // 1)
        // + ":\n" + m_tasks[i].getClassifier().toString());

        byte[] bytes = classifierToBytes(m_tasks[i].getClassifier(),
          m_tasks[i].getNumTrainingInstances());

        String constantKey = "classifier_fold_" + (i + 1);

        Text key = new Text();
        key.set(constantKey);
        BytesWritable value = new BytesWritable();
        value.set(bytes, 0, bytes.length);

        context.write(key, value);

        // save memory
        m_tasks[i] = null;
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }
}
