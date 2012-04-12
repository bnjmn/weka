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
 *    EnsembleLibraryModelComparator.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.classifiers;

import java.io.Serializable;
import java.util.Comparator;

/**
 * This class is a Comparator for the LibraryModel class. It basically ranks
 * them alphabetically based on their String Representations
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.1 $
 */
public final class EnsembleLibraryModelComparator 
  implements Comparator, Serializable {
  
  /** for serialization */
  private static final long serialVersionUID = -6522464740036141188L;

  /**
   * Compares its two arguments for order.
   * 
   * @param o1		first object
   * @param o2		second object
   * @return		a negative integer, zero, or a positive integer as the 
   * 			first argument is less than, equal to, or greater than 
   * 			the second.
   */
  public int compare(Object o1, Object o2) {
    
    int comparison = 0;
    
    if (o1 instanceof EnsembleLibraryModel
	&& o2 instanceof EnsembleLibraryModel) {
      
      comparison = ((String) ((EnsembleLibraryModel) o1)
	  .getStringRepresentation())
	  .compareTo(((String) ((EnsembleLibraryModel) o2)
	      .getStringRepresentation()));
      
    }
    
    return comparison;
  }
}