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
 *    Copyright (C) 2011 Pentaho Corporation
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileReader;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Icon;

import weka.core.Instances;
import weka.gui.visualize.Visualize3D;

/**
 * KnowledgeFlow perspective provides a 3D scatter plot visualization. 
 * Requires that the Java 3D system is installed.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class KF3DPerspective extends JPanel implements KnowledgeFlowApp.KFPerspective {

  /** The actual Visualize3D panel that does the work */
  protected Visualize3D m_vis = new Visualize3D();
  
  public KF3DPerspective() {
    setLayout(new BorderLayout());
    add(m_vis, BorderLayout.CENTER);
  }

  public String getPerspectiveTitle() {
    return "Visualize 3D";
  }

  public String getPerspectiveTipText() {
    return "3D scatter plot visualization";
  }

  public Icon getPerspectiveIcon() {
    java.awt.Image pic = null;
    java.net.URL imageURL = this.getClass().getClassLoader().
      getResource("weka/gui/beans/icons/scatterPlot3D.png");

    if (imageURL == null) {
    } else {
      pic = java.awt.Toolkit.getDefaultToolkit().
        getImage(imageURL);
    }
    return new javax.swing.ImageIcon(pic);
  }

  /**
   * Make this perspective the active (visible) one in the KF
   *
   * @param active true if this perspective is the currently active
   * one
   */
  public void setActive(boolean active) {
    if (active) {
      m_vis.updateDisplay();
    }
  }

  /**
   * Tell this perspective whether or not it is part of the users
   * perspectives toolbar in the KnowledgeFlow. If not part of
   * the current set of perspectives, then we can free some resources.
   *
   * @param loaded true if this perspective is part of the user-selected
   * perspectives in the KnowledgeFlow
   */
  public void setLoaded(boolean loaded) {
    if (!loaded) {
      freeResources();
    }
  }

  public void setMainKFPerspective(KnowledgeFlowApp.MainKFPerspective main) {
    // don't need this
  }

  public void setInstances(Instances inst) {
    m_vis.setInstances(inst);
  }

  public boolean acceptsInstances() {
    return true;
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
      
      final KF3DPerspective vis = new KF3DPerspective();
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
