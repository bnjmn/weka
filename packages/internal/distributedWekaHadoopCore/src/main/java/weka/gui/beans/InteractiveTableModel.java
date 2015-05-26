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
 *    InteractiveTableModel.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.beans;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model that automatically adds a new row to the table on pressing enter
 * in the last cell of a row.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class InteractiveTableModel extends AbstractTableModel {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -5113873323690309667L;

  /** Index of the hidden column */
  public int m_hidden_index;

  /** The names of the columns */
  protected String[] m_columnNames;

  /** Holds the data */
  protected List<List<String>> m_dataVector;

  /**
   * Constructor
   * 
   * @param columnNames the names of the columns
   */
  public InteractiveTableModel(String[] columnNames) {
    m_columnNames = columnNames;
    m_dataVector = new ArrayList<List<String>>();
    m_hidden_index = columnNames.length - 1;
  }

  @Override
  public String getColumnName(int column) {
    return m_columnNames[column];
  }

  @Override
  public boolean isCellEditable(int row, int column) {
    if (column == m_hidden_index) {
      return false;
    }
    return true;
  }

  @Override
  public Class<?> getColumnClass(int column) {
    return String.class;
  }

  @Override
  public Object getValueAt(int row, int column) {
    if (column >= m_columnNames.length) {
      return new Object();
    }

    List<String> rowData = m_dataVector.get(row);
    return rowData.get(column);
  }

  @Override
  public void setValueAt(Object value, int row, int column) {
    if (column >= m_columnNames.length) {
      System.err.println("Invalid index");
    }

    List<String> rowData = m_dataVector.get(row);
    rowData.set(column, value.toString());
    fireTableCellUpdated(row, column);
  }

  @Override
  public int getRowCount() {
    return m_dataVector.size();
  }

  @Override
  public int getColumnCount() {
    return m_columnNames.length;
  }

  /**
   * Returns true if the model has an empty row
   * 
   * @return true if the model has an empty row
   */
  public boolean hasEmptyRow() {
    if (m_dataVector.size() == 0) {
      return false;
    }

    List<String> dataRow = m_dataVector.get(m_dataVector.size() - 1);
    for (String s : dataRow) {
      if (s.length() != 0) {
        return false;
      }
    }

    return true;
  }

  /**
   * Adds an empty row to the model
   */
  public void addEmptyRow() {
    ArrayList<String> empty = new ArrayList<String>();
    for (int i = 0; i < m_columnNames.length; i++) {
      empty.add("");
    }
    m_dataVector.add(empty);
    fireTableRowsInserted(m_dataVector.size() - 1, m_dataVector.size() - 1);
  }
}
