/*
 *    TaskLogger.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.gui;

/** 
 * Interface for objects that display log and display information on
 * running tasks.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface TaskLogger {
  
  /**
   * Tells the task logger that a new task has been started
   */
  public void taskStarted();

  /**
   * Tells the task logger that a task has completed
   */
  public void taskFinished();
}
