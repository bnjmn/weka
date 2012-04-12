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
 *    LinearRegression.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.filters.*;

/**
 * Class for using linear regression for prediction. Uses the Akaike 
 * criterion for model selection, and is able to deal with weighted
 * instances. <p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Produce debugging output. <p>
 *
 * -S num <br>
 * Set the attriute selection method to use. 1 = None, 2 = Greedy
 * (default 0 = M5' method) <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.12 $
 */
public class LinearRegression extends Classifier implements OptionHandler,
  WeightedInstancesHandler {

  /** Array for storing coefficients of linear regression. */
  private double[] m_Coefficients;

  /** Which attributes are relevant? */
  private boolean[] m_SelectedAttributes;

  /** Variable for storing transformed training data. */
  private Instances m_TransformedData;

  /** The filter for removing missing values. */
  private ReplaceMissingValuesFilter m_MissingFilter;

  /** The filter storing the transformation from nominal to binary attributes.
   */
  private NominalToBinaryFilter m_TransformFilter;

  /** The standard deviations of the attributes */
  private double [] m_StdDev;

  /** The index of the class attribute */
  private int m_ClassIndex;

  /** True if debug output will be printed */
  private boolean b_Debug;

  /** The current attribute selection method */
  private int m_AttributeSelection;

  /* Attribute selection methods */
  private static final int SELECTION_M5 = 0;
  private static final int SELECTION_NONE = 1;
  private static final int SELECTION_GREEDY = 2;
  public static final Tag [] TAGS_SELECTION = {
    new Tag(SELECTION_NONE, "No attribute selection"),
    new Tag(SELECTION_M5, "M5 method"),
    new Tag(SELECTION_GREEDY, "Greedy method")
  };

  /**
   * Builds a regression model for the given data.
   *
   * @param data the training data to be used for generating the
   * linear regression function
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {
  
    if (!data.classAttribute().isNumeric()) {
      throw new Exception("Class attribute has to be numeric for regression!");
    }
    if (data.numInstances() == 0) {
      throw new Exception("No instances in training file!");
    }
    if (data.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }

    // Preprocess instances
    m_TransformedData = data;
    m_TransformFilter = new NominalToBinaryFilter();
    m_TransformFilter.setInputFormat(m_TransformedData);
    m_TransformedData = Filter.useFilter(m_TransformedData, m_TransformFilter);
    m_MissingFilter = new ReplaceMissingValuesFilter();
    m_MissingFilter.setInputFormat(m_TransformedData);
    m_TransformedData = Filter.useFilter(m_TransformedData, m_MissingFilter);
    m_TransformedData.deleteWithMissingClass();
    m_ClassIndex = m_TransformedData.classIndex();

    // Calculate attribute standard deviations
    calculateAttributeDeviations();

    // Perform the regression
    findBestModel();

    // Save memory
    m_TransformedData = new Instances(m_TransformedData, 0);
  }

  /**
   * Classifies the given instance using the linear regression function.
   *
   * @param instance the test instance
   * @return the classification
   * @exception Exception if classification can't be done successfully
   */
  public double classifyInstance(Instance instance) throws Exception {

    // Transform the input instance
    Instance transformedInstance = instance;
    m_TransformFilter.input(transformedInstance);
    m_TransformFilter.batchFinished();
    transformedInstance = m_TransformFilter.output();
    m_MissingFilter.input(transformedInstance);
    m_MissingFilter.batchFinished();
    transformedInstance = m_MissingFilter.output();

    // Calculate the dependent variable from the regression model
    return regressionPrediction(transformedInstance,
				m_SelectedAttributes,
				m_Coefficients);
  }

  /**
   * Outputs the linear regression model as a string.
   */
  public String toString() {

    if (m_TransformedData == null) {
      return "Linear Regression: No model built yet.";
    }
    try {
      StringBuffer text = new StringBuffer();
      int column = 0;
      boolean first = true;
      
      text.append("\nLinear Regression Model\n\n");
      
      text.append(m_TransformedData.classAttribute().name()+" =\n\n");
      for (int i = 0; i < m_TransformedData.numAttributes(); i++) {
	if ((i != m_ClassIndex) 
	    && (m_SelectedAttributes[i])) {
	  if (!first) 
	    text.append(" +\n");
	  else
	    first = false;
	  text.append(Utils.doubleToString(m_Coefficients[column], 12, 4)
		      + " * ");
	  text.append(m_TransformedData.attribute(i).name());
	  column++;
	}
      }
      text.append(" +\n" + 
		  Utils.doubleToString(m_Coefficients[column], 12, 4));
      return text.toString();
    } catch (Exception e) {
      return "Can't print Linear Regression!";
    }
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(2);
    newVector.addElement(new Option("\tProduce debugging output.\n"
				    + "\t(default no debugging output)",
				    "D", 0,"-D"));
    newVector.addElement(new Option("\tSet the attribute selection method"
				    + " to use. 1 = None, 2 = Greedy.\n"
				    + "\t(default 0 = M5' method)",
				    "S", 1,"-S <number of selection method>"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Produce debugging output. <p>
   *
   * -S num <br>
   * Set the attriute selection method to use. 1 = None, 2 = Greedy
   * (default 0 = M5' method) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String selectionString = Utils.getOption('S', options);
    if (selectionString.length() != 0) {
      setAttributeSelectionMethod(new SelectedTag(Integer
						  .parseInt(selectionString),
						  TAGS_SELECTION));
    } else {
      setAttributeSelectionMethod(new SelectedTag(SELECTION_M5,
						  TAGS_SELECTION));
    }

    setDebug(Utils.getFlag('D', options));
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [3];
    int current = 0;

    options[current++] = "-S";
    options[current++] = "" + getAttributeSelectionMethod()
      .getSelectedTag().getID();
    if (getDebug()) {
      options[current++] = "-D";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Get the number of coefficients used in the model
   *
   * @return the number of coefficients
   */
  public int numParameters()
  {
    return m_Coefficients.length-1;
  }

  /**
   * Sets the method used to select attributes for use in the
   * linear regression. 
   *
   * @param method the attribute selection method to use.
   */
  public void setAttributeSelectionMethod(SelectedTag method) {
    
    if (method.getTags() == TAGS_SELECTION) {
      m_AttributeSelection = method.getSelectedTag().getID();
    }
  }

  /**
   * Gets the method used to select attributes for use in the
   * linear regression. 
   *
   * @return the method to use.
   */
  public SelectedTag getAttributeSelectionMethod() {
    
    return new SelectedTag(m_AttributeSelection, TAGS_SELECTION);
  }

  /**
   * Controls whether debugging output will be printed
   *
   * @param debug true if debugging output should be printed
   */
  public void setDebug(boolean debug) {

    b_Debug = debug;
  }

  /**
   * Controls whether debugging output will be printed
   *
   * @param debug true if debugging output should be printed
   */
  public boolean getDebug() {

    return b_Debug;
  }

  /**
   * Calculates the standard deviations of each attribute. The
   * results are stored in m_StdDev.
   *
   * @exception Exception if any of the attributes are not numeric
   */
  private void calculateAttributeDeviations() throws Exception {

    m_StdDev = new double [m_TransformedData.numAttributes()];
    for (int i = 0; i < m_TransformedData.numAttributes(); i++) {
      m_StdDev[i] = Math.sqrt(m_TransformedData.variance(i));
    }
  }

  /**
   * Removes the attribute with the highest standardised coefficient
   * greater than 1.5 from the selected attributes.
   *
   * @param selectedAttributes an array of flags indicating which 
   * attributes are included in the regression model
   * @param coefficients an array of coefficients for the regression
   * model
   * @return true if an attribute was removed
   */
  private boolean deselectColinearAttribute(boolean [] selectedAttributes,
					    double [] coefficients) {

    double maxSC = 1.5;
    int maxAttr = -1, coeff = 0;
    for (int i = 0; i < selectedAttributes.length; i++) {
      if (selectedAttributes[i]) {
	double SC = Math.abs(coefficients[coeff] * m_StdDev[i] 
			     / m_StdDev[m_ClassIndex]);
	if (SC > maxSC) {
	  maxSC = SC;
	  maxAttr = i;
	}
	coeff++;
      }
    }
    if (maxAttr >= 0) {
      selectedAttributes[maxAttr] = false;
      if (b_Debug) {
	System.out.println("Deselected colinear attribute:" + (maxAttr + 1)
			   + " with standardised coefficient: " + maxSC);
      }
      return true;
    }
    return false;
  }

  /**
   * Performs a greedy search for the best regression model using
   * Akaike's criterion.
   *
   * @exception Exception if regression can't be done
   */
  private void findBestModel() throws Exception {

    int numAttributes = m_TransformedData.numAttributes();
    int numInstances = m_TransformedData.numInstances();
    boolean [] selectedAttributes = new boolean[numAttributes];
    for (int i = 0; i < numAttributes; i++) {
      if (i != m_ClassIndex) {
	selectedAttributes[i] = true;
      }
    }

    if (b_Debug) {
      System.out.println((new Instances(m_TransformedData, 0)).toString());
    }

    // Perform a regression for the full model, and remove colinear attributes
    double [] coefficients;
    do {
      coefficients = doRegression(selectedAttributes);
    } while (deselectColinearAttribute(selectedAttributes, coefficients));

    double fullMSE = calculateMSE(selectedAttributes, coefficients);
    double akaike = (numInstances - numAttributes) + 2 * numAttributes;
    if (b_Debug) {
      System.out.println("Initial Akaike value: " + akaike);
    }

    boolean improved;
    int currentNumAttributes = numAttributes;
    switch (m_AttributeSelection) {

    case SELECTION_GREEDY:

      // Greedy attribute removal
      do {
	boolean [] currentSelected = (boolean []) selectedAttributes.clone();
	improved = false;
	currentNumAttributes--;

	for (int i = 0; i < numAttributes; i++) {
	  if (currentSelected[i]) {

	    // Calculate the akaike rating without this attribute
	    currentSelected[i] = false;
	    double [] currentCoeffs = doRegression(currentSelected);
	    double currentMSE = calculateMSE(currentSelected, currentCoeffs);
	    double currentAkaike = currentMSE / fullMSE 
	    * (numInstances - numAttributes)
	    + 2 * currentNumAttributes;
	    if (b_Debug) {
	      System.out.println("(akaike: " + currentAkaike);
	    }

	    // If it is better than the current best
	    if (currentAkaike < akaike) {
	      if (b_Debug) {
		System.err.println("Removing attribute " + (i + 1)
				   + " improved Akaike: " + currentAkaike);
	      }
	      improved = true;
	      akaike = currentAkaike;
	      System.arraycopy(currentSelected, 0,
			       selectedAttributes, 0,
			       selectedAttributes.length);
	      coefficients = currentCoeffs;
	    }
	    currentSelected[i] = true;
	  }
	}
      } while (improved);
      break;

    case SELECTION_M5:

      // Step through the attributes removing the one with the smallest 
      // standardised coefficient until no improvement in Akaike
      do {
	improved = false;
	currentNumAttributes--;

	// Find attribute with smallest SC
	double minSC = 0;
	int minAttr = -1, coeff = 0;
	for (int i = 0; i < selectedAttributes.length; i++) {
	  if (selectedAttributes[i]) {
	    double SC = Math.abs(coefficients[coeff] * m_StdDev[i] 
				 / m_StdDev[m_ClassIndex]);

	    if ((coeff == 0) || (SC < minSC)) {
	      minSC = SC;
	      minAttr = i;
	    }
	    coeff++;
	  }
	}

	// See whether removing it improves the Akaike score
	if (minAttr >= 0) {
	  selectedAttributes[minAttr] = false;
	  double [] currentCoeffs = doRegression(selectedAttributes);
	  double currentMSE = calculateMSE(selectedAttributes, currentCoeffs);
	  double currentAkaike = currentMSE / fullMSE 
	  * (numInstances - numAttributes)
	  + 2 * currentNumAttributes;
	  if (b_Debug) {
	    System.out.println("(akaike: " + currentAkaike);
	  }

	  // If it is better than the current best
	  if (currentAkaike < akaike) {
	    if (b_Debug) {
	      System.err.println("Removing attribute " + (minAttr + 1)
				 + " improved Akaike: " + currentAkaike);
	    }
	    improved = true;
	    akaike = currentAkaike;
	    coefficients = currentCoeffs;
	  } else {
	    selectedAttributes[minAttr] = true;
	  }
	}
      } while (improved);
      break;

    case SELECTION_NONE:
      break;
    }
    m_SelectedAttributes = selectedAttributes;
    m_Coefficients = coefficients;
  }

  /**
   * Calculate the mean squared error of a regression model on the 
   * training data
   *
   * @param selectedAttributes an array of flags indicating which 
   * attributes are included in the regression model
   * @param coefficients an array of coefficients for the regression
   * model
   * @return the mean squared error on the training data
   * @exception Exception if there is a missing class value in the training
   * data
   */
  private double calculateMSE(boolean [] selectedAttributes, 
			      double [] coefficients) throws Exception {

    double mse = 0;
    for (int i = 0; i < m_TransformedData.numInstances(); i++) {
      double prediction = regressionPrediction(m_TransformedData.instance(i),
					       selectedAttributes,
					       coefficients);
      double error = prediction - m_TransformedData.instance(i).classValue();
      mse += error * error;
    }
    return mse;
  }

  /**
   * Calculate the dependent value for a given instance for a
   * given regression model.
   *
   * @param transformedInstance the input instance
   * @param selectedAttributes an array of flags indicating which 
   * attributes are included in the regression model
   * @param coefficients an array of coefficients for the regression
   * model
   * @return the regression value for the instance.
   * @exception Exception if the class attribute of the input instance
   * is not assigned
   */
  private double regressionPrediction(Instance transformedInstance,
				      boolean [] selectedAttributes,
				      double [] coefficients) 
  throws Exception {
    
    double result = 0;
    int column = 0;
    for (int j = 0; j < transformedInstance.numAttributes(); j++) 
      if ((m_ClassIndex != j) 
	  && (selectedAttributes[j])) {
	result += coefficients[column] * transformedInstance.value(j);
	column++;
      }
    result += coefficients[column];
    
    return result;
  }

  /**
   * Calculate a linear regression using the selected attributes
   *
   * @param selectedAttributes an array of booleans where each element
   * is true if the corresponding attribute should be included in the
   * regression.
   * @return an array of coefficients for the linear regression model.
   * @exception Exception if an error occurred during the regression.
   */
  private double [] doRegression(boolean [] selectedAttributes) 
  throws Exception {

    if (b_Debug) {
      System.out.print("doRegression(");
      for (int i = 0; i < selectedAttributes.length; i++) {
	System.out.print(" " + selectedAttributes[i]);
      }
      System.out.println(" )");
    }
    int numAttributes = 1;
    for (int i = 0; i < selectedAttributes.length; i++) {
      if (selectedAttributes[i]) {
	numAttributes++;
      }
    }

    Matrix independent = new Matrix(m_TransformedData.numInstances(), 
				    numAttributes);
    Matrix dependent = new Matrix(m_TransformedData.numInstances(), 1);
    for (int i = 0; i < m_TransformedData.numInstances(); i ++) {
      int column = 0;
      for (int j = 0; j < m_TransformedData.numAttributes(); j++) {
	if (j == m_ClassIndex) {
	  dependent.setElement(i, 0, 
			       m_TransformedData.instance(i).classValue());
	} else {
	  if (selectedAttributes[j]) {
	    independent.setElement(i, column,
				   m_TransformedData.instance(i).value(j));
	    column++;
	  }
	}
      }
      independent.setElement(i, column, 1.0);
    }

    // Grab instance weights
    double [] weights = new double [m_TransformedData.numInstances()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = m_TransformedData.instance(i).weight();
    }

    // Compute coefficients
    return independent.regression(dependent, weights);
  }
 
  /**
   * Generates a linear regression function predictor.
   *
   * @param String the options
   */
  public static void main(String argv[]) {
    
    try {
      System.out.println(Evaluation.evaluateModel(new LinearRegression(),
						  argv));
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}


  
