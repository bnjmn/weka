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
 *    BEPP.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi.miti;

/**
 * Class with static methods for calculating BEPP score. 
 *
 * @author Luke Bjerring
 * @version $Revision$
 */
public class BEPP {	

  /**
   * Calculates score for left subset based on given sufficient statistics and parameters.
   */
  public static double GetLeftBEPP(SufficientStatistics ss, int kBEPPConstant, boolean unbiasedEstimate)
  {
    return GetBEPP(ss.totalCountLeft(), ss.positiveCountLeft(), kBEPPConstant, unbiasedEstimate);
  }

  /**
   * Calculates score for right subset based on given sufficient statistics and parameters.
   */
  public static double GetRightBEPP(SufficientStatistics ss, int kBEPPConstant, boolean unbiasedEstimate)
  {
    return GetBEPP(ss.totalCountRight(), ss.positiveCountRight(), kBEPPConstant, unbiasedEstimate);
  }

  /**
   * Calculates score for entire set based on given sufficient statistics and parameters.
   */
  
  public static double GetBEPP(SufficientStatistics ss, int kBEPPConstant, boolean unbiasedEstimate)
  {
    return GetBEPP(ss.totalCountLeft() + ss.totalCountRight(), ss.positiveCountLeft() + ss.positiveCountRight(), kBEPPConstant, unbiasedEstimate);		
  }
	
  /**
   * Calculates score based on given counts and parameters.
   */
  public static double GetBEPP(double totalCount, double positiveCount, int kBEPPConstant, boolean unbiasedEstimate)
  {
    if (unbiasedEstimate) {
      return (positiveCount + (kBEPPConstant / 2.0)) / (totalCount + kBEPPConstant);
    } else {
      return (positiveCount) / (totalCount + kBEPPConstant);
    }
  }
}
