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
 *    CheckBoxNodeEditor.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor.tree;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/** 
 * This class is responsible for creating a simple checkBox editor for the 
 * CheckBoxNode class.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision$
 */
public class CheckBoxNodeEditor
  extends JPanel
  implements ItemListener {
  
  /** for serialization */
  private static final long serialVersionUID = -1506685976284982111L;

  /** the checkbox the user will interact with */
  private JCheckBox m_SelectedCheckBox;
  
  /** the label that will display the name of this parameter value */
  private JLabel m_Label;
  
  /** colors we'll use  */
  private Color textForeground, textBackground;
  
  /** a reference to the node this editor represents */
  private CheckBoxNode m_Node;
  
  /**
   * The constructor builds the simple CheckBox GUI based on the information 
   * in the node that is passed to it.  Specifically, the GUI consists of only
   * a JLabel showing the name and a CheckBox allowing the user to toggle it. 
   * 
   * @param node a reference to the node this editor represents
   */
  public CheckBoxNodeEditor(CheckBoxNode node) {
    
    this.m_Node = node;
    
    String name = node.getName();
    boolean selected = node.getSelected();
    
    textForeground = UIManager.getColor("Tree.textForeground");
    textBackground = UIManager.getColor("Tree.textBackground");
    
    setForeground(textForeground);
    setBackground(textBackground);
    
    Font fontValue;
    fontValue = UIManager.getFont("Tree.font");
    
    m_SelectedCheckBox = new JCheckBox();
    m_SelectedCheckBox.setSelected(selected);
    m_SelectedCheckBox.setForeground(textForeground);
    m_SelectedCheckBox.setBackground(textBackground);
    m_SelectedCheckBox.addItemListener(this);
    add(m_SelectedCheckBox);
    
    m_Label = new JLabel(name);
    if (fontValue != null) {
      m_Label.setFont(fontValue);
    }
    m_Label.setForeground(textForeground);
    m_Label.setBackground(textBackground);
    add(m_Label);
  }
  
  /**
   * This method provides a way for the ModelTreeNodeEditor
   * to register an itemListener with this editor.  This way, 
   * after the user has done something, the tree can update its
   * editing state as well as the tree node structure as necessary 
   * 
   * @param itemListener	the listener to use
   */
  public void setItemListener(ItemListener itemListener) {
    if (m_SelectedCheckBox != null)
      m_SelectedCheckBox.addItemListener(itemListener);
  }
  
  /**
   * This is the implementation of the itemListener for the CheckBoxNode.
   * Note that when we are deselcting a CheckBox we have to ask our parent
   * node if this is OK.  This is because we need to ensure that at least a
   * single CheckBox underneath the parent remains checked because we need at 
   * least one value for each classifier parameter.
   * 
   * @param e		the event
   */
  public void itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      m_Node.setSelected(true);
      m_Label.setEnabled(true);
      
    } else if (e.getStateChange() == ItemEvent.DESELECTED) {
      
      PropertyNode parent = (PropertyNode) m_Node.getParent();
      if (parent.canDeselect(m_Node)) {
	m_Node.setSelected(false);
	m_Label.setEnabled(false);
      } else {
	m_SelectedCheckBox.setSelected(true);
      }
    }
  }
}
