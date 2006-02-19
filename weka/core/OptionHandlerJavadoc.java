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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.StringTokenizer;
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
 * -file &lt;file&gt; <br/>
 *  The file to update the Javadoc for. <p/>
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 * @see #OPTIONS_STARTTAG
 * @see #OPTIONS_ENDTAG
 */
public class OptionHandlerJavadoc 
  implements OptionHandler {
  
  /** the start comment tag for inserting the generated Javadoc */
  public final static String OPTIONS_STARTTAG = "<!-- options-start -->";
  
  /** the end comment tag for inserting the generated Javadoc */
  public final static String OPTIONS_ENDTAG = "<!-- options-end -->";

  /** the optionhandler's classname */
  protected String m_Classname = OptionHandler.class.getName();
  
  /** whether to include the stars in the Javadoc */
  protected boolean m_UseStars = true;
  
  /** whether to include the "Valid options..." prolog in the Javadoc */
  protected boolean m_Prolog = true;

  /** the file to update */
  protected String m_Filename = "";
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();

    result.addElement(new Option(
        "\tThe option handler to retrieve the options from.",
        "W", 1, "-W <classname>"));
    
    result.addElement(new Option(
        "\tSuppresses the '*' in the Javadoc.",
        "nostars", 0, "-nostars"));
    
    result.addElement(new Option(
        "\tSuppresses the 'Valid options are...' prolog in the Javadoc.",
        "noprolog", 0, "-noprolog"));
    
    result.addElement(new Option(
        "\tThe file to update the Javadoc for.",
        "file", 1, "-file <file>"));
    
    return result.elements();
  }
  
  /**
   * Parses a given list of options. 
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String      		tmpStr;
    
    tmpStr = Utils.getOption('W', options);
    if (tmpStr.length() > 0)
      setClassname(tmpStr);
    else
      setClassname(this.getClass().getName());

    setUseStars(!Utils.getFlag("nostars", options));

    setProlog(!Utils.getFlag("noprolog", options));

    setFilename(Utils.getOption("file", options));
  }
  
  /**
   * Gets the current settings of this object.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector 	result;

    result = new Vector();
    
    result.add("-W");
    result.add(getClassname());
    
    if (!getUseStars())
      result.add("-nostars");
    
    if (!getProlog())
      result.add("-noprolog");
    
    if (getFilename().length() != 0) {
      result.add("-file");
      result.add(getFilename());
    }
    
    return (String[]) result.toArray(new String[result.size()]);
  }
  
  /**
   * sets the classname of the optionhandler to generate the Javadoc for
   */
  public void setClassname(String value) {
    m_Classname = value;
  }
  
  /**
   * returns the current classname of the optionhandler
   */
  public String getClassname() {
    return m_Classname;
  }
  
  /**
   * sets whether to prefix the Javadoc with "*"
   */
  public void setUseStars(boolean value) {
    m_UseStars = value;
  }
  
  /**
   * whether the Javadoc is prefixed with "*"
   */
  public boolean getUseStars() {
    return m_UseStars;
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
   * sets the file that is to be updated
   */
  public void setFilename(String value) {
    m_Filename = value;
  }
  
  /**
   * returns the current file to update
   */
  public String getFilename() {
    return m_Filename;
  }

  /**
   * converts the given String into HTML, i.e., replacing some char entities
   * with HTML entities.
   * 
   * @param s		the string to convert
   * @return		the HTML conform string
   */
  protected String toHTML(String s) {
    String	result;
    
    result = s;
    
    result = result.replaceAll("&", "&amp;");
    result = result.replaceAll("<", "&lt;");
    result = result.replaceAll(">", "&gt;");
    
    return result;
  }

  /**
   * indents the given string by a given number of indention strings
   * 
   * @param content	the string to indent
   * @param count	the number of times to indent one line
   * @param indentStr	the indention string
   */
  protected String indent(String content, int count, String indentStr) {
    String		result;
    StringTokenizer	tok;
    int			i;
    
    tok = new StringTokenizer(content, "\n", true);
    result = "";
    while (tok.hasMoreTokens()) {
      if (result.endsWith("\n") || (result.length() == 0)) {
	for (i = 0; i < count; i++)
	  result += indentStr;
      }
      result += tok.nextToken();
    }
    
    return result;
  }
  
  /**
   * generates and returns the Javadoc
   * 
   * @return		the generated Javadoc
   * @throws Exception 	in case the generation fails
   */
  protected String generateJavadoc() throws Exception {
    String		result;
    Class		cls;
    OptionHandler	handler;
    
    result = "";
    
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
    
    return result;
  }

  /**
   * updates the specified file and inserts the generated Javadoc between
   * the start and end tag
   * 
   * @return		true if the update was successful
   * @throws Exception 	in case the generation fails
   * @see #OPTIONS_STARTTAG
   * @see #OPTIONS_ENDTAG
   */
  protected String update(String javadoc) throws Exception {
    File		file;
    StringBuffer	contentBuf;
    StringBuffer	contentBufNew;
    String		content;
    String		line;
    BufferedReader	reader;
    int			indention;
    String		part;
    
    contentBufNew = new StringBuffer(javadoc);
    
    // non-existing?
    file = new File(getFilename());
    if (!file.exists()) {
      System.err.println("File '" + getFilename() + "' doesn't exist!");
      return contentBufNew.toString();
    }
    
    try {
      // load file
      reader     = new BufferedReader(new FileReader(file));
      contentBuf = new StringBuffer();
      while ((line = reader.readLine()) != null) {
	contentBuf.append(line + "\n");
      }
      reader.close();
      content = contentBuf.toString();
      
      // start and end tag?
      if (    (content.indexOf(OPTIONS_STARTTAG) == -1)
	   || (content.indexOf(OPTIONS_STARTTAG) == -1) ) {
	System.err.println("No options start and/or end tags found!");
	return content;
      }

      // replace option-tags
      contentBufNew = new StringBuffer();
      while (content.length() > 0) {
	if (content.indexOf(OPTIONS_STARTTAG) > -1) {
	  part      = content.substring(0, content.indexOf(OPTIONS_STARTTAG));
	  indention = part.length() - part.lastIndexOf("\n") - 1;
	  part      = part.substring(0, part.length() - indention);
	  contentBufNew.append(part);
	  contentBufNew.append(indent(OPTIONS_STARTTAG, indention, " ") + "\n");
	  contentBufNew.append(indent(javadoc, indention, " "));
	  contentBufNew.append(indent(OPTIONS_ENDTAG, indention, " "));
	  content = content.substring(content.indexOf(OPTIONS_ENDTAG));
	  content = content.substring(OPTIONS_ENDTAG.length());
	}
	else {
	  contentBufNew.append(content);
	  content = "";
	}
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      return contentBufNew.toString();
    }
    
    return contentBufNew.toString();
  }
  
  /**
   * generates either the plain Javadoc (if no filename specified) or the
   * updated file (if a filename is specified). The start and end tag for
   * the options have to be specified in the file in the latter case.
   * 
   * @return 		either the plain Javadoc or the modified file
   * @throws Exception 	in case the generation fails
   * @see #OPTIONS_STARTTAG
   * @see #OPTIONS_ENDTAG
   */
  public String generate() throws Exception {
    if (getFilename().length() == 0)
      return generateJavadoc();
    else
      return update(generateJavadoc());
  }
  
  /**
   * Parses the given commandline parameters and generates the Javadoc.
   * 
   * @param args	the commandline parameters for the object
   */
  public static void main(String[] args) {
    try {
      OptionHandlerJavadoc doc = new OptionHandlerJavadoc();
      
      try {
        doc.setOptions(args);
        Utils.checkForRemainingOptions(args);
      } 
      catch (Exception ex) {
        String result = ex.getMessage() + "\n\n" + doc.getClass().getName().replaceAll(".*\\.", "") + " Options:\n\n";
        Enumeration enm = doc.listOptions();
        while (enm.hasMoreElements()) {
          Option option = (Option) enm.nextElement();
          result += option.synopsis() + "\n" + option.description() + "\n";
        }
        throw new Exception(result);
      }

      System.out.println(doc.generate());
    } 
    catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }
}
