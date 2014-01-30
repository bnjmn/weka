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
 *    ExplorerRConsolePanel.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.explorer;

import java.awt.BorderLayout;
import java.lang.reflect.Method;

import javax.swing.JComponent;
import javax.swing.JPanel;

import weka.core.Instances;
import weka.core.JRILoader;
import weka.gui.Logger;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.gui.explorer.Explorer.LogHandler;

/**
 * Explorer plugin providing the r console and JavaGD graphics display.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ExplorerRConsolePanel extends JPanel implements ExplorerPanel,
  LogHandler {

  static {
    try {
      JRILoader.load();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /** The actual console panel */
  protected Object m_rConsole;

  // protected JavaGDConsolePanel m_rConsole;

  /**
   * Construct a new ExplorerRConsolePanel
   */
  public ExplorerRConsolePanel() {
    // m_rConsole = new JavaGDConsolePanel(false);

    setLayout(new BorderLayout());

    // use reflection here because JavaGDConsolePanel implements JavaGDListener,
    // which
    // is one of the key classes who's byte code must be loaded by the root
    // class loader.
    try {
      Class<?> p = Class.forName("weka.gui.beans.JavaGDConsolePanel");
      m_rConsole = p.newInstance();
      add((JComponent) m_rConsole, BorderLayout.CENTER);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    // add(new JavaGDConsolePanel(false), BorderLayout.CENTER);
  }

  /**
   * Set the log - unused by this panel
   * 
   * @param newLog the log
   */
  @Override
  public void setLog(final Logger newLog) {
    if (newLog instanceof JComponent && m_rConsole != null) {
      try {
        Method m = m_rConsole.getClass().getDeclaredMethod("setLogger",
          new Class[] { Logger.class });

        m.invoke(m_rConsole, new Object[] { newLog });
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      // m_rConsole.setLogger(newLog);
    }
  }

  /**
   * Set the parent Explorer - unused (this panel does not keep a reference to
   * the Explorer)
   * 
   * @param parent the parent Explorer
   */
  @Override
  public void setExplorer(Explorer parent) {
  }

  /**
   * Returns null - this panel does not keep a reference to the Explorer
   * 
   * @return null
   */
  @Override
  public Explorer getExplorer() {
    return null;
  }

  /**
   * Set the working instances for the panel - unused
   * 
   * @param inst the Instances to use
   */
  @Override
  public void setInstances(Instances inst) {
    try {
      Method m = m_rConsole.getClass().getDeclaredMethod("pushInstancesToR",
        new Class[] { Instances.class });

      m.invoke(m_rConsole, new Object[] { inst });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Get the title for this tab
   * 
   * @return the title for this tab
   */
  @Override
  public String getTabTitle() {
    return "RConsole";
  }

  /**
   * Get the tool tip for this tab
   * 
   * @return the tool tip for this tab
   */
  @Override
  public String getTabTitleToolTip() {
    return "Execute R commands";
  }
}
