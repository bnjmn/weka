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
 *    DefaultCallbackNotifierDelegate.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

/**
 * Default implementation of a CallbackNotifierDelegate. Notifies the
 * callback immediately.
 *
 * @author Mark hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class DefaultCallbackNotifierDelegate
  implements CallbackNotifierDelegate {

  /**
   * Notifies the callback immediately
   *
   * @param callback     the callback to notify
   * @param taskExecuted the StepTask that was executed
   * @param result       the ExecutionResult that was produced
   * @throws Exception if a problem occurs
   */
  @SuppressWarnings("unchecked")
  @Override
  public void notifyCallback(StepTaskCallback callback, StepTask taskExecuted,
    ExecutionResult result) throws Exception {

    if (result.getError() == null) {
      callback.taskFinished(result);
    } else {
      callback.taskFailed(taskExecuted, result);
    }
  }
}
