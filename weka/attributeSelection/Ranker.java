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

package weka.attributeSelection;
import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * Class for ranking the attributes evaluated by a AttributeEvaluator
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version 1.0 March 1999 (Mark)
 */
class Ranker extends RankedOutputSearch {

  private int [] starting;

  private int [] attributeList;

  private double [] attributeMerit;

  private boolean hasClass;

  private int classIndex;

  private int numAttribs;


  public Ranker()
  {
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
  public int [] search(int [] startSet,
		       ASEvaluation ASEval,
		       Instances data)
    throws Exception
  {
    int i,j;

    if (!(ASEval instanceof AttributeEvaluator))
      throw new Exception(ASEval.getClass().getName()+" is not a"+
			  "Attribute evaluator!");

    numAttribs = data.numAttributes();

    if (startSet != null)
      {
	starting = startSet;
      }

    if (ASEval instanceof UnsupervisedSubsetEvaluator)
      {
	hasClass = false;
      }
    else
      {
	hasClass = true;
	classIndex = data.classIndex();
      }

    int sl = 0;
    if (starting != null)
      sl = starting.length;

    if ((sl != 0) && (hasClass == true))
      {
	// see if the supplied list contains the class index
	boolean ok = false;
	for (i=0;i<sl;i++)
	  if (starting[i] == classIndex)
	    {
	      ok = true;
	      break;
	    }
	if (ok == false)
	  sl++;
      }
    else if (hasClass == true)
      sl++;

	
    attributeList = new int [numAttribs - sl];
    attributeMerit = new double [numAttribs - sl];

    // add in those attributes not in the starting (omit list)
    for (i=0,j=0;i<numAttribs;i++)
      if (!inStarting(i))
	attributeList[j++] = i;
	
    AttributeEvaluator ASEvaluator = (AttributeEvaluator)ASEval;

    for (i=0;i<attributeList.length;i++)
      {
	attributeMerit[i] = ASEvaluator.evaluateAttribute(attributeList[i]);
      }
    
    double [][] tempRanked = rankedAttributes();
    int [] rankedAttributes = new int [attributeList.length];
    for (i=0;i<attributeList.length;i++)
      rankedAttributes[i] = (int)tempRanked[i][0];

    return rankedAttributes;
  }

   /**
   * Sorts the evaluated attribute list
   *
   * @return an array of sorted (highest eval to lowest) attribute indexes
   * @exception Exception of sorting can't be done.
   */
  public double [][] rankedAttributes() throws Exception
  {
    int i,j;
    if (attributeList == null || attributeMerit == null)
      throw new Exception("Search must be performed before a ranked "+
			  "attribute list can be obtained");

    int [] ranked = Utils.sort(attributeMerit);

    // reverse the order of the ranked indexes
    double [][] bestToWorst = new double [ranked.length][2];
    for (i=ranked.length-1,j=0;i>=0;i--)
	bestToWorst[j++][0] = ranked[i];

    // convert the indexes to attribute indexes
    for (i=0;i<bestToWorst.length;i++)
      {
	int temp = ((int)bestToWorst[i][0]);
	bestToWorst[i][0] = attributeList[temp];
	bestToWorst[i][1] = attributeMerit[temp];
      }

    return bestToWorst;
  }

  /**
   * returns a description of the search as a String
   * @return a description of the search
   */
  public String toString()
  {
    StringBuffer BfString = new StringBuffer();
    
    BfString.append("\tAttribute ranking.\n");

    if (starting != null)
      {
	BfString.append("\tIgnored attributes: ");
	for (int i=0;i<starting.length;i++)
	  if (i == (starting.length-1))
	    BfString.append((starting[i]+1)+"\n");
	  else
	    BfString.append((starting[i]+1)+",");
      }

    return BfString.toString();
  }

  protected void resetOptions()
  {
     starting = null;
     attributeList = null;
     attributeMerit = null;
  }    

  private boolean inStarting(int feat)
  {
    // omit the class from the evaluation
    if ((hasClass == true) && (feat == classIndex))
      return true;

    if (starting == null)
      return false;
    
    for (int i=0;i<starting.length;i++)
      if (starting[i] == feat)
	return true;

    return false;
  }
  
}

