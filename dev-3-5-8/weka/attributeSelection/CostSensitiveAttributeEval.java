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
 * A meta subset evaluator that makes its base subset evaluator cost-sensitive.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -C &lt;cost file name&gt;
 *  File name of a cost matrix to use. If this is not supplied,
 *  a cost matrix will be loaded on demand. The name of the
 *  on-demand file is the relation name of the training data
 *  plus ".cost", and the path to the on-demand file is
 *  specified with the -N option.</pre>
 * 
 * <pre> -N &lt;directory&gt;
 *  Name of a directory to search for cost files when loading
 *  costs on demand (default current directory).</pre>
 * 
 * <pre> -cost-matrix &lt;matrix&gt;
 *  The cost matrix in Matlab single line format.</pre>
 * 
 * <pre> -S &lt;integer&gt;
 *  The seed to use for random number generation.</pre>
 * 
 * <pre> -W
 *  Full name of base evaluator.
 *  (default: weka.attributeSelection.ReliefFAttributeEval)</pre>
 * 
 * <pre> 
 * Options specific to evaluator weka.attributeSelection.ReliefFAttributeEval:
 * </pre>
 * 
 * <pre> -M &lt;num instances&gt;
 *  Specify the number of instances to
 *  sample when estimating attributes.
 *  If not specified, then all instances
 *  will be used.</pre>
 * 
 * <pre> -D &lt;seed&gt;
 *  Seed for randomly sampling instances.
 *  (Default = 1)</pre>
 * 
 * <pre> -K &lt;number of neighbours&gt;
 *  Number of nearest neighbours (k) used
 *  to estimate attribute relevances
 *  (Default = 10).</pre>
 * 
 * <pre> -W
 *  Weight nearest neighbours by distance</pre>
 * 
 * <pre> -A &lt;num&gt;
 *  Specify sigma value (used in an exp
 *  function to control how quickly
 *  weights for more distant instances
 *  decrease. Use in conjunction with -W.
 *  Sensible value=1/5 to 1/10 of the
 *  number of nearest neighbours.
 *  (Default = 2)</pre>
 * 
 <!-- options-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 1.2 $
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
    return RevisionUtils.extract("$Revision: 1.2 $");
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

