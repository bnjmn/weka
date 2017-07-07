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
 *    ExecuteProcess.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.JobEnvironment;
import weka.knowledgeflow.LogManager;
import weka.knowledgeflow.StepManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Knowledge Flow step that can execute static system commands or commands that
 * are dynamically defined by the values of attributes in incoming instance or
 * environment connections.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "ExecuteProcess",
  category = "Tools",
  toolTipText = "Execute either static or dynamic processes. Dynamic processes "
    + "can have commands, arguments and working directories specified in the "
    + "values of incoming string/nominal attributes in data-based or environment "
    + "connections.", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "ExecuteProcess.gif")
public class ExecuteProcess extends BaseStep {
  private static final long serialVersionUID = -9153714279540182885L;

  /** The process that is used to execute the user's command(s) */
  protected Process m_runningProcess;

  /** Single static command (may use env vars) */
  protected String m_staticExecCmd = "";

  /** Arguments (if necessary) for single static command */
  protected String m_staticArgs = "";

  /** Optional working directory for single static command */
  protected String m_staticWorkingDir = "";

  /** True to execute commands specified in incoming instance fields */
  protected boolean m_useDynamic;

  /**
   * Whether to raise an exception when a command fails completely (i.e. doesn't
   * exist or something) vs the case of a non-zero exit status. If not raising
   * an exception, then output indicating failure (with exist status = 1 in the
   * case of instance connections) will be generated
   */
  protected boolean m_raiseAnExceptionOnCommandFailure = true;

  /**
   * For commands and args in incoming instance connections (or connections that
   * pass through an instance as auxiliary data - CON_ENV, CON_VAR )
   */
  /** Name of attribute that will hold dynamic command to be executed */
  protected String m_fieldCmd = "";

  /** Name of attribute that will hold optional arguments for dynamic command */
  protected String m_fieldArgs = "";

  /**
   * Name of attribute that will hold optional working directory for dynamic
   * command
   */
  protected String m_fieldWorkingDir = "";

  /** Resolved attribute index of dynamic command */
  protected int m_cmdFieldIndex = -1;

  /** Resolved attribute index of dynamic arguments */
  protected int m_argsFieldIndex = -1;

  /** Resolved attribute index of dynamic working directory */
  protected int m_workingDirFieldIndex = -1;

  /** Std out from process */
  protected StringBuffer m_stdOutbuffer;

  /** Std err from process */
  protected StringBuffer m_stdErrBuffer;

  /** Structure of output for outgoing instance connections */
  protected Instances m_instanceOutHeader;

  /** True if the structure has been checked */
  protected boolean m_structureCheckComplete;

  /**
   * Get to raise an exception when a command fails completely (i.e. doesn't
   * exist or something) vs the case of a non-zero exit status. If not raising
   * an exception, then output indicating failure (with exist status = 1 in the
   * case of instance connections) will be generated.
   * 
   * @return true if an exception is to be generated on catastrophic command
   *         failure
   */
  public boolean getRaiseExceptionOnCommandFailure() {
    return m_raiseAnExceptionOnCommandFailure;
  }

  /**
   * Set to raise an exception when a command fails completely (i.e. doesn't
   * exist or something) vs the case of a non-zero exit status. If not raising
   * an exception, then output indicating failure (with exist status = 1 in the
   * case of instance connections) will be generated.
   *
   * @param raiseExceptionOnCommandFailure if an exception is to be generated on
   *          catastrophic command failure
   */
  public void setRaiseExceptionOnCommandFailure(
    boolean raiseExceptionOnCommandFailure) {
    m_raiseAnExceptionOnCommandFailure = raiseExceptionOnCommandFailure;
  }

  /**
   * Get whether to execute dynamic commands
   * 
   * @return true if dynamic commands are to be executed
   */
  public boolean getUseDynamic() {
    return m_useDynamic;
  }

  /**
   * Set whether to execute dynamic commands
   *
   * @param useDynamic true if dynamic commands are to be executed
   */
  public void setUseDynamic(boolean useDynamic) {
    m_useDynamic = useDynamic;
  }

  /**
   * Get the static command to be executed
   * 
   * @return the static command to be executed
   */
  public String getStaticCmd() {
    return m_staticExecCmd;
  }

  /**
   * Set the static command to be executed
   *
   * @param cmd the static command to be executed
   */
  public void setStaticCmd(String cmd) {
    m_staticExecCmd = cmd;
  }

  /**
   * Get the arguments for the static command
   * 
   * @return the arguments for the static command
   */
  public String getStaticArgs() {
    return m_staticArgs;
  }

  /**
   * Set the arguments for the static command
   *
   * @param args the arguments for the static command
   */
  public void setStaticArgs(String args) {
    m_staticArgs = args;
  }

  /**
   * Get the working directory for the static command
   * 
   * @return the working directory for the static command
   */
  public String getStaticWorkingDir() {
    return m_staticWorkingDir;
  }

  /**
   * Set the working directory for the static command
   *
   * @param workingDir the working directory for the static command
   */
  public void setStaticWorkingDir(String workingDir) {
    m_staticWorkingDir = workingDir;
  }

  /**
   * Get the name of the attribute in the incoming instance structure that
   * contains the command to execute
   * 
   * @return the name of the attribute containing the command to execute
   */
  public String getDynamicCmdField() {
    return m_fieldCmd;
  }

  /**
   * Set the name of the attribute in the incoming instance structure that
   * contains the command to execute
   *
   * @param cmdField the name of the attribute containing the command to execute
   */
  public void setDynamicCmdField(String cmdField) {
    m_fieldCmd = cmdField;
  }

  /**
   * Get the name of the attribute in the incoming instance structure that
   * contains the arguments to the command to execute
   * 
   * @return the name of the attribute containing the command's arguments
   */
  public String getDynamicArgsField() {
    return m_fieldArgs;
  }

  /**
   * Set the name of the attribute in the incoming instance structure that
   * contains the arguments to the command to execute
   *
   * @param argsField the name of the attribute containing the command's
   *          arguments
   */
  public void setDynamicArgsField(String argsField) {
    m_fieldArgs = argsField;
  }

  /**
   * Get the name of the attribute in the incoming instance structure that
   * containst the working directory for the command to execute
   *
   * @return the name of the attribute containing the command's working
   *         directory
   */
  public String getDynamicWorkingDirField() {
    return m_fieldWorkingDir;
  }

  /**
   * Set the name of the attribute in the incoming instance structure that
   * containst the working directory for the command to execute
   *
   * @param workingDirField the name of the attribute containing the command's
   *          working directory
   */
  public void setDynamicWorkingDirField(String workingDirField) {
    m_fieldWorkingDir = workingDirField;
  }

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    m_runningProcess = null;
    m_structureCheckComplete = false;

    Environment currentEnv =
      getStepManager().getExecutionEnvironment().getEnvironmentVariables();
    if (currentEnv != null && !(currentEnv instanceof JobEnvironment)) {
      currentEnv = new JobEnvironment(currentEnv);
      getStepManager().getExecutionEnvironment().setEnvironmentVariables(
        currentEnv);
    }

    if (!m_useDynamic && m_staticExecCmd.length() == 0) {
      throw new WekaException("No command to execute specified!");
    }

    if (m_useDynamic) {
      if (m_fieldCmd.length() == 0) {
        throw new WekaException(
          "No incoming attribute specified for obtaining command to execute!");
      }

      if (getStepManager().numIncomingConnections() == 0) {
        throw new WekaException(
          "Dynamic command to execute specified, but there "
            + "are no incoming connections!");
      }

      if (getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_INSTANCE) == 0
        && getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_ENVIRONMENT) == 0
        && getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_DATASET) == 0
        && getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_TRAININGSET) == 0
        && getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_TESTSET) == 0) {
        throw new WekaException(
          "Dynamic command execution can only be executed "
            + "on incoming instance, environment, dataset, trainingset or testset"
            + " connections");
      }
    }

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_INSTANCE) > 0
      || getStepManager().numOutgoingConnectionsOfType(
        StepManager.CON_ENVIRONMENT) > 0) {
      ArrayList<Attribute> atts = new ArrayList<>();
      atts.add(new Attribute("ExitStatus"));
      atts.add(new Attribute("StdOut", (List<String>) null));
      atts.add(new Attribute("StdErr", (List<String>) null));
      m_instanceOutHeader = new Instances("ProcessResults", atts, 0);
    }
  }

  /**
   * Start processing if operating as a start point in a flow
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void start() throws WekaException {
    if (getStepManager().numIncomingConnections() == 0) {
      try {
        ProcessBuilder builder = makeStaticProcess();
        getStepManager().processing();
        runProcess(builder, null, null, null);
      } catch (Exception e) {
        throw new WekaException(e);
      } finally {
        getStepManager().finished();
      }
    }
  }

  /**
   * Construct a ProcessBuilder instance for executing a static command
   *
   * @return a ProcessBuilder instance
   * @throws Exception if a problem occurs
   */
  protected ProcessBuilder makeStaticProcess() throws Exception {
    List<String> cmdList = new ArrayList<>();
    cmdList.add(environmentSubstitute(m_staticExecCmd));
    String[] args = Utils.splitOptions(environmentSubstitute(m_staticArgs));
    cmdList.addAll(Arrays.asList(args));

    ProcessBuilder builder =
      new ProcessBuilder(cmdList.toArray(new String[cmdList.size()]));
    if (m_staticWorkingDir.length() > 0) {
      builder.directory(new File(environmentSubstitute(m_staticWorkingDir)));
    }

    return builder;
  }

  /**
   * Construct a ProcessBuilder instance for executing a dynamic command
   *
   * @param incoming the incoming instance containing the command details
   * @return a ProcessBuilder instance
   * @throws Exception if a problem occurs
   */
  protected ProcessBuilder makeDynamicProcess(Instance incoming)
    throws Exception {

    if (!incoming.isMissing(m_cmdFieldIndex)) {
      String dynamicCommandVal =
        environmentSubstitute(incoming.stringValue(m_cmdFieldIndex));
      String dynamicOpts = "";
      String dynamicWorkingDir = "";
      if (m_argsFieldIndex >= 0) {
        if (!incoming.isMissing(m_argsFieldIndex)) {
          dynamicOpts =
            environmentSubstitute(incoming.stringValue(m_argsFieldIndex));
        }
      }
      if (m_workingDirFieldIndex >= 0) {
        if (!incoming.isMissing(m_workingDirFieldIndex)) {
          dynamicWorkingDir =
            environmentSubstitute(incoming.stringValue(m_workingDirFieldIndex));
        }
      }

      List<String> cmdList = new ArrayList<>();
      cmdList.add(dynamicCommandVal);
      String[] args = Utils.splitOptions(dynamicOpts);
      cmdList.addAll(Arrays.asList(args));
      ProcessBuilder builder =
        new ProcessBuilder(cmdList.toArray(new String[cmdList.size()]));
      if (dynamicWorkingDir.length() > 0) {
        builder.directory(new File(dynamicWorkingDir));
      }
      return builder;
    } else {
      getStepManager().logWarning(
        "Value of command to execute is missing in " + "incoming instance");
      return null;
    }
  }

  /**
   * Execute a configured process
   *
   * @param builder the ProcessBuilder to execute
   * @param varsToSet environment variables to pass on to the ProcessBuilder
   * @param propsToSet properties to pass on downstream
   * @param results results to pass on downstream
   * @return exit status code of process
   * @throws IOException if a problem occurs
   * @throws InterruptedException if a problem occurs
   * @throws WekaException if a problem occurs
   */
  protected void runProcess(ProcessBuilder builder,
    Map<String, String> varsToSet, Map<String, Map<String, String>> propsToSet,
    Map<String, LinkedHashSet<Data>> results) throws IOException,
    InterruptedException, WekaException {
    Map<String, String> env = builder.environment();
    // add environment vars
    Environment flowEnv =
      getStepManager().getExecutionEnvironment().getEnvironmentVariables();
    if (flowEnv != null) {
      Set<String> vars = flowEnv.getVariableNames();
      for (String var : vars) {
        env.put(var, flowEnv.getVariableValue(var));
      }
    }

    StringWriter stdOutWriter = new StringWriter();
    StringWriter stdErrWriter = new StringWriter();
    try {
      m_stdOutbuffer = new StringBuffer();
      m_stdErrBuffer = new StringBuffer();
      m_runningProcess = builder.start();
      copy(m_runningProcess.getInputStream(), stdOutWriter);
      copy(m_runningProcess.getErrorStream(), stdErrWriter);
      int status = m_runningProcess.waitFor();

      m_stdOutbuffer = stdOutWriter.getBuffer();
      m_stdErrBuffer = stdErrWriter.getBuffer();
      if (status == 0) {
        handleOutputSuccess(varsToSet, propsToSet, results,
          Utils.joinOptions(builder.command().toArray(new String[0])));
      } else {
        handleOutputFailure(status, varsToSet, propsToSet, results,
          Utils.joinOptions(builder.command().toArray(new String[0])));
      }
    } catch (IOException ex) {
      if (m_raiseAnExceptionOnCommandFailure) {
        throw ex;
      }
      getStepManager().logWarning(
        "Command: "
          + Utils.joinOptions(builder.command().toArray(new String[0]))
          + " failed with exception:\n" + LogManager.stackTraceToString(ex));
      handleOutputFailure(1, varsToSet, propsToSet, results,
        Utils.joinOptions(builder.command().toArray(new String[0])));
    }
  }

  /**
   * Output data relating to successful execution of a command
   *
   * @param varsToSet environment variables to pass on downstream
   * @param propsToSet properties to pass on downstream
   * @param results results to pass on downstream
   * @param command the actual command that was executed
   * @throws WekaException if a problem occurs
   */
  protected void handleOutputSuccess(Map<String, String> varsToSet,
    Map<String, Map<String, String>> propsToSet,
    Map<String, LinkedHashSet<Data>> results, String command)
    throws WekaException {
    if (getStepManager().numOutgoingConnectionsOfType(
      StepManager.CON_JOB_SUCCESS) > 0) {
      Data success =
        new Data(StepManager.CON_JOB_SUCCESS,
          m_stdOutbuffer.length() > 0 ? m_stdOutbuffer.toString()
            : "Process completed successfully: " + command);
      success.setPayloadElement(StepManager.CON_AUX_DATA_IS_INCREMENTAL, true);
      addAuxToData(success, varsToSet, propsToSet, results);
      getStepManager().outputData(success);
    }

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {
      // conn instance presumes that additional info (such as props/env settings
      // in instance data and results are not needed in the downstream steps).
      // Otherwise, conn job success/failure or conn environment should be used
      // as output

      m_instanceOutHeader.attribute(1).setStringValue(
        m_stdOutbuffer.length() > 0 ? m_stdOutbuffer.toString()
          : "Process completed successfully");
      m_instanceOutHeader.attribute(2).setStringValue(
        m_stdErrBuffer.length() > 0 ? m_stdErrBuffer.toString() : "");
      double[] vals = new double[3];
      vals[0] = 0; // success
      Instance outputInst = new DenseInstance(1.0, vals);
      outputInst.setDataset(m_instanceOutHeader);
      Data instOut = new Data(StepManager.CON_INSTANCE, outputInst);
      getStepManager().outputData(instOut);
    }

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_TEXT) > 0) {
      Data textOut = new Data(StepManager.CON_TEXT, m_stdOutbuffer.toString());
      textOut.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
        "Process completed successfully: " + command);
      getStepManager().outputData(textOut);
    }
  }

  /**
   * Output data relating to an unsuccessful execution of a command
   *
   * @param returnCode the return code generated by the process
   * @param varsToSet environment variables to pass on downstream
   * @param propsToSet properties to pass on downstream
   * @param results results to pass on downstream
   * @param command the command that was executed
   * @throws WekaException if a problem occurs
   */
  protected void handleOutputFailure(int returnCode,
    Map<String, String> varsToSet, Map<String, Map<String, String>> propsToSet,
    Map<String, LinkedHashSet<Data>> results, String command)
    throws WekaException {
    if (getStepManager().numOutgoingConnectionsOfType(
      StepManager.CON_JOB_FAILURE) > 0) {
      Data failure =
        new Data(StepManager.CON_JOB_FAILURE,
          m_stdErrBuffer.length() > 0 ? m_stdErrBuffer.toString()
            : "Process did not complete successfully - return code "
              + returnCode);
      failure.setPayloadElement(StepManager.CON_AUX_DATA_IS_INCREMENTAL, true);
      addAuxToData(failure, varsToSet, propsToSet, results);
      getStepManager().outputData(failure);
    }

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {

      m_instanceOutHeader.attribute(1).setStringValue(
        m_stdOutbuffer.length() > 0 ? m_stdOutbuffer.toString() : "");
      m_instanceOutHeader.attribute(2).setStringValue(
        m_stdErrBuffer.length() > 0 ? m_stdErrBuffer.toString()
          : "Process did " + "not complete successfully");
      double[] vals = new double[3];
      vals[0] = returnCode; // failure code
      Instance outputInst = new DenseInstance(1.0, vals);
      outputInst.setDataset(m_instanceOutHeader);
      Data instOut = new Data(StepManager.CON_INSTANCE, outputInst);
      getStepManager().outputData(instOut);
    }

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_TEXT) > 0) {
      Data textOut = new Data(StepManager.CON_TEXT, m_stdErrBuffer.toString());
      textOut.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
        "Process did not complete successfully: " + command);
      getStepManager().outputData(textOut);
    }
  }

  /**
   * Adds auxilliary information to a Data object
   *
   * @param data the Data object to add to
   * @param varsToSet environment variables to add
   * @param propsToSet properties to add
   * @param results results to add
   */
  protected void addAuxToData(Data data, Map<String, String> varsToSet,
    Map<String, Map<String, String>> propsToSet,
    Map<String, LinkedHashSet<Data>> results) {
    if (varsToSet != null) {
      data.setPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_VARIABLES,
        varsToSet);
    }
    if (propsToSet != null) {
      data.setPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_PROPERTIES,
        propsToSet);
    }
    if (results != null) {
      data.setPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_RESULTS,
        results);
    }
  }

  /**
   * Process an incoming Data object
   *
   * @param data the payload to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    if (!m_structureCheckComplete) {
      m_structureCheckComplete = true;

      Instances structure = null;
      if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
        structure = ((Instance) data.getPrimaryPayload()).dataset();
      } else if (data.getConnectionName().equals(StepManager.CON_ENVIRONMENT)) {
        structure =
          ((Instance) data.getPayloadElement(StepManager.CON_AUX_DATA_INSTANCE))
            .dataset();
      } else {
        structure = data.getPrimaryPayload();
      }

      checkStructure(structure);
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
      return;
    }

    if (data.isIncremental()) {
      if (getStepManager().isStreamFinished(data)) {
        Data finished = new Data(data.getConnectionName());
        if (data.getConnectionName().equals(StepManager.CON_ENVIRONMENT)
          || data.getConnectionName().equals(StepManager.CON_JOB_SUCCESS)
          || data.getConnectionName().equals(StepManager.CON_JOB_FAILURE)) {
          finished
            .setPayloadElement(
              StepManager.CON_AUX_DATA_ENVIRONMENT_VARIABLES,
              data
                .getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_VARIABLES));
          finished
            .setPayloadElement(
              StepManager.CON_AUX_DATA_ENVIRONMENT_PROPERTIES,
              data
                .getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_PROPERTIES));
        }

        if (data.getConnectionName().equals(StepManager.CON_JOB_SUCCESS)
          || data.getConnectionName().equals(StepManager.CON_JOB_FAILURE)) {
          finished.setPayloadElement(
            StepManager.CON_AUX_DATA_ENVIRONMENT_RESULTS, data
              .getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_RESULTS));
        }
        getStepManager().throughputFinished(finished);
        return;
      }

      Map<String, String> envVars =
        data.getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_VARIABLES);
      Map<String, Map<String, String>> propsToSet =
        data.getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_PROPERTIES);
      Map<String, LinkedHashSet<Data>> results =
        data.getPayloadElement(StepManager.CON_AUX_DATA_ENVIRONMENT_RESULTS);

      if (!m_useDynamic) {
        // just do the static thing
        getStepManager().throughputUpdateStart();
        try {
          ProcessBuilder builder = makeStaticProcess();
          runProcess(builder, envVars, propsToSet, results);
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
        getStepManager().throughputUpdateEnd();
      } else {
        if (data.getConnectionName().equals(StepManager.CON_INSTANCE)
          || data.getConnectionName().equals(StepManager.CON_ENVIRONMENT)) {
          Instance toProcess =
            (Instance) (data.getConnectionName().equals(
              StepManager.CON_INSTANCE) ? data.getPrimaryPayload() : data
              .getPayloadElement(StepManager.CON_AUX_DATA_INSTANCE));

          getStepManager().throughputUpdateStart();
          try {
            ProcessBuilder builder = makeDynamicProcess(toProcess);
            runProcess(builder, envVars, propsToSet, results);
          } catch (Exception ex) {
            throw new WekaException(ex);
          }
          getStepManager().throughputUpdateEnd();
        }
      }
    } else {
      getStepManager().processing();
      // handle dataset/trainingset/testset conns
      Instances toProcess = data.getPrimaryPayload();
      for (Instance inst : toProcess) {
        try {
          if (isStopRequested()) {
            getStepManager().interrupted();
            return;
          }
          ProcessBuilder builder = makeDynamicProcess(inst);
          runProcess(builder, null, null, null);
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
      }
      if (getStepManager().numOutgoingConnectionsOfType(
        StepManager.CON_INSTANCE) > 0) {
        // signal end of output stream
        Data finished = new Data(StepManager.CON_INSTANCE);
        getStepManager().throughputFinished(finished);
      }
      getStepManager().finished();
    }
  }

  /**
   * Check the incoming instance structure with respect to the attribute names
   * specified by the user for the command, args and working directory.
   * 
   * @param structure the incoming instance structure
   * @throws WekaException if a problem occurs
   */
  protected void checkStructure(Instances structure) throws WekaException {

    Attribute cmdAtt = structure.attribute(m_fieldCmd);
    if (cmdAtt == null) {
      throw new WekaException("Unable to find attribute (" + m_fieldCmd
        + ") holding command to execute in the incoming instance structure");
    }
    m_cmdFieldIndex = cmdAtt.index();

    if (m_fieldArgs != null && m_fieldArgs.length() > 0) {
      Attribute argsAtt = structure.attribute(m_fieldArgs);
      if (argsAtt == null) {
        throw new WekaException("Unable to find attribute (" + m_fieldArgs
          + ") holding command args in the incoming instance structure");
      }
      m_argsFieldIndex = argsAtt.index();
    }

    if (m_fieldWorkingDir != null && m_fieldWorkingDir.length() > 0) {
      Attribute workingAtt = structure.attribute(m_fieldWorkingDir);
      if (workingAtt == null) {
        throw new WekaException("Unable to find attribute ("
          + m_fieldWorkingDir
          + ") holding the working directory in the incoming instance stream");
      }
      m_workingDirFieldIndex = workingAtt.index();
    }
  }

  /**
   * Get the acceptable incoming connection types at this point in time
   *
   * @return a list of acceptable incoming connection types
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    // incoming success/failure can only be used to execute static commands
    // incoming instance and environment can be used to execute dynamic commands
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_INSTANCE, StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET,
        StepManager.CON_ENVIRONMENT, StepManager.CON_JOB_SUCCESS,
        StepManager.CON_JOB_FAILURE);
    }
    return null;
  }

  /**
   * Get a list of possible outgoing connection types at this point in time
   *
   * @return a list of possible outgoing connection types
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {

    // outgoing instance connections contain att vals with process exit status
    // and std out/err
    // job success/failure only indicate success/failure and pass on env
    // vars/props/results (results are not used/added to by this step)
    return Arrays.asList(StepManager.CON_INSTANCE, StepManager.CON_JOB_SUCCESS,
      StepManager.CON_JOB_FAILURE, StepManager.CON_TEXT);
  }

  /**
   * Get, if possible, the outgoing instance structure for the supplied incoming
   * connection type
   *
   * @param connectionName the name of the connection type to get the output
   *          structure for
   * @return an Instances object or null if outgoing structure is not applicable
   *         or cannot be determined
   * @throws WekaException if a problem occurs
   */
  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    if (getStepManager().numIncomingConnections() == 0
      || (!connectionName.equals(StepManager.CON_INSTANCE) && !connectionName
        .equals(StepManager.CON_ENVIRONMENT))) {
      return null;
    }

    // our output structure is the same as whatever kind of input we are getting
    return getStepManager().getIncomingStructureForConnectionType(
      connectionName);
  }

  /**
   * Get the name of the editor dialog for this step
   *
   * @return the name of the editor dialog for this step
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.ExecuteProcessStepEditorDialog";
  }

  /**
   * Copy an input stream to a writer
   *
   * @param input the input stream to copy from
   * @param out the writer to write to
   * @throws IOException if a problem occurs
   */
  protected static void copy(InputStream input, Writer out) throws IOException {
    InputStreamReader in = new InputStreamReader(input);
    int n = 0;
    char[] buffer = new char[1024 * 4];

    while ((n = in.read(buffer)) != -1) {
      out.write(buffer, 0, n);
    }
  }
}
