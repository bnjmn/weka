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
 *    GenericObjectNodeEditor.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor.tree;

import weka.gui.GenericObjectEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/** 
 * This class creates a simple user interface for selecting the Generic
 * Object Type. It consists of a button that brings up a popupMenu 
 * allowing the user to select the type of Generic Object to be edited.  
 * It also contains a JLabel that just prints out the type. Finally, we
 * also added the button from the PropertySheetPanel that brings up more
 * info about the Object
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.1 $
 */
public class GenericObjectNodeEditor
  extends JPanel
  implements PropertyChangeListener, ActionListener, PopupMenuListener {
  
  /** for serialization */
  private static final long serialVersionUID = -2382339640932830323L;

  /**  a reference to the GenericObjectEditor for this node*/
  private GenericObjectEditor m_GenericObjectEditor;
  
  /** the label that will display the node object type */
  private JLabel m_Label;
  
  /** the button that will create the JPopupMenu to choose the object type */
  private JButton m_ChooseClassButton;
  
  /** Button to pop up the full help text in a separate frame */
  private JButton m_MoreInfoButton;
  
  /** Help frame */
  private JFrame m_HelpFrame;
  
  /** a propertyChangeSupportListener to inform the TreeNodeEditor
   * when we stop editing - needs to be done manually for that popup*/
  private PropertyChangeSupport m_propertyChangeSupport = new PropertyChangeSupport(
      this);
  
  /** the colors */
  private Color textForeground, textBackground;
  
  /** A reference to the node */
  private GenericObjectNode m_Node;
  
  /** the popup menu to show the node parameter GUI */
  private JPopupMenu m_popup;
  
  /**
   * The constructor builds initializes the various member values from the
   * node
   * 
   * @param node	the node to base the node editor on
   */
  public GenericObjectNodeEditor(GenericObjectNode node) {
    m_GenericObjectEditor = node.getEditor();
    m_GenericObjectEditor.setValue(node.getObject());
    m_GenericObjectEditor.addPropertyChangeListener(node);
    m_GenericObjectEditor.addPropertyChangeListener(this);
    
    m_Node = node;
    updateEditor();
  }
  
  /**
   * This method updates the editor by creating the two buttons and the
   * JPanel that constiture the generic object editor GUI.
   */
  public void updateEditor() {
    
    textForeground = UIManager.getColor("Tree.textForeground");
    textBackground = UIManager.getColor("Tree.textBackground");
    
    setForeground(textForeground);
    setBackground(textBackground);
    
    m_ChooseClassButton = new JButton("Choose");
    m_ChooseClassButton.addActionListener(this);
    m_ChooseClassButton.setForeground(textForeground);
    m_ChooseClassButton.setBackground(textBackground);
    
    Font fontValue;
    fontValue = UIManager.getFont("Tree.font");
    
    String labelString = null;
    
    //This bit just prunes of the weka.classifier part of the lass name
    //for feng shui purposes
    
    boolean replaceName = false;
    String className = m_Node.getObject().getClass().toString();
    if (className.length() > 23) {
      if (className.substring(0, 23).equals("class weka.classifiers.")) {
	replaceName = true;
      }
    }
    
    if (replaceName) {
      labelString = new String(m_Node.getObject().getClass().toString()
	  .replaceAll("class weka.classifiers.", ""));
    } else {
      labelString = new String(m_Node.getObject().getClass().toString());
    }
    
    m_Label = new JLabel(labelString);
    
    if (fontValue != null) {
      m_Label.setFont(fontValue);
    }
    m_Label.setForeground(textForeground);
    m_Label.setBackground(textBackground);
    
    m_MoreInfoButton = new JButton("More Info");
    
    // anonymous action listener shows a JTree popup and allows the user
    // to choose the class they want
    m_MoreInfoButton.addActionListener(this);
    m_MoreInfoButton.setForeground(textForeground);
    m_MoreInfoButton.setBackground(textBackground);
    
    add(m_ChooseClassButton);
    add(m_Label);
    add(m_MoreInfoButton);
  }
  
  /**
   * Sets the prop change listener
   * 
   * @param al		the listener
   */
  public void setPropertyChangeListener(PropertyChangeListener al) {
    if (m_GenericObjectEditor != null)
      m_GenericObjectEditor.addPropertyChangeListener(al);
    m_propertyChangeSupport.addPropertyChangeListener(al);
  }
  
  /**
   * This method implements the property change listener for this node. 
   * When this happens, we just need to update the node value
   * 
   * @param evt		the event
   */
  public void propertyChange(PropertyChangeEvent evt) {
    updateEditor();
  }
  
  /**
   * This implements the action listener for the buttons in this editor,  If
   * they hit the choose class button then the popup menu for choosing the 
   * generic object ype is created.  Otherwise if its the more info button 
   * then the information frame is popped up
   * 
   * @param e		the event
   */
  public void actionPerformed(ActionEvent e) {
    
    if (e.getSource() == m_ChooseClassButton) {
      
      m_popup = m_GenericObjectEditor.getChooseClassPopupMenu();
      
      m_popup.addPopupMenuListener(this);
      m_popup.show(m_ChooseClassButton, m_ChooseClassButton.getX(),
	  m_ChooseClassButton.getY());
      
      m_popup.pack();
      
    } else if (e.getSource() == m_MoreInfoButton) {
      
      openHelpFrame();
      
    }
  }
  
  /**
   * This method was copied and modified from the ProeprtySheetPanel
   * class.
   */
  protected void openHelpFrame() {
    
    JTextArea ta = new JTextArea();
    ta.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    //ta.setBackground(getBackground());
    ta.setEditable(false);
    ta.setText(m_Node.getHelpText().toString());
    ta.setCaretPosition(0);
    final JFrame jf = new JFrame("Information");
    jf.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	jf.dispose();
	if (m_HelpFrame == jf) {
	  m_MoreInfoButton.setEnabled(true);
	}
      }
    });
    jf.getContentPane().setLayout(new BorderLayout());
    jf.getContentPane().add(new JScrollPane(ta), BorderLayout.CENTER);
    jf.pack();
    jf.setSize(400, 350);
    jf.setLocation(this.getTopLevelAncestor().getLocationOnScreen().x
	+ this.getTopLevelAncestor().getSize().width, this
	.getTopLevelAncestor().getLocationOnScreen().y);
    jf.setVisible(true);
    m_HelpFrame = jf;
  }
  
  /**
   * in the case that the popup menu is cancelled we need to manually 
   * inform the ModelTreeNodeEditor that we are done editing. If we
   * don't then we get stuck in a strange limbo state where the node
   * represented by this editor is completely frozen and the user 
   * can't interact with it.
   * 
   * @param e		the event
   */
  public void popupMenuCanceled(PopupMenuEvent e) {
    m_propertyChangeSupport.firePropertyChange(null, null, null);
  }
  
  /**
   * Necessary for popup listener interface.  Does nothing.
   * 
   * @param e		the event
   */
  public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    //do nothing
  }
  
  /**
   * Necessary for popup listener interface.  Does nothing.
   * 
   * @param e		the event
   */
  public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    //do nothing
  }
}