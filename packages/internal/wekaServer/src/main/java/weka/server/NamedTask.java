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
 *    NamedTask.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import weka.experiment.Task;

/**
 * Extension of the Task interface for tasks with a name/ID. Also adds some
 * methods for memory management.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface NamedTask extends Task {

  /**
   * Stop the currently running task (if possible). Does nothing if the task is
   * not currently executing.
   */
  void stop();

  /**
   * Set the name of this task
   * 
   * @param name the name of this task
   */
  void setName(String name);

  /**
   * Get the name of this task
   * 
   * @return the name of this task
   */
  String getName();

  /**
   * Tell the task that it can free any resources (memory, results etc.) that
   * would not be needed for another execution run.
   */
  void freeMemory();

  /**
   * Tell the task that it should persist any resources to disk (e.g. training
   * data, etc.). WekaServer.getTempFile() can be used to get a file to save to.
   */
  void persistResources();

  /**
   * Tell the task that it should load any stored resources from disk into
   * memory.
   */
  void loadResources();

  /**
   * Tell the task to load its result object (if it has one) from disk (if it
   * has persisted it in order to save memory). This method is called when a
   * client has requested to fetch the result.
   * 
   * @throws Exception if the result can't be loaded for some reason
   */
  void loadResult() throws Exception;

  /**
   * Tell the task to delete any disk-based resources.
   */
  void purge(); // delete all disk-based resources
}
