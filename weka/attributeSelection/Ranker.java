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
 *
 * Valid options are: <p>
 *
 * -T <threshold> <br>
 * Specify a threshold by which the AttributeSelection module can. <br>
 * discard attributes. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.7 $
 */
public class Ranker extends ASSearch 
  implements RankedOutputSearch, OptionHandler {

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
   * A threshold by which to discard attributes---used by the
   * AttributeSelection module
   */
  private double m_threshold;


  /**
   * Constructor
   */
  public Ranker () {
    resetOptions();
  }

  /**
   * Set the threshold by which the AttributeSelection module can discard
   * attributes.
   * @param threshold the threshold.
   */
  public void setThreshold(double threshold) {
    m_threshold = threshold;
  }

  /**
   * Returns the threshold so that the AttributeSelection module can
   * discard attributes from the ranking.
   */
  public double getThreshold() {
    return m_threshold;
  }

  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(1);
    newVector
      .addElement(new Option("\tSpecify a theshold by which attributes" 
			     + "\tmay be discarded from the ranking.","T",1
			     , "-T <threshold>"));

    return newVector.elements();

  }
  
  /**
   * Parses a given list of options.
   *
   * Valid options are: <p>
   *
   * -T <threshold> <br>
   * Specify a threshold by which the AttributeSelection module can. <br>
   * discard attributes. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions (String[] options)
    throws Exception
  {
    String optionString;
    resetOptions();

    optionString = Utils.getOption('T', options);
    if (optionString.length() != 0) {
      Double temp;
      temp = Double.valueOf(optionString);
      setThreshold(temp.doubleValue());
    }
  }

  /**
   * Gets the current settings of ReliefFAttributeEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[2];
    int current = 0;

    options[current++] = "-T";
    options[current++] = "" + getThreshold();

    while (current < options.length) {
      options[current++] = "";
    }
    return  options;
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

    if (m_threshold != -Double.MAX_VALUE) {
      BfString.append("\tThreshold for discarding attributes: "
		      + Utils.doubleToString(m_threshold,8,4)+"\n");
    }

    return BfString.toString();
  }


  /**
   * Resets stuff to default values
   */
  protected void resetOptions () {
    m_starting = null;
    m_attributeList = null;
    m_attributeMerit = null;
    m_threshold = -Double.MAX_VALUE;
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

