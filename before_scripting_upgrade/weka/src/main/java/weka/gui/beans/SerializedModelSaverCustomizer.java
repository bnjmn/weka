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
 *    SerializedModelSaverCustomizer.java
 *    Copyright (C) 2008 Pentaho Corporation
 *
 */

package weka.gui.beans;

import weka.gui.GenericObjectEditor;
import weka.gui.PropertySheetPanel;
import weka.core.Tag;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.Customizer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;

import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

/**
 * GUI Customizer for the SerializedModelSaver bean
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org
 * @version $Revision: 1.3 $
 */
public class SerializedModelSaverCustomizer
  extends JPanel
  implements Customizer, CustomizerCloseRequester {

  /** for serialization */
  private static final long serialVersionUID = -4874208115942078471L;

  static {
     GenericObjectEditor.registerEditors();
  }

  private PropertyChangeSupport m_pcSupport = 
    new PropertyChangeSupport(this);

  private weka.gui.beans.SerializedModelSaver m_smSaver;

  private PropertySheetPanel m_SaverEditor = 
    new PropertySheetPanel();

  private JFileChooser m_fileChooser 
    = new JFileChooser(new File(System.getProperty("user.dir")));
  

  private JFrame m_parentFrame;
  
  private JTextField m_prefixText;

  private JComboBox m_fileFormatBox;

  private JCheckBox m_relativeFilePath;
  

  /** Constructor */  
  public SerializedModelSaverCustomizer() {

    try {
      m_SaverEditor.addPropertyChangeListener(
	  new PropertyChangeListener() {
	      public void propertyChange(PropertyChangeEvent e) {
		repaint();
		if (m_smSaver != null) {
		  System.err.println("Property change!!");
                  //		  m_smSaver.setSaver(m_smSaver.getSaver());
		}
	      }
	    });
      repaint();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    setLayout(new BorderLayout());

    m_fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
    m_fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    m_fileChooser.setApproveButtonText("Select directory and prefix");

    m_fileChooser.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
	    try {
              m_smSaver.setPrefix(m_prefixText.getText());
              m_smSaver.setDirectory(m_fileChooser.getSelectedFile());               
	    } catch (Exception ex) {
	      ex.printStackTrace();
	    }
	  }
	  // closing
	  if (m_parentFrame != null) {
	    m_parentFrame.dispose();
	  }
	}
      });   
  }

  public void setParentFrame(JFrame parent) {
    m_parentFrame = parent;
  }
  
  private void setUpOther() {
    removeAll();
    add(m_SaverEditor, BorderLayout.CENTER);
    validate();
    repaint();
  }
  
  /** Sets up dialog for saving models to a file */  
  public void setUpFile() {
    removeAll();
    m_fileChooser.setFileFilter(new FileFilter()
        { public boolean accept(File f)
            { return f.isDirectory();}
          public String getDescription()
            { return "Directory";}
         });

    m_fileChooser.setAcceptAllFileFilterUsed(false);

    try{
      if (!m_smSaver.getDirectory().getPath().equals("")) {
        File tmp = m_smSaver.getDirectory();
        tmp = new File(tmp.getAbsolutePath());
        m_fileChooser.setCurrentDirectory(tmp);
      }
    } catch(Exception ex) {
      System.out.println(ex);
    }

    JPanel innerPanel = new JPanel();
    innerPanel.setLayout(new BorderLayout());
    try {
      m_prefixText = new JTextField(m_smSaver.getPrefix(), 25); 
      JLabel prefixLab = new JLabel("Prefix for file name:", SwingConstants.LEFT);
      JPanel prefixP = new JPanel();   
      prefixP.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      //      prefixP.setLayout(new FlowLayout(FlowLayout.LEFT));
      prefixP.setLayout(new BorderLayout());
      prefixP.add(prefixLab, BorderLayout.WEST);
      prefixP.add(m_prefixText, BorderLayout.CENTER);
      innerPanel.add(prefixP, BorderLayout.SOUTH);

      JPanel ffP = new JPanel();
      ffP.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      ffP.setLayout(new BorderLayout());
      ffP.add(new JLabel(" File format:"), BorderLayout.WEST);
      setUpFileFormatComboBox();
      ffP.add(m_fileFormatBox, BorderLayout.CENTER);
      innerPanel.add(ffP, BorderLayout.CENTER);
    } catch(Exception ex){
    }
    //innerPanel.add(m_SaverEditor, BorderLayout.SOUTH);
    JPanel about = m_SaverEditor.getAboutPanel();
    if (about != null) {
      innerPanel.add(about, BorderLayout.NORTH);
    }
    add(innerPanel, BorderLayout.NORTH);
    add(m_fileChooser, BorderLayout.CENTER);

    m_relativeFilePath = new JCheckBox("Use relative file paths");
    m_relativeFilePath.
      setSelected(m_smSaver.getUseRelativePath());

    m_relativeFilePath.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          m_smSaver.setUseRelativePath(m_relativeFilePath.isSelected());
        }
      });

    JPanel holderPanel = new JPanel();
    holderPanel.setLayout(new FlowLayout());
    holderPanel.add(m_relativeFilePath);
    add(holderPanel, BorderLayout.SOUTH);
  }

  /**
   * Set the model saver to be customized
   *
   * @param object a weka.gui.beans.SerializedModelSaver
   */
  public void setObject(Object object) {
    m_smSaver = (weka.gui.beans.SerializedModelSaver)object;
    m_SaverEditor.setTarget(m_smSaver);

    setUpFile();    
  }

  private void setUpFileFormatComboBox() {
    m_fileFormatBox = new JComboBox();
    for (int i = 0; i < SerializedModelSaver.s_fileFormatsAvailable.size(); i++) {
      Tag temp = SerializedModelSaver.s_fileFormatsAvailable.get(i);
      m_fileFormatBox.addItem(temp);
    }

    Tag result = m_smSaver.validateFileFormat(m_smSaver.getFileFormat());
    if (result == null) {
      m_fileFormatBox.setSelectedIndex(0);
    } else {
      m_fileFormatBox.setSelectedItem(result);
    }

    m_fileFormatBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Tag selected = (Tag)m_fileFormatBox.getSelectedItem();
          if (selected != null) {
            m_smSaver.setFileFormat(selected);
          }
        }
      });
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
