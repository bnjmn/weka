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
 *    ClassifierPanel.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.explorer;

import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.Sourcable;
import weka.classifiers.evaluation.CostCurve;
import weka.classifiers.evaluation.MarginCurve;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.pmml.consumer.PMMLClassifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.Drawable;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.SerializedObject;
import weka.core.Utils;
import weka.core.Version;
import weka.core.converters.IncrementalConverter;
import weka.core.converters.Loader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.pmml.PMMLFactory;
import weka.core.pmml.PMMLModel;
import weka.gui.CostMatrixEditor;
import weka.gui.ExtensionFileFilter;
import weka.gui.GenericObjectEditor;
import weka.gui.Logger;
import weka.gui.PropertyDialog;
import weka.gui.PropertyPanel;
import weka.gui.ResultHistoryPanel;
import weka.gui.SaveBuffer;
import weka.gui.SetInstancesPanel;
import weka.gui.SysErrLog;
import weka.gui.TaskLogger;
import weka.gui.beans.CostBenefitAnalysis;
import weka.gui.explorer.Explorer.CapabilitiesFilterChangeEvent;
import weka.gui.explorer.Explorer.CapabilitiesFilterChangeListener;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.gui.explorer.Explorer.LogHandler;
import weka.gui.graphvisualizer.BIFFormatException;
import weka.gui.graphvisualizer.GraphVisualizer;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeVisualizer;
import weka.gui.visualize.Plot2D;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;
import weka.gui.visualize.VisualizePanel;
import weka.gui.visualize.plugins.VisualizePlugin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

/** 
 * This panel allows the user to select and configure a classifier, set the
 * attribute of the current dataset to be used as the class, and evaluate
 * the classifier using a number of testing modes (test on the training data,
 * train/test on a percentage split, n-fold cross-validation, test on a
 * separate split). The results of classification runs are stored in a result
 * history so that previous results are accessible.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class ClassifierPanel 
  extends JPanel
  implements CapabilitiesFilterChangeListener, ExplorerPanel, LogHandler {
   
  /** for serialization */
  static final long serialVersionUID = 6959973704963624003L;

  /** the parent frame */
  protected Explorer m_Explorer = null;

  /** The filename extension that should be used for model files */
  public static String MODEL_FILE_EXTENSION = ".model";
  
  /** The filename extension that should be used for PMML xml files */
  public static String PMML_FILE_EXTENSION = ".xml";

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
  protected JRadioButton m_CVBut = new JRadioButton(Messages.getInstance().getString("ClassifierPanel_CVBut_JRadioButton_Text"));

  /** Click to set test mode to generate a % split */
  protected JRadioButton m_PercentBut = new JRadioButton(Messages.getInstance().getString("ClassifierPanel_PercentBut_JRadioButton_Text"));

  /** Click to set test mode to test on training data */
  protected JRadioButton m_TrainBut = new JRadioButton(Messages.getInstance().getString("ClassifierPanel_TrainBut_JRadioButton_Text"));

  /** Click to set test mode to a user-specified test set */
  protected JRadioButton m_TestSplitBut =
    new JRadioButton(Messages.getInstance().getString("ClassifierPanel_TestSplitBut_JRadioButton_Text"));

  /** Check to save the predictions in the results list for visualizing
      later on */
  protected JCheckBox m_StorePredictionsBut = 
    new JCheckBox(Messages.getInstance().getString("ClassifierPanel_StorePredictionsBut_JCheckBox_Text"));

  /** Check to output the model built from the training data */
  protected JCheckBox m_OutputModelBut = new JCheckBox(Messages.getInstance().getString("ClassifierPanel_OutputModelBut_JCheckBox_Text"));

  /** Check to output true/false positives, precision/recall for each class */
  protected JCheckBox m_OutputPerClassBut =
    new JCheckBox(Messages.getInstance().getString("ClassifierPanel_OutputPerClassBut_JCheckBox_Text"));

  /** Check to output a confusion matrix */
  protected JCheckBox m_OutputConfusionBut =
    new JCheckBox(Messages.getInstance().getString("ClassifierPanel_OutputConfusionBut_JCheckBox_Text"));

  /** Check to output entropy statistics */
  protected JCheckBox m_OutputEntropyBut =
    new JCheckBox(Messages.getInstance().getString("ClassifierPanel_OutputEntropyBut_JCheckBox_Text"));

  /** Check to output text predictions */
  protected JCheckBox m_OutputPredictionsTextBut =
    new JCheckBox(Messages.getInstance().getString("ClassifierPanel_OutputPredictionsTextBut_JCheckBox_Text"));
  
  /** Lists indices for additional attributes to output */
  protected JTextField m_OutputAdditionalAttributesText =
    new JTextField("", 10);

  /** Label for the text field with additional attributes in the output */
  protected JLabel m_OutputAdditionalAttributesLab = 
    new JLabel(Messages.getInstance().getString("ClassifierPanel_OutputAdditionalAttributesLab_JLabel_Text"));
  
  /** the range of attributes to output */
  protected Range m_OutputAdditionalAttributesRange = null;
  
  /** Check to evaluate w.r.t a cost matrix */
  protected JCheckBox m_EvalWRTCostsBut =
    new JCheckBox(Messages.getInstance().getString("ClassifierPanel_EvalWRTCostsBut_JCheckBox_Text"));

  /** for the cost matrix */
  protected JButton m_SetCostsBut = new JButton(Messages.getInstance().getString("ClassifierPanel_SetCostsBut_JButton_Text"));

  /** Label by where the cv folds are entered */
  protected JLabel m_CVLab = new JLabel(Messages.getInstance().getString("ClassifierPanel_CVLab_JLabel_Text"), SwingConstants.RIGHT);

  /** The field where the cv folds are entered */
  protected JTextField m_CVText = new JTextField(Messages.getInstance().getString("ClassifierPanel_CVText_JTextField_Text"), 3);

  /** Label by where the % split is entered */
  protected JLabel m_PercentLab = new JLabel(Messages.getInstance().getString("ClassifierPanel_PercentLab_JLabel_Text"), SwingConstants.RIGHT);

  /** The field where the % split is entered */
  protected JTextField m_PercentText = new JTextField(Messages.getInstance().getString("ClassifierPanel_PercentText_JTextField_Text"), 3);

  /** The button used to open a separate test dataset */
  protected JButton m_SetTestBut = new JButton(Messages.getInstance().getString("ClassifierPanel_SetTestBut_JButton_Text"));

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
  JButton m_MoreOptions = new JButton(Messages.getInstance().getString("ClassifierPanel_MoreOptions_JButton_Text"));

  /** User specified random seed for cross validation or % split */
  protected JTextField m_RandomSeedText = new JTextField(Messages.getInstance().getString("ClassifierPanel_RandomSeedText_JTextField_Text"), 3);
  
  /** the label for the random seed textfield */
  protected JLabel m_RandomLab = new JLabel(Messages.getInstance().getString("ClassifierPanel_RandomLab_JLabel_Text"), 
					    SwingConstants.RIGHT);

  /** Whether randomization is turned off to preserve order */
  protected JCheckBox m_PreserveOrderBut = new JCheckBox(Messages.getInstance().getString("ClassifierPanel_PreserveOrderBut_JCheckBox_Text"));

  /** Whether to output the source code (only for classifiers importing Sourcable) */
  protected JCheckBox m_OutputSourceCode = new JCheckBox(Messages.getInstance().getString("ClassifierPanel_OutputSourceCode_JCheckBox_Text"));

  /** The name of the generated class (only applicable to Sourcable schemes) */
  protected JTextField m_SourceCodeClass = new JTextField(Messages.getInstance().getString("ClassifierPanel_SourceCodeClass_JTextField_Text"), 10);
  
  /** Click to start running the classifier */
  protected JButton m_StartBut = new JButton(Messages.getInstance().getString("ClassifierPanel_StartBut_JButton_Text"));

  /** Click to stop a running classifier */
  protected JButton m_StopBut = new JButton(Messages.getInstance().getString("ClassifierPanel_StopBut_JButton_Text"));

  /** Stop the class combo from taking up to much space */
  private Dimension COMBO_SIZE = new Dimension(150, m_StartBut
					       .getPreferredSize().height);

  /** The cost matrix editor for evaluation costs */
  protected CostMatrixEditor m_CostMatrixEditor = new CostMatrixEditor();

  /** The main set of instances we're playing with */
  protected Instances m_Instances;

  /** The loader used to load the user-supplied test set (if any) */
  protected Loader m_TestLoader;
  
  /** A thread that classification runs in */
  protected Thread m_RunThread;

  /** The current visualization object */
  protected VisualizePanel m_CurrentVis = null;

  /** Filter to ensure only model files are selected */  
  protected FileFilter m_ModelFilter =
    new ExtensionFileFilter(MODEL_FILE_EXTENSION, Messages.getInstance().getString("ClassifierPanel_ModelFilter_FileFilter_Text"));
  
  protected FileFilter m_PMMLModelFilter =
    new ExtensionFileFilter(PMML_FILE_EXTENSION, Messages.getInstance().getString("ClassifierPanel_PMMLModelFilter_FileFilter_Text"));

  /** The file chooser for selecting model files */
  protected JFileChooser m_FileChooser 
    = new JFileChooser(new File(System.getProperty("user.dir")));

  /* Register the property editors we need */
  static {
     GenericObjectEditor.registerEditors();
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
    m_History.setBorder(BorderFactory.createTitledBorder(Messages.getInstance().getString("ClassifierPanel_History_BorderFactoryCreateTitledBorder_Text")));
    m_ClassifierEditor.setClassType(Classifier.class);
    m_ClassifierEditor.setValue(ExplorerDefaults.getClassifier());
    m_ClassifierEditor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        m_StartBut.setEnabled(true);
        // Check capabilities
        Capabilities currentFilter = m_ClassifierEditor.getCapabilitiesFilter();
        Classifier classifier = (Classifier) m_ClassifierEditor.getValue();
        Capabilities currentSchemeCapabilities =  null;
        if (classifier != null && currentFilter != null && 
            (classifier instanceof CapabilitiesHandler)) {
          currentSchemeCapabilities = ((CapabilitiesHandler)classifier).getCapabilities();
          
          if (!currentSchemeCapabilities.supportsMaybe(currentFilter) &&
              !currentSchemeCapabilities.supports(currentFilter)) {
            m_StartBut.setEnabled(false);
          }
        }
	repaint();
      }
    });

    m_ClassCombo.setToolTipText(Messages.getInstance().getString("ClassifierPanel_ClassCombo_SetToolTipText_Text"));
    m_TrainBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_TrainBut_SetToolTipText_Text"));
    m_CVBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_CVBut_SetToolTipText_Text"));
    m_PercentBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_PercentBut_SetToolTipText_Text"));
    m_TestSplitBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_TestSplitBut_SetToolTipText_Text"));
    m_StartBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_StartBut_SetToolTipText_Text"));
    m_StopBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_StopBut_SetToolTipText_Text"));
    m_StorePredictionsBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_StorePredictionsBut_SetToolTipText_Text"));
    m_OutputModelBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_OutputModelBut_SetToolTipText_Text"));
    m_OutputPerClassBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_OutputPerClassBut_SetToolTipText_Text"));
    m_OutputConfusionBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_OutputConfusionBut_SetToolTipText_Text"));
    m_OutputEntropyBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_OutputEntropyBut_SetToolTipText_Text"));
    m_EvalWRTCostsBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_EvalWRTCostsBut_SetToolTipText_Text"));
    m_OutputPredictionsTextBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_OutputPredictionsTextBut_SetToolTipText_Text"));
    m_OutputAdditionalAttributesText.setToolTipText(Messages.getInstance().getString("ClassifierPanel_OutputAdditionalAttributesText_SetToolTipText_Text"));
    m_RandomLab.setToolTipText(Messages.getInstance().getString("ClassifierPanel_RandomLab_SetToolTipText_Text"));
    m_RandomSeedText.setToolTipText(m_RandomLab.getToolTipText());
    m_PreserveOrderBut.setToolTipText(Messages.getInstance().getString("ClassifierPanel_PreserveOrderBut_SetToolTipText_Text"));
    m_OutputSourceCode.setToolTipText(Messages.getInstance().getString("ClassifierPanel_OutputSourceCode_SetToolTipText_Text"));
    m_SourceCodeClass.setToolTipText(Messages.getInstance().getString("ClassifierPanel_SourceCodeClass_SetToolTipText_Text"));

    m_FileChooser.addChoosableFileFilter(m_PMMLModelFilter);
    m_FileChooser.setFileFilter(m_ModelFilter);
    
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    m_StorePredictionsBut.setSelected(ExplorerDefaults.getClassifierStorePredictionsForVis());
    m_OutputModelBut.setSelected(ExplorerDefaults.getClassifierOutputModel());
    m_OutputPerClassBut.setSelected(ExplorerDefaults.getClassifierOutputPerClassStats());
    m_OutputConfusionBut.setSelected(ExplorerDefaults.getClassifierOutputConfusionMatrix());
    m_EvalWRTCostsBut.setSelected(ExplorerDefaults.getClassifierCostSensitiveEval());
    m_OutputEntropyBut.setSelected(ExplorerDefaults.getClassifierOutputEntropyEvalMeasures());
    m_OutputPredictionsTextBut.setSelected(ExplorerDefaults.getClassifierOutputPredictions());
    m_OutputPredictionsTextBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	m_OutputAdditionalAttributesText.setEnabled(m_OutputPredictionsTextBut.isSelected());
      }
    });
    m_OutputAdditionalAttributesText.setText(ExplorerDefaults.getClassifierOutputAdditionalAttributes());
    m_OutputAdditionalAttributesText.setEnabled(m_OutputPredictionsTextBut.isSelected());
    m_RandomSeedText.setText("" + ExplorerDefaults.getClassifierRandomSeed());
    m_PreserveOrderBut.setSelected(ExplorerDefaults.getClassifierPreserveOrder());
    m_OutputSourceCode.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_SourceCodeClass.setEnabled(m_OutputSourceCode.isSelected());
      }
    });
    m_OutputSourceCode.setSelected(ExplorerDefaults.getClassifierOutputSourceCode());
    m_SourceCodeClass.setText(ExplorerDefaults.getClassifierSourceCodeClass());
    m_SourceCodeClass.setEnabled(m_OutputSourceCode.isSelected());
    m_ClassCombo.setEnabled(false);
    m_ClassCombo.setPreferredSize(COMBO_SIZE);
    m_ClassCombo.setMaximumSize(COMBO_SIZE);
    m_ClassCombo.setMinimumSize(COMBO_SIZE);

    m_CVBut.setSelected(true);
    // see "testMode" variable in startClassifier
    m_CVBut.setSelected(ExplorerDefaults.getClassifierTestMode() == 1);
    m_PercentBut.setSelected(ExplorerDefaults.getClassifierTestMode() == 2);
    m_TrainBut.setSelected(ExplorerDefaults.getClassifierTestMode() == 3);
    m_TestSplitBut.setSelected(ExplorerDefaults.getClassifierTestMode() == 4);
    m_PercentText.setText("" + ExplorerDefaults.getClassifierPercentageSplit());
    m_CVText.setText("" + ExplorerDefaults.getClassifierCrossvalidationFolds());
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
	  if (PropertyDialog.getParentDialog(ClassifierPanel.this) != null)
	    m_SetCostsFrame = new PropertyDialog(
		PropertyDialog.getParentDialog(ClassifierPanel.this), 
		m_CostMatrixEditor, 100, 100);
	  else
	    m_SetCostsFrame = new PropertyDialog(
		PropertyDialog.getParentFrame(ClassifierPanel.this), 
		m_CostMatrixEditor, 100, 100);
	  m_SetCostsFrame.setTitle(Messages.getInstance().getString("ClassifierPanel_SetCostsFrame_SetTitle_Text"));
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
	  m_SetCostsFrame.setVisible(true);
	}
	
	// do we need to change the size of the matrix?
	int classIndex = m_ClassCombo.getSelectedIndex();
	int numClasses = m_Instances.attribute(classIndex).numValues();
	if (numClasses != ((CostMatrix) m_CostMatrixEditor.getValue()).numColumns())
	  m_CostMatrixEditor.setValue(new CostMatrix(numClasses));
	
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
	updateCapabilitiesFilter(m_ClassifierEditor.getCapabilitiesFilter());
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
	      visualize(name, e.getX(), e.getY());
	    } else {
	      visualize(null, e.getX(), e.getY());
	    }
	  }
	}
      });

    m_MoreOptions.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	m_MoreOptions.setEnabled(false);
	JPanel moreOptionsPanel = new JPanel();
	moreOptionsPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
	moreOptionsPanel.setLayout(new GridLayout(11, 1));
	moreOptionsPanel.add(m_OutputModelBut);
	moreOptionsPanel.add(m_OutputPerClassBut);	  
	moreOptionsPanel.add(m_OutputEntropyBut);	  
	moreOptionsPanel.add(m_OutputConfusionBut);	  
	moreOptionsPanel.add(m_StorePredictionsBut);
	moreOptionsPanel.add(m_OutputPredictionsTextBut);
	JPanel additionalAttsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	additionalAttsPanel.add(m_OutputAdditionalAttributesLab);
	additionalAttsPanel.add(m_OutputAdditionalAttributesText);
	moreOptionsPanel.add(additionalAttsPanel);
	JPanel costMatrixOption = new JPanel(new FlowLayout(FlowLayout.LEFT));
	costMatrixOption.add(m_EvalWRTCostsBut);
	costMatrixOption.add(m_SetCostsBut);
	moreOptionsPanel.add(costMatrixOption);
	JPanel seedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	seedPanel.add(m_RandomLab);
	seedPanel.add(m_RandomSeedText);
	moreOptionsPanel.add(seedPanel);
	moreOptionsPanel.add(m_PreserveOrderBut);
        JPanel sourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        m_OutputSourceCode.setEnabled(m_ClassifierEditor.getValue() instanceof Sourcable);
        m_SourceCodeClass.setEnabled(m_OutputSourceCode.isEnabled() && m_OutputSourceCode.isSelected());
        sourcePanel.add(m_OutputSourceCode);
        sourcePanel.add(m_SourceCodeClass);
        moreOptionsPanel.add(sourcePanel);

	JPanel all = new JPanel();
	all.setLayout(new BorderLayout());	

	JButton oK = new JButton(Messages.getInstance().getString("ClassifierPanel_OK_JButton_Text"));
	JPanel okP = new JPanel();
	okP.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	okP.setLayout(new GridLayout(1,1,5,5));
	okP.add(oK);

	all.add(moreOptionsPanel, BorderLayout.CENTER);
	all.add(okP, BorderLayout.SOUTH);
	
	final JDialog jd = 
	  new JDialog(PropertyDialog.getParentFrame(ClassifierPanel.this), Messages.getInstance().getString("ClassifierPanel_JD_JDialog_Text"));
	jd.getContentPane().setLayout(new BorderLayout());
	jd.getContentPane().add(all, BorderLayout.CENTER);
	jd.addWindowListener(new java.awt.event.WindowAdapter() {
	  public void windowClosing(java.awt.event.WindowEvent w) {
	    jd.dispose();
	    m_MoreOptions.setEnabled(true);
	  }
	});
	oK.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent a) {
	    m_MoreOptions.setEnabled(true);
	    jd.dispose();
	  }
	});
	jd.pack();
	jd.setLocation(m_MoreOptions.getLocationOnScreen());
	jd.setVisible(true);
      }
    });

    // Layout the GUI
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createCompoundBorder(
		 BorderFactory.createTitledBorder(Messages.getInstance().getString("ClassifierPanel_P1_JPanel_BorderFactoryCreateTitledBorder_Text")),
		 BorderFactory.createEmptyBorder(0, 5, 5, 5)
		 ));
    p1.setLayout(new BorderLayout());
    p1.add(m_CEPanel, BorderLayout.NORTH);

    JPanel p2 = new JPanel();
    GridBagLayout gbL = new GridBagLayout();
    p2.setLayout(gbL);
    p2.setBorder(BorderFactory.createCompoundBorder(
		 BorderFactory.createTitledBorder(Messages.getInstance().getString("ClassifierPanel_P2_JPanel_BorderFactoryCreateTitledBorder_Text")),
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
    
    JPanel p3 = new JPanel();
    p3.setBorder(BorderFactory.createTitledBorder(Messages.getInstance().getString("ClassifierPanel_P3_JPanel_BorderFactoryCreateTitledBorder_Text")));
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
   * Sets the Logger to receive informational messages
   *
   * @param newLog the Logger that will now get info messages
   */
  public void setLog(Logger newLog) {

    m_Log = newLog;
  }

  /**
   * Tells the panel to use a new set of instances.
   *
   * @param inst a set of Instances
   */
  public void setInstances(Instances inst) {
    m_Instances = inst;

    String [] attribNames = new String [m_Instances.numAttributes()];
    for (int i = 0; i < attribNames.length; i++) {
      String type = "";
      switch (m_Instances.attribute(i).type()) {
      case Attribute.NOMINAL:
	type = Messages.getInstance().getString("ClassifierPanel_SetInstances_Type_AttributeNOMINAL_Text");
	break;
      case Attribute.NUMERIC:
	type = Messages.getInstance().getString("ClassifierPanel_SetInstances_Type_AttributeNUMERIC_Text");
	break;
      case Attribute.STRING:
	type = Messages.getInstance().getString("ClassifierPanel_SetInstances_Type_AttributeSTRING_Text");
	break;
      case Attribute.DATE:
	type = Messages.getInstance().getString("ClassifierPanel_SetInstances_Type_AttributeDATE_Text");
	break;
      case Attribute.RELATIONAL:
	type = Messages.getInstance().getString("ClassifierPanel_SetInstances_Type_AttributeRELATIONAL_Text");
	break;
      default:
	type = Messages.getInstance().getString("ClassifierPanel_SetInstances_Type_AttributeDEFAULT_Text");
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
      m_StartBut.setEnabled(m_RunThread == null);
      m_StopBut.setEnabled(m_RunThread != null);
    } else {
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(false);
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

      if (m_TestLoader != null) {
        try {
          if (m_TestLoader.getStructure() != null)
            sp.setInstances(m_TestLoader.getStructure());
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
      sp.addPropertyChangeListener(new PropertyChangeListener() {
	public void propertyChange(PropertyChangeEvent e) {
	  m_TestLoader = sp.getLoader();
	}
      });
      // Add propertychangelistener to update m_TestLoader whenever
      // it changes in the settestframe
      m_SetTestFrame = new JFrame(Messages.getInstance().getString("ClassifierPanel_SetTestSet_SetTestFrame_JFrame_Text"));
      sp.setParentFrame(m_SetTestFrame);   // enable Close-Button
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
   * automatic shape type assignment; classifier error data points have
   * box shape type). For numeric class datasets, the actual data points
   * are stored in plotInstances and plotInfo stores the error (which is
   * later converted to shape size values)
   * @param toPredict the actual data point
   * @param classifier the classifier
   * @param eval the evaluation object to use for evaluating the classifier on
   * the instance to predict
   * @param plotInstances a set of plottable instances
   * @param plotShape additional plotting information (shape)
   * @param plotSize additional plotting information (size)
   */
  public static void processClassifierPrediction(Instance toPredict,
                                           Classifier classifier,
					   Evaluation eval,
					   Instances plotInstances,
					   FastVector plotShape,
					   FastVector plotSize) {
    try {
      double pred = eval.evaluateModelOnceAndRecordPrediction(classifier, 
							      toPredict);

      if (plotInstances != null) {
        double [] values = new double[plotInstances.numAttributes()];
        for (int i = 0; i < plotInstances.numAttributes(); i++) {
          if (i < toPredict.classIndex()) {
            values[i] = toPredict.value(i);
          } else if (i == toPredict.classIndex()) {
            values[i] = pred;
            values[i+1] = toPredict.value(i);
            /* // if the class value of the instances to predict is missing then
            // set it to the predicted value
            if (toPredict.isMissing(i)) {
	    values[i+1] = pred;
	    } */
            i++;
          } else {
            values[i] = toPredict.value(i-1);
          }
        }

        plotInstances.add(new Instance(1.0, values));
        if (toPredict.classAttribute().isNominal()) {
          if (toPredict.isMissing(toPredict.classIndex()) 
              || Instance.isMissingValue(pred)) {
            plotShape.addElement(new Integer(Plot2D.MISSING_SHAPE));
          } else if (pred != toPredict.classValue()) {
            // set to default error point shape
            plotShape.addElement(new Integer(Plot2D.ERROR_SHAPE));
          } else {
            // otherwise set to constant (automatically assigned) point shape
            plotShape.addElement(new Integer(Plot2D.CONST_AUTOMATIC_SHAPE));
          }
          plotSize.addElement(new Integer(Plot2D.DEFAULT_SHAPE_SIZE));
        } else {
          // store the error (to be converted to a point size later)
          Double errd = null;
          if (!toPredict.isMissing(toPredict.classIndex()) && 
              !Instance.isMissingValue(pred)) {
            errd = new Double(pred - toPredict.classValue());
            plotShape.addElement(new Integer(Plot2D.CONST_AUTOMATIC_SHAPE));
          } else {
            // missing shape if actual class not present or prediction is missing
            plotShape.addElement(new Integer(Plot2D.MISSING_SHAPE));
          }
          plotSize.addElement(errd);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Post processes numeric class errors into shape sizes for plotting
   * in the visualize panel
   * @param plotSize a FastVector of numeric class errors
   */
  private void postProcessPlotInfo(FastVector plotSize) {
    int maxpSize = 20;
    double maxErr = Double.NEGATIVE_INFINITY;
    double minErr = Double.POSITIVE_INFINITY;
    double err;
    
    for (int i = 0; i < plotSize.size(); i++) {
      Double errd = (Double)plotSize.elementAt(i);
      if (errd != null) {
	err = Math.abs(errd.doubleValue());
        if (err < minErr) {
	  minErr = err;
	}
	if (err > maxErr) {
	  maxErr = err;
	}
      }
    }
    
    for (int i = 0; i < plotSize.size(); i++) {
      Double errd = (Double)plotSize.elementAt(i);
      if (errd != null) {
	err = Math.abs(errd.doubleValue());
	if (maxErr - minErr > 0) {
	  double temp = (((err - minErr) / (maxErr - minErr)) 
			 * maxpSize);
	  plotSize.setElementAt(new Integer((int)temp), i);
	} else {
	  plotSize.setElementAt(new Integer(1), i);
	}
      } else {
	plotSize.setElementAt(new Integer(1), i);
      }
    }
  }

  /**
   * Sets up the structure for the visualizable instances. This dataset
   * contains the original attributes plus the classifier's predictions
   * for the class as an attribute called "predicted+WhateverTheClassIsCalled".
   * @param trainInstances the instances that the classifier is trained on
   * @return a new set of instances containing one more attribute (predicted
   * class) than the trainInstances
   */
  public static Instances setUpVisualizableInstances(Instances trainInstances) {
    FastVector hv = new FastVector();
    Attribute predictedClass;

    Attribute classAt = trainInstances.attribute(trainInstances.classIndex());
    if (classAt.isNominal()) {
      FastVector attVals = new FastVector();
      for (int i = 0; i < classAt.numValues(); i++) {
	attVals.addElement(classAt.value(i));
      }
      predictedClass = new Attribute(Messages.getInstance().getString("ClassifierPanel_SetUpVisualizableInstances_PredictedClass_Attribute_Text_First") + classAt.name(), attVals);
    } else {
      predictedClass = new Attribute(Messages.getInstance().getString("ClassifierPanel_SetUpVisualizableInstances_PredictedClass_Attribute_Text_Second") + classAt.name());
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
   * outputs the header for the predictions on the data
   * 
   * @param outBuff	the buffer to add the output to
   * @param inst	the data header
   * @param title	the title to print
   */
  protected void printPredictionsHeader(StringBuffer outBuff, Instances inst, String title) {
    outBuff.append(Messages.getInstance().getString("ClassifierPanel_PrintPredictionsHeader_OutBuffer_Text_First") + title 
        + " " +Messages.getInstance().getString("ClassifierPanel_PrintPredictionsHeader_OutBuffer_Text_First_Alpha"));
    outBuff.append(Messages.getInstance().getString("ClassifierPanel_PrintPredictionsHeader_OutBuffer_Text_Second"));
    if (inst.classAttribute().isNominal()) {
      outBuff.append(Messages.getInstance().getString("ClassifierPanel_PrintPredictionsHeader_OutBuffer_Text_Third"));
    }
    if (m_OutputAdditionalAttributesRange != null) {
      outBuff.append(" (");
      boolean first = true;
      for (int i = 0; i < inst.numAttributes() - 1; i++) {
	if (m_OutputAdditionalAttributesRange.isInRange(i)) {
	  if (!first)
	    outBuff.append(",");
	  else
	    first = false;
	  outBuff.append(inst.attribute(i).name());
	}
      }
      outBuff.append(")");
    }
    outBuff.append("\n");
  }
  
  /**
   * Starts running the currently configured classifier with the current
   * settings. This is run in a separate thread, and will only start if
   * there is no classifier already running. The classifier output is sent
   * to the results history panel.
   */
  protected void startClassifier() {

    if (m_RunThread == null) {
      synchronized (this) {
	m_StartBut.setEnabled(false);
	m_StopBut.setEnabled(true);
      }
      m_RunThread = new Thread() {
	public void run() {
	  // Copy the current state of things
	  m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_First"));
	  CostMatrix costMatrix = null;
	  Instances inst = new Instances(m_Instances);
	  DataSource source = null;
          Instances userTestStructure = null;
	  // additional vis info (either shape type or point size)
	  FastVector plotShape = new FastVector();
	  FastVector plotSize = new FastVector();
	  Instances predInstances = null;
	 
	  // for timing
	  long trainTimeStart = 0, trainTimeElapsed = 0;

          try {
            if (m_TestLoader != null && m_TestLoader.getStructure() != null) {
              m_TestLoader.reset();
              source = new DataSource(m_TestLoader);
              userTestStructure = source.getStructure();
            }
          } catch (Exception ex) {
            ex.printStackTrace();
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
	  boolean outputPredictionsText = m_OutputPredictionsTextBut.isSelected();
	  if (m_OutputAdditionalAttributesText.getText().equals("")) {
	    m_OutputAdditionalAttributesRange = null;
	  }
	  else {
	    m_OutputAdditionalAttributesRange = new Range(m_OutputAdditionalAttributesText.getText());
	    m_OutputAdditionalAttributesRange.setUpper(inst.numAttributes() - 1);
	  }

	  String grph = null;

	  int testMode = 0;
	  int numFolds = 10;
          double percent = 66;
	  int classIndex = m_ClassCombo.getSelectedIndex();
	  Classifier classifier = (Classifier) m_ClassifierEditor.getValue();
	  Classifier template = null;
	  try {
	    template = Classifier.makeCopy(classifier);
	  } catch (Exception ex) {
	    m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_LogMessage_Text_First") + ex.getMessage());
	  }
	  Classifier fullClassifier = null;
	  StringBuffer outBuff = new StringBuffer();
	  String name = (new SimpleDateFormat("HH:mm:ss - "))
	  .format(new Date());
	  String cname = classifier.getClass().getName();
	  if (cname.startsWith("weka.classifiers.")) {
	    name += cname.substring("weka.classifiers.".length());
	  } else {
	    name += cname;
	  }
          String cmd = m_ClassifierEditor.getValue().getClass().getName();
          if (m_ClassifierEditor.getValue() instanceof OptionHandler)
            cmd += " " + Utils.joinOptions(((OptionHandler) m_ClassifierEditor.getValue()).getOptions());
	  Evaluation eval = null;
	  try {
	    if (m_CVBut.isSelected()) {
	      testMode = 1;
	      numFolds = Integer.parseInt(m_CVText.getText());
	      if (numFolds <= 1) {
		throw new Exception(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Exception_Text_First"));
	      }
	    } else if (m_PercentBut.isSelected()) {
	      testMode = 2;
	      percent = Double.parseDouble(m_PercentText.getText());
	      if ((percent <= 0) || (percent >= 100)) {
		throw new Exception(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Exception_Text_Second"));
	      }
	    } else if (m_TrainBut.isSelected()) {
	      testMode = 3;
	    } else if (m_TestSplitBut.isSelected()) {
	      testMode = 4;
	      // Check the test instance compatibility
	      if (source == null) {
		throw new Exception(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Exception_Text_Third"));
	      }
	      if (!inst.equalHeaders(userTestStructure)) {
		throw new Exception(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Exception_Text_Fourth"));
	      }
              userTestStructure.setClassIndex(classIndex);
	    } else {
	      throw new Exception(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Exception_Text_Fifth"));
	    }
	    inst.setClassIndex(classIndex);

	    // set up the structure of the plottable instances for 
	    // visualization
            if (saveVis) {
              predInstances = setUpVisualizableInstances(inst);
              predInstances.setClassIndex(inst.classIndex()+1);
            } 

	    // Output some header information
	    m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_LogMessage_Text_Second") + cname);
	    m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_LogMessage_Text_Third") + cmd);
	    if (m_Log instanceof TaskLogger) {
	      ((TaskLogger)m_Log).taskStarted();
	    }
	    outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_First"));
	    outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Second") + cname);
	    if (classifier instanceof OptionHandler) {
	      String [] o = ((OptionHandler) classifier).getOptions();
	      outBuff.append(" " + Utils.joinOptions(o));
	    }
	    outBuff.append("\n");
	    outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Fourth") + inst.relationName() + '\n');
	    outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Sixth") + inst.numInstances() + '\n');
	    outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Eigth") + inst.numAttributes() + '\n');
	    if (inst.numAttributes() < 100) {
	      for (int i = 0; i < inst.numAttributes(); i++) {
		outBuff.append("              " + inst.attribute(i).name()
			       + '\n');
	      }
	    } else {
	      outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Twelveth"));
	    }

	    outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Thirteenth"));
	    switch (testMode) {
	      case 3: // Test on training
		outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Fourteenth"));
		break;
	      case 1: // CV mode
		outBuff.append("" + numFolds + Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Sixteenth"));
		break;
	      case 2: // Percent split
		outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Seventeenth") + percent
		    + Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Eighteenth"));
		break;
	      case 4: // Test on user split
		if (source.isIncremental())
		  outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Nineteenth"));
		else
		  outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_Twentyth")
		      + source.getDataSet().numInstances() + Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_TwentyFirst"));
		break;
	    }
            if (costMatrix != null) {
               outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_TwentySecond"))
               .append(costMatrix.toString()).append("\n");
            }
	    outBuff.append("\n");
	    m_History.addResult(name, outBuff);
	    m_History.setSingle(name);
	    
	    // Build the model and output it.
	    if (outputModel || (testMode == 3) || (testMode == 4)) {
	      m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Second"));

	      trainTimeStart = System.currentTimeMillis();
	      classifier.buildClassifier(inst);
	      trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;
	    }

	    if (outputModel) {
	      outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_TwentySixth"));
	      outBuff.append(classifier.toString() + "\n");
	      outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_TwentyEighth") +
			     Utils.doubleToString(trainTimeElapsed / 1000.0,2)
			     + " " + Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_TwentyNineth"));
	      m_History.updateResult(name);
	      if (classifier instanceof Drawable) {
		grph = null;
		try {
		  grph = ((Drawable)classifier).graph();
		} catch (Exception ex) {
		}
	      }
	      // copy full model for output
	      SerializedObject so = new SerializedObject(classifier);
	      fullClassifier = (Classifier) so.getObject();
	    }
	    
	    switch (testMode) {
	      case 3: // Test on training
	      m_Log.statusMessage("Evaluating on training data...");
	      eval = new Evaluation(inst, costMatrix);
	      
	      if (outputPredictionsText) {
		printPredictionsHeader(outBuff, inst, "training set");
	      }

	      for (int jj=0;jj<inst.numInstances();jj++) {
		processClassifierPrediction(inst.instance(jj), classifier,
					    eval, predInstances, plotShape, 
					    plotSize);
		
		if (outputPredictionsText) { 
		  outBuff.append(predictionText(classifier, inst.instance(jj), jj+1));
		}
		if ((jj % 100) == 0) {
		  m_Log.statusMessage("Evaluating on training data. Processed "
				      +jj+" instances...");
		}
	      }
	      if (outputPredictionsText) {
		outBuff.append("\n");
	      } 
	      outBuff.append("=== Evaluation on training set ===\n");
	      break;

	      case 1: // CV mode
	      m_Log.statusMessage("Randomizing instances...");
	      int rnd = 1;
	      try {
		rnd = Integer.parseInt(m_RandomSeedText.getText().trim());
		// System.err.println("Using random seed "+rnd);
	      } catch (Exception ex) {
		m_Log.logMessage("Trouble parsing random seed value");
		rnd = 1;
	      }
	      Random random = new Random(rnd);
	      inst.randomize(random);
	      if (inst.attribute(classIndex).isNominal()) {
		m_Log.statusMessage("Stratifying instances...");
		inst.stratify(numFolds);
	      }
	      eval = new Evaluation(inst, costMatrix);
      
	      if (outputPredictionsText) {
		printPredictionsHeader(outBuff, inst, "test data");
	      }

	      // Make some splits and do a CV
	      for (int fold = 0; fold < numFolds; fold++) {
		m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Eighth")
				    + (fold + 1) + Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Nineth"));
		Instances train = inst.trainCV(numFolds, fold, random);
		eval.setPriors(train);
		m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Tenth")
				    + (fold + 1) + Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Twelveth"));
		Classifier current = null;
		try {
		  current = Classifier.makeCopy(template);
		} catch (Exception ex) {
		  m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_LogMessage_Text_Fifth") + ex.getMessage());
		}
		current.buildClassifier(train);
		Instances test = inst.testCV(numFolds, fold);
		m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Eleventh")
				    + (fold + 1) + Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Twelveth"));
		for (int jj=0;jj<test.numInstances();jj++) {
		  processClassifierPrediction(test.instance(jj), current,
					      eval, predInstances, plotShape,
					      plotSize);
		  if (outputPredictionsText) { 
		    outBuff.append(predictionText(current, test.instance(jj), jj+1));
		  }
		}
	      }
	      if (outputPredictionsText) {
		outBuff.append("\n");
	      } 
	      if (inst.attribute(classIndex).isNominal()) {
		outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_ThirtyThird"));
	      } else {
		outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_ThirtyFourth"));
	      }
	      break;
		
	      case 2: // Percent split
	      if (!m_PreserveOrderBut.isSelected()) {
		m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Thirteenth"));
		try {
		  rnd = Integer.parseInt(m_RandomSeedText.getText().trim());
		} catch (Exception ex) {
		  m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Fourteenth"));
		  rnd = 1;
		}
		inst.randomize(new Random(rnd));
	      }
	      int trainSize = (int) Math.round(inst.numInstances() * percent / 100);
	      int testSize = inst.numInstances() - trainSize;
	      Instances train = new Instances(inst, 0, trainSize);
	      Instances test = new Instances(inst, trainSize, testSize);
	      m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Fifteenth") + trainSize+ Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Sixteenth"));
	      Classifier current = null;
	      try {
		current = Classifier.makeCopy(template);
	      } catch (Exception ex) {
		m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_LogMessage_Text_Sixth") + ex.getMessage());
	      }
	      current.buildClassifier(train);
	      eval = new Evaluation(train, costMatrix);
	      m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Seventeenth"));
	     
	      if (outputPredictionsText) {
		printPredictionsHeader(outBuff, inst, Messages.getInstance().getString("ClassifierPanel_StartClassifier_PrintPredictionsHeader_Text_First"));
	      }
     
	      for (int jj=0;jj<test.numInstances();jj++) {
		processClassifierPrediction(test.instance(jj), current,
					    eval, predInstances, plotShape,
					    plotSize);
		if (outputPredictionsText) { 
		    outBuff.append(predictionText(current, test.instance(jj), jj+1));
		}
		if ((jj % 100) == 0) {
		  m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Eighteenth")
				      +jj + Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Nineteenth"));
		}
	      }
	      if (outputPredictionsText) {
		outBuff.append("\n");
	      } 
	      outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_ThirtySixth"));
	      break;
		
	      case 4: // Test on user split
	      m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_Twentyth"));
	      eval = new Evaluation(inst, costMatrix);
	      
	      if (outputPredictionsText) {
		printPredictionsHeader(outBuff, inst, Messages.getInstance().getString("ClassifierPanel_StartClassifier_PrintPredictionsHeader_Text_Second"));
	      }

	      Instance instance;
	      int jj = 0;
	      while (source.hasMoreElements(userTestStructure)) {
		instance = source.nextElement(userTestStructure);
		processClassifierPrediction(instance, classifier,
		    eval, predInstances, plotShape,
		    plotSize);
		if (outputPredictionsText) { 
		  outBuff.append(predictionText(classifier, instance, jj+1));
		}
		if ((++jj % 100) == 0) {
		  m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_TwentyFirst")
		      + jj + Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_TwentySecond"));
		}
	      }

	      if (outputPredictionsText) {
		outBuff.append("\n");
	      } 
	      outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_ThirtyEighth"));
	      break;

	      default:
	      throw new Exception(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Exception_Text"));
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

            if (   (fullClassifier instanceof Sourcable) 
                 && m_OutputSourceCode.isSelected()) {
              outBuff.append(Messages.getInstance().getString("ClassifierPanel_StartClassifier_OutBuffer_Text_FourtySecond"));
              outBuff.append(
                Evaluation.wekaStaticWrapper(
                    ((Sourcable) fullClassifier),
                    m_SourceCodeClass.getText()));
            }

	    m_History.updateResult(name);
	    m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_LogMessage_Text_Seventh") + cname);
	    m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_TwentyThird"));
	  } catch (Exception ex) {
	    ex.printStackTrace();
	    m_Log.logMessage(ex.getMessage());
	    JOptionPane.showMessageDialog(ClassifierPanel.this,
					  Messages.getInstance().getString("ClassifierPanel_StartClassifier_JOptionPaneShowMessageDialog_Text_First")
					  + ex.getMessage(),
					  Messages.getInstance().getString("ClassifierPanel_StartClassifier_JOptionPaneShowMessageDialog_Text_Second"),
					  JOptionPane.ERROR_MESSAGE);
	    m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_TwentyFourth"));
	  } finally {
	    try {
              if (!saveVis && outputModel) {
		  FastVector vv = new FastVector();
		  vv.addElement(fullClassifier);
		  Instances trainHeader = new Instances(m_Instances, 0);
		  trainHeader.setClassIndex(classIndex);
		  vv.addElement(trainHeader);
                  if (grph != null) {
		    vv.addElement(grph);
		  }
		  m_History.addObject(name, vv);
              } else if (saveVis && predInstances != null && 
                  predInstances.numInstances() > 0) {
		if (predInstances.attribute(predInstances.classIndex())
		    .isNumeric()) {
		  postProcessPlotInfo(plotSize);
		}
		m_CurrentVis = new VisualizePanel();
		m_CurrentVis.setName(name+" ("+inst.relationName()+")");
		m_CurrentVis.setLog(m_Log);
		PlotData2D tempd = new PlotData2D(predInstances);
		tempd.setShapeSize(plotSize);
		tempd.setShapeType(plotShape);
		tempd.setPlotName(name+" ("+inst.relationName()+")");
		//tempd.addInstanceNumberAttribute();
		
		m_CurrentVis.addPlot(tempd);
		//m_CurrentVis.setColourIndex(predInstances.classIndex()+1);
		m_CurrentVis.setColourIndex(predInstances.classIndex());
	    
                FastVector vv = new FastVector();
                if (outputModel) {
                  vv.addElement(fullClassifier);
                  Instances trainHeader = new Instances(m_Instances, 0);
                  trainHeader.setClassIndex(classIndex);
                  vv.addElement(trainHeader);
                  if (grph != null) {
                    vv.addElement(grph);
                  }
                }
                vv.addElement(m_CurrentVis);
                
                if ((eval != null) && (eval.predictions() != null)) {
                  vv.addElement(eval.predictions());
                  vv.addElement(inst.classAttribute());
                }
                m_History.addObject(name, vv);
	      }
	    } catch (Exception ex) {
	      ex.printStackTrace();
	    }
	    
	    if (isInterrupted()) {
	      m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_LogMessage_Text_Eighth") + cname);
	      m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_StartClassifier_Log_StatusMessage_Text_TwentyFourth"));
	    }

	    synchronized (this) {
	      m_StartBut.setEnabled(true);
	      m_StopBut.setEnabled(false);
	      m_RunThread = null;
	    }
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
   * generates a prediction row for an instance
   * 
   * @param classifier the classifier to use for making the prediction
   * @param inst the instance to predict
   * @param instNum the index of the instance
   * @throws Exception if something goes wrong
   * @return the generated row
   */
  protected String predictionText(Classifier classifier, Instance inst, int instNum) throws Exception {

    //> inst#   actual   predicted   error  probability distribution

    StringBuffer text = new StringBuffer();
    // inst #
    text.append(Utils.padLeft("" + instNum, 6) + " ");
    if (inst.classAttribute().isNominal()) {

      // actual
      if (inst.classIsMissing()) text.append(Utils.padLeft("?", 10) + " ");
      else text.append(Utils.padLeft("" + ((int) inst.classValue()+1) + ":"
				+ inst.stringValue(inst.classAttribute()), 10) + " ");

      // predicted
      double[] probdist = null;
      double pred;
      if (inst.classAttribute().isNominal()) {
	probdist = classifier.distributionForInstance(inst);
	pred = (double) Utils.maxIndex(probdist);
	if (probdist[(int) pred] <= 0.0) pred = Instance.missingValue();
      } else {
	pred = classifier.classifyInstance(inst);
      }
      text.append(Utils.padLeft((Instance.isMissingValue(pred) ? "?" :
				 (((int) pred+1) + ":"
				 + inst.classAttribute().value((int) pred))), 10) + " ");
      // error
      if (pred == inst.classValue()) text.append(Utils.padLeft(" ", 6) + " ");
      else text.append(Utils.padLeft("+", 6) + " ");

      // prob dist
      if (inst.classAttribute().type() == Attribute.NOMINAL) {
	for (int i=0; i<probdist.length; i++) {
	  if (i == (int) pred) text.append(" *");
	  else text.append("  ");
	  text.append(Utils.doubleToString(probdist[i], 5, 3));
	}
      }
    } else {

      // actual
      if (inst.classIsMissing()) text.append(Utils.padLeft("?", 10) + " ");
      else text.append(Utils.doubleToString(inst.classValue(), 10, 3) + " ");

      // predicted
      double pred = classifier.classifyInstance(inst);
      if (Instance.isMissingValue(pred)) text.append(Utils.padLeft("?", 10) + " ");
      else text.append(Utils.doubleToString(pred, 10, 3) + " ");

      // err
      if (!inst.classIsMissing() && !Instance.isMissingValue(pred))
	text.append(Utils.doubleToString(pred - inst.classValue(), 10, 3));
    }
    
    // additional Attributes
    if (m_OutputAdditionalAttributesRange != null) {
      text.append(" (");
      boolean first = true;
      for (int i = 0; i < inst.numAttributes() - 1; i++) {
	if (m_OutputAdditionalAttributesRange.isInRange(i)) {
	  if (!first)
	    text.append(",");
	  else
	    first = false;
	  text.append(inst.toString(i));
	}
      }
      text.append(")");
    }
    
    text.append("\n");
    return text.toString();
  }

  /**
   * Handles constructing a popup menu with visualization options.
   * @param name the name of the result history list entry clicked on by
   * the user
   * @param x the x coordinate for popping up the menu
   * @param y the y coordinate for popping up the menu
   */
  protected void visualize(String name, int x, int y) {
    final String selectedName = name;
    JPopupMenu resultListMenu = new JPopupMenu();
    
    JMenuItem visMainBuffer = new JMenuItem(Messages.getInstance().getString("ClassifierPanel_Visualize_VisMainBuffer_JMenuItem_Text"));
    if (selectedName != null) {
      visMainBuffer.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    m_History.setSingle(selectedName);
	  }
	});
    } else {
      visMainBuffer.setEnabled(false);
    }
    resultListMenu.add(visMainBuffer);
    
    JMenuItem visSepBuffer = new JMenuItem(Messages.getInstance().getString("ClassifierPanel_Visualize_VisSepBuffer_JMenuItem_Text"));
    if (selectedName != null) {
      visSepBuffer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_History.openFrame(selectedName);
	}
      });
    } else {
      visSepBuffer.setEnabled(false);
    }
    resultListMenu.add(visSepBuffer);
    
    JMenuItem saveOutput = new JMenuItem(Messages.getInstance().getString("ClassifierPanel_Visualize_SaveOutput_JMenuItem_Text"));
    if (selectedName != null) {
      saveOutput.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    saveBuffer(selectedName);
	  }
	});
    } else {
      saveOutput.setEnabled(false);
    }
    resultListMenu.add(saveOutput);
    
    JMenuItem deleteOutput = new JMenuItem(Messages.getInstance().getString("ClassifierPanel_Visualize_DeleteOutput_JMenuItem_Text"));
    if (selectedName != null) {
      deleteOutput.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_History.removeResult(selectedName);
	}
      });
    } else {
      deleteOutput.setEnabled(false);
    }
    resultListMenu.add(deleteOutput);

    resultListMenu.addSeparator();
    
    JMenuItem loadModel = new JMenuItem(Messages.getInstance().getString("ClassifierPanel_Visualize_LoadModel_JMenuItem_Text"));
    loadModel.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  loadClassifier();
	}
      });
    resultListMenu.add(loadModel);

    FastVector o = null;
    if (selectedName != null) {
      o = (FastVector)m_History.getNamedObject(selectedName);
    }

    VisualizePanel temp_vp = null;
    String temp_grph = null;
    FastVector temp_preds = null;
    Attribute temp_classAtt = null;
    Classifier temp_classifier = null;
    Instances temp_trainHeader = null;
      
    if (o != null) { 
      for (int i = 0; i < o.size(); i++) {
	Object temp = o.elementAt(i);
	if (temp instanceof Classifier) {
	  temp_classifier = (Classifier)temp;
	} else if (temp instanceof Instances) { // training header
	  temp_trainHeader = (Instances)temp;
	} else if (temp instanceof VisualizePanel) { // normal errors
	  temp_vp = (VisualizePanel)temp;
	} else if (temp instanceof String) { // graphable output
	  temp_grph = (String)temp;
	} else if (temp instanceof FastVector) { // predictions
	  temp_preds = (FastVector)temp;
	} else if (temp instanceof Attribute) { // class attribute
	  temp_classAtt = (Attribute)temp;
	}
      }
    }

    final VisualizePanel vp = temp_vp;
    final String grph = temp_grph;
    final FastVector preds = temp_preds;
    final Attribute classAtt = temp_classAtt;
    final Classifier classifier = temp_classifier;
    final Instances trainHeader = temp_trainHeader;
    
    JMenuItem saveModel = new JMenuItem(Messages.getInstance().getString("ClassifierPanel_Visualize_SaveModel_JMenuItem_Text"));
    if (classifier != null) {
      saveModel.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    saveClassifier(selectedName, classifier, trainHeader);
	  }
	});
    } else {
      saveModel.setEnabled(false);
    }
    resultListMenu.add(saveModel);

    JMenuItem reEvaluate =
      new JMenuItem(Messages.getInstance().getString("ClassifierPanel_Visualize_ReEvaluate_JMenuItem_Text"));
    if (classifier != null && m_TestLoader != null) {
      reEvaluate.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    reevaluateModel(selectedName, classifier, trainHeader);
	  }
	});
    } else {
      reEvaluate.setEnabled(false);
    }
    resultListMenu.add(reEvaluate);
    
    resultListMenu.addSeparator();
    
    JMenuItem visErrors = new JMenuItem(Messages.getInstance().getString("ClassifierPanel_Visualize_VisErrors_JMenuItem_Text"));
    if (vp != null) {
      if ((vp.getXIndex() == 0) && (vp.getYIndex() == 1)) {
	try {
	  vp.setXIndex(vp.getInstances().classIndex());  // class
	  vp.setYIndex(vp.getInstances().classIndex() - 1);  // predicted class
	}
	catch (Exception e) {
	  // ignored
	}
      }
      visErrors.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    visualizeClassifierErrors(vp);
	  }
	});
    } else {
      visErrors.setEnabled(false);
    }
    resultListMenu.add(visErrors);

    JMenuItem visGrph = new JMenuItem(Messages.getInstance().getString("ClassifierPanel_Visualize_VisGrph_JMenuItem_Text_First"));
    if (grph != null) {
	if(((Drawable)temp_classifier).graphType()==Drawable.TREE) {
	    visGrph.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			String title;
			if (vp != null) title = vp.getName();
			else title = selectedName;
			visualizeTree(grph, title);
		    }
		});
	}
	else if(((Drawable)temp_classifier).graphType()==Drawable.BayesNet) {
	    visGrph.setText(Messages.getInstance().getString("ClassifierPanel_Visualize_VisGrph_JMenuItem_Text_Second"));
	    visGrph.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			Thread th = new Thread() {
				public void run() {
				visualizeBayesNet(grph, selectedName);
				}
			    };
			th.start();
		    }
		});
	}
	else
	    visGrph.setEnabled(false);
    } else {
      visGrph.setEnabled(false);
    }
    resultListMenu.add(visGrph);

    JMenuItem visMargin = new JMenuItem(Messages.getInstance().getString("ClassifierPanel_Visualize_VisMargin_JMenuItem_Text"));
    if (preds != null) {
      visMargin.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    try {
	      MarginCurve tc = new MarginCurve();
	      Instances result = tc.getCurve(preds);
	      VisualizePanel vmc = new VisualizePanel();
	      vmc.setName(result.relationName());
	      vmc.setLog(m_Log);
	      PlotData2D tempd = new PlotData2D(result);
	      tempd.setPlotName(result.relationName());
	      tempd.addInstanceNumberAttribute();
	      vmc.addPlot(tempd);
	      visualizeClassifierErrors(vmc);
	    } catch (Exception ex) {
	      ex.printStackTrace();
	    }
	  }
	});
    } else {
      visMargin.setEnabled(false);
    }
    resultListMenu.add(visMargin);

    JMenu visThreshold = new JMenu(Messages.getInstance().getString("ClassifierPanel_Visualize_VisThreshold_JMenu_Text"));
    if (preds != null && classAtt != null) {
      for (int i = 0; i < classAtt.numValues(); i++) {
	JMenuItem clv = new JMenuItem(classAtt.value(i));
	final int classValue = i;
	clv.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      try {
		ThresholdCurve tc = new ThresholdCurve();
		Instances result = tc.getCurve(preds, classValue);
		//VisualizePanel vmc = new VisualizePanel();
		ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
		vmc.setROCString(Messages.getInstance().getString("ClassifierPanel_Visualize_VMC_SetROCString_Text_First") + 
				 Utils.doubleToString(ThresholdCurve.getROCArea(result), 4) + Messages.getInstance().getString("ClassifierPanel_Visualize_VMC_SetROCString_Text_Second"));
		vmc.setLog(m_Log);
		vmc.setName(result.relationName()+Messages.getInstance().getString("ClassifierPanel_Visualize_VMC_SetName_Text_First") +
			    classAtt.value(classValue) + Messages.getInstance().getString("ClassifierPanel_Visualize_VMC_SetName_Text_Second"));
		PlotData2D tempd = new PlotData2D(result);
		tempd.setPlotName(result.relationName());
		tempd.addInstanceNumberAttribute();
		// specify which points are connected
		boolean[] cp = new boolean[result.numInstances()];
		for (int n = 1; n < cp.length; n++)
		  cp[n] = true;
		tempd.setConnectPoints(cp);
		// add plot
		vmc.addPlot(tempd);
		visualizeClassifierErrors(vmc);
	      } catch (Exception ex) {
		ex.printStackTrace();
	      }
	      }
	  });
	  visThreshold.add(clv);
      }
    } else {
      visThreshold.setEnabled(false);
    }
    resultListMenu.add(visThreshold);
    
    JMenu visCostBenefit = new JMenu(Messages.getInstance().getString("ClassifierPanel_Visualize_VisCostBenefit_JMenu_Text"));
    if ((preds != null) && (classAtt != null) && (classAtt.isNominal())) {
      for (int i = 0; i < classAtt.numValues(); i++) {
        JMenuItem clv = new JMenuItem(classAtt.value(i));
        final int classValue = i;
        clv.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              try {
                ThresholdCurve tc = new ThresholdCurve();
                Instances result = tc.getCurve(preds, classValue);

                // Create a dummy class attribute with the chosen
                // class value as index 0 (if necessary).
                Attribute classAttToUse = classAtt;
                if (classValue != 0) {
                  FastVector newNames = new FastVector();
                  newNames.addElement(classAtt.value(classValue));
                  for (int k = 0; k < classAtt.numValues(); k++) {
                    if (k != classValue) {
                      newNames.addElement(classAtt.value(k));
                    }
                  }
                  classAttToUse = new Attribute(classAtt.name(), newNames);
                }
                
                CostBenefitAnalysis cbAnalysis = new CostBenefitAnalysis();
                
                PlotData2D tempd = new PlotData2D(result);
                tempd.setPlotName(result.relationName());
                tempd.m_alwaysDisplayPointsOfThisSize = 10;
                // specify which points are connected
                boolean[] cp = new boolean[result.numInstances()];
                for (int n = 1; n < cp.length; n++)
                  cp[n] = true;
                tempd.setConnectPoints(cp);
                
                String windowTitle = "";
                if (classifier != null) {
                  String cname = classifier.getClass().getName();
                  if (cname.startsWith("weka.classifiers.")) {
                    windowTitle = "" + cname.substring("weka.classifiers.".length()) + " ";
                  }
                }
                windowTitle += Messages.getInstance().getString("ClassifierPanel_Visualize_WindowTitle_Text_First") + classAttToUse.value(0) + Messages.getInstance().getString("ClassifierPanel_Visualize_WindowTitle_Text_Second");
                
                // add plot
                cbAnalysis.setCurveData(tempd, classAttToUse);
                visualizeCostBenefitAnalysis(cbAnalysis, windowTitle);
              } catch (Exception ex) {
                ex.printStackTrace();
              }
              }
          });
          visCostBenefit.add(clv);
      }
    } else {
      visCostBenefit.setEnabled(false);
    }
    resultListMenu.add(visCostBenefit);

    JMenu visCost = new JMenu(Messages.getInstance().getString("ClassifierPanel_VisCost_JMenu_Text"));
    if (preds != null && classAtt != null) {
      for (int i = 0; i < classAtt.numValues(); i++) {
	JMenuItem clv = new JMenuItem(classAtt.value(i));
	final int classValue = i;
	clv.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      try {
		CostCurve cc = new CostCurve();
		Instances result = cc.getCurve(preds, classValue);
		VisualizePanel vmc = new VisualizePanel();
		vmc.setLog(m_Log);
		vmc.setName(result.relationName()+Messages.getInstance().getString("ClassifierPanel_Visualize_VMC_SetName_Text_Third") +
			    classAtt.value(classValue) + Messages.getInstance().getString("ClassifierPanel_Visualize_VMC_SetName_Text_Fourth"));
		PlotData2D tempd = new PlotData2D(result);
		tempd.m_displayAllPoints = true;
		tempd.setPlotName(result.relationName());
		boolean [] connectPoints = 
		  new boolean [result.numInstances()];
		for (int jj = 1; jj < connectPoints.length; jj+=2) {
		  connectPoints[jj] = true;
		}
		tempd.setConnectPoints(connectPoints);
		//		  tempd.addInstanceNumberAttribute();
		vmc.addPlot(tempd);
		visualizeClassifierErrors(vmc);
	      } catch (Exception ex) {
		ex.printStackTrace();
	      }
	    }
	  });
	visCost.add(clv);
      }
    } else {
      visCost.setEnabled(false);
    }
    resultListMenu.add(visCost);
    
    JMenu visPlugins = new JMenu(Messages.getInstance().getString("ClassifierPanel_Visualize_VisPlugins_JMenu_Text"));
    Vector pluginsVector = GenericObjectEditor.getClassnames(VisualizePlugin.class.getName());
    boolean availablePlugins = false;
    for (int i=0; i<pluginsVector.size(); i++) {
      String className = (String)(pluginsVector.elementAt(i));
      try {
        VisualizePlugin plugin = (VisualizePlugin) Class.forName(className).newInstance();
        if (plugin == null)
          continue;
        availablePlugins = true;
        JMenuItem pluginMenuItem = plugin.getVisualizeMenuItem(preds, classAtt);
        Version version = new Version();
        if (pluginMenuItem != null) {
          if (version.compareTo(plugin.getMinVersion()) < 0)
            pluginMenuItem.setText(pluginMenuItem.getText() + Messages.getInstance().getString("ClassifierPanel_Visualize_PluginMenuItemSetText_Text_First"));
          if (version.compareTo(plugin.getMaxVersion()) >= 0)
            pluginMenuItem.setText(pluginMenuItem.getText() + Messages.getInstance().getString("ClassifierPanel_Visualize_PluginMenuItemSetText_Text_Second"));
          visPlugins.add(pluginMenuItem);
        }
      }
      catch (ClassNotFoundException cnfe) {
        //System.out.println("Visualize plugin ClassNotFoundException " + cnfe.getMessage());
      }
      catch (InstantiationException ie) {
        //System.out.println("Visualize plugin InstantiationException " + ie.getMessage());
      }
      catch (IllegalAccessException iae) {
        //System.out.println("Visualize plugin IllegalAccessException " + iae.getMessage());
      }
    }
    if (availablePlugins)
      resultListMenu.add(visPlugins);

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
      new javax.swing.JFrame(Messages.getInstance().getString("ClassifierPanel_VisualizeTree_JF_JFrame_Text") + treeName);
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
    tv.fitToScreen();
  }

  /**
   * Pops up a GraphVisualizer for the BayesNet classifier from the currently
   * selected item in the results list
   * 
   * @param XMLBIF the description of the graph in XMLBIF ver. 0.3
   * @param graphName the name of the graph
   */
  protected void visualizeBayesNet(String XMLBIF, String graphName) {
    final javax.swing.JFrame jf = 
      new javax.swing.JFrame(Messages.getInstance().getString("ClassifierPanel_VisualizeBayesNet_JF_JFrame_Text") + graphName);
    jf.setSize(500,400);
    jf.getContentPane().setLayout(new BorderLayout());
    GraphVisualizer gv = new GraphVisualizer();
    try { gv.readBIF(XMLBIF);
    }
    catch(BIFFormatException be) { System.err.println(Messages.getInstance().getString("ClassifierPanel_VisualizeBayesNet_Error_Text")); be.printStackTrace(); }
    gv.layoutGraph();

    jf.getContentPane().add(gv, BorderLayout.CENTER);
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
	new javax.swing.JFrame(Messages.getInstance().getString("ClassifierPanel_VisualizeClassifierErrors_JF_JFrame_Text") + plotName);
	jf.setSize(600,400);
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
   * Pops up the Cost/Benefit analysis panel.
   * 
   * @param cb the CostBenefitAnalysis panel to pop up
   */
  protected void visualizeCostBenefitAnalysis(CostBenefitAnalysis cb, 
      String classifierAndRelationName) {
    if (cb != null) {
      String windowTitle = Messages.getInstance().getString("ClassifierPanel_VisualizeCostBenefitAnalysis_WindowTitle_Text");
      if (classifierAndRelationName != null) {
        windowTitle += "- " + classifierAndRelationName;
      }
      final javax.swing.JFrame jf = 
        new javax.swing.JFrame(windowTitle);
        jf.setSize(1000,600);
        jf.getContentPane().setLayout(new BorderLayout());

        jf.getContentPane().add(cb, BorderLayout.CENTER);
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
   * @param name the name of the buffer to save
   */
  protected void saveBuffer(String name) {
    StringBuffer sb = m_History.getNamedBuffer(name);
    if (sb != null) {
      if (m_SaveOut.save(sb)) {
	m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_SaveBuffer_Log_LogMessage_Text"));
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
   * Saves the currently selected classifier
   * 
   * @param name the name of the run
   * @param classifier the classifier to save
   * @param trainHeader the header of the training instances
   */
  protected void saveClassifier(String name, Classifier classifier,
				Instances trainHeader) {

    File sFile = null;
    boolean saveOK = true;
 
    int returnVal = m_FileChooser.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      sFile = m_FileChooser.getSelectedFile();
      if (!sFile.getName().toLowerCase().endsWith(MODEL_FILE_EXTENSION)) {
	sFile = new File(sFile.getParent(), sFile.getName() 
			 + MODEL_FILE_EXTENSION);
      }
      m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_Log_StatusMessage_Text"));
      
      try {
	OutputStream os = new FileOutputStream(sFile);
	if (sFile.getName().endsWith(".gz")) {
	  os = new GZIPOutputStream(os);
	}
	ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);
	objectOutputStream.writeObject(classifier);
	if (trainHeader != null) objectOutputStream.writeObject(trainHeader);
	objectOutputStream.flush();
	objectOutputStream.close();
      } catch (Exception e) {
	
	JOptionPane.showMessageDialog(null, e, Messages.getInstance().getString("ClassifierPanel_SaveClassifier_JOptionPaneShowMessageDialog_Text_First"),
				      JOptionPane.ERROR_MESSAGE);
	saveOK = false;
      }
      if (saveOK)
	m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_Log_LogMessage_Text_First") + name
			 + Messages.getInstance().getString("ClassifierPanel_SaveClassifier_Log_LogMessage_Text_Second") + sFile.getName() + Messages.getInstance().getString("ClassifierPanel_SaveClassifier_Log_LogMessage_Text_Third"));
      m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_JOptionPaneShowMessageDialog_Text"));
    }
  }

  /**
   * Loads a classifier
   */
  protected void loadClassifier() {

    int returnVal = m_FileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File selected = m_FileChooser.getSelectedFile();
      Classifier classifier = null;
      Instances trainHeader = null;

      m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_LoadClassifier_Log_StatusMessage_Text_First"));

      try {
	InputStream is = new FileInputStream(selected);
	if (selected.getName().endsWith(PMML_FILE_EXTENSION)) {
	  PMMLModel model = PMMLFactory.getPMMLModel(is, m_Log);
	  if (model instanceof PMMLClassifier) {
	    classifier = (PMMLClassifier)model;
	    /*trainHeader = 
	      ((PMMLClassifier)classifier).getMiningSchema().getMiningSchemaAsInstances(); */
	  } else {
	    throw new Exception(Messages.getInstance().getString("ClassifierPanel_LoadClassifier_Exception_Text"));
	  }
	} else {
	if (selected.getName().endsWith(".gz")) {
	  is = new GZIPInputStream(is);
	}
	ObjectInputStream objectInputStream = new ObjectInputStream(is);
	classifier = (Classifier) objectInputStream.readObject();
	try { // see if we can load the header
	  trainHeader = (Instances) objectInputStream.readObject();
	} catch (Exception e) {} // don't fuss if we can't
	objectInputStream.close();
	}
      } catch (Exception e) {
	
	JOptionPane.showMessageDialog(null, e, Messages.getInstance().getString("ClassifierPanel_LoadClassifier_JOptionPaneShowMessageDialog_Text"),
				      JOptionPane.ERROR_MESSAGE);
      }	

      m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_LoadClassifier_Log_StatusMessage_Text_Second"));
      
      if (classifier != null) {
	m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_Log_LogMessage_Text_Fourth") + selected.getName()+ Messages.getInstance().getString("ClassifierPanel_SaveClassifier_Log_LogMessage_Text_Fifth"));
	String name = (new SimpleDateFormat("HH:mm:ss - ")).format(new Date());
	String cname = classifier.getClass().getName();
	if (cname.startsWith("weka.classifiers."))
	  cname = cname.substring("weka.classifiers.".length());
	name += cname + Messages.getInstance().getString("ClassifierPanel_SaveClassifier_Name_Text_First") + selected.getName() + Messages.getInstance().getString("ClassifierPanel_SaveClassifier_Name_Text_Second");
	StringBuffer outBuff = new StringBuffer();

	outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_First"));
	outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Second") + selected.getName() + "\n");
	outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Fourth") + classifier.getClass().getName());
	if (classifier instanceof OptionHandler) {
	  String [] o = ((OptionHandler) classifier).getOptions();
	  outBuff.append(" " + Utils.joinOptions(o));
	}
	outBuff.append("\n");
	if (trainHeader != null) {
	  outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Fifth") + trainHeader.relationName() + '\n');
	  outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Seventh") + trainHeader.numAttributes() + '\n');
	  if (trainHeader.numAttributes() < 100) {
	    for (int i = 0; i < trainHeader.numAttributes(); i++) {
	      outBuff.append("              " + trainHeader.attribute(i).name()
			     + '\n');
	    }
	  } else {
	    outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Nineth"));
	  }
	} else {
	  outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Tenth"));
	} 

	outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Eleventh"));
	outBuff.append(classifier.toString() + "\n");
	
	m_History.addResult(name, outBuff);
	m_History.setSingle(name);
	FastVector vv = new FastVector();
	vv.addElement(classifier);
	if (trainHeader != null) vv.addElement(trainHeader);
	// allow visualization of graphable classifiers
	String grph = null;
	if (classifier instanceof Drawable) {
	  try {
	    grph = ((Drawable)classifier).graph();
	  } catch (Exception ex) {
	  }
	}
	if (grph != null) vv.addElement(grph);
	
	m_History.addObject(name, vv);
      }
    }
  }
  
  /**
   * Re-evaluates the named classifier with the current test set. Unpredictable
   * things will happen if the data set is not compatible with the classifier.
   *
   * @param name the name of the classifier entry
   * @param classifier the classifier to evaluate
   * @param trainHeader the header of the training set
   */
  protected void reevaluateModel(final String name, 
                                 final Classifier classifier, 
                                 final Instances trainHeader) {

    if (m_RunThread == null) {
      synchronized (this) {
	m_StartBut.setEnabled(false);
	m_StopBut.setEnabled(true);
      }
      m_RunThread = new Thread() {
          public void run() {
            // Copy the current state of things
            m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_StatusMessage_Text_First"));

            StringBuffer outBuff = m_History.getNamedBuffer(name);
            DataSource source = null;
            Instances userTestStructure = null;
            // additional vis info (either shape type or point size)
            FastVector plotShape = new FastVector();
            FastVector plotSize = new FastVector();
            Instances predInstances = null;

            CostMatrix costMatrix = null;
            if (m_EvalWRTCostsBut.isSelected()) {
              costMatrix = new CostMatrix((CostMatrix) m_CostMatrixEditor
                                          .getValue());
            }    
            boolean outputConfusion = m_OutputConfusionBut.isSelected();
            boolean outputPerClass = m_OutputPerClassBut.isSelected();
            boolean outputSummary = true;
            boolean outputEntropy = m_OutputEntropyBut.isSelected();
            boolean saveVis = m_StorePredictionsBut.isSelected();
            boolean outputPredictionsText = 
              m_OutputPredictionsTextBut.isSelected();
            String grph = null;    
            Evaluation eval = null;

            try {

              boolean incrementalLoader = (m_TestLoader instanceof IncrementalConverter);
              if (m_TestLoader != null && m_TestLoader.getStructure() != null) {
                m_TestLoader.reset();
                source = new DataSource(m_TestLoader);
                userTestStructure = source.getStructure();
              }
              // Check the test instance compatibility
              if (source == null) {
                throw new Exception(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Exception_Text_First"));
              }
              if (trainHeader != null) {
                if (trainHeader.classIndex() > 
                    userTestStructure.numAttributes()-1)
                  throw new Exception(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Exception_Text_Second"));
                userTestStructure.setClassIndex(trainHeader.classIndex());
                if (!trainHeader.equalHeaders(userTestStructure)) {
                  throw new Exception(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Exception_Text_Third"));
                }
              } else {
        	if (classifier instanceof PMMLClassifier) {
        	  // set the class based on information in the mining schema
        	  Instances miningSchemaStructure = 
        	    ((PMMLClassifier)classifier).getMiningSchema().getMiningSchemaAsInstances();
        	  String className = miningSchemaStructure.classAttribute().name();
        	  Attribute classMatch = userTestStructure.attribute(className);
        	  if (classMatch == null) {
        	    throw new Exception(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Exception_Text_Fourth") 
        		+ className + Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Exception_Text_Fifth"));
        	  }
        	  userTestStructure.setClass(classMatch);
        	} else {
        	  userTestStructure.
        	    setClassIndex(userTestStructure.numAttributes()-1);
        	}
              }
              if (m_Log instanceof TaskLogger) {
                ((TaskLogger)m_Log).taskStarted();
              }
              m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_StatusMessage_Text_Second"));
              m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_LogMessage_Text_First") + name 
                               + Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_LogMessage_Text_Second"));
              eval = new Evaluation(userTestStructure, costMatrix);
              eval.useNoPriors();
      
              // set up the structure of the plottable instances for 
              // visualization if selected
              if (saveVis) {
                predInstances = setUpVisualizableInstances(userTestStructure);
                predInstances.setClassIndex(userTestStructure.classIndex()+1);
              }
      
              outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Twelveth"));
              outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Thirteenth"));  
              outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Fourteenth") 
                             + userTestStructure.relationName() + '\n');
              if (incrementalLoader)
        	outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Sixteenth"));
              else
        	outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Seventeenth") + source.getDataSet().numInstances() + "\n");
              outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_Nineteenth") 
        	  + userTestStructure.numAttributes() + "\n\n");
              if (trainHeader == null &&
                  !(classifier instanceof
                      weka.classifiers.pmml.consumer.PMMLClassifier)) {

                outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_TwentyFirst"));

              }

              if (outputPredictionsText) {
                outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_TwentySecond"));
                outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_TwentyThird"));
                if (userTestStructure.classAttribute().isNominal()) {
                  outBuff.append(Messages.getInstance().getString("ClassifierPanel_SaveClassifier_OutBuffer_Text_TwentyFourth"));
                }
                outBuff.append("\n");
              }

	      Instance instance;
	      int jj = 0;
	      while (source.hasMoreElements(userTestStructure)) {
		instance = source.nextElement(userTestStructure);
		processClassifierPrediction(instance, classifier,
		    eval, predInstances, plotShape,
		    plotSize);
		if (outputPredictionsText) { 
		  outBuff.append(predictionText(classifier, instance, jj+1));
		}
		if ((++jj % 100) == 0) {
		  m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_StatusMessage_Text_Third")
		      +jj + Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_StatusMessage_Text_Fourth"));
		}
	      }

              if (outputPredictionsText) {
                outBuff.append("\n");
              } 
      
              if (outputSummary) {
                outBuff.append(eval.toSummaryString(outputEntropy) + "\n");
              }
      
              if (userTestStructure.classAttribute().isNominal()) {
	
                if (outputPerClass) {
                  outBuff.append(eval.toClassDetailsString() + "\n");
                }
	
                if (outputConfusion) {
                  outBuff.append(eval.toMatrixString() + "\n");
                }
              }
      
              m_History.updateResult(name);
              m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_LogMessage_Text_Third"));
              m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_StatusMessage_Text_Fifth"));
            } catch (Exception ex) {
              ex.printStackTrace();
              m_Log.logMessage(ex.getMessage());
              m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_StatusMessage_Text_Sixth"));

              ex.printStackTrace();
              m_Log.logMessage(ex.getMessage());
              JOptionPane.showMessageDialog(ClassifierPanel.this,
            		  Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_JOptionPaneShowMessageDialog_Text_First")
                                            + ex.getMessage(),
                                            Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_JOptionPaneShowMessageDialog_Text_Second"),
                                            JOptionPane.ERROR_MESSAGE);
              m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_StatusMessage_Text_Seventh"));
            } finally {
              try {
        	if (classifier instanceof PMMLClassifier) {
        	  // signal the end of the scoring run so
        	  // that the initialized state can be reset
        	  // (forces the field mapping to be recomputed
        	  // for the next scoring run).
        	  ((PMMLClassifier)classifier).done();
        	}
        	
                if (predInstances != null && predInstances.numInstances() > 0) {
                  if (predInstances.attribute(predInstances.classIndex())
                      .isNumeric()) {
                    postProcessPlotInfo(plotSize);
                  }
                  m_CurrentVis = new VisualizePanel();
                  m_CurrentVis.setName(name+" ("
                                       +userTestStructure.relationName()+")");
                  m_CurrentVis.setLog(m_Log);
                  PlotData2D tempd = new PlotData2D(predInstances);
                  tempd.setShapeSize(plotSize);
                  tempd.setShapeType(plotShape);
                  tempd.setPlotName(name+" ("+userTestStructure.relationName()
                                    +")");
                  //tempd.addInstanceNumberAttribute();
	  
                  m_CurrentVis.addPlot(tempd);
                  m_CurrentVis.setColourIndex(predInstances.classIndex());
                  //m_CurrentVis.setColourIndex(predInstances.classIndex()+1);
	  
                  if (classifier instanceof Drawable) {
                    try {
                      grph = ((Drawable)classifier).graph();
                    } catch (Exception ex) {
                    }
                  }

                  if (saveVis) {
                    FastVector vv = new FastVector();
                    vv.addElement(classifier);
                    if (trainHeader != null) vv.addElement(trainHeader);
                    vv.addElement(m_CurrentVis);
                    if (grph != null) {
                      vv.addElement(grph);
                    }
                    if ((eval != null) && (eval.predictions() != null)) {
                      vv.addElement(eval.predictions());
                      vv.addElement(userTestStructure.classAttribute());
                    }
                    m_History.addObject(name, vv);
                  } else {
                    FastVector vv = new FastVector();
                    vv.addElement(classifier);
                    if (trainHeader != null) vv.addElement(trainHeader);
                    m_History.addObject(name, vv);
                  }
                }
              } catch (Exception ex) {
                ex.printStackTrace();
              }
              if (isInterrupted()) {
                m_Log.logMessage(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_LogMessage_Text_Fourth"));
                m_Log.statusMessage(Messages.getInstance().getString("ClassifierPanel_ReEvaluateModel_Log_StatusMessage_Text_Seventh"));
              }

              synchronized (this) {
                m_StartBut.setEnabled(true);
                m_StopBut.setEnabled(false);
                m_RunThread = null;
              }

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
   * updates the capabilities filter of the GOE
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

    m_StartBut.setEnabled(true);
    // Check capabilities
    Capabilities currentFilter = m_ClassifierEditor.getCapabilitiesFilter();
    Classifier classifier = (Classifier) m_ClassifierEditor.getValue();
    Capabilities currentSchemeCapabilities =  null;
    if (classifier != null && currentFilter != null && 
        (classifier instanceof CapabilitiesHandler)) {
      currentSchemeCapabilities = ((CapabilitiesHandler)classifier).getCapabilities();
          
      if (!currentSchemeCapabilities.supportsMaybe(currentFilter) &&
          !currentSchemeCapabilities.supports(currentFilter)) {
        m_StartBut.setEnabled(false);
      }
    }
  }
  
  /**
   * method gets called in case of a change event
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
   * about changes in the data)
   * 
   * @param parent	the parent frame
   */
  public void setExplorer(Explorer parent) {
    m_Explorer = parent;
  }
  
  /**
   * returns the parent Explorer frame
   * 
   * @return		the parent
   */
  public Explorer getExplorer() {
    return m_Explorer;
  }
  
  /**
   * Returns the title for the tab in the Explorer
   * 
   * @return 		the title of this tab
   */
  public String getTabTitle() {
    return Messages.getInstance().getString("ClassifierPanel_GetTabTitle_Text");
  }
  
  /**
   * Returns the tooltip for the tab in the Explorer
   * 
   * @return 		the tooltip of this tab
   */
  public String getTabTitleToolTip() {
    return Messages.getInstance().getString("ClassifierPanel_GetTabTitleToolTip_Text");
  }
  
  /**
   * Tests out the classifier panel from the command line.
   *
   * @param args may optionally contain the name of a dataset to load.
   */
  public static void main(String [] args) {

    try {
      final javax.swing.JFrame jf =
	new javax.swing.JFrame(Messages.getInstance().getString("ClassifierPanel_Main_JFrame_Text"));
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
	System.err.println(Messages.getInstance().getString("ClassifierPanel_Main_Error_Text") + args[0]);
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
