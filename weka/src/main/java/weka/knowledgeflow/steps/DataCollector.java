package weka.knowledgeflow.steps;

import weka.core.WekaException;

/**
 * Auxiliary interface for steps that collect data results of some type - e.g.
 * visualization steps that collect results.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface DataCollector {

  /**
   * Get the data that this collector has collected
   *
   * @return the data collected by this collector
   */
  public Object retrieveData();

  /**
   * Set the data for this collector
   *
   * @param data the data to set
   * @throws WekaException if there is a problem restoring data
   */
  public void restoreData(Object data) throws WekaException;
}
