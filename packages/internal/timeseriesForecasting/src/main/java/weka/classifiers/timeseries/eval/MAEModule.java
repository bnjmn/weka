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
 *    MAEModule.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries.eval;

import java.util.List;

import weka.classifiers.evaluation.NumericPrediction;
import weka.core.Utils;

/**
 * An evaluation module that computes the mean absolute error
 * of forecasted values. 
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 45163 $
 */
public class MAEModule extends ErrorModule {
  
  /**
   * Return the short identifying name of this evaluation module
   * 
   * @return the short identifying name of this evaluation module
   */
  public String getEvalName() {
    return "MAE";
  }
    
  /**
   * Return the longer (single sentence) description
   * of this evaluation module
   * 
   * @return the longer description of this module
   */
  public String getDescription() {
    return "Mean absolute error";
  }
  
  /**
   * Return the mathematical formula that this
   * evaluation module computes.
   * 
   * @return the mathematical formula that this module
   * computes.
   */
  public String getDefinition() {
    return "sum(abs(predicted - actual)) / N";
  }

  /**
   * Calculate the measure that this module represents.
   * 
   * @return the value of the measure for this module for each
   * of the target(s).
   * @throws Exception if the measure can't be computed for some reason.
   */
  public double[] calculateMeasure() throws Exception {
    double[] result = new double[m_targetFieldNames.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = Utils.missingValue();
    }
    
    for (int i = 0; i < m_targetFieldNames.size(); i++) {
      double sumAbs = 0;
      List<NumericPrediction> preds = m_predictions.get(i);
     
      int count = 0;
      for (NumericPrediction p : preds) {
        if (!Utils.isMissingValue(p.error())) {
          sumAbs += Math.abs(p.error());
          count++;
        }
      }
      
      if (m_counts[i] > 0) {
        sumAbs /= m_counts[i];
      }
      
      if (count > 0) {
        result[i] = sumAbs;
      } else {
        result[i] = Utils.missingValue();
      }
    }
    
    return result;
  }
}
