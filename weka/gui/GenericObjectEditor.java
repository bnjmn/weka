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
import weka.core.Utils;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.io.File;
import java.io.FileOutputStream;


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
 * @version $Revision: 1.19 $
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
  protected static String PROPERTY_FILE = "weka/gui/GenericObjectEditor.props";

  /** Contains the editor properties */
  private static Properties EDITOR_PROPERTIES;

  /** Loads the configuration property file */
  static {

    // Allow a properties file in the current directory to override
    try {
      EDITOR_PROPERTIES = Utils.readProperties(PROPERTY_FILE);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null,
          "Could not read a configuration file for the generic object\n"
         +"editor. An example file is included with the Weka distribution.\n"
	 +"This file should be named \"" + PROPERTY_FILE + "\" and\n"
	 +"should be placed either in your user home (which is set\n"
	 + "to \"" + System.getProperties().getProperty("user.home") + "\")\n"
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

    /** Open object from disk */
    private JButton m_OpenBut;

    /** Save object to disk */
    private JButton m_SaveBut;

    /** ok button */
    private JButton m_okBut;
    
    /** cancel button */
    private JButton m_cancelBut;

    /** The filechooser for opening and saving object files */
    private JFileChooser m_FileChooser;

    /** Creates the GUI editor component */
    public GOEPanel() {
      m_Backup = copyObject(m_Object);
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

      m_OpenBut = new JButton("Open...");
      m_OpenBut.setEnabled(true);
      m_OpenBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  Object object = openObject();
          if (object != null) {
            System.err.println("Loaded object");
            setObject(object);
            updateClassType();
            updateChooser();
            updateChildPropertySheet();
          }
          // Make sure obj is of right type. 
          // Fire prop change.
	}
      });

      m_SaveBut = new JButton("Save...");
      m_SaveBut.setEnabled(true);
      m_SaveBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  saveObject(m_Object);
	}
      });

      m_okBut = new JButton("OK");
      m_okBut.setEnabled(true);
      m_okBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_Backup = copyObject(m_Object);
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
	    m_Object = copyObject(m_Backup);
	    setObject(m_Object);
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
      okcButs.setLayout(new GridLayout(1, 4, 5, 5));
      okcButs.add(m_OpenBut);
      okcButs.add(m_SaveBut);
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

    /**
     * Opens an object from a file selected by the user.
     * 
     * @return the loaded object, or null if the operation was cancelled
     */
    protected Object openObject() {

      if (m_FileChooser == null) {
        createFileChooser();
      }
      int returnVal = m_FileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	File selected = m_FileChooser.getSelectedFile();
	try {
	  ObjectInputStream oi = new ObjectInputStream(new BufferedInputStream(new FileInputStream(selected)));
          Object obj = oi.readObject();
          oi.close();
          if (!m_ClassType.isAssignableFrom(obj.getClass())) {
            throw new Exception("Object not of type: " + m_ClassType.getName());
          }
          return obj;
	} catch (Exception ex) {
	  JOptionPane.showMessageDialog(this,
					"Couldn't read object: "
					+ selected.getName() 
					+ "\n" + ex.getMessage(),
					"Open object file",
					JOptionPane.ERROR_MESSAGE);
	}
      }
      return null;
    }

    /**
     * Opens an object from a file selected by the user.
     * 
     * @return the loaded object, or null if the operation was cancelled
     */
    protected void saveObject(Object object) {

      if (m_FileChooser == null) {
        createFileChooser();
      }
      int returnVal = m_FileChooser.showSaveDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	File sFile = m_FileChooser.getSelectedFile();
	try {
	  ObjectOutputStream oo = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(sFile)));
          oo.writeObject(object);
          oo.close();
	} catch (Exception ex) {
	  JOptionPane.showMessageDialog(this,
					"Couldn't write to file: "
					+ sFile.getName() 
					+ "\n" + ex.getMessage(),
					"Save object",
					JOptionPane.ERROR_MESSAGE);
	}
      }
    }

    protected void createFileChooser() {

      m_FileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
      m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    }

    /**
     * Makes a copy of an object using serialization
     * @param source the object to copy
     * @return a copy of the source object
     */
    protected Object copyObject(Object source) {
      Object result = null;
      try {
	ByteArrayOutputStream bo = new ByteArrayOutputStream();
	BufferedOutputStream bbo = new BufferedOutputStream(bo);
	ObjectOutputStream oo = new ObjectOutputStream(bbo);
	oo.writeObject(source);
	oo.close();
	
	ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
	BufferedInputStream bbi = new BufferedInputStream(bi);
	ObjectInputStream oi = new ObjectInputStream(bbi);
	result = oi.readObject();
	oi.close();
      } catch (Exception ex) {
	System.err.println("GenericObjectEditor: Problem making backup object");
	System.err.print(ex);
      }
      return result;
    }

    /** 
     * This is used to hook an action listener to the ok button
     * @param a The action listener.
     */
    public void addOkListener(ActionListener a) {
      m_okBut.addActionListener(a);
    }

    /**
     * This is used to hook an action listener to the cancel button
     * @param a The action listener.
     */
    public void addCancelListener(ActionListener a) {
      m_cancelBut.addActionListener(a);
    }
    
    /**
     * This is used to remove an action listener from the ok button
     * @param a The action listener
     */
    public void removeOkListener(ActionListener a) {
      m_okBut.removeActionListener(a);
    }

    /**
     * This is used to remove an action listener from the cancel button
     * @param a The action listener
     */
    public void removeCancelListener(ActionListener a) {
      m_cancelBut.removeActionListener(a);
    }

     
    /** Called when the class of object being edited changes. */
    protected void updateClassType() {
      
      Vector classes = getClassesFromProperties();
      m_ObjectChooser.setModel(new DefaultComboBoxModel(classes));
      if (classes.size() > 0) {
	add(m_ObjectChooser, BorderLayout.NORTH);
      } else {
	remove(m_ObjectChooser);
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
      System.err.println("Warning: No configuration property found in\n"
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
