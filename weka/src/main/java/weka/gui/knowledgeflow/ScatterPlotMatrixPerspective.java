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
 *    ScatterPlotMatrixPerspective.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.Defaults;
import weka.core.Instances;
import weka.gui.AbstractPerspective;
import weka.gui.PerspectiveInfo;
import weka.gui.explorer.VisualizePanel;
import weka.gui.visualize.MatrixPanel;
import weka.gui.visualize.VisualizeUtils;

import java.awt.BorderLayout;

/**
 * Knowledge Flow perspective for the scatter plot matrix
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@PerspectiveInfo(ID = "weka.gui.knowledgeflow.scatterplotmatrixperspective",
  title = "Scatter plot matrix", toolTipText = "Scatter plots",
  iconPath = "weka/gui/knowledgeflow/icons/application_view_tile.png")
public class ScatterPlotMatrixPerspective extends AbstractPerspective {

  private static final long serialVersionUID = 5661598509822826837L;

  /** The actual matrix panel */
  protected MatrixPanel m_matrixPanel;

  /** The dataset being visualized */
  protected Instances m_visualizeDataSet;

  /**
   * Constructor
   */
  public ScatterPlotMatrixPerspective() {
    setLayout(new BorderLayout());
    m_matrixPanel = new MatrixPanel();
    add(m_matrixPanel, BorderLayout.CENTER);
  }

  /**
   * Get default settings
   *
   * @return the default settings of this perspective
   */
  @Override
  public Defaults getDefaultSettings() {
    // re-use explorer.VisualizePanel.ScatterDefaults, but set the ID
    // to be our perspective ID
    Defaults d = new VisualizePanel.ScatterDefaults();
    d.setID(getPerspectiveID());
    d.add(new VisualizeUtils.VisualizeDefaults());
    return d;
  }

  /**
   * Returns true - we accept instances.
   *
   * @return true
   */
  @Override
  public boolean acceptsInstances() {
    return true;
  }

  /**
   * Called when this perspective becomes the "active" (i.e. visible) one
   *
   * @param active true if this perspective is the active one
   */
  @Override
  public void setActive(boolean active) {
    super.setActive(active);
    if (m_isActive && m_visualizeDataSet != null) {
      m_matrixPanel.applySettings(getMainApplication().getApplicationSettings(),
        getPerspectiveID());
      m_matrixPanel.updatePanel();
    }
  }

  /**
   * Set the instances to use
   *
   * @param instances the instances instances to use
   */
  @Override
  public void setInstances(Instances instances) {
    m_visualizeDataSet = instances;
    m_matrixPanel.setInstances(m_visualizeDataSet);
  }

  /**
   * Can we be active (i.e. selected) at this point in time? True if we
   * have a dataset to visualize
   *
   * @return true if we have a dataset to visualize
   */
  @Override
  public boolean okToBeActive() {
    return m_visualizeDataSet != null;
  }

}
