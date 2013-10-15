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
 *    MapReduceJobConfigBeanInfo.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package distributed.hadoop;

import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.ArrayList;

/**
 * BeanInfo class for MapReduceJobConfig
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class MapReduceJobConfigBeanInfo extends SimpleBeanInfo {

  /**
   * Get an array of PropertyDescriptors for the InputMappedClassifier's public
   * properties.
   * 
   * @return an array of PropertyDescriptors
   */
  @Override
  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor p1;
      ArrayList<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();

      p1 = new PropertyDescriptor("HDFSHost", MapReduceJobConfig.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("HDFSPort", MapReduceJobConfig.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("jobTrackerHost", MapReduceJobConfig.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("jobTrackerPort", MapReduceJobConfig.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("numberOfMaps", MapReduceJobConfig.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("taskTrackerMapTasksMaximum",
        MapReduceJobConfig.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("inputPaths", MapReduceJobConfig.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("outputPath", MapReduceJobConfig.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("mapredMaxSplitSize",
        MapReduceJobConfig.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      return pds.toArray(new PropertyDescriptor[pds.size()]);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return null;
  }

}
