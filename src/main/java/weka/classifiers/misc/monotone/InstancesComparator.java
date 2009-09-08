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
 *    InstancesComparator.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc.monotone;

import weka.core.Instance;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

import java.util.Comparator;

/**
 * Class to compare instances with respect to a given attribute, indicated
 * by its index.  The ordering of the attribute values is determined
 * by the internal values of WEKA.  There is also the possibility of
 * reversing this order.
 * <p>
 * This implementation is part of the master's thesis: "Studie
 * en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd
 * rangschikken", Stijn Lievens, Ghent University, 2004. 
 * </p>
 * 
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision$
 */
public class InstancesComparator
  implements Comparator, RevisionHandler {

  /** index of the attribute  */
  private int m_Index;
  
  /** If 1 then the order is not reversed, when -1, the order is reversed */
  private int m_Reverse = 1;

  /**
   * Construct an <code> InstancesComparator </code> that compares 
   * the attributes with the given index.
   *
   * @param index the index on which to compare instances
   */
  public InstancesComparator(int index) {
    m_Index = index;
  }

  /**
   * Construct an <code> InstancesComparator </code> that compares 
   * the attributes with the given index, with the possibility of
   * reversing the order.
   *
   * @param index the index on which to compare instances
   * @param reverse if <code> true </code> the order is reversed, if
   * <code> false </code> the order is not reversed
   */
  public InstancesComparator(int index, boolean reverse) {
    m_Index = index;
    m_Reverse = (reverse == true) ? -1 : 1;
  }

  /**
   * Compares two objects (instances) with respect to the attribute
   * this comparator is constructed on.
   *
   * @param o1 the first object to be compared
   * @param o2 the second object to be compared
   * @return -1 if <code> o1 &lt; o2 </code> (wrt to the given attribute),
   * 1 if <code> o1 &gt; o2 </code>, and 0 if <code> o1 </code> and
   * <code> o2 </code> are equal (wrt to the given attribute)
   */
  public int compare(Object o1, Object o2) {
    Instance i1 = (Instance) o1;
    Instance i2 = (Instance) o2;

    if (i1.value(m_Index) < i2.value(m_Index)) {
      return -1 * m_Reverse;
    } else if (i1.value(m_Index) > i2.value(m_Index)) {
      return 1 * m_Reverse;
    }
    return 0;
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
