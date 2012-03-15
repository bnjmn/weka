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
 *    GroovyComponentBeanInfo.java
 *    Copyright (C) 2009 Pentaho Corporation
 *
 */

package org.pentaho.dm.kf;

import java.beans.BeanDescriptor;
import java.beans.EventSetDescriptor;
import java.beans.SimpleBeanInfo;

import weka.gui.beans.BatchClassifierListener;
import weka.gui.beans.BatchClustererListener;
import weka.gui.beans.ChartListener;
import weka.gui.beans.DataSourceListener;
import weka.gui.beans.GraphListener;
import weka.gui.beans.IncrementalClassifierListener;
import weka.gui.beans.InstanceListener;
import weka.gui.beans.ThresholdDataListener;
import weka.gui.beans.TrainingSetListener;
import weka.gui.beans.TestSetListener;
import weka.gui.beans.TextListener;
import weka.gui.beans.VisualizableErrorListener;

/**
 * BeanInfo class for the GroovyComponent.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision $
 */
public class GroovyComponentBeanInfo extends SimpleBeanInfo {
  public EventSetDescriptor [] getEventSetDescriptors() {
    try {
      EventSetDescriptor [] esds = { 
        new EventSetDescriptor(GroovyComponent.class, 
                               "batchClassifier", 
                               BatchClassifierListener.class, 
                               "acceptClassifier"),
        new EventSetDescriptor(GroovyComponent.class, 
                               "batchClusterer", 
                               BatchClustererListener.class, 
                               "acceptClusterer"),
        new EventSetDescriptor(GroovyComponent.class,
                               "graph",
                               GraphListener.class,
                               "acceptGraph"),
        new EventSetDescriptor(GroovyComponent.class,
                               "chart",
                               ChartListener.class,
                               "acceptDataPoint"),
        new EventSetDescriptor(GroovyComponent.class,
                               "thresholdData",
                               ThresholdDataListener.class,
                               "acceptDataSet"),
        new EventSetDescriptor(GroovyComponent.class,
                               "visualizableError",
                               VisualizableErrorListener.class,
                               "acceptDataSet"),
        new EventSetDescriptor(GroovyComponent.class,
                               "text",
                               TextListener.class,
                               "acceptText"),
        new EventSetDescriptor(GroovyComponent.class,
                               "incrementalClassifier",
                               IncrementalClassifierListener.class,
                               "acceptClassifier"),
        new EventSetDescriptor(GroovyComponent.class,
            "trainingSet",
            TrainingSetListener.class,
            "acceptTrainingSet"),
        new EventSetDescriptor(GroovyComponent.class,
            "testSet",
            TestSetListener.class,
            "acceptTestSet"),
        new EventSetDescriptor(GroovyComponent.class,
            "dataSet",
             DataSourceListener.class,
             "acceptDataSet"),
        new EventSetDescriptor(GroovyComponent.class,
            "instance",
            InstanceListener.class,
            "acceptInstance")    
        };
      return esds;
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
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(org.pentaho.dm.kf.GroovyComponent.class, 
                              GroovyComponentCustomizer.class);
  }
}
