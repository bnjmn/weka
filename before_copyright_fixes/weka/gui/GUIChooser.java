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

import weka.classifiers.EnsembleLibrary;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Copyright;
import weka.core.Instances;
import weka.core.Memory;
import weka.core.SystemInfo;
import weka.core.Utils;
import weka.core.Version;
import weka.gui.arffviewer.ArffViewer;
import weka.gui.beans.KnowledgeFlow;
import weka.gui.beans.KnowledgeFlowApp;
import weka.gui.boundaryvisualizer.BoundaryVisualizer;
import weka.gui.experiment.Experimenter;
import weka.gui.explorer.Explorer;
import weka.gui.graphvisualizer.GraphVisualizer;
import weka.gui.sql.SqlViewer;
import weka.gui.treevisualizer.Node;
import weka.gui.treevisualizer.NodePlace;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeBuild;
import weka.gui.treevisualizer.TreeVisualizer;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;
import weka.gui.visualize.VisualizePanel;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;

/** 
 * The main class for the Weka GUIChooser. Lets the user choose
 * which GUI they want to run.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.31 $
 */
public class GUIChooser
  extends JFrame {

  /** for serialization */
  private static final long serialVersionUID = 9001529425230247914L;

  /** the GUIChooser itself */
  protected GUIChooser m_Self;
  
  // Applications

  /** the panel for the application buttons */
  protected JPanel m_PanelApplications = new JPanel();
  
  /** Click to open the Explorer */
  protected Button m_ExplorerBut = new Button("Explorer");

  /** The frame containing the explorer interface */
  protected JFrame m_ExplorerFrame;

  /** Click to open the Explorer */
  protected Button m_ExperimenterBut = new Button("Experimenter");

  /** The frame containing the experiment interface */
  protected JFrame m_ExperimenterFrame;

  /** Click to open the KnowledgeFlow */
  protected Button m_KnowledgeFlowBut = new Button("KnowledgeFlow");

  /** The frame containing the knowledge flow interface */
  protected JFrame m_KnowledgeFlowFrame;

  /** Click to open the simplecli */
  protected Button m_SimpleBut = new Button("Simple CLI");
  
  /** The SimpleCLI */
  protected SimpleCLI m_SimpleCLI;

  // Tools

  /** the panel for the tool buttons */
  protected JPanel m_PanelTools = new JPanel();
  
  /** Click to open the ArffViewer */
  protected Button m_ArffViewerBut = new Button("ArffViewer");

  /** keeps track of the opened ArffViewer instancs */
  protected Vector m_ArffViewers = new Vector();
  
  /** Click to open the SqlViewer */
  protected Button m_SqlViewerBut = new Button("SqlViewer");

  /** The frame containing the SqlViewer */
  protected JFrame m_SqlViewerFrame;
  
  /** Click to open the EnsembleLibrary */
  protected Button m_EnsembleLibraryBut = new Button("Ensemble Library");

  /** The frame containing the ensemble library interface */
  protected JFrame m_EnsembleLibraryFrame;

  // Visualization

  /** the panel for the visualization buttons */
  protected JPanel m_PanelVisualization = new JPanel();
  
  /** Click to open the Plot visualization */
  protected Button m_PlotBut = new Button("Plot");

  /** keeps track of the opened plots */
  protected Vector m_Plots = new Vector();
  
  /** Click to open the ROC visualization */
  protected Button m_ROCBut = new Button("ROC");

  /** keeps track of the opened ROCs */
  protected Vector m_ROCs = new Vector();
  
  /** Click to open the tree visualizer */
  protected Button m_TreeVisualizerBut = new Button("TreeVisualizer");

  /** keeps track of the opened tree visualizer instancs */
  protected Vector m_TreeVisualizers = new Vector();
  
  /** Click to open the graph visualizer */
  protected Button m_GraphVisualizerBut = new Button("GraphVisualizer");

  /** keeps track of the opened graph visualizer instancs */
  protected Vector m_GraphVisualizers = new Vector();
  
  /** Click to open the boundary visualizer */
  protected Button m_BoundaryVisualizerBut = new Button("BoundaryVisualizer");

  /** The frame containing the boundary visualizer */
  protected JFrame m_BoundaryVisualizerFrame;
  
  // Help

  /** the panel for the help buttons */
  protected JPanel m_PanelHelp = new JPanel();
  
  /** Click to open the Weka homepage */
  protected Button m_HomepageBut = new Button("Homepage");
  
  /** Click to open the online documentation */
  protected Button m_WekaDocBut = new Button("Online doc");
  
  /** Click to open WekaWiki */
  protected Button m_WekaWikiBut = new Button("HOWTOs, ...");
  
  /** Click to open the Sourceforge homepage */
  protected Button m_SourceforgeBut = new Button("Sourceforge");
  
  /** Click to open the System info */
  protected Button m_SystemInfoBut = new Button("SystemInfo");

  /** The frame containing the system info */
  protected JFrame m_SystemInfoFrame;
  
  // Other

  /** the panel for the other buttons */
  protected JPanel m_PanelOther = new JPanel();

  /** Click to open the LogWindow */
  protected Button m_LogWindowBut = new Button("Log");
  
  /** Click to exit Weka */
  protected Button m_ExitBut = new Button("Exit");

  /** The frame of the LogWindow */
  protected static LogWindow m_LogWindow = new LogWindow();

  /** The weka image */
  Image m_weka = Toolkit.getDefaultToolkit().
    getImage(ClassLoader.getSystemResource("weka/gui/weka3.gif"));

  /** filechooser for the TreeVisualizer */
  protected JFileChooser m_FileChooserTreeVisualizer = new JFileChooser(new File(System.getProperty("user.dir")));

  /** filechooser for the GraphVisualizer */
  protected JFileChooser m_FileChooserGraphVisualizer = new JFileChooser(new File(System.getProperty("user.dir")));

  /** filechooser for Plots */
  protected JFileChooser m_FileChooserPlot = new JFileChooser(new File(System.getProperty("user.dir")));

  /** filechooser for ROC curves */
  protected JFileChooser m_FileChooserROC = new JFileChooser(new File(System.getProperty("user.dir")));
  
  /** the icon for the frames */
  protected Image m_Icon;
  
  /**
   * Creates the experiment environment gui with no initial experiment
   */
  public GUIChooser() {

    super("Weka GUI Chooser");
    
    m_Self = this;

    // filechoosers
    m_FileChooserGraphVisualizer.addChoosableFileFilter(
	  new ExtensionFileFilter(".bif", "BIF Files (*.bif)"));
    m_FileChooserGraphVisualizer.addChoosableFileFilter(
	  new ExtensionFileFilter(".xml", "XML Files (*.xml)"));

    m_FileChooserPlot.addChoosableFileFilter(
	  new ExtensionFileFilter(
	      Instances.FILE_EXTENSION,
	      "ARFF Files (*" + Instances.FILE_EXTENSION + ")"));
    m_FileChooserPlot.setMultiSelectionEnabled(true);
    
    m_FileChooserROC.addChoosableFileFilter(
	  new ExtensionFileFilter(
	      Instances.FILE_EXTENSION,
	      "ARFF Files (*" + Instances.FILE_EXTENSION + ")"));

    // general layout
    m_Icon = Toolkit.getDefaultToolkit().getImage(
	ClassLoader.getSystemResource("weka/gui/weka_icon.gif"));
    setIconImage(m_Icon);
    this.getContentPane().setLayout(new BorderLayout());
    JPanel panels = new JPanel();
    panels.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();

    c.fill = GridBagConstraints.BOTH;
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.weightx = 2;
    c.weighty = 2;
    panels.add(m_PanelApplications, c);
    
    c.weightx = 2;
    c.weighty = 2;
    panels.add(m_PanelTools, c);
    
    c.weightx = 2;
    c.weighty = 3;
    panels.add(m_PanelVisualization, c);
    
    c.weightx = 2;
    c.weighty = 3;
    panels.add(m_PanelHelp, c);

    c.weightx = 2;
    c.weighty = 1;
    panels.add(m_PanelOther, c);
    
    getContentPane().add(panels, BorderLayout.SOUTH);

    // applications
    m_PanelApplications.setBorder(BorderFactory.createTitledBorder("Applications"));
    m_PanelApplications.setLayout(new GridLayout(2, 2));
    m_PanelApplications.add(m_ExplorerBut);
    m_PanelApplications.add(m_ExperimenterBut);
    m_PanelApplications.add(m_KnowledgeFlowBut);
    m_PanelApplications.add(m_SimpleBut);
    
    // tools
    m_PanelTools.setBorder(BorderFactory.createTitledBorder("Tools"));
    m_PanelTools.setLayout(new GridLayout(2, 2));
    m_PanelTools.add(m_ArffViewerBut);
    m_PanelTools.add(m_SqlViewerBut);
    m_PanelTools.add(m_EnsembleLibraryBut);
    m_PanelTools.add(new JLabel(""));
    
    // visualization
    m_PanelVisualization.setBorder(BorderFactory.createTitledBorder("Visualization"));
    m_PanelVisualization.setLayout(new GridLayout(3, 2));
    m_PanelVisualization.add(m_PlotBut);
    m_PanelVisualization.add(m_ROCBut);
    m_PanelVisualization.add(m_TreeVisualizerBut);
    m_PanelVisualization.add(m_GraphVisualizerBut);
    m_PanelVisualization.add(m_BoundaryVisualizerBut);
    m_PanelVisualization.add(new JLabel(""));
    
    // help
    m_PanelHelp.setBorder(BorderFactory.createTitledBorder("Help"));
    m_PanelHelp.setLayout(new GridLayout(3, 2));
    m_PanelHelp.add(m_HomepageBut);
    m_PanelHelp.add(m_WekaDocBut);
    m_PanelHelp.add(m_WekaWikiBut);
    m_PanelHelp.add(m_SourceforgeBut);
    m_PanelHelp.add(m_SystemInfoBut);
    m_PanelHelp.add(new JLabel(""));
    
    // other
    m_PanelOther.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_PanelOther.setLayout(new GridLayout(1, 2));
    m_PanelOther.add(m_LogWindowBut);
    m_PanelOther.add(m_ExitBut);
    
    JPanel wekaPan = new JPanel();
    wekaPan.setToolTipText("Weka, a native bird of New Zealand");
    ImageIcon wii = new ImageIcon(m_weka);
    JLabel wekaLab = new JLabel(wii);
    wekaPan.add(wekaLab);
    this.getContentPane().add(wekaPan, BorderLayout.CENTER);
    
    JPanel titlePan = new JPanel();
    titlePan.setLayout(new GridLayout(8,1));
    titlePan.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    titlePan.add(new JLabel("Waikato Environment for", SwingConstants.CENTER));
    titlePan.add(new JLabel("Knowledge Analysis", SwingConstants.CENTER));
    titlePan.add(new JLabel(""));
    titlePan.add(new JLabel("Version " + Version.VERSION, SwingConstants.CENTER));
    titlePan.add(new JLabel(""));
    titlePan.add(new JLabel("(c) " + Copyright.getFromYear() + " - " + Copyright.getToYear(),
	SwingConstants.CENTER));
    titlePan.add(new JLabel(Copyright.getOwner(),
	SwingConstants.CENTER));
    titlePan.add(new JLabel(Copyright.getAddress(),
	SwingConstants.CENTER));
    this.getContentPane().add(titlePan, BorderLayout.NORTH);
    
    // applications

    m_ExplorerBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	if (m_ExplorerFrame == null) {
	  m_ExplorerBut.setEnabled(false);
	  m_ExplorerFrame = new JFrame("Weka Explorer");
	  m_ExplorerFrame.setIconImage(m_Icon);
	  m_ExplorerFrame.getContentPane().setLayout(new BorderLayout());
	  m_ExplorerFrame.getContentPane().add(new Explorer(), BorderLayout.CENTER);
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
	  m_ExperimenterFrame.setIconImage(m_Icon);
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
            m_KnowledgeFlowFrame.setIconImage(m_Icon);
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

    m_SimpleBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (m_SimpleCLI == null) {
          m_SimpleBut.setEnabled(false);
          try {
            m_SimpleCLI = new SimpleCLI();
            m_SimpleCLI.setIconImage(m_Icon);
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

    // tools
    
    m_ArffViewerBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final ArffViewer av = new ArffViewer();
        av.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent w) {
            m_ArffViewers.remove(av);
            checkExit();
          }
        });
        av.setVisible(true);
        m_ArffViewers.add(av);
      }
    });

    m_SqlViewerBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	if (m_SqlViewerFrame == null) {
	  m_SqlViewerBut.setEnabled(false);
	  final SqlViewer sql = new SqlViewer(null);
	  m_SqlViewerFrame = new JFrame("SqlViewer");
	  m_SqlViewerFrame.setIconImage(m_Icon);
	  m_SqlViewerFrame.getContentPane().setLayout(new BorderLayout());
	  m_SqlViewerFrame.getContentPane().add(sql, BorderLayout.CENTER);
	  m_SqlViewerFrame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent w) {
	      sql.saveSize();
	      m_SqlViewerFrame.dispose();
	      m_SqlViewerFrame = null;
	      m_SqlViewerBut.setEnabled(true);
	      checkExit();
	    }
	  });
	  m_SqlViewerFrame.pack();
	  m_SqlViewerFrame.setVisible(true);
	}
      }
    });

    m_EnsembleLibraryBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	if (m_EnsembleLibraryFrame == null) {
	  m_EnsembleLibraryBut.setEnabled(false);
	  m_EnsembleLibraryFrame = new JFrame("EnsembleLibrary");
	  m_EnsembleLibraryFrame.setIconImage(m_Icon);
	  m_EnsembleLibraryFrame.getContentPane().setLayout(new BorderLayout());
	  EnsembleLibrary value = new EnsembleLibrary();
	  EnsembleLibraryEditor libraryEditor = new EnsembleLibraryEditor();
	  libraryEditor.setValue(value);
	  m_EnsembleLibraryFrame.getContentPane().add(libraryEditor.getCustomEditor(), BorderLayout.CENTER);
	  m_EnsembleLibraryFrame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent w) {
	      m_EnsembleLibraryFrame.dispose();
	      m_EnsembleLibraryFrame = null;
	      m_EnsembleLibraryBut.setEnabled(true);
	      checkExit();
	    }
	  });
	  m_EnsembleLibraryFrame.pack();
	  m_EnsembleLibraryFrame.setSize(800, 600);
	  m_EnsembleLibraryFrame.setVisible(true);
	}
      }
    });

    // visualization
    
    m_PlotBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	// choose file
	int retVal = m_FileChooserPlot.showOpenDialog(m_Self);
	if (retVal != JFileChooser.APPROVE_OPTION)
	  return;

	// build plot
	VisualizePanel panel = new VisualizePanel();
	String filenames = "";
	File[] files = m_FileChooserPlot.getSelectedFiles();
	for (int j = 0; j < files.length; j++) {
	  String filename = files[j].getAbsolutePath();
	  if (j > 0)
	    filenames += ", ";
	  filenames += filename;
	  System.err.println("Loading instances from " + filename);
	  try {
	    Reader r = new java.io.BufferedReader(new FileReader(filename));
	    Instances i = new Instances(r);
	    i.setClassIndex(i.numAttributes()-1);
	    PlotData2D pd1 = new PlotData2D(i);

	    if (j == 0) {
	      pd1.setPlotName("Master plot");
	      panel.setMasterPlot(pd1);
	    } else {
	      pd1.setPlotName("Plot "+(j+1));
	      pd1.m_useCustomColour = true;
	      pd1.m_customColour = (j % 2 == 0) ? Color.red : Color.blue; 
	      panel.addPlot(pd1);
	    }
	  }
	  catch (Exception ex) {
	    ex.printStackTrace();
	    JOptionPane.showMessageDialog(
		m_Self, "Error loading file '" + files[j] + "':\n" + ex.getMessage());
	    return;
	  }
	}

	// create frame
	final JFrame frame = new JFrame("Plot - " + filenames);
	frame.setIconImage(m_Icon);
	frame.getContentPane().setLayout(new BorderLayout());
	frame.getContentPane().add(panel, BorderLayout.CENTER);
	frame.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent e) {
	    m_Plots.remove(frame);
	    frame.dispose();
	    checkExit();
	  }
	});
	frame.pack();
	frame.setSize(800, 600);
	frame.setVisible(true);
	m_Plots.add(frame);
      }
    });

    m_ROCBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	// choose file
	int retVal = m_FileChooserROC.showOpenDialog(m_Self);
	if (retVal != JFileChooser.APPROVE_OPTION)
	  return;

	// create plot
	String filename  = m_FileChooserROC.getSelectedFile().getAbsolutePath();
	Instances result = null;
	try {
	  result = new Instances(new BufferedReader(new FileReader(filename)));
	}
	catch (Exception ex) {
	  ex.printStackTrace();
	  JOptionPane.showMessageDialog(
	      m_Self, "Error loading file '" + filename + "':\n" + ex.getMessage());
	  return;
	}
	result.setClassIndex(result.numAttributes() - 1);
	ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
	vmc.setROCString("(Area under ROC = " + 
	    Utils.doubleToString(ThresholdCurve.getROCArea(result), 4) + ")");
	vmc.setName(result.relationName());
	PlotData2D tempd = new PlotData2D(result);
	tempd.setPlotName(result.relationName());
	tempd.addInstanceNumberAttribute();
	try {
	  vmc.addPlot(tempd);
	}
	catch (Exception ex) {
	  ex.printStackTrace();
	  JOptionPane.showMessageDialog(
	      m_Self, "Error adding plot:\n" + ex.getMessage());
	  return;
	}

	final JFrame frame = new JFrame("ROC - " + filename);
	frame.setIconImage(m_Icon);
	frame.getContentPane().setLayout(new BorderLayout());
	frame.getContentPane().add(vmc, BorderLayout.CENTER);
	frame.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent e) {
	    m_ROCs.remove(frame);
	    frame.dispose();
	    checkExit();
	  }
	});
	frame.pack();
	frame.setSize(800, 600);
	frame.setVisible(true);
	m_ROCs.add(frame);
      }
    });

    m_TreeVisualizerBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	// choose file
	int retVal = m_FileChooserTreeVisualizer.showOpenDialog(m_Self);
	if (retVal != JFileChooser.APPROVE_OPTION)
	  return;

	// build tree
	String filename = m_FileChooserTreeVisualizer.getSelectedFile().getAbsolutePath();
	TreeBuild builder = new TreeBuild();
	Node top = null;
	NodePlace arrange = new PlaceNode2();
	try {
	  top = builder.create(new FileReader(filename));
	}
	catch (Exception ex) {
	  ex.printStackTrace();
	  JOptionPane.showMessageDialog(
	      m_Self, "Error loading file '" + filename + "':\n" + ex.getMessage());
	  return;
	}

	// create frame
	final JFrame frame = new JFrame("TreeVisualizer - " + filename);
	frame.setIconImage(m_Icon);
	frame.getContentPane().setLayout(new BorderLayout());
	frame.getContentPane().add(new TreeVisualizer(null, top, arrange), BorderLayout.CENTER);
	frame.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent e) {
	    m_TreeVisualizers.remove(frame);
	    frame.dispose();
	    checkExit();
	  }
	});
	frame.pack();
	frame.setSize(800, 600);
	frame.setVisible(true);
	m_TreeVisualizers.add(frame);
      }
    });

    m_GraphVisualizerBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	// choose file
	int retVal = m_FileChooserGraphVisualizer.showOpenDialog(m_Self);
	if (retVal != JFileChooser.APPROVE_OPTION)
	  return;

	// build graph
	String filename = m_FileChooserGraphVisualizer.getSelectedFile().getAbsolutePath();
	GraphVisualizer panel = new GraphVisualizer();
	try{
	  if (    filename.toLowerCase().endsWith(".xml") 
	      || filename.toLowerCase().endsWith(".bif") ) {
	    panel.readBIF(new FileInputStream(filename));
	  }
	  else {
	    panel.readDOT(new FileReader(filename));
	  }
	}
	catch (Exception ex) {
	  ex.printStackTrace();
	  JOptionPane.showMessageDialog(
	      m_Self, "Error loading file '" + filename + "':\n" + ex.getMessage());
	  return;
	}

	// create frame
	final JFrame frame = new JFrame("GraphVisualizer - " + filename);
	frame.setIconImage(m_Icon);
	frame.getContentPane().setLayout(new BorderLayout());
	frame.getContentPane().add(panel, BorderLayout.CENTER);
	frame.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent e) {
	    m_GraphVisualizers.remove(frame);
	    frame.dispose();
	    checkExit();
	  }
	});
	frame.pack();
	frame.setSize(800, 600);
	frame.setVisible(true);
	m_GraphVisualizers.add(frame);
      }
    });

    m_BoundaryVisualizerBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	if (m_BoundaryVisualizerFrame == null) {
	  m_BoundaryVisualizerBut.setEnabled(false);
	  m_BoundaryVisualizerFrame = new JFrame("BoundaryVisualizer");
	  m_BoundaryVisualizerFrame.setIconImage(m_Icon);
	  m_BoundaryVisualizerFrame.getContentPane().setLayout(new BorderLayout());
	  m_BoundaryVisualizerFrame.getContentPane().add(new BoundaryVisualizer(), BorderLayout.CENTER);
	  m_BoundaryVisualizerFrame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent w) {
	      m_BoundaryVisualizerFrame.dispose();
	      m_BoundaryVisualizerFrame = null;
	      m_BoundaryVisualizerBut.setEnabled(true);
	      checkExit();
	    }
	  });
	  m_BoundaryVisualizerFrame.pack();
	  m_BoundaryVisualizerFrame.setSize(800, 600);
	  m_BoundaryVisualizerFrame.setVisible(true);
	  // dont' do a System.exit after last window got closed!
	  BoundaryVisualizer.setExitIfNoWindowsOpen(false);
	}
      }
    });

    // help
    
    m_HomepageBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	BrowserHelper.openURL("http://www.cs.waikato.ac.nz/~ml/weka/");
      }
    });

    m_WekaDocBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	BrowserHelper.openURL("http://weka.sourceforge.net/wekadoc/");
      }
    });

    m_WekaWikiBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	BrowserHelper.openURL("http://weka.sourceforge.net/wiki/");
      }
    });

    m_SourceforgeBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	BrowserHelper.openURL("http://sourceforge.net/projects/weka/");
      }
    });

    m_SystemInfoBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	if (m_SystemInfoFrame == null) {
	  m_SystemInfoBut.setEnabled(false);
	  m_SystemInfoFrame = new JFrame("SystemInfo");
	  m_SystemInfoFrame.setIconImage(m_Icon);
	  m_SystemInfoFrame.getContentPane().setLayout(new BorderLayout());

	  // get info
	  Hashtable info = new SystemInfo().getSystemInfo();

	  // sort names
	  Vector names = new Vector();
	  Enumeration enm = info.keys();
	  while (enm.hasMoreElements())
	    names.add(enm.nextElement());
	  Collections.sort(names);

	  // generate table
	  String[][] data = new String[info.size()][2];
	  for (int i = 0; i < names.size(); i++) {
	    data[i][0] = names.get(i).toString();
	    data[i][1] = info.get(data[i][0]).toString();
	  }
	  String[] titles = new String[]{"Key", "Value"};
	  JTable table = new JTable(data, titles);

	  m_SystemInfoFrame.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
	  m_SystemInfoFrame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent w) {
	      m_SystemInfoFrame.dispose();
	      m_SystemInfoFrame = null;
	      m_SystemInfoBut.setEnabled(true);
	      checkExit();
	    }
	  });
	  m_SystemInfoFrame.pack();
	  m_SystemInfoFrame.setSize(800, 600);
	  m_SystemInfoFrame.setVisible(true);
	}
      }
    });

    // other
    
    m_LogWindow.setIconImage(m_Icon);
    m_LogWindowBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_LogWindow.setVisible(true);
      }
    });

    m_ExitBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	dispose();
	checkExit();
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
	// applications
	&& (m_ExplorerFrame == null)
	&& (m_ExperimenterFrame == null)
	&& (m_KnowledgeFlowFrame == null)
	&& (m_SimpleCLI == null)
	// tools
	&& (m_ArffViewers.size() == 0)
	&& (m_SqlViewerFrame == null)
	&& (m_EnsembleLibraryFrame == null)
	// visualization
	&& (m_Plots.size() == 0)
	&& (m_ROCs.size() == 0)
	&& (m_TreeVisualizers.size() == 0)
	&& (m_GraphVisualizers.size() == 0)
	&& (m_BoundaryVisualizerFrame == null)
	// help
	&& (m_SystemInfoFrame == null) ) {
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
