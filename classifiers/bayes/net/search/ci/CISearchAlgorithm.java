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
 * CISearchAlgorithm.java
 * Copyright (C) 2004 Remco Bouckaert
 * 
 */

package weka.classifiers.bayes.net.search.ci;

import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.ParentSet;
import weka.classifiers.bayes.net.search.local.*;
import weka.core.Instances;

/** The CISearchAlgorithm class supports Bayes net structure search algorithms
 * that are based on conditional independence test (as opposed to for example
 * score based of cross validation based search algorithms).
 *
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.4 $
 */
public class CISearchAlgorithm extends LocalScoreSearchAlgorithm {
	BayesNet  m_BayesNet;
	Instances m_instances;

	/** IsConditionalIndependent tests whether two nodes X and Y are independent
	 *  given a set of variables Z. The test compares the score of the Bayes network
	 * with and without arrow Y->X where all nodes in Z are parents of X.
	 * @param iAttributeX - index of attribute representing variable X
	 * @param iAttributeY - index of attribute representing variable Y
 	 * @param iAttributesZ - array of integers representing indices of attributes in set Z
 	 * @param nAttributesZ - cardinality of Z
 	 * @return true if X and Y conditionally independent given Z 
	 */
	protected boolean isConditionalIndependent(
		int iAttributeX, 
		int iAttributeY, 
		int [] iAttributesZ, 
		int nAttributesZ) {
		ParentSet oParentSetX = m_BayesNet.getParentSet(iAttributeX);
		// clear parent set of AttributeX
		while (oParentSetX.getNrOfParents() > 0) {
			oParentSetX.deleteLastParent(m_instances);
		}
		
		// insert parents in iAttributeZ
		for (int iAttributeZ = 0; iAttributeZ < nAttributesZ; iAttributeZ++) {
			oParentSetX.addParent( iAttributesZ[iAttributeZ], m_instances);
		}
		
		double fScoreZ = calcNodeScore(iAttributeX);
		double fScoreZY = calcScoreWithExtraParent(iAttributeX, iAttributeY);
		if (fScoreZY <= fScoreZ) {
			// the score does not improve by adding Y to the parent set of X
			// so we conclude that nodes X and Y are conditionally independent
			// given the set of variables Z
			return true;
		}
		return false;
	} // IsConditionalIndependent
	        	
} // class CISearchAlgorithm

