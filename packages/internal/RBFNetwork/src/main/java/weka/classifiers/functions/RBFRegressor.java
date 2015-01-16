/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    RBFRegressor.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.functions;

import java.util.Arrays;
import java.util.Random;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.WeightedInstancesHandler;

/**
 * <!-- globalinfo-start -->
 * Class implementing radial basis function networks, trained in a fully supervised manner using WEKA's Optimization class by minimizing squared error with the BFGS method. Note that all attributes are normalized into the [0,1] scale.<br/>
 * <br/>
 * The initial centers for the Gaussian radial basis functions are found using WEKA's SimpleKMeans. The initial sigma values are set to the maximum distance between any center and its nearest neighbour in the set of centers.<br/>
 * <br/>
 * There are several parameters. The ridge parameter is used to penalize the size of the weights in the output layer. The number of basis functions can also be specified. Note that large numbers produce long training times. Another option determines whether one global sigma value is used for all units (fastest), whether one value is used per unit (common practice, it seems, and set as the default), or a different value is learned for every unit/attribute combination. It is also possible to learn attribute weights for the distance function. (The square of the value shown in the output is used.)  Finally, it is possible to use conjugate gradient descent rather than BFGS updates, which can be faster for cases with many parameters, and to use normalized basis functions instead of unnormalized ones.<br/>
 * <br/>
 * To improve speed, an approximate version of the logistic function is used as the activation function in the output layer. Also, if delta values in the backpropagation step are within the user-specified tolerance, the gradient is not updated for that particular instance, which saves some additional time.<br/>
 * <br/>
 * Paralled calculation of squared error and gradient is possible when multiple CPU cores are present. Data is split into batches and processed in separate threads in this case. Note that this only improves runtime for larger datasets.<br/>
 * <br/>
 * Nominal attributes are processed using the unsupervised  NominalToBinary filter and missing values are replaced globally using ReplaceMissingValues.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Eibe Frank (2014). Fully supervised training of Gaussian radial basis function networks in WEKA. Department of Computer Science, University of Waikato.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N &lt;int&gt;
 *  Number of Gaussian basis functions (default is 2).
 * </pre>
 * 
 * <pre> -R &lt;double&gt;
 *  Ridge factor for quadratic penalty on output weights (default is 0.01).
 * </pre>
 * 
 * <pre> -L &lt;double&gt;
 *  Tolerance parameter for delta values (default is 1.0e-6).
 * </pre>
 * 
 * <pre> -C &lt;1|2|3&gt;
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
 * <pre> -P &lt;int&gt;
 *  The size of the thread pool, for example, the number of cores in the CPU. (default 1)
 * </pre>
 * 
 * <pre> -E &lt;int&gt;
 *  The number of threads to use, which should be &gt;= size of thread pool. (default 1)
 * </pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class RBFRegressor extends RBFModel implements WeightedInstancesHandler {

  /** For serialization */
  private static final long serialVersionUID = -7847474276438394611L;

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Initialise output layer.
   */
  @Override
  protected void initializeOutputLayer(Random random) {

    for (int i = 0; i < m_numUnits + 1; i++) {
      m_RBFParameters[OFFSET_WEIGHTS + i] = (random.nextDouble() - 0.5) / 2;
    }
  }

  /**
   * Calculates error for single instance.
   */
  @Override
  protected double calculateError(double[] outputs, Instance inst) {

    final double err = getOutput(outputs) - inst.classValue();

    // Add to squared error
    return inst.weight() * err * err;
  }

  /**
   * Postprocess squared error if desired.
   */
  @Override
  protected double postprocessError(double error) {

    // Calculate squared sum of weights
    double squaredSumOfWeights = 0;
    for (int k = 0; k < m_numUnits; k++) {
      squaredSumOfWeights += m_RBFParameters[OFFSET_WEIGHTS + k]
        * m_RBFParameters[OFFSET_WEIGHTS + k];
    }

    return error + m_ridge * squaredSumOfWeights;
  }

  /**
   * Postprocess gradient if desired.
   */
  @Override
  protected void postprocessGradient(double[] grad) {

    // For each output weight, include effect of ridge
    for (int k = 0; k < m_numUnits; k++) {
      grad[OFFSET_WEIGHTS + k] += m_ridge * 2
        * m_RBFParameters[OFFSET_WEIGHTS + k];
    }
  }

  /**
   * Update the gradient for the weights in the output layer.
   */
  @Override
  protected void updateGradient(double[] grad, Instance inst, double[] outputs,
    double[] derivativesOutputs, double[] deltaHidden) {

    // Initialise deltaHidden
    Arrays.fill(deltaHidden, 0.0);

    // Calculate delta from output unit
    double deltaOut = inst.weight() * (getOutput(outputs) - inst.classValue());

    // Go to next output unit if update too small
    if (deltaOut <= m_tolerance && deltaOut >= -m_tolerance) {
      return;
    }

    // Establish offset
    int offsetOW = OFFSET_WEIGHTS;

    // Update deltaHidden
    for (int i = 0; i < m_numUnits; i++) {
      deltaHidden[i] += deltaOut * m_RBFParameters[offsetOW + i];
    }

    // Update gradient for output weights
    for (int i = 0; i < m_numUnits; i++) {
      grad[offsetOW + i] += deltaOut * outputs[i];
    }

    // Update gradient for bias
    grad[offsetOW + m_numUnits] += deltaOut;
  }

  /**
   * Calculates the output of the network based on the given hidden layer
   * outputs.
   */
  protected double getOutput(double[] outputs) {

    double result = 0;
    for (int i = 0; i < m_numUnits; i++) {
      result += m_RBFParameters[OFFSET_WEIGHTS + i] * outputs[i];
    }
    result += m_RBFParameters[OFFSET_WEIGHTS + m_numUnits];
    return result;
  }

  /**
   * Gets output "distribution" based on hidden layer outputs.
   */
  @Override
  protected double[] getDistribution(double[] outputs) {

    double[] dist = new double[1];
    dist[0] = getOutput(outputs) * m_x1 + m_x0;
    return dist;
  }

  /**
   * Outputs the network as a string.
   */
  @Override
  public String toString() {

    if (m_RBFParameters == null) {
      return "Classifier not built yet.";
    }

    String s = "";

    for (int i = 0; i < m_numUnits; i++) {
      s += "\n\nOutput weight: " + m_RBFParameters[OFFSET_WEIGHTS + i];
      s += "\n\nUnit center:\n";
      for (int j = 0; j < m_numAttributes; j++) {
        if (j != m_classIndex) {
          s += m_RBFParameters[OFFSET_CENTERS + (i * m_numAttributes) + j]
            + "\t";
        }
      }
      if (m_scaleOptimizationOption == USE_SCALE_PER_UNIT_AND_ATTRIBUTE) {
        s += "\n\nUnit scales:\n";
        for (int j = 0; j < m_numAttributes; j++) {
          if (j != m_classIndex) {
            s += m_RBFParameters[OFFSET_SCALES + (i * m_numAttributes) + j]
              + "\t";
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
      for (int j = 0; j < m_numAttributes; j++) {
        if (j != m_classIndex) {
          s += m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] + "\t";
        }
      }
    }
    s += "\n\nBias weight: " + m_RBFParameters[OFFSET_WEIGHTS + m_numUnits];

    return s;
  }

  /**
   * Main method to run the code from the command-line using the standard WEKA
   * options.
   */
  public static void main(String[] argv) {

    runClassifier(new RBFRegressor(), argv);
  }
}

