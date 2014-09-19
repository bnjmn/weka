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
 *    HadoopJobBeanInfo
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.ArrayList;

/**
 * BeanInfo class for the abstract HadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class HadoopJobBeanInfo extends SimpleBeanInfo {

  /**
   * Get an array of PropertyDescriptors for the HadoopJob's public properties.
   * 
   * @return an array of PropertyDescriptors
   */
  public ArrayList<PropertyDescriptor> getBaseDescriptors() {
    try {
      PropertyDescriptor p1;
      ArrayList<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();

      p1 = new PropertyDescriptor("pathToWekaJar", HadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.FileEnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("additionalWekaPackages", HadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("loggingInterval", HadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("debug", HadoopJob.class);
      pds.add(p1);

      return pds;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
