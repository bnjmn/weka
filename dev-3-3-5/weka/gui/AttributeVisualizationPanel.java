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
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
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
 * dataset. For numeric attributes, it shows the spread 
 * of the attribute using a scatter plot in which the 
 * X-axis correspond to the values of the attribute and 
 * a random noise is added to the Y-axis.
 *
 * @author Ashraf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */

public class AttributeVisualizationPanel extends JPanel {

  Instances m_data;
  AttributeStats as;
  int attribIndex, maxValue;
  FontMetrics fm;
    
  public AttributeVisualizationPanel() {
    this.setFont( new Font("Default", Font.PLAIN, 8) );
    fm = this.getFontMetrics( this.getFont() );
    this.setToolTipText("");
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
    attribIndex=index;
    as = m_data.attributeStats(attribIndex);

    if(as.nominalCounts!=null) {
      maxValue = as.nominalCounts[0];
      for(int i=0; i<m_data.attribute(attribIndex).numValues(); i++) { 
	
	if(as.nominalCounts[i]>maxValue)
	  maxValue = as.nominalCounts[i];
      }
      
    }
    this.repaint();
  }
    

  /**
   * Returns "<nominal value> [<nominal value count>]"
   * in case if displaying a bar plot and mouse is on some bar.
   * Otherwise returns ""
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
      return ""; 
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
	float widthRatio;
	int x, y;
	Random rnd = new Random();
		
	widthRatio = (float)((this.getWidth()-10)/(as.numericStats.max-as.numericStats.min));
	for(int i=0; i<m_data.numInstances(); i++) {

	  if(m_data.instance(i).isMissing(attribIndex))
	    continue;

	  x = (int)Math.round((m_data.instance(i).value(attribIndex)-as.numericStats.min)*widthRatio)+5;
	  y = rnd.nextInt( this.getHeight()-20 );

	  if(m_data.numInstances()<1500)
	    g.drawOval(x,y,1,1);
	  else
	    g.drawLine(x,y,x,y);
	}
	g.drawLine(5, this.getHeight()-18, this.getWidth()-5, this.getHeight()-18); //axis line
     
	g.drawLine(5, this.getHeight()-17, 5, this.getHeight()-12); //minimum line
	g.drawString(Utils.doubleToString(as.numericStats.min, 2), 
		     5, 
		     this.getHeight()-12+fm.getHeight()); //minimum line
	g.drawLine(5+(int)((as.numericStats.max-as.numericStats.min)*widthRatio/2),
		   this.getHeight()-17,
		   5+(int)((as.numericStats.max-as.numericStats.min)*widthRatio/2),
		   this.getHeight()-12); //median line
	g.drawString(Utils.doubleToString(as.numericStats.max/2+as.numericStats.min/2, 2), 
		     5+(int)((as.numericStats.max-as.numericStats.min)*widthRatio/2)
		     -fm.stringWidth(Utils.doubleToString(as.numericStats.max/2+as.numericStats.min/2, 2))/2,
		     this.getHeight()-12+fm.getHeight());
	g.drawLine(this.getWidth()-5, this.getHeight()-17, this.getWidth()-5, this.getHeight()-12); //maximum line
	g.drawString(Utils.doubleToString(as.numericStats.max, 2), 
		     this.getWidth()-5-fm.stringWidth(Utils.doubleToString(as.numericStats.max, 2)), 
		     this.getHeight()-12+fm.getHeight()); 
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
	ap.setAttribute( Integer.parseInt(args[1]) );
      }
      catch(Exception ex) { ex.printStackTrace(); System.exit(-1); }
      System.out.println("Loaded: "+args[0]+"\nRelation: "+ap.m_data.relationName()+"\nAttributes: "+ap.m_data.numAttributes());
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
