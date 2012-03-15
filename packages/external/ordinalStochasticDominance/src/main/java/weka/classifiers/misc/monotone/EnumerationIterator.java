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
 *    EnumerationIterator.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc.monotone;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

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
 * @version $Revision$
 */
public class EnumerationIterator
  implements Iterator, RevisionHandler {

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
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
