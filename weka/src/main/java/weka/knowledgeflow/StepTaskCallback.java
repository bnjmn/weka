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
 *    StepTaskCallback
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

/**
 * Callback that Steps can use when executing StepTasks via
 * EnvironmentManager.submitTask().
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * 
 * @param <T> the result return type (gets encapsulated in an ExecutionResult)
 */
public interface StepTaskCallback<T> {

  /**
   * Gets called when the {@code StepTask} finishes processing
   * 
   * @param result the {@code ExecutionrRsult} produced by the task
   * @throws Exception if a problem occurs
   */
  public void taskFinished(ExecutionResult<T> result) throws Exception;

  /**
   * Gets called if the {@code StepTask} fails for some reason
   * 
   * @param failedTask the {@StepTask} that failed
   * @param failedResult the {@ExecutionResult} produced by
   *          the failed task (might contain information pertaining to the
   *          failure)
   * @throws Exception if a problem occurs
   */
  public void
    taskFailed(StepTask<T> failedTask, ExecutionResult<T> failedResult)
      throws Exception;
}
