/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    PropertyDisplayName.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method annotation that can be used to provide some additional information for
 * handling file properties in the GUI. In particular, it can be used to
 * indicate whether an JFileChooser.OPEN_DIALOG or JFileChooser.SAVE_DIALOG
 * should be used with the property and, furthermore, whether the dialog should
 * allow only files or directories to be selected.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FilePropertyMetadata {

  /**
   * Specify the type of JFileChooser dialog that should be used - i.e. either
   * JFileChooser.OPEN_DIALOG or JFileChooser.SAVE_DIALOG
   *
   * @return the type of JFileChooser dialog to use
   */
  int fileChooserDialogType();

  /**
   * Returns true if the file chooser dialog should only allow directories to be
   * selected, otherwise it will allow only files to be selected
   *
   * @return true if only directories rather than files should be selected
   */
  boolean directoriesOnly();
}
