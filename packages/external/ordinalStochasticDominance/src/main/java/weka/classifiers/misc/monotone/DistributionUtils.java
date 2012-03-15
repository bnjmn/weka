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
 *    DistributionUtils.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc.monotone;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.estimators.DiscreteEstimator;

import java.util.Arrays;

/** 
 * Class with some simple methods acting on 
 * <code> CumulativeDiscreteDistribution. </code>
 * All of the methods in this class are very easily implemented
 * and the main use of this class is to gather all these methods
 * in a single place.  It could be argued that some of the methods
 * should be implemented in the class 
 * <code> CumulativeDiscreteDistribution </code> itself.
 * <p>
 * This implementation is part of the master's thesis: "Studie
 * en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd
 * rangschikken", Stijn Lievens, Ghent University, 2004. 
 * </p>
 * 
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision$
 */
public class DistributionUtils
  implements RevisionHandler {

  /**
   * Constant indicating the maximal number of classes
   * for which there is a minimal and maximal distribution
   * present in the pool.
   * One of the purposes of this class is to serve as a factory
   * for minimal and maximal cumulative probability distributions.
   * Since instances of <code> CumulativeDiscreteDistribution </code>
   * are immutable, we can create them beforehand and reuse them 
   * every time one is needed.  
   */
  private static final int MAX_CLASSES = 20;

  /**
   * Array filled with minimal cumulative discrete probability
   * distributions.  This means that probability one is given to the
   * first element.  This array serves as a pool for the method
   * <code> getMinimalCumulativeDiscreteDistribution. </code>
   */
  private static final CumulativeDiscreteDistribution[] m_minimalDistributions;

  /**
   * Array filled with maximal cumulative discrete probability
   * distributions. This means that probability one is given to the
   * largest element.  This array serves as a pool for the method
   * <code> getMaximalCumulativeDiscreteDistribution. </code>
   */
  private static final CumulativeDiscreteDistribution[] m_maximalDistributions;

  // fill both static arrays with the correct distributions
  static {
    m_minimalDistributions = new CumulativeDiscreteDistribution[MAX_CLASSES + 1];
    m_maximalDistributions = new CumulativeDiscreteDistribution[MAX_CLASSES + 1];

    for (int i = 1; i <= MAX_CLASSES; i++) {
      double[] dd = new double[i];
      dd[dd.length - 1] = 1;
      m_maximalDistributions[i] = new CumulativeDiscreteDistribution(dd);
      Arrays.fill(dd,1);
      m_minimalDistributions[i] = new CumulativeDiscreteDistribution(dd);
    }
  }

  /** 
   *  Compute a linear interpolation between the two given 
   *  <code> CumulativeDiscreteDistribution. </code>
   *  
   *  @param cdf1 the first <code> CumulativeDiscreteDistribution </code>
   *  @param cdf2 the second <code> CumulativeDiscreteDistribution </code>
   *  @param s the interpolation parameter
   *  @return (1 - s) &times; cdf1 + s &times; cdf2
   *  @throws IllegalArgumentException if the two distributions
   *  don't have the same size or if the parameter <code> s </code>
   *  is not in the range [0,1]
   */
  public static CumulativeDiscreteDistribution interpolate(
      CumulativeDiscreteDistribution cdf1,
      CumulativeDiscreteDistribution cdf2, double s) throws IllegalArgumentException {

    if (cdf1.getNumSymbols() != cdf2.getNumSymbols()) { 
      throw new IllegalArgumentException
      ("CumulativeDiscreteDistributions don't have " 
	  + "the same size");
    }

    if (s < 0 || s > 1) {
      throw new IllegalArgumentException
      ("Parameter s exceeds bounds");
    }

    double[] res = new double[cdf1.getNumSymbols()];
    for (int i = 0, n = cdf1.getNumSymbols(); i < n; i++) {
      res[i] = (1 - s) * cdf1.getCumulativeProbability(i) +
      s * cdf2.getCumulativeProbability(i);
    }
    return new CumulativeDiscreteDistribution(res);
  }

  /** 
   *  Compute a linear interpolation between the two given 
   *  <code> CumulativeDiscreteDistribution. </code>
   *  
   *  @param cdf1 the first <code> CumulativeDiscreteDistribution </code>
   *  @param cdf2 the second <code> CumulativeDiscreteDistribution </code>
   *  @param s the interpolation parameters, only the relevant number
   *  of entries is used, so the array may be longer than the common
   *  length of <code> cdf1 </code> and <code> cdf2 </code>
   *  @return (1 - s) &times; cdf1 + s &times; cdf2, or more specifically
   *  a distribution cd such that <code> 
   *  cd.getCumulativeProbability(i) = 
   *  (1-s[i]) &times; cdf1.getCumulativeProbability(i) + 
   *  s[i] &times; cdf2.getCumulativeProbability(i) </code> 
   *  @throws IllegalArgumentException if the two distributions
   *  don't have the same size or if the array <code> s </code>
   *  contains parameters not in the range <code> [0,1] </code>
   */
  public static CumulativeDiscreteDistribution interpolate(
      CumulativeDiscreteDistribution cdf1,
      CumulativeDiscreteDistribution cdf2, double[] s) throws IllegalArgumentException {

    if (cdf1.getNumSymbols() != cdf2.getNumSymbols()) { 
      throw new IllegalArgumentException
      ("CumulativeDiscreteDistributions don't have " 
	  + "the same size");
    }

    if (cdf1.getNumSymbols() > s.length) {
      throw new IllegalArgumentException
      ("Array with interpolation parameters is not "
	  + " long enough");
    }

    double[] res = new double[cdf1.getNumSymbols()];
    for (int i = 0, n = cdf1.getNumSymbols(); i < n; i++) {
      if (s[i] < 0 || s[i] > 1) {
	throw new IllegalArgumentException
	("Interpolation parameter exceeds bounds");
      }
      res[i] = (1 - s[i]) * cdf1.getCumulativeProbability(i) +
      s[i] * cdf2.getCumulativeProbability(i);
    }
    return new CumulativeDiscreteDistribution(res);
  }

  /** 
   *  Compute a linear interpolation between the two given 
   *  <code> DiscreteDistribution. </code>
   *  
   *  @param ddf1 the first <code> DiscreteDistribution </code>
   *  @param ddf2 the second <code> DiscreteDistribution </code>
   *  @param s the interpolation parameter
   *  @return <code> (1 - s) &times; ddf1 + s &times; ddf2 </code>
   *  @throws IllegalArgumentException if the two distributions
   *  don't have the same size or if the parameter <code> s </code>
   *  is not in the range [0,1]
   */
  public static DiscreteDistribution interpolate(
      DiscreteDistribution ddf1,
      DiscreteDistribution ddf2, double s) throws IllegalArgumentException {

    if (ddf1.getNumSymbols() != ddf2.getNumSymbols()) { 
      throw new IllegalArgumentException
      ("DiscreteDistributions don't have " 
	  + "the same size");
    }

    if (s < 0 || s > 1) {
      throw new IllegalArgumentException
      ("Parameter s exceeds bounds");
    }

    double[] res = new double[ddf1.getNumSymbols()];
    for (int i = 0, n = ddf1.getNumSymbols(); i < n; i++) {
      res[i] = (1 - s) * ddf1.getProbability(i) +
      s * ddf2.getProbability(i);
    }
    return new DiscreteDistribution(res);
  }

  /**
   * Create a new <code> CumulativeDiscreteDistribution </code>
   * that is the minimum of the two given <code>
   * CumulativeDiscreteDistribution. </code>
   * Each component of the resulting probability distribution 
   * is the minimum of the two corresponding components. <br/>
   * Note: despite of its name, the returned cumulative probability
   * distribution dominates both the arguments of this method.
   *
   * @param cdf1 first <code> CumulativeDiscreteDistribution </code>
   * @param cdf2 second <code> CumulativeDiscreteDistribution </code>
   * @return the minimum of the two distributions
   * @throws IllegalArgumentException if the two distributions
   * dont't have the same length
   */
  public static CumulativeDiscreteDistribution takeMin(
      CumulativeDiscreteDistribution cdf1,
      CumulativeDiscreteDistribution cdf2) throws IllegalArgumentException {
    
    if (cdf1.getNumSymbols() != cdf2.getNumSymbols() )
      throw new IllegalArgumentException
      ("Cumulative distributions don't have the same length");

    double[] cdf = new double[cdf1.getNumSymbols()];
    int n = cdf.length;
    for (int i = 0; i < n; i++) { 
      cdf[i] = Math.min(cdf1.getCumulativeProbability(i),
	  cdf2.getCumulativeProbability(i));
    }
    return new CumulativeDiscreteDistribution(cdf);
  }

  /**
   * Create a new <code> CumulativeDiscreteDistribution </code>
   * that is the maximum of the two given <code>
   * CumulativeDiscreteDistribution. </code>
   * Each component of the resulting probability distribution 
   * is the maximum of the two corresponding components.
   * Note: despite of its name, the returned cumulative probability
   * distribution is dominated by both the arguments of this method.
   *
   * @param cdf1 first <code> CumulativeDiscreteDistribution </code>
   * @param cdf2 second <code> CumulativeDiscreteDistribution </code>
   * @return the maximum of the two distributions
   * @throws IllegalArgumentException if the two distributions
   * dont't have the same length
   */
  public static CumulativeDiscreteDistribution takeMax(
      CumulativeDiscreteDistribution cdf1,
      CumulativeDiscreteDistribution cdf2) throws IllegalArgumentException {
    
    if (cdf1.getNumSymbols() != cdf2.getNumSymbols() )
      throw new IllegalArgumentException
      ("Cumulative distributions don't have the same length");

    double[] cdf = new double[cdf1.getNumSymbols()];
    int n = cdf.length;
    for (int i = 0; i < n; i++) { 
      cdf[i] = Math.max(cdf1.getCumulativeProbability(i),
	  cdf2.getCumulativeProbability(i));
    }
    return new CumulativeDiscreteDistribution(cdf);
  }

  /**
   * Converts a <code> DiscreteEstimator </code> to an array of 
   * doubles.
   *
   * @param df the <code> DiscreteEstimator </code> to be converted
   * @return an array of doubles representing the 
   * <code> DiscreteEstimator </code>
   */
  public static double[] getDistributionArray(DiscreteEstimator df) {
    double[] dfa = new double[df.getNumSymbols()];
    for (int i = 0; i < dfa.length; i++) {
      dfa[i] = df.getProbability(i);
    }
    return dfa;
  }

  /**
   * Get the minimal <code> CumulativeDiscreteDistribution </code>
   * over <code> numClasses </code> elements.  This means that
   * a probability of one is assigned to the first element.
   *
   * @param numClasses the number of elements 
   * @return the minimal <code> CumulativeDiscreteDistribution </code>
   * over the requested number of elements
   * @throws IllegalArgumentException if <code> numClasses </code> 
   * is smaller or equal than 0
   */
  public static CumulativeDiscreteDistribution getMinimalCumulativeDiscreteDistribution(
      int numClasses) throws IllegalArgumentException {
    
    if (numClasses <= 0) {
      throw new IllegalArgumentException
      ("Number of elements must be positive");
    }
    if (numClasses <= MAX_CLASSES) {
      return m_minimalDistributions[numClasses];
    }

    double[] dd = new double[numClasses];
    Arrays.fill(dd,1);
    return new CumulativeDiscreteDistribution(dd);
  }

  /**
   * Get the maximal <code> CumulativeDiscreteDistribution </code>
   * over <code> numClasses </code> elements.  This means that
   * a probability of one is assigned to the last class.
   *
   * @param numClasses the number of elements 
   * @return the maximal <code> CumulativeDiscreteDistribution </code>
   * over the requested number of elements
   * @throws IllegalArgumentException if <code> numClasses </code> 
   * is smaller or equal than 0
   */
  public static CumulativeDiscreteDistribution getMaximalCumulativeDiscreteDistribution(
      int numClasses) throws IllegalArgumentException {
    
    if (numClasses <= 0) {
      throw new IllegalArgumentException
      ("Number of elements must be positive");
    }
    if (numClasses <= MAX_CLASSES) {
      return m_maximalDistributions[numClasses];
    }

    double[] dd = new double[numClasses];
    dd[dd.length - 1] = 1;
    return new CumulativeDiscreteDistribution(dd);
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
