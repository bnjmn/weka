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
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.FastVector;
import weka.gui.Logger;
import weka.gui.visualize.PlotData2D;

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
 * @version $Revision: 1.13 $
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
  
  private Vector m_textListeners = new Vector();
  private Vector m_thresholdListeners = new Vector();
  private Vector m_visualizableErrorListeners = new Vector();

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

  // ----- Stuff for ROC curves
  private boolean m_rocListenersConnected = false;
  // Plottable Instances with predictions appended
  private Instances m_predInstances = null;
  // Actual predictions
  private FastVector m_plotShape = null;
  private FastVector m_plotSize = null;

  /**
   * Accept a classifier to be evaluated
   *
   * @param ce a <code>BatchClassifierEvent</code> value
   */
  public void acceptClassifier(final BatchClassifierEvent ce) {
    if (ce.getTestSet().isStructureOnly()) {
      return; // cant evaluate empty instances
    }
    try {
      if (m_evaluateThread == null) {
	m_evaluateThread = new Thread() {
	    public void run() {
	      final String oldText = m_visual.getText();
	      try {
		if (ce.getSetNumber() == 1 || 
		    ce.getClassifier() != m_classifier) {
		  m_eval = new Evaluation(ce.getTestSet().getDataSet());
		  m_classifier = ce.getClassifier();
		  m_predInstances = 
		    weka.gui.explorer.ClassifierPanel.
		    setUpVisualizableInstances(new Instances(ce.getTestSet().getDataSet()));
		  m_plotShape = new FastVector();
		  m_plotSize = new FastVector();
		}
		if (ce.getSetNumber() <= ce.getMaxSetNumber()) {
		  m_visual.setText("Evaluating ("+ce.getSetNumber()+")...");
		  if (m_logger != null) {
		    m_logger.statusMessage("ClassifierPerformaceEvaluator : "
					   +"evaluating ("+ce.getSetNumber()
					   +")...");
		  }
		  m_visual.setAnimated();
		  /*
		  m_eval.evaluateModel(ce.getClassifier(), 
		  ce.getTestSet().getDataSet()); */
		  for (int i = 0; i < ce.getTestSet().getDataSet().numInstances(); i++) {
		    Instance temp = ce.getTestSet().getDataSet().instance(i);
		    weka.gui.explorer.ClassifierPanel.
		    processClassifierPrediction(temp, ce.getClassifier(),
						m_eval, m_predInstances, m_plotShape,
						m_plotSize);
		  }
		}
		
		if (ce.getSetNumber() == ce.getMaxSetNumber()) {
		  System.err.println(m_eval.toSummaryString());
		  // m_resultsString.append(m_eval.toSummaryString());
		  // m_outText.setText(m_resultsString.toString());
		  String textTitle = m_classifier.getClass().getName();
		  textTitle = 
		    textTitle.substring(textTitle.lastIndexOf('.')+1,
					textTitle.length());
		  String resultT = "=== Evaluation result ===\n\n"
		    + "Scheme: " + textTitle + "\n"
		    + "Relation: " + ce.getTestSet().getDataSet().relationName()
		    + "\n\n" + m_eval.toSummaryString();
                  
                  if (ce.getTestSet().getDataSet().
                      classAttribute().isNominal()) {
                    resultT += "\n" + m_eval.toClassDetailsString()
                      + "\n" + m_eval.toMatrixString();
                  }
                  
		  TextEvent te = 
		    new TextEvent(ClassifierPerformanceEvaluator.this, 
				  resultT,
				  textTitle);
		  notifyTextListeners(te);

                  // set up visualizable errors
                  if (m_visualizableErrorListeners.size() > 0) {
                    PlotData2D errorD = new PlotData2D(m_predInstances);
                    errorD.setShapeSize(m_plotSize);
                    errorD.setShapeType(m_plotShape);
                    errorD.setPlotName(textTitle+" ("
                                       +ce.getTestSet().getDataSet().relationName()
                                       +")");
                    errorD.addInstanceNumberAttribute();
                    VisualizableErrorEvent vel = 
                      new VisualizableErrorEvent(ClassifierPerformanceEvaluator.this,
                                                 errorD);
                    notifyVisualizableErrorListeners(vel);
                  }
                  

		  if (ce.getTestSet().getDataSet().classAttribute().isNominal()) {
		    ThresholdCurve tc = new ThresholdCurve();
		    Instances result = tc.getCurve(m_eval.predictions(), 0);
		    result.
		      setRelationName(ce.getTestSet().getDataSet().relationName());
		    PlotData2D pd = new PlotData2D(result);
		    pd.setPlotName(textTitle+" ("
				   +ce.getTestSet().getDataSet().
				                   classAttribute().value(0)
				   +")");
		    boolean [] connectPoints = 
		      new boolean [result.numInstances()];
		    for (int jj = 1; jj < connectPoints.length; jj++) {
		      connectPoints[jj] = true;
		    }
		    pd.setConnectPoints(connectPoints);
		    ThresholdDataEvent rde = 
		      new ThresholdDataEvent(ClassifierPerformanceEvaluator.this,
				       pd);
		    notifyThresholdListeners(rde);
		    /*te = new TextEvent(ClassifierPerformanceEvaluator.this,
				       result.toString(),
				       "ThresholdCurveInst");
				       notifyTextListeners(te); */
		  }
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
   * Add a threshold data listener
   *
   * @param cl a <code>ThresholdDataListener</code> value
   */
  public synchronized void addThresholdDataListener(ThresholdDataListener cl) {
    m_thresholdListeners.addElement(cl);
  }

  /**
   * Remove a Threshold data listener
   *
   * @param cl a <code>ThresholdDataListener</code> value
   */
  public synchronized void removeThresholdDataListener(ThresholdDataListener cl) {
    m_thresholdListeners.remove(cl);
  }

  /**
   * Add a visualizable error listener
   *
   * @param vel a <code>VisualizableErrorListener</code> value
   */
  public synchronized void addVisualizableErrorListener(VisualizableErrorListener vel) {
    m_visualizableErrorListeners.add(vel);
  }

  /**
   * Remove a visualizable error listener
   *
   * @param vel a <code>VisualizableErrorListener</code> value
   */
  public synchronized void removeVisualizableErrorListener(VisualizableErrorListener vel) {
    m_visualizableErrorListeners.remove(vel);
  }

  /**
   * Notify all text listeners of a TextEvent
   *
   * @param te a <code>TextEvent</code> value
   */
  private void notifyTextListeners(TextEvent te) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_textListeners.clone();
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
   * Notify all ThresholdDataListeners of a ThresholdDataEvent
   *
   * @param te a <code>ThresholdDataEvent</code> value
   */
  private void notifyThresholdListeners(ThresholdDataEvent re) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_thresholdListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	//	System.err.println("Notifying text listeners "
	//			   +"(ClassifierPerformanceEvaluator)");
	((ThresholdDataListener)l.elementAt(i)).acceptDataSet(re);
      }
    }
  }

  /**
   * Notify all VisualizableErrorListeners of a VisualizableErrorEvent
   *
   * @param te a <code>VisualizableErrorEvent</code> value
   */
  private void notifyVisualizableErrorListeners(VisualizableErrorEvent re) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_visualizableErrorListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	//	System.err.println("Notifying text listeners "
	//			   +"(ClassifierPerformanceEvaluator)");
	((VisualizableErrorListener)l.elementAt(i)).acceptDataSet(re);
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

