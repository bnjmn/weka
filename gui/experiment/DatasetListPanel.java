
package weka.gui.experiment;

import weka.experiment.Experiment;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;

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
import java.awt.event.ActionEvent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.io.File;


public class DatasetListPanel extends JPanel implements ActionListener {

  protected Experiment m_Exp;
  
  protected JList m_List;
  protected JButton m_AddBut = new JButton("Add New");
  protected JButton m_DeleteBut = new JButton("DeleteSelected");
  protected FileFilter m_ArffFilter = new FileFilter() {
    public String getDescription() {
      return "Arff data files";
    }
    public boolean accept(File file) {
      String name = file.getName().toLowerCase();
      if (file.isDirectory()) {
	return true;
      }
      if (name.endsWith(".arff")) {
	return true;
      }
      return false;
    }
  };
  protected JFileChooser m_FileChooser = new JFileChooser();

  
  public DatasetListPanel(Experiment exp) {

    this();
    setExperiment(exp);
  }

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
    topLab.setLayout(new GridLayout(1,2));
    topLab.add(m_AddBut);
    topLab.add(m_DeleteBut);
    add(topLab, BorderLayout.NORTH);
    add(new JScrollPane(m_List), BorderLayout.CENTER);
  }

  public void setExperiment(Experiment exp) {

    m_Exp = exp;
    m_AddBut.setEnabled(true);
    m_List.setModel(m_Exp.getDatasets());
  }
  
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == m_AddBut) {
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
      // Suss out deleting multiple selections
      int selected = m_List.getSelectedIndex();
      if (selected != -1) {
	m_Exp.getDatasets().removeElementAt(selected);
	if (m_Exp.getDatasets().size() > selected) {
	  m_List.setSelectedIndex(selected);
	}
      }
      if (m_List.getSelectedIndex() == -1) {
	m_DeleteBut.setEnabled(false);
      }
    }
  }
  
  /**
   * Tests out the classifier editor from the command line.
   *
   * @param args may contain the class name of a classifier to edit
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
