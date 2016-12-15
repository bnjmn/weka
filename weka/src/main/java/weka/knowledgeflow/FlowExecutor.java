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
 *    FlockExecutor.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.Logger;

/**
 * Interface to something that can execute a Knowledge Flow process
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface FlowExecutor {

  /**
   * Set the flow to be executed
   * 
   * @param flow the flow to execute
   */
  void setFlow(Flow flow);

  /**
   * Get the flow to be executed
   * 
   * @return the flow to be executed
   */
  Flow getFlow();

  /**
   * Set a log to use
   * 
   * @param logger the log tos use
   */
  void setLogger(Logger logger);

  /**
   * Get the log in use
   * 
   * @return the log in use
   */
  Logger getLogger();

  /**
   * Set the level to log at
   * 
   * @param level the level to log at (logging messages at this level or below
   *          will be displayed in the log)
   */
  void setLoggingLevel(LoggingLevel level);

  /**
   * Get the logging level to log at
   * 
   * @return the logging level to log at
   */
  public LoggingLevel getLoggingLevel();

  /**
   * Set the execution environment to use
   * 
   * @param env the execution environment to use
   */
  void setExecutionEnvironment(BaseExecutionEnvironment env);

  /**
   * Convenience method for applying settings - implementers should delegate the
   * the execution environment
   * 
   * @param settings the settings to use
   */
  void setSettings(Settings settings);

  /**
   * Convenience method for getting current settings - implementers should
   * delegate the the execution environment
   *
   * @return the settings in use
   */
  Settings getSettings();

  /**
   * Return the execution environment object for this flow executor
   * 
   * @return the execution environment
   */
  BaseExecutionEnvironment getExecutionEnvironment();

  /**
   * Run the flow sequentially (i.e. launch start points sequentially rather
   * than in parallel)
   * 
   * @throws WekaException if a problem occurs during execution
   */
  void runSequentially() throws WekaException;

  /**
   * Run the flow by launching all start points in parallel
   * 
   * @throws WekaException if a problem occurs during execution
   */
  void runParallel() throws WekaException;

  /**
   * Block until all steps are no longer busy
   */
  void waitUntilFinished();

  /**
   * Stop all processing
   */
  void stopProcessing();

  /**
   * Returns true if execution was stopped via the stopProcessing() method
   *
   * @return true if execution was stopped
   */
  boolean wasStopped();

  /**
   * Add a callback to notify when execution finishes
   *
   * @param callback the callback to notify
   */
  void addExecutionFinishedCallback(ExecutionFinishedCallback callback);

  /**
   * Remove a callback
   *
   * @param callback the callback to remove
   */
  void removeExecutionFinishedCallback(ExecutionFinishedCallback callback);
}
