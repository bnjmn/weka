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

import weka.gui.ExtensionFileFilter;
import weka.gui.ListSelectorDialog;
import weka.gui.ResultHistoryPanel;
import weka.experiment.Experiment;
import weka.experiment.InstancesResultListener;
import weka.experiment.DatabaseResultListener;
import weka.experiment.PairedTTester;
import weka.experiment.InstanceQuery;
import weka.core.Utils;
import weka.core.Attribute;
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
import javax.swing.JCheckBox;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.awt.Dimension;
import javax.swing.SwingUtilities;


/** 
 * This panel controls simple analysis of experimental results.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.10 $
 */
public class ResultsPanel extends JPanel {

  /** Message shown when no experimental results have been loaded */
  protected final static String NO_SOURCE = "No source";

  /** Click to load results from a file */
  protected JButton m_FromFileBut = new JButton("File...");

  /** Click to load results from a database */
  protected JButton m_FromDBaseBut = new JButton("Database...");

  /** Click to get results from the destination given in the experiment */
  protected JButton m_FromExpBut = new JButton("Experiment");

  /** Displays a message about the current result set */
  protected JLabel m_FromLab = new JLabel(NO_SOURCE);

  /**
   * This is needed to get around a bug in Swing <= 1.1 -- Once everyone
   * is using Swing 1.1.1 or higher just remove this variable and use the
   * no-arg constructor to DefaultComboBoxModel
   */
  static private String [] FOR_JFC_1_1_DCBM_BUG = {""};

  /** The model embedded in m_DatasetCombo */
  protected DefaultComboBoxModel m_DatasetModel =
    new DefaultComboBoxModel(FOR_JFC_1_1_DCBM_BUG);
  
  /** The model embedded in m_RunCombo */
  protected DefaultComboBoxModel m_RunModel =
    new DefaultComboBoxModel(FOR_JFC_1_1_DCBM_BUG);
  
  /** The model embedded in m_CompareCombo */
  protected DefaultComboBoxModel m_CompareModel = 
    new DefaultComboBoxModel(FOR_JFC_1_1_DCBM_BUG);
  
  /** The model embedded in m_TestsCombo */
  protected DefaultComboBoxModel m_TestsModel = 
    new DefaultComboBoxModel(FOR_JFC_1_1_DCBM_BUG);
  
  /** Lets the user select which column contains the datset name */
  protected JComboBox m_DatasetCombo = new JComboBox(m_DatasetModel);

  /** Lets the user select which column contains the run number */
  protected JComboBox m_RunCombo = new JComboBox(m_RunModel);

  /** Displays the currently selected column names for the scheme & options */
  protected JLabel m_ResultKeyLabel = new JLabel("Result key fields",
						 SwingConstants.RIGHT);

  /** Click to edit the columns used to determine the scheme */
  protected JButton m_ResultKeyBut = new JButton("Select keys...");

  /** Stores the list of attributes for selecting the scheme columns */
  protected DefaultListModel m_ResultKeyModel = new DefaultListModel();

  /** Displays the list of selected columns determining the scheme */
  protected JList m_ResultKeyList = new JList(m_ResultKeyModel);

  /** Lets the user select which performance measure to analyze */
  protected JComboBox m_CompareCombo = new JComboBox(m_CompareModel);

  /** Lets the user edit the test significance */
  protected JTextField m_SigTex = new JTextField("0.05");

  /** Lets the user select which scheme to base comparisons against */
  protected JComboBox m_TestsCombo = new JComboBox(m_TestsModel);

  /** Lets the user select whether standard deviations are to be output
      or not */
  protected JCheckBox m_ShowStdDevs = 
    new JCheckBox("");

  /** Click to start the test */
  protected JButton m_PerformBut = new JButton("Perform test");

  /** Displays the output of tests */
  protected JTextArea m_OutText = new JTextArea();

  /** A panel controlling results viewing */
  protected ResultHistoryPanel m_History = new ResultHistoryPanel(m_OutText);

  /** Filter to ensure only arff files are selected for result files */  
  protected FileFilter m_ArffFilter =
    new ExtensionFileFilter(".arff", "Arff data files");

  
  /** The file chooser for selecting result files */
  protected JFileChooser m_FileChooser = new JFileChooser();

  /** The PairedTTester object */
  protected PairedTTester m_TTester = new PairedTTester();
  
  /** The instances we're extracting results from */
  protected Instances m_Instances;

  /** Does any database querying for us */
  protected InstanceQuery m_InstanceQuery;

  /** A thread to load results instances from a file or database */
  protected Thread m_LoadThread;
  
  /** An experiment (used for identifying a result source) -- optional */
  protected Experiment m_Exp;

  /** An actionlisteners that updates ttest settings */
  protected ActionListener m_ConfigureListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      setTTester();
    }
  };
  
  private Dimension COMBO_SIZE = new Dimension(150, m_ResultKeyBut
					       .getPreferredSize().height);
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
	if (m_LoadThread == null) {
	  m_LoadThread = new Thread() {
	    public void run() {
	      setInstancesFromExp(m_Exp);
	      m_LoadThread = null;
	    }
	  };
	  m_LoadThread.start();
	}
      }
    });
    m_FromDBaseBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	if (m_LoadThread == null) {
	  m_LoadThread = new Thread() {
	    public void run() {
	      setInstancesFromDBaseQuery();
	    m_LoadThread = null;
	    }
	  };
	  m_LoadThread.start();
	}
      }
    });
    m_FromFileBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	int returnVal = m_FileChooser.showOpenDialog(ResultsPanel.this);
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	  final File selected = m_FileChooser.getSelectedFile();
	  if (m_LoadThread == null) {
	    m_LoadThread = new Thread() {
	      public void run() {
		setInstancesFromFile(selected);
		m_LoadThread = null;
	      }
	    };
	    m_LoadThread.start();
	  }
	}
      }
    });
    setComboSizes();
    m_DatasetCombo.setEnabled(false);
    m_DatasetCombo.addActionListener(m_ConfigureListener);
    m_RunCombo.setEnabled(false);
    m_RunCombo.addActionListener(m_ConfigureListener);
    m_ResultKeyBut.setEnabled(false);
    m_ResultKeyBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setResultKeyFromDialog();
      }
    });
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
    m_OutText.setFont(new Font("Monospaced", Font.PLAIN, 12));
    m_OutText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_OutText.setEditable(false);
    m_History.setBorder(BorderFactory.createTitledBorder("Result list"));


    // Set up the GUI layout
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createTitledBorder("Source"));
    JPanel p2 = new JPanel();
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints constraints = new GridBagConstraints();
    p2.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
    //    p2.setLayout(new GridLayout(1, 3));
    p2.setLayout(gb);
    constraints.gridx=0;constraints.gridy=0;constraints.weightx=5;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridwidth=1;constraints.gridheight=1;
    constraints.insets = new Insets(0,2,0,2);
    p2.add(m_FromFileBut,constraints);
    constraints.gridx=1;constraints.gridy=0;constraints.weightx=5;
    constraints.gridwidth=1;constraints.gridheight=1;
    p2.add(m_FromDBaseBut,constraints);
    constraints.gridx=2;constraints.gridy=0;constraints.weightx=5;
    constraints.gridwidth=1;constraints.gridheight=1;
    p2.add(m_FromExpBut,constraints);
    p1.setLayout(new BorderLayout());
    p1.add(m_FromLab, BorderLayout.CENTER);
    p1.add(p2, BorderLayout.EAST);

    JPanel p3 = new JPanel();
    p3.setBorder(BorderFactory.createTitledBorder("Configure test"));
    GridBagLayout gbL = new GridBagLayout();
    p3.setLayout(gbL);
    JLabel lab = new JLabel("Dataset field", SwingConstants.RIGHT);
    GridBagConstraints gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.gridy = 0;     gbC.gridx = 0;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(lab,gbC);
    p3.add(lab);
    gbC = new GridBagConstraints();
    gbC.gridy = 0;     gbC.gridx = 1;  gbC.weightx = 100;
    gbC.insets = new Insets(5,0,5,0);
    gbL.setConstraints(m_DatasetCombo, gbC);
    p3.add(m_DatasetCombo);

    lab = new JLabel("Run field", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.gridy = 1;     gbC.gridx = 0;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(lab, gbC);
    p3.add(lab);
    gbC = new GridBagConstraints();
    gbC.gridy = 1;     gbC.gridx = 1;  gbC.weightx = 100;
    gbC.insets = new Insets(5,0,5,0);
    gbL.setConstraints(m_RunCombo, gbC);
    p3.add(m_RunCombo);
    
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.gridy = 2;     gbC.gridx = 0;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(m_ResultKeyLabel, gbC);
    p3.add(m_ResultKeyLabel);
    gbC = new GridBagConstraints();
    gbC.gridy = 2;     gbC.gridx = 1;  gbC.weightx = 100;
    gbC.insets = new Insets(5,0,5,0);
    gbL.setConstraints(m_ResultKeyBut, gbC);
    p3.add(m_ResultKeyBut);
    
    lab = new JLabel("Comparison field", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.gridy = 3;     gbC.gridx = 0;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(lab, gbC);
    p3.add(lab);
    gbC = new GridBagConstraints();
    gbC.gridy = 3;     gbC.gridx = 1;  gbC.weightx = 100;
    gbC.insets = new Insets(5,0,5,0);
    gbL.setConstraints(m_CompareCombo, gbC);
    p3.add(m_CompareCombo);
    
    lab = new JLabel("Significance", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.gridy = 4;     gbC.gridx = 0;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(lab, gbC);
    p3.add(lab);
    gbC = new GridBagConstraints();
    gbC.gridy = 4;     gbC.gridx = 1;  gbC.weightx = 100;
    gbL.setConstraints(m_SigTex, gbC);
    p3.add(m_SigTex);
    
    lab = new JLabel("Test base", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.gridy = 5;     gbC.gridx = 0;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(lab, gbC);
    p3.add(lab);
    gbC = new GridBagConstraints();
    gbC.gridy = 5;     gbC.gridx = 1;  gbC.weightx = 100;
    gbC.insets = new Insets(5,0,5,0);
    gbL.setConstraints(m_TestsCombo, gbC);
    p3.add(m_TestsCombo);

    lab = new JLabel("Show std. deviations", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.gridy = 6;     gbC.gridx = 0;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(lab, gbC);
    p3.add(lab);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 6;     gbC.gridx = 1;  gbC.weightx = 100;
    gbC.insets = new Insets(5,0,5,0);
    gbL.setConstraints(m_ShowStdDevs, gbC);
    p3.add(m_ShowStdDevs);

    JPanel output = new JPanel();
    output.setLayout(new BorderLayout());
    output.setBorder(BorderFactory.createTitledBorder("Test output"));
    output.add(new JScrollPane(m_OutText), BorderLayout.CENTER);

    JPanel mondo = new JPanel();
    gbL = new GridBagLayout();
    mondo.setLayout(gbL);
    gbC = new GridBagConstraints();
    //    gbC.anchor = GridBagConstraints.WEST;
    //    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 0;     gbC.gridx = 0;
    gbL.setConstraints(p3, gbC);
    mondo.add(p3);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.NORTH;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 1;     gbC.gridx = 0;
    gbC.insets = new Insets(5,5,5,5);
    gbL.setConstraints(m_PerformBut, gbC);
    mondo.add(m_PerformBut);
    gbC = new GridBagConstraints();
    //gbC.anchor = GridBagConstraints.NORTH;
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 2;     gbC.gridx = 0; gbC.weightx = 0;
    gbL.setConstraints(m_History, gbC);
    mondo.add(m_History);
    gbC = new GridBagConstraints();
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 0;     gbC.gridx = 1;
    gbC.gridheight = 3;
    gbC.weightx = 100; gbC.weighty = 100;
    gbL.setConstraints(output, gbC);
    mondo.add(output);

    setLayout(new BorderLayout());
    add(p1, BorderLayout.NORTH);
    add(mondo , BorderLayout.CENTER);
  }

  /**
   * Sets the combo-boxes to a fixed size so they don't take up too much room
   * that would be better devoted to the test output box
   */
  protected void setComboSizes() {
    
    m_DatasetCombo.setPreferredSize(COMBO_SIZE);
    m_RunCombo.setPreferredSize(COMBO_SIZE);
    m_ResultKeyBut.setPreferredSize(COMBO_SIZE);
    m_CompareCombo.setPreferredSize(COMBO_SIZE);
    m_SigTex.setPreferredSize(COMBO_SIZE);
    m_TestsCombo.setPreferredSize(COMBO_SIZE);

    m_DatasetCombo.setMaximumSize(COMBO_SIZE);
    m_RunCombo.setMaximumSize(COMBO_SIZE);
    m_ResultKeyBut.setMaximumSize(COMBO_SIZE);
    m_CompareCombo.setMaximumSize(COMBO_SIZE);
    m_SigTex.setMaximumSize(COMBO_SIZE);
    m_TestsCombo.setMaximumSize(COMBO_SIZE);

    m_DatasetCombo.setMinimumSize(COMBO_SIZE);
    m_RunCombo.setMinimumSize(COMBO_SIZE);
    m_ResultKeyBut.setMinimumSize(COMBO_SIZE);
    m_CompareCombo.setMinimumSize(COMBO_SIZE);
    m_SigTex.setMinimumSize(COMBO_SIZE);
    m_TestsCombo.setMinimumSize(COMBO_SIZE);
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

      DefaultListModel lm = new DefaultListModel();
      for (int i = 0; i < index.numInstances(); i++) {
	lm.addElement(index.instance(i).toString());
      }
      JList jl = new JList(lm);
      ListSelectorDialog jd = new ListSelectorDialog(null, jl);
      int result = jd.showDialog();
      if (result != ListSelectorDialog.APPROVE_OPTION) {
	m_FromLab.setText("Cancelled");
	return;
      }
      Instance selInst = index.instance(jl.getSelectedIndex());
      Attribute tableAttr = index.attribute(InstanceQuery.EXP_RESULT_COL);
      String table = InstanceQuery.EXP_RESULT_PREFIX
	+ selInst.toString(tableAttr);

      setInstancesFromDatabaseTable(table);
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
      m_FromLab.setText("Reading from database, please wait...");
      final Instances i = m_InstanceQuery.getInstances("SELECT * FROM "
						      + tableName);
      SwingUtilities.invokeAndWait(new Runnable() {
	public void run() {
	  setInstances(i);
	}
      });
      m_InstanceQuery.disconnectFromDatabase();
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
      ex.printStackTrace();
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
    m_DatasetModel.removeAllElements();
    m_RunModel.removeAllElements();
    m_ResultKeyModel.removeAllElements();
    m_CompareModel.removeAllElements();
    int datasetCol = -1;
    int runCol = -1;
    String selectedList = "";
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      String name = m_Instances.attribute(i).name();
      m_DatasetModel.addElement(name);
      m_RunModel.addElement(name);
      m_ResultKeyModel.addElement(name);
      m_CompareModel.addElement(name);

      if ((datasetCol == -1)
	  && (name.toLowerCase().indexOf("dataset") != -1)) {
	m_DatasetCombo.setSelectedIndex(i);
	datasetCol = i;
      }
      if ((runCol == -1)
	  && (name.toLowerCase().indexOf("run") != -1)) {
	m_RunCombo.setSelectedIndex(i);
	runCol = i;
      }
      if (name.toLowerCase().startsWith("key_")) {
	if ((i != datasetCol) && (i != runCol)) {
	  m_ResultKeyList.addSelectionInterval(i, i);
	  selectedList += "," + (i + 1);
	}
      }
      if (name.toLowerCase().indexOf("percent_correct") != -1) {
	m_CompareCombo.setSelectedIndex(i);
	//	break;
      }
    }
    if (datasetCol == -1) {
      datasetCol = 0;
    }
    if (runCol == -1) {
      runCol = 0;
    }
    m_DatasetCombo.setEnabled(true);
    m_RunCombo.setEnabled(true);
    m_ResultKeyBut.setEnabled(true);
    m_CompareCombo.setEnabled(true);
    
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

    m_SigTex.setEnabled(true);

    setTTester();
  }

  /**
   * Updates the test chooser with possible tests
   */
  protected void setTTester() {
    
    String name = (new SimpleDateFormat("HH:mm:ss - "))
      .format(new Date())
      + "Available resultsets";
    StringBuffer outBuff = new StringBuffer();
    outBuff.append("Available resultsets\n"
		   + m_TTester.resultsetKey() + "\n\n");
    m_History.addResult(name, outBuff);
    m_History.setSingle(name);

    m_TestsModel.removeAllElements();
    for (int i = 0; i < m_TTester.getNumResultsets(); i++) {
      String tname = m_TTester.getResultsetName(i);
      if (tname.length() > 20) {
	tname = tname.substring(0, 20);
      }
      m_TestsModel.addElement(tname);
    }
    m_TestsModel.addElement("Summary");
    m_TestsModel.addElement("Ranking");
    m_TestsCombo.setSelectedIndex(0);
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
    m_TTester.setShowStdDevs(m_ShowStdDevs.isSelected());
    int compareCol = m_CompareCombo.getSelectedIndex();
    int tType = m_TestsCombo.getSelectedIndex();
    String name = (new SimpleDateFormat("HH:mm:ss - "))
      .format(new Date())
      + (String) m_CompareCombo.getSelectedItem() + " - "
      + (String) m_TestsCombo.getSelectedItem();
    StringBuffer outBuff = new StringBuffer();
    outBuff.append(m_TTester.header(compareCol));
    outBuff.append("\n");
    m_History.addResult(name, outBuff);
    m_History.setSingle(name);
    try {
      if (tType < m_TTester.getNumResultsets()) {
	outBuff.append(m_TTester.multiResultsetFull(tType, compareCol));
      } else if (tType == m_TTester.getNumResultsets()) {
	outBuff.append(m_TTester.multiResultsetSummary(compareCol));
      } else {
	outBuff.append(m_TTester.multiResultsetRanking(compareCol));
      }
      outBuff.append("\n");
    } catch (Exception ex) {
      outBuff.append(ex.getMessage() + "\n");
    }
    m_History.updateResult(name);
  }

  
  public void setResultKeyFromDialog() {

    ListSelectorDialog jd = new ListSelectorDialog(null, m_ResultKeyList);

    // Open the dialog
    int result = jd.showDialog();
    
    // If accepted, update the ttester
    if (result == ListSelectorDialog.APPROVE_OPTION) {
      System.err.println("Fields Selected");
      int [] selected = m_ResultKeyList.getSelectedIndices();
      String selectedList = "";
      for (int i = 0; i < selected.length; i++) {
	selectedList += "," + (selected[i] + 1);
      }
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
      setTTester();
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
      jf.setSize(700, 550);
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
