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
 *    DatasetManager
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages datasets
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class DatasetManager {

  /** Holds batch dataset(s) in play */
  protected transient Map<String, Dataset> m_datasets =
    new HashMap<String, Dataset>();

  /**
   * Set a batch dataset
   *
   * @param key the name of the dataset
   * @param dataset the dataset itself
   */
  public void setDataset(String key, Dataset dataset) {
    m_datasets.put(key, dataset);
  }

  /**
   * Return a named batch dataset, or null if the name is unknown.
   *
   * @param key the name of the dataset to get
   * @return the named dataset or null
   */
  public Dataset getDataset(String key) {
    return m_datasets.get(key);
  }

  /**
   * Return an iterator over the named batch datasets.
   *
   * @return an iterator over the datasets for this job
   */
  public Iterator<Map.Entry<String, Dataset>> getDatasetIterator() {
    return m_datasets.entrySet().iterator();
  }

  /**
   * Get a collection of the datasets
   *
   * @return a collection of the datasets
   */
  public Collection<Dataset> getDatasets() {
    return m_datasets.values();
  }

  public void addAll(Iterator<Map.Entry<String, Dataset>> toAdd) {
    while (toAdd.hasNext()) {
      Map.Entry<String, Dataset> entry = toAdd.next();
      m_datasets.put(entry.getKey(), entry.getValue());
    }
  }
}
