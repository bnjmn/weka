package weka.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusAdapter;
import javax.swing.JTextField;
import java.beans.PropertyEditor;
import java.awt.Graphics;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

// Support for a PropertyEditor that uses text.
// Isn't going to work well if the property gets changed
// somewhere other than this field simultaneously
class PropertyText extends JTextField {

  private PropertyEditor m_Editor;

  PropertyText(PropertyEditor pe) {

    super(pe.getAsText());
    m_Editor = pe;
    
    m_Editor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
	updateUs();
      }
    });
    addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
	if (e.getKeyCode() == KeyEvent.VK_ENTER) {
	  updateEditor();
	}
      }
    });
    addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
	updateEditor();
      }
    });
  }

  protected void updateUs() {
    try {
      setText(m_Editor.getAsText());
    } catch (IllegalArgumentException ex) {
      // Quietly ignore.
    }
  }
  protected void updateEditor() {
    try {
      m_Editor.setAsText(getText());
    } catch (IllegalArgumentException ex) {
      // Quietly ignore.
    }
  }
}
