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
 *    OverlayForecaster.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries.core;

import java.io.PrintStream;
import java.util.List;

import weka.classifiers.evaluation.NumericPrediction;
import weka.core.Instances;

/**
 * Interface to a forecaster that has been trained with data containing 
 * "overlay" attributes. These are attributes whose values will be supplied
 * externally for future time periods to be forecasted. 
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 45163 $
 */
public interface OverlayForecaster {
  
  /**
   * Produce a forecast for the target field(s). Assumes that the model has been built
   * and/or primed so that a forecast can be generated. Also assumes that
   * the forecaster has been told which attributes are to be considered
   * "overlay" attributes in the data. Overlay data is data that the
   * forecaster will be provided with when making a forecast into the future - i.e.
   * it will be given the values of these attributes for future instances. The
   * overlay data provided to this method should have the same structure as
   * the original data used to train the forecaster - i.e. all original fields
   * should be present, including the targets and time stamp field (if supplied).
   * The values of targets will of course be missing ('?') since we want to forecast
   * those. The time stamp values (if a time stamp is in use) may be provided, in which
   * case the forecaster will use the time stamp values in the overlay instances. If
   * the time stamp values are missing, then date arithmetic (for date time stamps) will
   * be used to advance the time value beyond the last seen training value;
   * similarly, for artificial time stamps or non-date time stamps, the computed
   * time delta will be used to increment beyond the last seen training value.
   * 
   * The number of instances in the overlay data should typically match the number
   * of steps that have been requested for forecasting. If these differ, then
   * overlay.numInstances() will be the number of steps forecasted.
   * 
   * @param numSteps number of forecasted values to produce for each target. E.g.
   * a value of 5 would produce a prediction for t+1, t+2, ..., t+5.
   * @param overlay instances in the same format as the training data containing
   * values for overlay attributes for the time steps to be forecasted
   * @param progress an optional varargs parameter supplying progress objects
   * to report/log to
   * @return a List of Lists (one for each step) of forecasted values for each target
   * @throws Exception if the forecast can't be produced for some reason.
   */
  List<List<NumericPrediction>> forecast(int numSteps, Instances overlay,
      PrintStream... progress) throws Exception;
  
  /**
   * Returns true if this forecaster has been trained with data containing
   * overlay fields, and thus will expect to be provided with future values
   * for these fields when making a forecast.
   * 
   * @return true if this forecaster expects to be provided with overlay data
   * when making a forecast.
   */
  boolean isUsingOverlayData();
  
  /**
   * Set the fields to consider as overlay fields
   * 
   * @param overlayFields a comma-separated list of field names
   * @throws Exception if there is a problem setting the overlay fields
   */
  void setOverlayFields(String overlayFields) throws Exception;
  
  /**
   * Get a comma-separated list of fields that considered to be overlay
   * fields
   * 
   * @return a list of field names
   */
  String getOverlayFields();
}
