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
 * SearchAlgorithmTAN.java
 * Copyright (C) 2004 Remco Bouckaert
 * 
 */
 
 package weka.classifiers.bayes.net.search.score;

import java.util.Vector;
import java.util.Enumeration;

import weka.classifiers.bayes.BayesNet;
import weka.core.Instances;

/** Search for TAN = Tree Augmented Naive Bayes network structure
 * 	N. Friedman, D. Geiger, M. Goldszmidt. 
 *	Bayesian Network Classifiers. 
 *	Machine Learning, 29: 131--163, 1997
 * 
 * @author Remco Bouckaert
 * @version $Revision: 1.1 $
 */
public class SearchAlgorithmTAN extends ScoreSearchAlgorithm {

	/**
	 * buildStructure determines the network structure/graph of the network
	 * with Buntines greedy hill climbing algorithm, restricted by its initial
	 * structure (which can be an empty graph, or a Naive Bayes graph.
	 */
	public void buildStructure(BayesNet bayesNet, Instances instances) throws Exception {
		super.buildStructure(bayesNet, instances);
		m_bInitAsNaiveBayes = true;
		m_nMaxNrOfParents = 2;
		super.buildStructure(bayesNet, instances);

		// determine base scores
		double[] fBaseScores = new double[instances.numAttributes()];
		int      nNrOfAtts = instances.numAttributes();

		for (int iAttribute = 0; iAttribute < nNrOfAtts; iAttribute++) {
		  fBaseScores[iAttribute] = CalcNodeScore(iAttribute);
		} 

		// TAN greedy search (not restricted by ordering like K2)

		// cache scores & whether adding an arc makes sense
		boolean[][] bAddArcMakesSense = new boolean[nNrOfAtts][nNrOfAtts];
		double[][] fScore = new double[nNrOfAtts][nNrOfAtts];

		for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; 
		 iAttributeHead++) {
		  if (bayesNet.getParentSet(iAttributeHead).GetNrOfParents() < m_nMaxNrOfParents) {

		// only bother maintaining scores if adding parent makes sense
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

		// go do the hill climbing
	  for (int iArc = 0; iArc < nNrOfAtts - 1; iArc++) {
		  int    nBestAttributeTail = -1;
		  int    nBestAttributeHead = -1;
		  double fBestDeltaScore = 0.0;

		  // find best arc to add
		  for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) {
		if (bayesNet.getParentSet(iAttributeHead).GetNrOfParents() < m_nMaxNrOfParents) {
		  for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
			if (bAddArcMakesSense[iAttributeHead][iAttributeTail]) {
			  if ((nBestAttributeTail == -1) || 
			  (fScore[iAttributeHead][iAttributeTail] - fBaseScores[iAttributeHead] > fBestDeltaScore)) {
			if (AddArcMakesSense(bayesNet, instances, iAttributeHead, iAttributeTail)) {
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
		bayesNet.getParentSet(nBestAttributeHead).AddParent(nBestAttributeTail, 
							   instances);

		  } 
		} 
	} // buildStructure


	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	public Enumeration listOptions() {
	  return new Vector(1).elements();
	} // listOption

	/**
	 * Parses a given list of options. Valid options are:<p>
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
	} // setOptions
	
	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String [] getOptions() {
		return new String[0];
	} // getOptions

	/**
	 * This will return a string describing the classifier.
	 * @return The string.
	 */
	public String globalInfo() {
	  return "This Bayes Network learning algorithm uses a hill climbing algorithm" +
	  " and returns a Naive Bayes network augmented with a tree.";
	}

} // SearchAlgorithmTAN
