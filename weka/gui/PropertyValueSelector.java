/*
 *    PropertyValueSelector.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

import javax.swing.JComboBox;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.beans.PropertyEditor;

/** 
 * Support for any PropertyEditor that uses tags.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
class PropertyValueSelector extends JComboBox {

  /** The property editor */
  PropertyEditor m_Editor;
  
  /**
   * Sets up the editing component with the supplied editor.
   *
   * @param pe the PropertyEditor
   */
  PropertyValueSelector(PropertyEditor pe) {
      
    m_Editor = pe;
    Object value = m_Editor.getAsText();
    String tags[] = m_Editor.getTags();
    ComboBoxModel model = new DefaultComboBoxModel(tags) {
      public Object getSelectedItem() {
	return m_Editor.getAsText();
      }
      public void setSelectedItem(Object o) {
	m_Editor.setAsText((String)o);
      }
    };
    setModel(model);
    setSelectedItem(value);
  }
}


