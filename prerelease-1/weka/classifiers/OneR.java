/*
 *    OneR.java
 *    Copyright (C) 1999 Ian H. Witten
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


package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;


/**
 * Class for building and using a 1R classifier.
 * 
 * @author Ian H. Witten (ihw@cs.waikato.ac.nz)
 * @version 1.0
 */
public class OneR extends Classifier implements OptionHandler {

  /**
   * Class for storing store a 1R rule.
   */
  private class OneRRule {
  
    private Instances instances;
    private Attribute attr; // attribute to test
    private int correct; // training set examples this rule gets right
    private int[] classifications; // predicted class for each value of attr
    private int missingValueClass = -1; // predicted class for missing values
    private double[] breakpoints; // breakpoints (numeric attributes only)
  
    /**
     * Constructor for nominal attribute.
     */
    public OneRRule(Instances data, Attribute attribute) {

      instances = data;
      attr = attribute;
      correct = 0;
      classifications = new int[attr.numValues()];
    }

    /**
     * Constructor for numeric attribute.
     */
    public OneRRule(Instances data, Attribute attribute, int nBreaks) {

      instances = data;
      attr = attribute;
      correct = 0;
      classifications = new int[nBreaks];
      breakpoints = new double[nBreaks - 1]; // last breakpoint is infinity
    }
    
    /**
     * Returns a description of the rule.
     */
    public String toString() {

      try {
	StringBuffer text = new StringBuffer();
	text.append(attr.name() + ":\n");
	for (int v = 0; v < classifications.length; v++) {
	  text.append("\t");
	  if (attr.isNominal()) {
	    text.append(attr.value(v));
	  } else if (v < breakpoints.length) {
	    text.append("< " + breakpoints[v]);
	  } else {
	    text.append(">= " + breakpoints[v - 1]);
	  }
	  text.append("\t-> " + 
		      instances.classAttribute().value(classifications[v]) 
		      + "\n");
	}
	if (missingValueClass != -1) {
	  text.append("\t?\t-> " + 
		      instances.classAttribute().value(missingValueClass) 
		      + "\n");
	}
	text.append("(" + correct + "/" + instances.numInstances() +
		    " instances correct)\n");
	return text.toString();
      } catch (Exception e) {
	return "Can't print OneR classifier!";
      }
    }
  }
  
  private OneRRule rule;
  private int minBucketSize = 6;

  /**
   * Classifies a given instance.
   *
   * @param inst the instance to be classified
   */
  public double classifyInstance(Instance inst) {

    int v = 0;
    if (inst.isMissing(rule.attr)) {
      if (rule.missingValueClass != -1) {
	return rule.missingValueClass;
      } else {
	return 0;  // missing values occur in test but not training set    
      }
    }
    if (rule.attr.isNominal()) {
      v = (int) inst.value(rule.attr);
    } else {
      while (v < rule.breakpoints.length &&
	     inst.value(rule.attr) >= rule.breakpoints[v]) {
	v++;
      }
    }
    return rule.classifications[v];
  }

  /**
   * Generates the classifier.
   *
   * @param instances the instances to be used for building the classifier
   * @exception Exception if the classifier can't be built successfully
   */
  public void buildClassifier(Instances instances) 
    throws Exception {
    
    boolean noRule = true;

    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    if (instances.classAttribute().isNumeric()) {
      throw new Exception("Can't handle numeric class!");
    }

    Instances data = new Instances(instances);
    // new dataset without missing class values
    data.deleteWithMissingClass();
    if (data.numInstances() == 0) {
      throw new Exception("No instances with a class value!");
    }

    // for each attribute ...
    Enumeration enum = instances.enumerateAttributes();
    while (enum.hasMoreElements()) {
      OneRRule r = newRule((Attribute) enum.nextElement(), data);

      // if this attribute is the best so far, replace the rule
      if (noRule || r.correct > rule.correct) {
	rule = r;
      }
      noRule = false;
    }
  }

  /**
   * Create a rule branching on this attribute.
   *
   * @param attr the attribute to branch on
   * @param data the data to be used for creating the rule
   * @exception Exception if the rule can't be built successfully
   */
  public OneRRule newRule(Attribute attr, Instances data) throws Exception {

    OneRRule r;

    // ... create array to hold the missing value counts
    int[] missingValueCounts =
      new int [data.classAttribute().numValues()];
    
    if (attr.isNominal()) {
      r = newNominalRule(attr, data, missingValueCounts);
    } else {
      r = newNumericRule(attr, data, missingValueCounts);
    }
    r.missingValueClass = Utils.maxIndex(missingValueCounts);
    if (missingValueCounts[r.missingValueClass] == 0) {
      r.missingValueClass = -1; // signal for no missing value class
    } else {
      r.correct += missingValueCounts[r.missingValueClass];
    }
    return r;
  }

  /**
   * Create a rule branching on this nominal attribute.
   *
   * @param attr the attribute to branch on
   * @param data the data to be used for creating the rule
   * @param missingValueCounts to be filled in
   * @exception Exception if the rule can't be built successfully
   */
  public OneRRule newNominalRule(Attribute attr, Instances data,
                                 int[] missingValueCounts) throws Exception {

    // ... create arrays to hold the counts
    int[][] counts = new int [attr.numValues()]
                             [data.classAttribute().numValues()];
      
    // ... calculate the counts
    Enumeration enum = data.enumerateInstances();
    while (enum.hasMoreElements()) {
      Instance i = (Instance) enum.nextElement();
      if (i.isMissing(attr)) {
	missingValueCounts[(int) i.classValue()]++; 
      } else {
	counts[(int) i.value(attr)][(int) i.classValue()]++;
      }
    }

    OneRRule r = new OneRRule(data, attr); // create a new rule
    for (int value = 0; value < attr.numValues(); value++) {
      int best = Utils.maxIndex(counts[value]);
      r.classifications[value] = best;
      r.correct += counts[value][best];
    }
    return r;
  }

  /**
   * Create a rule branching on this numeric attribute
   *
   * @param attr the attribute to branch on
   * @param data the data to be used for creating the rule
   * @param missingValueCounts to be filled in
   * @exception Exception if the rule can't be built successfully
   */
  public OneRRule newNumericRule(Attribute attr, Instances data,
                             int[] missingValueCounts) throws Exception {

    data.sort(attr);
    // ... can't be more than numInstances buckets
    int [] classifications = new int[data.numInstances()];
    double [] breakpoints = new double[data.numInstances()];

    // create array to hold the counts
    int [] counts = new int[data.classAttribute().numValues()];
    int correct = 0;
    int lastInstance = data.numInstances();
    // missing values get sorted to the end of the instances
    while (lastInstance > 0 && 
           data.instance(lastInstance-1).isMissing(attr)) {
      lastInstance--;
      missingValueCounts[(int) data.instance(lastInstance).
                         classValue()]++; 
    }
    int i = 0; 
    int cl = 0; // index of next bucket to create
    int it;
    while (i < lastInstance) { // start a new bucket
      for (int j = 0; j < counts.length; j++) counts[j] = 0;
      do { // fill it until it has enough of the majority class
        it = (int) data.instance(i++).classValue();
        counts[it]++;
      } while (counts[it] < minBucketSize && i < lastInstance);
      // while class remains the same, keep on filling
      while (i < lastInstance && 
             (int) data.instance(i).classValue() == it) { 
        counts[it]++; 
        i++;
      }
      while (i < lastInstance && // keep on while attr value is the same
             (data.instance(i - 1).value(attr) 
	      == data.instance(i).value(attr))) {
        counts[(int) data.instance(i++).classValue()]++;
      }
      for (int j = 0; j < counts.length; j++) {
        if (counts[j] > counts[it]) { 
	  it = j;
	}
      }
      if (cl > 0) { // can we coalesce with previous class?
        if (counts[classifications[cl - 1]] == counts[it]) {
          it = classifications[cl - 1];
	}
        if (it == classifications[cl - 1]) {
	  cl--; // yes!
	}
      }
      correct += counts[it];
      classifications[cl] = it;
      if (i < lastInstance) {
        breakpoints[cl] = (data.instance(i - 1).value(attr)
			   + data.instance(i).value(attr)) / 2;
      }
      cl++;
    }
    if (cl == 0) {
      throw new Exception("Only missing values in the training data!");
    }
    OneRRule r = new OneRRule(data, attr, cl); // new rule with cl branches
    r.correct = correct;
    for (int v = 0; v < cl; v++) {
      r.classifications[v] = classifications[v];
      if (v < cl-1) {
	r.breakpoints[v] = breakpoints[v];
      }
    }

    return r;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    String string = "\tThe minimum number of objects in a bucket.";

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(string, "B", 1, 
				    "-B <minimum bucket size>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -B num <br>
   * Specify the minumum number of objects in a bucket <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String bucketSizeString = Utils.getOption('B', options);
    if (bucketSizeString.length() != 0) {
      minBucketSize = Integer.parseInt(bucketSizeString);
    } else {
      minBucketSize = 6;
    }
  }

  /**
   * Gets the current settings of the OneR classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [2];
    int current = 0;

    options[current++] = "-B"; options[current++] = "" + minBucketSize;

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns a description of the classifier
   */
  public String toString() {

    return rule.toString();
  }

  /**
   * Main method for testing this class
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new OneR(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}











