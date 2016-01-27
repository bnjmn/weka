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
 *    FlowLoader
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.WekaException;
import weka.gui.Logger;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;

/**
 * Interface to something that can load a Knowledge Flow
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface FlowLoader {

  /**
   * Set a log to use
   *
   * @param log log to use
   */
  void setLog(Logger log);

  /**
   * Get the extension of the Knowledge Flow file format handled by this loader
   *
   * @return the flow file extension
   */
  String getFlowFileExtension();

  /**
   * Get a description of the flow file format handled by this loader
   *
   * @return a description of the file format handles
   */
  String getFlowFileExtensionDescription();

  /**
   * Load a flow from the supplied file
   *
   * @param flowFile the file to load from
   * @return the loaded Flow
   * @throws WekaException if a problem occurs
   */
  Flow readFlow(File flowFile) throws WekaException;

  /**
   * Load a flow from the supplied input stream
   *
   * @param is the input stream to load from
   * @return the loaded Flow
   * @throws WekaException if a problem occurs
   */
  Flow readFlow(InputStream is) throws WekaException;

  /**
   * Load a flow from the supplied reader
   *
   * @param r the reader to load from
   * @return the loaded Flow
   * @throws WekaException if a problem occurs
   */
  Flow readFlow(Reader r) throws WekaException;
}
