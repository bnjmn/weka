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
 *    InvalidInputException.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor.tree;

/**
 * A custom Exception that is thrown when a user specifies an invalid
 * set of parameters to the LibraryEditor Tree GUI.  This covers things
 * like specifying a range of numerical values with the min being > than
 * the max, etc...
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.1 $
 */
public class InvalidInputException
  extends Exception {
  
  /** for serialization */
  private static final long serialVersionUID = 9192136737177003882L;

  /**
   * initializes the exception
   * 
   * @param s		the message of the exception
   */
  public InvalidInputException(String s) {
    super(s);
  }
}