/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    IBestSplitMeasure.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi.miti;

/**
 * Interface to be implemented by split selection measures.
 *
 * @author Luke Bjerring
 * @version $Revision$
 */
public interface IBestSplitMeasure {

  /**
   * Returns a purity score of the two groups after the split - larger is better
   */
  public double getScore(SufficientStatistics ss, int kBEPPConstant, boolean unbiasedEstimate);
	
	
  /**
   * Returns a purity score for the N groups after a nominal split - larger is better
   */
  public double getScore(double[] totalCounts, double[] positiveCounts, int kBEPPConstant, boolean unbiasedEstimate);
}
