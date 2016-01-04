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
 *    AbstractDataset
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import weka.core.Instances;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class AbstractDataset implements Serializable {

  private static final long serialVersionUID = 1917759900257827086L;

  /** The key for an arbitrary dataset */
  public static final String DATASET = "dataset";

  /** The key for a training dataset */
  public static final String TRAINING_DATA = "trainingData";

  /** The key for a test dataset */
  public static final String TEST_DATA = "testData";

  /** The header with optional summary metadata for the dataset */
  protected Instances m_headerWithSummary;

  /** Any additional data */
  protected Map<String, Object> m_additionalData =
    new HashMap<String, Object>();

  /**
   * Constructor
   * 
   * @param headerWithSummary header for the dataset (optionally with summary
   *          metadata)
   */
  public AbstractDataset(Instances headerWithSummary) {
    m_headerWithSummary = headerWithSummary;
  }

  /**
   * Add an additional piece of data to carry along with this dataset
   *
   * @param key the key for the data
   * @param value the data
   */
  public void setAdditionalDataElement(String key, Object value) {
    m_additionalData.put(key, value);
  }

  /**
   * Get an additional piece of data
   *
   * @param key the key of the data to get
   * @return the data, or null if this dataset does not have the named
   *         additional data element
   */
  public Object getAdditionalDataElement(String key) {
    return m_additionalData.get(key);
  }
}
