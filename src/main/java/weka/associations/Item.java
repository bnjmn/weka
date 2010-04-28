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
 *    Item.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.associations;

import java.io.Serializable;

import weka.core.Attribute;

/**
 * Class that encapsulates information about an individual item. An item
 * is a value of a nominal attribute, so this class has a backing Attribute.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com).
 * @version $Revision$
 */
public class Item implements Serializable, Comparable<Item> {
  
  /** For serialization */
  private static final long serialVersionUID = -430198211081183575L;

  protected int m_frequency;
  
  protected Attribute m_attribute;
  
  /** The index of the value considered to be positive */
  protected int m_valueIndex;
  
  /**
   * Constructs a new Item.
   * 
   * @param att the attribute that backs the item.
   * @param valueIndex the index of the value for this item.
   * @throws Exception if the Item can't be constructed.
   */
  public Item(Attribute att, int valueIndex) throws Exception {
    if (att.isNumeric()) {
      throw new Exception("Item must be constructed using a nominal attribute");
    }
    m_attribute = att;
    if (m_attribute.numValues() == 1) {
      m_valueIndex = 0; // unary attribute (? used to indicate absence from a basket)
    } else {
      m_valueIndex = valueIndex;
    }
  }
  
  /**
   * Increase the frequency of this item.
   * 
   * @param f the amount to increase the frequency by.
   */
  public void increaseFrequency(int f) {
    m_frequency += f;
  }
  
  /**
   * Decrease the frequency of this item.
   * 
   * @param f the amount by which to decrease the frequency.
   */
  public void decreaseFrequency(int f) {
    m_frequency -= f;
  }
  
  /**
   * Increment the frequency of this item.
   */
  public void increaseFrequency() {
    m_frequency++;
  }
  
  /**
   * Decrement the frequency of this item.
   */
  public void decreaseFrequency() {
    m_frequency--;
  }
  
  /**
   * Get the frequency of this item.
   * 
   * @return the frequency.
   */
  public int getFrequency() {
    return m_frequency;
  }
  
  /**
   * Get the attribute that this item originates from.
   * 
   * @return the corresponding attribute.
   */
  public Attribute getAttribute() {
    return m_attribute;
  }
  
  /**
   * Get the value index for this item.
   * 
   * @return the value index.
   */
  public int getValueIndex() {
    return m_valueIndex;
  }
  
  /**
   * A string representation of this item.
   * 
   * @return a string representation of this item.
   */
  public String toString() {
    return toString(false);
  }
  
  /**
   * A string representation of this item.
   * 
   * @param freq true if the frequency should be included.
   * @return a string representation of this item. 
   */
  public String toString(boolean freq) {
    String result = m_attribute.name() + "=" + m_attribute.value(m_valueIndex);
    if (freq) {
      result += ":" + m_frequency;
    }
    return result;
  }
  
  /**
   * Ensures that items will be sorted in descending order of frequency.
   * Ties are ordered by attribute name.
   * 
   * @param comp the Item to compare against.
   */
  public int compareTo(Item comp) {
    if (m_frequency == comp.getFrequency()) {
      // sort by name
      return -1 * m_attribute.name().compareTo(comp.getAttribute().name());
    }
    if (comp.getFrequency() < m_frequency) {
      return -1;
    }
    return 1;
  }
  
  /**
   * Equals. Just compares attribute and valueIndex.
   * @return true if this Item is equal to the argument.
   */
  public boolean equals(Object compareTo) {
    if (!(compareTo instanceof Item)) {
      return false;
    }
    
    Item b = (Item)compareTo;
    if (m_attribute.equals(b.getAttribute()) && 
//        m_frequency == b.getFrequency() && 
        m_valueIndex == b.getValueIndex()) {
      return true;
    }
    
    return false;
  }
  
  public int hashCode() {
    return (m_attribute.name().hashCode() ^ 
        m_attribute.numValues()) * m_frequency;
  }
  
  public String toXML() {
    String result = "<ITEM name=\"" +  m_attribute.name() + "\" value=\"=" 
    + m_attribute.value(m_valueIndex) + "\"/>";
    
    return result;
  }
}