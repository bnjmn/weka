/*
 *    M5Base.java
 *    Copyright (C) 2000 Mark Hall
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
package weka.classifiers.trees.m5;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.*;
import weka.filters.*;

/**
 * M5Base. Implements base routines
 * for generating M5 Model trees and rules. <p>
 * 
 * Valid options are:<p>
 * 
 * -U <br>
 * Use unsmoothed predictions. <p>
 *
 * @version $Revision: 1.1 $
 */
public abstract class M5Base extends Classifier 
  implements OptionHandler,
	     AdditionalMeasureProducer {

  /**
   * the instances covered by the tree/rules
   */
  private Instances		     m_instances;

  /**
   * the class index
   */
  private int			     m_classIndex;

  /**
   * the number of attributes
   */
  private int			     m_numAttributes;

  /**
   * the number of instances in the dataset
   */
  private int			     m_numInstances;

  /**
   * the rule set
   */
  protected FastVector		     m_ruleSet;

  /**
   * generate a decision list instead of a single tree.
   */
  private boolean		     m_generateRules;

  /**
   * use unsmoothed predictions
   */
  private boolean		     m_unsmoothedPredictions;

  /**
   * filter to fill in missing values
   */
  private ReplaceMissingValuesFilter m_replaceMissing;

  /**
   * filter to convert nominal attributes to binary
   */
  private NominalToBinaryFilter      m_nominalToBinary;

  /**
   * Save instances at each node in an M5 tree for visualization purposes.
   */
  protected boolean m_saveInstances = false;

  /**
   * Constructor
   */
  public M5Base() {
    m_generateRules = false;
    m_unsmoothedPredictions = false;
  }

  /**
   * Returns an enumeration describing the available options
   * 
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(1);

    newVector.addElement(new Option("\tUse unsmoothed predictions\n", 
				    "U", 0, "-U"));

    return newVector.elements();
  } 

  /**
   * Parses a given list of options. <p>
   * 
   * Valid options are:<p>
   * 
   * -U <br>
   * Use unsmoothed predictions. <p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    setUseUnsmoothed(Utils.getFlag('U', options));
    
    Utils.checkForRemainingOptions(options);
  } 

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    String[] options = new String[1];
    int      current = 0;

    if (getUseUnsmoothed()) {
      options[current++] = "-U";
    } 

    while (current < options.length) {
      options[current++] = "";
    } 
    return options;
  } 

  /**
   * Generate rules (decision list) rather than a tree
   * 
   * @param u true if rules are to be generated
   */
  protected void setGenerateRules(boolean u) {
    m_generateRules = u;
  } 

  /**
   * get whether rules are being generated rather than a tree
   * 
   * @return true if rules are to be generated
   */
  protected boolean getGenerateRules() {
    return m_generateRules;
  } 

  /**
   * Use unsmoothed predictions
   * 
   * @param s true if unsmoothed predictions are to be used
   */
  public void setUseUnsmoothed(boolean s) {
    m_unsmoothedPredictions = s;
  } 

  /**
   * Get whether or not smoothing is being used
   * 
   * @return true if unsmoothed predictions are to be used
   */
  public boolean getUseUnsmoothed() {
    return m_unsmoothedPredictions;
  } 

  /**
   * Generates the classifier.
   * 
   * @param data set of instances serving as training data
   * @exception Exception if the classifier has not been generated
   * successfully
   */
  public void buildClassifier(Instances data) throws Exception {
    if (data.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    } 

    m_instances = new Instances(data);
    m_replaceMissing = new ReplaceMissingValuesFilter();

    m_instances.deleteWithMissingClass();
    m_replaceMissing.setInputFormat(m_instances);

    m_instances = Filter.useFilter(m_instances, m_replaceMissing);
    m_nominalToBinary = new NominalToBinaryFilter();

    m_nominalToBinary.setInputFormat(m_instances);

    m_instances = Filter.useFilter(m_instances, m_nominalToBinary);

    // 
    m_instances.randomize(new Random(1));

    m_classIndex = m_instances.classIndex();
    m_numAttributes = m_instances.numAttributes();
    m_numInstances = m_instances.numInstances();
    m_ruleSet = new FastVector();

    Rule tempRule;

    if (m_generateRules) {
      Instances tempInst = m_instances;
      double sum = 0;
      double temp_sum = 0;
     
      do {
	tempRule = new Rule();
	tempRule.setSmoothing(!m_unsmoothedPredictions);

	tempRule.buildClassifier(tempInst);
	m_ruleSet.addElement(tempRule);
	//	System.err.println("Built rule : "+tempRule.toString());
	tempInst = tempRule.notCoveredInstances();
      } while (tempInst.numInstances() > 0);
    } else {
      // just build a single tree
      tempRule = new Rule();

      tempRule.setUseTree(true);
      tempRule.setGrowFullTree(true);
      tempRule.setSmoothing(!m_unsmoothedPredictions);
      tempRule.setSaveInstances(m_saveInstances);

      Instances temp_train;

      temp_train = m_instances;

      tempRule.buildClassifier(temp_train);

      m_ruleSet.addElement(tempRule);      

      // save space
      m_instances = new Instances(m_instances, 0);
      //      System.err.print(tempRule.m_topOfTree.treeToString(0));
    } 
  } 

  /**
   * Calculates a prediction for an instance using a set of rules
   * or an M5 model tree
   * 
   * @param inst the instance whos class value is to be predicted
   * @return the prediction
   * @exception if a prediction can't be made.
   */
  public double classifyInstance(Instance inst) throws Exception {
    Rule   temp;
    double prediction = 0;
    boolean success = false;

    m_replaceMissing.input(inst);
    inst = m_replaceMissing.output();
    m_nominalToBinary.input(inst);
    inst = m_nominalToBinary.output();

    if (m_ruleSet == null) {
      throw new Exception("Classifier has not been built yet!");
    } 

    if (!m_generateRules) {
      temp = (Rule) m_ruleSet.elementAt(0);
      return temp.classifyInstance(inst);
    } 

    boolean cont;
    int     i;

    for (i = 0; i < m_ruleSet.size(); i++) {
      cont = false;
      temp = (Rule) m_ruleSet.elementAt(i);

      try {
	prediction = temp.classifyInstance(inst);
	success = true;
      } catch (Exception e) {
	cont = true;
      } 

      if (!cont) {
	break;
      } 
    } 

    if (!success) {
      System.out.println("Error in predicting (DecList)");
    } 
    return prediction;
  } 

  /**
   * Returns a description of the classifier
   * 
   * @return a description of the classifier as a String
   */
  public String toString() {
    StringBuffer text = new StringBuffer();
    Rule	 temp;

    if (m_ruleSet == null) {
      return "Classifier hasn't been built yet!";
    } 

    if (m_generateRules) {
      text.append("M5 Rules  ");

      if (!m_unsmoothedPredictions) {
	text.append("(smoothed) ");
      }

      text.append(":\n");

      text.append("Number of Rules : " + m_ruleSet.size() + "\n\n");

      for (int j = 0; j < m_ruleSet.size(); j++) {
	temp = (Rule) m_ruleSet.elementAt(j);

	text.append("Rule: " + (j + 1) + "\n");
	text.append(temp.toString());
      } 
    } else {
      temp = (Rule) m_ruleSet.elementAt(0);
      text.append(temp.toString());
    } 
    return text.toString();
  } 

  /**
   * Returns an enumeration of the additional measure names
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    Vector newVector = new Vector(1);
    newVector.addElement("measureNumRules");
    return newVector.elements();
  }

  /**
   * Returns the value of the named measure
   * @param measureName the name of the measure to query for its value
   * @return the value of the named measure
   * @exception Exception if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) 
    throws IllegalArgumentException {
    if (additionalMeasureName.compareTo("measureNumRules") == 0) {
      return measureNumRules();
    } else {
      throw new IllegalArgumentException(additionalMeasureName 
					 + " not supported (M5)");
    }
  }

  /**
   * return the number of rules
   * @return the number of rules (same as # linear models &
   * # leaves in the tree)
   */
  public double measureNumRules() {
    if (m_generateRules) {
      return m_ruleSet.size();
    }
    return ((Rule)m_ruleSet.elementAt(0)).m_topOfTree.numberOfLinearModels();
  }
}




