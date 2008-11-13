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
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */

package  weka.attributeSelection;

import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Vector;

/** 
 <!-- globalinfo-start -->
 * ExhaustiveSearch : <br/>
 * <br/>
 * Performs an exhaustive search through the space of attribute subsets starting from the empty set of attrubutes. Reports the best subset found.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -V
 *  Output subsets as the search progresses.
 *  (default = false).</pre>
 * 
 <!-- options-end -->
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.15 $
 */
public class ExhaustiveSearch 
  extends ASSearch 
  implements OptionHandler {

  /** for serialization */
  static final long serialVersionUID = 5741842861142379712L;
  
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
      +"attrubutes. Reports the best subset found.";
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

    newVector.addElement(new Option("\tOutput subsets as the search progresses."
				    +"\n\t(default = false)."
				    , "V", 0
				    , "-V"));
    return  newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -V
   *  Output subsets as the search progresses.
   *  (default = false).</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   *
   **/
  public void setOptions (String[] options)
    throws Exception {

    resetOptions();

    setVerbose(Utils.getFlag('V',options));
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

    text.append("no attributes\n");

    text.append("\tNumber of evaluations: "+m_evaluations+"\n");
    text.append("\tMerit of best subset found: "
		+Utils.doubleToString(Math.abs(m_bestMerit),8,3)+"\n");

    return text.toString();
  }

  /**
   * Searches the attribute subset space using an exhaustive search.
   *
   * @param ASEval the attribute evaluator to guide the search
   * @param data the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @throws Exception if the search can't be completed
   */
   public int[] search (ASEvaluation ASEval, Instances data)
     throws Exception {
     double best_merit;
     double tempMerit;
     boolean done = false;
     int sizeOfBest;
     int tempSize;
     
     BigInteger space = BigInteger.ZERO;

     m_evaluations = 0;
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

     best_merit = ASEvaluator.evaluateSubset(m_bestGroup);
     m_evaluations++;
     sizeOfBest = countFeatures(m_bestGroup);

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
     }

     int numatts = (m_hasClass) 
       ? m_numAttribs - 1
       : m_numAttribs;
     BigInteger searchSpaceEnd = 
       BigInteger.ONE.add(BigInteger.ONE).pow(numatts).subtract(BigInteger.ONE);

     while (!done) {
       // the next subset
       space = space.add(BigInteger.ONE);
       if (space.equals(searchSpaceEnd)) {
         done = true;
       }
       tempGroup.clear();
       for (int i = 0; i < numatts; i++) {
         if (space.testBit(i)) {
           if (!m_hasClass) {
             tempGroup.set(i);
           } else {
             int j = (i >= m_classIndex)
               ? i + 1
               : i;
             tempGroup.set(j);
           }
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

    System.err.println("Size: "+size);
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
    System.err.println(printSubset(temp).toString());
  }
      
  /**
   * resets to defaults
   */
  private void resetOptions() {
    m_verbose = false;
    m_evaluations = 0;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.15 $");
  }
}
