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
 *    CostSensitiveSubsetEval.java
 *    Copyright (C) 2008 Pentaho Corporation
 *
 */

package  weka.attributeSelection;

import weka.core.OptionHandler;
import weka.core.RevisionUtils;

import java.util.BitSet;
import java.io.Serializable;

/**
 <!-- globalinfo-start -->
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 <!-- options-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 1.1 $
 */
public class CostSensitiveSubsetEval
  extends CostSensitiveASEvaluation
  implements Serializable, SubsetEvaluator, OptionHandler {

  /** For serialization */
  static final long serialVersionUID = 2924546096103426700L;

  /**
   * Default constructor.
   */
  public CostSensitiveSubsetEval() {
    setEvaluator(new CfsSubsetEval());
  }

  /**
   * Set the base evaluator.
   *
   * @param newEvaluator the evaluator to use.
   * @throws IllegalArgumentException if the evaluator is not an instance of SubsetEvaluator
   */
  public void setEvaluator(ASEvaluation newEvaluator) throws IllegalArgumentException {
    if (!(newEvaluator instanceof SubsetEvaluator)) {
      throw new IllegalArgumentException("Evaluator must be an SubsetEvaluator!");
    }

    m_evaluator = newEvaluator;
  }

  /**
   * Evaluates a subset of attributes. Delegates the actual evaluation to
   * the base subset evaluator.
   *
   * @param subset a bitset representing the attribute subset to be 
   * evaluated 
   * @return the "merit" of the subset
   * @exception Exception if the subset could not be evaluated
   */
  public double evaluateSubset(BitSet subset) throws Exception {
    return ((SubsetEvaluator)m_evaluator).evaluateSubset(subset);
  }

  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.1 $");
  }

  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {
    runEvaluator(new CostSensitiveSubsetEval(), args);
  }
}
