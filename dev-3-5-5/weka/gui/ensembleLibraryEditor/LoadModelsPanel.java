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
 *    LoadModelsPanel.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor;

import weka.classifiers.EnsembleLibraryModel;
import weka.classifiers.meta.ensembleSelection.EnsembleSelectionLibrary;
import weka.classifiers.meta.ensembleSelection.EnsembleSelectionLibraryModel;
import weka.gui.EnsembleSelectionLibraryEditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * 
 * @author  Robert Jung
 * @version $Revision: 1.1 $
 */
public class LoadModelsPanel
  extends JPanel
  implements ActionListener, PropertyChangeListener, ChangeListener {
  
  /** for serialization */
  private static final long serialVersionUID = -6961209378227736515L;

  /**
   * This is a reference to the main gui object that is responsible for
   * displaying the model library. This panel will add models to the main
   * panel through methods in this object.
   */
  private ListModelsPanel m_ListModelsPanel;
  
  /**
   * This will display messages associated with model loading. Currently the
   * number of models found and the number of data sets.
   */
  private JLabel m_LoadingLabel;
  
  /**
   * This will display the current working Directory of the Ensemble Library.
   */
  private JLabel m_DirectoryLabel;
  
  /**
   * This button will refresh the model list currently displayed in the case
   * that either the working directory changed or the models stored in it
   * changed.
   */
  private JButton m_RefreshListButton;
  
  /**
   * This button will allow users to remove all of the models currently
   * selected in the m_ModeList object
   */
  private JButton m_RemoveSelectedButton;
  
  /**
   * This button will allow users to add all models currently in the model
   * list to the model library in the ListModelsPanel. Note that this
   * operation will exclude any models that had errors
   */
  private JButton m_AddAllButton;
  
  /**
   * This button will add all of the models that had are currently selected in
   * the model list.
   */
  private JButton m_AddSelectedButton;
  
  /**
   * This object will store all of the models that were found in the library's
   * current working directory. The ModelList class is JList list = new
   * JList(listModel); a custom class in weka.gui that knows how to display
   * library model objects in a JList
   */
  private ModelList m_ModelList;
  
  /**
   * A reference to the main library object so that we can get the current
   * working Directory.
   */
  private EnsembleSelectionLibrary m_Library;
  
  /**
   * A reference to the libary editor that contains this panel so that we can
   * see if we're selected or not.
   */
  private EnsembleSelectionLibraryEditor m_EnsembleLibraryEditor;
  
  /**
   * 
   */
  private boolean m_workingDirectoryChanged = false;
  
  /**
   * This constructor simply stores the reference to the ListModelsPanel and
   * build the user interface.
   * 
   * @param listModelsPanel	the panel
   * @param editor		the editor 
   */
  public LoadModelsPanel(ListModelsPanel listModelsPanel,
      EnsembleSelectionLibraryEditor editor) {
    
    m_ListModelsPanel = listModelsPanel;
    m_EnsembleLibraryEditor = editor;
    createLoadModelsPanel();
  }
  
  /**
   * This method is responsible for building the user interface.
   */
  private void createLoadModelsPanel() {
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(new GridBagLayout());
    
    m_RefreshListButton = new JButton("Reload List");
    m_RefreshListButton.setToolTipText(
	"Discover the set of models existing in the current working directory");
    m_RefreshListButton.addActionListener(this);
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    add(m_RefreshListButton, gbc);
    
    m_DirectoryLabel = new JLabel("");
    updateDirectoryLabel();
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 3;
    add(m_DirectoryLabel, gbc);
    
    m_LoadingLabel = new JLabel("");
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 3;
    add(m_LoadingLabel, gbc);
    
    JPanel modelListPanel = new JPanel();
    modelListPanel.setBorder(
	BorderFactory.createTitledBorder("Working Set of Loadable Models"));
    
    m_ModelList = new ModelList();
    
    m_ModelList.getInputMap().put(
	KeyStroke.getKeyStroke("released DELETE"), "deleteSelected");
    m_ModelList.getActionMap().put("deleteSelected",
	new AbstractAction("deleteSelected") {
      public void actionPerformed(ActionEvent evt) {
	
	Object[] currentModels = m_ModelList.getSelectedValues();
	
	ModelList.SortedListModel dataModel = ((ModelList.SortedListModel) m_ModelList.getModel());
	
	for (int i = 0; i < currentModels.length; i++) {
	  dataModel.removeElement((EnsembleLibraryModel) currentModels[i]);
	}
	
	// Shrink the selected range to the first index that was
	// selected
	int selected[] = new int[1];
	selected[0] = m_ModelList.getSelectedIndices()[0];
	m_ModelList.setSelectedIndices(selected);
	
      }
    });
    
    m_ModelList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    m_ModelList.setLayoutOrientation(JList.VERTICAL);
    // m_ModelList.setVisibleRowCount(12);
    
    ToolTipManager.sharedInstance().registerComponent(m_ModelList);
    
    JScrollPane listView = new JScrollPane(m_ModelList);
    listView
    .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    listView.setPreferredSize(new Dimension(150, 50));
    
    modelListPanel.setLayout(new BorderLayout());
    modelListPanel.add(listView, BorderLayout.CENTER);
    
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 3;
    gbc.anchor = GridBagConstraints.WEST;
    add(modelListPanel, gbc);
    
    m_RemoveSelectedButton = new JButton("Remove Selected");
    m_RemoveSelectedButton.setToolTipText(
	"Remove all selected models from the above list");
    m_RemoveSelectedButton.addActionListener(this);
    gbc.weightx = 1;
    gbc.weighty = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    add(m_RemoveSelectedButton, gbc);
    
    m_AddSelectedButton = new JButton("Add Selected");
    m_AddSelectedButton.setToolTipText(
	"Add selected models in the above list to the model library");
    m_AddSelectedButton.addActionListener(this);
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 1;
    gbc.gridy = 4;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    add(m_AddSelectedButton, gbc);
    
    m_AddAllButton = new JButton("Add All");
    m_AddAllButton.setToolTipText(
	"Add all models in the above list to the model library");
    m_AddAllButton.addActionListener(this);
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 2;
    gbc.gridy = 4;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    add(m_AddAllButton, gbc);
  }
  
  /**
   * Sets the library to use
   * 
   * @param library 	the library to use
   */
  public void setLibrary(EnsembleSelectionLibrary library) {
    m_Library = library;
    updateDirectoryLabel();
    loadModels();
  }
  
  /**
   * This method will load all of the models found in the current working
   * directory into the ModelList object
   */
  public void loadModels() {
    
    ModelList.SortedListModel dataModel = ((ModelList.SortedListModel) m_ModelList.getModel());
    
    int directoryCount = 0;
    int modelCount = 0;
    
    dataModel.clear();
    
    File directory = m_Library.getWorkingDirectory();
    File subDirectories[] = directory.listFiles();
    
    if (subDirectories != null) {
      
      for (int i = 0; i < subDirectories.length; i++) {
	
	if (subDirectories[i].isDirectory()
	    && subDirectories[i].getName().matches(
	    ".*_instances_.*")) {
	  
	  directoryCount++;
	  
	  File[] subDirectoryFiles = subDirectories[i].listFiles();
	  
	  for (int j = 0; j < subDirectoryFiles.length; j++) {
	    
	    if (subDirectoryFiles[j].getName().matches(".*.elm")) {
	      
	      EnsembleSelectionLibraryModel model = EnsembleSelectionLibraryModel
	      .loadModel(subDirectoryFiles[j].getPath());
	      
	      // get those Classifier[] objects garbage collected!
	      model.releaseModel();
	      
	      if (!dataModel.contains(model)) {
		
		modelCount++;
		dataModel.add(model);
	      }
	    }
	  }
	}
      }
    }
    
    updateLoadingLabel(modelCount, directoryCount);
  }
  
  /**
   * updates the "directory" label
   */
  public void updateDirectoryLabel() {
    if (m_Library != null) {
      m_DirectoryLabel.setText("Directory: "
	  + m_Library.getWorkingDirectory());
    }
  }
  
  /**
   * updates the "loading" label
   * 
   * @param modelCount		the number of models found
   * @param directoryCount	the number of directories found
   */
  public void updateLoadingLabel(int modelCount, int directoryCount) {
    m_LoadingLabel.setText(" " + modelCount
	+ " unique model descriptions found in " + directoryCount
	+ " directories");
  }
  
  /**
   * This will support the button triggered events for this panel.
   * 
   * @param e		the event
   */
  public void actionPerformed(ActionEvent e) {
    
    ModelList.SortedListModel dataModel = ((ModelList.SortedListModel) m_ModelList.getModel());
    
    if (e.getSource() == m_RefreshListButton) {
      
      updateDirectoryLabel();
      loadModels();
      
    } else if (e.getSource() == m_RemoveSelectedButton) {
      
      // here we simply get the list of models that are
      // currently selected and ten remove them from the list
      
      Object[] currentModels = m_ModelList.getSelectedValues();
      
      for (int i = 0; i < currentModels.length; i++) {
	dataModel.removeElement(currentModels[i]);
      }
      
      // Shrink the selected range to the first index that was selected
      if (m_ModelList.getSelectedIndices().length > 0) {
	int selected[] = new int[1];
	selected[0] = m_ModelList.getSelectedIndices()[0];
	m_ModelList.setSelectedIndices(selected);
      }
      
    } else if (e.getSource() == m_AddAllButton) {
      
      // here we just need to add all of the models to the
      // ListModelsPanel object
      
      Iterator it = dataModel.iterator();
      
      while (it.hasNext()) {
	EnsembleLibraryModel currentModel = (EnsembleLibraryModel) it
	.next();
	
	m_ListModelsPanel.addModel(currentModel);
      }
      
      dataModel.clear();
      
    } else if (e.getSource() == m_AddSelectedButton) {
      
      // here we simply get the list of models that are
      // currently selected and ten remove them from the list
      
      Object[] currentModels = m_ModelList.getSelectedValues();
      
      for (int i = 0; i < currentModels.length; i++) {
	
	m_ListModelsPanel
	.addModel((EnsembleLibraryModel) currentModels[i]);
	dataModel.removeElement(currentModels[i]);
      }
    }
  }
  
  /**
   * This property change listener gets fired whenever the library's default
   * working directory changes. Here we check to see if the user has currently
   * selected the load models panel. If so then we immediately update the
   * list. Otherwise, we cache this event in the workingDirectoryChanged field
   * and reload the model list when the use switches back to this panel. This
   * is taken care of by the next state changed listener
   * 
   * @param evt		the event
   */
  public void propertyChange(PropertyChangeEvent evt) {
    
    if (m_EnsembleLibraryEditor.isLoadModelsPanelSelected()) {
      updateDirectoryLabel();
      loadModels();
    } else {
      m_workingDirectoryChanged = true;
    }
  }
  
  /**
   * this listener event fires when the use switches back to this panel it
   * checks to se if the working directory has changed since they were here
   * last. If so then it updates the model list.
   * 
   * @param e		the event
   */
  public void stateChanged(ChangeEvent e) {
    JTabbedPane pane = (JTabbedPane) e.getSource();
    
    if (pane.getSelectedComponent().equals(this)
	&& m_workingDirectoryChanged) {
      
      updateDirectoryLabel();
      loadModels();
      m_workingDirectoryChanged = false;
    }
  }
}
