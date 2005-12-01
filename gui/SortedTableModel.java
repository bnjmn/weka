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
 * SortedTableModel.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import weka.core.ClassDiscovery;

import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;

/**
 * Represents a TableModel with sorting functionality.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 */

public class SortedTableModel
  extends AbstractTableModel
  implements TableModelListener {

  /** the actual table model */
  protected TableModel mModel;

  /** the mapping between displayed and actual index */
  protected int[] mIndices;

  /** the sort column */
  protected int mSortColumn;

  /** whether sorting is ascending or descending */
  protected boolean mAscending;
  
  /**
   * initializes with no model
   */
  public SortedTableModel() {
    this(null);
  }

  /**
   * initializes with the given model
   *
   * @param model       the model to initialize the sorted model with
   */
  public SortedTableModel(TableModel model) {
    setModel(model);
  }

  /**
   * sets the model to use
   *
   * @param value       the model to use
   */
  public void setModel(TableModel value) {
    mModel = value;

    // initialize indices
    if (mModel == null) {
      mIndices = null;
    }
    else {
      initializeIndices();
      mSortColumn = -1;
      mAscending  = true;
      mModel.addTableModelListener(this);
    }
  }

  /**
   * (re-)initializes the indices
   */
  protected void initializeIndices() {
    int       i;

    mIndices = new int[mModel.getRowCount()];
    for (i = 0; i < mIndices.length; i++)
      mIndices[i] = i;
  }

  /**
   * returns the current model, can be null
   *
   * @return            the current model
   */
  public TableModel getModel() {
    return mModel;
  }

  /**
   * returns whether the table was sorted
   *
   * @return        true if the table was sorted
   */
  public boolean isSorted() {
    return (mSortColumn > -1);
  }

  /**
   * whether the model is initialized
   *
   * @return            true if the model is not null and the sort indices
   *                    match the number of rows
   */
  protected boolean isInitialized() {
    return (getModel() != null);
  }

  /**
   * Returns the most specific superclass for all the cell values in the
   * column.
   *
   * @param columnIndex     the index of the column
   * @return                the class of the specified column
   */
  public Class getColumnClass(int columnIndex) {
    if (!isInitialized())
      return null;
    else
      return getModel().getColumnClass(columnIndex);
  }

  /**
   * Returns the number of columns in the model
   *
   * @return          the number of columns in the model
   */
  public int getColumnCount() {
    if (!isInitialized())
      return 0;
    else
      return getModel().getColumnCount();
  }

  /**
   * Returns the name of the column at columnIndex
   *
   * @param columnIndex   the column to retrieve the name for
   * @return              the name of the specified column
   */
  public String getColumnName(int columnIndex) {
    if (!isInitialized())
      return null;
    else
      return getModel().getColumnName(columnIndex);
  }

  /**
   * Returns the number of rows in the model.
   *
   * @return              the number of rows in the model
   */
  public int getRowCount() {
    if (!isInitialized())
      return 0;
    else
      return getModel().getRowCount();
  }

  /**
   * Returns the value for the cell at columnIndex and rowIndex.
   *
   * @param rowIndex      the row
   * @param columnIndex   the column
   * @return              the value of the sepcified cell
   */
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (!isInitialized())
      return null;
    else
      return getModel().getValueAt(mIndices[rowIndex], columnIndex);
  }

  /**
   * Returns true if the cell at rowIndex and columnIndex is editable.
   *
   * @param rowIndex      the row
   * @param columnIndex   the column
   * @return              true if the cell is editable
   */
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    if (!isInitialized())
      return false;
    else
      return getModel().isCellEditable(mIndices[rowIndex], columnIndex);
  }

  /**
   * Sets the value in the cell at columnIndex and rowIndex to aValue.
   *
   * @param aValue        the new value of the cell
   * @param rowIndex      the row
   * @param columnIndex   the column
   */
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    if (isInitialized())
      getModel().setValueAt(aValue, mIndices[rowIndex], columnIndex);
  }

  /**
   * sorts the table over the given column (ascending)
   *
   * @param columnIndex     the column to sort over
   */
  public void sort(int columnIndex) {
    sort(columnIndex, true);
  }

  /**
   * sorts the table over the given column, either ascending or descending
   *
   * @param columnIndex     the column to sort over
   * @param ascending       ascending if true, otherwise descending
   */
  public void sort(int columnIndex, boolean ascending) {
    int       columnType;
    int       i;
    int       n;
    int       index;
    int       backup;

    // can we sort?
    if (    (!isInitialized())
         || (getModel().getRowCount() != mIndices.length) ) {

      System.out.println(
          this.getClass().getName() + ": Table model not initialized!");

      return;
    }

    // init
    mSortColumn = columnIndex;
    mAscending  = ascending;
    initializeIndices();
    
    // determine the column type: 0=string/other, 1=number, 2=date
    if (ClassDiscovery.isSubclass(Number.class, getColumnClass(mSortColumn)))
      columnType = 1;
    else if (ClassDiscovery.isSubclass(Date.class, getColumnClass(mSortColumn)))
      columnType = 2;
    else
      columnType = 0;

    // sort ascending (descending is done below)
    for (i = 0; i < getRowCount() - 1; i++) {
      index = i;
      for (n = i + 1; n < getRowCount(); n++) {
        if (compare(mIndices[index], mIndices[n], mSortColumn, columnType) > 0)
          index = n;
      }

      // found smaller one?
      if (index != i) {
        backup          = mIndices[i];
        mIndices[i]     = mIndices[index];
        mIndices[index] = backup;
      }
    }

    // reverse sorting?
    if (!mAscending) {
      for (i = 0; i < getRowCount() / 2; i++) {
        backup                          = mIndices[i];
        mIndices[i]                     = mIndices[getRowCount() - i - 1];
        mIndices[getRowCount() - i - 1] = backup;
      }
    }
  }

  /**
   * compares two cells, returns -1 if cell1 is less than cell2, 0 if equal
   * and +1 if greater.
   *
   * @param row1        row index of cell1
   * @param row2        row index of cell2
   * @param col         colunm index
   * @param type        the class type: 0=string/other, 1=number, 2=date
   * @return            -1 if cell1&lt;cell2, 0 if cell1=cell2, +1 if 
   *                    cell1&gt;cell2
   */
  protected int compare(int row1, int row2, int col, int type) {
    int           result;
    Object        o1;
    Object        o2;
    Double        d1;
    Double        d2;

    o1 = getModel().getValueAt(row1, col);
    o2 = getModel().getValueAt(row2, col);

    // null is always smaller than non-null values
    if ( (o1 == null) &&  (o2 == null) ) {
      result = 0;
    }
    else if (o1 == null) {
      result = -1;
    }
    else if (o2 == null) {
      result = 1;
    }
    else {
      switch (type) {
        // number
        case 1:
          d1 = new Double(((Number) o1).doubleValue());
          d2 = new Double(((Number) o2).doubleValue());
          result = d1.compareTo(d2);
          break;
          
        // date
        case 2:
          result = ((Date) o1).compareTo((Date) o2);
          break;
          
        // string
        default:
          result = o1.toString().compareTo(o2.toString());
          break;
      }
    }

    return result;
  }

  /**
   * This fine grain notification tells listeners the exact range of cells,
   * rows, or columns that changed.
   *
   * @param e       the event
   */
  public void tableChanged(TableModelEvent e) {
    initializeIndices();
    if (isSorted())
      sort(mSortColumn, mAscending);
    
    fireTableChanged(e);
  }
  
  /**
   * adds a mouselistener to the header
   *
   * @param table       the table to add the listener to
   */
  public void addMouseListenerToHeader(JTable table) {
    final SortedTableModel modelFinal = this;
    final JTable tableFinal = table;
    tableFinal.setColumnSelectionAllowed(false);
    JTableHeader header = tableFinal.getTableHeader();

    if (header != null) {
      MouseAdapter listMouseListener = new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          TableColumnModel columnModel = tableFinal.getColumnModel();
          int viewColumn = columnModel.getColumnIndexAtX(e.getX());
          int column = tableFinal.convertColumnIndexToModel(viewColumn);
          if (    e.getButton() == MouseEvent.BUTTON1 
               && e.getClickCount() == 1 
               && column != -1 ) {
            int shiftPressed = e.getModifiers() & InputEvent.SHIFT_MASK;
            boolean ascending = (shiftPressed == 0);
            modelFinal.sort(column, ascending);
          }
        }
      };
      
      header.addMouseListener(listMouseListener);
    }
  }
}
