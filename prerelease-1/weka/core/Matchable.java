/*
 *    Matchable.java
 *    Copyright (C) 1999 Len Trigg
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

/** 
 * Interface to something that can be matched with the tree matching
 * algorithms.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version 1.0 - 17 Mar 1999 - Initial version
 */
public interface Matchable {

  // ===============
  // Public methods.
  // ===============

  /**
   * Returns a string that describes a tree representing
   * the object in prefix order.
   *
   * @return the tree described as a string
   * @exception Exception if the tree can't be computed
   */
  String prefix() throws Exception;
}








