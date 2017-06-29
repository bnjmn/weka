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
 *    AbstractDataSource
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import org.apache.log4j.WriterAppender;
import org.apache.spark.api.java.JavaSparkContext;
import weka.core.Utils;
import weka.core.WekaException;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.distributed.spark.DataSource;
import weka.distributed.spark.Dataset;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base Knowledge Flow Step implementation for data sources
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class AbstractDataSource extends BaseStep {

  protected static final String CON_DATA = "dataFrame";
  protected static final String PAYLOAD_APPENDER = "logAppender";
  protected static final String PAYLOAD_CONTEXT = "sparkContext";
  protected static final String PAYLOAD_DATASETS = "sparkDatasets";
  protected static final String PAYLOAD_CACHING_STRATEGY = "cachingStrategy";
  protected static final String PAYLOAD_OUTPUT_DIRECTORY = "outputDirectory";

  private static final long serialVersionUID = -5506159575109532795L;

  /** The underlying datasource */
  protected DataSource m_dataSource;

  /** A copy of the datasource to actually execute */
  protected transient DataSource m_runningDataSource;

  /** Options for the underlying datasource */
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

  /**
   * Holds a reference to the dataset(s) in play (if any). This will allow
   * costly ops (such as random shuffling to be computed just once)
   */
  protected List<Map.Entry<String, Dataset>> m_currentDatasets;

  /** The caching strategy in play */
  protected CachingStrategy m_cachingStrategy;

  /** The output directory in play */
  protected String m_outputDirectory;

  /**
   * Flag set if this step has no outgoing connections
   */
  protected boolean m_last;

  /**
   * Returns the underlying datasource
   *
   * @return the underlying datasource
   */
  public DataSource getUnderlyingDatasource() {
    return m_dataSource;
  }

  /**
   * Get the options for the underlying datasource
   *
   * @return the options for the underlying datasource
   */
  public String getDatasourceOptions() {
    if (m_dataSource != null) {
      m_dsOpts = Utils.joinOptions(m_dataSource.getOptions());
    }

    return m_dsOpts;
  }

  /**
   * Set the options for the underlying datasource
   *
   * @param opts the options for the underlying datasource
   */
  public void setDatasourceOptions(String opts) {
    if (m_dataSource != null) {
      try {
        if (!DistributedJobConfig.isEmpty(opts)) {
          m_dsOpts = opts;
          m_dataSource.setOptions(Utils.splitOptions(m_dsOpts));
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

      if (m_dataSource != null && m_runningDataSource == null) {
        m_runningDataSource = m_dataSource.getClass().newInstance();
        m_runningDataSource
          .setStatusMessagePrefix(((StepManagerImpl) getStepManager())
            .stepStatusMessagePrefix());
        m_runningDataSource.setOptions(m_dataSource.getOptions());
        m_runningDataSource.setEnvironment(getStepManager()
          .getExecutionEnvironment().getEnvironmentVariables());
        m_runningDataSource.setLog(getStepManager().getLog());
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    m_last = getStepManager().numOutgoingConnectionsOfType(CON_DATA) == 0;
  }

  @Override
  public void start() throws WekaException {
    if (getStepManager().numIncomingConnections() == 0) {
      getStepManager().processing();
      ClassLoader orig = Thread.currentThread().getContextClassLoader();
      try {
        String jobName = "WekaKF:" + m_runningDataSource.getJobName();
        Thread.currentThread().setContextClassLoader(
          this.getClass().getClassLoader());

        m_runningDataSource.setJobName(jobName);
        getStepManager().logBasic(
          "Starting data source as start point: " + jobName);

        // we are a start point. Assumption is that we're the *only* start point
        // as things will break down if there are more than one.
        try {
          m_sparkLogAppender = m_runningDataSource.initJob(null);
        } catch (Exception ex) {
          m_runningDataSource = null;
          throw new WekaException(ex);
        }
        m_currentContext = m_runningDataSource.getSparkContext();
        m_cachingStrategy = m_runningDataSource.getCachingStrategy();
        m_outputDirectory =
          m_runningDataSource.getSparkJobConfig().getOutputDir();
      } finally {
        Thread.currentThread().setContextClassLoader(orig);
      }
      runDatasource();
    }
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
    runDatasource();
  }

  protected void runDatasource() throws WekaException {
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
          m_runningDataSource.setDataset(e.getKey(), e.getValue());
        }
      }

      if (m_cachingStrategy != null) {
        m_runningDataSource.setCachingStrategy(m_cachingStrategy);
      }

      if (m_outputDirectory != null && m_outputDirectory.length() > 0) {
        m_runningDataSource.getSparkJobConfig().setOutputDir(m_outputDirectory);
      }

      getStepManager().logBasic("Executing data source");
      getStepManager().statusMessage("Executing...");

      if (!m_runningDataSource.runJobWithContext(m_currentContext)) {
        getStepManager().logError("Data source failed - check log", null);

      } else {
        getStepManager().logBasic("Data source loaded successfully");
      }

      m_cachingStrategy = m_runningDataSource.getCachingStrategy();
      outputData(m_runningDataSource.getDatasetIterator());
    } catch (Exception ex) {
      // if we get an actual exception then we should fail hard here
      // and shut down the context
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
        m_runningDataSource.shutdownJob(m_sparkLogAppender);
      } */
      Thread.currentThread().setContextClassLoader(orig);
      m_runningDataSource = null;
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
    if (m_runningDataSource != null
      && m_runningDataSource.getJobStatus() == DistributedJob.JobStatus.RUNNING) {
      m_runningDataSource.stopJob();
      if (m_sparkLogAppender != null) {
        m_runningDataSource.shutdownJob(m_sparkLogAppender);
      }
    }
    super.stop();
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<>();
    if (getStepManager().numIncomingConnections() == 0) {
      result.add(CON_DATA);
    }
    return result;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<>();

    if (getStepManager().numOutgoingConnectionsOfType(CON_DATA) == 0) {
      result.add(CON_DATA);
    }
    return result;
  }

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.SparkDataSourceStepEditorDialog";
  }
}
