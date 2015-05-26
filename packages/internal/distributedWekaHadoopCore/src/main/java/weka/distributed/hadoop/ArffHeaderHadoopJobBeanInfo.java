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
 *    ArffHeaderHadoopJobBeanInfo
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;

/**
 * BeanInfo class for the ArffHeaderHadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ArffHeaderHadoopJobBeanInfo extends HadoopJobBeanInfo {

  /**
   * Get an array of PropertyDescriptors for the ArffHeaderHadoopJob's public
   * properties.
   * 
   * @return an array of PropertyDescriptors
   */
  @Override
  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor p1;
      ArrayList<PropertyDescriptor> pds = super.getBaseDescriptors();

      pds.remove(1); // additional weka libraries not needed for this job

      // p1 = new PropertyDescriptor("pathToWekaJar",
      // ArffHeaderHadoopJob.class);
      // p1.setPropertyEditorClass(weka.gui.beans.FileEnvironmentField.class);
      // pds.add(p1);

      p1 =
        new PropertyDescriptor("pathToExistingHeader",
          ArffHeaderHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("outputHeaderFileName",
          ArffHeaderHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("attributeNames", ArffHeaderHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("attributeNamesFile", ArffHeaderHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("generateCharts", ArffHeaderHadoopJob.class);
      pds.add(p1);

      return pds.toArray(new PropertyDescriptor[1]);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
