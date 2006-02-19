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
 * GlobalInfoJavadoc.java
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Generates Javadoc comments from the class's globalInfo method. Can 
 * automatically update the comments if they're surrounded by
 * the GLOBALINFO_STARTTAG and GLOBALINFO_ENDTAG (the indention is determined via
 * the GLOBALINFO_STARTTAG). <p/>
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * -W &lt;classname&gt; <br/>
 *  The class to retrieve the global info from. <p/>
 * 
 * -nostars <br/>
 *  Suppresses the '*' in the Javadoc. <p/>
 * 
 * -file &lt;file&gt; <br/>
 *  The file to update the Javadoc for. <p/>
 * 
 <!-- options-end -->
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 * @see #GLOBALINFO_METHOD
 * @see #GLOBALINFO_STARTTAG
 * @see #GLOBALINFO_ENDTAG
 */
public class GlobalInfoJavadoc 
  implements OptionHandler {
  
  /** the globalInfo method name */
  public final static String GLOBALINFO_METHOD = "globalInfo";
  
  /** the start comment tag for inserting the generated Javadoc */
  public final static String GLOBALINFO_STARTTAG = "<!-- globalinfo-start -->";
  
  /** the end comment tag for inserting the generated Javadoc */
  public final static String GLOBALINFO_ENDTAG = "<!-- globalinfo-end -->";

  /** the classname */
  protected String m_Classname = OptionHandlerJavadoc.class.getName();
  
  /** whether to include the stars in the Javadoc */
  protected boolean m_UseStars = true;

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
        "\tThe class to retrieve the global info from.",
        "W", 1, "-W <classname>"));
    
    result.addElement(new Option(
        "\tSuppresses the '*' in the Javadoc.",
        "nostars", 0, "-nostars"));
    
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
    
    if (getFilename().length() != 0) {
      result.add("-file");
      result.add(getFilename());
    }
    
    return (String[]) result.toArray(new String[result.size()]);
  }
  
  /**
   * sets the classname of the class to generate the Javadoc for
   */
  public void setClassname(String value) {
    m_Classname = value;
  }
  
  /**
   * returns the current classname
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
    result = result.replaceAll("\n", "<br/>\n");
    
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
    Object		obj;
    Method		method;
    
    result = "";
    
    cls = Class.forName(getClassname());
    try {
      method = cls.getMethod(GLOBALINFO_METHOD, (Class[]) null);
    }
    catch (Exception e) {
      // no method "globalInfo"
      return result;
    }
    
    // retrieve global info
    obj    = cls.newInstance();
    result = toHTML((String) method.invoke(obj, (Object[]) null));
    result = result.trim() + "\n<p/>\n";

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
   * @see #GLOBALINFO_STARTTAG
   * @see #GLOBALINFO_ENDTAG
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
      if (    (content.indexOf(GLOBALINFO_STARTTAG) == -1)
	   || (content.indexOf(GLOBALINFO_STARTTAG) == -1) ) {
	System.err.println("No globalinfo start and/or end tags found!");
	return content;
      }

      // replace option-tags
      contentBufNew = new StringBuffer();
      while (content.length() > 0) {
	if (content.indexOf(GLOBALINFO_STARTTAG) > -1) {
	  part      = content.substring(0, content.indexOf(GLOBALINFO_STARTTAG));
	  indention = part.length() - part.lastIndexOf("\n") - 1;
	  part      = part.substring(0, part.length() - indention);
	  contentBufNew.append(part);
	  contentBufNew.append(indent(GLOBALINFO_STARTTAG, indention, " ") + "\n");
	  contentBufNew.append(indent(javadoc, indention, " "));
	  contentBufNew.append(indent(GLOBALINFO_ENDTAG, indention, " "));
	  content = content.substring(content.indexOf(GLOBALINFO_ENDTAG));
	  content = content.substring(GLOBALINFO_ENDTAG.length());
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
   * the global info have to be specified in the file in the latter case.
   * 
   * @return 		either the plain Javadoc or the modified file
   * @throws Exception 	in case the generation fails
   * @see #GLOBALINFO_STARTTAG
   * @see #GLOBALINFO_ENDTAG
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
      GlobalInfoJavadoc doc = new GlobalInfoJavadoc();
      
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
