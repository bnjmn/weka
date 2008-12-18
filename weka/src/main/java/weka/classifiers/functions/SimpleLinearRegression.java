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
 *    SimpleLinearRegression.java
 *    Copyright (C) 2002 Eibe Frank
 *
 */

package weka.classifiers.functions;

import weka.core.*;
import weka.classifiers.*;

/**
 * Class for learning a simple linear regression model.
 * Picks the attribute that results in the lowest squared error.
 * Missing values are not allowed. Can only deal with numeric attributes.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $
 */
public class SimpleLinearRegression extends Classifier 
  implements WeightedInstancesHandler {

  /** The chosen attribute */
  private Attribute m_attribute;

  /** The index of the chosen attribute */
  private int m_attributeIndex;

  /** The slope */
  private double m_slope;
  
  /** The intercept */
  private double m_intercept;

  /** If true, suppress error message if no useful attribute was found*/   
  private boolean m_suppressErrorMessage = false;  

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Learns a simple linear regression model. "
      +"Picks the attribute that results in the lowest squared error. "
      +"Missing values are not allowed. Can only deal with numeric attributes.";
  }

  /**
   * Generate a prediction for the supplied instance.
   *
   * @param inst the instance to predict.
   * @return the prediction
   * @exception Exception if an error occurs
   */
  public double classifyInstance(Instance inst) throws Exception {
    
    if (m_attribute == null) {
      return m_intercept;
    } else {
      if (inst.isMissing(m_attribute.index())) {
	throw new Exception("SimpleLinearRegression: No missing values!");
      }
      return m_intercept + m_slope * inst.value(m_attribute.index());
    }
  }
  
  /**
   * Builds a simple linear regression model given the supplied training data.
   *
   * @param insts the training data.
   * @exception Exception if an error occurs
   */
  public void buildClassifier(Instances insts) throws Exception {

    if (!insts.classAttribute().isNumeric()) {
      throw new UnsupportedClassTypeException("Class attribute has to be numeric for regression!");
    }
    if (insts.numInstances() == 0) {
      throw new Exception("No instances in training file!");
    }
    if (insts.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }

    // Compute mean of target value
    double yMean = insts.meanOrMode(insts.classIndex());

    // Choose best attribute
    double minMsq = Double.MAX_VALUE;
    m_attribute = null;
    int chosen = -1;
    double chosenSlope = Double.NaN;
    double chosenIntercept = Double.NaN;
    for (int i = 0; i < insts.numAttributes(); i++) {
      if (i != insts.classIndex()) {
	if (!insts.attribute(i).isNumeric()) {
	  throw new Exception("SimpleLinearRegression: Only numeric attributes!");
	}
	m_attribute = insts.attribute(i);
	
	// Compute slope and intercept
	double xMean = insts.meanOrMode(i);
	double sumWeightedXDiffSquared = 0;
	double sumWeightedYDiffSquared = 0;
	m_slope = 0;
	for (int j = 0; j < insts.numInstances(); j++) {
	  Instance inst = insts.instance(j);
	  if (!inst.isMissing(i) && !inst.classIsMissing()) {
	    double xDiff = inst.value(i) - xMean;
	    double yDiff = inst.classValue() - yMean;
	    double weightedXDiff = inst.weight() * xDiff;
	    double weightedYDiff = inst.weight() * yDiff;
	    m_slope += weightedXDiff * yDiff;
	    sumWeightedXDiffSquared += weightedXDiff * xDiff;
	    sumWeightedYDiffSquared += weightedYDiff * yDiff;
	  }
	}

	// Skip attribute if not useful
	if (sumWeightedXDiffSquared == 0) {
	  continue;
	}
	double numerator = m_slope;
	m_slope /= sumWeightedXDiffSquared;
	m_intercept = yMean - m_slope * xMean;

	// Compute sum of squared errors
	double msq = sumWeightedYDiffSquared - m_slope * numerator;

	// Check whether this is the best attribute
	if (msq < minMsq) {
	  minMsq = msq;
	  chosen = i;
	  chosenSlope = m_slope;
	  chosenIntercept = m_intercept;
	}
      }
    }

    // Set parameters
    if (chosen == -1) {
      if (!m_suppressErrorMessage) System.err.println("----- no useful attribute found");
      m_attribute = null;
      m_attributeIndex = 0;
      m_slope = 0;
      m_intercept = yMean;
    } else {
      m_attribute = insts.attribute(chosen);
      m_attributeIndex = chosen;
      m_slope = chosenSlope;
      m_intercept = chosenIntercept;
    }
  }

  /**
   * Returns true if a usable attribute was found.
   *
   * @return true if a usable attribute was found.
   */
  public boolean foundUsefulAttribute(){
      return (m_attribute != null); 
  } 

  /**
   * Returns the index of the attribute used in the regression.
   *
   * @return the index of the attribute.
   */
  public int getAttributeIndex(){
      return m_attributeIndex;
  }

  /**
   * Returns the slope of the function.
   *
   * @return the slope.
   */
  public double getSlope(){
      return m_slope;
  }
    
  /**
   * Returns the intercept of the function.
   *
   * @return the intercept.
   */
  public double getIntercept(){
      return m_intercept;
  }  

  /**
   * Turn off the error message that is reported when no useful attribute is found.
   *
   * @param s if set to true turns off the error message
   */
  public void setSuppressErrorMessage(boolean s){
      m_suppressErrorMessage = s;
  }   

  /**
   * Returns a description of this classifier as a string
   *
   * @return a description of the classifier.
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    if (m_attribute == null) {
      text.append("Predicting constant " + m_intercept);
    } else {
      text.append("Linear regression on " + m_attribute.name() + "\n\n");
      text.append(Utils.doubleToString(m_slope,2) + " * " + 
		m_attribute.name());
      if (m_intercept > 0) {
	text.append(" + " + Utils.doubleToString(m_intercept, 2));
      } else {
      text.append(" - " + Utils.doubleToString((-m_intercept), 2)); 
      }
    }
    text.append("\n");
    return text.toString();
  }

  /**
   * Main method for testing this class
   *
   * @param argv options
   */
  public static void main(String [] argv){

    try{
      System.out.println(Evaluation.evaluateModel(new SimpleLinearRegression(), argv));
    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  } 

}
