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
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Statistics;
import weka.core.Utils;
import weka.clusterers.EM;

/**
 * Simple weighted mixture density estimator. Uses a mixture of Gaussians
 * and applies the leave-one-out bootstrap for model selection.
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

  /** The number of components to use (default is 2)*/
  protected int m_NumComponents = 2; 

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
  public void setNumComponents(int m_NumComponents) {
    this.m_NumComponents = m_NumComponents;
  }

  /**
   * Constructs the intial estimator
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
   * Updates the model based on the current data.
   * Uses the leave-one-out bootstrap to choose the number of components.
   */
  protected void updateModel() {

    if (m_MixtureModel != null) {
      return;
    } else if (m_Instances.numInstances() > 0) {
      try {
        m_MixtureModel = new EM();
        m_MixtureModel.setNumClusters(m_NumComponents);
        m_MixtureModel.buildClusterer(m_Instances);
      } catch (Exception e) {
        e.printStackTrace();
        m_MixtureModel = null;
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
    options.addElement(new Option("\tNumber of components to use.", "N", 0, "-N"));
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
      setNumComponents(2);
    }
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

    return options.toArray(new String[0]);
  }

  /**
   * Main method, used for testing this class.
   */
  public static void main(String[] args) {

    // Get random number generator initialized by system
    Random r = new Random();

    // Create density estimator
    UnivariateMixtureEstimator e = new UnivariateMixtureEstimator();

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