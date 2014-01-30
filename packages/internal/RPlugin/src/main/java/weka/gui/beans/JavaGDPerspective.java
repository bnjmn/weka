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
 *    JavaGDPerspective.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;

import javax.swing.Icon;
import javax.swing.JPanel;

import weka.core.Instances;
import weka.core.JRILoader;
import weka.gui.beans.KnowledgeFlowApp.MainKFPerspective;

/**
 * A KnowledgeFlow perspective providing the r console and R JavaGD graphics
 * display.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class JavaGDPerspective extends JPanel implements
  KnowledgeFlowApp.KFPerspective {

  static {
    try {
      JRILoader.load();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /** For serialization */
  private static final long serialVersionUID = -4809148083291668900L;

  /** The console panel */
  protected JavaGDConsolePanel m_consoleP;

  /**
   * Constructor
   */
  public JavaGDPerspective() {
    setLayout(new BorderLayout());

    m_consoleP = new JavaGDConsolePanel(true);
    add(m_consoleP, BorderLayout.CENTER);
  }

  /**
   * Set instances to use - not used by this perspective
   * 
   * @param insts the Instances
   * @throws Exception if a problem occurs
   */
  @Override
  public void setInstances(Instances insts) throws Exception {
    m_consoleP.pushInstancesToR(insts);
  }

  /**
   * Returns true - this panel pushes incoming instances into R as a data frame
   * called "rdata"
   * 
   * @return true - this perspective accepts instances
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
    return "R Console/visualize";
  }

  /**
   * Get the perspective's tool tip
   * 
   * @return the perspective's tool tip
   */
  @Override
  public String getPerspectiveTipText() {
    return "R console and visualizations using JavaGD";
  }

  /**
   * Get the perspective's icon
   * 
   * @return the perspective's icon
   */
  @Override
  public Icon getPerspectiveIcon() {
    java.awt.Image pic = null;
    java.net.URL imageURL = this.getClass().getClassLoader()
      .getResource("weka/gui/beans/icons/Rlogo_small.png");

    if (imageURL == null) {
    } else {
      pic = java.awt.Toolkit.getDefaultToolkit().getImage(imageURL);
    }
    return new javax.swing.ImageIcon(pic);
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
  public void setMainKFPerspective(MainKFPerspective main) {
  }
}
