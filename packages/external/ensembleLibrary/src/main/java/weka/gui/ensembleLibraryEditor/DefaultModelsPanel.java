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
 *    DefaultModelsPanel.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.EnsembleLibraryModel;
import weka.classifiers.meta.ensembleSelection.EnsembleSelectionLibrary;
import weka.core.Utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This class is intended to build a panel that contains as interface 
 * that will let users choose default lists of models to add to the 
 * library. There will be default a list of models provided by the 
 * EnsembleLibrary class.  In addition, the user will be able to prune
 * the list of defaults to remove models that have either high training
 * times, high testing times, or high file sizes on disk.  Finally, users
 * will be able to also prune the size of the current working default set
 * to a specific number of models with a slider bar.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision$
 */
public class DefaultModelsPanel 
  extends JPanel 
  implements ActionListener, ChangeListener {
  
  /** for serialization */
  private static final long serialVersionUID = -6123488873592563339L;

  /** the name of the property file */
  public static final String PROPERTY_FILE = "DefaultModels.props";
  
  /** Contains the editor properties */
  protected static Properties DEFAULT_PROPERTIES;
  
  /** options to exclude */
  public String EXCLUDE_OPTIONS[] = { "Train Time", "Test Time", "File Size" };
  
  /** an array of libray files to be used in populating
   * the default list comboBox*/
  private String[] m_DefaultFileNames;
  
  /** an array of model Strings that should be excluded 
   * if the file size option is selected*/
  private String[] m_LargeFileSizeModels;
  
  /** an array of model Strings that should be excluded 
   * if the train time option is selected*/
  private String[] m_LargeTrainTimeModels;
  
  /** an array of model Strings that should be excluded 
   * if the test time option is selected*/
  private String[] m_LargeTestTimeModels;
  
  /** this is a combo box that will allow the user to select
   * which set of models to remove from the list */
  private JComboBox m_ExcludeModelsComboBox;
  
  /** this is a button that will allow the user to select
   * which set of models to remove from the list */
  private JButton m_ExcludeModelsButton;
  
  /** this is a combo box that will allow the user to select
   * the default model file */
  private JComboBox m_DefaultFilesComboBox;
  
  /** allows the user to reload the default set */
  private JButton m_RefreshButton;
  
  /**
   * This object will store all of the models that can be selected
   * from the default list.  The ModelList class is 
   * a custom class in weka.gui that knows how to display library 
   * model objects in a JList
   */
  private ModelList m_ModelList;
  
  /** This button allows the user to remove all the models currently
   * selected in the model List from those that will be added*/
  private JButton m_RemoveSelectedButton;
  
  /** This button allows the user to add all the in the current 
   * working default set to the library */
  private JButton m_AddAllButton;
  
  /** This button allows the user to add all the models currently
   * selected to the current set of models in this library, after 
   * this is selected it should also send the user back to the 
   * main interface*/
  private JButton m_AddSelectedButton;
  
  /**
   * This is a reference to the main gui object that is responsible 
   * for displaying the model library.  This panel will add models
   * to the main panel through methods in this object.
   */
  private ListModelsPanel m_ListModelsPanel;
  
  /** whether an update is pending */
  private boolean m_ListUpdatePending = false;
  
  /** 
   * Loads the configuration property file 
   */
  static {
    
    try {
      System.out.println("package name: " + getPackageName());
      
      DEFAULT_PROPERTIES = Utils.readProperties(getPackageName()
						+ PROPERTY_FILE, DefaultModelsPanel.class.getClassLoader());
      java.util.Enumeration keys = (java.util.Enumeration) DEFAULT_PROPERTIES
      .propertyNames();
      if (!keys.hasMoreElements()) {
	throw new Exception("Failed to read a property file for the "
	    + "generic object editor");
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null,
	  "Could not read a configuration file for the default models\n"
	  + "panel.\n", "DefaultProperties",
	  JOptionPane.ERROR_MESSAGE);
    }
    
  }
  
  /**
   * Constructor to initialize the GUI
   * 
   * @param listModelsPanel	the panel to use
   */
  public DefaultModelsPanel(ListModelsPanel listModelsPanel) {
    m_ListModelsPanel = listModelsPanel;
    
    readProperties();
    
    createDefaultModelsPanel();
  }
  
  /**
   * This grabs the relevant properties from the Default model
   * properties file.
   *
   */
  private void readProperties() {
    
    m_DefaultFileNames = DEFAULT_PROPERTIES.getProperty("files").split(", ");
    
    m_LargeTrainTimeModels = DEFAULT_PROPERTIES.getProperty("train_time").split(", ");
    
    m_LargeTestTimeModels = DEFAULT_PROPERTIES.getProperty("test_time").split(", ");
    
    m_LargeFileSizeModels = DEFAULT_PROPERTIES.getProperty("file_size").split(", ");
  }
  
  /**
   * Initializes the GUI
   *
   */
  private void createDefaultModelsPanel() {
    
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    JLabel defaultFileLabel = new JLabel("Select default set: ");
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;
    add(defaultFileLabel, gbc);
    
    m_DefaultFilesComboBox = new JComboBox(m_DefaultFileNames);
    m_DefaultFilesComboBox.setSelectedItem(m_DefaultFileNames[0]);
    m_DefaultFilesComboBox.addActionListener(this);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;
    add(m_DefaultFilesComboBox, gbc);
    
    m_RefreshButton = new JButton("Reload set");
    m_RefreshButton.addActionListener(this);
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;
    add(m_RefreshButton, gbc);
    
    JLabel excludeModelsLabel = new JLabel("Exclude models w/ large");
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;
    add(excludeModelsLabel, gbc);
    
    m_ExcludeModelsComboBox = new JComboBox(EXCLUDE_OPTIONS);
    m_ExcludeModelsComboBox.setSelectedItem(EXCLUDE_OPTIONS[0]);
    //m_ExcludeModelsComboBox.addActionListener(this);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;
    add(m_ExcludeModelsComboBox, gbc);
    
    m_ExcludeModelsButton = new JButton("Exclude");
    m_ExcludeModelsButton.setToolTipText(
	"Exclude this type of models from the current working list");
    m_ExcludeModelsButton.addActionListener(this);
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;
    add(m_ExcludeModelsButton, gbc);
    
    JPanel modelListPanel = new JPanel();
    modelListPanel.setBorder(BorderFactory
	.createTitledBorder("Working set of Default Library Models"));
    
    m_ModelList = new ModelList();
    
    m_ModelList.getInputMap().put(
	KeyStroke.getKeyStroke("released DELETE"), "deleteSelected");
    m_ModelList.getActionMap().put("deleteSelected",
	new AbstractAction("deleteSelected") {
      
      private static final long serialVersionUID = 4601977182190493654L;
      
      public void actionPerformed(ActionEvent evt) {
	
	Object[] currentModels = m_ModelList.getSelectedValues();
	
	ModelList.SortedListModel dataModel = ((ModelList.SortedListModel) m_ModelList.getModel());
	
	for (int i = 0; i < currentModels.length; i++) {
	  dataModel.removeElement((EnsembleLibraryModel) currentModels[i]);
	}
	
	//Shrink the selected range to the first index that was selected
	int selected[] = new int[1];
	selected[0] = m_ModelList.getSelectedIndices()[0];
	m_ModelList.setSelectedIndices(selected);
	
      }
    });
    
    m_ModelList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    m_ModelList.setLayoutOrientation(JList.VERTICAL);
    //m_ModelList.setVisibleRowCount(12);
    
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
    gbc.gridy = 2;
    gbc.gridwidth = 3;
    gbc.anchor = GridBagConstraints.WEST;
    add(modelListPanel, gbc);
    
    m_RemoveSelectedButton = new JButton("Remove Selected");
    m_RemoveSelectedButton.addActionListener(this);
    m_RemoveSelectedButton.setToolTipText(
	"Remove the selected models from the current default set");
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0;
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 1;
    gbc.gridwidth = 1;
    add(m_RemoveSelectedButton, gbc);
    
    m_AddSelectedButton = new JButton("Add Selected");
    m_AddSelectedButton.addActionListener(this);
    m_AddSelectedButton.setToolTipText("Add selected models in "
	+ "above list to the library");
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    gbc.weightx = 1;
    add(m_AddSelectedButton, gbc);
    
    m_AddAllButton = new JButton("Add All");
    m_AddAllButton.addActionListener(this);
    m_AddAllButton.setToolTipText("Add all models in the above"
	+ "list to the Libray");
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 2;
    gbc.gridy = 3;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    gbc.weightx = 1;
    add(m_AddAllButton, gbc);
    
    m_ListUpdatePending = true;
    
  }
  
  /**
   * This method is called in response to user actions prompting
   * us to reload the model list: when they select a new list, 
   * or hit reload.
   *
   */
  public void updateDefaultList() {
    
    ((ModelList.SortedListModel) m_ModelList.getModel()).clear();
    
    String ensemblePackageString = getPackageName();
    
    int index = m_DefaultFilesComboBox.getSelectedIndex();
    
    Vector classifiers = null;
    
    LibrarySerialization serialization;
    try {
      
      serialization = new LibrarySerialization();
      
      String defaultFileString = ensemblePackageString
      + m_DefaultFileNames[index].trim() + ".model.xml";
      
      //System.out.println(defaultFileString);
    
      ClassLoader thisLoader = getClass().getClassLoader();
      // InputStream is = ClassLoader.getSystemResourceAsStream(defaultFileString);
      InputStream is = thisLoader.getResourceAsStream(defaultFileString);
      
      if (is == null) {
	File f = new File(defaultFileString);
	if (f.exists()) {
	  System.out.println("file existed: " + f.getPath());
	} else {
	  System.out.println("file didn't exist: " + f.getPath());
	}
	
      }
      
      // classifiers = (Vector) serialization.read(ClassLoader.getSystemResourceAsStream(defaultFileString));
      classifiers = (Vector) serialization.read(thisLoader.getResourceAsStream(defaultFileString));
      
      for (Iterator it = classifiers.iterator(); it.hasNext();) {
	EnsembleLibraryModel model = m_ListModelsPanel.getLibrary().createModel((Classifier) it.next());
	model.testOptions();
	((ModelList.SortedListModel) m_ModelList.getModel()).add(model);
      }
      
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  /**
   * Deals with user input to the various buttons in this GUI
   * 
   * @param e		the event
   */
  public void actionPerformed(ActionEvent e) {
    
    ModelList.SortedListModel dataModel = ((ModelList.SortedListModel) m_ModelList
	.getModel());
    
    if (e.getSource() == m_RefreshButton) {
      updateDefaultList();
      
    } else if (e.getSource() == m_DefaultFilesComboBox) {
      updateDefaultList();
      
    } else if (e.getSource() == m_RemoveSelectedButton) {
      
      //here we simply get the list of models that are 
      //currently selected and ten remove them from the list
      
      Object[] currentModels = m_ModelList.getSelectedValues();
      
      for (int i = 0; i < currentModels.length; i++) {
	dataModel.removeElement(currentModels[i]);
      }
      
      //Shrink the selected range to the first index that was selected
      if (m_ModelList.getSelectedIndices().length > 0) {
	int selected[] = new int[1];
	selected[0] = m_ModelList.getSelectedIndices()[0];
	m_ModelList.setSelectedIndices(selected);
      }
      
    } else if (e.getSource() == m_AddAllButton) {
      
      //here we just need to add all of the models to the 
      //ListModelsPanel object
      
      Iterator it = dataModel.iterator();
      
      while (it.hasNext()) {
	EnsembleLibraryModel currentModel = (EnsembleLibraryModel) it.next();
	m_ListModelsPanel.addModel(currentModel);
      }
      
      dataModel.clear();
      
    } else if (e.getSource() == m_AddSelectedButton) {
      
      //here we simply get the list of models that are 
      //currently selected and ten remove them from the list
      
      Object[] currentModels = m_ModelList.getSelectedValues();
      
      for (int i = 0; i < currentModels.length; i++) {
	
	m_ListModelsPanel.addModel((EnsembleLibraryModel) currentModels[i]);
	dataModel.removeElement(currentModels[i]);
      }
    } else if (e.getSource() == m_ExcludeModelsButton) {
      
      if (m_ExcludeModelsComboBox.getSelectedIndex() == 0) {
	applyTrainTimeFilters();
      } else if (m_ExcludeModelsComboBox.getSelectedIndex() == 1) {
	applyTestTimeFilters();
      } else if (m_ExcludeModelsComboBox.getSelectedIndex() == 2) {
	applyFileSizeFilters();
      }
    }
  }
  
  /**
   * this listener event fires when the use switches back to this panel
   * it checks to se if the working directory has changed since they were
   * here last.  If so then it updates the model list.
   * 
   * @param e		the event
   */
  public void stateChanged(ChangeEvent e) {
    JTabbedPane pane = (JTabbedPane) e.getSource();
    
    if (pane.getSelectedComponent().equals(this) && m_ListUpdatePending) {
      
      updateDefaultList();
      m_ListUpdatePending = false;
    }
  }
  
  /**
   * Removes models from the list that fit the regular expressions
   * defining models that have large train times
   */
  public void applyTrainTimeFilters() {
    
    //create an array of patterns and then pass them to the apply 
    //filters method
    Pattern patterns[] = new Pattern[m_LargeTrainTimeModels.length];
    
    for (int i = 0; i < m_LargeTrainTimeModels.length; i++) {
      patterns[i] = Pattern.compile(m_LargeTrainTimeModels[i]);
    }
    
    applyFilters(patterns);
  }
  
  /**
   * Removes models from the list that fit the regular expressions
   * defining models that have large test times
   */
  public void applyTestTimeFilters() {
    
    //create an array of patterns and then pass them to the apply 
    //filters method
    Pattern patterns[] = new Pattern[m_LargeTestTimeModels.length];
    
    for (int i = 0; i < m_LargeTestTimeModels.length; i++) {
      patterns[i] = Pattern.compile(m_LargeTestTimeModels[i]);
    }
    
    applyFilters(patterns);
  }
  
  /**
   * Removes models from the list that fit the regular expressions
   * defining models that have large file sizes
   */
  public void applyFileSizeFilters() {
    
    //create an array of patterns and then pass them to the apply 
    //filters method
    Pattern patterns[] = new Pattern[m_LargeFileSizeModels.length];
    
    for (int i = 0; i < m_LargeFileSizeModels.length; i++) {
      patterns[i] = Pattern.compile(m_LargeFileSizeModels[i]);
    }
    
    applyFilters(patterns);
  }
  
  /**
   * This is the code common to the previous three methods.  It basically 
   * takes a Java regexp pattern and applies it to the currently selected 
   * list of models, removing those that match as it goes.  
   *
   * @param patterns	the regexp patterns
   */
  public void applyFilters(Pattern[] patterns) {
    
    ModelList.SortedListModel dataModel = ((ModelList.SortedListModel) m_ModelList
	.getModel());
    
    //this will track all of the models that we need to remove
    //from our list
    Vector toRemove = new Vector();
    
    //here we simply get the list of models that are 
    //currently selected and ten remove them from the list
    //if they match one of the patterns
    
    Iterator it = dataModel.iterator();
    
    while (it.hasNext()) {
      EnsembleLibraryModel currentModel = (EnsembleLibraryModel) it.next();
      
      for (int i = 0; i < patterns.length; i++) {
	if (patterns[i].matcher(currentModel.getStringRepresentation()).matches()) {
	  toRemove.add(currentModel);
	  break;
	}
      }
    }
    
    it = toRemove.iterator();
    
    while (it.hasNext()) {
      dataModel.removeElement(it.next());
    }
  }
  
  /**
   * this bit of code grabs all of the .model.xml files located in
   * the ensemble selection package directory.  I decided to detect this *
   * directory in case we change the package name which I think we planned
   * on doing.  
   * 
   * @return		the package name
   */
  public static String getPackageName() {
    return 
       EnsembleSelectionLibrary.class.toString()
      .replaceAll("class ", "")
      .replaceAll("EnsembleSelectionLibrary", "")
      .replaceAll("\\.", "/");
  }
  
}
