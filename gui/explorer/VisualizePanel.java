/*
 *    VisualizePanel.java
 *    Copyright (C) 1999 Mark Hall
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

package weka.gui.explorer;

import weka.classifiers.*;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.Utils;
import weka.gui.Logger;
import weka.gui.SysErrLog;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JFrame;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JViewport;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

/** 
 * This panel allows the user to visualize a dataset (and if provided) a
 * classifier's predictions in two dimensions.
 *
 * If the user selects a nominal attribute as the colouring attribute then
 * each point is drawn in a colour that corresponds to the discrete value
 * of that attribute for the instance. If the user selects a numeric
 * attribute to colour on, then the points are coloured using a spectrum
 * ranging from blue to red (low values to high).
 *
 * When a classifiers predictions are supplied they are plotted in one
 * of two ways (depending on whether the class is nominal or numeric).<br>
 * For nominal class: an error made by a classifier is plotted as a square
 * in the colour corresponding to the class it predicted.<br>
 * For numeric class: predictions are plotted as varying sized x's, where
 * the size of the x is related to the magnitude of the error.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class VisualizePanel extends JPanel {

  /** Inner class to handle plotting */
  protected class PlotPanel extends JPanel {

    /** The instances to be plotted */
    protected Instances m_plotInstances=null;

    /** Indexes of the attributes to go on the x and y axis and the attribute
	to use for colouring */
    protected int m_xIndex=0;
    protected int m_yIndex=0;
    protected int m_cIndex=0;

    /** Set to true if colouring is to be done solely using predictions.
	Used when visualizing a clusterer's output */
    protected boolean m_colourUsingPreds = false;

    /** Holds the min and max values of the x, y and colouring attributes */
    protected double m_maxX;
    protected double m_minX;
    protected double m_maxY;
    protected double m_minY;
    protected double m_maxC;
    protected double m_minC;

    /** Holds the predictions of a classifier */
    private double [] m_preds=null;

    /** True if the predictions are for a numeric class */
    private boolean m_predsNumeric = false;
    
    /** Axis padding */
    private final int m_axisPad = 5;

    /** Tick size */
    private final int m_tickSize = 5;

    /** Height of the spectrum reference bar */
    private final int m_spectrumHeight = 5;

    /**the offsets of the axes once label metrics are calculated */
    private int m_XaxisStart;
    private int m_YaxisStart;
    private int m_XaxisEnd;
    private int m_YaxisEnd;

    /** Font for labels */
    private Font m_labelFont;
    private FontMetrics m_labelMetrics=null; 

    /** default colours for colouring discrete class */
    protected Color [] m_DefaultColors = {Color.blue,
					  Color.red,
					  Color.green,
					  Color.cyan,
					  Color.magenta,
					  Color.yellow,
                                          Color.orange,
                                          Color.gray,
                                          Color.darkGray,
                                          Color.black};


    /** Constructor */
    public PlotPanel() {
      setBackground(Color.white);
    }

    /**
     * Set the index of the attribute to go on the x axis
     * @param x the index of the attribute to use on the x axis
     */
    public void setXindex(int x) {
      m_xIndex = x;
      repaint();
    }

    /**
     * Set the index of the attribute to go on the y axis
     * @param y the index of the attribute to use on the y axis
     */
    public void setYindex(int y) {
      m_yIndex = y;
      repaint();
    }

    /**
     * Set the index of the attribute to use for colouring
     * @param c the index of the attribute to use for colouring
     */
    public void setCindex(int c) {
      m_cIndex = c;
      repaint();
    }

    /**
     * Set the instances to plot
     * @param inst the instances
     */
    public void setInstances(Instances inst) {
      m_plotInstances=inst;
      m_xIndex=0;
      m_yIndex=0;
    }

    /**
     * Set whether classifier predictions are for a numeric class
     * @param n true if classifier predictions are for a numeric class
     */
    public void setPredictionsNumeric(boolean n) {
      m_predsNumeric = n;
    }

    /**
     * Set an array of classifier predictions to be plotted. The predictions
     * need to correspond one to one with the Instances provided for plotting.
     * @param preds an array of classifier predictions
     */
    public void setPredictions(double [] preds) {
      m_preds = preds;
    }

    /**
     * Set up fonts and font metrics
     * @param gx the graphics context
     */
    private void setFonts(Graphics gx) {
      if (m_labelMetrics == null) {
	m_labelFont = new Font("Sanserif", Font.PLAIN, 12);
	m_labelMetrics = gx.getFontMetrics(m_labelFont);
      }
    }

    /**
     * Determine the min and max values for axis and colouring attributes
     */
    private void determineBounds() {
      double value,min,max;
      
      // x bounds
      min=Double.POSITIVE_INFINITY;
      max=Double.NEGATIVE_INFINITY;
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

      // y bounds
      min=Double.POSITIVE_INFINITY;
      max=Double.NEGATIVE_INFINITY;
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

    /**
     * Convert an raw x value to Panel x coordinate.
     * @param xval the raw x value
     * @return an x value for plotting in the panel.
     */
    private double convertToPanelX(double xval) {
      double temp = (xval - m_minX)/(m_maxX - m_minX);
      double temp2 = temp * (m_XaxisEnd - m_XaxisStart);
      
      return (temp2 + m_XaxisStart);
    }

    /**
     * Convert an raw y value to Panel y coordinate.
     * @param yval the raw y value
     * @return an y value for plotting in the panel.
     */
    private double convertToPanelY(double yval) {
      double temp = (yval - m_minY)/(m_maxY - m_minY);
      double temp2 = temp * (m_YaxisEnd - m_YaxisStart);
      
      return (m_YaxisEnd - temp2);
    }

    /**
     * Draws a data point at a given set of panel coordinates at a given
     * size.
     * @param x the x coord
     * @param y the y coord
     * @param size the size of the point
     * @param gx the graphics context
     */
    private void drawDataPoint(double x, 
			       double y,
			       int size,
			       Graphics gx) {
      if (size == 0) {
	size = 1;
      }

      gx.drawLine((int)(x-size),(int)(y-size),
	       (int)(x+size),(int)(y+size));
      gx.drawLine((int)(x+size),(int)(y-size),
	       (int)(x-size),(int)(y+size));
      
    }

    /**
     * Draws the data points and predictions (if provided).
     * @param gx the graphics context
     */
    private void paintData(Graphics gx) {
      double maxErr = Double.NEGATIVE_INFINITY;
      double minErr = Double.POSITIVE_INFINITY;

      // determine range of error for numeric predictions if necessary
      if (m_preds != null 
	  && m_predsNumeric 
	  && (m_plotInstances.classIndex() >= 0)) {
	int cind = m_plotInstances.classIndex();
	double err;
	for (int jj=0;jj<m_plotInstances.numInstances();jj++) {
	  err = Math.abs(m_preds[jj] - m_plotInstances.instance(jj).value(cind));
	  if (err < minErr) {
	    minErr = err;
	  }
	  if (err > maxErr) {
	    maxErr = err;
	  }
	}
      }

      for (int i=0;i<m_plotInstances.numInstances();i++) {
	if (m_plotInstances.instance(i).isMissing(m_xIndex) ||
	    m_plotInstances.instance(i).isMissing(m_yIndex)) {
	} else {
	  double x = convertToPanelX(m_plotInstances.
				     instance(i).value(m_xIndex));
	  double y = convertToPanelY(m_plotInstances.
				     instance(i).value(m_yIndex));
	  if (m_plotInstances.attribute(m_cIndex).isNominal() || 
	      m_colourUsingPreds) {

	    // if the cIndex is less than 0 then we are visualizing
	    // a clusterer's predictions and therefore the dataset has
	    // no apriori class to use for colouring
	    int ci = (!m_colourUsingPreds) 
	      ? (int)(m_plotInstances.instance(i).value(m_cIndex)) % 10
	      : (int)(m_preds[i] % 10);
	      
	    int mp = (!m_colourUsingPreds)
	      ? (int)(m_plotInstances.instance(i).value(m_cIndex) / 10)
	      : (int)(m_preds[i] / 10);
	    
	    mp *= 2;
	    Color pc = m_DefaultColors[ci];
	    for (int j=0;j<mp;j++) {
	      pc = pc.brighter();
	    }
	    
	    gx.setColor(pc);	    
	    drawDataPoint(x,y,2,gx);
	    if (m_preds != null 
		&& !m_predsNumeric 
		&& (m_plotInstances.classIndex() >= 0)) {
	      if (m_preds[i] != 
		  m_plotInstances.instance(i).
		  value(m_plotInstances.classIndex())) {
		ci = (int)(m_preds[i] % 10);
		mp = (int)(m_preds[i] / 10);
		mp *= 2;
		pc = m_DefaultColors[ci];
		for (int j=0;j<mp;j++) {
		  pc = pc.brighter();
		}
		
		gx.setColor(pc);
		gx.drawRect((int)(x-3),(int)(y-3),6,6);
		ci = (int)(m_plotInstances.instance(i).value(m_plotInstances.classIndex()) % 10);
	      }
	    }    
	  } else {
	    int maxpSize = 20;
	    if (m_preds != null 
		&& m_plotInstances.classIndex() >= 0 
		&& m_predsNumeric) {
	      double err = Math.abs((m_preds[i] -
				     m_plotInstances.instance(i).
				     value(m_plotInstances.classIndex())));
	      err = ((err - minErr)/(maxErr - minErr)) * maxpSize;
	      double r = (m_plotInstances.instance(i).
			  value(m_cIndex) - m_minC) /
		          (m_maxC - m_minC);
	      r = (r * 240) + 15;
	      gx.setColor(new Color((int)r,0,(int)(255-r)));
	      drawDataPoint(x,y,(int)err,gx);
	    } else if (m_preds != null) {
	      
	    } else {
	      double r = (m_plotInstances.instance(i).value(m_cIndex) - m_minC) /
		(m_maxC - m_minC);
	      r = (r * 240) + 15;
	      gx.setColor(new Color((int)r,0,(int)(255-r)));
	      drawDataPoint(x,y,2,gx);
	    }
	  }
	}
      }
    }

    /**
     * Draws the axis and a spectrum if the colouring attribute is numeric
     * @param gx the graphics context
     */
    private void paintAxis(Graphics gx) {
      setFonts(gx);

      int h = getHeight();
      int w = getWidth();
      int hf = m_labelMetrics.getAscent();
      int mswx=0;
      int mswy=0;

      determineBounds();
      int fieldWidthX = (int)((Math.log(m_maxX)/Math.log(10)))+1;
      String maxStringX = Utils.doubleToString(m_maxX,fieldWidthX+2,1);
      mswx = m_labelMetrics.stringWidth(maxStringX);
      int fieldWidthY = (int)((Math.log(m_maxY)/Math.log(10)))+1;
      String maxStringY = Utils.doubleToString(m_maxY,fieldWidthY+2,1);
      mswy = m_labelMetrics.stringWidth(maxStringY);
      m_YaxisStart = m_axisPad;
      m_XaxisStart = 0+m_axisPad+m_tickSize+mswy;
      m_XaxisEnd = w-m_axisPad-(mswx/2);
      m_YaxisEnd = h-m_axisPad-hf-m_tickSize;

      if ((!m_colourUsingPreds) && 
	  m_plotInstances.attribute(m_cIndex).isNumeric()) {
	m_YaxisEnd -= (m_spectrumHeight+hf+m_tickSize+m_axisPad);
	// draw spectrum
	double rs = 15;
	double incr = 240.0 / (double)(m_XaxisEnd-m_XaxisStart);
	for (int i=m_XaxisStart;i<
	       (m_XaxisStart+(m_XaxisEnd-m_XaxisStart));i++) {
	  Color c = new Color((int)rs,0,(int)(255-rs));
	  gx.setColor(c);
	  gx.drawLine(i,(m_YaxisEnd+hf+m_tickSize+m_axisPad),
		   i,(m_YaxisEnd+hf+m_tickSize+m_axisPad+m_spectrumHeight));
	  rs += incr;
	}

	int fieldWidthC = (int)((Math.log(m_maxC)/Math.log(10)))+1;
	String maxStringC = Utils.doubleToString(m_maxC,fieldWidthC+2,1);
	int mswc = m_labelMetrics.stringWidth(maxStringC);
	int tmsc = mswc;
	if (w > (2 * tmsc)) {
	  gx.setColor(Color.black);
	  gx.drawLine(m_XaxisStart,
		      (m_YaxisEnd+hf+m_tickSize+m_axisPad+m_spectrumHeight+5),
		      m_XaxisEnd,
		      (m_YaxisEnd+hf+m_tickSize+m_axisPad+m_spectrumHeight+5));

	  gx.drawLine(m_XaxisEnd,
		      (m_YaxisEnd+hf+m_tickSize+m_axisPad+m_spectrumHeight+5),
		      m_XaxisEnd,
		      (m_YaxisEnd+hf+m_tickSize+m_axisPad+m_spectrumHeight+5+m_tickSize));

	  gx.drawString(maxStringC, 
			m_XaxisEnd-(mswc/2),
			(m_YaxisEnd+hf+m_tickSize+
			m_axisPad+m_spectrumHeight+5+m_tickSize+hf));

	  gx.drawLine(m_XaxisStart,
		      (m_YaxisEnd+hf+m_tickSize+m_axisPad+m_spectrumHeight+5),
		      m_XaxisStart,
		      (m_YaxisEnd+hf+m_tickSize+m_axisPad+m_spectrumHeight+5+m_tickSize));
	  fieldWidthC = (int)((Math.log(m_minC)/Math.log(10)))+1;
	  maxStringC = Utils.doubleToString(m_minC,fieldWidthC+2,1);
	  mswc = m_labelMetrics.stringWidth(maxStringC);
	  gx.drawString(maxStringC, 
			m_XaxisStart-(mswc/2),
			(m_YaxisEnd+hf+m_tickSize+
			m_axisPad+m_spectrumHeight+5+m_tickSize+hf));

	  // draw the middle value if there is space
	  if (w > (3 * tmsc)) {
	    double mid = m_minC+((m_maxC-m_minC)/2.0);
	    gx.drawLine(m_XaxisStart+((m_XaxisEnd-m_XaxisStart)/2),
			(m_YaxisEnd+hf+m_tickSize+m_axisPad+m_spectrumHeight+5),
			m_XaxisStart+((m_XaxisEnd-m_XaxisStart)/2),
			(m_YaxisEnd+hf+m_tickSize+m_axisPad+m_spectrumHeight+5+m_tickSize));
	    fieldWidthC = (int)((Math.log(mid)/Math.log(10)))+1;
	    maxStringC = Utils.doubleToString(mid,fieldWidthC+2,1);
	    mswc = m_labelMetrics.stringWidth(maxStringC);
	    gx.drawString(maxStringC,
			  m_XaxisStart+((m_XaxisEnd-m_XaxisStart)/2)-(mswc/2),
			  (m_YaxisEnd+hf+m_tickSize+
			   m_axisPad+m_spectrumHeight+5+m_tickSize+hf));
	  }
	}
      }

      // draw axis
      gx.setColor(Color.black);
      if (m_plotInstances.attribute(m_xIndex).isNumeric()) {
	if (w > (2 * mswx)) {
	  gx.drawString(maxStringX, 
			m_XaxisEnd-(mswx/2),
			m_YaxisEnd+hf+m_tickSize);

	  fieldWidthX = (int)((Math.log(m_minX)/Math.log(10)))+1;
	  maxStringX = Utils.doubleToString(m_minX,fieldWidthX+2,1);
	  mswx = m_labelMetrics.stringWidth(maxStringX);
	  gx.drawString(maxStringX,
			(m_XaxisStart-(mswx/2)),
			m_YaxisEnd+hf+m_tickSize);

	  // draw the middle value
	  if (w > (3 * mswx)) {
	    double mid = m_minX+((m_maxX-m_minX)/2.0);
	    int fieldWidth = (int)((Math.log(mid)/Math.log(10)))+1;
	    String maxString = Utils.doubleToString(mid,fieldWidth+2,1);
	    int sw = m_labelMetrics.stringWidth(maxString);
	    double mx = m_XaxisStart+((double)(m_XaxisEnd-m_XaxisStart)/2.0);
	    gx.drawString(Utils.doubleToString(mid,
					       fieldWidth+2,1),
			  (int)(mx-(((double)sw)/2.0)),
			  m_YaxisEnd+hf+m_tickSize);
	    gx.drawLine((int)mx,m_YaxisEnd,(int)mx,m_YaxisEnd+m_tickSize);
	  }
	}
      }

      if (m_plotInstances.attribute(m_yIndex).isNumeric()) {
	if (h > (2 * hf)) {
	  gx.drawString(maxStringY, 
			m_XaxisStart-mswy-m_tickSize,
			m_YaxisStart+(hf));

	  gx.drawString(Utils.doubleToString(m_minY,fieldWidthY+2,1),
			(m_XaxisStart-mswy-m_tickSize),
			m_YaxisEnd);

	  // draw the middle value
	  if (w > (3 * hf)) {
	    double mid = m_minY+((m_maxY-m_minY)/2.0);
	    int fieldWidth = (int)((Math.log(mid)/Math.log(10)))+1;
	    String maxString = Utils.doubleToString(mid,fieldWidth+2,1);
	    int sw = m_labelMetrics.stringWidth(maxString);
	    double mx = m_YaxisStart+((double)(m_YaxisEnd-m_YaxisStart)/2.0);
	    gx.drawString(Utils.doubleToString(mid,
					       fieldWidth+2,1),
			  m_XaxisStart-sw-m_tickSize-1,
			  (int)(mx+(((double)hf)/2.0)));
	    gx.drawLine(m_XaxisStart-m_tickSize,(int)mx,m_XaxisStart,(int)mx);
	  }
	}
      }

      gx.drawLine(m_XaxisStart,
		  m_YaxisStart,
		  m_XaxisStart,
		  m_YaxisEnd);
      gx.drawLine(m_XaxisStart,
		  m_YaxisEnd,
		  m_XaxisEnd,
		  m_YaxisEnd);
    }

    /**
     * Renders this component
     * @param gx the graphics context
     */
    public void paintComponent(Graphics gx) {
      super.paintComponent(gx);
      if (m_plotInstances != null) {
	paintAxis(gx);
	paintData(gx);
      }
    }
  }

  /** Lets the user select the attribute for the x axis */
  protected JComboBox m_XCombo = new JComboBox();

  /** Lets the user select the attribute for the y axis */
  protected JComboBox m_YCombo = new JComboBox();

  /** Lets the user select the attribute to use for colouring */
  protected JComboBox m_ColourCombo = new JComboBox();

  /** The panel that displays the plot */
  protected PlotPanel m_plot = new PlotPanel();
  
  /**
   * Constructor
   */
  public VisualizePanel() {
    m_XCombo.setToolTipText("Select the attribute for the x axis");
    m_YCombo.setToolTipText("Select the attribute for the y axis");
    m_ColourCombo.setToolTipText("Select the attribute to colour on");
    m_XCombo.setEnabled(false);
    m_YCombo.setEnabled(false);
    m_ColourCombo.setEnabled(false);

    m_XCombo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  int selected = m_XCombo.getSelectedIndex();
	  m_plot.setXindex(selected);
	}
      });

    m_YCombo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  int selected = m_YCombo.getSelectedIndex();
	  m_plot.setYindex(selected);
	}
      });

    m_ColourCombo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  int selected = m_ColourCombo.getSelectedIndex();
	  m_plot.setCindex(selected);
	}
      });
    
    JPanel combos = new JPanel();
    combos.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    combos.setLayout(new GridLayout(1,3,5,5));
    combos.add(m_XCombo);
    combos.add(m_YCombo);
    combos.add(m_ColourCombo);
    

    setLayout(new BorderLayout());
    add(combos, BorderLayout.NORTH);
    add(m_plot, BorderLayout.CENTER);
  }

  /**
   * Sets the index used for colouring. If this method is called then
   * the supplied index is used and the combo box for selecting colouring
   * attribute is disabled. If a negative index is supplied then the
   * colouring combo box is disabled and only the classifier/clusterer's 
   * predictions are used for colouring
   * @param index the index of the attribute to use for colouring
   */
  public void setColourIndex(int index) {
    if (index >= 0) {
      m_ColourCombo.setSelectedIndex(index);
    } else {
      m_ColourCombo.setSelectedIndex(0);
      m_plot.m_colourUsingPreds = true;
    }
    m_ColourCombo.setEnabled(false);
  }

  /**
   * Set the classifier's predictions. These must correspond one to one with
   * the instances provided using the setInstances method.
   * @param preds an array of predictions
   */
  public void setPredictions(double [] preds) {
    m_plot.setPredictions(preds);
  }

  /**
   * Specify whether the classifier's predictions are for a numeric class
   * @param n true if the predictions are for a numeric class
   */
  public void setPredictionsNumeric(boolean n) {
    m_plot.setPredictionsNumeric(n);
  }

  /**
   * Tells the panel to use a new set of instances.
   * @param inst a set of Instances
   */
  public void setInstances(Instances inst) {
    
    m_plot.setInstances(inst);
    String [] XNames = new String [inst.numAttributes()];
    String [] YNames = new String [inst.numAttributes()];
    String [] CNames = new String [inst.numAttributes()];
    for (int i = 0; i < XNames.length; i++) {
      String type = "";
      switch (inst.attribute(i).type()) {
      case Attribute.NOMINAL:
	type = " (Nom)";
	break;
      case Attribute.NUMERIC:
	type = " (Num)";
	break;
      case Attribute.STRING:
	type = " (Str)";
	break;
      default:
	type = " (???)";
      }
      XNames[i] = "X: "+ inst.attribute(i).name()+type;
      YNames[i] = "Y: "+ inst.attribute(i).name()+type;
      CNames[i] = "Colour: "+ inst.attribute(i).name()+type;
    }
    m_XCombo.setModel(new DefaultComboBoxModel(XNames));
    m_YCombo.setModel(new DefaultComboBoxModel(YNames));
    m_ColourCombo.setModel(new DefaultComboBoxModel(CNames));
    m_XCombo.setEnabled(true);
    m_YCombo.setEnabled(true);
    m_ColourCombo.setEnabled(true);
  }

  /**
   * Main method for testing this class
   */
  public static void main(String [] args) {
    try {
      final javax.swing.JFrame jf = 
	new javax.swing.JFrame("Weka Knowledge Explorer: Visualize");
      jf.setSize(500,400);
      jf.getContentPane().setLayout(new BorderLayout());
      final VisualizePanel sp = new VisualizePanel();
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      //      jf.pack();
      jf.setVisible(true);
      if (args.length >= 1) {
	System.err.println("Loading instances from " + args[0]);
	java.io.Reader r = new java.io.BufferedReader(
			   new java.io.FileReader(args[0]));
	Instances i = new Instances(r);
	i.setClassIndex(i.numAttributes()-1);
	if (args.length > 1) {
	  weka.classifiers.Classifier cl;
	  if (args.length == 2) {
	    cl = weka.classifiers.Classifier.forName(args[1],null);
	    cl.buildClassifier(i);
	    double [] preds = new double [i.numInstances()];
	    for (int jj=0;jj<i.numInstances();jj++) {
	      preds[jj] = cl.classifyInstance(i.instance(jj));
	    }
	    sp.setPredictions(preds);
	    if (i.attribute(i.classIndex()).isNumeric()) {
	      sp.setPredictionsNumeric(true);
	    } else {
	      sp.setPredictionsNumeric(false);
	    }
	  }
	}
	sp.setInstances(i);	
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
