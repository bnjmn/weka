/*
 *    SubsetEvaluator.java
 *    Copyright (C) 1999 Mark Hall
 *
 */

package weka.attributeSelection;

import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * Abstract attribute subset evaluator.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
public abstract class SubsetEvaluator extends ASEvaluation {

  // ===============
  // Public methods.
  // ===============
  
  /**
   * evaluates a subset of attributes
   *
   * @param subset a bitset representing the attribute subset to be 
   * evaluated 
   * @return the "merit" of the subset
   * @exception Exception if the subset could not be evaluated
   */
  public abstract double evaluateSubset(BitSet subset) throws Exception;
}

