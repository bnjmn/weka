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
 * NonNegativeLogisticRegression.java
 * Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.functions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import weka.classifiers.RandomizableClassifier;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Optimization;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * <!-- globalinfo-start --> Class for learning a logistic regression model that
 * has non-negative coefficients. The first class value is assumed to be the
 * positive class value (i.e. 1.0).
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
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
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public class NonNegativeLogisticRegression extends RandomizableClassifier {

  /** For serialization */
  static final long serialVersionUID = -1223158323933117974L;

  /** The parameter vector */
  protected double[] m_weights;

  /** A reference to the data */
  protected Instances m_data;

  /** Data matrix for training */
  protected double[][] m_matrix;

  /** The number of threads to use to calculate gradient and squared error */
  protected int m_numThreads = 1;

  /** The size of the thread pool */
  protected int m_poolSize = 1;

  /** Thread pool */
  protected transient ExecutorService m_Pool = null;

  /**
   * Returns a string describing this classifier.
   * 
   * @return a description of the classifier suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for learning a logistic regression model that has "
      + "non-negative coefficients. The first class value is assumed "
      + "to be the positive class value (i.e. 1.0).";
  }

  /**
   * Returns deep copy of the coefficients that have been learned.
   */
  public double[] getCoefficients() {
    return Arrays.copyOf(m_weights, m_weights.length);
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
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(2);

    newVector.addElement(new Option(
      "\t" + poolSizeTipText() + " (default 1)\n", "P", 1, "-P <int>"));
    newVector.addElement(new Option("\t" + numThreadsTipText()
      + " (default 1)\n", "E", 1, "-E <int>"));

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
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * Options after -- are passed to the designated classifier.
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

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

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-P");
    options.add("" + getPoolSize());

    options.add("-E");
    options.add("" + getNumThreads());

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * Returns capabilities of the classifier (i.e. what type of data the
   * classifier can handle).
   */
  @Override
  public Capabilities getCapabilities() {

    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);

    // class
    result.enable(Capability.BINARY_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * A subclass of Optimization.java that specifies the objective function and
   * the gradient for this particular optimization problem.
   */
  protected class OptEng extends Optimization {

    /**
     * Returns the negative log-likelihood for the given parameter vector.
     */
    @Override
    protected double objectiveFunction(double[] weights) throws Exception {

      // Set weights of model to weights that we want to evaluate
      m_weights = weights;

      // Set up result set, and chunk size
      int chunksize = m_matrix.length / m_numThreads;
      Set<Future<Double>> results = new HashSet<Future<Double>>();

      // For each thread
      for (int j = 0; j < m_numThreads; j++) {

        // Determine batch to be processed
        final int lo = j * chunksize;
        final int hi = (j < m_numThreads - 1) ? (lo + chunksize)
          : m_matrix.length;

        // Create and submit new job, where each instance in batch is processed
        Future<Double> futureNLL = m_Pool.submit(new Callable<Double>() {
          @Override
          public Double call() {

            // Calculate NLL
            double NLL = 0;
            for (int i = lo; i < hi; i++) {

              // Calculate weighted usm (weight for class assumed to be zero)
              double weightedSum = 0;
              for (int j = 0; j < m_matrix[i].length; j++) {
                weightedSum += m_weights[j] * m_matrix[i][j];
              }

              // Update negative loglikelihood. Using NLL -= Math.log(class
              // probability) gives overflow problems: need to be a bit
              // careful, so we use the following.
              NLL -= (-weightedSum * m_matrix[i][m_data.classIndex()])
                - Math.log(1.0 + Math.exp(-weightedSum));
            }
            return NLL;
          }
        });
        results.add(futureNLL);
      }

      // Calculate NLL
      double NLL = 0;
      try {
        for (Future<Double> futureNLL : results) {
          NLL += futureNLL.get();
        }
      } catch (Exception e) {
        System.out.println("NLL could not be calculated.");
      }
      return NLL;
    }

    /**
     * Returns the gradient for the given parameter vector.
     */
    @Override
    protected double[] evaluateGradient(double[] weights) throws Exception {

      // Set weights of model to weights that we want to evaluate
      m_weights = weights;

      // Set up result set, and chunk size
      int chunksize = m_matrix.length / m_numThreads;
      Set<Future<double[]>> results = new HashSet<Future<double[]>>();

      // For each thread
      for (int j = 0; j < m_numThreads; j++) {

        // Determine batch to be processed
        final int lo = j * chunksize;
        final int hi = (j < m_numThreads - 1) ? (lo + chunksize)
          : m_matrix.length;

        // Create and submit new job, where each instance in batch is processed
        Future<double[]> futureGrad = m_Pool.submit(new Callable<double[]>() {
          @Override
          public double[] call() {

            // Calculate gradient
            double[] grad = new double[m_data.numAttributes()];
            for (int i = lo; i < hi; i++) {

              // Calculate weighted sum (weight for class assumed to be zero)
              double weightedSum = 0;
              for (int j = 0; j < m_matrix[i].length; j++) {
                weightedSum += m_weights[j] * m_matrix[i][j];
              }

              // Update gradient
              double classVal = m_matrix[i][m_data.classIndex()];
              double expTerm = Math.exp(-weightedSum);
              for (int j = 0; j < m_matrix[i].length; j++) {
                if (j != m_data.classIndex()) {
                  if (classVal == 0.0) {
                    grad[j] -= (expTerm / (1.0 + expTerm)) * m_matrix[i][j];
                  } else {
                    grad[j] += (1.0 / (1.0 + expTerm)) * m_matrix[i][j];
                    ;
                  }
                }
              }
            }
            return grad;
          }
        });
        results.add(futureGrad);
      }

      // Calculate final gradient
      double[] grad = new double[m_data.numAttributes()];
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
      return grad;
    }

    /**
     * Need to implement this as well....
     */
    @Override
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1111 $");
    }
  }

  /**
   * Method for building the classifier from training data.
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

    // Can classifier handle the data?
    getCapabilities().testWithFail(data);

    // Make shallow copy of data
    m_data = new Instances(data);

    // Delete instances with a missing class value
    m_data.deleteWithMissingClass();

    // Get random number generator
    Random rand = m_data.getRandomNumberGenerator(getSeed());

    // Randomize the data
    m_data.randomize(rand);

    // Create data matrix and squash values
    m_matrix = new double[data.numInstances()][data.numAttributes()];
    double[] scalingFactors = new double[m_data.numAttributes()];
    for (int i = 0; i < m_data.numInstances(); i++) {
      Instance inst = m_data.instance(i);
      for (int j = 0; j < m_data.numAttributes(); j++) {
        m_matrix[i][j] = inst.value(j);
        double absValue = Math.abs(m_matrix[i][j]);
        if (i == 0 || absValue > scalingFactors[j]) {
          scalingFactors[j] = absValue;
        }
      }
    }
    for (int i = 0; i < m_data.numInstances(); i++) {
      for (int j = 0; j < m_data.numAttributes(); j++) {
        if (scalingFactors[j] > 0 && j != m_data.classIndex()) {
          m_matrix[i][j] /= scalingFactors[j];
        }
      }
    }

    // Store reference to data (header information only)
    m_data = new Instances(m_data, 0);

    // Initialize weight vector
    m_weights = new double[m_data.numAttributes()];
    for (int i = 0; i < m_weights.length; i++) {
      if (i != m_data.classIndex() && scalingFactors[i] > 0) {
        m_weights[i] = 1.0 / (m_weights.length - 1);
      }
    }

    // We don't want to impose any constraints on the parameters
    double[][] b = new double[2][m_weights.length];
    for (int p = 0; p < m_weights.length; p++) {
      if (p == m_data.classIndex()) {
        b[0][p] = Double.NaN;
      } else {
        b[0][p] = 0;
      }
      b[1][p] = Double.NaN;
    }

    // Initialise thread pool
    m_Pool = Executors.newFixedThreadPool(m_poolSize);

    // Run BFGS-based optimisation
    OptEng opt = new OptEng();
    opt.setDebug(m_Debug);
    m_weights = opt.findArgmin(m_weights, b);
    while (m_weights == null) {
      m_weights = opt.getVarbValues();
      if (m_Debug) {
        System.out.println("First set of iterations finished, not enough!");
      }
      m_weights = opt.findArgmin(m_weights, b);
    }

    // Shut down thread pool
    m_Pool.shutdown();

    // Rescale weights
    for (int i = 0; i < m_weights.length; i++) {
      if (i != m_data.classIndex() && scalingFactors[i] > 0) {
        m_weights[i] /= scalingFactors[i];
      }
    }

    // Free memory
    m_matrix = null;
  }

  /**
   * Method for applying the classifier to a test instance.
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {

    double sum = 0;
    for (int i = 0; i < inst.numAttributes(); i++) {
      if (i != m_data.classIndex()) {
        sum += m_weights[i] * inst.value(i);
      }
    }
    double[] dist = new double[2];
    dist[0] = 1 / (1 + Math.exp(-sum));
    dist[1] = 1 - dist[0];
    return dist;
  }

  /**
   * Outputs description of classifier as a string.
   */
  @Override
  public String toString() {

    if (m_data == null) {
      return "Classifier not built yet.";
    }
    String s = "\nlog(x / (1 - x))\t=\n";
    for (int i = 0; i < m_data.numAttributes(); i++) {
      String name = "1";
      if (i != m_data.classIndex()) {
        name = m_data.attribute(i).name();
        if (i > 0) {
          s += "\t+  ";
        } else {
          s += "\t   ";
        }
        s += name + "   \t* " + Utils.doubleToString(m_weights[i], 6) + "\n";
      }
    }
    return s;
  }

  /**
   * Main method for running classifier from the command-line.
   */
  public static void main(String[] args) {

    runClassifier(new NonNegativeLogisticRegression(), args);
  }
}
