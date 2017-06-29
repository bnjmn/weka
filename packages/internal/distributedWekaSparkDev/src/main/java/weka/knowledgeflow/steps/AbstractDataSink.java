package weka.knowledgeflow.steps;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import org.apache.log4j.WriterAppender;
import org.apache.spark.api.java.JavaSparkContext;
import weka.core.Utils;
import weka.core.WekaException;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.distributed.spark.DataSink;
import weka.distributed.spark.Dataset;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManagerImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static weka.knowledgeflow.steps.AbstractDataSource.CON_DATA;
import static weka.knowledgeflow.steps.AbstractSparkJob.CON_FAILURE;
import static weka.knowledgeflow.steps.AbstractSparkJob.CON_SUCCESS;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class AbstractDataSink extends BaseStep {

  /** The underlying data sink */
  protected DataSink m_dataSink;

  /** A copy of hte data sink to actually execute */
  protected DataSink m_runningDataSink;

  /** Options for the underlying data sink */
  protected String m_dsOpts = "";

  /**
   * Holds a reference to the log appender used. The first step in a flow will
   * get this and pass it on downstream to any subsequent jobs so that the last
   * job can remove the appender after finishing execution
   */
  protected WriterAppender m_sparkLogAppender;

  /**
   * Holds a reference to the current context. The first step in a flow will get
   * this and pass it downstream to any subsequent jobs
   */
  protected JavaSparkContext m_currentContext;

  /** The caching strategy in play */
  protected CachingStrategy m_cachingStrategy;

  /**
   * Holds a reference to the dataset(s) in play (if any). This will allow
   * costly ops (such as random shuffling to be computed just once)
   */
  protected List<Map.Entry<String, Dataset>> m_currentDatasets;

  /** The output directory in play */
  protected String m_outputDirectory;

  /**
   * Flag set if this step has no outgoing connections
   */
  protected boolean m_last;

  public DataSink getUnderlyingDatasink() {
    return m_dataSink;
  }

  /**
   * Get the options for the underlying datasink
   *
   * @return the options for the underlying datasink
   */
  public String getDatasinkOptions() {
    if (m_dataSink != null) {
      m_dsOpts = Utils.joinOptions(m_dataSink.getOptions());
    }

    return m_dsOpts;
  }

  /**
   * Set the options for the underlying datasink
   *
   * @param opts the options for the underlying datasink
   */
  public void setDatasinkOptions(String opts) {
    if (m_dataSink != null) {
      try {
        if (!DistributedJobConfig.isEmpty(opts)) {
          m_dsOpts = opts;
          m_dataSink.setOptions(Utils.splitOptions(m_dsOpts));
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  @Override
  public void stepInit() throws WekaException {
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    m_sparkLogAppender = null;
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());

      if (m_dataSink != null && m_runningDataSink == null) {
        m_runningDataSink = m_dataSink.getClass().newInstance();
        m_runningDataSink
          .setStatusMessagePrefix(((StepManagerImpl) getStepManager())
            .stepStatusMessagePrefix());
        m_runningDataSink.setOptions(m_dataSink.getOptions());
        m_runningDataSink.setEnvironment(getStepManager()
          .getExecutionEnvironment().getEnvironmentVariables());
        m_runningDataSink.setLog(getStepManager().getLog());
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    m_last = getStepManager().numOutgoingConnectionsOfType(CON_DATA) == 0;
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

    getStepManager().processing();
    // boolean hardFailure = false;

    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());
      if (m_currentContext == null) {
        throw new DistributedWekaException("No spark context available!");
      }

      // pass on any existing datasets that are in play (this will be the
      // case if we are not the first datasource in a chain)
      if (m_currentDatasets != null) {
        for (Map.Entry<String, Dataset> e : m_currentDatasets) {
          m_runningDataSink.setDataset(e.getKey(), e.getValue());
        }
      }

      if (m_cachingStrategy != null) {
        m_runningDataSink.setCachingStrategy(m_cachingStrategy);
      }

      if (m_outputDirectory != null && m_outputDirectory.length() > 0) {
        m_runningDataSink.getSparkJobConfig().setOutputDir(m_outputDirectory);
      }

      getStepManager().logBasic("Executing data sink");
      getStepManager().statusMessage("Executing...");

      if (!m_runningDataSink.runJobWithContext(m_currentContext)) {
        getStepManager().logError("Data sink failed - check log", null);

      } else {
        getStepManager().logBasic("Data sink saved successfully");
      }

      outputData(m_runningDataSink.getDatasetIterator());
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
        || (m_last && getStepManager().numOutgoingConnectionsOfType(CON_DATA) == 0)) {
        getStepManager().logBasic("Shutting context down now!");
        m_runningDataSink.shutdownJob(m_sparkLogAppender);
      } */
      Thread.currentThread().setContextClassLoader(orig);
      m_runningDataSink = null;
    }

    getStepManager().finished();
  }

  public void outputData(Iterator<Map.Entry<String, Dataset>> datasetIterator)
    throws WekaException {
    List<Map.Entry<String, Dataset>> datasets =
      datasetIterator.hasNext() ? new ArrayList<Map.Entry<String, Dataset>>()
        : null;

    if (datasets != null) {
      while (datasetIterator.hasNext()) {
        datasets.add(datasetIterator.next());
      }
    }

    Data output = new Data(CON_DATA);
    if (m_sparkLogAppender != null) {
      output.setPayloadElement(AbstractDataSource.PAYLOAD_APPENDER,
        m_sparkLogAppender);
    }
    if (m_currentContext != null) {
      output.setPayloadElement(AbstractDataSource.PAYLOAD_CONTEXT,
        m_currentContext);
    }
    if (m_cachingStrategy != null) {
      output.setPayloadElement(AbstractDataSource.PAYLOAD_CACHING_STRATEGY,
        m_cachingStrategy);
    }

    if (datasets != null && datasets.size() > 0) {
      output.setPayloadElement(AbstractDataSource.PAYLOAD_DATASETS, datasets);
    }
    if (m_outputDirectory != null && m_outputDirectory.length() > 0) {
      output.setPayloadElement(AbstractDataSource.PAYLOAD_OUTPUT_DIRECTORY,
        m_outputDirectory);
    }
    getStepManager().outputData(output);
  }

  @Override
  public void stop() {
    if (m_runningDataSink != null
      && m_runningDataSink.getJobStatus() == DistributedJob.JobStatus.RUNNING) {
      m_runningDataSink.stopJob();
      if (m_sparkLogAppender != null) {
        m_runningDataSink.shutdownJob(m_sparkLogAppender);
      }
    }
    super.stop();
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<>();
    if (getStepManager().numIncomingConnections() == 0) {
      result.add(CON_DATA);
      result.add(CON_SUCCESS);
      result.add(CON_FAILURE);
    }
    return result;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<>();

    if (getStepManager().numOutgoingConnectionsOfType(CON_DATA) == 0
      && getStepManager().numIncomingConnections() > 0) {
      result.add(CON_DATA);
    }
    return result;
  }

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.SparkDataSinkStepEditorDialog";
  }
}
