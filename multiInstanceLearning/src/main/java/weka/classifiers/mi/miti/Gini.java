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
 *    Gini.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi.miti;

/**
 * Implements the Gini-based split selection measure.
 *
 * @author Luke Bjerring
 * @version $Revision$
 */
public class Gini implements IBestSplitMeasure {

  /**
   * Returns Gini impurity score of the two groups after the split - the closer to zero, the better the split.
   */
  public static double getGiniImpurity(SufficientStatistics ss,
                                       int kBEPPConstant, boolean unbiasedEstimate)
  {
    double leftBEPP = BEPP.GetLeftBEPP(ss, kBEPPConstant, unbiasedEstimate);
    double rightBEPP = BEPP.GetRightBEPP(ss, kBEPPConstant,
                                         unbiasedEstimate);
    
    // Unweighted
    // return leftBEPP * (1 - leftBEPP) + rightBEPP * (1 - rightBEPP);
    
    // Weighted for size of each subset
    return (leftBEPP * (1 - leftBEPP) * ss.totalCountLeft() + rightBEPP
            * (1 - rightBEPP) * ss.totalCountRight())
      / (ss.totalCountLeft() + ss.totalCountRight());
  }
  
  /**
   * Returns Gini impurity score for the N groups after a nominal split (the closer to zero, the better the split).
   */
  public static double getGiniImpurity(double[] totalCounts,
                                       double[] positiveCounts, int kBEPPConstant, boolean unbiasedEstimate)
  {
    double score = 0;
    double fullTotal = 0;
    for (int i = 0; i < totalCounts.length; i++)
      fullTotal += totalCounts[i];
    
    for (int i = 0; i < totalCounts.length; i++) {
      double beppScore = BEPP.GetBEPP(totalCounts[i], positiveCounts[i],
                                      kBEPPConstant, unbiasedEstimate);
      double gini = beppScore * (1 - beppScore);
      //      double proportionalGini = gini * 2 / totalCounts.length; Commented out by Eibe
      
      // Weighted
      score += gini * totalCounts[i] / fullTotal;
    }
    
    return score;
  }
  
  /**
   * Returns purity score of the two groups after the split - larger is better
   */
  @Override
    public double getScore(SufficientStatistics ss, int kBEPPConstant,
                           boolean unbiasedEstimate) {

    // Has desired effect even if we divide by zero
    return 1 / getGiniImpurity(ss, kBEPPConstant, unbiasedEstimate);
  }
  
  /**
   * Returns purity score for the N groups after a nominal split - larger is better
   */
  @Override
    public double getScore(double[] totalCounts, double[] positiveCounts,
                           int kBEPPConstant, boolean unbiasedEstimate) {

    // Has desired effect even if we divide by zero
    return 1 / getGiniImpurity(totalCounts, positiveCounts, kBEPPConstant,
                               unbiasedEstimate);
  }
}
