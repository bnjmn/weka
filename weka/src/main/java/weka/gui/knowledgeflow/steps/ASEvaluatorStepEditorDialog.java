package weka.gui.knowledgeflow.steps;

import java.awt.BorderLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.steps.ASEvaluator;
import weka.knowledgeflow.steps.Step;

/**
 * Step editor dialog for the ASEvaluator step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ASEvaluatorStepEditorDialog extends GOEStepEditorDialog {

  private static final long serialVersionUID = -166234654984288982L;

  protected JCheckBox m_treatXValFoldsSeparately = new JCheckBox(
    "Treat x-val folds separately");

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

  @Override
  protected void okPressed() {
    ((ASEvaluator) m_stepToEdit)
      .setTreatXValFoldsSeparately(m_treatXValFoldsSeparately.isSelected());
  }
}
