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
 *    DatasetListPanel.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui.experiment;

import weka.core.Instances;
import weka.core.RTSI;
import weka.experiment.Experiment;

import weka.gui.ExtensionFileFilter;
import java.io.File;
import java.util.Vector;
import java.util.Collections;
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
import javax.swing.JCheckBox;


/** 
 * This panel controls setting a list of datasets for an experiment to
 * iterate over.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.13.4.3 $
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

  /** Make file paths relative to the user (start) directory */
  protected JCheckBox m_relativeCheck = new JCheckBox("Use relative paths");

  /** A filter to ensure only arff files get selected */
  protected FileFilter m_ArffFilter =
    new ExtensionFileFilter(Instances.FILE_EXTENSION, "Arff data files");

  /** The user (start) directory */
  protected File m_UserDir = new File(System.getProperty("user.dir"));

  /** The file chooser component */
  protected JFileChooser m_FileChooser = new
    JFileChooser(ExperimenterDefaults.getInitialDatasetsDirectory());

  
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
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    m_DeleteBut.setEnabled(false);
    m_DeleteBut.addActionListener(this);
    m_AddBut.setEnabled(false);
    m_AddBut.addActionListener(this);
    m_relativeCheck.setSelected(ExperimenterDefaults.getUseRelativePaths());
    m_relativeCheck.setToolTipText("Store file paths relative to "
				   +"the start directory");
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

    constraints.gridx=0;constraints.gridy=1;constraints.weightx=5;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridwidth=1;constraints.gridheight=1;
    constraints.insets = new Insets(0,2,0,2);
    topLab.add(m_relativeCheck,constraints);

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
   * Gets all the files in the given directory
   * that match the currently selected extension.
   */
  protected void getFilesRecursively(File directory, Vector files) {

    try {
      String[] currentDirFiles = directory.list();
      for (int i = 0; i < currentDirFiles.length; i++) {
	currentDirFiles[i] = directory.getCanonicalPath() + File.separator + 
	  currentDirFiles[i];
	File current = new File(currentDirFiles[i]);
	if (m_FileChooser.getFileFilter().accept(current)) {
	  if (current.isDirectory()) {
	    getFilesRecursively(current, files);
	  } else {
	    files.addElement(current);
	  }
	}
      }
    } catch (Exception e) {
      System.err.println("IOError occured when reading list of files");
    }
  }

  /**
   * Converts a File's absolute path to a path relative to the user
   * (ie start) directory
   * @param absolute the File to convert to relative path
   * @return a File with a path that is relative to the user's directory
   * @exception Exception if the path cannot be constructed
   */
  protected File createRelativePath(File absolute) throws Exception {
    String userPath = m_UserDir.getAbsolutePath() + File.separator;
    String targetPath = (new File(absolute.getParent())).getPath() 
      + File.separator;
    String fileName = absolute.getName();
    StringBuffer relativePath = new StringBuffer();
    relativePath.append("."+File.separator);
    //    System.err.println("User dir "+userPath);
    //    System.err.println("Target path "+targetPath);
    
    // file is in user dir (or subdir)
    int subdir = targetPath.indexOf(userPath);
    if (subdir == 0) {
      if (userPath.length() == targetPath.length()) {
	relativePath.append(fileName);
      } else {
	int ll = userPath.length();
	relativePath.append(targetPath.substring(ll));
	relativePath.append(fileName);
      }
    } else {
      int sepCount = 0;
      String temp = new String(userPath);
      while (temp.indexOf(File.separator) != -1) {
	int ind = temp.indexOf(File.separator);
	sepCount++;
	temp = temp.substring(ind+1, temp.length());
      }
      
      String targetTemp = new String(targetPath);
      String userTemp = new String(userPath);
      int tcount = 0;
      while (targetTemp.indexOf(File.separator) != -1) {
	int ind = targetTemp.indexOf(File.separator);
	int ind2 = userTemp.indexOf(File.separator);
	String tpart = targetTemp.substring(0,ind+1);
	String upart = userTemp.substring(0,ind2+1);
	if (tpart.compareTo(upart) != 0) {
	  if (tcount == 0) {
	    tcount = -1;
	  }
	  break;
	}
	tcount++;
	targetTemp = targetTemp.substring(ind+1, targetTemp.length());
	userTemp = userTemp.substring(ind2+1, userTemp.length());
      }
      if (tcount == -1) {
	// then target file is probably on another drive (under windows)
	throw new Exception("Can't construct a path to file relative to user "
			    +"dir.");
      }
      if (targetTemp.indexOf(File.separator) == -1) {
	targetTemp = "";
      }
      for (int i = 0; i < sepCount - tcount; i++) {
	relativePath.append(".."+File.separator);
      }
      relativePath.append(targetTemp + fileName);
    }
    //    System.err.println("new path : "+relativePath.toString());
    return new File(relativePath.toString());
  }
  
  /**
   * Converts a File's absolute path to a path relative to the user
   * (ie start) directory. Includes an additional workaround for Cygwin, which
   * doesn't like upper case drive letters.
   * @param absolute the File to convert to relative path
   * @return a File with a path that is relative to the user's directory
   * @exception Exception if the path cannot be constructed
   */
  protected File convertToRelativePath(File absolute) throws Exception {
    File        result;
    String      fileStr;
    
    result = null;
    
    // if we're running windows, it could be Cygwin
    if (File.separator.equals("\\")) {
      // Cygwin doesn't like upper case drives -> try lower case drive
      try {
        fileStr = absolute.getPath();
        fileStr =   fileStr.substring(0, 1).toLowerCase() 
                  + fileStr.substring(1);
        result = createRelativePath(new File(fileStr));
      }
      catch (Exception e) {
        // no luck with Cygwin workaround, convert it like it is
        result = createRelativePath(absolute);
      }
    }
    else {
      result = createRelativePath(absolute);
    }

    return result;
  }
  
  /**
   * Handle actions when buttons get pressed.
   *
   * @param e a value of type 'ActionEvent'
   */
  public void actionPerformed(ActionEvent e) {
    boolean useRelativePaths = m_relativeCheck.isSelected();

    if (e.getSource() == m_AddBut) {
      // Let the user select an arff file from a file chooser
      int returnVal = m_FileChooser.showOpenDialog(this);
      if(returnVal == JFileChooser.APPROVE_OPTION) {
	if (m_FileChooser.isMultiSelectionEnabled()) {
	  File [] selected = m_FileChooser.getSelectedFiles();
	  for (int i = 0; i < selected.length; i++) {
	    if (selected[i].isDirectory()) {
	      Vector files = new Vector();
	      getFilesRecursively(selected[i], files);
    
	      // sort the result
	      RTSI r = new RTSI();
	      Collections.sort(files, r.new StringCompare());

	      for (int j = 0; j < files.size(); j++) {
		File temp = (File)files.elementAt(j);
		if (useRelativePaths) {
		  try {
		    temp = convertToRelativePath(temp);
		  } catch (Exception ex) {
		    ex.printStackTrace();
		  }
		}
		m_Exp.getDatasets().addElement(temp);
	      }
	    } else {
	      File temp = selected[i];
	      if (useRelativePaths) {
		try {
		  temp = convertToRelativePath(temp);
		} catch (Exception ex) {
		  ex.printStackTrace();
		}
	      }
	      m_Exp.getDatasets().addElement(temp);
	    }
	  }
	  m_DeleteBut.setEnabled(true);
	} else {
	  if (m_FileChooser.getSelectedFile().isDirectory()) {
	    Vector files = new Vector();
	    getFilesRecursively(m_FileChooser.getSelectedFile(), files);
    
	    // sort the result
	    RTSI r = new RTSI();
	    Collections.sort(files, r.new StringCompare());

	    for (int j = 0; j < files.size(); j++) {
	      File temp = (File)files.elementAt(j);
	      if (useRelativePaths) {
		try {
		  temp = convertToRelativePath(temp);
		} catch (Exception ex) {
		  ex.printStackTrace();
		}
	      }
	      m_Exp.getDatasets().addElement(temp);
	    }
	  } else {
	    File temp = m_FileChooser.getSelectedFile();
	    if (useRelativePaths) {
	      try {
		temp = convertToRelativePath(temp);
	      } catch (Exception ex) {
		ex.printStackTrace();
	      }
	    }
	    m_Exp.getDatasets().addElement(temp);
	  }
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
