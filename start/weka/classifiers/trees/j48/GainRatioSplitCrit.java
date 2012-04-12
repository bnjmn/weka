/*
 *    GainRatioSplitCrit.java
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

package weka.classifiers.j48;

import weka.core.*;

/**
 * Class for computing the gain ratio for a given distribution.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public final class GainRatioSplitCrit extends EntropyBasedSplitCrit{

  // ===============
  // Public methods.
  // ===============

  /**
   * This method is a straightforward implementation of the gain
   * ratio criterion.
   */

  public final double splitCritValue(Distribution bags){

    double numerator;
    double denumerator;
    
    numerator = oldEnt(bags)-newEnt(bags);

    // Splits with no gain are useless.

    if (Utils.eq(numerator,0))
      return Double.MAX_VALUE;
    denumerator = splitEnt(bags);
    
    // Test if split is trivial.

    if (Utils.eq(denumerator,0))
      return Double.MAX_VALUE;
    
    //  We take the reciprocal value because we want to minimize the
    // splitting criterion's value.

    return denumerator/numerator;
  }

  /**
   * This method computes the gain ratio in the same way C4.5 does.
   */

  public final double splitCritValue(Distribution bags,double totalnoInst,
				     double numerator){
    
    double denumerator;
    double noUnknown;
    double unknownRate;
    int i;
    
    // Compute split info.
    
    denumerator = splitEnt(bags,totalnoInst);
        
    // Test if split is trivial.

    if (Utils.eq(denumerator,0))
      return 0;  
    denumerator = denumerator/totalnoInst;

    return numerator/denumerator;
  }
  
  public final double splitEnt(Distribution bags,double totalnoInst){
    
    double returnValue = 0;
    double noUnknown;
    int i;
    
    noUnknown = totalnoInst-bags.total();
    if (Utils.gr(bags.total(),0)){
      for (i=0;i<bags.numBags();i++)
	returnValue = returnValue-logFunc(bags.perBag(i));
      returnValue = returnValue-logFunc(noUnknown);
      returnValue = returnValue+logFunc(totalnoInst);
    }
    return returnValue;
  }
}




