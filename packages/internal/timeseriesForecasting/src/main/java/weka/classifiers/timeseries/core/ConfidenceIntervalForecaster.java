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
 *    ConfidenceIntervalForecaster.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries.core;

/**
 * Interface to a forecaster that can compute confidence intervals
 * for its forecasts
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 45163 $
 */
public interface ConfidenceIntervalForecaster {

  /**
   * Set the confidence level for confidence intervals.
   * 
   * @param confLevel the confidence level to use.
   */
  void setConfidenceLevel(double confLevel);
  
  /**
   * Get the confidence level in use for computing confidence intervals.
   * 
   * @return the confidence level.
   */
  double getConfidenceLevel();
  
  /**
   * Returns true if this forecaster is computing confidence
   * limits for some or all of its future forecasts (i.e. 
   * getCalculateConfIntervalsForForecasts() > 0).
   * 
   * @return true if confidence limits will be produced for some
   * or all of its future forecasts.
   */
  boolean isProducingConfidenceIntervals();
  
  /**
   * Set the number of steps for which to compute confidence intervals for.
   * E.g. a value of 5 means that confidence bounds will be computed for
   * 1-step-ahead predictions, 2-step-ahead predictions, ..., 5-step-ahead
   * predictions. Setting a value of 0 indicates that no confidence intervals
   * will be computed/produced.
   * 
   * @param steps the number of steps for which to compute confidence intervals
   * for.
   */
  void setCalculateConfIntervalsForForecasts(int steps);
  
  /**
   * Return the number of steps for which confidence intervals will be computed. A 
   * value of 0 indicates that no confidence intervals will be computed/produced.
   * 
   * @return the number of steps for which confidence intervals will be computed.
   */
  int getCalculateConfIntervalsForForecasts();    
}
