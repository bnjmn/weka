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
 * CVSearchAlgorithm.java
 * Copyright (C) 2004 Remco Bouckaert
 * 
 */

package weka.classifiers.bayes.net.search.cv;

import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.ParentSet;
import weka.classifiers.bayes.net.search.*;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.Tag;
import weka.core.SelectedTag;

/** The CVSearchAlgorithm class supports Bayes net structure search algorithms
 * that are based on cross validation (as opposed to for example
 * score based of conditional independence based search algorithms).
 * 
 * @author Remco Bouckaert
 * @version $Revision: 1.1 $
 */
public class CVSearchAlgorithm extends SearchAlgorithm {
	BayesNet m_BayesNet;
	boolean m_bUseProb = false;
	int m_nNrOfFolds = 10;

	final static int LOOCV = 0;
	final static int CUMCV = 2;
	final static int KFOLDCV = 1;

	public static final Tag[] TAGS_CV_TYPE =
		{
			new Tag(LOOCV, "LOO-CV"),
			new Tag(KFOLDCV, "k-Fold-CV"),
			new Tag(CUMCV, "Cumulative-CV")
		};
	/**
	 * Holds the cross validation strategy used to measure quality of network
	 */
	int m_nCVType = LOOCV;



	/**
	 * performCV returns the accuracy calculated using cross validation.  
	 * The dataset used is m_Instances associated with the Bayes Network.
	 * @param bayesNet : Bayes Network containing structure to evaluate
	 * @return accuracy (in interval 0..1) measured using cv.
	 * @throws Exception whn m_nCVType is invalided + exceptions passed on by updateClassifier
	 */
	public double performCV(BayesNet bayesNet) throws Exception {
		switch (m_nCVType) {
			case LOOCV: 
				return LeaveOneOutCV(bayesNet);
			case CUMCV: 
				return CumulativeCV(bayesNet);
			case KFOLDCV: 
				return kFoldCV(bayesNet, m_nNrOfFolds);
			default:
				throw new Exception("Unrecognized cross validation type encountered: " + m_nCVType);
		}
	} // performCV

	/**
	 * Calc Node Score With AddedParent
	 * 
	 * @param nNode node for which the score is calculate
	 * @param nCandidateParent candidate parent to add to the existing parent set
	 * @return log score
	 */
	public double performCVWithExtraParent(int nNode, int nCandidateParent) throws Exception {
		ParentSet oParentSet = m_BayesNet.getParentSet(nNode);
		Instances instances = m_BayesNet.m_Instances;

		// sanity check: nCandidateParent should not be in parent set already
		for (int iParent = 0; iParent < oParentSet.GetNrOfParents(); iParent++) {
			if (oParentSet.GetParent(iParent) == nCandidateParent) {
				return -1e100;
			}
		}

		// determine cardinality of parent set & reserve space for frequency counts
		int nCardinality =
			oParentSet.GetCardinalityOfParents() * instances.attribute(nCandidateParent).numValues();
		int numValues = instances.attribute(nNode).numValues();
		int[][] nCounts = new int[nCardinality][numValues];

		// set up candidate parent
		oParentSet.AddParent(nCandidateParent, instances);

		// calculate the score
		double fAccuracy = performCV(m_BayesNet);

		// delete temporarily added parent
		oParentSet.DeleteLastParent(instances);

		return fAccuracy;
	} // performCVWithExtraParent

	/**
	 * LeaveOneOutCV returns the accuracy calculated using Leave One Out
	 * cross validation. The dataset used is m_Instances associated with
	 * the Bayes Network.
	 * @param bayesNet : Bayes Network containing structure to evaluate
	 * @return accuracy (in interval 0..1) measured using leave one out cv.
	 * @throws Exception passed on by updateClassifier
	 */
	public double LeaveOneOutCV(BayesNet bayesNet) throws Exception {
		m_BayesNet = bayesNet;
		double fAccuracy = 0.0;
		double fWeight = 0.0;
		Instances instances = bayesNet.m_Instances;
		bayesNet.estimateCPTs();
		for (int iInstance = 0; iInstance < instances.numInstances(); iInstance++) {
			Instance instance = instances.instance(iInstance);
			instance.setWeight(-instance.weight());
			bayesNet.updateClassifier(instance);
			fAccuracy += accuracyIncrease(instance);
			fWeight += instance.weight();
			instance.setWeight(-instance.weight());
			bayesNet.updateClassifier(instance);
		}
		return fAccuracy / fWeight;
	} // LeaveOneOutCV

	/**
	 * CumulativeCV returns the accuracy calculated using cumulative
	 * cross validation. The idea is to run through the data set and
	 * try to classify each of the instances based on the previously
	 * seen data.
	 * The data set used is m_Instances associated with the Bayes Network.
	 * @param bayesNet : Bayes Network containing structure to evaluate
	 * @return accuracy (in interval 0..1) measured using leave one out cv.
	 * @throws Exception passed on by updateClassifier
	 */
	public double CumulativeCV(BayesNet bayesNet) throws Exception {
		m_BayesNet = bayesNet;
		double fAccuracy = 0.0;
		double fWeight = 0.0;
		Instances instances = bayesNet.m_Instances;
		bayesNet.initCPTs();
		for (int iInstance = 0; iInstance < instances.numInstances(); iInstance++) {
			Instance instance = instances.instance(iInstance);
			fAccuracy += accuracyIncrease(instance);
			bayesNet.updateClassifier(instance);
			fWeight += instance.weight();
		}
		return fAccuracy / fWeight;
	} // LeaveOneOutCV
	
	/**
	 * kFoldCV uses k-fold cross validation to measure the accuracy of a Bayes
	 * network classifier.
	 * @param bayesNet : Bayes Network containing structure to evaluate
	 * @param nNrOfFolds : the number of folds k to perform k-fold cv
	 * @return accuracy (in interval 0..1) measured using leave one out cv.
	 * @throws Exception passed on by updateClassifier
	 */
	public double kFoldCV(BayesNet bayesNet, int nNrOfFolds) throws Exception {
		m_BayesNet = bayesNet;
		double fAccuracy = 0.0;
		double fWeight = 0.0;
		Instances instances = bayesNet.m_Instances;
		// estimate CPTs based on complete data set
		bayesNet.estimateCPTs();
		int nFoldStart = 0;
		int nFoldEnd = instances.numInstances() / nNrOfFolds;
		int iFold = 1;
		while (nFoldStart < instances.numInstances()) {
			// remove influence of fold iFold from the probability distribution
			for (int iInstance = nFoldStart; iInstance < nFoldEnd; iInstance++) {
				Instance instance = instances.instance(iInstance);
				instance.setWeight(-instance.weight());
				bayesNet.updateClassifier(instance);
			}
			
			// measure accuracy on fold iFold
			for (int iInstance = nFoldStart; iInstance < nFoldEnd; iInstance++) {
				Instance instance = instances.instance(iInstance);
				instance.setWeight(-instance.weight());
				fAccuracy += accuracyIncrease(instance);
				fWeight += instance.weight();
			}

			// restore influence of fold iFold from the probability distribution
			for (int iInstance = nFoldStart; iInstance < nFoldEnd; iInstance++) {
				Instance instance = instances.instance(iInstance);
				instance.setWeight(-instance.weight());
				bayesNet.updateClassifier(instance);
			}

			// go to next fold
			nFoldStart = nFoldEnd;
			iFold++;
			nFoldEnd = iFold * instances.numInstances() / nNrOfFolds;
		}
		return fAccuracy / fWeight;
	} // kFoldCV
	
	/** accuracyIncrease determines how much the accuracy estimate should
	 * be increased due to the contribution of a single given instance. 
	 * 
	 * @param instance : instance for which to calculate the accuracy increase.
	 * @return increase in accuracy due to given instance. 
	 * @throws Exception passed on by distributionForInstance and classifyInstance
	 */
	double accuracyIncrease(Instance instance) throws Exception {
		if (m_bUseProb) {
			double [] fProb = m_BayesNet.distributionForInstance(instance);
			return fProb[(int) instance.classValue()] * instance.weight();
		} else {
			if (m_BayesNet.classifyInstance(instance) == instance.classValue()) {
				return instance.weight();
			}
		}
		return 0;
	} // accuracyIncrease

	/**
	 * @return use probabilities or not in accuracy estimate
	 */
	public boolean getUseProb() {
		return m_bUseProb;
	} // getUseProb

	/**
	 * @param useProb : use probabilities or not in accuracy estimate
	 */
	public void setUseProb(boolean useProb) {
		m_bUseProb = useProb;
	} // setUseProb
	
	/**
	 * set cross validation strategy to be used in searching for networks.
	 * @param newCVType : cross validation strategy 
	 */
	public void setCVType(SelectedTag newCVType) {
		if (newCVType.getTags() == TAGS_CV_TYPE) {
			m_nCVType = newCVType.getSelectedTag().getID();
		}
	} // setCVType

	/**
	 * get cross validation strategy to be used in searching for networks.
	 * @return cross validation strategy 
	 */
	public SelectedTag getCVType() {
		return new SelectedTag(m_nCVType, TAGS_CV_TYPE);
	} // getCVType

	/**
	 * @return a string to describe the CVType option.
	 */
	public String CVTypeTipText() {
	  return "Select cross validation strategy to be used in searching for networks." +
	  "LOO-CV = Leave one out cross validation\n" +
	  "k-Fold-CV = k fold cross validation\n" +
	  "Cumulative-CV = cumulative cross validation."
	  ;
	} // CVTypeTipText

	/**
	 * @return a string to describe the UseProb option.
	 */
	public String useProbTipText() {
	  return "If set to true, the probability of the class if returned in the estimate of the "+
	  "accuracy. If set to false, the accuracy estimate is only increased if the classifier returns " +
	  "exactly the correct class.";
	} // useProbTipText

	/**
	 * This will return a string describing the search algorithm.
	 * @return The string.
	 */
	public String globalInfo() {
	  return "This Bayes Network learning algorithm uses cross validation to estimate " +
	  "classification accuracy.";
	} // globalInfo

} // class CVSearchAlgorithm
