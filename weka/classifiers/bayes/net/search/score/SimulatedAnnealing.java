/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * SimulatedAnnealing.java
 * Copyright (C) 2004 Remco Bouckaert
 * 
 */
 
package weka.classifiers.bayes.net.search.score;

import weka.classifiers.bayes.BayesNet;
import weka.core.*;
import java.util.*;

/** SimulatedAnnealing uses simulated annealing for learning Bayesian network
 * structures. For details, see for example 
 * 
 * R.R. Bouckaert. 
 * Bayesian Belief Networks: from Construction to Inference. 
 * Ph.D. thesis, 
 * University of Utrecht, 
 * 1995
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * Version: $Revision: 1.3 $
 */

public class SimulatedAnnealing extends ScoreSearchAlgorithm {

    /** start temperature **/
	double m_fTStart = 10;

	/** change in temperature at every run **/
	double m_fDelta = 0.999;

	/** number of runs **/
	int m_nRuns = 10000;

	/** use the arc reversal operator **/
	boolean m_bUseArcReversal = false;

    public void buildStructure (BayesNet bayesNet, Instances instances) throws Exception {
        super.buildStructure(bayesNet, instances);
        Random random = new Random();

        // determine base scores
        double [] fBaseScores = new double [instances.numAttributes()];
		double fCurrentScore = 0;
        for (int iAttribute = 0; iAttribute < instances.numAttributes(); iAttribute++) {
            fBaseScores[iAttribute] = calcNodeScore(iAttribute);
			fCurrentScore += fBaseScores[iAttribute];
        }

		// keep track of best scoring network
		double fBestScore = fCurrentScore;
		BayesNet bestBayesNet = new BayesNet();
		bestBayesNet.m_Instances = instances;
		bestBayesNet.initStructure();
		copyParentSets(bestBayesNet, bayesNet);

        double fTemp = m_fTStart;
        for (int iRun = 0; iRun < m_nRuns; iRun++) {
            boolean bRunSucces = false;
            double fDeltaScore = 0.0;
            while (!bRunSucces) {
	            // pick two nodes at random
	            int iTailNode = Math.abs(random.nextInt()) % instances.numAttributes();
	            int iHeadNode = Math.abs(random.nextInt()) % instances.numAttributes();
	            while (iTailNode == iHeadNode) {
		            iHeadNode = Math.abs(random.nextInt()) % instances.numAttributes();
	            }
	            if (isArc(bayesNet, iHeadNode, iTailNode)) {
                    bRunSucces = true;
	                // either try a delete
                    bayesNet.getParentSet(iHeadNode).deleteParent(iTailNode, instances);
                    double fScore = calcNodeScore(iHeadNode);
                    fDeltaScore = fScore - fBaseScores[iHeadNode];
//System.out.println("Try delete " + iTailNode + "->" + iHeadNode + " dScore = " + fDeltaScore);                    
                    if (fTemp * Math.log((Math.abs(random.nextInt()) % 10000)/10000.0  + 1e-100) < fDeltaScore) {
//System.out.println("success!!!");                    
						fCurrentScore += fDeltaScore;
                        fBaseScores[iHeadNode] = fScore;
                    } else {
                        // roll back
                        bayesNet.getParentSet(iHeadNode).addParent(iTailNode, instances);
                    }
	            } else {
	                // try to add an arc
	                if (addArcMakesSense(bayesNet, instances, iHeadNode, iTailNode)) {
                        bRunSucces = true;
                        double fScore = calcScoreWithExtraParent(iHeadNode, iTailNode);
                        fDeltaScore = fScore - fBaseScores[iHeadNode];
//System.out.println("Try add " + iTailNode + "->" + iHeadNode + " dScore = " + fDeltaScore);                    
                        if (fTemp * Math.log((Math.abs(random.nextInt()) % 10000)/10000.0  + 1e-100) < fDeltaScore) {
//System.out.println("success!!!");                    
                            bayesNet.getParentSet(iHeadNode).addParent(iTailNode, instances);
                            fBaseScores[iHeadNode] = fScore;
							fCurrentScore += fDeltaScore;
                        }
	                }
	            }
            }
			if (fCurrentScore > fBestScore) {
				copyParentSets(bestBayesNet, bayesNet);				
			}
            fTemp = fTemp * m_fDelta;
        }

		copyParentSets(bayesNet, bestBayesNet);
    } // buildStructure 
	
	/** CopyParentSets copies parent sets of source to dest BayesNet
	 * @param dest: destination network
	 * @param source: source network
	 */
	void copyParentSets(BayesNet dest, BayesNet source) {
		int nNodes = source.getNrOfNodes();
		// clear parent set first
		for (int iNode = 0; iNode < nNodes; iNode++) {
			dest.getParentSet(iNode).copy(source.getParentSet(iNode));
		}		
	} // CopyParentSets

    /**
     * @return double
     */
    public double getDelta() {
        return m_fDelta;
    }

    /**
     * @return double
     */
    public double getTStart() {
        return m_fTStart;
    }

    /**
     * @return int
     */
    public int getRuns() {
        return m_nRuns;
    }

    /**
     * Sets the m_fDelta.
     * @param m_fDelta The m_fDelta to set
     */
    public void setDelta(double fDelta) {
        m_fDelta = fDelta;
    }

    /**
     * Sets the m_fTStart.
     * @param m_fTStart The m_fTStart to set
     */
    public void setTStart(double fTStart) {
        m_fTStart = fTStart;
    }

    /**
     * Sets the m_nRuns.
     * @param m_nRuns The m_nRuns to set
     */
    public void setRuns(int nRuns) {
        m_nRuns = nRuns;
    }

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	public Enumeration listOptions() {
		Vector newVector = new Vector(3);

		newVector.addElement(new Option("\tStart temperature\n", "A", 1, "-A <float>"));
		newVector.addElement(new Option("\tNumber of runs\n", "U", 1, "-U <integer>"));
		newVector.addElement(new Option("\tDelta temperature\n", "D", 1, "-D <float>"));

		return newVector.elements();
	}

	/**
	 * Parses a given list of options. Valid options are:<p>
	 *
	 * For other options see search algorithm.
	 *
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		String sTStart = Utils.getOption('A', options);
		if (sTStart.length() != 0) {
			setTStart(Double.parseDouble(sTStart));
		}
		String sRuns = Utils.getOption('U', options);
		if (sRuns.length() != 0) {
			setRuns(Integer.parseInt(sRuns));
		}
		String sDelta = Utils.getOption('D', options);
		if (sDelta.length() != 0) {
			setDelta(Double.parseDouble(sDelta));
		}
		super.setOptions(options);
	}

	/**
	 * Gets the current settings of the search algorithm.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions() {
		String[] superOptions = super.getOptions();
		String[] options = new String[6 + superOptions.length];
		int current = 0;
		options[current++] = "-A";
		options[current++] = "" + getTStart();

		options[current++] = "-U";
		options[current++] = "" + getRuns();

		options[current++] = "-D";
		options[current++] = "" + getDelta();

		// insert options from parent class
		for (int iOption = 0; iOption < superOptions.length; iOption++) {
			options[current++] = superOptions[iOption];
		}

		// Fill up rest with empty strings, not nulls!
		while (current < options.length) {
			options[current++] = "";
		}
		return options;
	}

} // SimulatedAnnealing

