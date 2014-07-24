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
 * CapabilitiesIgnorer.java
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

/**
 * Classes implementing this interface make it possible to turn off
 * capabilities checking.
 * 
 * @author  Eibe Frank
 * @version $Revision: 11004 $
 * @see     Capabilities
 */
public interface CapabilitiesIgnorer {
  
  /** 
   * Returns true if we do not actually want to check
   * capabilities to conserver runtime.
   */
  public boolean getDoNotCheckCapabilities();
  
  /** 
   * If argument is true, capabilities are not actually
   * checked to improve runtime.
   */
  public void setDoNotCheckCapabilities(boolean flag);
}
