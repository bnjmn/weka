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
 *    MaxBEPP.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi.miti;

/**
 * Implements the MaxBEPP split selection measure.
 *
 * @author Luke Bjerring
 * @version $Revision$
 */
public class MaxBEPP implements IBestSplitMeasure {
	
  /**
   * Computes MaxBEPP score for two subsets; the larger the better.
   */
  public static double getMaxBEPP(SufficientStatistics ss, int kBEPPConstant, boolean unbiasedEstimate)
  {
    double leftBEPP = BEPP.GetLeftBEPP(ss, kBEPPConstant, unbiasedEstimate);
    double rightBEPP = BEPP.GetRightBEPP(ss, kBEPPConstant, unbiasedEstimate);
    
    return Math.max(leftBEPP, rightBEPP);
  }
  
  /**
   * Computes MaxBEPP score for nominal case; the larger the better.
   */
  public static double getMaxBEPP(double[] totalCounts, double[] positiveCounts, int kBEPPConstant, boolean unbiasedEstimate)
  {
    double max = 0;
    for (int i = 0; i < totalCounts.length; i++)
      {
        double bepp = BEPP.GetBEPP(totalCounts[i], positiveCounts[i], kBEPPConstant, unbiasedEstimate);
        if (bepp > max)
          max = bepp;
      }
    return max;
  }

  /**
   * Computes MaxBEPP score for two subsets; the larger the better.
   */
  @Override
    public double getScore(SufficientStatistics ss, int kBEPPConstant, boolean unbiasedEstimate) {
    return getMaxBEPP(ss, kBEPPConstant, unbiasedEstimate);
  }
  
  /**
   * Computes MaxBEPP score for nominal case; the larger the better.
   */
  @Override
    public double getScore(double[] totalCounts, double[] positiveCounts, int kBEPPConstant, boolean unbiasedEstimate) {
    return getMaxBEPP(totalCounts, positiveCounts, kBEPPConstant, unbiasedEstimate);
  }
}
