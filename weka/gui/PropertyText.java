/*
 *    PropertyText.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusAdapter;
import java.beans.PropertyEditor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.JTextField;

/** 
 * Support for a PropertyEditor that uses text.
 * Isn't going to work well if the property gets changed
 * somewhere other than this field simultaneously
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
class PropertyText extends JTextField {

  /** The property editor */
  private PropertyEditor m_Editor;

  /**
   * Sets up the editing component with the supplied editor.
   *
   * @param pe the PropertyEditor
   */
  PropertyText(PropertyEditor pe) {

    super(pe.getAsText());
    m_Editor = pe;
    
    /*    m_Editor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
	updateUs();
      }
      }); */
    addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
	//	if (e.getKeyCode() == KeyEvent.VK_ENTER) {
	updateEditor();
	//	}
      }
    });
    addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
	updateEditor();
      }
    });
  }

  /**
   * Attempts to update the textfield value from the editor.
   */
  protected void updateUs() {
    try {
      setText(m_Editor.getAsText());
    } catch (IllegalArgumentException ex) {
      // Quietly ignore.
    }
  }

  /**
   * Attempts to update the editor value from the textfield.
   */
  protected void updateEditor() {
    try {
      m_Editor.setAsText(getText());
    } catch (IllegalArgumentException ex) {
      // Quietly ignore.
    }
  }
}
