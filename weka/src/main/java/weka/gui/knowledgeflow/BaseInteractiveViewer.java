package weka.gui.knowledgeflow;

import weka.core.Defaults;
import weka.core.Settings;
import weka.gui.SettingsEditor;
import weka.knowledgeflow.steps.Step;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class BaseInteractiveViewer extends JPanel implements
  StepInteractiveViewer {

  private static final long serialVersionUID = -1191494001428785466L;

  protected Step m_step;

  protected JButton m_closeBut = new JButton("Close");
  protected JPanel m_buttonHolder = new JPanel(new GridLayout());

  protected Window m_parent;

  protected MainKFPerspective m_mainPerspective;

  public BaseInteractiveViewer() {
    super();
    setLayout(new BorderLayout());

    JPanel tempP = new JPanel(new BorderLayout());
    tempP.add(m_buttonHolder, BorderLayout.WEST);
    add(tempP, BorderLayout.SOUTH);

    m_closeBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        close();
      }
    });
    addButton(m_closeBut);

    if (getDefaultSettings() != null) {
      JButton editSettings = new JButton("Settings");
      editSettings.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          // popup single settings editor
          // String ID = getDefaultSettings().getID();
          try {
            if (SettingsEditor.showSingleSettingsEditor(getMainKFPerspective()
              .getMainApplication().getApplicationSettings(),
              getDefaultSettings().getID(), getViewerName(),
              BaseInteractiveViewer.this) == JOptionPane.OK_OPTION) {
              applySettings(getSettings());
            }
          } catch (IOException ex) {
            getMainKFPerspective().getMainApplication().showErrorDialog(ex);
          }
        }
      });
      addButton(editSettings);
    }
  }

  @Override
  public Settings getSettings() {
    return m_mainPerspective.getMainApplication().getApplicationSettings();
  }

  public void applySettings(Settings settings) {

  }

  @Override
  public void setMainKFPerspective(MainKFPerspective perspective) {
    m_mainPerspective = perspective;

    m_mainPerspective.getMainApplication().getApplicationSettings()
      .applyDefaults(getDefaultSettings());
  }

  @Override
  public MainKFPerspective getMainKFPerspective() {
    return m_mainPerspective;
  }

  @Override
  public void setStep(Step step) {
    m_step = step;
  }

  public Step getStep() {
    return m_step;
  }

  @Override
  public void nowVisible() {
    // no-op. Subclasses to override if necessary
  }

  @Override
  public void setParentWindow(Window parent) {
    m_parent = parent;
    m_parent.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        closePressed();
      }
    });
  }

  public void addButton(JButton button) {
    m_buttonHolder.add(button);
  }

  private void close() {
    closePressed();

    if (m_parent != null) {
      m_parent.dispose();
    }
  }

  public void closePressed() {
    // subclasses to override if they need to do something before
    // the window is closed
  }

  public Defaults getDefaultSettings() {
    // subclasses to override if they have default settings
    return null;
  }
}
