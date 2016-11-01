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
 *    ExecutionEnvironment.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.Logger;
import weka.gui.knowledgeflow.GraphicalEnvironmentCommandHandler;

import java.util.concurrent.Future;

/**
 * Client (i.e. from the Step perspective) interface for an execution
 * environment. Implementations of ExecutionEnvironment need to extend
 * BaseExecutionEnvironment
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public interface ExecutionEnvironment {

  /**
   * Get a description of this execution environment
   *
   * @return a description of this execution environment
   */
  String getDescription();

  /**
   * Get default settings for this ExecutionEnvironment.
   *
   * @return the default settings for this execution environment, or null if
   *         there are no default settings.
   */
  Defaults getDefaultSettings();

  /**
   * Set whether this execution environment is headless
   * 
   * @param headless true if the execution environment is headless
   */
  void setHeadless(boolean headless);

  /**
   * Get whether this execution environment is headless
   * 
   * @return true if this execution environment is headless
   */
  boolean isHeadless();

  /**
   * Set the environment for performing commands at the application-level in a
   * graphical environment.
   *
   * @handler the handler to use
   */
  void setGraphicalEnvironmentCommandHandler(
    GraphicalEnvironmentCommandHandler handler);

  /**
   * Get the environment for performing commands at the application-level in a
   * graphical environment.
   * 
   * @return the graphical environment command handler, or null if running
   *         headless
   */
  GraphicalEnvironmentCommandHandler getGraphicalEnvironmentCommandHandler();

  /**
   * Set environment variables for this execution environment
   * 
   * @param env the environment variables to use
   */
  void setEnvironmentVariables(Environment env);

  /**
   * Get environment variables for this execution environment
   * 
   * @return the environment variables for this execution environment
   */
  Environment getEnvironmentVariables();

  /**
   * Set knowledge flow settings for this execution environment
   *
   * @param settings the settings to use
   */
  void setSettings(Settings settings);

  /**
   * Get knowledge flow settings for this execution environment
   *
   * @return the settings to use
   */
  Settings getSettings();

  /**
   * Set the log to use
   * 
   * @param log the log to use
   */
  void setLog(Logger log);

  /**
   * Get the log in use
   * 
   * @return the log in use
   */
  Logger getLog();

  /**
   * Set the logging level to use
   *
   * @param level the logging level to use
   */
  void setLoggingLevel(LoggingLevel level);

  /**
   * Get the logging level in use
   *
   * @return the logging level in use
   */
  LoggingLevel getLoggingLevel();

  /**
   * Submit a task to be run by the execution environment. Client steps are free
   * to use this service or to just do their processing locally within their own
   * code.
   * 
   * @param callable the Callable encapsulating the task to be run
   * @return the Future holding the status and result when complete
   * @throws WekaException if processing fails in the case of
   */
  <T> Future<ExecutionResult<T>> submitTask(StepTask<T> callable)
    throws WekaException;

  /**
   * Step/StepManager can use this to request a stop to all processing
   */
  void stopProcessing();
}
