/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    WekaTaskMonitor.java
 *    Copyright (C) 2000 Mark Hall
 *
 */


package weka.gui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.image.*;

/** 
 * This panel records the number of weka tasks running and displays a
 * simple bird animation while their are active tasks
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
public class WekaTaskMonitor extends JPanel implements TaskLogger {

  /** The number of running weka threads */
  private int m_ActiveTasks = 0;

  /** The label for displaying info */
  private JLabel m_MonitorLabel;

  /** The icon for the stationary bird */
  private ImageIcon m_iconStationary;

  /** The icon for the animated bird */
  private ImageIcon m_iconAnimated;

  /** True if their are active tasks */
  private boolean m_animating = false;
  
  /**
   * Constructor
   */
  public WekaTaskMonitor() {
    Image pic = Toolkit.getDefaultToolkit().
      getImage(ClassLoader.getSystemResource("weka/gui/weka_stationary.gif"));
    Image pic2 = Toolkit.getDefaultToolkit().
      getImage(ClassLoader.getSystemResource("weka/gui/weka_animated.gif"));

    m_iconStationary = new ImageIcon(pic); 
    m_iconAnimated = new ImageIcon(pic2);
    
    m_MonitorLabel = new JLabel(" x "+m_ActiveTasks,m_iconStationary,SwingConstants.CENTER);
    /*
    setBorder(BorderFactory.createCompoundBorder(
  	      BorderFactory.createTitledBorder("Weka Tasks"),
  	      BorderFactory.createEmptyBorder(0, 5, 5, 5)
  	      ));
    */
    setLayout(new BorderLayout());
    Dimension d = m_MonitorLabel.getPreferredSize();
    m_MonitorLabel.setPreferredSize(new Dimension(d.width+15,d.height));
    m_MonitorLabel.setMinimumSize(new Dimension(d.width+15,d.height));
    add(m_MonitorLabel, BorderLayout.CENTER);
    

  }

  /**
   * Tells the panel that a new task has been started
   */
  public void taskStarted() {
    m_ActiveTasks++;
    updateMonitor();
  }

  /**
   * Tells the panel that a task has completed
   */
  public void taskFinished() {
    m_ActiveTasks--;
    if (m_ActiveTasks < 0) {
      m_ActiveTasks = 0;
    }
    updateMonitor();
  }

  /**
   * Updates the number of running tasks an the status of the bird
   * image
   */
  private void updateMonitor() {
    m_MonitorLabel.setText(" x "+m_ActiveTasks);
    if (m_ActiveTasks > 0 && !m_animating) {
      m_MonitorLabel.setIcon(m_iconAnimated);
      m_animating = true;
    }

    if (m_ActiveTasks == 0 && m_animating) {
      m_MonitorLabel.setIcon(m_iconStationary);
      m_animating = false;
    }
  }

  /**
   * Main method for testing this class
   */
  public static void main(String [] args) {
    
    try {
      final javax.swing.JFrame jf = new javax.swing.JFrame();
      jf.getContentPane().setLayout(new BorderLayout());
      final WekaTaskMonitor tm = new WekaTaskMonitor();
      tm.setBorder(BorderFactory.createCompoundBorder(
  	           BorderFactory.createTitledBorder("Weka Tasks"),
  	           BorderFactory.createEmptyBorder(0, 5, 5, 5)
  	           ));
      jf.getContentPane().add(tm, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
      tm.taskStarted();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
