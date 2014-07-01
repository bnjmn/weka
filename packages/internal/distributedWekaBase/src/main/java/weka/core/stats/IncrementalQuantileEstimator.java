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
 *    IncrementalQuantileEstimator.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stats;

import java.util.Arrays;

import weka.core.AttributeStats;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;

/**
 * <!-- globalinfo-start --> <!-- globalinfo-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class IncrementalQuantileEstimator implements
  TechnicalInformationHandler {

  /** The percentile to compute (default = median) */
  protected double m_p = 0.5;

  /** True if we've seen the 5 initial values required and have initialized */
  protected boolean m_initialized;

  /** Marker heights */
  protected double[] m_q = new double[5];

  /** Marker positions */
  protected int[] m_n = { 1, 2, 3, 4, 5 };

  /** Desired marker positions */
  protected double[] m_nPrime = new double[5];

  /** Increment for marker positions */
  protected double[] m_nPrimeIncrement = new double[5];

  /** Number of values seen so far */
  protected int m_count;

  protected static final double ZERO_PRECISION = 1e-6;

  /**
   * Constructs a new estimator with quantile = 0.5 (i.e. median)
   */
  public IncrementalQuantileEstimator() {
    this(0.5);
  }

  /**
   * Constructor
   * 
   * @param p the quantile to estimate
   */
  public IncrementalQuantileEstimator(double p) {
    m_p = p;
  }

  /**
   * Global information about this estimator
   * 
   * @return the global information for this estimator
   */
  public String globalInfo() {
    return "Class for estimating an arbitrary quantile incrementally using "
      + "the P^2 algorithm. For more information see:\n\n"
      + getTechnicalInformation().toString();
  }

  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result = null;

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Raj Jain and Imrich Chlamtac");
    result.setValue(Field.TITLE,
      "The P^2 Algorithm for Dynamic Calculation of Quantiles and "
        + "Histograms Without Storing Observations");
    result.setValue(Field.JOURNAL, "Communications of the ACM");
    result.setValue(Field.YEAR, "1985");
    result.setValue(Field.VOLUME, "28");
    result.setValue(Field.NUMBER, "10");
    result.setValue(Field.PAGES, "1076-1085");

    return result;
  }

  /**
   * Initialize the estimator - we require 5 values to initialize with
   */
  protected void initialize() {
    m_nPrime[0] = 1;
    m_nPrime[1] = 1.0 + (2.0 * m_p);
    m_nPrime[2] = 1.0 + (4.0 * m_p);
    m_nPrime[3] = 3.0 + (2.0 * m_p);

    m_nPrimeIncrement[0] = 0;
    m_nPrimeIncrement[1] = m_p / 2.0;
    m_nPrimeIncrement[2] = m_p;
    m_nPrimeIncrement[3] = (1.0 + m_p) / 2.0;
    m_nPrimeIncrement[4] = 1.0;

    Arrays.sort(m_q);
  }

  /**
   * Return the current estimate of the quantile
   * 
   * @return the current estimate of the quantile
   * @throws Exception if we have not yet seen 5 values to base the estimate on
   */
  public double getQuantile() throws Exception {
    if (m_count >= 5) {
      return m_q[2] < ZERO_PRECISION ? 0.0 : m_q[2];
    }

    throw new Exception(
      "Can't return estimate if fewer than 5 values have been seen");
  }

  /**
   * Add a new observation
   * 
   * @param val the new observed value
   */
  public void add(double val) {
    if (m_count < 5) {
      m_q[m_count++] = val;

      if (m_count == 5) {
        initialize();
      }
      return;
    }

    int k = -1;

    if (val < m_q[0]) {
      m_q[0] = val;
      k = 1;
    } else {
      for (int i = 1; i < m_q.length; i++) {
        if (m_q[i - 1] <= val && val < m_q[i]) {
          k = i;
          break;
        }
      }
    }
    if (k == -1) {
      k = 4;
      if (m_q[4] < val) {
        m_q[4] = val;
      }
    }

    // increment positions
    for (int i = k; i < m_n.length; i++) {
      m_n[i]++;
    }
    for (int i = 0; i < m_nPrime.length; i++) {
      m_nPrime[i] += m_nPrimeIncrement[i];
    }

    adjustMarkerHeights();
  }

  /**
   * Performs the p-squared piecewise parabolic fit
   * 
   * @param qp1 marker height to the right of q
   * @param q marker height q
   * @param qm1 marker height to the left of q
   * @param d difference between desired and actual marker positions
   * @param np1 marker position to the right of q
   * @param n marker position at q
   * @param nm1 marker position to the left of q
   * @return the adjusted marker height for q
   */
  protected static double calculatePSquared(double qp1, double q, double qm1,
    double d, double np1, double n, double nm1) {

    double first = d / (np1 - nm1);
    double second = (n - nm1 + d) * (qp1 - q) / (np1 - n);
    double third = (np1 - n - d) * (q - qm1) / (n - nm1);

    return q + first * (second + third);
  }

  /**
   * Adjust the marker heights
   */
  protected void adjustMarkerHeights() {
    for (int i = 1; i < m_n.length - 1; i++) {
      double d = m_nPrime[i] - m_n[i];
      double q = m_q[i];
      double n = m_n[i];

      if ((d >= 1 && m_n[i + 1] - m_n[i] > 1)
        || (d <= -1 && m_n[i - 1] - m_n[i] < -1)) {
        d = Math.copySign(1.0, d);

        double qPlus1 = m_q[i + 1];
        double qMinus1 = m_q[i - 1];
        double nPlus1 = m_n[i + 1];
        double nMinus1 = m_n[i - 1];

        double qNew =
          calculatePSquared(qPlus1, q, qMinus1, d, nPlus1, n, nMinus1);
        if (qMinus1 < qNew && qNew < qPlus1) {
          m_q[i] = qNew;
        } else {
          // linear adjust
          m_q[i] = q + d * (m_q[i + (int) d] - q) / (m_n[i + (int) d] - n);
        }
        m_n[i] = (int) (n + d);
      }
    }
  }

  public static void main(String[] args) {
    try {
      weka.core.Instances inst =
        new weka.core.Instances(new java.io.FileReader(args[0]));

      double quantile = Double.parseDouble(args[1]);
      IncrementalQuantileEstimator ps =
        new IncrementalQuantileEstimator(quantile);

      int attIndex = Integer.parseInt(args[2]) - 1;

      for (int i = 0; i < inst.numInstances(); i++) {
        if (!inst.instance(i).isMissing(attIndex)) {
          ps.add(inst.instance(i).value(attIndex));
        }
      }

      System.err.println("Estimated quantile (" + quantile + ") "
        + ps.getQuantile());

      inst.sort(attIndex);
      double actualQuant = 0;
      AttributeStats as = inst.attributeStats(attIndex);
      double pIndex = quantile * (inst.numInstances() - as.missingCount);
      double mean = as.numericStats.mean;
      if (pIndex - (int) pIndex > 0) {
        pIndex = (int) pIndex;
        actualQuant = inst.instance((int) pIndex).value(attIndex);
      } else {
        double f = inst.instance((int) pIndex - 1).value(attIndex);
        double s = inst.instance((int) pIndex).value(attIndex);
        actualQuant = (f + s) / 2.0;
      }

      System.err.println("Actual quantile (" + quantile + ") " + actualQuant);
      System.err.println("Mean: " + mean);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
