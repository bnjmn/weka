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
 *    DelayedCallbackNotifierDelegate.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

/**
 * Implementation of a CallbackNotifierDelegate that stores the ExecutionResult
 * and only notifies the callback when the notifyNow() method is called.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class DelayedCallbackNotifierDelegate
  implements CallbackNotifierDelegate {

  /** The callback to notify */
  protected StepTaskCallback m_callback;

  /** The task executed */
  protected StepTask m_taskExecuted;

  /** The result produced */
  protected ExecutionResult m_result;

  /**
   * Notify the callback. This implementation stores the result, and only
   * notifies the callback when the notifyNow() method is called.
   *
   * @param callback     the callback to notify
   * @param taskExecuted the StepTask that was executed
   * @param result       the ExecutionResult that was produced
   * @throws Exception if a problem occurs
   */
  @Override
  public void notifyCallback(StepTaskCallback callback, StepTask taskExecuted,
    ExecutionResult result) throws Exception {
    // just store callback and result here
    m_callback = callback;
    m_taskExecuted = taskExecuted;
    m_result = result;
  }

  /**
   * Do the notification now
   * 
   * @throws Exception if a problem occurs
   */
  @SuppressWarnings("unchecked")
  public void notifyNow() throws Exception {
    if (m_callback != null && m_result != null) {
      if (m_result.getError() != null) {
        m_callback.taskFinished(m_result);
      } else {
        m_callback.taskFailed(m_taskExecuted, m_result);
      }
    }
  }
}
