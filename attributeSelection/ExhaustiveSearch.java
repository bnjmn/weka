/*
 *    ExhaustiveSearch.java
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
 * Class for performing an exhaustive search. <p>
 *
 * Valid options are: <p>
 *
 * -V <br>
 * Verbose output. Output new best subsets as the search progresses. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class ExhaustiveSearch extends ASSearch implements OptionHandler {

  /** 
   * holds a starting set (if one is supplied). Becomes one member of the
   * initial random population
   */
  private int[] m_starting;

  /** the best feature set found during the search */
  private BitSet m_bestGroup;

  /** the merit of the best subset found */
  private double m_bestMerit;

 /** does the data have a class */
  private boolean m_hasClass;
 
  /** holds the class index */
  private int m_classIndex;
 
  /** number of attributes in the data */
  private int m_numAttribs;

  /** if true, then ouput new best subsets as the search progresses */
  private boolean m_verbose;

  /** 
   * stop after finding the first subset equal to or better than the
   * supplied start set (set to true if start set is supplied).
   */
  private boolean m_stopAfterFirst;
  
  /** the number of subsets evaluated during the search */
  private int m_evaluations;

  /**
   * Constructor
   */
  public ExhaustiveSearch () {
    resetOptions();
  }

  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(1);
    newVector.addElement(new Option("\tOutput subsets as the search progresses."
				    +"\n\t(default = false)."
				    , "V", 0
				    , "-V"));
    return  newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   * Valid options are: <p>
   *
   * -V <br>
   * Verbose output. Output new best subsets as the search progresses. <p>
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
    
    setVerbose(Utils.getFlag('V',options));
  }

  /**
   * set whether or not to output new best subsets as the search proceeds
   * @param v true if output is to be verbose
   */
  public void setVerbose(boolean v) {
    m_verbose = v;
  }

  /**
   * get whether or not output is verbose
   * @return true if output is set to verbose
   */
  public boolean getVerbose() {
    return m_verbose;
  }

  /**
   * Gets the current settings of RandomSearch.
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[1];
    int current = 0;

    if (m_verbose) {
      options[current++] = "-V";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return  options;
  }

  /**
   * prints a description of the search
   * @return a description of the search as a string
   */
  public String toString() {
    StringBuffer text = new StringBuffer();
    
    text.append("\tExhaustive Search.\n\tStart set: ");
    if (m_starting == null) {
      text.append("no attributes\n");
    }
    else {
      boolean didPrint;

      for (int i = 0; i < m_starting.length; i++) {
	didPrint = false;

	if ((m_hasClass == false) || 
	    (m_hasClass == true && i != m_classIndex)) {
	  text.append((m_starting[i] + 1));
	  didPrint = true;
	}

	if (i == (m_starting.length - 1)) {
	  text.append("\n");
	}
	else {
	  if (didPrint) {
	    text.append(",");
	  }
	}
      }
    }
    text.append("\tNumber of evaluations: "+m_evaluations+"\n");
    text.append("\tMerit of best subset found: "
		+Utils.doubleToString(Math.abs(m_bestMerit),8,3)+"\n");

    return text.toString();
  }

  /**
   * Searches the attribute subset space using a genetic algorithm.
   *
   * @param startSet a (possibly) ordered array of attribute indexes from
   * which to start the search from. Set to null if no explicit start
   * point. If a start point is supplied, Exhaustive search stops after finding
   * the smallest possible subset with merit as good as or better than the
   * start set. Otherwise, the search space is explored FULLY, and the
   * best subset returned.
   *
   * @param ASEvaluator the attribute evaluator to guide the search
   * @param data the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @exception Exception if the search can't be completed
   */
   public int[] search (int[] startSet, ASEvaluation ASEval, Instances data)
     throws Exception {
     double best_merit;
     double tempMerit;
     int setSize;
     boolean done = false;
     int sizeOfBest;
     int tempSize;
     
     m_numAttribs = data.numAttributes();
     m_bestGroup = new BitSet(m_numAttribs);
     
     if (!(ASEval instanceof SubsetEvaluator)) {
       throw  new Exception(ASEval.getClass().getName() 
			    + " is not a " 
			    + "Subset evaluator!");
     }
     
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
     
     SubsetEvaluator ASEvaluator = (SubsetEvaluator)ASEval;
     m_numAttribs = data.numAttributes();

     // If a starting subset has been supplied, then initialise the bitset
     if (m_starting != null) {
       m_stopAfterFirst = true;
       for (int i = 0; i < m_starting.length; i++) {
	 if ((m_starting[i]) != m_classIndex) {
	   m_bestGroup.set(m_starting[i]);
	 }
       }
     }
     best_merit = ASEvaluator.evaluateSubset(m_bestGroup);
     m_evaluations++;
     sizeOfBest = countFeatures(m_bestGroup);

     if (m_verbose) {
       if (m_stopAfterFirst) {
	 System.out.println("Initial subset ("
			    +Utils.doubleToString(Math.
						  abs(best_merit),8,5)
			    +"): "+printSubset(m_bestGroup));
       }
     }

     BitSet tempGroup = new BitSet(m_numAttribs);
     tempMerit = ASEvaluator.evaluateSubset(tempGroup);

     if (m_verbose) {
       System.out.println("Zero feature subset ("
			  +Utils.doubleToString(Math.
						abs(tempMerit),8,5)
			  +")");
     }

     if (tempMerit >= best_merit) {
       tempSize = countFeatures(tempGroup);
       if (tempMerit > best_merit || 
	   (tempSize < sizeOfBest)) {
	 best_merit = tempMerit;
	 m_bestGroup = (BitSet)(tempGroup.clone());
	 sizeOfBest = tempSize;
       }
       if (m_stopAfterFirst) {
	 done = true;
       }
     }

     int i,j;
     int subset;
     if (!done) {
       enumerateSizes: for (setSize = 1;setSize<=m_numAttribs;setSize++) {
	 // set up and evaluate initial subset of this size
	 subset = 0;
	 tempGroup = new BitSet(m_numAttribs);
	 for (i=0;i<setSize;i++) {
	   subset = (subset ^ (1<<i));
	   tempGroup.set(i);
	   if (m_hasClass && i == m_classIndex) {
	     tempGroup.clear(i);
	   }
	 }
	 tempMerit = ASEvaluator.evaluateSubset(tempGroup);
	 m_evaluations++;
	 if (tempMerit >= best_merit) {
	   tempSize = countFeatures(tempGroup);
	   if (tempMerit > best_merit || 
	       (tempSize < sizeOfBest)) {
	     best_merit = tempMerit;
	     m_bestGroup = (BitSet)(tempGroup.clone());
	     sizeOfBest = tempSize;
	     if (m_verbose) {
	       System.out.println("New best subset ("
				+Utils.doubleToString(Math.
						      abs(best_merit),8,5)
				  +"): "+printSubset(m_bestGroup));
	     }
	   }
	   if (m_stopAfterFirst) {
	     done = true;
	     break enumerateSizes;
	   }
	 }
	 // generate all the other subsets of this size
	 while (subset > 0) {
	   subset = generateNextSubset(subset, setSize, tempGroup);
	   if (subset > 0) {
	     tempMerit = ASEvaluator.evaluateSubset(tempGroup);
	     m_evaluations++;
	     if (tempMerit >= best_merit) {
	       tempSize = countFeatures(tempGroup);
	       if (tempMerit > best_merit || 
		   (tempSize < sizeOfBest)) {
		 best_merit = tempMerit;
		 m_bestGroup = (BitSet)(tempGroup.clone());
		 sizeOfBest = tempSize;
		 if (m_verbose) {
		   System.out.println("New best subset ("
				      +Utils.
				      doubleToString(Math.
						     abs(best_merit),8,5)
				      +"): "+printSubset(m_bestGroup));
		 }
	       }
	       if (m_stopAfterFirst) {
		 done = true;
		 break enumerateSizes;
	       }
	     }
	   }
	 }
       }
     }   
     m_bestMerit = best_merit;
     
     return attributeList(m_bestGroup);
   }

  /**
   * counts the number of features in a subset
   * @param featureSet the feature set for which to count the features
   * @return the number of features in the subset
   */
  private int countFeatures(BitSet featureSet) {
    int count = 0;
    for (int i=0;i<m_numAttribs;i++) {
      if (featureSet.get(i)) {
	count++;
      }
    }
    return count;
  }   

  /**
   * prints a subset as a series of attribute numbers
   * @param temp the subset to print
   * @return a subset as a String of attribute numbers
   */
  private String printSubset(BitSet temp) {
    StringBuffer text = new StringBuffer();

    for (int j=0;j<m_numAttribs;j++) {
      if (temp.get(j)) {
        text.append((j+1)+" ");
      }
    }
    return text.toString();
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
   * generates the next subset of size "size" given the subset "set"
   * coded as an integer. The next subset is returned (as an Integer) 
   * and temp contains this subset as a BitSet.
   * @param set the current subset coded as an integer
   * @param size the size of the feature subset (eg. 2 means that the 
   * current subset contains two features and the next generated subset
   * should also contain 2 features).
   * @param temp will hold the generated subset as a BitSet
   */
  private int generateNextSubset(int set, int size, BitSet temp) {
    int i,j;
    int counter = 0;
    boolean done = false;

    for (i=0;i<m_numAttribs;i++) {
      temp.clear(i);
    }

    while ((!done) && (counter < size)) {
      for (i=m_numAttribs-1-counter;i>=0;i--) {
	if ((set & (1<<i)) !=0) {
	  // erase and move
	  set = (set ^ (1<<i));

	  if (i != (m_numAttribs-1-counter)) {
	    set = (set ^ (1<<i+1));
	    for (j=0;j<counter;j++) {
	      set = (set ^ (1<<(i+2+j)));
	    }
	    done = true;
	    break;
	  } else {
	    counter++;
	    break;
	  }
	}
      }
    }

    for (i=m_numAttribs-1;i>=0;i--) {
      if ((set & (1<<i)) != 0) {
	if (i != m_classIndex) {
	  temp.set(i);
	}
      }
    }
    return set;
  }
      
  /**
   * resets to defaults
   */
  private void resetOptions() {
    m_starting = null;
    m_stopAfterFirst = false;
    m_verbose = false;
    m_evaluations = 0;
  }
}
