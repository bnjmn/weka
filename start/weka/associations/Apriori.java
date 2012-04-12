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
 * Class implementing an Apriori-type algorithm
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public class Apriori implements OptionHandler {

  // =================
  // Private variables
  // =================

  /**
   * The set of all sets of itemsets L.
   */

  private FastVector Ls;

  /**
   * The same information stored in hash tables.
   */

  private FastVector hashtables;

  /**
   * The list of all generated rules.
   */

  private FastVector[] allTheRules;

  /**
   * The minimum support.
   */

  private double minSupport;

  /**
   * The lower bound for the minimum support.
   */
  
  private double lowerBoundMinSupport;

  /**
   * The minimum confidence.
   */

  private double minConfidence;

  /**
   * The maximum number of rules that are output.
   */

  private int numRules;

  /**
   * Delta by which minSupport is decreased in each iteration.
   */

  private double delta;

  /**
   * Number of cycles used before required number of rules was one.
   */

  private int cycles;

  /**
   * Significance level for optional significance test.
   */

  private double significanceLevel;

  /**
   * The instances (transactions) to be used for generating the association rules.
   */

  private Instances theInstances;

  /**
   * Output itemsets found?
   */
  
  private boolean outputItemSets;

  // ==============
  // Public methods
  // ==============

  /**
   * Constructor that allows to sets default values for the 
   * minimum confidence and the maximum number of rules
   * the minimum confidence.
   */

  public Apriori() {
    resetOptions();
  }

  public void resetOptions() {
    delta = 0.05;
    minConfidence = 0.90;
    numRules = 10;
    lowerBoundMinSupport = 0.1;
    significanceLevel = -1;
    outputItemSets = false;
  }

  /**
   * Method that generates all large itemsets with a minimum support, and from
   * these all association rules with a minimum confidence.
   * @param instances the instances to be used for generating the associations
   * @exception Exception if rules can't be built successfully
   */

  public void buildAssociations(Instances instances) throws Exception {

    double[] confidences, supports;
    int[] indices;
    FastVector[] sortedRuleSet;

    // Decrease minimum support until desired number of rules found.

    cycles = 0;
    minSupport = 1.0;
    do {

      // Reserve space for variables
      
      Ls = new FastVector();
      hashtables = new FastVector();
      allTheRules = new FastVector[3];
      allTheRules[0] = new FastVector();
      allTheRules[1] = new FastVector();
      allTheRules[2] = new FastVector();
      sortedRuleSet = new FastVector[3];
      sortedRuleSet[0] = new FastVector();
      sortedRuleSet[1] = new FastVector();
      sortedRuleSet[2] = new FastVector();

      // Find large itemsets and rules
      
      findLargeItemSets(instances);
      if (significanceLevel != -1) 
	findRulesBruteForce();
      else
	findRulesQuickly();
      
      // Sort rules according to their support

      supports = new double[allTheRules[2].size()];
      for (int i = 0; i < allTheRules[2].size(); i++) 
	supports[i] = (double)((ItemSet)allTheRules[1].elementAt(i)).support();
      indices = Utils.sort(supports);
      for (int i = 0; i < allTheRules[2].size(); i++) {
	sortedRuleSet[0].addElement(allTheRules[0].elementAt(indices[i]));
	sortedRuleSet[1].addElement(allTheRules[1].elementAt(indices[i]));
	sortedRuleSet[2].addElement(allTheRules[2].elementAt(indices[i]));
      }

      // Sort rules according to their confidence

      allTheRules[0].removeAllElements();
      allTheRules[1].removeAllElements();
      allTheRules[2].removeAllElements();
      confidences = new double[sortedRuleSet[2].size()];
      for (int i = 0; i < sortedRuleSet[2].size(); i++) 
	confidences[i] = ((Double)sortedRuleSet[2].elementAt(i)).doubleValue();
      indices = Utils.sort(confidences);
      for (int i = sortedRuleSet[0].size() - 1; 
	   (i >= (sortedRuleSet[0].size() - numRules)) && (i >= 0); i--) {
	allTheRules[0].addElement(sortedRuleSet[0].elementAt(indices[i]));
	allTheRules[1].addElement(sortedRuleSet[1].elementAt(indices[i]));
	allTheRules[2].addElement(sortedRuleSet[2].elementAt(indices[i]));
      }
      minSupport -= delta;
      cycles++;
    } while ((allTheRules[0].size() < numRules) && 
	     (Utils.grOrEq(minSupport, lowerBoundMinSupport)));
    minSupport += delta;
  }

  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   */

  public Enumeration listOptions() {

    String string1 = "\tThe required number of rules. (default = " + numRules + ")",
      string2 = 
      "\tThe minimum confidence of a rule. (default = " + minConfidence + ")",
      string3 = "\tThe delta by which the minimum support is decreased in\n",
      string4 = "\teach iteration. (default = " + delta + ")",
      string5 = 
      "\tThe lower bound for the minimum support. (default = " + 
      lowerBoundMinSupport + ")",
      string6 = "\tIf used, rules are tested for significance at\n",
      string7 = "\tthe given level. Slower. (default = no significance testing)",
      string8 = "\tIf set the itemsets found are also output. (default = no)";

    FastVector newVector = new FastVector(2);

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
   * Parses a given list of options.
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
    
    if (numRulesString.length() != 0)
      numRules = Integer.parseInt(numRulesString);

    if (minConfidenceString.length() != 0)
      minConfidence = (new Double(minConfidenceString)).doubleValue();
    if (deltaString.length() != 0)
      delta = (new Double(deltaString)).doubleValue();
    if (minSupportString.length() != 0)
      lowerBoundMinSupport = (new Double(minSupportString)).doubleValue();
    if (significanceLevelString.length() != 0)
      significanceLevel = (new Double(significanceLevelString)).doubleValue();
    outputItemSets = Utils.getFlag('I', options);
  }

  /**
   * Gets the current settings of the Apriori object.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [11];
    int current = 0;

    if (outputItemSets) {
      options[current++] = "-I";
    }
    options[current++] = "-N"; options[current++] = "" + numRules;
    options[current++] = "-C"; options[current++] = "" + minConfidence;
    options[current++] = "-D"; options[current++] = "" + delta;
    options[current++] = "-M"; options[current++] = "" + minSupport;
    options[current++] = "-S"; options[current++] = "" + significanceLevel;

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

    if (Ls.size() <= 1)
      return "\nNo large itemsets and rules found!\n";
    text.append("\nApriori\n=======\n\n");
    text.append("Minimum support: " + Utils.doubleToString(minSupport,2) + '\n');
    text.append("Minimum confidence: "+Utils.doubleToString(minConfidence,2)+'\n');
    if (significanceLevel != -1)
      text.append("Significance level: "+
		  Utils.doubleToString(significanceLevel,2)+'\n');
    text.append("Number of cycles performed: " + cycles+'\n');
    text.append("\nGenerated sets of large itemsets:\n");
    for (int i = 0; i < Ls.size(); i++) {
      text.append("\nSize of set of large itemsets L("+(i+1)+"): "+
		  ((FastVector)Ls.elementAt(i)).size()+'\n');
      if (outputItemSets) {
	text.append("\nLarge Itemsets L("+(i+1)+"):\n");
	for (int j = 0; j < ((FastVector)Ls.elementAt(i)).size(); j++)
	  text.append(((ItemSet)((FastVector)Ls.elementAt(i)).elementAt(j)).
		      toString(theInstances)+"\n");
      }
    }
    text.append("\nBest rules found:\n\n");
    for (int i = 0; i < allTheRules[0].size(); i++) 
      text.append(Utils.doubleToString((double)i+1, 
				       (int)(Math.log(numRules)/Math.log(10)+1),0)+
		  ". " + ((ItemSet)allTheRules[0].elementAt(i)).
		  toString(theInstances) 
		  + " ==> " + ((ItemSet)allTheRules[1].elementAt(i)).
		  toString(theInstances) +" ("+  
		  Utils.doubleToString(((Double)allTheRules[2].
					elementAt(i)).doubleValue(),2)+")\n");
    return text.toString();
  }

  // ===============
  // Private methods
  // ===============

  /** 
   * Method that finds all large itemsets for the given set of instances.
   * @param the instances to be used
   * @exception Exception if an attribute is numeric
   */

  private void findLargeItemSets(Instances instances) throws Exception {
    
    FastVector kMinusOneSets, kSets;
    Hashtable hashtable;
    int necSupport, i = 0;
    
    theInstances = instances;
    
    // Find large itemsets

    necSupport = (int)(minSupport * (double)instances.numInstances());
    kSets = ItemSet.singletons(instances);
    ItemSet.upDateCounters(kSets, instances);
    kSets = ItemSet.deleteItemSets(kSets, necSupport);
    if (kSets.size() == 0)
      return;
    do {
      Ls.addElement(kSets);
      kMinusOneSets = kSets;
      kSets = ItemSet.mergeAllItemSets(kMinusOneSets, i);
      hashtable = ItemSet.getHashtable(kMinusOneSets, kMinusOneSets.size());
      hashtables.addElement(hashtable);
      kSets = ItemSet.pruneItemSets(kSets, hashtable);
      ItemSet.upDateCounters(kSets, instances);
      kSets = ItemSet.deleteItemSets(kSets, necSupport);
      i++;
    } while (kSets.size() > 0);
  }  

  /** 
   * Method that finds all association rules and performs significance test.
   * @exception Exception if an attribute is numeric
   */

  private void findRulesBruteForce() throws Exception {

    FastVector[] rules;

    // Build rules

    for (int j = 1; j < Ls.size(); j++) {
      FastVector currentItemSets = (FastVector)Ls.elementAt(j);
      Enumeration enumItemSets = currentItemSets.elements();
      while (enumItemSets.hasMoreElements()) {
	ItemSet currentItemSet = (ItemSet)enumItemSets.nextElement();
	rules=currentItemSet.generateRulesBruteForce(minConfidence,hashtables,j+1,
						     theInstances.numInstances(),
						     significanceLevel);
	for (int k = 0; k < rules[0].size(); k++) {
	  allTheRules[0].addElement(rules[0].elementAt(k));
	  allTheRules[1].addElement(rules[1].elementAt(k));
	  allTheRules[2].addElement(rules[2].elementAt(k));
	}
      }
    }
  }

  /** 
   * Method that finds all association rules.
   * @exception Exception if an attribute is numeric
   */

  private void findRulesQuickly() throws Exception {

    FastVector[] rules;

    // Build rules

    for (int j = 1; j < Ls.size(); j++) {
      FastVector currentItemSets = (FastVector)Ls.elementAt(j);
      Enumeration enumItemSets = currentItemSets.elements();
      while (enumItemSets.hasMoreElements()) {
	ItemSet currentItemSet = (ItemSet)enumItemSets.nextElement();
	rules = currentItemSet.generateRules(minConfidence, hashtables, j + 1);
	for (int k = 0; k < rules[0].size(); k++) {
	  allTheRules[0].addElement(rules[0].elementAt(k));
	  allTheRules[1].addElement(rules[1].elementAt(k));
	  allTheRules[2].addElement(rules[2].elementAt(k));
	}
      }
    }
  }

  // ===========
  // Main method
  // ===========

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
      e.printStackTrace();
    }
  }
}



