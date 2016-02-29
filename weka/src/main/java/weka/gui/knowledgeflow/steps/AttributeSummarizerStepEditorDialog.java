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

/*`
 *    AttributeSummarizerStepEditorDialog.java
 *    Copyright (C) 2002-2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.knowledgeflow.steps.AttributeSummarizer;

/**
 * Step editor dialog for the attribute summarizer step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class AttributeSummarizerStepEditorDialog extends
  ModelPerformanceChartStepEditorDialog {

  private static final long serialVersionUID = -4504946065343184549L;

  /**
   * Get the offscreen renderer and options from the step being edited
   */
  @Override
  protected void getCurrentSettings() {
    m_currentRendererName =
      ((AttributeSummarizer) getStepToEdit()).getOffscreenRendererName();
    m_currentRendererOptions =
      ((AttributeSummarizer) getStepToEdit()).getOffscreenAdditionalOpts();
  }

  /**
   * Called when OK is pressed
   */
  @Override
  public void okPressed() {
    ((AttributeSummarizer) getStepToEdit())
      .setOffscreenRendererName(m_offscreenSelector.getSelectedItem()
        .toString());
    ((AttributeSummarizer) getStepToEdit())
      .setOffscreenAdditionalOpts(m_rendererOptions.getText());
  }
}
