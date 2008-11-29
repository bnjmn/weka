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
 *    ConfigurationConstraints.java
 *    Copyright (C) 2008 Pentaho Corporation
 *
 */

package weka.gui.beans;

/**
 * Interface for those components that wan't to control
 * permit or deny user configuration at a given point in
 * time (the KnowledgeFlow will query this interface
 * and enable or disable the contextual menu entry for
 * configuration).
 * 
 * @author mhall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface ConfigurationConstraints {
  
  /**
   * Returns true if, at this point in time, the
   * implementing component can be configured (i.e. if
   * a component is busy doing some work based on
   * the current configuration it might deny a
   * request to change its configuration).
   * 
   * @return true if configuration can be performed
   * at this time.
   */
  boolean configurationAllowed();
}
