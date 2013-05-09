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
 *    NBNode.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.io.Serializable;

import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Implements a LearningNode that uses a naive Bayes model
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class NBNode extends ActiveHNode implements LearningNode, Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -1872415764817690961L;

  /** The naive Bayes model at the node */
  protected NaiveBayesUpdateable m_bayes;

  /**
   * The weight of instances that need to be seen by this node before allowing
   * naive Bayes to make predictions
   */
  protected double m_nbWeightThreshold;

  /**
   * Construct a new NBNode
   * 
   * @param header the instances structure of the data we're learning from
   * @param nbWeightThreshold the weight mass to see before allowing naive Bayes
   *          to predict
   * @throws Exception if a problem occurs
   */
  public NBNode(Instances header, double nbWeightThreshold) throws Exception {
    m_nbWeightThreshold = nbWeightThreshold;
    m_bayes = new NaiveBayesUpdateable();
    m_bayes.buildClassifier(header);
  }

  @Override
  public void updateNode(Instance inst) throws Exception {
    super.updateNode(inst);

    try {
      m_bayes.updateClassifier(inst);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected double[] bypassNB(Instance inst, Attribute classAtt)
      throws Exception {
    return super.getDistribution(inst, classAtt);
  }

  @Override
  public double[] getDistribution(Instance inst, Attribute classAtt)
      throws Exception {

    // totalWeight - m_weightSeenAtLastSplitEval is the weight mass
    // observed by this node's NB model

    boolean doNB = m_nbWeightThreshold == 0 ? true : (totalWeight()
        - m_weightSeenAtLastSplitEval > m_nbWeightThreshold);

    if (doNB) {
      return m_bayes.distributionForInstance(inst);
    }

    return super.getDistribution(inst, classAtt);
  }

  @Override
  protected int dumpTree(int depth, int leafCount, StringBuffer buff) {
    leafCount = super.dumpTree(depth, leafCount, buff);

    buff.append(" NB" + m_leafNum);

    return leafCount;
  }

  @Override
  protected void printLeafModels(StringBuffer buff) {
    buff.append("NB" + m_leafNum).append("\n").append(m_bayes.toString());
  }
}
