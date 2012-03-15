/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    CaRuleGeneration.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package weka.associations.classification;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.associations.RuleGeneration;
import weka.associations.RuleItem;
import weka.associations.ItemSet;

/**
 * Class implementing the rule generation procedure of the predictive apriori algorithm for class association rules.
 *
 * For association rules in gerneral the method is described in:
 * T. Scheffer (2001). <i>Finding Association Rules That Trade Support 
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
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision$ */
public class CaRuleGeneration extends RuleGeneration implements Serializable {

   /**
    * Constructor
    * @param itemSet the item set that forms the premise of the rule
    */
  public CaRuleGeneration(ItemSet itemSet){
       
       super(itemSet);  
   }
  
  
 

 
  /**
   * Generates all rules for an item set. The item set is the premise.
   * @param numRules the number of association rules the use wants to mine.
   * This number equals the size <i>n<\i> of the list of the
   * best rules.
   * @param midPoints the mid points of the intervals
   * @param priors Hashtable that contains the prior probabilities
   * @param expectation the minimum value of the expected predictive accuracy
   * that is needed to get into the list of the best rules
   * @param instances the instances for which association rules are generated
   * @param best the list of the <i>n<\i> best rules.
   * The list is implemented as a TreeSet
   * @param genTime the maximum time of generation
   * @return all the rules with minimum confidence for the given item set
   */
  public TreeSet generateRules(int numRules, double[] midPoints, Hashtable priors, double expectation, Instances instances, TreeSet best, int genTime) {
  
    boolean redundant = false;
    FastVector consequences = new FastVector();
    ItemSet premise;
    RuleItem current = null, old = null;
    
    Hashtable hashtable;
    
    m_change = false;
    m_midPoints = midPoints;
    m_priors = priors;
    m_best = best;
    m_expectation = expectation;
    m_count = genTime;
    m_instances = instances;
   
    //create rule body
    premise =null;
    premise = new ItemSet(m_totalTransactions);
    int[] premiseItems = new int[m_items.length];
    System.arraycopy(m_items, 0, premiseItems, 0, m_items.length);
    premise.setItem(premiseItems);
    premise.setCounter(m_counter);
    
    consequences = singleConsequence(instances);
               
            
    
    //create n best rules
    do{
        if(premise == null || consequences.size() == 0)
            return m_best;
        m_minRuleCount = 1;
        while(expectation((double)m_minRuleCount,premise.counter(),m_midPoints,m_priors) <= m_expectation){
            m_minRuleCount++;
            if(m_minRuleCount > premise.counter())
                return m_best;
        }
        redundant = false;
            
        //create possible heads  
        FastVector allRuleItems = new FastVector();
        int h = 0;
        while(h < consequences.size()){
            RuleItem dummie = new RuleItem();
            m_count++;
            current = dummie.generateRuleItem(premise,(ItemSet)consequences.elementAt(h),instances,m_count,m_minRuleCount,m_midPoints,m_priors);
            if(current != null)
                allRuleItems.addElement(current);
            h++;
        }
        
        //update best
        for(h =0; h< allRuleItems.size();h++){
            current = (RuleItem)allRuleItems.elementAt(h);
            if(m_best.size() < numRules){
                 m_change =true;
                 redundant = removeRedundant(current);  
            }
            else{
                m_expectation = ((RuleItem)(m_best.first())).accuracy();
                if(current.accuracy() > m_expectation){
                    boolean remove = m_best.remove(m_best.first());
                    m_change = true;
                    redundant = removeRedundant(current);
                    m_expectation = ((RuleItem)(m_best.first())).accuracy();
                    while(expectation((double)m_minRuleCount, (current.premise()).counter(),m_midPoints,m_priors) < m_expectation){
                        m_minRuleCount++;
                        if(m_minRuleCount > (current.premise()).counter())
                            break;
                    } 
                }  
            }   
        }   
    }while(redundant); 
    return m_best;
  }
  
  
  
  /**
   * Methods that decides whether or not rule a subsumes rule b.
   * The defintion of subsumption is:
   * Rule a subsumes rule b, if a subsumes b
   * AND
   * a has got least the same expected predictive accuracy as b.
   * @param a an association rule stored as a RuleItem
   * @param b an association rule stored as a RuleItem
   * @return true if rule a subsumes rule b or false otherwise.
   */  
  public static boolean aSubsumesB(RuleItem a, RuleItem b){
        
      if(!a.consequence().equals(b.consequence()))
          return false;
      if(a.accuracy() < b.accuracy())
          return false;
      for(int k = 0; k < ((a.premise()).items()).length;k++){
        if((a.premise()).itemAt(k) != (b.premise()).itemAt(k)){
            if(((a.premise()).itemAt(k) != -1 && (b.premise()).itemAt(k) != -1) || (b.premise()).itemAt(k) == -1)
                return false;
        }
        /*if(a.m_consequence.m_items[k] != b.m_consequence.m_items[k]){
            if((a.m_consequence.m_items[k] != -1 && b.m_consequence.m_items[k] != -1) || a.m_consequence.m_items[k] == -1)
                return false;
        }*/
      }
      return true;
     
  }





  /**
   * Converts the header info of the given set of instances into a set 
   * of item sets (singletons). The ordering of values in the header file 
   * determines the lexicographic order.
   *
   * @param instances the set of instances whose header info is to be used
   * @return a set of item sets, each containing a single item
   * @exception Exception if singletons can't be generated successfully
   */
 public static FastVector singletons(Instances instances) throws Exception {

    FastVector setOfItemSets = new FastVector();
    ItemSet current;

    if(instances.classIndex() == -1)
        throw new UnassignedClassException("Class index is negative (not set)!");
    Attribute att = instances.classAttribute();
    for (int i = 0; i < instances.numAttributes(); i++) {
      if (instances.attribute(i).isNumeric())
	throw new Exception("Can't handle numeric attributes!");
      if(i != instances.classIndex()){
        for (int j = 0; j < instances.attribute(i).numValues(); j++) {
            current = new ItemSet(instances.numInstances());
            int[] currentItems = new int[instances.numAttributes()];
            for (int k = 0; k < instances.numAttributes(); k++)
                currentItems[k] = -1;
            currentItems[i] = j;
            current.setItem(currentItems);
            setOfItemSets.addElement(current);
        }
      }
    }
    return setOfItemSets;
  }
  
  
   /**
   * generates a consequence of length 1 for a class association rule.
   * @param instances the instances under consideration
   * @return FastVector with consequences of length 1
   */  
  public static FastVector singleConsequence(Instances instances){
   
      ItemSet consequence;
      FastVector consequences = new FastVector();

      for (int j = 0; j < (instances.classAttribute()).numValues(); j++) {
        consequence = new ItemSet(instances.numInstances());
        int[] consequenceItems = new int[instances.numAttributes()];
        consequence.setItem(consequenceItems);
        for (int k = 0; k < instances.numAttributes(); k++) 
            consequence.setItemAt(-1,k);
        consequence.setItemAt(j,instances.classIndex());
        consequences.addElement(consequence);
      }
      return consequences;
      
  }
  
}

