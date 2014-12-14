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
 *    PreconstructedFilteredClusterer.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;
import weka.gui.GPCIgnore;
import weka.gui.beans.KFIgnore;

/**
 * A class for using a pre-constructed clusterer (i.e. already trained) along
 * with a filter (also pre-constructed) that was used to transform the data that
 * the clusterer was trained with. This class is purely for prediction and
 * calling buildClusterer() will raise an exception.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFIgnore
@GPCIgnore
public class PreconstructedFilteredClusterer extends FilteredClusterer {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 1874098806907765185L;

  @Override
  public void buildClusterer(Instances training) throws Exception {
    throw new Exception("This clusterer is \"pre-constructed\", i.e. the "
      + "base clusterer is expected to already be trained.");
  }

  @Override
  public void setFilter(Filter filter) {
    super.setFilter(filter);

    if (!(filter instanceof PreconstructedFilter)) {
      throw new IllegalArgumentException(
        "The filter must be a Preconstructed one!");
    } else if (!((PreconstructedFilter) filter).isConstructed()) {
      throw new IllegalArgumentException("PreconstructedFilter: "
        + filter.getClass().getName() + " has not been initialized!");
    }

    m_FilteredInstances = filter.getOutputFormat();
  }

  /**
   * Set the header for the filtered data.
   * 
   * @param filteredHeader the header for the filtered data
   */
  public void setFilteredHeader(Instances filteredHeader) {
    m_FilteredInstances = filteredHeader;
  }
}
