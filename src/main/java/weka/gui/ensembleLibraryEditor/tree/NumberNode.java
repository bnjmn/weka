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
 *    NumberNode.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor.tree;

import java.math.BigDecimal;
import java.text.NumberFormat;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This subclass is responsible for allowing users to specify either a minimum,
 * maximum, or iterator value for Integer attributes. It stores a value that is
 * of type java.lang.Number to accomodate the many different number types used
 * by Weka classifiers.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision$
 */
public class NumberNode
  extends DefaultMutableTreeNode {
  
  /** for serialization */
  private static final long serialVersionUID = -2505599954089243851L;

  /** the enumerated value indicating a node is not an iterator */
  public static final int NOT_ITERATOR = 0;
  
  /** the enumerated value indicating a node is a *= iterator */
  public static final int TIMES_EQUAL = 1;
  
  /** the enumerated value indicating a node is a += iterator */
  public static final int PLUS_EQUAL = 2;
  
  /** the name of the node to be displayed */
  private String m_Name;
  
  /** the iterator type, NOT_ITERATOR, TIMES_EQUAL, or PLUS_EQUAL */
  private int m_IteratorType;
  
  /** this stores whether or not this node should have a checkbox */
  private boolean m_Checkable;
  
  /** this stores the node's selected state */
  private boolean m_Selected;
  
  /** the node's tipText */
  private String m_ToolTipText;
  
  /**
   * This method rounds a double to the number of decimal places defined by
   * scale
   * 
   * @param a		the value to round
   * @return		the rounded value
   */
  public static double roundDouble(double a) {
    return new BigDecimal("" + a).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
  }
  
  /**
   * This method rounds a float to the number of decimal places defined by
   * scale
   * 
   * @param a		the value to round
   * @return		the rounded value
   */
  public static float roundFloat(float a) {
    return new BigDecimal("" + a).setScale(scale, BigDecimal.ROUND_HALF_UP).floatValue();
  }
  
  /**
   * this defines the number of decimal places we care about, we arbitrarily
   * chose 7 thinking that anything beyond this is overkill
   */
  public static final int scale = 7;
  
  /**
   * This is the maximum floating point value that we care about when testing
   * for equality.
   */
  public static final double epsilon = 0.000001;
  
  /**
   * The constructor simply initializes all of the member variables
   * 
   * @param text		the name
   * @param value		the actual value
   * @param iteratorType	the iterator type
   * @param checkable		true if it's checkable
   * @param toolTipText		the tooltip to use
   */
  public NumberNode(String text, Number value, int iteratorType,
      boolean checkable, String toolTipText) {
    
    this.m_Name = text;
    setValue(value);
    this.m_IteratorType = iteratorType;
    this.m_Checkable = checkable;
    this.m_Selected = false;
    this.m_ToolTipText = toolTipText;
  }
  
  /**
   * getter for the node selected state
   * 
   * @return whether or not this node is selected
   */
  public boolean getSelected() {
    return m_Selected;
  }
  
  /**
   * setter for the node selected state
   * 
   * @param newValue
   *            the new selected state
   */
  public void setSelected(boolean newValue) {
    m_Selected = newValue;
  }
  
  /**
   * getter for this node's object
   * 
   * @return	the current value
   */
  public Number getValue() {
    return (Number) getUserObject();
  }
  
  /**
   * setter for this nodes object
   * 
   * @param newValue	the new value to use
   */
  public void setValue(Number newValue) {
    userObject = newValue;
  }
  
  /**
   * getter for this node's iteratorType which will be one of the three
   * enumerated values
   * 
   * @return		the iterator type
   */
  public int getIteratorType() {
    return m_IteratorType;
  }
  
  /**
   * setter for this nodes iteratorType which should be one of the three
   * enumerated values
   * 
   * @param newValue	the new iterator type to use
   */
  public void setIteratorType(int newValue) {
    m_IteratorType = newValue;
  }
  
  /**
   * returns whether or not this node can be toggled on and off
   * 
   * @return		true if it's checkable
   */
  public boolean getCheckable() {
    return m_Checkable;
  }
  
  /**
   * returns the text to be displayed for this node
   * 
   * @return		the name
   */
  public String getText() {
    return m_Name;
  }
  
  /**
   * getter for the tooltip text
   * 
   * @return tooltip text
   */
  public String getToolTipText() {
    return m_ToolTipText;
  }
  
  /**
   * this is a simple filter for the setUserObject method. We basically don't
   * want null values to be passed in.
   * 
   * @param o		the user object
   */
  public void setUserObject(Object o) {
    if (o != null)
      super.setUserObject(o);
  }
  
  /**
   * returns a string representation
   * 
   * @return		a string representation
   */
  public String toString() {
    return getClass().getName() + "[" + m_Name + ": "
    + getUserObject().toString() + "]";
  }
  
  // *************** IMPORTANT!!! ***********************
  // I could not figure out a graceful way to deal with this!
  // we have a requirement here to add, multiply, and test for
  // equality various subclasses of java.lang.number the
  // following eight methods are extremely redundant and are
  // a horrible example cutting/pasting. However, this
  // is what I've ended up with and its very clunky looking.
  // If anyone knows a better way to do this then please be my
  // guest by all means!
  
  // I really can't beleive there's not some slick way of handling
  // this stuff built into the language
  
  /**
   * figures out the class of this node's object and returns a new instance of
   * it initialized with the value of "0".
   * 
   * @return					0 as object
   * @throws NumberClassNotFoundException	if number class not supported
   */
  public Number getZeroValue() throws NumberClassNotFoundException {
    
    Number value = getValue();
    Number zero = null;
    
    if (value instanceof Double)
      zero = new Double(0.0);
    else if (value instanceof Integer)
      zero = new Integer(0);
    else if (value instanceof Float)
      zero = new Float(0.0);
    else if (value instanceof Long)
      zero = new Long(0);
    else {
      throw new NumberClassNotFoundException(value.getClass()
	  + " not currently supported.");
    }
    
    return zero;
  }
  
  /**
   * figures out the class of this node's object and returns a new instance of
   * it initialized with the value of "1".
   * 
   * @return					1 as object
   * @throws NumberClassNotFoundException	if number class not supported
   */
  public Number getOneValue() throws NumberClassNotFoundException {
    
    Number value = getValue();
    Number one = null;
    
    if (value instanceof Double)
      one = new Double(1.0);
    else if (value instanceof Integer)
      one = new Integer(1);
    else if (value instanceof Float)
      one = new Float(1.0);
    else if (value instanceof Long)
      one = new Long(1);
    else {
      throw new NumberClassNotFoundException(value.getClass()
	  + " not currently supported.");
    }
    return one;
  }
  
  /**
   * figures out the class of this node's object and returns a new instance of
   * it initialized with the value of "2".
   * 
   * @return					2 as object
   * @throws NumberClassNotFoundException	if number class not supported
   */
  public Number getTwoValue() throws NumberClassNotFoundException {
    
    Number value = getValue();
    Number two = null;
    
    if (value instanceof Double)
      two = new Double(2.0);
    else if (value instanceof Integer)
      two = new Integer(2);
    else if (value instanceof Float)
      two = new Float(2.0);
    else if (value instanceof Long)
      two = new Long(2);
    else {
      throw new NumberClassNotFoundException(value.getClass()
	  + " not currently supported.");
    }
    return two;
  }
  
  /**
   * adds two objects that are instances of one of the child classes of
   * java.lang.Number
   * 
   * @param a	the first number
   * @param b	the second number
   * @return 	the sum: a+b
   * @throws NumberClassNotFoundException	if number class not supported
   */
  public Number addNumbers(Number a, Number b)
    throws NumberClassNotFoundException {
    
    Number sum = null;
    
    if (a instanceof Double && b instanceof Double) {
      sum = new Double(roundDouble(a.doubleValue() + b.doubleValue()));
      // trimNumber(sum);
      
    } else if (a instanceof Integer && b instanceof Integer) {
      sum = new Integer(a.intValue() + b.intValue());
    } else if (a instanceof Float && b instanceof Float) {
      sum = new Float(roundFloat(a.floatValue() + b.floatValue()));
      
      // trimNumber(sum);
      
    } else if (a instanceof Long && b instanceof Long) {
      sum = new Long(a.longValue() + b.longValue());
    } else {
      throw new NumberClassNotFoundException(a.getClass() + " and "
	  + b.getClass() + " not currently supported.");
    }
    return sum;
  }
  
  /**
   * multiplies two objects that are instances of one of the child classes of
   * java.lang.Number
   * 
   * @param a	the first number
   * @param b	the second number
   * @return 	the product: a*b
   * @throws NumberClassNotFoundException	if number class not supported
   */
  public Number multiplyNumbers(Number a, Number b)
    throws NumberClassNotFoundException {
    
    Number product = null;
    
    if (a instanceof Double && b instanceof Double) {
      product = new Double(roundDouble(a.doubleValue() * b.doubleValue()));
      
    } else if (a instanceof Integer && b instanceof Integer) {
      product = new Integer(a.intValue() * b.intValue());
    } else if (a instanceof Float && b instanceof Float) {
      product = new Float(roundFloat(a.floatValue() * b.floatValue()));
      
    } else if (a instanceof Long && b instanceof Long) {
      product = new Long(a.longValue() * b.longValue());
    } else {
      throw new NumberClassNotFoundException(a.getClass() + " and "
	  + b.getClass() + " not currently supported.");
    }
    return product;
  }
  
  /**
   * tests if the first argument is greater than the second among two objects
   * that are instances of one of the child classes of java.lang.Number
   * 
   * @param a	the first number
   * @param b	the second number
   * @return 	true if a is less than b
   * @throws NumberClassNotFoundException	if number class not supported
   */
  public boolean lessThan(Number a, Number b)
    throws NumberClassNotFoundException {
    
    boolean greater = false;
    
    if (a instanceof Double && b instanceof Double) {
      if (a.doubleValue() < b.doubleValue())
	greater = true;
    } else if (a instanceof Integer && b instanceof Integer) {
      if (a.intValue() < b.intValue())
	greater = true;
    } else if (a instanceof Float && b instanceof Float) {
      if (a.floatValue() < b.floatValue())
	greater = true;
    } else if (a instanceof Long && b instanceof Long) {
      if (a.longValue() < b.longValue())
	greater = true;
    } else {
      throw new NumberClassNotFoundException(a.getClass() + " and "
	  + b.getClass() + " not currently supported.");
    }
    
    return greater;
    
  }
  
  /**
   * tests for equality among two objects that are instances of one of the
   * child classes of java.lang.Number
   * 
   * @param a	the first number
   * @param b	the second number
   * @return 	true if the two values are equal
   * @throws NumberClassNotFoundException	if number class not supported
   */
  public boolean equals(Number a, Number b)
    throws NumberClassNotFoundException {
    
    boolean equals = false;
    
    if (a instanceof Double && b instanceof Double) {
      if (Math.abs(a.doubleValue() - b.doubleValue()) < epsilon)
	equals = true;
    } else if (a instanceof Integer && b instanceof Integer) {
      if (a.intValue() == b.intValue())
	equals = true;
    } else if (a instanceof Float && b instanceof Float) {
      if (Math.abs(a.floatValue() - b.floatValue()) < epsilon)
	equals = true;
    } else if (a instanceof Long && b instanceof Long) {
      if (a.longValue() == b.longValue())
	equals = true;
    } else {
      throw new NumberClassNotFoundException(a.getClass() + " and "
	  + b.getClass() + " not currently supported.");
    }
    
    return equals;
  }
  
  /**
   * A helper method to figure out what number format should be used to
   * display the numbers value in a formatted text box.
   * 
   * @return the number format
   * @throws NumberClassNotFoundException	if number class not supported
   */
  public NumberFormat getNumberFormat() throws NumberClassNotFoundException {
    NumberFormat numberFormat = null;
    
    Number value = getValue();
    
    if (value instanceof Double) {
      numberFormat = NumberFormat.getInstance();
      numberFormat.setMaximumFractionDigits(7);
    } else if (value instanceof Integer) {
      numberFormat = NumberFormat.getIntegerInstance();
    } else if (value instanceof Float) {
      numberFormat = NumberFormat.getInstance();
      numberFormat.setMaximumFractionDigits(7);
    } else if (value instanceof Long) {
      numberFormat = NumberFormat.getIntegerInstance();
    } else {
      throw new NumberClassNotFoundException(value.getClass()
	  + " not currently supported.");
    }
    
    return numberFormat;
  }
}
