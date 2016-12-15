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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.util.List;
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
  protected DynamicVariablesPanel m_dvp;

  @Override
  protected void layoutEditor() {
    String internalRep = ((SetVariables) getStepToEdit()).getVarsInternalRep();
    String dynamicInternalRep =
      ((SetVariables) getStepToEdit()).getDynamicVarsInternalRep();
    Map<String, String> vars = SetVariables.internalToMap(internalRep);
    Map<String, List<String>> dynamicVars =
      SetVariables.internalDynamicToMap(dynamicInternalRep);

    m_vp = new VariablesPanel(vars);
    m_dvp = new DynamicVariablesPanel(dynamicVars);
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.add("Static", m_vp);
    tabbedPane.add("Dynamic", m_dvp);

    add(tabbedPane, BorderLayout.CENTER);
  }

  @Override
  protected void okPressed() {
    String vi = m_vp.getVariablesInternal();
    String dvi = m_dvp.getVariablesInternal();
    ((SetVariables) getStepToEdit()).setVarsInternalRep(m_vp
      .getVariablesInternal());
    ((SetVariables) getStepToEdit()).setDynamicVarsInternalRep(m_dvp
      .getVariablesInternal());
  }

  protected static class DynamicVariablesPanel extends JPanel {

    private static final long serialVersionUID = -280047347103350039L;

    protected InteractiveTablePanel m_table = new InteractiveTablePanel(
      new String[] { "Attribute name/index", "Variable name", "Default value", "" });

    public DynamicVariablesPanel(Map<String, List<String>> vars) {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createTitledBorder("Variables to set from "
        + "incoming instances"));
      add(m_table, BorderLayout.CENTER);

      // populate table with variables
      int row = 0;
      JTable table = m_table.getTable();
      for (Map.Entry<String, List<String>> e : vars.entrySet()) {
        String attName = e.getKey();
        String varName = e.getValue().get(0);
        String defaultVal = e.getValue().get(1);
        if (attName != null && attName.length() > 0 && varName != null
          && varName.length() > 0) {
          table.getModel().setValueAt(attName, row, 0);
          table.getModel().setValueAt(varName, row, 1);
          table.getModel().setValueAt(defaultVal, row, 2);
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
        String attName = table.getValueAt(i, 0).toString();
        String varName = table.getValueAt(i, 1).toString();
        String defVal = table.getValueAt(i, 2).toString();
        if (attName.trim().length() > 0 && varName.trim().length() > 0) {
          if (defVal.length() == 0) {
            defVal = " ";
          }
          b.append(attName).append(SetVariables.SEP3).append(varName)
            .append(SetVariables.SEP2).append(defVal);
        }
        if (i < numRows - 1) {
          b.append(SetVariables.SEP1);
        }
      }

      return b.toString();
    }
  }

  /**
   * Panel that holds the editor for variables
   */
  protected static class VariablesPanel extends JPanel {
    private static final long serialVersionUID = 5188290550108775006L;

    protected InteractiveTablePanel m_table = new InteractiveTablePanel(
      new String[] { "Variable name", "Value", "" });

    public VariablesPanel(Map<String, String> vars) {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createTitledBorder("Static variables to set"));
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
