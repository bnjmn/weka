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
 * @version $Revision: 1.2 $
 */
public interface Compute extends Remote {
  
  /**
   * Execute a task
   * @param t Task to be executed
   * @exception RemoteException if something goes wrong.
   */
  Object executeTask(Task t) throws RemoteException;
}
