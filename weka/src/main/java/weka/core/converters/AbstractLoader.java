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
 *    AbstractLoader.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import weka.core.CommandlineRunnable;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Abstract class gives default implementation of setSource methods. All other
 * methods must be overridden.
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision$
 */
public abstract class AbstractLoader implements Loader, CommandlineRunnable {

  /** ID to avoid warning */
  private static final long serialVersionUID = 2425432084900694551L;
  /** The current retrieval mode */
  protected int m_retrieval;

  /**
   * Sets the retrieval mode.
   * 
   * @param mode the retrieval mode
   */
  @Override
  public void setRetrieval(int mode) {

    m_retrieval = mode;
  }

  /**
   * Gets the retrieval mode.
   * 
   * @return the retrieval mode
   */
  protected int getRetrieval() {

    return m_retrieval;
  }

  /**
   * Default implementation throws an IOException.
   * 
   * @param file the File
   * @exception IOException always
   */
  @Override
  public void setSource(File file) throws IOException {

    throw new IOException("Setting File as source not supported");
  }

  /**
   * Default implementation sets retrieval mode to NONE
   * 
   * @exception never.
   */
  @Override
  public void reset() throws Exception {
    m_retrieval = NONE;
  }

  /**
   * Default implementation throws an IOException.
   * 
   * @param input the input stream
   * @exception IOException always
   */
  @Override
  public void setSource(InputStream input) throws IOException {

    throw new IOException("Setting InputStream as source not supported");
  }

  /*
   * To be overridden.
   */
  @Override
  public abstract Instances getStructure() throws IOException;

  /*
   * To be overridden.
   */
  @Override
  public abstract Instances getDataSet() throws IOException;

  /*
   * To be overridden.
   */
  @Override
  public abstract Instance getNextInstance(Instances structure)
    throws IOException;

  /**
   * Perform any setup stuff that might need to happen before commandline
   * execution. Subclasses should override if they need to do something here
   *
   * @throws Exception if a problem occurs during setup
   */
  @Override
  public void preExecution() throws Exception {
  }

  /**
   * Execute the supplied object. Subclasses need to override this method.
   *
   * @param toRun the object to execute
   * @param options any options to pass to the object
   * @throws IllegalArgumentException if the object is not of the expected type.
   */
  @Override
  public void run(Object toRun, String[] options) {
    throw new IllegalArgumentException(
      "Subclass needs to override this method!");
  }

  /**
   * Perform any teardown stuff that might need to happen after execution.
   * Subclasses should override if they need to do something here
   *
   * @throws Exception if a problem occurs during teardown
   */
  @Override
  public void postExecution() throws Exception {
  }
}
