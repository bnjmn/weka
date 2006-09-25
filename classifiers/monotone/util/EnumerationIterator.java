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
 *    EnumerationIterator.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.monotone.util;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** 
 * Implementation of a simple wrapper class for the <code> Enumeration </code>
 * interface.
 * This makes it possible to use an <code> Enumeration </code> as if
 * it were an <code> Iterator. </code>
 * <p>
 * This implementation is part of the master's thesis: "Studie
 * en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd
 * rangschikken", Stijn Lievens, Ghent University, 2004. 
 * </p>
 * 
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision: 1.1 $
 */
public class EnumerationIterator
  implements Iterator {

  private Enumeration e;

  /**
   * Construct an <code> EnumerationIterator </code> on basis of on
   * <code> Enumeration. </code>
   * 
   * @param e the <code> Enumeration </code> on which the 
   * <code> Iterator </code> will be based
   */
  public EnumerationIterator(Enumeration e) {
    this.e = e;
  }

  /**
   * Returns <code> true </code> if there are more elements in the iteration.
   *
   * @return <code> true </code> if there are more elements in the iteration,
   * <code> false </code> otherwise
   */
  final public boolean hasNext() {
    return e.hasMoreElements();
  }

  /**
   * Returns the next element in the iteration.
   *
   * @return the next element in the iteration
   * @throws NoSuchElementException if the requested element does  not exist
   */
  final public Object next() throws NoSuchElementException  {
    return e.nextElement();
  }

  /**
   * Since the iteration is based on an enumeration, removal of elements
   * is not supported.
   *
   * @throws UnsupportedOperationException every time this method is invoked
   */
  final public void remove() {
    throw new UnsupportedOperationException();
  }
}
