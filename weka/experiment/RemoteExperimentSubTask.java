/*
 *    RemoteExperimentSubTask.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.experiment;

import weka.core.FastVector;
import java.io.File;

/**
 * Class to encapsulate an experiment as a task that can be executed on
 * a remote host.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class RemoteExperimentSubTask implements Task {

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
      String subTaskIdentifier;
      if (m_experiment.getRunLower() != m_experiment.getRunUpper()) {
	subTaskIdentifier = "(datataset "
	  + ((File)m_experiment.getDatasets().elementAt(0)).getName();
      } else {
	subTaskIdentifier = "(exp run # "+
	  m_experiment.getRunLower();
      }
      try {	
	System.err.println("Initializing " + subTaskIdentifier + ")...");
	m_experiment.initialize();
	System.err.println("Iterating " + subTaskIdentifier + ")...");
	m_experiment.runExperiment();
	System.err.println("Postprocessing " + subTaskIdentifier + ")...");
	m_experiment.postProcess();
      } catch (Exception ex) {
	ex.printStackTrace();
	String badResult =  "(sub)experiment " + subTaskIdentifier 
	  + ") : "+ex.toString();
	result.addElement(new Integer(RemoteExperiment.FAILED));
	result.addElement(badResult);
	return result;
      }            
      result.addElement(new Integer(RemoteExperiment.FINISHED));
      result.addElement(goodResult);
      return result;
    }
}
