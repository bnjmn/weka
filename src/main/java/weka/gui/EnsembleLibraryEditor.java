/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    EnsembleLibraryEditor.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui;

import weka.classifiers.EnsembleLibrary;
import weka.gui.ensembleLibraryEditor.AddModelsPanel;
import weka.gui.ensembleLibraryEditor.ListModelsPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Class for editing Library objects. Brings up a custom editing panel for the
 * user to edit the library model list, as well as save load libraries from
 * files.
 * <p/>
 * A model list file is simply a flat file with a single classifier on each
 * line. Each of these classifier is represented by the command line string that
 * would be used to create that specific model with the specified set of
 * paramters.
 * <p/>
 * This code in class is based on other custom editors in weka.gui such as the
 * CostMatrixEditor to try and maintain some consistency throughout the package.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision$
 */
public class EnsembleLibraryEditor
  implements PropertyEditor {
  
  /** The library being edited */
  protected EnsembleLibrary m_Library;
  
  /** A helper class for notifying listeners */
  protected transient PropertyChangeSupport m_PropSupport = new PropertyChangeSupport(
      this);
  
  /** An instance of the custom editor */
  protected transient CustomEditor m_CustomEditor = new CustomEditor();
  
  /**
   * the main panel of the editor that will let the user switch between the
   * different editing panels.
   */
  protected transient JTabbedPane m_ModelOptionsPane = new JTabbedPane();
  
  /**
   * the main panel that will display the current set of models in the library
   */
  protected transient ListModelsPanel m_ListModelsPanel = new ListModelsPanel(
      m_Library);
  
  /**
   * This class presents a GUI for editing the library, and saving and loading
   * model list from files.
   */
  private class CustomEditor
    extends JPanel {
    
    /** for serialization */
    private static final long serialVersionUID = -1553490570313707008L;

    /**
     * Constructs a new editor.
     */
    public CustomEditor() {
      
      m_ModelOptionsPane = new JTabbedPane();
      
      m_ListModelsPanel = new ListModelsPanel(m_Library);
      
      m_ModelOptionsPane.addTab("Current Library", m_ListModelsPanel);
      
      m_ModelOptionsPane.addTab("Add Models", new AddModelsPanel(
	  m_ListModelsPanel));
      
      // All added panels needs a reference to the main one so that
      // they can add models to it.
      setLayout(new BorderLayout());
      add(m_ModelOptionsPane, BorderLayout.CENTER);
    }
    
  }
  
  /**
   * Constructs a new LibraryEditor.
   */
  public EnsembleLibraryEditor() {
    m_PropSupport = new PropertyChangeSupport(this);
    m_CustomEditor = new CustomEditor();
  }
  
  /**
   * Sets the value of the Library to be edited.
   * 
   * @param value
   *            a Library object to be edited
   */
  public void setValue(Object value) {
    m_Library = (EnsembleLibrary) value;
    m_ListModelsPanel.setLibrary(m_Library);
  }
  
  /**
   * Gets the cost matrix that is being edited.
   * 
   * @return the edited CostMatrix object
   */
  public Object getValue() {
    return m_Library;
  }
  
  /**
   * Indicates whether the object can be represented graphically. In this case
   * it can.
   * 
   * @return true
   */
  public boolean isPaintable() {
    return true;
  }
  
  /**
   * Paints a graphical representation of the Object. For the ensemble library
   * it prints out the working directory as well as the number of models in
   * the library
   * 
   * @param gfx
   *            the graphics context to draw the representation to
   * @param box
   *            the bounds within which the representation should fit.
   */
  public void paintValue(Graphics gfx, Rectangle box) {
    gfx.drawString(m_Library.size() + " models selected", box.x, box.y
	+ box.height);
  }
  
  /**
   * Returns the Java code that generates an object the same as the one being
   * edited. Unfortunately this can't be done in a single line of code, so the
   * code returned will only build a default cost matrix of the same size.
   * 
   * @return the initialization string
   */
  public String getJavaInitializationString() {
    return ("new Library(" + m_Library.size() + ")");
  }
  
  /**
   * Some objects can be represented as text, but a library cannot.
   * 
   * @return null
   */
  public String getAsText() {
    return null;
  }
  
  /**
   * Some objects can be represented as text, but a library cannot.
   * 
   * @param text
   *            ignored
   * @throws always throws an IllegalArgumentException
   */
  public void setAsText(String text) {
    throw new IllegalArgumentException("LibraryEditor: "
	+ "Library properties cannot be " + "expressed as text");
  }
  
  /**
   * Some objects can return tags, but a cost matrix cannot.
   * 
   * @return null
   */
  public String[] getTags() {
    return null;
  }
  
  /**
   * Gets a GUI component with which the user can edit the cost matrix.
   * 
   * @return an editor GUI component
   */
  public Component getCustomEditor() {
    return m_CustomEditor;
  }
  
  /**
   * Indicates whether the library can be edited in a GUI, which it can.
   * 
   * @return true
   */
  public boolean supportsCustomEditor() {
    return true;
  }
  
  /**
   * Adds an object to the list of those that wish to be informed when the
   * library changes.
   * 
   * @param listener
   *            a new listener to add to the list
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    m_Library.addPropertyChangeListener(listener);
  }
  
  /**
   * Removes an object from the list of those that wish to be informed when
   * the cost matrix changes.
   * 
   * @param listener
   *            the listener to remove from the list
   */
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    m_PropSupport.removePropertyChangeListener(listener);
  }
  
  // ***************************************************************
  // The following three methods seem sort of out of place here.
  // Basically they are helper methods for the various classes that
  // constitute the AddModelsPanel. These static methods needed to
  // go here because the objects (PropertyText, PropertyPanel, etc)
  // involved are not visible from the weka.gui.libraryEditor
  // package where the AddModelsPanel classes reside.  If these 
  // objects ever became public then these three methods should be
  // moved within the gui.LibraryEditor package
  // ***************************************************************
  
  /**
   * This method handles the different object editor types in weka to obtain
   * their current values.
   * 
   * @param source	an Editor
   * @return		the value of the editor
   */
  public static Object getEditorValue(Object source) {
    
    Object value = null;
    
    if (source instanceof GenericArrayEditor) {
      value = ((GenericArrayEditor) source).getValue();
    } else if (source instanceof CostMatrixEditor) {
      value = ((CostMatrixEditor) source).getValue();
    } else if (source instanceof PropertyText) {
      value = ((PropertyText) source).getText();
    } else if (source instanceof PropertyEditor) {
      value = ((PropertyEditor) source).getValue();
    }
    
    return value;
  }
  
  /**
   * This is a helper function that creates a renderer for Default Objects.
   * These are basically objects that are not numeric, nominal, or generic
   * objects. These are objects that we don't want to do anything special with
   * and just display their values as normal. We simply create the editor the
   * same way that they would have been created in the PropertySheetPanel
   * class.
   * 
   * @param nodeEditor
   *            the editor created for the defaultNode
   * @return the Component to dispaly the defaultNode
   */
  public static Component getDefaultRenderer(PropertyEditor nodeEditor) {
    
    Component genericRenderer = null;
    
    if (nodeEditor.isPaintable() && nodeEditor.supportsCustomEditor()) {
      genericRenderer = new PropertyPanel(nodeEditor);
    } else if (nodeEditor.getTags() != null) {
      genericRenderer = new PropertyValueSelector(nodeEditor);
    } else if (nodeEditor.getAsText() != null) {
      // String init = editor.getAsText();
      genericRenderer = new PropertyText(nodeEditor);
      ((PropertyText) genericRenderer).setColumns(20);
    } else {
      System.err.println("Warning: Property \""
	  + nodeEditor.getClass().toString()
	  + "\" has non-displayabale editor.  Skipping.");
    }
    
    return genericRenderer;
  }
  
  /**
   * This is a helper function that creates a renderer for GenericObjects
   * 
   * @param classifierEditor
   *            the editor created for this
   * @return object renderer
   */
  public static Component createGenericObjectRenderer(
      GenericObjectEditor classifierEditor) {
    
    PropertyPanel propertyPanel = new PropertyPanel(classifierEditor);
    
    return propertyPanel;
  }
  
  
  /**
   * This is a simple main method that lets you run a LibraryEditor
   * on its own without having to deal with the Explorer, etc... 
   * This is useful only for building model lists.
   * 
   * @param args	the commandline arguments
   */
  public static void main(String[] args) {
    
    EnsembleLibrary value = new EnsembleLibrary();
    EnsembleLibraryEditor libraryEditor = new EnsembleLibraryEditor();
    libraryEditor.setValue(value);
    
    JPanel editor = (JPanel) libraryEditor.getCustomEditor();
    
    JFrame frame = new JFrame();
    frame.getContentPane().add(editor);
    
    frame.pack();
    frame.setVisible(true);
  }
  
}
