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

import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TreeSet;

/**
 <!-- globalinfo-start -->
 * Class implementing the predictive apriori algorithm to mine association rules.<br/>
 * It searches with an increasing support threshold for the best 'n' rules concerning a support-based corrected confidence value.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Tobias Scheffer: Finding Association Rules That Trade Support Optimally against Confidence. In: 5th European Conference on Principles of Data Mining and Knowledge Discovery, 424-435, 2001.<br/>
 * <br/>
 * The implementation follows the paper expect for adding a rule to the output of the 'n' best rules. A rule is added if:<br/>
 * the expected predictive accuracy of this rule is among the 'n' best and it is not subsumed by a rule with at least the same expected predictive accuracy (out of an unpublished manuscript from T. Scheffer).
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Scheffer2001,
 *    author = {Tobias Scheffer},
 *    booktitle = {5th European Conference on Principles of Data Mining and Knowledge Discovery},
 *    pages = {424-435},
 *    publisher = {Springer},
 *    title = {Finding Association Rules That Trade Support Optimally against Confidence},
 *    year = {2001}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N &lt;required number of rules output&gt;
 *  The required number of rules. (default = 100)</pre>
 * 
 * <pre> -A
 *  If set class association rules are mined. (default = no)</pre>
 * 
 * <pre> -c &lt;the class index&gt;
 *  The class index. (default = last)</pre>
 * 
 <!-- options-end -->
 *
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 1.7 $ */

public class PredictiveApriori 
  extends Associator 
  implements OptionHandler, CARuleMiner, TechnicalInformationHandler {
  
  /** for serialization */
  static final long serialVersionUID = 8109088846865075341L;
  
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
  
   /** The class index. */  
  protected int m_classIndex;
  
  /** Flag indicating whether class association rules are mined. */
  protected boolean m_car;

  /**
   * Returns a string describing this associator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Class implementing the predictive apriori algorithm to mine "
      + "association rules.\n"
      + "It searches with an increasing support threshold for the best 'n' "
      + "rules concerning a support-based corrected confidence value.\n\n"
      + "For more information see:\n\n"
      + getTechnicalInformation().toString() + "\n\n"
      + "The implementation follows the paper expect for adding a rule to the "
      + "output of the 'n' best rules. A rule is added if:\n"
      + "the expected predictive accuracy of this rule is among the 'n' best "
      + "and it is not subsumed by a rule with at least the same expected "
      + "predictive accuracy (out of an unpublished manuscript from T. "
      + "Scheffer).";
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Tobias Scheffer");
    result.setValue(Field.TITLE, "Finding Association Rules That Trade Support Optimally against Confidence");
    result.setValue(Field.BOOKTITLE, "5th European Conference on Principles of Data Mining and Knowledge Discovery");
    result.setValue(Field.YEAR, "2001");
    result.setValue(Field.PAGES, "424-435");
    result.setValue(Field.PUBLISHER, "Springer");
    
    return result;
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
    m_car = false;
    m_classIndex = -1;
    m_priors = new Hashtable();
    
    
   
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }
  
  /**
   * Method that generates all large itemsets with a minimum support, and from
   * these all association rules.
   *
   * @param instances the instances to be used for generating the associations
   * @throws Exception if rules can't be built successfully
   */
  public void buildAssociations(Instances instances) throws Exception {
      
    int temp = m_premiseCount, exactNumber = m_numRules-5; 

    m_premiseCount = 1;
    m_best = new TreeSet();
    m_bestChanged = false;
    m_expectation = 0;
    m_count = 1;
    m_instances = instances;

    if (m_classIndex == -1)
      m_instances.setClassIndex(m_instances.numAttributes()-1);     
    else if (m_classIndex < m_instances.numAttributes() && m_classIndex >= 0)
      m_instances.setClassIndex(m_classIndex);
    else
      throw new Exception("Invalid class index.");
    
    // can associator handle the data?
    getCapabilities().testWithFail(m_instances);
    
    //prior estimation
    m_priorEstimator = new PriorEstimation(m_instances,m_numRandRules,m_numIntervals,m_car);
    m_priors = m_priorEstimator.estimatePrior();
    m_midPoints = m_priorEstimator.getMidPoints();
    
    m_Ls = new FastVector();
    m_hashtables = new FastVector();
    
    for(int i =1; i < m_instances.numAttributes();i++){
      m_bestChanged = false;
      if(!m_car){
        // find large item sets
        findLargeItemSets(i);
      
        //find association rules (rule generation procedure)
        findRulesQuickly();
      }
      else{
        findLargeCarItemSets(i);
        findCaRulesQuickly();
      }
      
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
        k++;
        exactNumber--;
    }
  }
  
  /**
     * Method that mines the n best class association rules.
     * @return an sorted array of FastVector (depending on the expected predictive accuracy) containing the rules and metric information
     * @param data the instances for which class association rules should be mined
     * @throws Exception if rules can't be built successfully
     */
    public FastVector[] mineCARs(Instances data) throws Exception{
	 
        m_car = true;
        m_best = new TreeSet();
        m_premiseCount = 1;
        m_bestChanged = false;
        m_expectation = 0;
        m_count = 1;
	buildAssociations(data);
        FastVector[] allCARRules = new FastVector[3];
        allCARRules[0] = new FastVector();
        allCARRules[1] = new FastVector();
        allCARRules[2] = new FastVector();
        for(int k =0; k < m_allTheRules[0].size();k++){
            int[] newPremiseArray = new int[m_instances.numAttributes()-1];
            int help = 0;
            for(int j = 0;j < m_instances.numAttributes();j++){
                if(j != m_instances.classIndex()){
                    newPremiseArray[help] = ((ItemSet)m_allTheRules[0].elementAt(k)).itemAt(j);
                    help++;
                }
            }
            ItemSet newPremise = new ItemSet(m_instances.numInstances(), newPremiseArray);
            newPremise.setCounter (((ItemSet)m_allTheRules[0].elementAt(k)).counter());
            allCARRules[0].addElement(newPremise);
            int[] newConsArray = new int[1];
            newConsArray[0] =((ItemSet)m_allTheRules[1].elementAt(k)).itemAt(m_instances.classIndex());
            ItemSet newCons = new ItemSet(m_instances.numInstances(), newConsArray);
            newCons.setCounter(((ItemSet)m_allTheRules[1].elementAt(k)).counter());
            allCARRules[1].addElement(newCons);
            allCARRules[2].addElement(m_allTheRules[2].elementAt(k));
        }
        
	return allCARRules;
    }
    
    /**
     * Gets the instances without the class attribute
     * @return instances without class attribute
     */    
    public Instances getInstancesNoClass() {
      
      Instances noClass = null;
      try{
        noClass = LabeledItemSet.divide(m_instances,false);
      } 
      catch(Exception e){
        e.printStackTrace();
        System.out.println("\n"+e.getMessage());
      }
      //System.out.println(noClass);
      return noClass;
  }  
  
    /**
     * Gets the class attribute of all instances
     * @return Instances containing only the class attribute
     */    
  public Instances getInstancesOnlyClass() {
      
      Instances onlyClass = null;
      try{
        onlyClass = LabeledItemSet.divide(m_instances,true);
      } 
      catch(Exception e){
        e.printStackTrace();
        System.out.println("\n"+e.getMessage());
      }
      return onlyClass;
      
  }  

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    String string1 = "\tThe required number of rules. (default = " + (m_numRules-5) + ")",
      string2 = "\tIf set class association rules are mined. (default = no)",
      string3 = "\tThe class index. (default = last)";
    FastVector newVector = new FastVector(3);

    newVector.addElement(new Option(string1, "N", 1, 
				    "-N <required number of rules output>"));
    newVector.addElement(new Option(string2, "A", 0,
				    "-A"));
    newVector.addElement(new Option(string3, "c", 1,
				    "-c <the class index>"));
    return newVector.elements();
  }

 
/**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -N &lt;required number of rules output&gt;
   *  The required number of rules. (default = 100)</pre>
   * 
   * <pre> -A
   *  If set class association rules are mined. (default = no)</pre>
   * 
   * <pre> -c &lt;the class index&gt;
   *  The class index. (default = last)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported 
   */
  public void setOptions(String[] options) throws Exception {
    
    resetOptions();
    String numRulesString = Utils.getOption('N', options),
           classIndexString = Utils.getOption('c',options);
    
    if (numRulesString.length() != 0) 
	m_numRules = Integer.parseInt(numRulesString)+5;
    else
        m_numRules = Integer.MAX_VALUE;
    if (classIndexString.length() != 0) 
	m_classIndex = Integer.parseInt(numRulesString);
    m_car = Utils.getFlag('A', options);
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
    options[current++] = "-A"; options[current++] = "" + m_car;
    options[current++] = "-c"; options[current++] = "" + m_classIndex;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }


  /**
   * Outputs the association rules.
   * 
   * @return a string representation of the model
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
   * Sets the class index
   * @param index the index of the class attribute
   */  
  public void setClassIndex(int index){
      
      m_classIndex = index;
  }
  
  /**
   * Gets the index of the class attribute
   * @return the index of the class attribute
   */  
  public int getClassIndex(){
      
      return m_classIndex;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classIndexTipText() {
    return "Index of the class attribute.\n If set to -1, the last attribute will be taken as the class attribute.";
  }

    /**
   * Sets class association rule mining
   * @param flag if class association rules are mined, false otherwise
   */  
  public void setCar(boolean flag){
      
      m_car = flag;
  }
  
  /**
   * Gets whether class association ruels are mined
   * @return true if class association rules are mined, false otherwise
   */  
  public boolean getCar(){
      
      return m_car;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String carTipText() {
    return "If enabled class association rules are mined instead of (general) association rules.";
  }
  
    /**
   * Returns the metric string for the chosen metric type.
   * Predictive apriori uses the estimated predictive accuracy.
   * Therefore the metric string is "acc".
   * @return string "acc"
   */
  public String metricString() {
      
      return "acc";
  }

 
  /** 
   * Method that finds all large itemsets for the given set of instances.
   *
   * @param index the instances to be used
   * @throws Exception if an attribute is numeric
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
   * @throws Exception if an attribute is numeric
   */
  private void findRulesQuickly() throws Exception {

    RuleGeneration currentItemSet;
    
    // Build rules
    for (int j = 0; j < m_Ls.size(); j++) {
      FastVector currentItemSets = (FastVector)m_Ls.elementAt(j);
      Enumeration enumItemSets = currentItemSets.elements();
      while (enumItemSets.hasMoreElements()) { 
        currentItemSet = new RuleGeneration((ItemSet)enumItemSets.nextElement());
        m_best = currentItemSet.generateRules(m_numRules, m_midPoints,m_priors,m_expectation,
                                        m_instances,m_best,m_count);
          
        m_count = currentItemSet.m_count;
        if(!m_bestChanged && currentItemSet.m_change)
           m_bestChanged = true;
        //update minimum expected predictive accuracy to get into the n best
        if(m_best.size() >0)
            m_expectation = ((RuleItem)m_best.first()).accuracy();
        else m_expectation =0;
      }
    }
  }
  
  
  /**
   * Method that finds all large itemsets for class association rule mining for the given set of instances.
   * @param index the size of the large item sets
   * @throws Exception if an attribute is numeric
   */
  private void findLargeCarItemSets(int index) throws Exception {
    
    FastVector kMinusOneSets, kSets = new FastVector();
    Hashtable hashtable;
    int i = 0;
    // Find large itemsets
    if(index == 1){
        kSets = CaRuleGeneration.singletons(m_instances);
        ItemSet.upDateCounters(kSets, m_instances);
        kSets = ItemSet.deleteItemSets(kSets, m_premiseCount,Integer.MAX_VALUE);
        if (kSets.size() == 0)
            return;
        m_Ls.addElement(kSets);
    }
    
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
   * Method that finds all class association rules.
   *
   * @throws Exception if an attribute is numeric
   */
  private void findCaRulesQuickly() throws Exception {
    
    CaRuleGeneration currentLItemSet;
    // Build rules
    for (int j = 0; j < m_Ls.size(); j++) {
      FastVector currentItemSets = (FastVector)m_Ls.elementAt(j);
      Enumeration enumItemSets = currentItemSets.elements();
      while (enumItemSets.hasMoreElements()) {
        currentLItemSet = new CaRuleGeneration((ItemSet)enumItemSets.nextElement());
        m_best = currentLItemSet.generateRules(m_numRules, m_midPoints,m_priors,m_expectation,
                                        m_instances,m_best,m_count);
        m_count = currentLItemSet.count();
        if(!m_bestChanged && currentLItemSet.change())
                m_bestChanged = true;
        if(m_best.size() >0)
            m_expectation = ((RuleItem)m_best.first()).accuracy();
        else 
            m_expectation =0;
      }
    }
  }


  /**
   * Main method.
   * 
   * @param args the commandline parameters
   */
  public static void main(String[] args) {

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
      trainFileString = Utils.getOption('t', args);
      if (trainFileString.length() == 0) 
	throw new Exception("No training file given!");
      apriori.setOptions(args);
      reader = new BufferedReader(new FileReader(trainFileString));
      if(!apriori.getCar())
        apriori.buildAssociations(new Instances(reader));
      else
        apriori.mineCARs(new Instances(reader));
      System.out.println(apriori);
    } catch(Exception e) {
      e.printStackTrace();
      System.out.println("\n"+e.getMessage()+text);
    }
  }
  
}

