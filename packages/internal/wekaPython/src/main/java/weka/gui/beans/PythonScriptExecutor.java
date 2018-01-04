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
 *    PythonScriptExcecutor.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.EventSetDescriptor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.Logger;
import weka.python.PythonSession;

/**
 * Knowledge Flow component for executing a CPython script.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Scripting", toolTipText = "CPython scripting step")
public class PythonScriptExecutor extends JPanel implements BeanCommon,
  Visible, EventConstraints, Serializable, TrainingSetListener,
  TestSetListener, DataSourceListener, EnvironmentHandler, Startable {

  /** For serialization */
  private static final long serialVersionUID = 8220123645676178107L;

  /** Visual for this bean */
  protected BeanVisual m_visual = new BeanVisual("PythonScriptExecutor",
    BeanVisual.ICON_PATH + "PythonScriptExecutor.gif", BeanVisual.ICON_PATH
      + "PythonScriptExecutor.gif");

  /** Downstream steps interested in text events from us */
  protected ArrayList<TextListener> m_textListeners =
    new ArrayList<TextListener>();

  /** Downstream steps interested in Instances from us */
  protected ArrayList<DataSourceListener> m_dataListeners =
    new ArrayList<DataSourceListener>();

  /** Downstream steps interested in image events from us */
  protected ArrayList<ImageListener> m_imageListeners =
    new ArrayList<ImageListener>();

  /** Step talking to us */
  protected Object m_listenee;

  protected transient Environment m_env;
  protected transient Logger m_logger;

  /** The script to execute */
  protected String m_pyScript = "";

  /** Script file overrides any encapsulated script */
  protected String m_scriptFile = "";

  /** Variables to attempt to get from python as output */
  protected String m_varsToGet = "";

  /** True when this step is busy */
  protected transient boolean m_busy;

  /** Whether to output debugging info (from this step and the python server) */
  protected boolean m_debug;

  /** Attempt to keep going after detecting output on sys error from python */
  protected boolean m_continueOnSysErr;

  /**
   * Global info on this step
   *
   * @return global help info on this step
   */
  public String globalInfo() {
    return "A Knowledge Flow component that executes a user-supplied CPython "
      + "script. The script may be supplied via the GUI editor for the step "
      + "or from a file at run time. Incoming instances will be transferred to "
      + "pandas data frame (called py_data) in the Python environment.";
  }

  /**
   * Constructor
   */
  public PythonScriptExecutor() {
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);

    m_env = Environment.getSystemWide();
  }

  /**
   * Set the python script to execute. This will get ignored if a file to load a
   * script at run time has been set
   *
   * @param script the script to execute
   */
  public void setPythonScript(String script) {
    m_pyScript = script;
  }

  /**
   * Get the python script to execute. This might be null or empty if a script
   * is being sourced from a file at run time
   *
   * @return the script to execute
   */
  public String getPythonScript() {
    return m_pyScript;
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
   * Set the variables to retrieve from python
   *
   * @param varsToGet a comma-separated list of variables to get from python
   */
  public void setVariablesToGetFromPython(String varsToGet) {
    m_varsToGet = varsToGet;
  }

  /**
   * Get the list of variables to retrieve from python.
   *
   * @return a comma-separated list of variables to retrieve from python
   */
  public String getVariablesToGetFromPython() {
    return m_varsToGet;
  }

  /**
   * Set whether to output debugging info (both client and server side)
   *
   * @param debug true if debugging info is to be output
   */
  public void setDebug(boolean debug) {
    m_debug = debug;
  }

  /**
   * Get whether to output debugging info (both client and server side)
   *
   * @return true if debugging info is to be output
   */
  public boolean getDebug() {
    return m_debug;
  }

  @Override
  public void setCustomName(String name) {
    m_visual.setText(name);
  }

  @Override
  public String getCustomName() {
    return m_visual.getText();
  }

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

  @Override
  public boolean isBusy() {
    return m_busy;
  }

  @Override
  public void setLog(Logger logger) {
    m_logger = logger;
  }

  @Override
  public boolean connectionAllowed(EventSetDescriptor esd) {
    return connectionAllowed(esd.getName());
  }

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

  @Override
  public void connectionNotification(String eventName, Object source) {
    if (connectionAllowed(eventName)) {
      m_listenee = source;
    }
  }

  @Override
  public void disconnectionNotification(String eventName, Object source) {
    if (source == m_listenee) {
      m_listenee = null;
    }
  }

  @Override
  public void acceptDataSet(DataSetEvent e) {
    if (e.isStructureOnly()) {
      return;
    }

    acceptInstances(e.getDataSet());
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  @Override
  public boolean eventGeneratable(String eventName) {
    if (!eventName.equals("text") && !eventName.equals("dataSet")
      && !eventName.equals("image")) {
      return false;
    }

    if ((m_pyScript == null || m_pyScript.length() == 0)
      && (m_scriptFile == null || m_scriptFile.length() == 0)) {
      return false;
    }

    return true;
  }

  @Override
  public void start() throws Exception {
    if (m_listenee == null) {
      if ((m_pyScript != null && m_pyScript.length() > 0)
        || (m_scriptFile != null && m_scriptFile.length() > 0)) {
        String script = m_pyScript;
        if (m_scriptFile != null && m_scriptFile.length() > 0) {
          script = loadScript();
        }
        executeScript(getSession(), script);
      }
    }
  }

  @Override
  public String getStartMessage() {
    String message = "Execute script";

    if (m_listenee != null) {
      message = "$" + message;
    }

    return message;
  }

  @Override
  public void acceptTestSet(TestSetEvent e) {
    if (e.isStructureOnly()) {
      return;
    }

    acceptInstances(e.getTestSet());
  }

  @Override
  public void acceptTrainingSet(TrainingSetEvent e) {
    if (e.isStructureOnly()) {
      return;
    }

    acceptInstances(e.getTrainingSet());
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "PythonScriptExecutor.gif",
      BeanVisual.ICON_PATH + "PythonScriptExecutor.gif");
    m_visual.setText("PythonScriptExcecutor");
  }

  @Override
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  @Override
  public BeanVisual getVisual() {
    return m_visual;
  }

  protected String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|";
  }

  /**
   * Write to the log and/or status area of the Knowledge Flow.
   *
   * @param logMessage log message to write (can be null for none)
   * @param statusMessage status message to write (can be null for none)
   * @param ex exception to write to the log (can be null)
   */
  protected void log(String logMessage, String statusMessage, Exception ex) {
    if (m_logger != null) {
      if (ex != null) {
        m_logger.statusMessage(statusMessagePrefix()
          + "Error. See log for details.");
        m_logger.logMessage(statusMessagePrefix() + ex.getMessage());
        ex.printStackTrace();
      } else {
        if (logMessage != null) {
          m_logger.logMessage(statusMessagePrefix() + logMessage);
        }
        if (statusMessage != null) {
          m_logger.statusMessage(statusMessagePrefix() + statusMessage);
        }
      }
    }
  }

  protected PythonSession getSession() throws Exception {
    PythonSession session;
    if (!PythonSession.pythonAvailable()) {
      // try initializing
      if (!PythonSession.initSession("python", getDebug())) {
        String envEvalResults = PythonSession.getPythonEnvCheckResults();
        throw new Exception("Was unable to start python environment:\n\n"
          + envEvalResults);
      }
    }

    session = PythonSession.acquireSession(this);
    session.setLog(m_logger);

    return session;
  }

  /**
   * Accept and process incoming instances. Transfers instances into Python as a
   * pandas dataframe called "py_data". Then executes the user's script and
   * retrieves any named variables from pythong
   *
   * @param insts the instances to transfer
   */
  protected void acceptInstances(Instances insts) {
    m_busy = true;
    PythonSession session = null;
    try {
      session = getSession();

      if ((m_pyScript != null && m_pyScript.length() > 0)
        || (m_scriptFile != null && m_scriptFile.length() > 0)) {

        log("Converting incoming instances to pandas data frame",
          "Converting incoming instances to pandas data frame...", null);
        session.instancesToPython(insts, "py_data", getDebug());

        // now execute the user's script
        String script = m_pyScript;
        if (m_scriptFile != null && m_scriptFile.length() > 0) {
          script = loadScript();
        }

        executeScript(session, script);

        log(null, "Finished.", null);
      }
    } catch (Exception ex) {
      log(null, null, ex);
      stop();
      if (getDebug()) {
        if (session != null) {
          try {
            log("Getting debug info....", null, null);
            List<String> outAndErr = session.getPythonDebugBuffer(getDebug());
            log("Output from python:\n" + outAndErr.get(0), null, null);
            log("Error from python:\n" + outAndErr.get(1), null, null);
          } catch (WekaException e) {
            log(null, null, e);
            e.printStackTrace();
          }
        }
        log("Releasing python session", null, null);
      }
      PythonSession.releaseSession(this);
    } finally {
      m_busy = false;
    }
  }

  /**
   * Load a python script to execute at run time. This takes precedence over any
   * script text specified.
   *
   * @return the loaded script
   * @throws Exception if a problem occurs
   */
  protected String loadScript() throws Exception {
    String scriptFile = m_env.substitute(m_scriptFile);

    StringBuilder sb = new StringBuilder();
    BufferedReader br = new BufferedReader(new FileReader(scriptFile));
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line).append("\n");
    }
    br.close();

    return sb.toString();
  }

  /**
   * Executes the supplied script
   *
   * @param session the PythonSession to use
   * @param script the script to execute
   */
  protected void executeScript(PythonSession session, String script) {
    try {
      script = m_env.substitute(script);
      log("Executing user script", "Executing user script...", null);
      List<String> outAndErr = session.executeScript(script, getDebug());
      if (outAndErr.size() == 2 && outAndErr.get(1).length() > 0) {
        if (m_continueOnSysErr) {
          log(outAndErr.get(1), null, null);
        } else {
          throw new Exception(outAndErr.get(1));
        }
      }

      if (m_varsToGet != null && m_varsToGet.length() > 0) {
        String[] vars = m_env.substitute(m_varsToGet).split(",");
        boolean[] ok = new boolean[vars.length];
        PythonSession.PythonVariableType[] types =
          new PythonSession.PythonVariableType[vars.length];

        // check that the named variables exist in python
        int i = 0;
        for (String v : vars) {
          if (!session.checkIfPythonVariableIsSet(v.trim(), getDebug())) {
            if (m_continueOnSysErr) {
              log("Requested output variable '" + v + "' does not seem "
                + "to be set in python", null, null);
            } else {
              throw new Exception("Requested output variable '" + v
                + "' does not seem to be set in python");
            }
          } else {
            ok[i] = true;
            types[i++] = session.getPythonVariableType(v, getDebug());
          }
        }

        for (i = 0; i < vars.length; i++) {
          if (ok[i]) {
            if (getDebug()) {
              log(null, "Retrieving variable '" + vars[i].trim()
                + "' from python. Type: " + types[i].toString(), null);
            }
            if (types[i] == PythonSession.PythonVariableType.DataFrame) {
              // converting to instances takes precedence over textual form
              // if we have data set listeners
              if (m_dataListeners.size() > 0) {
                Instances pyFrame =
                  session.getDataFrameAsInstances(vars[i].trim(), getDebug());
                DataSetEvent d = new DataSetEvent(this, pyFrame);
                notifyDataListeners(d);
              } else if (m_textListeners.size() > 0) {
                String textPyFrame =
                  session.getVariableValueFromPythonAsPlainString(
                    vars[i].trim(), getDebug());
                TextEvent t =
                  new TextEvent(this, textPyFrame, vars[i].trim()
                    + ": data frame");
                notifyTextListeners(t);
              }
            } else if (types[i] == PythonSession.PythonVariableType.Image) {
              if (m_imageListeners.size() > 0) {
                BufferedImage image =
                  session.getImageFromPython(vars[i].trim(), getDebug());
                ImageEvent ie = new ImageEvent(this, image, vars[i].trim());
                notifyImageListeners(ie);
              }
            } else if (types[i] == PythonSession.PythonVariableType.String
              || types[i] == PythonSession.PythonVariableType.Unknown) {
              if (m_textListeners.size() > 0) {
                String varAsText =
                  session.getVariableValueFromPythonAsPlainString(
                    vars[i].trim(), getDebug());
                TextEvent t = new TextEvent(this, varAsText, vars[i].trim());
                notifyTextListeners(t);
              }
            }
          }
        }
      }
    } catch (Exception ex) {
      log(null, null, ex);
      stop();
    } finally {
      if (getDebug()) {
        if (session != null) {
          try {
            log("Getting debug info....", null, null);
            List<String> outAndErr = session.getPythonDebugBuffer(getDebug());
            log("Output from python:\n" + outAndErr.get(0), null, null);
            log("Error from python:\n" + outAndErr.get(1), null, null);
          } catch (WekaException e) {
            log(null, null, e);
            e.printStackTrace();
          }
        }
        log("Releasing python session", null, null);
      }
      PythonSession.releaseSession(this);
      m_busy = false;
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
  private void notifyImageListeners(ImageEvent e) {
    List<ImageListener> l;

    synchronized (this) {
      l = (List<ImageListener>) m_imageListeners.clone();
    }

    if (l.size() > 0) {
      for (ImageListener t : l) {
        t.acceptImage(e);
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
}
