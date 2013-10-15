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
 *    DistributedJob.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package distributed.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.distributed.DistributedWekaException;
import weka.gui.Logger;

/**
 * Abstract base class for all distributed jobs.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class DistributedJob implements EnvironmentHandler,
  Serializable {

  /** Property key for specifying weka packages to use in the job */
  public static final String WEKA_ADDITIONAL_PACKAGES_KEY = "weka.distributed.job.additional.packages";

  /** Job name */
  protected String m_jobName = "Job";

  /** Job description */
  protected String m_jobDescription = "A distributed job";

  /** For the Knowledge Flow status area */
  protected String m_statusMessagePrefix = "";

  /** Logger to use */
  protected transient Logger m_log;

  /** Environment variables to use */
  protected transient Environment m_env = Environment.getSystemWide();

  /** True if the current job should abort */
  protected boolean m_stopRunningJob = false;

  /** Enum of job status states */
  public static enum JobStatus {
    NOT_RUNNING, RUNNING, FINISHED, FAILED;
  }

  /** Job's current status */
  protected JobStatus m_status = JobStatus.NOT_RUNNING;

  /**
   * Get a list of weka packages to use from the supplied config
   * 
   * @param config the job config to extract weka package names from
   * @return a list of weka packages configured to be used
   */
  public List<String> getAdditionalWekaPackageNames(DistributedJobConfig config) {
    List<String> result = new ArrayList<String>();

    String packages = config
      .getUserSuppliedProperty(WEKA_ADDITIONAL_PACKAGES_KEY);
    if (!DistributedJobConfig.isEmpty(packages)) {
      packages = environmentSubstitute(packages);
      String[] parts = packages.split(",");
      for (String p : parts) {
        result.add(p.trim());
      }
    }

    return result;
  }

  /**
   * Substitute environment variables in the supplied string.
   * 
   * @param orig the string to modify
   * @return the string with environment variables resolved
   */
  public String environmentSubstitute(String orig) {
    if (m_env != null) {
      try {
        orig = m_env.substitute(orig);
      } catch (Exception ex) {
        // not interested if there are no variables substituted
      }
    }

    return orig;
  }

  /**
   * Get the job name
   * 
   * @return the job name
   */
  public String getJobName() {
    return m_jobName;
  }

  /**
   * Set the job name
   * 
   * @param jobName the name to use
   */
  public void setJobName(String jobName) {
    m_jobName = environmentSubstitute(jobName);
  }

  /**
   * Set the job description
   * 
   * @param jobDescription the description to use
   */
  public void setJobDescription(String jobDescription) {
    m_jobDescription = environmentSubstitute(jobDescription);
  }

  /**
   * Set the prefix to use for log status messages (primarily for use in the
   * Knowledge Flow's status area)
   * 
   * @param prefix the prefix to use
   */
  public void setStatusMessagePrefix(String prefix) {
    m_statusMessagePrefix = prefix;
  }

  /**
   * Set the status of the current job
   * 
   * @param status the status of the job
   */
  public void setJobStatus(JobStatus status) {
    m_status = status;
  }

  /**
   * Get the status of the current job
   * 
   * @return the status of the job
   */
  public JobStatus getJobStatus() {
    return m_status;
  }

  /**
   * Set the log to use
   * 
   * @param log the log to use
   */
  public void setLog(Logger log) {
    m_log = log;
  }

  /**
   * Get the log in use
   * 
   * @return the log in use
   */
  public Logger getLog() {
    return m_log;
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Log a message
   * 
   * @param message the message to log
   */
  protected void logMessage(String message) {
    if (m_log != null) {
      m_log.logMessage(m_statusMessagePrefix + message);
    } else {
      System.err.println(m_statusMessagePrefix + message);
    }
  }

  /**
   * Send a message to the status
   * 
   * @param message the message to status
   */
  protected void statusMessage(String message) {
    if (m_log != null) {
      m_log.statusMessage(m_statusMessagePrefix + message);
    } else {
      System.err.println(m_statusMessagePrefix + message);
    }
  }

  /**
   * Constructor
   */
  protected DistributedJob() {
  }

  /**
   * Constructor
   * 
   * @param jobName job name to use
   * @param jobDescription job description to use
   */
  protected DistributedJob(String jobName, String jobDescription) {
    setJobName(jobName);
    setJobDescription(jobDescription);
  }

  /**
   * Run the job. Subclasses to implement
   * 
   * @return true if the job completed successfully
   * @throws DistributedWekaException if a problem occurs
   */
  public abstract boolean runJob() throws DistributedWekaException;

  /**
   * Signal that the job should abort (if it is currently running)
   */
  public void stopJob() {
    if (m_status == JobStatus.RUNNING) {
      m_stopRunningJob = true;
    }
  }

  /**
   * Utility method to make a "help" options string for the supplied object (if
   * it is an OptionHandler)
   * 
   * @param obj the object to create an options description for
   * @return the options description for the supplied object
   */
  public static String makeOptionsStr(Object obj) {
    StringBuffer result = new StringBuffer();
    Option option;

    // build option string
    result.append("\n");
    result.append(obj.getClass().getName().replaceAll(".*\\.", ""));
    if (obj instanceof OptionHandler) {
      result.append(" options:\n\n");

      Enumeration enm = ((OptionHandler) obj).listOptions();
      while (enm.hasMoreElements()) {
        option = (Option) enm.nextElement();

        result.append(option.synopsis() + "\n");
        result.append(option.description() + "\n");
      }
    }

    return result.toString();
  }
}
