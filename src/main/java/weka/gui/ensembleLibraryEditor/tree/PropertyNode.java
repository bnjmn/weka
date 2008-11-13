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
 *    PropertyNode.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor.tree;

import weka.gui.GenericObjectEditor;
import weka.gui.ensembleLibraryEditor.AddModelsPanel;

import java.beans.PropertyEditor;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * This node class represents individual parameters of generic objects
 * (in practice this means classifiers).  So all direct children of a
 * classifier or other generic objects in the tree are going to be 
 * property nodes.  Note that these nodes do not themselves have editors
 * all editing in the user interface actaully happens in the child
 * nodes of this class that it controls.  On top of creating these 
 * child nodes and initializing them with the correct editing 
 * configuration, this class is also responsible for obtaining all of 
 * the possible values from the child nodes.
 *  
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.1 $
 */
public class PropertyNode 
  extends DefaultMutableTreeNode {
  
  /** for serialization */
  private static final long serialVersionUID = 8179038568780212829L;

  /** this is a reference to the parent panel of the JTree which is
   * needed to display correctly anchored JDialogs*/
  private final AddModelsPanel m_ParentPanel;
  
  /** the name of the node to be displayed */
  private String m_Name;
  
  /** the node's tip text*/
  private String m_ToolTipText;
  
  /** The propertyEditor created for the node, this is very useful in
   * figuring out exactly waht kind of child editor nodes to create */
  private PropertyEditor m_PropertyEditor;
  
  /** a reference to the tree model is necessary to be able to add and 
   * remove nodes in the tree */
  private DefaultTreeModel m_TreeModel;
  
  /** a reference to the tree */
  private JTree m_Tree;
  
  /**
   * The constructor initialiazes the member variables of this node, 
   * Note that the "value" of this generic object is stored as the treeNode
   * user object. After the values are initialized the constructor calls
   * the addEditorNodes to create all the child nodes necessary to allow
   * users to specify ranges of parameter values meaningful for the 
   * parameter that this node represents.
   * 
   * @param tree		the tree to use
   * @param panel		the pabel
   * @param name		the name
   * @param toolTipText		the tooltip
   * @param value		the actual value
   * @param pe			the property editor
   */
  public PropertyNode(JTree tree, AddModelsPanel panel, String name,
      String toolTipText, Object value, PropertyEditor pe) {
    
    super(value);
    
    m_Tree = tree;
    m_TreeModel = (DefaultTreeModel) m_Tree.getModel();
    m_ParentPanel = panel;
    m_Name = name;
    m_ToolTipText = toolTipText;
    m_PropertyEditor = pe;
    
    addEditorNodes(name, toolTipText);
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
   * getter for the name to be displayed for this node
   * 
   * @return		the name
   */
  public String getName() {
    return m_Name;
  }
  
  /**
   * this returns the property editor that was provided for this object. This
   * propertyEditor object is initially chosen inside of the GenericObjectNode
   * updateTree() method if you are interested in where it comes from.
   * 
   * @return the default editor for this node
   */
  public PropertyEditor getPropertyEditor() {
    return m_PropertyEditor;
  }
  
  /**
   * returns a string representation
   * 
   * @return		a string representation
   */
  public String toString() {
    return getClass().getName() + "[" + getUserObject().toString() + "]";
  }
  
  /**
   * This method figures out what kind of parameter type this node 
   * represents and then creates the appropriate set of child nodes 
   * for editing.
   * 
   * @param name	the name
   * @param toolTipText	the tooltip
   */
  public void addEditorNodes(String name, String toolTipText) {
    
    Object value = getUserObject();
    
    if (value instanceof Number) {
      
      NumberNode minNode = new NumberNode("min: ", (Number) value,
	  NumberNode.NOT_ITERATOR, false, toolTipText);
      
      m_TreeModel.insertNodeInto(minNode, this, 0);
      
      Number one = null;
      try {
	one = minNode.getOneValue();
      } catch (NumberClassNotFoundException e) {
	e.printStackTrace();
      }
      
      NumberNode iteratorNode = new NumberNode("iterator: ", one,
	  NumberNode.PLUS_EQUAL, true, toolTipText);
      m_TreeModel.insertNodeInto(iteratorNode, this, 1);
      
      NumberNode maxNode = new NumberNode("max: ", (Number) value,
	  NumberNode.NOT_ITERATOR, true, toolTipText);
      m_TreeModel.insertNodeInto(maxNode, this, 2);
      
    } else if (m_PropertyEditor instanceof GenericObjectEditor) {
      
      GenericObjectNode classifierNode = new GenericObjectNode(
	  m_ParentPanel, value,
	  (GenericObjectEditor) m_PropertyEditor, toolTipText);
      
      m_TreeModel.insertNodeInto(classifierNode, this, 0);
      classifierNode.setTree(m_Tree);
      classifierNode.updateTree();
      
    } else if (m_PropertyEditor.getTags() != null) {
      
      String selected = m_PropertyEditor.getAsText();
      String tags[] = m_PropertyEditor.getTags();
      if (tags != null)
	for (int i = 0; i < tags.length; i++) {
	  
	  CheckBoxNode checkBoxNode = new CheckBoxNode(tags[i],
	      selected.equals(tags[i]), toolTipText);
	  m_TreeModel.insertNodeInto(checkBoxNode, this, i);
	  
	}
      
    } else {
      
      DefaultNode defaultNode = new DefaultNode(name, toolTipText, value,
	  m_PropertyEditor);
      
      m_TreeModel.insertNodeInto(defaultNode, this, 0);
      
    }
  }
  
  /** 
   * This method gets the range of values as specified by the 
   * child editor nodes.
   * 
   * @return	all values
   */
  public Vector getAllValues() {
    
    Vector values = new Vector();
    
    //OK, there are four type of nodes that can branch off of a propertyNode
    
    DefaultMutableTreeNode child = (DefaultMutableTreeNode) m_TreeModel.getChild(this, 0);
    
    if (child instanceof GenericObjectNode) {
      //Here we let the generic object class handles this for us
      values = ((GenericObjectNode) child).getValues();
      
    } else if (child instanceof DefaultNode) {
      //This is perhaps the easiest case.  GenericNodes are only responsible
      //for a single value 				
      values.add(((DefaultNode) child).getUserObject());
      
    } else if (child instanceof CheckBoxNode) {
      //Iterate through all of the children add their
      //value if they're selected
      
      int childCount = m_TreeModel.getChildCount(this);
      
      for (int i = 0; i < childCount; i++) {
	
	CheckBoxNode currentChild = (CheckBoxNode) m_TreeModel
	.getChild(this, i);
	
	if (currentChild.getSelected())
	  values.add(currentChild.getUserObject());
	
      }
      
    } else if (child instanceof NumberNode) {
      //here we need to handle some weird cases for inpout validation
      
      NumberNode minChild = (NumberNode) m_TreeModel.getChild(this, 0);
      NumberNode iteratorChild = (NumberNode) m_TreeModel.getChild(this, 1);
      NumberNode maxChild = (NumberNode) m_TreeModel.getChild(this, 2);
      
      boolean ignoreIterator = false;
      
      try {
	
	if (iteratorChild.getSelected()) {
	  
	  //first we check to see if the min value is greater than the max value
	  //if so then we gotta problem
	  
	  if (maxChild.lessThan(maxChild.getValue(), minChild.getValue())) {
	    
	    ignoreIterator = true;
	    throw new InvalidInputException(
		"Invalid numeric input for node " + getName()
		+ ": min > max. ");
	  }
	  
	  //Make sure that the iterator value will actually "iterate" between the
	  //min and max values
	  if ((iteratorChild.getIteratorType() == NumberNode.PLUS_EQUAL)
	      && (iteratorChild.lessThan(
		  iteratorChild.getValue(), iteratorChild
		  .getZeroValue()) || (iteratorChild
		      .equals(iteratorChild.getValue(),
			  iteratorChild.getZeroValue())))) {
	    
	    ignoreIterator = true;
	    throw new InvalidInputException(
		"Invalid numeric input for node " + getName()
		+ ": += iterator <= 0. ");
	    
	  } else if ((iteratorChild.getIteratorType() == NumberNode.TIMES_EQUAL)
	      && (iteratorChild.lessThan(
		  iteratorChild.getValue(), iteratorChild
		  .getOneValue()) || (iteratorChild
		      .equals(iteratorChild.getValue(),
			  iteratorChild.getOneValue())))) {
	    
	    ignoreIterator = true;
	    throw new InvalidInputException(
		"Invalid numeric input for node " + getName()
		+ ": *= iterator <= 1. ");
	    
	  }
	  
	}
	
      } catch (InvalidInputException e) {
	
	JRootPane parent = m_ParentPanel.getRootPane();
	
	JOptionPane.showMessageDialog(parent, "Invalid Input: "
	    + e.getMessage(), "Input error",
	    JOptionPane.ERROR_MESSAGE);
	
	e.printStackTrace();
      } catch (NumberClassNotFoundException e) {
	e.printStackTrace();
      }
      
      if (!iteratorChild.getSelected() || ignoreIterator) {
	//easiest case - if we don't care about the Iterator then we just throw
	//in the min value along with the max value(if its selected)  
	values.add(minChild.getUserObject());
	
	if (maxChild.getSelected()
	    && (!maxChild.getValue().equals(minChild.getValue())))
	  values.add(maxChild.getUserObject());
	
      } else {
	//here we need to cycle through all of the values from min to max in
	//increments specified by the inrement value.
	
	Number current = minChild.getValue();
	
	try {
	  
	  values.add(minChild.getValue());
	  
	  do {
	    
	    Number newNumber = null;
	    
	    if (iteratorChild.getIteratorType() == NumberNode.PLUS_EQUAL) {
	      newNumber = iteratorChild.addNumbers(iteratorChild.getValue(), current);
	    } else if (iteratorChild.getIteratorType() == NumberNode.TIMES_EQUAL) {
	      newNumber = iteratorChild.multiplyNumbers(
		  iteratorChild.getValue(), current);
	    }
	    
	    current = newNumber;
	    
	    if (iteratorChild
		.lessThan(current, maxChild.getValue())
		&& (!iteratorChild.equals(current, maxChild.getValue()))) {
	      values.add(newNumber);
	    }
	    
	  } while (iteratorChild.lessThan(current, maxChild.getValue())
	      && (!iteratorChild.equals(current, maxChild.getValue())));
	  
	  if (maxChild.getSelected()
	      && (!maxChild.getValue().equals(minChild.getValue())))
	    values.add(maxChild.getUserObject());
	  
	} catch (Exception e) {
	  e.printStackTrace();
	}
	
      }
    }
    
    return values;
  }
  
  /**
   * This method informs a child number node whether or not it is 
   * allowed to be selected. NumberNodes are the only ones that need
   * to ask permission first.  This simply makes sure that iterator
   * nodes can't be selected when the max node is not selected.
   * 
   * @param node	the node to check
   * @return		true of the node can be selected
   */
  public boolean canSelect(NumberNode node) {
    
    boolean permission = true;
    
    NumberNode iteratorChild = (NumberNode) m_TreeModel.getChild(this, 1);
    NumberNode maxChild = (NumberNode) m_TreeModel.getChild(this, 2);
    
    //the one case where we want to say no: you can not have an iterator
    //without a maximum value
    if (node == iteratorChild && (maxChild.getSelected() == false))
      permission = false;
    
    return permission;
  }
  
  /**
   * informs a requesting child node whether or not it has permission
   * to be deselected. Note that only NumberNodes and CheckBoxNodes
   * are the only one's that have any notion of being deselected and
   * therefore should be the only one's calling this method.
   * 
   * @param node	the node to check
   * @return		true if it can be de-selected
   */
  public boolean canDeselect(DefaultMutableTreeNode node) {
    
    boolean permission = true;
    
    if (node instanceof NumberNode) {
      
      NumberNode iteratorChild = (NumberNode) m_TreeModel.getChild(this,
	  1);
      NumberNode maxChild = (NumberNode) m_TreeModel.getChild(this, 2);
      //the one case where we want to say no for number nodes: you can 
      //not have an iterator without a maximum value
      if (node == maxChild && (iteratorChild.getSelected() == true))
	permission = false;
      
    } else if (node instanceof CheckBoxNode) {
      
      //For check box nodes, we only want to say no if there's only one
      //box currently selected - because at least one box needs to be
      //checked.
      int totalSelected = 0;
      int childCount = m_TreeModel.getChildCount(this);
      
      for (int i = 0; i < childCount; i++) {
	
	CheckBoxNode currentChild = (CheckBoxNode) m_TreeModel
	.getChild(this, i);
	
	if (currentChild.getSelected())
	  totalSelected++;
	
      }
      
      if (totalSelected == 1)
	permission = false;
      
    }
    
    return permission;
  }
}