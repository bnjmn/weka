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
import weka.core.Instances;
import java.util.Random;

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
 * Version: $Revision: 1.1 $
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
        for (int iAttribute = 0; iAttribute < instances.numAttributes(); iAttribute++) {
            fBaseScores[iAttribute] = CalcNodeScore(iAttribute);
        }

      
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
	            if (IsArc(bayesNet, iHeadNode, iTailNode)) {
                    bRunSucces = true;
	                // either try a delete
                    bayesNet.getParentSet(iHeadNode).DeleteParent(iTailNode, instances);
                    double fScore = CalcNodeScore(iHeadNode);
                    fDeltaScore = fScore - fBaseScores[iHeadNode];
//System.out.println("Try delete " + iTailNode + "->" + iHeadNode + " dScore = " + fDeltaScore);                    
                    if (fTemp * Math.log((Math.abs(random.nextInt()) % 10000)/10000.0  + 1e-100) < fDeltaScore) {
//System.out.println("success!!!");                    
                        fBaseScores[iHeadNode] = fScore;
                    } else {
                        // roll back
                        bayesNet.getParentSet(iHeadNode).AddParent(iTailNode, instances);
                    }
	            } else {
	                // try to add an arc
	                if (AddArcMakesSense(bayesNet, instances, iHeadNode, iTailNode)) {
                        bRunSucces = true;
                        double fScore = CalcScoreWithExtraParent(iHeadNode, iTailNode);
                        fDeltaScore = fScore - fBaseScores[iHeadNode];
//System.out.println("Try add " + iTailNode + "->" + iHeadNode + " dScore = " + fDeltaScore);                    
                        if (fTemp * Math.log((Math.abs(random.nextInt()) % 10000)/10000.0  + 1e-100) < fDeltaScore) {
//System.out.println("success!!!");                    
                            bayesNet.getParentSet(iHeadNode).AddParent(iTailNode, instances);
                            fBaseScores[iHeadNode] = fScore;
                        }
	                }
	            }
            }

            fTemp = fTemp * m_fDelta;
        }
    } // buildStructure 
	
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

} // SimulatedAnnealing

