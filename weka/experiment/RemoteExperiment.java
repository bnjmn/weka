/*
 *    RemoteExperiment.java
 *    Copyright (C) 2000 Mark Hall
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package weka.experiment;

import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.Option;
import weka.core.FastVector;
import weka.core.Queue;
import java.rmi.*;

import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.Vector;
import java.beans.PropertyDescriptor;
import javax.swing.DefaultListModel;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;

/**
 * Holds all the necessary configuration information for a distributed
 * experiment. This object is able to be serialized for storage on disk.<p>
 * 
 * This class is experimental at present. Has been tested using 
 * CSVResultListener (sending results to standard out) and 
 * DatabaseResultListener (InstantDB + RmiJdbc bridge). <p>
 *
 * Getting started:<p>
 *
 * Start InstantDB (with the RMI bridge) on some machine. If using java2
 * then specify -Djava.security.policy=weka/experiment/remote.policy to the
 * virtual machine.<p>
 *
 * Start RemoteEngine servers on x machines. Again specify the security
 * policy file if using java2. There must be a 
 * DatabaseUtils.props in either the HOME or current directory of each
 * machine, listing all necessary jdbc drivers.<p>
 *
 * The machine where a RemoteExperiment is started must also have a copy
 * of DatabaseUtils.props listing the URL to the machine where the 
 * database server is running (RmiJdbc + InstantDB). <p>
 *
 * Here is an example of starting a RemoteExperiment: <p>
 *
 * <pre>
 *
 * java weka.experiment.RemoteExperiment -L 1 -U 10 \
 * -T /home/ml/datasets/UCI/iris.arff \
 * -D "weka.experiment.DatabaseResultListener" \
 * -P "weka.experiment.RandomSplitResultProducer" \
 * -h rosebud.cs.waikato.ac.nz -h blackbird.cs.waikato.ac.nz -r -- \
 * -W weka.experiment.ClassifierSplitEvaluator -- \
 * -W weka.classifiers.NaiveBayes
 *
 * </pre>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class RemoteExperiment extends Experiment {

  // Inner class for encapsulating sub experiments
  protected class ExperimentSubTask implements Task {

    /* The (sub) experiment to execute */
    private Experiment m_experiment;

    /**
     * Set the experiment for this sub task
     * @param task the experiment
     */
    public void setExperiment(Experiment task) {
      m_experiment = task;
    }

    /**
     * Get the experiment for this sub task
     * @return this sub task's experiment
     */
    public Experiment getExperiment() {
      return m_experiment;
    }
    
    /**
     * Run the experiment
     */
    public Object execute() {
      FastVector result = new FastVector();
      String goodResult = "(sub)experiment completed successfully";
      try {
	System.err.println("Initializing (exp run # "+m_experiment.getRunLower()
			   +")...");
	m_experiment.initialize();
	System.err.println("Iterating (exp run # "+m_experiment.getRunLower()
			   +")...");
	m_experiment.runExperiment();
	System.err.println("Postprocessing (exp run # "
			   +m_experiment.getRunLower()
			   +")...");
	m_experiment.postProcess();
      } catch (Exception ex) {
	ex.printStackTrace();
	String badResult =  "(sub)experiment (run # "
	  +m_experiment.getRunLower()
	  +") : "+ex.toString();
	result.addElement(new Integer(FAILED));
	result.addElement(badResult);
	return result;
      }            
      result.addElement(new Integer(FINISHED));
      result.addElement(goodResult);
      return result;
    }
  }

  /** The list of objects listening for remote experiment events */
  private FastVector m_listeners = new FastVector();

  /** Holds the names of machines with remoteEngine servers running */
  private FastVector m_remoteHosts = new FastVector();
  
  /** The queue of available hosts */
  private Queue m_remoteHostsQueue = new Queue();

  /** The status of each of the remote hosts */
  private int [] m_remoteHostsStatus;

  protected static final int AVAILABLE=0;
  protected static final int IN_USE=1;
  protected static final int CONNECTION_FAILED=2;
  protected static final int SOME_OTHER_FAILURE=3;

  protected static final int TO_BE_RUN=0;
  protected static final int PROCESSING=1;
  protected static final int FAILED=2;
  protected static final int FINISHED=3;

  protected static final int MAX_FAILURES=10;

  /** Set to true if MAX_FAILURES exceeded */
  private boolean m_experimentAborted = false;

  /** The count of failed sub-experiments */
  private int m_failedCount;

  /** The count of successfully completed sub-experiments */
  private int m_finishedCount;

  /** The base experiment to split up into sub experiments for remote
      execution */
  private Experiment m_baseExperiment = null;

  /** The sub experiments */
  protected Experiment [] m_subExperiments;

  /** The queue of sub experiments waiting to be processed */
  private Queue m_subExpQueue = new Queue();

  /** The status of each of the sub-experiments */
  protected int [] m_subExpComplete;

  /**
   * Add an object to the list of those interested in recieving update
   * information from the RemoteExperiment
   * @param r a listener
   */
  public void addRemoteExperimentListener(RemoteExperimentListener r) {
    m_listeners.addElement(r);
  }

  /**
   * Set the base experiment. A sub experiment will be created for each
   * run in the base experiment.
   * @param base the base experiment to use.
   * @exception Exception if supplied base experiment is null
   */
  public void setBaseExperiment(Experiment base) throws Exception {
    if (base == null) {
      throw new Exception("Base experiment is null!");
    }
    m_baseExperiment = base;
    setRunLower(m_baseExperiment.getRunLower());
    setRunUpper(m_baseExperiment.getRunUpper());
  }
    
  /**
   * Prepares a remote experiment for running, creates sub experiments
   *
   * @exception Exception if an error occurs
   */
  public void initialize() throws Exception {
    if (m_baseExperiment == null) {
      throw new Exception("No base experiment specified!");
    }

    m_experimentAborted = false;
    m_finishedCount = 0;
    m_failedCount = 0;
    m_RunNumber = getRunLower();
    m_DatasetNumber = 0;
    m_PropertyNumber = 0;
    m_CurrentProperty = -1;
    m_CurrentInstances = null;
    m_Finished = false;

    if (m_remoteHosts.size() == 0) {
      throw new Exception("No hosts specified!");
    }
    // initialize all remote hosts to available
    m_remoteHostsStatus = new int [m_remoteHosts.size()];    

    m_remoteHostsQueue = new Queue();
    // prime the hosts queue
    for (int i=0;i<m_remoteHosts.size();i++) {
      m_remoteHostsQueue.push(new Integer(i));
    }

    // set up sub experiments
    m_subExpQueue = new Queue();
    int numExps = getRunUpper() - getRunLower() + 1;
    m_subExperiments = new Experiment[numExps];
    m_subExpComplete = new int[numExps];
    for (int i = getRunLower(); i <= getRunUpper(); i++) {
      // create copy of base experiment using serialization
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      BufferedOutputStream bbo = new BufferedOutputStream(bo);
      ObjectOutputStream oo = new ObjectOutputStream(bbo);
      oo.writeObject(m_baseExperiment);
      oo.close();
      
      ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
      BufferedInputStream bbi = new BufferedInputStream(bi);
      ObjectInputStream oi = new ObjectInputStream(bbi);
      m_subExperiments[i-getRunLower()] = (Experiment)oi.readObject();
      oi.close();
      // one run for each sub experiment
      m_subExperiments[i-getRunLower()].setRunLower(i);
      m_subExperiments[i-getRunLower()].setRunUpper(i);

      m_subExpQueue.push(new Integer(i-getRunLower()));
    }    
  }

  /**
   * Inform all listeners of progress
   * @param status true if this is a status type of message
   * @param log true if this is a log type of message
   * @param finished true if the remote experiment has finished
   * @param message the message.
   */
  private void notifyListeners(boolean status, boolean log, boolean finished,
			  String message) {
    if (m_listeners.size() > 0) {
      for (int i=0;i<m_listeners.size();i++) {
	RemoteExperimentListener r = 
	  (RemoteExperimentListener)(m_listeners.elementAt(i));
	r.remoteExperimentStatus(new RemoteExperimentEvent(status,
							   log,
							   finished,
							   message));
      }
    } else {
      System.err.println(message);
    }
  }

  /**
   * Set the abort flag
   */
  public void abortExperiment() {
    m_experimentAborted = true;
  }

  /**
   * Increment the number of successfully completed sub experiments
   */
  protected synchronized void incrementFinished() {
    m_finishedCount++;
  }

  /**
   * Increment the number of failures
   */
  protected synchronized void incrementFailed() {
    m_failedCount++;
  }

  /**
   * Push an experiment back on the queue of waiting experiments
   * @param expNum the index of the experiment to push onto the queue
   */
  protected synchronized void waitingExperiment(int expNum) {
    m_subExpQueue.push(new Integer(expNum));
  }

  /**
   * Check to see if we have failed to connect to all hosts
   */
  private boolean checkForAllFailedHosts() {
    boolean allbad = true;
    for (int i = 0; i < m_remoteHostsStatus.length; i++) {
      if (m_remoteHostsStatus[i] != CONNECTION_FAILED) {
	allbad = false;
	break;
      }
    }
    if (allbad) {
      abortExperiment();
      notifyListeners(false,true,true,"Experiment aborted! All connections "
		      +"to remote hosts failed.");
    }
    return allbad;
  }

  /**
   * Returns some post experiment information.
   * @return a String containing some post experiment info
   */
  private String postExperimentInfo() {
    StringBuffer text = new StringBuffer();
    text.append(m_finishedCount+" runs completed successfully.\n"
		+m_failedCount+" failures during running.\n");
    System.err.print(text.toString());
    return text.toString();
  }

  /**
   * Pushes a host back onto the queue of available hosts and attempts to
   * launch a waiting experiment (if any).
   * @param hostNum the index of the host to push back onto the queue of
   * available hosts
   */
  protected synchronized void availableHost(int hostNum) {
    if (hostNum >= 0) {
      m_remoteHostsQueue.push(new Integer(hostNum));
    }

    // check for all sub exp complete or all hosts failed or failed count
    // exceeded
    if (m_failedCount == MAX_FAILURES) {
      abortExperiment();
      notifyListeners(false,true,true,"Experiment aborted! Max failures "
		      +"exceeded.");
      return;
    }

    if ((getRunUpper() - getRunLower() + 1) == m_finishedCount) {
      notifyListeners(false,true,true,"Experiment completed successfully.");
      return;
    }
    
    if (checkForAllFailedHosts()) {
      return;
    }
        
    if (!m_subExpQueue.empty() && !m_experimentAborted) {
      if (!m_remoteHostsQueue.empty()) {
	int availHost, waitingExp;
	try {
	  availHost = ((Integer)m_remoteHostsQueue.pop()).intValue();
	  waitingExp = ((Integer)m_subExpQueue.pop()).intValue();
	  launchNext(waitingExp, availHost);
	} catch (Exception ex) {
	  ex.printStackTrace();
	}
      }
    }    
  }

  /**
   * Launch a sub experiment on a remote host
   * @param wexp the index of the sub experiment to launch
   * @param ah the index of the available host to launch on
   */
  public void launchNext(final int wexp, final int ah) {
    
    Thread subExpThread;
    subExpThread = new Thread() {
	public void run() {	      
	  m_remoteHostsStatus[ah] = IN_USE;
	  m_subExpComplete[wexp] = PROCESSING;
	  ExperimentSubTask expSubTsk = new ExperimentSubTask();
	  expSubTsk.setExperiment(m_subExperiments[wexp]);
	  try {
	    String name = "//"
	      +((String)m_remoteHosts.elementAt(ah))
	      +"/RemoteEngine";
	    Compute comp = (Compute) Naming.lookup(name);
	    // assess the status of the sub-exp
	    notifyListeners(false,true,false,"Starting run : "
			    +m_subExperiments[wexp].getRunLower()
			    +" on host "
			    +((String)m_remoteHosts.elementAt(ah)));
	    FastVector status = (FastVector)comp.executeTask(expSubTsk);
	    System.out.println((String)status.elementAt(1));
	    m_remoteHostsStatus[ah] = AVAILABLE;
	    m_subExpComplete[wexp] = 
	      ((Integer)status.elementAt(0)).intValue();
	    if (((Integer)status.elementAt(0)).intValue() == FAILED) {
	      // a non connection related error---possibly host doesn't have
	      // access to data sets or security policy is not set up
	      // correctly or classifier(s) failed for some reason
	      m_remoteHostsStatus[ah] = SOME_OTHER_FAILURE;
	      m_subExpComplete[wexp] = FAILED;
	      notifyListeners(false,true,false,"Run : "
			      +m_subExperiments[wexp].getRunLower()
			      +" failed on host "
			      +((String)m_remoteHosts.elementAt(ah))
			      +". Scheduling for execution on another host.");
	      incrementFailed();
	      // push experiment back onto queue
	      waitingExperiment(wexp);	
	      // push host back onto queue and try launching any waiting 
	      // sub-experiments. Host is pushed back on the queue as the
	      // failure may be temporary---eg. with InstantDB using the
	      // RMI bridge, two or more threads may try to create the
	      // experiment index or results table simultaneously; all but
	      // one will throw an exception. These hosts are still usable
	      // however.
	    availableHost(ah);
	    } else {
	      notifyListeners(false,true,false,"Run : "
			      +m_subExperiments[wexp].getRunLower()
			      +" completed successfully.");
	      // push host back onto queue and try launching any waiting 
	      // sub-experiments
	      availableHost(ah);
	      incrementFinished();
	    }
	  } catch (Exception ce) {
	    m_remoteHostsStatus[ah] = CONNECTION_FAILED;
	    m_subExpComplete[wexp] = TO_BE_RUN;
	    System.err.println(ce);
	    notifyListeners(false,true,false,"Connection to "
			    +((String)m_remoteHosts.elementAt(ah))
			    +" failed. Scheduling run "
			    +m_subExperiments[wexp].getRunLower()
			    +" for execution on another host.");
	    checkForAllFailedHosts();
	    waitingExperiment(wexp);
	  } finally {
	    if (isInterrupted()) {
	      System.err.println("Sub exp Interupted!");
	    }
	  }
	}	   
      };
    subExpThread.setPriority(Thread.MIN_PRIORITY);
    subExpThread.start();
  }

  /**
   * Overides the one in Experiment
   * @exception Exception
   */
  public void nextIteration() throws Exception {

  }

  /** 
   * overides the one in Experiment
   */
  public void advanceCounters() {

  }

  /** 
   * overides the one in Experiment
   */
  public void postProcess() {
   
  }

  /**
   * Add a host name to the list of remote hosts
   * @param hostname the host name to add to the list
   */
  public void addRemoteHost(String hostname) {
    m_remoteHosts.addElement(hostname);
  }

  /**
   * Overides toString in Experiment
   * @return a description of this remote experiment
   */
  public String toString() {
    String result = m_baseExperiment.toString();

    result += "\nRemote Hosts:\n";
    for (int i=0;i<m_remoteHosts.size();i++) {
      result += ((String)m_remoteHosts.elementAt(i)) +'\n';
    }
    return result;
  }

  /**
   * Overides runExperiment in Experiment
   */
  public void runExperiment() {
    int totalHosts = m_remoteHostsQueue.size();
    // Try to launch sub experiments on all available hosts
    for (int i = 0; i < totalHosts; i++) {
      availableHost(-1);
    }
  }

  /**
   * Configures/Runs the Experiment from the command line.
   *
   * @param args command line arguments to the Experiment.
   */
  public static void main(String[] args) {

    try {
      RemoteExperiment exp = null;
      Experiment base = null;
      String expFile = Utils.getOption('l', args);
      String saveFile = Utils.getOption('s', args);
      boolean runExp = Utils.getFlag('r', args);
      FastVector remoteHosts = new FastVector();
      String runHost = " ";
      while (runHost.length() != 0) {
	runHost = Utils.getOption('h', args);
	if (runHost.length() != 0) {
	  remoteHosts.addElement(runHost);
	}
      }
      if (expFile.length() == 0) {
	base = new Experiment();
	try {
	  base.setOptions(args);
	  Utils.checkForRemainingOptions(args);
	} catch (Exception ex) {
	  ex.printStackTrace();
	  String result = "Usage:\n\n"
	    + "-l <exp file>\n"
	    + "\tLoad experiment from file (default use cli options)\n"
	    + "-s <exp file>\n"
	    + "\tSave experiment to file after setting other options\n"
	    + "\t(default don't save)\n"
	    + "-h <remote host name>\n"
	    +"\tHost to run experiment on (may be specified more than once\n"
	    +"\tfor multiple remote hosts)\n"
	    + "-r \n"
	    + "\tRun experiment on (default don't run)\n\n";
	  Enumeration enum = ((OptionHandler)base).listOptions();
	  while (enum.hasMoreElements()) {
	    Option option = (Option) enum.nextElement();
	    result += option.synopsis() + "\n";
	    result += option.description() + "\n";
	  }
	  throw new Exception(result + "\n" + ex.getMessage());
	}
      } else {
	FileInputStream fi = new FileInputStream(expFile);
	ObjectInputStream oi = new ObjectInputStream(
			       new BufferedInputStream(fi));
	Object tmp = oi.readObject();
	if (tmp instanceof RemoteExperiment) {
	  exp = (RemoteExperiment)tmp;
	} else {
	  base = (Experiment)tmp;
	}
	oi.close();
      }
      if (base != null) {
	exp = new RemoteExperiment();
	exp.setBaseExperiment(base);
      }
      for (int i=0;i<remoteHosts.size();i++) {
	exp.addRemoteHost((String)remoteHosts.elementAt(i));
      }
      System.err.println("Experiment:\n" + exp.toString());

      if (saveFile.length() != 0) {
	FileOutputStream fo = new FileOutputStream(saveFile);
	ObjectOutputStream oo = new ObjectOutputStream(
				new BufferedOutputStream(fo));
	oo.writeObject(exp);
	oo.close();
      }
      
      if (runExp) {
	System.err.println("Initializing...");
	exp.initialize();
	System.err.println("Iterating...");
	exp.runExperiment();
	System.err.println("Postprocessing...");
	exp.postProcess();
      }      
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
