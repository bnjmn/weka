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
 *    PMMLClassifierScoring.java
 *    Copyright (C) 2008 Pentaho Corporation
 *
 */

package weka.gui.beans;

import weka.classifiers.pmml.consumer.PMMLClassifier;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instances;
import weka.gui.Logger;
import weka.core.pmml.PMMLFactory;
import weka.core.pmml.PMMLModel;

import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.*;
import java.util.ArrayList;

import javax.swing.JPanel;

/**
 * Plugin KnowledgeFlow component for performing prediction (scoring) using
 * PMML models.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision 1.0 $
 */
@KFStep(category = "PMML", toolTipText = "Scoring using a PMML model")
public class PMMLClassifierScoring extends JPanel 
  implements BeanCommon, EventConstraints, 
  InstanceListener, TestSetListener, Serializable, 
             Visible, EnvironmentHandler {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -6805133068937296042L;

  /** Holds the pmml classifier to use */
  protected transient PMMLClassifier m_pmmlClassifier;
  
  /** Holds the path to the PMML classifier to use */
  protected String m_fileName = null;
  
  /** The logger */
  protected transient Logger m_log;

  /** The environment variables to use */
  protected transient Environment m_env;
  
  /**
   * Incremental classifier event for notifying downstream
   * components
   */
  protected IncrementalClassifierEvent m_ie =
    new IncrementalClassifierEvent(this);
  
  /** IncrementalClassifier listeners that we are talking to */
  protected ArrayList<IncrementalClassifierListener> 
    m_incrementalClassifierListeners = new ArrayList<IncrementalClassifierListener>();
  
  /** BatchClassifier listeners that we are talking to */
  protected ArrayList<BatchClassifierListener> 
    m_batchClassifierListeners = new ArrayList<BatchClassifierListener>();
  
  /** Component talking to us (i.e. producing InstanceEvents or TestSetEvents) */
  protected Object m_listenee = null;
  
  /** Incoming connection type */
  String m_incomingConnection = "";
  
  /** Visual representation */
  protected BeanVisual m_visual =
    new BeanVisual("PMMLClassifierScoring",
                   BeanVisual.ICON_PATH+"DefaultClassifier.gif",
                   BeanVisual.ICON_PATH+"DefaultClassifier_animated.gif");
  
  /**                                                                                           
   * Creates a new <code>PMMLClassifierScoring</code> instance.                                            
   */
  public PMMLClassifierScoring() {
    setLayout(new BorderLayout());
    useDefaultVisual();
    add(m_visual, BorderLayout.CENTER);
    //setClassifier(m_Classifier);
  }

  /**
   * Global about information for this component.
   * 
   * @return global information for this component
   */
  public String globalInfo() {
    return "Load classifiers from PMML and use for prediciton.";
  }  
  
  /**
   * Gets the PMMLClassifier that will be used.
   * 
   * @return the PMMLClassifier that will be used to score incoming data
   */
  public PMMLClassifier getClassifier() {
    return m_pmmlClassifier;
  }
  
  /**
   * Accepts and processes an TestSetEvent. Uses the PMMLClassifier to
   * score each instance in the test set.
   * 
   * @param e the TestSetEvent
   */
  public void acceptTestSet(TestSetEvent e) {
    if (e.isStructureOnly()) {
      if (!loadModel()) {
        return;
      }
    }
    
    if (m_pmmlClassifier == null) {
      return;
    }
    
    Instances testingSet = e.getTestSet();
    if (testingSet != null) {
      if (testingSet.classIndex() < 0) {
        testingSet.setClassIndex(testingSet.numAttributes()-1);
      }
    }


    BatchClassifierEvent ce =
      new BatchClassifierEvent(this, m_pmmlClassifier,
          new DataSetEvent(this, new Instances(testingSet, 0)),
          new DataSetEvent(this, testingSet),
          e.getSetNumber(), e.getMaxSetNumber());

    //      System.err.println("Just before notify classifier listeners");                                
    notifyBatchClassifierListeners(ce);
    //      System.err.println("Just after notify classifier listeners");
    
    // finished scoring run. Reset mapping.
    m_pmmlClassifier.done();

  }
  
  private boolean loadModel() {
    try {
      // if we don't have a model or a filename is set,
      // then see if we can load the PMML classifier here.
      if (m_pmmlClassifier == null || !isEmpty(m_fileName)) {

        if (!isEmpty(m_fileName)) {
          // replace any environment variables in the filename
          if (m_env == null) {
            m_env = Environment.getSystemWide();
          }
          String fileName = m_env.substitute(m_fileName);
          PMMLModel model = PMMLFactory.getPMMLModel(fileName, m_log);
          if (model instanceof PMMLClassifier) {
            m_pmmlClassifier = (PMMLClassifier)model;
          }
        } else {
          logOrError("Model is null or no filename specified to load from!");
          return false;
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      logOrError(ex.getMessage());
      return false;
    }
    return true;
  }
  
 /**
  * Accepts and processes an InstanceEvent. Uses the PMMLClassifier to
  * score the incoming instance.
  * 
  * @param e the InstanceEvent
  */
  public void acceptInstance(InstanceEvent e) {
    if (e.getStatus() == InstanceEvent.FORMAT_AVAILABLE ) {
      Instances dataset = e.getStructure();
      
      // default to the last column if no class is set                                          
      if (dataset.classIndex() < 0) {
        dataset.setClassIndex(dataset.numAttributes()-1);
      }

      if (!loadModel()) {
        return;
      }
      
      // Notify incremental classifier listeners of new batch                                   
      // System.err.println("NOTIFYING NEW BATCH");
      m_ie.setStructure(dataset);
      m_ie.setClassifier(m_pmmlClassifier);

      notifyIncrementalClassifierListeners(m_ie);
      return;
    }
    
    if (m_pmmlClassifier == null) {
      return;
    }
    
    try {
      // test on this instance
      int status = IncrementalClassifierEvent.WITHIN_BATCH;
      if (e.getStatus() == InstanceEvent.BATCH_FINISHED) {
        status = IncrementalClassifierEvent.BATCH_FINISHED;
      }
      
      m_ie.setStatus(status); m_ie.setClassifier(m_pmmlClassifier);
      m_ie.setCurrentInstance(e.getInstance());
      notifyIncrementalClassifierListeners(m_ie);
      
      if (status == IncrementalClassifierEvent.BATCH_FINISHED) {
        // scoring run finished. reset mapping status.
        m_pmmlClassifier.done();
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      logOrError(ex.getMessage());
    }
  }
  
  /**                                                                                                             
   * Notify all batch classifier listeners of a batch classifier event                                            
   *                                                                                                              
   * @param ce a <code>BatchClassifierEvent</code> value                                                          
   */
  private void notifyBatchClassifierListeners(BatchClassifierEvent ce) {
    ArrayList<BatchClassifierListener> l = null;
    synchronized (this) {
      l = (ArrayList<BatchClassifierListener>)m_batchClassifierListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
        l.get(i).acceptClassifier(ce);
      }
    }
  }
  
  /**                                                                                           
   * Notify all incremental classifier listeners of an incremental classifier                   
   * event                                                                                      
   *                                                                                            
   * @param ce an <code>IncrementalClassifierEvent</code> value                                 
   */
  private void
    notifyIncrementalClassifierListeners(IncrementalClassifierEvent ce) {
    ArrayList<IncrementalClassifierListener> l = null;
    synchronized (this) {
      l = (ArrayList<IncrementalClassifierListener>)m_incrementalClassifierListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
        l.get(i).acceptClassifier(ce);
      }
    }
  }
 
  /**
   * Utility method to check if a String is null or empty ("").
   *  
   * @param aString the String to check.
   * @return true if the supplied String is null or empty.
   */
  protected static boolean isEmpty(String aString) {
    if (aString == null || aString.length() == 0) {
      return true;
    }
    return false;
  }
  
  /**
   * Logs an error message to the log or System.err if the log
   * object is not set.
   * 
   * @param message the message to log
   */
  protected void logOrError(String message) {
    message = "[PMMLClassifierScoring] " + message;
    if (m_log != null) {
      m_log.logMessage(message);
    } else {
      System.err.println(message);
    }
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
   * Set a logger
   *
   * @param logger a <code>Logger</code> value
   */
  public void setLog(Logger logger) {
    m_log = logger;
  }
  
  /**
   * Stop the component from executing. Also attempts to tell
   * the immediate upstream component to stop.
   */
  public void stop() {
    // tell upstream component to stop
    if (m_listenee != null) {
      if (m_listenee instanceof BeanCommon) {
        ((BeanCommon)m_listenee).stop();
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
    if (m_listenee != null) {
      return false;
    }
    return true;
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
    if (connectionAllowed(eventName)) {
      m_listenee = source;
      m_incomingConnection = eventName;
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
  public synchronized void disconnectionNotification(String eventName,
                                                     Object source) {
    if (m_listenee == source) {
      m_listenee = null;
      m_incomingConnection = "";
    }
  }

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
    if (eventName.compareTo("incrementalClassifier") == 0
        || eventName.compareTo("batchClassifier") == 0) {
      if (m_listenee == null) {
        return false;
      }
      
      if (eventName.equals("incrementalClassifier") && 
          !m_incomingConnection.equals("instance")) {
        return false;
      }
      
      if (eventName.equals("batchClassifier") &&
          !m_incomingConnection.equals("testSet")) {
        return false;
      }

      if (m_listenee instanceof EventConstraints) {
        if (!((EventConstraints)m_listenee).eventGeneratable(eventName)) {
          return false;
        }
      }
    }
    return true;
  }
  
  /**
   * Set the filename to load from.
   * 
   * @param filename the filename to load from
   */
  public void setFilename(String filename) {
    m_fileName = filename;
  }
  
  /**
   * Get the filename to load from.
   * 
   * @return the filename to load from.
   */
  public String getFilename() {
    return m_fileName;
  }
  
  /**                                                                                           
   * Add an incremental classifier listener                                                     
   *                                                                                            
   * @param cl an <code>IncrementalClassifierListener</code> value                              
   */
  public synchronized void
    addIncrementalClassifierListener(IncrementalClassifierListener cl) {
    m_incrementalClassifierListeners.add(cl);
  }

  /**                                                                                           
   * Remove an incremental classifier listener                                                  
   *                                                                                            
   * @param cl an <code>IncrementalClassifierListener</code> value                              
   */
  public synchronized void
    removeIncrementalClassifierListener(IncrementalClassifierListener cl) {
    m_incrementalClassifierListeners.remove(cl);
  }
  
  /**                                                                                                             
   * Add a batch classifier listener                                                                              
   *                                                                                                              
   * @param cl a <code>BatchClassifierListener</code> value                                                       
   */
  public synchronized void
    addBatchClassifierListener(BatchClassifierListener cl) {
    m_batchClassifierListeners.add(cl);
  }

  /**                                                                                                             
   * Remove a batch classifier listener                                                                           
   *                                                                                                              
   * @param cl a <code>BatchClassifierListener</code> value                                                       
   */
  public synchronized void
    removeBatchClassifierListener(BatchClassifierListener cl) {
    m_batchClassifierListeners.remove(cl);
  }
  
  /**                                                                                           
   * Use the default images for a data source                                                   
   *                                                                                            
   */
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH+"DefaultClassifier.gif",
                       BeanVisual.ICON_PATH+"DefaultClassifier_animated.gif");
  }
  
  /**                                                                                           
   * Set the visual for this data source                                                        
   *                                                                                            
   * @param newVisual a <code>BeanVisual</code> value                                           
   */
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  /**
   * Get the visual being used by this data source.
   *
   */
  public BeanVisual getVisual() {
    return m_visual;
  }

  public boolean isBusy() {
    return false;
  }

  /**
   * Set environment variables to use.
   * 
   * @param env the environment variables to
   * use
   */
  public void setEnvironment(Environment env) {
    m_env = env;
  }
}
