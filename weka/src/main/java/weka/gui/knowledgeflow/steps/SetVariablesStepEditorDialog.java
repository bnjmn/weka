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
 *    SetVariablesStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.gui.InteractiveTableModel;
import weka.gui.InteractiveTablePanel;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.steps.SetVariables;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.util.Map;

/**
 * Editor dialog for the {@code SetVariables} step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SetVariablesStepEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = 5747505623497131129L;

  /** panel for editing variables */
  protected VariablesPanel m_vp;

  @Override
  protected void layoutEditor() {
    String internalRep = ((SetVariables) getStepToEdit()).getVarsInternalRep();
    Map<String, String> vars = SetVariables.internalToMap(internalRep);

    m_vp = new VariablesPanel(vars);
    add(m_vp, BorderLayout.CENTER);
  }

  @Override
  protected void okPressed() {
    String vi = m_vp.getVariablesInternal();
    ((SetVariables) getStepToEdit())
      .setVarsInternalRep(m_vp.getVariablesInternal());
  }

  /**
   * Panel that holds the editor for variables
   */
  protected static class VariablesPanel extends JPanel {
    private static final long serialVersionUID = 5188290550108775006L;

    protected InteractiveTablePanel m_table =
      new InteractiveTablePanel(new String[] { "Variable", "Value", "" });

    public VariablesPanel(Map<String, String> vars) {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createTitledBorder("Variables to set"));
      add(m_table, BorderLayout.CENTER);

      // populate table with variables
      int row = 0;
      JTable table = m_table.getTable();
      for (Map.Entry<String, String> e : vars.entrySet()) {
        String varName = e.getKey();
        String varVal = e.getValue();

        if (varVal != null && varVal.length() > 0) {
          table.getModel().setValueAt(varName, row, 0);
          table.getModel().setValueAt(varVal, row, 1);
          ((InteractiveTableModel) table.getModel()).addEmptyRow();
          row++;
        }
      }
    }

    /**
     * Get the variables in internal format
     *
     * @return the variables + settings in the internal format
     */
    public String getVariablesInternal() {
      StringBuilder b = new StringBuilder();
      JTable table = m_table.getTable();
      int numRows = table.getModel().getRowCount();

      for (int i = 0; i < numRows; i++) {
        String paramName = table.getValueAt(i, 0).toString();
        String paramValue = table.getValueAt(i, 1).toString();
        if (paramName.length() > 0 && paramValue.length() > 0) {
          b.append(paramName).append(SetVariables.SEP2).append(paramValue);
        }
        if (i < numRows - 1) {
          b.append(SetVariables.SEP1);
        }
      }

      return b.toString();
    }
  }
}
