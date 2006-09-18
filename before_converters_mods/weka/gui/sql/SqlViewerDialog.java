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
 * SqlViewerDialog.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.gui.sql;

import weka.gui.sql.event.ResultChangedEvent;
import weka.gui.sql.event.ResultChangedListener;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * A little dialog containing the SqlViewer.
 *
 *
 * @author      FracPete (fracpete at waikato dot ac dot nz)
 * @version     $Revision: 1.1 $
 */

public class SqlViewerDialog 
  extends JDialog 
  implements ResultChangedListener {
  
  /** the parent frame */
  protected JFrame m_Parent;
  
  /** the SQL panel */
  protected SqlViewer m_Viewer;

  /** the panel for the buttons */
  protected JPanel m_PanelButtons;

  /** the OK button */
  protected JButton m_ButtonOK = new JButton("OK");

  /** the Cancel button */
  protected JButton m_ButtonCancel = new JButton("Cancel");

  /** displays the current query */
  protected JLabel m_LabelQuery = new JLabel("");

  /** the return value */
  protected int m_ReturnValue = JOptionPane.CANCEL_OPTION;

  /** the connect string with which the query was run */
  protected String m_URL;

  /** the user that was used to connect to the DB */
  protected String m_User;

  /** the password that was used to connect to the DB */
  protected String m_Password;

  /** the currently selected query */
  protected String m_Query;
  
  /**
   * initializes the dialog
   */
  public SqlViewerDialog(JFrame parent) {
    super(parent, "SQL-Viewer", true);

    m_Parent   = parent;
    m_URL      = "";
    m_User     = "";
    m_Password = "";
    m_Query    = "";
    
    createDialog();
  }

  /**
   * builds the dialog and all its components
   */
  protected void createDialog() {
    JPanel                    panel;
    JPanel                    panel2;
    final SqlViewerDialog     dialog;
    
    dialog = this;
    setLayout(new BorderLayout());

    // sql panel
    m_Viewer = new SqlViewer(m_Parent);
    add(m_Viewer, BorderLayout.CENTER);
    
    panel2 = new JPanel(new BorderLayout());
    add(panel2, BorderLayout.SOUTH);
    
    // Buttons
    panel = new JPanel();
    panel.setLayout(new FlowLayout());
    panel2.add(panel, BorderLayout.EAST);
    m_ButtonOK.setMnemonic('O');
    panel.add(m_ButtonOK);
    m_ButtonOK.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent evt){
	  m_ReturnValue = JOptionPane.OK_OPTION;
          // remove listener, otherwise does the disposal of resultspanel
          // change the query again!
          m_Viewer.removeResultChangedListener(dialog);
	  dialog.dispose();
      }
    });
    m_ButtonCancel.setMnemonic('C');
    panel.add(m_ButtonCancel);
    m_ButtonCancel.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent evt){
	  m_ReturnValue = JOptionPane.CANCEL_OPTION;
          // remove listener, otherwise does the disposal of resultspanel
          // change the query again!
          m_Viewer.removeResultChangedListener(dialog);
	  dialog.dispose();
      }
    });
   
    // current Query
    panel = new JPanel(new FlowLayout());
    panel2.add(panel, BorderLayout.CENTER);
    panel.add(m_LabelQuery);
    
    pack();
    getRootPane().setDefaultButton(m_ButtonOK);
    setResizable(true);

    // listener
    m_Viewer.addResultChangedListener(this);
  }

  /**
   * displays the dialog if TRUE
   */
  public void setVisible(boolean b) {
    if (b)
      m_ReturnValue = JOptionPane.CANCEL_OPTION;

    super.setVisible(b);
    
    // free up memory
    if (b)
      m_Viewer.clear();
  }

  /**
   * returns whether the user clicked OK (JOptionPane.OK_OPTION) or whether he
   * cancelled the dialog (JOptionPane.CANCEL_OPTION)
   * @return      the return value
   * @see         JOptionPane
   */
  public int getReturnValue() {
    return m_ReturnValue;
  }

  /**
   * returns the chosen URL, if any
   */
  public String getURL() {
    return m_URL;
  }

  /**
   * returns the chosen user, if any
   */
  public String getUser() {
    return m_User;
  }

  /**
   * returns the chosen password, if any
   */
  public String getPassword() {
    return m_Password;
  }

  /**
   * returns the chosen query, if any
   */
  public String getQuery() {
    return m_Query;
  }

  /**
   * This method gets called when a query has been executed.
   */
  public void resultChanged(ResultChangedEvent evt) {
    m_URL      = evt.getURL();
    m_User     = evt.getUser();
    m_Password = evt.getPassword();
    m_Query    = evt.getQuery();
    m_LabelQuery.setText("Current query: " + m_Query);
  }

  /**
   * for testing only
   */
  public static void main(String[] args) {
    SqlViewerDialog       dialog;

    dialog = new SqlViewerDialog(null);
    dialog.setDefaultCloseOperation(SqlViewerDialog.DISPOSE_ON_CLOSE);
    dialog.setVisible(true);
    System.out.println("ReturnValue = " + dialog.getReturnValue());
    if (dialog.getReturnValue() == JOptionPane.OK_OPTION) {
      System.out.println("URL      = " + dialog.getURL());
      System.out.println("User     = " + dialog.getUser());
      System.out.println("Password = " + dialog.getPassword().replaceAll(".", "*"));
      System.out.println("Query    = " + dialog.getQuery());
    }
  }
}

