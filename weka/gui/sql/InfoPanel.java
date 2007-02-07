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
 * InfoPanel.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.sql;

import weka.gui.ComponentHelper;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * A simple panel for displaying information, e.g. progress information etc.
 *
 * @author      FracPete (fracpete at waikato dot ac dot nz)
 * @version     $Revision: 1.2 $
 */

public class InfoPanel
  extends JPanel {

  /** for serialization */
  private static final long serialVersionUID = -7701133696481997973L;
  
  /** the parent of this panel */
  protected JFrame m_Parent;
  
  /** the list the contains the messages */
  protected JList m_Info;

  /** the model for the list */
  protected DefaultListModel m_Model;

  /** the button to clear the area */
  protected JButton m_ButtonClear;
  
  /**
   * creates the panel
   * @param parent      the parent of this panel
   */
  public InfoPanel(JFrame parent) {
    super();
    m_Parent = parent;
    createPanel();
  }

  /**
   * inserts the components into the panel
   */
  protected void createPanel() {
    JPanel          panel;
    
    setLayout(new BorderLayout());

    // text
    m_Model = new DefaultListModel();
    m_Info  = new JList(m_Model);
    m_Info.setCellRenderer(new InfoPanelCellRenderer());
    add(new JScrollPane(m_Info), BorderLayout.CENTER);

    // clear button
    panel = new JPanel(new BorderLayout());
    add(panel, BorderLayout.EAST);
    m_ButtonClear = new JButton("Clear");
    m_ButtonClear.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  clear();
	}
      });
    panel.add(m_ButtonClear, BorderLayout.NORTH);
  }

  /**
   * sets the focus in a designated control
   */
  public void setFocus() {
    m_Info.requestFocus();
  }

  /**
   * clears the content of the panel
   */
  public void clear() {
    m_Model.clear();
  }

  /**
   * adds the given message to the end of the list (with the associated icon
   * at the beginning)
   * @param msg       the message to append to the list
   * @param icon      the filename of the icon
   */
  public void append(String msg, String icon) {
    append(new JLabel(msg, ComponentHelper.getImageIcon(icon), JLabel.LEFT));
  }

  /**
   * adds the given message to the end of the list
   * @param msg       the message to append to the list
   */
  public void append(Object msg) {
    if (msg instanceof String) {
      append(msg.toString(), "empty_small.gif");
      return;
    }

    m_Model.addElement(msg);
    m_Info.setSelectedIndex(m_Model.getSize() - 1);
    m_Info.ensureIndexIsVisible(m_Info.getSelectedIndex());
  }
}

