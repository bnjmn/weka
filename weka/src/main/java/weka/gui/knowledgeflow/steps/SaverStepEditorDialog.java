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
 *    SaverStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.converters.FileSourcedConverter;
import weka.gui.EnvironmentField;
import weka.gui.FileEnvironmentField;
import weka.gui.PropertySheetPanel;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.steps.Saver;
import weka.knowledgeflow.steps.Step;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Editor dialog for the saver step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SaverStepEditorDialog extends GOEStepEditorDialog {

  private static final long serialVersionUID = -8353826767500440827L;

  /**
   * Field for specifying either the file prefix to use or the actual filename
   * itself
   */
  protected EnvironmentField m_prefixOrFile;

  /** Field for specifying the directory to save into */
  protected FileEnvironmentField m_directory;

  /** Label for the directory field */
  protected JLabel m_dirLab = new JLabel("Directory ", SwingConstants.RIGHT);

  /** Label for the file/prefix field */
  protected JLabel m_prefLab = new JLabel("Prefix ", SwingConstants.RIGHT);

  /**
   * Constructor
   */
  public SaverStepEditorDialog() {
    super();
  }

  /**
   * Set the step to edit
   *
   * @param step the step to edit
   */
  public void setStepToEdit(Step step) {
    copyOriginal(step);

    Saver wrappedStep = (Saver) step;
    if (wrappedStep.getSaver() instanceof FileSourcedConverter) {
      setupFileSaver(wrappedStep);
    } else {
      super.setStepToEdit(step);
    }
  }

  /**
   * Setup for editing file-based savers
   *
   * @param wrappedStep the step to edit
   */
  protected void setupFileSaver(final Saver wrappedStep) {
    addPrimaryEditorPanel(BorderLayout.NORTH);
    m_prefixOrFile = new EnvironmentField();
    m_prefixOrFile.setEnvironment(m_env);
    m_directory = new FileEnvironmentField("", JFileChooser.SAVE_DIALOG, true);
    m_directory.setEnvironment(m_env);
    JPanel p = new JPanel();
    p.setLayout(new BorderLayout());
    m_secondaryEditor = new PropertySheetPanel(false);
    m_secondaryEditor.setEnvironment(m_env);
    m_secondaryEditor.setTarget(m_stepToEdit);
    p.add(m_secondaryEditor, BorderLayout.NORTH);
    JPanel tp = new JPanel();
    tp.setLayout(new BorderLayout());
    JPanel dp = new JPanel();
    dp.setLayout(new GridLayout(0, 1));
    dp.add(m_dirLab);
    dp.add(m_prefLab);
    JPanel dp2 = new JPanel();
    dp2.setLayout(new GridLayout(0, 1));
    dp2.add(m_directory);
    dp2.add(m_prefixOrFile);
    tp.add(dp, BorderLayout.WEST);
    tp.add(dp2, BorderLayout.CENTER);
    p.add(tp, BorderLayout.CENTER);

    m_primaryEditorHolder.add(p, BorderLayout.CENTER);
    add(m_editorHolder, BorderLayout.CENTER);

    if (!wrappedStep.getRelationNameForFilename()) {
      m_prefLab.setText("Filename");
    }

    try {
      String dir = wrappedStep.getSaver().retrieveDir();
      String prefixOrFile = wrappedStep.getSaver().filePrefix();
      m_directory.setText(dir);
      m_prefixOrFile.setText(prefixOrFile);
    } catch (Exception e) {
      e.printStackTrace();
    }

    m_secondaryEditor.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (wrappedStep.getRelationNameForFilename()) {
          if (m_prefLab.getText().startsWith("File")) {
            m_prefLab.setText("Prefix ");
          }
        } else {
          if (m_prefLab.getText().startsWith("Prefix")) {
            m_prefLab.setText("Filename ");
          }
        }
      }
    });
  }

  /**
   * Setup for other types of savers
   */
  protected void setupOther() {
    addPrimaryEditorPanel(BorderLayout.NORTH);
    addSecondaryEditorPanel(BorderLayout.CENTER);
    add(m_editorHolder, BorderLayout.CENTER);
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  protected void okPressed() {
    if (((Saver) m_stepToEdit).getSaver() instanceof FileSourcedConverter) {
      try {
        ((Saver) m_stepToEdit).getSaver().setDir(m_directory.getText());
        ((Saver) m_stepToEdit).getSaver().setFilePrefix(
          m_prefixOrFile.getText());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    super.okPressed(); // just in case saver has a customizer
  }
}
