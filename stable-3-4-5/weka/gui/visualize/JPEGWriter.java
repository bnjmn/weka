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
  *    JPEGWriter.java
  *    Copyright (C) 2005 Fracpete
  *
  */

package weka.gui.visualize;

import com.sun.image.codec.jpeg.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JComponent;

/** 
 * This class takes any JComponent and outputs it to a JPEG-file.
 * Scaling is by default disabled, since we always take a screenshot.
 *
 * @see #setScalingEnabled()
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1.2.1 $
 */
public class JPEGWriter extends JComponentWriter {
  /** the quality of the image */
  private float quality;
  /** the background color */
  private Color background;
  
  /**
   * initializes the object 
   */
  public JPEGWriter() {
    super();
  }

  /**
   * initializes the object with the given Component
   * 
   * @param c         the component to print in the output format
   */
  public JPEGWriter(JComponent c) {
    super(c);
  }

  /**
   * initializes the object with the given Component and filename
   * 
   * @param c         the component to print in the output format
   * @param f         the file to store the output in
   */
  public JPEGWriter(JComponent c, File f) {
    super(c, f);
    
    quality    = 1.0f;
    background = Color.WHITE;
  }
  
  /**
   * further initialization 
   */
  public void initialize() {
    super.initialize();
    
    quality    = 1.0f;
    background = Color.WHITE;
    setScalingEnabled(false);
  }

  /**
   * returns the name of the writer, to display in the FileChooser.
   * must be overridden in the derived class.
   */
  public String getDescription() {
    return "JPEG-Image";
  }
  
  /**
   * returns the extension (incl. ".") of the output format, to use in the
   * FileChooser. 
   * must be overridden in the derived class.
   */
  public String getExtension() {
    return ".jpg";
  }
  
  /**
   * returns the current background color
   */
  public Color getBackground() {
    return background;
  }
  
  /**
   * sets the background color to use in creating the JPEG
   */
  public void setBackground(Color c) {
    background = c;
  }
  
  /**
   * returns the quality the JPEG will be stored in
   */
  public float getQuality() {
    return quality;
  }
  
  /**
   * sets the quality the JPEG is saved in 
   */
  public void setQuality(float q) {
    quality = q;
  }
  
  /**
   * outputs the given component as JPEG in the specified file
   * 
   * @param c           the component to output as PS
   * @param f           the file to store the PS in 
   * @throws Exception  if component of file are <code>null</code>
   */
  public static void toOutput(JComponent c, File f) throws Exception {
    JComponentWriter        writer;
    
    writer = new JPEGWriter(c, f);
    writer.toOutput();
  }
  
  /**
   * saves the current component to the currently set file
   *
   * @throws Exception  if component of file are <code>null</code>
   */
  public void toOutput() throws Exception {
    BufferedImage                bi;
    JPEGImageEncoder             encoder;
    JPEGEncodeParam              param;
    Graphics                     g;
    BufferedOutputStream         ostream;

    ostream = new BufferedOutputStream(new FileOutputStream(getFile()));
    bi      = new BufferedImage(getComponent().getWidth(), getComponent().getHeight(), BufferedImage.TYPE_INT_RGB);
    g       = bi.getGraphics();
    g.setPaintMode();
    g.setColor(getBackground());
    if (g instanceof Graphics2D)
      ((Graphics2D) g).scale(getXScale(), getYScale());
    g.fillRect(0, 0, getComponent().getWidth(), getComponent().getHeight());
    getComponent().paint(g);
    encoder = JPEGCodec.createJPEGEncoder(ostream);
    param   = encoder.getDefaultJPEGEncodeParam(bi);
    param.setQuality(getQuality(), false);
    encoder.setJPEGEncodeParam(param);
    encoder.encode(bi);
    ostream.flush();
    ostream.close();
  }
  
  /**
   * for testing only 
   */
  public static void main(String[] args) throws Exception {
    System.out.println("building TreeVisualizer...");
    weka.gui.treevisualizer.TreeBuild builder = new weka.gui.treevisualizer.TreeBuild();
    weka.gui.treevisualizer.NodePlace arrange = new weka.gui.treevisualizer.PlaceNode2();
    weka.gui.treevisualizer.Node top = builder.create(new java.io.StringReader("digraph atree { top [label=\"the top\"] a [label=\"the first node\"] b [label=\"the second nodes\"] c [label=\"comes off of first\"] top->a top->b b->c }"));
    weka.gui.treevisualizer.TreeVisualizer tv = new weka.gui.treevisualizer.TreeVisualizer(null, top, arrange);
    tv.setSize(800 ,600);
    
    String filename = System.getProperty("java.io.tmpdir") + "test.jpg";
    System.out.println("outputting to '" + filename + "'...");
    toOutput(tv, new File(filename));

    System.out.println("done!");
  }
}
