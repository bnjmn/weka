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
 *    ClassValuePickerStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.EnvironmentField;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.steps.ClassValuePicker;
import weka.knowledgeflow.steps.Step;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Editor dialog for the ClassValuePicker step.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ClassValuePickerStepEditorDialog extends GOEStepEditorDialog {

  private static final long serialVersionUID = 7009335918650499183L;

  /** For displaying class values */
  protected JComboBox m_classValueCombo = new EnvironmentField.WideComboBox();

  /**
   * Set the step to edit
   *
   * @param step the step to edit
   */
  @Override
  @SuppressWarnings("unchecked")
  public void setStepToEdit(Step step) {
    copyOriginal(step);

    Instances incomingStructure = null;

    try {
      incomingStructure = step.getStepManager()
        .getIncomingStructureForConnectionType(StepManager.CON_DATASET);
      if (incomingStructure == null) {
        incomingStructure = step.getStepManager()
          .getIncomingStructureForConnectionType(StepManager.CON_TRAININGSET);
      }
      if (incomingStructure == null) {
        incomingStructure = step.getStepManager()
          .getIncomingStructureForConnectionType(StepManager.CON_TESTSET);
      }
      if (incomingStructure == null) {
        incomingStructure = step.getStepManager()
          .getIncomingStructureForConnectionType(StepManager.CON_INSTANCE);
      }
    } catch (WekaException ex) {
      showErrorDialog(ex);
    }

    if (incomingStructure != null) {

      if (incomingStructure.classIndex() < 0) {
        showInfoDialog("No class attribute is set in the data",
          "ClassValuePicker", true);
        add(new JLabel("No class attribute set in data"), BorderLayout.CENTER);
      } else if (incomingStructure.classAttribute().isNumeric()) {
        showInfoDialog("Cant set class value - class is numeric!",
          "ClassValuePicker", true);
        add(new JLabel("Can't set class value - class is numeric"),
          BorderLayout.CENTER);
      } else {
        m_classValueCombo.setEditable(true);
        m_classValueCombo
          .setToolTipText("Class label. /first, /last and /<num> "
            + "can be used to specify the first, last or specific index "
            + "of the label to use respectively");

        for (int i = 0; i < incomingStructure.classAttribute()
          .numValues(); i++) {
          m_classValueCombo
            .addItem(incomingStructure.classAttribute().value(i));
        }
        String stepL = ((ClassValuePicker) getStepToEdit()).getClassValue();
        if (stepL != null && stepL.length() > 0) {
          m_classValueCombo.setSelectedItem(stepL);
        }

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Choose class value"));
        p.add(m_classValueCombo, BorderLayout.NORTH);

        createAboutPanel(step);
        add(p, BorderLayout.CENTER);
      }
    } else {
      super.setStepToEdit(step);
    }
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  public void okPressed() {
    Object selected = m_classValueCombo.getSelectedItem();

    if (selected != null) {
      ((ClassValuePicker) getStepToEdit()).setClassValue(selected.toString());
    }
  }
}
