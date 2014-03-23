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
 *    PMMLProducer.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.pmml;

import weka.core.Instances;

/**
 * Interface to something that can produce a PMML representation of itself.
 * 
 * @author David Persons
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface PMMLProducer {

  /**
   * Produce a PMML representation
   * 
   * @param train the training data that might have been used by the
   *          implementer. If it is not needed by the implementer then clients
   *          can safely pass in null
   * 
   * @return a string containing the PMML representation
   */
  String toPMML(Instances train);
}
