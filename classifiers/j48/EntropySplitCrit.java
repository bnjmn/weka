/*
 *    EntropySplitCrit.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.j48;

import weka.core.*;

/**
 * Class for computing the entropy for a given distribution.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public final class EntropySplitCrit extends EntropyBasedSplitCrit {

  /**
   * Computes entropy for given distribution.
   */
  public final double splitCritValue(Distribution bags) {
    
    return newEnt(bags);
  }

  /**
   * Computes entropy of test distribution with respect to training distribution.
   */
  public final double splitCritValue(Distribution train, Distribution test) {

    double result = 0;
    int numClasses = 0;
    int i, j;
    
    // Find out relevant number of classes
    for (j = 0; j < test.numClasses(); j++)
      if (Utils.gr(train.perClass(j), 0) || Utils.gr(test.perClass(j), 0))
	numClasses++;

    // Compute entropy of test data with respect to training data
    for (i = 0; i < test.numBags(); i++)
      if (Utils.gr(test.perBag(i),0)) {
	for (j = 0; j < test.numClasses(); j++)
	  if (Utils.gr(test.perClassPerBag(i, j), 0))
	    result -= test.perClassPerBag(i, j)*
	      Math.log(train.perClassPerBag(i, j) + 1);
	result += test.perBag(i) * Math.log(train.perBag(i) + numClasses);
      }
  
    return result / log2;
  }
}




