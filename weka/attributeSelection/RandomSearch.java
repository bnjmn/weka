/*
 *    RandomSearch.java
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
 * Class for performing a random search. <p>
 *
 * Valid options are: <p>
 *
 * -P <percent) <br>
 * Percentage of the search space to consider. (default = 25). <p>
 *
 * -V <br>
 * Verbose output. Output new best subsets as the search progresses. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class RandomSearch extends ASSearch implements OptionHandler {

  /** 
   * holds a starting set (if one is supplied). Becomes one member of the
   * initial random population
   */
  private int[] m_starting;

  /** the best feature set found during the search */
  private BitSet m_bestGroup;

  /** the merit of the best subset found */
  private double m_bestMerit;

  /** 
   * only accept a feature set as being "better" than the best if its
   * merit is better or equal to the best, and it contains fewer
   * features than the best (this allows LVF to be implimented).
   */
  private boolean m_onlyConsiderBetterAndSmaller;

 /** does the data have a class */
  private boolean m_hasClass;
 
  /** holds the class index */
  private int m_classIndex;
 
  /** number of attributes in the data */
  private int m_numAttribs;

  /** seed for random number generation */
  private int m_seed;

  /** percentage of the search space to consider */
  private double m_searchSize;

  /** the number of iterations performed */
  private int m_iterations;

  /** random number object */
  private Random m_random;

  /** output new best subsets as the search progresses */
  private boolean m_verbose;

  public RandomSearch () {
    resetOptions();
  }

  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(3);
    newVector.addElement(new Option("\tPercent of search space to consider."
				    +"\n\t(default = 25%)."
				    , "P", 1
				    , "-P <percent> "));
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
   * -P <percent) <br>
   * Percentage of the search space to consider. (default = 25). <p>
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
    
    optionString = Utils.getOption('P',options);
    if (optionString.length() != 0) {
      setSearchPercent((new Double(optionString)).doubleValue());
    }

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
   * set the percentage of the search space to consider
   * @param p percent of the search space ( 0 < p <= 100)
   */
  public void setSearchPercent(double p) {
    p = Math.abs(p);
    if (p == 0) {
      p = 25;
    }

    if (p > 100.0) {
      p = 100;
    }

    m_searchSize = (p/100.0);
  }

  /**
   * get the percentage of the search space to consider
   * @return the percent of the search space explored
   */
  public double getSearchPercent() {
    return m_searchSize;
  }

  /**
   * Gets the current settings of RandomSearch.
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[3];
    int current = 0;

    if (m_verbose) {
      options[current++] = "-V";
    }

    options[current++] = "-P";
    options[current++] = "" + m_searchSize;

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
    
    text.append("\tRandom search.\n\tStart set: ");
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
    text.append("\tNumber of iterations: "+m_iterations+" ("
		+(m_searchSize * 100.0)+"% of the search space)\n");
    text.append("\tMerit of best subset found: "
		+Utils.doubleToString(Math.abs(m_bestMerit),8,3)+"\n");

    return text.toString();
  }

  /**
   * Searches the attribute subset space using a genetic algorithm.
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
     double best_merit;
     int sizeOfBest = m_numAttribs;
     BitSet temp;
     m_bestGroup = new BitSet(m_numAttribs);
     
     m_onlyConsiderBetterAndSmaller = false;
     if (!(ASEval instanceof SubsetEvaluator)) {
       throw  new Exception(ASEval.getClass().getName() 
			    + " is not a " 
			    + "Subset evaluator!");
     }

     m_random = new Random(m_seed);
     
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
       for (int i = 0; i < m_starting.length; i++) {
	 if ((m_starting[i]) != m_classIndex) {
	   m_bestGroup.set(m_starting[i]);
	 }
       }
       m_onlyConsiderBetterAndSmaller = true;
       best_merit = ASEvaluator.evaluateSubset(m_bestGroup);
       sizeOfBest = countFeatures(m_bestGroup);
     } else {
       // do initial random subset
       m_bestGroup = generateRandomSubset();
       best_merit = ASEvaluator.evaluateSubset(m_bestGroup);
     }
     
     if (m_verbose) {
       System.out.println("Initial subset ("
			  +Utils.doubleToString(Math.
						abs(best_merit),8,5)
			  +"): "+printSubset(m_bestGroup));
     }

     int i;
     if (m_hasClass) {
       i = m_numAttribs -1;
     } else {
       i = m_numAttribs;
     }
     m_iterations = (int)((m_searchSize * Math.pow(2, i)));
     
     int tempSize;
     double tempMerit;
     // main loop
     for (i=0;i<m_iterations;i++) {
       temp = generateRandomSubset();
       if (m_onlyConsiderBetterAndSmaller) {
	 tempSize = countFeatures(temp);
	 if (tempSize <= sizeOfBest) {
	   tempMerit = ASEvaluator.evaluateSubset(temp);
	   if (tempMerit >= best_merit) {
	     sizeOfBest = tempSize;
	     m_bestGroup = temp;
	     best_merit = tempMerit;
	     if (m_verbose) {
	       System.out.print("New best subset ("
				  +Utils.doubleToString(Math.
							abs(best_merit),8,5)
				  +"): "+printSubset(m_bestGroup) + " :");
	       System.out.println(Utils.
				  doubleToString((((double)i)/
						  ((double)m_iterations)*
						  100.0),5,1)
				  +"% done");
	     }
	   }
	 }
       } else {
	 tempMerit = ASEvaluator.evaluateSubset(temp);
	 if (tempMerit > best_merit) {
	   m_bestGroup = temp;
	   best_merit = tempMerit;
	   if (m_verbose) {
	     System.out.print("New best subset ("
				+Utils.doubleToString(Math.abs(best_merit),8,5)
				+"): "+printSubset(m_bestGroup) + " :");
	     System.out.println(Utils.
				doubleToString((((double)i)/
						((double)m_iterations)
						*100.0),5,1)
				+"% done");
	   }
	 }
       }
     }
     m_bestMerit = best_merit;
     return attributeList(m_bestGroup);
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
   * generates a random subset
   * @return a random subset as a BitSet
   */
  private BitSet generateRandomSubset() {
    BitSet temp = new BitSet(m_numAttribs);
    double r;

    for (int i=0;i<m_numAttribs;i++) {
      r = m_random.nextDouble();
      if (r <= 0.5) {
	if (m_hasClass && i == m_classIndex) {
	} else {
	  temp.set(i);
	}
      }
    }
    return temp;
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
   * resets to defaults
   */
  private void resetOptions() {
    m_starting = null;
    m_searchSize = 0.25;
    m_seed = 1;
    m_onlyConsiderBetterAndSmaller = false;
    m_verbose = false;
  }
}
