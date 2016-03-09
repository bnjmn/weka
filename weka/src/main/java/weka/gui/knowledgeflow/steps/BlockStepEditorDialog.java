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
 *    BlockStepEditorDialog.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.Block;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Step editor dialog for the Block step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class BlockStepEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = 1183316309880876170L;

  /** The combo box for choosing the step to block on */
  protected JComboBox<String> m_stepToBlockBox = new JComboBox<String>();

  /**
   * Layout the component
   */
  @Override
  public void layoutEditor() {
    m_stepToBlockBox.setEditable(true);

    StepManager sm = getStepToEdit().getStepManager();
    List<StepManagerImpl> flowSteps =
      getMainPerspective().getCurrentLayout().getFlow().getSteps();
    for (StepManagerImpl smi : flowSteps) {
      m_stepToBlockBox.addItem(smi.getName());
    }

    JPanel p = new JPanel(new BorderLayout());
    p.setBorder(BorderFactory.createTitledBorder("Choose step to wait for"));
    p.add(m_stepToBlockBox, BorderLayout.NORTH);

    add(p, BorderLayout.CENTER);

    String userSelected = ((Block) getStepToEdit()).getStepToWaitFor();
    if (userSelected != null) {
      m_stepToBlockBox.setSelectedItem(userSelected);
    }
  }

  /**
   * Called when OK is pressed
   */
  @Override
  public void okPressed() {

    String selected = (String) m_stepToBlockBox.getSelectedItem();
    ((Block) getStepToEdit()).setStepToWaitFor(selected);
  }
}
