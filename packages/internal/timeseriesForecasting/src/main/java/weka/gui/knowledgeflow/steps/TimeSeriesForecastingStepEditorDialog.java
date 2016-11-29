/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


/*
 *    TimeSeriesForecastingStepEditorDialog.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.knowledgeflow.steps;

import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Instances;
import weka.gui.EnvironmentField;
import weka.gui.FileEnvironmentField;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.steps.TimeSeriesForecasting;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;

/**
 * Editor dialog for the time series forecasting step
 * 
 * @author Mark Hall
 * @version $Revision: $
 */
public class TimeSeriesForecastingStepEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = -12022508491905917L;

  /** The underlying WekaForecaster */
  protected WekaForecaster m_forecastingModel;

  /** The header of the data used to train the forecaster */
  protected Instances m_header;

  /** The text area to display the model in */
  protected JTextArea m_modelDisplay = new JTextArea(20, 60);

  /** Handles the text field and file browser */
  protected FileEnvironmentField m_filenameField = new FileEnvironmentField();

  /** Label for the num steps field */
  protected JLabel m_numStepsLab;

  /** Number of steps to forecast */
  protected EnvironmentField m_numStepsToForecast = new EnvironmentField();

  /** Label for the artificial time stamp offset fields */
  protected JLabel m_artificialLab;

  /**
   * Number of steps beyond the end of the training data that incoming
   * historical priming data is
   */
  protected EnvironmentField m_artificialOffset = new EnvironmentField();

  /** Rebuild the forecaster ? */
  protected JCheckBox m_rebuildForecasterCheck = new JCheckBox();

  /** Label for the save forecaster field */
  protected JLabel m_saveLab;

  /** Text field for the filename to save the forecaster to */
  protected FileEnvironmentField m_saveFilenameField =
    new FileEnvironmentField();

  protected void initialize() {

    TimeSeriesForecasting forecaster = (TimeSeriesForecasting) getStepToEdit();
    String loadFilename = forecaster.getFilename().toString();
    if (!TimeSeriesForecasting.isEmpty(loadFilename)
      && !loadFilename.equals("-NONE-")) {
      m_filenameField.setText(loadFilename);
      loadModel();
    } else {
      String encodedForecaster = forecaster.getEncodedForecaster();
      if (!TimeSeriesForecasting.isEmpty(encodedForecaster)
        && !encodedForecaster.equals("-NONE-")) {
        try {
          List<Object> model =
            TimeSeriesForecasting.getForecaster(encodedForecaster);
          if (model != null) {
            m_forecastingModel = (WekaForecaster) model.get(0);
            m_header = (Instances) model.get(1);
            m_modelDisplay.setText(m_forecastingModel.toString());
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }

    if (!TimeSeriesForecasting
      .isEmpty(forecaster.getSaveFilename().toString())) {
      m_saveFilenameField.setText(forecaster.getSaveFilename().toString());
    }
    m_numStepsToForecast.setText(forecaster.getNumStepsToForecast());
    m_artificialOffset.setText(forecaster.getArtificialTimeStartOffset());
    m_rebuildForecasterCheck.setSelected(forecaster.getRebuildForecaster());

    m_saveLab.setEnabled(m_rebuildForecasterCheck.isSelected());
    m_saveFilenameField.setEnabled(m_rebuildForecasterCheck.isSelected());

    checkIfModelIsUsingArtificialTimeStamp();
    checkIfModelIsUsingOverlayData();
  }

  @Override
  protected void layoutEditor() {
    m_filenameField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (TimeSeriesForecasting.isEmpty(m_filenameField.getText())) {
          return;
        }

        loadModel();
        if (m_forecastingModel != null) {
          m_modelDisplay.setText(m_forecastingModel.toString());
          checkIfModelIsUsingArtificialTimeStamp();
          checkIfModelIsUsingOverlayData();
        }
      }
    });

    JTabbedPane tabHolder = new JTabbedPane();

    JPanel modelFilePanel = new JPanel();
    modelFilePanel.setLayout(new BorderLayout());
    JPanel tempP1 = new JPanel();

    tempP1.setLayout(new GridLayout(5, 2));
    JLabel fileLab = new JLabel("Load/import forecaster", SwingConstants.RIGHT);
    tempP1.add(fileLab);
    tempP1.add(m_filenameField);
    m_numStepsLab =
      new JLabel("Number of steps to forecast", SwingConstants.RIGHT);
    tempP1.add(m_numStepsLab);
    tempP1.add(m_numStepsToForecast);
    m_artificialLab = new JLabel(
      "Number of historical instances " + "beyond end of training data",
      SwingConstants.RIGHT);
    tempP1.add(m_artificialLab);
    tempP1.add(m_artificialOffset);
    JLabel rebuildLab =
      new JLabel("Rebuild/reestimate on incoming data", SwingConstants.RIGHT);
    tempP1.add(rebuildLab);
    tempP1.add(m_rebuildForecasterCheck);
    m_saveLab = new JLabel("Save forecaster", SwingConstants.RIGHT);
    tempP1.add(m_saveLab);
    tempP1.add(m_saveFilenameField);

    modelFilePanel.add(tempP1, BorderLayout.NORTH);

    tabHolder.addTab("Model file", modelFilePanel);

    add(tabHolder, BorderLayout.CENTER);

    JPanel modelPanel = new JPanel();
    modelPanel.setLayout(new BorderLayout());
    m_modelDisplay.setEditable(false);
    m_modelDisplay.setFont(new Font("Monospaced", Font.PLAIN, 12));
    m_modelDisplay.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    JScrollPane scrollPane = new JScrollPane(m_modelDisplay);

    modelPanel.add(scrollPane, BorderLayout.CENTER);

    tabHolder.addTab("Model", modelPanel);

    m_saveLab.setEnabled(false);
    m_saveFilenameField.setEnabled(false);
    m_rebuildForecasterCheck.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_saveFilenameField.setEnabled(m_rebuildForecasterCheck.isSelected());
        m_saveLab.setEnabled(m_rebuildForecasterCheck.isSelected());
      }
    });

    initialize();
  }

  private void loadModel() {
    if (!TimeSeriesForecasting.isEmpty(m_filenameField.getText())) {
      try {
        String filename = m_filenameField.getText();
        filename = environmentSubstitute(filename);

        File theFile = new File(filename);
        if (theFile.isFile()) {
          ObjectInputStream is = new ObjectInputStream(
            new BufferedInputStream(new FileInputStream(filename)));
          m_forecastingModel = (WekaForecaster) is.readObject();
          m_header = (Instances) is.readObject();
          // Instances header = (Instances)is.readObject();
          is.close();
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  private void checkIfModelIsUsingArtificialTimeStamp() {
    if (m_forecastingModel != null) {
      boolean usingA =
        m_forecastingModel.getTSLagMaker().isUsingAnArtificialTimeIndex();
      m_artificialLab.setEnabled(usingA);
      m_artificialOffset.setEnabled(usingA);
    }
  }

  private void checkIfModelIsUsingOverlayData() {
    if (m_forecastingModel != null) {
      if (m_forecastingModel.isUsingOverlayData()) {
        m_numStepsToForecast.setEnabled(false);
        m_numStepsLab.setEnabled(false);
        // remove any number set here since size of the overlay data (with
        // missing
        // targets set) determines the number of steps that will be forecast
        m_numStepsToForecast.setText("");
      } else {
        m_numStepsToForecast.setEnabled(true);
        m_numStepsLab.setEnabled(true);
      }
    }
  }

  @Override
  public void okPressed() {
    TimeSeriesForecasting forecaster =
      ((TimeSeriesForecasting) getStepToEdit());

    if (!TimeSeriesForecasting.isEmpty(m_filenameField.getText())) {
      forecaster.setFilename(new File(m_filenameField.getText()));
    } else {
      if (m_forecastingModel != null) {
        try {
          // set base64 field and clear filename field with ""
          String encodedModel = TimeSeriesForecasting
            .encodeForecasterToBase64(m_forecastingModel, m_header);
          forecaster.setFilename(new File(""));
          forecaster.setEncodedForecaster(encodedModel);
        } catch (Exception ex) {
          showErrorDialog(ex);
        }
      }
    }
    forecaster.setRebuildForecaster(m_rebuildForecasterCheck.isSelected());
    forecaster.setNumStepsToForecast(m_numStepsToForecast.getText());
    forecaster.setArtificialTimeStartOffset(m_artificialOffset.getText());
    if (m_rebuildForecasterCheck.isSelected()
      && !TimeSeriesForecasting.isEmpty(m_saveFilenameField.getText())) {
      forecaster.setSaveFilename(new File(m_saveFilenameField.getText()));
    } else {
      forecaster.setSaveFilename(new File(""));
    }
  }
}
