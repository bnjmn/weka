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
import java.util.Vector;
import java.util.Properties;
import java.util.StringTokenizer;
import java.io.FileInputStream;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.Window;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;


/** 
 * A PropertyEditor for objects that themselves have been defined as
 * editable in the GenericObjectEditor configuration file, which lists
 * possible values that can be selected from, and themselves configured.
 * The configuration file is called "GenericObjectEditor.props" and
 * may live in either the location given by "user.home" or the current
 * directory (this last will take precedence), and a default properties
 * file is read from the weka distribution. For speed, the properties
 * file is read only once when the class is first loaded -- this may need
 * to be changed if we ever end up running in a Java OS ;-).
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.14 $
 */
public class GenericObjectEditor implements PropertyEditor {

  /** The classifier being configured */
  private Object m_Object;

  /** Holds a copy of the current classifier that can be reverted to
      if the user decides to cancel */
  private Object m_Backup;
  
  /** Handles property change notification */
  private PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** The Class of objects being edited */
  private Class m_ClassType;

  /** The GUI component for editing values, created when needed */
  private GOEPanel m_EditorComponent;

  /** True if the GUI component is needed */
  private boolean m_Enabled = true;
  
  /** The name of the properties file */
  protected static String PROPERTY_FILE = "GenericObjectEditor.props";

  /** Contains the editor properties */
  private static Properties EDITOR_PROPERTIES;

  /** Loads the configuration property file */
  static {

    boolean noDefaultProps = false;
    boolean noUserProps = false;
    boolean noLocalProps = false;
    
    // Properties for user info
    Properties systemProps = System.getProperties();

    // Get default properties from the weka distribution
    Properties defaultProps = new Properties();
    try {
      // Apparently hardcoded slashes are OK here
      // jdk1.1/docs/guide/misc/resources.html
      defaultProps.load(ClassLoader.getSystemResourceAsStream("weka/gui/" +
							      PROPERTY_FILE));
    } catch (Exception ex) {
      noDefaultProps = true;
      System.err.println("Couldn't load default properties: "
			 + ex.getMessage());
    }
    
    // Allow a properties file in the users home directory to override
    Properties userProps = new Properties(defaultProps);
    try {
      userProps.load(new FileInputStream(systemProps.getProperty("user.home")
			 + systemProps.getProperty("file.separator")
			 + PROPERTY_FILE));
    } catch (Exception ex) {
      noUserProps = true;
      System.err.println("Couldn't load user properties: "
			 + ex.getMessage());
    }

    // Allow a properties file in the current directory to override
    EDITOR_PROPERTIES = new Properties(userProps);
    try {
      EDITOR_PROPERTIES.load(new FileInputStream(PROPERTY_FILE));
    } catch (Exception ex) {
      noLocalProps = true;
      System.err.println("Couldn't load localdir properties: "
			 + ex.getMessage());
    }

    if (noDefaultProps && noUserProps && noLocalProps) {
      JOptionPane.showMessageDialog(null,
	  "Could not read a configuration file for the generic object\n"
         +"editor. An example file is included with the Weka distribution.\n"
	 +"This file should be named \"" + PROPERTY_FILE + "\" and\n"
	 +"should be placed either in your user home (which is set\n"
	 + "to \"" + systemProps.getProperty("user.home") + "\")\n"
	 + "or the directory that java was started from\n",
	 "GenericObjectEditor",
	 JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Handles the GUI side of editing values.
   */
  public class GOEPanel extends JPanel implements ItemListener {
    
    /** The chooser component */
    private JComboBox m_ObjectChooser;
    
    /** The component that performs classifier customization */
    private PropertySheetPanel m_ChildPropertySheet;

    /** The model containing the list of names to select from */
    private DefaultComboBoxModel m_ObjectNames;

    /** ok button */
    private JButton m_okBut;
    
    /** cancel button */
    private JButton m_cancelBut;

    /** Creates the GUI editor component */
    public GOEPanel() {

      //System.err.println("GOE()");
      m_ObjectNames = new DefaultComboBoxModel(new String [0]);
      m_ObjectChooser = new JComboBox(m_ObjectNames);
      m_ObjectChooser.setEditable(false);

      m_ChildPropertySheet = new PropertySheetPanel();
      m_ChildPropertySheet.addPropertyChangeListener(
      new PropertyChangeListener() {
	public void propertyChange(PropertyChangeEvent evt) {
	  m_Support.firePropertyChange("", null, null);
	}
      });

      m_okBut = new JButton("OK");
      m_okBut.setEnabled(true);
      m_okBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_Backup = m_Object;
	  if ((getTopLevelAncestor() != null)
	      && (getTopLevelAncestor() instanceof Window)) {
	    Window w = (Window) getTopLevelAncestor();
	    w.dispose();
	  }
	}
      });

      m_cancelBut = new JButton("Cancel");
      m_cancelBut.setEnabled(true);
      m_cancelBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  if (m_Backup != null) {
	    setObject(m_Backup);
	    m_Backup=null;
	    updateClassType();
	    updateChooser();
	    updateChildPropertySheet();
	  }
	  if ((getTopLevelAncestor() != null)
	      && (getTopLevelAncestor() instanceof Window)) {
	    Window w = (Window) getTopLevelAncestor();
	    w.dispose();
	  }
	}
      });
      
      setLayout(new BorderLayout());
      add(m_ObjectChooser, BorderLayout.NORTH);
      add(new JScrollPane(m_ChildPropertySheet), BorderLayout.CENTER);

      JPanel okcButs = new JPanel();
      okcButs.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      okcButs.setLayout(new GridLayout(1, 2, 5, 5));
      okcButs.add(m_okBut);
      okcButs.add(m_cancelBut);
      add(okcButs, BorderLayout.SOUTH);
      
      if (m_ClassType != null) {
	updateClassType();
	updateChooser();
	updateChildPropertySheet();
      }
      m_ObjectChooser.addItemListener(this);
    }
        
    /** Called when the class of object being edited changes. */
    protected void updateClassType() {
      
      Vector classes = getClassesFromProperties();
      m_ObjectChooser.setModel(new DefaultComboBoxModel(classes));
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
      // Set the object as the target of the propertysheet
      m_ChildPropertySheet.setTarget(m_Object);

      // Adjust size of containing window if possible
      if ((getTopLevelAncestor() != null)
	  && (getTopLevelAncestor() instanceof Window)) {
	((Window) getTopLevelAncestor()).pack();
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
	String className = (String)m_ObjectChooser
			     .getSelectedItem();
	try {
	  //System.err.println("Setting object from chooser");
	  setObject((Object)Class
		    .forName(className)
		    .newInstance());
	  //System.err.println("done setting object from chooser");
	} catch (Exception ex) {
	  m_ObjectChooser.hidePopup();
	  m_ObjectChooser.setSelectedIndex(0);
	  JOptionPane.showMessageDialog(this,
					"Could not create an example of\n"
					+ className + "\n"
					+ "from the current classpath",
					"GenericObjectEditor",
					JOptionPane.ERROR_MESSAGE);
	}
      }
    }
  }

  /** Called when the class of object being edited changes. */
  protected Vector getClassesFromProperties() {
    
    Vector classes = new Vector();
    String className = m_ClassType.getName();
    String typeOptions = EDITOR_PROPERTIES.getProperty(className);
    if (typeOptions == null) {
      System.err.println("No configuration property found in\n"
			 + PROPERTY_FILE + "\n"
			 + "for " + className);
    } else {
      StringTokenizer st = new StringTokenizer(typeOptions, ", ");
      while (st.hasMoreTokens()) {
	String current = st.nextToken().trim();
	if (false) {
	  // Verify that class names are OK. Slow -- with Java2 we could
	  // try Class.forName(current, false, getClass().getClassLoader());
	  try {
	    Class c = Class.forName(current);
	    classes.addElement(current);
	  } catch (Exception ex) {
	    System.err.println("Couldn't find class with name" + current);
	  }
	} else {
	  classes.addElement(current);
	}
      }
    }
    return classes;
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
   * Sets the current object to be the default, taken as the first item in
   * the chooser
   */
  public void setDefaultValue() {

    if (m_ClassType == null) {
      System.err.println("No ClassType set up for GenericObjectEditor!!");
      return;
    }
    Vector v = getClassesFromProperties();
    try {
      if (v.size() > 0) {
	setObject((Object)Class.forName((String)v.elementAt(0)).newInstance());
      }
    } catch (Exception ex) {
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
    m_Backup = m_Object;
    m_Object = c;

    if (m_EditorComponent != null) {
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


    if (m_Enabled && m_Object != null) {
      String rep = m_Object.getClass().getName();
      int dotPos = rep.lastIndexOf('.');
      if (dotPos != -1) {
	rep = rep.substring(dotPos + 1);
      }
      if (m_Object instanceof OptionHandler) {
	rep += " " + Utils.joinOptions(((OptionHandler)m_Object)
				       .getOptions());
      }
      FontMetrics fm = gfx.getFontMetrics();
      int vpad = (box.height - fm.getHeight()) / 2;
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
	.registerEditor(weka.attributeSelection.ASEvaluation.class,
			GenericObjectEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(weka.attributeSelection.ASSearch.class,
			GenericObjectEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(SelectedTag.class,
			SelectedTagEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(java.io.File.class,
			FileEditor.class);
      GenericObjectEditor ce = new GenericObjectEditor();
      ce.setClassType(weka.filters.Filter.class);
      Object initial = new weka.filters.AddFilter();
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
