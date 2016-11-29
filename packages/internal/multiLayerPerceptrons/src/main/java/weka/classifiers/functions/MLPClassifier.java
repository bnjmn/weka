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
 *    MLPClassifier.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.functions;

import java.util.Arrays;
import java.util.Random;

import weka.classifiers.functions.activation.ActivationFunction;
import weka.classifiers.functions.activation.ApproximateSigmoid;
import weka.classifiers.functions.activation.Sigmoid;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

/**
 * <!-- globalinfo-start -->
 * Trains a multilayer perceptron with one hidden layer using WEKA's Optimization class by minimizing the given loss function plus a quadratic penalty with the BFGS method. Note that all attributes are standardized, including the target. There are several parameters. The ridge parameter is used to determine the penalty on the size of the weights. The number of hidden units can also be specified. Note that large numbers produce long training times. Finally, it is possible to use conjugate gradient descent rather than BFGS updates, which may be faster for cases with many parameters. To improve speed, an approximate version of the logistic function is used as the default activation function for the hidden layer, but other activation functions can be specified. In the output layer, the sigmoid function is used for classification. If the approximate sigmoid is specified for the hidden layers, it is also used for the output layer. For regression, the identity function is used activation function in the output layer. Also, if delta values in the backpropagation step are within the user-specified tolerance, the gradient is not updated for that particular instance, which saves some additional time. Parallel calculation of loss function and gradient is possible when multiple CPU cores are present. Data is split into batches and processed in separate threads in this case. Note that this only improves runtime for larger datasets. Nominal attributes are processed using the unsupervised NominalToBinary filter and missing values are replaced globally using ReplaceMissingValues.
 * <br><br>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -N &lt;int&gt;
 *  Number of hidden units (default is 2).</pre>
 * 
 * <pre> -R &lt;double&gt;
 *  Ridge factor for quadratic penalty on weights (default is 0.01).</pre>
 * 
 * <pre> -O &lt;double&gt;
 *  Tolerance parameter for delta values (default is 1.0e-6).</pre>
 * 
 * <pre> -G
 *  Use conjugate gradient descent (recommended for many attributes).</pre>
 * 
 * <pre> -P &lt;int&gt;
 *  The size of the thread pool, for example, the number of cores in the CPU. (default 1)</pre>
 * 
 * <pre> -E &lt;int&gt;
 *  The number of threads to use, which should be &gt;= size of thread pool. (default 1)</pre>
 * 
 * <pre> -L &lt;classname and parameters&gt;
 *  The loss function to use.
 *  (default: weka.classifiers.functions.loss.SquaredError)</pre>
 * 
 * <pre> -A &lt;classname and parameters&gt;
 *  The activation function to use.
 *  (default: weka.classifiers.functions.activation.ApproximateSigmoid)</pre>
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
 * <pre> -num-decimal-places
 *  The number of decimal places for the output of numbers in the model (default 2).</pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class MLPClassifier extends MLPModel implements WeightedInstancesHandler {

  /** For serialization */
  private static final long serialVersionUID = -3297474276438394644L;

  // The activation function to use in the output layer (depends on the data)
  protected ActivationFunction m_OutputActivationFunction = null;

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Method used to pre-process the data, perform clustering, and set the
   * initial parameter vector.
   */
  protected Instances initializeClassifier(Instances data, Random random) throws Exception {

    data = super.initializeClassifier(data, random);

    if (m_ActivationFunction instanceof ApproximateSigmoid) {
      m_OutputActivationFunction = new ApproximateSigmoid();
    } else {
      m_OutputActivationFunction = new Sigmoid();
    }

    if (data != null) {
      // Standardize data
      m_Filter = new Standardize();
      m_Filter.setInputFormat(data);
      data = Filter.useFilter(data, m_Filter);
   }
    return data;
  }

  /**
   * Calculates the error for one instance.
   *
   * @param outputs outputs of hidden layer
   * @param inst the instance to calculate the error for
   *
   * @return the error value
   */
  protected  double calculateErrorForOneInstance(double[] outputs, Instance inst) {

    // For all class values
    double sum = 0;
    for (int i = 0; i < m_numClasses; i++) {
      sum += m_Loss.loss(m_OutputActivationFunction.activation(getOutput(i, outputs), null, 0),
              ((int) inst.value(m_classIndex) == i) ? 0.99 : 0.01);
    }
    return inst.weight() * sum;
  }

  /**
   * Compute delta for output unit j.
   */
  protected double[] computeDeltas(Instance inst, double[] outputs) {

    // An array we can use to pass parameters
    double[] activationDerivativeOutput = new double[1];

    // Array for deltas
    double[] deltas = new double[inst.numClasses()];
    Arrays.fill(deltas, inst.weight());

    // Calculate delta from output unit
    for (int i = 0; i < deltas.length; i++) {
      deltas[i] *= m_Loss.derivative(m_OutputActivationFunction.activation(getOutput(i, outputs),
                      activationDerivativeOutput, 0),
              ((int) inst.value(m_classIndex) == i) ? 0.99 : 0.01) * activationDerivativeOutput[0];
    }
    return deltas;
  }

  /**
   * Postprocess distribution for prediction.
   */
  protected double[] postProcessDistribution(double[] dist) {

    for (int i = 0; i < m_numClasses; i++) {
      dist[i] = m_ActivationFunction.activation(dist[i], null, 0);
      if (dist[i] < 0) {
        dist[i] = 0;
      } else if (dist[i] > 1) {
        dist[i] = 1;
      }
    }
    double sum = 0;
    for (double d : dist) {
      sum += d;
    }
    if (sum > 0) { // We can get underflows for all classes.
      Utils.normalize(dist, sum);
      return dist;
    } else {
      return null;
    }
  }

  /**
   * Returns the model type as a string.
   */
  public String modelType() {

    return "MLPClassifier";
  }

  /**
   * Main method to run the code from the command-line using the standard WEKA
   * options.
   */
  public static void main(String[] argv) {

    runClassifier(new MLPClassifier(), argv);
  }
}

