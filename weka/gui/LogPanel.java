/*
 *    LogPanel.java
 *    Copyright (C) 1999 Len Trigg
 *
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

package weka.gui;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.JViewport;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.Point;

/** 
 * This panel allows log and status messages to be posted. Log messages
 * appear in a scrollable text area, and status messages appear as one-line
 * transient messages.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class LogPanel extends JPanel implements Logger {

  /** Displays the current status */
  protected JLabel m_StatusLab = new JLabel("OK");
  
  /** Displays the log messages */
  protected JTextArea m_LogText = new JTextArea(5, 20);

  /** To determine whether a change was caused by scrolling or appending */
  protected int m_TextHeight;

  /** An indicator for whether text has been output yet */
  protected boolean m_First = true;
  
  /**
   * Creates the log panel
   */
  public LogPanel() {

    m_LogText.setEditable(false);
    m_LogText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_StatusLab.setBorder(BorderFactory.createCompoundBorder(
			  BorderFactory.createTitledBorder("Status"),
			  BorderFactory.createEmptyBorder(0, 5, 5, 5)));
    
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createTitledBorder("Log"));
    p1.setLayout(new BorderLayout());
    JScrollPane js = new JScrollPane(m_LogText);
    p1.add(js, BorderLayout.CENTER);
    m_TextHeight = js.getViewport().getViewSize().height;
    js.getViewport().addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
	JViewport vp = (JViewport)e.getSource();
	int h = vp.getViewSize().height; 
	// If new content is added ViewSize changes. This event is also
	// fired when the user moves the scrollbar - in this case we
	// don't want to move it back to the bottom.
	if (h != m_TextHeight) {
	  m_TextHeight = h;
	  int x = h - vp.getExtentSize().height;
	  vp.setViewPosition(new Point(0, x));
	}
      }
    });

    setLayout(new BorderLayout());
    add(p1, BorderLayout.CENTER);
    add(m_StatusLab, BorderLayout.SOUTH);
  }

    
  /**
   * Gets a string containing current date and time.
   *
   * @return a string containing the date and time.
   */
  protected static String getTimestamp() {

    return (new SimpleDateFormat("HH:mm:ss:")).format(new Date());
  }

  /**
   * Sends the supplied message to the log area. The current timestamp will
   * be prepended.
   *
   * @param message a value of type 'String'
   */
  public void logMessage(String message) {

    if (m_First) {
      m_First = false;
    } else {
      m_LogText.append("\n");
    }
    m_LogText.append(LogPanel.getTimestamp() + ' ' + message);
  }

  /**
   * Sends the supplied message to the status line.
   *
   * @param message the status message
   */
  public void statusMessage(String message) {

    m_StatusLab.setText(message);
  }

  
  /**
   * Tests out the log panel from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      final javax.swing.JFrame jf = new javax.swing.JFrame("Log Panel");
      jf.getContentPane().setLayout(new BorderLayout());
      final LogPanel lp = new LogPanel();
      jf.getContentPane().add(lp, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
      lp.logMessage("Welcome to the generic log panel!");
      lp.statusMessage("Hi there");
      lp.logMessage("Funky chickens");
      
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
