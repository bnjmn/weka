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
 *    AddModelsPanel.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.EnsembleLibraryModel;
import weka.classifiers.trees.J48;
import weka.gui.GenericObjectEditor;
import weka.gui.ensembleLibraryEditor.tree.GenericObjectNode;
import weka.gui.ensembleLibraryEditor.tree.ModelTreeNodeEditor;
import weka.gui.ensembleLibraryEditor.tree.ModelTreeNodeRenderer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultTreeModel;

/**
 * The purpose of this class is to create a user interface that will
 * provide an intuitive method of building "playlists" of weka 
 * classifiers to be trained.  The main gui consists of two parts: the
 * top and bottom. The top will use a JTree to show all of the options
 * available for a particular classifier. The intent of showing these 
 * options in a tree is to allow users to collapse/expand tree nodes 
 * to quickly access all of the available options at different levels.
 * The bottom half of the gui will show the current "playlist" of 
 * chosen models that the user can opt to add to the current library.  
 * <p/>
 * The overall concept is that users will use the tree to specify 
 * combinations of options to the currently selected classifier type.
 * then they will use the "generate models" button to generate a set
 * models from the selected options. This can be done many times with
 * different sets of options for different model types to generate a
 * list in the bottom half of the gui.  Once the user is satisfied
 * with their list of models they are provided a button to "add all 
 * models" to the model library displayed by the ListModelsPanel.
 * <p/>
 * Note that there are currently 9 different classes that implement
 * tree nodes and tree node editors created to support this class
 * in modelling/rendering weka classifier parameters. see 
 * appropriate classes for details.  They currently reside in the 
 * weka.gui.libraryEditor.tree package.
 * <p/>
 * To instantiate the treeModel:
 * <ul>
 *   <li>ModelNodeEditor</li>
 *   <li>ModelNodeRenderer</li>
 * </ul>
 * 
 * To render/model weka objects:
 * <ul>
 *   <li>PropertyNode</li>
 *   <li>GenericObjectNode</li>
 *   <li>GenericObjectNodeEditor</li>
 *   <li>CheckBoxNode</li>
 *   <li>CheckBoxNodeEditor</li>
 *   <li>NumberNode</li>
 *   <li>NumberNodeEditor</li>
 *   <li>DefaultNode</li>
 * </ul>
 * 
 * These classes are responsible for 
 * representing the different kinds of tree nodes that will be 
 * contained in the JTree object, as well as the renderers and editors
 * that will be responsible for displaying their properties in the
 * user interface.  
 * <p/>
 * Code for this class was inspired and partly borrowed from the 
 * following excellent tutorial on creating custom JTree renderers 
 * and editors authored by John Zukowski: <br/>
 * <a href="http://www.java2s.com/ExampleCode/Swing-JFC/CheckBoxNodeTreeSample.htm" target="_blank">http://www.java2s.com/ExampleCode/Swing-JFC/CheckBoxNodeTreeSample.htm</a>
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision$
 */
public class AddModelsPanel 
  extends JPanel
  implements ActionListener {
  
  /** for serialization */
  private static final long serialVersionUID = 4874639416371962573L;

  /**
   * This is a reference to the main gui object that is responsible 
   * for displaying the model library.  This panel will add models
   * to the main panel through methods in this object.
   */
  private ListModelsPanel m_ListModelsPanel;
  
  /**
   * The JTree that will display the classifier options available in
   * the currently select3ed model type
   */
  private JTree m_Tree;
  
  /**
   * The tree model that will be used to add and remove nodes from 
   * the currently selected model type
   */
  private DefaultTreeModel m_TreeModel;
  
  /**
   * This button will allow users to generate a group of models from
   * the currently selected classifer options in the m_Tree object.
   */
  private JButton m_GenerateButton;
  
  /**
   * This will display messages associated with model generation.
   * Currently the number of models generated and the number of
   * them that had errors.
   */
  private JLabel m_GenerateLabel;
  
  /**
   * This button will allow users to remove all of the models 
   * currently selected in the m_ModeList object
   */
  private JButton m_RemoveSelectedButton;
  
  /**
   * This button will remove all of the models that had errors 
   * during model generation.
   */
  private JButton m_RemoveInvalidButton;
  
  /**
   * This button will add all of the models that had are 
   * currently selected in the model list.
   */
  private JButton m_AddSelectedButton;
  
  /**
   * This button will allow users to add all models currently in 
   * the model list to the model library in the ListModelsPanel. 
   * Note that this operation will exclude any models that had 
   * errors
   */
  private JButton m_AddAllButton;
  
  /**
   * This object will store all of the model sets generated from the
   * m_Tree.  The ModelList class is a custom class in weka.gui that
   * knows how to display library model objects in a JList
   */
  private ModelList m_ModelList;
  
  /** the scroll pane holding our classifer parameters */
  JScrollPane m_TreeView;
  
  /**
   * This constructor simply stores the reference to the 
   * ListModelsPanel and builf the user interface.
   * 
   * @param listModelsPanel	the reference to the panel
   */
  public AddModelsPanel(ListModelsPanel listModelsPanel) {
    m_ListModelsPanel = listModelsPanel;
    
    createAddModelsPanel();
  }
  
  /**
   * This method is responsible for building the use interface.
   */
  private void createAddModelsPanel() {
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(new GridBagLayout());
    
    m_TreeView = new JScrollPane();
    m_TreeView.setPreferredSize(new Dimension(150, 50));
    
    buildClassifierTree(new J48());
    
    ToolTipManager.sharedInstance().registerComponent(m_Tree);
    
    gbc.weightx = 1;
    gbc.weighty = 1.5;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 3;
    gbc.anchor = GridBagConstraints.WEST;
    add(m_TreeView, gbc);
    
    m_GenerateButton = new JButton("Generate Models");
    m_GenerateButton.setToolTipText(
	"Generate a set of models from options specified in options tree");
    m_GenerateButton.addActionListener(this);
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    add(m_GenerateButton, gbc);
    
    m_GenerateLabel = new JLabel("");
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 2;
    add(m_GenerateLabel, gbc);
    
    m_RemoveInvalidButton = new JButton("Remove Invalid");
    m_RemoveInvalidButton.setToolTipText(
	"Remove all invalid (red) models from the above list");
    m_RemoveInvalidButton.addActionListener(this);
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    //OK, this button was removed because we thought it was a waste
    //of space.  Instead of removing invalid models, we just explicitly
    //prevent the user from adding them to the main list.  I'm going to
    //leave the code in with this final "add" statement commented out
    //because we are still on the fence as to whether this is a good 
    //idea
    //add(m_RemoveInvalidButton, gbc);
    
    m_ModelList = new ModelList();
    
    m_ModelList.getInputMap().put(
	KeyStroke.getKeyStroke("released DELETE"), "deleteSelected");
    m_ModelList.getActionMap().put("deleteSelected",
	new AbstractAction("deleteSelected") {
      /** for serialization */
      private static final long serialVersionUID = -3351194234735560372L;
      
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
    
    m_ModelList
    .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    m_ModelList.setLayoutOrientation(JList.VERTICAL);
    m_ModelList.setVisibleRowCount(-1);
    
    JPanel modelListPanel = new JPanel();
    modelListPanel.setBorder(
	BorderFactory.createTitledBorder("Working Set of Newly Generated Models"));
    
    JScrollPane listView = new JScrollPane(m_ModelList);
    //listView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
    m_RemoveSelectedButton.setToolTipText("Remove all currently selected models from the above list");
    m_RemoveSelectedButton.addActionListener(this);
    gbc.weightx = 1;
    gbc.weighty = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 3;
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
    gbc.gridy = 3;
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
    gbc.gridy = 3;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    add(m_AddAllButton, gbc);
  }
  
  /**
   * This method necessarily seperates the process of building the
   * tree object from the rest of the GUI construction.  In order to
   * prevent all kinds of strange garbage collection problems, we take
   * the conservative approach of gutting and rebuilding the JTree 
   * every time a new classifier is chosen for the root node.
   * 
   * @param classifier	the classifier to build the tree for
   */
  public void buildClassifierTree(Classifier classifier) {
    
    //This block sets up the root node of the tree.  Note that 
    //the constructor for the GenericObjectNode will take care
    //of creating all of the child nodes containing the node
    //properties
    GenericObjectEditor classifierEditor = new GenericObjectEditor();
    classifierEditor.setClassType(Classifier.class);
    classifierEditor.setValue(classifier);
    
    GenericObjectNode rootNode = new GenericObjectNode(this, classifier,
	classifierEditor, "Current Classifier");
    
    m_TreeModel = new DefaultTreeModel(rootNode);
    m_Tree = new JTree(m_TreeModel);
    rootNode.setTree(m_Tree);
    rootNode.updateTree();
    
    m_Tree.setRootVisible(true);
    
    ModelTreeNodeRenderer renderer = new ModelTreeNodeRenderer();
    m_Tree.setCellRenderer(renderer);
    m_Tree.setCellEditor(new ModelTreeNodeEditor(m_Tree));
    m_Tree.setEditable(true);
    m_Tree.setVisibleRowCount(8);
    //ToolTipManager.sharedInstance().registerComponent(m_Tree);
    
    //This "tentatively seems to work better:
    m_Tree.setRowHeight(0);
    
    m_TreeView.setViewportView(m_Tree);
  }
  
  /**
   * This will support the button triggered events for this panel.
   * 
   * @param e	the event
   */
  public void actionPerformed(ActionEvent e) {
    
    ModelList.SortedListModel dataModel = ((ModelList.SortedListModel) m_ModelList.getModel());
    
    if (e.getSource() == m_GenerateButton) {
      
      //here we want to generate all permutations of the 
      //options specified and then add each of them to the 
      //model list panel
      
      Vector models = ((GenericObjectNode) m_TreeModel.getRoot()).getValues();
      
      int total = models.size();
      int invalid = 0;
      
      for (int i = 0; i < models.size(); i++) {
	Classifier classifier = (Classifier) models.get(i);
	
	//This method will invoke the classifier's setOptions
	//method to see if the current set of options was 
	//valid.  
	
	EnsembleLibraryModel model = m_ListModelsPanel.getLibrary().createModel(classifier);
	
	model.testOptions();
	
	if (!model.getOptionsWereValid())
	  invalid++;
	
	dataModel.add(model);
      }
      
      //update the message text with model generation info
      String generateString = new String("  " + total
	  + " models generated");
      generateString += ", " + invalid + " had errors";
      m_GenerateLabel.setText(generateString);
      
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
      
    } else if (e.getSource() == m_RemoveInvalidButton) {
      
      //here we simply remove all the models that were not
      //valid
      
      Vector toRemove = new Vector();
      
      for (int i = 0; i < dataModel.getSize(); i++) {
	
	EnsembleLibraryModel currentModel = (EnsembleLibraryModel) dataModel.getElementAt(i);
	if (!currentModel.getOptionsWereValid()) {
	  toRemove.add(currentModel);
	}
      }
      
      for (int i = 0; i < toRemove.size(); i++)
	dataModel.removeElement(toRemove.get(i));
      
    } else if (e.getSource() == m_AddAllButton) {
      
      //here we just need to add all of the models to the 
      //ListModelsPanel object
      
      Iterator it = dataModel.iterator();
      
      while (it.hasNext()) {
	EnsembleLibraryModel currentModel = (EnsembleLibraryModel) it.next();
	if (currentModel.getOptionsWereValid()) {
	  m_ListModelsPanel.addModel(currentModel);
	}
      }
      
      int size = dataModel.getSize();
      
      for (int i = 0; i < size; i++) {
	dataModel.removeElement(dataModel.getElementAt(0));
      }
    }
  }
}
