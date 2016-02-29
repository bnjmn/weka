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
 *    ASEvaluatorStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.steps.ASEvaluator;
import weka.knowledgeflow.steps.Step;

import javax.swing.*;
import java.awt.*;

/**
 * Step editor dialog for the ASEvaluator step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ASEvaluatorStepEditorDialog extends GOEStepEditorDialog {

  private static final long serialVersionUID = -166234654984288982L;

  /** Check box for selecting whether to treat x-val folds separately or not */
  protected JCheckBox m_treatXValFoldsSeparately = new JCheckBox(
    "Treat x-val folds separately");

  /**
   * Set the step to edit in this dialog
   *
   * @param step the step to edit
   */
  @Override
  public void setStepToEdit(Step step) {
    copyOriginal(step);
    addPrimaryEditorPanel(BorderLayout.NORTH);

    JPanel p = new JPanel(new BorderLayout());
    p.add(m_treatXValFoldsSeparately, BorderLayout.NORTH);
    m_primaryEditorHolder.add(p, BorderLayout.CENTER);

    add(m_editorHolder, BorderLayout.CENTER);
    m_treatXValFoldsSeparately.setSelected(((ASEvaluator) step)
      .getTreatXValFoldsSeparately());
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  protected void okPressed() {
    ((ASEvaluator) m_stepToEdit)
      .setTreatXValFoldsSeparately(m_treatXValFoldsSeparately.isSelected());
  }
}
