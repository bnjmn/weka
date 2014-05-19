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
 *    Copyright (C) 2004
 *    & Matthias Schubert (schubert@dbs.ifi.lmu.de)
 *    & Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 *    & Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 */

package weka.clusterers.forOPTICSAndDBScan.DataObjects;

import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;

import java.io.Serializable;

/**
 * <p>
 * DataObject.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht, Matthias Schubert <br/>
 * Date: Aug 19, 2004 <br/>
 * Time: 5:50:22 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision$
 */
public class DataObject implements Serializable, RevisionHandler {

  public static final int UNCLASSIFIED = -1;
  public static final int NOISE = Integer.MIN_VALUE;
  public static final double UNDEFINED = Integer.MAX_VALUE;

    /** for serialization */
    private static final long serialVersionUID = -4408119914898291075L;

    /**
     * Holds the original instance
     */
    private Instance instance;

    /**
     * Holds the (unique) key that is associated with this DataObject
     */
    private String key;

    /**
     * Holds the ID of the cluster, to which this DataObject is assigned
     */
    private int clusterID;

    /**
     * Holds the status for this DataObject (true, if it has been processed, else false)
     */
    private boolean processed;

    /**
     * Holds the coreDistance for this DataObject
     */
    private double c_dist;

    /**
     * Holds the reachabilityDistance for this DataObject
     */
    private double r_dist;

    /**
     * Holds the database, that is the keeper of this DataObject
     */
    private Database database;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    /**
     * Constructs a new DataObject. The original instance is kept as instance-variable
     * @param originalInstance the original instance
     */
    public DataObject(Instance originalInstance, String key, Database database) {
        this.database = database;
        this.key = key;
        instance = originalInstance;
        clusterID = DataObject.UNCLASSIFIED;
        processed = false;
        c_dist = DataObject.UNDEFINED;
        r_dist = DataObject.UNDEFINED;
    }

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Compares two DataObjects in respect to their attribute-values
     * @param dataObject The DataObject, that is compared with this.dataObject;
     *        now assumed to be of the same type and with the same structure
     * @return Returns true, if the DataObjects correspond in each value, else returns false
     */
    public boolean equals(DataObject dataObject) {
      if (this == dataObject) return true;

      Instance firstInstance = getInstance();
      Instance secondInstance = dataObject.getInstance(); 
      int firstNumValues = firstInstance.numValues();
      int secondNumValues = secondInstance.numValues();
      int numAttributes = firstInstance.numAttributes();

      int firstI, secondI;
      for (int p1 = 0, p2 = 0; p1 < firstNumValues || p2 < secondNumValues;) {
        if (p1 >= firstNumValues) {
          firstI = numAttributes;
        } else {
          firstI = firstInstance.index(p1);
        }

        if (p2 >= secondNumValues) {
          secondI = numAttributes;
        } else {
          secondI = secondInstance.index(p2);
        }

        if (firstI == secondI) {
          if (firstInstance.valueSparse(p1) != secondInstance.valueSparse(p2)) {
            return false;
          }
          p1++;
          p2++;
        } else if (firstI > secondI) {
          if (0 != secondInstance.valueSparse(p2)) {
            return false;
          }
          p2++;
        } else {
          if (0 != firstInstance.valueSparse(p1)) {
            return false;
          }
          p1++;
        }
      }
      return true;
    }

    /**
     * Returns the original instance
     * @return originalInstance
     */
    public Instance getInstance() {
        return instance;
    }

    /**
     * Returns the key for this DataObject
     * @return key
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key for this DataObject
     * @param key The key is represented as string
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Sets the clusterID (cluster), to which this DataObject belongs to
     * @param clusterID Number of the Cluster
     */
    public void setClusterLabel(int clusterID) {
        this.clusterID = clusterID;
    }

    /**
     * Returns the clusterID, to which this DataObject belongs to
     * @return clusterID
     */
    public int getClusterLabel() {
        return clusterID;
    }

    /**
     * Marks this dataObject as processed
     * @param processed True, if the DataObject has been already processed, false else
     */
    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    /**
     * Gives information about the status of a dataObject
     * @return True, if this dataObject has been processed, else false
     */
    public boolean isProcessed() {
        return processed;
    }

    /**
     * Sets a new coreDistance for this dataObject
     * @param c_dist coreDistance
     */
    public void setCoreDistance(double c_dist) {
        this.c_dist = c_dist;
    }

    /**
     * Returns the coreDistance for this dataObject
     * @return coreDistance
     */
    public double getCoreDistance() {
        return c_dist;
    }

    /**
     * Sets a new reachability-distance for this dataObject
     */
    public void setReachabilityDistance(double r_dist) {
        this.r_dist = r_dist;
    }

    /**
     * Returns the reachabilityDistance for this dataObject
     */
    public double getReachabilityDistance() {
        return r_dist;
    }

    public String toString() {
        return instance.toString();
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision$");
    }
}
