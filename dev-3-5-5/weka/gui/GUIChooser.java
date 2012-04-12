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
 *    GUIChooser.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

import weka.core.Memory;
import weka.core.Version;
import weka.gui.explorer.Explorer;
import weka.gui.experiment.Experimenter;
import weka.gui.beans.KnowledgeFlow;
import weka.gui.beans.KnowledgeFlowApp;
import weka.gui.arffviewer.ArffViewer;

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
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;


/** 
 * The main class for the Weka GUIChooser. Lets the user choose
 * which GUI they want to run.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.27 $
 */
public class GUIChooser extends JFrame {

  /** Click to open the simplecli */
  protected Button m_SimpleBut = new Button("Simple CLI");

  /** Click to open the Explorer */
  protected Button m_ExplorerBut = new Button("Explorer");

  /** Click to open the Explorer */
  protected Button m_ExperimenterBut = new Button("Experimenter");

  /** Click to open the KnowledgeFlow */
  protected Button m_KnowledgeFlowBut = new Button("KnowledgeFlow");

  /** Click to open the ArffViewer */
  protected Button m_ArffViewerBut = new Button("ArffViewer");

  /** keeps track of the opened ArffViewer instancs */
  protected Vector m_ArffViewers = new Vector();

  /** Click to open the LogWindow */
  protected Button m_LogWindowBut = new Button("Log");

  /** The SimpleCLI */
  protected SimpleCLI m_SimpleCLI;

  /** The frame containing the explorer interface */
  protected JFrame m_ExplorerFrame;

  /** The frame containing the experiment interface */
  protected JFrame m_ExperimenterFrame;

  /** The frame containing the knowledge flow interface */
  protected JFrame m_KnowledgeFlowFrame;

  /** The frame of the LogWindow */
  protected static LogWindow m_LogWindow = new LogWindow();

  /** The weka image */
  Image m_weka = Toolkit.getDefaultToolkit().
    getImage(ClassLoader.getSystemResource("weka/gui/weka3.gif"));

  
  /**
   * Creates the experiment environment gui with no initial experiment
   */
  public GUIChooser() {

    super("Weka GUI Chooser");
    
    Image icon = Toolkit.getDefaultToolkit().
    getImage(ClassLoader.getSystemResource("weka/gui/weka_icon.gif"));
    setIconImage(icon);
    this.getContentPane().setLayout(new BorderLayout());
    JPanel wbuts = new JPanel();
    wbuts.setBorder(BorderFactory.createTitledBorder("GUI"));
    wbuts.setLayout(new GridLayout(3, 2));
    wbuts.add(m_SimpleBut);
    wbuts.add(m_ExplorerBut);
    wbuts.add(m_ExperimenterBut);
    wbuts.add(m_KnowledgeFlowBut);
    wbuts.add(m_ArffViewerBut);
    wbuts.add(m_LogWindowBut);
    this.getContentPane().add(wbuts, BorderLayout.SOUTH);
    
    JPanel wekaPan = new JPanel();
    wekaPan.setToolTipText("Weka, a native bird of New Zealand");
    ImageIcon wii = new ImageIcon(m_weka);
    JLabel wekaLab = new JLabel(wii);
    wekaPan.add(wekaLab);
    this.getContentPane().add(wekaPan, BorderLayout.CENTER);
    
    JPanel titlePan = new JPanel();
    titlePan.setLayout(new GridLayout(8,1));
    titlePan.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    titlePan.add(new JLabel("Waikato Environment for",
                            SwingConstants.CENTER));
    titlePan.add(new JLabel("Knowledge Analysis",
                            SwingConstants.CENTER));
    titlePan.add(new JLabel(""));
    titlePan.add(new JLabel("Version " + Version.VERSION,
                            SwingConstants.CENTER));
    titlePan.add(new JLabel(""));
    titlePan.add(new JLabel("(c) 1999 - 2007",
    SwingConstants.CENTER));
    titlePan.add(new JLabel("University of Waikato",
    SwingConstants.CENTER));
    titlePan.add(new JLabel("New Zealand",
    SwingConstants.CENTER));
    this.getContentPane().add(titlePan, BorderLayout.NORTH);
    
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
	  m_ExplorerFrame = new JFrame("Weka Explorer");
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

    KnowledgeFlowApp.addStartupListener(new weka.gui.beans.StartUpListener() {
        public void startUpComplete() {
          if (m_KnowledgeFlowFrame == null) {
            final KnowledgeFlowApp kna = KnowledgeFlowApp.getSingleton();
            m_KnowledgeFlowBut.setEnabled(false);
            m_KnowledgeFlowFrame = new JFrame("Weka KnowledgeFlow Environment");
            m_KnowledgeFlowFrame.getContentPane().setLayout(new BorderLayout());
            m_KnowledgeFlowFrame.getContentPane()
              .add(kna, BorderLayout.CENTER);
            m_KnowledgeFlowFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent w) {
                  kna.clearLayout();
                  m_KnowledgeFlowFrame.dispose();
                  m_KnowledgeFlowFrame = null;
                  m_KnowledgeFlowBut.setEnabled(true);
                  checkExit();
                }
              });
            m_KnowledgeFlowFrame.pack();
            m_KnowledgeFlowFrame.setSize(900, 600);
            m_KnowledgeFlowFrame.setVisible(true);
          }
        }
      });

    m_KnowledgeFlowBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        KnowledgeFlow.startApp();
      }
    });

    m_ArffViewerBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ArffViewer av = new ArffViewer();
        av.setVisible(true);
        m_ArffViewers.add(av);
      }
    });

    m_LogWindowBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_LogWindow.setVisible(true);
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
	&& (m_ExperimenterFrame == null)
	&& (m_KnowledgeFlowFrame == null)) {
      System.exit(0);
    }
  }

  /** variable for the GUIChooser class which would be set to null by the memory 
      monitoring thread to free up some memory if we running out of memory
   */
  private static GUIChooser m_chooser;

  /** for monitoring the Memory consumption */
  private static Memory m_Memory = new Memory(true);

  /**
   * Tests out the GUIChooser environment.
   *
   * @param args ignored.
   */
  public static void main(String [] args) {

    LookAndFeel.setLookAndFeel();
    
    try {

      // uncomment to disable the memory management:
      //m_Memory.setEnabled(false);

      m_chooser = new GUIChooser();
      m_chooser.setVisible(true);

      Thread memMonitor = new Thread() {
        public void run() {
          while(true) {
            try {
              //System.out.println("before sleeping");
              this.sleep(4000);
              
              System.gc();
              
              if (m_Memory.isOutOfMemory()) {
                // clean up
                m_chooser.dispose();
                if(m_chooser.m_ExperimenterFrame!=null) {
                  m_chooser.m_ExperimenterFrame.dispose();
                  m_chooser.m_ExperimenterFrame =null;
                }
                if(m_chooser.m_ExplorerFrame!=null) {
                  m_chooser.m_ExplorerFrame.dispose();
                  m_chooser.m_ExplorerFrame = null;
                }
                if(m_chooser.m_KnowledgeFlowFrame!=null) {
                  m_chooser.m_KnowledgeFlowFrame.dispose();
                  m_chooser.m_KnowledgeFlowFrame = null;
                }
                if(m_chooser.m_SimpleCLI!=null) {
                  m_chooser.m_SimpleCLI.dispose();
                  m_chooser.m_SimpleCLI = null;
                }
                if (m_chooser.m_ArffViewers.size() > 0) {
                  for (int i = 0; i < m_chooser.m_ArffViewers.size(); i++) {
                    ArffViewer av = (ArffViewer) 
                                        m_chooser.m_ArffViewers.get(i);
                    av.dispose();
                  }
                  m_chooser.m_ArffViewers.clear();
                }
                m_chooser = null;
                System.gc();

                // stop threads
                m_Memory.stopThreads();

                // display error
                m_chooser.m_LogWindow.setVisible(true);
                m_chooser.m_LogWindow.toFront();
                System.err.println("\ndisplayed message:");
                m_Memory.showOutOfMemory();
                System.err.println("\nexiting...");
                System.exit(-1);
              }
            } 
            catch(InterruptedException ex) { 
              ex.printStackTrace(); 
            }
          }
        }
      };

      memMonitor.setPriority(Thread.NORM_PRIORITY);
      memMonitor.start();    
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
