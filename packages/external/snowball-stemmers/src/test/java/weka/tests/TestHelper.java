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

/**
 * TestHelper.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.tests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Helper class for tests.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class TestHelper {

  /**
   * Sets the root for the regression reference files.
   */
  public static void setRegressionRoot() {
    System.setProperty("weka.test.Regression.root", "src/test/resources");
  }

  /**
   * Returns the tmp directory.
   *
   * @return		the tmp directory
   */
  public static String getTmpDirectory() {
    return System.getProperty("java.io.tmpdir");
  }

  /**
   * Returns the location in the tmp directory for given resource.
   *
   * @param resource	the resource (path in project) to get the tmp location for
   * @return		the tmp location
   * @see		#getTmpDirectory()
   */
  public static String getTmpLocationFromResource(String resource) {
    String	result;
    File file;

    file   = new File(resource);
    result = getTmpDirectory() + File.separator + file.getName();

    return result;
  }

  /**
   * Copies the given resource to the tmp directory.
   *
   * @param resource	the resource (path in project) to copy
   * @return		false if copying failed
   */
  public static boolean copyResourceToTmp(String resource) {
    boolean			result;
    BufferedInputStream input;
    BufferedOutputStream output;
    byte[]			buffer;
    int				read;
    String			ext;

    input  = null;
    output = null;

    try {
      input  = new BufferedInputStream(ClassLoader.getSystemResourceAsStream(resource));
      output = new BufferedOutputStream(new FileOutputStream(getTmpLocationFromResource(resource)));
      buffer = new byte[1024];
      while ((read = input.read(buffer)) != -1) {
	output.write(buffer, 0, read);
	if (read < buffer.length)
	  break;
      }
      result = true;
    }
    catch (IOException e) {
      if (e.getMessage().equals("Stream closed")) {
	ext = resource.replaceAll(".*\\.", "");
	System.err.println(
	    "Resource '" + resource + "' not available? "
	    + "Or extension '*." + ext + "' not in pom.xml ('project.build.testSourceDirectory') listed?");
      }
      e.printStackTrace();
      result = false;
    }
    catch (Exception e) {
      e.printStackTrace();
      result = false;
    }

    if (input != null) {
      try {
	input.close();
      }
      catch (Exception e) {
	// ignored
      }
    }
    if (output != null) {
      try {
	output.close();
      }
      catch (Exception e) {
	// ignored
      }
    }

    return result;
  }

  /**
   * Removes the file from the tmp directory.
   *
   * @param filename	the file in the tmp directory to delete (no path!)
   * @return		true if deleting succeeded or file not present
   */
  public static boolean deleteFileFromTmp(String filename) {
    boolean	result;
    File	file;

    result = true;
    file   = new File(getTmpDirectory() + File.separator + filename);
    if (file.exists())
      result = file.delete();

    return result;
  }
}
