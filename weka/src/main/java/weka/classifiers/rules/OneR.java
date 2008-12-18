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
 *    OneR.java
 *    Copyright (C) 1999 Ian H. Witten
 *
 */

package weka.classifiers.rules;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for building and using a 1R classifier. For more information, see<p>
 *
 * R.C. Holte (1993). <i>Very simple classification rules
 * perform well on most commonly used datasets</i>. Machine Learning,
 * Vol. 11, pp. 63-91.<p>
 *
 * Valid options are:<p>
 *
 * -B num <br>
 * Specify the minimum number of objects in a bucket (default: 6). <p>
 * 
 * @author Ian H. Witten (ihw@cs.waikato.ac.nz)
 * @version $Revision: 1.17.2.1 $ 
*/
public class OneR extends Classifier implements OptionHandler {
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Class for building and using a 1R classifier; in other words, uses "
      + "the minimum-error attribute for prediction, discretizing numeric "
      + "attributes. For more information, see\n\n:"
      + "R.C. Holte (1993). \"Very simple classification rules "
      + "perform well on most commonly used datasets\". Machine Learning, "
      + "Vol. 11, pp. 63-91.";
  }

  /**
   * Class for storing store a 1R rule.
   */
  private class OneRRule implements Serializable {

    /** The class attribute. */
    private Attribute m_class;

    /** The number of instances used for building the rule. */
    private int m_numInst;

    /** Attribute to test */
    private Attribute m_attr; 

    /** Training set examples this rule gets right */
    private int m_correct; 

    /** Predicted class for each value of attr */
    private int[] m_classifications; 

    /** Predicted class for missing values */
    private int m_missingValueClass = -1; 

    /** Breakpoints (numeric attributes only) */
    private double[] m_breakpoints; 
  
    /**
     * Constructor for nominal attribute.
     */
    public OneRRule(Instances data, Attribute attribute) throws Exception {

      m_class = data.classAttribute();
      m_numInst = data.numInstances();
      m_attr = attribute;
      m_correct = 0;
      m_classifications = new int[m_attr.numValues()];
    }

    /**
     * Constructor for numeric attribute.
     */
    public OneRRule(Instances data, Attribute attribute, int nBreaks) throws Exception {

      m_class = data.classAttribute();
      m_numInst = data.numInstances();
      m_attr = attribute;
      m_correct = 0;
      m_classifications = new int[nBreaks];
      m_breakpoints = new double[nBreaks - 1]; // last breakpoint is infinity
    }
    
    /**
     * Returns a description of the rule.
     */
    public String toString() {

      try {
	StringBuffer text = new StringBuffer();
	text.append(m_attr.name() + ":\n");
	for (int v = 0; v < m_classifications.length; v++) {
	  text.append("\t");
	  if (m_attr.isNominal()) {
	    text.append(m_attr.value(v));
	  } else if (v < m_breakpoints.length) {
	    text.append("< " + m_breakpoints[v]);
	  } else if (v > 0) {
	    text.append(">= " + m_breakpoints[v - 1]);
	  } else {
	    text.append("not ?");
	  }
	  text.append("\t-> " + m_class.value(m_classifications[v]) + "\n");
	}
	if (m_missingValueClass != -1) {
	  text.append("\t?\t-> " + m_class.value(m_missingValueClass) + "\n");
	}
	text.append("(" + m_correct + "/" + m_numInst + " instances correct)\n");
	return text.toString();
      } catch (Exception e) {
	return "Can't print OneR classifier!";
      }
    }
  }
  
  /** A 1-R rule */
  private OneRRule m_rule;

  /** The minimum bucket size */
  private int m_minBucketSize = 6;

  /** a ZeroR model in case no model can be built from the data */
  private Classifier m_ZeroR;
    
  /**
   * Classifies a given instance.
   *
   * @param inst the instance to be classified
   */
  public double classifyInstance(Instance inst) throws Exception {

    // default model?
    if (m_ZeroR != null) {
      return m_ZeroR.classifyInstance(inst);
    }
    
    int v = 0;
    if (inst.isMissing(m_rule.m_attr)) {
      if (m_rule.m_missingValueClass != -1) {
	return m_rule.m_missingValueClass;
      } else {
	return 0;  // missing values occur in test but not training set    
      }
    }
    if (m_rule.m_attr.isNominal()) {
      v = (int) inst.value(m_rule.m_attr);
    } else {
      while (v < m_rule.m_breakpoints.length &&
	     inst.value(m_rule.m_attr) >= m_rule.m_breakpoints[v]) {
	v++;
      }
    }
    return m_rule.m_classifications[v];
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
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }
    if (instances.classAttribute().isNumeric()) {
      throw new UnsupportedClassTypeException("Can't handle numeric class!");
    }

    Instances data = new Instances(instances);

    // new dataset without missing class values
    data.deleteWithMissingClass();
    if (data.numInstances() == 0) {
      throw new Exception("No instances with a class value!");
    }

    // only class? -> build ZeroR model
    if (data.numAttributes() == 1) {
      System.err.println(
	  "Cannot build model (only class attribute present in data!), "
          + "using ZeroR model instead!");
      m_ZeroR = new weka.classifiers.rules.ZeroR();
      m_ZeroR.buildClassifier(data);
      return;
    }
    else {
      m_ZeroR = null;
    }
    
    // for each attribute ...
    Enumeration enu = instances.enumerateAttributes();
    while (enu.hasMoreElements()) {
      try {
	OneRRule r = newRule((Attribute) enu.nextElement(), data);

	// if this attribute is the best so far, replace the rule
	if (noRule || r.m_correct > m_rule.m_correct) {
	  m_rule = r;
	}
	noRule = false;
      } catch (Exception ex) {
      }
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
    r.m_missingValueClass = Utils.maxIndex(missingValueCounts);
    if (missingValueCounts[r.m_missingValueClass] == 0) {
      r.m_missingValueClass = -1; // signal for no missing value class
    } else {
      r.m_correct += missingValueCounts[r.m_missingValueClass];
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
    Enumeration enu = data.enumerateInstances();
    while (enu.hasMoreElements()) {
      Instance i = (Instance) enu.nextElement();
      if (i.isMissing(attr)) {
	missingValueCounts[(int) i.classValue()]++; 
      } else {
	counts[(int) i.value(attr)][(int) i.classValue()]++;
      }
    }

    OneRRule r = new OneRRule(data, attr); // create a new rule
    for (int value = 0; value < attr.numValues(); value++) {
      int best = Utils.maxIndex(counts[value]);
      r.m_classifications[value] = best;
      r.m_correct += counts[value][best];
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


    // ... can't be more than numInstances buckets
    int [] classifications = new int[data.numInstances()];
    double [] breakpoints = new double[data.numInstances()];

    // create array to hold the counts
    int [] counts = new int[data.classAttribute().numValues()];
    int correct = 0;
    int lastInstance = data.numInstances();

    // missing values get sorted to the end of the instances
    data.sort(attr);
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
      } while (counts[it] < m_minBucketSize && i < lastInstance);

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
    r.m_correct = correct;
    for (int v = 0; v < cl; v++) {
      r.m_classifications[v] = classifications[v];
      if (v < cl-1) {
	r.m_breakpoints[v] = breakpoints[v];
      }
    }

    return r;
  }

  /**
   * Returns an enumeration describing the available options..
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    String string = "\tThe minimum number of objects in a bucket (default: 6).";

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(string, "B", 1, 
				    "-B <minimum bucket size>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -B num <br>
   * Specify the minimum number of objects in a bucket (default: 6). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String bucketSizeString = Utils.getOption('B', options);
    if (bucketSizeString.length() != 0) {
      m_minBucketSize = Integer.parseInt(bucketSizeString);
    } else {
      m_minBucketSize = 6;
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

    options[current++] = "-B"; options[current++] = "" + m_minBucketSize;

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns a description of the classifier
   */
  public String toString() {

    // only ZeroR model?
    if (m_ZeroR != null) {
      StringBuffer buf = new StringBuffer();
      buf.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
      buf.append(this.getClass().getName().replaceAll(".*\\.", "").replaceAll(".", "=") + "\n\n");
      buf.append("Warning: No model could be built, hence ZeroR model is used:\n\n");
      buf.append(m_ZeroR.toString());
      return buf.toString();
    }
    
    if (m_rule == null) {
      return "OneR: No model built yet.";
    }
    return m_rule.toString();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minBucketSizeTipText() {
    return "The minimum bucket size used for discretizing numeric "
      + "attributes.";
  }
  
  /**
   * Get the value of minBucketSize.
   * @return Value of minBucketSize.
   */
  public int getMinBucketSize() {
    
    return m_minBucketSize;
  }
  
  /**
   * Set the value of minBucketSize.
   * @param v  Value to assign to minBucketSize.
   */
  public void setMinBucketSize(int v) {
    
    m_minBucketSize = v;
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











