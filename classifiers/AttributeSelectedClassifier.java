/*
 *    AttributeSelectedClassifier.java
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
package weka.classifiers;

import java.io.*;
import java.util.*;
import java.beans.MethodDescriptor;
import java.beans.IntrospectionException;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import weka.core.*;
import weka.attributeSelection.*;

/**
 * Class for running an arbitrary classifier on data that has been reduced
 * through attribute selection. <p>
 *
 * Valid options from the command line are:<p>
 *
 * -B classifierstring <br>
 * Classifierstring should contain the full class name of a classifier
 * followed by options to the classifier.
 * (required).<p>
 *
 * -E evaluatorstring <br>
 * Evaluatorstring should contain the full class name of an attribute
 * evaluator followed by any options.
 * (required).<p>
 *
 * -S searchstring <br>
 * Searchstring should contain the full class name of a search method
 * followed by any options.
 * (required). <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision 1.0 $
 */
public class AttributeSelectedClassifier extends DistributionClassifier 
implements OptionHandler {

  /** The classifier */
  protected Classifier m_Classifier = new weka.classifiers.ZeroR();

  /** The attribute selection object */
  protected AttributeSelection m_AttributeSelection = null;

  /** The attribute evaluator to use */
  protected ASEvaluation m_Evaluator = 
    new weka.attributeSelection.CfsSubsetEval();

  /** The search method to use */
  protected ASSearch m_Search = new weka.attributeSelection.BestFirst();

  /** The header of the dimensionally reduced data */
  protected Instances m_ReducedHeader;

  /** The number of class vals in the training data (1 if class is numeric) */
  protected int m_numClasses;

  /** The time taken to select attributes in milliseconds */
  protected double m_selectionTime;

  /** The time taken to select attributes AND build the classifier */
  protected double m_totalTime;
  
  private final int MAX_MEASURES = 4;
  /** The values from the classifier's additionalMeasures (if any) */
  protected double [] m_additionalMeasures;

  /**
   * Returns a string describing this search method
   * @return a description of the search method suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Dimensionality of training and test data is reduced by "
      +"attribute selection before being passed on to a classifier.";
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
     Vector newVector = new Vector(3);

    newVector.addElement(new Option(
	      "\tFull class name of classifier to use, followed\n"
	      + "\tby scheme options. (required)\n"
	      + "\teg: \"weka.classifiers.NaiveBayes -D\"",
	      "B", 1, "-B <classifier specification>"));
    
    newVector.addElement(new Option(
	      "\tFull class name of attribute evaluator, followed\n"
	      + "\tby its options. (required)\n"
	      + "\teg: \"weka.attributeSelection.CfsSubsetEval -L\"",
	      "E", 1, "-E <attribute evaluator specification>"));

    newVector.addElement(new Option(
	      "\tFull class name of search method, followed\n"
	      + "\tby its options. (required)\n"
	      + "\teg: \"weka.attributeSelection.BestFirst -D 1\"",
	      "S", 1, "-S <attribute evaluator specification>"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -B classifierstring <br>
   * Classifierstring should contain the full class name of a classifier
   * followed by options to the classifier.
   * (required).<p>
   *
   * -E evaluatorstring <br>
   * Evaluatorstring should contain the full class name of an attribute
   * evaluator followed by any options.
   * (required).<p>
   *
   * -S searchstring <br>
   * Searchstring should contain the full class name of a search method
   * followed by any options.
   * (required). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String classifierString = Utils.getOption('B', options);
    if (classifierString.length() == 0) {
      throw new Exception("A classifier must be specified"
			  + " with the -B option.");
    }
    String [] classifierSpec = Utils.splitOptions(classifierString);
    if (classifierSpec.length == 0) {
      throw new Exception("Invalid classifier specification string");
    }
    String classifierName = classifierSpec[0];
    classifierSpec[0] = "";
    setClassifier(Classifier.forName(classifierName, classifierSpec));

    // same for attribute evaluator
     String evaluatorString = Utils.getOption('E', options);
    if (evaluatorString.length() == 0) {
      throw new Exception("An attribute evaluator must be specified"
			  + " with the -E option.");
    }
    String [] evaluatorSpec = Utils.splitOptions(evaluatorString);
    if (evaluatorSpec.length == 0) {
      throw new Exception("Invalid attribute evaluator specification string");
    }
    String evaluatorName = evaluatorSpec[0];
    evaluatorSpec[0] = "";
    setEvaluator(ASEvaluation.forName(evaluatorName, evaluatorSpec));

    // same for search method
    String searchString = Utils.getOption('S', options);
    if (searchString.length() == 0) {
      throw new Exception("A search method must be specified"
			  + " with the -S option.");
    }
    String [] searchSpec = Utils.splitOptions(searchString);
    if (searchSpec.length == 0) {
      throw new Exception("Invalid search specification string");
    }
    String searchName = searchSpec[0];
    searchSpec[0] = "";
    setSearch(ASSearch.forName(searchName, searchSpec));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [6];
    int current = 0;

    options[current++] = "-B";
    options[current++] = "" + getClassifierSpec();

    // same attribute evaluator
    options[current++] = "-E";
    options[current++] = "" +getEvaluatorSpec();
    
    // same for search
    options[current++] = "-S";
    options[current++] = "" + getSearchSpec();

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classifierTipText() {
    return "Set the classifier to use";
  }

  /**
   * Sets the classifier
   *
   * @param classifier the classifier with all options set.
   */
  public void setClassifier(Classifier classifier) {

    m_Classifier = classifier;
  }

  /**
   * Gets the classifier used.
   *
   * @return the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }

  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @return the classifier string.
   */
  protected String getClassifierSpec() {
    
    Classifier c = getClassifier();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String evaluatorTipText() {
    return "Set the attribute evaluator to use. This evaluator is used "
      +"during the attribute selection phase before the classifier is "
      +"invoked.";
  }

  /**
   * Sets the attribute evaluator
   *
   * @param evaluator the evaluator with all options set.
   */
  public void setEvaluator(ASEvaluation evaluator) {
    m_Evaluator = evaluator;
  }

  /**
   * Gets the attribute evaluator used
   *
   * @return the attribute evaluator
   */
  public ASEvaluation getEvaluator() {
    return m_Evaluator;
  }

  /**
   * Gets the evaluator specification string, which contains the class name of
   * the attribute evaluator and any options to it
   *
   * @return the evaluator string.
   */
  protected String getEvaluatorSpec() {
    
    ASEvaluation e = getEvaluator();
    if (e instanceof OptionHandler) {
      return e.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)e).getOptions());
    }
    return e.getClass().getName();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String searchTipText() {
    return "Set the search method. This search method is used "
      +"during the attribute selection phase before the classifier is "
      +"invoked.";
  }
  
  /**
   * Sets the search method
   *
   * @param search the search method with all options set.
   */
  public void setSearch(ASSearch search) {
    m_Search = search;
  }

  /**
   * Gets the search method used
   *
   * @return the search method
   */
  public ASSearch getSearch() {
    return m_Search;
  }

  /**
   * Gets the search specification string, which contains the class name of
   * the search method and any options to it
   *
   * @return the search string.
   */
  protected String getSearchSpec() {
    
    ASSearch s = getSearch();
    if (s instanceof OptionHandler) {
      return s.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)s).getOptions());
    }
    return s.getClass().getName();
  }

  /**
   * Looks for a fixed set of additional measures in the supplied
   * classifier. If a classifier doesn't support a particular additional
   * measure its value is set to -9999.
   */
  private void determineAdditionalMeasures() {
    m_additionalMeasures = new double [MAX_MEASURES];
    for (int i = 0;i < MAX_MEASURES; i++) {
      m_additionalMeasures[i] = -9999;
    }
    MethodDescriptor methods[];
    try {
      BeanInfo bi = Introspector.getBeanInfo(m_Classifier.getClass());
      methods = bi.getMethodDescriptors();
    } catch (IntrospectionException ex) {
      System.err.println("AttributeSelectedClassifier: Couldn't "
			 +"introspect");
      return;
    }
    Class args [] = { };
    int index;
    for (int i = 0; i < methods.length; i++) {
      String name = methods[i].getDisplayName();
      Method meth = methods[i].getMethod();
      if (name.startsWith("measure")) {
	if (meth.getReturnType().equals(double.class)) {
	  if (name.compareTo("measureTreeSize") == 0) {
	    index = 0;
	  } else if (name.compareTo("measureNumRules") == 0) {
	    index = 1;
	  } else if (name.compareTo("measureNumLeaves") == 0) {
	    index = 2;
	  } else if (name.compareTo("measureNumLinearModels") == 0) {
	    index = 3;
	  } else {
	    System.err.println("AttributeSelectedClassifier: Unrecognized "
			       +"additional measure (skipping).");
	    index = MAX_MEASURES;
	  }
	  if (index < MAX_MEASURES) {
	    try {
	      Double value = (Double)(meth.invoke(m_Classifier, args));
	      m_additionalMeasures[index] = value.doubleValue();
	    } catch (Exception ex) {
	      System.err.println("AttributeSelectedClassifier: Problem "
				 +"invoking method.");
	    }
	  }
	}
      }
    }
  }

  /**
   * Build the classifier on the dimensionally reduced data.
   *
   * @param data the training data
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {
    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }

    if (m_Evaluator == null) {
      throw new Exception("No attribute evaluator has been set!");
    }

    if (m_Search == null) {
      throw new Exception("No search method has been set!");
    }
   
    Instances newData = new Instances(data);
    newData.deleteWithMissingClass();
    if (newData.classAttribute().isNominal()) {
      m_numClasses = newData.classAttribute().numValues();
    } else {
      m_numClasses = 1;
    }

    m_AttributeSelection = new AttributeSelection();
    m_AttributeSelection.setEvaluator(m_Evaluator);
    //    m_AttributeSelection.setEvaluator(new CfsSubsetEval());
    m_AttributeSelection.setSearch(m_Search);
    //    m_AttributeSelection.setSearch(new BestFirst());
    long start = System.currentTimeMillis();
    m_AttributeSelection.SelectAttributes(newData);
    long end = System.currentTimeMillis();
    newData = m_AttributeSelection.reduceDimensionality(newData);
    m_Classifier.buildClassifier(newData);
    long end2 = System.currentTimeMillis();
    m_ReducedHeader = new Instances(newData, 0);
    m_selectionTime = (double)(end - start);
    m_totalTime = (double)(end2 - start);
    determineAdditionalMeasures();
  }

  /**
   * Classifies a given instance after attribute selection
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double [] distributionForInstance(Instance instance)
    throws Exception {

    if (m_AttributeSelection == null) {
      throw new Exception("AttributeSelectedClassifier: No model built yet!");
    }
    Instance newInstance = m_AttributeSelection.reduceDimensionality(instance);

    if (m_Classifier instanceof DistributionClassifier) {
      return ((DistributionClassifier)m_Classifier)
	.distributionForInstance(newInstance);
    }

    double pred = m_Classifier.classifyInstance(newInstance);
    double [] result = new double[m_numClasses];
    if (Instance.isMissingValue(pred)) {
      return result;
    }
    switch (instance.classAttribute().type()) {
    case Attribute.NOMINAL:
      result[(int) pred] = 1.0;
      break;
    case Attribute.NUMERIC:
      result[0] = pred;
      break;
    default:
      throw new Exception("Unknown class type");
    }
    return result;
  }

  /**
   * Output a representation of this classifier
   */
  public String toString() {
    if (m_AttributeSelection == null) {
      return "AttributeSelectedClassifier: No model built yet.";
    }

    StringBuffer result = new StringBuffer();
    result.append("AttributeSelectedClassifier:\nSelection time: "
		  +measureSelectionTime()+"\n");
    result.append(m_AttributeSelection.toResultsString());
    result.append("\n\nHeader of reduced data:\n"+m_ReducedHeader.toString());
    result.append("\n\nClassifier Model\n"+m_Classifier.toString());

    return result.toString();
  }

  /**
   * Additional measure --- time taken (milliseconds) to select the attributes
   * @return the time taken to select attributes
   */
  public double measureSelectionTime() {
    return m_selectionTime;
  }

  /**
   * Additional measure --- time taken (milliseconds) to select attributes
   * and build the classifier
   * @return the total time (select attributes + build classifier)
   */
  public double measureTime() {
    return m_totalTime;
  }

  /**
   * Additional measure tree size. If classifier does not support then
   * -9999 is returned.
   * @return the tree size or -9999 if classifier does not produce a tree
   */
  public double measureTreeSize() {
    return m_additionalMeasures[0];
  }

  /**
   * Additional measure number of rules. If classifier does not support then
   * -9999 is returned.
   * @return the number of rules or -9999 if classifier does not produce 
   * a rule set
   */
  public double measureNumRules() {
    return m_additionalMeasures[1];
  }

  /**
   * Additional measure number of leaves. If classifier does not support then
   * -9999 is returned.
   * @return the number of leaves or -9999 if classifier does not produce 
   * a tree
   */
  public double measureNumLeaves() {
    return m_additionalMeasures[2];
  }

  /**
   * Additional measure number of linear models. If classifier does not 
   * support then -9999 is returned.
   * @return the number of linear models or -9999 if classifier does not 
   * produce a model tree
   */
  public double measureNumLinearModels() {
    return m_additionalMeasures[3];
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation
			 .evaluateModel(new AttributeSelectedClassifier(),
					argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
