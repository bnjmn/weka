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
 *    MAPEModule.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries.eval;

import java.util.List;

import weka.classifiers.evaluation.NumericPrediction;
import weka.core.Utils;

/**
 * Computes the mean absolute percentage error
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 46478 $
 */
public class MAPEModule extends ErrorModule {

  public String getEvalName() {
    return "MAPE";
  }
  
  public String getDescription() {
    return "Mean absolute percentage error";
  }
  
  public String getDefinition() {
    return "sum(abs((predicted - actual) / actual)) / N";
  }
  
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
        if (!Utils.isMissingValue(p.error()) && Math.abs(p.actual()) > 0) {
          sumAbs += Math.abs(p.error() / p.actual());
          count++;
        }
      }
      
      /*if (m_counts[i] > 0) {
        sumAbs /= m_counts[i];
      }*/
      
      if (count > 0) {
        sumAbs /= count;
        result[i] = sumAbs * 100.0;
      } else {
        result[i] = Utils.missingValue();
      }
    }
    
    return result;
  }  
}
