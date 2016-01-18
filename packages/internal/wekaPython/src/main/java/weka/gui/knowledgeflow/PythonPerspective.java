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

package weka.gui.knowledgeflow;

import java.awt.BorderLayout;

import weka.core.Instances;
import weka.gui.AbstractPerspective;
import weka.gui.PerspectiveInfo;
import weka.gui.PythonPanel;
import weka.gui.WorkbenchDefaults;

/**
 * A KnowledgeFlow perspective providing a CPython scripting environment
 *
 * @author Mark Hall
 * @version $Revision: $
 */
@PerspectiveInfo(ID = "python", title = "CPython Scripting",
  toolTipText = "Write and execute Python scripts",
  iconPath = "weka/gui/knowledgeflow/icons/python-logo_small.png")
public class PythonPerspective extends AbstractPerspective {

  /** For serialization */
  private static final long serialVersionUID = 1309879745978244501L;

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
   * Called by the PerspectiveManager once instantiation is complete and
   * a main application is available to this perspective
   */
  @Override
  public void instantiationComplete() {
    m_pythonPanel = new PythonPanel(
      !getMainApplication().getApplicationID().equals(WorkbenchDefaults.APP_ID),
      null);
    add(m_pythonPanel, BorderLayout.CENTER);
  }

  /**
   * Requires a log when running in the Workbench application
   *
   * @return true if running in the Workbench application
   */
  @Override
  public boolean requiresLog() {
    return getMainApplication().getApplicationID()
      .equals(WorkbenchDefaults.APP_ID);
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
   * Set the instances to use - pushes them over to python and converts to a
   * pandas dataframe
   *
   * @param instances the instances the instances to use
   */
  @Override
  public void setInstances(Instances instances) {
    try {
      m_pythonPanel.sendInstancesToPython(instances);
    } catch (Exception ex) {
      getMainApplication().showErrorDialog(ex);
    }
  }
}
