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
 *    WekaTaskMap.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import weka.core.LogHandler;
import weka.experiment.Task;
import weka.experiment.TaskStatusInfo;
import weka.gui.Logger;
import weka.server.logging.ServerLogger;

/**
 * Class that maintains a map of tasks for execution.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaTaskMap {

  /** The string to use to separate task name from ID */
  public static final String s_taskNameIDSeparator = "---";

  /**
   * Information about a task.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class WekaTaskEntry implements Comparator<WekaTaskEntry>,
    Comparable<WekaTaskEntry>, Serializable {

    /**
     * Special constant to augment those in TaskStatusInfo. Used when a slave
     * talks back its master at the time that a task is added to its executor
     * pool for execution. At this point it might get queued rather than
     * executed. Once it is executed, the slave will tell the master that it is
     * now executing (TaskStatusInfo.PROCESSING).
     */
    public static final int PENDING = 4;

    /**
     * Special constant to augment those in TaskStatusInfo. Used when a task has
     * been stopped (successfully). Only applies to the current execution of a
     * given task. Scheduled tasks will still execute again at the next
     * scheduled time.
     */
    public static final int STOPPED = 5;

    /**
     * For serialization
     */
    private static final long serialVersionUID = -5138133643265268371L;

    /** Name of the task */
    protected String m_taskName;

    /** ID of the task */
    protected String m_taskID;

    /**
     * ID of the task on a remote server (only used when this task is handed off
     * to a slave for execution). Note that this is name---ID.
     */
    protected String m_remoteID;

    /** holds the host:port of the last server we handed this task to */
    protected String m_server = "";

    /** the host port of us (the originating server) */
    protected String m_originatingServer = "";

    /**
     * If true then this task was handed to the current WekaServer from the
     * master server it is registered with.
     */
    protected boolean m_fromMaster;

    /** Date of last execution */
    protected Date m_lastExecution;

    /**
     * Constructor
     * 
     * @param name task name
     * @param id task id
     */
    public WekaTaskEntry(String name, String id) {
      m_taskName = name;
      m_taskID = id;
    }

    /**
     * Constructor
     * 
     * @param nameAndID name and ID as one string
     * @throws Exception if a problem occurs
     */
    public WekaTaskEntry(String nameAndID) throws Exception {
      String[] parts = nameAndID.split(s_taskNameIDSeparator);
      if (parts.length != 2) {
        throw new Exception("A task needs a name and an ID (\"name"
          + s_taskNameIDSeparator + "ID\"");
      }

      m_taskName = parts[0];
      m_taskID = parts[1];
    }

    public void setName(String name) {
      m_taskName = name;
    }

    public String getName() {
      return m_taskName;
    }

    public void setID(String id) {
      m_taskID = id;
    }

    public String getID() {
      return m_taskID;
    }

    public synchronized void setOriginatingServer(String orig) {
      m_originatingServer = orig;
    }

    public synchronized String getOriginatingServer() {
      return m_originatingServer;
    }

    public synchronized void setServer(String server) {
      m_server = server;
    }

    public synchronized String getServer() {
      return m_server;
    }

    public synchronized void setCameFromMaster(boolean master) {
      m_fromMaster = master;
    }

    public synchronized boolean getCameFromMaster() {
      return m_fromMaster;
    }

    public synchronized void setRemoteID(String id) {
      m_remoteID = id;
    }

    public synchronized String getRemoteID() {
      return m_remoteID;
    }

    public synchronized void setLastExecution(Date d) {
      m_lastExecution = d;
    }

    public synchronized Date getLastExecution() {
      return m_lastExecution;
    }

    @Override
    public String toString() {
      return m_taskName + s_taskNameIDSeparator + m_taskID;
    }

    @Override
    public int compare(WekaTaskEntry o1, WekaTaskEntry o2) {
      int nameComp = o1.getName().compareTo(o2.getName());

      if (nameComp != 0) {
        return nameComp;
      }

      return o1.getID().compareTo(o2.getID());
    }

    @Override
    public int compareTo(WekaTaskEntry o) {
      return compare(this, o);
    }

    @Override
    public boolean equals(Object other) {
      if (other == null || !(other instanceof WekaTaskEntry)) {
        return false;
      }

      return (compareTo((WekaTaskEntry) other) == 0);
    }

    @Override
    public int hashCode() {
      return m_taskID.hashCode();
    }
  }

  /**
   * Simple wrapper that wraps a NamedTask interface around a Task.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class SimpleNamedTask implements NamedTask, LogHandler {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 1479669814753658779L;

    protected Task m_theTask;
    protected String m_name;
    protected Logger m_log;

    public SimpleNamedTask(Task toWrap) {
      m_theTask = toWrap;
    }

    @Override
    public void execute() {
      m_log.logMessage(m_name + " beginning execution...");
      m_theTask.execute();
    }

    @Override
    public void stop() {
    }

    @Override
    public TaskStatusInfo getTaskStatus() {
      // delegate
      return m_theTask.getTaskStatus();
    }

    @Override
    public void setName(String name) {
      m_name = name;
    }

    @Override
    public String getName() {
      return m_name;
    }

    @Override
    public void setLog(Logger log) {
      m_log = log;
    }

    @Override
    public Logger getLog() {
      return m_log;
    }

    @Override
    public void freeMemory() {
      if (m_theTask != null && m_theTask.getTaskStatus() != null) {
        m_theTask.getTaskStatus().setTaskResult(null);
      }
    }

    @Override
    public void persistResources() {
    }

    @Override
    public void loadResources() {
    }

    @Override
    public void loadResult() {
    }

    @Override
    public void purge() {
    }
  }

  /**
   * Wrapper NamedTask that delegates to the task it wraps around. Useful for
   * converting scheduled tasks into unscheduled tasks. A scheduled task is
   * registered with the server it is first transmitted to and this server has
   * the responsibility of enforcing the schedule. If it hands off a task to a
   * slave then the version sent to the slave is a one-off unscheduled copy of
   * the original task.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class NamedClassDelegator implements NamedTask, LogHandler {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -6887110782055568934L;
    protected NamedTask m_wrappedTask;

    public NamedClassDelegator(NamedTask toWrap) {
      m_wrappedTask = toWrap;
    }

    @Override
    public void execute() {
      m_wrappedTask.execute();
    }

    @Override
    public void stop() {
      m_wrappedTask.stop();
    }

    @Override
    public TaskStatusInfo getTaskStatus() {
      return m_wrappedTask.getTaskStatus();
    }

    @Override
    public void setName(String name) {
      m_wrappedTask.setName(name);
    }

    @Override
    public String getName() {
      return m_wrappedTask.getName();
    }

    @Override
    public void setLog(Logger log) {
      if (m_wrappedTask instanceof LogHandler) {
        ((LogHandler) m_wrappedTask).setLog(log);
      }
    }

    @Override
    public Logger getLog() {
      if (m_wrappedTask instanceof LogHandler) {
        return ((LogHandler) m_wrappedTask).getLog();
      }
      return null;
    }

    @Override
    public void freeMemory() {
      m_wrappedTask.freeMemory();
    }

    @Override
    public void persistResources() {
      m_wrappedTask.persistResources();
    }

    @Override
    public void loadResources() {
      m_wrappedTask.loadResources();
    }

    @Override
    public void loadResult() throws Exception {
      m_wrappedTask.loadResult();
    }

    @Override
    public void purge() {
      m_wrappedTask.purge();
    }
  }

  /** The map of tasks */
  protected Map<WekaTaskEntry, NamedTask> m_taskMap = new HashMap<WekaTaskEntry, NamedTask>();

  /** Holds task ID returned by slave server */
  protected Map<WekaTaskEntry, String> m_slaveTask = new HashMap<WekaTaskEntry, String>();

  public synchronized void addTask(WekaTaskEntry entry, NamedTask task) {
    m_taskMap.put(entry, task);

  }

  public synchronized WekaTaskEntry addTask(NamedTask task) {
    String name = task.getName();
    String id = UUID.randomUUID().toString().trim();

    WekaTaskEntry taskEntry = new WekaTaskEntry(name, id);

    if (task instanceof LogHandler) {
      ServerLogger logger = new ServerLogger(taskEntry);
      ((LogHandler) task).setLog(logger);
    }

    addTask(taskEntry, task);

    return taskEntry;
  }

  public synchronized WekaTaskEntry addTask(Task task) {
    SimpleNamedTask taskWrapper = new SimpleNamedTask(task);
    taskWrapper.setName(task.getClass().getName() + task.hashCode());

    return addTask(taskWrapper);
  }

  public synchronized NamedTask getTask(WekaTaskEntry taskEntry) {
    return m_taskMap.get(taskEntry);
  }

  public synchronized NamedTask getTask(String taskName) {
    if (taskName == null || taskName.length() == 0) {
      return null;
    }

    // if we have an ID as well as a name then go straight to the task
    if (taskName.lastIndexOf(s_taskNameIDSeparator) >= 0) {
      String[] parts = taskName.split(s_taskNameIDSeparator);
      WekaTaskEntry lookup = new WekaTaskEntry(parts[0], parts[1]);

      return m_taskMap.get(lookup);
    }

    // if we just have a name then return the first match
    for (WekaTaskEntry taskEntry : m_taskMap.keySet()) {
      if (taskEntry.getName().equals(taskName)) {
        return m_taskMap.get(taskEntry);
      }
    }
    return null;
  }

  public synchronized void removeTask(WekaTaskEntry wte) {
    m_taskMap.remove(wte);
    // m_lastExecution.remove(wte);
  }

  public synchronized List<WekaTaskEntry> getTaskList() {
    ArrayList<WekaTaskEntry> taskList = new ArrayList<WekaTaskEntry>();
    for (WekaTaskEntry taskEntry : m_taskMap.keySet()) {
      taskList.add(taskEntry);
    }

    Collections.sort(taskList);

    return taskList;
  }

  public synchronized WekaTaskEntry getTaskKey(String taskID)
    throws IllegalArgumentException {
    if (taskID.lastIndexOf(s_taskNameIDSeparator) < 0) {
      throw new IllegalArgumentException("Task identifier needs both a "
        + "name and an ID!");
    }

    for (WekaTaskEntry taskEntry : m_taskMap.keySet()) {
      if (taskEntry.toString().equals(taskID)) {
        return taskEntry;
      }
    }

    return null;
  }

  /*
   * public synchronized Date getExecutionTime(WekaTaskEntry taskEntry) { return
   * m_lastExecution.get(taskEntry); }
   * 
   * public synchronized void setExecutionTime(WekaTaskEntry taskEntry, Date
   * time) { m_lastExecution.put(taskEntry, time); }
   */
}
