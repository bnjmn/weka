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
 *    IntegerKeyPartitioner
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import org.apache.spark.Partitioner;

/**
 * A simple partitioner that partitions according to the key
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 11605 $
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
