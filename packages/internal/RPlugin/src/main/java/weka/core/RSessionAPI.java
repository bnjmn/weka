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
 *    RSessionAPI.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 * Interface to an RSession.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface RSessionAPI {

  RSessionAPI init() throws Exception;

  RSessionAPI getSession(Object requester) throws Exception;

  void dropSession(Object requester);

  void setLog(Object requester, RLoggerAPI log) throws RSessionException;

  void clearConsoleBuffer(Object requester) throws RSessionException;

  String getConsoleBuffer(Object requester) throws RSessionException;

  boolean loadLibrary(Object requester, String libraryName)
    throws RSessionException, REngineException, REXPMismatchException;

  boolean installLibrary(Object requester, String libraryName)
    throws RSessionException, REngineException, REXPMismatchException;

  boolean installLibrary(Object requester, String libraryName, String repos)
    throws RSessionException, REngineException, REXPMismatchException;

  boolean isVariableSet(Object requester, String var) throws RSessionException,
    REngineException, REXPMismatchException;

  REXP createReference(Object requester, REXP source) throws RSessionException,
    REngineException, REXPMismatchException;

  REXP get(Object requester, String var) throws RSessionException,
    REngineException, REXPMismatchException;

  void assign(Object requester, String var, REXP val) throws RSessionException,
    REngineException, REXPMismatchException;

  void assign(Object requester, String var, byte[] val)
    throws RSessionException, REngineException;

  void assign(Object requester, String var, double[] val)
    throws RSessionException, REngineException;

  void assign(Object requester, String var, int[] val)
    throws RSessionException, REngineException;

  void assign(Object requester, String var, String[] val)
    throws RSessionException, REngineException;

  REXP parseAndEval(Object requester, String cmd) throws RSessionException,
    REngineException, REXPMismatchException;

  void close();
}
