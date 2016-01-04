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

package weka.gui.beans;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import org.apache.log4j.WriterAppender;
import org.apache.spark.api.java.JavaSparkContext;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.distributed.spark.Dataset;
import weka.distributed.spark.SparkJob;
import weka.gui.Logger;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for Knowledge Flow steps for Spark
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class AbstractSparkJob extends JPanel implements Startable, BeanCommon,
  EventConstraints, EnvironmentHandler, Visible, SuccessListener,
  FailureListener, Serializable {

  protected static final String PAYLOAD_APPENDER = "logAppender";
  protected static final String PAYLOAD_CONTEXT = "sparkContext";
  protected static final String PAYLOAD_DATASETS = "sparkDatasets";
  protected static final String CACHING_STRATEGY = "cachingStrategy";
  protected static final String OUTPUT_DIRECTORY = "outputDirectory";

  /**
   * For serialization
   */
  private static final long serialVersionUID = -5342320946698685339L;

  /** Object talking to us (if any) */
  protected Object m_listenee;

  /** Listeners for success events */
  protected List<SuccessListener> m_successListeners =
    new ArrayList<SuccessListener>();

  /** Listeners for failure events */
  protected List<FailureListener> m_failureListeners =
    new ArrayList<FailureListener>();

  /** Default bean visual */
  protected BeanVisual m_visual = new BeanVisual("SparkJob",
    BeanVisual.ICON_PATH + "DefaultDataSource.gif", BeanVisual.ICON_PATH
      + "DefaultDataSource_animated.gif");

  /** The underlying Spark job to run */
  protected SparkJob m_job;

  /** A copy of the Spark job to actually run */
  protected transient SparkJob m_runningJob;

  /** Logging */
  protected transient Logger m_log = null;

  /** Environment variables */
  protected transient Environment m_env = null;

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
   * Flag set by upstream spark steps if this step is the last in their list of
   * connections
   */
  protected boolean m_last;

  public AbstractSparkJob() {
    useDefaultVisual();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
  }

  public void setLast(boolean last) {
    m_last = last;
  }

  /**
   * Returns the underlying SparkJob object
   * 
   * @return the underlying SparkJob object
   */
  protected SparkJob getUnderlyingJob() {
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
  public void acceptFailure(FailureEvent e) {
    m_currentContext = e.getPayloadElement(PAYLOAD_CONTEXT);
    m_sparkLogAppender = e.getPayloadElement(PAYLOAD_APPENDER);
    m_currentDatasets = e.getPayloadElement(PAYLOAD_DATASETS);
    m_cachingStrategy = e.getPayloadElement(CACHING_STRATEGY);
    m_outputDirectory = e.getPayloadElement(OUTPUT_DIRECTORY);
    runJob();
  }

  @Override
  public void acceptSuccess(SuccessEvent s) {
    m_currentContext = s.getPayloadElement(PAYLOAD_CONTEXT);
    m_sparkLogAppender = s.getPayloadElement(PAYLOAD_APPENDER);
    m_currentDatasets = s.getPayloadElement(PAYLOAD_DATASETS);
    m_cachingStrategy = s.getPayloadElement(CACHING_STRATEGY);
    m_outputDirectory = s.getPayloadElement(OUTPUT_DIRECTORY);
    runJob();
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "DefaultDataSource.gif",
      BeanVisual.ICON_PATH + "DefaultDataSource_animated.gif");
  }

  @Override
  public BeanVisual getVisual() {
    return m_visual;
  }

  @Override
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  @Override
  public String getCustomName() {
    return m_visual.getText();
  }

  @Override
  public void setCustomName(String name) {
    m_visual.setText(name);
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

  @Override
  public void start() throws Exception {
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    String jobName = "WekaKF:";
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());
      if (m_job != null && m_runningJob == null) {
        m_runningJob = m_job.getClass().newInstance();
        m_runningJob.setStatusMessagePrefix(statusMessagePrefix());
        m_runningJob.setOptions(m_job.getOptions());
        m_runningJob.setEnvironment(m_env);
        m_runningJob.setLog(m_log);
      }
      jobName += m_runningJob.getJobName();
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    if (m_listenee == null) {
      try {
        Thread.currentThread().setContextClassLoader(
          this.getClass().getClassLoader());

        for (SuccessListener l : m_successListeners) {
          jobName +=
            "+" + ((AbstractSparkJob) l).getUnderlyingJob().getJobName();
        }
        if (m_log != null) {
          m_log.logMessage("Setting job name to: " + jobName);
        }
        m_runningJob.setJobName(jobName);

        // we are a start point. Assumption is that we're the *only* start point
        // as things will break down if there are more than one.
        m_sparkLogAppender = m_runningJob.initJob(null);
        m_currentContext = m_runningJob.getSparkContext();
        m_cachingStrategy = m_runningJob.getCachingStrategy();
        m_outputDirectory = m_runningJob.getSparkJobConfig().getOutputDir();
        if (m_successListeners.size() == 0 && m_failureListeners.size() == 0) {
          // no further downstream steps
          m_last = true;
        }
      } finally {
        Thread.currentThread().setContextClassLoader(orig);
      }
      runJob();
    }
  }

  @Override
  public String getStartMessage() {
    return "Run job";
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
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    if (m_runningJob != null) {
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
          m_runningJob.setCachingStrategy(m_cachingStrategy);
        }

        if (m_outputDirectory != null && m_outputDirectory.length() > 0) {
          m_runningJob.getSparkJobConfig().setOutputDir(m_outputDirectory);
        }

        if (m_log != null) {
          m_log.statusMessage(statusMessagePrefix() + "Executing...");
        }

        if (!m_runningJob.runJobWithContext(m_currentContext)) {
          if (m_log != null) {
            m_log.statusMessage(statusMessagePrefix()
              + "ERROR: job failed - check logs");
          } else {
            System.err.println(statusMessagePrefix()
              + "ERROR: job failed - check logs");
          }

          notifyFailureListeners("ERROR: job failed - check logs",
            m_runningJob.getDatasets());
        } else {
          if (m_log != null) {
            m_log.statusMessage(statusMessagePrefix() + "Finished.");
          }

          notifyJobOutputListeners();
          // grab the caching strategy again (just in case the job has
          // altered it for some reason)
          m_cachingStrategy = m_runningJob.getCachingStrategy();
          notifySuccessListeners(m_runningJob.getDatasets());
        }

      } catch (Exception ex) {
        if (m_log != null) {
          m_log.statusMessage(statusMessagePrefix() + "ERROR: "
            + ex.getMessage());
          m_log.logMessage(statusMessagePrefix() + "ERROR: " + ex.getMessage());
        }

        m_cachingStrategy = m_runningJob.getCachingStrategy();
        notifyFailureListeners(ex.getMessage(), m_runningJob.getDatasets());
        ex.printStackTrace();
      } finally {
        // shutdown the context if we are the last step in the chain
        // (i.e. we don't have any success or failure listeners registered)
        if (m_successListeners.size() == 0 && m_failureListeners.size() == 0
          && m_last) {
          if (m_log != null) {
            m_log.logMessage(statusMessagePrefix()
              + "Shutting down Spark context...");
          }
          m_runningJob.shutdownJob(m_sparkLogAppender);
          Thread.currentThread().setContextClassLoader(orig);
        }
        m_runningJob = null;
      }
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
  protected void notifySuccessListeners(
    Iterator<Map.Entry<String, Dataset<?>>> datasetIterator) {

    List<Map.Entry<String, Dataset<?>>> datasets =
      datasetIterator.hasNext() ? new ArrayList<Map.Entry<String, Dataset<?>>>()
        : null;
    if (datasets != null) {
      while (datasetIterator.hasNext()) {
        datasets.add(datasetIterator.next());
      }
    }
    SuccessEvent success = new SuccessEvent(this);

    if (m_sparkLogAppender != null) {
      success.setPayloadElement(PAYLOAD_APPENDER, m_sparkLogAppender);
    }
    if (m_currentContext != null) {
      success.setPayloadElement(PAYLOAD_CONTEXT, m_currentContext);
    }
    if (m_cachingStrategy != null) {
      success.setPayloadElement(CACHING_STRATEGY, m_cachingStrategy);
    }
    if (datasets != null && datasets.size() > 0) {
      success.setPayloadElement(PAYLOAD_DATASETS, datasets);
    }
    if (m_outputDirectory != null && m_outputDirectory.length() > 0) {
      success.setPayloadElement(OUTPUT_DIRECTORY, m_outputDirectory);
    }

    int numListeners = m_successListeners.size();
    int count = 0;
    for (SuccessListener s : m_successListeners) {
      if (count == numListeners - 1) {
        ((AbstractSparkJob) s).setLast(true);
      }
      s.acceptSuccess(success);
      count++;
    }
  }

  /**
   * Notifies any listeners of failure events on failure of this job
   * 
   * @param reason the reason for the failure (exception message)
   */
  protected void notifyFailureListeners(String reason,
    Iterator<Map.Entry<String, Dataset<?>>> datasetIterator) {

    List<Map.Entry<String, Dataset<?>>> datasets =
      datasetIterator.hasNext() ? new ArrayList<Map.Entry<String, Dataset<?>>>()
        : null;

    if (datasets != null) {
      while (datasetIterator.hasNext()) {
        datasets.add(datasetIterator.next());
      }
    }

    FailureEvent failure = new FailureEvent(this, reason);
    if (m_sparkLogAppender != null) {
      failure.setPayloadElement(PAYLOAD_APPENDER, m_sparkLogAppender);
    }
    if (m_currentContext != null) {
      failure.setPayloadElement(PAYLOAD_CONTEXT, m_currentContext);
    }
    if (m_cachingStrategy != null) {
      failure.setPayloadElement(CACHING_STRATEGY, m_cachingStrategy);
    }
    if (datasets != null && datasets.size() > 0) {
      failure.setPayloadElement(PAYLOAD_DATASETS, datasets);
    }
    if (m_outputDirectory != null && m_outputDirectory.length() > 0) {
      failure.setPayloadElement(OUTPUT_DIRECTORY, m_outputDirectory);
    }

    int numListeners = m_failureListeners.size();
    int count = 0;
    for (FailureListener f : m_failureListeners) {
      if (count == numListeners - 1) {
        ((AbstractSparkJob) f).setLast(true);
      }
      f.acceptFailure(failure);
      count++;
    }
  }

  public boolean hasUpstreamConnection() {
    return m_listenee != null;
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
    m_failureListeners.remove(l);
  }

  @Override
  public boolean eventGeneratable(String eventName) {

    if (m_successListeners.size() <= 1 && m_failureListeners.size() <= 1) {
      return true;
    }

    return false;
  }
}
