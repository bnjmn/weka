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
 *    VisualizePanel3D.java
 *    Copyright (C) 2010 Pentaho Corporation
 *
 */

package weka.gui.visualize;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.FileReader;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineArray;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Text2D;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;

/**
 * Panel that displays a 3D scatter plot. Requires Java 3D to be installed.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class VisualizePanel3D extends JPanel {
  
  /**
   * For serialization
   */
  private static final long serialVersionUID = -7632942453082374589L;

  /** default colors for coloring discrete class */
  protected static Color [] DEFAULT_COLORS = {Color.blue,
    Color.red,
    Color.green,
    Color.cyan,
    Color.pink,
    new Color(255, 0, 255),
    Color.orange,
    new Color(255, 0, 0),
    new Color(0, 255, 0),
    Color.white};
  
  /** The instances to be plotted */
  protected Instances m_plotInstances;
  
  /** The index of the attribute for the x axis */
  protected int m_xIndex;
  
  /** The index of the attribute for the y axis */
  protected int m_yIndex;
  
  /** The index of the attribute for the z axis */
  protected int m_zIndex;
  
  /** The index of the attribute for coloring */
  protected int m_colorIndex;
  
  protected double m_minX;
  protected double m_maxX;
  protected double m_minY;
  protected double m_maxY;
  protected double m_minZ;
  protected double m_maxZ;
  protected double m_minC;
  protected double m_maxC;
  
  /** A canvas 3D for displaying on */
  protected Canvas3D m_canvas3D;
  
  /** The orbit behavior */
  protected OrbitBehavior m_orbit;
  
  /** The universe to use */
  protected SimpleUniverse m_simpleU;
  
  /** Holds the scene to be rendered */
  protected BranchGroup m_scene;
  
  public VisualizePanel3D() {
    setLayout(new BorderLayout());
  }
  
  /**
   * Sets the indexes of the axis to be visualized (0-based indexing).
   * 
   * @param x the x index
   * @param y the y index
   * @param z the z index
   * @param c the color index
   */
  public void setAxes(int x, int y, int z, int c) {
    if (m_plotInstances == null) {
      return;
    }
    
    m_xIndex = x;
    m_yIndex = y;
    m_zIndex = z;
    m_colorIndex = c;
    int numAttributes = m_plotInstances.numAttributes();
    
    if (m_xIndex > numAttributes - 1 || m_yIndex > numAttributes - 1 ||
        m_zIndex > numAttributes - 1 || m_colorIndex > numAttributes - 1) {
      return;
    }
    
    if (m_plotInstances.attribute(x).isNumeric()) {
      m_minX = m_plotInstances.attributeStats(x).numericStats.min;
      m_maxX = m_plotInstances.attributeStats(x).numericStats.max;
    } else {
      m_minX = 0;
      m_maxX = m_plotInstances.attribute(x).numValues() - 1;
    }
    
    if (m_plotInstances.attribute(y).isNumeric()) {
      m_minY = m_plotInstances.attributeStats(y).numericStats.min;
      m_maxY = m_plotInstances.attributeStats(y).numericStats.max;
    } else {
      m_minY = 0;
      m_maxY = m_plotInstances.attribute(y).numValues() - 1;
    }
    
    if (m_plotInstances.attribute(z).isNumeric()) {
      m_minZ = m_plotInstances.attributeStats(z).numericStats.min;
      m_maxZ = m_plotInstances.attributeStats(z).numericStats.max;
    } else {
      m_minZ = 0;
      m_maxZ = m_plotInstances.attribute(z).numValues() - 1;
    }
    
    if (m_plotInstances.attribute(c).isNumeric()) {
      m_minC = m_plotInstances.attributeStats(c).numericStats.min;
      m_maxC = m_plotInstances.attributeStats(c).numericStats.max;
    }
    
    drawGraph();
  }
  
  /**
   * Set the instances to display
   * 
   * @param toPlot the instances to display
   * @param x the index of the attribute for the x axis
   * @param y the index of the attribute for the y axis
   * @param z the index of the attribute for the z axis
   * @param c the index of the attribute for coloring
   */
  public void setInstances(Instances toPlot, int x, int y, int z, int c) {
    m_plotInstances = toPlot;
    setAxes(x, y, z, c);
  }
  
  /**
   * Set the instances to display. No rendering is done until
   * the setAxis method is called.
   * 
   * @param toPlot the instances to display.
   */
  public void setInstances(Instances toPlot) {
    m_plotInstances = toPlot;
  }
  
  /**
   * Free memory used by the display.
   */
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
  
  /**
   * Set up resources and compose the scene graph.
   */
  protected void drawGraph() {
    if (m_plotInstances == null) {
      return; // nothing to do!
    }
    
    try {
      remove(m_canvas3D);
    } catch (Exception ex) {}
    
    m_canvas3D = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
    
    int scrHeight = (new Double(this.getSize().getHeight())).intValue();
    int scrWidth = (new Double(this.getSize().getWidth())).intValue();
    
    m_canvas3D.setSize(scrWidth, scrHeight);
    
    add(m_canvas3D, BorderLayout.CENTER);
    freeResources();
    
    // build the visualization
    m_scene = createSceneGraph();
    
    
    //m_scene.addChild(light1);
    
    // compile the scene
    m_scene.compile();
    
    // build the universe
    m_simpleU = new SimpleUniverse(m_canvas3D);
    
    // add the behaviors to the ViewingPlatform
    ViewingPlatform viewingPlatform = m_simpleU.getViewingPlatform();

    viewingPlatform.setNominalViewingTransform();

    // add orbit behavior to ViewingPlatform
    m_orbit = new OrbitBehavior(m_canvas3D, 
        OrbitBehavior.REVERSE_ALL | OrbitBehavior.STOP_ZOOM);
    
    BoundingSphere bounds =
      new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
    
    m_orbit.setSchedulingBounds(bounds);
    viewingPlatform.setViewPlatformBehavior(m_orbit);
    

    m_simpleU.addBranchGraph(m_scene);
  }
  
  /**
   * Creates the BranchGroup that contains the scene graph.
   * 
   * @return a BranchGroup containing the scene to be rendered.
   */
  protected BranchGroup createSceneGraph() {
    // create the main BranchGroup
    BranchGroup objRoot = new BranchGroup();

    objRoot.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    objRoot.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    
    /*double xRange = Math.max(Math.abs(m_maxX), Math.abs(m_minX));
    double yRange = Math.max(Math.abs(m_maxY), Math.abs(m_minY));
    double zRange = Math.max(Math.abs(m_maxZ), Math.abs(m_minZ)); */
    double xRange = m_maxX - m_minX;
    double yRange = m_maxY - m_minY;
    double zRange = m_maxZ - m_minZ;
    double xScale = 1.0 / xRange;
    double yScale = 1.0 / yRange;
    double zScale = 1.0 / zRange;
    
    // create a new BranchGroup to turn the scene
    Transform3D tRotateX = new Transform3D();
    tRotateX.rotX(Math.PI / 6.0);
    Transform3D tRotateY = new Transform3D();
    tRotateY.rotY(-Math.PI / 6.0);
    tRotateX.mul(tRotateY);
    //float minSize = Math.min(0.75f/(nbAttrs+10), 0.05f - (0.0006f * nbRules));
    double scaleS = 1.0;
    Transform3D tScale = new Transform3D();
    tScale.setScale(scaleS);
    tRotateX.mul(tScale);
    TransformGroup tgRotate = new TransformGroup(tRotateX);
    objRoot.addChild(tgRotate);
    
    BranchGroup bgObjGraph = new BranchGroup();
    tgRotate.addChild(bgObjGraph);
    
    addAxes(bgObjGraph);

    for (int i = 0; i < m_plotInstances.numInstances(); i++) {
      Instance current = m_plotInstances.instance(i);
      if (current.isMissing(m_xIndex) || current.isMissing(m_yIndex) || current.isMissing(m_zIndex)) {
        continue;
      }

      Primitive shape = null;
      shape = new Sphere(0.01f);
      Appearance appearance = new Appearance();
      float red = 255.0f, green = 255.0f, blue = 255.0f;
      if (m_colorIndex >= 0 && m_colorIndex < m_plotInstances.numAttributes()) {
        if (current.isMissing(m_colorIndex)) {
          // use a cone for missing class values
          shape = new Cone(0.01f, 0.015f);
        } else if (current.attribute(m_colorIndex).isNominal()) {
          Color temp = DEFAULT_COLORS[(int)current.value(m_colorIndex) % 10];
          red = temp.getRed();
          green = temp.getGreen();
          blue = temp.getBlue();
        } else {
          double r = (current.value(m_colorIndex) - m_minC) /
            (m_maxC - m_minC);
          r = (r * 240) + 15;
          red = (float)r;
          blue = 150f;
          green = (255f - (float)r);
        }
      }
      red /=  255f;
      blue /= 255f;
      green /= 255f;
      
      Color3f col = new Color3f(red, green, blue);
      ColoringAttributes ca = new ColoringAttributes(col, ColoringAttributes.NICEST);
      appearance.setColoringAttributes(ca);
      shape.setAppearance(appearance);
      //    Box box = new Box();
      //ColorCube cube = new ColorCube(0.05);

      TransformGroup tg = new TransformGroup();
      Transform3D transform = new Transform3D();
      /*double newX = current.value(m_xIndex) * xScale;
      double newY = current.value(m_yIndex) * yScale;
      double newZ = current.value(m_zIndex) * zScale; */
      double newX = (current.value(m_xIndex) - m_minX) / xRange * 2.0 -1.0;
      double newY = (current.value(m_yIndex) - m_minY) / yRange * 2.0 -1.0;
      double newZ = (current.value(m_zIndex) - m_minZ) / zRange * 2.0 -1.0;
      if (Double.isNaN(newX) || Double.isNaN(newY) || Double.isNaN(newZ)) {
        continue;
      }
      Vector3d vector = new Vector3d(newX, newY, newZ);
      transform.setTranslation(vector);
      tg.setTransform(transform);
      tg.addChild(shape);
      bgObjGraph.addChild(tg);
    }
    
    addLegend(bgObjGraph);

    Color3f light1Color = new Color3f(1f, 1f, 1f);
    Vector3f light1Direction = new Vector3f(4.0f, -7.0f, -12.0f);
    AmbientLight light1 = new AmbientLight(true, light1Color);
    BoundingSphere bounds =
      new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
    light1.setInfluencingBounds(bounds);
    
    bgObjGraph.addChild(light1);
    
    return objRoot;
  }
  
  private String formatNumberString(double num) {
    int whole = (int)Math.abs(num);
    double decimal = Math.abs(num) - whole;
    int nondecimal;
    nondecimal = (whole > 0) 
    ? (int)(Math.log(whole) / Math.log(10))
        : 1;

    int precision = (decimal > 0) 
    ? (int)Math.abs(((Math.log(Math.abs(num)) / 
        Math.log(10))))+2
        : 1;
    if (precision > VisualizeUtils.MAX_PRECISION) {
      precision = 1;
    }

    String retString = Utils.doubleToString(num,
        nondecimal + 1 + precision
        ,precision);
    
    return retString;
  }
  
  private void addAxes(BranchGroup group) {
    LineArray xAxis = new LineArray(2, LineArray.COORDINATES | LineArray.COLOR_3);
    xAxis.setCoordinate(0, new Point3d(-1, -1, -1));
    xAxis.setCoordinate(1, new Point3d(1, -1, -1));
    xAxis.setColor(0, new Color3f(1f, 0f, 1f));
    xAxis.setColor(1, new Color3f(1f, 0f, 1f));
    
    group.addChild(new Shape3D(xAxis));
    String xLabel = " " + m_plotInstances.attribute(m_xIndex).name();
    group.addChild(createTextX(0f, -1.05f, -1.05f, 0.04f, xLabel, false, new Color3f(Color.white)));

    if (m_plotInstances.attribute(m_xIndex).isNumeric()) {      
      String maxStringX = formatNumberString(m_maxX);

      group.addChild(createTextX(1.05f, -1.05f, -1.05f, 0.04f, maxStringX, false, new Color3f(Color.white)));
      
      String minStringX = formatNumberString(m_minX);
      group.addChild(createTextX(-1.05f, -1.05f, -1.05f, 0.04f, minStringX, false, new Color3f(Color.white)));
    } else {
      int numLabels = m_plotInstances.attribute(m_xIndex).numValues();
      
      // max of 50 labels
      if (numLabels <= 2.0 / 0.04) {
        float start = -1.0f;
        float incr = 2.0f / (float)(numLabels - 1);
        for (int i = 0; i < numLabels; i++) {
          String label = m_plotInstances.attribute(m_xIndex).value(i);
          group.addChild(createTextX(start + 0.04f, -1.0f, -1.08f, 0.04f, label, true, new Color3f(Color.white)));
          start += incr;
        }
      }
    }
    
    LineArray yAxis = new LineArray(2, LineArray.COORDINATES | LineArray.COLOR_3);
    yAxis.setCoordinate(0, new Point3d(-1, -1, -1));
    yAxis.setCoordinate(1, new Point3d(-1, 1, -1));
    yAxis.setColor(0, new Color3f(1f, 0f, 1f));
    yAxis.setColor(1, new Color3f(1f, 0f, 1f));
    
    group.addChild(new Shape3D(yAxis));
    String yLabel = m_plotInstances.attribute(m_yIndex).name();
    group.addChild(createTextY(-1f, 0f, -1.05f, 0.04f, yLabel, true, new Color3f(Color.white)));
    
    if (m_plotInstances.attribute(m_yIndex).isNumeric()) {      
      String maxStringY = formatNumberString(m_maxY);
      group.addChild(createTextY(-1f, 1.05f, -1.05f, 0.04f, maxStringY, false, new Color3f(Color.white)));


      String minStringY = formatNumberString(m_minY);
      group.addChild(createTextY(-1f, -1f, -1.05f, 0.04f, minStringY, false, new Color3f(Color.white)));
    } else {
      int numLabels = m_plotInstances.attribute(m_yIndex).numValues();
      
      // max of 50 labels
      if (numLabels <= 2.0 / 0.04) {
        float start = -1.0f;
        float incr = 2.0f / (float)(numLabels - 1);
        for (int i = 0; i < numLabels; i++) {
          String label = m_plotInstances.attribute(m_yIndex).value(i);
          group.addChild(createTextY(-1f, start, -1.05f, 0.04f, label, false, new Color3f(Color.white)));
          start += incr;
        }
      }
    }


    LineArray zAxis = new LineArray(2, LineArray.COORDINATES | LineArray.COLOR_3);
    zAxis.setCoordinate(0, new Point3d(-1, -1, -1));
    zAxis.setCoordinate(1, new Point3d(-1, -1, 1));
    zAxis.setColor(0, new Color3f(1f, 0f, 1f));
    zAxis.setColor(1, new Color3f(1f, 0f, 1f));
    
    group.addChild(new Shape3D(zAxis));
    String zLabel = m_plotInstances.attribute(m_zIndex).name();
    group.addChild(createTextZ(-1.05f, -1.05f, 0f, 0.04f, zLabel, true, new Color3f(Color.white)));
    
    if (m_plotInstances.attribute(m_zIndex).isNumeric()) {      
      String maxStringZ = formatNumberString(m_maxZ);
      group.addChild(createTextZ(-1.05f, -1.05f, 1f, 0.04f, maxStringZ, true, new Color3f(Color.white)));

      String minStringZ = formatNumberString(m_minZ);
      group.addChild(createTextZ(-1.05f, -1.05f, -1f, 0.04f, minStringZ, true, new Color3f(Color.white)));
    } else {
      int numLabels = m_plotInstances.attribute(m_zIndex).numValues();
      
      // max of 50 labels
      if (numLabels <= 2.0 / 0.04) {
        float start = -1.0f;
        float incr = 2.0f / (float)(numLabels - 1);
        for (int i = 0; i < numLabels; i++) {
          String label = m_plotInstances.attribute(m_zIndex).value(i);
          group.addChild(createTextZ(-1f, -1.05f, start, 0.04f, label, false, new Color3f(Color.white)));
          start += incr;
        }
      }
    }
    
  }
  
  private void addLegend(BranchGroup group) {
    if (m_plotInstances.attribute(m_colorIndex).isNominal()) {
      float ypos = 0.5f;
      for (int i = 0; i < m_plotInstances.attribute(m_colorIndex).numValues(); i++) {
        group.addChild(createTextX(1.05f, ypos, 0f, 0.04f, 
            m_plotInstances.attribute(m_colorIndex).value(i), false, new Color3f(DEFAULT_COLORS[i % 10])));
        ypos -= 0.04f;
      }
    } else {
      float ypos = 0.5f;
      for (int i = 100; i >= 0; i--) {
        float red = 2.55f * (float)i;
        float blue = 150f;
        float green = (255f - red);
//        System.err.println("" + red + " " + green + " " + blue);

        red /= 255f;
        blue /= 255f;
        green /= 255f;
        
        LineArray aLine = new LineArray(2, LineArray.COORDINATES | LineArray.COLOR_3);
        aLine.setCoordinate(0, new Point3d(1.05, ypos, 0));
        aLine.setCoordinate(1, new Point3d(1.1, ypos, 0));
        aLine.setColor(0, new Color3f(red, green, blue));
        aLine.setColor(1, new Color3f(red, green, blue));
        group.addChild(new Shape3D(aLine));
        ypos -= 0.01f;
      }
      
      String minStringC = formatNumberString(m_minC);
      String maxStringC = formatNumberString(m_maxC);
      String colorL = m_plotInstances.attribute(m_colorIndex).name();
      group.addChild(createTextX(1.05f, 0.5f + 0.04f, 0f, 0.04f, maxStringC, false, new Color3f(Color.white)));
      group.addChild(createTextX(1.05f, -0.5f - 0.08f, 0f, 0.04f, minStringC, false, new Color3f(Color.white)));
      group.addChild(createTextY(1f, 0f, -0f, 0.04f, colorL, true, new Color3f(Color.white)));
    }
  }
  
  /** 
   * A reference fixed-width font character - used for estimating widths
   * of strings.
   */
  private static Text2D REF_CHAR =
    new Text2D(
        "A",
        new Color3f(1f, 1f, 1f),
        "Monospaced",
        (new Float(300f * 0.04f)).intValue(),
        Font.BOLD);
  
  private static float CHAR_WIDTH = 0f;
  
  static {
    Point3d upper = new Point3d();
    Point3d lower = new Point3d();
    BoundingBox bounds = (BoundingBox)REF_CHAR.getBounds();
    bounds.getUpper(upper);
    bounds.getLower(lower);
    CHAR_WIDTH = (float)Math.abs(upper.x - lower.x);;
  }
  
  
  /**
   * Create a text with the currents specifications following the X axis
   * 
   * @param x position on the X axis
   * @param y position on the Y axis
   * @param z position on the Z axis
   * @param size height of the font
   * @param str the string to write
   * @param col the color of the text
   * @return a TransformGroup which contains the text
   */
  private TransformGroup createTextX(float x, float y, float z, float size,
      String str, boolean rotXZ, Color3f col) {

    // create the text
    Text2D txt = 
      new Text2D(str, col, "Monospaced", 
          (new Float(300f * size)).intValue(), Font.BOLD);


    // make the text two-sided
    Appearance app = txt.getAppearance();
    PolygonAttributes polyAttrib = new PolygonAttributes();
    polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
    polyAttrib.setBackFaceNormalFlip(true);
    app.setPolygonAttributes(polyAttrib);          

    // move and rotate the text
    Transform3D translate = new Transform3D();
    if (x < 0 && !rotXZ) {
      x -= (str.length() * CHAR_WIDTH);
    }
    translate.set(new Vector3f(x, y, z));

    if (rotXZ) {
      Transform3D rotate = new Transform3D();
      rotate.rotX(-Math.PI / 2.0d);
      translate.mul(rotate);

      rotate.rotZ(Math.PI / 2.0d);
      translate.mul(rotate);
    }

    TransformGroup tg = new TransformGroup(translate);

    // apply Transform3D
    tg.addChild(txt);

    return tg;
  }
  
  /**
   * Create a text with the currents specifications following the Y axis
   * 
   * @param x position on the X axis
   * @param y position on the Y axis
   * @param z position on the Z axis
   * @param size height of the font
   * @param str the string to write
   * @param col the color of the text
   * @return a TransformGroup which contains the text
   */
  private TransformGroup createTextY(float x, float y, float z, float size, 
      String str, boolean zRotate, Color3f col) {

    // create the text
    Text2D txt = 
      new Text2D(str, col, "Monospaced", 
          (new Float(300f * size)).intValue(), Font.BOLD);

    // make the text two-sided
    Appearance app = txt.getAppearance();
    PolygonAttributes polyAttrib = new PolygonAttributes();
    polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
    polyAttrib.setBackFaceNormalFlip(true);
    app.setPolygonAttributes(polyAttrib);          

    // move and rotate the text
    Transform3D translate = new Transform3D();
    if (x < 0 && !zRotate) {
      // x -= width;
      x -= (str.length() * CHAR_WIDTH);
    }
    translate.set(new Vector3f(x, y, z));

    if (zRotate) {
      Transform3D rotate = new Transform3D();
      rotate.rotZ(Math.PI / 2.0d);
      /*          translate.mul(rotate);
          rotate.rotZ(-Math.PI / 2.0); */
      translate.mul(rotate);
    }

    TransformGroup tg = new TransformGroup(translate);

    // apply Transform3D
    tg.addChild(txt);

    return tg;
  }
  
  /**
   * Create a text with the currents specifications following the Z axis
   * 
   * @param x position on the X axis
   * @param y position on the Y axis
   * @param z position on the Z axis
   * @param size height of the font
   * @param str the string to write
   * @param col the color of the text
   * @return a TransformGroup which contains the text
   */
  private TransformGroup createTextZ(float x, float y, float z, float size,
      String str, boolean yRotate, Color3f col) {

    // create the text
    Text2D txt = 
      new Text2D(str, col, "Monospaced", 
          (new Float(300f * size)).intValue(), Font.BOLD);

    // make the text two-sided
    Appearance app = txt.getAppearance();
    PolygonAttributes polyAttrib = new PolygonAttributes();
    polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
    polyAttrib.setBackFaceNormalFlip(true);
    app.setPolygonAttributes(polyAttrib);

    // move and rotate the text
    Transform3D translate = new Transform3D();
    if (z >= 1 && yRotate) {
      z += (str.length() * CHAR_WIDTH);
    } else if (!yRotate) {
      x -= (str.length() * CHAR_WIDTH);
    }
    translate.set(new Vector3f(x, y, z + size));

    if (yRotate) {
      Transform3D rotate = new Transform3D();
      rotate.rotY(Math.PI / 2.0d);
      /*          translate.mul(rotate);
          rotate.rotZ(-Math.PI / 2.0); */
      translate.mul(rotate);
    } else {
      // rotate around x for nominal labels
      Transform3D rotate = new Transform3D();
      rotate.rotX(-Math.PI / 2.0d);
      translate.mul(rotate);
    }

    TransformGroup tg = new TransformGroup(translate);

    // apply Transform3D
    tg.addChild(txt);

    return tg;
  }
  
  /**
   * Main method for testing this class. All indexes are 1-based.
   * 
   * @param args an array of command line arguments: <br><br>
   * 
   * VisualizePanel3D <arff file> <x-index> <y-index> <z-index> <color-index>
   */
  public static void main(String[] args) {
    try {
      Instances insts = new Instances(new BufferedReader(new FileReader(args[0])));
      
      if (args.length < 5) {
        System.out.println("Usage:\n\t VisualizePanel3D <arff file> <x att> <y att> <z att> <color att>");
        System.exit(1);
      }
      final VisualizePanel3D panel = new VisualizePanel3D();
      int x = Integer.parseInt(args[1]) - 1;
      int y = Integer.parseInt(args[2]) - 1;
      int z = Integer.parseInt(args[3]) - 1;
      int col = Integer.parseInt(args[4]) - 1;;
      
      panel.setInstances(insts, x, y, z, col);
      
      final JFrame frame = new JFrame("Vis 3D");
      frame.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          panel.freeResources();
          frame.dispose();
          System.exit(1);
        }
      });
      frame.setSize(800, 600);
      frame.setContentPane(panel);
      frame.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
