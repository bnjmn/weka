/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    Copyright (C) 2004
 *    & Matthias Schubert (schubert@dbs.ifi.lmu.de)
 *    & Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 *    & Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 */

package weka.clusterers.forOPTICSAndDBScan.DataObjects;

import weka.core.Instance;

/**
 * <p>
 * DataObject.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht, Matthias Schubert <br/>
 * Date: Aug 19, 2004 <br/>
 * Time: 5:48:59 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 1.2 $
 */
public interface DataObject {

    static final int UNCLASSIFIED = -1;
    static final int NOISE = Integer.MIN_VALUE;
    static final double UNDEFINED = Integer.MAX_VALUE;

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Compares two DataObjects in respect to their attribute-values
     * @param dataObject The DataObject, that is compared with this.dataObject
     * @return Returns true, if the DataObjects correspond in each value, else returns false
     */
    boolean equals(DataObject dataObject);

    /**
     * Calculates the distance between dataObject and this.dataObject
     * @param dataObject The DataObject, that is used for distance-calculation with this.dataObject
     * @return double-value The distance between dataObject and this.dataObject
     */
    double distance(DataObject dataObject);

    /**
     * Returns the original instance
     * @return originalInstance
     */
    Instance getInstance();

    /**
     * Returns the key for this DataObject
     * @return key
     */
    String getKey();

    /**
     * Sets the key for this DataObject
     * @param key The key is represented as string
     */
    void setKey(String key);

    /**
     * Sets the clusterID (cluster), to which this DataObject belongs to
     * @param clusterID Number of the Cluster
     */
    void setClusterLabel(int clusterID);

    /**
     * Returns the clusterID, to which this DataObject belongs to
     * @return clusterID
     */
    int getClusterLabel();

    /**
     * Marks this dataObject as processed
     * @param processed True, if the DataObject has been already processed, false else
     */
    void setProcessed(boolean processed);

    /**
     * Gives information about the status of a dataObject
     * @return True, if this dataObject has been processed, else false
     */
    boolean isProcessed();

    /**
     * Sets a new coreDistance for this dataObject
     * @param c_dist coreDistance
     */
    void setCoreDistance(double c_dist);

    /**
     * Returns the coreDistance for this dataObject
     * @return coreDistance
     */
    double getCoreDistance();

    /**
     * Sets a new reachability-distance for this dataObject
     */
    void setReachabilityDistance(double r_dist);

    /**
     * Returns the reachabilityDistance for this dataObject
     */
    double getReachabilityDistance();
}
