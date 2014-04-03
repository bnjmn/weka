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
 *    Preconstructed.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

/**
 * Marker interface for something that has been constructed/learned earlier
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface Preconstructed {

  /**
   * Returns true if this Preconstructed instance is initialized and ready to be
   * used
   * 
   * @return true if this instance is initialized and ready to be used
   */
  boolean isConstructed();

  /**
   * Reset. After calling reset() a call to isConstructed() should return false.
   */
  void resetPreconstructed();

}
