/*
 *    DatasetListPanel.java
 *    Copyright (C) 1999 Len Trigg
 *
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

package weka.gui.experiment;

import weka.core.Instances;
import weka.experiment.Experiment;

import weka.gui.ExtensionFileFilter;
import java.io.File;
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
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;


/** 
 * This panel controls setting a list of datasets for an experiment to
 * iterate over.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.8 $
 */
public class DatasetListPanel extends JPanel implements ActionListener {

  /** The experiment to set the dataset list of */
  protected Experiment m_Exp;

  /** The component displaying the dataset list */
  protected JList m_List;

  /** Click to add a dataset */
  protected JButton m_AddBut = new JButton("Add new...");

  /** Click to remove the selected dataset from the list */
  protected JButton m_DeleteBut = new JButton("Delete selected");

  /** A filter to ensure only arff files get selected */
  protected FileFilter m_ArffFilter =
    new ExtensionFileFilter(Instances.FILE_EXTENSION, "Arff data files");

  /** The file chooser component */
  protected JFileChooser m_FileChooser = new JFileChooser(new File(System.getProperty("user.dir")));

  
  /**
   * Creates the dataset list panel with the given experiment.
   *
   * @param exp a value of type 'Experiment'
   */
  public DatasetListPanel(Experiment exp) {

    this();
    setExperiment(exp);
  }

  /**
   * Create the dataset list panel initially disabled.
   */
  public DatasetListPanel() {
    
    m_List = new JList();
    m_FileChooser.setFileFilter(m_ArffFilter);
    // Multiselection isn't handled by the current implementation of the
    // swing look and feels.
    // m_FileChooser.setMultiSelectionEnabled(true);
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    m_DeleteBut.setEnabled(false);
    m_DeleteBut.addActionListener(this);
    m_AddBut.setEnabled(false);
    m_AddBut.addActionListener(this);
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createTitledBorder("Datasets"));
    JPanel topLab = new JPanel();
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints constraints = new GridBagConstraints();
    topLab.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    //    topLab.setLayout(new GridLayout(1,2,5,5));
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
    m_List.setModel(m_Exp.getDatasets());
    if (m_Exp.getDatasets().size() > 0) {
      m_DeleteBut.setEnabled(true);
    }
  }
  
  /**
   * Handle actions when buttons get pressed.
   *
   * @param e a value of type 'ActionEvent'
   */
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == m_AddBut) {
      // Let the user select an arff file from a file chooser
      int returnVal = m_FileChooser.showOpenDialog(this);
      if(returnVal == JFileChooser.APPROVE_OPTION) {
	if (m_FileChooser.isMultiSelectionEnabled()) {
	  File [] selected = m_FileChooser.getSelectedFiles();
	  for (int i = 0; i < selected.length; i++) {
	    m_Exp.getDatasets().addElement(selected[i]);
	  }
	  m_DeleteBut.setEnabled(true);
	} else {
	  m_Exp.getDatasets().addElement(m_FileChooser.getSelectedFile());
	  m_DeleteBut.setEnabled(true);
	}
      }
    } else if (e.getSource() == m_DeleteBut) {
      // Delete the selected files
      int [] selected = m_List.getSelectedIndices();
      if (selected != null) {
	for (int i = selected.length - 1; i >= 0; i--) {
	  int current = selected[i];
	  m_Exp.getDatasets().removeElementAt(current);
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
    }
  }
  
  /**
   * Tests out the dataset list panel from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      final JFrame jf = new JFrame("Dataset List Editor");
      jf.getContentPane().setLayout(new BorderLayout());
      DatasetListPanel dp = new DatasetListPanel();
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
