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

package weka.clusterers.forOPTICSAndDBScan.Utils;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * <p>
 * UpdateQueue.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht, Matthias Schubert <br/>
 * Date: Aug 27, 2004 <br/>
 * Time: 5:36:35 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 1.2 $
 */
public class UpdateQueue {

    /**
     * Used to store the binary heap
     */
    private ArrayList queue;

    /**
     * Used to get efficient access to the stored Objects
     */
    private TreeMap objectPositionsInHeap;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    /**
     * Creates a new PriorityQueue (backed on a binary heap) with the ability to efficiently
     * update the priority of the stored objects in the heap. The ascending (!) queue is
     * dynamically growing and shrinking.
     */
    public UpdateQueue() {
        queue = new ArrayList();
        objectPositionsInHeap = new TreeMap();
    }

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Adds a new Object to the queue
     * @param priority The priority associated with the object (in this case: the reachability-distance)
     * @param objectKey The key for this object
     * @param o
     */
    public void add(double priority, Object o, String objectKey) {
        int objectPosition = 0;

        if (objectPositionsInHeap.containsKey(objectKey)) {
            objectPosition = ((Integer) objectPositionsInHeap.get(objectKey)).intValue();
            if (((UpdateQueueElement) queue.get(objectPosition)).getPriority() <= priority) return;
            queue.set(objectPosition++, new UpdateQueueElement(priority, o, objectKey));
        } else {
            queue.add(new UpdateQueueElement(priority, o, objectKey));
            objectPosition = size();
        }
        heapValueUpwards(objectPosition);
    }

    /**
     * Returns the priority for the object at the specified index
     * @param index the index of the object
     * @return priority
     */
    public double getPriority(int index) {
        return ((UpdateQueueElement) queue.get(index)).getPriority();
    }

    /**
     * Restores the heap after inserting a new object
     */
    private void heapValueUpwards(int pos) {
        int a = pos;
        int c = a / 2;

        UpdateQueueElement recentlyInsertedElement = (UpdateQueueElement) queue.get(a - 1);

        /** ascending order! */
        while (c > 0 && getPriority(c - 1) > recentlyInsertedElement.getPriority()) {
            queue.set(a - 1, queue.get(c - 1));       //shift parent-node down
            objectPositionsInHeap.put(((UpdateQueueElement) queue.get(a - 1)).getObjectKey(), new Integer(a - 1));
            a = c;                                    //(c <= 0) => no parent-node remains
            c = a / 2;
        }
        queue.set(a - 1, recentlyInsertedElement);
        objectPositionsInHeap.put(((UpdateQueueElement) queue.get(a - 1)).getObjectKey(), new Integer(a - 1));
    }

    /**
     * Restores the heap after removing the next element
     */
    private void heapValueDownwards() {
        int a = 1;
        int c = 2 * a;           //descendant

        UpdateQueueElement updateQueueElement = (UpdateQueueElement) queue.get(a - 1);

        if (c < size() && (getPriority(c) < getPriority(c - 1))) c++;

        while (c <= size() && getPriority(c - 1) < updateQueueElement.getPriority()) {
            queue.set(a - 1, queue.get(c - 1));
            objectPositionsInHeap.put(((UpdateQueueElement) queue.get(a - 1)).getObjectKey(), new Integer(a - 1));
            a = c;
            c = 2 * a;
            if (c < size() && (getPriority(c) < getPriority(c - 1))) c++;
        }
        queue.set(a - 1, updateQueueElement);
        objectPositionsInHeap.put(((UpdateQueueElement) queue.get(a - 1)).getObjectKey(), new Integer(a - 1));
    }

    /**
     * Returns the queue's size
     * @return size
     */
    public int size() {
        return queue.size();
    }

    /**
     * Tests, if the queue has some more elements left
     * @return true, if there are any elements left, else false
     */
    public boolean hasNext() {
        return !(queue.size() == 0);
    }

    /**
     * Returns the element with the lowest priority
     * @return next element
     */
    public UpdateQueueElement next() {
        UpdateQueueElement next = (UpdateQueueElement) queue.get(0);
        queue.set(0, queue.get(size() - 1));
        queue.remove(size() - 1);
        objectPositionsInHeap.remove(next.getObjectKey());
        if (hasNext()) {
            heapValueDownwards();
        }
        return next;
    }

    // *****************************************************************************************************************
    // inner classes
    // *****************************************************************************************************************

}
