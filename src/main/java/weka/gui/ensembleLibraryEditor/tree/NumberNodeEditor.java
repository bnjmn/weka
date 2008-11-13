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
 *    NumberNodeEditor.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor.tree;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * This class is responsible for creating the number editor GUI to allow users
 * to specify ranges of numerical values.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.1 $
 */
public class NumberNodeEditor
  extends JPanel
  implements ActionListener, ItemListener, PropertyChangeListener {
  
  /** for serialization */
  private static final long serialVersionUID = 3486848815982334460L;

  /**
   * the button that toggles the type of iterator that this node represents
   */
  private JButton m_IteratorButton;
  
  /** the textField that allows editing of the number value */
  private JFormattedTextField m_NumberField;
  
  /** the checkBox that allows thi snode to be selected/unselected */
  private JCheckBox m_SelectedCheckBox;
  
  /** the label that prints the name of this node */
  private JLabel m_Label;
  
  /** the colors to use */
  private Color textForeground, textBackground;
  
  /** a reference to the node this editor is rendering */
  private NumberNode m_Node;
  
  /**
   * The constructor builds a user interface based on the information queried
   * from the node passed in. Specifically the iteratorType, whether the node
   * is checkable, whether it is currently selected, it's current value, and
   * the name to print on the label.
   * 
   * @param node	the node the editor is for
   */
  public NumberNodeEditor(NumberNode node) {
    
    this.m_Node = node;
    String name = node.getText();
    Number value = node.getValue();
    int iteratorType = node.getIteratorType();
    boolean showCheckBox = node.getCheckable();
    boolean selected = node.getSelected();
    
    textForeground = UIManager.getColor("Tree.textForeground");
    textBackground = UIManager.getColor("Tree.textBackground");
    
    setForeground(textForeground);
    setBackground(textBackground);
    
    Font fontValue;
    fontValue = UIManager.getFont("Tree.font");
    
    if (showCheckBox) {
      m_SelectedCheckBox = new JCheckBox();
      m_SelectedCheckBox.setSelected(selected);
      m_SelectedCheckBox.setForeground(textForeground);
      m_SelectedCheckBox.setBackground(textBackground);
      m_SelectedCheckBox.addItemListener(this);
      add(m_SelectedCheckBox);
    }
    
    m_Label = new JLabel(name);
    if (fontValue != null) {
      m_Label.setFont(fontValue);
    }
    m_Label.setForeground(textForeground);
    m_Label.setBackground(textBackground);
    add(m_Label);
    
    if (iteratorType != NumberNode.NOT_ITERATOR) {
      updateIteratorButton();
      m_IteratorButton.setForeground(textForeground);
      m_IteratorButton.setBackground(textBackground);
      add(m_IteratorButton);
    }
    
    NumberFormat numberFormat = null;
    try {
      numberFormat = node.getNumberFormat();
    } catch (NumberClassNotFoundException e) {
      e.printStackTrace();
    }
    
    m_NumberField = new JFormattedTextField(numberFormat);
    m_NumberField.setValue(value);
    m_NumberField.setColumns(10);
    m_NumberField.setForeground(textForeground);
    m_NumberField.setBackground(textBackground);
    
    m_NumberField.addPropertyChangeListener(this);
    
    add(m_NumberField);
    
    if (!selected && showCheckBox) {
      m_Label.setEnabled(false);
      if (m_IteratorButton != null)
	m_IteratorButton.setEnabled(false);
      m_NumberField.setEnabled(false);
    }
  }
  
  /**
   * This method provides a way for the ModelTreeNodeEditor to register an
   * actionListener with this editor. This way, after the user has done
   * something, the tree can update its editing state as well as the tree node
   * structure as necessary
   * 
   * @param actionListener	the listener to use
   */
  public void setActionListener(ActionListener actionListener) {
    if (m_IteratorButton != null)
      m_IteratorButton.addActionListener(actionListener);
  }
  
  /**
   * This method provides a way for the ModelTreeNodeEditor to register an
   * itemListener with this editor. This way, after the user has done
   * something, the tree can update its editing state as well as the tree node
   * structure as necessary
   * 
   * @param itemListener	the listener to use
   */
  public void setItemListener(ItemListener itemListener) {
    if (m_SelectedCheckBox != null)
      m_SelectedCheckBox.addItemListener(itemListener);
  }
  
  /**
   * This method provides a way for the ModelTreeNodeEditor to register a
   * PropertyListener with this editor. This way, after the user has done
   * something, the tree can update its editing state as well as the tree node
   * structure as necessary
   * 
   * @param propertyChangeListener	the listener to use
   */
  public void setPropertyChangeListener(
      PropertyChangeListener propertyChangeListener) {
    if (m_NumberField != null)
      m_NumberField.addPropertyChangeListener(propertyChangeListener);
  }
  
  /**
   * This is a helper function that repaints the iterator toggling button
   * after an event.
   * 
   */
  private void updateIteratorButton() {
    
    if (m_Node.getIteratorType() == NumberNode.PLUS_EQUAL) {
      m_IteratorButton = new JButton("+=");
      m_IteratorButton.addActionListener(this);
      
    } else if (m_Node.getIteratorType() == NumberNode.TIMES_EQUAL) {
      m_IteratorButton = new JButton("*=");
      m_IteratorButton.addActionListener(this);
      
    }
    
    m_IteratorButton.repaint();
    repaint();
  }
  
  /**
   * This is the actionListener that while handle events from the JButton that
   * specifies the type of iterator. It simply updates both the label string
   * for the button and the iteratorType of the NumberNode
   * 
   * @param e		the event
   */
  public void actionPerformed(ActionEvent e) {
    
    if (m_Node.getIteratorType() == NumberNode.PLUS_EQUAL) {
      m_Node.setIteratorType(NumberNode.TIMES_EQUAL);
      
      try {
	if ((m_Node.getValue()).equals(m_Node.getOneValue())) {
	  m_Node.setValue(m_Node.getTwoValue());
	  updateIteratorButton();
	}
	
      } catch (NumberClassNotFoundException e1) {
	e1.printStackTrace();
      }
      
    } else if (m_Node.getIteratorType() == NumberNode.TIMES_EQUAL) {
      m_Node.setIteratorType(NumberNode.PLUS_EQUAL);
      try {
	if ((m_Node.getValue()).equals(m_Node.getTwoValue())) {
	  m_Node.setValue(m_Node.getOneValue());
	  updateIteratorButton();
	}
	
      } catch (NumberClassNotFoundException e1) {
	e1.printStackTrace();
      }
    }
  }
  
  /**
   * This is the Listener that while handle events from the checkBox ,if this
   * node has one. Note that before we can select or deselect this node we
   * have to ask permission from this node's parent. (wow, what fitting
   * terminology). Anyway, the reason is that we cannot have an iterator node
   * selected if there is no max node selected - conversely we cannot have a
   * max node be deselected if the iterator node is selected.
   * 
   * @param e		the event
   */
  public void itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      
      PropertyNode parent = (PropertyNode) m_Node.getParent();
      
      if (parent.canSelect(m_Node)) {
	
	m_Node.setSelected(true);
	
	m_Label.setEnabled(true);
	if (m_IteratorButton != null)
	  m_IteratorButton.setEnabled(true);
	m_NumberField.setEnabled(true);
      } else {
	m_SelectedCheckBox.setSelected(false);
      }
      
    } else if (e.getStateChange() == ItemEvent.DESELECTED) {
      
      PropertyNode parent = (PropertyNode) m_Node.getParent();
      
      if (parent.canDeselect(m_Node)) {
	
	m_Node.setSelected(false);
	
	m_Label.setEnabled(false);
	if (m_IteratorButton != null)
	  m_IteratorButton.setEnabled(false);
	m_NumberField.setEnabled(false);
	
      } else {
	m_SelectedCheckBox.setSelected(true);
      }
    }
  }
  
  /**
   * This is the Listener that while handle events from the text box for this
   * node. It basically grabs the value from the text field and then casts it
   * to the correct type and sets the value of the node.
   * 
   * @param evt		the event
   */
  public void propertyChange(PropertyChangeEvent evt) {
    
    Object source = evt.getSource();
    
    if (source instanceof JFormattedTextField
	&& evt.getPropertyName().equals("value")
	&& (((JFormattedTextField) evt.getSource()).getValue() != null)) {
      
      Number newValue = null;
      
      Number value = m_Node.getValue();
      
      JFormattedTextField field = (JFormattedTextField) evt.getSource();
      
      Number fieldValue = (Number) field.getValue();
      
      if (value instanceof Double)
	newValue = new Double(NumberNode.roundDouble((fieldValue.doubleValue())));
      else if (value instanceof Integer)
	newValue = new Integer((fieldValue).intValue());
      else if (value instanceof Float)
	newValue = new Float(NumberNode.roundFloat((fieldValue.floatValue())));
      else if (value instanceof Long)
	newValue = new Long((fieldValue).longValue());
      else {
	try {
	  throw new NumberClassNotFoundException(value.getClass()
	      + " not currently supported.");
	} catch (NumberClassNotFoundException e) {
	  e.printStackTrace();
	}
      }
      
      field.setValue(newValue);
      
      m_Node.setValue(newValue);
    }
  }
}