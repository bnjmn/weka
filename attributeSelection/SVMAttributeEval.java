
/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * SVMAttributeEval.java
 * Copyright (C) 2002 Eibe Frank
 * Mod by Kieran Holland
 * 
 */
package weka.attributeSelection;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.functions.SMO;
import weka.filters.Filter;
import weka.filters.AttributeFilter;
import weka.attributeSelection.*;

/**
 * Class for Evaluating attributes individually by using the SVM
 * classifier. <p>
 * 
 * Valid options are: <p>
 * 
 * -E <constant rate of elimination> <br>
 * Specify constant rate at which attributes are eliminated per invocation
 * of the support vector machine. Default = 1.<p>
 * 
 * -P <percent rate of elimination> <br>
 * Specify the percentage rate at which attributes are eliminated per invocation
 * of the support vector machine. This setting trumps the constant rate setting.
 * Default = 0 (percentage rate ignored).<p>
 * 
 * -T <threshold for percent elimination> <br>
 * Specify the threshold below which the percentage elimination method
 * reverts to the constant elimination method.<p>
 * 
 * -C <complexity parameter> <br>
 * Specify the value of C - the complexity parameter to be passed on
 * to the support vector machine. <p>
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.7 $
 */
public class SVMAttributeEval extends AttributeEvaluator 
  implements OptionHandler {

  /**
   * The attribute scores
   */
  private double[] m_attScores;

  /**
   * Constant rate of attribute elimination per iteration
   */
  private int      m_numToEliminate = 1;

  /**
   * Percentage rate of attribute elimination, trumps constant
   * rate (above threshold), ignored if = 0
   */
  private int      m_percentToEliminate = 0;

  /**
   * Threshold below which percent elimination switches to
   * constant elimination
   */
  private int      m_percentThreshold = 0;

  /**
   * Complexity parameter to pass on to SMO
   */
  private double   m_smoCParameter = 1.0;

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "SVMAttributeEval :\n\nEvaluates the worth of an attribute by " 
	   + "using an SVM classifier.\n";
  } 

  /**
   * Constructor
   */
  public SVMAttributeEval() {
    resetOptions();
  }

  /**
   * Returns an enumeration describing all the available options
   * 
   * @return an enumeration of options
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(4);

    newVector
      .addElement(new Option("\tSpecify the constant rate of attribute\n" 
			     + "\telimination per invocation of\n" 
			     + "\tthe support vector machine.\n" 
			     + "\tDefault = 1.", "E", 1, 
			     "-N <constant rate of elimination>"));
    newVector
      .addElement(new Option("\tSpecify the percentage rate of attributes to\n" 
			     + "\telimination per invocation of\n" 
			     + "\tthe support vector machine.\n" 
			     + "\tTrumps constant rate (above threshold).\n" 
			     + "\tDefault = 0.", "P", 1, 
			     "-P <percent rate of elimination>"));
    newVector
      .addElement(new Option("\tSpecify the threshold below which \n" 
			     + "\tpercentage attribute elimination\n" 
			     + "\treverts to the constant method.\n", "T", 1, 
			     "-T <threshold for percent elimination>"));
    newVector.addElement(new Option("\tSpecify the value of C (complexity\n" 
				    + "\tparameter) to pass on to the\n" 
				    + "\tsupport vector machine.\n" 
				    + "\tDefault = 1.0", "C", 1, 
				    "-C <complexity>"));

    return newVector.elements();
  } 

  /**
   * Parses a given list of options
   * 
   * Valid options are: <p>
   * 
   * -E <constant rate of elimination> <br>
   * Specify constant rate at which attributes are eliminated per invocation
   * of the support vector machine. Default = 1.<p>
   * 
   * -P <percent rate of elimination> <br>
   * Specify the percentage rate at which attributes are eliminated per invocation
   * of the support vector machine. This setting trumps the constant rate setting.
   * Default = 0 (percentage rate ignored).<p>
   * 
   * -T <threshold for percent elimination> <br>
   * Specify the threshold below which the percentage elimination method
   * reverts to the constant elimination method.<p>
   * 
   * -C <complexity parameter> <br>
   * Specify the value of C - the complexity parameter to be passed on
   * to the support vector machine. <p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an error occurs
   */
  public void setOptions(String[] options) throws Exception {
    String optionString;

    optionString = Utils.getOption('E', options);

    if (optionString.length() != 0) {
      setAttsToEliminatePerIteration(Integer.parseInt(optionString));
    } 

    optionString = Utils.getOption('P', options);

    if (optionString.length() != 0) {
      setPercentToEliminatePerIteration(Integer.parseInt(optionString));
    } 

    optionString = Utils.getOption('T', options);

    if (optionString.length() != 0) {
      setPercentThreshold(Integer.parseInt(optionString));
    } 

    optionString = Utils.getOption('C', options);

    if (optionString.length() != 0) {
      setComplexityParameter((new Double(optionString)).doubleValue());
    } 

    Utils.checkForRemainingOptions(options);
  } 

  /**
   * Gets the current settings of SVMAttributeEval
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions() {
    String[] options = new String[8];
    int      current = 0;

    options[current++] = "-E";
    options[current++] = "" + getAttsToEliminatePerIteration();
    options[current++] = "-P";
    options[current++] = "" + getPercentToEliminatePerIteration();
    options[current++] = "-T";
    options[current++] = "" + getPercentThreshold();
    options[current++] = "-C";
    options[current++] = "" + getComplexityParameter();

    while (current < options.length) {
      options[current++] = "";
    } 

    return options;
  } 

  // ________________________________________________________________________

  /**
   * Returns a tip text for this property suitable for display in the
   * GUI
   * 
   * @return tip text string describing this property
   */
  public String attsToEliminatePerIterationTipText() {
    return "Constant rate of attribute elimination.";
  } 

  /**
   * Returns a tip text for this property suitable for display in the
   * GUI
   * 
   * @return tip text string describing this property
   */
  public String percentToEliminatePerIterationTipText() {
    return "Percent rate of attribute elimination.";
  } 

  /**
   * Returns a tip text for this property suitable for display in the
   * GUI
   * 
   * @return tip text string describing this property
   */
  public String percentThresholdTipText() {
    return "Threshold below which percent elimination reverts to constant elimination.";
  } 

  /**
   * Returns a tip text for this property suitable for display in the
   * GUI
   * 
   * @return tip text string describing this property
   */
  public String complexityParameterTipText() {
    return "C complexity parameter to pass to the SVM";
  } 

  // ________________________________________________________________________

  /**
   * Set the constant rate of attribute elimination per iteration
   * 
   * @param cRate the constant rate of attribute elimination per iteration
   */
  public void setAttsToEliminatePerIteration(int cRate) {
    m_numToEliminate = cRate;
  } 

  /**
   * Get the constant rate of attribute elimination per iteration
   * 
   * @return the constant rate of attribute elimination per iteration
   */
  public int getAttsToEliminatePerIteration() {
    return m_numToEliminate;
  } 

  /**
   * Set the percentage of attributes to eliminate per iteration
   * 
   * @param pRate percent of attributes to eliminate per iteration
   */
  public void setPercentToEliminatePerIteration(int pRate) {
    m_percentToEliminate = pRate;
  } 

  /**
   * Get the percentage rate of attribute elimination per iteration
   * 
   * @return the percentage rate of attribute elimination per iteration
   */
  public int getPercentToEliminatePerIteration() {
    return m_percentToEliminate;
  } 

  /**
   * Set the threshold below which percentage elimination reverts to
   * constant elimination.
   * 
   * @param thresh percent of attributes to eliminate per iteration
   */
  public void setPercentThreshold(int thresh) {
    m_percentThreshold = thresh;
  } 

  /**
   * Get the threshold below which percentage elimination reverts to
   * constant elimination.
   * 
   * @return the threshold below which percentage elimination stops
   */
  public int getPercentThreshold() {
    return m_percentThreshold;
  } 

  /**
   * Set the value of C for SMO
   * 
   * @param svmC the value of C
   */
  public void setComplexityParameter(double svmC) {
    m_smoCParameter = svmC;
  } 

  /**
   * Get the value of C used with SMO
   * 
   * @return the value of C
   */
  public double getComplexityParameter() {
    return m_smoCParameter;
  } 

  // ________________________________________________________________________

  /**
   * Initializes the evaluator.
   * 
   * @param data set of instances serving as training data
   * @exception Exception if the evaluator has not been
   * generated successfully
   */
  public void buildEvaluator(Instances data) throws Exception {
    if (data.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Can't handle string attributes!");
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
      if (data.attribute(i).isNominal() 
	      && (data.attribute(i).numValues() != 2)) {
	throw new Exception("All nominal attributes must be binary!");
      } 

      origIndices[i] = i;
    } 

    // We need to repeat the following loop until we've computed
    // a weight for every attribute (excluding the class)
    m_attScores = new double[data.numAttributes()];

    Instances trainCopy = new Instances(data);

    m_numToEliminate = (m_numToEliminate > 1) ? m_numToEliminate : 1;
    m_percentToEliminate = (m_percentToEliminate < 100) 
			   ? m_percentToEliminate : 100;
    m_percentToEliminate = (m_percentToEliminate > 0) ? m_percentToEliminate 
			   : 0;
    m_percentThreshold = (m_percentThreshold < m_attScores.length) 
			 ? m_percentThreshold : m_attScores.length - 1;
    m_percentThreshold = (m_percentThreshold > 0) ? m_percentThreshold : 0;

    int    i = 0;
    double pctToElim = ((double) m_percentToEliminate) / 100.0;

    while (trainCopy.numAttributes() > 1) {
      int numToElim;

      if (pctToElim > 0) {
	numToElim = (int) (trainCopy.numAttributes() * pctToElim);
	numToElim = (numToElim > 1) ? numToElim : 1;

	if (m_attScores.length - i - numToElim <= m_percentThreshold) {
	  pctToElim = 0;
	  numToElim = m_attScores.length - i - m_percentThreshold;
	} 
      } else {
	numToElim = (m_attScores.length - i - 1 >= m_numToEliminate) 
		    ? m_numToEliminate : m_attScores.length - i - 1;
      } 

      // System.out.println("Progress: " + trainCopy.numAttributes());
      // Build the linear SVM with default parameters
      SMO smo = new SMO();

      smo.setC(m_smoCParameter);
      smo.buildClassifier(trainCopy);

      // Find the attribute with maximum weight^2
      FastVector weightsAndIndices = smo.weights();
      double[]   weightsSparse = (double[]) weightsAndIndices.elementAt(0);
      int[]      indicesSparse = (int[]) weightsAndIndices.elementAt(1);
      double[]   weights = new double[trainCopy.numAttributes()];

      for (int j = 0; j < weightsSparse.length; j++) {
	weights[indicesSparse[j]] = weightsSparse[j] * weightsSparse[j];
      } 

      weights[trainCopy.classIndex()] = Double.MAX_VALUE;

      int       minWeightIndex;
      int[]     featArray = new int[numToElim];
      boolean[] eliminated = new boolean[origIndices.length];

      for (int j = 0; j < numToElim; j++) {
	minWeightIndex = Utils.minIndex(weights);
	m_attScores[origIndices[minWeightIndex]] = i + j + 1;
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
      int   k = 0;

      for (int j = 0; j < origIndices.length; j++) {
	if (!eliminated[j]) {
	  temp[k++] = origIndices[j];
	} 
      } 

      origIndices = temp;
      i += numToElim;
    } 
  } 

  /**
   * Resets options to defaults.
   */
  protected void resetOptions() {
    m_attScores = null;
  } 

  /**
   * Evaluates an attribute by returning the square of its coefficient in a
   * linear support vector machine.
   * 
   * @param attribute the index of the attribute to be evaluated
   * @exception Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute(int attribute) throws Exception {
    return m_attScores[attribute];
  } 

  /**
   * Return a description of the evaluator
   * @return description as a string
   */
  public String toString() {
    StringBuffer text = new StringBuffer();

    if (m_attScores == null) {
      text.append("\tSVM feature evaluator has not been built yet");
    } else {
      text.append("\tSVM feature evaluator");
    } 

    text.append("\n");

    return text.toString();
  } 

  /**
   * Main method for testing this class.
   * 
   * @param args the options
   */
  public static void main(String[] args) {
    try {
      File		 arff = new File("d:\\weka331\\data\\golub.arff");
      BufferedReader     br = new BufferedReader(new FileReader(arff));
      Instances		 test = new Instances(br);
      AttributeSelection as = new AttributeSelection();
      SVMAttributeEval   svm = new SVMAttributeEval();

      svm.setAttsToEliminatePerIteration(1);
      svm.setPercentThreshold(100);
      svm.setPercentToEliminatePerIteration(10);
      test.setClassIndex(0);
      as.setEvaluator(svm);
      as.setSearch(new Ranker());
      as.SelectAttributes(test);
      System.out.println(as.toResultsString());
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    } 
  } 

}




