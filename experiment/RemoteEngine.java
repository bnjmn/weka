/*
 *    RemoteEngine.java
 *    Copyright (C) 2000 Mark Hall
 *
 */


package weka.experiment;

import java.rmi.*;
import java.rmi.server.*;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Enumeration;

import weka.core.Queue;

/**
 * A general purpose server for executing Task objects sent via RMI.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class RemoteEngine extends UnicastRemoteObject
  implements Compute {

  /** The name of the host that this engine is started on */
  private String m_HostName = "local";

  /** A queue of waiting tasks */
  private Queue m_TaskQueue = new Queue();

  /** A queue of corresponding ID's for tasks */
  private Queue m_TaskIdQueue = new Queue();

  /** A hashtable of experiment status */
  private Hashtable m_TaskStatus = new Hashtable();

  /** Is there a task running */
  private boolean m_TaskRunning = false;

  /**
   * Constructor
   * @param hostName name of the host
   * @exception RemoteException if something goes wrong
   */
  public RemoteEngine(String hostName) throws RemoteException {
    super();
    m_HostName = hostName;

    /* launch a clean-up thread. Will purge any failed or finished 
       tasks still in the TaskStatus hashtable after an hour */
       
    Thread cleanUpThread;
    cleanUpThread = new Thread() {
	public void run() {
	  while (true) {
	    try {
	      // sleep for an hour
	      Thread.sleep(3600000);
	    } catch (InterruptedException ie) {}

	    if (m_TaskStatus.size() > 0) {
	      purge();
	    } else {
	      System.err.println("RemoteEngine : purge - no tasks to check.");
	    }
	  }
	}
      };
    cleanUpThread.setPriority(Thread.MIN_PRIORITY);
    cleanUpThread.start();
  }
  
  /**
   * Takes a task object and queues it for execution
   * @param t the Task object to execute
   * @return an identifier for the Task that can be used when querying
   * Task status
   */
  public synchronized Object executeTask(Task t) throws RemoteException {
    String taskId = ""+System.currentTimeMillis()+":";
    taskId += t.hashCode();
    addTaskToQueue(t, taskId);

    return taskId;
    //    return t.execute();
  }

  /**
   * Returns status information on a particular task
   *
   * @param taskId the ID of the task to check
   * @return a <code>TaskStatusInfo</code> encapsulating task status info
   * @exception Exception if an error occurs
   */
  public Object checkStatus(Object taskId) throws Exception {
    
    TaskStatusInfo inf = (TaskStatusInfo)m_TaskStatus.get(taskId);

    if (inf == null) {
      throw new Exception("RemoteEngine ("+m_HostName+") : Task not found.");
    }
    
    TaskStatusInfo result = new TaskStatusInfo();
    result.setExecutionStatus(inf.getExecutionStatus());
    result.setStatusMessage(inf.getStatusMessage());
    result.setTaskResult(inf.getTaskResult());

    if (inf.getExecutionStatus() == TaskStatusInfo.FINISHED ||
	inf.getExecutionStatus() == TaskStatusInfo.FAILED) {
      System.err.println("Finished/failed Task id : " 
			 + taskId + " checked by client. Removing.");
      m_TaskStatus.remove(taskId);
    }

    return result;
  }

  /**
   * Adds a new task to the queue.
   *
   * @param t a <code>Task</code> value to be added
   * @param taskId the id of the task to be added
   */
  private synchronized void addTaskToQueue(Task t, String taskId) {
    m_TaskQueue.push(t);
    m_TaskIdQueue.push(taskId);
    TaskStatusInfo newTask = new TaskStatusInfo();
    newTask.setStatusMessage("RemoteEngine ("
			     +m_HostName
			     +") : task queued at postion: "
			     +m_TaskQueue.size());
    // add task status to HashTable
    m_TaskStatus.put(taskId, newTask);
    System.err.println("Task id : " + taskId + "Queued.");
    if (m_TaskRunning == false) {
      startTask();
    }
  }

  /**
   * Checks to see if there are any waiting tasks, and if no task is
   * currently running starts a waiting task.
   */
  private void startTask() {
    if (m_TaskRunning == false && m_TaskQueue.size() > 0) {
      Thread activeTaskThread;
      activeTaskThread = new Thread() {
	  public void run() {
	    m_TaskRunning = true;
	    Task currentTask = (Task)m_TaskQueue.pop();
	    String taskId = (String)m_TaskIdQueue.pop();
	    TaskStatusInfo tsi = (TaskStatusInfo)m_TaskStatus.get(taskId);
	    tsi.setExecutionStatus(TaskStatusInfo.PROCESSING);
	    tsi.setStatusMessage("RemoteEngine ("
				 +m_HostName
				 +") : task running...");
	    try {
	      System.err.println("Launching task id : "
				 + taskId + "...");
	      TaskStatusInfo runStatus = (TaskStatusInfo)currentTask.execute();
	      tsi.setExecutionStatus(runStatus.getExecutionStatus());
	      tsi.setStatusMessage("RemoteExperiment ("
				   +m_HostName+") "
				   +runStatus.getStatusMessage());
	    } catch (Exception ex) {
	      tsi.setExecutionStatus(TaskStatusInfo.FAILED);
	      tsi.setStatusMessage("RemoteEngine ("
				   +m_HostName
				   +") : task failed.");
	      System.err.println("Task id " + taskId + "Failed!");
	    } finally {
	      m_TaskRunning = false;
	      // start any waiting tasks
	      startTask();
	    }
	  }
	};
      activeTaskThread.setPriority(Thread.MIN_PRIORITY);
      activeTaskThread.start();
    }
  }
  
  /**
   * Checks the hash table for failed/finished tasks. Any that have been
   * around for an hour or more are removed. Clients are expected to check
   * on the status of their remote tasks. Checking on the status of a
   * finished/failed task will remove it from the hash table, therefore
   * any failed/finished tasks left lying around for more than an hour
   * suggest that their client has died..
   *
   */
  private void purge() {
    Enumeration keys = m_TaskStatus.keys();
    long currentTime = System.currentTimeMillis();
    System.err.println("RemoteEngine purge. Current time : " + currentTime);
    while (keys.hasMoreElements()) {
      String tk = (String)keys.nextElement();
      System.err.print("Examining task id : " + tk + "...");
      String timeString = tk.substring(0, tk.indexOf(':'));
      long ts = Long.valueOf(timeString).longValue();
      if (currentTime - ts > 3600000) {
	TaskStatusInfo tsi = null;
	tsi = (TaskStatusInfo)m_TaskStatus.get(tsi);
	if ((tsi != null) 
	    && (tsi.getExecutionStatus() == TaskStatusInfo.FINISHED ||
	    tsi.getExecutionStatus() == TaskStatusInfo.FAILED)) {
	  System.err.println("\nTask id : " 
			     + tk + " has gone stale. Removing.");
	  m_TaskStatus.remove(tk);
	}
      } else {
	System.err.println("ok.");
      }
    }
  }

  /**
   * Main method. Gets address of the local host, creates a remote engine
   * object and binds it in the RMI registry. If there is no RMI registry,
   * then it tries to create one with default port 1099.
   *
   * @param args 
   */
  public static void main(String[] args) {
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }
    InetAddress localhost = null;
    try {
      localhost = InetAddress.getLocalHost();
      System.err.println("Host name : "+localhost.getHostName());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    String name;
    if (localhost != null) {
      name = "//"+localhost.getHostName()+"/RemoteEngine";
    } else {
      name = "//localhost/RemoteEngine";
    }
    
    try {
      Compute engine = new RemoteEngine(name);
      Naming.rebind(name, engine);
      System.out.println("RemoteEngine bound in RMI registry");
    } catch (Exception e) {
      System.err.println("RemoteEngine exception: " + 
			 e.getMessage());
      // try to bootstrap a new registry
      try {
	System.err.println("Attempting to start rmi registry...");
	java.rmi.registry.LocateRegistry.createRegistry(1099);
	Compute engine = new RemoteEngine(name);
	Naming.rebind(name, engine);
	System.out.println("RemoteEngine bound in RMI registry");
      } catch (Exception ex) {
	ex.printStackTrace();
      }
    }
  }
}
