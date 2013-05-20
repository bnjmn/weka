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
 *    SplitMetric.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Base class for split metrics
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class SplitMetric implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 2891555018707080818L;

  /**
   * Utility method to return the sum of instance weight in a distribution
   * 
   * @param dist the distribution
   * @return the sum of the weights contained in a distribution
   */
  public static double sum(Map<String, WeightMass> dist) {
    double sum = 0;

    for (Map.Entry<String, WeightMass> e : dist.entrySet()) {
      sum += e.getValue().m_weight;
    }

    return sum;
  }

  /**
   * Evaluate the merit of a split
   * 
   * @param preDist the class distribution before the split
   * @param postDist the class distributions after the split
   * @return the merit of the split
   */
  public abstract double evaluateSplit(Map<String, WeightMass> preDist,
      List<Map<String, WeightMass>> postDist);

  /**
   * Get the range of the splitting metric
   * 
   * @param preDist the pre-split class distribution
   * @return the range of the splitting metric
   */
  public abstract double getMetricRange(Map<String, WeightMass> preDist);
}
