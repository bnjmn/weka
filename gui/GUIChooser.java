/*
 *    GUIChooser.java
 *    Copyright (C) 1999 Len Trigg
 *
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
import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;;


/** 
 * The main class for the Weka GUIChooser. Lets the user choose
 * which GUI they want to run.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $
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

  /** The weka image */
  Image m_weka = Toolkit.getDefaultToolkit().
    getImage(ClassLoader.getSystemResource("weka/gui/weka3.gif"));

  
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
      Image icon = Toolkit.getDefaultToolkit().
	getImage(ClassLoader.getSystemResource("weka/gui/weka_icon.gif"));
      setIconImage(icon);
      setLayout(new BorderLayout());
      JPanel wbuts = new JPanel();
      wbuts.setBorder(BorderFactory.createTitledBorder("GUI"));
      wbuts.setLayout(new GridLayout(1, 3));
      wbuts.add(m_SimpleBut);
      wbuts.add(m_ExplorerBut);
      wbuts.add(m_ExperimenterBut);
      add(wbuts, BorderLayout.SOUTH);

      JPanel wekaPan = new JPanel();
      wekaPan.setToolTipText("Weka, a native bird of New Zealand");
      ImageIcon wii = new ImageIcon(m_weka);
      JLabel wekaLab = new JLabel(wii);
      wekaPan.add(wekaLab);
      add(wekaPan, BorderLayout.CENTER);

      JPanel titlePan = new JPanel();
      titlePan.setLayout(new GridLayout(6,1));
      titlePan.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
      titlePan.add(new JLabel("Waikato Environment for", 
			      SwingConstants.CENTER));
      titlePan.add(new JLabel("Knowledge Analysis", 
			      SwingConstants.CENTER));
      titlePan.add(new JLabel(""));
      titlePan.add(new JLabel("(c) 1999 - 2000", 
			      SwingConstants.CENTER));
      titlePan.add(new JLabel("University of Waikato", 
			      SwingConstants.CENTER));
      titlePan.add(new JLabel("New Zealand",
			      SwingConstants.CENTER));
      add(titlePan, BorderLayout.NORTH);
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
