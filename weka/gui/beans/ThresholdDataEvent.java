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
 *    ThresholdDataEvent.java
 *    Copyright (C) 2003 Mark Hall
 *
 */

package weka.gui.beans;

import weka.gui.visualize.PlotData2D;

import java.util.EventObject;

/**
 * Event encapsulating classifier performance data based on
 * varying a threshold over the classifier's predicted probabilities
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.2 $
 * @see EventObject
 */
public class ThresholdDataEvent
  extends EventObject {

  /** for serialization */
  private static final long serialVersionUID = -8309334224492439644L;

  private PlotData2D m_dataSet;

  public ThresholdDataEvent(Object source, PlotData2D dataSet) {
    super(source);
    m_dataSet = dataSet;
  }
  
  /**
   * Return the instances of the data set
   *
   * @return an <code>Instances</code> value
   */
  public PlotData2D getDataSet() {
    return m_dataSet;
  }
}
