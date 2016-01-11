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
 *    KF3DPerspective.java
 *    Copyright (C) 2016 Pentaho Corporation
 *
 */

package weka.gui.knowledgeflow;

import weka.core.Instances;
import weka.gui.AbstractPerspective;
import weka.gui.PerspectiveInfo;
import weka.gui.visualize.Visualize3D;

import java.awt.BorderLayout;

/**
 * New Knowledge Flow perspective for the scatter plot 3D visualization
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@PerspectiveInfo(ID = "weka.gui.knowledgeflow.visualize3d",
  title = "Visualize 3D",
  toolTipText = "Visualize instances in a 3D scatter plot",
  iconPath = "weka/gui/knowledgeflow/icons/scatterPlot3D.png")
public class KF3DPerspective extends AbstractPerspective {

  private static final long serialVersionUID = -400473918676274509L;

  /** The actual Visualize3D panel that does the work */
  protected Visualize3D m_vis = new Visualize3D();

  public KF3DPerspective() {
    setLayout(new BorderLayout());
    add(m_vis, BorderLayout.CENTER);
  }

  /**
   * Make this perspective the active (visible) one in the KF
   *
   * @param active true if this perspective is the currently active one
   */
  @Override
  public void setActive(boolean active) {
    super.setActive(active);
    if (active) {
      m_vis.updateDisplay();
    }
  }

  /**
   * Tell this perspective whether or not it is part of the users perspectives
   * toolbar in the KnowledgeFlow. If not part of the current set of
   * perspectives, then we can free some resources.
   *
   * @param loaded true if this perspective is part of the user-selected
   *          perspectives in the KnowledgeFlow
   */
  @Override
  public void setLoaded(boolean loaded) {
    super.setLoaded(loaded);
    if (!loaded) {
      freeResources();
    }
  }

  /**
   * Tell this panel to free resources held by the Java 3D system.
   */
  public void freeResources() {
    m_vis.freeResources();
  }

  @Override
  public void setInstances(Instances inst) {
    m_vis.setInstances(inst);
  }

  @Override
  public boolean acceptsInstances() {
    return true;
  }
}
