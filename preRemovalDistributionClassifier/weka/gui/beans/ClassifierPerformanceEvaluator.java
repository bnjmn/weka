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
 *    ClassifierPerformanceEvaluator.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.gui.Logger;

import java.io.Serializable;
import java.util.Vector;
import java.util.Enumeration;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.JFrame;
import javax.swing.BorderFactory;
import java.awt.*;
import javax.swing.JScrollPane;

/**
 * A bean that evaluates the performance of batch trained classifiers
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.4 $
 */
public class ClassifierPerformanceEvaluator 
  extends AbstractEvaluator
  implements BatchClassifierListener, 
	     Serializable, UserRequestAcceptor, EventConstraints {

  /**
   * Evaluation object used for evaluating a classifier
   */
  private transient Evaluation m_eval;

  /**
   * Holds the classifier to be evaluated
   */
  private transient Classifier m_classifier;

  private transient Thread m_evaluateThread = null;
  
  private Vector m_listeners = new Vector();

  public ClassifierPerformanceEvaluator() {
    m_visual.loadIcons(BeanVisual.ICON_PATH
		       +"ClassifierPerformanceEvaluator.gif",
		       BeanVisual.ICON_PATH
		       +"ClassifierPerformanceEvaluator_animated.gif");
    m_visual.setText("ClassifierPerformanceEvaluator");
  }
  
  /**
   * Global info for this bean
   *
   * @return a <code>String</code> value
   */
  public String globalInfo() {
    return "Evaluate the performance of batch trained classifiers.";
  }
  
  /**
   * Accept a classifier to be evaluated
   *
   * @param ce a <code>BatchClassifierEvent</code> value
   */
  public void acceptClassifier(final BatchClassifierEvent ce) {
    try {
      if (m_evaluateThread == null) {
	m_evaluateThread = new Thread() {
	    public void run() {
	      final String oldText = m_visual.getText();
	      try {
		if (ce.getSetNumber() == 1 || 
		    ce.getClassifier() != m_classifier) {
		  m_eval = new Evaluation(ce.getTestSet());
		  m_classifier = ce.getClassifier();
		}
		
		if (ce.getSetNumber() <= ce.getMaxSetNumber()) {
		  m_visual.setText("Evaluating ("+ce.getSetNumber()+")...");
		  if (m_logger != null) {
		    m_logger.statusMessage("ClassifierPerformaceEvaluator : "
					   +"evaluating ("+ce.getSetNumber()
					   +")...");
		  }
		  m_visual.setAnimated();
		  m_eval.evaluateModel(ce.getClassifier(), ce.getTestSet());
		}
		
		if (ce.getSetNumber() == ce.getMaxSetNumber()) {
		  System.err.println(m_eval.toSummaryString());
		  // m_resultsString.append(m_eval.toSummaryString());
		  // m_outText.setText(m_resultsString.toString());
		  String textTitle = m_classifier.getClass().getName();
		  textTitle = 
		    textTitle.substring(textTitle.lastIndexOf('.')+1,
					textTitle.length());
		  TextEvent te = 
		    new TextEvent(ClassifierPerformanceEvaluator.this, 
				  m_eval.toSummaryString(),
				  textTitle);
		  notifyTextListeners(te);
		  if (m_logger != null) {
		    m_logger.statusMessage("Done.");
		  }
		}
	      } catch (Exception ex) {
		ex.printStackTrace();
	      } finally {
		m_visual.setText(oldText);
		m_visual.setStatic();
		m_evaluateThread = null;
		if (isInterrupted()) {
		  if (m_logger != null) {
		    m_logger.logMessage("Evaluation interrupted!");
		    m_logger.statusMessage("OK");
		  }
		}
		block(false);
	      }
	    }
	  };
	m_evaluateThread.setPriority(Thread.MIN_PRIORITY);
	m_evaluateThread.start();

	// make sure the thread is still running before we block
	//	if (m_evaluateThread.isAlive()) {
	block(true);
	  //	}
	m_evaluateThread = null;
      }
    }  catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Try and stop any action
   */
  public void stop() {
    // tell the listenee (upstream bean) to stop
    if (m_listenee instanceof BeanCommon) {
      System.err.println("Listener is BeanCommon");
      ((BeanCommon)m_listenee).stop();
    }

    // stop the evaluate thread
    if (m_evaluateThread != null) {
      m_evaluateThread.interrupt();
      m_evaluateThread.stop();
    }
  }
  
  /**
   * Function used to stop code that calls acceptClassifier. This is 
   * needed as classifier evaluation is performed inside a separate
   * thread of execution.
   *
   * @param tf a <code>boolean</code> value
   */
  private synchronized void block(boolean tf) {
    if (tf) {
      try {
	// only block if thread is still doing something useful!
	if (m_evaluateThread != null && m_evaluateThread.isAlive()) {
	  wait();
	}
      } catch (InterruptedException ex) {
      }
    } else {
      notifyAll();
    }
  }

  /**
   * Return an enumeration of user activated requests for this bean
   *
   * @return an <code>Enumeration</code> value
   */
  public Enumeration enumerateRequests() {
    Vector newVector = new Vector(0);
    if (m_evaluateThread != null) {
      newVector.addElement("Stop");
    }
    return newVector.elements();
  }

  /**
   * Perform the named request
   *
   * @param request the request to perform
   * @exception IllegalArgumentException if an error occurs
   */
  public void performRequest(String request) {
    if (request.compareTo("Stop") == 0) {
      stop();
    } else {
      throw new 
	IllegalArgumentException(request

		    + " not supported (ClassifierPerformanceEvaluator)");
    }
  }

  /**
   * Add a text listener
   *
   * @param cl a <code>TextListener</code> value
   */
  public synchronized void addTextListener(TextListener cl) {
    m_listeners.addElement(cl);
  }

  /**
   * Remove a text listener
   *
   * @param cl a <code>TextListener</code> value
   */
  public synchronized void removeTextListener(TextListener cl) {
    m_listeners.remove(cl);
  }

  /**
   * Notify all text listeners of a TextEvent
   *
   * @param te a <code>TextEvent</code> value
   */
  private void notifyTextListeners(TextEvent te) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_listeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	//	System.err.println("Notifying text listeners "
	//			   +"(ClassifierPerformanceEvaluator)");
	((TextListener)l.elementAt(i)).acceptText(te);
      }
    }
  }

  /**
   * Returns true, if at the current time, the named event could
   * be generated. Assumes that supplied event names are names of
   * events that could be generated by this bean.
   *
   * @param eventName the name of the event in question
   * @return true if the named event could be generated at this point in
   * time
   */
  public boolean eventGeneratable(String eventName) {
    if (m_listenee == null) {
      return false;
    }

    if (m_listenee instanceof EventConstraints) {
      if (!((EventConstraints)m_listenee).
	  eventGeneratable("batchClassifier")) {
	return false;
      }
    }
    return true;
  }
}

