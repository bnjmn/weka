/*
 *    ForwardSelection.java
 *    Copyright (C) 1999 Mark Hall
 *
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
package  weka.attributeSelection;

import  java.io.*;
import  java.util.*;
import  weka.core.*;

/** 
 * Class for performing a forward selection hill climbing search. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class ForwardSelection extends RankedOutputSearch {

  /** holds a starting set (if one is supplied) */
  private int[] m_starting;
  
 /** does the data have a class */
  private boolean m_hasClass;
 
  /** holds the class index */
  private int m_classIndex;
 
  /** number of attributes in the data */
  private int m_numAttribs;

  /** 
   * go from one side of the search space to the other in order to generate
   * a ranking
   */
  private boolean m_doRank;

  /** a ranked list of attribute indexes */
  private double [][] m_rankedAtts;
  private int m_rankedSoFar;

  private BitSet m_best_group;
  private ASEvaluation m_ASEval;

  private Instances m_Instances;

  public ForwardSelection () {
    resetOptions();
  }

  public String toString() {
    StringBuffer FString = new StringBuffer();
    FString.append("\tForward Selection.\n\tStart set: ");

    if (m_starting == null) {
      FString.append("no attributes\n");
    }
    else {
      boolean didPrint;

      for (int i = 0; i < m_starting.length; i++) {
	didPrint = false;

	if ((m_hasClass == false) || 
	    (m_hasClass == true && i != m_classIndex)) {
	  FString.append((m_starting[i] + 1));
	  didPrint = true;
	}

	if (i == (m_starting.length - 1)) {
	  FString.append("\n");
	}
	else {
	  if (didPrint) {
	    FString.append(",");
	  }
	}
      }
    }
    return FString.toString();
  }


  /**
   * Searches the attribute subset space by forward selection.
   *
   * @param startSet a (possibly) ordered array of attribute indexes from
   * which to start the search from. Set to null if no explicit start
   * point.
   * @param ASEvaluator the attribute evaluator to guide the search
   * @param data the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @exception Exception if the search can't be completed
   */
  public int[] search (int[] startSet, ASEvaluation ASEval, Instances data)
    throws Exception {
    
    int i;
    double best_merit = -Double.MAX_VALUE;
    double temp_best,temp_merit;
    int temp_index=0;
    BitSet temp_group;

    if (m_ASEval == null) {
      m_ASEval = ASEval;
    }

    if (m_Instances == null) {
      m_Instances = data;
    }

    m_numAttribs = m_Instances.numAttributes();

    if (m_best_group == null) {
      m_best_group = new BitSet(m_numAttribs);
    }

    if (!(m_ASEval instanceof SubsetEvaluator)) {
      throw  new Exception(m_ASEval.getClass().getName() 
			   + " is not a " 
			   + "Subset evaluator!");
    }

    if (startSet != null) {
      m_starting = startSet;
    }

    if (m_ASEval instanceof UnsupervisedSubsetEvaluator) {
      m_hasClass = false;
    }
    else {
      m_hasClass = true;
      m_classIndex = m_Instances.classIndex();
    }

    SubsetEvaluator ASEvaluator = (SubsetEvaluator)m_ASEval;

    if (m_rankedAtts == null) {
      m_rankedAtts = new double[m_numAttribs][2];
      m_rankedSoFar = 0;
    }

    // If a starting subset has been supplied, then initialise the bitset
    if (m_starting != null) {
      for (i = 0; i < m_starting.length; i++) {
	if ((m_starting[i]) != m_classIndex) {
	  m_best_group.set(m_starting[i]);
	}
      }
    }

    // Evaluate the initial subset
    best_merit = ASEvaluator.evaluateSubset(m_best_group);

    // main search loop
    boolean done = false;
    boolean addone = false;
    while (!done) {
      temp_group = (BitSet)m_best_group.clone();
      temp_best = best_merit;
      if (m_doRank) {
	temp_best = -Double.MAX_VALUE;
      }
      done = true;
      addone = false;
      for (i=0;i<m_numAttribs;i++) {
	if ((i != m_classIndex) && (!temp_group.get(i))) {
	  // set the bit
	  temp_group.set(i);
	  temp_merit = ASEvaluator.evaluateSubset(temp_group);
	  if (temp_merit > temp_best) {
	    temp_best = temp_merit;
	    temp_index = i;
	    addone = true;
	    done = false;
	  }

	  // unset the bit
	  temp_group.clear(i);
	  if (m_doRank) {
	    done = false;
	  }
	}
      }
      if (addone) {
	m_best_group.set(temp_index);
	best_merit = temp_best;
	m_rankedAtts[m_rankedSoFar][0] = temp_index;
	m_rankedAtts[m_rankedSoFar][1] = best_merit;
	m_rankedSoFar++;
      }
    }
    
    return attributeList(m_best_group);
  }

  /**
   * Produces a ranked list of attributes. Search must have been performed
   * prior to calling this function. Search is called by this function to
   * complete the traversal of the the search space. A list of
   * attributes and merits are returned. The attributes a ranked by the
   * order they are added to the subset during a forward selection search.
   * Individual merit values reflect the merit associated with adding the
   * corresponding attribute to the subset; because of this, merit values
   * will initially increase but then decrease as the best subset is
   * "passed by" on the way to the far side of the search space.
   *
   * @return an array of attribute indexes and associated merit values
   * @exception if something goes wrong.
   */
  public double [][] rankedAttributes() throws Exception {
    
    if (m_rankedAtts == null || m_rankedSoFar == -1) {
      throw new Exception("Search must be performed before attributes "
			  +"can be ranked.");
    }
    m_doRank = true;
    search (null, m_ASEval, null);

    double [][] final_rank = new double [m_rankedSoFar][2];
    for (int i=0;i<m_rankedSoFar;i++) {
      final_rank[i][0] = m_rankedAtts[i][0];
      final_rank[i][1] = m_rankedAtts[i][1];
    }
    
    resetOptions();
    return final_rank;
  }

  /**
   * converts a BitSet into a list of attribute indexes 
   * @param group the BitSet to convert
   * @return an array of attribute indexes
   **/
  private int[] attributeList (BitSet group) {
    int count = 0;

    // count how many were selected
    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
	count++;
      }
    }

    int[] list = new int[count];
    count = 0;

    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
	list[count++] = i;
      }
    }

    return  list;
  }

  /**
   * Resets options
   */
  private void resetOptions() {
    m_doRank = false;
    m_starting = null;
    m_best_group = null;
    m_ASEval = null;
    m_Instances = null;
    m_rankedSoFar = -1;
    m_rankedAtts = null;
  }
}
