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
 *    DynamicArrayOfPosInt.java
 *    Copyright (C) 2002 Gabi Schmidberger
 *
 */

package weka.core;

/** 
 * Implements a dynamic array of positive integers.
 *
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class DynamicArrayOfPosInt {

  /* An array to hold the data. */
  private int[] m_Data; 

  /* The real length of array */
  private int m_Length = 0;

  /* The used length of array */
  private int m_Used = 0;
  
  /**
   * Constructor
   */
  public DynamicArrayOfPosInt() {
    m_Data = new int[1];  // Array will grow as necessary.
    m_Length = 1;
    m_Used = 0;
  }

  /**
   * Constructor with size larger than 1.
   * @param size initial size of the array
   */
  public DynamicArrayOfPosInt(int size) {
    m_Data = new int[size];  
    m_Length = size;
    m_Used = 0;
  }

  /*
   * Return length of array.
   * @return used length of array
   */
  public int length() {
    return m_Used;
  }

  /*
   * Get value from position in array.
   * When the specified position lies outside the actual physical size
   * of the data array, a value of -1 is returned.
   * @param position to get the data from
   * @return value of the array at the given position
   */
  public int get(int position) {

    if (position >= length())
      return -1;
    else
      return m_Data[position];
  }
  
  /**
   * Stores the value in the specified position in the array.
   * The data array will increase in size to include this
   * position, if necessary.
   * @param position position to write value to
   * @param value value that is written to array
   */
  public void set(int position, int value) {

    // attempt to write beyond used length 
    if (position >= m_Used)
      m_Used = position + 1;

    // attempt to write beyond used length available memory
    if (position >= m_Data.length) {
      // The specified position is outside the actual size of
      // the data array.  Double the size, or if that still does
      // not include the specified position, set the new size
      // to 2*position. 
      int newSize = 2 * m_Data.length;
      if (position >= newSize)
	newSize = 2 * position;
      int[] newData = new int[newSize];
      System.arraycopy(m_Data, 0, newData, 0, m_Data.length);
      m_Data = newData;
      m_Length = newSize;
    }
    m_Data[position] = value;
  }
  
  /**
   * Deletes an entry. The positive integers are here understood as indices.
   * Every indices is in the array only once. 
   * So if one is deleted all the others with a value higher have to
   * have substracted a 1.  
   * @param value value that is to be deleted
   */
  public int deleteOneIndex(int value) {

    int position = -1;
    int i = 0;
    for (; i < m_Used && m_Data[i] != value; i++) {
      if (m_Data[i] > value) m_Data[i] = m_Data[i] - 1;
    }
    if (i < m_Used) {
      position = i;
      System.arraycopy(m_Data, i + 1, 
		       m_Data, i, m_Used - i - 1);
                                     
      m_Used = m_Used - 1;
      for (; i < m_Used; i++) {
	if (m_Data[i] > value) m_Data[i] = m_Data[i] - 1;
      }
    }
    return position;
  }
  
  /**
   * Sqeezes in the value in the specified position in the array.
   * The used array size is increased by one and also the data array 
   * increases in size.
   * @param position position to write value to
   * @param value value that is written to array
   */
  public void squeezeIn(int position, int value) {

    int lengthOfRightPart = m_Length - position;

    // move the last value one further
    // so m_Length does grow by one and 
    // actual length might increase too
    int positionOfLast = m_Length - 1;
    set(m_Length, get(positionOfLast)); 

    // copy the right half away one to the right
    System.arraycopy(m_Data, position, m_Data, position + 1, 
		     lengthOfRightPart);

    // write new value to its position
    set(position, value);
  }      

  /**
   * Build a string representing this array.
   */
  public String toString() {
    
    String result = " ";
    for (int i = 0; i < m_Length; i++) {
      result += "[" + m_Data[i] + "]";
    }
    result += "\n";
    return result;
  } 
}









