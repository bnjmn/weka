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
 *    DataVisualizerStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.knowledgeflow.steps.DataVisualizer;

/**
 * Editor dialog for the DataVisualizer step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class DataVisualizerStepEditorDialog extends
  ModelPerformanceChartStepEditorDialog {

  private static final long serialVersionUID = -6032558757326543902L;

  /**
   * Get the name of the offscreen renderer and options from the step
   * being edited
   */
  @Override
  protected void getCurrentSettings() {
    m_currentRendererName =
      ((DataVisualizer) getStepToEdit()).getOffscreenRendererName();
    m_currentRendererOptions =
      ((DataVisualizer) getStepToEdit()).getOffscreenAdditionalOpts();
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  public void okPressed() {
    ((DataVisualizer) getStepToEdit())
      .setOffscreenRendererName(m_offscreenSelector.getSelectedItem()
        .toString());
    ((DataVisualizer) getStepToEdit())
      .setOffscreenAdditionalOpts(m_rendererOptions.getText());
  }
}
