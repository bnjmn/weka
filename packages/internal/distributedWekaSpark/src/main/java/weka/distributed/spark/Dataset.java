package weka.distributed.spark;

import org.apache.spark.api.java.JavaRDD;

import weka.core.Instance;
import weka.core.Instances;

public class Dataset {

  /** The RDD<Instance> dataset */
  protected JavaRDD<Instance> m_dataset;

  /** The header plus summary metadata for the dataset */
  protected Instances m_headerWithSummary;

  /**
   * Constructor
   * 
   * @param dataset the RDD<Instance> dataset
   * @param headerWithSummary the header for the data with summary attributes
   */
  public Dataset(JavaRDD<Instance> dataset, Instances headerWithSummary) {
    m_dataset = dataset;
    m_headerWithSummary = headerWithSummary;
  }

  /**
   * Get the RDD<Instance> dataset
   * 
   * @return the dataset
   */
  public JavaRDD<Instance> getDataset() {
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
