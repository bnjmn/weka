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
 *    StepInteractiveViewer.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.Settings;
import weka.core.WekaException;
import weka.knowledgeflow.steps.Step;

import java.awt.*;

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
