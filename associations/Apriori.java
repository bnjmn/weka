/*
 *    Apriori.java
 *    Copyright (C) 1999 Eibe Frank
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
package weka.associations;

import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class implementing an Apriori-type algorithm. Iteratively reduces the minimum
 * support until it finds the required number of rules with the given minimum 
 * confidence. <p>
 *
 * Reference: R. Agrawal, R. Srikant (1994). <i>Fast algorithms for
 * mining association rules in large databases </i>. Proc
 * International Conference on Very Large Databases,
 * pp. 478-499. Santiage, Chile: Morgan Kaufmann, Los Altos, CA. <p>
 *
 * Valid options are:<p>
 *   
 * -N required number of rules <br>
 * The required number of rules (default: 10). <p>
 *
 * -C minimum confidence of a rule <br>
 * The minimum confidence of a rule (default: 0.9). <p>
 *
 * -D delta for minimum support <br>
 * The delta by which the minimum support is decreased in
 * each iteration (default: 0.05).
 *
 * -M lower bound for minimum support <br>
 * The lower bound for the minimum support (default = 0.1). <p>
 *
 * -S significance level <br>
 * If used, rules are tested for significance at
 * the given level. Slower (default = no significance testing). <p>
 *
 * -I <br>
 * If set the itemsets found are also output (default = no). <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $ */
public class Apriori extends Associator implements OptionHandler {
  
  /** The minimum support. */
  private double m_minSupport;

  /** The lower bound for the minimum support. */
  private double m_lowerBoundMinSupport;

  /** The minimum confidence. */
  private double m_minConfidence;

  /** The maximum number of rules that are output. */
  private int m_numRules;

  /** Delta by which m_minSupport is decreased in each iteration. */
  private double m_delta;

  /** Significance level for optional significance test. */
  private double m_significanceLevel;

  /** Number of cycles used before required number of rules was one. */
  private int m_cycles;

  /** The set of all sets of itemsets L. */
  private FastVector m_Ls;

  /** The same information stored in hash tables. */
  private FastVector m_hashtables;

  /** The list of all generated rules. */
  private FastVector[] m_allTheRules;

  /** The instances (transactions) to be used for generating the association rules. */
  private Instances m_instances;

  /** Output itemsets found? */
  private boolean m_outputItemSets;

  /**
   * Constructor that allows to sets default values for the 
   * minimum confidence and the maximum number of rules
   * the minimum confidence.
   */
  public Apriori() {

    resetOptions();
  }

  /**
   * Resets the options to the default values.
   */
  public void resetOptions() {

    m_delta = 0.05;
    m_minConfidence = 0.90;
    m_numRules = 10;
    m_lowerBoundMinSupport = 0.1;
    m_significanceLevel = -1;
    m_outputItemSets = false;
  }

  /**
   * Method that generates all large itemsets with a minimum support, and from
   * these all association rules with a minimum confidence.
   *
   * @param instances the instances to be used for generating the associations
   * @exception Exception if rules can't be built successfully
   */
  public void buildAssociations(Instances instances) throws Exception {

    double[] confidences, supports;
    int[] indices;
    FastVector[] sortedRuleSet;
    int necSupport=0;

    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }

    // Decrease minimum support until desired number of rules found.
    m_cycles = 0;
    m_minSupport = 1.0;
    do {

      // Reserve space for variables
      m_Ls = new FastVector();
      m_hashtables = new FastVector();
      m_allTheRules = new FastVector[3];
      m_allTheRules[0] = new FastVector();
      m_allTheRules[1] = new FastVector();
      m_allTheRules[2] = new FastVector();
      sortedRuleSet = new FastVector[3];
      sortedRuleSet[0] = new FastVector();
      sortedRuleSet[1] = new FastVector();
      sortedRuleSet[2] = new FastVector();

      // Find large itemsets and rules
      findLargeItemSets(instances);
      if (m_significanceLevel != -1) 
	findRulesBruteForce();
      else
	findRulesQuickly();
      
      // Sort rules according to their support
      supports = new double[m_allTheRules[2].size()];
      for (int i = 0; i < m_allTheRules[2].size(); i++) 
	supports[i] = (double)((ItemSet)m_allTheRules[1].elementAt(i)).support();
      indices = Utils.sort(supports);
      for (int i = 0; i < m_allTheRules[2].size(); i++) {
	sortedRuleSet[0].addElement(m_allTheRules[0].elementAt(indices[i]));
	sortedRuleSet[1].addElement(m_allTheRules[1].elementAt(indices[i]));
	sortedRuleSet[2].addElement(m_allTheRules[2].elementAt(indices[i]));
      }

      // Sort rules according to their confidence
      m_allTheRules[0].removeAllElements();
      m_allTheRules[1].removeAllElements();
      m_allTheRules[2].removeAllElements();
      confidences = new double[sortedRuleSet[2].size()];
      for (int i = 0; i < sortedRuleSet[2].size(); i++) 
	confidences[i] = ((Double)sortedRuleSet[2].elementAt(i)).doubleValue();
      indices = Utils.sort(confidences);
      for (int i = sortedRuleSet[0].size() - 1; 
	   (i >= (sortedRuleSet[0].size() - m_numRules)) && (i >= 0); i--) {
	m_allTheRules[0].addElement(sortedRuleSet[0].elementAt(indices[i]));
	m_allTheRules[1].addElement(sortedRuleSet[1].elementAt(indices[i]));
	m_allTheRules[2].addElement(sortedRuleSet[2].elementAt(indices[i]));
      }
      m_minSupport -= m_delta;
      necSupport = (int)(m_minSupport * 
			 (double)instances.numInstances()+0.5);
      m_cycles++;
    } while ((m_allTheRules[0].size() < m_numRules) && 
	     (Utils.grOrEq(m_minSupport, m_lowerBoundMinSupport)) &&
	     (necSupport >= 1));
    m_minSupport += m_delta;
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    String string1 = "\tThe required number of rules. (default = " + m_numRules + ")",
      string2 = 
      "\tThe minimum confidence of a rule. (default = " + m_minConfidence + ")",
      string3 = "\tThe delta by which the minimum support is decreased in\n",
      string4 = "\teach iteration. (default = " + m_delta + ")",
      string5 = 
      "\tThe lower bound for the minimum support. (default = " + 
      m_lowerBoundMinSupport + ")",
      string6 = "\tIf used, rules are tested for significance at\n",
      string7 = "\tthe given level. Slower. (default = no significance testing)",
      string8 = "\tIf set the itemsets found are also output. (default = no)";

    FastVector newVector = new FastVector(6);

    newVector.addElement(new Option(string1, "N", 1, 
				    "-N <required number of rules output>"));
    newVector.addElement(new Option(string2, "C", 1, 
				    "-C <minimum confidence of a rule>"));
    newVector.addElement(new Option(string3 + string4, "D", 1,
				    "-D <delta for minimum support>"));
    newVector.addElement(new Option(string5, "M", 1,
				    "-M <lower bound for minimum support>"));
    newVector.addElement(new Option(string6 + string7, "S", 1,
				    "-S <significance level>"));
    newVector.addElement(new Option(string8, "S", 0,
				    "-I"));
    
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *   
   * -N required number of rules <br>
   * The required number of rules (default: 10). <p>
   *
   * -C minimum confidence of a rule <br>
   * The minimum confidence of a rule (default: 0.9). <p>
   *
   * -D delta for minimum support <br>
   * The delta by which the minimum support is decreased in
   * each iteration (default: 0.05).
   *
   * -M lower bound for minimum support <br>
   * The lower bound for the minimum support (default = 0.1). <p>
   *
   * -S significance level <br>
   * If used, rules are tested for significance at
   * the given level. Slower (default = no significance testing). <p>
   *
   * -I <br>
   * If set the itemsets found are also output (default = no). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported 
   */
  public void setOptions(String[] options) throws Exception{
    
    resetOptions();
    String numRulesString = Utils.getOption('N', options),
      minConfidenceString = Utils.getOption('C', options),
      deltaString = Utils.getOption('D', options),
      minSupportString = Utils.getOption('M', options),
      significanceLevelString = Utils.getOption('S', options);
    
    if (numRulesString.length() != 0) {
      m_numRules = Integer.parseInt(numRulesString);
    }
    if (minConfidenceString.length() != 0) {
      m_minConfidence = (new Double(minConfidenceString)).doubleValue();
    }
    if (deltaString.length() != 0) {
      m_delta = (new Double(deltaString)).doubleValue();
    }
    if (minSupportString.length() != 0) {
      m_lowerBoundMinSupport = (new Double(minSupportString)).doubleValue();
    }
    if (significanceLevelString.length() != 0) {
      m_significanceLevel = (new Double(significanceLevelString)).doubleValue();
    }
    m_outputItemSets = Utils.getFlag('I', options);
  }

  /**
   * Gets the current settings of the Apriori object.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [11];
    int current = 0;

    if (m_outputItemSets) {
      options[current++] = "-I";
    }
    options[current++] = "-N"; options[current++] = "" + m_numRules;
    options[current++] = "-C"; options[current++] = "" + m_minConfidence;
    options[current++] = "-D"; options[current++] = "" + m_delta;
    options[current++] = "-M"; options[current++] = "" + m_minSupport;
    options[current++] = "-S"; options[current++] = "" + m_significanceLevel;

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Outputs the size of all the generated sets of itemsets and the rules.
   */
  public String toString() {

    StringBuffer text = new StringBuffer();

    if (m_Ls.size() <= 1)
      return "\nNo large itemsets and rules found!\n";
    text.append("\nApriori\n=======\n\n");
    text.append("Minimum support: " + Utils.doubleToString(m_minSupport,2) + '\n');
    text.append("Minimum confidence: "+Utils.doubleToString(m_minConfidence,2)+'\n');
    if (m_significanceLevel != -1)
      text.append("Significance level: "+
		  Utils.doubleToString(m_significanceLevel,2)+'\n');
    text.append("Number of cycles performed: " + m_cycles+'\n');
    text.append("\nGenerated sets of large itemsets:\n");
    for (int i = 0; i < m_Ls.size(); i++) {
      text.append("\nSize of set of large itemsets L("+(i+1)+"): "+
		  ((FastVector)m_Ls.elementAt(i)).size()+'\n');
      if (m_outputItemSets) {
	text.append("\nLarge Itemsets L("+(i+1)+"):\n");
	for (int j = 0; j < ((FastVector)m_Ls.elementAt(i)).size(); j++)
	  text.append(((ItemSet)((FastVector)m_Ls.elementAt(i)).elementAt(j)).
		      toString(m_instances)+"\n");
      }
    }
    text.append("\nBest rules found:\n\n");
    for (int i = 0; i < m_allTheRules[0].size(); i++) 
      text.append(Utils.doubleToString((double)i+1, 
				       (int)(Math.log(m_numRules)/Math.log(10)+1),0)+
		  ". " + ((ItemSet)m_allTheRules[0].elementAt(i)).
		  toString(m_instances) 
		  + " ==> " + ((ItemSet)m_allTheRules[1].elementAt(i)).
		  toString(m_instances) +" ("+  
		  Utils.doubleToString(((Double)m_allTheRules[2].
					elementAt(i)).doubleValue(),2)+")\n");
    return text.toString();
  }
  
  /**
   * Get the value of minSupport.
   *
   * @return Value of minSupport.
   */
  public double getMinSupport() {
    
    return m_minSupport;
  }
  
  /**
   * Set the value of minSupport.
   *
   * @param v  Value to assign to minSupport.
   */
  public void setMinSupport(double v) {
    
    m_minSupport = v;
  }
  
  /**
   * Get the value of lowerBoundMinSupport.
   *
   * @return Value of lowerBoundMinSupport.
   */
  public double getLowerBoundMinSupport() {
    
    return m_lowerBoundMinSupport;
  }
  
  /**
   * Set the value of lowerBoundMinSupport.
   *
   * @param v  Value to assign to lowerBoundMinSupport.
   */
  public void setLowerBoundMinSupport(double v) {
    
    m_lowerBoundMinSupport = v;
  }
  
  /**
   * Get the value of minConfidence.
   *
   * @return Value of minConfidence.
   */
  public double getMinConfidence() {
    
    return m_minConfidence;
  }
  
  /**
   * Set the value of minConfidence.
   *
   * @param v  Value to assign to minConfidence.
   */
  public void setMinConfidence(double v) {
    
    m_minConfidence = v;
  }
  
  /**
   * Get the value of numRules.
   *
   * @return Value of numRules.
   */
  public int getNumRules() {
    
    return m_numRules;
  }
  
  /**
   * Set the value of numRules.
   *
   * @param v  Value to assign to numRules.
   */
  public void setNumRules(int v) {
    
    m_numRules = v;
  }
  
  /**
   * Get the value of delta.
   *
   * @return Value of delta.
   */
  public double getDelta() {
    
    return m_delta;
  }
  
  /**
   * Set the value of delta.
   *
   * @param v  Value to assign to delta.
   */
  public void setDelta(double v) {
    
    m_delta = v;
  }
  
  /**
   * Get the value of significanceLevel.
   *
   * @return Value of significanceLevel.
   */
  public double getSignificanceLevel() {
    
    return m_significanceLevel;
  }
  
  /**
   * Set the value of significanceLevel.
   *
   * @param v  Value to assign to significanceLevel.
   */
  public void setSignificanceLevel(double v) {
    
    m_significanceLevel = v;
  }

  /** 
   * Method that finds all large itemsets for the given set of instances.
   *
   * @param the instances to be used
   * @exception Exception if an attribute is numeric
   */
  private void findLargeItemSets(Instances instances) throws Exception {
    
    FastVector kMinusOneSets, kSets;
    Hashtable hashtable;
    int necSupport, i = 0;
    
    m_instances = instances;
    
    // Find large itemsets
    necSupport = (int)(m_minSupport * (double)instances.numInstances()+0.5);
   
    kSets = ItemSet.singletons(instances);
    ItemSet.upDateCounters(kSets, instances);
    kSets = ItemSet.deleteItemSets(kSets, necSupport);
    if (kSets.size() == 0)
      return;
    do {
      m_Ls.addElement(kSets);
      kMinusOneSets = kSets;
      kSets = ItemSet.mergeAllItemSets(kMinusOneSets, i);
      hashtable = ItemSet.getHashtable(kMinusOneSets, kMinusOneSets.size());
      m_hashtables.addElement(hashtable);
      kSets = ItemSet.pruneItemSets(kSets, hashtable);
      ItemSet.upDateCounters(kSets, instances);
      kSets = ItemSet.deleteItemSets(kSets, necSupport);
      i++;
    } while (kSets.size() > 0);
  }  

  /** 
   * Method that finds all association rules and performs significance test.
   *
   * @exception Exception if an attribute is numeric
   */
  private void findRulesBruteForce() throws Exception {

    FastVector[] rules;

    // Build rules
    for (int j = 1; j < m_Ls.size(); j++) {
      FastVector currentItemSets = (FastVector)m_Ls.elementAt(j);
      Enumeration enumItemSets = currentItemSets.elements();
      while (enumItemSets.hasMoreElements()) {
	ItemSet currentItemSet = (ItemSet)enumItemSets.nextElement();
	rules=currentItemSet.generateRulesBruteForce(m_minConfidence,m_hashtables,j+1,
						     m_instances.numInstances(),
						     m_significanceLevel);
	for (int k = 0; k < rules[0].size(); k++) {
	  m_allTheRules[0].addElement(rules[0].elementAt(k));
	  m_allTheRules[1].addElement(rules[1].elementAt(k));
	  m_allTheRules[2].addElement(rules[2].elementAt(k));
	}
      }
    }
  }

  /** 
   * Method that finds all association rules.
   *
   * @exception Exception if an attribute is numeric
   */
  private void findRulesQuickly() throws Exception {

    FastVector[] rules;

    // Build rules
    for (int j = 1; j < m_Ls.size(); j++) {
      FastVector currentItemSets = (FastVector)m_Ls.elementAt(j);
      Enumeration enumItemSets = currentItemSets.elements();
      while (enumItemSets.hasMoreElements()) {
	ItemSet currentItemSet = (ItemSet)enumItemSets.nextElement();
	rules = currentItemSet.generateRules(m_minConfidence, m_hashtables, j + 1);
	for (int k = 0; k < rules[0].size(); k++) {
	  m_allTheRules[0].addElement(rules[0].elementAt(k));
	  m_allTheRules[1].addElement(rules[1].elementAt(k));
	  m_allTheRules[2].addElement(rules[2].elementAt(k));
	}
      }
    }
  }

  /**
   * Main method for testing this class.
   */
  public static void main(String[] options) {

    String trainFileString;
    StringBuffer text = new StringBuffer();
    Apriori apriori = new Apriori();
    Reader reader;

    try {
      text.append("\n\nApriori options:\n\n");
      text.append("-t <training file>\n");
      text.append("\tThe name of the training file.\n");
      Enumeration enum = apriori.listOptions();
      while (enum.hasMoreElements()) {
	Option option = (Option)enum.nextElement();
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
      System.out.println("\n"+e.getMessage()+text);
    }
  }
}



