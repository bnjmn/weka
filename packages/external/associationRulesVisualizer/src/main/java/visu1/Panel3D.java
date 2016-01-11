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
 *    Panel3D.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package visu1;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.LineArray;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;

import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import rules.Attribute;
import rules.Rule;

import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.Text2D;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;
import drawingTools.ColorArray;

/**
 * @author Sebastien
 * @author Mark Hall (memory leak fixes).
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class Panel3D extends selection.DataPanel implements Printable {

	private BufferedImage internalImage;


	private Rule[] rules;
	private String[] criteres;

	private Canvas3D canvas3D;
	private OrbitBehavior orbit;
	private SimpleUniverse m_simpleU;
	private BranchGroup m_scene;
	private boolean m_canvasCreated;




	/**
	 * Constructor
	 */
	public Panel3D() {
		rules = null;
//		FlowLayout fl = new FlowLayout(FlowLayout.RIGHT,0,0);
		//setLayout(fl);
		setLayout(new java.awt.BorderLayout());

	}


	/**
	 * Create a picture with the visualisation
	 * @param posX X position of the top left corner
	 * @param posY Y position of the top left corner
	 */
	public void createInternalImage(int posX, int posY) {
		Robot robot = null;
		try {
			robot = new Robot();
		} catch (AWTException e) {
		}

		internalImage = robot.createScreenCapture(
				 new Rectangle(canvas3D.getX() + posX,canvas3D.getY() + posY,canvas3D.getWidth(),canvas3D.getHeight()));		
	}



	/**
	 * Save the visualzation as a picture
	 * 
	 * @param name of the file to save
	 */
	public void save(File f, int posX, int posY) throws IOException {
		Graphics g = internalImage.createGraphics();
		ImageIO.write(internalImage, "jpg", f);

	}


	/**
	 *  Print the visualisation
	 * 
	 * @param g Graphics object
	 * @param pf Page format
	 * @param page Index
	 */	
	public int print(Graphics g, PageFormat pf, int pageIndex) {

		Graphics2D g2 = (Graphics2D) g;
		
		int componentHeight = internalImage.getHeight();
		int componentWidth = internalImage.getWidth();

		double printableHeight = pf.getImageableHeight();
		double printableWidth = pf.getImageableWidth();

		double scaleHeight = printableHeight / componentHeight;
		double scaleWidth = printableWidth / componentWidth;

		double scale;

		if (scaleHeight < scaleWidth)
			scale = scaleHeight;
		else
			scale = scaleWidth;

		g2.scale(scale,scale);
		g2.translate((int) pf.getImageableX(), (int) pf.getImageableY());
		
		if (pageIndex >= 1)
			return NO_SUCH_PAGE;

		g2.drawImage(internalImage, 0, 0, this);


		return PAGE_EXISTS;
	}

	/**
	 * get all data of the graph we want to draw and create the graph
	 * 
	 * @param rules rules specified from the package rules
	 * @param criteres criteres we want to see on the visualisation
	 */
	public void setData(Rule[] rules, String[] criteres) {

		// get all data
		this.rules = rules;
		this.criteres = criteres;
		drawGraph();
	}
	
	public void freeResources() {
	  if (m_simpleU != null) {

	    // Mark Hall (23/3/2010).
	    // a rather complicated process is needed to ensure that
	    // all resources from the scene graph and virtual universe
	    // are freed up properly (SimpleUniverse.cleanup() by itself
	    // does NOT free up everything, even though it is supposed
	    // to). The following came from:
	    // http://forums.java.net/jive/message.jspa?messageID=258387            
	    m_scene.detach();
	    View view = m_simpleU.getViewer().getView();
	    view.stopView();
	    view.stopBehaviorScheduler();
	    view.getCanvas3D(0).stopRenderer();
	    view.removeAllCanvas3Ds();
	    m_simpleU.removeAllLocales();

	    // free resources
	    m_simpleU.cleanup();
	    /*System.gc();
               System.runFinalization(); */
	  }
	}


	public void drawGraph() {
		// build the panel
		try {
		  remove(canvas3D);
		
		} catch (Exception e) {
		}
//		if (!m_canvasCreated) {
		  canvas3D = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
//		}

		int scrHeight = (new Double(this.getSize().getHeight())).intValue();
		int scrWidth = (new Double(this.getSize().getWidth())).intValue();
		
		canvas3D.setSize(scrWidth - 120, scrHeight - 50);
		
//		if (!m_canvasCreated) {
		add(canvas3D, java.awt.BorderLayout.CENTER);
		m_canvasCreated = true;
//		}
		
		freeResources();				
		
		// build the visualisation
                m_scene = createSceneGraph();

                // compile the scene
                m_scene.compile();

	        // build the universe
		m_simpleU = new SimpleUniverse(canvas3D);


		// add the behaviors to the ViewingPlatform
		ViewingPlatform viewingPlatform = m_simpleU.getViewingPlatform();

		viewingPlatform.setNominalViewingTransform();

		// add orbit behavior to ViewingPlatform
		orbit =
			new OrbitBehavior(
				canvas3D,
				OrbitBehavior.REVERSE_ALL | OrbitBehavior.STOP_ZOOM);
		BoundingSphere bounds =
			new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
		orbit.setSchedulingBounds(bounds);
		viewingPlatform.setViewPlatformBehavior(orbit);
		

		m_simpleU.addBranchGraph(m_scene);

	}

	/**
	 * create all the graph
	 * 
	 * @return a BranchGroup containing all the graph
	 */
	private BranchGroup createSceneGraph() {

		// create the main BranchGroup
		BranchGroup objRoot = new BranchGroup();

		objRoot.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objRoot.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

		// reckon the size of the graph
		int nbRules = rules.length;

		float[] maxC = new float[criteres.length];
		float[] minC = new float[criteres.length];

		int nRule, nCrit;
		Attribute attr;
		float val;
		Hashtable htAttrs = new Hashtable();

		for (nRule = 0; nRule < nbRules; nRule++) {
			//make a list of all attributes
			Iterator it = (rules[nRule].getCondition()).listIterator();
			while (it.hasNext()) {
				attr = (Attribute) it.next();
				htAttrs.put(attr.toString(), attr);
			}

			attr = rules[nRule].getConclusion();
			htAttrs.put(attr.toString(), attr);

			//find min & max of each criteres
			for (nCrit = 0; nCrit < criteres.length; nCrit++) {
				val = (rules[nRule].getCritere(criteres[nCrit]).getValue());

				if (nRule == 0) {
					maxC[nCrit] = val;
					minC[nCrit] = val;
				} else {
					if (maxC[nCrit] < val)
						maxC[nCrit] = val;
					if (minC[nCrit] > val)
						minC[nCrit] = val;
				}
			}
		}

		LinkedList lAttrs = new LinkedList(htAttrs.values());
		int nbAttrs = lAttrs.size();

		float size = .040f;		
//		float size = 0.85f/(nbAttrs+10);
//		float size = 0.05f - (0.0006f * nbRules);

		// create a new BranchGroup to turn the scene
		Transform3D tRotateX = new Transform3D();
		tRotateX.rotX(Math.PI / 6.0d);
		Transform3D tRotateY = new Transform3D();
		tRotateY.rotY(-Math.PI / 6.0d);
		tRotateX.mul(tRotateY);
		float minSize = Math.min(0.75f/(nbAttrs+10), 0.05f - (0.0006f * nbRules));



		Transform3D tScale = new Transform3D();
		tScale.setScale(minSize/size);
		tRotateX.mul(tScale);
		TransformGroup tgRotate = new TransformGroup(tRotateX);

		objRoot.addChild(tgRotate);

		// create a new BranchGroup 
		BranchGroup bgObjGraph = new BranchGroup();

		tgRotate.addChild(bgObjGraph);

		// Draw the graph
		int nAtt;
		boolean drawn;
		Rule currentRule;
		LinkedList ruleLHS;
		Iterator idxRulesLHS;
		for (nRule = 0; nRule < nbRules; nRule++) {
			currentRule = rules[nRule];
			for (nAtt = 0; nAtt < nbAttrs; nAtt++) {
				idxRulesLHS = (currentRule.getCondition()).listIterator();
				drawn = false;
				if (currentRule
					.getConclusion()
					.equals(((Attribute) lAttrs.get(nAtt)))) {
					//draw as a conclusion	
					bgObjGraph.addChild(
						createBox(
							(nRule * size) - (nbRules / 2 * size),
							nAtt * size,
							size,
							size,
							new Color3f(Color.RED)));
					drawn = true;
				}
				if (!drawn) {
					while (idxRulesLHS.hasNext()) {
						if (((Attribute) idxRulesLHS.next())
							.equals((Attribute) lAttrs.get(nAtt))) {
							//draw as an attribute	
							bgObjGraph.addChild(
								createBox(
									(nRule * size) - (nbRules / 2 * size),
									nAtt * size,
									size,
									size,
									new Color3f(Color.green)));
							drawn = true;
						}
					}
				}
				if (!drawn) {
					//draw as nothing	
					bgObjGraph.addChild(
						createBox(
							(nRule * size) - (nbRules / 2 * size),
							nAtt * size,
							size,
							size / 10,
							new Color3f(Color.BLACK)));
				}

				// draw criteres values
				for (nCrit = 0; nCrit < criteres.length; nCrit++) {
					val = (rules[nRule].getCritere(criteres[nCrit]).getValue());
					bgObjGraph.addChild(
						createBox((nRule*size)-(nbRules/2*size),
							-(nCrit+1)*size,
							size,
							5*size*nCrit+(val-minC[nCrit])*4*size/(maxC[nCrit]-minC[nCrit]),
							new Color3f(ColorArray.colorArray[nCrit + 3])));

				}

			}
			//draw rule ID	
			bgObjGraph.addChild(
				createTextY(
					(nRule * size) - (nbRules / 2 * size),
					nAtt * size,
					size,
					currentRule.getId(),
					new Color3f(Color.WHITE)));

		}

		// draw ladder
		int i;
		LineArray axis;
		for (i = 0; i < 10; i++) {
			axis = new LineArray(2, LineArray.COORDINATES | LineArray.COLOR_3);
			axis.setCoordinate(
				0,
				new Point3f(- (nbRules + 4) * size / 2, i * size, -size));
			axis.setCoordinate(
				1,
				new Point3f((nbRules + 4) * size / 2, i * size, -size));
			axis.setColor(0, new Color3f(1f, 0f, 1f));
			axis.setColor(1, new Color3f(1f, 0f, 1f));

			bgObjGraph.addChild(new Shape3D(axis));
		}

		//draw min/max
		for (nCrit = 0; nCrit < criteres.length; nCrit++) {
			bgObjGraph.addChild(
				createTextScale(
					(nbRules + 5) * size / 2,
					nCrit * 5 * size - size / 2,
					size,
					(new Float(minC[nCrit])).toString(),
					new Color3f(ColorArray.colorArray[nCrit + 3])));
			bgObjGraph.addChild(
				createTextScale(
					(nbRules + 5) * size / 2,
					nCrit * 5 * size + 4 * size - size / 2,
					size,
					(new Float(maxC[nCrit])).toString(),
					new Color3f(ColorArray.colorArray[nCrit + 3])));
			bgObjGraph.addChild(
				createTextScale(
					(nbRules + 8) * size / 2,
					nCrit * 5 * size + 2 * size - size / 2,
					size,
					criteres[nCrit],
					new Color3f(ColorArray.colorArray[nCrit + 3])));

		}

		// write attributes
		for (nAtt = 0; nAtt < nbAttrs; nAtt++) {
			bgObjGraph.addChild(
				createTextX(
					(nRule * size) + size - (nbRules / 2 * size),
					nAtt * size + size / 3,
					size,
					((Attribute) lAttrs.get(nAtt)).toString(),
					new Color3f(Color.WHITE)));
		}


		// draw legend
		bgObjGraph.addChild(createBox(- (nbRules / 2 * size),(nbAttrs+5) * size,size,size,new Color3f(Color.green)));
		bgObjGraph.addChild(createTextX( size - (nbRules / 2 * size),(nbAttrs+5) * size + size / 3,size,"ANTECEDENT",new Color3f(Color.WHITE)));
		bgObjGraph.addChild(createBox(- (nbRules / 2 * size),(nbAttrs+7) * size,size,size,new Color3f(Color.red)));
		bgObjGraph.addChild(createTextX( size - (nbRules / 2 * size),(nbAttrs+7) * size + size / 3,size,"CONSEQUENT",new Color3f(Color.WHITE)));
			

		return objRoot;
	}

	/**
	 * Create a text with the currents specifications following the X axis
	 * 
	 * @param x position on the X axis
	 * @param z position on the Z axis
	 * @param size height of the font
	 * @param str the string to write
	 * @param col the color of the text
	 * @return a TransformGroup which contains the text
	 */
	private TransformGroup createTextScale(
		float x,
		float y,
		float size,
		String str,
		Color3f col) {

		// create the text
		Text2D txt =
			new Text2D(
				str,
				col,
				"courrier",
				(new Float(300f * size)).intValue(),
				Font.BOLD);
		
		// make the text two-sided
	        Appearance app = txt.getAppearance();
	        PolygonAttributes polyAttrib = new PolygonAttributes();
	        polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
	        polyAttrib.setBackFaceNormalFlip(true);
	        app.setPolygonAttributes(polyAttrib);

		// move and rotate the text
		Transform3D translate = new Transform3D();
		translate.set(new Vector3f(x, y, -size));

		TransformGroup tg = new TransformGroup(translate);

		// apply Transform3D
		tg.addChild(txt);

		return tg;
	}

	/**
	 * Create a text with the currents specifications following the X axis
	 * 
	 * @param x position on the X axis
	 * @param z position on the Z axis
	 * @param size height of the font
	 * @param str the string to write
	 * @param col the color of the text
	 * @return a TransformGroup which contains the text
	 */
	private TransformGroup createTextX(
		float x,
		float z,
		float size,
		String str,
		Color3f col) {

		// create the text
		Text2D txt =
			new Text2D(
				str,
				col,
				"courrier",
				(new Float(300f * size)).intValue(),
				Font.BOLD);
		
		// make the text two-sided
	        Appearance app = txt.getAppearance();
	        PolygonAttributes polyAttrib = new PolygonAttributes();
	        polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
	        polyAttrib.setBackFaceNormalFlip(true);
	        app.setPolygonAttributes(polyAttrib);

		// move and rotate the text
		Transform3D translate = new Transform3D();
		translate.set(new Vector3f(x, 0.0f, z + size));

		Transform3D rotate = new Transform3D();
		rotate.rotX(-Math.PI / 2.0d);
		translate.mul(rotate);

		TransformGroup tg = new TransformGroup(translate);

		// apply Transform3D
		tg.addChild(txt);

		return tg;
	}

	/**
	 * Create a text with the currents specifications following the Y axis
	 * 
	 * @param x position on the X axis
	 * @param z position on the Z axis
	 * @param size height of the font
	 * @param str the string to write
	 * @param col the color of the text
	 * @return a TransformGroup which contains the text
	 */
	private TransformGroup createTextY(
		float x,
		float z,
		float size,
		String str,
		Color3f col) {

		// create the text
		Text2D txt =
			new Text2D(
				str,
				col,
				"courrier",
				(new Float(300f * size)).intValue(),
				Font.BOLD);
		
		// make the text two-sided
	        Appearance app = txt.getAppearance();
	        PolygonAttributes polyAttrib = new PolygonAttributes();
	        polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
	        polyAttrib.setBackFaceNormalFlip(true);
	        app.setPolygonAttributes(polyAttrib);

		// move and rotate the text
		Transform3D translate = new Transform3D();
		translate.set(new Vector3f(x, 0.0f, size + z));

		Transform3D rotate = new Transform3D();
		rotate.rotX(-Math.PI / 2.0d);
		translate.mul(rotate);
		rotate.rotZ(-Math.PI / 2.0d);
		translate.mul(rotate);

		TransformGroup tg = new TransformGroup(translate);

		// apply Transform3D
		tg.addChild(txt);

		return tg;
	}

	/**
	 * Create a box with these specifications, the origin of the Box is the bottom, front left corner
	 * The Box has the specified color and borders of the box are drawn
	 * 
	 * @param x position on the X axis
	 * @param z position on the Z axis
	 * @param size size of the box on the X & Z
	 * @param height height of the box on the Y axis
	 * @param col color of the box
	 * @return a transformGroup which contains the box	
	 */
	private TransformGroup createBox(
		float x,
		float z,
		float size,
		float height,
		Color3f col) {

		//quadrilatere
		QuadArray quad =
			new QuadArray(24, QuadArray.COORDINATES | QuadArray.COLOR_3);
		quad.setCoordinate(0, new Point3f(0.001f, height - 0.001f, 0.001f));
		quad.setCoordinate(
			1,
			new Point3f(size - 0.001f, height - 0.001f, 0.001f));
		quad.setCoordinate(2, new Point3f(size - 0.001f, 0.001f, 0.001f));
		quad.setCoordinate(3, new Point3f(0.001f, 0.001f, 0.001f));

		quad.setCoordinate(4, new Point3f(0.001f, 0.001f, size - 0.001f));
		quad.setCoordinate(
			5,
			new Point3f(size - 0.001f, 0.001f, size - 0.001f));
		quad.setCoordinate(
			6,
			new Point3f(size - 0.001f, height - 0.001f, size - 0.001f));
		quad.setCoordinate(
			7,
			new Point3f(0.001f, height - 0.001f, size - 0.001f));

		quad.setCoordinate(8, new Point3f(0.001f, 0.001f, 0.001f));
		quad.setCoordinate(9, new Point3f(0.001f, 0.001f, size - 0.001f));
		quad.setCoordinate(
			10,
			new Point3f(0.001f, height - 0.001f, size - 0.001f));
		quad.setCoordinate(11, new Point3f(0.001f, height - 0.001f, 0.001f));

		quad.setCoordinate(
			12,
			new Point3f(size - 0.001f, height - 0.001f, 0.001f));
		quad.setCoordinate(
			13,
			new Point3f(size - 0.001f, height - 0.001f, size - 0.001f));
		quad.setCoordinate(
			14,
			new Point3f(size - 0.001f, 0.001f, size - 0.001f));
		quad.setCoordinate(15, new Point3f(size - 0.001f, 0.001f, 0.001f));

		quad.setCoordinate(16, new Point3f(size - 0.001f, 0.001f, 0.001f));
		quad.setCoordinate(
			17,
			new Point3f(size - 0.001f, 0.001f, size - 0.001f));
		quad.setCoordinate(18, new Point3f(0.001f, 0.001f, size - 0.001f));
		quad.setCoordinate(19, new Point3f(0.001f, 0.001f, 0.001f));

		quad.setCoordinate(20, new Point3f(0.001f, height - 0.001f, 0.001f));
		quad.setCoordinate(
			21,
			new Point3f(0.001f, height - 0.001f, size - 0.001f));
		quad.setCoordinate(
			22,
			new Point3f(size - 0.001f, height - 0.001f, size - 0.001f));
		quad.setCoordinate(
			23,
			new Point3f(size - 0.001f, height - 0.001f, 0.001f));

		for (int i = 0; i < 24; i++)
			quad.setColor(i, col);

		//dessine les aretes

		//quadrilatere
		QuadArray aretes =
			new QuadArray(24, QuadArray.COORDINATES | QuadArray.COLOR_3);
		aretes.setCoordinate(0, new Point3f(0.0f, 0.0f, 0.0f));
		aretes.setCoordinate(1, new Point3f(size, 0.0f, 0.0f));
		aretes.setCoordinate(2, new Point3f(size, height, 0.0f));
		aretes.setCoordinate(3, new Point3f(0.0f, height, 0.0f));

		aretes.setCoordinate(4, new Point3f(0.0f, 0.0f, size));
		aretes.setCoordinate(5, new Point3f(size, 0.0f, size));
		aretes.setCoordinate(6, new Point3f(size, height, size));
		aretes.setCoordinate(7, new Point3f(0.0f, height, size));

		aretes.setCoordinate(8, new Point3f(0.0f, 0.0f, 0.0f));
		aretes.setCoordinate(9, new Point3f(0.0f, 0.0f, size));
		aretes.setCoordinate(10, new Point3f(0.0f, height, size));
		aretes.setCoordinate(11, new Point3f(0.0f, height, 0.0f));

		aretes.setCoordinate(12, new Point3f(size, 0.0f, 0.0f));
		aretes.setCoordinate(13, new Point3f(size, 0.0f, size));
		aretes.setCoordinate(14, new Point3f(size, height, size));
		aretes.setCoordinate(15, new Point3f(size, height, 0.0f));

		for (int i = 0; i < 24; i++)
			aretes.setColor(i, new Color3f(1f, 1f, 1f));

		// move the box
		Transform3D translate = new Transform3D();
		translate.set(new Vector3f(x, 0.0f, z));

		TransformGroup tg = new TransformGroup(translate);

		// create the box
		tg.addChild(new Shape3D(quad));
		tg.addChild(new Shape3D(aretes, lineApp()));
		return tg;
	}

	/**
	 *  Define an appearance to draw only vertices of a shape
	 *
	 * @return the appearance
	 */
	Appearance lineApp() {
		Appearance app = new Appearance();
		app.setPolygonAttributes(
			new PolygonAttributes(
				PolygonAttributes.POLYGON_LINE,
				PolygonAttributes.CULL_NONE,
				0f));
		return app;
	}

}
