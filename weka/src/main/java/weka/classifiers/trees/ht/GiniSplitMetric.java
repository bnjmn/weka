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
 *    GiniSplitMetric.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Implements the gini splitting criterion
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class GiniSplitMetric extends SplitMetric implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -2037586582742660298L;

  @Override
  public double evaluateSplit(Map<String, WeightMass> preDist,
      List<Map<String, WeightMass>> postDist) {
    double totalWeight = 0.0;
    double[] distWeights = new double[postDist.size()];

    for (int i = 0; i < postDist.size(); i++) {
      distWeights[i] = SplitMetric.sum(postDist.get(i));
      totalWeight += distWeights[i];
    }
    double gini = 0;
    for (int i = 0; i < postDist.size(); i++) {
      gini += (distWeights[i] / totalWeight)
          * gini(postDist.get(i), distWeights[i]);
    }

    return 1.0 - gini;
  }

  /**
   * Return the gini metric computed from the supplied distribution
   * 
   * @param dist the distribution to compute the gini metric from
   * @param sumOfWeights the sum of the distribution weights
   * @return the gini metric
   */
  protected static double gini(Map<String, WeightMass> dist, double sumOfWeights) {
    double gini = 1.0;

    for (Map.Entry<String, WeightMass> e : dist.entrySet()) {
      double frac = e.getValue().m_weight / sumOfWeights;
      gini -= frac * frac;
    }

    return gini;
  }

  /**
   * Return the gini metric computed from the supplied distribution
   * 
   * @param dist dist the distribution to compute the gini metric from
   * @return
   */
  public static double gini(Map<String, WeightMass> dist) {
    return gini(dist, SplitMetric.sum(dist));
  }

  @Override
  public double getMetricRange(Map<String, WeightMass> preDist) {
    return 1.0;
  }
}
