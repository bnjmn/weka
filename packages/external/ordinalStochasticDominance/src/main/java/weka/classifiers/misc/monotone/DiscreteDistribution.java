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
 *    DiscreteDistribution.java
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
 * This class represents a discrete probability distribution 
 * over a finite number of values.
 * <p>
 * In the present implementation, objects of type 
 * <code> DiscreteDistribution </code> are in fact immutable,
 * so all one can do is create objects and retrieve information,
 * such as median and mean, from them.
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
public class DiscreteDistribution
  implements Serializable, RevisionHandler {

  /** for serialization. */
  private static final long serialVersionUID = 1954630934425689828L;

  /**
   * small tolerance to account for rounding errors when working
   * with doubles
   */
  private static final double TOLERANCE=Utils.SMALL;

  /** the array of probabilities */
  private double[] m_dd;

  /** 
   * Create a <code> DiscreteDistribution </code> based on a 
   * <code> DiscreteEstimator. </code>
   *
   * @param e the <code> DiscreteEstimator </code>
   */
  public DiscreteDistribution(DiscreteEstimator e) {
    m_dd = new double[e.getNumSymbols()];

    for (int i = 0; i < m_dd.length; i++) {
      m_dd[i] = e.getProbability(i);
    }
  }

  /** 
   * Create a <code> DiscreteDistribution </code> based on a 
   * <code> CumulativeDiscreteDistribution. </code>
   *
   * @param cdf the <code> CumulativeDiscreteDistribution </code>
   */
  public DiscreteDistribution(CumulativeDiscreteDistribution cdf) {
    m_dd = new double[cdf.getNumSymbols()];

    if (m_dd.length != 0) {
      m_dd[0] = cdf.getCumulativeProbability(0);
    }
    for (int i = 1; i < m_dd.length; i++) {
      m_dd[i] = cdf.getCumulativeProbability(i) 
      - cdf.getCumulativeProbability(i - 1);
    }
  }

  /** 
   * Create a <code> DiscreteDistribution </code> based on an 
   * array of doubles.
   *
   * @param dd the array of doubles representing a valid 
   * discrete distribution
   * @throws IllegalArgumentException if <code> dd </code>
   * does not represent a valid discrete distribution
   */
  public DiscreteDistribution(double[] dd) throws IllegalArgumentException {
    if (!validDiscreteDistribution(dd)) {
      throw new IllegalArgumentException
      ("Not a valid discrete distribution");
    }

    m_dd = new double[dd.length];
    System.arraycopy(dd,0,m_dd,0,dd.length);
  }

  /** 
   * Get the number of elements over which the <code>
   * DiscreteDistribution </code> is defined.
   * 
   * @return the number of elements over which the <code>
   * DiscreteDistribution </code> is defined
   */
  public int getNumSymbols() {
    return  (m_dd != null) ? m_dd.length : 0; 
  }

  /** 
   * Get the probability of finding the element at
   * a specified index.
   * 
   * @param index the index of the required element
   * @return the probability of finding the specified element
   */
  public double getProbability(int index) {
    return m_dd[index];
  }

  /** 
   * Calculate the mean of the distribution.  The scores for
   * calculating the mean start from 0 and have step size one,
   * i.e. if there are n elements then the scores are 0,1,...,n-1.
   * 
   * @return the mean of the distribution
   */
  public double mean() {
    double mean = 0;
    for (int i = 1; i < m_dd.length; i++) {
      mean += i * m_dd[i];
    }
    return mean;
  }

  /** 
   * Calculate the median of the distribution.  This means
   * the following: if there is a label m such that 
   * P(x &lt;= m) &gt;= &frac12; and
   * P(x &gt;= m) &gt;= &frac12;  then this label is returned.
   * If there is no such label, an interpolation between the
   * smallest label satisfying the first condition and the
   * largest label satisfying the second condition is performed.
   * The returned value is thus either an element label, or
   * exactly between two element labels.
   * 
   * @return the median of the distribution. 
   **/
  public double median() {

    /* cumulative probabilities starting from the left and
     * right respectively 
     */
    double cl=m_dd[0];
    double cr=m_dd[m_dd.length - 1]; // cumulative left and right

    int i = 0;
    while(cl < 0.5) {
      cl += m_dd[++i]; // pre-increment
    }

    int j = m_dd.length - 1;
    while(cr < 0.5) {
      cr += m_dd[--j]; // pre-increment
    }

    return i == j ? i : ( (double) (i + j) ) / 2;
  }

  /** 
   * Get a sorted array containing the indices of the elements with
   * maximal probability.
   *
   * @return an array of class indices with maximal probability.
   */
  public int[] modes() {
    int[] mm = new int[m_dd.length];
    double max = m_dd[0];
    int nr = 1; // number of relevant elements in mm
    for (int i = 1; i < m_dd.length; i++) {
      if (m_dd[i] > max + TOLERANCE) { 

	// new maximum
	max = m_dd[i];
	mm[0] = i;
	nr = 1;
      }
      else if (Math.abs(m_dd[i] - max) < TOLERANCE) {
	mm[nr++] = i;
      }
    }

    // trim to correct size
    int[] modes = new int[nr];
    System.arraycopy(mm, 0, modes, 0, nr);
    return modes;
  }

  /**
   * Convert the <code> DiscreteDistribution </code> to an
   * array of doubles.
   * 
   * @return an array of doubles representing the
   * <code> DiscreteDistribution </code>
   */
  public double[] toArray() {
    double[] dd = new double[m_dd.length];
    System.arraycopy(m_dd, 0, dd, 0, dd.length);
    return dd;
  }

  /** 
   * Get a string representation of the given <code>
   * DiscreteDistribution. </code>
   *
   * @return a string representation of this object
   */
  public String toString() {

    // XXX MAYBE WE SHOULD USE STRINGBUFFER AND FIXED WIDTH ...
    String s = "[" + getNumSymbols() + "]:";
    for (int i = 0; i < getNumSymbols(); i++) {
      s += " " + getProbability(i);
    }
    return s;
  }

  /**
   * Checks if <code> this </code> is dominated by <code> dd. </code> 

   * @param dd the DiscreteDistribution to compare to
   * @return <code> true </code> if <code> this </code> is dominated by 
   * <code> dd </code>, <code> false </code> otherwise
   * @throws IllegalArgumentException if the two distributions don't
   * have the same length
   */
  public boolean stochasticDominatedBy(DiscreteDistribution dd) throws IllegalArgumentException {
    return (new CumulativeDiscreteDistribution(this)).
    stochasticDominatedBy
    (new CumulativeDiscreteDistribution(dd));
  }

  /** 
   * Checks if the given array of doubles represents a valid discrete 
   * distribution.
   * 
   * @param dd an array holding the doubles
   * @return <code> true </code> if <code> dd </code> is a valid discrete distribution, 
   * <code> false </code> otherwise
   */
  private static boolean validDiscreteDistribution(double[] dd) {
    if (dd == null || dd.length == 0) {
      return false;
    }

    double sum = 0;
    for (int i = 0; i < dd.length; i++) {
      if (dd[i] < 0) {
	return false;
      }
      sum += dd[i];
    }
    return !(Math.abs(sum - 1) > TOLERANCE); 
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
