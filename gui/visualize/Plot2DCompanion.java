/*
 *    Plot2DCompanion.java
 *    Copyright (C) 2000 Mark Hall
 *
 */


package weka.gui.visualize;

import java.awt.Graphics;

/**
 * Interface for classes that need to draw to the Plot2D panel *before*
 * Plot2D renders anything (eg. VisualizePanel may need to draw polygons
 * etc.)
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface Plot2DCompanion {

  /**
   * Something to be drawn before the plot itself
   * @param gx the graphics context to render to
   */
  public abstract void prePlot(Graphics gx);
}
