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
 *    KernelLogisticRegression.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions;

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
import weka.classifiers.functions.supportVector.CachedKernel;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.ConjugateGradientOptimization;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Optimization;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.RemoveUseless;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;

/**
 * <!-- globalinfo-start --> This classifier generates a two-class kernel
 * logistic regression model. The model is fit by minimizing the negative
 * log-likelihood with a quadratic penalty using BFGS optimization, as
 * implemented in the Optimization class. Alternatively, conjugate gradient
 * optimization can be applied. The user can specify the kernel function and the
 * value of lambda, the multiplier for the quadractic penalty. Using a linear
 * kernel (the default) this method should give the same result as ridge
 * logistic regression implemented in Logistic, assuming the ridge parameter is
 * set to the same value as lambda, and not too small. By replacing the kernel
 * function, we can learn non-linear decision boundaries.<br/>
 * <br/>
 * Note that the data is filtered using ReplaceMissingValues, RemoveUseless,
 * NominalToBinary, and Standardize (in that order).<br/>
 * <br/>
 * If a CachedKernel is used, this class will overwrite the manually specified
 * cache size and use a full cache instead.<br/>
 * <br/>
 * To apply this classifier to multi-class problems, use the
 * MultiClassClassifier.<br/>
 * <br/>
 * This implementation stores the full kernel matrix at training time for speed
 * reasons.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
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
 * <pre>
 * -K &lt;classname and parameters&gt;
 *  The Kernel to use.
 *  (default: weka.classifiers.functions.supportVector.PolyKernel)
 * </pre>
 * 
 * <pre>
 * -L &lt;double&gt;
 *  The lambda penalty parameter. (default 0.01)
 * </pre>
 * 
 * <pre>
 * -G
 *  Use conjugate gradient descent instead of BFGS.
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
 * Options specific to kernel weka.classifiers.functions.supportVector.PolyKernel:
 * </pre>
 * 
 * <pre>
 * -D
 *  Enables debugging output (if available) to be printed.
 *  (default: off)
 * </pre>
 * 
 * <pre>
 * -no-checks
 *  Turns off all checks - use with caution!
 *  (default: checks on)
 * </pre>
 * 
 * <pre>
 * -C &lt;num&gt;
 *  The size of the cache (a prime number), 0 for full cache and 
 *  -1 to turn it off.
 *  (default: 250007)
 * </pre>
 * 
 * <pre>
 * -E &lt;num&gt;
 *  The Exponent to use.
 *  (default: 1.0)
 * </pre>
 * 
 * <pre>
 * -L
 *  Use lower-order terms.
 *  (default: no)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank
 * @version $Revision: ???? $
 */
public class KernelLogisticRegression extends RandomizableClassifier {

  /** For serialization */
  static final long serialVersionUID = 6332117032546553533L;

  /** Kernel to use **/
  protected Kernel m_kernel = new PolyKernel();

  /** The parameter vector */
  protected double[] m_weights;

  /** A reference to the data */
  protected Instances m_data;

  /** The penalty parameter */
  protected double m_lambda = 0.01;

  /** Whether to use conjugate gradient descent */
  protected boolean m_useCGD = false;

  /** The filters used to preprocess the data */
  protected Standardize m_standardize = new Standardize();
  protected ReplaceMissingValues m_replaceMissing = new ReplaceMissingValues();
  protected NominalToBinary m_nominalToBinary = new NominalToBinary();
  protected RemoveUseless m_removeUseless = new RemoveUseless();

  /** The number of threads to use to calculate gradient and loss */
  protected int m_numThreads = 1;

  /** The size of the thread pool */
  protected int m_poolSize = 1;

  /** Thread pool */
  protected transient ExecutorService m_Pool = null;

  /** The kernel matrix. */
  protected double[][] m_kernelMatrix = null;

  /** The class values in the training data. */
  protected double[] m_classValues = null;

  /**
   * Returns a string describing this classifier
   * 
   * @return a description of the classifier suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {

    return "This classifier generates a two-class kernel logistic regression model. The "
      + "model is fit by minimizing the negative log-likelihood with a quadratic penalty "
      + "using BFGS optimization, as implemented in the Optimization class. "
      + "Alternatively, conjugate gradient optimization can be applied. "
      + "The user can specify the kernel function and the value of lambda, "
      + "the multiplier for the quadractic penalty. Using a linear kernel (the default) "
      + "this method should give the same result as ridge logistic regression "
      + "implemented in Logistic, assuming the ridge parameter is set to the same value "
      + "as lambda, and not too small. By replacing the kernel function, we can "
      + "learn non-linear decision boundaries.\n\n"
      + "Note that the data is filtered using ReplaceMissingValues, RemoveUseless, "
      + "NominalToBinary, and Standardize (in that order).\n\n"
      + "If a CachedKernel is used, this class will overwrite the manually specified "
      + "cache size and use a full cache instead.\n\n"
      + "To apply this classifier to multi-class problems, use the MultiClassClassifier.\n\n"
      + "This implementation stores the full kernel matrix at training time for speed reasons.";
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
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.BINARY_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tThe Kernel to use.\n"
      + "\t(default: weka.classifiers.functions.supportVector.PolyKernel)",
      "K", 1, "-K <classname and parameters>"));

    result.addElement(new Option(
      "\tThe lambda penalty parameter. (default 0.01)", "L", 1, "-L <double>"));

    result.addElement(new Option(
      "\tUse conjugate gradient descent instead of BFGS.\n", "G", 0, "-G"));

    result.addElement(new Option("\t" + poolSizeTipText() + " (default 1)\n",
      "P", 1, "-P <int>"));
    result.addElement(new Option("\t" + numThreadsTipText() + " (default 1)\n",
      "E", 1, "-E <int>"));

    result.addAll(Collections.list(super.listOptions()));

    result.addElement(new Option("", "", 0, "\nOptions specific to kernel "
      + getKernel().getClass().getName() + ":"));

    result
      .addAll(Collections.list(((OptionHandler) getKernel()).listOptions()));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
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
   * <pre>
   * -K &lt;classname and parameters&gt;
   *  The Kernel to use.
   *  (default: weka.classifiers.functions.supportVector.PolyKernel)
   * </pre>
   * 
   * <pre>
   * -L &lt;double&gt;
   *  The lambda penalty parameter. (default 0.01)
   * </pre>
   * 
   * <pre>
   * -G
   *  Use conjugate gradient descent instead of BFGS.
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
   * Options specific to kernel weka.classifiers.functions.supportVector.PolyKernel:
   * </pre>
   * 
   * <pre>
   * -D
   *  Enables debugging output (if available) to be printed.
   *  (default: off)
   * </pre>
   * 
   * <pre>
   * -no-checks
   *  Turns off all checks - use with caution!
   *  (default: checks on)
   * </pre>
   * 
   * <pre>
   * -C &lt;num&gt;
   *  The size of the cache (a prime number), 0 for full cache and 
   *  -1 to turn it off.
   *  (default: 250007)
   * </pre>
   * 
   * <pre>
   * -E &lt;num&gt;
   *  The Exponent to use.
   *  (default: 1.0)
   * </pre>
   * 
   * <pre>
   * -L
   *  Use lower-order terms.
   *  (default: no)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;
    String[] tmpOptions;

    tmpStr = Utils.getOption('L', options);
    if (tmpStr.length() != 0) {
      setLambda(Double.parseDouble(tmpStr));
    } else {
      setLambda(0.01);
    }

    m_useCGD = Utils.getFlag('G', options);

    tmpStr = Utils.getOption('K', options);
    tmpOptions = Utils.splitOptions(tmpStr);
    if (tmpOptions.length != 0) {
      tmpStr = tmpOptions[0];
      tmpOptions[0] = "";
      setKernel(Kernel.forName(tmpStr, tmpOptions));
    }
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

    Utils.checkForRemainingOptions(tmpOptions);
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-K");
    result.add("" + getKernel().getClass().getName() + " "
      + Utils.joinOptions(getKernel().getOptions()));

    result.add("-L");
    result.add("" + getLambda());

    if (m_useCGD) {
      result.add("-G");
    }

    result.add("-P");
    result.add("" + getPoolSize());

    result.add("-E");
    result.add("" + getNumThreads());

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
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
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String lambdaTipText() {

    return "The penalty parameter lambda.";
  }

  /**
   * Get the value of lambda.
   * 
   * @return Value of lambda.
   */
  public double getLambda() {

    return m_lambda;
  }

  /**
   * Set the value of lambda.
   * 
   * @param v Value to assign to lambda.
   */
  public void setLambda(double v) {

    m_lambda = v;
  }

  /**
   * Returns the tip text for this property
   */
  public String kernelTipText() {
    return "The kernel to use.";
  }

  /**
   * Sets the kernel to use.
   */
  public void setKernel(Kernel value) {
    m_kernel = value;
  }

  /**
   * Returns the kernel to use.
   */
  public Kernel getKernel() {
    return m_kernel;
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
   * Returns the loss for the given parameter vector.
   */
  protected double calculateLoss() throws Exception {

    // Set up result set, and chunk size
    final int N = m_classValues.length;
    int chunksize = N / m_numThreads;
    Set<Future<Double>> results = new HashSet<Future<Double>>();

    // For each thread
    for (int k = 0; k < m_numThreads; k++) {

      // Determine batch to be processed
      final int lo = k * chunksize;
      final int hi = (k < m_numThreads - 1) ? (lo + chunksize) : N;

      // Create and submit new job, where each instance in batch is processed
      Future<Double> futureLoss = m_Pool.submit(new Callable<Double>() {
        @Override
        public Double call() throws Exception {
          double loss = 0;
          for (int i = lo; i < hi; i++) {

            // Computed weighted sum
            double weightedSum = 0;
            for (int j = 0; j < N; j++) {
              weightedSum += m_weights[j] * m_kernelMatrix[i][j];
            }

            // Add penalty to loss
            loss += m_lambda * m_weights[i] * weightedSum;

            // Add bias to weighted sum
            weightedSum += m_weights[N];

            // Update negative loglikelihood. Using NLL -= Math.log(class
            // probability) gives numerical problems: need to be a bit
            // careful, so we use the following.
            loss += Math.log(1.0 + Math.exp(-m_classValues[i] * weightedSum));
          }
          return loss;
        }
      });
      results.add(futureLoss);
    }

    // Calculate Loss
    double loss = 0;
    try {
      for (Future<Double> futureLoss : results) {
        loss += futureLoss.get();
      }
    } catch (Exception e) {
      System.out.println("Loss could not be calculated.");
      e.printStackTrace();
    }
    return loss;
  }

  /**
   * Returns the gradient for the given parameter vector.
   */
  protected double[] calculateGradient() throws Exception {

    // Set up result set, and chunk size
    final int N = m_classValues.length;
    int chunksize = N / m_numThreads;
    Set<Future<double[]>> results = new HashSet<Future<double[]>>();

    // For each thread
    for (int k = 0; k < m_numThreads; k++) {

      // Determine batch to be processed
      final int lo = k * chunksize;
      final int hi = (k < m_numThreads - 1) ? (lo + chunksize) : N;

      // Create and submit new job, where each instance in batch is processed
      Future<double[]> futureGrad = m_Pool.submit(new Callable<double[]>() {
        @Override
        public double[] call() throws Exception {

          // Calculate gradient
          double[] grad = new double[N + 1];
          for (int i = lo; i < hi; i++) {

            // Computed weighted sum
            double weightedSum = 0;
            for (int j = 0; j < N; j++) {
              weightedSum += m_weights[j] * m_kernelMatrix[i][j];
            }

            // Update gradient wrt to penalty
            grad[i] += 2 * m_lambda * weightedSum;

            // Add bias to weighted sum
            weightedSum += m_weights[N];

            // Update gradient
            final double multiplier = -m_classValues[i]
              * (1.0 / (1.0 + Math.exp(m_classValues[i] * weightedSum)));
            for (int j = 0; j < N; j++) {
              grad[j] += multiplier * m_kernelMatrix[i][j];
            }

            // Deal with bias term
            grad[N] += multiplier;
          }
          return grad;
        }
      });
      results.add(futureGrad);
    }

    // Calculate final gradient
    double[] grad = new double[N + 1];
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
   * Simple wrapper class needed to use the BFGS method implemented in
   * weka.core.Optimization.
   */
  protected class OptEng extends Optimization {

    /**
     * Returns the squared error given parameter values x.
     */
    @Override
    protected double objectiveFunction(double[] x) throws Exception {

      m_weights = x;
      return calculateLoss();
    }

    /**
     * Returns the gradient given parameter values x.
     */
    @Override
    protected double[] evaluateGradient(double[] x) throws Exception {

      m_weights = x;
      return calculateGradient();
    }

    /**
     * The revision string.
     */
    @Override
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1345 $");
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
    protected double objectiveFunction(double[] x) throws Exception {

      m_weights = x;
      return calculateLoss();
    }

    /**
     * Returns the gradient given parameter values x.
     */
    @Override
    protected double[] evaluateGradient(double[] x) throws Exception {

      m_weights = x;
      return calculateGradient();
    }

    /**
     * The revision string.
     */
    @Override
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 9345 $");
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

    // Filter the data
    m_replaceMissing = new ReplaceMissingValues();
    m_replaceMissing.setInputFormat(m_data);
    m_data = Filter.useFilter(m_data, m_replaceMissing);
    m_removeUseless = new RemoveUseless();
    m_removeUseless.setInputFormat(m_data);
    m_data = Filter.useFilter(m_data, m_removeUseless);
    m_nominalToBinary = new NominalToBinary();
    m_nominalToBinary.setInputFormat(m_data);
    m_data = Filter.useFilter(m_data, m_nominalToBinary);
    m_standardize = new Standardize();
    m_standardize.setInputFormat(m_data);
    m_data = Filter.useFilter(m_data, m_standardize);

    // Initialize kernel
    if (m_kernel instanceof CachedKernel) {
      CachedKernel cachedKernel = (CachedKernel) m_kernel;
      cachedKernel.setCacheSize(-1);
    }
    m_kernel.buildKernel(m_data);

    // Compute the kernel matrix (square array to maximize speed)
    // Also store class values
    m_kernelMatrix = new double[m_data.numInstances()][m_data.numInstances()];
    m_classValues = new double[m_data.numInstances()];

    // Initialise thread pool
    m_Pool = Executors.newFixedThreadPool(m_poolSize);

    // Set up result set, and chunk size
    final int N = m_classValues.length;
    int chunksize = N / m_numThreads;
    Set<Future<Void>> results = new HashSet<Future<Void>>();

    // For each thread
    for (int k = 0; k < m_numThreads; k++) {

      // Determine batch to be processed
      final int lo = k * chunksize;
      final int hi = (k < m_numThreads - 1) ? (lo + chunksize) : N;

      // Create and submit new job, where each instance in batch is processed
      Future<Void> futureMat = m_Pool.submit(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          for (int i = lo; i < hi; i++) {
            for (int j = 0; j < m_data.numInstances(); j++) {
              if ((j >= i) || (m_numThreads > 1)) {
                m_kernelMatrix[i][j] = m_kernel.eval(-1, i, m_data.instance(j));
              } else {
                m_kernelMatrix[i][j] = m_kernelMatrix[j][i];
              }
            }
            m_classValues[i] = 2.0 * m_data.instance(i).classValue() - 1.0;
          }
          return null;
        }
      });
      results.add(futureMat);
    }

    try {
      for (Future<Void> futureMat : results) {
        futureMat.get();
      }
    } catch (Exception e) {
      System.out.println("Kernel matrix could not be calculated.");
    }

    // Initialize weight vector
    m_weights = new double[m_data.numInstances() + 1];
    double initializer = 1.0 / m_data.numInstances();
    for (int i = 0; i < m_data.numInstances(); i++) {
      m_weights[i] = (m_data.instance(i).classValue() == 0.0) ? -initializer
        : initializer;
    }

    // Initialize bias
    double[] Nc = new double[2];
    for (int i = 0; i < m_data.numInstances(); i++) {
      Nc[(int) m_data.instance(i).classValue()]++;
    }
    m_weights[m_data.numInstances()] = Math.log(Nc[1] + 1.0)
      - Math.log(Nc[0] + 1.0);

    // We don't want to impose any constraints on the parameters
    double[][] b = new double[2][m_weights.length];
    for (int p = 0; p < m_weights.length; p++) {
      b[0][p] = Double.NaN;
      b[1][p] = Double.NaN;
    }

    // Run optimisation
    Optimization opt = null;
    if (m_useCGD) {
      opt = new OptEngCGD();
    } else {
      opt = new OptEng();
    }
    opt.setDebug(m_Debug);

    m_weights = opt.findArgmin(m_weights, b);
    while (m_weights == null) {
      m_weights = opt.getVarbValues();
      if (m_Debug) {
        System.out.println("First set of iterations finished, not enough!");
      }
      m_weights = opt.findArgmin(m_weights, b);
    }

    // Don't need kernel matrix anymore
    m_kernelMatrix = null;

    // Shut down thread pool
    m_Pool.shutdown();

    // Save memory (can't use Kernel.clean() because of polynominal kernel with
    // exponent 1)
    if (m_kernel instanceof CachedKernel) {
      m_kernel = Kernel.makeCopy(m_kernel);
      ((CachedKernel) m_kernel).setCacheSize(-1);
      m_kernel.buildKernel(m_data);
    }
  }

  /**
   * Method for applying the classifier to a test instance.
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {

    // Filter instance
    m_replaceMissing.input(inst);
    inst = m_replaceMissing.output();
    m_removeUseless.input(inst);
    inst = m_removeUseless.output();
    m_nominalToBinary.input(inst);
    inst = m_nominalToBinary.output();
    m_standardize.input(inst);
    inst = m_standardize.output();

    // Computed weighted sum
    double weightedSum = m_weights[m_data.numInstances()];
    for (int j = 0; j < m_data.numInstances(); j++) {
      weightedSum += m_weights[j] * m_kernel.eval(-1, j, inst);
    }
    double[] dist = new double[2];
    dist[1] = 1.0 / (1.0 + Math.exp(-weightedSum));
    dist[0] = 1.0 - dist[1];
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
    String s = "\nlog(p / (1 - p))\t=\n";
    for (int i = 0; i < m_data.numInstances(); i++) {
      String name = "(standardized) X" + (i + 1);
      if (i > 0) {
        s += "\t+  ";
      } else {
        s += "\t   ";
      }
      s += Utils.doubleToString(m_weights[i], 4) + "   \t* " + name + "\n";
    }
    s += "\t+  " + Utils.doubleToString(m_weights[m_data.numInstances()], 4)
      + "\n";
    return s;
  }

  /**
   * Need to implement this as well....
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: ???? $");
  }

  /**
   * The main method for running this class from the command-line.
   */
  public static void main(String[] args) {
    runClassifier(new KernelLogisticRegression(), args);
  }
}
