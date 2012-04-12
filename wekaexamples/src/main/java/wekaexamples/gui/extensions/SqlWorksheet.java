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
 * SqlWorksheet.java
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.extensions;

import weka.gui.MainMenuExtension;
import weka.gui.sql.SqlViewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;

/**
 * An extension to be displayed in the <code>weka.gui.Main</code> class
 * in the <i>Extension</i> menu: it opens a new frame with the SqlPanel in it.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SqlWorksheet
  implements MainMenuExtension {

  /**
   * Returns the name of the submenu. In this case null, since we want this
   * item to be displayed directly in the menu, not in a submenu.
   * 
   * @return		the title of the submenu
   */
  public String getSubmenuTitle() {
    return null;
  }

  /**
   * Returns the name of the menu item.
   * 
   * @return		the name of the menu item.
   */
  public String getMenuTitle() {
    return "SQL Worksheet";
  }

  /**
   * returns null.
   * 
   * @param owner 	the owner of potential dialogs
   */
  public ActionListener getActionListener(JFrame owner) {
    return null;
  }

  /**
   * fills the frame with the instance of an SqlViewer panel.
   * 
   * @param frame	the frame to fill
   */
  public void fillFrame(Component frame) {
    SqlViewer sql = new SqlViewer(null);

    // add sql viewer component
    if (frame instanceof JFrame) {
      JFrame f = (JFrame) frame;
      f.setLayout(new BorderLayout());
      f.add(sql, BorderLayout.CENTER);
      f.pack();
    }
    else if (frame instanceof JInternalFrame) {
      JInternalFrame f = (JInternalFrame) frame;
      f.setLayout(new BorderLayout());
      f.add(sql, BorderLayout.CENTER);
      f.pack();
    }

    // size + location (= centered)
    frame.setSize(800, 600);
    frame.validate();
    int screenHeight = frame.getGraphicsConfiguration().getBounds().height;
    int screenWidth  = frame.getGraphicsConfiguration().getBounds().width;
    frame.setLocation(
	  (screenWidth - frame.getBounds().width) / 2,
	  (screenHeight - frame.getBounds().height) / 2);
  }
}
