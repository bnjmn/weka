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
 *    StorePropertiesInEnvironmentStepEditorDialog.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.gui.InteractiveTableModel;
import weka.gui.InteractiveTablePanel;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.steps.StorePropertiesInEnvironment;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;

/**
 * Editor dialog for the StorePropertiesInEnvironment step.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class StorePropertiesInEnvironmentStepEditorDialog extends
  StepEditorDialog {

  private static final long serialVersionUID = 3797813883697010599L;

  protected DynamicPropertiesPanel m_dpp;

  @Override
  protected void layoutEditor() {
    String internalRep =
      ((StorePropertiesInEnvironment) getStepToEdit()).getPropsInternalRep();
    Map<String, List<String>> props =
      StorePropertiesInEnvironment.internalDynamicToMap(internalRep);
    m_dpp = new DynamicPropertiesPanel(props);

    add(m_dpp, BorderLayout.CENTER);
  }

  @Override
  public void okPressed() {
    String dps = m_dpp.getPropertiesInternal();
    ((StorePropertiesInEnvironment) getStepToEdit()).setPropsInternalRep(dps);
  }

  protected static class DynamicPropertiesPanel extends JPanel {
    private static final long serialVersionUID = 5864117781960584665L;

    protected InteractiveTablePanel m_table = new InteractiveTablePanel(
      new String[] { "Attribute name/index", "Target step", "Property name",
        "Default property value", "" });

    public DynamicPropertiesPanel(Map<String, List<String>> props) {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createTitledBorder("Properties to set from "
        + "incoming instances"));
      add(m_table, BorderLayout.CENTER);

      // populate table with variables
      int row = 0;
      JTable table = m_table.getTable();
      for (Map.Entry<String, List<String>> e : props.entrySet()) {
        String attName = e.getKey();
        String stepName = e.getValue().get(0);
        String propName = e.getValue().get(1);
        String defaultVal = e.getValue().get(2);
        if (stepName != null && stepName.length() > 0 && attName != null
          && attName.length() > 0) {
          table.getModel().setValueAt(attName, row, 0);
          table.getModel().setValueAt(stepName, row, 1);
          table.getModel().setValueAt(propName, row, 2);
          table.getModel().setValueAt(defaultVal, row, 3);
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
    public String getPropertiesInternal() {
      StringBuilder b = new StringBuilder();
      JTable table = m_table.getTable();
      int numRows = table.getModel().getRowCount();

      for (int i = 0; i < numRows; i++) {
        String attName = table.getValueAt(i, 0).toString();
        String stepName = table.getValueAt(i, 1).toString();
        String propName = table.getValueAt(i, 2).toString();
        String defVal = table.getValueAt(i, 3).toString();
        if (stepName.trim().length() > 0 && attName.trim().length() > 0) {
          if (propName.length() == 0) {
            propName = " ";
          }
          if (defVal.length() == 0) {
            defVal = " ";
          }
          b.append(attName).append(StorePropertiesInEnvironment.SEP2)
            .append(stepName).append(StorePropertiesInEnvironment.SEP2)
            .append(propName).append(StorePropertiesInEnvironment.SEP2)
            .append(defVal);
        }
        if (i < numRows - 1) {
          b.append(StorePropertiesInEnvironment.SEP1);
        }
      }

      return b.toString();
    }
  }
}
