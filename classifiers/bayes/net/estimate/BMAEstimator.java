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

import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.search.score.SearchAlgorithmK2;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Statistics;
import weka.estimators.Estimator;

/** BMAEstimator estimates conditional probability tables of a Bayes network using
 * Bayes Model Averaging (BMA).
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 *
 * @version $Revision: 1.1 $
 */

public class BMAEstimator extends SimpleEstimator {

    protected boolean m_bUseK2Prior = false;

    /**
     * estimateCPTs estimates the conditional probability tables for the Bayes
     * Net using the network structure.
     */
    public void estimateCPTs(BayesNet bayesNet) throws Exception {
        Instances instances = bayesNet.m_Instances;
        // sanity check to see if nodes have not more than one parent
        for (int iAttribute = 0; iAttribute < instances.numAttributes(); iAttribute++) {
            if (bayesNet.getParentSet(iAttribute).GetNrOfParents() > 1) {
                throw new Exception("Cannot handle networks with nodes with more than 1 parent (yet).");
            }
        }

        BayesNet EmptyNet = new BayesNet();
        SearchAlgorithmK2 oSearchAlgorithm = new SearchAlgorithmK2();
        oSearchAlgorithm.setInitAsNaiveBayes(false);
        oSearchAlgorithm.setMaxNrOfParents(0);
        EmptyNet.setSearchAlgorithm(oSearchAlgorithm);
        EmptyNet.buildClassifier(instances);

        BayesNet NBNet = new BayesNet();
        oSearchAlgorithm.setInitAsNaiveBayes(true);
        oSearchAlgorithm.setMaxNrOfParents(1);
        NBNet.setSearchAlgorithm(oSearchAlgorithm);
        NBNet.buildClassifier(instances);

        // estimate CPTs
        for (int iAttribute = 0; iAttribute < instances.numAttributes(); iAttribute++) {
            if (iAttribute != instances.classIndex()) {
                  double w1 = 0.0, w2 = 0.0;
                  int nAttValues = instances.attribute(iAttribute).numValues();
                  if (m_bUseK2Prior == true) {
                      // use Cooper and Herskovitz's metric
                      for (int iAttValue = 0; iAttValue < nAttValues; iAttValue++) {
                        w1 += Statistics.lnGamma(1 + ((DiscreteEstimatorBayes)EmptyNet.m_Distributions[iAttribute][0]).getCount(iAttValue))
                              - Statistics.lnGamma(1);
                      }
                      w1 += Statistics.lnGamma(nAttValues) - Statistics.lnGamma(nAttValues + instances.numInstances());

                      for (int iParent = 0; iParent < bayesNet.getParentSet(iAttribute).GetCardinalityOfParents(); iParent++) {
                        int nTotal = 0;
                          for (int iAttValue = 0; iAttValue < nAttValues; iAttValue++) {
                            double nCount = ((DiscreteEstimatorBayes)NBNet.m_Distributions[iAttribute][iParent]).getCount(iAttValue);
                            w2 += Statistics.lnGamma(1 + nCount)
                                  - Statistics.lnGamma(1);
                            nTotal += nCount;
                          }
                        w2 += Statistics.lnGamma(nAttValues) - Statistics.lnGamma(nAttValues + nTotal);
                      }
                  } else {
                      // use BDe metric
                      for (int iAttValue = 0; iAttValue < nAttValues; iAttValue++) {
                        w1 += Statistics.lnGamma(1.0/nAttValues + ((DiscreteEstimatorBayes)EmptyNet.m_Distributions[iAttribute][0]).getCount(iAttValue))
                              - Statistics.lnGamma(1.0/nAttValues);
                      }
                      w1 += Statistics.lnGamma(1) - Statistics.lnGamma(1 + instances.numInstances());

                      int nParentValues = bayesNet.getParentSet(iAttribute).GetCardinalityOfParents();
                      for (int iParent = 0; iParent < nParentValues; iParent++) {
                        int nTotal = 0;
                          for (int iAttValue = 0; iAttValue < nAttValues; iAttValue++) {
                            double nCount = ((DiscreteEstimatorBayes)NBNet.m_Distributions[iAttribute][iParent]).getCount(iAttValue);
                            w2 += Statistics.lnGamma(1.0/(nAttValues * nParentValues) + nCount)
                                  - Statistics.lnGamma(1.0/(nAttValues * nParentValues));
                            nTotal += nCount;
                          }
                        w2 += Statistics.lnGamma(1) - Statistics.lnGamma(1 + nTotal);
                      }
                  }
		
//    System.out.println(w1 + " " + w2 + " " + (w2 - w1));
                  if (w1 < w2) {
                    w2 = w2 - w1;
                    w1 = 0;
                    w1 = 1 / (1 + Math.exp(w2));
                    w2 = Math.exp(w2) / (1 + Math.exp(w2));
                  } else {
                    w1 = w1 - w2;
                    w2 = 0;
                    w2 = 1 / (1 + Math.exp(w1));
                    w1 = Math.exp(w1) / (1 + Math.exp(w1));
                  }
		
                  for (int iParent = 0; iParent < bayesNet.getParentSet(iAttribute).GetCardinalityOfParents(); iParent++) {
                      bayesNet.m_Distributions[iAttribute][iParent] = 
                      new DiscreteEstimatorFullBayes(
                        instances.attribute(iAttribute).numValues(), 
                        w1, w2,
                        (DiscreteEstimatorBayes) EmptyNet.m_Distributions[iAttribute][0],
                        (DiscreteEstimatorBayes) NBNet.m_Distributions[iAttribute][iParent],
                        m_fAlpha
                       );
                  } 
            }
        }
        int iAttribute = instances.classIndex();
        bayesNet.m_Distributions[iAttribute][0] = EmptyNet.m_Distributions[iAttribute][0];
    } // estimateCPTs

    /**
     * Updates the classifier with the given instance.
     * 
     * @param instance the new training instance to include in the model
     * @exception Exception if the instance could not be incorporated in
     * the model.
     */
    public void updateClassifier(BayesNet bayesNet, Instance instance) throws Exception {
        throw new Exception("updateClassifier does not apply to BMA estimator");
    } // updateClassifier

    /** initCPTs reserves space for CPTs and set all counts to zero
     */
    public void initCPTs(BayesNet bayesNet) throws Exception {
        // Reserve space for CPTs
        int nMaxParentCardinality = 1;

        for (int iAttribute = 0; iAttribute < bayesNet.m_Instances.numAttributes(); iAttribute++) {
            if (bayesNet.getParentSet(iAttribute).GetCardinalityOfParents() > nMaxParentCardinality) {
                nMaxParentCardinality = bayesNet.getParentSet(iAttribute).GetCardinalityOfParents();
            }
        }

        // Reserve plenty of memory
        bayesNet.m_Distributions = new Estimator[bayesNet.m_Instances.numAttributes()][nMaxParentCardinality];
    } // initCPTs


    /**
     * @return boolean
     */
    public boolean isUseK2Prior() {
        return m_bUseK2Prior;
    }

    /**
     * Sets the UseK2Prior.
     * @param bUseK2Prior The bUseK2Prior to set
     */
    public void setUseK2Prior(boolean bUseK2Prior) {
        m_bUseK2Prior = bUseK2Prior;
    }

} // class BMAEstimator
