/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    Version.java
 *    Copyright (C) 2005 FracPete
 *
 */

package weka.core;

/**
 * This class contains the version number of the current WEKA release and some
 * methods for comparing another version string. The normal layout of a
 * version string is "MAJOR.MINOR.REVISION", but it can also handle partial
 * version strings, e.g. "3.4".<br>
 * Should be used e.g. in exports to XML for keeping track, with which version 
 * of WEKA the file was produced.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1.2.2 $ 
 */
public class Version implements Comparable {
  /** the major version */
  public final static int MAJOR = 3; 
  
  /** the minor version */
  public final static int MINOR = 4; 
  
  /** the revision */
  public final static int REVISION = 4;
  
  /** the complete version */
  public final static String VERSION = MAJOR + "." + MINOR + "." + REVISION;
  
  /**
   * checks the version of this class against the given version-string
   * @param o     the version-string to compare with
   * @return      -1 if this version is less, 0 if equal and +1 if greater
   *              than the provided version 
   */
  public int compareTo(Object o) {
    int       result;
    int       major;
    int       minor;
    int       revision;
    String    tmpStr;
    
    major    = 0;
    minor    = 0;
    revision = 0;
    
    // do we have a string?
    if (o instanceof String) {
      try {
        tmpStr = o.toString();
        if (tmpStr.indexOf(".") > -1) {
           major  = Integer.parseInt(tmpStr.substring(0, tmpStr.indexOf(".")));
           tmpStr = tmpStr.substring(tmpStr.indexOf(".") + 1);
           if (tmpStr.indexOf(".") > -1) {
             minor  = Integer.parseInt(tmpStr.substring(0, tmpStr.indexOf(".")));
             tmpStr = tmpStr.substring(tmpStr.indexOf(".") + 1);
             if (!tmpStr.equals(""))
               revision = Integer.parseInt(tmpStr);
             else
               revision = 0;
           }
           else {
             if (!tmpStr.equals(""))
               minor = Integer.parseInt(tmpStr);
             else
               minor = 0;
           }
        }
        else {
          if (!tmpStr.equals(""))
            major = Integer.parseInt(tmpStr);
          else
            major = 0;
        }
      }
      catch (Exception e) {
        e.printStackTrace();
        major    = -1;
        minor    = -1;
        revision = -1;
      }
    }
    else {
      System.out.println(this.getClass().getName() + ": no version-string for comparTo povided!");
      major    = -1;
      minor    = -1;
      revision = -1;
    }

    if (MAJOR < major) {
      result = -1;
    }
    else if (MAJOR == major) {
      if (MINOR < minor) {
        result = -1;
      }
      else if (MINOR == minor) {
        if (REVISION < revision) {
          result = -1;
        }
        else if (REVISION == revision) {
          result = 0;
        }
        else {
          result = 1;
        }
      }
      else {
        result = 1;
      }
    }
    else {
      result = 1;
    }
    
    return result;
  }
  
  /**
   * whether the given version string is equal to this version
   * @param o       the version-string to compare to
   * @return        TRUE if the version-string is equals to its own
   */
  public boolean equals(Object o) {
    return (compareTo(o) == 0);
  }
  
  /**
   * checks whether this version is older than the one from the given 
   * version string
   * @param o       the version-string to compare with
   * @return        TRUE if this version is older than the given one
   */
  public boolean isOlder(Object o) {
    return (compareTo(o) == -1);
  }
  
  /**
   * checks whether this version is newer than the one from the given 
   * version string
   * @param o       the version-string to compare with
   * @return        TRUE if this version is newer than the given one
   */
  public boolean isNewer(Object o) {
    return (compareTo(o) == 1);
  }
  
  /**
   * only for testing
   */
  public static void main(String[] args) {
    Version       v;
    String        tmpStr;

    // print version
    System.out.println(VERSION + "\n");
    
    // test on different versions
    v = new Version();
    System.out.println("-1? " + v.compareTo("5.0.1"));
    System.out.println(" 0? " + v.compareTo(VERSION));
    System.out.println("+1? " + v.compareTo("3.4.0"));
    
    tmpStr = "5.0.1";
    System.out.println("\ncomparing with " + tmpStr);
    System.out.println("isOlder? " + v.isOlder(tmpStr));
    System.out.println("equals ? " + v.equals(tmpStr));
    System.out.println("isNewer? " + v.isNewer(tmpStr));
    
    tmpStr = VERSION;
    System.out.println("\ncomparing with " + tmpStr);
    System.out.println("isOlder? " + v.isOlder(tmpStr));
    System.out.println("equals ? " + v.equals(tmpStr));
    System.out.println("isNewer? " + v.isNewer(tmpStr));
    
    tmpStr = "3.4.0";
    System.out.println("\ncomparing with " + tmpStr);
    System.out.println("isOlder? " + v.isOlder(tmpStr));
    System.out.println("equals ? " + v.equals(tmpStr));
    System.out.println("isNewer? " + v.isNewer(tmpStr));
    
    tmpStr = "3.4";
    System.out.println("\ncomparing with " + tmpStr);
    System.out.println("isOlder? " + v.isOlder(tmpStr));
    System.out.println("equals ? " + v.equals(tmpStr));
    System.out.println("isNewer? " + v.isNewer(tmpStr));
    
    tmpStr = "5";
    System.out.println("\ncomparing with " + tmpStr);
    System.out.println("isOlder? " + v.isOlder(tmpStr));
    System.out.println("equals ? " + v.equals(tmpStr));
    System.out.println("isNewer? " + v.isNewer(tmpStr));
  }
}
