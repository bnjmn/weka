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
 *    GUIChooser.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import weka.classifiers.bayes.net.GUI;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Copyright;
import weka.core.Defaults;
import weka.core.Instances;
import weka.core.Memory;
import weka.core.PluginManager;
import weka.core.Settings;
import weka.core.SystemInfo;
import weka.core.Utils;
import weka.core.Version;
import weka.core.WekaPackageClassLoaderManager;
import weka.core.WekaPackageManager;
import weka.core.scripting.Groovy;
import weka.core.scripting.Jython;
import weka.gui.arffviewer.ArffViewer;
import weka.gui.boundaryvisualizer.BoundaryVisualizer;
import weka.gui.experiment.Experimenter;
import weka.gui.explorer.Explorer;
import weka.gui.graphvisualizer.GraphVisualizer;
import weka.gui.knowledgeflow.KnowledgeFlowApp;
import weka.gui.knowledgeflow.MainKFPerspective;
import weka.gui.scripting.JythonPanel;
import weka.gui.sql.SqlViewer;
import weka.gui.treevisualizer.Node;
import weka.gui.treevisualizer.NodePlace;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeBuild;
import weka.gui.treevisualizer.TreeVisualizer;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;
import weka.gui.visualize.VisualizePanel;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * The main class for the Weka GUIChooser. Lets the user choose which GUI they
 * want to run.
 * 
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class GUIChooserApp extends JFrame {

  /** for serialization */
  private static final long serialVersionUID = 9001529425230247914L;

  /** GUIChooser settings */
  private Settings m_settings;

  /** the GUIChooser itself */
  protected GUIChooserApp m_Self;

  // Menu stuff
  private JMenuBar m_jMenuBar;
  private JMenu m_jMenuProgram;
  private JMenu m_jMenuVisualization;
  private JMenu m_jMenuTools;
  private JMenu m_jMenuHelp;

  // Applications

  /** The frames for the various applications that are open */
  protected Vector<JFrame> m_Frames = new Vector<JFrame>();

  /** the panel for the application buttons */
  protected JPanel m_PanelApplications = new JPanel();

  /** Click to open the Workbench */
  protected JButton m_WorkbenchBut = new JButton("Workbench");

  /** Click to open the Explorer */
  protected JButton m_ExplorerBut = new JButton("Explorer");

  /** Click to open the Explorer */
  protected JButton m_ExperimenterBut = new JButton("Experimenter");

  /** Click to open the KnowledgeFlow */
  protected JButton m_KnowledgeFlowBut = new JButton("KnowledgeFlow");

  /** Click to open the simplecli */
  protected JButton m_SimpleBut = new JButton("Simple CLI");

  /** The frame of the LogWindow */
  protected static LogWindow m_LogWindow = new LogWindow();

  /** The weka image */
  Image m_weka = Toolkit.getDefaultToolkit().getImage(
    GUIChooserApp.class.getClassLoader().getResource(
      "weka/gui/images/weka_background.gif"));

  /** filechooser for the TreeVisualizer */
  protected JFileChooser m_FileChooserTreeVisualizer = new JFileChooser(
    new File(System.getProperty("user.dir")));

  /** filechooser for the GraphVisualizer */
  protected JFileChooser m_FileChooserGraphVisualizer = new JFileChooser(
    new File(System.getProperty("user.dir")));

  /** filechooser for Plots */
  protected JFileChooser m_FileChooserPlot = new JFileChooser(new File(
    System.getProperty("user.dir")));

  /** filechooser for ROC curves */
  protected JFileChooser m_FileChooserROC = new JFileChooser(new File(
    System.getProperty("user.dir")));

  /** the icon for the frames */
  protected Image m_Icon;

  /** contains the child frames (title &lt;-&gt; object). */
  protected HashSet<Container> m_ChildFrames = new HashSet<Container>();

  /**
   * Create a singleton instance of the GUIChooser
   */
  public static synchronized void createSingleton() {
    if (m_chooser == null) {
      m_chooser = new GUIChooserApp();
    }
  }

  /**
   * Get the singleton instance of the GUIChooser
   *
   * @return the singleton instance of the GUIChooser
   */
  public static GUIChooserApp getSingleton() {
    return m_chooser;
  }

  /**
   * Creates the experiment environment gui with no initial experiment
   */
  public GUIChooserApp() {

    super("Weka GUI Chooser");

    m_Self = this;

    m_settings = new Settings("weka", GUIChooserDefaults.APP_ID);
    GUIChooserDefaults guiChooserDefaults = new GUIChooserDefaults();
    Defaults pmDefaults =
      WekaPackageManager.getUnderlyingPackageManager().getDefaultSettings();
    guiChooserDefaults.add(pmDefaults);
    m_settings.applyDefaults(guiChooserDefaults);
    WekaPackageManager.getUnderlyingPackageManager().applySettings(m_settings);

    // filechoosers
    m_FileChooserGraphVisualizer
      .addChoosableFileFilter(new ExtensionFileFilter(".bif",
        "BIF Files (*.bif)"));
    m_FileChooserGraphVisualizer
      .addChoosableFileFilter(new ExtensionFileFilter(".xml",
        "XML Files (*.xml)"));

    m_FileChooserPlot.addChoosableFileFilter(new ExtensionFileFilter(
      Instances.FILE_EXTENSION, "ARFF Files (*" + Instances.FILE_EXTENSION
        + ")"));
    m_FileChooserPlot.setMultiSelectionEnabled(true);

    m_FileChooserROC.addChoosableFileFilter(new ExtensionFileFilter(
      Instances.FILE_EXTENSION, "ARFF Files (*" + Instances.FILE_EXTENSION
        + ")"));

    // general layout
    m_Icon =
      Toolkit.getDefaultToolkit().getImage(
        GUIChooserApp.class.getClassLoader().getResource(
          "weka/gui/weka_icon_new_48.png"));
    setIconImage(m_Icon);
    this.getContentPane().setLayout(new BorderLayout());

    this.getContentPane().add(m_PanelApplications, BorderLayout.EAST);

    // applications
    m_PanelApplications.setBorder(BorderFactory
      .createTitledBorder("Applications"));
    m_PanelApplications.setLayout(new GridLayout(0, 1));
    m_PanelApplications.add(m_ExplorerBut);
    m_PanelApplications.add(m_ExperimenterBut);
    m_PanelApplications.add(m_KnowledgeFlowBut);
    m_PanelApplications.add(m_WorkbenchBut);
    m_PanelApplications.add(m_SimpleBut);

    // Weka image plus copyright info
    JPanel wekaPan = new JPanel();
    wekaPan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    wekaPan.setLayout(new BorderLayout());
    wekaPan.setToolTipText("Weka, a native bird of New Zealand");
    ImageIcon wii = new ImageIcon(m_weka);
    JLabel wekaLab = new JLabel(wii);
    wekaPan.add(wekaLab, BorderLayout.CENTER);
    String infoString =
      "<html>" + "<font size=-2>"
        + "Waikato Environment for Knowledge Analysis<br>" + "Version "
        + Version.VERSION + "<br>" + "(c) " + Copyright.getFromYear() + " - "
        + Copyright.getToYear() + "<br>" + Copyright.getOwner() + "<br>"
        + Copyright.getAddress() + "</font>" + "</html>";
    JLabel infoLab = new JLabel(infoString);
    infoLab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    wekaPan.add(infoLab, BorderLayout.SOUTH);

    this.getContentPane().add(wekaPan, BorderLayout.CENTER);

    // Menu bar
    m_jMenuBar = new JMenuBar();

    // Program
    m_jMenuProgram = new JMenu();
    m_jMenuBar.add(m_jMenuProgram);
    m_jMenuProgram.setText("Program");
    m_jMenuProgram.setMnemonic('P');

    // Program/LogWindow
    JMenuItem jMenuItemProgramLogWindow = new JMenuItem();
    m_jMenuProgram.add(jMenuItemProgramLogWindow);
    jMenuItemProgramLogWindow.setText("LogWindow");
    // jMenuItemProgramLogWindow.setMnemonic('L');
    jMenuItemProgramLogWindow.setAccelerator(KeyStroke.getKeyStroke(
      KeyEvent.VK_L, KeyEvent.CTRL_MASK));
    m_LogWindow.setIconImage(m_Icon);
    jMenuItemProgramLogWindow.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_Self != null) {
          m_LogWindow.pack();
          m_LogWindow.setSize(800, 600);
          m_LogWindow.setLocationRelativeTo(m_Self);
        }
        m_LogWindow.setVisible(true);
      }
    });

    final JMenuItem jMenuItemProgramMemUsage = new JMenuItem();
    m_jMenuProgram.add(jMenuItemProgramMemUsage);
    jMenuItemProgramMemUsage.setText("Memory usage");
    // jMenuItemProgramMemUsage.setMnemonic('M');
    jMenuItemProgramMemUsage.setAccelerator(KeyStroke.getKeyStroke(
      KeyEvent.VK_M, KeyEvent.CTRL_MASK));

    jMenuItemProgramMemUsage.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final MemoryUsagePanel panel = new MemoryUsagePanel();
        final JFrame frame = Utils.getWekaJFrame("Memory usage", m_Self);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent w) {
            panel.stopMonitoring();
            frame.dispose();
            m_Frames.remove(frame);
            checkExit();
          }
        });
        frame.pack();
        frame.setSize(400, 50);
        Point l = panel.getFrameLocation();
        if ((l.x != -1) && (l.y != -1)) {
          frame.setLocation(l);
        }
        frame.setLocationRelativeTo(m_Self);
        frame.setVisible(true);
        Dimension size = frame.getPreferredSize();
        frame.setSize(new Dimension((int) size.getWidth(),
                (int) size.getHeight()));
        m_Frames.add(frame);
      }
    });

    final JMenuItem jMenuItemSettings = new JMenuItem();
    m_jMenuProgram.add(jMenuItemSettings);
    jMenuItemSettings.setText("Settings");
    jMenuItemSettings.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          int result =
            SettingsEditor.showSingleSettingsEditor(m_settings,
              GUIChooserDefaults.APP_ID, "GUIChooser",
              (JComponent) GUIChooserApp.this.getContentPane().getComponent(0),
              550, 100);
          if (result == JOptionPane.OK_OPTION) {
            WekaPackageManager.getUnderlyingPackageManager().applySettings(
              m_settings);
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });

    m_jMenuProgram.add(new JSeparator());

    // Program/Exit
    JMenuItem jMenuItemProgramExit = new JMenuItem();
    m_jMenuProgram.add(jMenuItemProgramExit);
    jMenuItemProgramExit.setText("Exit");
    // jMenuItemProgramExit.setMnemonic('E');
    jMenuItemProgramExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
      KeyEvent.CTRL_MASK));
    jMenuItemProgramExit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
        checkExit();
      }
    });

    // Visualization
    m_jMenuVisualization = new JMenu();
    m_jMenuBar.add(m_jMenuVisualization);
    m_jMenuVisualization.setText("Visualization");
    m_jMenuVisualization.setMnemonic('V');

    // Visualization/Plot
    JMenuItem jMenuItemVisualizationPlot = new JMenuItem();
    m_jMenuVisualization.add(jMenuItemVisualizationPlot);
    jMenuItemVisualizationPlot.setText("Plot");
    // jMenuItemVisualizationPlot.setMnemonic('P');
    jMenuItemVisualizationPlot.setAccelerator(KeyStroke.getKeyStroke(
      KeyEvent.VK_P, KeyEvent.CTRL_MASK));

    jMenuItemVisualizationPlot.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // choose file
        int retVal = m_FileChooserPlot.showOpenDialog(m_Self);
        if (retVal != JFileChooser.APPROVE_OPTION) {
          return;
        }

        // build plot
        VisualizePanel panel = new VisualizePanel();
        String filenames = "";
        File[] files = m_FileChooserPlot.getSelectedFiles();
        for (int j = 0; j < files.length; j++) {
          String filename = files[j].getAbsolutePath();
          if (j > 0) {
            filenames += ", ";
          }
          filenames += filename;
          System.err.println("Loading instances from " + filename);
          try {
            Reader r = new java.io.BufferedReader(new FileReader(filename));
            Instances i = new Instances(r);
            i.setClassIndex(i.numAttributes() - 1);
            PlotData2D pd1 = new PlotData2D(i);

            if (j == 0) {
              pd1.setPlotName("Master plot");
              panel.setMasterPlot(pd1);
            } else {
              pd1.setPlotName("Plot " + (j + 1));
              pd1.m_useCustomColour = true;
              pd1.m_customColour = (j % 2 == 0) ? Color.red : Color.blue;
              panel.addPlot(pd1);
            }
          } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(m_Self, "Error loading file '"
              + files[j] + "':\n" + ex.getMessage());
            return;
          }
        }

        // create frame
        final JFrame frame = Utils.getWekaJFrame("Plot - " + filenames, m_Self);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            m_Frames.remove(frame);
            frame.dispose();
            checkExit();
          }
        });
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(m_Self);
        frame.setVisible(true);
        m_Frames.add(frame);
      }
    });

    // Visualization/ROC
    JMenuItem jMenuItemVisualizationROC = new JMenuItem();
    m_jMenuVisualization.add(jMenuItemVisualizationROC);
    jMenuItemVisualizationROC.setText("ROC");
    // jMenuItemVisualizationROC.setMnemonic('R');
    jMenuItemVisualizationROC.setAccelerator(KeyStroke.getKeyStroke(
      KeyEvent.VK_R, KeyEvent.CTRL_MASK));

    jMenuItemVisualizationROC.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // choose file
        int retVal = m_FileChooserROC.showOpenDialog(m_Self);
        if (retVal != JFileChooser.APPROVE_OPTION) {
          return;
        }

        // create plot
        String filename = m_FileChooserROC.getSelectedFile().getAbsolutePath();
        Instances result = null;
        try {
          result = new Instances(new BufferedReader(new FileReader(filename)));
        } catch (Exception ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(m_Self, "Error loading file '"
            + filename + "':\n" + ex.getMessage());
          return;
        }
        result.setClassIndex(result.numAttributes() - 1);
        ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
        vmc.setROCString("(Area under ROC = "
          + Utils.doubleToString(ThresholdCurve.getROCArea(result), 4) + ")");
        vmc.setName(result.relationName());
        PlotData2D tempd = new PlotData2D(result);
        tempd.setPlotName(result.relationName());
        tempd.addInstanceNumberAttribute();
        try {
          vmc.addPlot(tempd);
        } catch (Exception ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(m_Self,
            "Error adding plot:\n" + ex.getMessage());
          return;
        }

        final JFrame frame = Utils.getWekaJFrame("ROC - " + filename, m_Self);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(vmc, BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            m_Frames.remove(frame);
            frame.dispose();
            checkExit();
          }
        });
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(m_Self);
        frame.setVisible(true);
        m_Frames.add(frame);
      }
    });

    // Visualization/TreeVisualizer
    JMenuItem jMenuItemVisualizationTree = new JMenuItem();
    m_jMenuVisualization.add(jMenuItemVisualizationTree);
    jMenuItemVisualizationTree.setText("TreeVisualizer");
    // jMenuItemVisualizationTree.setMnemonic('T');
    jMenuItemVisualizationTree.setAccelerator(KeyStroke.getKeyStroke(
      KeyEvent.VK_T, KeyEvent.CTRL_MASK));

    jMenuItemVisualizationTree.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // choose file
        int retVal = m_FileChooserTreeVisualizer.showOpenDialog(m_Self);
        if (retVal != JFileChooser.APPROVE_OPTION) {
          return;
        }

        // build tree
        String filename =
          m_FileChooserTreeVisualizer.getSelectedFile().getAbsolutePath();
        TreeBuild builder = new TreeBuild();
        Node top = null;
        NodePlace arrange = new PlaceNode2();
        try {
          top = builder.create(new FileReader(filename));
        } catch (Exception ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(m_Self, "Error loading file '"
            + filename + "':\n" + ex.getMessage());
          return;
        }

        // create frame
        final JFrame frame = Utils.getWekaJFrame("TreeVisualizer - " + filename, m_Self);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new TreeVisualizer(null, top, arrange),
          BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            m_Frames.remove(frame);
            frame.dispose();
            checkExit();
          }
        });
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(m_Self);
        frame.setVisible(true);
        m_Frames.add(frame);
      }
    });

    // Visualization/GraphVisualizer
    JMenuItem jMenuItemVisualizationGraph = new JMenuItem();
    m_jMenuVisualization.add(jMenuItemVisualizationGraph);
    jMenuItemVisualizationGraph.setText("GraphVisualizer");
    // jMenuItemVisualizationGraph.setMnemonic('G');
    jMenuItemVisualizationGraph.setAccelerator(KeyStroke.getKeyStroke(
      KeyEvent.VK_G, KeyEvent.CTRL_MASK));

    jMenuItemVisualizationGraph.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // choose file
        int retVal = m_FileChooserGraphVisualizer.showOpenDialog(m_Self);
        if (retVal != JFileChooser.APPROVE_OPTION) {
          return;
        }

        // build graph
        String filename =
          m_FileChooserGraphVisualizer.getSelectedFile().getAbsolutePath();
        GraphVisualizer panel = new GraphVisualizer();
        try {
          if (filename.toLowerCase().endsWith(".xml")
            || filename.toLowerCase().endsWith(".bif")) {
            panel.readBIF(new FileInputStream(filename));
          } else {
            panel.readDOT(new FileReader(filename));
          }
        } catch (Exception ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(m_Self, "Error loading file '"
            + filename + "':\n" + ex.getMessage());
          return;
        }

        // create frame
        final JFrame frame = Utils.getWekaJFrame("GraphVisualizer - " + filename, m_Self);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            m_Frames.remove(frame);
            frame.dispose();
            checkExit();
          }
        });
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(m_Self);
        frame.setVisible(true);
        m_Frames.add(frame);
      }
    });

    // Visualization/BoundaryVisualizer
    final JMenuItem jMenuItemVisualizationBoundary = new JMenuItem();
    m_jMenuVisualization.add(jMenuItemVisualizationBoundary);
    jMenuItemVisualizationBoundary.setText("BoundaryVisualizer");
    // jMenuItemVisualizationBoundary.setMnemonic('B');
    jMenuItemVisualizationBoundary.setAccelerator(KeyStroke.getKeyStroke(
      KeyEvent.VK_B, KeyEvent.CTRL_MASK));

    jMenuItemVisualizationBoundary.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final JFrame frame = Utils.getWekaJFrame("BoundaryVisualizer", m_Self);
        frame.getContentPane().setLayout(
                new BorderLayout());
        final BoundaryVisualizer bv = new BoundaryVisualizer();
        frame.getContentPane().add(bv,
                BorderLayout.CENTER);
        frame.setSize(bv.getMinimumSize());
        frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent w) {
            bv.stopPlotting();
            frame.dispose();
            m_Frames.remove(frame);
            checkExit();
          }
        });
        frame.pack();
        // frame.setSize(1024, 768);
        frame.setResizable(false);
        frame.setLocationRelativeTo(m_Self);
        frame.setVisible(true);
        m_Frames.add(frame);
        // dont' do a System.exit after last window got closed!
        BoundaryVisualizer.setExitIfNoWindowsOpen(false);
      }
    });

    // Extensions
    JMenu jMenuExtensions = new JMenu("Extensions");
    jMenuExtensions.setMnemonic(java.awt.event.KeyEvent.VK_E);
    m_jMenuBar.add(jMenuExtensions);
    jMenuExtensions.setVisible(false);

    String extensions =
      GenericObjectEditor.EDITOR_PROPERTIES.getProperty(
        MainMenuExtension.class.getName(), "");

    if (extensions.length() > 0) {
      jMenuExtensions.setVisible(true);
      String[] classnames =
        GenericObjectEditor.EDITOR_PROPERTIES.getProperty(
          MainMenuExtension.class.getName(), "").split(",");
      Hashtable<String, JMenu> submenus = new Hashtable<String, JMenu>();

      // add all extensions
      for (String classname : classnames) {
        try {
          MainMenuExtension ext =
            (MainMenuExtension) WekaPackageClassLoaderManager.objectForName(classname);

          // menuitem in a submenu?
          JMenu submenu = null;
          if (ext.getSubmenuTitle() != null) {
            submenu = submenus.get(ext.getSubmenuTitle());
            if (submenu == null) {
              submenu = new JMenu(ext.getSubmenuTitle());
              submenus.put(ext.getSubmenuTitle(), submenu);
              insertMenuItem(jMenuExtensions, submenu);
            }
          }

          // create menu item
          JMenuItem menuitem = new JMenuItem();
          menuitem.setText(ext.getMenuTitle());
          // does the extension need a frame or does it have its own
          // ActionListener?
          ActionListener listener = ext.getActionListener(m_Self);
          if (listener != null) {
            menuitem.addActionListener(listener);
          } else {
            final JMenuItem finalMenuitem = menuitem;
            final MainMenuExtension finalExt = ext;
            menuitem.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Component frame =
                  createFrame(m_Self, finalMenuitem.getText(), null, null,
                    null, -1, -1, null, false, false);
                finalExt.fillFrame(frame);
                frame.setVisible(true);
              }
            });
          }

          // sorted insert of menu item
          if (submenu != null) {
            insertMenuItem(submenu, menuitem);
          } else {
            insertMenuItem(jMenuExtensions, menuitem);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    // Tools
    m_jMenuTools = new JMenu();
    m_jMenuBar.add(m_jMenuTools);
    m_jMenuTools.setText("Tools");
    m_jMenuTools.setMnemonic('T');

    // Package Manager
    final JMenuItem jMenuItemToolsPackageManager = new JMenuItem();
    m_jMenuTools.add(jMenuItemToolsPackageManager);
    final String offline = (WekaPackageManager.m_offline ? " (offline)" : "");
    jMenuItemToolsPackageManager.setText("Package manager" + offline);
    jMenuItemToolsPackageManager.setAccelerator(KeyStroke.getKeyStroke(
      KeyEvent.VK_U, KeyEvent.CTRL_MASK));
    jMenuItemToolsPackageManager.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Thread temp = new Thread() {
          @Override
          public void run() {
            final weka.gui.PackageManager pm;
            pm = new weka.gui.PackageManager();
            if (!WekaPackageManager.m_noPackageMetaDataAvailable) {
              final JFrame frame = Utils.getWekaJFrame("Package Manager" + offline, m_Self);
              frame.getContentPane().setLayout(
                      new BorderLayout());
              frame.getContentPane().add(pm,
                      BorderLayout.CENTER);
              frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent w) {
                  frame.dispose();
                  m_Frames.remove(frame);
                  checkExit();
                }
              });
              frame.pack();
              Dimension screenSize =
                      frame.getToolkit().getScreenSize();
              int width = screenSize.width * 8 / 10;
              int height = screenSize.height * 8 / 10;
              frame.setBounds(width / 8, height / 8, width,
                      height);
              frame.setLocationRelativeTo(m_Self);
              frame.setVisible(true);
              pm.setInitialSplitPaneDividerLocation();
              m_Frames.add(frame);
            }
          }
        };
        temp.start();
      }
    });

    // Tools/ArffViewer
    JMenuItem jMenuItemToolsArffViewer = new JMenuItem();
    m_jMenuTools.add(jMenuItemToolsArffViewer);
    jMenuItemToolsArffViewer.setText("ArffViewer");
    // jMenuItemToolsArffViewer.setMnemonic('A');
    jMenuItemToolsArffViewer.setAccelerator(KeyStroke.getKeyStroke(
      KeyEvent.VK_A, KeyEvent.CTRL_MASK));

    jMenuItemToolsArffViewer.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final ArffViewer av = new ArffViewer();
        av.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent w) {
            m_Frames.remove(av);
            checkExit();
          }
        });
        av.pack();
        av.setSize(1024, 768);
        av.setLocationRelativeTo(m_Self);
        av.setVisible(true);
        m_Frames.add(av);
      }
    });

    // Tools/SqlViewer
    final JMenuItem jMenuItemToolsSql = new JMenuItem();
    m_jMenuTools.add(jMenuItemToolsSql);
    jMenuItemToolsSql.setText("SqlViewer");
    // jMenuItemToolsSql.setMnemonic('S');
    jMenuItemToolsSql.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
      KeyEvent.CTRL_MASK));

    jMenuItemToolsSql.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final JFrame frame = Utils.getWekaJFrame("SqlViewer", m_Self);
        final SqlViewer sql = new SqlViewer(frame);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(sql, BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent w) {
            sql.saveSize();
            frame.dispose();
            m_Frames.remove(frame);
            checkExit();
          }
        });
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(m_Self);
        frame.setVisible(true);
        m_Frames.add(frame);
      }
    });

    // Tools/Bayes net editor
    final JMenuItem jMenuItemBayesNet = new JMenuItem();
    m_jMenuTools.add(jMenuItemBayesNet);
    jMenuItemBayesNet.setText("Bayes net editor");
    jMenuItemBayesNet.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
      KeyEvent.CTRL_MASK));
    jMenuItemBayesNet.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final GUI bayesNetGUI = new GUI();
        JMenuBar bayesBar = bayesNetGUI.getMenuBar();
        final JFrame frame = Utils.getWekaJFrame("Bayes Network Editor", m_Self);
        frame.setJMenuBar(bayesBar);
        frame.getContentPane().add(bayesNetGUI,
                BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent w) {
            frame.dispose();
            m_Frames.remove(frame);
            checkExit();
          }
        });
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(m_Self);
        frame.setVisible(true);
        m_Frames.add(frame);
      }
    });

    // Tools/Groovy console
    if (Groovy.isPresent()) {
      final JMenuItem jMenuItemGroovyConsole = new JMenuItem();
      m_jMenuTools.add(jMenuItemGroovyConsole);
      jMenuItemGroovyConsole.setText("Groovy console");
      jMenuItemGroovyConsole.setAccelerator(KeyStroke.getKeyStroke(
        KeyEvent.VK_G, KeyEvent.CTRL_MASK));
      jMenuItemGroovyConsole.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            Class groovyConsoleClass = WekaPackageClassLoaderManager.forName("groovy.ui.Console");

            if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {

              // Awful hack to prevent the Groovy console from taking over the
              // Mac menu bar.
              // Could potentially cause problems due to multi-threading, but
              // hopefully
              // not problematic in practice.
              String realOS = System.getProperty("os.name");
              System.setProperty("os.name", "pretending_not_to_be_an_apple");
              groovyConsoleClass.getMethod("run").invoke(
                groovyConsoleClass.newInstance());
              System.setProperty("os.name", realOS);
            } else {
              groovyConsoleClass.getMethod("run").invoke(
                groovyConsoleClass.newInstance());
            }
          } catch (Exception ex) {
            System.err.println("Failed to start Groovy console.");
          }
        }
      });
    }

    // Tools/Jython console
    if (Jython.isPresent() || WekaPackageClassLoaderManager.getWekaPackageClassLoaderManager().getPackageClassLoader("tigerjython") != null) {
      final JMenuItem jMenuItemJythonConsole = new JMenuItem();
      m_jMenuTools.add(jMenuItemJythonConsole);
      jMenuItemJythonConsole.setText("Jython console");
      jMenuItemJythonConsole.setAccelerator(KeyStroke.getKeyStroke(
        KeyEvent.VK_J, KeyEvent.CTRL_MASK));
      jMenuItemJythonConsole.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {

          // Do we have TigerJython?
          try {
            ClassLoader tigerLoader = WekaPackageClassLoaderManager.getWekaPackageClassLoaderManager().getPackageClassLoader("tigerjython");
            if (tigerLoader == null) {
              throw new Exception("no tigerjython");
            }
            Class tigerJythonClass =
                    Class.forName("tigerjython.core.TigerJython", true, tigerLoader);
            Object[] args = new Object[1];
            args[0] = new String[0];
            tigerJythonClass.getMethod("main", String[].class).invoke(null,
                    args);
          } catch (Exception ex) {

            // Default to built-in console
            final JythonPanel jythonPanel = new JythonPanel();
            final JFrame frame = Utils.getWekaJFrame(jythonPanel.getPlainTitle(), m_Self);
            frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            frame.setJMenuBar(jythonPanel.getMenuBar());
            frame.getContentPane().add(jythonPanel,
                    BorderLayout.CENTER);
            frame.addWindowListener(new WindowAdapter() {
              @Override
              public void windowClosed(WindowEvent w) {
                m_Frames.remove(frame);
                checkExit();
              }
            });
            frame.pack();
            frame.setSize(1024, 768);
            frame.setLocationRelativeTo(m_Self);
            frame.setVisible(true);
            m_Frames.add(frame);
          }
        }
      });
    }

    // plugins for Visualization and Tools
    Set<String> pluginNames =
      PluginManager
        .getPluginNamesOfType("weka.gui.GUIChooser.GUIChooserMenuPlugin");
    if (pluginNames != null) {
      boolean firstVis = true;
      boolean firstTools = true;
      for (String name : pluginNames) {
        try {
          final GUIChooser.GUIChooserMenuPlugin p =
            (GUIChooser.GUIChooserMenuPlugin) PluginManager.getPluginInstance(
              "weka.gui.GUIChooser.GUIChooserMenuPlugin", name);

          if (p instanceof JComponent) {
            final JMenuItem mItem = new JMenuItem(p.getMenuEntryText());
            mItem.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                final JFrame appFrame = Utils.getWekaJFrame(p.getApplicationName(), m_Self);
                appFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                JMenuBar appMenu = p.getMenuBar();
                if (appMenu != null) {
                  appFrame.setJMenuBar(appMenu);
                }

                appFrame.getContentPane().add((JComponent) p,
                  BorderLayout.CENTER);
                appFrame.addWindowListener(new WindowAdapter() {
                  @Override
                  public void windowClosed(WindowEvent e) {
                    m_Frames.remove(appFrame);
                    checkExit();
                  }
                });
                appFrame.pack();
                appFrame.setSize(1024, 768);
                appFrame.setLocationRelativeTo(m_Self);
                appFrame.setVisible(true);
                m_Frames.add(appFrame);
              }
            });

            if (p.getMenuToDisplayIn() == GUIChooser.GUIChooserMenuPlugin.Menu.VISUALIZATION) {
              if (firstVis) {
                m_jMenuVisualization.add(new JSeparator());
                firstVis = false;
              }
              m_jMenuVisualization.add(mItem);
            } else {
              if (firstTools) {
                m_jMenuTools.add(new JSeparator());
                firstTools = false;
              }
              m_jMenuTools.add(mItem);
            }
          }
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    }

    // Help
    m_jMenuHelp = new JMenu();
    m_jMenuBar.add(m_jMenuHelp);
    m_jMenuHelp.setText("Help");
    m_jMenuHelp.setMnemonic('H');

    // Help/Homepage
    JMenuItem jMenuItemHelpHomepage = new JMenuItem();
    m_jMenuHelp.add(jMenuItemHelpHomepage);
    jMenuItemHelpHomepage.setText("Weka homepage");
    // jMenuItemHelpHomepage.setMnemonic('H');
    jMenuItemHelpHomepage.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H,
      KeyEvent.CTRL_MASK));
    jMenuItemHelpHomepage.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        BrowserHelper.openURL("http://www.cs.waikato.ac.nz/~ml/weka/");
      }
    });

    m_jMenuHelp.add(new JSeparator());

    // Help/WekaWiki
    JMenuItem jMenuItemHelpWekaWiki = new JMenuItem();
    m_jMenuHelp.add(jMenuItemHelpWekaWiki);
    jMenuItemHelpWekaWiki.setText("HOWTOs, code snippets, etc.");
    // jMenuItemHelpWekaWiki.setMnemonic('W');
    jMenuItemHelpWekaWiki.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
      KeyEvent.CTRL_MASK));

    jMenuItemHelpWekaWiki.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        BrowserHelper.openURL("http://weka.wikispaces.com/");
      }
    });

    // Help/Sourceforge
    JMenuItem jMenuItemHelpSourceforge = new JMenuItem();
    m_jMenuHelp.add(jMenuItemHelpSourceforge);
    jMenuItemHelpSourceforge.setText("Weka on Sourceforge");
    // jMenuItemHelpSourceforge.setMnemonic('F');
    jMenuItemHelpSourceforge.setAccelerator(KeyStroke.getKeyStroke(
      KeyEvent.VK_F, KeyEvent.CTRL_MASK));

    jMenuItemHelpSourceforge.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        BrowserHelper.openURL("http://sourceforge.net/projects/weka/");
      }
    });

    // Help/SystemInfo
    final JMenuItem jMenuItemHelpSysInfo = new JMenuItem();
    m_jMenuHelp.add(jMenuItemHelpSysInfo);
    jMenuItemHelpSysInfo.setText("SystemInfo");
    // jMenuItemHelpSysInfo.setMnemonic('S');
    jMenuItemHelpSysInfo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
      KeyEvent.CTRL_MASK));

    jMenuItemHelpSysInfo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final JFrame frame = Utils.getWekaJFrame("SystemInfo", m_Self);
        frame.getContentPane().setLayout(new BorderLayout());

        // get info
        Hashtable<String, String> info = new SystemInfo().getSystemInfo();

        // sort names
        Vector<String> names = new Vector<String>();
        Enumeration<String> enm = info.keys();
        while (enm.hasMoreElements()) {
          names.add(enm.nextElement());
        }
        Collections.sort(names);

        // generate table
        String[][] data = new String[info.size()][2];
        for (int i = 0; i < names.size(); i++) {
          data[i][0] = names.get(i).toString();
          data[i][1] = info.get(data[i][0]).toString();
        }
        String[] titles = new String[]{"Key", "Value"};
        JTable table = new JTable(data, titles);

        frame.getContentPane().add(new JScrollPane(table),
                BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent w) {
            frame.dispose();
            m_Frames.remove(frame);
            checkExit();
          }
        });
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(m_Self);
        frame.setVisible(true);
        m_Frames.add(frame);
      }
    });

    // applications

    m_ExplorerBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showExplorer(null);
      }
    });

    m_ExperimenterBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
          final JFrame frame = Utils.getWekaJFrame("Weka Experiment Environment", m_Self);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new Experimenter(false),
            BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent w) {
              frame.dispose();
              m_Frames.remove(frame);
              checkExit();
            }
          });
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(m_Self);
        frame.setVisible(true);
        m_Frames.add(frame);
      }
      });

    m_KnowledgeFlowBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showKnowledgeFlow(null);
      }
    });

    m_WorkbenchBut.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
          WorkbenchApp app = new WorkbenchApp();
          final JFrame frame = Utils.getWekaJFrame("Weka Workbench", m_Self);
        frame.add(app, BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
              frame.dispose();
              m_Frames.remove(frame);
              checkExit();
            }
          });
          app.showMenuBar(frame);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(m_Self);
        frame.setVisible(true);
        m_Frames.add(frame);
      }
      });

    m_SimpleBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          final JFrame frame = new SimpleCLI();

          frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent w) {
              frame.dispose();
              m_Frames.remove(frame);
              checkExit();
            }
          });
          frame.pack();
          frame.setSize(1024, 768);
          frame.setLocationRelativeTo(m_Self);
          frame.setVisible(true);
          m_Frames.add(frame);
        } catch (Exception ex) {
          throw new Error("Could not start SimpleCLI!");
        }
      }
    });

    /*
     * m_EnsembleLibraryBut.addActionListener(new ActionListener() { public void
     * actionPerformed(ActionEvent e) { if (m_EnsembleLibraryFrame == null) {
     * m_EnsembleLibraryBut.setEnabled(false); m_EnsembleLibraryFrame = new
     * JFrame("EnsembleLibrary"); m_EnsembleLibraryFrame.setIconImage(m_Icon);
     * m_EnsembleLibraryFrame.getContentPane().setLayout(new BorderLayout());
     * EnsembleLibrary value = new EnsembleLibrary(); EnsembleLibraryEditor
     * libraryEditor = new EnsembleLibraryEditor();
     * libraryEditor.setValue(value);
     * m_EnsembleLibraryFrame.getContentPane().add
     * (libraryEditor.getCustomEditor(), BorderLayout.CENTER);
     * m_EnsembleLibraryFrame.addWindowListener(new WindowAdapter() { public
     * void windowClosing(WindowEvent w) { m_EnsembleLibraryFrame.dispose();
     * m_EnsembleLibraryFrame = null; m_EnsembleLibraryBut.setEnabled(true);
     * checkExit(); } }); m_EnsembleLibraryFrame.pack();
     * m_EnsembleLibraryFrame.setSize(1024, 768);
     * m_EnsembleLibraryFrame.setVisible(true); } } });
     */

    setJMenuBar(m_jMenuBar);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent w) {
        dispose();
        checkExit();
      }
    });
    pack();

    if (!Utils.getDontShowDialog("weka.gui.GUIChooser.HowToFindPackageManager")) {
      Thread tipThread = new Thread() {
        @Override
        public void run() {
          JCheckBox dontShow = new JCheckBox("Do not show this message again");
          Object[] stuff = new Object[2];
          stuff[0] =
            "Weka has a package manager that you\n"
              + "can use to install many learning schemes and tools.\nThe package manager can be "
              + "found under the \"Tools\" menu.\n";
          stuff[1] = dontShow;
          // Display the tip on finding/using the package manager
          JOptionPane.showMessageDialog(GUIChooserApp.this, stuff,
            "Weka GUIChooser", JOptionPane.OK_OPTION);

          if (dontShow.isSelected()) {
            try {
              Utils
                .setDontShowDialog("weka.gui.GUIChooser.HowToFindPackageManager");
            } catch (Exception ex) {
              // quietly ignore
            }
          }
        }
      };
      tipThread.setPriority(Thread.MIN_PRIORITY);
      tipThread.start();
    }
  }

  public void showKnowledgeFlow(String fileToLoad) {
    final JFrame frame = Utils.getWekaJFrame("Weka KnowledgeFlow Environment", m_Self);
    frame.getContentPane().setLayout(new BorderLayout());
    final KnowledgeFlowApp knowledgeFlow = new KnowledgeFlowApp();
    frame.getContentPane().add(knowledgeFlow, BorderLayout.CENTER);
    knowledgeFlow.showMenuBar(frame);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent w) {

        ((MainKFPerspective) knowledgeFlow.getMainPerspective())
                .closeAllTabs();
        ((MainKFPerspective) knowledgeFlow.getMainPerspective())
                .addUntitledTab();

          /*
           * kna.closeAllTabs(); kna.clearLayout(); // add a single "Untitled"
           * tab ready for next // time
           */
        frame.dispose();
        m_Frames.remove(frame);
        checkExit();
      }
    });
    frame.pack();
    frame.setSize(1024, 768);
    frame.setLocationRelativeTo(m_Self);
    frame.setVisible(true);
    m_Frames.add(frame);
  }

  public void showExplorer(String fileToLoad) {
    final JFrame frame = Utils.getWekaJFrame("Weka Explorer", m_Self);
    frame.getContentPane().setLayout(new BorderLayout());
    Explorer expl = new Explorer();

    frame.getContentPane().add(expl, BorderLayout.CENTER);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent w) {
        frame.dispose();
        m_Frames.remove(frame);
        checkExit();
      }
    });
    frame.pack();
    frame.setSize(1024, 768);
    frame.setLocationRelativeTo(m_Self);
    frame.setVisible(true);
    m_Frames.add(frame);

    if (fileToLoad != null) {
      try {
        weka.core.converters.AbstractFileLoader loader =
                weka.core.converters.ConverterUtils.getLoaderForFile(fileToLoad);
        loader.setFile(new File(fileToLoad));
        expl.getPreprocessPanel().setInstancesFromFile(loader);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * insert the menu item in a sorted fashion.
   *
   * @param menu the menu to add the item to
   * @param menuitem the menu item to add
   */
  protected void insertMenuItem(JMenu menu, JMenuItem menuitem) {
    insertMenuItem(menu, menuitem, 0);
  }

  /**
   * insert the menu item in a sorted fashion.
   *
   * @param menu the menu to add the item to
   * @param menuitem the menu item to add
   * @param startIndex the index in the menu to start with (0-based)
   */
  protected void insertMenuItem(JMenu menu, JMenuItem menuitem, int startIndex) {
    boolean inserted;
    int i;
    JMenuItem current;
    String currentStr;
    String newStr;

    inserted = false;
    newStr = menuitem.getText().toLowerCase();

    // try to find a spot inbetween
    for (i = startIndex; i < menu.getMenuComponentCount(); i++) {
      if (!(menu.getMenuComponent(i) instanceof JMenuItem)) {
        continue;
      }

      current = (JMenuItem) menu.getMenuComponent(i);
      currentStr = current.getText().toLowerCase();
      if (currentStr.compareTo(newStr) > 0) {
        inserted = true;
        menu.insert(menuitem, i);
        break;
      }
    }

    // add it at the end if not yet inserted
    if (!inserted) {
      menu.add(menuitem);
    }
  }

  /**
   * creates a frame and returns it.
   *
   * @param parent the parent of the generated frame
   * @param title the title of the frame
   * @param c the component to place, can be null
   * @param layout the layout to use, e.g., BorderLayout
   * @param layoutConstraints the layout constraints, e.g., BorderLayout.CENTER
   * @param width the width of the frame, ignored if -1
   * @param height the height of the frame, ignored if -1
   * @param menu an optional menu
   * @param listener if true a default listener is added
   * @param visible if true then the frame is made visible immediately
   * @return the generated frame
   */
  protected Container createFrame(GUIChooserApp parent, String title,
    Component c, LayoutManager layout, Object layoutConstraints, int width,
    int height, JMenuBar menu, boolean listener, boolean visible) {

    Container result = null;

    final ChildFrameSDI frame = new ChildFrameSDI(parent, title);

    // layout
    frame.setLayout(layout);
    if (c != null) {
      frame.getContentPane().add(c, layoutConstraints);
    }

    // menu
    frame.setJMenuBar(menu);

    // size
    frame.pack();
    if ((width > -1) && (height > -1)) {
      frame.setSize(width, height);
    }
    frame.validate();

    // location
    //int screenHeight = getGraphicsConfiguration().getBounds().height;
    //int screenWidth = getGraphicsConfiguration().getBounds().width;
    //frame.setLocation((screenWidth - frame.getBounds().width) / 2,
    //  (screenHeight - frame.getBounds().height) / 2);

    frame.setLocationRelativeTo(parent);

    // listener?
    if (listener) {
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          frame.dispose();
        }
      });
    }

    // display frame
    if (visible) {
      frame.setVisible(true);
    }

    result = frame;

    return result;
  }

  /**
   * Specialized JFrame class.
   *
   * @author fracpete (fracpete at waikato dot ac dot nz)
   * @version $Revision$
   */
  public static class ChildFrameSDI extends JFrame {

    /** for serialization. */
    private static final long serialVersionUID = 8588293938686425618L;

    /** the parent frame. */
    protected GUIChooserApp m_Parent;

    /**
     * constructs a new internal frame that knows about its parent.
     *
     * @param parent the parent frame
     * @param title the title of the frame
     */
    public ChildFrameSDI(GUIChooserApp parent, String title) {
      super(title);

      m_Parent = parent;

      addWindowListener(new WindowAdapter() {
        @Override
        public void windowActivated(WindowEvent e) {
          // update title of parent
          if (getParentFrame() != null) {
            getParentFrame().createTitle(getTitle());
          }
        }
      });

      // add to parent
      if (getParentFrame() != null) {
        getParentFrame().addChildFrame(this);
        setIconImage(getParentFrame().getIconImage());
      }
    }

    /**
     * returns the parent frame, can be null.
     *
     * @return the parent frame
     */
    public GUIChooserApp getParentFrame() {
      return m_Parent;
    }

    /**
     * de-registers the child frame with the parent first.
     */
    @Override
    public void dispose() {
      if (getParentFrame() != null) {
        getParentFrame().removeChildFrame(this);
        getParentFrame().createTitle("");
      }

      super.dispose();
    }
  }

  /**
   * creates and displays the title.
   *
   * @param title the additional part of the title
   */
  protected void createTitle(String title) {
    String newTitle;

    newTitle = "Weka " + new Version();
    if (title.length() != 0) {
      newTitle += " - " + title;
    }

    setTitle(newTitle);
  }

  /**
   * adds the given child frame to the list of frames.
   *
   * @param c the child frame to add
   */
  public void addChildFrame(Container c) {
    m_ChildFrames.add(c);
  }

  /**
   * tries to remove the child frame, it returns true if it could do such.
   *
   * @param c the child frame to remove
   * @return true if the child frame could be removed
   */
  public boolean removeChildFrame(Container c) {
    boolean result = m_ChildFrames.remove(c);
    return result;
  }

  /**
   * Kills the JVM if all windows have been closed.
   */
  private void checkExit() {

    if (!isVisible() && (m_Frames.size() == 0)) {
      System.setSecurityManager(null);
      System.exit(0);
    }
  }

  /**
   * Inner class for defaults
   */
  public static final class GUIChooserDefaults extends Defaults {

    /** APP name (GUIChooser isn't really an "app" as such */
    public static final String APP_NAME = "GUIChooser";

    /** ID */
    public static final String APP_ID = "guichooser";

    /** Settings key for LAF */
    protected static final Settings.SettingKey LAF_KEY =
      new Settings.SettingKey(APP_ID + ".lookAndFeel", "Look and feel for UI",
        "Note: a restart is required for this setting to come into effect");

    /** Default value for LAF */
    protected static final String LAF =
      "javax.swing.plaf.nimbus.NimbusLookAndFeel";

    private static final long serialVersionUID = -8524894440289936685L;

    /**
     * Constructor
     */
    public GUIChooserDefaults() {
      super(APP_ID);
      List<String> lafs = LookAndFeel.getAvailableLookAndFeelClasses();
      lafs.add(0, "<use platform default>");
      LAF_KEY.setPickList(lafs);
      m_defaults.put(LAF_KEY, LAF);
    }
  }

  /**
   * variable for the GUIChooser class which would be set to null by the memory
   * monitoring thread to free up some memory if we running out of memory
   */
  private static GUIChooserApp m_chooser;

  /** for monitoring the Memory consumption */
  private static Memory m_Memory = new Memory(true);

  /**
   * Tests out the GUIChooser environment.
   *
   * @param args ignored.
   */
  public static void main(String[] args) {

    weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
      "Logging started");
    try {
      LookAndFeel.setLookAndFeel(GUIChooserDefaults.APP_ID,
        GUIChooserDefaults.APP_ID + ".lookAndFeel", GUIChooserDefaults.LAF);
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    // Save std err and std out because they may be redirected by external code
    final PrintStream savedStdOut = System.out;
    final PrintStream savedStdErr = System.err;

    // Set up security manager to intercept System.exit() calls in external code
    final SecurityManager sm = System.getSecurityManager();
    System.setSecurityManager(new SecurityManager() {
      public void checkExit(int status) {
        if (sm != null) {
          sm.checkExit(status);
        }

        // Currently, we are just checking for calls from TigerJython code
        for (Class cl : getClassContext()) {
          if (cl.getName().equals("tigerjython.gui.MainWindow")) {
            for (Frame frame : Frame.getFrames()) {
              if (frame.getTitle().toLowerCase().startsWith("tigerjython")) {
                frame.dispose();
              }
            }

            // Set std err and std out back to original values
            System.setOut(savedStdOut);
            System.setErr(savedStdErr);

            // Make entry in log and
            /*
             * weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
             * "Intercepted System.exit() from TigerJython. Please ignore");
             * throw new SecurityException(
             * "Intercepted System.exit() from TigerJython. Please ignore!");
             */
          }
        }

        weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
          "Intercepted System.exit() from a class other than the GUIChooser. "
            + "Please ignore.");
        throw new SecurityException("Intercepted System.exit() from "
          + "a class other than the GUIChooser. Please ignore.");
      }

      public void checkPermission(Permission perm) {
        if (sm != null) {
          sm.checkPermission(perm);
        }
      }

      public void checkPermission(Permission perm, Object context) {
        if (sm != null) {
          sm.checkPermission(perm, context);
        }
      }
    });

    try {

      // uncomment to disable the memory management:
      // m_Memory.setEnabled(false);
      // m_chooser = new GUIChooser();
      GUIChooserApp.createSingleton();
      m_chooser.pack();
      m_chooser.setSize(500, 350);
      m_chooser.setVisible(true);

      if (args != null && args.length > 0) {
        m_chooser.showExplorer(args[0]);
      }

      Thread memMonitor = new Thread() {
        @SuppressWarnings("static-access")
        @Override
        public void run() {
          while (true) {
            // try {
            // System.out.println("before sleeping");
            // this.sleep(10);

            if (m_Memory.isOutOfMemory()) {
              // clean up
              m_chooser.dispose();

              if (m_chooser.m_Frames.size() > 0) {
                for (int i = 0; i < m_chooser.m_Frames.size(); i++) {
                  JFrame av = m_chooser.m_Frames.get(i);
                  av.dispose();
                }
                m_chooser.m_Frames.clear();
              }
              m_chooser = null;
              System.gc();

              // display error
              GUIChooserApp.m_LogWindow.pack();
              GUIChooserApp.m_LogWindow.setSize(1024,768);
              GUIChooserApp.m_LogWindow.setVisible(true);
              GUIChooserApp.m_LogWindow.toFront();
              System.err.println("\ndisplayed message:");
              m_Memory.showOutOfMemory();
              System.err.println("\nexiting...");
              System.setSecurityManager(null);
              System.exit(-1);
            }
            // } catch (InterruptedException ex) {
            // ex.printStackTrace();
            // }
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
