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
 *    Autoencoder.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.filters.SimpleBatchFilter;
import weka.filters.UnsupervisedFilter;

import weka.core.Attribute;
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
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;

import weka.filters.unsupervised.attribute.Standardize;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.Filter;

import java.util.Random;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;

/**
 <!-- globalinfo-start -->
 * Implements an autoencoder with one hidden layer and tied weights using WEKA's Optimization class by minimizing the squared error plus a quadratic penalty (weight decay) with the BFGS method. Provides contractive autoencoder as an alternative to weight decay. Note that all attributes are standardized, including the target. There are several parameters. The lambda parameter is used to determine the penalty on the size of the weights. The number of hidden units can also be specified. Note that large numbers produce long training times. Finally, it is possible to use conjugate gradient descent rather than BFGS updates, which may be faster for cases with many parameters. To improve speed, an approximate version of the logistic function is used as the activation function. Also, if delta values in the backpropagation step are  within the user-specified tolerance, the gradient is not updated for that particular instance, which saves some additional time. Paralled calculation of squared error and gradient is possible when multiple CPU cores are present. Data is split into batches and processed in separate threads in this case. Note that this only improves runtime for larger datasets. For more information on autoencoders, see<br/>
 * <br/>
 * Salah Rifai, Pascal Vincent, Xavier Muller, Xavier Glorot, Yoshua Bengio: Contractive Auto-Encoders: Explicit Invariance During Feature Extraction. In: International Conference on Machine Learning, 833-840, 2011.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N &lt;int&gt;
 *  Number of hidden units (default is 2).
 * </pre>
 * 
 * <pre> -L &lt;double&gt;
 *  Lambda factor for penalty on weights (default is 0.01).
 * </pre>
 * 
 * <pre> -O &lt;double&gt;
 *  Tolerance parameter for delta values (default is 1.0e-6).
 * </pre>
 * 
 * <pre> -G
 *  Use conjugate gradient descent (recommended for many attributes).
 * </pre>
 * 
 * <pre> -C
 *  Use contractive autoencoder instead of autoencoder with weight decay.
 * </pre>
 * 
 * <pre> -X
 *  Use exact sigmoid function rather than approximation.
 * </pre>
 * 
 * <pre> -F
 *  Output data in original space, so do not output reduced data.
 * </pre>
 * 
 * <pre> -P &lt;int&gt;
 *  The size of the thread pool, for example, the number of cores in the CPU. (default 1)
 * </pre>
 * 
 * <pre> -E &lt;int&gt;
 *  The number of threads to use, which should be &gt;= size of thread pool. (default 1)
 * </pre>
 * 
 * <pre> -D
 *  Turns on output of debugging information.</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 9346 $
 */
public class Autoencoder extends SimpleBatchFilter implements UnsupervisedFilter, TechnicalInformationHandler  {
  
  /** For serialization */
  private static final long serialVersionUID = -277474276438394612L;

  /**
   * Returns default capabilities of the filter.
   *
   * @return      the capabilities of this filter
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.NO_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.NOMINAL_CLASS);
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
      return RevisionUtils.extract("$Revision: 9346 $");
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
      return RevisionUtils.extract("$Revision: 9346 $");
    }
  }

  // The number of hidden units
  protected int m_numUnits = 2;

  // A reference to the actual data
  protected Instances m_data = null;

  // The number of attributes in the data
  protected int m_numAttributes = -1;

  // The parameter vector
  protected double[] m_MLPParameters = null;

  // Offset for output unit biases
  protected int OFFSET_O_BIASES = -1;

  // Offset for hidden unit biases
  protected int OFFSET_H_BIASES = -1;

  // The lambda parameter
  protected double m_lambda = 0.01;

  // Whether to use conjugate gradient descent rather than BFGS updates
  protected boolean m_useCGD = false;

  // Tolerance parameter for delta values
  protected double m_tolerance = 1.0e-6;

  // The number of threads to use to calculate gradient and squared error
  protected int m_numThreads = 1;

  // The size of the thread pool
  protected int m_poolSize = 1;

  // The standardization filer
  protected Filter m_Filter = null;
  
  // Filter used to remove class attribute (if necessary)
  protected Remove m_Remove = null;
  
  // Thread pool
  protected transient ExecutorService m_Pool = null;

  // Whether to use contractive autoencoder instead of weight decay
  protected boolean m_useContractive = false;

  // Whether to use exact sigmoid instead of approximation
  protected boolean m_useExactSigmoid = false;

  // Whether to output the data in the original space
  protected boolean m_outputInOriginalSpace = false;

  /**
   * Method used to pre-process the data, perform clustering, and
   * set the initial parameter vector.
   */
  protected Instances initParameters(Instances data) throws Exception {

    // can filter handle the data?
    getCapabilities().testWithFail(data);

    data = new Instances(data);

    // Make sure data is shuffled
    Random random = new Random(1);
    if (data.numInstances() > 1) {
      random = data.getRandomNumberGenerator(1); 
    }
    data.randomize(random);

    // Remove class if necessary
    m_Remove = null;
    if (data.classIndex() >= 0) {
      m_Remove = new Remove();
      m_Remove.setAttributeIndices("" + (data.classIndex() + 1));
      m_Remove.setInputFormat(data);
      data = Filter.useFilter(data, m_Remove);
    }

    // Standardize data
    m_Filter = new Standardize();
    ((Standardize)m_Filter).setIgnoreClass(true);
    m_Filter.setInputFormat(data);
    data = Filter.useFilter(data, m_Filter); 
    
    m_numAttributes = data.numAttributes();
    
    // Set up array 
    OFFSET_O_BIASES = m_numAttributes * m_numUnits;
    OFFSET_H_BIASES = OFFSET_O_BIASES + m_numAttributes;
    m_MLPParameters = new double[OFFSET_H_BIASES + m_numUnits];
    
    // Initialize parameters 
    for (int i = 0; i < m_numUnits; i++) {
      int offset = i * m_numAttributes;
      for (int j = 0; j < m_numAttributes; j++) {
        m_MLPParameters[offset + j] = 0.1 * random.nextGaussian();
      }
    }

    // Initialize bias parameters
    for (int i = 0; i < m_numAttributes; i++) {
      m_MLPParameters[OFFSET_O_BIASES + i] = 0.1 * random.nextGaussian();
    }
    for (int i = 0; i < m_numUnits; i++) {
      m_MLPParameters[OFFSET_H_BIASES + i] = 0.1 * random.nextGaussian();
    }
    
    return data;
  }

  /**
   * Builds the autoencoder network based on the given data.
   */
  public void initFilter(Instances data) throws Exception {

    // Set up the initial arrays
    m_data = initParameters(data);
    if (m_data == null) {
      return;
    }

    // Initialise thread pool
    m_Pool = Executors.newFixedThreadPool(m_poolSize);

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

    // Shut down thread pool
    m_Pool.shutdown();
  } 

  /**
   * Calculates the (penalized) squared error based on the current parameter vector.
   */
  protected double calculateSE() {

    // Set up result set, and chunk size
    int chunksize = m_data.numInstances() / m_numThreads;
    Set<Future<Double>> results = new HashSet<Future<Double>>();

    // For each thread
    for (int j = 0; j < m_numThreads; j++) {

      // Determine batch to be processed
      final int lo = j * chunksize;
      final int hi = (j < m_numThreads - 1) ? (lo + chunksize) : m_data.numInstances();

      // Create and submit new job, where each instance in batch is processed
      Future<Double> futureSE = m_Pool.submit(new Callable<Double>() {
          public Double call() {
            final double[] outputs = new double[m_numUnits];
            double SE = 0;
            for (int k = lo; k < hi; k++) {
              final Instance inst = m_data.instance(k);
              
              // Calculate necessary input/output values and error term
              calculateOutputs(inst, outputs, null);
                
              // Add to squared error
              for (int index = 0; index < m_data.numAttributes(); index++) {
                final double err = getOutput(index, outputs) - inst.value(index);
                SE += 0.5 * err * err;
              }

              // Do we want to build a contractive autoencoder?
              if (m_useContractive) {

                // Add penalty
                for (int i = 0; i < outputs.length; i++) {
                  double sum = 0;
                  int offset = i * m_numAttributes;
                  for (int index = 0; index < m_data.numAttributes(); index++) {
                    sum += m_MLPParameters[offset + index] * m_MLPParameters[offset + index];
                  }
                  SE += m_lambda * outputs[i] * (1.0 - outputs[i]) *
                    outputs[i] * (1.0 - outputs[i]) * sum;
                }
              }
            }
            return SE;
          }
        });
      results.add(futureSE);
    }

    // Calculate SE
    double SE = 0;
    try {
      for (Future<Double> futureSE : results) {
        SE += futureSE.get();
      }
    } catch (Exception e) {
      System.out.println("Squared error could not be calculated.");
    }

    // Use quadratic penalty if we don't build a contractive autoencoder
    if (!m_useContractive) {

      // Calculate sum of squared weights, excluding bias weights
      double squaredSumOfWeights = 0;
      for (int i = 0; i < m_numUnits; i++) {
        int offset = i * m_numAttributes;
        for (int j = 0; j < m_numAttributes; j++) {
          squaredSumOfWeights += m_MLPParameters[offset + j] * m_MLPParameters[offset + j];
        }
      }
      SE += m_lambda * squaredSumOfWeights;
    }

    return SE / m_data.numInstances();
  }

  /**
   * Calculates the gradient based on the current parameter vector.
   */
  protected double[] calculateGradient() {

    // Set up result set, and chunk size
    int chunksize = m_data.numInstances() / m_numThreads;
    Set<Future<double[]>> results = new HashSet<Future<double[]>>();

    // For each thread
    for (int j = 0; j < m_numThreads; j++) {

      // Determine batch to be processed
      final int lo = j * chunksize;
      final int hi = (j < m_numThreads - 1) ? (lo + chunksize) : m_data.numInstances();

      // Create and submit new job, where each instance in batch is processed
      Future<double[]> futureGrad = m_Pool.submit(new Callable<double[]>() {
          public double[] call() {

            final double[] outputs = new double[m_numUnits];
            final double[] deltaHidden = new double[m_numUnits];
            final double[] sigmoidDerivativesHidden = new double[m_numUnits];
            final double[] localGrad = new double[m_MLPParameters.length];
            for (int k = lo; k < hi; k++) {
              final Instance inst = m_data.instance(k);
              calculateOutputs(inst, outputs, sigmoidDerivativesHidden);
              updateGradient(localGrad, inst, outputs, deltaHidden);
              updateGradientForHiddenUnits(localGrad, inst, sigmoidDerivativesHidden, deltaHidden);

              // Dow we want to build a contractive autoencoder?
              if (m_useContractive) {
                
                // Update gradient wrt penalty
                for (int i = 0; i < outputs.length; i++) {
                  double sum = 0;
                  for (int index = 0; index < m_data.numAttributes(); index++) {
                    sum += m_MLPParameters[i * m_numAttributes + index] * 
                      m_MLPParameters[i * m_numAttributes + index];
                  }
                  double multiplier = m_lambda * 2 * outputs[i] * (1.0 - outputs[i]) * outputs[i] * (1.0 - outputs[i]);
                  int offset = i * m_numAttributes;
                  for (int index = 0; index < m_data.numAttributes(); index++) {
                    localGrad[offset + index] +=  multiplier * (m_MLPParameters[offset + index] + inst.value(index) * (1.0 - 2.0 * outputs[i]) * sum);
                  }
                  
                  // Update gradient for hidden unit bias wrt penalty
                  localGrad[OFFSET_H_BIASES + i] += multiplier * (1.0 - 2.0 * outputs[i]) * sum;
                }
              }
            }
            return localGrad;
          }
        });
      results.add(futureGrad);
    }

    // Calculate final gradient
    double[] grad = new double[m_MLPParameters.length];
    try {
      for (Future<double[]> futureGrad : results) {
        double[] lg = futureGrad.get();
        for (int  i = 0; i < lg.length; i++) {
          grad[i] += lg[i];
        }
      }
    } catch (Exception e) {
      System.out.println("Gradient could not be calculated.");
    }

    // Use quadratic penalty if we don't build a contractive autoencoder
    if (!m_useContractive) {
      
      // For all network weights, perform weight decay
      for (int k = 0; k < m_numUnits; k++) {
        int offset = k * m_numAttributes;
        for (int j = 0; j < m_numAttributes; j++) {
          grad[offset + j] += m_lambda * 2 * m_MLPParameters[offset + j];
        }
      }
    }

    double factor = 1.0 / m_data.numInstances();
    for (int i = 0; i < grad.length; i++) {
      grad[i] *= factor;
    }

    // Check against empirical gradient
    if (m_Debug) {
      double MULTIPLIER = 1000;
      double EPSILON = 1e-4;
      for (int i = 0; i < m_MLPParameters.length; i++) {
        double backup = m_MLPParameters[i];
        m_MLPParameters[i] = backup + EPSILON;
        double val1 = calculateSE();
        m_MLPParameters[i] = backup - EPSILON;
        double val2 = calculateSE();
        m_MLPParameters[i] = backup;
        double empirical = (double)Math.round((val1 - val2) / (2 * EPSILON) * MULTIPLIER) / MULTIPLIER;
        double derived = (double)Math.round(grad[i] * MULTIPLIER) / MULTIPLIER;
        if (empirical != derived) {
          System.err.println("Empirical gradient: " + empirical + " Derived gradient: " + derived);
          System.exit(1);
        }
      }
    }

    return grad;
  }

  /**
   * Update the gradient for the weights in the output layer.
   */
  protected void updateGradient(double[] grad, Instance inst, double[] outputs, double[] deltaHidden) {

    // Initialise deltaHidden
    Arrays.fill(deltaHidden, 0.0);

    // Go through all output units
    for (int j = 0; j < m_numAttributes; j++) {

      // Get output 
      double pred = getOutput(j, outputs); 

      // Calculate delta from output unit
      double deltaOut = (pred - inst.value(j));

      // Go to next output unit if update too small
      if (deltaOut <= m_tolerance && deltaOut >= -m_tolerance) {
        continue;
      }

      // Update deltaHidden
      for (int i = 0; i < m_numUnits; i++) {
        deltaHidden[i] += deltaOut * m_MLPParameters[i * m_numAttributes + j];
      }

      // Update gradient for output weights
      for (int i = 0; i < m_numUnits; i++) {
        grad[i * m_numAttributes + j] += deltaOut * outputs[i];
      }
    
      // Update gradient for bias
      grad[OFFSET_O_BIASES + j] += deltaOut;
    }
  }

  /**
   * Update the gradient for the weights in the hidden layer.
   */
  protected void updateGradientForHiddenUnits(double[] grad, Instance inst,  double[] sigmoidDerivativesHidden, double[] deltaHidden) {

    // Finalize deltaHidden
    for (int i = 0; i < m_numUnits; i++) {
      deltaHidden[i] *= sigmoidDerivativesHidden[i];
    }

    // Update gradient for hidden units
    for (int i = 0; i < m_numUnits; i++) {
      
      // Skip calculations if update too small
      if (deltaHidden[i] <= m_tolerance && deltaHidden[i] >= -m_tolerance) {
        continue;
      }
      
      // Update gradient for all weights, including bias
      int offset = i * m_numAttributes;
      for (int l = 0; l < m_numAttributes; l++) {
        grad[offset + l] += deltaHidden[i] * inst.value(l);
      }
      grad[OFFSET_H_BIASES + i] += deltaHidden[i];
    }
  }

  /**
   * Calculates the array of outputs of the hidden units.
   */
  protected void calculateOutputs(Instance inst, double[] o,
                                  double[] d) {

    for (int i = 0; i < m_numUnits; i++) {
      double sum = 0;
      int offset = i * m_numAttributes;
      for (int j = 0; j < m_numAttributes; j++) {
        sum += inst.value(j) * m_MLPParameters[offset + j];
      }
      sum += m_MLPParameters[OFFSET_H_BIASES + i]; 
      o[i] = sigmoid(-sum, d, i);
    }
  }
  
  /**
   * Calculates the output of one output unit based on the given 
   * hidden layer outputs.
   */
  protected double getOutput(int j, double[] outputs) {

    double result = 0;
    for (int i = 0; i < m_numUnits; i++) {
      result +=  m_MLPParameters[i * m_numAttributes + j] * outputs[i];
    }
    result += m_MLPParameters[OFFSET_O_BIASES + j];
    return result;
  }    
  
  /**
   * Computes approximate sigmoid function. Derivative is 
   * stored in second argument at given index if d != null.
   */
  protected double sigmoid(double x, double[] d, int index) {

    double output = 0;
    if (m_useExactSigmoid) {
      output = 1.0 / (1.0 + Math.exp(x));
      if (d != null) {
        d[index] = output * (1.0 - output);
      }
    } else {
      
      // Compute approximate sigmoid
      double y = 1.0 + x / 4096.0;
      x = y * y; x *= x; x *= x; x *= x;
      x *= x; x *= x; x *= x; x *= x;
      x *= x; x *= x; x *= x; x *= x;
      output = 1.0 / (1.0 + x);
      
      // Compute derivative if desired
      if (d != null) {
        d[index] = output * (1.0 - output) / y;
      }
    }

    return output;
  }
  
  /**
   * Determines the output format based on the input format and returns 
   * this.
   *
   * @param inputFormat     the input format to base the output format on
   * @return                the output format
   */
  protected Instances determineOutputFormat(Instances inputFormat) {

    // Do we want to output reduced data?
    if (!m_outputInOriginalSpace) {

      // Construct header for pairwise data
      ArrayList<Attribute> atts = new ArrayList<Attribute>(m_numUnits + 1);
      for (int i = 0; i < m_numUnits; i++) {
        atts.add(new Attribute("hidden_unit_" + i + "output"));
      }
      
      // Add class attribute if present in the input data
      if (inputFormat.classIndex() >= 0) {
        atts.add((Attribute)inputFormat.classAttribute().copy());
      }
      
      Instances outputFormat = new Instances(inputFormat.relationName() + "_autoencoded", atts, 0);
      if (inputFormat.classIndex() >= 0) {
        outputFormat.setClassIndex(outputFormat.numAttributes() - 1);
      }
      return outputFormat;
    } else {
      return new Instances(inputFormat, 0);
    }
  }

  /**
   * Processes the given data.
   *
   * @param instances   the data to process
   * @return            the modified data
   * @throws Exception  in case the processing goes wrong
   */
  protected Instances process(Instances instances) throws Exception {

    // Do we need to build the filter model?
    if (!isFirstBatchDone()) {
      initFilter(instances);
    }

    // Generate the output and return it
    Instances result = determineOutputFormat(instances);
    for (int i = 0; i < instances.numInstances(); i++) {
      Instance inst = instances.instance(i);

      // Store class value if present and remove class attribute
      double classVal = 0;
      if (instances.classIndex() >= 0) {
        classVal = inst.classValue();
        m_Remove.input(inst);
        inst = m_Remove.output();
      }

      // Standardize instance
      m_Filter.input(inst);
      inst = m_Filter.output();

      // Get new values
      double[] outputs = new double[m_numUnits];
      calculateOutputs(inst, outputs, null);

      // Do we want to output reduced data?
      if (!m_outputInOriginalSpace) {
        
        // Copy over class value if necessary
        if (instances.classIndex() >= 0) {
          double[] newVals = new double[m_numUnits + 1];
          System.arraycopy(outputs, 0, newVals, 0, m_numUnits);
          newVals[newVals.length - 1] = classVal;
          outputs = newVals;
        }
        result.add(new DenseInstance(inst.weight(), outputs));
      } else {
        double[] newVals = new double[instances.numAttributes()];
        for (int index = 0; index < newVals.length; index++) {
          if (index != instances.classIndex()) {
            newVals[index] = getOutput(index, outputs);
          } else {
            newVals[index] = classVal;
          }
        }
        result.add(new DenseInstance(inst.weight(), newVals));
      }
    }
    return result;
  }

  /**
   * This will return a string describing the filter.
   * @return The string.
   */
  public String globalInfo() {

    return 
      "Implements an autoencoder with one hidden layer and tied weights using WEKA's Optimization class"
      + " by minimizing the squared error plus a quadratic penalty (weight decay) with the BFGS method."
      + " Provides contractive autoencoder as an alternative to weight decay."
      + " Note that all attributes are standardized, including the target. There are several parameters. The"
      + " lambda parameter is used to determine the penalty on the size of the weights. The"
      + " number of hidden units can also be specified. Note that large"
      + " numbers produce long training times. Finally, it is possible to use conjugate gradient"
      + " descent rather than BFGS updates, which may be faster for cases with many parameters."
      + " To improve speed, an approximate version of the logistic function is used as the"
      + " activation function. Also, if delta values in the backpropagation step are "
      + " within the user-specified tolerance, the gradient is not updated for that"
      + " particular instance, which saves some additional time. Paralled calculation"
      + " of squared error and gradient is possible when multiple CPU cores are present."
      + " Data is split into batches and processed in separate threads in this case."
      + " Note that this only improves runtime for larger datasets. For more information on autoencoders, see\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   *
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    TechnicalInformation 	additional;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Salah Rifai and Pascal Vincent and Xavier Muller and Xavier Glorot and Yoshua Bengio");
    result.setValue(Field.TITLE, "Contractive Auto-Encoders: Explicit Invariance During Feature Extraction");
    result.setValue(Field.BOOKTITLE, "International Conference on Machine Learning");
    result.setValue(Field.YEAR, "2011");
    result.setValue(Field.PAGES, "833-840");
    result.setValue(Field.PUBLISHER, "Omnipress");

    return result;
  }
  
  /**
   * @return a string to describe the option
   */
  public String toleranceTipText() {

    return "The tolerance parameter for the delta values.";
  }

  /**
   * Gets the tolerance parameter for the delta values.
   */
  public double getTolerance() {

    return m_tolerance;
  }
  
  /**
   * Sets the tolerance parameter for the delta values.
   */
  public void setTolerance(double newTolerance) {

    m_tolerance = newTolerance;
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
  public String lambdaTipText() {

    return "The lambda penalty factor for the penalty on the weights.";
  }

  /**
   * Gets the value of the lambda parameter.
   */
  public double getLambda() {

    return m_lambda;
  }
  
  /**
   * Sets the value of the lambda parameter.
   */
  public void setLambda(double newLambda) {

    m_lambda = newLambda;
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
   * @return a string to describe the option
   */
  public String useContractiveAutoencoderTipText() {

    return "Whether to use contractive autoencoder rather than autoencoder with weight decay.";
  }

  /**
   * Gets whether to use ContractiveAutoencoder.
   */
  public boolean getUseContractiveAutoencoder() {

    return m_useContractive;
  }
  
  /**
   * Sets whether to use ContractiveAutoencoder.
   */
  public void setUseContractiveAutoencoder(boolean newUseContractiveAutoencoder) {

    m_useContractive = newUseContractiveAutoencoder;
  }
  
  /**
   * @return a string to describe the option
   */
  public String useExactSigmoidTipText() {

    return "Whether to use exact sigmoid function rather than approximation.";
  }

  /**
   * Gets whether to use exact sigmoid.
   */
  public boolean getUseExactSigmoid() {

    return m_useExactSigmoid;
  }
  
  /**
   * Sets whether to use exact sigmoid.
   */
  public void setUseExactSigmoid(boolean newUseExactSigmoid) {

    m_useExactSigmoid = newUseExactSigmoid;
  }
  
  /**
   * @return a string to describe the option
   */
  public String outputInOriginalSpaceTipText() {

    return "Whether to output data in original space, so not reduced.";
  }

  /**
   * Gets whether to use original space.
   */
  public boolean getOutputInOriginalSpace() {

    return m_outputInOriginalSpace;
  }
  
  /**
   * Sets whether to use original space.
   */
  public void setOutputInOriginalSpace(boolean newOutputInOriginalSpace) {

    m_outputInOriginalSpace = newOutputInOriginalSpace;
  }

  /**
   * @return a string to describe the option
   */
  public String numThreadsTipText() {

    return "The number of threads to use, which should be >= size of thread pool.";
  }

  /**
   * Gets the number of threads.
   */
  public int getNumThreads() {

    return m_numThreads;
  }
  
  /**
   * Sets the number of threads
   */
  public void setNumThreads(int nT) {

    m_numThreads = nT;
  }

  /**
   * @return a string to describe the option
   */
  public String poolSizeTipText() {

    return "The size of the thread pool, for example, the number of cores in the CPU.";
  }

  /**
   * Gets the number of threads.
   */
  public int getPoolSize() {

    return m_poolSize;
  }
  
  /**
   * Sets the number of threads
   */
  public void setPoolSize(int nT) {

    m_poolSize = nT;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector<Option> newVector = new Vector<Option>(7);

    newVector.addElement(new Option(
                                    "\tNumber of hidden units (default is 2).\n", 
                                    "N", 1, "-N <int>"));

    newVector.addElement(new Option(
                                    "\tLambda factor for penalty on weights (default is 0.01).\n", 
                                    "L", 1, "-L <double>"));
    newVector.addElement(new Option(
                                    "\tTolerance parameter for delta values (default is 1.0e-6).\n", 
                                    "O", 1, "-O <double>"));
    newVector.addElement(new Option(
                                    "\tUse conjugate gradient descent (recommended for many attributes).\n", 
                                    "G", 0, "-G"));
    newVector.addElement(new Option(
                                    "\tUse contractive autoencoder instead of autoencoder with weight decay.\n", 
                                    "C", 0, "-C"));
    newVector.addElement(new Option(
                                    "\tUse exact sigmoid function rather than approximation.\n", 
                                    "X", 0, "-X"));
    newVector.addElement(new Option(
                                    "\tOutput data in original space, so do not output reduced data.\n", 
                                    "F", 0, "-F"));
    newVector.addElement(new Option(
                                    "\t" + poolSizeTipText() + " (default 1)\n", 
                                    "P", 1, "-P <int>"));
    newVector.addElement(new Option(
                                    "\t" + numThreadsTipText() + " (default 1)\n", 
                                    "E", 1, "-E <int>"));

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
   * <pre> -N &lt;int&gt;
   *  Number of hidden units (default is 2).
   * </pre>
   * 
   * <pre> -L &lt;double&gt;
   *  Lambda factor for penalty on weights (default is 0.01).
   * </pre>
   * 
   * <pre> -O &lt;double&gt;
   *  Tolerance parameter for delta values (default is 1.0e-6).
   * </pre>
   * 
   * <pre> -G
   *  Use conjugate gradient descent (recommended for many attributes).
   * </pre>
   * 
   * <pre> -C
   *  Use contractive autoencoder instead of autoencoder with weight decay.
   * </pre>
   * 
   * <pre> -X
   *  Use exact sigmoid function rather than approximation.
   * </pre>
   * 
   * <pre> -F
   *  Output data in original space, so do not output reduced data.
   * </pre>
   * 
   * <pre> -P &lt;int&gt;
   *  The size of the thread pool, for example, the number of cores in the CPU. (default 1)
   * </pre>
   * 
   * <pre> -E &lt;int&gt;
   *  The number of threads to use, which should be &gt;= size of thread pool. (default 1)
   * </pre>
   * 
   * <pre> -D
   *  Turns on output of debugging information.</pre>
   * 
   <!-- options-end -->
   *
   * Options after -- are passed to the designated filter.<p>
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
    String Lambda = Utils.getOption('L', options);
    if (Lambda.length() != 0) {
      setLambda(Double.parseDouble(Lambda));
    } else {
      setLambda(0.01);
    }
    String Tolerance = Utils.getOption('O', options);
    if (Tolerance.length() != 0) {
      setTolerance(Double.parseDouble(Tolerance));
    } else {
      setTolerance(1.0e-6);
    }
    m_useCGD = Utils.getFlag('G', options);
    m_useContractive = Utils.getFlag('C', options);
    m_useExactSigmoid = Utils.getFlag('X', options);
    m_outputInOriginalSpace = Utils.getFlag('F', options);
    String PoolSize = Utils.getOption('P', options);
    if (PoolSize.length() != 0) {
      setPoolSize(Integer.parseInt(PoolSize));
    } else {
      setPoolSize(1);
    }
    String NumThreads = Utils.getOption('E', options);
    if (NumThreads.length() != 0) {
      setNumThreads(Integer.parseInt(NumThreads));
    } else {
      setNumThreads(1);
    }

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {


    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 14];

    int current = 0;
    options[current++] = "-N"; 
    options[current++] = "" + getNumFunctions();

    options[current++] = "-L"; 
    options[current++] = "" + getLambda();

    options[current++] = "-O"; 
    options[current++] = "" + getTolerance();

    if (m_useCGD) {
      options[current++] = "-G";
    }

    if (m_useContractive) {
      options[current++] = "-C";
    }

    if (m_useExactSigmoid) {
      options[current++] = "-X";
    }

    if (m_outputInOriginalSpace) {
      options[current++] = "-F";
    }

    options[current++] = "-P"; 
    options[current++] = "" + getPoolSize();

    options[current++] = "-E"; 
    options[current++] = "" + getNumThreads();

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);

    current += superOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Main method to run the code from the command-line using
   * the standard WEKA options.
   */
  public static void main(String[] argv) {

    runFilter(new Autoencoder(), argv);
  }
}

