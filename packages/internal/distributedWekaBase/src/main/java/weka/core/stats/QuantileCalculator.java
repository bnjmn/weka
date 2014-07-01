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
 *    QuantileCalculator.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stats;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Class for maintaining quantile estimators for all the numeric attributes in a
 * dataset.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class QuantileCalculator implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -3781330565776266563L;

  /** The header for the dataset to be processed */
  protected Instances m_header;

  /** A map of incremental quantile estimators, keyed by attribute index */
  protected Map<Integer, List<IncrementalQuantileEstimator>> m_estimators =
    new HashMap<Integer, List<IncrementalQuantileEstimator>>();

  /** A map for looking up the index of a particular quantile */
  protected Map<Double, Integer> m_quantileIndexes =
    new HashMap<Double, Integer>();

  /**
   * Used to build histograms for numeric attributes (optional)
   */
  protected Map<Integer, NumericAttributeBinData> m_numericHistogramData;

  /**
   * Constructor
   * 
   * @param header the header of the dataset to process
   * @param quantiles an array of quantiles to compute for each numeric
   *          attribute
   */
  public QuantileCalculator(Instances header, double[] quantiles) {

    m_header = header;

    for (int i = 0; i < header.numAttributes(); i++) {
      if (header.attribute(i).isNumeric()) {
        List<IncrementalQuantileEstimator> ests =
          new ArrayList<IncrementalQuantileEstimator>();
        for (double quantile : quantiles) {
          ests.add(new IncrementalQuantileEstimator(quantile));
        }

        m_estimators.put(i, ests);
      }
    }

    for (int i = 0; i < quantiles.length; i++) {
      m_quantileIndexes.put(quantiles[i], i);
    }
  }

  /**
   * Set a map of initialized numeric histogram data to be updated for each
   * incoming row/instance
   * 
   * @param initializedHistData a map of NumericAttributeBinData that has been
   *          initialized. Keyed by attribute index
   */
  public void setHistogramMap(
    Map<Integer, NumericAttributeBinData> initializedHistData) {

    m_numericHistogramData = initializedHistData;
  }

  /**
   * Perform an update using an instance represented as an array of string
   * values
   * 
   * @param row the row to update with
   * @param missingValue the missing value string
   * @throws Exception if a problem occurs
   */
  public void update(String[] row, String missingValue) throws Exception {
    for (Map.Entry<Integer, List<IncrementalQuantileEstimator>> e : m_estimators
      .entrySet()) {
      int index = e.getKey();
      if (row[index] != null && row[index].length() > 0
        && !row[index].trim().equals(missingValue)) {
        try {
          double value = Double.parseDouble(row[index]);
          for (int i = 0; i < e.getValue().size(); i++) {
            e.getValue().get(i).add(value);
          }

          if (m_numericHistogramData != null) {
            // update histogram for this field
            NumericAttributeBinData binData = m_numericHistogramData.get(index);
            if (binData == null) {
              throw new Exception(
                "We don't seem to have bin data for attribute at index: "
                  + index);
            }

            binData.addValue(value, 1.0);
          }
        } catch (NumberFormatException ex) {
          throw new Exception(ex);
        }
      }
    }
  }

  /**
   * Perform an update using the supplied instance
   * 
   * @param inst the instance to update with
   * @throws Exception if a problem occurs
   */
  public void update(Instance inst) throws Exception {
    for (Map.Entry<Integer, List<IncrementalQuantileEstimator>> e : m_estimators
      .entrySet()) {
      int index = e.getKey();
      if (!inst.isMissing(index)) {
        double value = inst.value(index);
        for (int i = 0; i < e.getValue().size(); i++) {
          e.getValue().get(i).add(value);
        }

        if (m_numericHistogramData != null) {
          // update histogram for this field
          NumericAttributeBinData binData = m_numericHistogramData.get(index);
          if (binData == null) {
            throw new Exception(
              "We don't seem to have bin data for attribute at index: " + index);
          }

          binData.addValue(value, inst.weight());
        }
      }
    }
  }

  /**
   * Get the computed quantiles for the named attribute
   * 
   * @param attName the attribute to get the current computed quantiles for
   * @return the current values of the quantiles
   * @throws Exception if a problem occurs
   */
  public double[] getQuantiles(String attName) throws Exception {
    Attribute att = m_header.attribute(attName);

    if (att == null) {
      throw new Exception("Unknown attribute: " + attName);
    }

    if (!att.isNumeric()) {
      throw new Exception("Attribute '" + attName + "' is not numeric");
    }

    int index = att.index();
    List<IncrementalQuantileEstimator> l = m_estimators.get(index);
    double[] quantiles = new double[l.size()];
    for (int i = 0; i < l.size(); i++) {
      quantiles[i] = l.get(i).getQuantile();
    }

    return quantiles;
  }

  /**
   * Get a specific quantile for the named attribute
   * 
   * @param attName the name of the attribute to get a quantile for
   * @param quantileToGet the specific quantile to get
   * @return the current value of the quantile
   * @throws Exception if a problem occurs
   */
  public double getQuantile(String attName, double quantileToGet)
    throws Exception {
    double[] quantiles = getQuantiles(attName);

    Integer quantIndex = m_quantileIndexes.get(quantileToGet);
    if (quantIndex == null) {
      throw new Exception("We haven't computed quantile: " + quantileToGet);
    }

    return quantiles[quantIndex];
  }
}
