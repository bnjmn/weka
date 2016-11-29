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
 *    AbstractForecaster.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries;

import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.timeseries.eval.TSEvaluation;
import weka.core.CommandlineRunnable;
import weka.core.Instances;
import weka.core.SerializedObject;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class implementing TSForecaster that concrete subclasses
 * can extend.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 52585 $
 */
public abstract class AbstractForecaster implements TSForecaster, 
  CommandlineRunnable, Serializable {

  /**
   * For serialization 
   */
  private static final long serialVersionUID = 5179667114364013750L;
  
  /** The name of the attribute (class) to forecast */
  protected List<String> m_fieldsToForecast = null;
  
  /**
   * A utility method for converting a List of Strings to a single
   * comma separated String.
   * 
   * @param list the List<String> object to process
   * @return a single String containing a comma separated list of
   * elements from the original list.
   */
  public static List<String> stringToList(String list) {
    String[] fieldNames = list.split(",");
    List<String> thelist = new ArrayList<String>();
    for (String f : fieldNames) {
      thelist.add(f);
    }
    
    return thelist;
  }
  
  /**
   * Set the names of the fields/attributes in the data to forecast.
   * 
   * @param targets a list of names of fields to forecast
   * @throws Exception if a field(s) can't be found, or if multiple
   * fields are specified and this forecaster can't predict multiple
   * fields.
   */
  public void setFieldsToForecast(String targets) throws Exception {
        
    m_fieldsToForecast = stringToList(targets);
  }
  
  /**
   * Get the fields to forecast.
   * 
   * @return the fields to forecast
   */
  public String getFieldsToForecast() {
    String list = "";
    for (String f : m_fieldsToForecast) {
      list += (f + ",");
    }
    
    list = list.substring(0, list.lastIndexOf(','));
    return list;
  }
  
  /**
   * Builds a new forecasting model using the supplied training
   * data. The instances in the data are assumed to be sorted in
   * ascending order of time and equally spaced in time. Some
   * methods may not need to implement this method and may
   * instead do their work in the primeForecaster method.
   * 
   * @param insts the training instances.
   * @param progress an optional varargs parameter supplying progress objects
   * to report to
   * @throws Exception if the model can't be constructed for some
   * reason.
   */
  public abstract void buildForecaster(Instances insts, 
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
  public abstract void primeForecaster(Instances insts) throws Exception;
  
  /**
   * Produce a forecast for the target field(s). 
   * Assumes that the model has been built
   * and/or primed so that a forecast can be generated.
   * 
   * @param numSteps number of forecasted values to produce for each target. E.g.
   * a value of 5 would produce a prediction for t+1, t+2, ..., t+5.
   * @param progress an optional varargs parameter supplying progress objects
   * to report to
   * @return a List of Lists (one for each step) of forecasted values for each target
   * @throws Exception if the forecast can't be produced for some reason.
   */
  public abstract List<List<NumericPrediction>> forecast(int numSteps, 
      PrintStream... progress) throws Exception;

  @Override
  public void preExecution() throws Exception {
    // subclasses to override
  }

  @Override
  public void postExecution() throws Exception {
    // subclasses to override
  }


  /**
   * Run the supplied object using the supplied options on the command line.
   * 
   * @param toRun the object to run.
   * @param options the command line options to pass to the object.
   * @throws Exception if the supplied object is not an instance of TSForecaster.
   */
  public void run(Object toRun, String[] options) throws IllegalArgumentException {
    if (!(toRun instanceof TSForecaster)) {
      throw new IllegalArgumentException("Argument must be an object of type" +
      		" TSForecaster!");
    }
    
    runForecaster((TSForecaster)toRun, options);
  }
  
  /**
   * Creates a deep copy of the given forecaster using serialization.
   *
   * @param model the forecaster to copy
   * @return a deep copy of the forecaster
   * @exception Exception if an error occurs
   */
  public static TSForecaster makeCopy(TSForecaster model) throws Exception {
    return (TSForecaster)new SerializedObject(model).getObject();
  }
  
  /**
   * Run the supplied forecaster with the supplied options on the command line.
   * 
   * @param forecaster the forecaster to run
   * @param options the options to pass to the forecaster
   */
  public void runForecaster(TSForecaster forecaster, String[] options) {
    try {
      TSEvaluation.evaluateForecaster(forecaster, options);
    } catch (Exception e) {
      if (    ((e.getMessage() != null) && 
          (e.getMessage().indexOf("General options") == -1))
          || (e.getMessage() == null) ) {
        e.printStackTrace();
      } else {
        System.err.println(e.getMessage());
      }
    }
  }

}
