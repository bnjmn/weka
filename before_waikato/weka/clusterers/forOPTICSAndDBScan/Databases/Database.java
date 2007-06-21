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

package weka.clusterers.forOPTICSAndDBScan.Databases;

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.core.Instances;

import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Database.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht, Matthias Schubert <br/>
 * Date: Aug 20, 2004 <br/>
 * Time: 1:03:43 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 1.2 $
 */
public interface Database {

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Select a dataObject from the database
     * @param key The key that is associated with the dataObject
     * @return dataObject
     */
    DataObject getDataObject(String key);

    /**
     * Returns the size of the database (the number of dataObjects in the database)
     * @return size
     */
    int size();

    /**
     * Returns an iterator over all the keys
     * @return iterator
     */
    Iterator keyIterator();

    /**
     * Returns an iterator over all the dataObjects in the database
     * @return iterator
     */
    Iterator dataObjectIterator();

    /**
     * Tests if the database contains the dataObject_Query
     * @param dataObject_Query The query-object
     * @return true if the database contains dataObject_Query, else false
     */
    boolean contains(DataObject dataObject_Query);

    /**
     * Inserts a new dataObject into the database
     * @param dataObject
     */
    void insert(DataObject dataObject);

    /**
     * Returns the original instances delivered from WEKA
     * @return instances
     */
    Instances getInstances();

    /**
     * Sets the minimum and maximum values for each attribute in different arrays
     * by walking through every DataObject of the database
     */
    void setMinMaxValues();

    /**
     * Returns the array of minimum-values for each attribute
     * @return attributeMinValues
     */
    double[] getAttributeMinValues();

    /**
     * Returns the array of maximum-values for each attribute
     * @return attributeMaxValues
     */
    double[] getAttributeMaxValues();

    /**
     * Performs an epsilon range query for this dataObject
     * @param epsilon Specifies the range for the query
     * @param queryDataObject The dataObject that is used as query-object for epsilon range query
     * @return List with all the DataObjects that are within the specified range
     */
    List epsilonRangeQuery(double epsilon, DataObject queryDataObject);

    /**
     * Emits the k next-neighbours and performs an epsilon-range-query at the parallel.
     * The returned list contains two elements:
     * At index=0 --> list with all k next-neighbours;
     * At index=1 --> list with all dataObjects within epsilon;
     * @param k number of next neighbours
     * @param epsilon Specifies the range for the query
     * @param dataObject the start object
     * @return list with the k-next neighbours (PriorityQueueElements) and a list
     *         with candidates from the epsilon-range-query (EpsilonRange_ListElements)
     */
    List k_nextNeighbourQuery(int k, double epsilon, DataObject dataObject);

    /**
     * Calculates the coreDistance for the specified DataObject.
     * The returned list contains three elements:
     * At index=0 --> list with all k next-neighbours;
     * At index=1 --> list with all dataObjects within epsilon;
     * At index=2 --> coreDistance as Double-value
     * @param minPoints minPoints-many neighbours within epsilon must be found to have a non-undefined coreDistance
     * @param epsilon Specifies the range for the query
     * @param dataObject Calculate coreDistance for this dataObject
     * @return list with the k-next neighbours (PriorityQueueElements) and a list
     *         with candidates from the epsilon-range-query (EpsilonRange_ListElements) and
     *         the double-value for the calculated coreDistance
     */
    List coreDistance(int minPoints, double epsilon, DataObject dataObject);

}
