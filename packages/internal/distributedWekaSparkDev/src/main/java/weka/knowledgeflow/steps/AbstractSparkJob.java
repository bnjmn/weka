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
 *    AbstractSparkJob
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
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
import weka.distributed.spark.Dataset;
import weka.distributed.spark.SparkJob;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManagerImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base class for Knowledge Flow spark steps
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class AbstractSparkJob extends BaseStep {

  protected static final String CON_SUCCESS = "success";
  protected static final String CON_FAILURE = "failure";

  private static final long serialVersionUID = 4709189714177359453L;

  /** The underlying Spark job to run */
  protected SparkJob m_job;

  /** A copy of the Spark job to actually run */
  protected transient SparkJob m_runningJob;

  /** Options for the underlying job */
  protected String m_jobOpts = "";

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

  public void setLast(boolean last) {
    m_last = last;
  }

  /**
   * Returns the underlying SparkJob object
   *
   * @return the underlying SparkJob object
   */
  public SparkJob getUnderlyingJob() {
    return m_job;
  }

  /**
   * Update the underlying SparkJob
   *
   * @param job the SparkJob to update with
   */
  protected void updateUnderlyingJob(SparkJob job) {
    m_job = job;
    m_jobOpts = Utils.joinOptions(m_job.getOptions());
  }

  /**
   * Get the options for the underlying job
   *
   * @return options for the underlying job
   */
  public String getJobOptions() {

    if (m_job != null) {
      m_jobOpts = Utils.joinOptions(m_job.getOptions());
    }

    return m_jobOpts;
  }

  /**
   * Set the options for the underlying job
   *
   * @param opts options for the underlying job
   */
  public void setJobOptions(String opts) {
    if (m_job != null) {
      try {
        if (!DistributedJobConfig.isEmpty(opts)) {
          m_jobOpts = opts;
          m_job.setOptions(Utils.splitOptions(m_jobOpts));
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
      if (m_job != null && m_runningJob == null) {
        m_runningJob = m_job.getClass().newInstance();
        m_runningJob
          .setStatusMessagePrefix(((StepManagerImpl) getStepManager())
            .stepStatusMessagePrefix());
        m_runningJob.setOptions(m_job.getOptions());
        m_runningJob.setEnvironment(getStepManager().getExecutionEnvironment()
          .getEnvironmentVariables());
        m_runningJob.setLog(getStepManager().getLog());
      }
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
  public void start() throws WekaException {
    throw new WekaException("This step cannot operate as a start point");
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

  /**
   * Notifies listeners of output from this job. Subclasses need to override
   * this if they have specific output connections for various data types
   *
   * @throws WekaException if a problem occurs
   */
  protected void notifyJobOutputConnections() throws WekaException {
    // Subclasses override to tell their listeners
    // about any output from the job (e.g. text, instances,
    // classifiers etc.)
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

  @Override
  public void stop() {
    if (m_runningJob != null
      && m_runningJob.getJobStatus() == DistributedJob.JobStatus.RUNNING) {
      m_runningJob.stopJob();
      if (m_sparkLogAppender != null) {
        m_runningJob.shutdownJob(m_sparkLogAppender);
      }
    }
    super.stop();
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnections() == 0) {
      result.add(CON_SUCCESS);
      result.add(CON_FAILURE);
    }
    return result;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numOutgoingConnectionsOfType(CON_SUCCESS) == 0) {
      result.add(CON_SUCCESS);
    }
    if (getStepManager().numOutgoingConnectionsOfType(CON_FAILURE) == 0) {
      result.add(CON_FAILURE);
    }
    return result;
  }

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.SparkJobStepEditorDialog";
  }
}
