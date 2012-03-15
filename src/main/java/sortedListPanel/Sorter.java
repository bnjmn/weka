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
 * Sorter.java
 *
 * Created on 5 fevrier 2003, 18:59
 */

/*
 *    Sorter.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */
package sortedListPanel;

import java.util.*;

/**
 *
 * @author  beleg
 */
public class Sorter {

	HashMap sorted;

	/** Creates a new Sorter
	 */
	public Sorter() {
		sorted = new HashMap();
	}

	/** Sorts the table of objects and stocks the result into its HashMap
	 */
	public void sort(Object[] theObjects, Comparator c) {
		if (!sorted.containsKey(c)) {
			System.out.println("Tri en cours.......");
			Arrays.sort(theObjects, c);
			Object[] objectsToStock = new Object[theObjects.length];
			for (int i = 0; i < objectsToStock.length; i++) {
				objectsToStock[i] = theObjects[i];
			}
			sorted.put(c, objectsToStock);
		} else {
			System.out.println("Deja trie.");
			Object[] objectsInStock = (Object[]) sorted.get(c);
			for (int i = 0; i < objectsInStock.length; i++)
				theObjects[i] = objectsInStock[i];
		}
	}
	
	public void clear() {
		sorted.clear();
	}

}
