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
 *    OutputFormatDialog.java
 *    Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.experiment;

import weka.core.ClassDiscovery;
import weka.experiment.ResultMatrix;
import weka.experiment.ResultMatrixPlainText;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/** 
 * A dialog for setting various output format parameters.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.7 $
 */
public class OutputFormatDialog
  extends JDialog {

  /** for serialization */
  private static final long serialVersionUID = 2169792738187807378L;

  /** Signifies an OK property selection */
  public static final int APPROVE_OPTION = 0;

  /** Signifies a cancelled property selection */
  public static final int CANCEL_OPTION = 1;

  /** the result of the user's action, either OK or CANCEL */
  protected int m_Result = CANCEL_OPTION;
  
  /** the different classes for outputting the comparison tables */
  protected static Vector m_OutputFormatClasses = null;
  
  /** the different names of matrices for outputting the comparison tables */
  protected static Vector m_OutputFormatNames = null;
  
  /** determine all classes inheriting from the ResultMatrix (in the same
   * package!)
   * @see ResultMatrix
   * @see ClassDiscovery */
  static {
    Vector classes = ClassDiscovery.find(
                      ResultMatrix.class.getName(),
                      ResultMatrix.class.getPackage().getName());

    // set names and classes
    m_OutputFormatClasses = new Vector();
    m_OutputFormatNames   = new Vector();
    for (int i = 0; i < classes.size(); i++) {
      try {
        Class cls = Class.forName(classes.get(i).toString());
        ResultMatrix matrix = (ResultMatrix) cls.newInstance();
        m_OutputFormatClasses.add(cls);
        m_OutputFormatNames.add(matrix.getDisplayName());
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /** the output format specific matrix */
  protected Class m_ResultMatrix = ResultMatrixPlainText.class;

  /** lets the user choose the format for the output */
  protected JComboBox m_OutputFormatComboBox = new JComboBox(m_OutputFormatNames);

  /** the spinner to choose the precision for the mean from */
  protected JSpinner m_MeanPrecSpinner = new JSpinner();

  /** the spinner to choose the precision for the std. deviation from */
  protected JSpinner m_StdDevPrecSpinner = new JSpinner();

  /** the checkbox for outputting the average */
  protected JCheckBox m_ShowAverageCheckBox = new JCheckBox("");

  /** the checkbox for the removing of filter classnames */
  protected JCheckBox m_RemoveFilterNameCheckBox = new JCheckBox("");
  
  /** Click to activate the current set parameters */
  protected JButton m_OkButton = new JButton("OK");

  /** Click to cancel the dialog */
  protected JButton m_CancelButton = new JButton("Cancel");
  
  /** the number of digits after the period (= precision) for printing the mean */
  protected int m_MeanPrec = 2;
  
  /** the number of digits after the period (= precision) for printing the std.
   * deviation */
  protected int m_StdDevPrec = 2;

  /** whether to remove the filter names from the names */
  protected boolean m_RemoveFilterName = false;

  /** whether to show the average too */
  protected boolean m_ShowAverage = false;

  /**
   * initializes the dialog with the given parent frame
   * 
   * @param parent the parent of this dialog
   */
  public OutputFormatDialog(Frame parent) {
    super(parent, "Output Format...", true);
    createDialog();
  }
  
  /**
   * performs the creation of the dialog and all its components
   */
  protected void createDialog() {
    JPanel              panel;
    SpinnerNumberModel  model;
    JLabel              label;
    
    getContentPane().setLayout(new BorderLayout());
    
    panel = new JPanel(new GridLayout(5, 2));
    getContentPane().add(panel, BorderLayout.CENTER);
    
    // Precision
    model = (SpinnerNumberModel) m_MeanPrecSpinner.getModel();
    model.setMaximum(new Integer(20));
    model.setMinimum(new Integer(0));
    model = (SpinnerNumberModel) m_StdDevPrecSpinner.getModel();
    model.setMaximum(new Integer(20));
    model.setMinimum(new Integer(0));
    label = new JLabel("Mean Precision");
    label.setDisplayedMnemonic('M');
    label.setLabelFor(m_MeanPrecSpinner);
    panel.add(label);
    panel.add(m_MeanPrecSpinner);
    label = new JLabel("StdDev. Precision");
    label.setDisplayedMnemonic('S');
    label.setLabelFor(m_StdDevPrecSpinner);
    panel.add(label);
    panel.add(m_StdDevPrecSpinner);
    
    // Format
    label = new JLabel("Output Format");
    label.setDisplayedMnemonic('F');
    label.setLabelFor(m_OutputFormatComboBox);
    panel.add(label);
    panel.add(m_OutputFormatComboBox);
    m_OutputFormatComboBox.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  getData();
	}
      });

    // Average
    label = new JLabel("Show Average");
    label.setDisplayedMnemonic('A');
    label.setLabelFor(m_ShowAverageCheckBox);
    panel.add(label);
    panel.add(m_ShowAverageCheckBox);

    // Remove filter classname
    label = new JLabel("Remove filter classnames");
    label.setDisplayedMnemonic('R');
    label.setLabelFor(m_RemoveFilterNameCheckBox);
    panel.add(label);
    panel.add(m_RemoveFilterNameCheckBox);
    
    // Buttons
    panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    getContentPane().add(panel, BorderLayout.SOUTH);
    m_CancelButton.setMnemonic('C');
    m_CancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_Result = CANCEL_OPTION;
        setVisible(false);
      }
    });
    m_OkButton.setMnemonic('O');
    m_OkButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        getData();
        m_Result = APPROVE_OPTION;
        setVisible(false);
      }
    });
    panel.add(m_OkButton);
    panel.add(m_CancelButton);

    // default button
    getRootPane().setDefaultButton(m_OkButton);

    pack();
  }
  
  /**
   * initializes the GUI components with the data
   */
  private void setData() {
    // Precision
    m_MeanPrecSpinner.setValue(new Integer(m_MeanPrec));
    m_StdDevPrecSpinner.setValue(new Integer(m_StdDevPrec));

    // average
    m_ShowAverageCheckBox.setSelected(m_ShowAverage);

    // filter names
    m_RemoveFilterNameCheckBox.setSelected(m_RemoveFilterName);
    
    // format (must be last, since getData() will be called!)
    for (int i = 0; i < m_OutputFormatClasses.size(); i++) {
      if (m_OutputFormatClasses.get(i).equals(m_ResultMatrix)) {
        m_OutputFormatComboBox.setSelectedItem(m_OutputFormatNames.get(i));
        break;
      }
    }
  }    
  
  /**
   *  gets the data from GUI components
   */
  private void getData() {
    // Precision
    m_MeanPrec   = Integer.parseInt(m_MeanPrecSpinner.getValue().toString());
    m_StdDevPrec = Integer.parseInt(m_StdDevPrecSpinner.getValue().toString());

    // average
    m_ShowAverage = m_ShowAverageCheckBox.isSelected();

    // filter names
    m_RemoveFilterName = m_RemoveFilterNameCheckBox.isSelected();
    
    // format
    m_ResultMatrix = (Class) m_OutputFormatClasses.get(
                        m_OutputFormatComboBox.getSelectedIndex());
  }
  
  /**
   * Sets the precision of the mean output
   * @param precision the number of digits used in printing the mean
   */
  public void setMeanPrec(int precision) {
    m_MeanPrec = precision;
  }

  /**
   * Gets the precision used for printing the mean
   * @return the number of digits used in printing the mean
   */
  public int getMeanPrec() {
    return m_MeanPrec;
  }

  /**
   * Sets the precision of the std. deviation output
   * @param precision the number of digits used in printing the std. deviation
   */
  public void setStdDevPrec(int precision) {
    m_StdDevPrec = precision;
  }

  /**
   * Gets the precision used for printing the std. deviation
   * @return the number of digits used in printing the std. deviation
   */
  public int getStdDevPrec() {
    return m_StdDevPrec;
  }

  /**
   * Sets the matrix to use as initial selected output format
   * @param matrix the matrix to use as initial selected output format
   */
  public void setResultMatrix(Class matrix) {
    m_ResultMatrix = matrix;
  }

  /**
   * Gets the currently selected output format result matrix.
   * @return the currently selected matrix to use as output
   */
  public Class getResultMatrix() {
    return m_ResultMatrix;
  }

  /**
   * sets whether to remove the filter classname from the dataset name
   */
  public void setRemoveFilterName(boolean remove) {
    m_RemoveFilterName = remove;
  }

  /**
   * returns whether the filter classname is removed from the dataset name
   */
  public boolean getRemoveFilterName() {
    return m_RemoveFilterName;
  }

  /**
   * sets whether the average for each column is displayed
   */
  public void setShowAverage(boolean show) {
    m_ShowAverage = show;
  }

  /**
   * returns whether the average for each column is displayed
   */
  public boolean getShowAverage() {
    return m_ShowAverage;
  }

  /**
   * sets the class of the chosen result matrix
   */
  protected void setFormat() {
    for (int i = 0; i < m_OutputFormatClasses.size(); i++) {
      if (m_OutputFormatNames.get(i).toString().equals(
            m_OutputFormatComboBox.getItemAt(i).toString())) {
        m_OutputFormatComboBox.setSelectedIndex(i);
        break;
      }
    }
  }
  
  /**
   * the result from the last display of the dialog, the same is returned
   * from <code>showDialog</code>
   * 
   * @return the result from the last display of the dialog; 
   *         either APPROVE_OPTION, or CANCEL_OPTION
   * @see #showDialog()
   */
  public int getResult() {
    return m_Result;
  }

  /**
   * Pops up the modal dialog and waits for cancel or a selection.
   *
   * @return either APPROVE_OPTION, or CANCEL_OPTION
   */
  public int showDialog() {
    m_Result = CANCEL_OPTION;
    setData();
    setVisible(true);
    return m_Result;
  }

  /**
   * for testing only
   */
  public static void main(String[] args) {
    OutputFormatDialog      dialog;
    
    dialog = new OutputFormatDialog(null);
    if (dialog.showDialog() == APPROVE_OPTION)
      System.out.println("Accepted");
    else
      System.out.println("Aborted");
  }
}
