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
 *    NextSplitHeuristic.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi.miti;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import weka.core.Instance;

/**
 * Implements the node selection heuristic.
 *
 * @author Luke Bjerring
 * @version $Revision$
 */
public class NextSplitHeuristic implements Comparator<TreeNode> {	

  /**
   * Method used to sort nodes in the priority queue.
   */
  @Override
    public int compare(TreeNode o1, TreeNode o2) {
    return Double.compare(o1.nodeScore(), o2.nodeScore());
  }
	
  /**
   * Method used to get the BEPP scores based on the given arguments.
   */
  public static double getBepp(List<Instance> instances, HashMap<Instance, Bag> instanceBags, boolean unbiasedEstimate, int kBEPPConstant, boolean bagCount, double multiplier)
  {
    SufficientStatistics ss;
    if (!bagCount) {
      ss = new SufficientInstanceStatistics(instances, instanceBags);
    } else {
      ss = new SufficientBagStatistics(instances, instanceBags, multiplier);
    }
    return BEPP.GetBEPP(ss.totalCountRight(), ss.positiveCountRight(), kBEPPConstant, unbiasedEstimate);
  }
}
