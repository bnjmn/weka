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
 *    LoaderCustomizer.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.gui.FileEditor;

import java.io.File;
import java.beans.*;
import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JDialog;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertySheetPanel;
import weka.gui.ExtensionFileFilter;
import weka.core.converters.Loader;
import weka.core.converters.FileSourcedConverter;

/**
 * GUI Customizer for the loader bean
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version 1.0
 */
public class LoaderCustomizer extends JPanel
  implements Customizer, CustomizerCloseRequester {

  static {

    java.beans.PropertyEditorManager
      .registerEditor(weka.core.converters.Loader.class,
		      weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.core.SelectedTag.class,
		      weka.gui.SelectedTagEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.filters.Filter.class,
		      weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.attributeSelection.ASSearch.class,
		      weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.attributeSelection.ASEvaluation.class,
		      weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.classifiers.Classifier [].class,
		      weka.gui.GenericArrayEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(Object [].class,
		      weka.gui.GenericArrayEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.classifiers.Classifier.class,
		      weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.classifiers.CostMatrix.class,
		      weka.gui.CostMatrixEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(java.io.File.class,
		      weka.gui.FileEditor.class);
  }

  private PropertyChangeSupport m_pcSupport = 
    new PropertyChangeSupport(this);

  private weka.gui.beans.Loader m_dsLoader;

  private PropertySheetPanel m_LoaderEditor = 
    new PropertySheetPanel();

  private JFileChooser m_fileChooser 
    = new JFileChooser(new File(System.getProperty("user.dir")));
  /*  private JDialog m_chooserDialog = 
    new JDialog((JFrame)getTopLevelAncestor(),
    true); */

  private JFrame m_parentFrame;

  public LoaderCustomizer() {
    /*    m_fileEditor.addPropertyChangeListener(new PropertyChangeListener() {
	public void propertyChange(PropertyChangeEvent e) {
	  if (m_dsLoader != null) {
	    m_dsLoader.setDataSetFile((File)m_fileEditor.getValue());
	  }
	}
	}); */

    try {
      /*      m_LoaderEditor.setClassType(weka.core.converters.Loader.class);
	      m_LoaderEditor.setValue(new weka.core.converters.ArffLoader()); */
      m_LoaderEditor.addPropertyChangeListener(
	  new PropertyChangeListener() {
	      public void propertyChange(PropertyChangeEvent e) {
		repaint();
		if (m_dsLoader != null) {
		  System.err.println("Property change!!");
		  m_dsLoader.setLoader(m_dsLoader.getLoader());
		}
	      }
	    });
      repaint();
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    setLayout(new BorderLayout());
    //    add(m_fileEditor.getCustomEditor(), BorderLayout.CENTER);
    //    add(m_LoaderEditor, BorderLayout.CENTER);
    m_fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
    m_fileChooser.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
	    try {
	      ((FileSourcedConverter)m_dsLoader.getLoader()).
		setFile(m_fileChooser.getSelectedFile());
	      // tell the loader that a new file has been selected so
	      // that it can attempt to load the header
	      m_dsLoader.setLoader(m_dsLoader.getLoader());
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
    add(m_LoaderEditor, BorderLayout.CENTER);
    validate();
    repaint();
  }

  public void setUpFile() {
    removeAll();
    m_fileChooser.
      setSelectedFile(((FileSourcedConverter)m_dsLoader.getLoader()).getFile());
    ExtensionFileFilter ff = 
      new ExtensionFileFilter(((FileSourcedConverter)m_dsLoader.getLoader()).
			      getFileExtension(),
			      ((FileSourcedConverter)m_dsLoader.getLoader()).
			      getFileDescription());
    m_fileChooser.addChoosableFileFilter(ff);
    JPanel about = m_LoaderEditor.getAboutPanel();
    if (about != null) {
      add(about, BorderLayout.NORTH);
    }
    add(m_fileChooser, BorderLayout.CENTER);
  }

  /**
   * Set the loader to be customized
   *
   * @param object a weka.gui.beans.Loader
   */
  public void setObject(Object object) {
    m_dsLoader = (weka.gui.beans.Loader)object;
    m_LoaderEditor.setTarget(m_dsLoader.getLoader());
    //    m_fileEditor.setValue(m_dsLoader.getDataSetFile());
    if (m_dsLoader.getLoader() instanceof FileSourcedConverter) {
      setUpFile();
    } else {
      setUpOther();
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
