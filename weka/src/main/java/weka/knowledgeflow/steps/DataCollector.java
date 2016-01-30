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
 *    DataCollector.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.WekaException;

/**
 * Auxiliary interface for steps that collect data results of some type - e.g.
 * visualization steps that collect results.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface DataCollector {

  /**
   * Get the data that this collector has collected
   *
   * @return the data collected by this collector
   */
  public Object retrieveData();

  /**
   * Set the data for this collector
   *
   * @param data the data to set
   * @throws WekaException if there is a problem restoring data
   */
  public void restoreData(Object data) throws WekaException;
}
