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
 *    AttributeVisualizationPanel.java
 *    Copyright (C) 2003 Ashraf M. Kibriya
 *
 */

package weka.gui;

import java.io.FileReader;
import java.util.Random;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
//import java.awt.Image;
//import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JComboBox;

import weka.core.Instances;
import weka.core.AttributeStats;
import weka.core.Utils;
import weka.core.FastVector;

/**
 * Creates a panel that shows a visualization of an
 * attribute in a dataset. For nominal attribute it
 * shows a bar plot, with each bar corresponding to 
 * each nominal value of the attribute with its height 
 * equal to the frequecy that value appears in the
 * dataset. For numeric attributes, it displays a 
 * histogram. The number of intervals in the 
 * histogram is 10% of the number of instances in 
 * the dataset, otherwise number of intervals  is 
 * equal to the width of this panel in pixels minus 6
 * (if that 10% figure is > width).
 *
 * @author Ashraf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @version $Revision: 1.10 $
 */

public class AttributeVisualizationPanel extends JPanel {

  Instances m_data;
  AttributeStats as;
  int attribIndex, maxValue;
  int histBarCounts[]; 
  int histBarClassCounts[][];
  double m_barRange;
  int classIndex;
  Thread hc; 
  boolean threadRun=false;
  JComboBox m_colorAttrib;
  FontMetrics fm;
  private Integer m_locker = new Integer(1);
  //Image img;

  /** Contains discrete colours for colouring for nominal attributes */
  private FastVector m_colorList = new FastVector();

  /** default colour list */
  private static final Color [] m_defaultColors = {Color.blue,
		 				   Color.red,
						   Color.cyan,
						   new Color(75, 123, 130),
						   Color.pink,
						   Color.green,
						   Color.orange,
						   new Color(255, 0, 255),
						   new Color(255, 0, 0),
						   new Color(0, 255, 0),
  };

  public AttributeVisualizationPanel() {
    this(false);
  }
  public AttributeVisualizationPanel(boolean showColouringOption) {
    this.setFont( new Font("Default", Font.PLAIN, 9) );
    fm = this.getFontMetrics( this.getFont() );
    this.setToolTipText("");
    FlowLayout fl= new FlowLayout(FlowLayout.LEFT);
    this.setLayout(fl);
    this.addComponentListener( new ComponentAdapter() {
	public void componentResized(ComponentEvent ce) {
	  if(m_data!=null)
	    calcGraph();
	}
      });

    m_colorAttrib = new JComboBox();
    m_colorAttrib.addItemListener( new ItemListener() {
	public void itemStateChanged(ItemEvent ie) {
	  if(ie.getStateChange()==ItemEvent.SELECTED) {
	    classIndex = m_colorAttrib.getSelectedIndex();
	    if (as != null) {
	      setAttribute(attribIndex);
	    }
	  }
	}
      });

    if(showColouringOption) {
      //m_colorAttrib.setVisible(false);
      this.add(m_colorAttrib);
      validate();
    }
  }

  /**
   * Sets the instances for use
   *
   * @param newins a set of Instances
   */
  public void setInstances(Instances newins) {
    attribIndex = 0;
    m_data = newins;
    as=null;
    if(m_colorAttrib!=null) {
      m_colorAttrib.removeAllItems();
      for(int i=0; i<m_data.numAttributes(); i++) {
	m_colorAttrib.addItem(new String("Colour: "+m_data.attribute(i).name()+" "+
					 ((m_data.attribute(i).isNominal()) ? "(Nom)":"(Num)")));
      }
      m_colorAttrib.setSelectedIndex(m_data.numAttributes()-1);  
      //if (m_data.classIndex() >= 0) {
      //    m_colorAttrib.setSelectedIndex(m_data.classIndex());
      //}
    }
    classIndex = m_data.numAttributes()-1;
    
    this.repaint();
  }

  public JComboBox getColorBox() {
    return m_colorAttrib;
  }

  /**
   * Get the coloring index for the plot
   *
   * @return an <code>int</code> value
   */
  public int getColoringIndex() {
    return classIndex; //m_colorAttrib.getSelectedIndex();
  }

  /**
   * Set the coloring index for the plot
   *
   * @param ci an <code>int</code> value
   */
  public void setColoringIndex(int ci) {
    classIndex = ci;
    if(m_colorAttrib!=null) 
      m_colorAttrib.setSelectedIndex(ci);
    else 
      setAttribute(attribIndex);
  }

  /**
   * Tells the panel which attribute to visualize.
   *
   * @param index The index of the attribute
   */
  public void setAttribute(int index) {

    synchronized (m_locker) {
      threadRun = true;
      //if(hc!=null && hc.isAlive()) hc.stop(); 
      attribIndex=index;
      as = m_data.attributeStats(attribIndex);
      //classIndex = m_colorAttrib.getSelectedIndex();
    }
    calcGraph();
  }
  
  /* 
   * Recalculates the bar widths and heights required to display 
   * the barplot or histogram. Required usually when the component
   * is resized.
   */
  public void calcGraph() {

    synchronized (m_locker) {
      threadRun = true;
      if(as.nominalCounts!=null) {
	hc = new BarCalc();
	hc.setPriority(hc.MIN_PRIORITY);
	hc.start();
      }
      else if(as.numericStats!=null) {
	hc = new HistCalc();
	hc.setPriority(hc.MIN_PRIORITY);
	hc.start();
      }
      //this.repaint();
    }
  }


  private class BarCalc extends Thread { 
    public void run() {
      synchronized (m_locker) {
	if(m_data.attribute(classIndex).isNominal()) {
	  int histClassCounts[][]; 
	  histClassCounts = new int[m_data.attribute(attribIndex).numValues()]
	    [m_data.attribute(classIndex).numValues()+1];
	  
	  maxValue = as.nominalCounts[0];
	  for(int i=0; i<m_data.attribute(attribIndex).numValues(); i++) { 
	    if(as.nominalCounts[i]>maxValue)
	      maxValue = as.nominalCounts[i];
	  }
	  
	  if(m_colorList.size()==0)
	    m_colorList.addElement(Color.black);
	  for(int i=m_colorList.size(); i<m_data.attribute(classIndex).numValues()+1; i++) {
	    Color pc = m_defaultColors[(i-1) % 10];
	    int ija =  (i-1) / 10;
	    ija *= 2;
	    
	    for (int j=0;j<ija;j++) {
	      pc = pc.darker();     
	    }
	    
	    m_colorList.addElement(pc);
	  }
	  
	  for(int k=0; k<m_data.numInstances(); k++) {
	    //System.out.println("attrib: "+m_data.instance(k).value(attribIndex)+
	    //		   " class: "+m_data.instance(k).value(classIndex));
	    if(!m_data.instance(k).isMissing(attribIndex))
	      if(m_data.instance(k).isMissing(classIndex))
		histClassCounts[(int)m_data.instance(k).value(attribIndex)][0]++;
	      else
		histClassCounts[(int)m_data.instance(k).value(attribIndex)][(int)m_data.instance(k).value(classIndex)+1]++;
	  }
	  
	  //for(int i=0; i<histClassCounts.length; i++) {
	  //int sum=0;
	  //for(int j=0; j<histClassCounts[i].length; j++) {
	  //    sum = sum+histClassCounts[i][j];
	  //}
	  //System.out.println("histCount: "+sum+" Actual: "+as.nominalCounts[i]);
	  //}
	  
	  threadRun=false;
	  histBarClassCounts = histClassCounts;
	  //Image tmpImg = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
	  //drawGraph( tmpImg.getGraphics() );
	  //img = tmpImg;
	  AttributeVisualizationPanel.this.repaint();
	  
	}
	else {
	  int histCounts[];
	  histCounts  = new int[m_data.attribute(attribIndex).numValues()];
	  
	  maxValue = as.nominalCounts[0];
	  for(int i=0; i<m_data.attribute(attribIndex).numValues(); i++) { 
	    if(as.nominalCounts[i]>maxValue)
	      maxValue = as.nominalCounts[i];
	  }
	  
	  for(int k=0; k<m_data.numInstances(); k++) {
	    histCounts[(int)m_data.instance(k).value(attribIndex)]++;
	  }
	  threadRun=false;
	  histBarCounts = histCounts;
	  //Image tmpImg = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
	  //drawGraph( tmpImg.getGraphics() );
	  //img = tmpImg;
	  AttributeVisualizationPanel.this.repaint();
	}
      }
    }
  }


  private class HistCalc extends Thread {
    public void run() {
      synchronized (m_locker) {
	if(m_data.attribute(classIndex).isNominal()) {
	  int tempVal = AttributeVisualizationPanel.this.getWidth()-4;
	  if (tempVal < 1) {
	    tempVal = 1;
	  }
	  int histClassCounts[][]  = (AttributeVisualizationPanel.this.getWidth()<(int)(as.totalCount*0.1)) ?
	    new int[tempVal][m_data.attribute(classIndex).numValues()+1] : 
	    new int[(int)(as.totalCount*0.1)][m_data.attribute(classIndex).numValues()+1];
	  double barRange   = (as.numericStats.max - as.numericStats.min)/(double)histClassCounts.length;
	  double currentBar = as.numericStats.min; // + barRange;
	  maxValue = 0;
	  
	  if(m_colorList.size()==0)
	    m_colorList.addElement(Color.black);
	  for(int i=m_colorList.size(); i<m_data.attribute(classIndex).numValues()+1; i++) {
	    Color pc = m_defaultColors[(i-1) % 10];
	    int ija =  (i-1) / 10;
	    ija *= 2; 
	    for (int j=0;j<ija;j++) {
	      pc = pc.darker();     
	    }
	    m_colorList.addElement(pc);
	  }
	  
	  for(int k=0; k<m_data.numInstances(); k++) {
	    int t=0;
	    try {
	      if(!m_data.instance(k).isMissing(attribIndex)) {
		t = (int) Math.ceil((float)((m_data.instance(k).value(attribIndex)-as.numericStats.min)/barRange));
		if(t==0) {
		  if(m_data.instance(k).isMissing(classIndex))
		    histClassCounts[t][0]++;
		  else
		    histClassCounts[t][(int)m_data.instance(k).value(classIndex)+1]++;
		  //if(histCounts[t]>maxValue)
		  //  maxValue = histCounts[t];
		}
		else {
		  if(m_data.instance(k).isMissing(classIndex))
		    histClassCounts[t-1][0]++;
		  else
		    histClassCounts[t-1][(int)m_data.instance(k).value(classIndex)+1]++;
		  //if(histCounts[t-1]>maxValue)
		  //  maxValue = histCounts[t-1];
		}
	      }
	    }
	    catch(ArrayIndexOutOfBoundsException ae) { 
	      System.out.println("t:"+(t)+
				 " barRange:"+barRange+
				 " histLength:"+histClassCounts.length+
				 " value:"+m_data.instance(k).value(attribIndex)+
				 " min:"+as.numericStats.min+
				 " sumResult:"+(m_data.instance(k).value(attribIndex)-as.numericStats.min)+
				 " divideResult:"+(float)((m_data.instance(k).value(attribIndex)-as.numericStats.min)/barRange)+
				 " finalResult:"+
				 Math.ceil((float)((m_data.instance(k).value(attribIndex)-as.numericStats.min)/barRange)) ); }
	  }
	  for(int i=0; i<histClassCounts.length; i++) {
	    int sum=0;
	    for(int j=0; j<histClassCounts[i].length; j++) 
	      sum = sum+histClassCounts[i][j];
	    if(maxValue<sum)
	      maxValue = sum;
	  }
	  histBarClassCounts = histClassCounts;
	  m_barRange =  barRange;
	  
	}
	else { //else if the class attribute is numeric
	  int tempVal = AttributeVisualizationPanel.this.getWidth()-4;
	  if (tempVal < 1) {
	    tempVal = 1;
	  }
	  int histCounts[]  = (AttributeVisualizationPanel.this.getWidth()<(int)(as.totalCount*0.1)) ?
	    new int[tempVal] : new int[(int)(as.totalCount*0.1)];
	  double barRange   = (as.numericStats.max - as.numericStats.min)/(double)histCounts.length;
	  double currentBar = as.numericStats.min; // + barRange;
	  maxValue = 0;
	  
	  for(int k=0; k<m_data.numInstances(); k++) {
	    int t=0;
	    try {
	      t = (int) Math.ceil((float)((m_data.instance(k).value(attribIndex)-as.numericStats.min)/barRange));
	      if(t==0) {
		histCounts[t]++;
		if(histCounts[t]>maxValue)
		  maxValue = histCounts[t];
	      }
	      else {
		histCounts[t-1]++;
		if(histCounts[t-1]>maxValue)
		  maxValue = histCounts[t-1];
	      }
	    }
	    catch(ArrayIndexOutOfBoundsException ae) { 
	      ae.printStackTrace();
	      System.out.println("t:"+(t)+
				 " barRange:"+barRange+
				 " histLength:"+histCounts.length+
				 " value:"+m_data.instance(k).value(attribIndex)+
				 " min:"+as.numericStats.min+
				 " sumResult:"+(m_data.instance(k).value(attribIndex)-as.numericStats.min)+
				 " divideResult:"+(float)((m_data.instance(k).value(attribIndex)-as.numericStats.min)/barRange)+
				 " finalResult:"+
				 Math.ceil((float)((m_data.instance(k).value(attribIndex)-as.numericStats.min)/barRange)) ); }
	  }
	  histBarCounts = histCounts;
	  m_barRange =  barRange;
	}
	
	threadRun=false;
	//Image tmpImg = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
	//drawGraph( tmpImg.getGraphics() );
	//img = tmpImg;
	AttributeVisualizationPanel.this.repaint();
      }
    }
  }


  /**
   * Returns "&lt;nominal value&gt; [&lt;nominal value count&gt;]"
   * if displaying a bar plot and mouse is on some bar.
   * If displaying histogram then it 
   *     <li>returns "count &lt;br&gt; [&lt;bars Range&gt;]" if mouse is 
   *     on the first bar. </li>
   *     <li>returns "count &lt;br&gt; (&lt;bar's Range&gt;]" if mouse is 
   *     on some bar other than the first one. </li>
   * Otherwise it returns ""
   *
   * @param ev The mouse event
   */
  public String getToolTipText(MouseEvent ev) {
    if(as!=null && as.nominalCounts!=null) {
      float intervalWidth = this.getWidth()/(float)as.nominalCounts.length, heightRatio;
      int barWidth, x=0, y=0;
      if(intervalWidth>5)
	barWidth = (int)Math.floor(intervalWidth*0.8F);
      else
	barWidth = 1;

      x = x + (int)( (Math.floor(intervalWidth*0.1F))<1 ? 1:(Math.floor(intervalWidth*0.1F)) );
      if(  this.getWidth() - 
	   (x + as.nominalCounts.length*barWidth 
	    +(int)( (Math.floor(intervalWidth*0.2F))<1 ? 1:(Math.floor(intervalWidth*0.2F)) )*as.nominalCounts.length) > 5 )
	x += (this.getWidth() - 
	      (x + as.nominalCounts.length*barWidth + 
	       (int)( (Math.floor(intervalWidth*0.2F))<1 ? 1:(Math.floor(intervalWidth*0.2F)) )*as.nominalCounts.length))/2;
      for(int i=0; i<as.nominalCounts.length; i++) {
	heightRatio = (this.getHeight()-(float)fm.getHeight())/maxValue;
	y = this.getHeight()-Math.round(as.nominalCounts[i]*heightRatio);
	if(ev.getX()>=x && ev.getX()<=x+barWidth
	   && ev.getY()>=this.getHeight()-Math.round(as.nominalCounts[i]*heightRatio) )
	  return(m_data.attribute(attribIndex).value(i)+" ["+as.nominalCounts[i]+"]");
	x = x+barWidth+(int)( (Math.floor(intervalWidth*0.2F))<1 ? 1:(Math.floor(intervalWidth*0.2F)) );
      }
    }
    else if(threadRun==false && (histBarCounts!=null || histBarClassCounts!=null)) {
      float heightRatio, intervalWidth;
      int x=0, y=0,  barWidth;
      double bar = as.numericStats.min;
      
      if(m_data.attribute(classIndex).isNominal()) {
	barWidth = ((this.getWidth()-6)/histBarClassCounts.length)<1 ? 1:((this.getWidth()-6)/histBarClassCounts.length);
	  
	x = 3;
	if( (this.getWidth() - (x + histBarClassCounts.length*barWidth)) > 5 )
	  x += (this.getWidth() - (x + histBarClassCounts.length*barWidth))/2;
	  
	heightRatio = (this.getHeight()-(float)fm.getHeight())/maxValue;
	  
	if( ev.getX()-x >= 0) {
	  int temp = (int)((ev.getX()-x)/(barWidth+0.0000000001));
	  if(temp == 0){
	    int sum=0;
	    for(int k=0; k<histBarClassCounts[0].length; k++)
	      sum += histBarClassCounts[0][k];
	    return ("<html><center><font face=Dialog size=-1>"
		    +sum+"<br>["
		    +Utils.doubleToString(bar+m_barRange*temp,3)+", "+Utils.doubleToString((bar+m_barRange*(temp+1)),3)
		    +"]</font></center></html>");
	  }
	  else if( temp < histBarClassCounts.length ) {
	    int sum=0;
	    for(int k=0; k<histBarClassCounts[temp].length; k++)
	      sum+=histBarClassCounts[temp][k];
	    return ("<html><center><font face=Dialog size=-1>"
		    +sum+"<br>("
		    +Utils.doubleToString(bar+m_barRange*temp,3)+", "+Utils.doubleToString((bar+m_barRange*(temp+1)),3)
		    +"]</font></center></html>"); 
	  }
	}	  
      }
      else {
	barWidth = ((this.getWidth()-6)/histBarCounts.length)<1 ? 1:((this.getWidth()-6)/histBarCounts.length);
	  
	x = 3;
	if( (this.getWidth() - (x + histBarCounts.length*barWidth)) > 5 )
	  x += (this.getWidth() - (x + histBarCounts.length*barWidth))/2;
	  
	heightRatio = (this.getHeight()-(float)fm.getHeight())/maxValue;
	  
	if( ev.getX()-x >= 0) {
	  int temp = (int)((ev.getX()-x)/(barWidth+0.0000000001));
	  if(temp == 0)
	    return ("<html><center><font face=Dialog size=-1>"
		    +histBarCounts[0]+"<br>["
		    +Utils.doubleToString(bar+m_barRange*temp,3)+", "+Utils.doubleToString((bar+m_barRange*(temp+1)),3)
		    +"]</font></center></html>");
	  else if( temp < histBarCounts.length )
	    return ("<html><center><font face=Dialog size=-1>"
		    +histBarCounts[temp]+"<br>("
		    +Utils.doubleToString(bar+m_barRange*temp,3)+", "+Utils.doubleToString((bar+m_barRange*(temp+1)),3)
		    +"]</font></center></html>"); 
	}
      }
    }
    return ""; 
  }
    

  /**
   * Paints this component
   *
   * @param g The graphics object for this component
   */
  public void paintComponent(Graphics g) {
    g.clearRect(0,0,this.getWidth(), this.getHeight());

    if(as!=null) {
      if(threadRun==false) {
	int buttonHeight=0;
	if(m_colorAttrib!=null)
	  buttonHeight = m_colorAttrib.getHeight()+m_colorAttrib.getLocation().y;
	if(as.nominalCounts != null) { 
	  float heightRatio, intervalWidth;
	  int x=0, y=0, barHeight, barWidth;
		
	  if(m_data.attribute(classIndex).isNominal()) {
	    intervalWidth =  (this.getWidth()/(float)histBarClassCounts.length);
		    
	    if(intervalWidth>5)
	      barWidth = (int)Math.floor(intervalWidth*0.8F);
	    else
	      barWidth = 1;
		    
	    x = x + (int)( (Math.floor(intervalWidth*0.1F))<1 ? 1:(Math.floor(intervalWidth*0.1F)) );
		    
	    if( this.getWidth() - 
		(x + histBarClassCounts.length*barWidth 
		 +(int)( (Math.floor(intervalWidth*0.2F))<1 ? 
			 1:(Math.floor(intervalWidth*0.2F)) )*histBarClassCounts.length) > 5 )
	      x += (this.getWidth() - 
		    (x + histBarClassCounts.length*barWidth + 
		     (int)( (Math.floor(intervalWidth*0.2F))<1 ? 
			    1:(Math.floor(intervalWidth*0.2F)) )*histBarClassCounts.length))/2;
		    
	    int sum=0;
	    for(int i=0; i<histBarClassCounts.length; i++) {
	      heightRatio = (this.getHeight()-(float)fm.getHeight()-buttonHeight)/maxValue;
	      y=this.getHeight();
	      for(int j=0; j<histBarClassCounts[i].length; j++) {
		sum = sum + histBarClassCounts[i][j];
		y = y-Math.round(histBarClassCounts[i][j]*heightRatio);
		g.setColor( (Color)m_colorList.elementAt(j) );
		g.fillRect(x, y, barWidth, Math.round(histBarClassCounts[i][j]*heightRatio));
		g.setColor(Color.black);
	      }
	      if(fm.stringWidth(Integer.toString(sum))<intervalWidth)
		g.drawString(Integer.toString(sum), x, y-1);
	      x = x+barWidth+(int)( (Math.floor(intervalWidth*0.2F))<1 ? 1:(Math.floor(intervalWidth*0.2F)) );
	      sum=0;
	    }
		    
	  }
	  else {   //else if class attribute is numeric
	    intervalWidth =  (this.getWidth()/(float)histBarCounts.length);
		    
	    if(intervalWidth>5)
	      barWidth = (int)Math.floor(intervalWidth*0.8F);
	    else
	      barWidth = 1;
		    
	    x = x + (int)( (Math.floor(intervalWidth*0.1F))<1 ? 1:(Math.floor(intervalWidth*0.1F)) );
		    
	    if( this.getWidth() - 
		(x + histBarCounts.length*barWidth 
		 +(int)( (Math.floor(intervalWidth*0.2F))<1 ? 1:(Math.floor(intervalWidth*0.2F)) )*histBarCounts.length) > 5 )
	      x += (this.getWidth() - 
		    (x + histBarCounts.length*barWidth + 
		     (int)( (Math.floor(intervalWidth*0.2F))<1 ? 1:(Math.floor(intervalWidth*0.2F)) )*histBarCounts.length))/2;
		    
	    for(int i=0; i<histBarCounts.length; i++) {
	      heightRatio = (this.getHeight()-(float)fm.getHeight()-buttonHeight)/maxValue;
	      y = this.getHeight()-Math.round(histBarCounts[i]*heightRatio);
	      g.fillRect(x, y, barWidth, Math.round(histBarCounts[i]*heightRatio));
	      if(fm.stringWidth(Integer.toString(histBarCounts[i]))<intervalWidth)
		g.drawString(Integer.toString(histBarCounts[i]), x, y-1);
			
	      x = x+barWidth+(int)( (Math.floor(intervalWidth*0.2F))<1 ? 1:(Math.floor(intervalWidth*0.2F)) );
	    }
	  }
		
	}
	//else {
	//g.clearRect(0, 0, this.getWidth(), this.getHeight()); 
	//g.drawString("Calculating. Please Wait...", 
	//       this.getWidth()/2 - fm.stringWidth("Calculating. Please Wait...")/2,
	//       this.getHeight()/2-fm.getHeight()/2);
	//}
	//} 
	else if(as.numericStats != null) {
		
	  if(histBarClassCounts!=null || histBarCounts!=null) {
	    //threadRun==false && (histBarClassCounts!=null || histBarCounts!=null)) {
	    float heightRatio, intervalWidth;
	    int x=0, y=0,  barWidth;
		    
	    if(m_data.attribute(classIndex).isNominal()) {
	      barWidth = ((this.getWidth()-6)/histBarClassCounts.length)<1 ? 1:((this.getWidth()-6)/histBarClassCounts.length);
			
	      x = 3;
	      if( (this.getWidth() - (x + histBarClassCounts.length*barWidth)) > 5 )
		x += (this.getWidth() - (x + histBarClassCounts.length*barWidth))/2;
			
	      for(int i=0; i<histBarClassCounts.length; i++) {
		heightRatio = (this.getHeight()-(float)fm.getHeight()-buttonHeight-19)/maxValue;
		y = this.getHeight()-19;
		int sum = 0;
		for(int j=0; j<histBarClassCounts[i].length; j++) {
		  y = y-Math.round(histBarClassCounts[i][j]*heightRatio);
				//System.out.println("Filling x:"+x+" y:"+y+" width:"+barWidth+" height:"+(histBarCounts[i]*heightRatio));
		  g.setColor( (Color)m_colorList.elementAt(j) );
		  if(barWidth>1)
		    g.fillRect(x, y, barWidth, Math.round(histBarClassCounts[i][j]*heightRatio));
		  else if((histBarClassCounts[i][j]*heightRatio)>0)
		    g.drawLine(x, y, x, y+Math.round(histBarClassCounts[i][j]*heightRatio));
		  g.setColor(Color.black);
		  sum = sum + histBarClassCounts[i][j];
		}
		if(fm.stringWidth(" "+Integer.toString(sum))<barWidth)
		  g.drawString(" "+Integer.toString(sum), x, y-1);
			    
		x = x+barWidth;
	      }
			
	      x = 3;
	      if( (this.getWidth() - (x + histBarClassCounts.length*barWidth)) > 5 )
		x += (this.getWidth() - (x + histBarClassCounts.length*barWidth))/2;
			
	      g.drawLine(x, this.getHeight()-17, 
			 (barWidth==1) ? x+barWidth*histBarClassCounts.length-1:x+barWidth*histBarClassCounts.length,
			 this.getHeight()-17); //axis line
	      g.drawLine(x, this.getHeight()-16, x, this.getHeight()-12); //minimum line
	      g.drawString(Utils.doubleToString(as.numericStats.min, 2), 
			   x, 
			   this.getHeight()-12+fm.getHeight()); //minimum value
	      g.drawLine(x+(barWidth*histBarClassCounts.length)/2,
			 this.getHeight()-16,
			 x+(barWidth*histBarClassCounts.length)/2,
			 this.getHeight()-12); //median line
	      g.drawString(Utils.doubleToString(as.numericStats.max/2+as.numericStats.min/2, 2), 
			   x+(barWidth*histBarClassCounts.length)/2
			   -fm.stringWidth(Utils.doubleToString(as.numericStats.max/2+as.numericStats.min/2, 2))/2,
			   this.getHeight()-12+fm.getHeight()); //median value
	      g.drawLine( (barWidth==1) ? x+barWidth*histBarClassCounts.length-1:x+barWidth*histBarClassCounts.length, 
			  this.getHeight()-16, 
			  (barWidth==1) ? x+barWidth*histBarClassCounts.length-1:x+barWidth*histBarClassCounts.length, 
			  this.getHeight()-12); //maximum line
	      g.drawString(Utils.doubleToString(as.numericStats.max, 2), 
			   (barWidth==1) ?
			   x+barWidth*histBarClassCounts.length-fm.stringWidth(Utils.doubleToString(as.numericStats.max, 2))-1:
			   x+barWidth*histBarClassCounts.length-fm.stringWidth(Utils.doubleToString(as.numericStats.max, 2)), 
			   this.getHeight()-12+fm.getHeight()); //maximum value
	    }
	    else {  //if class attribute is numeric
	      barWidth = ((this.getWidth()-6)/histBarCounts.length)<1 ? 1:((this.getWidth()-6)/histBarCounts.length);
			
	      x = 3;
	      if( (this.getWidth() - (x + histBarCounts.length*barWidth)) > 5 )
		x += (this.getWidth() - (x + histBarCounts.length*barWidth))/2;
			
	      for(int i=0; i<histBarCounts.length; i++) {
		heightRatio = (this.getHeight()-(float)fm.getHeight()-buttonHeight-19)/maxValue;
		y = this.getHeight()-Math.round(histBarCounts[i]*heightRatio)-19;
		//System.out.println("Filling x:"+x+" y:"+y+" width:"+barWidth+" height:"+(histBarCounts[i]*heightRatio));
		if(barWidth>1)
		  g.drawRect(x, y, barWidth, Math.round(histBarCounts[i]*heightRatio));
		else if((histBarCounts[i]*heightRatio)>0)
		  g.drawLine(x, y, x, y+Math.round(histBarCounts[i]*heightRatio));
		if(fm.stringWidth(" "+Integer.toString(histBarCounts[i]))<barWidth)
		  g.drawString(" "+Integer.toString(histBarCounts[i]), x, y-1);
			    
		x = x+barWidth;
	      }
			
	      x = 3;
	      if( (this.getWidth() - (x + histBarCounts.length*barWidth)) > 5 )
		x += (this.getWidth() - (x + histBarCounts.length*barWidth))/2;
			
	      g.drawLine(x, this.getHeight()-17, 
			 (barWidth==1) ? x+barWidth*histBarCounts.length-1:x+barWidth*histBarCounts.length,
			 this.getHeight()-17); //axis line
	      g.drawLine(x, this.getHeight()-16, x, this.getHeight()-12); //minimum line
	      g.drawString(Utils.doubleToString(as.numericStats.min, 2), 
			   x, 
			   this.getHeight()-12+fm.getHeight()); //minimum value
	      g.drawLine(x+(barWidth*histBarCounts.length)/2,
			 this.getHeight()-16,
			 x+(barWidth*histBarCounts.length)/2,
			 this.getHeight()-12); //median line
	      g.drawString(Utils.doubleToString(as.numericStats.max/2+as.numericStats.min/2, 2), 
			   x+(barWidth*histBarCounts.length)/2
			   -fm.stringWidth(Utils.doubleToString(as.numericStats.max/2+as.numericStats.min/2, 2))/2,
			   this.getHeight()-12+fm.getHeight()); //median value
	      g.drawLine( (barWidth==1) ? x+barWidth*histBarCounts.length-1:x+barWidth*histBarCounts.length, 
			  this.getHeight()-16, 
			  (barWidth==1) ? x+barWidth*histBarCounts.length-1:x+barWidth*histBarCounts.length, 
			  this.getHeight()-12); //maximum line
	      g.drawString(Utils.doubleToString(as.numericStats.max, 2), 
			   (barWidth==1) ?
			   x+barWidth*histBarCounts.length-fm.stringWidth(Utils.doubleToString(as.numericStats.max, 2))-1:
			   x+barWidth*histBarCounts.length-fm.stringWidth(Utils.doubleToString(as.numericStats.max, 2)), 
			   this.getHeight()-12+fm.getHeight()); //maximum value
	    }
	    //System.out.println("barWidth:"+barWidth+" histBarCount:"+histBarCounts.length);
		    
	  }
	}
      }
      else {   //if still calculating the plot
	g.clearRect(0, 0, this.getWidth(), this.getHeight()); 
	g.drawString("Calculating. Please Wait...", 
		     this.getWidth()/2 - fm.stringWidth("Calculating. Please Wait...")/2,
		     this.getHeight()/2-fm.getHeight()/2);
      }
    }
  }
    
    
    
  /**
   * Main method to test this class from command line
   *
   * @param args The arff file and the index of the attribute to use
   */ 
  public static void main(String [] args) {
    if(args.length!=3) {
      final JFrame jf = new JFrame("AttribVisualization");
      AttributeVisualizationPanel ap = new AttributeVisualizationPanel();
      try { 
	Instances ins = new Instances( new FileReader(args[0]) );
	ap.setInstances(ins);
	System.out.println("Loaded: "+args[0]+"\nRelation: "+ap.m_data.relationName()+"\nAttributes: "+ap.m_data.numAttributes());
	ap.setAttribute( Integer.parseInt(args[1]) );
      }
      catch(Exception ex) { ex.printStackTrace(); System.exit(-1); }
      System.out.println("The attributes are: ");
      for(int i=0; i<ap.m_data.numAttributes(); i++)
	System.out.println(ap.m_data.attribute(i).name());

      jf.setSize(500, 300);
      jf.getContentPane().setLayout( new BorderLayout() );
      jf.getContentPane().add(ap, BorderLayout.CENTER );
      jf.setDefaultCloseOperation( jf.EXIT_ON_CLOSE );
      jf.show();
    }
    else
      System.out.println("Usage: java AttributeVisualizationPanel [arff file] [index of attribute]");
  }
}
