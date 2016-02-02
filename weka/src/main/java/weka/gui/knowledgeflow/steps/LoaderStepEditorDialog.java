package weka.gui.knowledgeflow.steps;

import weka.core.converters.FileSourcedConverter;
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

  protected FileEnvironmentField m_fileLoader;

  public LoaderStepEditorDialog() {
    super();
  }

  @Override
  public void setStepToEdit(Step step) {
    copyOriginal(step);
    Loader wrappedStep = (Loader) step;

    if (wrappedStep.getLoader() instanceof FileSourcedConverter) {
      setupFileLoader(wrappedStep);
    } else /* if (wrappedStep.getLoader() instanceof DatabaseConverter) */ {
      super.setStepToEdit(step);
    }
  }

  protected void setupFileLoader(Loader wrappedStep) {

    addPrimaryEditorPanel(BorderLayout.NORTH);
    m_fileLoader =
      new FileEnvironmentField("Filename", JFileChooser.OPEN_DIALOG, false);
    m_fileLoader.setEnvironment(m_env);
    JPanel p = new JPanel();
    p.setLayout(new BorderLayout());
    p.add(m_fileLoader, BorderLayout.NORTH);
    m_primaryEditorHolder.add(p, BorderLayout.CENTER);

    add(m_editorHolder, BorderLayout.CENTER);
    File currentFile =
      ((FileSourcedConverter) wrappedStep.getLoader()).retrieveFile();
    m_fileLoader.setValue(currentFile);
  }

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
