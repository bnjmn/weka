/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    Associator.java
 *    Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import weka.associations.Apriori;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.gui.Logger;

import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JPanel;

/**
 * Bean that wraps around weka.associations
 *
 * @author Mark Hall (mhall at cs dot waikato dot ac dot nz)
 * @version $Revision$
 * @since 1.0
 * @see JPanel
 * @see BeanCommon
 * @see Visible
 * @see WekaWrapper
 * @see Serializable
 * @see UserRequestAcceptor
 * @see TrainingSetListener
 * @see DataSourceListener
 */
public class Associator
  extends JPanel
  implements BeanCommon, Visible, 
	     WekaWrapper, EventConstraints,
	     Serializable, UserRequestAcceptor,
             DataSourceListener,
	     TrainingSetListener {

  /** for serialization */
  private static final long serialVersionUID = -7843500322130210057L;

  protected BeanVisual m_visual = 
    new BeanVisual("Associator",
		   BeanVisual.ICON_PATH+"DefaultAssociator.gif",
		   BeanVisual.ICON_PATH+"DefaultAssociator_animated.gif");

  private static int IDLE = 0;
  private static int BUILDING_MODEL = 1;

  private int m_state = IDLE;

  private Thread m_buildThread = null;

  /**
   * Global info for the wrapped associator (if it exists).
   */
  protected String m_globalInfo;

  /**
   * Objects talking to us
   */
  private Hashtable m_listenees = new Hashtable();

  /**
   * Objects listening for text events
   */
  private Vector m_textListeners = new Vector();

  /**
   * Objects listening for graph events
   */
  private Vector m_graphListeners = new Vector();

  private weka.associations.Associator m_Associator = new Apriori();

  private transient Logger m_log = null;

  /**
   * Global info (if it exists) for the wrapped classifier
   *
   * @return the global info
   */
  public String globalInfo() {
    return m_globalInfo;
  }

  /**
   * Creates a new <code>Associator</code> instance.
   */
  public Associator() {
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
    setAssociator(m_Associator);
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
   * Set the associator for this wrapper
   *
   * @param c a <code>weka.associations.Associator</code> value
   */
  public void setAssociator(weka.associations.Associator c) {
    boolean loadImages = true;
    if (c.getClass().getName().
	compareTo(m_Associator.getClass().getName()) == 0) {
      loadImages = false;
    } 
    m_Associator = c;
    String associatorName = c.getClass().toString();
    associatorName = associatorName.substring(associatorName.
					      lastIndexOf('.')+1, 
					      associatorName.length());
    if (loadImages) {
      if (!m_visual.loadIcons(BeanVisual.ICON_PATH+associatorName+".gif",
		       BeanVisual.ICON_PATH+associatorName+"_animated.gif")) {
	useDefaultVisual();
      }
    }
    m_visual.setText(associatorName);

    // get global info
    m_globalInfo = KnowledgeFlowApp.getGlobalInfo(m_Associator);
  }
  
  /**
   * Get the associator currently set for this wrapper
   *
   * @return a <code>weka.associations.Associator</code> value
   */
  public weka.associations.Associator getAssociator() {
    return m_Associator;
  }

  /**
   * Sets the algorithm (associator) for this bean
   *
   * @param algorithm an <code>Object</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public void setWrappedAlgorithm(Object algorithm) {

    if (!(algorithm instanceof weka.associations.Associator)) { 
      throw new IllegalArgumentException(algorithm.getClass()+" : incorrect "
					 +"type of algorithm (Associator)");
    }
    setAssociator((weka.associations.Associator)algorithm);
  }

  /**
   * Returns the wrapped associator
   *
   * @return an <code>Object</code> value
   */
  public Object getWrappedAlgorithm() {
    return getAssociator();
  }

  /**
   * Accept a training set
   *
   * @param e a <code>TrainingSetEvent</code> value
   */
  public void acceptTrainingSet(TrainingSetEvent e) {
    // construct and pass on a DataSetEvent
    Instances trainingSet = e.getTrainingSet();
    DataSetEvent dse = new DataSetEvent(this, trainingSet);
    acceptDataSet(dse);
  }

  public void acceptDataSet(final DataSetEvent e) {
    if (e.isStructureOnly()) {
      // no need to build an associator, just absorb and return
      return;
    }


    if (m_buildThread == null) {
      try {
	if (m_state == IDLE) {
	  synchronized (this) {
	    m_state = BUILDING_MODEL;
	  }
	  final Instances trainingData = e.getDataSet();
//	  final String oldText = m_visual.getText();
	  m_buildThread = new Thread() {
	      public void run() {
		try {
		  if (trainingData != null) {
		    m_visual.setAnimated();
//		    m_visual.setText("Building model...");
		    if (m_log != null) {
		      m_log.statusMessage(statusMessagePrefix() 
		          + "Building model...");
		    }
		    buildAssociations(trainingData);

		    if (m_textListeners.size() > 0) {
		      String modelString = m_Associator.toString();
		      String titleString = m_Associator.getClass().getName();
		      
		      titleString = titleString.
			substring(titleString.lastIndexOf('.') + 1,
				  titleString.length());
		      modelString = "=== Associator model ===\n\n" +
			"Scheme:   " +titleString+"\n" +
			"Relation: "  + trainingData.relationName() + 
                        "\n\n"
			+ modelString;
		      titleString = "Model: " + titleString;

		      TextEvent nt = new TextEvent(Associator.this,
						   modelString,
						   titleString);
		      notifyTextListeners(nt);
		    }

                    if (m_Associator instanceof weka.core.Drawable && 
			m_graphListeners.size() > 0) {
		      String grphString = 
			((weka.core.Drawable)m_Associator).graph();
                      int grphType = ((weka.core.Drawable)m_Associator).graphType();
		      String grphTitle = m_Associator.getClass().getName();
		      grphTitle = grphTitle.substring(grphTitle.
						      lastIndexOf('.')+1, 
						      grphTitle.length());
		      grphTitle = " ("
			+e.getDataSet().relationName() + ") "
			+grphTitle;
		      
		      GraphEvent ge = new GraphEvent(Associator.this, 
						     grphString, 
						     grphTitle,
                                                     grphType);
		      notifyGraphListeners(ge);
		    }
		  }
		} catch (Exception ex) {
		  Associator.this.stop();
		  if (m_log != null) {
		    m_log.statusMessage(statusMessagePrefix()
		        + "ERROR (See log for details)");
		    m_log.logMessage("[Associator] " + statusMessagePrefix()
		        + " problem training associator. " + ex.getMessage());
		  }
		  ex.printStackTrace();
		} finally {
//		  m_visual.setText(oldText);
		  m_visual.setStatic();
		  m_state = IDLE;
		  if (isInterrupted()) {
		    if (m_log != null) {
                      String titleString = m_Associator.getClass().getName();		      
		      titleString = titleString.
			substring(titleString.lastIndexOf('.') + 1,
				  titleString.length());
		      m_log.logMessage("[Associator] " + statusMessagePrefix() 
		          + " Build associator interrupted!");
		      m_log.statusMessage(statusMessagePrefix() + "INTERRUPTED");
		    }
		  } else {
		    if (m_log != null) {
		      m_log.statusMessage(statusMessagePrefix() + "Finished.");
		    }
		  }
		  block(false);
		}
	      }	
	    };
          m_buildThread.setPriority(Thread.MIN_PRIORITY);
	  m_buildThread.start();
	  // make sure the thread is still running before we block
	  //	  if (m_buildThread.isAlive()) {
	  block(true);
	    //	  }
	  m_buildThread = null;
	  m_state = IDLE;
	}
      } catch (Exception ex) {
	ex.printStackTrace();
      }
    }
  }


  private void buildAssociations(Instances data) 
    throws Exception {
    m_Associator.buildAssociations(data);
  }

  /**
   * Sets the visual appearance of this wrapper bean
   *
   * @param newVisual a <code>BeanVisual</code> value
   */
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  /**
   * Gets the visual appearance of this wrapper bean
   */
  public BeanVisual getVisual() {
    return m_visual;
  }

  /**
   * Use the default visual appearance for this bean
   */
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH+"DefaultAssociator.gif",
		       BeanVisual.ICON_PATH+"DefaultAssociator_animated.gif");
  }

  /**
   * Add a text listener
   *
   * @param cl a <code>TextListener</code> value
   */
  public synchronized void addTextListener(TextListener cl) {
    m_textListeners.addElement(cl);
  }

  /**
   * Remove a text listener
   *
   * @param cl a <code>TextListener</code> value
   */
  public synchronized void removeTextListener(TextListener cl) {
    m_textListeners.remove(cl);
  }

  /**
   * Add a graph listener
   *
   * @param cl a <code>GraphListener</code> value
   */
  public synchronized void addGraphListener(GraphListener cl) {
    m_graphListeners.addElement(cl);
  }

  /**
   * Remove a graph listener
   *
   * @param cl a <code>GraphListener</code> value
   */
  public synchronized void removeGraphListener(GraphListener cl) {
    m_graphListeners.remove(cl);
  }

  /**
   * Notify all text listeners of a text event
   *
   * @param ge a <code>TextEvent</code> value
   */
  private void notifyTextListeners(TextEvent ge) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_textListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	((TextListener)l.elementAt(i)).acceptText(ge);
      }
    }
  }

  /**
   * Notify all graph listeners of a graph event
   *
   * @param ge a <code>GraphEvent</code> value
   */
  private void notifyGraphListeners(GraphEvent ge) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_graphListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	((GraphListener)l.elementAt(i)).acceptGraph(ge);
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
    if (m_listenees.containsKey(eventName)) {
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
      m_listenees.put(eventName, source);
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
    m_listenees.remove(eventName);
  }

  /**
   * Function used to stop code that calls acceptTrainingSet. This is 
   * needed as classifier construction is performed inside a separate
   * thread of execution.
   *
   * @param tf a <code>boolean</code> value
   */
  private synchronized void block(boolean tf) {

    if (tf) {
      try {
	  // only block if thread is still doing something useful!
	if (m_buildThread.isAlive() && m_state != IDLE) {
	  wait();
	  }
      } catch (InterruptedException ex) {
      }
    } else {
      notifyAll();
    }
  }  
  
  /**
   * Returns true if. at this time, the bean is busy with some
   * (i.e. perhaps a worker thread is performing some calculation).
   * 
   * @return true if the bean is busy.
   */
  public boolean isBusy() {
    return (m_buildThread != null);
  }

  /**
   * Stop any associator action
   */
  public void stop() {
    // tell all listenees (upstream beans) to stop
    Enumeration en = m_listenees.keys();
    while (en.hasMoreElements()) {
      Object tempO = m_listenees.get(en.nextElement());
      if (tempO instanceof BeanCommon) {
	((BeanCommon)tempO).stop();
      }
    }

    // stop the build thread
    if (m_buildThread != null) {
      m_buildThread.interrupt();
      m_buildThread.stop();
      m_buildThread = null;
      m_visual.setStatic();
    }
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
   * Return an enumeration of requests that can be made by the user
   *
   * @return an <code>Enumeration</code> value
   */
  public Enumeration enumerateRequests() {
    Vector newVector = new Vector(0);
    if (m_buildThread != null) {
      newVector.addElement("Stop");
    }
    return newVector.elements();
  }

  /**
   * Perform a particular request
   *
   * @param request the request to perform
   * @exception IllegalArgumentException if an error occurs
   */
  public void performRequest(String request) {
    if (request.compareTo("Stop") == 0) {
      stop();
    } else {
      throw new IllegalArgumentException(request
					 + " not supported (Associator)");
    }
  }

  /**
   * Returns true, if at the current time, the event described by the
   * supplied event descriptor could be generated.
   *
   * @param esd an <code>EventSetDescriptor</code> value
   * @return a <code>boolean</code> value
   */
  public boolean eventGeneratable(EventSetDescriptor esd) {
    String eventName = esd.getName();
    return eventGeneratable(eventName);
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
    if (eventName.compareTo("text") == 0 ||
        eventName.compareTo("graph") == 0) {
      if (!m_listenees.containsKey("dataSet") &&
	  !m_listenees.containsKey("trainingSet")) {
	return false;
      }
      Object source = m_listenees.get("trainingSet");
      if (source != null && source instanceof EventConstraints) {
	if (!((EventConstraints)source).eventGeneratable("trainingSet")) {
	  return false;
	}
      }
      source = m_listenees.get("dataSet");
      if (source != null && source instanceof EventConstraints) {
	if (!((EventConstraints)source).eventGeneratable("dataSet")) {
	  return false;
	}
      }

      if (eventName.compareTo("graph") == 0 &&
          !(m_Associator instanceof weka.core.Drawable)) {
        return false;
      }
    }
    return true;
  }
  
  private String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|"
    + ((m_Associator instanceof OptionHandler && 
        Utils.joinOptions(((OptionHandler)m_Associator).getOptions()).length() > 0) 
        ? Utils.joinOptions(((OptionHandler)m_Associator).getOptions()) + "|"
            : "");
  }
}
