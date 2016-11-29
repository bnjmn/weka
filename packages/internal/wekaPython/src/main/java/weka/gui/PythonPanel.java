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
 *    PythonPanel.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jsyntaxpane.DefaultSyntaxKit;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.explorer.Explorer;
import weka.python.PythonSession;

/**
 * Panel for interacting with python. Provides functionality for executing
 * scripts, listing variables that are set in python and retrieving text and
 * image data from python.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class PythonPanel extends JPanel {

  private static final long serialVersionUID = -6294585211141702994L;

  /** Path for loading widget icons */
  protected static final String ICON_PATH = "weka/gui/icons/";

  /** The Explorer instance (if we are embedded in an Explorer plugin) */
  protected Explorer m_explorer;

  /** The script editor component */
  protected JEditorPane m_scriptEditor;

  /** List of variables that exist in python */
  protected JList<String> m_pyVarList = new JList<String>(
    new DefaultListModel<String>());

  /** Button for executing a script */
  protected JButton m_executeScriptBut = new JButton("Execute script");

  /** Button for getting the value of a python variable in text form */
  protected JButton m_getVarAsText = new JButton("Get text");

  /** Checkbox to turn on debugging output */
  protected JCheckBox m_debug = new JCheckBox("Output debug info to log");

  /**
   * Button for getting the value of a python variable (dataframe) as a set of
   * instances
   */
  protected JButton m_getVarAsInstances = new JButton("Get instances");

  /** Button for getting the value of a python variable as an image */
  protected JButton m_getVarAsImage = new JButton("Get image");

  /** Holds the textual output buffers (one entry for each tab) */
  protected List<JTextArea> m_textOutputBuffers = new ArrayList<JTextArea>();

  /** Holds the image buffers (one entry for each tab) */
  protected List<ImageDisplayer> m_imageOutputBuffers =
    new ArrayList<ImageDisplayer>();

  /** File chooser for loading/saving stuff */
  protected final JFileChooser m_fileChooser = new JFileChooser();

  /** Logger to use */
  protected Logger m_logPanel = new LogPanel(null, true, false, false);

  /** Textual output tabbed pane */
  protected JTabbedPane m_outputTabs = new JTabbedPane();

  /** Image tabbed pane */
  protected JTabbedPane m_imageTabs = new JTabbedPane();

  /** Whether python is available or not */
  private boolean m_pyAvailable = true;

  /**
   * Results from evaluating what packages etc. are installed in the python
   * environment
   */
  private String m_envEvalResults;

  /** Any exception thrown when initializing the python environment */
  private Exception m_envEvalException;

  /**
   * Inner class for displaying a BufferedImage.
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  private class ImageDisplayer extends JPanel {

    /** For serialization */
    private static final long serialVersionUID = 4161957589912537357L;

    /** The image to display */
    private BufferedImage m_image;

    /**
     * Set the image to display
     *
     * @param image the image to display
     */
    public void setImage(BufferedImage image) {
      m_image = image;
    }

    /**
     * Get the image being displayed
     *
     * @return the image being displayed
     */
    public BufferedImage getImage() {
      return m_image;
    }

    /**
     * Render the image
     *
     * @param g the graphics context
     */
    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      if (m_image != null) {
        int plotWidth = m_image.getWidth();
        int plotHeight = m_image.getHeight();

        int ourWidth = getWidth();
        int ourHeight = getHeight();

        // center if plot is smaller than us
        int x = 0, y = 0;
        if (plotWidth < ourWidth) {
          x = (ourWidth - plotWidth) / 2;
        }
        if (plotHeight < ourHeight) {
          y = (ourHeight - plotHeight) / 2;
        }

        g.drawImage(m_image, x, y, this);
      }
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      if (m_image != null) {
        int plotWidth = m_image.getWidth();
        int plotHeight = m_image.getHeight();

        d.setSize(plotWidth, plotHeight);
      }
      return d;
    }
  }

  /**
   * Creates the split pane that holds the script edtor and variable list
   *
   * @return a JSplitPane
   */
  protected JSplitPane createEditorAndVarPanel() {
    m_fileChooser.setAcceptAllFileFilterUsed(true);
    m_fileChooser.setMultiSelectionEnabled(false);
    ClassLoader orig = Thread.currentThread().getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
      DefaultSyntaxKit.initKit();
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }
    m_scriptEditor = new JEditorPane();
    JPanel scriptHolder = new JPanel(new BorderLayout());
    JToolBar toolBar = new JToolBar();
    toolBar.setOrientation(JToolBar.HORIZONTAL);
    JButton newB =
      new JButton(
        new ImageIcon(loadIcon(ICON_PATH + "page_add.png").getImage()));
    toolBar.add(newB);
    newB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_scriptEditor.setText("");
      }
    });
    newB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    newB.setToolTipText("New (clear script)");
    JButton loadB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "folder_add.png")
        .getImage()));
    loadB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int retVal = m_fileChooser.showOpenDialog(PythonPanel.this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
          // boolean ok = m_script.open(fileChooser.getSelectedFile());
          StringBuilder sb = new StringBuilder();
          try {
            BufferedReader br =
              new BufferedReader(
                new FileReader(m_fileChooser.getSelectedFile()));
            String line;
            while ((line = br.readLine()) != null) {
              sb.append(line).append("\n");
            }
            m_scriptEditor.setText(sb.toString());
            br.close();
          } catch (Exception ex) {
            JOptionPane.showMessageDialog(PythonPanel.this,
              "Couldn't open file '" + m_fileChooser.getSelectedFile() + "'!");
            ex.printStackTrace();
          }
        }
      }
    });
    loadB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    loadB.setToolTipText("Load a script");
    toolBar.add(loadB);
    JButton saveB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "disk.png").getImage()));
    saveB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // save(fileChooser);
        if (m_scriptEditor.getText() != null
          && m_scriptEditor.getText().length() > 0) {

          int retVal = m_fileChooser.showSaveDialog(PythonPanel.this);
          if (retVal == JFileChooser.APPROVE_OPTION) {
            try {
              BufferedWriter bw =
                new BufferedWriter(new FileWriter(m_fileChooser
                  .getSelectedFile()));
              bw.write(m_scriptEditor.getText());
              bw.flush();
              bw.close();
            } catch (Exception ex) {
              JOptionPane.showMessageDialog(
                PythonPanel.this,
                "Unable to save script file '"
                  + m_fileChooser.getSelectedFile() + "'!");
              ex.printStackTrace();
            }
          }
        }
      }
    });
    saveB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    saveB.setToolTipText("Save script");
    toolBar.add(saveB);
    scriptHolder.add(toolBar, BorderLayout.NORTH);
    scriptHolder.add(new JScrollPane(m_scriptEditor), BorderLayout.CENTER);
    scriptHolder.setBorder(BorderFactory.createTitledBorder("Python script"));
    JPanel butHolder1 = new JPanel();
    butHolder1.add(m_executeScriptBut);
    butHolder1.add(m_debug);
    scriptHolder.add(butHolder1, BorderLayout.SOUTH);
    m_scriptEditor.setContentType("text/python");
    JPanel varHolder = new JPanel(new BorderLayout());
    varHolder.setBorder(BorderFactory.createTitledBorder("Python variables"));
    varHolder.add(new JScrollPane(m_pyVarList), BorderLayout.CENTER);
    JPanel butHolder2 = new JPanel();
    butHolder2.add(m_getVarAsText);
    if (m_explorer != null) {
      butHolder2.add(m_getVarAsInstances);
    }
    butHolder2.add(m_getVarAsImage);
    varHolder.add(butHolder2, BorderLayout.SOUTH);

    JSplitPane pane =
      new JSplitPane(JSplitPane.VERTICAL_SPLIT, scriptHolder, varHolder);

    m_getVarAsText.setEnabled(false);
    m_getVarAsImage.setEnabled(false);
    m_getVarAsInstances.setEnabled(false);

    pane.setResizeWeight(0.8);

    Dimension d = new Dimension(450, 200);
    m_scriptEditor.setMinimumSize(d);

    return pane;
  }

  /**
   * Creates the split pane that contains the textual and image outputs
   *
   * @return a JSplitPane
   */
  protected JSplitPane createOutputPanel() {
    JPanel outputHolder = new JPanel(new BorderLayout());
    outputHolder.add(m_outputTabs, BorderLayout.CENTER);
    outputHolder.setBorder(BorderFactory.createTitledBorder("Output"));
    JToolBar outputToolBar = new JToolBar();
    outputToolBar.setOrientation(JToolBar.HORIZONTAL);
    JButton saveOutputB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "disk.png").getImage()));
    saveOutputB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    saveOutputB.setToolTipText("Save output");
    outputToolBar.add(saveOutputB);

    saveOutputB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_outputTabs.getTabCount() > 0) {

          int retVal = m_fileChooser.showSaveDialog(PythonPanel.this);
          if (retVal == JFileChooser.APPROVE_OPTION) {
            try {
              String outputToSave =
                m_textOutputBuffers.get(m_outputTabs.getSelectedIndex())
                  .getText();
              BufferedWriter bw =
                new BufferedWriter(new FileWriter(m_fileChooser
                  .getSelectedFile()));
              bw.write(outputToSave);
              bw.flush();
              bw.close();
            } catch (Exception ex) {
              JOptionPane.showMessageDialog(
                PythonPanel.this,
                "Unable to save output buffer '"
                  + m_outputTabs.getTitleAt(m_outputTabs.getSelectedIndex())
                  + "'!");
              ex.printStackTrace();
            }
          }
        }
      }
    });

    outputHolder.add(outputToolBar, BorderLayout.NORTH);

    JPanel imageHolder = new JPanel(new BorderLayout());
    imageHolder.add(m_imageTabs, BorderLayout.CENTER);
    imageHolder.setBorder(BorderFactory.createTitledBorder("Plots"));
    JToolBar imageToolBar = new JToolBar();
    imageToolBar.setOrientation(JToolBar.HORIZONTAL);
    JButton saveImageB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "disk.png").getImage()));
    saveImageB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    saveImageB.setToolTipText("Save image");
    imageToolBar.add(saveImageB);
    saveImageB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_outputTabs.getTabCount() > 0) {
          int retVal = m_fileChooser.showSaveDialog(PythonPanel.this);
          if (retVal == JFileChooser.APPROVE_OPTION) {
            try {
              String fileName = m_fileChooser.getSelectedFile().toString();
              if (!fileName.toLowerCase().endsWith(".png")) {
                fileName += ".png";
              }
              ImageIO.write(
                m_imageOutputBuffers.get(m_outputTabs.getSelectedIndex())
                  .getImage(), "png", new File(fileName));
            } catch (Exception ex) {
              JOptionPane.showMessageDialog(
                PythonPanel.this,
                "Unable to save image '"
                  + m_imageTabs.getTitleAt(m_imageTabs.getSelectedIndex())
                  + "'!");
              ex.printStackTrace();
            }
          }
        }
      }
    });
    imageHolder.add(imageToolBar, BorderLayout.NORTH);
    JSplitPane pane =
      new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputHolder, imageHolder);

    pane.setResizeWeight(0.5);
    pane.setOneTouchExpandable(true);
    return pane;
  }

  /**
   * Constructor
   * 
   * @param displayLogger true if a log panel is to be displayed at the bottom
   *          of this panel. Not needed in the Explorer as we get passed the
   *          Explorer's global log
   * @param explorer holds the Explorer instance (will be null if we're not
   *          being used in the Explorer)
   */
  public PythonPanel(boolean displayLogger, Explorer explorer) {
    m_explorer = explorer;
    setLayout(new BorderLayout());
    m_getVarAsText.setToolTipText("Get the value of the selected variable in "
      + "textual form");
    m_getVarAsImage
      .setToolTipText("Get the value of the selected variable as a"
        + " png image");
    m_getVarAsInstances
      .setToolTipText("Get the selected variable (must be "
        + "a DataFrame) as a set of instances. Gets passed to the Preprocess panel "
        + "of the Explorer");

    // check python availability
    m_pyAvailable = true;
    if (!PythonSession.pythonAvailable()) {
      // try initializing
      try {
        if (!PythonSession.initSession("python", m_debug.isSelected())) {
          m_envEvalResults = PythonSession.getPythonEnvCheckResults();
          m_pyAvailable = false;
        }
      } catch (Exception ex) {
        m_envEvalException = ex;
        m_pyAvailable = false;
        ex.printStackTrace();
        logMessage(null, ex);
      }
    }

    if (!m_pyAvailable) {
      System.err
        .println("Python is not available!!\n\n" + m_envEvalResults != null ? m_envEvalResults
          : "");
      if (m_logPanel != null) {
        logMessage("Python is not available!!", m_envEvalException);
        if (m_envEvalResults != null) {
          logMessage(m_envEvalResults, null);
        }
      }
    }

    JSplitPane scriptAndVars = createEditorAndVarPanel();
    JSplitPane output = createOutputPanel();
    JSplitPane pane =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scriptAndVars, output);
    pane.setOneTouchExpandable(true);
    add(pane, BorderLayout.CENTER);
    if (displayLogger) {
      add((JPanel) m_logPanel, BorderLayout.SOUTH);
    }

    if (m_pyAvailable) {
      m_pyVarList.addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          int index = m_pyVarList.getSelectedIndex();
          m_getVarAsImage.setEnabled(false);
          m_getVarAsInstances.setEnabled(false);
          m_getVarAsText.setEnabled(false);
          if (index >= 0) {
            String entry = m_pyVarList.getSelectedValue();
            String varType = entry.split(":")[1].trim();
            m_getVarAsText.setEnabled(true); // can always get as text
            if (varType.toLowerCase().startsWith("dataframe")) {
              m_getVarAsInstances.setEnabled(true);
            }
            if (varType.toLowerCase().startsWith("figure")) {
              m_getVarAsImage.setEnabled(true);
            }
          }
        }
      });

      m_executeScriptBut.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Runnable newT = new Runnable() {
            public void run() {
              PythonSession session;
              try {
                session = PythonSession.acquireSession(PythonPanel.this);
                List<String> outAndErr =
                  session.executeScript(m_scriptEditor.getText(),
                    m_debug.isSelected());
                if (outAndErr.get(0).length() > 0) {
                  logMessage(outAndErr.get(0), null);
                }
                if (outAndErr.get(1).length() > 0) {
                  throw new WekaException(outAndErr.get(1));
                }
                refreshVarList(session);
                checkDebug(session);
                m_logPanel.statusMessage("OK");
              } catch (WekaException ex) {
                ex.printStackTrace();
                logMessage(null, ex);
                m_logPanel.statusMessage("An error occurred. See log.");
              } finally {
                PythonSession.releaseSession(PythonPanel.this);
                m_executeScriptBut.setEnabled(true);
                revalidate();
              }
            }
          };
          m_executeScriptBut.setEnabled(false);
          m_logPanel.statusMessage("Executing script...");
          SwingUtilities.invokeLater(newT);
        }
      });

      m_getVarAsInstances.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          PythonSession session;
          try {
            session = PythonSession.acquireSession(PythonPanel.this);
            if (m_logPanel != null) {
              session.setLog(m_logPanel);
            }
            String varToGet = m_pyVarList.getSelectedValue();
            varToGet = varToGet.split(":")[0].trim();
            if (varToGet.length() > 0) {
              Instances instances =
                session.getDataFrameAsInstances(varToGet, m_debug.isSelected());
              m_explorer.getPreprocessPanel().setInstances(instances);
            }
            checkDebug(session);
          } catch (WekaException ex) {
            ex.printStackTrace();
            logMessage(null, ex);
          } finally {
            PythonSession.releaseSession(PythonPanel.this);
          }
        }
      });

      m_getVarAsText.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          PythonSession session;
          try {
            session = PythonSession.acquireSession(PythonPanel.this);
            if (m_logPanel != null) {
              session.setLog(m_logPanel);
            }
            String varToGet = m_pyVarList.getSelectedValue();
            varToGet = varToGet.split(":")[0].trim();
            if (varToGet.length() > 0) {
              String result =
                session.getVariableValueFromPythonAsPlainString(varToGet,
                  m_debug.isSelected());
              JTextArea textArea = getNamedTextOutput(varToGet);
              if (textArea == null) {
                textArea = new JTextArea();
                textArea.setFont(m_scriptEditor.getFont());
                textArea.setEditable(false);
                m_outputTabs.addTab(varToGet, new JScrollPane(textArea));
                m_textOutputBuffers.add(textArea);
                m_outputTabs.setTabComponentAt(m_outputTabs.getTabCount() - 1,
                  new CloseableTabTitle(m_outputTabs, null,
                    new CloseableTabTitle.ClosingCallback() {
                      @Override
                      public void tabClosing(int tabIndex) {
                        m_outputTabs.remove(tabIndex);
                        m_textOutputBuffers.remove(tabIndex);
                      }
                    }));
              }
              textArea.setText(result);
              setVisibleTab(m_outputTabs, varToGet);
              checkDebug(session);
            }
          } catch (WekaException ex) {
            ex.printStackTrace();
            logMessage(null, ex);
          } finally {
            PythonSession.releaseSession(PythonPanel.this);
          }
        }
      });

      m_getVarAsImage.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          PythonSession session;
          try {
            session = PythonSession.acquireSession(PythonPanel.this);
            if (m_logPanel != null) {
              session.setLog(m_logPanel);
            }
            String varToGet = m_pyVarList.getSelectedValue();
            varToGet = varToGet.split(":")[0].trim();
            if (varToGet.length() > 0) {
              BufferedImage result =
                session.getImageFromPython(varToGet, m_debug.isSelected());
              ImageDisplayer displayer = getNamedImageOutput(varToGet);
              if (displayer == null) {
                displayer = new ImageDisplayer();
                m_imageTabs.addTab(varToGet, new JScrollPane(displayer));
                m_imageOutputBuffers.add(displayer);
                m_imageTabs.setTabComponentAt(m_imageTabs.getTabCount() - 1,
                  new CloseableTabTitle(m_imageTabs, null,
                    new CloseableTabTitle.ClosingCallback() {
                      @Override
                      public void tabClosing(int tabIndex) {
                        m_imageTabs.remove(tabIndex);
                        m_imageOutputBuffers.remove(tabIndex);
                      }
                    }));
              }
              displayer.setImage(result);
              displayer.repaint();
              setVisibleTab(m_imageTabs, varToGet);
              checkDebug(session);
            }
          } catch (WekaException ex) {
            ex.printStackTrace();
            logMessage(null, ex);
          } finally {
            PythonSession.releaseSession(PythonPanel.this);
          }
        }
      });
    } else {
      m_executeScriptBut.setEnabled(false);
      JTextArea textArea = new JTextArea();
      textArea.setFont(m_scriptEditor.getFont());
      textArea.setEditable(false);
      StringBuilder b = new StringBuilder();
      b.append("The python environment is not available:\n\n");
      if (m_envEvalResults != null && m_envEvalResults.length() > 0) {
        b.append(m_envEvalResults).append("\n\n");
      }
      if (m_envEvalException != null) {
        b.append(m_envEvalException.getMessage());
      }
      textArea.setText(b.toString());
      m_outputTabs.addTab("Python not available", new JScrollPane(textArea));
      m_textOutputBuffers.add(textArea);
    }
  }

  /**
   * Selects a tab in a tabbed pane given the name of the tab to display.
   *
   * @param tabWidget the JTabbedPane to operate on
   * @param title the title of the tab to make the visible one
   */
  protected void setVisibleTab(JTabbedPane tabWidget, String title) {
    int index = -1;
    for (int i = 0; i < tabWidget.getTabCount(); i++) {
      if (tabWidget.getTitleAt(i).equals(title)) {
        index = i;
        break;
      }
    }

    if (index > -1) {
      tabWidget.setSelectedIndex(index);
    }
  }

  /**
   * Returns the named JTextArea from the textual output buffers
   *
   * @param name the name of the JTextArea to get
   * @return the named JTextArea, or null if unknown
   */
  protected JTextArea getNamedTextOutput(String name) {
    JTextArea result = null;

    for (int i = 0; i < m_outputTabs.getTabCount(); i++) {
      if (m_outputTabs.getTitleAt(i).equals(name)) {
        result = m_textOutputBuffers.get(i);
        break;
      }
    }
    return result;
  }

  /**
   * Get the named ImageDisplayer from the image buffers
   *
   * @param name the name of the ImageDisplayer to get
   * @return the named ImageDisplayer, or null if unknown
   */
  protected ImageDisplayer getNamedImageOutput(String name) {
    ImageDisplayer result = null;

    for (int i = 0; i < m_imageTabs.getTabCount(); i++) {
      if (m_imageTabs.getTitleAt(i).equals(name)) {
        result = m_imageOutputBuffers.get(i);
        break;
      }
    }
    return result;
  }

  /**
   * Set the log to use
   *
   * @param log the log to use
   */
  public void setLogger(Logger log) {
    m_logPanel = log;
    if (!m_pyAvailable) {
      m_logPanel.logMessage("Python is not available!!");
      if (m_envEvalResults != null) {
        m_logPanel.logMessage(m_envEvalResults);
      }
      if (m_envEvalException != null) {
        m_logPanel.logMessage(m_envEvalException.getMessage());
      }
    }
  }

  /**
   * Send a set of instances to the python environment. A variable, called
   * py_data, is created that points to a pnadas dataframe.
   *
   * @param instances the instances to transfer
   * @throws WekaException if a problem occurs
   */
  public void sendInstancesToPython(Instances instances) throws WekaException {
    if (m_pyAvailable) {
      PythonSession session;
      try {
        session = PythonSession.acquireSession(this);
        if (m_logPanel != null) {
          session.setLog(m_logPanel);
        }
        session.instancesToPython(instances, "py_data", m_debug.isSelected());
        if (m_logPanel != null) {
          m_logPanel.statusMessage("Transferred " + instances.relationName()
            + " into Python as 'py_data'");
          logMessage("Transferred " + instances.relationName()
            + " into Python as 'py_data'", null);
        }
        refreshVarList(session);
        checkDebug(session);
      } catch (WekaException ex) {
        ex.printStackTrace();
        logMessage(null, ex);
      } finally {
        PythonSession.releaseSession(this);
      }
    }
  }

  /**
   * Log a message and/or exception to the log.
   *
   * @param message the message to log (can be null)
   * @param ex the exception to log (can be null)
   */
  protected void logMessage(String message, Exception ex) {
    if (m_logPanel != null) {
      if (message != null) {
        m_logPanel.logMessage(message);
      }
      if (ex != null) {
        m_logPanel.logMessage(ex.getMessage());
      }
    }
  }

  /**
   * Checks if debug mode is turned on - if so, retrieves the output and error
   * buffers from python and displays them in the log
   *
   * @param session the PythonSession to use
   * @throws WekaException if a problem occurs
   */
  protected void checkDebug(PythonSession session) throws WekaException {
    if (m_debug.isSelected()) {
      List<String> outAndError = session.getPythonDebugBuffer(true);
      if (outAndError.get(0).length() > 0) {
        logMessage(outAndError.get(0), null);
      }
      if (outAndError.get(1).length() > 0) {
        logMessage(outAndError.get(1), null);
      }
    }
  }

  /**
   * Refresh the list of python variables.
   * 
   * @param session the session to use (can be null, in which case a new one is
   *          acquired)
   * @throws WekaException if a problem occurs
   */
  protected void refreshVarList(PythonSession session) throws WekaException {
    boolean sessionSupplied = session != null;
    if (session == null) {
      session = PythonSession.acquireSession(this);
    }

    try {
      ((DefaultListModel<String>) m_pyVarList.getModel()).removeAllElements();
      m_getVarAsInstances.setEnabled(false);
      m_getVarAsImage.setEnabled(false);
      m_getVarAsText.setEnabled(false);
      List<String[]> variableList =
        session.getVariableListFromPython(m_debug.isSelected());
      for (String[] v : variableList) {
        String varDescription = v[0] + ": " + v[1];
        ((DefaultListModel<String>) m_pyVarList.getModel())
          .addElement(varDescription);
      }
    } finally {
      if (!sessionSupplied) {
        PythonSession.releaseSession(this);
      }
    }
  }

  /**
   * Static utility for loading icons from the classpath
   *
   * @param iconPath the path to the icon
   * @return the icon
   */
  public static ImageIcon loadIcon(String iconPath) {
    java.net.URL imageURL =
      PythonSession.class.getClassLoader().getResource(iconPath);

    if (imageURL != null) {
      Image pic = Toolkit.getDefaultToolkit().getImage(imageURL);
      return new ImageIcon(pic);
    }

    throw new IllegalArgumentException("Unable to load icon: " + iconPath);
  }
}
