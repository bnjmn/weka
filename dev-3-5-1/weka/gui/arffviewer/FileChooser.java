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
 * FileChooser.java
 * Copyright (C) 2005 FracPete
 *
 */

package weka.gui.arffviewer;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;

/**
 * This class fixes a bug with the Swing JFileChooser: if you entered a new
 * filename in the save dialog and press Enter the <code>getSelectedFile</code>
 * method returns <code>null</code> instead of the filename.<br>
 * To solve this annoying behavior we call the save dialog once again s.t. the
 * filename is set. Might look a little bit strange to the user, but no 
 * NullPointerException! ;-)
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $ 
 */

public class FileChooser 
  extends JFileChooser {
  
  /**
   * default constructor, pointing to the user's default directory
   */
  public FileChooser() {
    super();
  }
  
  /**
   * sets the default directory to the given one
   * 
   * @param currentDirectoryPath    the directory to start in
   */
  public FileChooser(String currentDirectoryPath) {
    super(currentDirectoryPath);
  }
  
  /**
   * sets the default directory to the given one
   * 
   * @param currentDirectory    the directory to start in
   */
  public FileChooser(File currentDirectory) {
    super(currentDirectory);
  }
  
  /**
   * Pops up a "Save File" file chooser dialog
   */
  public int showSaveDialog(Component parent) {
    int         result;
    
    do {
      result = super.showSaveDialog(parent);
      if (result != APPROVE_OPTION)
        return result;
    }
    while (getSelectedFile() == null);
    
    return result;
  }
}
