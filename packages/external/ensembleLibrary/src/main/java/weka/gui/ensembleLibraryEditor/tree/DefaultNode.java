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
 *    DefaultNode.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor.tree;

import weka.gui.EnsembleLibraryEditor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class is responsible for representing objects that we haven't explicitly
 * written custom tree node editors for. In other words - Objects that are
 * "weird". It didn't make sense for us to try to come up with a nice tree
 * representation for absolutely everything, e.g. CostMatrixes or ArrayLists.
 * This class is responsible for representing Classifier parameter values that
 * are not numbers, an enumeration of values (e.g. true/false), or Objects that
 * have their own GenericObjectEditors (like other Classifiers). So in these
 * cases we can just use the default editor that came with the object.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision$
 */
public class DefaultNode
  extends DefaultMutableTreeNode
  implements PropertyChangeListener {
  
  /** for serialization */
  private static final long serialVersionUID = -2182147677358461880L;

  /** the name of this node */
  private String m_Name;
  
  /** the tip text for our node editor to display */
  private String m_ToolTipText;
  
  /** The default PropertyEditor that was supplied for this node */
  private PropertyEditor m_PropertyEditor;
  
  /**
   * The constructor initializes the members of this node.
   * 
   * @param name
   *            the name of the value represented by this node
   * @param toolTipText
   *            the tool tip text to be displayed
   * @param value
   *            the intial value
   * @param propertyEditor
   *            the editor provided for this node
   */
  public DefaultNode(String name, String toolTipText, Object value,
      PropertyEditor propertyEditor) {
    
    super(value);
    
    this.m_Name = name;
    this.m_ToolTipText = toolTipText;
    this.m_PropertyEditor = propertyEditor;
    
    m_PropertyEditor.addPropertyChangeListener(this);
    
  }
  
  /**
   * this returns the property editor that was provided for this object. This
   * propertyEditor object is initially chosen inside of the GenericObjectNode
   * updateTree() method if you are interested in where it comes from.
   * 
   * @return the default editor for this node
   */
  public PropertyEditor getEditor() {
    m_PropertyEditor.setValue(this.getUserObject());
    return m_PropertyEditor;
  }
  
  /**
   * getter for the tooltip text
   * 
   * @return tooltip text
   */
  public String getToolTipText() {
    return m_ToolTipText;
  }
  
  /**
   * gets the name of the parameter value represented by this node
   * 
   * @return the name of this parameter
   */
  public String getName() {
    return m_Name;
  }
  
  /**
   * this is a simple filter for the setUserObject method. We basically don't
   * want null values to be passed in.
   * 
   * @param o		the user object
   */
  public void setUserObject(Object o) {
    if (o != null)
      super.setUserObject(o);
  }
  
  /**
   * ToString method simply prints out the user object toString for this node
   * 
   * @return		a string representation
   */
  public String toString() {
    return getClass().getName() + "[" + getUserObject().toString() + "]";
  }
  
  /**
   * This implements the PropertyChangeListener for this node that gets
   * registered with its Editor. All we really have to do is change the Object
   * value stored internally at this node when its editor says the value
   * changed.
   * 
   * @param evt		the event
   */
  public void propertyChange(PropertyChangeEvent evt) {
    
    Object source = evt.getSource();
    
    Object value = EnsembleLibraryEditor.getEditorValue(source);
    
    /*
     * //was useful for debugging when we encountered some strange value
     * types //these printouts tell you the classes that the editor is
     * supplying System.out.println("prop name: " + evt.getPropertyName() +
     * "new value: " + evt.getNewValue() + "old value: " +
     * evt.getOldValue()); System.out.println("prop val: " +
     * source.toString() + " expected val: " + value);
     */
    setUserObject(value);
  }
}
