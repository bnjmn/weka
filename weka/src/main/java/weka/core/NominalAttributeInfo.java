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
 *    NominalAttributeInfo.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.core;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Stores information for nominal and string attributes.
 */
public class NominalAttributeInfo implements AttributeInfo {

  /** The attribute's values. */
  protected/* @ spec_public @ */ArrayList<Object> m_Values;

  /** Mapping of values to indices. */
  protected Hashtable<Object, Integer> m_Hashtable;

  /**
   * Constructs the info based on argument.
   */
  public NominalAttributeInfo(List<String> attributeValues, String attributeName) {

    if (attributeValues == null) {
      m_Values = new ArrayList<Object>();
      m_Hashtable = new Hashtable<Object, Integer>();
    } else {
      m_Values = new ArrayList<Object>(attributeValues.size());
      m_Hashtable = new Hashtable<Object, Integer>(attributeValues.size());
      for (int i = 0; i < attributeValues.size(); i++) {
        Object store = attributeValues.get(i);
        if (((String) store).length() > Attribute.STRING_COMPRESS_THRESHOLD) {
          try {
            store = new SerializedObject(attributeValues.get(i), true);
          } catch (Exception ex) {
            System.err.println("Couldn't compress nominal attribute value -"
              + " storing uncompressed.");
          }
        }
        if (m_Hashtable.containsKey(store)) {
          throw new IllegalArgumentException("A nominal attribute ("
            + attributeName + ") cannot" + " have duplicate labels (" + store
            + ").");
        }
        m_Values.add(store);
        m_Hashtable.put(store, new Integer(i));
      }
    }
  }
}