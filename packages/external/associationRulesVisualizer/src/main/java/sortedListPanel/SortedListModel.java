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
 * SortedListModel.java
 *
 * Created on 31 janvier 2003, 22:06
 */

/*
 *    SortedListModel.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */
package sortedListPanel;

import java.util.*;


/**
 *
 * @author  beleg
 */
public class SortedListModel extends javax.swing.AbstractListModel {
    
    private Object[] objects;
    private ParameterComparatorFactory factory;
    private int size;
    private Sorter theSorter;
    
    /** Creates a new instance of SortedListModel */
    public SortedListModel(ParameterComparatorFactory cf, Sorter theSorter, Object[] objects) {
        this.objects = objects;
        this.size = objects.length;
	this.factory = cf;
	this.theSorter = theSorter;
    }
    
    /** Returns the value at the specified index.
     * @param index the requested index
     * @return the value at <code>index</code>
     *
     */
    public Object getElementAt(int index) {
        return objects[index];
    }
    
    /**
     * Returns the length of the list.
     * @return the length of the list
     *
     */
    public int getSize() {
        return size;
    }
    
    /**Set the contents of the ListModel and notifies its listeners.
     *@param objects the objects to put in the list
     */
    public void setElements(Object[] objects) {
        this.objects = objects;
        this.fireContentsChanged(this, 0, size - 1);
        this.size = objects.length;
    }

    /**Sort the list
     *@param criteres the list of criteria whith which the list will be sorted.
     */
    public void sort(LinkedList criteres) {
	Iterator it = criteres.listIterator();
	ComposedComparator cc = new ComposedComparator();
	while(it.hasNext()) {
	    String critere = (String) it.next();
	    System.out.println(critere);
	    ParameterComparator comp = factory.createParameterComparator(critere);
	    cc.add(comp);
	}
	theSorter.sort(objects,cc);
	this.fireContentsChanged(this, 0, size - 1);
    }
}
