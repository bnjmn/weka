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
import weka.classifiers.bayes.net.ParentSet;
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
 * Version: $Revision: 1.3 $
 */
public class TabuSearch extends ScoreSearchAlgorithm {

    /** number of runs **/
    int m_nRuns = 10;

    /** size of tabu list **/
    int m_nTabuList = 5;

    class Operation implements Serializable {
    	final static int OPERATION_ADD = 0;
    	final static int OPERATION_DEL = 1;
//    	final static int OPERATION_REVERSE = 2;
        public Operation() {
        }
		public Operation(int nTail, int nHead, int nOperation) {
			m_nHead = nHead;
			m_nTail = nTail;
			m_nOperation = nOperation;
		}
		public boolean equals(Operation other) {
			if (other == null) {
				return false;
			}
			return ((	m_nOperation == other.m_nOperation) &&
			(m_nHead == other.m_nHead) &&
			(m_nTail == other.m_nTail));
		} // equals
        public int m_nTail;
        public int m_nHead;
        public int m_nOperation;
    } // class Operation

	// the actual tabu list
    Operation[] m_oTabuList = null;

	// cache for remembering the change in score for steps in the search space
	class Cache {
		double [] [] m_fDeltaScoreAdd;
		double [] [] m_fDeltaScoreDel;
		Cache(int nNrOfNodes) {
			m_fDeltaScoreAdd = new double [nNrOfNodes][nNrOfNodes];
			m_fDeltaScoreDel = new double [nNrOfNodes][nNrOfNodes];
		}

		public void put(Operation oOperation, double fValue) {
			if (oOperation.m_nOperation == Operation.OPERATION_ADD) {
				m_fDeltaScoreAdd[oOperation.m_nTail][oOperation.m_nHead] = fValue;
			} else {
				m_fDeltaScoreDel[oOperation.m_nTail][oOperation.m_nHead] = fValue;
			}
		}
		public double get(Operation oOperation) {
			if (oOperation.m_nOperation == Operation.OPERATION_ADD) {
				return m_fDeltaScoreAdd[oOperation.m_nTail][oOperation.m_nHead];
			}
			return m_fDeltaScoreDel[oOperation.m_nTail][oOperation.m_nHead];
		}
	}

	/** cache for storing score differences **/
	Cache m_Cache = null;
	
//    /** use the arc reversal operator **/
 //   boolean m_bUseArcReversal = false;

    /** keeps track of score of current structure **/
	double m_fCurrentScore;
	/** keeps track of score pf best structure found so far **/
	double m_fBestScore;
	/** keeps track of best structure found so far **/
	BayesNet m_bestBayesNet;

    public void buildStructure(BayesNet bayesNet, Instances instances) throws Exception {
        super.buildStructure(bayesNet, instances);
        Random random = new Random();
        m_oTabuList = new Operation[m_nTabuList];
        int iCurrentTabuList = 0;
        initCache(bayesNet, instances);

        for (int iRun = 0; iRun < m_nRuns; iRun++) {
            Operation oOperation = performOptimalOperation(bayesNet, instances);
            m_oTabuList[iCurrentTabuList] = oOperation;
            iCurrentTabuList = (iCurrentTabuList + 1) % m_nTabuList;
        }
		copyParentSets(bayesNet, m_bestBayesNet);
		
		// free up memory
		m_bestBayesNet = null;
		m_Cache = null;
    } // buildStructure

    void initCache(BayesNet bayesNet, Instances instances)  throws Exception {
    	
        // determine base scores
		double[] fBaseScores = new double[instances.numAttributes()];
        int nNrOfAtts = instances.numAttributes();

		m_Cache = new Cache (nNrOfAtts);
		
		m_fCurrentScore = 0;
        for (int iAttribute = 0; iAttribute < nNrOfAtts; iAttribute++) {
            fBaseScores[iAttribute] = calcNodeScore(iAttribute);
			m_fCurrentScore += fBaseScores[iAttribute];
        }

        for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) {
                for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
                	if (iAttributeHead != iAttributeTail) {
	                    Operation oOperation = new Operation(iAttributeTail, iAttributeHead, Operation.OPERATION_ADD);
	                    m_Cache.put(oOperation, calcScoreWithExtraParent(iAttributeHead, iAttributeTail) - fBaseScores[iAttributeHead]);
					}
            }
        }

		m_fBestScore = m_fCurrentScore;
		m_bestBayesNet = new BayesNet();
		m_bestBayesNet.m_Instances = instances;
		m_bestBayesNet.initStructure();
		copyParentSets(m_bestBayesNet, bayesNet);

    } // initCache

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

	boolean isNotTabu(Operation oOperation) {
		for (int iTabu = 0; iTabu < m_nTabuList; iTabu++) {
			if (oOperation.equals(m_oTabuList[iTabu])) {
					return false;
				}
		}
		return true;
	} // isNotTabu

    Operation performOptimalOperation(BayesNet bayesNet, Instances instances) throws Exception {
        Operation oBestOperation = null;
        double fBestDeltaScore = -1e100;
        int nNrOfAtts = instances.numAttributes();

        // find best arc to add
        for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) {
            if (bayesNet.getParentSet(iAttributeHead).getNrOfParents() < m_nMaxNrOfParents) {
                for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
                    if (addArcMakesSense(bayesNet, instances, iAttributeHead, iAttributeTail)) {
                        Operation oOperation = new Operation(iAttributeTail, iAttributeHead, Operation.OPERATION_ADD);
                        if (m_Cache.get(oOperation) > fBestDeltaScore) {
							if (isNotTabu(oOperation)) {
                        	oBestOperation = oOperation;
                        	fBestDeltaScore = m_Cache.get(oOperation);
							}
                        }
                    }
                }
            }
        }

		// find best arc to delete
		for (int iNode = 0; iNode < nNrOfAtts; iNode++) {
			ParentSet parentSet = bayesNet.getParentSet(iNode);
			for (int iParent = 0; iParent < parentSet.getNrOfParents(); iParent++) {
				Operation oOperation = new Operation(parentSet.getParent(iParent), iNode, Operation.OPERATION_DEL);
				if (m_Cache.get(oOperation) > fBestDeltaScore) {
					if (isNotTabu(oOperation)) {
					oBestOperation = oOperation;
					fBestDeltaScore = m_Cache.get(oOperation);
					}
				}
			}
		}
		
		// sanity check
		if (oBestOperation == null) {
			throw new Exception("Panic: could not find any step to make. Tabu list too long?");
		}
		// perform operation
		ParentSet bestParentSet = bayesNet.getParentSet(oBestOperation.m_nHead);
		if (oBestOperation.m_nOperation == Operation.OPERATION_ADD) {
			bestParentSet .addParent(oBestOperation.m_nTail, instances);
			if (bayesNet.getDebug()) {
				System.out.print("Add " + oBestOperation.m_nTail + " -> " + oBestOperation.m_nHead);
			}
		} else {
			bestParentSet .deleteParent(oBestOperation.m_nTail, instances);
			if (bayesNet.getDebug()) {
				System.out.print("Del " + oBestOperation.m_nTail + " -> " + oBestOperation.m_nHead);
			}
		}
		m_fCurrentScore += fBestDeltaScore;
		if (m_fCurrentScore > m_fBestScore) {
			m_fBestScore = m_fCurrentScore;
			copyParentSets(m_bestBayesNet, bayesNet);
		}
		if (bayesNet.getDebug()) {
			printTabuList();
		}
		updateCache(oBestOperation.m_nHead, nNrOfAtts, bestParentSet);
	
        return oBestOperation;
    } // getOptimalOperation

	void printTabuList() {
		for (int i = 0; i < m_nTabuList; i++) {
			Operation o = m_oTabuList[i];
			if (o != null) {
				if (o.m_nOperation == 0) {System.out.print(" +(");} else {System.out.print(" -(");}
				System.out.print(o.m_nTail + "->" + o.m_nHead + ")");
			}
		}
	} // printTabuList

	// update the cache
	void updateCache(int iAttributeHead, int nNrOfAtts, ParentSet parentSet) {
		// update cache entries for arrows heading towards iAttributeHead
		double fBaseScore = calcNodeScore(iAttributeHead);
		int nNrOfParents = parentSet.getNrOfParents();
		for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
			if (iAttributeTail != iAttributeHead) {
				if (!parentSet.contains(iAttributeTail)) {
					// add entries to cache for adding arcs
					if (nNrOfParents < m_nMaxNrOfParents) {
						Operation oOperation = new Operation(iAttributeTail, iAttributeHead, Operation.OPERATION_ADD);
						m_Cache.put(oOperation, calcScoreWithExtraParent(iAttributeHead, iAttributeTail) - fBaseScore);
					}
				} else {
					// add entries to cache for deleting arcs
					Operation oOperation = new Operation(iAttributeTail, iAttributeHead, Operation.OPERATION_DEL);
					m_Cache.put(oOperation, calcScoreWithMissingParent(iAttributeHead, iAttributeTail) - fBaseScore);
				}
			}
		}
	} // updateCache
	
    /**
    * @return number of runs
    */
    public int getRuns() {
        return m_nRuns;
    } // getRuns

    /**
     * Sets the number of runs
     * @param nRuns The number of runs to set
     */
    public void setRuns(int nRuns) {
        m_nRuns = nRuns;
    } // setRuns

    /**
     * @return the Tabu List length
     */
    public int getTabuList() {
        return m_nTabuList;
    } // getTabuList

    /**
     * Sets the Tabu List length.
     * @param nTabuList The nTabuList to set
     */
    public void setTabuList(int nTabuList) {
        m_nTabuList = nTabuList;
    } // setTabuList

	/**
	 * Method declaration
	 *
	 * @param nMaxNrOfParents
	 *
	 */
	public void setMaxNrOfParents(int nMaxNrOfParents) {
	  m_nMaxNrOfParents = nMaxNrOfParents;
	} 

	/**
	 * Method declaration
	 *
	 * @return
	 *
	 */
	public int getMaxNrOfParents() {
	  return m_nMaxNrOfParents;
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
		newVector.addElement(new Option("\tMaximum number of parents\n", "P", 1, "-P <nr of parents>"));

		return newVector.elements();
	} // listOptions

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
		String sMaxNrOfParents = Utils.getOption('P', options);
		if (sMaxNrOfParents.length() != 0) {
		  setMaxNrOfParents(Integer.parseInt(sMaxNrOfParents));
		} else {
		  setMaxNrOfParents(100000);
		}
		
		super.setOptions(options);
	} // setOptions

	/**
	 * Gets the current settings of the search algorithm.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions() {
		String[] superOptions = super.getOptions();
		String[] options = new String[6 + superOptions.length];
		int current = 0;
		options[current++] = "-L";
		options[current++] = "" + getTabuList();

		options[current++] = "-U";
		options[current++] = "" + getRuns();

		if (m_nMaxNrOfParents != 10000) {
		  options[current++] = "-P";
		  options[current++] = "" + m_nMaxNrOfParents;
		} 

		// insert options from parent class
		for (int iOption = 0; iOption < superOptions.length; iOption++) {
			options[current++] = superOptions[iOption];
		}

		// Fill up rest with empty strings, not nulls!
		while (current < options.length) {
			options[current++] = "";
		}
		return options;
	} // getOptions

} // SimulatedAnnealing
