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
 *    WekaClassifierMapTaskBeanInfo.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.ArrayList;

/**
 * BeanInfo class for the WekaClassifierMapTask
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierMapTaskBeanInfo extends SimpleBeanInfo {

  /**
   * Get an array of PropertyDescriptors for the CSVToArffHeaderMapTask's public
   * properties.
   * 
   * @return an array of PropertyDescriptors
   */
  @Override
  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor p1;
      ArrayList<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();

      p1 = new PropertyDescriptor("classifier", WekaClassifierMapTask.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("forceBatchLearningForUpdateableClassifiers",
        WekaClassifierMapTask.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("forceVotedEnsembleCreation",
        WekaClassifierMapTask.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("useReservoirSamplingWhenBatchLearning",
        WekaClassifierMapTask.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("reservoirSampleSize",
        WekaClassifierMapTask.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("filtersToUse", WekaClassifierMapTask.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("foldNumber", WekaClassifierMapTask.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("totalNumFolds", WekaClassifierMapTask.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("seed", WekaClassifierMapTask.class);
      pds.add(p1);

      return pds.toArray(new PropertyDescriptor[pds.size()]);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

}
