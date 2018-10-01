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
 *    SetEnvironmentVariables.java
 *    Copyright (C) 2018 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.core;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * JNA interface to set environment variables.
 *
 * @author Eibe Frank (with help from the web)
 * @version $Revision: 14435 $
 */
public class SetEnvironmentVariables {

  public static LibCWrapper INSTANCE;
    
  private static Object library;

  static {
      if (System.getProperty("os.name").contains("Windows")) {
	  library = Native.loadLibrary("msvcrt", Msvcrt.class);
      } else {
	  library = Native.loadLibrary("c", LibC.class);
      }
      INSTANCE = new LibCWrapper();
  }
  
  interface Msvcrt extends Library {
    public int _putenv(String name);
    public String getenv(String name);
  }

  interface LibC extends Library {
    public int setenv(String name, String value, int overwrite);
    public String getenv(String name);
    public int unsetenv(String name);
  }

  public static class LibCWrapper {
      
      public int setenv(String name, String value, int overwrite) {
	  if (library instanceof LibC) {
	      return ((LibC)library).setenv(name, value, overwrite);
	  }
	  else {
	      return ((Msvcrt)library)._putenv(name + "=" + value);
	  }
      }
      
      public String getenv(String name) {
	  if (library instanceof LibC) {
	      return ((LibC)library).getenv(name);
	  }
	  else {
	      return ((Msvcrt)library).getenv(name);
	  }
      }
      
      public int unsetenv(String name) {
	  if (library instanceof LibC) {
	      return ((LibC)library).unsetenv(name);
	  }
	  else {
	      return ((Msvcrt)library)._putenv(name + "=");
	  }
      }
  }
}
