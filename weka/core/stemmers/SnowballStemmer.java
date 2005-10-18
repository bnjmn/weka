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
 * SnowballStemmer.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stemmers;

import weka.core.RTSI;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Wrapper class for the <a href="http://snowball.tartarus.org/">Snowball</a>
 * stemmer library (Java port). The stemmer classes must be present in the
 * CLASSPATH to be available.
 *
 * @author    FracPete (fracpete at waikato dot ac dot nz)
 * @version   $Revision: 1.1 $
 */
public class SnowballStemmer 
  implements Stemmer {
  
  /** the package name for snowball */
  public final static String PACKAGE = "org.tartarus.snowball";
  
  /** the package name where the stemmers are located */
  public final static String PACKAGE_EXT = PACKAGE + ".ext";

  /** the snowball program, all stemmers are derived from */
  protected final static String SNOWBALL_PROGRAM = PACKAGE + ".SnowballProgram";
  
  /** whether the snowball stemmers are in the Classpath */
  protected static boolean m_Present = false;

  /** contains the all the found stemmers (language names) */
  protected static Vector m_Stemmers;

  /** the current stemmer */
  protected Object m_Stemmer;

  /** the stem method */
  protected Method m_StemMethod;

  /** the setCurrent method */
  protected Method m_SetCurrentMethod;

  /** the getCurrent method */
  protected Method m_GetCurrentMethod;
   
  /** check for Snowball statically (needs only to be done once) */
  static {
    checkForSnowball();
    loadStemmers();
  }

  /**
   * initializes the stemmer ("porter")
   */
  public SnowballStemmer() {
    this("porter");
  }

  /**
   * initializes the stemmer with the given stemmer
   *
   * @param name        the name of the stemmer
   */
  public SnowballStemmer(String name) {
    super();
      
    setStemmer(name);
  }

  /**
   * checks whether Snowball is present in the classpath
   */
  private static void checkForSnowball() {
    try {
      Class.forName(SNOWBALL_PROGRAM);
      m_Present = true;
    }
    catch (Exception e) {
      m_Present = false;
    }
  }

  /**
   * extracts the stemmer name form the classname
   * 
   * @param classname     the full classname of the stemmer
   * @return              the name of the stemmer
   */
  private static String getStemmerName(String classname) {
    return classname.replaceAll(".*\\.", "").replaceAll("Stemmer$", "");
  }

  /**
   * returns the full classname of the stemmer
   *
   * @param name          the name of the stemmer
   * @return              the full classname of the stemmer
   * @see                 #PACKAGE_EXT
   */
  private static String getStemmerClassname(String name) {
    return PACKAGE_EXT + "." + name + "Stemmer";
  }

  /**
   * retrieves the language names of the availabel stemmers
   */
  private static void loadStemmers() {
    Vector        classnames;
    int           i;
    
    m_Stemmers = new Vector();
    
    if (!m_Present)
      return;

    classnames = RTSI.find(PACKAGE_EXT, SNOWBALL_PROGRAM);
    for (i = 0; i < classnames.size(); i++)
      m_Stemmers.add(getStemmerName(classnames.get(i).toString()));
  }

  /**
   * returns whether Snowball is present or not, i.e. whether the classes are
   * in the classpath or not
   *
   * @return whether Snowball is available
   */
  public static boolean isPresent() {
    return m_Present;
  }

  /**
   * returns an enumeration over all currently stored stemmer names
   */
  public static Enumeration listStemmers() {
    return m_Stemmers.elements();
  }

  /**
   * returns the name of the current stemmer, null if none is set
   */
  public String getStemmer() {
    if (m_Stemmer == null)
      return null;
    else
      return getStemmerName(m_Stemmer.getClass().getName());
  }

  /**
   * sets the stemmer with the given name, e.g., "porter"
   *
   * @param name        the name of the stemmer, e.g., "porter"
   */
  public void setStemmer(String name) {
    Class       snowballClass;
    Class[]     argClasses;
    
    if (m_Stemmers.contains(name)) {
      try {
        snowballClass = Class.forName(getStemmerClassname(name));
        m_Stemmer     = snowballClass.newInstance();

        // methods
        argClasses         = new Class[0];
        m_StemMethod       = snowballClass.getMethod("stem", argClasses);
        
        argClasses         = new Class[1];
        argClasses[0]      = String.class;
        m_SetCurrentMethod = snowballClass.getMethod("setCurrent", argClasses);
        
        argClasses         = new Class[0];
        m_GetCurrentMethod = snowballClass.getMethod("getCurrent", argClasses);
      }
      catch (Exception e) {
        System.out.println(
              "Error initializing stemmer '" + name + "'!"
            + e.getMessage());
        m_Stemmer = null;
      }
    }
    else {
      System.out.println("Stemmer '" + name + "' unknown!");
      m_Stemmer = null;
    }
  }

  /**
   * Returns the word in its stemmed form.
   *
   * @param word      the unstemmed word
   * @return          the stemmed word
   */
  public String stem(String word) {
    String      result;
    Object[]    args;
    
    if (m_Stemmer == null) {
      result = new String(word);
    }
    else {
      try {
        // set word
        args    = new Object[1];
        args[0] = word;
        m_SetCurrentMethod.invoke(m_Stemmer, args);

        // stem word
        args = new Object[0];
        m_StemMethod.invoke(m_Stemmer, args);

        // get word
        args   = new Object[0];
        result = (String) m_GetCurrentMethod.invoke(m_Stemmer, args);
      }
      catch (Exception e) {
        e.printStackTrace();
        result = word;
      }
    }
      
    return result;
  }

  /**
   * for testing only
   */
  public static void main(String[] args) {
    SnowballStemmer   s;
    String            word;
    Enumeration       enm;
    String            name;
    
    s   = new SnowballStemmer();
    enm = SnowballStemmer.listStemmers();
    
    while (enm.hasMoreElements()) {
      name = enm.nextElement().toString();
      System.out.println("\nStemmer: " + name);
      s.setStemmer(name);
      
      word = "shoes";
      System.out.println("- " + word + " -> " + s.stem(word));
      
      word = "houses";
      System.out.println("- " + word + " -> " + s.stem(word));
      
      word = "programmed";
      System.out.println("- " + word + " -> " + s.stem(word));
    }
  }
}
