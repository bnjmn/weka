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
 *    ExecuteProcessStepEditorDialog.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import weka.core.Environment;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.FileEnvironmentField;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.steps.ExecuteProcess;

/**
 * Step editor dialog for the ExecuteProcess step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ExecuteProcessStepEditorDialog extends StepEditorDialog {
  private static final long serialVersionUID = -7740004961609720209L;

  /** Panel for static commands */
  protected StaticProcessPanel m_staticPanel;

  /** Panel for dynamic commands */
  protected DynamicProcessPanel m_dynamicPanel;

  /** Checkbox to indicate whether dynamic commands are to be executed */
  protected JCheckBox m_useDynamicCheck = new JCheckBox(
    "Execute dynamic commands");

  /**
   * Checkbox to indicate whether complete command failure (vs just returning a
   * non-zero status) should generate an exception
   */
  protected JCheckBox m_raiseExceptonOnCommandFailure = new JCheckBox(
    "Raise an exception for command failure");

  /**
   * Layout the editor dialog
   */
  @Override
  protected void layoutEditor() {
    ExecuteProcess executeProcess = ((ExecuteProcess) getStepToEdit());
    boolean hasIncomingInstances =
      executeProcess.getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_INSTANCE) > 0
        || executeProcess.getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_ENVIRONMENT) > 0;

    Environment env = Environment.getSystemWide();

    JPanel holderP = new JPanel(new BorderLayout());
    JPanel checkP = new JPanel(new BorderLayout());
    holderP.add(checkP, BorderLayout.NORTH);
    if (hasIncomingInstances) {
      checkP.add(m_useDynamicCheck, BorderLayout.NORTH);
      m_useDynamicCheck.setSelected(executeProcess.getUseDynamic());
    } else {
      m_useDynamicCheck = null;
    }
    checkP.add(m_raiseExceptonOnCommandFailure, BorderLayout.SOUTH);
    m_raiseExceptonOnCommandFailure.setSelected(executeProcess
      .getRaiseExceptionOnCommandFailure());

    m_staticPanel = new StaticProcessPanel(executeProcess, env);
    if (hasIncomingInstances) {
      JTabbedPane tabbedPane = new JTabbedPane();
      tabbedPane.add("Static", m_staticPanel);
      m_dynamicPanel = new DynamicProcessPanel(executeProcess);
      tabbedPane.add("Dynamic", m_dynamicPanel);
      holderP.add(tabbedPane, BorderLayout.CENTER);
    } else {
      holderP.add(m_staticPanel, BorderLayout.CENTER);
    }

    add(holderP, BorderLayout.CENTER);
  }

  /**
   * Executed when the OK button is pressed
   */
  @Override
  protected void okPressed() {
    ExecuteProcess executeProcess = (ExecuteProcess) getStepToEdit();
    if (m_useDynamicCheck != null && m_useDynamicCheck.isSelected()) {
      m_dynamicPanel.applyToStep(executeProcess);
    }
    executeProcess
      .setRaiseExceptionOnCommandFailure(m_raiseExceptonOnCommandFailure
        .isSelected());

    m_staticPanel.applyToStep(executeProcess);
  }

  /**
   * Panel for static command configuration
   */
  protected class StaticProcessPanel extends JPanel {

    private static final long serialVersionUID = -4215730214067279661L;
    protected JTextField m_cmdText = new JTextField(10);
    protected JTextField m_argsText = new JTextField(10);
    protected FileEnvironmentField m_workingDirField;

    public StaticProcessPanel(ExecuteProcess executeProcess, Environment env) {
      m_workingDirField =
        new FileEnvironmentField("", env, JFileChooser.OPEN_DIALOG, true);

      setLayout(new GridLayout(0, 2));
      add(new JLabel("Command", SwingConstants.RIGHT));
      add(m_cmdText);
      add(new JLabel("Arguments", SwingConstants.RIGHT));
      add(m_argsText);
      add(new JLabel("Working directory", SwingConstants.RIGHT));
      add(m_workingDirField);

      String cmd = executeProcess.getStaticCmd();
      if (cmd != null && cmd.length() > 0) {
        m_cmdText.setText(cmd);
      }
      String args = executeProcess.getStaticArgs();
      if (args != null && args.length() > 0) {
        m_argsText.setText(args);
      }
      String workingDir = executeProcess.getStaticWorkingDir();
      if (workingDir != null && workingDir.length() > 0) {
        m_workingDirField.setCurrentDirectory(workingDir);
      }
      m_workingDirField.setEnvironment(env);
    }

    public void applyToStep(ExecuteProcess executeProcess) {
      executeProcess.setStaticCmd(m_cmdText.getText());
      executeProcess.setStaticArgs(m_argsText.getText());
      executeProcess.setStaticWorkingDir(m_workingDirField.getText());
    }
  }

  /**
   * Panel for dynamic command configuration
   */
  protected class DynamicProcessPanel extends JPanel {

    private static final long serialVersionUID = 6440523583792476595L;
    protected JComboBox<String> m_cmdField = new JComboBox<>();
    protected JComboBox<String> m_argsField = new JComboBox<>();
    protected JComboBox<String> m_workingDirField = new JComboBox<>();

    public DynamicProcessPanel(ExecuteProcess executeProcess) {
      m_cmdField.setEditable(true);
      m_argsField.setEditable(true);
      m_workingDirField.setEditable(true);
      Map<String, List<StepManager>> incomingConnTypes =
        executeProcess.getStepManager().getIncomingConnections();

      // we are guaranteed exactly one incoming connection
      String incomingConnType = incomingConnTypes.keySet().iterator().next();

      try {
        Instances incomingStructure =
          executeProcess.outputStructureForConnectionType(incomingConnType);

        if (incomingStructure != null) {
          // add blank entries to indicate no args or working directory
          // necessary
          m_argsField.addItem("");
          m_workingDirField.addItem("");
          for (int i = 0; i < incomingStructure.numAttributes(); i++) {
            if (incomingStructure.attribute(i).isString()
              || incomingStructure.attribute(i).isNominal()) {
              m_cmdField.addItem(incomingStructure.attribute(i).name());
              m_argsField.addItem(incomingStructure.attribute(i).name());
              m_workingDirField.addItem(incomingStructure.attribute(i).name());
            }
          }
        }
        String currentCmdField = executeProcess.getDynamicCmdField();
        if (currentCmdField != null && currentCmdField.length() > 0) {
          m_cmdField.setSelectedItem(currentCmdField);
        }
        String currentArgsField = executeProcess.getDynamicArgsField();
        if (currentArgsField != null && currentArgsField.length() > 0) {
          m_argsField.setSelectedItem(currentArgsField);
        }
        String currentWorkingDirField =
          executeProcess.getDynamicWorkingDirField();
        if (currentWorkingDirField != null
          && currentWorkingDirField.length() > 0) {
          m_workingDirField.setSelectedItem(currentWorkingDirField);
        }
      } catch (WekaException ex) {
        showErrorDialog(ex);
      }

      setLayout(new GridLayout(0, 2));
      add(new JLabel("Command attribute", SwingConstants.RIGHT));
      add(m_cmdField);
      add(new JLabel("Arguments attribute", SwingConstants.RIGHT));
      add(m_argsField);
      add(new JLabel("Working dir attribute", SwingConstants.RIGHT));
      add(m_workingDirField);
    }

    public void applyToStep(ExecuteProcess executeProcess) {
      executeProcess
        .setDynamicCmdField(m_cmdField.getSelectedItem().toString());
      executeProcess.setDynamicArgsField(m_argsField.getSelectedItem()
        .toString());
      executeProcess.setDynamicWorkingDirField(m_workingDirField
        .getSelectedItem().toString());
      executeProcess.setUseDynamic(m_useDynamicCheck.isSelected());
    }
  }
}
