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
 *    ClassifierCustomizer.java
 *    Copyright (C) 2002 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import weka.classifiers.Classifier;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertySheetPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.Customizer;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * GUI customizer for the classifier wrapper bean
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision$
 */
public class ClassifierCustomizer
  extends JPanel
  implements Customizer, CustomizerClosingListener {

  /** for serialization */
  private static final long serialVersionUID = -6688000820160821429L;

  static {
     GenericObjectEditor.registerEditors();
  }

  private PropertyChangeSupport m_pcSupport = 
    new PropertyChangeSupport(this);
  
  private weka.gui.beans.Classifier m_dsClassifier;
  /*  private GenericObjectEditor m_ClassifierEditor = 
      new GenericObjectEditor(true); */
  private PropertySheetPanel m_ClassifierEditor = 
    new PropertySheetPanel();

  private JPanel m_incrementalPanel = new JPanel();
  private JCheckBox m_updateIncrementalClassifier 
    = new JCheckBox("Update classifier on incoming instance stream");
  private boolean m_panelVisible = false;
  
  private JPanel m_holderPanel = new JPanel();
  private JTextField m_executionSlotsText = new JTextField();

  public ClassifierCustomizer() {
    
    m_ClassifierEditor.
      setBorder(BorderFactory.createTitledBorder("Classifier options"));
    
    m_updateIncrementalClassifier.
      setToolTipText("Train the classifier on "
		     +"each individual incoming streamed instance.");
    m_updateIncrementalClassifier.
      addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    if (m_dsClassifier != null) {
	      m_dsClassifier.
		setUpdateIncrementalClassifier(m_updateIncrementalClassifier.
					       isSelected());
	    }
	  }
	});
    m_incrementalPanel.add(m_updateIncrementalClassifier);
    
    m_executionSlotsText.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (m_dsClassifier != null &&
            m_executionSlotsText.getText().length() > 0) {
          int newSlots = Integer.parseInt(m_executionSlotsText.getText());
          m_dsClassifier.setExecutionSlots(newSlots);
        }
      }
    });
    
    JPanel executionSlotsPanel = new JPanel();
    executionSlotsPanel.
      setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    JLabel executionSlotsLabel = new JLabel("Execution slots");
    executionSlotsPanel.setLayout(new BorderLayout());
    executionSlotsPanel.add(executionSlotsLabel, BorderLayout.WEST);
    executionSlotsPanel.add(m_executionSlotsText, BorderLayout.CENTER);
    m_holderPanel.
      setBorder(BorderFactory.createTitledBorder("More options"));
    m_holderPanel.setLayout(new BorderLayout());
    m_holderPanel.add(executionSlotsPanel, BorderLayout.NORTH);
    
    setLayout(new BorderLayout());
    add(m_ClassifierEditor, BorderLayout.CENTER);
    add(m_holderPanel, BorderLayout.SOUTH);
  }
  
  private void checkOnClassifierType() {
    Classifier editedC = m_dsClassifier.getClassifier();
    if (editedC instanceof weka.classifiers.UpdateableClassifier && 
	m_dsClassifier.hasIncomingStreamInstances()) {
      if (!m_panelVisible) {
	m_holderPanel.add(m_incrementalPanel, BorderLayout.SOUTH);
	m_panelVisible = true;
	m_executionSlotsText.setEnabled(false);
      }
    } else {
      if (m_panelVisible) {
	m_holderPanel.remove(m_incrementalPanel);
	m_executionSlotsText.setEnabled(true);
	m_panelVisible = false;
      }
    }
  }

  /**
   * Set the classifier object to be edited
   *
   * @param object an <code>Object</code> value
   */
  public void setObject(Object object) {
    m_dsClassifier = (weka.gui.beans.Classifier)object;
    //    System.err.println(Utils.joinOptions(((OptionHandler)m_dsClassifier.getClassifier()).getOptions()));
    m_ClassifierEditor.setTarget(m_dsClassifier.getClassifier());
    m_updateIncrementalClassifier.
      setSelected(m_dsClassifier.getUpdateIncrementalClassifier());
    m_executionSlotsText.setText(""+m_dsClassifier.getExecutionSlots());
    checkOnClassifierType();
  }
  
  /* (non-Javadoc)
   * @see weka.gui.beans.CustomizerClosingListener#customizerClosing()
   */
  public void customizerClosing() {
    if (m_executionSlotsText.getText().length() > 0) {
      int newSlots = Integer.parseInt(m_executionSlotsText.getText());
      m_dsClassifier.setExecutionSlots(newSlots);
    }
  }

  /**
   * Add a property change listener
   *
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.addPropertyChangeListener(pcl);
  }

  /**
   * Remove a property change listener
   *
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.removePropertyChangeListener(pcl);
  }
}
