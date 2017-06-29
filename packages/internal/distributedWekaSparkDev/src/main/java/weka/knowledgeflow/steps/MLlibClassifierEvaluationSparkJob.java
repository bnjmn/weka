package weka.knowledgeflow.steps;

import static weka.knowledgeflow.steps.AbstractSparkJob.CON_FAILURE;
import static weka.knowledgeflow.steps.AbstractSparkJob.CON_SUCCESS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.WriterAppender;
import org.apache.spark.api.java.JavaSparkContext;

import weka.classifiers.mllib.MLlibClassifier;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.core.WekaException;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.distributed.spark.Dataset;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

/**
 * A Knowledge Flow step for the MLlib classifier evaluation Spark job
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "MLLibClassifierEvaluationSparkJob", category = "Spark",
  toolTipText = "builds and evaluates an MLlib classifier/regressor via "
    + "cross-validation in Spark", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "WekaClassifierEvaluationSparkJob.gif")
public class MLlibClassifierEvaluationSparkJob extends BaseStep {

  private static final long serialVersionUID = -818676922174748154L;

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

  /** Template - for option setting */
  protected weka.distributed.spark.MLlibClassifierEvaluationSparkJob m_templateJob =
    new weka.distributed.spark.MLlibClassifierEvaluationSparkJob();

  /** The actual job that will run */
  protected weka.distributed.spark.MLlibClassifierEvaluationSparkJob m_runningJob;

  /**
   * Get an optional subdirectory of [output-dir]/eval in which to store results
   *
   * @return an optional subdirectory in the output directory for results
   */
  public String getOutputSubdir() {
    return m_templateJob.getOutputSubdir();
  }

  /**
   * Set an optional subdirectory of [output-dir]/eval in which to store results
   *
   * @param subdir an optional subdirectory in the output directory for results
   */
  @OptionMetadata(displayName = "Output subdirectory",
    description = "An optional subdirectory of <output-dir>/eval in which to "
      + "store the results ", displayOrder = 1)
  public void setOutputSubdir(String subdir) {
    m_templateJob.setOutputSubdir(subdir);
  }

  /**
   * Get the percentage of predictions to retain (via uniform random sampling)
   * for computing AUC and AUPRC. If not specified, then no predictions are
   * retained and these metrics are not computed.
   *
   * @return the fraction (between 0 and 1) of all predictions to retain for
   *         computing AUC/AUPRC.
   */
  public String getSampleFractionForAUC() {
    return m_templateJob.getSampleFractionForAUC();
  }

  /**
   * Set the percentage of predictions to retain (via uniform random sampling)
   * for computing AUC and AUPRC. If not specified, then no predictions are
   * retained and these metrics are not computed.
   *
   * @param f the fraction (between 0 and 1) of all predictions to retain for
   *          computing AUC/AUPRC.
   */
  @OptionMetadata(
    displayName = "Sampling fraction for computing AUC",
    description = "The percentage of all predictions (randomly sampled) to retain "
      + "for computing AUC and AUPRC. If not specified, then these metrics are not"
      + "computed and no predictions are kept. Use this option to keep the number "
      + "of predictions retained under control when computing AUC/PRC.",
    displayOrder = 2)
  public
    void setSampleFractionForAUC(String f) {
    m_templateJob.setSampleFractionForAUC(f);
  }

  /**
   * Set whether to evaluate on a test set (if present in the dataset manager).
   * Knowledge Flow mode only
   *
   * @param evaluateOnTestSetIfPresent true to evaluate on a test set if
   *          available
   */
  @OptionMetadata(displayName = "Evaluate on separate test set if present",
    description = "Evaluate on a separate test dataset (if present) rather "
      + "than perform cross-validation on the training data.", displayOrder = 3)
  public
    void setEvaluateOnTestSetIfPresent(boolean evaluateOnTestSetIfPresent) {
    m_templateJob.setEvaluateOnTestSetIfPresent(evaluateOnTestSetIfPresent);
  }

  /**
   * Get whether to evaluate on a test set (if present in the dataset manager).
   * Knowledge Flow mode only
   *
   * @return true to evaluate on a test set if available
   */
  public boolean getEvaluateOnTestSetIfPresent() {
    return m_templateJob.getEvaluateOnTestSetIfPresent();
  }

  /**
   * Set the total number of folds to use
   *
   * @param totalFolds the total number of folds for the cross-validation
   */
  @OptionMetadata(displayName = "Number of folds", description = "Number of "
    + "folds to use when cross-validating", displayOrder = 4)
  public void setTotalFolds(String totalFolds) {
    m_templateJob.setTotalFolds(totalFolds);
  }

  /**
   * Get the total number of folds to use
   *
   * @return the total number of folds for the cross-validation
   */
  public String getTotalFolds() {
    return m_templateJob.getTotalFolds();
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
    + "name or 1-based index of the class attribute", displayOrder = 6)
  public void setClassAttribute(String c) {
    m_templateJob.setClassAttribute(c);
  }

  /**
   * Set the MLlib classifier/regressor to evaluate
   *
   * @param classifier the classifier ro regressor to evaluate
   */
  @OptionMetadata(displayName = "MLlib classifier/regressor",
    description = "The MLlib classifier or regressor to evaluate",
    displayOrder = 0)
  public void setClassifier(MLlibClassifier classifier) {
    m_templateJob.setClassifier(classifier);
  }

  /**
   * Get the MLlib classifier/regressor to evaluate
   *
   * @return the classifier ro regressor to evaluate
   */
  public MLlibClassifier getClassifier() {
    return m_templateJob.getClassifier();
  }

  @Override
  public void stepInit() throws WekaException {
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    m_sparkLogAppender = null;

    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());
      m_runningJob =
        new weka.distributed.spark.MLlibClassifierEvaluationSparkJob();
      m_runningJob.setOptions(m_templateJob.getOptions());
      m_runningJob.setStatusMessagePrefix(((StepManagerImpl) getStepManager())
        .stepStatusMessagePrefix());
      m_runningJob.setEnvironment(getStepManager().getExecutionEnvironment()
        .getEnvironmentVariables());
      m_runningJob.setLog(getStepManager().getLog());
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
      result.add(StepManager.CON_DATASET);
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
    Instances evalAsInstances = m_runningJob.getInstances();
    String evalAsText = m_runningJob.getText();

    if (evalAsText != null && evalAsText.length() > 0) {
      if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_TEXT) > 0) {
        String title = "Spark MLlib Eval: ";
        String classifierSpec =
          m_templateJob.getClassifier().getClass().getCanonicalName();
        if (m_templateJob.getClassifier() instanceof OptionHandler) {
          classifierSpec +=
            " " + Utils.joinOptions(m_templateJob.getClassifier().getOptions());
        }
        title += classifierSpec;
        Data textData = new Data(StepManager.CON_TEXT, evalAsText);
        textData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE, title);
        getStepManager().outputData(textData);
      }

      if (getStepManager()
        .numOutgoingConnectionsOfType(StepManager.CON_DATASET) > 0) {
        Data instData = new Data(StepManager.CON_DATASET, evalAsInstances);
        instData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
        instData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
        getStepManager().outputData(instData);
      }
    }
  }

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.HiddenPropertyMLlibJobStepEditorDialog";
  }
}
