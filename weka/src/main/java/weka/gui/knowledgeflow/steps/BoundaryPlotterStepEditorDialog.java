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
 *    BoundaryPlotterStepEditorDialog.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.steps.BoundaryPlotter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Editor dialog for the boundary plotter step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class BoundaryPlotterStepEditorDialog extends GOEStepEditorDialog {

  private static final long serialVersionUID = 4351742205211273840L;

  /**
   * Combo box for selecting the x attribute to plot when there is an incoming
   * instances structure at design time
   */
  protected JComboBox<String> m_xCombo = new JComboBox<String>();

  /**
   * Combo box for selecting the y attribute to plot when there is an incoming
   * instances structure at design time
   */
  protected JComboBox<String> m_yCombo = new JComboBox<String>();

  /**
   * Environment field for specifying the x attribute when there isn't incoming
   * instances structure at design time
   */
  protected weka.gui.EnvironmentField m_xEnviro =
    new weka.gui.EnvironmentField();

  /**
   * Environment field for specifying the y attribute when there isn't incoming
   * instances structure at design time
   */
  protected weka.gui.EnvironmentField m_yEnviro =
    new weka.gui.EnvironmentField();

  /**
   * Layout the editor
   */
  @Override
  public void layoutEditor() {
    // just need to add widgets for choosing the x and y visualization
    // attributes
    m_xCombo.setEditable(true);
    m_yCombo.setEditable(true);

    BoundaryPlotter step = (BoundaryPlotter) getStepToEdit();
    Instances incomingStructure = null;
    try {
      incomingStructure =
        step.getStepManager().getIncomingStructureForConnectionType(
          StepManager.CON_DATASET);
      if (incomingStructure == null) {
        incomingStructure =
          step.getStepManager().getIncomingStructureForConnectionType(
            StepManager.CON_TRAININGSET);
      }
      if (incomingStructure == null) {
        incomingStructure =
          step.getStepManager().getIncomingStructureForConnectionType(
            StepManager.CON_TESTSET);
      }
      if (incomingStructure == null) {
        incomingStructure =
          step.getStepManager().getIncomingStructureForConnectionType(
            StepManager.CON_INSTANCE);
      }
    } catch (WekaException ex) {
      showErrorDialog(ex);
    }

    JPanel attPan = new JPanel(new GridLayout(1, 2));
    JPanel xHolder = new JPanel(new BorderLayout());
    JPanel yHolder = new JPanel(new BorderLayout());
    xHolder.setBorder(new TitledBorder("X axis"));
    yHolder.setBorder(new TitledBorder("Y axis"));
    attPan.add(xHolder);
    attPan.add(yHolder);

    if (incomingStructure != null) {
      m_xEnviro = null;
      m_yEnviro = null;
      xHolder.add(m_xCombo, BorderLayout.CENTER);
      yHolder.add(m_yCombo, BorderLayout.CENTER);
      String xAttN = step.getXAttName();
      String yAttN = step.getYAttName();

      // populate combos and try to match
      int numAdded = 0;
      for (int i = 0; i < incomingStructure.numAttributes(); i++) {
        Attribute att = incomingStructure.attribute(i);
        if (att.isNumeric()) {
          m_xCombo.addItem(att.name());
          m_yCombo.addItem(att.name());
          numAdded++;
        }
      }
      attPan.add(xHolder);
      attPan.add(yHolder);

      if (numAdded < 2) {
        showInfoDialog("There are not enough numeric attributes in "
          + "the incoming data to visualize with", "Not enough attributes "
          + "available", true);
      } else {
        // try to match
        if (xAttN != null && xAttN.length() > 0) {
          m_xCombo.setSelectedItem(xAttN);
        }
        if (yAttN != null && yAttN.length() > 0) {
          m_yCombo.setSelectedItem(yAttN);
        }
      }
    } else {
      m_xCombo = null;
      m_yCombo = null;
      xHolder.add(m_xEnviro, BorderLayout.CENTER);
      yHolder.add(m_yEnviro, BorderLayout.CENTER);
      m_xEnviro.setText(step.getXAttName());
      m_yEnviro.setText(step.getYAttName());
    }

    m_editorHolder.add(attPan, BorderLayout.SOUTH);
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  public void okPressed() {
    String xName =
      m_xCombo != null ? m_xCombo.getSelectedItem().toString() : m_xEnviro
        .getText();
    String yName =
      m_yCombo != null ? m_yCombo.getSelectedItem().toString() : m_yEnviro
        .getText();
    ((BoundaryPlotter) getStepToEdit()).setXAttName(xName);
    ((BoundaryPlotter) getStepToEdit()).setYAttName(yName);
  }
}
