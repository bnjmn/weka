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
 *    Environment.java
 *    Copyright (C) 2008 Pentaho Coproration
 *
 */

package weka.core;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Enumeration;

/**
 * This class encapsulates a map of all environment and java system properties.
 * There are methods for adding and removing variables as well as a method for
 * replacing key names (enclosed by ${}) with their associated value in Strings.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com
 * @version $Revision: 1.1.2.3 $
 */
public class Environment implements RevisionHandler {
  
  // Map to hold all the environment variables + java properties
  private static Map<String,String> ENV_VARS = new HashMap<String,String>();

  // Set up
  static {
    // get the env variables first
    Map<String,String> env = System.getenv();
    Set<String> keys = env.keySet();
    Iterator<String> i = keys.iterator();
    while (i.hasNext()) {
      String kv = i.next();
      String value = env.get(kv);
      ENV_VARS.put(kv, value);
    }

    // get the java properties
    Properties jvmProps = System.getProperties();
    Enumeration pKeys = jvmProps.propertyNames();
    while (pKeys.hasMoreElements()) {
      String kv = (String)pKeys.nextElement();
      String value = jvmProps.getProperty(kv);
      ENV_VARS.put(kv, value);
    }
    ENV_VARS.put("weka.version", Version.VERSION);
  }

  /**
   * Substitute a variable names for their values in the given string.
   * 
   * @param source the source string to replace variables in
   * @return a String with all variable names replaced with their values
   * @throws Exception if an unknown variable name is encountered
   */
  public static String substitute(String source) throws Exception {
    // Grab each variable out of the string
    int index = source.indexOf("${");

    while (index >= 0) {
      index += 2;
      int endIndex = source.indexOf('}');
      if (endIndex >= 0 && endIndex > index +1) {
        String key = source.substring(index, endIndex);

        // look this sucker up
        String replace = ENV_VARS.get(key);
        if (replace != null) {
          String toReplace = "${" + key + "}";
          source = source.replace(toReplace, replace);
        } else {
          throw new Exception("[Environment] Variable " 
                              + key + " doesn't seem to be set.");
        }
      }
      index = source.indexOf("${");
    }
    return source;
  }

  /**
   * Add a variable to the internal map.
   *
   * @param key the name of the variable
   * @param value its value
   */
  public static void addVariable(String key, String value) {
    ENV_VARS.put(key, value);
  }

  /**
   * Remove a named variable from the map.
   *
   * @param key the name of the varaible to remove.
   */
  public static void removeVariable(String key) {
    ENV_VARS.remove(key);
  }

  /**
   * Get the value for a particular variable.
   *
   * @param key the name of the variable to get
   * @return the associated value or null if this variable
   * is not in the internal map
   */
  public static String getVariable(String key) {
    return ENV_VARS.get(key);
  }

  /**
   * Main method for testing this class.
   *
   * @param a list of strings to replace variables in 
   * (e.g. "\${os.name} "\${java.version}")
   */
  public static void main(String[] args) {
    Environment t = new Environment();
    //    String test = "Here is a string with the variable ${java.version} and ${os.name} in it";

    if (args.length == 0) {
      System.err.println("Usage: java weka.core.Environment <string> <string> ...");
    } else {
      try {
        for (int i = 0; i < args.length; i++) {
          String newS = Environment.substitute(args[i]);
          System.out.println("Original string:\n" + args[i] +"\n\nNew string:\n" + newS);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.1.2.3 $");
  }
}