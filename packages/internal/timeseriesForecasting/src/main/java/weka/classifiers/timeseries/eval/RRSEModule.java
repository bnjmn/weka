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
 *    RRSEModule.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries.eval;

import java.util.List;

import weka.classifiers.evaluation.NumericPrediction;
import weka.core.Instance;
import weka.core.Utils;

/**
 * An evaluation module that computes the root relative squared error
 * of forecasted values. I.e. the root mean squared error of forecasted values
 * is computed by this module and these are divided by the root mean squared
 * error obtained by using a target value from a previous time step
 * as the predicted value.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 45163 $
 */
public class RRSEModule extends ErrorModule {
  
  protected double[] m_previousActual;
  protected double[] m_sumOfSE;
  
  /**
   * Holds the RRSE module that this one is relative to - i.e.
   * computations of the predictions provided to this module
   * will be relative to the actual target values obtained from
   * m_relativeRRSE.getPreviousActual(). If no previous RRSEModule
   * is set, then this module will use immediately previous actual
   * values accumulated as evaluateForInstance() is called (i.e. 
   * evaluation is relative to using the immediately preceding
   * actual value as the forecast. Setting a previous RRSEModule
   * allows evaluation relative to actual values further back in
   * the past
   */
  protected RRSEModule m_relativeRRSE;
  
  protected static final double SMALL = 0.000001;
  
  /**
   * Reset this module
   */
  public void reset() {
    super.reset();
    m_previousActual = new double[m_targetFieldNames.size()];
    m_sumOfSE = new double[m_targetFieldNames.size()];
    for (int i = 0; i < m_targetFieldNames.size(); i++) {
      m_previousActual[i] = Utils.missingValue();
      m_sumOfSE[i] = 0;
    }
  }
  
  /**
   * Set a RRSEModule to use for the relative calculations - i.e.
   * actual target values from this module will be used.
   * 
   * @param relative the RRSE module to use for relative computations.
   */
  public void setRelativeRRSEModule(RRSEModule relative) {
    m_relativeRRSE = relative;
  }
  
  /**
   * Get the actual target values from the immediately preceding 
   * time step.
   * 
   * @return the actual target values from the immediately preceding
   * time step.
   */
  public double[] getPreviousActual() {
    return m_previousActual;
  }
  
  /**
   * Return the short identifying name of this evaluation module
   * 
   * @return the short identifying name of this evaluation module
   */
  public String getEvalName() {
    return "RRSE";
  }
  
  /**
   * Return the longer (single sentence) description
   * of this evaluation module
   * 
   * @return the longer description of this module
   */
  public String getDescription() {
    return "Root relative squared error";
  }
  
  /**
   * Return the mathematical formula that this
   * evaluation module computes.
   * 
   * @return the mathematical formula that this module
   * computes.
   */
  public String getDefinition() {
    return "sqrt(sum((predicted - actual)^2) / N) / " +
                "sqrt(sum(previous_target - actual)^2) / N)";
  }
  
  protected void evaluatePredictionForTargetForInstance(int targetIndex, 
      NumericPrediction forecast, double actualValue) {
    
    double predictedValue = forecast.predicted();
    double[][] intervals = forecast.predictionIntervals();
    
    NumericPrediction pred = new NumericPrediction(actualValue, predictedValue, 1,
        intervals);
    m_predictions.get(targetIndex).add(pred);
    m_counts[targetIndex]++;
  }
  
  /**
   * Evaluate the given forecast(s) with respect to the given
   * test instance. Targets with missing values are ignored.
   * 
   * @param forecasts a List of forecasted values. Each element 
   * corresponds to one of the targets and is assumed to be in the same
   * order as the list of targets supplied to the setTargetFields() method.
   * @throws Exception if the evaluation can't be completed for some
   * reason. 
   */
  public void evaluateForInstance(List<NumericPrediction> forecasts, 
      Instance inst) throws Exception {
    
    // here just compute the running sum of abs errors for each target
    // with respect to using the previous value of the target as a prediction
    for (int i = 0; i < m_targetFieldNames.size(); i++) {
      double actualValue = getTargetValue(m_targetFieldNames.get(i), inst);
      
      if (m_relativeRRSE != null) {
        m_previousActual = m_relativeRRSE.getPreviousActual();
      }
      
      
      if (m_relativeRRSE == null && 
          Utils.isMissingValue(m_previousActual[i])) {
        m_previousActual[i] = actualValue;
      } else {        
        // only compute for non-missing previous actual values and non-missing
        // current actual values
        if (!Utils.isMissingValue(actualValue) && 
            !Utils.isMissingValue(m_previousActual[i])) {
          evaluatePredictionForTargetForInstance(i, forecasts.get(i), actualValue);
          m_sumOfSE[i] += ((m_previousActual[i] - actualValue) * 
              (m_previousActual[i] - actualValue));
  //        m_newCounts[i]++;
        }
        if (m_relativeRRSE == null) {
          m_previousActual[i] = actualValue;
        }
      }
    }    
  }
  
  public double[] calculateMeasure() throws Exception {
    double[] result = new double[m_targetFieldNames.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = Utils.missingValue();
    }
    
    for (int i = 0; i < m_targetFieldNames.size(); i++) {
      double sumSE = 0;
      double count = 0;
      List<NumericPrediction> preds = m_predictions.get(i);
      
      for (NumericPrediction p : preds) {
        if (!Utils.isMissingValue(p.error())) {
          sumSE += (p.error() * p.error());
          count++;
        }
      }
      
      if (m_sumOfSE[i] == 0) {
        m_sumOfSE[i] = SMALL;
      }
      /*System.err.println("--- pred " + sumAbs + " prev " + m_sumOfAbsE[i]);
      System.err.println(sumAbs / m_sumOfAbsE[i]); */
      if (count == 0) {
        result[i] = Utils.missingValue();
      } else {
        double rootMSEPrev = Math.sqrt(sumSE / count);
        double rootMSE = Math.sqrt(m_sumOfSE[i] / count);
        result[i] = (rootMSEPrev / rootMSE) * 100.0;
      }
    }
    
    return result;
  }
}
