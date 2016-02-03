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
 *    LogHandler.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.LogHandler;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.gui.Logger;
import weka.knowledgeflow.steps.Step;
import weka.knowledgeflow.steps.WekaAlgorithmWrapper;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Class that wraps a {@code weka.gui.Logger} and filters log messages according
 * to the set logging level. Note that warnings and errors reported via the
 * logWarning() and logError() methods will always be output regardless of the
 * logging level set.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class LogManager implements LogHandler {

  /** Prefix to use for status messages */
  protected String m_statusMessagePrefix = "";

  /** The log to use */
  protected Logger m_log;

  /** True if status messages should be output as well as log messages */
  protected boolean m_status;

  /**
   * The level at which to report messages to the log. Messages at this level or
   * higher will be reported to the log/status. Note that warnings and errors
   * and errors reported via the logWarning() and logError() methods will always
   * be output, regardless of the logging level set
   */
  protected LoggingLevel m_levelToLogAt = LoggingLevel.BASIC;

  /**
   * Constructor that takes a {@code Step}. Uses the log from the step
   * 
   * @param source the source {@code Step}
   */
  public LogManager(Step source) {
    m_status = true;
    String prefix = (source != null ? source.getName() : "Unknown") + "$";

    prefix += (source != null ? source.hashCode() : 1) + "|";
    if (source instanceof WekaAlgorithmWrapper) {
      Object wrappedAlgo =
        ((WekaAlgorithmWrapper) source).getWrappedAlgorithm();
      if (wrappedAlgo instanceof OptionHandler) {
        prefix +=
          Utils.joinOptions(((OptionHandler) wrappedAlgo).getOptions()) + "|";
      }
    }

    m_statusMessagePrefix = prefix;
    if (source != null) {
      m_log = ((StepManagerImpl) source.getStepManager()).getLog();
      setLoggingLevel(((StepManagerImpl) source.getStepManager())
        .getLoggingLevel());
    }
  }

  /**
   * Constructor that takes a log
   * 
   * @param log the log to wrap
   */
  public LogManager(Logger log) {
    this(log, true);
  }

  /**
   * Constructor that takes a log
   * 
   * @param log the log to wrap
   * @param status true if warning and error messages should be output to the
   *          status area as well as to the log
   */
  public LogManager(Logger log, boolean status) {
    m_log = log;
    m_status = status;
  }

  /**
   * Utility method to convert a stack trace to a String
   *
   * @param throwable the {@code Throwable} to convert to a stack trace string
   * @return the string containing the stack trace
   */
  public static String stackTraceToString(Throwable throwable) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);

    return sw.toString();
  }

  /**
   * Set the log wrap
   *
   * @param log the log to wrap
   */
  public void setLog(Logger log) {
    m_log = log;
  }

  /**
   * Get the wrapped log
   *
   * @return the wrapped log
   */
  public Logger getLog() {
    return m_log;
  }

  /**
   * Get the logging level in use
   *
   * @return
   */
  public LoggingLevel getLoggingLevel() {
    return m_levelToLogAt;
  }

  /**
   * Set the logging level to use
   *
   * @param level the level to use
   */
  public void setLoggingLevel(LoggingLevel level) {
    m_levelToLogAt = level;
  }

  /**
   * Log at the low level
   *
   * @param message the message to log
   */
  public void logLow(String message) {
    log(message, LoggingLevel.LOW);
  }

  /**
   * Log at the basic level
   *
   * @param message the message to log
   */
  public void logBasic(String message) {
    log(message, LoggingLevel.BASIC);
  }

  /**
   * Log at the detailed level
   *
   * @param message the message to log
   */
  public void logDetailed(String message) {
    log(message, LoggingLevel.DETAILED);
  }

  /**
   * Log at the debugging level
   *
   * @param message the message to log
   */
  public void logDebug(String message) {
    log(message, LoggingLevel.DEBUGGING);
  }

  /**
   * Log a warning
   *
   * @param message the message to log
   */
  public void logWarning(String message) {
    log(message, LoggingLevel.WARNING);
    if (m_status) {
      statusMessage("WARNING: " + message);
    }
  }

  /**
   * Log an error
   *
   * @param message the message to log
   * @param cause the cause of the error
   */
  public void logError(String message, Exception cause) {
    log(message, LoggingLevel.ERROR, cause);
    if (m_status) {
      statusMessage("ERROR: " + message);
    }
  }

  /**
   * Output a status message
   *
   * @param message the status message
   */
  public void statusMessage(String message) {
    if (m_log != null) {
      m_log.statusMessage(statusMessagePrefix() + message);
    }
  }

  /**
   * Log a message at the supplied level
   *
   * @param message the message to log
   * @param messageLevel the level to log at
   */
  public void log(String message, LoggingLevel messageLevel) {
    log(message, messageLevel, null);
  }

  /**
   * Log a message at the supplied level
   *
   * @param message the message to log
   * @param messageLevel the level to log at
   * @param cause an optional exception for error level messages
   */
  protected void
    log(String message, LoggingLevel messageLevel, Throwable cause) {
    if (messageLevel == LoggingLevel.WARNING
      || messageLevel == LoggingLevel.ERROR
      || messageLevel.ordinal() <= m_levelToLogAt.ordinal()) {
      String logMessage =
        "[" + messageLevel.toString() + "] " + statusMessagePrefix() + message;
      if (cause != null) {
        logMessage += "\n" + stackTraceToString(cause);
      }
      if (m_log != null) {
        m_log.logMessage(logMessage);
        if (messageLevel == LoggingLevel.ERROR
          || messageLevel == LoggingLevel.WARNING) {
          statusMessage(messageLevel.toString() + " (see log for details)");
        }
      } else {
        System.err.println(logMessage);
      }
    }
  }

  private String statusMessagePrefix() {
    return m_statusMessagePrefix;
  }
}
