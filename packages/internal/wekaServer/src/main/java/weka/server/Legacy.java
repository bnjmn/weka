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
 *    Legacy.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Binary/legacy related task stuff. Binary is used for internal execution and
 * master - slave transfer of tasks/results
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class Legacy {

  public static String LEGACY_CLIENT_KEY = "client";

  /**
   * Utility routine to persist a result
   *
   * @param result the result to persist
   * @return the {@code File} the result was persisted to
   * @throws Exception if a problem occurs
   */
  public static File persistResult(Object result) throws Exception {
    ObjectOutputStream oos = null;
    File persistedResult = null;
    try {
      persistedResult = WekaServer.getTempFile();
      oos =
        new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(
          new FileOutputStream(persistedResult))));
      oos.writeObject(result);
      oos.flush();
    } finally {
      if (oos != null) {
        oos.close();
      }
    }

    return persistedResult;
  }

  /**
   * Load a result from a file
   *
   * @param persistedResult the {@code File} to load the result from
   * @return the loaded result
   * @throws Exception if a problem occurs
   */
  public static Object loadResult(File persistedResult) throws Exception {
    if (persistedResult == null || !persistedResult.exists()) {
      throw new Exception("Result file seems to have disapeared!");
    }

    ObjectInputStream ois = null;
    try {
      ois =
        new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
          new FileInputStream(persistedResult))));
      return  ois.readObject();
    } finally {
      if (ois != null) {
        ois.close();
      }
    }
  }
}
