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
 *    Explorer3DPanel.java
 *    Copyright (C) 2010 Pentaho Corporation
 *
 */

package weka.gui.explorer;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileReader;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import weka.core.Instances;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.gui.visualize.Visualize3D;

/**
 * Explorer plugin class that provides a 3D scatter plot visualization
 * as a separate tab in the Explorer. Requires that the Java 3D system
 * is installed.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class Explorer3DPanel extends JPanel implements ExplorerPanel {
  
  /** For serialization */
  private static final long serialVersionUID = -5925569550210900854L;

  /** Reference to the Explorer object */
  protected Explorer m_explorer;
  
  /** The actual Visualize3D panel that does the work */
  protected Visualize3D m_vis = new Visualize3D();

  /** 
   * Whether the actual visualzation panel has been added to this panel or not. We
   * have to actually remove the vis panel from us whenever the user switches to 
   * another tab in order to overcome a bug with Java 3D with Oracle Java 1.7 on the 
   * Mac where the vis panel remains visible (and obscures) the other tab's contents
   */
  protected boolean m_visAdded = false;
  
  public Explorer3DPanel() {
    setLayout(new BorderLayout());
    //    add(m_vis, BorderLayout.CENTER);
  }

  /**
   * returns the parent Explorer frame
   * 
   * @return          the parent
   */
  public Explorer getExplorer() {
    return m_explorer;
  }

  /**
   * Returns the title for the tab in the Explorer
   * 
   * @return the title of this tab
   */
  public String getTabTitle() {
    return "Visualize 3D";
  }
  
  /**
   * Returns the tooltip for the tab in the Explorer
   * 
   * @return the tooltip of this tab
   */
  public String getTabTitleToolTip() {
    return "Visualize instances in a 3D scatter plot";
  }
  
  /**
   * Sets the Explorer to use as parent frame (used for sending notifications
   * about changes in the data)
   * 
   * @param parent    the parent frame
   */
  public void setExplorer(Explorer parent) { 
    m_explorer = parent;
    m_explorer.getTabbedPane().addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        if (m_explorer.getTabbedPane().getSelectedComponent() == 
          Explorer3DPanel.this) {
          add(m_vis, BorderLayout.CENTER);
          m_vis.updateDisplay();
          m_visAdded = true;
          Explorer3DPanel.this.revalidate();
          Explorer3DPanel.this.repaint();
        } else {
          if (m_visAdded) {
            remove(0);
            Explorer3DPanel.this.revalidate();
            Explorer3DPanel.this.repaint();
            m_visAdded = false;
          }
        }
      }
    });
  }

  /**
   * Tells the panel to use a new set of instances.
   *
   * @param inst a set of Instances
   */
  public void setInstances(Instances inst) {
    boolean display = 
      (m_explorer.getTabbedPane().getSelectedComponent() == this);
    m_vis.setInstances(inst, display);
  }
  
  /**
   * Tell this panel to free resources held by the Java 3D system.
   */
  public void freeResources() {
    m_vis.freeResources();
  }

  /**
   * Main method for testing this class
   * 
   * @param args
   */
  public static void main(String[] args) {
    try {
      Instances insts = new Instances(new BufferedReader(new FileReader(args[0])));
      
      final Explorer3DPanel vis = new Explorer3DPanel();
      vis.setInstances(insts);
      
      final JFrame frame = new JFrame("Visualize 3D");
      frame.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          vis.freeResources();
          frame.dispose();
          System.exit(1);
        }
      });
      frame.setSize(800, 600);
      frame.setContentPane(vis);
      frame.setVisible(true);
      
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
