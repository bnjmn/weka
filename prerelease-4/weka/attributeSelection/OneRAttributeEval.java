/*
 *    OneRAttributeEval.java
 *    Copyright (C) 1999 Mark Hall
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
package  weka.attributeSelection;

import  java.io.*;
import  java.util.*;
import  weka.core.*;
import  weka.classifiers.*;
import  weka.filters.*;

/** 
 * Class for Evaluating attributes individually by using the OneR
 * classifier. <p>
 *
 * No options. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class OneRAttributeEval
  extends AttributeEvaluator
{

  /** The training instances */
  private Instances m_trainInstances;

  /** The class index */
  private int m_classIndex;

  /** The number of attributes */
  private int m_numAttribs;

  /** The number of instances */
  private int m_numInstances;


  /**
   * Constructor
   */
  public OneRAttributeEval () {
    resetOptions();
  }


  /**
   * Initializes an information gain attribute evaluator.
   * Discretizes all attributes that are numeric.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator (Instances data)
    throws Exception
  {
    m_trainInstances = data;

    if (m_trainInstances.checkForStringAttributes()) {
      throw  new Exception("Can't handle string attributes!");
    }

    m_classIndex = m_trainInstances.classIndex();
    m_numAttribs = m_trainInstances.numAttributes();
    m_numInstances = m_trainInstances.numInstances();

    if (m_trainInstances.attribute(m_classIndex).isNumeric()) {
      throw  new Exception("Class must be nominal!");
    }
  }


  /**
   * rests to defaults.
   */
  protected void resetOptions () {
    m_trainInstances = null;
  }


  /**
   * evaluates an individual attribute by measuring the amount
   * of information gained about the class given the attribute.
   *
   * @param attribute the index of the attribute to be evaluated
   * @exception Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute (int attribute)
    throws Exception
  {
    int[] featArray = new int[2]; // feat + class
    double errorRate;
    Evaluation o_Evaluation;
    AttributeFilter delTransform = new AttributeFilter();
    delTransform.setInvertSelection(true);
    // copy the instances
    Instances trainCopy = new Instances(m_trainInstances);
    featArray[0] = attribute;
    featArray[1] = trainCopy.classIndex();
    delTransform.setAttributeIndicesArray(featArray);
    delTransform.inputFormat(trainCopy);
    trainCopy = Filter.useFilter(trainCopy, delTransform);
    o_Evaluation = new Evaluation(trainCopy);
    o_Evaluation.crossValidateModel("weka.classifiers.OneR", trainCopy, 10, null);
    errorRate = o_Evaluation.errorRate();
    return  (1 - errorRate)*100.0;
  }


  /**
   * Return a description of the evaluator
   * @return description as a string
   */
  public String toString () {
    StringBuffer text = new StringBuffer();

    if (m_trainInstances == null) {
      text.append("\tOneR feature evaluator has not been built yet");
    }
    else {
      text.append("\tOneR feature evaluator");
    }

    text.append("\n");
    return  text.toString();
  }


  // ============
  // Test method.
  // ============
  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {
    try {
      System.out.println(AttributeSelection.
			 SelectAttributes(new OneRAttributeEval(), args));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

}

