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
 * OpenBrowser.java
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.extensions;

import weka.gui.BrowserHelper;
import weka.gui.MainMenuExtension;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

/**
 * An extension to be displayed in the <code>weka.gui.Main</code> class
 * in the <i>Extension</i> menu: starts a browser with the Weka homepage.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class StartBrowser
  implements MainMenuExtension {

  /**
   * Returns the name of the submenu.
   * 
   * @return		the title of the submenu
   */
  public String getSubmenuTitle() {
    return "Internet";
  }

  /**
   * Returns the name of the menu item.
   * 
   * @return		the name of the menu item.
   */
  public String getMenuTitle() {
    return "Start browser";
  }

  /**
   * returns an ActionListener to start up a browser
   * 
   * @param owner 	the owner of potential dialogs
   */
  public ActionListener getActionListener(JFrame owner) {
    final JFrame finalOwner = owner;
    
    ActionListener result = new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
	BrowserHelper.openURL(finalOwner, "http://www.cs.waikato.ac.nz/~ml/weka/");
      }
    };
    
    return result;
  }

  /**
   * ignored
   * 
   * @param frame	the frame object to embed components, etc.
   */
  public void fillFrame(Component frame) {
  }
}
