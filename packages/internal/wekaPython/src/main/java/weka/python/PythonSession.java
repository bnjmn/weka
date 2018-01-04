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
 *    PythonSession.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.python;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.apache.commons.io.IOUtils;

import weka.core.Instances;
import weka.core.WekaException;
import weka.core.WekaPackageManager;
import weka.gui.Logger;

/**
 * Class that manages interaction with the python micro server. Launches the
 * server and shuts it down on VM exit.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class PythonSession {

  public static enum PythonVariableType {
    DataFrame, Image, String, Unknown;
  }

  /** The command used to start python */
  private String m_pythonCommand;

  /** The session singleton */
  private static PythonSession s_sessionSingleton;

  /** the current session holder */
  private static Object s_sessionHolder;

  /** The results of the python check script */
  private static String s_pythonEnvCheckResults = "";

  /** For locking */
  protected SessionMutex m_mutex = new SessionMutex();

  /** Server socket */
  protected ServerSocket m_serverSocket;

  /** Local socket for comms with the python server */
  protected Socket m_localSocket;

  /** The process executing the server */
  protected Process m_serverProcess;

  /** True when the server has been shutdown */
  protected boolean m_shutdown;

  /** A shutdown hook for stopping the server */
  protected Thread m_shutdownHook;

  /** PID of the running python server */
  protected int m_pythonPID = -1;

  /** True to output debugging info */
  protected boolean m_debug;

  /** Logger to use (if any) */
  protected Logger m_log;

  /**
   * Acquire the session for the requester
   *
   * @param requester the object requesting the session
   * @return the session singleton
   * @throws WekaException if python is not available
   */
  public static PythonSession acquireSession(Object requester)
    throws WekaException {
    return s_sessionSingleton.getSession(requester);
  }

  /**
   * Release the session so that other clients can obtain it. This method does
   * nothing if the requester is not the current session holder
   *
   * @param requester the session holder
   */
  public static void releaseSession(Object requester) {
    s_sessionSingleton.dropSession(requester);
  }

  /**
   * Returns true if the python environment/server is available
   *
   * @return true if the python environment/server is available
   */
  public static boolean pythonAvailable() {
    return s_sessionSingleton != null;
  }

  /**
   * Private constructor
   *
   * @param pythonCommand the command used to start python
   * @param debug true for debugging output
   * @throws IOException if a problem occurs
   */
  private PythonSession(String pythonCommand, boolean debug) throws IOException {
    m_debug = debug;
    m_pythonCommand = pythonCommand;
    s_sessionSingleton = null;
    s_pythonEnvCheckResults = "";
    String tester =
      WekaPackageManager.PACKAGES_DIR.getAbsolutePath() + File.separator
        + "wekaPython" + File.separator + "resources" + File.separator + "py"
        + File.separator + "pyCheck.py";
    ProcessBuilder builder = new ProcessBuilder(pythonCommand, tester);
    Process pyProcess = builder.start();
    StringWriter writer = new StringWriter();
    IOUtils.copy(pyProcess.getInputStream(), writer);
    s_pythonEnvCheckResults = writer.toString();
    m_shutdown = false;

    // launch the server socket and python server
    if (s_pythonEnvCheckResults.length() < 5) {
      launchServer(true);
      s_sessionSingleton = this;
    }
  }

  /**
   * Gets the access to python for a requester. Handles locking.
   *
   * @param requester the requesting object
   * @return the session
   * @throws WekaException if python is not available
   */
  private synchronized PythonSession getSession(Object requester)
    throws WekaException {
    if (s_sessionSingleton == null) {
      throw new WekaException("Python not available!");
    }

    if (s_sessionHolder == requester) {
      return this;
    }

    m_mutex.safeLock();
    s_sessionHolder = requester;
    return this;
  }

  /**
   * Release the session for a requester
   *
   * @param requester the requesting object
   */
  private void dropSession(Object requester) {
    if (requester == s_sessionHolder) {
      s_sessionHolder = null;
      m_mutex.unlock();
    }
  }

  /**
   * Launches the python server. Performs some basic requirements checks for the
   * python environment - e.g. python needs to have numpy, pandas and sklearn
   * installed.
   *
   * @param startPython true if the server is to actually be started. False is
   *          really just for debugging/development where the server can be
   *          manually started in a separate terminal
   * @throws IOException if a problem occurs
   */
  private void launchServer(boolean startPython) throws IOException {
    if (m_debug) {
      System.err.println("Launching server socket...");
    }
    m_serverSocket = new ServerSocket(0);
    m_serverSocket.setSoTimeout(10000);
    int localPort = m_serverSocket.getLocalPort();
    if (m_debug) {
      System.err.println("Local port: " + localPort);
    }
    Thread acceptThread = new Thread() {
      @Override
      public void run() {
        try {
          m_localSocket = m_serverSocket.accept();
        } catch (IOException e) {
          m_localSocket = null;
        }
      }
    };
    acceptThread.start();

    if (startPython) {
      String serverScript =
        WekaPackageManager.PACKAGES_DIR.getAbsolutePath() + File.separator
          + "wekaPython" + File.separator + "resources" + File.separator + "py"
          + File.separator + "pyServer.py";
      ProcessBuilder processBuilder =
        new ProcessBuilder(m_pythonCommand, serverScript, "" + localPort,
          m_debug ? "debug" : "");
      m_serverProcess = processBuilder.start();
    }
    try {
      acceptThread.join();
    } catch (InterruptedException e) {
    }

    if (m_localSocket == null) {
      shutdown();
      throw new IOException("Was unable to start python server");
    } else {
      m_pythonPID =
        ServerUtils.receiveServerPIDAck(m_localSocket.getInputStream());

      m_shutdownHook = new Thread() {
        @Override
        public void run() {
          shutdown();
        }
      };
      Runtime.getRuntime().addShutdownHook(m_shutdownHook);
    }
  }

  /**
   * Set a log
   *
   * @param log the log to use
   */
  public void setLog(Logger log) {
    m_log = log;
  }

  /**
   * Get the type of a variable in python
   * 
   * @param varName the name of the variable to get the type for
   * @param debug true for debugging output
   * @return the type of the variable. Known types, for which we can do useful
   *         things with in the Weka environment, are pandas data frames (can be
   *         converted to instances), pyplot figure/images (retrieve as png) and
   *         textual data. Any variable of type unknown should be able to be
   *         retrieved in string form.
   * @throws WekaException if a problem occurs
   */
  public PythonVariableType
    getPythonVariableType(String varName, boolean debug) throws WekaException {

    try {
      return ServerUtils.getPythonVariableType(varName,
        m_localSocket.getOutputStream(), m_localSocket.getInputStream(), m_log,
        debug);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Transfer Weka instances into python as a named pandas data frame
   *
   * @param instances the instances to transfer
   * @param pythonFrameName the name of the data frame to use in python
   * @param debug true for debugging output
   * @throws WekaException if a problem occurs
   */
  public void instancesToPython(Instances instances, String pythonFrameName,
    boolean debug) throws WekaException {
    try {
      ServerUtils.sendInstances(instances, pythonFrameName,
        m_localSocket.getOutputStream(), m_localSocket.getInputStream(), m_log,
        debug);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Transfer Weka instances into python as a pandas data frame and then extract
   * out numpy arrays of input and target features/columns. These arrays are
   * named X and Y respectively in python. If there is no class set in the
   * instances then only an X array is extracted.
   *
   * @param instances the instances to transfer
   * @param pythonFrameName the name of the pandas data frame to use in python
   * @param debug true for debugging output
   * @throws WekaException if a problem occurs
   */
  public void instancesToPythonAsScikitLearn(Instances instances,
    String pythonFrameName, boolean debug) throws WekaException {
    try {
      ServerUtils.sendInstancesScikitLearn(instances, pythonFrameName,
        m_localSocket.getOutputStream(), m_localSocket.getInputStream(), m_log,
        debug);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Retrieve a pandas data frame from Python and convert it to a set of
   * instances. The resulting set of instances will not have a class index set.
   *
   * @param frameName the name of the pandas data frame to extract and convert
   *          to instances
   * @param debug true for debugging output
   * @return an Instances object
   * @throws WekaException if the named data frame does not exist in python or
   *           is not a pandas data frame
   */
  public Instances getDataFrameAsInstances(String frameName, boolean debug)
    throws WekaException {
    try {
      return ServerUtils.receiveInstances(frameName,
        m_localSocket.getOutputStream(), m_localSocket.getInputStream(), m_log,
        debug);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Execute an arbitrary script in python
   *
   * @param pyScript the script to execute
   * @param debug true for debugging output
   * @return a List of strings - index 0 contains std out from the script and
   *         index 1 contains std err
   * @throws WekaException if a problem occurs
   */
  public List<String> executeScript(String pyScript, boolean debug)
    throws WekaException {
    try {
      return ServerUtils.executeUserScript(pyScript,
        m_localSocket.getOutputStream(), m_localSocket.getInputStream(), m_log,
        debug);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Check if a named variable is set/exists in the python environment
   *
   * @param varName the name of the variable to check
   * @param debug true for debugging output
   * @return true if the variable is set in python
   * @throws WekaException if a problem occurs
   */
  public boolean checkIfPythonVariableIsSet(String varName, boolean debug)
    throws WekaException {
    try {
      return ServerUtils.checkIfPythonVariableIsSet(varName,
        m_localSocket.getOutputStream(), m_localSocket.getInputStream(), m_log,
        debug);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Attempt to retrieve the value of a variable in python using serialization
   * to Json. If successful, then the resulting Object is either a Map or List
   * containing more Maps and Lists that represent the Json structure of the
   * serialized variable
   *
   * @param varName the name of the variable to retrieve
   * @param debug true for debugging output
   * @return a Map/List based structure
   * @throws WekaException if a problem occurs
   */
  public Object getVariableValueFromPythonAsJson(String varName, boolean debug)
    throws WekaException {
    try {
      return ServerUtils.receiveJsonVariableValue(varName,
        m_localSocket.getOutputStream(), m_localSocket.getInputStream(), m_log,
        debug);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Attempt to retrieve the value of a variable in python using pickle
   * serialization. If successful, then the result is a string containing the
   * pickled object.
   *
   * @param varName the name of the variable to retrieve
   * @param debug true for debugging output
   * @return a string containing the pickled variable value
   * @throws WekaException if a problem occurs
   */
  public String getVariableValueFromPythonAsPickledObject(String varName,
    boolean debug) throws WekaException {
    try {
      return ServerUtils.receivePickledVariableValue(varName,
        m_localSocket.getOutputStream(), m_localSocket.getInputStream(), false,
        m_log, debug);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Attempt to retrieve the value of a variable in python as a plain string
   * (i.e. executes a 'str(varName)' in python).
   *
   * @param varName the name of the variable to retrieve
   * @param debug true for debugging output
   * @return the value of the variable as a plain string
   * @throws WekaException if a problem occurs
   */
  public String getVariableValueFromPythonAsPlainString(String varName,
    boolean debug) throws WekaException {
    try {
      return ServerUtils.receivePickledVariableValue(varName,
        m_localSocket.getOutputStream(), m_localSocket.getInputStream(), true,
        m_log, debug);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Get a list of variables that are set in python. Returns a list of two
   * element string arrays. Each entry in the list is a variable. The first
   * element of the array is the name of the variable and the second is its type
   * in python.
   *
   * @param debug true if debugging info is to be output
   * @return a list of variables set in python
   * @throws WekaException if a problem occurs
   */
  public List<String[]> getVariableListFromPython(boolean debug)
    throws WekaException {
    try {
      return ServerUtils.receiveVariableList(m_localSocket.getOutputStream(),
        m_localSocket.getInputStream(), m_log, debug);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Push a pickled python variable value back into python. Deserializes the
   * value in python.
   *
   * @param varName the name of the variable in python that will hold the
   *          deserialized value
   * @param varValue the pickled string value of the variable
   * @param debug true for debugging output
   * @throws WekaException if a problem occurs
   */
  public void setPythonPickledVariableValue(String varName, String varValue,
    boolean debug) throws WekaException {
    try {
      ServerUtils.sendPickledVariableValue(varName, varValue,
        m_localSocket.getOutputStream(), m_localSocket.getInputStream(), m_log,
        debug);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Grab the contents of the debug buffer from the python server. The server
   * redirects both sys out and sys err to StringIO objects. If debug has been
   * specified, then server debugging output will have been collected in these
   * buffers. Note that the buffers will potentially also contain output from
   * the execution of arbitrary scripts too. Calling this method also resets the
   * buffers.
   *
   * @param debug true for debugging output (from the execution of this specific
   *          command)
   * @return the contents of the sys out and sys err streams. Element 0 in the
   *         list contains sys out and element 1 contains sys err
   * @throws WekaException if a problem occurs
   */
  public List<String> getPythonDebugBuffer(boolean debug) throws WekaException {
    try {
      return ServerUtils.receiveDebugBuffer(m_localSocket.getOutputStream(),
        m_localSocket.getInputStream(), m_log, debug);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Retrieve an image from python. Assumes that the image in python is stored
   * as a matplotlib.figure.Figure object. Returns a BufferedImage containing
   * the image.
   *
   * @param varName the name of the variable in python that contains the image
   * @param debug true to output debugging info
   * @return a BufferedImage
   * @throws WekaException if the variable doesn't exist, doesn't contain a
   *           Figure object or there is a comms error.
   */
  public BufferedImage getImageFromPython(String varName, boolean debug)
    throws WekaException {
    try {
      return ServerUtils.getPNGImageFromPython(varName,
        m_localSocket.getOutputStream(), m_localSocket.getInputStream(), m_log,
        debug);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Shutdown the python server
   */
  private void shutdown() {
    if (!m_shutdown) {
      try {
        m_shutdown = true;
        if (m_localSocket != null) {
          if (m_debug) {
            System.err.println("Sending shutdown command...");
          }
          if (m_debug) {
            List<String> outAndErr =
              ServerUtils.receiveDebugBuffer(m_localSocket.getOutputStream(),
                m_localSocket.getInputStream(), m_log, m_debug);
            if (outAndErr.get(0).length() > 0) {
              System.err.println("Python debug std out:\n" + outAndErr.get(0)
                + "\n");
            }
            if (outAndErr.get(1).length() > 0) {
              System.err.println("Python debug std err:\n" + outAndErr.get(1)
                + "\n");
            }
          }
          ServerUtils.sendServerShutdown(m_localSocket.getOutputStream());
          m_localSocket.close();
          if (m_serverProcess != null) {
            m_serverProcess.destroy();
          }
        }

        if (m_serverSocket != null) {
          m_serverSocket.close();
        }
        s_sessionSingleton = null;
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Initialize the session. This needs to be called exactly once in order to
   * run checks and launch the server. Creates a session singleton.
   *
   * @param pythonCommand the python command
   * @param debug true for debugging output
   * @return true if the server launched successfully
   * @throws WekaException if there was a problem - missing packages in python,
   *           or python could not be started for some reason
   */
  public static boolean initSession(String pythonCommand, boolean debug)
    throws WekaException {
    if (s_sessionSingleton != null) {
      throw new WekaException("The python environment is already available!");
    }

    try {
      new PythonSession(pythonCommand, debug);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }

    return s_pythonEnvCheckResults.length() < 5;
  }

  /**
   * Gets the result of running the checks in python
   *
   * @return a string containing the possible errors
   */
  public static String getPythonEnvCheckResults() {
    return s_pythonEnvCheckResults;
  }

  /**
   * Some quick tests...
   *
   * @param args
   */
  public static void main(String[] args) {
    try {
      if (!PythonSession.initSession("python", true)) {
        System.err.println("Initialization failed!");
        System.exit(1);
      }

      String temp = "";
      PythonSession session = PythonSession.acquireSession(temp);
      // String script =
      // "import matplotlib.pyplot as plt\nfig, ax = plt.subplots( nrows=1, ncols=1 )\n"
      // + "ax.plot([0,1,2], [10,20,3])\n";
      // String script = "my_var = 'hello'\n";
      String script =
        "from sklearn import datasets\nfrom pandas import DataFrame\ndiabetes = "
          + "datasets.load_diabetes()\ndd = DataFrame(diabetes.data)\n";
      session.executeScript(script, true);

      script = "def foo():\n\treturn 100\n\nx = foo()\n";
      session.executeScript(script, true);

      // BufferedImage img = session.getImageFromPython("fig", true);
      List<String[]> vars = session.getVariableListFromPython(true);
      for (String[] v : vars) {
        System.err.println(v[0] + ":" + v[1]);
      }
      
      Object result = session.getVariableValueFromPythonAsJson("x", true);
      System.err.println("Value of x: " + result.toString());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
