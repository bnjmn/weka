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
 *    Aggregateable.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

/**
 * Interface to something that can aggregate an object of the same type with
 * itself.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface Aggregateable<E> {

  /**
   * Aggregate an object with this one
   * 
   * @param toAggregate the object to aggregate
   * @return the result of aggregation
   * @throws Exception if the supplied object can't be aggregated for some
   *           reason
   */
  E aggregate(E toAggregate) throws Exception;

  /**
   * Call to complete the aggregation process. Allows implementers to do any
   * final processing based on how many objects were aggregated.
   * 
   * @throws Exception if the aggregation can't be finalized for some reason
   */
  void finalizeAggregation() throws Exception;
}
