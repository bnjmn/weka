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
 *    ItemSet.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.associations;

import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for storing a set of items. Item sets are stored in a lexicographic
 * order, which is determined by the header information of the set of instances
 * used for generating the set of items. All methods in this class assume that
 * item sets are stored in lexicographic order.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.8 $
 */
public class ItemSet implements Serializable {

  /** The items stored as an array of of ints. */
  protected int[] m_items;

  /** Counter for how many transactions contain this item set. */
  protected int m_counter;

  /** The total number of transactions */
  protected int m_totalTransactions;

  /**
   * Constructor
   * @param totalTrans the total number of transactions in the data
   */
  public ItemSet(int totalTrans) {
    m_totalTransactions = totalTrans;
  }

  /**
   * Outputs the confidence for a rule.
   *
   * @param premise the premise of the rule
   * @param consequence the consequence of the rule
   * @return the confidence on the training data
   */
  public static double confidenceForRule(ItemSet premise, 
					 ItemSet consequence) {

    return (double)consequence.m_counter/(double)premise.m_counter;
  }

  /**
   * Outputs the lift for a rule. Lift is defined as:<br>
   * confidence / prob(consequence)
   *
   * @param premise the premise of the rule
   * @param consequence the consequence of the rule
   * @param consequenceCount how many times the consequence occurs independent
   * of the premise
   * @return the lift on the training data
   */
  public double liftForRule(ItemSet premise, 
			    ItemSet consequence,
			    int consequenceCount) {
    double confidence = confidenceForRule(premise, consequence);

   return confidence / ((double)consequenceCount / 
	  (double)m_totalTransactions);
  }

  /**
   * Outputs the leverage for a rule. Leverage is defined as: <br>
   * prob(premise & consequence) - (prob(premise) * prob(consequence))
   *
   * @param premise the premise of the rule
   * @param consequence the consequence of the rule
   * @param premiseCount how many times the premise occurs independent
   * of the consequent
   * @param consequenceCount how many times the consequence occurs independent
   * of the premise
   * @return the leverage on the training data
   */
  public double leverageForRule(ItemSet premise,
				ItemSet consequence,
				int premiseCount,
				int consequenceCount) {
    double coverageForItemSet = (double)consequence.m_counter / 
      (double)m_totalTransactions;
    double expectedCoverageIfIndependent = 
      ((double)premiseCount / (double)m_totalTransactions) * 
      ((double)consequenceCount / (double)m_totalTransactions);
    double lev = coverageForItemSet - expectedCoverageIfIndependent;
    return lev;
  }

  /**
   * Outputs the conviction for a rule. Conviction is defined as: <br>
   * prob(premise) * prob(!consequence) / prob(premise & !consequence)
   *
   * @param premise the premise of the rule
   * @param consequence the consequence of the rule
   * @param premiseCount how many times the premise occurs independent
   * of the consequent
   * @param consequenceCount how many times the consequence occurs independent
   * of the premise
   * @return the conviction on the training data
   */
  public double convictionForRule(ItemSet premise,
				   ItemSet consequence,
				   int premiseCount,
				   int consequenceCount) {
    double num = 
      (double)premiseCount * (double)(m_totalTransactions - consequenceCount) *
       (double)m_totalTransactions;
    double denom = 
      ((premiseCount - consequence.m_counter)+1);
    
    if (num < 0 || denom < 0) {
      System.err.println("*** "+num+" "+denom);
      System.err.println("premis count: "+premiseCount+" consequence count "+consequenceCount+" total trans "+m_totalTransactions);
    }
    return num / denom;
  }

  /**
   * Checks if an instance contains an item set.
   *
   * @param instance the instance to be tested
   * @return true if the given instance contains this item set
   */
  public final boolean containedBy(Instance instance) {
    
    for (int i = 0; i < instance.numAttributes(); i++) 
      if (m_items[i] > -1) {
	if (instance.isMissing(i))
	  return false;
	if (m_items[i] != (int)instance.value(i))
	  return false;
      }
    return true;
  }

  /**
   * Deletes all item sets that don't have minimum support.
   *
   * @param itemSets the set of item sets to be pruned
   * @param minSupport the minimum number of transactions to be covered
   * @return the reduced set of item sets
   */
  public static FastVector deleteItemSets(FastVector itemSets, 
					  int minSupport,
					  int maxSupport) {

    FastVector newVector = new FastVector(itemSets.size());

    for (int i = 0; i < itemSets.size(); i++) {
      ItemSet current = (ItemSet)itemSets.elementAt(i);
      if ((current.m_counter >= minSupport) 
	  && (current.m_counter <= maxSupport))
	newVector.addElement(current);
    }
    return newVector;
  }

  /**
   * Tests if two item sets are equal.
   *
   * @param itemSet another item set
   * @return true if this item set contains the same items as the given one
   */
  public final boolean equals(Object itemSet) {

    if ((itemSet == null) || !(itemSet.getClass().equals(this.getClass()))) {
      return false;
    }
    if (m_items.length != ((ItemSet)itemSet).m_items.length)
      return false;
    for (int i = 0; i < m_items.length; i++)
      if (m_items[i] != ((ItemSet)itemSet).m_items[i])
	return false;
    return true;
  }

  /**
   * Generates all rules for an item set.
   *
   * @param minConfidence the minimum confidence the rules have to have
   * @param hashtables containing all(!) previously generated
   * item sets
   * @param numItemsInSet the size of the item set for which the rules
   * are to be generated
   * @return all the rules with minimum confidence for the given item set
   */
  public final FastVector[] generateRules(double minConfidence, 
					  FastVector hashtables,
					  int numItemsInSet) {

    FastVector premises = new FastVector(),consequences = new FastVector(),
      conf = new FastVector();
    FastVector[] rules = new FastVector[3], moreResults;
    ItemSet premise, consequence;
    Hashtable hashtable = (Hashtable)hashtables.elementAt(numItemsInSet - 2);

    // Generate all rules with one item in the consequence.
    for (int i = 0; i < m_items.length; i++) 
      if (m_items[i] != -1) {
	premise = new ItemSet(m_totalTransactions);
	consequence = new ItemSet(m_totalTransactions);
	premise.m_items = new int[m_items.length];
	consequence.m_items = new int[m_items.length];
	consequence.m_counter = m_counter;
	for (int j = 0; j < m_items.length; j++) 
	  consequence.m_items[j] = -1;
	System.arraycopy(m_items, 0, premise.m_items, 0, m_items.length);
	premise.m_items[i] = -1;
	consequence.m_items[i] = m_items[i];
	premise.m_counter = ((Integer)hashtable.get(premise)).intValue();
	premises.addElement(premise);
	consequences.addElement(consequence);
	conf.addElement(new Double(confidenceForRule(premise, consequence)));
      }
    rules[0] = premises;
    rules[1] = consequences;
    rules[2] = conf;
    pruneRules(rules, minConfidence);

    // Generate all the other rules
    moreResults = moreComplexRules(rules, numItemsInSet, 1, minConfidence,
				   hashtables);
    if (moreResults != null) 
      for (int i = 0; i < moreResults[0].size(); i++) {
	rules[0].addElement(moreResults[0].elementAt(i));
	rules[1].addElement(moreResults[1].elementAt(i));
	rules[2].addElement(moreResults[2].elementAt(i));
      }
    return rules;
  }

  /**
   * Generates all significant rules for an item set.
   *
   * @param minMetric the minimum metric (confidence, lift, leverage, 
   * improvement) the rules have to have
   * @param metricType (confidence=0, lift, leverage, improvement)
   * @param hashtables containing all(!) previously generated
   * item sets
   * @param numItemsInSet the size of the item set for which the rules
   * are to be generated
   * @param the significance level for testing the rules
   * @return all the rules with minimum metric for the given item set
   * @exception Exception if something goes wrong
   */
  public final FastVector[] generateRulesBruteForce(double minMetric,
						    int metricType,
						FastVector hashtables,
						int numItemsInSet,
						int numTransactions,
						double significanceLevel) 
  throws Exception {

    FastVector premises = new FastVector(),consequences = new FastVector(),
      conf = new FastVector(), lift = new FastVector(), lev = new FastVector(),
      conv = new FastVector(); 
    FastVector[] rules = new FastVector[6];
    ItemSet premise, consequence;
    Hashtable hashtableForPremise, hashtableForConsequence;
    int numItemsInPremise, help, max, consequenceUnconditionedCounter;
    double[][] contingencyTable = new double[2][2];
    double metric, chiSquared;

    // Generate all possible rules for this item set and test their
    // significance.
    max = (int)Math.pow(2, numItemsInSet);
    for (int j = 1; j < max; j++) {
      numItemsInPremise = 0;
      help = j;
      while (help > 0) {
	if (help % 2 == 1)
	  numItemsInPremise++;
	help /= 2;
      }
      if (numItemsInPremise < numItemsInSet) {
	hashtableForPremise = 
	  (Hashtable)hashtables.elementAt(numItemsInPremise-1);
	hashtableForConsequence = 
	  (Hashtable)hashtables.elementAt(numItemsInSet-numItemsInPremise-1);
	premise = new ItemSet(m_totalTransactions);
	consequence = new ItemSet(m_totalTransactions);
	premise.m_items = new int[m_items.length];
	consequence.m_items = new int[m_items.length];
	consequence.m_counter = m_counter;
	help = j;
	for (int i = 0; i < m_items.length; i++) 
	  if (m_items[i] != -1) {
	    if (help % 2 == 1) {          
	      premise.m_items[i] = m_items[i];
	      consequence.m_items[i] = -1;
	    } else {
	      premise.m_items[i] = -1;
	      consequence.m_items[i] = m_items[i];
	    }
	    help /= 2;
	  } else {
	    premise.m_items[i] = -1;
	    consequence.m_items[i] = -1;
	  }
	premise.m_counter = ((Integer)hashtableForPremise.get(premise)).intValue();
	consequenceUnconditionedCounter =
	  ((Integer)hashtableForConsequence.get(consequence)).intValue();

	if (metricType == 0) {
	  contingencyTable[0][0] = (double)(consequence.m_counter);
	  contingencyTable[0][1] = (double)(premise.m_counter - consequence.m_counter);
	  contingencyTable[1][0] = (double)(consequenceUnconditionedCounter -
					    consequence.m_counter);
	  contingencyTable[1][1] = (double)(numTransactions - premise.m_counter -
					    consequenceUnconditionedCounter +
					    consequence.m_counter);
	  chiSquared = ContingencyTables.chiSquared(contingencyTable, false);
	
	  metric = confidenceForRule(premise, consequence);
	
	  if ((!(metric < minMetric)) &&
	      (!(chiSquared > significanceLevel))) {
	    premises.addElement(premise);
	    consequences.addElement(consequence);
	    conf.addElement(new Double(metric));
	    lift.addElement(new Double(liftForRule(premise, consequence, 
				       consequenceUnconditionedCounter)));
	    lev.addElement(new Double(leverageForRule(premise, consequence,
				     premise.m_counter,
				     consequenceUnconditionedCounter)));
	    conv.addElement(new Double(convictionForRule(premise, consequence,
				       premise.m_counter,
				       consequenceUnconditionedCounter)));
	  }
	} else {
	  double tempConf = confidenceForRule(premise, consequence);
	  double tempLift = liftForRule(premise, consequence, 
					consequenceUnconditionedCounter);
	  double tempLev = leverageForRule(premise, consequence,
					   premise.m_counter,
					   consequenceUnconditionedCounter);
	  double tempConv = convictionForRule(premise, consequence,
					      premise.m_counter,
					      consequenceUnconditionedCounter);
	  switch(metricType) {
	  case 1: 
	    metric = tempLift;
	    break;
	  case 2:
	    metric = tempLev;
	    break;
	  case 3: 
	    metric = tempConv;
	    break;
	  default:
	    throw new Exception("ItemSet: Unknown metric type!");
	  }
	  if (!(metric < minMetric)) {
	    premises.addElement(premise);
	    consequences.addElement(consequence);
	    conf.addElement(new Double(tempConf));
	    lift.addElement(new Double(tempLift));
	    lev.addElement(new Double(tempLev));
	    conv.addElement(new Double(tempConv));
	  }
	}
      }
    }
    rules[0] = premises;
    rules[1] = consequences;
    rules[2] = conf;
    rules[3] = lift;
    rules[4] = lev;
    rules[5] = conv;
    return rules;
  }

  /**
   * Return a hashtable filled with the given item sets.
   *
   * @param itemSets the set of item sets to be used for filling the hash table
   * @param initialSize the initial size of the hashtable
   * @return the generated hashtable
   */
  public static Hashtable getHashtable(FastVector itemSets, int initialSize) {

    Hashtable hashtable = new Hashtable(initialSize);

    for (int i = 0; i < itemSets.size(); i++) {
      ItemSet current = (ItemSet)itemSets.elementAt(i);
      hashtable.put(current, new Integer(current.m_counter));
    }
    return hashtable;
  }

  /**
   * Produces a hash code for a item set.
   *
   * @return a hash code for a set of items
   */
  public final int hashCode() {

    long result = 0;

    for (int i = m_items.length-1; i >= 0; i--)
      result += (i * m_items[i]);
    return (int)result;
  }

  /**
   * Merges all item sets in the set of (k-1)-item sets 
   * to create the (k)-item sets and updates the counters.
   *
   * @param itemSets the set of (k-1)-item sets
   * @param size the value of (k-1)
   * @return the generated (k)-item sets
   */
  public static FastVector mergeAllItemSets(FastVector itemSets, int size, 
					    int totalTrans) {

    FastVector newVector = new FastVector();
    ItemSet result;
    int numFound, k;

    for (int i = 0; i < itemSets.size(); i++) {
      ItemSet first = (ItemSet)itemSets.elementAt(i);
    out:
      for (int j = i+1; j < itemSets.size(); j++) {
	ItemSet second = (ItemSet)itemSets.elementAt(j);
	result = new ItemSet(totalTrans);
	result.m_items = new int[first.m_items.length];

	// Find and copy common prefix of size 'size'
	numFound = 0;
	k = 0;
	while (numFound < size) {
	  if (first.m_items[k] == second.m_items[k]) {
	    if (first.m_items[k] != -1) 
	      numFound++;
	    result.m_items[k] = first.m_items[k];
	  } else 
	    break out;
	  k++;
	}
	
	// Check difference
	while (k < first.m_items.length) {
	  if ((first.m_items[k] != -1) && (second.m_items[k] != -1))
	    break;
	  else {
	    if (first.m_items[k] != -1)
	      result.m_items[k] = first.m_items[k];
	    else
	      result.m_items[k] = second.m_items[k];
	  }
	  k++;
	}
	if (k == first.m_items.length) {
	  result.m_counter = 0;
	  newVector.addElement(result);
	}
      }
    }
    return newVector;
  }

  /**
   * Prunes a set of (k)-item sets using the given (k-1)-item sets.
   *
   * @param toPrune the set of (k)-item sets to be pruned
   * @param kMinusOne the (k-1)-item sets to be used for pruning
   * @return the pruned set of item sets
   */
  public static FastVector pruneItemSets(FastVector toPrune, Hashtable kMinusOne) {

    FastVector newVector = new FastVector(toPrune.size());
    int help, j;

    for (int i = 0; i < toPrune.size(); i++) {
      ItemSet current = (ItemSet)toPrune.elementAt(i);
      for (j = 0; j < current.m_items.length; j++)
	if (current.m_items[j] != -1) {
	  help = current.m_items[j];
	  current.m_items[j] = -1;
	  if (kMinusOne.get(current) == null) {
	    current.m_items[j] = help;
	    break;
	  } else 
	    current.m_items[j] = help;
	}
      if (j == current.m_items.length) 
	newVector.addElement(current);
    }
    return newVector;
  }

  /**
   * Prunes a set of rules.
   *
   * @param rules a two-dimensional array of lists of item sets. The first list
   * of item sets contains the premises, the second one the consequences.
   * @param minConfidence the minimum confidence the rules have to have
   */
  public static void pruneRules(FastVector[] rules, double minConfidence) {

    FastVector newPremises = new FastVector(rules[0].size()),
      newConsequences = new FastVector(rules[1].size()),
      newConf = new FastVector(rules[2].size());

    for (int i = 0; i < rules[0].size(); i++) 
      if (!(((Double)rules[2].elementAt(i)).doubleValue() <
	    minConfidence)) {
	newPremises.addElement(rules[0].elementAt(i));
	newConsequences.addElement(rules[1].elementAt(i));
	newConf.addElement(rules[2].elementAt(i));
      }
    rules[0] = newPremises;
    rules[1] = newConsequences;
    rules[2] = newConf;
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

    for (int i = 0; i < instances.numAttributes(); i++) {
      if (instances.attribute(i).isNumeric())
	throw new Exception("Can't handle numeric attributes!");
      for (int j = 0; j < instances.attribute(i).numValues(); j++) {
	current = new ItemSet(instances.numInstances());
	current.m_items = new int[instances.numAttributes()];
	for (int k = 0; k < instances.numAttributes(); k++)
	  current.m_items[k] = -1;
	current.m_items[i] = j;
	setOfItemSets.addElement(current);
      }
    }
    return setOfItemSets;
  }
  
  /**
   * Subtracts an item set from another one.
   *
   * @param toSubtract the item set to be subtracted from this one.
   * @return an item set that only contains items form this item sets that
   * are not contained by toSubtract
   */
  public final ItemSet subtract(ItemSet toSubtract) {

    ItemSet result = new ItemSet(m_totalTransactions);
    
    result.m_items = new int[m_items.length];
    for (int i = 0; i < m_items.length; i++) 
      if (toSubtract.m_items[i] == -1)
	result.m_items[i] = m_items[i];
      else
	result.m_items[i] = -1;
    result.m_counter = 0;
    return result;
  }

  /**
   * Outputs the support for an item set.
   *
   * @return the support
   */
  public final int support() {

    return m_counter;
  }

  /**
   * Returns the contents of an item set as a string.
   *
   * @param instances contains the relevant header information
   * @return string describing the item set
   */
  public final String toString(Instances instances) {

    StringBuffer text = new StringBuffer();

    for (int i = 0; i < instances.numAttributes(); i++)
      if (m_items[i] != -1) {
	text.append(instances.attribute(i).name()+'=');
	text.append(instances.attribute(i).value(m_items[i])+' ');
      }
    text.append(m_counter);
    return text.toString();
  }

  /**
   * Updates counter of item set with respect to given transaction.
   *
   * @param instance the instance to be used for ubdating the counter
   */
  public final void upDateCounter(Instance instance) {

    if (containedBy(instance))
      m_counter++;
  }

  /**
   * Updates counters for a set of item sets and a set of instances.
   *
   * @param itemSets the set of item sets which are to be updated
   * @param instances the instances to be used for updating the counters
   */
  public static void upDateCounters(FastVector itemSets, Instances instances) {

    for (int i = 0; i < instances.numInstances(); i++) {
      Enumeration enum = itemSets.elements();
      while (enum.hasMoreElements()) 
	((ItemSet)enum.nextElement()).upDateCounter(instances.instance(i));
    }
  }

  /**
   * Generates rules with more than one item in the consequence.
   *
   * @param rules all the rules having (k-1)-item sets as consequences
   * @param numItemsInSet the size of the item set for which the rules
   * are to be generated
   * @param numItemsInConsequence the value of (k-1)
   * @param minConfidence the minimum confidence a rule has to have
   * @param hashtables the hashtables containing all(!) previously generated
   * item sets
   * @return all the rules having (k)-item sets as consequences
   */
  private final FastVector[] moreComplexRules(FastVector[] rules, 
					      int numItemsInSet, 
					      int numItemsInConsequence,
					      double minConfidence, 
					      FastVector hashtables) {

    ItemSet newPremise;
    FastVector[] result, moreResults;
    FastVector newConsequences, newPremises = new FastVector(), 
      newConf = new FastVector();
    Hashtable hashtable;

    if (numItemsInSet > numItemsInConsequence + 1) {
      hashtable =
	(Hashtable)hashtables.elementAt(numItemsInSet - numItemsInConsequence - 2);
      newConsequences = mergeAllItemSets(rules[1], 
					 numItemsInConsequence - 1,
					 m_totalTransactions);
      Enumeration enum = newConsequences.elements();
      while (enum.hasMoreElements()) {
	ItemSet current = (ItemSet)enum.nextElement();
	current.m_counter = m_counter;
	newPremise = subtract(current);
	newPremise.m_counter = ((Integer)hashtable.get(newPremise)).intValue();
	newPremises.addElement(newPremise);
	newConf.addElement(new Double(confidenceForRule(newPremise, current)));
      }
      result = new FastVector[3];
      result[0] = newPremises;
      result[1] = newConsequences;
      result[2] = newConf;
      pruneRules(result, minConfidence);
      moreResults = moreComplexRules(result,numItemsInSet,numItemsInConsequence+1,
				     minConfidence, hashtables);
      if (moreResults != null) 
	for (int i = 0; i < moreResults[0].size(); i++) {
	  result[0].addElement(moreResults[0].elementAt(i));
	  result[1].addElement(moreResults[1].elementAt(i));
	  result[2].addElement(moreResults[2].elementAt(i));
	}
      return result;
    } else
      return null;
  }
}
