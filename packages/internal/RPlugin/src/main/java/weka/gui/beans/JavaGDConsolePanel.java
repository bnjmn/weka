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
 *    JavaGDConsolePanel.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;

import weka.core.Environment;
import weka.core.Instances;
import weka.core.JavaGDListener;
import weka.core.JavaGDOffscreenRenderer;
import weka.core.RSession;
import weka.core.RUtils;
import weka.core.Utils;
import weka.gui.Logger;
import weka.gui.ResultHistoryPanel;
import weka.gui.visualize.VisualizeUtils;

/**
 * Implements a panel containing an area for displaying R graphics produced by
 * the JavaGD graphics device and also a general R console for interactively
 * evaluating R commands.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class JavaGDConsolePanel extends JPanel implements JavaGDListener {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -5627826390585643774L;

  /**
   * Implements a logger for status messages only.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  private class StatusOnlyLogger extends JPanel implements Logger {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -1420821275378202329L;
    private final JLabel m_status = new JLabel("Ready.");

    public StatusOnlyLogger() {
      setLayout(new BorderLayout());
      m_status.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Status"),
        BorderFactory.createEmptyBorder(0, 5, 5, 5)));
      add(m_status, BorderLayout.CENTER);
    }

    /**
     * Log a message. Not used in this logger.
     * 
     * @param message the message to log
     */
    @Override
    public void logMessage(String message) {
    }

    /**
     * Display a status message.
     * 
     * @param message the message to display
     */
    @Override
    public void statusMessage(String message) {
      m_status.setText(message);
    }
  }

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
  }

  private boolean m_rAvailable = true;

  /** editor setup */
  public final static String PROPERTIES_FILE = "weka/gui/beans/R.props";

  /** Panel for displaying the image */
  protected ImageDisplayer m_plotter;

  /** Keeps a history of the images received */
  protected ResultHistoryPanel m_history;

  /** The console */
  protected JTextPane m_rConsole;

  /** Command history - accessible with up and down arrows */
  protected List<String> m_historyBuffer = new ArrayList<String>();

  /** Current position in the history */
  protected int m_historyPos = 0;

  // protected JLabel m_statusLab = new JLabel("Ready.");

  /** Log status messages */
  protected Logger m_statusLogger = new StatusOnlyLogger();

  /** Thread for executing the current command */
  protected Thread m_executor = null;

  /**
   * Document filter that only allows text to be inserted/removed if the cursor
   * is beyond the prompt position
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  private class Filter extends DocumentFilter {
    @Override
    public void insertString(final FilterBypass fb, final int offset,
      final String string, final AttributeSet attr) throws BadLocationException {
      int promptPos = m_rConsole.getText().lastIndexOf(">>> ") + 4;
      if (offset >= promptPos) {
        super.insertString(fb, offset, string, attr);
      }
    }

    @Override
    public void remove(final FilterBypass fb, final int offset, final int length)
      throws BadLocationException {
      int promptPos = m_rConsole.getText().lastIndexOf(">>> ") + 4;
      if (offset >= promptPos) {
        super.remove(fb, offset, length);
      }
    }

    @Override
    public void replace(final FilterBypass fb, final int offset,
      final int length, final String text, final AttributeSet attrs)
      throws BadLocationException {
      int promptPos = m_rConsole.getText().lastIndexOf(">>> ") + 4;
      if (offset >= promptPos) {
        super.replace(fb, offset, length, text, attrs);
      }
    }
  }

  /**
   * Construct a new JavaGDConsolePanel
   */
  public JavaGDConsolePanel() {
    this(false);
  }

  /**
   * Construct a new JavaGDConsolePanel
   * 
   * @param displayLogger whether to display the status logger or not
   */
  public JavaGDConsolePanel(boolean displayLogger) {
    setLayout(new BorderLayout());

    JPanel topP = new JPanel();
    topP.setLayout(new BorderLayout());
    m_plotter = new ImageDisplayer();
    m_plotter.setBorder(BorderFactory.createTitledBorder("R plot"));
    m_plotter.setMinimumSize(new Dimension(810, 610));
    m_plotter.setPreferredSize(new Dimension(810, 610));
    topP.add(new JScrollPane(m_plotter), BorderLayout.CENTER);

    m_history = new ResultHistoryPanel(null);
    m_history.setHandleRightClicks(false);
    m_history.setBorder(BorderFactory.createTitledBorder("Graph history"));

    m_history.getList().addMouseListener(
      new ResultHistoryPanel.RMouseAdapter() {
        /**
         * For serialization
         */
        private static final long serialVersionUID = -8727624332408164442L;

        @Override
        public void mouseReleased(MouseEvent e) {
          int index = m_history.getList().locationToIndex(e.getPoint());
          if (index != -1) {
            String name = m_history.getNameAtIndex(index);

            if (((e.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK)
              || e.isAltDown()) {
              visualize(name, e.getX(), e.getY());
            } else {

              Object pic = m_history.getNamedObject(name);
              if (pic instanceof BufferedImage) {
                m_plotter.setImage((BufferedImage) pic);
                m_plotter.repaint();
              }
            }
          }
        }
      });

    topP.add(m_history, BorderLayout.WEST);

    Properties props = null;

    try {
      props = Utils.readProperties(PROPERTIES_FILE);
    } catch (Exception ex) {
      ex.printStackTrace();
      props = new Properties();
    }
    final Properties propsCopy = props;

    // check for SyntaxDocument
    boolean syntaxDocAvailable = true;
    try {
      Class.forName("weka.gui.scripting.SyntaxDocument");
    } catch (Exception ex) {
      syntaxDocAvailable = false;
    }

    m_rConsole = new JTextPane();
    if (props.getProperty("Syntax", "false").equals("true")
      && syntaxDocAvailable) {

      try {
        Class syntaxClass = Class.forName("weka.gui.scripting.SyntaxDocument");
        Constructor constructor = syntaxClass.getConstructor(Properties.class);
        Object doc = constructor.newInstance(props);
        m_rConsole.setDocument((DefaultStyledDocument) doc);
        m_rConsole.setBackground(VisualizeUtils.processColour(
          props.getProperty("BackgroundColor", "white"), Color.WHITE));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      /*
       * SyntaxDocument doc = new SyntaxDocument(props);
       * m_rConsole.setDocument(doc);
       * m_rConsole.setBackground(doc.getBackgroundColor());
       */
    } else {
      m_rConsole.setForeground(VisualizeUtils.processColour(
        props.getProperty("ForegroundColor", "black"), Color.BLACK));
      m_rConsole.setBackground(VisualizeUtils.processColour(
        props.getProperty("BackgroundColor", "white"), Color.WHITE));
      m_rConsole.setFont(new Font(props.getProperty("FontName", "monospaced"),
        Font.PLAIN, Integer.parseInt(props.getProperty("FontSize", "12"))));
    }

    // check R availablility
    m_rAvailable = RSession.rAvailable();
    /*
     * try { RSession.acquireSession(this); } catch (Exception e) { m_rAvailable
     * = false; } finally { RSession.releaseSession(this); }
     */

    try {
      if (!m_rAvailable) {
        String message = "R does not seem to be available. Check that "
          + "you have the R_HOME environment variable set, R is in your"
          + " path and that java.library.path property points to the "
          + "JRI native library. Information on settup for different "
          + "OS can be found at http://www.rforge.net/JRI\n\n";
        m_rConsole.getDocument().insertString(0, message, null);
      }
    } catch (BadLocationException e) {
      e.printStackTrace();
    }

    /*
     * m_statusLab.setBorder(BorderFactory.createCompoundBorder(
     * BorderFactory.createTitledBorder("Status"),
     * BorderFactory.createEmptyBorder(0, 5, 5, 5)));
     */
    JPanel consolePan = new JPanel();
    consolePan.setLayout(new BorderLayout());
    JScrollPane consoleScroller = new JScrollPane(m_rConsole);
    consoleScroller.setBorder(BorderFactory.createTitledBorder("R console"));
    consolePan.add(consoleScroller, BorderLayout.CENTER);
    // add(consolePan, BorderLayout.SOUTH);
    consolePan.setMinimumSize(new Dimension(810, 100));
    consolePan.setPreferredSize(new Dimension(810, 100));
    JPanel botP = new JPanel();
    botP.setLayout(new BorderLayout());
    botP.add(consolePan, BorderLayout.CENTER);
    // botP.add(m_statusLab, BorderLayout.SOUTH);
    if (displayLogger) {
      botP.add((JPanel) m_statusLogger, BorderLayout.SOUTH);
    }

    JSplitPane splitP = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topP, botP);
    splitP.setOneTouchExpandable(true);
    splitP.setDividerLocation(0.7);
    splitP.setResizeWeight(1.0);

    add(splitP, BorderLayout.CENTER);

    // register as a listener with the JavaGDOffscreenRenderer
    JavaGDOffscreenRenderer.getJavaGDNotifier().addListener(this);

    AbstractDocument doc = (AbstractDocument) m_rConsole.getDocument();
    try {
      doc.insertString(m_rConsole.getText().length(), ">>> ", null);
      m_rConsole.setCaretPosition(m_rConsole.getText().length());
    } catch (BadLocationException e1) {
      e1.printStackTrace();
    }

    doc.setDocumentFilter(new Filter());

    // prevent up and down arrow keys from moving the cursor up and down -
    // we want to use these to access the command history buffer
    reassignUpDown(m_rConsole.getInputMap(), m_rConsole.getActionMap());
    reassignUpDown(
      m_rConsole.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT),
      m_rConsole.getActionMap());

    // make sure that the cursor can't be moved over the command prompt
    m_rConsole.setNavigationFilter(new NavigationFilter() {
      @Override
      public void setDot(NavigationFilter.FilterBypass fb, int dot,
        Position.Bias bias) {
        int promptPos = m_rConsole.getText().lastIndexOf(">>> ") + 4;
        if (dot >= promptPos) {
          fb.setDot(dot, bias);
        }
      }

      @Override
      public void moveDot(NavigationFilter.FilterBypass fb, int dot,
        Position.Bias bias) {
        fb.setDot(dot, bias);
      }
    });

    // handle enter key events
    m_rConsole.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {

          if (m_executor != null) {
            return;
          }

          m_executor = new Thread() {
            @Override
            public void run() {

              int promptPos = m_rConsole.getText().lastIndexOf(">>> ") + 4;
              if (m_rConsole.getCaretPosition() >= promptPos) {
                // try {
                // String lastTyped =
                // m_rConsole.getDocument().getText(m_docLength, newLength);

                String text = m_rConsole.getText();
                String lastTyped = text.substring(text.lastIndexOf(">>> ") + 4,
                  text.length());
                // System.out.println(lastTyped);
                lastTyped = lastTyped.replace("\n", "");
                if (lastTyped.length() > 0) {
                  RSession eng = null;
                  try {
                    // m_statusLab.setText("Working...");
                    // m_statusLab.repaint();
                    m_statusLogger.statusMessage("Working...");
                    eng = RSession.acquireSession(JavaGDConsolePanel.this);
                    eng.clearConsoleBuffer(JavaGDConsolePanel.this);
                    eng.parseAndEval(JavaGDConsolePanel.this, lastTyped);
                    String consoleOut = eng
                      .getConsoleBuffer(JavaGDConsolePanel.this);
                    if (consoleOut != null && consoleOut.length() > 0) {
                      m_rConsole.getDocument().insertString(
                        m_rConsole.getText().length(), consoleOut + "\n", null);
                    }

                    m_historyBuffer.add(lastTyped);
                    m_historyPos = m_historyBuffer.size() - 1;
                    m_firstHistoryAccess = true;

                    // m_statusLab.setText("Ready.");
                    m_statusLogger.statusMessage("Ready.");
                  } catch (Exception ex) {
                    ex.printStackTrace();
                    // m_statusLab.setText("An error occurred - check log.");
                    m_statusLogger
                      .statusMessage("An error occurred - check log.");
                  } finally {
                    RSession.releaseSession(JavaGDConsolePanel.this);
                  }
                }

                try {
                  m_rConsole.getDocument().insertString(
                    m_rConsole.getText().length(), ">>> ", null);
                  m_rConsole.setCaretPosition(m_rConsole.getText().length());
                } catch (BadLocationException e1) {
                  e1.printStackTrace();
                }
              }

              m_executor = null;
            }
          };

          m_executor.setPriority(Thread.MIN_PRIORITY);
          m_executor.start();
        }
      }
    });
  }

  private boolean m_firstHistoryAccess = true;

  private void reassignUpDown(InputMap inputMap, ActionMap actMap) {
    String[] keys = { "UP", "DOWN" };

    for (String key : keys) {
      final String keyV = key;
      inputMap.put(KeyStroke.getKeyStroke(key), key);
      actMap.put(key, new AbstractAction() {

        /**
         * for serialization
         */
        private static final long serialVersionUID = -3038871531997610335L;

        @Override
        public void actionPerformed(ActionEvent e) {
          if (keyV.equalsIgnoreCase("up")) {
            // System.out.println("Up");

            if (m_historyPos > 0 && !m_firstHistoryAccess) {
              m_historyPos--;
            }

            if (m_historyBuffer.size() > 0) {
              String history = m_historyBuffer.get(m_historyPos);

              int pos = m_rConsole.getText().lastIndexOf(">>> ") + 4;
              int end = m_rConsole.getText().length();

              try {
                if (end - pos > 0) {
                  m_rConsole.getDocument().remove(pos, end - pos);
                }
                m_rConsole.getDocument().insertString(
                  m_rConsole.getText().length(), history, null);
                m_rConsole.setCaretPosition(m_rConsole.getText().length());
              } catch (BadLocationException e1) {
                e1.printStackTrace();
              }
            }

            if (m_historyPos > 0 && m_firstHistoryAccess) {
              m_historyPos--;
              m_firstHistoryAccess = false;
            }
          } else if (keyV.equalsIgnoreCase("down")) {

            if (m_historyBuffer.size() > 0
              && m_historyPos < m_historyBuffer.size() - 1) {
              // if (m_historyPos < m_historyBuffer.size() - 1) {
              m_historyPos++;
              // }
              String history = m_historyBuffer.get(m_historyPos);

              int pos = m_rConsole.getText().lastIndexOf(">>> ") + 4;
              int end = m_rConsole.getText().length();

              try {
                if (end - pos > 0) {
                  m_rConsole.getDocument().remove(pos, end - pos);
                }
                m_rConsole.getDocument().insertString(
                  m_rConsole.getText().length(), history, null);
                m_rConsole.setCaretPosition(m_rConsole.getText().length());
              } catch (BadLocationException e1) {
                e1.printStackTrace();
              }

            }
          }
        }
      });
    }
  }

  protected void visualize(String name, int x, int y) {
    // final JPanel panel = this;
    final String selectedName = name;

    JPopupMenu resultListMenu = new JPopupMenu();

    JMenuItem visSepWindow = new JMenuItem("View in a separate window");
    if (selectedName != null) {
      visSepWindow.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Object pic = m_history.getNamedObject(selectedName);
          if (pic instanceof BufferedImage) {
            final javax.swing.JFrame jf = new javax.swing.JFrame(selectedName);
            BufferedImage image = (BufferedImage) pic;
            jf.setSize(image.getWidth() + 10, image.getHeight() + 10);
            jf.getContentPane().setLayout(new BorderLayout());
            ImageDisplayer d = new ImageDisplayer();
            d.setImage(image);
            jf.getContentPane().add(d, BorderLayout.CENTER);
            jf.addWindowListener(new java.awt.event.WindowAdapter() {
              @Override
              public void windowClosing(java.awt.event.WindowEvent e) {
                jf.dispose();
              }
            });

            jf.setVisible(true);
          }
        }
      });
    } else {
      visSepWindow.setEnabled(false);
    }
    resultListMenu.add(visSepWindow);

    JMenuItem saveOutput = new JMenuItem("Save plot");
    if (selectedName != null) {
      saveOutput.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          final Object pic = m_history.getNamedObject(selectedName);
          if (pic instanceof BufferedImage) {
            final Environment vars = Environment.getSystemWide();
            final JDialog jf = new JDialog(
              (java.awt.Frame) JavaGDConsolePanel.this.getTopLevelAncestor(),
              ModalityType.DOCUMENT_MODAL);

            final FileEnvironmentField saver = new FileEnvironmentField(
              "Filename", vars, JFileChooser.SAVE_DIALOG);
            JPanel holder = new JPanel();
            holder.setLayout(new BorderLayout());
            holder.add(saver, BorderLayout.NORTH);
            JButton okBut = new JButton("OK");
            JButton cancelBut = new JButton("Cancel");
            okBut.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                String fileName = saver.getText();
                if (fileName != null && fileName.length() > 0) {
                  try {
                    fileName = vars.substitute(fileName);
                  } catch (Exception ex) {
                  }

                  if (fileName.toLowerCase().indexOf(".png") < 0) {
                    fileName += ".png";
                  }
                  File file = new File(fileName);
                  try {
                    ImageIO.write((BufferedImage) pic, "png", file);
                  } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(JavaGDConsolePanel.this,
                      "Unable to save plot (" + ex.getMessage() + ")", "Error",
                      JOptionPane.ERROR_MESSAGE);
                  }
                }

                jf.dispose();
              }
            });
            cancelBut.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                jf.dispose();
              }
            });

            JPanel butP = new JPanel();
            butP.setLayout(new GridLayout(0, 2));
            butP.add(okBut, BorderLayout.WEST);
            butP.add(cancelBut);
            holder.add(butP);

            jf.setLayout(new BorderLayout());
            jf.getContentPane().add(holder, BorderLayout.CENTER);
            jf.pack();
            jf.setLocationRelativeTo(JavaGDConsolePanel.this);
            jf.setVisible(true);
          }
        }
      });
    } else {
      saveOutput.setEnabled(false);
    }
    resultListMenu.add(saveOutput);

    JMenuItem deletePlot = new JMenuItem("Delete plot");
    if (selectedName != null) {
      deletePlot.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          m_history.removeResult(selectedName);
        }
      });
    } else {
      deletePlot.setEnabled(false);
    }
    resultListMenu.add(deletePlot);

    resultListMenu.show(m_history, x, y);
  }

  /**
   * Transfers an Instances object into R as a data frame called "rdata"
   * 
   * @param insts the instances to transfer
   */
  public void pushInstancesToR(Instances insts) {
    if (!m_rAvailable) {
      return;
    }

    RSession eng = null;
    try {
      m_statusLogger.statusMessage("Transfered " + insts.relationName()
        + " into R as \"rdata\"");
      m_statusLogger.logMessage("Transfered " + insts.relationName()
        + " into R as \"rdata\"");
      eng = RSession.acquireSession(this);
      RUtils.instancesToDataFrame(eng, this, insts, "rdata");
    } catch (Exception ex) {
      m_statusLogger.statusMessage("Problem transfering instances to R: "
        + ex.getMessage());
      ex.printStackTrace();
    } finally {
      RSession.releaseSession(this);
    }
  }

  /**
   * Set the logger to use
   * 
   * @param log the logger to use
   */
  public void setLogger(Logger log) {
    m_statusLogger = log;
  }

  /**
   * Display an image and store in the history list. Gets called by the
   * JavaGDOffscreenRenderer when a new graphic has been generated in R.
   * 
   * @param image the image to display and store.
   */
  @Override
  public void imageGenerated(BufferedImage image) {

    String name = (new SimpleDateFormat("HH:mm:ss")).format(new Date());
    name = "Plot at " + name;
    m_history.addResult(name, new StringBuffer());
    m_history.addObject(name, image);
    m_plotter.setImage(image);
    m_plotter.repaint();
  }
}
