/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    CostSensitiveSubsetEval.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
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
 *  (default: weka.attributeSelection.CfsSubsetEval)</pre>
 * 
 * <pre> 
 * Options specific to evaluator weka.attributeSelection.CfsSubsetEval:
 * </pre>
 * 
 * <pre> -M
 *  Treat missing values as a seperate value.</pre>
 * 
 * <pre> -L
 *  Don't include locally predictive attributes.</pre>
 * 
 <!-- options-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
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
    return RevisionUtils.extract("$Revision$");
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

