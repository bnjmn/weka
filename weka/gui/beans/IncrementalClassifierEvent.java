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
 *    IncrementalClassifierEvent.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.core.Instances;
import weka.core.Instance;
import weka.classifiers.Classifier;
import weka.classifiers.DistributionClassifier;
import java.util.EventObject;

/**
 * Class encapsulating an incrementally built classifier and current instance
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version 1.0
 * @since 1.0
 * @see EventObject
 */
public class IncrementalClassifierEvent extends EventObject {

  public final static int WITHIN_BATCH = 0;
  public final static int NEW_BATCH = 1;
  public final static int BATCH_FINISHED = 2;

  private int m_status;
  protected Classifier m_classifier;
  protected Instance m_currentInstance;

  /**
   * Creates a new <code>IncrementalClassifierEvent</code> instance.
   *
   * @param source the source of the event
   * @param scheme the classifier
   * @param tstI the current instance
   */
  public IncrementalClassifierEvent(Object source, Classifier scheme,
			 Instance currentI, int status) {
    super(source);
    //    m_trainingSet = trnI;
    m_classifier = scheme;
    m_currentInstance = currentI;
    m_status = status;
  }

  public IncrementalClassifierEvent(Object source) {
    super(source);
  }

  /**
   * Get the classifier
   *
   * @return the classifier
   */
  public Classifier getClassifier() {
    return m_classifier;
  }
  
  public void setClassifier(Classifier c) {
    m_classifier = c;
  }

  /**
   * Get the current instance
   *
   * @return the current instance
   */
  public Instance getCurrentInstance() {
    return m_currentInstance;
  }

  /**
   * Set the current instance for this event
   *
   * @param i an <code>Instance</code> value
   */
  public void setCurrentInstance(Instance i) {
    m_currentInstance = i;
  }

  /**
   * Get the status
   *
   * @return an <code>int</code> value
   */
  public int getStatus() {
    return m_status;
  }

  /**
   * Set the status
   *
   * @param s an <code>int</code> value
   */
  public void setStatus(int s) {
    m_status = s;
  }
}

