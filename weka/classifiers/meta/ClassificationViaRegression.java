/*
 *    MultiClassClassifier.java
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
 * @version $Revision: 1.1 $
 */
public class ClassificationViaRegression extends DistributionClassifier 
  implements OptionHandler {

  /** The classifiers. (One for each class.) */
  private Classifier[] m_Classifiers;

  /** The filters used to transform the class. */
  private MakeIndicatorFilter[] m_ClassFilters;

  /** The options for the base classifiers. */
  private String[] m_Options;

  /** The class name of the base classifier. */
  private String m_BaseClassifier;

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
      throw new Exception("ClassificationViaRegression can't handle a numeric class!");
    }
    m_Classifiers = new Classifier[insts.numClasses()];
    m_ClassFilters = new MakeIndicatorFilter[insts.numClasses()];
    for (int i = 0; i < insts.numClasses(); i++) {
      m_Classifiers[i] = (Classifier)Class.forName(m_BaseClassifier).newInstance();
      copy = new String[m_Options.length];
      System.arraycopy(m_Options, 0, copy, 0, m_Options.length);
      if (m_Classifiers[i] instanceof OptionHandler) {
	((OptionHandler)m_Classifiers[i]).setOptions(copy);
      }
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

    StringBuffer text = new StringBuffer();

    text.append("Classification via Regression\n\n");
    for (int i = 0; i < m_Classifiers.length; i++) {
      text.append(m_Classifiers[i].toString()+"\n");
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
    
    if (m_BaseClassifier != null) {
      try {
	c = Class.forName(m_BaseClassifier).newInstance();
	if (c instanceof OptionHandler) {
	  vec.addElement(new Option("",
				    "", 0, "\nOptions specific to weak learner "
				    +c.getClass().getName()+":"));
	  Enumeration enum = ((OptionHandler)c).listOptions();
	  while (enum.hasMoreElements()) {
	    vec.addElement(enum.nextElement());
	  }
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
  
    m_BaseClassifier = Utils.getOption('W', options);
    if (m_BaseClassifier.length() == 0) {
      throw new Exception("A 'base' learner must be specified "+
			  "with the -W option.");
    }
      
    // Set the options for the base classifier
    Classifier c = (Classifier)Class.forName(m_BaseClassifier).newInstance();
    if ((c != null) &&
	(c instanceof OptionHandler)) {
      m_Options = Utils.partitionOptions(options);
      String [] tempOptions = (String [])m_Options.clone();
      ((OptionHandler)c).setOptions(tempOptions);
      Utils.checkForRemainingOptions(tempOptions);
    } else {
      m_Options = null;
    }
  };

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    
    String [] classifierOptions = new String [0];
    if (m_BaseClassifier != null) {
      try {
	Classifier c = (Classifier)Class.forName(m_BaseClassifier)
	  .newInstance();
	if (c instanceof OptionHandler) {
	  if (m_Options != null) {
	    ((OptionHandler)c).setOptions((String [])m_Options.clone());
	  }
	  classifierOptions = ((OptionHandler)c).getOptions();
	}
      } catch (Exception ex) {
	;
      }
    }
    String [] options = new String [classifierOptions.length + 3];
    int current = 0;

    if (m_BaseClassifier != null) {
      options[current++] = "-W"; options[current++] = m_BaseClassifier;
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
