/*
 *    ExtensionFileFilter.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

import java.io.File;
import java.io.FilenameFilter;
import javax.swing.filechooser.FileFilter;

/**
 * Provides a file filter for FileChoosers that accepts or rejects files
 * based on their extension. Compatible with both java.io.FilenameFilter and
 * javax.swing.filechooser.FileFilter (why there are two I have no idea).
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class ExtensionFileFilter extends FileFilter implements FilenameFilter {

  /** The text description of the types of files accepted */
  protected String m_Description;

  /** The filename extension of accepted files */
  protected String m_Extension;

  /**
   * Creates the ExtensionFileFilter
   *
   * @param extension the extension of accepted files.
   * @param description a text description of accepted files.
   */
  public ExtensionFileFilter(String extension, String description) {

    m_Extension = extension;
    m_Description = description;
  }
  
  /**
   * Gets the description of accepted files.
   *
   * @return the description.
   */
  public String getDescription() {
    
    return m_Description;
  }
  
  /**
   * Returns true if the supplied file should be accepted (i.e. if it
   * has the required extension or is a directory).
   *
   * @param file the file of interest.
   * @return true if the file is accepted by the filter.
   */
  public boolean accept(File file) {
    
    String name = file.getName().toLowerCase();
    if (file.isDirectory()) {
      return true;
    }
    if (name.endsWith(m_Extension)) {
      return true;
    }
    return false;
  }
  
  /**
   * Returns true if the file in the given directory with the given name
   * should be accepted.
   *
   * @param dir the directory where the file resides.
   * @param name the name of the file.
   * @return true if the file is accepted.
   */
  public boolean accept(File dir, String name) {
    return accept(new File(dir, name));
  }
} // ExtensionFileFilter
