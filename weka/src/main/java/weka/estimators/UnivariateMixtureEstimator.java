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
 *    UnivariateNormalEstimator.java
 *    Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.estimators;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;
import java.util.ArrayList;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.ContingencyTables;
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Statistics;
import weka.core.Utils;
import weka.clusterers.EM;

/**
 * Simple weighted mixture density estimator. Uses a mixture of Gaussians
 * and applies the leave-one-out Bootstrap for model selection.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public class UnivariateMixtureEstimator implements UnivariateDensityEstimator,
UnivariateIntervalEstimator,
UnivariateQuantileEstimator,
OptionHandler,
Serializable {

  /** For serialization */
  private static final long serialVersionUID = -2035274930137353656L;

  /** The instances to be used for the estimator */
  protected Instances m_Instances;

  /** The current mixture model */
  protected EM m_MixtureModel;

  /** The number of components to use (default is -1)*/
  protected int m_NumComponents = -1; 

  /** The maximum number of components to use (default is 5) */
  protected int m_MaxNumComponents = 5;

  /** The random number seed to use (default is 1*/
  protected int m_Seed = 1;

  /** The number of Bootstrap runs to use to select the number of components (default is 10) */
  protected int m_NumBootstrapRuns = 10;

  /** The current bandwidth (only computed when needed) */
  protected double m_Width = Double.MAX_VALUE;

  /** The exponent to use in computation of bandwidth (default: -0.25) */
  protected double m_Exponent = -0.25;

  /** The minimum allowed value of the kernel width (default: 1.0E-6) */
  protected double m_MinWidth = 1.0E-6;

  /** The number of intervals used to approximate prediction interval. */
  protected int m_NumIntervals = 1000;

  /** The smallest value in the data */
  protected double m_Min = Double.MAX_VALUE;

  /** The largest value in the data */
  protected double m_Max = -Double.MAX_VALUE;

  /** The weighted sum of values */
  protected double m_WeightedSum = 0;

  /** The weighted sum of squared values */
  protected double m_WeightedSumSquared = 0;

  /** The total sum of weights. */
  protected double m_SumOfWeights = 0;

  /** Whether to use normalized entropy instance of bootstrap. */
  protected boolean m_UseNormalizedEntropy = false;
  
  /** Whether to output debug info. */
  protected boolean m_Debug = true;

  /**
   * Returns a string describing the estimator.
   */
  public String globalInfo() {
    return "Estimates a univariate mixture model.";
  }

  /**
   * @return whether normalized entropy is used
   */
  public boolean getUseNormalizedEntropy() {
    return m_UseNormalizedEntropy;
  }

  /**
   * @param useNormalizedEntropy whether to use normalized entropy
   */
  public void setUseNormalizedEntropy(boolean useNormalizedEntropy) {
    m_UseNormalizedEntropy = useNormalizedEntropy;
  }

  /**
   * The tool tip for this property.
   */
  public String numBootstrapRunsToolTipText() {
    return "The number of Bootstrap runs to choose the number of components.";
  }

  /**
   * Returns the number of Bootstrap runs.
   * 
   * @return the number of Bootstrap runs
   */
  public int getNumBootstrapRuns() {
    return m_NumBootstrapRuns;
  }

  /**
   * Sets the number of Bootstrap runs.
   * 
   * @param mnumBootstrapRuns the number of Bootstrap runs
   */
  public void setNumBootstrapRuns(int numBootstrapRuns) {
    m_NumBootstrapRuns = numBootstrapRuns;
  }

  /**
   * The tool tip for this property.
   */
  public String numComponentsToolTipText() {
    return "The number of mixture components to use.";
  }

  /**
   * Returns the number of components to use.
   * 
   * @return the m_NumComponents
   */
  public int getNumComponents() {
    return m_NumComponents;
  }

  /**
   * Sets the number of components to use.
   * 
   * @param m_NumComponents the m_NumComponents to set
   */
  public void setNumComponents(int numComponents) {
    this.m_NumComponents = numComponents;
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
   * The tool tip for this property.
   */
  public String maxNumComponentsToolTipText() {
    return "The maximum number of mixture components to use.";
  }

  /**
   * Returns the number of components to use.
   * 
   * @return the maximum number of components to use
   */
  public int getMaxNumComponents() {
    return m_MaxNumComponents;
  }

  /**
   * Sets the number of components to use.
   * 
   * @param maxNumComponents the maximum number of components to evaluate
   */
  public void setMaxNumComponents(int maxNumComponents) {
    m_MaxNumComponents = maxNumComponents;
  }

  /**
   * Constructs the initial estimator
   */
  public UnivariateMixtureEstimator() {

    ArrayList<Attribute> att = new ArrayList<Attribute>(1);
    att.add(new Attribute("x"));
    m_Instances = new Instances("Mixture estimator data", att, 100);
  }

  /**
   * Adds a value to the density estimator.
   *
   * @param value the value to add
   * @param weight the weight of the value
   */
  public void addValue(double value, double weight) {

    m_MixtureModel = null;
    m_Instances.add(new DenseInstance(weight, new double[] {value}));

    // Update statistics
    m_WeightedSum += value * weight;
    m_WeightedSumSquared += value * value * weight;
    m_SumOfWeights += weight;
    if (value < m_Min) {
      m_Min = value;
    }
    if (value > m_Max) {
      m_Max = value;
    }
  }

  /**
   * Selects the number of components using leave-one-out Bootstrap, estimating loglikelihood.
   *
   * @return the number of components to use
   */
  protected int findNumComponentsUsingBootStrap() {

    if (m_NumComponents > 0) {
      return m_NumComponents;
    }
    if (m_MaxNumComponents <= 1) {
      return 1;
    }

    Random random = new Random(m_Seed);

    double bestLogLikelihood = -Double.MAX_VALUE;
    int bestNumComponents = 1;  
    for (int i = 1; i <= m_MaxNumComponents; i++) {
      double logLikelihood = 0;
      for (int k = 0; k <= m_NumBootstrapRuns; k++) {
        double locLogLikelihood = 0;
        boolean[] inBag = new boolean[m_Instances.numInstances()];
        EM mixtureModel = buildModel(m_Seed, i, m_Instances.resampleWithWeights(random, inBag, true));
        try {
          double totalWeight = 0;
          for (int j = 0; j < m_Instances.numInstances(); j++) {
            if (!inBag[j]) {
              double weight = m_Instances.instance(j).weight();
              locLogLikelihood += weight * mixtureModel.logDensityForInstance(m_Instances.instance(j));
              totalWeight += weight;
            }
          }
          locLogLikelihood /= totalWeight;
        } catch (Exception ex) {
          ex.printStackTrace();
          locLogLikelihood = -Double.MAX_VALUE;
        }
        logLikelihood += locLogLikelihood;
      }
      logLikelihood /= (double)m_NumBootstrapRuns;
      if (m_Debug) {
        System.err.println("Loglikelihood: " + logLikelihood + "\tNumber of components: " + i);
      }
      if (logLikelihood > bestLogLikelihood) {
        bestNumComponents = i;
        bestLogLikelihood = logLikelihood;
      }
    }

    return bestNumComponents;
  }

  /**
   * Calculates loglikelihood for given model and data.
   */
  protected double loglikelihood(EM model, Instances data) {

    double logLikelihood = 0;
    try {
      for (int j = 0; j < data.numInstances(); j++) {
        logLikelihood += data.instance(j).weight() * model.logDensityForInstance(data.instance(j));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      logLikelihood = -Double.MAX_VALUE;
    }
    return logLikelihood;
  }

  /**
   * Calculates entrpy for given model and data.
   */
  protected double entropy(EM model, Instances data) {

    double entropy = 0;
    try {
      for (int j = 0; j < data.numInstances(); j++) {
        entropy += data.instance(j).weight() * 
          ContingencyTables.entropy(model.distributionForInstance(data.instance(j)));
      }
      entropy *= Utils.log2; // Need natural logarithm, not base-2 logarithm
    } catch (Exception ex) {
      ex.printStackTrace();
      entropy = Double.MAX_VALUE;
    }
    return entropy;
  }


  /**
   * Selects the number of components using normalized entropy.
   *
   * @return the model to use
   */
  protected EM findModelUsingNormalizedEntroy() {

    if (m_NumComponents > 0) {
      return buildModel(m_Seed, m_NumComponents, m_Instances);
    }
    if (m_MaxNumComponents <= 1) {
      return buildModel(m_Seed, 1, m_Instances);
    }

    //Loglikelihood for one cluster
    EM bestMixtureModel = buildModel(m_Seed, 1, m_Instances);
    double loglikelihoodForOneCluster = loglikelihood(bestMixtureModel, m_Instances);
    double bestNormalizedEntropy = 1;
    for (int i = 2; i <= m_MaxNumComponents; i++) {
      EM mixtureModel = buildModel(m_Seed, i, m_Instances);
      
      double loglikelihood = loglikelihood(mixtureModel, m_Instances);
      if (loglikelihood < loglikelihoodForOneCluster) {
        // This appears to happen in practice, hopefully not because of a bug...
        continue;
      }     
      double entropy = entropy(mixtureModel, m_Instances);
      double normalizedEntropy = entropy / (loglikelihood - loglikelihoodForOneCluster);

      if (m_Debug) {
        System.err.println("Entropy: " + entropy + "\tLogLikelihood: " + loglikelihood +
            "\tLoglikelihood for one cluster: " + loglikelihoodForOneCluster + "\tNormalized entropy: " + normalizedEntropy + "\tNumber of components: " + i);
      }
      if (normalizedEntropy < bestNormalizedEntropy) {
        bestMixtureModel = mixtureModel;
        bestNormalizedEntropy = normalizedEntropy;
      }
    }

    return bestMixtureModel;
  }

  /**
   * Builds model from given dataset
   */
  protected EM buildModel(int seed, int numComponents, Instances data) {

    try {
      EM mixtureModel = new EM();
      mixtureModel.setSeed(seed);
      mixtureModel.setNumClusters(numComponents);
      mixtureModel.buildClusterer(data);
      return mixtureModel;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } 
  }

  /**
   * Updates the model based on the current data.
   * Uses the leave-one-out Bootstrap to choose the number of components.
   */
  protected void updateModel() {

    if (m_MixtureModel != null) {
      return;
    } else if (m_Instances.numInstances() > 0) {

      if (m_UseNormalizedEntropy) {
        m_MixtureModel = findModelUsingNormalizedEntroy();
      } else {
        m_MixtureModel = buildModel(m_Seed, findNumComponentsUsingBootStrap(), m_Instances);
          
      }

      // Update widths for cases that are out of bounds,
      // using same code as in kernel estimator

      // First, compute variance for scaling
      double mean = m_WeightedSum / m_SumOfWeights;
      double variance = m_WeightedSumSquared / m_SumOfWeights - mean * mean;
      if (variance < 0) {
        variance = 0;
      }

      // Compute kernel bandwidth
      m_Width = Math.sqrt(variance) * Math.pow(m_SumOfWeights, m_Exponent);

      if (m_Width <= m_MinWidth) {
        m_Width = m_MinWidth;
      }
    }
  }

  /**
   * Returns the interval for the given confidence value. 
   * 
   * @param conf the confidence value in the interval [0, 1]
   * @return the interval
   */
  public double[][] predictIntervals(double conf) {

    updateModel();

    // Compute minimum and maximum value, and delta
    double val = Statistics.normalInverse(1.0 - (1.0 - conf) / 2);
    double min = m_Min - val * m_Width;
    double max = m_Max + val * m_Width;
    double delta = (max - min) / m_NumIntervals;

    // Create array with estimated probabilities
    double[] probabilities = new double[m_NumIntervals];
    double leftVal = Math.exp(logDensity(min));
    for (int i = 0; i < m_NumIntervals; i++) {
      double rightVal = Math.exp(logDensity(min + (i + 1) * delta));
      probabilities[i] = 0.5 * (leftVal + rightVal) * delta;
      leftVal = rightVal;
    }

    // Sort array based on area of bin estimates
    int[] sortedIndices = Utils.sort(probabilities);

    // Mark the intervals to use
    double sum = 0;
    boolean[] toUse = new boolean[probabilities.length];
    int k = 0;
    while ((sum < conf) && (k < toUse.length)) {
      toUse[sortedIndices[toUse.length - (k + 1)]] = true;
      sum += probabilities[sortedIndices[toUse.length - (k + 1)]];
      k++;
    }

    // Don't need probabilities anymore
    probabilities = null;

    // Create final list of intervals
    ArrayList<double[]> intervals = new ArrayList<double[]>();

    // The current interval
    double[] interval = null;

    // Iterate through kernels
    boolean haveStartedInterval = false;
    for (int i = 0; i < m_NumIntervals; i++) {

      // Should the current bin be used?
      if (toUse[i]) {

        // Do we need to create a new interval?
        if (haveStartedInterval == false) {
          haveStartedInterval = true;
          interval = new double[2];
          interval[0] = min + i * delta;
        }

        // Regardless, we should update the upper boundary
        interval[1] = min + (i + 1) * delta;
      } else {

        // We need to finalize and store the last interval
        // if necessary.
        if (haveStartedInterval) {
          haveStartedInterval = false;
          intervals.add(interval);
        }
      }
    }

    // Add last interval if there is one
    if (haveStartedInterval) {
      intervals.add(interval);
    }

    return intervals.toArray(new double[0][0]);
  }

  /**
   * Returns the quantile for the given percentage.
   * 
   * @param percentage the percentage
   * @return the quantile
   */
  public double predictQuantile(double percentage) {

    updateModel();

    // Compute minimum and maximum value, and delta
    double val = Statistics.normalInverse(1.0 - (1.0 - 0.95) / 2);
    double min = m_Min - val * m_Width;
    double max = m_Max + val * m_Width;
    double delta = (max - min) / m_NumIntervals;

    double sum = 0;
    double leftVal = Math.exp(logDensity(min));
    for (int i = 0; i < m_NumIntervals; i++) {
      if (sum >= percentage) {
        return min + i * delta;
      }
      double rightVal = Math.exp(logDensity(min + (i + 1) * delta));
      sum += 0.5 * (leftVal + rightVal) * delta;
      leftVal = rightVal;
    }
    return max;
  }

  /**
   * Returns the natural logarithm of the density estimate at the given
   * point.
   *
   * @param value the value at which to evaluate
   * @return the natural logarithm of the density estimate at the given
   * value
   */
  public double logDensity(double value) {

    updateModel();
    if (m_MixtureModel == null) {
      return Math.log(Double.MIN_VALUE);
    }
    try {
      return m_MixtureModel.logDensityForInstance(new DenseInstance(1.0, new double[] {value}));
    } catch (Exception e) {
      e.printStackTrace();
      return Double.NaN; // We should get NaN in downstream calculations
    }
  }

  /**
   * Returns textual description of this estimator.
   */
  public String toString() {

    updateModel();
    if (m_MixtureModel == null) {
      return "";
    }
    return m_MixtureModel.toString();
  }

  /**
   * Returns an enumeration that lists the command-line options that are available
   * 
   * @return the list of options as an enumeration
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> options = new Vector<Option>();
    options.addElement(new Option("\tNumber of components to use (default: -1).", "N", 1, "-N"));
    options.addElement(new Option("\tMaximum number of components to use (default: 5).", "M", 1, "-M"));
    options.addElement(new Option("\tSeed for the random number generator (default: 1).", "S", 1, "-S"));
    options.addElement(new Option("\tThe number of bootstrap runs to use (default: 10).", "B", 1, "-B"));
    options.addElement(new Option("\tUse normalized entropy instead of bootstrap.", "E", 1, "-E"));
    return options.elements();
  }

  /**
   * Sets options based on the given array of strings.
   * 
   * @param options the list of options to parse
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String optionString = Utils.getOption("N", options);
    if (optionString.length() > 0) {
      setNumComponents(Integer.parseInt(optionString));    
    } else {
      setNumComponents(-1);
    }
    optionString = Utils.getOption("M", options);
    if (optionString.length() > 0) {
      setMaxNumComponents(Integer.parseInt(optionString));    
    } else {
      setMaxNumComponents(5);
    }
    optionString = Utils.getOption("S", options);
    if (optionString.length() > 0) {
      setSeed(Integer.parseInt(optionString));    
    } else {
      setSeed(1);
    }
    optionString = Utils.getOption("B", options);
    if (optionString.length() > 0) {
      setNumBootstrapRuns(Integer.parseInt(optionString));    
    } else {
      setNumBootstrapRuns(10);
    }
    m_UseNormalizedEntropy = Utils.getFlag("E", options);
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Returns the current set of options.
   * 
   * @return the current set of options as a string
   */
  @Override
  public String[] getOptions() {
    Vector<String> options = new Vector<String>();

    options.add("-N");
    options.add("" + getNumComponents());

    options.add("-M");
    options.add("" + getMaxNumComponents());

    options.add("-S");
    options.add("" + getSeed());

    options.add("-B");
    options.add("" + getNumBootstrapRuns());

    if (m_UseNormalizedEntropy) {
      options.add("-E");
    }
    
    return options.toArray(new String[0]);
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10971 $");
  }

  /**
   * Main method, used for testing this class.
   */
  public static void main(String[] args) {

    // Whether to use normalized entropy instead of bootstrap
    boolean useNormalizedEntropy = true;
    
    // Get random number generator initialized by system
    Random r = new Random();

    // Create density estimator
    UnivariateMixtureEstimator e = new UnivariateMixtureEstimator();
    e.setUseNormalizedEntropy(useNormalizedEntropy);

    // Output the density estimator
    System.out.println(e);

    // Monte Carlo integration
    double sum = 0;
    for (int i = 0; i < 100000; i++) {
      sum += Math.exp(e.logDensity(r.nextDouble() * 10.0 - 5.0));
    }
    System.out.println("Approximate integral: " + 10.0 * sum / 100000);

    // Add Gaussian values into it
    for (int i = 0; i < 100000; i++) {
      e.addValue(r.nextGaussian() * 0.5 - 1, 1);
      e.addValue(r.nextGaussian() * 0.5 + 1, 3);
    }

    // Output the density estimator
    System.out.println(e);

    // Monte Carlo integration
    sum = 0;
    for (int i = 0; i < 100000; i++) {
      sum += Math.exp(e.logDensity(r.nextDouble() * 10.0 - 5.0));
    }
    System.out.println("Approximate integral: " + 10.0 * sum / 100000);

    // Create density estimator
    e = new UnivariateMixtureEstimator();
    e.setUseNormalizedEntropy(useNormalizedEntropy);

    // Add Gaussian values into it
    for (int i = 0; i < 100000; i++) {
      e.addValue(r.nextGaussian() * 0.5 - 1, 1);
      e.addValue(r.nextGaussian() * 0.5 + 1, 1);
      e.addValue(r.nextGaussian() * 0.5 + 1, 1);
      e.addValue(r.nextGaussian() * 0.5 + 1, 1);
    }

    // Output the density estimator
    System.out.println(e);

    // Monte Carlo integration
    sum = 0;
    for (int i = 0; i < 100000; i++) {
      sum += Math.exp(e.logDensity(r.nextDouble() * 10.0 - 5.0));
    }
    System.out.println("Approximate integral: " + 10.0 * sum / 100000);

    // Create density estimator
    e = new UnivariateMixtureEstimator();
    e.setUseNormalizedEntropy(useNormalizedEntropy);

    // Add Gaussian values into it
    for (int i = 0; i < 100000; i++) {
      e.addValue(r.nextGaussian() * 5.0 + 3.0 , 1);
    }

    // Output the density estimator
    System.out.println(e);

    // Check interval estimates
    double[][] intervals = e.predictIntervals(0.95);
    System.out.println("Lower: " + intervals[0][0] + " Upper: " + intervals[0][1]);
    double covered = 0;
    for (int i = 0; i < 100000; i++) {
      double val = r.nextGaussian() * 5.0 + 3.0;
      if (val >= intervals[0][0] && val <= intervals[0][1]) {
        covered++;
      }
    }
    System.out.println("Coverage: " + covered / 100000);

    intervals = e.predictIntervals(0.8);
    System.out.println("Lower: " + intervals[0][0] + " Upper: " + intervals[0][1]);
    covered = 0;
    for (int i = 0; i < 100000; i++) {
      double val = r.nextGaussian() * 5.0 + 3.0;
      if (val >= intervals[0][0] && val <= intervals[0][1]) {
        covered++;
      }
    }
    System.out.println("Coverage: " + covered / 100000);
  }
}