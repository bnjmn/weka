/*
 *    FastVector.java
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

import java.util.*;
import java.io.*;

/**
 * Implements a fast vector class without synchronized
 * methods.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0 September 1998 (Eibe)
 */

public class FastVector implements Copyable, Serializable {

  // ==============
  // Nested classes
  // ==============

  public class FastVectorEnumeration implements Enumeration {

    // =================
    // Private variables
    // =================

    /**
     * The counter.
     */

    private int theCounter;

    /**
     * The vector.
     */

    private FastVector theVector;

    /**
     * Special element. Skipped during enumeration.
     */

    private int theSpecialElement;

    // ==============
    // Public methods
    // ==============

    /**
     * Constructs an enumeration.
     * @param vector the vector which is to be enumerated
     */

    public FastVectorEnumeration(FastVector vector) {

      theCounter = 0;
      theVector = vector;
      theSpecialElement = -1;
    }

    /**
     * Constructs an enumeration with a special element.
     * The special element is skipped during the enumeration.
     * @param vector the vector which is to be enumerated
     * @param special the index of the special element
     */
      
    public FastVectorEnumeration(FastVector vector, int special) {

      theVector = vector;
      theSpecialElement = special;
      if (special == 0)
	theCounter = 1;
      else
	theCounter = 0;
    }


    /**
     * Tests if there are any more elements to enumerate.
     * @return true if there are some elements left
     */

    public final boolean hasMoreElements() {

      if (theCounter < theVector.size())
	return true;
      return false;
    }

    /**
     * Returns the next element.
     * @return the next element to be enumerated
     */

    public final Object nextElement() {
  
      Object result = theVector.elementAt(theCounter);

      theCounter++;
      if (theCounter == theSpecialElement)
	theCounter++;
      return result;
    }
  }

  // =================
  // Private variables
  // =================

  /**
   * The array of objects.
   */

  private Object[] theObjects;

  /**
   * The current size;
   */

  private int theSize;

  /**
   * The capacity increment
   */

  private int theCapacityIncrement;
  
  /**
   * The capacity multiplier.
   */

  private double theCapacityMultiplier;
   

  // ===============
  // Public methods.
  // ===============

  /**
   * Constructs an empty vector with initial
   * capacity zero.
   */

  public FastVector() {
  
    theObjects = new Object[0];
    theSize = 0;
    theCapacityIncrement = 1;
    theCapacityMultiplier = 2;
  }

  /**
   * Constructs a vector with the given capacity.
   * @param capacity the vector's initial capacity
   */

  public FastVector(int capacity) {

    theObjects = new Object[capacity];
    theSize = 0;
    theCapacityIncrement = 1;
    theCapacityMultiplier = 2;
  }

  /**
   * Constructs a vector with the given capacity, capacity 
   * increment and capacity mulitplier.
   * @param capacity the vector's initial capacity
   */

  public FastVector(int capacity, int capIncrement, 
		    double capMultiplier) {

    theObjects = new Object[capacity];
    theSize = 0;
    theCapacityIncrement = capIncrement;
    theCapacityMultiplier = capMultiplier;
  }

  /**
   * Adds an element to this vector. Increases its
   * capacity if its not large enough.
   * @param element the element to add
   */

  public final void addElement(Object element) {

    Object[] newObjects;

    if (theSize == theObjects.length) {
      newObjects = new Object[(int)theCapacityMultiplier *
			     (theObjects.length +
			      theCapacityIncrement)];
      System.arraycopy(theObjects, 0, newObjects, 0, theSize);
      theObjects = newObjects;
    }
    theObjects[theSize] = element;
    theSize++;
  }

  /**
   * Returns the capacity of the vector.
   * @return the capacity of the vector
   */

  public final int capacity() {
  
    return theObjects.length;
  }

  /**
   * Produces a shallow copy of this vector.
   * @return the new vector
   */

  public final Object copy() {

    FastVector copy = new FastVector(theObjects.length, 
				     theCapacityIncrement,
				     theCapacityMultiplier);
    copy.theSize = theSize;
    System.arraycopy(theObjects, 0, copy.theObjects, 0, theSize);
    return copy;
  }

  /**
   * Clones the vector and shallow copies all its elements.
   * The elements have to implement the Copyable interface.
   * @return the new vector
   */

  public final Object copyElements() {

    FastVector copy = new FastVector(theObjects.length, 
				     theCapacityIncrement,
				     theCapacityMultiplier);
    copy.theSize = theSize;
    for (int i = 0; i < theSize; i++) 
      copy.theObjects[i] = ((Copyable)theObjects[i]).copy();
    return copy;
  }

  /**
   * Returns the element at the given position.
   * @param index the element's index
   * @return the element with the given index
   */
  
  public final Object elementAt(int index) {

    return theObjects[index];
  }

  /**
   * Returns an enumeration of this vector.
   * @return an enumeration of this vector
   */

  public final Enumeration elements() {
  
    return new FastVectorEnumeration(this);
  }

  /**
   * Returns an enumeration of this vector, skipping the
   * element with the given index.
   * @param index the element to skip
   * @return an enumeration of this vector
   */

  public final Enumeration elements(int index) {
  
    return new FastVectorEnumeration(this, index);
  }

  /**
   * Returns the first element of the vector.
   * @return the first element of the vector
   */

  public final Object firstElement() {

    return theObjects[0];
  }

  /**
   * Searches for the first occurence of the given argument, 
   * testing for equality using the equals method. 
   * @param element the element to be found
   * @return the index of the first occurrence of the argument 
   * in this vector; returns -1 if the object is not found
   */

  public final int indexOf(Object element) {

    for (int i = 0; i < theSize; i++)
      if (element.equals(theObjects[i]))
	return i;
    return -1;
  }

  /**
   * Inserts an element at the given position.
   * @param element the element to be inserted
   * @param index the element's index
   */

  public final void insertElementAt(Object element, int index) {

    Object[] newObjects;

    if (theSize < theObjects.length) {
      for (int i = theSize - 1; i >= index; i--)
	theObjects[i + 1] = theObjects[i];
      theObjects[index] = element;
    } else {
      newObjects = new Object[(int)theCapacityMultiplier *
			     (theObjects.length +
			      theCapacityIncrement)];
      System.arraycopy(theObjects, 0, newObjects, 0, index);
      newObjects[index] = element;
      System.arraycopy(theObjects, index, newObjects, index + 1,
		       theSize - index);
      theObjects = newObjects;
    }
    theSize++;
  }

  /**
   * Returns the last element of the vector.
   * @return the last element of the vector
   */

  public final Object lastElement() {

    return theObjects[theSize - 1];
  }

  /**
   * Deletes an element from this vector.
   * @param index the index of the element to be deleted
   */

  public final void removeElementAt(int index) {

    Object[] newObjects = new Object[theObjects.length];

    System.arraycopy(theObjects, 0, newObjects, 0, index);
    System.arraycopy(theObjects, index + 1, newObjects,
		     index, theObjects.length - (index + 1));
    theObjects = newObjects;
    theSize--;
  }

  /**
   * Removes all components from this vector and sets its 
   * size to zero. 
   */

  public final void removeAllElements() {

    theObjects = new Object[theObjects.length];
    theSize = 0;
  }

  /**
   * Sets the vector's capacity to the given value.
   * @param capacity the new capacity
   */

  public final void setCapacity(int capacity) {

    Object[] newObjects = new Object[capacity];
   
    System.arraycopy(theObjects, 0, newObjects, 0, capacity);
    theObjects = newObjects;
    if (theObjects.length < theSize)
      theSize = theObjects.length;
  }

  /**
   * Sets the element at the given index.
   * @param element the element to be put into the vector
   * @param index the index at which the element is to be placed
   */

  public final void setElementAt(Object element, int index) {

    theObjects[index] = element;
  }

  /**
   * Returns the vector's current size.
   * @return the vector's current size
   */

  public final int size() {

    return theSize;
  }

  /**
   * Swaps two elements in the vector.
   * @param first index of the first element
   * @param second index of the second element
   */

  public final void swap(int first, int second) {

    Object help = theObjects[first];

    theObjects[first] = theObjects[second];
    theObjects[second] = help;
  }

  /**
   * Sets the vector's capacity to its size.
   */

  public final void trimToSize() {

    Object[] newObjects = new Object[theSize];
    
    System.arraycopy(theObjects, 0, newObjects, 0, theSize);
    theObjects = newObjects;
  }
}
