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
 *    RBFModel.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
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

import weka.classifiers.Classifier;
import weka.classifiers.RandomizableClassifier;
import weka.clusterers.SimpleKMeans;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.ConjugateGradientOptimization;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Optimization;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.RemoveUseless;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 * Abstract super class that can be extended by sub classes that learn RBF
 * models.
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8966 $
 */
public abstract class RBFModel extends RandomizableClassifier implements TechnicalInformationHandler {

  /** For serialization */
  private static final long serialVersionUID = -7847473336438394611L;

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    return result;
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

    result = new TechnicalInformation(TechnicalInformation.Type.TECHREPORT);
    result.setValue(TechnicalInformation.Field.AUTHOR, "Eibe Frank");
    result.setValue(TechnicalInformation.Field.TITLE,
            "Fully supervised training of Gaussian radial basis function networks in WEKA");
    result.setValue(TechnicalInformation.Field.YEAR, "2014");
    result.setValue(TechnicalInformation.Field.NUMBER, "04/14");
    result.setValue(TechnicalInformation.Field.ADDRESS, "Department of Computer Science, University of Waikato");

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

      m_RBFParameters = x;
      return calculateSE();
    }

    /**
     * Returns the gradient given parameter values x.
     */
    @Override
    protected double[] evaluateGradient(double[] x) {

      m_RBFParameters = x;
      return calculateGradient();
    }

    /**
     * The revision string.
     */
    @Override
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 8966 $");
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

      m_RBFParameters = x;
      return calculateSE();
    }

    /**
     * Returns the gradient given parameter values x.
     */
    @Override
    protected double[] evaluateGradient(double[] x) {

      m_RBFParameters = x;
      return calculateGradient();
    }

    /**
     * The revision string.
     */
    @Override
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 8966 $");
    }
  }

  /** Constants for scale optimization options */
  public static final int USE_GLOBAL_SCALE = 1;
  public static final int USE_SCALE_PER_UNIT = 2;
  public static final int USE_SCALE_PER_UNIT_AND_ATTRIBUTE = 3;
  public static final Tag[] TAGS_SCALE = {
    new Tag(USE_GLOBAL_SCALE, "Use global scale"),
    new Tag(USE_SCALE_PER_UNIT, "Use scale per unit"),
    new Tag(USE_SCALE_PER_UNIT_AND_ATTRIBUTE,
      "Use scale per unit and attribute") };

  // The chosen scale optimization option
  protected int m_scaleOptimizationOption = USE_SCALE_PER_UNIT;

  // The number of units
  protected int m_numUnits = 2;

  // The class index of the dataset
  protected int m_classIndex = -1;

  // A reference to the actual data
  protected Instances m_data = null;

  // The number of attributes in the data
  protected int m_numAttributes = -1;

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

  // Tolerance parameter for delta values
  protected double m_tolerance = 1.0e-6;

  // The number of threads to use to calculate gradient and squared error
  protected int m_numThreads = 1;

  // The size of the thread pool
  protected int m_poolSize = 1;

  // The normalization filer
  protected Filter m_Filter = null;

  // The offsets for the different components in the parameter vector
  protected int OFFSET_WEIGHTS = -1;
  protected int OFFSET_SCALES = -1;
  protected int OFFSET_CENTERS = -1;
  protected int OFFSET_ATTRIBUTE_WEIGHTS = -1;

  // An attribute filter
  protected RemoveUseless m_AttFilter;

  // The filter used to make attributes numeric.
  protected NominalToBinary m_NominalToBinary;

  // The filter used to get rid of missing values.
  protected ReplaceMissingValues m_ReplaceMissingValues;

  // A ZeroR model in case no model can be built from the data
  protected Classifier m_ZeroR;

  // Thread pool
  protected transient ExecutorService m_Pool = null;

  // The number of classes in the data
  protected int m_numClasses = -1;

  // Two values need to convert target/class values back into original scale (in
  // regression case)
  protected double m_x1 = 1.0;
  protected double m_x0 = 0.0;

  /**
   * Method used to pre-process the data, perform clustering, and set the
   * initial parameter vector.
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

    double y0 = data.instance(0).classValue(); // This stuff is not relevant in
                                               // classification case
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
      System.err
        .println("Cannot build model (only class attribute present in data after removing useless attributes!), "
          + "using ZeroR model instead!");
      m_ZeroR = new weka.classifiers.rules.ZeroR();
      m_ZeroR.buildClassifier(data);
      return data;
    } else {
      m_ZeroR = null;
    }

    // Transform attributes
    m_NominalToBinary = new NominalToBinary();
    m_NominalToBinary.setInputFormat(data);
    data = Filter.useFilter(data, m_NominalToBinary);

    m_Filter = new Normalize();
    ((Normalize) m_Filter).setIgnoreClass(true);
    m_Filter.setInputFormat(data);
    data = Filter.useFilter(data, m_Filter);
    double z0 = data.instance(0).classValue(); // This stuff is not relevant in
                                               // classification case
    double z1 = data.instance(index).classValue();
    m_x1 = (y0 - y1) / (z0 - z1); // no division by zero, since y0 != y1
                                  // guaranteed => z0 != z1 ???
    m_x0 = (y0 - m_x1 * z0); // = y1 - m_x1 * z1

    m_classIndex = data.classIndex();
    m_numClasses = data.numClasses();
    m_numAttributes = data.numAttributes();

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
      OFFSET_ATTRIBUTE_WEIGHTS = (m_numUnits + 1) * m_numClasses;
      OFFSET_CENTERS = OFFSET_ATTRIBUTE_WEIGHTS + m_numAttributes;
    } else {
      OFFSET_ATTRIBUTE_WEIGHTS = -1;
      OFFSET_CENTERS = (m_numUnits + 1) * m_numClasses;
    }
    OFFSET_SCALES = OFFSET_CENTERS + m_numUnits * m_numAttributes;

    switch (m_scaleOptimizationOption) {
    case USE_GLOBAL_SCALE:
      m_RBFParameters = new double[OFFSET_SCALES + 1];
      break;
    case USE_SCALE_PER_UNIT_AND_ATTRIBUTE:
      m_RBFParameters = new double[OFFSET_SCALES + m_numUnits * m_numAttributes];
      break;
    default:
      m_RBFParameters = new double[OFFSET_SCALES + m_numUnits];
      break;
    }

    // Compute value required to set initiale scale parameter(s)
    double maxMinDist = -1;

    // Special case if there's only one basis function: simply
    // take distance to furthest data point
    if (centers.numInstances() == 1) {
      Instance center = centers.instance(0);
      for (int i = 0; i < dataRemoved.numInstances(); i++) {
        double dist = 0;
        for (int k = 0; k < centers.numAttributes(); k++) {
          double diff = dataRemoved.instance(i).value(k) -
            center.value(k);
          dist += diff * diff;
        }
        if (dist > maxMinDist) {
          maxMinDist = dist;
        }
      }
    } else {
      
      // Set initial radius based on distance to nearest other basis function
      for (int i = 0; i < centers.numInstances(); i++) {
        double minDist = Double.MAX_VALUE;
        for (int j = i + 1; j < centers.numInstances(); j++) {
          double dist = 0;
          for (int k = 0; k < centers.numAttributes(); k++) {
            if (k != centers.classIndex()) {
              double diff = centers.instance(i).value(k)
                - centers.instance(j).value(k);
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
    }

    // Initialize parameters
    if (m_scaleOptimizationOption == USE_GLOBAL_SCALE) {
      m_RBFParameters[OFFSET_SCALES] = Math.sqrt(maxMinDist);
    }
    for (int i = 0; i < m_numUnits; i++) {
      if (m_scaleOptimizationOption == USE_SCALE_PER_UNIT) {
        m_RBFParameters[OFFSET_SCALES + i] = Math.sqrt(maxMinDist);
      }
      int k = 0;
      for (int j = 0; j < m_numAttributes; j++) {
        if (k == centers.classIndex()) {
          k++;
        }
        if (j != data.classIndex()) {
          if (m_scaleOptimizationOption == USE_SCALE_PER_UNIT_AND_ATTRIBUTE) {
            m_RBFParameters[OFFSET_SCALES + (i * m_numAttributes + j)] = Math
              .sqrt(maxMinDist);
          }
          m_RBFParameters[OFFSET_CENTERS + (i * m_numAttributes) + j] = centers
            .instance(i).value(k);
          k++;
        }
      }
    }

    if (m_useAttributeWeights) {
      for (int j = 0; j < m_numAttributes; j++) {
        if (j != data.classIndex()) {
          m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j] = 1.0;
        }
      }
    }

    initializeOutputLayer(random);

    return data;
  }

  /**
   * Initialise output layer.
   */
  protected abstract void initializeOutputLayer(Random random);

  /**
   * Builds the RBF network regressor based on the given dataset.
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

    // Set up the initial arrays
    m_data = initializeClassifier(data);

    if (m_ZeroR != null) {
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
    double[][] b = new double[2][m_RBFParameters.length];
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < m_RBFParameters.length; j++) {
        b[i][j] = Double.NaN;
      }
    }

    m_RBFParameters = opt.findArgmin(m_RBFParameters, b);
    while (m_RBFParameters == null) {
      m_RBFParameters = opt.getVarbValues();
      if (m_Debug) {
        System.out.println("200 iterations finished, not enough!");
      }
      m_RBFParameters = opt.findArgmin(m_RBFParameters, b);
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
   * Calculates error for single instance.
   */
  protected abstract double calculateError(double[] outputs, Instance inst);

  /**
   * Postprocess error.
   */
  protected abstract double postprocessError(double error);

  /**
   * Postprocess gradient.
   */
  protected abstract void postprocessGradient(double[] grad);

  /**
   * Calculates the (penalized) squared error based on the current parameter
   * vector.
   */
  protected double calculateSE() {

    // Set up result set, and chunk size
    int chunksize = m_data.numInstances() / m_numThreads;
    Set<Future<Double>> results = new HashSet<Future<Double>>();

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
          final double[] outputs = new double[m_numUnits];
          double SE = 0;
          for (int k = lo; k < hi; k++) {
            final Instance inst = m_data.instance(k);

            // Calculate necessary input/output values and error term
            calculateOutputs(inst, outputs, null);
            SE += calculateError(outputs, inst);
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

    return postprocessError(0.5 * SE);
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
      final int hi = (j < m_numThreads - 1) ? (lo + chunksize) : m_data
        .numInstances();

      // Create and submit new job, where each instance in batch is processed
      Future<double[]> futureGrad = m_Pool.submit(new Callable<double[]>() {
        @Override
        public double[] call() {

          final double[] outputs = new double[m_numUnits];
          final double[] deltaHidden = new double[m_numUnits];
          final double[] derivativesOutput = new double[1];
          final double[] derivativesHidden = new double[m_numUnits];
          final double[] localGrad = new double[m_RBFParameters.length];
          for (int k = lo; k < hi; k++) {
            final Instance inst = m_data.instance(k);
            calculateOutputs(inst, outputs, derivativesHidden);
            updateGradient(localGrad, inst, outputs, derivativesOutput,
              deltaHidden);
            updateGradientForHiddenUnits(localGrad, inst, derivativesHidden,
              deltaHidden);
          }
          return localGrad;
        }
      });
      results.add(futureGrad);
    }

    // Calculate final gradient
    double[] grad = new double[m_RBFParameters.length];
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

    // Postprocess gradient
    postprocessGradient(grad);

    return grad;
  }

  /**
   * Update the gradient for the weights in the output layer.
   */
  protected abstract void updateGradient(double[] grad, Instance inst,
    double[] outputs, double[] sigmoidDerivativeOutput, double[] deltaHidden);

  /**
   * Update the gradient for the weights in the hidden layer.
   */
  protected void updateGradientForHiddenUnits(double[] grad, Instance inst,
    double[] derivativesHidden, double[] deltaHidden) {

    // Finalize deltaHidden
    for (int i = 0; i < m_numUnits; i++) {
      deltaHidden[i] *= derivativesHidden[i];
    }

    // Update gradient for hidden units
    for (int i = 0; i < m_numUnits; i++) {

      // Skip calculations if update too small
      if (deltaHidden[i] <= m_tolerance && deltaHidden[i] >= -m_tolerance) {
        continue;
      }

      // Update gradient for centers and possibly scale
      switch (m_scaleOptimizationOption) {
      case USE_GLOBAL_SCALE: { // Just one global scale parameter
        grad[OFFSET_SCALES] += derivativeOneScale(grad, deltaHidden,
          m_RBFParameters[OFFSET_SCALES], inst, i);
        break;
      }
      case USE_SCALE_PER_UNIT_AND_ATTRIBUTE: { // One scale parameter for each
                                               // unit and attribute
        derivativeScalePerAttribute(grad, deltaHidden, inst, i);
        break;
      }
      default: { // Only one scale parameter per unit
        grad[OFFSET_SCALES + i] += derivativeOneScale(grad, deltaHidden,
          m_RBFParameters[OFFSET_SCALES + i], inst, i);
        break;
      }
      }
    }
  }

  /**
   * Calculates partial derivatives in the case of different sigma per attribute
   * and unit.
   */
  protected void derivativeScalePerAttribute(double[] grad,
    double[] deltaHidden, Instance inst, int unitIndex) {

    double constant = deltaHidden[unitIndex];
    int offsetC = OFFSET_CENTERS + (unitIndex * m_numAttributes);
    int offsetS = OFFSET_SCALES + (unitIndex * m_numAttributes);
    double attWeight = 1.0;
    for (int j = 0; j < m_classIndex; j++) {
      double diff = (inst.value(j) - m_RBFParameters[offsetC + j]);
      double scalePart = (m_RBFParameters[offsetS + j] * m_RBFParameters[offsetS
        + j]);
      if (m_useAttributeWeights) {
        attWeight = m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j]
          * m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
        grad[OFFSET_ATTRIBUTE_WEIGHTS + j] -= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS
          + j]
          * constant * diff * diff / scalePart;
      }
      grad[offsetS + j] += constant * attWeight * diff * diff
        / (scalePart * m_RBFParameters[offsetS + j]);
      grad[offsetC + j] += constant * attWeight * diff / scalePart;
    }
    for (int j = m_classIndex + 1; j < m_numAttributes; j++) {
      double diff = (inst.value(j) - m_RBFParameters[offsetC + j]);
      double scalePart = (m_RBFParameters[offsetS + j] * m_RBFParameters[offsetS
        + j]);
      if (m_useAttributeWeights) {
        attWeight = m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j]
          * m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
        grad[OFFSET_ATTRIBUTE_WEIGHTS + j] -= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS
          + j]
          * constant * diff * diff / scalePart;
      }
      grad[offsetS + j] += constant * attWeight * diff * diff
        / (scalePart * m_RBFParameters[offsetS + j]);
      grad[offsetC + j] += constant * attWeight * diff / scalePart;
    }
  }

  /**
   * Calculates partial derivatives in the case of one sigma (either globally or
   * per unit).
   */
  protected double derivativeOneScale(double[] grad, double[] deltaHidden,
    double scale, Instance inst, int unitIndex) {

    double constant = deltaHidden[unitIndex] / (scale * scale);
    double sumDiffSquared = 0;
    int offsetC = OFFSET_CENTERS + (unitIndex * m_numAttributes);
    double attWeight = 1.0;
    for (int j = 0; j < m_classIndex; j++) {
      double diff = (inst.value(j) - m_RBFParameters[offsetC + j]);
      double diffSquared = diff * diff;
      if (m_useAttributeWeights) {
        attWeight = m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j]
          * m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
        grad[OFFSET_ATTRIBUTE_WEIGHTS + j] -= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS
          + j]
          * constant * diffSquared;
      }
      sumDiffSquared += attWeight * diffSquared;
      grad[offsetC + j] += constant * attWeight * diff;
    }
    for (int j = m_classIndex + 1; j < m_numAttributes; j++) {
      double diff = (inst.value(j) - m_RBFParameters[offsetC + j]);
      double diffSquared = diff * diff;
      if (m_useAttributeWeights) {
        attWeight = m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j]
          * m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
        grad[OFFSET_ATTRIBUTE_WEIGHTS + j] -= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS
          + j]
          * constant * diffSquared;
      }
      sumDiffSquared += attWeight * diffSquared;
      grad[offsetC + j] += constant * attWeight * diff;
    }
    return constant * sumDiffSquared / scale;
  }

  /**
   * Calculates the array of outputs of the hidden units, normalized if that
   * option has been chosen. Also calculates derivatives if d != null.
   */
  protected void calculateOutputs(Instance inst, double[] o, double[] d) {

    for (int i = 0; i < m_numUnits; i++) {

      double sumSquaredDiff = 0;
      switch (m_scaleOptimizationOption) {
      case USE_GLOBAL_SCALE: // Just one global scale parameter
        sumSquaredDiff = sumSquaredDiffOneScale(m_RBFParameters[OFFSET_SCALES],
          inst, i);
        break;
      case USE_SCALE_PER_UNIT_AND_ATTRIBUTE: { // One scale parameter for each
                                               // unit and attribute
        sumSquaredDiff = sumSquaredDiffScalePerAttribute(inst, i);
        break;
      }
      default: // Only one scale parameter per unit
        sumSquaredDiff = sumSquaredDiffOneScale(m_RBFParameters[OFFSET_SCALES
          + i], inst, i);
      }
      if (!m_useNormalizedBasisFunctions) {
        o[i] = Math.exp(-sumSquaredDiff);
        if (d != null) {
          d[i] = o[i];
        }
      } else {
        o[i] = -sumSquaredDiff;
      }
    }

    if (m_useNormalizedBasisFunctions) {
      double max = o[Utils.maxIndex(o)];
      double sum = 0.0;
      for (int i = 0; i < o.length; i++) {
        o[i] = Math.exp(o[i] - max);
        sum += o[i];
      }
      for (int i = 0; i < o.length; i++) {
        o[i] /= sum;
      }
      if (d != null) {
        for (int i = 0; i < o.length; i++) {
          d[i] = o[i] * (1 - o[i]);
        }
      }
    }
  }

  /**
   * The exponent of the RBF in the case of a different sigma per attribute and
   * unit.
   */
  protected double sumSquaredDiffScalePerAttribute(Instance inst, int unitIndex) {

    int offsetS = OFFSET_SCALES + unitIndex * m_numAttributes;
    int offsetC = OFFSET_CENTERS + unitIndex * m_numAttributes;
    double sumSquaredDiff = 0;
    for (int j = 0; j < m_classIndex; j++) {
      double diff = m_RBFParameters[offsetC + j] - inst.value(j);
      if (m_useAttributeWeights) {
        diff *= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
      }
      sumSquaredDiff += diff * diff
        / (2 * m_RBFParameters[offsetS + j] * m_RBFParameters[offsetS + j]);
    }
    for (int j = m_classIndex + 1; j < m_numAttributes; j++) {
      double diff = m_RBFParameters[offsetC + j] - inst.value(j);
      if (m_useAttributeWeights) {
        diff *= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
      }
      sumSquaredDiff += diff * diff
        / (2 * m_RBFParameters[offsetS + j] * m_RBFParameters[offsetS + j]);
    }
    return sumSquaredDiff;
  }

  /**
   * The exponent of the RBF in the case of a fixed sigma per unit.
   */
  protected double sumSquaredDiffOneScale(double scale, Instance inst,
    int unitIndex) {

    int offsetC = OFFSET_CENTERS + unitIndex * m_numAttributes;
    double sumSquaredDiff = 0;
    for (int j = 0; j < m_classIndex; j++) {
      double diff = m_RBFParameters[offsetC + j] - inst.value(j);
      if (m_useAttributeWeights) {
        diff *= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
      }
      sumSquaredDiff += diff * diff;
    }
    for (int j = m_classIndex + 1; j < m_numAttributes; j++) {
      double diff = m_RBFParameters[offsetC + j] - inst.value(j);
      if (m_useAttributeWeights) {
        diff *= m_RBFParameters[OFFSET_ATTRIBUTE_WEIGHTS + j];
      }
      sumSquaredDiff += diff * diff;
    }
    return sumSquaredDiff / (2 * scale * scale);
  }

  /**
   * Gets output "distribution" based on hidden layer outputs.
   */
  protected abstract double[] getDistribution(double[] outputs);

  /**
   * Calculates the output of the network after the instance has been piped
   * through the fliters to replace missing values, etc.
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {

    m_ReplaceMissingValues.input(inst);
    inst = m_ReplaceMissingValues.output();
    m_AttFilter.input(inst);
    inst = m_AttFilter.output();

    // default model?
    if (m_ZeroR != null) {
      return m_ZeroR.distributionForInstance(inst);
    }

    m_NominalToBinary.input(inst);
    inst = m_NominalToBinary.output();
    m_Filter.input(inst);
    inst = m_Filter.output();

    double[] outputs = new double[m_numUnits];
    calculateOutputs(inst, outputs, null);
    return getDistribution(outputs);
  }

  /**
   * This will return a string describing the classifier.
   * 
   * @return The string.
   */
  public String globalInfo() {

    return "Class implementing radial basis function networks,"
      + " trained in a fully supervised manner using WEKA's Optimization"
      + " class by minimizing squared error with the BFGS method. Note that"
      + " all attributes are normalized into the [0,1] scale.\n\n"
      + "The initial centers for the Gaussian radial basis functions"
      + " are found using WEKA's SimpleKMeans. The initial sigma values are"
      + " set to the maximum distance between any center and its nearest"
      + " neighbour in the set of centers.\n\nThere are several parameters. The"
      + " ridge parameter is used to penalize the size of the weights in the"
      + " output layer. The number of basis functions can also be specified. Note that large"
      + " numbers produce long training times. Another option determines"
      + " whether one global sigma value is used for all units (fastest),"
      + " whether one value is used per unit (common practice, it seems, and"
      + " set as the default), or a different value is learned for every"
      + " unit/attribute combination. It is also possible to learn attribute"
      + " weights for the distance function. (The square of the value shown"
      + " in the output is used.)  Finally, it is possible to use conjugate gradient"
      + " descent rather than BFGS updates, which can be faster for cases with many parameters, and"
      + " to use normalized basis functions instead of unnormalized ones.\n\n"
      + "To improve speed, an approximate version of the logistic function is used as the"
      + " activation function in the output layer. Also, if delta values in the backpropagation step are"
      + " within the user-specified tolerance, the gradient is not updated for that"
      + " particular instance, which saves some additional time.\n\nParalled calculation"
      + " of squared error and gradient is possible when multiple CPU cores are present."
      + " Data is split into batches and processed in separate threads in this case."
      + " Note that this only improves runtime for larger datasets.\n\n"
      + "Nominal attributes are processed using the unsupervised "
      + " NominalToBinary filter and missing values are replaced globally"
      + " using ReplaceMissingValues.\n\n"
      + "For more information see:\n\n" + getTechnicalInformation().toString();
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
  public void setUseNormalizedBasisFunctions(
    boolean newUseNormalizedBasisFunctions) {

    m_useNormalizedBasisFunctions = newUseNormalizedBasisFunctions;
  }

  /**
   * @return a string to describe the option
   */
  public String scaleOptimizationOptionTipText() {

    return "The number of sigma parameters to use.";
  }

  /**
   * Gets the scale optimisation method to use.
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

    Vector<Option> newVector = new Vector<Option>(9);

    newVector.addElement(new Option(
      "\tNumber of Gaussian basis functions (default is 2).\n", "N", 1,
      "-N <int>"));

    newVector
      .addElement(new Option(
        "\tRidge factor for quadratic penalty on output weights (default is 0.01).\n",
        "R", 1, "-R <double>"));
    newVector.addElement(new Option(
      "\tTolerance parameter for delta values (default is 1.0e-6).\n", "L", 1,
      "-L <double>"));
    newVector
      .addElement(new Option(
        "\tThe scale optimization option: global scale (1), one scale per unit (2), scale per unit and attribute (3) (default is 2).\n",
        "C", 1, "-C <1|2|3>"));

    newVector.addElement(new Option(
      "\tUse conjugate gradient descent (recommended for many attributes).\n",
      "G", 0, "-G"));

    newVector.addElement(new Option("\tUse normalized basis functions.\n", "O",
      0, "-O"));
    newVector
      .addElement(new Option("\tUse attribute weights.\n", "A", 0, "-A"));
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
   * -N
   *  Number of Gaussian basis functions (default is 2).
   * </pre>
   * 
   * <pre>
   * -R
   *  Ridge factor for quadratic penalty on output weights (default is 0.01).
   * </pre>
   * 
   * <pre>
   * -C
   *  The scale optimization option: global scale (1), one scale per unit (2), scale per unit and attribute (3) (default is 2).
   * </pre>
   * 
   * <pre>
   * -G
   *  Use conjugate gradient descent (recommended for many attributes).
   * </pre>
   * 
   * <pre>
   * -O
   *  Use normalized basis functions.
   * </pre>
   * 
   * <pre>
   * -A
   *  Use attribute weights.
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)
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
      setScaleOptimizationOption(new SelectedTag(Integer.parseInt(scale),
        TAGS_SCALE));
    } else {
      setScaleOptimizationOption(new SelectedTag(USE_SCALE_PER_UNIT, TAGS_SCALE));
    }
    String Tolerance = Utils.getOption('L', options);
    if (Tolerance.length() != 0) {
      setTolerance(Double.parseDouble(Tolerance));
    } else {
      setTolerance(1.0e-6);
    }
    m_useCGD = Utils.getFlag('G', options);
    m_useNormalizedBasisFunctions = Utils.getFlag('O', options);
    m_useAttributeWeights = Utils.getFlag('A', options);
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

    options.add("-N");
    options.add("" + getNumFunctions());

    options.add("-R");
    options.add("" + getRidge());

    options.add("-L");
    options.add("" + getTolerance());

    options.add("-C");
    options.add("" + getScaleOptimizationOption().getSelectedTag().getID());

    if (m_useCGD) {
      options.add("-G");
    }

    if (m_useNormalizedBasisFunctions) {
      options.add("-O");
    }

    if (m_useAttributeWeights) {
      options.add("-A");
    }

    options.add("-P");
    options.add("" + getPoolSize());

    options.add("-E");
    options.add("" + getNumThreads());

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }
}
