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
 *    GenericPropertiesCreator.java
 *    Copyright (C) 2005 FracPete
 *
 */
package weka.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import weka.core.RTSI;

/**
 * This class can generate the properties object that is normally loaded from
 * the <code>GenericObjectEditor.props</code> file (= PROPERTY_FILE). It takes
 * the <code>GenericPropertiesCreator.props</code> file as a template to
 * determine all the derived classes by checking the classes in the given
 * packages. <br>
 * E.g. if we want to have all the subclasses of the <code>Classifier</code>
 * class then we specify the superclass ("weka.classifiers.Classifier") and the
 * packages where to look for ("weka.classifiers.bayes" etc.):
 * 
 * <pre>
 * 
 *   weka.classifiers.Classifier=\
 *     weka.classifiers.bayes,\
 *     weka.classifiers.functions,\
 *     weka.classifiers.lazy,\
 *     weka.classifiers.meta,\
 *     weka.classifiers.trees,\
 *     weka.classifiers.rules
 *  
 * </pre>
 * 
 * This creates the same list as stored in the
 * <code>GenericObjectEditor.props</code> file, but it will also add
 * additional classes, that are not listed in the static list (e.g. a newly
 * developed Classifier), but still in the classpath. <br>
 * <br>
 * For discovering the subclasses the whole classpath is inspected, which means
 * that you can have several parallel directories with the same package
 * structure (e.g. a release directory and a developer directory with additional
 * classes). <br>
 * <br>
 * Code used and adapted from the following JavaWorld Tips:
 * <ul>
 *    <li><a href="http://www.javaworld.com/javaworld/javatips/jw-javatip113.html" target="_blank">Tip 113 </a>: Identify subclasses at runtime</li>
 *    <li><a href="http://www.javaworld.com/javaworld/javatips/jw-javatip105.html" target="_blank">Tip 105 </a>: Mastering the classpath with JWhich</li>
 * </ul>
 * 
 * @see #CREATOR_FILE
 * @see #PROPERTY_FILE
 * @see GenericObjectEditor
 * @see weka.core.RTSI
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1.2.1 $
 */
public class GenericPropertiesCreator {
  /** whether to output some debug information */
  public final static boolean VERBOSE = false;
  
  /** The name of the properties file to use as a template. Ccontains the 
   * packages in which to look for derived classes. It has the same structure
   * as the <code>PROPERTY_FILE</code>
   * @see #PROPERTY_FILE
   */
  protected static String CREATOR_FILE = "weka/gui/GenericPropertiesCreator.props";
  
  /** The name of the properties file for the static GenericObjectEditor 
   * (<code>USE_DYNAMIC</code> = <code>false</code>) 
   * @see GenericObjectEditor 
   * @see GenericObjectEditor#USE_DYNAMIC 
   */
  protected static String PROPERTY_FILE = "weka/gui/GenericObjectEditor.props";
  
  /** the input file with the packages */
  protected String inputFilename;
  
  /** the output props file for the GenericObjectEditor */
  protected String outputFilename;
  
  /** the "template" properties file with the layout and the packages */
  protected Properties inputProperties;
  
  /** the output properties file with the filled in classes */
  protected Properties outputProperties;
  
  /**
   * initializes the creator  
   * 
   * @throws Exception if loading of CREATOR_FILE fails
   * @see #CREATOR_FILE
   */
  public GenericPropertiesCreator() throws Exception {
    this(CREATOR_FILE);
  }

  /**
   * initializes the creator  
   * 
   * @param filename the file containing the packages to create a props file from
   * @throws Exception if loading of the file fails
   * @see #CREATOR_FILE
   */
  public GenericPropertiesCreator(String filename) throws Exception {
    super();
    inputFilename    = filename;
    outputFilename   = PROPERTY_FILE;
    inputProperties  = null;
    outputProperties = null;
  }

  /**
   * returns the name of the output file
   * 
   * @return the name of the output file 
   */
  public String getOutputFilename() {
    return outputFilename;
  }
  
  /**
   * sets the file to output the properties for the GEO to
   * 
   * @param filename the filename for the output 
   */
  public void setOutputFilename(String filename) {
    outputFilename = filename;
  }

  /**
   * returns the name of the input file
   * 
   * @return the name of the input file 
   */
  public String getInputFilename() {
    return inputFilename;
  }
  
  /**
   * sets the file to get the information about the packages from
   * 
   * @param filename the filename for the input 
   */
  public void setInputFilename(String filename) {
    inputFilename = filename;
  }
  
  /**
   * returns the input properties object (template containing the packages)
   */
  public Properties getInputProperties() {
    return inputProperties;
  }
  
  /**
   * returns the output properties object (structure like the template, but
   * filled with classes instead of packages)
   */
  public Properties getOutputProperties() {
    return outputProperties;
  }
  
  /**
   * loads the property file containing the layout and the packages of 
   * the output-property-file
   * 
   * @see #inputProperties
   * @see #inputFilename
   */
  protected void loadInputProperties() throws Exception {
    if (VERBOSE)
      System.out.println("Loading '" + getInputFilename() + "'...");
    inputProperties = new Properties();
    try {
      File f = new File(getInputFilename());
      if (f.exists())
        inputProperties.load(new FileInputStream(getInputFilename()));
      else
        inputProperties.load(ClassLoader.getSystemResourceAsStream(getInputFilename()));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * fills in all the classes (based on the packages in the input properties 
   * file) into the output properties file
   *     
   * @see #outputProperties
   */
  protected void generateOutputProperties() throws Exception {
    Enumeration       keys;
    String            key;
    String            value;
    String            pkg;
    StringTokenizer   tok;
    Vector            classes;
    int               i;
    
    outputProperties = new Properties();
    keys             = inputProperties.propertyNames();
    while (keys.hasMoreElements()) {
      key   = keys.nextElement().toString();
      tok   = new StringTokenizer(inputProperties.getProperty(key), ",");
      value = "";
      // get classes for all packages
      while (tok.hasMoreTokens()) {
        pkg     = tok.nextToken().trim();
        classes = RTSI.find(pkg, Class.forName(key));
        for (i = 0; i < classes.size(); i++) {
          if (!value.equals(""))
            value += ",";
          value += classes.get(i).toString();
        }
        if (VERBOSE)
          System.out.println(pkg + " -> " + value);
      }
      outputProperties.setProperty(key, value);
    }
  }
  
  /**
   * stores the generated output properties file
   * 
   * @see #outputProperties
   * @see #outputFilename 
   */
  protected void storeOutputProperties() throws Exception {
    if (VERBOSE)
      System.out.println("Saving '" + getOutputFilename() + "'...");
    outputProperties.store(
        new FileOutputStream(getOutputFilename()), 
        " Customises the list of options given by the GenericObjectEditor\n# for various superclasses.");
  }
  
  /**
   * generates the props-file for the GenericObjectEditor and stores it
   * 
   * @see #execute(boolean)
   */
  public void execute() throws Exception {
    execute(true);
  }
  
  /**
   * generates the props-file for the GenericObjectEditor and stores it only
   * if the the param <code>store</code> is TRUE. If it is FALSE then the
   * generated properties file can be retrieved via the <code>getOutputProperties</code>
   * method. 
   * 
   * @param store     if TRUE then the properties file is stored to the stored 
   *                  filename
   * @see #getOutputFilename()
   * @see #setOutputFilename(String)
   * @see #getOutputProperties()
   */
  public void execute(boolean store) throws Exception {
    // read properties file
    loadInputProperties();
    
    // generate the props file
    generateOutputProperties();
    
    // write properties file
    if (store)
      storeOutputProperties();
  }
  
  /**
   * for executing the  
   */
  public static void main(String[] args) throws Exception {
    GenericPropertiesCreator   c = null;
    
    if (args.length == 0) {
      c = new GenericPropertiesCreator();
    }
    else if (args.length == 1) {
      c = new GenericPropertiesCreator();
      c.setOutputFilename(args[0]);
    }
    else if (args.length == 2) {
      c = new GenericPropertiesCreator(args[0]);
      c.setOutputFilename(args[1]);
    }
    else {
      System.out.println("usage: " + GenericPropertiesCreator.class.getName() + " [<input.props>] [<output.props>]");
      System.exit(1);
    }
    
    c.execute(true);
  }
}
