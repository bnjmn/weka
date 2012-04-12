/*
 *    ResultHistoryPanel.java
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

import java.util.Hashtable;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;

import javax.swing.JPanel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

/** 
 * A component that accepts named stringbuffers and displays the name in a list
 * box. When a name is double-clicked, a frame is popped up that contains
 * the string held by the stringbuffer. Optionally a text component may be
 * provided that will have it's text set to the named result text on a
 * single-click.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class ResultHistoryPanel extends JPanel {
  
  /** An optional component for single-click display */
  protected JTextComponent m_SingleText;

  /** The named result being viewed in the single-click display */
  protected String m_SingleName;
  
  /** The list model */
  protected DefaultListModel m_Model = new DefaultListModel();

  /** The list component */
  protected JList m_List = new JList(m_Model);
  
  /** A Hashtable mapping names to result buffers */
  protected Hashtable m_Results = new Hashtable();

  /** A Hashtable mapping names to output text components */
  protected Hashtable m_FramedOutput = new Hashtable();
  
  /**
   * Create the result history object
   *
   * @param text the optional text component for single-click display
   */
  public ResultHistoryPanel(JTextComponent text) {
    
    m_SingleText = text;
    m_List.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
	if ((e.getModifiers() & InputEvent.BUTTON1_MASK)
	    == InputEvent.BUTTON1_MASK) {
	  int index = m_List.locationToIndex(e.getPoint());
	  if ((index != -1) && (m_SingleText != null)) {
	    setSingle((String)m_Model.elementAt(index));
	  }
	} else {
	  int index = m_List.locationToIndex(e.getPoint());
	  if (index != -1) {
	    openFrame((String)m_Model.elementAt(index));
	  }
	}
      }
    });
    setLayout(new BorderLayout());
    //    setBorder(BorderFactory.createTitledBorder("Result history"));
    add(new JScrollPane(m_List), BorderLayout.CENTER);
  }

  /**
   * Adds a new result to the result list.
   *
   * @param name the name to associate with the result
   * @param result the StringBuffer that contains the result text
   */
  public void addResult(String name, StringBuffer result) {
    
    m_Model.addElement(name);
    m_Results.put(name, result);
  }

  /**
   * Sets the single-click display to view the named result.
   *
   * @param name the name of the result to display.
   */
  public void setSingle(String name) {

    StringBuffer buff = (StringBuffer) m_Results.get(name);
    if (buff != null) {
      m_SingleName = name;
      m_SingleText.setText(buff.toString());
    }
  }
  
  /**
   * Opens the named result in a separate frame.
   *
   * @param name the name of the result to open.
   */
  public void openFrame(String name) {

    StringBuffer buff = (StringBuffer) m_Results.get(name);
    JTextComponent currentText = (JTextComponent) m_FramedOutput.get(name);
    if ((buff != null) && (currentText == null)) {
      // Open the frame.
      JTextArea ta = new JTextArea();
      ta.setFont(new Font("Dialoginput", Font.PLAIN, 10));
      ta.setEditable(false);
      ta.setText(buff.toString());
      m_FramedOutput.put(name, ta);
      final JFrame jf = new JFrame(name);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  m_FramedOutput.remove(jf.getTitle());
	  jf.dispose();
	}
      });
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(new JScrollPane(ta), BorderLayout.CENTER);
      jf.pack();
      jf.setSize(450, 350);
      jf.setVisible(true);
    }
  }

  /**
   * Tells any component currently displaying the named result that the
   * contents of the result text in the StringBuffer have been updated.
   *
   * @param name the name of the result that has been updated.
   */
  public void updateResult(String name) {

    StringBuffer buff = (StringBuffer) m_Results.get(name);
    if (buff == null) {
      return;
    }
    if (m_SingleName == name) {
      m_SingleText.setText(buff.toString());
    }
    JTextComponent currentText = (JTextComponent) m_FramedOutput.get(name);
    if (currentText != null) {
      currentText.setText(buff.toString());
    }
  }
  
  /**
   * Tests out the result history from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      final javax.swing.JFrame jf =
	new javax.swing.JFrame("Weka Knowledge Explorer: Classifier");
      jf.getContentPane().setLayout(new BorderLayout());
      final ResultHistoryPanel jd = new ResultHistoryPanel(null);
      jd.addResult("blah", new StringBuffer("Nothing to see here"));
      jd.addResult("blah1", new StringBuffer("Nothing to see here1"));
      jd.addResult("blah2", new StringBuffer("Nothing to see here2"));
      jd.addResult("blah3", new StringBuffer("Nothing to see here3"));
      jf.getContentPane().add(jd, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
