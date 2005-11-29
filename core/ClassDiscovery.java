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
 * ClassDiscovery.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This class is used for discovering classes that implement a certain
 * interface or a derived from a certain class.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 * @see StringCompare
 */
public class ClassDiscovery {

  /** whether to output some debug information */
  public final static boolean VERBOSE = false;
  
  /** notify if VERBOSE is still on */
  static {
    if (VERBOSE)
      System.err.println(ClassDiscovery.class.getName() + ": VERBOSE ON");
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
   * @param pkgname           the package to look for
   * @return                  if found, the url as string, otherwise null
   */
  protected static URL getURL(String classpathPart, String pkgname) {
    String              urlStr;
    URL                 result;
    File                classpathFile;
    File                file;
    JarFile             jarfile;
    Enumeration         enm;
    String              pkgnameTmp;
    
    result = null;
    urlStr = null;

    try {
      classpathFile = new File(classpathPart);
      
      // directory or jar?
      if (classpathFile.isDirectory()) {
        // does the package exist in this directory?
        file = new File(classpathPart + pkgname);
        if (file.exists())
          urlStr = "file:" + classpathPart + pkgname;
      }
      else {
        // is package actually included in jar?
        jarfile    = new JarFile(classpathPart);
        enm        = jarfile.entries();
        pkgnameTmp = pkgname.substring(1);   // remove the leading "/"
        while (enm.hasMoreElements()) {
          if (enm.nextElement().toString().startsWith(pkgnameTmp)) {
            urlStr = "jar:file:" + classpathPart + "!" + pkgname;
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
        System.err.println(
            "Trying to create URL from '" + urlStr 
            + "' generates this exception:\n" + e);
        result = null;
      }
    }

    return result;
  }

  /**
   * Checks the given packages for classes that inherited from the given class,
   * in case it's a class, or implement this class, in case it's an interface.
   *
   * @param classname       the class/interface to look for
   * @param pkgnames        the packages to search in
   * @return                a list with all the found classnames
   */
  public static Vector find(String classname, String[] pkgnames) {
    Vector      result;
    Class       cls;

    result = new Vector();

    try {
      cls    = Class.forName(classname);
      result = find(cls, pkgnames);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /**
   * Checks the given package for classes that inherited from the given class,
   * in case it's a class, or implement this class, in case it's an interface.
   *
   * @param classname       the class/interface to look for
   * @param pkgname         the package to search in
   * @return                a list with all the found classnames
   */
  public static Vector find(String classname, String pkgname) {
    Vector      result;
    Class       cls;

    result = new Vector();

    try {
      cls    = Class.forName(classname);
      result = find(cls, pkgname);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /**
   * Checks the given packages for classes that inherited from the given class,
   * in case it's a class, or implement this class, in case it's an interface.
   *
   * @param cls             the class/interface to look for
   * @param pkgnames        the packages to search in
   * @return                a list with all the found classnames
   */
  public static Vector find(Class cls, String[] pkgnames) {
    Vector        result;
    int           i;

    result = new Vector();

    for (i = 0; i < pkgnames.length; i++)
      result.addAll(find(cls, pkgnames[i]));

    // sort result
    Collections.sort(result, new ClassDiscovery().new StringCompare());

    return result;
  }

  /**
   * Checks the given package for classes that inherited from the given class,
   * in case it's a class, or implement this class, in case it's an interface.
   *
   * @param cls             the class/interface to look for
   * @param pkgname         the package to search in
   * @return                a list with all the found classnames
   */
  public static Vector find(Class cls, String pkgname) {
    Vector                result;
    StringTokenizer       tok;
    String                part;
    String                pkgpath;
    File                  dir;
    File[]                files;
    URL                   url;
    int                   i;
    Class                 clsNew;
    String                classname;
    JarFile               jar;
    JarEntry              entry;
    Enumeration           enm;

    result = new Vector();

    if (VERBOSE)
      System.out.println(
          "Searching for '" + cls.getName() + "' in '" + pkgname + "':");

    // turn package into path
    pkgpath = pkgname.replaceAll("\\.", "/");
    
    // check all parts of the classpath, to include additional classes from
    // "parallel" directories/jars, not just the first occurence
    tok = new StringTokenizer(
        System.getProperty("java.class.path"), 
        System.getProperty("path.separator"));

    while (tok.hasMoreTokens()) {
      part = tok.nextToken();
      if (VERBOSE)
        System.out.println("Classpath-part: " + part);
      
      // does package exist in this part of the classpath?
      url = getURL(part, "/" + pkgpath);
      if (VERBOSE) {
        if (url == null)
          System.out.println("   " + pkgpath + " NOT FOUND");
        else
          System.out.println("   " + pkgpath + " FOUND");
      }
      if (url == null)
        continue;

      // find classes
      dir = new File(part + "/" + pkgpath);
      if (dir.exists()) {
        files = dir.listFiles();
        for (i = 0; i < files.length; i++) {
          // only class files
          if (    (!files[i].isFile()) 
               || (!files[i].getName().endsWith(".class")) )
            continue;

          try {
            classname =   pkgname + "." 
                        + files[i].getName().replaceAll(".*/", "")
                                            .replaceAll("\\.class", "");
            result.add(classname);
          }
          catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      else {
        try {
          jar = new JarFile(part);
          enm = jar.entries();
          while (enm.hasMoreElements()) {
            entry = (JarEntry) enm.nextElement();
            
            // only class files
            if (    (entry.isDirectory())
                 || (!entry.getName().endsWith(".class")) )
              continue;

            classname = entry.getName().replaceAll("\\.class", "");

            // only classes in the particular package
            if (!classname.startsWith(pkgpath))
              continue;

            // no sub-package
            if (classname.substring(pkgpath.length() + 1).indexOf("/") > -1)
              continue;

            result.add(classname.replaceAll("/", "."));
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    // check classes
    i = 0;
    while (i < result.size()) {
      try {
        clsNew = Class.forName((String) result.get(i));
        
        // no abstract classes
        if (Modifier.isAbstract(clsNew.getModifiers()))
          result.remove(i);
        // must implement interface
        else if ( (cls.isInterface()) && (!hasInterface(cls, clsNew)) )
          result.remove(i);
        // must be derived from class
        else if ( (!cls.isInterface()) && (!isSubclass(cls, clsNew)) )
          result.remove(i);
        else
          i++;
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    // sort result
    Collections.sort(result, new ClassDiscovery().new StringCompare());

    return result;
  }

  /**
   * For testing only. Takes two arguments:
   * <ol>
   *    <li>classname</li>
   *    <li>packagename</li>
   * </ol>
   * Prints the classes it found.
   */
  public static void main(String[] args) {
    Vector      list;
    int         i;
    
    if (args.length != 2) {
      System.out.println(
          "\nUsage: " + ClassDiscovery.class.getName() 
          + " <classname> <packagename>\n");
      System.exit(1);
    }

    // search
    list = ClassDiscovery.find(args[0], args[1]);

    // print result, if any
    System.out.println(
        "Searching for '" + args[0] + "' in '" + args[1] + "':\n" 
        + "  " + list.size() + " found.");
    for (i = 0; i < list.size(); i++)
      System.out.println("  " + (i+1) + ". " + list.get(i));
  }
  
  /**
   * compares two strings with the following order:<br/>
   * <ul>
   *    <li>case insensitive</li>
   *    <li>german umlauts (&auml; , &ouml; etc.) or other non-ASCII letters
   *    are treated as special chars</li>
   *    <li>special chars &lt; numbers &lt; letters</li>
   * </ul>
   */
  public class StringCompare 
    implements Comparator {

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
