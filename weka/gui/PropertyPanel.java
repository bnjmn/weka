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
 *    PropertyPanel.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.beans.PropertyEditor;
import javax.swing.JPanel;
import javax.swing.BorderFactory;

/** 
 * Support for drawing a property value in a component.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.7 $
 */
public class PropertyPanel extends JPanel {

  /** The property editor */
  private PropertyEditor m_Editor;

  /** The currently displayed property dialog, if any */
  private PropertyDialog m_PD;
  
  /**
   * Create the panel with the supplied property editor.
   *
   * @param pe the PropertyEditor
   */
  public PropertyPanel(PropertyEditor pe) {

    //    System.err.println("PropertyPanel::PropertyPanel()");
    setBorder(BorderFactory.createEtchedBorder());
    setToolTipText("Click to edit properties for this object");
    setOpaque(true);
    m_Editor = pe;
    addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent evt) {
	if (m_Editor.getValue() != null) {
	  if (m_PD == null) {
	    int x = getLocationOnScreen().x;
	    int y = getLocationOnScreen().y;
	    m_PD = new PropertyDialog(m_Editor, x, y);
	  } else {
	    m_PD.setVisible(true);
	  }
	}
      }
    });
    Dimension newPref = getPreferredSize();
    newPref.height = getFontMetrics(getFont()).getHeight() * 5 / 4;
    newPref.width = newPref.height * 5;
    setPreferredSize(newPref);
  }

  public void removeNotify() {
    if (m_PD != null) {
      m_PD.dispose();
      m_PD = null;
    }
  }

  /**
   * Paints the component, using the property editor's paint method.
   *
   * @param g the current graphics context
   */
  public void paintComponent(Graphics g) {

    Insets i = getInsets();
    Rectangle box = new Rectangle(i.left, i.top,
				  getSize().width - i.left - i.right - 1,
				  getSize().height - i.top - i.bottom - 1);
    
    g.clearRect(i.left, i.top,
		getSize().width - i.right - i.left,
		getSize().height - i.bottom - i.top);
    m_Editor.paintValue(g, box);
  }

}
