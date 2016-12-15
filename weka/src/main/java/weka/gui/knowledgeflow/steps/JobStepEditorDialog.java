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
 *    JobStepEditorDialog.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.Flow;
import weka.knowledgeflow.JSONFlowLoader;
import weka.knowledgeflow.steps.Job;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Editor dialog for the Job step.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class JobStepEditorDialog extends GOEStepEditorDialog {

  private static final long serialVersionUID = -4921559717867446684L;

  /** Button for loading the sub-flow in a new tab */
  protected JButton m_editSubFlow = new JButton("Edit sub-flow...");

  /**
   * Layout the custom part of the editor
   */
  @Override
  public void layoutEditor() {
    JPanel butHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
    butHolder.add(m_editSubFlow);
    m_editSubFlow.setEnabled(false);
    m_editorHolder.add(butHolder, BorderLayout.CENTER);
    m_editSubFlow.setEnabled(true);

    m_editSubFlow.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          Flow toEdit = loadFlow(((Job) getStepToEdit()).getFlowFile().toString());
          getMainPerspective().addTab(toEdit.getFlowName());
          getMainPerspective().getCurrentLayout().setFlow(toEdit);

          if (m_parent != null) {
            m_parent.dispose();
          }

          if (m_closingListener != null) {
            m_closingListener.closing();
          }
        } catch (Exception ex) {
          showErrorDialog(ex);
        }
      }
    });
  }

  protected Flow loadFlow(String toLoad) throws Exception {
    Flow result = null;
    toLoad = environmentSubstitute(toLoad);
    if (new File(toLoad).exists()) {
      result = Flow.loadFlow(new File(toLoad), null);
    } else {
      String fileNameWithCorrectSeparators =
        toLoad.replace(File.separatorChar, '/');

      if (this.getClass().getClassLoader()
        .getResource(fileNameWithCorrectSeparators) != null) {
        result =
          Flow.loadFlow(
            this.getClass().getClassLoader()
              .getResourceAsStream(fileNameWithCorrectSeparators),
            new JSONFlowLoader());
      }
    }
    return result;
  }
}
