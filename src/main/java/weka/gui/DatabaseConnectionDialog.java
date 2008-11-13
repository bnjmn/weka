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
 *    DatabaseConnectionDialog.java
 *    Copyright (C) 2004 Dale Fletcher
 *
 */

package weka.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.Font;

/** 
 * A dialog to enter URL, username and password for a database connection.
 *
 * @author Dale Fletcher (dale@cs.waikato.ac.nz)
 * @version $Revision: 1.2.2.2 $
 */

public class DatabaseConnectionDialog extends JDialog {

  /* URL field and label */
  protected JTextField m_DbaseURLText;
  protected JLabel m_DbaseURLLab;

  /* Username field and label */
  protected JTextField m_UserNameText; 
  protected JLabel m_UserNameLab;

  /* Password field and label */
  protected JPasswordField m_PasswordText; 
  protected JLabel m_PasswordLab;
  
  /* whether dialog was cancel'ed or OK'ed */
  protected int m_returnValue;

  /**
   * Create database connection dialog.
   *
   * @param parentFrame the parent frame of the dialog
   */
  public DatabaseConnectionDialog(Frame parentFrame){
    super(parentFrame,"Database Connection Parameters",true);
    DbConnectionDialog("","");
  }
  /**
   * Create database connection dialog.
   *
   * @param parentFrame the parent frame of the dialog
   * @param url initial text for URL field
   * @param uname initial text for username field
   */
  public DatabaseConnectionDialog(Frame parentFrame,String url, String uname) {
    super(parentFrame,"Database Connection Parameters",true);
    DbConnectionDialog(url, uname);
  }

  /**
   * Returns URL from dialog 
   *
   * @return URL string
   */
  public String getURL(){
    return(m_DbaseURLText.getText());
  }

  /**
   * Returns Username from dialog 
   *
   * @return Username string
   */ 
  public String getUsername(){
    return(m_UserNameText.getText());
  }

  /**
   * Returns password from dialog 
   *
   * @return Password string
   */
  public String getPassword(){
    return(new String(m_PasswordText.getPassword()));
  }

  /**
   * Returns which of OK or cancel was clicked from dialog 
   *
   * @return either JOptionPane.OK_OPTION or JOptionPane.CLOSED_OPTION
   */
  public int getReturnValue(){
    return(m_returnValue);
  }
  
  /**
   * Display the database connection dialog
   *
   * @param url initial text for URL field
   * @param uname initial text for username field
   */
  public void DbConnectionDialog(String url, String uname) {

    JPanel DbP = new JPanel();
    //DbP.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

    DbP.setLayout(new GridLayout(4, 1));
    //DbP.setLayout(new FlowLayout(FlowLayout.LEFT));
    m_DbaseURLText = new JTextField(url,50); 
    m_DbaseURLLab = new JLabel(" Database URL:", SwingConstants.LEFT);
    m_DbaseURLLab.setFont(new Font("Monospaced", Font.PLAIN, 12));

    m_UserNameText = new JTextField(uname,25); 
    m_UserNameLab = new JLabel(" Username:    ", SwingConstants.LEFT);
    m_UserNameLab.setFont(new Font("Monospaced", Font.PLAIN, 12));

    m_PasswordText = new JPasswordField(25); 
    m_PasswordLab = new JLabel(" Password:    ", SwingConstants.LEFT);
    m_PasswordLab.setFont(new Font("Monospaced", Font.PLAIN, 12));

    JPanel urlP = new JPanel();   
    //urlP.setLayout(new BorderLayout());
    urlP.setLayout(new FlowLayout(FlowLayout.LEFT));
    urlP.add(m_DbaseURLLab);//, BorderLayout.WEST);
    urlP.add(m_DbaseURLText);//, BorderLayout.CENTER);
    DbP.add(urlP);

    JPanel usernameP = new JPanel();   
    //usernameP.setLayout(new BorderLayout());
    usernameP.setLayout(new FlowLayout(FlowLayout.LEFT));
    usernameP.add(m_UserNameLab);//, BorderLayout.WEST);
    usernameP.add(m_UserNameText);//, BorderLayout.CENTER);
    DbP.add(usernameP);

    JPanel passwordP = new JPanel();   
    //passwordP.setLayout(new BorderLayout());
    passwordP.setLayout(new FlowLayout(FlowLayout.LEFT));
    passwordP.add(m_PasswordLab);//, BorderLayout.WEST);
    passwordP.add(m_PasswordText);//, BorderLayout.CENTER);
    DbP.add(passwordP);

    JPanel buttonsP = new JPanel();
    buttonsP.setLayout(new FlowLayout());
    JButton ok,cancel;
    buttonsP.add(ok = new JButton("OK"));
    buttonsP.add(cancel=new JButton("Cancel"));
    ok.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent evt){
	  m_returnValue=JOptionPane.OK_OPTION;
	  DatabaseConnectionDialog.this.dispose();
      }
    });
    cancel.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent evt){
	  m_returnValue=JOptionPane.CLOSED_OPTION;
	  DatabaseConnectionDialog.this.dispose();
      }
    });

    // Listen for window close events
    addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          System.err.println("Cancelled!!");
          m_returnValue = JOptionPane.CLOSED_OPTION;
        }
      });
   
    DbP.add(buttonsP);
    this.getContentPane().add(DbP,BorderLayout.CENTER);
    this.pack();
    setResizable(false);
  }
  public static void main(String[] args){
 
    DatabaseConnectionDialog dbd= new DatabaseConnectionDialog(null,"URL","username");
    dbd.setVisible(true);
    System.out.println(dbd.getReturnValue()+":"+dbd.getUsername()+":"+dbd.getPassword()+":"+dbd.getURL());
  }

}




