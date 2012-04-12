/*
 *    OptionHandler.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
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

package weka.core;

import java.util.*;

/** 
 * Interface to something that understands options.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version 1.1 - 25 Mar 1999 - Renamed parseOptions to setOptions, 
 *          added getOptions (Len) <br>
 *          1.0 - Initial version (Eibe)
 */

public interface OptionHandler {

  // ===============
  // Public methods.
  // ===============

  /**
   * Returns an enumeration of all the available options.
   *
   * @return an enumeration of all available options
   */
  Enumeration listOptions();

  /**
   * Sets the OptionHandler's options from the given list. All options
   * will be set (or reset) during this call (i.e. incremental setting
   * of options is not possible).
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  void setOptions(String[] options) throws Exception;

  /**
   * Gets the current option settings for the OptionHandler.
   *
   * @return the list of current option settings as an array of strings
   */
  String [] getOptions();
}








