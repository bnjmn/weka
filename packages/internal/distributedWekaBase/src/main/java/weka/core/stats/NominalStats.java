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
 *    NominalStats
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stats;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import weka.core.Attribute;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;

/**
 * Class for computing nominal statistics (primarily frequency counts)
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class NominalStats extends Stats implements Serializable {

  /** A map of values to counts */
  protected Map<String, NominalStats.Count> m_counts =
    new TreeMap<String, NominalStats.Count>();

  /** The number of missing values for this nominal attribute */
  protected double m_numMissing;

  /** A "label" to use when storing the number of missing values */
  public static final String MISSING_LABEL = "**missing**";

  /** For serialization */
  private static final long serialVersionUID = -6176046647546730423L;

  /**
   * Class that encapsulates a count for nominal value
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class Count implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = 4310467271632108735L;

    /** The value of the count */
    public double m_count;
  }

  /**
   * Constructs a new NominalStats
   * 
   * @param attributeName the name of the data attribute that these stats
   *          pertain to
   */
  public NominalStats(String attributeName) {
    super(attributeName);
  }

  /**
   * Convert a summary meta attribute to a NominalStats
   * 
   * @param a the attribute to convert
   * @return a NominalStats
   * @throws IllegalArgumentException if a problem occurs
   */
  public static NominalStats attributeToStats(Attribute a)
    throws IllegalArgumentException {

    if (!a.isNominal()) {
      throw new IllegalArgumentException("Stats attribute is not nominal!");
    }

    NominalStats ns = new NominalStats(a.name());
    for (int j = 0; j < a.numValues(); j++) {
      String v = a.value(j);
      String label = v.substring(0, v.lastIndexOf("_"));
      String freqCount = v.substring(v.lastIndexOf("_") + 1, v.length());
      try {
        double fC = Double.parseDouble(freqCount);
        if (label.equals(NominalStats.MISSING_LABEL)) {
          ns.add(null, fC);
        } else {
          ns.add(label, fC);
        }
      } catch (NumberFormatException n) {
        throw new IllegalArgumentException(n);
      }
    }

    return ns;
  }

  /**
   * Adds to the count for a given label. If the label is null then it adds to
   * the count for missing.
   * 
   * @param label the label to add the count to
   * @param value the count to add
   */
  public void add(String label, double value) {

    if (label == null) {
      m_numMissing += value;
    } else {

      NominalStats.Count c = m_counts.get(label);
      if (c == null) {
        c = new Count();
        m_counts.put(label, c);
      }

      c.m_count += value;
    }
  }

  /**
   * Get the set of labels seen by this NominalStats
   * 
   * @return the set of labels
   */
  public Set<String> getLabels() {
    return m_counts.keySet();
  }

  /**
   * Get the count for a given label
   * 
   * @param label the label to get the count for
   * @return the count or missing value if the label is unknown
   */
  public double getCount(String label) {
    NominalStats.Count c = m_counts.get(label);

    if (c == null) {
      return Utils.missingValue();
    }

    return c.m_count;
  }

  /**
   * Get the number of missing values for this attribute
   * 
   * @return the number of missing values seen
   */
  public double getNumMissing() {
    return m_numMissing;
  }

  /**
   * Get the index of the mode
   * 
   * @return the index (in the sorted list of labels) of the mode
   */
  public int getMode() {
    double max = -1;
    int maxIndex = -1;

    int index = 0;
    for (Map.Entry<String, NominalStats.Count> e : m_counts.entrySet()) {
      if (e.getValue().m_count > max) {
        max = e.getValue().m_count;
        maxIndex = index;
      }
      index++;
    }

    return maxIndex;
  }

  /**
   * Get the most frequent label (not including missing values)
   * 
   * @return the most frequent label
   */
  public String getModeLabel() {
    double max = -1;
    String maxLabel = "";

    for (Map.Entry<String, NominalStats.Count> e : m_counts.entrySet()) {
      if (e.getValue().m_count > max) {
        max = e.getValue().m_count;
        maxLabel = e.getKey();
      }
    }

    return maxLabel;
  }

  /**
   * Set the number of missing values for this attribute
   * 
   * @param missing the number of missing values
   */
  public void setNumMissing(double missing) {
    m_numMissing = missing;
  }

  @Override
  public Attribute makeAttribute() {
    ArrayList<String> vals = new ArrayList<String>();

    for (Map.Entry<String, NominalStats.Count> e : m_counts.entrySet()) {
      vals.add(e.getKey() + "_" + e.getValue().m_count);
    }

    vals.add(MISSING_LABEL + "_" + m_numMissing);

    Attribute a =
      new Attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
        + m_attributeName, vals);

    return a;
  }
}
