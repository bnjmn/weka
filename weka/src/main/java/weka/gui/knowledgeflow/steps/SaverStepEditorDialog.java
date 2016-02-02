package weka.gui.knowledgeflow.steps;

import weka.core.converters.FileSourcedConverter;
import weka.gui.EnvironmentField;
import weka.gui.FileEnvironmentField;
import weka.gui.PropertySheetPanel;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.steps.Saver;
import weka.knowledgeflow.steps.Step;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SaverStepEditorDialog extends GOEStepEditorDialog {

  protected EnvironmentField m_prefixOrFile;
  protected FileEnvironmentField m_directory;

  protected JLabel m_dirLab = new JLabel("Directory ", SwingConstants.RIGHT);
  protected JLabel m_prefLab = new JLabel("Prefix ", SwingConstants.RIGHT);

  public SaverStepEditorDialog() {
    super();
  }

  public void setStepToEdit(Step step) {
    copyOriginal(step);

    Saver wrappedStep = (Saver) step;
    if (wrappedStep.getSaver() instanceof FileSourcedConverter) {
      setupFileSaver(wrappedStep);
    } else {
      super.setStepToEdit(step);
    }
  }

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

  protected void setupOther() {
    addPrimaryEditorPanel(BorderLayout.NORTH);
    addSecondaryEditorPanel(BorderLayout.CENTER);
    add(m_editorHolder, BorderLayout.CENTER);
  }

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
