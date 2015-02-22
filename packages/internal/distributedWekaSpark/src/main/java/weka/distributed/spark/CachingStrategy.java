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
 *    CachingStrategy
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import java.io.IOException;
import java.io.Serializable;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.storage.StorageLevel;

import weka.distributed.DistributedWekaException;

/**
 * Implements a caching strategy based on available cluster memory and size of
 * input on disk. Based on the strategy outlined in Aris Koliopoulos' masters
 * thesis.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CachingStrategy implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -6938733961276489866L;

  /** True if serialized RDD objects are to be compressed */
  protected boolean m_compress;

  /** The StorageLevel to use */
  protected StorageLevel m_storageLevel = StorageLevel.DISK_ONLY();

  /**
   * Constructor
   * 
   * @param inputPath the path to the input file/directory
   * @param totalClusterMem the total available cluster memory (in Gb)
   * @param objectOverhead overhead factor (as a multiple of on-disk size) for
   *          data size in object form in memory
   * @param context the context to adjust if necessary (can be null).
   * @throws DistributedWekaException if a problem occurs
   */
  public CachingStrategy(String inputPath, double totalClusterMem,
    double objectOverhead, JavaSparkContext context)
    throws DistributedWekaException {

    try {
      if (totalClusterMem <= 0) {
        m_storageLevel = StorageLevel.MEMORY_AND_DISK();

        return;
      }

      long inputSizeOnDisk = SparkJob.getSizeInBytesOfPath(inputPath);

      System.err.println("Cluster memory: " + totalClusterMem
        + "Gb Input size: "
        + (inputSizeOnDisk / (1024 * 1024 * 1024)) + "Gb");
      totalClusterMem *= 1024 * 1024 * 1024; // bytes
      if (totalClusterMem > inputSizeOnDisk * objectOverhead) {
        m_storageLevel = StorageLevel.MEMORY_AND_DISK();
      } else if (totalClusterMem > inputSizeOnDisk) {
        m_storageLevel = StorageLevel.MEMORY_AND_DISK_SER();
      } else if (totalClusterMem > 0.5 * inputSizeOnDisk) {
        m_storageLevel = StorageLevel.MEMORY_AND_DISK_SER();
        m_compress = true;
        context.getConf().set("spark.rdd.compress", "true");
      } // otherwise keep data on disk only

    } catch (IOException ex) {
      throw new DistributedWekaException(ex);
    }
  }

  /**
   * Get the determined storage level
   * 
   * @return the storage level
   */
  public StorageLevel getStorageLevel() {
    return m_storageLevel;
  }

  /**
   * Get whether compression should be used
   * 
   * @return true if compression should be used
   */
  public boolean getUseCompression() {
    return m_compress;
  }
}
