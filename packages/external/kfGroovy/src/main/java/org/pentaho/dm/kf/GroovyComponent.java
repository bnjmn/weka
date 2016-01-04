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
 *    GroovyComponent.java
 *    Copyright (C) 2009 Pentaho Corporation
 *
 */

package org.pentaho.dm.kf;

import groovy.lang.GroovyClassLoader;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.gui.Logger;
import weka.gui.beans.BatchClassifierEvent;
import weka.gui.beans.BatchClassifierListener;
import weka.gui.beans.BatchClustererEvent;
import weka.gui.beans.BatchClustererListener;
import weka.gui.beans.BeanCommon;
import weka.gui.beans.BeanVisual;
import weka.gui.beans.ChartEvent;
import weka.gui.beans.ChartListener;
import weka.gui.beans.ConfigurationEvent;
import weka.gui.beans.ConfigurationListener;
import weka.gui.beans.DataSetEvent;
import weka.gui.beans.DataSourceListener;
import weka.gui.beans.EventConstraints;
import weka.gui.beans.GraphEvent;
import weka.gui.beans.GraphListener;
import weka.gui.beans.IncrementalClassifierEvent;
import weka.gui.beans.IncrementalClassifierListener;
import weka.gui.beans.InstanceEvent;
import weka.gui.beans.InstanceListener;
import weka.gui.beans.KFStep;
import weka.gui.beans.Startable;
import weka.gui.beans.TestSetEvent;
import weka.gui.beans.TestSetListener;
import weka.gui.beans.TextEvent;
import weka.gui.beans.TextListener;
import weka.gui.beans.ThresholdDataEvent;
import weka.gui.beans.ThresholdDataListener;
import weka.gui.beans.TrainingSetEvent;
import weka.gui.beans.TrainingSetListener;
import weka.gui.beans.UserRequestAcceptor;
import weka.gui.beans.Visible;
import weka.gui.beans.VisualizableErrorEvent;
import weka.gui.beans.VisualizableErrorListener;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A KnowledgeFlow plugin that allows the user to write a Groovy script
 * to do stuff. The Groovy script gets compiled dynamically at runtime and 
 * can accept and process as many KnowledgeFlow event types as desired.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision $
 */
@KFStep(category = "Scripting", toolTipText = "Implement a knowledge flow component using Groovy")
public class GroovyComponent 
  extends JPanel 
  implements EnvironmentHandler, BeanCommon, Visible, EventConstraints,
             UserRequestAcceptor, Startable, TrainingSetListener,
             TestSetListener, InstanceListener,
             TextListener, DataSourceListener,
             BatchClassifierListener, IncrementalClassifierListener,
             BatchClustererListener, GraphListener,
             ChartListener, ThresholdDataListener,
             VisualizableErrorListener, ConfigurationListener,
             GroovyHelper, Serializable {

  /** The visual appearance of this bean */
  protected BeanVisual m_visual = 
    new BeanVisual("GroovyComponent",
                   "org/pentaho/dm/kf/icons/GroovyComponent.gif",
                   "org/pentaho/dm/kf/icons/GroovyComponent.gif");
  
  /** The logging object to use */
  protected transient Logger m_log = null;
  
  /** The environment variables to use */
  protected transient Environment m_env = Environment.getSystemWide();
  
  /** The script to use */
  protected String m_groovyScript = null;
  
  /** The object constructed from the script */
  protected transient Object m_groovyObject = null;
  
  /** Objects sending events to us */
  protected ArrayList<Object> m_incomingConnections =
    new ArrayList<Object>();
  protected ArrayList<String> m_incomingConnectionNames = 
    new ArrayList<String>();
  
  /** Objects listening for trainingSet events */
  protected ArrayList<TrainingSetListener> m_trainingSetListeners = 
    new ArrayList<TrainingSetListener>();
  
  /** Objects listening for testSet events */
  protected ArrayList<TestSetListener> m_testSetListeners = 
    new ArrayList<TestSetListener>();
  
  /** Objects listening for testSet events */
  protected ArrayList<DataSourceListener> m_dataSourceListeners = 
    new ArrayList<DataSourceListener>();
  
  /** Objects listening for instance events */
  protected ArrayList<InstanceListener> m_instanceListeners = 
    new ArrayList<InstanceListener>();
  
  /** Objects listening for text events */
  protected ArrayList<TextListener> m_textListeners = 
    new ArrayList<TextListener>();
  
  /** Objects listening for batch classifier events */
  protected ArrayList<BatchClassifierListener> m_batchClassifierListeners = 
    new ArrayList<BatchClassifierListener>();
  
  /** Objects listening for incremental classifier events */
  protected ArrayList<IncrementalClassifierListener> m_incrementalClassifierListeners = 
    new ArrayList<IncrementalClassifierListener>();
  
  /** Objects listening for batch clusterer events */
  protected ArrayList<BatchClustererListener> m_batchClustererListeners = 
    new ArrayList<BatchClustererListener>();
  
  /** Objects listening for graph events */
  protected ArrayList<GraphListener> m_graphListeners = 
    new ArrayList<GraphListener>();
  
  /** Objects listening for chart events */
  protected ArrayList<ChartListener> m_chartListeners = 
    new ArrayList<ChartListener>();
  
  /** Objects listening for threshold data events */
  protected ArrayList<ThresholdDataListener> m_thresholdDataListeners = 
    new ArrayList<ThresholdDataListener>();
  
  /** Objects listening for visualizable error events */
  protected ArrayList<VisualizableErrorListener> m_visualizableErrorListeners = 
    new ArrayList<VisualizableErrorListener>();
  
  public GroovyComponent() {
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);    
  }
  
  /**
   * Set the script to use.
   * 
   * @param script the script to use.
   */
  public void setScript(String script) {
    m_groovyScript = script;
    
    if (m_groovyScript.length() > 0) {
      try {
        m_groovyObject = compileScript(m_groovyScript);
        
        // set log and environment variables on the new object
        if (m_log != null) {
          setLog(m_log);
        }
        
        if (m_env != null) {
          setEnvironment(m_env);
        }
      } catch (Exception ex) {
        String message = statusMessagePrefix() + "ERROR: Unable to compile script!"; 
        if (m_log != null) {
          m_log.statusMessage(message);
          m_log.logMessage(message);
        } else {
          System.err.println(message);
        }
      }
    }
    
    if (!(m_groovyObject instanceof KFGroovyScript)) {
      String message = statusMessagePrefix() + "ERROR: Script does not implement" +
      		"KFGroovyScript!"; 
      if (m_log != null) {
        m_log.statusMessage(message);
        m_log.logMessage(message);
      } else {
        System.err.println(message);
      }
    } else {
      try {
        ((KFGroovyScript)m_groovyObject).setManager(this);
        if (m_groovyObject instanceof BeanCommon) {
          // pass on any known incoming connections to the new Groovy
          // object
          for (int i = 0; i < m_incomingConnections.size(); i++) {
            if (connectionAllowed(m_incomingConnectionNames.get(i))) {
              ((BeanCommon)m_groovyObject).
                connectionNotification(m_incomingConnectionNames.get(i), 
                  m_incomingConnections.get(i));
            }
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }
  
  /**
   * Get the current script.
   * 
   * @return the current script.
   */
  public String getScript() {
    return m_groovyScript;
  }
  
  /**
   * Compile a groovy script
   * 
   * @param script the script to compile.
   * @return an new object from the compiled groovy script
   * @throws Exception if there is a problem during compilation
   */
  protected Object compileScript(String script) throws Exception {
    GroovyClassLoader gcl = new GroovyClassLoader();
    Class scriptClass = gcl.parseClass(script);
    return scriptClass.newInstance();
  }
  
  private String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|";
  }
  
  // ---------------- EnvironmentHandler methods ---------
  
  /**
   * Set environment variables to use
   * 
   * @param env the environment variables to use
   */
  public void setEnvironment(Environment env) {
    m_env = env;
    
    // defer to Groovy object
    if (m_groovyObject instanceof EnvironmentHandler) {
      ((EnvironmentHandler)m_groovyObject).setEnvironment(env);
    }
  }

  
  // ---------------- BeanCommon methods -----------------
  
  /**
   * Returns true if, at this time, 
   * the object will accept a connection with respect to the named event
   *
   * @param eventName the event
   * @return true if the object will accept a connection
   */
  public boolean connectionAllowed(String eventName) {
    // defer to Groovy object
    if (m_groovyObject instanceof BeanCommon) {
      return ((BeanCommon)m_groovyObject).connectionAllowed(eventName);
    }
    return false;
  }
  
  /**
   * Returns true if, at this time, 
   * the object will accept a connection with respect to the named event
   *
   * @param esd the event
   * @return true if the object will accept a connection
   */
  public boolean connectionAllowed(EventSetDescriptor esd) {
    // defer to Groovy object
    if (m_groovyObject instanceof BeanCommon) {
      return ((BeanCommon)m_groovyObject).connectionAllowed(esd.getName());
    }
    return false;
  }

  /**
   * Notify this object that it has been registered as a listener with
   * a source with respect to the named event
   *
   * @param eventName the event
   * @param source the source with which this object has been registered as
   * a listener
   */
  public void connectionNotification(String eventName, Object source) {
    // defer to Groovy object
    if (m_groovyObject instanceof BeanCommon) {
      if (connectionAllowed(eventName)) {
        m_incomingConnections.add(source);
        m_incomingConnectionNames.add(eventName);
        ((BeanCommon)m_groovyObject).connectionNotification(eventName, source);
      }
    }
  }

  /**
   * Notify this object that it has been deregistered as a listener with
   * a source with respect to the supplied event name
   *
   * @param eventName the event
   * @param source the source with which this object has been registered as
   * a listener
   */
  public void disconnectionNotification(String eventName, Object source) {
    // defer to Groovy object
    if (m_groovyObject instanceof BeanCommon) {
      m_incomingConnections.remove(source);
      m_incomingConnectionNames.remove(eventName);
      ((BeanCommon)m_groovyObject).disconnectionNotification(eventName, source);
    }
  }

  /**
   * Get the custom (descriptive) name for this component
   *  (if one has been set)
   * 
   * @return the custom name (or the default name)
   */
  public String getCustomName() {
    return m_visual.getText();
  }

  /**
   * Set a custom (descriptive) name for this component
   * 
   * @param name the name to use
   */
  public void setCustomName(String name) {
    m_visual.setText(name);
    if (m_groovyObject instanceof BeanCommon) {
      ((BeanCommon)m_groovyObject).setCustomName(name);
    }
  }
  
  /**
   * Returns true if. at this time, the component is busy with some
   * (i.e. perhaps a worker thread is performing some calculation).
   * 
   * @return true if the bean is busy.
   */
  public boolean isBusy() {
    // defer to Groovy object
    if (m_groovyObject instanceof BeanCommon) {
      return ((BeanCommon)m_groovyObject).isBusy();
    }
    return false;
  }

  /**
   * Set a logger
   *
   * @param logger a <code>Logger</code> value
   */
  public void setLog(Logger logger) {
    m_log = logger;
    
    // pass on m_log to Groovy object
    if (m_groovyObject instanceof BeanCommon) {
      ((BeanCommon)m_groovyObject).setLog(logger);
    }
  }

  /**
   * Stop any processing
   */
  public void stop() {
    // defer to Groovy object
    if (m_groovyObject instanceof BeanCommon) {
      ((BeanCommon)m_groovyObject).stop();
    }
  }
  
  // ---------------- Visible methods ---------------------- 

  /**
   * Gets the visual appearance of this component
   * 
   * @return the visual appearance of this component
   */
  public BeanVisual getVisual() {
    return m_visual;
  }

  /**
   * Sets the visual appearance of this component
   *
   * @param newVisual a <code>BeanVisual</code> value
   */
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;    
  }

  /**
   * Use the default visual appearance for this bean
   */
  public void useDefaultVisual() {
    m_visual.loadIcons("org/pentaho/dm/kf/icons/GroovyComponent.gif",
        "org/pentaho/dm/kf/icons/GroovyComponent.gif");
  }

  // ---------------- EventConstraints methods ----------------------

  /**
   * Returns true, if at the current time, the named event could
   * be generated. Assumes that the supplied event name is
   * an event that could be generated by this bean
   *
   * @param eventName the name of the event in question
   * @return true if the named event could be generated at this point in
   * time
   */
  public boolean eventGeneratable(String eventName) {
    // delegate to Groovy object
    if (m_groovyObject instanceof EventConstraints) {
      return ((EventConstraints)m_groovyObject).eventGeneratable(eventName);
    }
    return false;
  }
  
  // ---------------- UserRequestAcceptor methods ----------------------  

  /**
   * Return an enumeration of the names of any methods that the user
   * can invoke via the main KnowledgeFlow GUI's contextual popup menus
   * at runtime
   * 
   * @return an Enumeration of user-invokable method names.
   */
  public Enumeration enumerateRequests() {
    // delegate to Groovy object
    if (m_groovyObject instanceof UserRequestAcceptor) {
      return ((UserRequestAcceptor)m_groovyObject).enumerateRequests();
    }
    return new Vector<String>().elements();
  }

  /**
   * Invoke a user method.
   * 
   * @param requestName the name of the method to invoke
   */
  public void performRequest(String requestName) {
    // delegate to Groovy object
    if (m_groovyObject instanceof UserRequestAcceptor) {
      ((UserRequestAcceptor)m_groovyObject).performRequest(requestName);
    }
  }
  
  // ---------------- Startable methods --------------------------------
  
  public String getStartMessage() {
    String message = "$Start";
    
    if (m_groovyObject instanceof Startable) {
      message = ((Startable)m_groovyObject).getStartMessage();
    }
    return message;
  }
  
  public void start() throws Exception {
    if (getStartMessage().charAt(0) == '$') {
      return;
    }
    
    if (m_groovyObject instanceof Startable) {
      ((Startable)m_groovyObject).start();
    }
  }
  
  // ---------------- TrainingSetListener methods ----------------------    

  public void acceptTrainingSet(TrainingSetEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof TrainingSetListener) {
      ((TrainingSetListener)m_groovyObject).acceptTrainingSet(e);
    }
  }
  
  // ---------------- TestSetListener methods --------------------------
  
  public void acceptTestSet(TestSetEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof TestSetListener) {
      ((TestSetListener)m_groovyObject).acceptTestSet(e);
    }
  }
  
  // ----------------- DataSourceListener methods -----------------------
  
  public void acceptDataSet(DataSetEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof DataSourceListener) {
      ((DataSourceListener)m_groovyObject).acceptDataSet(e);
    }
  }
  
  // ----------------- InstanceListener methods ------------------------
  
  public void acceptInstance(InstanceEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof InstanceListener) {
      ((InstanceListener)m_groovyObject).acceptInstance(e);
    }
  }
  
  // ----------------- TextListener methods ----------------------------
  
  public void acceptText(TextEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof TextListener) {
      ((TextListener)m_groovyObject).acceptText(e);
    }
  }
  
  // ----------------- BatchClassifierListener methods ------------------
  
  public void acceptClassifier(BatchClassifierEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof BatchClassifierListener) {
      ((BatchClassifierListener)m_groovyObject).acceptClassifier(e);
    }
  }
  
  // ------------------ IncrementalClassifierListener methods ------------
  
  public void acceptClassifier(IncrementalClassifierEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof IncrementalClassifierListener) {
      ((IncrementalClassifierListener)m_groovyObject).acceptClassifier(e);
    }
  }
  
  // ----------------- BatchClustererListener methods --------------------
  
  public void acceptClusterer(BatchClustererEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof BatchClustererListener) {
      ((BatchClustererListener)m_groovyObject).acceptClusterer(e);
    }
  }
  
  // ----------------- GraphListener methods ------------------------------
  
  public void acceptGraph(GraphEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof GraphListener) {
      ((GraphListener)m_groovyObject).acceptGraph(e);
    }
  }
  
  // ----------------- ChartListener methods ------------------------------
  
  public void acceptDataPoint(ChartEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof ChartListener) {
      ((ChartListener)m_groovyObject).acceptDataPoint(e);
    }
  }
  
  // ----------------- ThresholdDataListener methods -----------------------
  
  public void acceptDataSet(ThresholdDataEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof ThresholdDataListener) {
      ((ThresholdDataListener)m_groovyObject).acceptDataSet(e);
    }
  }
  
  // ------------------ VisualizableErrorListener methods ------------------
  
  public void acceptDataSet(VisualizableErrorEvent e) {
    // delegate to Groovy object
    if (m_groovyObject instanceof VisualizableErrorListener) {
      ((VisualizableErrorListener)m_groovyObject).acceptDataSet(e);
    }
  }
  
  // ------------------ ConfigurationListener methods ----------------------
  
  public void acceptConfiguration(ConfigurationEvent e) {
    // nothing to do (configuration handling is done on a pull rather than
    // push basis)
  }
  
  // ------- Registration methods for all events to be emitted ---------
  
  
  public synchronized void addTrainingSetListener(TrainingSetListener t) {
    // delegate to Groovy object if possible
    
//    tryRegistrationMethod(t, "trainingSet", "addTrainingSetListener");
    m_trainingSetListeners.add(t);
  }
  
  public synchronized void removeTrainingSetListener(TrainingSetListener t) {
    m_trainingSetListeners.remove(t);
  }
  
  public synchronized void addTestSetListener(TestSetListener t) {
    m_testSetListeners.add(t);
  }
  
  public synchronized void removeTestSetListener(TestSetListener t) {
    m_testSetListeners.remove(t);
  }
  
  public synchronized void addInstanceListener(InstanceListener t) {
    m_instanceListeners.add(t);
  }
  
  public synchronized void addDataSourceListener(DataSourceListener t) {
    m_dataSourceListeners.add(t);
  }
  
  public synchronized void removeDataSourceListener(DataSourceListener t) {
    m_dataSourceListeners.remove(t);
  }
  
  public synchronized void removeInstanceListener(InstanceListener t) {
    m_instanceListeners.remove(t);
  }
  
  public synchronized void addTextListener(TextListener t) {
    m_textListeners.add(t);
  }
  
  public synchronized void removeTextListener(TextListener t) {
    m_textListeners.remove(t);
  }
  
  public synchronized void addBatchClassifierListener(BatchClassifierListener t) {
    m_batchClassifierListeners.add(t);
  }
  
  public synchronized void removeBatchClassifierListener(BatchClassifierListener t) {
    m_batchClassifierListeners.remove(t);
  }
  
  public synchronized void 
    addIncrementalClassifierListener(IncrementalClassifierListener t) {
    m_incrementalClassifierListeners.add(t);
  }
  
  public synchronized void 
    removeIncrementalClassifierListener(IncrementalClassifierListener t) {
    m_incrementalClassifierListeners.remove(t);
  }
  
  public synchronized void addBatchClustererListener(BatchClustererListener t) {
    m_batchClustererListeners.add(t);
  }
  
  public synchronized void removeBatchClustererListener(BatchClustererListener t) {
    m_batchClustererListeners.remove(t);
  }
  
  public synchronized void addGraphListener(GraphListener t) {
    m_graphListeners.add(t);
  }
  
  public synchronized void removeGraphListener(GraphListener t) {
    m_graphListeners.remove(t);
  }
  
  public synchronized void addChartListener(ChartListener t) {
    m_chartListeners.add(t);
  }
  
  public synchronized void removeChartListener(ChartListener t) {
    m_chartListeners.remove(t);
  }
  
  public synchronized void addThresholdDataListener(ThresholdDataListener t) {
    m_thresholdDataListeners.add(t);
  }
  
  public synchronized void removeThresholdDataListener(ThresholdDataListener t) {
    m_thresholdDataListeners.remove(t);
  }
  
  public synchronized void addVisualizableErrorListener(VisualizableErrorListener t) {
    m_visualizableErrorListeners.add(t);
  }
  
  public synchronized void removeVisualizableErrorListener(VisualizableErrorListener t) {
    m_visualizableErrorListeners.remove(t);
  }
  
  
  // ----- GroovyHelper methods ---
  
  /**
   * Notify the appropriate listener(s) for the supplied
   * event.
   * 
   * @param event the event to pass on to listeners.
   */
  public void notifyListenerType(Object event) {
    if (event instanceof TrainingSetEvent) {
      notifyTrainingSetListeners((TrainingSetEvent)event);
    } else if (event instanceof TestSetEvent) {
      notifyTestSetListeners((TestSetEvent)event);
    } else if (event instanceof DataSetEvent) {
      notifyDataSourceListeners((DataSetEvent)event);
    } else if (event instanceof InstanceEvent) {
      notifyInstanceListeners((InstanceEvent)event);
    } else if (event instanceof TextEvent) {
      notifyTextListeners((TextEvent)event);
    } else if (event instanceof BatchClassifierEvent) {
      notifyBatchClassifierListeners((BatchClassifierEvent)event);
    } else if (event instanceof IncrementalClassifierEvent) {
      notifyIncrementalClassifierListeners((IncrementalClassifierEvent)event);
    } else if (event instanceof BatchClustererEvent) {
      notifyBatchClustererListeners((BatchClustererEvent)event);
    } else if (event instanceof GraphEvent) {
      notifyGraphListeners((GraphEvent)event);
    } else if (event instanceof ChartEvent) {
      notifyChartListeners((ChartEvent)event);
    } else if (event instanceof ThresholdDataEvent) {
      notifyThresholdDataListeners((ThresholdDataEvent)event);
    } else if (event instanceof VisualizableErrorEvent) {
      notifyVisualizableErrorListeners((VisualizableErrorEvent)event);
    }
  }
  
  public ArrayList<TrainingSetListener> getTrainingSetListeners() {
    return m_trainingSetListeners;
  }
  
  public ArrayList<TestSetListener> getTestSetListeners() {
    return m_testSetListeners;
  }
  
  public ArrayList<DataSourceListener> getDataSourceListeners() {
    return m_dataSourceListeners;
  }
  
  public ArrayList<InstanceListener> getInstanceListeners() {
    return m_instanceListeners;
  }
  
  public ArrayList<TextListener> getTextListeners() {
    return m_textListeners;
  }
  
  public ArrayList<BatchClassifierListener> getBatchClassifierListeners() {
    return m_batchClassifierListeners;
  }
  
  public ArrayList<IncrementalClassifierListener> getIncrementalClassifierListeners() {
    return m_incrementalClassifierListeners;
  }
  
  public ArrayList<BatchClustererListener> getBatchClustererListeners() {
    return m_batchClustererListeners;
  }
  
  public ArrayList<GraphListener> getGraphListeners() {
    return m_graphListeners;
  }
  
  public ArrayList<ChartListener> getChartListeners() {
    return m_chartListeners;
  }
  
  public ArrayList<ThresholdDataListener> getThresholdDataListeners() {
    return m_thresholdDataListeners;
  }
  
  public ArrayList<VisualizableErrorListener> getVisualizableErrorListeners() {
    return m_visualizableErrorListeners;
  }
  
  protected void notifyTrainingSetListeners(TrainingSetEvent t) {    
    synchronized (m_trainingSetListeners) {
      for (TrainingSetListener l : m_trainingSetListeners) {
        l.acceptTrainingSet(t);
      }
    } 
  }
  
  protected void notifyTestSetListeners(TestSetEvent t) {    
    synchronized (m_testSetListeners) {
      for (TestSetListener l : m_testSetListeners) {
        l.acceptTestSet(t);
      }
    } 
  }
  
  protected void notifyDataSourceListeners(DataSetEvent t) {    
    synchronized (m_dataSourceListeners) {
      for (DataSourceListener l : m_dataSourceListeners) {
        l.acceptDataSet(t);
      }
    } 
  }
  
  protected void notifyInstanceListeners(InstanceEvent t) {    
    synchronized (m_instanceListeners) {
      for (InstanceListener l : m_instanceListeners) {
        l.acceptInstance(t);
      }
    } 
  }
  
  protected void notifyTextListeners(TextEvent t) {    
    synchronized (m_textListeners) {
      for (TextListener l : m_textListeners) {
        l.acceptText(t);
      }
    } 
  }
  
  protected void notifyBatchClassifierListeners(BatchClassifierEvent t) {    
    synchronized (m_batchClassifierListeners) {
      for (BatchClassifierListener l : m_batchClassifierListeners) {
        l.acceptClassifier(t);
      }
    } 
  }
  
  protected void notifyIncrementalClassifierListeners(IncrementalClassifierEvent t) {    
    synchronized (m_incrementalClassifierListeners) {
      for (IncrementalClassifierListener l : m_incrementalClassifierListeners) {
        l.acceptClassifier(t);
      }
    } 
  }
  
  protected void notifyBatchClustererListeners(BatchClustererEvent t) {    
    synchronized (m_batchClustererListeners) {
      for (BatchClustererListener l : m_batchClustererListeners) {
        l.acceptClusterer(t);
      }
    } 
  }
  
  protected void notifyGraphListeners(GraphEvent t) {    
    synchronized (m_graphListeners) {
      for (GraphListener l : m_graphListeners) {
        l.acceptGraph(t);
      }
    } 
  }
  
  protected void notifyChartListeners(ChartEvent t) {    
    synchronized (m_chartListeners) {
      for (ChartListener l : m_chartListeners) {
        l.acceptDataPoint(t);
      }
    } 
  }
  
  protected void notifyThresholdDataListeners(ThresholdDataEvent t) {    
    synchronized (m_thresholdDataListeners) {
      for (ThresholdDataListener l : m_thresholdDataListeners) {
        l.acceptDataSet(t);
      }
    } 
  }
  
  protected void notifyVisualizableErrorListeners(VisualizableErrorEvent t) {    
    synchronized (m_visualizableErrorListeners) {
      for (VisualizableErrorListener l : m_visualizableErrorListeners) {
        l.acceptDataSet(t);
      }
    } 
  }
}
