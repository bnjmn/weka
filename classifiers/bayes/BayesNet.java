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
 * Copyright (C) 2001 Remco Bouckaert
 * 
 */
package weka.classifiers.bayes;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.estimators.*;
import weka.classifiers.*;

/**
 * Base class for a Bayes Network classifier. Provides datastructures (network structure,
 * conditional probability distributions, etc.) and facilities common to Bayes Network
 * learning algorithms like K2 and B.
 * Works with nominal variables and no missing values only.
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.3 $
 */
public class BayesNet extends DistributionClassifier implements OptionHandler, 
	WeightedInstancesHandler {

  /**
   * topological ordering of the network
   */
  protected int[]	  m_nOrder;

  /**
   * The parent sets.
   */
  protected ParentSet[]   m_ParentSets;

  /**
   * The attribute estimators containing CPTs.
   */
  protected Estimator[][] m_Distributions;

  /**
   * The number of classes
   */
  protected int		  m_NumClasses;

  /**
   * The dataset header for the purposes of printing out a semi-intelligible
   * model
   */
  protected Instances     m_Instances;

  public static final Tag [] TAGS_SCORE_TYPE = {
    new Tag(Scoreable.BAYES, "BAYES"),
    new Tag(Scoreable.MDL, "MDL"),
    new Tag(Scoreable.ENTROPY, "ENTROPY"),
    new Tag(Scoreable.AIC, "AIC")
  };

  /**
   * Holds the score type used to measure quality of network
   */
  int			  m_nScoreType = Scoreable.BAYES;

  /**
   * Holds prior on count
   */
  double		  m_fAlpha = 0.5;

  /**
   * Holds upper bound on number of parents
   */
  int			  m_nMaxNrOfParents = 100000;

  /**
   * determines whether initial structure is an empty graph or a Naive Bayes network
   */
  boolean		  m_bInitAsNaiveBayes = true;

  /**
   * Generates the classifier.
   * 
   * @param instances set of instances serving as training data
   * @exception Exception if the classifier has not been generated
   * successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    // Copy the instances
    m_Instances = new Instances(instances);

    // check that all variables are nominal
    Enumeration enum = m_Instances.enumerateAttributes();

    while (enum.hasMoreElements()) {
      Attribute attribute = (Attribute) enum.nextElement();

      if (attribute.type() != Attribute.NOMINAL) {
	throw new Exception("BayesNet handles nominal variables only. Non-nominal variable in dataset detected.");
      } 
    } 

    // TODO: check that there are no missing values
    // sanity check: need more than 1 variable in datat set
    m_NumClasses = instances.numClasses();

    if (m_NumClasses < 0) {
      throw new Exception("Dataset has no class attribute");
    } 

    // build the network structure
    initStructure();

    // build the network structure
    buildStructure();

    // build the set of CPTs
    estimateCPTs();

    // Save space
    // m_Instances = new Instances(m_Instances, 0);
  }    // buildClassifier
 
  /**
   * Init structure initializes the structure to an empty graph or a Naive Bayes
   * graph (depending on the -N flag).
   */
  public void initStructure() throws Exception {

    // initialize topological ordering
    m_nOrder = new int[m_Instances.numAttributes()];
    m_nOrder[0] = m_Instances.classIndex();

    int nAttribute = 0;

    for (int iOrder = 1; iOrder < m_Instances.numAttributes(); iOrder++) {
      if (nAttribute == m_Instances.classIndex()) {
	nAttribute++;
      } 

      m_nOrder[iOrder] = nAttribute++;
    } 

    // reserce memory
    m_ParentSets = new ParentSet[m_Instances.numAttributes()];

    for (int iAttribute = 0; iAttribute < m_Instances.numAttributes(); 
	 iAttribute++) {
      m_ParentSets[iAttribute] = new ParentSet(m_Instances.numAttributes());
    } 

    if (m_bInitAsNaiveBayes) {

      // initialize parent sets to have arrow from classifier node to
      // each of the other nodes
      for (int iOrder = 1; iOrder < m_Instances.numAttributes(); iOrder++) {
	int iAttribute = m_nOrder[iOrder];

	m_ParentSets[iAttribute].AddParent(m_Instances.classIndex(), 
					   m_Instances);
      } 
    } 
  } 

  /**
   * buildStructure determines the network structure/graph of the network.
   * The default behavior is creating a network where all nodes have the first
   * node as its parent (i.e., a BayesNet that behaves like a naive Bayes classifier).
   * This method can be overridden by derived classes to restrict the class
   * of network structures that are acceptable.
   */
  public void buildStructure() throws Exception {

    // place holder for structure learing algorithms like K2, B, etc.
  }    // buildStructure
 
  /**
   * estimateCPTs estimates the conditional probability tables for the Bayes
   * Net using the network structure.
   */
  public void estimateCPTs() throws Exception {

    // Reserve space for CPTs
    int nMaxParentCardinality = 1;

    for (int iAttribute = 0; iAttribute < m_Instances.numAttributes(); 
	 iAttribute++) {
      if (m_ParentSets[iAttribute].GetCardinalityOfParents() 
	      > nMaxParentCardinality) {
	nMaxParentCardinality = 
	  m_ParentSets[iAttribute].GetCardinalityOfParents();
      } 
    } 

    // Reserve plenty of memory
    m_Distributions = 
      new Estimator[m_Instances.numAttributes()][nMaxParentCardinality];

    // estimate CPTs
    for (int iAttribute = 0; iAttribute < m_Instances.numAttributes(); 
	 iAttribute++) {
      for (int iParent = 0; 
	   iParent < m_ParentSets[iAttribute].GetCardinalityOfParents(); 
	   iParent++) {
	m_Distributions[iAttribute][iParent] = 
	  new DiscreteEstimatorBayes(m_Instances.attribute(iAttribute)
	    .numValues(), m_fAlpha);
      } 
    } 

    // Compute counts
    Enumeration enumInsts = m_Instances.enumerateInstances();

    while (enumInsts.hasMoreElements()) {
      Instance instance = (Instance) enumInsts.nextElement();

      updateClassifier(instance);
    } 
  }    // estimateCPTs
 
  /**
   * Updates the classifier with the given instance.
   * 
   * @param instance the new training instance to include in the model
   * @exception Exception if the instance could not be incorporated in
   * the model.
   */
  public void updateClassifier(Instance instance) throws Exception {
    for (int iAttribute = 0; iAttribute < m_Instances.numAttributes(); 
	 iAttribute++) {
      double iCPT = 0;

      for (int iParent = 0; 
	   iParent < m_ParentSets[iAttribute].GetNrOfParents(); iParent++) {
	int nParent = m_ParentSets[iAttribute].GetParent(iParent);

	iCPT = iCPT * m_Instances.attribute(nParent).numValues() 
	       + instance.value(nParent);
      } 

      m_Distributions[iAttribute][(int) iCPT]
	.addValue(instance.value(iAttribute), instance.weight());
    } 
  }    // updateClassifier
 
  /**
   * Calculates the class membership probabilities for the given test
   * instance.
   * 
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if there is a problem generating the prediction
   */
  public double[] distributionForInstance(Instance instance) 
	  throws Exception {
    double[] fProbs = new double[m_NumClasses];

    for (int iClass = 0; iClass < m_NumClasses; iClass++) {
      fProbs[iClass] = 1.0;
    } 

    for (int iClass = 0; iClass < m_NumClasses; iClass++) {
      double fP = 1.0;

      for (int iAttribute = 0; iAttribute < m_Instances.numAttributes(); 
	   iAttribute++) {
	double iCPT = 0;

	for (int iParent = 0; 
	     iParent < m_ParentSets[iAttribute].GetNrOfParents(); iParent++) {
	  int nParent = m_ParentSets[iAttribute].GetParent(iParent);

	  if (nParent == m_Instances.classIndex()) {
	    iCPT = iCPT * m_NumClasses + iClass;
	  } else {
	    iCPT = iCPT * m_Instances.attribute(nParent).numValues() 
		   + instance.value(nParent);
	  } 
	} 

	if (iAttribute == m_Instances.classIndex()) {
	  fP *= 
	    m_Distributions[iAttribute][(int) iCPT].getProbability(iClass);
	} else {
	  fP *= 
	    m_Distributions[iAttribute][(int) iCPT]
	      .getProbability(instance.value(iAttribute));
	} 
      } 

      fProbs[iClass] *= fP;
    } 

    // Display probabilities
    Utils.normalize(fProbs);

    return fProbs;
  }    // distributionForInstance
 
  /**
   * Returns an enumeration describing the available options
   * 
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(4);

    newVector
      .addElement(new Option("\tScore type (BAYES, MDL, ENTROPY, or AIC)\n", 
			     "S", 1, "-S [BAYES|MDL|ENTROPY|AIC]"));
    newVector.addElement(new Option("\tInitial count (alpha)\n", "A", 1, 
				    "-A <alpha>"));
    newVector
      .addElement(new Option("\tInitial structure is empty (instead of Naive Bayes)\n", 
			     "N", 0, "-N"));
    newVector.addElement(new Option("\tMaximum number of parents\n", "P", 1, 
				    "-P <nr of parents>"));

    return newVector.elements();
  }    // listOptions
 
  /**
   * Parses a given list of options. Valid options are:<p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    m_bInitAsNaiveBayes = !(Utils.getFlag('N', options));

    String sScore = Utils.getOption('S', options);

    if (sScore.compareTo("BAYES") == 0) {
      //      m_nScoreType = Scoreable.BAYES;
      setScoreType(new SelectedTag(Scoreable.BAYES, TAGS_SCORE_TYPE));
    } 

    if (sScore.compareTo("MDL") == 0) {
      //      m_nScoreType = Scoreable.MDL;
      setScoreType(new SelectedTag(Scoreable.MDL, TAGS_SCORE_TYPE));
    } 

    if (sScore.compareTo("ENTROPY") == 0) {
      //      m_nScoreType = Scoreable.ENTROPY;
      setScoreType(new SelectedTag(Scoreable.ENTROPY, TAGS_SCORE_TYPE));
    } 

    if (sScore.compareTo("AIC") == 0) {
      //      m_nScoreType = Scoreable.AIC;
      setScoreType(new SelectedTag(Scoreable.AIC, TAGS_SCORE_TYPE));
    } 

    String sAlpha = Utils.getOption('A', options);

    if (sAlpha.length() != 0) {
      m_fAlpha = (new Float(sAlpha)).floatValue();
    } else {
      m_fAlpha = 0.5f;
    } 

    String sMaxNrOfParents = Utils.getOption('P', options);

    if (sMaxNrOfParents.length() != 0) {
      setMaxNrOfParents(Integer.parseInt(sMaxNrOfParents));
    } else {
      setMaxNrOfParents(100000);
    } 

    Utils.checkForRemainingOptions(options);
  }    // setOptions
 
  /**
   * Method declaration
   *
   * @param scoreType
   *
   */
  public void setScoreType(SelectedTag newScoreType) {

    if (newScoreType.getTags() == TAGS_SCORE_TYPE) {
      m_nScoreType = newScoreType.getSelectedTag().getID();
    }
  } 

  /**
   * Method declaration
   *
   * @return
   *
   */
  public SelectedTag getScoreType() {
    return new SelectedTag(m_nScoreType, TAGS_SCORE_TYPE);
  } 

  /**
   * Method declaration
   *
   * @param fAlpha
   *
   */
  public void setAlpha(double fAlpha) {
    m_fAlpha = fAlpha;
  } 

  /**
   * Method declaration
   *
   * @return
   *
   */
  public double getAlpha() {
    return m_fAlpha;
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
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    String[] options = new String[7];
    int      current = 0;

    options[current++] = "-S";

    switch (m_nScoreType) {

    case (Scoreable.BAYES):
      options[current++] = "BAYES";

      break;

    case (Scoreable.MDL):
      options[current++] = "MDL";

      break;

    case (Scoreable.ENTROPY):
      options[current++] = "ENTROPY";

      break;

    case (Scoreable.AIC):
      options[current++] = "AIC";

      break;
    }

    options[current++] = "-A";
    options[current++] = "" + m_fAlpha;

    if (!m_bInitAsNaiveBayes) {
      options[current++] = "-N";
    } 

    if (m_nMaxNrOfParents != 10000) {
      options[current++] = "-P";
      options[current++] = "" + m_nMaxNrOfParents;
    } 

    // Fill up rest with empty strings, not nulls!
    while (current < options.length) {
      options[current++] = "";
    } 

    return options;
  }    // getOptions
 
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

    for (int iAttribute = 0; iAttribute < m_Instances.numAttributes(); 
	 iAttribute++) {
      for (int iParent = 0; 
	   iParent < m_ParentSets[iAttribute].GetCardinalityOfParents(); 
	   iParent++) {
	fLogScore += 
	  ((Scoreable) m_Distributions[iAttribute][iParent]).logScore(nType);
      } 

      switch (nType) {

      case (Scoreable.MDL): {
	fLogScore -= 0.5 * m_ParentSets[iAttribute].GetCardinalityOfParents() 
		     * (m_Instances.attribute(iAttribute).numValues() - 1) 
		     * Math.log(m_Instances.numInstances());
      } 

	break;

      case (Scoreable.AIC): {
	fLogScore -= m_ParentSets[iAttribute].GetCardinalityOfParents() 
		     * (m_Instances.attribute(iAttribute).numValues() - 1);
      } 

	break;
      }
    } 

    return fLogScore;
  }    // logScore
 
  /**
   * Returns a description of the classifier.
   * 
   * @return a description of the classifier as a string.
   */
  public String toString() {
    StringBuffer text = new StringBuffer();

    text.append("Bayes Network Classifier");

    if (m_Instances == null) {
      text.append(": No model built yet.");
    } else {

      // TODO: flatten BayesNet down to text
      text.append("\n#attributes=");
      text.append(m_Instances.numAttributes());
      text.append(" #classindex=");
      text.append(m_Instances.classIndex());
      text.append("\nNetwork structure (nodes followed by parents)\n");

      for (int iAttribute = 0; iAttribute < m_Instances.numAttributes(); 
	   iAttribute++) {
	text.append(m_Instances.attribute(iAttribute).name() + "(" 
		    + m_Instances.attribute(iAttribute).numValues() + "): ");

	for (int iParent = 0; 
	     iParent < m_ParentSets[iAttribute].GetNrOfParents(); iParent++) {
	  text
	    .append(m_Instances.attribute(m_ParentSets[iAttribute].GetParent(iParent)).name() 
		    + " ");
	} 

	text.append("\n");

	// Description of distributions tends to be too much detail, so it is commented out here
	// for (int iParent = 0; iParent < m_ParentSets[iAttribute].GetCardinalityOfParents(); iParent++) {
	// text.append('(' + m_Distributions[iAttribute][iParent].toString() + ')');
	// }
	// text.append("\n");
      } 

      text.append("LogScore Bayes: " + logScore(Scoreable.BAYES) + "\n");
      text.append("LogScore MDL: " + logScore(Scoreable.MDL) + "\n");
      text.append("LogScore ENTROPY: " + logScore(Scoreable.ENTROPY) + "\n");
      text.append("LogScore AIC: " + logScore(Scoreable.AIC) + "\n");

      String[] options = getOptions();

      for (int iOption = 0; iOption < 1; iOption++) {
	text.append(options[iOption]);
      } 
    } 

    return text.toString();
  }    // toString
 
  /**
   * Calc Node Score With AddedParent
   * 
   * @param nNode node for which the score is calculate
   * @param nCandidateParent candidate parent to add to the existing parent set
   * @return log score
   */
  protected double CalcScoreWithExtraParent(int nNode, int nCandidateParent) {

    // sanity check: nCandidateParent should not be in parent set already
    for (int iParent = 0; iParent < m_ParentSets[nNode].GetNrOfParents(); 
	 iParent++) {
      if (m_ParentSets[nNode].GetParent(iParent) == nCandidateParent) {
	return -1e100;
      } 
    } 

    // determine cardinality of parent set & reserve space for frequency counts
    int     nCardinality = 
      m_ParentSets[nNode].GetCardinalityOfParents() 
      * m_Instances.attribute(nCandidateParent).numValues();
    int     numValues = m_Instances.attribute(nNode).numValues();
    int[][] nCounts = new int[nCardinality][numValues];

    // set up candidate parent
    m_ParentSets[nNode].AddParent(nCandidateParent, m_Instances);

    // calculate the score
    double logScore = CalcNodeScore(nNode);

    // delete temporarily added parent
    m_ParentSets[nNode].DeleteLastParent(m_Instances);

    return logScore;
  }    // CalcScore
 
  /**
   * Calc Node Score for given parent set
   * 
   * @param nNode node for which the score is calculate
   * @return log score
   */
  protected double CalcNodeScore(int nNode) {
    return CalcNodeScore(nNode, m_Instances);
  } 

  /**
   * helper function for CalcNodeScore above
   * @param nNode node for which the score is calculate
   * @param instances used to calculate score with
   * @return log score
   */
  private double CalcNodeScore(int nNode, Instances instances) {

    // determine cardinality of parent set & reserve space for frequency counts
    int     nCardinality = m_ParentSets[nNode].GetCardinalityOfParents();
    int     numValues = instances.attribute(nNode).numValues();
    int[][] nCounts = new int[nCardinality][numValues];

    // initialize (don't need this?)
    for (int iParent = 0; iParent < nCardinality; iParent++) {
      for (int iValue = 0; iValue < numValues; iValue++) {
	nCounts[iParent][iValue] = 0;
      } 
    } 

    // estimate distributions
    Enumeration enumInsts = instances.enumerateInstances();

    while (enumInsts.hasMoreElements()) {
      Instance instance = (Instance) enumInsts.nextElement();

      // updateClassifier;
      double   iCPT = 0;

      for (int iParent = 0; iParent < m_ParentSets[nNode].GetNrOfParents(); 
	   iParent++) {
	int nParent = m_ParentSets[nNode].GetParent(iParent);

	iCPT = iCPT * instances.attribute(nParent).numValues() 
	       + instance.value(nParent);
      } 

      nCounts[(int) iCPT][(int) instance.value(nNode)]++;
    } 

    return CalcScoreOfCounts(nCounts, nCardinality, numValues, instances);
  }    // CalcNodeScore
 
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
  protected double CalcScoreOfCounts(int[][] nCounts, int nCardinality, 
				     int numValues, Instances instances) {

    // calculate scores using the distributions
    double fLogScore = 0.0;

    for (int iParent = 0; iParent < nCardinality; iParent++) {
      switch (m_nScoreType) {

      case (Scoreable.BAYES): {
	double nSumOfCounts = 0;

	for (int iSymbol = 0; iSymbol < numValues; iSymbol++) {
	  if (m_fAlpha + nCounts[iParent][iSymbol] != 0) {
	    fLogScore += 
	      Statistics.lnGamma(m_fAlpha + nCounts[iParent][iSymbol]);
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

      case (Scoreable.MDL):

      case (Scoreable.AIC):

      case (Scoreable.ENTROPY): {
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

      default: {}
      }
    } 

    switch (m_nScoreType) {

    case (Scoreable.MDL): {
      fLogScore -= 0.5 * nCardinality * (numValues - 1) 
		   * Math.log(instances.numInstances());

      // it seems safe to assume that numInstances>0 here
    } 

      break;

    case (Scoreable.AIC): {
      fLogScore -= nCardinality * (numValues - 1);
    } 

      break;
    }

    return fLogScore;
  }    // CalcNodeScore
 
  /**
   * Main method for testing this class.
   * 
   * @param argv the options
   */
  public static void main(String[] argv) {
    try {
      System.out.println(Evaluation.evaluateModel(new BayesNet(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    } 
  }    // main
 
}      // class BayesNet




