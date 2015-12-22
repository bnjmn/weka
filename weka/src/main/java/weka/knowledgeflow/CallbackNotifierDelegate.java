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
 *    CallbackNotifierDelegate.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

/**
 * Interface to something that can notify a Step that a Task submitted by
 * ExecutionEnvironment.submitTask() has finished. The default implementation
 * notifies the Step as soon as the task has completed. Other implementations
 * might delay notification (e.g. if a task gets executed on a remote machine
 * then we will want to delay notification until receiving the result back over
 * the wire.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface CallbackNotifierDelegate {

  /**
   * Notify the supplied callback
   *
   * @param callback the callback to notify
   * @param taskExecuted the StepTask that was executed
   * @param result the ExecutionResult that was produced
   * @throws Exception if a problem occurs
   */
    void notifyCallback(StepTaskCallback callback, StepTask taskExecuted,
      ExecutionResult result) throws Exception;
}
