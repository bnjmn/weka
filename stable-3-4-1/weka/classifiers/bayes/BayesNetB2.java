
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
 * BayesNetB2.java
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
 * Class for a Bayes Network classifier based on Buntines hill climbing algorithm for
 * learning structure, but augmented to allow arc reversal as an operation.
 * Works with nominal variables only.
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.3 $
 */
public class BayesNetB2 extends BayesNetB {

  /**
   * This will return a string describing the classifier.
   * @return The string.
   */
  public String globalInfo() {
    return "This Bayes Network learning algorithm uses Buntine's hill climbing "
      + "algorithm but augmented to allow arc reversal as an operation. "
      + "Works with nominal variables only.";
  }

  /**
   * buildStructure determines the network structure/graph of the network
   * with Buntines greedy hill climbing algorithm, restricted by its initial
   * structure (which can be an empty graph, or a Naive Bayes graph.
   */
  public void buildStructure() throws Exception {

    // determine base scores
    double[] fBaseScores = new double[m_Instances.numAttributes()];
    int      nNrOfAtts = m_Instances.numAttributes();

    for (int iAttribute = 0; iAttribute < nNrOfAtts; iAttribute++) {
      fBaseScores[iAttribute] = CalcNodeScore(iAttribute);
    } 

    // Determine initial structure by finding a good parent-set for classification
    // node using greedy search
    int     iAttribute = m_Instances.classIndex();
    double  fBestScore = fBaseScores[iAttribute];

    // /////////////////////////////////////////////////////////////////////////////////////////

    /*
     * int nBestAttribute1 = -1;
     * int nBestAttribute2 = -1;
     * for (int iAttribute1 = 0; iAttribute1 < m_Instances.numAttributes(); iAttribute1++) {
     * if (iAttribute != iAttribute1) {
     * for (int iAttribute2 = 0; iAttribute2 < iAttribute1; iAttribute2++) {
     * if (iAttribute != iAttribute2) {
     * m_ParentSets[iAttribute].AddParent(iAttribute1, m_Instances);
     * double fScore = CalcScoreWithExtraParent(iAttribute, iAttribute2);
     * m_ParentSets[iAttribute].DeleteLastParent(m_Instances);
     * if (fScore > fBestScore) {
     * fBestScore = fScore;
     * nBestAttribute1 = iAttribute1;
     * nBestAttribute2 = iAttribute2;
     * }
     * }
     * }
     * }
     * }
     * if (nBestAttribute1 != -1) {
     * m_ParentSets[iAttribute].AddParent(nBestAttribute1, m_Instances);
     * m_ParentSets[iAttribute].AddParent(nBestAttribute2, m_Instances);
     * fBaseScores[iAttribute] = fBestScore;
     * System.out.println("Added " +  nBestAttribute1 + " & " + nBestAttribute2);
     * }
     */
    int     m_nMaxNrOfClassifierParents = 4;

    // /////////////////////////////////////////////////////////////////////////////////////////
    // double fBestScore = CalcNodeScore(iAttribute);
    boolean bProgress = true;

    while (bProgress 
	   && m_ParentSets[iAttribute].GetNrOfParents() 
	      < m_nMaxNrOfClassifierParents) {
      int nBestAttribute = -1;

      for (int iAttribute2 = 0; iAttribute2 < m_Instances.numAttributes(); 
	   iAttribute2++) {
	if (iAttribute != iAttribute2) {
	  double fScore = CalcScoreWithExtraParent(iAttribute, iAttribute2);

	  if (fScore > fBestScore) {
	    fBestScore = fScore;
	    nBestAttribute = iAttribute2;
	  } 
	} 
      } 

      if (nBestAttribute != -1) {
	m_ParentSets[iAttribute].AddParent(nBestAttribute, m_Instances);

	fBaseScores[iAttribute] = fBestScore;
      } else {
	bProgress = false;
      } 
    } 

    // Recalc Base scores
    // Correction for Naive Bayes structures: delete arcs from classification node to children
    for (int iParent = 0; 
	 iParent < m_ParentSets[iAttribute].GetNrOfParents(); iParent++) {
      int nParentNode = m_ParentSets[iAttribute].GetParent(iParent);

      if (IsArc(nParentNode, iAttribute)) {
	m_ParentSets[nParentNode].DeleteLastParent(m_Instances);
      } 

      // recalc base scores
      fBaseScores[nParentNode] = CalcNodeScore(nParentNode);
    } 

    // super.buildStructure();
    // Do algorithm B from here onwards
    // cache scores & whether adding an arc makes sense
    boolean[][] bAddArcMakesSense = new boolean[nNrOfAtts][nNrOfAtts];
    double[][]  fScore = new double[nNrOfAtts][nNrOfAtts];

    for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; 
	 iAttributeHead++) {
      if (m_ParentSets[iAttributeHead].GetNrOfParents() < m_nMaxNrOfParents) {

	// only bother maintaining scores if adding parent does not violate the upper bound on nr of parents
	for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; 
	     iAttributeTail++) {
	  bAddArcMakesSense[iAttributeHead][iAttributeTail] = 
	    AddArcMakesSense(iAttributeHead, iAttributeTail);

	  if (bAddArcMakesSense[iAttributeHead][iAttributeTail]) {
	    fScore[iAttributeHead][iAttributeTail] = 
	      CalcScoreWithExtraParent(iAttributeHead, iAttributeTail);
	  } 
	} 
      } 
    } 

    bProgress = true;

    // go do the hill climbing
    while (bProgress) {
      bProgress = false;

      int    nBestAttributeTail = -1;
      int    nBestAttributeHead = -1;
      double fBestDeltaScore = 0.0;

      // find best arc to add
      for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; 
	   iAttributeHead++) {
	if (m_ParentSets[iAttributeHead].GetNrOfParents() 
		< m_nMaxNrOfParents) {
	  for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; 
	       iAttributeTail++) {
	    if (bAddArcMakesSense[iAttributeHead][iAttributeTail]) {

	      // System.out.println("gain " +  iAttributeTail + " -> " + iAttributeHead + ": "+ (fScore[iAttributeHead][iAttributeTail] - fBaseScores[iAttributeHead]));
	      if (fScore[iAttributeHead][iAttributeTail] 
		      - fBaseScores[iAttributeHead] > fBestDeltaScore) {
		if (AddArcMakesSense(iAttributeHead, iAttributeTail)) {
		  fBestDeltaScore = fScore[iAttributeHead][iAttributeTail] 
				    - fBaseScores[iAttributeHead];
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
	// System.out.println("Added " + nBestAttributeTail + " -> " + nBestAttributeHead);
	m_ParentSets[nBestAttributeHead].AddParent(nBestAttributeTail, 
						   m_Instances);

	if (m_ParentSets[nBestAttributeHead].GetNrOfParents() 
		< m_nMaxNrOfParents) {

	  // only bother updating scores if adding parent does not violate the upper bound on nr of parents
	  fBaseScores[nBestAttributeHead] += fBestDeltaScore;

	  // System.out.println(fScore[nBestAttributeHead][nBestAttributeTail] + " " + fBaseScores[nBestAttributeHead] + " " + fBestDeltaScore);
	  for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; 
	       iAttributeTail++) {
	    bAddArcMakesSense[nBestAttributeHead][iAttributeTail] = 
	      AddArcMakesSense(nBestAttributeHead, iAttributeTail);

	    if (bAddArcMakesSense[nBestAttributeHead][iAttributeTail]) {
	      fScore[nBestAttributeHead][iAttributeTail] = 
		CalcScoreWithExtraParent(nBestAttributeHead, iAttributeTail);

	      // System.out.println(iAttributeTail + " -> " + nBestAttributeHead + ": " + fScore[nBestAttributeHead][iAttributeTail]);
	    } 
	  } 
	} 

	bProgress = true;
      } 
    } 
  }    // buildStructure
 
  /**
   * IsArc checks whether the arc from iAttributeTail to iAttributeHead already exists
   * 
   * @param index of the attribute that becomes head of the arrow
   * @param index of the attribute that becomes tail of the arrow
   */
  private boolean IsArc(int iAttributeHead, int iAttributeTail) {
    for (int iParent = 0; 
	 iParent < m_ParentSets[iAttributeHead].GetNrOfParents(); iParent++) {
      if (m_ParentSets[iAttributeHead].GetParent(iParent) == iAttributeTail) {
	return true;
      } 
    } 

    return false;
  }    // IsArc
 
  /**
   * AddArcMakesSense checks whether adding the arc from iAttributeTail to iAttributeHead
   * does not already exists and does not introduce a cycle
   * 
   * @param index of the attribute that becomes head of the arrow
   * @param index of the attribute that becomes tail of the arrow
   */
  private boolean AddArcMakesSense(int iAttributeHead, int iAttributeTail) {
    if (iAttributeHead == iAttributeTail) {
      return false;
    } 

    // sanity check: arc should not be in parent set already
    if (IsArc(iAttributeHead, iAttributeTail)) {
      return false;
    } 

    // sanity check: arc should not introduce a cycle
    int       nNodes = m_Instances.numAttributes();
    boolean[] bDone = new boolean[nNodes];

    for (int iNode = 0; iNode < nNodes; iNode++) {
      bDone[iNode] = false;
    } 

    // check for cycles
    m_ParentSets[iAttributeHead].AddParent(iAttributeTail, m_Instances);

    for (int iNode = 0; iNode < nNodes; iNode++) {

      // find a node for which all parents are 'done'
      boolean bFound = false;

      for (int iNode2 = 0; !bFound && iNode2 < nNodes; iNode2++) {
	if (!bDone[iNode2]) {
	  boolean bHasNoParents = true;

	  for (int iParent = 0; 
	       iParent < m_ParentSets[iNode2].GetNrOfParents(); iParent++) {
	    if (!bDone[m_ParentSets[iNode2].GetParent(iParent)]) {
	      bHasNoParents = false;
	    } 
	  } 

	  if (bHasNoParents) {
	    bDone[iNode2] = true;
	    bFound = true;
	  } 
	} 
      } 

      if (!bFound) {
	m_ParentSets[iAttributeHead].DeleteLastParent(m_Instances);

	return false;
      } 
    } 

    m_ParentSets[iAttributeHead].DeleteLastParent(m_Instances);

    return true;
  }    // AddArcMakesCycle
 
  /**
   * ReverseArcMakesCycle checks whether the arc from iAttributeTail to
   * iAttributeHead exists and reversing does not introduce a cycle
   * 
   * @param index of the attribute that is head of the arrow
   * @param index of the attribute that is tail of the arrow
   */
  private boolean ReverseArcMakesCycle(int iAttributeHead, 
				       int iAttributeTail) {
    if (iAttributeHead == iAttributeTail) {
      return false;
    } 

    // sanity check: arc should be in parent set already
    if (!IsArc(iAttributeHead, iAttributeTail)) {
      return false;
    } 

    // sanity check: arc should not introduce a cycle
    int       nNodes = m_Instances.numAttributes();
    boolean[] bDone = new boolean[nNodes];

    for (int iNode = 0; iNode < nNodes; iNode++) {
      bDone[iNode] = false;
    } 

    // check for cycles
    m_ParentSets[iAttributeTail].AddParent(iAttributeHead, m_Instances);

    for (int iNode = 0; iNode < nNodes; iNode++) {

      // find a node for which all parents are 'done'
      boolean bFound = false;

      for (int iNode2 = 0; !bFound && iNode2 < nNodes; iNode2++) {
	if (!bDone[iNode2]) {
	  boolean bHasNoParents = true;

	  for (int iParent = 0; 
	       iParent < m_ParentSets[iNode2].GetNrOfParents(); iParent++) {
	    if (!bDone[m_ParentSets[iNode2].GetParent(iParent)]) {

	      // this one has a parent which is not 'done' UNLESS it is the arc to be reversed
	      if (iNode2 != iAttributeHead 
		      || m_ParentSets[iNode2].GetParent(iParent) 
			 != iAttributeTail) {
		bHasNoParents = false;
	      } 
	    } 
	  } 

	  if (bHasNoParents) {
	    bDone[iNode2] = true;
	    bFound = true;
	  } 
	} 
      } 

      if (!bFound) {
	m_ParentSets[iAttributeTail].DeleteLastParent(m_Instances);

	return false;
      } 
    } 

    m_ParentSets[iAttributeTail].DeleteLastParent(m_Instances);

    return true;
  }    // ReverseArcMakesCycle
 
  /**
   * Main method for testing this class.
   * 
   * @param argv the options
   */
  public static void main(String[] argv) {
    try {
      System.out.println(Evaluation.evaluateModel(new BayesNetB2(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    } 
  }    // main
 
}      // class BayesNetB2




