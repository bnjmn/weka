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
 *    ClustererPanel.java
 *    Copyright (C) 1999 Mark Hall
 *
 */


package weka.gui.explorer;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.OptionHandler;
import weka.core.Attribute;
import weka.core.Utils;
import weka.core.FastVector;
import weka.core.SerializedObject;
import weka.clusterers.Clusterer;
import weka.clusterers.ClusterEvaluation;
import weka.gui.Logger;
import weka.gui.TaskLogger;
import weka.gui.SysErrLog;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;
import weka.gui.ResultHistoryPanel;
import weka.gui.SetInstancesPanel;
import weka.gui.InstancesSummaryPanel;
import weka.gui.SaveBuffer;
import weka.filters.Filter;
import weka.filters.AttributeFilter;
import weka.gui.visualize.VisualizePanel;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.Plot2D;
import weka.gui.treevisualizer.*;
import weka.gui.ListSelectorDialog;

import java.util.Random;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JFrame;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JViewport;
import javax.swing.JCheckBox;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Point;
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.DefaultListModel;
import javax.swing.JList;

/** 
 * This panel allows the user to select and configure a clusterer, and evaluate
 * the clusterer using a number of testing modes (test on the training data,
 * train/test on a percentage split, test on a
 * separate split). The results of clustering runs are stored in a result
 * history so that previous results are accessible.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.30 $
 */
public class ClustererPanel extends JPanel {

  /** Lets the user configure the clusterer */
  protected GenericObjectEditor m_ClustererEditor =
    new GenericObjectEditor();

  /** The panel showing the current clusterer selection */
  protected PropertyPanel m_CLPanel = new PropertyPanel(m_ClustererEditor);
  
  /** The output area for classification results */
  protected JTextArea m_OutText = new JTextArea(20, 40);

  /** The destination for log/status messages */
  protected Logger m_Log = new SysErrLog();

  /** The buffer saving object for saving output */
  SaveBuffer m_SaveOut = new SaveBuffer(m_Log, this);

  /** A panel controlling results viewing */
  protected ResultHistoryPanel m_History = new ResultHistoryPanel(m_OutText);

  /** Click to set test mode to generate a % split */
  protected JRadioButton m_PercentBut = new JRadioButton("Percentage split");

  /** Click to set test mode to test on training data */
  protected JRadioButton m_TrainBut = new JRadioButton("Use training set");

  /** Click to set test mode to a user-specified test set */
  protected JRadioButton m_TestSplitBut =
    new JRadioButton("Supplied test set");

  /** Click to set test mode to classes to clusters based evaluation */
  protected JRadioButton m_ClassesToClustersBut = 
    new JRadioButton("Classes to clusters evaluation");

  /** Lets the user select the class column for classes to clusters based
      evaluation */
  protected JComboBox m_ClassCombo = new JComboBox();

  /** Label by where the % split is entered */
  protected JLabel m_PercentLab = new JLabel("%", SwingConstants.RIGHT);

  /** The field where the % split is entered */
  protected JTextField m_PercentText = new JTextField("66");

  /** The button used to open a separate test dataset */
  protected JButton m_SetTestBut = new JButton("Set...");

  /** The frame used to show the test set selection panel */
  protected JFrame m_SetTestFrame;

  /** The button used to popup a list for choosing attributes to ignore while
      clustering */
  protected JButton m_ignoreBut = new JButton("Ignore attributes");

  protected DefaultListModel m_ignoreKeyModel = new DefaultListModel();
  protected JList m_ignoreKeyList = new JList(m_ignoreKeyModel);

  //  protected AttributeFilter m_ignoreFilter = null;
  
  /**
   * Alters the enabled/disabled status of elements associated with each
   * radio button
   */
  ActionListener m_RadioListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      updateRadioLinks();
    }
  };

  /** Click to start running the clusterer */
  protected JButton m_StartBut = new JButton("Start");

  /** Stop the class combo from taking up to much space */
  private Dimension COMBO_SIZE = new Dimension(250, m_StartBut
					       .getPreferredSize().height);

  /** Click to stop a running clusterer */
  protected JButton m_StopBut = new JButton("Stop");

  /** The main set of instances we're playing with */
  protected Instances m_Instances;

  /** The user-supplied test set (if any) */
  protected Instances m_TestInstances;

  /** The user supplied test set after preprocess filters have been applied */
  protected Instances m_TestInstancesCopy;

  /** The current visualization object */
  protected VisualizePanel m_CurrentVis = null;

  /** default x index for visualizing */
  protected int m_visXIndex;
  
  /** default y index for visualizing */
  protected int m_visYIndex;

  /** Check to save the predictions in the results list for visualizing
      later on */
  protected JCheckBox m_StorePredictionsBut = 
    new JCheckBox("Store clusters for visualization");
  
  /** A thread that clustering runs in */
  protected Thread m_RunThread;

  /** The pre-process object from which to fetch filters for applying
      to a user specified test set */
  protected PreprocessPanel m_Preprocess;
  
  /** The instances summary panel displayed by m_SetTestFrame */
  protected InstancesSummaryPanel m_Summary;

  /* Register the property editors we need */
  static {
    java.beans.PropertyEditorManager
      .registerEditor(weka.core.SelectedTag.class,
		      weka.gui.SelectedTagEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.clusterers.Clusterer.class,
		      weka.gui.GenericObjectEditor.class);
  }
  
  /**
   * Creates the clusterer panel
   */
  public ClustererPanel() {

    // Connect / configure the components
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
    m_History.setBorder(BorderFactory.createTitledBorder("Result list"));
    m_ClustererEditor.setClassType(Clusterer.class);
    m_ClustererEditor.setValue(new weka.clusterers.EM());
    m_ClustererEditor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	repaint();
      }
    });

    m_TrainBut.setToolTipText("Cluster the same set that the clusterer"
			      + " is trained on");
    m_PercentBut.setToolTipText("Train on a percentage of the data and"
				+ " cluster the remainder");
    m_TestSplitBut.setToolTipText("Cluster a user-specified dataset");
    m_ClassesToClustersBut.setToolTipText("Evaluate clusters with respect to a"
					  +" class");
    m_ClassCombo.setToolTipText("Select the class attribute for class based"
				+" evaluation");
    m_StartBut.setToolTipText("Starts the clustering");
    m_StopBut.setToolTipText("Stops a running clusterer");
    m_StorePredictionsBut.
      setToolTipText("Store predictions in the result list for later "
		     +"visualization");
    m_ignoreBut.setToolTipText("Ignore attributes during clustering");

    m_ClassCombo.setPreferredSize(COMBO_SIZE);
    m_ClassCombo.setMaximumSize(COMBO_SIZE);
    m_ClassCombo.setMinimumSize(COMBO_SIZE);
    m_ClassCombo.setEnabled(false);

    m_TrainBut.setSelected(true);
    m_StorePredictionsBut.setSelected(true);
    updateRadioLinks();
    ButtonGroup bg = new ButtonGroup();
    bg.add(m_TrainBut);
    bg.add(m_PercentBut);
    bg.add(m_TestSplitBut);
    bg.add(m_ClassesToClustersBut);
    m_TrainBut.addActionListener(m_RadioListener);
    m_PercentBut.addActionListener(m_RadioListener);
    m_TestSplitBut.addActionListener(m_RadioListener);
    m_ClassesToClustersBut.addActionListener(m_RadioListener);
    m_SetTestBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setTestSet();
      }
    });

    m_StartBut.setEnabled(false);
    m_StopBut.setEnabled(false);
    m_ignoreBut.setEnabled(false);
    m_StartBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	startClusterer();
      }
    });
    m_StopBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	stopClusterer();
      }
    });

    m_ignoreBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  setIgnoreColumns();
	}
      });
   
    m_History.setHandleRightClicks(false);
    // see if we can popup a menu for the selected result
    m_History.getList().addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent e) {
	  if ((e.getModifiers() & InputEvent.BUTTON1_MASK)
	      == InputEvent.BUTTON1_MASK) {
	  } else {
	    int index = m_History.getList().locationToIndex(e.getPoint());
	    if (index != -1) {
	      String name = m_History.getNameAtIndex(index);
	      visualizeClusterer(name, e.getX(), e.getY());
	    }
	  }
	}
      });

    // Layout the GUI
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createCompoundBorder(
		 BorderFactory.createTitledBorder("Clusterer"),
		 BorderFactory.createEmptyBorder(0, 5, 5, 5)
		 ));
    p1.setLayout(new BorderLayout());
    p1.add(m_CLPanel, BorderLayout.NORTH);

    JPanel p2 = new JPanel();
    GridBagLayout gbL = new GridBagLayout();
    p2.setLayout(gbL);
    p2.setBorder(BorderFactory.createCompoundBorder(
		 BorderFactory.createTitledBorder("Cluster mode"),
		 BorderFactory.createEmptyBorder(0, 5, 5, 5)
		 ));
    GridBagConstraints gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 0;     gbC.gridx = 0;
    gbL.setConstraints(m_TrainBut, gbC);
    p2.add(m_TrainBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 1;     gbC.gridx = 0;
    gbL.setConstraints(m_TestSplitBut, gbC);
    p2.add(m_TestSplitBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 1;     gbC.gridx = 1;    gbC.gridwidth = 2;
    gbC.insets = new Insets(2, 10, 2, 0);
    gbL.setConstraints(m_SetTestBut, gbC);
    p2.add(m_SetTestBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 2;     gbC.gridx = 0;
    gbL.setConstraints(m_PercentBut, gbC);
    p2.add(m_PercentBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 2;     gbC.gridx = 1;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(m_PercentLab, gbC);
    p2.add(m_PercentLab);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 2;     gbC.gridx = 2;  gbC.weightx = 100;
    gbC.ipadx = 20;
    gbL.setConstraints(m_PercentText, gbC);
    p2.add(m_PercentText);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 3;     gbC.gridx = 0;  gbC.gridwidth = 2;
    gbL.setConstraints(m_ClassesToClustersBut, gbC);
    p2.add(m_ClassesToClustersBut);

    m_ClassCombo.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 4;     gbC.gridx = 0;  gbC.gridwidth = 2;
    gbL.setConstraints(m_ClassCombo, gbC);
    p2.add(m_ClassCombo);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 5;     gbC.gridx = 0;  gbC.gridwidth = 2;
    gbL.setConstraints(m_StorePredictionsBut, gbC);
    p2.add(m_StorePredictionsBut);

    JPanel buttons = new JPanel();
    buttons.setLayout(new GridLayout(2, 1));
    JPanel ssButs = new JPanel();
    ssButs.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    ssButs.setLayout(new GridLayout(1, 2, 5, 5));
    ssButs.add(m_StartBut);
    ssButs.add(m_StopBut);
    JPanel ib = new JPanel();
    ib.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    ib.setLayout(new GridLayout(1, 1, 5, 5));
    ib.add(m_ignoreBut);
    buttons.add(ib);
    buttons.add(ssButs);
    
    JPanel p3 = new JPanel();
    p3.setBorder(BorderFactory.createTitledBorder("Clusterer output"));
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
   * Updates the enabled status of the input fields and labels.
   */
  protected void updateRadioLinks() {
    
    m_SetTestBut.setEnabled(m_TestSplitBut.isSelected());
    if ((m_SetTestFrame != null) && (!m_TestSplitBut.isSelected())) {
      m_SetTestFrame.setVisible(false);
    }
    m_PercentText.setEnabled(m_PercentBut.isSelected());
    m_PercentLab.setEnabled(m_PercentBut.isSelected());
    m_ClassCombo.setEnabled(m_ClassesToClustersBut.isSelected());
  }

  /**
   * Sets the preprocess panel through which user selected
   * filters can be applied to any supplied test data
   * @param p the preprocess panel to use
   */
  public void setPreprocess(PreprocessPanel p) {
    m_Preprocess = p;
  }

  /**
   * Sets the Logger to receive informational messages
   *
   * @param newLog the Logger that will now get info messages
   */
  public void setLog(Logger newLog) {

    m_Log = newLog;
  }
  
  /**
   * Set the default attributes to use on the x and y axis
   * of a new visualization object.
   * @param x the index of the attribute to use on the x axis
   * @param y the index of the attribute to use on the y axis
   */
  public void setXY_VisualizeIndexes(int x, int y) {
    m_visXIndex = x;
    m_visYIndex = y;
  }

  /**
   * Tells the panel to use a new set of instances.
   *
   * @param inst a set of Instances
   */
  public void setInstances(Instances inst) {
    
    setXY_VisualizeIndexes(0,0);

    m_Instances = inst;
   
    m_ignoreKeyModel.removeAllElements();
    
    String [] attribNames = new String [m_Instances.numAttributes()];
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      String name = m_Instances.attribute(i).name();
      m_ignoreKeyModel.addElement(name);

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
      default:
	type = "(???) ";
      }
      String attnm = m_Instances.attribute(i).name();
     
      attribNames[i] = type + attnm;
    }

    
    m_StartBut.setEnabled(m_RunThread == null);
    m_StopBut.setEnabled(m_RunThread != null);
    m_ignoreBut.setEnabled(true);
    m_ClassCombo.setModel(new DefaultComboBoxModel(attribNames));
    m_ClassCombo.setSelectedIndex(attribNames.length - 1);
    updateRadioLinks();
  }

  /**
   * Attempts to filter the user specified test set through
   * the most currently used set of filters (if any) from the
   * pre-process panel.
   */
  protected void filterUserTestInstances() {

    if (m_Preprocess != null && m_TestInstances != null) {
      m_TestInstancesCopy = new Instances(m_TestInstances);
      SerializedObject sf = m_Preprocess.getMostRecentFilters();
      if (sf != null) {
	Filter [] filters = null;
	try {
	  filters = (Filter [])sf.getObject();
	} catch (Exception ex) {
	  JOptionPane.showMessageDialog(this,
					"Could not deserialize filters",
					null,
					JOptionPane.ERROR_MESSAGE);
	}
	if (filters.length != 0) {
	  try {
	    m_Log.statusMessage("Applying preprocess filters to test data...");
	    m_TestInstancesCopy = new Instances(m_TestInstances);
	    for (int i = 0; i < filters.length; i++) {
	      m_Log.statusMessage("Passing through filter " + (i + 1) + ": "
				  + filters[i].getClass().getName());
	      filters[i].setInputFormat(m_TestInstancesCopy);
	      m_TestInstancesCopy = Filter.useFilter(m_TestInstancesCopy, 
						     filters[i]);
	    }
	    m_Log.statusMessage("OK");
	  } catch (Exception ex) {
	    m_Log.statusMessage("See error log");
	    m_Log.logMessage("Problem applying filters to test data "
			     +"(Cluster Panel");
	  }
	  if (m_Summary != null && m_TestInstancesCopy != null) {
	    m_Summary.setInstances(m_TestInstancesCopy);
	  }
	}
      }
    }
  }

  /**
   * Sets the user test set. Information about the current test set
   * is displayed in an InstanceSummaryPanel and the user is given the
   * ability to load another set from a file or url.
   *
   */
  protected void setTestSet() {

    if (m_SetTestFrame == null) {
      final SetInstancesPanel sp = new SetInstancesPanel();
      m_Summary = sp.getSummary();
      if (m_TestInstancesCopy != null) {
	sp.setInstances(m_TestInstances);
      }
      sp.addPropertyChangeListener(new PropertyChangeListener() {
	public void propertyChange(PropertyChangeEvent e) {
	  m_TestInstances = sp.getInstances();
	  filterUserTestInstances();
	}
      });
      // Add propertychangelistener to update m_TestInstances whenever
      // it changes in the settestframe
      m_SetTestFrame = new JFrame("Test Instances");
      m_SetTestFrame.getContentPane().setLayout(new BorderLayout());
      m_SetTestFrame.getContentPane().add(sp, BorderLayout.CENTER);
      m_SetTestFrame.pack();
    }
    m_SetTestFrame.setVisible(true);
  }

  /**
   * Sets up the structure for the visualizable instances. This dataset
   * contains the original attributes plus the clusterer's cluster assignments
   * @param testInstancs the instances that the clusterer has clustered
   * @return a PlotData2D object encapsulating the visualizable instances. The    * instances contain one more attribute (predicted
   * cluster) than the testInstances
   */
  private PlotData2D setUpVisualizableInstances(Instances testInstances,
						ClusterEvaluation eval) 
    throws Exception {

    int numClusters = eval.getNumClusters();
    double [] clusterAssignments = eval.getClusterAssignments();

    FastVector hv = new FastVector();
    Instances newInsts;

    Attribute predictedCluster;
    FastVector clustVals = new FastVector();

    for (int i = 0; i < numClusters; i++) {
      clustVals.addElement("cluster"+i);
    }
    predictedCluster = new Attribute("Cluster", clustVals);
    for (int i = 0; i < testInstances.numAttributes(); i++) {
      hv.addElement(testInstances.attribute(i).copy());
    }
    hv.addElement(predictedCluster);
    
    newInsts = new Instances(testInstances.relationName()+"_clustered", hv, 
			     testInstances.numInstances());

    double [] values;
    int j;
    int [] pointShapes = null;
    int [] classAssignments = null;
    if (testInstances.classIndex() >= 0) {
      classAssignments = eval.getClassesToClusters();
      pointShapes = new int[testInstances.numInstances()];
      for (int i = 0; i < testInstances.numInstances(); i++) {
	pointShapes[i] = Plot2D.CONST_AUTOMATIC_SHAPE;
      }
    }

    for (int i = 0; i < testInstances.numInstances(); i++) {
      values = new double[newInsts.numAttributes()];
      for (j = 0; j < testInstances.numAttributes(); j++) {
	values[j] = testInstances.instance(i).value(j);
      }
      values[j] = clusterAssignments[i];
      newInsts.add(new Instance(1.0, values));
      if (pointShapes != null) {
	if ((int)testInstances.instance(i).classValue() != 
	    classAssignments[(int)clusterAssignments[i]]) {
	  pointShapes[i] = Plot2D.ERROR_SHAPE;
	}
      }
    }
    PlotData2D plotData = new PlotData2D(newInsts);
    if (pointShapes != null) {
      plotData.setShapeType(pointShapes);
    }
    plotData.addInstanceNumberAttribute();
    return plotData;
  }
  
  /**
   * Starts running the currently configured clusterer with the current
   * settings. This is run in a separate thread, and will only start if
   * there is no clusterer already running. The clusterer output is sent
   * to the results history panel.
   */
  protected void startClusterer() {

    if (m_RunThread == null) {
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(true);
      m_ignoreBut.setEnabled(false);
      m_RunThread = new Thread() {
	public void run() {
	  // Copy the current state of things
	  m_Log.statusMessage("Setting up...");
	  Instances inst = new Instances(m_Instances);
	  Instances userTest = null;
	  PlotData2D predData = null;
	  if (m_TestInstances != null) {
	    userTest = new Instances(m_TestInstancesCopy);
	  }
	  
	  boolean saveVis = m_StorePredictionsBut.isSelected();

	  int testMode = 0;
	  int percent = 66;
	  Clusterer clusterer = (Clusterer) m_ClustererEditor.getValue();
	  StringBuffer outBuff = new StringBuffer();
	  String name = (new SimpleDateFormat("HH:mm:ss - "))
	  .format(new Date());
	  String cname = clusterer.getClass().getName();
	  if (cname.startsWith("weka.clusterers.")) {
	    name += cname.substring("weka.clusterers.".length());
	  } else {
	    name += cname;
	  }
	  try {
	    m_Log.logMessage("Started " + cname);
	    if (m_Log instanceof TaskLogger) {
	      ((TaskLogger)m_Log).taskStarted();
	    }
	    if (m_PercentBut.isSelected()) {
	      testMode = 2;
	      percent = Integer.parseInt(m_PercentText.getText());
	      if ((percent <= 0) || (percent >= 100)) {
		throw new Exception("Percentage must be between 0 and 100");
	      }
	    } else if (m_TrainBut.isSelected()) {
	      testMode = 3;
	    } else if (m_TestSplitBut.isSelected()) {
	      testMode = 4;
	      // Check the test instance compatibility
	      if (userTest == null) {
		throw new Exception("No user test set has been opened");
	      }
	      if (!inst.equalHeaders(userTest)) {
		throw new Exception("Train and test set are not compatible");
	      }
	    } else if (m_ClassesToClustersBut.isSelected()) {
	      testMode = 5;
	    } else {
	      throw new Exception("Unknown test mode");
	    }

	    Instances trainInst = new Instances(inst);
	    if (m_ClassesToClustersBut.isSelected()) {
	      trainInst.setClassIndex(m_ClassCombo.getSelectedIndex());
	      inst.setClassIndex(m_ClassCombo.getSelectedIndex());
	      if (inst.classAttribute().isNumeric()) {
		throw new Exception("Class must be nominal for class based "
				    +"evaluation!");
	      }
	    }
	    if (!m_ignoreKeyList.isSelectionEmpty()) {
	      trainInst = removeIgnoreCols(trainInst);
	    }

	    // Output some header information
	    outBuff.append("=== Run information ===\n\n");
	    outBuff.append("Scheme:       " + cname);
	    if (clusterer instanceof OptionHandler) {
	      String [] o = ((OptionHandler) clusterer).getOptions();
	      outBuff.append(" " + Utils.joinOptions(o));
	    }
	    outBuff.append("\n");
	    outBuff.append("Relation:     " + inst.relationName() + '\n');
	    outBuff.append("Instances:    " + inst.numInstances() + '\n');
	    outBuff.append("Attributes:   " + inst.numAttributes() + '\n');
	    if (inst.numAttributes() < 100) {
	      boolean [] selected = new boolean [inst.numAttributes()];
	      for (int i = 0; i < inst.numAttributes(); i++) {
		selected[i] = true;
	      }
	      if (!m_ignoreKeyList.isSelectionEmpty()) {
		int [] indices = m_ignoreKeyList.getSelectedIndices();
		for (int i = 0; i < indices.length; i++) {
		  selected[indices[i]] = false;
		}
	      }
	      if (m_ClassesToClustersBut.isSelected()) {
		selected[m_ClassCombo.getSelectedIndex()] = false;
	      }
	      for (int i = 0; i < inst.numAttributes(); i++) {
		if (selected[i]) {
		  outBuff.append("              " + inst.attribute(i).name()
				 + '\n');
		}
	      }
	      if (!m_ignoreKeyList.isSelectionEmpty() 
		  || m_ClassesToClustersBut.isSelected()) {
		outBuff.append("Ingored:\n");
		for (int i = 0; i < inst.numAttributes(); i++) {
		  if (!selected[i]) {
		    outBuff.append("              " + inst.attribute(i).name()
				   + '\n');
		  }
		}
	      }
	    } else {
	      outBuff.append("              [list of attributes omitted]\n");
	    }

	    outBuff.append("Test mode:    ");
	    switch (testMode) {
	      case 3: // Test on training
	      outBuff.append("evaluate on training data\n");
	      break;
	      case 2: // Percent split
	      outBuff.append("split " + percent
			       + "% train, remainder test\n");
	      break;
	      case 4: // Test on user split
	      outBuff.append("user supplied test set: "
			     + userTest.numInstances() + " instances\n");
	      break;
	    case 5: // Classes to clusters evaluation on training
	      outBuff.append("Classes to clusters evaluation on training data");
	      
	      break;
	    }
	    outBuff.append("\n");
	    m_History.addResult(name, outBuff);
	    m_History.setSingle(name);
	    
	    // Build the model and output it.
	    m_Log.statusMessage("Building model on training data...");

	    // remove the class attribute (if set) and build the clusterer
	    clusterer.buildClusterer(removeClass(trainInst));

	    outBuff.append("\n=== Clustering model (full training set) ===\n\n");
	    outBuff.append(clusterer.toString() + '\n');
	    m_History.updateResult(name);
	    
	    ClusterEvaluation eval = new ClusterEvaluation();
	    eval.setClusterer(clusterer);
	    switch (testMode) {
	      case 3: case 5: // Test on training
	      m_Log.statusMessage("Clustering training data...");
	      eval.evaluateClusterer(trainInst);
	      predData = setUpVisualizableInstances(inst,eval);
	      outBuff.append("=== Evaluation on training set ===\n\n");
	      break;

	      case 2: // Percent split
	      m_Log.statusMessage("Randomizing instances...");
	      inst.randomize(new Random(42));
	      trainInst.randomize(new Random(42));
	      int trainSize = trainInst.numInstances() * percent / 100;
	      int testSize = trainInst.numInstances() - trainSize;
	      Instances train = new Instances(trainInst, 0, trainSize);
	      Instances test = new Instances(trainInst, trainSize, testSize);
	      Instances testVis = new Instances(inst, trainSize, testSize);
	      m_Log.statusMessage("Building model on training split...");
	      clusterer.buildClusterer(train);
	      m_Log.statusMessage("Evaluating on test split...");
	      eval.evaluateClusterer(test);
	      predData = setUpVisualizableInstances(testVis, eval);
	      outBuff.append("=== Evaluation on test split ===\n");
	      break;
		
	      case 4: // Test on user split
	      m_Log.statusMessage("Evaluating on test data...");
	      Instances userTestT = new Instances(userTest);
	      if (!m_ignoreKeyList.isSelectionEmpty()) {
		userTestT = removeIgnoreCols(userTestT);
	      }
	      eval.evaluateClusterer(userTestT);
	      predData = setUpVisualizableInstances(userTest, eval);
	      outBuff.append("=== Evaluation on test set ===\n");
	      break;

	      default:
	      throw new Exception("Test mode not implemented");
	    }
	    outBuff.append(eval.clusterResultsToString());
	    outBuff.append("\n");
	    m_History.updateResult(name);
	    m_Log.logMessage("Finished " + cname);
	    m_Log.statusMessage("OK");
	  } catch (Exception ex) {
	    m_Log.logMessage(ex.getMessage());
	    m_Log.statusMessage("See error log");
	    ex.printStackTrace();
	  } finally {
	    if (predData != null) {
	      m_CurrentVis = new VisualizePanel();
	      m_CurrentVis.setName(name+" ("+inst.relationName()+")");
	      m_CurrentVis.setLog(m_Log);
	      predData.setPlotName(name+" ("+inst.relationName()+")");
	      
	      try {
		m_CurrentVis.addPlot(predData);
		m_CurrentVis.setXIndex(m_visXIndex); 
		m_CurrentVis.setYIndex(m_visYIndex);
	      } catch (Exception ex) {
		System.err.println(ex);
	      }
	      m_CurrentVis.addActionListener(new ActionListener() {
		  public void actionPerformed(ActionEvent e) {
		    if (m_CurrentVis.getInstances().
			relationName().
			compareTo(m_Instances.relationName()) == 0) {
		      setXY_VisualizeIndexes(m_CurrentVis.getXIndex(), 
					     m_CurrentVis.getYIndex());
		    }
		  }
		});

	      if (saveVis) {
		FastVector vv = new FastVector();
		vv.addElement(m_CurrentVis);
		m_History.addObject(name, vv);
	      }
	    }
	    if (isInterrupted()) {
	      m_Log.logMessage("Interrupted " + cname);
	      m_Log.statusMessage("See error log");
	    }
	    m_RunThread = null;
	    m_StartBut.setEnabled(true);
	    m_StopBut.setEnabled(false);
	    m_ignoreBut.setEnabled(true);
	    if (m_Log instanceof TaskLogger) {
	      ((TaskLogger)m_Log).taskFinished();
	    }
	  }
	}
      };
      m_RunThread.setPriority(Thread.MIN_PRIORITY);
      m_RunThread.start();
    }
  }

  private Instances removeClass(Instances inst) {
    AttributeFilter af = new AttributeFilter();
    Instances retI = null;
    
    try {
      if (inst.classIndex() < 0) {
	retI = inst;
      } else {
	af.setAttributeIndices(""+(inst.classIndex()+1));
	af.setInvertSelection(false);
	af.setInputFormat(inst);
	retI = Filter.useFilter(inst, af);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return retI;
  }

  private Instances removeIgnoreCols(Instances inst) {
    
    // If the user is doing classes to clusters evaluation and
    // they have opted to ignore the class, then unselect the class in
    // the ignore list
    if (m_ClassesToClustersBut.isSelected()) {
      int classIndex = m_ClassCombo.getSelectedIndex();
      if (m_ignoreKeyList.isSelectedIndex(classIndex)) {
	m_ignoreKeyList.removeSelectionInterval(classIndex, classIndex);
      }
    }
    int [] selected = m_ignoreKeyList.getSelectedIndices();
    AttributeFilter af = new AttributeFilter();
    Instances retI = null;

    try {
      af.setAttributeIndicesArray(selected);
      af.setInvertSelection(false);
      af.setInputFormat(inst);
      retI = Filter.useFilter(inst, af);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return retI;
  }

  /**
   * Stops the currently running clusterer (if any).
   */
  protected void stopClusterer() {

    if (m_RunThread != null) {
      m_RunThread.interrupt();
      
      // This is deprecated (and theoretically the interrupt should do).
      m_RunThread.stop();
      
    }
  }

  /**
   * Pops up a TreeVisualizer for the clusterer from the currently
   * selected item in the results list
   * @param dottyString the description of the tree in dotty format
   * @param treeName the title to assign to the display
   */
  protected void visualizeTree(String dottyString, String treeName) {
    final javax.swing.JFrame jf = 
      new javax.swing.JFrame("Weka Classifier Tree Visualizer: "+treeName);
    jf.setSize(500,400);
    jf.getContentPane().setLayout(new BorderLayout());
    TreeVisualizer tv = new TreeVisualizer(null,
					   dottyString,
					   new PlaceNode2());
    jf.getContentPane().add(tv, BorderLayout.CENTER);
    jf.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  jf.dispose();
	}
      });
    
    jf.setVisible(true);
  }

  /**
   * Pops up a visualize panel to display cluster assignments
   * @param sp the visualize panel to display
   */
  protected void visualizeClusterAssignments(VisualizePanel sp) {
    if (sp != null) {
      String plotName = sp.getName();
      final javax.swing.JFrame jf = 
	new javax.swing.JFrame("Weka Clusterer Visualize: "+plotName);
      jf.setSize(500,400);
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	  public void windowClosing(java.awt.event.WindowEvent e) {
	    jf.dispose();
	  }
	});

      jf.setVisible(true);
    }
  }

  /**
   * Handles constructing a popup menu with visualization options
   * @param name the name of the result history list entry clicked on by
   * the user
   * @param x the x coordinate for popping up the menu
   * @param y the y coordinate for popping up the menu
   */
  protected void visualizeClusterer(String name, int x, int y) {
    final String selectedName = name;
    JPopupMenu resultListMenu = new JPopupMenu();

    JMenuItem visMainBuffer = new JMenuItem("View in main window");
    visMainBuffer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_History.setSingle(selectedName);
	}
      });
    resultListMenu.add(visMainBuffer);

    JMenuItem visSepBuffer = new JMenuItem("View in separate window");
    visSepBuffer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_History.openFrame(selectedName);
	}
      });
    resultListMenu.add(visSepBuffer);

    JMenuItem saveOutput = new JMenuItem("Save result buffer");
    saveOutput.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  saveBuffer(selectedName);
	}
      });
    resultListMenu.add(saveOutput);
    resultListMenu.addSeparator();

    FastVector o = (FastVector)m_History.getNamedObject(selectedName);

    if (o != null) {
      VisualizePanel temp_vp = null;
      String temp_grph = null;
     
      for (int i = 0; i < o.size(); i++) {
	Object temp = o.elementAt(i);
	if (temp instanceof VisualizePanel) { // normal errors
	  temp_vp = (VisualizePanel)temp;
	} else if (temp instanceof String) { // graphable output
	  temp_grph = (String)temp;
	}
      }

      final VisualizePanel vp = temp_vp;
      final String grph = temp_grph;

      JMenuItem visClusts = new JMenuItem("Visualize cluster assignments");
      if (vp != null) {
	visClusts.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      visualizeClusterAssignments(vp);
	    }
	  });
	resultListMenu.add(visClusts);
      }
      JMenuItem visTree = new JMenuItem("Visualize tree");
      if (grph != null) {
	visTree.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      visualizeTree(grph,vp.getName());
	    }
	  });
	resultListMenu.add(visTree);
      }
      
    }
    resultListMenu.show(m_History.getList(), x, y);
  }

  /**
   * Save the currently selected clusterer output to a file.
   * @param name the name of the buffer to save
   */
  protected void saveBuffer(String name) {
    StringBuffer sb = m_History.getNamedBuffer(name);
    if (sb != null) {
      if (m_SaveOut.save(sb)) {
	m_Log.logMessage("Save succesful.");
      }
    }
  }

  private void setIgnoreColumns() {
    ListSelectorDialog jd = new ListSelectorDialog(null, m_ignoreKeyList);

    // Open the dialog
    int result = jd.showDialog();
    
    if (result != ListSelectorDialog.APPROVE_OPTION) {
      // clear selected indices
      m_ignoreKeyList.clearSelection();
    }
  }

  /**
   * Tests out the clusterer panel from the command line.
   *
   * @param args may optionally contain the name of a dataset to load.
   */
  public static void main(String [] args) {

    try {
      final javax.swing.JFrame jf =
	new javax.swing.JFrame("Weka Knowledge Explorer: Cluster");
      jf.getContentPane().setLayout(new BorderLayout());
      final ClustererPanel sp = new ClustererPanel();
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
	java.io.Reader r = new java.io.BufferedReader(
			   new java.io.FileReader(args[0]));
	Instances i = new Instances(r);
	sp.setInstances(i);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
