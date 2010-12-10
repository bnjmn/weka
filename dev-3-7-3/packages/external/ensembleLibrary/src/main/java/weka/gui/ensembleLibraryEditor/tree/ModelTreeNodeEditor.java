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
 *    ModelTreeNodeEditor.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor.tree;

import weka.gui.EnsembleLibraryEditor;
import weka.gui.GenericObjectEditor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;

/**
 * This class is in charge of dynamically creating editor GUI objects on
 * demand for the main JTree class that will display our Classifier tree
 * model of parameters.  This is in fact the CellEditor class that is
 * registered with our tree.
 * <p/>
 * Basically it delegates much of the work to the various NodeEditor 
 * classes found in this package. All it really has to do is detect 
 * what of node it is and then instantiate an editor of the appropriate
 * type.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.1 $
 */
public class ModelTreeNodeEditor
  extends AbstractCellEditor
  implements TreeCellEditor, ItemListener, PropertyChangeListener, ActionListener {
  
  /** for serialization */
  private static final long serialVersionUID = 7057924814405386358L;
  
  /**
   * This is the underlying tree holding all our parameter nodes in the GUI.
   */
  private JTree m_Tree;
  
  /**
   * default Constructor
   * 
   * @param tree	the tree to use
   */
  public ModelTreeNodeEditor(JTree tree) {
    m_Tree = tree;
  }
  
  /**
   * I'm supposed to implemnent this as part of the TreeCellEDitor
   * interface. however, it's not necessary for this class so it
   * returns null.
   * 
   * @return		always null
   */
  public Object getCellEditorValue() {
    return null;
  }
  
  /**
   * This tells the JTree whether or not to let nodes in the tree be
   * edited.  Basically all this does is return true for all nodes that
   * aren't PropertyNodes - which don't have any interactive widgets
   * 
   * @param event	the event
   * @return		true if editable
   */
  public boolean isCellEditable(EventObject event) {
    boolean returnValue = false;
    if (event instanceof MouseEvent) {
      MouseEvent mouseEvent = (MouseEvent) event;
      TreePath path = m_Tree.getPathForLocation(mouseEvent.getX(),
	  mouseEvent.getY());
      if (path != null) {
	Object node = path.getLastPathComponent();
	if ((node != null) && (node instanceof DefaultMutableTreeNode)) {
	  DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
	  returnValue = (!(treeNode instanceof PropertyNode));
	}
      }
    }
    return returnValue;
  }
  
  /**
   * This method uses the ModelTreeNodeRenderer class to get the individual
   * editors and then registers this classes editing event listeners with 
   * them
   * 
   * @param tree	the associated tree
   * @param value	the value
   * @param selected	true if item is selected
   * @param expanded	true if it is expanded
   * @param leaf	true if it is a leaf
   * @param row		the row in the tree
   * @return		the rendering component
   */
  public Component getTreeCellEditorComponent(JTree tree, Object value,
      boolean selected, boolean expanded, boolean leaf, int row) {
    
    //make the renderer to all the "painting" work, then add all of the
    //necessary action listeners to the objects.
    //Component customEditor = m_Renderer.getTreeCellRendererComponent(tree,
    //	value,selected,expanded,leaf,row,false);
    
    JComponent customRenderer = null;
    
    if (value instanceof GenericObjectNode) {
      GenericObjectNode node = (GenericObjectNode) value;
      customRenderer = new GenericObjectNodeEditor(node);
      customRenderer.setToolTipText(node.getToolTipText());
      ((GenericObjectNodeEditor) customRenderer)
      .setPropertyChangeListener(this);
      
    } else if (value instanceof PropertyNode) {
      PropertyNode node = (PropertyNode) value;
      JLabel label = new JLabel(node.getName());
      label.setToolTipText(node.getToolTipText());
      customRenderer = label;
      customRenderer.setToolTipText(node.getToolTipText());
      //do nothing, these nodes aren't editable
      
    } else if (value instanceof CheckBoxNode) {
      CheckBoxNode node = (CheckBoxNode) value;
      customRenderer = new CheckBoxNodeEditor(node);
      customRenderer.setToolTipText(node.getToolTipText());
      ((CheckBoxNodeEditor) customRenderer).setItemListener(this);
      
    } else if (value instanceof NumberNode) {
      NumberNode node = (NumberNode) value;
      customRenderer = new NumberNodeEditor(node);
      customRenderer.setToolTipText(node.getToolTipText());
      ((NumberNodeEditor) customRenderer).setPropertyChangeListener(this);
      ((NumberNodeEditor) customRenderer).setItemListener(this);
      ((NumberNodeEditor) customRenderer).setActionListener(this);
      
    } else if (value instanceof DefaultNode) {
      
      DefaultNode node = (DefaultNode) value;
      PropertyEditor nodeEditor = node.getEditor();
      customRenderer = (JComponent) EnsembleLibraryEditor
      .getDefaultRenderer(nodeEditor);
      ((JComponent) customRenderer).setToolTipText(node.getToolTipText());
      nodeEditor.addPropertyChangeListener(this);
      
    }
    
    return customRenderer;
  }
  
  /**
   * The item Listener that gets registered with all node editors that
   * have a widget that had itemStateChangeg events.  It just fires the
   * editing stopped event.
   * 
   * @param e		the event
   */
  public void itemStateChanged(ItemEvent e) {
    if (stopCellEditing()) {
      fireEditingStopped();
    }
  }
  
  /**
   * The prtopertyListener that gets registered with all node editors that
   * have a widget that had propertyStateChangeg events.  It just fires the
   * editing stopped event.
   * 
   * @param evt		the event
   */
  public void propertyChange(PropertyChangeEvent evt) {
    
    if (evt.getSource() instanceof GenericObjectEditor
	|| evt.getSource() instanceof GenericObjectNodeEditor) {
      
      if (stopCellEditing()) {
	fireEditingStopped();
      }
      
    } else if (evt.getPropertyName() != null) {
      if (evt.getPropertyName().equals("value")) {
	
	if (stopCellEditing()) {
	  fireEditingStopped();
	}
      }
    }
  }
  
  /**
   * The item Listener that gets registered with all node editors that
   * have a widget that had actionPerformed events.  It just fires the
   * editing stopped event.
   * 
   * @param e		the event
   */
  public void actionPerformed(ActionEvent e) {
    if (stopCellEditing()) {
      fireEditingStopped();
    }
  }
}