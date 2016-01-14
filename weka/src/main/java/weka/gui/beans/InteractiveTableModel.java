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

/**
 * Table model that automatically adds a new row to the table on pressing enter
 * in the last cell of a row.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 47640 $
 * @deprecated Use {@code weka.gui.InteractiveTableModel} instead. Retained for
 * backward compatibility
 */
@Deprecated
public class InteractiveTableModel extends weka.gui.InteractiveTableModel {

  private static final long serialVersionUID = 7628124449228704885L;

  /**
   * Constructor
   *
   * @param columnNames the names of the columns
   */
  public InteractiveTableModel(String[] columnNames) {
    super(columnNames);
  }
}
