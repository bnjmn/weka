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
 *    NumericAttributeBinData
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stats;

import weka.core.Aggregateable;
import weka.core.Attribute;
import weka.core.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for managing bin data for a histogram based on an attribute
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class NumericAttributeBinData implements
  Aggregateable<NumericAttributeBinData> {

  /** Maximum bins to create */
  public static final int MAX_BINS = 10;

  /** The name of the attribute */
  protected String m_attName;

  /** Holds the number of bins chosen for this attribute */
  protected int m_numBins;

  /** Holds the width of each bin (equal width bins) */
  protected double m_binWidth;

  /** The cut points for the bins */
  protected double[] m_binCutpoints;

  /** The frequency of each bin */
  protected double[] m_binFreqs;

  /** The frequency of missing value */
  protected double m_missingFreq;

  /**
   * Constructor
   * 
   * @param attName the name of the attribute
   * @param summaryAtt the summary attribute containing the bin cutpoints and
   *          frequencies
   * @param maxBins the maximum number of bins to allow (if setting bin
   *          cutpoints based on range and overall count) or -1 to use the
   *          default max.
   */
  public NumericAttributeBinData(String attName, Attribute summaryAtt,
    int maxBins) {
    m_attName = attName;

    double missingFreq =
      ArffSummaryNumericMetric.MISSING.valueFromAttribute(summaryAtt);

    double numPoints =
      ArffSummaryNumericMetric.COUNT.valueFromAttribute(summaryAtt);
    double min = ArffSummaryNumericMetric.MIN.valueFromAttribute(summaryAtt);
    double max = ArffSummaryNumericMetric.MAX.valueFromAttribute(summaryAtt);
    double stdDev =
      ArffSummaryNumericMetric.STDDEV.valueFromAttribute(summaryAtt);

    NumericStats stats = NumericStats.attributeToStats(summaryAtt);
    List<String> binLabels = stats.getHistogramBinLabels();
    List<Double> binFreqs = stats.getHistogramFrequencies();

    setup(attName, numPoints, min, max, stdDev, missingFreq, binLabels,
      binFreqs, maxBins);
  }

  /**
   * Constructor
   * 
   * @param attName the name of the attribute
   * @param numPoints the number of points that have been seen for this
   *          attribute
   * @param min the minimum value
   * @param max the maximum value
   * @param stdDev the standard deviation
   * @param missingFreq the number of missing values
   * @param maxBins the maximum number of bins to allow (if setting bin
   *          cutpoints based on range and overall count) or -1 to use the
   *          default max.
   */
  public NumericAttributeBinData(String attName, double numPoints, double min,
    double max, double stdDev, double missingFreq, int maxBins) {
    setup(attName, numPoints, min, max, stdDev, missingFreq, null, null,
      maxBins);
  }

  /**
   * Constructor
   * 
   * @param attName the name of the attribute
   * @param numPoints the number of points that have been seen for this
   *          attribute
   * @param min the minimum value
   * @param max the maximum value
   * @param stdDev the standard deviation
   * @param missingFreq the number of missing values
   * @param lowerPercentile the lower percentile to use for the equal width
   *          binning
   * @param upperPercentile the upper percentile to use for hte equal width
   *          binning
   * @param maxBins the maximum number of bins to use
   */
  public NumericAttributeBinData(String attName, double numPoints, double min,
    double max, double stdDev, double missingFreq, double lowerPercentile,
    double upperPercentile, int maxBins) {

    if (upperPercentile - lowerPercentile > 0) {
      setupWithPercentiles(attName, numPoints, min, max, stdDev, missingFreq,
        lowerPercentile, upperPercentile, maxBins);
    } else {
      setup(attName, numPoints, min, max, stdDev, missingFreq, null, null,
        maxBins);
    }
  }

  /**
   * Set up histogram using upper and lower bound percentiles
   *
   * @param attName the name of the attribute
   * @param numPoints the number of points seen
   * @param min the minimum
   * @param max the maximum
   * @param stdDev the standard deviation
   * @param missingFreq the number of missing values
   * @param lowPercentile the lower bound percentile to use
   * @param highPercentile the upper bound percentile to use
   * @param maxBins the maximum number of bins to allow (if setting bin
   *          cutpoints based on range and overall count) or -1 to use the
   *          default max.
   */
  protected void setupWithPercentiles(String attName, double numPoints,
    double min, double max, double stdDev, double missingFreq,
    double lowPercentile, double highPercentile, int maxBins) {

    m_missingFreq = missingFreq;

    // number of bins for the region where 90% of the data resides
    // reserve a bin each for low and high outliers
    double adjustedNumPoints = (highPercentile - lowPercentile) * numPoints;
    int numBins =
      // numPoints > 0 ? (maxBins > 0 ? maxBins - 2 : MAX_BINS - 2)
      // : 0;
      numPoints > 0 ? numBinsHeuristic(stdDev, adjustedNumPoints,
        lowPercentile, highPercentile, maxBins > 0 ? maxBins - 2 : MAX_BINS - 2)
        : 0;

    m_binCutpoints = new double[numBins + 2];
    m_binFreqs = new double[numBins + 2];

    m_binCutpoints[0] = lowPercentile;
    m_binCutpoints[m_binCutpoints.length - 1] = max;
    double step = 0;
    if (numBins > 0) {
      double range = highPercentile - lowPercentile;
      step = range / numBins;

      for (int i = 1; i <= numBins; i++) {
        m_binCutpoints[i] = lowPercentile + (i * step);
      }
    }

    m_numBins = numBins + 2;

    // width of the bins that lie between 10th and 90th percentile
    m_binWidth = step;
  }

  /**
   * Set up histogram using equal width between min and max
   * 
   * @param attName the name of the attribute
   * @param numPoints the number of points seen
   * @param min the minimum
   * @param max the maximum
   * @param stdDev the standard deviation
   * @param missingFreq the number of missing values
   * @param binLabels a list of bin labels to use (may be null)
   * @param binFreqs a list of frequencies corresponding to bins (may be null)
   * @param maxBins the maximum number of bins to allow (if setting bin
   *          cutpoints based on range and overall count) or -1 to use the
   *          default max.
   */
  protected void setup(String attName, double numPoints, double min,
    double max, double stdDev, double missingFreq, List<String> binLabels,
    List<Double> binFreqs, int maxBins) {

    m_missingFreq = missingFreq;

    if (binLabels == null && binFreqs == null) {
      int numBins =
        numPoints > 0 ? numBinsHeuristic(stdDev, numPoints, min, max,
          maxBins > 0 ? maxBins : MAX_BINS) : 0;

      m_binCutpoints = new double[numBins];
      m_binFreqs = new double[numBins];

      double step = 0;
      if (numBins > 0) {
        double range = max - min;
        step = range / numBins;

        for (int i = 0; i < numBins; i++) {
          if (i == numBins - 1) {
            m_binCutpoints[i] = max;
          } else {
            m_binCutpoints[i] = min + ((i + 1) * step);
          }
        }
      }

      m_numBins = numBins;
      m_binWidth = step;
    } else {
      m_numBins = binLabels.size();
      m_binCutpoints = new double[m_numBins];
      m_binFreqs = new double[m_numBins];

      for (int i = 0; i < binLabels.size(); i++) {
        String l = binLabels.get(i).replace("]", "");
        m_binCutpoints[i] = Double.parseDouble(l);

        m_binFreqs[i] = binFreqs.get(i);
      }

      m_binWidth =
        m_numBins > 1 ? m_binCutpoints[1] - m_binCutpoints[0] : max - min;
    }
  }

  /**
   * Get the number of bins for this attribute
   * 
   * @return the number of bins
   */
  public int getNumBins() {
    return m_numBins;
  }

  /**
   * Get the bin width for this attribute
   * 
   * @return the bin width for this attribute
   */
  public double getBinWidth() {
    return m_binWidth;
  }

  /**
   * Get a list of bin labels for this histogram
   * 
   * @return a list of bin labels
   */
  public List<String> getBinLabels() {
    List<String> labs = new ArrayList<String>();

    for (double c : m_binCutpoints) {
      labs.add(Utils.doubleToString(c, 3) + "]");
    }

    return labs;
  }

  /**
   * Get a list of bin frequencies for this histogram
   * 
   * @return a list of bin frequencies
   */
  public List<Double> getBinFreqs() {
    List<Double> freqs = new ArrayList<Double>();

    for (double f : m_binFreqs) {
      freqs.add(f);
    }

    return freqs;
  }

  /**
   * Get the number of missing values
   * 
   * @return the number of missing values
   */
  public double getMissingFreq() {
    return m_missingFreq;
  }

  /**
   * Get the name of the attribute that this histogram is for
   * 
   * @return the name of the attribute that this histogram is for
   */
  public String getAttributeName() {
    return m_attName;
  }

  /**
   * Add a value to the histogram. Finds the correct bin and increases the
   * frequency
   * 
   * @param value the value to add
   * @param weight the weight
   */
  public void addValue(double value, double weight) {
    for (int i = 0; i < m_binCutpoints.length; i++) {
      if (value <= m_binCutpoints[i]) {
        m_binFreqs[i] += weight;
        break;
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();

    List<String> labs = getBinLabels();
    for (int i = 0; i < m_binCutpoints.length; i++) {
      b.append(labs.get(i)).append(" : ").append("" + m_binFreqs[i])
        .append("\n");
    }

    return b.toString();
  }

  @Override
  public NumericAttributeBinData aggregate(NumericAttributeBinData b)
    throws Exception {

    if (!b.m_attName.equals(m_attName)) {
      throw new Exception(
        "Can't aggregate histograms for different attributes!");
    }

    if (b.m_binCutpoints.length != m_binCutpoints.length) {
      throw new Exception("Can't aggregate histogram data for attribute '"
        + m_attName + "' - differing numbers of bins");
    }

    // don't aggregate missing as this is already global (computed on
    // the first pass over the data)

    for (int i = 0; i < m_binFreqs.length; i++) {
      m_binFreqs[i] += b.m_binFreqs[i];
    }

    return this;
  }

  @Override
  public void finalizeAggregation() throws Exception {
    // Nothing to do
  }

  /**
   * Compute the number of bins for a histogram given summary stats
   * 
   * @param stdDev the standard deviation of the variable in question
   * @param numPoints the number of observed data points
   * @param min the minimum value
   * @param max the maximum
   * @param maxBins the maximum number of bins to allow
   * @return the number of bins
   */
  public static int numBinsHeuristic(double stdDev, double numPoints,
    double min, double max, int maxBins) {
    double intervalWidth =
      3.49 * stdDev * StrictMath.pow(numPoints, (-1.0 / 3.0));
    double range = max - min;
    int numBins =
      StrictMath.max(1, (int) StrictMath.round(range / intervalWidth));

    if (numBins > maxBins) {
      numBins = maxBins;
    }

    return numBins;
  }

  public static void main(String[] args) {
    try {
      double count = 4898430;
      double min = 0;
      double max = 1.379963888E9;
      double missing = 0;
      double stdDev = 941431.170584845;

      NumericAttributeBinData b =
        new NumericAttributeBinData("test", count, min, max, stdDev, missing,
          -1);
      System.out.println(b);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
