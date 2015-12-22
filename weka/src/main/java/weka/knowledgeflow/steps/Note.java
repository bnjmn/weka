package weka.knowledgeflow.steps;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class Note extends BaseStep {

  protected String m_noteText = "New note";

  @Override
  public void stepInit() {
    // nothing to do
  }

  public void setNoteText(String text) {
    m_noteText = text;
  }

  public String getNoteText() {
    return m_noteText;
  }

  /**
   * We override this so that we don't get messages printed to the log and
   * status area for notes :-)
   */
  @Override
  public void stop() {
    m_stopRequested = true;
    getStepManager().throughputUpdateEnd();
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    return new ArrayList<String>();
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return new ArrayList<String>();
  }

  // TODO set the name of the step editor dialog

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.NoteEditorDialog";
  }
}
