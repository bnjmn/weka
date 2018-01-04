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

package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.FilePropertyMetadata;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.python.PythonSession;

import javax.swing.JFileChooser;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Knowledge flow step (new knowledge flow) for executing a CPython script
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "PythonScriptExecutor", category = "Scripting",
  toolTipText = "CPython scripting step", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "PythonScriptExecutor.gif")
public class PythonScriptExecutor extends BaseStep {

  private static final long serialVersionUID = -491300310357178468L;

  /** The script to execute */
  protected String m_pyScript = "";

  /** Script file overrides any encapsulated script */
  protected File m_scriptFile = new File("");

  /** Variables to attempt to get from python as output */
  protected String m_varsToGet = "";

  /** Whether to output debugging info (from this step and the python server) */
  protected boolean m_debug;

  /** Attempt to keep going after detecting output on sys error from python */
  protected boolean m_continueOnSysErr;

  /**
   * Set whether to try and continue after seeing output on the sys error
   * stream. Some schemes write warnings (rather than errors) to sys error.
   *
   * @param c true if we should try to continue after seeing output on the sys
   *          error stream
   */
  public void setContinueOnSysErr(boolean c) {
    m_continueOnSysErr = c;
  }

  /**
   * Get whether to try and continue after seeing output on the sys error
   * stream. Some schemes write warnings (rather than errors) to sys error.
   *
   * @return true if we should try to continue after seeing output on the sys
   *         error stream
   */
  @OptionMetadata(
    displayName = "Try to continue after sys err output from script",
    description = "Try to continue after sys err output from script.\nSome schemes"
      + " report warnings to the system error stream.", displayOrder = 5)
  public
    boolean getContinueOnSysErr() {
    return m_continueOnSysErr;
  }

  /**
   * Set whether to output debugging info (both client and server side)
   *
   * @param debug true if debugging info is to be output
   */
  @OptionMetadata(displayName = "Output debugging info from python",
    description = "Whether to output debugging info from python",
    displayOrder = 10)
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

  /**
   * Set the python script to execute. This will get ignored if a file to load a
   * script at run time has been set
   *
   * @param script the script to execute
   */
  @ProgrammaticProperty
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
  @OptionMetadata(
    displayName = "File to load script from",
    description = "A file to load the python script from (if set takes precendence"
      + "over any script from the editor", displayOrder = 1)
  @FilePropertyMetadata(fileChooserDialogType = JFileChooser.OPEN_DIALOG,
    directoriesOnly = false)
  public void setScriptFile(File scriptFile) {
    m_scriptFile = scriptFile;
  }

  /**
   * Get the filename of the script to load at runtime
   *
   * @return the filename of the script to load at runtime
   */
  public File getScriptFile() {
    return m_scriptFile;
  }

  /**
   * Set the variables to retrieve from python
   *
   * @param varsToGet a comma-separated list of variables to get from python
   */
  @OptionMetadata(
    displayName = "Variables to get from Python",
    description = "A comma-separated list of variables to retrieve from Python",
    displayOrder = 2)
  public
    void setVariablesToGetFromPython(String varsToGet) {
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
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {

  }

  /**
   * Executed if we are operating as a start point
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void start() throws WekaException {
    if (getStepManager().numIncomingConnections() == 0) {
      if ((m_pyScript != null && m_pyScript.length() > 0)
        || (m_scriptFile != null && m_scriptFile.toString().length() > 0)) {
        String script = m_pyScript;
        if (m_scriptFile != null && m_scriptFile.toString().length() > 0) {
          try {
            script = loadScript();
          } catch (Exception ex) {
            throw new WekaException(ex);
          }
        }

        getStepManager().processing();
        executeScript(getSession(), script);
        getStepManager().finished();
      }
    }
  }

  /**
   * Process incoming instances
   *
   * @param data data object - we process dataSet, trainingSet and testSet
   *          connections
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    Instances incoming = data.getPrimaryPayload();

    if (incoming.numInstances() > 0) {
      PythonSession session = null;
      try {
        getStepManager().processing();
        session = getSession();

        if ((m_pyScript != null && m_pyScript.length() > 0)
          || (m_scriptFile != null && m_scriptFile.toString().length() > 0)) {
          getStepManager().logDetailed(
            "Converting incoming instances to " + "pandas data frame");
          session.instancesToPython(incoming, "py_data", getDebug());

          // now execute the user's script
          String script = m_pyScript;
          if (m_scriptFile != null && m_scriptFile.toString().length() > 0) {
            try {
              script = loadScript();
            } catch (IOException ex) {
              throw new WekaException(ex);
            }
          }

          executeScript(session, script);
        }
      } finally {
        if (PythonSession.pythonAvailable()) {
          PythonSession.releaseSession(this);
        }
        getStepManager().finished();
      }
    }
  }

  /**
   * Get the incoming connection types we can accept at the given time
   *
   * @return a list of incoming connection types
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnections() == 0) {
      result.add(StepManager.CON_DATASET);
      result.add(StepManager.CON_TRAININGSET);
      result.add(StepManager.CON_TESTSET);
    }
    return result;
  }

  /**
   * Get the outgoing connection types we can produce at the given time
   *
   * @return a list of outgoing connection types
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if ((m_pyScript != null && m_pyScript.length() > 0)
      || (m_scriptFile != null && m_scriptFile.length() > 0)) {
      result.add(StepManager.CON_DATASET);
      result.add(StepManager.CON_TEXT);
      result.add(StepManager.CON_IMAGE);
    }

    return result;
  }

  /**
   * Load a python script to execute at run time. This takes precedence over any
   * script text specified.
   *
   * @return the loaded script
   * @throws IOException if a problem occurs
   */
  protected String loadScript() throws IOException {
    String scriptFile = environmentSubstitute(m_scriptFile.toString());

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
   * Gets a python session object to use for interacting with python
   *
   * @return a PythonSession object
   * @throws WekaException if a problem occurs
   */
  protected PythonSession getSession() throws WekaException {
    PythonSession session;
    if (!PythonSession.pythonAvailable()) {
      // try initializing
      if (!PythonSession.initSession("python", getDebug())) {
        String envEvalResults = PythonSession.getPythonEnvCheckResults();
        throw new WekaException("Was unable to start python environment:\n\n"
          + envEvalResults);
      }
    }

    session = PythonSession.acquireSession(this);
    session.setLog(getStepManager().getLog());

    return session;
  }

  /**
   * Executes the supplied script
   *
   * @param session the PythonSession to use
   * @param script the script to execute
   */
  protected void executeScript(PythonSession session, String script)
    throws WekaException {
    try {
      script = environmentSubstitute(script);
      getStepManager().statusMessage("Executing user script");
      getStepManager().logBasic("Executing user script");
      List<String> outAndErr = session.executeScript(script, getDebug());
      if (outAndErr.size() == 2 && outAndErr.get(1).length() > 0) {
        if (m_continueOnSysErr) {
          getStepManager().logWarning(outAndErr.get(1));
        } else {
          throw new WekaException(outAndErr.get(1));
        }
      }

      if (m_varsToGet != null && m_varsToGet.length() > 0) {
        String[] vars = environmentSubstitute(m_varsToGet).split(",");
        boolean[] ok = new boolean[vars.length];
        PythonSession.PythonVariableType[] types =
          new PythonSession.PythonVariableType[vars.length];

        // check that the named variables exist in python
        int i = 0;
        for (String v : vars) {
          if (!session.checkIfPythonVariableIsSet(v.trim(), getDebug())) {
            if (m_continueOnSysErr) {
              getStepManager().logWarning(
                "Requested output variable '" + v + "' does not seem "
                  + "to be set in python");
            } else {
              throw new WekaException("Requested output variable '" + v
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
              getStepManager().logDetailed(
                "Retrieving variable '" + vars[i].trim()
                  + "' from python. Type: " + types[i].toString());
            }
            if (types[i] == PythonSession.PythonVariableType.DataFrame) {
              // converting to instances takes precedence over textual form
              // if we have data set listeners
              if (getStepManager().numOutgoingConnectionsOfType(
                StepManager.CON_DATASET) > 0) {
                Instances pyFrame =
                  session.getDataFrameAsInstances(vars[i].trim(), getDebug());
                Data output = new Data(StepManager.CON_DATASET, pyFrame);
                output.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
                output.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
                  1);
                getStepManager().outputData(output);
              } else if (getStepManager().numOutgoingConnectionsOfType(
                StepManager.CON_TEXT) > 0) {
                String textPyFrame =
                  session.getVariableValueFromPythonAsPlainString(
                    vars[i].trim(), getDebug());
                Data output = new Data(StepManager.CON_TEXT, textPyFrame);
                output.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
                  vars[i].trim() + ": data frame");
                getStepManager().outputData(output);
              }
            } else if (types[i] == PythonSession.PythonVariableType.Image) {
              if (getStepManager().numOutgoingConnectionsOfType(
                StepManager.CON_IMAGE) > 0) {
                BufferedImage image =
                  session.getImageFromPython(vars[i].trim(), getDebug());
                Data output = new Data(StepManager.CON_IMAGE, image);
                output.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
                  vars[i].trim());
                getStepManager().outputData(output);
              }
            } else if (types[i] == PythonSession.PythonVariableType.String
              || types[i] == PythonSession.PythonVariableType.Unknown) {
              if (getStepManager().numOutgoingConnectionsOfType(
                StepManager.CON_TEXT) > 0) {
                String varAsText =
                  session.getVariableValueFromPythonAsPlainString(
                    vars[i].trim(), getDebug());
                Data output = new Data(StepManager.CON_TEXT, varAsText);
                output.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
                  vars[i].trim());
                getStepManager().outputData(output);
              }
            }
          }
        }
      }
    } finally {
      if (getDebug()) {
        if (session != null) {
          getStepManager().logBasic("Getting debug info....");
          List<String> outAndErr = session.getPythonDebugBuffer(getDebug());
          getStepManager().logBasic(
            "System output from python:\n" + outAndErr.get(0));
          getStepManager().logBasic(
            "System err from python:\n" + outAndErr.get(1));
        }

      }
      getStepManager().logBasic("Releasing python session");
    }
    PythonSession.releaseSession(this);
  }

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.PythonScriptExecutorStepEditorDialog";
  }
}
