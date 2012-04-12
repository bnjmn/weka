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

package weka.attributeSelection;
import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.*;
import weka.filters.*;

/** 
 * Class for Evaluating attributes individually by using the OneR
 * classifier.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version 1.0 March 1999 (Mark)
 */
public class OneRAttributeEval 
  extends AttributeEvaluator
{
  
  private Instances trainInstances;

  private int classIndex;
  
  private int numAttribs;

  private int numInstances;

  public OneRAttributeEval()
  {
    resetOptions();
  }


  /* //**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   *
   **
  public Enumeration listOptions() 
  {
    
    Vector newVector = new Vector(1);
    
    newVector.addElement(new Option("\ttreat missing values as a seperate value.", "M", 0,"-M"));

    return newVector.elements();
  }

  //**
   * Parses a given list of options.
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **
  public void setOptions(String[] options) throws Exception
  {
    resetOptions();
    missing_merge = !(Utils.getFlag('M',options));
  }

  /**
   * Gets the current settings of WrapperSubsetEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   *
  public String [] getOptions()
  {
    String [] options = new String [1];
    int current = 0;

    if (!missing_merge)
      {
	options[current++] = "-M";
      }

    while (current < options.length) 
      {
	options[current++] = "";
      }

    return options;

    } */

  /**
   * Initializes an information gain attribute evaluator.
   * Discretizes all attributes that are numeric.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator(Instances data) throws Exception
  {
    trainInstances = data;
    classIndex = trainInstances.classIndex();
    numAttribs = trainInstances.numAttributes();
    numInstances = trainInstances.numInstances();

    if (trainInstances.attribute(classIndex).isNumeric())
      throw new Exception("Class must be nominal!");
  }


  protected void resetOptions()
  {
    trainInstances = null;
  }

  /**
   * evaluates an individual attribute by measuring the amount
   * of information gained about the class given the attribute.
   *
   * @param attribute the index of the attribute to be evaluated
   * @exception Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute(int attribute) throws Exception
  {
    int [] featArray = new int [2]; // feat + class
    double errorRate;
    Evaluation o_Evaluation;
    AttributeFilter delTransform = new AttributeFilter();
    delTransform.setInvertSelection(true);

    // copy the instances
    Instances trainCopy = new Instances(trainInstances);

    featArray[0] = attribute;
    featArray[1] = trainCopy.classIndex();

    delTransform.setAttributeIndicesArray(featArray);
    delTransform.inputFormat(trainCopy);
    trainCopy = Filter.useFilter(trainCopy,delTransform);

    o_Evaluation = new Evaluation(trainCopy);
    o_Evaluation.crossValidateModel("weka.classifiers.OneR",
					trainCopy,
					10,
					null);
    
    errorRate = o_Evaluation.errorRate();

    return (1-errorRate)*100.0;
  }

  public String toString()
  {
    StringBuffer text = new StringBuffer();
    
    text.append("\tOneR Ranking Filter");

    text.append("\n");

    return text.toString();
  }

  // ============
  // Test method.
  // ============
  
  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file
   */
  
  public static void main(String [] argv)
  {
    
    try {
      System.out.println(AttributeSelection.SelectAttributes(new OneRAttributeEval(), argv));
    }
    catch (Exception e)
      {
	e.printStackTrace();
	System.out.println(e.getMessage());
      }
  }
  

}
