/*
 *    ResultsPanel.java
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

import weka.experiment.Experiment;
import weka.experiment.InstancesResultListener;
import weka.experiment.DatabaseResultListener;
import weka.experiment.PairedTTester;
import weka.experiment.InstanceQuery;
import weka.core.Utils;
import weka.core.Instances;
import weka.core.Range;
import weka.core.Instance;

import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.JFileChooser;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;


/** 
 * This panel controls simple analysis of experimental results.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class ResultsPanel extends JPanel {

  /** Message shown when no experimental results have been loaded */
  protected final static String NO_SOURCE = "No source";

  /** Click to load results from a file */
  protected JButton m_FromFileBut = new JButton("File");

  /** Click to load results from a database */
  protected JButton m_FromDBaseBut = new JButton("Database");

  /** Click to get results from the destination given in the experiment */
  protected JButton m_FromExpBut = new JButton("Experiment");

  /** Displays a message about the current result set */
  protected JLabel m_FromLab = new JLabel(NO_SOURCE);

  /** Lets the user select which column contains the datset name */
  protected JComboBox m_DatasetCombo = new JComboBox();

  /** Lets the user select which column contains the run number */
  protected JComboBox m_RunCombo = new JComboBox();

  /** Displays the currently selected column names for the scheme & options */
  protected JLabel m_ResultKeyLabel = new JLabel("Result key fields",
						 SwingConstants.RIGHT);

  /** Click to edit the columns used to determine the scheme */
  protected JButton m_ResultKeyBut = new JButton("Select keys");

  /** Stores the list of attributes for selecting the scheme columns */
  protected DefaultListModel m_ResultKeyModel = new DefaultListModel();

  /** Displays the list of selected columns determining the scheme */
  protected JList m_ResultKeyList = new JList(m_ResultKeyModel);

  /** Lets the user select which performance measure to analyze */
  protected JComboBox m_CompareCombo = new JComboBox();

  /** Lets the user edit the test significance */
  protected JTextField m_SigTex = new JTextField("0.05");

  /** Lets the user select which scheme to base comparisons against */
  protected JComboBox m_TestsCombo = new JComboBox();

  /** Click to start the test */
  protected JButton m_PerformBut = new JButton("Perform test");

  /** Displays the output of tests */
  protected JTextArea m_OutputTex = new JTextArea();

  /** Filter to ensure only arff files are selected for result files */  
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
  
  /** The file chooser for selecting result files */
  protected JFileChooser m_FileChooser = new JFileChooser();

  /** The PairedTTester object */
  protected PairedTTester m_TTester = new PairedTTester();
  
  /** The names of the attributes in the instance set */
  protected String [] m_AttributeNames;
  
  /** The instances we're extracting results from */
  protected Instances m_Instances;

  /** Does any database querying for us */
  protected InstanceQuery m_InstanceQuery;
  
  /** An experiment (used for identifying a result source) -- optional */
  protected Experiment m_Exp;

  /** An actionlisteners that updates ttest settings */
  protected ActionListener m_ConfigureListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      setTTester();
    }
  };
  
  /**
   * Creates the results panel with no initial experiment.
   */
  public ResultsPanel() {

    // Create/Configure/Connect components
    m_FileChooser.setFileFilter(m_ArffFilter);
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    m_FromExpBut.setEnabled(false);
    m_FromExpBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setInstancesFromExp(m_Exp);
      }
    });
    m_FromDBaseBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setInstancesFromDBaseQuery();
      }
    });
    m_FromFileBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	int returnVal = m_FileChooser.showOpenDialog(ResultsPanel.this);
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	  File selected = m_FileChooser.getSelectedFile();
	  setInstancesFromFile(selected);
	}
      }
    });
    m_DatasetCombo.setEnabled(false);
    m_DatasetCombo.addActionListener(m_ConfigureListener);
    m_RunCombo.setEnabled(false);
    m_RunCombo.addActionListener(m_ConfigureListener);
    m_ResultKeyBut.setEnabled(false);
    m_ResultKeyList.setSelectionMode(ListSelectionModel
				     .MULTIPLE_INTERVAL_SELECTION);
    m_CompareCombo.setEnabled(false);
    m_SigTex.setEnabled(false);
    m_TestsCombo.setEnabled(false);
    m_PerformBut.setEnabled(false);
    m_PerformBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	performTest();
      }
    });
    m_OutputTex.setFont(new Font("Dialoginput", Font.PLAIN, 10));
    m_OutputTex.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_OutputTex.setEditable(false);


    // Set up the GUI layout
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createTitledBorder("Result source"));
    JPanel p2 = new JPanel();
    p2.setLayout(new GridLayout(1, 3));
    p2.add(m_FromFileBut);
    p2.add(m_FromDBaseBut);
    p2.add(m_FromExpBut);
    p1.setLayout(new BorderLayout());
    p1.add(m_FromLab, BorderLayout.CENTER);
    p1.add(p2, BorderLayout.EAST);

    JPanel p3 = new JPanel();
    p3.setBorder(BorderFactory.createTitledBorder("Configure fields"));
    p3.setLayout(new GridLayout(3, 2, 10, 5));
    p3.add(new JLabel("Dataset field", SwingConstants.RIGHT));
    p3.add(m_DatasetCombo);
    p3.add(new JLabel("Run field", SwingConstants.RIGHT));
    p3.add(m_RunCombo);
    p3.add(m_ResultKeyLabel);
    p3.add(m_ResultKeyBut);

    JPanel p4 = new JPanel();
    p4.setBorder(BorderFactory.createTitledBorder("Configure test"));
    p4.setLayout(new GridLayout(3, 2, 10, 5));
    p4.add(new JLabel("Comparison field", SwingConstants.RIGHT));
    p4.add(m_CompareCombo);
    p4.add(new JLabel("Significance", SwingConstants.RIGHT));
    p4.add(m_SigTex);
    p4.add(new JLabel("Test base", SwingConstants.RIGHT));
    p4.add(m_TestsCombo);

    JPanel p5 = new JPanel();
    p5.setLayout(new GridLayout(1,2));
    p5.add(p3, BorderLayout.NORTH);
    p5.add(p4, BorderLayout.SOUTH);

    JPanel p6 = new JPanel();
    p6.setLayout(new BorderLayout());
    p6.add(p1, BorderLayout.NORTH);
    p6.add(p5, BorderLayout.SOUTH);

    JPanel p7 = new JPanel();
    p7.setLayout(new BorderLayout());
    p7.add(p6, BorderLayout.NORTH);
    p7.add(m_PerformBut, BorderLayout.SOUTH);
    
    setLayout(new BorderLayout());
    add(p7, BorderLayout.NORTH);
    JScrollPane js = new JScrollPane(m_OutputTex);
    js.setBorder(BorderFactory.createTitledBorder("Test results"));
    add(js , BorderLayout.CENTER);
  }

  
  /**
   * Tells the panel to use a new experiment.
   *
   * @param exp a value of type 'Experiment'
   */
  public void setExperiment(Experiment exp) {
    
    m_Exp = exp;
    setFromExpEnabled();
  }

  /**
   * Updates whether the current experiment is of a type that we can
   * determine the results destination.
   */
  protected void setFromExpEnabled() {

    if ((m_Exp.getResultListener() instanceof InstancesResultListener)
	|| (m_Exp.getResultListener() instanceof DatabaseResultListener)) {
      m_FromExpBut.setEnabled(true);
    } else {
      m_FromExpBut.setEnabled(false);
    }
  }

  /**
   * Queries the user enough to make a database query to retrieve experiment
   * results.
   */
  protected void setInstancesFromDBaseQuery() {

    try {
      if (m_InstanceQuery == null) {
	m_InstanceQuery = new InstanceQuery();
      }
      String dbaseURL = m_InstanceQuery.getDatabaseURL();
      dbaseURL = (String) JOptionPane.showInputDialog(this,
					     "Enter the database URL",
					     "Query Database",
					     JOptionPane.PLAIN_MESSAGE,
					     null,
					     null,
					     dbaseURL);
      if (dbaseURL == null) {
	m_FromLab.setText("Cancelled");
	return;
      }
      m_InstanceQuery.setDatabaseURL(dbaseURL);
      m_InstanceQuery.connectToDatabase();
      if (!m_InstanceQuery.experimentIndexExists()) {
	m_FromLab.setText("No experiment index");
	return;
      }
      m_FromLab.setText("Getting experiment index");
      Instances index = m_InstanceQuery.getInstances("SELECT * FROM "
				       + InstanceQuery.EXP_INDEX_TABLE);
      if (index.numInstances() == 0) {
	m_FromLab.setText("No experiments available");
	return;	
      }
      m_FromLab.setText("Got experiment index");
      Instance [] options = new Instance [index.numInstances()];
      for (int i = 0; i < options.length; i++) {
	options[i] = index.instance(i);
      }
      Instance exp = (Instance) JOptionPane.showInputDialog(this,
					     "Select the experiment",
					     "Experiment",
					     JOptionPane.PLAIN_MESSAGE,
					     null,
					     options,
					     options[0]);
      if (exp == null) {
	m_FromLab.setText("Cancelled");
	return;
      }
      String table = InstanceQuery.EXP_RESULT_PREFIX
	+ exp.toString(index.attribute(InstanceQuery.EXP_RESULT_COL));
      setInstancesFromDatabaseTable(table);
      m_InstanceQuery.disconnectFromDatabase();
    } catch (Exception ex) {
      m_FromLab.setText("Problem reading database");
    }
  }
  
  /**
   * Examines the supplied experiment to determine the results destination
   * and attempts to load the results.
   *
   * @param exp a value of type 'Experiment'
   */
  protected void setInstancesFromExp(Experiment exp) {

    if (exp.getResultListener() instanceof InstancesResultListener) {
      File resultFile = ((InstancesResultListener) exp.getResultListener())
	.getOutputFile();
      if ((resultFile == null) || (resultFile.getName().equals("-"))) {
	m_FromLab.setText("No result file");
      } else {
	setInstancesFromFile(resultFile);
      }
    } else if (exp.getResultListener() instanceof DatabaseResultListener) {
      String dbaseURL = ((DatabaseResultListener) exp.getResultListener())
	.getDatabaseURL();
      try {
	if (m_InstanceQuery == null) {
	  m_InstanceQuery = new InstanceQuery();
	}
	m_InstanceQuery.setDatabaseURL(dbaseURL);
	m_InstanceQuery.connectToDatabase();
	String tableName = m_InstanceQuery
	  .getResultsTableName(exp.getResultProducer());
	setInstancesFromDatabaseTable(tableName);
	m_InstanceQuery.disconnectFromDatabase();
      } catch (Exception ex) {
	m_FromLab.setText("Problem reading database");
      }
    } else {
      m_FromLab.setText("Can't get results from experiment");
    }
  }

  
  /**
   * Queries a database to load results from the specified table name. The
   * database connection must have already made by m_InstanceQuery.
   *
   * @param tableName the name of the table containing results to retrieve.
   */
  protected void setInstancesFromDatabaseTable(String tableName) {

    try {
      m_FromLab.setText("Reading from database...");
      setInstances(m_InstanceQuery.getInstances("SELECT * FROM " + tableName));
    } catch (Exception ex) {
      m_FromLab.setText(ex.getMessage());
    }
  }
  
  /**
   * Loads results from a set of instances contained in the supplied
   * file.
   *
   * @param f a value of type 'File'
   */
  protected void setInstancesFromFile(File f) {
      
    try {
      m_FromLab.setText("Reading from file...");
      Reader r = new BufferedReader(new FileReader(f));
      setInstances(new Instances(r));
    } catch (Exception ex) {
      m_FromLab.setText(ex.getMessage());
    }
  }

  /**
   * Sets up the panel with a new set of instances, attempting
   * to guess the correct settings for various columns.
   *
   * @param newInstances the new set of results.
   */
  public void setInstances(Instances newInstances) {

    m_Instances = newInstances;
    m_TTester.setInstances(m_Instances);
    m_FromLab.setText("Got " + m_Instances.numInstances() + " results");

    // Temporarily remove the configuration listener
    m_DatasetCombo.removeActionListener(m_ConfigureListener);
    m_RunCombo.removeActionListener(m_ConfigureListener);
    
    // Do other stuff
    m_AttributeNames = new String [m_Instances.numAttributes()];
    for (int i = 0; i < m_AttributeNames.length; i++) {
      m_AttributeNames[i] = m_Instances.attribute(i).name();
    }

    m_DatasetCombo.setModel(new DefaultComboBoxModel(m_AttributeNames));
    int datasetCol = 0;
    for (int i = 0; i < m_AttributeNames.length; i++) {
      if (m_AttributeNames[i].toLowerCase().indexOf("dataset") != -1) {
	m_DatasetCombo.setSelectedIndex(i);
	datasetCol = i;
	break;
      }
    }
    m_DatasetCombo.setEnabled(true);

    m_RunCombo.setModel(new DefaultComboBoxModel(m_AttributeNames));
    int runCol = 0;
    for (int i = 0; i < m_AttributeNames.length; i++) {
      if (m_AttributeNames[i].toLowerCase().indexOf("run") != -1) {
	m_RunCombo.setSelectedIndex(i);
	runCol = i;
	break;
      }
    }
    m_RunCombo.setEnabled(true);

    m_ResultKeyModel.removeAllElements();
    String selected = "";
    String selectedList = "";
    for (int i = 0; i < m_AttributeNames.length; i++) {
      m_ResultKeyModel.addElement(m_AttributeNames[i]);
      if (m_AttributeNames[i].toLowerCase().startsWith("key_")) {
	if ((i != datasetCol) && (i != runCol)) {
	  m_ResultKeyList.addSelectionInterval(i, i);
	  selected += m_AttributeNames[i] + ' ';
	  selectedList += "," + (i + 1);
	}
      }
    }
    m_ResultKeyBut.setEnabled(true);

    // Reconnect the configuration listener
    m_DatasetCombo.addActionListener(m_ConfigureListener);
    m_RunCombo.addActionListener(m_ConfigureListener);
    
    // Set up the TTester with the new data
    m_TTester.setDatasetColumn(datasetCol);
    m_TTester.setRunColumn(runCol);
    Range generatorRange = new Range();
    if (selectedList.length() != 0) {
      try {
	generatorRange.setRanges(selectedList);
      } catch (Exception ex) {
	ex.printStackTrace();
	System.err.println(ex.getMessage());
      }
    }
    m_TTester.setResultsetKeyColumns(generatorRange);

    // Default test configurations
    m_CompareCombo.setModel(new DefaultComboBoxModel(m_AttributeNames));
    for (int i = 0; i < m_AttributeNames.length; i++) {
      if (m_AttributeNames[i].toLowerCase().indexOf("percent_correct") != -1) {
	m_CompareCombo.setSelectedIndex(i);
	break;
      }
    }
    m_CompareCombo.setEnabled(true);

    m_SigTex.setEnabled(true);

    setTTester();
  }

  /**
   * Updates the test chooser with possible tests
   */
  protected void setTTester() {
    
    m_OutputTex.append("Available resultsets\n"
		       + m_TTester.resultsetKey() + "\n\n");

    String [] testTypes = new String[m_TTester.getNumResultsets() + 1];
    for (int i = 0; i < m_TTester.getNumResultsets(); i++) {
      testTypes[i] = m_TTester.getResultsetName(i);
    }
    testTypes[testTypes.length - 1] = "Summary";
    m_TestsCombo.setModel(new DefaultComboBoxModel(testTypes));
    m_TestsCombo.setEnabled(true);

    m_PerformBut.setEnabled(true);
    
  }

  
  /**
   * Carries out a t-test using the current configuration.
   */
  protected void performTest() {

    String sigStr = m_SigTex.getText();
    if (sigStr.length() != 0) {
      m_TTester.setSignificanceLevel((new Double(sigStr)).doubleValue());
    } else {
      m_TTester.setSignificanceLevel(0.05);
    }

    // Carry out the test chosen and biff the results to the output area
    try {
      int compareCol = m_CompareCombo.getSelectedIndex();
      int tType = m_TestsCombo.getSelectedIndex();
      m_OutputTex.append(m_TTester.header(compareCol));
      m_OutputTex.append("\n");
      if (tType < m_TTester.getNumResultsets()) {
	m_OutputTex.append(m_TTester.multiResultsetFull(tType, compareCol));
      } else {
	m_OutputTex.append(m_TTester.multiResultsetSummary(compareCol));
      }
      m_OutputTex.append("\n");
    } catch (Exception ex) {
      m_OutputTex.append(ex.getMessage() + "\n");
    }
  }
  
  /**
   * Tests out the results panel from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      final JFrame jf = new JFrame("Weka Experiment: Results Analysis");
      jf.getContentPane().setLayout(new BorderLayout());
      final ResultsPanel sp = new ResultsPanel();
      //sp.setBorder(BorderFactory.createTitledBorder("Setup"));
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
