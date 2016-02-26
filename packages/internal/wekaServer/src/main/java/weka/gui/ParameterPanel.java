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
 *    ParameterPanel.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * Class providing a panel for configuring flow variables
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ParameterPanel extends JPanel {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -491659781619944544L;

  /** The JTable for configuring variables */
  protected InteractiveTablePanel m_table = new InteractiveTablePanel(
    new String[] { "Variable", "Value", "" });

  /**
   * Construct a new ParameterPanel
   */
  public ParameterPanel() {
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createTitledBorder("Flow variables"));
    add(m_table, BorderLayout.CENTER);
  }

  /**
   * Get the configured variables
   *
   * @return a Map of variable names and values
   */
  public Map<String, String> getParameters() {

    Map<String, String> result = new HashMap<String, String>();
    JTable table = m_table.getTable();
    int numRows = table.getModel().getRowCount();

    for (int i = 0; i < numRows; i++) {
      String paramName = table.getValueAt(i, 0).toString();
      String paramValue = table.getValueAt(i, 1).toString();
      if (paramName.length() > 0 && paramValue.length() > 0) {
        result.put(paramName, paramValue);
      }
    }

    return result;
  }
}
