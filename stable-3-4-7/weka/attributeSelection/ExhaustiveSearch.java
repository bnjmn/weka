/*
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

/*
 *    ExhaustiveSearch.java
 *    Copyright (C) 1999 Mark Hall
 *
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
 * -P <start set> <br>
 * Specify a starting set of attributes. Eg 1,4,7-9. <p>
 *
 * -V <br>
 * Verbose output. Output new best subsets as the search progresses. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.8.2.1 $
 */
public class ExhaustiveSearch extends ASSearch 
  implements StartSetHandler, OptionHandler {

  /** 
   * holds a starting set as an array of attributes.
   */
  private int[] m_starting;

  /** the start set as a Range */
  private Range m_startRange;

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
   * Returns a string describing this search method
   * @return a description of the search suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "ExhaustiveSearch : \n\nPerforms an exhaustive search through "
      +"the space of attribute subsets starting from the empty set of "
      +"attrubutes. Reports the best subset found. If a start set is "
      +"supplied, the algorithm searches backward from the start point "
      +"and reports the smallest subset with as good or better evaluation "
      +"as the start point.\n";
  }

  /**
   * Constructor
   */
  public ExhaustiveSearch () {
    resetOptions();
  }

  /**
   * Returns an enumeration describing the available options.
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(2);

    newVector.addElement(new Option("\tSpecify a starting set of attributes." 
				    + "\n\tEg. 1,3,5-7."
				    +"\n\tIf a start point is supplied,"
				    +"\n\tExhaustive search stops after"
				    +"\n\tfinding the smallest possible subset"
				    +"\n\twith merit as good as or better than"
				    +"\n\tthe start set."
				    ,"P",1
				    , "-P <start set>"));
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
   * -P <start set> <br>
   * Specify a starting set of attributes. Eg 1,4,7-9. <p>
   *
   * -V <br>
   * Verbose output. Output new best subsets as the search progresses. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions (String[] options)
    throws Exception {
    String optionString;
    resetOptions();

    optionString = Utils.getOption('P', options);
    if (optionString.length() != 0) {
      setStartSet(optionString);
    }

    setVerbose(Utils.getFlag('V',options));
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String startSetTipText() {
    return "Set the start point for the search. This is specified as a comma "
      +"seperated list off attribute indexes starting at 1. It can include "
      +"ranges. Eg. 1,2,5-9,17.";
  }

  /**
   * Sets a starting set of attributes for the search. It is the
   * search method's responsibility to report this start set (if any)
   * in its toString() method.
   * @param startSet a string containing a list of attributes (and or ranges),
   * eg. 1,2,6,10-15. "" indicates no start set.
   * If a start point is supplied, Exhaustive search stops after finding
   * the smallest possible subset with merit as good as or better than the
   * start set. Otherwise, the search space is explored FULLY, and the
   * best subset returned.
   * @exception Exception if start set can't be set.
   */
  public void setStartSet (String startSet) throws Exception {
    m_startRange.setRanges(startSet);
  }

  /**
   * Returns a list of attributes (and or attribute ranges) as a String
   * @return a list of attributes (and or attribute ranges)
   */
  public String getStartSet () {
    return m_startRange.getRanges();
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String verboseTipText() {
    return "Print progress information. Sends progress info to the terminal "
      +"as the search progresses.";
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
    String[] options = new String[3];
    int current = 0;

    if (!(getStartSet().equals(""))) {
      options[current++] = "-P";
      options[current++] = ""+startSetToString();
    }
	
    if (m_verbose) {
      options[current++] = "-V";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return  options;
  }

  /**
   * converts the array of starting attributes to a string. This is
   * used by getOptions to return the actual attributes specified
   * as the starting set. This is better than using m_startRanges.getRanges()
   * as the same start set can be specified in different ways from the
   * command line---eg 1,2,3 == 1-3. This is to ensure that stuff that
   * is stored in a database is comparable.
   * @return a comma seperated list of individual attribute numbers as a String
   */
  private String startSetToString() {
    StringBuffer FString = new StringBuffer();
    boolean didPrint;
    
    if (m_starting == null) {
      return getStartSet();
    }

    for (int i = 0; i < m_starting.length; i++) {
      didPrint = false;
      
      if ((m_hasClass == false) || 
	  (m_hasClass == true && i != m_classIndex)) {
	FString.append((m_starting[i] + 1));
	didPrint = true;
      }
      
      if (i == (m_starting.length - 1)) {
	FString.append("");
      }
      else {
	if (didPrint) {
	  FString.append(",");
	  }
      }
    }

    return FString.toString();
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
      text.append(startSetToString()+"\n");
    }
    text.append("\tNumber of evaluations: "+m_evaluations+"\n");
    text.append("\tMerit of best subset found: "
		+Utils.doubleToString(Math.abs(m_bestMerit),8,3)+"\n");

    return text.toString();
  }

  /**
   * Searches the attribute subset space using an exhaustive search.
   *
   * @param ASEvaluator the attribute evaluator to guide the search
   * @param data the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @exception Exception if the search can't be completed
   */
   public int[] search (ASEvaluation ASEval, Instances data)
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
     
     if (ASEval instanceof UnsupervisedSubsetEvaluator) {
       m_hasClass = false;
     }
     else {
       m_hasClass = true;
       m_classIndex = data.classIndex();
     }
     
     SubsetEvaluator ASEvaluator = (SubsetEvaluator)ASEval;
     m_numAttribs = data.numAttributes();

     m_startRange.setUpper(m_numAttribs-1);
     if (!(getStartSet().equals(""))) {
       m_starting = m_startRange.getSelection();
    }
     
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
     if (!done) {
       enumerateSizes: for (setSize = 1;setSize<=m_numAttribs;setSize++) {
	 // set up and evaluate initial subset of this size
         //	 subset = 0;
	 tempGroup = new BitSet(m_numAttribs);
	 for (i=0;i<setSize;i++) {
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
         while (tempGroup.cardinality() > 0) {
           generateNextSubset(setSize, tempGroup);
           if (tempGroup.cardinality() > 0) {
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
   * generates the next subset of size "size" given the subset "temp".
   * @param size the size of the feature subset (eg. 2 means that the 
   * current subset contains two features and the next generated subset
   * should also contain 2 features).
   * @param temp will hold the generated subset as a BitSet
   */
  private void generateNextSubset(int size, BitSet temp) {
    int i,j;
    int counter = 0;
    boolean done = false;
    BitSet temp2 = (BitSet)temp.clone();

    for (i=0;i<m_numAttribs;i++) {
      temp2.clear(i);
    }

    while ((!done) && (counter < size)) {
      for (i=m_numAttribs-1-counter;i>=0;i--) {
        if (temp.get(i)) {

          temp.clear(i);

          int newP;
	  if (i != (m_numAttribs-1-counter)) {
            newP = i+1;
            if (newP == m_classIndex) {
              newP++;
            }

            if (newP < m_numAttribs) {
              temp.set(newP);

              for (j=0;j<counter;j++) {
                if (newP+1+j == m_classIndex) {
                  newP++;
                }

                if (newP+1+j < m_numAttribs) {
                  temp.set(newP+1+j);
                }
              }
              done = true;
            } else {
              counter++;
            }
	    break;
	  } else {
	    counter++;
	    break;
	  }
	}
      }
    }

    if (temp.cardinality() < size) {
      temp.clear();
    }
    //    System.err.println(printSubset(temp).toString());
  }
      
  /**
   * resets to defaults
   */
  private void resetOptions() {
    m_starting = null;
    m_startRange = new Range();
    m_stopAfterFirst = false;
    m_verbose = false;
    m_evaluations = 0;
  }
}
