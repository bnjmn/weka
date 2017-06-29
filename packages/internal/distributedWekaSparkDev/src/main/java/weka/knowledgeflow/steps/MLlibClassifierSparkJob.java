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
 *    MLlibClassifierSparkJob
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import org.apache.log4j.WriterAppender;
import org.apache.spark.api.java.JavaSparkContext;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.core.WekaException;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.distributed.spark.Dataset;
import weka.classifiers.mllib.MLlibClassifier;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static weka.knowledgeflow.steps.AbstractSparkJob.CON_FAILURE;
import static weka.knowledgeflow.steps.AbstractSparkJob.CON_SUCCESS;

@KFStep(name = "MLLibClassifierSparkJob", category = "Spark",
  toolTipText = "Builds an MLlib classifier/regressor in Spark.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "WekaClassifierSparkJob.gif")
public class MLlibClassifierSparkJob extends BaseStep {

  private static final long serialVersionUID = -3880480775968942957L;

  /**
   * Holds a reference to the log appender used. The first step in a flow will
   * get this and pass it on downstream to any subsequent jobs so that the last
   * job can remove the appender after finishing execution
   */
  protected transient WriterAppender m_sparkLogAppender;

  /**
   * Holds a reference to the current context. The first step in a flow will get
   * this and pass it downstream to any subsequent jobs
   */
  protected transient JavaSparkContext m_currentContext;

  /**
   * Holds a reference to the dataset(s) in play (if any). This will allow
   * costly ops (such as random shuffling to be computed just once)
   */
  protected transient List<Map.Entry<String, Dataset>> m_currentDatasets;

  /** The caching strategy in play */
  protected CachingStrategy m_cachingStrategy;

  /** The output directory in play */
  protected String m_outputDirectory;

  /**
   * Flag set if this step has no outgoing connections
   */
  protected boolean m_last;

  /** template for option setting */
  protected weka.distributed.spark.MLlibClassifierSparkJob m_templateJob =
    new weka.distributed.spark.MLlibClassifierSparkJob();

  /** The actual job that will run */
  protected weka.distributed.spark.MLlibClassifierSparkJob m_runningJob;

  @OptionMetadata(displayName = "MLlib classifier",
    description = "The MLlib classifier/regressor to use", displayOrder = 1)
  public void setClassifier(MLlibClassifier classifier) {
    m_templateJob.setClassifier(classifier);
  }

  public MLlibClassifier getClassifier() {
    return (MLlibClassifier) m_templateJob.getClassifier();
  }

  /**
   * Get the name or index of the class attribute ("first" and "last" can also
   * be used)
   *
   * @return the name or index of the class attribute
   */
  public String getClassAttribute() {
    return m_templateJob.getClassAttribute();
  }

  /**
   * Set the name or index of the class attribute ("first" and "last" can also
   * be used)
   *
   * @param c the name or index of the class attribute
   */
  @OptionMetadata(displayName = "Class attribute/index", description = "The "
    + "name or 1-based index of the class attribute", displayOrder = 2)
  public void setClassAttribute(String c) {
    m_templateJob.setClassAttribute(c);
  }

  /**
   * Get the name only for the model file
   *
   * @return the name only (not full path) that the model should be saved to
   */
  public String getModelFileName() {
    return m_templateJob.getModelFileName();
  }

  /**
   * Set the name only for the model file
   *
   * @param m the name only (not full path) that the model should be saved to
   */
  @OptionMetadata(
    displayName = "Name for the serialized model file",
    description = "The name of the file to write the final model to - this will "
      + "be written to the output directory", displayOrder = 3)
  public
    void setModelFileName(String m) {
    m_templateJob.setModelFileName(m);
  }

  public void setLast(boolean last) {
    m_last = last;
  }

  @Override
  public void start() throws WekaException {
    throw new WekaException("This step cannot operate as a start point");
  }

  @Override
  public void stepInit() throws WekaException {
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    m_sparkLogAppender = null;

    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());
      m_runningJob = new weka.distributed.spark.MLlibClassifierSparkJob();
      m_runningJob.setStatusMessagePrefix(((StepManagerImpl) getStepManager())
        .stepStatusMessagePrefix());
      m_runningJob.setEnvironment(getStepManager().getExecutionEnvironment()
        .getEnvironmentVariables());
      m_runningJob.setLog(getStepManager().getLog());
      // m_runningJob.setClassifier(m_classifier);
      m_runningJob.setOptions(m_templateJob.getOptions());
    } catch (Exception ex) {
      throw new WekaException(ex);
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    if (getStepManager().numOutgoingConnectionsOfType("success") == 0
      && getStepManager().numOutgoingConnectionsOfType("failure") == 0) {
      m_last = true;
    }
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<>();

    if (getStepManager().numIncomingConnections() == 0) {
      result.add(CON_SUCCESS);
      result.add(CON_FAILURE);
    }

    return result;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<>();
    if (getStepManager().numIncomingConnectionsOfType(CON_SUCCESS) > 0
      || getStepManager().numIncomingConnectionsOfType(CON_FAILURE) > 0) {
      result.add(CON_SUCCESS);
      result.add(CON_FAILURE);
    }
    if (getStepManager().numIncomingConnections() > 0) {
      result.add(StepManager.CON_BATCH_CLASSIFIER);
      result.add(StepManager.CON_TEXT);
    }
    return result;
  }

  protected void runJob() throws WekaException {
    getStepManager().processing();
    // boolean hardFailure = false;
    ClassLoader orig = Thread.currentThread().getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());
      if (m_currentContext == null) {
        throw new DistributedWekaException("No spark context available!");
      }

      // pass any datasets in play on to avoid re-laoding re-randomizing etc.
      if (m_currentDatasets != null) {
        for (Map.Entry<String, Dataset> e : m_currentDatasets) {
          m_runningJob.setDataset(e.getKey(), e.getValue());
        }
      }

      if (m_cachingStrategy != null) {
        getStepManager().logDebug(
          "Setting caching strategy: " + m_cachingStrategy);
        m_runningJob.setCachingStrategy(m_cachingStrategy);
      }

      if (m_outputDirectory != null && m_outputDirectory.length() > 0) {
        m_runningJob.getSparkJobConfig().setOutputDir(m_outputDirectory);
      }

      getStepManager().logBasic("Executing Spark job");
      getStepManager().statusMessage("Executing...");

      if (!m_runningJob.runJobWithContext(m_currentContext)) {
        getStepManager()
          .logError(
            "Job failed "
              + (getStepManager().numOutgoingConnectionsOfType(CON_FAILURE) > 0 ? "- invoking failure connection"
                : ""), null);
        outputFailureData("ERROR: job failed - check logs",
          m_runningJob.getDatasetIterator());
      } else {
        getStepManager()
          .logBasic(
            "Job finished successfully "
              + (getStepManager().numOutgoingConnectionsOfType(CON_SUCCESS) > 0 ? "- invoking "
                + "success connection"
                : ""));

        notifyJobOutputConnections();

        // grab the caching strategy again (just in case the job has
        // altered it for some reason)
        m_cachingStrategy = m_runningJob.getCachingStrategy();
        outputSuccessData(m_runningJob.getDatasetIterator());
      }

      getStepManager().finished();
    } catch (Exception ex) {
      // hardFailure = true;
      throw new WekaException(ex);
    } finally {
      /* // shutdown the context if we are the last step in the chain
      // (i.e. no outgoing success or failure connections), or we've
      // had an exception
      if (m_last) {
        getStepManager().logBasic("Last step - shutting down Spark context");
      } else if (hardFailure) {
        getStepManager().logLow("Shutting down Spark context due to error");
      }

      if (hardFailure
        || (m_last
          && getStepManager().numOutgoingConnectionsOfType(CON_SUCCESS) == 0 && getStepManager()
          .numOutgoingConnectionsOfType(CON_FAILURE) == 0)) {
        m_runningJob.shutdownJob(m_sparkLogAppender);
      } */
      Thread.currentThread().setContextClassLoader(orig);
      m_runningJob = null;
    }

    getStepManager().finished();
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    m_currentContext =
      data.getPayloadElement(AbstractDataSource.PAYLOAD_CONTEXT);
    m_sparkLogAppender =
      data.getPayloadElement(AbstractDataSource.PAYLOAD_APPENDER);
    m_currentDatasets =
      data.getPayloadElement(AbstractDataSource.PAYLOAD_DATASETS);
    m_outputDirectory =
      data.getPayloadElement(AbstractDataSource.PAYLOAD_OUTPUT_DIRECTORY);
    m_cachingStrategy =
      data.getPayloadElement(AbstractDataSource.PAYLOAD_CACHING_STRATEGY);

    runJob();
  }

  protected void outputFailureData(String reason,
    Iterator<Map.Entry<String, Dataset>> datasetIterator) throws WekaException {

    List<Map.Entry<String, Dataset>> datasets =
      datasetIterator.hasNext() ? new ArrayList<Map.Entry<String, Dataset>>()
        : null;

    if (datasets != null) {
      while (datasetIterator.hasNext()) {
        datasets.add(datasetIterator.next());
      }
    }

    Data failure = new Data(CON_FAILURE, reason);
    if (m_sparkLogAppender != null) {
      failure.setPayloadElement(AbstractDataSource.PAYLOAD_APPENDER,
        m_sparkLogAppender);
    }
    if (m_currentContext != null) {
      failure.setPayloadElement(AbstractDataSource.PAYLOAD_CONTEXT,
        m_currentContext);
    }
    if (m_cachingStrategy != null) {
      failure.setPayloadElement(AbstractDataSource.PAYLOAD_CACHING_STRATEGY,
        m_cachingStrategy);
    }
    if (datasets != null && datasets.size() > 0) {
      failure.setPayloadElement(AbstractDataSource.PAYLOAD_DATASETS, datasets);
    }
    if (m_outputDirectory != null && m_outputDirectory.length() > 0) {
      failure.setPayloadElement(AbstractDataSource.PAYLOAD_OUTPUT_DIRECTORY,
        m_outputDirectory);
    }
    getStepManager().outputData(failure);
  }

  protected void outputSuccessData(
    Iterator<Map.Entry<String, Dataset>> datasetIterator) throws WekaException {
    List<Map.Entry<String, Dataset>> datasets =
      datasetIterator.hasNext() ? new ArrayList<Map.Entry<String, Dataset>>()
        : null;

    if (datasets != null) {
      while (datasetIterator.hasNext()) {
        datasets.add(datasetIterator.next());
      }
    }

    Data success = new Data(CON_SUCCESS);
    if (m_sparkLogAppender != null) {
      success.setPayloadElement(AbstractDataSource.PAYLOAD_APPENDER,
        m_sparkLogAppender);
    }
    if (m_currentContext != null) {
      success.setPayloadElement(AbstractDataSource.PAYLOAD_CONTEXT,
        m_currentContext);
    }
    if (m_cachingStrategy != null) {
      success.setPayloadElement(AbstractDataSource.PAYLOAD_CACHING_STRATEGY,
        m_cachingStrategy);
    }
    if (datasets != null && datasets.size() > 0) {
      success.setPayloadElement(AbstractDataSource.PAYLOAD_DATASETS, datasets);
    }
    if (m_outputDirectory != null && m_outputDirectory.length() > 0) {
      success.setPayloadElement(AbstractDataSource.PAYLOAD_OUTPUT_DIRECTORY,
        m_outputDirectory);
    }
    getStepManager().outputData(success);
  }

  public void notifyJobOutputConnections() throws WekaException {
    Instances trainingHeader = m_runningJob.getTrainingHeader();
    weka.classifiers.Classifier model = m_runningJob.getClassifier();
    String classAtt = m_runningJob.getClassAttribute();

    try {
      weka.distributed.spark.WekaClassifierSparkJob.setClassIndex(classAtt,
        trainingHeader, true);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    if (model == null) {
      getStepManager().logWarning("No classifier produced!");
    }

    if (trainingHeader == null) {
      getStepManager()
        .logWarning("No training header available for the model!");
    }

    if (model != null) {
      if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_TEXT) > 0) {
        String textual = model.toString();

        String title = "Spark: ";
        String classifierSpec = model.getClass().getName();
        if (model instanceof OptionHandler) {
          classifierSpec +=
            " " + Utils.joinOptions(((OptionHandler) model).getOptions());
        }
        title += classifierSpec;

        Data textData = new Data(StepManager.CON_TEXT, textual);
        textData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE, title);
        getStepManager().outputData(textData);
      }
    }

    if (trainingHeader != null) {
      Data headerData = new Data(StepManager.CON_DATASET, trainingHeader);
      headerData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
      headerData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
      getStepManager().outputData(headerData);
    }
  }

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.HiddenPropertyMLlibJobStepEditorDialog";
  }
}
