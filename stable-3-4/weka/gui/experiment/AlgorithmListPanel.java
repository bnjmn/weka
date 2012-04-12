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

import weka.gui.GenericObjectEditor;
import weka.gui.PropertyDialog;

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
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.DefaultListCellRenderer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;

/** 
 * This panel controls setting a list of algorithms for an experiment to
 * iterate over.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $
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

  /** Click to remove the selected dataset from the list */
  protected JButton m_DeleteBut = new JButton("Delete selected");
  
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
    topLab.add(m_DeleteBut,constraints);

    add(topLab, BorderLayout.NORTH);
    add(new JScrollPane(m_List), BorderLayout.CENTER);
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
    if (m_AlgorithmListModel.size() > 0) {
      m_DeleteBut.setEnabled(true);
    }
  }

  /**
   * Add a new algorithm to the list.
   */
  private void addNewAlgorithm(Classifier newScheme) {

    m_AlgorithmListModel.addElement(newScheme);
    Classifier[] cArray = new Classifier[m_AlgorithmListModel.size()];
    for (int i=0; i<cArray.length; i++) {
      cArray[i] = (Classifier) m_AlgorithmListModel.elementAt(i);
    }
    m_Exp.setPropertyArray(cArray); 
  }

  /**
   * Handle actions when buttons get pressed.
   *
   * @param e a value of type 'ActionEvent'
   */
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == m_AddBut) {

      if (m_PD == null) {
	int x = getLocationOnScreen().x;
	int y = getLocationOnScreen().y;
	m_PD = new PropertyDialog(m_ClassifierEditor, x, y);
      } else {
	m_PD.setVisible(true);
      }
      m_DeleteBut.setEnabled(true);
     
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
	m_DeleteBut.setEnabled(false);
      }

      Classifier[] cArray = new Classifier[m_AlgorithmListModel.size()];
      for (int i=0; i<cArray.length; i++) {
	cArray[i] = (Classifier) m_AlgorithmListModel.elementAt(i);
      }
      m_Exp.setPropertyArray(cArray); 
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
