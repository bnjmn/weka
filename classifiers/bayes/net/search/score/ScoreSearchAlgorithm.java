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
 * ScoreSearchAlgorithm.java
 * Copyright (C) 2004 Remco Bouckaert
 * 
 */
 
package weka.classifiers.bayes.net.search.score;

import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.ParentSet;
import weka.classifiers.bayes.net.search.SearchAlgorithm;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.Utils;
import weka.core.Statistics;
import weka.core.Tag;
import weka.core.Option;
import weka.core.SelectedTag;

import java.util.Vector;
import java.util.Enumeration;

/** The ScoreBasedSearchAlgorithm class supports Bayes net structure search algorithms
 * that are based on maximizing scores (as opposed to for example
 * conditional independence based search algorithms).
 * 
 * @author Remco Bouckaert
 * @version $Revision: 1.1 $
 */
public class ScoreSearchAlgorithm extends SearchAlgorithm {
	BayesNet m_BayesNet;
	Instances m_Instances;
	
	/**
	 * Holds prior on count
	 */
	double m_fAlpha = 0.5;

	public static final Tag[] TAGS_SCORE_TYPE =
		{
            new Tag(Scoreable.BAYES, "BAYES"),
            new Tag(Scoreable.BDeu, "BDeu"),
			new Tag(Scoreable.MDL, "MDL"),
			new Tag(Scoreable.ENTROPY, "ENTROPY"),
			new Tag(Scoreable.AIC, "AIC")
		};

	/**
	 * Holds the score type used to measure quality of network
	 */
	int m_nScoreType = Scoreable.BAYES;

	/**
	 * logScore returns the log of the quality of a network
	 * (e.g. the posterior probability of the network, or the MDL
	 * value).
	 * @param nType score type (Bayes, MDL, etc) to calculate score with
	 * @return log score.
	 */
    public double logScore(int nType) {
        if (nType < 0) {
            nType = m_nScoreType;
        }

        double fLogScore = 0.0;

        for (int iAttribute = 0; iAttribute < m_Instances.numAttributes(); iAttribute++) {
            for (int iParent = 0; iParent < m_BayesNet.getParentSet(iAttribute).GetCardinalityOfParents(); iParent++) {
                fLogScore += ((Scoreable) m_BayesNet.m_Distributions[iAttribute][iParent]).logScore(nType);
            }

            switch (nType) {
                case (Scoreable.MDL) :
                    {
                        fLogScore -= 0.5
                            * m_BayesNet.getParentSet(iAttribute).GetCardinalityOfParents()
                            * (m_Instances.attribute(iAttribute).numValues() - 1)
                            * Math.log(m_Instances.numInstances());
                    }
                    break;
                case (Scoreable.AIC) :
                    {
                        fLogScore -= m_BayesNet.getParentSet(iAttribute).GetCardinalityOfParents()
                            * (m_Instances.attribute(iAttribute).numValues() - 1);
                    }
                    break;
            }
        }

        return fLogScore;
    } // logScore

	/**
	* buildStructure determines the network structure/graph of the network
	* with the K2 algorithm, restricted by its initial structure (which can
	* be an empty graph, or a Naive Bayes graph.
	*/
	public void buildStructure (BayesNet bayesNet, Instances instances) throws Exception {
		m_BayesNet = bayesNet;
		m_Instances = instances;
	} // buildStructure


	/**
	 * Calc Node Score for given parent set
	 * 
	 * @param nNode node for which the score is calculate
	 * @return log score
	 */
	public double CalcNodeScore(int nNode) {
		if (m_BayesNet.getUseADTree() && m_BayesNet.getADTree() != null) {
			return CalcNodeScoreADTree(nNode, m_Instances);
		} else {
			return CalcNodeScore(nNode, m_Instances);
		}
	}

	/**
	 * helper function for CalcNodeScore above using the ADTree data structure
	 * @param nNode node for which the score is calculate
	 * @param instances used to calculate score with
	 * @return log score
	 */
	private double CalcNodeScoreADTree(int nNode, Instances instances) {
		ParentSet oParentSet = m_BayesNet.getParentSet(nNode);
		// get set of parents, insert iNode
		int nNrOfParents = oParentSet.GetNrOfParents();
		int[] nNodes = new int[nNrOfParents + 1];
		for (int iParent = 0; iParent < nNrOfParents; iParent++) {
			nNodes[iParent] = oParentSet.GetParent(iParent);
		}
		nNodes[nNrOfParents] = nNode;

		// calculate offsets
		int[] nOffsets = new int[nNrOfParents + 1];
		int nOffset = 1;
		nOffsets[nNrOfParents] = 1;
		nOffset *= instances.attribute(nNode).numValues();
		for (int iNode = nNrOfParents - 1; iNode >= 0; iNode--) {
			nOffsets[iNode] = nOffset;
			nOffset *= instances.attribute(nNodes[iNode]).numValues();
		}

		// sort nNodes & offsets
		for (int iNode = 1; iNode < nNodes.length; iNode++) {
			int iNode2 = iNode;
			while (iNode2 > 0 && nNodes[iNode2] < nNodes[iNode2 - 1]) {
				int h = nNodes[iNode2];
				nNodes[iNode2] = nNodes[iNode2 - 1];
				nNodes[iNode2 - 1] = h;
				h = nOffsets[iNode2];
				nOffsets[iNode2] = nOffsets[iNode2 - 1];
				nOffsets[iNode2 - 1] = h;
				iNode2--;
			}
		}

		// get counts from ADTree
		int nCardinality = oParentSet.GetCardinalityOfParents();
		int numValues = instances.attribute(nNode).numValues();
		int[] nCounts = new int[nCardinality * numValues];
		//if (nNrOfParents > 1) {

		m_BayesNet.getADTree().getCounts(nCounts, nNodes, nOffsets, 0, 0, false);

		return CalcScoreOfCounts(nCounts, nCardinality, numValues, instances);
	} // CalcNodeScore

	private double CalcNodeScore(int nNode, Instances instances) {
		ParentSet oParentSet = m_BayesNet.getParentSet(nNode);

		// determine cardinality of parent set & reserve space for frequency counts
		int nCardinality = oParentSet.GetCardinalityOfParents();
		int numValues = instances.attribute(nNode).numValues();
		int[] nCounts = new int[nCardinality * numValues];

		// initialize (don't need this?)
		for (int iParent = 0; iParent < nCardinality * numValues; iParent++) {
			nCounts[iParent] = 0;
		}

		// estimate distributions
		Enumeration enumInsts = instances.enumerateInstances();

		while (enumInsts.hasMoreElements()) {
			Instance instance = (Instance) enumInsts.nextElement();

			// updateClassifier;
			double iCPT = 0;

			for (int iParent = 0; iParent < oParentSet.GetNrOfParents(); iParent++) {
				int nParent = oParentSet.GetParent(iParent);

				iCPT = iCPT * instances.attribute(nParent).numValues() + instance.value(nParent);
			}

			nCounts[numValues * ((int) iCPT) + (int) instance.value(nNode)]++;
		}

		return CalcScoreOfCounts(nCounts, nCardinality, numValues, instances);
	} // CalcNodeScore

	/**
	 * utility function used by CalcScore and CalcNodeScore to determine the score
	 * based on observed frequencies.
	 * 
	 * @param nCounts array with observed frequencies
	 * @param nCardinality ardinality of parent set
	 * @param numValues number of values a node can take
	 * @param instances to calc score with
	 * @return log score
	 */
	protected double CalcScoreOfCounts(int[] nCounts, int nCardinality, int numValues, Instances instances) {

		// calculate scores using the distributions
		double fLogScore = 0.0;

		for (int iParent = 0; iParent < nCardinality; iParent++) {
			switch (m_nScoreType) {

				case (Scoreable.BAYES) :
					{
						double nSumOfCounts = 0;

						for (int iSymbol = 0; iSymbol < numValues; iSymbol++) {
							if (m_fAlpha + nCounts[iParent * numValues + iSymbol] != 0) {
								fLogScore += Statistics.lnGamma(m_fAlpha + nCounts[iParent * numValues + iSymbol]);
								nSumOfCounts += m_fAlpha + nCounts[iParent * numValues + iSymbol];
							}
						}

						if (nSumOfCounts != 0) {
							fLogScore -= Statistics.lnGamma(nSumOfCounts);
						}

						if (m_fAlpha != 0) {
							fLogScore -= numValues * Statistics.lnGamma(m_fAlpha);
							fLogScore += Statistics.lnGamma(numValues * m_fAlpha);
						}
					}

					break;
                case (Scoreable.BDeu) :
                {
                    double nSumOfCounts = 0;

                    for (int iSymbol = 0; iSymbol < numValues; iSymbol++) {
                        if (m_fAlpha + nCounts[iParent * numValues + iSymbol] != 0) {
                            fLogScore += Statistics.lnGamma(1.0/(numValues * nCardinality) + nCounts[iParent * numValues + iSymbol]);
                            nSumOfCounts += 1.0/(numValues * nCardinality) + nCounts[iParent * numValues + iSymbol];
                        }
                    }
                    fLogScore -= Statistics.lnGamma(nSumOfCounts);

                    fLogScore -= numValues * Statistics.lnGamma(1.0);
                    fLogScore += Statistics.lnGamma(numValues * 1.0);
                }
	                break;

				case (Scoreable.MDL) :

				case (Scoreable.AIC) :

				case (Scoreable.ENTROPY) :
					{
						double nSumOfCounts = 0;

						for (int iSymbol = 0; iSymbol < numValues; iSymbol++) {
							nSumOfCounts += nCounts[iParent * numValues + iSymbol];
						}

						for (int iSymbol = 0; iSymbol < numValues; iSymbol++) {
							if (nCounts[iParent * numValues + iSymbol] > 0) {
								fLogScore += nCounts[iParent * numValues
									+ iSymbol] * Math.log(nCounts[iParent * numValues + iSymbol] / nSumOfCounts);
							}
						}
					}

					break;

				default :
					{
					}
			}
		}

		switch (m_nScoreType) {

			case (Scoreable.MDL) :
				{
					fLogScore -= 0.5 * nCardinality * (numValues - 1) * Math.log(instances.numInstances());

					// it seems safe to assume that numInstances>0 here
				}

				break;

			case (Scoreable.AIC) :
				{
					fLogScore -= nCardinality * (numValues - 1);
				}

				break;
		}

		return fLogScore;
	} // CalcNodeScore

	protected double CalcScoreOfCounts2(int[][] nCounts, int nCardinality, int numValues, Instances instances) {

		// calculate scores using the distributions
		double fLogScore = 0.0;

		for (int iParent = 0; iParent < nCardinality; iParent++) {
			switch (m_nScoreType) {

				case (Scoreable.BAYES) :
					{
						double nSumOfCounts = 0;

						for (int iSymbol = 0; iSymbol < numValues; iSymbol++) {
							if (m_fAlpha + nCounts[iParent][iSymbol] != 0) {
								fLogScore += Statistics.lnGamma(m_fAlpha + nCounts[iParent][iSymbol]);
								nSumOfCounts += m_fAlpha + nCounts[iParent][iSymbol];
							}
						}

						if (nSumOfCounts != 0) {
							fLogScore -= Statistics.lnGamma(nSumOfCounts);
						}

						if (m_fAlpha != 0) {
							fLogScore -= numValues * Statistics.lnGamma(m_fAlpha);
							fLogScore += Statistics.lnGamma(numValues * m_fAlpha);
						}
					}

					break;

				case (Scoreable.BDeu) :
				{
					double nSumOfCounts = 0;

					for (int iSymbol = 0; iSymbol < numValues; iSymbol++) {
						if (m_fAlpha + nCounts[iParent * numValues][iSymbol] != 0) {
							fLogScore += Statistics.lnGamma(1.0/(numValues * nCardinality) + nCounts[iParent * numValues][iSymbol]);
							nSumOfCounts += 1.0/(numValues * nCardinality) + nCounts[iParent * numValues][iSymbol];
						}
					}
					fLogScore -= Statistics.lnGamma(nSumOfCounts);

					fLogScore -= numValues * Statistics.lnGamma(1.0);
					fLogScore += Statistics.lnGamma(numValues * 1.0);
				}
					break;

				case (Scoreable.MDL) :

				case (Scoreable.AIC) :

				case (Scoreable.ENTROPY) :
					{
						double nSumOfCounts = 0;

						for (int iSymbol = 0; iSymbol < numValues; iSymbol++) {
							nSumOfCounts += nCounts[iParent][iSymbol];
						}

						for (int iSymbol = 0; iSymbol < numValues; iSymbol++) {
							if (nCounts[iParent][iSymbol] > 0) {
								fLogScore += nCounts[iParent][iSymbol]
									* Math.log(nCounts[iParent][iSymbol] / nSumOfCounts);
							}
						}
					}

					break;

				default :
					{
					}
			}
		}

		switch (m_nScoreType) {

			case (Scoreable.MDL) :
				{
					fLogScore -= 0.5 * nCardinality * (numValues - 1) * Math.log(instances.numInstances());

					// it seems safe to assume that numInstances>0 here
				}

				break;

			case (Scoreable.AIC) :
				{
					fLogScore -= nCardinality * (numValues - 1);
				}

				break;
		}

		return fLogScore;
	} // CalcNodeScore


	/**
	 * Calc Node Score With AddedParent
	 * 
	 * @param nNode node for which the score is calculate
	 * @param nCandidateParent candidate parent to add to the existing parent set
	 * @return log score
	 */
	public double CalcScoreWithExtraParent(int nNode, int nCandidateParent) {
		ParentSet oParentSet = m_BayesNet.getParentSet(nNode);

		// sanity check: nCandidateParent should not be in parent set already
		for (int iParent = 0; iParent < oParentSet.GetNrOfParents(); iParent++) {
			if (oParentSet.GetParent(iParent) == nCandidateParent) {
				return -1e100;
			}
		}

		// determine cardinality of parent set & reserve space for frequency counts
		int nCardinality =
			oParentSet.GetCardinalityOfParents() * m_Instances.attribute(nCandidateParent).numValues();
		int numValues = m_Instances.attribute(nNode).numValues();
		int[][] nCounts = new int[nCardinality][numValues];

		// set up candidate parent
		oParentSet.AddParent(nCandidateParent, m_Instances);

		// calculate the score
		double logScore = CalcNodeScore(nNode);

		// delete temporarily added parent
		oParentSet.DeleteLastParent(m_Instances);

		return logScore;
	} // CalcScoreWithExtraParent


	/**
	 * set quality measure to be used in searching for networks.
	 * @param scoreType
	 */
	public void setScoreType(SelectedTag newScoreType) {
		if (newScoreType.getTags() == TAGS_SCORE_TYPE) {
			m_nScoreType = newScoreType.getSelectedTag().getID();
		}
	}

	/**
	 * get quality measure to be used in searching for networks.
	 * @return quality measure
	 */
	public SelectedTag getScoreType() {
		return new SelectedTag(m_nScoreType, TAGS_SCORE_TYPE);
	}

	/**
	 * Returns an enumeration describing the available options
	 * 
	 * @return an enumeration of all the available options
	 */
	public Enumeration listOptions() {
		Vector newVector = new Vector(1);

		newVector.addElement(
			new Option(
				"\tScore type (BAYES, BDeu, MDL, ENTROPY and AIC)\n",
				"S",
				1,
				"-S [BAYES|MDL|ENTROPY|AIC|CROSS_CLASSIC|CROSS_BAYES]"));

		return newVector.elements();
	} // listOptions

	public void setOptions(String[] options) throws Exception {

		String sScore = Utils.getOption('S', options);

		if (sScore.compareTo("BAYES") == 0) {
			setScoreType(new SelectedTag(Scoreable.BAYES, TAGS_SCORE_TYPE));
		}
		if (sScore.compareTo("BDeu") == 0) {
			setScoreType(new SelectedTag(Scoreable.BDeu, TAGS_SCORE_TYPE));
		}
		if (sScore.compareTo("MDL") == 0) {
			setScoreType(new SelectedTag(Scoreable.MDL, TAGS_SCORE_TYPE));
		}
		if (sScore.compareTo("ENTROPY") == 0) {
			setScoreType(new SelectedTag(Scoreable.ENTROPY, TAGS_SCORE_TYPE));
		}
		if (sScore.compareTo("AIC") == 0) {
			setScoreType(new SelectedTag(Scoreable.AIC, TAGS_SCORE_TYPE));
		}
	} // setOptions

	public String[] getOptions() {
		String[] options = new String[2];
		int current = 0;

		options[current++] = "-S";

		switch (m_nScoreType) {

			case (Scoreable.BAYES) :
				options[current++] = "BAYES";
				break;

			case (Scoreable.BDeu) :
				options[current++] = "BDeu";
				break;

			case (Scoreable.MDL) :
				options[current++] = "MDL";
				break;

			case (Scoreable.ENTROPY) :
				options[current++] = "ENTROPY";

				break;

			case (Scoreable.AIC) :
				options[current++] = "AIC";
				break;
		}

		// Fill up rest with empty strings, not nulls!
		while (current < options.length) {
			options[current++] = "";
		}

		return options;
	} // getOptions

	/**
	 * @return a string to describe the ScoreType option.
	 */
	public String scoreTypeTipText() {
		return "The score type determines the measure used to judge the quality of a"
			+ " network structure. It can be one of Bayes, BDeu, Minimum Description Length (MDL),"
			+ " Akaike Information Criterion (AIC), Entropy, classic and Bayes cross validation.";
	}
} // class ScoreBasedSearchAlgorithm
