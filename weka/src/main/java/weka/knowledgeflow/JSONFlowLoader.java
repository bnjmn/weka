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
 *    JSONFlowLoader.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;

import weka.core.WekaException;
import weka.gui.Logger;

/**
 * Flow loader that wraps the routines in JSONFlowUtils
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class JSONFlowLoader implements FlowLoader {

  protected Logger m_log;

  public static final String EXTENSION = "kf";

  @Override
  public void setLog(Logger log) {
    m_log = log;
  }

  @Override
  public String getFlowFileExtension() {
    return EXTENSION;
  }

  @Override
  public String getFlowFileExtensionDescription() {
    return "JSON Knowledge Flow configuration files";
  }

  @Override
  public Flow readFlow(File flowFile) throws WekaException {
    return JSONFlowUtils.readFlow(flowFile);
  }

  @Override
  public Flow readFlow(InputStream is) throws WekaException {
    return JSONFlowUtils.readFlow(is);
  }

  @Override
  public Flow readFlow(Reader r) throws WekaException {
    return JSONFlowUtils.readFlow(r);
  }
}
