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
 * TabuSearch.java
 * Copyright (C) 2004 Remco Bouckaert
 * 
 */
 
package weka.classifiers.bayes.net.search.score;

import weka.classifiers.bayes.BayesNet;
import weka.core.*;
import java.util.*;
import java.io.Serializable;

/** TabuSearch implements tabu search for learning Bayesian network
 * structures. For details, see for example 
 * 
 * R.R. Bouckaert. 
 * Bayesian Belief Networks: from Construction to Inference. 
 * Ph.D. thesis, 
 * University of Utrecht, 
 * 1995
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * Version: $Revision: 1.2 $
 */
public class TabuSearch extends ScoreSearchAlgorithm {

    /** number of runs **/
    int m_nRuns = 10000;

    /** size of tabu list **/
    int m_nTabuList = 100;

    class Operation implements Serializable {
    	final static int OPERATION_ADD = 0;
    	final static int OPERATION_DEL = 0;
        public Operation() {
        }
        public int m_nTail;
        public int m_nHead;
        public int m_nOperation;
    } // class Operation

    Operation[] m_oTabuList;

    /** use the arc reversal operator **/
    boolean m_bUseArcReversal = false;

    public void buildStructure(BayesNet bayesNet, Instances instances) throws Exception {
        super.buildStructure(bayesNet, instances);
        Random random = new Random();
        m_oTabuList = new Operation[m_nTabuList];
        int iCurrentTabuList = 0;
        initBaseScores(bayesNet, instances);

        for (int iRun = 0; iRun < m_nRuns; iRun++) {
            Operation oOperation = getOptimalOperation(bayesNet, instances);
            m_oTabuList[iCurrentTabuList] = oOperation;
            iCurrentTabuList = (iCurrentTabuList + 1) % m_nTabuList;
        }
    } // buildStructure

    double[] fBaseScores;
    boolean[][] bAddArcMakesSense;
    double[][] fScore;

    void initBaseScores(BayesNet bayesNet, Instances instances) {
        // determine base scores
        fBaseScores = new double[instances.numAttributes()];
        int nNrOfAtts = instances.numAttributes();

        for (int iAttribute = 0; iAttribute < nNrOfAtts; iAttribute++) {
            fBaseScores[iAttribute] = CalcNodeScore(iAttribute);
        }

        // Determine initial structure by finding a good parent-set for classification
        // node using greedy search
        int iAttribute = instances.classIndex();
        double fBestScore = fBaseScores[iAttribute];

        // /////////////////////////////////////////////////////////////////////////////////////////
        int m_nMaxNrOfClassifierParents = 4;

        // /////////////////////////////////////////////////////////////////////////////////////////
        // double fBestScore = CalcNodeScore(iAttribute);
        boolean bProgress = true;

        while (bProgress && bayesNet.getParentSet(iAttribute).GetNrOfParents() < m_nMaxNrOfClassifierParents) {
            int nBestAttribute = -1;

            for (int iAttribute2 = 0; iAttribute2 < instances.numAttributes(); iAttribute2++) {
                if (iAttribute != iAttribute2) {
                    double fScore = CalcScoreWithExtraParent(iAttribute, iAttribute2);

                    if (fScore > fBestScore) {
                        fBestScore = fScore;
                        nBestAttribute = iAttribute2;
                    }
                }
            }

            if (nBestAttribute != -1) {
                bayesNet.getParentSet(iAttribute).AddParent(nBestAttribute, instances);

                fBaseScores[iAttribute] = fBestScore;
            } else {
                bProgress = false;
            }
        }

        // Recalc Base scores
        // Correction for Naive Bayes structures: delete arcs from classification node to children
        for (int iParent = 0; iParent < bayesNet.getParentSet(iAttribute).GetNrOfParents(); iParent++) {
            int nParentNode = bayesNet.getParentSet(iAttribute).GetParent(iParent);

            if (IsArc(bayesNet, nParentNode, iAttribute)) {
                bayesNet.getParentSet(nParentNode).DeleteLastParent(instances);
            }

            // recalc base scores
            fBaseScores[nParentNode] = CalcNodeScore(nParentNode);
        }

        // super.buildStructure();
        // Do algorithm B from here onwards
        // cache scores & whether adding an arc makes sense
        bAddArcMakesSense = new boolean[nNrOfAtts][nNrOfAtts];
        fScore = new double[nNrOfAtts][nNrOfAtts];

        for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) {
            if (bayesNet.getParentSet(iAttributeHead).GetNrOfParents() < m_nMaxNrOfParents) {

                // only bother maintaining scores if adding parent does not violate the upper bound on nr of parents
                for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
                    bAddArcMakesSense[iAttributeHead][iAttributeTail] =
                        AddArcMakesSense(bayesNet, instances, iAttributeHead, iAttributeTail);

                    if (bAddArcMakesSense[iAttributeHead][iAttributeTail]) {
                        fScore[iAttributeHead][iAttributeTail] =
                            CalcScoreWithExtraParent(iAttributeHead, iAttributeTail);
                    }
                }
            }
        }
    } // initBaseScores

	boolean isNotTabu(int iAttributeTail, int iAttributeHead, int eOperation) {
		for (int iTabu = 0; iTabu < m_nTabuList; iTabu++) {
			Operation oOperation = m_oTabuList[iTabu];
			if ((oOperation.m_nOperation == eOperation) &&
				(oOperation.m_nHead == iAttributeHead) &&
				(oOperation.m_nTail == iAttributeTail)) {
					return true;
				}
		}
		return true;
	} // isNotTabu

    Operation getOptimalOperation(BayesNet bayesNet, Instances instances) {
        Operation oOperation = new Operation();
        int nBestAttributeTail = -1;
        int nBestAttributeHead = -1;
        double fBestDeltaScore = 0.0;
        int nBestOperation;
        int nNrOfAtts = instances.numAttributes();

        // find best arc to add
        for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) {
            if (bayesNet.getParentSet(iAttributeHead).GetNrOfParents() < m_nMaxNrOfParents) {
                for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
                    if (bAddArcMakesSense[iAttributeHead][iAttributeTail]) {
                        // System.out.println("gain " +  iAttributeTail + " -> " + iAttributeHead + ": "+ (fScore[iAttributeHead][iAttributeTail] - fBaseScores[iAttributeHead]));
                        if (fScore[iAttributeHead][iAttributeTail] - fBaseScores[iAttributeHead] > fBestDeltaScore) {
                            if (AddArcMakesSense(bayesNet, instances, iAttributeHead, iAttributeTail)) {
                            	if (isNotTabu(iAttributeTail, iAttributeHead, Operation.OPERATION_ADD)) {
	                                fBestDeltaScore = fScore[iAttributeHead][iAttributeTail] - fBaseScores[iAttributeHead];
	                                nBestAttributeTail = iAttributeTail;
	                                nBestAttributeHead = iAttributeHead;
	                                nBestOperation = Operation.OPERATION_ADD;
								}
                            } else {
                                bAddArcMakesSense[iAttributeHead][iAttributeTail] = false;
                            }
                        }
                    }
                }
            }
        }

		// find best arc to delete
		// TODO SORT THIS OUT
		/*
		for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) {
				for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
						if (fScore[iAttributeHead][iAttributeTail] - fBaseScores[iAttributeHead] > fBestDeltaScore) {
							if (AddArcMakesSense(bayesNet, instances, iAttributeHead, iAttributeTail)) {
								if (isNotTabu(iAttributeTail, iAttributeHead, Operation.OPERATION_DEL)) {
									fBestDeltaScore = fScore[iAttributeHead][iAttributeTail] - fBaseScores[iAttributeHead];
									nBestAttributeTail = iAttributeTail;
									nBestAttributeHead = iAttributeHead;
									nBestOperation = Operation.OPERATION_DEL;
								}
							} else {
								bAddArcMakesSense[iAttributeHead][iAttributeTail] = false;
							}
						}
				}
		*/

        if (nBestAttributeHead >= 0) {

            // update network structure
            // System.out.println("Added " + nBestAttributeTail + " -> " + nBestAttributeHead);
            bayesNet.getParentSet(nBestAttributeHead).AddParent(nBestAttributeTail, instances);

            if (bayesNet.getParentSet(nBestAttributeHead).GetNrOfParents() < m_nMaxNrOfParents) {

                // only bother updating scores if adding parent does not violate the upper bound on nr of parents
                fBaseScores[nBestAttributeHead] += fBestDeltaScore;

                // System.out.println(fScore[nBestAttributeHead][nBestAttributeTail] + " " + fBaseScores[nBestAttributeHead] + " " + fBestDeltaScore);
                for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
                    bAddArcMakesSense[nBestAttributeHead][iAttributeTail] =
                        AddArcMakesSense(bayesNet, instances, nBestAttributeHead, iAttributeTail);

                    if (bAddArcMakesSense[nBestAttributeHead][iAttributeTail]) {
                        fScore[nBestAttributeHead][iAttributeTail] =
                            CalcScoreWithExtraParent(nBestAttributeHead, iAttributeTail);

                        // System.out.println(iAttributeTail + " -> " + nBestAttributeHead + ": " + fScore[nBestAttributeHead][iAttributeTail]);
                    }
                }
            }
        }
        return oOperation;
    } // getOptimalOperation

    /**
    * @return number of runs
    */
    public int getRuns() {
        return m_nRuns;
    }

    /**
     * Sets the number of runs
     * @param nRuns The number of runs to set
     */
    public void setRuns(int nRuns) {
        m_nRuns = nRuns;
    }

    /**
     * @return the Tabu List length
     */
    public int getTabuList() {
        return m_nTabuList;
    }

    /**
     * Sets the Tabu List length.
     * @param nTabuList The nTabuList to set
     */
    public void setTabuList(int nTabuList) {
        m_nTabuList = nTabuList;
    }


	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	public Enumeration listOptions() {
		Vector newVector = new Vector(2);

		newVector.addElement(new Option("\tTabu list length\n", "L", 1, "-L <integer>"));
		newVector.addElement(new Option("\tNumber of runs\n", "U", 1, "-U <integer>"));

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
		String sTabuList = Utils.getOption('L', options);
		if (sTabuList.length() != 0) {
			setTabuList(Integer.parseInt(sTabuList));
		}
		String sRuns = Utils.getOption('U', options);
		if (sRuns.length() != 0) {
			setRuns(Integer.parseInt(sRuns));
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
		String[] options = new String[4 + superOptions.length];
		int current = 0;
		options[current++] = "-L";
		options[current++] = "" + getTabuList();

		options[current++] = "-U";
		options[current++] = "" + getRuns();

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
