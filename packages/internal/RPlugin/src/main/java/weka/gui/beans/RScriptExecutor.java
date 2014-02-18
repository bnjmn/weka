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
 *    RScriptExcecutor.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.beans.EventSetDescriptor;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instances;
import weka.core.JavaGDListener;
import weka.core.RLoggerAPI;
import weka.core.RSession;
import weka.core.RSessionException;
import weka.core.RUtils;
import weka.gui.Logger;

// JRI dynamically linked library
// needs -Djava.library.path=/Library/Frameworks/R.framework/Resources/library/rJava/jri/

/**
 * A Knowledge Flow component that executes a user-supplied R script. The script
 * may be supplied via the GUI editor for the component or loaded from a file at
 * run time. Incoming instances will be transfered to a data frame in the R
 * environment and can be accessed from the user script by referencing the
 * variable "rdata". Console output in R can be forwarded to a TextViewer by
 * using "print" statements in R. E.g. <br>
 * <br>
 * 
 * <pre>
 * data(iris)
 * print(iris)
 * </pre>
 * <p>
 * 
 * Data frames in R can be extracted, converted to Weka Instances and passed on
 * via dataSet connections by simply stating the name of the data frame in the
 * user's R script. This <b>must</b> be the last command in the script. E.g:
 * <p>
 * 
 * <pre>
 * data(iris)
 * iris
 * </pre>
 * <p>
 * 
 * Graphics in R can be captured by making use of the JavaGD graphics device for
 * R (http://stats.math.uni-augsburg.de/R/JavaGD/). The Weka RPlugin will
 * attempt to install this package for the user automatically - if this fails
 * for some reason then the package should be installed by the user from R via
 * install.packages("JavaGD"). The default size for graphics created in R via
 * JavaGD is 800x600. The user can alter this in their R scripts by executing
 * the following command:<br>
 * <br>
 * 
 * <pre>
 * JavaGD(width = w, height = h)
 * </pre>
 * 
 * where "w" and "h" are the new width and height in pixels. Note that this
 * setting stays in effect until another "JavaGD()" command is executed.
 * <p>
 * 
 * The RScriptExecutor can be connected to an ImageSaver component in order to
 * save any graphics generated to a file. If running within the graphical
 * Knowledge Flow UI any generated images will also be sent to the R
 * Console/Visualize perspective.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class RScriptExecutor extends JPanel implements BeanCommon, Visible,
  EventConstraints, Serializable, TrainingSetListener, TestSetListener,
  DataSourceListener, EnvironmentHandler, Startable, JavaGDListener {

  /** For serialization */
  private static final long serialVersionUID = 8134803291450969241L;

  /** Visual for this bean */
  protected BeanVisual m_visual = new BeanVisual("RScriptExecutor",
    BeanVisual.ICON_PATH + "RScriptExecutor.gif", BeanVisual.ICON_PATH
      + "RScriptExecutor_animated.gif");

  /** Downstream steps interested in text events from us */
  protected ArrayList<TextListener> m_textListeners = new ArrayList<TextListener>();

  /** Downstream steps interested in Instances from us */
  protected ArrayList<DataSourceListener> m_dataListeners = new ArrayList<DataSourceListener>();

  /** Downstream steps interested in image events from us */
  protected ArrayList<ImageListener> m_imageListeners = new ArrayList<ImageListener>();

  /** Step talking to us */
  protected Object m_listenee;

  protected transient Environment m_env;
  protected transient Logger m_logger;
  protected transient LogWrapper m_logWrapper;

  protected String m_rScript = "";

  /** Script file overrides any encapsulated script */
  protected String m_scriptFile = "";

  /** True when this step is busy */
  protected transient boolean m_busy;

  protected transient boolean m_rWarning;

  /**
   * Wrap a log so that we can detect any error or warning messages coming back
   * from R and also so that status messages can be associated with the correct
   * KF component in the KF status area of the log.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  protected class LogWrapper implements RLoggerAPI {
    protected Logger m_wrappedLog;
    protected String m_prefix;

    /**
     * Construct a new LogWrapper
     * 
     * @param wrapped the log to wrap around
     * @param prefix prefix for uniquely identifying the owning KF component
     */
    public LogWrapper(Logger wrapped, String prefix) {
      m_wrappedLog = wrapped;
      m_prefix = prefix;
    }

    /**
     * Print a message to the log
     * 
     * @param message the message to print
     */
    @Override
    public void logMessage(String message) {
      if (m_wrappedLog != null) {
        m_wrappedLog.logMessage(m_prefix + message);
      }
    }

    /**
     * Print a status message
     * 
     * @param message the status message to print
     */
    @Override
    public void statusMessage(String message) {
      if (m_wrappedLog != null) {
        if (message.startsWith("WARNING") || message.startsWith("ERROR")) {
          m_rWarning = true;
        }
        m_wrappedLog.statusMessage(m_prefix + message);
      }
    }
  }

  /**
   * Get about information to display in the GUI
   * 
   * @return Textual about information
   */
  public String globalInfo() {
    return "A Knowledge Flow component that executes a user-supplied R script. The "
      + "script may be supplied via the GUI editor for the component or loaded "
      + "from a file at run time. Incoming instances will be transfered to a "
      + "data frame in the R environment and can be accessed from the user script "
      + "by referencing the variable \"rdata\". Console output in R can be forwarded "
      + "to a TextViewer by using \"print\" statements in R. E.g. \n\n"
      + "data(iris)\nprint(iris)\n\n"
      + "Data frames in R can be extracted, converted to Weka Instances and passed "
      + "on via dataSet connections by simply stating the name of the data frame in "
      + "the user's R script. This *must* be the last command in the script. E.g:\n\n"
      + "data(iris)\niris\n\n"
      + "Graphics in R can be captured by making use of the JavaGD graphics device "
      + "for R (http://stats.math.uni-augsburg.de/R/JavaGD/). The Weka RPlugin will "
      + "attempt to install this package for the user automatically - if this fails "
      + "for some reason then the package should be installed by the user from R "
      + "via install.packages(\"JavaGD\"). The default size for graphics created in "
      + "R via JavaGD is 800x600. The user can alter this in their R scripts by "
      + "executing the following command:\n\n"
      + "JavaGD(width=w, height=h)\n\n"
      + "where \"w\" and \"h\" are the new width and height in pixels. Note that this "
      + "setting stays in effect until another \"JavaGD()\" command is executed.\n\n"
      + "The RScriptExecutor can be connected to "
      + "an ImageSaver component in order to save any graphics generated to a file. "
      + "If running within the graphical Knowledge Flow UI any generated images will "
      + "also be sent to the R Console/Visualize perspective.";
  }

  /**
   * Constructor
   */
  public RScriptExecutor() {
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);

    m_env = Environment.getSystemWide();
  }

  /**
   * Set the script to execute
   * 
   * @param script the R script to execute
   */
  public void setRScript(String script) {
    m_rScript = script;
  }

  /**
   * Get the script to execute
   * 
   * @return the R script to execute
   */
  public String getRScript() {
    return m_rScript;
  }

  /**
   * Set the filename containing the script to be loaded at runtime
   * 
   * @param scriptFile the name of the script file to load at runtime
   */
  public void setScriptFile(String scriptFile) {
    m_scriptFile = scriptFile;
  }

  /**
   * Get the filename of the script to load at runtime
   * 
   * @return the filename of the script to load at runtime
   */
  public String getScriptFile() {
    return m_scriptFile;
  }

  /**
   * Pass a batch set of instances into R as a data frame and then execute the
   * user's script.
   * 
   * We will only allow one incoming data connection, so we can use a set name
   * for the data frame.
   * 
   * @param insts the instances to transfer into R
   */
  protected void acceptInstances(Instances insts) {
    m_busy = true;
    m_rWarning = false;

    RSession eng = null;
    try {
      if (m_logWrapper != null) {
        m_logWrapper.statusMessage("Aquiring R session");
        m_logWrapper.logMessage("Aquiring R session");
      }
      eng = RSession.acquireSession(this);
      eng.setLog(this, m_logWrapper);
    } catch (Exception ex) {
      stop();
      if (m_logWrapper != null) {
        String s = "Error. ";
        m_logWrapper.statusMessage(s + "See log for details.");
        m_logWrapper.logMessage(s + ex.getMessage());
      }
      ex.printStackTrace();
      m_busy = false;
      return;
    }

    if (m_logWrapper != null) {
      m_logWrapper
        .statusMessage("Converting incoming instances to R data frame...");
      m_logWrapper.logMessage("Converting incoming instances to R data frame");
    }
    if ((m_rScript != null && m_rScript.length() > 0)
      || (m_scriptFile != null && m_scriptFile.length() > 0)) {
      try {

        // transfer instances into an R data frame
        RUtils.instancesToDataFrame(eng, this, insts, "rdata");

        // now execute the user's script
        if ((m_scriptFile != null && m_scriptFile.length() > 0)
          || (m_rScript != null && m_rScript.length() > 0)) {
          executeUserScript();
        } else {
          if (m_logWrapper != null && !m_rWarning) {
            m_logWrapper.statusMessage("Finished.");
          }
        }

      } catch (REngineException e) {
        stop();
        if (m_logger != null) {
          String s = statusMessagePrefix() + "Error. ";
          m_logger.statusMessage(s + "See log for details.");
          m_logger.logMessage(s + e.getMessage());
        }
        e.printStackTrace();
        RSession.releaseSession(this);
        m_busy = false;
        return;
      } catch (REXPMismatchException r) {
        stop();
        if (m_logger != null) {
          String s = statusMessagePrefix() + "Error. ";
          m_logger.statusMessage(s + "See log for details.");
          m_logger.logMessage(s + r.getMessage());
        }
        r.printStackTrace();
        RSession.releaseSession(this);
        m_busy = false;
        return;
      } catch (RSessionException e) {
        if (m_logger != null) {
          String s = statusMessagePrefix() + "Error. ";
          m_logger.statusMessage(s + "See log for details.");
          m_logger.logMessage(s + e.getMessage());
        }
        e.printStackTrace();
        RSession.releaseSession(this);
        m_busy = false;
        return;
      }
    }

    m_busy = false;
    RSession.releaseSession(this);
  }

  /**
   * Execute the user's R script
   */
  protected void executeUserScript() {
    m_busy = true;
    RSession eng = null;
    try {
      if (m_logWrapper != null) {
        m_logWrapper.statusMessage("Aquiring R session");
        m_logWrapper.logMessage("Aquiring R session");
      }

      eng = RSession.acquireSession(this);
      eng.setLog(this, m_logWrapper);
      eng.clearConsoleBuffer(this);
    } catch (Exception ex) {
      stop();
      if (m_logWrapper != null) {
        m_logWrapper.statusMessage("Error. See log for details.");
        m_logWrapper.logMessage("Error " + ex.getMessage());
      }
      ex.printStackTrace();
      RSession.releaseSession(this);
      m_busy = false;
      return;
    }

    String script = null;
    if (m_scriptFile != null && m_scriptFile.length() > 0) {
      String scriptFile = m_scriptFile;
      try {
        scriptFile = m_env.substitute(scriptFile);
      } catch (Exception ex) {
      }

      // load script from file.
      StringBuffer sb = new StringBuffer();
      try {
        BufferedReader br = new BufferedReader(new FileReader(scriptFile));
        String line = null;
        while ((line = br.readLine()) != null) {
          sb.append(line).append("\n");
        }
        br.close();
      } catch (FileNotFoundException e) {
        stop();
        if (m_logWrapper != null) {
          m_logWrapper.statusMessage("Error. Script file not found.");
          m_logWrapper.logMessage("Error " + e.getMessage());
        }
        RSession.releaseSession(this);
        m_busy = false;
        return;
        // e.printStackTrace();
      } catch (IOException e) {
        stop();
        if (m_logWrapper != null) {
          m_logWrapper.statusMessage("Error. An error occurred while "
            + "reading script from file.");
          m_logWrapper.logMessage("Error " + e.getMessage());
        }
        RSession.releaseSession(this);
        m_busy = false;
        return;
        // e.printStackTrace();
      }

      script = sb.toString();
    } else {
      script = m_rScript;
    }

    try {
      script = m_env.substitute(script);
    } catch (Exception ex) {
    }

    try {
      if (m_logWrapper != null) {
        m_logWrapper.statusMessage("Executing user script...");
        m_logWrapper.logMessage("Executing user script");
      }
      REXP result = eng.parseAndEval(this, script);
      checkResultType(result, eng);

    } catch (REngineException e) {
      stop();
      if (m_logger != null) {
        String s = statusMessagePrefix() + "Error. ";
        m_logger.statusMessage(s + "See log for details.");
        m_logger.logMessage(s + e.getMessage());
      }
      e.printStackTrace();
      RSession.releaseSession(this);
      m_busy = false;
      return;
    } catch (REXPMismatchException e) {
      stop();
      if (m_logger != null) {
        String s = statusMessagePrefix() + "Error. ";
        m_logger.statusMessage(s + "See log for details.");
        m_logger.logMessage(s + e.getMessage());
      }
      e.printStackTrace();
      RSession.releaseSession(this);
      m_busy = false;
      return;
    } catch (Exception e) {
      stop();
      if (m_logger != null) {
        String s = statusMessagePrefix() + "Error. ";
        m_logger.statusMessage(s + "See log for details.");
        m_logger.logMessage(s + e.getMessage());
      }
      e.printStackTrace();
      RSession.releaseSession(this);
      m_busy = false;
      return;
    }

    RSession.releaseSession(this);
    m_busy = false;

    if (m_logWrapper != null && !m_rWarning) {
      m_logWrapper.statusMessage("Finished.");
    }
  }

  protected void checkResultType(REXP r, RSession eng) throws Exception {
    String result = null;
    Instances outInst = null;
    if (r.isList()) {
      // System.out.println("Result is a list!");
      /*
       * if (m_textListeners.size() > 0) { result = listToText(r); }
       */

      // if there are some data set listeners
      // then call the routine to turn this into Instances
      if (m_dataListeners.size() > 0) {
        try {
          // outInst = dataFrameToInstances(r);
          outInst = RUtils.dataFrameToInstances(r);
        } catch (Exception ex) {
          if (m_logger != null) {
            m_logger.logMessage(statusMessagePrefix() + ex.getMessage());
            ex.printStackTrace();
          }
        }
      }
    }

    if (m_dataListeners.size() > 0 && outInst != null) {
      DataSetEvent d = new DataSetEvent(this, outInst);
      notifyDataListeners(d);
    }

    if (m_textListeners.size() > 0) {

      // grab buffer and create text event
      String consoleOut = eng.getConsoleBuffer(this);
      if (consoleOut != null && consoleOut.length() > 0) {
        TextEvent t = new TextEvent(this, consoleOut, "R script result");
        notifyTextListeners(t);
      }
    }

    // System.out.println("Debug " + r.toDebugString());
  }

  /**
   * Set environment variables to use
   * 
   * @param env the environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Accept a test set and convert it into a data frame in R
   * 
   * @param e the test set event
   */
  @Override
  public void acceptTestSet(TestSetEvent e) {

    if (e.isStructureOnly()) {
      return;
    }

    acceptInstances(e.getTestSet());
  }

  /**
   * Accept a training set and convert it into a data frame in R
   * 
   * @param e the training set event
   */
  @Override
  public void acceptTrainingSet(TrainingSetEvent e) {
    if (e.isStructureOnly()) {
      return;
    }

    acceptInstances(e.getTrainingSet());
  }

  /**
   * Accept a data set and convert it into a data frame in R
   * 
   * @param e the data set event
   */
  @Override
  public void acceptDataSet(DataSetEvent e) {
    if (e.isStructureOnly()) {
      return;
    }

    acceptInstances(e.getDataSet());
    // executeUserScript(true);
  }

  /**
   * Returns true if, at this time, the named event can be generated
   * 
   * @param eventName the event to check
   */
  @Override
  public boolean eventGeneratable(String eventName) {
    /*
     * if (m_listenee == null) { return false; }
     */

    if (!eventName.equals("text") && !eventName.equals("dataSet")
      && !eventName.equals("image")) {
      return false;
    }

    if ((m_rScript == null || m_rScript.length() == 0)
      && (m_scriptFile == null || m_scriptFile.length() == 0)) {
      return false;
    }

    return true;
  }

  /**
   * Set a custom (descriptive) name for this bean
   * 
   * @param name the name to use
   */
  @Override
  public void setCustomName(String name) {
    m_visual.setText(name);
  }

  /**
   * Get the custom (descriptive) name for this bean (if one has been set)
   * 
   * @return the custom name (or the default name)
   */
  @Override
  public String getCustomName() {
    return m_visual.getText();
  }

  /**
   * Stop any processing that the bean might be doing.
   */
  @Override
  public void stop() {
    if (m_listenee != null) {
      if (m_listenee instanceof BeanCommon) {
        ((BeanCommon) m_listenee).stop();
      }
    }

    if (m_logger != null) {
      m_logger.statusMessage(statusMessagePrefix() + "Stopped");
    }

    m_busy = false;
  }

  /**
   * Returns true if. at this time, the bean is busy with some (i.e. perhaps a
   * worker thread is performing some calculation).
   * 
   * @return true if the bean is busy.
   */
  @Override
  public boolean isBusy() {
    return m_busy;
  }

  /**
   * Set a logger
   * 
   * @param logger a <code>weka.gui.Logger</code> value
   */
  @Override
  public void setLog(Logger logger) {
    m_logger = logger;
    m_logWrapper = new LogWrapper(m_logger, statusMessagePrefix());
  }

  /**
   * Returns true if, at this time, the object will accept a connection via the
   * named event
   * 
   * @param esd the EventSetDescriptor for the event in question
   * @return true if the object will accept a connection
   */
  @Override
  public boolean connectionAllowed(EventSetDescriptor esd) {
    return connectionAllowed(esd.getName());
  }

  /**
   * Returns true if a connection can be made to this step using the named
   * connection type
   * 
   * @param eventName the connection type
   */
  @Override
  public boolean connectionAllowed(String eventName) {
    if (!eventName.equals("instance") && !eventName.equals("dataSet")
      && !eventName.equals("trainingSet") && !eventName.equals("testSet")) {
      return false;
    }

    if (m_listenee != null) {
      return false;
    }

    return true;
  }

  /**
   * Notification of a particular connection made to us
   * 
   * @param eventName the connection type
   * @param source the source step connecting to us
   */
  @Override
  public void connectionNotification(String eventName, Object source) {
    if (connectionAllowed(eventName)) {
      m_listenee = source;
    }
  }

  /**
   * Notification of disconnection from us
   * 
   * @param eventName the connection type being removed
   * @param source the step disconnecting from us
   */
  @Override
  public void disconnectionNotification(String eventName, Object source) {
    if (source == m_listenee) {
      m_listenee = null;
    }
  }

  /**
   * Use the default icon for this step
   */
  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "RScriptExecutor.gif",
      BeanVisual.ICON_PATH + "RScriptExecutor_animated.gif");
    m_visual.setText("RScriptExcecutor");
  }

  /**
   * Set the visual for this step
   * 
   * @param newVisual the visual representation to use
   */
  @Override
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  /**
   * Get the visual representation of this step
   * 
   * @return the visual representation of this step
   */
  @Override
  public BeanVisual getVisual() {
    return m_visual;
  }

  protected String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|";
  }

  @SuppressWarnings("unchecked")
  private void notifyTextListeners(TextEvent e) {
    List<TextListener> l;

    synchronized (this) {
      l = (List<TextListener>) m_textListeners.clone();
    }

    if (l.size() > 0) {
      for (TextListener t : l) {
        t.acceptText(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void notifyDataListeners(DataSetEvent d) {
    List<DataSourceListener> l;

    synchronized (this) {
      l = (List<DataSourceListener>) m_dataListeners.clone();
    }

    if (l.size() > 0) {
      for (DataSourceListener t : l) {
        t.acceptDataSet(d);
      }
    }
  }

  /**
   * Add a text listener
   * 
   * @param cl a <code>TextListener</code> value
   */
  public synchronized void addTextListener(TextListener cl) {
    m_textListeners.add(cl);
  }

  /**
   * Remove a text listener
   * 
   * @param cl a <code>TextListener</code> value
   */
  public synchronized void removeTextListener(TextListener cl) {
    m_textListeners.remove(cl);
  }

  /**
   * Add a data source listener
   * 
   * @param dl a <code>DataSourceListener</code> value
   */
  public synchronized void addDataSourceListener(DataSourceListener dl) {
    m_dataListeners.add(dl);
  }

  /**
   * Remove a data source listener
   * 
   * @param dl a <code>DataSourceListener</code> value
   */
  public synchronized void removeDataSourceListener(DataSourceListener dl) {
    m_dataListeners.remove(dl);
  }

  /**
   * Add an image listener
   * 
   * @param i a <code>ImageListener</code> value
   */
  public synchronized void addImageListener(ImageListener i) {
    m_imageListeners.add(i);
  }

  /**
   * Remove an data image listener
   * 
   * @param i a <code>ImageListener</code> value
   */
  public synchronized void removeImageListener(ImageListener i) {
    m_imageListeners.remove(i);
  }

  /**
   * Start this step executing.
   */
  @Override
  public void start() {
    if (m_listenee == null) {
      if ((m_rScript != null && m_rScript.length() > 0)
        || (m_scriptFile != null && m_scriptFile.length() > 0)) {
        m_rWarning = false;
        executeUserScript();
      }
    }
  }

  /**
   * Get the start message string (no longer used in Weka 3.7). Also indicates
   * when this step can't be the start point in flow.
   * 
   * @return the start message string
   */
  @Override
  public String getStartMessage() {
    String message = "Execute script";

    if (m_listenee != null) {
      message = "$" + message;
    }

    return message;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void imageGenerated(BufferedImage image) {
    List<ImageListener> l;

    synchronized (this) {
      l = (List<ImageListener>) m_imageListeners.clone();
    }

    ImageEvent d = new ImageEvent(this, image);
    if (l.size() > 0) {
      for (ImageListener t : l) {
        t.acceptImage(d);
      }
    }
  }
}
