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
 * SorterComboBoxModel.java
 *
 * Created on 13 fevrier 2003, 12:28
 */

/*
 *    SorterComboBoxModel.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */
package sortedListPanel;




/**
 *
 * @author  beleg
 */
public class SorterComboBoxModel extends javax.swing.DefaultComboBoxModel {
    
    /** Creates a new instance of SortedListModel */
    public SorterComboBoxModel(Object[] objects) {
	super(objects);
    }
    
    /**Set the contents of the ListModel and notifies its listeners.
     *@param objects the objects to put in the list
     */
    public void setElements(Object[] objects) {
	this.removeAllElements();
	for (int i = 0; i < objects.length; i++)
	    this.addElement(objects[i]);
    }

}
