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
 *    Copyright (C) 2005 FracPete
 *
 */


package weka.gui.experiment;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
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
 * @version $Revision: 1.1.2.1 $
 */
public class OutputFormatDialog extends JDialog {
  /** Signifies an OK property selection */
  public static final int APPROVE_OPTION = 0;

  /** Signifies a cancelled property selection */
  public static final int CANCEL_OPTION = 1;

  /** the result of the user's action, either OK or CANCEL */
  protected int m_Result = CANCEL_OPTION;
  
  /** the different types for outputting the comparison tables */
  protected final static String[] m_OutputFormats = {"Plain Text", "LaTeX", "CSV"};
  
  /** lets the user choose the format for the output */
  protected JComboBox m_OutputFormatComboBox = new JComboBox(m_OutputFormats);

  /** the spinner to choose the precision for the mean from */
  protected JSpinner m_MeanPrecSpinner = new JSpinner();

  /** the spinner to choose the precision for the std. deviation from */
  protected JSpinner m_StdDevPrecSpinner = new JSpinner();
  
  /** Click to activate the current set parameters */
  protected JButton m_OkButton = new JButton("OK");

  /** Click to cancel the dialog */
  protected JButton m_CancelButton = new JButton("Cancel");
  
  /** Produce tables in latex format */
  protected boolean m_latexOutput = false;
  
  /** Produce tables in csv format */
  protected boolean m_csvOutput = false;
  
  /** the number of digits after the period (= precision) for printing the mean */
  protected int m_MeanPrec = 2;
  
  /** the number of digits after the period (= precision) for printing the std. deviation */
  protected int m_StdDevPrec = 2;

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
    
    getContentPane().setLayout(new BorderLayout());
    
    panel = new JPanel(new GridLayout(3, 2));
    getContentPane().add(panel, BorderLayout.CENTER);
    
    // Precision
    model = (SpinnerNumberModel) m_MeanPrecSpinner.getModel();
    model.setMaximum(new Integer(20));
    model.setMinimum(new Integer(0));
    model = (SpinnerNumberModel) m_StdDevPrecSpinner.getModel();
    model.setMaximum(new Integer(20));
    model.setMinimum(new Integer(0));
    panel.add(new JLabel("Mean Precision"));
    panel.add(m_MeanPrecSpinner);
    panel.add(new JLabel("StdDev. Precision"));
    panel.add(m_StdDevPrecSpinner);
    
    // Format
    panel.add(new JLabel("Output Format"));
    panel.add(m_OutputFormatComboBox);
    
    // Buttons
    panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    getContentPane().add(panel, BorderLayout.SOUTH);
    m_CancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_Result = CANCEL_OPTION;
        setVisible(false);
      }
    });
    m_OkButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        getData();
        m_Result = APPROVE_OPTION;
        setVisible(false);
      }
    });
    panel.add(m_OkButton);
    panel.add(m_CancelButton);

    pack();
  }
  
  /**
   * initializes the GUI components with the data
   */
  private void setData() {
    // Precision
    m_MeanPrecSpinner.setValue(new Integer(m_MeanPrec));
    m_StdDevPrecSpinner.setValue(new Integer(m_StdDevPrec));
    
    // format
    if (getProduceLatex())
      m_OutputFormatComboBox.setSelectedIndex(1);
    else
    if (getProduceCSV())
      m_OutputFormatComboBox.setSelectedIndex(2);
    else
      m_OutputFormatComboBox.setSelectedIndex(0);
  }    
  
  /**
   *  gets the data from GUI components
   */
  private void getData() {
    // Precision
    m_MeanPrec   = Integer.parseInt(m_MeanPrecSpinner.getValue().toString());
    m_StdDevPrec = Integer.parseInt(m_StdDevPrecSpinner.getValue().toString());
    
    // format
    setProduceLatex(m_OutputFormatComboBox.getSelectedIndex() == 1);
    setProduceCSV(m_OutputFormatComboBox.getSelectedIndex() == 2);
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
   * Set whether latex is output
   * @param l true if tables are to be produced in Latex format
   */
  public void setProduceLatex(boolean l) {
    m_latexOutput = l;
    if (m_latexOutput)
      setProduceCSV(false);
  }

  /**
   * Get whether latex is output
   * @return true if Latex is to be output
   */
  public boolean getProduceLatex() {
    return m_latexOutput;
  }
  
  /**
   * Set whether csv is output
   * @param c true if tables are to be produced in csv format
   */
  public void setProduceCSV(boolean c) {
    m_csvOutput = c;
    if (m_csvOutput)
      setProduceLatex(false);
  }
  
  /**
   * Get whether csv is output
   * @return true if csv is to be output
   */
  public boolean getProduceCSV() {
    return m_csvOutput;
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
