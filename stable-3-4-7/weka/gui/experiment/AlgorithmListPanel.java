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
 *    AlgorithmListPanel.java
 *    Copyright (C) 2002 Richard Kirkby
 *
 */


package weka.gui.experiment;

import weka.core.OptionHandler;
import weka.core.Utils;

import weka.classifiers.Classifier;

import weka.gui.ExtensionFileFilter;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyDialog;

import weka.classifiers.xml.XMLClassifier;
import weka.core.Instances;
import weka.core.SerializedObject;
import weka.experiment.Experiment;

import java.util.Vector;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.DefaultListCellRenderer;
import javax.swing.filechooser.FileFilter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;
import java.io.File;

/** 
 * This panel controls setting a list of algorithms for an experiment to
 * iterate over.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.7.2.5 $
 */
public class AlgorithmListPanel extends JPanel implements ActionListener {

  /* Class required to show the Classifiers nicely in the list */
  public class ObjectCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list,
						  Object value,
						  int index,
						  boolean isSelected,
						  boolean cellHasFocus) {

      Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      String rep = value.getClass().getName();
      int dotPos = rep.lastIndexOf('.');
      if (dotPos != -1) {
	rep = rep.substring(dotPos + 1);
      }
      if (value instanceof OptionHandler) {
	rep += " " + Utils.joinOptions(((OptionHandler)value)
				       .getOptions());
      }
      setText(rep);
      return c;
    }
  }
  
  /** The experiment to set the algorithm list of */
  protected Experiment m_Exp;

  /** The component displaying the algorithm list */
  protected JList m_List;

  /** Click to add an algorithm */
  protected JButton m_AddBut = new JButton("Add new...");
  
  /** Click to edit the selected algorithm */
  protected JButton m_EditBut = new JButton("Edit selected...");

  /** Click to remove the selected dataset from the list */
  protected JButton m_DeleteBut = new JButton("Delete selected");
  
  /** Click to edit the load the options for athe selected algorithm */
  protected JButton m_LoadOptionsBut = new JButton("Load options...");
  
  /** Click to edit the save the options from selected algorithm */
  protected JButton m_SaveOptionsBut = new JButton("Save options...");
  
  /** The file chooser for selecting experiments */
  protected JFileChooser m_FileChooser =
    new JFileChooser(new File(System.getProperty("user.dir")));

  /** A filter to ensure only experiment (in XML format) files get shown in the chooser */
  protected FileFilter m_XMLFilter = 
    new ExtensionFileFilter(".xml", 
                            "Experiment configuration files (*.xml)");

  /** Whether an algorithm is added or only edited  */
  protected boolean m_Editing = false;
  
  /** Lets the user configure the classifier */
  protected GenericObjectEditor m_ClassifierEditor =
    new GenericObjectEditor(true);

  /** The currently displayed property dialog, if any */
  protected PropertyDialog m_PD;

  /** The list model used */
  protected DefaultListModel m_AlgorithmListModel = new DefaultListModel();

  /* Register the property editors we need */
  static {
    java.beans.PropertyEditorManager
      .registerEditor(weka.core.SelectedTag.class,
		      weka.gui.SelectedTagEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.filters.Filter.class,
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
  }

  /**
   * Creates the algorithm list panel with the given experiment.
   *
   * @param exp a value of type 'Experiment'
   */
  public AlgorithmListPanel(Experiment exp) {

    this();
    setExperiment(exp);
  }

  /**
   * Create the algorithm list panel initially disabled.
   */
  public AlgorithmListPanel() {
    
    m_List = new JList();
  
    m_ClassifierEditor.setClassType(Classifier.class);
    m_ClassifierEditor.setValue(new weka.classifiers.rules.ZeroR());
    m_ClassifierEditor.addPropertyChangeListener(new PropertyChangeListener() {
	public void propertyChange(PropertyChangeEvent e) {
	  repaint();
	}
      });
    ((GenericObjectEditor.GOEPanel) m_ClassifierEditor.getCustomEditor()).addOkListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  Classifier newCopy =
	    (Classifier) copyObject(m_ClassifierEditor.getValue());
	  addNewAlgorithm(newCopy);
	}
      });
    
    m_DeleteBut.setEnabled(false);
    m_DeleteBut.addActionListener(this);
    m_AddBut.setEnabled(false);
    m_AddBut.addActionListener(this);
    m_EditBut.setEnabled(false);
    m_EditBut.addActionListener(this);
    m_LoadOptionsBut.setEnabled(false);
    m_LoadOptionsBut.addActionListener(this);
    m_SaveOptionsBut.setEnabled(false);
    m_SaveOptionsBut.addActionListener(this);
    
    m_List.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          setButtons(e);
        }
      });
    
    m_FileChooser.addChoosableFileFilter(m_XMLFilter);
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    setLayout(new BorderLayout());
    setBorder(BorderFactory.createTitledBorder("Algorithms"));
    JPanel topLab = new JPanel();
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints constraints = new GridBagConstraints();
    topLab.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    topLab.setLayout(gb);
   
    constraints.gridx=0;constraints.gridy=0;constraints.weightx=5;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridwidth=1;constraints.gridheight=1;
    constraints.insets = new Insets(0,2,0,2);
    topLab.add(m_AddBut,constraints);
    constraints.gridx=1;constraints.gridy=0;constraints.weightx=5;
    constraints.gridwidth=1;constraints.gridheight=1;
    topLab.add(m_EditBut,constraints);
    constraints.gridx=2;constraints.gridy=0;constraints.weightx=5;
    constraints.gridwidth=1;constraints.gridheight=1;
    topLab.add(m_DeleteBut,constraints);

    JPanel bottomLab = new JPanel();
    gb = new GridBagLayout();
    constraints = new GridBagConstraints();
    bottomLab.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    bottomLab.setLayout(gb);
    
    constraints.gridx=0;constraints.gridy=0;constraints.weightx=5;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridwidth=1;constraints.gridheight=1;
    constraints.insets = new Insets(0,2,0,2);
    bottomLab.add(m_LoadOptionsBut,constraints);
    constraints.gridx=1;constraints.gridy=0;constraints.weightx=5;
    constraints.gridwidth=1;constraints.gridheight=1;
    bottomLab.add(m_SaveOptionsBut,constraints);

    add(topLab, BorderLayout.NORTH);
    add(new JScrollPane(m_List), BorderLayout.CENTER);
    add(bottomLab, BorderLayout.SOUTH);
  }

  /**
   * Tells the panel to act on a new experiment.
   *
   * @param exp a value of type 'Experiment'
   */
  public void setExperiment(Experiment exp) {

    m_Exp = exp;
    m_AddBut.setEnabled(true);
    m_List.setModel(m_AlgorithmListModel);
    m_List.setCellRenderer(new ObjectCellRenderer());
    m_AlgorithmListModel.removeAllElements();
    if (m_Exp.getPropertyArray() instanceof Classifier[]) {
      Classifier[] algorithms = (Classifier[]) m_Exp.getPropertyArray();
      for (int i=0; i<algorithms.length; i++) {
	m_AlgorithmListModel.addElement(algorithms[i]);
      }
    }
    m_EditBut.setEnabled((m_AlgorithmListModel.size() > 0));
    m_DeleteBut.setEnabled((m_AlgorithmListModel.size() > 0));
    m_LoadOptionsBut.setEnabled((m_AlgorithmListModel.size() > 0));
    m_SaveOptionsBut.setEnabled((m_AlgorithmListModel.size() > 0));
  }

  /**
   * Add a new algorithm to the list.
   */
  private void addNewAlgorithm(Classifier newScheme) {

    if (!m_Editing)
      m_AlgorithmListModel.addElement(newScheme);
    else
      m_AlgorithmListModel.setElementAt(newScheme, m_List.getSelectedIndex());
    Classifier[] cArray = new Classifier[m_AlgorithmListModel.size()];
    for (int i=0; i<cArray.length; i++) {
      cArray[i] = (Classifier) m_AlgorithmListModel.elementAt(i);
    }
    m_Exp.setPropertyArray(cArray); 
    m_Editing = false;
  }
  
  /**
   * sets the state of the buttons according to the selection state of the
   * JList
   */
  private void setButtons(ListSelectionEvent e) {
    if (e.getSource() == m_List) {
      m_DeleteBut.setEnabled(m_List.getSelectedIndex() > -1);
      m_AddBut.setEnabled(true);
      m_EditBut.setEnabled(m_List.getSelectedIndices().length == 1);
      m_LoadOptionsBut.setEnabled(m_List.getSelectedIndices().length == 1);
      m_SaveOptionsBut.setEnabled(m_List.getSelectedIndices().length == 1);
    }
  }

  /**
   * Handle actions when buttons get pressed.
   *
   * @param e a value of type 'ActionEvent'
   */
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == m_AddBut) {
      m_Editing = false;
      if (m_PD == null) {
	int x = getLocationOnScreen().x;
	int y = getLocationOnScreen().y;
	m_PD = new PropertyDialog(m_ClassifierEditor, x, y);
      } else {
	m_PD.setVisible(true);
      }
      
    } else if (e.getSource() == m_EditBut) {
      if (m_List.getSelectedValue() != null) {
         m_Editing = true;
         if (m_PD == null) {
            int x = getLocationOnScreen().x;
            int y = getLocationOnScreen().y;
            m_PD = new PropertyDialog(m_ClassifierEditor, x, y);
         } else {
            m_PD.setVisible(true);
         }
         m_PD.getEditor().setValue(m_List.getSelectedValue());
      }

    } else if (e.getSource() == m_DeleteBut) {

      int [] selected = m_List.getSelectedIndices();
      if (selected != null) {
	for (int i = selected.length - 1; i >= 0; i--) {
	  int current = selected[i];
	  m_AlgorithmListModel.removeElementAt(current);
	  if (m_Exp.getDatasets().size() > current) {
	    m_List.setSelectedIndex(current);
	  } else {
	    m_List.setSelectedIndex(current - 1);
	  }
	}
      }
      if (m_List.getSelectedIndex() == -1) {
        m_EditBut.setEnabled(false);
        m_DeleteBut.setEnabled(false);
        m_LoadOptionsBut.setEnabled(false);
        m_SaveOptionsBut.setEnabled(false);
      }

      Classifier[] cArray = new Classifier[m_AlgorithmListModel.size()];
      for (int i=0; i<cArray.length; i++) {
	cArray[i] = (Classifier) m_AlgorithmListModel.elementAt(i);
      }
      m_Exp.setPropertyArray(cArray); 
    } else if (e.getSource() == m_LoadOptionsBut) {
      if (m_List.getSelectedValue() != null) {
        int returnVal = m_FileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          try {
            File file = m_FileChooser.getSelectedFile();
            if (!file.getAbsolutePath().toLowerCase().endsWith(".xml"))
              file = new File(file.getAbsolutePath() + ".xml");
            XMLClassifier xmlcls = new XMLClassifier();
            Classifier c = (Classifier) xmlcls.read(file);
            m_AlgorithmListModel.setElementAt(c, m_List.getSelectedIndex());
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      }
   } else if (e.getSource() == m_SaveOptionsBut) {
      if (m_List.getSelectedValue() != null) {
        int returnVal = m_FileChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          try {
            File file = m_FileChooser.getSelectedFile();
            if (!file.getAbsolutePath().toLowerCase().endsWith(".xml"))
              file = new File(file.getAbsolutePath() + ".xml");
            XMLClassifier xmlcls = new XMLClassifier();
            xmlcls.write(file, m_List.getSelectedValue());
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * Makes a copy of an object using serialization
   * @param source the object to copy
   * @return a copy of the source object
   */
  protected Object copyObject(Object source) {
    
    Object result = null;
    try {
      SerializedObject so = new SerializedObject(source);
      result = so.getObject();
    } catch (Exception ex) {
      System.err.println("AlgorithmListPanel: Problem copying object");
      System.err.println(ex);
    }
    return result;
  }
  
  /**
   * Tests out the algorithm list panel from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      final JFrame jf = new JFrame("Algorithm List Editor");
      jf.getContentPane().setLayout(new BorderLayout());
      AlgorithmListPanel dp = new AlgorithmListPanel();
      jf.getContentPane().add(dp,
			      BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
      System.err.println("Short nap");
      Thread.currentThread().sleep(3000);
      System.err.println("Done");
      dp.setExperiment(new Experiment());
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
