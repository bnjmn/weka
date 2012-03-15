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
 *    EnsembleSelectionLibraryEditor.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui;

import weka.classifiers.meta.ensembleSelection.EnsembleSelectionLibrary;
import weka.gui.ensembleLibraryEditor.AddModelsPanel;
import weka.gui.ensembleLibraryEditor.DefaultModelsPanel;
import weka.gui.ensembleLibraryEditor.ListModelsPanel;
import weka.gui.ensembleLibraryEditor.LoadModelsPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Class for editing Ensemble Library objects. This basically 
 * does the same thing as its parent class except that it also
 * adds two extra panels.  A "loadModelsPanel" to detect model 
 * specifications from an ensemble working directory and a 
 * "defaltModelsPanel" which lets you choose sets of model specs 
 * from a few default lists.  Brings up a custom editing 
 * panel with which the user can edit the library interactively, as well 
 * as save load libraries from files.  
 *
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision$
 */
public class EnsembleSelectionLibraryEditor extends EnsembleLibraryEditor {
  
  /** An instance of the custom  editor */
  protected CustomEditor m_CustomEditor = new CustomEditor();
  
  /** the panel showing models in a workingDirectory */
  protected LoadModelsPanel m_LoadModelsPanel;
  
  /** The panel showing the defaults */
  protected DefaultModelsPanel m_DefaultModelsPanel;
  
  /**
   * This class presents a GUI for editing the library, and saving and 
   * loading model list from files.
   */
  private class CustomEditor 
    extends JPanel {
    
    /** for serialization */
    private static final long serialVersionUID = -3859973750432725901L;

    /**
     * Constructs a new CustomEditor.
     *
     */
    public CustomEditor() {
      
      m_ModelOptionsPane = new JTabbedPane();
      
      m_ListModelsPanel = new ListModelsPanel(m_Library);
      m_ModelOptionsPane.addTab("Library List", m_ListModelsPanel);
      
      //Each of the three other panels needs a reference to the main one so that 
      //they can add models to it.
      m_ModelOptionsPane.addTab("Add Models", new AddModelsPanel(
	  m_ListModelsPanel));
      
      m_DefaultModelsPanel = new DefaultModelsPanel(m_ListModelsPanel);
      m_ModelOptionsPane.addTab("Add Default Set", m_DefaultModelsPanel);
      m_ModelOptionsPane.addChangeListener(m_DefaultModelsPanel);
      
      m_LoadModelsPanel = new LoadModelsPanel(m_ListModelsPanel,
	  EnsembleSelectionLibraryEditor.this);
      m_ModelOptionsPane.addTab("Load Models", m_LoadModelsPanel);
      m_ModelOptionsPane.addChangeListener(m_LoadModelsPanel);
      
      setLayout(new BorderLayout());
      add(m_ModelOptionsPane, BorderLayout.CENTER);
      
      setPreferredSize(new Dimension(420, 500));
    }
  }
  
  /**
   * Constructs a new LibraryEditor.
   *
   */
  public EnsembleSelectionLibraryEditor() {
    super();
    
    m_CustomEditor = new CustomEditor();
  }
  
  /**
   * Sets the value of the Library to be edited.
   *
   * @param value a Library object to be edited
   */
  public void setValue(Object value) {
    super.setValue(value);
    m_LoadModelsPanel.setLibrary((EnsembleSelectionLibrary) m_Library);
    
    ((EnsembleSelectionLibrary) m_Library)
    .addWorkingDirectoryListener(m_LoadModelsPanel);
  }
  
  /**
   * Gets a GUI component with which the user can edit the cost matrix.
   *
   * @return an editor GUI component
   */
  public Component getCustomEditor() {
    return m_CustomEditor;
  }
  
  /**
   * returns whether or not the LoadModelsPanel is currently selected
   * 
   * @return true if selected
   */
  public boolean isLoadModelsPanelSelected() {
    return m_ModelOptionsPane.getSelectedComponent().equals(
	m_LoadModelsPanel);
  }
}
