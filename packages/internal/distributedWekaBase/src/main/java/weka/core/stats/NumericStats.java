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
 *    NumericStats
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stats;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import weka.core.Attribute;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;

import com.clearspring.analytics.stream.quantile.TDigest;

/**
 * Class for computing numeric stats
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class NumericStats extends Stats implements Serializable {

  /** Default compression for TDigest quantile estimators */
  public static final double Q_COMPRESSION = 50.0;

  /** For serialization */
  private static final long serialVersionUID = 5328158049841703129L;

  /** Holds the actual stats values */
  protected double[] m_stats =
    new double[ArffSummaryNumericMetric.values().length];

  /** For quantiles/histograms (if we are estimating them) */
  protected TDigest m_quantileEstimator;

  /**
   * The compression level to use (bigger = less compression/more accuracy/more
   * space/more time)
   */
  protected double m_quantileCompression = Q_COMPRESSION;

  /** Labels for a histogram */
  List<String> m_binLabels;

  /** Bin frequencies */
  List<Double> m_binFreqs;

  /**
   * Constructs a new NumericStats
   * 
   * @param attributeName the name of the attribute that these statistics are
   *          for
   */
  public NumericStats(String attributeName) {
    super(attributeName);

    m_stats[ArffSummaryNumericMetric.MIN.ordinal()] = Utils.missingValue();
    m_stats[ArffSummaryNumericMetric.MAX.ordinal()] = Utils.missingValue();
    m_stats[ArffSummaryNumericMetric.FIRSTQUARTILE.ordinal()] =
      Utils.missingValue();
    m_stats[ArffSummaryNumericMetric.MEDIAN.ordinal()] = Utils.missingValue();
    m_stats[ArffSummaryNumericMetric.THIRDQUARTILE.ordinal()] =
      Utils.missingValue();
  }

  /**
   * Construct a new NumericStats
   * 
   * @param attributeName the name of the attribute that these statistics are
   *          for
   * @param quantileCompression the degree of compression for quantile
   *          estimation (bigger = less compression)
   */
  public NumericStats(String attributeName, double quantileCompression) {
    this(attributeName);

    m_quantileCompression = quantileCompression;
  }

  /**
   * Update the incremental aggregateable portions of this NumericStats with the
   * supplied value
   * 
   * @param value the value to update with
   * @param weight the weight to use
   * @param treatZeroAsMissing true if zeros count as missing
   * @param updateQuantiles true if we should update our quantile estimator
   */
  public void update(double value, double weight, boolean treatZeroAsMissing,
    boolean updateQuantiles) {
    if (Utils.isMissingValue(value) || (treatZeroAsMissing && value == 0)) {
      m_stats[ArffSummaryNumericMetric.MISSING.ordinal()] += weight;
    } else {
      m_stats[ArffSummaryNumericMetric.COUNT.ordinal()] += weight;
      m_stats[ArffSummaryNumericMetric.SUM.ordinal()] += value * weight;
      m_stats[ArffSummaryNumericMetric.SUMSQ.ordinal()] +=
        value * value * weight;
      if (Double.isNaN(m_stats[ArffSummaryNumericMetric.MIN.ordinal()])) {
        m_stats[ArffSummaryNumericMetric.MIN.ordinal()] =
          m_stats[ArffSummaryNumericMetric.MAX.ordinal()] = value;
      } else if (value < m_stats[ArffSummaryNumericMetric.MIN.ordinal()]) {
        m_stats[ArffSummaryNumericMetric.MIN.ordinal()] = value;
      } else if (value > m_stats[ArffSummaryNumericMetric.MAX.ordinal()]) {
        m_stats[ArffSummaryNumericMetric.MAX.ordinal()] = value;
      }

      if (updateQuantiles) {
        if (m_quantileEstimator == null) {
          m_quantileEstimator =
            new TDigest(m_quantileCompression, new Random(1));
        }
        m_quantileEstimator.add(value, (int) (weight < 1 ? 1 : weight));
      }
    }
  }

  /**
   * Return the array of statistics
   * 
   * @return the array of statistics
   */
  public double[] getStats() {
    return m_stats;
  }

  /**
   * Sets the array of statistics. Does not check to see if the supplied array
   * is of the correct length.
   * 
   * @param stats the stats array to use
   */
  public void setStats(double[] stats) {
    m_stats = stats;
  }

  /**
   * Get the quantile estimator in use (if any)
   * 
   * @return the quantile estmator
   */
  public TDigest getQuantileEstimator() {
    return m_quantileEstimator;
  }

  /**
   * Set the quantile estimator to use
   * 
   * @param estimator the estimator to use
   */
  public void setQuantileEstimator(TDigest estimator) {
    m_quantileEstimator = estimator;
  }

  /**
   * Set histogram data for this numeric stats
   * 
   * @param labs bin labels
   * @param freqs bin frequencies
   */
  public void setHistogramData(List<String> labs, List<Double> freqs) {
    m_binLabels = labs;
    m_binFreqs = freqs;
  }

  /**
   * Get the histogram labels
   * 
   * @return the list of histogram labels or null if not set
   */
  public List<String> getHistogramBinLabels() {
    return m_binLabels;
  }

  /**
   * Get the histogram bin frequencies
   * 
   * @return the list of histogram bin frequencies or null if not set
   */
  public List<Double> getHistogramFrequencies() {
    return m_binFreqs;
  }

  @Override
  public Attribute makeAttribute() {
    ArrayList<String> vals = new ArrayList<String>();

    for (ArffSummaryNumericMetric m : ArffSummaryNumericMetric.values()) {
      if (m.ordinal() > m_stats.length - 1) {
        continue;
      }
      if (m == ArffSummaryNumericMetric.FIRSTQUARTILE
        || m == ArffSummaryNumericMetric.MEDIAN
        || m == ArffSummaryNumericMetric.THIRDQUARTILE) {
        if (Utils
          .isMissingValue(m_stats[ArffSummaryNumericMetric.FIRSTQUARTILE
            .ordinal()])
          && Utils.isMissingValue(m_stats[ArffSummaryNumericMetric.MEDIAN
            .ordinal()])
          && Utils
            .isMissingValue(m_stats[ArffSummaryNumericMetric.THIRDQUARTILE
              .ordinal()])) {
          continue;
        }
      }
      String v = m.makeAttributeValue(m_stats[m.ordinal()]);
      vals.add(v);
    }

    // histogram (if present)
    if (m_binLabels != null && m_binLabels.size() > 0) {
      StringBuilder b = new StringBuilder();
      for (int i = 0; i < m_binLabels.size(); i++) {
        String v = m_binLabels.get(i) + ":" + m_binFreqs.get(i);
        b.append(v);
        if (i < m_binLabels.size() - 1) {
          b.append("!");
        }
      }
      vals.add(b.toString());
    }

    Attribute a =
      new Attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
        + m_attributeName, vals);
    return a;
  }

  /**
   * Convert a summary meta attribute into a NumericStats object (does not
   * recover the internal TDigest quantile estimator)
   * 
   * @param a the summary meta attribute to convert
   * @return a NumericStats instance
   * @throws IllegalArgumentException if a problem occurs
   */
  public static NumericStats attributeToStats(Attribute a)
    throws IllegalArgumentException {
    if (!a.isNominal()) {
      throw new IllegalArgumentException("Stats attribute is not nominal!");
    }

    // we assume that either just the set of aggregateable stats will
    // be present or all the stats (i.e. + quartiles and histogram)
    if (a.numValues() != ArffSummaryNumericMetric.values().length + 1
      && a.numValues() != ArffSummaryNumericMetric.values().length - 3) {
      throw new IllegalArgumentException("Was expecting there to be either "
        + (ArffSummaryNumericMetric.values().length + 1) + " or "
        + (ArffSummaryNumericMetric.values().length - 3)
        + " values in a summary attribute, but found " + a.numValues());
    }

    double[] stats = new double[ArffSummaryNumericMetric.values().length];
    stats[ArffSummaryNumericMetric.MIN.ordinal()] = Utils.missingValue();
    stats[ArffSummaryNumericMetric.MAX.ordinal()] = Utils.missingValue();
    stats[ArffSummaryNumericMetric.FIRSTQUARTILE.ordinal()] =
      Utils.missingValue();
    stats[ArffSummaryNumericMetric.MEDIAN.ordinal()] = Utils.missingValue();
    stats[ArffSummaryNumericMetric.THIRDQUARTILE.ordinal()] =
      Utils.missingValue();

    for (ArffSummaryNumericMetric m : ArffSummaryNumericMetric.values()) {

      if (m.ordinal() < a.numValues()) {
        String v = a.value(m.ordinal());

        double value = m.toValue(v, m.toString());
        stats[m.ordinal()] = value;
      }
    }

    List<String> histLabs = null;
    List<Double> histFreqs = null;
    if (a.numValues() > ArffSummaryNumericMetric.values().length) {
      String hist = a.value(a.numValues() - 1);
      histLabs = new ArrayList<String>();
      histFreqs = new ArrayList<Double>();

      String[] parts = hist.split("!");
      for (String p : parts) {
        String[] entry = p.split(":");
        histLabs.add(entry[0]);
        histFreqs.add(Double.parseDouble(entry[1]));
      }
    }

    NumericStats s =
      new NumericStats(a.name().replace(
        CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX, ""));
    s.m_stats = stats;
    s.setHistogramData(histLabs, histFreqs);

    return s;
  }

  /**
   * Compute the derived statistics
   */
  public void computeDerived() {
    double count = m_stats[ArffSummaryNumericMetric.COUNT.ordinal()];
    double sum = m_stats[ArffSummaryNumericMetric.SUM.ordinal()];
    double sumSq = m_stats[ArffSummaryNumericMetric.SUMSQ.ordinal()];
    double mean = 0;

    double stdDev = 0;
    if (count > 0) {
      mean = sum / count;
      // stdDev = Double.POSITIVE_INFINITY;
      if (count > 1) {
        stdDev = sumSq - (sum * sum) / count;
        stdDev /= (count - 1);
        if (stdDev < 0) {
          stdDev = 0;
        }
        stdDev = Math.sqrt(stdDev);
      }
    }

    m_stats[ArffSummaryNumericMetric.MEAN.ordinal()] = mean;
    m_stats[ArffSummaryNumericMetric.STDDEV.ordinal()] = stdDev;
  }

  /**
   * Computes derived stats and computes quartiles and histogram data from our
   * quantile estimator. If quartiles are not being estimated then the result is
   * just to call computeDerived().
   */
  public void computeQuartilesAndHistogram() {
    computeDerived();

    if (m_quantileEstimator == null) {
      return;
    }

    m_stats[ArffSummaryNumericMetric.FIRSTQUARTILE.ordinal()] =
      m_quantileEstimator.quantile(0.25);
    m_stats[ArffSummaryNumericMetric.MEDIAN.ordinal()] =
      m_quantileEstimator.quantile(0.5);
    m_stats[ArffSummaryNumericMetric.THIRDQUARTILE.ordinal()] =
      m_quantileEstimator.quantile(0.75);

    double min = m_stats[ArffSummaryNumericMetric.MIN.ordinal()];
    double count = m_stats[ArffSummaryNumericMetric.COUNT.ordinal()];
    NumericAttributeBinData binData =
      new NumericAttributeBinData(m_attributeName, count, min,
        m_stats[ArffSummaryNumericMetric.MAX.ordinal()],
        m_stats[ArffSummaryNumericMetric.STDDEV.ordinal()],
        m_stats[ArffSummaryNumericMetric.MISSING.ordinal()], -1);

    // heuristic based on count & std. dev
    int numBins = binData.getNumBins();

    // Check against compression (another hokey heuristic)
    numBins = Math.min(numBins, (int) m_quantileCompression * 2 / 10);
    if (numBins != binData.getNumBins()) {
      binData =
        new NumericAttributeBinData(m_attributeName, count, min,
          m_stats[ArffSummaryNumericMetric.MAX.ordinal()],
          m_stats[ArffSummaryNumericMetric.STDDEV.ordinal()],
          m_stats[ArffSummaryNumericMetric.MISSING.ordinal()], numBins);
    }

    double binWidth = binData.getBinWidth();
    double prev = 0;

    for (int i = 0; i < numBins; i++) {
      double lower = min + (i * binWidth);
      double upper = min + ((i + 1) * binWidth);
      double midVal = lower + ((upper - lower) / 2.0);
      double cdf = m_quantileEstimator.cdf(upper);

      boolean ok = !Double.isInfinite(cdf) && !Double.isNaN(cdf);

      double freq = ok ? cdf : 0;
      if (i > 0 && ok) {
        freq = cdf - prev;
      }
      if (freq < 0) {
        freq = 0;
      }
      freq *= count;

      binData.addValue(midVal, freq);

      if (ok) {
        prev = cdf;
      }
    }

    m_binLabels = binData.getBinLabels();
    m_binFreqs = binData.getBinFreqs();
  }
}
