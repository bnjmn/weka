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
 * RuleItem.java
 * Copyright (C) 2004 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.associations;

import weka.core.Instances;
import java.io.Serializable;
import java.util.Hashtable;

/**
 * Class for storing an (class) association rule.
 * The premise and the consequence are stored each as separate item sets.
 * For every rule their expected predictive accuracy and the time of generation is stored.
 * These two measures allow to introduce a sort order for rules.
 *
 * @author Stefan Mutter
 * @version $Revision: 1.4 $
 */
public class RuleItem implements Comparable, Serializable{
      
    /** for serialization */
    private static final long serialVersionUID = -3761299128347476534L;

    /** The premise of a rule. */ 
    protected ItemSet m_premise;
    
    /** The consequence of a rule. */ 
    protected ItemSet m_consequence;
    
    /** The expected predictive accuracy of a rule. */ 
    protected double m_accuracy;
    
    /** The generation time of a rule. */ 
    protected int m_genTime;
      
    
    /**
     * Constructor for an empty RuleItem
     */    
    public RuleItem(){
          
      }
      
    /**
     * Constructor that generates a RuleItem out of a given one
     * @param toCopy RuleItem to copy
     */    
      public RuleItem(RuleItem toCopy){
      
            m_premise = toCopy.m_premise;
            m_consequence = toCopy.m_consequence;
            m_accuracy = toCopy.m_accuracy;
            m_genTime = toCopy.m_genTime;
      }
      
      /**
       * Constructor
       * @param premise the premise of the future RuleItem
       * @param consequence the consequence of the future RuleItem
       * @param genTime the time of generation of the future RuleItem
       * @param ruleSupport support of the rule
       * @param m_midPoints the mid poitns of the intervals
       * @param m_priors Hashtable containing the estimated prior probablilities
       */      
      public RuleItem(ItemSet premise, ItemSet consequence, int genTime,int ruleSupport,double [] m_midPoints, Hashtable m_priors){
      
          
            m_premise = premise;
            m_consequence = consequence;
            m_accuracy = RuleGeneration.expectation((double)ruleSupport,m_premise.m_counter,m_midPoints,m_priors);
            //overflow, underflow
            if(Double.isNaN(m_accuracy) || m_accuracy < 0){
                m_accuracy = Double.MIN_VALUE;
            }
            m_consequence.m_counter = ruleSupport;
            m_genTime = genTime;
      }
      
      /**
       * Constructs a new RuleItem if the support of the given rule is above the support threshold.
       * @param premise the premise
       * @param consequence the consequence
       * @param instances the instances
       * @param genTime the time of generation of the current premise and consequence
       * @param minRuleCount the support threshold
       * @param m_midPoints the mid points of the intervals
       * @param m_priors the estimated priori probabilities (in a hashtable)
       * @return a RuleItem if its support is above the threshold, null otherwise
       */      
      public RuleItem generateRuleItem(ItemSet premise, ItemSet consequence, Instances instances,int genTime, int minRuleCount,double[] m_midPoints, Hashtable m_priors){
          ItemSet rule = new ItemSet(instances.numInstances());
          rule.m_items = new int[(consequence.m_items).length];
          System.arraycopy(premise.m_items, 0, rule.m_items, 0, (premise.m_items).length);
          for(int k = 0;k < consequence.m_items.length; k++){
            if(consequence.m_items[k] != -1)
                rule.m_items[k] = consequence.m_items[k];
          }
          for (int i = 0; i < instances.numInstances(); i++) 
            rule.upDateCounter(instances.instance(i));
          int ruleSupport = rule.support();
          if(ruleSupport > minRuleCount){
              RuleItem newRule = new RuleItem(premise,consequence,genTime,ruleSupport,m_midPoints,m_priors);
              return newRule;
          }
          return null;
      }
      
      //Note: this class has a natural ordering that is inconsistent with equals
      /**
       * compares two RuleItems and allows an ordering concerning
       * expected predictive accuracy and time of generation
       * Note: this class has a natural ordering that is inconsistent with equals
       * @param o RuleItem to compare
       * @return integer indicating the sort oder of the two RuleItems
       */      
      public int compareTo(Object o) {
          
          if(this.m_accuracy == ((RuleItem)o).m_accuracy){ 
            if((this.m_genTime == ((RuleItem)o).m_genTime))
		return 0;
            if(this.m_genTime > ((RuleItem)o).m_genTime)
		return -1;
            if(this.m_genTime < ((RuleItem)o).m_genTime)
		return 1;
          }
	  if(this.m_accuracy < ((RuleItem)o).m_accuracy)
		return -1;
	    return 1;
      }
      
      /**
       * returns whether two RuleItems are equal
       * @param o RuleItem to compare
       * @return true if the rules are equal, false otherwise
       */      
      public  boolean equals(Object o){
       
          if(o == null)
              return false;
          if(m_premise.equals(((RuleItem)o).m_premise) && m_consequence.equals(((RuleItem)o).m_consequence))
              return true;
          return false;
      }
      
      /**
       * Gets the expected predictive accuracy of a rule
       * @return the expected predictive accuracy of a rule stored as a RuleItem
       */      
      public double accuracy(){
          
          return m_accuracy;
      }
      
      /**
       * Gets the premise of a rule
       * @return the premise of a rule stored as a RuleItem
       */      
      public ItemSet premise(){
          
          return m_premise;
      }
      
      /**
       * Gets the consequence of a rule
       * @return the consequence of a rule stored as a RuleItem
       */      
      public ItemSet consequence(){
          
          return m_consequence;
      }
      
      
  }
  
