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

import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.DistributedWekaException;
import weka.gui.Logger;
import weka.gui.ProgrammaticProperty;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Abstract base class for all distributed jobs.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class DistributedJob implements EnvironmentHandler,
  Serializable, CommandlineRunnable {

  /** Property key for specifying weka packages to use in the job */
  public static final String WEKA_ADDITIONAL_PACKAGES_KEY =
    "*weka.distributed.job.additional.packages";

  /** For serialization */
  private static final long serialVersionUID = 1752660860796976806L;

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
  protected boolean m_stopRunningJob;
  /** Job's current status */
  protected JobStatus m_status = JobStatus.NOT_RUNNING;

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

  /**
   * Utility method to parse an Instance out of a row of CSV data.
   *
   * @param row the row of data to convert to an Instance
   * @param rowHelper the CSVToARFFHeaderMap task to use for parsing purposes
   * @param headerNoSummary the header of the data (sans summary attributes)
   *          that contains attribute information for the instance
   * @param setStringVals true if the values of string attributes are to be set
   *          on the header (rather than accumulate in the header).
   * @return an Instance
   * @throws IOException if a problem occurs
   */
  public static Instance parseInstance(String row,
    CSVToARFFHeaderMapTask rowHelper, Instances headerNoSummary,
    boolean setStringVals) throws IOException {
    Instance result = null;

    String[] parsed = rowHelper.parseRowOnly(row);
    if (parsed.length != headerNoSummary.numAttributes()) {
      throw new IOException(
        "Parsed a row that contains a different number of values than "
          + "there are attributes in the training ARFF header: " + row);
    }

    try {
      result = rowHelper.makeInstance(headerNoSummary, setStringVals, parsed);
    } catch (Exception ex) {
      throw new IOException(ex);
    }

    return result;
  }

  /**
   * Utility method to convert a row of values into an Instance
   *
   * @param row the row of data to convert to an Instance
   * @param rowHelper the configured CSVToARFFHeaderMap task to use for getting
   *          default nominal values from
   * @param headerNoSummary the header of the data (sans summary attributes)
   *          that contains attribute information for the instance
   * @param setStringVals true if the values of string attributes are to be set
   *          on the header (rather than accumulate in the header).
   * @param sparse true if a sparse instance should be created instead of a
   *          dense one
   * @return an Instance
   * @throws IOException if a problem occurs
   */
  public static Instance objectRowToInstance(Object[] row,
    CSVToARFFHeaderMapTask rowHelper, Instances headerNoSummary,
    boolean setStringVals, boolean sparse) throws IOException {

    if (row.length != headerNoSummary.numAttributes()) {
      throw new IOException("The supplied Object[] row contains a different "
        + "number of values than there are attributes in the supplied "
        + "ARFF header");
    }

    try {
      return rowHelper.makeInstanceFromObjectRow(headerNoSummary,
        setStringVals, row, sparse);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Convert a stack trace from a Throwable to a string
   *
   * @param throwable the Throwable to get the stack trace from
   * @return the stack trace as a string
   */
  public static String stackTraceToString(Throwable throwable) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);

    return sw.toString();
  }

  /**
   * Get a list of weka packages to use from the supplied config
   *
   * @param config the job config to extract weka package names from
   * @return a list of weka packages configured to be used
   */
  public List<String>
    getAdditionalWekaPackageNames(DistributedJobConfig config) {
    List<String> result = new ArrayList<String>();

    String packages =
      config.getUserSuppliedProperty(WEKA_ADDITIONAL_PACKAGES_KEY);
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
  @ProgrammaticProperty
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
   * Get the status of the current job
   *
   * @return the status of the job
   */
  public JobStatus getJobStatus() {
    return m_status;
  }

  /**
   * Set the status of the current job
   *
   * @param status the status of the job
   */
  @ProgrammaticProperty
  public void setJobStatus(JobStatus status) {
    m_status = status;
  }

  /**
   * Get the log in use
   *
   * @return the log in use
   */
  public Logger getLog() {
    return m_log;
  }

  /**
   * Set the log to use
   *
   * @param log the log to use
   */
  public void setLog(Logger log) {
    m_log = log;
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
  public void logMessage(String message) {
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
  public void statusMessage(String message) {
    if (m_log != null) {
      m_log.statusMessage(m_statusMessagePrefix + message);
    } else {
      System.err.println(m_statusMessagePrefix + message);
    }
  }

  /**
   * Log a stack trace from an exception
   *
   * @param ex the exception to extract the stack trace from
   */
  public void logMessage(Throwable ex) {
    if (m_log != null) {
      String stackTrace = stackTraceToString(ex);
      m_log.logMessage(stackTrace);
    }
  }

  /**
   * Log a message with a stack trace from an exception
   *
   * @param message the message to log
   * @param ex the Exception to extract the stack trace from
   */
  public void logMessage(String message, Throwable ex) {
    logMessage(message);
    logMessage(ex);
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

  /** Enum of job status states */
  public static enum JobStatus {
    NOT_RUNNING, RUNNING, FINISHED, FAILED;
  }

  /**
   * Perform any setup stuff that might need to happen before commandline
   * execution. Subclasses should override if they need to do something here
   *
   * @throws Exception if a problem occurs during setup
   */
  @Override
  public void preExecution() throws Exception {
  }

  /**
   * Execute the supplied object. Subclasses need to override this method.
   *
   * @param toRun the object to execute
   * @param options any options to pass to the object
   * @throws IllegalArgumentException if the object is not of the expected type.
   */
  @Override
  public void run(Object toRun, String[] options) {
    throw new IllegalArgumentException(
      "Subclass needs to override this method!");
  }

  /**
   * Perform any teardown stuff that might need to happen after execution.
   * Subclasses should override if they need to do something here
   *
   * @throws Exception if a problem occurs during teardown
   */
  @Override
  public void postExecution() throws Exception {
  }
}
