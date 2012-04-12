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
 *    MultiDimensionalSort.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc.monotone;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

import java.util.Arrays;
import java.util.Comparator;

/** 
 * Class for doing multidimensional sorting, using an array of
 * <code> Comparator. </code>
 * The goal is to sort an array topologically.  If <code> o1 </code>
 * and <code> o2 </code> are two objects of the array <code> a, </code>
 * and for all valid indices <code> i </code> in the array <code> c </code>
 * if holds that <code> c[i].compare(o1,o2) &lt; 0 </code> then
 * <code> o1 </code> comes before <code> o2 </code> in the sorted array.
 * <p>
 * A typical is the sorting of vectors in an n-dimensional space,
 * where the ordering is determined by the product ordering.
 * </p>
 * <p>
 * This implementation is part of the master's thesis: "Studie
 * en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd
 * rangschikken", Stijn Lievens, Ghent University, 2004. 
 * </p>
 * 
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision$
 */
public class MultiDimensionalSort
  implements RevisionHandler {

  /** 
   * Sort an array using different comparators.
   *
   * @param a the array to be sorted.  The sorted array is returned 
   * in the array <code> a </code> itself.
   * @param c an array holding the different comparators
   */
  public static void multiDimensionalSort (Object[] a, Comparator[] c) {
    multiDimensionalSort(a, 0, a.length, c);
  }

  /** 
   * Sort part of an array using different comparators.
   * 
   * @param a the array to be sorted, the indicated part of the array will
   * be replaced by the sorted elements
   * @param fromIndex index of the first element to be sorted (inclusive)
   * @param toIndex index of the last element to be sorted (exclusive)
   * @param c array holding the different comparators
   * @throws IllegalArgumentException if <code> fromIndex &gt; toIndex </code>
   */
  public static void multiDimensionalSort(Object[] a, int fromIndex,
      int toIndex, Comparator[] c) 
    throws IllegalArgumentException {
    
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException
      ("Illegal range: fromIndex can be at most toIndex");
    }
    multiDimensionalSort(a, fromIndex, toIndex, c, 0);
  }

  /**
   * Do the actual sorting in a recursive way.
   * 
   * @param a the array to be sorted, the indicated part of the array will
   * be replaced by the sorted elements
   * @param fromIndex index of the first element to be sorted (inclusive)
   * @param toIndex index of the last element to be sorted (exclusive)
   * @param c array holding the different comparators
   * @param depth the index of the comparator to use
   */
  private static void multiDimensionalSort(Object[] a, int fromIndex,
      int toIndex, Comparator[] c, int depth) {

    if (depth == c.length) {
      return; // maximum depth reached
    }

    Comparator comp = c[depth]; // comparator to use
    Arrays.sort(a, fromIndex, toIndex, comp); // sort part of the array 

    // look for breakpoints in the array and sort recursively
    int mark = fromIndex;
    for (int i = fromIndex + 1; i < toIndex; i++) {
      if (comp.compare(a[i - 1], a[i]) != 0) {
	// a[i] is different from a[i-1], breakpoint detected
	multiDimensionalSort(a, mark, i, c, depth + 1);
	mark = i;
      }
    }
    // sort the last part 
    multiDimensionalSort(a, mark, toIndex, c, depth + 1);
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
