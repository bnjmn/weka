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
 *    ConverterUtils.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.core.converters;

import weka.core.ClassDiscovery;
import weka.gui.GenericPropertiesCreator;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

/**
 * Utility routines for the converter package.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.5 $
 * @see Serializable
 */
public class ConverterUtils
  implements Serializable {

  /** for serialization */
  static final long serialVersionUID = -2460855349276148760L;
  
  /** all available loaders (extension &lt;-&gt; classname) */
  protected static Hashtable<String,String> m_FileLoaders;
  
  /** all available URL loaders (extension &lt;-&gt; classname) */
  protected static Hashtable<String,String> m_URLFileLoaders;

  /** all available savers (extension &lt;-&gt; classname) */
  protected static Hashtable<String,String> m_FileSavers;
  
  // determine all loaders/savers
  static {
    Vector classnames;
    
    try {
      GenericPropertiesCreator creator = new GenericPropertiesCreator();
      creator.execute(false);
      Properties props = creator.getOutputProperties();
      
      // loaders
      m_FileLoaders = getFileConverters(
	  		props.getProperty(Loader.class.getName(), ""),
	  		new String[]{FileSourcedConverter.class.getName()});
      
      // URL loaders
      m_URLFileLoaders = getFileConverters(
	  		   props.getProperty(Loader.class.getName(), ""),
	  		   new String[]{
	  		     FileSourcedConverter.class.getName(), 
	  		     URLSourcedLoader.class.getName()});

      // savers
      m_FileSavers = getFileConverters(
	  		props.getProperty(Saver.class.getName(), ""),
	  		new String[]{FileSourcedConverter.class.getName()});
    }
    catch (Exception e) {
      // loaders
      classnames = ClassDiscovery.find(
		AbstractFileLoader.class, 
		AbstractFileLoader.class.getPackage().getName());
      m_FileLoaders = getFileConverters(
	  		classnames,
	  		new String[]{FileSourcedConverter.class.getName()});

      // URL loaders
      classnames = ClassDiscovery.find(
		AbstractFileLoader.class, 
		AbstractFileLoader.class.getPackage().getName());
      m_URLFileLoaders = getFileConverters(
	  		   classnames,
	  		   new String[]{
	  		       FileSourcedConverter.class.getName(), 
	  		       URLSourcedLoader.class.getName()});

      // savers
      classnames = ClassDiscovery.find(
		AbstractFileSaver.class, 
		AbstractFileSaver.class.getPackage().getName());
      m_FileSavers = getFileConverters(
	  		classnames,
	  		new String[]{FileSourcedConverter.class.getName()});
    }
  }
  
  /**
   * returns a hashtable with the association 
   * "file extension &lt;-&gt; converter classname" for the comma-separated list
   * of converter classnames.
   * 
   * @param classnames	comma-separated list of converter classnames
   * @param intf	interfaces the converters have to implement
   * @return		hashtable with ExtensionFileFilters
   */
  protected static Hashtable<String,String> getFileConverters(String classnames, String[] intf) {
    Vector	list;
    String[]	names;
    int		i;
    
    list  = new Vector();
    names = classnames.split(",");
    for (i = 0; i < names.length; i++)
      list.add(names[i]);
    
    return getFileConverters(list, intf);
  }
  
  /**
   * returns a hashtable with the association 
   * "file extension &lt;-&gt; converter classname" for the list of converter 
   * classnames.
   * 
   * @param classnames	list of converter classnames
   * @param intf	interfaces the converters have to implement
   * @return		hashtable with ExtensionFileFilters
   */
  protected static Hashtable<String,String> getFileConverters(Vector classnames, String[] intf) {
    Hashtable<String,String>	result;
    String 			classname;
    Class 			cls;
    String[] 			ext;
    FileSourcedConverter 	converter;
    int 			i;
    int				n;
    
    result = new Hashtable<String,String>();
    
    for (i = 0; i < classnames.size(); i++) {
      classname = (String) classnames.get(i);

      // all necessary interfaces implemented?
      for (n = 0; n < intf.length; n++) {
	if (!ClassDiscovery.hasInterface(intf[n], classname))
	  continue;
      }
      
      // get data from converter
      try {
	cls       = Class.forName(classname);
	converter = (FileSourcedConverter) cls.newInstance();
	ext       = converter.getFileExtensions();
      }
      catch (Exception e) {
	cls       = null;
	converter = null;
	ext       = new String[0];
      }
      
      if (converter == null)
	continue;

      for (n = 0; n < ext.length; n++)
	result.put(ext[n], classname);
    }
    
    return result;
  }

  /**
   * Gets token, skipping empty lines.
   *
   * @param tokenizer 		the stream tokenizer
   * @throws IOException 	if reading the next token fails
   */
  public static void getFirstToken(StreamTokenizer tokenizer) 
    throws IOException {
    
    while (tokenizer.nextToken() == StreamTokenizer.TT_EOL){};
    if ((tokenizer.ttype == '\'') ||
	(tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    } else if ((tokenizer.ttype == StreamTokenizer.TT_WORD) &&
	       (tokenizer.sval.equals("?"))) {
      tokenizer.ttype = '?';
    }
  }

  /**
   * Gets token.
   *
   * @param tokenizer 		the stream tokenizer
   * @throws IOException 	if reading the next token fails
   */
  public static void getToken(StreamTokenizer tokenizer) throws IOException {
    
    tokenizer.nextToken();
    if (tokenizer.ttype== StreamTokenizer.TT_EOL) {
      return;
    }

    if ((tokenizer.ttype == '\'') ||
	(tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    } else if ((tokenizer.ttype == StreamTokenizer.TT_WORD) &&
	       (tokenizer.sval.equals("?"))) {
      tokenizer.ttype = '?';
    }
  }

  /**
   * Throws error message with line number and last token read.
   *
   * @param theMsg 		the error message to be thrown
   * @param tokenizer 		the stream tokenizer
   * @throws IOExcpetion 	containing the error message
   */
  public static void errms(StreamTokenizer tokenizer, String theMsg) 
    throws IOException {
    
    throw new IOException(theMsg + ", read " + tokenizer.toString());
  }

  /**
   * returns a vector with the classnames of all the loaders from the 
   * given hashtable
   * 
   * @param ht		the hashtable with the extension/converter relation
   * @return		the classnames of the loaders
   */
  protected static Vector<String> getConverters(Hashtable<String,String> ht) {
    Vector<String>	result;
    Enumeration<String>	enm;
    String		converter;
    
    result = new Vector<String>();
    
    // get all classnames
    enm = ht.elements();
    while (enm.hasMoreElements()) {
      converter = enm.nextElement();
      if (!result.contains(converter))
	result.add(converter);
    }
    
    // sort names
    Collections.sort(result);
    
    return result;
  }
  
  /**
   * tries to determine the converter to use for this kind of file, returns
   * null if none can be found in the given hashtable
   * 
   * @param filename	the file to return a converter for
   * @param ht		the hashtable with the relation extension/converter
   * @return		the converter if one was found, null otherwise
   */
  protected static Object getConverterForFile(String filename, Hashtable<String,String> ht) {
    Object	result;
    String	extension;
    
    result = null;
    
    if (filename.lastIndexOf('.') > -1) {
      extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
      result    = getConverterForExtension(extension, ht);
    }
    
    return result;
  }

  /**
   * tries to determine the loader to use for this kind of extension, returns
   * null if none can be found
   * 
   * @param extension	the file extension to return a converter for
   * @param ht		the hashtable with the relation extension/converter
   * @return		the converter if one was found, null otherwise
   */
  protected static Object getConverterForExtension(String extension, Hashtable<String,String> ht) {
    Object	result;
    String	classname;
    
    classname = (String) ht.get(extension);
    try {
      result = Class.forName(classname).newInstance();
    }
    catch (Exception e) {
      result = null;
      e.printStackTrace();
    }
    
    return result;
  }
  
  /**
   * returns a vector with the classnames of all the file loaders
   * 
   * @return		the classnames of the loaders
   */
  public static Vector<String> getFileLoaders() {
    return getConverters(m_FileLoaders);
  }
  
  /**
   * tries to determine the loader to use for this kind of file, returns
   * null if none can be found
   * 
   * @param filename	the file to return a converter for
   * @return		the converter if one was found, null otherwise
   */
  public static AbstractFileLoader getLoaderForFile(String filename) {
    return (AbstractFileLoader) getConverterForFile(filename, m_FileLoaders);
  }

  /**
   * tries to determine the loader to use for this kind of file, returns
   * null if none can be found
   * 
   * @param file	the file to return a converter for
   * @return		the converter if one was found, null otherwise
   */
  public static AbstractFileLoader getLoaderForFile(File file) {
    return getLoaderForFile(file.getAbsolutePath());
  }

  /**
   * tries to determine the loader to use for this kind of extension, returns
   * null if none can be found
   * 
   * @param extension	the file extension to return a converter for
   * @return		the converter if one was found, null otherwise
   */
  public static AbstractFileLoader getLoaderForExtension(String extension) {
    return (AbstractFileLoader) getConverterForExtension(extension, m_FileLoaders);
  }

  /**
   * returns a vector with the classnames of all the URL file loaders
   * 
   * @return		the classnames of the loaders
   */
  public static Vector<String> getURLFileLoaders() {
    return getConverters(m_URLFileLoaders);
  }
  
  /**
   * tries to determine the URL loader to use for this kind of file, returns
   * null if none can be found
   * 
   * @param filename	the file to return a URL converter for
   * @return		the converter if one was found, null otherwise
   */
  public static AbstractFileLoader getURLLoaderForFile(String filename) {
    return (AbstractFileLoader) getConverterForFile(filename, m_URLFileLoaders);
  }

  /**
   * tries to determine the URL loader to use for this kind of file, returns
   * null if none can be found
   * 
   * @param file	the file to return a URL converter for
   * @return		the converter if one was found, null otherwise
   */
  public static AbstractFileLoader getURLLoaderForFile(File file) {
    return getURLLoaderForFile(file.getAbsolutePath());
  }

  /**
   * tries to determine the URL loader to use for this kind of extension, returns
   * null if none can be found
   * 
   * @param extension	the file extension to return a URL converter for
   * @return		the converter if one was found, null otherwise
   */
  public static AbstractFileLoader getURLLoaderForExtension(String extension) {
    return (AbstractFileLoader) getConverterForExtension(extension, m_URLFileLoaders);
  }

  /**
   * returns a vector with the classnames of all the file savers
   * 
   * @return		the classnames of the savers
   */
  public static Vector<String> getFileSavers() {
    return getConverters(m_FileSavers);
  }

  /**
   * tries to determine the saver to use for this kind of file, returns
   * null if none can be found
   * 
   * @param filename	the file to return a converter for
   * @return		the converter if one was found, null otherwise
   */
  public static AbstractFileSaver getSaverForFile(String filename) {
    return (AbstractFileSaver) getConverterForFile(filename, m_FileSavers);
  }

  /**
   * tries to determine the saver to use for this kind of file, returns
   * null if none can be found
   * 
   * @param file	the file to return a converter for
   * @return		the converter if one was found, null otherwise
   */
  public static AbstractFileSaver getSaverForFile(File file) {
    return getSaverForFile(file.getAbsolutePath());
  }

  /**
   * tries to determine the saver to use for this kind of extension, returns
   * null if none can be found
   * 
   * @param extension	the file extension to return a converter for
   * @return		the converter if one was found, null otherwise
   */
  public static AbstractFileSaver getSaverForExtension(String extension) {
    return (AbstractFileSaver) getConverterForExtension(extension, m_FileSavers);
  }
}
