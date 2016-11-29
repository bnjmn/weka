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
 *    RMSEModule.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries.eval;

import weka.core.Utils;

/**
 * An evaluation module that computes the root mean squared error
 * of forecasted values.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 45163 $
 */
public class RMSEModule extends MSEModule {
  
  /**
   * Return the short identifying name of this evaluation module
   * 
   * @return the short identifying name of this evaluation module
   */
  public String getEvalName() {
    return "RMSE";
  }

  /**
   * Return the longer (single sentence) description
   * of this evaluation module
   * 
   * @return the longer description of this module
   */
  public String getDescription() {
    return "Root mean squared error";
  }
  
  /**
   * Return the mathematical formula that this
   * evaluation module computes.
   * 
   * @return the mathematical formula that this module
   * computes.
   */
  public String getDefinition() {
    return "sqrt(sum((predicted - actual)^2) / N)";
  }
  
  /**
   * Calculate the measure that this module represents.
   * 
   * @return the value of the measure for this module for each
   * of the target(s).
   * @throws Exception if the measure can't be computed for some reason.
   */
  public double[] calculateMeasure() throws Exception {
    double[] result = super.calculateMeasure();
    
    for (int i = 0; i < result.length; i++) {
      if (!Utils.isMissingValue(result[i])) {
        result[i] = Math.sqrt(result[i]);
      }
    }
    
    return result;
  }
}
