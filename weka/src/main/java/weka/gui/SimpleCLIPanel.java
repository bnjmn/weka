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
 * SimpleCLIPanel.java
 * Copyright (C) 2009-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.gui;

import weka.core.ClassDiscovery;
import weka.core.Defaults;
import weka.core.Instances;
import weka.core.Trie;
import weka.core.Utils;
import weka.core.WekaPackageManager;
import weka.gui.knowledgeflow.StepVisual;
import weka.gui.scripting.ScriptingPanel;
import weka.gui.simplecli.AbstractCommand;
import weka.gui.simplecli.Help;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/**
 * Creates a very simple command line for invoking the main method of classes.
 * System.out and System.err are redirected to an output area. Features a simple
 * command history -- use up and down arrows to move through previous commmands.
 * This gui uses only AWT (i.e. no Swing).
 * 
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
@PerspectiveInfo(ID = "simplecli", title = "Simple CLI",
  toolTipText = "Simple CLI for Weka",
  iconPath = "weka/gui/weka_icon_new_small.png")
public class SimpleCLIPanel extends ScriptingPanel implements ActionListener,
  Perspective {

  /** for serialization. */
  private static final long serialVersionUID = 1089039734615114942L;

  /** The filename of the properties file. */
  protected static String FILENAME = "SimpleCLI.props";

  /** The default location of the properties file. */
  protected static String PROPERTY_FILE = "weka/gui/" + FILENAME;

  /** Contains the SimpleCLI properties. */
  protected static Properties PROPERTIES;

  /** Main application (if any) owning this perspective */
  protected GUIApplication m_mainApp;

  /** The Icon for this perspective */
  protected Icon m_perspectiveIcon;

  static {
    // Allow a properties file in the current directory to override
    try {
      PROPERTIES = Utils.readProperties(PROPERTY_FILE);
      java.util.Enumeration<?> keys = PROPERTIES.propertyNames();
      if (!keys.hasMoreElements()) {
        throw new Exception("Failed to read a property file for the SimpleCLI");
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null,
        "Could not read a configuration file for the SimpleCLI.\n"
          + "An example file is included with the Weka distribution.\n"
          + "This file should be named \"" + PROPERTY_FILE + "\" and\n"
          + "should be placed either in your user home (which is set\n"
          + "to \"" + System.getProperties().getProperty("user.home") + "\")\n"
          + "or the directory that java was started from\n", "SimpleCLI",
        JOptionPane.ERROR_MESSAGE);
    }
  }

  /** The output area canvas added to the frame. */
  protected JTextPane m_OutputArea;

  /** The command input area. */
  protected JTextField m_Input;

  /** The history of commands entered interactively. */
  protected Vector<String> m_CommandHistory;

  /** The current position in the command history. */
  protected int m_HistoryPos;

  /** The thread currently running a class main method. */
  protected Thread m_RunThread;

  /** The commandline completion. */
  protected CommandlineCompletion m_Completion;

  /** for storing variables. */
  protected Map<String,Object> m_Variables;

  @Override
  public void instantiationComplete() {

  }

  @Override
  public boolean okToBeActive() {
    return true;
  }

  @Override
  public void setActive(boolean active) {

  }

  @Override
  public void setLoaded(boolean loaded) {

  }

  @Override
  public void setMainApplication(GUIApplication main) {
    m_mainApp = main;
  }

  @Override
  public GUIApplication getMainApplication() {
    return m_mainApp;
  }

  @Override
  public String getPerspectiveID() {
    return "simplecli";
  }

  @Override
  public String getPerspectiveTitle() {
    return "Simple CLI";
  }

  @Override
  public Icon getPerspectiveIcon() {
    if (m_perspectiveIcon != null) {
      return m_perspectiveIcon;
    }

    PerspectiveInfo perspectiveA =
      this.getClass().getAnnotation(PerspectiveInfo.class);
    if (perspectiveA != null && perspectiveA.iconPath() != null
      && perspectiveA.iconPath().length() > 0) {
      m_perspectiveIcon = StepVisual.loadIcon(perspectiveA.iconPath());
    }

    return m_perspectiveIcon;
  }

  @Override
  public String getPerspectiveTipText() {
    return "Simple CLI interface for Weka";
  }

  @Override
  public List<JMenu> getMenus() {
    return null;
  }

  @Override
  public Defaults getDefaultSettings() {
    return null;
  }

  @Override
  public void settingsChanged() {

  }

  @Override
  public boolean acceptsInstances() {
    return false;
  }

  @Override
  public void setInstances(Instances instances) {

  }

  @Override
  public boolean requiresLog() {
    return false;
  }

  @Override
  public void setLog(Logger log) {

  }

  /**
   * A class that handles running the main method of the class in a separate
   * thread.
   * 
   * @author Len Trigg (trigg@cs.waikato.ac.nz)
   * @version $Revision$
   */
  public static class ClassRunner extends Thread {

    /** the owner. */
    protected SimpleCLIPanel m_Owner;

    /** Stores the main method to call. */
    protected Method m_MainMethod;

    /** Stores the command line arguments to pass to the main method. */
    String[] m_CommandArgs;

    /** True if the class to be executed is weka.Run */
    protected boolean m_classIsRun;

    /**
     * Sets up the class runner thread.
     * 
     * @param theClass the Class to call the main method of
     * @param commandArgs an array of Strings to use as command line args
     * @throws Exception if an error occurs
     */
    public ClassRunner(SimpleCLIPanel owner, Class<?> theClass, String[] commandArgs)
      throws Exception {

      m_Owner = owner;
      m_classIsRun = (theClass.getClass().getName().equals("weka.Run"));

      setDaemon(true);
      Class<?>[] argTemplate = { String[].class };
      m_CommandArgs = commandArgs;
      if (m_classIsRun) {
        if (!commandArgs[0].equalsIgnoreCase("-h")
          && !commandArgs[0].equalsIgnoreCase("-help")) {
          m_CommandArgs = new String[commandArgs.length + 1];
          System.arraycopy(commandArgs, 0, m_CommandArgs, 1, commandArgs.length);
          m_CommandArgs[0] = "-do-not-prompt-if-multiple-matches";
        }
      }
      m_MainMethod = theClass.getMethod("main", argTemplate);
      if (((m_MainMethod.getModifiers() & Modifier.STATIC) == 0)
        || (m_MainMethod.getModifiers() & Modifier.PUBLIC) == 0) {
        throw new NoSuchMethodException("main(String[]) method of "
          + theClass.getName() + " is not public and static.");
      }
    }

    /**
     * Starts running the main method.
     */
    @Override
    public void run() {
      PrintStream outOld = null;
      PrintStream outNew = null;
      String outFilename = null;

      // is the output redirected?
      if (m_CommandArgs.length > 2) {
        String action = m_CommandArgs[m_CommandArgs.length - 2];
        if (action.equals(">")) {
          outOld = System.out;
          try {
            outFilename = m_CommandArgs[m_CommandArgs.length - 1];
            // since file may not yet exist, command-line completion doesn't
            // work, hence replace "~" manually with home directory
            if (outFilename.startsWith("~")) {
              outFilename =
                outFilename.replaceFirst("~", System.getProperty("user.home"));
            }
            outNew = new PrintStream(new File(outFilename));
            System.setOut(outNew);
            m_CommandArgs[m_CommandArgs.length - 2] = "";
            m_CommandArgs[m_CommandArgs.length - 1] = "";
            // some main methods check the length of the "args" array
            // -> removed the two empty elements at the end
            String[] newArgs = new String[m_CommandArgs.length - 2];
            System.arraycopy(m_CommandArgs, 0, newArgs, 0,
              m_CommandArgs.length - 2);
            m_CommandArgs = newArgs;
          } catch (Exception e) {
            System.setOut(outOld);
            outOld = null;
          }
        }
      }

      try {
        Object[] args = { m_CommandArgs };
        m_MainMethod.invoke(null, args);
        if (isInterrupted()) {
          System.err.println("[...Interrupted]");
        }
      } catch (Exception ex) {
        if (ex.getMessage() == null) {
          System.err.println("[...Killed]");
        } else {
          System.err.println("[Run exception] " + ex.getMessage());
        }
      } finally {
        m_Owner.stopThread();
      }

      // restore old System.out stream
      if (outOld != null) {
        outNew.flush();
        outNew.close();
        System.setOut(outOld);
        System.out.println("Finished redirecting output to '" + outFilename
          + "'.");
      }
    }
  }

  /**
   * A class for commandline completion of classnames.
   * 
   * @author FracPete (fracpete at waikato dot ac dot nz)
   * @version $Revision$
   */
  public static class CommandlineCompletion {

    /** all the available packages. */
    protected Vector<String> m_Packages;

    /** a trie for storing the packages. */
    protected Trie m_Trie;

    /** debug mode on/off. */
    protected boolean m_Debug = false;

    /**
     * default constructor.
     */
    public CommandlineCompletion() {
      super();

      // build incremental list of packages
      if (m_Packages == null) {
        // get all packages
        Vector<String> list = ClassDiscovery.findPackages();

        // create incremental list
        HashSet<String> set = new HashSet<String>();
        for (int i = 0; i < list.size(); i++) {
          String[] parts = list.get(i).split("\\.");
          for (int n = 1; n < parts.length; n++) {
            String pkg = "";
            for (int m = 0; m <= n; m++) {
              if (m > 0) {
                pkg += ".";
              }
              pkg += parts[m];
            }
            set.add(pkg);
          }
        }

        // init packages
        m_Packages = new Vector<String>();
        m_Packages.addAll(set);
        Collections.sort(m_Packages);

        m_Trie = new Trie();
        m_Trie.addAll(m_Packages);
      }
    }

    /**
     * returns whether debug mode is on.
     * 
     * @return true if debug is on
     */
    public boolean getDebug() {
      return m_Debug;
    }

    /**
     * sets debug mode on/off.
     * 
     * @param value if true then debug mode is on
     */
    public void setDebug(boolean value) {
      m_Debug = value;
    }

    /**
     * tests whether the given partial string is the name of a class with
     * classpath - it basically tests, whether the string consists only of
     * alphanumeric literals, underscores and dots.
     * 
     * @param partial the string to test
     * @return true if it looks like a classname
     */
    public boolean isClassname(String partial) {
      return (partial.replaceAll("[a-zA-Z0-9\\-\\.]*", "").length() == 0);
    }

    /**
     * returns the packages part of the partial classname.
     * 
     * @param partial the partial classname
     * @return the package part of the partial classname
     */
    public String getPackage(String partial) {
      String result;
      int i;
      boolean wasDot;
      char c;

      result = "";
      wasDot = false;
      for (i = 0; i < partial.length(); i++) {
        c = partial.charAt(i);

        // start of classname?
        if (wasDot && ((c >= 'A') && (c <= 'Z'))) {
          break;
        }
        // package/class separator
        else if (c == '.') {
          wasDot = true;
          result += "" + c;
        }
        // regular char
        else {
          wasDot = false;
          result += "" + c;
        }
      }

      // remove trailing "."
      if (result.endsWith(".")) {
        result = result.substring(0, result.length() - 1);
      }

      return result;
    }

    /**
     * returns the classname part of the partial classname.
     * 
     * @param partial the partial classname
     * @return the class part of the classname
     */
    public String getClassname(String partial) {
      String result;
      String pkg;

      pkg = getPackage(partial);
      if (pkg.length() + 1 < partial.length()) {
        result = partial.substring(pkg.length() + 1);
      } else {
        result = "";
      }

      return result;
    }

    /**
     * returns all the file/dir matches with the partial search string.
     * 
     * @param partial the partial search string
     * @return all the matches
     */
    public Vector<String> getFileMatches(String partial) {
      Vector<String> result;
      File file;
      File dir;
      File[] files;
      int i;
      String prefix;
      boolean caseSensitive;
      String name;
      boolean match;

      result = new Vector<String>();

      // is the OS case-sensitive?
      caseSensitive = (File.separatorChar != '\\');
      if (m_Debug) {
        System.out.println("case-sensitive=" + caseSensitive);
      }

      // is "~" used for home directory? -> replace with actual home directory
      if (partial.startsWith("~")) {
        partial = System.getProperty("user.home") + partial.substring(1);
      }

      // determine dir and possible prefix
      file = new File(partial);
      dir = null;
      prefix = null;
      if (file.exists()) {
        // determine dir to read
        if (file.isDirectory()) {
          dir = file;
          prefix = null; // retrieve all
        } else {
          dir = file.getParentFile();
          prefix = file.getName();
        }
      } else {
        dir = file.getParentFile();
        prefix = file.getName();
      }

      if (m_Debug) {
        System.out.println("search in dir=" + dir + ", prefix=" + prefix);
      }

      // list all files in dir
      if (dir != null) {
        files = dir.listFiles();
        if (files != null) {
          for (i = 0; i < files.length; i++) {
            name = files[i].getName();

            // does the name match?
            if ((prefix != null) && caseSensitive) {
              match = name.startsWith(prefix);
            } else if ((prefix != null) && !caseSensitive) {
              match = name.toLowerCase().startsWith(prefix.toLowerCase());
            } else {
              match = true;
            }

            if (match) {
              if (prefix != null) {
                result.add(partial.substring(0,
                  partial.length() - prefix.length())
                  + name);
              } else {
                if (partial.endsWith("\\") || partial.endsWith("/")) {
                  result.add(partial + name);
                } else {
                  result.add(partial + File.separator + name);
                }
              }
            }
          }
        } else {
          System.err.println("Invalid path: " + partial);
        }
      }

      // sort the result
      if (result.size() > 1) {
        Collections.sort(result);
      }

      // print results
      if (m_Debug) {
        System.out.println("file matches:");
        for (i = 0; i < result.size(); i++) {
          System.out.println(result.get(i));
        }
      }

      return result;
    }

    /**
     * returns all the class/package matches with the partial search string.
     * 
     * @param partial the partial search string
     * @return all the matches
     */
    public Vector<String> getClassMatches(String partial) {
      String pkg;
      String cls;
      Vector<String> result;
      Vector<String> list;
      int i;
      int index;
      Trie tmpTrie;
      HashSet<String> set;
      String tmpStr;

      pkg = getPackage(partial);
      cls = getClassname(partial);

      if (getDebug()) {
        System.out.println("\nsearch for: '" + partial + "' => package=" + pkg
          + ", class=" + cls);
      }

      result = new Vector<String>();

      // find all packages that start with that string
      if (cls.length() == 0) {
        list = m_Trie.getWithPrefix(pkg);
        set = new HashSet<String>();
        for (i = 0; i < list.size(); i++) {
          tmpStr = list.get(i);
          if (tmpStr.length() < partial.length()) {
            continue;
          }
          if (tmpStr.equals(partial)) {
            continue;
          }

          index = tmpStr.indexOf('.', partial.length() + 1);
          if (index > -1) {
            set.add(tmpStr.substring(0, index));
          } else {
            set.add(tmpStr);
          }
        }

        result.addAll(set);
        if (result.size() > 1) {
          Collections.sort(result);
        }
      }

      // find all classes that start with that string
      list = ClassDiscovery.find(Object.class, pkg);
      tmpTrie = new Trie();
      tmpTrie.addAll(list);
      list = tmpTrie.getWithPrefix(partial);
      result.addAll(list);

      // sort the result
      if (result.size() > 1) {
        Collections.sort(result);
      }

      // print results
      if (m_Debug) {
        System.out.println("class/package matches:");
        for (i = 0; i < result.size(); i++) {
          System.out.println(result.get(i));
        }
      }

      return result;
    }

    /**
     * returns all the matches with the partial search string, files or classes.
     * 
     * @param partial the partial search string
     * @return all the matches
     */
    public Vector<String> getMatches(String partial) {
      if (isClassname(partial)) {
        return getClassMatches(partial);
      } else {
        return getFileMatches(partial);
      }
    }

    /**
     * returns the common prefix for all the items in the list.
     * 
     * @param list the list to return the common prefix for
     * @return the common prefix of all the items
     */
    public String getCommonPrefix(Vector<String> list) {
      String result;
      Trie trie;

      trie = new Trie();
      trie.addAll(list);
      result = trie.getCommonPrefix();

      if (m_Debug) {
        System.out.println(list + "\n  --> common prefix: '" + result + "'");
      }

      return result;
    }
  }

  /**
   * For initializing member variables.
   */
  @Override
  protected void initialize() {
    super.initialize();

    m_CommandHistory = new Vector<String>();
    m_HistoryPos = 0;
    m_Completion = new CommandlineCompletion();
    m_Variables  = new HashMap<>();
  }

  /**
   * Sets up the GUI after initializing the members.
   */
  @Override
  protected void initGUI() {
    super.initGUI();

    setLayout(new BorderLayout());

    m_OutputArea = new JTextPane();
    m_OutputArea.setEditable(false);
    m_OutputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    add(new JScrollPane(m_OutputArea), "Center");

    m_Input = new JTextField();
    m_Input.setFont(new Font("Monospaced", Font.PLAIN, 12));
    m_Input.addActionListener(this);
    m_Input.setFocusTraversalKeysEnabled(false);
    m_Input.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        doHistory(e);
        doCommandlineCompletion(e);
      }
    });
    add(m_Input, "South");
  }

  /**
   * Finishes up after initializing members and setting up the GUI.
   */
  @Override
  protected void initFinish() {
    super.initFinish();

    System.out.println("\nWelcome to the WEKA SimpleCLI\n\n"
      + "Enter commands in the textfield at the bottom of \n"
      + "the window. Use the up and down arrows to move \n"
      + "through previous commands.\n"
      + "Command completion for classnames and files is \n"
      + "initiated with <Tab>. In order to distinguish \n"
      + "between files and classnames, file names must \n"
      + "be either absolute or start with '." + File.separator + "' or '~/'\n"
      + "(the latter is a shortcut for the home directory).\n"
      + "<Alt+BackSpace> is used for deleting the text\n"
      + "in the commandline in chunks.\n"
      + "\n"
      + "Type 'help' followed by <Enter> to see an overview \n"
      + "of all commands.");

    loadHistory();

    SwingUtilities.invokeLater(() -> m_Input.requestFocus());
  }

  /**
   * Returns an icon to be used in a frame.
   * 
   * @return the icon
   */
  @Override
  public ImageIcon getIcon() {
    return ComponentHelper.getImageIcon("weka_icon_new_48.png");
  }

  /**
   * Returns the current title for the frame/dialog.
   * 
   * @return the title
   */
  @Override
  public String getTitle() {
    return "SimpleCLI";
  }

  /**
   * Returns the text area that is used for displaying output on stdout and
   * stderr.
   * 
   * @return the JTextArea
   */
  @Override
  public JTextPane getOutput() {
    return m_OutputArea;
  }

  /**
   * Not supported.
   * 
   * @return always null
   */
  @Override
  public JMenuBar getMenuBar() {
    return null;
  }

  /**
   * Checks whether a thread is currently running.
   *
   * @return		true if thread active
   */
  public boolean isBusy() {
    return (m_RunThread != null);
  }

  /**
   * Starts the thread.
   *
   * @param runner 	the thread to start
   */
  public void startThread(ClassRunner runner) {
    m_RunThread = runner;
    m_RunThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
    m_RunThread.start();
  }

  /**
   * Stops the currently running thread, if any.
   */
  @SuppressWarnings("deprecation")
  public void stopThread() {
    if (m_RunThread != null) {
      if (m_RunThread.isAlive()) {
        try {
	  m_RunThread.stop();
	}
	catch (Throwable t) {
          // ignored
	}
      }
      m_RunThread = null;
    }
  }

  /**
   * Returns the variables.
   *
   * @return		the variables
   */
  public Map<String,Object> getVariables() {
    return m_Variables;
  }

  /**
   * The output area.
   *
   * @return		the output area
   */
  public JTextPane getOutputArea() {
    return m_OutputArea;
  }

  /**
   * Returns the command history.
   *
   * @return		the history
   */
  public List<String> getCommandHistory() {
    return m_CommandHistory;
  }

  /**
   * Executes a simple cli command.
   * 
   * @param command the command string
   * @throws Exception if an error occurs
   */
  public void runCommand(String command) throws Exception {
    System.out.println("> " + command + '\n');
    System.out.flush();
    String[] commandArgs;
    if (!System.getProperty("os.name").toLowerCase().contains("win")) {
      commandArgs = Utils.splitOptions(command);
    } else { // If we are on Windows, we must not replace \t, \n, etc., at this stage because \ is path separator
      commandArgs = Utils.splitOptions(command, new String[] { "\\\\", "\\\""}, new char[] { '\\', '"' });
    }

    // no command
    if (commandArgs.length == 0) {
      return;
    }

    // find command
    AbstractCommand cmd = AbstractCommand.getCommand(commandArgs[0]);

    // unknown command
    if (cmd == null) {
      System.err.println("Unknown command: " + commandArgs[0]);
      Help help = new Help();
      help.setOwner(this);
      help.execute(new String[0]);
      return;
    }

    // execute command
    String[] params = new String[commandArgs.length -1];
    System.arraycopy(commandArgs, 1, params, 0, params.length);
    cmd.setOwner(this);
    try {
      cmd.execute(params);
    }
    catch (Exception e) {
      System.err.println("Error executing: " + command);
      e.printStackTrace();
    }
  }

  /**
   * Changes the currently displayed command line when certain keys are pressed.
   * The up arrow moves back through history entries and the down arrow moves
   * forward through history entries.
   * 
   * @param e a value of type 'KeyEvent'
   */
  public void doHistory(KeyEvent e) {

    if (e.getSource() == m_Input) {
      switch (e.getKeyCode()) {
      case KeyEvent.VK_UP:
        if (m_HistoryPos > 0) {
          m_HistoryPos--;
          String command = m_CommandHistory.elementAt(m_HistoryPos);
          m_Input.setText(command);
        }
        break;
      case KeyEvent.VK_DOWN:
        if (m_HistoryPos < m_CommandHistory.size()) {
          m_HistoryPos++;
          String command = "";
          if (m_HistoryPos < m_CommandHistory.size()) {
            command = m_CommandHistory.elementAt(m_HistoryPos);
          }
          m_Input.setText(command);
        }
        break;
      default:
        break;
      }
    }
  }

  /**
   * performs commandline completion on packages and classnames.
   * 
   * @param e a value of type 'KeyEvent'
   */
  public void doCommandlineCompletion(KeyEvent e) {
    if (e.getSource() == m_Input) {
      switch (e.getKeyCode()) {
      // completion
      case KeyEvent.VK_TAB:
        if (e.getModifiers() == 0) {
          // it might take a while before we determined all of the possible
          // matches (Java doesn't have an application wide cursor handling??)
          m_Input.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          m_OutputArea
            .setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

          try {
            String txt = m_Input.getText();

            // java call?
            if (txt.trim().startsWith("java ")) {
              int pos = m_Input.getCaretPosition();
              int nonNameCharPos = -1;
              // find first character not part of a name, back from current
              // position
              // i.e., " or blank
              for (int i = pos - 1; i >= 0; i--) {
                if ((txt.charAt(i) == '"') || (txt.charAt(i) == ' ')) {
                  nonNameCharPos = i;
                  break;
                }
              }

              if (nonNameCharPos > -1) {
                String search = txt.substring(nonNameCharPos + 1, pos);

                // find matches and common prefix
                Vector<String> list = m_Completion.getMatches(search);
                String common = m_Completion.getCommonPrefix(list);

                // just extending by separator is not a real extension
                if ((search.toLowerCase() + File.separator).equals(common
                  .toLowerCase())) {
                  common = search;
                }

                // can we complete the string?
                if (common.length() > search.length()) {
                  try {
                    m_Input.getDocument().remove(nonNameCharPos + 1,
                      search.length());
                    m_Input.getDocument().insertString(nonNameCharPos + 1,
                      common, null);
                  } catch (Exception ex) {
                    ex.printStackTrace();
                  }
                }
                // ambigiuous? -> print matches
                else if (list.size() > 1) {
                  System.out.println("\nPossible matches:");
                  for (int i = 0; i < list.size(); i++) {
                    System.out.println("  " + list.get(i));
                  }
                } else {
                  // nothing found, don't do anything
                }
              }
            }
          } finally {
            // set cursor back to default
            m_Input.setCursor(null);
            m_OutputArea.setCursor(null);
          }
        }
        break;

      // delete last part up to next blank or dot
      case KeyEvent.VK_BACK_SPACE:
        if (e.getModifiers() == KeyEvent.ALT_MASK) {
          String txt = m_Input.getText();
          int pos = m_Input.getCaretPosition();

          // skip whitespaces
          int start = pos;
          start--;
          while (start >= 0) {
            if ((txt.charAt(start) == '.') || (txt.charAt(start) == ' ')
              || (txt.charAt(start) == '\\') || (txt.charAt(start) == '/')) {
              start--;
            } else {
              break;
            }
          }

          // find first blank or dot back from position
          int newPos = -1;
          for (int i = start; i >= 0; i--) {
            if ((txt.charAt(i) == '.') || (txt.charAt(i) == ' ')
              || (txt.charAt(i) == '\\') || (txt.charAt(i) == '/')) {
              newPos = i;
              break;
            }
          }

          // remove string
          try {
            m_Input.getDocument().remove(newPos + 1, pos - newPos - 1);
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
        break;
      }
    }
  }

  /**
   * Only gets called when return is pressed in the input area, which starts the
   * command running.
   * 
   * @param e a value of type 'ActionEvent'
   */
  @Override
  public void actionPerformed(ActionEvent e) {

    try {
      if (e.getSource() == m_Input) {
        String command = m_Input.getText();
        int last = m_CommandHistory.size() - 1;
        if ((last < 0) || !command.equals(m_CommandHistory.elementAt(last))) {
          m_CommandHistory.addElement(command);
          saveHistory();
        }
        m_HistoryPos = m_CommandHistory.size();
        runCommand(command);

        m_Input.setText("");
      }
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }

  /**
   * loads the command history from the user's properties file.
   */
  protected void loadHistory() {
    int size;
    int i;
    String cmd;

    size = Integer.parseInt(PROPERTIES.getProperty("HistorySize", "50"));

    m_CommandHistory.clear();
    for (i = 0; i < size; i++) {
      cmd = PROPERTIES.getProperty("Command" + i, "");
      if (cmd.length() != 0) {
        m_CommandHistory.add(cmd);
      } else {
        break;
      }
    }

    m_HistoryPos = m_CommandHistory.size();
  }

  /**
   * saves the current command history in the user's home directory.
   */
  protected void saveHistory() {
    int size;
    int from;
    int i;
    String filename;
    BufferedOutputStream stream;

    size = Integer.parseInt(PROPERTIES.getProperty("HistorySize", "50"));

    // determine first command to save
    from = m_CommandHistory.size() - size;
    if (from < 0) {
      from = 0;
    }

    // fill properties
    PROPERTIES.setProperty("HistorySize", "" + size);
    for (i = from; i < m_CommandHistory.size(); i++) {
      PROPERTIES.setProperty("Command" + (i - from), m_CommandHistory.get(i));
    }

    try {
      filename =
        WekaPackageManager.PROPERTIES_DIR.getAbsolutePath() + File.separatorChar
          + FILENAME;
      stream = new BufferedOutputStream(new FileOutputStream(filename));
      PROPERTIES.store(stream, "SimpleCLI");
      stream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Displays the panel in a frame.
   * 
   * @param args ignored
   */
  public static void main(String[] args) {


    weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
            "Logging started");

    LookAndFeel.setLookAndFeel();
    // make sure that packages are loaded and the GenericPropertiesCreator
    // executes to populate the lists correctly
    weka.gui.GenericObjectEditor.determineClasses();

    showPanel(new SimpleCLIPanel(), args);
  }
}
