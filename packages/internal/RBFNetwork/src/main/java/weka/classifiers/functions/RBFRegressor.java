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
 *    RBFRegressor.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.functions;

import weka.classifiers.RandomizableClassifier;

import weka.clusterers.SimpleKMeans;

import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.Option;
import weka.core.Matrix;
import weka.core.Optimization;
import weka.core.ConjugateGradientOptimization;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;


import weka.filters.unsupervised.attribute.Normalize;
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
 * Class implementing radial basis function networks for regression, trained in a fully supervised manner using WEKA's Optimization class by minimizing squared error with the BFGS method. Note that all attributes are normalized into the [0,1] scale, including the class. The initial centers for the Gaussian radial basis functions are found using WEKA's SimpleKMeans. The initial sigma values are set to the maximum distance between any center and its nearest neighbour in the set of centers. There are several parameters. The ridge parameter is used to penalize the size of the weights in the output layer, which implements a simple linear combination. The number of basis functions can also be specified. Note that large numbers produce long training times. Another option determines whether one global sigma value is used for all units (fastest), whether one value is used per unit (common practice, it seems, and set as the default), or a different value is learned for every unit/attribute combination. It is also possible to learn attribute weights for the distance function. (The square of the value shown in the output is used.)  Finally, it is possible to use conjugate gradient descent rather than BFGS updates, which is faster for cases with many parameters, and to use normalized basis functions instead of unnormalized ones. Nominal attributes are processed using the unsupervised  NominalToBinary filter and missing values are replaced globally using ReplaceMissingValues.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N
 *  Number of Gaussian basis functions (default is 2).
 * </pre>
 * 
 * <pre> -R
 *  Ridge factor for quadratic penalty on output weights (default is 0.01).
 * </pre>
 * 
 * <pre> -C
 *  The scale optimization option: global scale (1), one scale per unit (2), scale per unit and attribute (3) (default is 2).
 * </pre>
 * 
 * <pre> -G
 *  Use conjugate gradient descent (recommended for many attributes).
 * </pre>
 * 
 * <pre> -O
 *  Use normalized basis functions.
 * </pre>
 * 
 * <pre> -A
 *  Use attribute weights.
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
public class RBFRegressor extends RandomizableClassifier {
  
  /** For serialization */
  private static final long serialVersionUID = -4847474276438394611L;

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
    result.enable(Capability.DATE_CLASS);
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
      
      m_RBFParameters = x;
      return calculateSE();
    }

    /**
     * Returns the gradient given parameter values x.
     */
    protected double[] evaluateGradient(double[] x){
      
      m_RBFParameters = x;
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
      
      m_RBFParameters = x;
      return calculateSE();
    }

    /**
     * Returns the gradient given parameter values x.
     */
    protected double[] evaluateGradient(double[] x){
      
      m_RBFParameters = x;
      return calculateGradient();
    }

    /**
     * The revision string.
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision$");
    }
  }

  /** Constants for scale optimization options */
  public static final int USE_GLOBAL_SCALE = 1;
  public static final int USE_SCALE_PER_UNIT = 2;
  public static final int USE_SCALE_PER_UNIT_AND_ATTRIBUTE = 3;
  public static final Tag [] TAGS_SCALE = {
    new Tag(USE_GLOBAL_SCALE, "Use global scale"),
    new Tag(USE_SCALE_PER_UNIT, "Use scale per unit"),
    new Tag(USE_SCALE_PER_UNIT_AND_ATTRIBUTE, "Use scale per unit and attribute")
  };
  
  // The chosen scale optimization option
  protected int m_scaleOptimizationOption = USE_SCALE_PER_UNIT;

  // The number of units
  protected int m_numUnits = 2;
  
  // The class index of the dataset
  protected int m_classIndex = -1;

  // A reference to the actual data
  protected Instances m_data = null;

  // The parameter vector
  protected double[] m_RBFParameters = null;

  // The ridge parameter
  protected double m_ridge = 0.01;

  // Whether to use conjugate gradient descent rather than BFGS updates
  protected boolean m_useCGD = false;

  // Whether to use normalized basis functions
  protected boolean m_useNormalizedBasisFunctions = false;

  // Whether to use attribute weights
  protected boolean m_useAttributeWeights = false;

  // The normalization filer
  protected Filter m_Filter = null;

  // Two values need to convert target/class values back into original scale
  protected double m_x1 = 1.0;
  protected double m_x0 = 0.0;

  // Whether the network has been trained
  protected boolean m_trained = false;

  // The offsets for the different components in the parameter vector
  protected int OFFSET_WEIGHTS = -1; 
  protected int OFFSET_SCALES = -1;
  protected int OFFSET_CENTERS = -1;
  protected int OFFSET_ATTRIBUTE_WEIGHTS = -1;
    
  /** An attribute filter */
  private RemoveUseless m_AttFilter;
    
  /** The filter used to make attributes numeric. */
  private NominalToBinary m_NominalToBinary;
    
  /** The filter used to get rid of missing values. */
  private ReplaceMissingValues m_ReplaceMissingValues;

  /** a ZeroR model in case no model can be built from the data */
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
    if (data.numInstances() > 2) {
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
      m_ZeroR = new weka.classifiers.rules.ZeroR();
      m_ZeroR.buildClassifier(data);
      return data;
    }
    else {
      m_ZeroR = null;
    }

    // Transform attributes
    m_NominalToBinary = new NominalToBinary();
    m_NominalToBinary.setInputFormat(data);
    data = Filter.useFilter(data, m_NominalToBinary);
    
    m_Filter = new Normalize();
    ((Normalize)m_Filter).setIgnoreClass(true);
    m_Filter.setInputFormat(data);
    data = Filter.useFilter(data, m_Filter); 
    double z0 = data.instance(0).classValue();
    double z1 = data.instance(index).classValue();
    m_x1 = (y0 - y1) / (z0 - z1); // no division by zero, since y0 != y1 guaranteed => z0 != z1 ???
    m_x0 = (y0 - m_x1 * z0); // = y1 - m_x1 * z1
    
    m_classIndex = data.classIndex();

    // Run k-means
    SimpleKMeans skm = new SimpleKMeans();
    skm.setMaxIterations(10000);
    skm.setNumClusters(m_numUnits);
    Remove rm = new Remove();
    data.setClassIndex(-1);
    rm.setAttributeIndices((m_classIndex + 1) + "");
    rm.setInputFormat(data);
    Instances dataRemoved = Filter.useFilter(data, rm);
    data.setClassIndex(m_classIndex);
    skm.buildClusterer(dataRemoved);
    Instances centers = skm.getClusterCentroids();

    if (centers.numInstances() < m_numUnits) {
      m_numUnits = centers.numInstances();
    }
    
    // Set up arrays
    OFFSET_WEIGHTS = 0;
    if (m_useAttributeWeights) {
      OFFSET_ATTRIBUTE_WEIGHTS = m_numUnits + 1;
      OFFSET_CENTERS = OFFSET_ATTRIBUTE_WEIGHTS + data.numAttributes();
    } else {
      OFFSET_ATTRIBUTE_WEIGHTS = -1;
      OFFSET_CENTERS = m_numUnits + 1;
    }
    OFFSET_SCALES = OFFSET_CENTERS  + m_numUnits * data.numAttributes();

    switch (m_scaleOptimizationOption) {
    case USE_GLOBAL_SCALE: 
      m_RBFParameters = new double[OFFSET_SCALES + 1];
      break;
    case USE_SCALE_PER_UNIT_AND_ATTRIBUTE:
      m_RBFParameters = new double[OFFSET_SCALES + m_numUnits * data.numAttributes()];
      break;
    default:
      m_RBFParameters = new double[OFFSET_SCALES + m_numUnits];
      break;
    }

    // Set initial radius based on distance to nearest other basis function
    double maxMinDist = -1;
    for (int i = 0; i < centers.numInstances(); i++) {
      double minDist = Double.MAX_VALUE;
      for (int j = i + 1; j < centers.numInstances(); j++) {
        double dist = 0;
        for (int k = 0; k < centers.numAttributes(); k++) {
          if (k != centers.classIndex()) {
            double diff = centers.instance(i).value(k) - centers.instance(j).value(k);
            dist += diff * diff;
          }
        }
        if (dist < minDist) {
          minDist = dist;
        }
      }
      if ((minDist != Double.MAX_VALUE) && (minDist > maxMinDist)) {
        maxMinDist = minDist;
      }
    }
   
    // Initialize parameters
    if (m_scaleOptimizationOption == USE_GLOBAL_SCALE) {
      m_RBFParameters[OFFSET_SCALES] = Math.sqrt(maxMinDist);
    }
    for (int i = 0; i < m_numUnits; i++) {
      if (m_scaleOptimizationOption == USE_SCALE_PER_UNIT) {
        m_RBFParameters[OFFSET_SCALES + i] = Math.sqrt(maxMinDist);
      }
      m_RBFParameters[OFFSET_WEIGHTS + i] = (random.nextDouble() - 0.5) / 2;
      int k = 0;
      for (int j = 0; j < data.numAttributes(); j++) {
        if (k == centers.classIndex()) {
          k++;
        }
        if (j != data.classIndex()) {
          if (m_scaleOptimizationOption == USE_SCALE_PER_UNIT_AND_ATTRIBUTE) {
            m_RBFParameters[OFFSET_SCALES + (i * data.numAttributes() + j)] = Math.sqrt(maxMinDist);
          }
          m_RBFParameters[OFFSET_CENTERS + (i * data.numAttributes()) + j] = centers.instance(i).value(k);
          k++;
        }
      }
    }

    if (m_useAttributeWeights) {
      for (int j = 0; j < data.numAttributes(); j++) {
        if (j != data.classIndex()) {
          m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] = 1.0;
        }
      }
    }

    m_RBFParameters[OFFSET_WEIGHTS + m_numUnits] = (random.nextDouble() - 0.5) / 2 + 0.5;
    
    return data;
  }

  /**
   * Builds the RBF network regressor based on the given dataset.
   */
  public void buildClassifier(Instances data) throws Exception {

    m_trained = false;

    // Set up the initial arrays
    m_data = initializeClassifier(data);

    if (m_ZeroR != null) {
      m_trained = true;
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
    double[][] b = new double[2][m_RBFParameters.length];
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < m_RBFParameters.length; j++) {
        b[i][j] = Double.NaN;
      }
    }

    m_RBFParameters = opt.findArgmin(m_RBFParameters, b);
    while(m_RBFParameters == null){
      m_RBFParameters = opt.getVarbValues();
      if (m_Debug) {
        System.out.println("200 iterations finished, not enough!");
      }
      m_RBFParameters = opt.findArgmin(m_RBFParameters, b);
    }
    if (m_Debug) {	
      System.out.println("SE (normalized space) after optimization: " + opt.getMinFunction()); 
    }
    
    m_data = new Instances(m_data, 0); // Save memory
    m_trained = true;
  } 

  /**
   * Calculates the (penalized) squared error based on the current parameter vector.
   */
  protected double calculateSE() {

    double SE = 0;

    // For each instance
    for (int k = 0; k < m_data.numInstances(); k++) {
      Instance inst = m_data.instance(k);

      // Calculate necessary input/output values and error term
      double err = getOutput(inst) - inst.classValue();
      SE += err * err;
    }

    // Calculate squared sum of weights
    double squaredSumOfWeights = 0;
    for (int k = 0; k < m_numUnits; k++) {
      squaredSumOfWeights += m_RBFParameters[OFFSET_WEIGHTS + k] *  m_RBFParameters[OFFSET_WEIGHTS + k];
    }

    return (m_ridge * squaredSumOfWeights) + (0.5 * SE);
  }

  /**
   * Calculates the gradient based on the current parameter vector.
   */
  protected double[] calculateGradient() {

    // Need to put gradient into a one-dimensional array
    double[] grad = new double[m_RBFParameters.length];

    // For each instance
    for (int k = 0; k < m_data.numInstances(); k++) {
      Instance inst = m_data.instance(k);

      // Calculate necessary input/output values and error term
      double[] outputs = calculateOutputs(inst);
      double pred = getOutput(inst, outputs);
      double actual = inst.classValue();
      double err = pred - actual;

      // For each hidden unit
      for (int i = 0; i < m_numUnits; i++) {

        // Update gradient for output weights
        grad[OFFSET_WEIGHTS + i] += err * outputs[i];

        // Update gradient for centers and possibly scale
        switch (m_scaleOptimizationOption) {
        case USE_GLOBAL_SCALE: { // Just one global scale parameter
          grad[OFFSET_SCALES] +=  derivativeOneScale(grad, err, m_RBFParameters[OFFSET_WEIGHTS + i], outputs[i], m_RBFParameters[OFFSET_SCALES], 
                                                     inst, i, m_classIndex, m_data.numAttributes());
          break; }
        case USE_SCALE_PER_UNIT_AND_ATTRIBUTE: { // One scale parameter for each unit and attribute
          derivativeScalePerAttribute(grad, err, m_RBFParameters[OFFSET_WEIGHTS + i], outputs[i], inst, i, m_classIndex, m_data.numAttributes());
          break; }
        default: { // Only one scale parameter per unit
          grad[OFFSET_SCALES + i] +=  derivativeOneScale(grad, err, m_RBFParameters[OFFSET_WEIGHTS + i], outputs[i], m_RBFParameters[OFFSET_SCALES + i], 
                                                         inst, i, m_classIndex, m_data.numAttributes());
          break; }
        }
      }

      // Update gradient for bias
      grad[OFFSET_WEIGHTS + m_numUnits] += err;
    }

    // For each output weight, include effect of ridge, do the same for scales
    for (int k = 0; k < m_numUnits; k++) {
      grad[OFFSET_WEIGHTS + k] += m_ridge * 2 * m_RBFParameters[OFFSET_WEIGHTS + k];
    }

    return grad;
  }

  /**
   * Calculates partial derivatives in the case of different sigma per attribute and unit.
   */
  protected void derivativeScalePerAttribute(double[] grad, double err, double weight, double output, Instance inst, int unitIndex, int classIndex, int numAttributes) {  

    double constant = err * weight * output;
    if (m_useNormalizedBasisFunctions) {
      constant *= (1 - output);
    }
    int offsetC = OFFSET_CENTERS + (unitIndex * numAttributes);
    int offsetS = OFFSET_SCALES + (unitIndex * numAttributes);
    double attWeight = 1.0;
    for (int j = 0; j < classIndex; j++) {
      double diff = (inst.value(j) - m_RBFParameters[offsetC + j]);
      double scalePart = (m_RBFParameters[offsetS + j] * m_RBFParameters[offsetS + j]);
      if (m_useAttributeWeights) {
        attWeight = m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] * m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
        grad[OFFSET_ATTRIBUTE_WEIGHTS + j] -= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] * constant * diff * diff / scalePart;
      }
      grad[offsetS + j] += constant * attWeight * diff * diff / (scalePart * m_RBFParameters[offsetS + j]);
      grad[offsetC + j] += constant * attWeight * diff / scalePart;
    }
    for (int j = classIndex + 1; j < numAttributes; j++) {
      double diff = (inst.value(j) - m_RBFParameters[offsetC + j]);
      double scalePart = (m_RBFParameters[offsetS + j] * m_RBFParameters[offsetS + j]);
      if (m_useAttributeWeights) {
        attWeight = m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] * m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
        grad[OFFSET_ATTRIBUTE_WEIGHTS + j] -= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] * constant * diff * diff / scalePart;
      }
      grad[offsetS + j] += constant * attWeight * diff * diff / (scalePart * m_RBFParameters[offsetS + j]);
      grad[offsetC + j] += constant * attWeight * diff / scalePart;
    }
  }

  /**
   * Calculates partial derivatives in the case of one sigma (either globally or per unit).
   */
  protected double derivativeOneScale(double[] grad, double err, double weight, double output,  double scale, Instance inst, int unitIndex, int classIndex, int numAttributes) {  

    double constant = err * weight * output / (scale * scale);
    if (m_useNormalizedBasisFunctions) {
      constant *= (1 - output);
    }
    double sumDiffSquared = 0;
    int offsetC = OFFSET_CENTERS + (unitIndex * numAttributes);
    double attWeight = 1.0;
    for (int j = 0; j < classIndex; j++) {
      double diff = (inst.value(j) - m_RBFParameters[offsetC + j]);
      double diffSquared = diff * diff;
      if (m_useAttributeWeights) {
        attWeight = m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] * m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
        grad[OFFSET_ATTRIBUTE_WEIGHTS + j] -= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] * constant * diffSquared;
      }
      sumDiffSquared += attWeight * diffSquared;
      grad[offsetC + j] += constant * attWeight * diff;
    }
    for (int j = classIndex + 1; j < numAttributes; j++) {
      double diff = (inst.value(j) - m_RBFParameters[offsetC + j]);
      double diffSquared = diff * diff;
      if (m_useAttributeWeights) {
        attWeight = m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] * m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
        grad[OFFSET_ATTRIBUTE_WEIGHTS + j] -= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] * constant * diffSquared;
      }
      sumDiffSquared += attWeight * diffSquared;
      grad[offsetC + j] += constant * attWeight * diff;
    }
    return constant * sumDiffSquared / scale;
  }

  /**
   * Calculates the array of outputs of the hidden units, normalized if that
   * option has been chosen.
   */
  protected double[] calculateOutputs(Instance inst) {

    double[] o = new double[m_numUnits];
    for (int i = 0; i < m_numUnits; i++) {
      o[i] = outputOfUnit(i, inst);
    }
    
    if (!m_useNormalizedBasisFunctions) {
      return o;
    } else {
      return Utils.logs2probs(o);
    }
  }

  /**
   * The output of a single unit.
   */
  protected double outputOfUnit(int i, Instance inst) {
    
    double sumSquaredDiff = 0;

    switch (m_scaleOptimizationOption) {
    case USE_GLOBAL_SCALE:  // Just one global scale parameter
      sumSquaredDiff = sumSquaredDiffOneScale(m_RBFParameters[OFFSET_SCALES], inst, i, m_classIndex, m_data.numAttributes()); 
      break; 
    case USE_SCALE_PER_UNIT_AND_ATTRIBUTE: { // One scale parameter for each unit and attribute
      sumSquaredDiff = sumSquaredDiffScalePerAttribute(inst, i, m_classIndex, m_data.numAttributes()); 
      break; }
    default:  // Only one scale parameter per unit
      sumSquaredDiff = sumSquaredDiffOneScale(m_RBFParameters[OFFSET_SCALES + i], inst, i, m_classIndex, m_data.numAttributes()); 
    }
        
    if (!m_useNormalizedBasisFunctions) {
      return Math.exp(-sumSquaredDiff);
    } else {
      return -sumSquaredDiff;
    }
  }

  /**
   * The exponent of the RBF in the case of a differeent sigma per attribute and unit.
   */
  protected double sumSquaredDiffScalePerAttribute(Instance inst, int unitIndex, int classIndex, int numAttributes) {  
   
    int offsetS = OFFSET_SCALES + unitIndex * numAttributes;
    int offsetC = OFFSET_CENTERS + unitIndex * numAttributes;
    double sumSquaredDiff = 0;
    for (int j = 0; j < classIndex; j++) {
      double diff = m_RBFParameters[offsetC + j] - inst.value(j);
      if (m_useAttributeWeights) {
        diff *= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
      }
      sumSquaredDiff += diff * diff / (2 * m_RBFParameters[offsetS + j] * m_RBFParameters[offsetS + j]);
    }
    for (int j = classIndex + 1; j < numAttributes; j++) {
      double diff = m_RBFParameters[offsetC + j] - inst.value(j);
      if (m_useAttributeWeights) {
        diff *= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
      }
      sumSquaredDiff += diff * diff / (2 * m_RBFParameters[offsetS + j] * m_RBFParameters[offsetS + j]);
    }
    return sumSquaredDiff;
  }    
    
  /**
   * The exponent of the RBF in the case of a fixed sigma per unit.
   */
  protected double sumSquaredDiffOneScale(double scale, Instance inst, int unitIndex, int classIndex, int numAttributes) {

    int offsetC = OFFSET_CENTERS + unitIndex * numAttributes;
    double sumSquaredDiff = 0;
    for (int j = 0; j < classIndex; j++) {
      double diff = m_RBFParameters[offsetC + j] - inst.value(j);
      if (m_useAttributeWeights) {
        diff *= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
      }
      sumSquaredDiff += diff * diff;
    }
    for (int j = classIndex + 1; j < numAttributes; j++) {
      double diff = m_RBFParameters[offsetC + j] - inst.value(j);
      if (m_useAttributeWeights) {
        diff *= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
      }
      sumSquaredDiff += diff * diff;
    }
    return sumSquaredDiff / (2 * scale * scale);
  }

  /**
   * Calculates the output of the network based on the given 
   * hidden layer outputs.
   */
  protected double getOutput(Instance inst, double[] outputs) {

    double result = 0;
    for (int i = 0; i < m_numUnits; i++) {
      result +=  m_RBFParameters[OFFSET_WEIGHTS + i] * outputs[i];
    }
    result += m_RBFParameters[OFFSET_WEIGHTS + m_numUnits];
    return result;
  }    

  /**
   * Calculates the output of the network for the given instance.
   */
  protected double getOutput(Instance inst) {
    
    return getOutput(inst, calculateOutputs(inst));
  }    
  
  /**
   * Calculates the output of the network after the instance has been
   * piped through the fliters to replace missing values, etc.
   */
  public double classifyInstance(Instance inst) throws Exception {
      
    if (m_trained) {
	
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
      m_Filter.batchFinished();
      inst = m_Filter.output();
      
      return getOutput(inst) * m_x1 + m_x0;
    } else {
      return getOutput(inst);
    }
  }

  /**
   * This will return a string describing the classifier.
   * @return The string.
   */
  public String globalInfo() {

    return 
      "Class implementing radial basis function networks for regression,"
      + " trained in a fully supervised manner using WEKA's Optimization"
      + " class by minimizing squared error with the BFGS method. Note that"
      + " all attributes are normalized into the [0,1] scale, including the"
      + " class. The initial centers for the Gaussian radial basis functions"
      + " are found using WEKA's SimpleKMeans. The initial sigma values are"
      + " set to the maximum distance between any center and its nearest"
      + " neighbour in the set of centers. There are several parameters. The"
      + " ridge parameter is used to penalize the size of the weights in the"
      + " output layer, which implements a simple linear combination. The"
      + " number of basis functions can also be specified. Note that large"
      + " numbers produce long training times. Another option determines"
      + " whether one global sigma value is used for all units (fastest),"
      + " whether one value is used per unit (common practice, it seems, and"
      + " set as the default), or a different value is learned for every"
      + " unit/attribute combination. It is also possible to learn attribute"
      + " weights for the distance function. (The square of the value shown"
      + " in the output is used.)  Finally, it is possible to use conjugate gradient"
      + " descent rather than BFGS updates, which is faster for cases with many parameters, and"
      + " to use normalized basis functions instead of unnormalized ones."
      + " Nominal attributes are processed using the unsupervised "
      + " NominalToBinary filter and missing values are replaced globally"
      + " using ReplaceMissingValues.";
  }
  
  /**
   * @return a string to describe the option
   */
  public String numFunctionsTipText() {

    return "The number of basis functions to use.";
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

    return "The ridge penalty factor for the output layer.";
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

    return "Whether to use conjugate gradient descent (recommended for many parameters).";
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
   * @return a string to describe the option
   */
  public String useAttributeWeightsTipText() {

    return "Whether to use attribute weights.";
  }

  /**
   * Gets whether to use attribute weights
   */
  public boolean getUseAttributeWeights() {

    return m_useAttributeWeights;
  }
  
  /**
   * Sets whether to use attribute weights.
   */
  public void setUseAttributeWeights(boolean newUseAttributeWeights) {

    m_useAttributeWeights = newUseAttributeWeights;
  }
  
  /**
   * @return a string to describe the option
   */
  public String useNormalizedBasisFunctionsTipText() {

    return "Whether to use normalized basis functions.";
  }

  /**
   * Gets whether to use normalized basis functions.
   */
  public boolean getUseNormalizedBasisFunctions() {

    return m_useNormalizedBasisFunctions;
  }
  
  /**
   * Sets whether to use normalized basis functions.
   */
  public void setUseNormalizedBasisFunctions(boolean newUseNormalizedBasisFunctions) {

    m_useNormalizedBasisFunctions = newUseNormalizedBasisFunctions;
  }
  
  /**
   * @return a string to describe the option
   */
  public String scaleOptimizationOptionTipText() {

    return "The number of sigma parameters to use.";
  }
  
  /**
   * Gets the distance weighting method used. Will be one of
   * WEIGHT_NONE, WEIGHT_INVERSE, or WEIGHT_SIMILARITY
   */
  public SelectedTag getScaleOptimizationOption() {

    return new SelectedTag(m_scaleOptimizationOption, TAGS_SCALE);
  }
  
  /**
   * Sets the scale optimization option to use.
   */
  public void setScaleOptimizationOption(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_SCALE) {
      m_scaleOptimizationOption = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(6);

    newVector.addElement(new Option(
                                    "\tNumber of Gaussian basis functions (default is 2).\n", 
                                    "N", 1, "-N"));

    newVector.addElement(new Option(
                                    "\tRidge factor for quadratic penalty on output weights (default is 0.01).\n", 
                                    "R", 1, "-R"));
    newVector.addElement(new Option(
                                    "\tThe scale optimization option: global scale (1), one scale per unit (2), scale per unit and attribute (3) (default is 2).\n", 
                                    "C", 1, "-C"));

    newVector.addElement(new Option(
                                    "\tUse conjugate gradient descent (recommended for many attributes).\n", 
                                    "G", 0, "-G"));

    newVector.addElement(new Option(
                                    "\tUse normalized basis functions.\n", 
                                    "O", 0, "-O"));
    newVector.addElement(new Option(
                                    "\tUse attribute weights.\n", 
                                    "A", 0, "-A"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
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
   *  Number of Gaussian basis functions (default is 2).
   * </pre>
   * 
   * <pre> -R
   *  Ridge factor for quadratic penalty on output weights (default is 0.01).
   * </pre>
   * 
   * <pre> -C
   *  The scale optimization option: global scale (1), one scale per unit (2), scale per unit and attribute (3) (default is 2).
   * </pre>
   * 
   * <pre> -G
   *  Use conjugate gradient descent (recommended for many attributes).
   * </pre>
   * 
   * <pre> -O
   *  Use normalized basis functions.
   * </pre>
   * 
   * <pre> -A
   *  Use attribute weights.
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
    String scale = Utils.getOption('C', options);
    if (scale.length() != 0) {
      setScaleOptimizationOption(new SelectedTag(Integer.parseInt(scale), TAGS_SCALE));
    } else {
      setScaleOptimizationOption(new SelectedTag(USE_SCALE_PER_UNIT, TAGS_SCALE));
    }
    m_useCGD = Utils.getFlag('G', options);
    m_useNormalizedBasisFunctions = Utils.getFlag('O', options);
    m_useAttributeWeights = Utils.getFlag('A', options);

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {


    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 10];

    int current = 0;
    options[current++] = "-N"; 
    options[current++] = "" + getNumFunctions();

    options[current++] = "-R"; 
    options[current++] = "" + getRidge();

    options[current++] = "-C"; 
    options[current++] = "" + getScaleOptimizationOption().getSelectedTag().getID();

    if (m_useCGD) {
      options[current++] = "-G";
    }

    if (m_useNormalizedBasisFunctions) {
      options[current++] = "-O";
    }

    if (m_useAttributeWeights) {
      options[current++] = "-A";
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

    if (m_RBFParameters == null) {
      return "Classifier not built yet.";
    }

    String s = "";
    
    for (int i = 0; i < m_numUnits; i++) {
      s += "\n\nOutput weight: " + m_RBFParameters[OFFSET_WEIGHTS + i];
      s += "\n\nUnit center:\n";
      for (int j = 0; j < m_data.numAttributes(); j++) {
        if (j != m_classIndex) {
          s += m_RBFParameters[OFFSET_CENTERS + (i * m_data.numAttributes()) + j] + "\t";
        }
      }
      if (m_scaleOptimizationOption == USE_SCALE_PER_UNIT_AND_ATTRIBUTE) {
        s += "\n\nUnit scales:\n";
        for (int j = 0; j < m_data.numAttributes(); j++) {
          if (j != m_classIndex) {
            s += m_RBFParameters[OFFSET_SCALES + (i * m_data.numAttributes()) + j] + "\t";
          }
        }
      } else if (m_scaleOptimizationOption == USE_SCALE_PER_UNIT) {
        s += "\n\nUnit scale:\n";
        s += m_RBFParameters[OFFSET_SCALES + i] + "\t";
      }
    }  
    if (m_scaleOptimizationOption == USE_GLOBAL_SCALE) {
      s += "\n\nScale:\n";
      s += m_RBFParameters[OFFSET_SCALES] + "\t";
    }
    if (m_useAttributeWeights) {
      s += "\n\nAttribute weights:\n";
      for (int j = 0; j < m_data.numAttributes(); j++) {
        if (j != m_classIndex) {
          s += m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] + "\t";
        }
      }
    }
    s += "\n\nBias weight: " + m_RBFParameters[OFFSET_WEIGHTS + m_numUnits];

    return s;
  }

  /**
   * Main method to run the code from the command-line using
   * the standard WEKA options.
   */
  public static void main(String[] argv) {

    runClassifier(new RBFRegressor(), argv);
  }
}

