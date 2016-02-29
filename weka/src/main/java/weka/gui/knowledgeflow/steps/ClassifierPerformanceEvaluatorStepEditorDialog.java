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
 *    ClassifierPerformanceEvaluatorStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.gui.EvaluationMetricSelectionDialog;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.steps.ClassifierPerformanceEvaluator;
import weka.knowledgeflow.steps.Step;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI step editor dialog for the ClassifierPerformanceEvaluator step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ClassifierPerformanceEvaluatorStepEditorDialog extends
  GOEStepEditorDialog {

  private static final long serialVersionUID = -4459460141076392499L;

  /** Button for popping up the evaluation metrics selector dialog */
  protected JButton m_showEvalDialog = new JButton("Evaluation metrics...");

  /** Holds selected evaluation metrics */
  protected List<String> m_evaluationMetrics = new ArrayList<String>();

  /**
   * Constructor
   */
  public ClassifierPerformanceEvaluatorStepEditorDialog() {
    super();
  }

  /**
   * Set the step to edit
   *
   * @param step the step to edit
   */
  @Override
  public void setStepToEdit(Step step) {
    copyOriginal(step);

    addPrimaryEditorPanel(BorderLayout.NORTH);
    JPanel p = new JPanel(new BorderLayout());
    p.add(m_showEvalDialog, BorderLayout.NORTH);
    m_primaryEditorHolder.add(p, BorderLayout.CENTER);
    add(m_editorHolder, BorderLayout.NORTH);

    String evalM =
      ((ClassifierPerformanceEvaluator) step).getEvaluationMetricsToOutput();
    if (evalM != null && evalM.length() > 0) {
      String[] parts = evalM.split(",");
      for (String s : parts) {
        m_evaluationMetrics.add(s.trim());
      }
    }

    m_showEvalDialog.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EvaluationMetricSelectionDialog esd =
          new EvaluationMetricSelectionDialog(m_parent, m_evaluationMetrics);
        esd.setLocation(m_showEvalDialog.getLocationOnScreen());
        esd.pack();
        esd.setVisible(true);
        m_evaluationMetrics = esd.getSelectedEvalMetrics();
      }
    });
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  protected void okPressed() {
    StringBuilder b = new StringBuilder();
    for (String s : m_evaluationMetrics) {
      b.append(s).append(",");
    }
    String newList = b.substring(0, b.length() - 1);
    ((ClassifierPerformanceEvaluator) getStepToEdit())
      .setEvaluationMetricsToOutput(newList);
  }
}
