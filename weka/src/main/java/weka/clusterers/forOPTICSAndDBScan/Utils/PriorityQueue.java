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

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

import java.util.ArrayList;

/**
 * <p>
 * PriorityQueue.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht, Matthias Schubert <br/>
 * Date: Aug 27, 2004 <br/>
 * Time: 5:36:35 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 1.3 $
 */
public class PriorityQueue
    implements RevisionHandler {

    /**
     * Used to store the binary heap
     */
    private ArrayList queue;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    /**
     * Creates a new PriorityQueue backed on a binary heap. The queue is
     * dynamically growing and shrinking and it is descending, that is: the highest
     * priority is always in the root.
     */
    public PriorityQueue() {
        queue = new ArrayList();
    }

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Adds a new Object to the queue
     * @param priority The priority associated with the object
     * @param o
     */
    public void add(double priority, Object o) {
        queue.add(new PriorityQueueElement(priority, o));
        heapValueUpwards();
    }

    /**
     * Returns the priority for the object at the specified index
     * @param index the index of the object
     * @return priority
     */
    public double getPriority(int index) {
        return ((PriorityQueueElement) queue.get(index)).getPriority();
    }

    /**
     * Restores the heap after inserting a new object
     */
    private void heapValueUpwards() {
        int a = size();
        int c = a / 2;

        PriorityQueueElement recentlyInsertedElement = (PriorityQueueElement) queue.get(a - 1);

        while (c > 0 && getPriority(c - 1) < recentlyInsertedElement.getPriority()) {
            queue.set(a - 1, queue.get(c - 1));       //shift parent-node down
            a = c;                                    //(c <= 0) => no parent-node remains
            c = a / 2;
        }
        queue.set(a - 1, recentlyInsertedElement);
    }

    /**
     * Restores the heap after removing the next element
     */
    private void heapValueDownwards() {
        int a = 1;
        int c = 2 * a;           //descendant

        PriorityQueueElement priorityQueueElement = (PriorityQueueElement) queue.get(a - 1);

        if (c < size() && (getPriority(c) > getPriority(c - 1))) c++;

        while (c <= size() && getPriority(c - 1) > priorityQueueElement.getPriority()) {
            queue.set(a - 1, queue.get(c - 1));
            a = c;
            c = 2 * a;
            if (c < size() && (getPriority(c) > getPriority(c - 1))) c++;
        }
        queue.set(a - 1, priorityQueueElement);
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
        return !(size() == 0);
    }

    /**
     * Returns the element with the highest priority
     * @return next element
     */
    public PriorityQueueElement next() {
        PriorityQueueElement next = (PriorityQueueElement) queue.get(0);
        queue.set(0, queue.get(size() - 1));
        queue.remove(size() - 1);
        if (hasNext()) {
            heapValueDownwards();
        }
        return next;
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.3 $");
    }
}
