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
 *    GOEStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.gui.GenericObjectEditor;
import weka.gui.PropertySheetPanel;
import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.Step;
import weka.knowledgeflow.steps.WekaAlgorithmWrapper;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

/**
 * A step editor dialog that uses the GOE mechanism to provide property editors.
 * This class is used for editing a Step if it does not supply a custom editor.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class GOEStepEditorDialog extends StepEditorDialog {

  /** For serialization */
  private static final long serialVersionUID = -2500973437145276268L;

  protected StepManagerImpl m_manager;
  protected Step m_stepOriginal;

  protected PropertySheetPanel m_editor = new PropertySheetPanel();
  protected PropertySheetPanel m_secondaryEditor;

  /** The main holder panel */
  protected JPanel m_editorHolder = new JPanel();

  /** The panel that contains the main editor */
  protected JPanel m_primaryEditorHolder = new JPanel();

  public GOEStepEditorDialog() {
    super();
  }

  @Override
  protected void setStepToEdit(Step step) {
    // override (and don't call super) as
    // PropertySheetPanel will do a global info panel for us

    copyOriginal(step);

    addPrimaryEditorPanel(BorderLayout.NORTH);
    addSecondaryEditorPanel(BorderLayout.SOUTH);

    JScrollPane scrollPane = new JScrollPane(m_editorHolder);
    add(scrollPane, BorderLayout.CENTER);

    if (step.getDefaultSettings() != null) {
      addSettingsButton();
    }

    layoutEditor();
  }

  protected void copyOriginal(Step step) {
    m_manager = (StepManagerImpl) step.getStepManager();
    m_stepToEdit = step;
    try {
      // copy the original config in case of cancel
      m_stepOriginal = (Step) GenericObjectEditor.makeCopy(step);
    } catch (Exception ex) {
      showErrorDialog(ex);
    }
  }

  protected void addPrimaryEditorPanel(String borderLayoutPos) {
    String className =
      m_stepToEdit instanceof WekaAlgorithmWrapper ? ((WekaAlgorithmWrapper) m_stepToEdit)
        .getWrappedAlgorithm().getClass().getName()
        : m_stepToEdit.getClass().getName();

    className =
      className.substring(className.lastIndexOf('.') + 1, className.length());

    m_primaryEditorHolder.setLayout(new BorderLayout());

    m_primaryEditorHolder.setBorder(BorderFactory.createTitledBorder(className
      + " options"));
    m_editor.setUseEnvironmentPropertyEditors(true);
    m_editor.setEnvironment(m_env);
    m_editor
      .setTarget(m_stepToEdit instanceof WekaAlgorithmWrapper ? ((WekaAlgorithmWrapper) m_stepToEdit)
        .getWrappedAlgorithm() : m_stepToEdit);
    m_editorHolder.setLayout(new BorderLayout());
    if (m_editor.editableProperties() > 0 || m_editor.hasCustomizer()) {
      m_primaryEditorHolder.add(m_editor, BorderLayout.NORTH);
      m_editorHolder.add(m_primaryEditorHolder, borderLayoutPos);
    } else {
      JPanel about = m_editor.getAboutPanel();
      m_editorHolder.add(about, borderLayoutPos);
    }
  }

  protected void addSecondaryEditorPanel(String borderLayoutPos) {
    if (m_stepToEdit instanceof WekaAlgorithmWrapper) {
      m_secondaryEditor = new PropertySheetPanel(false);
      m_secondaryEditor.setUseEnvironmentPropertyEditors(true);
      m_secondaryEditor.setBorder(BorderFactory
        .createTitledBorder("Additional options"));
      m_secondaryEditor.setEnvironment(m_env);
      m_secondaryEditor.setTarget(m_stepToEdit);
      if (m_secondaryEditor.editableProperties() > 0
        || m_secondaryEditor.hasCustomizer()) {
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(m_secondaryEditor, BorderLayout.NORTH);
        m_editorHolder.add(p, borderLayoutPos);
      }
    }
  }

  @Override
  protected void cancelPressed() {
    // restore original state
    if (m_stepOriginal != null && m_manager != null) {
      m_manager.setManagedStep(m_stepOriginal);
    }
  }

  @Override
  protected void okPressed() {
    if (m_editor.hasCustomizer()) {
      m_editor.closingOK();
    }
  }
}
