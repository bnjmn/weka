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
 *    PrimingDataLearner.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries;

/**
 * Interface to a forecaster that learns from the priming data
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface PrimingDataLearner {

  /**
   * Reset this forecaster ready to learn from a new set of priming data
   */
  void reset();

  /**
   * Return the minimum number of training/priming data points required before a
   * forecast can be made
   * 
   * @return the minimum number of training/priming data points required
   */
  int getMinRequiredTrainingPoints();

  /**
   * Update the forecaster on a priming instance or predicted value (for
   * closed-loop projection)
   * 
   * @param primingOrPredictedTargetValue the instance to update from
   */
  void updateForecaster(double primingOrPredictedTargetValue);
}
