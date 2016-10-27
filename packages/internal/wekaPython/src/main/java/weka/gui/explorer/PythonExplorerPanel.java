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
 *    PythonExplorerPanel.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.explorer;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.Logger;
import weka.gui.PythonPanel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Explorer plugin for CPython scripting
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class PythonExplorerPanel extends JPanel implements
  Explorer.ExplorerPanel, Explorer.LogHandler {

  /** For serialization */
  private static final long serialVersionUID = -340790747195368260L;

  /** The Explorer instance */
  protected Explorer m_explorer;

  /** The PythonPanel that does all the work */
  PythonPanel m_pythonPanel;

  /**
   * Constructor
   */
  public PythonExplorerPanel() {
    setLayout(new BorderLayout());
  }

  /**
   * Layout the panel
   */
  protected void setup() {
    m_pythonPanel = new PythonPanel(false, m_explorer);
    add(m_pythonPanel, BorderLayout.CENTER);
  }

  /**
   * Set the Explorer instance
   *
   * @param parent the parent frame
   */
  @Override
  public void setExplorer(Explorer parent) {
    m_explorer = parent;
    setup();
  }

  /**
   * Get the Explorer instance
   *
   * @return the Explorer instance
   */
  @Override
  public Explorer getExplorer() {
    return m_explorer;
  }

  /**
   * Sets the current instances object (passes them through to python)
   *
   * @param inst a set of Instances
   */
  @Override
  public void setInstances(Instances inst) {
    if (m_pythonPanel != null) {
      try {
        m_pythonPanel.sendInstancesToPython(inst);
      } catch (WekaException ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Get the tab title for this plugin
   *
   * @return the tab title
   */
  @Override
  public String getTabTitle() {
    return "CPython Scripting";
  }

  /**
   * Get the tool tip for this plugin
   *
   * @return the tool tip
   */
  @Override
  public String getTabTitleToolTip() {
    return "Write and execute Python scripts";
  }

  /**
   * Set the log to use
   *
   * @param newLog the Logger that will now get info messages
   */
  @Override
  public void setLog(Logger newLog) {
    if (newLog instanceof JComponent && m_pythonPanel != null) {
      m_pythonPanel.setLogger(newLog);
    }
  }
}
