/*
 *    VisualizePanel.java
 *    Copyright (C) 1999 Mark Hall, Malcolm Ware
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
import weka.core.Instance;
import weka.core.Attribute;
import weka.core.FastVector;
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
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JColorChooser;
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
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 1.11 $
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

    /** True if using preds for colouring. */
    protected boolean m_colourUsingPreds = false;
    
    /** The number of different pred types. */
    protected int m_numPreds;
       

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

    /** The old width. */
    private int m_oldWidth = -9000;
    
    /** Instances being plotted */
    private Instances m_Instances=null;

    /** Index of the colouring attribute */
    private int m_cIndex;

    /** Inner Inner class used to create labels for nominal attributes
     * so that there color can be changed.
     */
    private class NomLabel extends JLabel {
      private int m_index = 0;

      /** 
       * Creates a label with its name and class index value.
       * @param name The name of the nominal class value.
       * @param id The index value for that class value.
       */
      public NomLabel(String name, int id) {
	super(name);
	m_index = id;

	addMouseListener(new MouseAdapter() {
	    public void mouseClicked(MouseEvent e) {
	      
	      if ((e.getModifiers() & e.BUTTON1_MASK) == e.BUTTON1_MASK) {
		Color tmp = JColorChooser.showDialog
		  (VisualizePanel.this, "Select new Color", 
		   (Color)m_colorList.elementAt(m_index));
		
		if (tmp != null) {
		  m_colorList.setElementAt(tmp, m_index);
		  m_attrib.repaint();
		  m_plot.repaint();
		  m_oldWidth = -9000;
		  m_classPanel.repaint();
		}
	      }
	    }
	  });
      }
    }
    
    /** default colours for colouring discrete class */
    /*    protected Color [] m_DefaultColors = {Color.blue,
					  Color.red,
					  Color.green,
					  Color.cyan,
					  Color.pink,
					  new Color(255, 0, 255),
                                          Color.orange,
                                          new Color(255, 0, 0),
                                          new Color(0, 255, 0),
                                          Color.white};*/

    
    /**
     * Set up fonts and font metrics
     * @param gx the graphics context
     */
    private void setFonts(Graphics gx) {
      if (m_labelMetrics == null) {
	m_labelFont = new Font("Monospaced", Font.PLAIN, 12);
	m_labelMetrics = gx.getFontMetrics(m_labelFont);
	int hf = m_labelMetrics.getAscent();
	if (getHeight() < (3*hf)) {
	  m_labelFont = new Font("Monospaced",Font.PLAIN,11);
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
      m_oldWidth = -9000;
      //repaint();
      /*System.out.println("why");
      //removeAll();
      int numClasses = m_Instances.attribute(m_cIndex).numValues();
      for (int noa = 0; noa < numClasses; noa++)
	{
	  System.out.println(noa);
	  JLabel jj = new JLabel(m_Instances.attribute(m_cIndex).value(noa));
	  
	  //jj.setFont(gx.getFont());
	  //jj.setBackground(Color.black);
	  //jj.setSize(m_labelMetrics.stringWidth(jj.getText()),
	  //     m_labelMetrics.getAscent());
	  //talk about having to do backflips for seemingly
	  //basic things, and flowlayout doesn't set the size of the
	  //component at all (what am i doing wrong !?!?!).
					       //jj.setText("hellod");
	  add(jj);
	  //System.out.println(jj.getLocation().x);

	  }*/
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
      int numClasses;

      if (m_colourUsingPreds) {
	numClasses = m_numPreds;
      }
      else {
	numClasses = m_Instances.attribute(m_cIndex).numValues();
      }

      int maxLabelLen = 0;
      int idx=0;
      int legendHeight;
      int w = getWidth();
      int hf = m_labelMetrics.getAscent();
      //System.out.println("not good");
      if (m_colourUsingPreds) {
	maxLabelLen = m_labelMetrics.stringWidth("88"); //if there
      } //are more than 99 clusters then it's outa hand anyway
      else {
	for (int i=0;i<numClasses;i++) {
	  if (m_Instances.attribute(m_cIndex).value(i).length() > 
	      maxLabelLen) {
	    maxLabelLen = m_Instances.
	      attribute(m_cIndex).value(i).length();
	    idx = i;
	  }
	}
      
	maxLabelLen = m_labelMetrics.stringWidth
	  (m_Instances.attribute(m_cIndex).value(idx));
      }

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
	/*ci = i % 10; mp = i / 10; mp *= 2;
	pc = m_DefaultColors[ci];
	for (int j=0;j<mp;j++) {
	  pc = pc.darker();
	  }*/
	gx.setColor((Color)m_colorList.elementAt(i));
	// can we fit the full label or will each need to be trimmed?
	if ((numToDo * maxLabelLen) > (w-(m_HorizontalPad*2))) {
	  String val;
	  if (m_colourUsingPreds) {
	    val = String.valueOf(i);
	  }
	  else {
	    val = m_Instances.attribute(m_cIndex).value(i);
	  }
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
	  NomLabel jj = new NomLabel(val, i);
	  jj.setFont(gx.getFont());
	  //jj.setBackground(Color.black);
	  jj.setSize(m_labelMetrics.stringWidth(jj.getText()),
		     m_labelMetrics.getAscent() + 4);
	  add(jj);
	  jj.setLocation(x, y);
	  jj.setForeground((Color)m_colorList.
			   elementAt(i % m_colorList.size()));
	  //gx.drawString(val,x,y);
	  x += sw + 2;
	} else {
	  //JLabel jj = new JLabel(m_Instances.attribute(m_cIndex).value(i));
	  //add(jj);
	  //jj.setFont(gx.getFont());
	  //jj.setLocation(x, y);
	  NomLabel jj;
	  if (m_colourUsingPreds) {
	    jj = new NomLabel(String.valueOf(i), i);
	  }
	  else {
	    jj = new NomLabel(m_Instances.attribute(m_cIndex).value(i), i);
	  }
	  jj.setFont(gx.getFont());
	  //jj.setBackground(Color.black);
	  jj.setSize(m_labelMetrics.stringWidth(jj.getText()),
		     m_labelMetrics.getAscent() + 4);
	  add(jj);
	  jj.setLocation(x, y);
	  jj.setForeground((Color)m_colorList.
			   elementAt(i % m_colorList.size()));
	  //  gx.drawString(m_Instances.attribute(m_cIndex).value(i),x,y);
  
	  //gx.drawString(m_Instances.attribute(m_cIndex).value(i),x,y);
	  x += ((w-(m_HorizontalPad*2)) / numToDo);
	}	  
      }

      x = m_HorizontalPad;
      y = 1+ hf + 5 +hf;
      for (int i=numToDo;i<numClasses;i++) {
	/*ci = i % 10; mp = i / 10; mp *= 2;
	pc = m_DefaultColors[ci];
	for (int j=0;j<mp;j++) {
	  pc = pc.darker();
	  }*/
	gx.setColor((Color)m_colorList.elementAt(i));
	if (((numClasses-numToDo+1) * maxLabelLen) > 
	    (w - (m_HorizontalPad*2))) {
	  String val;
	  if (m_colourUsingPreds) {
	    val = String.valueOf(i);
	  }
	  else {
	    val = m_Instances.attribute(m_cIndex).value(i);
	  }
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
	  //this is the clipped string
	  NomLabel jj = new NomLabel(val, i);
	  jj.setFont(gx.getFont());
	  //jj.setBackground(Color.black);
	  jj.setSize(m_labelMetrics.stringWidth(jj.getText()),
		     m_labelMetrics.getAscent() + 4);

	  add(jj);
	  jj.setLocation(x, y);
	  jj.setForeground((Color)m_colorList.
			   elementAt(i % m_colorList.size()));
	  //gx.drawString(val,x,y);
	  //	  x += ((w-(m_HorizontalPad*2)) / (numClasses-numToDo))+3;
	  x += sw +2;
	} else {
	  //this is the full string
	  NomLabel jj;
	  if (m_colourUsingPreds) {
	    jj = new NomLabel(String.valueOf(i), i);
	  }
	  else {
	    jj = new NomLabel(m_Instances.attribute(m_cIndex).value(i), i);
	  }
	  jj.setFont(gx.getFont());
	  //jj.setBackground(Color.black);
	  jj.setSize(m_labelMetrics.stringWidth(jj.getText()),
		     m_labelMetrics.getAscent() + 4);
	  add(jj);
	  jj.setLocation(x, y);
	  jj.setForeground((Color)m_colorList.
			   elementAt(i % m_colorList.size()));
	  //  gx.drawString(m_Instances.attribute(m_cIndex).value(i),x,y);
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
	Color c = new Color((int)rs,150,(int)(255-rs));
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
	//removeAll();
	//setLayout(new FlowLayout());
	if (m_isNumeric) {
	  //System.out.println("numo");
	  m_oldWidth = -9000;   //done so that if change back to nom, it will
	  //work
	  removeAll();
	  paintNumeric(gx);
	} else {
	  if (m_Instances != null) {
	    if (m_oldWidth != getWidth()) {
	      removeAll();
	      m_oldWidth = getWidth();
	      paintNominal(gx);
	    }
	  }
	}
      }
    }
  }
  
  
  /** Inner class to handle attribute display */
  protected class AttributePanel extends JScrollPane {
    /** The instances to be plotted */
    protected Instances m_plotInstances=null;

    /** True if colouring for clusterer */
    protected boolean m_colourUsingPreds=false;
    
    /** The predictions. */
    protected double[] m_preds= null;
    
    /** Holds the min and max values of the colouring attributes */
    protected double m_maxC;
    protected double m_minC;
    protected int m_cIndex;
    protected int m_xIndex;
    protected int m_yIndex;

    /** Holds the random height for each instance. */
    protected int[] m_heights;
    //protected Color[] colors_array;

    /** The container window for the attribute bars, and also where the
     * X,Y or B get printed.
     */ 
    protected JPanel m_span=null;
    
    /** inner inner class used for plotting the points 
     * into a bar for a particular attribute. 
     */
    protected class AttributeSpacing extends JPanel {
      /** The min and max values for this attribute. */
      protected double m_maxVal;
      protected double m_minVal;


      /** The attribute itself. */
      protected Attribute m_attrib;
      
      /** The index for this attribute. */
      protected int m_attribIndex;
      
      /** The x position of each point. */
      protected int[] m_cached;
      //note for m_cached, if you wanted to speed up the drawing algorithm
      // and save memory, the system could be setup to drop out any
      // of the instances not being drawn (you would need to find a new way
      //of matching the height however).

      /** A temporary array used to strike any instances that would be 
       * drawn redundantly.
       */
      protected boolean[][] m_pointDrawn;
      
      /** Used to determine if the positions need to be recalculated. */
      protected int m_oldWidth=-9000;

      /** The container window for the attribute bars, and also where the
       * X,Y or B get printed.
       */
      
      /**
       * This constructs the bar with the specified attribute and
       * sets its index to be used for selecting by the mouse.
       * @param a The attribute this bar represents.
       * @param aind The index of this bar.
       */
      public AttributeSpacing(Attribute a, int aind) {
	m_attrib = a;
	m_attribIndex = aind;
	setBackground(Color.black);
	setPreferredSize(new Dimension(0, 20));
	setMinimumSize(new Dimension(0, 20));
	m_cached = new int[m_plotInstances.numInstances()];
	//m_pointDrawn = new int[getWidth()][20];
	//setBorder(BorderFactory.createEtchedBorder());
	
	//this will only get allocated if m_plotInstances != null
	//this is used to determine the min and max values for plotting
	double min=Double.POSITIVE_INFINITY;
	double max=Double.NEGATIVE_INFINITY;
	double value;
	if (m_plotInstances.attribute(m_attribIndex).isNominal()) {
	  m_minVal = 0;
	  m_maxVal = m_plotInstances.attribute(m_attribIndex).numValues()-1;
	} else {
	  for (int i=0;i<m_plotInstances.numInstances();i++) {
	    if (!m_plotInstances.instance(i).isMissing(m_attribIndex)) {
	      value = m_plotInstances.instance(i).value(m_attribIndex);
	      if (value < min) {
		min = value;
	      }
	      if (value > max) {
		max = value;
	      }
	    }
	  }
	  m_minVal = min; m_maxVal = max;
	  if (min == max) {
	    m_maxVal += 0.05;
	    m_minVal -= 0.05;
	  }
	}
	
	addMouseListener(new MouseAdapter() {
	    public void mouseClicked(MouseEvent e) {
	      if ((e.getModifiers() & e.BUTTON1_MASK) == e.BUTTON1_MASK) {
		//then put this on the x axis
		//for best propagation I will go straight to the m combo
		//and set its value directly this, 
		//will then propogate through the
		//panels
		
		m_XCombo.setSelectedIndex(m_attribIndex);
	      }
	      else {
		//put it on the y axis
		m_YCombo.setSelectedIndex(m_attribIndex);
	      }
	    }
	  });
      }
      
      /**
       * Convert an raw x value to Panel x coordinate.
       * @param val the raw x value
       * @return an x value for plotting in the panel.
       */
      private double convertToPanel(double val) {
	double temp = (val - m_minVal)/(m_maxVal - m_minVal);
	double temp2 = temp * (getWidth() - 10);
	
	return temp2 + 4; 
      }
      
      /**
       * paints all the visible instances to the panel , and recalculates
       * their position if need be.
       * @param gx The graphics context.
       */
      public void paintComponent(Graphics gx) {
	super.paintComponent(gx);
	int xp, yp, h;
	h = getWidth();
	if (m_plotInstances != null) {
	  if (m_oldWidth != h) {
	    m_pointDrawn = new boolean[h][20];
	    for (int noa = 0; noa < m_plotInstances.numInstances(); noa++) {
	      if (!m_plotInstances.instance(noa).isMissing(m_attribIndex)
		  && !m_plotInstances.instance(noa).isMissing(m_cIndex)) {
		m_cached[noa] = (int)convertToPanel(m_plotInstances.
						  instance(noa).
						  value(m_attribIndex));
		
		if (m_pointDrawn[m_cached[noa] % h][m_heights[noa]]) {
		  m_cached[noa] = -9000;
		}
		else {
		  m_pointDrawn[m_cached[noa]%h][m_heights[noa]] = true;
		}
		
	      }
	      else {
		m_cached[noa] = -9000; //this value will not happen 
		//so it is safe
	      }
	    }
	    m_oldWidth = h;
	  }
	  
	  if (m_plotInstances.attribute(m_cIndex).isNominal() 
	      || m_colourUsingPreds) {
	    for (int noa = 0; noa < m_plotInstances.numInstances(); noa++) {
	      
	      if (m_cached[noa] != -9000) {
		xp = m_cached[noa];
		yp = m_heights[noa];
		if (m_plotInstances.attribute(m_attribIndex).
		    isNominal()) {
		  xp += (int)(Math.random() * 5) - 2;
		}
		int ci = (!m_colourUsingPreds)
		  ? (int)m_plotInstances.instance(noa).value(m_cIndex)
		  : (int)m_preds[noa];

		gx.setColor((Color)m_colorList.elementAt
			    (ci % m_colorList.size()));
		gx.drawRect(xp, yp, 1, 1);
	      }
	    }
	  }
	  else {
	    double r;
	    for (int noa = 0; noa < m_plotInstances.numInstances(); noa++) {
	      if (m_cached[noa] != -9000) {		  
		
		r = (m_plotInstances.instance(noa).value(m_cIndex) 
		     - m_minC) / (m_maxC - m_minC);
		r = (r * 240) + 15;
		gx.setColor(new Color((int)r,150,(int)(255-r)));
		
		xp = m_cached[noa];
		yp = m_heights[noa];
		if (m_plotInstances.attribute(m_attribIndex).
		    isNominal()) {
		  xp += (int)(Math.random() * 5) - 2;
		}
		gx.drawRect(xp, yp, 1, 1);
	      }
	    }
	  }
	} 
      }
    }   
    
    /**
     * This constructs an attributePanel.
     */
    public AttributePanel() {
      setBackground(Color.blue);
      setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
      //setLayout(null);
    }
    
    /**
     * Set an array of classifier predictions to be plotted. The predictions
     * need to correspond one to one with the Instances provided for plotting.
     * @param preds an array of classifier predictions
     */
    public void setPredictions(double [] preds) {
      m_preds = preds;
    }
    
    public void setColor(int c, double h, double l) {
      m_cIndex = c;
      m_maxC = h;
      m_minC = l;
      repaint();
    }

    /** 
     * This sets the instances to be drawn into the attribute panel
     * @param ins The instances.
     */
    public void setInstances(Instances ins) {
      if (m_span == null) {
	m_span = new JPanel() {
	    public void paintComponent(Graphics gx) {
	      super.paintComponent(gx);
	      gx.setColor(Color.red);
	      if (m_yIndex != m_xIndex) {
		gx.drawString("X", 5, m_xIndex * 20 + 16);
		gx.drawString("Y", 5, m_yIndex * 20 + 16);
	      }
	      else {
		gx.drawString("B", 5, m_xIndex * 20 + 16);
	      }
	    }
	  };
      }

      JPanel padder = new JPanel();
      JPanel padd2 = new JPanel();
      //padd2.setBackground(Color.black);
      //m_span.setBackground(Color.red);
      m_plotInstances = ins;
      
      if (m_splitListener != null) {
	m_plotInstances.randomize(new Random());
      }
      m_heights = new int[ins.numInstances()];
      //colors_array = new Color[ins.numInstances()];
      m_cIndex = ins.numAttributes() - 1;
      for (int noa = 0; noa < ins.numInstances(); noa++) {
	m_heights[noa] = (int)(Math.random() * 19);
	/*Color pc = m_DefaultColors[((int)m_plotInstances.instance(noa).
	  value(m_cIndex)) % 10];
	  int ija =  (int)(m_plotInstances.instance(noa).value(m_cIndex) / 10);
	  ija *= 2; 
	  for (int j=0;j<ija;j++)
	  {
	  pc = pc.darker();
	  }
	*/
	// if (!m_plotInstances.attribute(m_cIndex).isNumeric())
	// {
	//colors_array[noa] = (Color)
	//m_colorList.elementAt((int)m_plotInstances.instance(noa).
	//		     value(m_cIndex));	    
	//}
      }
      m_span.setPreferredSize(new Dimension(m_span.getPreferredSize().width, 
					  (m_cIndex + 1) * 20));
      m_span.setMaximumSize(new Dimension(m_span.getMaximumSize().width, 
					(m_cIndex + 1) * 20));
      AttributeSpacing tmp;
      
      GridBagLayout gb = new GridBagLayout();
      GridBagLayout gb2 = new GridBagLayout();
      GridBagConstraints constraints = new GridBagConstraints();
      //getViewport().setLayout(gb);
      
      m_span.removeAll();

      padder.setLayout(gb);
      m_span.setLayout(gb2);
      constraints.anchor = GridBagConstraints.CENTER;
      constraints.gridx=0;constraints.gridy=0;constraints.weightx=5;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.gridwidth=1;constraints.gridheight=1;
      constraints.insets = new Insets(0, 0, 0, 0);
      padder.add(m_span, constraints);
      constraints.gridx=0;constraints.gridy=1;constraints.weightx=5;
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridwidth=1;constraints.gridheight=1;constraints.weighty=5;
      constraints.insets = new Insets(0, 0, 0, 0);
      padder.add(padd2, constraints);
      constraints.weighty=0;
      setViewportView(padder);
      //getViewport().setLayout(null);
      //m_span.setMinimumSize(new Dimension(100, (m_cIndex + 1) * 24));
      //m_span.setSize(100, (m_cIndex + 1) * 24);
      constraints.anchor = GridBagConstraints.CENTER;
      constraints.gridx=0;constraints.gridy=0;constraints.weightx=5;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.gridwidth=1;constraints.gridheight=1;constraints.weighty=5;
      constraints.insets = new Insets(2,20,2,4);

      

       for (int noa = 0; noa < ins.numAttributes(); noa++) {
	 tmp = new AttributeSpacing(ins.attribute(noa), noa);
	 
	 constraints.gridy = noa;
	 //tmp.setBackground(Color.white);
	 //tmp.setMinimumSize(new Dimension(0, 40));
	 m_span.add(tmp, constraints);
       }
    }
    
    /**
     * shows which bar is the current x attribute.
     * @param x The attributes index.
     */
    public void setX(int x) {
      m_xIndex = x;
      m_span.repaint();
    }
    
    /**
     * shows which bar is the current y attribute.
     * @param y The attributes index.
     */
    public void setY(int y) {
      m_yIndex = y;
      m_span.repaint();
    }
  }

  /** Inner class to handle plotting */
  protected class PlotPanel extends JPanel {

    /** The instances to be plotted */
    protected Instances m_plotInstances=null;

    /** The original instances. */
    protected Instances m_origInstances=null;
    
    /** The original preds. */
    // protected  m_origPreds

    /** Indexes of the attributes to go on the x and y axis and the attribute
	to use for colouring and the current shape for drawing */
    protected int m_xIndex=0;
    protected int m_yIndex=0;
    protected int m_cIndex=0;
    protected int m_sIndex=0;

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

    /** Holds the original predictions. */
    private double [] m_origPreds=null;

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

    /** An array used to show if a point is hidden or not.
     * This is used for speeding up the drawing of the plot panel
     * although I am not sure how much performance this grants over
     * not having it.
     */
    private int[][] m_drawnPoints;

    /** Font for labels */
    private Font m_labelFont;
    private FontMetrics m_labelMetrics=null; 

    /** the level of jitter */
    private int m_JitterVal=0;

    /** random values for perterbing the data points */
    private Random m_JRand = new Random(0);

    /** lookup table for plotted points */
    private double [][] m_pointLookup=null;
    private double [][] m_extPointLookup=null;

    private JFrame m_InstanceInfo = null;
    private JTextArea m_InstanceInfoText = new JTextArea();
    
    /** True if the user is currently dragging a box. */
    private boolean m_createShape;
    
    /** contains all the shapes that have been drawn for these attribs */
    private FastVector m_shapes;

    /** contains the points of the shape currently being drawn. */
    private FastVector m_shapePoints;

    /** contains the position of the mouse (used for rubberbanding). */
    private Dimension m_newMousePos;

    //////
    /** Constructor */
    public PlotPanel() {
      setBackground(Color.black);
      m_InstanceInfoText.setFont(new Font("Monospaced", Font.PLAIN,12));
      m_InstanceInfoText.setEditable(false);
      m_createShape = false;        
      m_shapes = null;////
      m_shapePoints = null;
      m_newMousePos = new Dimension();
      m_drawnPoints = new int[getWidth()][getHeight()];

      addMouseListener(new MouseAdapter() {
	  ///////      
	  public void mousePressed(MouseEvent e) {
	    if ((e.getModifiers() & e.BUTTON1_MASK) == e.BUTTON1_MASK) {
	      //
	      if (m_sIndex == 0) {
		//do nothing it will get dealt to in the clicked method
	      }
	      else if (m_sIndex == 1) {
		m_createShape = true;
		m_shapePoints = new FastVector(5);
		m_shapePoints.addElement(new Double(m_sIndex));
		m_shapePoints.addElement(new Double(e.getX()));
		m_shapePoints.addElement(new Double(e.getY()));
		m_shapePoints.addElement(new Double(e.getX()));
		m_shapePoints.addElement(new Double(e.getY()));
		Graphics g = getGraphics();
		g.setColor(Color.black);
		g.setXORMode(Color.white);
		g.drawRect(((Double)m_shapePoints.elementAt(1)).intValue(),
			   ((Double)m_shapePoints.elementAt(2)).intValue(),
			   ((Double)m_shapePoints.elementAt(3)).intValue() -
			   ((Double)m_shapePoints.elementAt(1)).intValue(), 
			   ((Double)m_shapePoints.elementAt(4)).intValue() -
			   ((Double)m_shapePoints.elementAt(2)).intValue());
		g.dispose();
	      }
	      //System.out.println("clicked");
	    }
	    //System.out.println("clicked");
	  }
	  //////
	  public void mouseClicked(MouseEvent e) {
	    
	    if ((m_sIndex == 2 || m_sIndex == 3) && 
		(m_createShape || 
		 (e.getModifiers() & e.BUTTON1_MASK) == e.BUTTON1_MASK)) {
	      if (m_createShape) {
		//then it has been started already.
		Graphics g = getGraphics();
		g.setColor(Color.black);
		g.setXORMode(Color.white);
		if ((e.getModifiers() & e.BUTTON1_MASK) == e.BUTTON1_MASK) {
		  m_shapePoints.addElement(new 
		    Double(convertToAttribX(e.getX())));
		  
		  m_shapePoints.addElement(new 
		    Double(convertToAttribY(e.getY())));
		  
		  m_newMousePos.width = e.getX();
		  m_newMousePos.height = e.getY();
		  g.drawLine((int)Math.ceil
			     (convertToPanelX
			      (((Double)m_shapePoints.
				elementAt(m_shapePoints.size() - 2)).
			       doubleValue())),
			     
			     (int)Math.ceil
			     (convertToPanelY
			      (((Double)m_shapePoints.
				elementAt(m_shapePoints.size() - 1)).
			       doubleValue())),
			     m_newMousePos.width, m_newMousePos.height);
		  
		}
		else if (m_sIndex == 3) {
		  //then extend the lines to infinity 
		  //(100000 or so should be enough).
		  //the area is selected by where the user right clicks 
		  //the mouse button
		  
		  m_createShape = false;
		  if (m_shapePoints.size() >= 5) {
		    double cx = Math.ceil
		      (convertToPanelX
		       (((Double)m_shapePoints.elementAt
			 (m_shapePoints.size() - 4)).doubleValue()));
		    
		    double cx2 = Math.ceil
		      (convertToPanelX
		       (((Double)m_shapePoints.elementAt
			 (m_shapePoints.size() - 2)).doubleValue())) - 
		      cx;
		    
		    cx2 *= 50000;
		    
		    double cy = Math.ceil
		      (convertToPanelY(((Double)m_shapePoints.elementAt
					(m_shapePoints.size() - 3)).
				       doubleValue()));
		    double cy2 = Math.ceil
		      (convertToPanelY(((Double)m_shapePoints.elementAt
					(m_shapePoints.size() - 1)).
				       doubleValue())) - cy;
		    cy2 *= 50000;
			    
		    
		    double cxa = Math.ceil(convertToPanelX
					   (((Double)m_shapePoints.
					     elementAt(3)).
					    doubleValue()));
		    double cxa2 = Math.ceil(convertToPanelX
					    (((Double)m_shapePoints.
					      elementAt(1)).
					     doubleValue())) - cxa;
		    cxa2 *= 50000;
		    
		    
		    double cya = Math.ceil
		      (convertToPanelY
		       (((Double)m_shapePoints.elementAt(4)).
			doubleValue()));
		    double cya2 = Math.ceil
		      (convertToPanelY
		       (((Double)m_shapePoints.elementAt(2)).
			doubleValue())) - cya;
		    
		    cya2 *= 50000;
		    
		    m_shapePoints.setElementAt
		      (new Double(convertToAttribX(cxa2 + cxa)), 1);
		    
		    m_shapePoints.setElementAt
		      (new Double(convertToAttribY(cy2 + cy)), 
		       m_shapePoints.size() - 1);
		    
		    m_shapePoints.setElementAt
		      (new Double(convertToAttribX(cx2 + cx)), 
		       m_shapePoints.size() - 2);
		    
		    m_shapePoints.setElementAt
		      (new Double(convertToAttribY(cya2 + cya)), 2);
		    
		    
		    //determine how infinity line should be built
		    
		    cy = Double.POSITIVE_INFINITY;
		    cy2 = Double.NEGATIVE_INFINITY;
		    if (((Double)m_shapePoints.elementAt(1)).
			doubleValue() > 
			((Double)m_shapePoints.elementAt(3)).
			doubleValue()) {
		      if (((Double)m_shapePoints.elementAt(2)).
			  doubleValue() == 
			  ((Double)m_shapePoints.elementAt(4)).
			  doubleValue()) {
			cy = ((Double)m_shapePoints.elementAt(2)).
			  doubleValue();
		      }
		    }
		    if (((Double)m_shapePoints.elementAt
			 (m_shapePoints.size() - 2)).doubleValue() > 
			((Double)m_shapePoints.elementAt
			 (m_shapePoints.size() - 4)).doubleValue()) {
		      if (((Double)m_shapePoints.elementAt
			   (m_shapePoints.size() - 3)).
			  doubleValue() == 
			  ((Double)m_shapePoints.elementAt
			   (m_shapePoints.size() - 1)).doubleValue()) {
			cy2 = ((Double)m_shapePoints.lastElement()).
			  doubleValue();
		      }
		    }
		    m_shapePoints.addElement(new Double(cy));
		    m_shapePoints.addElement(new Double(cy2));
		    
		    if (!inPolyline(m_shapePoints, convertToAttribX
				    (e.getX()), 
				    convertToAttribY(e.getY()))) {
		      Double tmp = (Double)m_shapePoints.
			elementAt(m_shapePoints.size() - 2);
		      m_shapePoints.setElementAt
			(m_shapePoints.lastElement(), 
			 m_shapePoints.size() - 2);
		      m_shapePoints.setElementAt
			(tmp, m_shapePoints.size() - 1);
		    }
		    
		    if (m_shapes == null) {
		      m_shapes = new FastVector(4);
		    }
		    m_shapes.addElement(m_shapePoints);

		    m_submit.setText("Submit");
		    m_submit.setActionCommand("Submit");
		    
		    m_submit.setEnabled(true);
		  }
		  
		  m_shapePoints = null;
		  repaint();
		  
		}
		else {
		  //then close the shape
		  m_createShape = false;
		  if (m_shapePoints.size() >= 7) {
		    m_shapePoints.addElement(m_shapePoints.elementAt(1));
		    m_shapePoints.addElement(m_shapePoints.elementAt(2));
		    if (m_shapes == null) {
		      m_shapes = new FastVector(4);
		    }
		    m_shapes.addElement(m_shapePoints);
			   
		    m_submit.setText("Submit");
		    m_submit.setActionCommand("Submit");
		    
		    m_submit.setEnabled(true);
		  }
		  m_shapePoints = null;
		  repaint();
		}
		g.dispose();
		//repaint();
	      }
	      else if ((e.getModifiers() & e.BUTTON1_MASK) == e.BUTTON1_MASK) {
		//then this is the first point
		m_createShape = true;
		m_shapePoints = new FastVector(17);
		m_shapePoints.addElement(new Double(m_sIndex));
		m_shapePoints.addElement(new 
		  Double(convertToAttribX(e.getX())));      //the new point
		m_shapePoints.addElement(new 
		  Double(convertToAttribY(e.getY())));
		m_newMousePos.width = e.getX();      //the temp mouse point
		m_newMousePos.height = e.getY();
		Graphics g = getGraphics();
		g.setColor(Color.black);
		g.setXORMode(Color.white);
		g.drawLine((int)Math.ceil
			   (convertToPanelX(((Double)m_shapePoints.
					     elementAt(1)).doubleValue())),
			   (int)Math.ceil
			   (convertToPanelY(((Double)m_shapePoints.
					     elementAt(2)).doubleValue())),
			   m_newMousePos.width, m_newMousePos.height);
		g.dispose();
	      }
	    }
	    else {
	      if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == 
		  InputEvent.BUTTON1_MASK) {
		
		searchPoints(e.getX(),e.getY(), false);
	      } else {
		searchPoints(e.getX(), e.getY(), true);
	      }
	    }
	  }
	  
	  /////////             
	  public void mouseReleased(MouseEvent e) {
	    //System.out.println("rel");
	    if (m_createShape) {
	      if (((Double)m_shapePoints.elementAt(0)).intValue() == 1) {
		m_createShape = false;
		Graphics g = getGraphics();
		g.setColor(Color.black);
		g.setXORMode(Color.white);
		g.drawRect(((Double)m_shapePoints.elementAt(1)).
			   intValue(), 
			   ((Double)m_shapePoints.elementAt(2)).intValue(),
			   ((Double)m_shapePoints.elementAt(3)).intValue() -
			   ((Double)m_shapePoints.elementAt(1)).intValue(), 
			   ((Double)m_shapePoints.elementAt(4)).intValue() -
			   ((Double)m_shapePoints.elementAt(2)).intValue());
		
		g.dispose();
		if (checkPoints(((Double)m_shapePoints.elementAt(1)).
				doubleValue(), 
				((Double)m_shapePoints.elementAt(2)).
				doubleValue()) &&
		    checkPoints(((Double)m_shapePoints.elementAt(3)).
				doubleValue(), 
				((Double)m_shapePoints.elementAt(4)).
				doubleValue())) {
		  //then the points all land on the screen
		  //now do special check for the rectangle
		  if (((Double)m_shapePoints.elementAt(1)).doubleValue() <
		      ((Double)m_shapePoints.elementAt(3)).doubleValue() 
		      &&
		      ((Double)m_shapePoints.elementAt(2)).doubleValue() <
		      ((Double)m_shapePoints.elementAt(4)).doubleValue()) {
		    //then the rectangle is valid
		    if (m_shapes == null) {
		      m_shapes = new FastVector(2);
		    }
		    m_shapePoints.setElementAt(new 
		      Double(convertToAttribX(((Double)m_shapePoints.
					       elementAt(1)).
					      doubleValue())), 1);
		    m_shapePoints.setElementAt(new 
		      Double(convertToAttribY(((Double)m_shapePoints.
					       elementAt(2)).
					      doubleValue())), 2);
		    m_shapePoints.setElementAt(new 
		      Double(convertToAttribX(((Double)m_shapePoints.
					       elementAt(3)).
					      doubleValue())), 3);
		    m_shapePoints.setElementAt(new 
		      Double(convertToAttribY(((Double)m_shapePoints.
					       elementAt(4)).
					      doubleValue())), 4);
		    
		    m_shapes.addElement(m_shapePoints);
		    
		    m_submit.setText("Submit");
		    m_submit.setActionCommand("Submit");
		    
		    m_submit.setEnabled(true);

		    repaint();
		  }
		}
		m_shapePoints = null;
	      }
	    }
	  }
	});
      
      addMouseMotionListener(new MouseMotionAdapter() {
	  public void mouseDragged(MouseEvent e) {
	    //check if the user is dragging a box
	    if (m_createShape) {
	      if (((Double)m_shapePoints.elementAt(0)).intValue() == 1) {
		Graphics g = getGraphics();
		g.setColor(Color.black);
		g.setXORMode(Color.white);
		g.drawRect(((Double)m_shapePoints.elementAt(1)).intValue(), 
			   ((Double)m_shapePoints.elementAt(2)).intValue(),
			   ((Double)m_shapePoints.elementAt(3)).intValue() -
			   ((Double)m_shapePoints.elementAt(1)).intValue(), 
			   ((Double)m_shapePoints.elementAt(4)).intValue() -
			   ((Double)m_shapePoints.elementAt(2)).intValue());
		
		m_shapePoints.setElementAt(new Double(e.getX()), 3);
		m_shapePoints.setElementAt(new Double(e.getY()), 4);
		
		g.drawRect(((Double)m_shapePoints.elementAt(1)).intValue(), 
			   ((Double)m_shapePoints.elementAt(2)).intValue(),
			   ((Double)m_shapePoints.elementAt(3)).intValue() -
			   ((Double)m_shapePoints.elementAt(1)).intValue(), 
			   ((Double)m_shapePoints.elementAt(4)).intValue() -
			   ((Double)m_shapePoints.elementAt(2)).intValue());
		g.dispose();
	      }
	    }
	  }
	  
	  public void mouseMoved(MouseEvent e) {
	    if (m_createShape) {
	      if (((Double)m_shapePoints.elementAt(0)).intValue() == 2 || 
		  ((Double)m_shapePoints.elementAt(0)).intValue() == 3) {
		Graphics g = getGraphics();
		g.setColor(Color.black);
		g.setXORMode(Color.white);
		g.drawLine((int)Math.ceil(convertToPanelX
					  (((Double)m_shapePoints.elementAt
					    (m_shapePoints.size() - 2)).
					   doubleValue())),
			   (int)Math.ceil(convertToPanelY
					  (((Double)m_shapePoints.elementAt
					    (m_shapePoints.size() - 1)).
					   doubleValue())),
			   m_newMousePos.width, m_newMousePos.height);
		
		m_newMousePos.width = e.getX();
		m_newMousePos.height = e.getY();
		
		g.drawLine((int)Math.ceil(convertToPanelX
					  (((Double)m_shapePoints.elementAt
					    (m_shapePoints.size() - 2)).
					   doubleValue())),
			   (int)Math.ceil(convertToPanelY
					  (((Double)m_shapePoints.elementAt
					    (m_shapePoints.size() - 1)).
					   doubleValue())),
			   m_newMousePos.width, m_newMousePos.height);
		g.dispose();
	      }
	    }
	  }
	});
      
      m_submit.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	 
	    if (e.getActionCommand().equals("Submit")) {
	      if (m_splitListener != null && m_shapes != null) {
		//then send the split to the listener
		Instances sub_set1 = new Instances(m_plotInstances, 500);
		Instances sub_set2 = new Instances(m_plotInstances, 500);
		
		if (m_plotInstances != null) {
		  
		  for (int noa = 0 ; noa < m_plotInstances.numInstances(); 
		       noa++) {
		    if (!m_plotInstances.instance(noa).isMissing(m_xIndex)
			&& !m_plotInstances.instance(noa).isMissing(m_yIndex)){
		      
		      if (inSplit(m_plotInstances.instance(noa))) {
			sub_set1.add(m_plotInstances.instance(noa));
		      }
		      else {
			sub_set2.add(m_plotInstances.instance(noa));
		      }
		    }
		  }
		  FastVector tmp = m_shapes;
		  cancelShapes();
		  m_splitListener.userDataEvent(new 
		    VisualizePanelEvent(tmp, sub_set1, sub_set2, m_xIndex, 
					m_yIndex));
		}
	      }
	      else if (m_shapes != null && m_plotInstances != null) { 
		Instances sub_set1 = new Instances(m_plotInstances, 500);
		int count = 0;
		for (int noa = 0 ; noa < m_plotInstances.numInstances(); 
		     noa++) {
		  if (inSplit(m_plotInstances.instance(noa))) {
		    sub_set1.add(m_plotInstances.instance(noa));
		      count++;
		  }
		
		}
   		double [] nPreds = null;
		int x = m_xIndex;
		int y = m_yIndex;
		//boolean p = m_predsNumeric;
		if (m_origInstances == null) {
		    //this sets these instances as the instances 
		    //to go back to.
		    m_origInstances = m_plotInstances;
		  }
		if (m_origPreds == null && m_preds != null) {
		  m_origPreds = m_preds;
		}

		if (m_preds != null && count > 0) {
		  nPreds = new double[m_origInstances.numInstances()];
		  count = 0;
		  for (int noa = 0; noa < m_plotInstances.numInstances(); 
		       noa++) {
		    if (inSplit(m_plotInstances.instance(noa))) {
		      //System.out.println(m_preds[noa]);
		      nPreds[count] = m_preds[noa];
		      count++;
		    }
		  }
		}
		cancelShapes();
		VisualizePanel.this.setInstances(sub_set1);
		if (m_preds != null) {
		  VisualizePanel.this.setPredictions(nPreds);
		  //VisualizePanel.this.setPredictionsNumeric(p);
		}
		try {
		  VisualizePanel.this.setXIndex(x);
		  VisualizePanel.this.setYIndex(y);
		} catch(Exception er) {
		  System.out.println("Error : " + er);
		  //  System.out.println("Part of user input so had to" +
		  //		 " catch here");
		}
	      }
	    }
	    else if (e.getActionCommand().equals("Reset")) {
	      int x = m_xIndex;
	      int y = m_yIndex;
	      //boolean p = m_predsNumeric;
	      VisualizePanel.this.setInstances(m_origInstances);
	      if (m_origPreds != null) {
		VisualizePanel.this.setPredictions(m_origPreds);
		//VisualizePanel.this.setPredictionsNumeric(p);
	      }
	      try {
		VisualizePanel.this.setXIndex(x);
		VisualizePanel.this.setYIndex(y);
	      } catch(Exception er) {
		System.out.println("Error : " + er);
	      }
	    }
	  }  
	});

      m_cancel.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    cancelShapes();
	    repaint();
	  }
	});
      ////////////
    }
    
    /**
     * @return The FastVector containing all the shapes.
     */
    public FastVector getShapes() {
      
      return m_shapes;
    }
    
    /**
     * Sets the list of shapes to empty and also cancels
     * the current shape being drawn (if applicable).
     */
    public void cancelShapes() {
       
      if (m_splitListener == null) {
	m_submit.setText("Reset");
	m_submit.setActionCommand("Reset");
	if (m_origInstances == null || m_origInstances == m_plotInstances) {
	  m_submit.setEnabled(false);
	}
	else {
	  m_submit.setEnabled(true);
	}
      }
      else {
	m_submit.setEnabled(false);
      }
      
      m_createShape = false;
      m_shapePoints = null;
      m_shapes = null;
      repaint();
    }

    /**
     * This can be used to set the shapes that should appear.
     * @param v The list of shapes.
     */
    public void setShapes(FastVector v) {
      //note that this method should be fine for doubles,
      //but anything else that uses something other than doubles 
      //(or uneditable objects) could have unsafe copies.
      if (v != null) {
	FastVector temp;
	m_shapes = new FastVector(v.size());
	for (int noa = 0; noa < v.size(); noa++) {
	  temp = new FastVector(((FastVector)v.elementAt(noa)).size());
	  m_shapes.addElement(temp);
	  for (int nob = 0; nob < ((FastVector)v.elementAt(noa)).size()
		 ; nob++) {
	    
	    temp.addElement(((FastVector)v.elementAt(noa)).elementAt(nob));
	    
	  }
	}
  
      
      }
      else {
	m_shapes = null;
      }
      repaint();

    }
    
    /** 
     * This will check the values of the screen points passed and make sure 
     * that they land on the screen
     * @param x1 The x coord.
     * @param y1 The y coord.
     */
    private boolean checkPoints(double x1, double y1) {
      if (x1 < 0 || x1 > getSize().width || y1 < 0 || y1 > getSize().height) {
	return false;
      }
      return true;
    }
    
    /**
     * This will check if an instance is inside or outside of the current
     * shapes.
     * @param i The instance to check.
     * @return True if 'i' falls inside the shapes, false otherwise.
     */
    public boolean inSplit(Instance i) {
      //this will check if the instance lies inside the shapes or not
      
      if (m_shapes != null) {
	FastVector stmp;
	double x1, y1, x2, y2;
	for (int noa = 0; noa < m_shapes.size(); noa++) {
	  stmp = (FastVector)m_shapes.elementAt(noa);
	  if (((Double)stmp.elementAt(0)).intValue() == 1) {
	    //then rectangle
	    x1 = ((Double)stmp.elementAt(1)).doubleValue();
	    y1 = ((Double)stmp.elementAt(2)).doubleValue();
	    x2 = ((Double)stmp.elementAt(3)).doubleValue();
	    y2 = ((Double)stmp.elementAt(4)).doubleValue();
	    if (i.value(m_xIndex) >= x1 && i.value(m_xIndex) <= x2 &&
		i.value(m_yIndex) <= y1 && i.value(m_yIndex) >= y2) {
	      //then is inside split so return true;
	      return true;
	    }
	  }
	  else if (((Double)stmp.elementAt(0)).intValue() == 2) {
	    //then polygon
	    if (inPoly(stmp, i.value(m_xIndex), i.value(m_yIndex))) {
	      return true;
	    }
	  }
	  else if (((Double)stmp.elementAt(0)).intValue() == 3) {
	    //then polyline
	    if (inPolyline(stmp, i.value(m_xIndex), i.value(m_yIndex))) {
	      return true;
	    }
	  }
	}
      }
      return false;
    }
    
    /**
     * Checks to see if the coordinate passed is inside the ployline
     * passed, Note that this is done using attribute values and not there
     * respective screen values.
     * @param ob The polyline.
     * @param x The x coord.
     * @param y The y coord.
     * @return True if it falls inside the polyline, false otherwise.
     */
    private boolean inPolyline(FastVector ob, double x, double y) {
      //this works similar to the inPoly below except that
      //the first and last lines are treated as extending infinite in one 
      //direction and 
      //then infinitly in the x dirction their is a line that will 
      //normaly be infinite but
      //can be finite in one or both directions
      
      int countx = 0;
      double vecx, vecy;
      double change;
      double x1, y1, x2, y2;
      
      for (int noa = 1; noa < ob.size() - 4; noa+= 2) {
	y1 = ((Double)ob.elementAt(noa+1)).doubleValue();
	y2 = ((Double)ob.elementAt(noa+3)).doubleValue();
	x1 = ((Double)ob.elementAt(noa)).doubleValue();
	x2 = ((Double)ob.elementAt(noa+2)).doubleValue();
	
	//System.err.println(y1 + " " + y2 + " " + x1 + " " + x2);
	vecy = y2 - y1;
	vecx = x2 - x1;
	if (noa == 1 && noa == ob.size() - 6) {
	  //then do special test first and last edge
	  if (vecy != 0) {
	    change = (y - y1) / vecy;
	    if (vecx * change + x1 >= x) {
	      //then intersection
	      countx++;
	    }
	  }
	}
	else if (noa == 1) {
	  if ((y < y2 && vecy > 0) || (y > y2 && vecy < 0)) {
	    //now just determine intersection or not
	    change = (y - y1) / vecy;
	    if (vecx * change + x1 >= x) {
	      //then intersection on horiz
	      countx++;
	    }
	  }
	}
	else if (noa == ob.size() - 6) {
	  //then do special test on last edge
	  if ((y <= y1 && vecy < 0) || (y >= y1 && vecy > 0)) {
	    change = (y - y1) / vecy;
	    if (vecx * change + x1 >= x) {
	      countx++;
	    }
	  }
	}
	else if ((y1 <= y && y < y2) || (y2 < y && y <= y1)) {
	  //then continue tests.
	  if (vecy == 0) {
	    //then lines are parallel stop tests in 
	    //ofcourse it should never make it this far
	  }
	  else {
	    change = (y - y1) / vecy;
	    if (vecx * change + x1 >= x) {
	      //then intersects on horiz
	      countx++;
	    }
	  }
	}
      }
      
      //now check for intersection with the infinity line
      y1 = ((Double)ob.elementAt(ob.size() - 2)).doubleValue();
      y2 = ((Double)ob.elementAt(ob.size() - 1)).doubleValue();
      
      if (y1 > y2) {
	//then normal line
	if (y1 >= y && y > y2) {
	  countx++;
	}
      }
      else {
	//then the line segment is inverted
	if (y1 >= y || y > y2) {
	  countx++;
	}
      }
      
      if ((countx % 2) == 1) {
	return true;
      }
      else {
	return false;
      }
    }


    /**
     * This checks to see if The coordinate passed is inside
     * the polygon that was passed.
     * @param ob The polygon.
     * @param x The x coord.
     * @param y The y coord.
     * @return True if the coordinate is in the polygon, false otherwise.
     */
    private boolean inPoly(FastVector ob, double x, double y) {
      //brief on how this works
      //it draws a line horizontally from the point to the right (infinitly)
      //it then sees how many lines of the polygon intersect this, 
      //if it is even then the point is
      // outside the polygon if it's odd then it's inside the polygon
      int count = 0;
      double vecx, vecy;
      double change;
      double x1, y1, x2, y2;
      for (int noa = 1; noa < ob.size() - 2; noa += 2) {
	y1 = ((Double)ob.elementAt(noa+1)).doubleValue();
	y2 = ((Double)ob.elementAt(noa+3)).doubleValue();
	if ((y1 <= y && y < y2) || (y2 < y && y <= y1)) {
	  //then continue tests.
	  vecy = y2 - y1;
	  if (vecy == 0) {
	    //then lines are parallel stop tests for this line
	  }
	  else {
	    x1 = ((Double)ob.elementAt(noa)).doubleValue();
	    x2 = ((Double)ob.elementAt(noa+2)).doubleValue();
	    vecx = x2 - x1;
	    change = (y - y1) / vecy;
	    if (vecx * change + x1 >= x) {
	      //then add to count as an intersected line
	      count++;
	    }
	  }
	}
      }
      if ((count % 2) == 1) {
	//then lies inside polygon
	//System.out.println("in");
	return true;
      }
      else {
	//System.out.println("out");
	return false;
      }
      //System.out.println("WHAT?!?!?!?!!?!??!?!");
      //return false;
    }
    
    /**
     * This will create a new visualize window with the instances 
     * inside the rectangle.
     *
     * @param x1 The left side.
     * @param y1 The top.
     * @param x2 The right side.
     * @param y2 The bottom.
     * @param new_win True if a new Visualize panel should be made to display
     * the data.
     */
    private void selection(int x1, int y1, int x2, int y2, boolean new_win) {
      if (m_pointLookup != null) {
	Instances sub_set = new Instances(m_plotInstances, 500);
	Instances sub_set2 = new Instances(m_plotInstances, 500);
	for (int i=0;i<m_plotInstances.numInstances();i++) {
	  if (m_pointLookup[i][0] != Double.NEGATIVE_INFINITY) {
	    double px = m_pointLookup[i][0]+m_pointLookup[i][3];
	    double py = m_pointLookup[i][1]+m_pointLookup[i][4];
	    double size = m_pointLookup[i][2];
	    if ((x1 <= px-size) && (x2 >= px+size) &&
		(y1 <= py-size) && (y2 >= py+size)) {
	      //then instance lies in the rect
	  
	      sub_set.add(m_plotInstances.instance(i));
	    }
	    else {
	      //then instance lies outside the rect
	      sub_set2.add(m_plotInstances.instance(i));
	    }
	  }
	}
	if (m_splitListener != null) {
	  double[] ruy = new double[1]; //just so that the compiler won't 
	  //complain
	  if (m_sIndex == 0) {
	    //rectangle
	    ruy = new double[4];
	    ruy[0] = convertToAttribX(x1);
	    ruy[1] = convertToAttribY(y1);
	    ruy[2] = convertToAttribX(x2);
	    ruy[3] = convertToAttribY(y2);
	  }
	}
	//VisualizePanel pan = new VisualizePanel();
	VisualizePanel.this.setInstances(sub_set);
	//    JFrame nf = new JFrame();
	//    nf.setSize(400, 300);
	//    nf.getContentPane().add(pan);
	//    nf.show();
      }
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
	  m_drawnPoints = new int[m_XaxisEnd - m_XaxisStart + 1]
	    [m_YaxisEnd - m_YaxisStart + 1];
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

      // this just ensures that the shapes get disposed of 
      //if the attribs change
      if (x != m_xIndex) {
	cancelShapes();
      }
      m_xIndex = x;
      repaint();
    }
    
    /**
     * Set the index of the attribute to go on the y axis
     * @param y the index of the attribute to use on the y axis
     */
    public void setYindex(int y) {
    
      // this just ensures that the shapes get disposed of 
      //if the attribs change
      if (y != m_yIndex) {
	cancelShapes();
      }
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
     * Set the index of the attribute to use for the shape.
     * @param s the index of the attribute to use for the shape
     */
    public void setSindex(int s) {
      if (s != m_sIndex) {
	m_shapePoints = null;
	m_createShape = false;
      }
      m_sIndex = s;
      repaint();
    }
    
    /**
     * Set the instances to plot
     * @param inst the instances
     * @param cIndex the index of the colouring attribute
     */
    public void setInstances(Instances inst, int cIndex) {
      
      if (m_splitListener == null) {
	m_submit.setText("Reset");
	m_submit.setActionCommand("Reset");
	if (m_origInstances == null || m_origInstances == inst) {
	  m_submit.setEnabled(false);
	}
	else {
	  m_submit.setEnabled(true);
	}
      } 
      else {
	m_submit.setEnabled(false);
      }

      m_plotInstances = inst;
      if (m_splitListener != null) {
	m_plotInstances.randomize(new Random());
      }
      m_xIndex=0;
      m_yIndex=0;
      m_cIndex=cIndex;
      m_pointLookup = new double [m_plotInstances.numInstances()][5];
      cancelShapes();
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
	m_labelFont = new Font("Monospaced", Font.PLAIN, 12);
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
     * @param newFrame true if instance info is to be displayed in a
     * new frame.
     */
    private void searchPoints(int x, int y, final boolean newFrame) {
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
		    (m_plotInstances.classIndex() < 0 || 
		     !m_plotInstances.instance(i).
		     isMissing(m_plotInstances.classIndex()))) {
		  if (longest > 9) {
		    for (int k = 0;k < (longest-9); k++) {
		      insts.append(" ");
		    }
		  }
		   
		  if (m_plotInstances.classIndex() >=0) {
		    insts.append("predicted : ");
		    if (m_plotInstances.attribute(m_cIndex).isNominal()) {
		      insts.append(m_plotInstances.
				   attribute(m_plotInstances.classIndex()).
				   value((int)m_preds[i]));
		    } else {
		      insts.append(m_preds[i]);
		    }
		  } else {
		    insts.append("  cluster : ");
		    insts.append((int)m_preds[i]);
		  }
		 
		  insts.append("\n");
		}
	      }
	    }
	  }
	}

	if (insts.length() > 0) {
	  // Pop up a new frame
	  if (newFrame || m_InstanceInfo == null) {
	    JTextArea jt = new JTextArea();
	    jt.setFont(new Font("Monospaced", Font.PLAIN,12));
	    jt.setEditable(false);
	    jt.setText(insts.toString());
	    final JFrame jf = new JFrame("Weka : Instance info");
	    final JFrame testf = m_InstanceInfo;
	    jf.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		  if (!newFrame || testf == null) {
		    m_InstanceInfo = null;
		  }
		  jf.dispose();
		}
	      });
	    jf.getContentPane().setLayout(new BorderLayout());
	    jf.getContentPane().add(new JScrollPane(jt), BorderLayout.CENTER);
	    jf.pack();
	    jf.setSize(320, 400);
	    jf.setVisible(true);
	    if (m_InstanceInfo == null) {
	      m_InstanceInfo = jf;
	      m_InstanceInfoText = jt;
	    }
	  }  else {
	    // Overwrite info in existing frame	  
	    m_InstanceInfoText.setText(insts.toString());
	  }
	}
      }
    }

    /**
     * Determine the min and max values for axis and colouring attributes
     */
    public void determineBounds() {
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
	  //m_extinstances is instances dropped into the visualize panel
	  //which would not normally exist there
	  if (m_extInstances != null) {
	    for (int noa = 0; noa < m_extInstances.numInstances(); noa++) {
	      if (!m_extInstances.instance(noa).isMissing(m_xIndex)) {
		value = m_extInstances.instance(noa).value(m_xIndex);
		if (value < min) {
		  min = value;
		}
		if (value > max) {
		  max = value;
		}
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
	  
	  if (m_extInstances != null) {
	    for (int noa = 0; noa < m_extInstances.numInstances(); noa++) {
	      if (!m_extInstances.instance(noa).isMissing(m_yIndex)) {
		value = m_extInstances.instance(noa).value(m_yIndex);
		if (value < min) {
		  min = value;
		}
		if (value > max) {
		  max = value;
		}
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
	if (m_extInstances != null) {
	  for (int noa = 0; noa < m_extInstances.numInstances(); noa++) {
	    if (!m_extInstances.instance(noa).isMissing(m_cIndex)) {
	      value = m_extInstances.instance(noa).value(m_cIndex);
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
	m_attrib.setColor(m_cIndex, m_maxC, m_minC);

	if (m_pointLookup != null) {
	  if (m_extInstances != null) {
	    m_extPointLookup = new double[m_extInstances.numInstances()][5];
	  }
	  fillLookup();
	  setJitter(m_JitterVal);
	  repaint();
	}
      }
    }

    
    //to convert screen coords to attrib values
    // note that I use a double to avoid accuracy 
    //headaches with ints
    /**
     * convert a Panel x coordinate to a raw x value.
     * @param scx The Panel x coordinate
     * @return A raw x value.
     */
    private double convertToAttribX(double scx) {
      double temp = m_XaxisEnd - m_XaxisStart;
      double temp2 = ((scx - m_XaxisStart) * (m_maxX - m_minX)) / temp;
      
      temp2 = temp2 + m_minX;
      
      return temp2;
    }
    
    /**
     * convert a Panel y coordinate to a raw y value.
     * @param scy The Panel y coordinate
     * @return A raw y value.
     */
    private double convertToAttribY(double scy) {
      double temp = m_YaxisEnd - m_YaxisStart;
      double temp2 = ((scy - m_YaxisEnd) * (m_maxY - m_minY)) / temp;
      
      temp2 = -(temp2 - m_minY);
      
      return temp2;
    }
    //////
    
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
	//	gx.drawLine((int)(x-size),(int)(y-size+1),
	//    (int)(x+size),(int)(y+size+1));
	
	//gx.drawLine((int)(x-size),(int)(y-size-1),
	//    (int)(x+size+1),(int)(y+size));
	//gx.drawLine((int)(x-size-1),(int)(y-size),
		    //    (int)(x+size),(int)(y+size+1));
	
	gx.drawLine((int)(x+size),(int)(y-size),
		    (int)(x-size),(int)(y+size));
	//	gx.drawLine((int)(x+size),(int)(y-size+1),
	//    (int)(x-size),(int)(y+size+1));
	//gx.drawLine((int)(x+size),(int)(y-size-1),
	//    (int)(x-size-1),(int)(y+size));
	//gx.drawLine((int)(x+size+1),(int)(y-size),
	//    (int)(x-size),(int)(y+size+1));
	break;
      case 1: 
	gx.drawRect((int)(x-size),(int)(y-size),(size*2),(size*2));
	break;
	
      case 2:
	//gx.fillOval((int)(x-size),(int)(y-size),(size*2),(size*2));
	gx.drawLine((int)x, (int)y, (int)x, (int)y);
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
	  err = Math.abs(m_preds[jj] - m_plotInstances.instance(jj).
			 value(cind));
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
      if (m_extInstances != null) {
	for (int noa =0; noa < m_extInstances.numInstances(); noa++) {
	  if (m_extInstances.instance(noa).isMissing(m_xIndex) ||
	      m_extInstances.instance(noa).isMissing(m_yIndex)) {
	    m_extPointLookup[noa][0] = Double.NEGATIVE_INFINITY;
	    m_extPointLookup[noa][1] = Double.NEGATIVE_INFINITY;
	    m_extPointLookup[noa][2] = Double.NEGATIVE_INFINITY;
	  }
	  else {
	    double x = convertToPanelX(m_extInstances.instance(noa).
				       value(m_xIndex));
	    double y = convertToPanelY(m_extInstances.instance(noa).
				       value(m_yIndex));
	    m_extPointLookup[noa][0] = x;
	    m_extPointLookup[noa][1] = y;
	    m_extPointLookup[noa][2] = 3;
	  }
	}
	
      }
    }
    
    /**
     * This will draw the shapes created onto the panel.
     * For best visual, this should be the first thing to be drawn
     * (and it currently is).
     * @param gx The graphics context.
     */
    private void drawShapes(Graphics gx) {
      //FastVector tmp = m_plot.getShapes();
      
      if (m_shapes != null) {
	FastVector stmp;
	int x1, y1, x2, y2;
	for (int noa = 0; noa < m_shapes.size(); noa++) {
	  stmp = (FastVector)m_shapes.elementAt(noa);
	  if (((Double)stmp.elementAt(0)).intValue() == 1) {
	    //then rectangle
	    x1 = (int)convertToPanelX(((Double)stmp.elementAt(1)).
				      doubleValue());
	    y1 = (int)convertToPanelY(((Double)stmp.elementAt(2)).
				      doubleValue());
	    x2 = (int)convertToPanelX(((Double)stmp.elementAt(3)).
				      doubleValue());
	    y2 = (int)convertToPanelY(((Double)stmp.elementAt(4)).
				      doubleValue());
	    
	    gx.setColor(Color.gray);
	    gx.fillRect(x1, y1, x2 - x1, y2 - y1);
	    gx.setColor(Color.black);
	    gx.drawRect(x1, y1, x2 - x1, y2 - y1);
	    
	  }
	  else if (((Double)stmp.elementAt(0)).intValue() == 2) {
	    //then polygon
	    int[] ar1, ar2;
	    ar1 = getXCoords(stmp);
	    ar2 = getYCoords(stmp);
	    gx.setColor(Color.gray);
	    gx.fillPolygon(ar1, ar2, (stmp.size() - 1) / 2); 
	    gx.setColor(Color.black);
	    gx.drawPolyline(ar1, ar2, (stmp.size() - 1) / 2);
	  }
	  else if (((Double)stmp.elementAt(0)).intValue() == 3) {
	    //then polyline
	    int[] ar1, ar2;
	    FastVector tmp = makePolygon(stmp);
	    ar1 = getXCoords(tmp);
	    ar2 = getYCoords(tmp);
	    
	    gx.setColor(Color.gray);
	    gx.fillPolygon(ar1, ar2, (tmp.size() - 1) / 2);
	    gx.setColor(Color.black);
	    gx.drawPolyline(ar1, ar2, (tmp.size() - 1) / 2);
	  }
	}
      }
      
      if (m_shapePoints != null) {
	//then the current image needs to be refreshed
	if (((Double)m_shapePoints.elementAt(0)).intValue() == 2 ||
	    ((Double)m_shapePoints.elementAt(0)).intValue() == 3) {
	  gx.setColor(Color.black);
	  gx.setXORMode(Color.white);
	  int[] ar1, ar2;
	  ar1 = getXCoords(m_shapePoints);
	  ar2 = getYCoords(m_shapePoints);
	  gx.drawPolyline(ar1, ar2, (m_shapePoints.size() - 1) / 2);
	  m_newMousePos.width = (int)Math.ceil
	    (convertToPanelX(((Double)m_shapePoints.elementAt
			      (m_shapePoints.size() - 2)).doubleValue()));
	  
	  m_newMousePos.height = (int)Math.ceil
	    (convertToPanelY(((Double)m_shapePoints.elementAt
			      (m_shapePoints.size() - 1)).doubleValue()));
	  
	  gx.drawLine((int)Math.ceil
		      (convertToPanelX(((Double)m_shapePoints.elementAt
					(m_shapePoints.size() - 2)).
				       doubleValue())),
		      (int)Math.ceil(convertToPanelY
				     (((Double)m_shapePoints.elementAt
				       (m_shapePoints.size() - 1)).
				      doubleValue())),
		      m_newMousePos.width, m_newMousePos.height);
	  gx.setPaintMode();
	}
      }
      
    }
    
    /**
     * This is called for polylines to see where there two lines that
     * extend to infinity cut the border of the view.
     * @param x1 an x point along the line
     * @param y1 the accompanying y point.
     * @param x2 The x coord of the end point of the line.
     * @param y2 The y coord of the end point of the line.
     * @param x 0 or the width of the border line if it has one.
     * @param y 0 or the height of the border line if it has one.
     * @param offset The offset for the border line (either for x or y
     * dependant on which one doesn't change).
     * @return double array that contains the coordinate for the point 
     * that the polyline cuts the border (which ever side that may be).
     */
    private double[] lineIntersect(double x1, double y1, double x2, double y2, 
				   double x, double y, double offset) {
      //the first 4 params are thestart and end points of a line
      //the next param is either 0 for no change in x or change in x, 
      //the next param is the same for y
      //the final 1 is the offset for either x or y (which ever has no change)
      double xval;
      double yval;
      double xn = -100, yn = -100;
      double change;
      if (x == 0) {
	if ((x1 <= offset && offset < x2) || (x1 >= offset && offset > x2)) {
	  //then continue
	  xval = x1 - x2;
	  change = (offset - x2) / xval;
	  yn = (y1 - y2) * change + y2;
	  if (0 <= yn && yn <= y) {
	    //then good
	    xn = offset;
	  }
	  else {
	    //no intersect
	    xn = -100;
	  }
	}
      }
      else if (y == 0) {
	if ((y1 <= offset && offset < y2) || (y1 >= offset && offset > y2)) {
	  //the continue
	  yval = (y1 - y2);
	  change = (offset - y2) / yval;
	  xn = (x1 - x2) * change + x2;
	  if (0 <= xn && xn <= x) {
	    //then good
	    yn = offset;
	  }
	  else {
	    xn = -100;
	  }
	}
      }
      double[] ret = new double[2];
      ret[0] = xn;
      ret[1] = yn;
      return ret;
    }


    /**
     * This will convert a polyline to a polygon for drawing purposes
     * So that I can simply use the polygon drawing function.
     * @param v The polyline to convert.
     * @return A FastVector containing the polygon.
     */
    private FastVector makePolygon(FastVector v) {
      FastVector building = new FastVector(v.size() + 10);
      double x1, y1, x2, y2;
      int edge1 = 0, edge2 = 0;
      for (int noa = 0; noa < v.size() - 2; noa++) {
	building.addElement(new Double(((Double)v.elementAt(noa)).
				       doubleValue()));
      }
      
      //now clip the lines
      double[] new_coords;
      //note lineIntersect , expects the values to have been converted to 
      //screen coords
      //note the first point passed is the one that gets shifted.
      x1 = convertToPanelX(((Double)v.elementAt(1)).doubleValue());
      y1 = convertToPanelY(((Double)v.elementAt(2)).doubleValue());
      x2 = convertToPanelX(((Double)v.elementAt(3)).doubleValue());
      y2 = convertToPanelY(((Double)v.elementAt(4)).doubleValue());

      if (x1 < 0) {
	//test left
	new_coords = lineIntersect(x1, y1, x2, y2, 0, getHeight(), 0);
	edge1 = 0;
	if (new_coords[0] < 0) {
	  //then not left
	  if (y1 < 0) {
	    //test top
	    new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 0);
	    edge1 = 1;
	  }
	  else {
	    //test bottom
	    new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 
				       getHeight());
	    edge1 = 3;
	  }
	}
      }
      else if (x1 > getWidth()) {
	//test right
	new_coords = lineIntersect(x1, y1, x2, y2, 0, getHeight(), 
				   getWidth());
	edge1 = 2;
	if (new_coords[0] < 0) {
	  //then not right
	  if (y1 < 0) {
	    //test top
	    new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 0);
	    edge1 = 1;
	  }
	  else {
	    //test bottom
	    new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 
				       getHeight());
	    edge1 = 3;
	  }
	}
      }
      else if (y1 < 0) {
	//test top
	new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 0);
	edge1 = 1;
      }
      else {
	//test bottom
	new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 
				   getHeight());
	edge1 = 3;
      }
      
      building.setElementAt(new Double(convertToAttribX(new_coords[0])), 1);
      building.setElementAt(new Double(convertToAttribY(new_coords[1])), 2);

      x1 = convertToPanelX(((Double)v.elementAt(v.size() - 4)).doubleValue());
      y1 = convertToPanelY(((Double)v.elementAt(v.size() - 3)).doubleValue());
      x2 = convertToPanelX(((Double)v.elementAt(v.size() - 6)).doubleValue());
      y2 = convertToPanelY(((Double)v.elementAt(v.size() - 5)).doubleValue());
      
      if (x1 < 0) {
	//test left
	new_coords = lineIntersect(x1, y1, x2, y2, 0, getHeight(), 0);
	edge2 = 0;
	if (new_coords[0] < 0) {
	  //then not left
	  if (y1 < 0) {
	    //test top
	    new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 0);
	    edge2 = 1;
	  }
	  else {
	    //test bottom
	    new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 
				       getHeight());
	    edge2 = 3;
	  }
	}
      }
      else if (x1 > getWidth()) {
	//test right
	new_coords = lineIntersect(x1, y1, x2, y2, 0, getHeight(), 
				   getWidth());
	edge2 = 2;
	if (new_coords[0] < 0) {
	  //then not right
	  if (y1 < 0) {
	    //test top
	    new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 0);
	    edge2 = 1;
	  }
	  else {
	    //test bottom
	    new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 
				       getHeight());
	    edge2 = 3;
	  }
	}
      }
      else if (y1 < 0) {
	//test top
	new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 0);
	edge2 = 1;
      }
      else {
	//test bottom
	new_coords = lineIntersect(x1, y1, x2, y2, getWidth(), 0, 
				   getHeight());
	edge2 = 3;
      }
      
      building.setElementAt(new Double(convertToAttribX(new_coords[0])), 
			    building.size() - 2);
      building.setElementAt(new Double(convertToAttribY(new_coords[1])), 
			    building.size() - 1);
      

      //trust me this complicated piece of code will
      //determine what points on the boundary of the view to add to the polygon
      int xp, yp;

      xp = getWidth() * ((edge2 & 1) ^ ((edge2 & 2) / 2));
      yp = getHeight() * ((edge2 & 2) / 2);
      //System.out.println(((-1 + 4) % 4) + " hoi");
      
      if (inPolyline(v, convertToAttribX(xp), convertToAttribY(yp))) {
	//then add points in a clockwise direction
	building.addElement(new Double(convertToAttribX(xp)));
	building.addElement(new Double(convertToAttribY(yp)));
	for (int noa = (edge2 + 1) % 4; noa != edge1; noa = (noa + 1) % 4) {
	  xp = getWidth() * ((noa & 1) ^ ((noa & 2) / 2));
	  yp = getHeight() * ((noa & 2) / 2);
	  building.addElement(new Double(convertToAttribX(xp)));
	  building.addElement(new Double(convertToAttribY(yp)));
	}
      }
      else {
	xp = getWidth() * ((edge2 & 2) / 2);
	yp = getHeight() * (1 & ~((edge2 & 1) ^ ((edge2 & 2) / 2)));
	if (inPolyline(v, convertToAttribX(xp), convertToAttribY(yp))) {
	  //then add points in anticlockwise direction
	  building.addElement(new Double(convertToAttribX(xp)));
	  building.addElement(new Double(convertToAttribY(yp)));
	  for (int noa = (edge2 + 3) % 4; noa != edge1; noa = (noa + 3) % 4) {
	    xp = getWidth() * ((noa & 2) / 2);
	    yp = getHeight() * (1 & ~((noa & 1) ^ ((noa & 2) / 2)));
	    building.addElement(new Double(convertToAttribX(xp)));
	    building.addElement(new Double(convertToAttribY(yp)));
	  }
	}
      }
      return building;
    }

    /**
     * This will extract from a polygon shape its x coodrdinates
     * so that an awt.Polygon can be created.
     * @param v The polygon shape.
     * @return an int array containing the screen x coords for the polygon.
     */
    private int[] getXCoords(FastVector v) {
      int cach = (v.size() - 1) / 2;
      int[] ar = new int[cach];
      for (int noa = 0; noa < cach; noa ++) {
	ar[noa] = (int)convertToPanelX(((Double)v.elementAt(noa * 2 + 1)).
				       doubleValue());
      }
      return ar;
    }

    /**
     * This will extract from a polygon shape its y coordinates
     * so that an awt.Polygon can be created.
     * @param v The polygon shape.
     * @return an int array containing the screen y coords for the polygon.
     */
    private int[] getYCoords(FastVector v) {
      int cach = (v.size() - 1) / 2;
      int[] ar = new int[cach];
      for (int noa = 0; noa < cach; noa ++) {
	ar[noa] = (int)convertToPanelY(((Double)v.elementAt(noa * 2 + 2)).
				       doubleValue());
      }
      return ar;
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
	  err = Math.abs(m_preds[jj] - m_plotInstances.instance(jj).
	  value(cind));
	  if (err < minErr) {
	    minErr = err;
	  }
	  if (err > maxErr) {
	    maxErr = err;
	  }
	}
	} */

      if (m_extInstances != null) {
	for (int noa = 0; noa < m_extInstances.numInstances(); noa++) {
	  if (m_extInstances.instance(noa).isMissing(m_xIndex) ||
	      m_extInstances.instance(noa).isMissing(m_yIndex)) {
	  }
	  else {
	    double x = m_extPointLookup[noa][0];
	    double y = m_extPointLookup[noa][1];
	    
	    int x_range = (int)x - m_XaxisStart;
	    int y_range = (int)y - m_YaxisStart;
	    
	    if (x_range >= 0 && y_range >= 0) {
	      if (m_extInstances.attribute(m_cIndex).isNominal()) {
		int ci = (int)m_extInstances.instance(noa).
		  value(m_cIndex);
		
		gx.setColor((Color)m_colorList.elementAt(ci));	    
		drawDataPoint(x,y,2,2,gx);
	      }
	      else {
		double r = (m_extInstances.instance(noa).
			    value(m_cIndex) - m_minC) /
		  (m_maxC - m_minC);
		r = (r * 240) + 15;
		gx.setColor(new Color((int)r,150,(int)(255-r)));
		drawDataPoint(x,y,2,2,gx);
		
	      }
	    }
	  }
	}
      }
      
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

	  int x_range = (int)x - m_XaxisStart;
	  int y_range = (int)y - m_YaxisStart;
	  if (x_range >= 0 && y_range >= 0) {
	    if (m_drawnPoints[x_range][y_range] == i 
		|| m_drawnPoints[x_range][y_range] == 0) {
	      m_drawnPoints[x_range][y_range] = i;
	      if (m_plotInstances.attribute(m_cIndex).isNominal() || 
		  m_colourUsingPreds) {
		
		// if the cIndex is less than 0 then we are visualizing
		// a clusterer's predictions and therefore the dataset has
		// no apriori class to use for colouring
		/*int ci = (!m_colourUsingPreds) 
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
		*/
		
		int ci = (!m_colourUsingPreds)
		  ? (int)m_plotInstances.instance(i).value(m_cIndex)
		  : (int)m_preds[i];

		gx.setColor((Color)m_colorList.elementAt(ci));	    
		drawDataPoint(x,y,2,0,gx);
		if (m_preds != null 
		    && !m_predsNumeric 
		    && (m_plotInstances.classIndex() >= 0)) {
		  if (m_preds[i] != 
		      m_plotInstances.instance(i).
		      value(m_plotInstances.classIndex())) {
		    /*ci = (int)(m_preds[i] % 10);
		    mp = (int)(m_preds[i] / 10);
		    mp *= 2;
		    pc = m_DefaultColors[ci];
		    for (int j=0;j<mp;j++) {
		      pc = pc.darker();
		    }
		    */
		    
		    gx.setColor((Color)m_colorList.elementAt((int)m_preds[i]));
		    drawDataPoint(x,y,(int)m_pointLookup[i][2],1,gx);
		    //		gx.drawRect((int)(x-3),(int)(y-3),6,6);
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
			      err = ((err - minErr)/(maxErr - minErr)) * 
			      maxpSize; */
		  double r = (m_plotInstances.instance(i).
			      value(m_cIndex) - m_minC) /
		    (m_maxC - m_minC);
		  r = (r * 240) + 15;
		  gx.setColor(new Color((int)r,150,(int)(255-r)));
		  drawDataPoint(x,y,(int)m_pointLookup[i][2],0,gx);
		} else {
		  double r = (m_plotInstances.instance(i).value(m_cIndex) - 
			      m_minC) /
		    (m_maxC - m_minC);
		  r = (r * 240) + 15;
		  gx.setColor(new Color((int)r,150,(int)(255-r)));
		  drawDataPoint(x,y,2,0,gx);
		}
	      }
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

      //I'm putting my draw shapes here because they rely 
      //on the m_axis values but also need to be painted first
      drawShapes(gx);

      // draw the class legend
      m_attrib.setColor(m_cIndex, m_maxC, m_minC);
      if ((!m_colourUsingPreds) && 
	  m_plotInstances.attribute(m_cIndex).isNumeric()) {

	m_classPanel.setNumeric(m_maxC, m_minC);
      } else { //if ((!m_colourUsingPreds) && 
	//m_plotInstances.attribute(m_cIndex).isNominal()) {
	m_classPanel.setNominal(m_plotInstances, m_cIndex);
	newColorAttribute(m_cIndex, m_plotInstances);
      }

      // draw axis
      gx.setColor(Color.green);
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
	  int x_range = m_XaxisEnd - m_XaxisStart;
	  int y_range = m_YaxisEnd - m_YaxisStart;
	  if (x_range < 10) {
	    x_range = 10;
	  }
	  if (y_range < 10) {
	    y_range = 10;
	  }
	  m_drawnPoints = new int[x_range + 1][y_range + 1];
	  fillLookup();
	}
	paintData(gx);
      }
    }
  }

  /** default colours for colouring discrete class */
  protected Color [] m_DefaultColors = {Color.blue,
					Color.red,
					Color.green,
					Color.cyan,
					Color.pink,
					new Color(255, 0, 255),
					Color.orange,
					new Color(255, 0, 0),
					new Color(0, 255, 0),
					Color.white};


  /** Lets the user select the attribute for the x axis */
  protected JComboBox m_XCombo = new JComboBox();

  /** Lets the user select the attribute for the y axis */
  protected JComboBox m_YCombo = new JComboBox();

  /** Lets the user select the attribute to use for colouring */
  protected JComboBox m_ColourCombo = new JComboBox();
  
  /** Lets the user select the shape they want to create for instance 
   * selection. */
  protected JComboBox m_ShapeCombo = new JComboBox();

  /** Button for the user to enter the splits. */
  protected JButton m_submit = new JButton("Submit");
  
  /** Button for the user to remove all splits. */
  protected JButton m_cancel = new JButton("Clear");

  /** Label for the jitter slider */
  protected JLabel m_JitterLab= new JLabel("Jitter",SwingConstants.RIGHT);

  /** The jitter slider */
  protected JSlider m_Jitter = new JSlider(0,50,0);

  /** The panel that displays the plot */
  protected PlotPanel m_plot = new PlotPanel();

  /** The panel that displays the attributes , using color to represent 
   * another attribute. */
  protected AttributePanel m_attrib = new AttributePanel();

  /** Panel that surrounds the plot panel with a titled border */
  protected JPanel m_plotSurround = new JPanel();

  /** An optional listener that we will inform when ComboBox selections
      change */
  protected ActionListener listener = null;

  /** An Instances object that contains instances that normally wouldn't
   * reach that node. */
  protected Instances m_extInstances = null;

  /** An optional listener that we will inform when the user creates a 
   * split to seperate instances. */
  protected VisualizePanelListener m_splitListener = null;

  /** The name of the plot (not currently displayed, but can be used
      in the containing Frame or Panel) */
  protected String m_plotName = "";

  /** The panel that displays the legend for the colouring attribute */
  protected ClassPanel m_classPanel = new ClassPanel();
  
  /** The list of the colors used */
  protected FastVector m_colorList;

  /** This constructor allows a VisualizePanelListener to be set. */
  public VisualizePanel(VisualizePanelListener ls) {
    this();
    m_splitListener = ls;
  }

  /**
   * Constructor
   */
  public VisualizePanel() {
    m_XCombo.setToolTipText("Select the attribute for the x axis");
    m_YCombo.setToolTipText("Select the attribute for the y axis");
    m_ColourCombo.setToolTipText("Select the attribute to colour on");
    m_ShapeCombo.setToolTipText("Select the shape to use for data selection"); 
    //////////
    m_XCombo.setEnabled(false);
    m_YCombo.setEnabled(false);
    m_ColourCombo.setEnabled(false);
    m_ShapeCombo.setEnabled(false);

    m_colorList = new FastVector(10);
    for (int noa = m_colorList.size(); noa < 10; noa++) {
      Color pc = m_DefaultColors[noa % 10];
      int ija =  noa / 10;
      ija *= 2; 
      for (int j=0;j<ija;j++) {
	pc = pc.darker();
      }
      
      m_colorList.addElement(pc);
    }
    
    m_XCombo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  int selected = m_XCombo.getSelectedIndex();
	  if (selected < 0) {
	    selected = 0;
	  }
	  m_plot.setXindex(selected);
	  m_plot.determineBounds();
	  m_attrib.setX(selected);
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
	  m_attrib.setY(selected);
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
    
    ///////
    m_ShapeCombo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  int selected = m_ShapeCombo.getSelectedIndex();
	  if (selected < 0) {
	    selected = 0;
	  }
	  m_plot.setSindex(selected);
	  // try sending on the event if anyone is listening
	  if (listener != null) {
	    listener.actionPerformed(e);
	  }
	}
      });


    /*  m_submit.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  if (m_splitListener != null)
	    {
	      //then tell the classifier that a new split has been made
	      m_splitListener.userDataEvent(new VisualizePanelEvent(
	      }*/
    ///////////////////////////////////////

    m_Jitter.addChangeListener(new ChangeListener() {
	public void stateChanged(ChangeEvent e) {
	  m_plot.setJitter(m_Jitter.getValue());
	}
      });
    
    JPanel combos = new JPanel();
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints constraints = new GridBagConstraints();


    m_XCombo.setLightWeightPopupEnabled(false);
    m_YCombo.setLightWeightPopupEnabled(false);
    m_ColourCombo.setLightWeightPopupEnabled(false);
    m_ShapeCombo.setLightWeightPopupEnabled(false);
    combos.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    //    combos.setLayout(new GridLayout(1,3,5,5));
    combos.setLayout(gb);
    constraints.gridx=0;constraints.gridy=0;constraints.weightx=5;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridwidth=2;constraints.gridheight=1;
    constraints.insets = new Insets(0,2,0,2);
    combos.add(m_XCombo,constraints);
    constraints.gridx=2;constraints.gridy=0;constraints.weightx=5;
    constraints.gridwidth=2;constraints.gridheight=1;
    combos.add(m_YCombo,constraints);
    constraints.gridx=0;constraints.gridy=1;constraints.weightx=5;
    constraints.gridwidth=2;constraints.gridheight=1;
    combos.add(m_ColourCombo,constraints);
    //
    constraints.gridx=2;constraints.gridy=1;constraints.weightx=5;
    constraints.gridwidth=2;constraints.gridheight=1;
    combos.add(m_ShapeCombo,constraints);
    
    constraints.gridx=0;constraints.gridy=2;constraints.weightx=5;
    constraints.gridwidth=1;constraints.gridheight=1;
    combos.add(m_submit, constraints);
    
    constraints.gridx=1;constraints.gridy=2;constraints.weightx=5;
    constraints.gridwidth=1;constraints.gridheight=1;
    combos.add(m_cancel, constraints);

    ////////////////////////////////
    constraints.gridx=2;constraints.gridy=2;constraints.weightx=5;
    constraints.insets = new Insets(10,0,0,5);
    combos.add(m_JitterLab,constraints);
    constraints.gridx=3;constraints.gridy=2;
    constraints.weightx=5;
    constraints.insets = new Insets(10,0,0,0);
    combos.add(m_Jitter,constraints);

    JPanel cp = new JPanel();
    cp.setBorder(BorderFactory.createTitledBorder("Class colour")); 
    cp.setLayout(new BorderLayout());

    m_classPanel.setBorder(BorderFactory.createEmptyBorder(15,10,10,10));
    cp.add(m_classPanel, BorderLayout.CENTER);



    GridBagLayout gb2 = new GridBagLayout();
    m_plotSurround.setBorder(BorderFactory.createTitledBorder("Plot"));
    m_plotSurround.setLayout(gb2);
    //m_plotSurround.add(m_plot, BorderLayout.CENTER);
    //m_plotSurround.add(m_attrib, BorderLayout.CENTER);
    constraints.fill = constraints.BOTH;
    constraints.insets = new Insets(0, 0, 0, 10);
    constraints.gridx=0;constraints.gridy=0;constraints.weightx=3;
    constraints.gridwidth=4;constraints.gridheight=1;constraints.weighty=5;
    m_plotSurround.add(m_plot, constraints);
    
    constraints.insets = new Insets(0, 0, 0, 0);
    constraints.gridx=4;constraints.gridy=0;constraints.weightx=1;
    constraints.gridwidth=1;constraints.gridheight=1;constraints.weighty=5;
    m_plotSurround.add(m_attrib, constraints);

    

    setLayout(new BorderLayout());
    add(combos, BorderLayout.NORTH);
    add(m_plotSurround, BorderLayout.CENTER);
    add(cp, BorderLayout.SOUTH);

    
    String [] SNames = new String [4];
    SNames[0] = "Select Instance";
    SNames[1] = "Rectangle";
    SNames[2] = "Polygon";
    SNames[3] = "Polyline";

    m_ShapeCombo.setModel(new DefaultComboBoxModel(SNames));
    m_ShapeCombo.setEnabled(true);

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
      int highest = -1;
      for (int noa = 0; noa < m_attrib.m_preds.length; noa++) {
	if (m_attrib.m_preds[noa] > highest) {
	  highest = (int)m_attrib.m_preds[noa];
	}
      }
   
      for (int noa = m_colorList.size(); noa <= highest;noa++) {
	Color pc = m_DefaultColors[noa % 10];
	int ija =  noa / 10;
	ija *= 2; 
	for (int j=0;j<ija;j++) {
	  pc = pc.brighter();
	}
	
	m_colorList.addElement(pc);
      }
      
       
      m_plot.m_colourUsingPreds = true;
      m_attrib.m_colourUsingPreds = true;
      m_classPanel.m_colourUsingPreds = true;
      m_classPanel.m_numPreds = highest+1;
    }
    m_ColourCombo.setEnabled(false);
  }
  
  
  /**
   * Set the index of the attribute for the x axis 
   * @param index the index for the x axis
   * @exception if index is out of range.
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
   * @exception if index is out of range.
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
   * Get the index of the shape selected for creating splits.
   * @return The index of the shape.
   */
  public int getSIndex() {
    return m_ShapeCombo.getSelectedIndex();
  }
  
  /** 
   * Set the shape for creating splits.
   * @param index The index of the shape.
   * @exception If s index is out of range.
   */
  public void setSIndex(int index) throws Exception {
    if (index >= 0 && index < m_ShapeCombo.getItemCount()) {
      m_ShapeCombo.setSelectedIndex(index);
    }
    else {
      throw new Exception("s index is out of range!");
    }
  }

  /**
   * Set the classifier's predictions. These must correspond one to one with
   * the instances provided using the setInstances method.
   * @param preds an array of predictions
   */
  public void setPredictions(double [] preds) {
    m_plot.setPredictions(preds);
    m_attrib.setPredictions(preds);
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
   * Sets the Colors in use for a different attrib
   * if it is not a nominal attrib and or does not have
   * more possible values then this will do nothing.
   * otherwise it will add default colors to see that
   * there is a color for the attrib to begin with.
   * @param a The index of the attribute to color.
   * @param i The instances object that contains the attribute.
   */
  protected void newColorAttribute(int a, Instances i) {
    if (i.attribute(a).isNominal()) {
      for (int noa = m_colorList.size(); noa < i.attribute(a).numValues();
	   noa++) {
	Color pc = m_DefaultColors[noa % 10];
	int ija =  noa / 10;
	ija *= 2; 
	for (int j=0;j<ija;j++) {
	  pc = pc.brighter();
	}
	
	m_colorList.addElement(pc);
      }
    }
  }


  /**
   * This will set the shapes for the instances.
   * @param l A list of the shapes, providing that
   * the objects in the lists are non editable the data will be
   * kept intact.
   */
  public void setShapes(FastVector l) {
    m_plot.setShapes(l);
  }

  /** 
   * Tells the panel to add some extra instances.
   * @param inst a set of Instances
   */
  public void setExtInstances(Instances inst) {
    //note this may allow some instances to occur multiple times
    //(this is bad);

    if (m_extInstances == null) {
      m_extInstances = new Instances(inst, 200);
    }
    for (int noa = 0; noa < inst.numInstances(); noa++) {
      m_extInstances.add(inst.instance(noa));
    }
    m_plot.determineBounds();
  }

  /**
   * Tells the panel to use a new set of instances.
   * @param inst a set of Instances
   */
  public void setInstances(Instances inst) {
    newColorAttribute(inst.numAttributes()-1, inst);
    m_extInstances = null;
    m_plot.setInstances(inst, inst.numAttributes()-1);
    m_attrib.setInstances(inst);
    String [] XNames = new String [inst.numAttributes()];
    String [] YNames = new String [inst.numAttributes()];
    String [] CNames = new String [inst.numAttributes()];
    String [] SNames = new String [4];
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
    //m_ShapeCombo.setModel(new DefaultComboBoxModel(SNames));
    //m_ShapeCombo.setEnabled(true);
    m_XCombo.setEnabled(true);
    m_YCombo.setEnabled(true);
    
    if (!m_plot.m_colourUsingPreds && m_splitListener == null) {
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











