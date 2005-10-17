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
 * SearchAlgorithmK2.java
 * Copyright (C) 2001 Remco Bouckaert
 * 
 */
package weka.classifiers.bayes.net.search.local;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Random;

import weka.classifiers.bayes.BayesNet;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;


/**
 * Class for a Bayes Network classifier based on K2 for learning structure.
 * K2 is a hill climbing algorihtm by Greg Cooper and Ed Herskovitz,
 * Proceedings Uncertainty in Artificial Intelligence, 1991, Also in
 * Machine Learning, 1992 pages 309-347.
 * Works with nominal variables and no missing values only.
 *
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.2.2.1 $
 */
public class K2 extends LocalScoreSearchAlgorithm {
	/** Holds flag to indicate ordering should be random **/
	boolean m_bRandomOrder = false;

	/**
	* buildStructure determines the network structure/graph of the network
	* with the K2 algorithm, restricted by its initial structure (which can
	* be an empty graph, or a Naive Bayes graph.
	*/
	public void buildStructure (BayesNet bayesNet, Instances instances) throws Exception {
		super.buildStructure(bayesNet, instances);
		int nOrder[] = new int [instances.numAttributes()];
		nOrder[0] = instances.classIndex();

		int nAttribute = 0;

		for (int iOrder = 1; iOrder < instances.numAttributes(); iOrder++) {
		  if (nAttribute == instances.classIndex()) {
		    nAttribute++;
		  } 
		  nOrder[iOrder] = nAttribute++;
		} 

		if (m_bRandomOrder) {
			// generate random ordering (if required)
			Random random = new Random();
					int iClass;
					if (getInitAsNaiveBayes()) {
						iClass = 0; 
					} else {
						iClass = -1;
					}
			for (int iOrder = 0; iOrder < instances.numAttributes(); iOrder++) {
			int iOrder2 = Math.abs(random.nextInt()) % instances.numAttributes();
						if (iOrder != iClass && iOrder2 != iClass) {
							int nTmp = nOrder[iOrder];
							nOrder[iOrder] = nOrder[iOrder2];
							nOrder[iOrder2] = nTmp;
						}
			}
		}

		// determine base scores
		double [] fBaseScores = new double [instances.numAttributes()];
		for (int iOrder = 0; iOrder < instances.numAttributes(); iOrder++) {
			int iAttribute = nOrder[iOrder];
			fBaseScores[iAttribute] = calcNodeScore(iAttribute);
		}

		// K2 algorithm: greedy search restricted by ordering 
		for (int iOrder = 1; iOrder < instances.numAttributes(); iOrder++) {
			int iAttribute = nOrder[iOrder];
			double fBestScore = fBaseScores[iAttribute];

			boolean bProgress = (bayesNet.getParentSet(iAttribute).getNrOfParents() < getMaxNrOfParents());
			while (bProgress) {
				int nBestAttribute = -1;
				for (int iOrder2 = 0; iOrder2 < iOrder; iOrder2++) {
					int iAttribute2 = nOrder[iOrder2];
					double fScore = calcScoreWithExtraParent(iAttribute, iAttribute2);
					if (fScore > fBestScore) {
						fBestScore = fScore;
						nBestAttribute = iAttribute2;
					}
				}
				if (nBestAttribute != -1) {
					bayesNet.getParentSet(iAttribute).addParent(nBestAttribute, instances);
					fBaseScores[iAttribute] = fBestScore;
					bProgress = (bayesNet.getParentSet(iAttribute).getNrOfParents() < getMaxNrOfParents());
				} else {
					bProgress = false;
				}
			}
		}
	} // buildStructure 

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
	 * Method declaration
	 *
	 * @param bInitAsNaiveBayes
	 *
	 */
	public void setInitAsNaiveBayes(boolean bInitAsNaiveBayes) {
	  m_bInitAsNaiveBayes = bInitAsNaiveBayes;
	} 

	/**
	 * Method declaration
	 *
	 * @return
	 *
	 */
	public boolean getInitAsNaiveBayes() {
	  return m_bInitAsNaiveBayes;
	} 

	/** 
	* Set random order flag 
	*
	* @param bRandomOrder
	**/
	public void setRandomOrder(boolean bRandomOrder) {
		m_bRandomOrder = bRandomOrder;
	} // SetRandomOrder

	/** 
	* Get random order flag 
	*
	* @returns m_bRandomOrder
	**/
	public boolean getRandomOrder() {
		return m_bRandomOrder;
	} // getRandomOrder
  
	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	public Enumeration listOptions() {
	  Vector newVector = new Vector(0);

	  newVector.addElement(new Option("\tInitial structure is empty (instead of Naive Bayes)\n", 
					 "N", 0, "-N"));

	  newVector.addElement(new Option("\tMaximum number of parents\n", "P", 1, 
						"-P <nr of parents>"));

	  newVector.addElement(new Option(
			"\tRandom order.\n"
			+ "\t(default false)",
			"R", 0, "-R"));

		Enumeration enu = super.listOptions();
		while (enu.hasMoreElements()) {
	    	newVector.addElement(enu.nextElement());
		}
	  return newVector.elements();
	}

	/**
	 * Parses a given list of options. Valid options are:<p>
	 *
	 * -R
	 * Set the random order to true (default false). <p>
	 *
	 * For other options see search algorithm.
	 *
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
    
	  setRandomOrder(Utils.getFlag('R', options));

	  m_bInitAsNaiveBayes = !(Utils.getFlag('N', options));

	  String sMaxNrOfParents = Utils.getOption('P', options);

	  if (sMaxNrOfParents.length() != 0) {
		setMaxNrOfParents(Integer.parseInt(sMaxNrOfParents));
	  } else {
		setMaxNrOfParents(100000);
	  }
	  super.setOptions(options);
	}

	/**
	 * Gets the current settings of the search algorithm.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String [] getOptions() {
          String[] superOptions = super.getOptions();
	  String [] options  = new String [4 + superOptions.length];
	  int current = 0;
          options[current++] = "-P";
          options[current++] = "" + m_nMaxNrOfParents;
	  if (!m_bInitAsNaiveBayes) {
		options[current++] = "-N";
	  }	  if (getRandomOrder()) {
		options[current++] = "-R";
	  }

          // insert options from parent class
          for (int iOption = 0; iOption < superOptions.length; iOption++) {
                  options[current++] = superOptions[iOption];
          }

	  while (current < options.length) {
		options[current++] = "";
	  }
	  // Fill up rest with empty strings, not nulls!
	  return options;
	}

	/**
	 * This will return a string describing the search algorithm.
	 * @return The string.
	 */
	public String globalInfo() {
	  return "This Bayes Network learning algorithm uses a hill climbing algorithm" +
	  " restricted by an order on the variables";
	}

	/**
	 * @return a string to describe the RandomOrder option.
	 */
	public String randomOrderTipText() {
	  return "When set to true, the order of the nodes in the network is random." +
	  " Default random order is false and the order" +
	  " of the nodes in the dataset is used." +
	  " In any case, when the network was initialized as Naive Bayes Network, the" +
	  " class variable is first in the ordering though.";
	} // randomOrderTipText

  
} // class SearchAlgorithmK2 
