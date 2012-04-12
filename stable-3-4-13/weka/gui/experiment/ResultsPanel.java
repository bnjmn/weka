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
 *    ResultsPanel.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui.experiment;

import weka.gui.ExtensionFileFilter;
import weka.gui.ListSelectorDialog;
import weka.gui.ResultHistoryPanel;
import weka.gui.SaveBuffer;
import weka.gui.DatabaseConnectionDialog;
import weka.experiment.Experiment;
import weka.experiment.CSVResultListener;
import weka.experiment.DatabaseResultListener;
import weka.experiment.PairedCorrectedTTester;
import weka.experiment.PairedTTester;
import weka.experiment.InstanceQuery;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Range;
import weka.core.Instance;
import weka.core.converters.CSVLoader;

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
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.JFileChooser;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JSplitPane;
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
 * @version $Revision: 1.28.2.5 $
 */
public class ResultsPanel extends JPanel {

  /** Message shown when no experimental results have been loaded */
  protected static final String NO_SOURCE = "No source";

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
  private static String [] FOR_JFC_1_1_DCBM_BUG = {""};

  /** The model embedded in m_DatasetCombo */
  protected DefaultComboBoxModel m_DatasetModel =
    new DefaultComboBoxModel(FOR_JFC_1_1_DCBM_BUG);
  
  /** The model embedded in m_CompareCombo */
  protected DefaultComboBoxModel m_CompareModel = 
    new DefaultComboBoxModel(FOR_JFC_1_1_DCBM_BUG);
  
  /** The model embedded in m_TestsList */
  protected DefaultListModel m_TestsModel = new DefaultListModel();
  
  /** The model embedded in m_DisplayedList */
  protected DefaultListModel m_DisplayedModel = new DefaultListModel();

  /** Displays the currently selected column names for the scheme & options */
  protected JLabel m_DatasetKeyLabel = new JLabel("Row",
						 SwingConstants.RIGHT);

  /** Click to edit the columns used to determine the scheme */
  protected JButton m_DatasetKeyBut = new JButton("Select");

  /** Stores the list of attributes for selecting the scheme columns */
  protected DefaultListModel m_DatasetKeyModel = new DefaultListModel();

  /** Displays the list of selected columns determining the scheme */
  protected JList m_DatasetKeyList = new JList(m_DatasetKeyModel);

  /** Displays the currently selected column names for the scheme & options */
  protected JLabel m_ResultKeyLabel = new JLabel("Column",
						 SwingConstants.RIGHT);

  /** Click to edit the columns used to determine the scheme */
  protected JButton m_ResultKeyBut = new JButton("Select");

  /** Stores the list of attributes for selecting the scheme columns */
  protected DefaultListModel m_ResultKeyModel = new DefaultListModel();

  /** Displays the list of selected columns determining the scheme */
  protected JList m_ResultKeyList = new JList(m_ResultKeyModel);

  /** Lets the user select which scheme to base comparisons against */
  protected JButton m_TestsButton = new JButton("Select base...");

  /** Lets the user select which schemes are compared to base */
  protected JButton m_DisplayedButton = new JButton("Select");

  /** Holds the list of schemes to base the test against */
  protected JList m_TestsList = new JList(m_TestsModel);

  /** Holds the list of schemes to display */
  protected JList m_DisplayedList = new JList(m_DisplayedModel);

  /** Lets the user select which performance measure to analyze */
  protected JComboBox m_CompareCombo = new JComboBox(m_CompareModel);

  /** Lets the user edit the test significance */
  protected JTextField m_SigTex = new JTextField(
      "" + ExperimenterDefaults.getSignificance());

  /** Lets the user select whether standard deviations are to be output
      or not */
  protected JCheckBox m_ShowStdDevs = 
    new JCheckBox("");
  
  /** lets the user choose the format for the output */
  protected JButton m_OutputFormatButton = new JButton("Select");

  /** Click to start the test */
  protected JButton m_PerformBut = new JButton("Perform test");
  
  /** Click to save test output to a file */
  protected JButton m_SaveOutBut = new JButton("Save output");

  /** The buffer saving object for saving output */
  SaveBuffer m_SaveOut = new SaveBuffer(null, this);

  /** Displays the output of tests */
  protected JTextArea m_OutText = new JTextArea();

  /** A panel controlling results viewing */
  protected ResultHistoryPanel m_History = new ResultHistoryPanel(m_OutText);
  
  /** The file chooser for selecting result files */
  protected JFileChooser m_FileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
 
  /** File filters for various file types */
  protected ExtensionFileFilter m_csvFileFilter = 
    new ExtensionFileFilter(CSVLoader.FILE_EXTENSION,
			    "CSV data files");

  protected ExtensionFileFilter m_arffFileFilter = 
    new ExtensionFileFilter(Instances.FILE_EXTENSION,
			    "Arff data files");

  /** The PairedTTester object */
  protected PairedTTester m_TTester = new PairedCorrectedTTester();
  
  /** The instances we're extracting results from */
  protected Instances m_Instances;

  /** Does any database querying for us */
  protected InstanceQuery m_InstanceQuery;

  /** A thread to load results instances from a file or database */
  protected Thread m_LoadThread;
  
  /** An experiment (used for identifying a result source) -- optional */
  protected Experiment m_Exp;
  
  private Dimension COMBO_SIZE = new Dimension(150, m_ResultKeyBut
					       .getPreferredSize().height);

  /** Produce tables in latex format */
  protected boolean m_latexOutput = false;
  
  /** Produce tables in csv format */
  protected boolean m_csvOutput = false;
  
  /** the number of digits after the period (= precision) for printing the mean */
  protected int m_MeanPrec = ExperimenterDefaults.getMeanPrecision();
  
  /** the number of digits after the period (= precision) for printing the std. deviation */
  protected int m_StdDevPrec = ExperimenterDefaults.getStdDevPrecision();

  /**
   * Creates the results panel with no initial experiment.
   */
  public ResultsPanel() {

    // Create/Configure/Connect components
    
    m_FileChooser.
      addChoosableFileFilter(m_csvFileFilter);
    m_FileChooser.
      addChoosableFileFilter(m_arffFileFilter);

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
    m_DatasetKeyBut.setEnabled(false);
    m_DatasetKeyBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setDatasetKeyFromDialog();
      }
    });
    m_DatasetKeyList.setSelectionMode(ListSelectionModel
				      .MULTIPLE_INTERVAL_SELECTION);
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
    m_TestsButton.setEnabled(false);
    m_TestsButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  setTestBaseFromDialog();
	}
      });

    m_DisplayedButton.setEnabled(false);
    m_DisplayedButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setDisplayedFromDialog();
      }
      });

    m_ShowStdDevs.setEnabled(false);
    m_ShowStdDevs.setSelected(ExperimenterDefaults.getShowStdDevs());
    m_OutputFormatButton.setEnabled(false);
    m_OutputFormatButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setOutputFormatFromDialog();
      }
      });
    
    m_PerformBut.setEnabled(false);
    m_PerformBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	performTest();
	m_SaveOutBut.setEnabled(true);
      }
    });

    m_PerformBut.setToolTipText("Performs test using corrected resampled t-test statistic (Nadeau and Bengio)");

    m_SaveOutBut.setEnabled(false);
    m_SaveOutBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  saveBuffer();
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

    GridBagConstraints gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.gridy = 0;     gbC.gridx = 0;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(m_DatasetKeyLabel,gbC);
    p3.add(m_DatasetKeyLabel);
    gbC = new GridBagConstraints();
    gbC.gridy = 0;     gbC.gridx = 1;  gbC.weightx = 100;
    gbC.insets = new Insets(5,0,5,0);
    gbL.setConstraints(m_DatasetKeyBut, gbC);
    p3.add(m_DatasetKeyBut);
    
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
    
    JLabel lab = new JLabel("Comparison field", SwingConstants.RIGHT);
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
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 5;     gbC.gridx = 1;  gbC.weightx = 100;
    gbC.insets = new Insets(5,0,5,0);
    gbL.setConstraints(m_TestsButton, gbC);
    p3.add(m_TestsButton);

    lab = new JLabel("Displayed Columns", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.gridy = 6;     gbC.gridx = 0;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(lab, gbC);
    p3.add(lab);
    gbC = new GridBagConstraints();
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 6;     gbC.gridx = 1;  gbC.weightx = 100;
    gbC.insets = new Insets(5,0,5,0);
    gbL.setConstraints(m_DisplayedButton, gbC);
    p3.add(m_DisplayedButton);


    lab = new JLabel("Show std. deviations", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.gridy = 7;     gbC.gridx = 0;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(lab, gbC);
    p3.add(lab);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 7;     gbC.gridx = 1;  gbC.weightx = 100;
    gbC.insets = new Insets(5,0,5,0);
    gbL.setConstraints(m_ShowStdDevs, gbC);
    p3.add(m_ShowStdDevs);

    lab = new JLabel("Output Format", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.gridy = 8;     gbC.gridx = 0;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(lab, gbC);
    p3.add(lab);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 8;     gbC.gridx = 1;  gbC.weightx = 100;
    gbC.insets = new Insets(5,0,5,0);
    gbL.setConstraints(m_OutputFormatButton, gbC);
    p3.add(m_OutputFormatButton);
    
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

    JPanel bts = new JPanel();
    bts.setLayout(new GridLayout(1,2,5,5));
    bts.add(m_PerformBut);
    bts.add(m_SaveOutBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.NORTH;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 1;     gbC.gridx = 0;
    gbC.insets = new Insets(5,5,5,5);
    gbL.setConstraints(bts, gbC);
    mondo.add(bts);
    gbC = new GridBagConstraints();
    //gbC.anchor = GridBagConstraints.NORTH;
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 2;     gbC.gridx = 0; gbC.weightx = 0;
    gbC.weighty = 100;
    gbL.setConstraints(m_History, gbC);
    mondo.add(m_History);
    /*gbC = new GridBagConstraints();
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 0;     gbC.gridx = 1;
    gbC.gridheight = 3;
    gbC.weightx = 100; gbC.weighty = 100;
    gbL.setConstraints(output, gbC);*/
    //mondo.add(output);
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					  mondo, output);
    splitPane.setOneTouchExpandable(true);
    //splitPane.setDividerLocation(100);

    setLayout(new BorderLayout());
    add(p1, BorderLayout.NORTH);
    //add(mondo , BorderLayout.CENTER);
    add(splitPane , BorderLayout.CENTER);
  }

  /**
   * Sets the combo-boxes to a fixed size so they don't take up too much room
   * that would be better devoted to the test output box
   */
  protected void setComboSizes() {
    
    m_DatasetKeyBut.setPreferredSize(COMBO_SIZE);
    m_ResultKeyBut.setPreferredSize(COMBO_SIZE);
    m_CompareCombo.setPreferredSize(COMBO_SIZE);
    m_SigTex.setPreferredSize(COMBO_SIZE);

    m_DatasetKeyBut.setMaximumSize(COMBO_SIZE);
    m_ResultKeyBut.setMaximumSize(COMBO_SIZE);
    m_CompareCombo.setMaximumSize(COMBO_SIZE);
    m_SigTex.setMaximumSize(COMBO_SIZE);

    m_DatasetKeyBut.setMinimumSize(COMBO_SIZE);
    m_ResultKeyBut.setMinimumSize(COMBO_SIZE);
    m_CompareCombo.setMinimumSize(COMBO_SIZE);
    m_SigTex.setMinimumSize(COMBO_SIZE);
  }
  
  /**
   * Tells the panel to use a new experiment.
   *
   * @param exp a value of type 'Experiment'
   */
  public void setExperiment(Experiment exp) {
    
    m_Exp = exp;
    m_FromExpBut.setEnabled(exp != null);
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
      String username = m_InstanceQuery.getUsername();
      String passwd = m_InstanceQuery.getPassword();
      /*dbaseURL = (String) JOptionPane.showInputDialog(this,
					     "Enter the database URL",
					     "Query Database",
					     JOptionPane.PLAIN_MESSAGE,
					     null,
					     null,
					     dbaseURL);*/
     
      
      
      DatabaseConnectionDialog dbd= new DatabaseConnectionDialog(null,dbaseURL,username);
      dbd.setVisible(true);
      
      //if (dbaseURL == null) {
      if (dbd.getReturnValue()==JOptionPane.CLOSED_OPTION) {
	m_FromLab.setText("Cancelled");
	return;
      }
      dbaseURL=dbd.getURL();
      username=dbd.getUsername();
      passwd=dbd.getPassword();
      m_InstanceQuery.setDatabaseURL(dbaseURL);
      
      m_InstanceQuery.setUsername(username);
      m_InstanceQuery.setPassword(passwd);
      
      m_InstanceQuery.connectToDatabase();
      if (!m_InstanceQuery.experimentIndexExists()) {
	System.err.println("not found");
	m_FromLab.setText("No experiment index");
        m_InstanceQuery.disconnectFromDatabase();
	return;
      }
      System.err.println("found");
      m_FromLab.setText("Getting experiment index");
      Instances index = m_InstanceQuery.retrieveInstances("SELECT * FROM "
				       + InstanceQuery.EXP_INDEX_TABLE);
      if (index.numInstances() == 0) {
	m_FromLab.setText("No experiments available");
        m_InstanceQuery.disconnectFromDatabase();
	return;	
      }
      m_FromLab.setText("Got experiment index");

      DefaultListModel lm = new DefaultListModel();
      for (int i = 0; i < index.numInstances(); i++) {
	lm.addElement(index.instance(i).toString());
      }
      JList jl = new JList(lm);
      jl.setSelectedIndex(0);
      ListSelectorDialog jd = new ListSelectorDialog(null, jl);
      int result = jd.showDialog();
      if (result != ListSelectorDialog.APPROVE_OPTION) {
	m_FromLab.setText("Cancelled");
        m_InstanceQuery.disconnectFromDatabase();
	return;
      }
      Instance selInst = index.instance(jl.getSelectedIndex());
      Attribute tableAttr = index.attribute(InstanceQuery.EXP_RESULT_COL);
      String table = InstanceQuery.EXP_RESULT_PREFIX
	+ selInst.toString(tableAttr);
      setInstancesFromDatabaseTable(table);
      
    } catch (Exception ex) {
       // 1. print complete stacktrace
       ex.printStackTrace();
       // 2. print message in panel
       m_FromLab.setText("Problem reading database: '" + ex.getMessage() + "'");
    }
  }
  
  /**
   * Examines the supplied experiment to determine the results destination
   * and attempts to load the results.
   *
   * @param exp a value of type 'Experiment'
   */
  protected void setInstancesFromExp(Experiment exp) {

    if ((exp.getResultListener() instanceof CSVResultListener)) { 
      File resultFile = ((CSVResultListener) exp.getResultListener())
	.getOutputFile();
      if ((resultFile == null)) {
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
      final Instances i = m_InstanceQuery.retrieveInstances("SELECT * FROM "
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

    String fileType = f.getName();
    try {
      m_FromLab.setText("Reading from file...");
      if (f.getName().toLowerCase().endsWith(Instances.FILE_EXTENSION)) {	    
	fileType = "arff";
	Reader r = new BufferedReader(new FileReader(f));
	setInstances(new Instances(r));
	r.close();
      } else if (f.getName().toLowerCase().endsWith(CSVLoader.FILE_EXTENSION)) {
	fileType = "csv";
	CSVLoader cnv = new CSVLoader();
	cnv.setSource(f);
	Instances inst = cnv.getDataSet();
	setInstances(inst);
      } else {
	throw new Exception("Unrecognized file type");
      }
    } catch (Exception ex) {
      m_FromLab.setText("File '" + f.getName() + "' not recognised as an "
			  +fileType+" file.");
      if (JOptionPane.showOptionDialog(ResultsPanel.this,
				       "File '" + f.getName()
				       + "' not recognised as an "
				       +fileType+" file.\n"
				       + "Reason:\n" + ex.getMessage(),
				       "Load Instances",
				       0,
				       JOptionPane.ERROR_MESSAGE,
				       null,
				       new String[] {"OK"},
				       null) == 1) {
	
      }
    }  
  }
  
  /**
   * Returns a vector with column names of the dataset, listed in "list". If
   * a column cannot be found or the list is empty the ones from the default 
   * list are returned. 
   * 
   * @param list           comma-separated list of attribute names
   * @param defaultList    the default list of attribute names
   * @param inst           the instances to get the attribute names from
   * @return               a vector containing attribute names
   */
  protected Vector determineColumnNames(String list, String defaultList, Instances inst) {
    Vector              result;
    Vector              atts;
    StringTokenizer     tok;
    int                 i;
    String              item;
    
    // get attribute names
    atts = new Vector();
    for (i = 0; i < inst.numAttributes(); i++)
      atts.add(inst.attribute(i).name().toLowerCase());

    // process list
    result = new Vector();
    tok = new StringTokenizer(list, ",");
    while (tok.hasMoreTokens()) {
      item = tok.nextToken().toLowerCase();
      if (atts.contains(item)) {
        result.add(item);
      }
      else {
        result.clear();
        break;
      }
    }
    
    // do we have to return defaults?
    if (result.size() == 0) {
      tok = new StringTokenizer(defaultList, ",");
      while (tok.hasMoreTokens())
        result.add(tok.nextToken().toLowerCase());
    }
    
    return result;
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
    
    // setup row and column names
    Vector rows = determineColumnNames(
        ExperimenterDefaults.getRow(), "Key_Dataset", m_Instances);
    Vector cols = determineColumnNames(
        ExperimenterDefaults.getColumn(), "Key_Scheme,Key_Scheme_options,Key_Scheme_version_ID", m_Instances);
    
    // Do other stuff
    m_DatasetKeyModel.removeAllElements();
    m_ResultKeyModel.removeAllElements();
    m_CompareModel.removeAllElements();
    String selectedList = "";
    String selectedListDataset = "";
    boolean comparisonFieldSet = false; 
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      String name = m_Instances.attribute(i).name();
      if (name.toLowerCase().startsWith("key_", 0)) {
	m_DatasetKeyModel.addElement(name.substring(4));
	m_ResultKeyModel.addElement(name.substring(4));
	m_CompareModel.addElement(name.substring(4));
      } else {
	m_DatasetKeyModel.addElement(name);
	m_ResultKeyModel.addElement(name);
	m_CompareModel.addElement(name);
      }

      if (rows.contains(name.toLowerCase())) {
	m_DatasetKeyList.addSelectionInterval(i, i);
	selectedListDataset += "," + (i + 1);
      } else if (name.toLowerCase().equals("key_run")) {
	m_TTester.setRunColumn(i);
      } else if (name.toLowerCase().equals("key_fold")) {
	m_TTester.setFoldColumn(i);
      } else if (cols.contains(name.toLowerCase())) {
	m_ResultKeyList.addSelectionInterval(i, i);
	selectedList += "," + (i + 1);
      } else if (name.toLowerCase().indexOf(ExperimenterDefaults.getComparisonField()) != -1) {
	m_CompareCombo.setSelectedIndex(i);
	comparisonFieldSet = true;
	//	break;
      } else if ((name.toLowerCase().indexOf("root_relative_squared_error") != -1) &&
          (!comparisonFieldSet)) {
	m_CompareCombo.setSelectedIndex(i);
	comparisonFieldSet = true;
      }
    }
    m_DatasetKeyBut.setEnabled(true);
    m_ResultKeyBut.setEnabled(true);
    m_CompareCombo.setEnabled(true);

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

    generatorRange = new Range();
    if (selectedListDataset.length() != 0) {
      try {
	generatorRange.setRanges(selectedListDataset);
      } catch (Exception ex) {
	ex.printStackTrace();
	System.err.println(ex.getMessage());
      }
    }
    m_TTester.setDatasetKeyColumns(generatorRange);

    m_SigTex.setEnabled(true);

    setTTester();
  }

  /**
   * Updates the test chooser with possible tests
   */
  protected void setTTester() {
    
    // default is to display all columns
    m_TTester.setDisplayedResultsets(null);       

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
      /*      if (tname.length() > 20) {
	tname = tname.substring(0, 20);
	} */
      m_TestsModel.addElement(tname);
    }
    
    m_DisplayedModel.removeAllElements();
    for (int i = 0; i < m_TestsModel.size(); i++)
      m_DisplayedModel.addElement(m_TestsModel.elementAt(i));
    
    m_TestsModel.addElement("Summary");
    m_TestsModel.addElement("Ranking");

    m_TestsList.setSelectedIndex(0);
    m_DisplayedList.setSelectionInterval(0, m_DisplayedModel.size() - 1);
    
    m_TestsButton.setEnabled(true);
    m_DisplayedButton.setEnabled(true);
    m_ShowStdDevs.setEnabled(true);
    m_OutputFormatButton.setEnabled(true);
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
      m_TTester.setSignificanceLevel(ExperimenterDefaults.getSignificance());
    }

    // Carry out the test chosen and biff the results to the output area
    m_TTester.setShowStdDevs(m_ShowStdDevs.isSelected());
    int compareCol = m_CompareCombo.getSelectedIndex();
    int tType = m_TestsList.getSelectedIndex();

    String name = (new SimpleDateFormat("HH:mm:ss - "))
      .format(new Date())
      + (String) m_CompareCombo.getSelectedItem() + " - "
      + (String) m_TestsList.getSelectedValue();
    StringBuffer outBuff = new StringBuffer();
    outBuff.append(m_TTester.header(compareCol));
    outBuff.append("\n");
    m_History.addResult(name, outBuff);
    m_History.setSingle(name);
    m_TTester.setDisplayedResultsets(m_DisplayedList.getSelectedIndices());
    m_TTester.setMeanPrec(m_MeanPrec);
    m_TTester.setStdDevPrec(m_StdDevPrec);
    m_TTester.setProduceLatex(m_latexOutput);
    m_TTester.setProduceCSV(m_csvOutput);
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
  
  public void setDatasetKeyFromDialog() {

    ListSelectorDialog jd = new ListSelectorDialog(null, m_DatasetKeyList);

    // Open the dialog
    int result = jd.showDialog();
    
    // If accepted, update the ttester
    if (result == ListSelectorDialog.APPROVE_OPTION) {
      int [] selected = m_DatasetKeyList.getSelectedIndices();
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
      m_TTester.setDatasetKeyColumns(generatorRange);
      setTTester();
    }
  }

  public void setTestBaseFromDialog() {
    ListSelectorDialog jd = new ListSelectorDialog(null, m_TestsList);

    // Open the dialog
    jd.showDialog();
  }

  public void setDisplayedFromDialog() {
    ListSelectorDialog jd = new ListSelectorDialog(null, m_DisplayedList);

    // Open the dialog
    jd.showDialog();
  }
  
  /**
   * displays the Dialog for the output format and sets the chosen settings, 
   * if the user approves
   */
  public void setOutputFormatFromDialog() {
    OutputFormatDialog dialog = new OutputFormatDialog(null);
    
    dialog.setProduceLatex(m_latexOutput);
    dialog.setProduceCSV(m_csvOutput);
    dialog.setMeanPrec(m_MeanPrec);
    dialog.setStdDevPrec(m_StdDevPrec);
    
    if (dialog.showDialog() == OutputFormatDialog.APPROVE_OPTION) {
      m_latexOutput = dialog.getProduceLatex();
      m_csvOutput   = dialog.getProduceCSV();
      m_MeanPrec    = dialog.getMeanPrec();
      m_StdDevPrec  = dialog.getStdDevPrec();
    }
  }

  /**
   * Save the currently selected result buffer to a file.
   */
  protected void saveBuffer() {
    StringBuffer sb = m_History.getSelectedBuffer();
    if (sb != null) {
      if (m_SaveOut.save(sb)) {
	JOptionPane.showMessageDialog(this,
				      "File saved",
				      "Results",
				      JOptionPane.INFORMATION_MESSAGE);
      }
    } else {
      m_SaveOutBut.setEnabled(false);
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
