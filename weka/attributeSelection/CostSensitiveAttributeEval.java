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
 *    CostSensitiveAttributeEval.java
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
public class CostSensitiveAttributeEval
  extends CostSensitiveASEvaluation
  implements Serializable, AttributeEvaluator, OptionHandler {

  /** For serialization */
  static final long serialVersionUID = 4484876541145458447L;

  /**
   * Default constructor.
   */
  public CostSensitiveAttributeEval() {
    setEvaluator(new ReliefFAttributeEval());
  }
 
  /**
   * Return the name of the default evaluator.
   *
   * @return the name of the default evaluator
   */
  public String defaultEvaluatorString() {
    return "weka.attributeSelection.ReliefFAttributeEval";
  }

  /**
   * Set the base evaluator.
   *
   * @param newEvaluator the evaluator to use.
   * @throws IllegalArgumentException if the evaluator is not an instance of AttributeEvaluator
   */
  public void setEvaluator(ASEvaluation newEvaluator) throws IllegalArgumentException {
    if (!(newEvaluator instanceof AttributeEvaluator)) {
      throw new IllegalArgumentException("Evaluator must be an AttributeEvaluator!");
    }

    m_evaluator = newEvaluator;
  }

  /**
   * Evaluates an individual attribute. Delegates the actual evaluation to the
   * base attribute evaluator.
   *
   * @param attribute the index of the attribute to be evaluated
   * @return the "merit" of the attribute
   * @exception Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute(int attribute) throws Exception {
    return ((AttributeEvaluator)m_evaluator).evaluateAttribute(attribute);
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
    runEvaluator(new CostSensitiveAttributeEval(), args);
  }
}