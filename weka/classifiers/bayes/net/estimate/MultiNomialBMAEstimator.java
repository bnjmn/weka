package weka.classifiers.bayes.net.estimate;

import weka.classifiers.bayes.BayesNet;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.Utils;
import weka.core.Statistics;
import weka.estimators.Estimator;
import weka.classifiers.bayes.net.search.local.K2;

public class MultiNomialBMAEstimator extends BayesNetEstimator {

    protected boolean m_bUseK2Prior = true;

    /**
     * estimateCPTs estimates the conditional probability tables for the Bayes
     * Net using the network structure.
     */
    public void estimateCPTs(BayesNet bayesNet) throws Exception {
        
        // sanity check to see if nodes have not more than one parent
        for (int iAttribute = 0; iAttribute < bayesNet.m_Instances.numAttributes(); iAttribute++) {
            if (bayesNet.getParentSet(iAttribute).getNrOfParents() > 1) {
                throw new Exception("Cannot handle networks with nodes with more than 1 parent (yet).");
            }
        }

		// filter data to binary
        Instances instances = new Instances(bayesNet.m_Instances);
        while (instances.numInstances() > 0) {
            instances.delete(0);
        }
        for (int iAttribute = instances.numAttributes(); iAttribute >= 0; iAttribute--) {
            if (iAttribute != instances.classIndex()) {
                Attribute a = new Attribute(instances.attribute(iAttribute).name());
                a.addStringValue("0");
                a.addStringValue("1");
                instances.deleteAttributeAt(iAttribute);
                instances.insertAttributeAt(a,iAttribute);
            }
        }
        
        for (int iInstance = 0; iInstance < bayesNet.m_Instances.numInstances(); iInstance++) {
            Instance instanceOrig = bayesNet.m_Instances.instance(iInstance);
            Instance instance = new Instance(instances.numAttributes());
            for (int iAttribute = 0; iAttribute < instances.numAttributes(); iAttribute++) {
                if (iAttribute != instances.classIndex()) {
                    if (instanceOrig.value(iAttribute) > 0) {
                        instance.setValue(iAttribute, 1);
                    }
                } else {
                    instance.setValue(iAttribute, instanceOrig.value(iAttribute));
                }
            }
        }
        // ok, now all data is binary, except the class attribute
        // now learn the empty and tree network

        BayesNet EmptyNet = new BayesNet();
        K2 oSearchAlgorithm = new K2();
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

                      for (int iParent = 0; iParent < bayesNet.getParentSet(iAttribute).getCardinalityOfParents(); iParent++) {
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

					  int nParentValues = bayesNet.getParentSet(iAttribute).getCardinalityOfParents();
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
                  // normalize weigths
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
		
                  for (int iParent = 0; iParent < bayesNet.getParentSet(iAttribute).getCardinalityOfParents(); iParent++) {
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
        // Reserve sufficient memory
        bayesNet.m_Distributions = new Estimator[bayesNet.m_Instances.numAttributes()][2];
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

    /**
     * Calculates the class membership probabilities for the given test
     * instance.
     * 
     * @param instance the instance to be classified
     * @return predicted class probability distribution
     * @exception Exception if there is a problem generating the prediction
     */
    public double[] distributionForInstance(BayesNet bayesNet, Instance instance) throws Exception {
        Instances instances = bayesNet.m_Instances;
        int nNumClasses = instances.numClasses();
        double[] fProbs = new double[nNumClasses];

        for (int iClass = 0; iClass < nNumClasses; iClass++) {
            fProbs[iClass] = 1.0;
        }

        for (int iClass = 0; iClass < nNumClasses; iClass++) {
            double logfP = 0;

            for (int iAttribute = 0; iAttribute < instances.numAttributes(); iAttribute++) {
                double iCPT = 0;

                for (int iParent = 0; iParent < bayesNet.getParentSet(iAttribute).getNrOfParents(); iParent++) {
                    int nParent = bayesNet.getParentSet(iAttribute).getParent(iParent);

                    if (nParent == instances.classIndex()) {
                        iCPT = iCPT * nNumClasses + iClass;
                    } else {
                        iCPT = iCPT * instances.attribute(nParent).numValues() + instance.value(nParent);
                    }
                }

                if (iAttribute == instances.classIndex()) {
                    logfP += Math.log(bayesNet.m_Distributions[iAttribute][(int) iCPT].getProbability(iClass));
                } else {
                    logfP += instance.value(iAttribute) * Math.log(
                      bayesNet.m_Distributions[iAttribute][(int) iCPT].getProbability(instance.value(1)));
                }
            }

            fProbs[iClass] += logfP;
        }

        // Find maximum
        double fMax = fProbs[0];
        for (int iClass = 0; iClass < nNumClasses; iClass++) {
            if (fProbs[iClass] > fMax) {
                fMax = fProbs[iClass];
            }
        }
        // transform from log-space to normal-space
        for (int iClass = 0; iClass < nNumClasses; iClass++) {
            fProbs[iClass] = Math.exp(fProbs[iClass] - fMax);
        }

        // Display probabilities
        Utils.normalize(fProbs);

        return fProbs;
    } // distributionForInstance

} // class MultiNomialBMAEstimator
