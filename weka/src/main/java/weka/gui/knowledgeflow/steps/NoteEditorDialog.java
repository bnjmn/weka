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
 *    NoteEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.steps.Note;
import weka.knowledgeflow.steps.Step;

import javax.swing.*;
import java.awt.*;

/**
 * Editor dialog for Notes
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class NoteEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = 2358735294813135692L;

  /** Text area of the note */
  protected JTextArea m_textArea = new JTextArea(5, 40);

  /**
   * Set the step (note) being edited
   *
   * @param step the step to edit
   */
  @Override
  protected void setStepToEdit(Step step) {
    // override to prevent an "about" panel getting added
    m_stepToEdit = step;
    layoutEditor();
  }

  /**
   * Layout the note editor
   */
  @Override
  public void layoutEditor() {
    m_textArea.setLineWrap(true);
    String noteText = ((Note) getStepToEdit()).getNoteText();
    m_textArea.setText(noteText);
    JScrollPane sc = new JScrollPane(m_textArea);

    JPanel holder = new JPanel(new BorderLayout());
    holder.setBorder(BorderFactory.createTitledBorder("Note Editor"));
    holder.add(sc, BorderLayout.CENTER);
    add(holder, BorderLayout.CENTER);
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  public void okPressed() {
    ((Note) getStepToEdit()).setNoteText(m_textArea.getText());
  }
}
