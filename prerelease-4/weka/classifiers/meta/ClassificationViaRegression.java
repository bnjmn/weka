/*
 *    ClassificationViaRegression.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
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

import java.util.*;
import weka.core.*;
import weka.filters.*;

/**
 * Class for doing classification using regression methods.
 *
 * Valid options are:<p>
 *
 * -W classname <br>
 * Specify the full class name of a numeric predictor as the basis for 
 * the classifier (required).<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class ClassificationViaRegression extends DistributionClassifier 
  implements OptionHandler {

  /** The classifiers. (One for each class.) */
  private Classifier[] m_Classifiers;

  /** The filters used to transform the class. */
  private MakeIndicatorFilter[] m_ClassFilters;

  /** The class name of the base classifier. */
  private Classifier m_Classifier = new weka.classifiers.ZeroR();

  /**
   * Builds the classifiers.
   *
   * @param insts the training data.
   * @exception Exception if a classifier can't be built
   */
  public void buildClassifier(Instances insts) throws Exception {

    String[] copy;
    Instances newInsts;

    if (insts.classAttribute().isNumeric()) {
      throw new Exception("ClassificationViaRegression can't handle a"
			  + " numeric class!");
    }
    m_Classifiers = Classifier.makeCopies(m_Classifier, insts.numClasses());
    m_ClassFilters = new MakeIndicatorFilter[insts.numClasses()];
    for (int i = 0; i < insts.numClasses(); i++) {
      m_ClassFilters[i] = new MakeIndicatorFilter();
      m_ClassFilters[i].setAttributeIndex(insts.classIndex());
      m_ClassFilters[i].setValueIndex(i);
      m_ClassFilters[i].setNumeric(true);
      m_ClassFilters[i].inputFormat(insts);
      newInsts = Filter.useFilter(insts, m_ClassFilters[i]);
      m_Classifiers[i].buildClassifier(newInsts);
    }
  }

  /**
   * Returns the distribution for an instance.
   *
   * @exception Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
    
    double[] probs = new double[inst.numClasses()];
    Instance newInst;
    double sum = 0, max = Double.MIN_VALUE, min = Double.MAX_VALUE;

    for (int i = 0; i < inst.numClasses(); i++) {
      m_ClassFilters[i].input(inst);
      newInst = m_ClassFilters[i].output();
      probs[i] = m_Classifiers[i].classifyInstance(newInst);
      if (probs[i] > 1) {
        probs[i] = 1;
      }
      if (probs[i] < 0){
	probs[i] = 0;
      }
      sum += probs[i];
    }
    if (sum != 0) {
      Utils.normalize(probs, sum);
    } 
    return probs;
  }

  /**
   * Prints the classifiers.
   */
  public String toString() {

    if (m_Classifiers == null) {
      return "Classification via Regression: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("Classification via Regression\n\n");
    for (int i = 0; i < m_Classifiers.length; i++) {
      text.append(m_Classifiers[i].toString() + "\n");
    }
    return text.toString();
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions()  {

    Vector vec = new Vector(1);
    Object c;
    
    vec.addElement(new Option("\tSets the base classifier.",
			      "W", 1, "-W <base classifier>"));
    
    if (m_Classifier != null) {
      try {
	vec.addElement(new Option("",
				  "", 0, "\nOptions specific to classifier "
				  + m_Classifier.getClass().getName() + ":"));
	Enumeration enum = ((OptionHandler)m_Classifier).listOptions();
	while (enum.hasMoreElements()) {
	  vec.addElement(enum.nextElement());
	}
      } catch (Exception e) {
      }
    }
    return vec.elements();
  }

  /**
   * Sets a given list of options. Valid options are:<p>
   *
   * -W classname <br>
   * Specify the full class name of a numeric predictor as the basis for 
   * the classifier (required).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception{
  
    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }
    setClassifier(Classifier.forName(classifierName,
				     Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    
    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) &&
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }
    String [] options = new String [classifierOptions.length + 3];
    int current = 0;

    if (getClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getClassifier().getClass().getName();
    }
    options[current++] = "--";

    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Set the base classifier. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the base classifier (regression scheme) used as the classifier
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options for the learner
   */
  public static void main(String [] argv){

    DistributionClassifier scheme;

    try {
      scheme = new ClassificationViaRegression();
      System.out.println(Evaluation.evaluateModel(scheme,argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
}
