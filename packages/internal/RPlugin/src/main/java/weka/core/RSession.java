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
 *    RSession.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.lang.reflect.Method;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 * Maintains a proxy singleton interface to the actual RSession implementation.
 * We use this rather convoluted process because of the limitation that native
 * libraries may be loaded only once (by one class loader) combined with the
 * fact that the R runtime is single threaded. This means that the JRI Rengine
 * interface is also a singleton and all clients must use it. Because of this,
 * there would be problems in a multi class loader situation (like an app
 * server) if more than one classloader/plugin loads the JRI interface. For this
 * reason we have to ensure that the root class loader not only loads the native
 * library (so that it is visible to all child class loaders) but also loads
 * some key classes from this package plus all the JRI-related classes.
 * Reflection-based byte code injection is used to make this happen (see the
 * JRILoader class).
 * <p>
 * 
 * Many thanks to the snappy-java guys for their clear and well documented code
 * demonstrating this approach (hacky though it may be).
 * http://code.google.com/p/snappy-java/.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class RSession {

  /**
   * the single proxy session instance that we maintain in order to hand to
   * clients
   */
  private static RSession s_sessionProxySingleton;

  /** the actual implementation delegate */
  private static Object s_sessionImpl;

  static {
    try {
      if (s_sessionProxySingleton == null) {
        s_sessionProxySingleton = new RSession();
      }

      // inject byte code into the root class loader and make sure
      // that the native libraries and all JRI key classes are loaded
      // by the root class loader
      s_sessionImpl = JRILoader.load();
      if (s_sessionImpl != null) {
        JavaGDOffscreenRenderer.javaGDAvailable();
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      s_sessionProxySingleton = null;
      s_sessionImpl = null;
    }
  }

  /**
   * Returns true if the R environment has been loaded and is available to
   * clients
   * 
   * @return true if R is available
   */
  public static boolean rAvailable() {
    if (s_sessionImpl == null) {
      return false;
    }

    try {
      Method m = s_sessionImpl.getClass().getDeclaredMethod("rAvailable",
        new Class[] {});

      Object result = m.invoke(null, new Object[] {});
      if (result == null) {
        return false;
      } else if (!(result instanceof Boolean)) {
        return false;
      } else {
        return ((Boolean) result).booleanValue();
      }
    } catch (Exception ex) {
      return false;
    }
  }

  /**
   * Acquire the session for the requester
   * 
   * @param requester the object requesting the session
   * @return the session singleton
   * @throws Exception if the session is currently held by another client
   */
  public static RSession acquireSession(Object requester) throws Exception {
    // just call so that we wait until R is available
    ((RSessionAPI) s_sessionImpl).getSession(requester);

    return s_sessionProxySingleton;
  }

  /**
   * Release the session so that other clients can obtain it. This method does
   * nothing if the requester is not the current session holder
   * 
   * @param requester the session holder
   */
  public static void releaseSession(Object requester) {
    ((RSessionAPI) s_sessionImpl).dropSession(requester);
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
  public void setLog(Object requester, RLoggerAPI log) throws RSessionException {
    ((RSessionAPI) s_sessionImpl).setLog(requester, log);
  }

  /**
   * Clear the contents of the R console buffer
   * 
   * @param requester the requesting object (must be the session holder)
   * @throws RSessionException if the requester is not the session holder
   */
  public void clearConsoleBuffer(Object requester) throws RSessionException {
    ((RSessionAPI) s_sessionImpl).clearConsoleBuffer(requester);
  }

  /**
   * Get the contents of the R console buffer
   * 
   * @param requester the requesting object (must be the session holder)
   * @return the contents of the console buffer as a string
   * @throws RSessionException if the requester is not the session holder
   */
  public String getConsoleBuffer(Object requester) throws RSessionException {
    return ((RSessionAPI) s_sessionImpl).getConsoleBuffer(requester);
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
  public boolean loadLibrary(Object requester, String libraryName)
    throws RSessionException, REngineException, REXPMismatchException {
    return ((RSessionAPI) s_sessionImpl).loadLibrary(requester, libraryName);
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
  public boolean installLibrary(Object requester, String libraryName)
    throws RSessionException, REngineException, REXPMismatchException {
    return ((RSessionAPI) s_sessionImpl).installLibrary(requester, libraryName);
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
  public boolean installLibrary(Object requester, String libraryName,
    String repos) throws RSessionException, REngineException,
    REXPMismatchException {
    return ((RSessionAPI) s_sessionImpl).installLibrary(requester, libraryName,
      repos);
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
  public boolean isVariableSet(Object requester, String var)
    throws RSessionException, REngineException, REXPMismatchException {
    return ((RSessionAPI) s_sessionImpl).isVariableSet(requester, var);
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
  public REXP createReference(Object requester, REXP source)
    throws RSessionException, REngineException, REXPMismatchException {
    return ((RSessionAPI) s_sessionImpl).createReference(requester, source);
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
  public REXP get(Object requester, String var) throws RSessionException,
    REngineException, REXPMismatchException {
    return ((RSessionAPI) s_sessionImpl).get(requester, var);
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
  public void assign(Object requester, String var, REXP val)
    throws RSessionException, REngineException, REXPMismatchException {
    ((RSessionAPI) s_sessionImpl).assign(requester, var, val);
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
  public void assign(Object requester, String var, byte[] val)
    throws RSessionException, REngineException {
    ((RSessionAPI) s_sessionImpl).assign(requester, var, val);
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
  public void assign(Object requester, String var, double[] val)
    throws RSessionException, REngineException {
    ((RSessionAPI) s_sessionImpl).assign(requester, var, val);
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
  public void assign(Object requester, String var, int[] val)
    throws RSessionException, REngineException {
    ((RSessionAPI) s_sessionImpl).assign(requester, var, val);
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
  public void assign(Object requester, String var, String[] val)
    throws RSessionException, REngineException {
    ((RSessionAPI) s_sessionImpl).assign(requester, var, val);
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
  public REXP parseAndEval(Object requester, String cmd)
    throws RSessionException, REngineException, REXPMismatchException {
    return ((RSessionAPI) s_sessionImpl).parseAndEval(requester, cmd);
  }
}
