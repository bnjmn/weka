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
 *    ListModelsPanel.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor;

import weka.classifiers.EnsembleLibrary;
import weka.classifiers.EnsembleLibraryModel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;

/**
 * This class is responsible for creating the main panel in the library editor
 * gui. It is responsible for displaying the entire list of models currently in
 * the list. It is also responsible for allowing the user to save/load this list
 * as a flat file and choosing the working directory for the library.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.2 $
 */
public class ListModelsPanel
  extends JPanel 
  implements ActionListener {
  
  /** for serialization */
  private static final long serialVersionUID = -1986253077944432252L;

  /** The library being edited */
  private EnsembleLibrary m_Library;
  
  /** The button for removing selected models */
  private JButton m_RemoveSelectedButton;
  
  /** The button for opening a model list from a file */
  private JButton m_OpenModelFileButton;
  
  /** The button for saving a model list to a file */
  private JButton m_SaveModelFileButton;
  
  /** The ModelList object that displays all currently selected models */
  private ModelList m_ModelList;
  
  /** The file chooser for the user to select model list files to save and load */
  private JFileChooser m_modelListChooser = new JFileChooser(new File(
      System.getProperty("user.dir")));
  
  /** 
   * Constructor to initialize library object and GUI
   * 
   * @param library 	the library to use
   */
  public ListModelsPanel(EnsembleLibrary library) {
    m_Library = library;
    createListModelsPanel();
  }
  
  /** 
   * this is necessay to set the Library object after initiialization
   * 
   * @param library 	the library to use
   */
  public void setLibrary(EnsembleLibrary library) {
    
    m_Library = library;
  }
  
  /**
   * Builds the GUI.
   */
  private void createListModelsPanel() {
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(new GridBagLayout());
    
    JPanel modelListPanel = new JPanel();
    modelListPanel.setBorder(
	BorderFactory.createTitledBorder("Currently Chosen Library Models"));
    
    m_ModelList = new ModelList();
    
    m_ModelList.getInputMap().put(
	KeyStroke.getKeyStroke("released DELETE"), "deleteSelected");
    m_ModelList.getActionMap().put("deleteSelected",
	new AbstractAction("deleteSelected") {

      private static final long serialVersionUID = 8178827388328307805L;
      
      public void actionPerformed(ActionEvent evt) {
	
	Object[] currentModels = m_ModelList.getSelectedValues();
	
	for (int i = 0; i < currentModels.length; i++) {
	  removeModel((EnsembleLibraryModel) currentModels[i]);
	}
	
	// Shrink the selected range to the first index that was
	// selected
	int selected[] = new int[1];
	selected[0] = m_ModelList.getSelectedIndices()[0];
	m_ModelList.setSelectedIndices(selected);
      }
    });
    
    m_ModelList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    m_ModelList.setVisibleRowCount(-1);
    m_ModelList.setLayoutOrientation(JList.VERTICAL);
    JScrollPane listScroller = new JScrollPane(m_ModelList);
    listScroller.setPreferredSize(new Dimension(150, 50));
    
    modelListPanel.setLayout(new BorderLayout());
    modelListPanel.add(listScroller, BorderLayout.CENTER);
    
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 3;
    add(modelListPanel, gbc);
    
    m_RemoveSelectedButton = new JButton("Remove Selected");
    m_RemoveSelectedButton.addActionListener(this);
    m_RemoveSelectedButton.setToolTipText(
	"Remove all currently selected models from the above list");
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 1;
    gbc.weighty = 0;
    gbc.gridwidth = 1;
    add(m_RemoveSelectedButton, gbc);
    
    m_modelListChooser.setAcceptAllFileFilterUsed(false);
    
    XMLModelFileFilter xmlFilter = new XMLModelFileFilter();
    m_modelListChooser.addChoosableFileFilter(xmlFilter);
    m_modelListChooser.addChoosableFileFilter(new FlatModelFileFilter());
    // set up the file chooser
    m_modelListChooser.setFileFilter(xmlFilter);
    m_modelListChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    // create the buttons + field
    m_OpenModelFileButton = new JButton("Open...");
    m_OpenModelFileButton.addActionListener(this);
    m_OpenModelFileButton.setToolTipText(
	"Import a model list file from the file system");
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    gbc.weightx = 1;
    gbc.weighty = 0;
    add(m_OpenModelFileButton, gbc);
    
    m_SaveModelFileButton = new JButton("Save...");
    m_SaveModelFileButton.addActionListener(this);
    m_SaveModelFileButton.setToolTipText(
	"Save the current list of models to a file");
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    gbc.weightx = 1;
    gbc.weighty = 0;
    add(m_SaveModelFileButton, gbc);
  }
  
  /**
   * returns the current library
   * 
   * @return		the current library
   */
  public EnsembleLibrary getLibrary() {
    return m_Library;
  }
  
  /**
   * This handles all of the button events. Specifically the buttons
   * associated with saving/loading model lists as well as removing models
   * from the currently displayed list.
   * 
   * @param e
   *            the action event that occured
   */
  public void actionPerformed(ActionEvent e) {
    
    if (e.getSource() == m_OpenModelFileButton) {
      
      int returnVal = m_modelListChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	
	clearAll();
	
	File selectedFile = m_modelListChooser.getSelectedFile();
	
	// add .mlf extension if that was the file filter used
	if ((m_modelListChooser.getFileFilter() instanceof FlatModelFileFilter)) {
	  if (!selectedFile.getName().endsWith(
	      EnsembleLibrary.FLAT_FILE_EXTENSION)) {
	    selectedFile = new File(selectedFile.getPath()
		+ EnsembleLibrary.FLAT_FILE_EXTENSION);
	  }
	  
	  // otherwise use .model.xml file extension by default
	} else {
	  if (!selectedFile.getName().endsWith(
	      EnsembleLibrary.XML_FILE_EXTENSION)) {
	    selectedFile = new File(selectedFile.getPath()
		+ EnsembleLibrary.XML_FILE_EXTENSION);
	  }
	}
	
	EnsembleLibrary.loadLibrary(selectedFile, this, m_Library);
	
	ModelList.SortedListModel dataModel = ((ModelList.SortedListModel) m_ModelList.getModel());
	
	TreeSet models = m_Library.getModels();
	for (Iterator it = models.iterator(); it.hasNext();) {
	  addModel((EnsembleLibraryModel) it.next());
	  // dataModel.add(it.next());
	}
	
      }
      
    } else if (e.getSource() == m_SaveModelFileButton) {
      
      int returnVal = m_modelListChooser.showSaveDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	
	File selectedFile = m_modelListChooser.getSelectedFile();
	
	// add .mlf extension if that was the file filter used
	if ((m_modelListChooser.getFileFilter() instanceof FlatModelFileFilter)) {
	  if (!selectedFile.getName().endsWith(
	      EnsembleLibrary.FLAT_FILE_EXTENSION)) {
	    selectedFile = new File(selectedFile.getPath()
		+ EnsembleLibrary.FLAT_FILE_EXTENSION);
	  }
	  
	  // otherwise use .model.xml file extension by default
	} else {
	  if (!selectedFile.getName().endsWith(
	      EnsembleLibrary.XML_FILE_EXTENSION)) {
	    selectedFile = new File(selectedFile.getPath()
		+ EnsembleLibrary.XML_FILE_EXTENSION);
	  }
	}
	
	EnsembleLibrary.saveLibrary(selectedFile, m_Library, this);
      }
      
    } else if (e.getSource() == m_RemoveSelectedButton) {
      
      // here we simply get the list of models that are
      // currently selected and ten remove them from the list
      
      // ModelList.SortedListModel dataModel =
      // ((ModelList.SortedListModel)m_ModelList.getModel());
      
      Object[] currentModels = m_ModelList.getSelectedValues();
      
      for (int i = 0; i < currentModels.length; i++) {
	removeModel((EnsembleLibraryModel) currentModels[i]);
      }
      
      // Shrink the selected range to the first index that was selected
      if (m_ModelList.getSelectedIndices().length > 0) {
	int selected[] = new int[1];
	selected[0] = m_ModelList.getSelectedIndices()[0];
	m_ModelList.setSelectedIndices(selected);
      }
    }
  }
  
  /**
   * This removes all the models from the current list in the GUI
   */
  public void clearAll() {
    ((ModelList.SortedListModel) m_ModelList.getModel()).clear();
    m_Library.clearModels();
  }
  
  /**
   * Adds a model to the current library
   * 
   * @param model	the model to add
   */
  public void addModel(EnsembleLibraryModel model) {
    
    ((ModelList.SortedListModel) m_ModelList.getModel()).add(model);
    m_Library.addModel(model);
    
  }
  
  /**
   * Removes a model to the current library
   * 
   * @param model	the model to remove
   */
  public void removeModel(EnsembleLibraryModel model) {
    ((ModelList.SortedListModel) m_ModelList.getModel())
    .removeElement(model);
    m_Library.removeModel(model);
  }
  
  /**
   * A helper class for filtering xml model files
   */
  class XMLModelFileFilter 
    extends FileFilter {
    
    /**
     * Whether the given file is accepted by this filter.
     * 
     * @param file	the file to check
     * @return		true if the file got accepted
     */
    public boolean accept(File file) {
      String filename = file.getName();
      return (filename.endsWith(EnsembleLibrary.XML_FILE_EXTENSION) || file
	  .isDirectory());
    }
    
    /**
     * The description of this filter.
     * 
     * @return		the description
     */
    public String getDescription() {
      return "XML Library Files (*.model.xml)";
    }
  }
  
  /**
   * A helper class for filtering flat model files
   * 
   */
  class FlatModelFileFilter 
    extends FileFilter {
    
    /**
     * Whether the given file is accepted by this filter.
     * 
     * @param file	the file to check
     * @return		true if the file got accepted
     */
    public boolean accept(File file) {
      String filename = file.getName();
      return (filename.endsWith(EnsembleLibrary.FLAT_FILE_EXTENSION) || file
	  .isDirectory());
    }
    
    /**
     * The description of this filter.
     * 
     * @return		the description
     */
    public String getDescription() {
      return "Model List Files (*.mlf)";
    }
  }
  
}
