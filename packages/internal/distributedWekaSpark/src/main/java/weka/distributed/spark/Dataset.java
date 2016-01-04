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
 *    Dataset
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import org.apache.spark.api.java.JavaRDD;
import weka.core.Instances;

/**
 * Class that encapsulates dataset information - i.e. an {@code RDD<Instance>} along
 * with ARFF header + summary attribute information
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class Dataset<T> extends AbstractDataset {

  private static final long serialVersionUID = -5152522334987920816L;

  /** The {@code RDD<T>} dataset */
  protected JavaRDD<T> m_dataset;

  /**
   * Constructor
   * 
   * @param dataset the {@code RDD<T>} dataset
   * @param headerWithSummary the header for the data with summary attributes
   */
  public Dataset(JavaRDD<T> dataset, Instances headerWithSummary) {
    super(headerWithSummary);
    m_dataset = dataset;
  }

  /**
   * Get the {@code RDD<T>} dataset
   * 
   * @return the dataset
   */
  public JavaRDD<T> getDataset() {
    return m_dataset;
  }

  /**
   * Get the header (with summary attributes) for the dataset
   *
   * @return the header (with summary attributes)
   */
  public Instances getHeaderWithSummary() {
    return m_headerWithSummary;
  }
}
