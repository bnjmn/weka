
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
 * BayesNetB2.java
 * Copyright (C) 2001 Remco Bouckaert
 * 
 */
package weka.classifiers.bayes.net.search.score;

import weka.classifiers.bayes.BayesNet;
import weka.core.Instances;


/**
 * Class for a Bayes Network classifier based on Buntines hill climbing algorithm for
 * learning structure, but augmented to allow arc reversal as an operation.
 * Works with nominal variables only.
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.2 $
 */
public class SearchAlgorithmB2 extends SearchAlgorithmB {

	/**
	 * buildStructure determines the network structure/graph of the network
	 * with Buntines greedy hill climbing algorithm, restricted by its initial
	 * structure (which can be an empty graph, or a Naive Bayes graph.
	 */
	public void buildStructure(BayesNet bayesNet, Instances instances) throws Exception {
  	  super.buildStructure(bayesNet, instances);

	  // determine base scores
	  double[] fBaseScores = new double[instances.numAttributes()];
	  int      nNrOfAtts = instances.numAttributes();

	  for (int iAttribute = 0; iAttribute < nNrOfAtts; iAttribute++) {
		fBaseScores[iAttribute] = calcNodeScore(iAttribute);
	  } 

	  // Determine initial structure by finding a good parent-set for classification
	  // node using greedy search
	  int     iAttribute = instances.classIndex();
	  double  fBestScore = fBaseScores[iAttribute];

	  // /////////////////////////////////////////////////////////////////////////////////////////
	  int     m_nMaxNrOfClassifierParents = 4;

	  // /////////////////////////////////////////////////////////////////////////////////////////
	  // double fBestScore = CalcNodeScore(iAttribute);
	  boolean bProgress = true;

	  while (bProgress 
		 && bayesNet.getParentSet(iAttribute).getNrOfParents() 
			< m_nMaxNrOfClassifierParents) {
		int nBestAttribute = -1;

		for (int iAttribute2 = 0; iAttribute2 < instances.numAttributes(); 
		 iAttribute2++) {
	  if (iAttribute != iAttribute2) {
		double fScore = calcScoreWithExtraParent(iAttribute, iAttribute2);

		if (fScore > fBestScore) {
		  fBestScore = fScore;
		  nBestAttribute = iAttribute2;
		} 
	  } 
		} 

		if (nBestAttribute != -1) {
	  bayesNet.getParentSet(iAttribute).addParent(nBestAttribute, instances);

	  fBaseScores[iAttribute] = fBestScore;
		} else {
	  bProgress = false;
		} 
	  } 

	  // Recalc Base scores
	  // Correction for Naive Bayes structures: delete arcs from classification node to children
	  for (int iParent = 0; 
	   iParent < bayesNet.getParentSet(iAttribute).getNrOfParents(); iParent++) {
		int nParentNode = bayesNet.getParentSet(iAttribute).getParent(iParent);

		if (isArc(bayesNet, nParentNode, iAttribute)) {
	  bayesNet.getParentSet(nParentNode).deleteLastParent(instances);
		} 

		// recalc base scores
		fBaseScores[nParentNode] = calcNodeScore(nParentNode);
	  } 

	  // super.buildStructure();
	  // Do algorithm B from here onwards
	  // cache scores & whether adding an arc makes sense
	  boolean[][] bAddArcMakesSense = new boolean[nNrOfAtts][nNrOfAtts];
	  double[][]  fScore = new double[nNrOfAtts][nNrOfAtts];

	  for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; 
	   iAttributeHead++) {
		if (bayesNet.getParentSet(iAttributeHead).getNrOfParents() < m_nMaxNrOfParents) {

	  // only bother maintaining scores if adding parent does not violate the upper bound on nr of parents
	  for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; 
		   iAttributeTail++) {
		bAddArcMakesSense[iAttributeHead][iAttributeTail] = 
		  addArcMakesSense(bayesNet, instances, iAttributeHead, iAttributeTail);

		if (bAddArcMakesSense[iAttributeHead][iAttributeTail]) {
		  fScore[iAttributeHead][iAttributeTail] = 
			calcScoreWithExtraParent(iAttributeHead, iAttributeTail);
		} 
	  } 
		} 
	  } 

	  bProgress = true;

	  // go do the hill climbing
	  while (bProgress) {
		bProgress = false;

		int    nBestAttributeTail = -1;
		int    nBestAttributeHead = -1;
		double fBestDeltaScore = 0.0;

		// find best arc to add
		for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; 
		 iAttributeHead++) {
	  if (bayesNet.getParentSet(iAttributeHead).getNrOfParents() 
		  < m_nMaxNrOfParents) {
		for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; 
			 iAttributeTail++) {
		  if (bAddArcMakesSense[iAttributeHead][iAttributeTail]) {

			// System.out.println("gain " +  iAttributeTail + " -> " + iAttributeHead + ": "+ (fScore[iAttributeHead][iAttributeTail] - fBaseScores[iAttributeHead]));
			if (fScore[iAttributeHead][iAttributeTail] 
				- fBaseScores[iAttributeHead] > fBestDeltaScore) {
		  if (addArcMakesSense(bayesNet, instances, iAttributeHead, iAttributeTail)) {
			fBestDeltaScore = fScore[iAttributeHead][iAttributeTail] 
					  - fBaseScores[iAttributeHead];
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
	  // System.out.println("Added " + nBestAttributeTail + " -> " + nBestAttributeHead);
	  bayesNet.getParentSet(nBestAttributeHead).addParent(nBestAttributeTail, 
							 instances);

	  if (bayesNet.getParentSet(nBestAttributeHead).getNrOfParents() 
		  < m_nMaxNrOfParents) {

		// only bother updating scores if adding parent does not violate the upper bound on nr of parents
		fBaseScores[nBestAttributeHead] += fBestDeltaScore;

		// System.out.println(fScore[nBestAttributeHead][nBestAttributeTail] + " " + fBaseScores[nBestAttributeHead] + " " + fBestDeltaScore);
		for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; 
			 iAttributeTail++) {
		  bAddArcMakesSense[nBestAttributeHead][iAttributeTail] = 
			addArcMakesSense(bayesNet, instances, nBestAttributeHead, iAttributeTail);

		  if (bAddArcMakesSense[nBestAttributeHead][iAttributeTail]) {
			fScore[nBestAttributeHead][iAttributeTail] = 
			calcScoreWithExtraParent(nBestAttributeHead, iAttributeTail);

			// System.out.println(iAttributeTail + " -> " + nBestAttributeHead + ": " + fScore[nBestAttributeHead][iAttributeTail]);
		  } 
		} 
	  } 

	  bProgress = true;
		} 
	  } 
	}    // buildStructure
 
 
 

	/**
	 * This will return a string describing the classifier.
	 * @return The string.
	 */
	public String globalInfo() {
	  return "This Bayes Network learning algorithm uses a hill climbing algorithm" +
	  " without restriction on the order of variables and it looks at arc reversals" +
	  " as well as adding arcs.";
	}

} // class SearchAlgorithmB2
