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
 *    InfoGainSplitMetric.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import weka.core.ContingencyTables;
import weka.core.Utils;

/**
 * Implements the info gain splitting criterion
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class InfoGainSplitMetric extends SplitMetric implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 2173840581308675428L;

  protected double m_minFracWeightForTwoBranches;

  public InfoGainSplitMetric(double minFracWeightForTwoBranches) {
    m_minFracWeightForTwoBranches = minFracWeightForTwoBranches;
  }

  @Override
  public double evaluateSplit(Map<String, WeightMass> preDist,
      List<Map<String, WeightMass>> postDist) {

    double[] pre = new double[preDist.size()];
    int count = 0;
    for (Map.Entry<String, WeightMass> e : preDist.entrySet()) {
      pre[count++] = e.getValue().m_weight;
    }

    double preEntropy = ContingencyTables.entropy(pre);

    double[] distWeights = new double[postDist.size()];
    double totalWeight = 0.0;
    for (int i = 0; i < postDist.size(); i++) {
      distWeights[i] = SplitMetric.sum(postDist.get(i));
      totalWeight += distWeights[i];
    }

    int fracCount = 0;
    for (double d : distWeights) {
      if (d / totalWeight > m_minFracWeightForTwoBranches) {
        fracCount++;
      }
    }

    if (fracCount < 2) {
      return Double.NEGATIVE_INFINITY;
    }

    double postEntropy = 0;
    for (int i = 0; i < postDist.size(); i++) {
      Map<String, WeightMass> d = postDist.get(i);
      double[] post = new double[d.size()];
      count = 0;
      for (Map.Entry<String, WeightMass> e : d.entrySet()) {
        post[count++] = e.getValue().m_weight;
      }
      postEntropy += distWeights[i] * ContingencyTables.entropy(post);
    }

    if (totalWeight > 0) {
      postEntropy /= totalWeight;
    }

    return preEntropy - postEntropy;
  }

  @Override
  public double getMetricRange(Map<String, WeightMass> preDist) {

    int numClasses = preDist.size();
    if (numClasses < 2) {
      numClasses = 2;
    }

    return Utils.log2(numClasses);
  }

}
