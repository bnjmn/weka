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
 *    PropertyDialog.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditor;
import javax.swing.JFrame;
import javax.swing.JButton;

/**
 * Support for PropertyEditors with custom editors: puts the editor into
 * a separate frame.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class PropertyDialog extends JFrame {

  /** The property editor */
  private PropertyEditor m_Editor;

  /** The custom editor component */
  private Component m_EditorComponent;

  /**
   * Creates the editor frame.
   *
   * @param pe the PropertyEditor
   * @param x initial x coord for the frame
   * @param y initial y coord for the frame
   */
  public PropertyDialog(PropertyEditor pe, int x, int y) {

    super(pe.getClass().getName());
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	e.getWindow().dispose();
      }
    });
    getContentPane().setLayout(new BorderLayout());

    m_Editor = pe;
    m_EditorComponent = pe.getCustomEditor();
    getContentPane().add(m_EditorComponent, BorderLayout.CENTER);

    pack();

    int screenWidth = getGraphicsConfiguration().getBounds().width;
    int screenHeight = getGraphicsConfiguration().getBounds().height;

    // adjust height to a maximum of 95% of screen height
    if (getHeight() > (double) screenHeight * 0.95)
      setSize(getWidth(), (int) ((double) screenHeight * 0.95));

    if ((x == -1) && (y == -1)) {
      setLocationRelativeTo(null);
    }
    else {
      // adjust position if necessary
      if (x + getWidth() > screenWidth)
	x = screenWidth - getWidth();
      if (y + getHeight() > screenHeight)
	y = screenHeight - getHeight();
      setLocation(x, y);
    }

    setVisible(true);
  }

  /**
   * Gets the current property editor.
   *
   * @return a value of type 'PropertyEditor'
   */
  public PropertyEditor getEditor() {

    return m_Editor;
  }
}

