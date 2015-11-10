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

import java.util.Random;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
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
public class MLPRegressor extends MLPModel implements WeightedInstancesHandler {

  /** For serialization */
  private static final long serialVersionUID = -4477474276438394655L;

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
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  // Two values need to convert target/class values back into original scale
  protected double m_x1 = 1.0;
  protected double m_x0 = 0.0;

  /**
   * Method used to pre-process the data, perform clustering, and set the
   * initial parameter vector.
   */
  protected Instances initializeClassifier(Instances data, Random random) throws Exception {

    data = super.initializeClassifier(data, random);

    if (data != null) {
      // Get a couple of target values to figure out the effect of standardization
      double y0 = data.instance(0).classValue();
      int index = 1;
      while (index < data.numInstances()
              && data.instance(index).classValue() == y0) {
        index++;
      }
      if (index == data.numInstances()) {
        // degenerate case, all class values are equal
        // we don't want to deal with this, too much hassle
        throw new Exception(
                "All class values are the same. At least two class values should be different");
      }
      double y1 = data.instance(index).classValue();

      // Standardize data
      m_Filter = new Standardize();
      ((Standardize) m_Filter).setIgnoreClass(true);
      m_Filter.setInputFormat(data);
      data = Filter.useFilter(data, m_Filter);
      double z0 = data.instance(0).classValue();
      double z1 = data.instance(index).classValue();
      m_x1 = (y0 - y1) / (z0 - z1); // no division by zero, since y0 != y1
      // guaranteed => z0 != z1 ???
      m_x0 = (y0 - m_x1 * z0); // = y1 - m_x1 * z1
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

    return inst.weight() * m_Loss.loss(getOutput(0, outputs), inst.value(m_classIndex));
  }

  /**
   * Compute delta for output units.
   */
  protected double[] computeDeltas(Instance inst, double[] outputs) {

    double[] deltas = new double[1];
    deltas[0] = inst.weight() * m_Loss.derivative(getOutput(0, outputs), inst.value(m_classIndex));
    return deltas;
  }

  /**
   * Postprocess distribution for prediction.
   */
  protected double[] postProcessDistribution(double[] dist) {

    dist[0] = dist[0] * m_x1 + m_x0;

    return dist;
  }

  /**
   * Returns the model type as a string.
   */
  public String modelType() {

    return "MPRegressor";
  }

  /**
   * Main method to run the code from the command-line using the standard WEKA
   * options.
   */
  public static void main(String[] argv) {

    runClassifier(new MLPRegressor(), argv);
  }
}

