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
 *    MLPAutoencoder.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.ConjugateGradientOptimization;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Optimization;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.SparseInstance;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.SimpleBatchFilter;
import weka.filters.UnsupervisedFilter;

/**
 * <!-- globalinfo-start --> Implements an autoencoder with one hidden layer and
 * tied weights using WEKA's Optimization class by minimizing the squared error
 * plus a quadratic penalty (weight decay) with the BFGS method. Provides
 * contractive autoencoder as an alternative to weight decay. Note that all
 * attributes are standardized, including the target. There are several
 * parameters. The lambda parameter is used to determine the penalty on the size
 * of the weights. The number of hidden units can also be specified. Note that
 * large numbers produce long training times. Finally, it is possible to use
 * conjugate gradient descent rather than BFGS updates, which may be faster for
 * cases with many parameters. To improve speed, an approximate version of the
 * logistic function is used as the activation function. Also, if delta values
 * in the backpropagation step are within the user-specified tolerance, the
 * gradient is not updated for that particular instance, which saves some
 * additional time. Paralled calculation of squared error and gradient is
 * possible when multiple CPU cores are present. Data is split into batches and
 * processed in separate threads in this case. Note that this only improves
 * runtime for larger datasets. For more information on autoencoders, see<br/>
 * <br/>
 * Salah Rifai, Pascal Vincent, Xavier Muller, Xavier Glorot, Yoshua Bengio:
 * Contractive Auto-Encoders: Explicit Invariance During Feature Extraction. In:
 * International Conference on Machine Learning, 833-840, 2011.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -N &lt;int&gt;
 *  Number of hidden units (default is 2).
 * </pre>
 * 
 * <pre>
 * -L &lt;double&gt;
 *  Lambda factor for penalty on weights (default is 0.01).
 * </pre>
 * 
 * <pre>
 * -O &lt;double&gt;
 *  Tolerance parameter for delta values (default is 1.0e-6).
 * </pre>
 * 
 * <pre>
 * -G
 *  Use conjugate gradient descent (recommended for many attributes).
 * </pre>
 * 
 * <pre>
 * -C
 *  Use contractive autoencoder instead of autoencoder with weight decay.
 * </pre>
 * 
 * <pre>
 * -X
 *  Use exact sigmoid function rather than approximation.
 * </pre>
 * 
 * <pre>
 * -F
 *  Output data in original space, so do not output reduced data.
 * </pre>
 * 
 * <pre>
 * -P &lt;int&gt;
 *  The size of the thread pool, for example, the number of cores in the CPU. (default 1)
 * </pre>
 * 
 * <pre>
 * -E &lt;int&gt;
 *  The number of threads to use, which should be &gt;= size of thread pool. (default 1)
 * </pre>
 * 
 * <pre>
 * -weights-file &lt;filename&gt;
 *  The file to write weight vectors to in ARFF format.
 *  (default: none)
 * </pre>
 * 
 * <pre>
 * -S
 *  Whether to 0=normalize/1=standardize/2=neither. (default 1=standardize)
 * </pre>
 *
 * <pre>
 * -seed &lt;int&gt;
 *  The seed for the random number generator (default is 1).
 * </pre>
 *
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 9346 $
 */
public class MLPAutoencoder extends SimpleBatchFilter implements
  UnsupervisedFilter, TechnicalInformationHandler {

  /** For serialization */
  private static final long serialVersionUID = -277474276438394612L;

  /**
   * Returns default capabilities of the filter.
   * 
   * @return the capabilities of this filter
   */
  @Override
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
   * Simple wrapper class needed to use the BFGS method implemented in
   * weka.core.Optimization.
   */
  protected class OptEng extends Optimization {

    /**
     * Returns the squared error given parameter values x.
     */
    @Override
    protected double objectiveFunction(double[] x) {

      m_MLPParameters = x;
      return calculateSE();
    }

    /**
     * Returns the gradient given parameter values x.
     */
    @Override
    protected double[] evaluateGradient(double[] x) {

      m_MLPParameters = x;
      return calculateGradient();
    }

    /**
     * The revision string.
     */
    @Override
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 9346 $");
    }
  }

  /**
   * Simple wrapper class needed to use the CGD method implemented in
   * weka.core.ConjugateGradientOptimization.
   */
  protected class OptEngCGD extends ConjugateGradientOptimization {

    /**
     * Returns the squared error given parameter values x.
     */
    @Override
    protected double objectiveFunction(double[] x) {

      m_MLPParameters = x;
      return calculateSE();
    }

    /**
     * Returns the gradient given parameter values x.
     */
    @Override
    protected double[] evaluateGradient(double[] x) {

      m_MLPParameters = x;
      return calculateGradient();
    }

    /**
     * The revision string.
     */
    @Override
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

  // filter: Normalize training data
  public static final int FILTER_NORMALIZE = 0;

  // filter: Standardize training data
  public static final int FILTER_STANDARDIZE = 1;

  // filter: No normalization/standardization
  public static final int FILTER_NONE = 2;

  // The filter to apply to the training data
  public static final Tag[] TAGS_FILTER = {
    new Tag(FILTER_NORMALIZE, "Normalize training data"),
    new Tag(FILTER_STANDARDIZE, "Standardize training data"),
    new Tag(FILTER_NONE, "No normalization/standardization"), };

  // Whether to normalize/standardize/neither
  protected int m_filterType = FILTER_STANDARDIZE;

  // The filer
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

  // File to write weight vectors to
  protected File m_WeightsFile = new File(System.getProperty("user.dir"));;

  // Parameters for transformation
  protected double[] m_offsets = null;
  protected double[] m_factors = null;

  // The random number seed.
  protected int m_Seed = 1;

  /**
   * Calculates min and max for all attributes in the given data.
   */
  protected double[][] minAndMax(Instances data) {

    double[][] result = new double[2][m_numAttributes];
    for (int j = 0; j < m_numAttributes; j++) {
      result[0][j] = Double.MAX_VALUE;
      result[1][j] = -Double.MAX_VALUE;
    }
    for (int i = 0; i < data.numInstances(); i++) {
      for (int j = 0; j < m_numAttributes; j++) {
        double value = data.instance(i).value(j);
        if (value < result[0][j]) {
          result[0][j] = value;
        }
        if (value > result[1][j]) {
          result[1][j] = value;
        }
      }
    }
    return result;
  }

  /**
   * Method used to pre-process the data, perform clustering, and set the
   * initial parameter vector.
   */
  protected Instances initParameters(Instances data) throws Exception {

    // can filter handle the data?
    getCapabilities().testWithFail(data);

    data = new Instances(data);

    // Make sure data is shuffled
    Random random = new Random(getSeed());
    if (data.numInstances() > 1) {
      random = data.getRandomNumberGenerator(getSeed());
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

    m_numAttributes = data.numAttributes();

    if (m_filterType == FILTER_STANDARDIZE || m_filterType == FILTER_NORMALIZE) {
      double[][] beforeTransformation = minAndMax(data);
      if (m_filterType == FILTER_STANDARDIZE) {
        m_Filter = new Standardize();
      } else {
        m_Filter = new Normalize();
      }
      m_Filter.setInputFormat(data);
      data = Filter.useFilter(data, m_Filter);
      double[][] afterTransformation = minAndMax(data);
      m_offsets = new double[m_numAttributes];
      m_factors = new double[m_numAttributes];
      for (int j = 0; j < m_numAttributes; j++) {
        if (beforeTransformation[1][j] > beforeTransformation[0][j]) {
          m_factors[j] = (beforeTransformation[1][j] - beforeTransformation[0][j])
            / (afterTransformation[1][j] - afterTransformation[0][j]);
        } else {
          m_factors[j] = 1.0;
        }
        m_offsets[j] = beforeTransformation[0][j] - afterTransformation[0][j]
          * m_factors[j];
      }
    } else {
      m_Filter = null;
      m_offsets = null;
      m_factors = null;
    }

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
    while (m_MLPParameters == null) {
      m_MLPParameters = opt.getVarbValues();
      if (m_Debug) {
        System.out.println("First set of iterations finished, not enough!");
      }
      m_MLPParameters = opt.findArgmin(m_MLPParameters, b);
    }
    if (m_Debug) {
      System.out.println("SE (normalized space) after optimization: "
        + opt.getMinFunction());
    }

    m_data = new Instances(m_data, 0); // Save memory

    // Shut down thread pool
    m_Pool.shutdown();
  }

  /**
   * Calculates the (penalized) squared error based on the current parameter
   * vector.
   */
  protected double calculateSE() {

    // Set up result set, and chunk size
    int chunksize = m_data.numInstances() / m_numThreads;
    ArrayList<Future<Double>> results = new ArrayList<Future<Double>>();

    // For each thread
    for (int j = 0; j < m_numThreads; j++) {

      // Determine batch to be processed
      final int lo = j * chunksize;
      final int hi = (j < m_numThreads - 1) ? (lo + chunksize) : m_data
        .numInstances();

      // Create and submit new job, where each instance in batch is processed
      Future<Double> futureSE = m_Pool.submit(new Callable<Double>() {
        @Override
        public Double call() {
          final double[] outputsHidden = new double[m_numUnits];
          final double[] outputsOut = new double[m_numAttributes];
          double SE = 0;
          for (int k = lo; k < hi; k++) {
            final Instance inst = m_data.instance(k);

            // Calculate necessary input/output values
            calculateOutputsHidden(inst, outputsHidden, null);
            calculateOutputsOut(outputsHidden, outputsOut);

            // Add to squared error
            if (inst instanceof SparseInstance) {
              int instIndex = 0;
              for (int index = 0; index < m_numAttributes; index++) {
                if (instIndex < inst.numValues()
                  && inst.index(instIndex) == index) {
                  final double err = outputsOut[index]
                    - inst.valueSparse(instIndex++);
                  SE += 0.5 * inst.weight() * err * err;
                } else {
                  final double err = outputsOut[index];
                  SE += 0.5 * inst.weight() * err * err;
                }
              }
            } else {
              for (int index = 0; index < m_numAttributes; index++) {
                final double err = outputsOut[index] - inst.value(index);
                SE += 0.5 * inst.weight() * err * err;
              }
            }

            // Do we want to build a contractive autoencoder?
            if (m_useContractive) {

              // Add penalty
              for (int i = 0; i < outputsHidden.length; i++) {
                double sum = 0;
                int offset = i * m_numAttributes;
                for (int index = 0; index < m_numAttributes; index++) {
                  sum += m_MLPParameters[offset + index]
                    * m_MLPParameters[offset + index];
                }
                SE += m_lambda * inst.weight() * 
                  outputsHidden[i] * (1.0 - outputsHidden[i])
                  * outputsHidden[i] * (1.0 - outputsHidden[i]) * sum;
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
          squaredSumOfWeights += m_MLPParameters[offset + j]
            * m_MLPParameters[offset + j];
        }
      }
      SE += m_lambda * squaredSumOfWeights;
    }

    return SE / m_data.sumOfWeights();
  }

  /**
   * Calculates the gradient based on the current parameter vector.
   */
  protected double[] calculateGradient() {

    // Set up result set, and chunk size
    int chunksize = m_data.numInstances() / m_numThreads;
    ArrayList<Future<double[]>> results = new ArrayList<Future<double[]>>();

    // For each thread
    for (int j = 0; j < m_numThreads; j++) {

      // Determine batch to be processed
      final int lo = j * chunksize;
      final int hi = (j < m_numThreads - 1) ? (lo + chunksize) : m_data
        .numInstances();

      // Create and submit new job, where each instance in batch is processed
      Future<double[]> futureGrad = m_Pool.submit(new Callable<double[]>() {
        @Override
        public double[] call() {

          final double[] outputsHidden = new double[m_numUnits];
          final double[] outputsOut = new double[m_numAttributes];
          final double[] deltaHidden = new double[m_numUnits];
          final double[] deltaOut = new double[m_numAttributes];
          final double[] sigmoidDerivativesHidden = new double[m_numUnits];
          final double[] localGrad = new double[m_MLPParameters.length];
          for (int k = lo; k < hi; k++) {
            final Instance inst = m_data.instance(k);
            calculateOutputsHidden(inst, outputsHidden,
              sigmoidDerivativesHidden);
            updateGradient(localGrad, inst, outputsHidden, deltaHidden,
              outputsOut, deltaOut);
            updateGradientForHiddenUnits(localGrad, inst,
              sigmoidDerivativesHidden, deltaHidden);

            // Dow we want to build a contractive autoencoder?
            if (m_useContractive) {

              // Update gradient wrt penalty
              for (int i = 0; i < outputsHidden.length; i++) {
                double sum = 0;
                int offset = i * m_numAttributes;
                for (int index = 0; index < m_numAttributes; index++) {
                  sum += m_MLPParameters[offset + index]
                    * m_MLPParameters[offset + index];
                }
                double multiplier = m_lambda * inst.weight() * 2 * outputsHidden[i]
                  * (1.0 - outputsHidden[i]) * outputsHidden[i]
                  * (1.0 - outputsHidden[i]);
                for (int index = 0; index < m_numAttributes; index++) {
                  localGrad[offset + index] += multiplier
                    * m_MLPParameters[offset + index];
                }
                double multiplier2 = multiplier
                  * (1.0 - 2.0 * outputsHidden[i]) * sum;
                if (inst instanceof SparseInstance) {
                  for (int index = 0; index < inst.numValues(); index++) {
                    localGrad[offset + inst.index(index)] += multiplier2
                      * inst.valueSparse(index);
                  }
                } else {
                  for (int index = 0; index < m_numAttributes; index++) {
                    localGrad[offset + index] += multiplier2
                      * inst.value(index);
                  }
                }

                // Update gradient for hidden unit bias wrt penalty
                localGrad[OFFSET_H_BIASES + i] += multiplier2;
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
        for (int i = 0; i < lg.length; i++) {
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

    double factor = 1.0 / m_data.sumOfWeights();
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
        double empirical = Math.round((val1 - val2) / (2 * EPSILON)
          * MULTIPLIER)
          / MULTIPLIER;
        double derived = Math.round(grad[i] * MULTIPLIER) / MULTIPLIER;
        if (empirical != derived) {
          System.err.println("Empirical gradient: " + empirical
            + " Derived gradient: " + derived);
        }
      }
    }

    return grad;
  }

  /**
   * Update the gradient for the weights in the output layer.
   */
  protected void updateGradient(double[] grad, Instance inst,
    double[] outputsHidden, double[] deltaHidden, double[] outputsOut,
    double[] deltaOut) {

    // Initialise deltaHidden
    Arrays.fill(deltaHidden, 0.0);

    // Get ouputs
    calculateOutputsOut(outputsHidden, outputsOut);

    // Go through all output units
    if (inst instanceof SparseInstance) {
      int instIndex = 0;
      for (int index = 0; index < m_numAttributes; index++) {
        if (instIndex < inst.numValues() && inst.index(instIndex) == index) {
          deltaOut[index] = inst.weight() * (outputsOut[index] - inst.valueSparse(instIndex++));
        } else {
          deltaOut[index] = inst.weight() * outputsOut[index];
        }
      }
    } else {
      for (int j = 0; j < m_numAttributes; j++) {
        deltaOut[j] = inst.weight() * (outputsOut[j] - inst.value(j));
      }
    }

    // Go through all output units
    for (int j = 0; j < m_numAttributes; j++) {

      // Go to next output unit if update too small
      if (deltaOut[j] <= m_tolerance && deltaOut[j] >= -m_tolerance) {
        continue;
      }

      // Update deltaHidden
      for (int i = 0; i < m_numUnits; i++) {
        deltaHidden[i] += deltaOut[j]
          * m_MLPParameters[i * m_numAttributes + j];
      }

      // Update gradient for output weights
      for (int i = 0; i < m_numUnits; i++) {
        grad[i * m_numAttributes + j] += deltaOut[j] * outputsHidden[i];
      }

      // Update gradient for bias
      grad[OFFSET_O_BIASES + j] += deltaOut[j];
    }
  }

  /**
   * Update the gradient for the weights in the hidden layer.
   */
  protected void updateGradientForHiddenUnits(double[] grad, Instance inst,
    double[] sigmoidDerivativesHidden, double[] deltaHidden) {

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
      if (inst instanceof SparseInstance) {
        for (int index = 0; index < inst.numValues(); index++) {
          grad[offset + inst.index(index)] += deltaHidden[i]
            * inst.valueSparse(index);
        }
      } else {
        for (int l = 0; l < m_numAttributes; l++) {
          grad[offset + l] += deltaHidden[i] * inst.value(l);
        }
      }
      grad[OFFSET_H_BIASES + i] += deltaHidden[i];
    }
  }

  /**
   * Calculates the array of outputs of the hidden units.
   */
  protected void calculateOutputsHidden(Instance inst, double[] out, double[] d) {

    for (int i = 0; i < m_numUnits; i++) {
      double sum = 0;
      int offset = i * m_numAttributes;
      if (inst instanceof SparseInstance) {
        for (int index = 0; index < inst.numValues(); index++) {
          sum += inst.valueSparse(index)
            * m_MLPParameters[offset + inst.index(index)];
        }
      } else {
        for (int j = 0; j < m_numAttributes; j++) {
          sum += inst.value(j) * m_MLPParameters[offset + j];
        }
      }
      sum += m_MLPParameters[OFFSET_H_BIASES + i];
      out[i] = sigmoid(-sum, d, i);
    }
  }

  /**
   * Calculates the outputs of output units based on the given hidden layer
   * outputs.
   */
  protected void calculateOutputsOut(double[] outputsHidden, double[] outputsOut) {

    // Initialise outputs
    Arrays.fill(outputsOut, 0.0);

    // Calculate outputs
    for (int i = 0; i < m_numUnits; i++) {
      int offset = i * m_numAttributes;
      for (int j = 0; j < m_numAttributes; j++) {
        outputsOut[j] += m_MLPParameters[offset + j] * outputsHidden[i];
      }
    }

    // Add bias weights
    for (int j = 0; j < m_numAttributes; j++) {
      outputsOut[j] += m_MLPParameters[OFFSET_O_BIASES + j];
    }
  }

  /**
   * Computes approximate sigmoid function. Derivative is stored in second
   * argument at given index if d != null.
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
      x = y * y;
      x *= x;
      x *= x;
      x *= x;
      x *= x;
      x *= x;
      x *= x;
      x *= x;
      x *= x;
      x *= x;
      x *= x;
      x *= x;
      output = 1.0 / (1.0 + x);

      // Compute derivative if desired
      if (d != null) {
        d[index] = output * (1.0 - output) / y;
      }
    }

    return output;
  }

  /**
   * Determines the output format based on the input format and returns this.
   * 
   * @param inputFormat the input format to base the output format on
   * @return the output format
   */
  @Override
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
        atts.add((Attribute) inputFormat.classAttribute().copy());
      }

      Instances outputFormat = new Instances(inputFormat.relationName()
        + "_autoencoded", atts, 0);
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
   * @param instances the data to process
   * @return the modified data
   * @throws Exception in case the processing goes wrong
   */
  @Override
  protected Instances process(Instances instances) throws Exception {

    // Do we need to build the filter model?
    if (!isFirstBatchDone()) {
      initFilter(instances);

      // Write weight vectors into file as instances if desired
      if (!m_WeightsFile.isDirectory()) {
        PrintWriter pw = new PrintWriter(m_WeightsFile);
        pw.println(new Instances(m_data, 0));
        for (int i = 0; i < m_numUnits; i++) {
          int offset = i * m_numAttributes;
          for (int j = 0; j < m_numAttributes; j++) {
            if (j > 0) {
              pw.print(",");
            }
            pw.print(m_MLPParameters[offset + j]);
          }
          pw.println();
        }
        pw.close();
      }
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

      // Filter instance
      if (m_Filter != null) {
        m_Filter.input(inst);
        m_Filter.batchFinished();
        inst = m_Filter.output();
      }

      // Get new values
      double[] outputsHidden = new double[m_numUnits];
      calculateOutputsHidden(inst, outputsHidden, null);

      // Do we want to output reduced data?
      if (!m_outputInOriginalSpace) {

        // Copy over class value if necessary
        if (instances.classIndex() >= 0) {
          double[] newVals = new double[m_numUnits + 1];
          System.arraycopy(outputsHidden, 0, newVals, 0, m_numUnits);
          newVals[newVals.length - 1] = classVal;
          outputsHidden = newVals;
        }
        result.add(new DenseInstance(inst.weight(), outputsHidden));
      } else {
        double[] newVals = new double[instances.numAttributes()];
        double[] outputsOut = new double[m_numAttributes];
        calculateOutputsOut(outputsHidden, outputsOut);
        int j = 0;
        for (int index = 0; index < newVals.length; index++) {
          if (index != instances.classIndex()) {
            if (m_filterType == FILTER_STANDARDIZE
              || m_filterType == FILTER_NORMALIZE) {
              newVals[index] = outputsOut[j] * m_factors[j] + m_offsets[j];
            } else {
              newVals[index] = outputsOut[j];
            }
            j++;
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
   * 
   * @return The string.
   */
  @Override
  public String globalInfo() {

    return "Implements an autoencoder with one hidden layer and tied weights using WEKA's Optimization class"
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
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result
      .setValue(
        Field.AUTHOR,
        "Salah Rifai and Pascal Vincent and Xavier Muller and Xavier Glorot and Yoshua Bengio");
    result
      .setValue(Field.TITLE,
        "Contractive Auto-Encoders: Explicit Invariance During Feature Extraction");
    result.setValue(Field.BOOKTITLE,
      "International Conference on Machine Learning");
    result.setValue(Field.YEAR, "2011");
    result.setValue(Field.PAGES, "833-840");
    result.setValue(Field.PUBLISHER, "Omnipress");

    return result;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "The random number seed to be used.";
  }

  /**
   * Set the seed for random number generation.
   *
   * @param seed the seed
   */
  public void setSeed(int seed) {

    m_Seed = seed;
  }

  /**
   * Gets the seed for the random number generations
   *
   * @return the seed for the random number generation
   */
  public int getSeed() {

    return m_Seed;
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
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String weightsFileTipText() {
    return "The file to write weights to in ARFF format. Nothing is written if this is a directory.";
  }

  /**
   * Gets current weights file.
   * 
   * @return the weights file.
   */
  public File getWeightsFile() {
    return m_WeightsFile;
  }

  /**
   * Sets the weights file to use.
   * 
   * @param value the weights file.
   */
  public void setWeightsFile(File value) {
    m_WeightsFile = value;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String filterTypeTipText() {
    return "Determines how/if the data will be transformed.";
  }

  /**
   * Gets how the training data will be transformed. Will be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   * 
   * @return the filtering mode
   */
  public SelectedTag getFilterType() {

    return new SelectedTag(m_filterType, TAGS_FILTER);
  }

  /**
   * Sets how the training data will be transformed. Should be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   * 
   * @param newType the new filtering mode
   */
  public void setFilterType(SelectedTag newType) {

    if (newType.getTags() == TAGS_FILTER) {
      m_filterType = newType.getSelectedTag().getID();
    }
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(7);

    newVector.addElement(new Option(
      "\tNumber of hidden units (default is 2).\n", "N", 1, "-N <int>"));

    newVector.addElement(new Option(
      "\tLambda factor for penalty on weights (default is 0.01).\n", "L", 1,
      "-L <double>"));
    newVector.addElement(new Option(
      "\tTolerance parameter for delta values (default is 1.0e-6).\n", "O", 1,
      "-O <double>"));
    newVector.addElement(new Option(
      "\tUse conjugate gradient descent (recommended for many attributes).\n",
      "G", 0, "-G"));
    newVector
      .addElement(new Option(
        "\tUse contractive autoencoder instead of autoencoder with weight decay.\n",
        "C", 0, "-C"));
    newVector
      .addElement(new Option(
        "\tUse exact sigmoid function rather than approximation.\n", "X", 0,
        "-X"));
    newVector.addElement(new Option(
      "\tOutput data in original space, so do not output reduced data.\n", "F",
      0, "-F"));
    newVector.addElement(new Option(
      "\t" + poolSizeTipText() + " (default 1)\n", "P", 1, "-P <int>"));
    newVector.addElement(new Option("\t" + numThreadsTipText()
      + " (default 1)\n", "E", 1, "-E <int>"));

    newVector.addElement(new Option(
      "\tThe file to write weight vectors to in ARFF format.\n"
        + "\t(default: none)", "weights-file", 1, "-weights-file <filename>"));

    newVector.addElement(new Option(
      "\tWhether to 0=normalize/1=standardize/2=neither. "
        + "(default 1=standardize)", "S", 1, "-S"));

    newVector.addElement(new Option(
            "\tRandom number seed.\n"
                    + "\t(default 1)",
            "seed", 1, "-seed <num>"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -N &lt;int&gt;
   *  Number of hidden units (default is 2).
   * </pre>
   * 
   * <pre>
   * -L &lt;double&gt;
   *  Lambda factor for penalty on weights (default is 0.01).
   * </pre>
   * 
   * <pre>
   * -O &lt;double&gt;
   *  Tolerance parameter for delta values (default is 1.0e-6).
   * </pre>
   * 
   * <pre>
   * -G
   *  Use conjugate gradient descent (recommended for many attributes).
   * </pre>
   * 
   * <pre>
   * -C
   *  Use contractive autoencoder instead of autoencoder with weight decay.
   * </pre>
   * 
   * <pre>
   * -X
   *  Use exact sigmoid function rather than approximation.
   * </pre>
   * 
   * <pre>
   * -F
   *  Output data in original space, so do not output reduced data.
   * </pre>
   * 
   * <pre>
   * -P &lt;int&gt;
   *  The size of the thread pool, for example, the number of cores in the CPU. (default 1)
   * </pre>
   * 
   * <pre>
   * -E &lt;int&gt;
   *  The number of threads to use, which should be &gt;= size of thread pool. (default 1)
   * </pre>
   * 
   * <pre>
   * -weights-file &lt;filename&gt;
   *  The file to write weight vectors to in ARFF format.
   *  (default: none)
   * </pre>
   * 
   * <pre>
   * -S
   *  Whether to 0=normalize/1=standardize/2=neither. (default 1=standardize)
   * </pre>
   *
   * <pre>
   * -seed &lt;int&gt;
   *  The seed for the random number generator (default is 1).
   * </pre>
   *
   * <!-- options-end -->
   * 
   * Options after -- are passed to the designated filter.
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
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
    String tmpStr = Utils.getOption("weights-file", options);
    if (tmpStr.length() != 0) {
      setWeightsFile(new File(tmpStr));
    } else {
      setWeightsFile(new File(System.getProperty("user.dir")));
    }
    tmpStr = Utils.getOption('S', options);
    if (tmpStr.length() != 0) {
      setFilterType(new SelectedTag(Integer.parseInt(tmpStr), TAGS_FILTER));
    } else {
      setFilterType(new SelectedTag(FILTER_STANDARDIZE, TAGS_FILTER));
    }

    String seed = Utils.getOption("seed", options);
    if (seed.length() != 0) {
      setSeed(Integer.parseInt(seed));
    } else {
      setSeed(1);
    }
    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Filter.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-N");
    options.add("" + getNumFunctions());

    options.add("-L");
    options.add("" + getLambda());

    options.add("-O");
    options.add("" + getTolerance());

    if (m_useCGD) {
      options.add("-G");
    }

    if (m_useContractive) {
      options.add("-C");
    }

    if (m_useExactSigmoid) {
      options.add("-X");
    }

    if (m_outputInOriginalSpace) {
      options.add("-F");
    }

    options.add("-P");
    options.add("" + getPoolSize());

    options.add("-E");
    options.add("" + getNumThreads());

    options.add("-weights-file");
    options.add("" + getWeightsFile());

    options.add("-S");
    options.add("" + m_filterType);

    options.add("-seed");
    options.add("" + getSeed());

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * Main method to run the code from the command-line using the standard WEKA
   * options.
   */
  public static void main(String[] argv) {

    runFilter(new MLPAutoencoder(), argv);
  }
}
