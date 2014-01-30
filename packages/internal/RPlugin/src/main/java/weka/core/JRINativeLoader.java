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
 *    JRINativeLoader.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.File;

import org.rosuda.JRI.Rengine;

/**
 * Takes care of loading the native JRI library. The byte code for this class
 * gets injected into the root class loader by weka.core.JRILoader in order to
 * ensure that the native library is visible to all child class loaders.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class JRINativeLoader {

  protected static boolean s_jriLoaded;

  /**
   * This is the currently used library.
   */
  private static String s_jriLibrary = null;

  /**
   * This method will try to load a JRI library that is in the accessible
   * library path. If anything goes wrong, an exception is thrown and
   * {@link #isJriLoaded()} will still return false
   */
  public static void loadLibrary() throws UnsatisfiedLinkError,
    SecurityException {
    // loadLibrary(new File("jri"));
    System.err.println("Trying to load R library from java.library.path");
    System.err.println("Engine class: " + Rengine.class + " ClassLoader:"
      + Rengine.class.getClassLoader());
    s_jriLoaded = false;
    s_jriLibrary = "jri";
    System.loadLibrary(s_jriLibrary);
    System.err.println("Successfully loaded R library from java.library.path");
  }

  /**
   * This method will try to load a JRI library that is addressed by
   * libraryFile. This still needs to have the R libraries in the accessible
   * path to resolve links.
   * 
   * If anything goes wrong, an exception is thrown and {@link #isJriLoaded()}
   * will still return false
   */
  public static void loadLibrary(String libraryFile)
    throws UnsatisfiedLinkError, SecurityException {
    System.err.println("Trying to loaded R library from " + libraryFile);
    System.err.println("Engine class: " + Rengine.class + " ClassLoader:"
      + Rengine.class.getClassLoader());
    s_jriLoaded = false;
    s_jriLibrary = libraryFile;
    try {
      System.load(s_jriLibrary);
      s_jriLoaded = true;
      System.err.println("Successfully loaded R library from " + s_jriLibrary);
    } catch (UnsatisfiedLinkError e) {
      System.err.println("Unable to load R library from " + s_jriLibrary + ": "
        + e.getMessage());
    }
  }

  /**
   * This method will try to load a JRI library that is addressed by
   * libraryFile. This still needs to have the R libraries in the accessible
   * path to resolve links.
   * 
   * If anything goes wrong, an exception is thrown and {@link #isJriLoaded()}
   * will still return false
   */
  public static void loadLibrary(File libraryFile) throws UnsatisfiedLinkError,
    SecurityException {
    loadLibrary(libraryFile.getAbsolutePath());
  }

  /**
   * This indicates if the library for communicating with R could be loaded at
   * all.
   */
  public static boolean isJriLoaded() {
    if (!s_jriLoaded) {
      if (s_jriLibrary != null) {
        try {
          System.err.println("Access on Rengine but initialization failed! ID:"
            + Rengine.class.hashCode());
          System.err.println("Loading library on the fly...");
          loadLibrary(s_jriLibrary);
        } catch (UnsatisfiedLinkError e) {
          System.err.println("Error during loading library on the fly! "
            + e.getMessage());
        } catch (SecurityException e) {
          System.err.println("Error during loading library on the fly! "
            + e.getMessage());
        }
      } else {
        /*
         * System.err.println("Access on uninitialized Rengine!");
         * System.err.println("Engine ID: " + Rengine.class + " ClassLoader:" +
         * Rengine.class.getClassLoader());
         */
      }
    }
    return s_jriLoaded;
  }
}
