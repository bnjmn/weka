package weka.gui;

import javax.swing.JComboBox;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.beans.PropertyEditor;

// Support for PropertyEditors that use tags.
class PropertyValueSelector extends JComboBox {

  PropertyEditor m_Editor;
  
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


