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
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.classifiers.Classifier;
import weka.classifiers.DistributionClassifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.Utils;
import weka.gui.Logger;

import java.io.Serializable;
import java.util.Vector;
import java.util.Enumeration;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import java.awt.*;

/**
 * Bean that evaluates incremental classifiers
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.3 $
 */
public class IncrementalClassifierEvaluator
  extends AbstractEvaluator
  implements IncrementalClassifierListener,
	     EventConstraints {

  private transient Evaluation m_eval;

  private transient Classifier m_classifier;
  
  private Vector m_listeners = new Vector();

  private Vector m_dataLegend = new Vector();

  private ChartEvent m_ce = new ChartEvent(this);
  private double [] m_dataPoint = new double[1];
  private boolean m_reset = false;

  private double m_min = Double.MAX_VALUE;
  private double m_max = Double.MIN_VALUE;

  public IncrementalClassifierEvaluator() {
    super();
    m_visual.setText("IncrementalClassifierEvaluator");
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
	m_eval = new Evaluation(ce.getCurrentInstance().dataset());
	m_dataLegend = new Vector();
	m_reset = true;
	m_dataPoint = new double[1];
	Instance inst = ce.getCurrentInstance();
	if (inst.classIndex() >= 0) {
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
	}
      } else {
	Instance inst = ce.getCurrentInstance();
	//	if (inst.attribute(inst.classIndex()).isNominal()) {
	  double [] dist = null;
	  double pred = 0;
	  if (ce.getClassifier() instanceof DistributionClassifier) {
	    dist = ((DistributionClassifier)ce.getClassifier())
	      .distributionForInstance(inst);
	  }
	  if (dist == null) {
	    if (!inst.isMissing(inst.classIndex())) {
	      m_eval.evaluateModelOnce(ce.getClassifier(), inst);
	    } else {
	      pred = ce.getClassifier().classifyInstance(inst);
	    }
	  } else {
	    if (!inst.isMissing(inst.classIndex())) {
	      m_eval.evaluateModelOnce(dist, inst);
	    } else {
	      pred = ce.getClassifier().classifyInstance(inst);
	    }
	  }
	  if (inst.classIndex() >= 0) {
	    // need to check that the class is not missing
	    if (inst.attribute(inst.classIndex()).isNominal()) {
	      if (dist != null && !inst.isMissing(inst.classIndex())) {
		if (m_dataPoint.length < 2) {
		  m_dataPoint = new double[2];
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
	      }
	      double primaryMeasure = 0;
	      if (!inst.isMissing(inst.classIndex())) {
		primaryMeasure = 1.0 - m_eval.errorRate();
	      } else if (dist != null) {
		// record confidence as the primary measure
		// (another possibility would be entropy of
		// the distribution, or perhaps average
		// confidence)
		primaryMeasure = dist[Utils.maxIndex(dist)];
	      } else {
		// need something for non distribution classifiers when the
		// actual class is missing!
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
	      if (dist != null && !inst.isMissing(inst.classIndex())) {
		double update;
		if (!inst.isMissing(inst.classIndex())) {
		  update = m_eval.rootRelativeSquaredError();
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
	  }
	  //	}
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
    // nothing to do
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
}
