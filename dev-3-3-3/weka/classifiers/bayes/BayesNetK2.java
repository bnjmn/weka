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
 * @version $Revision: 1.2 $
 */
public class BayesNetK2 extends BayesNet {
	/** Holds flag to indicate ordering should be random **/
	boolean m_bRandomOrder = false;

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

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
	      "\tRandom order.\n"
	      + "\t(default false)",
	      "R", 0, "-R"));

      Enumeration enum = super.listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -R
   * Set the random order to true (default false). <p>
   *
   * For other options see bagging.
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    setRandomOrder(Utils.getFlag('R', options));
    super.setOptions(options);
  }
  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  
  public String [] getOptions() {

    String [] classifierOptions;
    classifierOptions = super.getOptions();
    String [] options = new String [classifierOptions.length + 1];
    int current = 0;
    if (getRandomOrder()) {
      options[current++] = "-R";
    }
    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
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

  /**
   * This will return a string describing the classifier.
   * @return The string.
   */
  public String globalInfo() {
    return "This Bayes Network learning algorithm uses a hill climbing algorithm" +
    " restricted by an order on the variables";
  }
  
  
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

