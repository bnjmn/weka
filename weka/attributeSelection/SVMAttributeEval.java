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
 *    SVMAttributeEval.java
 *    Copyright (C) 2002 Eibe Frank
 *
 */

package weka.attributeSelection;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.functions.SMO;
import weka.filters.Filter;
import weka.filters.AttributeFilter;

/** 
 * Class for Evaluating attributes individually by using the SVM
 * classifier. <p>
 *
 * Valid options are: <p>
 *
 * -E <num atts to eliminate> <br>
 * Specify the number of attributes to eliminate on each invocation
 * of the support vector machine. <p>
 *
 * -P <complexity parameter> <br>
 * Specify the value of C - the complexity parameter to be passed on
 * to the support vecotor machine. <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class SVMAttributeEval 
  extends AttributeEvaluator 
  implements OptionHandler {
  
  /** The attribute scores */
  private double[] m_attScores;

  /** The number of attributes to eliminate per iteration */
  private int m_numToEliminate = 1;

  /** Complexity parameter to pass on to SMO */
  private double m_smoCParameter = 1.0;

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "SVMAttributeEval :\n\nEvaluates the worth of an attribute by "
      +"using an SVM classifier.\n";
  }

  /**
   * Constructor
   */
  public SVMAttributeEval () {
    resetOptions();
  }

  /**
   * Returns an enumeration describing all the available options
   *
   * @return an enumeration of options
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(2);

    newVector.addElement(new Option("\tSpecify the number of attributes to\n"
				    + "\teliminate with each invocation of\n"
				    + "\tthe support vector machine.\n"
				    + "\tDefault = 1.", "E", 1,
				    "-N <num atts to eliminate>"));

    newVector.addElement(new Option("\tSpecify the value of C (complexity\n"
				    + "\tparameter) to pass on to the\n"
				    + "\tsupport vector machine.\n"
				    + "\tDefault = 1.0", "P", 1,
				    "-P <complexity>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options
   *
   * Valid options are: <p>
   *
   * -E <num atts to eliminate> <br>
   * Specify the number of attributes to eliminate on each invocation
   * of the support vector machine. <p>
   *
   * -P <complexity parameter> <br>
   * Specify the value of C - the complexity parameter to be passed on
   * to the support vecotor machine. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an error occurs
   */
  public void setOptions(String [] options) throws Exception {
    String optionString;

    optionString = Utils.getOption('E', options);
    if (optionString.length() != 0) {
      setAttsToEliminatePerIteration(Integer.parseInt(optionString));
    }
    
    optionString = Utils.getOption('P', options);
    if (optionString.length() != 0) {
      setSVMComplexityParameter((new Double(optionString)).doubleValue());
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of SVMAttributeEval
   *
   * @return an array of strings suitable for passing to setOptions() 
   */
  public String [] getOptions() {
    String [] options = new String [4];
    int current = 0;

    options[current++] = "-E"; 
    options[current++] = ""+getAttsToEliminatePerIteration();
    
    options[current++] = "-P"; 
    options[current++] = ""+getSVMComplexityParameter();

    while (current < options.length) {
      options[current++] = "";
    }
    
    return options;
  }

  /**
   * Returns a tip text for this property suitable for display in the
   * GUI
   *
   * @return tip text string describing this property
   */
  public String svmComplexityParameterTipText() {
    return "C complexity parameter to pass to the SVM";
  }

  /**
   * Set the value of C for SMO
   *
   * @param svmC the value of C
   */
  public void setSVMComplexityParameter(double svmC) {
    m_smoCParameter = svmC;
  }

  /**
   * Get the value of C used with SMO
   *
   * @return the value of C
   */
  public double getSVMComplexityParameter() {
    return m_smoCParameter;
  }

  /**
   * Returns a tip text for this property suitable for display in the
   * GUI
   *
   * @return tip text string describing this property
   */
  public String attsToEliminatePerIterationTipText() {
    return "Number of attributes to eliminate per iteration "
      +"(invokation of SVM)";
  }

  /**
   * Set the number of attributes to eliminate per iteration
   *
   * @param numE number of attributes to eliminate per iteration
   */
  public void setAttsToEliminatePerIteration(int numE) {
    m_numToEliminate = numE;
  }

  /**
   * Get the number of attributes to eliminate per iteration
   *
   * @return the number of attributes to eliminate per iteration
   */
  public int getAttsToEliminatePerIteration() {
    return m_numToEliminate;
  }

  /**
   * Initializes the evaluator.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator(Instances data) throws Exception {
    
    if (data.checkForStringAttributes()) {
      throw  new Exception("Can't handle string attributes!");
    }
    
    if (!data.classAttribute().isNominal()) {
      throw new Exception("Class must be nominal!");
    }
    
    if (data.classAttribute().numValues() != 2) {
      throw new Exception("Can only deal with binary class problems!");
    }
    
    // Holds a mapping into the original array of attribute indices
    int[] origIndices = new int[data.numAttributes()];
    for (int i = 0; i < origIndices.length; i++) {
      if (data.attribute(i).isNominal() && 
	  (data.attribute(i).numValues() != 2)) {
	throw new Exception("All nominal attributes must be binary!");
      }
      origIndices[i] = i;
    }
    
    // We need to repeat the following loop until we've computed
    // a weight for every attribute (excluding the class).
    m_attScores = new double[data.numAttributes() - 1];
    Instances trainCopy = new Instances(data);
    //    for (int i = 0; i < m_attScores.length; i++) {
    int i = 0;
    do {
      int numToElim = (m_attScores.length - i >= m_numToEliminate)
	? m_numToEliminate
	: m_attScores.length - i;
      
      // Build the linear SVM with default parameters
      SMO smo = new SMO();
      smo.setC(m_smoCParameter);
      smo.buildClassifier(trainCopy);

      // Find the attribute with maximum weight^2
      FastVector weightsAndIndices = smo.weights();
      double[] weightsSparse = (double[])weightsAndIndices.elementAt(0);
      int[] indicesSparse = (int[])weightsAndIndices.elementAt(1);
      double[] weights = new double[trainCopy.numAttributes()];
      for (int j = 0; j < weightsSparse.length; j++) {
	weights[indicesSparse[j]] = weightsSparse[j] * weightsSparse[j];
      }
      weights[trainCopy.classIndex()] = Double.MAX_VALUE;

      int minWeightIndex;
      int[] featArray = new int[numToElim];
      boolean [] eliminated = new boolean [origIndices.length];
      for (int j = 0; j <numToElim; j++) {
	minWeightIndex = Utils.minIndex(weights);
	m_attScores[origIndices[minWeightIndex]] = i+j+1;
	featArray[j] = minWeightIndex;
	eliminated[minWeightIndex] = true;
	weights[minWeightIndex] = Double.MAX_VALUE;
      }
      
      // Delete the best attribute. 
      AttributeFilter delTransform = new AttributeFilter();
      delTransform.setInvertSelection(false);

      delTransform.setAttributeIndicesArray(featArray);
      delTransform.setInputFormat(trainCopy);
      trainCopy = Filter.useFilter(trainCopy, delTransform);
      
      // Update the array of indices
      int[] temp = new int[origIndices.length - numToElim];
      int k = 0;
      for (int j = 0; j < origIndices.length; j++) {
	if (!eliminated[j]) {
	  temp[k++] = origIndices[j];
	}
      }

      origIndices = temp;
      i += numToElim;
    } while (i < m_attScores.length);
  }

  /**
   * Resets options to defaults.
   */
  protected void resetOptions () {

    m_attScores = null;
  }

  /**
   * Evaluates an attribute by returning the square of its coefficient in a
   * linear support vector machine.
   *
   * @param attribute the index of the attribute to be evaluated
   * @exception Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute (int attribute) throws Exception {

    return  m_attScores[attribute];
  }

  /**
   * Return a description of the evaluator
   * @return description as a string
   */
  public String toString () {

    StringBuffer text = new StringBuffer();

    if (m_attScores == null) {
      text.append("\tSVM feature evaluator has not been built yet");
    } else {
      text.append("\tSVM feature evaluator");
    }

    text.append("\n");
    return  text.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {

    try {
      System.out.println(AttributeSelection.
			 SelectAttributes(new SVMAttributeEval(), args));
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
}

