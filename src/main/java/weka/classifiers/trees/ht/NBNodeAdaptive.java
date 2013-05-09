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
 *    NBNodeAdaptive.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.io.Serializable;
import java.util.Map;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Implements a LearningNode that chooses between using majority class or naive
 * Bayes for prediction
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class NBNodeAdaptive extends NBNode implements LearningNode,
    Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -4509802312019989686L;

  /** The number of correct predictions made by the majority class */
  protected double m_majClassCorrectWeight = 0;

  /** The number of correct predictions made by naive Bayes */
  protected double m_nbCorrectWeight = 0;

  /**
   * Constructor
   * 
   * @param header the structure of the instances we're training from
   * @param nbWeightThreshold the weight mass to see before allowing naive Bayes
   *          to predict
   * @throws Exception if a problem occurs
   */
  public NBNodeAdaptive(Instances header, double nbWeightThreshold)
      throws Exception {
    super(header, nbWeightThreshold);
  }

  protected String majorityClass() {
    String mc = "";
    double max = -1;

    for (Map.Entry<String, WeightMass> e : m_classDistribution.entrySet()) {
      if (e.getValue().m_weight > max) {
        max = e.getValue().m_weight;
        mc = e.getKey();
      }
    }

    return mc;
  }

  @Override
  public void updateNode(Instance inst) throws Exception {

    String trueClass = inst.classAttribute().value((int) inst.classValue());
    int trueClassIndex = (int) inst.classValue();

    if (majorityClass().equals(trueClass)) {
      m_majClassCorrectWeight += inst.weight();
    }

    if (m_bayes.classifyInstance(inst) == trueClassIndex) {
      m_nbCorrectWeight += inst.weight();
    }

    super.updateNode(inst);
  }

  @Override
  public double[] getDistribution(Instance inst, Attribute classAtt)
      throws Exception {

    if (m_majClassCorrectWeight > m_nbCorrectWeight) {
      return super.bypassNB(inst, classAtt);
    }

    return super.getDistribution(inst, classAtt);
  }

  @Override
  protected int dumpTree(int depth, int leafCount, StringBuffer buff) {
    leafCount = super.dumpTree(depth, leafCount, buff);

    buff.append(" NB adaptive" + m_leafNum);

    return leafCount;
  }

  @Override
  protected void printLeafModels(StringBuffer buff) {
    buff.append("NB adaptive" + m_leafNum).append("\n")
        .append(m_bayes.toString());
  }

}
