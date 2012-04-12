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
 *    ModelTreeNodeRenderer.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor.tree;

import weka.gui.EnsembleLibraryEditor;

import java.awt.Component;
import java.beans.PropertyEditor;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

/**
 * This class renders a tree nodes. It determines which type of node the m_Tree
 * is trying to render and then returns the approapriate gui widget. It is
 * basically the same thing as the ModelTreeNodeEditor class except it does not
 * have to.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.1 $
 */
public class ModelTreeNodeRenderer
  implements TreeCellRenderer {
  
  /**
   * empty Constructor
   */
  public ModelTreeNodeRenderer() {
    super();
  }
  
  /**
   * This is the method of this class that is responsible for figuring out how
   * to display each of the tree nodes. All it does is figure out the type of
   * the node and then return a new instance of the apropriate editor type for
   * the node.
   * 
   * @param tree	the associated tree
   * @param value	the value
   * @param selected	true if item is selected
   * @param expanded	true if node is expanded
   * @param leaf	true if node is leaf
   * @param row		the row in the tree
   * @param hasFocus	true if item has the focus
   * @return		the rendering component
   */
  public Component getTreeCellRendererComponent(JTree tree, Object value,
      boolean selected, boolean expanded, boolean leaf, int row,
      boolean hasFocus) {
    
    JComponent customRenderer = null;
    
    if (value instanceof GenericObjectNode) {
      
      GenericObjectNode node = (GenericObjectNode) value;
      customRenderer = new GenericObjectNodeEditor(node);
      customRenderer.setToolTipText(node.getToolTipText());
      
    } else if (value instanceof PropertyNode) {
      PropertyNode node = (PropertyNode) value;
      JLabel label = new JLabel(node.getName());
      label.setToolTipText(node.getToolTipText());
      customRenderer = label;
      customRenderer.setToolTipText(node.getToolTipText());
      
    } else if (value instanceof CheckBoxNode) {
      
      CheckBoxNode node = (CheckBoxNode) value;
      customRenderer = new CheckBoxNodeEditor(node);
      customRenderer.setToolTipText(node.getToolTipText());
      
    } else if (value instanceof NumberNode) {
      
      NumberNode node = (NumberNode) value;
      customRenderer = new NumberNodeEditor(node);
      customRenderer.setToolTipText(node.getToolTipText());
      
    } else if (value instanceof DefaultNode) {
      
      DefaultNode node = (DefaultNode) value;
      PropertyEditor nodeEditor = node.getEditor();
      customRenderer = (JComponent) EnsembleLibraryEditor
      .getDefaultRenderer(nodeEditor);
      customRenderer.setToolTipText(node.getToolTipText());
      
    }
    
    return customRenderer;
  }
}