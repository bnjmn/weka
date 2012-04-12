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
 *    BayesNetK2.java
 *    Copyright (C) 2001 Remco Bouckaert
 *
 */

package weka.classifiers.bayes;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.estimators.*;
import weka.classifiers.*;

import java.util.Random;
/**
 * Class for a Bayes Network classifier based on K2 for learning structure.
 * K2 is a hill climbing algorihtm by Greg Cooper and Ed Herskovitz,
 * Proceedings Uncertainty in Artificial Intelligence, 1991, Also in
 * Machine Learning, 1992 pages 309-347.
 * Works with nominal variables and no missing values only.
 *
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.1 $
 */
public class BayesNetK2 extends BayesNet {
	/** Holds flag to indicate ordering should be random **/
	boolean m_bRandomOrder = true;
	/** 
	* Set random order flag 
	*
	* @param bRandomOrder
	**/
	public void UseRandomOrder(boolean bRandomOrder) {
		m_bRandomOrder = bRandomOrder;
	} // SetRandomOrder

	/**
	* buildStructure determines the network structure/graph of the network
	* with the K2 algorithm, restricted by its initial structure (which can
	* be an empty graph, or a Naive Bayes graph.
	*/
	public void buildStructure() throws Exception {
		
		if (m_bRandomOrder) {
			// generate random ordering (if required)
		    Random random = new Random();
                    int iClass;
                    if (m_bInitAsNaiveBayes) {
                        iClass = 0; 
                    } else {
                        iClass = -1;
                    }
		    for (int iOrder = 0; iOrder < m_Instances.numAttributes(); iOrder++) {
			int iOrder2 = Math.abs(random.nextInt()) % m_Instances.numAttributes();
                        if (iOrder != iClass && iOrder2 != iClass) {
                            int nTmp = m_nOrder[iOrder];
                            m_nOrder[iOrder] = m_nOrder[iOrder2];
                            m_nOrder[iOrder2] = nTmp;
                        }
		    }
		}

		// determine base scores
		double [] fBaseScores = new double [m_Instances.numAttributes()];
		for (int iOrder = 0; iOrder < m_Instances.numAttributes(); iOrder++) {
			int iAttribute = m_nOrder[iOrder];
			fBaseScores[iAttribute] = CalcNodeScore(iAttribute);
		}

		// K2 algorithm: greedy search restricted by ordering 
		for (int iOrder = 1; iOrder < m_Instances.numAttributes(); iOrder++) {
			int iAttribute = m_nOrder[iOrder];
			double fBestScore = fBaseScores[iAttribute];

			boolean bProgress = (m_ParentSets[iAttribute].GetNrOfParents() < m_nMaxNrOfParents);
			while (bProgress) {
				int nBestAttribute = -1;
				for (int iOrder2 = 0; iOrder2 < iOrder; iOrder2++) {
					int iAttribute2 = m_nOrder[iOrder2];
					double fScore = CalcScoreWithExtraParent(iAttribute, iAttribute2);
					if (fScore > fBestScore) {
						fBestScore = fScore;
						nBestAttribute = iAttribute2;
					}
				}
				if (nBestAttribute != -1) {
					m_ParentSets[iAttribute].AddParent(nBestAttribute, m_Instances);
					fBaseScores[iAttribute] = fBestScore;
                                        bProgress = (m_ParentSets[iAttribute].GetNrOfParents() < m_nMaxNrOfParents);
				} else {
					bProgress = false;
				}
			}
		}

	} // buildStructure


  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new BayesNetK2(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  } // main

} // class BayesNetK2

