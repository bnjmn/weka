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
 *    GenericArrayEditor.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

import weka.core.SelectedTag;
import weka.core.SerializedObject;
import weka.classifiers.Classifier;

import java.lang.reflect.Array;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.ListCellRenderer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JOptionPane;


/** 
 * A PropertyEditor for arrays of objects that themselves have
 * property editors.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.13.2.1 $
 */
public class GenericArrayEditor extends JPanel
  implements PropertyEditor {

  /** Handles property change notification */
  private PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** The label for when we can't edit that type */
  private JLabel m_Label = new JLabel("Can't edit", SwingConstants.CENTER);
  
  /** The list component displaying current values */
  private JList m_ElementList = new JList();

  /** The class of objects allowed in the array */
  private Class m_ElementClass = String.class;

  /** The defaultlistmodel holding our data */
  private DefaultListModel m_ListModel;

  /** The property editor for the class we are editing */
  private PropertyEditor m_ElementEditor;

  /** Click this to delete the selected array values */
  private JButton m_DeleteBut = new JButton("Delete");

  /** Click to add the current object configuration to the array */
  private JButton m_AddBut = new JButton("Add");

  /** Listens to buttons being pressed and taking the appropriate action */
  private ActionListener m_InnerActionListener =
    new ActionListener() {

    public void actionPerformed(ActionEvent e) {

      if (e.getSource() == m_DeleteBut) {
	int [] selected = m_ElementList.getSelectedIndices();
	if (selected != null) {
	  for (int i = 0; i < selected.length; i++) {
	    int current = selected[i];
	    m_ListModel.removeElementAt(current);
	    if (m_ListModel.size() > current) {
	      m_ElementList.setSelectedIndex(current);
	    }
	  }
	  m_Support.firePropertyChange("", null, null);
	}
	if (m_ElementList.getSelectedIndex() == -1) {
	  m_DeleteBut.setEnabled(false);
	}
      } else if (e.getSource() == m_AddBut) {
	int selected = m_ElementList.getSelectedIndex();
	Object addObj = m_ElementEditor.getValue();
	
	// Make a full copy of the object using serialization
	try {
          SerializedObject so = new SerializedObject(addObj);
	  addObj = so.getObject();
	  if (selected != -1) {
	    m_ListModel.insertElementAt(addObj, selected);
	  } else {
	    m_ListModel.addElement(addObj);
	  }
	  m_Support.firePropertyChange("", null, null);
	} catch (Exception ex) {
	  JOptionPane.showMessageDialog(GenericArrayEditor.this,
					"Could not create an object copy",
					null,
					JOptionPane.ERROR_MESSAGE);
	}
      } 
    }
  };

  /** Listens to list items being selected and takes appropriate action */
  private ListSelectionListener m_InnerSelectionListener =
    new ListSelectionListener() {

      public void valueChanged(ListSelectionEvent e) {

	if (e.getSource() == m_ElementList) {
	  // Enable the delete button
	  if (m_ElementList.getSelectedIndex() != -1) {
	    m_DeleteBut.setEnabled(true);
	  }
	}
      }
  };
    

  /**
   * Sets up the array editor.
   */
  public GenericArrayEditor() {

    setLayout(new BorderLayout());
    add(m_Label, BorderLayout.CENTER);
    m_DeleteBut.addActionListener(m_InnerActionListener);
    m_AddBut.addActionListener(m_InnerActionListener);
    m_ElementList.addListSelectionListener(m_InnerSelectionListener);
    m_AddBut.setToolTipText("Add the current item to the list");
    m_DeleteBut.setToolTipText("Delete the selected list item");
  }

  /** This class handles the creation of list cell renderers from the 
   * property editors.
   */
  private class EditorListCellRenderer implements ListCellRenderer {

    /** The class of the property editor for array objects */
    private Class m_EditorClass;

    /** The class of the array values */
    private Class m_ValueClass;

    /**
     * Creates the list cell renderer.
     *
     * @param editorClass The class of the property editor for array objects
     * @param valueClass The class of the array values
     */
    public EditorListCellRenderer(Class editorClass, Class valueClass) {
      m_EditorClass = editorClass;
      m_ValueClass = valueClass;
    }

    /**
     * Creates a cell rendering component.
     *
     * @param JList the list that will be rendered in
     * @param Object the cell value
     * @param int which element of the list to render
     * @param boolean true if the cell is selected
     * @param boolean true if the cell has the focus
     * @return the rendering component
     */
    public Component getListCellRendererComponent(final JList list,
						  final Object value,
						  final int index,
						  final boolean isSelected,
						  final boolean cellHasFocus) {
      try {
	final PropertyEditor e = (PropertyEditor)m_EditorClass.newInstance();
	if (e instanceof GenericObjectEditor) {
	  //	  ((GenericObjectEditor) e).setDisplayOnly(true);
	  ((GenericObjectEditor) e).setClassType(m_ValueClass);
	}
	e.setValue(value);
	return new JPanel() {

	  public void paintComponent(Graphics g) {

	    Insets i = this.getInsets();
	    Rectangle box = new Rectangle(i.left, i.top,
					  this.getWidth() - i.right,
					  this.getHeight() - i.bottom );
	    g.setColor(isSelected
		       ? list.getSelectionBackground()
		       : list.getBackground());
	    g.fillRect(0, 0, this.getWidth(), this.getHeight());
	    g.setColor(isSelected
		       ? list.getSelectionForeground()
		       : list.getForeground());
	    e.paintValue(g, box);
	  }
	  
	  public Dimension getPreferredSize() {

	    Font f = this.getFont();
	    FontMetrics fm = this.getFontMetrics(f);
	    return new Dimension(0, fm.getHeight());
	  }
	};
      } catch (Exception ex) {
	return null;
      }
    }
  }

  /**
   * Updates the type of object being edited, so attempts to find an
   * appropriate propertyeditor.
   *
   * @param o a value of type 'Object'
   */
  private void updateEditorType(Object o) {

    // Determine if the current object is an array
    m_ElementEditor = null; m_ListModel = null;
    removeAll();
    if ((o != null) && (o.getClass().isArray())) {
      Class elementClass = o.getClass().getComponentType();    
      PropertyEditor editor = PropertyEditorManager.findEditor(elementClass);
      Component view = null;
      ListCellRenderer lcr = new DefaultListCellRenderer();
      if (editor != null) {
	if (editor instanceof GenericObjectEditor) {
	  ((GenericObjectEditor) editor).setClassType(elementClass);
	}

        //setting the value in the editor so that
        //we don't get a NullPointerException
        //when we do getAsText() in the constructor of
        //PropertyValueSelector()
	if(Array.getLength(o) > 0) {
	  editor.setValue(Array.get(o,0));
	} else {
	  if (editor instanceof GenericObjectEditor) {
	    ((GenericObjectEditor)editor).setDefaultValue();
	  } else {   
            try {
	    editor.setValue(elementClass.newInstance());
            } catch(Exception ex) {
              m_ElementEditor=null;
              System.err.println(ex.getMessage());
              add(m_Label, BorderLayout.CENTER);
              m_Support.firePropertyChange("", null, null);
              validate();
              return;
            }
	  }
	}
        
	if (editor.isPaintable() && editor.supportsCustomEditor()) {
	  view = new PropertyPanel(editor);
	  lcr = new EditorListCellRenderer(editor.getClass(), elementClass);
	} else if (editor.getTags() != null) {
	  view = new PropertyValueSelector(editor);
	} else if (editor.getAsText() != null) {
	  view = new PropertyText(editor);
	}
      }
      if (view == null) {
	System.err.println("No property editor for class: "
			   + elementClass.getName());
      } else {
	m_ElementEditor = editor;

	// Create the ListModel and populate it
	m_ListModel = new DefaultListModel();
	m_ElementClass = elementClass;
	for (int i = 0; i < Array.getLength(o); i++) {
	  m_ListModel.addElement(Array.get(o,i));
	}
	m_ElementList.setCellRenderer(lcr);
	m_ElementList.setModel(m_ListModel);
	if (m_ListModel.getSize() > 0) {
	  m_ElementList.setSelectedIndex(0);
	  m_DeleteBut.setEnabled(true);
	} else {
	  m_DeleteBut.setEnabled(false);
	}

        //have already set the value above in the editor
	//try {
	  //if (m_ListModel.getSize() > 0) {
	  //  m_ElementEditor.setValue(m_ListModel.getElementAt(0));
	  //} else {
	  //  if (m_ElementEditor instanceof GenericObjectEditor) {
	  //    ((GenericObjectEditor)m_ElementEditor).setDefaultValue();
	  //  } else {
	  //    m_ElementEditor.setValue(m_ElementClass.newInstance());
	  //  }
	  //}
	  
	  JPanel panel = new JPanel();
	  panel.setLayout(new BorderLayout());
	  panel.add(view, BorderLayout.CENTER);
	  panel.add(m_AddBut, BorderLayout.EAST);
	  add(panel, BorderLayout.NORTH);
	  add(new JScrollPane(m_ElementList), BorderLayout.CENTER);
	  add(m_DeleteBut, BorderLayout.SOUTH);
	  m_ElementEditor
	    .addPropertyChangeListener(new PropertyChangeListener() {
	    public void propertyChange(PropertyChangeEvent e) {
	      repaint();
	    }
	  });
	//} catch (Exception ex) {
	//  System.err.println(ex.getMessage());
	//  m_ElementEditor = null;
	//}
      }
    }
    if (m_ElementEditor == null) {
      add(m_Label, BorderLayout.CENTER);
    }
    m_Support.firePropertyChange("", null, null);
    validate();
  }

  /**
   * Sets the current object array.
   *
   * @param o an object that must be an array.
   */
  public void setValue(Object o) {

    // Create a new list model, put it in the list and resize?
    updateEditorType(o);
  }

  /**
   * Gets the current object array.
   *
   * @return the current object array
   */
  public Object getValue() {

    if (m_ListModel == null) {
      return null;
    }
    // Convert the listmodel to an array of strings and return it.
    int length = m_ListModel.getSize();
    Object result = Array.newInstance(m_ElementClass, length);
    for (int i = 0; i < length; i++) {
      Array.set(result, i, m_ListModel.elementAt(i));
    }
    return result;
  }
  
  /**
   * Supposedly returns an initialization string to create a classifier
   * identical to the current one, including it's state, but this doesn't
   * appear possible given that the initialization string isn't supposed to
   * contain multiple statements.
   *
   * @return the java source code initialisation string
   */
  public String getJavaInitializationString() {

    return "null";
  }

  /**
   * Returns true to indicate that we can paint a representation of the
   * string array
   *
   * @return true
   */
  public boolean isPaintable() {
    return true;
  }

  /**
   * Paints a representation of the current classifier.
   *
   * @param gfx the graphics context to use
   * @param box the area we are allowed to paint into
   */
  public void paintValue(java.awt.Graphics gfx, java.awt.Rectangle box) {

    FontMetrics fm = gfx.getFontMetrics();
    int vpad = (box.height - fm.getAscent()) / 2;
    String rep = m_ListModel.getSize() + " " + m_ElementClass.getName();
    gfx.drawString(rep, 2, fm.getHeight() + vpad);
  }

  /**
   * Returns null as we don't support getting/setting values as text.
   *
   * @return null
   */
  public String getAsText() {
    return null;
  }

  /**
   * Returns null as we don't support getting/setting values as text. 
   *
   * @param text the text value
   * @exception IllegalArgumentException as we don't support
   * getting/setting values as text.
   */
  public void setAsText(String text) {
    throw new IllegalArgumentException(text);
  }

  /**
   * Returns null as we don't support getting values as tags.
   *
   * @return null
   */
  public String[] getTags() {
    return null;
  }

  /**
   * Returns true because we do support a custom editor.
   *
   * @return true
   */
  public boolean supportsCustomEditor() {
    return true;
  }
  
  /**
   * Returns the array editing component.
   *
   * @return a value of type 'java.awt.Component'
   */
  public java.awt.Component getCustomEditor() {
    return this;
  }

  /**
   * Adds a PropertyChangeListener who will be notified of value changes.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    m_Support.addPropertyChangeListener(l);
  }

  /**
   * Removes a PropertyChangeListener.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    m_Support.removePropertyChangeListener(l);
  }

  /**
   * Tests out the array editor from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      GenericObjectEditor.registerEditors();
      
      final GenericArrayEditor ce = new GenericArrayEditor();

      final weka.filters.Filter [] initial = new weka.filters.Filter [0];
	/*
      {
	new weka.filters.AddFilter()
	};*/
      /*
      final String [] initial = {
	"Hello",
	"There",
	"Bob"
	};*/
      PropertyDialog pd = new PropertyDialog(ce, 100, 100);
      pd.setSize(200,200);
      pd.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  System.exit(0);
	}
      });
      ce.setValue(initial);
      //ce.validate();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }

}

