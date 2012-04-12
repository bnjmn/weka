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
 *    ListSelectorDialog.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JList;

/** 
 * A dialog to present the user with a list of items, that the user can
 * make a selection from, or cancel the selection.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4.4.1 $
 */
public class ListSelectorDialog extends JDialog {
  
  /** Click to choose the currently selected property */
  protected JButton m_SelectBut = new JButton("Select");

  /** Click to cancel the property selection */
  protected JButton m_CancelBut = new JButton("Cancel");

  /** The list component */
  protected JList m_List;
  
  /** Whether the selection was made or cancelled */
  protected int m_Result;

  /** Signifies an OK property selection */
  public static final int APPROVE_OPTION = 0;

  /** Signifies a cancelled property selection */
  public static final int CANCEL_OPTION = 1;
  
  /**
   * Create the list selection dialog.
   *
   * @param parentFrame the parent frame of the dialog
   * @param userList the JList component the user will select from
   */
  public ListSelectorDialog(Frame parentFrame, JList userList) {
    
    super(parentFrame, "Select items", true);
    m_List = userList;
    m_CancelBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	m_Result = CANCEL_OPTION;
	setVisible(false);
      }
    });
    m_SelectBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	m_Result = APPROVE_OPTION;
	setVisible(false);
      }
    });
    
    Container c = getContentPane();
    c.setLayout(new BorderLayout());
    //    setBorder(BorderFactory.createTitledBorder("Select a property"));
    Box b1 = new Box(BoxLayout.X_AXIS);
    b1.add(m_SelectBut);
    b1.add(Box.createHorizontalStrut(10));
    b1.add(m_CancelBut);
    c.add(b1, BorderLayout.SOUTH);
    c.add(new JScrollPane(m_List), BorderLayout.CENTER);
    pack();

    // make sure, it's not bigger than the screen
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int width  = getWidth() > screen.getWidth() 
                    ? (int) screen.getWidth() : getWidth();
    int height = getHeight() > screen.getHeight() 
                    ? (int) screen.getHeight() : getHeight();
    setSize(width, height);
  }

  /**
   * Pops up the modal dialog and waits for cancel or a selection.
   *
   * @return either APPROVE_OPTION, or CANCEL_OPTION
   */
  public int showDialog() {

    m_Result = CANCEL_OPTION;
    int [] origSelected = m_List.getSelectedIndices();
    setVisible(true);
    if (m_Result == CANCEL_OPTION) {
      m_List.setSelectedIndices(origSelected);
    }
    return m_Result;
  }
  
  /**
   * Tests out the list selector from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      DefaultListModel lm = new DefaultListModel();      
      lm.addElement("one");
      lm.addElement("two");
      lm.addElement("three");
      lm.addElement("four");
      lm.addElement("five");
      JList jl = new JList(lm);
      final ListSelectorDialog jd = new ListSelectorDialog(null, jl);
      int result = jd.showDialog();
      if (result == ListSelectorDialog.APPROVE_OPTION) {
	System.err.println("Fields Selected");
	int [] selected = jl.getSelectedIndices();
	for (int i = 0; i < selected.length; i++) {
	  System.err.println("" + selected[i]
			     + " " + lm.elementAt(selected[i]));
	}
      } else {
	System.err.println("Cancelled");
      }
      System.exit(0);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
