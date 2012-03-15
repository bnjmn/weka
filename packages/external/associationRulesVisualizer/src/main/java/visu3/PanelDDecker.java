/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    PanelDDecker.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package visu3;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import javax.imageio.ImageIO;

import drawingTools.ColorArray;

import rules.Attribute;
import rules.Cardinality;
import rules.Rule;

/**
 * @author Sebastien
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PanelDDecker extends javax.swing.JPanel implements Printable{

	private Rule[] rules;

	/**
	 * Constructor
	 */
	public PanelDDecker() {
		this.setBackground(Color.WHITE);
		rules = null;
	}



	 /**
     * Save the visualization as a picture
     * 
     * @param f the name of the file
     */

	public void save(File f) throws IOException {
		int w = this.getWidth();
		int h = this.getHeight();
		BufferedImage image =
			new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.createGraphics();
		this.print(g);
		ImageIO.write(image, "jpg", f);

	}
	
	
	/**
	 * Print the visualization
	 * @param g the Graphics object
	 * @param pf page format
	 * @param pageIndex page index
	 */
	public int print(Graphics g, PageFormat pf, int pageIndex) {

		Graphics2D g2 = (Graphics2D) g;

		if (pageIndex >= 1)
			return NO_SUCH_PAGE;

		g2.translate((int) pf.getImageableX(), (int) pf.getImageableY());

		int componentHeight = this.getHeight();
		int componentWidth = this.getWidth();

		double printableHeight = pf.getImageableHeight();
		double printableWidth = pf.getImageableWidth();

		double scaleHeight = printableHeight / componentHeight;
		double scaleWidth = printableWidth / componentWidth;

		double scale;

		if (scaleHeight < scaleWidth)
			scale = scaleHeight;
		else
			scale = scaleWidth;

		g2.scale(scale, scale);

		this.print(g2);

		return PAGE_EXISTS;
	}

	/**
	 * Get datas and draw the visualisation on this panel
	 * 
	 * @param rules rules
	 * @param criteres criteres we want to see on this visualisation (all is better)
	 */
	public void setData(Rule[] rules) {
		this.rules = rules;
	}


	/**
	 * return the index of an element from a hashSet
	 * @param h hasset
	 * @object o search element
	 * @return the index
	 */
	public int getHashSetIndex(HashSet h, Object o){
		Iterator it = h.iterator();
		int index = 0;
		while(it.hasNext()){
			if (it.next().equals(o))
				return index;
			index++;		
		}
	
		return -1;	
	}

	/**	
	 * Draw the component when the window changes
	 * 
	 * @param g the graphic
	 */
	protected void paintComponent(Graphics g) {
		int c, i, j;
		String str;
		//
		//  Init values
		//
		Rule theRule = rules[0];
		int nbAttrs = theRule.getCondition().size();

		//
		//  set the drawing area
		//
		// panel size
		int scrHeight = (new Double(this.getSize().getHeight())).intValue();
		int scrWidth = (new Double(this.getSize().getWidth())).intValue();
		// drawing area size
		int height = scrHeight - 50;
		int width = scrWidth;
		// graph origin
		int gX = 30;
		int gY = height - 150;
		// graph size
		int gWidth = width - gX - 150;
		int gHeight = gY - 20;

		//
		// initialize the graph background
		//
		g.setColor(this.getBackground());
		g.fillRect(0, 0, width, height);

		//
		//  is there selected rules?
		//
		if (rules == null) {
			g.setFont(new Font("arial", Font.BOLD, 20));

			g.setColor(Color.RED);
			g.drawString("No selected rules", (width - 200) / 2, 50);
			g.drawString("Check the selection tab", (width - 250) / 2, 75);
			return;
		}

		// 
		//  Draw ladder
		//

		g.setColor(Color.BLACK);
		g.drawString("0", 5, gY - 0 * gHeight / 10);
		g.drawString("50", 5, gY - 5 * gHeight / 10);
		g.drawString("100", 5, gY - 10 * gHeight / 10);

		g.drawLine(gX, 10, gX, gY + 10);
		for (i = 0; i <= 10; i++) {
			g.setColor(new Color(0.5f, 0.5f, 0.5f));
			g.drawLine(gX,gY-i*gHeight/10,gX+20,gY-i*gHeight/10);
		}

		//
		//  Draw the graph
		//

		int space = 5;
		int currentX = gX + 20 ;

		int nbCards = theRule.getCardinalities().size();
		int previousXDigit[] = new int[nbAttrs];
		String previousAttr[] = new String[nbAttrs];

		Cardinality card = null;
		Attribute attr = null;	
		
		
		// search possible values for each attributes
		HashSet[] values = new HashSet[nbAttrs];
		for (i=0; i<nbAttrs; i++)
			values[i] = new HashSet();
		
		
		for (c=0; c<nbCards; c++){
			card = (Cardinality) theRule.getCardinalities().get(c);
			for (i=0; i<nbAttrs; i++){
				attr = (Attribute)card.getAttributes().get(i);
				values[i].add(attr.getValue());
			}
		}				
		
		
		// start drawing the graph		
		for (c=0; c<nbCards; c++){
			card = (Cardinality) theRule.getCardinalities().get(c);
			for (i=0; i<nbAttrs; i++){
				attr = (Attribute)card.getAttributes().get(i);
				
				if (! attr.getValue().equals(previousAttr[i])){
					if(previousAttr[i] != null){
						// color of attribute
						g.setColor(ColorArray.colorArray[7+getHashSetIndex(values[i], previousAttr[i] )]);
						g.fillRect(previousXDigit[i]-space,gY+30*(nbAttrs-i),currentX-previousXDigit[i],10);

						// rect of attribute
						g.setColor(Color.BLACK);
						g.drawRect(previousXDigit[i]-space,gY+30*(nbAttrs-i),currentX-previousXDigit[i],10);				
					}
					previousXDigit[i] = currentX+space;
					previousAttr[i] = attr.getValue();
				}
					
				
			}	
			// draw value of confidence and cardinality
			currentX += drawColumn(g, card.getConfidence(), card.getFrequency(), currentX, gY, gHeight, gWidth);											
			currentX += space;
		}

		for (i=0; i<nbAttrs; i++){
			// color of attribute
			g.setColor(ColorArray.colorArray[7+getHashSetIndex(values[i], previousAttr[i] )]);
			g.fillRect(previousXDigit[i]-space,gY+30*(nbAttrs-i),currentX-previousXDigit[i],10);

			// rect of attribute
			g.setColor(Color.BLACK);
			g.drawRect(previousXDigit[i]-space,gY+30*(nbAttrs-i),currentX-previousXDigit[i],10);
			
			// draw attributes names
			g.drawString(((Attribute)card.getAttributes().get(i)).getName(), currentX+10, gY+30*nbAttrs-i*30+10);
		}
		
		// draw legend

		Iterator it ;
		for (i=0; i<nbAttrs; i++){
			j=0; 
			currentX = gX+20;
			g.setColor(Color.BLACK);
			g.drawString(((Attribute)card.getAttributes().get(i)).getName()+"  (",currentX,gY+30*(nbAttrs-i)-5);			
			currentX+=8*((Attribute)card.getAttributes().get(i)).getName().length();
			it = ((HashSet)values[i]).iterator();
			while(it.hasNext()){
				g.setColor(ColorArray.colorArray[7+j]);
				str = (String)it.next();
				g.drawString(str,currentX,gY+30*(nbAttrs-i)-5);			
				currentX+=8*str.length();
				j++;
			}	
			g.setColor(Color.BLACK);			
			g.drawString(")",currentX,gY+30*(nbAttrs-i)-5);	

		}
				
		// write right hand size
		g.drawString(theRule.getConclusion().getName(), gX+gWidth+50, gY - 30);
		g.drawString(theRule.getConclusion().getValue(), gX+gWidth+50, gY - 20);

	}

	protected int drawColumn(
		Graphics g,
		float conf,
		float supp,
		int x,
		int y,
		int height,
		int width) {
		int rectHeight = (new Float(conf * height / 100)).intValue();
		int rectWidth = (new Float(supp * width / 100)).intValue();
		g.setColor(Color.BLACK);
		g.drawRect(x, y - height, rectWidth, height);
		g.setColor(Color.RED);
		g.fillRect(x, y - rectHeight, rectWidth, rectHeight);

		return rectWidth;
	}
}
