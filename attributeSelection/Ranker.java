/*
 *    Ranker.java
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
 * Class for ranking the attributes evaluated by a AttributeEvaluator
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $
 */
class Ranker
  extends RankedOutputSearch
{

  /** Holds the starting set of attributes if specified */
  private int[] m_starting;
  /** Holds the ordered list of attributes */
  private int[] m_attributeList;
  /** Holds the list of attribute merit scores */
  private double[] m_attributeMerit;
  /** Data has class attribute---if unsupervised evaluator then no class */
  private boolean m_hasClass;
  /** Class index of the data if supervised evaluator */
  private int m_classIndex;
  /** The number of attribtes */
  private int m_numAttribs;


  /**
   * Constructor
   */
  public Ranker () {
    resetOptions();
  }


  /**
   * Kind of a dummy search algorithm. Calls a Attribute evaluator to
   * evaluate each attribute not included in the startSet and then sorts
   * them to produce a ranked list of attributes.
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
    throws Exception
  {
    int i, j;

    if (!(ASEval instanceof AttributeEvaluator)) {
      throw  new Exception(ASEval.getClass().getName() 
			   + " is not a" 
			   + "Attribute evaluator!");
    }

    m_numAttribs = data.numAttributes();

    if (startSet != null) {
      m_starting = startSet;
    }

    if (ASEval instanceof UnsupervisedSubsetEvaluator) {
      m_hasClass = false;
    }
    else {
      m_hasClass = true;
      m_classIndex = data.classIndex();
    }

    int sl = 0;

    if (m_starting != null) {
      sl = m_starting.length;
    }

    if ((sl != 0) && (m_hasClass == true)) {
      // see if the supplied list contains the class index
      boolean ok = false;

      for (i = 0; i < sl; i++) {
        if (m_starting[i] == m_classIndex) {
          ok = true;
          break;
        }
      }

      if (ok == false) {
        sl++;
      }
    }
    else {if (m_hasClass == true) {
      sl++;
    }
    }

    m_attributeList = new int[m_numAttribs - sl];
    m_attributeMerit = new double[m_numAttribs - sl];

    // add in those attributes not in the starting (omit list)
    for (i = 0, j = 0; i < m_numAttribs; i++) {
      if (!inStarting(i)) {
	m_attributeList[j++] = i;
      }
    }

    AttributeEvaluator ASEvaluator = (AttributeEvaluator)ASEval;

    for (i = 0; i < m_attributeList.length; i++) {
      m_attributeMerit[i] = ASEvaluator.evaluateAttribute(m_attributeList[i]);
    }

    double[][] tempRanked = rankedAttributes();
    int[] rankedAttributes = new int[m_attributeList.length];

    for (i = 0; i < m_attributeList.length; i++) {
      rankedAttributes[i] = (int)tempRanked[i][0];
    }

    return  rankedAttributes;
  }


  /**
   * Sorts the evaluated attribute list
   *
   * @return an array of sorted (highest eval to lowest) attribute indexes
   * @exception Exception of sorting can't be done.
   */
  public double[][] rankedAttributes ()
    throws Exception
  {
    int i, j;

    if (m_attributeList == null || m_attributeMerit == null) {
      throw  new Exception("Search must be performed before a ranked " 
			   + "attribute list can be obtained");
    }

    int[] ranked = Utils.sort(m_attributeMerit);
    // reverse the order of the ranked indexes
    double[][] bestToWorst = new double[ranked.length][2];

    for (i = ranked.length - 1, j = 0; i >= 0; i--) {
      bestToWorst[j++][0] = ranked[i];
    }

    // convert the indexes to attribute indexes
    for (i = 0; i < bestToWorst.length; i++) {
      int temp = ((int)bestToWorst[i][0]);
      bestToWorst[i][0] = m_attributeList[temp];
      bestToWorst[i][1] = m_attributeMerit[temp];
    }

    return  bestToWorst;
  }


  /**
   * returns a description of the search as a String
   * @return a description of the search
   */
  public String toString () {
    StringBuffer BfString = new StringBuffer();
    BfString.append("\tAttribute ranking.\n");

    if (m_starting != null) {
      BfString.append("\tIgnored attributes: ");

      for (int i = 0; i < m_starting.length; i++) {
	if (i == (m_starting.length - 1)) {
	  BfString.append((m_starting[i] + 1) + "\n");
	}
	else {
	  BfString.append((m_starting[i] + 1) + ",");
	}
      }
    }

    return  BfString.toString();
  }


  /**
   * Resets stuff to default values
   */
  protected void resetOptions () {
    m_starting = null;
    m_attributeList = null;
    m_attributeMerit = null;
  }


  private boolean inStarting (int feat) {
    // omit the class from the evaluation
    if ((m_hasClass == true) && (feat == m_classIndex)) {
      return  true;
    }

    if (m_starting == null) {
      return  false;
    }

    for (int i = 0; i < m_starting.length; i++) {
      if (m_starting[i] == feat) {
	return  true;
      }
    }

    return  false;
  }

}

