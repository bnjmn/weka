/*
 *    RemoteExperimentSubTask.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.experiment;

import weka.core.FastVector;

/**
 * Class to encapsulate an experiment as a task that can be executed on
 * a remote host.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
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
	result.addElement(new Integer(RemoteExperiment.FAILED));
	result.addElement(badResult);
	return result;
      }            
      result.addElement(new Integer(RemoteExperiment.FINISHED));
      result.addElement(goodResult);
      return result;
    }
}
