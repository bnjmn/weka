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
 *    RSessionImpl.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.rosuda.JRI.Mutex;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineCallbacks;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.REngineOutputInterface;

/**
 * Maintains a singleton object for managing R sessions. Because only one
 * instance of R is allowed by JRI, and R is single threaded, we have to make
 * sure that only one client has access to the R runtime at any one time.
 * <p>
 * 
 * This class should not be used directly. Instead, the weka.core.RSession class
 * should be used - especially if running inside a system where multiple class
 * loaders/plugins are used. This will ensure that the native libraries and
 * supporting JRI classes get loaded by the root class loader and are thus
 * available to all child class loaders.
 * <p>
 * 
 * For R to be available to Weka it is necessary for the user to:
 * <p>
 * 
 * <ol>
 * <li>Install the rJava (http://www.rforge.net/rJava/) package in R:
 * install.packages("rJava")</li>
 * <li>Make sure that R is on the PATH</li>
 * <li>Make sure that the R_HOME environment variable points to the directory
 * where the R executable is</li>
 * <li>Make sure that the R executable is in the PATH environment variable</li>
 * <li>Under Windows, R will prefer to install libraries into a personal library
 * directory under the user's home directory. This is for security reasons and
 * the user may not have write access for packages to be installed in the
 * system-wide R install directory. It is essential that, after manually
 * installing rJava, the user set the environment variable R_LIBS_USER to point
 * to this directory as Weka will ask R to install certain packages</li>
 * <li>The package will try loading the native library from a known location
 * under various OS's <br>
 * under Mac OSX this is in
 * /Library/Frameworks/R.framework/Resources/library/rJava/jri/ <br>
 * The user can override this by setting the property jri.native.library to
 * point to the native library when starting Weka</li>
 * </ol>
 * 
 * More information on configuring Java to access JRI under various OS can be
 * found at http://www.rforge.net/JRI/
 * 
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class RSessionImpl implements RSessionAPI, REngineCallbacks,
  REngineOutputInterface {

  /** The current session holder */
  private static Object s_sessionHolder;

  /** The session singleton */
  private static RSessionImpl s_sessionSingleton;// = new RSession();

  /** The REngine */
  private static REngine s_engine;

  /**
   * Maintains a list of listeners that are interested in graphics produced by
   * JavaGD
   */
  private static JavaGDNotifier s_javaGD = JavaGDOffscreenRenderer
    .getJavaGDNotifier();

  /**
   * Create a new REngine instance to use (if necessary)
   */
  protected void createREngine() {
    if (s_engine == null) {
      try {
        s_engine = REngine.engineForClass("org.rosuda.REngine.JRI.JRIEngine",
          new String[] { "--no-save" }, s_sessionSingleton, false);

        // install a default mirror to use for package downloads so that R
        // does not try and start a tcl/tk interface for mirror selection!
        System.err.println("Setting a default package mirror in R...");
        s_engine.parseAndEval("local({r <- getOption(\"repos\"); "
          + "r[\"CRAN\"] <- \"http://cran.stat.ucla.edu/\"; "
          + "options(repos=r)})");
      } catch (Exception ex) {
        // R engine not available for one reason or another
        System.err.println("Unable to establish R engine (" + ex.getMessage()
          + ")");
        s_sessionSingleton = null;
      }
    }
  }

  /**
   * Initialization routine. Attempts to load the JRI native library via
   * weka.core.JRINativeLoader using reflection. This method is itself called
   * through reflection by RSession via JRILoader. This somewhat convoluted
   * process ensures that the JRI native library is loaded in the root
   * classloader only (and is thus visible to all child classloaders).
   * Furthermore, JRILoader makes sure that the bytecode for various core JRI,
   * REngine and JavaGD classes gets injected into the root classloader. This
   * ensures that there is just one singleton JRIEngine/Rengine available to all
   * clients (potentially loaded in different classloaders), thus maintaining
   * the single-threaded access to the underlying R engine.
   */
  @Override
  public RSessionAPI init() throws Exception {
    if (s_sessionSingleton == null) {
      s_sessionSingleton = new RSessionImpl();
    }
    if (s_engine == null) {

      Class<?> nativeLoaderClass = Class.forName("weka.core.JRINativeLoader");
      if (nativeLoaderClass == null) {
        throw new Exception("Failed to load native loader class!");
      }

      // has the user pointed to a specific library file?
      String libraryLocation = System.getProperty("jri.native.library");

      // the java.library.path
      String systemLibraryPath = System.getProperty("java.library.path", "");

      // java.library.path overrides our default platform-specific locations
      if (libraryLocation == null
        && !systemLibraryPath.toLowerCase().contains("jri")) {
        String rLibsUser = System.getenv("R_LIBS_USER");
        String osType = System.getProperty("os.name");
        if (osType != null) {
          if (osType.contains("Windows")) {
            // Try our best (with respect to Windows 7)
            boolean is64bit = false;
            if (System.getProperty("os.name").contains("Windows")) {
              is64bit = (System.getenv("ProgramFiles(x86)") != null);
            } else {
              is64bit = (System.getProperty("os.arch").indexOf("64") != -1);
            }

            if (is64bit) {
              System.err.println("Detected Windows 64 bit OS");
            } else {
              System.err.println("Windows 32 bit OS");
            }

            if (rLibsUser != null) {
              // Following the R Wiki's guidelines of a personal package library
              /*
               * String temp = System.getProperty("user.home") + File.separator
               * + "Documents" + File.separator + "R" + File.separator +
               * "win-library";
               */
              System.err.println("Trying R_LIBS_USER (" + rLibsUser + ")");

              String jriDirS = null;
              rLibsUser = rLibsUser + File.separator + "rJava";
              File rJavaF = new File(rLibsUser);
              if (rJavaF.exists()) {
                System.err.println("Found rJava installed in "
                  + rJavaF.getPath());
                jriDirS = rJavaF.getPath() + File.separator + "jri";
                if (is64bit) {
                  jriDirS += File.separator + "x64" + File.separator
                    + "jri.dll";
                } else {
                  jriDirS += File.separator + "i386" + File.separator
                    + "jri.dll";
                }
              }

              if (jriDirS != null) {
                libraryLocation = jriDirS;
              }
            }
          } else if (osType.contains("Mac OS X")) {
            System.err
              .println("Trying systemwide default location for R under Mac OS...");
            libraryLocation = "/Library/Frameworks/R.framework/Resources/library/rJava/jri/libjri.jnilib";
            File rJavaF = new File(libraryLocation);
            if (!rJavaF.exists() && rLibsUser != null) {
              System.err.println("Trying R_LIBS_USER (" + rLibsUser + ")");
              rLibsUser = rLibsUser + File.separator + "rJava";
              rJavaF = new File(rLibsUser);
              if (rJavaF.exists()) {
                System.err.println("Found rJava installed in "
                  + rJavaF.getPath());
                libraryLocation = rJavaF.getPath() + File.separator + "jri"
                  + File.separator + "libjri.jnilib";
              }
            }
          } else if (osType.contains("Linux")) {
            System.err.println("Trying R_LIBS_USER (" + rLibsUser
              + ") under Linux");
            rLibsUser = rLibsUser + File.separator + "rJava";
            File rJavaF = new File(rLibsUser);
            if (rJavaF.exists()) {
              System.err
                .println("Found rJava installed in " + rJavaF.getPath());
              libraryLocation = rJavaF.getPath() + File.separator + "jri"
                + File.separator + "libjri.so";
            }
          } else if (osType.contains("Unix")) {
            // TODO
          }
        }
      }

      Method loadMethod = null;

      try {
        if (libraryLocation != null) {
          loadMethod = nativeLoaderClass.getDeclaredMethod("loadLibrary",
            new Class[] { String.class });
          loadMethod.invoke(null, libraryLocation);
        } else {
          // hope that the user has set the java.library.path to point to the
          // native library
          loadMethod = nativeLoaderClass.getDeclaredMethod("loadLibrary",
            new Class[] {});
          loadMethod.invoke(null, new Object[] {});
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        return s_sessionSingleton;
      }

      // System.err.println("RSessionImp - classloader " +
      // RSessionImpl.class.getClassLoader());
      System.err.println("Getting REngine....");
      s_engine = REngine.getLastEngine();
    }

    createREngine();

    return s_sessionSingleton;
  }

  /**
   * Get the reference to the singleton. Used by proxy RSession's to obtain a
   * reference.
   * 
   * @return the singleton
   */
  public static RSessionAPI getSessionSingleton() {
    return s_sessionSingleton;
  }

  /** Log object to send R error messages etc. to */
  protected RLoggerAPI m_logger;

  /** For locking */
  protected Mutex m_mutex = new Mutex();

  /**
   * Used to retain current output of the R console. Clients should call
   * clearConsoleBuffer() before executing R commands and then retrieve the
   * console buffer afterwards.
   */
  protected List<String> m_consoleBuffer = new ArrayList<String>();

  private static void checkSessionHolder(Object requester)
    throws RSessionException {
    if (s_sessionHolder != requester) {
      throw new RSessionException("Can't assign - you don't hold the session");
    }
  }

  /**
   * Set the logger to use
   * 
   * @param requester the object registering the log (must be the current
   *          session holder)
   * @param log the log to use
   * @throws RSessionException if the requester is not the current session
   *           holder
   */
  @Override
  public void setLog(Object requester, RLoggerAPI log) throws RSessionException {
    checkSessionHolder(requester);

    m_logger = log;
  }

  /**
   * Clear the contents of the R console buffer
   * 
   * @param requester the requesting object (must be the session holder)
   * @throws RSessionException if the requester is not the session holder
   */
  @Override
  public void clearConsoleBuffer(Object requester) throws RSessionException {
    checkSessionHolder(requester);

    m_consoleBuffer.clear();
  }

  /**
   * Get the contents of the R console buffer
   * 
   * @param requester the requesting object (must be the session holder)
   * @return the contents of the console buffer as a string
   * @throws RSessionException if the requester is not the session holder
   */
  @Override
  public String getConsoleBuffer(Object requester) throws RSessionException {
    checkSessionHolder(requester);

    StringBuilder buff = new StringBuilder();
    for (String s : m_consoleBuffer) {
      buff.append(s);
    }

    return buff.toString();
  }

  private synchronized void block(boolean tf) {
    if (tf) {
      try {
        wait();
      } catch (InterruptedException ex) {
      }
    } else {
      notifyAll();
    }
  }

  @Override
  public synchronized RSessionAPI getSession(Object requester) throws Exception {
    if (s_engine == null) {
      // try and re-establish (in case it has been shutdown by another user)
      createREngine();

      if (s_engine == null) {
        throw new Exception("R engine not available!!");
      }
    }

    if (s_sessionHolder == requester) {
      return this;
    }

    m_mutex.safeLock();
    s_sessionHolder = requester;

    /*
     * if (s_sessionHolder == null) { //init(); s_sessionHolder = requester;
     * //System.out.println("Grabbing session"); return this; } else {
     * //System.out.println("Waiting for session..."); block(true); //
     * System.out.println("Notified."); if (s_sessionHolder == null) {
     * s_sessionHolder = requester; return this; } else { //
     * System.out.println("Session holder not null!"); } }
     */

    return this;
  }

  @Override
  public void dropSession(Object requester) {
    if (requester == s_sessionHolder) {

      // if requester is interested in plotting then get the
      // javagd renderer to pass on any image
      if (s_javaGD != null) {
        if (requester instanceof JavaGDListener
          && JavaGDOffscreenRenderer.javaGDAvailable()) {
          s_javaGD.notifyListeners((JavaGDListener) requester);
        } else {
          // make sure that the perspective gets the image
          s_javaGD.notifyListeners();
        }
      }

      s_sessionHolder = null;
      m_logger = null;
      m_mutex.unlock();
      // System.out.println("Releasing session");
      // block(false);
    }
  }

  /**
   * Checks if R is available
   * 
   * @return true if R is available
   */
  public static boolean rAvailable() {
    return (s_engine != null);
  }

  /**
   * Acquire the session for the requester
   * 
   * @param requester the object requesting the session
   * @return the session singleton
   * @throws Exception if the session is currently held by another client
   */
  public static RSessionAPI acquireSession(Object requester) throws Exception {
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
   * Convenience method for getting R to load a named library.
   * 
   * @param requester the requesting object
   * @param libraryName the name of the library to load
   * @return true if the library was loaded successfully
   * 
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R end
   * @throws REXPMismatchException if a problem occurs on the R end
   */
  @Override
  public boolean loadLibrary(Object requester, String libraryName)
    throws RSessionException, REngineException, REXPMismatchException {

    checkSessionHolder(requester);

    REXP result = parseAndEval(requester, "library(" + libraryName + ")");
    if (result.isNull()) {
      return false;
    }

    return true;
  }

  /**
   * Convenience method for getting R to install a library.
   * 
   * @param requester the requesting object
   * @param libraryName the name of the library to install
   * @return true if the library was installed successfully
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R end
   * @throws REXPMismatchException if a problem occurs on the R end
   */
  @Override
  public boolean installLibrary(Object requester, String libraryName)
    throws RSessionException, REngineException, REXPMismatchException {

    checkSessionHolder(requester);

    REXP result = parseAndEval(requester, "install.packages(\"" + libraryName
      + "\")");
    if (result.isNull()) {
      return false;
    }

    return true;
  }

  /**
   * Convenience method for getting R to install a library.
   * 
   * @param requester the requesting object
   * @param libraryName the name of the library to install
   * @param repos the repository(s) to use
   * @return true if the library was installed successfully
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R end
   * @throws REXPMismatchException if a problem occurs on the R end
   */
  @Override
  public boolean installLibrary(Object requester, String libraryName,
    String repos) throws RSessionException, REngineException,
    REXPMismatchException {

    checkSessionHolder(requester);

    REXP result = parseAndEval(requester, "install.packages(\"" + libraryName
      + "\", " + "repos = \"" + repos + "\")");
    if (result.isNull()) {
      return false;
    }

    return true;
  }

  /**
   * Check if a named variable is set in the R environment
   * 
   * @param requester the requesting object
   * @param var the name of the variable to check
   * @return true if the variable is set in the R environment
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R side
   * @throws REXPMismatchException if a problem occurs on the R side
   */
  @Override
  public boolean isVariableSet(Object requester, String var)
    throws RSessionException, REngineException, REXPMismatchException {

    checkSessionHolder(requester);

    REXP result = parseAndEval(requester, "\"" + var + "\" %in% ls()");
    if (result == null) {
      return false;
    }

    if (result.isLogical()) {
      return ((REXPLogical) result).isTRUE()[0];
    }

    return false;
  }

  /**
   * Pushes an REXP back into R and, if successful, returns a reference REXP
   * that points to the pushed object in R. The reference can be used in an
   * assign() to assign a variable to the object.
   * 
   * @param requester the requesting object
   * @param source the source object to push into R
   * @return a reference to the object in R
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R end
   * @throws REXPMismatchException if a problem occurs on the R end
   */
  @Override
  public REXP createReference(Object requester, REXP source)
    throws RSessionException, REngineException, REXPMismatchException {
    checkSessionHolder(requester);

    return s_engine.createReference(source);
  }

  /**
   * Get a named object from R. If the object is a reference (pointer) then this
   * will return a fully dereferenced REXP.
   * 
   * @param requester the requesting object
   * @param var the name of the variable in R to get
   * @return the fully dereferenced R object corresponding to var
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R end
   * @throws REXPMismatchException if a problem occurs on the R end
   */
  @Override
  public REXP get(Object requester, String var) throws RSessionException,
    REngineException, REXPMismatchException {
    checkSessionHolder(requester);

    REXP result = s_engine.get(var, null, true);

    return result;
  }

  /**
   * Wraps REngine.assign
   * 
   * @param requester the requesting object
   * @param var the name of the variable to assign to
   * @param val an REXP object
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R end
   */
  @Override
  public void assign(Object requester, String var, REXP val)
    throws RSessionException, REngineException, REXPMismatchException {
    checkSessionHolder(requester);

    s_engine.assign(var, val);
  }

  /**
   * Wraps REngine.assign
   * 
   * @param requester the requesting object
   * @param var the name of the variable to assign to
   * @param val a byte[] value
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R end
   */
  @Override
  public void assign(Object requester, String var, byte[] val)
    throws RSessionException, REngineException {
    checkSessionHolder(requester);

    s_engine.assign(var, val);
  }

  /**
   * Wraps REngine.assign
   * 
   * @param requester the requesting object
   * @param var the name of the variable to assign to
   * @param val a double[] value
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R end
   */
  @Override
  public void assign(Object requester, String var, double[] val)
    throws RSessionException, REngineException {
    checkSessionHolder(requester);

    s_engine.assign(var, val);
  }

  /**
   * Wraps REngine.assign
   * 
   * @param requester the requesting object
   * @param var the name of the variable to assign to
   * @param val a int[] value
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R end
   */
  @Override
  public void assign(Object requester, String var, int[] val)
    throws RSessionException, REngineException {
    checkSessionHolder(requester);

    s_engine.assign(var, val);
  }

  /**
   * Wraps REngine.assign
   * 
   * @param requester the requesting object
   * @param var the name of the variable to assign to
   * @param val a String[] value
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R end
   */
  @Override
  public void assign(Object requester, String var, String[] val)
    throws RSessionException, REngineException {
    checkSessionHolder(requester);

    s_engine.assign(var, val);
  }

  /**
   * Wraps REngine.parseAndEval. Evaluates one or more R commands as a script.
   * 
   * @param requester the requesting object
   * @param cmd the command(s)
   * @return the REXP object containing the results
   * @throws RSessionException if the requester is not the current session
   *           holder
   * @throws REngineException if a problem occurs on the R end
   * @throws REXPMismatchException if a problem occurs on the R end
   */
  @Override
  public REXP parseAndEval(Object requester, String cmd)
    throws RSessionException, REngineException, REXPMismatchException {
    checkSessionHolder(requester);

    return s_engine.parseAndEval(cmd);
  }

  // TODO the rest of assign()s

  /**
   * called by R to flush (display) any pending console output.
   * 
   * @param eng calling engine
   */
  @Override
  public void RFlushConsole(REngine eng) {
    // nothing to do
    // System.out.println("Flush");
  }

  /**
   * called when R wants to show a warning/error message box (not
   * console-related).
   * 
   * @param eng calling engine
   * @param text text to display in the message
   */
  @Override
  public void RShowMessage(REngine eng, String text) {
    String t = "ERROR. See log for details";
    if (m_logger != null) {
      m_logger.statusMessage(t);
      m_logger.logMessage(text);
    }
  }

  /**
   * called when R prints output to the console.
   * 
   * @param eng calling engine
   * @param text text to display in the console
   * @param oType output type (0=regular, 1=error/warning)
   */
  @Override
  public void RWriteConsole(REngine eng, String text, int oType) {
    if (oType == 0) {
      m_consoleBuffer.add(text);
    }
    String t = "";
    if (oType == 1) {
      t += "WARNING: ";
      t += text;
      if (m_logger != null) {
        m_logger.statusMessage("WARNING: check log");
        m_logger.logMessage(t);
      }
    }
  }

  @Override
  public void close() {
    if (s_engine != null) {
      boolean result = s_engine.close();
      if (result) {
        s_engine = null;
      }
    }
  }
}
