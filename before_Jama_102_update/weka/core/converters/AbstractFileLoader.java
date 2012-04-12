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
 * AbstractFileLoader.java
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.core.converters;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Abstract superclass for all file loaders.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public abstract class AbstractFileLoader
  extends AbstractLoader
  implements FileSourcedConverter {

  /** the file */
  protected String m_File = (new File(System.getProperty("user.dir"))).getAbsolutePath();

  /** Holds the determined structure (header) of the data set. */
  protected Instances m_structure = null;

  /** Holds the source of the data set. */
  protected File m_sourceFile = null;

  /**
   * get the File specified as the source
   *
   * @return the source file
   */
  public File retrieveFile() {
    return new File(m_File);
  }

  /**
   * sets the source File
   *
   * @param file the source file
   * @exception IOException if an error occurs
   */
  public void setFile(File file) throws IOException {
    m_structure = null;
    setRetrieval(NONE);

    m_File = file.getAbsolutePath();
    setSource(file);
  }
  
  /**
   * Resets the loader ready to read a new data set
   * 
   * @throws IOException if something goes wrong
   */
  public void reset() throws IOException {
    m_structure = null;
    setRetrieval(NONE);
  }

  /**
   * Resets the Loader object and sets the source of the data set to be 
   * the supplied File object.
   *
   * @param file the source file.
   * @exception IOException if an error occurs
   */
  public void setSource(File file) throws IOException {
    m_structure = null;
    setRetrieval(NONE);

    if (file == null) {
      throw new IOException("Source file object is null!");
    }

    try {
      setSource(new FileInputStream(file));
    }
    catch (FileNotFoundException ex) {
      throw new IOException("File not found");
    }

    m_sourceFile = file;
    m_File       = file.getAbsolutePath();
  }

  /**
   * generates a string suitable for output on the command line displaying
   * all available options (currently only a simple usage).
   * 
   * @param loader	the loader to create the option string for
   * @return		the option string
   */
  protected static String makeOptionStr(AbstractFileLoader loader) {
    String 	result;
    
    result = "\nUsage:\n";
    result += "\t" + loader.getClass().getName().replaceAll(".*\\.", "");
    result += " <";
    String[] ext = loader.getFileExtensions();
    for (int i = 0; i < ext.length; i++) {
	if (i > 0)
	  result += " | ";
	result += "file" + ext[i];
    }
    result += ">\n";
    
    return result;
  }
  
  /**
   * runs the given loader with the provided options
   * 
   * @param loader	the loader to run
   * @param options	the commandline options, first argument must be the
   * 			file to load
   */
  public static void runFileLoader(AbstractFileLoader loader, String[] options) {
    // help request?
    try {
      String[] tmpOptions = (String[]) options.clone();
      if (Utils.getFlag('h', tmpOptions)) {
	System.err.println("\nHelp requested\n" + makeOptionStr(loader));
	return;
      }
    }
    catch (Exception e) {
      // ignore it
    }
    
    if (options.length > 0) {
      try {
	loader.setFile(new File(options[0]));
	// incremental
	if (loader instanceof IncrementalConverter) {
	  Instances structure = loader.getStructure();
	  System.out.println(structure);
	  Instance temp;
	  do {
	    temp = loader.getNextInstance(structure);
	    if (temp != null)
	      System.out.println(temp);
	  }
	  while (temp != null);
	}
	// batch
	else {
	  System.out.println(loader.getDataSet());
	}
      }
      catch (Exception ex) {
	ex.printStackTrace();
      }
    }
    else {
      System.err.println(makeOptionStr(loader));
    }
  }
}
