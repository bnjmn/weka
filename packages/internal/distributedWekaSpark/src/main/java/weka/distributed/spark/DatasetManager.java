package weka.distributed.spark;

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
  protected transient Map<String, Dataset<?>> m_datasets =
    new HashMap<String, Dataset<?>>();

  /**
   * Set a batch dataset for this job to potentially make use of
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
  public Dataset<?> getDataset(String key) {
    return m_datasets.get(key);
  }

  /**
   * Return an iterator over the named batch datasets for this job
   *
   * @return an iterator over the datasets for this job
   */
  public Iterator<Map.Entry<String, Dataset<?>>> getDatasets() {
    return m_datasets.entrySet().iterator();
  }
}
