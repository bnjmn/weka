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
 * ArffTable.java
 * Copyright (C) 2005 FracPete
 *
 */

package weka.gui.arffviewer;

import weka.gui.ComponentHelper;
import weka.gui.JTableHelper;
import weka.core.Attribute;
import java.awt.datatransfer.StringSelection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A specialized JTable for the Arff-Viewer.
 *
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2.2.2 $ 
 */

public class ArffTable extends JTable {
  // the search string
  private String                   searchString;
  // the listeners for changes
  private HashSet                  changeListeners;
  
  /**
   * initializes with no model
   */
  public ArffTable() {
    this(new ArffTableSorter(""));
  }
  
  /**
   * initializes with the given model
   */
  public ArffTable(TableModel model) {
    super(model);
    
    setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
  }
  
  /**
   * sets the new model
   */
  public void setModel(TableModel model) {
    ArffTableSorter      arffModel;
    
    // initialize the search
    searchString = null;
    
    // init the listeners
    if (changeListeners == null)
      changeListeners = new HashSet();
    
    super.setModel(model);
    
    if (model == null)
      return;
    
    if (!(model instanceof ArffTableSorter))
      return;
    
    arffModel = (ArffTableSorter) model;
    arffModel.addMouseListenerToHeaderInTable(this);
    arffModel.addTableModelListener(this);
    arffModel.sortByColumn(0);
    setLayout();
    setSelectedColumn(0);
  }
  
  /**
   * sets the cell renderer and calcs the optimal column width
   */
  private void setLayout() {
    ArffTableSorter      arffModel;
    int                  i;
    JComboBox            combo;
    Enumeration          enm;
    
    arffModel = (ArffTableSorter) getModel();
    
    for (i = 0; i < getColumnCount(); i++) {
      // optimal colwidths (only according to header!)
      JTableHelper.setOptimalHeaderWidth(this, i);
      
      // CellRenderer
      getColumnModel().getColumn(i).setCellRenderer(
          new ArffTableCellRenderer());
      
      // CellEditor
      if (i > 0) {
        if (arffModel.getType(i) == Attribute.NOMINAL) {
          combo = new JComboBox();
          combo.addItem(null);
          enm  = arffModel.getInstances().attribute(i - 1).enumerateValues();
          while (enm.hasMoreElements())
            combo.addItem(enm.nextElement());
          getColumnModel().getColumn(i).setCellEditor(new DefaultCellEditor(combo));
        }
        else {
          getColumnModel().getColumn(i).setCellEditor(null);
        }
      }
    }
  }
  
  /**
   * returns the basically the attribute name of the column and not the
   * HTML column name via getColumnName(int)
   */
  public String getPlainColumnName(int columnIndex) {
    ArffTableSorter      arffModel;
    String               result;
    
    result = "";
    
    if (getModel() == null)
      return result;
    if (!(getModel() instanceof ArffTableSorter))  
      return result;
    
    arffModel = (ArffTableSorter) getModel();
    
    if ( (columnIndex >= 0) && (columnIndex < getColumnCount()) ) {
      if (columnIndex == 0)
        result = "No.";
      else
        result = arffModel.getAttributeAt(columnIndex).name();
    }
    
    return result;
  }
  
  /**
   * returns the selected content in a StringSelection that can be copied to
   * the clipboard and used in Excel, if nothing is selected the whole table
   * is copied to the clipboard
   */
  public StringSelection getStringSelection() {
    StringSelection         result;
    int[]                   indices;
    int                     i;
    int                     n;
    StringBuffer            tmp;
    
    result = null;
    
    // nothing selected? -> all
    if (getSelectedRow() == -1) {
      // really?
      if (ComponentHelper.showMessageBox(
            getParent(),
            "Question...",
            "Do you really want to copy the whole table?",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE ) != JOptionPane.YES_OPTION)
        return result;
      
      indices = new int[getRowCount()];
      for (i = 0; i < indices.length; i++)
        indices[i] = i;
    }
    else {
      indices = getSelectedRows();
    }
    
    // get header
    tmp = new StringBuffer();
    for (i = 0; i < getColumnCount(); i++) {
      if (i > 0)
        tmp.append("\t");
      tmp.append(getPlainColumnName(i));
    }
    tmp.append("\n");
    
    // get content
    for (i = 0; i < indices.length; i++) {
      for (n = 0; n < getColumnCount(); n++) {
        if (n > 0)
          tmp.append("\t");
        tmp.append(getValueAt(indices[i], n).toString());
      }
      tmp.append("\n");
    }
    
    result = new StringSelection(tmp.toString());
    
    return result;
  }
  
  /**
   * sets the search string to look for in the table, NULL or "" disables
   * the search
   */
  public void setSearchString(String searchString) {
    this.searchString = searchString;
    repaint();
  }
  
  /**
   * returns the search string, can be NULL if no search string is set
   */
  public String getSearchString() {
    return searchString;
  }
  
  /**
   * sets the selected column
   */
  public void setSelectedColumn(int index) {
    getColumnModel().getSelectionModel().clearSelection();
    getColumnModel().getSelectionModel().setSelectionInterval(index, index);
    resizeAndRepaint();
    if (getTableHeader() != null)
      getTableHeader().resizeAndRepaint();
  }
  
  /**
   * This fine grain notification tells listeners the exact range of cells, 
   * rows, or columns that changed.
   */
  public void tableChanged(TableModelEvent e) {
    super.tableChanged(e);
    
    setLayout();
    notifyListener();
  }
  
  /**
   * notfies all listener of the change
   */
  private void notifyListener() {
    Iterator                iter;
    
    iter = changeListeners.iterator();
    while (iter.hasNext())
      ((ChangeListener) iter.next()).stateChanged(new ChangeEvent(this));
  }
  
  /**
   * Adds a ChangeListener to the panel
   */
  public void addChangeListener(ChangeListener l) {
    changeListeners.add(l);
  }
  
  /**
   * Removes a ChangeListener from the panel
   */
  public void removeChangeListener(ChangeListener l) {
    changeListeners.remove(l);
  }
}
