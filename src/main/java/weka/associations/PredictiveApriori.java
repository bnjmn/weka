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
 *    PredictiveApriori.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package weka.associations;

import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TreeSet;
import java.lang.Math;
import weka.core.Instances;
import weka.core.FastVector;
import weka.core.OptionHandler;
import weka.core.Option;
import weka.core.Utils;
import weka.core.UnassignedClassException;
import weka.associations.Associator;
import weka.associations.PriorEstimation;
import weka.associations.RuleGeneration;

/**
 * Class implementing the predictive apriori algorithm to mine association rules. 
 * It searches with an increasing support threshold for the best <i>n<\i> rules 
 * concerning a support-based corrected confidence value. 
 *
 * Reference: T. Scheffer (2001). <i>Finding Association Rules That Trade Support 
 * Optimally against Confidence</i>. Proc of the 5th European Conf.
 * on Principles and Practice of Knowledge Discovery in Databases (PKDD'01),
 * pp. 424-435. Freiburg, Germany: Springer-Verlag. <p>
 *
 * The implementation follows the paper expect for adding a rule to the output of the
 * <i>n<\i> best rules. A rule is added if:
 * the expected predictive accuracy of this rule is among the <i>n<\i> best and it is 
 * not subsumed by a rule with at least the same expected predictive accuracy
 * (out of an unpublished manuscript from T. Scheffer). 
 *
 * Valid option is:<p>
 *   
 * -N required number of rules <br>
 * The required number of rules (default: 100). <p>
 *
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 1.3.2.3 $ */

public class PredictiveApriori extends Associator implements OptionHandler {
  
  /** The minimum support. */
  protected int m_premiseCount;
 
  /** The maximum number of rules that are output. */
  protected int m_numRules;

  /** The number of rules created for the prior estimation. */
  protected static final int m_numRandRules = 1000;
 
  /** The number of intervals used for the prior estimation. */
  protected static final int m_numIntervals = 100;
  
  /** The set of all sets of itemsets. */
  protected FastVector m_Ls;

  /** The same information stored in hash tables. */
  protected FastVector m_hashtables;

  /** The list of all generated rules. */
  protected FastVector[] m_allTheRules;

  /** The instances (transactions) to be used for generating 
      the association rules. */
  protected Instances m_instances;

  /** The hashtable containing the prior probabilities. */
  protected Hashtable m_priors;
  
  /** The mid points of the intervals used for the prior estimation. */
  protected double[] m_midPoints;

  /** The expected predictive accuracy a rule needs to be a candidate for the output. */
  protected double m_expectation;
  
  /** The n best rules. */
  protected TreeSet m_best;
  
  /** Flag keeping track if the list of the n best rules has changed. */
  protected boolean m_bestChanged;
  
  /** Counter for the time of generation for an association rule. */
  protected int m_count;
  
  /** The prior estimator. */
  protected PriorEstimation m_priorEstimator;

  /**
   * Returns a string describing this associator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Finds association rules sorted by predictive accuracy.";
  }

  /**
   * Constructor that allows to sets default values for the 
   * minimum confidence and the maximum number of rules
   * the minimum confidence.
   */
  public PredictiveApriori() {

    resetOptions();
  }

  /**
   * Resets the options to the default values.
   */
  public void resetOptions() {
    
    m_numRules = 105;
    m_premiseCount = 1;
    m_best = new TreeSet();
    m_bestChanged = false;
    m_expectation = 0;
    m_count = 1;
    
   
  }
  
  /**
   * Method that generates all large itemsets with a minimum support, and from
   * these all association rules.
   *
   * @param instances the instances to be used for generating the associations
   * @exception Exception if rules can't be built successfully
   */
  public void buildAssociations(Instances instances) throws Exception {
      
    int temp = m_premiseCount, exactNumber = m_numRules-5; 

    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    m_instances = new Instances(instances);
    m_instances.setClassIndex(m_instances.numAttributes()-1);
    
    //prior estimation
    m_priorEstimator = new PriorEstimation(m_instances,m_numRandRules,m_numIntervals,false);
    m_priors = m_priorEstimator.estimatePrior();
    m_midPoints = m_priorEstimator.getMidPoints();
    
    m_Ls = new FastVector();
    m_hashtables = new FastVector();
    
    for(int i =1; i < m_instances.numAttributes();i++){
      m_bestChanged = false;
      
      // find large item sets
      findLargeItemSets(i);
      
      //find association rules (rule generation procedure)
      findRulesQuickly();
      
      if(m_bestChanged){
        temp =m_premiseCount;
        while(RuleGeneration.expectation(m_premiseCount, m_premiseCount,m_midPoints,m_priors) <= m_expectation){
            m_premiseCount++; 
            if(m_premiseCount > m_instances.numInstances())
                break;
        }
      }
      if(m_premiseCount > m_instances.numInstances()){
          
         // Reserve space for variables
        m_allTheRules = new FastVector[3];
        m_allTheRules[0] = new FastVector();
        m_allTheRules[1] = new FastVector();
        m_allTheRules[2] = new FastVector();
      
        int k = 0;
        while(m_best.size()>0 && exactNumber > 0){
            m_allTheRules[0].insertElementAt((ItemSet)((RuleItem)m_best.last()).premise(),k);
            m_allTheRules[1].insertElementAt((ItemSet)((RuleItem)m_best.last()).consequence(),k);
            m_allTheRules[2].insertElementAt(new Double(((RuleItem)m_best.last()).accuracy()),k);
            boolean remove = m_best.remove(m_best.last());
            k++;
            exactNumber--;
        }
        return;    
      }
      
      if(temp != m_premiseCount && m_Ls.size() > 0){
        FastVector kSets = (FastVector)m_Ls.lastElement();
        m_Ls.removeElementAt(m_Ls.size()-1);
        kSets = ItemSet.deleteItemSets(kSets, m_premiseCount,Integer.MAX_VALUE);
        m_Ls.addElement(kSets);
      }
    }
    
    // Reserve space for variables
    m_allTheRules = new FastVector[3];
    m_allTheRules[0] = new FastVector();
    m_allTheRules[1] = new FastVector();
    m_allTheRules[2] = new FastVector();
      
    int k = 0;
    while(m_best.size()>0 && exactNumber > 0){
        m_allTheRules[0].insertElementAt((ItemSet)((RuleItem)m_best.last()).premise(),k);
        m_allTheRules[1].insertElementAt((ItemSet)((RuleItem)m_best.last()).consequence(),k);
        m_allTheRules[2].insertElementAt(new Double(((RuleItem)m_best.last()).accuracy()),k);
        boolean remove = m_best.remove(m_best.last());
        k++;
        exactNumber--;
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    String string1 = "\tThe required number of rules. (default = " + (m_numRules-5) + ")";
    FastVector newVector = new FastVector(1);

    newVector.addElement(new Option(string1, "N", 1, 
				    "-N <required number of rules output>"));
    return newVector.elements();
  }

 
/**
   * Parses a given list of options. Valid option is:<p>
   *   
   * -N required number of rules <br>
   * The required number of rules (default: 10). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported 
   */
  public void setOptions(String[] options) throws Exception {
    
    resetOptions();
    String numRulesString = Utils.getOption('N', options);
    
    if (numRulesString.length() != 0) 
	m_numRules = Integer.parseInt(numRulesString)+5;
    else
        m_numRules = Integer.MAX_VALUE;
  }

  /**
   * Gets the current settings of the PredictiveApriori object.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [10];
    int current = 0;
    options[current++] = "-N"; options[current++] = "" + (m_numRules-5);
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }


  /**
   * Outputs the association rules.
   */
  public String toString() {

    StringBuffer text = new StringBuffer();

    if (m_allTheRules[0].size() == 0)
      return "\nNo large itemsets and rules found!\n";
    text.append("\nPredictiveApriori\n===================\n\n");
    text.append("\nBest rules found:\n\n");
    
    for (int i = 0; i < m_allTheRules[0].size(); i++) {
	    text.append(Utils.doubleToString((double)i+1, 
					     (int)(Math.log(m_numRules)/Math.log(10)+1),0)+
			". " + ((ItemSet)m_allTheRules[0].elementAt(i)).
			toString(m_instances) 
			+ " ==> " + ((ItemSet)m_allTheRules[1].elementAt(i)).
			toString(m_instances) +"    acc:("+  
			Utils.doubleToString(((Double)m_allTheRules[2].
					      elementAt(i)).doubleValue(),5)+")");
      
      text.append('\n');
    }
    
    
    return text.toString();
  }

 
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numRulesTipText() {
    return "Number of rules to find.";
  }

  /**
   * Get the value of the number of required rules.
   *
   * @return Value of the number of required rules.
   */
  public int getNumRules() {
      
    return m_numRules-5;
  }
  
  /**
   * Set the value of required rules.
   *
   * @param v  Value to assign to number of required rules.
   */
  public void setNumRules(int v) {
	 
      m_numRules = v+5;
  }

 
  /** 
   * Method that finds all large itemsets for the given set of instances.
   *
   * @param the instances to be used
   * @exception Exception if an attribute is numeric
   */
  private void findLargeItemSets(int index) throws Exception {
    
    FastVector kMinusOneSets, kSets = new FastVector();
    Hashtable hashtable;
    int i = 0;
    // Find large itemsets
    //of length 1
    if(index == 1){
        kSets = ItemSet.singletons(m_instances);
        ItemSet.upDateCounters(kSets, m_instances);
        kSets = ItemSet.deleteItemSets(kSets, m_premiseCount,Integer.MAX_VALUE);
        if (kSets.size() == 0)
            return;
        m_Ls.addElement(kSets);
    }
    //of length > 1
    if(index >1){
        if(m_Ls.size() > 0)
            kSets = (FastVector)m_Ls.lastElement();
        m_Ls.removeAllElements();
        i = index-2;
        kMinusOneSets = kSets;
        kSets = ItemSet.mergeAllItemSets(kMinusOneSets, i, m_instances.numInstances());
        hashtable = ItemSet.getHashtable(kMinusOneSets, kMinusOneSets.size());
        m_hashtables.addElement(hashtable);
        kSets = ItemSet.pruneItemSets(kSets, hashtable);
        ItemSet.upDateCounters(kSets, m_instances);
        kSets = ItemSet.deleteItemSets(kSets, m_premiseCount,Integer.MAX_VALUE);
        if(kSets.size() == 0)
            return;
        m_Ls.addElement(kSets);
    }
  } 


  

  /** 
   * Method that finds all association rules.
   *
   * @exception Exception if an attribute is numeric
   */
  private void findRulesQuickly() throws Exception {

    FastVector[] rules;
    RuleGeneration currentItemSet;
    
    // Build rules
    for (int j = 0; j < m_Ls.size(); j++) {
      FastVector currentItemSets = (FastVector)m_Ls.elementAt(j);
      Enumeration enumItemSets = currentItemSets.elements();
      while (enumItemSets.hasMoreElements()) { 
        currentItemSet = new RuleGeneration((ItemSet)enumItemSets.nextElement());
        m_best = currentItemSet.generateRules(m_numRules-5, m_midPoints,m_priors,m_expectation,
                                        m_instances,m_best,m_count);
          
        m_count = currentItemSet.m_count;
        if(!m_bestChanged && currentItemSet.m_change)
           m_bestChanged = true;
        //update minimum expected predictive accuracy to get into the n best
        if(m_best.size() >= m_numRules-5)
            m_expectation = ((RuleItem)m_best.first()).accuracy();
        else m_expectation =0;
      }
    }
  }


  /**
   * Main method for testing this class.
   */
  public static void main(String[] options) {

    String trainFileString;
    StringBuffer text = new StringBuffer();
    PredictiveApriori apriori = new PredictiveApriori();
    Reader reader;

    try {
      text.append("\n\nPredictiveApriori options:\n\n");
      text.append("-t <training file>\n");
      text.append("\tThe name of the training file.\n");
      Enumeration enu = apriori.listOptions();
      while (enu.hasMoreElements()) {
	Option option = (Option)enu.nextElement();
	text.append(option.synopsis()+'\n');
	text.append(option.description()+'\n');
      }
      trainFileString = Utils.getOption('t', options);
      if (trainFileString.length() == 0) 
	throw new Exception("No training file given!");
      apriori.setOptions(options);
      reader = new BufferedReader(new FileReader(trainFileString));
      apriori.buildAssociations(new Instances(reader));
      System.out.println(apriori);
    } catch(Exception e) {
      e.printStackTrace();
      System.out.println("\n"+e.getMessage()+text);
    }
  }
  
}



