/*
 *    RemoteExperimentSubTask.java
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

import weka.core.FastVector;

/**
 * Class to encapsulate an experiment as a task that can be executed on
 * a remote host.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
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
