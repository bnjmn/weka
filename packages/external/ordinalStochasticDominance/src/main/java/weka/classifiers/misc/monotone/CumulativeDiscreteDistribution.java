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
 *    CumulativeDiscreteDistribution.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc.monotone;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.estimators.DiscreteEstimator;

import java.io.Serializable;

/**
 * Represents a discrete cumulative probability distribution 
 * over a totally ordered discrete set.  The elements of this set
 * are numbered consecutively starting from 0. 
 *<p>
 * In this implementation object of type 
 * <code> CumulativeDiscreteDistribution </code> are immutable.
 * </p>
 * <p> 
 * This implementation is part of the master's thesis: "Studie
 * en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd
 * rangschikken", Stijn Lievens, Ghent University, 2004. 
 * </p>
 * 
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision$
 */
public class CumulativeDiscreteDistribution
  implements Serializable, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = -2959806903004453176L;

  /** 
   * small tolerance to account for rounding errors when working
   * with doubles
   */
  private static final double TOLERANCE = Utils.SMALL;

  /** The cumulative probabilities */
  private double[] m_cdf;

  /** 
   * Create a discrete cumulative probability distribution based on a 
   * <code> DiscreteEstimator. </code>
   *
   * @param e the <code> DiscreteEstimator </code> on which the 
   * cumulative probability distribution will be based
   */
  public CumulativeDiscreteDistribution(DiscreteEstimator e) {
    m_cdf = new double[e.getNumSymbols()];

    if (m_cdf.length != 0) {
      m_cdf[0] = e.getProbability(0);
    }
    for (int i = 1; i < m_cdf.length; i++) {
      m_cdf[i] = m_cdf[i - 1] + e.getProbability(i);
    }
  }

  /** 
   * Create a <code> CumulativeDiscreteDistribution </code> based on a 
   * <code> DiscreteDistribution. </code>
   *
   * @param d the <code> DiscreteDistribution </code> on which the
   * cumulative probability distribution will be based
   */
  public CumulativeDiscreteDistribution(DiscreteDistribution d) {
    m_cdf = new double[d.getNumSymbols()];

    if (m_cdf.length != 0) {
      m_cdf[0] = d.getProbability(0);
    }
    for (int i = 1; i < m_cdf.length; i++) {
      m_cdf[i] = m_cdf[i - 1] + d.getProbability(i);
    }
  }

  /**
   * Create a <code> CumulativeDiscreteDistribution </code> based on an 
   * array of doubles.  The array <code> cdf </code> is copied, so
   * the caller can reuse the same array.
   *
   * @param cdf an array that represents a valid discrete cumulative 
   * probability distribution 
   * @throws IllegalArgumentException if the array doesn't 
   * represent a valid cumulative discrete distribution function
   */
  public CumulativeDiscreteDistribution(double[] cdf) throws IllegalArgumentException {
    if (!validCumulativeDistribution(cdf)) {
      throw new IllegalArgumentException
      ("Not a cumulative probability distribution");
    }
    m_cdf = new double[cdf.length];
    System.arraycopy(cdf, 0, m_cdf, 0, cdf.length);
  }

  /** 
   * Get the number of elements over which the cumulative
   * probability distribution is defined.
   * 
   * @return the number of elements over which the cumulative 
   * probability distribution is defined.
   */
  public int getNumSymbols() {
    return  (m_cdf != null) ? m_cdf.length : 0; 
  }

  /**
   * Get the probability of finding an element 
   * smaller or equal than <code> index. </code>
   * 
   * @param index the required index
   * @return the probability of finding an element &lt;= index 
   */
  public double getCumulativeProbability(int index) {
    return m_cdf[index];
  }

  /** 
   * Get an array representation of the cumulative probability
   * distribution.
   * 
   * @return an array of doubles representing the cumulative
   * probability distribution
   */
  public double[] toArray() {
    double cdf[] = new double[m_cdf.length];
    System.arraycopy(m_cdf, 0, cdf, 0, cdf.length);
    return cdf;
  }

  /**
   * Returns if <code> this </code> is dominated by <code> cdf. </code> 
   * This means that we check if, for all indices <code> i </code>, it
   * holds that <code> this.getProbability(i) &gt;= cdf.getProbability(i).
   * </code>
   * 
   * @param cdf the <code> CumulativeDiscreteDistribution </code>
   * <code> this </code> is compared to
   * @return <code> true </code> if <code> this </code> is dominated by 
   * <code> cdf </code>, <code> false </code> otherwise
   * @throws IllegalArgumentException if the two distributions don't
   * have the same length
   */
  public boolean stochasticDominatedBy(CumulativeDiscreteDistribution cdf) throws IllegalArgumentException {
    if (getNumSymbols() != cdf.getNumSymbols()) {
      throw new IllegalArgumentException
      ("Cumulative distributions are not defined over"
	  + " the same number of symbols");
    }
    for (int i = 0; i < m_cdf.length; i++) {
      if (m_cdf[i] < cdf.m_cdf[i]) {
	return false;
      }
    }
    return true;
  }

  /**
   * Indicates if the object <code> o </code> equals <code> this. </code>
   * Note: for practical reasons I was forced to use a small tolerance
   * whilst comparing the distributions, meaning that the transitivity
   * property of <code> equals </code> is not guaranteed.
   *
   * @param o the reference object with which to compare
   * @return <code> true </code> if <code> o </code> equals <code>
   * this, </code> <code> false </code> otherwise
   */
  public boolean equals(Object o) {
    if (!(o instanceof CumulativeDiscreteDistribution)) {
      return false;
    }
    CumulativeDiscreteDistribution cdf = 
      (CumulativeDiscreteDistribution) o;
    if (m_cdf.length != cdf.getNumSymbols()) {
      return false;
    }
    for (int i = 0; i < m_cdf.length; i++) {
      if (Math.abs(m_cdf[i] - cdf.m_cdf[i]) > TOLERANCE) {
	return false;
      }
    }
    return true;
  }

  /** 
   * Get a string representation of the cumulative probability
   * distribution.
   * 
   * @return a string representation of the distribution.
   */
  public String toString() {
    // XXX MAYBE WE SHOULD USE STRINGBUFFER AND USE A FIXED
    // NUMBER OF DECIMALS BEHIND THE COMMA
    String s = "[" + getNumSymbols() + "]:";
    for (int i = 0; i < getNumSymbols(); i++)
      s += " " + getCumulativeProbability(i);
    return s;
  }

  /** 
   * Checks if the given array represents a valid cumulative 
   * distribution.
   *
   * @param cdf an array holding the presumed cumulative distribution
   * @return <code> true </code> if <code> cdf </code> represents
   * a valid cumulative discrete distribution function, <code> false
   * </code> otherwise
   */
  private static boolean validCumulativeDistribution(double[] cdf) {
    if (cdf == null || cdf.length == 0 || 
	Math.abs(cdf[cdf.length - 1] - 1.) > TOLERANCE || cdf[0] < 0) {
      return false;
    }
    for (int i = 1; i < cdf.length; i++) {

      // allow small tolerance for increasing check
      if (cdf[i] < cdf[i - 1] - TOLERANCE) {   
	return false;
      }
    }
    return true;
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
