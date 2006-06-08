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
  *    JComponentWriter.java
  *    Copyright (C) 2005 Fracpete
  *
  */

package weka.gui.visualize;

import java.io.File;
import javax.swing.JComponent;

/** 
 * This class takes any JComponent and outputs it to a file. Scaling is by
 * default enabled.
 *
 * @see #setScalingEnabled(boolean)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public abstract class JComponentWriter {
  /** whether to print some debug information */
  protected final static boolean DEBUG = false;
  
  /** output if we're in debug mode */
  static {
    if (DEBUG)
      System.err.println(JComponentWriter.class.getName() + ": DEBUG ON");
  }
  
  /** the component to print in the output format */
  private JComponent component;
  
  /** the file to write the output stream to */
  private File outputFile;
  
  /** the x scale factor */
  protected double m_xScale;
  
  /** the y scale factor */
  protected double m_yScale;

  /** whether scaling is enabled */
  protected boolean m_ScalingEnabled;
  
  /**
   * initializes the object 
   */
  public JComponentWriter() {
    this(null);
  }
  
  /**
   * initializes the object with the given Component
   * 
   * @param c         the component to print in the output format
   */
  public JComponentWriter(JComponent c) {
    this(c, null);
  }
  
  /**
   * initializes the object with the given Component and filename
   * 
   * @param c         the component to print in the output format
   * @param f         the file to store the output in
   */
  public JComponentWriter(JComponent c, File f) {
    component  = c;
    outputFile = f;
    
    initialize();
  }
  
  /**
   * further initialization can take place here 
   */
  protected void initialize() {
    m_xScale = 1.0;
    m_yScale = 1.0;
    m_ScalingEnabled = true;
  }
  
  /**
   * sets the component to print to an output format
   * 
   * @param c the component to print
   */
  public void setComponent(JComponent c) {
    component = c;
  }
  
  /**
   * returns the component that is stored in the output format
   * 
   * @return the component to print
   */
  public JComponent getComponent() {
    return component;
  }
  
  /**
   * sets the file to store the output in
   * 
   * @param f the file to store the output in
   */
  public void setFile(File f) {
    outputFile = f;
  }
  
  /**
   * returns the file being used for storing the output
   * 
   * @return the file to store the output in
   */
  public File getFile() {
    return outputFile;
  }
  
  /**
   * returns the name of the writer, to display in the FileChooser.
   * must be overridden in the derived class.
   * 
   * @return the name of the writer
   */
  public String getDescription() {
    return null;
  }
  
  /**
   * returns the extension (incl. ".") of the output format, to use in the
   * FileChooser. 
   * must be overridden in the derived class.
   * 
   * @return the file extension
   */
  public String getExtension() {
    return null;
  }
  
  /**
   * whether scaling is enabled or ignored
   * 
   * @return true if scaling is enabled
   */
  public boolean getScalingEnabled() {
    return m_ScalingEnabled;
  }
  
  /**
   * sets whether to enable scaling
   * 
   * @param enabled whether scaling is enabled
   */
  public void setScalingEnabled(boolean enabled) {
    m_ScalingEnabled = enabled;
  }
  
  /**
   * sets the scale factor - is ignored since we always create a screenshot!
   * @param x the scale factor for the x-axis 
   * @param y the scale factor for the y-axis 
   */
  public void setScale(double x, double y) {
    if (getScalingEnabled()) {
      m_xScale = x;
      m_yScale = y;
    }
    else {
      m_xScale = 1.0;
      m_yScale = 1.0;
    }
    
    if (DEBUG)
      System.err.println("xScale = " + m_xScale + ", yScale = " + m_yScale);
  }
  
  /**
   * returns the scale factor for the x-axis
   * 
   * @return the scale scale factor for the x-axis
   */
  public double getXScale() {
    return m_xScale;
  }
  
  /**
   * returns the scale factor for the y-axis
   * 
   * @return the scale scale factor for the y-axis
   */
  public double getYScale() {
    return m_xScale;
  }
  
  /**
   * saves the current component to the currently set file.<p>
   * <b>Note:</b> this method has to be overriden in subclasses!
   * @throws Exception      if either the file or the component is <code>null</code>
   */
  public void toOutput() throws Exception {
    if (getFile() == null)
      throw new Exception("The file is not set!");
    if (getComponent() == null)
      throw new Exception("The component is not set!");
  }
}
