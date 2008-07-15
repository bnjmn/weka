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
import weka.clusterers.forOPTICSAndDBScan.Utils.EpsilonRange_ListElement;
import weka.clusterers.forOPTICSAndDBScan.Utils.PriorityQueue;
import weka.clusterers.forOPTICSAndDBScan.Utils.PriorityQueueElement;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * <p>
 * SequentialDatabase.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht, Matthias Schubert <br/>
 * Date: Aug 20, 2004 <br/>
 * Time: 1:23:38 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 1.4 $
 */
public class SequentialDatabase
    implements Database, Serializable, RevisionHandler {

    /** for serialization */
    private static final long serialVersionUID = 787245523118665778L;

    /**
     * Internal, sorted Treemap for storing all the DataObjects
     */
    private TreeMap treeMap;

    /**
     * Holds the original instances delivered from WEKA
     */
    private Instances instances;

    /**
     * Holds the minimum value for each attribute
     */
    private double[] attributeMinValues;

    /**
     * Holds the maximum value for each attribute
     */
    private double[] attributeMaxValues;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    /**
     * Constructs a new sequential database and holds the original instances
     * @param instances
     */
    public SequentialDatabase(Instances instances) {
        this.instances = instances;
        treeMap = new TreeMap();
    }

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Select a dataObject from the database
     * @param key The key that is associated with the dataObject
     * @return dataObject
     */
    public DataObject getDataObject(String key) {
        return (DataObject) treeMap.get(key);
    }

    /**
     * Sets the minimum and maximum values for each attribute in different arrays
     * by walking through every DataObject of the database
     */
    public void setMinMaxValues() {
        attributeMinValues = new double[getInstances().numAttributes()];
        attributeMaxValues = new double[getInstances().numAttributes()];

        //Init
        for (int i = 0; i < getInstances().numAttributes(); i++) {
            attributeMinValues[i] = attributeMaxValues[i] = Double.NaN;
        }

        Iterator iterator = dataObjectIterator();
        while (iterator.hasNext()) {
            DataObject dataObject = (DataObject) iterator.next();
            for (int j = 0; j < getInstances().numAttributes(); j++) {
                if (Double.isNaN(attributeMinValues[j])) {
                    attributeMinValues[j] = dataObject.getInstance().value(j);
                    attributeMaxValues[j] = dataObject.getInstance().value(j);
                } else {
                    if (dataObject.getInstance().value(j) < attributeMinValues[j])
                        attributeMinValues[j] = dataObject.getInstance().value(j);
                    if (dataObject.getInstance().value(j) > attributeMaxValues[j])
                        attributeMaxValues[j] = dataObject.getInstance().value(j);
                }
            }
        }
    }

    /**
     * Returns the array of minimum-values for each attribute
     * @return attributeMinValues
     */
    public double[] getAttributeMinValues() {
        return attributeMinValues;
    }

    /**
     * Returns the array of maximum-values for each attribute
     * @return attributeMaxValues
     */
    public double[] getAttributeMaxValues() {
        return attributeMaxValues;
    }

    /**
     * Performs an epsilon range query for this dataObject
     * @param epsilon Specifies the range for the query
     * @param queryDataObject The dataObject that is used as query-object for epsilon range query
     * @return List with all the DataObjects that are within the specified range
     */
    public List epsilonRangeQuery(double epsilon, DataObject queryDataObject) {
        ArrayList epsilonRange_List = new ArrayList();
        Iterator iterator = dataObjectIterator();
        while (iterator.hasNext()) {
            DataObject dataObject = (DataObject) iterator.next();
            double distance = queryDataObject.distance(dataObject);
            if (distance < epsilon) {
                epsilonRange_List.add(dataObject);
            }
        }

        return epsilonRange_List;
    }

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
    public List k_nextNeighbourQuery(int k, double epsilon, DataObject dataObject) {
        Iterator iterator = dataObjectIterator();

        List return_List = new ArrayList();
        List nextNeighbours_List = new ArrayList();
        List epsilonRange_List = new ArrayList();

        PriorityQueue priorityQueue = new PriorityQueue();

        while (iterator.hasNext()) {
            DataObject next_dataObject = (DataObject) iterator.next();
            double dist = dataObject.distance(next_dataObject);

            if (dist <= epsilon) epsilonRange_List.add(new EpsilonRange_ListElement(dist, next_dataObject));

            if (priorityQueue.size() < k) {
                priorityQueue.add(dist, next_dataObject);
            } else {
                if (dist < priorityQueue.getPriority(0)) {
                    priorityQueue.next(); //removes the highest distance
                    priorityQueue.add(dist, next_dataObject);
                }
            }
        }

        while (priorityQueue.hasNext()) {
            nextNeighbours_List.add(0, priorityQueue.next());
        }

        return_List.add(nextNeighbours_List);
        return_List.add(epsilonRange_List);
        return return_List;
    }

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
    public List coreDistance(int minPoints, double epsilon, DataObject dataObject) {
        List list = k_nextNeighbourQuery(minPoints, epsilon, dataObject);

        if (((List) list.get(1)).size() < minPoints) {
            list.add(new Double(DataObject.UNDEFINED));
            return list;
        } else {
            List nextNeighbours_List = (List) list.get(0);
            PriorityQueueElement priorityQueueElement =
                    (PriorityQueueElement) nextNeighbours_List.get(nextNeighbours_List.size() - 1);
            if (priorityQueueElement.getPriority() <= epsilon) {
                list.add(new Double(priorityQueueElement.getPriority()));
                return list;
            } else {
                list.add(new Double(DataObject.UNDEFINED));
                return list;
            }
        }
    }

    /**
     * Returns the size of the database (the number of dataObjects in the database)
     * @return size
     */
    public int size() {
        return treeMap.size();
    }

    /**
     * Returns an iterator over all the keys
     * @return iterator
     */
    public Iterator keyIterator() {
        return treeMap.keySet().iterator();
    }

    /**
     * Returns an iterator over all the dataObjects in the database
     * @return iterator
     */
    public Iterator dataObjectIterator() {
        return treeMap.values().iterator();
    }

    /**
     * Tests if the database contains the dataObject_Query
     * @param dataObject_Query The query-object
     * @return true if the database contains dataObject_Query, else false
     */
    public boolean contains(DataObject dataObject_Query) {
        Iterator iterator = dataObjectIterator();
        while (iterator.hasNext()) {
            DataObject dataObject = (DataObject) iterator.next();
            if (dataObject.equals(dataObject_Query)) return true;
        }
        return false;
    }

    /**
     * Inserts a new dataObject into the database
     * @param dataObject
     */
    public void insert(DataObject dataObject) {
        treeMap.put(dataObject.getKey(), dataObject);
    }

    /**
     * Returns the original instances delivered from WEKA
     * @return instances
     */
    public Instances getInstances() {
        return instances;
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.4 $");
    }
}
