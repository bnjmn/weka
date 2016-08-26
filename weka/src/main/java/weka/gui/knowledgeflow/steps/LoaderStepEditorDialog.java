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
 *    LoaderStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.converters.FileSourcedConverter;
import weka.gui.ExtensionFileFilter;
import weka.gui.FileEnvironmentField;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.steps.Loader;
import weka.knowledgeflow.steps.Step;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;

/**
 * Provides a custom editor dialog for Loaders.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class LoaderStepEditorDialog extends GOEStepEditorDialog {

  private static final long serialVersionUID = -6501371943783384741L;

  /** Widget for specifying/choosing a file for file-based loaders */
  protected FileEnvironmentField m_fileLoader;

  /**
   * Constructor
   */
  public LoaderStepEditorDialog() {
    super();
  }

  /**
   * Set the step to edit in this editor
   * 
   * @param step the step to edit
   */
  @Override
  public void setStepToEdit(Step step) {
    copyOriginal(step);
    Loader wrappedStep = (Loader) step;

    if (wrappedStep.getLoader() instanceof FileSourcedConverter) {
      setupFileLoader(wrappedStep);
    } else /* if (wrappedStep.getLoader() instanceof DatabaseConverter) */{
      super.setStepToEdit(step);
    }
  }

  /**
   * Sets up the editor for dealing with file-based loaders
   * 
   * @param wrappedStep the {@code weka.core.converters.Loader} wrapped by the
   *          loader step
   */
  protected void setupFileLoader(Loader wrappedStep) {

    addPrimaryEditorPanel(BorderLayout.NORTH);
    m_fileLoader =
      new FileEnvironmentField("Filename", JFileChooser.OPEN_DIALOG, false);
    m_fileLoader.setEnvironment(m_env);
    m_fileLoader.resetFileFilters();
    FileSourcedConverter loader =
      ((FileSourcedConverter) ((Loader) getStepToEdit()).getLoader());
    String[] ext = loader.getFileExtensions();
    ExtensionFileFilter firstFilter = null;
    for (int i = 0; i < ext.length; i++) {
      ExtensionFileFilter ff = new ExtensionFileFilter(ext[i],
              loader.getFileDescription() + " (*" + ext[i] + ")");
      if (i == 0) {
        firstFilter = ff;
      }
      m_fileLoader.addFileFilter(ff);
    }

    if (firstFilter != null) {
      m_fileLoader.setFileFilter(firstFilter);
    }

    JPanel p = new JPanel();
    p.setLayout(new BorderLayout());
    p.add(m_fileLoader, BorderLayout.NORTH);
    m_primaryEditorHolder.add(p, BorderLayout.CENTER);

    add(m_editorHolder, BorderLayout.CENTER);
    File currentFile =
      ((FileSourcedConverter) wrappedStep.getLoader()).retrieveFile();
    m_fileLoader.setValue(currentFile);
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  protected void okPressed() {
    if (((Loader) m_stepToEdit).getLoader() instanceof FileSourcedConverter) {
      try {
        ((FileSourcedConverter) ((Loader) m_stepToEdit).getLoader())
          .setFile((File) m_fileLoader.getValue());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    super.okPressed(); // just in case the loader has a customizer
  }
}
