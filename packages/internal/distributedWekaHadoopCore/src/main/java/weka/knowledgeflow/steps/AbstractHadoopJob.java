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
 *    AbstractHadoopJob
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import weka.core.Utils;
import weka.core.WekaException;
import weka.distributed.hadoop.HadoopJob;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManagerImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for Knowledge Flow Hadoop steps
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class AbstractHadoopJob extends BaseStep {

  protected static final String CON_SUCCESS = "success";
  protected static final String CON_FAILURE = "failure";
  private static final long serialVersionUID = 1633295473478059365L;

  /** The underlying Hadoop job to run */
  protected HadoopJob m_job = null;

  /** Copy of the job to actually run */
  protected transient HadoopJob m_runningJob = null;

  /** Options for the underlying job */
  protected String m_jobOpts = "";

  /**
   * Returns the underlying HadoopJob object
   *
   * @return the underlying HadoopJob object
   */
  public HadoopJob getUnderlyingJob() {
    return m_job;
  }

  /**
   * Update the underlying HadoopJob
   *
   * @param job the HadoopJob to update with
   */
  public void updateUnderlyingJob(HadoopJob job) {
    m_job = job;
    m_jobOpts = Utils.joinOptions(m_job.getOptions());
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
    return "weka.gui.knowledgeflow.steps.HadoopJobStepEditorDialog";
  }

  @Override
  public void stepInit() throws WekaException {
    try {
      if (m_job != null) {
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
    }
  }

  @Override
  public void start() throws WekaException {
    if (getStepManager().numIncomingConnections() == 0) {
      getStepManager().logBasic("Starting Hadoop job as a start point");
      runJob();
    }
  }

  @Override
  public void stop() {
    if (m_runningJob != null
      && m_runningJob.getJobStatus() == DistributedJob.JobStatus.RUNNING) {
      m_runningJob.stopJob();
    }
    super.stop();
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    runJob();
  }

  protected void runJob() throws WekaException {
    try {
      getStepManager().processing();
      getStepManager().logBasic("Execution Hadoop Job");
      getStepManager().statusMessage("Executing...");
      if (!m_runningJob.runJob()) {
        getStepManager()
          .logError(
            "Job failed "
              + (getStepManager().numOutgoingConnectionsOfType(CON_FAILURE) > 0 ? "- invoking failure connection"
                : ""), null);
        getStepManager().outputData(
          new Data(CON_FAILURE, "ERROR: job failed - check logs"));
      } else {
        getStepManager()
          .logBasic(
            "Job finished successfully "
              + (getStepManager().numOutgoingConnectionsOfType(CON_SUCCESS) > 0 ? "- invoking "
                + "success connection"
                : ""));
        notifyJobOutputConnections();
        getStepManager().outputData(new Data(CON_SUCCESS));
      }
      getStepManager().finished();
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
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
}
