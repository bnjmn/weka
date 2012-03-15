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
 *    MultipleListSelectionEvent.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package selection;

import javax.swing.event.ListSelectionEvent;

/**
 * @author beleg
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class MultipleListSelectionEvent extends ListSelectionEvent {
	
	private int listIndex;
	boolean isEmptySelection;
	
	/**
	 * creates a new MultipleSelectionEvent
	 * @param source the source object of this event
	 * @param listIndex the index of the list which send a ListSelectionEvent
	 * @param isEmptySelection says if the new selection is empty or not
	 * @param firstIndex the first index of the interval changed
	 * @param lastIndex the last index of the interval changed
	 * @param isAdjusting
	 */
	public MultipleListSelectionEvent(Object source, int listIndex, boolean isEmptySelection, int firstIndex, int lastIndex,boolean isAdjusting) {
		super(source, firstIndex, lastIndex, isAdjusting);
		this.listIndex = listIndex;
		this.isEmptySelection = isEmptySelection;	
	}
	
	/**
	 * return the index of the list which send a ListSelectionEvent
	 * @return the index of the list
	 */
	public int getListIndex() {
		return listIndex;
	}
	
	
	/**
	 * return if the new selection is empty or not
	 * @return true if the selection is empty, false otherwise
	 */
	public boolean isEmptySelection() {
		return isEmptySelection;	
	}

}
