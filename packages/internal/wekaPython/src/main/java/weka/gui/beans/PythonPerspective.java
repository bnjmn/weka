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
 *    PythonPerspective.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;

import javax.swing.Icon;
import javax.swing.JPanel;

import weka.core.Instances;
import weka.gui.PythonPanel;

/**
 * A KnowledgeFlow perspective providing a CPython scripting environment
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class PythonPerspective extends JPanel implements
  KnowledgeFlowApp.KFPerspective {

  /** For serialization */
  private static final long serialVersionUID = 8089719525675841189L;

  /** The actual python panel */
  protected PythonPanel m_pythonPanel;

  /**
   * Constructor
   */
  public PythonPerspective() {
    setLayout(new BorderLayout());
    m_pythonPanel = new PythonPanel(true, null);
    add(m_pythonPanel, BorderLayout.CENTER);
  }

  /**
   * Set the instances to use - pushes them over to python and converts to a
   * pandas dataframe
   *
   * @param insts the instances the instances to use
   * @throws Exception if a problem occurs
   */
  @Override
  public void setInstances(Instances insts) throws Exception {
    m_pythonPanel.sendInstancesToPython(insts);
  }

  /**
   * Returns true, as this panel sends instances into the python environment
   *
   * @return true
   */
  @Override
  public boolean acceptsInstances() {
    return true;
  }

  /**
   * Get the perspective's title
   *
   * @return the title of this perspective
   */
  @Override
  public String getPerspectiveTitle() {
    return "CPython scripting";
  }

  /**
   * Get the perspective's tool tip
   *
   * @return the perspective's tool tip
   */
  @Override
  public String getPerspectiveTipText() {
    return "Write and execute Python scripts";
  }

  /**
   * Get the perspective's icon
   *
   * @return the perspective's icon
   */
  @Override
  public Icon getPerspectiveIcon() {
    return PythonPanel.loadIcon("weka/gui/beans/icons/python-logo_small.png");
  }

  /**
   * Called when the perspective becomes the active one in the KnowledgeFlow
   *
   * @param active true if the perspective is the active one
   */
  @Override
  public void setActive(boolean active) {
  }

  /**
   * Called when the perspective is loaded
   *
   * @param loaded true if the perspective has been loaded
   */
  @Override
  public void setLoaded(boolean loaded) {
  }

  /**
   * Set a reference to the main KnowlegeFlow perspective
   *
   * @param main the main knowledge flow perspective
   */
  @Override
  public void setMainKFPerspective(KnowledgeFlowApp.MainKFPerspective main) {
  }
}
