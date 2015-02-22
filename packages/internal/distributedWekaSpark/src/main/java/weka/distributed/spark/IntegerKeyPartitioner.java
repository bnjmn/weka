package weka.distributed.spark;

import org.apache.spark.Partitioner;

/**
 * A simple partitioner that partitions according to the key
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * 
 */
public class IntegerKeyPartitioner extends Partitioner {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 570276804429461464L;

  protected final int m_numPartitions;

  public IntegerKeyPartitioner(final int numPartitions) {
    m_numPartitions = numPartitions;
  }

  @Override
  public int getPartition(Object key) {
    return ((Number) key).intValue();
  }

  @Override
  public int numPartitions() {
    return m_numPartitions;
  }
}
