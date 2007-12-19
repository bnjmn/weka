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
 *    RuleGeneration.java
 *    Copyright (C) 2004 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.associations;

import java.io.*;
import java.util.*;
import java.lang.Math;
import weka.core.*;
import weka.associations.ItemSet;

/**
 * Class implementing the rule generation procedure of the predictive apriori algorithm.
 *
 * Reference: T. Scheffer (2001). <i>Finding Association Rules That Trade Support 
 * Optimally against Confidence</i>. Proc of the 5th European Conf.
 * on Principles and Practice of Knowledge Discovery in Databases (PKDD'01),
 * pp. 424-435. Freiburg, Germany: Springer-Verlag. <p>
 *
 * The implementation follows the paper expect for adding a rule to the output of the
 * <i>n</i> best rules. A rule is added if:
 * the expected predictive accuracy of this rule is among the <i>n</i> best and it is 
 * not subsumed by a rule with at least the same expected predictive accuracy
 * (out of an unpublished manuscript from T. Scheffer). 
 *
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $ */
public class RuleGeneration implements Serializable {

  /** for serialization */
  private static final long serialVersionUID = -8927041669872491432L;

  /** The items stored as an array of of integer. */
  protected int[] m_items;

  /** Counter for how many transactions contain this item set. */
  protected int m_counter;

  /** The total number of transactions */
  protected int m_totalTransactions;
  
  /** Flag indicating whether the list fo the best rules has changed. */
  protected boolean m_change = false;
  
  /** The minimum expected predictive accuracy that is needed to be a candidate for the list of the best rules. */
  protected double m_expectation;
  
  /** Threshold. If the support of the premise is higher the binomial distrubution is approximated by a normal one. */
  protected static final int MAX_N = 300;
  
  /** The minimum support a rule needs to be a candidate for the list of the best rules. */
  protected int m_minRuleCount;
  
  /** Sorted array of the mied points of the intervals used for prior estimation. */
  protected double[] m_midPoints;
  
  /** Hashtable conatining the estimated prior probabilities. */
  protected Hashtable m_priors;
  
  /** The list of the actual <i>n</i> best rules. */
  protected TreeSet m_best;
  
  /** Integer indicating the generation time of a rule. */
  protected int m_count;
  
  /** The instances. */
  protected Instances m_instances;
   

   /**
    * Constructor
    * @param itemSet item set for that rules should be generated.
    * The item set will form the premise of the rules.
    */
  public RuleGeneration(ItemSet itemSet){
       
       m_totalTransactions = itemSet.m_totalTransactions;
       m_counter = itemSet.m_counter;
       m_items = itemSet.m_items;
   }
  
  
  /**
   * calculates the probability using a binomial distribution.
   * If the support of the premise is too large this distribution
   * is approximated by a normal distribution.
   * @param accuracy the accuracy value
   * @param ruleCount the support of the whole rule
   * @param premiseCount the support of the premise
   * @return the probability value
   */  
  public static final double binomialDistribution(double accuracy, double ruleCount, double premiseCount){
    
     double mu, sigma;
      
     if(premiseCount < MAX_N) 
        return Math.pow(2,(Utils.log2(Math.pow(accuracy,ruleCount))+Utils.log2(Math.pow((1.0-accuracy),(premiseCount-ruleCount)))+PriorEstimation.logbinomialCoefficient((int)premiseCount,(int)ruleCount)));
     else{
        mu = premiseCount * accuracy;
        sigma = Math.sqrt((premiseCount * (1.0 - accuracy))*accuracy);
        return Statistics.normalProbability(((ruleCount+0.5)-mu)/(sigma*Math.sqrt(2)));
     }
   }
  
  /**
   * calculates the expected predctive accuracy of a rule
   * @param ruleCount the support of the rule
   * @param premiseCount the premise support of the rule
   * @param midPoints array with all mid points
   * @param priors hashtable containing the prior probabilities
   * @return the expected predictive accuracy
   */  
  public static final double expectation(double ruleCount, int premiseCount,double[] midPoints, Hashtable priors){
   
      double numerator = 0, denominator = 0;
      for(int i = 0;i < midPoints.length; i++){
        Double actualPrior = (Double)priors.get(new Double(midPoints[i]));
        if(actualPrior != null){
            if(actualPrior.doubleValue() != 0){
                double addend = actualPrior.doubleValue() * binomialDistribution(midPoints[i], ruleCount, (double)premiseCount);
                denominator += addend;
                numerator += addend*midPoints[i];
            }
        }
      }
      if(denominator <= 0 || Double.isNaN(denominator))
                System.out.println("RuleItem denominator: "+denominator);
            if(numerator <= 0 || Double.isNaN(numerator))
                System.out.println("RuleItem numerator: "+numerator);
      return numerator/denominator;
  }
  
  /**
   * Generates all rules for an item set. The item set is the premise.
   * @param numRules the number of association rules the use wants to mine.
   * This number equals the size <i>n</i> of the list of the
   * best rules.
   * @param midPoints the mid points of the intervals
   * @param priors Hashtable that contains the prior probabilities
   * @param expectation the minimum value of the expected predictive accuracy
   * that is needed to get into the list of the best rules
   * @param instances the instances for which association rules are generated
   * @param best the list of the <i>n</i> best rules.
   * The list is implemented as a TreeSet
   * @param genTime the maximum time of generation
   * @return all the rules with minimum confidence for the given item set
   */
  public TreeSet generateRules(int numRules, double[] midPoints, Hashtable priors, double expectation, Instances instances,TreeSet best,int genTime) {
  
    boolean redundant = false;
    FastVector consequences = new FastVector(), consequencesMinusOne = new FastVector();
    ItemSet premise;
    int s = 0;
    RuleItem current = null, old;
    
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
    premise.m_items = new int[m_items.length];
    System.arraycopy(m_items, 0, premise.m_items, 0, m_items.length);
    premise.m_counter = m_counter;
    
    
    do{  
        m_minRuleCount = 1;
        while(expectation((double)m_minRuleCount,premise.m_counter,m_midPoints,m_priors) <= m_expectation){
            m_minRuleCount++;
            if(m_minRuleCount > premise.m_counter)
                return m_best;
        }
        redundant = false;
        for(int i = 0; i < instances.numAttributes();i++){        
            if(i == 0){    
                for(int j = 0; j < m_items.length;j++)
                    if(m_items[j] == -1)
                        consequences = singleConsequence(instances, j,consequences);           
                if(premise == null || consequences.size() == 0)
                    return m_best;
            }
            FastVector allRuleItems = new FastVector();
            int index = 0;
            do {
                int h = 0;
                while(h < consequences.size()){
                    RuleItem dummie = new RuleItem();
                    current = dummie.generateRuleItem(premise,(ItemSet)consequences.elementAt(h),instances,m_count,m_minRuleCount,m_midPoints,m_priors);
                    if(current != null){
                        allRuleItems.addElement(current);
                        h++;
                    }
                    else
                        consequences.removeElementAt(h);
                }
                if(index == i)
                    break;
                consequencesMinusOne = consequences;
                consequences = ItemSet.mergeAllItemSets(consequencesMinusOne, index, instances.numInstances());
                hashtable = ItemSet.getHashtable(consequencesMinusOne, consequencesMinusOne.size());
                consequences = ItemSet.pruneItemSets(consequences, hashtable);
                index++;
            } while (consequences.size() > 0); 
            for(int h = 0;h < allRuleItems.size();h++){
                current = (RuleItem)allRuleItems.elementAt(h);
                m_count++;
                if(m_best.size() < numRules){
                    m_change =true;
                    redundant = removeRedundant(current);
                }
                else{
                    if(current.accuracy() > m_expectation){
                        m_expectation = ((RuleItem)(m_best.first())).accuracy();
                        boolean remove = m_best.remove(m_best.first());
                        m_change = true;
                        redundant = removeRedundant(current);
                        m_expectation = ((RuleItem)(m_best.first())).accuracy();
                        while(expectation((double)m_minRuleCount, (current.premise()).m_counter,m_midPoints,m_priors) < m_expectation){
                            m_minRuleCount++;
                            if(m_minRuleCount > (current.premise()).m_counter)
                                break;
                        }
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
    
      if(a.m_accuracy < b.m_accuracy)
          return false;
      for(int k = 0; k < a.premise().m_items.length;k++){
        if(a.premise().m_items[k] != b.premise().m_items[k]){
            if((a.premise().m_items[k] != -1 && b.premise().m_items[k] != -1) || b.premise().m_items[k] == -1)
                return false;
        }
        if(a.consequence().m_items[k] != b.consequence().m_items[k]){
            if((a.consequence().m_items[k] != -1 && b.consequence().m_items[k] != -1) || a.consequence().m_items[k] == -1)
                return false;
        }
      }
      return true;
     
  }

  /**
   * generates a consequence of length 1 for an association rule.
   * @param instances the instances under consideration
   * @param attNum an item that does not occur in the premise
   * @param consequences FastVector that possibly already contains other consequences of length 1
   * @return FastVector with consequences of length 1
   */  
  public static FastVector singleConsequence(Instances instances, int attNum, FastVector consequences){
   
      ItemSet consequence;

      for (int i = 0; i < instances.numAttributes(); i++) {
          if( i == attNum){
            for (int j = 0; j < instances.attribute(i).numValues(); j++) {
                consequence = new ItemSet(instances.numInstances());
                consequence.m_items = new int[instances.numAttributes()];
                for (int k = 0; k < instances.numAttributes(); k++) 
                    consequence.m_items[k] = -1;
                consequence.m_items[i] = j;
                consequences.addElement(consequence);
            }
          }
      }
    return consequences;
      
  }

  /**
   * Method that removes redundant rules out of the list of the best rules.
   * A rule is in that list if:
   * the expected predictive accuracy of this rule is among the best and it is
   * not subsumed by a rule with at least the same expected predictive accuracy
   * @param toInsert the rule that should be inserted into the list
   * @return true if the method has changed the list, false otherwise
   */  
public boolean removeRedundant(RuleItem toInsert){
   
        boolean redundant = false, fSubsumesT = false, tSubsumesF = false;
        RuleItem first;
        int subsumes = 0;
        Object [] best = m_best.toArray();
        for(int i=0; i < best.length; i++){
            first = (RuleItem)best[i];
            fSubsumesT = aSubsumesB(first,toInsert);
            tSubsumesF = aSubsumesB(toInsert, first);
            if(fSubsumesT){
                subsumes = 1;
                break;
            }
            else{
                if(tSubsumesF){
                    boolean remove = m_best.remove(first);
                    subsumes = 2;
                    redundant =true;
                }
            }
        }
        if(subsumes == 0 || subsumes == 2)
            m_best.add(toInsert);
        return redundant;
  }

/**
 * Gets the actual maximum value of the generation time
 * @return the actual maximum value of the generation time
 */
public int count(){
    
    return m_count;
}

/**
 * Gets if the list fo the best rules has been changed
 * @return whether or not the list fo the best rules has been changed
 */
public boolean change(){
    
    return m_change;
}
}
