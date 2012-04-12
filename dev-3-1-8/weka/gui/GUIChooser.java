/*
 *    GUIChooser.java
 *    Copyright (C) 1999 Len Trigg
 *
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

package weka.gui;

import weka.gui.explorer.Explorer;
import weka.gui.experiment.Experimenter;

import java.awt.Panel;
import java.awt.Button;
import java.awt.GridLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;


/** 
 * The main class for the Weka GUIChooser. Lets the user choose
 * which GUI they want to run.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class GUIChooser extends Frame {

  /** Click to open the simplecli */
  protected Button m_SimpleBut = new Button("Simple CLI");

  /** Click to open the Explorer */
  protected Button m_ExplorerBut = new Button("Explorer");

  /** Click to open the Explorer */
  protected Button m_ExperimenterBut = new Button("Experimenter");

  /** The SimpleCLI */
  protected SimpleCLI m_SimpleCLI;

  /** The frame containing the explorer interface */
  protected JFrame m_ExplorerFrame;

  /** The frame containing the experiment interface */
  protected JFrame m_ExperimenterFrame;

  
  /**
   * Creates the experiment environment gui with no initial experiment
   */
  public GUIChooser() {

    super("Weka GUI Chooser");
    boolean haveSwing = true;
    try {
      Class c = Class.forName("javax.swing.JFrame");
    } catch (Exception ex) {
      haveSwing = false;
    }
    if (!haveSwing) {
      System.err.println("Swing is not installed");
      // Pop up dialog saying you get extra if you have swing
      setLayout(new GridLayout(2, 1));
      add(m_SimpleBut);
      add(new Label("Swing is not installed"));
    } else {
      setLayout(new GridLayout(3, 1));
      add(m_SimpleBut);
      add(m_ExplorerBut);
      add(m_ExperimenterBut);
    }

    m_SimpleBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	if (m_SimpleCLI == null) {
	  m_SimpleBut.setEnabled(false);
	  try {
	    m_SimpleCLI = new SimpleCLI();
	  } catch (Exception ex) {
	    throw new Error("Couldn't start SimpleCLI!");
	  }
	  m_SimpleCLI.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent w) {
	      m_SimpleCLI.dispose();
	      m_SimpleCLI = null;
	      m_SimpleBut.setEnabled(true);
	      checkExit();
	    }
	  });
	  m_SimpleCLI.setVisible(true);
	}
      }
    });

    m_ExplorerBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	if (m_ExplorerFrame == null) {
	  m_ExplorerBut.setEnabled(false);
	  m_ExplorerFrame = new JFrame("Weka Knowledge Explorer");
	  m_ExplorerFrame.getContentPane().setLayout(new BorderLayout());
	  m_ExplorerFrame.getContentPane()
	    .add(new Explorer(), BorderLayout.CENTER);
	  m_ExplorerFrame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent w) {
	      m_ExplorerFrame.dispose();
	      m_ExplorerFrame = null;
	      m_ExplorerBut.setEnabled(true);
	      checkExit();
	    }
	  });
	  m_ExplorerFrame.pack();
	  m_ExplorerFrame.setSize(800, 600);
	  m_ExplorerFrame.setVisible(true);
	}
      }
    });

    m_ExperimenterBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	if (m_ExperimenterFrame == null) {
	  m_ExperimenterBut.setEnabled(false);
	  m_ExperimenterFrame = new JFrame("Weka Experiment Environment");
	  m_ExperimenterFrame.getContentPane().setLayout(new BorderLayout());
	  m_ExperimenterFrame.getContentPane()
	    .add(new Experimenter(false), BorderLayout.CENTER);
	  m_ExperimenterFrame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent w) {
	      m_ExperimenterFrame.dispose();
	      m_ExperimenterFrame = null;
	      m_ExperimenterBut.setEnabled(true);
	      checkExit();
	    }
	  });
	  m_ExperimenterFrame.pack();
	  m_ExperimenterFrame.setSize(800, 600);
	  m_ExperimenterFrame.setVisible(true);
	}
      }
    });

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent w) {
	dispose();
	checkExit();
      }
    });
    pack();
  }

  /**
   * Kills the JVM if all windows have been closed.
   */
  private void checkExit() {

    if (!isVisible()
	&& (m_SimpleCLI == null)
	&& (m_ExplorerFrame == null)
	&& (m_ExperimenterFrame == null)) {
      System.exit(0);
    }
  }
  
  /**
   * Tests out the GUIChooser environment.
   *
   * @param args ignored.
   */
  public static void main(String [] args) {

    try {
      GUIChooser c = new GUIChooser();
      c.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
