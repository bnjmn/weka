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
 *    TextViewer.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.gui.ResultHistoryPanel;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.ImageIcon;

import javax.swing.SwingConstants;
import javax.swing.JFrame;
import javax.swing.BorderFactory;
import java.awt.*;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Bean that collects and displays pieces of text
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.4 $
 */
public class TextViewer 
  extends JPanel
  implements TextListener, DataSourceListener, 
	     TrainingSetListener, TestSetListener,
	     Visible, UserRequestAcceptor, 
	     Serializable {

  protected BeanVisual m_visual = 
    new BeanVisual("TextViewer", 
		   BeanVisual.ICON_PATH+"DefaultText.gif",
		   BeanVisual.ICON_PATH+"DefaultText_animated.gif");


  private transient JFrame m_resultsFrame = null;

  /**
   * Output area for a piece of text
   */
  private transient JTextArea m_outText = new JTextArea(20, 80);

  /**
   * List of text revieved so far
   */
  protected transient ResultHistoryPanel m_history = new ResultHistoryPanel(m_outText);
  
  public TextViewer() {
    setUpResultHistory();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
  }

  private void setUpResultHistory() {
    if (m_outText == null) {
      m_outText = new JTextArea(20, 80);
      m_history = new ResultHistoryPanel(m_outText);
    }
    m_outText.setEditable(false);
    m_outText.setFont(new Font("Monospaced", Font.PLAIN, 12));
    m_outText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_history.setBorder(BorderFactory.createTitledBorder("Result list"));
  }

//    public synchronized void acceptClassifier(BatchClassifierEvent e) {
//      weka.classifiers.Classifier c = e.getClassifier();
//      String modelString = c.toString();
//      String titleString = c.getClass().getName();
//      titleString = titleString.substring(titleString.lastIndexOf('.')+1,
//  					titleString.length());
//      titleString = "Set "+e.getSetNumber()+" ("+e.getTestSet().relationName()
//        +") "+titleString+" model";
//      TextEvent nt = new TextEvent(e.getSource(),
//  				 modelString,
//  				 titleString);
//      acceptText(nt);
//    }

  /**
   * Accept a data set for displaying as text
   *
   * @param e a <code>DataSetEvent</code> value
   */
  public synchronized void acceptDataSet(DataSetEvent e) {
    TextEvent nt = new TextEvent(e.getSource(), 
				 e.getDataSet().toString(),
				 e.getDataSet().relationName());
    acceptText(nt);
  }

  /**
   * Accept a training set for displaying as text
   *
   * @param e a <code>TrainingSetEvent</code> value
   */
  public synchronized void acceptTrainingSet(TrainingSetEvent e) {
    TextEvent nt = new TextEvent(e.getSource(), 
				 e.getTrainingSet().toString(),
				 e.getTrainingSet().relationName());
    acceptText(nt);
  }

  /**
   * Accept a test set for displaying as text
   *
   * @param e a <code>TestSetEvent</code> value
   */
  public synchronized void acceptTestSet(TestSetEvent e) {
    TextEvent nt = new TextEvent(e.getSource(), 
				 e.getTestSet().toString(),
				 e.getTestSet().relationName());
    acceptText(nt);
  }

  /**
   * Accept some text
   *
   * @param e a <code>TextEvent</code> value
   */
  public synchronized void acceptText(TextEvent e) {
    if (m_outText == null) {
      setUpResultHistory();
    }
    StringBuffer result = new StringBuffer();
    result.append(e.getText());
    //    m_resultsString.append(e.getText());
    //    m_outText.setText(m_resultsString.toString());
    String name = (new SimpleDateFormat("HH:mm:ss - "))
      .format(new Date());
    name += e.getTextTitle();

    // see if there is an entry with this name already in the list -
    // could happen if two items with the same name arrive at the same second
    int mod = 2;
    String nameOrig = new String(name);
    while (m_history.getNamedBuffer(name) != null) {
      name = new String(nameOrig+""+mod);
      mod++;
    }
    m_history.addResult(name, result);
    m_history.setSingle(name);
  }

  /**
   * Describe <code>setVisual</code> method here.
   *
   * @param newVisual a <code>BeanVisual</code> value
   */
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  /**
   * Get the visual appearance of this bean
   */
  public BeanVisual getVisual() {
    return m_visual;
  }
  
  /**
   * Use the default visual appearance for this bean
   */
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH+"DefaultText.gif",
		       BeanVisual.ICON_PATH+"DefaultText_animated.gif");
  }

  /**
   * Popup a component to display the selected text
   */
  public void showResults() {
    if (m_resultsFrame == null) {
      if (m_outText == null) {
	setUpResultHistory();
      }
      m_resultsFrame = new JFrame("Text Viewer");
      m_resultsFrame.getContentPane().setLayout(new BorderLayout());
      final JScrollPane js = new JScrollPane(m_outText);
      js.setBorder(BorderFactory.createTitledBorder("Text"));
      m_resultsFrame.getContentPane().add(js, BorderLayout.CENTER);
      m_resultsFrame.getContentPane().add(m_history, BorderLayout.WEST);
      m_resultsFrame.addWindowListener(new java.awt.event.WindowAdapter() {
	  public void windowClosing(java.awt.event.WindowEvent e) {
	    m_resultsFrame.dispose();
	    m_resultsFrame = null;
	  }
	});
      m_resultsFrame.pack();
      m_resultsFrame.setVisible(true);
    }
  }

  /**
   * Get a list of user requests
   *
   * @return an <code>Enumeration</code> value
   */
  public Enumeration enumerateRequests() {
    Vector newVector = new Vector(0);
    if (m_resultsFrame == null) {
      newVector.addElement("Show results");
    }
    newVector.addElement("Clear results");
    return newVector.elements();
  }

  /**
   * Perform the named request
   *
   * @param request a <code>String</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public void performRequest(String request) {
    if (request.compareTo("Show results") == 0) {
      showResults();
    } else if (request.compareTo("Clear results") == 0) {
      m_outText.setText("");
    } else {
      throw new 
	IllegalArgumentException(request
		    + " not supported (TextViewer)");
    }
  }
}
