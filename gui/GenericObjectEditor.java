/*
 *    GenericObjectEditor.java
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

import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.Tag;
import weka.core.SelectedTag;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.DefaultComboBoxModel;
import java.util.Properties;
import java.io.FileInputStream;
import java.util.StringTokenizer;


/** 
 * A PropertyEditor for objects that themselves have been defined as
 * editable in the GenericObjectEditor configuration file, which lists
 * possible values that can be selected from, and themselves configured.
 * The configuration file is called ".weka.gui.GenericObjectEditor" and
 * may live in either the location given by "user.home" or the current
 * directory (this last will take precedence). For speed, the properties
 * file is read only once when the class is first loaded -- this may need
 * to be changed if we ever end up running in a Java OS ;-).
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class GenericObjectEditor
  implements PropertyEditor {

  /** The classifier being configured */
  private Object m_Object;
  
  /** Handles property change notification */
  private PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** The Class of objects being edited */
  private Class m_ClassType;

  /** The GUI component for editing values, created when needed */
  private GOEPanel m_EditorComponent;

  /** True if the GUI component is needed */
  private boolean m_Enabled = true;
  
  /** The name of the properties file */
  protected static String PROPERTY_FILE = ".weka.gui.GenericObjectEditor";

  /** Contains the editor properties */
  private static Properties EDITOR_PROPERTIES;

  /** Loads the configuration property file */
  static {

    // Global properties
    Properties systemProps = System.getProperties();

    // Allow a properties file in the users home directory to override
    Properties userProps = new Properties();
    try {
      userProps.load(new FileInputStream(systemProps.getProperty("user.home")
			 + systemProps.getProperty("file.separator")
			 + PROPERTY_FILE));
    } catch (Exception ex) {
    }

    // Allow a properties file in the current directory to override
    EDITOR_PROPERTIES = new Properties(userProps);
    try {
      EDITOR_PROPERTIES.load(new FileInputStream(PROPERTY_FILE));
    } catch (Exception ex) {
    }
  }

  /**
   * Handles the GUI side of editing values.
   */
  public class GOEPanel extends JPanel
    implements ItemListener, ActionListener {
    
    /** The chooser component */
    private JComboBox m_ObjectChooser;
    
    /** The button allowing classifier customization */
    private JButton m_CustomizeBut;

    /** The component that performs classifier customization */
    private PropertySheet m_ChildPropertySheet;

    /** The model containing the list of names to select from */
    private DefaultComboBoxModel m_ObjectNames;

    /** Creates the GUI editor component */
    public GOEPanel() {

      //System.err.println("GOE()");
      m_CustomizeBut = new JButton("Customize");
      m_ObjectNames = new DefaultComboBoxModel(new String [0]);
      m_ObjectChooser = new JComboBox(m_ObjectNames);
      m_ObjectChooser.setEditable(false);

      if (m_ClassType != null) {
	updateClassType();
	updateChooser();
	updateChildPropertySheet();
      }

      m_ObjectChooser.addItemListener(this);
      m_CustomizeBut.addActionListener(this);

      setLayout(new BorderLayout());
      add(m_ObjectChooser, BorderLayout.CENTER);
      add(m_CustomizeBut, BorderLayout.WEST);

    }
    
    /** Called when the class of object being edited changes. */
    protected void updateClassType() {
      
      //System.err.println("GOE::updateClassType()");
      String className = m_ClassType.getName();
      String typeOptions = EDITOR_PROPERTIES.getProperty(className);
      if (typeOptions == null) {
	System.err.println("No configuration property found in\n"
			   + ".weka.experiment.GenericObjectEditor\n"
			   + "for " + className);
      } else {
	StringTokenizer st = new StringTokenizer(typeOptions, ", ");
	DefaultComboBoxModel newModel = new DefaultComboBoxModel();
	while (st.hasMoreTokens()) {
	  String current = st.nextToken().trim();
	  if (false) {
	    // Verify that class names are OK. Slow -- with Java2 we could
	    // try Class.forName(current, false, getClass().getClassLoader());
	    try {
	      Class c = Class.forName(current);
	      newModel.addElement(current);
	    } catch (Exception ex) {
	      System.err.println("Couldn't find class with name" + current);
	    }
	  } else {
	    newModel.addElement(current);
	  }
	}
	m_ObjectChooser.setModel(newModel);
      }
    }

    /** Called to update the list of values to be selected from */
    protected void updateChooser() {

      //System.err.println("GOE::updateChooser()");
      String objectName = m_Object.getClass().getName();
      boolean found = false;
      for (int i = 0; i < m_ObjectNames.getSize(); i++) {
	if (objectName.equals((String)m_ObjectNames.getElementAt(i))) {
	  found = true;
	  break;
	}
      }
      if (!found) {
	m_ObjectNames.addElement(objectName);
      }
      m_ObjectChooser.setSelectedItem(objectName);
    }

    /** Updates the child property sheet, and creates if needed */
    public void updateChildPropertySheet() {

      //System.err.println("GOE::updateChildPropertySheet()");
      if (m_ChildPropertySheet == null) {
	//System.err.println("Creating child property sheet");
	int x = getLocation().x;
	int y = getLocation().y + getSize().height;
	m_ChildPropertySheet = new PropertySheet(x, y);
	m_ChildPropertySheet
	  .addPropertyChangeListener(new PropertyChangeListener() {
	    public void propertyChange(PropertyChangeEvent evt) {
	      m_Support.firePropertyChange("", null, null);
	    }
	  }
				     );
	m_ChildPropertySheet.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent w) {
	    m_CustomizeBut.setEnabled(true);
	  }
	});
      }
      // Set the object as the target of the propertysheet
      //System.err.println("Setting child property sheet target");
      m_ChildPropertySheet.setTarget(m_Object);

      //System.err.println("Adjusting visible/enabled");
      if (m_ChildPropertySheet.editableProperties() == 0) {
	m_ChildPropertySheet.setVisible(false);
	m_CustomizeBut.setEnabled(false);
      } else {
	m_CustomizeBut.setEnabled(!m_ChildPropertySheet.isVisible());
      }
    }

    /**
     * Handles opening the Object customization property sheet for
     * Objects that support options.
     *
     * @param e a value of type 'ActionEvent'
     */
    public void actionPerformed(ActionEvent e) {

      //System.err.println("GOE::actionPerformed()");
      if (e.getSource() == m_CustomizeBut) {
	// Pop up the property sheet for the Object
	m_CustomizeBut.setEnabled(false);
	m_ChildPropertySheet.setVisible(true);
      }
    }
    
    /**
     * When the chooser selection is changed, ensures that the Object
     * is changed appropriately.
     *
     * @param e a value of type 'ItemEvent'
     */
    public void itemStateChanged(ItemEvent e) {

      //System.err.println("GOE::itemStateChanged()");
      if ((e.getSource() == m_ObjectChooser)
	  && (e.getStateChange() == ItemEvent.SELECTED)){
	try {
	  //System.err.println("Setting object from chooser");
	  setObject((Object)Class
		    .forName((String)m_ObjectChooser
			     .getSelectedItem())
		    .newInstance());
	  //System.err.println("done setting object from chooser");
	} catch (Exception ex) {
	}
      }
    }
  }

  /**
   * Sets whether the editor is "enabled", meaning that the current
   * values will be painted.
   *
   * @param newVal a value of type 'boolean'
   */
  public void setEnabled(boolean newVal) {

    if (newVal != m_Enabled) {
      m_Enabled = newVal;
      /*
      if (m_EditorComponent != null) {
	m_EditorComponent.setEnabled(m_Enabled);
      }
      */
    }
  }
  
  /**
   * Sets the class of values that can be edited.
   *
   * @param type a value of type 'Class'
   */
  public void setClassType(Class type) {

    //System.err.println("setClassType("
    //		       + (type == null? "<null>" : type.getName()) + ")");
    m_ClassType = type;

    if (m_EditorComponent != null) {
      m_EditorComponent.updateClassType();
    }
  }
  
  /**
   * Sets the current Object. If the Object is in the
   * Object chooser, this becomes the selected item (and added
   * to the chooser if necessary).
   *
   * @param o an object that must be a Object.
   */
  public void setValue(Object o) {

    //System.err.println("setValue()");
    if (m_ClassType == null) {
      System.err.println("No ClassType set up for GenericObjectEditor!!");
      return;
    }
    if (!m_ClassType.isAssignableFrom(o.getClass())) {
      System.err.println("setValue object not of correct type!");
      return;
    }
    
    setObject((Object)o);
      
    if (m_EditorComponent != null) {
      m_EditorComponent.updateChooser();
    }
  }

  /**
   * Sets the current Object, but doesn't worry about updating
   * the state of the Object chooser.
   *
   * @param c a value of type 'Object'
   */
  private void setObject(Object c) {

    /*
    System.err.println("Didn't even try to make a Object copy!! "
		       + "(using original)");
    */
    //    System.err.println("setObject()");
    m_Object = c;

    if (m_EditorComponent != null) {
      //    System.err.println("ObjectEditor::firePropertyChange()");
      m_EditorComponent.updateChildPropertySheet();
      m_Support.firePropertyChange("", null, null);
    }
  }

  
  /**
   * Gets the current Object.
   *
   * @return the current Object
   */
  public Object getValue() {

    //System.err.println("getValue()");
    return m_Object;
  }
  
  /**
   * Supposedly returns an initialization string to create a Object
   * identical to the current one, including it's state, but this doesn't
   * appear possible given that the initialization string isn't supposed to
   * contain multiple statements.
   *
   * @return the java source code initialisation string
   */
  public String getJavaInitializationString() {
    return "new " + m_Object.getClass().getName() + "()";
  }

  /**
   * Returns true to indicate that we can paint a representation of the
   * Object.
   *
   * @return true
   */
  public boolean isPaintable() {
    return true;
  }

  /**
   * Paints a representation of the current Object.
   *
   * @param gfx the graphics context to use
   * @param box the area we are allowed to paint into
   */
  public void paintValue(java.awt.Graphics gfx, java.awt.Rectangle box) {

    FontMetrics fm = gfx.getFontMetrics();
    int vpad = (box.height - fm.getHeight()) / 2 ;

    if (m_Enabled) {
      String rep = m_Object.getClass().getName();
      int dotPos = rep.lastIndexOf('.');
      if (dotPos != -1) {
	rep = rep.substring(dotPos + 1);
      }
      if (m_Object instanceof OptionHandler) {
	rep += " " + Utils.joinOptions(((OptionHandler)m_Object)
				       .getOptions());
      }
      //gfx.clearRect(0, 0, box.width, box.height);
      //gfx.drawRect(0, 0, box.width, box.height);
      gfx.drawString(rep, 2, fm.getHeight() + vpad);
    } else {
    }
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
  public void setAsText(String text) throws IllegalArgumentException {
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

    //System.err.println("getCustomEditor()");
    if (m_EditorComponent == null) {
      //System.err.println("creating new editing component");
      m_EditorComponent = new GOEPanel();
    }
    return m_EditorComponent;
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
   * Tests out the Object editor from the command line.
   *
   * @param args may contain the class name of a Object to edit
   */
  public static void main(String [] args) {

    try {
      System.err.println("---Registering Weka Editors---");
      java.beans.PropertyEditorManager
	.registerEditor(weka.experiment.ResultProducer.class,
			GenericObjectEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(weka.experiment.SplitEvaluator.class,
			GenericObjectEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(weka.classifiers.Classifier.class,
			GenericObjectEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(SelectedTag.class,
			SelectedTagEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(java.io.File.class,
			FileEditor.class);
      GenericObjectEditor ce = new GenericObjectEditor();
      ce.setClassType(weka.experiment.ResultListener.class);
      Object initial = new weka.experiment.CSVResultListener();
      if (args.length > 0) {
	initial = (Object)Class.forName(args[0]).newInstance();
      }
      ce.setValue(initial);

      PropertyDialog pd = new PropertyDialog(ce, 100, 100);
      pd.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  PropertyEditor pe = ((PropertyDialog)e.getSource()).getEditor();
	  Object c = (Object)pe.getValue();
	  String options = "";
	  if (c instanceof OptionHandler) {
	    options = Utils.joinOptions(((OptionHandler)c).getOptions());
	  }
	  System.out.println(c.getClass().getName() + " " + options);
	  System.exit(0);
	}
      });
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
