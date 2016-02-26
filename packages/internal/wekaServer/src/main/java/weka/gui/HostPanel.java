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
 *    HostPanel.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import weka.core.Environment;
import weka.server.RootServlet;
import weka.server.WekaServer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Class providing a panel for configuring and testing a connection to a
 * server
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class HostPanel extends JPanel {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 7893017818866180239L;

  /** Text field for entering the host name */
  protected JTextField m_hostField = new JTextField(25);

  /** Text field for entering the port number */
  protected JTextField m_portField = new JTextField(8);

  /** Button for testing the configured connection details */
  protected JButton m_testBut = new JButton("Test connection");

  /** Label for displaying server status (and load level) */
  protected JLabel m_serverStatus = new JLabel();

  /** Username */
  protected String m_username = "";

  /** Password */
  protected String m_password = "";

  /**
   * Constructor
   *
   * @param username optional username for authentication
   * @param password optional password for authentication
   */
  public HostPanel(String username, String password) {
    m_username = username;
    m_password = password;
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createTitledBorder("Remote host"));

    JPanel temp = new JPanel();
    temp.setLayout(new BorderLayout());

    JPanel holder1 = new JPanel();
    holder1.setLayout(new BorderLayout());
    holder1
      .add(new JLabel("Host: ", SwingConstants.RIGHT), BorderLayout.WEST);
    holder1.add(m_hostField, BorderLayout.CENTER);

    JPanel holder2 = new JPanel();
    holder2.setLayout(new BorderLayout());
    holder2
      .add(new JLabel("Port: ", SwingConstants.RIGHT), BorderLayout.WEST);
    holder2.add(m_portField, BorderLayout.CENTER);

    JPanel holder3 = new JPanel();
    holder3.setLayout(new BorderLayout());
    holder3.add(holder1, BorderLayout.CENTER);
    holder3.add(holder2, BorderLayout.EAST);

    temp.add(holder3, BorderLayout.NORTH);

    m_hostField.setText("localhost");
    m_portField.setText("" + WekaServer.PORT);

    JPanel holder4 = new JPanel();
    holder4.setLayout(new BorderLayout());
    holder4.add(m_testBut, BorderLayout.WEST);

    JPanel holder5 = new JPanel();
    holder5.setLayout(new BorderLayout());
    holder5.setBorder(BorderFactory.createTitledBorder("Server status"));
    holder5.add(m_serverStatus, BorderLayout.CENTER);
    holder5.add(holder4, BorderLayout.WEST);

    temp.add(holder5, BorderLayout.SOUTH);

    add(temp, BorderLayout.NORTH);

    m_testBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        hostOK();
      }
    });
  }

  /**
   * Get the host name
   *
   * @return the host name
   */
  public String getHostName() {
    return m_hostField.getText();
  }

  /**
   * Get the port
   *
   * @return the port
   */
  public String getPort() {
    return m_portField.getText();
  }

  /**
   * Set the host name
   *
   * @param hostName the host name to use
   */
  public void setHostName(String hostName) {
    m_hostField.setText(hostName);
  }

  /**
   * Set the port
   *
   * @param port the port to use
   */
  public void setPort(String port) {
    m_portField.setText(port);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.awt.Component#toString()
   */
  @Override
  public String toString() {
    String hostPort = m_hostField.getText() + ":" + m_portField.getText();
    return hostPort;
  }

  /**
   * Test the connection details
   *
   * @return true if a connection can be made to the server
   */
  public boolean hostOK() {
    if (m_hostField.getText() != null && m_hostField.getText().length() > 0) {
      String host = m_hostField.getText();
      String port = "" + WekaServer.PORT;
      if (m_portField.getText() != null && m_portField.getText().length() > 0) {
        port = m_portField.getText();
      }

      try {
        host = Environment.getSystemWide().substitute(host);
      } catch (Exception ex) {
      }
      try {
        port = Environment.getSystemWide().substitute(port);
      } catch (Exception ex) {
      }

      String server = host + ":" + port;
      double serverLoad =
        RootServlet.getSlaveLoad(server, m_username, m_password);
      if (serverLoad < 0) {
        m_serverStatus.setText("Unable to connect to server.");
        return false;
      } else {
        m_serverStatus.setText("OK. Server load: " + serverLoad);
        return true;
      }
    }
    return false;
  }
}
