/*
 *    ClassifierPanel.java
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

package weka.gui.explorer;

import weka.filters.*;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.FastVector;
import weka.core.OptionHandler;
import weka.core.Attribute;
import weka.core.Utils;
import weka.core.Drawable;
import weka.classifiers.Classifier;
import weka.classifiers.DistributionClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.CostMatrix;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.MarginCurve;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.filters.Filter;
import weka.gui.Logger;
import weka.gui.TaskLogger;
import weka.gui.SysErrLog;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;
import weka.gui.ResultHistoryPanel;
import weka.gui.SetInstancesPanel;
import weka.gui.CostMatrixEditor;
import weka.gui.PropertyDialog;
import weka.gui.InstancesSummaryPanel;
import weka.gui.SaveBuffer;
//import weka.gui.VisualizePanel;
import weka.gui.visualize.VisualizePanel;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.Plot2D;

import weka.gui.treevisualizer.*;

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
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Window;
import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.ObjectInputStream;


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
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/** 
 * This panel allows the user to select and configure a classifier, set the
 * attribute of the current dataset to be used as the class, and evaluate
 * the classifier using a number of testing modes (test on the training data,
 * train/test on a percentage split, n-fold cross-validation, test on a
 * separate split). The results of classification runs are stored in a result
 * history so that previous results are accessible.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.32 $
 */
public class ClassifierPanel extends JPanel {

  /** Lets the user configure the classifier */
  protected GenericObjectEditor m_ClassifierEditor =
    new GenericObjectEditor();

  /** The panel showing the current classifier selection */
  protected PropertyPanel m_CEPanel = new PropertyPanel(m_ClassifierEditor);
  
  /** The output area for classification results */
  protected JTextArea m_OutText = new JTextArea(20, 40);

  /** The destination for log/status messages */
  protected Logger m_Log = new SysErrLog();

  /** The buffer saving object for saving output */
  SaveBuffer m_SaveOut = new SaveBuffer(m_Log, this);

  /** A panel controlling results viewing */
  protected ResultHistoryPanel m_History = new ResultHistoryPanel(m_OutText);

  /** Lets the user select the class column */
  protected JComboBox m_ClassCombo = new JComboBox();

  /** Click to set test mode to cross-validation */
  protected JRadioButton m_CVBut = new JRadioButton("Cross-validation");

  /** Click to set test mode to generate a % split */
  protected JRadioButton m_PercentBut = new JRadioButton("Percentage split");

  /** Click to set test mode to test on training data */
  protected JRadioButton m_TrainBut = new JRadioButton("Use training set");

  /** Click to set test mode to a user-specified test set */
  protected JRadioButton m_TestSplitBut =
    new JRadioButton("Supplied test set");

  /** Check to save the predictions in the results list for visualizing
      later on */
  protected JCheckBox m_StorePredictionsBut = 
    new JCheckBox("Store predictions for visualization");

  /** Check to output the model built from the training data */
  protected JCheckBox m_OutputModelBut = new JCheckBox("Output model");

  /** Check to output true/false positives, precision/recall for each class */
  protected JCheckBox m_OutputPerClassBut =
    new JCheckBox("Output per-class stats");

  /** Check to output a confusion matrix */
  protected JCheckBox m_OutputConfusionBut =
    new JCheckBox("Output confusion matrix");

  /** Check to output entropy statistics */
  protected JCheckBox m_OutputEntropyBut =
    new JCheckBox("Output entropy evaluation measures");

  /** Check to evaluate w.r.t a cost matrix */
  protected JCheckBox m_EvalWRTCostsBut =
    new JCheckBox("Cost-sensitive evaluation");

  protected JButton m_SetCostsBut = new JButton("Set...");

  /** Label by where the cv folds are entered */
  protected JLabel m_CVLab = new JLabel("Folds", SwingConstants.RIGHT);

  /** The field where the cv folds are entered */
  protected JTextField m_CVText = new JTextField("10");

  /** Label by where the % split is entered */
  protected JLabel m_PercentLab = new JLabel("%", SwingConstants.RIGHT);

  /** The field where the % split is entered */
  protected JTextField m_PercentText = new JTextField("66");

  /** The button used to open a separate test dataset */
  protected JButton m_SetTestBut = new JButton("Set...");

  /** The frame used to show the test set selection panel */
  protected JFrame m_SetTestFrame;

  /** The frame used to show the cost matrix editing panel */
  protected PropertyDialog m_SetCostsFrame;

  /**
   * Alters the enabled/disabled status of elements associated with each
   * radio button
   */
  ActionListener m_RadioListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      updateRadioLinks();
    }
  };

  /** Button for further output/visualize options */
  JButton m_MoreOptions = new JButton("More options...");

  /** Click to start running the classifier */
  protected JButton m_StartBut = new JButton("Start");

  /** Click to stop a running classifier */
  protected JButton m_StopBut = new JButton("Stop");

  /** Stop the class combo from taking up to much space */
  private Dimension COMBO_SIZE = new Dimension(150, m_StartBut
					       .getPreferredSize().height);

  /** The cost matrix editor for evaluation costs */
  protected CostMatrixEditor m_CostMatrixEditor = new CostMatrixEditor();

  /** The main set of instances we're playing with */
  protected Instances m_Instances;

  /** The user-supplied test set (if any) */
  protected Instances m_TestInstances;

  /** The user supplied test set after preprocess filters have been applied */
  protected Instances m_TestInstancesCopy;
  
  /** A thread that classification runs in */
  protected Thread m_RunThread;

  /** default x index for visualizing */
  protected int m_visXIndex;
  
  /** default y index for visualizing */
  protected int m_visYIndex;

  /** The current visualization object */
  protected VisualizePanel m_CurrentVis = null;

  /** The preprocess panel through which filters can be applied to
      user supplied test data sets */
  protected PreprocessPanel m_Preprocess = null;

  /** The instances summary panel displayed by m_SetTestFrame */
  protected InstancesSummaryPanel m_Summary = null;

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
      .registerEditor(weka.classifiers.DistributionClassifier.class,
		      weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.classifiers.Classifier.class,
		      weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.classifiers.CostMatrix.class,
		      weka.gui.CostMatrixEditor.class);
  }
  
  /**
   * Creates the classifier panel
   */
  public ClassifierPanel() {

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
    m_ClassifierEditor.setClassType(Classifier.class);
    m_ClassifierEditor.setValue(new weka.classifiers.ZeroR());
    m_ClassifierEditor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	repaint();
      }
    });

    m_ClassCombo.setToolTipText("Select the attribute to use as the class");
    m_TrainBut.setToolTipText("Test on the same set that the classifier"
			      + " is trained on");
    m_CVBut.setToolTipText("Perform a n-fold cross-validation");
    m_PercentBut.setToolTipText("Train on a percentage of the data and"
				+ " test on the remainder");
    m_TestSplitBut.setToolTipText("Test on a user-specified dataset");
    m_StartBut.setToolTipText("Starts the classification");
    m_StopBut.setToolTipText("Stops a running classification");
    m_StorePredictionsBut.
      setToolTipText("Store predictions in the result list for later "
		     +"visualization");
    m_OutputModelBut
      .setToolTipText("Output the model obtained from the full training set");
    m_OutputPerClassBut.setToolTipText("Output precision/recall & true/false"
				    + " positives for each class");
    m_OutputConfusionBut
      .setToolTipText("Output the matrix displaying class confusions");
    m_OutputEntropyBut
      .setToolTipText("Output entropy-based evaluation measures");
    m_EvalWRTCostsBut
      .setToolTipText("Evaluate errors with respect to a cost matrix");
    m_StorePredictionsBut.setSelected(true);
    m_OutputModelBut.setSelected(true);
    m_OutputPerClassBut.setSelected(true);
    m_OutputConfusionBut.setSelected(true);
    m_ClassCombo.setEnabled(false);
    m_ClassCombo.setPreferredSize(COMBO_SIZE);
    m_ClassCombo.setMaximumSize(COMBO_SIZE);
    m_ClassCombo.setMinimumSize(COMBO_SIZE);

    m_CVBut.setSelected(true);
    updateRadioLinks();
    ButtonGroup bg = new ButtonGroup();
    bg.add(m_TrainBut);
    bg.add(m_CVBut);
    bg.add(m_PercentBut);
    bg.add(m_TestSplitBut);
    m_TrainBut.addActionListener(m_RadioListener);
    m_CVBut.addActionListener(m_RadioListener);
    m_PercentBut.addActionListener(m_RadioListener);
    m_TestSplitBut.addActionListener(m_RadioListener);
    m_SetTestBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setTestSet();
      }
    });
    m_EvalWRTCostsBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	m_SetCostsBut.setEnabled(m_EvalWRTCostsBut.isSelected());
	if ((m_SetCostsFrame != null) 
	    && (!m_EvalWRTCostsBut.isSelected())) {
	  m_SetCostsFrame.setVisible(false);
	}
      }
    });
    m_CostMatrixEditor.setValue(new CostMatrix(1));
    m_SetCostsBut.setEnabled(m_EvalWRTCostsBut.isSelected());
    m_SetCostsBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	m_SetCostsBut.setEnabled(false);
	if (m_SetCostsFrame == null) {
	  m_SetCostsFrame = new PropertyDialog(m_CostMatrixEditor, 100, 100);
	  //	pd.setSize(250,150);
	  m_SetCostsFrame.addWindowListener(new java.awt.event.WindowAdapter() {
	    public void windowClosing(java.awt.event.WindowEvent p) {
	      m_SetCostsBut.setEnabled(m_EvalWRTCostsBut.isSelected());
	      if ((m_SetCostsFrame != null) 
		  && (!m_EvalWRTCostsBut.isSelected())) {
		m_SetCostsFrame.setVisible(false);
	      }
	    }
	  });
	}
	m_SetCostsFrame.setVisible(true);
      }
    });

    m_StartBut.setEnabled(false);
    m_StopBut.setEnabled(false);
    m_StartBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	startClassifier();
      }
    });
    m_StopBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	stopClassifier();
      }
    });
   
    m_ClassCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	int selected = m_ClassCombo.getSelectedIndex();
	if (selected != -1) {
	  boolean isNominal = m_Instances.attribute(selected).isNominal();
	  m_OutputPerClassBut.setEnabled(isNominal);
	  m_OutputConfusionBut.setEnabled(isNominal);	
	}
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
	      visualize(name, e.getX(), e.getY());
	    }
	  }
	}
      });

    m_MoreOptions.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	m_MoreOptions.setEnabled(false);
	JPanel moreOptionsPanel = new JPanel();
	moreOptionsPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
	moreOptionsPanel.setLayout(new GridLayout(6, 1));
	moreOptionsPanel.add(m_OutputModelBut);
	moreOptionsPanel.add(m_OutputPerClassBut);	  
	moreOptionsPanel.add(m_OutputEntropyBut);	  
	moreOptionsPanel.add(m_OutputConfusionBut);	  
	moreOptionsPanel.add(m_StorePredictionsBut);
	JPanel costMatrixOption = new JPanel();
	costMatrixOption.setLayout(new BorderLayout());
	costMatrixOption.add(m_EvalWRTCostsBut, BorderLayout.WEST);
	costMatrixOption.add(m_SetCostsBut, BorderLayout.EAST);
	moreOptionsPanel.add(costMatrixOption);

	JPanel all = new JPanel();
	all.setLayout(new BorderLayout());	

	JButton oK = new JButton("OK");
	JPanel okP = new JPanel();
	okP.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	okP.setLayout(new GridLayout(1,1,5,5));
	okP.add(oK);

	all.add(moreOptionsPanel, BorderLayout.CENTER);
	all.add(okP, BorderLayout.SOUTH);
	
	final javax.swing.JFrame jf = 
	  new javax.swing.JFrame("Classifier evaluation options");
	jf.getContentPane().setLayout(new BorderLayout());
	jf.getContentPane().add(all, BorderLayout.CENTER);
	jf.addWindowListener(new java.awt.event.WindowAdapter() {
	  public void windowClosing(java.awt.event.WindowEvent w) {
	    jf.dispose();
	    m_MoreOptions.setEnabled(true);
	  }
	});
	oK.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent a) {
	    m_MoreOptions.setEnabled(true);
	    jf.dispose();
	  }
	});
	jf.pack();
	jf.setLocation(m_MoreOptions.getLocationOnScreen());
	jf.setVisible(true);
      }
    });

    // Layout the GUI
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createCompoundBorder(
		 BorderFactory.createTitledBorder("Classifier"),
		 BorderFactory.createEmptyBorder(0, 5, 5, 5)
		 ));
    p1.setLayout(new BorderLayout());
    p1.add(m_CEPanel, BorderLayout.NORTH);

    JPanel p2 = new JPanel();
    GridBagLayout gbL = new GridBagLayout();
    p2.setLayout(gbL);
    p2.setBorder(BorderFactory.createCompoundBorder(
		 BorderFactory.createTitledBorder("Test options"),
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
    gbL.setConstraints(m_CVBut, gbC);
    p2.add(m_CVBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 2;     gbC.gridx = 1;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(m_CVLab, gbC);
    p2.add(m_CVLab);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 2;     gbC.gridx = 2;  gbC.weightx = 100;
    gbC.ipadx = 20;
    gbL.setConstraints(m_CVText, gbC);
    p2.add(m_CVText);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 3;     gbC.gridx = 0;
    gbL.setConstraints(m_PercentBut, gbC);
    p2.add(m_PercentBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 3;     gbC.gridx = 1;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(m_PercentLab, gbC);
    p2.add(m_PercentLab);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 3;     gbC.gridx = 2;  gbC.weightx = 100;
    gbC.ipadx = 20;
    gbL.setConstraints(m_PercentText, gbC);
    p2.add(m_PercentText);


    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 4;     gbC.gridx = 0;  gbC.weightx = 100;
    gbC.gridwidth = 3;
    //gbC.ipadx = 20;
    gbC.insets = new Insets(3, 0, 1, 0);
    gbL.setConstraints(m_MoreOptions, gbC);
    p2.add(m_MoreOptions);



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
    /*JPanel vPl = new JPanel();
    vPl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    vPl.setLayout(new GridLayout(1,1,5,5));
    vPl.add(m_SaveOutBut);
    buttons.add(vPl); */
    
    JPanel p3 = new JPanel();
    p3.setBorder(BorderFactory.createTitledBorder("Classifier output"));
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
    m_CVText.setEnabled(m_CVBut.isSelected());
    m_CVLab.setEnabled(m_CVBut.isSelected());
    m_PercentText.setEnabled(m_PercentBut.isSelected());
    m_PercentLab.setEnabled(m_PercentBut.isSelected());
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
    m_Instances = inst;

    setXY_VisualizeIndexes(0,0); // reset the default x and y indexes
    String [] attribNames = new String [m_Instances.numAttributes()];
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
      default:
	type = "(???) ";
      }
      attribNames[i] = type + m_Instances.attribute(i).name();
    }
    m_ClassCombo.setModel(new DefaultComboBoxModel(attribNames));
    if (attribNames.length > 0) {
      m_ClassCombo.setSelectedIndex(attribNames.length - 1);
      m_ClassCombo.setEnabled(true);
      m_StartBut.setEnabled(m_RunThread == null);
      m_StopBut.setEnabled(m_RunThread != null);
    } else {
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(false);
    }
  }

  /**
   * Attempts to filter the user specified test set through
   * the most currently used set of filters (if any) from the
   * pre-process panel.
   */
  protected void filterUserTestInstances() {

    if (m_Preprocess != null && m_TestInstances != null) {
      m_TestInstancesCopy = new Instances(m_TestInstances);
      byte [] sf = m_Preprocess.getMostRecentFilters();
      if (sf != null) {
	Filter [] filters = null;
	try {
	  ByteArrayInputStream bi = new ByteArrayInputStream(sf);
	  BufferedInputStream bbi = new BufferedInputStream(bi);
	  ObjectInputStream oi = new ObjectInputStream(bbi);
	  filters = (Filter [])oi.readObject();
	  oi.close();
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
	      filters[i].inputFormat(m_TestInstancesCopy);
	      m_TestInstancesCopy = Filter.useFilter(m_TestInstancesCopy, 
						     filters[i]);
	    }
	    m_Log.statusMessage("OK");
	  } catch (Exception ex) {
	    m_Log.statusMessage("See error log");
	    m_Log.logMessage("Problem applying filters to test data "
			     +"(Classifier Panel)");
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
	sp.setInstances(m_TestInstancesCopy);
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
   * Process a classifier's prediction for an instance and update a
   * set of plotting instances and additional plotting info. plotInfo
   * for nominal class datasets holds shape types (actual data points have
   * automatic shape type assignment; classifer error data points have
   * box shape type). For numeric class datasets, the actual data points
   * are stored in plotInstances and plotInfo stores the error (which is
   * later converted to shape size values)
   * @param toPredict the actual data point
   * @param classifier the classifier
   * @param eval the evaluation object to use for evaluating the classifer on
   * the instance to predict
   * @param predictions a fastvector to add the prediction to
   * @param plotInstances a set of plottable instances
   * @param plotInfo additional plotting information (either shape type or
   * shape size).
   */
  private void processClassifierPrediction(Instance toPredict,
					   Classifier classifier,
					   Evaluation eval,
					   FastVector predictions,
					   Instances plotInstances,
					   FastVector plotInfo) {
    try {
      double pred;
      // classifier is a distribution classifer and class is nominal
      if (predictions != null) {
	DistributionClassifier dc = 
	  (DistributionClassifier)classifier;
	double [] dist = 
	  dc.distributionForInstance(toPredict);
	pred = eval.evaluateModelOnce(dist, toPredict);
	int actual = (int)toPredict.classValue();
	predictions.addElement(new 
	  NominalPrediction(actual, dist, toPredict.weight()));
      } else {
	pred = eval.evaluateModelOnce(classifier, 
				      toPredict);
      }

      double [] values = new double[plotInstances.numAttributes()];
      for (int i = 0; i < plotInstances.numAttributes(); i++) {
	if (i < toPredict.classIndex()) {
	  values[i] = toPredict.value(i);
	} else if (i == toPredict.classIndex()) {
	  values[i] = pred;
	  values[i+1] = toPredict.value(i);
	  // if the class value of the instances to predict is missing then
	  // set it to the predicted value
	  if (toPredict.isMissing(i)) {
	    values[i+1] = pred;
	  }
	  i++;
	} else {
	  values[i] = toPredict.value(i-1);
	}
      }

      plotInstances.add(new Instance(1.0, values));
      if (toPredict.classAttribute().isNominal()) {
	if (pred != toPredict.classValue()) {
	  // set to default error point shape
	  plotInfo.addElement(new Integer(Plot2D.ERROR_SHAPE));
	} else {
	  // otherwise set to constant (automatically assigned) point shape
	  plotInfo.addElement(new Integer(Plot2D.CONST_AUTOMATIC_SHAPE));
	}
      } else {
	// store the error (to be converted to a point size later)
	plotInfo.addElement(new Double(pred - toPredict.classValue()));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Post processes numeric class errors into shape sizes for plotting
   * in the visualize panel
   * @param plotInfo a FastVector of numeric class errors
   */
  private void postProcessPlotInfo(FastVector plotInfo) {
    int maxpSize = 20;
    double maxErr = Double.NEGATIVE_INFINITY;
    double minErr = Double.POSITIVE_INFINITY;
    double err;
    
    for (int i = 0; i < plotInfo.size(); i++) {
      err = Math.abs(((Double)plotInfo.elementAt(i)).doubleValue());
        if (err < minErr) {
	minErr = err;
      }
      if (err > maxErr) {
	maxErr = err;
      }
    }
    
    for (int i = 0; i < plotInfo.size(); i++) {
      err = Math.abs(((Double)plotInfo.elementAt(i)).doubleValue());
      if (maxErr - minErr > 0) {
	Double temp = new Double(((err - minErr) / (maxErr - minErr)) 
				 * maxpSize);
	plotInfo.setElementAt(temp, i);
      } else {
	plotInfo.setElementAt(new Double(1.0), i);
      }
    }
  }

  /**
   * Sets up the structure for the visualizable instances. This dataset
   * contains the original attributes plus the classifier's predictions
   * for the class as an attribute called "predicted+WhateverTheClassIsCalled".
   * @param trainInstancs the instances that the classifier is trained on
   * @return a new set of instances containing one more attribute (predicted
   * class) than the trainInstances
   */
  private Instances setUpVisualizableInstances(Instances trainInstances) {
    FastVector hv = new FastVector();
    Attribute predictedClass;

    Attribute classAt = trainInstances.attribute(trainInstances.classIndex());
    if (classAt.isNominal()) {
      FastVector attVals = new FastVector();
      for (int i = 0; i < classAt.numValues(); i++) {
	attVals.addElement(classAt.value(i));
      }
      predictedClass = new Attribute("predicted"+classAt.name(), attVals);
    } else {
      predictedClass = new Attribute("predicted"+classAt.name());
    }

    for (int i = 0; i < trainInstances.numAttributes(); i++) {
      if (i == trainInstances.classIndex()) {
	hv.addElement(predictedClass);
      }
      hv.addElement(trainInstances.attribute(i).copy());
    }
    return new Instances(trainInstances.relationName()+"_predicted", hv, 
			 trainInstances.numInstances());
  }

  /**
   * Starts running the currently configured classifier with the current
   * settings. This is run in a separate thread, and will only start if
   * there is no classifier already running. The classifier output is sent
   * to the results history panel.
   */
  protected void startClassifier() {

    if (m_RunThread == null) {
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(true);
      m_RunThread = new Thread() {
	public void run() {
	  // Copy the current state of things
	  m_Log.statusMessage("Setting up...");
	  CostMatrix costMatrix = null;
	  Instances inst = new Instances(m_Instances);
	  Instances userTest = null;
	  // additional vis info (either shape type or point size)
	  FastVector plotInfo = new FastVector();
	  Instances predInstances = null;

	  // will hold the prediction objects if the class is nominal
	  FastVector predictions = null;
	 
	  if (m_TestInstances != null) {
	    userTest = new Instances(m_TestInstancesCopy);
	  }
	  if (m_EvalWRTCostsBut.isSelected()) {
	    costMatrix = new CostMatrix((CostMatrix) m_CostMatrixEditor
					.getValue());
	  }
	  boolean outputModel = m_OutputModelBut.isSelected();
	  boolean outputConfusion = m_OutputConfusionBut.isSelected();
	  boolean outputPerClass = m_OutputPerClassBut.isSelected();
	  boolean outputSummary = true;
          boolean outputEntropy = m_OutputEntropyBut.isSelected();
	  boolean saveVis = m_StorePredictionsBut.isSelected();

	  String grph = null;

	  int testMode = 0;
	  int numFolds = 10, percent = 66;
	  int classIndex = m_ClassCombo.getSelectedIndex();
	  Classifier classifier = (Classifier) m_ClassifierEditor.getValue();
	  StringBuffer outBuff = new StringBuffer();
	  String name = (new SimpleDateFormat("HH:mm:ss - "))
	  .format(new Date());
	  String cname = classifier.getClass().getName();
	  if (cname.startsWith("weka.classifiers.")) {
	    name += cname.substring("weka.classifiers.".length());
	  } else {
	    name += cname;
	  }
	  try {
	    if (m_CVBut.isSelected()) {
	      testMode = 1;
	      numFolds = Integer.parseInt(m_CVText.getText());
	      if (numFolds <= 1) {
		throw new Exception("Number of folds must be greater than 1");
	      }
	    } else if (m_PercentBut.isSelected()) {
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
	      userTest.setClassIndex(classIndex);
	    } else {
	      throw new Exception("Unknown test mode");
	    }
	    inst.setClassIndex(classIndex);

	    // set up the structure of the plottable instances for 
	    // visualization
	    predInstances = setUpVisualizableInstances(inst);
	    predInstances.setClassIndex(inst.classIndex()+1);
	    
	    if (inst.classAttribute().isNominal() && 
		classifier instanceof DistributionClassifier) {
	      predictions = new FastVector();
	    }

	    // Output some header information
	    m_Log.logMessage("Started " + cname);
	    if (m_Log instanceof TaskLogger) {
	      ((TaskLogger)m_Log).taskStarted();
	    }
	    outBuff.append("=== Run information ===\n\n");
	    outBuff.append("Scheme:       " + cname);
	    if (classifier instanceof OptionHandler) {
	      String [] o = ((OptionHandler) classifier).getOptions();
	      outBuff.append(" " + Utils.joinOptions(o));
	    }
	    outBuff.append("\n");
	    outBuff.append("Relation:     " + inst.relationName() + '\n');
	    outBuff.append("Instances:    " + inst.numInstances() + '\n');
	    outBuff.append("Attributes:   " + inst.numAttributes() + '\n');
	    if (inst.numAttributes() < 100) {
	      for (int i = 0; i < inst.numAttributes(); i++) {
		outBuff.append("              " + inst.attribute(i).name()
			       + '\n');
	      }
	    } else {
	      outBuff.append("              [list of attributes omitted]\n");
	    }

	    outBuff.append("Test mode:    ");
	    switch (testMode) {
	      case 3: // Test on training
	      outBuff.append("evaluate on training data\n");
	      break;
	      case 1: // CV mode
	      outBuff.append("" + numFolds + "-fold cross-validation\n");
	      break;
	      case 2: // Percent split
	      outBuff.append("split " + percent
			       + "% train, remainder test\n");
	      break;
	      case 4: // Test on user split
	      outBuff.append("user supplied test set: "
			     + userTest.numInstances() + " instances\n");
	      break;
	    }
            if (costMatrix != null) {
               outBuff.append("Evaluation cost matrix:\n")
               .append(costMatrix.toString()).append("\n");
            }
	    outBuff.append("\n");
	    m_History.addResult(name, outBuff);
	    m_History.setSingle(name);
	    
	    // Build the model and output it.
	    if (outputModel || (testMode == 3) || (testMode == 4)) {
	      m_Log.statusMessage("Building model on training data...");
	      classifier.buildClassifier(inst);
	    }

	    if (outputModel) {
	      outBuff.append("=== Classifier model (full training set) ===\n\n");
	      outBuff.append(classifier.toString() + "\n\n");
	      m_History.updateResult(name);
	      if (classifier instanceof Drawable) {
		grph = null;
		try {
		  grph = ((Drawable)classifier).graph();
		} catch (Exception ex) {
		}
	      }
	    }
	    
	    Evaluation eval = null;
	    switch (testMode) {
	      case 3: // Test on training
	      m_Log.statusMessage("Evaluating on training data...");
	      eval = new Evaluation(inst, costMatrix);
	      for (int jj=0;jj<inst.numInstances();jj++) {
		processClassifierPrediction(inst.instance(jj), classifier,
					    eval, predictions,
					    predInstances, plotInfo);
		
		if ((jj % 100) == 0) {
		  m_Log.statusMessage("Evaluating on training data. Processed "
				      +jj+" instances...");
		}
	      }
	      outBuff.append("=== Evaluation on training set ===\n");
	      break;

	      case 1: // CV mode
	      m_Log.statusMessage("Randomizing instances...");
	      inst.randomize(new Random(42));
	      if (inst.attribute(classIndex).isNominal()) {
		m_Log.statusMessage("Stratifying instances...");
		inst.stratify(numFolds);
	      }
	      eval = new Evaluation(inst, costMatrix);

	      // Make some splits and do a CV
	      for (int fold = 0; fold < numFolds; fold++) {
		m_Log.statusMessage("Creating splits for fold "
				    + (fold + 1) + "...");
		Instances train = inst.trainCV(numFolds, fold);
		Instances test = inst.testCV(numFolds, fold);
		m_Log.statusMessage("Building model for fold "
				    + (fold + 1) + "...");
		classifier.buildClassifier(train);
		m_Log.statusMessage("Evaluating model for fold "
				    + (fold + 1) + "...");
		for (int jj=0;jj<test.numInstances();jj++) {
		  processClassifierPrediction(test.instance(jj), classifier,
					      eval, predictions,
					      predInstances, plotInfo);
		}
	      }
	      if (inst.attribute(classIndex).isNominal()) {
		outBuff.append("=== Stratified cross-validation ===\n");
	      } else {
		outBuff.append("=== Cross-validation ===\n");
	      }
	      break;
		
	      case 2: // Percent split
	      m_Log.statusMessage("Randomizing instances...");
	      inst.randomize(new Random(42));
	      int trainSize = inst.numInstances() * percent / 100;
	      int testSize = inst.numInstances() - trainSize;
	      Instances train = new Instances(inst, 0, trainSize);
	      Instances test = new Instances(inst, trainSize, testSize);
	      m_Log.statusMessage("Building model on training split...");
	      classifier.buildClassifier(train);
	      eval = new Evaluation(train, costMatrix);
	      m_Log.statusMessage("Evaluating on test split...");
	     
	      for (int jj=0;jj<test.numInstances();jj++) {
		processClassifierPrediction(test.instance(jj), classifier,
					    eval, predictions,
					    predInstances, plotInfo);

		if ((jj % 100) == 0) {
		  m_Log.statusMessage("Evaluating on test split. Processed "
				      +jj+" instances...");
		}
	      }
	      outBuff.append("=== Evaluation on test split ===\n");
	      break;
		
	      case 4: // Test on user split
	      m_Log.statusMessage("Evaluating on test data...");
	      eval = new Evaluation(inst, costMatrix);
	      
	      for (int jj=0;jj<userTest.numInstances();jj++) {
		processClassifierPrediction(userTest.instance(jj), classifier,
					    eval, predictions,
					    predInstances, plotInfo);
		if ((jj % 100) == 0) {
		  m_Log.statusMessage("Evaluating on test data. Processed "
				      +jj+" instances...");
		}
	      }
	      outBuff.append("=== Evaluation on test set ===\n");
	      break;

	      default:
	      throw new Exception("Test mode not implemented");
	    }
	    
	    if (outputSummary) {
	      outBuff.append(eval.toSummaryString(outputEntropy) + "\n");
	    }

	    if (inst.attribute(classIndex).isNominal()) {

	      if (outputPerClass) {
		outBuff.append(eval.toClassDetailsString() + "\n");
	      }

	      if (outputConfusion) {
		outBuff.append(eval.toMatrixString() + "\n");
	      }
	    }

	    m_History.updateResult(name);
	    m_Log.logMessage("Finished " + cname);
	    m_Log.statusMessage("OK");
	  } catch (Exception ex) {
	    ex.printStackTrace();
	    m_Log.logMessage(ex.getMessage());
	    m_Log.statusMessage("See error log");
	  } finally {
	    try {
	      if (predInstances != null) {
		if (predInstances.attribute(predInstances.classIndex())
		    .isNumeric()) {
		  postProcessPlotInfo(plotInfo);
		}
		m_CurrentVis = new VisualizePanel();
		m_CurrentVis.setName(name+" ("+inst.relationName()+")");
		PlotData2D tempd = new PlotData2D(predInstances);
		if (predInstances.classAttribute().isNumeric()) {
		  tempd.setShapeSize(plotInfo);
		} else {
		  tempd.setShapeType(plotInfo);
		}
		tempd.setPlotName(name+" ("+inst.relationName()+")");
		
		m_CurrentVis.addPlot(tempd);
		m_CurrentVis.setColourIndex(predInstances.classIndex());
		
		m_CurrentVis.setXIndex(m_visXIndex); 
		m_CurrentVis.setYIndex(m_visYIndex);
		
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
		  if (grph != null) {
		    vv.addElement(grph);
		  }
		  if (predictions != null) {
		    vv.addElement(predictions);
		    vv.addElement(inst.classAttribute());
		  }
		  m_History.addObject(name, vv);
		}
	      }
	    } catch (Exception ex) {
	      ex.printStackTrace();
	    }
	    
	    if (isInterrupted()) {
	      m_Log.logMessage("Interrupted " + cname);
	      m_Log.statusMessage("See error log");
	    }

	    m_RunThread = null;
	    m_StartBut.setEnabled(true);
	    m_StopBut.setEnabled(false);
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

  /**
   * Handles constructing a popup menu with visualization options.
   * @param x the x coordinate for popping up the menu
   * @param y the y coordinate for popping up the menu
   */
  protected void visualize(String name, int x, int y) {
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
	  saveBuffer();
	}
      });
    resultListMenu.add(saveOutput);
    resultListMenu.addSeparator();

    FastVector o = (FastVector)m_History.getNamedObject(selectedName);

    if (o != null) {
      VisualizePanel temp_vp = null;
      String temp_grph = null;
      FastVector temp_preds = null;
      Attribute temp_classAtt = null;
     
      for (int i = 0; i < o.size(); i++) {
	Object temp = o.elementAt(i);
	if (temp instanceof VisualizePanel) { // normal errors
	  temp_vp = (VisualizePanel)temp;
	} else if (temp instanceof String) { // graphable output
	  temp_grph = (String)temp;
	} else if (temp instanceof FastVector) { // predictions
	  temp_preds = (FastVector)temp;
	} else if (temp instanceof Attribute) { // class attribute
	  temp_classAtt = (Attribute)temp;
	}
      }

      final VisualizePanel vp = temp_vp;
      final String grph = temp_grph;
      final FastVector preds = temp_preds;
      final Attribute classAtt = temp_classAtt;

      JMenuItem visErrors = new JMenuItem("Visualize classifer errors");
      if (vp != null) {
	visErrors.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      visualizeClassifierErrors(vp);
	    }
	  });
	resultListMenu.add(visErrors);
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
      JMenuItem visMargin = new JMenuItem("Visualize margin curve");
      if (preds != null) {
	visMargin.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      try {
		MarginCurve tc = new MarginCurve();
		Instances result = tc.getCurve(preds);
		VisualizePanel vmc = new VisualizePanel();
		vmc.setName(result.relationName());
		PlotData2D tempd = new PlotData2D(result);
		tempd.setPlotName(result.relationName());
		vmc.addPlot(tempd);
		visualizeClassifierErrors(vmc);
	      } catch (Exception ex) {
		ex.printStackTrace();
	      }
	    }
	  });
	resultListMenu.add(visMargin);
      }
      JMenu visThreshold = new JMenu("Visualize threshold curve");
      if (preds != null && classAtt != null) {
	for (int i = 0; i < classAtt.numValues(); i++) {
	  JMenuItem clv = new JMenuItem(classAtt.value(i));
	  final int classValue = i;
	  clv.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent e) {
		try {
		  ThresholdCurve tc = new ThresholdCurve();
		  Instances result = tc.getCurve(preds, classValue);
		  VisualizePanel vmc = new VisualizePanel();
		  vmc.setName(result.relationName()+". Class value "+
			      classAtt.value(classValue)+")");
		  PlotData2D tempd = new PlotData2D(result);
		  tempd.setPlotName(result.relationName());
		  vmc.addPlot(tempd);
		  visualizeClassifierErrors(vmc);
		} catch (Exception ex) {
		  ex.printStackTrace();
		}
	      }
	    });
	  visThreshold.add(clv);
	}
	resultListMenu.add(visThreshold);
      }
    }
    resultListMenu.show(m_History.getList(), x, y);
  }

  /**
   * Pops up a TreeVisualizer for the classifier from the currently
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
   * Pops up a VisualizePanel for visualizing the data and errors for 
   * the classifier from the currently selected item in the results list
   * @param sp the VisualizePanel to pop up.
   */
  protected void visualizeClassifierErrors(VisualizePanel sp) {
   
    if (sp != null) {
      String plotName = sp.getName(); 
	final javax.swing.JFrame jf = 
	new javax.swing.JFrame("Weka Classifier Visualize: "+plotName);
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
   * Save the currently selected classifier output to a file.
   */
  protected void saveBuffer() {
    StringBuffer sb = m_History.getSelectedBuffer();
    if (sb != null) {
      if (m_SaveOut.save(sb)) {
	m_Log.logMessage("Save succesful.");
      }
    }
  }
  

  /**
   * Stops the currently running classifier (if any).
   */
  protected void stopClassifier() {

    if (m_RunThread != null) {
      m_RunThread.interrupt();
      
      // This is deprecated (and theoretically the interrupt should do).
      m_RunThread.stop();
      
    }
  }
  
  /**
   * Tests out the classifier panel from the command line.
   *
   * @param args may optionally contain the name of a dataset to load.
   */
  public static void main(String [] args) {

    try {
      final javax.swing.JFrame jf =
	new javax.swing.JFrame("Weka Knowledge Explorer: Classifier");
      jf.getContentPane().setLayout(new BorderLayout());
      final ClassifierPanel sp = new ClassifierPanel();
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
