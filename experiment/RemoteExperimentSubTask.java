/*
 *    RemoteExperimentSubTask.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.experiment;

import java.io.File;

/**
 * Class to encapsulate an experiment as a task that can be executed on
 * a remote host.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
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
    public TaskStatusInfo execute() {
      //      FastVector result = new FastVector();
      TaskStatusInfo result = new TaskStatusInfo();
      result.setStatusMessage("Running...");
      String goodResult = "(sub)experiment completed successfully";
      String subTaskType;
      if (m_experiment.getRunLower() != m_experiment.getRunUpper()) {
	subTaskType = "(datataset "
	  + ((File)m_experiment.getDatasets().elementAt(0)).getName();
      } else {
	subTaskType = "(exp run # "+
	  m_experiment.getRunLower();
      }
      try {	
	System.err.println("Initializing " + subTaskType + ")...");
	m_experiment.initialize();
	System.err.println("Iterating " + subTaskType + ")...");
	m_experiment.runExperiment();
	System.err.println("Postprocessing " + subTaskType + ")...");
	m_experiment.postProcess();
      } catch (Exception ex) {
	ex.printStackTrace();
	String badResult =  "(sub)experiment " + subTaskType 
	  + ") failed : "+ex.toString();
	result.setExecutionStatus(TaskStatusInfo.FAILED);
	//	result.addElement(new Integer(RemoteExperiment.FAILED));
	//	result.addElement(badResult);
	result.setStatusMessage(badResult);
	result.setTaskResult("Failed");
	return result;
      }            
      //      result.addElement(new Integer(RemoteExperiment.FINISHED));
      //      result.addElement(goodResult);
      result.setExecutionStatus(TaskStatusInfo.FINISHED);
      result.setStatusMessage(goodResult+" "+subTaskType+").");
      result.setTaskResult("No errors");
      return result;
    }
}
