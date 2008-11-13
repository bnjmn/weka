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

package weka.classifiers.bayes.net.estimate;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.OptionHandler;
import weka.classifiers.bayes.BayesNet;
import weka.core.Instance;
import weka.core.Option;
import weka.core.Utils;

/** BayesNetEstimator is the base class for estimating the conditional probability
 * tables of a Bayes network once the structure has been learned.
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.1 $
 */

public class BayesNetEstimator implements OptionHandler, Serializable {
    
    /**
     * Holds prior on count
     */
    protected double m_fAlpha = 0.5;

    /**
     * estimateCPTs estimates the conditional probability tables for the Bayes
     * Net using the network structure.
     */
    public void estimateCPTs(BayesNet bayesNet) throws Exception {
        throw new Exception("Incorrect BayesNetEstimator: use subclass instead.");
    }

    /**
     * Updates the classifier with the given instance.
     * 
     * @param instance the new training instance to include in the model
     * @exception Exception if the instance could not be incorporated in
     * the model.
     */
    public void updateClassifier(BayesNet bayesNet, Instance instance) throws Exception {
        throw new Exception("Incorrect BayesNetEstimator: use subclass instead.");
    }

    /**
     * Calculates the class membership probabilities for the given test
     * instance.
     * 
     * @param instance the instance to be classified
     * @return predicted class probability distribution
     * @exception Exception if there is a problem generating the prediction
     */
    public double[] distributionForInstance(BayesNet bayesNet, Instance instance) throws Exception {
        throw new Exception("Incorrect BayesNetEstimator: use subclass instead.");
    }

    /** initCPTs reserves space for CPTs and set all counts to zero
     */
    public void initCPTs(BayesNet bayesNet) throws Exception {
        throw new Exception("Incorrect BayesNetEstimator: use subclass instead.");
    } // initCPTs

    /**
     * Returns an enumeration describing the available options
     * 
     * @return an enumeration of all the available options
     */
    public Enumeration listOptions() {
        Vector newVector = new Vector(1);

        newVector.addElement(new Option("\tInitial count (alpha)\n", "A", 1, "-A <alpha>"));

        return newVector.elements();
    } // listOptions

    /**
     * Parses a given list of options. Valid options are:<p>
     * 
     * @param options the list of options as an array of strings
     * @exception Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception {
        String sAlpha = Utils.getOption('A', options);

        if (sAlpha.length() != 0) {
            m_fAlpha = (new Float(sAlpha)).floatValue();
        } else {
            m_fAlpha = 0.5f;
        }

        Utils.checkForRemainingOptions(options);
    } // setOptions

    /**
     * Gets the current settings of the classifier.
     * 
     * @return an array of strings suitable for passing to setOptions
     */
    public String[] getOptions() {
        String[] options = new String[2];
        int current = 0;

        options[current++] = "-A";
        options[current++] = "" + m_fAlpha;

        return options;
    } // getOptions

    /**
     * Set prior used in probability table estimation
     * @param fAlpha representing prior
     */
    public void setAlpha(double fAlpha) {
        m_fAlpha = fAlpha;
    }

    /**
     * Get prior used in probability table estimation
     * @return prior
     */
    public double getAlpha() {
        return m_fAlpha;
    }


    /**
     * @return a string to describe the Alpha option.
     */
    public String alphaTipText() {
        return "Alpha is used for estimating the probability tables and can be interpreted"
            + " as the initial count on each value.";
    }

    /**
     * This will return a string describing the class.
     * @return The string.
     */
    public String globalInfo() {
        return "An Estimator is an algorithm for finding the conditional probability tables"
            + " of the Bayes Network.";
    }

} // BayesNetEstimator
