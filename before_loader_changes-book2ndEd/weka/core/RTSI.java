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
 *    RTSI.java
 *    Copyright (C) Daniel Le Berre and http://www.javaworld.com
 *
 */
package weka.core;

import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.JarURLConnection;
import java.util.jar.*;
import java.util.zip.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This utility class is looking for all the classes implementing or 
 * inheriting from a given interface or class.<br>
 * (RTSI = RunTime Subclass Identification)
 * <p>
 * <b>Notes</b><br>
 * <ul>
 *    <li>Source: JavaWorld <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip113.html" target="_blank">Tip 113</a>: Identify subclasses at runtime</li>
 *    <li>JWhich: JavaWorld <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip105.html" target="_blank">Tip 105</a>: Mastering the classpath with JWhich</li>
 *    <li>
 *       Modifications by FracPete:<br>
 *       <ul>
 *          <li>it returns Vectors with the classnames (for the sorting see <code>StringCompare</code>)</li>
 *          <li>doesn't create an instance of class anymore, but rather tests, whether the superclass/interface is
 *              somewhere in the class hierarchy of the found class and whether it is abstract or not</li>
 *          <li>checks all parts of the classpath for the package and does not take the first one only
 *              (i.e. you can have a dir with the default classes and an additional dir with more classes
 *              that are not part of the default ones, e.g. developer classes)</li>
 *    </li>
 * </ul>
 *
 * @see StringCompare
 * @author <a href="mailto:daniel@satlive.org">Daniel Le Berre</a>
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2.2.4 $
 */
public class RTSI {
  /** whether to output some debug information */
  public final static boolean VERBOSE = false;
  
  /** notify if VERBOSE is still on */
  static {
    if (VERBOSE)
      System.err.println(RTSI.class.getName() + ": VERBOSE ON");
  }
  
  /**
   * Returns all the classes inheriting or implementing a given
   * class in the currently loaded packages.<br>
   * <b>Note:</b> If a package, containing subclasses, has not been loaded
   * by the time of this method call, these classes won't be found! It's 
   * better to define the package name explicitly in which to look for 
   * subclasses, like in <code>find(String,String)</code>. 
   * 
   * @param tosubclassname    the name of the class to inherit from
   * @return                  a Vector with all the classnames
   * @see #find(String,String)
   */
  public static Vector find(String tosubclassname) {
    Vector        result;
    Vector        tmpResult;
    
    result = new Vector();
    
    try {
      Class tosubclass = Class.forName(tosubclassname);
      Package[] pcks   = Package.getPackages();
      for (int i = 0;i < pcks.length; i++) {
        tmpResult = find(pcks[i].getName(), tosubclass);
        result.addAll(tmpResult);
      }
    } 
    catch (ClassNotFoundException ex) {
      System.err.println("Class " + tosubclassname + " not found!");
    }

    return result;
  }
  
  /**
   * Returns all the classes inheriting or implementing a given
   * class in a given package.
   * 
   * @param pckgname      the fully qualified name of the package
   * @param tosubclass    the name of the class to inherit from
   * @return              a Vector with all the classnames
   */
  public static Vector find(String pckname, String tosubclassname) {
    try {
      Class tosubclass = Class.forName(tosubclassname);
      return find(pckname, tosubclass);
    } 
    catch (ClassNotFoundException ex) {
      System.err.println("Class " + tosubclassname + " not found!");
      return new Vector();
    }
  }
  
  /**
   * Checks whether the "otherclass" is a subclass of the given "superclass".
   * 
   * @param superclass      the superclass to check against
   * @param otherclass      this class is checked whether it is a subclass
   *                        of the the superclass
   * @return                TRUE if "otherclass" is a true subclass
   */
  public static boolean isSubclass(Class superclass, Class otherclass) {
    Class       currentclass;
    boolean     result;
    
    result       = false;
    currentclass = otherclass;
    do {
      result = currentclass.equals(superclass);
      
      // topmost class reached?
      if (currentclass.equals(Object.class))
        break;
      
      if (!result)
        currentclass = currentclass.getSuperclass(); 
    } 
    while (!result);
    
    return result;
  }
  
  /**
   * Checks whether the given class implements the given interface.
   * 
   * @param intf      the interface to look for in the given class
   * @param cls       the class to check for the interface
   * @return          TRUE if the class contains the interface 
   */
  public static boolean hasInterface(Class intf, Class cls) {
    Class[]       intfs;
    int           i;
    boolean       result;
    Class         currentclass;
    
    result       = false;
    currentclass = cls;
    do {
      // check all the interfaces, this class implements
      intfs = currentclass.getInterfaces();
      for (i = 0; i < intfs.length; i++) {
        if (intfs[i].equals(intf)) {
          result = true;
          break;
        }
      }

      // get parent class
      if (!result) {
        currentclass = currentclass.getSuperclass();
        
        // topmost class reached?
        if (currentclass.equals(Object.class))
          break;
      }
    } 
    while (!result);
      
    return result;
  }
  
  /**
   * If the given package can be found in this part of the classpath then 
   * an URL object is returned, otherwise <code>null</code>.
   * 
   * @param classpathPart     the part of the classpath to look for the package
   * @param pckgname          the package to look for
   * @return                  if found the url in a string, otherwise null
   */
  protected static URL getURL(String classpathPart, String pckgname) {
    String              urlStr;
    URL                 result;
    File                classpathFile;
    File                file;
    JarFile             jarfile;
    Enumeration         enm;
    String              pckgnameTmp;
    
    result = null;
    urlStr = null;

    try {
      classpathFile = new File(classpathPart);
      
      // directory or jar?
      if (classpathFile.isDirectory()) {
        // does the package exist in this directory?
        file = new File(classpathPart + pckgname);
        if (file.exists())
          urlStr = "file:" + classpathPart + pckgname;
      }
      else {
        // is package actually included in jar?
        jarfile     = new JarFile(classpathPart);
        enm         = jarfile.entries();
        pckgnameTmp = pckgname.substring(1);   // remove the leading "/"
        while (enm.hasMoreElements()) {
          if (enm.nextElement().toString().startsWith(pckgnameTmp)) {
            urlStr = "jar:file:" + classpathPart + "!" + pckgname;
            break;
          }
        }
      }
    }
    catch (Exception e) {
      // ignore
    }
    
    // try to generate URL from url string
    if (urlStr != null) {
      try {
        result = new URL(urlStr);
      }
      catch (Exception e) {
        System.err.println("Trying to create URL from '" + urlStr + "' generates this exception:\n" + e);
        result = null;
      }
    }
    
    if (VERBOSE)
      System.out.println("Classpath " + classpathPart + ", package " + pckgname + " -> " + result);
  
    return result;
  }
  
  /**
   * Return all the classes inheriting or implementing a given
   * class in a given package.
   * 
   * @param pckgname    the fully qualified name of the package
   * @param tosubclass  the Class object to inherit from
   * @return            a Vector with all the classnames
   */
  public static Vector find(String pckgname, Class tosubclass) {
    Vector          result;
    
    result = new Vector();
    
    // Code from JWhich
    // ======
    // Translate the package name into an absolute path
    String name = new String(pckgname);
    if (!name.startsWith("/")) {
      name = "/" + name;
    }	
    name = name.replace('.','/');

    // traverse complete classpath, since we might have additional classes
    // in a parallel path...
    StringTokenizer tok = new StringTokenizer(System.getProperty("java.class.path"), System.getProperty("path.separator"));
    while (tok.hasMoreTokens()) {
      String part = tok.nextToken();
      URL url     = getURL(part, name);

      // did we find the package in this classpath-part?
      if (url == null)
      	continue;

      // file in filesystem or jar? 
      File directory = new File(url.getFile());
      if (directory.exists()) {
        // Get the list of the files contained in the package
        String[] files = directory.list();
        for (int i = 0; i < files.length; i++) {
          // we are only interested in .class files
          if (files[i].endsWith(".class")) {
            // removes the .class extension
            String classname = files[i].substring(0, files[i].length() - 6);
            try {
              Class cls = Class.forName(pckgname + "." + classname);
              if (VERBOSE)
                System.out.println("- Checking: " + classname);
              if (    !Modifier.isAbstract(cls.getModifiers()) 
                   && !cls.isPrimitive()) {
                if (    (!tosubclass.isInterface() && isSubclass(tosubclass, cls)) 
                     || (tosubclass.isInterface() && hasInterface(tosubclass, cls))) {
                  if (!result.contains(cls.getName())) {
                    if (VERBOSE)
                      System.out.println("- Added: " + classname);
                    result.add(cls.getName());
                  }
                }
              }
            } 
            catch (ClassNotFoundException cnfex) {
              System.err.println(cnfex);
            } 
          }
        }
      } 
      else {
        try {
          // It does not work with the filesystem: we must
          // be in the case of a package contained in a jar file.
          JarURLConnection conn = (JarURLConnection)url.openConnection();
          String starts = conn.getEntryName();
          JarFile jfile = conn.getJarFile();
          Enumeration e = jfile.entries();
          while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry)e.nextElement();
            String entryname = entry.getName();
            if (entryname.startsWith(starts)
                && (entryname.lastIndexOf('/') <= starts.length())
                && entryname.endsWith(".class")) {
              String classname = entryname.substring(0, entryname.length() - 6);
              if (classname.startsWith("/")) 
                classname = classname.substring(1);
              classname = classname.replace('/', '.');
              try {
                // package name is already included!
                //Class cls = Class.forName(pckgname + "." + classname);
                Class cls = Class.forName(classname);
                if (VERBOSE)
                  System.out.println("- Checking: " + classname);
                if (    !Modifier.isAbstract(cls.getModifiers()) 
                     && !cls.isPrimitive()) {
                  if (    (!tosubclass.isInterface() && isSubclass(tosubclass, cls)) 
                       || (tosubclass.isInterface() && hasInterface(tosubclass, cls))) {
                    if (!result.contains(cls.getName())) {
                      if (VERBOSE)
                        System.out.println("- Added: " + classname);
                      result.add(cls.getName());
                    }
                  }
                }
              } 
              catch (ClassNotFoundException cnfex) {
                System.err.println(cnfex);
              } 
            }
          }
        } 
        catch (IOException ioex) {
          System.err.println(ioex);
        } 
      }
    }
    
    // sort the result
    RTSI r = new RTSI();
    Collections.sort(result, r.new StringCompare());
    
    return result;
  }
  
  /**
   * for testing only
   */
  public static void main(String []args) {
    if (args.length == 2) {
      System.out.println(find(args[0], args[1]));
    } 
    else {
      if (args.length == 1) {
        System.out.println(find(args[0]));
      } 
      else {
        System.out.println("Usage: java " + RTSI.class.getName() + " [<package>] <subclass>");
      }
    }
  }
  
  /**
   * compares two strings with the following order:<br>
   * <ul>
   *    <li>case insensitive</li>
   *    <li>german umlauts (&auml; , &ouml; etc.) or other non-ASCII letters are treated as special chars</li>
   *    <li>special chars &lt; numbers &lt; letters</li>
   * </ul>
   */
  public class StringCompare implements Comparator {
    /**
     * appends blanks to the string if its shorter than <code>len</code>
     */
    private String fillUp(String s, int len) {
      while (s.length() < len)
        s += " ";
      return s;
    }
    
    /**
     * returns the group of the character: 0=special char, 1=number, 2=letter 
     */
    private int charGroup(char c) {
      int         result;
      
      result = 0;
      
      if ( (c >= 'a') && (c <= 'z') )
        result = 2;
      else if ( (c >= '0') && (c <= '9') )
        result = 1;
      
      return result;
    }
    
    /**
     * Compares its two arguments for order.
     */    
    public int compare(Object o1, Object o2) {
      String        s1;
      String        s2;
      int           i;
      int           result;
      int           v1;
      int           v2;
      
      result = 0;   // they're equal
      
      // get lower case string
      s1 = o1.toString().toLowerCase();
      s2 = o2.toString().toLowerCase();
      
      // same length
      s1 = fillUp(s1, s2.length());
      s2 = fillUp(s2, s1.length());
      
      for (i = 0; i < s1.length(); i++) {
        // same char?
        if (s1.charAt(i) == s2.charAt(i)) {
          result = 0;
        }
        else {
          v1 = charGroup(s1.charAt(i));
          v2 = charGroup(s2.charAt(i));
          
          // different type (special, number, letter)?
          if (v1 != v2) {
            if (v1 < v2)
              result = -1;
            else
              result = 1;
          }
          else {
            if (s1.charAt(i) < s2.charAt(i))
              result = -1;
            else
              result = 1;
          }
          
          break;
        }
      }
      
      return result;
    }
    
    /**
     * Indicates whether some other object is "equal to" this Comparator. 
     */
    public boolean equals(Object obj) {
      return (obj instanceof StringCompare);
    }
  }
}
