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
 *    CSVToARFFHadoopJobBeanInfo
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.beans.BeanDescriptor;
import java.beans.EventSetDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 * BeanInfo class for the CSVToARFFHadoopJob KF step
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CSVToARFFHadoopJobBeanInfo extends AbstractHadoopJobBeanInfo {

  /**
   * Get the event set descriptors pertinent to data sources
   * 
   * @return an <code>EventSetDescriptor[]</code> value
   */
  @Override
  public EventSetDescriptor[] getEventSetDescriptors() {
    try {
      List<EventSetDescriptor> descriptors =
        new ArrayList<EventSetDescriptor>();
      for (EventSetDescriptor es : super.getEventSetDescriptors()) {
        descriptors.add(es);
      }
      descriptors.add(new EventSetDescriptor(CSVToARFFHadoopJob.class,
        "dataSet", DataSourceListener.class, "acceptDataSet"));

      descriptors.add(new EventSetDescriptor(CSVToARFFHadoopJob.class, "image",
        ImageListener.class, "acceptImage"));

      descriptors.add(new EventSetDescriptor(CSVToARFFHadoopJob.class, "text",
        TextListener.class, "acceptText"));

      return descriptors.toArray(new EventSetDescriptor[descriptors.size()]);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return null;
  }

  /**
   * Get the bean descriptor for this bean
   * 
   * @return a <code>BeanDescriptor</code> value
   */
  @Override
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(weka.gui.beans.CSVToARFFHadoopJob.class,
      HadoopJobCustomizer.class);
  }
}
