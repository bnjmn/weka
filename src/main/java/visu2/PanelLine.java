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
 *    PanelLine.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package visu2;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import rules.Rule;

import drawingTools.ColorArray;

/**
 * @author Sebastien
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PanelLine extends selection.DataPanel implements Printable {

	private Rule[] rules;
	private String[] criteres;

	/**
	 * Constructor
	 */
	public PanelLine() {
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
		int sizeRules = rules.length * 20;
		BufferedImage image = null;
		if (rules.length < 15) {
			image =
			new BufferedImage(w, h + sizeRules, BufferedImage.TYPE_INT_RGB);
		}
		else
			image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.createGraphics();
		g.setColor(Color.LIGHT_GRAY);
		if (rules.length < 15) 
			g.fillRect(0, 0, w, h + sizeRules);
		else
			g.fillRect(0, 0, w, h);
		this.print(g);
		if (rules.length < 15) {
			g.setFont(new Font("arial", Font.PLAIN, 12));
			for (int i = 0; i < rules.length; i++) {

				g.setColor(ColorArray.colorArray[i]);
				g.drawString(rules[i].toString(), 0, i * 20 + h);
			}
		}
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
		if (rules.length < 15) {
			g2.setFont(new Font("arial", Font.PLAIN, 12));
			for (int i = 0; i < rules.length; i++) {

				g2.setColor(ColorArray.colorArray[i]);
				g2.drawString(
					rules[i].toString(),
					0,
					i * 20 + this.getHeight());
			}
		}

		return PAGE_EXISTS;
	}

	/**
	 * Get datas and draw the visualisation on this panel
	 * 
	 * @param rules rules
	 * @param criteres criteres we want to see on this visualisation (all is better)
	 */
	public void setData(Rule[] rules, String[] criteres) {
		this.criteres = criteres;
		this.rules = rules;

	}

	/**	
	 * Draw the component when the window changes
	 * 
	 * @param g the graphic
	 */
	protected void paintComponent(Graphics g) {

		int i;

		//
		//  set the drawing area
		//
		// panel size
		int scrHeight = (new Double(this.getSize().getHeight())).intValue();
		int scrWidth = (new Double(this.getSize().getWidth())).intValue();
		// drawing area size
		int height = scrHeight - 20;
		int width = scrWidth;
		// graph origin
		int gX = 50;
		int gY = height - 50;
		// graph size
		int gWidth = width - gX - 20;
		int gHeight = gY - 20;

		//
		// initialize the graph background
		//
		g.setColor(this.getBackground());
		//g.fillRect(0, 0, width, height);

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
		//  draw axis
		//
		g.setColor(Color.black);
		g.drawLine(gX, gY, gX, gY - gHeight);
		g.drawLine(gX, gY, gX + gWidth, gY);
		//draw Y ladder
		int nb = 10;
		for (i = 0; i <= nb; i++)
			g.drawLine(
				gX - 2,
				i * (gHeight) / nb + gY - gHeight,
				gX + 2,
				i * (gHeight) / nb + gY - gHeight);
		//draw X ladder
		int scaleX = (gWidth) / criteres.length;
		for (i = 0; i <= criteres.length; i++)
			g.drawLine(gX + i * scaleX, gY - 2, gX + i * scaleX, gY + 2);
		//write min/max
		g.drawString("min", gX - 40, gY + 5);
		g.drawString("max", gX - 40, gY + 5 - gHeight);

		//
		//  draw graph
		//
		int nRule, nCrit;
		int posX1 = 0, posY1 = 0, posX2 = 0, posY2 = 0;
		float val, min = 0f, max = 0f, pmin = 0f, pmax = 0f;

		for (nCrit = 0; nCrit < criteres.length; nCrit++) {

			// calculate the scale for this critere
			min = (rules[0].getCritere(criteres[nCrit])).getValue();
			max = min;
			for (nRule = 1; nRule < rules.length; nRule++) {
				// search max & min 
				val = (rules[nRule].getCritere(criteres[nCrit])).getValue();
				if (val > max)
					max = val;
				else if (val < min)
					min = val;
			}

			//min = 0;
			//max = 100;
			min -= (max - min) * 2 / 100;

			// write on the graph
			g.setColor(Color.BLACK);
			g.drawString(
				(new Float(max)).toString(),
				gX + (nCrit + 1) * scaleX - 30,
				gY - gHeight - 5);
			g.drawString(
				(new Float(min)).toString(),
				gX + (nCrit + 1) * scaleX - 30,
				gY - 5);
			g.drawString(
				criteres[nCrit],
				gX + (nCrit + 1) * scaleX - 50,
				gY + 15);

			// draw line for all rules
			for (nRule = 0; nRule < rules.length; nRule++) {

				// Set the color
				if (rules.length < 15)
					g.setColor(ColorArray.colorArray[nRule]);
				else
					g.setColor(Color.BLACK);

				// draw all rules for this critere
				if (nCrit == 0) {

					val = rules[nRule].getCritere(criteres[0]).getValue();
					posX1 = gX;
					posY1 =
						gY
							- (new Float((val - min) * gHeight / (max - min)))
								.intValue();
					if (posY1 == gY)
						posY1--;

					val = rules[nRule].getCritere(criteres[nCrit]).getValue();
					posX2 = gX + (nCrit + 1) * scaleX;
					posY2 =
						gY
							- (new Float((val - min) * gHeight / (max - min)))
								.intValue();
					if (posY2 == gY)
						posY2--;

					//write the rule ID 	
					g.drawString((rules[nRule]).getId(), posX1 + 3, posY1 - 3);
				} else {
					val =
						rules[nRule].getCritere(criteres[nCrit - 1]).getValue();
					posX1 = gX + nCrit * scaleX;
					posY1 =
						gY
							- (new Float((val - pmin) * gHeight / (pmax - pmin)))
								.intValue();
					if (posY1 == gY)
						posY1--;

					val = rules[nRule].getCritere(criteres[nCrit]).getValue();
					posX2 = gX + (nCrit + 1) * scaleX;
					posY2 =
						gY
							- (new Float((val - min) * gHeight / (max - min)))
								.intValue();
					if (posY2 == gY)
						posY2--;
				}

				g.drawLine(posX1, posY1, posX2, posY2);

			}
			pmin = min;
			pmax = max;

		}
	}
}
