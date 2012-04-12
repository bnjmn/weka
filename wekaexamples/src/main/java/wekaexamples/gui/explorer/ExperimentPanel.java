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
 *    ExperimentPanel.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.gui.explorer;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instances;
import weka.core.Range;
import weka.core.Utils;
import weka.core.converters.ArffLoader;
import weka.core.converters.Loader;
import weka.core.converters.XRFFLoader;
import weka.core.converters.ConverterUtils.DataSink;
import weka.experiment.ClassifierSplitEvaluator;
import weka.experiment.CrossValidationResultProducer;
import weka.experiment.Experiment;
import weka.experiment.InstancesResultListener;
import weka.experiment.PairedCorrectedTTester;
import weka.experiment.PairedTTester;
import weka.experiment.PropertyNode;
import weka.experiment.RandomSplitResultProducer;
import weka.experiment.RegressionSplitEvaluator;
import weka.experiment.ResultMatrix;
import weka.experiment.ResultMatrixPlainText;
import weka.experiment.SplitEvaluator;
import weka.gui.GenericObjectEditor;
import weka.gui.Logger;
import weka.gui.PropertyPanel;
import weka.gui.ResultHistoryPanel;
import weka.gui.SaveBuffer;
import weka.gui.SysErrLog;
import weka.gui.TaskLogger;
import weka.gui.explorer.Explorer;
import weka.gui.explorer.ExplorerDefaults;
import weka.gui.explorer.Explorer.CapabilitiesFilterChangeEvent;
import weka.gui.explorer.Explorer.CapabilitiesFilterChangeListener;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.gui.explorer.Explorer.LogHandler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/** 
 * This panel allows the user to select and configure a classifier, set the
 * attribute of the current dataset to be used as the class, and perform an
 * Experiment (like in the Experimenter) with this Classifier/Dataset
 * combination. The results of the experiment runs are stored in a result
 * history so that previous results are accessible. <p/>
 * 
 * Based on the ClassifierPanel code (by Len Trigg, Mark Hall and 
 * Richard Kirkby).
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class ExperimentPanel 
  extends JPanel
  implements CapabilitiesFilterChangeListener, ExplorerPanel, LogHandler {
   
  /** for serialization. */
  private static final long serialVersionUID = 2078066653508312179L;

  /** the parent frame. */
  protected Explorer m_Explorer = null;

  /** Lets the user configure the classifier. */
  protected GenericObjectEditor m_ClassifierEditor = new GenericObjectEditor();

  /** The panel showing the current classifier selection. */
  protected PropertyPanel m_CEPanel = new PropertyPanel(m_ClassifierEditor);
  
  /** The output area for classification results. */
  protected JTextArea m_OutText = new JTextArea(20, 40);

  /** The destination for log/status messages. */
  protected Logger m_Log = new SysErrLog();

  /** The buffer saving object for saving output. */
  protected SaveBuffer m_SaveOut = new SaveBuffer(m_Log, this);

  /** A panel controlling results viewing. */
  protected ResultHistoryPanel m_History = new ResultHistoryPanel(m_OutText);

  /** The spinner for the number of runs. */
  protected JSpinner m_RunsSpinner = new JSpinner();
  
  /** The type of evaluation: cross-validation or random split. */
  protected JComboBox m_EvalCombo = new JComboBox(new String[]{"Cross-validation", "Random split"});

  /** The label for either the number of folds or the percentage for the random split. */
  protected JLabel m_FoldsPercLabel = new JLabel("Folds");

  /** Either the number of folds or the percentage for the random split. */
  protected JTextField m_FoldsPercText = new JTextField("10", 10);

  /** Lets the user select the class column. */
  protected JComboBox m_ClassCombo = new JComboBox();
  
  /** Click to start running the experiment. */
  protected JButton m_StartBut = new JButton("Start");

  /** Click to stop a running experiment. */
  protected JButton m_StopBut = new JButton("Stop");

  /** Stop the class combo from taking up to much space. */
  private Dimension COMBO_SIZE = new Dimension(100, m_StartBut.getPreferredSize().height);

  /** The main set of instances we're playing with. */
  protected Instances m_Instances;

  /** The loader used to load the user-supplied test set (if any). */
  protected Loader m_TestLoader;
  
  /** A thread that classification runs in. */
  protected Thread m_RunThread;

  // Register the property editors we need
  static {
     GenericObjectEditor.registerEditors();
  }
  
  /**
   * Creates the Experiment panel.
   */
  public ExperimentPanel() {
    m_OutText.setEditable(false);
    m_OutText.setFont(new Font("Monospaced", Font.PLAIN, 12));
    m_OutText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_OutText.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
	if ((e.getModifiers() & InputEvent.BUTTON1_MASK)
	    != InputEvent.BUTTON1_MASK) {
	  m_OutText.selectAll();
	}
      }
    });
    
    m_History.setBorder(BorderFactory.createTitledBorder("Result list (right-click for options)"));

    m_ClassifierEditor.setClassType(Classifier.class);
    m_ClassifierEditor.setValue(ExplorerDefaults.getClassifier());
    m_ClassifierEditor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	repaint();
      }
    });

    m_RunsSpinner.setToolTipText("The number of runs to perform");
    m_RunsSpinner.setEnabled(false);
    ((SpinnerNumberModel) m_RunsSpinner.getModel()).setMinimum(new Integer(1));
    ((SpinnerNumberModel) m_RunsSpinner.getModel()).setValue(new Integer(10));

    m_EvalCombo.setToolTipText("The type of evaluation to be performed");
    m_EvalCombo.setEnabled(false);
    m_EvalCombo.setPreferredSize(COMBO_SIZE);
    m_EvalCombo.setMaximumSize(COMBO_SIZE);
    m_EvalCombo.setMinimumSize(COMBO_SIZE);
    m_EvalCombo.setSelectedIndex(0);
    m_EvalCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	int selected = m_EvalCombo.getSelectedIndex();
	if (selected == 0) {
	  m_FoldsPercLabel.setText("Folds");
	  m_FoldsPercText.setText("10");
	}
	else if (selected == 1) {
	  m_FoldsPercLabel.setText("Percentage");
	  m_FoldsPercText.setText("66");
	}
      }
    });

    m_FoldsPercText.setToolTipText("Folds for cross-validation, percentage for random split");
    m_FoldsPercText.setEnabled(false);
    
    m_ClassCombo.setToolTipText("Select the attribute to use as the class");
    m_ClassCombo.setEnabled(false);
    m_ClassCombo.setPreferredSize(COMBO_SIZE);
    m_ClassCombo.setMaximumSize(COMBO_SIZE);
    m_ClassCombo.setMinimumSize(COMBO_SIZE);
    m_ClassCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	updateCapabilitiesFilter(m_ClassifierEditor.getCapabilitiesFilter());
      }
    });

    m_StartBut.setToolTipText("Starts the experiment");
    m_StartBut.setEnabled(false);
    m_StartBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	startExperiment();
      }
    });
    
    m_StopBut.setToolTipText("Stops a running experiment");
    m_StopBut.setEnabled(false);
    m_StopBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	stopExperiment();
      }
    });
   
    m_History.setHandleRightClicks(false);
    // see if we can popup a menu for the selected result
    m_History.getList().addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
	if (((e.getModifiers() & InputEvent.BUTTON1_MASK)
	    != InputEvent.BUTTON1_MASK) || e.isAltDown()) {
	  int index = m_History.getList().locationToIndex(e.getPoint());
	  if (index != -1) {
	    String name = m_History.getNameAtIndex(index);
	    showPopup(name, e.getX(), e.getY());
	  } else {
	    showPopup(null, e.getX(), e.getY());
	  }
	}
      }
    });

    // Layout the GUI
    JPanel p1 = new JPanel();
    p1.setBorder(
	BorderFactory.createCompoundBorder(
	    BorderFactory.createTitledBorder("Classifier"),
	    BorderFactory.createEmptyBorder(0, 5, 5, 5)));
    p1.setLayout(new BorderLayout());
    p1.add(m_CEPanel, BorderLayout.NORTH);

    JPanel p2 = new JPanel();
    GridBagLayout gbL = new GridBagLayout();
    p2.setLayout(gbL);
    p2.setBorder(
	BorderFactory.createCompoundBorder(
	    BorderFactory.createTitledBorder("Experiment options"),
	    BorderFactory.createEmptyBorder(0, 5, 5, 5)));

    GridBagConstraints gbC;
    JLabel label;

    // Runs
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 0;
    gbC.gridx = 0;
    gbC.insets = new Insets(2, 5, 2, 5);
    label = new JLabel("Runs");
    gbL.setConstraints(label, gbC);
    p2.add(label);
    
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 0;
    gbC.gridx = 1;
    gbC.weightx = 100;
    gbC.ipadx = 20;
    gbC.insets = new Insets(2, 5, 2, 5);
    gbL.setConstraints(m_RunsSpinner, gbC);
    p2.add(m_RunsSpinner);
    
    // Evaluation
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 1;
    gbC.gridx = 0;
    gbC.insets = new Insets(2, 5, 2, 5);
    label = new JLabel("Evaluation");
    gbL.setConstraints(label, gbC);
    p2.add(label);
    
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 1;
    gbC.gridx = 1;
    gbC.weightx = 100;
    gbC.ipadx = 20;
    gbC.insets = new Insets(2, 5, 2, 5);
    gbL.setConstraints(m_EvalCombo, gbC);
    p2.add(m_EvalCombo);
    
    // folds/percentage
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 2;
    gbC.gridx = 0;
    gbC.insets = new Insets(2, 5, 2, 5);
    gbL.setConstraints(m_FoldsPercLabel, gbC);
    p2.add(m_FoldsPercLabel);
    
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 2;
    gbC.gridx = 1;
    gbC.weightx = 100;
    gbC.ipadx = 20;
    gbC.insets = new Insets(2, 5, 2, 5);
    gbL.setConstraints(m_FoldsPercText, gbC);
    p2.add(m_FoldsPercText);
    
    JPanel buttons = new JPanel();
    buttons.setLayout(new GridLayout(2, 2));
    buttons.add(m_ClassCombo);
    m_ClassCombo.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    JPanel ssButs = new JPanel();
    ssButs.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    ssButs.setLayout(new GridLayout(1, 2, 5, 5));
    ssButs.add(m_StartBut);
    ssButs.add(m_StopBut);

    buttons.add(ssButs);
    
    JPanel p3 = new JPanel();
    p3.setBorder(BorderFactory.createTitledBorder("Experiment output"));
    p3.setLayout(new BorderLayout());
    final JScrollPane js = new JScrollPane(m_OutText);
    p3.add(js, BorderLayout.CENTER);
    js.getViewport().addChangeListener(new ChangeListener() {
      private int lastHeight;
      public void stateChanged(ChangeEvent e) {
	JViewport vp = (JViewport)e.getSource();
	int h = vp.getViewSize().height; 
	if (h != lastHeight) { // i.e. an addition not just a user scrolling
	  lastHeight = h;
	  int x = h - vp.getExtentSize().height;
	  vp.setViewPosition(new Point(0, x));
	}
      }
    });
    
    JPanel mondo = new JPanel();
    gbL = new GridBagLayout();
    mondo.setLayout(gbL);
    gbC = new GridBagConstraints();
    //    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 0;     gbC.gridx = 0;
    gbL.setConstraints(p2, gbC);
    mondo.add(p2);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.NORTH;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 1;     gbC.gridx = 0;
    gbL.setConstraints(buttons, gbC);
    mondo.add(buttons);
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
    gbL.setConstraints(p3, gbC);
    mondo.add(p3);

    setLayout(new BorderLayout());
    add(p1, BorderLayout.NORTH);
    add(mondo, BorderLayout.CENTER);
  }

  /**
   * Sets the Logger to receive informational messages.
   *
   * @param newLog 	the Logger that will now get info messages
   */
  public void setLog(Logger newLog) {
    m_Log = newLog;
  }

  /**
   * Tells the panel to use a new set of instances.
   *
   * @param inst 	a set of Instances
   */
  public void setInstances(Instances inst) {
    m_Instances = inst;

    String[] attribNames = new String [m_Instances.numAttributes()];
    for (int i = 0; i < attribNames.length; i++) {
      String type = "";
      switch (m_Instances.attribute(i).type()) {
      case Attribute.NOMINAL:
	type = "(Nom) ";
	break;
      case Attribute.NUMERIC:
	type = "(Num) ";
	break;
      case Attribute.STRING:
	type = "(Str) ";
	break;
      case Attribute.DATE:
	type = "(Dat) ";
	break;
      case Attribute.RELATIONAL:
	type = "(Rel) ";
	break;
      default:
	type = "(???) ";
      }
      attribNames[i] = type + m_Instances.attribute(i).name();
    }
    m_ClassCombo.setModel(new DefaultComboBoxModel(attribNames));
    if (attribNames.length > 0) {
      if (inst.classIndex() == -1)
	m_ClassCombo.setSelectedIndex(attribNames.length - 1);
      else
	m_ClassCombo.setSelectedIndex(inst.classIndex());
      m_ClassCombo.setEnabled(true);
      m_EvalCombo.setEnabled(true);
      m_RunsSpinner.setEnabled(true);
      m_FoldsPercText.setEnabled(true);
      m_StartBut.setEnabled(m_RunThread == null);
      m_StopBut.setEnabled(m_RunThread != null);
    }
    else {
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(false);
    }
  }

  /**
   * Handles constructing a popup menu with visualization options.
   * 
   * @param name 	the name of the result history list entry clicked on by
   * 			the user
   * @param x 		the x coordinate for popping up the menu
   * @param y 		the y coordinate for popping up the menu
   */
  protected void showPopup(String name, int x, int y) {
    final String selectedName = name;
    JPopupMenu resultListMenu = new JPopupMenu();
    
    JMenuItem viewMainBuffer = new JMenuItem("View in main window");
    if (selectedName != null) {
      viewMainBuffer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_History.setSingle(selectedName);
	}
      });
    }
    else {
      viewMainBuffer.setEnabled(false);
    }
    resultListMenu.add(viewMainBuffer);

    JMenuItem viewSepBuffer = new JMenuItem("View in separate window");
    if (selectedName != null) {
      viewSepBuffer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_History.openFrame(selectedName);
	}
      });
    }
    else {
      viewSepBuffer.setEnabled(false);
    }
    resultListMenu.add(viewSepBuffer);

    JMenuItem saveOutput = new JMenuItem("Save result buffer");
    if (selectedName != null) {
      saveOutput.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  saveBuffer(selectedName);
	}
      });
    }
    else {
      saveOutput.setEnabled(false);
    }
    resultListMenu.add(saveOutput);

    JMenuItem deleteOutput = new JMenuItem("Delete result buffer");
    if (selectedName != null) {
      deleteOutput.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_History.removeResult(selectedName);
	}
      });
    }
    else {
      deleteOutput.setEnabled(false);
    }
    resultListMenu.add(deleteOutput);

    resultListMenu.show(m_History.getList(), x, y);
  }

  /**
   * Starts running the currently configured classifier with the current
   * settings in an experiment. This is run in a separate thread, and will 
   * only start if there is no experiment already running. The experiment 
   * output is sent to the results history panel.
   */
  protected void startExperiment() {
    if (m_RunThread == null) {
      synchronized (this) {
	m_StartBut.setEnabled(false);
	m_StopBut.setEnabled(true);
      }
      
      m_RunThread = new Thread() {
	public void run() {
	  // set up everything:
	  m_Log.statusMessage("Setting up...");
	  
	  // 1. save instances to tmp file
	  Instances inst = new Instances(m_Instances);
	  int classIndex = m_ClassCombo.getSelectedIndex();
	  inst.setClassIndex(classIndex);
	  File tmpDataset = null;
	  try {
	    tmpDataset = File.createTempFile("weka_", XRFFLoader.FILE_EXTENSION_COMPRESSED);
	    tmpDataset.deleteOnExit();
	    DataSink.write(tmpDataset.getAbsolutePath(), inst);
	  }
	  catch (Exception ex) {
	    m_Log.logMessage("Problem saving instances to tmp. file: " + ex.getMessage());
	  }

	  Experiment exp = new Experiment();
	  exp.setPropertyArray(new Classifier[0]);
	  exp.setUsePropertyIterator(true);

	  // classification or regression
	  SplitEvaluator se = null;
	  Classifier sec    = null;
	  if (inst.classAttribute().isNominal()) {
	    se  = new ClassifierSplitEvaluator();
	    sec = ((ClassifierSplitEvaluator) se).getClassifier();
	  }
	  else if (inst.classAttribute().isNumeric()) {
	    se  = new RegressionSplitEvaluator();
	    sec = ((RegressionSplitEvaluator) se).getClassifier();
	  }
	  else {
	    throw new IllegalArgumentException("Unknown evaluation type!");
	  }

	  // crossvalidation or randomsplit
	  if (m_EvalCombo.getSelectedIndex() == 0) {
	    CrossValidationResultProducer cvrp = new CrossValidationResultProducer();
	    cvrp.setNumFolds(Integer.parseInt(m_FoldsPercText.getText()));
	    cvrp.setSplitEvaluator(se);

	    PropertyNode[] propertyPath = new PropertyNode[2];
	    try {
	      propertyPath[0] = new PropertyNode(
		  se, 
		  new PropertyDescriptor("splitEvaluator",
		      CrossValidationResultProducer.class),
		      CrossValidationResultProducer.class);
	      propertyPath[1] = new PropertyNode(
		  sec, 
		  new PropertyDescriptor("classifier",
		      se.getClass()),
		      se.getClass());
	    }
	    catch (IntrospectionException e) {
	      e.printStackTrace();
	    }

	    exp.setResultProducer(cvrp);
	    exp.setPropertyPath(propertyPath);

	  }
	  else if (m_EvalCombo.getSelectedIndex() == 1) {
	    RandomSplitResultProducer rsrp = new RandomSplitResultProducer();
	    rsrp.setRandomizeData(true);
	    rsrp.setTrainPercent(Double.parseDouble(m_FoldsPercText.getText()));
	    rsrp.setSplitEvaluator(se);

	    PropertyNode[] propertyPath = new PropertyNode[2];
	    try {
	      propertyPath[0] = new PropertyNode(
		  se, 
		  new PropertyDescriptor("splitEvaluator",
		      RandomSplitResultProducer.class),
		      RandomSplitResultProducer.class);
	      propertyPath[1] = new PropertyNode(
		  sec, 
		  new PropertyDescriptor("classifier",
		      se.getClass()),
		      se.getClass());
	    }
	    catch (IntrospectionException e) {
	      e.printStackTrace();
	    }

	    exp.setResultProducer(rsrp);
	    exp.setPropertyPath(propertyPath);
	  }
	  else {
	    throw new IllegalArgumentException("Unknown evaluation type!");
	  }

	  // runs
	  exp.setRunLower(1);
	  exp.setRunUpper((Integer) m_RunsSpinner.getValue());

	  // classifier
	  try {
	    exp.setPropertyArray(new Classifier[]{Classifier.makeCopy((Classifier) m_ClassifierEditor.getValue())}); 
	  }
	  catch (Exception ex) {
	    m_Log.logMessage("Problem creating copy of classifier: " + ex.getMessage());
	  }

	  // datasets
	  DefaultListModel model = new DefaultListModel();
	  model.addElement(tmpDataset);
	  exp.setDatasets(model);

	  // result
	  InstancesResultListener irl = new InstancesResultListener();
	  File tmpResult = null;
	  try {
	    tmpResult = File.createTempFile("weka_result_", ArffLoader.FILE_EXTENSION);
	    tmpResult.deleteOnExit();
	  }
	  catch (Exception ex) {
	    m_Log.logMessage("Problem creating tmp file for experiment result: " + ex.getMessage());
	  }
	  irl.setOutputFile(tmpResult);
	  exp.setResultListener(irl);

	  try {
	    m_Log.logMessage("Started experiment for " + m_ClassifierEditor.getValue().getClass().getName());
	    if (m_Log instanceof TaskLogger)
	      ((TaskLogger)m_Log).taskStarted();

	    // running the experiment
	    m_Log.statusMessage("Experiment started...");
	    exp.initialize();
	    exp.runExperiment();
	    exp.postProcess();
	    
	    // evaluating
	    m_Log.statusMessage("Evaluating experiment...");
	    Instances result = new Instances(
				new BufferedReader(
				    new FileReader(irl.getOutputFile())));
	    PairedTTester tester = new PairedCorrectedTTester();
	    tester.setInstances(result);
	    tester.setSortColumn(-1);
	    tester.setRunColumn(result.attribute("Key_Run").index());
	    if (inst.classAttribute().isNominal())
	      tester.setFoldColumn(result.attribute("Key_Fold").index());
	    tester.setResultsetKeyColumns(
		new Range(
		    "" 
		    + (result.attribute("Key_Dataset").index() + 1)));
	    tester.setDatasetKeyColumns(
		new Range(
		    "" 
		    + (result.attribute("Key_Scheme").index() + 1)
		    + ","
		    + (result.attribute("Key_Scheme_options").index() + 1)
		    + ","
		    + (result.attribute("Key_Scheme_version_ID").index() + 1)));
	    tester.setResultMatrix(new ResultMatrixPlainText());
	    tester.setDisplayedResultsets(null);       
	    tester.setSignificanceLevel(0.05);  // irrelevant, since only 1 scheme on 1 dataset
	    tester.setShowStdDevs(true);

	    // retrieve the results
	    int startIndex = result.attribute("Date_time").index() + 1;  // start past "Date_time"
	    int decimals = 4;
	    int width = 12;
	    int[] widths = new int[3];
	    String[] values;
	    Vector<String[]> list = new Vector<String[]>();
	    list.add(new String[]{"Measure", "Mean", "StdDev"});
	    list.add(new String[]{"=======", "====", "======"});
	    for (int i = startIndex; i < result.numAttributes(); i++) {
	      if (!result.attribute(i).isNumeric())
		continue;
	      values = new String[3];
	      list.add(values);
	      tester.multiResultsetFull(0, i);
	      ResultMatrix matrix = tester.getResultMatrix();
	      values[0] = result.attribute(i).name();
	      values[1] = Utils.doubleToString(matrix.getMean(0, 0), width, decimals);
	      values[2] = Utils.doubleToString(matrix.getStdDev(0, 0), width, decimals);
	      // record widths
	      for (int n = 0; n < 3; n++) {
		if (widths[n] < values[0].length())
		  widths[n] = values[0].length();
	      }
	    }

	    // pad and assemble values
	    StringBuffer outBuff = new StringBuffer();
	    outBuff.append("Runs......: " + m_RunsSpinner.getValue() + "\n");
	    outBuff.append("Evaluation: " + m_EvalCombo.getSelectedItem() + "\n");
	    if (m_EvalCombo.getSelectedIndex() == 0)
	      outBuff.append("Folds.....: ");
	    else
	      outBuff.append("Percentage: ");
	    outBuff.append(m_FoldsPercText.getText() + "\n\n\n");
	    for (int i = 0; i < list.size(); i++) {
	      values = list.get(i);
	      for (int n = 0; n < values.length; n++) {
		if (n > 0) {
		  for (int m = values[n].length(); m < widths[n]; m++)
		    outBuff.append(" ");
		}
		outBuff.append(values[n]);
		if (n == 0) {
		  for (int m = values[n].length(); m < widths[n]; m++)
		    outBuff.append(" ");
		}
	      }
	      outBuff.append("\n");
	    }

	    String name = m_ClassifierEditor.getValue().getClass().getName().replaceAll("weka\\.classifiers\\.", "");
	    SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
	    name = df.format(new Date()) + " - " + name;
	    m_History.addResult(name, outBuff);
	    m_History.setSingle(name);
	    m_Log.statusMessage("Experiment finished.");
	    m_Log.statusMessage("OK");
	  }
	  catch (Exception ex) {
	    ex.printStackTrace();
	    m_Log.logMessage(ex.getMessage());
	    JOptionPane.showMessageDialog(
		ExperimentPanel.this,
		"Problem running experiment:\n" + ex.getMessage(),
		"Running experiment",
		JOptionPane.ERROR_MESSAGE);
	    m_Log.statusMessage("Problem running experiment");
	  }
	  finally {
	    synchronized (this) {
	      m_StartBut.setEnabled(true);
	      m_StopBut.setEnabled(false);
	      m_RunThread = null;
	    }
	    
	    if (m_Log instanceof TaskLogger)
              ((TaskLogger)m_Log).taskFinished();
	  }
	}
      };
      m_RunThread.setPriority(Thread.MIN_PRIORITY);
      m_RunThread.start();
    }
  }

  /**
   * Save the currently selected experiment output to a file.
   * 
   * @param name 	the name of the buffer to save
   */
  protected void saveBuffer(String name) {
    StringBuffer sb = m_History.getNamedBuffer(name);
    if (sb != null) {
      if (m_SaveOut.save(sb))
	m_Log.logMessage("Save successful.");
    }
  }

  /**
   * Stops the currently running experiment (if any).
   */
  protected void stopExperiment() {
    if (m_RunThread != null) {
      m_RunThread.interrupt();
      
      // This is deprecated (and theoretically the interrupt should do).
      m_RunThread.stop();
    }
  }
  
  /**
   * updates the capabilities filter of the GOE.
   * 
   * @param filter	the new filter to use
   */
  protected void updateCapabilitiesFilter(Capabilities filter) {
    Instances 		tempInst;
    Capabilities 	filterClass;

    if (filter == null) {
      m_ClassifierEditor.setCapabilitiesFilter(new Capabilities(null));
      return;
    }
    
    if (!ExplorerDefaults.getInitGenericObjectEditorFilter())
      tempInst = new Instances(m_Instances, 0);
    else
      tempInst = new Instances(m_Instances);
    tempInst.setClassIndex(m_ClassCombo.getSelectedIndex());

    try {
      filterClass = Capabilities.forInstances(tempInst);
    }
    catch (Exception e) {
      filterClass = new Capabilities(null);
    }
    
    // set new filter
    m_ClassifierEditor.setCapabilitiesFilter(filterClass);
  }
  
  /**
   * method gets called in case of a change event.
   * 
   * @param e		the associated change event
   */
  public void capabilitiesFilterChanged(CapabilitiesFilterChangeEvent e) {
    if (e.getFilter() == null)
      updateCapabilitiesFilter(null);
    else
      updateCapabilitiesFilter((Capabilities) e.getFilter().clone());
  }

  /**
   * Sets the Explorer to use as parent frame (used for sending notifications
   * about changes in the data).
   * 
   * @param parent	the parent frame
   */
  public void setExplorer(Explorer parent) {
    m_Explorer = parent;
  }
  
  /**
   * returns the parent Explorer frame.
   * 
   * @return		the parent
   */
  public Explorer getExplorer() {
    return m_Explorer;
  }
  
  /**
   * Returns the title for the tab in the Explorer.
   * 
   * @return 		the title of this tab
   */
  public String getTabTitle() {
    return "Experiment";
  }
  
  /**
   * Returns the tooltip for the tab in the Explorer.
   * 
   * @return 		the tooltip of this tab
   */
  public String getTabTitleToolTip() {
    return "Perform experiments";
  }
  
  /**
   * Tests out the Experiment panel from the command line.
   *
   * @param args 	may optionally contain the name of a dataset to load.
   */
  public static void main(String[] args) {
    try {
      final javax.swing.JFrame jf = new javax.swing.JFrame("Weka Explorer: Experiment");
      jf.getContentPane().setLayout(new BorderLayout());
      final ExperimentPanel sp = new ExperimentPanel();
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      weka.gui.LogPanel lp = new weka.gui.LogPanel();
      sp.setLog(lp);
      jf.getContentPane().add(lp, BorderLayout.SOUTH);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setSize(800, 600);
      jf.setVisible(true);
      if (args.length == 1) {
	System.err.println("Loading instances from " + args[0]);
	Reader r = new java.io.BufferedReader(new java.io.FileReader(args[0]));
	Instances i = new Instances(r);
	sp.setInstances(i);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
