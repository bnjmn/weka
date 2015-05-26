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
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Utils;
import weka.distributed.hadoop.HadoopJob;
import weka.gui.Logger;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;

/**
 * Abstract base Knowledge Flow component for the Hadoop jobs
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class AbstractHadoopJob extends JPanel implements Startable, BeanCommon,
  EnvironmentHandler, Visible, SuccessListener, FailureListener, Serializable {

  /** For serialization */
  private static final long serialVersionUID = -179758256778557347L;

  /** Object talking to us (if any) */
  protected Object m_listenee;

  /** Listeners for success events */
  protected List<SuccessListener> m_successListeners = new ArrayList<SuccessListener>();

  /** Listeners for failure events */
  protected List<FailureListener> m_failureListeners = new ArrayList<FailureListener>();

  /** Default bean visual */
  protected BeanVisual m_visual = new BeanVisual("HadoopJob",
    BeanVisual.ICON_PATH + "DefaultDataSource.gif", BeanVisual.ICON_PATH
      + "DefaultDataSource_animated.gif");

  /** Logging */
  protected transient Logger m_log = null;

  /** Environment variables */
  protected transient Environment m_env = null;

  /** The underlying Hadoop job to run */
  protected HadoopJob m_job = null;

  /** Copy of the job to actually run */
  protected transient HadoopJob m_runningJob = null;

  /** Options for the underlying job */
  protected String m_jobOpts = "";

  /**
   * Constructor
   */
  public AbstractHadoopJob() {
    useDefaultVisual();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
  }

  /**
   * Returns the underlying HadoopJob object
   * 
   * @return the underlying HadoopJob object
   */
  protected HadoopJob getUnderlyingJob() {
    return m_job;
  }

  /**
   * Update the underlying HadoopJob
   * 
   * @param job the HadoopJob to update with
   */
  protected void updateUnderlyingJob(HadoopJob job) {
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
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "DefaultDataSource.gif",
      BeanVisual.ICON_PATH + "DefaultDataSource_animated.gif");
  }

  @Override
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  @Override
  public BeanVisual getVisual() {
    return m_visual;
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  @Override
  public void setCustomName(String name) {
    m_visual.setText(name);
  }

  @Override
  public String getCustomName() {
    return m_visual.getText();
  }

  @Override
  public void stop() {
    if (m_runningJob != null
      && m_runningJob.getJobStatus() == DistributedJob.JobStatus.RUNNING) {
      m_runningJob.stopJob();
    }
  }

  @Override
  public boolean isBusy() {
    if (m_runningJob != null) {
      if (m_runningJob.getJobStatus() == DistributedJob.JobStatus.RUNNING) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void setLog(Logger logger) {
    m_log = logger;
  }

  @Override
  public boolean connectionAllowed(EventSetDescriptor esd) {
    return (m_listenee == null);
  }

  @Override
  public boolean connectionAllowed(String eventName) {
    return (m_listenee == null);
  }

  @Override
  public void connectionNotification(String eventName, Object source) {
    m_listenee = source;
  }

  @Override
  public void disconnectionNotification(String eventName, Object source) {
    if (source == m_listenee) {
      m_listenee = null;
    }
  }

  /**
   * Unique prefix for logging in the Knowledge Flow
   * 
   * @return the unique prefix for this step
   */
  protected String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|";
  }

  /**
   * Runs the underlying job
   */
  protected void runJob() {
    if (m_job != null && m_runningJob == null) {
      try {
        m_runningJob = m_job.getClass().newInstance();
        m_runningJob.setStatusMessagePrefix(statusMessagePrefix());
        m_runningJob.setOptions(m_job.getOptions());
        m_runningJob.setEnvironment(m_env);
        m_runningJob.setLog(m_log);

        if (!m_runningJob.runJob()) {
          if (m_log != null) {
            m_log.statusMessage(statusMessagePrefix()
              + "ERROR: job failed - check logs");
          } else {
            System.err.println(statusMessagePrefix()
              + "ERROR: job failed - check logs");
          }

          notifyFailureListeners("ERROR: job failed - check logs");
        } else {
          if (m_log != null) {
            m_log.statusMessage(statusMessagePrefix() + "Finished.");
          }

          notifyJobOutputListeners();
          notifySuccessListeners();
        }

      } catch (Exception ex) {
        if (m_log != null) {
          m_log.statusMessage(statusMessagePrefix() + "ERROR: "
            + ex.getMessage());
          m_log.logMessage(statusMessagePrefix() + "ERROR: " + ex.getMessage());
        }

        notifyFailureListeners(ex.getMessage());
        ex.printStackTrace();
      } finally {
        m_runningJob = null;
      }
    }
  }

  @Override
  public void start() {
    if (m_listenee == null) {
      // act like a start point if we don't have
      // an upstream step talking to us. Otherwise
      // we'll only start running on receiving an event
      runJob();
    }
  }

  /**
   * Notifies listeners of output from this job. Subclasses need to override
   * this if they have listeners for specific types of output.
   */
  protected void notifyJobOutputListeners() {
    // Subclasses override to tell their listeners
    // about any output from the job (e.g. text, instances,
    // classifiers etc.)
  }

  /**
   * Notifies any listeners of success events on successful execution of this
   * job
   */
  protected void notifySuccessListeners() {
    for (SuccessListener s : m_successListeners) {
      s.acceptSuccess(new SuccessEvent(this));
    }
  }

  /**
   * Notifies any listeners of failure events on failure of this job
   * 
   * @param reason the reason for the failure (exception message)
   */
  protected void notifyFailureListeners(String reason) {
    for (FailureListener f : m_failureListeners) {
      f.acceptFailure(new FailureEvent(this, reason));
    }
  }

  @Override
  public String getStartMessage() {
    return "Run job";
  }

  @Override
  public void acceptFailure(FailureEvent e) {
    runJob();
  }

  @Override
  public void acceptSuccess(SuccessEvent s) {
    runJob();
  }

  /**
   * Add a success listener
   * 
   * @param l the success listener to add
   */
  public synchronized void addSuccessListener(SuccessListener l) {
    m_successListeners.add(l);
  }

  /**
   * Remove a success listener
   * 
   * @param l the success listener to remove
   */
  public synchronized void removeSuccessListener(SuccessListener l) {
    m_successListeners.remove(l);
  }

  /**
   * Add a failure listener
   * 
   * @param l the failure listener to add
   */
  public synchronized void addFailureListener(FailureListener l) {
    m_failureListeners.add(l);
  }

  /**
   * Remove a failure listener
   * 
   * @param l the failure listener to remove
   */
  public synchronized void removeFailureListener(FailureListener l) {
    m_successListeners.remove(l);
  }
}
