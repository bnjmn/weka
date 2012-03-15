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
 *    SSBEPP.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi.miti;


/**
 * Implements the SSBEPP split selection measure.
 *
 * @author Luke Bjerring
 * @version $Revision$
 */
public class SSBEPP implements IBestSplitMeasure {
	
  /**
   * Returns SSBEPP score of the two groups after the split - the larger the better
   */
  public static double getSSBEPP(SufficientStatistics ss, int kBEPPConstant, boolean unbiasedEstimate)
  {
    double leftBEPP = BEPP.GetLeftBEPP(ss, kBEPPConstant, unbiasedEstimate);
    double rightBEPP = BEPP.GetRightBEPP(ss, kBEPPConstant, unbiasedEstimate);
    return leftBEPP * leftBEPP / 2 + rightBEPP * rightBEPP / 2;
  }
  
  /**
   * Returns SSBEPP score of the two groups after the split - the larger the better
   */
  @Override
    public double getScore(SufficientStatistics ss, int kBEPPConstant, boolean unbiasedEstimate) {
    return getSSBEPP(ss, kBEPPConstant, unbiasedEstimate);
  }
  
  /**
   * Stub: implementation for nominal attributes not complete; will simply exit.
   */
  @Override
    public double getScore(double[] totalCounts, double[] positiveCounts, int kBEPPConstant, boolean unbiasedEstimate) {

    System.out.println("Implementation of SSBEPP not available for nominal attributes");
    System.exit(1);
    return 0;
  }
}
