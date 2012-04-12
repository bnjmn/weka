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
 * BayesNet.java
 * Copyright (C) 2004 Remco Bouckaert
 * 
 */

package weka.classifiers.bayes.net.search.local;

import java.util.Enumeration;

import weka.classifiers.bayes.BayesNet;
import weka.core.Instances;

/** Search for TAN = Tree Augmented Naive Bayes network structure
 *      N. Friedman, D. Geiger, M. Goldszmidt.
 *      Bayesian Network Classifiers.
 *      Machine Learning, 29: 131--163, 1997
 *
 * @author Remco Bouckaert
 * @version $Revision: 1.3 $
 */

public class TAN extends LocalScoreSearchAlgorithm {

	/**
	 * buildStructure determines the network structure/graph of the network
	 * using the maximimum weight spanning tree algorithm of Chow and Liu
	 * 
	 */
	public void buildStructure(BayesNet bayesNet, Instances instances) throws Exception {

		m_bInitAsNaiveBayes = true;
		m_nMaxNrOfParents = 2;
		super.buildStructure(bayesNet, instances);
		int      nNrOfAtts = instances.numAttributes();

		// determine base scores
		double[] fBaseScores = new double[instances.numAttributes()];

		for (int iAttribute = 0; iAttribute < nNrOfAtts; iAttribute++) {
		  fBaseScores[iAttribute] = calcNodeScore(iAttribute);
		} 

		//		// cache scores & whether adding an arc makes sense
		double[][]  fScore = new double[nNrOfAtts][nNrOfAtts];

		for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) {
			for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
				if (iAttributeHead != iAttributeTail) {
					fScore[iAttributeHead][iAttributeTail] = calcScoreWithExtraParent(iAttributeHead, iAttributeTail);
				}
			} 
		}
		
		// TAN greedy search (not restricted by ordering like K2)
		// 1. find strongest link
		// 2. find remaining links by adding strongest link to already
		//    connected nodes
		// 3. assign direction to links
		int nClassNode = instances.classIndex();
		int [] link1 = new int [nNrOfAtts - 1];
		int [] link2 = new int [nNrOfAtts - 1];
		boolean [] linked = new boolean [nNrOfAtts];

		// 1. find strongest link
		int    nBestLinkNode1 = -1;
		int    nBestLinkNode2 = -1;
		double fBestDeltaScore = 0.0;
		int iLinkNode1;
		for (iLinkNode1 = 0; iLinkNode1 < nNrOfAtts; iLinkNode1++) {
			if (iLinkNode1 != nClassNode) {
			for (int iLinkNode2 = 0; iLinkNode2 < nNrOfAtts; iLinkNode2++) {
				if ((iLinkNode1 != iLinkNode2) &&
				    (iLinkNode2 != nClassNode) && (
				    (nBestLinkNode1 == -1) || (fScore[iLinkNode1][iLinkNode2] - fBaseScores[iLinkNode1] > fBestDeltaScore)
				)) {
					fBestDeltaScore = fScore[iLinkNode1][iLinkNode2] - fBaseScores[iLinkNode1];
					nBestLinkNode1 = iLinkNode2;
					nBestLinkNode2 = iLinkNode1;
			    } 
			} 
			}
		}
		link1[0] = nBestLinkNode1;
		link2[0] = nBestLinkNode2;
		linked[nBestLinkNode1] = true;
		linked[nBestLinkNode2] = true;
	
		// 2. find remaining links by adding strongest link to already
		//    connected nodes
		for (int iLink = 1; iLink < nNrOfAtts - 2; iLink++) {
			nBestLinkNode1 = -1;
			for (iLinkNode1 = 0; iLinkNode1 < nNrOfAtts; iLinkNode1++) {
				if (iLinkNode1 != nClassNode) {
				for (int iLinkNode2 = 0; iLinkNode2 < nNrOfAtts; iLinkNode2++) {
					if ((iLinkNode1 != iLinkNode2) &&
					    (iLinkNode2 != nClassNode) && 
					(linked[iLinkNode1] || linked[iLinkNode2]) &&
					(!linked[iLinkNode1] || !linked[iLinkNode2]) &&
					(
					(nBestLinkNode1 == -1) || (fScore[iLinkNode1][iLinkNode2] - fBaseScores[iLinkNode1] > fBestDeltaScore)
					)) {
						fBestDeltaScore = fScore[iLinkNode1][iLinkNode2] - fBaseScores[iLinkNode1];
						nBestLinkNode1 = iLinkNode2;
						nBestLinkNode2 = iLinkNode1;
					} 
				} 
				}
			}

			link1[iLink] = nBestLinkNode1;
			link2[iLink] = nBestLinkNode2;
			linked[nBestLinkNode1] = true;
			linked[nBestLinkNode2] = true;
		}
		
		// 3. assign direction to links
		boolean [] hasParent = new boolean [nNrOfAtts];
		for (int iLink = 0; iLink < nNrOfAtts - 2; iLink++) {
			if (!hasParent[link1[iLink]]) {
				bayesNet.getParentSet(link1[iLink]).addParent(link2[iLink], instances);
				hasParent[link1[iLink]] = true;
			} else {
				if (hasParent[link2[iLink]]) {
					throw new Exception("Bug condition found: too many arrows");
				}
				bayesNet.getParentSet(link2[iLink]).addParent(link1[iLink], instances);
				hasParent[link2[iLink]] = true;
			}
		}

	} // buildStructure


	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	public Enumeration listOptions() {
		return super.listOptions();
	} // listOption

	/**
	 * Parses a given list of options. Valid options are:<p>
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		super.setOptions(options);
	} // setOptions
	
	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String [] getOptions() {
		return super.getOptions();
	} // getOptions

	/**
	 * This will return a string describing the classifier.
	 * @return The string.
	 */
	public String globalInfo() {
		return "This Bayes Network learning algorithm determines the maximum weight spanning tree " +
		" and returns a Naive Bayes network augmented with a tree.";
	} // globalInfo

} // TAN
