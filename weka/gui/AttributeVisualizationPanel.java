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
 *    AttributeSummaryPanel.java
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JButton;

import weka.core.Instances;
import weka.core.AttributeStats;
import weka.core.Utils;


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
 * @version $Revision: 1.2 $
 */

public class AttributeVisualizationPanel extends JPanel {

  Instances m_data;
  AttributeStats as;
  int attribIndex, maxValue;
  int histBarCounts[]; double m_barRange;
  Thread hc; 
  boolean threadRun=false;

  FontMetrics fm;
    
  public AttributeVisualizationPanel() {
    this.setFont( new Font("Default", Font.PLAIN, 8) );
    fm = this.getFontMetrics( this.getFont() );
    this.setToolTipText("");
    this.addComponentListener( new ComponentAdapter() {
	public void componentResized(ComponentEvent ce) {
	  if(m_data!=null)
	    setAttribute(attribIndex);
	}
      });
  }

  /**
   * Sets the instances for use
   *
   * @param newins a set of Instances
   */
  public void setInstances(Instances newins) {
    m_data = newins;
    as=null;
    this.repaint();
  }
    
  /**
   * Tells the panel which attribute to visualize.
   *
   * @param index The index of the attribute
   */
  public void setAttribute(int index) { 
    threadRun = false; 
    if(hc!=null && hc.isAlive()) hc.stop(); 
    attribIndex=index;
    as = m_data.attributeStats(attribIndex);

    if(as.nominalCounts!=null) {
      maxValue = as.nominalCounts[0];
      for(int i=0; i<m_data.attribute(attribIndex).numValues(); i++) { 
	if(as.nominalCounts[i]>maxValue)
	  maxValue = as.nominalCounts[i];
      }
      //this.repaint();      
    }
    else if(as.numericStats!=null) {
      hc = new HistCalc(); threadRun = true;
      hc.start();
    }
    this.repaint();
  }
  

  private class HistCalc extends Thread {
    public void run() {
      int histCounts[]  = (AttributeVisualizationPanel.this.getWidth()<(int)(as.totalCount*0.1)) ?
	new int[AttributeVisualizationPanel.this.getWidth()-4] : new int[(int)(as.totalCount*0.1)];
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
	    

      /*
	System.out.println("barRange: "+barRange);
	for(int i=0; i<histCounts.length ; i++) {
	for(int k=0; k<m_data.numInstances() ; k++) {
	if(currentBar == as.numericStats.min) {
	if(m_data.instance(k).value(attribIndex)>=currentBar 
	&& m_data.instance(k).value(attribIndex)<=(currentBar+barRange) ) {
	histCounts[i]++; System.out.print(m_data.instance(k).value(attribIndex)+" ");
	}
	}
	else if(m_data.instance(k).value(attribIndex)>currentBar 
	&& m_data.instance(k).value(attribIndex)<=(currentBar+barRange) ) {
	histCounts[i]++; System.out.print(m_data.instance(k).value(attribIndex)+" ");
	}
	}
	System.out.println("--> in "+(i+1)+"from "+currentBar+" to "+(currentBar+barRange));
	if(histCounts[i]>maxValue)
	maxValue = histCounts[i];
	currentBar = currentBar  + barRange;
	}
      */

      //if(threadRun==false)
      // 	return;
      //else
      threadRun=false;
      histBarCounts = histCounts;
      m_barRange =  barRange;
      AttributeVisualizationPanel.this.repaint();
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
    else if(histBarCounts!=null && threadRun==false) {
      float heightRatio, intervalWidth;
      int x=0, y=0,  barWidth;
      double bar = as.numericStats.min;
	
      barWidth = ((this.getWidth()-6)/histBarCounts.length)<1 ? 1:((this.getWidth()-6)/histBarCounts.length);
	
      x = 3;
      if( (this.getWidth() - (x + histBarCounts.length*barWidth)) > 5 )
	x += (this.getWidth() - (x + histBarCounts.length*barWidth))/2;
	
      heightRatio = (this.getHeight()-(float)fm.getHeight())/maxValue;

      if( ev.getX()-x >= 0) {
	int temp = (ev.getX()-x)/barWidth;
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
      if(as.nominalCounts != null) { 
	float heightRatio, intervalWidth;
	int x=0, y=0, barHeight, barWidth;
	    
	intervalWidth =  (this.getWidth()/(float)as.nominalCounts.length);
		
	if(intervalWidth>5)
	  barWidth = (int)Math.floor(intervalWidth*0.8F);
	else
	  barWidth = 1;
		
	x = x + (int)( (Math.floor(intervalWidth*0.1F))<1 ? 1:(Math.floor(intervalWidth*0.1F)) );

	if( this.getWidth() - 
	    (x + as.nominalCounts.length*barWidth 
	     +(int)( (Math.floor(intervalWidth*0.2F))<1 ? 1:(Math.floor(intervalWidth*0.2F)) )*as.nominalCounts.length) > 5 )
	  x += (this.getWidth() - 
		(x + as.nominalCounts.length*barWidth + 
		 (int)( (Math.floor(intervalWidth*0.2F))<1 ? 1:(Math.floor(intervalWidth*0.2F)) )*as.nominalCounts.length))/2;
	
	for(int i=0; i<as.nominalCounts.length; i++) {
	  heightRatio = (this.getHeight()-(float)fm.getHeight())/maxValue;
	  y = this.getHeight()-Math.round(as.nominalCounts[i]*heightRatio);
	  g.fillRect(x, y, barWidth, Math.round(as.nominalCounts[i]*heightRatio));
	  if(fm.stringWidth(Integer.toString(as.nominalCounts[i]))<intervalWidth)
	    g.drawString(Integer.toString(as.nominalCounts[i]), x, y-1);
	  
	  x = x+barWidth+(int)( (Math.floor(intervalWidth*0.2F))<1 ? 1:(Math.floor(intervalWidth*0.2F)) );
	}
      } else if(as.numericStats != null) {

	if(threadRun==false && histBarCounts!=null) {
	  float heightRatio, intervalWidth;
	  int x=0, y=0,  barWidth;
	      
	  barWidth = ((this.getWidth()-6)/histBarCounts.length)<1 ? 1:((this.getWidth()-6)/histBarCounts.length);
	      	      
	  x = 3;
	  if( (this.getWidth() - (x + histBarCounts.length*barWidth)) > 5 )
	    x += (this.getWidth() - (x + histBarCounts.length*barWidth))/2;
	      
	  for(int i=0; i<histBarCounts.length; i++) {
	    heightRatio = (this.getHeight()-(float)fm.getHeight()-19)/maxValue;
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
	  //System.out.println("barWidth:"+barWidth+" histBarCount:"+histBarCounts.length);
	      
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
	else {
	  g.clearRect(0, 0, this.getWidth(), this.getHeight()); 
	  g.drawString("Calculating. Please Wait...", 
		       this.getWidth()/2 - fm.stringWidth("Calculating. Please Wait...")/2,
		       this.getHeight()/2-fm.getHeight()/2);
	}
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
