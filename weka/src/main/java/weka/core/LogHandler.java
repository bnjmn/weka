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
 *    LogHandler.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import weka.gui.Logger;

/**
 * Interface to something that can output messages to a log
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 47640 $
 */
public interface LogHandler {

  /**
   * Set the log to use
   * 
   * @param log the log to use
   */
  void setLog(Logger log);

  /**
   * Get the log in use
   * 
   * @return the log in use
   */
  Logger getLog();
}
