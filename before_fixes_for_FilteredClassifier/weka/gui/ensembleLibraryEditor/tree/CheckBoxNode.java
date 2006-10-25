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
 *    CheckBoxNode.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor.tree;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class is responsible for implementing the underlying logic of 
 * tree nodes representing a single nominal value.  This is either going to
 * be true/false values or an enumeration of values defined by the model.
 * Check box nodes are relatively simple in that they are simply toggled
 * on or off by the user indicating whether or not they are to be used.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.1 $
 */
public class CheckBoxNode
  extends DefaultMutableTreeNode
  implements ItemListener {
  
  /** for serialization */
  private static final long serialVersionUID = 727140674668443817L;

  /** tracks whether this node is currently selected as a model parameter */
  private boolean m_Selected;
  
  /** the tip text for our node editor to display */
  private String m_ToolTipText;
  
  /**
   * The constructor initializes the members of this node.  Note that
   * the text String is stored as the userObject.
   * 
   * @param name the name of this attribute
   * @param selected the initial value of this node
   * @param toolTipText the toolTipText to be displayed
   */
  public CheckBoxNode(String name, boolean selected, String toolTipText) {
    super(name);
    setName(name);
    this.m_Selected = selected;
    this.m_ToolTipText = toolTipText;
  }
  
  /**
   * getter for the node state
   * 
   * @return whether or not this node is selected
   */
  public boolean getSelected() {
    return m_Selected;
  }
  
  /**
   * setter for the node state
   * 
   * @param newValue the new selected state
   */
  public void setSelected(boolean newValue) {
    m_Selected = newValue;
  }
  
  /**
   * sets whether the box is selected
   * 
   * @param newValue	if true the box will be selected
   */
  public void setBoxSelected(boolean newValue) {
    m_Selected = newValue;
  }
  
  /**
   * gets the name of the parameter value represented by this node 
   * which is stored as the node's user object
   * 
   * @return the name of this parameter
   */
  public String getName() {
    return (String) getUserObject().toString();
  }
  
  /**
   * sets the name of the parameter value represented by this node 
   * and stores it as the node's user object
   * 
   * @param newValue	the new name
   */
  public void setName(String newValue) {
    setUserObject(newValue);
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
   * this is a simple filter for the setUserObject method.  We basically
   * don't want null values to be passed in.
   * 
   * @param o		the user object
   */
  public void setUserObject(Object o) {
    if (o != null)
      super.setUserObject(o);
  }
  
  /**
   * ToString methods prints out the toString method of this nodes user 
   * object
   * 
   * @return		a string representation
   */
  public String toString() {
    return getClass().getName() + "[" + getUserObject() + "/" + m_Selected
    + "]";
  }
  
  /**
   * This is the listener that fires when the check box is actually toggled.
   * Here we just want to change the selected state accordingly.
   * 
   * @param e		the event
   */
  public void itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      setBoxSelected(true);
      
    } else if (e.getStateChange() == ItemEvent.DESELECTED) {
      setBoxSelected(false);
    }
  }
}