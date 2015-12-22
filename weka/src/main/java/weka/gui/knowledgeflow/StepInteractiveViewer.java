package weka.gui.knowledgeflow;

import java.awt.Window;

import weka.core.Settings;
import weka.core.WekaException;
import weka.knowledgeflow.steps.Step;

/**
 * Interface for GUI interactive viewer components that can be popped up from
 * the contextual menu in the Knowledge Flow that appears when you right-click
 * over a step on the layout.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface StepInteractiveViewer {

  /**
   * Set the step that owns this viewer. Implementations may want to access data
   * that has been computed by the step in question.
   * 
   * @param theStep the step that owns this viewer
   */
  void setStep(Step theStep);

  /**
   * Set the main knowledge flow perspective. Implementations can then access
   * application settings if necessary
   *
   * @param perspective the main knowledge flow perspective
   */
  void setMainKFPerspective(MainKFPerspective perspective);

  /**
   * Get the main knowledge flow perspective. Implementations can the access
   * application settings if necessary
   *
   * @return
   */
  MainKFPerspective getMainKFPerspective();

  /**
   * Get the name of this viewer
   * 
   * @return the name of this viewer
   */
  String getViewerName();

  /**
   * Set the parent window for this viewer
   *
   * @param parent the parent window
   */
  void setParentWindow(Window parent);

  /**
   * Get the settings object
   *
   * @return the settings object
   */
  Settings getSettings();

  /**
   * Initialize this viewer. The KnowledgeFlow application will call this method
   * after constructing the viewer and after calling setStep() and
   * setParentWindow(). Implementers will typically layout their view in this
   * method (rather than a constructor)
   *
   * @throws WekaException if the initialization fails
   */
  void init() throws WekaException;

  /**
   * Called by the KnowledgeFlow application once the enclosing JFrame is
   * visible
   */
  void nowVisible();
}
