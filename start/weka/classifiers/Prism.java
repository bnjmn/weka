/*
 *    Prism.java
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
 * Class for building and using a PRISM classifier
 * 
 * @author Ian H. Witten (ihw@cs.waikato.ac.nz)
 * @version 1.0
 */
public class Prism extends Classifier {

  /**
   * Class for storing a PRISM ruleset, i.e. a list of rules
   */
  private class PrismRule {
    private int classification;
    private Instances instances;
    private Test test; // first test of this rule
    private int errors; // number of errors made by this rule (will end up 0)
    private PrismRule next;

    /**
     * Constructor that takes instances and the classification.
     *
     * @param data the instances
     * @param cl the class
     * @exception Exception if something goes wrong
     */
    public PrismRule(Instances data, int cl) throws Exception {

      instances = data;
      classification = cl;
      test = null;
      next = null;
      errors = 0;
      Enumeration enum = data.enumerateInstances();
      while (enum.hasMoreElements()) {
        if ((int) ((Instance) enum.nextElement()).classValue() != cl) {
	  errors++;
	}
      }
    }  

    /**
     * Returns the result assigned by this rule to a given instance.
     *
     * @param inst the instance to be classified
     * @return the classification
     */
    public int resultRule(Instance inst) {

      if (test == null || test.satisfies(inst)) {
	return classification;
      } else {
	return -1;
      }
    }

    /**
     * Returns the result assigned by these rules to a given instance.
     *
     * @param inst the instance to be classified
     * @return the classification
     */
    public int resultRules(Instance inst) {

      if (resultRule(inst) != -1) {
	return classification;
      } else if (next != null) {
	return next.resultRules(inst);
      } else {
	return -1;
      }
    }

    /**
     * Returns the set of instances that are covered by this rule.
     *
     * @param data the instances to be checked
     * @return the instances covered
     */
    public Instances coveredBy(Instances data) {

      Instances r = new Instances(data, data.numInstances());
      Enumeration enum = data.enumerateInstances();
      while (enum.hasMoreElements()) {
	Instance i = (Instance) enum.nextElement();
	if (resultRule(i) != -1) {
	  r.add(i);
	}
      }
      r.compactify();
      return r;
    }

    /**
     * Returns the set of instances that are not covered by this rule.
     *
     * @param data the instances to be checked
     * @return the instances not covered
     */
    public Instances notCoveredBy(Instances data) {

      Instances r = new Instances(data, data.numInstances());
      Enumeration enum = data.enumerateInstances();
      while (enum.hasMoreElements()) {
	Instance i = (Instance) enum.nextElement();
	if (resultRule(i) == -1) {
	  r.add(i);
	}
      }
      r.compactify();
      return r;
    }

    /**
     * Prints the set of rules.
     *
     * @return a description of the rules as a string
     */
    public String toString() {

      try {
	StringBuffer text = new StringBuffer();
	if (test != null) {
	  text.append("If ");
	  for (Test t = test; t != null; t = t.next) {
	    if (t.attr == -1) {
	      text.append("?");
	    } else {
	      text.append(instances.attribute(t.attr).name() + " = " +
			  instances.attribute(t.attr).value(t.val));
	    }
	    if (t.next != null) {
	      text.append("\n   and ");
	    }
	  }
	  text.append(" then ");
	}
	text.append(instances.classAttribute().value(classification) + "\n");
	if (next != null) {
	  text.append(next.toString());
	}
	return text.toString();
      } catch (Exception e) {
	return "Can't print Prism classifier!";
      }
    }
  }
  
  /**
   * Class for storing a list of attribute-value tests
   */
  private class Test { 

    private int attr = -1; // attribute to test
    private int val; // value of that attribute
    private Test next = null; // next test

    /**
     * Returns whether a given instance satisfies this test.
     *
     * @param inst the instance to be tested
     * @return true if the instance satisfies the test
     */
    private boolean satisfies(Instance inst) {

      if ((int) inst.value(attr) == val) {
        if (next == null) {
	  return true;
	} else {
	  return next.satisfies(inst);
	}
      }
      return false;    
    }
  }

  private PrismRule rules;

  /**
   * Classifies a given instance.
   *
   * @param inst the instance to be classified
   * @return the classification
   */
  public double classifyInstance(Instance inst) {

    int result = rules.resultRules(inst);
    if (result == -1) {
      return Instance.missingValue();
    } else {
      return (double)result;
    }
  }

  /**
   * Generates the classifier.
   *
   * @param data the data to be used
   * @exception Exception if the classifier can't built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    int cl; // possible value of theClass
    Instances E, ruleE;
    PrismRule rule = null;
    Test test = null;
    int bestCorrect, bestCovers, attUsed;

    if (data.classAttribute().isNumeric()) {
      throw new Exception("Prism can't handle a numeric class!");
    }
    Enumeration enumAtt = data.enumerateAttributes();
    while (enumAtt.hasMoreElements()) {
      Attribute attr = (Attribute) enumAtt.nextElement();
      if (attr.isNumeric()) {
	throw new Exception("Prism can't handle numeric attributes!");
      }
      Enumeration enum = data.enumerateInstances();
      while (enum.hasMoreElements()) {
	if (((Instance) enum.nextElement()).isMissing(attr)) {
	  throw new 
	    Exception("Prism can't handle attributes with missing values!");
	}
      }
    }
    data.deleteWithMissingClass(); // delete all instances with a missing class
    if (data.numInstances() == 0) {
      throw new Exception("No instances with a class value!");
    }
    for (cl = 0; cl < data.numClasses(); cl++) { // for each class cl
      E = data; // initialize E to the instance set
      while (contains(E, cl)) { // while E contains examples in class cl
        rule = addRule(rule, new PrismRule(E, cl)); // make a new rule
        ruleE = E; // examples covered by this rule
        while (rule.errors != 0) { // until the rule is perfect
          test = addTest(rule, test, new Test()); // make a new test
          bestCorrect = bestCovers = attUsed = 0;
          // for every attribute not mentioned in the rule
          enumAtt = ruleE.enumerateAttributes();
          while (enumAtt.hasMoreElements()) {
            Attribute attr = (Attribute) enumAtt.nextElement();
            if (isMentionedIn(attr, rule.test)) {
	      attUsed++; 
	      continue;
	    }
            int M = attr.numValues();
            int[] covers = new int [M];
            int[] correct = new int [M];
            for (int j = 0; j < M; j++) {
	      covers[j] = correct[j] = 0;
	    }

            // ... calculate the counts for this class
            Enumeration enum = ruleE.enumerateInstances();
            while (enum.hasMoreElements()) {
              Instance i = (Instance) enum.nextElement();
              covers[(int) i.value(attr)]++;
              if ((int) i.classValue() == cl) {
                correct[(int) i.value(attr)]++;
	      }
            }

            // ... for each value of this attribute, see if this test is better
            for (int val = 0; val < M; val ++) {
              int diff = correct[val] * bestCovers - bestCorrect * covers[val];
              // this is a ratio test, correct/covers vs best correct/covers
              if (test.attr == -1
                  || diff > 0 || (diff == 0 && correct[val] > bestCorrect)) {
                // update the rule to use this test
                bestCorrect = correct[val];
                bestCovers = covers[val];
                test.attr = attr.index();
                test.val = val;
                rule.errors = bestCovers - bestCorrect;
              }
            }
          }
          ruleE = rule.coveredBy(ruleE);
	  if (attUsed == (data.numAttributes() - 1)) { // Used all attributes.
	    break;
	  }
        }
        E = rule.notCoveredBy(E);
      }
    }
  }

  /**
   * Add a rule to the ruleset.
   *
   * @param lastRule the last rule in the rule set
   * @param newRule the rule to be added
   * @return the new last rule in the rule set
   */
  private PrismRule addRule(PrismRule lastRule, PrismRule newRule) {

    if (lastRule == null) {
      rules = newRule;
    } else {
      lastRule.next = newRule;
    }
    return newRule;
  }

  /**
   * Add a test to this rule.
   *
   * @param rule the rule to which test is to be added
   * @param lastTest the rule's last test
   * @param newTest the test to be added
   * @return the new last test of the rule
   */
  private Test addTest(PrismRule rule, Test lastTest, Test newTest) {

    if (rule.test == null) {
      rule.test = newTest;
    } else {
      lastTest.next = newTest;
    }
    return newTest;
  }

  /**
   * Does E contain any examples in the class C?
   *
   * @param E the instances to be checked
   * @param C the class
   * @return true if there are any instances of class C
   */
  private static boolean contains(Instances E, int C) throws Exception {

    Enumeration enum = E.enumerateInstances();
    while (enum.hasMoreElements()) {
      if ((int) ((Instance) enum.nextElement()).classValue() == C) {
	return true;
      }
    }
    return false;
  }

  /**
   * Is this attribute mentioned in the rule?
   *
   * @param attr the attribute to be checked for
   * @param t test contained by rule
   */
  private static boolean isMentionedIn(Attribute attr, Test t) {

    if (t == null) { 
      return false;
    }
    if (t.attr == attr.index()) {
      return true;
    }
    return isMentionedIn(attr, t.next);
  }    

  /**
   * Prints a description of the classifier.
   *
   * @return a description of the classifier as a string
   */
  public String toString() {

    return "Prism rules\n----------\n" + rules.toString();
  }

  /**
   * Main method for testing this class
   */
  public static void main(String[] args) {

    try {
      System.out.println(Evaluation.evaluateModel(new Prism(), args));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}



