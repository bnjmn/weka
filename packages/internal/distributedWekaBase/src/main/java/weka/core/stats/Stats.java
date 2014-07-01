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
 *    Stats
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stats;

import java.io.Serializable;

import weka.core.Attribute;

/**
 * Stats base class for the numeric and nominal summary meta data
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class Stats implements Serializable {

  /** For serialization */
  private static final long serialVersionUID = 3662688283840145572L;

  /** The name of the attribute that this Stats pertains to */
  protected String m_attributeName = "";

  /**
   * Construct a new Stats
   * 
   * @param attributeName the name of the attribute/field that this Stats
   *          pertains to
   */
  public Stats(String attributeName) {
    m_attributeName = attributeName;
  }

  /**
   * Get the name of the attribute that this Stats pertains to
   * 
   * @return the name of the attribute
   */
  public String getName() {
    return m_attributeName;
  }

  /**
   * Makes a Attribute that encapsulates the meta data
   * 
   * @return an Attribute that encapsulates the meta data
   */
  public abstract Attribute makeAttribute();

}
