/*
 * ComposedComparator.java
 *
 * Created on 5 fevrier 2003, 18:49
 */

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
 *    ComposedComparator.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */
package sortedListPanel;

import java.util.*;

/**
 *
 * @author  beleg
 */
public class ComposedComparator implements java.util.Comparator {
    
    private LinkedList theComparators;

    public ComposedComparator() {
	theComparators = new LinkedList();
    }
    
    public void add(Comparator c) {
	theComparators.add(c);
    }
    /** Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.<p>
     * Note: this comparator
     * imposes orderings that are inconsistent with equals.
     *
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return a negative integer, zero, or a positive integer as the
     * 	       first argument is less than, equal to, or greater than the
     * 	       second.
     * @throws ClassCastException if the arguments' types prevent them from
     * 	       being compared by this Comparator.
     *
     */
    public int compare(Object o1, Object o2) {
	int res = 0;
	Iterator it = theComparators.listIterator();
	while(it.hasNext()) {
	    Comparator c = (Comparator) it.next();
	    res = c.compare(o1,o2);
	    if (res != 0)
		return res;
	}
	return res;
    }

    /** returns if this object is equal to another object
     */
    public boolean equals(Object o) {
	ComposedComparator c = null;
	try {
	    c = (ComposedComparator) o;
	}
	catch(ClassCastException e) {
	    return false;
	}
	Iterator it1 = this.theComparators.listIterator();
	Iterator it2 = c.theComparators.listIterator();
	while(it1.hasNext()) {
	    Comparator c1 = (Comparator) it1.next();
	    Comparator c2 = null;
	    if (it2.hasNext())
		c2 = (Comparator) it2.next();
	    else
		return false;
	    if (!c1.equals(c2))
		return false;
	}
	if (it2.hasNext())
	    return false;
	return true;

    }

    public int hashCode() {
	return theComparators.hashCode();
    }
    
}
