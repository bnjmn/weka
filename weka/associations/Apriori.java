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
 *    Apriori.java
 *    Copyright (C) 1999 Eibe Frank,Mark Hall
 *
 */

package weka.associations;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

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
 * -T type of metric by which to sort rules <br>
 * 0 = confidence | 1 = lift | 2 = leverage | 3 = Conviction. <p>
 *
 * -C minimum confidence of a rule <br>
 * The minimum confidence of a rule (default: 0.9). <p>
 *
 * -D delta for minimum support <br>
 * The delta by which the minimum support is decreased in
 * each iteration (default: 0.05). <p>
 *
 * -U upper bound for minimum support <br>
 * The upper bound for minimum support. Don't explicitly look for 
 * rules with more than this level of support. <p>
 *
 * -M lower bound for minimum support <br>
 * The lower bound for the minimum support (default = 0.1). <p>
 *
 * -S significance level <br>
 * If used, rules are tested for significance at
 * the given level. Slower (default = no significance testing). <p>
 *
 * -R <br>
 * If set then columns that contain all missing values are removed from
 * the data.
 *
 * -I <br>
 * If set the itemsets found are also output (default = no). <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.19.2.2 $ */

public class Apriori extends Associator implements OptionHandler {

  
  /** The minimum support. */
  protected double m_minSupport;

  /** The upper bound on the support */
  protected double m_upperBoundMinSupport;

  /** The lower bound for the minimum support. */
  protected double m_lowerBoundMinSupport;

  /** Metric types. */
  protected static final int CONFIDENCE = 0;
  protected static final int LIFT = 1;
  protected static final int LEVERAGE = 2;
  protected static final int CONVICTION = 3;
  public static final Tag [] TAGS_SELECTION = {
    new Tag(CONFIDENCE, "Confidence"),
    new Tag(LIFT, "Lift"),
    new Tag(LEVERAGE, "Leverage"),
    new Tag(CONVICTION, "Conviction")
      };

  /** The selected metric type. */
  protected int m_metricType = CONFIDENCE;

  /** The minimum metric score. */
  protected double m_minMetric;

  /** The maximum number of rules that are output. */
  protected int m_numRules;

  /** Delta by which m_minSupport is decreased in each iteration. */
  protected double m_delta;

  /** Significance level for optional significance test. */
  protected double m_significanceLevel;

  /** Number of cycles used before required number of rules was one. */
  protected int m_cycles;

  /** The set of all sets of itemsets L. */
  protected FastVector m_Ls;

  /** The same information stored in hash tables. */
  protected FastVector m_hashtables;

  /** The list of all generated rules. */
  protected FastVector[] m_allTheRules;

  /** The instances (transactions) to be used for generating 
      the association rules. */
  protected Instances m_instances;

  /** Output itemsets found? */
  protected boolean m_outputItemSets;

  protected boolean m_removeMissingCols;

  /** Report progress iteratively */
  protected boolean m_verbose;

  /**
   * Returns a string describing this associator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Finds association rules.";
  }

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
    
    m_removeMissingCols = false;
    m_verbose = false;
    m_delta = 0.05;
    m_minMetric = 0.90;
    m_numRules = 10;
    m_lowerBoundMinSupport = 0.1;
    m_upperBoundMinSupport = 1.0;
    m_significanceLevel = -1;
    m_outputItemSets = false;
  }

  /**
   * Removes columns that are all missing from the data
   * @param instances the instances
   * @return a new set of instances with all missing columns removed
   */
  protected Instances removeMissingColumns(Instances instances) 
    throws Exception {
    int numInstances = instances.numInstances();
    StringBuffer deleteString = new StringBuffer();
    int removeCount = 0;
    boolean first = true;
    int maxCount = 0;
    
    for (int i=0;i<instances.numAttributes();i++) {
      AttributeStats as = instances.attributeStats(i);
      if (m_upperBoundMinSupport == 1.0 && maxCount != numInstances) {
	// see if we can decrease this by looking for the most frequent value
	int [] counts = as.nominalCounts;
	if (counts[Utils.maxIndex(counts)] > maxCount) {
	  maxCount = counts[Utils.maxIndex(counts)];
	}
      }
      if (as.missingCount == numInstances) {
	if (first) {
	  deleteString.append((i+1));
	  first = false;
	} else {
	  deleteString.append(","+(i+1));
	}
	removeCount++;
      }
    }
    if (m_verbose) {
      System.err.println("Removed : "+removeCount+" columns with all missing "
			 +"values.");
    }
    if (m_upperBoundMinSupport == 1.0 && maxCount != numInstances) {
      m_upperBoundMinSupport = (double)maxCount / (double)numInstances;
      if (m_verbose) {
	System.err.println("Setting upper bound min support to : "
			   +m_upperBoundMinSupport);
      }
    }

    if (deleteString.toString().length() > 0) {
      Remove af = new Remove();
      af.setAttributeIndices(deleteString.toString());
      af.setInvertSelection(false);
      af.setInputFormat(instances);
      Instances newInst = Filter.useFilter(instances, af);

      return newInst;
    }
    return instances;
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

    if (m_removeMissingCols) {
      instances = removeMissingColumns(instances);
    }

    // Decrease minimum support until desired number of rules found.
    m_cycles = 0;
    m_minSupport = m_upperBoundMinSupport - m_delta;
    m_minSupport = (m_minSupport < m_lowerBoundMinSupport) 
      ? m_lowerBoundMinSupport 
      : m_minSupport;
    do {

      // Reserve space for variables
      m_Ls = new FastVector();
      m_hashtables = new FastVector();
      m_allTheRules = new FastVector[6];
      m_allTheRules[0] = new FastVector();
      m_allTheRules[1] = new FastVector();
      m_allTheRules[2] = new FastVector();
      if (m_metricType != CONFIDENCE || m_significanceLevel != -1) {
	m_allTheRules[3] = new FastVector();
	m_allTheRules[4] = new FastVector();
	m_allTheRules[5] = new FastVector();
      }
      sortedRuleSet = new FastVector[6];
      sortedRuleSet[0] = new FastVector();
      sortedRuleSet[1] = new FastVector();
      sortedRuleSet[2] = new FastVector();
      if (m_metricType != CONFIDENCE || m_significanceLevel != -1) {
	sortedRuleSet[3] = new FastVector();
	sortedRuleSet[4] = new FastVector();
	sortedRuleSet[5] = new FastVector();
      }

      // Find large itemsets and rules
      findLargeItemSets(instances);
      if (m_significanceLevel != -1 || m_metricType != CONFIDENCE) 
	findRulesBruteForce();
      else
	findRulesQuickly();
      
      // Sort rules according to their support
      supports = new double[m_allTheRules[2].size()];
      for (int i = 0; i < m_allTheRules[2].size(); i++) 
	supports[i] = (double)((AprioriItemSet)m_allTheRules[1].elementAt(i)).support();
      indices = Utils.stableSort(supports);
      for (int i = 0; i < m_allTheRules[2].size(); i++) {
	sortedRuleSet[0].addElement(m_allTheRules[0].elementAt(indices[i]));
	sortedRuleSet[1].addElement(m_allTheRules[1].elementAt(indices[i]));
	sortedRuleSet[2].addElement(m_allTheRules[2].elementAt(indices[i]));
	if (m_metricType != CONFIDENCE || m_significanceLevel != -1) {
	  sortedRuleSet[3].addElement(m_allTheRules[3].elementAt(indices[i]));
	  sortedRuleSet[4].addElement(m_allTheRules[4].elementAt(indices[i]));
	  sortedRuleSet[5].addElement(m_allTheRules[5].elementAt(indices[i]));
	}
      }

      // Sort rules according to their confidence
      m_allTheRules[0].removeAllElements();
      m_allTheRules[1].removeAllElements();
      m_allTheRules[2].removeAllElements();
      if (m_metricType != CONFIDENCE || m_significanceLevel != -1) {
	m_allTheRules[3].removeAllElements();
	m_allTheRules[4].removeAllElements();
	m_allTheRules[5].removeAllElements();
      }
      confidences = new double[sortedRuleSet[2].size()];
      int sortType = 2 + m_metricType;

      for (int i = 0; i < sortedRuleSet[2].size(); i++) 
	confidences[i] = 
	  ((Double)sortedRuleSet[sortType].elementAt(i)).doubleValue();
      indices = Utils.stableSort(confidences);
      for (int i = sortedRuleSet[0].size() - 1; 
	   (i >= (sortedRuleSet[0].size() - m_numRules)) && (i >= 0); i--) {
	m_allTheRules[0].addElement(sortedRuleSet[0].elementAt(indices[i]));
	m_allTheRules[1].addElement(sortedRuleSet[1].elementAt(indices[i]));
	m_allTheRules[2].addElement(sortedRuleSet[2].elementAt(indices[i]));
	if (m_metricType != CONFIDENCE || m_significanceLevel != -1) {
	  m_allTheRules[3].addElement(sortedRuleSet[3].elementAt(indices[i]));
	  m_allTheRules[4].addElement(sortedRuleSet[4].elementAt(indices[i]));
	  m_allTheRules[5].addElement(sortedRuleSet[5].elementAt(indices[i]));
	}
      }

      if (m_verbose) {
	if (m_Ls.size() > 1) {
	  System.out.println(toString());
	}
      }
      m_minSupport -= m_delta;
      /*      m_minSupport = (m_minSupport < m_lowerBoundMinSupport) 
	? 0 
	: m_minSupport; */

      necSupport = (int)(m_minSupport * 
			 (double)instances.numInstances()+0.5);

      m_cycles++;
    } while ((m_allTheRules[0].size() < m_numRules) &&
	     (Utils.grOrEq(m_minSupport, m_lowerBoundMinSupport))
	     /*	     (necSupport >= lowerBoundNumInstancesSupport)*/
	     /*	     (Utils.grOrEq(m_minSupport, m_lowerBoundMinSupport)) */ &&     
	     (necSupport >= 1));
    m_minSupport += m_delta;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    String string1 = "\tThe required number of rules. (default = " + m_numRules + ")",
      string2 = 
      "\tThe minimum confidence of a rule. (default = " + m_minMetric + ")",
      string3 = "\tThe delta by which the minimum support is decreased in\n",
      string4 = "\teach iteration. (default = " + m_delta + ")",
      string5 = 
      "\tThe lower bound for the minimum support. (default = " + 
      m_lowerBoundMinSupport + ")",
      string6 = "\tIf used, rules are tested for significance at\n",
      string7 = "\tthe given level. Slower. (default = no significance testing)",
      string8 = "\tIf set the itemsets found are also output. (default = no)",
      stringType = "\tThe metric type by which to rank rules. (default = "
      +"confidence)";
    

    FastVector newVector = new FastVector(9);

    newVector.addElement(new Option(string1, "N", 1, 
				    "-N <required number of rules output>"));
    newVector.addElement(new Option(stringType, "T", 1,
				    "-T <0=confidence | 1=lift | "
				    +"2=leverage | 3=Conviction>"));
    newVector.addElement(new Option(string2, "C", 1, 
				    "-C <minimum metric score of a rule>"));
    newVector.addElement(new Option(string3 + string4, "D", 1,
				    "-D <delta for minimum support>"));
    newVector.addElement(new Option("\tUpper bound for minimum support. "
				    +"(default = 1.0)", "U", 1,
				     "-U <upper bound for minimum support>"));
    newVector.addElement(new Option(string5, "M", 1,
				    "-M <lower bound for minimum support>"));
    newVector.addElement(new Option(string6 + string7, "S", 1,
				    "-S <significance level>"));
    newVector.addElement(new Option(string8, "I", 0,
				    "-I"));
    newVector.addElement(new Option("\tRemove columns that contain "
				    +"all missing values (default = no)"
				    , "R", 0,
				    "-R"));
    newVector.addElement(new Option("\tReport progress iteratively. (default "
				    +"= no)", "V", 0,
				    "-V"));
    
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *   
   * -N required number of rules <br>
   * The required number of rules (default: 10). <p>
   *
   * -T type of metric by which to sort rules <br>
   * 0 = confidence | 1 = lift | 2 = leverage | 3 = Conviction. <p>
   *
   * -C minimum metric score of a rule <br>
   * The minimum confidence of a rule (default: 0.9). <p>
   *
   * -D delta for minimum support <br>
   * The delta by which the minimum support is decreased in
   * each iteration (default: 0.05).
   *
   * -U upper bound for minimum support <br>
   * The upper bound for minimum support. Don't explicitly look for 
   * rules with more than this level of support. <p>
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
   * -V <br>
   * If set then progress is reported iteratively during execution. <p>
   *
   * -R <br>
   * If set then columns that contain all missing values are removed from
   * the data. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported 
   */
  public void setOptions(String[] options) throws Exception {
    
    resetOptions();
    String numRulesString = Utils.getOption('N', options),
      minConfidenceString = Utils.getOption('C', options),
      deltaString = Utils.getOption('D', options),
      maxSupportString = Utils.getOption('U', options),
      minSupportString = Utils.getOption('M', options),
      significanceLevelString = Utils.getOption('S', options);
    String metricTypeString = Utils.getOption('T', options);
    if (metricTypeString.length() != 0) {
      setMetricType(new SelectedTag(Integer.parseInt(metricTypeString),
				    TAGS_SELECTION));
    }
    
    if (numRulesString.length() != 0) {
      m_numRules = Integer.parseInt(numRulesString);
    }
    if (minConfidenceString.length() != 0) {
      m_minMetric = (new Double(minConfidenceString)).doubleValue();
    }
    if (deltaString.length() != 0) {
      m_delta = (new Double(deltaString)).doubleValue();
    }
    if (maxSupportString.length() != 0) {
      setUpperBoundMinSupport((new Double(maxSupportString)).doubleValue());
    }
    if (minSupportString.length() != 0) {
      m_lowerBoundMinSupport = (new Double(minSupportString)).doubleValue();
    }
    if (significanceLevelString.length() != 0) {
      m_significanceLevel = (new Double(significanceLevelString)).doubleValue();
    }
    m_outputItemSets = Utils.getFlag('I', options);
    m_verbose = Utils.getFlag('V', options);
    setRemoveAllMissingCols(Utils.getFlag('R', options));
  }

  /**
   * Gets the current settings of the Apriori object.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [17];
    int current = 0;

    if (m_outputItemSets) {
      options[current++] = "-I";
    }

    if (getRemoveAllMissingCols()) {
      options[current++] = "-R";
    }

    options[current++] = "-N"; options[current++] = "" + m_numRules;
    options[current++] = "-T"; options[current++] = "" + m_metricType;
    options[current++] = "-C"; options[current++] = "" + m_minMetric;
    options[current++] = "-D"; options[current++] = "" + m_delta;
    options[current++] = "-U"; options[current++] = ""+m_upperBoundMinSupport;
    options[current++] = "-M"; options[current++] = ""+m_lowerBoundMinSupport;
    options[current++] = "-S"; options[current++] = "" + m_significanceLevel;
    if (m_verbose)
      options[current++] = "-V";

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
    text.append("Minimum support: " 
		+ Utils.doubleToString(m_minSupport,2) + '\n');
    text.append("Minimum metric <");
    switch(m_metricType) {
    case CONFIDENCE:
      text.append("confidence>: ");
      break;
    case LIFT:
      text.append("lift>: ");
      break;
    case LEVERAGE:
      text.append("leverage>: ");
      break;
    case CONVICTION:
      text.append("conviction>: ");
      break;
    }
    text.append(Utils.doubleToString(m_minMetric,2)+'\n');
   
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
	  text.append(((AprioriItemSet)((FastVector)m_Ls.elementAt(i)).elementAt(j)).
		      toString(m_instances)+"\n");
      }
    }
    text.append("\nBest rules found:\n\n");
    for (int i = 0; i < m_allTheRules[0].size(); i++) {
      text.append(Utils.doubleToString((double)i+1, 
		  (int)(Math.log(m_numRules)/Math.log(10)+1),0)+
		  ". " + ((AprioriItemSet)m_allTheRules[0].elementAt(i)).
		  toString(m_instances) 
		  + " ==> " + ((AprioriItemSet)m_allTheRules[1].elementAt(i)).
		  toString(m_instances) +"    conf:("+  
		  Utils.doubleToString(((Double)m_allTheRules[2].
					elementAt(i)).doubleValue(),2)+")");
      if (m_metricType != CONFIDENCE || m_significanceLevel != -1) {
	text.append((m_metricType == LIFT ? " <" : "")+" lift:("+  
		    Utils.doubleToString(((Double)m_allTheRules[3].
					  elementAt(i)).doubleValue(),2)
		    +")"+(m_metricType == LIFT ? ">" : ""));
	text.append((m_metricType == LEVERAGE ? " <" : "")+" lev:("+  
		    Utils.doubleToString(((Double)m_allTheRules[4].
					  elementAt(i)).doubleValue(),2)
		    +")");
	text.append(" ["+
		    (int)(((Double)m_allTheRules[4].elementAt(i))
			  .doubleValue() * (double)m_instances.numInstances())
		    +"]"+(m_metricType == LEVERAGE ? ">" : ""));
	text.append((m_metricType == CONVICTION ? " <" : "")+" conv:("+  
		    Utils.doubleToString(((Double)m_allTheRules[5].
					  elementAt(i)).doubleValue(),2)
		    +")"+(m_metricType == CONVICTION ? ">" : ""));
      }
      text.append('\n');
    }
    return text.toString();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String removeAllMissingColsTipText() {
    return "Remove columns with all missing values.";
  }

  /**
   * Remove columns containing all missing values.
   * @param r true if cols are to be removed.
   */
  public void setRemoveAllMissingCols(boolean r) {
    m_removeMissingCols = r;
  }

  /**
   * Returns whether columns containing all missing values are to be removed
   * @return true if columns are to be removed.
   */
  public boolean getRemoveAllMissingCols() {
    return m_removeMissingCols;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String upperBoundMinSupportTipText() {
    return "Upper bound for minimum support. Start iteratively decreasing "
      +"minimum support from this value.";
  }

  /**
   * Get the value of upperBoundMinSupport.
   *
   * @return Value of upperBoundMinSupport.
   */
  public double getUpperBoundMinSupport() {
    
    return m_upperBoundMinSupport;
  }
  
  /**
   * Set the value of upperBoundMinSupport.
   *
   * @param v  Value to assign to upperBoundMinSupport.
   */
  public void setUpperBoundMinSupport(double v) {
    
    m_upperBoundMinSupport = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String lowerBoundMinSupportTipText() {
    return "Lower bound for minimum support.";
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
   * Get the metric type
   *
   * @return the type of metric to use for ranking rules
   */
  public SelectedTag getMetricType() {
    return new SelectedTag(m_metricType, TAGS_SELECTION);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String metricTypeTipText() {
    return "Set the type of metric by which to rank rules. Confidence is "
      +"the proportion of the examples covered by the premise that are also "
      +"covered by the consequence. Lift is confidence divided by the "
      +"proportion of all examples that are covered by the consequence. This "
      +"is a measure of the importance of the association that is independent "
      +"of support. Leverage is the proportion of additional examples covered "
      +"by both the premise and consequence above those expected if the "
      +"premise and consequence were independent of each other. The total "
      +"number of examples that this represents is presented in brackets "
      +"following the leverage. Conviction is "
      +"another measure of departure from independence and furthermore takes into "
      +"account implicaton. Conviction is given "
      +"by P(premise)P(!consequence) / P(premise, !consequence).";
  }

  /**
   * Set the metric type for ranking rules
   *
   * @param d the type of metric
   */
  public void setMetricType (SelectedTag d) {
    
    if (d.getTags() == TAGS_SELECTION) {
      m_metricType = d.getSelectedTag().getID();
    }

    if (m_significanceLevel != -1 && m_metricType != CONFIDENCE) {
      m_metricType = CONFIDENCE;
    }

    if (m_metricType == CONFIDENCE) {
      setMinMetric(0.9);
    }

    if (m_metricType == LIFT || m_metricType == CONVICTION) {
      setMinMetric(1.1);
    }
  
    if (m_metricType == LEVERAGE) {
      setMinMetric(0.1);
    }
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minMetricTipText() {
    return "Minimum metric score. Consider only rules with scores higher than "
      +"this value.";
  }

  /**
   * Get the value of minConfidence.
   *
   * @return Value of minConfidence.
   */
  public double getMinMetric() {
    
    return m_minMetric;
  }
  
  /**
   * Set the value of minConfidence.
   *
   * @param v  Value to assign to minConfidence.
   */
  public void setMinMetric(double v) {
    
    m_minMetric = v;
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
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String deltaTipText() {
    return "Iteratively decrease support by this factor. Reduces support "
      +"until min support is reached or required number of rules has been "
      +"generated.";
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
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String significanceLevelTipText() {
    return "Significance level. Significance test (confidence metric only).";
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
   * Sets whether itemsets are output as well
   * @param flag true if itemsets are to be output as well
   */  
  public void setOutputItemSets(boolean flag){
    m_outputItemSets = flag;
  }
  
  /**
   * Gets whether itemsets are output as well
   * @return true if itemsets are output as well
   */  
  public boolean getOutputItemSets(){
    return m_outputItemSets;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String outputItemSetsTipText() {
    return "If enabled the itemsets are output as well.";
  }

  /**
   * Sets verbose mode
   * @param flag true if algorithm should be run in verbose mode
   */  
  public void setVerbose(boolean flag){
    m_verbose = flag;
  }
  
  /**
   * Gets whether algorithm is run in verbose mode
   * @return true if algorithm is run in verbose mode
   */  
  public boolean getVerbose(){
    return m_verbose;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String verboseTipText() {
    return "If enabled the algorithm will be run in verbose mode.";
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
    int necSupport, necMaxSupport,i = 0;
    
    m_instances = instances;
    
    // Find large itemsets

    // minimum support
    necSupport = (int)(m_minSupport * (double)instances.numInstances()+0.5);
    necMaxSupport = (int)(m_upperBoundMinSupport * (double)instances.numInstances()+0.5);
   
    kSets = AprioriItemSet.singletons(instances);
    AprioriItemSet.upDateCounters(kSets, instances);
    kSets = AprioriItemSet.deleteItemSets(kSets, necSupport, necMaxSupport);
    if (kSets.size() == 0)
      return;
    do {
      m_Ls.addElement(kSets);
      kMinusOneSets = kSets;
      kSets = AprioriItemSet.mergeAllItemSets(kMinusOneSets, i, instances.numInstances());
      hashtable = AprioriItemSet.getHashtable(kMinusOneSets, kMinusOneSets.size());
      m_hashtables.addElement(hashtable);
      kSets = AprioriItemSet.pruneItemSets(kSets, hashtable);
      AprioriItemSet.upDateCounters(kSets, instances);
      kSets = AprioriItemSet.deleteItemSets(kSets, necSupport, necMaxSupport);
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
	AprioriItemSet currentItemSet = (AprioriItemSet)enumItemSets.nextElement();
        //AprioriItemSet currentItemSet = new AprioriItemSet((ItemSet)enumItemSets.nextElement());
	rules=currentItemSet.generateRulesBruteForce(m_minMetric,m_metricType,
				  m_hashtables,j+1,
				  m_instances.numInstances(),
				  m_significanceLevel);
	for (int k = 0; k < rules[0].size(); k++) {
	  m_allTheRules[0].addElement(rules[0].elementAt(k));
	  m_allTheRules[1].addElement(rules[1].elementAt(k));
	  m_allTheRules[2].addElement(rules[2].elementAt(k));

	  m_allTheRules[3].addElement(rules[3].elementAt(k));
	  m_allTheRules[4].addElement(rules[4].elementAt(k));
	  m_allTheRules[5].addElement(rules[5].elementAt(k));
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
	AprioriItemSet currentItemSet = (AprioriItemSet)enumItemSets.nextElement();
        //AprioriItemSet currentItemSet = new AprioriItemSet((ItemSet)enumItemSets.nextElement());
	rules = currentItemSet.generateRules(m_minMetric, m_hashtables, j + 1);
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



