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
 * Main.java
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import weka.classifiers.EnsembleLibrary;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Instances;
import weka.core.Memory;
import weka.core.SystemInfo;
import weka.core.Utils;
import weka.core.Version;
import weka.gui.arffviewer.ArffViewerMainPanel;
import weka.gui.beans.KnowledgeFlow;
import weka.gui.beans.KnowledgeFlowApp;
import weka.gui.beans.StartUpListener;
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
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * Menu-based GUI for Weka, replacement for the GUIChooser.
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public class Main
  extends JFrame {
  
  /** for serialization */
  private static final long serialVersionUID = 1453813254824253849L;
  
  /**
   * DesktopPane with background image
   * 
   * @author  fracpete (fracpete at waikato dot ac dot nz)
   * @version $Revision: 1.2 $
   */
  public static class BackgroundDesktopPane
    extends JDesktopPane {
    
    /** for serialization */
    private static final long serialVersionUID = 2046713123452402745L;
    
    /** the actual background image */
    protected Image m_Background;
    
    /**
     * intializes the desktop pane
     * 
     * @param image	the image to use as background
     */
    public BackgroundDesktopPane(String image) {
      super();
      
      try {
	m_Background = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource(image));
      }
      catch (Exception e) {
	e.printStackTrace();
      }
    }
    
    /**
     * draws the background image
     * 
     * @param g		the graphics context
     */
    public void paintComponent(Graphics g) {
      super.paintComponent(g);
     
      if (m_Background != null) {
	g.setColor(Color.WHITE);
	g.clearRect(0, 0, getWidth(), getHeight());
	
	int width  = m_Background.getWidth(null);
	int height = m_Background.getHeight(null);
	int x = (getWidth() - width) / 2;
	int y = (getHeight() - height) / 2;
	g.drawImage(m_Background, x, y, width, height, this);
      }
    }
  }
  
  /**
   * Specialized JInternalFrame class.
   * 
   * @author  fracpete (fracpete at waikato dot ac dot nz)
   * @version $Revision: 1.2 $
   */
  public static class ChildFrame
    extends JInternalFrame {
    
    /** for serialization */
    private static final long serialVersionUID = 3772573515346899959L;
    
    /** the parent frame */
    protected Main m_Parent;
    
    /**
     * constructs a new internal frame that knows about its parent
     * 
     * @param parent	the parent frame
     * @param title	the title of the frame
     */
    public ChildFrame(Main parent, String title) {
      super(title, true, true, true, true);
      
      m_Parent = parent;

      addInternalFrameListener(new InternalFrameAdapter() {
	public void internalFrameActivated(InternalFrameEvent e) {
	  // update title of parent
	  if (getParentFrame() != null)
	    getParentFrame().createTitle(getTitle());
	}
      });
      
      // add to parent
      if (getParentFrame() != null) {
	getParentFrame().addChildFrame(this);
	getParentFrame().jDesktopPane.add(this);
      }
      
      // display frame
      setVisible(true);
      try {
	setSelected(true);
      }
      catch (Exception e) {
	e.printStackTrace();
      }
    }
    
    /**
     * returns the parent frame, can be null
     * 
     * @return		the parent frame
     */
    public Main getParentFrame() {
      return m_Parent;
    }
    
    /**
     * de-registers the child frame with the parent first
     */
    public void dispose() {
      if (getParentFrame() != null) {
	getParentFrame().removeChildFrame(this);
	getParentFrame().createTitle("");
      }
      
      super.dispose();
    }
  }
  
  /** the frame itself */
  protected Main m_Self;
  
  /** variable for the Main class which would be set to null by the memory 
   *  monitoring thread to free up some memory if we running out of memory */
  protected static Main m_MainCommandline;
  
  /** singleton instance of the GUI */
  protected static Main m_MainSingleton;

  /** list of things to be notified when the startup process of
   *  the KnowledgeFlow is complete */
  protected static Vector m_StartupListeners = new Vector();

  /** for monitoring the Memory consumption */
  protected static Memory m_Memory = new Memory(true);
  
  /** contains the child frames (title &lt;-&gt; object) */
  protected HashSet<ChildFrame> m_ChildFrames = new HashSet<ChildFrame>();

  /** The frame of the LogWindow */
  protected static LogWindow m_LogWindow = new LogWindow();

  /** filechooser for the TreeVisualizer */
  protected JFileChooser m_FileChooserTreeVisualizer = new JFileChooser(new File(System.getProperty("user.dir")));

  /** filechooser for the GraphVisualizer */
  protected JFileChooser m_FileChooserGraphVisualizer = new JFileChooser(new File(System.getProperty("user.dir")));

  /** filechooser for Plots */
  protected JFileChooser m_FileChooserPlot = new JFileChooser(new File(System.getProperty("user.dir")));

  /** filechooser for ROC curves */
  protected JFileChooser m_FileChooserROC = new JFileChooser(new File(System.getProperty("user.dir")));
  
  // GUI components
  private JMenu jMenuHelp;
  private JMenu jMenuVisualization;
  private JMenu jMenuTools;
  private JDesktopPane jDesktopPane;
  private JMenu jMenuApplications;
  private JSeparator jSeparatorProgram1;
  private JMenuItem jMenuItemHelpSystemInfo;
  private JSeparator jSeparatorHelp1;
  private JSeparator jSeparatorHelp2;
  private JMenuItem jMenuItemHelpAbout;
  private JMenuItem jMenuItemHelpHomepage;
  private JMenuItem jMenuItemVisualizationBoundaryVisualizer;
  private JMenuItem jMenuItemVisualizationGraphVisualizer;
  private JMenuItem jMenuItemVisualizationTreeVisualizer;
  private JMenuItem jMenuItemVisualizationROC;
  private JMenuItem jMenuItemVisualizationPlot;
  private JMenuItem jMenuItemToolsEnsembleLibrary;
  private JMenuItem jMenuItemToolsSqlViewer;
  private JMenuItem jMenuItemToolsArffViewer;
  private JMenuItem jMenuItemApplicationsSimpleCLI;
  private JMenuItem jMenuItemApplicationsKnowledgeFlow;
  private JMenuItem jMenuItemApplicationsExperimenter;
  private JMenuItem jMenuItemApplicationsExplorer;
  private JMenuItem jMenuItemProgramExit;
  private JMenuItem jMenuItemProgramLogWindow;
  private JMenuItem jMenuItemProgramPreferences;
  private JMenu jMenuProgram;
  private JMenu jMenuWindows;
  private JMenuBar jMenuBar;
  
  /**
   * default constructor
   */
  public Main() {
    super();
    
    initGUI();
  }
  
  /**
   * initializes the GUI
   */
  private void initGUI() {
    m_Self = this;
    
    try {
      // main window
      setSize(1000, 800);
      int screenHeight = getGraphicsConfiguration().getBounds().height;
      int screenWidth  = getGraphicsConfiguration().getBounds().width;
      setLocation(
	  (screenWidth - getBounds().width) / 2,
	  (screenHeight - getBounds().height) / 2);
      createTitle("");
      this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      this.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("weka/gui/weka_icon.gif")).getImage());

      // bits and pieces
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

      // Desktop
      jDesktopPane = new BackgroundDesktopPane("weka/gui/images/weka_background.gif");
      jDesktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
      setContentPane(jDesktopPane);
      
      // Menu
      jMenuBar = new JMenuBar();
      setJMenuBar(jMenuBar);
      
      // Program
      jMenuProgram = new JMenu();
      jMenuBar.add(jMenuProgram);
      jMenuProgram.setText("Program");
      jMenuProgram.setMnemonic('P');
      
      // Program/Preferences
      /*
      jMenuItemProgramPreferences = new JMenuItem();
      jMenuProgram.add(jMenuItemProgramPreferences);
      jMenuItemProgramPreferences.setText("Preferences");
      jMenuItemProgramPreferences.setMnemonic('P');
      jMenuItemProgramPreferences.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  System.out.println("jMenuItemProgramPreferences.actionPerformed, event="+evt);
	  //TODO add your code for jMenuItemProgramPreferences.actionPerformed
	}
      });
      */
      
      // Program/LogWindow
      jMenuItemProgramLogWindow = new JMenuItem();
      jMenuProgram.add(jMenuItemProgramLogWindow);
      jMenuItemProgramLogWindow.setText("LogWindow");
      jMenuItemProgramLogWindow.setMnemonic('L');
      jMenuItemProgramLogWindow.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  m_LogWindow.setVisible(true);
	}
      });
      
      jSeparatorProgram1 = new JSeparator();
      jMenuProgram.add(jSeparatorProgram1);
      
      // Program/Exit
      jMenuItemProgramExit = new JMenuItem();
      jMenuProgram.add(jMenuItemProgramExit);
      jMenuItemProgramExit.setText("Exit");
      jMenuItemProgramExit.setMnemonic('E');
      jMenuItemProgramExit.addActionListener(new ActionListener() {	
	public void actionPerformed(ActionEvent evt) {
	  // close all children
	  Iterator iter = getWindowList();
	  Vector<ChildFrame> list = new Vector<ChildFrame>();
	  while (iter.hasNext())
	    list.add((ChildFrame) iter.next());
	  for (int i = 0; i < list.size(); i++)
	    list.get(i).dispose();
	  // close logwindow
	  m_LogWindow.dispose();
	  // close main window
	  m_Self.dispose();
	  // make sure we stop
	  System.exit(0);
	}
      });
      
      // Applications
      jMenuApplications = new JMenu();
      jMenuBar.add(jMenuApplications);
      jMenuApplications.setText("Applications");
      jMenuApplications.setMnemonic('A');
      
      // Applications/Explorer
      jMenuItemApplicationsExplorer = new JMenuItem();
      jMenuApplications.add(jMenuItemApplicationsExplorer);
      jMenuItemApplicationsExplorer.setText("Explorer");
      jMenuItemApplicationsExplorer.setMnemonic('E');
      jMenuItemApplicationsExplorer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  String title = jMenuItemApplicationsExplorer.getText();
	  if (!containsWindow(title)) {
	    final ChildFrame frame = new ChildFrame(m_Self, title);
	    frame.getContentPane().setLayout(new BorderLayout());
	    frame.getContentPane().add(new Explorer(), BorderLayout.CENTER);
	    frame.addInternalFrameListener(new InternalFrameAdapter() {
	      public void internalFrameClosing(InternalFrameEvent e) {
		frame.dispose();
	      }
	    });
	    frame.pack();
	    frame.setSize(800, 600);
	  }
	  else {
	    showWindow(getWindow(title));
	  }
	}
      });
      
      // Applications/Experimenter
      jMenuItemApplicationsExperimenter = new JMenuItem();
      jMenuApplications.add(jMenuItemApplicationsExperimenter);
      jMenuItemApplicationsExperimenter.setText("Experimenter");
      jMenuItemApplicationsExperimenter.setMnemonic('X');
      jMenuItemApplicationsExperimenter.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  String title = jMenuItemApplicationsExperimenter.getText();
	  if (!containsWindow(title)) {
	    final ChildFrame frame = new ChildFrame(m_Self, title);
	    frame.getContentPane().setLayout(new BorderLayout());
	    frame.getContentPane().add(new Experimenter(false), BorderLayout.CENTER);
	    frame.addInternalFrameListener(new InternalFrameAdapter() {
	      public void internalFrameClosing(InternalFrameEvent e) {
		frame.dispose();
	      }
	    });
	    frame.pack();
	    frame.setSize(800, 600);
	  }
	  else {
	    showWindow(getWindow(title));
	  }
	}
      });
      
      // Applications/KnowledgeFlow
      jMenuItemApplicationsKnowledgeFlow = new JMenuItem();
      jMenuApplications.add(jMenuItemApplicationsKnowledgeFlow);
      jMenuItemApplicationsKnowledgeFlow.setText("KnowledgeFlow");
      jMenuItemApplicationsKnowledgeFlow.setMnemonic('K');
      jMenuItemApplicationsKnowledgeFlow.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  KnowledgeFlow.startApp();
	}
      });
      KnowledgeFlowApp.addStartupListener(new weka.gui.beans.StartUpListener() {
        public void startUpComplete() {
	  String title = jMenuItemApplicationsKnowledgeFlow.getText();
	  if (!containsWindow(title)) {
	    final ChildFrame frame = new ChildFrame(m_Self, title);
	    frame.getContentPane().setLayout(new BorderLayout());
	    frame.getContentPane().add(KnowledgeFlowApp.getSingleton(), BorderLayout.CENTER);
	    frame.addInternalFrameListener(new InternalFrameAdapter() {
	      public void internalFrameClosing(InternalFrameEvent e) {
		frame.dispose();
	      }
	    });
	    frame.pack();
	    frame.setSize(900, 600);
          }
	  else {
	    showWindow(getWindow(title));
	  }
        }
      });
      
      // Applications/SimpleCLI
      jMenuItemApplicationsSimpleCLI = new JMenuItem();
      jMenuApplications.add(jMenuItemApplicationsSimpleCLI);
      jMenuItemApplicationsSimpleCLI.setText("SimpleCLI");
      jMenuItemApplicationsSimpleCLI.setMnemonic('S');
      jMenuItemApplicationsSimpleCLI.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  String title = jMenuItemApplicationsSimpleCLI.getText();
	  if (!containsWindow(title)) {
	    final ChildFrame frame = new ChildFrame(m_Self, title);
	    frame.getContentPane().setLayout(new BorderLayout());
	    try {
	      frame.getContentPane().add(new SimpleCLIPanel(), BorderLayout.CENTER);
	    }
	    catch (Exception e) {
	      e.printStackTrace();
	      JOptionPane.showMessageDialog(
		  m_Self, "Error instantiating SimpleCLI:\n" + e.getMessage());
	      return;
	    }
	    frame.addInternalFrameListener(new InternalFrameAdapter() {
	      public void internalFrameClosing(InternalFrameEvent e) {
		frame.dispose();
	      }
	    });
	    frame.pack();
	    frame.setSize(600, 500);
          }
	  else {
	    showWindow(getWindow(title));
	  }
	}
      });
      
      // Tools
      jMenuTools = new JMenu();
      jMenuBar.add(jMenuTools);
      jMenuTools.setText("Tools");
      jMenuTools.setMnemonic('T');
      
      // Tools/ArffViewer
      jMenuItemToolsArffViewer = new JMenuItem();
      jMenuTools.add(jMenuItemToolsArffViewer);
      jMenuItemToolsArffViewer.setText("ArffViewer");
      jMenuItemToolsArffViewer.setMnemonic('A');
      jMenuItemToolsArffViewer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  String title = jMenuItemToolsArffViewer.getText();
	  if (!containsWindow(title)) {
	    final ChildFrame frame = new ChildFrame(m_Self, title);
	    frame.getContentPane().setLayout(new BorderLayout());
	    ArffViewerMainPanel panel = new ArffViewerMainPanel(frame);
	    panel.setConfirmExit(false);
	    frame.getContentPane().add(panel, BorderLayout.CENTER);
	    frame.setJMenuBar(panel.getMenu());
	    frame.addInternalFrameListener(new InternalFrameAdapter() {
	      public void internalFrameClosing(InternalFrameEvent e) {
		frame.dispose();
	      }
	    });
	    frame.pack();
	    frame.setSize(800, 600);
	  }
	  else {
	    showWindow(getWindow(title));
	  }
	}
      });
      
      // Tools/SqlViewer
      jMenuItemToolsSqlViewer = new JMenuItem();
      jMenuTools.add(jMenuItemToolsSqlViewer);
      jMenuItemToolsSqlViewer.setText("SqlViewer");
      jMenuItemToolsSqlViewer.setMnemonic('S');
      jMenuItemToolsSqlViewer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  String title = jMenuItemToolsSqlViewer.getText();
	  if (!containsWindow(title)) {
	    final ChildFrame frame = new ChildFrame(m_Self, title);
	    frame.getContentPane().setLayout(new BorderLayout());
	    frame.getContentPane().add(new SqlViewer(null), BorderLayout.CENTER);
	    frame.addInternalFrameListener(new InternalFrameAdapter() {
	      public void internalFrameClosing(InternalFrameEvent e) {
		frame.dispose();
	      }
	    });
	    frame.pack();
	    frame.setSize(800, 600);
	  }
	  else {
	    showWindow(getWindow(title));
	  }
	}
      });
      
      // Tools/EnsembleLibrary
      jMenuItemToolsEnsembleLibrary = new JMenuItem();
      jMenuTools.add(jMenuItemToolsEnsembleLibrary);
      jMenuItemToolsEnsembleLibrary.setText("EnsembleLibrary");
      jMenuItemToolsEnsembleLibrary.setMnemonic('E');
      jMenuItemToolsEnsembleLibrary.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  String title = jMenuItemToolsEnsembleLibrary.getText();
	  if (!containsWindow(title)) {
	    final ChildFrame frame = new ChildFrame(m_Self, title);
	    frame.getContentPane().setLayout(new BorderLayout());
	    EnsembleLibrary value = new EnsembleLibrary();
	    EnsembleLibraryEditor libraryEditor = new EnsembleLibraryEditor();
	    libraryEditor.setValue(value);
	    frame.getContentPane().add(libraryEditor.getCustomEditor(), BorderLayout.CENTER);
	    frame.addInternalFrameListener(new InternalFrameAdapter() {
	      public void internalFrameClosing(InternalFrameEvent e) {
		frame.dispose();
	      }
	    });
	    frame.pack();
	    frame.setSize(800, 600);
	  }
	  else {
	    showWindow(getWindow(title));
	  }
	}
      });
      
      // Visualization
      jMenuVisualization = new JMenu();
      jMenuBar.add(jMenuVisualization);
      jMenuVisualization.setText("Visualization");
      jMenuVisualization.setMnemonic('V');
      
      // Visualization/Plot
      jMenuItemVisualizationPlot = new JMenuItem();
      jMenuVisualization.add(jMenuItemVisualizationPlot);
      jMenuItemVisualizationPlot.setText("Plot");
      jMenuItemVisualizationPlot.setMnemonic('P');
      jMenuItemVisualizationPlot.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
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
	    catch (Exception e) {
	      e.printStackTrace();
	      JOptionPane.showMessageDialog(
		  m_Self, "Error loading file '" + files[j] + "':\n" + e.getMessage());
	      return;
	    }
	  }

	  // create frame
	  final ChildFrame frame = new ChildFrame(m_Self, jMenuItemVisualizationPlot.getText() + " - " + filenames);
	  frame.getContentPane().setLayout(new BorderLayout());
	  frame.getContentPane().add(panel, BorderLayout.CENTER);
	  frame.addInternalFrameListener(new InternalFrameAdapter() {
	    public void internalFrameClosing(InternalFrameEvent e) {
	      frame.dispose();
	    }
	  });
	  frame.pack();
	  frame.setSize(800, 600);
	}
      });
      
      // Visualization/ROC
      // based on this Wiki article:
      // http://weka.sourceforge.net/wiki/index.php/Visualizing_ROC_curve
      jMenuItemVisualizationROC = new JMenuItem();
      jMenuVisualization.add(jMenuItemVisualizationROC);
      jMenuItemVisualizationROC.setText("ROC");
      jMenuItemVisualizationROC.setMnemonic('R');
      jMenuItemVisualizationROC.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
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
	  catch (Exception e) {
	    e.printStackTrace();
	    JOptionPane.showMessageDialog(
		m_Self, "Error loading file '" + filename + "':\n" + e.getMessage());
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
	  catch (Exception e) {
	    e.printStackTrace();
	    JOptionPane.showMessageDialog(
		m_Self, "Error adding plot:\n" + e.getMessage());
	    return;
	  }
	  
	  final ChildFrame frame = new ChildFrame(m_Self, jMenuItemVisualizationROC.getText() + " - " + filename);
	  frame.getContentPane().setLayout(new BorderLayout());
	  frame.getContentPane().add(vmc, BorderLayout.CENTER);
	  frame.addInternalFrameListener(new InternalFrameAdapter() {
	    public void internalFrameClosing(InternalFrameEvent e) {
	      frame.dispose();
	    }
	  });
	  frame.pack();
	  frame.setSize(800, 600);
	}
      });
      
      // Visualization/TreeVisualizer
      jMenuItemVisualizationTreeVisualizer = new JMenuItem();
      jMenuVisualization.add(jMenuItemVisualizationTreeVisualizer);
      jMenuItemVisualizationTreeVisualizer.setText("TreeVisualizer");
      jMenuItemVisualizationTreeVisualizer.setMnemonic('T');
      jMenuItemVisualizationTreeVisualizer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
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
	  catch (Exception e) {
	    e.printStackTrace();
	    JOptionPane.showMessageDialog(
		m_Self, "Error loading file '" + filename + "':\n" + e.getMessage());
	    return;
	  }
	  
	  // create frame
	  final ChildFrame frame = new ChildFrame(m_Self, jMenuItemVisualizationTreeVisualizer.getText() + " - " + filename);
	  frame.getContentPane().setLayout(new BorderLayout());
	  frame.getContentPane().add(new TreeVisualizer(null, top, arrange), BorderLayout.CENTER);
	  frame.addInternalFrameListener(new InternalFrameAdapter() {
	    public void internalFrameClosing(InternalFrameEvent e) {
	      frame.dispose();
	    }
	  });
	  frame.pack();
	  frame.setSize(800, 600);
	}
      });
      
      // Visualization/GraphVisualizer
      jMenuItemVisualizationGraphVisualizer = new JMenuItem();
      jMenuVisualization.add(jMenuItemVisualizationGraphVisualizer);
      jMenuItemVisualizationGraphVisualizer.setText("GraphVisualizer");
      jMenuItemVisualizationGraphVisualizer.setMnemonic('G');
      jMenuItemVisualizationGraphVisualizer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  // choose file
	  int retVal = m_FileChooserGraphVisualizer.showOpenDialog(m_Self);
	  if (retVal != JFileChooser.APPROVE_OPTION)
	    return;
	  
	  // build graph
	  String filename = m_FileChooserGraphVisualizer.getSelectedFile().getAbsolutePath();
	  GraphVisualizer panel = new GraphVisualizer();
	  try{
	    if(filename.toLowerCase().endsWith(".xml")) {
	      panel.readBIF(new FileInputStream(filename));
	    }
	    else {
	      panel.readDOT(new FileReader(filename));
	    }
	  }
	  catch (Exception e) {
	    e.printStackTrace();
	    JOptionPane.showMessageDialog(
		m_Self, "Error loading file '" + filename + "':\n" + e.getMessage());
	    return;
	  }
	  
	  // create frame
	  final ChildFrame frame = new ChildFrame(m_Self, jMenuItemVisualizationGraphVisualizer.getText() + " - " + filename);
	  frame.getContentPane().setLayout(new BorderLayout());
	  frame.getContentPane().add(panel, BorderLayout.CENTER);
	  frame.addInternalFrameListener(new InternalFrameAdapter() {
	    public void internalFrameClosing(InternalFrameEvent e) {
	      frame.dispose();
	    }
	  });
	  frame.pack();
	  frame.setSize(800, 600);
	}
      });
      
      // Visualization/BoundaryVisualizer
      jMenuItemVisualizationBoundaryVisualizer = new JMenuItem();
      jMenuVisualization.add(jMenuItemVisualizationBoundaryVisualizer);
      jMenuItemVisualizationBoundaryVisualizer.setText("BoundaryVisualizer");
      jMenuItemVisualizationBoundaryVisualizer.setMnemonic('B');
      jMenuItemVisualizationBoundaryVisualizer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  String title = jMenuItemVisualizationBoundaryVisualizer.getText();
	  if (!containsWindow(title)) {
	    final ChildFrame frame = new ChildFrame(m_Self, title);
	    frame.getContentPane().setLayout(new BorderLayout());
	    frame.getContentPane().add(new BoundaryVisualizer(), BorderLayout.CENTER);
	    frame.addInternalFrameListener(new InternalFrameAdapter() {
	      public void internalFrameClosing(InternalFrameEvent e) {
		frame.dispose();
	      }
	    });
	    frame.pack();
	    frame.setSize(800, 600);
	  }
	  else {
	    showWindow(getWindow(title));
	  }
	}
      });
      
      // Help
      jMenuHelp = new JMenu();
      jMenuBar.add(jMenuHelp);
      jMenuHelp.setText("Help");
      jMenuHelp.setMnemonic('H');
      
      // Help/Homepage
      jMenuItemHelpHomepage = new JMenuItem();
      jMenuHelp.add(jMenuItemHelpHomepage);
      jMenuItemHelpHomepage.setText("Weka homepage");
      jMenuItemHelpHomepage.setMnemonic('H');
      jMenuItemHelpHomepage.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  BrowserHelper.openURL(m_Self, "http://www.cs.waikato.ac.nz/~ml/weka/");
	}
      });
      
      jSeparatorHelp1 = new JSeparator();
      jMenuHelp.add(jSeparatorHelp1);

      // Help/SystemInfo
      jMenuItemHelpSystemInfo = new JMenuItem();
      jMenuHelp.add(jMenuItemHelpSystemInfo);
      jMenuItemHelpSystemInfo.setText("SystemInfo");
      jMenuItemHelpHomepage.setMnemonic('S');
      jMenuItemHelpSystemInfo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  String title = jMenuItemHelpSystemInfo.getText();
	  if (!containsWindow(title)) {
	    final ChildFrame frame = new ChildFrame(m_Self, title);
	    frame.getContentPane().setLayout(new BorderLayout());

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
	    
	    frame.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
	    frame.addInternalFrameListener(new InternalFrameAdapter() {
	      public void internalFrameClosing(InternalFrameEvent e) {
		frame.dispose();
	      }
	    });
	    frame.pack();
	    frame.setSize(800, 600);
	  }
	  else {
	    showWindow(getWindow(title));
	  }
	}
      });

      jSeparatorHelp2 = new JSeparator();
      jMenuHelp.add(jSeparatorHelp2);

      // Help/About
      jMenuItemHelpAbout = new JMenuItem();
      jMenuHelp.add(jMenuItemHelpAbout);
      jMenuItemHelpAbout.setText("About");
      jMenuItemHelpAbout.setMnemonic('A');
      jMenuItemHelpAbout.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  String title = jMenuItemHelpAbout.getText();
	  if (!containsWindow(title)) {
	    final ChildFrame frame = new ChildFrame(m_Self, title);
	    frame.getContentPane().setLayout(new BorderLayout());
	    
	    JPanel wekaPan = new JPanel();
	    wekaPan.setToolTipText("Weka, a native bird of New Zealand");
	    ImageIcon wii = new ImageIcon(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("weka/gui/weka3.gif")));
	    JLabel wekaLab = new JLabel(wii);
	    wekaPan.add(wekaLab);
	    frame.getContentPane().add(wekaPan, BorderLayout.CENTER);
	    
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
	    titlePan.add(new JLabel("(c) 1999 - 2005",
	    SwingConstants.CENTER));
	    titlePan.add(new JLabel("University of Waikato",
	    SwingConstants.CENTER));
	    titlePan.add(new JLabel("New Zealand",
	    SwingConstants.CENTER));
	    frame.getContentPane().add(titlePan, BorderLayout.NORTH);
	    
	    frame.addInternalFrameListener(new InternalFrameAdapter() {
	      public void internalFrameClosing(InternalFrameEvent e) {
		frame.dispose();
	      }
	    });
	    frame.pack();
	  }
	  else {
	    showWindow(getWindow(title));
	  }
	}
      });
    } 
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * creates and displays the title
   * 
   * @param title 	the additional part of the title
   */
  protected void createTitle(String title) {
    String	newTitle;
    
    newTitle = "Weka " + new Version();
    if (title.length() != 0)
      newTitle += " - " + title;
    
    setTitle(newTitle);
  }
  
  /**
   * adds the given child frame to the list of frames
   * 
   * @param c 		the child frame to add
   */
  public void addChildFrame(ChildFrame c) {
    m_ChildFrames.add(c);
    windowListChanged();
  }
  
  /**
   * tries to remove the child frame, it returns true if it could do such
   * 
   * @param c 		the child frame to remove
   * @return 		true if the child frame could be removed
   */
  public boolean removeChildFrame(ChildFrame c) {
    boolean result = m_ChildFrames.remove(c);
    windowListChanged();
    return result;
  }
  
  /**
   * brings child frame to the top
   * 
   * @param c 		the frame to activate
   * @return 		true if frame was activated
   */
  public boolean showWindow(ChildFrame c) {
    boolean        result;
    
    if (c != null) {
      try {
	c.setIcon(false);
      }
      catch (Exception e) {
	e.printStackTrace();
      }
      c.toFront();
      createTitle(c.getTitle());
      result = true;
    }
    else {
      result = false;
    }
    
    return result;
  }
  
  /**
   * brings the first frame to the top that is of the specified
   * window class
   *  
   * @param windowClass	the class to display the first child for
   * @return		true, if a child was found and brought to front
   */
  public boolean showWindow(Class windowClass) {
    return showWindow(getWindow(windowClass));
  }
  
  /**
   * returns all currently open frames
   * 
   * @return 		an iterator over all currently open frame
   */
  public Iterator getWindowList() {
    return m_ChildFrames.iterator();
  }

  /**
   * returns the first instance of the given window class, null if none can be 
   * found
   * 
   * @param windowClass	the class to retrieve the first instance for
   * @return		null, if no instance can be found
   */
  public ChildFrame getWindow(Class windowClass) {
    ChildFrame	result;
    Iterator	iter;
    ChildFrame	current;
    
    result = null;
    iter   = getWindowList();
    while (iter.hasNext()) {
      current = (ChildFrame) iter.next();
      if (current.getClass() == windowClass) {
        result = current;
        break;
      }
    }
    
    return result;
  }

  /**
   * returns the first window with the given title, null if none can be 
   * found
   * 
   * @param title	the title to look for
   * @return		null, if no instance can be found
   */
  public ChildFrame getWindow(String title) {
    ChildFrame	result;
    Iterator	iter;
    ChildFrame	current;
    
    result = null;
    iter   = getWindowList();
    while (iter.hasNext()) {
      current = (ChildFrame) iter.next();
      if (current.getTitle().equals(title)) {
        result = current;
        break;
      }
    }
    
    return result;
  }
  
  /**
   * checks, whether an instance of the given window class is already in
   * the Window list
   * 
   * @param windowClass	the class to check for an instance in the current
   * 			window list
   * @return		true if the class is already listed in the Window list
   */
  public boolean containsWindow(Class windowClass) {
    return (getWindow(windowClass) != null);
  }
  
  /**
   * checks, whether a window with the given title is already in
   * the Window list
   * 
   * @param title	the title to check for in the current window list
   * @return		true if a window with the given title is already 
   * 			listed in the Window list
   */
  public boolean containsWindow(String title) {
    return (getWindow(title) != null);
  }
  
  /**
   * minimizes all windows
   */
  public void minimizeWindows() {
    Iterator	iter;
    ChildFrame	frame;
    
    iter = getWindowList();
    while (iter.hasNext()) {
      frame = (ChildFrame) iter.next();
      try {
	frame.setIcon(true);
      }
      catch (Exception e) {
	e.printStackTrace();
      }
    }
  }
  
  /**
   * restores all windows
   */
  public void restoreWindows() {
    Iterator	iter;
    ChildFrame	frame;
    
    iter = getWindowList();
    while (iter.hasNext()) {
      frame = (ChildFrame) iter.next();
      try {
	frame.setIcon(false);
      }
      catch (Exception e) {
	e.printStackTrace();
      }
    }
  }
  
  /**
   * is called when window list changed somehow (add or remove)
   */
  public void windowListChanged() {
    createWindowMenu();
  }
  
  /**
   * creates the menu of currently open windows
   */
  private void createWindowMenu() {
    Iterator          iter;
    JMenuItem         menuItem;
    
    // first time?
    if (jMenuWindows == null) {
      jMenuWindows = new JMenu("Windows");
      jMenuWindows.setMnemonic(java.awt.event.KeyEvent.VK_W);
      jMenuBar.remove(jMenuHelp);
      jMenuBar.add(jMenuWindows);
      jMenuBar.add(jMenuHelp);
    }

    // remove all existing entries
    jMenuWindows.removeAll();
    
    // minimize + restore + separator
    menuItem = new JMenuItem("Minimize");
    menuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        minimizeWindows();
      }
    });
    jMenuWindows.add(menuItem);
    
    menuItem = new JMenuItem("Restore");
    menuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        restoreWindows();
      }
    });
    jMenuWindows.add(menuItem);
    
    jMenuWindows.addSeparator();
    
    // windows
    iter = getWindowList();
    jMenuWindows.setVisible(iter.hasNext());
    while (iter.hasNext()) {
      ChildFrame frame = (ChildFrame) iter.next();
      menuItem = new JMenuItem(((ChildFrame) frame).getTitle());
      jMenuWindows.add(menuItem);
      menuItem.setActionCommand(Integer.toString(frame.hashCode()));
      menuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          ChildFrame frame = null;
          Iterator iter = getWindowList();
          while (iter.hasNext()) {
            frame = (ChildFrame) iter.next();
            String hashFrame = Integer.toString(frame.hashCode());
            if (hashFrame.equals(evt.getActionCommand())) {
              showWindow(frame);
              break;
            }
          }
          showWindow(frame);
        }
      });
      jMenuWindows.add(menuItem);
    }
  }
  
  /**
   * Shows or hides this component depending on the value of parameter b.
   * 
   * @param b		if true, shows this component; otherwise, hides this 
   * 			component
   */
  public void setVisible(boolean b) {
    super.setVisible(b);
    
    if (b)
      paint(this.getGraphics());
  }
  
  /**
   * Create the singleton instance of the Main GUI
   * 
   * @param args 	ignored at present
   */
  public static void createSingleton(String[] args) {
    if (m_MainSingleton == null)
      m_MainSingleton = new Main();

    // notify listeners (if any)
    for (int i = 0; i < m_StartupListeners.size(); i++)
      ((StartUpListener) m_StartupListeners.elementAt(i)).startUpComplete();
  }

  /**
   * Return the singleton instance of the Main GUI
   *
   * @return the singleton instance
   */
  public static Main getSingleton() {
    return m_MainSingleton;
  }

  /**
   * Add a listener to be notified when startup is complete
   * 
   * @param s 		a listener to add
   */
  public static void addStartupListener(StartUpListener s) {
    m_StartupListeners.add(s);
  }
  
  /**
   * starts the application
   * 
   * @param args	the commandline arguments - ignored
   */
  public static void main(String[] args) {
    LookAndFeel.setLookAndFeel();
    
    try {
      // uncomment the following line to disable the memory management:
      //m_Memory.setEnabled(false);

      // setup splash screen
      Main.addStartupListener(new weka.gui.beans.StartUpListener() {
        public void startUpComplete() {
          m_MainCommandline = Main.getSingleton();
          m_MainCommandline.setVisible(true);
        }
      });
      Main.addStartupListener(new StartUpListener() {
        public void startUpComplete() {
          SplashWindow.disposeSplash();
        }
      });
      SplashWindow.splash(ClassLoader.getSystemResource("weka/gui/images/weka_splash.gif"));

      // start GUI
      Thread nt = new Thread() {
	public void run() {
	  weka.gui.SplashWindow.invokeMethod(
	      "weka.gui.Main", "createSingleton", new String [1]);
	}
      };
      nt.start();
      
      Thread memMonitor = new Thread() {
	public void run() {
	  while(true) {
	    try {
	      Thread.sleep(4000);
	      System.gc();
	      
	      if (m_Memory.isOutOfMemory()) {
		// clean up
		m_MainCommandline = null;
		System.gc();
		
		// stop threads
		m_Memory.stopThreads();
		
		// display error
		System.err.println("\ndisplayed message:");
		m_Memory.showOutOfMemory();
		System.err.println("\nexiting");
		System.exit(-1);
	      }
	      
	    } catch(InterruptedException ex) { ex.printStackTrace(); }
	  }
	}
      };
      
      memMonitor.setPriority(Thread.MAX_PRIORITY);
      memMonitor.start();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
