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
 *    Note.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import java.util.ArrayList;
import java.util.List;

/**
 * A Knowledge Flow "step" that implements a note on the GUI layout
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class Note extends BaseStep {

  /** The text of the note */
  protected String m_noteText = "New note";

  /**
   * Initialize - does nothing in the case of a note :-)
   */
  @Override
  public void stepInit() {
    // nothing to do
  }

  /**
   * Set the text of the note
   *
   * @param text the text
   */
  public void setNoteText(String text) {
    m_noteText = text;
  }

  /**
   * Get the text of the note
   *
   * @return the text
   */
  public String getNoteText() {
    return m_noteText;
  }

  /**
   * Get incoming connections accepted - none in the case of a note :-)
   *
   * @return a list of incoming connections
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    return new ArrayList<String>();
  }

  /**
   * Get outgoing connections produced - none in the case of a note :-)
   *
   * @return a list of outgoing connections
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    return new ArrayList<String>();
  }

  /**
   * Return the fully qualified name of a custom editor component (JComponent)
   * to use for editing the properties of the step. This method can return null,
   * in which case the system will dynamically generate an editor using the
   * GenericObjectEditor
   *
   * @return the fully qualified name of a step editor component
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.NoteEditorDialog";
  }
}
