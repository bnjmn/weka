/*
 *    NamedColor.java
 *    Copyright (C) 1999 Malcolm Ware
 *
 */


package weka.gui.treevisualizer;

import java.awt.*;

/**
 * This class contains a color name and the rgb values of that color
 *
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class NamedColor {

  /** The name of the color */
  public String m_name;

  /** The actual color object */
  public Color m_col;
  
  /**
   * @param n The name of the color.
   * @param r The red component of the color.
   * @param g The green component of the color.
   * @param b The blue component of the color.
   */   
  public NamedColor(String n,int r,int g,int b) {
    m_name = n;
    m_col = new Color(r,g,b);
  }
}







