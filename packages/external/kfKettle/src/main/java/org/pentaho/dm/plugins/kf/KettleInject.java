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
 *    KettleInject.java
 *    Copyright (C) 2008 Pentaho Corporation
 *
 */


package org.pentaho.dm.plugins.kf;

import java.beans.EventSetDescriptor;
import java.util.Vector;

import weka.core.Instance;
import weka.core.Instances;
import weka.gui.Logger;
import weka.gui.beans.AbstractDataSource;
import weka.gui.beans.BeanCommon;
import weka.gui.beans.BeanInstance;
import weka.gui.beans.BeanVisual;
import weka.gui.beans.DataSetEvent;
import weka.gui.beans.DataSourceListener;
import weka.gui.beans.EventConstraints;
import weka.gui.beans.InstanceEvent;
import weka.gui.beans.InstanceListener;
import weka.gui.beans.KFStep;
import weka.gui.beans.TestSetEvent;
import weka.gui.beans.TestSetListener;
import weka.gui.beans.TestSetProducer;
import weka.gui.beans.TrainingSetEvent;
import weka.gui.beans.TrainingSetListener;
import weka.gui.beans.TrainingSetProducer;

/**
 * Entry point into a KnowledgeFlow for data from a Kettle transform
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @see AbstractDataSource
 * @version $Revision$
 */
@KFStep(category = "DataSources", toolTipText = "Accept input from Kettle")
public class KettleInject 
  extends AbstractDataSource
  implements BeanCommon, 
             TrainingSetListener, 
             TrainingSetProducer, 
             TestSetListener,
             TestSetProducer,
             DataSourceListener,
             InstanceListener,
             EventConstraints {

  /**
   * Keep track of how many listeners for different types of events there are.
   */
  private int m_instanceEventTargets = 0;
  private int m_dataSetEventTargets = 0;
  private int m_trainingSetEventTargets = 0;
  private int m_testSetEventTargets = 0;

  protected transient Logger m_log = null;
  private static String ICON_PATH = "org/pentaho/dm/plugins/kf/icons/";

  public KettleInject() {
    /*    m_visual.loadIcons(ICON_PATH
		       +"KettleInject.gif",
		       ICON_PATH
		       +"KettleInject_animated.gif"); */
    m_visual.setText("KettleInject");
  }

  public void setLog(Logger l) {
    m_log = l;
  }

  /**
   * Set a custom (descriptive) name for this bean
   * 
   * @param name the name to use
   */
  public void setCustomName(String name) {
    m_visual.setText(name);
  }
  
  /**
   * Get the custom (descriptive) name for this bean (if one has been set)
   * 
   * @return the custom name (or the default name)
   */
  public String getCustomName() {
    return m_visual.getText();
  }

  /**
   * Stop any processing that the bean might be doing.
   */
  public void stop() {
    // there is probably not much we can do about telling upstream
    // Kettle stuff to stop :-)
  }
  
  /**
   * Returns true if. at this time, the bean is busy with some
   * (i.e. perhaps a worker thread is performing some calculation).
   * 
   * @return true if the bean is busy.
   */
  public boolean isBusy() {
    return false;
  }
  
  public void waitWhileFlowBusy(Vector flow) {
    // poll the beans periodically to see if there are any
    // that are still busy
    while(true) {
      boolean busy = false;
      for (int i = 0; i < flow.size(); i++) {
        BeanInstance bi = (BeanInstance)flow.elementAt(i);
        if (bi.getBean() instanceof BeanCommon) {
          if (((BeanCommon)bi.getBean()).isBusy()) {
            busy = true;
            break; // for
          }
        }        
      }
      if (busy) {
        // wait a bit...
        try {
        Thread.sleep(3000);
        } catch (InterruptedException ex) {
          // ignore.
        }
      } else {
        break; // while
      }
    }
  }

  /**
   * Returns true if, at this time, 
   * the object will accept a connection with respect to the named event
   *
   * @param eventName the event
   * @return true if the object will accept a connection
   */
  public boolean connectionAllowed(String eventName) {
    if (eventName.equals("dataSet")) {
      return (m_dataSetEventTargets > 0);
    }

    if (eventName.equals("instance")) {
      return (m_instanceEventTargets > 0);
    }

    if (eventName.equals("trainingSet")) {
      return (m_trainingSetEventTargets > 0);
    }

    if (eventName.equals("testSet")) {
      return (m_testSetEventTargets > 0);
    }
    return false;
  }

  /**
   * Returns true if, at this time, 
   * the object will accept a connection according to the supplied
   * EventSetDescriptor
   *
   * @param esd the EventSetDescriptor
   * @return true if the object will accept a connection
   */
  public boolean connectionAllowed(EventSetDescriptor esd) {
    return connectionAllowed(esd.getName());
  }


  /**
   * Notify this object that it has been registered as a listener with
   * a source with respect to the named event
   *
   * @param eventName the event
   * @param source the source with which this object has been registered as
   * a listener
   */
  public synchronized void connectionNotification(String eventName,
						  Object source) {
    // Don't have to do anything special
  }

  /**
   * Notify this object that it has been deregistered as a listener with
   * a source with respect to the supplied event name
   *
   * @param eventName the event
   * @param source the source with which this object has been registered as
   * a listener
   */
  public synchronized void disconnectionNotification(String eventName,
						     Object source) {
    // Don't have to do anything special
  }

  /**
   * Returns how many downstream listeners are InstanceListeners.
   * NOTE: there will be either InstanceListeners OR
   * DataSetListeners or TrainingSetListeners or TestSetListeners 
   * connected to this source - but never more than
   * one type simultaneously.
   *
   * @return the number of InstanceListeners connected to this
   * source
   */
  public int getNumberOfInstanceTargets() {
    return m_instanceEventTargets;
  }

  /**
   * Returns how many downstream listeners are DataSetListenersListeners.
   * NOTE: there will be either InstanceListeners OR
   * DataSetListeners connected to this source - never both
   * types simultaneously.
   *
   * @return the number of DataSetListeners connected to this
   * source
   */
  public int getNumberOfDataSetTargets() {
    return m_dataSetEventTargets;
  }

  /**
   * Notify all instance listeners that a new instance is available
   *
   * @param e an <code>InstanceEvent</code> value
   */
  protected void passOnInstance(InstanceEvent e) {
    if (m_listeners.size() > 0) {
      for(int i = 0; i < m_listeners.size(); i++) {
	((InstanceListener)m_listeners.elementAt(i)).acceptInstance(e);
      }
    }
  }
  
  public void acceptInstance(InstanceEvent e) {
    passOnInstance(e);
  }

  public void acceptDataSet(DataSetEvent e) {
    passOnDataset(e);
  }

  /**
   * Notify all Data source listeners that a data set is available
   *
   * @param e a <code>DataSetEvent</code> value
   */
  protected void passOnDataset(DataSetEvent e) {
    if (m_listeners.size() > 0) {
      for(int i = 0; i < m_listeners.size(); i++) {
	((DataSourceListener)m_listeners.elementAt(i)).acceptDataSet(e);
      }
    }
  }

  public void acceptTrainingSet(TrainingSetEvent e) {
    passOnTrainingSet(e);
  }

  /**
   * Notify all training set listeners that a data set is available
   *
   * @param e a <code>TrainingSetEvent</code> value
   */
  protected void passOnTrainingSet(TrainingSetEvent e) {
    if (m_listeners.size() > 0) {
      for(int i = 0; i < m_listeners.size(); i++) {
	((TrainingSetListener)m_listeners.elementAt(i)).acceptTrainingSet(e);
      }
    }
  }

  public void acceptTestSet(TestSetEvent e) {
    passOnTestSet(e);
  }

  /**
   * Notify all test set listeners that a data set is available
   *
   * @param e a <code>TrainingSetEvent</code> value
   */
  protected void passOnTestSet(TestSetEvent e) {
    if (m_listeners.size() > 0) {
      for(int i = 0; i < m_listeners.size(); i++) {
	((TestSetListener)m_listeners.elementAt(i)).acceptTestSet(e);
      }
    }
  }
  
  /**
   * Returns true if the named event can be generated at this time
   *
   * @param eventName the event
   * @return a <code>boolean</code> value
   */
  public boolean eventGeneratable(String eventName) {
    if (eventName.compareTo("instance") == 0) {
      if (m_dataSetEventTargets > 0 || 
          m_trainingSetEventTargets > 0  ||
          m_testSetEventTargets > 0) {
	return false;
      }
    }

    if (eventName.compareTo("dataSet") == 0) {
      if (m_instanceEventTargets > 0 ||
          m_trainingSetEventTargets > 0 ||
          m_testSetEventTargets > 0) {
	return false;
      }
    }

    if (eventName.compareTo("trainingSet") == 0) {
      if (m_dataSetEventTargets > 0 ||
          m_testSetEventTargets > 0 ||
          m_instanceEventTargets > 0) {
        return false;
      }
    }

    if (eventName.compareTo("testSet") == 0) {
      if (m_dataSetEventTargets > 0 ||
          m_trainingSetEventTargets > 0 ||
          m_instanceEventTargets > 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Add a listener
   *
   * @param dsl a <code>DataSourceListener</code> value
   */
  public synchronized void addDataSourceListener(DataSourceListener dsl) {
    super.addDataSourceListener(dsl);
    m_dataSetEventTargets++;
    // pass on any current instance format
    //    notifyStructureAvailable(m_dataFormat);
  }
  
  /**
   * Remove a listener
   *
   * @param dsl a <code>DataSourceListener</code> value
   */
  public synchronized void removeDataSourceListener(DataSourceListener dsl) {
    super.removeDataSourceListener(dsl);
    m_dataSetEventTargets--;
  }

  /**
   * Add an instance listener
   *
   * @param dsl a <code>InstanceListener</code> value
   */
  public synchronized void addInstanceListener(InstanceListener dsl) {
    super.addInstanceListener(dsl);
    m_instanceEventTargets++;

    // pass on any current instance format      
    //    notifyStructureAvailable(m_dataFormat);
  }


  /**
   * Use the default visual appearance for this bean
   */
  public void useDefaultVisual() {
    m_visual.loadIcons(ICON_PATH + "KettleInject.gif",
        ICON_PATH+ "KettleInject.gif");
  }
  
  /**
   * Remove an instance listener
   *
   * @param dsl a <code>InstanceListener</code> value
   */
  public synchronized void removeInstanceListener(InstanceListener dsl) {
    super.removeInstanceListener(dsl);
    m_instanceEventTargets--;
  }

  public synchronized void addTrainingSetListener(TrainingSetListener tsl) {
    m_listeners.add(tsl);
    m_trainingSetEventTargets++;
  }

  public synchronized void removeTrainingSetListener(TrainingSetListener tsl) {
    m_listeners.remove(tsl);
    m_trainingSetEventTargets--;
  }

 public synchronized void addTestSetListener(TestSetListener tsl) {
    m_listeners.add(tsl);
    m_testSetEventTargets++;
  }

  public synchronized void removeTestSetListener(TestSetListener tsl) {
    m_listeners.remove(tsl);
    m_testSetEventTargets--;
  }
}
