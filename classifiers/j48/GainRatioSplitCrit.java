/*
 *    GainRatioSplitCrit.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.j48;

import weka.core.*;

/**
 * Class for computing the gain ratio for a given distribution.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public final class GainRatioSplitCrit extends EntropyBasedSplitCrit{

  /**
   * This method is a straightforward implementation of the gain
   * ratio criterion for the given distribution.
   */
  public final double splitCritValue(Distribution bags) {

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
   *
   * @param bags the distribution
   * @param totalnoInst the weight of ALL instances
   * @param numerator the info gain
   */
  public final double splitCritValue(Distribution bags, double totalnoInst,
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
  
  /**
   * Help method for computing the split entropy.
   */
  private final double splitEnt(Distribution bags,double totalnoInst){
    
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




