/*
 *    PropertySheet.java
 *    Copyright (C) 1999 Len Trigg
 *
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

package weka.gui;

import java.util.Hashtable;
import java.util.Vector;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.Customizer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.IntrospectionException;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyEditorManager;
import java.beans.PropertyVetoException;
import java.beans.Beans;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;

/** 
 * Displays a property sheet where (supported) properties of the target
 * object may be edited.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class PropertySheet extends JFrame implements PropertyChangeListener {

  /** The panel displaying the properties */
  private PropertySheetPanel panel;

  /**
   * Creates the propertysheet frame at a given location.
   *
   * @param x the initial x coord
   * @param y the initial y coord
   */
  public PropertySheet(int x, int y) {
    
    super("Properties - <initializing...>");
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	e.getWindow().setVisible(false);
      }
    });
    setLocation(x, y);
    panel = new PropertySheetPanel(this);
  }

  /**
   * Sets a new target object for the property sheet.
   *
   * @param targ a value of type 'Object'
   */
  public void setTarget(Object targ) {

    panel.setTarget(targ);
    setTitle("Properties - " + targ.getClass().getName());
    pack();
  }

  /**
   * Gets a count of the number of properties in the propertysheet.
   *
   * @return the number of editable properties.
   */
  public int editableProperties() {

    return panel.editableProperties();
  }

  /** A support object for handling property change listeners */ 
  private PropertyChangeSupport support = new PropertyChangeSupport(this);

  /**
   * Updates the property sheet panel with a changed property and also passed
   * the event along.
   *
   * @param evt a value of type 'PropertyChangeEvent'
   */
  public void propertyChange(PropertyChangeEvent evt) {
    panel.wasModified(evt); // Let our panel update before guys downstream
    support.firePropertyChange("", null, null);
  }

  /**
   * Adds a PropertyChangeListener.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    support.addPropertyChangeListener(l);
  }

  /**
   * Removes a PropertyChangeListener.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    support.removePropertyChangeListener(l);
  }
}




/**
 * Displays the editable properties in a panel.
 */
class PropertySheetPanel extends JPanel {

  /** The parent frame we are embedded in */
  private PropertySheet m_Frame;

  /** The target object being edited */
  private Object m_Target;

  /** Holds properties of the target */
  private PropertyDescriptor m_Properties[];

  /** Holds property editors of the object */
  private PropertyEditor m_Editors[];

  /** Holds current object values for each property */
  private Object m_Values[];

  /** Stores GUI components containing each editing component */
  private Component m_Views[];

  /** The labels for each property */
  private JLabel m_Labels[];

  /** A count of the number of properties we have an editor for */
  private int m_NumEditable = 0;

  /**
   * Creates the property sheet with the given parent frame.
   *
   * @param frame a value of type 'PropertySheet'
   */
  PropertySheetPanel(PropertySheet frame) {
    this.m_Frame = frame;
    //    setBorder(BorderFactory.createLineBorder(Color.red));
    setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
  }

  /**
   * Sets a new target object for customisation.
   *
   * @param targ a value of type 'Object'
   */
  synchronized void setTarget(Object targ) {
	
    m_Frame.getContentPane().removeAll();	
    removeAll();
    GridBagLayout gbLayout = new GridBagLayout();
    setLayout(gbLayout);
    setVisible(false);
    m_NumEditable = 0;
    m_Target = targ;
    try {
      BeanInfo bi = Introspector.getBeanInfo(m_Target.getClass());
      m_Properties = bi.getPropertyDescriptors();
    } catch (IntrospectionException ex) {
      System.err.println("PropertySheet: Couldn't introspect");
      return;
    }

    m_Editors = new PropertyEditor[m_Properties.length];
    m_Values = new Object[m_Properties.length];
    m_Views = new Component[m_Properties.length];
    m_Labels = new JLabel[m_Properties.length];
    for (int i = 0; i < m_Properties.length; i++) {

      // Don't display hidden or expert properties.
      if (m_Properties[i].isHidden() || m_Properties[i].isExpert()) {
	continue;
      }

      String name = m_Properties[i].getDisplayName();
      Class type = m_Properties[i].getPropertyType();
      Method getter = m_Properties[i].getReadMethod();
      Method setter = m_Properties[i].getWriteMethod();

      // Only display read/write properties.
      if (getter == null || setter == null) {
	continue;
      }
	
      Component view = null;

      try {
	Object args[] = { };
	Object value = getter.invoke(m_Target, args);
	m_Values[i] = value;

	PropertyEditor editor = null;
	Class pec = m_Properties[i].getPropertyEditorClass();
	if (pec != null) {
	  try {
	    editor = (PropertyEditor)pec.newInstance();
	  } catch (Exception ex) {
	    // Drop through.
	  }
	}
	if (editor == null) {
	  editor = PropertyEditorManager.findEditor(type);
	}
	m_Editors[i] = editor;

	// If we can't edit this component, skip it.
	if (editor == null) {
	  // If it's a user-defined property we give a warning.
	  String getterClass = m_Properties[i].getReadMethod()
	    .getDeclaringClass().getName();
	  if (getterClass.indexOf("java.") != 0) {
	    System.err.println("Warning: Can't find public property editor"
			       + " for property \"" + name + "\" (class \""
			       + type.getName() + "\").  Skipping.");
	  }
	  continue;
	}
	if (editor instanceof GenericObjectEditor) {
	  ((GenericObjectEditor) editor).setClassType(type);
	}

	// Don't try to set null values:
	if (value == null) {
	  // If it's a user-defined property we give a warning.
	  String getterClass = m_Properties[i].getReadMethod()
	    .getDeclaringClass().getName();
	  if (getterClass.indexOf("java.") != 0) {
	    System.err.println("Warning: Property \"" + name 
			       + "\" has null initial value.  Skipping.");
	  }
	  continue;
	}

	editor.setValue(value);

	// Now figure out how to display it...
	if (editor.isPaintable() && editor.supportsCustomEditor()) {
	  view = new PropertyPanel(editor);
	} else if (editor.getTags() != null) {
	  view = new PropertyValueSelector(editor);
	} else if (editor.getAsText() != null) {
	  //String init = editor.getAsText();
	  view = new PropertyText(editor);
	} else {
	  System.err.println("Warning: Property \"" + name 
			     + "\" has non-displayabale editor.  Skipping.");
	  continue;
	}

	editor.addPropertyChangeListener(m_Frame);

      } catch (InvocationTargetException ex) {
	System.err.println("Skipping property " + name
			   + " ; exception on target: "
			   + ex.getTargetException());
	ex.getTargetException().printStackTrace();
	continue;
      } catch (Exception ex) {
	System.err.println("Skipping property " + name
			   + " ; exception: " + ex);
	ex.printStackTrace();
	continue;
      }

      m_Labels[i] = new JLabel(name, SwingConstants.RIGHT);
      m_Labels[i].setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 5));
      m_Views[i] = view;
      GridBagConstraints gbConstraints = new GridBagConstraints();
      gbConstraints.anchor = GridBagConstraints.EAST;
      gbConstraints.fill = GridBagConstraints.HORIZONTAL;
      gbConstraints.gridy = i;     gbConstraints.gridx = 0;
      gbLayout.setConstraints(m_Labels[i], gbConstraints);
      add(m_Labels[i]);
      JPanel newPanel = new JPanel();
      newPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 10));
      newPanel.setLayout(new BorderLayout());
      newPanel.add(m_Views[i], BorderLayout.CENTER);
      gbConstraints = new GridBagConstraints();
      gbConstraints.anchor = GridBagConstraints.WEST;
      gbConstraints.fill = GridBagConstraints.BOTH;
      gbConstraints.gridy = i;     gbConstraints.gridx = 1;
      gbConstraints.weightx = 100;
      gbLayout.setConstraints(newPanel, gbConstraints);
      add(newPanel);
      m_NumEditable ++;
    }

    m_Frame.getContentPane().add(this);
    //    doLayout();
    m_Frame.pack();
    setVisible(true);	
  }

  /**
   * Gets the number of editable properties for the current target.
   *
   * @return the number of editable properties.
   */
  public int editableProperties() {

    return m_NumEditable;
  }
  
  /**
   * Updates the propertysheet when a value has been changed (from outside
   * the propertysheet?).
   *
   * @param evt a value of type 'PropertyChangeEvent'
   */
  synchronized void wasModified(PropertyChangeEvent evt) {

    //    System.err.println("wasModified");
    if (evt.getSource() instanceof PropertyEditor) {
      PropertyEditor editor = (PropertyEditor) evt.getSource();
      for (int i = 0 ; i < m_Editors.length; i++) {
	if (m_Editors[i] == editor) {
	  PropertyDescriptor property = m_Properties[i];
	  Object value = editor.getValue();
	  m_Values[i] = value;
	  Method setter = property.getWriteMethod();
	  try {
	    Object args[] = { value };
	    args[0] = value;
	    setter.invoke(m_Target, args);
	  } catch (InvocationTargetException ex) {
	    if (ex.getTargetException()
		instanceof PropertyVetoException) {
	      System.err.println("WARNING: Vetoed; reason is: " 
				 + ex.getTargetException().getMessage());
	    } else {
	      System.err.println("InvocationTargetException while updating " 
				 + property.getName());
	    }
	  } catch (Exception ex) {
	    System.err.println("Unexpected exception while updating " 
		  + property.getName());
	  }
	  if (m_Views[i] != null && m_Views[i] instanceof PropertyPanel) {
	    //System.err.println("Trying to repaint the property canvas");
	    m_Views[i].repaint();
	    revalidate();
	  }
	  break;
	}
      }
    }

    // Now re-read all the properties and update the editors
    // for any other properties that have changed.
    for (int i = 0; i < m_Properties.length; i++) {
      Object o;
      try {
	Method getter = m_Properties[i].getReadMethod();
	Object args[] = { };
	o = getter.invoke(m_Target, args);
      } catch (Exception ex) {
	o = null;
      }
      if (o == m_Values[i] || (o != null && o.equals(m_Values[i]))) {
	// The property is equal to its old value.
	continue;
      }
      m_Values[i] = o;
      // Make sure we have an editor for this property...
      if (m_Editors[i] == null) {
	continue;
      }
      // The property has changed!  Update the editor.
      m_Editors[i].removePropertyChangeListener(m_Frame);
      m_Editors[i].setValue(o);
      m_Editors[i].addPropertyChangeListener(m_Frame);
      if (m_Views[i] != null) {
	//System.err.println("Trying to repaint " + (i + 1));
	m_Views[i].repaint();
      }
    }

    // Make sure the target bean gets repainted.
    if (Beans.isInstanceOf(m_Target, Component.class)) {
      ((Component)(Beans.getInstanceOf(m_Target, Component.class))).repaint();
    }
  }
}


