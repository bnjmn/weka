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
 *    IncrementalClassifierEvaluator.java
 *    Copyright (C) 2002 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import java.util.Vector;

/**
 * Bean that evaluates incremental classifiers
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.12 $
 */
public class IncrementalClassifierEvaluator
  extends AbstractEvaluator
  implements IncrementalClassifierListener,
	     EventConstraints {

  /** for serialization */
  private static final long serialVersionUID = -3105419818939541291L;

  private transient Evaluation m_eval;

  private transient Classifier m_classifier;
  
  private Vector m_listeners = new Vector();
  private Vector m_textListeners = new Vector();

  private Vector m_dataLegend = new Vector();

  private ChartEvent m_ce = new ChartEvent(this);
  private double [] m_dataPoint = new double[1];
  private boolean m_reset = false;

  private double m_min = Double.MAX_VALUE;
  private double m_max = Double.MIN_VALUE;

  public IncrementalClassifierEvaluator() {
     m_visual.loadIcons(BeanVisual.ICON_PATH
		       +"IncrementalClassifierEvaluator.gif",
		       BeanVisual.ICON_PATH
		       +"IncrementalClassifierEvaluator_animated.gif");
    m_visual.setText("IncrementalClassifierEvaluator");
  }

  /**
   * Global info for this bean
   *
   * @return a <code>String</code> value
   */
  public String globalInfo() {
    return "Evaluate the performance of incrementally trained classifiers.";
  }

  /**
   * Accepts and processes a classifier encapsulated in an incremental
   * classifier event
   *
   * @param ce an <code>IncrementalClassifierEvent</code> value
   */
  public void acceptClassifier(final IncrementalClassifierEvent ce) {
    try {
      if (ce.getStatus() == IncrementalClassifierEvent.NEW_BATCH) {
	//	m_eval = new Evaluation(ce.getCurrentInstance().dataset());
	m_eval = new Evaluation(ce.getStructure());
	m_dataLegend = new Vector();
	m_reset = true;
	m_dataPoint = new double[0];
	Instances inst = ce.getStructure();
	System.err.println("NEW BATCH");
	/* if (inst.classIndex() >= 0) {
	  if (inst.attribute(inst.classIndex()).isNominal()) {
	    if (inst.isMissing(inst.classIndex())) {
	      m_dataLegend.addElement("Confidence");
	    } else {
	      m_dataLegend.addElement("Accuracy");
	    }
	  } else {
	    if (inst.isMissing(inst.classIndex())) {
	      m_dataLegend.addElement("Prediction");
	    } else {
	      m_dataLegend.addElement("RRSE");
	    }
	  }
	} */
      } else {
	Instance inst = ce.getCurrentInstance();
	//	if (inst.attribute(inst.classIndex()).isNominal()) {
	double [] dist = ce.getClassifier().distributionForInstance(inst);
	double pred = 0;
	if (!inst.isMissing(inst.classIndex())) {
	  m_eval.evaluateModelOnce(dist, inst);
	} else {
	  pred = ce.getClassifier().classifyInstance(inst);
	}
	if (inst.classIndex() >= 0) {
	  // need to check that the class is not missing
	  if (inst.attribute(inst.classIndex()).isNominal()) {
	    if (!inst.isMissing(inst.classIndex())) {
	      if (m_dataPoint.length < 2) {
		m_dataPoint = new double[2];
		m_dataLegend.addElement("Accuracy");
		m_dataLegend.addElement("RMSE (prob)");
	      }
	      //		int classV = (int) inst.value(inst.classIndex());
	      m_dataPoint[1] = m_eval.rootMeanSquaredError();
	      //  		int maxO = Utils.maxIndex(dist);
	      //  		if (maxO == classV) {
	      //  		  dist[classV] = -1;
	      //  		  maxO = Utils.maxIndex(dist);
	      //  		}
	      //  		m_dataPoint[1] -= dist[maxO];
	    } else {
	      if (m_dataPoint.length < 1) {
		m_dataPoint = new double[1];
		m_dataLegend.addElement("Confidence");
	      }
	    }
	    double primaryMeasure = 0;
	    if (!inst.isMissing(inst.classIndex())) {
	      primaryMeasure = 1.0 - m_eval.errorRate();
	    } else {
	      // record confidence as the primary measure
	      // (another possibility would be entropy of
	      // the distribution, or perhaps average
	      // confidence)
	      primaryMeasure = dist[Utils.maxIndex(dist)];
	    }
	    //	    double [] dataPoint = new double[1];
	    m_dataPoint[0] = primaryMeasure;
	    //	    double min = 0; double max = 100;
	    /*	    ChartEvent e = 
		    new ChartEvent(IncrementalClassifierEvaluator.this, 
		    m_dataLegend, min, max, dataPoint); */
	    m_ce.setLegendText(m_dataLegend);
	    m_ce.setMin(0); m_ce.setMax(1);
	    m_ce.setDataPoint(m_dataPoint);
	    m_ce.setReset(m_reset);
	    m_reset = false;
	  } else {
	    // numeric class
	    if (m_dataPoint.length < 1) {
	      m_dataPoint = new double[1];
	      if (inst.isMissing(inst.classIndex())) {
		m_dataLegend.addElement("Prediction");
	      } else {
		m_dataLegend.addElement("RMSE");
	      }
	    }
	    if (!inst.isMissing(inst.classIndex())) {
	      double update;
	      if (!inst.isMissing(inst.classIndex())) {
		update = m_eval.rootMeanSquaredError();
	      } else {
		update = pred;
	      }
	      m_dataPoint[0] = update;
	      if (update > m_max) {
		  m_max = update;
	      }
	      if (update < m_min) {
		m_min = update;
	      }
	    }
	    
	    m_ce.setLegendText(m_dataLegend);
	    m_ce.setMin((inst.isMissing(inst.classIndex()) 
			 ? m_min
			 : 0)); 
	    m_ce.setMax(m_max);
	    m_ce.setDataPoint(m_dataPoint);
	    m_ce.setReset(m_reset);
	    m_reset = false;
	  }
	  notifyChartListeners(m_ce);

	  if (ce.getStatus() == IncrementalClassifierEvent.BATCH_FINISHED) {
	    if (m_textListeners.size() > 0) {
	      String textTitle = ce.getClassifier().getClass().getName();
	      textTitle = 
		textTitle.substring(textTitle.lastIndexOf('.')+1,
				    textTitle.length());
	      String results = "=== Performance information ===\n\n"
		+  "Scheme:   " + textTitle + "\n"
		+  "Relation: "+ inst.dataset().relationName() + "\n\n"
		+ m_eval.toSummaryString();
	      textTitle = "Results: " + textTitle;
	      TextEvent te = 
		new TextEvent(this, 
			      results,
			    textTitle);
	      notifyTextListeners(te);
	    }
	  }
	}
      }
    } catch (Exception ex) {
      ex.printStackTrace();
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
	  eventGeneratable("incrementalClassifier")) {
	return false;
      }
    }
    return true;
  }

  /**
   * Stop all action
   */
  public void stop() {
    // tell the listenee (upstream bean) to stop
    if (m_listenee instanceof BeanCommon) {
      //      System.err.println("Listener is BeanCommon");
      ((BeanCommon)m_listenee).stop();
    }
  }

  private void notifyChartListeners(ChartEvent ce) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_listeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	((ChartListener)l.elementAt(i)).acceptDataPoint(ce);
      }
    }
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
   * Add a chart listener
   *
   * @param cl a <code>ChartListener</code> value
   */
  public synchronized void addChartListener(ChartListener cl) {
    m_listeners.addElement(cl);
  }

  /**
   * Remove a chart listener
   *
   * @param cl a <code>ChartListener</code> value
   */
  public synchronized void removeChartListener(ChartListener cl) {
    m_listeners.remove(cl);
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
}
