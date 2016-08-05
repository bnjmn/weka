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
 *    IncrementallyPrimable.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries.core;

import weka.core.Instance;

/**
 * An interface to a forecaster that can be primed incrementally. I.e. 
 * one instance at a time.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 45163 $
 */
public interface IncrementallyPrimeable {
  
  /**
   * Update the priming information incrementally, i.e. one instance at
   * a time. To indicate the start of a new batch of priming data
   * an empty set of instances must be passed to TSForecaster.primeForecaster()
   * before the first call to primeForecasterIncremental()
   * 
   * @param inst the instance to prime with.
   * @throws Exception if something goes wrong.
   */
  void primeForecasterIncremental(Instance inst) throws Exception;    
}
