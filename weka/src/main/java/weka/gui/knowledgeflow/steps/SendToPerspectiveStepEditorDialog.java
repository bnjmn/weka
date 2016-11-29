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
 *    SendToPerspectiveStepEditorDialog.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.WekaException;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.gui.knowledgeflow.GetPerspectiveNamesGraphicalCommand;
import weka.knowledgeflow.steps.SendToPerspective;
import weka.knowledgeflow.steps.Step;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Dialog for the SendToPerspective step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SendToPerspectiveStepEditorDialog extends GOEStepEditorDialog {

  private static final long serialVersionUID = -8282588308511754826L;

  /** Combo box for selecting the perspective to send to */
  protected JComboBox<String> m_perspectivesCombo = new JComboBox<>();

  /**
   * Sets the step to edit and configures the dialog
   *
   * @param step the step to edit
   */
  @Override
  public void setStepToEdit(Step step) {
    copyOriginal(step);

    try {
      List<String> visiblePerspectives =
        getGraphicalEnvironmentCommandHandler().performCommand(
          GetPerspectiveNamesGraphicalCommand.GET_PERSPECTIVE_NAMES_KEY);
      for (String s : visiblePerspectives) {
        m_perspectivesCombo.addItem(s);
      }

    } catch (WekaException ex) {
      showErrorDialog(ex);
    }

    String current = ((SendToPerspective) getStepToEdit()).getPerspectiveName();
    m_perspectivesCombo.setSelectedItem(current);

    JPanel p = new JPanel(new BorderLayout());
    p.setBorder(BorderFactory
      .createTitledBorder("Choose perspective to send to"));
    p.add(m_perspectivesCombo, BorderLayout.NORTH);

    createAboutPanel(step);
    add(p, BorderLayout.CENTER);
  }

  /**
   * Handle the OK button
   */
  @Override
  public void okPressed() {
    String selectedPerspective =
      m_perspectivesCombo.getSelectedItem().toString();
    ((SendToPerspective) getStepToEdit())
      .setPerspectiveName(selectedPerspective);
  }
}
