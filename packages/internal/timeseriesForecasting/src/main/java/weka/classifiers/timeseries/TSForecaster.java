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
 *    TSForecaster.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries;

import java.io.PrintStream;
import java.util.List;

import weka.classifiers.evaluation.NumericPrediction;
import weka.core.Instances;

/**
 * Interface for something that can produce time series predictions.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 45163 $
 */
public interface TSForecaster {
  
  
  /**
   * Provides a short name that describes the underlying algorithm
   * in some way.
   * 
   * @return a short description of this forecaster.
   */
  public String getAlgorithmName();
  
  /**
   * Reset this forecaster so that it is ready to construct a
   * new model.
   */
  public void reset();
  
  /**
   * Set the names of the fields/attributes in the data to forecast.
   * 
   * @param targets a list of names of fields to forecast
   * @throws Exception if a field(s) can't be found, or if multiple
   * fields are specified and this forecaster can't predict multiple
   * fields.
   */
  public void setFieldsToForecast(String targets) throws Exception;
  
  /**
   * Get the fields to forecast.
   * 
   * @return the fields to forecast
   */
  public String getFieldsToForecast();
  
  /**
   * Builds a new forecasting model using the supplied training
   * data. The instances in the data are assumed to be sorted in
   * ascending order of time and equally spaced in time. Some
   * methods may not need to implement this method and may
   * instead do their work in the primeForecaster method.
   * 
   * @param insts the training instances.
   * @param progress an optional varargs parameter supplying progress objects
   * to report/log to
   * @throws Exception if the model can't be constructed for some
   * reason.
   */
  public void buildForecaster(Instances insts, 
      PrintStream... progress) throws Exception;
  
  /**
   * Supply the (potentially) trained model with enough historical
   * data, up to and including the current time point, in order
   * to produce a forecast. Instances are assumed to be sorted in
   * ascending order of time and equally spaced in time.
   * 
   * @param insts the instances to prime the model with
   * @throws Exception if the model can't be primed for some
   * reason.
   */
  public void primeForecaster(Instances insts) throws Exception;  
  
  /**
   * Produce a forecast for the target field(s). 
   * Assumes that the model has been built
   * and/or primed so that a forecast can be generated.
   * 
   * @param numSteps number of forecasted values to produce for each target. E.g.
   * a value of 5 would produce a prediction for t+1, t+2, ..., t+5.
   * @param progress an optional varargs parameter supplying progress objects
   * to report/log to
   * @return a List of Lists (one for each step) of forecasted values for each target
   * @throws Exception if the forecast can't be produced for some reason.
   */
  public List<List<NumericPrediction>> forecast(int numSteps, 
      PrintStream... progress) throws Exception;
  
  /**
   * Run the supplied forecaster with the supplied options on the command line.
   * 
   * @param forecaster the forecaster to run
   * @param options the options to pass to the forecaster
   */
  public abstract void runForecaster(TSForecaster forecaster, String[] options);
}
