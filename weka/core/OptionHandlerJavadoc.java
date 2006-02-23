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
 * OptionHandlerJavadoc.java
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Generates Javadoc comments from the OptionHandler's options. Can 
 * automatically update the option comments if they're surrounded by
 * the OPTIONS_STARTTAG and OPTIONS_ENDTAG (the indention is determined via
 * the OPTIONS_STARTTAG). <p/>
 * 
 * Valid options are: <p/>
 * 
 * -W &lt;classname&gt; <br/>
 *  The option handler to retrieve the options from. <p/>
 * 
 * -nostars <br/>
 *  Suppresses the '*' in the Javadoc. <p/>
 * 
 * -noprolog <br/>
 *  Suppresses the 'Valid options are...' prolog in the Javadoc. <p/>
 * 
 * -dir &lt;dir&gt; <br/>
 *  The directory above the package hierarchy of the class. <p/>
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 * @see #OPTIONS_STARTTAG
 * @see #OPTIONS_ENDTAG
 */
public class OptionHandlerJavadoc 
  extends Javadoc {
  
  /** the start comment tag for inserting the generated Javadoc */
  public final static String OPTIONS_STARTTAG = "<!-- options-start -->";
  
  /** the end comment tag for inserting the generated Javadoc */
  public final static String OPTIONS_ENDTAG = "<!-- options-end -->";
  
  /** whether to include the "Valid options..." prolog in the Javadoc */
  protected boolean m_Prolog = true;
  
  /**
   * default constructor 
   */
  public OptionHandlerJavadoc() {
    super();
    
    m_StartTag    = new String[1];
    m_EndTag      = new String[1];
    m_StartTag[0] = OPTIONS_STARTTAG;
    m_EndTag[0]   = OPTIONS_ENDTAG;
  }
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector        result;
    Enumeration   en;
    
    result = new Vector();
    
    en = super.listOptions();
    while (en.hasMoreElements())
      result.addElement(en.nextElement());

    result.addElement(new Option(
        "\tSuppresses the 'Valid options are...' prolog in the Javadoc.",
        "noprolog", 0, "-noprolog"));
    
    return result.elements();
  }
  
  /**
   * Parses a given list of options. 
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    super.setOptions(options);

    setProlog(!Utils.getFlag("noprolog", options));
  }
  
  /**
   * Gets the current settings of this object.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    String[]      options;
    int           i;
    
    result  = new Vector();
    
    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    if (!getProlog())
      result.add("-noprolog");
    
    return (String[]) result.toArray(new String[result.size()]);
  }
  
  /**
   * sets whether to add the "Valid options are..." prolog
   */
  public void setProlog(boolean value) {
    m_Prolog = value;
  }
  
  /**
   * whether "Valid options are..." prolog is included in the Javadoc
   */
  public boolean getProlog() {
    return m_Prolog;
  }
  
  /**
   * generates and returns the Javadoc for the specified start/end tag pair.
   * 
   * @param index	the index in the start/end tag array
   * @return		the generated Javadoc
   * @throws Exception 	in case the generation fails
   */
  protected String generateJavadoc(int index) throws Exception {
    String		result;
    Class		cls;
    OptionHandler	handler;
    
    result = "";
    
    if (index == 0) {
      cls = Class.forName(getClassname());
      if (!ClassDiscovery.hasInterface(OptionHandler.class, cls))
	throw new Exception("Class '" + getClassname() + "' is not an OptionHandler!");
      
      // prolog?
      if (getProlog())
	result = "Valid options are: <p/>\n\n";
      
      // options
      handler = (OptionHandler) cls.newInstance();
      Enumeration enm = handler.listOptions();
      while (enm.hasMoreElements()) {
	Option option = (Option) enm.nextElement();
	result +=   toHTML(option.synopsis()) 
	          + " <br/>\n" 
	          + toHTML(option.description().replaceAll("\\t", " ")) 
	          + " <p/>\n\n";
      }
      
      // stars?
      if (getUseStars()) 
	result = indent(result, 1, "* ");
    }
    
    return result;
  }
  
  /**
   * Parses the given commandline parameters and generates the Javadoc.
   * 
   * @param args	the commandline parameters for the object
   */
  public static void main(String[] args) {
    try {
      Javadoc doc = new OptionHandlerJavadoc();
      
      try {
	if (Utils.getFlag('h', args))
	  throw new Exception("Help requested");

	doc.setOptions(args);
        Utils.checkForRemainingOptions(args);
      } 
      catch (Exception ex) {
        String result = "\n" + ex.getMessage() + "\n\n" + doc.generateHelp();
        throw new Exception(result);
      }

      System.out.println(doc.generate() + "\n");
    } 
    catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }
}
