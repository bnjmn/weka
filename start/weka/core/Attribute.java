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
 * Class for handling an attribute.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.1 7.9.98 Added string attributes. (Eibe)
 */


public class Attribute implements Copyable, Serializable {

  //=================
  // Public variables
  //=================

  /**
   * Constant set for numeric attributes.
   */

  public final static int NUMERIC = 0;

  /**
   * Constant set for nominal attributes.
   */

  public final static int NOMINAL = 1;

  /**
   * Constant set for attributes with string values.
   */

  public final static int STRING = 2;

  //==================
  // Private variables
  //==================

  /**
   * The attribute's name.
   */

  private String theName;

  /**
   * The attribute's type.
   */

  private int theType;

  /**
   * The attribute's values (if nominal).
   */

  private FastVector theValues;

  /**
   * The attribute's index.
   */

  private int theIndex;

  //===============
  // Public Methods
  //===============

  /**
   * Constructor for a numeric attribute.
   * @param attributeName the name for the attribute
   */

  public Attribute(String attributeName) {

    theName = attributeName;
    theIndex = -1;
    theValues = null;
    theType = NUMERIC;
  }

  /**
   * Constructor for nominal attributes and string attributes.
   * If a null vector of attribute values is passed to the method,
   * the attribute is assumed to be a string.
   * @param attributeName the name for the attribute
   * @param attributeValues a vector of strings denoting the 
   * attribute values. Null if the attribute is a string attribute.
   */

  public Attribute(String attributeName, 
		   FastVector attributeValues) {

    theName = attributeName;
    theIndex = -1;
    theValues = attributeValues;
    if (theValues == null) {
      theValues = new FastVector();
      theType = STRING;
    } else
      theType = NOMINAL;
  }

  /**
   * Produces a shallow copy of this attribute
   * @return a clone of this attribute with the same index
   */

  public Object copy() {

    Attribute copy = new Attribute(theName);

    copy.theIndex = theIndex;
    if (!isNominal() && !isString())
      return copy;
    copy.theType = theType;
    copy.theValues = theValues;
 
    return copy;
  }

  /**
   * Returns enumeration of all the attribute's values if
   * the attribute is nominal or a string, null otherwise. 
   * @return enumeration of all the attribute's values
   */

  public final Enumeration enumerateValues() {

    if (isNominal() || isString())
      return theValues.elements();
    return null;
  }

  /**
   * Tests if given attribute is equal to this attribute.
   * @param other the attribute to be compared to this attribute
   * @return true if the given attribute is equal to this attribute
   */

  public final boolean equals(Attribute other) {

    if (!theName.equals(other.theName))
      return false;
    if (isNumeric() && other.isNumeric())
      return true;
    if (isNumeric() || other.isNumeric())
      return false;
    if (theValues.size() != other.theValues.size())
      return false;
    for (int i = 0; i < theValues.size(); i++)
      if (!((String) theValues.elementAt(i)).equals
	  ((String) other.theValues.elementAt(i)))
	return false;
    return true;
  }

  /**
   * Returns the index of this attribute
   * @return the index of this attribute
   */

  public final int index() {

    return theIndex;
  }

  /**
   * Returns the index of a given attribute value. (The index of
   * the first occurence of this value.)
   * @param value the value for which the index is to be returned
   * @return the index of the given attribute value if attribute
   * is nominal or a string, -1 if it is numeric or the value 
   * can't be found
   */

  public final int indexOfValue(String value) {

    if (!isNominal() && !isString())
      return -1;
    return theValues.indexOf(value);
  }

  /**
   * Test if the attribute is nominal.
   * @result true if the attribute is nominal
   */

  public final boolean isNominal() {

    return (theType == NOMINAL);
  }

  /**
   * Tests if the attribute is numeric.
   * @result true if the attribute is numeric
   */

  public final boolean isNumeric() {

    return (theType == NUMERIC);
  }

  /**
   * Tests if the attribute is a string.
   * @result true if the attribute is a string
   */

  public final boolean isString() {

    return (theType == STRING);
  }

  /**
   * Returns the attribute's name.
   * @return the attribute's name as a string
   */

  public final String name() {

    return theName;
  }
  
  /**
   * Returns the number of attribute values. Returns 0 for numeric attributes
   * and string attributes.
   * @return the number of attribute values
   */

  public final int numValues() {

    if (!isNominal() && !isString())
      return 0;
    else
      return theValues.size();
  }

  /**
   * Returns a description of this attribute in ARFF format.
   * @return a description of this attribute as a string
   */

  public final String toString() {

    StringBuffer text = new StringBuffer();

    text.append("@attribute "+theName+" ");
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
	text.append("real");
      else
	text.append("string");
    
    return text.toString();
  }

  /**
   * Returns the attribute's type as an integer.
   * @returns the attribute's type.
   */

  public final int type() {

    return theType;
  }

  /**
   * Returns a value of a nominal or string attribute. 
   * Returns an empty string if the attribute is neither
   * nominal nor a string attribute.
   * @param valIndex the value's index
   * @return the attribute's value as a string
   */

  public final String value(int valIndex) {
    
    if (!isNominal() && !isString())
      return "";
    else
      return (String) theValues.elementAt(valIndex);
  }

  // ====================
  // Semi-private methods
  // ====================

  /**
   * Constructor for a numeric attribute.
   * @param attributeName the name for the attribute
   * @param index the attribute's index
   */

  Attribute(String attributeName, int index) {

    this(attributeName);

    theIndex = index;
  }

  /**
   * Constructor for nominal attributes and string attributes.
   * If a null vector of attribute values is passed to the method,
   * the attribute is assumed to be a string.
   * @param attributeName the name for the attribute
   * @param attributeValues a vector of strings denoting the attribute values.
   * Null if the attribute is a string attribute.
   * @param index the attribute's index
   */

  Attribute(String attributeName, FastVector attributeValues, 
	    int index) {

    this(attributeName, attributeValues);

    theIndex = index;
  }

  /**
   * Adds an attribute value.
   */

  final void addValue(String value) {

    freshAttributeValues();
    theValues.addElement(value);
  }

  /**
   * Removes a value of a nominal or string attribute.
   * @param index the value's index
   * @exception Exception if the attribute is not nominal
   */
  
  final void delete(int index) throws Exception {
    
    if (!isNominal() && !isString()) 
      throw new Exception("Can only remove value of"+
			  "nominal or string attribute!");
    else {
      freshAttributeValues();
      theValues.removeElementAt(index);
    }
  }

  /**
   * Adds an attribute value.
   */

  final void forceAddValue(String value) {

    theValues.addElement(value);
  }

  /**
   * Sets the index of this attribute
   * @param the index of this attribute
   */

  final void setIndex(int index) {

    theIndex = index;
  }

  /**
   * Sets a value of a nominal attribute or string attribute.
   * @param index the value's index
   * @param string the value
   * @exception Exception if the attribute is not nominal
   */

  final void setValue(int index, String string) 
       throws Exception {

    if (!isNominal() && !isString()) 
      throw new Exception("Can only set value of nominal"+
			  "or string attribute!");
    else {
      freshAttributeValues();
      theValues.setElementAt(string, index);
    }
  }

  // ===============
  // Private methods
  // ===============

  /**
   * Produces a fresh vector of attribute values.
   */

  private void freshAttributeValues() {

    theValues = (FastVector)theValues.copy();
  }
}
  
