/*
 *    PlotData2D.java
 *    Copyright (C) 2000 Mark Hall
 *
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

package weka.gui.visualize;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Utils;

import java.awt.Color;

/**
 * This class is a container for plottable data. Instances form the
 * primary data. An optional array of classifier/clusterer predictions
 * (associated 1 for 1 with the instances) can also be provided.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class PlotData2D {

  /** The instances */
  protected Instances m_plotInstances = null;

  /** The name of this plot */
  protected String m_plotName = "new plot";

  /**  The predictions array (if any) associated with this plot */
  protected double [] m_preds = null;

  /** Colour data using the predictions array (for clusterers) */
  protected boolean m_colourUsingPreds = false;

  /** Predictions are numeric? */
  protected boolean m_predsNumeric = false;

  /** When colouring using predictions (ie clusters), this is the
      highest cluster number */
  protected int m_highestPred = 0;

  /** Custom colour for this plot */
  public boolean m_useCustomColour = false;

  public Color m_customColour = null;

  /** Panel coordinate cache for data points */
  protected double [][] m_pointLookup;

  /** These are used to determine bounds */

  /** The x index */
  private int m_xIndex;

  /** The y index */
  private int m_yIndex;

  /** The colouring index */
  private int m_cIndex;

  /** Holds the min and max values of the x, y and colouring attributes 
   for this plot */
  protected double m_maxX;
  protected double m_minX;
  protected double m_maxY;
  protected double m_minY;
  protected double m_maxC;
  protected double m_minC;

  public PlotData2D(Instances insts) {
    m_plotInstances = insts;
    m_xIndex = m_yIndex = m_cIndex = 0;
    m_pointLookup = new double [m_plotInstances.numInstances()][5];
    determineBounds();
  }

  /**
   * Returns the instances for this plot
   * @return the instances for this plot
   */
  public Instances getPlotInstances() {
    return new Instances(m_plotInstances);
  }

  /**
   * Set the name of this plot
   * @param name the name for this plot
   */
  public void setPlotName(String name) {
    m_plotName = name;
  }

  /**
   * Get the name of this plot
   * @return the name of this plot
   */
  public String getPlotName() {
    return m_plotName;
  }

  /**
   * Set an array of predictions corresponding 1 to 1 with the instances
   * for this plot
   * @param preds the predictions
   */
  public void setPredictions(double [] preds) {
    m_preds = preds;
    if (m_plotInstances.classIndex() < 0) {
      setPredictionsNumeric(false);
      setColourUsingPreds(true);
      m_highestPred = (int)m_preds[Utils.maxIndex(m_preds)];
    } else {
      calculateErrors();
    }
  }

  /**
   * Get the predictions (if any) for this plot
   * @return the array of predictions or null if there are no predictions
   */
  public double [] getPredictions() {
    if (m_preds != null) {
      return (double [])m_preds.clone();
    } else {
      return null;
    }
  }

  /**
   * Specify that colouring for this plot should be done on the basis
   * of the predictions array (ie, the predictions are from a clusterer)
   * @param c true if colouring should be done on the basis of the predictions
   * array
   */
  public void setColourUsingPreds(boolean c) {
    m_colourUsingPreds = c;
    determineBounds();
  }

  /**
   * Returns true if colouring for this plot should be done on the basis of
   * the predictions array.
   * @return true if colouring should be done using the predictions array
   */
  public boolean getColourUsingPreds() {
    return m_colourUsingPreds;
  }

  /**
   * Specify whether the predictions array contains numeric predictions
   * rather than discrete class predictions
   * @param n true if the prediction array contains numeric predictions
   */
  public void setPredictionsNumeric(boolean n) {
    m_predsNumeric = n;
    if (m_predsNumeric && m_preds != null) {
      calculateErrors();
    }
  }

  /**
   * Get whether predictions are numeric or not
   * @return true if predictions array contaings numeric predictions
   */
  public boolean getPredictionsNumeric() {
    return m_predsNumeric;
  }

  /**
   * Set a custom colour to use for this plot. This overides any
   * data index to use for colouring. If null, then will revert back
   * to the default (no custom colouring).
   * @param c a custom colour to use for this plot or null (default---no
   * colouring).
   */
  public void setCustomColour(Color c) {
    m_customColour = c;
    if (c != null) {
      m_useCustomColour = true;
    } else {
      m_useCustomColour = false;
    }
  }

  /**
   * Set the x index of the data.
   * @param x the x index
   */
  public void setXindex(int x) {
    m_xIndex = x;
    determineBounds();
  }

  /**
   * Set the y index of the data
   * @param y the y index
   */
  public void setYindex(int y) {
    m_yIndex = y;
    determineBounds();
  }

  /**
   * Set the colouring index of the data
   * @param c the colouring index
   */
  public void setCindex(int c) {
    m_cIndex = c;
    determineBounds();
  }

  /**
   * Get the currently set x index of the data
   * @return the current x index
   */
  public int getXindex() {
    return m_xIndex;
  }

  /**
   * Get the currently set y index of the data
   * @return the current y index
   */
  public int getYindex() {
    return m_yIndex;
  }

  /**
   * Get the currently set colouring index of the data
   * @return the current colouring index
   */
  public int getCindex() {
    return m_cIndex;
  }


  /**
   * Calculate errors (numeric class) from predictions array and store
   * in the point lookup cache
   */
  private void calculateErrors() {
    int maxpSize = 20;
    double maxErr = Double.NEGATIVE_INFINITY;
    double minErr = Double.POSITIVE_INFINITY;
    
    int cind = m_plotInstances.classIndex();
    double err;
    for (int jj=0;jj<m_plotInstances.numInstances();jj++) {
      err = Math.abs(m_preds[jj] - 
		     m_plotInstances.instance(jj).
		     value(cind));

      if (err < minErr) {
	minErr = err;
      }
      if (err > maxErr) {
	maxErr = err;
      }
    }

    for (int i=0;i<m_plotInstances.numInstances();i++) {
      if (m_plotInstances.instance(i).isMissing(m_xIndex) ||
	  m_plotInstances.instance(i).isMissing(m_yIndex) ||
	  m_plotInstances.instance(i).
	  isMissing(m_plotInstances.classIndex())) {
      } else {
	err = Math.abs((m_preds[i] -
			m_plotInstances.instance(i).
			value(m_plotInstances.classIndex())));
	err = ((err - minErr)/(maxErr - minErr)) * maxpSize;
	
	if (err < 1) {
	  err = 1; // just so the point is visible!
	}
	m_pointLookup[i][2] = err;
      }
    }
  }

  /**
   * Determine bounds for the current x,y and colouring indexes
   */
  private void determineBounds() {
     double value,min,max;
    
    if (m_plotInstances != null) {
      // x bounds
      min=Double.POSITIVE_INFINITY;
      max=Double.NEGATIVE_INFINITY;
      if (m_plotInstances.attribute(m_xIndex).isNominal()) {
	m_minX = 0;
	m_maxX = m_plotInstances.attribute(m_xIndex).numValues()-1;
      } else {
	for (int i=0;i<m_plotInstances.numInstances();i++) {
	  if (!m_plotInstances.instance(i).isMissing(m_xIndex)) {
	    value = m_plotInstances.instance(i).value(m_xIndex);
	    if (value < min) {
	      min = value;
	    }
	    if (value > max) {
	      max = value;
	    }
	  }
	}
	
	m_minX = min; m_maxX = max;
	if (min == max) {
	  m_maxX += 0.05;
	  m_minX -= 0.05;
	}
      }

      // y bounds
      min=Double.POSITIVE_INFINITY;
      max=Double.NEGATIVE_INFINITY;
      if (m_plotInstances.attribute(m_yIndex).isNominal()) {
	m_minY = 0;
	m_maxY = m_plotInstances.attribute(m_yIndex).numValues()-1;
      } else {
	for (int i=0;i<m_plotInstances.numInstances();i++) {
	  if (!m_plotInstances.instance(i).isMissing(m_yIndex)) {
	    value = m_plotInstances.instance(i).value(m_yIndex);
	    if (value < min) {
	      min = value;
	    }
	    if (value > max) {
	      max = value;
	    }
	  }
	}
	
	m_minY = min; m_maxY = max;
	if (min == max) {
	  m_maxY += 0.05;
	  m_minY -= 0.05;
	}
      }
      
      // colour bounds
      min=Double.POSITIVE_INFINITY;
      max=Double.NEGATIVE_INFINITY;
      if (!m_colourUsingPreds) {
	for (int i=0;i<m_plotInstances.numInstances();i++) {
	  if (!m_plotInstances.instance(i).isMissing(m_cIndex)) {
	    value = m_plotInstances.instance(i).value(m_cIndex);
	    if (value < min) {
	      min = value;
	    }
	    if (value > max) {
	      max = value;
	    }
	  }
	}
      }
     
      m_minC = min; m_maxC = max;
    }
  }
}
