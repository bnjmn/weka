/*
 *    Attribute.java
 *    Copyright (C) 1999 Eibe Frank
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

import java.io.*;
import java.util.*;

/** 
 * Class for handling an attribute.<p>
 *
 * Three attribute types are supported:
 * <ul>
 *    <li> numeric: <ul>
 *         This type of attribute represents a floating-point number.
 *    </ul>
 *    <li> nominal: <ul>
 *         This type of attribute represents a fixed set of nominal values.
 *    </ul>
 *    <li> string: <ul>
 *         This type of attribute represents a dynamically expanding set of
 *         nominal values. String attributes are not used by the learning
 *         schemes in Weka. They can be used, for example,  to store an 
 *         identifier with each instance in a dataset.
 *    </ul>
 * </ul>
 * Typical usage (code from the main() method of this class): <p>
 *
 * <code>
 * ... <br>
 *
 * // Create numeric attributes "length" and "weight" <br>
 * Attribute length = new Attribute("length"); <br>
 * Attribute weight = new Attribute("weight"); <br><br>
 * 
 * // Create vector to hold nominal values "first", "second", "third" <br>
 * FastVector my_nominal_values = new FastVector(3); <br>
 * my_nominal_values.addElement("first"); <br>
 * my_nominal_values.addElement("second"); <br>
 * my_nominal_values.addElement("third"); <br><br>
 *
 * // Create nominal attribute "position" <br>
 * Attribute position = new Attribute("position", my_nominal_values);<br>
 *
 * ... <br>
 * </code><p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class Attribute implements Copyable, Serializable {

  /** Constant set for numeric attributes. */
  public final static int NUMERIC = 0;

  /** Constant set for nominal attributes. */
  public final static int NOMINAL = 1;

  /** Constant set for attributes with string values. */
  public final static int STRING = 2;

  /** The attribute's name. */
  private String m_Name;

  /** The attribute's type. */
  private int m_Type;

  /** The attribute's values (if nominal). */
  private FastVector m_Values;

  /** The attribute's index. */
  private int m_Index;

  /**
   * Constructor for a numeric attribute.
   *
   * @param attributeName the name for the attribute
   */
  public Attribute(String attributeName) {

    m_Name = attributeName;
    m_Index = -1;
    m_Values = null;
    m_Type = NUMERIC;
  }

  /**
   * Constructor for nominal attributes and string attributes.
   * If a null vector of attribute values is passed to the method,
   * the attribute is assumed to be a string.
   *
   * @param attributeName the name for the attribute
   * @param attributeValues a vector of strings denoting the 
   * attribute values. Null if the attribute is a string attribute.
   */
  public Attribute(String attributeName, 
		   FastVector attributeValues) {

    m_Name = attributeName;
    m_Index = -1;
    m_Values = attributeValues;
    if (m_Values == null) {
      m_Values = new FastVector();
      m_Type = STRING;
    } else
      m_Type = NOMINAL;
  }

  /**
   * Produces a shallow copy of this attribute.
   *
   * @return a copy of this attribute with the same index
   */
  public Object copy() {

    Attribute copy = new Attribute(m_Name);

    copy.m_Index = m_Index;
    if (!isNominal() && !isString())
      return copy;
    copy.m_Type = m_Type;
    copy.m_Values = m_Values;
 
    return copy;
  }

  /**
   * Returns an enumeration of all the attribute's values if
   * the attribute is nominal or a string, null otherwise. 
   *
   * @return enumeration of all the attribute's values
   */
  public final Enumeration enumerateValues() {

    if (isNominal() || isString())
      return m_Values.elements();
    return null;
  }

  /**
   * Tests if given attribute is equal to this attribute.
   *
   * @param other the attribute to be compared to this attribute
   * @return true if the given attribute is equal to this attribute
   */
  public final boolean equals(Attribute other) {

    if (!m_Name.equals(other.m_Name))
      return false;
    if (isNumeric() && other.isNumeric())
      return true;
    if (isNumeric() || other.isNumeric())
      return false;
    if (m_Values.size() != other.m_Values.size())
      return false;
    for (int i = 0; i < m_Values.size(); i++)
      if (!((String) m_Values.elementAt(i)).equals
	  ((String) other.m_Values.elementAt(i)))
	return false;
    return true;
  }

  /**
   * Returns the index of this attribute.
   *
   * @return the index of this attribute
   */
  public final int index() {

    return m_Index;
  }

  /**
   * Returns the index of a given attribute value. (The index of
   * the first occurence of this value.)
   *
   * @param value the value for which the index is to be returned
   * @return the index of the given attribute value if attribute
   * is nominal or a string, -1 if it is numeric or the value 
   * can't be found
   */
  public final int indexOfValue(String value) {

    if (!isNominal() && !isString())
      return -1;
    return m_Values.indexOf(value);
  }

  /**
   * Test if the attribute is nominal.
   *
   * @result true if the attribute is nominal
   */
  public final boolean isNominal() {

    return (m_Type == NOMINAL);
  }

  /**
   * Tests if the attribute is numeric.
   *
   * @result true if the attribute is numeric
   */
  public final boolean isNumeric() {

    return (m_Type == NUMERIC);
  }

  /**
   * Tests if the attribute is a string.
   *
   * @result true if the attribute is a string
   */
  public final boolean isString() {

    return (m_Type == STRING);
  }

  /**
   * Returns the attribute's name.
   *
   * @return the attribute's name as a string
   */
  public final String name() {

    return m_Name;
  }
  
  /**
   * Returns the number of attribute values. Returns 0 for numeric attributes
   * and string attributes.
   *
   * @return the number of attribute values
   */
  public final int numValues() {

    if (!isNominal() && !isString()) {
      return 0;
    } else {
      return m_Values.size();
    }
  }

  /**
   * Returns a description of this attribute in ARFF format.
   *
   * @return a description of this attribute as a string
   */
  public final String toString() {

    StringBuffer text = new StringBuffer();

    text.append("@attribute "+m_Name+" ");
    if (isNominal()) {
      text.append('{');
      Enumeration enum = enumerateValues();
      while (enum.hasMoreElements()) {
	text.append((String) enum.nextElement());
	if (enum.hasMoreElements())
	  text.append(',');
      }
      text.append('}');
    } else 
      if (isNumeric())
	text.append("numeric");
      else
	text.append("string");
    
    return text.toString();
  }

  /**
   * Returns the attribute's type as an integer.
   *
   * @returns the attribute's type.
   */
  public final int type() {

    return m_Type;
  }

  /**
   * Returns a value of a nominal or string attribute. 
   * Returns an empty string if the attribute is neither
   * nominal nor a string attribute.
   *
   * @param valIndex the value's index
   * @return the attribute's value as a string
   */
  public final String value(int valIndex) {
    
    if (!isNominal() && !isString())
      return "";
    else
      return (String) m_Values.elementAt(valIndex);
  }

  /**
   * Constructor for a numeric attribute with a particular index.
   *
   * @param attributeName the name for the attribute
   * @param index the attribute's index
   */
  Attribute(String attributeName, int index) {

    this(attributeName);

    m_Index = index;
  }

  /**
   * Constructor for nominal attributes and string attributes with
   * a particular index.
   * If a null vector of attribute values is passed to the method,
   * the attribute is assumed to be a string.
   *
   * @param attributeName the name for the attribute
   * @param attributeValues a vector of strings denoting the attribute values.
   * Null if the attribute is a string attribute.
   * @param index the attribute's index
   */
  Attribute(String attributeName, FastVector attributeValues, 
	    int index) {

    this(attributeName, attributeValues);

    m_Index = index;
  }

  /**
   * Adds an attribute value. Creates a fresh list of attribute
   * values before adding it.
   *
   * @param value the attribute value
   */
  final void addValue(String value) {

    freshAttributeValues();
    m_Values.addElement(value);
  }

  /**
   * Removes a value of a nominal or string attribute. Creates a 
   * fresh list of attribute values before removing it.
   *
   * @param index the value's index
   * @exception Exception if the attribute is not nominal
   */
  final void delete(int index) throws Exception {
    
    if (!isNominal() && !isString()) 
      throw new Exception("Can only remove value of"+
			  "nominal or string attribute!");
    else {
      freshAttributeValues();
      m_Values.removeElementAt(index);
    }
  }

  /**
   * Adds an attribute value.
   *
   * @param value the attribute value
   */
  final void forceAddValue(String value) {

    m_Values.addElement(value);
  }

  /**
   * Sets the index of this attribute.
   *
   * @param the index of this attribute
   */
  final void setIndex(int index) {

    m_Index = index;
  }

  /**
   * Sets a value of a nominal attribute or string attribute.
   * Creates a fresh list of attribute values before it is set.
   *
   * @param index the value's index
   * @param string the value
   * @exception Exception if the attribute is not nominal
   */
  final void setValue(int index, String string) 
       throws Exception {

    if (!isNominal() && !isString()) {
      throw new Exception("Can only set value of nominal"+
			  "or string attribute!");
    } else {
      freshAttributeValues();
      m_Values.setElementAt(string, index);
    }
  }

  /**
   * Produces a fresh vector of attribute values.
   */
  private void freshAttributeValues() {

    m_Values = (FastVector)m_Values.copy();
  }

  /**
   * Simple main method for testing this class.
   */
  public static void main(String[] ops) {

    try {
      
      // Create numeric attributes "length" and "weight"
      Attribute length = new Attribute("length");
      Attribute weight = new Attribute("weight");
      
      // Create vector to hold nominal values "first", "second", "third" 
      FastVector my_nominal_values = new FastVector(3); 
      my_nominal_values.addElement("first"); 
      my_nominal_values.addElement("second"); 
      my_nominal_values.addElement("third"); 
      
      // Create nominal attribute "position" 
      Attribute position = new Attribute("position", my_nominal_values);

      // Print the name of "position"
      System.out.println("Name of \"position\": " + position.name());

      // Print the values of "position"
      Enumeration attValues = position.enumerateValues();
      while (attValues.hasMoreElements()) {
	String string = (String)attValues.nextElement();
	System.out.println("Value of \"position\": " + string);
      }

      // Shallow copy attribute "position"
      Attribute copy = (Attribute) position.copy();

      // Test if attributes are the same
      System.out.println("Copy is the same as original: " + copy.equals(position));

      // Print index of attribute "weight" (should be unset: -1)
      System.out.println("Index of attribute \"weight\" (should be -1): " + 
			 weight.index());

      // Print index of value "first" of attribute "position"
      System.out.println("Index of value \"first\" of \"position\" (should be 0): " +
			 position.indexOfValue("first"));

      // Tests type of attribute "position"
      System.out.println("\"position\" is numeric: " + position.isNumeric());
      System.out.println("\"position\" is nominal: " + position.isNominal());
      System.out.println("\"position\" is string: " + position.isString());

      // Prints name of attribute "position"
      System.out.println("Name of \"position\": " + position.name());
    
      // Prints number of values of attribute "position"
      System.out.println("Number of values for \"position\": " + position.numValues());

      // Prints the values (againg)
      for (int i = 0; i < position.numValues(); i++) {
	System.out.println("Value " + i + ": " + position.value(i));
      }

      // Prints the attribute "position" in ARFF format
      System.out.println(position);

      // Checks type of attribute "position" using constants
      switch (position.type()) {
      case Attribute.NUMERIC:
	System.out.println("\"position\" is numeric");
	break;
      case Attribute.NOMINAL:
	System.out.println("\"position\" is nominal");
	break;
      case Attribute.STRING:
	System.out.println("\"position\" is string");
	break;
      default:
	System.out.println("\"position\" has unknown type");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
  
