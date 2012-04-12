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
 *    GenericObjectEditor.java
 *    Copyright (C) 2001 Len Trigg, Xin Xu
 *
 */

package weka.gui;

import weka.gui.PropertySheetPanel;
import weka.gui.SelectedTagEditor;
import weka.gui.FileEditor;
import weka.gui.PropertyDialog;

import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.Tag;
import weka.core.SelectedTag;
import weka.core.SerializedObject;
import weka.core.Utils;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
//import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
//import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MouseInputAdapter;

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
 * @author Xin Xu (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.33 $
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
	    java.util.Enumeration keys = 
		(java.util.Enumeration)EDITOR_PROPERTIES.propertyNames();
	    if (!keys.hasMoreElements()) {
		throw new Exception("Failed to read a property file for the "
				    +"generic object editor");
	    }
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
    public class GOEPanel extends JPanel{
	
	/** The chooser component */
	private CascadedComboBox m_ObjectChooser;
	
	/** The component that performs classifier customization */
	private PropertySheetPanel m_ChildPropertySheet;
	
	/** The model containing the list of names to select from */
	private HierarchyPropertyParser m_ObjectNames;
	
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

	/** The Menubar to show the classes */
	private JMenuBar m_MenuBar;	
	
	/** 
	 * The inner class that implements a combo box that
	 * can cascade according to the hierachy structure of
	 * the property.  Note that MenuMouseDrag event is not
	 * supported by this class and the listener to mouse drag
	 * is disabled because of synchronization problems.
	 */
	private class CascadedComboBox extends MouseInputAdapter 
	    implements MenuListener{
	    
	    /** The label showing the property selected */
	    private JMenu label = null;
	    
	    /** The property in selection */
	    private String selected = null;
	
	    public CascadedComboBox(){
		label = new JMenu();
		label.addMouseListener(new MouseInputAdapter(){	  
		// Re-define mouse listener so that drag is not available
			public void mousePressed(MouseEvent e){
			    if((JMenu)e.getSource() == label)
				label.setPopupMenuVisible(false);
			}
			
			public void mouseReleased(MouseEvent e){
			    if((JMenu)e.getSource() == label){ 
				label.setPopupMenuVisible(true);
			    }
			}			
		    });
		
		if(m_ObjectNames.depth() > 0){
		    try{
			m_ObjectNames.goToRoot();
			build();	
		    }catch(Exception e){
			e.printStackTrace();
			System.err.println(e.getMessage());
		    }	    
		    if(m_Object != null)
			setLabel(m_Object.getClass().getName());
		}
	    }
	    
	    /** 
	     * Return the top level menu, which shows the class name
	     *
	     * @return the menu
	     */
	    public JMenu getLabel(){
		return label;
	    }
	    
	    /**
	     * set the text of label menu
	     *
	     * @param text the text to be set
	     */
	    public void setLabel(String text){
		//System.err.println("Label text: "+text);
		label.setText(text);
	    }
	    
	    /** Private procedure to build the cascaded comboBox */
	    private JMenuItem build(){		
		JMenuItem menu;
		int singleItem = 0;
		boolean isFromRoot = m_ObjectNames.isRootReached();
		String delim = m_ObjectNames.getSeperator();		
		StringBuffer sb=new StringBuffer(m_ObjectNames.getValue());

		while(m_ObjectNames.numChildren() == 1){
		    try{
			m_ObjectNames.goToChild(0);
		    }catch(Exception e){}
		    sb.append(delim + m_ObjectNames.getValue());
		    singleItem++;
		}		    		    		
		
		if(isFromRoot && (!m_ObjectNames.isLeafReached())){
		    String[] kids = m_ObjectNames.childrenValues();
		    for(int i=0; i < kids.length; i++){
			String child = kids[i];
			m_ObjectNames.goToChild(child);
			if(m_ObjectNames.isLeafReached()){
			    menu = new JMenuItem(m_ObjectNames.fullValue());
			    menu.addMouseListener(this);
			}
			else{
			    menu = new JMenu(m_ObjectNames.fullValue());
			    ((JMenu)menu).addMenuListener(this);
			    String[] grandkids = 
				m_ObjectNames.childrenValues();
			    for(int j=0; j < grandkids.length; j++){
				m_ObjectNames.goToChild(grandkids[j]);
				menu.add(build());
			    }				
			}

			m_ObjectNames.goToParent();
			label.add(menu);
		    }
		    menu = null;
		}
		else if(m_ObjectNames.isLeafReached()){
		    menu = new JMenuItem(sb.toString());
		    menu.addMouseListener(this);
		} 
		else{
		    menu = new JMenu(sb.toString());
		    ((JMenu)menu).addMenuListener(this);		
		    
		    String[] kids = m_ObjectNames.childrenValues();
		    for(int i=0; i < kids.length; i++){
			String child = kids[i];
			m_ObjectNames.goToChild(child);
			menu.add(build());
		    }
		}
		
		for(int i=0; i <= singleItem; i++)
		    m_ObjectNames.goToParent(); // One more level up		    
		
		return menu;		
	    }    
	    
	    /**
	     * Search the HierarchyPropertyParser for the given class name,
	     * if not found, add it into the appropriate position.  Does
	     * nothing if already there
	     *
	     * @param className the given class name
	     */
	    public void add(String className){
		if(!m_ObjectNames.contains(className)){
		    m_ObjectNames.add(className);
		    m_ObjectNames.goToRoot();
		    try{			
			build();	
		    }catch(Exception e){
		    }
		    
		    if(m_Object != null)
			setLabel(m_Object.getClass().getName());
		}
	    }
	    
	    /** 
	     * Called when a menu is selected, to update the underlying
	     * HierarchyPropertyParser, m_ObjectNames.
	     * 
	     * @param e the menuEvent concerned
	     */
	    public void menuSelected(MenuEvent e){
		JMenu child = (JMenu)e.getSource();
		String value = child.getText();		
		
		if(!m_ObjectNames.goToChild(value))  // Idempotent if value wrong
		    if(!m_ObjectNames.goDown(value)) // If values merged
			m_ObjectNames.goTo(value);   // If merged from the root
		
		setLabel(m_ObjectNames.fullValue());
		//System.err.println("Select menu: " + m_ObjectNames.fullValue()+ "|" + value); 
	    }
	    
	    /** 
	     * Called when a menu is de-selected, to update the underlying
	     * HierarchyPropertyParser, m_ObjectNames.
	     * 
	     * @param e the menuEvent concerned
	     */
	    public void menuDeselected(MenuEvent e){
		JMenu target = (JMenu)e.getSource();
		StringTokenizer sptk = 
		    new StringTokenizer(target.getText(), 
					m_ObjectNames.getSeperator());
		sptk.nextToken();
		while(sptk.hasMoreTokens()){		    
		    m_ObjectNames.goToParent();
		    sptk.nextToken();
		}
		m_ObjectNames.goToParent();
		
		if(label.isMenuComponent(target)) // Top level reached 
		    setLabel(m_Object.getClass().getName());	   
		else
		    setLabel(m_ObjectNames.fullValue());	
		//System.err.println("Deselect menu: " + m_ObjectNames.fullValue());
	    }
	    
	    /** 
	     * Called when a menu is cancelled, to update the underlying
	     * HierarchyPropertyParser, m_ObjectNames.
	     * 
	     * @param e the menuEvent concerned
	     */
	    public void menuCanceled(MenuEvent e){
		menuDeselected(e);
	    }		
	    
	    /** 
	     * Called when a leaf menu item is temporarily selected, to update
	     * the underlying HierarchyPropertyParser, m_ObjectNames and record
	     * the class in selection
	     * 
	     * @param e the MouseEvent concerned
	     */
	    public void mouseEntered(MouseEvent me){
		JMenuItem target = (JMenuItem)me.getSource();		
		String value = target.getText();
		
		if(!m_ObjectNames.goToChild(value)) // Idempotent if value wrong
		    if(!m_ObjectNames.goDown(value)) // If values merged
			m_ObjectNames.goTo(value);   // If merged from the root
		
		selected = m_ObjectNames.fullValue();
		setLabel(selected);		
		//System.err.println("Mouse in: " + m_ObjectNames.fullValue()
		//		   + " | "+ value);
	    }
	    
	    /** 
	     * Called when a leaf menu item is no longer selected, to update the 
	     * underlying HierarchyPropertyParser, m_ObjectNames.
	     * 
	     * @param e the MouseEvent concerned
	     */
	    public void mouseExited(MouseEvent me){
		JMenuItem target = (JMenuItem)me.getSource();
		StringTokenizer sptk = 
		    new StringTokenizer(target.getText(), 
					m_ObjectNames.getSeperator());
		
		sptk.nextToken();
		while(sptk.hasMoreTokens()){
		    sptk.nextToken(); 
		    m_ObjectNames.goToParent();
		}		

		m_ObjectNames.goToParent();
		
		selected = null;
		setLabel(m_ObjectNames.fullValue());
		//System.err.println("Mouse out: " + m_ObjectNames.fullValue());
	    }
	    
	    /** 
	     * Called when a leaf menu item is confirmatively selected, to record the
	     * class in selection and try to load it
	     * 
	     * @param e the MouseEvent concerned
	     */
	    public void mouseReleased(MouseEvent me){
		try {		    
		    if ((m_Object != null) && m_Object.getClass().getName().equals(selected)){
			setLabel(m_Object.getClass().getName());
			return;
		    }
		    
		    // System.err.println("Different class type");
		    setValue(Class.forName(selected).newInstance());		   
		    // System.err.println("done setting object from chooser"); 
		} catch (Exception ex) {
		    JOptionPane.showMessageDialog(null,
						  "Could not create an example of\n"
						  + selected + "\n"
						  + "from the current classpath",
						  "example loaded",
						  JOptionPane.ERROR_MESSAGE);
		    try{
			if(m_Backup != null)
			    setValue(m_Backup);
			else
			    setDefaultValue();			
		    }catch(Exception e){
			System.err.println(ex.getMessage());
			ex.printStackTrace();
		    }
		}
		//System.err.println("Mouse released: " + m_ObjectNames.fullValue()+ "|" + selected +" |Backup: "+ m_Backup.getClass().getName()+" |Object: "+m_Object.getClass().getName());
		JMenuItem target = (JMenuItem)me.getSource();
		StringTokenizer sptk = 
		    new StringTokenizer(target.getText(), 
					m_ObjectNames.getSeperator());
		sptk.nextToken();
		while(sptk.hasMoreTokens()){
		    sptk.nextToken();	
		    m_ObjectNames.goToParent();
		}
		m_ObjectNames.goToParent();				
		selected = null;	    	   
	    }
	}
	
	/** Creates the GUI editor component */
	public GOEPanel() {
	    //System.err.println("GOE(): " + m_Object);	    
	    m_Backup = copyObject(m_Object);
	    
	    m_MenuBar = new JMenuBar();
	    m_MenuBar.setLayout(new BorderLayout());	
	    m_ObjectNames = new HierarchyPropertyParser();
	    //if(m_ObjectChooser == null)
	    m_ObjectChooser = new CascadedComboBox();		
	    m_MenuBar.add(m_ObjectChooser.getLabel(), BorderLayout.CENTER);
	    m_MenuBar.setSelected(m_ObjectChooser.getLabel());

	    m_ChildPropertySheet = new PropertySheetPanel();
	    m_ChildPropertySheet.addPropertyChangeListener
		(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
			    m_Support.firePropertyChange("", null, null);
			}
		    });
	    
	    m_OpenBut = new JButton("Open...");
	    m_OpenBut.setToolTipText("Load a configured object");
	    m_OpenBut.setEnabled(true);
	    m_OpenBut.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			Object object = openObject();
			if (object != null) {
			    // setValue takes care of: Making sure obj is of right type,
			    // and firing property change.
			    setValue(object);
			    // Need a second setValue to get property values filled in OK.
			    // Not sure why.
			    setValue(object);
			}
		    }
		});
	    
	    m_SaveBut = new JButton("Save...");
	    m_SaveBut.setToolTipText("Save the current configured object");
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
			//System.err.println("\nOK--Backup: "+
			// m_Backup.getClass().getName()+
			//	   "\nOK--Object: "+
			//		   m_Object.getClass().getName());
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
			    //System.err.println("\nCncl--Backup: "+
			    // m_Backup.getClass().getName()+
			    //	       "\nCncl--Object: "+
			    //       m_Object.getClass().getName());
			    m_Object = copyObject(m_Backup);

			    // To fire property change
			    m_Support.firePropertyChange("", null, null);
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
	   
	    add(m_MenuBar, BorderLayout.NORTH);
	    add(m_ChildPropertySheet, BorderLayout.CENTER);
	    // Since we resize to the size of the property sheet, a scrollpane isn't
	    // typically needed
	    // add(new JScrollPane(m_ChildPropertySheet), BorderLayout.CENTER);
	    
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
		if(m_Object != null){
		    updateChooser();
		    updateChildPropertySheet();
		}
	    }
	}

	/**
	 * Enables/disables the cancel button.
	 *
	 * @param flag true to enable cancel button, false
	 * to disable it
	 */
	protected void setCancelButton(boolean flag) {
	    if(m_cancelBut != null)
		m_cancelBut.setEnabled(flag);
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
		SerializedObject so = new SerializedObject(source);
		result = so.getObject();
		setCancelButton(true);
		
	    } catch (Exception ex) {
		setCancelButton(false);
		System.err.println("GenericObjectEditor: Problem making backup object");
		System.err.println(ex);
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
    
	    //System.err.println("GOE::updateClassType()"); 
	    m_ObjectNames = getClassesFromProperties();
	    if(m_ObjectNames.depth() > 0){
		m_ObjectChooser = new CascadedComboBox();
		m_MenuBar.removeAll();
		m_MenuBar.add(m_ObjectChooser.getLabel(), BorderLayout.CENTER);
	    }
	}
		
	/** Called to update the cascaded combo box of the values to
	 * to be selected from */
	protected void updateChooser(){
  	    
	    String objectName = m_Object.getClass().getName();	    	
	    // Try to add m_0bject. If already there, simply ignore.
 	    m_ObjectChooser.add(objectName);	    
	        
	    m_ObjectChooser.setLabel(objectName);
	    //m_MenuBar.setSelected(m_ObjectChooser.getLabel());
	    repaint(); 
	    //System.err.println("GOE::updateChooser(): " +
	    //		       objectName + " |feedback: "+
	    //		       ((JMenu)m_MenuBar.getComponent(0)).getText());
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
    }
        
    
    /** Called when the class of object being edited changes. */
    protected HierarchyPropertyParser getClassesFromProperties() {	    
	HierarchyPropertyParser hpp = new HierarchyPropertyParser();
	String className = m_ClassType.getName();
	String typeOptions = EDITOR_PROPERTIES.getProperty(className);
	if (typeOptions == null) {
	    System.err.println("Warning: No configuration property found in\n"
			       + PROPERTY_FILE + "\n"
			       + "for " + className);
	} else {		    
	    try {
		hpp.build(typeOptions, ", ");
	    } catch (Exception ex) {
		System.err.println("Invalid property: " + typeOptions);
	    }	    
	}
	return hpp;
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
	//		   + (type == null? "<null>" : type.getName()) + ")");
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
	
	//System.err.println("GOE::setDefaultValue()");
	if (m_ClassType == null) {
	    System.err.println("No ClassType set up for GenericObjectEditor!!");
	    return;
	}	
	
	HierarchyPropertyParser hpp = getClassesFromProperties();
	try{
	    if(hpp.depth() > 0){		
		hpp.goToRoot();
		while(!hpp.isLeafReached())
		    hpp.goToChild(0);
		
		String defaultValue = hpp.fullValue();
		setValue(Class.forName(defaultValue).newInstance());
	    }
	}catch(Exception ex){
	    System.err.println("Problem loading the first class: "+
			       hpp.fullValue());
	    ex.printStackTrace();
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
	
	if(m_EditorComponent != null)
	    m_EditorComponent.updateChooser();
    }
    
    /**
     * Sets the current Object, but doesn't worry about updating
     * the state of the object chooser.
     *
     * @param c a value of type 'Object'
     */
    private void setObject(Object c) {
	
	// This should really call equals() for comparison.
	boolean trueChange ;
	if(getValue() != null){
	    trueChange = (!c.equals(getValue()));
	    //System.err.println("GEO::setObject(): Changed? " + trueChange+ getValue().getClass().getName());
	}
	else
	    trueChange = true;

	/*
	  System.err.println("Didn't even try to make a Object copy!! "
	  + "(using original)");
	*/
	m_Backup = m_Object;

	m_Object = c;
	
	if (m_EditorComponent != null) {
	    m_EditorComponent.updateChildPropertySheet();
	    if (trueChange) {
		m_Support.firePropertyChange("", null, null);
	    }
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
      ce.setClassType(weka.classifiers.Classifier.class);
      Object initial = new weka.classifiers.rules.ZeroR();
      if (args.length > 0){
	  ce.setClassType(Class.forName(args[0]));
	  if(args.length > 1){
	      initial = (Object)Class.forName(args[1]).newInstance();
	      ce.setValue(initial);
	  }
	  else
	      ce.setDefaultValue();
      }
      else	  
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
