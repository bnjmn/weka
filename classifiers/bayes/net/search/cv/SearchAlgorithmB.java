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
 * SearchAlgorithmB.java
 * Copyright (C) 2003 Remco Bouckaert
 * 
 */
package weka.classifiers.bayes.net.search.cv;

import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.bayes.BayesNet;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.Option;

/**
 * Class for a Bayes Network classifier based on a hill climbing algorithm for
 * learning structure as described in Buntine, W. (1991). Theory refinement on
 * Bayesian networks. In Proceedings of Seventh Conference on Uncertainty in
 * Artificial Intelligence, Los Angeles, CA, pages 52--60. Morgan Kaufmann.
 * Works with nominal variables and no missing values only.
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.3 $
 */
public class SearchAlgorithmB extends CVSearchAlgorithm {

    /**
     * buildStructure determines the network structure/graph of the network
     * with Buntines greedy hill climbing algorithm, restricted by its initial
     * structure (which can be an empty graph, or a Naive Bayes graph.
     */
    public void buildStructure(BayesNet bayesNet, Instances instances) throws Exception {
        super.buildStructure(bayesNet, instances);

        // determine base scores
        double fBaseAccuracy;
        int nNrOfAtts = instances.numAttributes();

        fBaseAccuracy = performCV(bayesNet);

        // B algorithm: greedy search (not restricted by ordering like K2)
        boolean bProgress = true;

        // cache whether adding an arc makes sense
        boolean[][] bAddArcMakesSense = new boolean[nNrOfAtts][nNrOfAtts];
        for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) {
            if (bayesNet.getParentSet(iAttributeHead).getNrOfParents() < getMaxNrOfParents()) {
                // only bother maintaining scores if adding parent does not violate the upper bound on nr of parents
                for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
                    bAddArcMakesSense[iAttributeHead][iAttributeTail] =
                        addArcMakesSense(bayesNet, instances, iAttributeHead, iAttributeTail);

                }
            }
        }
        // go do the hill climbing
        while (bProgress) {
            bProgress = false;

            int nBestAttributeTail = -1;
            int nBestAttributeHead = -1;
            double fBestAccuracy = fBaseAccuracy;

            // find best arc to add
            for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) {
                if (bayesNet.getParentSet(iAttributeHead).getNrOfParents() < getMaxNrOfParents()) {
                    for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
                        if (bAddArcMakesSense[iAttributeHead][iAttributeTail]) {
                            double fNewAccuracy = performCVWithExtraParent(iAttributeHead, iAttributeTail);

                            if (fNewAccuracy > fBestAccuracy) {
                                if (addArcMakesSense(bayesNet, instances, iAttributeHead, iAttributeTail)) {
                                    fBestAccuracy = fNewAccuracy;
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
                bayesNet.getParentSet(nBestAttributeHead).addParent(nBestAttributeTail, instances);
                fBaseAccuracy = fBestAccuracy;
                bProgress = true;
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
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    public Enumeration listOptions() {
        Vector newVector = new Vector(0);

        newVector.addElement(new Option("\tInitial structure is empty (instead of Naive Bayes)\n", "N", 0, "-N"));

        newVector.addElement(new Option("\tMaximum number of parents\n", "P", 1, "-P <nr of parents>"));

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
    public String[] getOptions() {
    	String[] superOptions = super.getOptions();
        String[] options = new String[3 + superOptions.length];
        int current = 0;
        if (m_nMaxNrOfParents != 10000) {
            options[current++] = "-P";
            options[current++] = "" + m_nMaxNrOfParents;
        }
        if (!m_bInitAsNaiveBayes) {
            options[current++] = "-N";
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
    }

    /**
     * This will return a string describing the classifier.
     * @return The string.
     */
    public String globalInfo() {
        return "This Bayes Network learning algorithm uses a hill climbing algorithm"
            + " without restriction on the order of variables";
    }

} // class SearchAlgorithmB