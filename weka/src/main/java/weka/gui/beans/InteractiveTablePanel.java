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
 *    InteractiveTablePanel.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Provides a panel using an interactive table model.
 * 
 * @author Mark Hall (mhall{[at]}penthao{[dot]}com)
 * @version $Revision$
 */
public class InteractiveTablePanel extends JPanel {

  /** For serialization */
  private static final long serialVersionUID = 4495705463732140410L;

  /** Holds column names */
  protected String[] m_columnNames;

  /** The table itself */
  protected JTable m_table;

  /** Scroll panel for the table */
  protected JScrollPane m_scroller;

  /** Model for the table */
  protected InteractiveTableModel m_tableModel;

  /**
   * Constructor
   * 
   * @param colNames the names of the columns
   */
  public InteractiveTablePanel(String[] colNames) {
    m_columnNames = colNames;
    initComponent();
  }

  /**
   * Initializes the component
   */
  public void initComponent() {
    m_tableModel = new InteractiveTableModel(m_columnNames);
    m_tableModel
      .addTableModelListener(new InteractiveTablePanel.InteractiveTableModelListener());
    m_table = new JTable();
    m_table.setModel(m_tableModel);
    m_table.setSurrendersFocusOnKeystroke(true);
    if (!m_tableModel.hasEmptyRow()) {
      m_tableModel.addEmptyRow();
    }

    InteractiveTableModel model = (InteractiveTableModel) m_table.getModel();
    m_scroller = new javax.swing.JScrollPane(m_table);
    m_table.setPreferredScrollableViewportSize(new java.awt.Dimension(500, 80));
    TableColumn hidden =
      m_table.getColumnModel().getColumn(model.m_hidden_index);
    hidden.setMinWidth(2);
    hidden.setPreferredWidth(2);
    hidden.setMaxWidth(2);
    hidden.setCellRenderer(new InteractiveRenderer(model.m_hidden_index));

    setLayout(new BorderLayout());
    add(m_scroller, BorderLayout.CENTER);
  }

  /**
   * Get the JTable component
   * 
   * @return the JTable
   */
  public JTable getTable() {
    return m_table;
  }

  /**
   * Highlight the last row in the table
   * 
   * @param row the row
   */
  public void highlightLastRow(int row) {
    int lastrow = m_tableModel.getRowCount();
    if (row == lastrow - 1) {
      m_table.setRowSelectionInterval(lastrow - 1, lastrow - 1);
    } else {
      m_table.setRowSelectionInterval(row + 1, row + 1);
    }

    m_table.setColumnSelectionInterval(0, 0);
  }

  /**
   * Renderer for the InteractiveTablePanel
   */
  class InteractiveRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 6186813827783402502L;

    /** The index of the interactive column */
    protected int m_interactiveColumn;

    /**
     * Constructor
     * 
     * @param interactiveColumn the column that is interactive (i.e. you can
     *          press return to generate a new row from it)
     */
    public InteractiveRenderer(int interactiveColumn) {
      m_interactiveColumn = interactiveColumn;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {
      Component c =
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
          row, column);
      if (column == m_interactiveColumn && hasFocus) {
        if ((InteractiveTablePanel.this.m_tableModel.getRowCount() - 1) == row
          && !InteractiveTablePanel.this.m_tableModel.hasEmptyRow()) {
          InteractiveTablePanel.this.m_tableModel.addEmptyRow();
        }

        highlightLastRow(row);
      }

      return c;
    }
  }

  /**
   * Listener for the InteractiveTablePanel
   */
  public class InteractiveTableModelListener implements TableModelListener {
    @Override
    public void tableChanged(TableModelEvent evt) {
      if (evt.getType() == TableModelEvent.UPDATE) {
        int column = evt.getColumn();
        int row = evt.getFirstRow();
        m_table.setColumnSelectionInterval(column + 1, column + 1);
        m_table.setRowSelectionInterval(row, row);
      }
    }
  }
}
