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

import java.util.Random;

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Dimension;

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
import javax.swing.JSlider;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

/** 
 * This panel allows the user to visualize a dataset (and if provided) a
 * classifier's/clusterer's predictions in two dimensions.
 *
 * If the user selects a nominal attribute as the colouring attribute then
 * each point is drawn in a colour that corresponds to the discrete value
 * of that attribute for the instance. If the user selects a numeric
 * attribute to colour on, then the points are coloured using a spectrum
 * ranging from blue to red (low values to high).
 *
 * When a classifier's predictions are supplied they are plotted in one
 * of two ways (depending on whether the class is nominal or numeric).<br>
 * For nominal class: an error made by a classifier is plotted as a square
 * in the colour corresponding to the class it predicted.<br>
 * For numeric class: predictions are plotted as varying sized x's, where
 * the size of the x is related to the magnitude of the error.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
public class VisualizePanel extends JPanel {

  /** Inner class to handle displaying the legend for the colouring attrib */
  protected class ClassPanel extends JPanel {
    
    /** True when the panel has been enabled (ie after the plot panel
	has called either setNumeric or setNominal */
    private boolean m_isEnabled = false;

    /** True if the colouring attribute is numeric */
    private boolean m_isNumeric = false;
    
    /** The height of the spectrum for numeric class */
    private final int m_spectrumHeight = 5;

    /**  The maximum value for the colouring attribute */
    private double m_maxC;
    
    /** The minimum value for the colouring attribute */
    private double m_minC;

    /** The size of the ticks */
    private final int m_tickSize = 5;

    /** Font metrics */
    private FontMetrics m_labelMetrics = null;

    /** The font used in labeling */
    private Font m_labelFont = null;

    /** The amount of space to leave either side of the legend */ 
    private int m_HorizontalPad=10;

    /** The precision with which to display real values */
    private int m_precisionC;

    /** Field width for numeric values */
    private int m_fieldWidthC;
    
    /** Instances being plotted */
    private Instances m_Instances=null;

    /** Index of the colouring attribute */
    private int m_cIndex;

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

    /**
     * Set up fonts and font metrics
     * @param gx the graphics context
     */
    private void setFonts(Graphics gx) {
      if (m_labelMetrics == null) {
	m_labelFont = new Font("Sanserif", Font.PLAIN, 12);
	m_labelMetrics = gx.getFontMetrics(m_labelFont);
	int hf = m_labelMetrics.getAscent();
	if (getHeight() < (3*hf)) {
	  m_labelFont = new Font("Sanserif",Font.PLAIN,10);
	  m_labelMetrics = gx.getFontMetrics(m_labelFont);
	}
      }
      gx.setFont(m_labelFont);
    }

    /**
     * Enables the panel
     * @param e true to enable the panel
     */
    protected void setOn(boolean e) {
      m_isEnabled = e;
    }

    /**
     * Sets the legend to be for a nominal variable
     * @param plotInstances the instances currently being plotted
     * @param cIndex the index of the colouring attribute
     */
    protected void setNominal(Instances plotInstances, int cIndex) {
      m_isNumeric = false;
      m_Instances = plotInstances;
      m_cIndex = cIndex;
      m_HorizontalPad = 10;
      setOn(true);
      repaint();
    }

    /**
     * Sets the legend to be for a numeric variable
     * @param mxC the maximum value of the colouring attribute
     * @param mnC the minimum value of the colouring attribute
     */
    protected void setNumeric(double mxC, 
			      double mnC) {
      m_isNumeric = true;
      m_maxC = mxC;
      m_minC = mnC;

      m_fieldWidthC = (int)((Math.log(m_maxC)/Math.log(10)))+1;
      m_precisionC = 1;
      if ((Math.abs(m_maxC-m_minC) < 1) && ((m_maxC-m_minC) != 0)) {
	m_precisionC = (int)Math.abs(((Math.log(Math.abs(m_maxC-m_minC)) / 
				     Math.log(10))))+1;
      }
      String maxStringC = Utils.doubleToString(m_maxC,
					       m_fieldWidthC+1+m_precisionC
					       ,m_precisionC);
      if (m_labelMetrics != null) {
	m_HorizontalPad = m_labelMetrics.stringWidth(maxStringC);
      }

      setOn(true);
      repaint();
    }

    /**
     * Renders the legend for a nominal colouring attribute
     * @param gx the graphics context
     */
    protected void paintNominal(Graphics gx) {
      setFonts(gx);
      int numClasses = m_Instances.attribute(m_cIndex).numValues();
      int maxLabelLen = 0;
      int idx=0;
      int legendHeight;
      int w = getWidth();
      int hf = m_labelMetrics.getAscent();

      for (int i=0;i<numClasses;i++) {
	if (m_Instances.attribute(m_cIndex).value(i).length() > 
	    maxLabelLen) {
	  maxLabelLen = m_Instances.
	    attribute(m_cIndex).value(i).length();
	  idx = i;
	}
      }
      maxLabelLen = m_labelMetrics.stringWidth(m_Instances.
					       attribute(m_cIndex).value(idx));


      if (((w-(2*m_HorizontalPad))/(maxLabelLen+5)) >= numClasses) {
	legendHeight = 1;
      } else {
	legendHeight = 2;
      }
	
      int x = m_HorizontalPad;
      int y = 1 + hf;

      // do the first row
      int ci, mp;
      Color pc;
      int numToDo = ((legendHeight==1) ? numClasses : (numClasses/2));
      for (int i=0;i<numToDo;i++) {
	ci = i % 10; mp = i / 10; mp *= 2;
	pc = m_DefaultColors[ci];
	for (int j=0;j<mp;j++) {
	  pc = pc.darker();
	}
	gx.setColor(pc);
	// can we fit the full label or will each need to be trimmed?
	if ((numToDo * maxLabelLen) > (w-(m_HorizontalPad*2))) {
	  String val = m_Instances.attribute(m_cIndex).value(i);
	  int sw = m_labelMetrics.stringWidth(val);
	  int rm=0;
	  // truncate string if necessary
	  if (sw > ((w-(m_HorizontalPad*2)) / (numToDo))) {
	    int incr = (sw / val.length());
	    rm = (sw -  ((w-(m_HorizontalPad*2)) / numToDo)) / incr;
	    if (rm <= 0) {
	      rm = 0;
	    }
	    val = val.substring(0,val.length()-rm);
	    sw = m_labelMetrics.stringWidth(val);
	  }
	  gx.drawString(val,x,y);
	  x += sw + 2;
	} else {
	  gx.drawString(m_Instances.attribute(m_cIndex).value(i),x,y);
	  x += ((w-(m_HorizontalPad*2)) / numToDo);
	}	  
      }

      x = m_HorizontalPad;
      y = 1+ hf + 5 +hf;
      for (int i=numToDo;i<numClasses;i++) {
	ci = i % 10; mp = i / 10; mp *= 2;
	pc = m_DefaultColors[ci];
	for (int j=0;j<mp;j++) {
	  pc = pc.darker();
	}
	gx.setColor(pc);
	if (((numClasses-numToDo+1) * maxLabelLen) > 
	    (w - (m_HorizontalPad*2))) {
	  String val = m_Instances.attribute(m_cIndex).value(i);
	  int sw = m_labelMetrics.stringWidth(val);
	  int rm=0;
	  // truncate string if necessary
	  if (sw > ((w-(m_HorizontalPad*2)) / (numClasses-numToDo+1))) {
	    int incr = (sw / val.length());
	    rm = (sw -  ((w-(m_HorizontalPad*2)) / (numClasses-numToDo))) 
	      / incr;
	    if (rm <= 0) {
	      rm = 0;
	    }
	    val = val.substring(0,val.length()-rm);
	    sw = m_labelMetrics.stringWidth(val);
	  }
	  gx.drawString(val,x,y);
	  //	  x += ((w-(m_HorizontalPad*2)) / (numClasses-numToDo))+3;
	  x += sw +2;
	} else {
	  gx.drawString(m_Instances.attribute(m_cIndex).value(i),x,y);
	  x += ((w - (m_HorizontalPad*2)) / (numClasses-numToDo));
	}	  
      }
    }

    /**
     * Renders the legend for a numeric colouring attribute
     * @param gx the graphics context
     */
    protected void paintNumeric(Graphics gx) {
      setFonts(gx);
      int w = getWidth();
      double rs = 15;
      double incr = 240.0 / (double)(w-(m_HorizontalPad*2));
      int hf = m_labelMetrics.getAscent();
      
      for (int i=m_HorizontalPad;i<
	     (w-m_HorizontalPad);i++) {
	Color c = new Color((int)rs,0,(int)(255-rs));
	gx.setColor(c);
	gx.drawLine(i,0,
		    i,0+m_spectrumHeight);
	rs += incr;
	}

      	int fieldWidthC = (int)((Math.log(m_maxC)/Math.log(10)))+1;
	
	String maxStringC = Utils.doubleToString(m_maxC,
						 fieldWidthC+1+m_precisionC,
						 m_precisionC);
	
	int mswc = m_labelMetrics.stringWidth(maxStringC);
	int tmsc = mswc;
	if (w > (2 * tmsc)) {
	  gx.setColor(Color.black);
	  gx.drawLine(m_HorizontalPad,
		      (m_spectrumHeight+5),
		      w-m_HorizontalPad,
		      (m_spectrumHeight+5));

	  gx.drawLine(w-m_HorizontalPad,
		      (m_spectrumHeight+5),
		      w-m_HorizontalPad,
		      (m_spectrumHeight+5+m_tickSize));

	  gx.drawString(maxStringC, 
			(w-m_HorizontalPad)-(mswc/2),
			(m_spectrumHeight+5+m_tickSize+hf));

	  gx.drawLine(m_HorizontalPad,
		      (m_spectrumHeight+5),
		      m_HorizontalPad,
		      (m_spectrumHeight+5+m_tickSize));

	  fieldWidthC = (int)((Math.log(m_minC)/Math.log(10)))+1;
	  maxStringC = Utils.doubleToString(m_minC,
					    fieldWidthC+1+m_precisionC,
					    m_precisionC);
	  mswc = m_labelMetrics.stringWidth(maxStringC);
	  gx.drawString(maxStringC, 
			m_HorizontalPad-(mswc/2),
			(m_spectrumHeight+5+m_tickSize+hf));

	  // draw the middle value if there is space
	  if (w > (3 * tmsc)) {
	    double mid = m_minC+((m_maxC-m_minC)/2.0);
	    gx.drawLine(m_HorizontalPad+((w-(2*m_HorizontalPad))/2),
			(m_spectrumHeight+5),
			m_HorizontalPad+((w-(2*m_HorizontalPad))/2),
			(m_spectrumHeight+5+m_tickSize));
	    fieldWidthC = (int)((Math.log(mid)/Math.log(10)))+1;
	    maxStringC = Utils.doubleToString(mid,
					      fieldWidthC+1+m_precisionC,
					      m_precisionC);
	    mswc = m_labelMetrics.stringWidth(maxStringC);
	    gx.drawString(maxStringC,
			  m_HorizontalPad+((w-(2*m_HorizontalPad))/2)-(mswc/2),
			  (m_spectrumHeight+5+m_tickSize+hf));
	  }
	}
    }

    /**
     * Renders this component
     * @param gx the graphics context
     */
    public void paintComponent(Graphics gx) {
      super.paintComponent(gx);
      if (m_isEnabled) {
	if (m_isNumeric) {
	  paintNumeric(gx);
	} else {
	  if (m_Instances != null) {
	    paintNominal(gx);
	  }
	}
      }
    }
  }

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

    /**the offsets of the axes once label metrics are calculated */
    private int m_XaxisStart=0;
    private int m_YaxisStart=0;
    private int m_XaxisEnd=0;
    private int m_YaxisEnd=0;

    /** if the user resizes the window, or the attributes selected for
	the attributes change, then the lookup table for points needs
	to be recalculated */
    private boolean m_axisChanged = true;

    /** Font for labels */
    private Font m_labelFont;
    private FontMetrics m_labelMetrics=null; 

    /** the level of jitter */
    private int m_JitterVal=0;

    /** random values for perterbing the data points */
    private Random m_JRand = new Random(0);

    /** lookup table for plotted points */
    private double [][] m_pointLookup=null;

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
      addMouseListener(new MouseAdapter() {
	  public void mouseClicked(MouseEvent e) {
	    searchPoints(e.getX(),e.getY());
	  }
	});
    }

    /**
     * Set level of jitter and repaint the plot using the new jitter value
     * @param j the level of jitter
     */
    public void setJitter(int j) {
      if (j >= 0) {
	m_JitterVal = j;
	m_JRand = new Random(m_JitterVal);
	if (m_pointLookup != null) {
	  updatePturb();
	}
	repaint();
      }
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
     * @param cIndex the index of the colouring attribute
     */
    public void setInstances(Instances inst, int cIndex) {
      m_plotInstances=inst;
      m_xIndex=0;
      m_yIndex=0;
      m_cIndex=cIndex;
      m_pointLookup = new double [m_plotInstances.numInstances()][5];
      determineBounds();
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
      gx.setFont(m_labelFont);
    }

    /**
     * When the user clicks in the plot window this function pops
     * up a window displaying attribute information on any instances
     * corresponding to the points under the clicked point
     * @param x the x value of the clicked point
     * @param y the y value of the clicked point
     */
    private void searchPoints(int x, int y) {
      if (m_pointLookup != null) {
	int longest=0;
	for (int j=0;j<m_plotInstances.numAttributes();j++) {
	  if (m_plotInstances.attribute(j).name().length() > longest) {
	    longest = m_plotInstances.attribute(j).name().length();
	  }
	}
	if (m_preds != null) {
	  longest = (longest < 9) ? 9 : longest;
	}
	StringBuffer insts = new StringBuffer(); 
	for (int i=0;i<m_plotInstances.numInstances();i++) {
	  if (m_pointLookup[i][0] != Double.NEGATIVE_INFINITY) {
	    double px = m_pointLookup[i][0]+m_pointLookup[i][3];
	    double py = m_pointLookup[i][1]+m_pointLookup[i][4];
	    double size = m_pointLookup[i][2];
	    if ((x >= px-size) && (x <= px+size) &&
		(y >= py-size) && (y <= py+size)) {
	      {
		insts.append("\nInstance: "+i+"\n");
		for (int j=0;j<m_plotInstances.numAttributes();j++) {
		  for (int k = 0;k < 
			 (longest-m_plotInstances.
			  attribute(j).name().length()); k++) {
		    insts.append(" ");
		  }
		  insts.append(m_plotInstances.attribute(j).name());  
		  insts.append(" : ");
		  
		  if (m_plotInstances.instance(i).isMissing(j)) {
		    insts.append("Missing");
		  } else if (m_plotInstances.attribute(j).isNominal()) {
		    insts.append(m_plotInstances.
				 attribute(j).
				 value((int)m_plotInstances.
				       instance(i).value(j)));
		  } else {
		    insts.append(m_plotInstances.instance(i).value(j));
		  }
		  insts.append("\n");
		}
		if (m_preds != null && 
		    (!m_plotInstances.instance(i).
		     isMissing(m_plotInstances.classIndex()))) {
		  if (longest > 9) {
		    for (int k = 0;k < (longest-9); k++) {
		      insts.append(" ");
		    }
		  }
		    
		  insts.append("predicted : ");
		  if (m_plotInstances.attribute(m_cIndex).isNominal()) {
		    insts.append(m_plotInstances.
				 attribute(m_plotInstances.classIndex()).
				 value((int)m_preds[i]));
		  } else {
		    insts.append(m_preds[i]);
		  }
		  insts.append("\n");
		}
	      }
	    }
	  }
	}
	if (insts.length() > 0) {
	  JTextArea jt = new JTextArea();
	  jt.setFont(new Font("Dialoginput", Font.PLAIN,10));
	  jt.setEditable(false);
	  jt.setText(insts.toString());
	  final JFrame jf = new JFrame("Weka: Instance info");
	  jf.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
	      jf.dispose();
	    }
	  });
	  jf.getContentPane().setLayout(new BorderLayout());
	  jf.getContentPane().add(new JScrollPane(jt), BorderLayout.CENTER);
	  jf.pack();
	  jf.setSize(320, 400);
	  jf.setVisible(true);
	}
      }
    }

    /**
     * Determine the min and max values for axis and colouring attributes
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
	if (m_pointLookup != null) {
	  fillLookup();
	  setJitter(m_JitterVal);
	  repaint();
	}
      }
    }

    /**
     * returns a value by which an x value can be peturbed. Makes sure
     * that the x value+pturb stays within the plot bounds
     * @param xvalP the x coordinate to be peturbed
     * @param xj a random number to use in calculating a peturb value
     * @return a peturb value
     */
    int pturbX(double xvalP, double xj) {
      int xpturb = 0;
      if (m_JitterVal > 0) {
	xpturb = (int)((double)m_JitterVal * (xj / 2.0));
	if (((xvalP + xpturb) < m_XaxisStart) || 
	    ((xvalP + xpturb) > m_XaxisEnd)) {
	  xpturb *= -1;
	}
      }
      return xpturb;
    }

    /**
     * Convert an raw x value to Panel x coordinate.
     * @param xval the raw x value
     * @return an x value for plotting in the panel.
     */
    private double convertToPanelX(double xval) {
      double temp = (xval - m_minX)/(m_maxX - m_minX);
      double temp2 = temp * (m_XaxisEnd - m_XaxisStart);
      
      temp2 = temp2 + m_XaxisStart;
	
      return temp2;
    }

    /**
     * returns a value by which a y value can be peturbed. Makes sure
     * that the y value+pturb stays within the plot bounds
     * @param yvalP the y coordinate to be peturbed
     * @param yj a random number to use in calculating a peturb value
     * @return a peturb value
     */
    int pturbY(double yvalP, double yj) {
      int ypturb = 0;
      if (m_JitterVal > 0) {
	ypturb = (int)((double)m_JitterVal * (yj / 2.0));
	if (((yvalP + ypturb) < m_YaxisStart) || 
	    ((yvalP + ypturb) > m_YaxisEnd)) {
	  ypturb *= -1;
	}
      }
      return ypturb;
    }

    /**
     * Convert an raw y value to Panel y coordinate.
     * @param yval the raw y value
     * @return an y value for plotting in the panel.
     */
    private double convertToPanelY(double yval) {
      double temp = (yval - m_minY)/(m_maxY - m_minY);
      double temp2 = temp * (m_YaxisEnd - m_YaxisStart);
      
      temp2 = m_YaxisEnd - temp2;

      return temp2;
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
			       int type,
			       Graphics gx) {
      if (size == 0) {
	size = 1;
      }
      
      switch (type) {
      case 0:
	gx.drawLine((int)(x-size),(int)(y-size),
		    (int)(x+size),(int)(y+size));
	gx.drawLine((int)(x+size),(int)(y-size),
		    (int)(x-size),(int)(y+size));
	break;
      case 1: 
	gx.drawRect((int)(x-size),(int)(y-size),(size*2),(size*2));
	break;
      }
    }

    private void updatePturb() {
      double xj=0;
      double yj=0;
      for (int i=0;i<m_plotInstances.numInstances();i++) {
	if (m_plotInstances.instance(i).isMissing(m_xIndex) ||
	    m_plotInstances.instance(i).isMissing(m_yIndex)) {
	} else {
	  if (m_JitterVal > 0) {
	    xj = m_JRand.nextGaussian();
	    yj = m_JRand.nextGaussian();
	  }
	  m_pointLookup[i][3] = pturbX(m_pointLookup[i][0],xj);
	  m_pointLookup[i][4] = pturbY(m_pointLookup[i][1],yj);
	}
      }
    }

    private void fillLookup() {
      int maxpSize = 20;
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
	  m_pointLookup[i][0] = Double.NEGATIVE_INFINITY;
	  m_pointLookup[i][1] = Double.NEGATIVE_INFINITY;
	  m_pointLookup[i][2] = Double.NEGATIVE_INFINITY;
	  
	} else {
	  double x = convertToPanelX(m_plotInstances.
				     instance(i).value(m_xIndex));
	  double y = convertToPanelY(m_plotInstances.
				     instance(i).value(m_yIndex));
	  m_pointLookup[i][0] = x;
	  m_pointLookup[i][1] = y;

	  if (m_plotInstances.attribute(m_cIndex).isNominal() || 
	      m_preds == null || m_colourUsingPreds) {
	    m_pointLookup[i][2] = 3;
	  } else {
	    double err = Math.abs((m_preds[i] -
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
    }

    /**
     * Draws the data points and predictions (if provided).
     * @param gx the graphics context
     */
    private void paintData(Graphics gx) {
      double maxErr = Double.NEGATIVE_INFINITY;
      double minErr = Double.POSITIVE_INFINITY;
      double xj=0;
      double yj=0;

      /*      // determine range of error for numeric predictions if necessary
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
	} */

      for (int i=0;i<m_plotInstances.numInstances();i++) {
	if (m_plotInstances.instance(i).isMissing(m_xIndex) ||
	    m_plotInstances.instance(i).isMissing(m_yIndex)) {
	} else {
	  if (m_JitterVal > 0) {
	    xj = m_JRand.nextGaussian();
	    yj = m_JRand.nextGaussian();
	  }
	  /*	  double x = convertToPanelX(m_plotInstances.
				     instance(i).value(m_xIndex),xj);
	  double y = convertToPanelY(m_plotInstances.
	  instance(i).value(m_yIndex),yj); */
	  double x = (m_pointLookup[i][0] + m_pointLookup[i][3]);
	  double y = (m_pointLookup[i][1] + m_pointLookup[i][4]);

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
	      pc = pc.darker();
	    }
	    
	    gx.setColor(pc);	    
	    drawDataPoint(x,y,2,0,gx);
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
		  pc = pc.darker();
		}
		
		gx.setColor(pc);
		drawDataPoint(x,y,(int)m_pointLookup[i][2],1,gx);
		//		gx.drawRect((int)(x-3),(int)(y-3),6,6);
		//ci = (int)(m_plotInstances.instance(i).value(m_plotInstances.classIndex()) % 10);
	      }
	    }    
	  } else {
	    int maxpSize = 20;
	    if (m_preds != null 
		&& m_plotInstances.classIndex() >= 0 
		&& m_predsNumeric) {
	      /*	      double err = Math.abs((m_preds[i] -
				     m_plotInstances.instance(i).
				     value(m_plotInstances.classIndex())));
				     err = ((err - minErr)/(maxErr - minErr)) * maxpSize; */
	      double r = (m_plotInstances.instance(i).
			  value(m_cIndex) - m_minC) /
		          (m_maxC - m_minC);
	      r = (r * 240) + 15;
	      gx.setColor(new Color((int)r,0,(int)(255-r)));
	      drawDataPoint(x,y,(int)m_pointLookup[i][2],0,gx);
	    } else {
	      double r = (m_plotInstances.instance(i).value(m_cIndex) - m_minC) /
		(m_maxC - m_minC);
	      r = (r * 240) + 15;
	      gx.setColor(new Color((int)r,0,(int)(255-r)));
	      drawDataPoint(x,y,2,0,gx);
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

      Graphics classPgx = m_classPanel.getGraphics();
      int mxs = m_XaxisStart;
      int mxe = m_XaxisEnd;
      int mys = m_YaxisStart;
      int mye = m_YaxisEnd;
      m_axisChanged = false;

      int h = getHeight();
      int w = getWidth();
      int hf = m_labelMetrics.getAscent();
      int mswx=0;
      int mswy=0;

      //      determineBounds();
      int fieldWidthX = (int)((Math.log(m_maxX)/Math.log(10)))+1;
      int precisionX = 1;
      if ((Math.abs(m_maxX-m_minX) < 1) && ((m_maxY-m_minX) != 0)) {
	precisionX = (int)Math.abs(((Math.log(Math.abs(m_maxX-m_minX)) / 
				     Math.log(10))))+1;
      }
      String maxStringX = Utils.doubleToString(m_maxX,
					       fieldWidthX+1+precisionX
					       ,precisionX);
      mswx = m_labelMetrics.stringWidth(maxStringX);
      int fieldWidthY = (int)((Math.log(m_maxY)/Math.log(10)))+1;
      int precisionY = 1;
      if (Math.abs((m_maxY-m_minY)) < 1 && ((m_maxY-m_minY) != 0)) {
	precisionY = (int)Math.abs(((Math.log(Math.abs(m_maxY-m_minY)) / 
				     Math.log(10))))+1;
      }
      String maxStringY = Utils.doubleToString(m_maxY,
					       fieldWidthY+1+precisionY
					       ,precisionY);
      String minStringY = Utils.doubleToString(m_minY,
					       fieldWidthY+1+precisionY
					       ,precisionY);

      if (m_plotInstances.attribute(m_yIndex).isNumeric()) {
	mswy = (m_labelMetrics.stringWidth(maxStringY) > 
		m_labelMetrics.stringWidth(minStringY))
	  ? m_labelMetrics.stringWidth(maxStringY)
	  : m_labelMetrics.stringWidth(minStringY);
      } else {
	mswy = m_labelMetrics.stringWidth("MM");
      }

      m_YaxisStart = m_axisPad;
      m_XaxisStart = 0+m_axisPad+m_tickSize+mswy;

      m_XaxisEnd = w-m_axisPad-(mswx/2);
      
      m_YaxisEnd = h-m_axisPad-(2 * hf)-m_tickSize;

      // draw the class legend
      if ((!m_colourUsingPreds) && 
	  m_plotInstances.attribute(m_cIndex).isNumeric()) {

	m_classPanel.setNumeric(m_maxC, m_minC);

      } else if ((!m_colourUsingPreds) && 
	  m_plotInstances.attribute(m_cIndex).isNominal()) {

	m_classPanel.setNominal(m_plotInstances, m_cIndex);
      }

      // draw axis
      gx.setColor(Color.black);
      if (m_plotInstances.attribute(m_xIndex).isNumeric()) {
	if (w > (2 * mswx)) {
	  gx.drawString(maxStringX, 
			m_XaxisEnd-(mswx/2),
			m_YaxisEnd+hf+m_tickSize);

	  fieldWidthX = (int)((Math.log(m_minX)/Math.log(10)))+1;
	  maxStringX = Utils.doubleToString(m_minX,
					    fieldWidthX+1+precisionX,
					    precisionX);
	  mswx = m_labelMetrics.stringWidth(maxStringX);
	  gx.drawString(maxStringX,
			(m_XaxisStart-(mswx/2)),
			m_YaxisEnd+hf+m_tickSize);

	  // draw the middle value
	  if (w > (3 * mswx) && 
	      (m_plotInstances.attribute(m_xIndex).isNumeric())) {
	    double mid = m_minX+((m_maxX-m_minX)/2.0);
	    int fieldWidth = (int)((Math.log(mid)/Math.log(10)))+1;
	    String maxString = Utils.doubleToString(mid,
						    fieldWidth+1+precisionX,
						    precisionX);
	    int sw = m_labelMetrics.stringWidth(maxString);
	    double mx = m_XaxisStart+((double)(m_XaxisEnd-m_XaxisStart)/2.0);
	    gx.drawString(maxString,
			  (int)(mx-(((double)sw)/2.0)),
			  m_YaxisEnd+hf+m_tickSize);
	    gx.drawLine((int)mx,m_YaxisEnd,(int)mx,m_YaxisEnd+m_tickSize);
	  }
	}
      } else {
	int numValues = m_plotInstances.attribute(m_xIndex).numValues();
	int div = (numValues % 2 > 0) ? (numValues/2)+1 : (numValues/2);
	int maxXStringWidth = (m_XaxisEnd - m_XaxisStart) / numValues;

	for (int i=0;i<numValues;i++) {
	  String val = m_plotInstances.attribute(m_xIndex).value(i);
	  int sw = m_labelMetrics.stringWidth(val);
	  int rm;
	  // truncate string if necessary
	  if (sw > maxXStringWidth) {
	    int incr = (sw / val.length());
	    rm = (sw - maxXStringWidth) / incr;
	    if (rm == 0) {
	      rm = 1;
	    }
	    val = val.substring(0,val.length()-rm);
	    sw = m_labelMetrics.stringWidth(val);
	  }
	  if (i == 0) {
	    gx.drawString(val,(int)convertToPanelX(i),
			  m_YaxisEnd+hf+m_tickSize);
	  } else if (i == numValues -1) {
	    if ((i % 2) == 0) {
	      gx.drawString(val,
			    m_XaxisEnd-sw,
			    m_YaxisEnd+hf+m_tickSize);
	    } else {
	       gx.drawString(val,
			    m_XaxisEnd-sw,
			    m_YaxisEnd+(2*hf)+m_tickSize);
	    }
	  } else {
	    if ((i % 2) == 0) {
	      gx.drawString(val,
			    (int)convertToPanelX(i)-(sw/2),
			    m_YaxisEnd+hf+m_tickSize);
	    } else {
	      gx.drawString(val,
			    (int)convertToPanelX(i)-(sw/2),
			    m_YaxisEnd+(2*hf)+m_tickSize);
	    }
	  }
	  gx.drawLine((int)convertToPanelX(i),
		      m_YaxisEnd,
		      (int)convertToPanelX(i),
		      m_YaxisEnd+m_tickSize);
	}
	
      }

      // draw the y axis
      if (m_plotInstances.attribute(m_yIndex).isNumeric()) {
	if (h > (2 * hf)) {
	  gx.drawString(maxStringY, 
			m_XaxisStart-mswy-m_tickSize,
			m_YaxisStart+(hf));

	  gx.drawString(Utils.doubleToString(m_minY,
					     fieldWidthY+1+precisionY,
					     precisionY),
			(m_XaxisStart-mswy-m_tickSize),
			m_YaxisEnd);

	  // draw the middle value
	  if (w > (3 * hf) && 
	      (m_plotInstances.attribute(m_yIndex).isNumeric())) {
	    double mid = m_minY+((m_maxY-m_minY)/2.0);
	    int fieldWidth = (int)((Math.log(mid)/Math.log(10)))+1;
	    String maxString = Utils.doubleToString(mid,
						    fieldWidth+1+precisionY,
						    precisionY);
	    int sw = m_labelMetrics.stringWidth(maxString);
	    double mx = m_YaxisStart+((double)(m_YaxisEnd-m_YaxisStart)/2.0);
	    gx.drawString(maxString,
			  m_XaxisStart-sw-m_tickSize-1,
			  (int)(mx+(((double)hf)/2.0)));
	    gx.drawLine(m_XaxisStart-m_tickSize,(int)mx,m_XaxisStart,(int)mx);
	  }
	}
      } else {
	int numValues = m_plotInstances.attribute(m_yIndex).numValues();
	int div = ((numValues % 2) == 0) ? (numValues/2) : (numValues/2+1);
	int maxYStringHeight = (m_YaxisEnd - m_XaxisStart) / div;
	int sw = m_labelMetrics.stringWidth("M");
	for (int i=0;i<numValues;i++) {
	  // can we at least print 2 characters
	  if (maxYStringHeight >= (2*hf)) {
	    String val = m_plotInstances.attribute(m_yIndex).value(i);
	    int numPrint = ((maxYStringHeight/hf) > val.length()) ?
	      val.length() :
	      (maxYStringHeight/hf);
	    
	    for (int j=0;j<numPrint;j++) {
	      String ll = val.substring(j,j+1);
	      if (val.charAt(j) == '_' || val.charAt(j) == '-') {
		ll = "|";
	      }
	      if (i == 0) {
		gx.drawString(ll,m_XaxisStart-sw-m_tickSize-1,
			      (int)convertToPanelY(i)
			      -((numPrint-1)*hf)
			      +(j*hf)
			      +(hf/2));
	      } else if (i == (numValues-1)) {
		if ((i % 2) == 0) {
		  gx.drawString(ll,m_XaxisStart-sw-m_tickSize-1,
				(int)convertToPanelY(i)
				+(j*hf)
				+(hf/2));
		} else {
		  gx.drawString(ll,m_XaxisStart-(2*sw)-m_tickSize-1,
				(int)convertToPanelY(i)
				+(j*hf)
				+(hf/2));
		}
	      } else {
		if ((i % 2) == 0) {
		  gx.drawString(ll,m_XaxisStart-sw-m_tickSize-1,
				(int)convertToPanelY(i)
				-(((numPrint-1)*hf)/2)
				+(j*hf)
				+(hf/2));
		} else {
		  gx.drawString(ll,m_XaxisStart-(2*sw)-m_tickSize-1,
				(int)convertToPanelY(i)
				-(((numPrint-1)*hf)/2)
				  +(j*hf)
				+(hf/2));
		}
	      }
	    }
	  }
	  gx.drawLine(m_XaxisStart-m_tickSize,
		      (int)convertToPanelY(i),
		      m_XaxisStart,
		      (int)convertToPanelY(i));
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

      if (m_XaxisStart != mxs || m_XaxisEnd != mxe ||
	  m_YaxisStart != mys || m_YaxisEnd != mye) {
	m_axisChanged = true;
      }
    }

    /**
     * Renders this component
     * @param gx the graphics context
     */
    public void paintComponent(Graphics gx) {
      super.paintComponent(gx);
      if (m_plotInstances != null) {
	m_JRand = new Random(m_JitterVal);
	paintAxis(gx);
	if (m_axisChanged) {
	  fillLookup();
	}
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

  /** Label for the jitter slider */
  protected JLabel m_JitterLab= new JLabel("Jitter",SwingConstants.RIGHT);

  /** The jitter slider */
  protected JSlider m_Jitter = new JSlider(0,50,0);

  /** The panel that displays the plot */
  protected PlotPanel m_plot = new PlotPanel();

  /** Panel that surrounds the plot panel with a titled border */
  protected JPanel m_plotSurround = new JPanel();

  /** An optional listener that we will inform when ComboBox selections
      change */
  protected ActionListener listener = null;

  /** The name of the plot (not currently displayed, but can be used
      in the containing Frame or Panel) */
  protected String m_plotName = "";

  /** The panel that displays the legend for the colouring attribute */
  protected ClassPanel m_classPanel = new ClassPanel();
  
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
	  if (selected < 0) {
	    selected = 0;
	  }
	  m_plot.setXindex(selected);
	  m_plot.determineBounds();
	  // try sending on the event if anyone is listening
	  if (listener != null) {
	    listener.actionPerformed(e);
	  }
	}
      });

    m_YCombo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  int selected = m_YCombo.getSelectedIndex();
	  if (selected < 0) {
	    selected = 0;
	  }
	  m_plot.setYindex(selected);
	  m_plot.determineBounds();
	  // try sending on the event if anyone is listening
	  if (listener != null) {
	    listener.actionPerformed(e);
	  }
	}
      });

    m_ColourCombo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  int selected = m_ColourCombo.getSelectedIndex();
	  if (selected < 0) {
	    selected = 0;
	  }
	  m_plot.setCindex(selected);
	  m_plot.determineBounds();
	  // try sending on the event if anyone is listening
	  if (listener != null) {
	    listener.actionPerformed(e);
	  }
	}
      });

    m_Jitter.addChangeListener(new ChangeListener() {
	public void stateChanged(ChangeEvent e) {
	  m_plot.setJitter(m_Jitter.getValue());
	}
      });
    
    JPanel combos = new JPanel();
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints constraints = new GridBagConstraints();
    combos.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    //    combos.setLayout(new GridLayout(1,3,5,5));
    combos.setLayout(gb);
    constraints.gridx=0;constraints.gridy=0;constraints.weightx=5;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridwidth=1;constraints.gridheight=1;
    constraints.insets = new Insets(0,2,0,2);
    combos.add(m_XCombo,constraints);
    constraints.gridx=1;constraints.gridy=0;constraints.weightx=5;
    constraints.gridwidth=1;constraints.gridheight=1;
    combos.add(m_YCombo,constraints);
    constraints.gridx=2;constraints.gridy=0;constraints.weightx=5;
    constraints.gridwidth=1;constraints.gridheight=1;
    combos.add(m_ColourCombo,constraints);
    constraints.gridx=0;constraints.gridy=1;constraints.weightx=5;
    constraints.insets = new Insets(10,0,0,5);
    combos.add(m_JitterLab,constraints);
    constraints.gridx=1;constraints.gridy=1;
    constraints.gridwidth=2;constraints.weightx=5;
    constraints.insets = new Insets(10,0,0,0);
    combos.add(m_Jitter,constraints);

    JPanel cp = new JPanel();
    cp.setBorder(BorderFactory.createTitledBorder("Class colour")); 
    cp.setLayout(new BorderLayout());

    m_classPanel.setBorder(BorderFactory.createEmptyBorder(15,10,10,10));
    cp.add(m_classPanel, BorderLayout.CENTER);

    m_plotSurround.setBorder(BorderFactory.createTitledBorder("Plot"));
    m_plotSurround.setLayout(new BorderLayout());
    m_plotSurround.add(m_plot, BorderLayout.CENTER);
    setLayout(new BorderLayout());
    add(combos, BorderLayout.NORTH);
    add(m_plotSurround, BorderLayout.CENTER);
    add(cp, BorderLayout.SOUTH);
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
      // m_ColourCombo.setSelectedIndex(0);
      m_plot.m_colourUsingPreds = true;
    }
    m_ColourCombo.setEnabled(false);
  }

  /**
   * Set the index of the attribute for the x axis 
   * @param index the index for the x axis
   */
  public void setXIndex(int index) throws Exception {
    if (index >= 0 && index < m_XCombo.getItemCount()) {
      m_XCombo.setSelectedIndex(index);
    } else {
      throw new Exception("x index is out of range!");
    }
  }

  /**
   * Get the index of the attribute on the x axis
   * @return the index of the attribute on the x axis
   */
  public int getXIndex() {
    return m_XCombo.getSelectedIndex();
  }

  /**
   * Set the index of the attribute for the y axis 
   * @param index the index for the y axis
   */
  public void setYIndex(int index) throws Exception {
    if (index >= 0 && index < m_YCombo.getItemCount()) {
      m_YCombo.setSelectedIndex(index);
    } else {
      throw new Exception("y index is out of range!");
    }
  }
  
  /**
   * Get the index of the attribute on the y axis
   * @return the index of the attribute on the x axis
   */
  public int getYIndex() {
    return m_YCombo.getSelectedIndex();
  }

  /**
   * Get the index of the attribute selected for coloring
   * @return the index of the attribute on the x axis
   */
  public int getCIndex() {
    return m_ColourCombo.getSelectedIndex();
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
   * Add a listener for this visualize panel
   * @param act an ActionListener
   */
  public void addActionListener(ActionListener act) {
    listener = act;
  }

  /**
   * Set a name for this plot
   * @param plotName the name for the plot
   */
  public void setName(String plotName) {
    m_plotName = plotName;
  }

  /**
   * Returns the name associated with this plot. "" is returned if no
   * name is set.
   * @return the name of the plot
   */
  public String getName() {
    return m_plotName;
  }

  /**
   * Get the instances being plotted
   * @return the instances being plotted
   */
  public Instances getInstances() {
    return m_plot.m_plotInstances;
  }

  /**
   * Tells the panel to use a new set of instances.
   * @param inst a set of Instances
   */
  public void setInstances(Instances inst) {
    
    m_plot.setInstances(inst, inst.numAttributes()-1);
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
    if (!m_plot.m_colourUsingPreds) {
      m_ColourCombo.setEnabled(true);
      m_ColourCombo.setSelectedIndex(inst.numAttributes()-1);
    }
    m_plotSurround.setBorder((BorderFactory.createTitledBorder("Plot: "
			      +inst.relationName())));
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
      sp.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  System.err.println("Recieved a combo box change event");
	}
      });
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
