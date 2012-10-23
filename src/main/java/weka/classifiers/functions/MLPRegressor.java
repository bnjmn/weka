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
 *    MLPRegressor.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.functions;

import weka.classifiers.RandomizableClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.Evaluation;

import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.Option;
import weka.core.Optimization;
import weka.core.ConjugateGradientOptimization;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;

import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.RemoveUseless;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.Filter;

import java.util.Random;
import java.util.Vector;
import java.util.Enumeration;

/**
 <!-- globalinfo-start -->
 * Trains a multilayer perceptron with one hidden layer using WEKA's Optimization class by minimizing the squared error plus a quadratic penalty with the BFGS method. Note that all attributes are standardized, including the target. There are several parameters. The ridge parameter is used to determine the penalty on the size of the weights. The number of hidden units can also be specified. Note that large numbers produce long training times.Finally, it is possible to use conjugate gradient descent rather than BFGS updates, which may be faster for cases with many parameters. Nominal attributes are processed using the unsupervised  NominalToBinary filter and missing values are replaced globally using ReplaceMissingValues.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N
 *  Number of hidden units (default is 2).
 * </pre>
 * 
 * <pre> -R
 *  Ridge factor for quadratic penalty on weights (default is 0.01).
 * </pre>
 * 
 * <pre> -G
 *  Use conjugate gradient descent (recommended for many attributes).
 * </pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class MLPRegressor extends RandomizableClassifier {
  
  /** For serialization */
  private static final long serialVersionUID = -3377474276438394655L;

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }
  
  /**
   * Simple wrapper class needed to use the BFGS method
   * implemented in weka.core.Optimization.
   */
  protected class OptEng extends Optimization {

    /**
     * Returns the squared error given parameter values x.
     */
    protected double objectiveFunction(double[] x){
      
      m_MLPParameters = x;
      return calculateSE();
    }

    /**
     * Returns the gradient given parameter values x.
     */
    protected double[] evaluateGradient(double[] x){
      
      m_MLPParameters = x;
      return calculateGradient();
    }

    /**
     * The revision string.
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision$");
    }
  }
  
  /**
   * Simple wrapper class needed to use the CGD method
   * implemented in weka.core.ConjugateGradientOptimization.
   */
  protected class OptEngCGD extends ConjugateGradientOptimization {

    /**
     * Returns the squared error given parameter values x.
     */
    protected double objectiveFunction(double[] x){
      
      m_MLPParameters = x;
      return calculateSE();
    }

    /**
     * Returns the gradient given parameter values x.
     */
    protected double[] evaluateGradient(double[] x){
      
      m_MLPParameters = x;
      return calculateGradient();
    }

    /**
     * The revision string.
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision$");
    }
  }

  // The number of hidden units
  protected int m_numUnits = 2;
  
  // The class index of the dataset
  protected int m_classIndex = -1;

  // A reference to the actual data
  protected Instances m_data = null;

  // The number of attributes in the data
  protected int m_numAttributes = -1;

  // The parameter vector
  protected double[] m_MLPParameters = null;

  // Offset for output unit parameters
  protected int OFFSET_WEIGHTS = -1;

  // Offset for parameters of hidden units
  protected int OFFSET_ATTRIBUTE_WEIGHTS = -1;

  // Two values need to convert target/class values back into original scale
  protected double m_x1 = 1.0;
  protected double m_x0 = 0.0;

  // The ridge parameter
  protected double m_ridge = 0.01;

  // Whether to use conjugate gradient descent rather than BFGS updates
  protected boolean m_useCGD = false;

  // The standardization filer
  protected Filter m_Filter = null;

  // An attribute filter
  protected RemoveUseless m_AttFilter;
    
  // The filter used to make attributes numeric.
  protected NominalToBinary m_NominalToBinary;
    
  // The filter used to get rid of missing values.
  protected ReplaceMissingValues m_ReplaceMissingValues;

  // a ZeroR model in case no model can be built from the data
  private Classifier m_ZeroR;

  /**
   * Method used to pre-process the data, perform clustering, and
   * set the initial parameter vector.
   */
  protected Instances initializeClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    data = new Instances(data);
    data.deleteWithMissingClass();

    // Make sure data is shuffled
    Random random = new Random(m_Seed);
    if (data.numInstances() > 1) {
      random = data.getRandomNumberGenerator(m_Seed); 
    }
    data.randomize(random);
    
    double y0 = data.instance(0).classValue();
    int index = 1;
    while (index < data.numInstances() && data.instance(index).classValue() == y0) {
      index++;
    }
    if (index == data.numInstances()) {
      // degenerate case, all class values are equal
      // we don't want to deal with this, too much hassle
      throw new Exception("All class values are the same. At least two class values should be different");
    }
    double y1 = data.instance(index).classValue();

    // Replace missing values	
    m_ReplaceMissingValues = new ReplaceMissingValues();
    m_ReplaceMissingValues.setInputFormat(data);
    data = Filter.useFilter(data, m_ReplaceMissingValues);

    // Remove useless attributes
    m_AttFilter = new RemoveUseless();
    m_AttFilter.setInputFormat(data);
    data = Filter.useFilter(data, m_AttFilter);

    // only class? -> build ZeroR model
    if (data.numAttributes() == 1) {
      System.err.println(
	  "Cannot build model (only class attribute present in data after removing useless attributes!), "
	  + "using ZeroR model instead!");
      m_ZeroR = new ZeroR();
      m_ZeroR.buildClassifier(data);
      return null;
    }
    else {
      m_ZeroR = null;
    }

    // Transform nominal attributes
    m_NominalToBinary = new NominalToBinary();
    m_NominalToBinary.setInputFormat(data);
    data = Filter.useFilter(data, m_NominalToBinary);
    
    // Standardize data
    m_Filter = new Standardize();
    ((Standardize)m_Filter).setIgnoreClass(true);
    m_Filter.setInputFormat(data);
    data = Filter.useFilter(data, m_Filter); 
    double z0 = data.instance(0).classValue();
    double z1 = data.instance(index).classValue();
    m_x1 = (y0 - y1) / (z0 - z1); // no division by zero, since y0 != y1 guaranteed => z0 != z1 ???
    m_x0 = (y0 - m_x1 * z0); // = y1 - m_x1 * z1
    
    m_classIndex = data.classIndex();
    m_numAttributes = data.numAttributes();

    // Set up array
    OFFSET_WEIGHTS = 0;
    OFFSET_ATTRIBUTE_WEIGHTS = (m_numUnits + 1);
    m_MLPParameters = new double[OFFSET_ATTRIBUTE_WEIGHTS + m_numUnits * m_numAttributes];
    
    // Initialize parameters
    int offsetOW = OFFSET_WEIGHTS;
    for (int i = 0; i < m_numUnits; i++) {
      m_MLPParameters[offsetOW + i] = 0.1 * random.nextGaussian();
    }
    m_MLPParameters[offsetOW + m_numUnits] = 0.1 * random.nextGaussian();
    for (int i = 0; i < m_numUnits; i++) {
      int offsetW = OFFSET_ATTRIBUTE_WEIGHTS + (i * m_numAttributes);
      for (int j = 0; j < m_numAttributes; j++) {
        m_MLPParameters[offsetW + j] = 0.1 * random.nextGaussian();
      }
    }
    
    return data;
  }

  /**
   * Builds the MLP network classifier based on the given dataset.
   */
  public void buildClassifier(Instances data) throws Exception {

    // Set up the initial arrays
    m_data = initializeClassifier(data);
    if (m_data == null) {
      return;
    }

    // Apply optimization class to train the network
    Optimization opt = null;
    if (!m_useCGD) {
      opt = new OptEng();
    } else {
      opt = new OptEngCGD();
    }
    opt.setDebug(m_Debug);

    // No constraints
    double[][] b = new double[2][m_MLPParameters.length];
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < m_MLPParameters.length; j++) {
        b[i][j] = Double.NaN;
      }
    }

    m_MLPParameters = opt.findArgmin(m_MLPParameters, b);
    while(m_MLPParameters == null){
      m_MLPParameters = opt.getVarbValues();
      if (m_Debug) {
        System.out.println("First set of iterations finished, not enough!");
      }
      m_MLPParameters = opt.findArgmin(m_MLPParameters, b);
    }
    if (m_Debug) {	
      System.out.println("SE (normalized space) after optimization: " + opt.getMinFunction()); 
    }
    
    m_data = new Instances(m_data, 0); // Save memory
  } 

  /**
   * Calculates the (penalized) squared error based on the current parameter vector.
   */
  protected double calculateSE() {

    double SE = 0;

    // Outputs from hidden units
    double[] outputs = new double[m_numUnits];

    // For each instance
    for (int k = 0; k < m_data.numInstances(); k++) {
      Instance inst = m_data.instance(k);

      // Calculate necessary input/output values and error term
      calculateOutputs(inst, outputs);
      int cv = (int)inst.value(m_classIndex);
      double err = getOutput(outputs) - inst.classValue();
      SE += err * err;
    }

    // Calculate sum of squared weights, excluding bias
    double squaredSumOfWeights = 0;
    int offsetOW = OFFSET_WEIGHTS;
    for (int k = 0; k < m_numUnits; k++) {
      squaredSumOfWeights += m_MLPParameters[offsetOW + k] * m_MLPParameters[offsetOW + k];
    }
    for (int k = 0; k < m_numUnits; k++) {
      int offsetW = OFFSET_ATTRIBUTE_WEIGHTS + k * m_numAttributes;
      for (int j = 0; j < m_classIndex; j++) {
        squaredSumOfWeights += m_MLPParameters[offsetW + j] * m_MLPParameters[offsetW + j];
      }
      for (int j = m_classIndex + 1; j < m_numAttributes; j++) {
        squaredSumOfWeights += m_MLPParameters[offsetW + j] * m_MLPParameters[offsetW + j];
      }
    }
    
    return ((m_ridge * squaredSumOfWeights) + (0.5 * SE)) / m_data.numInstances();
  }

  /**
   * Calculates the gradient based on the current parameter vector.
   */
  protected double[] calculateGradient() {

    // Need to put gradient into a one-dimensional array
    double[] grad = new double[m_MLPParameters.length];

    // Outputs from hidden units
    double[] outputs = new double[m_numUnits];

    // For each instance
    for (int k = 0; k < m_data.numInstances(); k++) {
      Instance inst = m_data.instance(k);

      // Calculate necessary input/output values and error term
      calculateOutputs(inst, outputs);
      double cv = inst.classValue();
      double pred = getOutput(outputs);
      updateGradient(grad, inst, outputs, (pred - cv));
    }

    // For all network weights, perform weight decay
    int offsetOW = OFFSET_WEIGHTS;
    for (int k = 0; k < m_numUnits; k++) {
      grad[offsetOW + k] += m_ridge * 2 * m_MLPParameters[offsetOW + k];
    }
    for (int k = 0; k < m_numUnits; k++) {
      int offsetW = OFFSET_ATTRIBUTE_WEIGHTS + k * m_numAttributes;
      for (int j = 0; j < m_classIndex; j++) {
        grad[offsetW + j] += m_ridge * 2 * m_MLPParameters[offsetW + j];
      }
      for (int j = m_classIndex + 1; j < m_numAttributes; j++) {
        grad[offsetW + j] += m_ridge * 2 * m_MLPParameters[offsetW + j];
      }
    }

    double factor = 1.0 / m_data.numInstances();
    for (int i = 0; i < grad.length; i++) {
      grad[i] *= factor;
    }

    return grad;
  }

  /**
   * Update the gradient for the weights in the output layer.
   */
  protected void updateGradient(double[] grad, Instance inst, double[] outputs, double deltaOut) {

    // For output unit j
    int offsetOW = OFFSET_WEIGHTS;

    // Update gradient for output weights
    for (int i = 0; i < m_numUnits; i++) {
      grad[offsetOW + i] += deltaOut * outputs[i];
    }
    
    // Update gradient for bias
    grad[offsetOW + m_numUnits] += deltaOut;

    // Update gradient for hidden units
    for (int i = 0; i < m_numUnits; i++) {
      updateGradientForHiddenUnits(grad, inst, deltaOut * m_MLPParameters[offsetOW + i] * 
                                   (outputs[i] * (1.0 - outputs[i])), i);
    }
  }

  /**
   * Update the gradient for the weights in the hidden layer.
   */
  protected void updateGradientForHiddenUnits(double[] grad, Instance inst, double deltaHidden, int i) {
      
    // For hidden unit i
    int offsetW = OFFSET_ATTRIBUTE_WEIGHTS + i * m_numAttributes;

    // Update gradient for all weights, including bias at classIndex
    for (int l = 0; l < m_classIndex; l++) {
      grad[offsetW + l] += deltaHidden * inst.value(l);
    }
    grad[offsetW + m_classIndex] += deltaHidden;
    for (int l = m_classIndex + 1; l < m_numAttributes; l++) {
      grad[offsetW + l] += deltaHidden * inst.value(l);
    }
  }

  /**
   * Calculates the array of outputs of the hidden units.
   */
  protected void calculateOutputs(Instance inst, double[] o) {

    for (int i = 0; i < m_numUnits; i++) {
      int offsetW = OFFSET_ATTRIBUTE_WEIGHTS + i * m_numAttributes;
      double sum = 0;
      for (int j = 0; j < m_classIndex; j++) {
        sum += inst.value(j) * m_MLPParameters[offsetW + j];
      }
      sum += m_MLPParameters[offsetW + m_classIndex]; 
      for (int j = m_classIndex + 1; j < m_numAttributes; j++) {
        sum += inst.value(j) * m_MLPParameters[offsetW + j];
      }
      o[i] = 1.0 / (1.0 + Math.exp(-sum));
    }
  }
  
  /**
   * Calculates the output of output unit based on the given 
   * hidden layer outputs.
   */
  protected double getOutput(double[] outputs) {

    int offsetOW = OFFSET_WEIGHTS;
    double result = 0;
    for (int i = 0; i < m_numUnits; i++) {
      result +=  m_MLPParameters[offsetOW + i] * outputs[i];
    }
    result += m_MLPParameters[offsetOW + m_numUnits];
    return result;
  }    
  
  /**
   * Calculates the output of the network after the instance has been
   * piped through the fliters to replace missing values, etc.
   */
  public double classifyInstance(Instance inst) throws Exception {
      
    m_ReplaceMissingValues.input(inst);
    inst = m_ReplaceMissingValues.output();
    m_AttFilter.input(inst);
    inst = m_AttFilter.output();

    // default model?
    if (m_ZeroR != null) {
      return m_ZeroR.classifyInstance(inst);
    }

    m_NominalToBinary.input(inst);
    inst = m_NominalToBinary.output();
    m_Filter.input(inst);
    inst = m_Filter.output();

    double[] outputs = new double[m_numUnits];
    calculateOutputs(inst, outputs);
    return getOutput(outputs) * m_x1 + m_x0;
  }

  /**
   * This will return a string describing the classifier.
   * @return The string.
   */
  public String globalInfo() {

    return 
      "Trains a multilayer perceptron with one hidden layer using WEKA's Optimization class"
      + " by minimizing the squared error plus a quadratic penalty with the BFGS method."
      + " Note that all attributes are standardized, including the target. There are several parameters. The"
      + " ridge parameter is used to determine the penalty on the size of the weights. The"
      + " number of hidden units can also be specified. Note that large"
      + " numbers produce long training times.Finally, it is possible to use conjugate gradient"
      + " descent rather than BFGS updates, which may be faster for cases with many parameters."
      + " Nominal attributes are processed using the unsupervised "
      + " NominalToBinary filter and missing values are replaced globally"
      + " using ReplaceMissingValues.";
  }
  
  /**
   * @return a string to describe the option
   */
  public String numFunctionsTipText() {

    return "The number of hidden units to use.";
  }

  /**
   * Gets the number of functions.
   */
  public int getNumFunctions() {

    return m_numUnits;
  }
  
  /**
   * Sets the number of functions.
   */
  public void setNumFunctions(int newNumFunctions) {

    m_numUnits = newNumFunctions;
  }
  
  /**
   * @return a string to describe the option
   */
  public String ridgeTipText() {

    return "The ridge penalty factor for the quadratic penalty on the weights.";
  }

  /**
   * Gets the value of the ridge parameter.
   */
  public double getRidge() {

    return m_ridge;
  }
  
  /**
   * Sets the value of the ridge parameter.
   */
  public void setRidge(double newRidge) {

    m_ridge = newRidge;
  }
  
  /**
   * @return a string to describe the option
   */
  public String useCGDTipText() {

    return "Whether to use conjugate gradient descent (potentially useful for many parameters).";
  }

  /**
   * Gets whether to use CGD.
   */
  public boolean getUseCGD() {

    return m_useCGD;
  }
  
  /**
   * Sets whether to use CGD.
   */
  public void setUseCGD(boolean newUseCGD) {

    m_useCGD = newUseCGD;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector<Option> newVector = new Vector<Option>(3);

    newVector.addElement(new Option(
                                    "\tNumber of hidden units (default is 2).\n", 
                                    "N", 1, "-N"));

    newVector.addElement(new Option(
                                    "\tRidge factor for quadratic penalty on weights (default is 0.01).\n", 
                                    "R", 1, "-R"));
    newVector.addElement(new Option(
                                    "\tUse conjugate gradient descent (recommended for many attributes).\n", 
                                    "G", 0, "-G"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement((Option)enu.nextElement());
    }
    return newVector.elements();
  }


  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -N
   *  Number of hidden units (default is 2).
   * </pre>
   * 
   * <pre> -R
   *  Ridge factor for quadratic penalty on weights (default is 0.01).
   * </pre>
   * 
   * <pre> -G
   *  Use conjugate gradient descent (recommended for many attributes).
   * </pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   <!-- options-end -->
   *
   * Options after -- are passed to the designated classifier.<p>
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String numFunctions = Utils.getOption('N', options);
    if (numFunctions.length() != 0) {
      setNumFunctions(Integer.parseInt(numFunctions));
    } else {
      setNumFunctions(2);
    }
    String Ridge = Utils.getOption('R', options);
    if (Ridge.length() != 0) {
      setRidge(Double.parseDouble(Ridge));
    } else {
      setRidge(0.01);
    }
    m_useCGD = Utils.getFlag('G', options);

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {


    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 5];

    int current = 0;
    options[current++] = "-N"; 
    options[current++] = "" + getNumFunctions();

    options[current++] = "-R"; 
    options[current++] = "" + getRidge();

    if (m_useCGD) {
      options[current++] = "-G";
    }

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);

    current += superOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Outputs the network as a string.
   */
  public String toString() {

    if (m_ZeroR != null) {
      return m_ZeroR.toString();
    }

    if (m_MLPParameters == null) {
      return "Classifier not built yet.";
    }

    String s = "MLPRegressor with ridge value " + getRidge() +
      " and " + getNumFunctions() + " hidden units (useCGD=" + 
      getUseCGD() + ")\n\n";
    
    for (int i = 0; i < m_numUnits; i++) {
      s += "Output unit weight for hidden unit " + i + ": " + 
        m_MLPParameters[OFFSET_WEIGHTS + i] + "\n";
      s += "\nHidden unit weights:\n\n";
      for (int j = 0; j < m_numAttributes; j++) {
        if (j != m_classIndex) {
          s += m_MLPParameters[OFFSET_ATTRIBUTE_WEIGHTS + 
                               (i * m_numAttributes) + j] 
            + " " + m_data.attribute(j).name() + "\n";
        }
      }
      s += "\nHidden unit bias: " + 
        m_MLPParameters[OFFSET_ATTRIBUTE_WEIGHTS + 
                        (i * m_numAttributes + m_classIndex)] + "\n\n";
    }  
    s += "Output unit bias: " + m_MLPParameters[OFFSET_WEIGHTS + 
                                                m_numUnits] + "\n";

    return s;
  }

  /**
   * Main method to run the code from the command-line using
   * the standard WEKA options.
   */
  public static void main(String[] argv) {

    runClassifier(new MLPRegressor(), argv);
  }
}

