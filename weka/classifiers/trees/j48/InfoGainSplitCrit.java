/*
 *    InfoGainSplitCrit.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.j48;

import weka.core.*;

/**
 * Class for computing the information gain for a given distribution.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public final class InfoGainSplitCrit extends EntropyBasedSplitCrit{

  /**
   * This method is a straightforward implementation of the information
   * gain criterion for the given distribution.
   */
  public final double splitCritValue(Distribution bags) {

    double numerator;
        
    numerator = oldEnt(bags)-newEnt(bags);

    // Splits with no gain are useless.
    if (Utils.eq(numerator,0))
      return Double.MAX_VALUE;
        
    // We take the reciprocal value because we want to minimize the
    // splitting criterion's value.
    return bags.total()/numerator;
  }

  /**
   * This method computes the information gain in the same way 
   * C4.5 does.
   *
   * @param distribution the distribution
   * @param totalNoInst weight of ALL instances (including the
   * ones with missing values).
   */
  public final double splitCritValue(Distribution bags,double totalNoInst) {
    
    double numerator;
    double noUnknown;
    double unknownRate;
    int i;
    
    noUnknown = totalNoInst-bags.total();
    unknownRate = noUnknown/totalNoInst;
    numerator = (oldEnt(bags)-newEnt(bags));
    numerator = (1-unknownRate)*numerator;
    
    // Splits with no gain are useless.
    if (Utils.eq(numerator,0))
      return 0;
    
    return numerator/bags.total();
  }

  /**
   * This method computes the information gain in the same way 
   * C4.5 does.
   *
   * @param distribution the distribution
   * @param totalNoInst weight of ALL instances 
   * @param oldEnt entropy with respect to "no-split"-model.
   */
  public final double splitCritValue(Distribution bags,double totalNoInst,
                                     double oldEnt) {
    
    double numerator;
    double noUnknown;
    double unknownRate;
    int i;
    
    noUnknown = totalNoInst-bags.total();
    unknownRate = noUnknown/totalNoInst;
    numerator = (oldEnt-newEnt(bags));
    numerator = (1-unknownRate)*numerator;
    
    // Splits with no gain are useless.
    if (Utils.eq(numerator,0))
      return 0;
    
    return numerator/bags.total();
  }
}









