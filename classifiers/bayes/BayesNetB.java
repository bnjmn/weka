/*
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

/*
 *    BayesNetB.java
 *    Copyright (C) 2001 Remco Bouckaert
 *
 */

package weka.classifiers.bayes;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.estimators.*;
import weka.classifiers.*;

/**
 * Class for a Bayes Network classifier based on a hill climbing algorithm for 
 * learning structure as described in Buntine, W. (1991). Theory refinement on 
 * Bayesian networks. In Proceedings of Seventh Conference on Uncertainty in 
 * Artificial Intelligence, Los Angeles, CA, pages 52--60. Morgan Kaufmann. 
 * Works with nominal variables and no missing values only.
 *
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.1 $
 */
public class BayesNetB extends BayesNet {


	/**
	* buildStructure determines the network structure/graph of the network
	* with Buntines greedy hill climbing algorithm, restricted by its initial 
	* structure (which can be an empty graph, or a Naive Bayes graph.
	*/
	public void buildStructure() throws Exception {

		// determine base scores
		double [] fBaseScores = new double [m_Instances.numAttributes()];
		int nNrOfAtts = m_Instances.numAttributes();
		for (int iAttribute = 0; iAttribute < nNrOfAtts; iAttribute++) 
		{
			fBaseScores[iAttribute] = CalcNodeScore(iAttribute);
		}
		// B algorithm: greedy search (not restricted by ordering like K2)
		boolean bProgress = true;

		// cache scores & whether adding an arc makes sense
		boolean [][] bAddArcMakesSense = new boolean [nNrOfAtts][nNrOfAtts];
		double [] [] fScore = new double [nNrOfAtts][nNrOfAtts];
		for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) 
		{
			if (m_ParentSets[iAttributeHead].GetNrOfParents() < m_nMaxNrOfParents) {
				// only bother maintaining scores if adding parent does not violate the upper bound on nr of parents
				for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) 
				{
					bAddArcMakesSense[iAttributeHead][iAttributeTail] = AddArcMakesSense(iAttributeHead, iAttributeTail);
					if (bAddArcMakesSense[iAttributeHead][iAttributeTail]) 
					{
							fScore[iAttributeHead][iAttributeTail] = CalcScoreWithExtraParent(iAttributeHead, iAttributeTail);
					}
				}
			}
		}

		// go do the hill climbing
		while (bProgress) {
			bProgress = false;
			int nBestAttributeTail = -1;
			int nBestAttributeHead = -1;
			double fBestDeltaScore = 0.0;
			// find best arc to add
			for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) 
			{
				if (m_ParentSets[iAttributeHead].GetNrOfParents() < m_nMaxNrOfParents) {
					for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) 
					{
						if (bAddArcMakesSense[iAttributeHead][iAttributeTail]) 
						{
							if (fScore[iAttributeHead][iAttributeTail] - fBaseScores[iAttributeHead] > fBestDeltaScore) 
							{
								if (AddArcMakesSense(iAttributeHead, iAttributeTail)) 
								{
									fBestDeltaScore = fScore[iAttributeHead][iAttributeTail] - fBaseScores[iAttributeHead];
									nBestAttributeTail = iAttributeTail;
									nBestAttributeHead = iAttributeHead;
								} else {
									bAddArcMakesSense[iAttributeHead][iAttributeTail] = false;
								}
							}
						}
					}
				}
			}

			if (nBestAttributeHead >= 0) {
				// update network structure
				m_ParentSets[nBestAttributeHead].AddParent(nBestAttributeTail, m_Instances);
				if (m_ParentSets[nBestAttributeHead].GetNrOfParents() < m_nMaxNrOfParents) {
					// only bother updating scores if adding parent does not violate the upper bound on nr of parents
					fBaseScores[nBestAttributeHead] += fBestDeltaScore;
					for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) 
					{
						bAddArcMakesSense[nBestAttributeHead][iAttributeTail] = AddArcMakesSense(nBestAttributeHead, iAttributeTail);
						if (bAddArcMakesSense[nBestAttributeHead][iAttributeTail]) 
						{
								fScore[nBestAttributeHead][iAttributeTail] = CalcScoreWithExtraParent(nBestAttributeHead, iAttributeTail);
						}
					}
				}
				bProgress = true;
			}
		}
	} // buildStructure


	/** 
	* AddArcMakesSense checks whether adding the arc from iAttributeTail to iAttributeHead
	* does not already exists and does not introduce a cycle
	*
	* @param iAttributeHead index of the attribute that becomes head of the arrow
	* @param iAttributeTail index of the attribute that becomes tail of the arrow
        * @return true if adding arc is allowed, otherwise false
	**/
	private boolean AddArcMakesSense(int iAttributeHead, int iAttributeTail)
	{
		if (iAttributeHead == iAttributeTail) {
			return false;
		}
		// sanity check: arc should not be in parent set already
		for (int iParent = 0; iParent < m_ParentSets[iAttributeHead].GetNrOfParents(); iParent++) {
			if (m_ParentSets[iAttributeHead].GetParent(iParent) == iAttributeTail) {
				return false;
			}
		}

		// sanity check: arc should not introduce a cycle
		int nNodes = m_Instances.numAttributes();
		boolean [] bDone = new boolean [nNodes];
		for (int iNode = 0; iNode < nNodes; iNode++) {
			bDone[iNode] = false;
		}

		// check for cycles
		m_ParentSets[iAttributeHead].AddParent(iAttributeTail, m_Instances);
		for (int iNode = 0; iNode < nNodes; iNode++) {
			// find a node for which all parents are 'done'
			boolean bFound = false;
			for (int iNode2 = 0; !bFound && iNode2 < nNodes; iNode2++) {
				if (!bDone[iNode2]) {
					boolean bHasNoParents = true;
					for (int iParent = 0; iParent < m_ParentSets[iNode2].GetNrOfParents(); iParent++) {
						if (!bDone[m_ParentSets[iNode2].GetParent(iParent)]) {
							bHasNoParents = false;
						}
					}
					if (bHasNoParents) {
						bDone[iNode2] = true;
						bFound = true;
					}
				}
			}
			if (!bFound) {
				m_ParentSets[iAttributeHead].DeleteLastParent(m_Instances);
				return false;
			}
		}
		m_ParentSets[iAttributeHead].DeleteLastParent(m_Instances);
		return true;
	} // AddArcMakesCycle

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new BayesNetB(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  } // main

} // class BayesNetB

