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
 *    ClassAssignerCustomizer.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import java.io.File;
import java.beans.*;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import weka.gui.PropertySheetPanel;
import weka.core.Instances;
import weka.core.Attribute;

/**
 * GUI customizer for the class assigner bean
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.3 $
 */
public class ClassAssignerCustomizer extends JPanel
  implements Customizer, CustomizerClosingListener, DataFormatListener {

  private boolean m_displayColNames = false;

  private ClassAssigner m_classAssigner;

  private PropertyChangeSupport m_pcSupport = 
    new PropertyChangeSupport(this);

  private PropertySheetPanel m_caEditor = 
    new PropertySheetPanel();

  private JComboBox m_ClassCombo = new JComboBox();
  private JPanel m_holderP = new JPanel();

  public ClassAssignerCustomizer() {
    setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5));

    setLayout(new BorderLayout());
    add(new javax.swing.JLabel("ClassAssignerCustomizer"), 
	BorderLayout.NORTH);
    m_holderP.setLayout(new BorderLayout());
    m_holderP.setBorder(BorderFactory.createTitledBorder("Choose class attribute"));
    m_holderP.add(m_ClassCombo, BorderLayout.CENTER);
    m_ClassCombo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  if (m_classAssigner != null && m_displayColNames == true) {
	    m_classAssigner.setClassColumn(""+(m_ClassCombo.getSelectedIndex()+1));
	  }
	}
      });
    add(m_caEditor, BorderLayout.CENTER);
  }

  private void setUpStandardSelection() {
    if (m_displayColNames == true) {
      remove(m_holderP);
      m_caEditor.setTarget(m_classAssigner);
      add(m_caEditor, BorderLayout.CENTER);
      m_displayColNames = false;
    }
    validate(); repaint();
  }
  
  private void setUpColumnSelection(Instances format) {
    if (m_displayColNames == false) {
      remove(m_caEditor);
    }
    int existingClassCol = format.classIndex();
    if (existingClassCol < 0) {
      existingClassCol = 0;
    }
    String [] attribNames = new String [format.numAttributes()];
    for (int i = 0; i < attribNames.length; i++) {
      String type = "";
      switch (format.attribute(i).type()) {
      case Attribute.NOMINAL:
	type = "(Nom) ";
	break;
      case Attribute.NUMERIC:
	type = "(Num) ";
	break;
      case Attribute.STRING:
	type = "(Str) ";
	break;
      default:
	type = "(???) ";
      }
      attribNames[i] = type + format.attribute(i).name();
    }
    m_ClassCombo.setModel(new DefaultComboBoxModel(attribNames));
    if (attribNames.length > 0) {
      m_ClassCombo.setSelectedIndex(existingClassCol);
    }
    if (m_displayColNames == false) {
      add(m_holderP, BorderLayout.CENTER);
      m_displayColNames = true;
    }
    validate(); repaint();
  }

  /**
   * Set the bean to be edited
   *
   * @param object an <code>Object</code> value
   */
  public void setObject(Object object) {
    if (m_classAssigner != (ClassAssigner)object) {
      // remove ourselves as a listener from the old ClassAssigner (if necessary)
      if (m_classAssigner != null) {
	m_classAssigner.removeDataFormatListener(this);
      }
      m_classAssigner = (ClassAssigner)object;
      // add ourselves as a data format listener
      m_classAssigner.addDataFormatListener(this);
      m_caEditor.setTarget(m_classAssigner);
      if (m_classAssigner.getConnectedFormat() != null) {
	setUpColumnSelection(m_classAssigner.getConnectedFormat());
      }
    }
  }

  public void customizerClosing() {
    // remove ourselves as a listener from the ClassAssigner (if necessary)
    if (m_classAssigner != null) {
      System.err.println("Customizer deregistering with class assigner");
      m_classAssigner.removeDataFormatListener(this);
    }
  }

  public void newDataFormat(DataSetEvent dse) {
    if (dse.getDataSet() != null) {
      //      System.err.println("Setting up column selection.........");
      setUpColumnSelection(m_classAssigner.getConnectedFormat());
    } else {
      setUpStandardSelection();
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
