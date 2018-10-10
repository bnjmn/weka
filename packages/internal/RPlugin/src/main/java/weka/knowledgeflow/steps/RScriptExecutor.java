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
 *    Copyright (C) 2012-2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import org.rosuda.REngine.REXP;
import weka.core.Instances;
import weka.core.JavaGDListener;
import weka.core.OptionMetadata;
import weka.core.RLoggerAPI;
import weka.core.RSession;
import weka.core.RUtils;
import weka.core.WekaException;
import weka.gui.FilePropertyMetadata;
import weka.gui.Logger;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import javax.swing.JFileChooser;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
 *
 * <p>
 * Data frames in R can be extracted, converted to Weka Instances and passed on
 * via dataSet connections by simply stating the name of the data frame in the
 * user's R script. This <b>must</b> be the last command in the script. E.g:
 * </p>
 *
 * <pre>
 * data(iris)
 * iris
 * </pre>
 *
 * <p>
 * Graphics in R can be captured by making use of the JavaGD graphics device for
 * R https://cran.r-project.org/web/packages/JavaGD/index.html). The Weka RPlugin will
 * attempt to install this package for the user automatically - if this fails
 * for some reason then the package should be installed by the user from R via
 * install.packages("JavaGD"). The default size for graphics created in R via
 * JavaGD is 800x600. The user can alter this in their R scripts by executing
 * the following command:
 * </p>
 *
 * <pre>
 * JavaGD(width = w, height = h)
 * </pre>
 *
 * <p>
 * where "w" and "h" are the new width and height in pixels. Note that this
 * setting stays in effect until another "JavaGD()" command is executed.
 * </p>
 *
 * The RScriptExecutor can be connected to an ImageSaver component in order to
 * save any graphics generated to a file. If running within the graphical
 * Knowledge Flow UI any generated images will also be sent to the R
 * Console/Visualize perspective.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "RScriptExecutor", category = "Scripting",
  toolTipText = "A Knowledge Flow component that executes a user-supplied R script. The "
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
    + "for R (https://cran.r-project.org/web/packages/JavaGD/index.html). The Weka RPlugin will "
    + "attempt to install this package for the user automatically - if this fails "
    + "for some reason then the package should be installed by the user from R "
    + "via install.packages(\"JavaGD\"). The default size for graphics created in "
    + "R via JavaGD is 800x600. The user can alter this in their R scripts by "
    + "executing the following command:\n\n" + "JavaGD(width=w, height=h)\n\n"
    + "where \"w\" and \"h\" are the new width and height in pixels. Note that this "
    + "setting stays in effect until another \"JavaGD()\" command is executed.\n\n"
    + "The RScriptExecutor can be connected to "
    + "an ImageSaver component in order to save any graphics generated to a file. "
    + "If running within the graphical Knowledge Flow UI any generated images will "
    + "also be sent to the R Console/Visualize perspective.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "RScriptExecutor.gif")
public class RScriptExecutor extends BaseStep implements JavaGDListener {

  private static final long serialVersionUID = -6969085410136820569L;

  /** The R script to execute */
  protected String m_rScript = "";

  /** Script file overrides any encapsulated script */
  protected File m_scriptFile = new File("");

  protected transient boolean m_rWarning;

  protected transient LogWrapper m_logWrapper;

  /**
   * Set the script to execute
   *
   * @param script the R script to execute
   */
  @ProgrammaticProperty
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
  @OptionMetadata(displayName = "File to load script from",
    description = "A file to load the R script from (if set takes precedence"
      + "over any script from the editor",
    displayOrder = 1)
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

  @Override
  public void stepInit() throws WekaException {
    m_rWarning = false;
    if (m_logWrapper == null) {
      m_logWrapper = new LogWrapper(getStepManager().getLog(),
        ((StepManagerImpl) getStepManager()).stepStatusMessagePrefix());
    }
  }

  @Override
  public void start() throws WekaException {
    if (getStepManager().numIncomingConnections() == 0) {
      if ((m_rScript != null && m_rScript.length() > 0)
        || (m_scriptFile != null && m_scriptFile.toString().length() > 0)) {
        String script = m_rScript;
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

  @Override
  public void processIncoming(Data data) throws WekaException {
    Instances incoming = data.getPrimaryPayload();

    if (incoming.numInstances() > 0) {
      getStepManager().processing();
      RSession session = getSession();

      if ((m_rScript != null && m_rScript.length() > 0)
        || (m_scriptFile != null && m_scriptFile.toString().length() > 0)) {
        getStepManager().logDetailed("Converting instances to R data frame");
        try {
          RUtils.instancesToDataFrame(session, this, incoming, "rdata");
          String script = m_rScript;
          if (m_scriptFile != null && m_scriptFile.toString().length() > 0) {
            script = loadScript();
          }

          executeScript(session, script);
          getStepManager().finished();
        } catch (Exception ex) {
          throw new WekaException(ex);
        } finally {
          RSession.releaseSession(this);
        }
      }
    }
  }

  protected void executeScript(RSession session, String script)
    throws WekaException {
    try {
      session.clearConsoleBuffer(this);
      script = environmentSubstitute(script);
      getStepManager().statusMessage("Executing user script");
      getStepManager().logBasic("Executing user script");

      REXP result = session.parseAndEval(this, script);
      checkResultType(result, session);
    } catch (Exception ex) {
      throw new WekaException(ex);
    } finally {
      RSession.releaseSession(this);
    }
  }

  protected void checkResultType(REXP r, RSession eng) throws Exception {
    Instances outInst;
    if (r.isList()) {
      // if there are some data set listeners
      // then call the routine to turn this into Instances
      if (getStepManager()
        .numOutgoingConnectionsOfType(StepManager.CON_DATASET) > 0) {
        outInst = RUtils.dataFrameToInstances(r);
        Data output = new Data(StepManager.CON_DATASET, outInst);
        output.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
        output.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
        getStepManager().outputData(output);
      }
    }

    if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_TEXT) > 0) {
      // grab buffer and create text event
      String consoleOut = eng.getConsoleBuffer(this);
      if (consoleOut != null && consoleOut.length() > 0) {
        Data output = new Data(StepManager.CON_TEXT, consoleOut);
        output.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
          "R script result");
        getStepManager().outputData(output);
      }
    }
  }

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

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if ((m_rScript != null && m_rScript.length() > 0)
      || (m_scriptFile != null && m_scriptFile.length() > 0)) {
      result.add(StepManager.CON_DATASET);
      result.add(StepManager.CON_TEXT);
      result.add(StepManager.CON_IMAGE);
    }

    return result;
  }

  /**
   * Get an R session object to use for interacting with R
   *
   * @return an RSession object
   * @throws WekaException if a problem occurs
   */
  protected RSession getSession() throws WekaException {
    RSession session;
    if (!RSession.rAvailable()) {
      throw new WekaException("The R environment is not available, or Weka "
        + "has not been configured correctly to access it.");
    }
    try {
      session = RSession.acquireSession(this);
      session.setLog(this, m_logWrapper);
    } catch (Exception e) {
      throw new WekaException(e);
    }
    return session;
  }

  /**
   * Load an R script to execute at run time. This takes precedence over any
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

  @Override
  public void imageGenerated(BufferedImage image) {
    Data imageD = new Data(StepManager.CON_IMAGE, image);
    imageD.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
      "Image from R");
    try {
      getStepManager().outputData(imageD);
    } catch (Exception ex) {
      getStepManager().logError(
        "A problem occurred while sending an " + "R image downstream", ex);
    }
  }

  /**
   * Wrap a log so that we can detect any error or warning messages coming back
   * from R and also so that status messages can be associated with the correct
   * KF component in the KF status area of the log.
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision: 11975 $
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

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.RScriptExecutorStepEditorDialog";
  }
}
