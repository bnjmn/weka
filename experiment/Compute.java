/*
 *    Compute.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.experiment;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface to something that can accept remote connections and execute
 * a task.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public interface Compute extends Remote {
  
  /**
   * Execute a task
   * @param t Task to be executed
   * @exception RemoteException if something goes wrong.
   * @return a unique ID for the task
   */
  Object executeTask(Task t) throws RemoteException;

  /**
   * Check on the status of a <code>Task</code>
   *
   * @param taskId the ID for the Task to be checked
   * @return the status of the Task
   * @exception Exception if an error occurs
   */
  Object checkStatus(Object taskId) throws Exception;
}

