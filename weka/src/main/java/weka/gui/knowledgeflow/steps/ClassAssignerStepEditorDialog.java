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
 *    ClassAssignerStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.steps.ClassAssigner;
import weka.knowledgeflow.steps.Step;

import javax.swing.*;
import java.awt.*;

/**
 * Step editor dialog for the ClassAssigner step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ClassAssignerStepEditorDialog extends GOEStepEditorDialog {
  private static final long serialVersionUID = 3105898651212196539L;

  /** Combo box for selecting the class attribute */
  protected JComboBox<String> m_classCombo = new JComboBox<String>();

  /**
   * Set the step being edited
   *
   * @param step the step to edit
   */
  @Override
  public void setStepToEdit(Step step) {
    copyOriginal(step);

    Instances incomingStructure = null;
    try {
      incomingStructure =
        step.getStepManager().getIncomingStructureForConnectionType(
          StepManager.CON_DATASET);
      if (incomingStructure == null) {
        incomingStructure =
          step.getStepManager().getIncomingStructureForConnectionType(
            StepManager.CON_TRAININGSET);
      }
      if (incomingStructure == null) {
        incomingStructure =
          step.getStepManager().getIncomingStructureForConnectionType(
            StepManager.CON_TESTSET);
      }
      if (incomingStructure == null) {
        incomingStructure =
          step.getStepManager().getIncomingStructureForConnectionType(
            StepManager.CON_INSTANCE);
      }
    } catch (WekaException ex) {
      showErrorDialog(ex);
    }

    if (incomingStructure != null) {
      m_classCombo.setEditable(true);
      for (int i = 0; i < incomingStructure.numAttributes(); i++) {
        Attribute a = incomingStructure.attribute(i);
        String attN = "(" + Attribute.typeToStringShort(a) + ") " + a.name();
        m_classCombo.addItem(attN);
      }

      setComboToClass(incomingStructure);

      JPanel p = new JPanel(new BorderLayout());
      p.setBorder(BorderFactory.createTitledBorder("Choose class attribute"));
      p.add(m_classCombo, BorderLayout.NORTH);

      createAboutPanel(step);
      add(p, BorderLayout.CENTER);
    } else {
      m_classCombo = null;
      super.setStepToEdit(step);
    }
  }

  /**
   * Populate the class combo box using the supplied instances structure
   *
   * @param incomingStructure the instances structure to use
   */
  protected void setComboToClass(Instances incomingStructure) {
    String stepC = ((ClassAssigner) getStepToEdit()).getClassColumn();
    if (stepC != null && stepC.length() > 0) {
      if (stepC.equalsIgnoreCase("/first")) {
        m_classCombo.setSelectedIndex(0);
      } else if (stepC.equalsIgnoreCase("/last")) {
        m_classCombo.setSelectedIndex(m_classCombo.getItemCount() - 1);
      } else {
        Attribute a = incomingStructure.attribute(stepC);
        if (a != null) {
          String attN = "(" + Attribute.typeToStringShort(a) + ") " + a.name();
          m_classCombo.setSelectedItem(attN);
        } else {
          // try and parse as a number
          try {
            int num = Integer.parseInt(stepC);
            num--;
            if (num >= 0 && num < incomingStructure.numAttributes()) {
              m_classCombo.setSelectedIndex(num);
            }
          } catch (NumberFormatException e) {
            // just set the value as is
            m_classCombo.setSelectedItem(stepC);
          }
        }
      }
    }
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  public void okPressed() {
    if (m_classCombo != null) {
      String selected = m_classCombo.getSelectedItem().toString();
      selected =
        selected.substring(selected.indexOf(')') + 1, selected.length()).trim();
      ((ClassAssigner) getStepToEdit()).setClassColumn(selected);
    }
  }
}
