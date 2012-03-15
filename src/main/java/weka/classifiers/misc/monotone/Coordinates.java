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
 *    Coordinates.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc.monotone;

import weka.core.Instance;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

import java.io.Serializable;

/**
 * This is a simple implementation of the data space.  The <code>
 * Coordinates </code> holds the internal weka value of an instance,
 * but <i> with the class removed </i>.  The class is immutable,
 * and works best when all attibutes are nominal (ordinal), although
 * it will not give an error when working with numeric attributes.
 * In the latter case, performance will degrade because of the way
 * in which the hashcode is calculated.
 * 
 * <p>
 * This implementation is part of the master's thesis: "Studie
 * en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd
 * rangschikken", Stijn Lievens, Ghent University, 2004. 
 * </p>
 * 
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision$
 */
public class Coordinates
  implements Serializable, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = 2319016195345994738L;

  /**
   * The internal weka values of the attributes of the instance, minus
   * the class attribute.
   */
  private int[] m_coord;

  /**
   * The hashcode of this object.  Calculated during construction.
   */
  private int m_hashCode;

  /**
   * Create the <code> Coordinates </code> for the given instance.
   *
   * @param instance the <code> Instance </code> on which the <code>
   * Coordinates </code> will be based
   */
  public Coordinates(Instance instance) {

    double[] values = instance.toDoubleArray();
    int classIndex = instance.classIndex();
    if (classIndex == -1) {
      m_coord = new int[values.length];
    } else {
      m_coord = new int[values.length - 1];
    }

    m_hashCode = 0;
    int factor=1;
    for (int i = 0,j = 0;i < values.length; i++) { 
      if (i != classIndex) {
	m_coord[j] = (int) values[i];
	if (i > 0 && i - 1 != classIndex) {
	  factor *= instance.attribute(i-1).numValues();
	} else if (i - 1 == classIndex && classIndex != -1 
	    && classIndex != 0) {
	  factor *= instance.attribute(i - 2).numValues();
	}
	m_hashCode += (m_coord[j])*factor;
	j++;    
      }
    }
  }

  /**
   * Get the value of the attribute with index <code> index, </code>
   * ignoring the class attribute.  Indices are counted starting from
   * 0.  
   *
   * @param index the index of the requested attribute
   * @return the value of this attribute, in internal floating point format.
   */
  public double getValue(int index) {
    return m_coord[index];
  }

  /**
   * Get the values of the coordinates.
   * 
   * @param values array serving as output, and the first 
   * <code> dimension() </code> values are filled in.
   */
  public void getValues(double[] values) {
    // XXX this is a rather strange method, maybe it should be changed
    // into one that returns (using System.arraycopy) the requested values
    for (int i = 0; i < m_coord.length; i++) {
      values[i] = m_coord[i];
    }
  }

  /**
   * Indicates if the object <code> o </code> equals <code> this. </code>
   *
   * @param o the reference object with which to compare
   * @return <code> true </code> if <code> o </code> equals <code>
   * this, </code> <code> false </code> otherwise
   */
  public boolean equals(Object o) {
    if (! (o instanceof Coordinates) ) {
      return false;
    }

    Coordinates cc = (Coordinates) o;
    // if the length or hashCodes differ, the objects are certainly not equal
    if (m_coord.length != cc.m_coord.length || m_hashCode != cc.m_hashCode) {
      return false;
    }

    for (int i = 0; i < m_coord.length; i++) {
      if (m_coord[i] != cc.m_coord[i]) {
	return false;
      }
    }
    return true;
  }

  /** 
   * Checks if <code> this </code> is strictly smaller than <code> cc. </code>
   * This means that for all indices i it holds that 
   * <code> this.getValue(i) &lt;= cc.getValue(i) </code> and that there is
   * at least one index i such that 
   * <code> this.getValue(i) &ne; cc.getValue(i) </code>
   * 
   * @param cc the <code> Coordinates </code> that <code> this </code> is
   * compared to
   * @return <code> true </code> if <code> this </code> is strictly
   * smaller than <code> cc, </code> <code> false </code> otherwise
   * @throws IllegalArgumentException if the dimensions of both objects differ
   */
  public boolean strictlySmaller(Coordinates cc) throws IllegalArgumentException {
    if (cc.m_coord.length != m_coord.length) { 
      throw new IllegalArgumentException
      ("Coordinates are not from the same space");
    }

    // Skip all equal values
    int i = 0;
    while(i < m_coord.length && cc.m_coord[i] == m_coord[i]) { 
      i++;
    }

    if (i == m_coord.length) {
      return false; // equality !
    }

    for (; i < m_coord.length; i++) {
      if (m_coord[i] > cc.m_coord[i]) { 
	return false;
      }
    }
    return true;
  }

  /** 
   * Checks if <code> this </code> is smaller or equal than <code> cc. </code>
   * This means that for all indices i it holds that 
   * <code> this.getValue(i) &lt;= cc.getValue(i). </code>
   * 
   * @param cc the <code> Coordinates </code> that <code> this </code> is
   * compared to
   * @return <code> true </code> if <code> this </code> is
   * smaller or equal than <code> cc, </code> <code> false </code> otherwise
   * @throws IllegalArgumentException if the dimensions of both objects differ
   */
  public boolean smallerOrEqual(Coordinates cc) throws IllegalArgumentException {
    if (cc.m_coord.length != m_coord.length) { 
      throw new IllegalArgumentException
      ("Coordinates are not from the same space");
    }
    for (int i = 0; i < m_coord.length; i++) {
      if (m_coord[i] > cc.m_coord[i]) { 
	return false;
      }
    }
    return true;
  }

  /**
   * Gets the hash code value for this object.
   *
   * @return the requested hash code
   */
  public int hashCode() {
    return m_hashCode;
  }

  /** 
   * Gets the dimension of the data space, this is the number of attributes,
   * exluding the class attribute.
   * 
   * @return the dimension of the data space this object resides in
   */
  public int dimension() {
    return m_coord.length;
  }

  /**
   * Get a string representation of this object.
   *
   * @return the requested string representation
   */
  public String toString() {
    String s = "(";
    for (int i = 0; i < m_coord.length - 1; i++) {
      s += m_coord[i] + ",";
    }
    return s + m_coord[m_coord.length - 1] + ")";
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
