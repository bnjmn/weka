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
 *    ScheduledNamedKnowledgeFlowTask.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server.knowledgeFlow;

import weka.core.LogHandler;
import weka.experiment.TaskStatusInfo;
import weka.gui.Logger;
import weka.server.NamedTask;
import weka.server.Schedule;
import weka.server.Scheduled;

import java.io.Serializable;
import java.util.Map;

/**
 * A scheduled knowledge flow task for the new Knowledge Flow implementation
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ScheduledNamedKnowledgeFlowTask implements NamedTask, Scheduled,
  LogHandler, Serializable {

  private static final long serialVersionUID = 7384217715086132002L;

  /** Delegate for the actual execution */
  protected UnscheduledNamedKnowledgeFlowTask m_wrappedTask;

  /** The schedule to use for execution */
  protected Schedule m_schedule;

  /**
   * Constructor
   * 
   * @param wrappedTask the wrapped {@code UnscheduledNamedKnowledgeFlowTask}
   *          that contains the actual task to execute (with respect to the
   *          schedule)
   * @param schedule the schedule for execution
   */
  public ScheduledNamedKnowledgeFlowTask(
    UnscheduledNamedKnowledgeFlowTask wrappedTask, Schedule schedule) {
    m_wrappedTask = wrappedTask;
    m_schedule = schedule;
  }

  /**
   * Get parameters (environment variable settings) for the task
   * 
   * @return the parameters for the task
   */
  public Map<String, String> getParameters() {
    return m_wrappedTask.getParameters();
  }

  /**
   * Get whether this flow task should execute with start points launched
   * sequentially rather than in parallel
   * 
   * @return true if start points are to be launched sequentially
   */
  public boolean getSequentialExecution() {
    return m_wrappedTask.getSequentialExecution();
  }

  /**
   * Get the flow in JSON format
   *
   * @return the flow in JSON format
   */
  public String getFlowJSON() {
    return m_wrappedTask.getFlowJSON();
  }

  /**
   * Set the log to use when executing this task
   *
   * @param log the log to use
   */
  @Override
  public void setLog(Logger log) {
    m_wrappedTask.setLog(log);
  }

  /**
   * Get the log in use
   *
   * @return the log in use
   */
  @Override
  public Logger getLog() {
    return m_wrappedTask.getLog();
  }

  /**
   * Attempt to stop the task
   */
  @Override
  public void stop() {
    m_wrappedTask.stop();
  }

  /**
   * Set the name of this named task
   *
   * @param name the name of this task
   */
  @Override
  public void setName(String name) {
    m_wrappedTask.setName(name);
  }

  /**
   * Get the name of this named task
   *
   * @return the name of the task
   */
  @Override
  public String getName() {
    return m_wrappedTask.getName();
  }

  /**
   * Free memory
   */
  @Override
  public void freeMemory() {
    m_wrappedTask.freeMemory();
  }

  @Override
  public void persistResources() {
    m_wrappedTask.persistResources();
  }

  /**
   * Load any resources for this task
   */
  @Override
  public void loadResources() {
    m_wrappedTask.loadResources();
  }

  /**
   * Load any result for this task
   *
   * @throws Exception if a problem occurs
   */
  @Override
  public void loadResult() throws Exception {
    m_wrappedTask.loadResources();
  }

  /**
   * Purge any held resources/memory
   */
  @Override
  public void purge() {
    m_wrappedTask.purge();
  }

  /**
   * Set the schedule for this task
   *
   * @param s the schedule to use
   */
  @Override
  public void setSchedule(Schedule s) {
    m_schedule = m_schedule;
  }

  /**
   * Get the schedule for this task
   *
   * @return the schedule for this task
   */
  @Override
  public Schedule getSchedule() {
    return m_schedule;
  }

  /**
   * Execute this task
   */
  @Override
  public void execute() {
    // execute now
    m_wrappedTask.execute();
  }

  /**
   * Get the status of this task
   *
   * @return the status of the task
   */
  @Override
  public TaskStatusInfo getTaskStatus() {
    return m_wrappedTask.getTaskStatus();
  }
}
