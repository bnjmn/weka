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
 *    FileHelper.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.File;

/**
 * Wrapper class for File objects. File objects wrapped in this class
 * can be serialized by Weka's XML serialization mechanism.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class FileHelper {

  /** The file path as a string */
  protected String m_filePath;

  /**
   * Constructor
   *
   * @param file the file to wrap
   */
  public FileHelper(File file) {
    m_filePath = file.toString();
  }

  /**
   * No-op constructor for beans conformity
   */
  public FileHelper() {
  }

  /**
   * Set the file path
   *
   * @param path the path to set
   */
  public void setFilePath(String path) {
    m_filePath = path;
  }

  /**
   * Get the file path
   *
   * @return  the path to set
   */
  public String getFilePath() {
    return m_filePath;
  }

  /**
   * Get the file wrapped in this instance
   *
   * @return the File object
   */
  public File getFile() {
    if (m_filePath != null) {
      return new File(m_filePath);
    }

    return new File(System.getProperty("user.home"));
  }
}
