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
 * StringLocator.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.io.Serializable;
import java.util.Vector;

/**
 * This class locates and records the indices of a certain type of attributes, 
 * recursively in case of Relational attributes.
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 * @see Attribute#RELATIONAL
 */
public class AttributeLocator 
  implements Serializable {
  
  /** contains either true or false Boolean objects or a reference to a 
   * further StringLocator object due to a RELATIONAL attribute */
  protected Vector m_Locations = null;

  /** the type of the attribute */
  protected int m_Type = -1;
  
  /** the referenced data */
  protected Instances m_Data = null;

  /** the indices */
  protected int[] m_Indices = null;

  /** the indices of locator objects */
  protected int[] m_LocatorIndices = null;
  
  /**
   * initializes the AttributeLocator with the given data for the specified
   * type of attribute
   * 
   * @param data	the data to work on
   * @param type	the type of attribute to locate
   */
  public AttributeLocator(Instances data, int type) {
    super();
      
    m_Data = data;
    m_Type = type;
    
    locate();

    m_Indices        = find(true);
    m_LocatorIndices = find(false);
  }
  
  /**
   * returns the type of attribute that is located
   */
  public int getType() {
    return m_Type;
  }
  
  /**
   * sets up the structure
   */
  protected void locate() {
    int         i;
    
    m_Locations = new Vector();
    
    for (i = 0; i < m_Data.numAttributes(); i++) {
      if (m_Data.attribute(i).type() == getType())
        m_Locations.add(new Boolean(true));
      else if (m_Data.attribute(i).type() == Attribute.RELATIONAL)
	m_Locations.add(new AttributeLocator(m_Data.attribute(i).relation(), getType()));
      else
        m_Locations.add(new Boolean(false));
    }
  }
  
  /**
   * returns the underlying data
   * 
   * @return      the underlying Instances object
   */
  public Instances getData() {
    return m_Data;
  }
  
  /**
   * returns the indices of the searched-for attributes (if TRUE) or the indices
   * of AttributeLocator objects (if FALSE)
   * 
   * @param stringAtts    if true the indices of String attributes are located,
   *                      otherwise the ones of AttributeLocator objects
   * @return              the indices of the attributes or the AttributeLocator objects
   */
  protected int[] find(boolean stringAtts) {
    int       count;
    int       i;
    int[]     result;
    
      // count them
      count = 0;
      for (i = 0; i < m_Locations.size(); i++) {
        if (stringAtts) {
          if ( (m_Locations.get(i) instanceof Boolean) && (((Boolean) m_Locations.get(i)).booleanValue()) )
            count++;
        }
        else {
          if (m_Locations.get(i) instanceof AttributeLocator)
            count++;
        }
      }
      
      // fill array
      result = new int[count];
      count     = 0;
      for (i = 0; i < m_Locations.size(); i++) {
        if (stringAtts) {
          if ( (m_Locations.get(i) instanceof Boolean) && (((Boolean) m_Locations.get(i)).booleanValue()) ) {
            result[count] = i;
            count++;
          }
        }
        else {
          if (m_Locations.get(i) instanceof AttributeLocator) {
            result[count] = i;
            count++;
          }
        }
      }

    return result;
  }
  
  /**
   * returns the indices of the String attributes
   * 
   * @return      the indices of the attributes
   */
  public int[] getAttributeIndices() {
    return m_Indices;
  }
  
  /**
   * returns the indices of the AttributeLocator objects
   * 
   * @return      the indices of the AttributeLocator objects
   */
  public int[] getLocatorIndices() {
    return m_LocatorIndices;
  }
  
  /**
   * returns the AttributeLocator at the given index
   * 
   * @param index   the index of the locator to retrieve
   * @return        the AttributeLocator at the given index
   */
  public AttributeLocator getLocator(int index) {
    return (AttributeLocator) m_Locations.get(index);
  }
  
  /**
   * returns a string representation of this object
   */
  public String toString() {
    return m_Locations.toString();
  }
}
